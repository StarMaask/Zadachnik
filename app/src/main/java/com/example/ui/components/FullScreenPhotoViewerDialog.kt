package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PhotoReportItem
import com.example.data.model.TaskDocument
import com.example.domain.reporting.ReportDocumentGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FullScreenPhotoDialog(
    photo: PhotoReportItem,
    allPhotos: List<PhotoReportItem> = listOf(photo),
    task: TaskDocument? = null,
    taskTitle: String = task?.title.orEmpty(),
    preparedByName: String = "",
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    cloudPhotoCacheVersion: Int = 0,
    isHydratingCloudPhotos: Boolean = false,
    isUploadingPhotosToCloud: Boolean = false,
    hasCloudPhotoSyncError: Boolean = false,
    onRetryCloudSync: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val effectivePhotos = remember(allPhotos, photo) {
        if (allPhotos.isNotEmpty()) allPhotos else listOf(photo)
    }
    val initialIdx = remember(effectivePhotos, photo.id) {
        effectivePhotos.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
    }
    var currentIndex by remember(photo.id, effectivePhotos.size) {
        mutableIntStateOf(initialIdx.coerceIn(0, (effectivePhotos.size - 1).coerceAtLeast(0)))
    }
    val activePhoto = effectivePhotos.getOrElse(currentIndex) { photo }
    val photoNumber = currentIndex + 1

    var scale by remember(activePhoto.id) { mutableFloatStateOf(1f) }
    var panOffset by remember(activePhoto.id) { mutableStateOf(Offset.Zero) }
    var showChrome by remember { mutableStateOf(true) }

    var isDownloadingPhoto by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var exportedPdfFile by remember { mutableStateOf<File?>(null) }
    var pendingSafPhoto by remember { mutableStateOf<PhotoReportItem?>(null) }

    val safCreatePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { targetUri: Uri? ->
        val safItem = pendingSafPhoto
        pendingSafPhoto = null
        if (targetUri != null && safItem != null) {
            scope.launch {
                isDownloadingPhoto = true
                val ok = withContext(Dispatchers.IO) {
                    val src = PhotoCaptureUtils.prepareOriginalPhotoExportFile(
                        context = context,
                        photo = safItem,
                        taskTitle = taskTitle,
                        photoIndex = photoNumber
                    ) ?: return@withContext false
                    runCatching {
                        context.contentResolver.openOutputStream(targetUri)?.use { out ->
                            src.inputStream().use { input -> input.copyTo(out) }
                        } != null
                    }.getOrDefault(false)
                }
                isDownloadingPhoto = false
                statusMessage = if (ok) "Фото сохранено на устройство" else "Не удалось сохранить фото"
            }
        }
    }

    var originalResult by remember(
        activePhoto.id,
        activePhoto.uriString,
        cloudPhotoCacheVersion
    ) {
        mutableStateOf(
            PhotoCaptureUtils.OriginalPhotoLoadResult(
                bitmap = null,
                sourceLabel = "Загрузка...",
                width = 0,
                height = 0
            )
        )
    }
    var isDecodingOriginal by remember(activePhoto.id, activePhoto.uriString, cloudPhotoCacheVersion) {
        mutableStateOf(true)
    }

    val sanitizedActiveGps = remember(activePhoto.gpsCoords, task?.geoLocation, task?.groupName) {
        com.example.data.seed.CorporateSeedData.sanitizeSiteOrLocation(
            raw = activePhoto.gpsCoords,
            fallbackSite = task?.geoLocation.orEmpty(),
            fallbackSubdivision = task?.groupName.orEmpty()
        )
    }

    LaunchedEffect(activePhoto.id, activePhoto.uriString, taskTitle, sanitizedActiveGps, cloudPhotoCacheVersion) {
        isDecodingOriginal = true
        val decoded = withContext(Dispatchers.IO) {
            PhotoCaptureUtils.loadOriginalBitmapFromUriOrPath(
                uriOrPath = activePhoto.uriString,
                fallbackComment = activePhoto.comment,
                fallbackAuthor = activePhoto.authorName,
                fallbackTime = activePhoto.capturedAtText,
                fallbackGps = sanitizedActiveGps,
                fallbackTaskTitle = taskTitle,
                context = context,
                photoId = activePhoto.id
            )
        }
        originalResult = decoded
        isDecodingOriginal = false
    }

    val effectiveTaskForPdf = remember(task, taskTitle, activePhoto, sanitizedActiveGps) {
        task ?: TaskDocument(
            id = "photo_report_${activePhoto.id}",
            title = taskTitle.ifBlank { activePhoto.comment.ifBlank { "Фотоотчёт" } },
            assigneeName = activePhoto.authorName,
            geoLocation = sanitizedActiveGps
        )
    }

    val triggerDownloadCurrentPhoto: () -> Unit = {
        if (!isDownloadingPhoto) {
            scope.launch {
                isDownloadingPhoto = true
                statusMessage = null
                val res = withContext(Dispatchers.IO) {
                    PhotoCaptureUtils.savePhotoToDeviceDownloads(
                        context = context,
                        photo = activePhoto,
                        taskTitle = taskTitle,
                        photoIndex = photoNumber
                    )
                }
                isDownloadingPhoto = false
                res.onSuccess { savedPath ->
                    statusMessage = "Скачано: $savedPath"
                    Toast.makeText(context, "Фото сохранено в «Загрузки»", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    val needsSaf = err.message?.contains("Permission", ignoreCase = true) == true ||
                        err is SecurityException
                    if (needsSaf) {
                        pendingSafPhoto = activePhoto
                        val fileName = PhotoCaptureUtils.buildSafePhotoFileName(
                            photo = activePhoto,
                            taskTitle = taskTitle,
                            photoIndex = photoNumber
                        )
                        safCreatePhotoLauncher.launch(fileName)
                    } else {
                        statusMessage = err.message ?: "Не удалось скачать фото"
                    }
                }
            }
        }
    }

    val triggerExportAllPhotosPdf: () -> Unit = {
        if (!isExportingPdf && effectivePhotos.isNotEmpty()) {
            scope.launch {
                isExportingPdf = true
                statusMessage = null
                val res = withContext(Dispatchers.IO) {
                    ReportDocumentGenerator.exportTaskPhotosToPdf(
                        context = context,
                        task = effectiveTaskForPdf,
                        photos = effectivePhotos,
                        preparedByName = preparedByName,
                        saveToDownloads = true
                    )
                }
                isExportingPdf = false
                res.onSuccess { pdfResult ->
                    exportedPdfFile = pdfResult.file
                    val where = pdfResult.downloadedToPath ?: pdfResult.file.name
                    statusMessage = "PDF (${pdfResult.photoCount} фото, ${pdfResult.pageCount} стр.): $where"
                }.onFailure { err ->
                    statusMessage = err.message ?: "Ошибка выгрузки PDF"
                }
            }
        }
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
                .background(Color(0xFF060A12))
        ) {
            // Основная полноэкранная область просмотра исходного фото с зумом и панорамированием
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(activePhoto.id) {
                        detectTapGestures(
                            onTap = { showChrome = !showChrome },
                            onDoubleTap = {
                                if (scale > 1.05f) {
                                    scale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
                    .pointerInput(activePhoto.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            panOffset = if (newScale > 1.02f) {
                                panOffset + pan
                            } else {
                                Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val bmp = originalResult.bitmap
                when {
                    bmp != null -> {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Фотоотчёт в исходном формате",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = panOffset.x,
                                    translationY = panOffset.y
                                )
                                .testTag("fullscreen_original_photo_image")
                        )
                    }

                    isDecodingOriginal || isHydratingCloudPhotos || isUploadingPhotosToCloud -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF60A5FA),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = when {
                                    isUploadingPhotosToCloud -> "Отправка фото в облако..."
                                    isHydratingCloudPhotos -> "Загрузка оригинала из облака..."
                                    else -> "Подготовка исходного изображения..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = activePhoto.comment.ifBlank { "Фото #$photoNumber" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (hasCloudPhotoSyncError) {
                                    "Ошибка загрузки из облака. Нажмите ниже для повтора."
                                } else {
                                    "Исходный файл ещё синхронизируется с устройства автора."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(
                                onClick = onRetryCloudSync,
                                border = BorderStroke(1.dp, Color(0xFF60A5FA)),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("fullscreen_retry_photo_sync_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Загрузить из облака", color = Color(0xFF60A5FA))
                            }
                        }
                    }
                }
            }

            // Стрелки перелистывания при наличии нескольких фото в отчёте
            if (showChrome && effectivePhotos.size > 1) {
                if (currentIndex > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.56f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 10.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { currentIndex -= 1 }
                            .testTag("fullscreen_prev_photo_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Предыдущее фото",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                if (currentIndex < effectivePhotos.lastIndex) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.56f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { currentIndex += 1 }
                            .testTag("fullscreen_next_photo_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Следующее фото",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Верхняя панель с заголовком, метаданными исходного формата и действиями
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xE6060A12),
                                    Color(0x99060A12),
                                    Color.Transparent
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val counterSuffix = if (effectivePhotos.size > 1) {
                                " ($photoNumber из ${effectivePhotos.size})"
                            } else {
                                ""
                            }
                            Text(
                                text = "Просмотр фотоотчёта$counterSuffix",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val dimText = if (originalResult.width > 0 && originalResult.height > 0) {
                                " • ${originalResult.width}×${originalResult.height} px · ${originalResult.sourceLabel}"
                            } else {
                                ""
                            }
                            Text(
                                text = "${activePhoto.authorName} • ${activePhoto.capturedAtText}$dimText",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (scale > 1.05f) {
                                        scale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("fullscreen_zoom_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (scale > 1.05f) Icons.Default.ZoomOutMap else Icons.Default.ZoomIn,
                                    contentDescription = if (scale > 1.05f) "Сбросить масштаб" else "Увеличить фото",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = {
                                    PhotoCaptureUtils.openOriginalPhotoInExternalViewer(
                                        context = context,
                                        photo = activePhoto,
                                        taskTitle = taskTitle,
                                        photoIndex = photoNumber
                                    ).onFailure { err ->
                                        statusMessage = err.message ?: "Нет приложения для просмотра фото"
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("fullscreen_open_original_viewer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Открыть исходный файл в галерее",
                                    tint = Color(0xFF93C5FD)
                                )
                            }

                            if (canDelete) {
                                IconButton(
                                    onClick = { onDelete(activePhoto.id) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("fullscreen_delete_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Удалить это фото",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("fullscreen_close_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Закрыть просмотр",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Нижняя панель: лента миниатюр, описание, кнопки «Скачать фото» и «Все фото в PDF»
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xCC060A12),
                                    Color(0xF2060A12)
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (effectivePhotos.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            effectivePhotos.forEachIndexed { idx, thumbItem ->
                                val isSelected = idx == currentIndex
                                val thumbSanitizedGps = com.example.data.seed.CorporateSeedData.sanitizeSiteOrLocation(
                                    raw = thumbItem.gpsCoords,
                                    fallbackSite = task?.geoLocation.orEmpty(),
                                    fallbackSubdivision = task?.groupName.orEmpty()
                                )
                                val thumbBmp = remember(thumbItem.id, thumbItem.uriString, thumbSanitizedGps, cloudPhotoCacheVersion) {
                                    PhotoCaptureUtils.loadBitmapFromUriOrPath(
                                        uriOrPath = thumbItem.uriString,
                                        fallbackComment = thumbItem.comment,
                                        fallbackAuthor = thumbItem.authorName,
                                        fallbackTime = thumbItem.capturedAtText,
                                        fallbackGps = thumbSanitizedGps,
                                        fallbackTaskTitle = taskTitle,
                                        context = context,
                                        photoId = thumbItem.id
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .background(Color(0xFF1E293B))
                                        .clickable { currentIndex = idx }
                                        .testTag("fullscreen_thumb_${thumbItem.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (thumbBmp != null) {
                                        Image(
                                            bitmap = thumbBmp.asImageBitmap(),
                                            contentDescription = "Фото ${idx + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = activePhoto.comment.ifBlank { "Фотофиксация #$photoNumber" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Участок / Подразделение: ${sanitizedActiveGps.ifBlank { "Не указано" }} • Двойной тап или сведение пальцев для масштаба",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (statusMessage != null || exportedPdfFile != null) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusMessage.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF86EFAC),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val pdfFile = exportedPdfFile
                                if (pdfFile != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                ReportDocumentGenerator.openPdfFile(
                                                    context = context,
                                                    pdfFile = pdfFile,
                                                    title = "Фотоотчёт ${taskTitle.take(28)}"
                                                )
                                            },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .testTag("fullscreen_open_exported_pdf_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "Открыть PDF",
                                                tint = Color(0xFF60A5FA),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                ReportDocumentGenerator.sharePdfFile(
                                                    context = context,
                                                    pdfFile = pdfFile,
                                                    title = "Фотоотчёт ${taskTitle.take(28)}"
                                                )
                                            },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .testTag("fullscreen_share_exported_pdf_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Поделиться PDF",
                                                tint = Color(0xFF60A5FA),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = triggerDownloadCurrentPhoto,
                            enabled = !isDownloadingPhoto && originalResult.bitmap != null,
                            border = BorderStroke(1.dp, Color(0xFF60A5FA)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("fullscreen_download_photo_button")
                        ) {
                            if (isDownloadingPhoto) {
                                CircularProgressIndicator(
                                    color = Color(0xFF60A5FA),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Скачать фото",
                                color = Color(0xFF93C5FD),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = triggerExportAllPhotosPdf,
                            enabled = !isExportingPdf && effectivePhotos.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .heightIn(min = 48.dp)
                                .testTag("fullscreen_export_all_photos_pdf_button")
                        ) {
                            if (isExportingPdf) {
                                CircularProgressIndicator(
                                    color = Color.White,
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (effectivePhotos.size > 1) {
                                    "Все фото в PDF (${effectivePhotos.size})"
                                } else {
                                    "Фото в PDF"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
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
