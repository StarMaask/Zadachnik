package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.RecurrenceRule
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskDocument
import com.example.data.model.TaskDraft
import com.example.data.model.TaskPriority
import com.example.domain.reporting.ReportDocumentGenerator
import com.example.ui.theme.LocalAppThemeTokens
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private enum class CreateSheetSection {
    ASSIGNEES,
    DEADLINE,
    PHOTO,
    WORK_SCOPE
}

private data class CreateTaskInitialValues(
    val title: String,
    val description: String,
    val initialAssigneeIds: List<String>,
    val useCustomAssigneeName: Boolean,
    val customAssigneeName: String,
    val resolvedInitialDueDay: Long,
    val initialMonthOffset: Int,
    val selectedPriority: TaskPriority,
    val deadlineTime: String,
    val requiresPhoto: Boolean,
    val minPhotoCountText: String,
    val initialEditSubtasks: List<SubtaskItem>,
    val hasVolumeRequirement: Boolean,
    val targetVolumeText: String,
    val selectedUnit: String,
    val isCustomUnitSelected: Boolean,
    val customUnitText: String,
    val requiresApproval: Boolean,
    val selectedRecurrence: RecurrenceRule,
    val geoLocation: String,
    val showMoreOptions: Boolean,
    val hasRecurrenceSeries: Boolean
)

