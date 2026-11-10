package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
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

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class TaskFilterTab(val titleRu: String) {
    TODAY("На сегодня"),
    TOMORROW("План на завтра"),
    ON_REVIEW("На проверке"),
    OVERDUE("Просроченные"),
    APPROVED("Выполненные"),
    RECURRING("Повторяющиеся"),
    ARCHIVE("Архив"),
    ALL("Все задачи")
}

class EzhednevnikViewModel(
    private val repository: EzhednevnikRepository,
    private val currentUserId: String,
    private val currentUserEmail: String = "",
    private val currentUserName: String = "",
    private val initialHeadActivated: Boolean = false
) : ViewModel() {

    private val isCloudAuthenticated: Boolean
        get() = Firebase.auth.currentUser != null

    private val _currentEpochDay = MutableStateFlow(CorporateSeedData.BASE_EPOCH_DAY)
    val currentEpochDay: StateFlow<Long> = _currentEpochDay.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaskFilterTab.TODAY)
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

    // Local state fallback & instant optimistic updates
    private val _localPersonaId = MutableStateFlow("emp_head_1")
    private val _localTasks = MutableStateFlow(CorporateSeedData.buildInitialTasks(currentUserId))
    private val _localTemplates = MutableStateFlow(CorporateSeedData.buildInitialReportTemplates(currentUserId))
    private val _localGeneratedReports = MutableStateFlow<List<GeneratedReportDocument>>(emptyList())
    private val _localAuditLogs = MutableStateFlow(CorporateSeedData.buildInitialAuditLogs(currentUserId))
    private val _localSiteMembers = MutableStateFlow(CorporateSeedData.buildInitialSiteMembers())
    private val _currentMemberRecord = MutableStateFlow<SiteMemberDocument?>(null)
    val currentMemberRecord: StateFlow<SiteMemberDocument?> = _currentMemberRecord.asStateFlow()

    private val firestoreProfileFlow = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(
                UiState.Success(
                    UserProfile(
                        id = currentUserId,
                        userId = currentUserId,
                        displayName = currentUserName.ifBlank { "Ковалёв Д.С." },
                        siteName = CorporateSeedData.DEFAULT_SITE_NAME,
                        activePersonaId = _localPersonaId.value
                    )
                )
            )
        }

    val userProfileState: StateFlow<UiState<UserProfile?>> = combine(
        firestoreProfileFlow,
        _localPersonaId
    ) { fsState, localPersona ->
        val fsProfile = (fsState as? UiState.Success)?.data
        if (fsProfile != null) {
            UiState.Success(fsProfile)
        } else {
            UiState.Success(
                UserProfile(
                    id = currentUserId,
                    userId = currentUserId,
                    displayName = currentUserName.ifBlank { "Ковалёв Д.С." },
                    siteName = CorporateSeedData.DEFAULT_SITE_NAME,
                    activePersonaId = localPersona
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    private val firestoreSiteMembersFlow = repository.observeSiteMembers()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }

    val siteMembersState: StateFlow<List<SiteMemberDocument>> = combine(
        firestoreSiteMembersFlow,
        _localSiteMembers
    ) { remoteList, localList ->
        val mergedMap = LinkedHashMap<String, SiteMemberDocument>()
        localList.forEach { m -> mergedMap[m.memberId.ifBlank { m.id }] = m }
        remoteList.forEach { m -> mergedMap[m.memberId.ifBlank { m.id }] = m }
        mergedMap.values.sortedWith(
            compareBy<SiteMemberDocument> { !it.isPendingRole }
                .thenBy { it.fullName }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CorporateSeedData.buildInitialSiteMembers()
    )

    val activeEmployee: StateFlow<CorporateEmployee> = combine(
        userProfileState,
        _currentMemberRecord,
        _localPersonaId
    ) { state, memberRecord, localPersona ->
        val profilePersonaId = (state as? UiState.Success)?.data?.activePersonaId ?: localPersona
        // Find in seed employees or in dynamically approved site members
        val seedMatch = CorporateSeedData.employees.firstOrNull { it.id == profilePersonaId }
        if (seedMatch != null) {
            seedMatch
        } else if (memberRecord != null && !memberRecord.isPendingRole) {
            memberRecord.toCorporateEmployee()
        } else {
            CorporateSeedData.findEmployee(profilePersonaId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CorporateSeedData.findEmployee("emp_head_1")
    )

    private val firestoreTasksFlow = repository.observeTasks(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }

    val tasksState: StateFlow<UiState<List<TaskDocument>>> = combine(
        firestoreTasksFlow,
        _localTasks
    ) { remoteTasks, localTasks ->
        val list = if (remoteTasks.isNotEmpty()) remoteTasks else localTasks
        UiState.Success(
            list.sortedWith(
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
        UiState.Success(if (remote.isNotEmpty()) remote else local)
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
        UiState.Success(if (remote.isNotEmpty()) remote else local)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Success(_localAuditLogs.value)
    )

    val statisticsState: StateFlow<StatisticsSummary> = combine(
        activeEmployee,
        tasksState
    ) { employee, tState ->
        val allTasks = (tState as? UiState.Success)?.data ?: emptyList()
        repository.calculateRoleScopedStatistics(employee, allTasks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = repository.calculateRoleScopedStatistics(
            CorporateSeedData.findEmployee("emp_head_1"),
            _localTasks.value
        )
    )

    init {
        initializeUserAndRoleStatus()
    }

    private fun initializeUserAndRoleStatus() {
        viewModelScope.launch {
            if (isCloudAuthenticated) {
                repository.ensureWorkspaceSeeded(currentUserName.ifBlank { "Ковалёв Д.С." })
            }

            // Check if user matches a pre-registered or root Head email
            val normalizedEmail = currentUserEmail.trim().lowercase()
            val isRootHead = initialHeadActivated ||
                SiteMemberDocument.isRootHeadEmail(normalizedEmail) ||
                normalizedEmail.isBlank()

            val existingMatch = _localSiteMembers.value.firstOrNull {
                it.email.equals(normalizedEmail, ignoreCase = true) ||
                    it.memberId == currentUserId
            }

            val resolvedMember = if (isRootHead) {
                repository.registerOrResolveSiteMember(
                    rawMemberId = currentUserId,
                    email = normalizedEmail.ifBlank { "m241185m@gmail.com" },
                    fullName = currentUserName.ifBlank { "Ковалёв Дмитрий Сергеевич" },
                    position = "Начальник строительного участка",
                    requestedGroup = CorporateSeedData.workGroups.last(),
                    forceHeadActivation = true
                ).getOrNull() ?: SiteMemberDocument(
                    id = currentUserId,
                    memberId = currentUserId,
                    email = normalizedEmail.ifBlank { "m241185m@gmail.com" },
                    fullName = currentUserName.ifBlank { "Ковалёв Дмитрий Сергеевич" },
                    position = "Начальник строительного участка",
                    role = CorporateRole.HEAD.code,
                    approvalStatus = MemberApprovalStatus.APPROVED.code,
                    groupId = "grp_control",
                    groupName = "Служба ИТР и стройконтроля",
                    approvedByName = "Система (Владелец / Руководитель)"
                )
            } else if (existingMatch != null) {
                existingMatch
            } else {
                val created = repository.registerOrResolveSiteMember(
                    rawMemberId = currentUserId,
                    email = normalizedEmail,
                    fullName = currentUserName.ifBlank { "Новый сотрудник" },
                    position = "Специалист участка",
                    requestedGroup = CorporateSeedData.workGroups.first(),
                    forceHeadActivation = false
                ).getOrNull() ?: SiteMemberDocument(
                    id = currentUserId,
                    memberId = currentUserId,
                    email = normalizedEmail,
                    fullName = currentUserName.ifBlank { "Новый сотрудник" },
                    position = "Специалист участка",
                    role = "PENDING",
                    approvalStatus = MemberApprovalStatus.PENDING.code,
                    groupId = "grp_monolith",
                    groupName = "Бригада №1 • Монолит и ЖБК"
                )
                _localSiteMembers.value = listOf(created) + _localSiteMembers.value.filterNot {
                    it.memberId == created.memberId
                }
                created
            }

            _currentMemberRecord.value = resolvedMember
            if (!resolvedMember.isPendingRole) {
                val targetPersonaId = when (resolvedMember.parsedRoleOrNull) {
                    CorporateRole.HEAD -> "emp_head_1"
                    CorporateRole.ITR -> "emp_itr_1"
                    CorporateRole.WORKER -> "emp_worker_1"
                    null -> "emp_worker_1"
                }
                _localPersonaId.value = targetPersonaId
            }
        }
    }

    fun activateHeadAccessWithCode(code: String): Boolean {
        if (code.trim() != SiteMemberDocument.DEFAULT_HEAD_ACTIVATION_CODE) {
            _bannerMessage.value = "Неверный код активации руководителя (используйте код 1001)"
            return false
        }
        viewModelScope.launch {
            val email = currentUserEmail.ifBlank { "m241185m@gmail.com" }
            val name = currentUserName.ifBlank { "Ковалёв Дмитрий Сергеевич" }
            val headMember = SiteMemberDocument(
                id = currentUserId,
                memberId = currentUserId,
                email = email,
                fullName = name,
                position = "Начальник строительного участка",
                role = CorporateRole.HEAD.code,
                approvalStatus = MemberApprovalStatus.APPROVED.code,
                groupId = "grp_control",
                groupName = "Служба ИТР и стройконтроля",
                approvedByName = "Активация по коду руководителя (1001)"
            )
            _currentMemberRecord.value = headMember
            _localPersonaId.value = "emp_head_1"
            _localSiteMembers.value = listOf(headMember) + _localSiteMembers.value.filterNot {
                it.memberId == headMember.memberId
            }
            repository.registerOrResolveSiteMember(
                rawMemberId = currentUserId,
                email = email,
                fullName = name,
                position = "Начальник строительного участка",
                requestedGroup = CorporateSeedData.workGroups.last(),
                forceHeadActivation = true
            )
            _bannerMessage.value = "Подтверждён вход Руководителя участка. Полный доступ открыт."
        }
        return true
    }

    fun updateMyPendingRequestDetails(position: String, group: WorkGroup) {
        val current = _currentMemberRecord.value ?: return
        val updated = current.copy(
            position = position.ifBlank { "Специалист участка" },
            groupId = group.id,
            groupName = group.name
        )
        _currentMemberRecord.value = updated
        _localSiteMembers.value = _localSiteMembers.value.map {
            if (it.memberId == updated.memberId) updated else it
        }
        viewModelScope.launch {
            repository.registerOrResolveSiteMember(
                rawMemberId = updated.memberId,
                email = updated.email,
                fullName = updated.fullName,
                position = updated.position,
                requestedGroup = group,
                forceHeadActivation = false
            )
            _bannerMessage.value = "Заявка обновлена и отправлена Руководителю участка"
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
                val targetPersonaId = when (latestFromList.parsedRoleOrNull) {
                    CorporateRole.HEAD -> "emp_head_1"
                    CorporateRole.ITR -> "emp_itr_1"
                    CorporateRole.WORKER -> "emp_worker_1"
                    null -> "emp_worker_1"
                }
                _localPersonaId.value = targetPersonaId
                _bannerMessage.value = "Руководитель назначил вам роль: ${latestFromList.parsedRoleOrNull?.titleRu}"
            } else {
                _bannerMessage.value = "Статус заявки: ожидает назначения роли руководителем участка"
            }
        }
    }

    fun assignRoleToMember(
        targetMember: SiteMemberDocument,
        assignedRole: CorporateRole,
        assignedGroup: WorkGroup,
        customPosition: String? = null
    ) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val result = repository.assignRoleToSiteMember(
                actor = actor,
                targetMember = targetMember,
                assignedRole = assignedRole,
                assignedGroup = assignedGroup,
                customPosition = customPosition
            )
            if (result.isSuccess) {
                val updated = result.getOrThrow()
                _localSiteMembers.value = _localSiteMembers.value.map {
                    if (it.memberId == updated.memberId || it.id == updated.id) updated else it
                }.let { list ->
                    if (list.none { it.memberId == updated.memberId }) listOf(updated) + list else list
                }
                if (_currentMemberRecord.value?.memberId == updated.memberId) {
                    _currentMemberRecord.value = updated
                }
                appendLocalAudit(
                    actor = actor,
                    actionType = "ASSIGN_USER_ROLE",
                    targetType = "USER",
                    targetId = updated.memberId,
                    details = "Назначена роль «${assignedRole.titleRu}» (${assignedGroup.name}) пользователю ${updated.fullName}"
                )
                _bannerMessage.value = "Назначена роль «${assignedRole.titleRu}» для ${updated.fullName}"
            } else {
                _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Ошибка назначения роли"
            }
        }
    }

    fun rejectMemberAccess(targetMember: SiteMemberDocument) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val result = repository.rejectSiteMemberAccess(actor, targetMember)
            if (result.isSuccess) {
                val updated = result.getOrThrow()
                _localSiteMembers.value = _localSiteMembers.value.map {
                    if (it.memberId == updated.memberId || it.id == updated.id) updated else it
                }
                appendLocalAudit(
                    actor = actor,
                    actionType = "REJECT_USER_ACCESS",
                    targetType = "USER",
                    targetId = updated.memberId,
                    details = "Отклонён доступ пользователю ${updated.fullName} (${updated.email})"
                )
                _bannerMessage.value = "Заявка пользователя ${updated.fullName} отклонена"
            } else {
                _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Ошибка отклонения заявки"
            }
        }
    }

    fun preRegisterEmployeeByEmail(
        email: String,
        fullName: String,
        position: String,
        assignedRole: CorporateRole,
        assignedGroup: WorkGroup
    ) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val result = repository.preRegisterSiteMemberByEmail(
                actor = actor,
                email = email,
                fullName = fullName,
                position = position,
                assignedRole = assignedRole,
                assignedGroup = assignedGroup
            )
            if (result.isSuccess) {
                val created = result.getOrThrow()
                _localSiteMembers.value = listOf(created) + _localSiteMembers.value.filterNot {
                    it.email.equals(created.email, ignoreCase = true)
                }
                appendLocalAudit(
                    actor = actor,
                    actionType = "PRE_REGISTER_USER",
                    targetType = "USER",
                    targetId = created.memberId,
                    details = "Зарегистрирован сотрудник ${created.fullName} (${created.email}) с ролью «${assignedRole.titleRu}»"
                )
                _bannerMessage.value = "Сотрудник ${created.fullName} добавлен с ролью «${assignedRole.titleRu}»"
            } else {
                _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Ошибка регистрации сотрудника"
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
            "Автогенерация «План работ на завтра» включена (ежедневно в 19:00)"
        } else {
            "Автогенерация вечернего плана отключена"
        }
    }

    fun toggleAutoScheduleReport(enabled: Boolean) {
        _autoScheduleReportNight.value = enabled
        _bannerMessage.value = if (enabled) {
            "Автогенерация «Отчёт за текущий день» включена (ежедневно в 21:00)"
        } else {
            "Автогенерация вечернего отчёта отключена"
        }
    }

    fun switchPersona(personaId: String) {
        viewModelScope.launch {
            val emp = CorporateSeedData.findEmployee(personaId)
            _localPersonaId.value = personaId
            if (isCloudAuthenticated) {
                repository.switchActivePersona(personaId)
            }
            _bannerMessage.value = "Активный профиль: ${emp.fullName} (${emp.role.titleRu})"
        }
    }

    fun createNewTask(draft: TaskDraft) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val assignee = CorporateSeedData.findEmployee(draft.assigneeId)
            if (!actor.role.canAssignTo(assignee.role) ||
                (actor.role == CorporateRole.WORKER && assignee.id != actor.id)
            ) {
                _bannerMessage.value = "Недостаточно прав: роль «${actor.role.titleRu}» не может назначать задачи роли «${assignee.role.titleRu}»"
                return@launch
            }

            if (isCloudAuthenticated) {
                val result = repository.createTask(actor, draft)
                if (result.isSuccess) {
                    _bannerMessage.value = "Задача «${draft.title}» успешно поставлена"
                    return@launch
                }
            }

            // Local fallback update
            val newTask = TaskDocument(
                id = "local_task_${System.currentTimeMillis()}",
                userId = currentUserId,
                title = draft.title.trim(),
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
                deadlineTime = draft.deadlineTime,
                carryoverCount = 0L,
                requiresPhotoReport = draft.requiresPhotoReport,
                minPhotoCount = draft.minPhotoCount.toLong(),
                requiresApproval = draft.requiresApproval,
                recurrenceRule = draft.recurrenceRule.code,
                tags = draft.tags,
                subtaskItems = draft.subtasks.mapIndexed { idx, s ->
                    SubtaskItem("sub_${idx + 1}", s, false).encode()
                },
                historyItems = listOf(
                    StatusHistoryEntry(nowText(), actor.shortName, "NEW", "Задача создана (${actor.role.titleRu})").encode()
                ),
                geoLocation = draft.geoLocation.ifBlank { "Участок №1" }
            )
            _localTasks.value = listOf(newTask) + _localTasks.value
            _bannerMessage.value = "Задача «${draft.title}» успешно поставлена"
        }
    }

    fun addPhotoToTask(task: TaskDocument, uriString: String, gpsCoords: String, comment: String) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val newPhoto = PhotoReportItem(
                id = "ph_${System.currentTimeMillis()}",
                uriString = uriString,
                authorId = actor.id,
                authorName = actor.shortName,
                capturedAtText = nowText(),
                gpsCoords = gpsCoords.ifBlank { "55.7558° с.ш., 37.6173° в.д." },
                comment = comment.ifBlank { "Фотофиксация выполнения на участке" }
            ).encode()

            val updatedTask = task.copy(
                photoItems = task.photoItems + newPhoto,
                status = if (task.taskStatus == TaskStatus.NEW) TaskStatus.IN_PROGRESS.code else task.status,
                historyItems = task.historyItems + StatusHistoryEntry(
                    nowText(),
                    actor.shortName,
                    TaskStatus.IN_PROGRESS.code,
                    "Добавлено фото в отчёт (всего: ${task.photoItems.size + 1})"
                ).encode()
            )
            _localTasks.value = _localTasks.value.map { if (it.id == task.id) updatedTask else it }

            if (isCloudAuthenticated) {
                repository.addPhotoToTask(actor, task, uriString, gpsCoords, comment)
            }
            _bannerMessage.value = "Фото добавлено в фотоотчёт (${updatedTask.photoItems.size} шт.)"
        }
    }

    fun toggleSubtask(task: TaskDocument, subtaskId: String) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val updatedSubtasks = task.decodedSubtasks().map {
                if (it.id == subtaskId) it.copy(isDone = !it.isDone) else it
            }.map { it.encode() }
            _localTasks.value = _localTasks.value.map {
                if (it.id == task.id) it.copy(subtaskItems = updatedSubtasks) else it
            }
            if (isCloudAuthenticated) {
                repository.toggleSubtask(actor, task, subtaskId)
            }
        }
    }

    fun addComment(task: TaskDocument, text: String, mentionedUser: String) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            if (text.isBlank()) return@launch
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
            _localTasks.value = _localTasks.value.map {
                if (it.id == task.id) it.copy(commentItems = it.commentItems + newComment) else it
            }
            if (isCloudAuthenticated) {
                repository.addComment(actor, task, text, mentionedUser)
            }
        }
    }

    fun submitCompletion(task: TaskDocument, note: String) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            if (task.requiresPhotoReport && task.photoItems.size < task.minPhotoCount) {
                _bannerMessage.value = "Обязателен фотоотчёт: прикреплено ${task.photoItems.size} из ${task.minPhotoCount} фото"
                return@launch
            }
            val nextStatus = if (task.requiresApproval) TaskStatus.ON_REVIEW else TaskStatus.APPROVED
            val updatedTask = task.copy(
                status = nextStatus.code,
                rejectionReason = null,
                historyItems = task.historyItems + StatusHistoryEntry(
                    nowText(),
                    actor.shortName,
                    nextStatus.code,
                    note.ifBlank { "Отправлено на проверку руководителю/ИТР" }
                ).encode()
            )
            _localTasks.value = _localTasks.value.map { if (it.id == task.id) updatedTask else it }

            if (isCloudAuthenticated) {
                repository.submitTaskCompletion(actor, task, note)
            }
            _bannerMessage.value = if (nextStatus == TaskStatus.ON_REVIEW) {
                "Задача отправлена на проверку ИТР / Руководителю"
            } else {
                "Задача отмечена выполненной"
            }
        }
    }

    fun approveReport(task: TaskDocument, comment: String) {
        viewModelScope.launch {
            val supervisor = activeEmployee.value
            val assigneeRole = CorporateSeedData.findEmployee(task.assigneeId).role
            if (!supervisor.role.canApproveReportOf(assigneeRole)) {
                _bannerMessage.value = "Недостаточно прав: роль «${supervisor.role.titleRu}» не может подтверждать отчёт роли «${assigneeRole.titleRu}»"
                return@launch
            }
            val updatedTask = task.copy(
                status = TaskStatus.APPROVED.code,
                approvedByName = supervisor.fullName,
                rejectionReason = null,
                historyItems = task.historyItems + StatusHistoryEntry(
                    nowText(),
                    supervisor.shortName,
                    TaskStatus.APPROVED.code,
                    comment.ifBlank { "Фотоотчёт проверен и подтверждён" }
                ).encode()
            )
            _localTasks.value = _localTasks.value.map { if (it.id == task.id) updatedTask else it }

            if (isCloudAuthenticated) {
                repository.approveTaskReport(supervisor, task, comment)
            }
            _bannerMessage.value = "Отчёт подтверждён. Задача зачёркнута и зафиксирована в аудите"
        }
    }

    fun rejectReport(task: TaskDocument, reason: String) {
        viewModelScope.launch {
            val supervisor = activeEmployee.value
            if (reason.isBlank()) {
                _bannerMessage.value = "Укажите обязательную причину отклонения отчёта"
                return@launch
            }
            val assigneeRole = CorporateSeedData.findEmployee(task.assigneeId).role
            if (!supervisor.role.canApproveReportOf(assigneeRole)) {
                _bannerMessage.value = "Недостаточно прав для отклонения этого отчёта"
                return@launch
            }
            val updatedTask = task.copy(
                status = TaskStatus.REJECTED.code,
                rejectionReason = reason.trim(),
                historyItems = task.historyItems + StatusHistoryEntry(
                    nowText(),
                    supervisor.shortName,
                    TaskStatus.REJECTED.code,
                    "Отклонено: ${reason.trim()}"
                ).encode()
            )
            _localTasks.value = _localTasks.value.map { if (it.id == task.id) updatedTask else it }

            if (isCloudAuthenticated) {
                repository.rejectTaskReport(supervisor, task, reason)
            }
            _bannerMessage.value = "Отчёт отклонён и возвращён исполнителю с указанием причины"
        }
    }

    fun archiveTask(task: TaskDocument) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            _localTasks.value = _localTasks.value.map {
                if (it.id == task.id) it.copy(status = TaskStatus.ARCHIVED.code) else it
            }
            if (isCloudAuthenticated) {
                repository.archiveTask(actor, task)
            }
            _bannerMessage.value = "Задача перемещена в архив"
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
                            "Автоперенос смены",
                            t.status,
                            "Перенесено на следующий день (перенос №${t.carryoverCount + 1L})"
                        ).encode()
                    )
                } else {
                    t
                }
            }
            _localTasks.value = updatedList
            if (isCloudAuthenticated) {
                repository.runEndOfDayCarryover(actor, allTasks, day)
            }
            _bannerMessage.value = if (movedCount > 0) {
                "Закрытие смены: автоматически перенесено $movedCount невыполненных задач на следующий день"
            } else {
                "Все активные задачи на сегодня уже закрыты или находятся на проверке"
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
                        userId = currentUserId,
                        templateId = template.id.ifBlank { "tpl_custom" },
                        templateName = template.name,
                        reportType = template.reportType,
                        authorName = actor.fullName,
                        periodLabel = generated.periodLabel,
                        sheetLayout = sheetLayout.code,
                        exportFormat = exportFormat.code,
                        filePath = generated.file.absolutePath,
                        summaryText = generated.summaryText,
                        recipients = listOf("ПТО участка", "Руководитель СМР"),
                        filterSummary = generated.roleScopeLabel,
                        rowCount = rowCount,
                        createdAt = Timestamp.now()
                    )
                    _localGeneratedReports.value = listOf(record) + _localGeneratedReports.value
                    if (isCloudAuthenticated) {
                        repository.recordGeneratedReport(actor, record)
                    }
                    _bannerMessage.value = "Сформирован документ: ${generated.file.name} (${exportFormat.code})"
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
            _bannerMessage.value = "Шаблон отчёта «${template.name}» сохранён"
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
            userId = currentUserId,
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
        private const val TAG = "EzhednevnikVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
