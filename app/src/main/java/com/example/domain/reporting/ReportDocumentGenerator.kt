package com.example.domain.reporting

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.time.LocalDate
import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.ExportFormat
import com.example.data.model.PhotoReportItem
import com.example.data.model.ReportTemplateDocument
import com.example.data.model.ReportType
import com.example.data.model.SheetLayout
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskDocument
import com.example.data.model.TaskStatus
import com.example.ui.components.PhotoCaptureUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ReportSectionKind(
    val code: String,
    val badgeRu: String
) {
    DAILY_FACT_REPORT("FACT_REPORT", "ОТЧЁТ ЗА ДЕНЬ (ФАКТ)"),
    NEXT_DAY_PLAN("NEXT_DAY_PLAN", "ПЛАН НА СЛЕДУЮЩИЙ ДЕНЬ")
}

data class ReportTableRow(
    val index: Int,
    val sectionKind: ReportSectionKind = ReportSectionKind.DAILY_FACT_REPORT,
    val rowTypeBadge: String = "ОТЧЁТ (ФАКТ)",
    val assigneeFio: String,
    val groupName: String,
    val taskTitle: String,
    val volumeInfo: String,
    val planVolumeLabel: String = "",
    val factVolumeLabel: String = "",
    val checklistBreakdown: List<String> = emptyList(),
    val completionPercent: Int,
    val priorityLabel: String,
    val statusLabel: String,
    val deadlineLabel: String,
    val photoInfo: String,
    val carryoverInfo: String,
    val commentSummary: String
)

data class ReportSectionBlock(
    val kind: ReportSectionKind,
    val bannerTitle: String,
    val bannerSubtitle: String,
    val dateLabel: String,
    val emptyStateMessage: String,
    val headers: List<String>,
    val rows: List<ReportTableRow>
)

data class ReportAttachedPhoto(
    val taskTitle: String,
    val assigneeFio: String,
    val photo: PhotoReportItem
)

data class ReportSheetPreview(
    val sheetIndex: Int,
    val sheetTitle: String,
    val subtitle: String,
    val headers: List<String>,
    val rows: List<ReportTableRow>,
    val sections: List<ReportSectionBlock> = emptyList()
)

data class GeneratedDocumentResult(
    val file: File,
    val orgHeader: String,
    val reportTitle: String,
    val periodLabel: String,
    val generatedAtLabel: String,
    val preparedByLabel: String,
    val roleScopeLabel: String,
    val sheetLayout: SheetLayout,
    val exportFormat: ExportFormat,
    val includePhotos: Boolean = true,
    val attachedPhotos: List<ReportAttachedPhoto> = emptyList(),
    val sheets: List<ReportSheetPreview>,
    val signatureBlock: List<String>,
    val summaryText: String,
    val downloadedToPath: String = ""
)

object ReportDocumentGenerator {

    fun formatEpochDay(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return String.format(
            Locale.US,
            "%02d.%02d.%04d",
            date.dayOfMonth,
            date.monthValue,
            date.year
        )
    }

    fun resolveTemplateDisplayName(template: ReportTemplateDocument, currentEpochDay: Long): String {
        val dateStr = formatEpochDay(currentEpochDay)
        val rType = template.parsedReportType
        val rawName = template.name.trim()
        return if (
            rType == ReportType.DAILY_REPORT ||
            rawName.contains("Суточный рапорт", ignoreCase = true) ||
            rawName.contains("Отчёт за текущий", ignoreCase = true) ||
            rawName.contains("Отчет за текущий", ignoreCase = true)
        ) {
            "Отчет за текущий день ($dateStr)"
        } else {
            rawName
        }
    }

