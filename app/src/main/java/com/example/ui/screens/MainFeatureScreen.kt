package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
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
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
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
import com.example.data.model.SiteMemberDocument
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskDocument
import com.example.data.model.TaskStatus
import com.example.data.repository.EzhednevnikRepository
import com.example.data.seed.CorporateSeedData
import com.example.domain.reporting.ReportDocumentGenerator
import com.example.ui.auth.CorporateLoginSession
import com.example.ui.auth.PendingRoleApprovalScreen
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.auth.signOut
import com.example.ui.components.CreateTaskDialog
import com.example.ui.components.DirectorExecutiveDashboard
import com.example.ui.components.ItrFilterPillsRow
import com.example.ui.components.ItrHorizontalDateStrip
import com.example.ui.components.ItrPersonaTaskCard
import com.example.ui.components.ItrQuickFilter
import com.example.ui.components.ItrScopeFilter
import com.example.ui.components.ItrSubdivisionScopeBar
import com.example.ui.components.PersonaModeSwitcherBar
import com.example.ui.components.PersonaUiMode
import com.example.ui.components.TaskDetailDialog
import com.example.ui.components.TaskItemCard
import com.example.ui.components.ThemeSettingsDialog
import com.example.ui.components.WorkerPersonaTaskCard
import com.example.ui.components.WorkerTodayHeaderCard
import com.example.ui.components.statusColor
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
    val isCloudSyncActive by viewModel.isCloudSyncActive.collectAsStateWithLifecycle()

    val triggerCloudGoogleConnect: (SiteMemberDocument?) -> Unit = { targetMember ->
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            selectedSubdivision = targetMember?.groupName ?: session.requestedSubdivision,
            selectedSiteName = targetMember?.effectiveSiteName ?: session.requestedSiteName,
            selectedPosition = targetMember?.position ?: session.requestedPosition,
            selectedFullName = targetMember?.fullName ?: session.fullName,
            preferredEmail = targetMember?.email ?: session.email,
            onAuthSuccess = {
                viewModel.onCloudAuthConnected()
            },
            onAuthError = {},
            scope = scope
        )
    }

    // Экран ожидания назначения роли для новых сотрудников
    val member = currentMemberRecord
    if (member != null && (member.isPendingRole || member.isRejected)) {
        PendingRoleApprovalScreen(
            member = member,
            bannerMessage = bannerMessage,
            availableSubdivisions = subdivisionsList,
            availableSites = sitesList,
            isCloudSyncActive = isCloudSyncActive,
            onConnectCloudSync = { triggerCloudGoogleConnect(member) },
            onUpdateRequestDetails = viewModel::updateMyPendingRequestDetails,
            onRefreshStatus = viewModel::refreshMyRoleAssignmentStatus,
            onSignOut = {
                signOut(context, credentialManager, scope, onSignOutComplete)
            }
        )
        return
    }

    var currentTab by remember { mutableStateOf(MainNavDestination.TODAY) }
    var approvalsOpenArchiveByDefault by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var createDialogTargetEpochDay by remember(currentEpochDay) { mutableLongStateOf(currentEpochDay) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showTopOverflowMenu by remember { mutableStateOf(false) }
    var showOrgSettingsDialog by remember { mutableStateOf(false) }
    var showThemeSettingsDialog by remember { mutableStateOf(false) }

    val isWorkerRole = activeEmployee.role == CorporateRole.WORKER
    val isAdminOrHead = activeEmployee.role.isAdminOrHead
    val isPersonalProfileOnlyRole = !isAdminOrHead

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(isAdminOrHead) {
        if (isAdminOrHead && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val visibleNavDestinations = remember(isWorkerRole) {
        if (isWorkerRole) {
            listOf(
                MainNavDestination.TODAY,
                MainNavDestination.STATS,
                MainNavDestination.ADMIN
            )
        } else {
            MainNavDestination.entries
        }
    }

    if (currentTab !in visibleNavDestinations) {
        currentTab = MainNavDestination.TODAY
    }

    BackHandler(enabled = currentTab != MainNavDestination.TODAY) {
        currentTab = MainNavDestination.TODAY
    }

    val allTasksRaw = (tasksState as? UiState.Success)?.data ?: emptyList()
    val pendingMembersList = remember(allSiteMembers) {
        allSiteMembers.filter { it.isPendingRole && !it.isRejected }
    }
    val pendingMembersCount = pendingMembersList.size

    // Строгая фильтрация видимости задач по уровню доступа:
    // - Рабочий видит информацию только рабочего
    // - ИТР видит информацию рабочего и ИТР
    // - Руководитель / Администратор видит всю информацию
    val roleVisibleTasks = remember(allTasksRaw, activeEmployee, availableEmployees) {
        val roleByEmpId = availableEmployees.associate { it.id to it.role }
        when (activeEmployee.role) {
            CorporateRole.ADMIN, CorporateRole.HEAD -> allTasksRaw
            CorporateRole.ITR -> allTasksRaw.filter { task ->
                task.isRelevantToItr(activeEmployee, availableEmployees)
            }
            CorporateRole.WORKER -> allTasksRaw.filter { task ->
                task.isAssignedToEmployee(activeEmployee) ||
                    task.creatorId == activeEmployee.id
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
                            modifier = if (isAdminOrHead) {
                                Modifier.clickable { showOrgSettingsDialog = true }
                            } else {
                                Modifier
                            }
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
                                    createDialogTargetEpochDay = if (selectedFilter == TaskFilterTab.CALENDAR_DATE) {
                                        selectedCalendarEpochDay
                                    } else if (selectedFilter == TaskFilterTab.TOMORROW) {
                                        currentEpochDay + 1L
                                    } else {
                                        currentEpochDay
                                    }
                                    showCreateTaskDialog = true
                                },
                                modifier = Modifier.testTag("top_add_task_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Новая задача")
                            }
                            IconButton(
                                onClick = {
                                    showSearchBar = !showSearchBar
                                    if (!showSearchBar) viewModel.updateSearchQuery("")
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Поиск")
                            }
                        }

                        if (isAdminOrHead) {
                            IconButton(
                                onClick = {
                                    viewModel.refreshSiteMembersQueue(showBanner = true)
                                    currentTab = MainNavDestination.ADMIN
                                },
                                modifier = Modifier.testTag("top_pending_members_button")
                            ) {
                                if (pendingMembersCount > 0) {
                                    BadgedBox(
                                        badge = { Badge { Text(pendingMembersCount.toString()) } }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = "Заявки пользователей ($pendingMembersCount)",
                                            tint = Color(0xFFD97706)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Заявки пользователей"
                                    )
                                }
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
                                if (isAdminOrHead) {
                                    DropdownMenuItem(
                                        text = { Text("Настройки организации и участка") },
                                        leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null) },
                                        onClick = {
                                            showTopOverflowMenu = false
                                            showOrgSettingsDialog = true
                                        },
                                        modifier = Modifier.testTag("menu_org_settings")
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Перенести невыполненные на завтра") },
                                    leadingIcon = { Icon(Icons.Default.Update, contentDescription = null) },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        viewModel.triggerEndOfDayCarryover()
                                    }
                                )
                                if (!isWorkerRole) {
                                    DropdownMenuItem(
                                        text = { Text("Принятые отчёты (Архив заданий)") },
                                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                        onClick = {
                                            showTopOverflowMenu = false
                                            approvalsOpenArchiveByDefault = true
                                            currentTab = MainNavDestination.APPROVALS
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
                                }
                                if (isAdminOrHead) {
                                    DropdownMenuItem(
                                        text = { Text("Персонал и роли") },
                                        leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                                        onClick = {
                                            showTopOverflowMenu = false
                                            currentTab = MainNavDestination.ADMIN
                                        }
                                    )
                                }
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
            floatingActionButton = {},
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        visibleNavDestinations.forEach { dest ->
                            val tabTitle = if (dest == MainNavDestination.ADMIN && isPersonalProfileOnlyRole) {
                                "Профиль"
                            } else {
                                dest.titleRu
                            }
                            val tabIcon = if (dest == MainNavDestination.ADMIN && isPersonalProfileOnlyRole) {
                                Icons.Default.Person
                            } else {
                                dest.icon
                            }
                            NavigationBarItem(
                                selected = currentTab == dest,
                                onClick = {
                                    if (dest == MainNavDestination.APPROVALS) {
                                        approvalsOpenArchiveByDefault = false
                                    }
                                    currentTab = dest
                                },
                                icon = {
                                    when {
                                        dest == MainNavDestination.APPROVALS && reviewQueueCount > 0 -> {
                                            BadgedBox(
                                                badge = { Badge { Text(reviewQueueCount.toString()) } }
                                            ) {
                                                Icon(tabIcon, contentDescription = tabTitle)
                                            }
                                        }
                                        dest == MainNavDestination.ADMIN && isAdminOrHead && pendingMembersCount > 0 -> {
                                            BadgedBox(
                                                badge = { Badge { Text(pendingMembersCount.toString()) } }
                                            ) {
                                                Icon(tabIcon, contentDescription = tabTitle)
                                            }
                                        }
                                        else -> {
                                            Icon(tabIcon, contentDescription = tabTitle)
                                        }
                                    }
                                },
                                label = { Text(tabTitle) },
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
                        visibleNavDestinations.forEach { dest ->
                            val tabTitle = if (dest == MainNavDestination.ADMIN && isPersonalProfileOnlyRole) {
                                "Профиль"
                            } else {
                                dest.titleRu
                            }
                            val tabIcon = if (dest == MainNavDestination.ADMIN && isPersonalProfileOnlyRole) {
                                Icons.Default.Person
                            } else {
                                dest.icon
                            }
                            NavigationRailItem(
                                selected = currentTab == dest,
                                onClick = { currentTab = dest },
                                icon = {
                                    when {
                                        dest == MainNavDestination.APPROVALS && reviewQueueCount > 0 -> {
                                            BadgedBox(
                                                badge = { Badge { Text(reviewQueueCount.toString()) } }
                                            ) {
                                                Icon(tabIcon, contentDescription = tabTitle)
                                            }
                                        }
                                        dest == MainNavDestination.ADMIN && isAdminOrHead && pendingMembersCount > 0 -> {
                                            BadgedBox(
                                                badge = { Badge { Text(pendingMembersCount.toString()) } }
                                            ) {
                                                Icon(tabIcon, contentDescription = tabTitle)
                                            }
                                        }
                                        else -> {
                                            Icon(tabIcon, contentDescription = tabTitle)
                                        }
                                    }
                                },
                                label = { Text(tabTitle) },
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

                    // Если администратор вошёл локально без Google-сессии, показываем кнопку подключения облака
                    if (isAdminOrHead && !isCloudSyncActive) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFD97706)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("admin_cloud_sync_prompt")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Облачная синхронизация заявок не активна",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Подключите аккаунт Google, чтобы принимать заявки с других телефонов",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF92400E)
                                    )
                                }
                                TextButton(
                                    onClick = { triggerCloudGoogleConnect(currentMemberRecord) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("admin_connect_cloud_button")
                                ) {
                                    Text(
                                        text = "Подключить",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                    }

                    // Наглядная карточка-извещение о новых пользователях, ожидающих подтверждения (с быстрым назначением роли в 1 клик)
                    if (isAdminOrHead && pendingMembersList.isNotEmpty() && currentTab != MainNavDestination.ADMIN) {
                        val firstPending = pendingMembersList.first()
                        var quickSelectedRole by remember(firstPending.memberId) {
                            mutableStateOf(CorporateRole.WORKER)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, Color(0xFFD97706)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("global_pending_member_alert_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
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
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Ожидает подтверждения (${pendingMembersList.size}): ${firstPending.fullName}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    TextButton(
                                        onClick = { currentTab = MainNavDestination.ADMIN },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("open_all_pending_requests_button")
                                    ) {
                                        Text(
                                            text = "Все заявки →",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }

                                val pendingMeta = buildString {
                                    if (firstPending.email.isNotBlank() && !firstPending.email.endsWith("@site1.ru")) {
                                        append(firstPending.email)
                                        append(" • ")
                                    }
                                    append(firstPending.position.ifBlank { "Специалист" })
                                    if (firstPending.groupName.isNotBlank()) append(" • ${firstPending.groupName}")
                                    if (firstPending.effectiveSiteName.isNotBlank()) append(" • ${firstPending.effectiveSiteName}")
                                }
                                Text(
                                    text = pendingMeta,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CorporateRole.entries.forEach { roleOpt ->
                                        FilterChip(
                                            selected = quickSelectedRole == roleOpt,
                                            onClick = { quickSelectedRole = roleOpt },
                                            label = {
                                                Text(
                                                    text = roleOpt.shortTitleRu,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val grp = CorporateSeedData.workGroups.firstOrNull {
                                                it.id == firstPending.groupId
                                            }?.copy(name = firstPending.groupName.ifBlank { "Основное подразделение" })
                                                ?: CorporateSeedData.workGroups.first().copy(name = firstPending.groupName.ifBlank { "Основное подразделение" })
                                            viewModel.assignRoleToMember(
                                                firstPending,
                                                quickSelectedRole,
                                                grp,
                                                firstPending.position
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981),
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(38.dp)
                                            .testTag("quick_approve_pending_${firstPending.memberId}")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Подтвердить (${quickSelectedRole.shortTitleRu})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.rejectMemberAccess(firstPending) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(0.7f)
                                            .height(38.dp)
                                    ) {
                                        Text("Отклонить", style = MaterialTheme.typography.labelMedium)
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
                                onOpenAcceptedArchive = {
                                    if (isWorkerRole) {
                                        currentTab = MainNavDestination.STATS
                                    } else {
                                        approvalsOpenArchiveByDefault = true
                                        currentTab = MainNavDestination.APPROVALS
                                    }
                                },
                                onOpenCreateDialogForDate = { targetDay ->
                                    createDialogTargetEpochDay = targetDay
                                    showCreateTaskDialog = true
                                },
                                onOpenReportingTab = { currentTab = MainNavDestination.REPORTING },
                                onTaskClick = { selectedTaskId = it.id },
                                onQuickCompleteTask = viewModel::quickCompleteTaskFromCard,
                                onApproveTask = { viewModel.approveReport(it, "Принято руководителем") },
                                onRejectTask = { task, reason -> viewModel.rejectReport(task, reason) },
                                onDeleteTask = viewModel::deleteTask,
                                onToggleSubtask = viewModel::toggleSubtask
                            )
                        }
                        MainNavDestination.APPROVALS -> {
                            ApprovalsQueueScreen(
                                activeEmployee = activeEmployee,
                                allTasks = roleVisibleTasks,
                                availableEmployees = availableEmployees,
                                initialShowArchive = approvalsOpenArchiveByDefault,
                                onTaskClick = { selectedTaskId = it.id },
                                onApproveTask = { viewModel.approveReport(it, "Принято руководителем") },
                                onRejectTask = { task, reason -> viewModel.rejectReport(task, reason) }
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
                                allTasks = roleVisibleTasks,
                                onTaskClick = { selectedTaskId = it.id },
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
                                allTasks = roleVisibleTasks,
                                currentEpochDay = currentEpochDay,
                                onTaskClick = { selectedTaskId = it.id },
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
                                onRefreshSiteMembers = viewModel::refreshSiteMembersQueue,
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
            existingTasks = roleVisibleTasks,
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
            onAddPhotosBatch = { items, gps -> viewModel.addPhotosToTask(task, items, gps) },
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
            onUpdateAssignees = { empIds, customName ->
                viewModel.updateTaskAssignees(task, empIds, customName)
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
    onOpenAcceptedArchive: () -> Unit = {},
    onOpenCreateDialogForDate: (Long) -> Unit,
    onOpenReportingTab: () -> Unit,
    onTaskClick: (TaskDocument) -> Unit,
    onQuickCompleteTask: (TaskDocument) -> Unit,
    onApproveTask: (TaskDocument) -> Unit,
    onRejectTask: (TaskDocument, String) -> Unit,
    onDeleteTask: (TaskDocument) -> Unit,
    onToggleSubtask: (TaskDocument, String) -> Unit
) {
    val allowedModes = remember(activeEmployee.role) {
        PersonaUiMode.allowedModesForRole(activeEmployee.role)
    }
    var selectedPersonaMode by remember(activeEmployee.role) {
        mutableStateOf(PersonaUiMode.defaultForRole(activeEmployee.role))
    }
    val effectivePersonaMode = if (selectedPersonaMode in allowedModes) {
        selectedPersonaMode
    } else {
        allowedModes.first()
    }

    var selectedItrFilter by remember { mutableStateOf(ItrQuickFilter.ALL) }
    var selectedItrScope by remember { mutableStateOf(ItrScopeFilter.SUBDIVISION) }
    var showFullMonthCalendar by remember { mutableStateOf(false) }
    var workerShowCompletedTab by remember { mutableStateOf(false) }

    var homeTaskPendingReject by remember { mutableStateOf<TaskDocument?>(null) }
    var homeRejectCommentInput by remember { mutableStateOf("") }
    var homeRejectedOutcomeInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var homeApprovedOutcomeTask by remember { mutableStateOf<TaskDocument?>(null) }

    val completedHomeTasks = remember(visibleTasks) {
        visibleTasks.filter {
            it.taskStatus == TaskStatus.APPROVED || it.taskStatus == TaskStatus.ARCHIVED
        }.sortedByDescending { it.dueDateEpochDay }
    }
    val acceptedAndArchivedCount = completedHomeTasks.size

    val activeHomeTasks = remember(visibleTasks) {
        visibleTasks.filter {
            it.taskStatus != TaskStatus.APPROVED && it.taskStatus != TaskStatus.ARCHIVED
        }
    }

    val itrScopeTasks = remember(activeHomeTasks, selectedItrScope, activeEmployee) {
        activeHomeTasks.filter { task ->
            when (selectedItrScope) {
                ItrScopeFilter.SUBDIVISION -> {
                    task.belongsToSubdivision(activeEmployee.groupName, activeEmployee.groupId) ||
                        task.creatorId == activeEmployee.id ||
                        task.isAssignedToEmployee(activeEmployee)
                }
                ItrScopeFilter.SITE -> {
                    task.belongsToSite(activeEmployee.siteName, activeEmployee.siteId) ||
                        task.belongsToSubdivision(activeEmployee.groupName, activeEmployee.groupId) ||
                        task.creatorId == activeEmployee.id ||
                        task.isAssignedToEmployee(activeEmployee)
                }
                ItrScopeFilter.PERSONAL -> {
                    task.isAssignedToEmployee(activeEmployee)
                }
            }
        }
    }

    val itrCompletedScopeTasks = remember(completedHomeTasks, selectedItrScope, activeEmployee) {
        completedHomeTasks.filter { task ->
            when (selectedItrScope) {
                ItrScopeFilter.SUBDIVISION -> {
                    task.belongsToSubdivision(activeEmployee.groupName, activeEmployee.groupId) ||
                        task.creatorId == activeEmployee.id ||
                        task.isAssignedToEmployee(activeEmployee)
                }
                ItrScopeFilter.SITE -> {
                    task.belongsToSite(activeEmployee.siteName, activeEmployee.siteId) ||
                        task.belongsToSubdivision(activeEmployee.groupName, activeEmployee.groupId) ||
                        task.creatorId == activeEmployee.id ||
                        task.isAssignedToEmployee(activeEmployee)
                }
                ItrScopeFilter.PERSONAL -> {
                    task.isAssignedToEmployee(activeEmployee)
                }
            }
        }
    }

    val itrSubdivisionCount = remember(activeHomeTasks, activeEmployee) {
        activeHomeTasks.count {
            it.belongsToSubdivision(activeEmployee.groupName, activeEmployee.groupId) ||
                it.creatorId == activeEmployee.id ||
                it.isAssignedToEmployee(activeEmployee)
        }
    }
    val itrSiteCount = remember(activeHomeTasks, activeEmployee) {
        activeHomeTasks.count {
            it.belongsToSite(activeEmployee.siteName, activeEmployee.siteId) ||
                it.belongsToSubdivision(activeEmployee.groupName, activeEmployee.groupId) ||
                it.creatorId == activeEmployee.id ||
                it.isAssignedToEmployee(activeEmployee)
        }
    }
    val itrPersonalCount = remember(activeHomeTasks, activeEmployee) {
        activeHomeTasks.count {
            it.isAssignedToEmployee(activeEmployee)
        }
    }

    val filteredTasks = remember(
        activeHomeTasks,
        completedHomeTasks,
        itrScopeTasks,
        itrCompletedScopeTasks,
        effectivePersonaMode,
        selectedItrFilter,
        selectedItrScope,
        workerShowCompletedTab,
        selectedFilter,
        searchQuery,
        currentEpochDay,
        selectedCalendarEpochDay
    ) {
        val baseList = when (effectivePersonaMode) {
            PersonaUiMode.ITR -> {
                if (selectedItrFilter == ItrQuickFilter.COMPLETED) itrCompletedScopeTasks else itrScopeTasks
            }
            PersonaUiMode.WORKER -> {
                if (workerShowCompletedTab) completedHomeTasks else activeHomeTasks
            }
            PersonaUiMode.DIRECTOR -> activeHomeTasks
        }
        baseList.filter { task ->
            val matchesQuery = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.assigneeName.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true)

            val matchesModeAndFilter = when (effectivePersonaMode) {
                PersonaUiMode.WORKER -> {
                    if (workerShowCompletedTab) {
                        true
                    } else if (selectedFilter == TaskFilterTab.CALENDAR_DATE) {
                        task.dueDateEpochDay == selectedCalendarEpochDay
                    } else {
                        true
                    }
                }
                PersonaUiMode.ITR -> {
                    val matchesDateOrAll = if (selectedItrFilter == ItrQuickFilter.COMPLETED) {
                        true
                    } else if (selectedFilter == TaskFilterTab.CALENDAR_DATE) {
                        task.dueDateEpochDay == selectedCalendarEpochDay
                    } else {
                        true
                    }
                    val matchesItrPill = when (selectedItrFilter) {
                        ItrQuickFilter.IN_WORK -> {
                            task.taskStatus == TaskStatus.NEW ||
                                task.taskStatus == TaskStatus.IN_PROGRESS ||
                                task.taskStatus == TaskStatus.REJECTED
                        }
                        ItrQuickFilter.ON_REVIEW -> {
                            task.taskStatus == TaskStatus.ON_REVIEW
                        }
                        ItrQuickFilter.PROBLEMS -> {
                            task.taskStatus == TaskStatus.OVERDUE ||
                                task.taskStatus == TaskStatus.REJECTED ||
                                task.carryoverCount > 0L
                        }
                        ItrQuickFilter.COMPLETED -> {
                            task.taskStatus == TaskStatus.APPROVED || task.taskStatus == TaskStatus.ARCHIVED
                        }
                        ItrQuickFilter.ALL -> true
                    }
                    matchesDateOrAll && matchesItrPill
                }
                PersonaUiMode.DIRECTOR -> true
            }
            matchesQuery && matchesModeAndFilter
        }
    }

    val itrInWorkCount = remember(itrScopeTasks) {
        itrScopeTasks.count {
            it.taskStatus == TaskStatus.NEW ||
                it.taskStatus == TaskStatus.IN_PROGRESS ||
                it.taskStatus == TaskStatus.REJECTED
        }
    }
    val itrOnReviewCount = remember(itrScopeTasks) {
        itrScopeTasks.count { it.taskStatus == TaskStatus.ON_REVIEW }
    }
    val itrProblemsCount = remember(itrScopeTasks) {
        itrScopeTasks.count {
            it.taskStatus == TaskStatus.OVERDUE ||
                it.taskStatus == TaskStatus.REJECTED ||
                it.carryoverCount > 0L
        }
    }
    val itrCompletedCount = remember(itrCompletedScopeTasks) {
        itrCompletedScopeTasks.size
    }
    val itrAllActiveCount = remember(itrScopeTasks) {
        itrScopeTasks.size
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

        // Переключатель ролевых интерфейсов показываем только для Администратора / Руководителя
        if (activeEmployee.role.isAdminOrHead && allowedModes.size > 1) {
            item {
                PersonaModeSwitcherBar(
                    selectedMode = effectivePersonaMode,
                    allowedModes = allowedModes,
                    onSelectMode = { mode ->
                        if (mode in allowedModes) {
                            selectedPersonaMode = mode
                            if (mode == PersonaUiMode.ITR && selectedFilter == TaskFilterTab.CALENDAR_DATE) {
                                onSelectFilter(TaskFilterTab.ALL)
                            }
                        }
                    }
                )
            }
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

        when (effectivePersonaMode) {
            // =========================================================================
            // 1. ИНТЕРФЕЙС РАБОЧЕГО (Монтажник)
            // =========================================================================
            PersonaUiMode.WORKER -> {
                val activeWorkerDate = if (selectedFilter == TaskFilterTab.CALENDAR_DATE) {
                    selectedCalendarEpochDay
                } else {
                    currentEpochDay
                }
                val doneCount = completedHomeTasks.size + activeHomeTasks.count {
                    it.taskStatus == TaskStatus.ON_REVIEW
                }
                item {
                    WorkerTodayHeaderCard(
                        currentEpochDay = currentEpochDay,
                        selectedEpochDay = activeWorkerDate,
                        totalTasksToday = activeHomeTasks.size + completedHomeTasks.size,
                        completedTasksToday = doneCount,
                        onCreateTaskClick = { onOpenCreateDialogForDate(activeWorkerDate) },
                        onSelectDate = { chosenDay ->
                            onSelectCalendarDate(chosenDay)
                        }
                    )
                }

                // Наглядный переключатель для Рабочего: В работе | Выполненные задачи
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (!workerShowCompletedTab) MaterialTheme.colorScheme.surface else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { workerShowCompletedTab = false }
                                    .testTag("worker_tab_active")
                            ) {
                                Text(
                                    text = "В работе (${activeHomeTasks.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (!workerShowCompletedTab) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!workerShowCompletedTab) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (workerShowCompletedTab) MaterialTheme.colorScheme.surface else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { workerShowCompletedTab = true }
                                    .testTag("worker_tab_completed")
                            ) {
                                Text(
                                    text = "Выполненные (${completedHomeTasks.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (workerShowCompletedTab) FontWeight.Bold else FontWeight.Medium,
                                    color = if (workerShowCompletedTab) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (filteredTasks.isEmpty()) {
                    item {
                        EmptyTasksCleanCard(
                            targetEpochDay = activeWorkerDate,
                            onCreateClick = { onOpenCreateDialogForDate(activeWorkerDate) }
                        )
                    }
                } else {
                    items(filteredTasks, key = { "worker_${it.id}" }) { task ->
                        WorkerPersonaTaskCard(
                            task = task,
                            onOpenDetail = { onTaskClick(task) },
                            onQuickComplete = { onQuickCompleteTask(task) },
                            onToggleSubtask = { subId -> onToggleSubtask(task, subId) },
                            canApprove = activeEmployee.role.canApproveReportOf(CorporateRole.WORKER),
                            onApproveTask = {
                                onApproveTask(task)
                                homeApprovedOutcomeTask = task
                            },
                            onRejectTask = {
                                homeRejectCommentInput = ""
                                homeTaskPendingReject = task
                            }
                        )
                    }
                }
                if (acceptedAndArchivedCount > 0 && !workerShowCompletedTab) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { workerShowCompletedTab = true }
                                .testTag("open_accepted_archive_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
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
                                        text = "Выполненные задачи ($acceptedAndArchivedCount) — показать список",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF059669)
                                    )
                                }
                                Text(
                                    text = "Открыть →",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
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
                        allVisibleTasks = itrScopeTasks,
                        onSelectDate = { day ->
                            if (selectedFilter == TaskFilterTab.CALENDAR_DATE && selectedCalendarEpochDay == day) {
                                onSelectFilter(TaskFilterTab.ALL)
                            } else {
                                onSelectCalendarDate(day)
                            }
                        },
                        onCreateTaskForDate = onOpenCreateDialogForDate,
                        activeEmployee = activeEmployee,
                        selectedScope = selectedItrScope,
                        subdivisionCount = itrSubdivisionCount,
                        siteCount = itrSiteCount,
                        personalCount = itrPersonalCount,
                        onSelectScope = { scope -> selectedItrScope = scope }
                    )
                }

                item {
                    ItrFilterPillsRow(
                        selectedItrFilter = selectedItrFilter,
                        inWorkCount = itrInWorkCount,
                        onReviewCount = itrOnReviewCount,
                        problemsCount = itrProblemsCount,
                        completedCount = itrCompletedCount,
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
                                    onToggleSubtask = { subId -> onToggleSubtask(task, subId) },
                                    canApprove = activeEmployee.role.canApproveReportOf(CorporateRole.WORKER),
                                    onApproveTask = {
                                        onApproveTask(task)
                                        homeApprovedOutcomeTask = task
                                    },
                                    onRejectTask = {
                                        homeRejectCommentInput = ""
                                        homeTaskPendingReject = task
                                    },
                                    onQuickComplete = { onQuickCompleteTask(task) }
                                )
                            }
                        }
                        if (itrCompletedCount > 0 && selectedItrFilter != ItrQuickFilter.COMPLETED) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedItrFilter = ItrQuickFilter.COMPLETED }
                                        .testTag("open_accepted_archive_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
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
                                                text = "Выполненные задачи ($itrCompletedCount) — показать выполненные",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                        Text(
                                            text = "Открыть →",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }
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
                        onApproveTask = { task ->
                            onApproveTask(task)
                            homeApprovedOutcomeTask = task
                        },
                        onRejectTask = { task ->
                            homeRejectCommentInput = ""
                            homeTaskPendingReject = task
                        },
                        onOpenTaskDetail = onTaskClick
                    )
                }
            }
        }
    }

    homeTaskPendingReject?.let { targetTask ->
        AlertDialog(
            onDismissRequest = { homeTaskPendingReject = null },
            title = { Text("Отклонение задания") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Укажите комментарий, почему произошло отклонение задания «${targetTask.title}»:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = homeRejectCommentInput,
                        onValueChange = { homeRejectCommentInput = it },
                        label = { Text("Комментарий (причина отклонения) *") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_rejection_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanReason = homeRejectCommentInput.trim()
                        if (cleanReason.isNotBlank()) {
                            onRejectTask(targetTask, cleanReason)
                            homeTaskPendingReject = null
                            homeRejectedOutcomeInfo = targetTask.title to cleanReason
                        }
                    },
                    enabled = homeRejectCommentInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Отклонить", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { homeTaskPendingReject = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    homeRejectedOutcomeInfo?.let { (taskTitle, commentText) ->
        AlertDialog(
            onDismissRequest = { homeRejectedOutcomeInfo = null },
            title = { Text("Задание отклонено") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Задание «$taskTitle» отклонено и возвращено на доработку.")
                    Text(
                        text = "Комментарий: $commentText",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(onClick = { homeRejectedOutcomeInfo = null }) {
                    Text("ОК")
                }
            }
        )
    }

    homeApprovedOutcomeTask?.let { acceptedTask ->
        AlertDialog(
            onDismissRequest = { homeApprovedOutcomeTask = null },
            title = { Text("Отчёт принят") },
            text = {
                Text("Выполнение задания «${acceptedTask.title}» подтверждено. Принятый отчёт перемещён с главной страницы в раздел принятых заданий (Приёмка / Архив).")
            },
            confirmButton = {
                Button(
                    onClick = { homeApprovedOutcomeTask = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    )
                ) {
                    Text("ОК")
                }
            }
        )
    }
}

@Composable
private fun EmptyTasksCleanCard(
    targetEpochDay: Long,
    onCreateClick: () -> Unit
) {
    val tokens = LocalAppThemeTokens.current
    val formattedDate = remember(targetEpochDay) {
        ReportDocumentGenerator.formatEpochDay(targetEpochDay)
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = Color(0xFF10B981)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val cardTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = surfaceColor,
        shadowElevation = tokens.cardShadowElevationDp.dp,
        border = if (tokens.cardBorderWidthDp > 0f) {
            BorderStroke(tokens.cardBorderWidthDp.dp, tokens.cardBorderColor)
        } else {
            null
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_tasks_state_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Стилизованная многослойная иллюстрация: стопка листов сменного задания + бренд-бейдж с галочкой
            Box(
                modifier = Modifier.size(116.dp),
                contentAlignment = Alignment.Center
            ) {
                // Мягкий ореол под иллюстрацией
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(cardTint)
                )

                // Нижний лист стопки (под небольшим смещением)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = primaryColor.copy(alpha = 0.14f),
                    modifier = Modifier
                        .size(width = 62.dp, height = 74.dp)
                        .padding(top = 8.dp, start = 10.dp)
                ) {}

                // Верхний основной лист чек-листа
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = surfaceColor,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.28f)),
                    modifier = Modifier.size(width = 60.dp, height = 72.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        // Заголовочная полоска документа
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(primaryColor)
                        )
                        // Строки чек-листа
                        repeat(3) { idx ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (idx == 0) secondaryColor else primaryColor.copy(alpha = 0.3f)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (idx == 2) 0.65f else 0.9f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(primaryColor.copy(alpha = 0.18f))
                                )
                            }
                        }
                    }
                }

                // Акцентный изумрудный чек-бокс в правом нижнем углу иллюстрации
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = secondaryColor,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 8.dp)
                        .size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "На $formattedDate задач пока нет",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Сменный план чист. Чтобы добавить новое поручение или объём работ, нажмите кнопку «+ Новая задача» в правом нижнем углу.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
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
    onOpenReportingTab: () -> Unit,
    canAccessReporting: Boolean = true,
    onTaskClick: (TaskDocument) -> Unit = {}
) {
    var expandedMonthView by remember { mutableStateOf(true) }
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
            .testTag("corporate_calendar_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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

            // Панель сводки и список задач по выбранной в календаре дате
            val formattedSelectedDate = ReportDocumentGenerator.formatEpochDay(selectedCalendarEpochDay)
            val dateRelationLabel = when (selectedCalendarEpochDay) {
                currentEpochDay -> "Сегодня"
                currentEpochDay + 1L -> "План на завтра"
                currentEpochDay - 1L -> "Вчера"
                else -> if (selectedCalendarEpochDay > currentEpochDay) "План работ" else "Архив дня"
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            text = "Задач: ${tasksOnSelectedDate.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Выполнено: $completedOnDate • На проверке: $onReviewOnDate • В работе: $plannedOrInProgressOnDate • Фото: $photosOnDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Список задач на выбранную дату прямо внутри развёрнутого календаря
                    if (tasksOnSelectedDate.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tasksOnSelectedDate.forEach { dayTask ->
                                val stColor = statusColor(dayTask.taskStatus)
                                val tVol = dayTask.effectiveTargetVolume
                                val cVol = dayTask.effectiveCompletedVolume
                                val uStr = dayTask.effectiveVolumeUnit.ifBlank { "шт" }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onTaskClick(dayTask) }
                                        .testTag("calendar_task_item_${dayTask.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(stColor)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = dayTask.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val metaLine = buildString {
                                                    append(dayTask.assigneeName.ifBlank { "Исполнитель" })
                                                    if (tVol > 0.0) {
                                                        append(" • ${SubtaskItem.formatVolumeNumber(cVol)}/${SubtaskItem.formatVolumeNumber(tVol)} $uStr")
                                                    }
                                                }
                                                Text(
                                                    text = metaLine,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = stColor.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = dayTask.taskStatus.titleRu,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = stColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "На выбранную дату задач пока не запланировано.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (!isCalendarFilterActive) {
                                    onSelectDate(selectedCalendarEpochDay)
                                } else if (canAccessReporting) {
                                    onOpenReportingTab()
                                } else {
                                    onCreateTaskForDate(selectedCalendarEpochDay)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            Text(
                                text = when {
                                    !isCalendarFilterActive -> "Показать задачи за $formattedSelectedDate"
                                    canAccessReporting -> "Перейти к отчёту за $formattedSelectedDate"
                                    else -> "+ Добавить задачу на $formattedSelectedDate"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
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
