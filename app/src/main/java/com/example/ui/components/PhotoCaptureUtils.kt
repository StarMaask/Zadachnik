package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Утилита для сохранения реальных снимков с камеры (с нанесением штампа даты/времени/объекта),
 * кросс-девайсной упаковки сжатого JPEG в Base64 для синхронизации через Cloud Firestore,
 * генерации графических фотоштампов СМР и загрузки миниатюр/полноэкранных фото на любом устройстве.
 */
object PhotoCaptureUtils {

    private const val B64_DELIMITER = "#b64="
    private const val MAX_CLOUD_IMAGE_DIMENSION = 560
    private const val MAX_LOCAL_IMAGE_DIMENSION = 960

    private val bitmapCache = object : LruCache<String, Bitmap>(24) {}

    private fun photosDir(context: Context): File {
        val dir = File(context.filesDir, "task_photos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun nowStampText(): String =
        SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.forLanguageTag("ru")).format(Date())

    /**
     * Сжимает Bitmap в компактный JPEG и кодирует в строку Base64 (без переносов строк и символов '|'),
     * чтобы снимок гарантированно передавался через Cloud Firestore на телефоны проверяющих ИТР и Руководителя.
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
            var quality = 64
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            while (baos.size() > 28_000 && quality > 36) {
                baos.reset()
                quality -= 10
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
        val file = File(cleanPath)
        if (!file.exists()) return uriOrPath
        val bmp = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull() ?: return uriOrPath
        val b64 = encodeBitmapToCloudBase64(bmp)
        return if (b64.isNotBlank()) {
            "${file.absolutePath}$B64_DELIMITER$b64"
        } else {
            uriOrPath
        }
    }

    /**
     * Загружает Bitmap из content:// URI (например, из системного Photo Picker), наносит штамп СМР
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
        val decoded = runCatching {
            context.contentResolver.openInputStream(contentUri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull() ?: return null

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
        val srcW = sourceBitmap.width.coerceAtLeast(1)
        val srcH = sourceBitmap.height.coerceAtLeast(1)
        val maxSide = maxOf(srcW, srcH)
        val downScale = if (maxSide > MAX_LOCAL_IMAGE_DIMENSION) {
            MAX_LOCAL_IMAGE_DIMENSION.toFloat() / maxSide.toFloat()
        } else if (maxSide < 480) {
            640f / maxSide.toFloat()
        } else {
            1f
        }
        val targetWidth = (srcW * downScale).toInt().coerceAtLeast(480)
        val targetHeight = (srcH * downScale).toInt().coerceAtLeast(360)

        val outBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        canvas.drawBitmap(
            Bitmap.createScaledBitmap(sourceBitmap, targetWidth, targetHeight, true),
            0f,
            0f,
            null
        )

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

    fun generatePhotoStampFile(
        context: Context,
        taskTitle: String,
        authorName: String,
        gpsCoords: String,
        comment: String,
        photoIndex: Int
    ): String {
        val stampTime = nowStampText()
        val bitmap = createStampBitmap(
            taskTitle = taskTitle,
            authorName = authorName,
            gpsCoords = gpsCoords,
            comment = comment,
            stampTimeText = stampTime,
            photoIndex = photoIndex
        )

        val file = File(photosDir(context), "stamp_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        val b64 = encodeBitmapToCloudBase64(bitmap)
        val combinedUri = if (b64.isNotBlank()) {
            "${file.absolutePath}$B64_DELIMITER$b64"
        } else {
            file.absolutePath
        }
        bitmapCache.put(cacheKeyFor(combinedUri), bitmap)
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

        // Фон чертежной карточки фотофиксации
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#0F172A")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Координатная сетка
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#1E293B")
            strokeWidth = 2f
        }
        for (x in 0..width step 50) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        }
        for (y in 0..height step 50) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
        }

        // Рамка штампа в центре
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#1E293B")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#3B82F6")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        val rect = RectF(48f, 44f, (width - 48).toFloat(), (height - 165).toFloat())
        canvas.drawRoundRect(rect, 22f, 22f, cardPaint)
        canvas.drawRoundRect(rect, 22f, 22f, borderPaint)

        // Бейдж подтверждённой фиксации
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#1D4ED8")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(80f, 74f, 430f, 118f), 12f, 12f, badgePaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#EFF6FF")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("ФОТОФИКСАЦИЯ СМР #$photoIndex", 98f, 104f, titlePaint)

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
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#38BDF8")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "Отметка: ${comment.ifBlank { "Выполнение СМР на объекте" }.take(44)}",
            80f,
            222f,
            accentPaint
        )
        canvas.drawText(
            "Исполнитель: ${authorName.ifBlank { "Сотрудник участка" }}",
            80f,
            268f,
            subPaint
        )
        canvas.drawText(
            "Дата и время: ${stampTimeText.ifBlank { nowStampText() }}",
            80f,
            314f,
            subPaint
        )
        canvas.drawText(
            "Геометка: ${gpsCoords.ifBlank { "Участок СМР" }.take(44)}",
            80f,
            360f,
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
        val barHeight = (height * 0.20f).coerceAtLeast(104f)
        val topY = height - barHeight

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(215, 15, 23, 42)
        }
        canvas.drawRect(0f, topY, width.toFloat(), height.toFloat(), barPaint)

        val textScale = (width / 720f).coerceIn(0.72f, 1.45f)
        val line1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#F8FAFC")
            textSize = 21f * textScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val line2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#94A3B8")
            textSize = 17f * textScale
        }

        val padX = 22f * textScale
        val effectiveTitle = taskTitle.ifBlank { comment.ifBlank { "Фотоотчёт СМР" } }
        canvas.drawText(
            "${stampTimeText.ifBlank { nowStampText() }} • ${effectiveTitle.take(36)}",
            padX,
            topY + 34f * textScale,
            line1Paint
        )
        canvas.drawText(
            "Автор: ${authorName.ifBlank { "Исполнитель" }} • GPS: ${gpsCoords.ifBlank { "Участок" }.take(34)}",
            padX,
            topY + 64f * textScale,
            line2Paint
        )
        if (comment.isNotBlank()) {
            canvas.drawText(
                "Комментарий: ${comment.take(48)}",
                padX,
                topY + 92f * textScale,
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
     * 1) локального файла на устройстве (если снимок сделан на этом телефоне);
     * 2) встроенного в uriString блока Base64 (#b64=... или data:image/...;base64,...), переданного через Cloud Firestore с другого телефона;
     * 3) если снимок был отправлен со старой версии приложения без Base64-полезной нагрузки, генерирует графический фотоштамп СМР с метаданными отчёта.
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
                val decodedFile = runCatching {
                    BitmapFactory.decodeFile(file.absolutePath)
                }.getOrNull()
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

        // 3. Fallback для ранее отправленных отчётов без Base64 или виртуальных штампов (stamp://)
        if (uriOrPath.isNotBlank() || fallbackComment.isNotBlank()) {
            val inferredIndex = Regex("""#(\d+)""").find(fallbackComment)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
            val generated = createStampBitmap(
                taskTitle = fallbackTaskTitle.ifBlank { fallbackComment.ifBlank { "Фотоотчёт по заданию" } },
                authorName = fallbackAuthor.ifBlank { "Исполнитель СМР" },
                gpsCoords = fallbackGps.ifBlank { "Строительный участок" },
                comment = fallbackComment.ifBlank { "Фотофиксация СМР #$inferredIndex" },
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