private fun computeCreateTaskInitialValues(
    editingTask: TaskDocument?,
    assignees: List<CorporateEmployee>,
    currentEpochDay: Long,
    initialDueDateEpochDay: Long,
    defaultSiteName: String
): CreateTaskInitialValues {
    val fallbackAssigneeId = assignees.first().id
    if (editingTask == null) {
        val baseYm = YearMonth.from(LocalDate.ofEpochDay(currentEpochDay))
        val targetYm = YearMonth.from(LocalDate.ofEpochDay(initialDueDateEpochDay))
        val monthOffset = ChronoUnit.MONTHS.between(baseYm, targetYm).toInt()
        return CreateTaskInitialValues(
            title = "",
            description = "",
            initialAssigneeIds = listOf(fallbackAssigneeId),
            useCustomAssigneeName = false,
            customAssigneeName = "",
            resolvedInitialDueDay = initialDueDateEpochDay,
            initialMonthOffset = monthOffset,
            selectedPriority = TaskPriority.MEDIUM,
            deadlineTime = "18:00",
            requiresPhoto = true,
            minPhotoCountText = "1",
            initialEditSubtasks = emptyList(),
            hasVolumeRequirement = false,
            targetVolumeText = "",
            selectedUnit = "шт",
            isCustomUnitSelected = false,
            customUnitText = "",
            requiresApproval = true,
            selectedRecurrence = RecurrenceRule.NONE,
            geoLocation = defaultSiteName,
            showMoreOptions = false,
            hasRecurrenceSeries = false
        )
    }

    val matchedIds = assignees.filter { editingTask.isAssignedToEmployee(it) }.map { it.id }
    val useCustomName = matchedIds.isEmpty() && editingTask.assigneeName.isNotBlank()
    val customName = if (matchedIds.isEmpty()) editingTask.assigneeName else ""
    val dueDay = editingTask.dueDateEpochDay
    val baseYm = YearMonth.from(LocalDate.ofEpochDay(currentEpochDay))
    val targetYm = YearMonth.from(LocalDate.ofEpochDay(dueDay))
    val monthOffset = ChronoUnit.MONTHS.between(baseYm, targetYm).toInt()

    val subtasks = editingTask.decodedSubtasks()
    val hasSingleVol = subtasks.isEmpty() && editingTask.effectiveTargetVolume > 0.0
    val editUnit = editingTask.effectiveVolumeUnit.ifBlank { "шт" }
    val isStdUnit = editUnit in STANDARD_VOLUME_UNITS

    return CreateTaskInitialValues(
        title = editingTask.title,
        description = editingTask.description,
        initialAssigneeIds = matchedIds.ifEmpty { listOf(fallbackAssigneeId) }.distinct(),
        useCustomAssigneeName = useCustomName,
        customAssigneeName = customName,
        resolvedInitialDueDay = dueDay,
        initialMonthOffset = monthOffset,
        selectedPriority = editingTask.taskPriority,
        deadlineTime = editingTask.deadlineTime.ifBlank { "18:00" },
        requiresPhoto = editingTask.requiresPhotoReport,
        minPhotoCountText = editingTask.minPhotoCount.toInt().coerceAtLeast(1).toString(),
        initialEditSubtasks = subtasks,
        hasVolumeRequirement = hasSingleVol,
        targetVolumeText = if (hasSingleVol) {
            SubtaskItem.formatVolumeNumber(editingTask.effectiveTargetVolume)
        } else {
            ""
        },
        selectedUnit = if (isStdUnit) editUnit else "шт",
        isCustomUnitSelected = !isStdUnit,
        customUnitText = if (!isStdUnit) editUnit else "",
        requiresApproval = editingTask.requiresApproval,
        selectedRecurrence = editingTask.recurrence,
        geoLocation = editingTask.geoLocation ?: defaultSiteName,
        showMoreOptions = editingTask.description.isNotBlank() ||
            editingTask.taskPriority != TaskPriority.MEDIUM ||
            editingTask.recurrence != RecurrenceRule.NONE ||
            !editingTask.requiresApproval ||
            editingTask.recurrenceSeriesId != null,
        hasRecurrenceSeries = editingTask.recurrenceSeriesId != null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    activeEmployee: CorporateEmployee,
    availableEmployees: List<CorporateEmployee> = listOf(activeEmployee),
    existingTasks: List<TaskDocument> = emptyList(),
    currentEpochDay: Long,
    initialDueDateEpochDay: Long = currentEpochDay,
    editingTask: TaskDocument? = null,
    onDismiss: () -> Unit,
    onCreate: (TaskDraft) -> Unit
) {
    val tokens = LocalAppThemeTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editTaskId = editingTask?.id
    val isEditMode = editingTask != null

    var itrAssigneeFilterBySubdivision by remember(editTaskId) {
        mutableStateOf(activeEmployee.role == CorporateRole.ITR && !isEditMode)
    }
    val assignees = remember(availableEmployees, activeEmployee, itrAssigneeFilterBySubdivision) {
        if (!activeEmployee.role.canAssignToOtherEmployees()) {
            listOf(activeEmployee)
        } else {
            val candidates = availableEmployees
                .filter { activeEmployee.role.canAssignTo(it.role) }
                .ifEmpty { listOf(activeEmployee) }

            if (activeEmployee.role == CorporateRole.ITR && itrAssigneeFilterBySubdivision) {
                val myGroup = candidates.filter {
                    it.groupId == activeEmployee.groupId ||
                        (it.groupName.isNotBlank() && it.groupName.equals(activeEmployee.groupName, ignoreCase = true)) ||
                        it.id == activeEmployee.id
                }
                myGroup.ifEmpty { candidates }
            } else {
                candidates.sortedWith(
                    compareByDescending<CorporateEmployee> {
                        it.groupId == activeEmployee.groupId ||
                            (it.groupName.isNotBlank() && it.groupName.equals(activeEmployee.groupName, ignoreCase = true))
                    }.thenBy { it.fullName }
                )
            }
        }
    }

    val initialValues = remember(
        editTaskId,
        assignees,
        currentEpochDay,
        initialDueDateEpochDay,
        activeEmployee.siteName
    ) {
        computeCreateTaskInitialValues(
            editingTask = editingTask,
            assignees = assignees,
            currentEpochDay = currentEpochDay,
            initialDueDateEpochDay = initialDueDateEpochDay,
            defaultSiteName = activeEmployee.siteName
        )
    }

    var title by remember(editTaskId) { mutableStateOf(initialValues.title) }
    var description by remember(editTaskId) { mutableStateOf(initialValues.description) }

    val selectedAssigneeIds = remember(editTaskId, assignees) {
        mutableStateListOf<String>().apply { addAll(initialValues.initialAssigneeIds) }
    }
    val selectedAssignees = remember(assignees, selectedAssigneeIds.toList()) {
        assignees.filter { it.id in selectedAssigneeIds }.ifEmpty { listOf(assignees.first()) }
    }
    val selectedAssignee = selectedAssignees.first()
    var useCustomAssigneeName by remember(editTaskId) {
        mutableStateOf(initialValues.useCustomAssigneeName)
    }
    var customAssigneeName by remember(editTaskId) {
        mutableStateOf(initialValues.customAssigneeName)
    }
    var assigneeSearchQuery by remember(editTaskId) { mutableStateOf("") }

    var selectedPriority by remember(editTaskId) {
        mutableStateOf(initialValues.selectedPriority)
    }
    var selectedDueEpochDay by remember(editTaskId, initialDueDateEpochDay) {
        mutableStateOf(initialValues.resolvedInitialDueDay)
    }
    var calendarExpanded by remember(editTaskId) { mutableStateOf(false) }
    var displayedMonthOffset by remember(editTaskId, initialValues.initialMonthOffset) {
        mutableIntStateOf(initialValues.initialMonthOffset)
    }
    var deadlineTime by remember(editTaskId) { mutableStateOf(initialValues.deadlineTime) }

    var requiresPhoto by remember(editTaskId) { mutableStateOf(initialValues.requiresPhoto) }
    var minPhotoCountText by remember(editTaskId) { mutableStateOf(initialValues.minPhotoCountText) }

    val draftChecklistItems = remember(editTaskId) {
        mutableStateListOf<SubtaskItem>().apply { addAll(initialValues.initialEditSubtasks) }
    }
    var draftItemTitle by remember(editTaskId) { mutableStateOf("") }
    var draftItemVolText by remember(editTaskId) { mutableStateOf("10") }
    var draftItemUnit by remember(editTaskId) { mutableStateOf("шт") }
    var draftUnitMenuExpanded by remember(editTaskId) { mutableStateOf(false) }

    var hasVolumeRequirement by remember(editTaskId) {
        mutableStateOf(initialValues.hasVolumeRequirement)
    }
    var targetVolumeText by remember(editTaskId) {
        mutableStateOf(initialValues.targetVolumeText)
    }
    var selectedUnit by remember(editTaskId) { mutableStateOf(initialValues.selectedUnit) }
    var isCustomUnitSelected by remember(editTaskId) {
        mutableStateOf(initialValues.isCustomUnitSelected)
    }
    var customUnitText by remember(editTaskId) { mutableStateOf(initialValues.customUnitText) }

    var activeSection by remember(editTaskId) { mutableStateOf<CreateSheetSection?>(null) }
    var requiresApproval by remember(editTaskId) { mutableStateOf(initialValues.requiresApproval) }
    var selectedRecurrence by remember(editTaskId) {
        mutableStateOf(initialValues.selectedRecurrence)
    }
    var applyToFutureSeries by remember(editTaskId) { mutableStateOf(false) }
    var geoLocation by remember(editTaskId) { mutableStateOf(initialValues.geoLocation) }
    var showMoreOptions by remember(editTaskId) { mutableStateOf(initialValues.showMoreOptions) }

    val formScrollState = rememberScrollState()

    val saveTaskAction: () -> Unit = {
        val dueDay = selectedDueEpochDay
        val assigneeLabel = if (useCustomAssigneeName && customAssigneeName.isNotBlank()) {
            customAssigneeName.trim()
        } else {
            selectedAssignees.joinToString(", ") { it.shortName }
        }

        val pendingDraftSubtasks = buildList {
            addAll(draftChecklistItems)
            if (draftItemTitle.isNotBlank()) {
                val v = (draftItemVolText.replace(",", ".").toDoubleOrNull() ?: 1.0).coerceAtLeast(0.1)
                add(
                    SubtaskItem(
                        id = "sub_${System.currentTimeMillis()}_${size + 1}",
                        title = draftItemTitle.trim(),
                        isDone = false,
                        targetVolume = v,
                        completedVolume = 0.0,
                        unit = draftItemUnit.trim().ifBlank { "шт" }
                    )
                )
            }
        }

        val fallbackTitle = description.lines().map { it.trim() }.firstOrNull { it.isNotBlank() } ?: ""
        val effectiveTitle = title.trim()
            .ifBlank { fallbackTitle }
            .ifBlank { "Задание — $assigneeLabel (${ReportDocumentGenerator.formatEpochDay(dueDay)})" }

        val resolvedUnitFromTask = if (hasVolumeRequirement) {
            if (isCustomUnitSelected) customUnitText.trim().ifBlank { "шт" } else selectedUnit
        } else {
            pendingDraftSubtasks.firstOrNull()?.effectiveUnit ?: selectedUnit.ifBlank { "шт" }
        }
        val resolvedTargetVolFromTask = if (hasVolumeRequirement) {
            (targetVolumeText.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        } else {
            0.0
        }

        val encodedSubtasks = pendingDraftSubtasks.map { it.encode() }
        val resolvedTargetVol = when {
            pendingDraftSubtasks.isNotEmpty() -> pendingDraftSubtasks.sumOf { it.effectiveTargetVolume }
            resolvedTargetVolFromTask > 0.0 -> resolvedTargetVolFromTask
            else -> 0.0
        }
        val resolvedMinPhotos = (minPhotoCountText.toIntOrNull() ?: 1).coerceAtLeast(1)

        onCreate(
            TaskDraft(
                title = effectiveTitle,
                description = description.trim(),
                assigneeId = selectedAssignee.id,
                assigneeIds = selectedAssignees.map { it.id },
                customAssigneeName = if (useCustomAssigneeName) customAssigneeName.trim() else "",
                priority = selectedPriority,
                dueDateEpochDay = dueDay,
                deadlineTime = deadlineTime.ifBlank { "18:00" },
                requiresPhotoReport = requiresPhoto,
                minPhotoCount = if (requiresPhoto) resolvedMinPhotos else 0,
                requiresApproval = requiresApproval,
                recurrenceRule = selectedRecurrence,
                targetVolume = resolvedTargetVol,
                volumeUnit = resolvedUnitFromTask,
                tags = emptyList(),
                subtasks = encodedSubtasks,
                geoLocation = geoLocation.trim(),
                applyToFutureSeries = applyToFutureSeries
            )
        )
        onDismiss()
    }

    val softFieldBg = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = if (tokens.isDark) 0.40f else 0.50f
    )
    val softPanelBg = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = if (tokens.isDark) 0.24f else 0.34f
    )

    val dueChipLabel = remember(selectedDueEpochDay, currentEpochDay, deadlineTime, selectedRecurrence) {
        val dayPart = when (selectedDueEpochDay) {
            currentEpochDay -> "Сегодня"
            currentEpochDay + 1L -> "Завтра"
            currentEpochDay - 1L -> "Вчера"
            else -> ReportDocumentGenerator.formatEpochDay(selectedDueEpochDay).take(5)
        }
        val base = "$dayPart, ${deadlineTime.ifBlank { "18:00" }}"
        if (selectedRecurrence != RecurrenceRule.NONE) "$base · ${selectedRecurrence.titleRu}" else base
    }

    val assigneeChipLabel = remember(
        activeEmployee,
        useCustomAssigneeName,
        customAssigneeName,
        selectedAssignees
    ) {
        when {
            !activeEmployee.role.canAssignToOtherEmployees() -> "Я (${activeEmployee.shortName})"
            useCustomAssigneeName && customAssigneeName.isNotBlank() -> customAssigneeName.trim().take(18)
            selectedAssignees.size > 1 -> "Исполнители (${selectedAssignees.size})"
            else -> selectedAssignee.shortName
        }
    }

    val photoChipLabel = remember(requiresPhoto, minPhotoCountText) {
        if (requiresPhoto) "${minPhotoCountText.ifBlank { "1" }} фото" else "Без фото"
    }

    val workScopeChipLabel = remember(
        draftChecklistItems.size,
        draftItemTitle,
        hasVolumeRequirement,
        targetVolumeText,
        selectedUnit,
        isCustomUnitSelected,
        customUnitText
    ) {
        val stagesCount = draftChecklistItems.size + if (draftItemTitle.isNotBlank()) 1 else 0
        when {
            stagesCount > 0 -> "Этапы ($stagesCount)"
            hasVolumeRequirement && targetVolumeText.isNotBlank() -> {
                val u = if (isCustomUnitSelected) customUnitText.ifBlank { "шт" } else selectedUnit
                "$targetVolumeText $u"
            }
            else -> "+ Объём / этапы"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEditMode) "Редактировать задачу" else "Новая задача",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (activeEmployee.siteName.isNotBlank()) {
                        Text(
                            text = activeEmployee.siteName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(formScrollState)
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            text = "Что нужно сделать?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    },
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                        unfocusedContainerColor = softFieldBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_task_title_input")
                )

                CreateTaskQuickChipsRow(
                    activeEmployee = activeEmployee,
                    activeSection = activeSection,
                    selectedAssigneesCount = selectedAssignees.size,
                    assigneeChipLabel = assigneeChipLabel,
                    dueChipLabel = dueChipLabel,
                    requiresPhoto = requiresPhoto,
                    photoChipLabel = photoChipLabel,
                    hasScopeData = draftChecklistItems.isNotEmpty() || (hasVolumeRequirement && targetVolumeText.isNotBlank()),
                    workScopeChipLabel = workScopeChipLabel,
                    softFieldBg = softFieldBg,
                    onSelectSection = { section ->
                        activeSection = if (activeSection == section) null else section
                    }
                )

                AnimatedVisibility(visible = activeSection != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = softPanelBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (activeSection) {
                            CreateSheetSection.ASSIGNEES -> CreateTaskAssigneesPanel(
                                activeEmployee = activeEmployee,
                                assignees = assignees,
                                selectedAssigneesCount = selectedAssignees.size,
                                selectedAssigneeIds = selectedAssigneeIds,
                                useCustomAssigneeName = useCustomAssigneeName,
                                onToggleCustomAssigneeName = { useCustomAssigneeName = it },
                                customAssigneeName = customAssigneeName,
                                onCustomAssigneeNameChange = { customAssigneeName = it },
                                itrAssigneeFilterBySubdivision = itrAssigneeFilterBySubdivision,
                                onItrFilterChange = { itrAssigneeFilterBySubdivision = it },
                                assigneeSearchQuery = assigneeSearchQuery,
                                onAssigneeSearchQueryChange = { assigneeSearchQuery = it }
                            )

                            CreateSheetSection.DEADLINE -> CreateTaskDeadlinePanel(
                                currentEpochDay = currentEpochDay,
                                existingTasks = existingTasks,
                                selectedDueEpochDay = selectedDueEpochDay,
                                onSelectDueEpochDay = { selectedDueEpochDay = it },
                                deadlineTime = deadlineTime,
                                onDeadlineTimeChange = { deadlineTime = it },
                                calendarExpanded = calendarExpanded,
                                onToggleCalendar = { calendarExpanded = !calendarExpanded },
                                displayedMonthOffset = displayedMonthOffset,
                                onChangeMonthOffset = { displayedMonthOffset += it }
                            )

                            CreateSheetSection.PHOTO -> CreateTaskPhotoPanel(
                                requiresPhoto = requiresPhoto,
                                onRequiresPhotoChange = { requiresPhoto = it },
                                minPhotoCountText = minPhotoCountText,
                                onMinPhotoCountTextChange = { minPhotoCountText = it },
                                softFieldBg = softFieldBg
                            )

                            CreateSheetSection.WORK_SCOPE -> CreateTaskWorkScopePanel(
                                hasVolumeRequirement = hasVolumeRequirement,
                                onHasVolumeRequirementChange = { hasVolumeRequirement = it },
                                draftChecklistItems = draftChecklistItems,
                                draftItemTitle = draftItemTitle,
                                onDraftItemTitleChange = { draftItemTitle = it },
                                draftItemVolText = draftItemVolText,
                                onDraftItemVolTextChange = { draftItemVolText = it },
                                draftItemUnit = draftItemUnit,
                                onDraftItemUnitChange = { draftItemUnit = it },
                                draftUnitMenuExpanded = draftUnitMenuExpanded,
                                onDraftUnitMenuExpandedChange = { draftUnitMenuExpanded = it },
                                targetVolumeText = targetVolumeText,
                                onTargetVolumeTextChange = { targetVolumeText = it },
                                selectedUnit = selectedUnit,
                                isCustomUnitSelected = isCustomUnitSelected,
                                onSelectStandardUnit = { unitItem ->
                                    isCustomUnitSelected = false
                                    selectedUnit = unitItem
                                },
                                softFieldBg = softFieldBg
                            )

                            null -> Unit
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showMoreOptions = !showMoreOptions }
                        .padding(horizontal = 4.dp, vertical = 10.dp)
                        .testTag("toggle_advanced_options_button"),
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Ещё параметры",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val advancedSummary = buildList {
                                if (description.isNotBlank()) add("есть примечание")
                                if (selectedPriority != TaskPriority.MEDIUM) add(selectedPriority.titleRu.lowercase())
                                if (selectedRecurrence != RecurrenceRule.NONE) add(selectedRecurrence.titleRu.lowercase())
                                if (!requiresApproval) add("без приёмки")
                            }.joinToString(" · ").ifBlank { "Описание, приоритет, участок, повтор" }
                            Text(
                                text = advancedSummary,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = if (showMoreOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = showMoreOptions) {
                    CreateTaskMoreOptionsPanel(
                        description = description,
                        onDescriptionChange = { description = it },
                        geoLocation = geoLocation,
                        onGeoLocationChange = { geoLocation = it },
                        selectedPriority = selectedPriority,
                        onSelectPriority = { selectedPriority = it },
                        selectedRecurrence = selectedRecurrence,
                        onSelectRecurrence = { selectedRecurrence = it },
                        selectedDueEpochDay = selectedDueEpochDay,
                        isEditMode = isEditMode,
                        hasRecurrenceSeries = initialValues.hasRecurrenceSeries,
                        applyToFutureSeries = applyToFutureSeries,
                        onApplyToFutureSeriesChange = { applyToFutureSeries = it },
                        requiresApproval = requiresApproval,
                        onRequiresApprovalChange = { requiresApproval = it },
                        softPanelBg = softPanelBg
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "Отмена",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = saveTaskAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.7f)
                        .height(50.dp)
                        .testTag("confirm_create_task_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditMode) "Сохранить изменения" else "Поставить задачу",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateTaskQuickChipsRow(
    activeEmployee: CorporateEmployee,
    activeSection: CreateSheetSection?,
    selectedAssigneesCount: Int,
    assigneeChipLabel: String,
    dueChipLabel: String,
    requiresPhoto: Boolean,
    photoChipLabel: String,
    hasScopeData: Boolean,
    workScopeChipLabel: String,
    softFieldBg: Color,
    onSelectSection: (CreateSheetSection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val assigneeChipActive = activeSection == CreateSheetSection.ASSIGNEES
        Surface(
            shape = RoundedCornerShape(50),
            color = if (assigneeChipActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
            },
            modifier = Modifier
                .heightIn(min = 38.dp)
                .clip(RoundedCornerShape(50))
                .clickable { onSelectSection(CreateSheetSection.ASSIGNEES) }
                .testTag(
                    if (activeEmployee.role.canAssignToOtherEmployees()) {
                        "assignee_dropdown_field"
                    } else {
                        "self_assignee_readonly_card"
                    }
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (selectedAssigneesCount > 1) Icons.Default.Group else Icons.Default.Person,
                    contentDescription = "Исполнитель",
                    tint = if (assigneeChipActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = assigneeChipLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (assigneeChipActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
        }

        val deadlineChipActive = activeSection == CreateSheetSection.DEADLINE
        Surface(
            shape = RoundedCornerShape(50),
            color = if (deadlineChipActive) MaterialTheme.colorScheme.primary else softFieldBg,
            modifier = Modifier
                .heightIn(min = 38.dp)
                .clip(RoundedCornerShape(50))
                .clickable { onSelectSection(CreateSheetSection.DEADLINE) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = "Срок",
                    tint = if (deadlineChipActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = dueChipLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (deadlineChipActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        val photoChipActive = activeSection == CreateSheetSection.PHOTO
        Surface(
            shape = RoundedCornerShape(50),
            color = when {
                photoChipActive -> MaterialTheme.colorScheme.primary
                requiresPhoto -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                else -> softFieldBg
            },
            modifier = Modifier
                .heightIn(min = 38.dp)
                .clip(RoundedCornerShape(50))
                .clickable { onSelectSection(CreateSheetSection.PHOTO) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Фотоотчёт",
                    tint = when {
                        photoChipActive -> MaterialTheme.colorScheme.onPrimary
                        requiresPhoto -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = photoChipLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        photoChipActive -> MaterialTheme.colorScheme.onPrimary
                        requiresPhoto -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        val scopeChipActive = activeSection == CreateSheetSection.WORK_SCOPE
        Surface(
            shape = RoundedCornerShape(50),
            color = when {
                scopeChipActive -> MaterialTheme.colorScheme.primary
                hasScopeData -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
                else -> softFieldBg
            },
            modifier = Modifier
                .heightIn(min = 38.dp)
                .clip(RoundedCornerShape(50))
                .clickable { onSelectSection(CreateSheetSection.WORK_SCOPE) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FormatListBulleted,
                    contentDescription = "Этапы и объём",
                    tint = when {
                        scopeChipActive -> MaterialTheme.colorScheme.onPrimary
                        hasScopeData -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = workScopeChipLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        scopeChipActive -> MaterialTheme.colorScheme.onPrimary
                        hasScopeData -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun CreateTaskAssigneesPanel(
    activeEmployee: CorporateEmployee,
    assignees: List<CorporateEmployee>,
    selectedAssigneesCount: Int,
    selectedAssigneeIds: SnapshotStateList<String>,
    useCustomAssigneeName: Boolean,
    onToggleCustomAssigneeName: (Boolean) -> Unit,
    customAssigneeName: String,
    onCustomAssigneeNameChange: (String) -> Unit,
    itrAssigneeFilterBySubdivision: Boolean,
    onItrFilterChange: (Boolean) -> Unit,
    assigneeSearchQuery: String,
    onAssigneeSearchQueryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!activeEmployee.role.canAssignToOtherEmployees()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = activeEmployee.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${activeEmployee.position} • Назначение себе",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedAssigneesCount > 1) {
                        "Выбрано: $selectedAssigneesCount"
                    } else {
                        "Исполнители"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!useCustomAssigneeName && assignees.size > 1) {
                        TextButton(
                            onClick = {
                                if (selectedAssigneeIds.size == assignees.size) {
                                    selectedAssigneeIds.clear()
                                    selectedAssigneeIds.add(assignees.first().id)
                                } else {
                                    selectedAssigneeIds.clear()
                                    selectedAssigneeIds.addAll(assignees.map { it.id })
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("select_all_assignees_button")
                        ) {
                            Text(
                                text = if (selectedAssigneeIds.size == assignees.size) "Сбросить" else "Все (${assignees.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    TextButton(
                        onClick = { onToggleCustomAssigneeName(!useCustomAssigneeName) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag(if (useCustomAssigneeName) "assignee_mode_dropdown_chip" else "custom_assignee_chip")
                    ) {
                        Text(
                            text = if (useCustomAssigneeName) "Из списка" else "+ ФИО",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (activeEmployee.role == CorporateRole.ITR && !useCustomAssigneeName) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = itrAssigneeFilterBySubdivision,
                        onClick = { onItrFilterChange(true) },
                        label = { Text("Моё подразделение", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("create_itr_assignees_subdivision_chip")
                    )
                    FilterChip(
                        selected = !itrAssigneeFilterBySubdivision,
                        onClick = { onItrFilterChange(false) },
                        label = { Text("Весь участок", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("create_itr_assignees_all_chip")
                    )
                }
            }

            if (useCustomAssigneeName) {
                TextField(
                    value = customAssigneeName,
                    onValueChange = onCustomAssigneeNameChange,
                    placeholder = { Text("ФИО через запятую...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_assignee_name_input")
                )
            } else {
                if (assignees.size > 5) {
                    TextField(
                        value = assigneeSearchQuery,
                        onValueChange = onAssigneeSearchQueryChange,
                        placeholder = { Text("Поиск сотрудника...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                val filteredAssignees = remember(assignees, assigneeSearchQuery) {
                    val q = assigneeSearchQuery.trim()
                    if (q.isEmpty()) assignees
                    else assignees.filter {
                        it.fullName.contains(q, ignoreCase = true) ||
                            it.position.contains(q, ignoreCase = true)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 190.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filteredAssignees.forEach { emp ->
                        val isChecked = selectedAssigneeIds.contains(emp.id)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChecked) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isChecked) {
                                        if (selectedAssigneeIds.size > 1) {
                                            selectedAssigneeIds.remove(emp.id)
                                        }
                                    } else {
                                        selectedAssigneeIds.add(emp.id)
                                    }
                                    onToggleCustomAssigneeName(false)
                                }
                                .testTag("quick_assignee_chip_${emp.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(checked = isChecked, onCheckedChange = null)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = emp.fullName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${emp.position} • ${emp.role.titleRu}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    }
}

@Composable
private fun CreateTaskDeadlinePanel(
    currentEpochDay: Long,
    existingTasks: List<TaskDocument>,
    selectedDueEpochDay: Long,
    onSelectDueEpochDay: (Long) -> Unit,
    deadlineTime: String,
    onDeadlineTimeChange: (String) -> Unit,
    calendarExpanded: Boolean,
    onToggleCalendar: () -> Unit,
    displayedMonthOffset: Int,
    onChangeMonthOffset: (Int) -> Unit
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
            Text(
                text = ReportDocumentGenerator.formatEpochDay(selectedDueEpochDay),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onToggleCalendar,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(30.dp)
                    .testTag("create_task_calendar_expand_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (calendarExpanded) "Свернуть" else "Месяц",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        val stripDays = remember(currentEpochDay) {
            (0L..10L).map { currentEpochDay + it }
        }
        val dayNamesMondayFirst = remember {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stripDays.forEach { dayEpoch ->
                val isSelected = dayEpoch == selectedDueEpochDay
                val isToday = dayEpoch == currentEpochDay
                val localDate = remember(dayEpoch) { LocalDate.ofEpochDay(dayEpoch) }
                val dow = dayNamesMondayFirst[(localDate.dayOfWeek.value - 1).coerceIn(0, 6)]
                val tasksCountOnDay = remember(existingTasks, dayEpoch) {
                    existingTasks.count { it.dueDateEpochDay == dayEpoch }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier
                        .width(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectDueEpochDay(dayEpoch) }
                        .testTag("date_pill_$dayEpoch")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = dow,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = localDate.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (tasksCountOnDay > 0) {
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.primary
                                    } else Color.Transparent
                                )
                        )
                    }
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
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            val timePresets = remember(deadlineTime) {
                (listOf("09:00", "12:00", "16:00", "18:00", "20:00") +
                    listOfNotNull(deadlineTime.takeIf { it.isNotBlank() })).distinct()
            }
            timePresets.forEach { tPreset ->
                val selTime = deadlineTime == tPreset
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onDeadlineTimeChange(tPreset) }
                ) {
                    Text(
                        text = tPreset,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selTime) FontWeight.Bold else FontWeight.Medium,
                        color = if (selTime) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = calendarExpanded) {
            val displayedYearMonth = remember(currentEpochDay, displayedMonthOffset) {
                val baseDate = LocalDate.ofEpochDay(currentEpochDay)
                YearMonth.of(baseDate.year, baseDate.monthValue).plusMonths(displayedMonthOffset.toLong())
            }
            val ruLocale = remember { Locale.forLanguageTag("ru-RU") }
            val monthTitle = remember(displayedYearMonth, ruLocale) {
                val mName = displayedYearMonth.month
                    .getDisplayName(TextStyle.FULL_STANDALONE, ruLocale)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(ruLocale) else it.toString() }
                "$mName ${displayedYearMonth.year}"
            }
            val daysInMonth = displayedYearMonth.lengthOfMonth()
            val firstOfMonth = displayedYearMonth.atDay(1)
            val firstDayWeekOffset = firstOfMonth.dayOfWeek.value - 1
            val baseMonthStartEpochDay = firstOfMonth.toEpochDay()

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
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
                        IconButton(
                            onClick = { onChangeMonthOffset(-1) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий месяц")
                        }
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { onChangeMonthOffset(1) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Следующий месяц")
                        }
                    }
                    val totalCells = firstDayWeekOffset + daysInMonth
                    val rowsCount = (totalCells + 6) / 7
                    for (r in 0 until rowsCount) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (c in 0 until 7) {
                                val cellIdx = r * 7 + c
                                val dayNum = cellIdx - firstDayWeekOffset + 1
                                if (dayNum in 1..daysInMonth) {
                                    val cellEpochDay = baseMonthStartEpochDay + (dayNum - 1)
                                    val isSel = cellEpochDay == selectedDueEpochDay
                                    val isTodayCell = cellEpochDay == currentEpochDay
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .padding(1.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSel -> MaterialTheme.colorScheme.primary
                                                    isTodayCell -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { onSelectDueEpochDay(cellEpochDay) }
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSel || isTodayCell) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).height(34.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateTaskPhotoPanel(
    requiresPhoto: Boolean,
    onRequiresPhotoChange: (Boolean) -> Unit,
    minPhotoCountText: String,
    onMinPhotoCountTextChange: (String) -> Unit,
    softFieldBg: Color
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Обязательный фотоотчёт",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (requiresPhoto) "Нужно приложить фото при сдаче" else "Сдача без обязательной фотофиксации",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = requiresPhoto, onCheckedChange = onRequiresPhotoChange)
        }

        if (requiresPhoto) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Минимум фото",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = softFieldBg,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                                val cur = minPhotoCountText.toIntOrNull() ?: 1
                                onMinPhotoCountTextChange((cur - 1).coerceAtLeast(1).toString())
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, contentDescription = "Уменьшить", modifier = Modifier.size(16.dp))
                        }
                    }
                    BasicTextField(
                        value = minPhotoCountText,
                        onValueChange = { valDigits ->
                            onMinPhotoCountTextChange(valDigits.filter { it.isDigit() }.take(2))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .width(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(softFieldBg)
                            .padding(vertical = 6.dp)
                            .testTag("min_photo_manual_input")
                    )
                    Surface(
                        shape = CircleShape,
                        color = softFieldBg,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                                val cur = minPhotoCountText.toIntOrNull() ?: 1
                                onMinPhotoCountTextChange((cur + 1).coerceAtMost(99).toString())
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Увеличить", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateTaskWorkScopePanel(
    hasVolumeRequirement: Boolean,
    onHasVolumeRequirementChange: (Boolean) -> Unit,
    draftChecklistItems: SnapshotStateList<SubtaskItem>,
    draftItemTitle: String,
    onDraftItemTitleChange: (String) -> Unit,
    draftItemVolText: String,
    onDraftItemVolTextChange: (String) -> Unit,
    draftItemUnit: String,
    onDraftItemUnitChange: (String) -> Unit,
    draftUnitMenuExpanded: Boolean,
    onDraftUnitMenuExpandedChange: (Boolean) -> Unit,
    targetVolumeText: String,
    onTargetVolumeTextChange: (String) -> Unit,
    selectedUnit: String,
    isCustomUnitSelected: Boolean,
    onSelectStandardUnit: (String) -> Unit,
    softFieldBg: Color
) {
    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(false to "Этапы чек-листа", true to "Единый объём").forEach { (isSingleVol, label) ->
                    val sel = hasVolumeRequirement == isSingleVol
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (sel) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onHasVolumeRequirementChange(isSingleVol) }
                            .then(
                                if (isSingleVol) Modifier.testTag("toggle_volume_requirement_switch")
                                else Modifier
                            )
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                            color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        if (!hasVolumeRequirement) {
            draftChecklistItems.forEachIndexed { idx, item ->
                var rowQtyText by remember(item.id) {
                    mutableStateOf(SubtaskItem.formatVolumeNumber(item.effectiveTargetVolume))
                }
                var rowUnitExpanded by remember(item.id) { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}.",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        BasicTextField(
                            value = item.title,
                            onValueChange = { newTitle ->
                                draftChecklistItems[idx] = item.copy(title = newTitle)
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(softFieldBg)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            BasicTextField(
                                value = rowQtyText,
                                onValueChange = { newVal ->
                                    rowQtyText = newVal
                                    val parsed = newVal.replace(",", ".").toDoubleOrNull()
                                    if (parsed != null && parsed > 0.0) {
                                        draftChecklistItems[idx] = item.copy(targetVolume = parsed)
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .width(38.dp)
                                    .testTag("draft_subtask_qty_row_$idx")
                            )
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { rowUnitExpanded = true }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.effectiveUnit,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = rowUnitExpanded,
                                    onDismissRequest = { rowUnitExpanded = false }
                                ) {
                                    STANDARD_VOLUME_UNITS.forEach { uOpt ->
                                        DropdownMenuItem(
                                            text = { Text(uOpt) },
                                            onClick = {
                                                draftChecklistItems[idx] = item.copy(unit = uOpt)
                                                rowUnitExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(
                            onClick = { draftChecklistItems.removeAt(idx) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Удалить",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (draftItemTitle.isEmpty()) {
                            Text(
                                text = "Название этапа...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            )
                        }
                        BasicTextField(
                            value = draftItemTitle,
                            onValueChange = onDraftItemTitleChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("draft_subtask_title_input")
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(softFieldBg)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        BasicTextField(
                            value = draftItemVolText,
                            onValueChange = onDraftItemVolTextChange,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .width(38.dp)
                                .testTag("draft_subtask_volume_input")
                        )
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onDraftUnitMenuExpandedChange(true) }
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                    .testTag("draft_subtask_unit_dropdown")
                            ) {
                                Text(
                                    text = draftItemUnit,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = draftUnitMenuExpanded,
                                onDismissRequest = { onDraftUnitMenuExpandedChange(false) }
                            ) {
                                STANDARD_VOLUME_UNITS.forEach { uItem ->
                                    DropdownMenuItem(
                                        text = { Text(uItem) },
                                        onClick = {
                                            onDraftItemUnitChange(uItem)
                                            onDraftUnitMenuExpandedChange(false)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = {
                    if (draftItemTitle.isNotBlank()) {
                        val v = (draftItemVolText.replace(",", ".").toDoubleOrNull() ?: 1.0).coerceAtLeast(0.1)
                        val u = draftItemUnit.trim().ifBlank { "шт" }
                        draftChecklistItems.add(
                            SubtaskItem(
                                id = "sub_${System.currentTimeMillis()}_${draftChecklistItems.size + 1}",
                                title = draftItemTitle.trim(),
                                isDone = false,
                                targetVolume = v,
                                completedVolume = 0.0,
                                unit = u
                            )
                        )
                        onDraftItemTitleChange("")
                    }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("add_draft_subtask_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Добавить этап", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        } else {
            TextField(
                value = targetVolumeText,
                onValueChange = onTargetVolumeTextChange,
                label = { Text("Плановый объём работ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_task_target_volume_input")
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                STANDARD_VOLUME_UNITS.forEach { unitItem ->
                    FilterChip(
                        selected = !isCustomUnitSelected && selectedUnit == unitItem,
                        onClick = { onSelectStandardUnit(unitItem) },
                        label = { Text(unitItem) },
                        modifier = Modifier.testTag("unit_chip_$unitItem")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateTaskMoreOptionsPanel(
    description: String,
    onDescriptionChange: (String) -> Unit,
    geoLocation: String,
    onGeoLocationChange: (String) -> Unit,
    selectedPriority: TaskPriority,
    onSelectPriority: (TaskPriority) -> Unit,
    selectedRecurrence: RecurrenceRule,
    onSelectRecurrence: (RecurrenceRule) -> Unit,
    selectedDueEpochDay: Long,
    isEditMode: Boolean,
    hasRecurrenceSeries: Boolean,
    applyToFutureSeries: Boolean,
    onApplyToFutureSeriesChange: (Boolean) -> Unit,
    requiresApproval: Boolean,
    onRequiresApprovalChange: (Boolean) -> Unit,
    softPanelBg: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(softPanelBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextField(
            value = description,
            onValueChange = onDescriptionChange,
            placeholder = { Text("Описание, тех. требования, отметки...") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_task_desc_input")
        )

        TextField(
            value = geoLocation,
            onValueChange = onGeoLocationChange,
            label = { Text("Участок / зона на объекте") },
            leadingIcon = {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Приоритет",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TaskPriority.entries.forEach { prio ->
                        val isSel = selectedPriority == prio
                        val pColor = priorityColor(prio)
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (isSel) pColor.copy(alpha = 0.14f) else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { onSelectPriority(prio) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(pColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = prio.titleRu,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) pColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Повторяемость",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    RecurrenceRule.NONE,
                    RecurrenceRule.DAILY,
                    RecurrenceRule.WEEKDAYS,
                    RecurrenceRule.WEEKLY,
                    RecurrenceRule.MONTHLY
                ).forEach { rule ->
                    FilterChip(
                        selected = selectedRecurrence == rule,
                        onClick = { onSelectRecurrence(rule) },
                        label = { Text(rule.titleRu, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("recurrence_chip_${rule.code}")
                    )
                }
            }
            if (selectedRecurrence != RecurrenceRule.NONE) {
                val scheduledDays = remember(selectedRecurrence, selectedDueEpochDay) {
                    selectedRecurrence.generateOccurrenceEpochDays(selectedDueEpochDay)
                }
                Text(
                    text = "Будет добавлено в календарь: ${scheduledDays.size} дн. (${ReportDocumentGenerator.formatEpochDay(scheduledDays.first())} – ${ReportDocumentGenerator.formatEpochDay(scheduledDays.last())})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            if (isEditMode && hasRecurrenceSeries) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Применить к будущим задачам серии",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Обновит все неутверждённые задачи серии от выбранной даты",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = applyToFutureSeries,
                        onCheckedChange = onApplyToFutureSeriesChange,
                        modifier = Modifier.testTag("edit_apply_future_series_switch")
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Требует подтверждения (приёмки)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "После выполнения уйдёт на проверку",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = requiresApproval, onCheckedChange = onRequiresApprovalChange)
        }
    }
}
