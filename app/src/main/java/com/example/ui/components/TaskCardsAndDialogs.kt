package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
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
    onDeleteTask: () -> Unit = {},
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
    val canDeleteThisTask = activeEmployee.role != com.example.data.model.CorporateRole.WORKER ||
        task.creatorId == activeEmployee.id
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
    var showExtraSections by remember { mutableStateOf(false) }

    // Реальная фотосъёмка (через FileProvider + системная камера) и выбор реальных фото из галереи
    var continuousShootingMode by remember { mutableStateOf(false) }
    var burstShotCount by remember { mutableIntStateOf(0) }
    var isProcessingPhoto by remember { mutableStateOf(false) }
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
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (canDeleteThisTask) {
                                IconButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier
                                        .size(40.dp)
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
                                modifier = Modifier.size(40.dp)
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
        FullScreenPhotoDialog(
            photo = photoItem,
            taskTitle = task.title,
            canDelete = task.taskStatus != TaskStatus.APPROVED,
            onDismiss = { selectedPhotoForPreview = null },
            onDelete = {
                onDeletePhoto(photoItem.id)
                selectedPhotoForPreview = null
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
            text = { Text("Задача «${task.title}» будет удалена из списка.") },
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

@Composable
fun FullScreenPhotoDialog(
    photo: PhotoReportItem,
    taskTitle: String = "",
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    cloudPhotoCacheVersion: Int = 0,
    isHydratingCloudPhotos: Boolean = false,
    isUploadingPhotosToCloud: Boolean = false,
    hasCloudPhotoSyncError: Boolean = false,
    onRetryCloudSync: () -> Unit = {}
) {
    val context = LocalContext.current
    val bitmap = remember(
        photo.id,
        photo.uriString,
        photo.comment,
        photo.capturedAtText,
        taskTitle,
        cloudPhotoCacheVersion
    ) {
        PhotoCaptureUtils.loadBitmapFromUriOrPath(
            uriOrPath = photo.uriString,
            fallbackComment = photo.comment,
            fallbackAuthor = photo.authorName,
            fallbackTime = photo.capturedAtText,
            fallbackGps = photo.gpsCoords,
            fallbackTaskTitle = taskTitle,
            context = context,
            photoId = photo.id
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Просмотр фотоотчёта",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${photo.authorName} • ${photo.capturedAtText}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Row {
                            if (canDelete) {
                                IconButton(
                                    onClick = onDelete,
                                    modifier = Modifier.testTag("fullscreen_delete_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Удалить это фото",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Закрыть просмотр",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(min = 200.dp, max = 380.dp)
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Полноэкранное фото",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isHydratingCloudPhotos || isUploadingPhotosToCloud) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF60A5FA),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = if (isUploadingPhotosToCloud) {
                                                "Отправка фото в облако..."
                                            } else {
                                                "Загрузка фото из облака..."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            tint = Color(0xFF60A5FA),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Text(
                                            text = photo.comment,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = if (hasCloudPhotoSyncError) {
                                                "Ошибка синхронизации с облаком. Нажмите ниже для повтора."
                                            } else {
                                                "Фото ещё не загружено в облако с устройства автора или требуется повторная загрузка."
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8),
                                            textAlign = TextAlign.Center
                                        )
                                        OutlinedButton(
                                            onClick = onRetryCloudSync,
                                            border = BorderStroke(1.dp, Color(0xFF60A5FA)),
                                            modifier = Modifier.testTag("fullscreen_retry_photo_sync_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = Color(0xFF60A5FA),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Загрузить из облака", color = Color(0xFF60A5FA))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = photo.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Геометка: ${photo.gpsCoords}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (canDelete) {
                            OutlinedButton(
                                onClick = onDelete,
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Удалить фото", color = Color(0xFFEF4444))
                            }
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Закрыть")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Премиальный экран «Постановка задачи» в стиле Linear / Notion / Apple:
 * 1. Шапка: только заголовок «Постановка задачи» и крестик закрытия (без верхней кнопки сохранения).
 * 2. Основная информация (всегда открыта):
 *    - Большое текстовое поле «Название задачи» без жёсткой рамки (мягкая подложка со скруглениями).
 *    - Компактный выпадающий список «Исполнитель» с круглым аватаром и опцией «+ Ввести ФИО вручную» (без сжатого вертикального текста).
 *    - Элегантный селектор дат: горизонтальная лента дней + кнопка разворота полноценного календаря с задачами на выбранную дату.
 * 3. Настройки выполнения (сворачиваемые аккордеоны):
 *    - Уровень важности (Приоритет): компактный сегментированный контрол в одну строку с цветовой индикацией.
 *    - Фотоотчёт: переключатель «Обязательный фотоотчёт» + плавно раскрывающийся компактный степпер (− / +) без лишних дублирующих чипов.
 *    - Чек-лист: чистый список строк (название + компактное поле количества + интегрированный выпадающий список единиц измерения прямо в строке) и кнопка «+ Добавить пункт».
 * 4. Дополнительно:
 *    - Кнопка «+ Добавить описание», раскрывающая многострочное поле описания без жёсткой рамки.
 *    - Блок «Продвинутые опции» в самом низу перед футером.
 * 5. Футер: закреплённая нижняя панель с мягкой тенью и двумя кнопками: «Отмена» и «Создать задачу».
 */
private enum class CreateSheetSection {
    ASSIGNEES,
    DEADLINE,
    PHOTO,
    WORK_SCOPE
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    activeEmployee: CorporateEmployee,
    availableEmployees: List<CorporateEmployee> = listOf(activeEmployee),
    existingTasks: List<TaskDocument> = emptyList(),
    currentEpochDay: Long,
    initialDueDateEpochDay: Long = currentEpochDay,
    onDismiss: () -> Unit,
    onCreate: (TaskDraft) -> Unit
) {
    val tokens = LocalAppThemeTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var itrAssigneeFilterBySubdivision by remember {
        mutableStateOf(activeEmployee.role == com.example.data.model.CorporateRole.ITR)
    }
    val assignees = remember(availableEmployees, activeEmployee, itrAssigneeFilterBySubdivision) {
        if (!activeEmployee.role.canAssignToOtherEmployees()) {
            listOf(activeEmployee)
        } else {
            val candidates = availableEmployees
                .filter { activeEmployee.role.canAssignTo(it.role) }
                .ifEmpty { listOf(activeEmployee) }

            if (activeEmployee.role == com.example.data.model.CorporateRole.ITR && itrAssigneeFilterBySubdivision) {
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

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val selectedAssigneeIds = remember(assignees) {
        mutableStateListOf(assignees.first().id)
    }
    val selectedAssignees = remember(assignees, selectedAssigneeIds.toList()) {
        assignees.filter { it.id in selectedAssigneeIds }.ifEmpty { listOf(assignees.first()) }
    }
    val selectedAssignee = selectedAssignees.first()
    var useCustomAssigneeName by remember { mutableStateOf(false) }
    var customAssigneeName by remember { mutableStateOf("") }
    var assigneeSearchQuery by remember { mutableStateOf("") }

    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var selectedDueEpochDay by remember(initialDueDateEpochDay) { mutableStateOf(initialDueDateEpochDay) }
    var calendarExpanded by remember { mutableStateOf(false) }
    var displayedMonthOffset by remember { mutableIntStateOf(0) }
    var deadlineTime by remember { mutableStateOf("18:00") }

    var requiresPhoto by remember { mutableStateOf(true) }
    var minPhotoCountText by remember { mutableStateOf("1") }

    // Единый блок: либо этапы чек-листа, либо общий плановый объём
    val draftChecklistItems = remember { mutableStateListOf<SubtaskItem>() }
    var draftItemTitle by remember { mutableStateOf("") }
    var draftItemVolText by remember { mutableStateOf("10") }
    var draftItemUnit by remember { mutableStateOf("шт") }
    var draftUnitMenuExpanded by remember { mutableStateOf(false) }

    var hasVolumeRequirement by remember { mutableStateOf(false) }
    var targetVolumeText by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("шт") }
    var isCustomUnitSelected by remember { mutableStateOf(false) }
    var customUnitText by remember { mutableStateOf("") }

    // Layer 1 active quick-chip section & Layer 2 progressive disclosure
    var activeSection by remember { mutableStateOf<CreateSheetSection?>(null) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var requiresApproval by remember { mutableStateOf(true) }
    var selectedRecurrence by remember { mutableStateOf(RecurrenceRule.NONE) }
    var geoLocation by remember { mutableStateOf(activeEmployee.siteName) }

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
                geoLocation = geoLocation.trim()
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

    val dueChipLabel = remember(selectedDueEpochDay, currentEpochDay, deadlineTime) {
        val dayPart = when (selectedDueEpochDay) {
            currentEpochDay -> "Сегодня"
            currentEpochDay + 1L -> "Завтра"
            currentEpochDay - 1L -> "Вчера"
            else -> ReportDocumentGenerator.formatEpochDay(selectedDueEpochDay).take(5)
        }
        "$dayPart, ${deadlineTime.ifBlank { "18:00" }}"
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
            // ШАПКА: минималистичная
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Новая задача",
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

            // ТЕЛО: ДВУХСЛОЙНАЯ ФОРМА
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(formScrollState)
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // СЛОЙ 1.1: КРУПНОЕ ПОЛЕ ЗАГОЛОВКА (БЕЗ РАМКИ)
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

                // СЛОЙ 1.2: SMART HORIZONTAL CHIPS ROW (Исполнитель • Срок • Фото • Объём/Этапы)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Чип: Исполнитель
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
                            .clickable {
                                activeSection = if (assigneeChipActive) null else CreateSheetSection.ASSIGNEES
                            }
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
                                imageVector = if (selectedAssignees.size > 1) Icons.Default.Group else Icons.Default.Person,
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

                    // Чип: Срок
                    val deadlineChipActive = activeSection == CreateSheetSection.DEADLINE
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (deadlineChipActive) MaterialTheme.colorScheme.primary else softFieldBg,
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                activeSection = if (deadlineChipActive) null else CreateSheetSection.DEADLINE
                            }
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

                    // Чип: Фотоотчёт
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
                            .clickable {
                                activeSection = if (photoChipActive) null else CreateSheetSection.PHOTO
                            }
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

                    // Чип: Объём / этапы
                    val scopeChipActive = activeSection == CreateSheetSection.WORK_SCOPE
                    val hasScopeData = draftChecklistItems.isNotEmpty() || (hasVolumeRequirement && targetVolumeText.isNotBlank())
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
                            .clickable {
                                activeSection = if (scopeChipActive) null else CreateSheetSection.WORK_SCOPE
                            }
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

                // КОНТЕКСТНАЯ ПАНЕЛЬ ВЫБРАННОГО ЧИПА
                AnimatedVisibility(visible = activeSection != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = softPanelBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (activeSection) {
                            CreateSheetSection.ASSIGNEES -> {
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
                                                text = if (selectedAssignees.size > 1) {
                                                    "Выбрано: ${selectedAssignees.size}"
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
                                                    onClick = { useCustomAssigneeName = !useCustomAssigneeName },
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

                                        if (activeEmployee.role == com.example.data.model.CorporateRole.ITR && !useCustomAssigneeName) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                FilterChip(
                                                    selected = itrAssigneeFilterBySubdivision,
                                                    onClick = { itrAssigneeFilterBySubdivision = true },
                                                    label = {
                                                        Text(
                                                            text = "Моё подразделение",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    },
                                                    modifier = Modifier.testTag("create_itr_assignees_subdivision_chip")
                                                )
                                                FilterChip(
                                                    selected = !itrAssigneeFilterBySubdivision,
                                                    onClick = { itrAssigneeFilterBySubdivision = false },
                                                    label = {
                                                        Text(
                                                            text = "Весь участок",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    },
                                                    modifier = Modifier.testTag("create_itr_assignees_all_chip")
                                                )
                                            }
                                        }

                                        if (useCustomAssigneeName) {
                                            TextField(
                                                value = customAssigneeName,
                                                onValueChange = { customAssigneeName = it },
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
                                                    onValueChange = { assigneeSearchQuery = it },
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
                                                                useCustomAssigneeName = false
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
                                                            Checkbox(
                                                                checked = isChecked,
                                                                onCheckedChange = null
                                                            )
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

                            CreateSheetSection.DEADLINE -> {
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
                                            onClick = { calendarExpanded = !calendarExpanded },
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

                                    // Быстрая лента дат
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
                                            val localDate = remember(dayEpoch) { java.time.LocalDate.ofEpochDay(dayEpoch) }
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
                                                    .clickable { selectedDueEpochDay = dayEpoch }
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

                                    // Быстрое время сдачи
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
                                        listOf("12:00", "16:00", "18:00", "20:00").forEach { tPreset ->
                                            val selTime = deadlineTime == tPreset
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (selTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable { deadlineTime = tPreset }
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
                                            val baseDate = java.time.LocalDate.ofEpochDay(currentEpochDay)
                                            java.time.YearMonth.of(baseDate.year, baseDate.monthValue)
                                                .plusMonths(displayedMonthOffset.toLong())
                                        }
                                        val ruLocale = remember { Locale.forLanguageTag("ru-RU") }
                                        val monthTitle = remember(displayedYearMonth, ruLocale) {
                                            val mName = displayedYearMonth.month
                                                .getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, ruLocale)
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
                                                        onClick = { displayedMonthOffset -= 1 },
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
                                                        onClick = { displayedMonthOffset += 1 },
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
                                                                        .clickable { selectedDueEpochDay = cellEpochDay }
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

                            CreateSheetSection.PHOTO -> {
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
                                        Switch(
                                            checked = requiresPhoto,
                                            onCheckedChange = { requiresPhoto = it }
                                        )
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
                                                            minPhotoCountText = (cur - 1).coerceAtLeast(1).toString()
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Уменьшить", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                BasicTextField(
                                                    value = minPhotoCountText,
                                                    onValueChange = { valDigits ->
                                                        minPhotoCountText = valDigits.filter { it.isDigit() }.take(2)
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
                                                            minPhotoCountText = (cur + 1).coerceAtMost(99).toString()
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

                            CreateSheetSection.WORK_SCOPE -> {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Переключатель: Чек-лист этапов VS Единый объём
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
                                                        .clickable { hasVolumeRequirement = isSingleVol }
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
                                                            androidx.compose.material3.DropdownMenu(
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
                                                        onValueChange = { draftItemTitle = it },
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
                                                        onValueChange = { draftItemVolText = it },
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
                                                                .clickable { draftUnitMenuExpanded = true }
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
                                                        androidx.compose.material3.DropdownMenu(
                                                            expanded = draftUnitMenuExpanded,
                                                            onDismissRequest = { draftUnitMenuExpanded = false }
                                                        ) {
                                                            STANDARD_VOLUME_UNITS.forEach { uItem ->
                                                                DropdownMenuItem(
                                                                    text = { Text(uItem) },
                                                                    onClick = {
                                                                        draftItemUnit = uItem
                                                                        draftUnitMenuExpanded = false
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
                                                    draftItemTitle = ""
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
                                            onValueChange = { targetVolumeText = it },
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
                                                    onClick = {
                                                        isCustomUnitSelected = false
                                                        selectedUnit = unitItem
                                                    },
                                                    label = { Text(unitItem) },
                                                    modifier = Modifier.testTag("unit_chip_$unitItem")
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            null -> Unit
                        }
                    }
                }

                // СЛОЙ 2: PROGRESSIVE DISCLOSURE («Ещё параметры ›»)
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(softPanelBg)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Описание
                        TextField(
                            value = description,
                            onValueChange = { description = it },
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

                        // Участок / зона
                        TextField(
                            value = geoLocation,
                            onValueChange = { geoLocation = it },
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

                        // Приоритет
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
                                                .clickable { selectedPriority = prio }
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

                        // Повторяемость
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    RecurrenceRule.WEEKLY
                                ).forEach { rule ->
                                    FilterChip(
                                        selected = selectedRecurrence == rule,
                                        onClick = { selectedRecurrence = rule },
                                        label = { Text(rule.titleRu, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        // Приёмка руководителем
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
                            Switch(
                                checked = requiresApproval,
                                onCheckedChange = { requiresApproval = it }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // ФИКСИРОВАННЫЙ ФУТЕР (поднят с запасом от нижней кромки экрана и навигации)
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
                        text = "Поставить задачу",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


