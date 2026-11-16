package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Calendar
import java.util.Locale
import com.example.data.model.CorporateEmployee
import com.example.data.model.PhotoReportItem
import com.example.data.model.RecurrenceRule
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskDocument
import com.example.data.model.TaskDraft
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.seed.CorporateSeedData
import com.example.domain.reporting.ReportDocumentGenerator
import com.example.ui.theme.LocalAppThemeTokens

val STANDARD_VOLUME_UNITS = listOf("шт", "кг", "м²", "м³", "п.м.", "т", "л", "компл.", "ч")

@Composable
fun statusColor(status: TaskStatus): Color {
    val tokens = LocalAppThemeTokens.current
    return when (status) {
        TaskStatus.NEW -> MaterialTheme.colorScheme.primary
        TaskStatus.IN_PROGRESS -> tokens.statusInProgressColor
        TaskStatus.ON_REVIEW -> if (tokens.isHighContrastField) Color(0xFF38BDF8) else Color(0xFF7C3AED)
        TaskStatus.APPROVED -> tokens.statusDoneColor
        TaskStatus.REJECTED -> Color(0xFFDC2626)
        TaskStatus.OVERDUE -> Color(0xFFB91C1C)
        TaskStatus.CANCELLED -> Color(0xFF64748B)
        TaskStatus.ARCHIVED -> Color(0xFF475569)
    }
}

@Composable
fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.LOW -> Color(0xFF64748B)
    TaskPriority.MEDIUM -> Color(0xFF2563EB)
    TaskPriority.HIGH -> Color(0xFFD97706)
    TaskPriority.CRITICAL -> Color(0xFFDC2626)
    TaskPriority.EMERGENCY -> Color(0xFF991B1B)
}

