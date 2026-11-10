package com.example.domain.reporting

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.ExportFormat
import com.example.data.model.ReportTemplateDocument
import com.example.data.model.ReportType
import com.example.data.model.SheetLayout
import com.example.data.model.TaskDocument
import com.example.data.model.TaskStatus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ReportTableRow(
    val index: Int,
    val assigneeFio: String,
    val groupName: String,
    val taskTitle: String,
    val priorityLabel: String,
    val statusLabel: String,
    val deadlineLabel: String,
    val photoInfo: String,
    val carryoverInfo: String,
    val commentSummary: String
)

data class ReportSheetPreview(
    val sheetIndex: Int,
    val sheetTitle: String,
    val subtitle: String,
    val headers: List<String>,
    val rows: List<ReportTableRow>
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
    val sheets: List<ReportSheetPreview>,
    val signatureBlock: List<String>,
    val summaryText: String
)

object ReportDocumentGenerator {

    fun formatEpochDay(epochDay: Long): String {
        val baseDay = 20768L // 10.11.2026
        val diff = (epochDay - baseDay).toInt()
        val dayOfMonth = (10 + diff).coerceIn(1, 30)
        return String.format(Locale.US, "%02d.11.2026", dayOfMonth)
    }

    fun buildPreview(
        actor: CorporateEmployee,
        template: ReportTemplateDocument,
        allTasks: List<TaskDocument>,
        currentEpochDay: Long,
        overrideLayout: SheetLayout? = null,
        overrideFormat: ExportFormat? = null
    ): GeneratedDocumentResult {
        val reportType = template.parsedReportType
        val layout = overrideLayout ?: template.parsedSheetLayout
        val format = overrideFormat ?: template.parsedExportFormat

        // Enforce RBAC for consolidated reports
        if (reportType.isConsolidatedOnly && !actor.role.canExportConsolidatedPersonnelReport()) {
            throw IllegalAccessException(
                "Сводный отчёт «${reportType.titleRu}» доступен только для ролей ИТР и Руководитель"
            )
        }

        // Scope tasks by role hierarchy
        val roleScopedTasks = when (actor.role) {
            CorporateRole.HEAD -> allTasks
            CorporateRole.ITR -> allTasks.filter {
                it.groupId == actor.groupId || it.assigneeId == actor.id || it.creatorId == actor.id
            }
            CorporateRole.WORKER -> allTasks.filter { it.assigneeId == actor.id }
        }

        val roleScopeLabel = when (actor.role) {
            CorporateRole.HEAD -> "Сводный по всему персоналу участка (${actor.siteName})"
            CorporateRole.ITR -> "Сводный по подчинённому персоналу (${actor.groupName})"
            CorporateRole.WORKER -> "Личная отчётность исполнителя (${actor.fullName})"
        }

        val planTasks = roleScopedTasks.filter {
            it.dueDateEpochDay >= currentEpochDay + 1L ||
                (it.dueDateEpochDay == currentEpochDay && it.taskStatus.isEligibleForCarryover)
        }

        val factTasks = when (reportType) {
            ReportType.NEXT_DAY_PLAN -> planTasks
            ReportType.DAILY_REPORT -> roleScopedTasks.filter { it.dueDateEpochDay <= currentEpochDay }
            ReportType.PHOTO_QUALITY -> roleScopedTasks.filter { it.requiresPhotoReport || it.photoItems.isNotEmpty() }
            ReportType.OVERDUE_CARRYOVER -> roleScopedTasks.filter {
                it.carryoverCount > 0L || it.taskStatus == TaskStatus.OVERDUE || it.taskStatus == TaskStatus.REJECTED
            }
            ReportType.RECURRING_EXECUTION -> roleScopedTasks.filter { it.recurrenceRule != "NONE" }
            else -> roleScopedTasks
        }

        val headers = listOf(
            "№",
            "ФИО исполнителя",
            "Бригада / Группа",
            "Наименование работ",
            "Приоритет",
            "Статус",
            "Срок / Дедлайн",
            "Фотоотчёт",
            "Переносы",
            "Примечание / Контроль"
        )

        val planRows = mapTasksToRows(planTasks)
        val factRows = mapTasksToRows(factTasks)

        val sheets = if (layout == SheetLayout.SPLIT_SHEETS) {
            listOf(
                ReportSheetPreview(
                    sheetIndex = 1,
                    sheetTitle = "Лист 1. План работ на следующий день (${formatEpochDay(currentEpochDay + 1L)})",
                    subtitle = "Запланированные и переходящие задачи с ФИО исполнителей и требованием фотофиксации",
                    headers = headers,
                    rows = planRows
                ),
                ReportSheetPreview(
                    sheetIndex = 2,
                    sheetTitle = "Лист 2. ${reportType.titleRu} (${formatEpochDay(currentEpochDay)})",
                    subtitle = "Фактическое выполнение, подтверждения ИТР, возвраты и переносы",
                    headers = headers,
                    rows = factRows
                )
            )
        } else {
            val combinedRows = if (reportType == ReportType.NEXT_DAY_PLAN) {
                planRows
            } else {
                (factRows + planRows).distinctBy { "${it.assigneeFio}_${it.taskTitle}" }
                    .mapIndexed { idx, row -> row.copy(index = idx + 1) }
            }
            listOf(
                ReportSheetPreview(
                    sheetIndex = 1,
                    sheetTitle = "Единый лист: ${reportType.titleRu} + План работ",
                    subtitle = "Совмещённая ведомость выполнения и плана на одном листе",
                    headers = headers,
                    rows = combinedRows
                )
            )
        }

        val nowLabel = SimpleDateFormat("dd.MM.2026 HH:mm", Locale.forLanguageTag("ru")).format(Date())
        val periodLabel = when (reportType) {
            ReportType.NEXT_DAY_PLAN -> "На смену ${formatEpochDay(currentEpochDay + 1L)}"
            ReportType.DAILY_REPORT -> "За смену ${formatEpochDay(currentEpochDay)}"
            else -> "${formatEpochDay(currentEpochDay - template.defaultPeriodDays.coerceAtLeast(1L) + 1L)} — ${formatEpochDay(currentEpochDay)}"
        }

        val signatures = listOf(
            "Руководитель участка: ____________________ / Ковалёв Д.С. /",
            "Ответственный ИТР:   ____________________ / Соколов А.В. /",
            "Подготовил документ: ____________________ / ${actor.shortName} (${actor.role.titleRu}) /"
        )

        val totalRows = sheets.sumOf { it.rows.size }
        val summaryText = buildString {
            appendLine("${template.orgHeader}")
            appendLine("Документ: ${template.name} (${format.code}, ${layout.shortRu})")
            appendLine("Период: $periodLabel • Охват: $roleScopeLabel")
            appendLine("Листов: ${sheets.size} • Всего записей: $totalRows")
            sheets.forEach { sheet ->
                appendLine("--- ${sheet.sheetTitle} (${sheet.rows.size} поз.) ---")
                sheet.rows.take(5).forEach { r ->
                    appendLine("${r.index}. ${r.assigneeFio} — ${r.taskTitle} [${r.statusLabel}, фото: ${r.photoInfo}]")
                }
            }
        }.trim()

        val dummyFile = File("preview_${template.id}.${format.extension}")
        return GeneratedDocumentResult(
            file = dummyFile,
            orgHeader = template.orgHeader,
            reportTitle = template.name,
            periodLabel = periodLabel,
            generatedAtLabel = nowLabel,
            preparedByLabel = "${actor.fullName} (${actor.position})",
            roleScopeLabel = roleScopeLabel,
            sheetLayout = layout,
            exportFormat = format,
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
        overrideFormat: ExportFormat? = null
    ): GeneratedDocumentResult {
        val preview = buildPreview(
            actor = actor,
            template = template,
            allTasks = allTasks,
            currentEpochDay = currentEpochDay,
            overrideLayout = overrideLayout,
            overrideFormat = overrideFormat
        )
        val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
        val safeName = template.parsedReportType.code.lowercase(Locale.US)
        val fileName = "ezhednevnik_${safeName}_${System.currentTimeMillis()}.${preview.exportFormat.extension}"
        val outFile = File(reportsDir, fileName)

        when (preview.exportFormat) {
            ExportFormat.PDF -> writePdfFile(outFile, preview)
            ExportFormat.XLSX -> writeXlsxFile(outFile, preview)
            ExportFormat.DOCX -> writeDocxFile(outFile, preview)
            ExportFormat.CSV -> writeCsvFile(outFile, preview)
        }

        return preview.copy(file = outFile)
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
            appendLine("Формат: ${result.exportFormat.code} (${result.sheetLayout.shortRu})")
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

    private fun mapTasksToRows(tasks: List<TaskDocument>): List<ReportTableRow> {
        val sorted = tasks.sortedWith(
            compareBy<TaskDocument> { it.assigneeName }
                .thenBy { it.dueDateEpochDay }
        )
        return sorted.mapIndexed { idx, task ->
            val photoText = if (task.requiresPhotoReport) {
                "${task.photoItems.size}/${task.minPhotoCount} фото"
            } else {
                "${task.photoItems.size} (не обяз.)"
            }
            val carryoverText = if (task.carryoverCount > 0L) {
                "Перенос ×${task.carryoverCount} (с ${formatEpochDay(task.originalDueDateEpochDay)})"
            } else {
                "В срок"
            }
            val note = when {
                !task.rejectionReason.isNullOrBlank() -> "Возврат: ${task.rejectionReason}"
                !task.approvedByName.isNullOrBlank() -> "Принял: ${task.approvedByName}"
                task.decodedComments().isNotEmpty() -> task.decodedComments().last().text
                else -> task.geoLocation ?: "Без замечаний"
            }
            ReportTableRow(
                index = idx + 1,
                assigneeFio = task.assigneeName,
                groupName = task.groupName,
                taskTitle = task.title,
                priorityLabel = task.taskPriority.titleRu,
                statusLabel = task.taskStatus.titleRu,
                deadlineLabel = "${formatEpochDay(task.dueDateEpochDay)} ${task.deadlineTime}",
                photoInfo = photoText,
                carryoverInfo = carryoverText,
                commentSummary = note
            )
        }
    }

    // --- 1. PDF WRITER (Android PdfDocument with Single-Sheet or Split-Sheets) ---
    private fun writePdfFile(outFile: File, doc: GeneratedDocumentResult) {
        val pdf = PdfDocument()
        val pageWidth = 842 // A4 Landscape width in points
        val pageHeight = 595 // A4 Landscape height in points

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 41, 66)
            textSize = 14f
            isFakeBoldText = true
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(60, 72, 88)
            textSize = 9.5f
        }
        val headerBgPaint = Paint().apply {
            color = Color.rgb(15, 41, 66)
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.5f
            isFakeBoldText = true
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 35, 42)
            textSize = 8f
        }
        val linePaint = Paint().apply {
            color = Color.rgb(210, 218, 226)
            strokeWidth = 0.8f
        }

