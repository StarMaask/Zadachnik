package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Утилита для сохранения реальных снимков с камеры (с нанесением штампа даты/времени/объекта),
 * генерации графических фотоштампов СМР и загрузки миниатюр/полноэкранных фото.
 */
object PhotoCaptureUtils {

    private fun photosDir(context: Context): File {
        val dir = File(context.filesDir, "task_photos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun nowStampText(): String =
        SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.forLanguageTag("ru")).format(Date())

    fun saveCameraBitmapWithStamp(
        context: Context,
        sourceBitmap: Bitmap,
        taskTitle: String,
        authorName: String,
        gpsCoords: String,
        comment: String
    ): String {
        val targetWidth = sourceBitmap.width.coerceAtLeast(640)
        val scale = targetWidth.toFloat() / sourceBitmap.width.coerceAtLeast(1)
        val targetHeight = (sourceBitmap.height * scale).toInt().coerceAtLeast(480)

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
            comment = comment
        )

        val file = File(photosDir(context), "cam_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            outBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    fun generatePhotoStampFile(
        context: Context,
        taskTitle: String,
        authorName: String,
        gpsCoords: String,
        comment: String,
        photoIndex: Int
    ): String {
        val width = 960
        val height = 720
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
        for (x in 0..width step 60) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        }
        for (y in 0..height step 60) {
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
            strokeWidth = 4f
        }
        val rect = RectF(60f, 60f, (width - 60).toFloat(), (height - 200).toFloat())
        canvas.drawRoundRect(rect, 24f, 24f, cardPaint)
        canvas.drawRoundRect(rect, 24f, 24f, borderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#60A5FA")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("ФОТОШТАМП СМР #$photoIndex", 100f, 130f, titlePaint)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cleanTask = taskTitle.ifBlank { "Контроль выполнения работ" }.take(38)
        canvas.drawText(cleanTask, 100f, 200f, bodyPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#CBD5E1")
            textSize = 24f
        }
        canvas.drawText(
            "Примечание: ${comment.ifBlank { "Контрольная фиксация узла на участке" }.take(45)}",
            100f,
            265f,
            subPaint
        )
        canvas.drawText("Исполнитель: $authorName", 100f, 320f, subPaint)
        canvas.drawText("Время съёмки: ${nowStampText()}", 100f, 375f, subPaint)
        canvas.drawText("Координаты: ${gpsCoords.ifBlank { "Участок №1" }}", 100f, 430f, subPaint)

        drawMetadataStampOverlay(
            canvas = canvas,
            width = width,
            height = height,
            taskTitle = taskTitle,
            authorName = authorName,
            gpsCoords = gpsCoords,
            comment = comment
        )

        val file = File(photosDir(context), "stamp_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    private fun drawMetadataStampOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        taskTitle: String,
        authorName: String,
        gpsCoords: String,
        comment: String
    ) {
        val barHeight = (height * 0.20f).coerceAtLeast(110f)
        val topY = height - barHeight

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(210, 15, 23, 42)
        }
        canvas.drawRect(0f, topY, width.toFloat(), height.toFloat(), barPaint)

        val textScale = (width / 720f).coerceIn(0.75f, 1.5f)
        val line1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#F8FAFC")
            textSize = 22f * textScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val line2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#94A3B8")
            textSize = 18f * textScale
        }

        val padX = 24f * textScale
        canvas.drawText(
            "${nowStampText()} • ${taskTitle.take(36)}",
            padX,
            topY + 36f * textScale,
            line1Paint
        )
        canvas.drawText(
            "Автор: $authorName • GPS: ${gpsCoords.ifBlank { "Участок" }}",
            padX,
            topY + 68f * textScale,
            line2Paint
        )
        if (comment.isNotBlank()) {
            canvas.drawText(
                "Комментарий: ${comment.take(48)}",
                padX,
                topY + 96f * textScale,
                line2Paint
            )
        }
    }

    fun loadBitmapFromUriOrPath(uriOrPath: String): Bitmap? {
        if (uriOrPath.isBlank()) return null
        val cleanPath = uriOrPath.removePrefix("file://")
        val file = File(cleanPath)
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)
        }.getOrNull()
    }

    fun deletePhotoFileIfLocal(uriOrPath: String) {
        val cleanPath = uriOrPath.removePrefix("file://")
        val file = File(cleanPath)
        if (file.exists() && file.absolutePath.contains("task_photos")) {
            runCatching { file.delete() }
        }
    }
}