    fun buildPreview(
        actor: CorporateEmployee,
        template: ReportTemplateDocument,
        allTasks: List<TaskDocument>,
        currentEpochDay: Long,
        overrideLayout: SheetLayout? = null,
        overrideFormat: ExportFormat? = null,
        includePhotos: Boolean = true
    ): GeneratedDocumentResult {
        val reportType = template.parsedReportType
        val layout = overrideLayout ?: template.parsedSheetLayout
        val format = overrideFormat ?: template.parsedExportFormat
        val resolvedReportTitle = resolveTemplateDisplayName(template, currentEpochDay)
        val todayStr = formatEpochDay(currentEpochDay)
        val tomorrowStr = formatEpochDay(currentEpochDay + 1L)

        // Enforce RBAC for consolidated reports
        if (reportType.isConsolidatedOnly && !actor.role.canExportConsolidatedPersonnelReport()) {
            throw IllegalAccessException(
                "Сводный отчёт «${reportType.titleRu}» доступен только для ролей ИТР и Руководитель"
            )
        }

        // Scope tasks by role hierarchy
        val roleScopedTasks = when (actor.role) {
            CorporateRole.ADMIN, CorporateRole.HEAD -> allTasks
            CorporateRole.ITR -> allTasks.filter {
                it.groupId == actor.groupId || it.assigneeId == actor.id || it.creatorId == actor.id
            }
            CorporateRole.WORKER -> allTasks.filter { it.assigneeId == actor.id }
        }

        val roleScopeLabel = when (actor.role) {
            CorporateRole.ADMIN -> "Полный доступ администратора (${actor.siteName})"
            CorporateRole.HEAD -> "Сводный по всему персоналу участка (${actor.siteName})"
            CorporateRole.ITR -> "Сводный по подчинённому персоналу (${actor.groupName})"
            CorporateRole.WORKER -> "Личная отчётность исполнителя (${actor.fullName})"
        }

        // 1. Задачи для ПЛАНА РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ:
        // - Задачи, назначенные на следующую смену (dueDateEpochDay >= currentEpochDay + 1)
        // - Незавершённые задачи текущего дня (остаток объёма < 100% или статус переходящий)
        val planTasks = roleScopedTasks.filter {
            it.dueDateEpochDay >= currentEpochDay + 1L ||
                (it.dueDateEpochDay == currentEpochDay && (it.taskStatus.isEligibleForCarryover || !it.isVolumeCompleted100Percent()))
        }

        // 2. Задачи для ОТЧЁТА ЗА ТЕКУЩИЙ ДЕНЬ (ФАКТ):
        val factTasks = when (reportType) {
            ReportType.NEXT_DAY_PLAN -> roleScopedTasks.filter { it.dueDateEpochDay <= currentEpochDay }
            ReportType.DAILY_REPORT -> roleScopedTasks.filter { it.dueDateEpochDay <= currentEpochDay }
            ReportType.PHOTO_QUALITY -> roleScopedTasks.filter { it.requiresPhotoReport || it.photoItems.isNotEmpty() }
            ReportType.OVERDUE_CARRYOVER -> roleScopedTasks.filter {
                it.carryoverCount > 0L || it.taskStatus == TaskStatus.OVERDUE || it.taskStatus == TaskStatus.REJECTED || !it.isVolumeCompleted100Percent()
            }
            ReportType.RECURRING_EXECUTION -> roleScopedTasks.filter { it.recurrenceRule != "NONE" }
            else -> roleScopedTasks.filter { it.dueDateEpochDay <= currentEpochDay }.ifEmpty { roleScopedTasks }
        }

        val reportHeaders = if (includePhotos) {
            listOf(
                "№",
                "Раздел",
                "ФИО исполнителя",
                "Подразделение",
                "Наименование работ (Отчёт)",
                "Объём: Выполнено из Плана (%)",
                "Статус приёмки",
                "Срок",
                "Фотоотчёт",
                "Переносы",
                "Контроль / Примечание"
            )
        } else {
            listOf(
                "№",
                "Раздел",
                "ФИО исполнителя",
                "Подразделение",
                "Наименование работ (Отчёт)",
                "Объём: Выполнено из Плана (%)",
                "Статус приёмки",
                "Срок",
                "Переносы",
                "Контроль / Примечание"
            )
        }

        val planHeaders = if (includePhotos) {
            listOf(
                "№",
                "Раздел",
                "ФИО исполнителя",
                "Подразделение",
                "Наименование плановых работ",
                "Плановый объём на смену",
                "Приоритет",
                "Основание / Статус",
                "Срок выполнения",
                "Требование фото",
                "Примечание"
            )
        } else {
            listOf(
                "№",
                "Раздел",
                "ФИО исполнителя",
                "Подразделение",
                "Наименование плановых работ",
                "Плановый объём на смену",
                "Приоритет",
                "Основание / Статус",
                "Срок выполнения",
                "Примечание"
            )
        }

        val factRows = mapTasksToRows(
            tasks = factTasks,
            includePhotos = includePhotos,
            sectionKind = ReportSectionKind.DAILY_FACT_REPORT,
            currentEpochDay = currentEpochDay
        )
        val planRows = mapTasksToRows(
            tasks = planTasks,
            includePhotos = includePhotos,
            sectionKind = ReportSectionKind.NEXT_DAY_PLAN,
            currentEpochDay = currentEpochDay
        )

        val factSection = ReportSectionBlock(
            kind = ReportSectionKind.DAILY_FACT_REPORT,
            bannerTitle = "ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ ($todayStr) — ФАКТИЧЕСКОЕ ВЫПОЛНЕНИЕ РАБОТ",
            bannerSubtitle = "Выполненные и выполняемые за смену ($todayStr) работы, фактический объём по чек-листам и статус приёмки",
            dateLabel = todayStr,
            emptyStateMessage = "За текущий день ($todayStr) задач в отчёте не числится.",
            headers = reportHeaders,
            rows = factRows
        )

        val planSection = ReportSectionBlock(
            kind = ReportSectionKind.NEXT_DAY_PLAN,
            bannerTitle = "ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ ($tomorrowStr) — ПЛАНОВОЕ ЗАДАНИЕ НА СМЕНУ",
            bannerSubtitle = "Новые задачи, назначенные на $tomorrowStr, и переходящие невыполненные объёмы с текущей смены ($todayStr)",
            dateLabel = tomorrowStr,
            emptyStateMessage = "На следующий день ($tomorrowStr) плановых или переходящих задач пока не назначено.",
            headers = planHeaders,
            rows = planRows
        )

        val sheets = if (layout == SheetLayout.SPLIT_SHEETS) {
            listOf(
                ReportSheetPreview(
                    sheetIndex = 1,
                    sheetTitle = "ЛИСТ 1 ИЗ 2 • ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ ($tomorrowStr)",
                    subtitle = "Отдельный лист плана работ на $tomorrowStr (плановые задания и переходящие остатки объёмов)",
                    headers = planHeaders,
                    rows = planRows,
                    sections = listOf(
                        planSection.copy(
                            bannerTitle = "ЛИСТ 1 (ОТДЕЛЬНЫЙ ЛИСТ ПЛАНА) • ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ ($tomorrowStr)"
                        )
                    )
                ),
                ReportSheetPreview(
                    sheetIndex = 2,
                    sheetTitle = "ЛИСТ 2 ИЗ 2 • ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ ($todayStr)",
                    subtitle = "Отдельный лист отчёта за $todayStr (фактически выполненные объёмы работ, чек-листы и фотофиксация)",
                    headers = reportHeaders,
                    rows = factRows,
                    sections = listOf(
                        factSection.copy(
                            bannerTitle = "ЛИСТ 2 (ОТДЕЛЬНЫЙ ЛИСТ ОТЧЁТА) • ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ ($todayStr)"
                        )
                    )
                )
            )
        } else {
            val allSingleSheetRows = factRows + planRows.mapIndexed { idx, r ->
                r.copy(index = idx + 1)
            }
            listOf(
                ReportSheetPreview(
                    sheetIndex = 1,
                    sheetTitle = "ЕДИНЫЙ ЛИСТ: 1) ОТЧЁТ ЗА ДЕНЬ ($todayStr) + 2) ПЛАН НА СЛЕДУЮЩИЙ ДЕНЬ ($tomorrowStr)",
                    subtitle = "Совмещённый документ на одном листе с двумя раздельными таблицами: Блок 1 — Отчёт за $todayStr, Блок 2 — План на $tomorrowStr",
                    headers = reportHeaders,
                    rows = allSingleSheetRows,
                    sections = listOf(
                        factSection.copy(
                            bannerTitle = "БЛОК 1 (ВЕРХНЯЯ ТАБЛИЦА) • ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ ($todayStr) — ФАКТ ВЫПОЛНЕНИЯ"
                        ),
                        planSection.copy(
                            bannerTitle = "БЛОК 2 (НИЖНЯЯ ТАБЛИЦА) • ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ ($tomorrowStr) — ПЛАНОВОЕ ЗАДАНИЕ"
                        )
                    )
                )
            )
        }

        val attachedPhotos = if (includePhotos) {
            roleScopedTasks.flatMap { t ->
                t.decodedPhotos().map { ph ->
                    ReportAttachedPhoto(
                        taskTitle = t.title,
                        assigneeFio = t.assigneeName,
                        photo = ph
                    )
                }
            }
        } else {
            emptyList()
        }

        val nowLabel = SimpleDateFormat("dd.MM.2026 HH:mm", Locale.forLanguageTag("ru")).format(Date())
        val periodLabel = "Отчёт за $todayStr / План на $tomorrowStr"

        val signatures = listOf(
            "Подготовил документ: ____________________ / ${actor.fullName} (${actor.role.titleRu}) /",
            "Проверил / Принял:   ____________________ / Подпись руководителя /"
        )

        val photoModeLabel = if (includePhotos) "С фотоотчётом (${attachedPhotos.size} фото)" else "Без фотографий (только таблица)"
        val layoutModeLabel = if (layout == SheetLayout.SPLIT_SHEETS) {
            "НА РАЗДЕЛЬНЫХ ЛИСТАХ (2 листа: Отчёт и План отдельно)"
        } else {
            "НА ОДНОМ ЛИСТЕ (Блок 1: Отчёт за $todayStr + Блок 2: План на $tomorrowStr)"
        }

        val summaryText = buildString {
            appendLine(template.orgHeader)
            appendLine("Документ: $resolvedReportTitle (${format.code})")
            appendLine("Компоновка: $layoutModeLabel • Режим фото: $photoModeLabel")
            appendLine("Период: $periodLabel • Охват: $roleScopeLabel")
            appendLine()
            appendLine("================ 1. ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ ($todayStr) — ФАКТ (${factRows.size} зад.) ================")
            if (factRows.isEmpty()) {
                appendLine("   (По отчёту за $todayStr записей нет)")
            } else {
                factRows.forEach { r ->
                    val phPart = if (includePhotos) ", фото: ${r.photoInfo}" else ""
                    appendLine("${r.index}. [${r.rowTypeBadge}] ${r.assigneeFio} — ${r.taskTitle} | Объём: ${r.volumeInfo} | Статус: ${r.statusLabel}$phPart")
                    r.checklistBreakdown.forEach { subLine ->
                        appendLine("   • Чек-лист: $subLine")
                    }
                }
            }
            appendLine()
            appendLine("================ 2. ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ ($tomorrowStr) — ПЛАН (${planRows.size} зад.) ================")
            if (planRows.isEmpty()) {
                appendLine("   (На следующий день $tomorrowStr плановых или переходящих задач нет)")
            } else {
                planRows.forEach { r ->
                    appendLine("${r.index}. [${r.rowTypeBadge}] ${r.assigneeFio} — ${r.taskTitle} | Плановый объём: ${r.planVolumeLabel} | Срок: ${r.deadlineLabel}")
                    r.checklistBreakdown.forEach { subLine ->
                        appendLine("   • Чек-лист: $subLine")
                    }
                }
            }
        }.trim()

        val dummyFile = File("preview_${template.id}.${format.extension}")
        return GeneratedDocumentResult(
            file = dummyFile,
            orgHeader = template.orgHeader,
            reportTitle = resolvedReportTitle,
            periodLabel = periodLabel,
            generatedAtLabel = nowLabel,
            preparedByLabel = "${actor.fullName} (${actor.position})",
            roleScopeLabel = roleScopeLabel,
            sheetLayout = layout,
            exportFormat = format,
            includePhotos = includePhotos,
            attachedPhotos = attachedPhotos,
            sheets = sheets,
            signatureBlock = signatures,
            summaryText = summaryText
        )
    }

