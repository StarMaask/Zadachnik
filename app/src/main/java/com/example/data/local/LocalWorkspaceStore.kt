package com.example.data.local

import android.content.Context
import com.example.data.model.SiteMemberDocument
import com.example.data.model.TaskDocument
import org.json.JSONArray
import org.json.JSONObject

/**
 * Надёжное локальное хранилище задач, участников и удалённых записей.
 * Обеспечивает мгновенное сохранение на телефоне независимо от состояния сети/VPN,
 * синхронизируясь поверх облачной базы Firestore.
 */
object LocalWorkspaceStore {
    private const val PREFS_NAME = "ezhednevnik_workspace_store_v2"
    private const val KEY_TASKS = "tasks_json"
    private const val KEY_DELETED_TASK_IDS = "deleted_task_ids"
    private const val KEY_MEMBERS = "members_json"
    private const val KEY_DELETED_MEMBER_IDS = "deleted_member_ids"

    fun loadTasks(context: Context): List<TaskDocument> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TASKS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(jsonToTask(obj))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveTasks(context: Context, tasks: List<TaskDocument>) {
        val arr = JSONArray()
        tasks.forEach { task ->
            arr.put(taskToJson(task))
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TASKS, arr.toString())
            .apply()
    }

    fun loadDeletedTaskIds(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_DELETED_TASK_IDS, emptySet()) ?: emptySet()
    }

    fun addDeletedTaskId(context: Context, taskId: String) {
        val current = loadDeletedTaskIds(context).toMutableSet()
        current.add(taskId)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_DELETED_TASK_IDS, current)
            .apply()
    }

    fun loadMembers(context: Context): List<SiteMemberDocument> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MEMBERS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(jsonToMember(obj))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveMembers(context: Context, members: List<SiteMemberDocument>) {
        val arr = JSONArray()
        members.forEach { m ->
            arr.put(memberToJson(m))
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MEMBERS, arr.toString())
            .apply()
    }

    fun loadDeletedMemberIds(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_DELETED_MEMBER_IDS, emptySet()) ?: emptySet()
    }

    fun addDeletedMemberId(context: Context, memberId: String) {
        val current = loadDeletedMemberIds(context).toMutableSet()
        current.add(memberId)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_DELETED_MEMBER_IDS, current)
            .apply()
    }

    private fun taskToJson(t: TaskDocument): JSONObject = JSONObject().apply {
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

    private fun jsonToTask(obj: JSONObject): TaskDocument {
        return TaskDocument(
            id = obj.optString("id"),
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
    }

    private fun memberToJson(m: SiteMemberDocument): JSONObject = JSONObject().apply {
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

    private fun jsonToMember(obj: JSONObject): SiteMemberDocument {
        val mId = obj.optString("memberId").ifBlank { obj.optString("id") }
        return SiteMemberDocument(
            id = mId,
            memberId = mId,
            email = obj.optString("email"),
            fullName = obj.optString("fullName"),
            position = obj.optString("position", "Сотрудник"),
            role = obj.optString("role", "PENDING"),
            approvalStatus = obj.optString("approvalStatus", "PENDING"),
            groupId = obj.optString("groupId", "grp_monolith"),
            groupName = obj.optString("groupName", "Основное подразделение"),
            approvedByName = obj.optString("approvedByName").takeIf { it.isNotBlank() }
        )
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                add(arr.optString(i))
            }
        }
    }
}
