package com.example.data.local

import android.content.Context
import com.example.data.model.SiteMemberDocument
import com.example.data.model.TaskDocument
import com.example.data.seed.CorporateSeedData
import org.json.JSONArray
import org.json.JSONObject

/**
 * Надёжное локальное хранилище задач, участников, удалённых записей и настроек организации/участка.
 * Обеспечивает мгновенное сохранение на телефоне независимо от состояния сети/VPN,
 * синхронизируясь поверх облачной базы Firestore.
 */
object LocalWorkspaceStore {
    private const val PREFS_NAME = "ezhednevnik_workspace_store_v2"
    private const val KEY_TASKS = "tasks_json"
    private const val KEY_DELETED_TASK_IDS = "deleted_task_ids"
    private const val KEY_MEMBERS = "members_json"
    private const val KEY_DELETED_MEMBER_IDS = "deleted_member_ids"
    private const val KEY_ORG_NAME = "organization_name"
    private const val KEY_SITE_NAME = "site_name"
    private const val KEY_SUBDIVISIONS = "admin_subdivisions_v2_json"
    private const val KEY_SITES = "admin_sites_v2_json"

    val DEFAULT_SUBDIVISIONS: List<String> = emptyList()

    val DEFAULT_SITES: List<String> = emptyList()

    private val LEGACY_FAKE_SUBDIVISIONS = setOf(
        "Бригада №1 • Монолит и ЖБК",
        "Бригада №2 • Инженерные сети (ЭОМ и ОВиК)",
        "Служба ИТР и стройконтроля",
        "Административно-управленческий персонал"
    )

    private val LEGACY_FAKE_SITES = setOf(
        "Участок №1 — ПГС и Инженерные сети",
        "Участок №2 — СМР и Отделка",
        "Участок №3 — Наружные сети и Благоустройство"
    )