    fun generateAndSaveDocument(
        context: Context,
        actor: CorporateEmployee,
        template: ReportTemplateDocument,
        allTasks: List<TaskDocument>,
        currentEpochDay: Long,
        overrideLayout: SheetLayout? = null,
        overrideFormat: ExportFormat? = null,
        includePhotos: Boolean = true,
        saveToDownloads: Boolean = false
    ): GeneratedDocumentResult {
        val preview = buildPreview(
            actor = actor,
            template = template,
            allTasks = allTasks,
            currentEpochDay = currentEpochDay,
            overrideLayout = overrideLayout,
            overrideFormat = overrideFormat,
            includePhotos = includePhotos
        )
        val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
        val safeName = template.parsedReportType.code.lowercase(Locale.US)
        val photoSuffix = if (includePhotos) "with_photos" else "no_photos"
        val layoutSuffix = if (preview.sheetLayout == SheetLayout.SPLIT_SHEETS) "2sheets" else "1sheet"
        val fileName = "ezhednevnik_${safeName}_${layoutSuffix}_${photoSuffix}_${System.currentTimeMillis()}.${preview.exportFormat.extension}"
        val outFile = File(reportsDir, fileName)

        when (preview.exportFormat) {
            ExportFormat.PDF -> writePdfFile(outFile, preview)
            ExportFormat.XLSX -> writeXlsxFile(outFile, preview)
            ExportFormat.DOCX -> writeDocxFile(outFile, preview)
            ExportFormat.CSV -> writeCsvFile(outFile, preview)
        }

        val withLocalFile = preview.copy(file = outFile)
        return if (saveToDownloads) {
            val savedDownloadLocation = saveFileToPhoneDownloads(
                context = context,
                sourceFile = outFile,
                mimeType = preview.exportFormat.mimeType,
                showToast = true
            ).getOrDefault("Загрузки/${outFile.name}")
            withLocalFile.copy(downloadedToPath = savedDownloadLocation)
        } else {
            withLocalFile
        }
    }

    /**
     * Сохраняет сформированный файл отчёта непосредственно в системную папку «Загрузки» (Downloads) телефона
     * через MediaStore.Downloads (Android 10+) или Environment.DIRECTORY_DOWNLOADS.
     */
    fun saveFileToPhoneDownloads(
        context: Context,
        sourceFile: File,
        mimeType: String,
        showToast: Boolean = true
    ): Result<String> = runCatching {
        require(sourceFile.exists() && sourceFile.length() > 0L) {
            "Исходный файл отчёта не найден"
        }
        val displayLocation: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Не удалось создать запись в папке Загрузки")
            resolver.openOutputStream(uri)?.use { outStream ->
                sourceFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            } ?: throw IllegalStateException("Не удалось записать данные в папку Загрузки")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Загрузки/${sourceFile.name}"
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .apply { mkdirs() }
            val targetFile = File(downloadsDir, sourceFile.name)
            sourceFile.copyTo(targetFile, overwrite = true)
            targetFile.absolutePath
        }

