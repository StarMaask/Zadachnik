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
        CorporateSeedData.buildInitialReportTemplates(currentUserId)
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
        siteMembersState
    ) { memberRecord, allMembers ->
        val matched = allMembers.firstOrNull {
            it.memberId == currentUserId ||
                (currentUserEmail.isNotBlank() && it.email.equals(currentUserEmail, ignoreCase = true))
        } ?: memberRecord

        if (matched != null) {
            matched.toCorporateEmployee()
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
                siteName = CorporateSeedData.DEFAULT_SITE_NAME,
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
            siteName = CorporateSeedData.DEFAULT_SITE_NAME,
            supervisorId = null,
            phone = "",
            email = currentUserEmail
        )
    )

    // Список реальных сотрудников для назначения задач (без фейковых людей)
    val availableEmployees: StateFlow<List<CorporateEmployee>> = combine(
        activeEmployee,
        siteMembersState
    ) { currentEmp, members ->
        val approvedEmployees = members
            .filter { !it.isPendingRole && !it.isRejected }
            .map { it.toCorporateEmployee() }
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
        _localTemplates
    ) { remote, local ->
        UiState.Success(if (remote.isNotEmpty()) remote else local)
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
        assignedGroup: WorkGroup
    ) {
        if (email.isBlank() || fullName.isBlank()) {
            _bannerMessage.value = "Укажите ФИО и Email сотрудника"
            return
        }
        val actor = activeEmployee.value
        val normalizedEmail = email.trim().lowercase()
        val safeId = "emp_${normalizedEmail.replace(Regex("[^a-zA-Z0-9]"), "_").take(40)}"
        val resolvedPosition = position.ifBlank { assignedRole.titleRu }

        val created = SiteMemberDocument(
            id = safeId,
            memberId = safeId,
            email = normalizedEmail,
            fullName = fullName.trim(),
            position = resolvedPosition,
            role = assignedRole.code,
            approvalStatus = MemberApprovalStatus.APPROVED.code,
            groupId = assignedGroup.id,
            groupName = assignedGroup.name,
            approvedByName = actor.fullName
        )
        upsertLocalMember(created)
        appendLocalAudit(
            actor = actor,
            actionType = "PRE_REGISTER_USER",
            targetType = "USER",
            targetId = created.memberId,
            details = "Добавлен сотрудник ${created.fullName} (${created.email}) — ${assignedRole.titleRu}"
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
                    assignedGroup = assignedGroup
                )
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
        _bannerMessage.value = if (enabled) {
            "Автогенерация суточного отчёта (21:00) включена"
        } else {
            "Автогенерация суточного отчёта отключена"
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
        val assignee = availableEmployees.value.firstOrNull { it.id == draft.assigneeId } ?: actor

        val taskId = "task_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val encodedSubtasks = draft.subtasks
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapIndexed { idx, s ->
                SubtaskItem("sub_${System.currentTimeMillis()}_${idx + 1}", s, false).encode()
            }

        val newTask = TaskDocument(
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
            tags = draft.tags,
            subtaskItems = encodedSubtasks,
            photoItems = emptyList(),
            commentItems = emptyList(),
            historyItems = listOf(
                StatusHistoryEntry(
                    nowText(),
                    actor.shortName,
                    TaskStatus.NEW.code,
                    "Задача создана"
                ).encode()
            ),
            geoLocation = draft.geoLocation.ifBlank { "Участок №1" }
        )

        // Мгновенно добавляем задачу в локальный список и сбрасываем фильтр, чтобы она сразу отобразилась
        upsertLocalTask(newTask)
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

        _bannerMessage.value = "Задача «$cleanTitle» добавлена"

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.createTask(
                    actor = actor,
                    draft = draft,
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
        val actor = activeEmployee.value
        val decoded = currentTask.decodedSubtasks()
        val updatedSubtasks = decoded.map { item ->
            if (item.id == subtaskId) item.copy(isDone = !item.isDone) else item
        }.map { it.encode() }

        val updatedTask = currentTask.copy(subtaskItems = updatedSubtasks)
        upsertLocalTask(updatedTask)

        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.toggleSubtask(actor, currentTask, subtaskId)
            }
        }
    }

    fun addSubtaskToTask(task: TaskDocument, title: String) {
        val clean = title.trim()
        if (clean.isBlank()) return
        val currentTask = findTaskById(task.id) ?: task
        val newItem = SubtaskItem(
            id = "sub_${System.currentTimeMillis()}",
            title = clean,
            isDone = false
        ).encode()
        val updatedTask = currentTask.copy(
            subtaskItems = currentTask.subtaskItems + newItem
        )
        upsertLocalTask(updatedTask)
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
        val nextStatus = if (currentTask.requiresApproval) TaskStatus.ON_REVIEW else TaskStatus.APPROVED
        val updatedTask = currentTask.copy(
            status = nextStatus.code,
            rejectionReason = null,
            historyItems = currentTask.historyItems + StatusHistoryEntry(
                nowText(),
                actor.shortName,
                nextStatus.code,
                note.ifBlank { "Отправлено на проверку" }
            ).encode()
        )
        upsertLocalTask(updatedTask)
        _bannerMessage.value = if (nextStatus == TaskStatus.ON_REVIEW) {
            "Задача отправлена на проверку"
        } else {
            "Задача выполнена"
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
                            "Перенесено на следующий день (перенос №${t.carryoverCount + 1L})"
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
        saveToHistory: Boolean
    ) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val allTasks = (tasksState.value as? UiState.Success)?.data ?: emptyList()
            val day = _currentEpochDay.value
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    ReportDocumentGenerator.generateAndSaveDocument(
                        context = context,
                        actor = actor,
                        template = template,
                        allTasks = allTasks,
                        currentEpochDay = day,
                        overrideLayout = sheetLayout,
                        overrideFormat = exportFormat
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
                        templateName = template.name,
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
