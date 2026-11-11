package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.ui.components.PhotoCaptureUtils
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    private val appContext: Context? = null
) : ViewModel() {

    private val isCloudAuthenticated: Boolean
        get() = Firebase.auth.currentUser != null

    private val effectiveUserId: String
        get() = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: currentUserId

    private val _currentEpochDay = MutableStateFlow(CorporateSeedData.BASE_EPOCH_DAY)
    val currentEpochDay: StateFlow<Long> = _currentEpochDay.asStateFlow()

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

    // Чистое локальное состояние без фейковых задач и фейковых сотрудников
    private val _deletedTaskIds = MutableStateFlow(
        appContext?.let { LocalWorkspaceStore.loadDeletedTaskIds(it) } ?: emptySet()
    )
    private val _deletedMemberIds = MutableStateFlow(
        appContext?.let { LocalWorkspaceStore.loadDeletedMemberIds(it) } ?: emptySet()
    )
    private val _localTasks = MutableStateFlow(
        appContext?.let { ctx ->
            LocalWorkspaceStore.loadTasks(ctx).filterNot {
                CorporateSeedData.isLegacyFakeSeedTaskId(it.id)
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

    private val firestoreProfileFlow = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
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
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }

    // Локальные изменения участников имеют приоритет над устаревшим кешем Firestore
    val siteMembersState: StateFlow<List<SiteMemberDocument>> = combine(
        firestoreSiteMembersFlow,
        _localSiteMembers,
        _deletedMemberIds
    ) { remoteList, localList, deletedIds ->
        val mergedMap = LinkedHashMap<String, SiteMemberDocument>()
        remoteList.forEach { m ->
            val key = m.memberId.ifBlank { m.id }
            if (key.isNotBlank() &&
                !CorporateSeedData.isLegacyFakeSeedMemberId(key) &&
                key !in deletedIds
            ) {
                mergedMap[key] = m
            }
        }
        // Локальный список перезаписывает remoteList, чтобы утверждение роли работало мгновенно
        localList.forEach { m ->
            val key = m.memberId.ifBlank { m.id }
            if (key.isNotBlank() &&
                !CorporateSeedData.isLegacyFakeSeedMemberId(key) &&
                key !in deletedIds
            ) {
                mergedMap[key] = m
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
        val matched = allMembers.firstOrNull {
            it.memberId == currentUserId ||
                (currentUserEmail.isNotBlank() && it.email.equals(currentUserEmail, ignoreCase = true))
        } ?: memberRecord

        if (matched != null) {
            matched.toCorporateEmployee(defaultSiteName = currentSiteName)
        } else {
            val defaultName = currentUserName.ifBlank {
                currentUserEmail.substringBefore("@").ifBlank { "Руководитель" }
            }
            CorporateEmployee(
                id = currentUserId,
                fullName = defaultName,
                shortName = defaultName,
                position = "Руководитель",
                role = CorporateRole.HEAD,
                groupId = "grp_control",
                groupName = "Основное подразделение",
                siteId = CorporateSeedData.DEFAULT_SITE_ID,
                siteName = currentSiteName,
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
            fullName = currentUserName.ifBlank { "Руководитель" },
            shortName = currentUserName.ifBlank { "Руководитель" },
            position = "Руководитель",
            role = CorporateRole.HEAD,
            groupId = "grp_control",
            groupName = "Основное подразделение",
            siteId = CorporateSeedData.DEFAULT_SITE_ID,
            siteName = _siteName.value,
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
        map[currentEmp.id] = currentEmp
        approvedEmployees.forEach { emp ->
            map[emp.id] = emp
        }
        map.values.toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = listOf(activeEmployee.value)
    )

    private val firestoreTasksFlow = repository.observeTasks(currentUserId)
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
        // Локальные задачи и правки (чек-листы, фото, статусы) имеют приоритет
        localTasks.forEach { task ->
            if (task.id.isNotBlank() &&
                !CorporateSeedData.isLegacyFakeSeedTaskId(task.id) &&
                task.id !in deletedIds
            ) {
                mergedMap[task.id] = task
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

    init {
        initializeUserAndRoleStatus()
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
                currentUserEmail.substringBefore("@").ifBlank { "Руководитель" }
            }
            if (isCloudAuthenticated) {
                repository.ensureWorkspaceSeeded(cleanName)
            }

            val normalizedEmail = currentUserEmail.trim().lowercase()
            val isRootHead = initialHeadActivated ||
                SiteMemberDocument.isRootHeadEmail(normalizedEmail) ||
                normalizedEmail.isBlank()

            val existingLocal = _localSiteMembers.value.firstOrNull {
                it.memberId == currentUserId ||
                    (normalizedEmail.isNotBlank() && it.email.equals(normalizedEmail, ignoreCase = true))
            }

            val resolvedMember = if (isRootHead) {
                val headDoc = SiteMemberDocument(
                    id = currentUserId,
                    memberId = currentUserId,
                    email = normalizedEmail.ifBlank { "m241185m@gmail.com" },
                    fullName = cleanName,
                    position = "Руководитель",
                    role = CorporateRole.HEAD.code,
                    approvalStatus = MemberApprovalStatus.APPROVED.code,
                    groupId = "grp_control",
                    groupName = "Основное подразделение",
                    approvedByName = "Владелец"
                )
                upsertLocalMember(headDoc)
                if (isCloudAuthenticated) {
                    repository.registerOrResolveSiteMember(
                        rawMemberId = currentUserId,
                        email = headDoc.email,
                        fullName = headDoc.fullName,
                        position = headDoc.position,
                        requestedGroup = CorporateSeedData.workGroups.last(),
                        forceHeadActivation = true
                    )
                }
                headDoc
            } else if (existingLocal != null) {
                existingLocal
            } else {
                val created = repository.registerOrResolveSiteMember(
                    rawMemberId = currentUserId,
                    email = normalizedEmail,
                    fullName = cleanName,
                    position = "Специалист",
                    requestedGroup = CorporateSeedData.workGroups.first(),
                    forceHeadActivation = false
                ).getOrNull() ?: SiteMemberDocument(
                    id = currentUserId,
                    memberId = currentUserId,
                    email = normalizedEmail,
                    fullName = cleanName,
                    position = "Специалист",
                    role = "PENDING",
                    approvalStatus = MemberApprovalStatus.PENDING.code,
                    groupId = "grp_monolith",
                    groupName = CorporateSeedData.workGroups.first().name
                )
                upsertLocalMember(created)
                created
            }

            _currentMemberRecord.value = resolvedMember
        }
    }

    private fun upsertLocalMember(member: SiteMemberDocument) {
        val key = member.memberId.ifBlank { member.id }
        val updated = listOf(member.copy(id = key, memberId = key)) +
            _localSiteMembers.value.filterNot {
                val itKey = it.memberId.ifBlank { it.id }
                itKey == key || (member.email.isNotBlank() && it.email.equals(member.email, ignoreCase = true))
            }
        persistMembersLocally(updated)
    }

    private fun upsertLocalTask(task: TaskDocument) {
        val updated = listOf(task) + _localTasks.value.filterNot { it.id == task.id }
        persistTasksLocally(updated)
    }

    private fun findTaskById(taskId: String): TaskDocument? {
        val fromState = (tasksState.value as? UiState.Success)?.data?.firstOrNull { it.id == taskId }
        return fromState ?: _localTasks.value.firstOrNull { it.id == taskId }
    }

    fun activateHeadAccessWithCode(code: String): Boolean {
        if (code.trim() != SiteMemberDocument.DEFAULT_HEAD_ACTIVATION_CODE) {
            _bannerMessage.value = "Неверный код руководителя (используйте 1001)"
            return false
        }
        viewModelScope.launch {
            val email = currentUserEmail.ifBlank { "m241185m@gmail.com" }
            val name = currentUserName.ifBlank { "Руководитель" }
            val headMember = SiteMemberDocument(
                id = currentUserId,
                memberId = currentUserId,
                email = email,
                fullName = name,
                position = "Руководитель",
                role = CorporateRole.HEAD.code,
                approvalStatus = MemberApprovalStatus.APPROVED.code,
                groupId = "grp_control",
                groupName = "Основное подразделение",
                approvedByName = "Код руководителя (1001)"
            )
            _currentMemberRecord.value = headMember
            upsertLocalMember(headMember)
            if (isCloudAuthenticated) {
                repository.registerOrResolveSiteMember(
                    rawMemberId = currentUserId,
                    email = email,
                    fullName = name,
                    position = "Руководитель",
                    requestedGroup = CorporateSeedData.workGroups.last(),
                    forceHeadActivation = true
                )
            }
            _bannerMessage.value = "Статус Руководителя подтверждён"
        }
        return true
    }

    fun updateMyPendingRequestDetails(position: String, group: WorkGroup) {
        val current = _currentMemberRecord.value ?: return
        val updated = current.copy(
            position = position.ifBlank { "Специалист" },
            groupId = group.id,
            groupName = group.name
        )
        _currentMemberRecord.value = updated
        upsertLocalMember(updated)
        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.registerOrResolveSiteMember(
                    rawMemberId = updated.memberId,
                    email = updated.email,
                    fullName = updated.fullName,
                    position = updated.position,
                    requestedGroup = group,
                    forceHeadActivation = false
                )
            }
            _bannerMessage.value = "Данные заявки обновлены"
        }
    }

    fun refreshMyRoleAssignmentStatus() {
        val current = _currentMemberRecord.value ?: return
        val latestFromList = siteMembersState.value.firstOrNull {
            it.memberId == current.memberId ||
                (current.email.isNotBlank() && it.email.equals(current.email, ignoreCase = true))
        }
        if (latestFromList != null) {
            _currentMemberRecord.value = latestFromList
            if (!latestFromList.isPendingRole) {
                _bannerMessage.value = "Назначена роль: ${latestFromList.parsedRoleOrNull?.titleRu}"
            } else {
                _bannerMessage.value = "Заявка ожидает подтверждения руководителем"
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
        val key = targetMember.memberId.ifBlank { targetMember.id }
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .ifBlank { "usr_${System.currentTimeMillis()}" }
        val newPosition = customPosition?.takeIf { it.isNotBlank() }
            ?: targetMember.position.ifBlank { assignedRole.titleRu }

        val approvedMember = targetMember.copy(
            id = key,
            memberId = key,
            role = assignedRole.code,
            approvalStatus = MemberApprovalStatus.APPROVED.code,
            groupId = assignedGroup.id,
            groupName = assignedGroup.name,
            position = newPosition,
            approvedByName = actor.fullName
        )

        // Мгновенно обновляем локальный список и сохраняем на устройство
        upsertLocalMember(approvedMember)
        if (_currentMemberRecord.value?.memberId == key) {
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
        // Удаляем отклонённую заявку из очереди ожидания
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
        val key = targetMember.memberId.ifBlank { targetMember.id }
        _deletedMemberIds.value = _deletedMemberIds.value + key
        appContext?.let { LocalWorkspaceStore.addDeletedMemberId(it, key) }
        val remaining = _localSiteMembers.value.filterNot {
            val itKey = it.memberId.ifBlank { it.id }
            itKey == key
        }
        persistMembersLocally(remaining)
        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.deleteSiteMember(activeEmployee.value, key)
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
        val safeId = "emp_${normalizedEmail.replace(Regex("[^a-zA-Z0-9]"), "_").take(40)}"
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
            approvedByName = baseCreated.encodeApprovedByForFirestore(actor.fullName)
        )
        upsertLocalMember(created)
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
        val cleanName = fullName.trim()
        if (cleanName.isBlank()) {
            _bannerMessage.value = "ФИО сотрудника не может быть пустым"
            return
        }
        val actor = activeEmployee.value
        val key = targetMember.memberId.ifBlank { targetMember.id }
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .ifBlank { "emp_${System.currentTimeMillis()}" }
        val resolvedPosition = position.trim().ifBlank { role.titleRu }
        val resolvedSite = siteName.trim().ifBlank { _siteName.value }
        val resolvedGroupName = groupName.trim().ifBlank { targetMember.groupName.ifBlank { "Основное подразделение" } }
        val matchedGroup = CorporateSeedData.workGroups.firstOrNull {
            it.name.equals(resolvedGroupName, ignoreCase = true) || it.id == targetMember.groupId
        }
        val resolvedGroupId = matchedGroup?.id ?: targetMember.groupId.ifBlank { "grp_monolith" }

        val draftUpdated = targetMember.copy(
            id = key,
            memberId = key,
            fullName = cleanName,
            position = resolvedPosition,
            role = role.code,
            approvalStatus = MemberApprovalStatus.APPROVED.code,
            groupId = resolvedGroupId,
            groupName = resolvedGroupName,
            email = email.trim().ifBlank { targetMember.email.ifBlank { "$key@site1.ru" } },
            personnelNumber = personnelNumber.trim(),
            siteName = resolvedSite,
            phone = phone.trim()
        )
        val updatedMember = draftUpdated.copy(
            approvedByName = draftUpdated.encodeApprovedByForFirestore(actor.fullName)
        )

        upsertLocalMember(updatedMember)
        if (_currentMemberRecord.value?.memberId == key || _currentMemberRecord.value?.id == key) {
            _currentMemberRecord.value = updatedMember
        }

        // Обновляем ФИО и подразделение в текущих задачах этого сотрудника
        val updatedTasks = _localTasks.value.map { t ->
            if (t.assigneeId == key) {
                t.copy(
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
            details = "Изменены данные сотрудника: $cleanName, должн.: $resolvedPosition, таб. №: ${personnelNumber.ifBlank { "—" }}, участок: $resolvedSite"
        )
        _bannerMessage.value = "Данные сотрудника $cleanName сохранены"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.updateSiteMemberDetails(actor, updatedMember)
            }
        }
    }

    fun updateOrganizationAndSite(newOrgName: String, newSiteName: String) {
        val cleanOrg = newOrgName.trim().ifBlank { CorporateSeedData.DEFAULT_ORG_NAME }
        val cleanSite = newSiteName.trim().ifBlank { CorporateSeedData.DEFAULT_SITE_NAME }
        _organizationName.value = cleanOrg
        _siteName.value = cleanSite
        appContext?.let { LocalWorkspaceStore.saveOrganizationAndSite(it, cleanOrg, cleanSite) }

        // Обновляем заголовок организации в шаблонах отчётов
        val newHeader = "$cleanOrg • $cleanSite"
        _localTemplates.value = _localTemplates.value.map { it.copy(orgHeader = newHeader) }

        // Обновляем принадлежность к участку у текущего пользователя, если не задан индивидуальный участок
        _currentMemberRecord.value?.let { cur ->
            val updatedCur = cur.copy(siteName = cleanSite)
            _currentMemberRecord.value = updatedCur
            upsertLocalMember(updatedCur)
        }

        appendLocalAudit(
            actor = activeEmployee.value,
            actionType = "UPDATE_ORG_SETTINGS",
            targetType = "SITE",
            targetId = "site_1",
            details = "Обновлены реквизиты: организация «$cleanOrg», участок «$cleanSite»"
        )
        _bannerMessage.value = "Сохранено: $cleanOrg • $cleanSite"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.updateOrganizationAndSiteProfile(cleanOrg, cleanSite)
            }
        }
    }

    fun selectFilter(tab: TaskFilterTab) {
        _selectedFilter.value = tab
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

    fun createNewTask(draft: TaskDraft) {
        val cleanTitle = draft.title.trim()
        if (cleanTitle.isBlank()) {
            _bannerMessage.value = "Введите название задачи"
            return
        }

        val actor = activeEmployee.value
        val customFio = draft.customAssigneeName.trim()
        val assignee: CorporateEmployee = if (customFio.isNotBlank()) {
            val existingByName = availableEmployees.value.firstOrNull {
                it.fullName.equals(customFio, ignoreCase = true) ||
                    it.shortName.equals(customFio, ignoreCase = true)
            }
            if (existingByName != null) {
                existingByName
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
                    approvedByName = actor.fullName
                )
                upsertLocalMember(newMember)
                newMember.toCorporateEmployee()
            }
        } else {
            availableEmployees.value.firstOrNull { it.id == draft.assigneeId } ?: actor
        }

        val taskId = "task_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val safeTargetVol = draft.targetVolume.coerceAtLeast(0.0)
        val safeUnit = draft.volumeUnit.trim().ifBlank { if (safeTargetVol > 0.0) "шт" else "" }

        val rawSubtaskLines = draft.subtasks
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val encodedSubtasks = if (rawSubtaskLines.isEmpty() && safeTargetVol > 0.0) {
            listOf(
                SubtaskItem(
                    id = "sub_vol_main",
                    title = cleanTitle,
                    isDone = false,
                    targetVolume = safeTargetVol,
                    completedVolume = 0.0,
                    unit = safeUnit
                ).encode()
            )
        } else {
            rawSubtaskLines.mapIndexed { idx, s ->
                if (s.contains("|")) {
                    s
                } else if (idx == 0 && safeTargetVol > 0.0) {
                    SubtaskItem(
                        id = "sub_${System.currentTimeMillis()}_${idx + 1}",
                        title = s,
                        isDone = false,
                        targetVolume = safeTargetVol,
                        completedVolume = 0.0,
                        unit = safeUnit
                    ).encode()
                } else {
                    SubtaskItem(
                        id = "sub_${System.currentTimeMillis()}_${idx + 1}",
                        title = s,
                        isDone = false
                    ).encode()
                }
            }
        }

        val baseTask = TaskDocument(
            id = taskId,
            userId = effectiveUserId,
            title = cleanTitle,
            description = draft.description.trim(),
            creatorId = actor.id,
            creatorName = actor.fullName,
            assigneeId = assignee.id,
            assigneeName = assignee.fullName,
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
            targetVolume = safeTargetVol,
            completedVolume = 0.0,
            volumeUnit = safeUnit,
            tags = draft.tags,
            subtaskItems = encodedSubtasks,
            photoItems = emptyList(),
            commentItems = emptyList(),
            historyItems = listOf(
                StatusHistoryEntry(
                    nowText(),
                    actor.shortName,
                    TaskStatus.NEW.code,
                    if (safeTargetVol > 0.0) {
                        "Задача создана (плановый объём: ${SubtaskItem.formatVolumeNumber(safeTargetVol)} $safeUnit)"
                    } else {
                        "Задача создана"
                    }
                ).encode()
            ),
            geoLocation = draft.geoLocation.ifBlank { "Участок №1" }
        ).withUpdatedVolume(safeTargetVol, 0.0, safeUnit)

        upsertLocalTask(baseTask)
        _searchQuery.value = ""
        if (_selectedFilter.value != TaskFilterTab.ALL &&
            _selectedFilter.value != TaskFilterTab.TODAY &&
            _selectedFilter.value != TaskFilterTab.TOMORROW
        ) {
            _selectedFilter.value = TaskFilterTab.ALL
        } else if (draft.dueDateEpochDay > _currentEpochDay.value && _selectedFilter.value == TaskFilterTab.TODAY) {
            _selectedFilter.value = TaskFilterTab.TOMORROW
        } else if (draft.dueDateEpochDay <= _currentEpochDay.value && _selectedFilter.value == TaskFilterTab.TOMORROW) {
            _selectedFilter.value = TaskFilterTab.TODAY
        }

        _bannerMessage.value = "Задача «$cleanTitle» назначена: ${assignee.shortName}"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.createTask(
                    actor = actor,
                    draft = draft.copy(
                        assigneeId = assignee.id,
                        subtasks = encodedSubtasks,
                        tags = baseTask.tags
                    ),
                    resolvedAssignee = assignee,
                    customTaskId = taskId
                )
            }
        }
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

    fun addPhotoToTask(task: TaskDocument, uriString: String, gpsCoords: String, comment: String) {
        val currentTask = findTaskById(task.id) ?: task
        val actor = activeEmployee.value
        val newPhoto = PhotoReportItem(
            id = "ph_${System.currentTimeMillis()}_${(100..999).random()}",
            uriString = uriString,
            authorId = actor.id,
            authorName = actor.shortName,
            capturedAtText = nowText(),
            gpsCoords = gpsCoords.ifBlank { "Участок №1" },
            comment = comment.ifBlank { "Фотофиксация выполнения" }
        ).encode()

        val updatedPhotos = currentTask.photoItems + newPhoto
        val nextStatus = if (currentTask.taskStatus == TaskStatus.NEW) {
            TaskStatus.IN_PROGRESS.code
        } else {
            currentTask.status
        }
        val updatedTask = currentTask.copy(
            photoItems = updatedPhotos,
            status = nextStatus,
            historyItems = currentTask.historyItems + StatusHistoryEntry(
                nowText(),
                actor.shortName,
                nextStatus,
                "Добавлено фото (${updatedPhotos.size} шт.)"
            ).encode()
        )
        upsertLocalTask(updatedTask)
        _bannerMessage.value = "Фото добавлено (${updatedPhotos.size} шт.)"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.addPhotoToTask(actor, currentTask, uriString, gpsCoords, comment)
            }
        }
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

    fun toggleSubtask(task: TaskDocument, subtaskId: String) {
        val currentTask = findTaskById(task.id) ?: task
        val decoded = currentTask.decodedSubtasks()
        val updatedDecoded = decoded.map { item ->
            if (item.id == subtaskId) {
                val nextDone = !item.isDone
                val nextCompletedVol = if (item.hasVolume) {
                    if (nextDone) item.targetVolume else 0.0
                } else {
                    item.completedVolume
                }
                item.copy(isDone = nextDone, completedVolume = nextCompletedVol)
            } else {
                item
            }
        }
        val updatedEncoded = updatedDecoded.map { it.encode() }

        // Синхронизируем общий объём задачи с чек-листом, если объём ведётся по пунктам
        val tVol = currentTask.effectiveTargetVolume
        val nextTaskCompletedVol = if (tVol > 0.0 && updatedDecoded.isNotEmpty()) {
            val volSubs = updatedDecoded.filter { it.hasVolume }
            if (volSubs.size == 1 && volSubs.first().targetVolume == tVol) {
                volSubs.first().completedVolume
            } else {
                val avgRatio = updatedDecoded.sumOf { it.progressPercent } / (updatedDecoded.size * 100.0)
                (tVol * avgRatio).coerceIn(0.0, tVol)
            }
        } else {
            currentTask.effectiveCompletedVolume
        }

        val nextStatus = if (currentTask.taskStatus == TaskStatus.NEW && updatedDecoded.any { it.progressPercent > 0 }) {
            TaskStatus.IN_PROGRESS.code
        } else {
            currentTask.status
        }

        val updatedTask = currentTask
            .copy(subtaskItems = updatedEncoded, status = nextStatus)
            .withUpdatedVolume(tVol, nextTaskCompletedVol, currentTask.effectiveVolumeUnit)

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
        val updatedDecoded = decoded.map { item ->
            if (item.id == subtaskId) {
                val clamped = if (item.targetVolume > 0.0) {
                    newCompletedVol.coerceIn(0.0, item.targetVolume)
                } else {
                    newCompletedVol.coerceAtLeast(0.0)
                }
                val isDoneNow = if (item.targetVolume > 0.0) clamped >= item.targetVolume else item.isDone
                item.copy(completedVolume = clamped, isDone = isDoneNow)
            } else {
                item
            }
        }
        val updatedEncoded = updatedDecoded.map { it.encode() }

        val tVol = currentTask.effectiveTargetVolume
        val nextTaskCompletedVol = if (tVol > 0.0) {
            val matchingVolSub = updatedDecoded.firstOrNull { it.id == subtaskId && it.targetVolume == tVol }
            if (matchingVolSub != null) {
                matchingVolSub.completedVolume
            } else {
                val avgRatio = updatedDecoded.sumOf { it.progressPercent } / (updatedDecoded.size.coerceAtLeast(1) * 100.0)
                (tVol * avgRatio).coerceIn(0.0, tVol)
            }
        } else {
            currentTask.effectiveCompletedVolume
        }

        val nextStatus = if (currentTask.taskStatus == TaskStatus.NEW && newCompletedVol > 0.0) {
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
                    "Отчёт по чек-листу: выполнено ${SubtaskItem.formatVolumeNumber(newCompletedVol)} (${updatedDecoded.firstOrNull { it.id == subtaskId }?.progressPercent ?: 0}%)"
                ).encode()
            )
            .withUpdatedVolume(tVol, nextTaskCompletedVol, currentTask.effectiveVolumeUnit)

        upsertLocalTask(updatedTask)
        _bannerMessage.value = "Объём по пункту чек-листа сохранён (${updatedTask.completionPercent()}%)"

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
        val pct = ((clamped / tVol) * 100.0).toInt().coerceIn(0, 100)

        // Синхронизируем с подзадачей объёма в чек-листе
        val decoded = currentTask.decodedSubtasks()
        val updatedDecoded = decoded.map { sub ->
            if (sub.id == "sub_vol_main" || sub.targetVolume == tVol) {
                sub.copy(
                    completedVolume = clamped,
                    isDone = clamped >= tVol
                )
            } else if (pct == 100) {
                sub.copy(
                    isDone = true,
                    completedVolume = if (sub.hasVolume) sub.targetVolume else sub.completedVolume
                )
            } else {
                sub
            }
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
        targetVolume: Double = 0.0,
        unit: String = ""
    ) {
        val clean = title.trim()
        if (clean.isBlank()) return
        val currentTask = findTaskById(task.id) ?: task
        val newItem = SubtaskItem(
            id = "sub_${System.currentTimeMillis()}",
            title = clean,
            isDone = false,
            targetVolume = targetVolume.coerceAtLeast(0.0),
            completedVolume = 0.0,
            unit = unit.trim()
        ).encode()
        val updatedEncoded = currentTask.subtaskItems + newItem
        val updatedTask = currentTask.copy(
            subtaskItems = updatedEncoded
        )
        upsertLocalTask(updatedTask)

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

    fun submitCompletion(task: TaskDocument, note: String) {
        val currentTask = findTaskById(task.id) ?: task
        val actor = activeEmployee.value
        if (currentTask.requiresPhotoReport && currentTask.photoItems.size < currentTask.minPhotoCount) {
            _bannerMessage.value = "Прикрепите минимум ${currentTask.minPhotoCount} фото (сейчас: ${currentTask.photoItems.size})"
            return
        }
        if (currentTask.hasVolumeTracking() && !currentTask.isVolumeCompleted100Percent()) {
            _bannerMessage.value = "Задача считается выполненной при 100% объёма в чек-листе (сейчас: ${currentTask.completionPercent()}%)"
            return
        }
        val nextStatus = if (currentTask.requiresApproval) TaskStatus.ON_REVIEW else TaskStatus.APPROVED
        val updatedTask = currentTask.copy(
            status = nextStatus.code,
            rejectionReason = null,
            historyItems = currentTask.historyItems + StatusHistoryEntry(
                nowText(),
                actor.shortName,
                nextStatus.code,
                note.ifBlank { "Отправлено на проверку (выполнено 100%)" }
            ).encode()
        )
        upsertLocalTask(updatedTask)
        _bannerMessage.value = if (nextStatus == TaskStatus.ON_REVIEW) {
            "Задача (100%) отправлена на проверку"
        } else {
            "Задача выполнена на 100%"
        }

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.submitTaskCompletion(actor, currentTask, note)
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
        includePhotos: Boolean = true
    ) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val allTasks = (tasksState.value as? UiState.Success)?.data ?: emptyList()
            val day = _currentEpochDay.value
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
                        includePhotos = includePhotos
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
                        templateName = "${template.name} (${if (includePhotos) "с фото" else "без фото"})",
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
                    _bannerMessage.value = "Сформирован файл: ${generated.file.name}"
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
