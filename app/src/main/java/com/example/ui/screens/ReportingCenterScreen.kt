package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CorporateEmployee
import com.example.data.model.ExportFormat
import com.example.data.model.GeneratedReportDocument
import com.example.data.model.ReportTemplateDocument
import com.example.data.model.ReportType
import com.example.data.model.SheetLayout
import com.example.data.model.TaskDocument
import com.example.data.model.TaskStatus
import com.example.data.seed.CorporateSeedData
import com.example.domain.reporting.GeneratedDocumentResult
import com.example.domain.reporting.ReportDocumentGenerator
import com.example.domain.reporting.ReportSectionBlock
import com.example.domain.reporting.ReportSectionKind
import com.example.ui.components.TaskItemCard
import com.example.ui.theme.LocalAppThemeTokens
import java.io.File

/**
 * Смысловые уровни (категории) отчётов в премиальной дизайн-системе:
 * 1. Принятые / Архив заданий (скрытые с главной страницы после подтверждения)
 * 2. Ежедневные (Оперативные)
 * 3. Аналитика (Для ИТР)
 * 4. Конструктор
 */
private enum class ReportCategoryTab(
    val code: String,
    val tabLabel: String,
    val sectionTitle: String,
    val sectionSubtitle: String,
    val accentColor: Color
) {
    ALL(
        code = "ALL",
        tabLabel = "Все",
        sectionTitle = "Все отчёты",
        sectionSubtitle = "Полный каталог оперативных и аналитических сводок",
        accentColor = Color(0xFF2563EB)
    ),
    ACCEPTED_ARCHIVE(
        code = "ACCEPTED_ARCHIVE",
        tabLabel = "Принятые задания",
        sectionTitle = "Принятые отчёты и архив заданий",
        sectionSubtitle = "Подтверждённые отчёты по заданиям, перенесённые с главной страницы",
        accentColor = Color(0xFF059669)
    ),
    DAILY_OPERATIONAL(
        code = "DAILY_OPERATIONAL",
        tabLabel = "Ежедневные",
        sectionTitle = "Ежедневные (Оперативные)",
        sectionSubtitle = "Отчёт за смену, план-наряд на завтра и сводка работ по участку",
        accentColor = Color(0xFF2563EB)
    ),
    ITR_ANALYTICS(
        code = "ITR_ANALYTICS",
        tabLabel = "Аналитика ИТР",
        sectionTitle = "Аналитика (Для ИТР)",
        sectionSubtitle = "Контроль фотофиксации, реестр просрочек, дисциплина SLA и выработка бригад",
        accentColor = Color(0xFF7C3AED)
    ),
    CONSTRUCTOR(
        code = "CONSTRUCTOR",
        tabLabel = "Конструктор",
        sectionTitle = "Конструктор",
        sectionSubtitle = "Пользовательские шаблоны с настраиваемыми колонками и группировкой",
        accentColor = Color(0xFFD97706)
    )
}

private data class ReportVisualSpec(
    val category: ReportCategoryTab,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBgColor: Color,
    val badgeLabel: String? = null
)

