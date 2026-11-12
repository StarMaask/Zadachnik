package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import java.io.File
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CorporateEmployee
import com.example.data.model.ExportFormat
import com.example.data.model.GeneratedReportDocument
import com.example.data.model.ReportTemplateDocument
import com.example.data.model.ReportType
import com.example.data.model.SheetLayout
import com.example.data.model.TaskStatus
import com.example.data.seed.CorporateSeedData
import com.example.domain.reporting.GeneratedDocumentResult
import com.example.domain.reporting.ReportDocumentGenerator
import com.example.domain.reporting.ReportSectionBlock
import com.example.domain.reporting.ReportSectionKind

@Composable
fun ReportingCenterScreen(
    activeEmployee: CorporateEmployee,
    templates: List<ReportTemplateDocument>,
    generatedReports: List<GeneratedReportDocument>,
    currentEpochDay: Long = CorporateSeedData.BASE_EPOCH_DAY,
    organizationName: String = CorporateSeedData.DEFAULT_ORG_NAME,
    siteName: String = CorporateSeedData.DEFAULT_SITE_NAME,
    autoPlanEvening: Boolean,
    autoReportNight: Boolean,
    onToggleAutoPlan: (Boolean) -> Unit,
    onToggleAutoReport: (Boolean) -> Unit,
    onOpenOrganizationSettings: () -> Unit = {},
    onGenerateReport: (
        template: ReportTemplateDocument,
        layout: SheetLayout,
        format: ExportFormat,
        saveToHistory: Boolean,
        includePhotos: Boolean
    ) -> Unit,
    onSaveCustomTemplate: (ReportTemplateDocument) -> Unit
) {
    val context = LocalContext.current
    var selectedLayout by remember { mutableStateOf(SheetLayout.SPLIT_SHEETS) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var includePhotosInReport by remember { mutableStateOf(true) }
    var showSettingsSubmenu by remember { mutableStateOf(false) }
    var showCustomTemplateDialog by remember { mutableStateOf(false) }
    var lastDownloadedNotice by remember { mutableStateOf<String?>(null) }

    val todayFormatted = remember(currentEpochDay) {
        ReportDocumentGenerator.formatEpochDay(currentEpochDay)
    }
    val tomorrowFormatted = remember(currentEpochDay) {
        ReportDocumentGenerator.formatEpochDay(currentEpochDay + 1L)
    }

    val allReportCatalog = remember(templates, todayFormatted, organizationName, siteName) {
        val headerText = "$organizationName • $siteName"
        val normalizedTemplates = templates.map { tpl ->
            tpl.copy(
                name = ReportDocumentGenerator.resolveTemplateDisplayName(tpl, currentEpochDay),
                orgHeader = headerText
            )
        }
        val existingTypes = normalizedTemplates.map { it.parsedReportType }.toSet()
        val extraTemplates = ReportType.entries
            .filter { it !in existingTypes }
            .map { rType ->
                ReportTemplateDocument(
                    id = "catalog_${rType.code.lowercase()}",
                    name = rType.formatTitleWithDate(todayFormatted),
                    reportType = rType.code,
                    sheetLayout = SheetLayout.SPLIT_SHEETS.code,
                    exportFormat = ExportFormat.PDF.code,
                    orgHeader = headerText
                )
            }
        normalizedTemplates + extraTemplates
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reporting_center_list"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Главная панель настройки выгрузки: Компоновка листов (На 2 листах / На 1 листе), Фото (С фото / Без фото), Формат (PDF / XLSX / DOCX / CSV)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Формирование отчётов и планов",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$organizationName • $siteName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Отчёт за $todayFormatted + План на $tomorrowFormatted",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(onClick = { showSettingsSubmenu = !showSettingsSubmenu }) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showSettingsSubmenu) "Скрыть" else "Ещё")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. ЯВНЫЙ ВЫБОР КОМПОНОВКИ ЛИСТОВ (2 КНОПКИ РЯДОМ БЕЗ ПРОКРУТКИ: НА РАЗДЕЛЬНЫХ СТРАНИЦАХ ИЛИ НА ОДНОЙ)
                    Text(
                        text = "1. Размещение плана и отчёта в документе (выберите режим):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isSplit = selectedLayout == SheetLayout.SPLIT_SHEETS
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSplit) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSplit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedLayout = SheetLayout.SPLIT_SHEETS }
                                .testTag("sheet_layout_SPLIT_SHEETS")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "На раздельных страницах",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSplit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Стр. 1: Отчёт • Стр. 2: План",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSplit) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        val isSingle = selectedLayout == SheetLayout.SINGLE_SHEET
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSingle) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSingle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedLayout = SheetLayout.SINGLE_SHEET }
                                .testTag("sheet_layout_SINGLE_SHEET")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "На одной странице",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSingle) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Блок 1: Отчёт + Блок 2: План",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSingle) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }

                    // Пояснение выбранного режима компоновки в PDF
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = if (selectedLayout == SheetLayout.SPLIT_SHEETS) {
                                "Выбрано: НА РАЗДЕЛЬНЫХ СТРАНИЦАХ — Страница 1: зелёная шапка «ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ ($todayFormatted)»; Страница 2: синяя шапка «ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ ($tomorrowFormatted)»."
                            } else {
                                "Выбрано: НА ОДНОЙ СТРАНИЦЕ — На одном общем листе сверху «БЛОК 1. ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ ($todayFormatted)», снизу «БЛОК 2. ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ ($tomorrowFormatted)»."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. Выбор режима фотоотчёта и формата файла
                    Text(
                        text = "2. Режим фотоотчёта и формат выгрузки:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = includePhotosInReport,
                            onClick = { includePhotosInReport = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = { Text("С фото") },
                            modifier = Modifier.testTag("report_mode_with_photos")
                        )
                        FilterChip(
                            selected = !includePhotosInReport,
                            onClick = { includePhotosInReport = false },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.HideImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = { Text("Без фото") },
                            modifier = Modifier.testTag("report_mode_without_photos")
                        )
                        ExportFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = { Text(fmt.code) },
                                modifier = Modifier.testTag("export_format_${fmt.code}")
                            )
                        }
                    }

                    AnimatedVisibility(visible = showSettingsSubmenu) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Автовыгрузка плана (19:00)", style = MaterialTheme.typography.bodySmall)
                                Switch(checked = autoPlanEvening, onCheckedChange = onToggleAutoPlan)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Автовыгрузка отчета за текущий день ($todayFormatted) в 21:00", style = MaterialTheme.typography.bodySmall)
                                Switch(checked = autoReportNight, onCheckedChange = onToggleAutoReport)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onOpenOrganizationSettings,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_org_settings_from_reports")
                                ) {
                                    Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Организация и участок", style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = { showCustomTemplateDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_template_constructor_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Свой шаблон", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Уведомление об успешном скачивании в папку «Загрузки» телефона
        lastDownloadedNotice?.let { noticeText ->
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = noticeText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { lastDownloadedNotice = null },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }

        // Лаконичный табличный список видов отчётов с прямым выбором компоновки и скачиванием в «Загрузки» телефона
        items(allReportCatalog, key = { it.id.ifBlank { it.name } }) { tpl ->
            val rType = tpl.parsedReportType
            val displayName = ReportDocumentGenerator.resolveTemplateDisplayName(tpl, currentEpochDay)
            val isLockedForWorker = rType.isConsolidatedOnly && !activeEmployee.role.canExportConsolidatedPersonnelReport()

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = rType.subtitleRu,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Быстрый переключатель режима страниц прямо внутри карточки отчёта
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isSplit = selectedLayout == SheetLayout.SPLIT_SHEETS
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSplit) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSplit) 1.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedLayout = SheetLayout.SPLIT_SHEETS }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = "На 2 страницах (Отчёт / План)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSplit) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSplit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            val isSingle = selectedLayout == SheetLayout.SINGLE_SHEET
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSingle) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSingle) 1.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedLayout = SheetLayout.SINGLE_SHEET }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = "На 1 странице (Отчёт + План)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSingle) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSingle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isLockedForWorker) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Доступно для ИТР и Руководителя",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onGenerateReport(
                                        tpl.copy(name = displayName),
                                        selectedLayout,
                                        selectedFormat,
                                        false,
                                        includePhotosInReport
                                    )
                                },
                                modifier = Modifier
                                    .weight(0.95f)
                                    .testTag("preview_report_${rType.code}"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (includePhotosInReport) "Просмотр (с фото)" else "Просмотр (без фото)",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = {
                                    onGenerateReport(
                                        tpl.copy(name = displayName),
                                        selectedLayout,
                                        selectedFormat,
                                        true,
                                        includePhotosInReport
                                    )
                                    lastDownloadedNotice = "Файл «$displayName» (${selectedFormat.code}, ${selectedLayout.shortRu}) сохранён в папку «Загрузки» телефона."
                                },
                                modifier = Modifier
                                    .weight(1.15f)
                                    .testTag("generate_report_${rType.code}"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Скачать ${selectedFormat.code} в Загрузки",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        if (generatedReports.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Сформированные файлы в памяти и Загрузках (${generatedReports.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            items(generatedReports, key = { it.id.ifBlank { it.filePath } }) { rep ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rep.templateName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${rep.periodLabel} • Папка «Загрузки» / ${rep.filePath.substringAfterLast('/')}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                val srcFile = File(rep.filePath)
                                val fmt = ExportFormat.entries.firstOrNull {
                                    it.code.equals(rep.exportFormat, ignoreCase = true)
                                } ?: ExportFormat.PDF
                                val savedRes = ReportDocumentGenerator.saveFileToPhoneDownloads(
                                    context = context,
                                    sourceFile = srcFile,
                                    mimeType = fmt.mimeType,
                                    showToast = true
                                )
                                if (savedRes.isSuccess) {
                                    lastDownloadedNotice = "Повторно сохранено в «Загрузки»: ${srcFile.name}"
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("download_history_file_${rep.id}")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("В Загрузки (${rep.exportFormat})", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    if (showCustomTemplateDialog) {
        CustomTemplateConstructorDialog(
            defaultOrgHeader = "$organizationName • $siteName",
            onDismiss = { showCustomTemplateDialog = false },
            onSave = { newTpl ->
                onSaveCustomTemplate(newTpl)
                showCustomTemplateDialog = false
            }
        )
    }
}

/**
 * Диалог предпросмотра документа:
 * - Наглядно показывает разделение «ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ (ФАКТ)» и «ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ»
 *   как в режиме раздельных листов (2 вкладки), так и на одном совмещённом листе (2 отдельных блока друг под другом).
 */
@Composable
fun ReportPreviewDialog(
    result: GeneratedDocumentResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeSheetTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.86f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Закрепленная верхняя шапка
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.reportTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = result.orgHeader,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${result.exportFormat.code} • ${result.sheetLayout.shortRu} • ${if (result.includePhotos) "С фотоотчётом" else "Без фото"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (result.sheets.size > 1) {
                        TabRow(selectedTabIndex = activeSheetTab) {
                            result.sheets.forEachIndexed { index, sheet ->
                                val isPlanSheet = sheet.sections.firstOrNull()?.kind == ReportSectionKind.NEXT_DAY_PLAN
                                Tab(
                                    selected = activeSheetTab == index,
                                    onClick = { activeSheetTab = index },
                                    text = {
                                        Text(
                                            text = if (isPlanSheet) {
                                                "Лист ${index + 1}: ПЛАН НА СЛЕД. ДЕНЬ (${sheet.rows.size})"
                                            } else {
                                                "Лист ${index + 1}: ОТЧЁТ ЗА ДЕНЬ (${sheet.rows.size})"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        // Баннер режима «На одном листе»
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "РЕЖИМ «НА ОДНОМ ЛИСТЕ»: Ниже на одном листе расположены 2 отдельных раздела — 1) Отчёт за текущий день и 2) План работ на следующий день",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Прокручиваемая средняя область с явными блоками разделов
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val currentSheet = result.sheets.getOrElse(activeSheetTab) { result.sheets.first() }
                        val sectionsToDisplay = currentSheet.sections.ifEmpty {
                            listOf(
                                ReportSectionBlock(
                                    kind = ReportSectionKind.DAILY_FACT_REPORT,
                                    bannerTitle = currentSheet.sheetTitle,
                                    bannerSubtitle = currentSheet.subtitle,
                                    dateLabel = result.periodLabel,
                                    emptyStateMessage = "Нет задач за выбранный период.",
                                    headers = currentSheet.headers,
                                    rows = currentSheet.rows
                                )
                            )
                        }

                        sectionsToDisplay.forEachIndexed { secIdx, section ->
                            val isPlanSection = section.kind == ReportSectionKind.NEXT_DAY_PLAN
                            val sectionHeaderColor = if (isPlanSection) Color(0xFF1E40AF) else Color(0xFF0F766E)
                            val sectionBgTint = if (isPlanSection) Color(0xFFDBEAFE) else Color(0xFFCCFBF1)
                            val sectionBadgeTextColor = if (isPlanSection) Color(0xFF1E3A8A) else Color(0xFF115E59)

                            if (secIdx > 0) {
                                // Явный визуальный разделитель между Отчётом за день и Планом на следующий день на одном листе
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "▼ ВТОРОЙ РАЗДЕЛ НА ЭТОМ ЖЕ ЛИСТЕ: ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ ▼",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Контейнер раздела (Отчёт за день ИЛИ План на следующий день)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, sectionHeaderColor),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    // Яркая шапка раздела
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(sectionHeaderColor)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = section.bannerTitle,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${section.bannerSubtitle} • Позиций: ${section.rows.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.88f)
                                            )
                                        }
                                    }

                                    if (section.rows.isEmpty()) {
                                        Text(
                                            text = section.emptyStateMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    } else {
                                        section.rows.forEachIndexed { idx, row ->
                                            val isApproved = row.statusLabel == TaskStatus.APPROVED.titleRu
                                            val deco = if (isApproved && !isPlanSection) TextDecoration.LineThrough else TextDecoration.None
                                            val isPartialVolume = row.completionPercent in 0..99 && row.volumeInfo.startsWith("Не до конца")

                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(5.dp))
                                                                .background(sectionBgTint)
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = row.rowTypeBadge,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = sectionBadgeTextColor
                                                            )
                                                        }
                                                        Text(
                                                            text = "${row.index}. ${row.taskTitle}",
                                                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = deco),
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = row.statusLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = sectionHeaderColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (isPlanSection) {
                                                        "Плановый объём на смену: ${row.planVolumeLabel.ifBlank { row.volumeInfo }}"
                                                    } else {
                                                        "Фактический объём работ: ${row.factVolumeLabel.ifBlank { row.volumeInfo }}"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isPartialVolume && !isPlanSection) {
                                                        Color(0xFFB45309)
                                                    } else if (row.completionPercent >= 100 && !isPlanSection) {
                                                        Color(0xFF15803D)
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )

                                                if (row.checklistBreakdown.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Text(
                                                                text = if (isPlanSection) {
                                                                    "Пункты чек-листа в плане:"
                                                                } else {
                                                                    "Выполненный объём по пунктам чек-листа:"
                                                                },
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = sectionHeaderColor
                                                            )
                                                            row.checklistBreakdown.forEach { subLine ->
                                                                Text(
                                                                    text = "• $subLine",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(3.dp))
                                                val metaLine = buildString {
                                                    append("Исполнитель: ${row.assigneeFio} (${row.groupName}) • Срок: ${row.deadlineLabel}")
                                                    if (result.includePhotos) {
                                                        append(" • Фото: ${row.photoInfo}")
                                                    }
                                                }
                                                Text(
                                                    text = metaLine,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (idx < section.rows.lastIndex) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (result.includePhotos && result.attachedPhotos.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "В выгруженный файл включено приложение с фотофиксацией (${result.attachedPhotos.size} фото).",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Закрепленная нижняя панель: Скачать в папку «Загрузки» телефона + Открыть + Поделиться
                    var downloadedBannerInDialog by remember(result.downloadedToPath) {
                        mutableStateOf(
                            if (result.downloadedToPath.isNotBlank()) {
                                "✓ Файл скачан в папку «Загрузки» телефона: ${result.file.name}"
                            } else {
                                null
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        downloadedBannerInDialog?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val saved = ReportDocumentGenerator.saveReportToPhoneDownloads(
                                        context = context,
                                        result = result,
                                        showToast = true
                                    )
                                    if (saved.isSuccess) {
                                        downloadedBannerInDialog = "✓ Файл сохранён в папку «Загрузки» телефона: ${result.file.name}"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF059669),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("download_to_phone_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Скачать в Загрузки",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    ReportDocumentGenerator.openGeneratedReport(context, result)
                                },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("open_report_file_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Открыть", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = {
                                    ReportDocumentGenerator.shareGeneratedReport(context, result, "SHARE")
                                },
                                modifier = Modifier
                                    .weight(0.95f)
                                    .testTag("share_report_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Поделиться", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomTemplateConstructorDialog(
    defaultOrgHeader: String = "${CorporateSeedData.DEFAULT_ORG_NAME} • ${CorporateSeedData.DEFAULT_SITE_NAME}",
    onDismiss: () -> Unit,
    onSave: (ReportTemplateDocument) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var orgHeader by remember(defaultOrgHeader) { mutableStateOf(defaultOrgHeader) }
    var selectedLayout by remember { mutableStateOf(SheetLayout.SPLIT_SHEETS) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var groupBy by remember { mutableStateOf("ASSIGNEE") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Новый шаблон отчёта",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название шаблона *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = orgHeader,
                    onValueChange = { orgHeader = it },
                    label = { Text("Заголовок документа (Организация и участок)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Компоновка листов:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SheetLayout.entries.forEach { layout ->
                        FilterChip(
                            selected = selectedLayout == layout,
                            onClick = { selectedLayout = layout },
                            label = { Text(layout.shortRu) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ASSIGNEE" to "По ФИО", "GROUP" to "По группе", "STATUS" to "По статусу").forEach { (code, label) ->
                        FilterChip(
                            selected = groupBy == code,
                            onClick = { groupBy = code },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    ReportTemplateDocument(
                                        name = name.trim(),
                                        reportType = ReportType.CUSTOM.code,
                                        sheetLayout = selectedLayout.code,
                                        exportFormat = selectedFormat.code,
                                        orgHeader = orgHeader.trim(),
                                        defaultPeriodDays = 7L,
                                        groupByField = groupBy,
                                        includeSignatures = true
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}
