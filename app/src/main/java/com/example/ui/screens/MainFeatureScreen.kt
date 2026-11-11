package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.AuditLogDocument
import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.ReportTemplateDocument
import com.example.data.model.ReportType
import com.example.data.model.SheetLayout
import com.example.data.model.TaskDocument
import com.example.data.model.TaskStatus
import com.example.data.repository.EzhednevnikRepository
import com.example.data.seed.CorporateSeedData
import com.example.domain.reporting.ReportDocumentGenerator
import com.example.ui.auth.CorporateLoginSession
import com.example.ui.auth.PendingRoleApprovalScreen
import com.example.ui.auth.signOut
import com.example.ui.components.CreateTaskDialog
import com.example.ui.components.DirectorExecutiveDashboard
import com.example.ui.components.ItrFilterPillsRow
import com.example.ui.components.ItrHorizontalDateStrip
import com.example.ui.components.ItrPersonaTaskCard
import com.example.ui.components.ItrQuickFilter
import com.example.ui.components.PersonaModeSwitcherBar
import com.example.ui.components.PersonaUiMode
import com.example.ui.components.TaskDetailDialog
import com.example.ui.components.TaskItemCard
import com.example.ui.components.ThemeSettingsDialog
import com.example.ui.components.WorkerPersonaTaskCard
import com.example.ui.components.WorkerTodayHeaderCard
import com.example.ui.theme.LocalAppThemeTokens
import com.example.ui.viewmodel.EzhednevnikViewModel
import com.example.ui.viewmodel.TaskFilterTab
import com.example.ui.viewmodel.UiState
import com.google.firebase.firestore.FirebaseFirestore

enum class MainNavDestination(
    val route: String,
    val titleRu: String,
    val icon: ImageVector
) {
    TODAY("today", "Задачи", Icons.Default.CalendarToday),
    APPROVALS("approvals", "Приёмка", Icons.AutoMirrored.Filled.FactCheck),
    REPORTING("reporting", "Отчёты", Icons.Default.Description),
    STATS("stats", "Сводка", Icons.Default.Analytics),
    ADMIN("admin", "Персонал", Icons.Default.AdminPanelSettings)
}