private fun ReportType.visualSpec(): ReportVisualSpec {
    return when (this) {
        ReportType.DAILY_REPORT -> ReportVisualSpec(
            category = ReportCategoryTab.DAILY_OPERATIONAL,
            icon = Icons.Default.CalendarToday,
            iconTint = Color(0xFF059669),
            iconBgColor = Color(0xFF10B981).copy(alpha = 0.14f)
        )
        ReportType.NEXT_DAY_PLAN -> ReportVisualSpec(
            category = ReportCategoryTab.DAILY_OPERATIONAL,
            icon = Icons.Default.EventNote,
            iconTint = Color(0xFF2563EB),
            iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.14f)
        )
        ReportType.PERIOD_REPORT -> ReportVisualSpec(
            category = ReportCategoryTab.DAILY_OPERATIONAL,
            icon = Icons.Default.DateRange,
            iconTint = Color(0xFF0284C7),
            iconBgColor = Color(0xFF0EA5E9).copy(alpha = 0.14f)
        )
        ReportType.PHOTO_QUALITY -> ReportVisualSpec(
            category = ReportCategoryTab.ITR_ANALYTICS,
            icon = Icons.Default.PhotoCamera,
            iconTint = Color(0xFF7C3AED),
            iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.14f),
            badgeLabel = "Фотоконтроль"
        )
        ReportType.OVERDUE_CARRYOVER -> ReportVisualSpec(
            category = ReportCategoryTab.ITR_ANALYTICS,
            icon = Icons.Default.HistoryToggleOff,
            iconTint = Color(0xFFDC2626),
            iconBgColor = Color(0xFFEF4444).copy(alpha = 0.13f),
            badgeLabel = "Дедлайны"
        )
        ReportType.DISCIPLINE_RATING -> ReportVisualSpec(
            category = ReportCategoryTab.ITR_ANALYTICS,
            icon = Icons.Default.Insights,
            iconTint = Color(0xFF7C3AED),
            iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.14f),
            badgeLabel = "ИТР+"
        )
        ReportType.APPROVAL_SLA -> ReportVisualSpec(
            category = ReportCategoryTab.ITR_ANALYTICS,
            icon = Icons.Default.Speed,
            iconTint = Color(0xFF6D28D9),
            iconBgColor = Color(0xFF7C3AED).copy(alpha = 0.14f),
            badgeLabel = "ИТР+"
        )
        ReportType.GROUP_SITE_SUMMARY -> ReportVisualSpec(
            category = ReportCategoryTab.ITR_ANALYTICS,
            icon = Icons.Default.Groups,
            iconTint = Color(0xFF4F46E5),
            iconBgColor = Color(0xFF6366F1).copy(alpha = 0.14f),
            badgeLabel = "ИТР+"
        )
        ReportType.IDLE_ACTIVITY -> ReportVisualSpec(
            category = ReportCategoryTab.ITR_ANALYTICS,
            icon = Icons.Default.Timeline,
            iconTint = Color(0xFFB45309),
            iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.15f),
            badgeLabel = "ИТР+"
        )
        ReportType.RECURRING_EXECUTION -> ReportVisualSpec(
            category = ReportCategoryTab.ITR_ANALYTICS,
            icon = Icons.Default.Assignment,
            iconTint = Color(0xFF0D9488),
            iconBgColor = Color(0xFF14B8A6).copy(alpha = 0.14f)
        )
        ReportType.PERIOD_COMPARISON -> ReportVisualSpec(
            category = ReportCategoryTab.ITR_ANALYTICS,
            icon = Icons.Default.PublishedWithChanges,
            iconTint = Color(0xFF4338CA),
            iconBgColor = Color(0xFF6366F1).copy(alpha = 0.14f)
        )
        ReportType.INDIVIDUAL_1ON1 -> ReportVisualSpec(
            category = ReportCategoryTab.ITR_ANALYTICS,
            icon = Icons.Default.PersonSearch,
            iconTint = Color(0xFF0369A1),
            iconBgColor = Color(0xFF0EA5E9).copy(alpha = 0.14f)
        )
        ReportType.CUSTOM -> ReportVisualSpec(
            category = ReportCategoryTab.CONSTRUCTOR,
            icon = Icons.Default.FolderSpecial,
            iconTint = Color(0xFFD97706),
            iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.15f),
            badgeLabel = "Шаблон"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportingCenterScreen(
    activeEmployee: CorporateEmployee,
    templates: List<ReportTemplateDocument>,
    generatedReports: List<GeneratedReportDocument>,
    allTasks: List<TaskDocument> = emptyList(),
    onTaskClick: (TaskDocument) -> Unit = {},
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
    val tokens = LocalAppThemeTokens.current
    val tonalSurface = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = if (tokens.isDark) 0.28f else 0.44f
    )
    var selectedLayout by remember { mutableStateOf(SheetLayout.SPLIT_SHEETS) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var includePhotosInReport by remember { mutableStateOf(true) }
    var showDefaultParamsAccordion by remember { mutableStateOf(false) }
    var selectedCategoryTab by remember { mutableStateOf(ReportCategoryTab.ALL) }
    var selectedReportEpochDay by remember(currentEpochDay) { mutableLongStateOf(currentEpochDay) }
    var showCustomTemplateDialog by remember { mutableStateOf(false) }
    var activeActionSheetTemplate by remember { mutableStateOf<ReportTemplateDocument?>(null) }
    var lastDownloadedNotice by remember { mutableStateOf<String?>(null) }

    val todayFormatted = remember(selectedReportEpochDay) {
        ReportDocumentGenerator.formatEpochDay(selectedReportEpochDay)
    }
    val tomorrowFormatted = remember(selectedReportEpochDay) {
        ReportDocumentGenerator.formatEpochDay(selectedReportEpochDay + 1L)
    }
    val isTodaySelected = selectedReportEpochDay == currentEpochDay

    val isAdminOrHead = activeEmployee.role.isAdminOrHead

    val allReportCatalog = remember(
        templates,
        todayFormatted,
        organizationName,
        siteName,
        activeEmployee.groupName,
        activeEmployee.siteName,
        selectedReportEpochDay,
        isAdminOrHead
    ) {
        val headerText = ReportDocumentGenerator.formatOrgHeader(
            organizationName = organizationName,
            subdivisionName = activeEmployee.groupName,
            siteName = activeEmployee.siteName.ifBlank { siteName }
        )
        val normalizedTemplates = templates.map { tpl ->
            tpl.copy(
                name = ReportDocumentGenerator.resolveTemplateDisplayName(tpl, selectedReportEpochDay),
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
        // Сортируем в логическом порядке: сначала DAILY_REPORT, затем NEXT_DAY_PLAN, PERIOD_REPORT и далее
        val orderPriority = listOf(
            ReportType.DAILY_REPORT,
            ReportType.NEXT_DAY_PLAN,
            ReportType.PERIOD_REPORT,
            ReportType.PHOTO_QUALITY,
            ReportType.OVERDUE_CARRYOVER,
            ReportType.DISCIPLINE_RATING,
            ReportType.APPROVAL_SLA,
            ReportType.GROUP_SITE_SUMMARY,
            ReportType.IDLE_ACTIVITY,
            ReportType.RECURRING_EXECUTION,
            ReportType.PERIOD_COMPARISON,
            ReportType.INDIVIDUAL_1ON1,
            ReportType.CUSTOM
        )
        (normalizedTemplates + extraTemplates)
            .filter { tpl ->
                // Контроль SLA проверяющих ИТР доступен только Руководителю / Администратору
                if (!isAdminOrHead && tpl.parsedReportType == ReportType.APPROVAL_SLA) {
                    false
                } else {
                    true
                }
            }
            .sortedBy { tpl ->
                val idx = orderPriority.indexOf(tpl.parsedReportType)
                if (idx >= 0) idx else 999
            }
    }

    val dailyOperationalList = remember(allReportCatalog) {
        allReportCatalog.filter { it.parsedReportType.visualSpec().category == ReportCategoryTab.DAILY_OPERATIONAL }
    }
    val itrAnalyticsList = remember(allReportCatalog) {
        allReportCatalog.filter { it.parsedReportType.visualSpec().category == ReportCategoryTab.ITR_ANALYTICS }
    }
    val constructorList = remember(allReportCatalog) {
        allReportCatalog.filter { it.parsedReportType.visualSpec().category == ReportCategoryTab.CONSTRUCTOR }
    }
    val acceptedAndArchivedTasks = remember(allTasks) {
        allTasks.filter {
            it.taskStatus == TaskStatus.APPROVED || it.taskStatus == TaskStatus.ARCHIVED
        }.sortedByDescending { it.dueDateEpochDay }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reporting_center_list"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. УМНЫЙ ВЕРХНИЙ БЛОК: Заголовок «Отчёты», компактный селектор даты и тонкая строка параметров по умолчанию
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Строка заголовка и селектора даты смены
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Отчёты",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "$organizationName • $siteName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Компактный тональный селектор даты формирования без тяжёлых рамок
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = tonalSurface
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { selectedReportEpochDay -= 1L },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Предыдущий день",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedReportEpochDay = currentEpochDay }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isTodaySelected) "Сегодня • $todayFormatted" else todayFormatted,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "План: $tomorrowFormatted",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { selectedReportEpochDay += 1L },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Следующий день",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Ультра-компактная строка «Параметры по умолчанию» (в одну тонкую линию)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = tonalSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDefaultParamsAccordion = !showDefaultParamsAccordion }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("toggle_default_export_params"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "По умолчанию: ${selectedFormat.code} • ${if (selectedLayout == SheetLayout.SPLIT_SHEETS) "2 стр. (Отчёт / План)" else "1 стр. (Отчёт + План)"} • ${if (includePhotosInReport) "С фото" else "Без фото"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (showDefaultParamsAccordion) "Свернуть" else "Изменить",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = if (showDefaultParamsAccordion) {
                                        Icons.Default.ExpandLess
                                    } else {
                                        Icons.Default.ExpandMore
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // Скрытые по умолчанию настройки (раскрываются в аккордеон по нажатию)
                        AnimatedVisibility(
                            visible = showDefaultParamsAccordion,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )

                                Text(
                                    text = "Размещение отчёта и плана в документе",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

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
                                            MaterialTheme.colorScheme.surface
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedLayout = SheetLayout.SPLIT_SHEETS }
                                            .testTag("sheet_layout_SPLIT_SHEETS")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "На 2 страницах",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSplit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Стр. 1: Отчёт • Стр. 2: План",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSplit) {
                                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
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
                                            MaterialTheme.colorScheme.surface
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedLayout = SheetLayout.SINGLE_SHEET }
                                            .testTag("sheet_layout_SINGLE_SHEET")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "На 1 странице",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSingle) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Блок 1: Отчёт + Блок 2: План",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSingle) {
                                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Формат файла и фотофиксация",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

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

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Автовыгрузка плана на завтра (19:00)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Switch(checked = autoPlanEvening, onCheckedChange = onToggleAutoPlan)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Автовыгрузка отчёта за смену (21:00)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Switch(checked = autoReportNight, onCheckedChange = onToggleAutoReport)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isAdminOrHead) {
                                        OutlinedButton(
                                            onClick = onOpenOrganizationSettings,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("open_org_settings_from_reports")
                                        ) {
                                            Icon(
                                                Icons.Default.Business,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Организация и участок", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { showCustomTemplateDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_template_constructor_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Свой шаблон", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. СЕГМЕНТИРОВАННЫЕ ТАБЫ КАТЕГОРИЙ ОТЧЁТОВ (Все • Ежедневные • Аналитика ИТР • Конструктор)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReportCategoryTab.entries.forEach { tab ->
                        val isSelected = selectedCategoryTab == tab
                        val countBadge = when (tab) {
                            ReportCategoryTab.ALL -> allReportCatalog.size
                            ReportCategoryTab.ACCEPTED_ARCHIVE -> acceptedAndArchivedTasks.size
                            ReportCategoryTab.DAILY_OPERATIONAL -> dailyOperationalList.size
                            ReportCategoryTab.ITR_ANALYTICS -> itrAnalyticsList.size
                            ReportCategoryTab.CONSTRUCTOR -> constructorList.size
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                tonalSurface
                            },
                            shadowElevation = if (isSelected && !tokens.isDark) 1.5.dp else 0.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedCategoryTab = tab }
                                .testTag("report_category_tab_${tab.code}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = tab.tabLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = countBadge.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
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
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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

        // СЕКЦИЯ 0: Принятые отчёты и архив заданий (перемещённые с главной страницы)
        if (selectedCategoryTab == ReportCategoryTab.ACCEPTED_ARCHIVE ||
            (selectedCategoryTab == ReportCategoryTab.ALL && acceptedAndArchivedTasks.isNotEmpty())
        ) {
            item {
                ReportCategorySectionHeader(
                    category = ReportCategoryTab.ACCEPTED_ARCHIVE,
                    topAirSpacing = 4.dp
                )
            }
            if (acceptedAndArchivedTasks.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = if (tokens.isDark) 0.22f else 0.34f
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF059669).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "Принятых отчётов пока нет",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "После подтверждения отчёты скрываются с главной страницы и сохраняются здесь.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(acceptedAndArchivedTasks, key = { "rep_arch_${it.id}" }) { task ->
                    TaskItemCard(
                        task = task,
                        onClick = { onTaskClick(task) }
                    )
                }
            }
        }

        // СЕКЦИЯ 1: Ежедневные (Оперативные)
        if (selectedCategoryTab == ReportCategoryTab.ALL || selectedCategoryTab == ReportCategoryTab.DAILY_OPERATIONAL) {
            item {
                ReportCategorySectionHeader(
                    category = ReportCategoryTab.DAILY_OPERATIONAL,
                    topAirSpacing = 4.dp
                )
            }
            items(dailyOperationalList, key = { "daily_${it.id.ifBlank { it.name }}" }) { tpl ->
                val displayName = ReportDocumentGenerator.resolveTemplateDisplayName(tpl, selectedReportEpochDay)
                PremiumReportCard(
                    template = tpl,
                    displayName = displayName,
                    selectedLayout = selectedLayout,
                    selectedFormat = selectedFormat,
                    includePhotos = includePhotosInReport,
                    onOpenActionSheet = { activeActionSheetTemplate = tpl.copy(name = displayName) }
                )
            }
        }

        // СЕКЦИЯ 2: Аналитика (Для ИТР)
        if (selectedCategoryTab == ReportCategoryTab.ALL || selectedCategoryTab == ReportCategoryTab.ITR_ANALYTICS) {
            item {
                ReportCategorySectionHeader(
                    category = ReportCategoryTab.ITR_ANALYTICS,
                    topAirSpacing = if (selectedCategoryTab == ReportCategoryTab.ALL) 16.dp else 4.dp
                )
            }
            items(itrAnalyticsList, key = { "itr_${it.id.ifBlank { it.name }}" }) { tpl ->
                val displayName = ReportDocumentGenerator.resolveTemplateDisplayName(tpl, selectedReportEpochDay)
                PremiumReportCard(
                    template = tpl,
                    displayName = displayName,
                    selectedLayout = selectedLayout,
                    selectedFormat = selectedFormat,
                    includePhotos = includePhotosInReport,
                    onOpenActionSheet = { activeActionSheetTemplate = tpl.copy(name = displayName) }
                )
            }
        }

        // СЕКЦИЯ 3: Конструктор (Пользовательский отчёт)
        if (selectedCategoryTab == ReportCategoryTab.ALL || selectedCategoryTab == ReportCategoryTab.CONSTRUCTOR) {
            item {
                ReportCategorySectionHeader(
                    category = ReportCategoryTab.CONSTRUCTOR,
                    topAirSpacing = if (selectedCategoryTab == ReportCategoryTab.ALL) 16.dp else 4.dp,
                    trailingActionLabel = "+ Шаблон",
                    onTrailingAction = { showCustomTemplateDialog = true }
                )
            }
            items(constructorList, key = { "custom_${it.id.ifBlank { it.name }}" }) { tpl ->
                val displayName = ReportDocumentGenerator.resolveTemplateDisplayName(tpl, selectedReportEpochDay)
                PremiumReportCard(
                    template = tpl,
                    displayName = displayName,
                    selectedLayout = selectedLayout,
                    selectedFormat = selectedFormat,
                    includePhotos = includePhotosInReport,
                    onOpenActionSheet = { activeActionSheetTemplate = tpl.copy(name = displayName) }
                )
            }
        }

        // ИСТОРИЯ СФОРМИРОВАННЫХ ФАЙЛОВ В ЗАГРУЗКАХ
        if (generatedReports.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Недавно сформированные файлы (${generatedReports.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(generatedReports, key = { it.id.ifBlank { it.filePath } }) { rep ->
                val isPdf = rep.exportFormat.equals("PDF", ignoreCase = true)
                val badgeTint = if (isPdf) Color(0xFFDC2626) else Color(0xFF059669)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = tokens.cardShadowElevationDp.dp,
                    border = if (tokens.cardBorderWidthDp > 0f) {
                        BorderStroke(tokens.cardBorderWidthDp.dp, tokens.cardBorderColor)
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(badgeTint.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rep.exportFormat.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeTint
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = rep.templateName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${rep.periodLabel} • ${rep.filePath.substringAfterLast('/')}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
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
                                }
                                .testTag("download_history_file_${rep.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Сохранить в Загрузки",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Скачать",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ACTION SHEET ОТЧЁТА: открывается при нажатии на любую карточку отчёта
    activeActionSheetTemplate?.let { sheetTpl ->
        val rType = sheetTpl.parsedReportType
        val isLockedForWorker = rType.isConsolidatedOnly && !activeEmployee.role.canExportConsolidatedPersonnelReport()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { activeActionSheetTemplate = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            ReportActionBottomSheetContent(
                template = sheetTpl,
                isLockedForWorker = isLockedForWorker,
                selectedLayout = selectedLayout,
                selectedFormat = selectedFormat,
                includePhotos = includePhotosInReport,
                todayFormatted = todayFormatted,
                tomorrowFormatted = tomorrowFormatted,
                onSelectLayout = { selectedLayout = it },
                onSelectFormat = { selectedFormat = it },
                onTogglePhotos = { includePhotosInReport = it },
                onPreviewOnScreen = {
                    activeActionSheetTemplate = null
                    onGenerateReport(
                        sheetTpl,
                        selectedLayout,
                        selectedFormat,
                        false,
                        includePhotosInReport
                    )
                },
                onDownloadToPhone = {
                    activeActionSheetTemplate = null
                    onGenerateReport(
                        sheetTpl,
                        selectedLayout,
                        selectedFormat,
                        true,
                        includePhotosInReport
                    )
                    lastDownloadedNotice = "Файл «${sheetTpl.name}» (${selectedFormat.code}, ${selectedLayout.shortRu}) сохранён в папку «Загрузки» телефона."
                },
                onDismiss = { activeActionSheetTemplate = null }
            )
        }
    }

    if (showCustomTemplateDialog) {
        CustomTemplateConstructorDialog(
            defaultOrgHeader = ReportDocumentGenerator.formatOrgHeader(
                organizationName = organizationName,
                subdivisionName = activeEmployee.groupName,
                siteName = activeEmployee.siteName.ifBlank { siteName }
            ),
            onDismiss = { showCustomTemplateDialog = false },
            onSave = { newTpl ->
                onSaveCustomTemplate(newTpl)
                showCustomTemplateDialog = false
            }
        )
    }
}

/**
 * Крупный выразительный заголовок смысловой секции с «воздухом» между категориями отчётов.
 */
@Composable
private fun ReportCategorySectionHeader(
    category: ReportCategoryTab,
    topAirSpacing: androidx.compose.ui.unit.Dp = 8.dp,
    trailingActionLabel: String? = null,
    onTrailingAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topAirSpacing, bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(category.accentColor)
                )
                Text(
                    text = category.sectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (trailingActionLabel != null && onTrailingAction != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = category.accentColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTrailingAction() }
                ) {
                    Text(
                        text = trailingActionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = category.accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = category.sectionSubtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

/**
 * Полностью кликабельная карточка отчёта в стиле Linear / Notion / Stripe / Apple:
 * - Без жёстких рамок, только мягкая подложка и деликатная тень;
 * - Цветная тематическая иконка слева (календарь для ежедневных, график для аналитики, папка для конструктора);
 * - Жирный заголовок + серый подзаголовок;
 * - Ненавязчивая строка метаданных мелким серым шрифтом с мини-иконками;
 * - В углу — фиолетовый бейдж со щитом «ИТР+» (вместо серых замков);
 * - Без дублирующихся кнопок «Сформировать» внутри карточки — вся карточка открывает Bottom Sheet действий.
 */
@Composable
private fun PremiumReportCard(
    template: ReportTemplateDocument,
    displayName: String,
    selectedLayout: SheetLayout,
    selectedFormat: ExportFormat,
    includePhotos: Boolean,
    onOpenActionSheet: () -> Unit
) {
    val tokens = LocalAppThemeTokens.current
    val rType = template.parsedReportType
    val visual = rType.visualSpec()
    val requiresItrPlus = rType.isConsolidatedOnly

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = tokens.cardShadowElevationDp.dp,
        border = if (tokens.cardBorderWidthDp > 0f) {
            BorderStroke(tokens.cardBorderWidthDp.dp, tokens.cardBorderColor)
        } else {
            null
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenActionSheet() }
            .testTag("report_card_${rType.code}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Тематическая цветная иконка типа отчёта
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(visual.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = null,
                    tint = visual.iconTint,
                    modifier = Modifier.size(23.dp)
                )
            }

            // Центральный блок: Заголовок + Бейдж «ИТР+» + Подзаголовок + Ненавязчивая строка параметров
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    if (requiresItrPlus) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ItrPlusPremiumBadge()
                    } else if (visual.badgeLabel != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = visual.iconBgColor
                        ) {
                            Text(
                                text = visual.badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = visual.iconTint,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = rType.subtitleRu,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Ненавязчивая строка метаданных мелким серым шрифтом с мини-иконками
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = selectedFormat.code,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                    Text(
                        text = if (selectedLayout == SheetLayout.SPLIT_SHEETS) "2 стр. (Отчёт / План)" else "1 стр. (Отчёт + План)",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (includePhotos) Icons.Default.PhotoCamera else Icons.Default.HideImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (includePhotos) "С фото" else "Без фото",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                        )
                    }
                }
            }

            // Ненавязчивый шеврон перехода к Action Sheet
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Открыть меню отчёта",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Элегантный фиолетовый бейдж «ИТР+» со щитом в углу аналитических карточек.
 */
@Composable
private fun ItrPlusPremiumBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF7C3AED).copy(alpha = 0.14f),
                        Color(0xFFD97706).copy(alpha = 0.16f)
                    )
                )
            )
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Доступ ИТР+",
                tint = Color(0xFF7C3AED),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "ИТР+",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF6D28D9)
            )
        }
    }
}

