package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.data.model.SiteMemberDocument
import com.example.data.model.TaskDocument
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalCorporateStorage {
    private const val PREFS_NAME = "ezhednevnik_clean_store_v2"
    private const val KEY_TASKS = "tasks_json"
    private const val KEY_MEMBERS = "members_json"
    private const val KEY_DELETED_TASK_IDS = "deleted_task_ids"
    private const val KEY_DELETED_MEMBER_IDS = "deleted_member_ids"
    private const val KEY_LEGACY_PURGED = "legacy_seed_purged_v2"

    val LEGACY_FAKE_MEMBER_IDS = setOf(
        "emp_head_1",
        "emp_itr_1",
        "emp_itr_2",
        "emp_worker_1",
        "emp_worker_2",
        "emp_worker_3",
        "emp_worker_4",
        "pending_emp_1",
        "pending_emp_2"
    )

    fun isLegacyFakeTaskId(id: String): Boolean {
        return id.startsWith("seed_task_") || id.contains("_seed_task_")
    }

    fun isLegacyFakeMember(member: SiteMemberDocument): Boolean {
        val mid = member.memberId.ifBlank { member.id }
        if (mid in LEGACY_FAKE_MEMBER_IDS) return true
        val email = member.email.lowercase()
        return email.endsWith("@glavstroy.ru")
    }

    fun isLegacyPurged(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LEGACY_PURGED, false)
    }

    fun markLegacyPurged(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LEGACY_PURGED, true).apply()
    }

    fun loadDeletedTaskIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_DELETED_TASK_IDS, emptySet()) ?: emptySet()
    }

    fun addDeletedTaskId(context: Context, taskId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = loadDeletedTaskIds(context).toMutableSet().apply { add(taskId) }
        prefs.edit().putStringSet(KEY_DELETED_TASK_IDS, updated).apply()
    }

    fun loadDeletedMemberIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_DELETED_MEMBER_IDS, emptySet()) ?: emptySet()
    }

    fun addDeletedMemberId(context: Context, memberId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = loadDeletedMemberIds(context).toMutableSet().apply { add(memberId) }
        prefs.edit().putStringSet(KEY_DELETED_MEMBER_IDS, updated).apply()
    }

    fun saveTasks(context: Context, tasks: List<TaskDocument>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        tasks.filterNot { isLegacyFakeTaskId(it.id) }.forEach { t ->
            val obj = JSONObject().apply {
                put("id", t.id)
                put("userId", t.userId)
                put("title", t.title)
                put("description", t.description)
                put("creatorId", t.creatorId)
                put("creatorName", t.creatorName)
                put("assigneeId", t.assigneeId)
                put("assigneeName", t.assigneeName)
                put("groupId", t.groupId)
                put("groupName", t.groupName)
                put("siteId", t.siteId)
                put("priority", t.priority)
                put("status", t.status)
                put("dueDateEpochDay", t.dueDateEpochDay)
                put("originalDueDateEpochDay", t.originalDueDateEpochDay)
                put("deadlineTime", t.deadlineTime)
                put("carryoverCount", t.carryoverCount)
                put("requiresPhotoReport", t.requiresPhotoReport)
                put("minPhotoCount", t.minPhotoCount)
                put("requiresApproval", t.requiresApproval)
                put("recurrenceRule", t.recurrenceRule)
                put("tags", JSONArray(t.tags))
                put("subtaskItems", JSONArray(t.subtaskItems))
                put("photoItems", JSONArray(t.photoItems))
                put("commentItems", JSONArray(t.commentItems))
                put("historyItems", JSONArray(t.historyItems))
                put("rejectionReason", t.rejectionReason ?: "")
                put("approvedByName", t.approvedByName ?: "")
                put("geoLocation", t.geoLocation ?: "")
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_TASKS, arr.toString()).apply()
    }

    fun loadTasks(context: Context): List<TaskDocument> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TASKS, null) ?: return emptyList()
        val deletedIds = loadDeletedTaskIds(context)
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<TaskDocument>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                if (id.isBlank() || id in deletedIds || isLegacyFakeTaskId(id)) continue
                list.add(
                    TaskDocument(
                        id = id,
                        userId = obj.optString("userId"),
                        title = obj.optString("title"),
                        description = obj.optString("description"),
                        creatorId = obj.optString("creatorId"),
                        creatorName = obj.optString("creatorName"),
                        assigneeId = obj.optString("assigneeId"),
                        assigneeName = obj.optString("assigneeName"),
                        groupId = obj.optString("groupId"),
                        groupName = obj.optString("groupName"),
                        siteId = obj.optString("siteId", "site_1"),
                        priority = obj.optString("priority", "MEDIUM"),
                        status = obj.optString("status", "NEW"),
                        dueDateEpochDay = obj.optLong("dueDateEpochDay", 20768L),
                        originalDueDateEpochDay = obj.optLong("originalDueDateEpochDay", 20768L),
                        deadlineTime = obj.optString("deadlineTime", "18:00"),
                        carryoverCount = obj.optLong("carryoverCount", 0L),
                        requiresPhotoReport = obj.optBoolean("requiresPhotoReport", false),
                        minPhotoCount = obj.optLong("minPhotoCount", 1L),
                        requiresApproval = obj.optBoolean("requiresApproval", true),
                        recurrenceRule = obj.optString("recurrenceRule", "NONE"),
                        tags = jsonArrayToList(obj.optJSONArray("tags")),
                        subtaskItems = jsonArrayToList(obj.optJSONArray("subtaskItems")),
                        photoItems = jsonArrayToList(obj.optJSONArray("photoItems")),
                        commentItems = jsonArrayToList(obj.optJSONArray("commentItems")),
                        historyItems = jsonArrayToList(obj.optJSONArray("historyItems")),
                        rejectionReason = obj.optString("rejectionReason").takeIf { it.isNotBlank() },
                        approvedByName = obj.optString("approvedByName").takeIf { it.isNotBlank() },
                        geoLocation = obj.optString("geoLocation").takeIf { it.isNotBlank() }
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun saveMembers(context: Context, members: List<SiteMemberDocument>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        members.filterNot { isLegacyFakeMember(it) }.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id.ifBlank { m.memberId })
                put("memberId", m.memberId.ifBlank { m.id })
                put("email", m.email)
                put("fullName", m.fullName)
                put("position", m.position)
                put("role", m.role)
                put("approvalStatus", m.approvalStatus)
                put("groupId", m.groupId)
                put("groupName", m.groupName)
                put("approvedByName", m.approvedByName ?: "")
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_MEMBERS, arr.toString()).apply()
    }

    fun loadMembers(context: Context): List<SiteMemberDocument> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_MEMBERS, null) ?: return emptyList()
        val deletedIds = loadDeletedMemberIds(context)
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<SiteMemberDocument>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val memberId = obj.optString("memberId").ifBlank { obj.optString("id") }
                if (memberId.isBlank() || memberId in deletedIds) continue
                val doc = SiteMemberDocument(
                    id = obj.optString("id", memberId),
                    memberId = memberId,
                    email = obj.optString("email"),
                    fullName = obj.optString("fullName"),
                    position = obj.optString("position", "Специалист"),
                    role = obj.optString("role", "PENDING"),
                    approvalStatus = obj.optString("approvalStatus", "PENDING"),
                    groupId = obj.optString("groupId", "grp_monolith"),
                    groupName = obj.optString("groupName", "Основное подразделение"),
                    approvedByName = obj.optString("approvedByName").takeIf { it.isNotBlank() }
                )
                if (!isLegacyFakeMember(doc)) {
                    list.add(doc)
                }
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val res = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            res.add(arr.optString(i))
        }
        return res
    }
}

