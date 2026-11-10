package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.CorporateEmployee
import com.example.data.model.PhotoReportItem
import com.example.data.model.RecurrenceRule
import com.example.data.model.TaskDocument
import com.example.data.model.TaskDraft
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.seed.CorporateSeedData
import com.example.domain.reporting.ReportDocumentGenerator

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
 * Лаконичная табличная строка задачи (без визуального шума).
 */
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
    val cardAlpha = if (isApproved) 0.62f else 1f
    val subtasks = remember(task.subtaskItems) { task.decodedSubtasks() }
    val doneSubtasks = subtasks.count { it.isDone }

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
                    .padding(horizontal = 14.dp, vertical = 12.dp)
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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

                if (subtasks.isNotEmpty() && onToggleSubtask != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        subtasks.forEach { sub ->
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
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sub.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (sub.isDone) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Компактная информационная строка (табличный вид метаданных)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.assigneeName.ifBlank { "Исполнитель" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Text(
                        text = "${ReportDocumentGenerator.formatEpochDay(task.dueDateEpochDay)} ${task.deadlineTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (subtasks.isNotEmpty()) {
                        Text(
                            text = "✓ $doneSubtasks/${subtasks.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (doneSubtasks == subtasks.size) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
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
                        .padding(end = 4.dp)
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

@Composable
fun TaskDetailDialog(
    task: TaskDocument,
    activeEmployee: CorporateEmployee,
    onDismiss: () -> Unit,
    onAddPhoto: (uriString: String, gpsCoords: String, comment: String) -> Unit,
    onDeletePhoto: (photoId: String) -> Unit = {},
    onToggleSubtask: (subtaskId: String) -> Unit,
    onAddSubtask: (title: String) -> Unit = {},
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
    var newSubtaskTitle by remember { mutableStateOf("") }
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
            // Если включена непрерывная (серийная) съёмка — сразу открываем камеру для следующего кадра
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .imePadding(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Шапка карточки задачи с кнопками Удалить и Закрыть
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.testTag("dialog_delete_task_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Удалить задачу",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(textDecoration = textDeco),
                    fontWeight = FontWeight.Bold
                )
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium.copy(textDecoration = textDeco),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Компактная таблица параметров задачи
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Исполнитель:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(task.assigneeName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Срок:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${ReportDocumentGenerator.formatEpochDay(task.dueDateEpochDay)} до ${task.deadlineTime}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (!task.approvedByName.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Принял:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    task.approvedByName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // РАЗДЕЛ 1: ЧЕК-ЛИСТ ПОДЗАДАЧ (интерактивный)
                Spacer(modifier = Modifier.height(14.dp))
                val subtasks = task.decodedSubtasks()
                Text(
                    text = "Чек-лист (${subtasks.count { it.isDone }}/${subtasks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleSubtask(sub.id) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("subtask_row_${sub.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = sub.isDone,
                                        onCheckedChange = { onToggleSubtask(sub.id) },
                                        modifier = Modifier.testTag("subtask_checkbox_${sub.id}")
                                    )
                                    Text(
                                        text = sub.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (sub.isDone) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (idx < subtasks.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }

                // Быстрое добавление пункта в чек-лист
                if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSubtaskTitle,
                            onValueChange = { newSubtaskTitle = it },
                            placeholder = { Text("Новый пункт чек-листа...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_subtask_input")
                        )
                        IconButton(
                            onClick = {
                                if (newSubtaskTitle.isNotBlank()) {
                                    onAddSubtask(newSubtaskTitle)
                                    newSubtaskTitle = ""
                                }
                            },
                            modifier = Modifier.testTag("add_subtask_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Добавить пункт")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // РАЗДЕЛ 2: ФОТООТЧЁТ (просмотр, удаление, серийная камера, фотоштамп)
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

                Spacer(modifier = Modifier.height(8.dp))

                val photos = task.decodedPhotos()
                if (photos.isEmpty()) {
                    Text(
                        text = "Фотографии не прикреплены. Нажмите «Камера» или «+ Фотоштамп СМР» ниже.",
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
                                    // Миниатюра реального фото
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Миниатюра фото ${idx + 1}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth()
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
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = ph.comment,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Нажмите для просмотра",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = { selectedPhotoForPreview = ph },
                                        modifier = Modifier.size(36.dp)
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
                                                .size(36.dp)
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

                // Управление съёмкой (доступно до утверждения задачи)
                if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Переключатель непрерывной (серийной) съёмки
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Непрерывная съёмка (серия фото подряд)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Камера открывается снова сразу после каждого кадра",
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

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = photoComment,
                        onValueChange = { photoComment = it },
                        placeholder = { Text("Подпись к фото (необязательно)...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("photo_comment_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                            modifier = Modifier
                                .weight(1f)
                                .testTag("capture_camera_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Камера", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (continuousShootingMode) "Камера (серия)" else "Камера")
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
                            modifier = Modifier
                                .weight(1f)
                                .testTag("instant_photo_stamp_button")
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Фотоштамп СМР", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Фотоштамп СМР")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val canComplete = task.hasEnoughPhotos()
                    Button(
                        onClick = {
                            onSubmitCompletion(photoComment)
                            if (canComplete) onDismiss()
                        },
                        enabled = canComplete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("submit_task_completion_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (canComplete) {
                                if (task.requiresApproval) "Отправить на проверку" else "Завершить задачу"
                            } else {
                                "Добавьте ещё ${task.minPhotoCount - task.photoItems.size} фото для закрытия"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Решение проверяющего (для ИТР и Руководителя)
                if (canApproveThisTask && (task.taskStatus == TaskStatus.ON_REVIEW || task.taskStatus == TaskStatus.IN_PROGRESS)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = "Приёмка работы (${activeEmployee.role.titleRu})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = supervisorNote,
                        onValueChange = { supervisorNote = it },
                        placeholder = { Text("Комментарий проверяющего (необязательно)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                                .testTag("approve_report_button")
                        ) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Принять")
                        }
                        OutlinedButton(
                            onClick = { showRejectDialog = true },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reject_report_button")
                        ) {
                            Icon(Icons.Default.ThumbDown, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Отклонить", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Подменю для комментариев, истории и архива (чтобы не перегружать карточку)
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = { showExtraSections = !showExtraSections },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (showExtraSections) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (showExtraSections) {
                            "Скрыть комментарии и историю"
                        } else {
                            "Комментарии (${task.commentItems.size}) и история (${task.historyItems.size})"
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
                                .padding(top = 6.dp),
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Журнал событий:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            history.forEach { h ->
                                Text(
                                    text = "• ${h.timestampText} (${h.actorName}): ${h.note}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (task.taskStatus != TaskStatus.ARCHIVED) {
                                TextButton(
                                    onClick = {
                                        onArchiveTask()
                                        onDismiss()
                                    }
                                ) {
                                    Text("В архив")
                                }
                            }
                            TextButton(
                                onClick = { showDeleteConfirm = true }
                            ) {
                                Text("Удалить задачу", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Полноэкранный просмотр фотографии с функцией удаления
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
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Просмотр фотоотчёта",
                            style = MaterialTheme.typography.titleMedium,
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

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 440.dp)
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
                                    modifier = Modifier.size(48.dp)
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = photo.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Геометка: ${photo.gpsCoords}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateTaskDialog(
    activeEmployee: CorporateEmployee,
    availableEmployees: List<CorporateEmployee> = listOf(activeEmployee),
    currentEpochDay: Long,
    onDismiss: () -> Unit,
    onCreate: (TaskDraft) -> Unit
) {
    val assignees = remember(availableEmployees, activeEmployee) {
        availableEmployees.ifEmpty { listOf(activeEmployee) }
    }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedAssignee by remember(assignees) { mutableStateOf(assignees.first()) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var isTomorrow by remember { mutableStateOf(false) }
    var deadlineTime by remember { mutableStateOf("18:00") }
    var requiresPhoto by remember { mutableStateOf(true) }
    var minPhotoCount by remember { mutableIntStateOf(1) }
    var requiresApproval by remember { mutableStateOf(true) }
    var selectedRecurrence by remember { mutableStateOf(RecurrenceRule.NONE) }
    var subtasksRaw by remember { mutableStateOf("") }
    var tagsRaw by remember { mutableStateOf("") }
    var geoLocation by remember { mutableStateOf(activeEmployee.siteName) }
    var showAdvancedOptions by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 12.dp)
                .imePadding(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Заголовок диалога
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Новая задача",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                // Прокручиваемая форма
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        label = { Text("Название задачи *") },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text("Введите название задачи") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_task_title_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Описание / требования (необязательно)") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_task_desc_input")
                    )

                    // Выбор исполнителя (только реальные пользователи)
                    if (assignees.size > 1) {
                        Text(
                            text = "Исполнитель:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            assignees.forEach { emp ->
                                FilterChip(
                                    selected = selectedAssignee.id == emp.id,
                                    onClick = { selectedAssignee = emp },
                                    label = { Text("${emp.shortName} (${emp.role.titleRu})") }
                                )
                            }
                        }
                    }

                    // Срок и приоритет в компактной строке
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isTomorrow,
                            onClick = { isTomorrow = false },
                            label = { Text("На сегодня") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isTomorrow,
                            onClick = { isTomorrow = true },
                            label = { Text("На завтра") },
                            modifier = Modifier.weight(1f)
                        )
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

                    // Фотоотчёт
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Обязательный фотоотчёт",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (requiresPhoto) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(1, 2, 3).forEach { count ->
                                        FilterChip(
                                            selected = minPhotoCount == count,
                                            onClick = { minPhotoCount = count },
                                            label = { Text("$count фото") }
                                        )
                                    }
                                }
                            }
                        }
                        Switch(
                            checked = requiresPhoto,
                            onCheckedChange = { requiresPhoto = it }
                        )
                    }

                    // Чек-лист подзадач
                    OutlinedTextField(
                        value = subtasksRaw,
                        onValueChange = { subtasksRaw = it },
                        label = { Text("Пункты чек-листа (каждый с новой строки)") },
                        placeholder = { Text("Например:\nПроверить комплектацию\nВыполнить фотофиксацию") },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_task_subtasks_input")
                    )

                    // Дополнительные настройки в подменю
                    TextButton(
                        onClick = { showAdvancedOptions = !showAdvancedOptions },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (showAdvancedOptions) "Скрыть доп. параметры"
                            else "Дополнительные параметры (повтор, время, участок)"
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

                Spacer(modifier = Modifier.height(12.dp))

                // Закреплённая панель кнопок внизу (всегда видна над клавиатурой)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }
                            val dueDay = if (isTomorrow) currentEpochDay + 1L else currentEpochDay
                            val subtasks = subtasksRaw.lines()
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            val tags = tagsRaw.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }

                            onCreate(
                                TaskDraft(
                                    title = title.trim(),
                                    description = description.trim(),
                                    assigneeId = selectedAssignee.id,
                                    priority = selectedPriority,
                                    dueDateEpochDay = dueDay,
                                    deadlineTime = deadlineTime.ifBlank { "18:00" },
                                    requiresPhotoReport = requiresPhoto,
                                    minPhotoCount = if (requiresPhoto) minPhotoCount else 0,
                                    requiresApproval = requiresApproval,
                                    recurrenceRule = selectedRecurrence,
                                    tags = tags,
                                    subtasks = subtasks,
                                    geoLocation = geoLocation.trim()
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("confirm_create_task_button")
                    ) {
                        Text("Поставить задачу", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
