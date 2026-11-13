package com.example.ui.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainActivity
import com.example.R
import com.example.data.local.LocalWorkspaceStore
import com.example.data.model.AuditLogDocument
import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.ExportFormat
import com.example.data.model.GeneratedReportDocument
import com.example.data.model.MemberApprovalStatus
import com.example.data.model.PhotoReportItem
import com.example.data.model.ReportTemplateDocument
import com.example.data.model.SheetLayout
import com.example.data.model.SiteMemberDocument
import com.example.data.model.StatisticsSummary
import com.example.data.model.StatusHistoryEntry
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskCommentItem
import com.example.data.model.TaskDocument
import com.example.data.model.TaskDraft
import com.example.data.model.TaskStatus
import com.example.data.model.UserProfile
import com.example.data.model.WorkGroup
import com.example.data.repository.EzhednevnikRepository
import com.example.data.seed.CorporateSeedData
import com.example.domain.reporting.GeneratedDocumentResult
import com.example.domain.reporting.ReportDocumentGenerator
import com.example.ui.auth.CorporateLoginSession
import com.example.ui.auth.saveCorporateSession
import com.example.ui.components.PhotoCaptureUtils
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class TaskFilterTab(val titleRu: String) {
    ALL("Все"),
    TODAY("Сегодня"),
    TOMORROW("Завтра"),
    CALENDAR_DATE("По дате"),
    ON_REVIEW("Проверка"),
    OVERDUE("Просрочено"),
    APPROVED("Готово"),
    RECURRING("Регулярные"),
    ARCHIVE("Архив")
}