object PhotoFileManager {

    private fun getPhotoDir(context: Context): File {
        val dir = File(context.filesDir, "task_photos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveCameraBitmapWithStamp(
        context: Context,
        sourceBitmap: Bitmap,
        taskTitle: String,
        authorName: String,
        gpsCoords: String,
        comment: String
    ): String {
        val width = sourceBitmap.width.coerceAtLeast(640)
        val height = sourceBitmap.height.coerceAtLeast(480)
        val scaled = Bitmap.createScaledBitmap(sourceBitmap, width, height, true)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(scaled, 0f, 0f, null)

        drawStampOverlay(
            canvas = canvas,
            width = width,
            height = height,
            taskTitle = taskTitle,
            authorName = authorName,
            gpsCoords = gpsCoords,
            comment = comment
        )

        val file = File(getPhotoDir(context), "cam_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            output.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    fun generateSmrStampPhotoFile(
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

        // Technical blueprint / inspection card background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle technical grid
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            strokeWidth = 2f
        }
        for (x in 0..width step 60) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        }
        for (y in 0..height step 60) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
        }

        // Center badge
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E3A8A")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(60f, 80f, width - 60f, 380f), 24f, 24f, boxPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#60A5FA")
            textSize = 28f
            isFakeBoldText = true
        }
        canvas.drawText("ФОТОШТАМП СМР #$photoIndex • КОНТРОЛЬ ВЫПОЛНЕНИЯ", 96f, 140f, titlePaint)

        val mainTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            isFakeBoldText = true
        }
        val cleanTitle = taskTitle.ifBlank { "Рабочее задание" }
        canvas.drawText(cleanTitle.take(36), 96f, 205f, mainTextPaint)
        if (cleanTitle.length > 36) {
            canvas.drawText(cleanTitle.drop(36).take(36), 96f, 250f, mainTextPaint)
        }

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 26f
        }
        val cleanComment = comment.ifBlank { "Контрольная фиксация узла на участке" }
        canvas.drawText("Примечание: ${cleanComment.take(45)}", 96f, 320f, subPaint)

        drawStampOverlay(
            canvas = canvas,
            width = width,
            height = height,
            taskTitle = taskTitle,
            authorName = authorName,
            gpsCoords = gpsCoords,
            comment = cleanComment
        )

        val file = File(getPhotoDir(context), "stamp_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    private fun drawStampOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        taskTitle: String,
        authorName: String,
        gpsCoords: String,
        comment: String
    ) {
        val barHeight = (height * 0.24f).coerceAtLeast(120f)
        val topY = height - barHeight
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, topY, width.toFloat(), height.toFloat(), overlayPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (width * 0.032f).coerceIn(18f, 30f)
            isFakeBoldText = true
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = (width * 0.026f).coerceIn(14f, 24f)
        }
        val nowStr = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.forLanguageTag("ru")).format(Date())
        val padX = 24f
        var curY = topY + textPaint.textSize + 12f
        canvas.drawText("ШТАМП СМР • $nowStr • $authorName", padX, curY, textPaint)
        curY += smallPaint.textSize + 10f
        canvas.drawText("Задача: ${taskTitle.take(48)}", padX, curY, smallPaint)
        curY += smallPaint.textSize + 8f
        canvas.drawText(
            "Координаты: ${gpsCoords.ifBlank { "Участок №1" }} | ${comment.ifBlank { "Фотоотчёт" }.take(40)}",
            padX,
            curY,
            smallPaint
        )
    }

    fun loadBitmapFromPath(pathOrUri: String): Bitmap? {
        if (pathOrUri.isBlank() || pathOrUri.startsWith("stamp://") || pathOrUri.startsWith("camera://")) {
            return null
        }
        val file = File(pathOrUri)
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)
        }.getOrNull()
    }
}
