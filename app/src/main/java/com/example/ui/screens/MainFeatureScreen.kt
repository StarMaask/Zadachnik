package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
    TODAY("today", "Сегодня", Icons.Default.CalendarToday),
    APPROVALS("approvals", "Проверка", Icons.Default.FactCheck),
    REPORTING("reporting", "Отчётность", Icons.Default.Description),
    STATS("stats", "Статистика", Icons.Default.Analytics),
    ADMIN("admin", "Участок / Роль", Icons.Default.AdminPanelSettings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureScreen(
    currentUserId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: EzhednevnikViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                EzhednevnikViewModel(EzhednevnikRepository(db), currentUserId)
            }
        }
    )

    val activeEmployee by viewModel.activeEmployee.collectAsStateWithLifecycle()
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

    var currentTab by remember { mutableStateOf(MainNavDestination.TODAY) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = currentTab != MainNavDestination.TODAY) {
        currentTab = MainNavDestination.TODAY
    }

    val allTasksRaw = (tasksState as? UiState.Success)?.data ?: emptyList()

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
                        Column {
                            Text(
                                text = "Ежедневник • ${activeEmployee.shortName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${activeEmployee.role.titleRu} • ${activeEmployee.groupName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        AssistChip(
                            onClick = { currentTab = MainNavDestination.ADMIN },
                            label = { Text(activeEmployee.role.code) },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("top_role_switcher_chip")
                        )
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
                        modifier = Modifier.testTag("fab_create_task")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Поставить задачу")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Поставить задачу", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        MainNavDestination.entries.forEach { dest ->
                            NavigationBarItem(
                                selected = currentTab == dest,
                                onClick = { currentTab = dest },
                                icon = {
                                    if (dest == MainNavDestination.APPROVALS && reviewQueueCount > 0) {
                                        BadgedBox(
                                            badge = { Badge { Text(reviewQueueCount.toString()) } }
                                        ) {
                                            Icon(dest.icon, contentDescription = dest.titleRu)
                                        }
                                    } else {
                                        Icon(dest.icon, contentDescription = dest.titleRu)
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
                                color = Color(0xFF0F2942),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.clearBanner() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Скрыть уведомление",
                                            tint = Color.White
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
                                selectedFilter = selectedFilter,
                                searchQuery = searchQuery,
                                currentEpochDay = currentEpochDay,
                                onSelectFilter = viewModel::selectFilter,
                                onSearchChange = viewModel::updateSearchQuery,
                                onSwitchPersona = viewModel::switchPersona,
                                onTriggerCarryover = viewModel::triggerEndOfDayCarryover,
                                onQuickOpenReporting = { currentTab = MainNavDestination.REPORTING },
                                onTaskClick = { selectedTaskId = it.id }
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
                                ?: CorporateSeedData.buildInitialReportTemplates(currentUserId)
                            val genReps = (generatedReportsState as? UiState.Success)?.data ?: emptyList()
                            ReportingCenterScreen(
                                activeEmployee = activeEmployee,
                                templates = tpls,
                                generatedReports = genReps,
                                autoPlanEvening = autoPlanEvening,
                                autoReportNight = autoReportNight,
                                onToggleAutoPlan = viewModel::toggleAutoSchedulePlan,
                                onToggleAutoReport = viewModel::toggleAutoScheduleReport,
                                onGenerateReport = { tpl, layout, fmt, save ->
                                    viewModel.previewOrGenerateReport(context, tpl, layout, fmt, save)
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
                                        name = "Статистическая сводка участка",
                                        reportType = ReportType.GROUP_SITE_SUMMARY.code,
                                        sheetLayout = SheetLayout.SINGLE_SHEET.code,
                                        exportFormat = fmt.code
                                    )
                                    viewModel.previewOrGenerateReport(
                                        context = context,
                                        template = statTpl,
                                        sheetLayout = SheetLayout.SINGLE_SHEET,
                                        exportFormat = fmt,
                                        saveToHistory = true
                                    )
                                }
                            )
                        }
                        MainNavDestination.ADMIN -> {
                            val logs = (auditLogsState as? UiState.Success)?.data ?: emptyList()
                            AdminAndAuditScreen(
                                activeEmployee = activeEmployee,
                                auditLogs = logs,
                                onSwitchPersona = viewModel::switchPersona,
                                onSignOut = {
                                    signOut(credentialManager, scope)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateTaskDialog) {
        CreateTaskDialog(
            activeEmployee = activeEmployee,
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
            onToggleSubtask = { subId -> viewModel.toggleSubtask(task, subId) },
            onAddComment = { txt, mention -> viewModel.addComment(task, txt, mention) },
            onSubmitCompletion = { note -> viewModel.submitCompletion(task, note) },
            onApproveReport = { note -> viewModel.approveReport(task, note) },
            onRejectReport = { reason -> viewModel.rejectReport(task, reason) },
            onArchiveTask = { viewModel.archiveTask(task) }
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
    selectedFilter: TaskFilterTab,
    searchQuery: String,
    currentEpochDay: Long,
    onSelectFilter: (TaskFilterTab) -> Unit,
    onSearchChange: (String) -> Unit,
    onSwitchPersona: (String) -> Unit,
    onTriggerCarryover: () -> Unit,
    onQuickOpenReporting: () -> Unit,
    onTaskClick: (TaskDocument) -> Unit
) {
    val filteredTasks = remember(visibleTasks, selectedFilter, searchQuery, currentEpochDay) {
        visibleTasks.filter { task ->
            val matchesQuery = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.assigneeName.contains(searchQuery, ignoreCase = true) ||
                task.groupName.contains(searchQuery, ignoreCase = true) ||
                task.tags.any { it.contains(searchQuery, ignoreCase = true) }

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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Banner & Quick Operations Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_site_banner_1794330702001),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x990A1D30),
                                        Color(0xF20A1D30)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFF59E0B),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Смена: ${ReportDocumentGenerator.formatEpochDay(currentEpochDay)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "Задач: ${visibleTasks.size} • На проверке: ${visibleTasks.count { it.taskStatus == TaskStatus.ON_REVIEW }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeEmployee.siteName,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onTriggerCarryover,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("carryover_shift_button")
                            ) {
                                Icon(Icons.Default.Update, contentDescription = "Автоперенос", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Автоперенос на завтра", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onQuickOpenReporting,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_open_reporting_button")
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = "План и отчёт",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "План / Отчёт дня",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick 1-Tap Role Switcher Bar (Руководитель / ИТР / Рабочий)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Роль:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                listOf(
                    "emp_head_1" to "Руководитель (Ковалёв)",
                    "emp_itr_1" to "ИТР (Соколов)",
                    "emp_worker_1" to "Рабочий (Морозов)",
                    "emp_worker_3" to "Рабочий (Кузнецов)"
                ).forEach { (empId, label) ->
                    FilterChip(
                        selected = activeEmployee.id == empId,
                        onClick = { onSwitchPersona(empId) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("quick_role_$empId")
                    )
                }
            }
        }

        // Search and Filter Chips
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Поиск по задаче, ФИО исполнителя, бригаде или #тегу...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Очистить поиск")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_search_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskFilterTab.entries.forEach { tab ->
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
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "В разделе «${selectedFilter.titleRu}» задач не найдено",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Переключите вкладку фильтра или нажмите «+ Поставить задачу»",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            onClick = { onTaskClick(task) }
                        )
                    }
                }
            }
        }
    }
}