class EzhednevnikViewModel(
    private val repository: EzhednevnikRepository,
    private val currentUserId: String,
    private val currentUserEmail: String = "",
    private val currentUserName: String = "",
    private val initialHeadActivated: Boolean = false,
    private val initialSubdivision: String = "",
    private val initialSiteName: String = "",
    private val initialPosition: String = "",
    private val appContext: Context? = null
) : ViewModel() {

    private val isCloudAuthenticated: Boolean
        get() = try {
            Firebase.auth.currentUser != null
        } catch (_: Exception) {
            false
        }

    private val _isCloudSyncActive = MutableStateFlow(isCloudAuthenticated)
    val isCloudSyncActive: StateFlow<Boolean> = _isCloudSyncActive.asStateFlow()

    private val notifiedPendingMemberKeys = mutableSetOf<String>()

    private val effectiveUserId: String
        get() = try {
            Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: currentUserId
        } catch (_: Exception) {
            currentUserId
        }

    private val _currentEpochDay = MutableStateFlow(CorporateSeedData.currentDeviceEpochDay())
    val currentEpochDay: StateFlow<Long> = _currentEpochDay.asStateFlow()

    private val _selectedCalendarEpochDay = MutableStateFlow(CorporateSeedData.currentDeviceEpochDay())
    val selectedCalendarEpochDay: StateFlow<Long> = _selectedCalendarEpochDay.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaskFilterTab.ALL)
    val selectedFilter: StateFlow<TaskFilterTab> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _activeReportPreview = MutableStateFlow<GeneratedDocumentResult?>(null)
    val activeReportPreview: StateFlow<GeneratedDocumentResult?> = _activeReportPreview.asStateFlow()

    private val _autoSchedulePlanEvening = MutableStateFlow(true)
    val autoSchedulePlanEvening: StateFlow<Boolean> = _autoSchedulePlanEvening.asStateFlow()

    private val _autoScheduleReportNight = MutableStateFlow(true)
    val autoScheduleReportNight: StateFlow<Boolean> = _autoScheduleReportNight.asStateFlow()

    private val _organizationName = MutableStateFlow(
        appContext?.let { LocalWorkspaceStore.loadOrganizationName(it) } ?: CorporateSeedData.DEFAULT_ORG_NAME
    )
    val organizationName: StateFlow<String> = _organizationName.asStateFlow()

    private val _siteName = MutableStateFlow(
        appContext?.let { LocalWorkspaceStore.loadSiteName(it) } ?: CorporateSeedData.DEFAULT_SITE_NAME
    )
    val siteName: StateFlow<String> = _siteName.asStateFlow()

    private val _subdivisionsList = MutableStateFlow(
        appContext?.let { LocalWorkspaceStore.loadSubdivisions(it) } ?: LocalWorkspaceStore.DEFAULT_SUBDIVISIONS
    )
    val subdivisionsList: StateFlow<List<String>> = _subdivisionsList.asStateFlow()

    private val _sitesList = MutableStateFlow(
        appContext?.let { LocalWorkspaceStore.loadSites(it) } ?: LocalWorkspaceStore.DEFAULT_SITES
    )
    val sitesList: StateFlow<List<String>> = _sitesList.asStateFlow()

    // Чистое локальное состояние без фейковых задач, фейковых сотрудников и дублирующих пунктов sub_vol_main
    private val _deletedTaskIds = MutableStateFlow(
        appContext?.let { LocalWorkspaceStore.loadDeletedTaskIds(it) } ?: emptySet()
    )
    private val _deletedMemberIds = MutableStateFlow(
        appContext?.let { LocalWorkspaceStore.loadDeletedMemberIds(it) } ?: emptySet()
    )
    private val _localTasks = MutableStateFlow(
        appContext?.let { ctx ->
            val todayReal = CorporateSeedData.currentDeviceEpochDay()
            LocalWorkspaceStore.loadTasks(ctx)
                .filterNot { CorporateSeedData.isLegacyFakeSeedTaskId(it.id) }
                .map { task ->
                    val cleanSubtasks = task.decodedSubtasks()
                    val cleanEncoded = cleanSubtasks.map { it.encode() }
                    val normalizedDateTask = if (task.dueDateEpochDay == 20768L && todayReal > 20768L) {
                        task.copy(dueDateEpochDay = todayReal, originalDueDateEpochDay = todayReal)
                    } else {
                        task
                    }
                    if (cleanEncoded != normalizedDateTask.subtaskItems) {
                        val newT = if (cleanSubtasks.isNotEmpty()) cleanSubtasks.sumOf { it.effectiveTargetVolume } else normalizedDateTask.effectiveTargetVolume
                        val newC = if (cleanSubtasks.isNotEmpty()) cleanSubtasks.sumOf { it.effectiveCompletedVolume }.coerceIn(0.0, newT) else normalizedDateTask.effectiveCompletedVolume
                        normalizedDateTask.copy(subtaskItems = cleanEncoded).withUpdatedVolume(newT, newC, normalizedDateTask.effectiveVolumeUnit)
                    } else {
                        normalizedDateTask
                    }
                }
        } ?: emptyList()
    )
    private val _localSiteMembers = MutableStateFlow(
        appContext?.let { ctx ->
            LocalWorkspaceStore.loadMembers(ctx).filterNot {
                CorporateSeedData.isLegacyFakeSeedMemberId(it.memberId.ifBlank { it.id })
            }
        } ?: emptyList()
    )
    private val _localTemplates = MutableStateFlow(
        CorporateSeedData.buildInitialReportTemplates(
            userId = currentUserId,
            dateLabel = ReportDocumentGenerator.formatEpochDay(_currentEpochDay.value),
            orgName = _organizationName.value,
            siteName = _siteName.value
        )
    )
    private val _localGeneratedReports = MutableStateFlow<List<GeneratedReportDocument>>(emptyList())
    private val _localAuditLogs = MutableStateFlow<List<AuditLogDocument>>(emptyList())

    private val _currentMemberRecord = MutableStateFlow<SiteMemberDocument?>(null)
    val currentMemberRecord: StateFlow<SiteMemberDocument?> = _currentMemberRecord.asStateFlow()

    private fun normalizeMemberKey(raw: String): String =
        raw.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

    private val normalizedCurrentUserId: String =
        SiteMemberDocument.canonicalMemberId(currentUserId, currentUserEmail)

    private val firestoreProfileFlow = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .onStart { emit(UiState.Success(null)) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(
                UiState.Success(
                    UserProfile(
                        id = currentUserId,
                        userId = currentUserId,
                        displayName = currentUserName.ifBlank { "Руководитель" },
                        siteName = CorporateSeedData.DEFAULT_SITE_NAME,
                        activePersonaId = currentUserId
                    )
                )
            )
        }

    val userProfileState: StateFlow<UiState<UserProfile?>> = firestoreProfileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    private val firestoreSiteMembersFlow = repository.observeSiteMembers()
        .onStart { emit(emptyList()) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }

    // Объединение облачных и локальных сотрудников без потери заявок на назначение роли
    val siteMembersState: StateFlow<List<SiteMemberDocument>> = combine(
        firestoreSiteMembersFlow,
        _localSiteMembers,
        _deletedMemberIds
    ) { remoteList, localList, deletedIds ->
        val mergedMap = LinkedHashMap<String, SiteMemberDocument>()
        remoteList.forEach { m ->
            val rawKey = m.memberId.ifBlank { m.id }
            val normKey = normalizeMemberKey(rawKey)
            val canonicalKey = SiteMemberDocument.canonicalMemberId(rawKey, m.email)
            val isDeleted = (rawKey in deletedIds || normKey in deletedIds || canonicalKey in deletedIds) && !m.isPendingRole
            if (canonicalKey.isNotBlank() &&
                !CorporateSeedData.isLegacyFakeSeedMemberId(rawKey) &&
                !CorporateSeedData.isLegacyFakeSeedMemberId(normKey) &&
                !isDeleted
            ) {
                mergedMap[canonicalKey] = m.copy(id = canonicalKey, memberId = canonicalKey)
            }
        }
        // Локальный список объединяется с облачным:
        // - если в облаке заявка ожидает подтверждения (PENDING), облачная заявка имеет приоритет над устаревшей локальной записью,
        //   кроме случая, когда администратор только что локально утвердил эту заявку (!m.isPendingRole && !m.isRejected).
        // - если в облаке заявка уже подтверждена (APPROVED), сохраняем подтверждённую роль из облака.
        localList.forEach { m ->
            val rawKey = m.memberId.ifBlank { m.id }
            val normKey = normalizeMemberKey(rawKey)
            val canonicalKey = SiteMemberDocument.canonicalMemberId(rawKey, m.email)
            val isDeleted = (rawKey in deletedIds || normKey in deletedIds || canonicalKey in deletedIds) && !m.isPendingRole
            if (canonicalKey.isNotBlank() &&
                !CorporateSeedData.isLegacyFakeSeedMemberId(rawKey) &&
                !CorporateSeedData.isLegacyFakeSeedMemberId(normKey) &&
                !isDeleted
            ) {
                val existingRemote = mergedMap[canonicalKey]
                when {
                    existingRemote == null -> {
                        mergedMap[canonicalKey] = m.copy(id = canonicalKey, memberId = canonicalKey)
                    }
                    existingRemote.isPendingRole && !existingRemote.isRejected -> {
                        // В облаке висит активная заявка PENDING: оставляем её видимой для администратора,
                        // заменяя только если администратор уже утвердил её локально
                        val isAdminActor = initialHeadActivated || SiteMemberDocument.isRootAdminEmail(currentUserEmail)
                        if (isAdminActor && !m.isPendingRole && !m.isRejected && m.cleanApprovedByName.isNotBlank() && m.cleanApprovedByName != "Заявка") {
                            mergedMap[canonicalKey] = m.copy(id = canonicalKey, memberId = canonicalKey)
                        } else {
                            mergedMap[canonicalKey] = existingRemote
                        }
                    }
                    !existingRemote.isPendingRole && !existingRemote.isRejected && m.isPendingRole -> {
                        mergedMap[canonicalKey] = existingRemote
                    }
                    else -> {
                        mergedMap[canonicalKey] = m.copy(id = canonicalKey, memberId = canonicalKey)
                    }
                }
            }
        }
        mergedMap.values.sortedWith(
            compareBy<SiteMemberDocument> { !it.isPendingRole }
                .thenBy { it.fullName }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = _localSiteMembers.value
    )

    val activeEmployee: StateFlow<CorporateEmployee> = combine(
        _currentMemberRecord,
        siteMembersState,
        _siteName
    ) { memberRecord, allMembers, currentSiteName ->
        // Сначала проверяем обновлённый _currentMemberRecord, затем ищем в списке участников
        val matchedFromList = allMembers.firstOrNull { m ->
            val mNorm = normalizeMemberKey(m.memberId.ifBlank { m.id })
            mNorm == normalizedCurrentUserId ||
                m.memberId == currentUserId ||
                m.id == currentUserId ||
                (memberRecord != null && mNorm == normalizeMemberKey(memberRecord.memberId.ifBlank { memberRecord.id })) ||
                (currentUserEmail.isNotBlank() && m.email.equals(currentUserEmail, ignoreCase = true))
        }
        val matched = memberRecord ?: matchedFromList

        if (matched != null) {
            matched.toCorporateEmployee(defaultSiteName = currentSiteName)
        } else {
            val isRootAdmin = initialHeadActivated || SiteMemberDocument.isRootAdminEmail(currentUserEmail)
            val fallbackRole = if (isRootAdmin) CorporateRole.ADMIN else CorporateRole.HEAD
            val defaultName = currentUserName.ifBlank {
                currentUserEmail.substringBefore("@").ifBlank { fallbackRole.titleRu }
            }
            CorporateEmployee(
                id = currentUserId,
                fullName = defaultName,
                shortName = defaultName,
                position = fallbackRole.titleRu,
                role = fallbackRole,
                groupId = "grp_control",
                groupName = initialSubdivision.ifBlank { "АУП и управление участком" },
                siteId = CorporateSeedData.DEFAULT_SITE_ID,
                siteName = initialSiteName.ifBlank { currentSiteName },
                supervisorId = null,
                phone = "",
                email = currentUserEmail
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CorporateEmployee(
            id = currentUserId,
            fullName = currentUserName.ifBlank { "Администратор" },
            shortName = currentUserName.ifBlank { "Администратор" },
            position = "Администратор",
            role = if (initialHeadActivated || SiteMemberDocument.isRootAdminEmail(currentUserEmail)) {
                CorporateRole.ADMIN
            } else {
                CorporateRole.HEAD
            },
            groupId = "grp_control",
            groupName = initialSubdivision.ifBlank { "АУП и управление участком" },
            siteId = CorporateSeedData.DEFAULT_SITE_ID,
            siteName = initialSiteName.ifBlank { _siteName.value },
            supervisorId = null,
            phone = "",
            email = currentUserEmail
        )
    )

    // Список реальных сотрудников для назначения задач (без фейковых людей)
    val availableEmployees: StateFlow<List<CorporateEmployee>> = combine(
        activeEmployee,
        siteMembersState,
        _siteName
    ) { currentEmp, members, currentSiteName ->
        val approvedEmployees = members
            .filter { !it.isPendingRole && !it.isRejected }
            .map { it.toCorporateEmployee(defaultSiteName = currentSiteName) }
        val map = LinkedHashMap<String, CorporateEmployee>()
        map[normalizeMemberKey(currentEmp.id)] = currentEmp
        approvedEmployees.forEach { emp ->
            map[normalizeMemberKey(emp.id)] = emp
        }
        map.values.toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = listOf(activeEmployee.value)
    )

    private val firestoreTasksFlow = repository.observeTasks(currentUserId)
        .onStart { emit(emptyList()) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }

    // Локальные задачи и изменения объединяются поверх Firestore и фильтруют удалённые/демо задачи
    val tasksState: StateFlow<UiState<List<TaskDocument>>> = combine(
        firestoreTasksFlow,
        _localTasks,
        _deletedTaskIds
    ) { remoteTasks, localTasks, deletedIds ->
        val mergedMap = LinkedHashMap<String, TaskDocument>()
        remoteTasks.forEach { task ->
            if (task.id.isNotBlank() &&
                !CorporateSeedData.isLegacyFakeSeedTaskId(task.id) &&
                task.id !in deletedIds
            ) {
                mergedMap[task.id] = task
            }
        }
        // Объединяем локальные и облачные задачи, выбирая более свежую ревизию по истории/фото/комментариям
        localTasks.forEach { localTask ->
            if (localTask.id.isNotBlank() &&
                !CorporateSeedData.isLegacyFakeSeedTaskId(localTask.id) &&
                localTask.id !in deletedIds
            ) {
                val remoteTask = mergedMap[localTask.id]
                if (remoteTask == null) {
                    mergedMap[localTask.id] = localTask
                } else {
                    val remoteWeight = remoteTask.historyItems.size * 10 + remoteTask.photoItems.size * 5 + remoteTask.commentItems.size * 2
                    val localWeight = localTask.historyItems.size * 10 + localTask.photoItems.size * 5 + localTask.commentItems.size * 2
                    mergedMap[localTask.id] = if (remoteWeight > localWeight) remoteTask else localTask
                }
            }
        }
        UiState.Success(
            mergedMap.values.sortedWith(
                compareBy<TaskDocument> { it.taskStatus.isTerminalOrDone }
                    .thenBy { it.dueDateEpochDay }
                    .thenByDescending { it.taskPriority.weight }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Success(_localTasks.value)
    )

    private val firestoreTemplatesFlow = repository.observeReportTemplates(currentUserId)
        .onStart { emit(emptyList()) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }

    val templatesState: StateFlow<UiState<List<ReportTemplateDocument>>> = combine(
        firestoreTemplatesFlow,
        _localTemplates,
        _currentEpochDay,
        _organizationName,
        _siteName
    ) { remote, local, epochDay, org, site ->
        val baseList = if (remote.isNotEmpty()) remote else local
        val normalized = baseList.map { tpl ->
            val resolvedName = ReportDocumentGenerator.resolveTemplateDisplayName(tpl, epochDay)
            val resolvedHeader = "$org • $site"
            tpl.copy(
                name = resolvedName,
                orgHeader = resolvedHeader
            )
        }
        UiState.Success(normalized)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Success(_localTemplates.value)
    )

    private val firestoreGeneratedReportsFlow = repository.observeGeneratedReports(currentUserId)
        .onStart { emit(emptyList()) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }

    val generatedReportsState: StateFlow<UiState<List<GeneratedReportDocument>>> = combine(
        firestoreGeneratedReportsFlow,
        _localGeneratedReports
    ) { remote, local ->
        val merged = (local + remote).distinctBy { it.id.ifBlank { it.filePath } }
        UiState.Success(merged)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Success(emptyList())
    )

    private val firestoreAuditLogsFlow = repository.observeAuditLogs(currentUserId)
        .onStart { emit(emptyList()) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }

    val auditLogsState: StateFlow<UiState<List<AuditLogDocument>>> = combine(
        firestoreAuditLogsFlow,
        _localAuditLogs
    ) { remote, local ->
        val cleanRemote = remote.filterNot { it.id.contains("audit_seed_") }
        val merged = (local + cleanRemote).distinctBy { it.id }
        UiState.Success(merged)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Success(_localAuditLogs.value)
    )

    val statisticsState: StateFlow<StatisticsSummary> = combine(
        activeEmployee,
        tasksState,
        availableEmployees
    ) { employee, tState, employeesList ->
        val allTasks = (tState as? UiState.Success)?.data ?: emptyList()
        repository.calculateRoleScopedStatistics(employee, allTasks, employeesList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = repository.calculateRoleScopedStatistics(
            activeEmployee.value,
            _localTasks.value,
            listOf(activeEmployee.value)
        )
    )

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val connected = firebaseAuth.currentUser != null
        val wasConnected = _isCloudSyncActive.value
        _isCloudSyncActive.value = connected
        if (connected && !wasConnected) {
            onCloudAuthConnected()
        }
    }

    init {
        try {
            Firebase.auth.addAuthStateListener(authStateListener)
        } catch (_: Exception) {
        }
        initializeUserAndRoleStatus()
        observeMemberRoleUpdates()
        startPeriodicCloudMembersSync()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            Firebase.auth.removeAuthStateListener(authStateListener)
        } catch (_: Exception) {
        }
    }

    fun onCloudAuthConnected() {
        _isCloudSyncActive.value = isCloudAuthenticated
        initializeUserAndRoleStatus()
        refreshSiteMembersQueue(showBanner = true)
    }

    private fun startPeriodicCloudMembersSync() {
        viewModelScope.launch {
            // Первичная подгрузка очереди заявок из Firestore при старте
            delay(1200L)
            if (isCloudAuthenticated) {
                syncSiteMembersFromCloudSilently()
            }
            while (isActive) {
                delay(12_000L)
                _isCloudSyncActive.value = isCloudAuthenticated
                if (isCloudAuthenticated) {
                    syncSiteMembersFromCloudSilently()
                    val cur = _currentMemberRecord.value
                    if (cur != null && cur.isPendingRole) {
                        val grp = CorporateSeedData.workGroups.firstOrNull {
                            it.name.equals(cur.groupName, ignoreCase = true)
                        }?.copy(name = cur.groupName)
                            ?: CorporateSeedData.workGroups.first().copy(id = cur.groupId, name = cur.groupName)
                        val remoteResolved = repository.registerOrResolveSiteMember(
                            rawMemberId = SiteMemberDocument.canonicalMemberId(cur.memberId.ifBlank { cur.id }, cur.email),
                            email = cur.email,
                            fullName = cur.fullName,
                            position = cur.position,
                            requestedGroup = grp,
                            siteName = cur.effectiveSiteName.ifBlank { _siteName.value },
                            forceHeadActivation = false
                        ).getOrNull()
                        if (remoteResolved != null && !remoteResolved.isPendingRole && !remoteResolved.isRejected) {
                            val canonicalId = SiteMemberDocument.canonicalMemberId(cur.memberId.ifBlank { cur.id }, cur.email)
                            val unlocked = remoteResolved.copy(id = canonicalId, memberId = canonicalId)
                            _currentMemberRecord.value = unlocked
                            upsertLocalMember(unlocked)
                            _bannerMessage.value = "Доступ открыт! Назначена роль: ${unlocked.parsedRoleOrNull?.titleRu}"
                        }
                    }
                }
            }
        }
    }

    private suspend fun syncSiteMembersFromCloudSilently() {
        val remoteMembers = repository.fetchAllSiteMembersOnce()
        if (remoteMembers.isEmpty()) return
        remoteMembers.forEach { remote ->
            val canonicalId = SiteMemberDocument.canonicalMemberId(remote.memberId.ifBlank { remote.id }, remote.email)
            val existingLocal = _localSiteMembers.value.firstOrNull {
                SiteMemberDocument.canonicalMemberId(it.memberId.ifBlank { it.id }, it.email) == canonicalId
            }
            if (existingLocal == null ||
                (remote.isPendingRole && !remote.isRejected) ||
                (!remote.isPendingRole && !remote.isRejected && existingLocal.isPendingRole)
            ) {
                upsertLocalMember(remote.copy(id = canonicalId, memberId = canonicalId))
            }
        }
    }

    private fun postSystemNotificationForPendingMember(member: SiteMemberDocument, totalPendingCount: Int) {
        val ctx = appContext ?: return
        try {
            val channelId = "pending_user_requests_channel"
            val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Заявки новых пользователей",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Извещения руководителю и администратору о новых заявках на вход в систему"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                ctx,
                member.memberId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val emailPart = if (member.email.isNotBlank() && !member.email.endsWith("@site1.ru")) {
                " (${member.email})"
            } else {
                ""
            }
            val detailsText = buildString {
                append("${member.fullName}$emailPart ожидает подтверждения роли")
                if (member.position.isNotBlank()) append(" • ${member.position}")
                if (member.groupName.isNotBlank()) append(" • ${member.groupName}")
                if (totalPendingCount > 1) append(" (всего ожидают: $totalPendingCount чел.)")
            }

            val notification = NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Новая заявка на вход в систему")
                .setContentText(detailsText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(detailsText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(
                10000 + (member.memberId.ifBlank { member.id }.hashCode() and 0x7FFF),
                notification
            )
        } catch (_: Exception) {
            // Безопасная обработка при отсутствии разрешения POST_NOTIFICATIONS или в unit-тестах
        }
    }

    private fun observeMemberRoleUpdates() {
        viewModelScope.launch {
            siteMembersState.collect { members ->
                // Sync subdivisions and sites from discovered member records
                val remoteSubdivisions = members.map { it.groupName.trim() }.filter { it.isNotBlank() }
                val mergedSubdivisions = (_subdivisionsList.value + remoteSubdivisions).distinct()
                if (mergedSubdivisions.size != _subdivisionsList.value.size) {
                    _subdivisionsList.value = mergedSubdivisions
                    appContext?.let { LocalWorkspaceStore.saveSubdivisions(it, mergedSubdivisions) }
                }
                val remoteSites = members.map { it.effectiveSiteName.trim() }.filter { it.isNotBlank() }
                val mergedSites = (_sitesList.value + remoteSites).distinct()
                if (mergedSites.size != _sitesList.value.size) {
                    _sitesList.value = mergedSites
                    appContext?.let { LocalWorkspaceStore.saveSites(it, mergedSites) }
                }

                // Извещение для Администратора / Руководителя о новых пользователях, ожидающих подтверждения
                val isCurrentAdminOrHead = initialHeadActivated ||
                    SiteMemberDocument.isRootAdminEmail(currentUserEmail) ||
                    activeEmployee.value.role.isAdminOrHead

                if (isCurrentAdminOrHead) {
                    val activePendingList = members.filter { it.isPendingRole && !it.isRejected }
                    val newPendingMembers = activePendingList.filter { pending ->
                        val key = SiteMemberDocument.canonicalMemberId(
                            pending.memberId.ifBlank { pending.id },
                            pending.email
                        )
                        key.isNotBlank() && key != normalizedCurrentUserId && notifiedPendingMemberKeys.add(key)
                    }
                    if (newPendingMembers.isNotEmpty()) {
                        val newest = newPendingMembers.first()
                        val emailLabel = newest.email.takeIf { it.isNotBlank() && !it.endsWith("@site1.ru") }?.let { " ($it)" }.orEmpty()
                        _bannerMessage.value = if (activePendingList.size == 1) {
                            "Новая заявка на вход: ${newest.fullName}$emailLabel — ожидает подтверждения роли"
                        } else {
                            "Новые заявки на вход (${activePendingList.size} чел.): ${newest.fullName}$emailLabel — назначьте роль"
                        }
                        newPendingMembers.forEach { pendingUser ->
                            postSystemNotificationForPendingMember(pendingUser, activePendingList.size)
                        }
                    }
                }

                // If current user was pending and got approved in siteMembersState, unlock immediately
                val cur = _currentMemberRecord.value ?: return@collect
                if (cur.isPendingRole) {
                    val curCanonical = SiteMemberDocument.canonicalMemberId(cur.memberId.ifBlank { cur.id }, cur.email)
                    val matched = members.firstOrNull { m ->
                        val mCanonical = SiteMemberDocument.canonicalMemberId(m.memberId.ifBlank { m.id }, m.email)
                        mCanonical == curCanonical ||
                            (cur.email.isNotBlank() && !cur.email.endsWith("@site1.ru") && m.email.equals(cur.email, ignoreCase = true))
                    }
                    if (matched != null && !matched.isPendingRole && !matched.isRejected) {
                        _currentMemberRecord.value = matched
                        upsertLocalMember(matched)
                    }
                }
            }
        }
    }

    private fun persistTasksLocally(tasks: List<TaskDocument>) {
        _localTasks.value = tasks
        appContext?.let { LocalWorkspaceStore.saveTasks(it, tasks) }
    }

    private fun persistMembersLocally(members: List<SiteMemberDocument>) {
        _localSiteMembers.value = members
        appContext?.let { LocalWorkspaceStore.saveMembers(it, members) }
    }

    private fun initializeUserAndRoleStatus() {
        viewModelScope.launch {
            val cleanName = currentUserName.ifBlank {
                currentUserEmail.substringBefore("@").ifBlank { "Администратор" }
            }
            if (isCloudAuthenticated) {
                repository.ensureWorkspaceSeeded(cleanName)
            }

            val normalizedEmail = currentUserEmail.trim().lowercase()
            val isRootAdmin = initialHeadActivated || SiteMemberDocument.isRootAdminEmail(normalizedEmail)

            // Un-mark current user's keys from deletedIds so re-registration is never hidden
            val keysToUnblock = listOf(
                currentUserId,
                normalizeMemberKey(currentUserId),
                normalizedCurrentUserId
            ).filter { it.isNotBlank() }.distinct()
            _deletedMemberIds.value = _deletedMemberIds.value - keysToUnblock.toSet()
            appContext?.let { LocalWorkspaceStore.removeDeletedMemberIds(it, keysToUnblock) }

            // Refresh local store snapshot on startup
            appContext?.let { ctx ->
                val freshLocal = LocalWorkspaceStore.loadMembers(ctx).filterNot {
                    CorporateSeedData.isLegacyFakeSeedMemberId(it.memberId.ifBlank { it.id })
                }
                if (freshLocal.isNotEmpty()) {
                    _localSiteMembers.value = freshLocal
                }
            }

            val existingLocal = _localSiteMembers.value.firstOrNull { m ->
                val mEmail = m.email.trim().lowercase()
                val hasDifferentRealEmail = normalizedEmail.isNotBlank() &&
                    !normalizedEmail.endsWith("@site1.ru") &&
                    mEmail.isNotBlank() &&
                    !mEmail.endsWith("@site1.ru") &&
                    !mEmail.equals(normalizedEmail, ignoreCase = true)

                if (hasDifferentRealEmail) {
                    false
                } else {
                    val mNorm = SiteMemberDocument.canonicalMemberId(m.memberId.ifBlank { m.id }, mEmail)
                    mNorm == normalizedCurrentUserId ||
                        m.memberId == currentUserId ||
                        m.id == currentUserId ||
                        (normalizedEmail.isNotBlank() && mEmail == normalizedEmail)
                }
            }

            val resolvedMember = if (existingLocal != null) {
                val preserved = if (isRootAdmin && (existingLocal.isPendingRole || existingLocal.parsedRoleOrNull != CorporateRole.ADMIN)) {
                    val baseAdmin = existingLocal.copy(
                        id = normalizedCurrentUserId,
                        memberId = normalizedCurrentUserId,
                        email = normalizedEmail.ifBlank { existingLocal.email },
                        role = CorporateRole.ADMIN.code,
                        approvalStatus = MemberApprovalStatus.APPROVED.code,
                        position = existingLocal.position.ifBlank { "Администратор" }
                    )
                    baseAdmin.copy(
                        approvedByName = baseAdmin.encodeApprovedByForFirestore(
                            approverName = "Администратор",
                            isAdminRole = true
                        )
                    )
                } else if (!isRootAdmin && existingLocal.isPendingRole) {
                    val updatedSub = initialSubdivision.ifBlank { existingLocal.groupName }
                    val updatedSite = initialSiteName.ifBlank { existingLocal.effectiveSiteName.ifBlank { _siteName.value } }
                    val updatedPos = initialPosition.ifBlank { existingLocal.position.ifBlank { "Специалист" } }
                    val matchedGroup = CorporateSeedData.workGroups.firstOrNull {
                        it.name.equals(updatedSub, ignoreCase = true)
                    } ?: CorporateSeedData.workGroups.first().copy(name = updatedSub)
                    val draftPending = existingLocal.copy(
                        id = normalizedCurrentUserId,
                        memberId = normalizedCurrentUserId,
                        email = normalizedEmail.ifBlank { existingLocal.email },
                        fullName = cleanName.ifBlank { existingLocal.fullName },
                        position = updatedPos,
                        groupId = matchedGroup.id,
                        groupName = updatedSub,
                        siteName = updatedSite
                    )
                    draftPending.copy(
                        approvedByName = draftPending.encodeApprovedByForFirestore(
                            approverName = "Заявка",
                            explicitSiteName = updatedSite,
                            isAdminRole = false
                        )
                    )
                } else {
                    existingLocal.copy(
                        id = normalizedCurrentUserId,
                        memberId = normalizedCurrentUserId,
                        email = normalizedEmail.ifBlank { existingLocal.email }
                    )
                }
                upsertLocalMember(preserved, previousKey = existingLocal.memberId.ifBlank { existingLocal.id })

                // Always sync with Firestore so pending users appear on Admin devices and approved users get unlocked
                val remoteSynced = if (isCloudAuthenticated) {
                    val grp = CorporateSeedData.workGroups.firstOrNull {
                        it.name.equals(preserved.groupName, ignoreCase = true)
                    }?.copy(name = preserved.groupName)
                        ?: CorporateSeedData.workGroups.first().copy(id = preserved.groupId, name = preserved.groupName)

                    repository.registerOrResolveSiteMember(
                        rawMemberId = normalizedCurrentUserId,
                        email = preserved.email,
                        fullName = preserved.fullName,
                        position = preserved.position,
                        requestedGroup = grp,
                        siteName = preserved.effectiveSiteName.ifBlank { _siteName.value },
                        forceHeadActivation = isRootAdmin
                    ).getOrNull()
                } else {
                    null
                }

                if (remoteSynced != null && !remoteSynced.isPendingRole && !remoteSynced.isRejected && preserved.isPendingRole) {
                    val unlocked = remoteSynced.copy(
                        id = normalizedCurrentUserId,
                        memberId = normalizedCurrentUserId
                    )
                    upsertLocalMember(unlocked)
                    unlocked
                } else {
                    preserved
                }
            } else if (isRootAdmin) {
                val resolvedSubdivision = initialSubdivision.ifBlank { "АУП и управление участком" }
                val resolvedSite = initialSiteName.ifBlank { _siteName.value }
                val baseAdminDoc = SiteMemberDocument(
                    id = normalizedCurrentUserId,
                    memberId = normalizedCurrentUserId,
                    email = normalizedEmail,
                    fullName = cleanName,
                    position = initialPosition.ifBlank { "Администратор" },
                    role = CorporateRole.ADMIN.code,
                    approvalStatus = MemberApprovalStatus.APPROVED.code,
                    groupId = "grp_control",
                    groupName = resolvedSubdivision,
                    approvedByName = "Администратор",
                    siteName = resolvedSite
                )
                val adminDoc = baseAdminDoc.copy(
                    approvedByName = baseAdminDoc.encodeApprovedByForFirestore(
                        approverName = "Администратор",
                        explicitSiteName = resolvedSite,
                        isAdminRole = true
                    )
                )
                upsertLocalMember(adminDoc)
                if (isCloudAuthenticated) {
                    repository.registerOrResolveSiteMember(
                        rawMemberId = normalizedCurrentUserId,
                        email = adminDoc.email,
                        fullName = adminDoc.fullName,
                        position = adminDoc.position,
                        requestedGroup = CorporateSeedData.workGroups.last().copy(name = resolvedSubdivision),
                        siteName = resolvedSite,
                        forceHeadActivation = true
                    )
                }
                adminDoc
            } else {
                val resolvedSubdivision = initialSubdivision.ifBlank {
                    _subdivisionsList.value.firstOrNull() ?: CorporateSeedData.workGroups.first().name
                }
                val resolvedSite = initialSiteName.ifBlank { _siteName.value }
                val resolvedPos = initialPosition.ifBlank { "Специалист" }
                val matchedGroup = CorporateSeedData.workGroups.firstOrNull {
                    it.name.equals(resolvedSubdivision, ignoreCase = true)
                } ?: CorporateSeedData.workGroups.first().copy(name = resolvedSubdivision)

                if (resolvedSite.isNotBlank() && resolvedSite !in _sitesList.value) {
                    val updatedSites = (_sitesList.value + resolvedSite).distinct()
                    _sitesList.value = updatedSites
                    appContext?.let { LocalWorkspaceStore.saveSites(it, updatedSites) }
                }
                if (resolvedSubdivision.isNotBlank() && resolvedSubdivision !in _subdivisionsList.value) {
                    val updatedSubdivisions = (_subdivisionsList.value + resolvedSubdivision).distinct()
                    _subdivisionsList.value = updatedSubdivisions
                    appContext?.let { LocalWorkspaceStore.saveSubdivisions(it, updatedSubdivisions) }
                }

                val baseLocalPending = SiteMemberDocument(
                    id = normalizedCurrentUserId,
                    memberId = normalizedCurrentUserId,
                    email = normalizedEmail,
                    fullName = cleanName,
                    position = resolvedPos,
                    role = "PENDING",
                    approvalStatus = MemberApprovalStatus.PENDING.code,
                    groupId = matchedGroup.id,
                    groupName = resolvedSubdivision,
                    siteName = resolvedSite
                ).let { doc ->
                    doc.copy(
                        approvedByName = doc.encodeApprovedByForFirestore(
                            approverName = "Заявка",
                            explicitSiteName = resolvedSite,
                            isAdminRole = false
                        )
                    )
                }
                // Save locally first so the pending request is persisted immediately even before network completes
                upsertLocalMember(baseLocalPending)
                _currentMemberRecord.value = baseLocalPending

                val created = repository.registerOrResolveSiteMember(
                    rawMemberId = normalizedCurrentUserId,
                    email = normalizedEmail,
                    fullName = cleanName,
                    position = resolvedPos,
                    requestedGroup = matchedGroup.copy(name = resolvedSubdivision),
                    siteName = resolvedSite,
                    forceHeadActivation = false
                ).getOrNull()?.let { remoteMember ->
                    remoteMember.copy(
                        id = normalizedCurrentUserId,
                        memberId = normalizedCurrentUserId,
                        groupName = remoteMember.groupName.ifBlank { resolvedSubdivision },
                        siteName = remoteMember.effectiveSiteName.ifBlank { resolvedSite }
                    )
                } ?: baseLocalPending

                upsertLocalMember(created)
                created
            }

            _currentMemberRecord.value = resolvedMember
        }
    }

    private fun upsertLocalMember(member: SiteMemberDocument, previousKey: String? = null) {
        val rawKey = member.memberId.ifBlank { member.id }
        val normKey = SiteMemberDocument.canonicalMemberId(rawKey, member.email)
            .ifBlank { "emp_${System.currentTimeMillis()}" }
        val prevNormKey = previousKey?.let { normalizeMemberKey(it) }
        val cleanMemberEmail = member.email.trim().lowercase()
        val hasRealEmail = cleanMemberEmail.isNotBlank() && !cleanMemberEmail.endsWith("@site1.ru")

        val keysToUnblock = listOfNotNull(rawKey, normalizeMemberKey(rawKey), normKey, previousKey, prevNormKey)
            .filter { it.isNotBlank() }
            .distinct()
        _deletedMemberIds.value = _deletedMemberIds.value - keysToUnblock.toSet()
        appContext?.let { LocalWorkspaceStore.removeDeletedMemberIds(it, keysToUnblock) }

        val normalizedMember = member.copy(id = normKey, memberId = normKey)
        val updated = listOf(normalizedMember) +
            _localSiteMembers.value.filterNot { existing ->
                val itRaw = existing.memberId.ifBlank { existing.id }
                val itNorm = SiteMemberDocument.canonicalMemberId(itRaw, existing.email)
                val existingEmail = existing.email.trim().lowercase()
                val sameRealEmail = hasRealEmail && existingEmail == cleanMemberEmail
                itNorm == normKey ||
                    itRaw == rawKey ||
                    sameRealEmail ||
                    (prevNormKey != null && (itNorm == prevNormKey || itRaw == previousKey))
            }
        persistMembersLocally(updated)
    }

    private fun upsertLocalTask(task: TaskDocument) {
        val updated = listOf(task) + _localTasks.value.filterNot { it.id == task.id }
        persistTasksLocally(updated)
    }

    private fun findTaskById(taskId: String): TaskDocument? {
        val fromLocal = _localTasks.value.firstOrNull { it.id == taskId }
        val fromState = (tasksState.value as? UiState.Success)?.data?.firstOrNull { it.id == taskId }
        return when {
            fromLocal != null && fromState != null -> {
                val stateWeight = fromState.historyItems.size * 10 + fromState.photoItems.size * 5 + fromState.commentItems.size * 2
                val localWeight = fromLocal.historyItems.size * 10 + fromLocal.photoItems.size * 5 + fromLocal.commentItems.size * 2
                if (stateWeight > localWeight) fromState else fromLocal
            }
            fromLocal != null -> fromLocal
            else -> fromState
        }
    }

    fun updateMyPendingRequestDetails(
        position: String,
        group: WorkGroup,
        siteName: String = _siteName.value
    ) {
        val current = _currentMemberRecord.value ?: return
        val cleanSite = siteName.trim().ifBlank { _siteName.value }
        val canonicalId = SiteMemberDocument.canonicalMemberId(current.memberId.ifBlank { current.id }, current.email)
        val draftUpdated = current.copy(
            id = canonicalId,
            memberId = canonicalId,
            position = position.ifBlank { "Специалист" },
            groupId = group.id,
            groupName = group.name,
            siteName = cleanSite
        )
        val updated = draftUpdated.copy(
            approvedByName = draftUpdated.encodeApprovedByForFirestore(
                approverName = "Заявка",
                explicitSiteName = cleanSite,
                isAdminRole = false
            )
        )
        _currentMemberRecord.value = updated
        upsertLocalMember(updated, previousKey = current.memberId.ifBlank { current.id })
        appContext?.let { ctx ->
            saveCorporateSession(
                ctx,
                CorporateLoginSession(
                    userId = canonicalId,
                    email = updated.email,
                    fullName = updated.fullName,
                    isHeadActivated = false,
                    requestedSubdivision = group.name,
                    requestedSiteName = cleanSite,
                    requestedPosition = updated.position
                )
            )
        }
        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.registerOrResolveSiteMember(
                    rawMemberId = updated.memberId,
                    email = updated.email,
                    fullName = updated.fullName,
                    position = updated.position,
                    requestedGroup = group,
                    siteName = cleanSite,
                    forceHeadActivation = false,
                    forceUpdatePendingDetails = true
                )
            }
            _bannerMessage.value = "Данные заявки обновлены (подразделение: ${group.name}, участок: $cleanSite)"
        }
    }

    fun refreshMyRoleAssignmentStatus() {
        val current = _currentMemberRecord.value ?: return
        val currentNorm = SiteMemberDocument.canonicalMemberId(current.memberId.ifBlank { current.id }, current.email)

        // Reload local store in case role was assigned on this device
        appContext?.let { ctx ->
            val freshLocal = LocalWorkspaceStore.loadMembers(ctx).filterNot {
                CorporateSeedData.isLegacyFakeSeedMemberId(it.memberId.ifBlank { it.id })
            }
            if (freshLocal.isNotEmpty()) {
                _localSiteMembers.value = freshLocal
            }
        }

        viewModelScope.launch {
            val remoteFetched = if (isCloudAuthenticated) {
                val existing = repository.fetchSiteMemberByIdOrEmail(currentNorm, current.email)
                if (existing != null) {
                    existing
                } else {
                    val grp = CorporateSeedData.workGroups.firstOrNull {
                        it.name.equals(current.groupName, ignoreCase = true)
                    }?.copy(name = current.groupName)
                        ?: CorporateSeedData.workGroups.first().copy(id = current.groupId, name = current.groupName)
                    repository.registerOrResolveSiteMember(
                        rawMemberId = currentNorm,
                        email = current.email,
                        fullName = current.fullName,
                        position = current.position,
                        requestedGroup = grp,
                        siteName = current.effectiveSiteName.ifBlank { _siteName.value },
                        forceHeadActivation = false,
                        forceUpdatePendingDetails = true
                    ).getOrNull()
                }
            } else {
                null
            }
            val latestFromList = siteMembersState.value.firstOrNull {
                SiteMemberDocument.canonicalMemberId(it.memberId.ifBlank { it.id }, it.email) == currentNorm ||
                    (current.email.isNotBlank() && it.email.equals(current.email, ignoreCase = true))
            }
            val resolved = when {
                remoteFetched != null && !remoteFetched.isPendingRole -> remoteFetched.copy(id = currentNorm, memberId = currentNorm)
                latestFromList != null && !latestFromList.isPendingRole -> latestFromList
                remoteFetched != null -> remoteFetched.copy(id = currentNorm, memberId = currentNorm)
                else -> latestFromList
            }
            if (resolved != null) {
                _currentMemberRecord.value = resolved
                upsertLocalMember(resolved)
                if (!resolved.isPendingRole) {
                    _bannerMessage.value = "Назначена роль: ${resolved.parsedRoleOrNull?.titleRu}"
                } else {
                    _bannerMessage.value = if (isCloudAuthenticated) {
                        "Заявка отправлена в облако и ожидает подтверждения администратором"
                    } else {
                        "Подключите Google-синхронизацию для отправки заявки администратору"
                    }
                }
            } else {
                _bannerMessage.value = "Заявка ожидает подтверждения администратором"
            }
        }
    }

    fun refreshSiteMembersQueue(showBanner: Boolean = false) {
        appContext?.let { ctx ->
            val freshLocal = LocalWorkspaceStore.loadMembers(ctx).filterNot {
                CorporateSeedData.isLegacyFakeSeedMemberId(it.memberId.ifBlank { it.id })
            }
            _localSiteMembers.value = freshLocal
            _deletedMemberIds.value = LocalWorkspaceStore.loadDeletedMemberIds(ctx)
        }
        viewModelScope.launch {
            if (isCloudAuthenticated) {
                val remoteMembers = repository.fetchAllSiteMembersOnce()
                remoteMembers.forEach { remote ->
                    val canonicalId = SiteMemberDocument.canonicalMemberId(remote.memberId.ifBlank { remote.id }, remote.email)
                    val existsLocally = _localSiteMembers.value.any {
                        SiteMemberDocument.canonicalMemberId(it.memberId.ifBlank { it.id }, it.email) == canonicalId
                    }
                    if (!existsLocally || remote.isPendingRole) {
                        upsertLocalMember(remote.copy(id = canonicalId, memberId = canonicalId))
                    }
                }
            }
            if (showBanner) {
                val pendingCount = siteMembersState.value.count { it.isPendingRole && !it.isRejected }
                _bannerMessage.value = if (pendingCount > 0) {
                    "Список обновлён: ожидают назначения роли — $pendingCount чел."
                } else {
                    "Список персонала обновлён (новых заявок нет)"
                }
            }
        }
    }

    fun assignRoleToMember(
        targetMember: SiteMemberDocument,
        assignedRole: CorporateRole,
        assignedGroup: WorkGroup,
        customPosition: String? = null
    ) {
        val actor = activeEmployee.value
        val originalRawKey = targetMember.memberId.ifBlank { targetMember.id }
        val key = SiteMemberDocument.canonicalMemberId(originalRawKey, targetMember.email)
            .ifBlank { "usr_${System.currentTimeMillis()}" }
        val newPosition = customPosition?.takeIf { it.isNotBlank() }
            ?: targetMember.position.ifBlank { assignedRole.titleRu }

        val draftApproved = targetMember.copy(
            id = key,
            memberId = key,
            role = assignedRole.code,
            approvalStatus = MemberApprovalStatus.APPROVED.code,
            groupId = targetMember.groupId.ifBlank { assignedGroup.id },
            groupName = targetMember.groupName.ifBlank { assignedGroup.name },
            position = newPosition
        )
        val approvedMember = draftApproved.copy(
            approvedByName = draftApproved.encodeApprovedByForFirestore(
                approverName = actor.fullName,
                isAdminRole = assignedRole == CorporateRole.ADMIN
            )
        )

        // Мгновенно обновляем локальный список и сохраняем на устройство
        upsertLocalMember(approvedMember, previousKey = originalRawKey)
        val currentRecKey = _currentMemberRecord.value?.let {
            SiteMemberDocument.canonicalMemberId(it.memberId.ifBlank { it.id }, it.email)
        }
        if (currentRecKey == key || key == normalizedCurrentUserId) {
            _currentMemberRecord.value = approvedMember
        }
        appendLocalAudit(
            actor = actor,
            actionType = "ASSIGN_USER_ROLE",
            targetType = "USER",
            targetId = key,
            details = "Назначена роль «${assignedRole.titleRu}» пользователю ${approvedMember.fullName}"
        )
        _bannerMessage.value = "Пользователь ${approvedMember.fullName} утверждён (${assignedRole.titleRu})"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.assignRoleToSiteMember(
                    actor = actor,
                    targetMember = approvedMember,
                    assignedRole = assignedRole,
                    assignedGroup = assignedGroup,
                    customPosition = newPosition
                )
            }
        }
    }

    fun rejectMemberAccess(targetMember: SiteMemberDocument) {
        val actor = activeEmployee.value
        val key = targetMember.memberId.ifBlank { targetMember.id }
        deleteSiteMember(targetMember)
        appendLocalAudit(
            actor = actor,
            actionType = "REJECT_USER_ACCESS",
            targetType = "USER",
            targetId = key,
            details = "Отклонена заявка ${targetMember.fullName}"
        )
        _bannerMessage.value = "Заявка ${targetMember.fullName} отклонена"
    }

    fun deleteSiteMember(targetMember: SiteMemberDocument) {
        val rawKey = targetMember.memberId.ifBlank { targetMember.id }
        val normKey = SiteMemberDocument.canonicalMemberId(rawKey, targetMember.email)
        _deletedMemberIds.value = _deletedMemberIds.value + setOf(rawKey, normKey)
        appContext?.let {
            LocalWorkspaceStore.addDeletedMemberId(it, rawKey)
            LocalWorkspaceStore.addDeletedMemberId(it, normKey)
        }
        val remaining = _localSiteMembers.value.filterNot {
            val itRaw = it.memberId.ifBlank { it.id }
            val itNorm = SiteMemberDocument.canonicalMemberId(itRaw, it.email)
            itRaw == rawKey || itNorm == normKey
        }
        persistMembersLocally(remaining)
        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.deleteSiteMember(activeEmployee.value, normKey)
            }
            _bannerMessage.value = "Запись пользователя ${targetMember.fullName} удалена"
        }
    }

    fun preRegisterEmployeeByEmail(
        email: String,
        fullName: String,
        position: String,
        assignedRole: CorporateRole,
        assignedGroup: WorkGroup,
        personnelNumber: String = "",
        siteName: String = "",
        phone: String = ""
    ) {
        if (fullName.isBlank()) {
            _bannerMessage.value = "Укажите ФИО сотрудника"
            return
        }
        val actor = activeEmployee.value
        val normalizedEmail = email.trim().lowercase().ifBlank {
            "emp_${System.currentTimeMillis()}@site1.ru"
        }
        val fallbackId = "emp_${normalizedEmail.replace(Regex("[^a-zA-Z0-9]"), "_").take(40)}"
        val safeId = SiteMemberDocument.canonicalMemberId(fallbackId, normalizedEmail)
        val resolvedPosition = position.ifBlank { assignedRole.titleRu }
        val resolvedSite = siteName.trim().ifBlank { _siteName.value }

        val baseCreated = SiteMemberDocument(
            id = safeId,
            memberId = safeId,
            email = normalizedEmail,
            fullName = fullName.trim(),
            position = resolvedPosition,
            role = assignedRole.code,
            approvalStatus = MemberApprovalStatus.APPROVED.code,
            groupId = assignedGroup.id,
            groupName = assignedGroup.name,
            approvedByName = actor.fullName,
            personnelNumber = personnelNumber.trim(),
            siteName = resolvedSite,
            phone = phone.trim()
        )
        val created = baseCreated.copy(
            approvedByName = baseCreated.encodeApprovedByForFirestore(
                approverName = actor.fullName,
                explicitPersonnelNumber = personnelNumber.trim(),
                explicitSiteName = resolvedSite,
                explicitPhone = phone.trim(),
                isAdminRole = assignedRole == CorporateRole.ADMIN
            )
        )
        // Если сотрудник с таким safeId был ранее удалён, снимаем пометку удаления
        _deletedMemberIds.value = _deletedMemberIds.value - setOf(safeId, fallbackId)
        upsertLocalMember(created, previousKey = fallbackId)
        appendLocalAudit(
            actor = actor,
            actionType = "PRE_REGISTER_USER",
            targetType = "USER",
            targetId = created.memberId,
            details = "Добавлен сотрудник ${created.fullName} (таб. № ${personnelNumber.ifBlank { "—" }}, участок: $resolvedSite) — ${assignedRole.titleRu}"
        )
        _bannerMessage.value = "Сотрудник ${created.fullName} добавлен (${assignedRole.titleRu})"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.preRegisterSiteMemberByEmail(
                    actor = actor,
                    email = normalizedEmail,
                    fullName = fullName.trim(),
                    position = resolvedPosition,
                    assignedRole = assignedRole,
                    assignedGroup = assignedGroup,
                    personnelNumber = personnelNumber.trim(),
                    siteName = resolvedSite,
                    phone = phone.trim()
                )
            }
        }
    }

    fun updateEmployeeDetails(
        targetMember: SiteMemberDocument,
        fullName: String,
        position: String,
        personnelNumber: String,
        siteName: String,
        groupName: String,
        phone: String,
        email: String,
        role: CorporateRole
    ) {
        val cleanName = fullName.trim().ifBlank { targetMember.fullName.ifBlank { "Сотрудник участка" } }
        val actor = activeEmployee.value
        val originalRawKey = targetMember.memberId.ifBlank { targetMember.id }
        val prelimCleanEmail = email.trim().ifBlank { targetMember.email }
        val key = SiteMemberDocument.canonicalMemberId(originalRawKey, prelimCleanEmail)
            .ifBlank { "emp_${System.currentTimeMillis()}" }
        val resolvedPosition = position.trim().ifBlank { role.titleRu }
        val resolvedSite = siteName.trim().ifBlank { _siteName.value }
        val resolvedGroupName = groupName.trim().ifBlank {
            targetMember.groupName.ifBlank { "Основное подразделение" }
        }
        val matchedGroup = CorporateSeedData.workGroups.firstOrNull {
            it.name.equals(resolvedGroupName, ignoreCase = true) || it.id == targetMember.groupId
        }
        val resolvedGroupId = matchedGroup?.id ?: targetMember.groupId.ifBlank { "grp_monolith" }
        val cleanTab = personnelNumber.trim()
        val cleanPhone = phone.trim()
        val cleanEmail = prelimCleanEmail.ifBlank { "$key@site1.ru" }

        val draftUpdated = targetMember.copy(
            id = key,
            memberId = key,
            fullName = cleanName,
            position = resolvedPosition,
            role = role.code,
            approvalStatus = MemberApprovalStatus.APPROVED.code,
            groupId = resolvedGroupId,
            groupName = resolvedGroupName,
            email = cleanEmail,
            personnelNumber = cleanTab,
            siteName = resolvedSite,
            phone = cleanPhone
        )
        val encodedMeta = draftUpdated.encodeApprovedByForFirestore(
            approverName = actor.fullName,
            explicitPersonnelNumber = cleanTab,
            explicitSiteName = resolvedSite,
            explicitPhone = cleanPhone
        )
        val updatedMember = draftUpdated.copy(approvedByName = encodedMeta)

        // Мгновенно сохраняем локально и перезаписываем предыдущую запись
        upsertLocalMember(updatedMember, previousKey = originalRawKey)

        // Проверяем, является ли редактируемый сотрудник текущим пользователем
        val currentRecNorm = _currentMemberRecord.value?.let { normalizeMemberKey(it.memberId.ifBlank { it.id }) }
        val isEditingCurrentUser = key == normalizedCurrentUserId ||
            originalRawKey == currentUserId ||
            currentRecNorm == key ||
            (currentUserEmail.isNotBlank() && targetMember.email.equals(currentUserEmail, ignoreCase = true))

        if (isEditingCurrentUser) {
            _currentMemberRecord.value = updatedMember
            appContext?.let { ctx ->
                saveCorporateSession(
                    ctx,
                    CorporateLoginSession(
                        userId = currentUserId,
                        email = cleanEmail,
                        fullName = cleanName,
                        isHeadActivated = role.isAdminOrHead || initialHeadActivated,
                        requestedSubdivision = resolvedGroupName,
                        requestedSiteName = resolvedSite,
                        requestedPosition = resolvedPosition
                    )
                )
            }
        }

        // Обновляем ФИО и подразделение в текущих задачах этого сотрудника
        val updatedTasks = _localTasks.value.map { t ->
            if (t.assigneeId == key || t.assigneeId == originalRawKey || normalizeMemberKey(t.assigneeId) == key) {
                t.copy(
                    assigneeId = key,
                    assigneeName = cleanName,
                    groupName = resolvedGroupName
                )
            } else {
                t
            }
        }
        persistTasksLocally(updatedTasks)

        appendLocalAudit(
            actor = actor,
            actionType = "UPDATE_EMPLOYEE",
            targetType = "USER",
            targetId = key,
            details = "Изменены данные сотрудника: $cleanName, должн.: $resolvedPosition, таб. №: ${cleanTab.ifBlank { "—" }}, участок: $resolvedSite"
        )
        _bannerMessage.value = "Карточка сотрудника «$cleanName» обновлена"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.updateSiteMemberDetails(actor, updatedMember)
            }
        }
    }

    fun updateOrganizationAndSite(newOrgName: String, newSiteName: String) {
        updateAdminSettings(
            newOrgName = newOrgName,
            newSiteName = newSiteName,
            newSubdivisions = _subdivisionsList.value,
            newSites = _sitesList.value
        )
    }

    fun updateAdminSettings(
        newOrgName: String,
        newSiteName: String,
        newSubdivisions: List<String>,
        newSites: List<String>
    ) {
        val cleanOrg = newOrgName.trim().ifBlank { CorporateSeedData.DEFAULT_ORG_NAME }
        val cleanSite = newSiteName.trim().ifBlank { CorporateSeedData.DEFAULT_SITE_NAME }
        val cleanSubdivisions = newSubdivisions.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            .ifEmpty { LocalWorkspaceStore.DEFAULT_SUBDIVISIONS }
        val cleanSites = (newSites.map { it.trim() }.filter { it.isNotBlank() } + cleanSite).distinct()
            .ifEmpty { LocalWorkspaceStore.DEFAULT_SITES }

        _organizationName.value = cleanOrg
        _siteName.value = cleanSite
        _subdivisionsList.value = cleanSubdivisions
        _sitesList.value = cleanSites

        appContext?.let { ctx ->
            LocalWorkspaceStore.saveOrganizationAndSite(ctx, cleanOrg, cleanSite)
            LocalWorkspaceStore.saveSubdivisions(ctx, cleanSubdivisions)
            LocalWorkspaceStore.saveSites(ctx, cleanSites)
        }

        val newHeader = "$cleanOrg • $cleanSite"
        _localTemplates.value = _localTemplates.value.map { it.copy(orgHeader = newHeader) }

        _currentMemberRecord.value?.let { cur ->
            val updatedCur = cur.copy(
                siteName = cleanSite,
                approvedByName = cur.encodeApprovedByForFirestore(explicitSiteName = cleanSite)
            )
            _currentMemberRecord.value = updatedCur
            upsertLocalMember(updatedCur)
        }

        appendLocalAudit(
            actor = activeEmployee.value,
            actionType = "UPDATE_ORG_SETTINGS",
            targetType = "SITE",
            targetId = "site_1",
            details = "Обновлены настройки администратора: организация «$cleanOrg», участок «$cleanSite», подразделений: ${cleanSubdivisions.size}, участков: ${cleanSites.size}"
        )
        _bannerMessage.value = "Настройки сохранены: $cleanOrg • $cleanSite"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.updateOrganizationAndSiteProfile(cleanOrg, cleanSite)
            }
        }
    }

    fun selectFilter(tab: TaskFilterTab) {
        _selectedFilter.value = tab
    }

    fun selectCalendarDate(epochDay: Long) {
        _selectedCalendarEpochDay.value = epochDay
        _selectedFilter.value = TaskFilterTab.CALENDAR_DATE
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun closeReportPreview() {
        _activeReportPreview.value = null
    }

    fun toggleAutoSchedulePlan(enabled: Boolean) {
        _autoSchedulePlanEvening.value = enabled
        _bannerMessage.value = if (enabled) {
            "Автогенерация плана на завтра (19:00) включена"
        } else {
            "Автогенерация плана отключена"
        }
    }

    fun toggleAutoScheduleReport(enabled: Boolean) {
        _autoScheduleReportNight.value = enabled
        val dateStr = ReportDocumentGenerator.formatEpochDay(_currentEpochDay.value)
        _bannerMessage.value = if (enabled) {
            "Автогенерация отчета за текущий день ($dateStr) в 21:00 включена"
        } else {
            "Автогенерация отчета за текущий день отключена"
        }
    }

    fun switchPersona(personaId: String) {
        val target = availableEmployees.value.firstOrNull { it.id == personaId } ?: return
        _bannerMessage.value = "Профиль: ${target.fullName} (${target.role.titleRu})"
    }

    private fun resolveOrCreateEmployees(
        selectedIds: List<String>,
        customNamesRaw: String
    ): List<CorporateEmployee> {
        val actor = activeEmployee.value
        val result = LinkedHashMap<String, CorporateEmployee>()

        selectedIds.map { it.trim() }.filter { it.isNotBlank() }.forEach { targetId ->
            val found = availableEmployees.value.firstOrNull {
                it.id == targetId || normalizeMemberKey(it.id) == normalizeMemberKey(targetId)
            }
            if (found != null) {
                result[normalizeMemberKey(found.id)] = found
            }
        }

        val customNames = customNamesRaw.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        customNames.forEach { customFio ->
            val existingByName = availableEmployees.value.firstOrNull {
                it.fullName.equals(customFio, ignoreCase = true) ||
                    it.shortName.equals(customFio, ignoreCase = true)
            }
            if (existingByName != null) {
                result[normalizeMemberKey(existingByName.id)] = existingByName
            } else {
                val newEmpId = "emp_${UUID.randomUUID().toString().replace("-", "").take(10)}"
                val newMember = SiteMemberDocument(
                    id = newEmpId,
                    memberId = newEmpId,
                    email = "$newEmpId@site1.ru",
                    fullName = customFio,
                    position = "Исполнитель",
                    role = CorporateRole.WORKER.code,
                    approvalStatus = MemberApprovalStatus.APPROVED.code,
                    groupId = CorporateSeedData.workGroups.first().id,
                    groupName = CorporateSeedData.workGroups.first().name,
                    approvedByName = actor.fullName,
                    siteName = _siteName.value
                )
                upsertLocalMember(newMember)
                val createdEmp = newMember.toCorporateEmployee(defaultSiteName = _siteName.value)
                result[normalizeMemberKey(createdEmp.id)] = createdEmp
            }
        }

        return result.values.toList()
    }

    fun createNewTask(draft: TaskDraft) {
        val actor = activeEmployee.value
        val candidateIds = (draft.assigneeIds + listOfNotNull(draft.assigneeId.takeIf { it.isNotBlank() })).distinct()
        val resolvedAssignees = resolveOrCreateEmployees(
            selectedIds = candidateIds,
            customNamesRaw = draft.customAssigneeName
        ).ifEmpty { listOf(actor) }

        val assignee = resolvedAssignees.first()
        val allAssigneeIds = resolvedAssignees.map { it.id }
        val allAssigneeFullNames = resolvedAssignees.map { it.fullName }
        val allAssigneeShortNames = resolvedAssignees.joinToString(", ") { it.shortName }

        val cleanTitle = draft.title.trim().ifBlank {
            draft.description.lines().firstOrNull { it.isNotBlank() }?.trim()
                ?: "Задание — $allAssigneeShortNames (${ReportDocumentGenerator.formatEpochDay(draft.dueDateEpochDay)})"
        }

        val taskId = "task_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val safeTargetVol = draft.targetVolume.coerceAtLeast(0.0)
        val safeUnit = draft.volumeUnit.trim().ifBlank { "шт" }

        val rawSubtaskLines = draft.subtasks
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // Не создаём фиктивный пункт из наименования задачи (sub_vol_main), в чек-лист входят только реальные пункты
        val decodedInitialSubtasks: List<SubtaskItem> = if (rawSubtaskLines.isEmpty()) {
            emptyList()
        } else {
            val perItemDefaultVol = if (safeTargetVol > 0.0 && rawSubtaskLines.size == 1) {
                safeTargetVol
            } else {
                1.0
            }
            rawSubtaskLines.mapIndexed { idx, s ->
                SubtaskItem.parseFromRawLine(
                    rawLine = s,
                    index = idx + 1,
                    defaultTargetVolume = if (idx == 0 && safeTargetVol > 0.0 && !s.contains("|")) safeTargetVol else perItemDefaultVol,
                    defaultUnit = safeUnit
                )
            }.filterNot { it.id == "sub_vol_main" }
        }

        val encodedSubtasks = decodedInitialSubtasks.map { it.encode() }
        val sumSubTargetVol = decodedInitialSubtasks.sumOf { it.effectiveTargetVolume }
        val resolvedTaskTargetVol = when {
            sumSubTargetVol > 0.0 -> sumSubTargetVol
            safeTargetVol > 0.0 -> safeTargetVol
            else -> 0.0
        }
        val resolvedTaskUnit = if (draft.volumeUnit.isNotBlank()) {
            draft.volumeUnit.trim()
        } else {
            decodedInitialSubtasks.firstOrNull()?.effectiveUnit ?: "шт"
        }

        val baseTask = TaskDocument(
            id = taskId,
            userId = effectiveUserId,
            title = cleanTitle,
            description = draft.description.trim(),
            creatorId = actor.id,
            creatorName = actor.fullName,
            assigneeId = assignee.id,
            assigneeName = allAssigneeFullNames.joinToString(", ").take(120),
            groupId = assignee.groupId,
            groupName = assignee.groupName,
            siteId = assignee.siteId,
            priority = draft.priority.code,
            status = TaskStatus.NEW.code,
            dueDateEpochDay = draft.dueDateEpochDay,
            originalDueDateEpochDay = draft.dueDateEpochDay,
            deadlineTime = draft.deadlineTime.ifBlank { "18:00" },
            carryoverCount = 0L,
            requiresPhotoReport = draft.requiresPhotoReport,
            minPhotoCount = if (draft.requiresPhotoReport) draft.minPhotoCount.coerceAtLeast(1).toLong() else 0L,
            requiresApproval = draft.requiresApproval,
            recurrenceRule = draft.recurrenceRule.code,
            targetVolume = resolvedTaskTargetVol,
            completedVolume = 0.0,
            volumeUnit = resolvedTaskUnit,
            tags = draft.tags,
            subtaskItems = encodedSubtasks,
            photoItems = emptyList(),
            commentItems = emptyList(),
            historyItems = listOf(
                StatusHistoryEntry(
                    nowText(),
                    actor.shortName,
                    TaskStatus.NEW.code,
                    "Задача создана (исп.: $allAssigneeShortNames, пунктов: ${decodedInitialSubtasks.size}, план: ${SubtaskItem.formatVolumeNumber(resolvedTaskTargetVol)} $resolvedTaskUnit)"
                ).encode()
            ),
            geoLocation = draft.geoLocation.ifBlank { _siteName.value }
        )
            .withUpdatedVolume(resolvedTaskTargetVol, 0.0, resolvedTaskUnit)
            .withUpdatedAssignees(allAssigneeIds, allAssigneeFullNames)

        // Мгновенно сохраняем задачу локально
        upsertLocalTask(baseTask)
        _searchQuery.value = ""
        _selectedCalendarEpochDay.value = draft.dueDateEpochDay

        // Автоматически переключаем вкладку фильтра так, чтобы созданная задача сразу была видна в списке
        when {
            _selectedFilter.value == TaskFilterTab.CALENDAR_DATE -> {
                _selectedCalendarEpochDay.value = draft.dueDateEpochDay
            }
            draft.dueDateEpochDay == _currentEpochDay.value && _selectedFilter.value == TaskFilterTab.TODAY -> {
                // остаёмся на «Сегодня»
            }
            draft.dueDateEpochDay == _currentEpochDay.value + 1L && _selectedFilter.value == TaskFilterTab.TOMORROW -> {
                // остаёмся на «Завтра»
            }
            draft.dueDateEpochDay == _currentEpochDay.value -> {
                if (_selectedFilter.value != TaskFilterTab.ALL) {
                    _selectedFilter.value = TaskFilterTab.TODAY
                }
            }
            draft.dueDateEpochDay == _currentEpochDay.value + 1L -> {
                if (_selectedFilter.value != TaskFilterTab.ALL) {
                    _selectedFilter.value = TaskFilterTab.TOMORROW
                }
            }
            else -> {
                _selectedFilter.value = TaskFilterTab.CALENDAR_DATE
            }
        }

        _bannerMessage.value = "Задача «$cleanTitle» назначена (${ReportDocumentGenerator.formatEpochDay(draft.dueDateEpochDay)}, исп.: $allAssigneeShortNames)"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.createTask(
                    actor = actor,
                    draft = draft.copy(
                        title = cleanTitle,
                        assigneeId = assignee.id,
                        assigneeIds = allAssigneeIds,
                        targetVolume = resolvedTaskTargetVol,
                        volumeUnit = resolvedTaskUnit,
                        subtasks = encodedSubtasks,
                        tags = baseTask.tags
                    ),
                    resolvedAssignee = assignee,
                    customTaskId = taskId,
                    resolvedAssignees = resolvedAssignees
                )
            }
        }
    }

    fun updateTaskAssignees(
        task: TaskDocument,
        assigneeIds: List<String>,
        customAssigneeName: String = ""
    ) {
        val currentTask = findTaskById(task.id) ?: task
        val actor = activeEmployee.value
        val resolvedAssignees = resolveOrCreateEmployees(
            selectedIds = assigneeIds,
            customNamesRaw = customAssigneeName
        )
        if (resolvedAssignees.isEmpty()) return

        val primary = resolvedAssignees.first()
        val allIds = resolvedAssignees.map { it.id }
        val allFullNames = resolvedAssignees.map { it.fullName }
        val displayNames = allFullNames.joinToString(", ").take(120)

        val updatedTask = currentTask
            .copy(
                assigneeId = primary.id,
                assigneeName = displayNames,
                groupId = primary.groupId,
                groupName = primary.groupName,
                historyItems = currentTask.historyItems + StatusHistoryEntry(
                    nowText(),
                    actor.shortName,
                    currentTask.status,
                    "Назначены исполнители: $displayNames"
                ).encode()
            )
            .withUpdatedAssignees(allIds, allFullNames)

        upsertLocalTask(updatedTask)
        _bannerMessage.value = "Исполнители задачи: $displayNames"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.updateTaskAssigneesInFirestore(
                    task = updatedTask,
                    assigneeId = updatedTask.assigneeId,
                    assigneeName = updatedTask.assigneeName,
                    groupId = updatedTask.groupId,
                    groupName = updatedTask.groupName,
                    tags = updatedTask.tags,
                    historyItems = updatedTask.historyItems
                )
            }
        }
    }

    fun updateTaskAssignee(task: TaskDocument, assigneeId: String, customAssigneeName: String = "") {
        updateTaskAssignees(
            task = task,
            assigneeIds = listOfNotNull(assigneeId.takeIf { it.isNotBlank() }),
            customAssigneeName = customAssigneeName
        )
    }

    fun deleteTask(task: TaskDocument) {
        val taskId = task.id
        if (taskId.isBlank()) return
        _deletedTaskIds.value = _deletedTaskIds.value + taskId
        appContext?.let { LocalWorkspaceStore.addDeletedTaskId(it, taskId) }
        val remaining = _localTasks.value.filterNot { it.id == taskId }
        persistTasksLocally(remaining)
        _bannerMessage.value = "Задача «${task.title}» удалена"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.deleteTask(activeEmployee.value, task)
            }
        }
    }

    fun addPhotosToTask(
        task: TaskDocument,
        items: List<Pair<String, String>>,
        gpsCoords: String
    ) {
        if (items.isEmpty()) return
        val currentTask = findTaskById(task.id) ?: task
        val actor = activeEmployee.value
        val nowStr = nowText()
        val encodedNew = items.mapIndexed { idx, (uriString, comment) ->
            PhotoReportItem(
                id = "ph_${System.currentTimeMillis()}_${idx}_${(100..999).random()}",
                uriString = uriString,
                authorId = actor.id,
                authorName = actor.shortName,
                capturedAtText = nowStr,
                gpsCoords = gpsCoords.ifBlank { "Участок №1" },
                comment = comment.ifBlank { "Фотофиксация выполнения" }
            ).encode()
        }
        val updatedPhotos = (currentTask.photoItems + encodedNew).takeLast(30)
        val nextStatus = if (currentTask.taskStatus == TaskStatus.NEW) {
            TaskStatus.IN_PROGRESS.code
        } else {
            currentTask.status
        }
        val updatedTask = currentTask.copy(
            photoItems = updatedPhotos,
            status = nextStatus,
            historyItems = currentTask.historyItems + StatusHistoryEntry(
                nowStr,
                actor.shortName,
                nextStatus,
                "Добавлено фото в отчёт (+${encodedNew.size} шт., всего: ${updatedPhotos.size} шт.)"
            ).encode()
        )
        upsertLocalTask(updatedTask)
        _bannerMessage.value = "Фотоотчёт сохранён (всего фото: ${updatedPhotos.size} шт.)"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                var runningTask = currentTask
                items.forEach { (uriString, comment) ->
                    repository.addPhotoToTask(actor, runningTask, uriString, gpsCoords, comment)
                    val addedEncoded = PhotoReportItem(
                        id = "ph_${System.currentTimeMillis()}",
                        uriString = uriString,
                        authorId = actor.id,
                        authorName = actor.shortName,
                        capturedAtText = nowStr,
                        gpsCoords = gpsCoords.ifBlank { "Участок №1" },
                        comment = comment.ifBlank { "Фотофиксация выполнения" }
                    ).encode()
                    runningTask = runningTask.copy(photoItems = runningTask.photoItems + addedEncoded)
                }
            }
        }
    }

    fun addPhotoToTask(task: TaskDocument, uriString: String, gpsCoords: String, comment: String) {
        addPhotosToTask(task, listOf(uriString to comment), gpsCoords)
    }

    fun deletePhotoFromTask(task: TaskDocument, photoId: String) {
        val currentTask = findTaskById(task.id) ?: task
        val decoded = currentTask.decodedPhotos()
        val targetPhoto = decoded.firstOrNull { it.id == photoId }
        if (targetPhoto != null) {
            PhotoCaptureUtils.deletePhotoFileIfLocal(targetPhoto.uriString)
        }
        val remainingEncoded = decoded.filterNot { it.id == photoId }.map { it.encode() }
        val updatedTask = currentTask.copy(photoItems = remainingEncoded)
        upsertLocalTask(updatedTask)
        _bannerMessage.value = "Фото удалено"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.deletePhotoFromTask(activeEmployee.value, currentTask, photoId)
            }
        }
    }

    private fun syncTaskVolumeFromChecklist(
        currentTask: TaskDocument,
        updatedSubtasks: List<SubtaskItem>
    ): Triple<Double, Double, String> {
        if (updatedSubtasks.isEmpty()) {
            return Triple(
                currentTask.effectiveTargetVolume,
                currentTask.effectiveCompletedVolume,
                currentTask.effectiveVolumeUnit
            )
        }
        val distinctUnits = updatedSubtasks.map { it.effectiveUnit.lowercase() }.distinct()
        return if (distinctUnits.size == 1) {
            val totalTarget = updatedSubtasks.sumOf { it.effectiveTargetVolume }
            val totalCompleted = updatedSubtasks.sumOf { it.effectiveCompletedVolume }.coerceIn(0.0, totalTarget)
            val unit = updatedSubtasks.first().effectiveUnit
            Triple(totalTarget, totalCompleted, unit)
        } else {
            val tVol = currentTask.effectiveTargetVolume.takeIf { it > 0.0 }
                ?: updatedSubtasks.sumOf { it.effectiveTargetVolume }
            val avgRatio = updatedSubtasks.sumOf { it.progressPercent } / (updatedSubtasks.size * 100.0)
            val cVol = (tVol * avgRatio).coerceIn(0.0, tVol)
            val unit = currentTask.effectiveVolumeUnit.ifBlank { updatedSubtasks.first().effectiveUnit }
            Triple(tVol, cVol, unit)
        }
    }

    fun toggleSubtask(task: TaskDocument, subtaskId: String) {
        val currentTask = findTaskById(task.id) ?: task
        val decoded = currentTask.decodedSubtasks()
        val updatedDecoded = decoded.map { item ->
            if (item.id == subtaskId) {
                val nextDone = !item.isDone
                val nextCompletedVol = if (nextDone) item.effectiveTargetVolume else 0.0
                item.copy(
                    isDone = nextDone,
                    targetVolume = item.effectiveTargetVolume,
                    completedVolume = nextCompletedVol,
                    unit = item.effectiveUnit
                )
            } else {
                item
            }
        }
        val updatedEncoded = updatedDecoded.map { it.encode() }
        val (newTarget, newCompleted, newUnit) = syncTaskVolumeFromChecklist(currentTask, updatedDecoded)

        val nextStatus = if (currentTask.taskStatus == TaskStatus.NEW && updatedDecoded.any { it.progressPercent > 0 }) {
            TaskStatus.IN_PROGRESS.code
        } else {
            currentTask.status
        }

        val updatedTask = currentTask
            .copy(subtaskItems = updatedEncoded, status = nextStatus)
            .withUpdatedVolume(newTarget, newCompleted, newUnit)

        upsertLocalTask(updatedTask)

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.updateSubtasksList(updatedTask, updatedEncoded, updatedTask.tags)
            }
        }
    }

    fun updateSubtaskVolume(task: TaskDocument, subtaskId: String, newCompletedVol: Double) {
        val currentTask = findTaskById(task.id) ?: task
        val actor = activeEmployee.value
        val decoded = currentTask.decodedSubtasks()
        var changedItem: SubtaskItem? = null
        val updatedDecoded = decoded.map { item ->
            if (item.id == subtaskId) {
                val effTarget = item.effectiveTargetVolume
                val clamped = newCompletedVol.coerceIn(0.0, effTarget)
                val isDoneNow = clamped >= effTarget
                val updatedItem = item.copy(
                    targetVolume = effTarget,
                    completedVolume = clamped,
                    isDone = isDoneNow,
                    unit = item.effectiveUnit
                )
                changedItem = updatedItem
                updatedItem
            } else {
                item
            }
        }
        val updatedEncoded = updatedDecoded.map { it.encode() }
        val (newTarget, newCompleted, newUnit) = syncTaskVolumeFromChecklist(currentTask, updatedDecoded)

        val nextStatus = if (currentTask.taskStatus == TaskStatus.NEW && newCompletedVol > 0.0) {
            TaskStatus.IN_PROGRESS.code
        } else {
            currentTask.status
        }

        val itemLog = changedItem?.let {
            "Пункт «${it.title}»: выполнено ${SubtaskItem.formatVolumeNumber(it.effectiveCompletedVolume)} из ${SubtaskItem.formatVolumeNumber(it.effectiveTargetVolume)} ${it.effectiveUnit} (${it.progressPercent}%)"
        } ?: "Обновлён объём по чек-листу"

        val updatedTask = currentTask
            .copy(
                subtaskItems = updatedEncoded,
                status = nextStatus,
                historyItems = currentTask.historyItems + StatusHistoryEntry(
                    nowText(),
                    actor.shortName,
                    nextStatus,
                    itemLog
                ).encode()
            )
            .withUpdatedVolume(newTarget, newCompleted, newUnit)

        upsertLocalTask(updatedTask)
        _bannerMessage.value = itemLog

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.updateSubtasksList(updatedTask, updatedEncoded, updatedTask.tags)
            }
        }
    }

    fun updateSubtaskDetails(
        task: TaskDocument,
        subtaskId: String,
        newTargetVol: Double,
        newCompletedVol: Double,
        newUnit: String
    ) {
        val currentTask = findTaskById(task.id) ?: task
        val actor = activeEmployee.value
        val safeTarget = newTargetVol.takeIf { it > 0.0 } ?: 1.0
        val safeCompleted = newCompletedVol.coerceIn(0.0, safeTarget)
        val safeUnit = newUnit.trim().ifBlank { "шт" }
        var changedItem: SubtaskItem? = null

        val updatedDecoded = currentTask.decodedSubtasks().map { item ->
            if (item.id == subtaskId) {
                val updatedItem = item.copy(
                    targetVolume = safeTarget,
                    completedVolume = safeCompleted,
                    isDone = safeCompleted >= safeTarget,
                    unit = safeUnit
                )
                changedItem = updatedItem
                updatedItem
            } else {
                item
            }
        }
        val updatedEncoded = updatedDecoded.map { it.encode() }
        val (taskTarget, taskCompleted, taskUnit) = syncTaskVolumeFromChecklist(currentTask, updatedDecoded)

        val nextStatus = if (currentTask.taskStatus == TaskStatus.NEW && safeCompleted > 0.0) {
            TaskStatus.IN_PROGRESS.code
        } else {
            currentTask.status
        }

        val itemLog = changedItem?.let {
            "Пункт «${it.title}»: выполнено ${SubtaskItem.formatVolumeNumber(it.effectiveCompletedVolume)} из ${SubtaskItem.formatVolumeNumber(it.effectiveTargetVolume)} ${it.effectiveUnit} (${it.progressPercent}%)"
        } ?: "Обновлены параметры пункта чек-листа"

        val updatedTask = currentTask
            .copy(
                subtaskItems = updatedEncoded,
                status = nextStatus,
                historyItems = currentTask.historyItems + StatusHistoryEntry(
                    nowText(),
                    actor.shortName,
                    nextStatus,
                    itemLog
                ).encode()
            )
            .withUpdatedVolume(taskTarget, taskCompleted, taskUnit)

        upsertLocalTask(updatedTask)
        _bannerMessage.value = itemLog

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.updateSubtasksList(updatedTask, updatedEncoded, updatedTask.tags)
            }
        }
    }

    fun updateTaskVolumeProgress(task: TaskDocument, newCompletedVol: Double) {
        val currentTask = findTaskById(task.id) ?: task
        val actor = activeEmployee.value
        val tVol = currentTask.effectiveTargetVolume
        if (tVol <= 0.0) return

        val clamped = newCompletedVol.coerceIn(0.0, tVol)
        val unit = currentTask.effectiveVolumeUnit.ifBlank { "шт" }
        val ratio = (clamped / tVol).coerceIn(0.0, 1.0)
        val pct = (ratio * 100.0).toInt().coerceIn(0, 100)

        // Пропорционально синхронизируем выполненный объём по пунктам чек-листа
        val decoded = currentTask.decodedSubtasks()
        val updatedDecoded = decoded.map { sub ->
            val subTarget = sub.effectiveTargetVolume
            val subCompleted = if (decoded.size == 1) {
                clamped.coerceIn(0.0, subTarget)
            } else {
                (subTarget * ratio).coerceIn(0.0, subTarget)
            }
            sub.copy(
                targetVolume = subTarget,
                completedVolume = subCompleted,
                isDone = subCompleted >= subTarget,
                unit = sub.effectiveUnit
            )
        }
        val updatedEncoded = updatedDecoded.map { it.encode() }

        val nextStatus = if (currentTask.taskStatus == TaskStatus.NEW && clamped > 0.0) {
            TaskStatus.IN_PROGRESS.code
        } else {
            currentTask.status
        }

        val updatedTask = currentTask
            .copy(
                subtaskItems = updatedEncoded,
                status = nextStatus,
                historyItems = currentTask.historyItems + StatusHistoryEntry(
                    nowText(),
                    actor.shortName,
                    nextStatus,
                    "Обновлён объём работ: ${SubtaskItem.formatVolumeNumber(clamped)} из ${SubtaskItem.formatVolumeNumber(tVol)} $unit ($pct%)"
                ).encode()
            )
            .withUpdatedVolume(tVol, clamped, unit)

        upsertLocalTask(updatedTask)
        _bannerMessage.value = "Выполнено: ${SubtaskItem.formatVolumeNumber(clamped)} из ${SubtaskItem.formatVolumeNumber(tVol)} $unit ($pct%)"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.updateSubtasksList(updatedTask, updatedEncoded, updatedTask.tags)
            }
        }
    }

    fun addSubtaskToTask(
        task: TaskDocument,
        title: String,
        targetVolume: Double = 1.0,
        unit: String = "шт"
    ) {
        val clean = title.trim()
        if (clean.isBlank()) return
        val currentTask = findTaskById(task.id) ?: task
        val resolvedTarget = targetVolume.takeIf { it > 0.0 } ?: 1.0
        val resolvedUnit = unit.trim().ifBlank { currentTask.effectiveVolumeUnit.ifBlank { "шт" } }
        val newItem = SubtaskItem(
            id = "sub_${System.currentTimeMillis()}",
            title = clean,
            isDone = false,
            targetVolume = resolvedTarget,
            completedVolume = 0.0,
            unit = resolvedUnit
        )
        val updatedDecoded = currentTask.decodedSubtasks() + newItem
        val updatedEncoded = updatedDecoded.map { it.encode() }
        val (taskTarget, taskCompleted, taskUnit) = syncTaskVolumeFromChecklist(currentTask, updatedDecoded)

        val updatedTask = currentTask
            .copy(subtaskItems = updatedEncoded)
            .withUpdatedVolume(taskTarget, taskCompleted, taskUnit)

        upsertLocalTask(updatedTask)
        _bannerMessage.value = "Добавлен пункт чек-листа «$clean» (план: ${SubtaskItem.formatVolumeNumber(resolvedTarget)} $resolvedUnit)"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.updateSubtasksList(updatedTask, updatedEncoded, updatedTask.tags)
            }
        }
    }

    fun deleteSubtaskFromTask(task: TaskDocument, subtaskId: String) {
        val currentTask = findTaskById(task.id) ?: task
        val updatedDecoded = currentTask.decodedSubtasks().filterNot { it.id == subtaskId }
        val updatedEncoded = updatedDecoded.map { it.encode() }
        val (taskTarget, taskCompleted, taskUnit) = if (updatedDecoded.isNotEmpty()) {
            syncTaskVolumeFromChecklist(currentTask, updatedDecoded)
        } else {
            Triple(0.0, 0.0, currentTask.effectiveVolumeUnit)
        }

        val updatedTask = currentTask
            .copy(subtaskItems = updatedEncoded)
            .withUpdatedVolume(taskTarget, taskCompleted, taskUnit)

        upsertLocalTask(updatedTask)
        _bannerMessage.value = "Пункт чек-листа удалён"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.updateSubtasksList(updatedTask, updatedEncoded, updatedTask.tags)
            }
        }
    }

    fun addComment(task: TaskDocument, text: String, mentionedUser: String) {
        if (text.isBlank()) return
        val currentTask = findTaskById(task.id) ?: task
        val actor = activeEmployee.value
        val newComment = TaskCommentItem(
            id = "cmt_${System.currentTimeMillis()}",
            authorId = actor.id,
            authorName = actor.shortName,
            authorRole = actor.role.code,
            text = text.trim(),
            mentionedUser = mentionedUser.trim(),
            createdAtText = nowText(),
            isRead = false
        ).encode()
        val updatedTask = currentTask.copy(
            commentItems = currentTask.commentItems + newComment
        )
        upsertLocalTask(updatedTask)

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.addComment(actor, currentTask, text, mentionedUser)
            }
        }
    }

    fun submitWorkerReportWithVolume(
        task: TaskDocument,
        manualCompletedVolume: Double?,
        note: String
    ) {
        var currentTask = findTaskById(task.id) ?: task
        val actor = activeEmployee.value

        val targetVol = currentTask.effectiveTargetVolume
        val unitLabel = currentTask.effectiveVolumeUnit.ifBlank { "шт" }
        val decoded = currentTask.decodedSubtasks()

        if (manualCompletedVolume != null && targetVol > 0.0) {
            val clamped = manualCompletedVolume.coerceIn(0.0, targetVol)
            val ratio = (clamped / targetVol).coerceIn(0.0, 1.0)
            val updatedDecoded = decoded.map { sub ->
                val subTarget = sub.effectiveTargetVolume
                val subCompleted = if (decoded.size == 1) {
                    clamped.coerceIn(0.0, subTarget)
                } else {
                    (subTarget * ratio).coerceIn(0.0, subTarget)
                }
                sub.copy(
                    targetVolume = subTarget,
                    completedVolume = subCompleted,
                    isDone = subCompleted >= subTarget,
                    unit = sub.effectiveUnit
                )
            }
            val updatedEncoded = updatedDecoded.map { it.encode() }
            currentTask = currentTask
                .copy(subtaskItems = updatedEncoded)
                .withUpdatedVolume(targetVol, clamped, unitLabel)
        } else if (manualCompletedVolume == null && currentTask.completionPercent() == 0 && (decoded.isNotEmpty() || targetVol > 0.0)) {
            // Если работник нажимает «Подтвердить выполнение» без ручного ввода, отмечаем 100% выполнения
            val updatedDecoded = decoded.map { sub ->
                val subTarget = sub.effectiveTargetVolume
                sub.copy(
                    targetVolume = subTarget,
                    completedVolume = subTarget,
                    isDone = true,
                    unit = sub.effectiveUnit
                )
            }
            val updatedEncoded = updatedDecoded.map { it.encode() }
            currentTask = currentTask
                .copy(subtaskItems = updatedEncoded)
                .let { if (targetVol > 0.0) it.withUpdatedVolume(targetVol, targetVol, unitLabel) else it }
        }

        val finalCompleted = currentTask.effectiveCompletedVolume
        val finalTarget = currentTask.effectiveTargetVolume
        val pct = currentTask.completionPercent()

        val nextStatus = if (currentTask.requiresApproval) {
            TaskStatus.ON_REVIEW
        } else {
            TaskStatus.APPROVED
        }

        val reportLogNote = buildString {
            if (finalTarget > 0.0) {
                append("Выполнено и передано на проверку: ${SubtaskItem.formatVolumeNumber(finalCompleted)} из ${SubtaskItem.formatVolumeNumber(finalTarget)} $unitLabel ($pct%)")
            } else {
                append("Выполнено и передано на проверку ($pct%)")
            }
            if (note.isNotBlank()) {
                append(" • ${note.trim()}")
            }
        }

        val updatedTask = currentTask.copy(
            status = nextStatus.code,
            rejectionReason = null,
            approvedByName = if (nextStatus == TaskStatus.APPROVED) actor.fullName else currentTask.approvedByName,
            historyItems = currentTask.historyItems + StatusHistoryEntry(
                nowText(),
                actor.shortName,
                nextStatus.code,
                reportLogNote
            ).encode()
        )
        upsertLocalTask(updatedTask)
        _bannerMessage.value = reportLogNote

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.updateSubtasksList(updatedTask, updatedTask.subtaskItems, updatedTask.tags)
                repository.submitTaskCompletion(actor, updatedTask, reportLogNote)
            }
        }
    }

    fun submitCompletion(task: TaskDocument, note: String) {
        submitWorkerReportWithVolume(task = task, manualCompletedVolume = null, note = note)
    }

    /**
     * Быстрая отметка «Выполнено» прямо с карточки задачи на главном экране РАБОЧЕГО без проваливания внутрь.
     */
    fun quickCompleteTaskFromCard(task: TaskDocument) {
        val currentTask = findTaskById(task.id) ?: task
        val actor = activeEmployee.value
        val targetVol = currentTask.effectiveTargetVolume
        val unitLabel = currentTask.effectiveVolumeUnit.ifBlank { "шт" }
        val decoded = currentTask.decodedSubtasks()
        val updatedDecoded = decoded.map { sub ->
            val subTarget = sub.effectiveTargetVolume
            sub.copy(
                targetVolume = subTarget,
                completedVolume = subTarget,
                isDone = true,
                unit = sub.effectiveUnit
            )
        }
        val updatedEncoded = updatedDecoded.map { it.encode() }
        val nextStatus = if (currentTask.requiresApproval) TaskStatus.ON_REVIEW else TaskStatus.APPROVED
        val logText = if (targetVol > 0.0) {
            "Отмечено «Выполнено» с карточки: ${SubtaskItem.formatVolumeNumber(targetVol)} / ${SubtaskItem.formatVolumeNumber(targetVol)} $unitLabel"
        } else {
            "Отмечено «Выполнено» с карточки"
        }
        val updatedTask = currentTask
            .copy(
                subtaskItems = updatedEncoded,
                status = nextStatus.code,
                rejectionReason = null,
                historyItems = currentTask.historyItems + StatusHistoryEntry(
                    nowText(),
                    actor.shortName,
                    nextStatus.code,
                    logText
                ).encode()
            )
            .let {
                if (targetVol > 0.0) it.withUpdatedVolume(targetVol, targetVol, unitLabel) else it
            }

        upsertLocalTask(updatedTask)
        _bannerMessage.value = "Задача «${currentTask.title}» отмечена выполненной (${nextStatus.titleRu})"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.updateSubtasksList(updatedTask, updatedTask.subtaskItems, updatedTask.tags)
                repository.submitTaskCompletion(actor, updatedTask, logText)
            }
        }
    }

    fun approveReport(task: TaskDocument, comment: String) {
        val currentTask = findTaskById(task.id) ?: task
        val supervisor = activeEmployee.value
        val updatedTask = currentTask.copy(
            status = TaskStatus.APPROVED.code,
            approvedByName = supervisor.fullName,
            rejectionReason = null,
            historyItems = currentTask.historyItems + StatusHistoryEntry(
                nowText(),
                supervisor.shortName,
                TaskStatus.APPROVED.code,
                comment.ifBlank { "Отчёт подтверждён" }
            ).encode()
        )
        upsertLocalTask(updatedTask)
        _bannerMessage.value = "Выполнение задачи подтверждено"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.approveTaskReport(supervisor, currentTask, comment)
            }
        }
    }

    fun rejectReport(task: TaskDocument, reason: String) {
        if (reason.isBlank()) {
            _bannerMessage.value = "Укажите причину возврата задачи"
            return
        }
        val currentTask = findTaskById(task.id) ?: task
        val supervisor = activeEmployee.value
        val updatedTask = currentTask.copy(
            status = TaskStatus.REJECTED.code,
            rejectionReason = reason.trim(),
            historyItems = currentTask.historyItems + StatusHistoryEntry(
                nowText(),
                supervisor.shortName,
                TaskStatus.REJECTED.code,
                "Отклонено: ${reason.trim()}"
            ).encode()
        )
        upsertLocalTask(updatedTask)
        _bannerMessage.value = "Отчёт отклонён и возвращён на доработку"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.rejectTaskReport(supervisor, currentTask, reason)
            }
        }
    }

    fun archiveTask(task: TaskDocument) {
        val currentTask = findTaskById(task.id) ?: task
        val actor = activeEmployee.value
        val updatedTask = currentTask.copy(status = TaskStatus.ARCHIVED.code)
        upsertLocalTask(updatedTask)
        _bannerMessage.value = "Задача перемещена в архив"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.archiveTask(actor, currentTask)
            }
        }
    }

    fun triggerEndOfDayCarryover() {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val allTasks = (tasksState.value as? UiState.Success)?.data ?: emptyList()
            val day = _currentEpochDay.value
            val nextDay = day + 1L
            var movedCount = 0
            val updatedList = allTasks.map { t ->
                if (t.dueDateEpochDay <= day && t.taskStatus.isEligibleForCarryover) {
                    movedCount++
                    t.copy(
                        dueDateEpochDay = nextDay,
                        carryoverCount = t.carryoverCount + 1L,
                        historyItems = t.historyItems + StatusHistoryEntry(
                            nowText(),
                            "Автоперенос",
                            t.status,
                            "Перенесено на следующий день (выполнено ${t.completionPercent()}%, перенос №${t.carryoverCount + 1L})"
                        ).encode()
                    )
                } else {
                    t
                }
            }
            persistTasksLocally(updatedList)
            if (isCloudAuthenticated) {
                repository.runEndOfDayCarryover(actor, allTasks, day)
            }
            _bannerMessage.value = if (movedCount > 0) {
                "Перенесено на завтра невыполненных задач: $movedCount"
            } else {
                "Нет активных невыполненных задач для переноса"
            }
        }
    }

    fun previewOrGenerateReport(
        context: Context,
        template: ReportTemplateDocument,
        sheetLayout: SheetLayout,
        exportFormat: ExportFormat,
        saveToHistory: Boolean,
        includePhotos: Boolean = true,
        targetEpochDay: Long? = null
    ) {
        viewModelScope.launch {
            val todayReal = CorporateSeedData.currentDeviceEpochDay()
            if (_currentEpochDay.value != todayReal) {
                _currentEpochDay.value = todayReal
            }
            val actor = activeEmployee.value
            val allTasks = (tasksState.value as? UiState.Success)?.data ?: emptyList()
            val day = targetEpochDay ?: _currentEpochDay.value
            val normalizedTemplate = template.copy(
                name = ReportDocumentGenerator.resolveTemplateDisplayName(template, day),
                orgHeader = "${_organizationName.value} • ${_siteName.value}"
            )
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    ReportDocumentGenerator.generateAndSaveDocument(
                        context = context,
                        actor = actor,
                        template = normalizedTemplate,
                        allTasks = allTasks,
                        currentEpochDay = day,
                        overrideLayout = sheetLayout,
                        overrideFormat = exportFormat,
                        includePhotos = includePhotos,
                        saveToDownloads = saveToHistory
                    )
                }
            }
            if (result.isSuccess) {
                val generated = result.getOrThrow()
                _activeReportPreview.value = generated
                if (saveToHistory) {
                    val rowCount = generated.sheets.sumOf { it.rows.size }.toLong()
                    val record = GeneratedReportDocument(
                        id = "rep_${System.currentTimeMillis()}",
                        userId = effectiveUserId,
                        templateId = template.id.ifBlank { "tpl_custom" },
                        templateName = "${normalizedTemplate.name} (${sheetLayout.shortRu}, ${if (includePhotos) "с фото" else "без фото"})",
                        reportType = template.reportType,
                        authorName = actor.fullName,
                        periodLabel = generated.periodLabel,
                        sheetLayout = sheetLayout.code,
                        exportFormat = exportFormat.code,
                        filePath = generated.file.absolutePath,
                        summaryText = generated.summaryText,
                        recipients = listOf("Руководитель"),
                        filterSummary = generated.roleScopeLabel,
                        rowCount = rowCount,
                        createdAt = Timestamp.now()
                    )
                    _localGeneratedReports.value = listOf(record) + _localGeneratedReports.value
                    if (isCloudAuthenticated) {
                        repository.recordGeneratedReport(actor, record)
                    }
                    _bannerMessage.value = "Скачано в папку «Загрузки» телефона: ${generated.file.name}"
                }
            } else {
                _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Ошибка формирования отчёта"
            }
        }
    }

    fun saveCustomTemplate(template: ReportTemplateDocument) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            _localTemplates.value = listOf(template) + _localTemplates.value.filterNot { it.id == template.id }
            if (isCloudAuthenticated) {
                repository.saveReportTemplate(actor, template)
            }
            _bannerMessage.value = "Шаблон «${template.name}» сохранён"
        }
    }

    private fun appendLocalAudit(
        actor: CorporateEmployee,
        actionType: String,
        targetType: String,
        targetId: String,
        details: String
    ) {
        val log = AuditLogDocument(
            id = "audit_${System.currentTimeMillis()}",
            userId = effectiveUserId,
            actorId = actor.id,
            actorName = actor.fullName,
            actorRole = actor.role.code,
            actionType = actionType,
            targetType = targetType,
            targetId = targetId,
            details = details,
            createdAt = Timestamp.now()
        )
        _localAuditLogs.value = listOf(log) + _localAuditLogs.value
    }

    private fun nowText(): String =
        SimpleDateFormat("dd.MM HH:mm", Locale.forLanguageTag("ru")).format(Date())

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