/**
 * Компактная адаптивная карточка задачи с отображением исполнителя, чек-листа и объёма выполнения.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskItemCard(
    task: TaskDocument,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onToggleSubtask: ((String) -> Unit)? = null,
    expectedXp: Int? = null,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAppThemeTokens.current
    val isApproved = task.taskStatus == TaskStatus.APPROVED
    val textDeco = if (isApproved) TextDecoration.LineThrough else TextDecoration.None
    val cardAlpha = if (isApproved) 0.68f else 1f
    val subtasks = remember(task.subtaskItems) { task.decodedSubtasks() }
    val doneSubtasks = subtasks.count { it.isDone }
    val targetVol = task.effectiveTargetVolume
    val completedVol = task.effectiveCompletedVolume
    val unitLabel = task.effectiveVolumeUnit.ifBlank { "шт" }
    val pct = task.completionPercent()
    val doneColor = tokens.statusDoneColor

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable(onClick = onClick)
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = tokens.cardShadowElevationDp.dp,
        border = if (tokens.cardBorderWidthDp > 0f) {
            BorderStroke(tokens.cardBorderWidthDp.dp, tokens.cardBorderColor)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(textDecoration = textDeco),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (expectedXp != null && expectedXp > 0 && !task.taskStatus.isTerminalOrDone) {
                        Surface(
                            color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "+$expectedXp XP",
                                color = Color(0xFFB45309),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Surface(
                        color = statusColor(task.taskStatus).copy(alpha = if (tokens.isDark) 0.16f else 0.10f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = task.taskStatus.titleRu,
                            color = statusColor(task.taskStatus),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = textDeco),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Прогресс: чистый текст («Выполнено X из Y» или «Объём») + ультратонкая линия (2.5dp)
                if (targetVol > 0.0 || subtasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val summaryLabel = when {
                        targetVol > 0.0 -> "Объём: ${SubtaskItem.formatVolumeNumber(completedVol)} / ${SubtaskItem.formatVolumeNumber(targetVol)} $unitLabel"
                        subtasks.isNotEmpty() -> "Чек-лист: $doneSubtasks из ${subtasks.size} выполнено"
                        else -> ""
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (summaryLabel.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = summaryLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (pct > 0) {
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pct >= 100) doneColor else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        LinearProgressIndicator(
                            progress = { (pct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (pct >= 100) doneColor else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Метаданные мелким серым текстом (исполнитель, дата, фото)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.assigneeName.ifBlank { "Исполнитель" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (task.requiresPhotoReport || task.photoItems.isNotEmpty()) {
                            val photoReady = task.hasEnoughPhotos()
                            Text(
                                text = "📷 ${task.photoItems.size}/${task.minPhotoCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (photoReady) doneColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${ReportDocumentGenerator.formatEpochDay(task.dueDateEpochDay)} • до ${task.deadlineTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!task.rejectionReason.isNullOrBlank() && task.taskStatus == TaskStatus.REJECTED) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Причина отклонения: ${task.rejectionReason}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (task.taskStatus == TaskStatus.APPROVED && !task.approvedByName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Подтвердил: ${task.approvedByName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = doneColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(36.dp)
                        .testTag("delete_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Удалить задачу",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Диалог детального просмотра задачи:
 * - Полностью адаптирован под любые размеры экранов телефонов (фиксированная шапка и нижняя панель кнопок).
 * - Отчётность персонала по объёму выполнения в чек-листе (100% = выполнено).
 * - Непрерывная съёмка с камеры, фотоштамп СМР, полноэкранный просмотр и удаление фото.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailDialog(
    task: TaskDocument,
    activeEmployee: CorporateEmployee,
    availableEmployees: List<CorporateEmployee> = listOf(activeEmployee),
    onDismiss: () -> Unit,
    onAddPhoto: (uriString: String, gpsCoords: String, comment: String) -> Unit,
    onAddPhotosBatch: (items: List<Pair<String, String>>, gpsCoords: String) -> Unit = { items, gps ->
        items.forEach { (uri, comment) -> onAddPhoto(uri, gps, comment) }
    },
    onPhotoCaptureError: (String) -> Unit = {},
    onDeletePhoto: (photoId: String) -> Unit = {},
    onToggleSubtask: (subtaskId: String) -> Unit,
    onUpdateSubtaskVolume: (subtaskId: String, completedVol: Double) -> Unit = { _, _ -> },
    onUpdateSubtaskDetails: (subtaskId: String, targetVol: Double, completedVol: Double, unit: String) -> Unit = { _, _, _, _ -> },
    onUpdateTaskVolume: (completedVol: Double) -> Unit = {},
    onUpdateAssignee: (assigneeId: String, customAssigneeName: String) -> Unit = { _, _ -> },
    onUpdateAssignees: (assigneeIds: List<String>, customAssigneeName: String) -> Unit = { ids, custom ->
        onUpdateAssignee(ids.firstOrNull().orEmpty(), custom)
    },
    onAddSubtask: (title: String, targetVolume: Double, unit: String) -> Unit = { _, _, _ -> },
    onDeleteSubtask: (subtaskId: String) -> Unit = {},
    onAddComment: (text: String, mentionedUser: String) -> Unit,
    onSubmitCompletion: (note: String) -> Unit,
    onSubmitWorkerVolumeReport: (manualCompletedVol: Double?, note: String) -> Unit = { _, note ->
        onSubmitCompletion(note)
    },
    onApproveReport: (comment: String) -> Unit,
    onRejectReport: (reason: String) -> Unit,
    onArchiveTask: () -> Unit,
    onCancelTask: () -> Unit = {},
    onDeleteTask: () -> Unit = {},
    onEditTask: () -> Unit = {},
    expectedXp: Int? = null,
    cloudPhotoCacheVersion: Int = 0,
    isUploadingPhotosToCloud: Boolean = false,
    isHydratingCloudPhotos: Boolean = false,
    hasCloudPhotoSyncError: Boolean = false,
    onRetryCloudSync: () -> Unit = {},
    onRequestCloudPhotosHydration: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(task.id, task.photoItems.size) {
        onRequestCloudPhotosHydration()
    }
    var photoComment by remember { mutableStateOf("") }
    val photoGps = remember(task.geoLocation) { task.geoLocation ?: "Участок №1" }
    val canReassignTask = activeEmployee.role.canAssignToOtherEmployees()
    val canEditThisTask = remember(task, activeEmployee) {
        task.canBeEditedBy(activeEmployee)
    }
    val canDeleteThisTask = activeEmployee.role != com.example.data.model.CorporateRole.WORKER ||
        task.canBeDeletedBy(activeEmployee)
    var detailItrAssigneeFilterBySubdivision by remember {
        mutableStateOf(activeEmployee.role == com.example.data.model.CorporateRole.ITR)
    }
    val assignees = remember(availableEmployees, activeEmployee, canReassignTask, detailItrAssigneeFilterBySubdivision) {
        if (!canReassignTask) {
            listOf(activeEmployee)
        } else {
            val candidates = availableEmployees
                .filter { activeEmployee.role.canAssignTo(it.role) }
                .ifEmpty { listOf(activeEmployee) }

            if (activeEmployee.role == com.example.data.model.CorporateRole.ITR && detailItrAssigneeFilterBySubdivision) {
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
    var showEditAssigneeSection by remember { mutableStateOf(false) }
    var detailUseManualAssignee by remember { mutableStateOf(false) }
    var detailManualAssigneeText by remember(task.assigneeName) { mutableStateOf("") }
    var detailAssigneeDropdownExpanded by remember { mutableStateOf(false) }
    val detailSelectedAssigneeIds = remember(task.id, task.tags, task.assigneeId, assignees) {
        val initialIds = assignees.filter { task.isAssignedToEmployee(it) }.map { it.id }
            .ifEmpty { task.allAssigneeIds }
        mutableStateListOf<String>().apply { addAll(initialIds.distinct()) }
    }

    // Поля для добавления нового пункта чек-листа в новой строке (текст + ячейка количества + кнопка "+")
    var newSubtaskTitle by remember { mutableStateOf("") }
    var newSubtaskTargetVolText by remember { mutableStateOf("1") }
    var newSubtaskUnit by remember(task.effectiveVolumeUnit) {
        mutableStateOf(task.effectiveVolumeUnit.ifBlank { "шт" })
    }

    // Поле для ручного ввода работником выполненного объёма от всего объёма задачи
    val targetVol = task.effectiveTargetVolume
    val completedVol = task.effectiveCompletedVolume
    val unitLabel = task.effectiveVolumeUnit.ifBlank { "шт" }
    var taskCompletedVolInput by remember(task.id, completedVol) {
        mutableStateOf(if (completedVol > 0.0) SubtaskItem.formatVolumeNumber(completedVol) else "")
    }

    var newCommentText by remember { mutableStateOf("") }
    var supervisorNote by remember { mutableStateOf("") }
    var rejectionReasonInput by remember { mutableStateOf("") }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showRejectedOutcomeDialog by remember { mutableStateOf(false) }
    var submittedRejectionComment by remember { mutableStateOf("") }
    var showApprovedOutcomeDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showExtraSections by remember { mutableStateOf(false) }

    // Реальная фотосъёмка (через FileProvider + системная камера) и выбор реальных фото из галереи
    var continuousShootingMode by remember { mutableStateOf(false) }
    var burstShotCount by remember { mutableIntStateOf(0) }
    var isProcessingPhoto by remember { mutableStateOf(false) }
    var isExportingPhotosPdf by remember { mutableStateOf(false) }
    var photoStatusError by remember { mutableStateOf<String?>(null) }
    var pendingCameraFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPhotoForPreview by remember { mutableStateOf<PhotoReportItem?>(null) }
    var launchNextBurstShot by remember { mutableStateOf<(() -> Unit)?>(null) }

    val fallbackCameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val commentStr = photoComment.trim().ifBlank { "Фотофиксация #${task.photoItems.size + 1}" }
            coroutineScope.launch {
                isProcessingPhoto = true
                try {
                    val savedPath = withContext(Dispatchers.IO) {
                        PhotoCaptureUtils.saveCameraBitmapWithStamp(
                            context = context,
                            sourceBitmap = bitmap,
                            taskTitle = task.title,
                            authorName = activeEmployee.shortName,
                            gpsCoords = photoGps,
                            comment = commentStr
                        )
                    }
                    photoStatusError = null
                    onAddPhoto(savedPath, photoGps, commentStr)
                    photoComment = ""
                    burstShotCount++
                } finally {
                    isProcessingPhoto = false
                }
            }
        } else {
            val msg = "Снимок с камеры не получен"
            photoStatusError = msg
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingCameraFilePath
        pendingCameraFilePath = null
        val file = path?.let { File(it) }
        if (success && file != null && file.exists() && file.length() > 0L) {
            val nextIdx = task.photoItems.size + 1
            val effectiveComment = photoComment.trim().ifBlank { "Фотофиксация #$nextIdx" }
            coroutineScope.launch {
                isProcessingPhoto = true
                val savedPath = try {
                    withContext(Dispatchers.IO) {
                        PhotoCaptureUtils.saveCameraFileWithStamp(
                            context = context,
                            capturedFile = file,
                            taskTitle = task.title,
                            authorName = activeEmployee.shortName,
                            gpsCoords = photoGps,
                            comment = effectiveComment
                        )
                    }
                } finally {
                    isProcessingPhoto = false
                }
                if (savedPath != null) {
                    photoStatusError = null
                    onAddPhoto(savedPath, photoGps, effectiveComment)
                    photoComment = ""
                    burstShotCount++
                    if (continuousShootingMode) {
                        launchNextBurstShot?.invoke()
                    }
                } else {
                    val msg = "Не удалось обработать снимок с камеры"
                    photoStatusError = msg
                    onPhotoCaptureError(msg)
                }
            }
        } else {
            file?.let { runCatching { it.delete() } }
        }
    }

    val openNativeCamera: () -> Unit = {
        photoStatusError = null
        runCatching {
            val tempFile = PhotoCaptureUtils.createTempCameraCaptureFile(context)
            val tempUri = PhotoCaptureUtils.getFileProviderUri(context, tempFile)
            pendingCameraFilePath = tempFile.absolutePath
            takePictureLauncher.launch(tempUri)
        }.onFailure {
            pendingCameraFilePath = null
            runCatching {
                fallbackCameraPreviewLauncher.launch(null)
            }.onFailure {
                val msg = "Камера недоступна на этом устройстве. Выберите фото из галереи."
                photoStatusError = msg
                onPhotoCaptureError(msg)
            }
        }
    }
    launchNextBurstShot = openNativeCamera

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            photoStatusError = null
            val baseIndex = task.photoItems.size + 1
            val customBaseComment = photoComment.trim()
            coroutineScope.launch {
                isProcessingPhoto = true
                val (savedBatch, failedCount) = try {
                    withContext(Dispatchers.IO) {
                        val batch = mutableListOf<Pair<String, String>>()
                        var failed = 0
                        uris.forEachIndexed { idx, uri ->
                            val itemComment = if (customBaseComment.isNotBlank()) {
                                if (uris.size > 1) "$customBaseComment (${idx + 1})" else customBaseComment
                            } else {
                                "Фотофиксация #${baseIndex + idx}"
                            }
                            val savedPath = PhotoCaptureUtils.savePickedUriWithStamp(
                                context = context,
                                contentUri = uri,
                                taskTitle = task.title,
                                authorName = activeEmployee.shortName,
                                gpsCoords = photoGps,
                                comment = itemComment
                            )
                            if (savedPath != null) {
                                batch.add(savedPath to itemComment)
                            } else {
                                failed++
                            }
                        }
                        batch to failed
                    }
                } finally {
                    isProcessingPhoto = false
                }
                if (savedBatch.isNotEmpty()) {
                    onAddPhotosBatch(savedBatch, photoGps)
                    photoComment = ""
                }
                if (failedCount > 0) {
                    val msg = if (savedBatch.isEmpty()) {
                        "Не удалось загрузить выбранное фото"
                    } else {
                        "Загружено ${savedBatch.size} фото, не удалось прочитать: $failedCount"
                    }
                    photoStatusError = msg
                    onPhotoCaptureError(msg)
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openNativeCamera()
        } else {
            val msg = "Доступ к камере запрещён. Выберите реальное фото из галереи или разрешите камеру в настройках."
            photoStatusError = msg
            onPhotoCaptureError(msg)
        }
    }

    val tokens = LocalAppThemeTokens.current
    val assigneeRole = remember(task, availableEmployees, activeEmployee) {
        task.resolveAssigneeRole(availableEmployees, activeEmployee)
    }
    val canApproveThisTask = remember(task, activeEmployee, availableEmployees) {
        task.canBeApprovedBy(activeEmployee, availableEmployees)
    }
    val isApproved = task.taskStatus == TaskStatus.APPROVED
    val textDeco = if (isApproved) TextDecoration.LineThrough else TextDecoration.None
    val completionPct = task.completionPercent()
    val isVolume100 = task.isVolumeCompleted100Percent()
    val emeraldPrimary = Color(0xFF10B981)
    val mutedAmber = Color(0xFFF59E0B)
    val crimsonSecondary = Color(0xFFEF4444)

    var showChecklistInfoTooltip by remember { mutableStateOf(false) }
    var showAddSubtaskComposer by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val softFieldBg = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = if (tokens.isDark) 0.38f else 0.48f
    )
    val softPanelBg = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = if (tokens.isDark) 0.24f else 0.34f
    )

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
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sheetMaxHeight = if (constraints.hasBoundedHeight) {
                (maxHeight * 0.92f).coerceAtLeast(380.dp)
            } else {
                720.dp
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight)
                    .imePadding()
            ) {
                // =========================================================================
                // 1. МИНИМАЛИСТИЧНАЯ ШАПКА
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val badgeAccent = when (task.taskStatus) {
                            TaskStatus.APPROVED -> emeraldPrimary
                            TaskStatus.REJECTED, TaskStatus.OVERDUE -> crimsonSecondary
                            else -> mutedAmber
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = badgeAccent.copy(alpha = 0.13f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(badgeAccent)
                                    )
                                    Text(
                                        text = "${task.taskStatus.titleRu} · ${task.taskPriority.titleRu}",
                                        color = badgeAccent,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (expectedXp != null && expectedXp > 0 && task.taskStatus != TaskStatus.CANCELLED) {
                                Surface(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "+$expectedXp XP",
                                        color = Color(0xFFB45309),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (canEditThisTask) {
                                IconButton(
                                    onClick = onEditTask,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("dialog_edit_task_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Редактировать задачу",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (canDeleteThisTask) {
                                IconButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("dialog_delete_task_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Удалить задачу",
                                        tint = if (showDeleteConfirm) crimsonSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineSmall.copy(textDecoration = textDeco),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = textDeco),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // =========================================================================
                // 2. ПРОКРУЧИВАЕМОЕ ТЕЛО КАРТОЧКИ
                // =========================================================================
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Баннер причины отклонения (мягкая тональная подложка без жёсткой рамки)
                    if (!task.rejectionReason.isNullOrBlank()) {
                        Surface(
                            color = crimsonSecondary.copy(alpha = 0.09f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_detail_rejection_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(crimsonSecondary)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "Возвращено на доработку",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = crimsonSecondary
                                    )
                                    Text(
                                        text = task.rejectionReason,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Баннер подтверждения приёмки
                    if (task.taskStatus == TaskStatus.APPROVED && !task.approvedByName.isNullOrBlank()) {
                        Surface(
                            color = emeraldPrimary.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = emeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Принято: ${task.approvedByName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = emeraldPrimary
                                )
                            }
                        }
                    }

                    // =====================================================================
                    // КОМПАКТНАЯ КАРТОЧКА МЕТАДАННЫХ (Исполнители, Срок, Объём, Участок)
                    // =====================================================================
                    Surface(
                        color = softPanelBg,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Исполнитель + кнопка смены
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val resolvedNames = task.resolveDisplayAssigneeNames(availableEmployees)
                                    val resolvedJoined = task.resolveDisplayAssigneeName(availableEmployees)
                                    val initials = resolvedJoined
                                        .split(" ", ",")
                                        .filter { it.isNotBlank() }
                                        .take(2)
                                        .joinToString("") { it.take(1).uppercase() }
                                        .ifBlank { "ИС" }
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (resolvedNames.size > 1) "${resolvedNames.size}" else initials,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = if (resolvedNames.size > 1) {
                                                "Исполнители (${resolvedNames.size})"
                                            } else {
                                                "Исполнитель"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = resolvedJoined.ifBlank { "Не назначен" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (canReassignTask) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (showEditAssigneeSection) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { showEditAssigneeSection = !showEditAssigneeSection }
                                            .testTag("toggle_edit_task_assignee_button")
                                    ) {
                                        Text(
                                            text = if (showEditAssigneeSection) "Скрыть" else "Изменить",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (showEditAssigneeSection) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = canReassignTask && showEditAssigneeSection) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (activeEmployee.role == com.example.data.model.CorporateRole.ITR) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilterChip(
                                                selected = detailItrAssigneeFilterBySubdivision,
                                                onClick = { detailItrAssigneeFilterBySubdivision = true },
                                                label = { Text("Моя бригада", style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.testTag("detail_itr_assignees_subdivision_chip")
                                            )
                                            FilterChip(
                                                selected = !detailItrAssigneeFilterBySubdivision,
                                                onClick = { detailItrAssigneeFilterBySubdivision = false },
                                                label = { Text("Весь участок", style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.testTag("detail_itr_assignees_all_chip")
                                            )
                                        }
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 180.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        assignees.forEach { emp ->
                                            val isChecked = detailSelectedAssigneeIds.contains(emp.id)
                                            val isMySub = emp.groupId == activeEmployee.groupId ||
                                                (emp.groupName.isNotBlank() && emp.groupName.equals(activeEmployee.groupName, ignoreCase = true))
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isChecked) {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        if (isChecked) {
                                                            detailSelectedAssigneeIds.remove(emp.id)
                                                        } else {
                                                            detailSelectedAssigneeIds.add(emp.id)
                                                        }
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { checked ->
                                                            if (checked) {
                                                                if (!detailSelectedAssigneeIds.contains(emp.id)) {
                                                                    detailSelectedAssigneeIds.add(emp.id)
                                                                }
                                                            } else {
                                                                detailSelectedAssigneeIds.remove(emp.id)
                                                            }
                                                        },
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = emp.fullName,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.SemiBold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            if (activeEmployee.role == com.example.data.model.CorporateRole.ITR) {
                                                                Text(
                                                                    text = if (isMySub) "Бригада" else "Смежник",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = if (isMySub) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = "${emp.position} • ${emp.groupName}",
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

                                    TextField(
                                        value = detailManualAssigneeText,
                                        onValueChange = { detailManualAssigneeText = it },
                                        placeholder = { Text("Доп. ФИО вручную (через запятую)", style = MaterialTheme.typography.bodySmall) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                if (detailSelectedAssigneeIds.isNotEmpty() || detailManualAssigneeText.isNotBlank()) {
                                                    onUpdateAssignees(
                                                        detailSelectedAssigneeIds.toList(),
                                                        detailManualAssigneeText.trim()
                                                    )
                                                    showEditAssigneeSection = false
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("save_task_assignees_button")
                                        ) {
                                            Text("Сохранить")
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))

                            // Срок + Объём в компактную сетку
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Срок",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${ReportDocumentGenerator.formatEpochDay(task.dueDateEpochDay)} · до ${task.deadlineTime}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (targetVol > 0.0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Объём",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${SubtaskItem.formatVolumeNumber(completedVol)} / ${SubtaskItem.formatVolumeNumber(targetVol)} $unitLabel",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isVolume100) emeraldPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            if (!task.geoLocation.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = task.geoLocation,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (task.recurrence != RecurrenceRule.NONE) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Повтор: ${task.recurrence.titleRu}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // =====================================================================
                    // 3. ЭТАПЫ И ОБЪЁМ ВЫПОЛНЕНИЯ
                    // =====================================================================
                    val subtasks = task.decodedSubtasks()
                    val isEditableTask = task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Этапы и объём",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { showChecklistInfoTooltip = !showChecklistInfoTooltip },
                                    modifier = Modifier
                                        .size(26.dp)
                                        .testTag("checklist_info_tooltip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = "Правила учёта объёма",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "$completionPct%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (completionPct >= 100) emeraldPrimary else MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(visible = showChecklistInfoTooltip) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = softFieldBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Задача считается выполненной при достижении 100% объёма работ. Если работа выполнена частично, в отчёт автоматически попадёт фактически выполненный объём.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // Чистый прогресс-бар
                        val progressFraction = (completionPct / 100f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                emeraldPrimary
                                            )
                                        )
                                    )
                            )
                        }

                        if (subtasks.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                subtasks.forEach { sub ->
                                    val itemTarget = sub.effectiveTargetVolume
                                    val itemCompleted = sub.effectiveCompletedVolume
                                    val itemUnit = sub.effectiveUnit
                                    val stepDelta = when {
                                        itemTarget >= 100.0 -> 10.0
                                        itemTarget >= 20.0 -> 5.0
                                        else -> 1.0
                                    }
                                    var subVolInput by remember(sub.id, itemCompleted) {
                                        mutableStateOf(SubtaskItem.formatVolumeNumber(itemCompleted))
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = softPanelBg,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("subtask_row_${sub.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Checkbox(
                                                checked = sub.isDone,
                                                onCheckedChange = { onToggleSubtask(sub.id) },
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .testTag("subtask_checkbox_${sub.id}")
                                            )

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { onToggleSubtask(sub.id) }
                                            ) {
                                                Text(
                                                    text = sub.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None
                                                    ),
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (sub.isDone) {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                Text(
                                                    text = "${SubtaskItem.formatVolumeNumber(itemCompleted)} / ${SubtaskItem.formatVolumeNumber(itemTarget)} $itemUnit · ${sub.progressPercent}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            if (isEditableTask) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surface
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(RoundedCornerShape(7.dp))
                                                                .clickable {
                                                                    val nextVal = (itemCompleted - stepDelta).coerceAtLeast(0.0)
                                                                    subVolInput = SubtaskItem.formatVolumeNumber(nextVal)
                                                                    onUpdateSubtaskVolume(sub.id, nextVal)
                                                                }
                                                                .testTag("subtask_minus_${sub.id}"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Remove,
                                                                contentDescription = "Уменьшить количество",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }

                                                        BasicTextField(
                                                            value = subVolInput,
                                                            onValueChange = { raw ->
                                                                subVolInput = raw
                                                                val parsed = raw.replace(",", ".").toDoubleOrNull()
                                                                if (parsed != null) {
                                                                    onUpdateSubtaskVolume(sub.id, parsed)
                                                                }
                                                            },
                                                            singleLine = true,
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            textStyle = MaterialTheme.typography.labelMedium.copy(
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center
                                                            ),
                                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                            modifier = Modifier
                                                                .width(42.dp)
                                                                .testTag("subtask_volume_input_${sub.id}")
                                                        )

                                                        Box(
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(RoundedCornerShape(7.dp))
                                                                .clickable {
                                                                    val nextVal = (itemCompleted + stepDelta).coerceAtMost(itemTarget)
                                                                    subVolInput = SubtaskItem.formatVolumeNumber(nextVal)
                                                                    onUpdateSubtaskVolume(sub.id, nextVal)
                                                                }
                                                                .testTag("subtask_plus_${sub.id}"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = "Увеличить количество",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { onDeleteSubtask(sub.id) },
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .testTag("subtask_delete_${sub.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Удалить пункт",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = "${SubtaskItem.formatVolumeNumber(itemCompleted)} $itemUnit",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = emeraldPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (targetVol > 0.0 && isEditableTask) {
                            // Прямой ввод фактического объёма для задач без чек-листа
                            val stepDelta = when {
                                targetVol >= 100.0 -> 10.0
                                targetVol >= 20.0 -> 5.0
                                else -> 1.0
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = softPanelBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Факт выполнения",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "План: ${SubtaskItem.formatVolumeNumber(targetVol)} $unitLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(7.dp))
                                                        .clickable {
                                                            val cur = taskCompletedVolInput.replace(",", ".").toDoubleOrNull() ?: completedVol
                                                            val next = (cur - stepDelta).coerceAtLeast(0.0)
                                                            taskCompletedVolInput = SubtaskItem.formatVolumeNumber(next)
                                                            onUpdateTaskVolume(next)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Уменьшить", modifier = Modifier.size(15.dp))
                                                }
                                                BasicTextField(
                                                    value = taskCompletedVolInput,
                                                    onValueChange = { raw ->
                                                        taskCompletedVolInput = raw
                                                        val parsed = raw.replace(",", ".").toDoubleOrNull()
                                                        if (parsed != null) {
                                                            onUpdateTaskVolume(parsed.coerceAtLeast(0.0))
                                                        }
                                                    },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    textStyle = MaterialTheme.typography.labelMedium.copy(
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center
                                                    ),
                                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                    modifier = Modifier.width(46.dp)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(7.dp))
                                                        .clickable {
                                                            val cur = taskCompletedVolInput.replace(",", ".").toDoubleOrNull() ?: completedVol
                                                            val next = (cur + stepDelta).coerceAtMost(targetVol)
                                                            taskCompletedVolInput = SubtaskItem.formatVolumeNumber(next)
                                                            onUpdateTaskVolume(next)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Увеличить", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                                }
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = emeraldPrimary.copy(alpha = 0.14f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    taskCompletedVolInput = SubtaskItem.formatVolumeNumber(targetVol)
                                                    onUpdateTaskVolume(targetVol)
                                                }
                                        ) {
                                            Text(
                                                text = "100%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = emeraldPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (subtasks.isEmpty()) {
                            Text(
                                text = "Этапы не заданы",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isEditableTask) {
                            if (!showAddSubtaskComposer) {
                                TextButton(
                                    onClick = { showAddSubtaskComposer = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("expand_add_subtask_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Добавить этап",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            AnimatedVisibility(visible = showAddSubtaskComposer) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = softPanelBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                value = newSubtaskTitle,
                                                onValueChange = { newSubtaskTitle = it },
                                                placeholder = { Text("Название этапа...", style = MaterialTheme.typography.bodySmall) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp),
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("add_subtask_input")
                                            )
                                            TextField(
                                                value = newSubtaskTargetVolText,
                                                onValueChange = { newSubtaskTargetVolText = it },
                                                placeholder = { Text("Кол-во", style = MaterialTheme.typography.bodySmall) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp),
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent
                                                ),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier
                                                    .width(78.dp)
                                                    .testTag("add_subtask_volume_input")
                                            )
                                            FilledIconButton(
                                                onClick = {
                                                    if (newSubtaskTitle.isNotBlank()) {
                                                        val vol = (newSubtaskTargetVolText.replace(",", ".").toDoubleOrNull() ?: 1.0)
                                                            .coerceAtLeast(0.1)
                                                        val u = newSubtaskUnit.trim().ifBlank { "шт" }
                                                        onAddSubtask(newSubtaskTitle.trim(), vol, u)
                                                        newSubtaskTitle = ""
                                                        newSubtaskTargetVolText = "1"
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .testTag("add_subtask_button")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Добавить пункт")
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            STANDARD_VOLUME_UNITS.forEach { uChip ->
                                                FilterChip(
                                                    selected = newSubtaskUnit == uChip,
                                                    onClick = { newSubtaskUnit = uChip },
                                                    label = { Text(uChip, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                            TextButton(onClick = { showAddSubtaskComposer = false }) {
                                                Text("Свернуть", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // =====================================================================
                    // 4. ФОТООТЧЁТ (Горизонтальная галерея превью + компактный захват)
                    // =====================================================================
                    val photos = task.decodedPhotos()
                    val minReqPhotos = task.minPhotoCount.coerceAtLeast(1L)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Фотоотчёт",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val allSynced = photos.isNotEmpty() && photos.all {
                                    PhotoCaptureUtils.hasCloudReference(it.uriString) ||
                                        PhotoCaptureUtils.hasCachedCloudPhoto(context, it.id)
                                }
                                when {
                                    isProcessingPhoto || isUploadingPhotosToCloud -> {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(11.dp), strokeWidth = 1.8.dp)
                                                Text(
                                                    text = "Отправка...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    isHydratingCloudPhotos -> {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(11.dp), strokeWidth = 1.8.dp)
                                                Text(
                                                    text = "Загрузка...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    hasCloudPhotoSyncError -> {
                                        Surface(
                                            color = crimsonSecondary.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .clickable(onClick = onRetryCloudSync)
                                                .testTag("retry_photo_cloud_sync_chip")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = "Повторить", tint = crimsonSecondary, modifier = Modifier.size(12.dp))
                                                Text(
                                                    text = "Повторить",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = crimsonSecondary
                                                )
                                            }
                                        }
                                    }
                                    allSynced -> {
                                        Surface(
                                            color = emeraldPrimary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = emeraldPrimary, modifier = Modifier.size(12.dp))
                                                Text(
                                                    text = "В облаке",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = emeraldPrimary
                                                )
                                            }
                                        }
                                    }
                                    photos.isNotEmpty() -> {
                                        Surface(
                                            color = mutedAmber.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .clickable(onClick = onRetryCloudSync)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.CloudOff, contentDescription = "Обновить", tint = mutedAmber, modifier = Modifier.size(12.dp))
                                                Text(
                                                    text = "Обновить",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = mutedAmber
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "${photos.size} / $minReqPhotos фото",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (task.hasEnoughPhotos()) emeraldPrimary else mutedAmber
                            )
                        }

                        if (isProcessingPhoto) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        // Горизонтальная карусель реальных миниатюр фото
                        if (photos.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                photos.forEachIndexed { idx, ph ->
                                    val bitmap = remember(
                                        ph.id,
                                        ph.uriString,
                                        ph.comment,
                                        ph.capturedAtText,
                                        task.title,
                                        cloudPhotoCacheVersion
                                    ) {
                                        PhotoCaptureUtils.loadBitmapFromUriOrPath(
                                            uriOrPath = ph.uriString,
                                            fallbackComment = ph.comment,
                                            fallbackAuthor = ph.authorName,
                                            fallbackTime = ph.capturedAtText,
                                            fallbackGps = ph.gpsCoords,
                                            fallbackTaskTitle = task.title,
                                            context = context,
                                            photoId = ph.id
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .width(116.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { selectedPhotoForPreview = ph }
                                            .testTag("photo_item_${ph.id}"),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(104.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(softFieldBg)
                                        ) {
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Фото ${idx + 1}",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (isHydratingCloudPhotos) {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                                }
                                            } else {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.PhotoCamera,
                                                        contentDescription = "Фото ${idx + 1}",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Метка номера и времени внизу
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.56f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(6.dp)
                                            ) {
                                                Text(
                                                    text = "#${idx + 1} · ${ph.capturedAtText.takeLast(5)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            if (task.taskStatus != TaskStatus.APPROVED) {
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.56f),
                                                    shape = CircleShape,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .clickable { onDeletePhoto(ph.id) }
                                                        .testTag("delete_photo_${ph.id}")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Удалить фото",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Text(
                                            text = ph.comment.ifBlank { "Фото #${idx + 1}" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!isExportingPhotosPdf) {
                                        coroutineScope.launch {
                                            isExportingPhotosPdf = true
                                            photoStatusError = null
                                            val pdfRes = withContext(Dispatchers.IO) {
                                                ReportDocumentGenerator.exportTaskPhotosToPdf(
                                                    context = context,
                                                    task = task,
                                                    photos = photos,
                                                    preparedByName = activeEmployee.shortName,
                                                    saveToDownloads = true
                                                )
                                            }
                                            isExportingPhotosPdf = false
                                            pdfRes.onSuccess { result ->
                                                ReportDocumentGenerator.openPdfFile(
                                                    context = context,
                                                    pdfFile = result.file,
                                                    title = "Фотоотчёт ${task.title.take(28)}"
                                                )
                                            }.onFailure { err ->
                                                val msg = err.message ?: "Ошибка выгрузки фотоотчёта в PDF"
                                                photoStatusError = msg
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isExportingPhotosPdf,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("task_photos_export_pdf_button")
                            ) {
                                if (isExportingPhotosPdf) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Выгрузить все фото в PDF (${photos.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (isEditableTask) {
                            val triggerCamera: () -> Unit = {
                                val hasCamPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasCamPerm) {
                                    openNativeCamera()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }

                            if (photos.isEmpty()) {
                                val dashColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.40f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(softPanelBg)
                                        .drawBehind {
                                            drawRoundRect(
                                                color = dashColor,
                                                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                                                style = Stroke(
                                                    width = 1.25.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                                                )
                                            )
                                        }
                                        .clickable(onClick = triggerCamera)
                                        .padding(vertical = 14.dp, horizontal = 14.dp)
                                        .testTag("capture_camera_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Приложите реальное фото для отчёта (${photos.size}/$minReqPhotos)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = triggerCamera,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .height(42.dp)
                                                    .testTag("native_camera_photo_button")
                                            ) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Камера", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                            }
                                            Button(
                                                onClick = {
                                                    photoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .height(42.dp)
                                                    .testTag("pick_gallery_photo_button")
                                            ) {
                                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Из галереи", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Компактная строка добора фото, когда снимки уже приложены
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = triggerCamera,
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("native_camera_photo_button")
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Камера", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("pick_gallery_photo_button")
                                    ) {
                                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Галерея", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            photoStatusError?.let { errText ->
                                Surface(
                                    color = crimsonSecondary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = errText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = crimsonSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            // Компактная строка: подпись к фото + тумблер серии кадров
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = photoComment,
                                    onValueChange = { photoComment = it },
                                    placeholder = {
                                        Text("Подпись к фото...", style = MaterialTheme.typography.bodySmall)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = softFieldBg,
                                        unfocusedContainerColor = softPanelBg,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("photo_comment_input")
                                )
                                FilterChip(
                                    selected = continuousShootingMode,
                                    onClick = { continuousShootingMode = !continuousShootingMode },
                                    label = {
                                        Text("Серия", style = MaterialTheme.typography.labelSmall)
                                    },
                                    modifier = Modifier.testTag("continuous_camera_switch")
                                )
                            }
                        }
                    }

                    // =====================================================================
                    // 5. КОММЕНТАРИИ И ИСТОРИЯ
                    // =====================================================================
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showExtraSections = !showExtraSections }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Комментарии (${task.commentItems.size}) · Журнал (${task.historyItems.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = if (showExtraSections) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(visible = showExtraSections) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val comments = task.decodedComments()
                                comments.forEach { cmt ->
                                    Surface(
                                        color = softPanelBg,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "${cmt.authorName} · ${cmt.createdAtText}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(text = cmt.text, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TextField(
                                        value = newCommentText,
                                        onValueChange = { newCommentText = it },
                                        placeholder = { Text("Написать комментарий...", style = MaterialTheme.typography.bodySmall) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = softFieldBg,
                                            unfocusedContainerColor = softPanelBg,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilledIconButton(
                                        onClick = {
                                            if (newCommentText.isNotBlank()) {
                                                onAddComment(newCommentText, "")
                                                newCommentText = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить", modifier = Modifier.size(18.dp))
                                    }
                                }

                                val history = task.decodedHistory()
                                if (history.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier.padding(top = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        history.forEach { h ->
                                            Text(
                                                text = "• ${h.timestampText} · ${h.actorName} — ${h.note}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (task.canBeCancelledBy(activeEmployee)) {
                                    TextButton(
                                        onClick = { showCancelConfirm = true },
                                        modifier = Modifier.testTag("dialog_cancel_task_button")
                                    ) {
                                        Text("Отменить задачу (с откатом XP)", color = crimsonSecondary)
                                    }
                                }
                                if (task.taskStatus != TaskStatus.ARCHIVED) {
                                    TextButton(
                                        onClick = {
                                            onArchiveTask()
                                            onDismiss()
                                        }
                                    ) {
                                        Text("Переместить в архив")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }

                // =========================================================================
                // 6. ЗАКРЕПЛЁННАЯ НИЖНЯЯ ПАНЕЛЬ ДЕЙСТВИЙ
                // =========================================================================
                if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
                        ) {
                            if (canApproveThisTask) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { showRejectDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, crimsonSecondary.copy(alpha = 0.65f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("reject_report_button")
                                    ) {
                                        Text(
                                            text = "Отклонить",
                                            color = crimsonSecondary,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            onApproveReport(supervisorNote)
                                            showApprovedOutcomeDialog = true
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = emeraldPrimary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1.35f)
                                            .height(50.dp)
                                            .testTag("approve_report_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Принять отчёт",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                val hasEnoughPhotosNow = task.hasEnoughPhotos()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!hasEnoughPhotosNow) {
                                        OutlinedButton(
                                            onClick = {
                                                val hasCamPerm = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.CAMERA
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (hasCamPerm) {
                                                    openNativeCamera()
                                                } else {
                                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .height(50.dp)
                                                .testTag("bottom_bar_open_camera_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = null,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Фото (${task.photoItems.size}/${task.minPhotoCount})",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    val submitLabel = when {
                                        task.taskStatus == TaskStatus.ON_REVIEW -> "Обновить отчёт"
                                        assigneeRole == com.example.data.model.CorporateRole.ITR -> "Сдать руководителю"
                                        task.requiresApproval -> "Сдать на проверку"
                                        else -> "Завершить задачу"
                                    }
                                    Button(
                                        onClick = {
                                            onSubmitWorkerVolumeReport(null, photoComment)
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = emeraldPrimary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("submit_task_completion_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = submitLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.navigationBarsPadding().height(12.dp))
                }
            }
        }
    }

    selectedPhotoForPreview?.let { photoItem ->
        val dialogPhotos = task.decodedPhotos()
        FullScreenPhotoDialog(
            photo = photoItem,
            allPhotos = dialogPhotos,
            task = task,
            taskTitle = task.title,
            preparedByName = activeEmployee.fullName,
            canDelete = task.taskStatus != TaskStatus.APPROVED,
            onDismiss = { selectedPhotoForPreview = null },
            onDelete = { deletePhotoId ->
                onDeletePhoto(deletePhotoId)
                selectedPhotoForPreview = dialogPhotos.firstOrNull { it.id != deletePhotoId }
            },
            cloudPhotoCacheVersion = cloudPhotoCacheVersion,
            isHydratingCloudPhotos = isHydratingCloudPhotos,
            isUploadingPhotosToCloud = isUploadingPhotosToCloud,
            hasCloudPhotoSyncError = hasCloudPhotoSyncError,
            onRetryCloudSync = onRetryCloudSync
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить задачу?") },
            text = { Text("Задача «${task.title}» будет удалена, а начисленный за неё XP — откачен.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteTask()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Отменить задачу?") },
            text = { Text("Статус задачи «${task.title}» сменится на «Отменена», а связанные XP и квестовые зачёты будут откачены.") },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirm = false
                        onCancelTask()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Отменить задачу", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text("Назад")
                }
            }
        )
    }

    if (showRejectDialog) {
        val quickRejectReasons = remember {
            listOf(
                "Недостаточно фотоотчёта",
                "Объём выполнен не полностью",
                "Замечание по качеству СМР",
                "Требуется корректировка данных"
            )
        }
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Отклонение задания") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Укажите комментарий, почему задание возвращается исполнителю на доработку:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickRejectReasons.forEach { preset ->
                            FilterChip(
                                selected = rejectionReasonInput == preset,
                                onClick = { rejectionReasonInput = preset },
                                label = {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        label = { Text("Комментарий (причина отклонения) *") },
                        placeholder = { Text("Опишите замечание для исполнителя...") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rejection_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanReason = rejectionReasonInput.trim()
                        if (cleanReason.isNotBlank()) {
                            submittedRejectionComment = cleanReason
                            onRejectReport(cleanReason)
                            showRejectDialog = false
                            showRejectedOutcomeDialog = true
                        }
                    },
                    enabled = rejectionReasonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reject_dialog_button")
                ) {
                    Text("Отклонить", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showRejectedOutcomeDialog) {
        AlertDialog(
            onDismissRequest = {
                showRejectedOutcomeDialog = false
                onDismiss()
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = crimsonSecondary
                    )
                    Text("Задание отклонено")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Задание «${task.title}» отклонено и возвращено исполнителю (${task.assigneeName}) на доработку.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (submittedRejectionComment.isNotBlank()) {
                        Surface(
                            color = crimsonSecondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Комментарий: $submittedRejectionComment",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = crimsonSecondary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectedOutcomeDialog = false
                        onDismiss()
                    },
                    modifier = Modifier.testTag("confirm_rejected_outcome_button")
                ) {
                    Text("ОК")
                }
            }
        )
    }

    if (showApprovedOutcomeDialog) {
        AlertDialog(
            onDismissRequest = {
                showApprovedOutcomeDialog = false
                onDismiss()
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = emeraldPrimary
                    )
                    Text("Отчёт принят")
                }
            },
            text = {
                Text(
                    text = "Выполнение задания «${task.title}» подтверждено (${activeEmployee.shortName}). Принятый отчёт скрыт с главной страницы и перемещён на вкладку принятых заданий (Приёмка / Архив).",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApprovedOutcomeDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = emeraldPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_approved_outcome_button")
                ) {
                    Text("ОК")
                }
            }
        )
    }
}