        doc.sheets.forEachIndexed { idx, sheet ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, idx + 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var y = 36f
            canvas.drawText(doc.orgHeader, 32f, y, titlePaint)
            y += 18f
            canvas.drawText("${doc.reportTitle} — ${sheet.sheetTitle}", 32f, y, titlePaint)
            y += 15f
            canvas.drawText(
                "Период: ${doc.periodLabel}   |   Дата формирования: ${doc.generatedAtLabel}   |   Подготовил: ${doc.preparedByLabel}",
                32f,
                y,
                metaPaint
            )
            y += 14f
            canvas.drawText("Охват: ${doc.roleScopeLabel}", 32f, y, metaPaint)
            y += 14f

            // Table Header
            canvas.drawRect(32f, y, pageWidth - 32f, y + 20f, headerBgPaint)
            val colX = floatArrayOf(36f, 62f, 175f, 285f, 485f, 545f, 615f, 680f, 740f)
            val shortHeaders = listOf("№", "ФИО исполнителя", "Бригада", "Задача", "Приор.", "Статус", "Срок", "Фото", "Контроль")
            shortHeaders.forEachIndexed { cIdx, hText ->
                canvas.drawText(hText, colX[cIdx], y + 13f, headerTextPaint)
            }
            y += 24f

            sheet.rows.take(16).forEach { row ->
                val cells = listOf(
                    row.index.toString(),
                    row.assigneeFio.take(20),
                    row.groupName.take(18),
                    row.taskTitle.take(36),
                    row.priorityLabel.take(8),
                    row.statusLabel.take(12),
                    row.deadlineLabel.take(11),
                    row.photoInfo.take(10),
                    row.commentSummary.take(18)
                )
                val strike = row.statusLabel == TaskStatus.APPROVED.titleRu
                cellPaint.isStrikeThruText = strike
                cells.forEachIndexed { cIdx, text ->
                    canvas.drawText(text, colX[cIdx], y + 11f, cellPaint)
                }
                cellPaint.isStrikeThruText = false
                y += 18f
                canvas.drawLine(32f, y, pageWidth - 32f, y, linePaint)
            }

            // Footer Signatures
            var sigY = pageHeight - 62f
            doc.signatureBlock.forEach { sig ->
                canvas.drawText(sig, 32f, sigY, metaPaint)
                sigY += 15f
            }

            pdf.finishPage(page)
        }

