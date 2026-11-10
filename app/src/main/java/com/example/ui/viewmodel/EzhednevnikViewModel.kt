package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLogDocument
import com.example.data.model.CorporateEmployee
import com.example.data.model.ExportFormat
import com.example.data.model.GeneratedReportDocument
import com.example.data.model.ReportTemplateDocument
import com.example.data.model.SheetLayout
import com.example.data.model.StatisticsSummary
import com.example.data.model.TaskDocument
import com.example.data.model.TaskDraft
import com.example.data.model.TaskStatus
import com.example.data.model.UserProfile
import com.example.data.repository.EzhednevnikRepository
import com.example.data.seed.CorporateSeedData
import com.example.domain.reporting.GeneratedDocumentResult
import com.example.domain.reporting.ReportDocumentGenerator
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
    private val currentUserId: String
) : ViewModel() {

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

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing userProfile", rootError)
            emit(UiState.Error(rootError.message ?: "Ошибка загрузки профиля"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val activeEmployee: StateFlow<CorporateEmployee> = userProfileState
        .map { state ->
            val personaId = (state as? UiState.Success)?.data?.activePersonaId ?: "emp_head_1"
            CorporateSeedData.findEmployee(personaId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CorporateSeedData.findEmployee("emp_head_1")
        )

    val tasksState: StateFlow<UiState<List<TaskDocument>>> = repository.observeTasks(currentUserId)
        .map<List<TaskDocument>, UiState<List<TaskDocument>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing tasks", rootError)
            emit(UiState.Error(rootError.message ?: "Ошибка загрузки задач"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val templatesState: StateFlow<UiState<List<ReportTemplateDocument>>> =
        repository.observeReportTemplates(currentUserId)
            .map<List<ReportTemplateDocument>, UiState<List<ReportTemplateDocument>>> {
                if (it.isEmpty()) {
                    UiState.Success(CorporateSeedData.buildInitialReportTemplates(currentUserId))
                } else {
                    UiState.Success(it)
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing report templates", rootError)
                emit(UiState.Error(rootError.message ?: "Ошибка загрузки шаблонов"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Success(CorporateSeedData.buildInitialReportTemplates(currentUserId))
            )

    val generatedReportsState: StateFlow<UiState<List<GeneratedReportDocument>>> =
        repository.observeGeneratedReports(currentUserId)
            .map<List<GeneratedReportDocument>, UiState<List<GeneratedReportDocument>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing generated reports", rootError)
                emit(UiState.Error(rootError.message ?: "Ошибка загрузки истории отчётов"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val auditLogsState: StateFlow<UiState<List<AuditLogDocument>>> =
        repository.observeAuditLogs(currentUserId)
            .map<List<AuditLogDocument>, UiState<List<AuditLogDocument>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing audit logs", rootError)
                emit(UiState.Error(rootError.message ?: "Ошибка загрузки аудита"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
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
            emptyList()
        )
    )

    init {
        viewModelScope.launch {
            repository.ensureWorkspaceSeeded()
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
            val result = repository.switchActivePersona(personaId)
            if (result.isSuccess) {
                _bannerMessage.value = "Активный профиль: ${emp.fullName} (${emp.role.titleRu})"
            } else {
                _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Ошибка смены роли"
            }
        }
    }

    fun createNewTask(draft: TaskDraft) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val result = repository.createTask(actor, draft)
            if (result.isSuccess) {
                _bannerMessage.value = "Задача «${draft.title}» успешно поставлена"
            } else {
                _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Ошибка создания задачи"
            }
        }
    }

    fun addPhotoToTask(task: TaskDocument, uriString: String, gpsCoords: String, comment: String) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val result = repository.addPhotoToTask(actor, task, uriString, gpsCoords, comment)
            if (result.isSuccess) {
                _bannerMessage.value = "Фото добавлено в фотоотчёт (${task.photoItems.size + 1} шт.)"
            } else {
                _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Ошибка добавления фото"
            }
        }
    }

    fun toggleSubtask(task: TaskDocument, subtaskId: String) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            repository.toggleSubtask(actor, task, subtaskId)
        }
    }

    fun addComment(task: TaskDocument, text: String, mentionedUser: String) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val result = repository.addComment(actor, task, text, mentionedUser)
            if (result.isFailure) {
                _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Ошибка отправки комментария"
            }
        }
    }

    fun submitCompletion(task: TaskDocument, note: String) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val result = repository.submitTaskCompletion(actor, task, note)
            if (result.isSuccess) {
                val newStatus = result.getOrNull()
                _bannerMessage.value = if (newStatus == TaskStatus.ON_REVIEW) {
                    "Задача отправлена на проверку ИТР/Руководителю"
                } else {
                    "Задача отмечена выполненной"
                }
            } else {
                _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Не удалось завершить задачу"
            }
        }
    }

    fun approveReport(task: TaskDocument, comment: String) {
        viewModelScope.launch {
            val supervisor = activeEmployee.value
            val result = repository.approveTaskReport(supervisor, task, comment)
            if (result.isSuccess) {
                _bannerMessage.value = "Отчёт подтверждён. Задача зачёркнута и зафиксирована в аудите"
            } else {
                _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Ошибка подтверждения"
            }
        }
    }

    fun rejectReport(task: TaskDocument, reason: String) {
        viewModelScope.launch {
            val supervisor = activeEmployee.value
            val result = repository.rejectTaskReport(supervisor, task, reason)
            if (result.isSuccess) {
                _bannerMessage.value = "Отчёт отклонён и возвращён исполнителю с причиной"
            } else {
                _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Укажите причину отклонения"
            }
        }
    }

    fun archiveTask(task: TaskDocument) {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val result = repository.archiveTask(actor, task)
            if (result.isSuccess) {
                _bannerMessage.value = "Задача перемещена в архив"
            } else {
                _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Ошибка архивации"
            }
        }
    }

    fun triggerEndOfDayCarryover() {
        viewModelScope.launch {
            val actor = activeEmployee.value
            val allTasks = (tasksState.value as? UiState.Success)?.data ?: emptyList()
            val day = _currentEpochDay.value
            val result = repository.runEndOfDayCarryover(actor, allTasks, day)
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _bannerMessage.value = if (count > 0) {
                    "Закрытие смены: автоматически перенесено $count невыполненных задач на следующий день"
                } else {
                    "Все активные задачи на сегодня уже закрыты или на проверке"
                }
            } else {
                _bannerMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Ошибка автопереноса"
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
                // Persist template layout & format choice
                repository.saveReportTemplate(
                    actor = actor,
                    template = template.copy(
                        sheetLayout = sheetLayout.code,
                        exportFormat = exportFormat.code
                    )
                )
                if (saveToHistory) {
                    val rowCount = generated.sheets.sumOf { it.rows.size }.toLong()
                    repository.recordGeneratedReport(
                        actor = actor,
                        report = GeneratedReportDocument(
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
                            rowCount = rowCount
                        )
                    )
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
            val res = repository.saveReportTemplate(actor, template)
            if (res.isSuccess) {
                _bannerMessage.value = "Шаблон отчёта «${template.name}» сохранён"
            } else {
                _bannerMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Ошибка сохранения шаблона"
            }
        }
    }

    companion object {
        private const val TAG = "EzhednevnikVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
