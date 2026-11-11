package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
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

                // Индикатор объёма выполнения без дублирования процентов — только чистые цифры (190 / 450)
                if (targetVol > 0.0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Объём работ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${SubtaskItem.formatVolumeNumber(completedVol)} / ${SubtaskItem.formatVolumeNumber(targetVol)} $unitLabel",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (pct >= 100) doneColor else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (tokens.isHighContrastField) 7.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (pct >= 100) doneColor else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Интерактивный чек-лист прямо на карточке с чистыми цифрами объёма без дублей %
                if (subtasks.isNotEmpty() && onToggleSubtask != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        subtasks.take(4).forEach { sub ->
                            val subCompleted = SubtaskItem.formatVolumeNumber(sub.effectiveCompletedVolume)
                            val subTarget = SubtaskItem.formatVolumeNumber(sub.effectiveTargetVolume)
                            val subUnit = sub.effectiveUnit.ifBlank { unitLabel }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleSubtask(sub.id) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = sub.isDone,
                                    onCheckedChange = { onToggleSubtask(sub.id) },
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sub.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (sub.isDone) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$subCompleted / $subTarget $subUnit",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (sub.isDone) doneColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Метаданные мелким серым текстом (исполнитель, дата, фото)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.assigneeName.ifBlank { "Исполнитель" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${ReportDocumentGenerator.formatEpochDay(task.dueDateEpochDay)} • ${task.deadlineTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (subtasks.isNotEmpty()) {
                        Text(
                            text = "Чек-лист: $doneSubtasks/${subtasks.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (pct >= 100) doneColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (task.requiresPhotoReport || task.photoItems.isNotEmpty()) {
                        val photoReady = task.hasEnoughPhotos()
                        Text(
                            text = "📷 ${task.photoItems.size}/${task.minPhotoCount} фото",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (photoReady) doneColor else Color(0xFFD97706)
                        )
                    }
                }

                if (!task.rejectionReason.isNullOrBlank() && task.taskStatus == TaskStatus.REJECTED) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Возврат: ${task.rejectionReason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
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
    onDeletePhoto: (photoId: String) -> Unit = {},
    onToggleSubtask: (subtaskId: String) -> Unit,
    onUpdateSubtaskVolume: (subtaskId: String, completedVol: Double) -> Unit = { _, _ -> },
    onUpdateSubtaskDetails: (subtaskId: String, targetVol: Double, completedVol: Double, unit: String) -> Unit = { _, _, _, _ -> },
    onUpdateTaskVolume: (completedVol: Double) -> Unit = {},
    onUpdateAssignee: (assigneeId: String, customAssigneeName: String) -> Unit = { _, _ -> },
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
    onDeleteTask: () -> Unit = {}
) {
    val context = LocalContext.current
    var photoComment by remember { mutableStateOf("") }
    val photoGps = remember(task.geoLocation) { task.geoLocation ?: "Участок №1" }
    val assignees = remember(availableEmployees, activeEmployee) {
        availableEmployees.ifEmpty { listOf(activeEmployee) }
    }
    var showEditAssigneeSection by remember { mutableStateOf(false) }
    var detailUseManualAssignee by remember { mutableStateOf(false) }
    var detailManualAssigneeText by remember(task.assigneeName) { mutableStateOf(task.assigneeName) }
    var detailAssigneeDropdownExpanded by remember { mutableStateOf(false) }

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
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showExtraSections by remember { mutableStateOf(false) }

    // Серийная съёмка (непрерывная съёмка нескольких кадров подряд)
    var continuousShootingMode by remember { mutableStateOf(true) }
    var burstShotCount by remember { mutableIntStateOf(0) }
    var selectedPhotoForPreview by remember { mutableStateOf<PhotoReportItem?>(null) }

    lateinit var launchCameraAgain: () -> Unit

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val savedPath = PhotoCaptureUtils.saveCameraBitmapWithStamp(
                context = context,
                sourceBitmap = bitmap,
                taskTitle = task.title,
                authorName = activeEmployee.shortName,
                gpsCoords = photoGps,
                comment = photoComment.ifBlank { "Кадр #${task.photoItems.size + 1}" }
            )
            onAddPhoto(
                savedPath,
                photoGps,
                photoComment.ifBlank { "Снимок с камеры #${task.photoItems.size + 1}" }
            )
            burstShotCount++
            if (continuousShootingMode) {
                launchCameraAgain()
            }
        } else {
            burstShotCount = 0
        }
    }

    launchCameraAgain = {
        cameraLauncher.launch(null)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            burstShotCount = 0
            cameraLauncher.launch(null)
        } else {
            val savedPath = PhotoCaptureUtils.generatePhotoStampFile(
                context = context,
                taskTitle = task.title,
                authorName = activeEmployee.shortName,
                gpsCoords = photoGps,
                comment = photoComment.ifBlank { "Фотофиксация выполнения" },
                photoIndex = task.photoItems.size + 1
            )
            onAddPhoto(
                savedPath,
                photoGps,
                photoComment.ifBlank { "Фотофиксация выполнения работ" }
            )
            photoComment = ""
        }
    }

    val tokens = LocalAppThemeTokens.current
    val seedAssignee = remember(task.assigneeId) {
        CorporateSeedData.employees.firstOrNull { it.id == task.assigneeId }
    }
    val assigneeRole = seedAssignee?.role ?: com.example.data.model.CorporateRole.WORKER
    val canApproveThisTask = activeEmployee.role.canApproveReportOf(assigneeRole)
    val isApproved = task.taskStatus == TaskStatus.APPROVED
    val textDeco = if (isApproved) TextDecoration.LineThrough else TextDecoration.None
    val completionPct = task.completionPercent()
    val isVolume100 = task.isVolumeCompleted100Percent()
    val emeraldPrimary = Color(0xFF10B981)
    val mutedAmber = Color(0xFFF59E0B)
    val crimsonSecondary = Color(0xFFEF4444)

    var showChecklistInfoTooltip by remember { mutableStateOf(false) }
    var showAddSubtaskComposer by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.90f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = if (tokens.isDark) 12.dp else 10.dp,
                border = if (tokens.useGlassmorphism) {
                    BorderStroke(0.8.dp, tokens.cardBorderColor)
                } else {
                    null
                }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // =========================================================================
                    // 1. ШАПКА (HEADER) — без жёсткой рамки, полупрозрачный янтарный pill-бейдж,
                    // минималистичные иконки без красных контуров, крупный H1 заголовок задачи
                    // =========================================================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 14.dp, top = 16.dp, bottom = 10.dp)
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
                            Surface(
                                color = badgeAccent.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
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
                                        text = "${task.taskStatus.titleRu} • ${task.taskPriority.titleRu}",
                                        color = badgeAccent,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("dialog_delete_task_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Удалить задачу",
                                        tint = if (showDeleteConfirm) crimsonSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(38.dp)
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // H1 — Крупный жирный заголовок задачи
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
                                style = MaterialTheme.typography.bodySmall.copy(textDecoration = textDeco),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // ПРОКРУЧИВАЕМАЯ ЦЕНТРАЛЬНАЯ ОБЛАСТЬ
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // =====================================================================
                        // 2. БЛОК ИНФОРМАЦИИ (МЕТАДАННЫЕ)
                        // Исполнитель, срок и объём в единой мягкой подложке без рамки.
                        // Объём указывается ТОЛЬКО здесь в формате 190 / 450 шт (без процентов).
                        // =====================================================================
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = if (tokens.isDark) 0.42f else 0.55f
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Строка 1: Исполнитель
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Исполнитель",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showEditAssigneeSection = !showEditAssigneeSection }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                            .testTag("toggle_edit_task_assignee_button")
                                    ) {
                                        Text(
                                            text = task.assigneeName.ifBlank { "Не назначен" },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (showEditAssigneeSection) "Скрыть" else "Изменить",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = showEditAssigneeSection) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp, bottom = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilterChip(
                                                selected = !detailUseManualAssignee,
                                                onClick = { detailUseManualAssignee = false },
                                                label = { Text("Из списка сотрудников") }
                                            )
                                            FilterChip(
                                                selected = detailUseManualAssignee,
                                                onClick = { detailUseManualAssignee = true },
                                                label = { Text("Ввод вручную") }
                                            )
                                        }

                                        if (!detailUseManualAssignee) {
                                            ExposedDropdownMenuBox(
                                                expanded = detailAssigneeDropdownExpanded,
                                                onExpandedChange = { detailAssigneeDropdownExpanded = !detailAssigneeDropdownExpanded },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                OutlinedTextField(
                                                    value = task.assigneeName,
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    singleLine = true,
                                                    label = { Text("Выберите сотрудника") },
                                                    trailingIcon = {
                                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = detailAssigneeDropdownExpanded)
                                                    },
                                                    modifier = Modifier
                                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                        .fillMaxWidth()
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = detailAssigneeDropdownExpanded,
                                                    onDismissRequest = { detailAssigneeDropdownExpanded = false }
                                                ) {
                                                    assignees.forEach { emp ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Column {
                                                                    Text(emp.fullName, fontWeight = FontWeight.SemiBold)
                                                                    Text(
                                                                        "${emp.position} • ${emp.groupName}",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            },
                                                            onClick = {
                                                                onUpdateAssignee(emp.id, "")
                                                                detailAssigneeDropdownExpanded = false
                                                                showEditAssigneeSection = false
                                                            }
                                                        )
                                                    }
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                "+ Ввести ФИО вручную...",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        },
                                                        onClick = {
                                                            detailUseManualAssignee = true
                                                            detailAssigneeDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = detailManualAssigneeText,
                                                    onValueChange = { detailManualAssigneeText = it },
                                                    label = { Text("ФИО исполнителя") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Button(
                                                    onClick = {
                                                        if (detailManualAssigneeText.isNotBlank()) {
                                                            onUpdateAssignee("", detailManualAssigneeText.trim())
                                                            showEditAssigneeSection = false
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("ОК")
                                                }
                                            }
                                        }
                                    }
                                }

                                // Строка 2: Срок
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Срок выполнения",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${ReportDocumentGenerator.formatEpochDay(task.dueDateEpochDay)} • до ${task.deadlineTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Строка 3: Объём работ (ТОЛЬКО здесь, формат 190 / 450 шт, без процентов)
                                if (targetVol > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Объём работ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${SubtaskItem.formatVolumeNumber(completedVol)} / ${SubtaskItem.formatVolumeNumber(targetVol)} $unitLabel",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // =====================================================================
                        // 3. БЛОК «ЧЕК-ЛИСТ И ОБЪЁМ»
                        // - Маленькая иконка (i) с тултипом вместо длинного пояснительного текста
                        // - Один элегантный прогресс-бар с плавным градиентом и рядом цифра 42%
                        // - Список пунктов с компактным инпут-степпером [- | 50 | +]
                        // - Ненавязчивая текстовая кнопка «+ Добавить пункт в новой строке»
                        // =====================================================================
                        val subtasks = task.decodedSubtasks()
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
                                        text = "Чек-лист и объём",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { showChecklistInfoTooltip = !showChecklistInfoTooltip },
                                        modifier = Modifier
                                            .size(24.dp)
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
                            }

                            // Компактный тултип по нажатию на (i)
                            AnimatedVisibility(visible = showChecklistInfoTooltip) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Задача считается выполненной при достижении 100% объёма работ. Если работа выполнена частично, в суточный отчёт автоматически попадёт фактически выполненный объём.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            // Элегантный прогресс-бар с плавным градиентом + рядом с ним цифры 42%
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val progressFraction = (completionPct / 100f).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progressFraction)
                                            .height(8.dp)
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
                                Text(
                                    text = "$completionPct%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (completionPct >= 100) emeraldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Список пунктов чек-листа (без жёстких рамок, со степпером [- | число | +])
                            if (subtasks.isEmpty()) {
                                Text(
                                    text = "Пункты чек-листа пока не добавлены.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
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
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = if (tokens.isDark) 0.34f else 0.45f
                                            ),
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
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Выполнено: ${SubtaskItem.formatVolumeNumber(itemCompleted)} из ${SubtaskItem.formatVolumeNumber(itemTarget)} (${sub.progressPercent}%)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                // Справа — компактный инпут-степпер [- | число | +]
                                                if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = MaterialTheme.colorScheme.surface
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                                                        ) {
                                                            // Кнопка «-»
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

                                                            // Компактное поле ввода количества
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
                                                                    .width(44.dp)
                                                                    .testTag("subtask_volume_input_${sub.id}")
                                                            )

                                                            // Кнопка «+»
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
                                                            .size(26.dp)
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
                            }

                            // Ненавязчивая текстовая кнопка «+ Добавить пункт в новой строке»
                            // (чипсы выбора единиц измерения появляются только при нажатии на добавление)
                            if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                                if (!showAddSubtaskComposer) {
                                    TextButton(
                                        onClick = { showAddSubtaskComposer = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("expand_add_subtask_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Добавить пункт в новой строке",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = showAddSubtaskComposer) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = if (tokens.isDark) 0.4f else 0.55f
                                        ),
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
                                                OutlinedTextField(
                                                    value = newSubtaskTitle,
                                                    onValueChange = { newSubtaskTitle = it },
                                                    placeholder = { Text("Наименование пункта...") },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("add_subtask_input")
                                                )
                                                OutlinedTextField(
                                                    value = newSubtaskTargetVolText,
                                                    onValueChange = { newSubtaskTargetVolText = it },
                                                    placeholder = { Text("Кол-во") },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(10.dp),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier
                                                        .width(86.dp)
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
                                                        .size(42.dp)
                                                        .testTag("add_subtask_button")
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Добавить пункт")
                                                }
                                            }

                                            // Горизонтальные чипсы единиц измерения (появляются только при добавлении)
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
                                                        label = { Text(uChip) }
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
                        // 4. БЛОК «ФОТООТЧЁТ»
                        // - Современная пунктирная зона Drag-and-Drop с иконкой камеры по центру
                        //   и текстом «Добавить фото (0/1)»
                        // - Аккуратный тумблер «Непрерывная съёмка» мелким шрифтом
                        // - Минималистичная строка ввода «Подпись к фото» без лишней обводки
                        // =====================================================================
                        val photos = task.decodedPhotos()
                        val minReqPhotos = task.minPhotoCount.coerceAtLeast(1L)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Фотоотчёт",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${photos.size} / $minReqPhotos фото",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (task.hasEnoughPhotos()) emeraldPrimary else mutedAmber
                                )
                            }

                            // Список уже прикреплённых фотографий (без жёстких рамок)
                            if (photos.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    photos.forEachIndexed { idx, ph ->
                                        val bitmap = remember(ph.uriString) {
                                            PhotoCaptureUtils.loadBitmapFromUriOrPath(ph.uriString)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = if (tokens.isDark) 0.36f else 0.5f
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedPhotoForPreview = ph }
                                                .testTag("photo_item_${ph.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.size(44.dp)
                                                ) {
                                                    if (bitmap != null) {
                                                        Image(
                                                            bitmap = bitmap.asImageBitmap(),
                                                            contentDescription = "Миниатюра фото ${idx + 1}",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.PhotoCamera,
                                                                contentDescription = "Фото ${idx + 1}",
                                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Фото #${idx + 1} • ${ph.capturedAtText}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = ph.comment,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { selectedPhotoForPreview = ph },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Visibility,
                                                        contentDescription = "Просмотреть фото",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                if (task.taskStatus != TaskStatus.APPROVED) {
                                                    IconButton(
                                                        onClick = { onDeletePhoto(ph.id) },
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .testTag("delete_photo_${ph.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Удалить фото",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(17.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                                // Пунктирная зона Drag-and-Drop / Dropzone для добавления фото
                                val dashColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                                val dropzoneBg = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = if (tokens.isDark) 0.25f else 0.35f
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(dropzoneBg)
                                        .drawBehind {
                                            drawRoundRect(
                                                color = dashColor,
                                                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                                                style = Stroke(
                                                    width = 1.5.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(
                                                        floatArrayOf(18f, 14f),
                                                        0f
                                                    )
                                                )
                                            )
                                        }
                                        .clickable {
                                            val hasCamPerm = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.CAMERA
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasCamPerm) {
                                                burstShotCount = 0
                                                cameraLauncher.launch(null)
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }
                                        .padding(vertical = 20.dp, horizontal = 16.dp)
                                        .testTag("capture_camera_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.CameraAlt,
                                                    contentDescription = "Добавить фото",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Добавить фото (${photos.size}/$minReqPhotos)",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Нажмите для съёмки с камеры или прикрепите фотоштамп СМР",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        TextButton(
                                            onClick = {
                                                val savedStampPath = PhotoCaptureUtils.generatePhotoStampFile(
                                                    context = context,
                                                    taskTitle = task.title,
                                                    authorName = activeEmployee.shortName,
                                                    gpsCoords = photoGps,
                                                    comment = photoComment.ifBlank { "Контрольный снимок узла" },
                                                    photoIndex = task.photoItems.size + 1
                                                )
                                                onAddPhoto(
                                                    savedStampPath,
                                                    photoGps,
                                                    photoComment.ifBlank { "Фотоштамп СМР #${task.photoItems.size + 1}" }
                                                )
                                                photoComment = ""
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("instant_photo_stamp_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddAPhoto,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "+ Быстрый фотоштамп СМР",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Аккуратный тумблер «Непрерывная съёмка» с подписью мелким шрифтом
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Непрерывная съёмка (серия кадров подряд)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Switch(
                                        checked = continuousShootingMode,
                                        onCheckedChange = { continuousShootingMode = it },
                                        modifier = Modifier.testTag("continuous_camera_switch")
                                    )
                                }

                                // Минималистичная строка ввода «Подпись к фото» без лишней обводки
                                TextField(
                                    value = photoComment,
                                    onValueChange = { photoComment = it },
                                    placeholder = {
                                        Text(
                                            "Подпись к фото (необязательно)...",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("photo_comment_input")
                                )
                            }
                        }

                        // Сворачиваемый блок комментариев и истории
                        TextButton(
                            onClick = { showExtraSections = !showExtraSections },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (showExtraSections) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (showExtraSections) {
                                    "Скрыть комментарии и журнал"
                                } else {
                                    "Комментарии (${task.commentItems.size}) и журнал (${task.historyItems.size})"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        AnimatedVisibility(visible = showExtraSections) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val comments = task.decodedComments()
                                comments.forEach { cmt ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "${cmt.authorName} • ${cmt.createdAtText}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(text = cmt.text, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = newCommentText,
                                        onValueChange = { newCommentText = it },
                                        placeholder = { Text("Написать комментарий...", style = MaterialTheme.typography.bodySmall) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            if (newCommentText.isNotBlank()) {
                                                onAddComment(newCommentText, "")
                                                newCommentText = ""
                                            }
                                        }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                                    }
                                }

                                val history = task.decodedHistory()
                                if (history.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    history.forEach { h ->
                                        Text(
                                            text = "• ${h.timestampText} (${h.actorName}): ${h.note}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (task.taskStatus != TaskStatus.ARCHIVED) {
                                    TextButton(
                                        onClick = {
                                            onArchiveTask()
                                            onDismiss()
                                        }
                                    ) {
                                        Text("Переместить задачу в архив")
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // 5. ФИКСИРОВАННАЯ НИЖНЯЯ ПАНЕЛЬ ДЕЙСТВИЙ (ACTION BAR / BOTTOM SHEET)
                    // Мягко отделяется от контента тенью без жёсткой рамки.
                    // Primary — сочный изумрудный («Принять отчёт»), Secondary — прозрачная с красной обводкой («Отклонить»).
                    // =========================================================================
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 14.dp,
                        tonalElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (canApproveThisTask && (task.taskStatus == TaskStatus.ON_REVIEW || task.taskStatus == TaskStatus.IN_PROGRESS)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Второстепенная (Secondary) — прозрачная с красной обводкой
                                    OutlinedButton(
                                        onClick = { showRejectDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, crimsonSecondary.copy(alpha = 0.65f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .testTag("reject_report_button")
                                    ) {
                                        Text(
                                            text = "Отклонить",
                                            color = crimsonSecondary,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Основная (Primary) — премиальный изумрудный цвет
                                    Button(
                                        onClick = {
                                            onApproveReport(supervisorNote)
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = emeraldPrimary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1.35f)
                                            .height(46.dp)
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
                            }

                            if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                                val hasEnoughPhotosNow = task.hasEnoughPhotos()
                                val submitLabel = when {
                                    !hasEnoughPhotosNow -> "Прикрепите фото (${task.photoItems.size}/${task.minPhotoCount})"
                                    task.requiresApproval -> "Отправить отчёт на проверку"
                                    else -> "Завершить задачу"
                                }
                                Button(
                                    onClick = {
                                        onSubmitWorkerVolumeReport(null, photoComment)
                                        if (hasEnoughPhotosNow) onDismiss()
                                    },
                                    enabled = hasEnoughPhotosNow,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canApproveThisTask) {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        } else {
                                            emeraldPrimary
                                        },
                                        contentColor = if (canApproveThisTask) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            Color.White
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("submit_task_completion_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = submitLabel,
                                        style = MaterialTheme.typography.labelLarge,
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
    }

    selectedPhotoForPreview?.let { photoItem ->
        FullScreenPhotoDialog(
            photo = photoItem,
            canDelete = task.taskStatus != TaskStatus.APPROVED,
            onDismiss = { selectedPhotoForPreview = null },
            onDelete = {
                onDeletePhoto(photoItem.id)
                selectedPhotoForPreview = null
            }
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
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Причина возврата на доработку") },
            text = {
                OutlinedTextField(
                    value = rejectionReasonInput,
                    onValueChange = { rejectionReasonInput = it },
                    label = { Text("Укажите замечание *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rejection_reason_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReasonInput.isNotBlank()) {
                            onRejectReport(rejectionReasonInput)
                            showRejectDialog = false
                            onDismiss()
                        }
                    },
                    enabled = rejectionReasonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
}

@Composable
fun FullScreenPhotoDialog(
    photo: PhotoReportItem,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(photo.uriString) {
        PhotoCaptureUtils.loadBitmapFromUriOrPath(photo.uriString)
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
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = photo.comment,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
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
 * Диалог создания задачи:
 * - Полностью адаптируется под экран любого телефона (верхняя шапка и нижние кнопки всегда видны).
 * - Всегда отображает блок выбора исполнителя (чипы сотрудников + поле ввода ФИО исполнителя).
 * - Ручной ввод минимального количества фотографий для обязательного фотоотчёта.
 * - Постановка объёма выполнения задачи (при необходимости) с выбором единиц измерения (шт, кг, м² и др., включая ручной ввод своей единицы).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    activeEmployee: CorporateEmployee,
    availableEmployees: List<CorporateEmployee> = listOf(activeEmployee),
    currentEpochDay: Long,
    initialDueDateEpochDay: Long = currentEpochDay,
    onDismiss: () -> Unit,
    onCreate: (TaskDraft) -> Unit
) {
    val assignees = remember(availableEmployees, activeEmployee) {
        availableEmployees.ifEmpty { listOf(activeEmployee) }
    }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Выбор исполнителя: из выпадающего списка сотрудников или ввод ФИО вручную
    var selectedAssignee by remember(assignees) { mutableStateOf(assignees.first()) }
    var useCustomAssigneeName by remember { mutableStateOf(false) }
    var customAssigneeName by remember { mutableStateOf("") }
    var assigneeDropdownExpanded by remember { mutableStateOf(false) }

    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var selectedDueEpochDay by remember(initialDueDateEpochDay) { mutableStateOf(initialDueDateEpochDay) }
    var deadlineTime by remember { mutableStateOf("18:00") }

    // Обязательный фотоотчёт с возможностью ручного ввода минимального числа фото
    var requiresPhoto by remember { mutableStateOf(true) }
    var minPhotoCountText by remember { mutableStateOf("1") }

    // Объём выполнения задачи с выбором единиц измерения (шт, кг и другое)
    var hasVolumeRequirement by remember { mutableStateOf(false) }
    var targetVolumeText by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("шт") }
    var isCustomUnitSelected by remember { mutableStateOf(false) }
    var customUnitText by remember { mutableStateOf("") }

    var requiresApproval by remember { mutableStateOf(true) }
    var selectedRecurrence by remember { mutableStateOf(RecurrenceRule.NONE) }
    var subtasksRaw by remember { mutableStateOf("") }
    // Структурированный список пунктов чек-листа, где у каждого пункта свой объём и ед. изм.
    val draftChecklistItems = remember { mutableStateListOf<SubtaskItem>() }
    var draftItemTitle by remember { mutableStateOf("") }
    var draftItemVolText by remember { mutableStateOf("10") }
    var draftItemUnit by remember { mutableStateOf("шт") }

    var tagsRaw by remember { mutableStateOf("") }
    var geoLocation by remember { mutableStateOf(activeEmployee.siteName) }
    var showAdvancedOptions by remember { mutableStateOf(false) }

    val formScrollState = rememberScrollState()

    val saveTaskAction: () -> Unit = {
        val dueDay = selectedDueEpochDay
        val assigneeLabel = if (useCustomAssigneeName && customAssigneeName.isNotBlank()) {
            customAssigneeName.trim()
        } else {
            selectedAssignee.shortName
        }

        // Если пользователь ввёл текст в строку добавления пункта, но не нажал «+», сохраняем и его
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

        // Наименование задачи не подтягивается из пунктов чек-листа и не создаёт дублирующий пункт в чек-листе
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

        val parsedFromRawLines = subtasksRaw.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapIndexed { idx, line ->
                SubtaskItem.parseFromRawLine(
                    rawLine = line,
                    index = pendingDraftSubtasks.size + idx + 1,
                    defaultTargetVolume = if (resolvedTargetVolFromTask > 0.0 && pendingDraftSubtasks.isEmpty() && idx == 0) {
                        resolvedTargetVolFromTask
                    } else {
                        1.0
                    },
                    defaultUnit = resolvedUnitFromTask
                )
            }

        val allSubtaskObjects = pendingDraftSubtasks + parsedFromRawLines
        val encodedSubtasks = allSubtaskObjects.map { it.encode() }

        val resolvedTargetVol = when {
            allSubtaskObjects.isNotEmpty() -> allSubtaskObjects.sumOf { it.effectiveTargetVolume }
            resolvedTargetVolFromTask > 0.0 -> resolvedTargetVolFromTask
            else -> 0.0
        }

        val tags = tagsRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val resolvedMinPhotos = (minPhotoCountText.toIntOrNull() ?: 1).coerceAtLeast(1)

        onCreate(
            TaskDraft(
                title = effectiveTitle,
                description = description.trim(),
                assigneeId = selectedAssignee.id,
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
                tags = tags,
                subtasks = encodedSubtasks,
                geoLocation = geoLocation.trim()
            )
        )
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.84f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. ЗАКРЕПЛЁННАЯ ВЕРХНЯЯ ПАНЕЛЬ С КНОПКОЙ СОХРАНЕНИЯ ЗАДАНИЯ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Постановка задачи",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = saveTaskAction,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("top_save_task_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Сохранить", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 2. ПРОКРУЧИВАЕМАЯ ФОРМА
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(formScrollState)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Наименование задачи *") },
                            placeholder = { Text("Например: Монтаж опалубки перекрытия") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_task_title_input")
                        )

                        // БЛОК ВЫБОРА ИСПОЛНИТЕЛЯ: ВЫПАДАЮЩИЙ СПИСОК СОТРУДНИКОВ ИЛИ ВВОД ВРУЧНУЮ
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Исполнитель задания:",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = !useCustomAssigneeName,
                                            onClick = { useCustomAssigneeName = false },
                                            label = { Text("Из списка (${assignees.size})") },
                                            modifier = Modifier.testTag("assignee_mode_dropdown_chip")
                                        )
                                        FilterChip(
                                            selected = useCustomAssigneeName,
                                            onClick = { useCustomAssigneeName = true },
                                            label = { Text("Ввод вручную") },
                                            modifier = Modifier.testTag("custom_assignee_chip")
                                        )
                                    }
                                }

                                if (!useCustomAssigneeName) {
                                    // 1. Выпадающий список всех сотрудников участка
                                    ExposedDropdownMenuBox(
                                        expanded = assigneeDropdownExpanded,
                                        onExpandedChange = { assigneeDropdownExpanded = !assigneeDropdownExpanded },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = "${selectedAssignee.fullName} • ${selectedAssignee.position}",
                                            onValueChange = {},
                                            readOnly = true,
                                            singleLine = true,
                                            label = { Text("Выберите сотрудника из списка") },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = assigneeDropdownExpanded)
                                            },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                .fillMaxWidth()
                                                .testTag("assignee_dropdown_field")
                                        )
                                        ExposedDropdownMenu(
                                            expanded = assigneeDropdownExpanded,
                                            onDismissRequest = { assigneeDropdownExpanded = false }
                                        ) {
                                            assignees.forEach { emp ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(
                                                                text = emp.fullName,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Text(
                                                                text = "${emp.position} (${emp.role.titleRu}) • ${emp.groupName}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedAssignee = emp
                                                        useCustomAssigneeName = false
                                                        assigneeDropdownExpanded = false
                                                    },
                                                    modifier = Modifier.testTag("dropdown_assignee_${emp.id}")
                                                )
                                            }
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "+ Ввести ФИО исполнителя вручную...",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                onClick = {
                                                    useCustomAssigneeName = true
                                                    assigneeDropdownExpanded = false
                                                },
                                                modifier = Modifier.testTag("dropdown_custom_assignee_option")
                                            )
                                        }
                                    }

                                    // Быстрые чипы для выбора в одно касание
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        assignees.forEach { emp ->
                                            FilterChip(
                                                selected = selectedAssignee.id == emp.id,
                                                onClick = {
                                                    useCustomAssigneeName = false
                                                    selectedAssignee = emp
                                                },
                                                label = { Text("${emp.shortName} (${emp.role.titleRu})") },
                                                modifier = Modifier.testTag("select_assignee_${emp.id}")
                                            )
                                        }
                                    }
                                } else {
                                    // 2. Поле ручного ввода ФИО исполнителя (или бригады / субподрядчика)
                                    OutlinedTextField(
                                        value = customAssigneeName,
                                        onValueChange = { customAssigneeName = it },
                                        label = { Text("Введите ФИО сотрудника или бригаду вручную *") },
                                        placeholder = { Text("Например: Петров А.С. / Бригада монолитчиков") },
                                        trailingIcon = {
                                            if (customAssigneeName.isNotBlank()) {
                                                IconButton(onClick = { customAssigneeName = "" }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("custom_assignee_name_input")
                                    )
                                    Text(
                                        text = "Можно вписать любого сотрудника вручную или нажать на сотрудника из списка ниже для подстановки:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        assignees.forEach { emp ->
                                            FilterChip(
                                                selected = customAssigneeName.equals(emp.fullName, ignoreCase = true),
                                                onClick = {
                                                    customAssigneeName = emp.fullName
                                                    selectedAssignee = emp
                                                },
                                                label = { Text(emp.fullName) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // БЛОК ОБЪЁМА ВЫПОЛНЕНИЯ ЗАДАЧИ С ВЫБОРЫМ ЕДИНИЦ ИЗМЕРЕНИЯ (шт, кг и другое)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Объём выполнения задачи",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Отчётность в чек-листе по объёму (100% = выполнено)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = hasVolumeRequirement,
                                        onCheckedChange = { hasVolumeRequirement = it },
                                        modifier = Modifier.testTag("toggle_volume_requirement_switch")
                                    )
                                }

                                AnimatedVisibility(visible = hasVolumeRequirement) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = targetVolumeText,
                                            onValueChange = { targetVolumeText = it },
                                            label = { Text("Плановый объём работ *") },
                                            placeholder = { Text("Например: 150") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("create_task_target_volume_input")
                                        )

                                        Text(
                                            text = "Единица измерения:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium
                                        )

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
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
                                            FilterChip(
                                                selected = isCustomUnitSelected,
                                                onClick = { isCustomUnitSelected = true },
                                                label = { Text("Другое...") },
                                                modifier = Modifier.testTag("unit_chip_custom")
                                            )
                                        }

                                        AnimatedVisibility(visible = isCustomUnitSelected) {
                                            OutlinedTextField(
                                                value = customUnitText,
                                                onValueChange = { customUnitText = it },
                                                label = { Text("Своя единица измерения") },
                                                placeholder = { Text("Например: секц., рул., рейс, узел") },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("custom_unit_input")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Срок (выбор даты календаря) и приоритет
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                    Text(
                                        text = "Дата выполнения: ${ReportDocumentGenerator.formatEpochDay(selectedDueEpochDay)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(
                                            onClick = { selectedDueEpochDay -= 1L },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("-1 дн.", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = { selectedDueEpochDay += 1L },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("+1 дн.", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedDueEpochDay == currentEpochDay,
                                        onClick = { selectedDueEpochDay = currentEpochDay },
                                        label = { Text("На сегодня (${ReportDocumentGenerator.formatEpochDay(currentEpochDay)})") }
                                    )
                                    FilterChip(
                                        selected = selectedDueEpochDay == currentEpochDay + 1L,
                                        onClick = { selectedDueEpochDay = currentEpochDay + 1L },
                                        label = { Text("На завтра (${ReportDocumentGenerator.formatEpochDay(currentEpochDay + 1L)})") }
                                    )
                                    if (initialDueDateEpochDay != currentEpochDay && initialDueDateEpochDay != currentEpochDay + 1L) {
                                        FilterChip(
                                            selected = selectedDueEpochDay == initialDueDateEpochDay,
                                            onClick = { selectedDueEpochDay = initialDueDateEpochDay },
                                            label = { Text("Выбранная: ${ReportDocumentGenerator.formatEpochDay(initialDueDateEpochDay)}") }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TaskPriority.entries.forEach { prio ->
                                FilterChip(
                                    selected = selectedPriority == prio,
                                    onClick = { selectedPriority = prio },
                                    label = { Text(prio.titleRu) }
                                )
                            }
                        }

                        // БЛОК ОБЯЗАТЕЛЬНОГО ФОТООТЧЁТА С РУЧНЫМ ВВОДОМ МИНИМАЛЬНОГО ЧИСЛА ФОТО
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                    Text(
                                        text = "Обязательный фотоотчёт",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Switch(
                                        checked = requiresPhoto,
                                        onCheckedChange = { requiresPhoto = it }
                                    )
                                }

                                AnimatedVisibility(visible = requiresPhoto) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val cur = minPhotoCountText.toIntOrNull() ?: 1
                                                    minPhotoCountText = (cur - 1).coerceAtLeast(1).toString()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Уменьшить")
                                            }
                                            OutlinedTextField(
                                                value = minPhotoCountText,
                                                onValueChange = { valDigits ->
                                                    minPhotoCountText = valDigits.filter { it.isDigit() }
                                                },
                                                label = { Text("Мин. кол-во фото (вручную)") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("min_photo_manual_input")
                                            )
                                            IconButton(
                                                onClick = {
                                                    val cur = minPhotoCountText.toIntOrNull() ?: 1
                                                    minPhotoCountText = (cur + 1).coerceAtMost(99).toString()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Увеличить")
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(1, 2, 3, 5, 10).forEach { preset ->
                                                FilterChip(
                                                    selected = (minPhotoCountText.toIntOrNull() ?: 1) == preset,
                                                    onClick = { minPhotoCountText = preset.toString() },
                                                    label = { Text("$preset фото") }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // КОНСТРУКТОР ПУНКТОВ ЧЕК-ЛИСТА: ПОЛЕ ТЕКСТА + КНОПКА "+" И ЯЧЕЙКА КОЛИЧЕСТВА НАПРОТИВ КАЖДОЙ СТРОКИ
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Чек-лист задачи (пункт + количество напротив строки)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Введите наименование пункта, укажите количество в ячейке напротив и нажмите «+» для добавления новой строки.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Список добавленных строк чек-листа: наименование слева и ячейка количества прямо напротив строки
                                if (draftChecklistItems.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        draftChecklistItems.forEachIndexed { idx, item ->
                                            var rowQtyText by remember(item.id) {
                                                mutableStateOf(SubtaskItem.formatVolumeNumber(item.effectiveTargetVolume))
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${idx + 1}. ${item.title}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    OutlinedTextField(
                                                        value = rowQtyText,
                                                        onValueChange = { newVal ->
                                                            rowQtyText = newVal
                                                            val parsed = newVal.replace(",", ".").toDoubleOrNull()
                                                            if (parsed != null && parsed > 0.0) {
                                                                draftChecklistItems[idx] = item.copy(targetVolume = parsed)
                                                            }
                                                        },
                                                        label = { Text("Кол-во (${item.effectiveUnit})") },
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        modifier = Modifier
                                                            .width(102.dp)
                                                            .testTag("draft_subtask_qty_row_$idx")
                                                    )
                                                    IconButton(
                                                        onClick = { draftChecklistItems.removeAt(idx) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = "Удалить строку",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Новая строка: поле ввода текста пункта + ячейка количества напротив + кнопка "+"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = draftItemTitle,
                                        onValueChange = { draftItemTitle = it },
                                        label = { Text("Наименование пункта") },
                                        placeholder = { Text("Введите пункт...") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("draft_subtask_title_input")
                                    )
                                    OutlinedTextField(
                                        value = draftItemVolText,
                                        onValueChange = { draftItemVolText = it },
                                        label = { Text("Кол-во ($draftItemUnit)") },
                                        placeholder = { Text("10") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .width(96.dp)
                                            .testTag("draft_subtask_volume_input")
                                    )
                                    FilledIconButton(
                                        onClick = {
                                            if (draftItemTitle.isNotBlank()) {
                                                val v = (draftItemVolText.replace(",", ".").toDoubleOrNull() ?: 1.0)
                                                    .coerceAtLeast(0.1)
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
                                        modifier = Modifier
                                            .size(44.dp)
                                            .testTag("add_draft_subtask_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Добавить пункт в новой строке")
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    STANDARD_VOLUME_UNITS.forEach { uItem ->
                                        FilterChip(
                                            selected = draftItemUnit == uItem,
                                            onClick = { draftItemUnit = uItem },
                                            label = { Text(uItem) }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Описание / примечание (необязательно)") },
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_task_desc_input")
                        )

                        // Кнопка сохранения прямо внутри прокручиваемой области
                        Button(
                            onClick = saveTaskAction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("inline_save_task_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Сохранить задание", fontWeight = FontWeight.Bold)
                        }

                        // Дополнительные настройки в подменю
                        TextButton(
                            onClick = { showAdvancedOptions = !showAdvancedOptions },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (showAdvancedOptions) "Скрыть доп. параметры"
                                else "Доп. параметры (повтор, время, участок)"
                            )
                        }

                        AnimatedVisibility(visible = showAdvancedOptions) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Повторяемость:", style = MaterialTheme.typography.labelMedium)
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
                                            label = { Text(rule.titleRu) }
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = deadlineTime,
                                        onValueChange = { deadlineTime = it },
                                        label = { Text("Время") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = geoLocation,
                                        onValueChange = { geoLocation = it },
                                        label = { Text("Участок / зона") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.5f)
                                    )
                                }
                                OutlinedTextField(
                                    value = tagsRaw,
                                    onValueChange = { tagsRaw = it },
                                    label = { Text("Теги через запятую") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 3. ЗАКРЕПЛЁННАЯ НИЖНЯЯ ПАНЕЛЬ КНОПОК (всегда видна на любом телефоне!)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = saveTaskAction,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(46.dp)
                                .testTag("confirm_create_task_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Сохранить задание", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