        FileOutputStream(outFile).use { fos ->
            pdf.writeTo(fos)
        }
        pdf.close()
    }

    // --- 2.CSV WRITER (UTF-8 with BOM for Excel compatibility) ---
    private fun writeCsvFile(outFile: File, doc: GeneratedDocumentResult) {
        val sb = StringBuilder()
        sb.append('\uFEFF') // UTF-8 BOM
        sb.appendLine("Организация;${escapeCsv(doc.orgHeader)}")
        sb.appendLine("Отчёт;${escapeCsv(doc.reportTitle)}")
        sb.appendLine("Период;${escapeCsv(doc.periodLabel)};Дата формирования;${escapeCsv(doc.generatedAtLabel)}")
        sb.appendLine("Подготовил;${escapeCsv(doc.preparedByLabel)};Режим листов;${escapeCsv(doc.sheetLayout.titleRu)}")
        sb.appendLine()

        doc.sheets.forEach { sheet ->
            sb.appendLine("=== ${escapeCsv(sheet.sheetTitle)} ===")
            sb.appendLine(sheet.headers.joinToString(";") { escapeCsv(it) })
            sheet.rows.forEach { row ->
                sb.appendLine(
                    listOf(
                        row.index.toString(),
                        row.assigneeFio,
                        row.groupName,
                        row.taskTitle,
                        row.priorityLabel,
                        row.statusLabel,
                        row.deadlineLabel,
                        row.photoInfo,
                        row.carryoverInfo,
                        row.commentSummary
                    ).joinToString(";") { escapeCsv(it) }
                )
            }
            sb.appendLine()
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

    // --- 3. XLSX WRITER (OpenXML SpreadsheetML with 1 or 2 real sheets) ---
    private fun writeXlsxFile(outFile: File, doc: GeneratedDocumentResult) {
        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            // [Content_Types].xml
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

            // _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()

            // xl/workbook.xml
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            val sheetsXml = doc.sheets.mapIndexed { idx, s ->
                val shortSheetName = if (doc.sheets.size > 1) {
                    if (idx == 0) "Лист 1 - План" else "Лист 2 - Отчёт"
                } else {
                    "План и Отчёт"
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

            // xl/_rels/workbook.xml.rels
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

            // xl/worksheets/sheetN.xml
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
                appendStringRow(listOf(sheet.sheetTitle, "Период: ${doc.periodLabel}", "Подготовил: ${doc.preparedByLabel}"))
                appendStringRow(sheet.headers)
                sheet.rows.forEach { r ->
                    appendStringRow(
                        listOf(
                            r.index.toString(),
                            r.assigneeFio,
                            r.groupName,
                            r.taskTitle,
                            r.priorityLabel,
                            r.statusLabel,
                            r.deadlineLabel,
                            r.photoInfo,
                            r.carryoverInfo,
                            r.commentSummary
                        )
                    )
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

    // --- 4. DOCX WRITER (OpenXML WordprocessingML with page break for Split-Sheets) ---
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
                appendParagraph("Охват: ${doc.roleScopeLabel}")
                sheet.rows.forEach { r ->
                    appendParagraph(
                        "${r.index}. ${r.assigneeFio} (${r.groupName}) — ${r.taskTitle} | Статус: ${r.statusLabel} | Срок: ${r.deadlineLabel} | Фото: ${r.photoInfo} | ${r.carryoverInfo}"
                    )
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