    fun loadOrganizationName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ORG_NAME, CorporateSeedData.DEFAULT_ORG_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: CorporateSeedData.DEFAULT_ORG_NAME
    }

    fun loadSiteName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SITE_NAME, CorporateSeedData.DEFAULT_SITE_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: CorporateSeedData.DEFAULT_SITE_NAME
    }

    fun saveOrganizationAndSite(context: Context, orgName: String, siteName: String) {
        val cleanSite = siteName.trim().ifBlank { CorporateSeedData.DEFAULT_SITE_NAME }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ORG_NAME, orgName.trim().ifBlank { CorporateSeedData.DEFAULT_ORG_NAME })
            .putString(KEY_SITE_NAME, cleanSite)
            .apply()
        val currentSites = loadSites(context)
        if (cleanSite.isNotBlank() && cleanSite !in LEGACY_FAKE_SITES && cleanSite !in currentSites) {
            saveSites(context, listOf(cleanSite) + currentSites)
        }
    }

    fun loadSubdivisions(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SUBDIVISIONS, null)
        val fromStore = if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i).trim()
                        if (item.isNotBlank()) add(item)
                    }
                }
            }.getOrDefault(emptyList())
        }
        val fromApprovedMembers = loadMembers(context)
            .filter { !it.isPendingRole && !it.isRejected && !it.isWorkspaceConfig }
            .map { it.groupName.trim() }
            .filter { it.isNotBlank() && it !in LEGACY_FAKE_SUBDIVISIONS && it != "Основное подразделение" }
        val fromConfig = loadMembers(context)
            .firstOrNull { it.isWorkspaceConfig }
            ?.decodeWorkspaceSubdivisions()
            .orEmpty()
        return (fromStore + fromConfig + fromApprovedMembers).distinct()
    }

    fun saveSubdivisions(context: Context, subdivisions: List<String>) {
        val cleaned = subdivisions.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val arr = JSONArray(cleaned)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SUBDIVISIONS, arr.toString())
            .apply()
    }

    fun loadSites(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SITES, null)
        val fromStore = if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i).trim()
                        if (item.isNotBlank()) add(item)
                    }
                }
            }.getOrDefault(emptyList())
        }
        val fromApprovedMembers = loadMembers(context)
            .filter { !it.isPendingRole && !it.isRejected && !it.isWorkspaceConfig }
            .map { it.effectiveSiteName.trim() }
            .filter { it.isNotBlank() && it !in LEGACY_FAKE_SITES }
        val fromConfig = loadMembers(context)
            .firstOrNull { it.isWorkspaceConfig }
            ?.decodeWorkspaceSites()
            .orEmpty()
        val activeSite = prefs.getString(KEY_SITE_NAME, null)?.trim().orEmpty()
        val extraActive = if (activeSite.isNotBlank() && activeSite !in LEGACY_FAKE_SITES) listOf(activeSite) else emptyList()
        return (fromStore + fromConfig + extraActive + fromApprovedMembers).distinct()
    }

    fun saveSites(context: Context, sites: List<String>) {
        val cleaned = sites.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val arr = JSONArray(cleaned)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SITES, arr.toString())
            .apply()
    }

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
        val activeKeys = mutableSetOf<String>()
        members.forEach { m ->
            arr.put(memberToJson(m))
            val rawId = m.id.trim()
            val mId = m.memberId.trim()
            if (rawId.isNotBlank()) activeKeys.add(rawId)
            if (mId.isNotBlank()) activeKeys.add(mId)
            val canonical = SiteMemberDocument.canonicalMemberId(mId.ifBlank { rawId }, m.email)
            if (canonical.isNotBlank()) activeKeys.add(canonical)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentDeleted = prefs.getStringSet(KEY_DELETED_MEMBER_IDS, emptySet()).orEmpty()
        val cleanedDeleted = currentDeleted - activeKeys
        prefs.edit()
            .putString(KEY_MEMBERS, arr.toString())
            .putStringSet(KEY_DELETED_MEMBER_IDS, cleanedDeleted)
            .apply()
    }

    fun loadDeletedMemberIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawDeleted = prefs.getStringSet(KEY_DELETED_MEMBER_IDS, emptySet()) ?: emptySet()
        if (rawDeleted.isEmpty()) return emptySet()
        val activeMembers = loadMembers(context)
        val activeIds = buildSet {
            activeMembers.forEach { m ->
                if (m.id.isNotBlank()) add(m.id.trim())
                if (m.memberId.isNotBlank()) add(m.memberId.trim())
                val canonical = SiteMemberDocument.canonicalMemberId(m.memberId.ifBlank { m.id }, m.email)
                if (canonical.isNotBlank()) add(canonical)
            }
        }
        val cleaned = rawDeleted - activeIds
        if (cleaned.size != rawDeleted.size) {
            prefs.edit().putStringSet(KEY_DELETED_MEMBER_IDS, cleaned).apply()
        }
        return cleaned
    }

    fun addDeletedMemberId(context: Context, memberId: String) {
        val current = loadDeletedMemberIds(context).toMutableSet()
        current.add(memberId)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_DELETED_MEMBER_IDS, current)
            .apply()
    }

    fun removeDeletedMemberIds(context: Context, memberIds: Collection<String>) {
        if (memberIds.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_DELETED_MEMBER_IDS, emptySet()).orEmpty().toMutableSet()
        if (current.removeAll(memberIds.toSet())) {
            prefs.edit().putStringSet(KEY_DELETED_MEMBER_IDS, current).apply()
        }
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
        put("targetVolume", t.targetVolume)
        put("completedVolume", t.completedVolume)
        put("volumeUnit", t.volumeUnit)
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
            targetVolume = obj.optDouble("targetVolume", 0.0),
            completedVolume = obj.optDouble("completedVolume", 0.0),
            volumeUnit = obj.optString("volumeUnit", ""),
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
        put("personnelNumber", m.effectivePersonnelNumber)
        put("siteName", m.effectiveSiteName)
        put("phone", m.effectivePhone)
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
            approvedByName = obj.optString("approvedByName").takeIf { it.isNotBlank() },
            personnelNumber = obj.optString("personnelNumber", ""),
            siteName = obj.optString("siteName", ""),
            phone = obj.optString("phone", "")
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