        if (showToast) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Скачано в папку «Загрузки»: ${sourceFile.name}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        displayLocation
    }

    fun saveReportToPhoneDownloads(
        context: Context,
        result: GeneratedDocumentResult,
        showToast: Boolean = true
    ): Result<String> = saveFileToPhoneDownloads(
        context = context,
        sourceFile = result.file,
        mimeType = result.exportFormat.mimeType,
        showToast = showToast
    )

    fun openGeneratedReport(
        context: Context,
        result: GeneratedDocumentResult
    ) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                result.file
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, result.exportFormat.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(viewIntent, "Открыть отчёт (${result.exportFormat.code})").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }.onFailure {
            shareGeneratedReport(context, result, "SHARE")
        }
    }

    fun shareGeneratedReport(
        context: Context,
        result: GeneratedDocumentResult,
        channelHint: String = "SHARE"
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            result.file
        )
        val subject = "${result.reportTitle} (${result.periodLabel})"
        val body = buildString {
            appendLine(result.orgHeader)
            appendLine(subject)
            appendLine("Подготовил: ${result.preparedByLabel}")
            appendLine("Формат: ${result.exportFormat.code} (${result.sheetLayout.shortRu}, ${if (result.includePhotos) "С фото" else "Без фото"})")
            appendLine()
            appendLine(result.summaryText)
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = result.exportFormat.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserTitle = when (channelHint) {
            "EMAIL" -> "Отправить отчёт по Email"
            "MESSENGER" -> "Отправить отчёт в мессенджер"
            else -> "Поделиться отчётным документом"
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun mapTasksToRows(
        tasks: List<TaskDocument>,
        includePhotos: Boolean,
        sectionKind: ReportSectionKind,
        currentEpochDay: Long
    ): List<ReportTableRow> {
        val sorted = tasks.sortedWith(
            compareBy<TaskDocument> { it.assigneeName }
                .thenBy { it.dueDateEpochDay }
        )
        return sorted.mapIndexed { idx, task ->
            val photoText = if (!includePhotos) {
                "Скрыто (без фото)"
            } else if (task.requiresPhotoReport) {
                "${task.photoItems.size}/${task.minPhotoCount} фото"
            } else {
                "${task.photoItems.size} (не обяз.)"
            }

            val isCarryoverFromToday = sectionKind == ReportSectionKind.NEXT_DAY_PLAN && task.dueDateEpochDay <= currentEpochDay
            val rowBadge = when {
                sectionKind == ReportSectionKind.DAILY_FACT_REPORT -> "ОТЧЁТ (ФАКТ)"
                isCarryoverFromToday -> "ПЕРЕНОС В ПЛАН"
                else -> "ПЛАН НА СМЕНУ"
            }

            val carryoverText = when {
                isCarryoverFromToday -> "Перенос остатка с ${formatEpochDay(task.dueDateEpochDay)}"
                task.carryoverCount > 0L -> "Перенос ×${task.carryoverCount} (с ${formatEpochDay(task.originalDueDateEpochDay)})"
                sectionKind == ReportSectionKind.NEXT_DAY_PLAN -> "Новое плановое задание"
                else -> "В срок"
            }

            val targetVol = task.effectiveTargetVolume
            val completedVol = task.effectiveCompletedVolume
            val remainingVol = (targetVol - completedVol).coerceAtLeast(0.0)
            val unit = task.effectiveVolumeUnit.ifBlank { "шт" }
            val pct = task.completionPercent()

            val planVolString = if (targetVol > 0.0) {
                if (isCarryoverFromToday && remainingVol > 0.0) {
                    "Остаток: ${SubtaskItem.formatVolumeNumber(remainingVol)} из ${SubtaskItem.formatVolumeNumber(targetVol)} $unit"
                } else {
                    "План: ${SubtaskItem.formatVolumeNumber(targetVol)} $unit"
                }
            } else {
                "По чек-листу"
            }

            val factVolString = task.formatVolumeSummaryForReport()
            val volumeSummary = if (sectionKind == ReportSectionKind.NEXT_DAY_PLAN) {
                "$planVolString (вып. $pct%)"
            } else {
                factVolString
            }

            val checklistDetails = task.formatChecklistBreakdownForReport()
            val statusWithPct = when {
                sectionKind == ReportSectionKind.NEXT_DAY_PLAN && isCarryoverFromToday -> "Переходящая ($pct%)"
                sectionKind == ReportSectionKind.NEXT_DAY_PLAN -> "Запланировано (${formatEpochDay(task.dueDateEpochDay)})"
                task.hasVolumeTracking() && pct < 100 && task.taskStatus != TaskStatus.APPROVED -> "${task.taskStatus.titleRu} ($pct%)"
                else -> task.taskStatus.titleRu
            }

            val note = when {
                !task.rejectionReason.isNullOrBlank() -> "Возврат: ${task.rejectionReason}"
                !task.approvedByName.isNullOrBlank() -> "Принял: ${task.approvedByName}"
                task.decodedComments().isNotEmpty() -> task.decodedComments().last().text
                else -> task.geoLocation ?: "Без замечаний"
            }

            ReportTableRow(
                index = idx + 1,
                sectionKind = sectionKind,
                rowTypeBadge = rowBadge,
                assigneeFio = task.assigneeName,
                groupName = task.groupName,
                taskTitle = task.title,
                volumeInfo = volumeSummary,
                planVolumeLabel = planVolString,
                factVolumeLabel = factVolString,
                checklistBreakdown = checklistDetails,
                completionPercent = pct,
                priorityLabel = task.taskPriority.titleRu,
                statusLabel = statusWithPct,
                deadlineLabel = "${formatEpochDay(task.dueDateEpochDay)} ${task.deadlineTime}",
                photoInfo = photoText,
                carryoverInfo = carryoverText,
                commentSummary = note
            )
        }
    }

    // =========================================================================
    // 1. PDF WRITER (Явное визуальное разделение ОТЧЁТА ЗА ДЕНЬ и ПЛАНА НА СЛЕДУЮЩИЙ ДЕНЬ
    //    как на раздельных листах, так и на одном совмещённом листе)
    // =========================================================================
    private fun writePdfFile(outFile: File, doc: GeneratedDocumentResult) {
        val pdf = PdfDocument()
        val pageWidth = 842 // A4 Landscape width in points
        val pageHeight = 595 // A4 Landscape height in points

        val headerBoxBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        val headerBoxBorderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val orgTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11.5f
            isFakeBoldText = true
        }
        val docModePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 64, 175)
            textSize = 10f
            isFakeBoldText = true
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8.2f
        }

        // Цвета для БЛОКА ОТЧЁТА ЗА ТЕКУЩИЙ ДЕНЬ (Изумрудно-зелёный акцент факта)
        val reportBannerBgPaint = Paint().apply {
            color = Color.rgb(15, 118, 110) // Deep Teal-Emerald
            style = Paint.Style.FILL
        }
        val reportSubHeaderBgPaint = Paint().apply {
            color = Color.rgb(204, 251, 241)
            style = Paint.Style.FILL
        }
        val reportSubHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 94, 89)
            textSize = 7.8f
            isFakeBoldText = true
        }
        val reportBadgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 118, 110)
            textSize = 7.4f
            isFakeBoldText = true
        }

        // Цвета для БЛОКА ПЛАНА РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ (Сине-индиго акцент плана)
        val planBannerBgPaint = Paint().apply {
            color = Color.rgb(30, 64, 175) // Royal Indigo Blue
            style = Paint.Style.FILL
        }
        val planSubHeaderBgPaint = Paint().apply {
            color = Color.rgb(219, 234, 254)
            style = Paint.Style.FILL
        }
        val planSubHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 7.8f
            isFakeBoldText = true
        }
        val planBadgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 64, 175)
            textSize = 7.4f
            isFakeBoldText = true
        }

        val bannerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.8f
            isFakeBoldText = true
        }
        val bannerSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 7.5f
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 7.8f
        }
        val cellBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 7.8f
            isFakeBoldText = true
        }
        val checklistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 7.3f
        }
        val zebraRowPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.7f
        }
        val dividerBannerPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        var pageNumber = 1

        // В режиме SPLIT_SHEETS выводим сначала Лист 1 (Отчёт за текущий день), затем Лист 2 (План на следующий день),
        // чтобы в PDF порядок страниц был максимально логичным и наглядным.
        val pdfSheetsOrder = if (doc.sheetLayout == SheetLayout.SPLIT_SHEETS && doc.sheets.size == 2) {
            val reportSheet = doc.sheets.firstOrNull {
                it.sections.firstOrNull()?.kind == ReportSectionKind.DAILY_FACT_REPORT
            }
            val planSheet = doc.sheets.firstOrNull {
                it.sections.firstOrNull()?.kind == ReportSectionKind.NEXT_DAY_PLAN
            }
            if (reportSheet != null && planSheet != null) {
                listOf(
                    reportSheet.copy(
                        sheetIndex = 1,
                        sheetTitle = "ЛИСТ 1 ИЗ 2 • ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ (ФАКТИЧЕСКОЕ ВЫПОЛНЕНИЕ)",
                        sections = reportSheet.sections.map {
                            it.copy(bannerTitle = "ЛИСТ 1 ИЗ 2 (ОТДЕЛЬНЫЙ ЛИСТ ОТЧЁТА) • ${it.bannerTitle.substringAfter("• ").trim()}")
                        }
                    ),
                    planSheet.copy(
                        sheetIndex = 2,
                        sheetTitle = "ЛИСТ 2 ИЗ 2 • ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ (ПЛАНОВОЕ ЗАДАНИЕ)",
                        sections = planSheet.sections.map {
                            it.copy(bannerTitle = "ЛИСТ 2 ИЗ 2 (ОТДЕЛЬНЫЙ ЛИСТ ПЛАНА) • ${it.bannerTitle.substringAfter("• ").trim()}")
                        }
                    )
                )
            } else {
                doc.sheets
            }
        } else {
            doc.sheets
        }

        val colX = if (doc.includePhotos) {
            floatArrayOf(30f, 48f, 128f, 228f, 385f, 555f, 635f, 700f, 755f)
        } else {
            floatArrayOf(30f, 48f, 132f, 240f, 415f, 590f, 670f, 735f)
        }

        fun drawTopDocumentHeader(canvas: Canvas, sheetTitleText: String, isContinuation: Boolean): Float {
            val boxRect = RectF(26f, 18f, pageWidth - 26f, 68f)
            canvas.drawRoundRect(boxRect, 6f, 6f, headerBoxBgPaint)
            canvas.drawRoundRect(boxRect, 6f, 6f, headerBoxBorderPaint)

            val contSuffix = if (isContinuation) " (продолжение)" else ""
            canvas.drawText("${doc.orgHeader}$contSuffix", 34f, 33f, orgTitlePaint)

            val layoutDescription = if (doc.sheetLayout == SheetLayout.SPLIT_SHEETS) {
                "РЕЖИМ ВЫГРУЗКИ: НА РАЗДЕЛЬНЫХ ЛИСТАХ (2 ЛИСТА)  —  $sheetTitleText"
            } else {
                "РЕЖИМ ВЫГРУЗКИ: НА ОДНОМ ЛИСТЕ (БЛОК 1: ОТЧЁТ ЗА ДЕНЬ  +  БЛОК 2: ПЛАН НА СЛЕДУЮЩИЙ ДЕНЬ)"
            }
            canvas.drawText(layoutDescription, 34f, 48f, docModePaint)

            val photoBadge = if (doc.includePhotos) "Фотоотчёт: Включён" else "Фотоотчёт: Без фото"
            canvas.drawText(
                "Период: ${doc.periodLabel}   |   Сформирован: ${doc.generatedAtLabel}   |   $photoBadge   |   Подготовил: ${doc.preparedByLabel}",
                34f,
                61f,
                metaPaint
            )
            return 76f
        }

        fun drawSectionHeaderAndColumns(canvas: Canvas, section: ReportSectionBlock, startY: Float): Float {
            var y = startY
            val isPlan = section.kind == ReportSectionKind.NEXT_DAY_PLAN
            val bannerPaint = if (isPlan) planBannerBgPaint else reportBannerBgPaint
            val subHeaderBg = if (isPlan) planSubHeaderBgPaint else reportSubHeaderBgPaint
            val subHeaderText = if (isPlan) planSubHeaderTextPaint else reportSubHeaderTextPaint

            // 1. Крупная цветная плашка заголовка раздела (Отчёт vs План)
            val bannerRect = RectF(26f, y, pageWidth - 26f, y + 28f)
            canvas.drawRoundRect(bannerRect, 4f, 4f, bannerPaint)
            canvas.drawText(
                "${section.bannerTitle}   [Всего позиций: ${section.rows.size}]",
                34f,
                y + 13f,
                bannerTitlePaint
            )
            canvas.drawText(
                section.bannerSubtitle,
                34f,
                y + 24f,
                bannerSubtitlePaint
            )
            y += 30f

            // 2. Шапка колонок таблицы раздела
            canvas.drawRect(26f, y, pageWidth - 26f, y + 18f, subHeaderBg)
            canvas.drawRect(26f, y, pageWidth - 26f, y + 18f, headerBoxBorderPaint)

            val shortHeaders = if (isPlan) {
                if (doc.includePhotos) {
                    listOf("№", "Раздел", "Исполнитель", "Планируемые работы (План)", "Плановый объём на смену", "Статус / Тип", "Срок", "Фото", "Примечание")
                } else {
                    listOf("№", "Раздел", "Исполнитель", "Планируемые работы (План)", "Плановый объём на смену", "Статус / Тип", "Срок", "Примечание")
                }
            } else {
                if (doc.includePhotos) {
                    listOf("№", "Раздел", "Исполнитель", "Выполненные работы (Отчёт)", "Факт объёма из Плана (%)", "Статус", "Срок", "Фото", "Контроль")
                } else {
                    listOf("№", "Раздел", "Исполнитель", "Выполненные работы (Отчёт)", "Факт объёма из Плана (%)", "Статус", "Срок", "Контроль")
                }
            }

            shortHeaders.forEachIndexed { cIdx, hText ->
                if (cIdx < colX.size) {
                    canvas.drawText(hText, colX[cIdx], y + 12.5f, subHeaderText)
                }
            }
            return y + 20f
        }

        pdfSheetsOrder.forEach { sheet ->
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            var page = pdf.startPage(pageInfo)
            var canvas: Canvas = page.canvas
            var y = drawTopDocumentHeader(canvas, sheet.sheetTitle, isContinuation = false)

            val sectionsToRender = sheet.sections.ifEmpty {
                listOf(
                    ReportSectionBlock(
                        kind = ReportSectionKind.DAILY_FACT_REPORT,
                        bannerTitle = sheet.sheetTitle,
                        bannerSubtitle = sheet.subtitle,
                        dateLabel = doc.periodLabel,
                        emptyStateMessage = "Записей нет",
                        headers = sheet.headers,
                        rows = sheet.rows
                    )
                )
            }

            sectionsToRender.forEachIndexed { secIdx, section ->
                // Если на едином листе начинается второй блок (План на следующий день), рисуем чёткий визуальный разделитель
                if (secIdx > 0) {
                    if (y > pageHeight - 155f) {
                        pdf.finishPage(page)
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                        page = pdf.startPage(pageInfo)
                        canvas = page.canvas
                        y = drawTopDocumentHeader(canvas, sheet.sheetTitle, isContinuation = true)
                    } else {
                        y += 6f
                        canvas.drawRect(26f, y, pageWidth - 26f, y + 14f, dividerBannerPaint)
                        canvas.drawText(
                            "▼ ВТОРОЙ РАЗДЕЛ НА ЭТОМ ЖЕ ЛИСТЕ: ПЕРЕХОД ОТ ОТЧЁТА ЗА ТЕКУЩИЙ ДЕНЬ К ПЛАНУ РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ ▼",
                            140f,
                            y + 10f,
                            docModePaint
                        )
                        y += 18f
                    }
                }

                y = drawSectionHeaderAndColumns(canvas, section, y)
                val isPlan = section.kind == ReportSectionKind.NEXT_DAY_PLAN
                val badgePaint = if (isPlan) planBadgeTextPaint else reportBadgeTextPaint

                if (section.rows.isEmpty()) {
                    canvas.drawRect(26f, y, pageWidth - 26f, y + 20f, zebraRowPaint)
                    canvas.drawRect(26f, y, pageWidth - 26f, y + 20f, linePaint)
                    canvas.drawText(section.emptyStateMessage, 36f, y + 13f, metaPaint)
                    y += 24f
                } else {
                    section.rows.forEachIndexed { rIdx, row ->
                        val neededHeight = 16f + (row.checklistBreakdown.take(4).size * 11f)
                        if (y + neededHeight > pageHeight - 62f) {
                            pdf.finishPage(page)
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                            page = pdf.startPage(pageInfo)
                            canvas = page.canvas
                            y = drawTopDocumentHeader(canvas, sheet.sheetTitle, isContinuation = true)
                            y = drawSectionHeaderAndColumns(canvas, section, y)
                        }

                        if (rIdx % 2 == 1) {
                            canvas.drawRect(26f, y - 2f, pageWidth - 26f, y + neededHeight, zebraRowPaint)
                        }

                        val volumeCellText = if (isPlan) {
                            row.planVolumeLabel.ifBlank { row.volumeInfo }
                        } else {
                            row.factVolumeLabel.ifBlank { row.volumeInfo }
                        }

                        val cells = if (doc.includePhotos) {
                            listOf(
                                row.index.toString(),
                                row.rowTypeBadge.take(15),
                                row.assigneeFio.take(17),
                                row.taskTitle.take(27),
                                volumeCellText.take(32),
                                row.statusLabel.take(15),
                                row.deadlineLabel.take(10),
                                row.photoInfo.take(10),
                                row.commentSummary.take(14)
                            )
                        } else {
                            listOf(
                                row.index.toString(),
                                row.rowTypeBadge.take(15),
                                row.assigneeFio.take(18),
                                row.taskTitle.take(30),
                                volumeCellText.take(35),
                                row.statusLabel.take(15),
                                row.deadlineLabel.take(10),
                                row.commentSummary.take(16)
                            )
                        }

                        cells.forEachIndexed { cIdx, text ->
                            if (cIdx < colX.size) {
                                val paintToUse = when (cIdx) {
                                    1 -> badgePaint
                                    4 -> cellBoldPaint
                                    else -> cellPaint
                                }
                                canvas.drawText(text, colX[cIdx], y + 10f, paintToUse)
                            }
                        }
                        y += 13f

                        row.checklistBreakdown.take(4).forEach { subLine ->
                            val prefix = if (isPlan) "   ↳ Пункт плана по чек-листу: " else "   ↳ Выполнение по чек-листу: "
                            canvas.drawText("$prefix${subLine.take(92)}", colX[3], y + 8.5f, checklistPaint)
                            y += 11f
                        }

                        y += 2f
                        canvas.drawLine(26f, y, pageWidth - 26f, y, linePaint)
                    }
                }

                // Итоговая строка по конкретному разделу (Отчёт или План)
                val summaryStripText = if (isPlan) {
                    "ИТОГО ПО РАЗДЕЛУ «ПЛАН РАБОТ НА ${section.dateLabel}»: запланировано задач — ${section.rows.size}"
                } else {
                    val done100 = section.rows.count { it.completionPercent >= 100 }
                    "ИТОГО ПО РАЗДЕЛУ «ОТЧЁТ ЗА ${section.dateLabel}»: всего задач — ${section.rows.size}, выполнено на 100% — $done100, в работе/частично — ${section.rows.size - done100}"
                }
                canvas.drawText(summaryStripText, 32f, y + 11f, if (isPlan) planSubHeaderTextPaint else reportSubHeaderTextPaint)
                y += 16f
            }

            // Нижний колонтитул с подписями
            var sigY = (pageHeight - 36f).coerceAtLeast(y + 10f)
            if (sigY <= pageHeight - 14f) {
                doc.signatureBlock.forEach { sig ->
                    if (sigY <= pageHeight - 10f) {
                        canvas.drawText(sig, 28f, sigY, metaPaint)
                        sigY += 12f
                    }
                }
            }

            pdf.finishPage(page)
        }

        // Приложение с фотоотчётами (на отдельном листе после таблиц)
        if (doc.includePhotos && doc.attachedPhotos.isNotEmpty()) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            var y = 36f
            canvas.drawText("${doc.reportTitle} — Приложение: Фотоотчёты (${doc.attachedPhotos.size} шт.)", 28f, y, orgTitlePaint)
            y += 20f

            var col = 0
            val cardW = 245f
            val cardH = 190f
            doc.attachedPhotos.take(6).forEachIndexed { idx, item ->
                val x = 28f + col * (cardW + 18f)
                val boxRect = RectF(x, y, x + cardW, y + cardH)
                canvas.drawRoundRect(boxRect, 8f, 8f, linePaint)

                val bmp = PhotoCaptureUtils.loadBitmapFromUriOrPath(item.photo.uriString)
                if (bmp != null) {
                    val dstRect = RectF(x + 6f, y + 6f, x + cardW - 6f, y + 125f)
                    canvas.drawBitmap(bmp, null, dstRect, null)
                }
                canvas.drawText("${idx + 1}. ${item.taskTitle.take(28)}", x + 8f, y + 142f, cellPaint)
                canvas.drawText("${item.assigneeFio.take(22)} • ${item.photo.capturedAtText}", x + 8f, y + 156f, metaPaint)
                canvas.drawText(item.photo.comment.take(32), x + 8f, y + 170f, metaPaint)

                col++
                if (col >= 3) {
                    col = 0
                    y += cardH + 16f
                }
            }

            pdf.finishPage(page)
        }

        FileOutputStream(outFile).use { fos ->
            pdf.writeTo(fos)
        }
        pdf.close()
    }

    // --- 2. CSV WRITER (UTF-8 with BOM for Excel compatibility) ---
    private fun writeCsvFile(outFile: File, doc: GeneratedDocumentResult) {
        val sb = StringBuilder()
        sb.append('\uFEFF') // UTF-8 BOM
        sb.appendLine("Организация;${escapeCsv(doc.orgHeader)}")
        sb.appendLine("Отчёт;${escapeCsv(doc.reportTitle)};Компоновка;${escapeCsv(doc.sheetLayout.titleRu)}")
        sb.appendLine("Период;${escapeCsv(doc.periodLabel)};Дата формирования;${escapeCsv(doc.generatedAtLabel)}")
        sb.appendLine("Подготовил;${escapeCsv(doc.preparedByLabel)};Фотоотчёт;${escapeCsv(if (doc.includePhotos) "Включён" else "Без фото")}")
        sb.appendLine()

        doc.sheets.forEach { sheet ->
            sb.appendLine("=== ${escapeCsv(sheet.sheetTitle)} ===")
            val sections = sheet.sections.ifEmpty {
                listOf(
                    ReportSectionBlock(
                        kind = ReportSectionKind.DAILY_FACT_REPORT,
                        bannerTitle = sheet.sheetTitle,
                        bannerSubtitle = sheet.subtitle,
                        dateLabel = doc.periodLabel,
                        emptyStateMessage = "Записей нет",
                        headers = sheet.headers,
                        rows = sheet.rows
                    )
                )
            }
            sections.forEach { sec ->
                sb.appendLine(">>> ${escapeCsv(sec.bannerTitle)} <<<")
                sb.appendLine(sec.headers.joinToString(";") { escapeCsv(it) })
                if (sec.rows.isEmpty()) {
                    sb.appendLine(escapeCsv(sec.emptyStateMessage))
                } else {
                    sec.rows.forEach { row ->
                        val cells = if (doc.includePhotos) {
                            listOf(
                                row.index.toString(),
                                row.rowTypeBadge,
                                row.assigneeFio,
                                row.groupName,
                                row.taskTitle,
                                row.volumeInfo,
                                row.priorityLabel,
                                row.statusLabel,
                                row.deadlineLabel,
                                row.photoInfo,
                                row.commentSummary
                            )
                        } else {
                            listOf(
                                row.index.toString(),
                                row.rowTypeBadge,
                                row.assigneeFio,
                                row.groupName,
                                row.taskTitle,
                                row.volumeInfo,
                                row.priorityLabel,
                                row.statusLabel,
                                row.deadlineLabel,
                                row.commentSummary
                            )
                        }
                        sb.appendLine(cells.joinToString(";") { escapeCsv(it) })
                        row.checklistBreakdown.forEach { subLine ->
                            sb.appendLine(";;;;\"Пункт чек-листа\";${escapeCsv(subLine)}")
                        }
                    }
                }
                sb.appendLine()
            }
        }

        doc.signatureBlock.forEach { sig ->
            sb.appendLine(escapeCsv(sig))
        }
        outFile.writeText(sb.toString(), Charsets.UTF_8)
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"").replace("\n", " ")
        return "\"$clean\""
    }

    // --- 3. XLSX WRITER (OpenXML SpreadsheetML with explicit section blocks) ---
    private fun writeXlsxFile(outFile: File, doc: GeneratedDocumentResult) {
        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            val sheetOverrides = doc.sheets.mapIndexed { idx, _ ->
                """<Override PartName="/xl/worksheets/sheet${idx + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
            }.joinToString("\n")
            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  $sheetOverrides
</Types>""".toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            val sheetsXml = doc.sheets.mapIndexed { idx, _ ->
                val shortSheetName = if (doc.sheets.size > 1) {
                    if (idx == 0) "Лист 1 - План работ" else "Лист 2 - Отчёт за день"
                } else {
                    "Отчёт за день и План"
                }
                """<sheet name="${escapeXml(shortSheetName)}" sheetId="${idx + 1}" r:id="rId${idx + 1}"/>"""
            }.joinToString("\n    ")
            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    $sheetsXml
  </sheets>
</workbook>""".toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            val relsXml = doc.sheets.mapIndexed { idx, _ ->
                """<Relationship Id="rId${idx + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${idx + 1}.xml"/>"""
            }.joinToString("\n  ")
            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  $relsXml
</Relationships>""".toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()

            doc.sheets.forEachIndexed { idx, sheet ->
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet${idx + 1}.xml"))
                val rowsXml = StringBuilder()
                var rNum = 1
                fun appendStringRow(cells: List<String>) {
                    rowsXml.append("""<row r="$rNum">""")
                    cells.forEach { text ->
                        rowsXml.append("""<c t="inlineStr"><is><t>${escapeXml(text)}</t></is></c>""")
                    }
                    rowsXml.append("</row>\n")
                    rNum++
                }

                appendStringRow(listOf(doc.orgHeader))
                appendStringRow(
                    listOf(
                        sheet.sheetTitle,
                        "Период: ${doc.periodLabel}",
                        "Подготовил: ${doc.preparedByLabel}",
                        if (doc.includePhotos) "С фотоотчётом" else "Без фотографий"
                    )
                )

                val sections = sheet.sections.ifEmpty {
                    listOf(
                        ReportSectionBlock(
                            kind = ReportSectionKind.DAILY_FACT_REPORT,
                            bannerTitle = sheet.sheetTitle,
                            bannerSubtitle = sheet.subtitle,
                            dateLabel = doc.periodLabel,
                            emptyStateMessage = "Записей нет",
                            headers = sheet.headers,
                            rows = sheet.rows
                        )
                    )
                }

                sections.forEach { sec ->
                    appendStringRow(listOf("=== ${sec.bannerTitle} ==="))
                    appendStringRow(listOf(sec.bannerSubtitle))
                    appendStringRow(sec.headers)
                    if (sec.rows.isEmpty()) {
                        appendStringRow(listOf(sec.emptyStateMessage))
                    } else {
                        sec.rows.forEach { r ->
                            val rowCells = if (doc.includePhotos) {
                                listOf(
                                    r.index.toString(),
                                    r.rowTypeBadge,
                                    r.assigneeFio,
                                    r.groupName,
                                    r.taskTitle,
                                    r.volumeInfo,
                                    r.priorityLabel,
                                    r.statusLabel,
                                    r.deadlineLabel,
                                    r.photoInfo,
                                    r.commentSummary
                                )
                            } else {
                                listOf(
                                    r.index.toString(),
                                    r.rowTypeBadge,
                                    r.assigneeFio,
                                    r.groupName,
                                    r.taskTitle,
                                    r.volumeInfo,
                                    r.priorityLabel,
                                    r.statusLabel,
                                    r.deadlineLabel,
                                    r.commentSummary
                                )
                            }
                            appendStringRow(rowCells)
                            r.checklistBreakdown.forEach { subLine ->
                                appendStringRow(listOf("", r.rowTypeBadge, r.assigneeFio, r.groupName, "   ↳ Пункт чек-листа", subLine))
                            }
                        }
                    }
                    appendStringRow(listOf(""))
                }

                doc.signatureBlock.forEach { sig ->
                    appendStringRow(listOf(sig))
                }

                zip.write(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    $rowsXml
  </sheetData>
</worksheet>""".toByteArray(Charsets.UTF_8)
                )
                zip.closeEntry()
            }
        }
    }

    // --- 4. DOCX WRITER (OpenXML WordprocessingML with explicit section blocks) ---
    private fun writeDocxFile(outFile: File, doc: GeneratedDocumentResult) {
        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("word/document.xml"))
            val bodyXml = StringBuilder()
            fun appendParagraph(text: String, pageBreakBefore: Boolean = false) {
                bodyXml.append("<w:p><w:r>")
                if (pageBreakBefore) bodyXml.append("""<w:br w:type="page"/>""")
                bodyXml.append("<w:t>${escapeXml(text)}</w:t></w:r></w:p>\n")
            }

            doc.sheets.forEachIndexed { idx, sheet ->
                appendParagraph(doc.orgHeader, pageBreakBefore = idx > 0)
                appendParagraph("${doc.reportTitle} — ${sheet.sheetTitle}")
                appendParagraph("Период: ${doc.periodLabel} | Сформирован: ${doc.generatedAtLabel} | Подготовил: ${doc.preparedByLabel}")
                appendParagraph("Охват: ${doc.roleScopeLabel} | Фотоотчёт: ${if (doc.includePhotos) "Включён" else "Без фото"}")

                val sections = sheet.sections.ifEmpty {
                    listOf(
                        ReportSectionBlock(
                            kind = ReportSectionKind.DAILY_FACT_REPORT,
                            bannerTitle = sheet.sheetTitle,
                            bannerSubtitle = sheet.subtitle,
                            dateLabel = doc.periodLabel,
                            emptyStateMessage = "Записей нет",
                            headers = sheet.headers,
                            rows = sheet.rows
                        )
                    )
                }

                sections.forEach { sec ->
                    appendParagraph("======================================================================")
                    appendParagraph(sec.bannerTitle)
                    appendParagraph(sec.bannerSubtitle)
                    if (sec.rows.isEmpty()) {
                        appendParagraph("   ${sec.emptyStateMessage}")
                    } else {
                        sec.rows.forEach { r ->
                            val photoSegment = if (doc.includePhotos) " | Фото: ${r.photoInfo}" else ""
                            appendParagraph(
                                "${r.index}. [${r.rowTypeBadge}] ${r.assigneeFio} (${r.groupName}) — ${r.taskTitle} | Объём: ${r.volumeInfo} | Статус: ${r.statusLabel} | Срок: ${r.deadlineLabel}$photoSegment | ${r.carryoverInfo}"
                            )
                            r.checklistBreakdown.forEach { subLine ->
                                appendParagraph("   • Чек-лист: $subLine")
                            }
                        }
                    }
                }

                if (doc.includePhotos && doc.attachedPhotos.isNotEmpty()) {
                    appendParagraph("--- Приложенные фотоотчёты (${doc.attachedPhotos.size} шт.) ---")
                    doc.attachedPhotos.forEachIndexed { pIdx, ap ->
                        appendParagraph("Фото #${pIdx + 1}: ${ap.taskTitle} (${ap.assigneeFio}) — ${ap.photo.capturedAtText}, ${ap.photo.gpsCoords}, комм.: ${ap.photo.comment}")
                    }
                }
                doc.signatureBlock.forEach { sig ->
                    appendParagraph(sig)
                }
            }

            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    $bodyXml
  </w:body>
</w:document>""".toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()
        }
    }

    private fun escapeXml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
