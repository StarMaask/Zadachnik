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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
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
import com.example.ui.components.TaskDetailDialog
import com.example.ui.components.TaskItemCard
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
            email = "m241185m@gmail.com",
            fullName = "Руководитель",
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
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val activeReportPreview by viewModel.activeReportPreview.collectAsStateWithLifecycle()
    val autoPlanEvening by viewModel.autoSchedulePlanEvening.collectAsStateWithLifecycle()
    val autoReportNight by viewModel.autoScheduleReportNight.collectAsStateWithLifecycle()
    val organizationName by viewModel.organizationName.collectAsStateWithLifecycle()
    val siteName by viewModel.siteName.collectAsStateWithLifecycle()

    // Экран ожидания назначения роли для новых сотрудников
    val member = currentMemberRecord
    if (member != null && (member.isPendingRole || member.isRejected)) {
        PendingRoleApprovalScreen(
            member = member,
            bannerMessage = bannerMessage,
            onUpdateRequestDetails = viewModel::updateMyPendingRequestDetails,
            onRefreshStatus = viewModel::refreshMyRoleAssignmentStatus,
            onActivateHeadWithCode = viewModel::activateHeadAccessWithCode,
            onSignOut = {
                signOut(context, credentialManager, scope, onSignOutComplete)
            }
        )
        return
    }

    var currentTab by remember { mutableStateOf(MainNavDestination.TODAY) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showTopOverflowMenu by remember { mutableStateOf(false) }
    var showOrgSettingsDialog by remember { mutableStateOf(false) }

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
            CorporateRole.HEAD -> allTasksRaw
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
                        onClick = { showCreateTaskDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("fab_create_task")
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
                            TodayTasksContent(
                                activeEmployee = activeEmployee,
                                tasksState = tasksState,
                                visibleTasks = roleVisibleTasks,
                                pendingMembersCount = pendingMembersCount,
                                selectedFilter = selectedFilter,
                                showSearchBar = showSearchBar,
                                searchQuery = searchQuery,
                                currentEpochDay = currentEpochDay,
                                onSelectFilter = viewModel::selectFilter,
                                onSearchChange = viewModel::updateSearchQuery,
                                onOpenRoleAssignments = { currentTab = MainNavDestination.ADMIN },
                                onOpenCreateDialog = { showCreateTaskDialog = true },
                                onTaskClick = { selectedTaskId = it.id },
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

    if (showOrgSettingsDialog) {
        OrganizationSettingsDialog(
            currentOrganizationName = organizationName,
            currentSiteName = siteName,
            onDismiss = { showOrgSettingsDialog = false },
            onSave = { newOrg, newSite ->
                viewModel.updateOrganizationAndSite(newOrg, newSite)
                showOrgSettingsDialog = false
            }
        )
    }

    if (showCreateTaskDialog) {
        CreateTaskDialog(
            activeEmployee = activeEmployee,
            availableEmployees = availableEmployees,
            currentEpochDay = currentEpochDay,
            onDismiss = { showCreateTaskDialog = false },
            onCreate = viewModel::createNewTask
        )
    }

    selectedTask?.let { task ->
        TaskDetailDialog(
            task = task,
            activeEmployee = activeEmployee,
            onDismiss = { selectedTaskId = null },
            onAddPhoto = { uri, gps, cmt -> viewModel.addPhotoToTask(task, uri, gps, cmt) },
            onDeletePhoto = { photoId -> viewModel.deletePhotoFromTask(task, photoId) },
            onToggleSubtask = { subId -> viewModel.toggleSubtask(task, subId) },
            onUpdateSubtaskVolume = { subId, vol -> viewModel.updateSubtaskVolume(task, subId, vol) },
            onUpdateTaskVolume = { vol -> viewModel.updateTaskVolumeProgress(task, vol) },
            onAddSubtask = { title, vol, unit -> viewModel.addSubtaskToTask(task, title, vol, unit) },
            onAddComment = { txt, mention -> viewModel.addComment(task, txt, mention) },
            onSubmitCompletion = { note -> viewModel.submitCompletion(task, note) },
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
    pendingMembersCount: Int,
    selectedFilter: TaskFilterTab,
    showSearchBar: Boolean,
    searchQuery: String,
    currentEpochDay: Long,
    onSelectFilter: (TaskFilterTab) -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenRoleAssignments: () -> Unit,
    onOpenCreateDialog: () -> Unit,
    onTaskClick: (TaskDocument) -> Unit,
    onDeleteTask: (TaskDocument) -> Unit,
    onToggleSubtask: (TaskDocument, String) -> Unit
) {
    val filteredTasks = remember(visibleTasks, selectedFilter, searchQuery, currentEpochDay) {
        visibleTasks.filter { task ->
            val matchesQuery = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.assigneeName.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedFilter) {
                TaskFilterTab.TODAY -> task.dueDateEpochDay <= currentEpochDay && task.taskStatus != TaskStatus.ARCHIVED
                TaskFilterTab.TOMORROW -> task.dueDateEpochDay > currentEpochDay && task.taskStatus != TaskStatus.ARCHIVED
                TaskFilterTab.ON_REVIEW -> task.taskStatus == TaskStatus.ON_REVIEW
                TaskFilterTab.OVERDUE -> task.taskStatus == TaskStatus.OVERDUE || task.taskStatus == TaskStatus.REJECTED || task.carryoverCount > 0L
                TaskFilterTab.APPROVED -> task.taskStatus == TaskStatus.APPROVED
                TaskFilterTab.RECURRING -> task.recurrenceRule != "NONE"
                TaskFilterTab.ARCHIVE -> task.taskStatus == TaskStatus.ARCHIVED
                TaskFilterTab.ALL -> true
            }
            matchesQuery && matchesTab
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("today_tasks_list"),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Компактное уведомление для Руководителя при наличии новых заявок
        if (activeEmployee.role == CorporateRole.HEAD && pendingMembersCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenRoleAssignments() }
                        .testTag("head_pending_roles_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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

        // Collapsible Search Bar
        if (showSearchBar || searchQuery.isNotBlank()) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Поиск по названию задачи или исполнителю...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_search_input")
                )
            }
        }

        // Compact Filter Tabs Row + Date Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Дата: ${ReportDocumentGenerator.formatEpochDay(currentEpochDay)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Всего: ${filteredTasks.size} из ${visibleTasks.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    TaskFilterTab.TODAY,
                    TaskFilterTab.TOMORROW,
                    TaskFilterTab.ON_REVIEW,
                    TaskFilterTab.APPROVED,
                    TaskFilterTab.ALL
                ).forEach { tab ->
                    FilterChip(
                        selected = selectedFilter == tab,
                        onClick = { onSelectFilter(tab) },
                        label = { Text(tab.titleRu) },
                        modifier = Modifier.testTag("filter_tab_${tab.name}")
                    )
                }
            }
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Нажмите кнопку «Новая задача», чтобы добавить первую задачу.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = onOpenCreateDialog) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Создать задачу")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItemCard(
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
}