@Composable
fun MainFeatureScreen(
    currentUserId: String
) {
    MainFeatureScreen(
        session = CorporateLoginSession(
            userId = currentUserId,
            email = "",
            fullName = "Администратор",
            isHeadActivated = true
        ),
        onSignOutComplete = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureScreen(
    session: CorporateLoginSession,
    onSignOutComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: EzhednevnikViewModel = viewModel(
        key = "${session.userId}_${session.email}_${session.isHeadActivated}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                EzhednevnikViewModel(
                    repository = EzhednevnikRepository(db),
                    currentUserId = session.userId,
                    currentUserEmail = session.email,
                    currentUserName = session.fullName,
                    initialHeadActivated = session.isHeadActivated,
                    initialSubdivision = session.requestedSubdivision,
                    initialSiteName = session.requestedSiteName,
                    initialPosition = session.requestedPosition,
                    appContext = app.applicationContext
                )
            }
        }
    )

    val activeEmployee by viewModel.activeEmployee.collectAsStateWithLifecycle()
    val availableEmployees by viewModel.availableEmployees.collectAsStateWithLifecycle()
    val currentMemberRecord by viewModel.currentMemberRecord.collectAsStateWithLifecycle()
    val allSiteMembers by viewModel.siteMembersState.collectAsStateWithLifecycle()
    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val templatesState by viewModel.templatesState.collectAsStateWithLifecycle()
    val generatedReportsState by viewModel.generatedReportsState.collectAsStateWithLifecycle()
    val auditLogsState by viewModel.auditLogsState.collectAsStateWithLifecycle()
    val stats by viewModel.statisticsState.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentEpochDay by viewModel.currentEpochDay.collectAsStateWithLifecycle()
    val selectedCalendarEpochDay by viewModel.selectedCalendarEpochDay.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val activeReportPreview by viewModel.activeReportPreview.collectAsStateWithLifecycle()
    val autoPlanEvening by viewModel.autoSchedulePlanEvening.collectAsStateWithLifecycle()
    val autoReportNight by viewModel.autoScheduleReportNight.collectAsStateWithLifecycle()
    val organizationName by viewModel.organizationName.collectAsStateWithLifecycle()
    val siteName by viewModel.siteName.collectAsStateWithLifecycle()
    val subdivisionsList by viewModel.subdivisionsList.collectAsStateWithLifecycle()
    val sitesList by viewModel.sitesList.collectAsStateWithLifecycle()

    // Экран ожидания назначения роли для новых сотрудников
    val member = currentMemberRecord
    if (member != null && (member.isPendingRole || member.isRejected)) {
        PendingRoleApprovalScreen(
            member = member,
            bannerMessage = bannerMessage,
            availableSubdivisions = subdivisionsList,
            availableSites = sitesList,
            onUpdateRequestDetails = viewModel::updateMyPendingRequestDetails,
            onRefreshStatus = viewModel::refreshMyRoleAssignmentStatus,
            onSignOut = {
                signOut(context, credentialManager, scope, onSignOutComplete)
            }
        )
        return
    }

    var currentTab by remember { mutableStateOf(MainNavDestination.TODAY) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var createDialogTargetEpochDay by remember(currentEpochDay) { mutableLongStateOf(currentEpochDay) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showTopOverflowMenu by remember { mutableStateOf(false) }
    var showOrgSettingsDialog by remember { mutableStateOf(false) }
    var showThemeSettingsDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = currentTab != MainNavDestination.TODAY) {
        currentTab = MainNavDestination.TODAY
    }

    val allTasksRaw = (tasksState as? UiState.Success)?.data ?: emptyList()
    val pendingMembersCount = remember(allSiteMembers) {
        allSiteMembers.count { it.isPendingRole && !it.isRejected }
    }

    // Role-based task visibility filter
    val roleVisibleTasks = remember(allTasksRaw, activeEmployee) {
        when (activeEmployee.role) {
            CorporateRole.ADMIN, CorporateRole.HEAD -> allTasksRaw
            CorporateRole.ITR -> allTasksRaw.filter {
                it.groupId == activeEmployee.groupId ||
                    it.assigneeId == activeEmployee.id ||
                    it.creatorId == activeEmployee.id
            }
            CorporateRole.WORKER -> allTasksRaw.filter {
                it.assigneeId == activeEmployee.id || it.creatorId == activeEmployee.id
            }
        }
    }

    val reviewQueueCount = remember(roleVisibleTasks) {
        roleVisibleTasks.count { it.taskStatus == TaskStatus.ON_REVIEW }
    }

    val selectedTask = remember(roleVisibleTasks, selectedTaskId) {
        roleVisibleTasks.firstOrNull { it.id == selectedTaskId }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.clickable { showOrgSettingsDialog = true }
                        ) {
                            Text(
                                text = "Ежедневник • $siteName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${activeEmployee.shortName} (${activeEmployee.role.titleRu}) • $organizationName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        if (currentTab == MainNavDestination.TODAY) {
                            IconButton(
                                onClick = {
                                    showSearchBar = !showSearchBar
                                    if (!showSearchBar) viewModel.updateSearchQuery("")
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Поиск")
                            }
                        }

                        IconButton(
                            onClick = { showThemeSettingsDialog = true },
                            modifier = Modifier.testTag("top_theme_switcher_button")
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = "Оформление (Темы)")
                        }

                        Box {
                            IconButton(
                                onClick = { showTopOverflowMenu = true },
                                modifier = Modifier.testTag("top_role_switcher_chip")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                            }
                            DropdownMenu(
                                expanded = showTopOverflowMenu,
                                onDismissRequest = { showTopOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Оформление (Темы)") },
                                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        showThemeSettingsDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_theme_settings")
                                )
                                DropdownMenuItem(
                                    text = { Text("Настройки организации и участка") },
                                    leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null) },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        showOrgSettingsDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_org_settings")
                                )
                                DropdownMenuItem(
                                    text = { Text("Перенести невыполненные на завтра") },
                                    leadingIcon = { Icon(Icons.Default.Update, contentDescription = null) },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        viewModel.triggerEndOfDayCarryover()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Сформировать план / отчёт") },
                                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        currentTab = MainNavDestination.REPORTING
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Персонал и роли") },
                                    leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        currentTab = MainNavDestination.ADMIN
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Выйти из учётной записи") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        signOut(context, credentialManager, scope, onSignOutComplete)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (currentTab == MainNavDestination.TODAY) {
                    FloatingActionButton(
                        onClick = {
                            createDialogTargetEpochDay = if (selectedFilter == TaskFilterTab.CALENDAR_DATE) {
                                selectedCalendarEpochDay
                            } else if (selectedFilter == TaskFilterTab.TOMORROW) {
                                currentEpochDay + 1L
                            } else {
                                currentEpochDay
                            }
                            showCreateTaskDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .padding(end = 6.dp, bottom = 4.dp)
                            .testTag("fab_create_task")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Новая задача")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Новая задача", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        MainNavDestination.entries.forEach { dest ->
                            NavigationBarItem(
                                selected = currentTab == dest,
                                onClick = { currentTab = dest },
                                icon = {
                                    when {
                                        dest == MainNavDestination.APPROVALS && reviewQueueCount > 0 -> {
                                            BadgedBox(
                                                badge = { Badge { Text(reviewQueueCount.toString()) } }
                                            ) {
                                                Icon(dest.icon, contentDescription = dest.titleRu)
                                            }
                                        }
                                        dest == MainNavDestination.ADMIN && pendingMembersCount > 0 -> {
                                            BadgedBox(
                                                badge = { Badge { Text(pendingMembersCount.toString()) } }
                                            ) {
                                                Icon(dest.icon, contentDescription = dest.titleRu)
                                            }
                                        }
                                        else -> {
                                            Icon(dest.icon, contentDescription = dest.titleRu)
                                        }
                                    }
                                },
                                label = { Text(dest.titleRu) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        MainNavDestination.entries.forEach { dest ->
                            NavigationRailItem(
                                selected = currentTab == dest,
                                onClick = { currentTab = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.titleRu) },
                                label = { Text(dest.titleRu) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    AnimatedVisibility(visible = bannerMessage != null) {
                        bannerMessage?.let { msg ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.clearBanner() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Скрыть",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    when (currentTab) {
                        MainNavDestination.TODAY -> {
                            val logs = (auditLogsState as? UiState.Success)?.data ?: emptyList()
                            TodayTasksContent(
                                activeEmployee = activeEmployee,
                                tasksState = tasksState,
                                visibleTasks = roleVisibleTasks,
                                activeStaffCount = availableEmployees.size.coerceAtLeast(1),
                                auditLogs = logs,
                                pendingMembersCount = pendingMembersCount,
                                selectedFilter = selectedFilter,
                                showSearchBar = showSearchBar,
                                searchQuery = searchQuery,
                                currentEpochDay = currentEpochDay,
                                selectedCalendarEpochDay = selectedCalendarEpochDay,
                                onSelectFilter = viewModel::selectFilter,
                                onSelectCalendarDate = viewModel::selectCalendarDate,
                                onSearchChange = viewModel::updateSearchQuery,
                                onOpenRoleAssignments = { currentTab = MainNavDestination.ADMIN },
                                onOpenCreateDialogForDate = { targetDay ->
                                    createDialogTargetEpochDay = targetDay
                                    showCreateTaskDialog = true
                                },
                                onOpenReportingTab = { currentTab = MainNavDestination.REPORTING },
                                onTaskClick = { selectedTaskId = it.id },
                                onQuickCompleteTask = viewModel::quickCompleteTaskFromCard,
                                onApproveTask = { viewModel.approveReport(it, "Принято руководителем") },
                                onRejectTask = { viewModel.rejectReport(it, "Возвращено на доработку руководителем") },
                                onDeleteTask = viewModel::deleteTask,
                                onToggleSubtask = viewModel::toggleSubtask
                            )
                        }
                        MainNavDestination.APPROVALS -> {
                            ApprovalsQueueScreen(
                                activeEmployee = activeEmployee,
                                allTasks = roleVisibleTasks,
                                onTaskClick = { selectedTaskId = it.id }
                            )
                        }
                        MainNavDestination.REPORTING -> {
                            val tpls = (templatesState as? UiState.Success)?.data
                                ?: CorporateSeedData.buildInitialReportTemplates(
                                    userId = session.userId,
                                    dateLabel = ReportDocumentGenerator.formatEpochDay(currentEpochDay),
                                    orgName = organizationName,
                                    siteName = siteName
                                )
                            val genReps = (generatedReportsState as? UiState.Success)?.data ?: emptyList()
                            ReportingCenterScreen(
                                activeEmployee = activeEmployee,
                                templates = tpls,
                                generatedReports = genReps,
                                currentEpochDay = currentEpochDay,
                                organizationName = organizationName,
                                siteName = siteName,
                                autoPlanEvening = autoPlanEvening,
                                autoReportNight = autoReportNight,
                                onToggleAutoPlan = viewModel::toggleAutoSchedulePlan,
                                onToggleAutoReport = viewModel::toggleAutoScheduleReport,
                                onOpenOrganizationSettings = { showOrgSettingsDialog = true },
                                onGenerateReport = { tpl, layout, fmt, save, includePhotos ->
                                    viewModel.previewOrGenerateReport(
                                        context = context,
                                        template = tpl,
                                        sheetLayout = layout,
                                        exportFormat = fmt,
                                        saveToHistory = save,
                                        includePhotos = includePhotos
                                    )
                                },
                                onSaveCustomTemplate = viewModel::saveCustomTemplate
                            )
                        }
                        MainNavDestination.STATS -> {
                            StatisticsDashboardScreen(
                                activeEmployee = activeEmployee,
                                stats = stats,
                                onExportStats = { fmt ->
                                    val statTpl = ReportTemplateDocument(
                                        id = "tpl_stats_export",
                                        name = "Статистическая сводка",
                                        reportType = ReportType.GROUP_SITE_SUMMARY.code,
                                        sheetLayout = SheetLayout.SINGLE_SHEET.code,
                                        exportFormat = fmt.code
                                    )
                                    viewModel.previewOrGenerateReport(
                                        context = context,
                                        template = statTpl,
                                        sheetLayout = SheetLayout.SINGLE_SHEET,
                                        exportFormat = fmt,
                                        saveToHistory = true,
                                        includePhotos = true
                                    )
                                }
                            )
                        }
                        MainNavDestination.ADMIN -> {
                            val logs = (auditLogsState as? UiState.Success)?.data ?: emptyList()
                            AdminAndAuditScreen(
                                activeEmployee = activeEmployee,
                                siteMembers = allSiteMembers,
                                auditLogs = logs,
                                organizationName = organizationName,
                                siteName = siteName,
                                availableSubdivisions = subdivisionsList,
                                availableSites = sitesList,
                                onOpenOrganizationSettings = { showOrgSettingsDialog = true },
                                onAssignRoleToMember = viewModel::assignRoleToMember,
                                onEditEmployeeDetails = viewModel::updateEmployeeDetails,
                                onRejectMemberAccess = viewModel::rejectMemberAccess,
                                onDeleteSiteMember = viewModel::deleteSiteMember,
                                onPreRegisterEmployee = viewModel::preRegisterEmployeeByEmail,
                                onSwitchPersona = viewModel::switchPersona,
                                onSignOut = {
                                    signOut(context, credentialManager, scope, onSignOutComplete)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showThemeSettingsDialog) {
        ThemeSettingsDialog(
            onDismiss = { showThemeSettingsDialog = false }
        )
    }

    if (showOrgSettingsDialog) {
        OrganizationSettingsDialog(
            currentOrganizationName = organizationName,
            currentSiteName = siteName,
            currentSubdivisions = subdivisionsList,
            currentSites = sitesList,
            onDismiss = { showOrgSettingsDialog = false },
            onSave = { newOrg, newSite, newSubdivisions, newSites ->
                viewModel.updateAdminSettings(newOrg, newSite, newSubdivisions, newSites)
                showOrgSettingsDialog = false
            }
        )
    }

    if (showCreateTaskDialog) {
        CreateTaskDialog(
            activeEmployee = activeEmployee,
            availableEmployees = availableEmployees,
            currentEpochDay = currentEpochDay,
            initialDueDateEpochDay = createDialogTargetEpochDay,
            onDismiss = { showCreateTaskDialog = false },
            onCreate = viewModel::createNewTask
        )
    }

    selectedTask?.let { task ->
        TaskDetailDialog(
            task = task,
            activeEmployee = activeEmployee,
            availableEmployees = availableEmployees,
            onDismiss = { selectedTaskId = null },
            onAddPhoto = { uri, gps, cmt -> viewModel.addPhotoToTask(task, uri, gps, cmt) },
            onDeletePhoto = { photoId -> viewModel.deletePhotoFromTask(task, photoId) },
            onToggleSubtask = { subId -> viewModel.toggleSubtask(task, subId) },
            onUpdateSubtaskVolume = { subId, vol -> viewModel.updateSubtaskVolume(task, subId, vol) },
            onUpdateSubtaskDetails = { subId, tVol, cVol, unit ->
                viewModel.updateSubtaskDetails(task, subId, tVol, cVol, unit)
            },
            onUpdateTaskVolume = { vol -> viewModel.updateTaskVolumeProgress(task, vol) },
            onUpdateAssignee = { empId, customName ->
                viewModel.updateTaskAssignee(task, empId, customName)
            },
            onAddSubtask = { title, vol, unit -> viewModel.addSubtaskToTask(task, title, vol, unit) },
            onDeleteSubtask = { subId -> viewModel.deleteSubtaskFromTask(task, subId) },
            onAddComment = { txt, mention -> viewModel.addComment(task, txt, mention) },
            onSubmitCompletion = { note -> viewModel.submitCompletion(task, note) },
            onSubmitWorkerVolumeReport = { manualVol, note ->
                viewModel.submitWorkerReportWithVolume(task, manualVol, note)
            },
            onApproveReport = { note -> viewModel.approveReport(task, note) },
            onRejectReport = { reason -> viewModel.rejectReport(task, reason) },
            onArchiveTask = { viewModel.archiveTask(task) },
            onDeleteTask = { viewModel.deleteTask(task) }
        )
    }

    activeReportPreview?.let { previewResult ->
        ReportPreviewDialog(
            result = previewResult,
            onDismiss = viewModel::closeReportPreview
        )
    }
}

@Composable
private fun TodayTasksContent(
    activeEmployee: CorporateEmployee,
    tasksState: UiState<List<TaskDocument>>,
    visibleTasks: List<TaskDocument>,
    activeStaffCount: Int,
    auditLogs: List<AuditLogDocument>,
    pendingMembersCount: Int,
    selectedFilter: TaskFilterTab,
    showSearchBar: Boolean,
    searchQuery: String,
    currentEpochDay: Long,
    selectedCalendarEpochDay: Long,
    onSelectFilter: (TaskFilterTab) -> Unit,
    onSelectCalendarDate: (Long) -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenRoleAssignments: () -> Unit,
    onOpenCreateDialogForDate: (Long) -> Unit,
    onOpenReportingTab: () -> Unit,
    onTaskClick: (TaskDocument) -> Unit,
    onQuickCompleteTask: (TaskDocument) -> Unit,
    onApproveTask: (TaskDocument) -> Unit,
    onRejectTask: (TaskDocument) -> Unit,
    onDeleteTask: (TaskDocument) -> Unit,
    onToggleSubtask: (TaskDocument, String) -> Unit
) {
    val tokens = LocalAppThemeTokens.current
    var selectedPersonaMode by remember(activeEmployee.role) {
        mutableStateOf(PersonaUiMode.defaultForRole(activeEmployee.role))
    }
    var selectedItrFilter by remember { mutableStateOf(ItrQuickFilter.ALL) }
    var showFullMonthCalendarInItr by remember { mutableStateOf(false) }

    val filteredTasks = remember(
        visibleTasks,
        selectedPersonaMode,
        selectedItrFilter,
        selectedFilter,
        searchQuery,
        currentEpochDay,
        selectedCalendarEpochDay
    ) {
        visibleTasks.filter { task ->
            val matchesQuery = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.assigneeName.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true)

            val matchesModeAndFilter = when (selectedPersonaMode) {
                PersonaUiMode.WORKER -> {
                    task.taskStatus != TaskStatus.ARCHIVED && task.dueDateEpochDay <= currentEpochDay + 1L
                }
                PersonaUiMode.ITR -> {
                    val matchesDateOrAll = if (selectedFilter == TaskFilterTab.CALENDAR_DATE) {
                        task.dueDateEpochDay == selectedCalendarEpochDay
                    } else {
                        true
                    }
                    val matchesItrPill = when (selectedItrFilter) {
                        ItrQuickFilter.IN_WORK -> {
                            task.taskStatus == TaskStatus.NEW || task.taskStatus == TaskStatus.IN_PROGRESS
                        }
                        ItrQuickFilter.ON_REVIEW -> {
                            task.taskStatus == TaskStatus.ON_REVIEW
                        }
                        ItrQuickFilter.PROBLEMS -> {
                            task.taskStatus == TaskStatus.OVERDUE ||
                                task.taskStatus == TaskStatus.REJECTED ||
                                task.carryoverCount > 0L
                        }
                        ItrQuickFilter.ALL -> task.taskStatus != TaskStatus.ARCHIVED
                    }
                    matchesDateOrAll && matchesItrPill
                }
                PersonaUiMode.DIRECTOR -> true
            }
            matchesQuery && matchesModeAndFilter
        }
    }

    val itrInWorkCount = remember(visibleTasks) {
        visibleTasks.count { it.taskStatus == TaskStatus.NEW || it.taskStatus == TaskStatus.IN_PROGRESS }
    }
    val itrOnReviewCount = remember(visibleTasks) {
        visibleTasks.count { it.taskStatus == TaskStatus.ON_REVIEW }
    }
    val itrProblemsCount = remember(visibleTasks) {
        visibleTasks.count {
            it.taskStatus == TaskStatus.OVERDUE ||
                it.taskStatus == TaskStatus.REJECTED ||
                it.carryoverCount > 0L
        }
    }
    val itrAllActiveCount = remember(visibleTasks) {
        visibleTasks.count { it.taskStatus != TaskStatus.ARCHIVED }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("today_tasks_list"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Компактное уведомление для Администратора / Руководителя при наличии новых заявок
        if (activeEmployee.role.isAdminOrHead && pendingMembersCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenRoleAssignments() }
                        .testTag("head_pending_roles_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Новые пользователи ожидают роли: $pendingMembersCount чел.",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E)
                            )
                        }
                        Text(
                            text = "Открыть →",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }
        }

        // Переключатель 3 ролевых интерфейсов: Рабочий | ИТР (Прораб) | Руководитель
        item {
            PersonaModeSwitcherBar(
                selectedMode = selectedPersonaMode,
                onSelectMode = { mode ->
                    selectedPersonaMode = mode
                    if (mode == PersonaUiMode.ITR && selectedFilter == TaskFilterTab.CALENDAR_DATE) {
                        onSelectFilter(TaskFilterTab.ALL)
                    }
                }
            )
        }

        // Поиск (при необходимости)
        if (showSearchBar || searchQuery.isNotBlank()) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Поиск по задаче, участку или исполнителю...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_search_input")
                )
            }
        }

        when (selectedPersonaMode) {
            // =========================================================================
            // 1. ИНТЕРФЕЙС РАБОЧЕГО (Монтажник)
            // =========================================================================
            PersonaUiMode.WORKER -> {
                val doneCount = filteredTasks.count {
                    it.taskStatus == TaskStatus.APPROVED || it.taskStatus == TaskStatus.ON_REVIEW
                }
                item {
                    WorkerTodayHeaderCard(
                        currentEpochDay = currentEpochDay,
                        totalTasksToday = filteredTasks.size,
                        completedTasksToday = doneCount,
                        onCreateTaskClick = { onOpenCreateDialogForDate(currentEpochDay) }
                    )
                }

                if (filteredTasks.isEmpty()) {
                    item {
                        EmptyTasksCleanCard(
                            targetEpochDay = currentEpochDay,
                            onCreateClick = { onOpenCreateDialogForDate(currentEpochDay) }
                        )
                    }
                } else {
                    items(filteredTasks, key = { "worker_${it.id}" }) { task ->
                        WorkerPersonaTaskCard(
                            task = task,
                            onOpenDetail = { onTaskClick(task) },
                            onQuickComplete = { onQuickCompleteTask(task) },
                            onToggleSubtask = { subId -> onToggleSubtask(task, subId) }
                        )
                    }
                }
            }

            // =========================================================================
            // 2. ИНТЕРФЕЙС ИТР (Прораб, Мастер)
            // =========================================================================
            PersonaUiMode.ITR -> {
                item {
                    ItrHorizontalDateStrip(
                        currentEpochDay = currentEpochDay,
                        selectedEpochDay = selectedCalendarEpochDay,
                        allVisibleTasks = visibleTasks,
                        onSelectDate = { day ->
                            if (selectedFilter == TaskFilterTab.CALENDAR_DATE && selectedCalendarEpochDay == day) {
                                onSelectFilter(TaskFilterTab.ALL)
                            } else {
                                onSelectCalendarDate(day)
                            }
                        },
                        onCreateTaskForDate = onOpenCreateDialogForDate,
                        onToggleFullCalendar = { showFullMonthCalendarInItr = !showFullMonthCalendarInItr },
                        isFullCalendarExpanded = showFullMonthCalendarInItr
                    )
                }

                if (showFullMonthCalendarInItr) {
                    item {
                        CorporateCalendarSection(
                            currentEpochDay = currentEpochDay,
                            selectedCalendarEpochDay = selectedCalendarEpochDay,
                            isCalendarFilterActive = selectedFilter == TaskFilterTab.CALENDAR_DATE,
                            allVisibleTasks = visibleTasks,
                            onSelectDate = onSelectCalendarDate,
                            onCreateTaskForDate = onOpenCreateDialogForDate,
                            onOpenReportingTab = onOpenReportingTab
                        )
                    }
                }

                item {
                    ItrFilterPillsRow(
                        selectedItrFilter = selectedItrFilter,
                        inWorkCount = itrInWorkCount,
                        onReviewCount = itrOnReviewCount,
                        problemsCount = itrProblemsCount,
                        allCount = itrAllActiveCount,
                        onSelectFilter = { pill -> selectedItrFilter = pill }
                    )
                }

                when (tasksState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = tasksState.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                    is UiState.Success -> {
                        if (filteredTasks.isEmpty()) {
                            item {
                                val targetDayForNewTask = if (selectedFilter == TaskFilterTab.CALENDAR_DATE) {
                                    selectedCalendarEpochDay
                                } else {
                                    currentEpochDay
                                }
                                EmptyTasksCleanCard(
                                    targetEpochDay = targetDayForNewTask,
                                    onCreateClick = { onOpenCreateDialogForDate(targetDayForNewTask) }
                                )
                            }
                        } else {
                            items(filteredTasks, key = { "itr_${it.id}" }) { task ->
                                ItrPersonaTaskCard(
                                    task = task,
                                    onClick = { onTaskClick(task) },
                                    onDelete = { onDeleteTask(task) },
                                    onToggleSubtask = { subId -> onToggleSubtask(task, subId) }
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. ИНТЕРФЕЙС РУКОВОДИТЕЛЯ (Директор) — Executive Dashboard
            // =========================================================================
            PersonaUiMode.DIRECTOR -> {
                item {
                    DirectorExecutiveDashboard(
                        allTasks = visibleTasks,
                        activeStaffCount = activeStaffCount,
                        auditLogs = auditLogs,
                        onApproveTask = onApproveTask,
                        onRejectTask = onRejectTask,
                        onOpenTaskDetail = onTaskClick
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksCleanCard(
    targetEpochDay: Long,
    onCreateClick: () -> Unit
) {
    val tokens = LocalAppThemeTokens.current
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Список задач пуст",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Поставьте задачу или запланируйте объём работ на ${ReportDocumentGenerator.formatEpochDay(targetEpochDay)}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onCreateClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "+ Задача на ${ReportDocumentGenerator.formatEpochDay(targetEpochDay)}",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Интерактивный календарь задач и планов:
 * - Позволяет переключаться между компактной неделей и полным календарным месяцем.
 * - При нажатии на любую дату отображает сводку за выбранный день (задачи, планы, выполнение объёма, фотоотчёты)
 *   и фильтрует список задач за эту дату.
 */
@Composable
private fun CorporateCalendarSection(
    currentEpochDay: Long,
    selectedCalendarEpochDay: Long,
    isCalendarFilterActive: Boolean,
    allVisibleTasks: List<TaskDocument>,
    onSelectDate: (Long) -> Unit,
    onCreateTaskForDate: (Long) -> Unit,
    onOpenReportingTab: () -> Unit
) {
    var expandedMonthView by remember { mutableStateOf(false) }
    val selectedLocalDate = remember(selectedCalendarEpochDay) {
        LocalDate.ofEpochDay(selectedCalendarEpochDay)
    }
    var displayedYearMonth by remember(selectedLocalDate.year, selectedLocalDate.monthValue) {
        mutableStateOf(YearMonth.of(selectedLocalDate.year, selectedLocalDate.monthValue))
    }

    val tasksByEpochDay = remember(allVisibleTasks) {
        allVisibleTasks
            .filter { it.taskStatus != TaskStatus.ARCHIVED }
            .groupBy { it.dueDateEpochDay }
    }

    val tasksOnSelectedDate = remember(tasksByEpochDay, selectedCalendarEpochDay) {
        tasksByEpochDay[selectedCalendarEpochDay].orEmpty()
    }

    val completedOnDate = remember(tasksOnSelectedDate) {
        tasksOnSelectedDate.count { it.taskStatus == TaskStatus.APPROVED || it.completionPercent() >= 100 }
    }
    val onReviewOnDate = remember(tasksOnSelectedDate) {
        tasksOnSelectedDate.count { it.taskStatus == TaskStatus.ON_REVIEW }
    }
    val plannedOrInProgressOnDate = remember(tasksOnSelectedDate) {
        tasksOnSelectedDate.count {
            it.taskStatus != TaskStatus.APPROVED && it.taskStatus != TaskStatus.ON_REVIEW
        }
    }
    val photosOnDate = remember(tasksOnSelectedDate) {
        tasksOnSelectedDate.sumOf { it.photoItems.size }
    }

    val russianLocale = remember { Locale.forLanguageTag("ru-RU") }
    val monthTitle = remember(displayedYearMonth, russianLocale) {
        val mName = displayedYearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, russianLocale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }
        "$mName ${displayedYearMonth.year}"
    }

    val tokens = LocalAppThemeTokens.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = tokens.cardShadowElevationDp.dp,
        border = BorderStroke(tokens.cardBorderWidthDp.dp, tokens.cardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("corporate_calendar_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Шапка календаря с переключением месяца и режима (Неделя / Месяц)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Календарь задач и планов",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (expandedMonthView) {
                                displayedYearMonth = displayedYearMonth.minusMonths(1)
                            } else {
                                onSelectDate(selectedCalendarEpochDay - 7L)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Предыдущий период"
                        )
                    }
                    TextButton(
                        onClick = {
                            val todayDate = LocalDate.ofEpochDay(currentEpochDay)
                            displayedYearMonth = YearMonth.of(todayDate.year, todayDate.monthValue)
                            onSelectDate(currentEpochDay)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Сегодня", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = {
                            if (expandedMonthView) {
                                displayedYearMonth = displayedYearMonth.plusMonths(1)
                            } else {
                                onSelectDate(selectedCalendarEpochDay + 7L)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Следующий период"
                        )
                    }
                    IconButton(
                        onClick = { expandedMonthView = !expandedMonthView },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("toggle_calendar_month_button")
                    ) {
                        Icon(
                            imageVector = if (expandedMonthView) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expandedMonthView) "Свернуть календарь" else "Развернуть месяц"
                        )
                    }
                }
            }

            // Дни недели (Пн..Вс)
            val weekDays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEachIndexed { index, dayLabel ->
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (index >= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!expandedMonthView) {
                // Компактная полоса текущей недели (Пн..Вс вокруг выбранной даты)
                val mondayOfSelectedWeek = remember(selectedLocalDate) {
                    val shift = selectedLocalDate.dayOfWeek.value - 1
                    selectedLocalDate.minusDays(shift.toLong())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (dayOffset in 0..6) {
                        val cellDate = mondayOfSelectedWeek.plusDays(dayOffset.toLong())
                        val cellEpoch = cellDate.toEpochDay()
                        val cellTasks = tasksByEpochDay[cellEpoch].orEmpty()
                        CalendarDayCell(
                            dayNumber = cellDate.dayOfMonth,
                            isToday = cellEpoch == currentEpochDay,
                            isSelected = cellEpoch == selectedCalendarEpochDay,
                            isCurrentMonth = true,
                            tasksCount = cellTasks.size,
                            allCompleted = cellTasks.isNotEmpty() && cellTasks.all {
                                it.taskStatus == TaskStatus.APPROVED || it.completionPercent() >= 100
                            },
                            onClick = { onSelectDate(cellEpoch) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calendar_day_$cellEpoch")
                        )
                    }
                }
            } else {
                // Полная сетка месяца (по неделям)
                val firstOfMonth = displayedYearMonth.atDay(1)
                val daysInMonth = displayedYearMonth.lengthOfMonth()
                val leadingEmptyCells = firstOfMonth.dayOfWeek.value - 1
                val totalCells = ((leadingEmptyCells + daysInMonth + 6) / 7) * 7
                val weeksCount = totalCells / 7

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (week in 0 until weeksCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (dow in 0..6) {
                                val cellIndex = week * 7 + dow
                                val dayOfMonth = cellIndex - leadingEmptyCells + 1
                                if (dayOfMonth in 1..daysInMonth) {
                                    val cellDate = displayedYearMonth.atDay(dayOfMonth)
                                    val cellEpoch = cellDate.toEpochDay()
                                    val cellTasks = tasksByEpochDay[cellEpoch].orEmpty()
                                    CalendarDayCell(
                                        dayNumber = dayOfMonth,
                                        isToday = cellEpoch == currentEpochDay,
                                        isSelected = cellEpoch == selectedCalendarEpochDay,
                                        isCurrentMonth = true,
                                        tasksCount = cellTasks.size,
                                        allCompleted = cellTasks.isNotEmpty() && cellTasks.all {
                                            it.taskStatus == TaskStatus.APPROVED || it.completionPercent() >= 100
                                        },
                                        onClick = { onSelectDate(cellEpoch) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("calendar_day_$cellEpoch")
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Панель сводки по выбранной в календаре дате (Задачи, Планы, Объёмы, Фотоотчёты)
            val formattedSelectedDate = ReportDocumentGenerator.formatEpochDay(selectedCalendarEpochDay)
            val dateRelationLabel = when (selectedCalendarEpochDay) {
                currentEpochDay -> "Сегодня"
                currentEpochDay + 1L -> "План на завтра"
                currentEpochDay - 1L -> "Вчера"
                else -> if (selectedCalendarEpochDay > currentEpochDay) "План работ" else "Архив дня"
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$formattedSelectedDate ($dateRelationLabel)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Задач и планов: ${tasksOnSelectedDate.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Выполнено (100%): $completedOnDate • На проверке: $onReviewOnDate • В плане/работе: $plannedOrInProgressOnDate • Фото: $photosOnDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onCreateTaskForDate(selectedCalendarEpochDay) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(34.dp)
                                .testTag("calendar_add_task_for_date_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ Задача / план на ${formattedSelectedDate.take(5)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                if (!isCalendarFilterActive) {
                                    onSelectDate(selectedCalendarEpochDay)
                                } else {
                                    onOpenReportingTab()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Text(
                                text = if (!isCalendarFilterActive) "Показать день" else "Отчёт за день",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    isToday: Boolean,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    tasksCount: Int,
    allCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        isCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = if (isToday && !isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            if (tasksCount > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                        allCompleted -> Color(0xFFDCFCE7)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = "$tasksCount зад.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            allCompleted -> Color(0xFF166534)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
