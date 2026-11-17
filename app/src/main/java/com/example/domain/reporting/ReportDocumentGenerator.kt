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
import com.example.data.model.TaskPriority
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

enum class ReportContentScope(
    val code: String,
    val titleRu: String
) {
    REPORT_ONLY("REPORT_ONLY", "Только отчёт о выполненных работах"),
    PLAN_ONLY("PLAN_ONLY", "Только план работ"),
    COMBINED_REPORT_AND_PLAN("COMBINED_REPORT_AND_PLAN", "Отчёт за текущий день и план работ на следующий день (на 1 странице)")
}

enum class ReportPeriodMode(
    val code: String,
    val titleRu: String
) {
    CURRENT_OR_NEXT_DAY("CURRENT_OR_NEXT_DAY", "За текущий / следующий день"),
    SINGLE_DATE("SINGLE_DATE", "На указанную дату"),
    DATE_RANGE("DATE_RANGE", "За указанный период")
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

data class AspectFitRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class TaskPhotoPdfResult(
    val file: File,
    val downloadedToPath: String?,
    val photoCount: Int,
    val pageCount: Int
)

object ReportDocumentGenerator {

    private val RUSSIAN_PHOTO_DATE_REGEX = Regex("""^(\d{2})\.(\d{2})(?:\.(\d{4}))?(?:\s+.*)?$""")

    fun parseRussianPhotoEpochDay(capturedAtText: String, referenceYear: Int): Long? {
        val clean = capturedAtText.trim()
        if (clean.isBlank()) return null
        val match = RUSSIAN_PHOTO_DATE_REGEX.find(clean) ?: return null
        val day = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val year = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: referenceYear
        return runCatching {
            LocalDate.of(year, month, day).toEpochDay()
        }.getOrNull()
    }

    fun photoMatchesEpochDayRange(
        photo: PhotoReportItem,
        taskDueDateEpochDay: Long,
        startEpochDay: Long,
        endEpochDay: Long
    ): Boolean {
        val refYear = runCatching { LocalDate.ofEpochDay(endEpochDay).year }.getOrDefault(2026)
        val parsedPhotoDay = parseRussianPhotoEpochDay(photo.capturedAtText, refYear)
        return if (parsedPhotoDay != null) {
            parsedPhotoDay in startEpochDay..endEpochDay
        } else {
            taskDueDateEpochDay in startEpochDay..endEpochDay
        }
    }

    fun formatOrgHeader(
        organizationName: String,
        subdivisionName: String = "",
        siteName: String = ""
    ): String {
        val cleanSub = com.example.data.seed.CorporateSeedData.sanitizeSubdivisionName(subdivisionName)
        val cleanSite = com.example.data.seed.CorporateSeedData.sanitizeSiteOrLocation(siteName, fallbackSubdivision = cleanSub)
        val parts = mutableListOf<String>()
        listOf(organizationName, cleanSub, cleanSite).forEach { raw ->
            val clean = raw.trim()
            if (clean.isNotBlank() && parts.none { it.equals(clean, ignoreCase = true) }) {
                parts.add(clean)
            }
        }
        return parts.joinToString(", ")
    }

    fun formatPreparedBySignature(
        actor: CorporateEmployee,
        defaultSiteName: String = ""
    ): String {
        val positionText = com.example.data.seed.CorporateSeedData.sanitizePositionName(
            raw = actor.position,
            role = actor.role
        )
        val siteText = com.example.data.seed.CorporateSeedData.sanitizeSiteOrLocation(
            raw = actor.siteName.ifBlank { defaultSiteName },
            fallbackSite = defaultSiteName,
            fallbackSubdivision = actor.groupName
        )
        val siteSuffix = if (siteText.isNotBlank()) ", Участок: $siteText" else ""
        return "____________________ (подпись), ${actor.fullName}, $positionText$siteSuffix"
    }

    fun computeAspectFitBounds(
        srcWidth: Int,
        srcHeight: Int,
        frameLeft: Float,
        frameTop: Float,
        frameRight: Float,
        frameBottom: Float
    ): AspectFitRect {
        val frameW = (frameRight - frameLeft).coerceAtLeast(1f)
        val frameH = (frameBottom - frameTop).coerceAtLeast(1f)
        if (srcWidth <= 0 || srcHeight <= 0) {
            return AspectFitRect(frameLeft, frameTop, frameRight, frameBottom)
        }
        val srcRatio = srcWidth.toFloat() / srcHeight.toFloat()
        val frameRatio = frameW / frameH
        val drawnW: Float
        val drawnH: Float
        if (srcRatio > frameRatio) {
            drawnW = frameW
            drawnH = (frameW / srcRatio).coerceAtLeast(1f)
        } else {
            drawnH = frameH
            drawnW = (frameH * srcRatio).coerceAtLeast(1f)
        }
        val left = frameLeft + (frameW - drawnW) / 2f
        val top = frameTop + (frameH - drawnH) / 2f
        return AspectFitRect(
            left = left,
            top = top,
            right = left + drawnW,
            bottom = top + drawnH
        )
    }

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

    fun formatEpochDayRangeLabel(startEpochDay: Long, endEpochDay: Long): String {
        return if (startEpochDay == endEpochDay) {
            formatEpochDay(startEpochDay)
        } else {
            "${formatEpochDay(startEpochDay)} — ${formatEpochDay(endEpochDay)}"
        }
    }

    fun resolveTemplateDisplayName(
        template: ReportTemplateDocument,
        currentEpochDay: Long,
        startEpochDay: Long? = null,
        endEpochDay: Long? = null,
        contentScope: ReportContentScope? = null
    ): String {
        val sDay = startEpochDay ?: currentEpochDay
        val eDay = endEpochDay ?: sDay
        val rangeStr = formatEpochDayRangeLabel(minOf(sDay, eDay), maxOf(sDay, eDay))
        val tomorrowStr = formatEpochDay(currentEpochDay + 1L)
        val rType = template.parsedReportType
        val rawName = template.name.trim()
        return when {
            contentScope == ReportContentScope.COMBINED_REPORT_AND_PLAN ||
                rType == ReportType.COMBINED_DAILY_AND_PLAN ->
                "Отчет за ${formatEpochDay(currentEpochDay)} и план работ на $tomorrowStr"
            contentScope == ReportContentScope.PLAN_ONLY || rType == ReportType.NEXT_DAY_PLAN -> {
                val pStart = startEpochDay ?: (currentEpochDay + 1L)
                val pEnd = endEpochDay ?: pStart
                val pRange = formatEpochDayRangeLabel(minOf(pStart, pEnd), maxOf(pStart, pEnd))
                if (pStart == pEnd && pStart == currentEpochDay + 1L) {
                    "План работ на следующий день ($pRange)"
                } else if (pStart == pEnd) {
                    "План работ на дату ($pRange)"
                } else {
                    "План работ за период ($pRange)"
                }
            }
            rType == ReportType.DAILY_REPORT ||
                rawName.contains("Суточный рапорт", ignoreCase = true) ||
                rawName.contains("Отчёт за текущий", ignoreCase = true) ||
                rawName.contains("Отчет за текущий", ignoreCase = true) -> {
                if (sDay == eDay && sDay == currentEpochDay) {
                    "Отчет за текущий день ($rangeStr)"
                } else if (sDay == eDay) {
                    "Отчет за дату ($rangeStr)"
                } else {
                    "Отчет о выполненных работах за период ($rangeStr)"
                }
            }
            startEpochDay != null && endEpochDay != null && sDay != eDay ->
                "$rawName ($rangeStr)"
            else -> rawName
        }
    }

    fun buildPreview(
        actor: CorporateEmployee,
        template: ReportTemplateDocument,
        allTasks: List<TaskDocument>,
        currentEpochDay: Long,
        overrideLayout: SheetLayout? = null,
        overrideFormat: ExportFormat? = null,
        includePhotos: Boolean = true,
        allEmployees: List<CorporateEmployee> = emptyList(),
        rpgState: com.example.domain.rpg.RpgWorkspaceState? = null,
        startEpochDay: Long? = null,
        endEpochDay: Long? = null,
        contentScope: ReportContentScope? = null
    ): GeneratedDocumentResult {
        val effectiveRpgState = rpgState ?: com.example.domain.rpg.RpgWorkspaceState()
        val reportType = template.parsedReportType
        val effectiveScope = contentScope ?: when (reportType) {
            ReportType.COMBINED_DAILY_AND_PLAN -> ReportContentScope.COMBINED_REPORT_AND_PLAN
            else -> ReportContentScope.COMBINED_REPORT_AND_PLAN
        }
        val layout = when (effectiveScope) {
            ReportContentScope.REPORT_ONLY,
            ReportContentScope.PLAN_ONLY -> SheetLayout.SINGLE_SHEET
            ReportContentScope.COMBINED_REPORT_AND_PLAN -> overrideLayout ?: template.parsedSheetLayout
        }
        val format = overrideFormat ?: template.parsedExportFormat

        // Вычисляем точный диапазон дат для отчёта и для плана работ
        val rawReportStart = startEpochDay ?: currentEpochDay
        val rawReportEnd = endEpochDay ?: rawReportStart
        val reportStartDay = minOf(rawReportStart, rawReportEnd)
        val reportEndDay = maxOf(rawReportStart, rawReportEnd)

        val rawPlanStart = when (effectiveScope) {
            ReportContentScope.PLAN_ONLY -> startEpochDay ?: (currentEpochDay + 1L)
            else -> currentEpochDay + 1L
        }
        val rawPlanEnd = when (effectiveScope) {
            ReportContentScope.PLAN_ONLY -> endEpochDay ?: rawPlanStart
            else -> rawPlanStart
        }
        val planStartDay = minOf(rawPlanStart, rawPlanEnd)
        val planEndDay = maxOf(rawPlanStart, rawPlanEnd)

        val resolvedReportTitle = resolveTemplateDisplayName(
            template = template,
            currentEpochDay = currentEpochDay,
            startEpochDay = startEpochDay,
            endEpochDay = endEpochDay,
            contentScope = contentScope
        )
        val reportRangeLabel = formatEpochDayRangeLabel(reportStartDay, reportEndDay)
        val planRangeLabel = formatEpochDayRangeLabel(planStartDay, planEndDay)

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
                it.isRelevantToItr(actor, allEmployees)
            }
            CorporateRole.WORKER -> allTasks.filter { it.isAssignedToEmployee(actor) }
        }

        val cleanActorSite = com.example.data.seed.CorporateSeedData.sanitizeSiteOrLocation(
            raw = actor.siteName,
            fallbackSubdivision = actor.groupName
        )
        val cleanActorSubdivision = com.example.data.seed.CorporateSeedData.sanitizeSubdivisionName(actor.groupName)
        val cleanActorPosition = com.example.data.seed.CorporateSeedData.sanitizePositionName(
            raw = actor.position,
            role = actor.role
        )

        val roleScopeLabel = when (actor.role) {
            CorporateRole.ADMIN -> "Полный доступ администратора ($cleanActorSite, $cleanActorSubdivision)"
            CorporateRole.HEAD -> "Сводный по персоналу ($cleanActorSite, $cleanActorSubdivision)"
            CorporateRole.ITR -> "Сводный по подразделению ($cleanActorSubdivision, $cleanActorSite)"
            CorporateRole.WORKER -> "Личная отчётность исполнителя (${actor.fullName}, $cleanActorPosition)"
        }

        // 1. Задачи для ПЛАНА РАБОТ:
        // Включаются задачи, назначенные на выбранную дату/период плана (planStartDay..planEndDay),
        // а если план формируется на следующий день после базового (planStartDay == currentEpochDay + 1L) —
        // также незавершённые переходящие задачи за currentEpochDay.
        val planTasks = roleScopedTasks.filter { task ->
            val inPlanRange = task.dueDateEpochDay in planStartDay..planEndDay
            val isCarryoverFromBaseDay = (planStartDay == currentEpochDay + 1L) &&
                task.dueDateEpochDay == currentEpochDay &&
                !task.taskStatus.isTerminalOrDone &&
                (task.taskStatus.isEligibleForCarryover || !task.isVolumeCompleted100Percent())
            inPlanRange || isCarryoverFromBaseDay
        }

        // 2. Задачи для ОТЧЁТА (ФАКТ):
        // Строго фильтруются по выбранному дню или периоду (reportStartDay..reportEndDay),
        // чтобы в отчёт за 17.11.2026 никогда не попадали старые задачи за 13.11, 14.11, 15.11, 16.11.
        val tasksInReportPeriod = roleScopedTasks.filter {
            it.dueDateEpochDay in reportStartDay..reportEndDay
        }
        val hasExplicitDateFilter = startEpochDay != null || endEpochDay != null || contentScope != null
        val factTasks = when (reportType) {
            ReportType.NEXT_DAY_PLAN,
            ReportType.DAILY_REPORT,
            ReportType.COMBINED_DAILY_AND_PLAN -> tasksInReportPeriod
            ReportType.PERIOD_REPORT,
            ReportType.PERIOD_COMPARISON -> if (hasExplicitDateFilter) tasksInReportPeriod else roleScopedTasks
            ReportType.PHOTO_QUALITY -> {
                val base = if (hasExplicitDateFilter) tasksInReportPeriod else roleScopedTasks
                base.filter { it.requiresPhotoReport || it.photoItems.isNotEmpty() }
            }
            ReportType.OVERDUE_CARRYOVER -> {
                val base = if (hasExplicitDateFilter) tasksInReportPeriod else roleScopedTasks
                base.filter {
                    it.carryoverCount > 0L ||
                        it.taskStatus == TaskStatus.OVERDUE ||
                        it.taskStatus == TaskStatus.REJECTED ||
                        (!it.taskStatus.isTerminalOrDone && !it.isVolumeCompleted100Percent())
                }
            }
            ReportType.RECURRING_EXECUTION -> {
                val base = if (hasExplicitDateFilter) tasksInReportPeriod else roleScopedTasks
                base.filter { it.recurrenceRule != "NONE" }
            }
            ReportType.PRIORITY_DISTRIBUTION,
            ReportType.GAMIFICATION_SUMMARY -> if (hasExplicitDateFilter) tasksInReportPeriod else roleScopedTasks
            else -> if (hasExplicitDateFilter) tasksInReportPeriod else roleScopedTasks
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

        val factRows = when (reportType) {
            ReportType.PRIORITY_DISTRIBUTION -> mapPriorityDistributionRows(
                tasks = factTasks,
                config = effectiveRpgState.config,
                sectionKind = ReportSectionKind.DAILY_FACT_REPORT
            )
            ReportType.GAMIFICATION_SUMMARY -> mapGamificationSummaryRows(
                actor = actor,
                employees = allEmployees.ifEmpty { listOf(actor) },
                rpgState = effectiveRpgState,
                sectionKind = ReportSectionKind.DAILY_FACT_REPORT
            )
            else -> mapTasksToRows(
                tasks = factTasks,
                includePhotos = includePhotos,
                sectionKind = ReportSectionKind.DAILY_FACT_REPORT,
                currentEpochDay = currentEpochDay,
                reportStartDay = reportStartDay,
                reportEndDay = reportEndDay,
                actor = actor,
                allEmployees = allEmployees
            )
        }
        val planRows = mapTasksToRows(
            tasks = planTasks,
            includePhotos = includePhotos,
            sectionKind = ReportSectionKind.NEXT_DAY_PLAN,
            currentEpochDay = currentEpochDay,
            reportStartDay = planStartDay,
            reportEndDay = planEndDay,
            actor = actor,
            allEmployees = allEmployees
        )

        val factTitleLabel = if (reportStartDay == reportEndDay && reportStartDay == currentEpochDay) {
            "ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ ($reportRangeLabel) — ФАКТИЧЕСКОЕ ВЫПОЛНЕНИЕ РАБОТ"
        } else if (reportStartDay == reportEndDay) {
            "ОТЧЁТ ЗА ДАТУ ($reportRangeLabel) — ФАКТИЧЕСКОЕ ВЫПОЛНЕНИЕ РАБОТ"
        } else {
            "ОТЧЁТ ЗА ПЕРИОД ($reportRangeLabel) — ФАКТИЧЕСКОЕ ВЫПОЛНЕНИЕ РАБОТ"
        }

        val planTitleLabel = if (planStartDay == planEndDay && planStartDay == currentEpochDay + 1L) {
            "ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ ($planRangeLabel) — ПЛАНОВОЕ ЗАДАНИЕ НА СМЕНУ"
        } else if (planStartDay == planEndDay) {
            "ПЛАН РАБОТ НА ДАТУ ($planRangeLabel) — ПЛАНОВОЕ ЗАДАНИЕ"
        } else {
            "ПЛАН РАБОТ ЗА ПЕРИОД ($planRangeLabel) — ПЛАНОВЫЕ ЗАДАНИЯ"
        }

        val factSection = ReportSectionBlock(
            kind = ReportSectionKind.DAILY_FACT_REPORT,
            bannerTitle = factTitleLabel,
            bannerSubtitle = "Выполненные и выполняемые за период ($reportRangeLabel) работы, фактический объём по чек-листам и статус приёмки",
            dateLabel = reportRangeLabel,
            emptyStateMessage = "За выбранный период ($reportRangeLabel) задач в отчёте не числится.",
            headers = reportHeaders,
            rows = factRows
        )

        val planSection = ReportSectionBlock(
            kind = ReportSectionKind.NEXT_DAY_PLAN,
            bannerTitle = planTitleLabel,
            bannerSubtitle = if (planStartDay == currentEpochDay + 1L && planStartDay == planEndDay) {
                "Новые задачи, назначенные на $planRangeLabel, и переходящие невыполненные объёмы с текущей смены ($reportRangeLabel)"
            } else {
                "Плановые задания, назначенные на период ($planRangeLabel)"
            },
            dateLabel = planRangeLabel,
            emptyStateMessage = "На выбранный период ($planRangeLabel) плановых или переходящих задач не назначено.",
            headers = planHeaders,
            rows = planRows
        )

        val sheets = when (effectiveScope) {
            ReportContentScope.REPORT_ONLY -> listOf(
                ReportSheetPreview(
                    sheetIndex = 1,
                    sheetTitle = factTitleLabel,
                    subtitle = "Отчётный документ за период ($reportRangeLabel): фактически выполненные работы, чек-листы и фотофиксация",
                    headers = reportHeaders,
                    rows = factRows,
                    sections = listOf(factSection)
                )
            )
            ReportContentScope.PLAN_ONLY -> listOf(
                ReportSheetPreview(
                    sheetIndex = 1,
                    sheetTitle = planTitleLabel,
                    subtitle = "Плановый документ на период ($planRangeLabel): запланированные работы и переходящие остатки объёмов",
                    headers = planHeaders,
                    rows = planRows,
                    sections = listOf(planSection)
                )
            )
            ReportContentScope.COMBINED_REPORT_AND_PLAN -> if (layout == SheetLayout.SPLIT_SHEETS) {
                listOf(
                    ReportSheetPreview(
                        sheetIndex = 1,
                        sheetTitle = "ЛИСТ 1 ИЗ 2 • ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ ($planRangeLabel)",
                        subtitle = "Отдельный лист плана работ на $planRangeLabel (плановые задания и переходящие остатки объёмов)",
                        headers = planHeaders,
                        rows = planRows,
                        sections = listOf(
                            planSection.copy(
                                bannerTitle = "ЛИСТ 1 (ОТДЕЛЬНЫЙ ЛИСТ ПЛАНА) • ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ ($planRangeLabel)"
                            )
                        )
                    ),
                    ReportSheetPreview(
                        sheetIndex = 2,
                        sheetTitle = "ЛИСТ 2 ИЗ 2 • ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ ($reportRangeLabel)",
                        subtitle = "Отдельный лист отчёта за $reportRangeLabel (фактически выполненные объёмы работ, чек-листы и фотофиксация)",
                        headers = reportHeaders,
                        rows = factRows,
                        sections = listOf(
                            factSection.copy(
                                bannerTitle = "ЛИСТ 2 (ОТДЕЛЬНЫЙ ЛИСТ ОТЧЁТА) • ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ ($reportRangeLabel)"
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
                        sheetTitle = "ЕДИНЫЙ ЛИСТ: 1) ОТЧЁТ ЗА ДЕНЬ ($reportRangeLabel) + 2) ПЛАН НА СЛЕДУЮЩИЙ ДЕНЬ ($planRangeLabel)",
                        subtitle = "Совмещённый документ на одной странице с двумя раздельными таблицами: Блок 1 — Отчёт за $reportRangeLabel, Блок 2 — План на $planRangeLabel",
                        headers = reportHeaders,
                        rows = allSingleSheetRows,
                        sections = listOf(
                            factSection.copy(
                                bannerTitle = "БЛОК 1 (ВЕРХНЯЯ ТАБЛИЦА) • ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ ($reportRangeLabel) — ФАКТ ВЫПОЛНЕНИЯ"
                            ),
                            planSection.copy(
                                bannerTitle = "БЛОК 2 (НИЖНЯЯ ТАБЛИЦА) • ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ ($planRangeLabel) — ПЛАНОВОЕ ЗАДАНИЕ"
                            )
                        )
                    )
                )
            }
        }

        // Для планов работ фотоприложение не выгружает старые снимки;
        // для отчётов и совмещённого режима выгружаются строго фотографии, снятые за период reportStartDay..reportEndDay.
        val isStrictPhotoPeriodFilter = hasExplicitDateFilter ||
            reportType == ReportType.DAILY_REPORT ||
            reportType == ReportType.NEXT_DAY_PLAN ||
            reportType == ReportType.COMBINED_DAILY_AND_PLAN
        val attachedPhotos = if (includePhotos && effectiveScope != ReportContentScope.PLAN_ONLY) {
            factTasks.distinctBy { it.id }.flatMap { t ->
                val matchedEmp = allEmployees.firstOrNull { t.isAssignedToEmployee(it) }
                val cleanSub = com.example.data.seed.CorporateSeedData.sanitizeSubdivisionName(
                    raw = matchedEmp?.groupName?.ifBlank { t.groupName } ?: t.groupName,
                    fallbackSubdivision = cleanActorSubdivision
                )
                val cleanTaskSite = com.example.data.seed.CorporateSeedData.sanitizeSiteOrLocation(
                    raw = t.geoLocation,
                    fallbackSite = matchedEmp?.siteName?.ifBlank { cleanActorSite } ?: cleanActorSite,
                    fallbackSubdivision = cleanSub
                )
                t.decodedPhotos()
                    .filter { ph ->
                        !isStrictPhotoPeriodFilter ||
                            photoMatchesEpochDayRange(ph, t.dueDateEpochDay, reportStartDay, reportEndDay)
                    }
                    .map { ph ->
                        val sanitizedPhoto = ph.copy(
                            gpsCoords = com.example.data.seed.CorporateSeedData.sanitizeSiteOrLocation(
                                raw = ph.gpsCoords,
                                fallbackSite = cleanTaskSite,
                                fallbackSubdivision = cleanSub
                            )
                        )
                        ReportAttachedPhoto(
                            taskTitle = t.title,
                            assigneeFio = t.assigneeName,
                            photo = sanitizedPhoto
                        )
                    }
            }
        } else {
            emptyList()
        }

        val nowLabel = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru")).format(Date())
        val periodLabel = when (effectiveScope) {
            ReportContentScope.REPORT_ONLY -> "Отчёт за $reportRangeLabel"
            ReportContentScope.PLAN_ONLY -> "План работ на $planRangeLabel"
            ReportContentScope.COMBINED_REPORT_AND_PLAN -> "Отчёт за $reportRangeLabel / План на $planRangeLabel"
        }

        val sanitizedOrgHeader = formatOrgHeader(
            organizationName = com.example.data.seed.CorporateSeedData.DEFAULT_ORG_NAME,
            subdivisionName = cleanActorSubdivision,
            siteName = cleanActorSite
        ).ifBlank { template.orgHeader }

        val signatures = listOf(
            "Подготовил документ: ${formatPreparedBySignature(actor, cleanActorSite)}",
            "Проверил / Принял: ____________________ (подпись), ФИО, должность"
        )
        val preparedByLabel = listOfNotNull(
            actor.fullName.takeIf { it.isNotBlank() },
            cleanActorPosition.takeIf { it.isNotBlank() },
            cleanActorSubdivision.takeIf { it.isNotBlank() },
            cleanActorSite.takeIf { it.isNotBlank() }?.let { "Участок: $it" }
        ).joinToString(", ")

        val photoModeLabel = if (includePhotos && effectiveScope != ReportContentScope.PLAN_ONLY) {
            "С фотоотчётом (${attachedPhotos.size} фото за $reportRangeLabel)"
        } else {
            "Без фотографий (только таблица)"
        }
        val layoutModeLabel = when (effectiveScope) {
            ReportContentScope.REPORT_ONLY -> "ТОЛЬКО ОТЧЁТ О ВЫПОЛНЕННЫХ РАБОТАХ ($reportRangeLabel)"
            ReportContentScope.PLAN_ONLY -> "ТОЛЬКО ПЛАН РАБОТ ($planRangeLabel)"
            ReportContentScope.COMBINED_REPORT_AND_PLAN -> if (layout == SheetLayout.SPLIT_SHEETS) {
                "НА РАЗДЕЛЬНЫХ ЛИСТАХ (2 листа: Отчёт и План отдельно)"
            } else {
                "НА ОДНОМ ЛИСТЕ (Блок 1: Отчёт за $reportRangeLabel + Блок 2: План на $planRangeLabel)"
            }
        }

        val summaryText = buildString {
            appendLine(sanitizedOrgHeader)
            appendLine("Документ: $resolvedReportTitle (${format.code})")
            appendLine("Компоновка: $layoutModeLabel • Режим фото: $photoModeLabel")
            appendLine("Период: $periodLabel • Охват: $roleScopeLabel")
            appendLine()
            if (effectiveScope != ReportContentScope.PLAN_ONLY) {
                appendLine("================ ОТЧЁТ ЗА ПЕРИОД ($reportRangeLabel) — ФАКТ (${factRows.size} зад.) ================")
                if (factRows.isEmpty()) {
                    appendLine("   (По отчёту за $reportRangeLabel записей нет)")
                } else {
                    factRows.forEach { r ->
                        val phPart = if (includePhotos) ", фото: ${r.photoInfo}" else ""
                        appendLine("${r.index}. [${r.rowTypeBadge}] ${r.assigneeFio} (${r.groupName}) — ${r.taskTitle} | Объём: ${r.volumeInfo} | Статус: ${r.statusLabel}$phPart")
                        r.checklistBreakdown.forEach { subLine ->
                            appendLine("   • Чек-лист: $subLine")
                        }
                    }
                }
                appendLine()
            }
            if (effectiveScope != ReportContentScope.REPORT_ONLY) {
                appendLine("================ ПЛАН РАБОТ ($planRangeLabel) — ПЛАН (${planRows.size} зад.) ================")
                if (planRows.isEmpty()) {
                    appendLine("   (На период $planRangeLabel плановых или переходящих задач нет)")
                } else {
                    planRows.forEach { r ->
                        appendLine("${r.index}. [${r.rowTypeBadge}] ${r.assigneeFio} (${r.groupName}) — ${r.taskTitle} | Плановый объём: ${r.planVolumeLabel} | Срок: ${r.deadlineLabel}")
                        r.checklistBreakdown.forEach { subLine ->
                            appendLine("   • Чек-лист: $subLine")
                        }
                    }
                }
            }
        }.trim()

        val dummyFile = File("preview_${template.id}.${format.extension}")
        return GeneratedDocumentResult(
            file = dummyFile,
            orgHeader = sanitizedOrgHeader,
            reportTitle = resolvedReportTitle,
            periodLabel = periodLabel,
            generatedAtLabel = nowLabel,
            preparedByLabel = preparedByLabel,
            roleScopeLabel = roleScopeLabel,
            sheetLayout = layout,
            exportFormat = format,
            includePhotos = includePhotos && effectiveScope != ReportContentScope.PLAN_ONLY,
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
        saveToDownloads: Boolean = false,
        allEmployees: List<CorporateEmployee> = emptyList(),
        rpgState: com.example.domain.rpg.RpgWorkspaceState? = null,
        startEpochDay: Long? = null,
        endEpochDay: Long? = null,
        contentScope: ReportContentScope? = null
    ): GeneratedDocumentResult {
        val preview = buildPreview(
            actor = actor,
            template = template,
            allTasks = allTasks,
            currentEpochDay = currentEpochDay,
            overrideLayout = overrideLayout,
            overrideFormat = overrideFormat,
            includePhotos = includePhotos,
            allEmployees = allEmployees,
            rpgState = rpgState,
            startEpochDay = startEpochDay,
            endEpochDay = endEpochDay,
            contentScope = contentScope
        )
        val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
        val safeName = template.parsedReportType.code.lowercase(Locale.US)
        val photoSuffix = if (preview.includePhotos) "with_photos" else "no_photos"
        val layoutSuffix = if (preview.sheetLayout == SheetLayout.SPLIT_SHEETS) "2sheets" else "1sheet"
        val fileName = "ezhednevnik_${safeName}_${layoutSuffix}_${photoSuffix}_${System.currentTimeMillis()}.${preview.exportFormat.extension}"
        val outFile = File(reportsDir, fileName)

        when (preview.exportFormat) {
            ExportFormat.PDF -> writePdfFile(context, outFile, preview)
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

    fun openPdfFile(
        context: Context,
        pdfFile: File,
        title: String = "Фотоотчёт PDF"
    ) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(viewIntent, "Открыть $title").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }.onFailure {
            sharePdfFile(context, pdfFile, title)
        }
    }

    fun sharePdfFile(
        context: Context,
        pdfFile: File,
        title: String = "Фотоотчёт PDF"
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Поделиться PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Выгружает все фотографии указанного фотоотчёта по задаче в отдельный многостраничный PDF-документ
     * (формат A4 портрет, 2 крупных снимка на страницу с сохранением точных пропорций исходников и метаданными).
     */
    fun exportTaskPhotosToPdf(
        context: Context,
        task: TaskDocument,
        photos: List<PhotoReportItem>,
        preparedByName: String = "",
        saveToDownloads: Boolean = true
    ): Result<TaskPhotoPdfResult> = runCatching {
        require(photos.isNotEmpty()) { "В фотоотчёте нет фотографий для экспорта в PDF" }

        val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
        val safeTitle = task.title
            .replace(Regex("[^a-zA-Zа-яА-ЯёЁ0-9_\\-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(32)
            .ifBlank { "task" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(reportsDir, "PhotoReport_${safeTitle}_$stamp.pdf")

        val pdf = PdfDocument()
        val pageWidth = 595 // A4 Portrait width in points
        val pageHeight = 842 // A4 Portrait height in points

        val headerBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val frameBgPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            style = Paint.Style.FILL
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 12f
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8.5f
        }
        val cardTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            isFakeBoldText = true
        }
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val chunks = photos.chunked(2)
        val totalPages = chunks.size
        val generatedAt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru")).format(Date())

        chunks.forEachIndexed { pageIdx, chunk ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIdx + 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            val headerRect = RectF(28f, 24f, pageWidth - 28f, 82f)
            canvas.drawRoundRect(headerRect, 8f, 8f, headerBgPaint)
            canvas.drawRoundRect(headerRect, 8f, 8f, borderPaint)

            canvas.drawText(
                "Фотоотчёт: ${task.title.ifBlank { "Задача" }.take(56)}",
                38f,
                44f,
                titlePaint
            )
            val siteOrGroup = com.example.data.seed.CorporateSeedData.sanitizeSiteOrLocation(
                raw = task.geoLocation,
                fallbackSubdivision = task.groupName
            )
            val executor = task.assigneeName.ifBlank { "Исполнитель" }
            canvas.drawText(
                "Объект: ${siteOrGroup.take(44)}  •  Исполнитель: ${executor.take(28)}  •  Срок: ${formatEpochDay(task.dueDateEpochDay)}",
                38f,
                60f,
                subPaint
            )
            val authorSuffix = if (preparedByName.isNotBlank()) "  •  Сформировал: ${preparedByName.take(26)}" else ""
            canvas.drawText(
                "Всего фото: ${photos.size} шт.  •  Дата выгрузки: $generatedAt$authorSuffix  •  Стр. ${pageIdx + 1} из $totalPages",
                38f,
                74f,
                subPaint
            )

            val cardLeft = 28f
            val cardRight = pageWidth - 28f
            val cardHeight = 352f
            val cardGap = 16f
            var y = 94f

            chunk.forEachIndexed { localIdx, photo ->
                val globalIndex = pageIdx * 2 + localIdx + 1
                val cardRect = RectF(cardLeft, y, cardRight, y + cardHeight)
                canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

                val frameLeft = cardLeft + 10f
                val frameTop = y + 10f
                val frameRight = cardRight - 10f
                val frameBottom = y + cardHeight - 58f
                val frameRect = RectF(frameLeft, frameTop, frameRight, frameBottom)
                canvas.drawRoundRect(frameRect, 6f, 6f, frameBgPaint)

                val cleanPhotoGps = com.example.data.seed.CorporateSeedData.sanitizeSiteOrLocation(
                    raw = photo.gpsCoords,
                    fallbackSite = siteOrGroup,
                    fallbackSubdivision = task.groupName
                )
                val origResult = PhotoCaptureUtils.loadOriginalBitmapFromUriOrPath(
                    uriOrPath = photo.uriString,
                    fallbackComment = photo.comment,
                    fallbackAuthor = photo.authorName.ifBlank { executor },
                    fallbackTime = photo.capturedAtText,
                    fallbackGps = cleanPhotoGps,
                    fallbackTaskTitle = task.title,
                    context = context,
                    photoId = photo.id
                )
                val bmp = origResult.bitmap
                if (bmp != null && bmp.width > 0 && bmp.height > 0) {
                    val fitted = computeAspectFitBounds(
                        srcWidth = bmp.width,
                        srcHeight = bmp.height,
                        frameLeft = frameLeft + 2f,
                        frameTop = frameTop + 2f,
                        frameRight = frameRight - 2f,
                        frameBottom = frameBottom - 2f
                    )
                    val dstRect = RectF(fitted.left, fitted.top, fitted.right, fitted.bottom)
                    canvas.drawBitmap(bmp, null, dstRect, bitmapPaint)
                } else {
                    val missingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(148, 163, 184)
                        textSize = 11f
                    }
                    canvas.drawText("Изображение недоступно для предпросмотра", frameLeft + 20f, (frameTop + frameBottom) / 2f, missingPaint)
                }

                val resLabel = if (origResult.width > 0 && origResult.height > 0) {
                    " • ${origResult.width}×${origResult.height} px (${origResult.sourceLabel})"
                } else {
                    ""
                }
                canvas.drawText(
                    "Фото #$globalIndex из ${photos.size}  •  ${photo.authorName.ifBlank { executor }}  •  ${photo.capturedAtText}$resLabel",
                    cardLeft + 12f,
                    frameBottom + 18f,
                    cardTitlePaint
                )
                val commentLine = photo.comment.ifBlank { "Фотофиксация #$globalIndex" }.take(74)
                val gpsLine = cleanPhotoGps.take(54)
                canvas.drawText(
                    "Комментарий: $commentLine",
                    cardLeft + 12f,
                    frameBottom + 33f,
                    subPaint
                )
                canvas.drawText(
                    "Участок / Подразделение: $gpsLine",
                    cardLeft + 12f,
                    frameBottom + 46f,
                    subPaint
                )

                y += cardHeight + cardGap
            }

            pdf.finishPage(page)
        }

        FileOutputStream(outFile).use { fos ->
            pdf.writeTo(fos)
        }
        pdf.close()

        val downloadedPath = if (saveToDownloads) {
            saveFileToPhoneDownloads(
                context = context,
                sourceFile = outFile,
                mimeType = "application/pdf",
                showToast = true
            ).getOrNull()
        } else {
            null
        }

        TaskPhotoPdfResult(
            file = outFile,
            downloadedToPath = downloadedPath,
            photoCount = photos.size,
            pageCount = totalPages
        )
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
        currentEpochDay: Long,
        reportStartDay: Long = currentEpochDay,
        reportEndDay: Long = currentEpochDay,
        actor: CorporateEmployee? = null,
        allEmployees: List<CorporateEmployee> = emptyList()
    ): List<ReportTableRow> {
        val sorted = tasks.sortedWith(
            compareBy<TaskDocument> { it.assigneeName }
                .thenBy { it.dueDateEpochDay }
        )
        return sorted.mapIndexed { idx, task ->
            val matchedEmployee = allEmployees.firstOrNull { task.isAssignedToEmployee(it) }
            val resolvedSubdivision = com.example.data.seed.CorporateSeedData.sanitizeSubdivisionName(
                raw = matchedEmployee?.groupName?.ifBlank { task.groupName } ?: task.groupName,
                fallbackSubdivision = actor?.groupName ?: com.example.data.seed.CorporateSeedData.DEFAULT_SUBDIVISION_NAME
            )
            val resolvedSite = com.example.data.seed.CorporateSeedData.sanitizeSiteOrLocation(
                raw = task.geoLocation,
                fallbackSite = matchedEmployee?.siteName?.ifBlank { actor?.siteName.orEmpty() }
                    ?: actor?.siteName.orEmpty(),
                fallbackSubdivision = resolvedSubdivision
            )

            val relevantPhotoCount = if (sectionKind == ReportSectionKind.DAILY_FACT_REPORT) {
                task.decodedPhotos().count { ph ->
                    photoMatchesEpochDayRange(ph, task.dueDateEpochDay, reportStartDay, reportEndDay)
                }
            } else {
                task.photoItems.size
            }

            val photoText = if (!includePhotos) {
                "Скрыто (без фото)"
            } else if (task.requiresPhotoReport) {
                "$relevantPhotoCount/${task.minPhotoCount} фото"
            } else {
                "$relevantPhotoCount (не обяз.)"
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
                resolvedSite.isNotBlank() -> resolvedSite
                else -> "Без замечаний"
            }

            ReportTableRow(
                index = idx + 1,
                sectionKind = sectionKind,
                rowTypeBadge = rowBadge,
                assigneeFio = task.assigneeName,
                groupName = resolvedSubdivision,
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

    private fun mapPriorityDistributionRows(
        tasks: List<TaskDocument>,
        config: com.example.domain.rpg.GamificationConfig,
        sectionKind: ReportSectionKind
    ): List<ReportTableRow> {
        return TaskPriority.entries.mapIndexed { idx, prio ->
            val groupTasks = tasks.filter { it.taskPriority == prio }
            val approved = groupTasks.count { it.taskStatus == TaskStatus.APPROVED }
            val overdue = groupTasks.count { it.taskStatus == TaskStatus.OVERDUE || it.carryoverCount > 0L }
            val avgPct = if (groupTasks.isEmpty()) 0 else groupTasks.map { it.completionPercent() }.average().toInt()
            val rule = config.ruleFor(prio)
            ReportTableRow(
                index = idx + 1,
                sectionKind = sectionKind,
                rowTypeBadge = "ПРИОРИТЕТ",
                assigneeFio = prio.titleRu,
                groupName = "Вес: ${prio.weight}",
                taskTitle = "Задач: ${groupTasks.size} (принято: $approved, проср.: $overdue)",
                volumeInfo = "База ${rule.baseXp} XP (+${rule.approvalBonusXp} приёмка)",
                planVolumeLabel = "База ${rule.baseXp} XP",
                factVolumeLabel = "$approved из ${groupTasks.size} ($avgPct%)",
                checklistBreakdown = emptyList(),
                completionPercent = avgPct,
                priorityLabel = prio.titleRu,
                statusLabel = "Закрыто: $approved",
                deadlineLabel = "Проср.: $overdue",
                photoInfo = "Фото: ${groupTasks.count { it.photoItems.isNotEmpty() }}",
                carryoverInfo = "Переносов: ${groupTasks.sumOf { it.carryoverCount.toInt() }}",
                commentSummary = "Ср. прогресс $avgPct%"
            )
        }
    }

    private fun mapGamificationSummaryRows(
        actor: CorporateEmployee,
        employees: List<CorporateEmployee>,
        rpgState: com.example.domain.rpg.RpgWorkspaceState,
        sectionKind: ReportSectionKind
    ): List<ReportTableRow> {
        val visibleEmployees = employees.filter {
            com.example.domain.rpg.RpgProgressionEngine.canViewProgression(actor, it)
        }.distinctBy { it.id }
        return visibleEmployees.mapIndexed { idx, emp ->
            val profile = rpgState.profiles[emp.id]
                ?: com.example.domain.rpg.EmployeeRpgProfile(emp.id)
            val lvl = com.example.domain.rpg.RpgProgressionEngine.resolveUserLevel(profile.totalXp, rpgState.config)
            val status = com.example.domain.rpg.RpgProgressionEngine.resolveUserStatus(emp.role, lvl.level)
            val prestigePart = if (profile.prestige.prestigeLevel > 0) " • Престиж ${profile.prestige.prestigeLevel}" else ""
            ReportTableRow(
                index = idx + 1,
                sectionKind = sectionKind,
                rowTypeBadge = "RPG УР.${lvl.level}",
                assigneeFio = emp.fullName,
                groupName = emp.groupName,
                taskTitle = "${status.titleRu}$prestigePart • Ур. ${lvl.level}/${rpgState.config.maxLevel}",
                volumeInfo = "XP: ${profile.totalXp} (Сезон: ${profile.seasonXp})",
                planVolumeLabel = "Репутация: ${profile.reputation.score}/100",
                factVolumeLabel = "Серия: ${profile.streak.currentDays} дн. (рекорд ${profile.streak.bestDays})",
                checklistBreakdown = listOf(
                    "Достижений: ${profile.achievements.size}, SP доступно: ${profile.skillPointsAvailable}, Навыков: ${profile.skills.sumOf { it.level }}"
                ),
                completionPercent = profile.reputation.score,
                priorityLabel = emp.role.shortTitleRu,
                statusLabel = "Реп. ${profile.reputation.score}/100",
                deadlineLabel = "Серия ${profile.streak.currentDays} д.",
                photoInfo = "Ачивок: ${profile.achievements.size}",
                carryoverInfo = profile.reputation.tierTitleRu,
                commentSummary = "Сезон ${rpgState.currentSeason.quarterCode}: ${profile.seasonXp} XP"
            )
        }
    }

    // =========================================================================
    // 1. PDF WRITER (Явное визуальное разделение ОТЧЁТА ЗА ДЕНЬ и ПЛАНА НА СЛЕДУЮЩИЙ ДЕНЬ
    //    как на раздельных листах, так и на одном совмещённом листе)
    // =========================================================================
    private fun writePdfFile(context: Context, outFile: File, doc: GeneratedDocumentResult) {
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

            val layoutDescription = if (doc.sheetLayout == SheetLayout.SPLIT_SHEETS && doc.sheets.size > 1) {
                "РЕЖИМ ВЫГРУЗКИ: НА РАЗДЕЛЬНЫХ ЛИСТАХ (2 ЛИСТА)  —  $sheetTitleText"
            } else if (doc.sheets.firstOrNull()?.sections?.size == 1) {
                "ДОКУМЕНТ: $sheetTitleText"
            } else {
                "РЕЖИМ ВЫГРУЗКИ: НА ОДНОЙ СТРАНИЦЕ (БЛОК 1: ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ  +  БЛОК 2: ПЛАН НА СЛЕДУЮЩИЙ ДЕНЬ)"
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

            // Нижний колонтитул с подписями (с переносом на новую страницу, если таблица дошла до самого низа)
            val neededSigHeight = (doc.signatureBlock.size * 13f) + 12f
            if (y + neededSigHeight > pageHeight - 14f) {
                pdf.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                y = drawTopDocumentHeader(canvas, sheet.sheetTitle, isContinuation = true)
            }
            var sigY = (pageHeight - 36f).coerceAtLeast(y + 10f).coerceAtMost(pageHeight - neededSigHeight)
            doc.signatureBlock.forEach { sig ->
                if (sigY <= pageHeight - 8f) {
                    canvas.drawText(sig, 28f, sigY, metaPaint)
                    sigY += 13f
                }
            }

            pdf.finishPage(page)
        }

        // Приложение с фотоотчётами (все фото с сохранением точных пропорций, пагинация по 6 карточек на лист)
        if (doc.includePhotos && doc.attachedPhotos.isNotEmpty()) {
            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val photoFrameBgPaint = Paint().apply {
                color = Color.rgb(241, 245, 249)
                style = Paint.Style.FILL
            }
            val photoChunks = doc.attachedPhotos.chunked(6)
            val totalPages = photoChunks.size

            photoChunks.forEachIndexed { chunkIdx, chunk ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas

                var y = 32f
                val pageSuffix = if (totalPages > 1) " (стр. ${chunkIdx + 1} из $totalPages)" else ""
                canvas.drawText(
                    "${doc.reportTitle} — Приложение: Фотоотчёты (${doc.attachedPhotos.size} шт.)$pageSuffix",
                    28f,
                    y,
                    orgTitlePaint
                )
                y += 18f

                var col = 0
                val cardW = 246f
                val cardH = 232f
                val colGap = 18f
                val rowGap = 14f
                val frameInset = 6f
                val frameHeight = 166f

                chunk.forEachIndexed { localIdx, item ->
                    val globalIdx = chunkIdx * 6 + localIdx
                    val x = 28f + col * (cardW + colGap)
                    val boxRect = RectF(x, y, x + cardW, y + cardH)
                    canvas.drawRoundRect(boxRect, 8f, 8f, linePaint)

                    val frameLeft = x + frameInset
                    val frameTop = y + frameInset
                    val frameRight = x + cardW - frameInset
                    val frameBottom = y + frameInset + frameHeight
                    val frameRect = RectF(frameLeft, frameTop, frameRight, frameBottom)
                    canvas.drawRoundRect(frameRect, 4f, 4f, photoFrameBgPaint)

                    val bmp = PhotoCaptureUtils.loadBitmapFromUriOrPath(
                        uriOrPath = item.photo.uriString,
                        fallbackComment = item.photo.comment,
                        fallbackAuthor = item.photo.authorName.ifBlank { item.assigneeFio },
                        fallbackTime = item.photo.capturedAtText,
                        fallbackGps = item.photo.gpsCoords,
                        fallbackTaskTitle = item.taskTitle,
                        context = context,
                        photoId = item.photo.id
                    )
                    if (bmp != null && bmp.width > 0 && bmp.height > 0) {
                        val fitted = computeAspectFitBounds(
                            srcWidth = bmp.width,
                            srcHeight = bmp.height,
                            frameLeft = frameLeft,
                            frameTop = frameTop,
                            frameRight = frameRight,
                            frameBottom = frameBottom
                        )
                        val dstRect = RectF(fitted.left, fitted.top, fitted.right, fitted.bottom)
                        canvas.drawBitmap(bmp, null, dstRect, bitmapPaint)
                    }
                    canvas.drawText("${globalIdx + 1}. ${item.taskTitle.take(30)}", x + 8f, y + 188f, cellBoldPaint)
                    canvas.drawText("${item.assigneeFio.take(24)} • ${item.photo.capturedAtText}", x + 8f, y + 203f, metaPaint)
                    canvas.drawText(item.photo.comment.take(36), x + 8f, y + 217f, metaPaint)

                    col++
                    if (col >= 3) {
                        col = 0
                        y += cardH + rowGap
                    }
                }

                pdf.finishPage(page)
            }
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
