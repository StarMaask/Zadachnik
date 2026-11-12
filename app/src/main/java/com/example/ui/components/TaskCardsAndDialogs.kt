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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListBulleted
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import android.graphics.SurfaceTexture
import android.view.TextureView
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
    onAddPhotosBatch: (items: List<Pair<String, String>>, gpsCoords: String) -> Unit = { items, gps ->
        items.forEach { (uri, comment) -> onAddPhoto(uri, gps, comment) }
    },
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
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showExtraSections by remember { mutableStateOf(false) }

    // Современная студия фотосъёмки СМР без промежуточного подтверждения каждого снимка
    var continuousShootingMode by remember { mutableStateOf(true) }
    var burstShotCount by remember { mutableIntStateOf(0) }
    var showModernCameraDialog by remember { mutableStateOf(false) }
    var selectedPhotoForPreview by remember { mutableStateOf<PhotoReportItem?>(null) }

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
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // В любом случае открываем современное окно камеры СМР без промежуточных подтверждений
        showModernCameraDialog = true
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
                                // Строка 1: Исполнители (один или несколько)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (task.allAssigneeNames.size > 1) {
                                            "Исполнители (${task.allAssigneeNames.size})"
                                        } else {
                                            "Исполнитель"
                                        },
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
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 195.dp)
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
                                        Text(
                                            text = "Отметьте одного или нескольких исполнителей:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            assignees.forEach { emp ->
                                                val isChecked = detailSelectedAssigneeIds.contains(emp.id)
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
                                                            Text(
                                                                text = emp.fullName,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Text(
                                                                text = "${emp.position} • ${emp.groupName}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = detailManualAssigneeText,
                                            onValueChange = { detailManualAssigneeText = it },
                                            label = { Text("Доп. ФИО вручную (через запятую)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
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
                                                Text("Сохранить исполнителей")
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
                                                showModernCameraDialog = true
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
                            if (canApproveThisTask && task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!hasEnoughPhotosNow) {
                                        OutlinedButton(
                                            onClick = {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .height(44.dp)
                                                .testTag("bottom_bar_open_camera_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
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
                                        task.taskStatus == TaskStatus.ON_REVIEW -> "Обновить отчёт (На проверке)"
                                        task.requiresApproval -> "Подтвердить выполнение"
                                        else -> "Завершить задачу"
                                    }
                                    Button(
                                        onClick = {
                                            onSubmitWorkerVolumeReport(null, photoComment)
                                            onDismiss()
                                        },
                                        enabled = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (canApproveThisTask) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                            } else {
                                                emeraldPrimary
                                            },
                                            contentColor = if (canApproveThisTask) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.White
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("submit_task_completion_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
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
    }

    if (showModernCameraDialog) {
        ModernPhotoCaptureDialog(
            taskTitle = task.title,
            authorName = activeEmployee.shortName,
            gpsCoords = photoGps,
            initialComment = photoComment,
            existingPhotoCount = task.photoItems.size,
            minRequiredPhotos = task.minPhotoCount.toInt().coerceAtLeast(1),
            onOpenSystemCamera = {
                cameraLauncher.launch(null)
            },
            onSaveCapturedPhotos = { capturedItems ->
                onAddPhotosBatch(capturedItems, photoGps)
                photoComment = ""
                showModernCameraDialog = false
            },
            onDismiss = { showModernCameraDialog = false }
        )
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
 * Современная студия фотосъёмки СМР:
 * - Снимает кадры мгновенно по нажатию на кнопку затвора (или касанием видоискателя) БЕЗ промежуточного диалога подтверждения каждого снимка.
 * - Отображает живой видоискатель камеры (при наличии разрешения и камеры) с инженерным штампом СМР (дата/время, GPS, автор, задача).
 * - Накапливает серию снимков в нижней ленте миниатюр с возможностью удалить лишний кадр.
 * - Завершает фотосъёмку и прикрепляет все сделанные кадры по нажатию кнопки «Сохранить».
 */
@Suppress("DEPRECATION")
@Composable
fun ModernPhotoCaptureDialog(
    taskTitle: String,
    authorName: String,
    gpsCoords: String,
    initialComment: String,
    existingPhotoCount: Int,
    minRequiredPhotos: Int,
    onOpenSystemCamera: () -> Unit,
    onSaveCapturedPhotos: (List<Pair<String, String>>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    val capturedSessionItems = remember { mutableStateListOf<Pair<String, String>>() }
    var captionInput by remember { mutableStateOf(initialComment) }
    var tapToShoot by remember { mutableStateOf(true) }
    var lastShotFlashTrigger by remember { mutableIntStateOf(0) }
    var liveTextureView by remember { mutableStateOf<TextureView?>(null) }

    val takeInstantShot: () -> Unit = {
        val shotNum = existingPhotoCount + capturedSessionItems.size + 1
        val effectiveCaption = captionInput.trim().ifBlank { "Фотофиксация СМР #$shotNum" }
        val liveBitmap = runCatching { liveTextureView?.bitmap }.getOrNull()
        val savedPath = if (liveBitmap != null && liveBitmap.width > 32 && liveBitmap.height > 32) {
            PhotoCaptureUtils.saveCameraBitmapWithStamp(
                context = context,
                sourceBitmap = liveBitmap,
                taskTitle = taskTitle,
                authorName = authorName,
                gpsCoords = gpsCoords,
                comment = effectiveCaption
            )
        } else {
            PhotoCaptureUtils.generatePhotoStampFile(
                context = context,
                taskTitle = taskTitle,
                authorName = authorName,
                gpsCoords = gpsCoords,
                comment = effectiveCaption,
                photoIndex = shotNum
            )
        }
        capturedSessionItems.add(savedPath to effectiveCaption)
        lastShotFlashTrigger++
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
                .padding(horizontal = 12.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF090D16),
                shadowElevation = 18.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. ВЕРХНЯЯ ПАНЕЛЬ КАМЕРЫ СМР
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "КАМЕРА СМР",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = taskTitle.ifBlank { "Фотофиксация выполнения работ" },
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val totalWithSession = existingPhotoCount + capturedSessionItems.size
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (totalWithSession >= minRequiredPhotos) {
                                    Color(0xFF10B981).copy(alpha = 0.22f)
                                } else {
                                    Color(0xFF3B82F6).copy(alpha = 0.22f)
                                }
                            ) {
                                Text(
                                    text = "Кадров: $totalWithSession / $minRequiredPhotos",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalWithSession >= minRequiredPhotos) {
                                        Color(0xFF34D399)
                                    } else {
                                        Color(0xFF60A5FA)
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            if (capturedSessionItems.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        onSaveCapturedPhotos(capturedSessionItems.toList())
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("top_save_captured_photos_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Готово (${capturedSessionItems.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Закрыть камеру",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                // 2. ОБЛАСТЬ ВИДОИСКАТЕЛЯ С ЖИВЫМ ШТАМПОМ СМР И ТАПОМ ДЛЯ СЪЁМКИ
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF111827),
                                    Color(0xFF1E293B),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                        .clickable(enabled = tapToShoot) {
                            takeInstantShot()
                        }
                        .testTag("camera_viewfinder_box")
                ) {
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { ctx ->
                                TextureView(ctx).apply {
                                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                        private var cameraInstance: android.hardware.Camera? = null

                                        override fun onSurfaceTextureAvailable(
                                            surface: SurfaceTexture,
                                            width: Int,
                                            height: Int
                                        ) {
                                            runCatching {
                                                cameraInstance = android.hardware.Camera.open()?.apply {
                                                    setDisplayOrientation(90)
                                                    setPreviewTexture(surface)
                                                    startPreview()
                                                }
                                            }
                                        }

                                        override fun onSurfaceTextureSizeChanged(
                                            surface: SurfaceTexture,
                                            width: Int,
                                            height: Int
                                        ) = Unit

                                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                            runCatching {
                                                cameraInstance?.stopPreview()
                                                cameraInstance?.release()
                                            }
                                            cameraInstance = null
                                            return true
                                        }

                                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                                    }
                                    liveTextureView = this
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Инженерная прицельная сетка и рамка фокуса
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .drawBehind {
                                val stroke = 2.dp.toPx()
                                val cornerLen = 28.dp.toPx()
                                val bracketColor = Color.White.copy(alpha = 0.55f)
                                // Верхний левый угол
                                drawLine(bracketColor, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(cornerLen, 0f), stroke)
                                drawLine(bracketColor, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, cornerLen), stroke)
                                // Верхний правый угол
                                drawLine(bracketColor, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width - cornerLen, 0f), stroke)
                                drawLine(bracketColor, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width, cornerLen), stroke)
                                // Нижний левый угол
                                drawLine(bracketColor, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(cornerLen, size.height), stroke)
                                drawLine(bracketColor, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(0f, size.height - cornerLen), stroke)
                                // Нижний правый угол
                                drawLine(bracketColor, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width - cornerLen, size.height), stroke)
                                drawLine(bracketColor, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height - cornerLen), stroke)
                            }
                    )

                    // Верхний бейдж режима съёмки без подтверждения
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.Black.copy(alpha = 0.55f)
                        ) {
                            Text(
                                text = if (lastShotFlashTrigger > 0) {
                                    "✓ Кадр #${capturedSessionItems.size} снят мгновенно — продолжайте съёмку или нажмите «Сохранить»"
                                } else {
                                    "Нажмите на затвор или экран для мгновенного снимка без подтверждения"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Центральный индикатор видоискателя
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Нижний живой штамп СМР прямо на видоискателе
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.82f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ШТАМП СМР • ${PhotoCaptureUtils.nowStampText()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF60A5FA),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Исп.: $authorName • Участок: ${gpsCoords.ifBlank { "Участок №1" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(
                                onClick = onOpenSystemCamera,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("system_camera_fallback_button")
                            ) {
                                Text(
                                    text = "Сист. камера",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF93C5FD)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. ЛЕНТА ОТСНЯТЫХ КАДРОВ СЕССИИ + ПОДПИСЬ
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (capturedSessionItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            capturedSessionItems.forEachIndexed { idx, (path, cap) ->
                                val thumb = remember(path) {
                                    PhotoCaptureUtils.loadBitmapFromUriOrPath(path)
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f)),
                                    modifier = Modifier.size(width = 110.dp, height = 66.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (thumb != null) {
                                            Image(
                                                bitmap = thumb.asImageBitmap(),
                                                contentDescription = cap,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.Black.copy(alpha = 0.65f),
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                text = "#${idx + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { capturedSessionItems.removeAt(idx) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(22.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Удалить кадр",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TextField(
                        value = captionInput,
                        onValueChange = { captionInput = it },
                        placeholder = {
                            Text(
                                "Примечание на фотоштамп (необязательно)...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.75f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("camera_caption_input")
                    )

                    // 4. ПАНЕЛЬ ЗАТВОРА (БЕЗ ПОДТВЕРЖДЕНИЯ КАЖДОГО СНИМКА) + КНОПКА «СОХРАНИТЬ» ДЛЯ ЗАВЕРШЕНИЯ ФОТОСЪЁМКИ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("Отмена", color = Color.White)
                        }

                        // Кнопка мгновенного спуска затвора (можно нажимать многократно без подтверждений)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable { takeInstantShot() }
                                .testTag("shutter_capture_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Сделать снимок",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Кнопка «Сохранить» для завершения фотосъёмки
                        Button(
                            onClick = {
                                if (capturedSessionItems.isEmpty()) {
                                    takeInstantShot()
                                }
                                onSaveCapturedPhotos(capturedSessionItems.toList())
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("save_captured_photos_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (capturedSessionItems.isEmpty()) {
                                    "Сохранить"
                                } else {
                                    "Сохранить (${capturedSessionItems.size})"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
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
    val assignees = remember(availableEmployees, activeEmployee) {
        availableEmployees.ifEmpty { listOf(activeEmployee) }
    }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showDescriptionInput by remember { mutableStateOf(false) }

    // Выбор одного или нескольких исполнителей: из списка с чекбоксами или ввод ФИО вручную
    val selectedAssigneeIds = remember(assignees) {
        mutableStateListOf(assignees.first().id)
    }
    val selectedAssignees = remember(assignees, selectedAssigneeIds.toList()) {
        assignees.filter { it.id in selectedAssigneeIds }.ifEmpty { listOf(assignees.first()) }
    }
    val selectedAssignee = selectedAssignees.first()
    var useCustomAssigneeName by remember { mutableStateOf(false) }
    var customAssigneeName by remember { mutableStateOf("") }
    var assigneeDropdownExpanded by remember { mutableStateOf(false) }

    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var priorityAccordionExpanded by remember { mutableStateOf(true) }

    var selectedDueEpochDay by remember(initialDueDateEpochDay) { mutableStateOf(initialDueDateEpochDay) }
    var calendarExpanded by remember { mutableStateOf(false) }
    var displayedMonthOffset by remember { mutableIntStateOf(0) }
    var deadlineTime by remember { mutableStateOf("18:00") }

    // Обязательный фотоотчёт с компактным степпером
    var requiresPhoto by remember { mutableStateOf(true) }
    var minPhotoCountText by remember { mutableStateOf("1") }

    // Чек-лист (аккордеон)
    var checklistAccordionExpanded by remember { mutableStateOf(true) }
    val draftChecklistItems = remember { mutableStateListOf<SubtaskItem>() }
    var draftItemTitle by remember { mutableStateOf("") }
    var draftItemVolText by remember { mutableStateOf("10") }
    var draftItemUnit by remember { mutableStateOf("шт") }
    var draftUnitMenuExpanded by remember { mutableStateOf(false) }

    // Продвинутые опции в самом низу
    var showAdvancedOptions by remember { mutableStateOf(false) }
    var hasVolumeRequirement by remember { mutableStateOf(false) }
    var targetVolumeText by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("шт") }
    var isCustomUnitSelected by remember { mutableStateOf(false) }
    var customUnitText by remember { mutableStateOf("") }
    var requiresApproval by remember { mutableStateOf(true) }
    var selectedRecurrence by remember { mutableStateOf(RecurrenceRule.NONE) }
    var subtasksRaw by remember { mutableStateOf("") }
    var tagsRaw by remember { mutableStateOf("") }
    var geoLocation by remember { mutableStateOf(activeEmployee.siteName) }

    val formScrollState = rememberScrollState()

    val saveTaskAction: () -> Unit = {
        val dueDay = selectedDueEpochDay
        val assigneeLabel = if (useCustomAssigneeName && customAssigneeName.isNotBlank()) {
            customAssigneeName.trim()
        } else {
            selectedAssignees.joinToString(", ") { it.shortName }
        }

        // Если пользователь ввёл текст в строку добавления пункта, но не нажал «+ Добавить пункт», сохраняем и его
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
                tags = tags,
                subtasks = encodedSubtasks,
                geoLocation = geoLocation.trim()
            )
        )
        onDismiss()
    }

    val softFieldBg = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = if (tokens.isDark) 0.42f else 0.55f
    )
    val softBlockBg = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = if (tokens.isDark) 0.26f else 0.35f
    )

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
                .padding(horizontal = 12.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 18.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. ШАПКА: ТОЛЬКО ЗАГОЛОВОК «ПОСТАНОВКА ЗАДАЧИ» И КРЕСТИК ЗАКРЫТИЯ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Постановка задачи",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Новое производственное задание СМР",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(softFieldBg)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 2. ПРОКРУЧИВАЕМОЕ ТЕЛО ФОРМЫ (БЕЗ ПРОМЕЖУТОЧНЫХ КНОПОК СОХРАНЕНИЯ И ЖЁСТКИХ РАМОК)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(formScrollState)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // БЛОК 2.1: НАЗВАНИЕ ЗАДАЧИ (Главный акцент, без жёсткой рамки, мягкая подложка)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Название задачи",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextField(
                                value = title,
                                onValueChange = { title = it },
                                placeholder = {
                                    Text(
                                        text = "Например: Монтаж опалубки перекрытия...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    unfocusedContainerColor = softFieldBg,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("create_task_title_input")
                            )
                        }

                        // БЛОК 2.2: ИСПОЛНИТЕЛИ (Мультивыбор одного или нескольких сотрудников + опция ручного ввода)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedAssignees.size > 1) {
                                        "Исполнители (выбрано: ${selectedAssignees.size})"
                                    } else {
                                        "Исполнители (можно выбрать несколько)"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier
                                                .height(28.dp)
                                                .testTag("select_all_assignees_button")
                                        ) {
                                            Text(
                                                text = if (selectedAssigneeIds.size == assignees.size) "Сбросить" else "Все (${assignees.size})",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = { useCustomAssigneeName = !useCustomAssigneeName },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag(if (useCustomAssigneeName) "assignee_mode_dropdown_chip" else "custom_assignee_chip")
                                    ) {
                                        Text(
                                            text = if (useCustomAssigneeName) {
                                                "Из списка (${assignees.size})"
                                            } else {
                                                "+ Ввести ФИО"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (!useCustomAssigneeName) {
                                ExposedDropdownMenuBox(
                                    expanded = assigneeDropdownExpanded,
                                    onExpandedChange = { assigneeDropdownExpanded = !assigneeDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = softFieldBg,
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                            .testTag("assignee_dropdown_field")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            val avatarBadgeText = if (selectedAssignees.size > 1) {
                                                "${selectedAssignees.size}"
                                            } else {
                                                selectedAssignee.fullName
                                                    .split(" ")
                                                    .filter { it.isNotBlank() }
                                                    .take(2)
                                                    .joinToString("") { it.take(1).uppercase() }
                                                    .ifBlank { "ИС" }
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = avatarBadgeText,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = selectedAssignees.joinToString(", ") { it.fullName },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (selectedAssignees.size > 1) {
                                                        "Задача упадёт всем выбранным исполнителям (${selectedAssignees.size} чел.)"
                                                    } else {
                                                        "${selectedAssignee.position} • ${selectedAssignee.role.titleRu}"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = assigneeDropdownExpanded)
                                        }
                                    }

                                    ExposedDropdownMenu(
                                        expanded = assigneeDropdownExpanded,
                                        onDismissRequest = { assigneeDropdownExpanded = false }
                                    ) {
                                        assignees.forEach { emp ->
                                            val isChecked = selectedAssigneeIds.contains(emp.id)
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = null
                                                    )
                                                },
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
                                                    if (isChecked) {
                                                        if (selectedAssigneeIds.size > 1) {
                                                            selectedAssigneeIds.remove(emp.id)
                                                        }
                                                    } else {
                                                        selectedAssigneeIds.add(emp.id)
                                                    }
                                                    useCustomAssigneeName = false
                                                },
                                                modifier = Modifier.testTag("dropdown_assignee_${emp.id}")
                                            )
                                        }
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "✓ Готово (выбрано: ${selectedAssignees.size})",
                                                    color = Color(0xFF10B981),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            onClick = {
                                                assigneeDropdownExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "+ Ввести ФИО исполнителей вручную...",
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

                                // Компактные чипы быстрого переключения исполнителей в один тап
                                if (assignees.size > 1) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        assignees.forEach { emp ->
                                            val isSelected = selectedAssigneeIds.contains(emp.id)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    if (isSelected) {
                                                        if (selectedAssigneeIds.size > 1) {
                                                            selectedAssigneeIds.remove(emp.id)
                                                        }
                                                    } else {
                                                        selectedAssigneeIds.add(emp.id)
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = emp.shortName,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                } else null,
                                                modifier = Modifier.testTag("quick_assignee_chip_${emp.id}")
                                            )
                                        }
                                    }
                                }
                            } else {
                                TextField(
                                    value = customAssigneeName,
                                    onValueChange = { customAssigneeName = it },
                                    placeholder = {
                                        Text("Введите одно или несколько ФИО через запятую...")
                                    },
                                    trailingIcon = {
                                        if (customAssigneeName.isNotBlank()) {
                                            IconButton(onClick = { customAssigneeName = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        unfocusedContainerColor = softFieldBg,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_assignee_name_input")
                                )
                            }
                        }

                        // БЛОК 2.3: СРОКИ (ДАТА) — Горизонтальный скролл дат + кнопка разворота календаря с задачами
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = softBlockBg,
                            modifier = Modifier.fillMaxWidth()
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Срок выполнения",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = ReportDocumentGenerator.formatEpochDay(selectedDueEpochDay) +
                                                    when (selectedDueEpochDay) {
                                                        currentEpochDay -> " • Сегодня"
                                                        currentEpochDay + 1L -> " • Завтра"
                                                        else -> ""
                                                    },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Кнопка разворота полного календаря с задачами
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (calendarExpanded) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        } else {
                                            softFieldBg
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { calendarExpanded = !calendarExpanded }
                                            .testTag("create_task_calendar_expand_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "Развернуть календарь",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (calendarExpanded) "Свернуть" else "Календарь",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(
                                                imageVector = if (calendarExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Горизонтальный скролл дат (пилюли дней с индикаторами задач)
                                val stripDays = remember(currentEpochDay) {
                                    (-1L..12L).map { currentEpochDay + it }
                                }
                                val dayNamesMondayFirst = remember {
                                    listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    stripDays.forEach { dayEpoch ->
                                        val isSelected = dayEpoch == selectedDueEpochDay
                                        val isToday = dayEpoch == currentEpochDay
                                        val localDate = remember(dayEpoch) {
                                            java.time.LocalDate.ofEpochDay(dayEpoch)
                                        }
                                        val dayOfMonth = localDate.dayOfMonth
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
                                                .width(54.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { selectedDueEpochDay = dayEpoch }
                                                .testTag("date_pill_$dayEpoch")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = dow,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                                Text(
                                                    text = dayOfMonth.toString(),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                if (tasksCountOnDay > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                                else MaterialTheme.colorScheme.primary
                                                            )
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.height(5.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                // Развёрнутый календарь месяца со списком задач на выбранную дату
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
                                    val tasksForSelectedDate = remember(existingTasks, selectedDueEpochDay) {
                                        existingTasks.filter { it.dueDateEpochDay == selectedDueEpochDay }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surface,
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

                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { dName ->
                                                    Text(
                                                        text = dName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.weight(1f)
                                                    )
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
                                                            val countOnCell = existingTasks.count { it.dueDateEpochDay == cellEpochDay }
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(36.dp)
                                                                    .padding(2.dp)
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
                                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                    Text(
                                                                        text = dayNum.toString(),
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        fontWeight = if (isSel || isTodayCell) FontWeight.Bold else FontWeight.Normal,
                                                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                                                        else MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                    if (countOnCell > 0) {
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .size(4.dp)
                                                                                .clip(CircleShape)
                                                                                .background(
                                                                                    if (isSel) MaterialTheme.colorScheme.onPrimary
                                                                                    else MaterialTheme.colorScheme.primary
                                                                                )
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            Spacer(modifier = Modifier.weight(1f).height(36.dp))
                                                        }
                                                    }
                                                }
                                            }

                                            // Задачи на выбранную дату внутри календаря
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            Text(
                                                text = "Задачи на ${ReportDocumentGenerator.formatEpochDay(selectedDueEpochDay)} (${tasksForSelectedDate.size}):",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (tasksForSelectedDate.isEmpty()) {
                                                Text(
                                                    text = "На выбранную дату задач пока не назначено.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                                )
                                            } else {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    tasksForSelectedDate.take(4).forEach { tItem ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(softBlockBg)
                                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "• ${tItem.title}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Medium,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            Text(
                                                                text = tItem.assigneeName,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary
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

                        // 3. НАСТРОЙКИ ВЫПОЛНЕНИЯ (АККОРДЕОНЫ — СВОРАЧИВАЕМЫЕ БЛОКИ)

                        // АККОРДЕОН 3.1: УРОВЕНЬ ВАЖНОСТИ (ПРИОРИТЕТ) — Компактный Segmented Control в одну строку
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = softBlockBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { priorityAccordionExpanded = !priorityAccordionExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = priorityColor(selectedPriority),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Приоритет задачи",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = priorityColor(selectedPriority).copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = selectedPriority.titleRu,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = priorityColor(selectedPriority),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                        Icon(
                                            imageVector = if (priorityAccordionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = priorityAccordionExpanded) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = softFieldBg,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp)
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
                                                    color = if (isSel) {
                                                        MaterialTheme.colorScheme.surface
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                    shadowElevation = if (isSel) 2.dp else 0.dp,
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
                            }
                        }

                        // АККОРДЕОН 3.2: ОБЯЗАТЕЛЬНЫЙ ФОТООТЧЁТ (Переключатель + компактный степпер без дублирующих чипов)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = softBlockBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Обязательный фотоотчёт",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (requiresPhoto) {
                                                Text(
                                                    text = "Минимум ${minPhotoCountText.ifBlank { "1" }} фото для сдачи на проверку",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Switch(
                                        checked = requiresPhoto,
                                        onCheckedChange = { requiresPhoto = it }
                                    )
                                }

                                AnimatedVisibility(visible = requiresPhoto) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(softFieldBg)
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Минимальное количество фото:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surface,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        val cur = minPhotoCountText.toIntOrNull() ?: 1
                                                        minPhotoCountText = (cur - 1).coerceAtLeast(1).toString()
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Remove,
                                                        contentDescription = "Уменьшить",
                                                        modifier = Modifier.size(16.dp)
                                                    )
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
                                                    .width(42.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .padding(vertical = 6.dp)
                                                    .testTag("min_photo_manual_input")
                                            )

                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surface,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        val cur = minPhotoCountText.toIntOrNull() ?: 1
                                                        minPhotoCountText = (cur + 1).coerceAtMost(99).toString()
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Увеличить",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // АККОРДЕОН 3.3: ЧЕК-ЛИСТ (Чистый список + интегрированный выбор единиц измерения прямо в строке)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = softBlockBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { checklistAccordionExpanded = !checklistAccordionExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FormatListBulleted,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Чек-лист работ и объёмы",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (draftChecklistItems.isEmpty()) {
                                                    "Добавьте этапы работ с плановым количеством"
                                                } else {
                                                    "Пунктов: ${draftChecklistItems.size}"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (checklistAccordionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                AnimatedVisibility(visible = checklistAccordionExpanded) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Добавленные строки чек-листа
                                        draftChecklistItems.forEachIndexed { idx, item ->
                                            var rowQtyText by remember(item.id) {
                                                mutableStateOf(SubtaskItem.formatVolumeNumber(item.effectiveTargetVolume))
                                            }
                                            var rowUnitExpanded by remember(item.id) { mutableStateOf(false) }

                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
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

                                                    // Компактное поле количества с интегрированным выбором единицы измерения
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(9.dp))
                                                            .background(softFieldBg)
                                                            .padding(horizontal = 8.dp, vertical = 5.dp)
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
                                                            textStyle = MaterialTheme.typography.labelLarge.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.End,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            ),
                                                            modifier = Modifier
                                                                .width(44.dp)
                                                                .testTag("draft_subtask_qty_row_$idx")
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
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
                                                                    contentDescription = "Единица измерения",
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
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Удалить строку",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Строка ввода нового пункта чек-листа: название + интегрированное поле количества и ед. изм.
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = softFieldBg,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    if (draftItemTitle.isEmpty()) {
                                                        Text(
                                                            text = "Название пункта работ...",
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

                                                // Маленькое поле количества + интегрированный выпадающий список единиц измерения
                                                Surface(
                                                    shape = RoundedCornerShape(9.dp),
                                                    color = MaterialTheme.colorScheme.surface
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        BasicTextField(
                                                            value = draftItemVolText,
                                                            onValueChange = { draftItemVolText = it },
                                                            singleLine = true,
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            textStyle = MaterialTheme.typography.labelLarge.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.End,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            ),
                                                            modifier = Modifier
                                                                .width(42.dp)
                                                                .testTag("draft_subtask_volume_input")
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
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
                                                                    contentDescription = "Выбрать единицу измерения",
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
                                        }

                                        // Аккуратная текстовая кнопка «+ Добавить пункт»
                                        TextButton(
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
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("add_draft_subtask_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "+ Добавить пункт",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. ДОПОЛНИТЕЛЬНО: ОПИСАНИЕ ПОД КНОПКОЙ «+ ДОБАВИТЬ ОПИСАНИЕ» И «ПРОДВИНУТЫЕ ОПЦИИ» ВНИЗУ
                        if (!showDescriptionInput && description.isBlank()) {
                            TextButton(
                                onClick = { showDescriptionInput = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("toggle_description_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+ Добавить описание / примечание",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Описание / примечание",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = {
                                            description = ""
                                            showDescriptionInput = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("Скрыть", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                TextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    placeholder = {
                                        Text("Технические требования, отметки по чертежу, особые указания...")
                                    },
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        unfocusedContainerColor = softFieldBg,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("create_task_desc_input")
                                )
                            }
                        }

                        // ПРОДВИНУТЫЕ ОПЦИИ (В самом низу перед футером)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = softBlockBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showAdvancedOptions = !showAdvancedOptions }
                                        .testTag("toggle_advanced_options_button"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Продвинутые опции (повтор, время, участок, объём)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                AnimatedVisibility(visible = showAdvancedOptions) {
                                    Column(
                                        modifier = Modifier.padding(top = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextField(
                                                value = deadlineTime,
                                                onValueChange = { deadlineTime = it },
                                                label = { Text("Время сдачи") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = softFieldBg,
                                                    unfocusedContainerColor = softFieldBg,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextField(
                                                value = geoLocation,
                                                onValueChange = { geoLocation = it },
                                                label = { Text("Участок / зона") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = softFieldBg,
                                                    unfocusedContainerColor = softFieldBg,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent
                                                ),
                                                modifier = Modifier.weight(1.5f)
                                            )
                                        }

                                        Text("Повторяемость:", style = MaterialTheme.typography.labelSmall)
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

                                        // Опциональный общий объём задачи (если задаётся без чек-листа)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Задать единый объём на всю задачу",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                            Switch(
                                                checked = hasVolumeRequirement,
                                                onCheckedChange = { hasVolumeRequirement = it },
                                                modifier = Modifier.testTag("toggle_volume_requirement_switch")
                                            )
                                        }

                                        AnimatedVisibility(visible = hasVolumeRequirement) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextField(
                                                    value = targetVolumeText,
                                                    onValueChange = { targetVolumeText = it },
                                                    label = { Text("Плановый объём работ") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = TextFieldDefaults.colors(
                                                        focusedContainerColor = softFieldBg,
                                                        unfocusedContainerColor = softFieldBg,
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
                                }
                            }
                        }
                    }

                    // 5. ФИКСИРОВАННЫЙ ФУТЕР С МЯГКОЙ ТЕНЬЮ И ДВУМЯ КНОПКАМИ: «ОТМЕНА» И «СОЗДАТЬ ЗАДАЧУ»
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = softFieldBg,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onDismiss() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Отмена",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = saveTaskAction,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1.6f)
                                    .height(48.dp)
                                    .testTag("confirm_create_task_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Создать задачу",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

