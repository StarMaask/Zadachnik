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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

val STANDARD_VOLUME_UNITS = listOf("шт", "кг", "м²", "м³", "п.м.", "т", "л", "компл.", "ч")

@Composable
fun statusColor(status: TaskStatus): Color = when (status) {
    TaskStatus.NEW -> Color(0xFF2563EB)
    TaskStatus.IN_PROGRESS -> Color(0xFFD97706)
    TaskStatus.ON_REVIEW -> Color(0xFF7C3AED)
    TaskStatus.APPROVED -> Color(0xFF16A34A)
    TaskStatus.REJECTED -> Color(0xFFDC2626)
    TaskStatus.OVERDUE -> Color(0xFFB91C1C)
    TaskStatus.CANCELLED -> Color(0xFF64748B)
    TaskStatus.ARCHIVED -> Color(0xFF475569)
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
    val isApproved = task.taskStatus == TaskStatus.APPROVED
    val textDeco = if (isApproved) TextDecoration.LineThrough else TextDecoration.None
    val cardAlpha = if (isApproved) 0.65f else 1f
    val subtasks = remember(task.subtaskItems) { task.decodedSubtasks() }
    val doneSubtasks = subtasks.count { it.isDone }
    val targetVol = task.effectiveTargetVolume
    val completedVol = task.effectiveCompletedVolume
    val unitLabel = task.effectiveVolumeUnit.ifBlank { "шт" }
    val pct = task.completionPercent()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable(onClick = onClick)
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Тонкий цветовой индикатор статуса слева
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusColor(task.taskStatus))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall.copy(textDecoration = textDeco),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = statusColor(task.taskStatus).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.taskStatus.titleRu,
                            color = statusColor(task.taskStatus),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = textDeco),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Индикатор объёма выполнения (если задан плановый объём)
                if (targetVol > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Объём: ${SubtaskItem.formatVolumeNumber(completedVol)} из ${SubtaskItem.formatVolumeNumber(targetVol)} $unitLabel",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (pct >= 100) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (pct >= 100) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (pct >= 100) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Интерактивный чек-лист прямо на карточке с указанием объёма по каждому пункту
                if (subtasks.isNotEmpty() && onToggleSubtask != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        subtasks.take(4).forEach { sub ->
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
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${sub.title} — ${sub.formatVolumeStatus()}",
                                    style = MaterialTheme.typography.labelSmall.copy(
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
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Адаптивная строка метаданных (FlowRow не обрезается на узких экранах)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "👤 ${task.assigneeName.ifBlank { "Исполнитель" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "📅 ${ReportDocumentGenerator.formatEpochDay(task.dueDateEpochDay)} ${task.deadlineTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (subtasks.isNotEmpty()) {
                        Text(
                            text = "✓ $doneSubtasks/${subtasks.size} ($pct%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (pct >= 100) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                        )
                    }

                    if (task.requiresPhotoReport || task.photoItems.isNotEmpty()) {
                        val photoReady = task.hasEnoughPhotos()
                        Text(
                            text = "📷 ${task.photoItems.size}/${task.minPhotoCount}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (photoReady) Color(0xFF16A34A) else Color(0xFFD97706)
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskDetailDialog(
    task: TaskDocument,
    activeEmployee: CorporateEmployee,
    onDismiss: () -> Unit,
    onAddPhoto: (uriString: String, gpsCoords: String, comment: String) -> Unit,
    onDeletePhoto: (photoId: String) -> Unit = {},
    onToggleSubtask: (subtaskId: String) -> Unit,
    onUpdateSubtaskVolume: (subtaskId: String, completedVol: Double) -> Unit = { _, _ -> },
    onUpdateSubtaskDetails: (subtaskId: String, targetVol: Double, completedVol: Double, unit: String) -> Unit = { _, _, _, _ -> },
    onUpdateTaskVolume: (completedVol: Double) -> Unit = {},
    onAddSubtask: (title: String, targetVolume: Double, unit: String) -> Unit = { _, _, _ -> },
    onAddComment: (text: String, mentionedUser: String) -> Unit,
    onSubmitCompletion: (note: String) -> Unit,
    onApproveReport: (comment: String) -> Unit,
    onRejectReport: (reason: String) -> Unit,
    onArchiveTask: () -> Unit,
    onDeleteTask: () -> Unit = {}
) {
    val context = LocalContext.current
    var photoComment by remember { mutableStateOf("") }
    val photoGps = remember(task.geoLocation) { task.geoLocation ?: "Участок №1" }

    // Поля для добавления нового пункта чек-листа (всегда с указанием планового объёма и ед. изм.)
    var newSubtaskTitle by remember { mutableStateOf("") }
    var newSubtaskTargetVolText by remember { mutableStateOf("1") }
    var newSubtaskUnit by remember(task.effectiveVolumeUnit) {
        mutableStateOf(task.effectiveVolumeUnit.ifBlank { "шт" })
    }

    // Поле для ввода выполненного объёма по задаче в целом
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

    val seedAssignee = remember(task.assigneeId) {
        CorporateSeedData.employees.firstOrNull { it.id == task.assigneeId }
    }
    val assigneeRole = seedAssignee?.role ?: com.example.data.model.CorporateRole.WORKER
    val canApproveThisTask = activeEmployee.role.canApproveReportOf(assigneeRole)
    val isApproved = task.taskStatus == TaskStatus.APPROVED
    val textDeco = if (isApproved) TextDecoration.LineThrough else TextDecoration.None
    val completionPct = task.completionPercent()
    val isVolume100 = task.isVolumeCompleted100Percent()
    val canSubmitCompletion = task.hasEnoughPhotos() && (!task.hasVolumeTracking() || isVolume100)

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
                    // 1. ЗАКРЕПЛЁННАЯ ВЕРХНЯЯ ПАНЕЛЬ (всегда видна на любом экране телефона)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = statusColor(task.taskStatus).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${task.taskStatus.titleRu} • ${task.taskPriority.titleRu}",
                                color = statusColor(task.taskStatus),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("dialog_delete_task_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Удалить задачу",
                                    tint = MaterialTheme.colorScheme.error
                                )
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

                    // 2. ПРОКРУЧИВАЕМАЯ ЦЕНТРАЛЬНАЯ ОБЛАСТЬ
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(textDecoration = textDeco),
                            fontWeight = FontWeight.Bold
                        )
                        if (task.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodySmall.copy(textDecoration = textDeco),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Таблица реквизитов задачи
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Исполнитель:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(task.assigneeName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Срок:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${ReportDocumentGenerator.formatEpochDay(task.dueDateEpochDay)} до ${task.deadlineTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (targetVol > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Объём работ:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "${SubtaskItem.formatVolumeNumber(completedVol)} из ${SubtaskItem.formatVolumeNumber(targetVol)} $unitLabel ($completionPct%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (completionPct >= 100) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // РАЗДЕЛ 1: ЧЕК-ЛИСТ И ОТЧЁТНОСТЬ ПЕРСОНАЛА ПО ОБЪЁМУ ВЫПОЛНЕНИЯ (100% = ВЫПОЛНЕНО)
                        Spacer(modifier = Modifier.height(12.dp))
                        val subtasks = task.decodedSubtasks()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Чек-лист и объём выполнения",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (completionPct >= 100) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Выполнено: $completionPct%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (completionPct >= 100) Color(0xFF166534) else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Задача считается выполненной при достижении 100% объёма работ. Если работа выполнена не до конца, в отчёт попадёт фактический объём.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (completionPct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (completionPct >= 100) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Блок ввода фактического объёма по задаче (если задан плановый объём)
                        if (targetVol > 0.0 && task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Отчёт по объёму: план ${SubtaskItem.formatVolumeNumber(targetVol)} $unitLabel",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = taskCompletedVolInput,
                                            onValueChange = { taskCompletedVolInput = it },
                                            label = { Text("Факт ($unitLabel)") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("task_completed_volume_input")
                                        )
                                        Button(
                                            onClick = {
                                                val parsed = taskCompletedVolInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                                                onUpdateTaskVolume(parsed)
                                            },
                                            modifier = Modifier.testTag("save_task_volume_button")
                                        ) {
                                            Text("Сохранить")
                                        }
                                    }
                                    // Быстрые кнопки доли выполнения объёма
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(25, 50, 75, 100).forEach { pctStep ->
                                            val stepVal = (targetVol * pctStep) / 100.0
                                            FilterChip(
                                                selected = completionPct == pctStep,
                                                onClick = {
                                                    taskCompletedVolInput = SubtaskItem.formatVolumeNumber(stepVal)
                                                    onUpdateTaskVolume(stepVal)
                                                },
                                                label = {
                                                    Text("$pctStep% (${SubtaskItem.formatVolumeNumber(stepVal)} $unitLabel)")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Список пунктов чек-листа (с обязательным указанием объёма и вводом факта по каждому пункту)
                        if (subtasks.isEmpty()) {
                            Text(
                                text = "Пункты чек-листа не добавлены",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    subtasks.forEachIndexed { idx, sub ->
                                        val itemTarget = sub.effectiveTargetVolume
                                        val itemCompleted = sub.effectiveCompletedVolume
                                        val itemUnit = sub.effectiveUnit
                                        var subVolInput by remember(sub.id, itemCompleted) {
                                            mutableStateOf(
                                                if (itemCompleted > 0.0) SubtaskItem.formatVolumeNumber(itemCompleted) else ""
                                            )
                                        }
                                        var showEditPlanRow by remember(sub.id) { mutableStateOf(false) }
                                        var editTargetInput by remember(sub.id, itemTarget) {
                                            mutableStateOf(SubtaskItem.formatVolumeNumber(itemTarget))
                                        }
                                        var editUnitInput by remember(sub.id, itemUnit) {
                                            mutableStateOf(itemUnit)
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                                .testTag("subtask_row_${sub.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onToggleSubtask(sub.id) },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = sub.isDone,
                                                    onCheckedChange = { onToggleSubtask(sub.id) },
                                                    modifier = Modifier.testTag("subtask_checkbox_${sub.id}")
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${idx + 1}. ${sub.title}",
                                                        style = MaterialTheme.typography.bodySmall.copy(
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
                                                        text = "Объём по пункту: ${sub.formatVolumeStatus()}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (sub.progressPercent >= 100) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                                                    TextButton(
                                                        onClick = { showEditPlanRow = !showEditPlanRow },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = if (showEditPlanRow) "Скрыть" else "План/ед.",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { (sub.progressPercent / 100f).coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 36.dp)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = if (sub.progressPercent >= 100) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )

                                            // Поле ввода конкретного выполненного объёма прямо по пункту чек-листа
                                            if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 36.dp, top = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedTextField(
                                                        value = subVolInput,
                                                        onValueChange = { subVolInput = it },
                                                        label = { Text("Факт ($itemUnit)") },
                                                        placeholder = {
                                                            Text("из ${SubtaskItem.formatVolumeNumber(itemTarget)} $itemUnit")
                                                        },
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("subtask_volume_input_${sub.id}")
                                                    )
                                                    Button(
                                                        onClick = {
                                                            val v = subVolInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                                                            onUpdateSubtaskVolume(sub.id, v)
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                        modifier = Modifier.testTag("subtask_save_volume_${sub.id}")
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Факт", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }

                                                // Быстрые кнопки доли выполнения конкретного пункта чек-листа
                                                FlowRow(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 36.dp, top = 4.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    listOf(25, 50, 75, 100).forEach { pctStep ->
                                                        val stepVal = (itemTarget * pctStep) / 100.0
                                                        FilterChip(
                                                            selected = sub.progressPercent == pctStep,
                                                            onClick = {
                                                                subVolInput = SubtaskItem.formatVolumeNumber(stepVal)
                                                                onUpdateSubtaskVolume(sub.id, stepVal)
                                                            },
                                                            label = {
                                                                Text("$pctStep% (${SubtaskItem.formatVolumeNumber(stepVal)} $itemUnit)")
                                                            }
                                                        )
                                                    }
                                                }

                                                // Редактирование планового объёма и единицы измерения данного пункта чек-листа
                                                AnimatedVisibility(visible = showEditPlanRow) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(start = 36.dp, top = 6.dp)
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(8.dp),
                                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "Изменить плановый объём и ед. изм. пункта:",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                OutlinedTextField(
                                                                    value = editTargetInput,
                                                                    onValueChange = { editTargetInput = it },
                                                                    label = { Text("План") },
                                                                    singleLine = true,
                                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                                OutlinedTextField(
                                                                    value = editUnitInput,
                                                                    onValueChange = { editUnitInput = it },
                                                                    label = { Text("Ед. изм.") },
                                                                    singleLine = true,
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                                Button(
                                                                    onClick = {
                                                                        val newT = editTargetInput.replace(",", ".").toDoubleOrNull()
                                                                            ?.takeIf { it > 0.0 } ?: itemTarget
                                                                        val newC = subVolInput.replace(",", ".").toDoubleOrNull() ?: itemCompleted
                                                                        val newU = editUnitInput.trim().ifBlank { "шт" }
                                                                        onUpdateSubtaskDetails(sub.id, newT, newC, newU)
                                                                        showEditPlanRow = false
                                                                    },
                                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                                ) {
                                                                    Text("ОК", style = MaterialTheme.typography.labelSmall)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (idx < subtasks.lastIndex) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }
                                }
                            }
                        }

                        // Добавление нового пункта в чек-лист (всегда с указанием объёма и ед. изм.)
                        if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Добавить пункт в чек-лист (с плановым объёмом)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    OutlinedTextField(
                                        value = newSubtaskTitle,
                                        onValueChange = { newSubtaskTitle = it },
                                        label = { Text("Наименование пункта чек-листа *") },
                                        placeholder = { Text("Например: Монтаж кабельного лотка") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("add_subtask_input")
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newSubtaskTargetVolText,
                                            onValueChange = { newSubtaskTargetVolText = it },
                                            label = { Text("Плановый объём *") },
                                            placeholder = { Text("10") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("add_subtask_volume_input")
                                        )
                                        OutlinedTextField(
                                            value = newSubtaskUnit,
                                            onValueChange = { newSubtaskUnit = it },
                                            label = { Text("Ед. изм.") },
                                            placeholder = { Text("шт, кг, м²") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("add_subtask_unit_input")
                                        )
                                        Button(
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
                                            modifier = Modifier.testTag("add_subtask_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Добавить пункт", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Добавить")
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        STANDARD_VOLUME_UNITS.forEach { uChip ->
                                            FilterChip(
                                                selected = newSubtaskUnit == uChip,
                                                onClick = { newSubtaskUnit = uChip },
                                                label = { Text(uChip) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // РАЗДЕЛ 2: ФОТООТЧЁТ
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Фотоотчёт (${task.photoItems.size}/${task.minPhotoCount})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (!task.hasEnoughPhotos()) {
                                Text(
                                    text = "Нужно мин. ${task.minPhotoCount} фото",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val photos = task.decodedPhotos()
                        if (photos.isEmpty()) {
                            Text(
                                text = "Фотографии не прикреплены. Нажмите «Камера» или «+ Фотоштамп СМР».",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                photos.forEachIndexed { idx, ph ->
                                    val bitmap = remember(ph.uriString) {
                                        PhotoCaptureUtils.loadBitmapFromUriOrPath(ph.uriString)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedPhotoForPreview = ph }
                                            .testTag("photo_item_${ph.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.size(48.dp)
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

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Фото #${idx + 1} • ${ph.capturedAtText}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = ph.comment,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            IconButton(
                                                onClick = { selectedPhotoForPreview = ph },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Visibility,
                                                    contentDescription = "Просмотреть фото",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            if (task.taskStatus != TaskStatus.APPROVED) {
                                                IconButton(
                                                    onClick = { onDeletePhoto(ph.id) },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .testTag("delete_photo_${ph.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Удалить фото",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Непрерывная съёмка (серия фото)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Автоматически делать несколько фото подряд",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = continuousShootingMode,
                                    onCheckedChange = { continuousShootingMode = it },
                                    modifier = Modifier.testTag("continuous_camera_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = photoComment,
                                onValueChange = { photoComment = it },
                                placeholder = { Text("Подпись к фото (необязательно)...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("photo_comment_input"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
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
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("capture_camera_button")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (continuousShootingMode) "Камера (серия)" else "Камера", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
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
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("instant_photo_stamp_button")
                                ) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+ Фотоштамп СМР", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        // Подменю для комментариев и истории
                        Spacer(modifier = Modifier.height(6.dp))
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
                                }
                            )
                        }

                        AnimatedVisibility(visible = showExtraSections) {
                            Column {
                                val comments = task.decodedComments()
                                comments.forEach { cmt ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newCommentText,
                                        onValueChange = { newCommentText = it },
                                        placeholder = { Text("Написать комментарий...") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
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
                                    Spacer(modifier = Modifier.height(6.dp))
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

                    // 3. ЗАКРЕПЛЁННАЯ НИЖНЯЯ ПАНЕЛЬ ДЕЙСТВИЙ (всегда видна внизу экрана телефона!)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                            val buttonLabel = when {
                                !task.hasEnoughPhotos() -> "Добавьте ещё ${task.minPhotoCount - task.photoItems.size} фото"
                                task.hasVolumeTracking() && !isVolume100 -> "Для закрытия нужен объём 100% (сейчас $completionPct%)"
                                task.requiresApproval -> "Отправить на проверку (100%)"
                                else -> "Завершить задачу (100%)"
                            }
                            Button(
                                onClick = {
                                    onSubmitCompletion(photoComment)
                                    if (canSubmitCompletion) onDismiss()
                                },
                                enabled = canSubmitCompletion,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF16A34A),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("submit_task_completion_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = buttonLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (canApproveThisTask && (task.taskStatus == TaskStatus.ON_REVIEW || task.taskStatus == TaskStatus.IN_PROGRESS)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onApproveReport(supervisorNote)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF16A34A),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("approve_report_button")
                                ) {
                                    Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Принять отчёт", style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = { showRejectDialog = true },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("reject_report_button")
                                ) {
                                    Icon(Icons.Default.ThumbDown, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Отклонить", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
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
@OptIn(ExperimentalLayoutApi::class)
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

    // Выбор исполнителя (из списка или ввод ФИО вручную)
    var selectedAssignee by remember(assignees) { mutableStateOf(assignees.first()) }
    var useCustomAssigneeName by remember { mutableStateOf(false) }
    var customAssigneeName by remember { mutableStateOf("") }

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

        val fallbackTitle = pendingDraftSubtasks.firstOrNull()?.title
            ?: subtasksRaw.lines().map { it.trim() }.firstOrNull { it.isNotBlank() }
            ?: description.lines().map { it.trim() }.firstOrNull { it.isNotBlank() }
            ?: ""
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
            resolvedTargetVolFromTask > 0.0 -> resolvedTargetVolFromTask
            allSubtaskObjects.isNotEmpty() -> allSubtaskObjects.sumOf { it.effectiveTargetVolume }
            else -> 1.0
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

                        // СТРОКА ВЫБОРА ИСПОЛНИТЕЛЯ (всегда отображается!)
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

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    assignees.forEach { emp ->
                                        FilterChip(
                                            selected = !useCustomAssigneeName && selectedAssignee.id == emp.id,
                                            onClick = {
                                                useCustomAssigneeName = false
                                                selectedAssignee = emp
                                            },
                                            label = { Text("${emp.shortName} (${emp.role.titleRu})") },
                                            modifier = Modifier.testTag("select_assignee_${emp.id}")
                                        )
                                    }
                                    FilterChip(
                                        selected = useCustomAssigneeName,
                                        onClick = { useCustomAssigneeName = true },
                                        label = { Text("+ Указать ФИО исполнителя") },
                                        modifier = Modifier.testTag("custom_assignee_chip")
                                    )
                                }

                                AnimatedVisibility(visible = useCustomAssigneeName) {
                                    OutlinedTextField(
                                        value = customAssigneeName,
                                        onValueChange = { customAssigneeName = it },
                                        label = { Text("ФИО исполнителя (новый или из бригады)") },
                                        placeholder = { Text("Например: Петров А.С.") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("custom_assignee_name_input")
                                    )
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

                        // КОНСТРУКТОР ПУНКТОВ ЧЕК-ЛИСТА С ОБЪЁМОМ ПО КАЖДОМУ ПУНКТУ
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
                                    text = "Чек-лист с указанием объёма по каждому пункту",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Добавьте пункты работ и укажите плановый объём для каждого пункта (персонал отчитается по каждому пункту, а в отчёт попадёт конкретный выполненный объём).",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (draftChecklistItems.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        draftChecklistItems.forEachIndexed { idx, item ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "${idx + 1}. ${item.title}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = "Плановый объём: ${SubtaskItem.formatVolumeNumber(item.effectiveTargetVolume)} ${item.effectiveUnit}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { draftChecklistItems.removeAt(idx) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = "Удалить пункт",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = draftItemTitle,
                                    onValueChange = { draftItemTitle = it },
                                    label = { Text("Наименование пункта чек-листа") },
                                    placeholder = { Text("Например: Укладка кабеля ВВГнг") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("draft_subtask_title_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = draftItemVolText,
                                        onValueChange = { draftItemVolText = it },
                                        label = { Text("Объём *") },
                                        placeholder = { Text("50") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("draft_subtask_volume_input")
                                    )
                                    OutlinedTextField(
                                        value = draftItemUnit,
                                        onValueChange = { draftItemUnit = it },
                                        label = { Text("Ед. изм.") },
                                        placeholder = { Text("п.м., шт, кг") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("draft_subtask_unit_input")
                                    )
                                    Button(
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
                                        modifier = Modifier.testTag("add_draft_subtask_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("В чек-лист")
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

                                OutlinedTextField(
                                    value = subtasksRaw,
                                    onValueChange = { subtasksRaw = it },
                                    label = { Text("Быстрый ввод списком (Формат: Пункт : 50 кг)") },
                                    placeholder = { Text("Например:\nМонтаж лотка : 40 п.м.\nКрепёж кронштейнов : 80 шт") },
                                    maxLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("create_task_subtasks_input")
                                )
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