/**
 * Контент нижнего меню действий (Action Sheet) при нажатии на карточку отчёта:
 * 1. «Посмотреть на экране»
 * 2. «Скачать в папку Загрузки»
 * 3. «Настройки страниц и формата»
 */
@Composable
private fun ReportActionBottomSheetContent(
    template: ReportTemplateDocument,
    isLockedForWorker: Boolean,
    selectedLayout: SheetLayout,
    selectedFormat: ExportFormat,
    includePhotos: Boolean,
    todayFormatted: String,
    tomorrowFormatted: String,
    onSelectLayout: (SheetLayout) -> Unit,
    onSelectFormat: (ExportFormat) -> Unit,
    onTogglePhotos: (Boolean) -> Unit,
    onPreviewOnScreen: () -> Unit,
    onDownloadToPhone: () -> Unit,
    onDismiss: () -> Unit
) {
    val rType = template.parsedReportType
    val visual = rType.visualSpec()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Шапка выбранного отчёта
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(visual.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = null,
                    tint = visual.iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (rType.isConsolidatedOnly) {
                        ItrPlusPremiumBadge()
                    }
                }
                Text(
                    text = rType.subtitleRu,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

        if (isLockedForWorker) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF7C3AED).copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Корпоративная аналитика уровня ИТР+",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6D28D9)
                        )
                    }
                    Text(
                        text = "Сводная аналитика по подразделению, контроль SLA и рейтинг бригад доступны для ролей ИТР (Инженер / Мастер), Руководитель участка и Администратор.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Понятно")
            }
        } else {
            // Блок «Настройки страниц и формата» внутри Action Sheet
            Text(
                text = "Настройки страниц и формата",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

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
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectLayout(SheetLayout.SPLIT_SHEETS) }
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
                            text = "Стр. 1: Отчёт ($todayFormatted) • Стр. 2: План ($tomorrowFormatted)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSplit) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
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
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectLayout(SheetLayout.SINGLE_SHEET) }
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
                            text = "Отчёт ($todayFormatted) + План ($tomorrowFormatted) вместе",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSingle) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = includePhotos,
                    onClick = { onTogglePhotos(true) },
                    leadingIcon = {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    label = { Text("С фото") }
                )
                FilterChip(
                    selected = !includePhotos,
                    onClick = { onTogglePhotos(false) },
                    leadingIcon = {
                        Icon(Icons.Default.HideImage, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    label = { Text("Без фото") }
                )
                ExportFormat.entries.forEach { fmt ->
                    FilterChip(
                        selected = selectedFormat == fmt,
                        onClick = { onSelectFormat(fmt) },
                        label = { Text(fmt.code) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Главные действия Action Sheet: «Просмотр» и «Скачать»
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPreviewOnScreen,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("action_sheet_preview_button")
                ) {
                    Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Просмотр",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onDownloadToPhone,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.15f)
                        .height(50.dp)
                        .testTag("action_sheet_download_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Скачать ${selectedFormat.code}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Диалог предпросмотра документа:
 * - Наглядно показывает разделение «ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ (ФАКТ)» и «ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ»
 *   как в режиме раздельных листов (2 вкладки), так и на одном совмещённом листе (2 отдельных блока друг под другом).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPreviewDialog(
    result: GeneratedDocumentResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalAppThemeTokens.current
    val emeraldPrimary = tokens.statusDoneColor
    val tonalSurface = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = if (tokens.isDark) 0.26f else 0.36f
    )
    var activeSheetTab by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sheetMaxHeight = (maxHeight * 0.92f).coerceAtLeast(360.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight)
            ) {
                // Шапка предпросмотра
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = result.reportTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = result.orgHeader,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = "${result.exportFormat.code} · ${result.sheetLayout.shortRu}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = tonalSurface
                        ) {
                            Text(
                                text = if (result.includePhotos) "С фото" else "Без фото",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (result.sheets.size > 1) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = tonalSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                result.sheets.forEachIndexed { index, sheet ->
                                    val isPlanSheet = sheet.sections.firstOrNull()?.kind == ReportSectionKind.NEXT_DAY_PLAN
                                    val isSelected = activeSheetTab == index
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                        shadowElevation = if (isSelected && !tokens.isDark) 1.5.dp else 0.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { activeSheetTab = index }
                                    ) {
                                        Text(
                                            text = if (isPlanSheet) {
                                                "План (${sheet.rows.size})"
                                            } else {
                                                "Отчёт (${sheet.rows.size})"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Прокручиваемая средняя область
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
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

                    sectionsToDisplay.forEach { section ->
                        val isPlanSection = section.kind == ReportSectionKind.NEXT_DAY_PLAN
                        val sectionAccent = if (isPlanSection) Color(0xFF2563EB) else Color(0xFF0F766E)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = tonalSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 4.dp, height = 22.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(sectionAccent)
                                        )
                                        Column {
                                            Text(
                                                text = section.bannerTitle,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = section.bannerSubtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = sectionAccent.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "${section.rows.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = sectionAccent,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                if (section.rows.isEmpty()) {
                                    Text(
                                        text = section.emptyStateMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            section.rows.forEachIndexed { idx, row ->
                                                val isApproved = row.statusLabel == TaskStatus.APPROVED.titleRu
                                                val deco = if (isApproved && !isPlanSection) TextDecoration.LineThrough else TextDecoration.None
                                                val isPartialVolume = row.completionPercent in 0..99 && row.volumeInfo.startsWith("Не до конца")

                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
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
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = sectionAccent.copy(alpha = 0.12f)
                                                            ) {
                                                                Text(
                                                                    text = row.rowTypeBadge,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = sectionAccent,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                                                            color = sectionAccent,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    Text(
                                                        text = if (isPlanSection) {
                                                            "План: ${row.planVolumeLabel.ifBlank { row.volumeInfo }}"
                                                        } else {
                                                            "Факт: ${row.factVolumeLabel.ifBlank { row.volumeInfo }}"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isPartialVolume && !isPlanSection) {
                                                            Color(0xFFB45309)
                                                        } else if (row.completionPercent >= 100 && !isPlanSection) {
                                                            emeraldPrimary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )

                                                    if (row.checklistBreakdown.isNotEmpty()) {
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                                alpha = if (tokens.isDark) 0.25f else 0.42f
                                                            ),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                                            ) {
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

                                                    val metaLine = buildString {
                                                        append("${row.assigneeFio} (${row.groupName}) • ${row.deadlineLabel}")
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
                                                    HorizontalDivider(
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                                                        modifier = Modifier.padding(horizontal = 12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (result.includePhotos && result.attachedPhotos.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "В выгруженный файл добавлено приложение с фотофиксацией (${result.attachedPhotos.size} шт.).",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Закрепленная нижняя панель действий
                var downloadedBannerInDialog by remember(result.downloadedToPath) {
                    mutableStateOf(
                        if (result.downloadedToPath.isNotBlank()) {
                            "Файл сохранён в Загрузки: ${result.file.name}"
                        } else {
                            null
                        }
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        downloadedBannerInDialog?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = emeraldPrimary.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = emeraldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = emeraldPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    ReportDocumentGenerator.openGeneratedReport(context, result)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("open_report_file_button")
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Открыть", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    ReportDocumentGenerator.shareGeneratedReport(context, result, "SHARE")
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_report_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Поделиться", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = {
                                val saved = ReportDocumentGenerator.saveReportToPhoneDownloads(
                                    context = context,
                                    result = result,
                                    showToast = true
                                )
                                if (saved.isSuccess) {
                                    downloadedBannerInDialog = "Файл сохранён в Загрузки: ${result.file.name}"
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emeraldPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("download_to_phone_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Скачать ${result.exportFormat.code} в Загрузки",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Новый шаблон отчёта",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название шаблона *") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = orgHeader,
                onValueChange = { orgHeader = it },
                label = { Text("Заголовок документа (Организация и участок)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Компоновка листов",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Группировка",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Отмена", fontWeight = FontWeight.SemiBold)
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Сохранить", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
