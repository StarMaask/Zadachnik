package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.LruCache
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Утилита для съёмки и загрузки реальных фотографий (с камеры через FileProvider и из галереи),
 * корректной обработки EXIF-ориентации и масштабирования, нанесения компактного штампа метаданных
 * и кросс-девайсной синхронизации сжатого JPEG в Base64 через Cloud Firestore.
 */
object PhotoCaptureUtils {

    private const val B64_DELIMITER = "#b64="
    private const val MAX_CLOUD_IMAGE_DIMENSION = 720
    private const val MAX_LOCAL_IMAGE_DIMENSION = 1080
    private const val MAX_CLOUD_JPEG_BYTES = 45_000

    private val bitmapCache = object : LruCache<String, Bitmap>(24) {}

    private fun photosDir(context: Context): File {
        val dir = File(context.filesDir, "task_photos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun createTempCameraCaptureFile(context: Context): File {
        val dir = File(context.cacheDir, "camera_tmp")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "cam_capture_${System.currentTimeMillis()}.jpg")
    }

    fun getFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun nowStampText(): String =
        SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.forLanguageTag("ru")).format(Date())

    /**
     * Сжимает Bitmap в компактный JPEG и кодирует в строку Base64 (без переносов строк и символов '|'),
     * чтобы реальный снимок гарантированно передавался через Cloud Firestore на телефоны проверяющих.
     */
    fun encodeBitmapToCloudBase64(source: Bitmap): String {
        return runCatching {
            val srcW = source.width.coerceAtLeast(1)
            val srcH = source.height.coerceAtLeast(1)
            val maxSide = maxOf(srcW, srcH)
            val scale = if (maxSide > MAX_CLOUD_IMAGE_DIMENSION) {
                MAX_CLOUD_IMAGE_DIMENSION.toFloat() / maxSide.toFloat()
            } else {
                1f
            }
            val targetW = (srcW * scale).toInt().coerceAtLeast(240)
            val targetH = (srcH * scale).toInt().coerceAtLeast(180)
            val scaled = if (targetW != srcW || targetH != srcH) {
                Bitmap.createScaledBitmap(source, targetW, targetH, true)
            } else {
                source
            }

            val baos = ByteArrayOutputStream()
            var quality = 74
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            while (baos.size() > MAX_CLOUD_JPEG_BYTES && quality > 38) {
                baos.reset()
                quality -= 8
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            }
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }.getOrDefault("")
    }

    /**
     * Если uriString содержит только локальный путь к файлу (без #b64=), а сам файл существует на этом телефоне,
     * дописывает сжатый Base64-слепок для автоматической облачной синхронизации со всеми устройствами.
     */
    fun ensureCloudSyncedUriString(uriOrPath: String): String {
        if (uriOrPath.isBlank() || uriOrPath.contains(B64_DELIMITER) || uriOrPath.startsWith("data:image")) {
            return uriOrPath
        }
        val cleanPath = uriOrPath.removePrefix("file://").trim()
        if (cleanPath.startsWith("stamp://")) return uriOrPath
        val file = File(cleanPath)
        if (!file.exists()) return uriOrPath
        val bmp = decodeFileSafely(file, MAX_CLOUD_IMAGE_DIMENSION) ?: return uriOrPath
        val b64 = encodeBitmapToCloudBase64(bmp)
        return if (b64.isNotBlank()) {
            "${file.absolutePath}$B64_DELIMITER$b64"
        } else {
            uriOrPath
        }
    }

    private fun computeSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        while (width / inSampleSize > maxDim * 2 || height / inSampleSize > maxDim * 2) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap)
    }

    private fun decodeFileSafely(file: File, maxDim: Int = MAX_LOCAL_IMAGE_DIMENSION): Bitmap? {
        if (!file.exists() || file.length() <= 0L) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val decoded = runCatching {
                val source = ImageDecoder.createSource(file)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width.coerceAtLeast(1)
                    val h = info.size.height.coerceAtLeast(1)
                    val maxSide = maxOf(w, h)
                    if (maxSide > maxDim) {
                        val ratio = maxDim.toFloat() / maxSide.toFloat()
                        decoder.setTargetSize(
                            (w * ratio).toInt().coerceAtLeast(1),
                            (h * ratio).toInt().coerceAtLeast(1)
                        )
                    }
                }
            }.getOrNull()
            if (decoded != null) return decoded
        }

        return runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = computeSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDim)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val raw = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
            val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull()
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL
            applyExifOrientation(raw, orientation)
        }.getOrNull()
    }

    private fun decodeUriSafely(
        context: Context,
        contentUri: Uri,
        maxDim: Int = MAX_LOCAL_IMAGE_DIMENSION
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val decoded = runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, contentUri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width.coerceAtLeast(1)
                    val h = info.size.height.coerceAtLeast(1)
                    val maxSide = maxOf(w, h)
                    if (maxSide > maxDim) {
                        val ratio = maxDim.toFloat() / maxSide.toFloat()
                        decoder.setTargetSize(
                            (w * ratio).toInt().coerceAtLeast(1),
                            (h * ratio).toInt().coerceAtLeast(1)
                        )
                    }
                }
            }.getOrNull()
            if (decoded != null) return decoded
        }

        // Надёжный fallback: копируем поток во временный файл, чтобы считать границы, EXIF и декодировать с inSampleSize
        val tmpCopy = createTempCameraCaptureFile(context)
        return try {
            val copied = runCatching {
                context.contentResolver.openInputStream(contentUri)?.use { input ->
                    FileOutputStream(tmpCopy).use { out ->
                        input.copyTo(out)
                    }
                    true
                } ?: false
            }.getOrDefault(false)
            if (!copied || !tmpCopy.exists() || tmpCopy.length() <= 0L) return null
            decodeFileSafely(tmpCopy, maxDim)
        } finally {
            runCatching { tmpCopy.delete() }
        }
    }

    /**
     * Обрабатывает реальный снимок с камеры, сохранённый через FileProvider, наносит компактный штамп
     * и возвращает комбинированный путь + Base64 (#b64=...) для синхронизации между устройствами.
     */
    fun saveCameraFileWithStamp(
        context: Context,
        capturedFile: File,
        taskTitle: String,
        authorName: String,
        gpsCoords: String,
        comment: String
    ): String? {
        val decoded = decodeFileSafely(capturedFile, MAX_LOCAL_IMAGE_DIMENSION) ?: return null
        return try {
            saveCameraBitmapWithStamp(
                context = context,
                sourceBitmap = decoded,
                taskTitle = taskTitle,
                authorName = authorName,
                gpsCoords = gpsCoords,
                comment = comment
            )
        } finally {
            runCatching { capturedFile.delete() }
        }
    }

    /**
     * Загружает реальное фото из content:// URI (например, из системного Photo Picker), наносит штамп
     * и возвращает комбинированный URI с встроенным Base64 для синхронизации между устройствами.
     */
    fun savePickedUriWithStamp(
        context: Context,
        contentUri: Uri,
        taskTitle: String,
        authorName: String,
        gpsCoords: String,
        comment: String
    ): String? {
        val decoded = decodeUriSafely(context, contentUri, MAX_LOCAL_IMAGE_DIMENSION) ?: return null
        return saveCameraBitmapWithStamp(
            context = context,
            sourceBitmap = decoded,
            taskTitle = taskTitle,
            authorName = authorName,
            gpsCoords = gpsCoords,
            comment = comment
        )
    }

    fun saveCameraBitmapWithStamp(
        context: Context,
        sourceBitmap: Bitmap,
        taskTitle: String,
        authorName: String,
        gpsCoords: String,
        comment: String
    ): String {
        val softwareSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            sourceBitmap.config == Bitmap.Config.HARDWARE
        ) {
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: sourceBitmap
        } else {
            sourceBitmap
        }
        val srcW = softwareSource.width.coerceAtLeast(1)
        val srcH = softwareSource.height.coerceAtLeast(1)
        val maxSide = maxOf(srcW, srcH)
        val downScale = if (maxSide > MAX_LOCAL_IMAGE_DIMENSION) {
            MAX_LOCAL_IMAGE_DIMENSION.toFloat() / maxSide.toFloat()
        } else if (maxSide < 480) {
            640f / maxSide.toFloat()
        } else {
            1f
        }
        val targetWidth = (srcW * downScale).toInt().coerceAtLeast(320)
        val targetHeight = (srcH * downScale).toInt().coerceAtLeast(240)

        val outBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        val scaledSource = if (targetWidth != srcW || targetHeight != srcH) {
            Bitmap.createScaledBitmap(softwareSource, targetWidth, targetHeight, true)
        } else {
            softwareSource
        }
        canvas.drawBitmap(scaledSource, 0f, 0f, null)

        drawMetadataStampOverlay(
            canvas = canvas,
            width = targetWidth,
            height = targetHeight,
            taskTitle = taskTitle,
            authorName = authorName,
            gpsCoords = gpsCoords,
            comment = comment,
            stampTimeText = nowStampText()
        )

        val file = File(photosDir(context), "cam_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            outBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        val b64 = encodeBitmapToCloudBase64(outBitmap)
        val combinedUri = if (b64.isNotBlank()) {
            "${file.absolutePath}$B64_DELIMITER$b64"
        } else {
            file.absolutePath
        }
        bitmapCache.put(cacheKeyFor(combinedUri), outBitmap)
        return combinedUri
    }

    fun createStampBitmap(
        taskTitle: String,
        authorName: String,
        gpsCoords: String,
        comment: String,
        stampTimeText: String = nowStampText(),
        photoIndex: Int = 1
    ): Bitmap {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#0F172A")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#1E293B")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#3B82F6")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        val rect = RectF(48f, 44f, (width - 48).toFloat(), (height - 150).toFloat())
        canvas.drawRoundRect(rect, 22f, 22f, cardPaint)
        canvas.drawRoundRect(rect, 22f, 22f, borderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#EFF6FF")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("ФОТОФИКСАЦИЯ #$photoIndex", 80f, 108f, titlePaint)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cleanTask = taskTitle.ifBlank { comment.ifBlank { "Контроль выполнения работ" } }.take(40)
        canvas.drawText(cleanTask, 80f, 168f, bodyPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#CBD5E1")
            textSize = 21f
        }
        canvas.drawText(
            "Исполнитель: ${authorName.ifBlank { "Сотрудник участка" }}",
            80f,
            240f,
            subPaint
        )
        canvas.drawText(
            "Дата и время: ${stampTimeText.ifBlank { nowStampText() }}",
            80f,
            286f,
            subPaint
        )
        canvas.drawText(
            "Геометка: ${gpsCoords.ifBlank { "Участок" }.take(44)}",
            80f,
            332f,
            subPaint
        )

        drawMetadataStampOverlay(
            canvas = canvas,
            width = width,
            height = height,
            taskTitle = taskTitle.ifBlank { comment },
            authorName = authorName.ifBlank { "Сотрудник" },
            gpsCoords = gpsCoords,
            comment = comment,
            stampTimeText = stampTimeText
        )
        return bitmap
    }

    private fun drawMetadataStampOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        taskTitle: String,
        authorName: String,
        gpsCoords: String,
        comment: String,
        stampTimeText: String = nowStampText()
    ) {
        val barHeight = (height * 0.14f).coerceIn(78f, 128f)
        val topY = height - barHeight

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(196, 15, 23, 42)
        }
        canvas.drawRect(0f, topY, width.toFloat(), height.toFloat(), barPaint)

        val textScale = (width / 760f).coerceIn(0.66f, 1.35f)
        val line1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#F8FAFC")
            textSize = 19f * textScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val line2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#CBD5E1")
            textSize = 15.5f * textScale
        }

        val padX = 20f * textScale
        val effectiveTitle = taskTitle.ifBlank { comment.ifBlank { "Фотофиксация" } }
        canvas.drawText(
            "${stampTimeText.ifBlank { nowStampText() }} • ${effectiveTitle.take(36)}",
            padX,
            topY + 28f * textScale,
            line1Paint
        )
        canvas.drawText(
            "Автор: ${authorName.ifBlank { "Исполнитель" }} • ${gpsCoords.ifBlank { "Участок" }.take(34)}",
            padX,
            topY + 54f * textScale,
            line2Paint
        )
        if (comment.isNotBlank() && barHeight >= 96f) {
            canvas.drawText(
                comment.take(52),
                padX,
                topY + 78f * textScale,
                line2Paint
            )
        }
    }

    private fun cacheKeyFor(
        uriOrPath: String,
        fallbackComment: String = "",
        fallbackAuthor: String = "",
        fallbackTime: String = ""
    ): String {
        val prefix = uriOrPath.take(120)
        val suffix = uriOrPath.takeLast(64)
        return "${uriOrPath.length}_${prefix}_${suffix}_${fallbackComment}_${fallbackAuthor}_$fallbackTime"
    }

    /**
     * Загружает Bitmap из:
     * 1) локального файла на устройстве (если снимок сделан/выбран на этом телефоне);
     * 2) встроенного в uriString блока Base64 (#b64=... или data:image/...;base64,...), переданного через Cloud Firestore с другого телефона;
     * 3) для явно виртуальных старых штампов (stamp://) генерирует карточку; для реальных фото без файла/Base64 возвращает null.
     */
    fun loadBitmapFromUriOrPath(
        uriOrPath: String,
        fallbackComment: String = "",
        fallbackAuthor: String = "",
        fallbackTime: String = "",
        fallbackGps: String = "",
        fallbackTaskTitle: String = ""
    ): Bitmap? {
        if (uriOrPath.isBlank() && fallbackComment.isBlank()) return null

        val key = cacheKeyFor(uriOrPath, fallbackComment, fallbackAuthor, fallbackTime)
        bitmapCache.get(key)?.let { cached ->
            if (!cached.isRecycled) return cached
        }

        val localPart = uriOrPath.substringBefore(B64_DELIMITER).removePrefix("file://").trim()
        val b64Part = when {
            uriOrPath.contains(B64_DELIMITER) -> uriOrPath.substringAfter(B64_DELIMITER, "").trim()
            uriOrPath.startsWith("data:image") && uriOrPath.contains("base64,") ->
                uriOrPath.substringAfter("base64,", "").trim()
            else -> ""
        }

        // 1. Попытка чтения локального файла (на устройстве автора снимка)
        if (localPart.isNotBlank() && !localPart.startsWith("stamp://") && !localPart.startsWith("data:")) {
            val file = File(localPart)
            if (file.exists()) {
                val decodedFile = decodeFileSafely(file, MAX_LOCAL_IMAGE_DIMENSION)
                if (decodedFile != null) {
                    bitmapCache.put(key, decodedFile)
                    return decodedFile
                }
            }
        }

        // 2. Декодирование встроенного сжатого JPEG из Base64 (на телефоне руководителя / ИТР / другого участника)
        if (b64Part.isNotBlank()) {
            val decodedB64 = runCatching {
                val bytes = Base64.decode(b64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
            if (decodedB64 != null) {
                bitmapCache.put(key, decodedB64)
                return decodedB64
            }
        }

        // 3. Явный fallback только для legacy stamp://
        if (localPart.startsWith("stamp://")) {
            val inferredIndex = Regex("""#(\d+)""").find(fallbackComment)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
            val generated = createStampBitmap(
                taskTitle = fallbackTaskTitle.ifBlank { fallbackComment.ifBlank { "Фотоотчёт по заданию" } },
                authorName = fallbackAuthor.ifBlank { "Исполнитель" },
                gpsCoords = fallbackGps.ifBlank { "Строительный участок" },
                comment = fallbackComment.ifBlank { "Фотофиксация #$inferredIndex" },
                stampTimeText = fallbackTime.ifBlank { nowStampText() },
                photoIndex = inferredIndex
            )
            bitmapCache.put(key, generated)
            return generated
        }

        return null
    }

    fun deletePhotoFileIfLocal(uriOrPath: String) {
        val cleanPath = uriOrPath.substringBefore(B64_DELIMITER).removePrefix("file://").trim()
        if (cleanPath.isBlank()) return
        val file = File(cleanPath)
        if (file.exists() && file.absolutePath.contains("task_photos")) {
            runCatching { file.delete() }
        }
    }
}
