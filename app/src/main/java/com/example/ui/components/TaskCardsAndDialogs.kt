package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.graphics.Color
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
import com.example.data.model.RecurrenceRule
import com.example.data.model.TaskDocument
import com.example.data.model.TaskDraft
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.seed.CorporateSeedData
import com.example.domain.reporting.ReportDocumentGenerator

@Composable
fun statusColor(status: TaskStatus): Color = when (status) {
    TaskStatus.NEW -> Color(0xFF0284C7)
    TaskStatus.IN_PROGRESS -> Color(0xFFD97706)
    TaskStatus.ON_REVIEW -> Color(0xFF7C3AED)
    TaskStatus.APPROVED -> Color(0xFF059669)
    TaskStatus.REJECTED -> Color(0xFFDC2626)
    TaskStatus.OVERDUE -> Color(0xFFB91C1C)
    TaskStatus.CANCELLED -> Color(0xFF64748B)
    TaskStatus.ARCHIVED -> Color(0xFF475569)
}

@Composable
fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.LOW -> Color(0xFF64748B)
    TaskPriority.MEDIUM -> Color(0xFF0284C7)
    TaskPriority.HIGH -> Color(0xFFD97706)
    TaskPriority.CRITICAL -> Color(0xFFDC2626)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskItemCard(
    task: TaskDocument,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isApproved = task.taskStatus == TaskStatus.APPROVED
    val textDeco = if (isApproved) TextDecoration.LineThrough else TextDecoration.None
    val cardAlpha = if (isApproved) 0.58f else 1f
    val subtasks = remember(task.subtaskItems) { task.decodedSubtasks() }
    val doneSubtasks = subtasks.count { it.isDone }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable(onClick = onClick)
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isApproved) 1.dp else 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = statusColor(task.taskStatus).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = task.taskStatus.titleRu,
                            color = statusColor(task.taskStatus),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = priorityColor(task.taskPriority).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = task.taskPriority.titleRu,
                            color = priorityColor(task.taskPriority),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "${ReportDocumentGenerator.formatEpochDay(task.dueDateEpochDay)} • ${task.deadlineTime}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium.copy(textDecoration = textDeco),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = textDeco),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!task.rejectionReason.isNullOrBlank() && task.taskStatus == TaskStatus.REJECTED) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Причина отклонения",
                            tint = Color(0xFFB91C1C),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Причина возврата: ${task.rejectionReason}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF7F1D1D),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Исполнитель",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.assigneeName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (task.requiresPhotoReport) {
                    val photoReady = task.hasEnoughPhotos()
                    Surface(
                        color = if (photoReady) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Фотоотчёт",
                                modifier = Modifier.size(14.dp),
                                tint = if (photoReady) Color(0xFF065F46) else Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Фото: ${task.photoItems.size}/${task.minPhotoCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (photoReady) Color(0xFF065F46) else Color(0xFF92400E),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (task.carryoverCount > 0L) {
                    Surface(
                        color = Color(0xFFFFEDD5),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "↻ Перенос ×${task.carryoverCount} (исх. ${ReportDocumentGenerator.formatEpochDay(task.originalDueDateEpochDay)})",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF9A3412),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (subtasks.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Чек-лист: $doneSubtasks/${subtasks.size}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (task.recurrence != RecurrenceRule.NONE) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Повторяемость",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.recurrence.titleRu,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
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
    onToggleSubtask: (subtaskId: String) -> Unit,
    onAddComment: (text: String, mentionedUser: String) -> Unit,
    onSubmitCompletion: (note: String) -> Unit,
    onApproveReport: (comment: String) -> Unit,
    onRejectReport: (reason: String) -> Unit,
    onArchiveTask: () -> Unit
) {
    val context = LocalContext.current
    var photoComment by remember { mutableStateOf("") }
    var photoGps by remember { mutableStateOf(task.geoLocation ?: "55.7558° N, 37.6173° E") }
    var newCommentText by remember { mutableStateOf("") }
    var mentionTarget by remember { mutableStateOf("") }
    var supervisorNote by remember { mutableStateOf("") }
    var rejectionReasonInput by remember { mutableStateOf("") }
    var showRejectDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        val stampLabel = if (bitmap != null) {
            "camera://Снимок камеры (${bitmap.width}×${bitmap.height}) • ${task.title.take(28)}"
        } else {
            "stamp://Фотофиксация с камеры • ${task.title.take(30)}"
        }
        onAddPhoto(
            stampLabel,
            photoGps,
            photoComment.ifBlank { "Снимок с камеры приложения на объекте" }
        )
        photoComment = ""
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            onAddPhoto(
                "stamp://Фотофиксация на участке • ${task.title.take(32)}",
                photoGps,
                photoComment.ifBlank { "Фотофиксация выполнения работ" }
            )
            photoComment = ""
        }
    }

    val assignee = remember(task.assigneeId) { CorporateSeedData.findEmployee(task.assigneeId) }
    val canApproveThisTask = activeEmployee.role.canApproveReportOf(assignee.role)
    val isApproved = task.taskStatus == TaskStatus.APPROVED
    val textDeco = if (isApproved) TextDecoration.LineThrough else TextDecoration.None

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = statusColor(task.taskStatus).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${task.taskStatus.titleRu} • ${task.taskPriority.titleRu}",
                            color = statusColor(task.taskStatus),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть карточку задачи")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge.copy(textDecoration = textDeco),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = textDeco),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Поставил: ${task.creatorName}", style = MaterialTheme.typography.labelMedium)
                        Text("Исполнитель: ${task.assigneeName} (${task.groupName})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Срок: ${ReportDocumentGenerator.formatEpochDay(task.dueDateEpochDay)} до ${task.deadlineTime} • Переносов: ${task.carryoverCount}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (!task.geoLocation.isNullOrBlank()) {
                            Text("Геометка / Участок: ${task.geoLocation}", style = MaterialTheme.typography.labelMedium)
                        }
                        if (!task.approvedByName.isNullOrBlank()) {
                            Text(
                                "Подтвердил: ${task.approvedByName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Subtasks Checklist
                val subtasks = task.decodedSubtasks()
                if (subtasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Чек-лист подзадач (${subtasks.count { it.isDone }}/${subtasks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    subtasks.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubtask(sub.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sub.isDone,
                                onCheckedChange = { onToggleSubtask(sub.id) }
                            )
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Photo Report Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Фотоотчёт (${task.photoItems.size} из мин. ${task.minPhotoCount} фото)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!task.hasEnoughPhotos()) {
                        Text(
                            text = "Обязательно!",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val photos = task.decodedPhotos()
                if (photos.isEmpty()) {
                    Text(
                        text = "Фотографии ещё не приложены. Сделайте снимок через камеру приложения.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        photos.forEachIndexed { idx, ph ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Фото ${idx + 1}",
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Фото #${idx + 1} • ${ph.authorName} (${ph.capturedAtText})",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = ph.comment,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "GPS: ${ph.gpsCoords}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Add Photo Controls (allowed until APPROVED or ARCHIVED)
                if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = photoComment,
                        onValueChange = { photoComment = it },
                        label = { Text("Комментарий к снимку и привязка к осям") },
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
                                    cameraLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("capture_camera_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Камера")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Снять камерой")
                        }
                        OutlinedButton(
                            onClick = {
                                onAddPhoto(
                                    "stamp://Фотоштамп объекта #${task.photoItems.size + 1} (${task.title.take(24)})",
                                    photoGps,
                                    photoComment.ifBlank { "Контрольный снимок узла на объекте" }
                                )
                                photoComment = ""
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("instant_photo_stamp_button")
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Быстрый фотоштамп")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Фотоштамп СМР")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val canComplete = task.hasEnoughPhotos()
                    Button(
                        onClick = {
                            onSubmitCompletion(photoComment)
                            if (canComplete) onDismiss()
                        },
                        enabled = canComplete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_task_completion_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Отметить выполнение")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (canComplete) {
                                if (task.requiresApproval) "Отметить выполненной → На проверку" else "Завершить задачу"
                            } else {
                                "Добавьте ещё ${task.minPhotoCount - task.photoItems.size} фото для закрытия"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Supervisor Approval / Rejection Bar (for ITR & Head)
                if (canApproveThisTask && (task.taskStatus == TaskStatus.ON_REVIEW || task.taskStatus == TaskStatus.IN_PROGRESS)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = "Решение проверяющего (${activeEmployee.role.titleRu})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = supervisorNote,
                        onValueChange = { supervisorNote = it },
                        label = { Text("Резолюция / комментарий ИТР при приёмке") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onApproveReport(supervisorNote)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("approve_report_button")
                        ) {
                            Icon(Icons.Default.ThumbUp, contentDescription = "Подтвердить")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Подтвердить")
                        }
                        Button(
                            onClick = { showRejectDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reject_report_button")
                        ) {
                            Icon(Icons.Default.ThumbDown, contentDescription = "Отклонить")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Отклонить")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Comments & @Mentions
                val comments = task.decodedComments()
                Text(
                    text = "Комментарии и @упоминания (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                comments.forEach { cmt ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${cmt.authorName} (${cmt.authorRole}) • ${cmt.createdAtText}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = cmt.text, style = MaterialTheme.typography.bodyMedium)
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
                        placeholder = { Text("Комментарий или @ФИО...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                onAddComment(newCommentText, mentionTarget)
                                newCommentText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Отправить комментарий")
                    }
                }

                // History Log
                val history = task.decodedHistory()
                if (history.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = "История изменений и переносов",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    history.forEach { h ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${h.timestampText} • ${h.actorName}: ${h.note}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (task.taskStatus != TaskStatus.ARCHIVED) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            onArchiveTask()
                            onDismiss()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = "Архив")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Переместить задачу в архив")
                    }
                }
            }
        }
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Отклонить отчёт с указанием причины") },
            text = {
                Column {
                    Text(
                        "Задача вернётся исполнителю в статус «Отклонена», причина будет видна в карточке задачи и в журнале аудита.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        label = { Text("Обязательная причина отклонения *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rejection_reason_input")
                    )
                }
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Отклонить отчёт", color = Color.White)
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
fun CreateTaskDialog(
    activeEmployee: CorporateEmployee,
    currentEpochDay: Long,
    onDismiss: () -> Unit,
    onCreate: (TaskDraft) -> Unit
) {
    val allowedAssignees = remember(activeEmployee) {
        CorporateSeedData.getAssigneesForActor(activeEmployee)
    }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedAssignee by remember { mutableStateOf(allowedAssignees.first()) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.HIGH) }
    var isTomorrow by remember { mutableStateOf(false) }
    var deadlineTime by remember { mutableStateOf("18:00") }
    var requiresPhoto by remember { mutableStateOf(true) }
    var minPhotoCount by remember { mutableIntStateOf(2) }
    var requiresApproval by remember { mutableStateOf(true) }
    var selectedRecurrence by remember { mutableStateOf(RecurrenceRule.NONE) }
    var subtasksRaw by remember { mutableStateOf("Проверить геодезическую отметку\nВыполнить фотофиксацию узла") }
    var tagsRaw by remember { mutableStateOf("СМР, Контроль") }
    var geoLocation by remember { mutableStateOf(activeEmployee.siteName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Постановка задачи (${activeEmployee.role.titleRu})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Text(
                    text = "Быстрые шаблоны СМР:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CorporateSeedData.quickTaskTemplates.forEach { tpl ->
                        AssistChip(
                            onClick = {
                                title = tpl.title
                                description = tpl.description
                                selectedPriority = tpl.priority
                                requiresPhoto = tpl.requiresPhotoReport
                                minPhotoCount = tpl.minPhotoCount
                                selectedRecurrence = tpl.recurrenceRule
                                subtasksRaw = tpl.defaultSubtasks.joinToString("\n")
                                tagsRaw = tpl.tags.joinToString(", ")
                            },
                            label = { Text(tpl.title.take(26) + "…") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Заголовок задачи / наряда *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Техническое описание и требования") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Исполнитель (с учётом иерархии прав):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allowedAssignees.forEach { emp ->
                        FilterChip(
                            selected = selectedAssignee.id == emp.id,
                            onClick = { selectedAssignee = emp },
                            label = { Text("${emp.shortName} (${emp.role.titleRu})") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text("Дата и приоритет:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isTomorrow,
                        onClick = { isTomorrow = false },
                        label = { Text("На сегодня (${ReportDocumentGenerator.formatEpochDay(currentEpochDay)})") }
                    )
                    FilterChip(
                        selected = isTomorrow,
                        onClick = { isTomorrow = true },
                        label = { Text("В план на завтра (${ReportDocumentGenerator.formatEpochDay(currentEpochDay + 1L)})") }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.entries.forEach { prio ->
                        FilterChip(
                            selected = selectedPriority == prio,
                            onClick = { selectedPriority = prio },
                            label = { Text(prio.titleRu) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Обязательный фотоотчёт", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Без минимального числа фото закрыть задачу нельзя",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = requiresPhoto, onCheckedChange = { requiresPhoto = it })
                }

                if (requiresPhoto) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Мин. количество фото:", style = MaterialTheme.typography.bodyMedium)
                        listOf(1, 2, 3, 5).forEach { count ->
                            FilterChip(
                                selected = minPhotoCount == count,
                                onClick = { minPhotoCount = count },
                                label = { Text("$count фото") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text("Повторяемость:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecurrenceRule.entries.forEach { rule ->
                        FilterChip(
                            selected = selectedRecurrence == rule,
                            onClick = { selectedRecurrence = rule },
                            label = { Text(rule.titleRu) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subtasksRaw,
                    onValueChange = { subtasksRaw = it },
                    label = { Text("Подзадачи чек-листа (каждая с новой строки)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onCreate(
                                TaskDraft(
                                    title = title.trim(),
                                    description = description.trim(),
                                    assigneeId = selectedAssignee.id,
                                    priority = selectedPriority,
                                    dueDateEpochDay = if (isTomorrow) currentEpochDay + 1L else currentEpochDay,
                                    deadlineTime = deadlineTime,
                                    requiresPhotoReport = requiresPhoto,
                                    minPhotoCount = if (requiresPhoto) minPhotoCount else 0,
                                    requiresApproval = requiresApproval,
                                    recurrenceRule = selectedRecurrence,
                                    tags = tagsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                    subtasks = subtasksRaw.lines().map { it.trim() }.filter { it.isNotBlank() },
                                    geoLocation = geoLocation
                                )
                            )
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_create_task_button")
                ) {
                    Text("Поставить задачу", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
