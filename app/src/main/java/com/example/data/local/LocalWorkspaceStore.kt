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
    private const val KEY_NOTIFIED_TASK_EVENTS = "notified_task_events_v1"
    private const val KEY_TASK_NOTIF_BASELINE_READY = "task_notif_baseline_ready_v1"
    private const val KEY_PENDING_SYNC_TASK_IDS = "pending_cloud_sync_task_ids_v1"

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

    fun loadNotifiedTaskEvents(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_NOTIFIED_TASK_EVENTS, emptySet())
            ?.toSet()
            ?: emptySet()
    }

    fun saveNotifiedTaskEvents(context: Context, events: Set<String>) {
        val bounded = if (events.size > 600) {
            events.toList().takeLast(600).toSet()
        } else {
            events.toSet()
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_NOTIFIED_TASK_EVENTS, bounded)
            .apply()
    }

    fun isTaskNotifBaselineReady(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_TASK_NOTIF_BASELINE_READY, false)
    }

    fun setTaskNotifBaselineReady(context: Context, ready: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TASK_NOTIF_BASELINE_READY, ready)
            .apply()
    }

    fun loadOrganizationName(context: Context): String {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ORG_NAME, CorporateSeedData.DEFAULT_ORG_NAME)
            ?.trim()
            .orEmpty()
        return if (raw.isBlank() || CorporateSeedData.isLegacyFakeSiteName(raw)) {
            CorporateSeedData.DEFAULT_ORG_NAME
        } else {
            raw
        }
    }

    fun loadSiteName(context: Context): String {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SITE_NAME, CorporateSeedData.DEFAULT_SITE_NAME)
            ?.trim()
            .orEmpty()
        return if (raw.isBlank() || CorporateSeedData.isLegacyFakeSiteName(raw)) {
            CorporateSeedData.DEFAULT_SITE_NAME
        } else {
            raw
        }
    }

    fun saveOrganizationAndSite(context: Context, orgName: String, siteName: String) {
        val cleanSite = CorporateSeedData.sanitizeSiteOrLocation(siteName, CorporateSeedData.DEFAULT_SITE_NAME)
        val cleanOrg = orgName.trim().takeIf { it.isNotBlank() && !CorporateSeedData.isLegacyFakeSiteName(it) }
            ?: CorporateSeedData.DEFAULT_ORG_NAME
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ORG_NAME, cleanOrg)
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

    fun loadPendingSyncTaskIds(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_PENDING_SYNC_TASK_IDS, emptySet())
            ?.toSet()
            ?: emptySet()
    }

    fun savePendingSyncTaskIds(context: Context, taskIds: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_PENDING_SYNC_TASK_IDS, taskIds.toSet())
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
        val rawGroup = obj.optString("groupName")
        val cleanGroup = CorporateSeedData.sanitizeSubdivisionName(rawGroup)
        val rawGeo = obj.optString("geoLocation").takeIf { it.isNotBlank() }
        val cleanGeo = rawGeo?.let {
            if (CorporateSeedData.isLegacyFakeSiteName(it) || CorporateSeedData.isLegacyFakeSubdivisionName(it)) {
                null
            } else {
                it
            }
        }
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
            groupName = cleanGroup,
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
            geoLocation = cleanGeo
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
        val rawGroup = obj.optString("groupName", "")
        val cleanGroup = if (CorporateSeedData.isLegacyFakeSubdivisionName(rawGroup)) {
            CorporateSeedData.DEFAULT_SUBDIVISION_NAME
        } else {
            rawGroup.ifBlank { CorporateSeedData.DEFAULT_SUBDIVISION_NAME }
        }
        val rawSite = obj.optString("siteName", "")
        val cleanSite = if (CorporateSeedData.isLegacyFakeSiteName(rawSite)) {
            ""
        } else {
            rawSite
        }
        return SiteMemberDocument(
            id = mId,
            memberId = mId,
            email = obj.optString("email"),
            fullName = obj.optString("fullName"),
            position = obj.optString("position", "Сотрудник"),
            role = obj.optString("role", "PENDING"),
            approvalStatus = obj.optString("approvalStatus", "PENDING"),
            groupId = obj.optString("groupId", "grp_monolith"),
            groupName = cleanGroup,
            approvedByName = obj.optString("approvedByName").takeIf { it.isNotBlank() },
            personnelNumber = obj.optString("personnelNumber", ""),
            siteName = cleanSite,
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

    private const val KEY_RPG_WORKSPACE_STATE = "rpg_workspace_state_json"

    fun serializeRpgStateToJson(state: com.example.domain.rpg.RpgWorkspaceState): String {
        val root = JSONObject().apply {
            val cfgObj = JSONObject().apply {
                put("onTimeBonusRate", state.config.onTimeBonusRate)
                put("earlyBonusRate", state.config.earlyBonusRate)
                put("cleanPhotoBonusRate", state.config.cleanPhotoBonusRate)
                put("overduePenaltyRate", state.config.overduePenaltyRate)
                put("rejectedPenaltyRate", state.config.rejectedPenaltyRate)
                put("streakTier1Days", state.config.streakTier1Days)
                put("streakTier1Multiplier", state.config.streakTier1Multiplier)
                put("streakTier2Days", state.config.streakTier2Days)
                put("streakTier2Multiplier", state.config.streakTier2Multiplier)
                put("streakTier3Days", state.config.streakTier3Days)
                put("streakTier3Multiplier", state.config.streakTier3Multiplier)
                put("levelCurveBaseXp", state.config.levelCurveBaseXp)
                put("maxLevel", state.config.maxLevel)
                put("seasonalResetSpPenalty", state.config.seasonalResetSpPenalty)
                val prioObj = JSONObject()
                state.config.priorityRules.forEach { (prio, rule) ->
                    prioObj.put(
                        prio.code,
                        JSONObject().apply {
                            put("baseXp", rule.baseXp)
                            put("approvalBonusXp", rule.approvalBonusXp)
                        }
                    )
                }
                put("priorityRules", prioObj)
            }
            put("config", cfgObj)
            put("season", JSONObject().apply {
                put("id", state.currentSeason.id)
                put("titleRu", state.currentSeason.titleRu)
                put("quarterCode", state.currentSeason.quarterCode)
                put("startEpochDay", state.currentSeason.startEpochDay)
                put("endEpochDay", state.currentSeason.endEpochDay)
                put("isActive", state.currentSeason.isActive)
            })
            val profilesArr = JSONArray()
            state.profiles.values.forEach { p ->
                profilesArr.put(JSONObject().apply {
                    put("employeeId", p.employeeId)
                    put("totalXp", p.totalXp)
                    put("seasonXp", p.seasonXp)
                    put("skillPointsAvailable", p.skillPointsAvailable)
                    put("skillPointsSpent", p.skillPointsSpent)
                    put("leaderboardPrivacy", p.leaderboardPrivacy.code)
                    put("seasonSkillResetDoneForSeasonId", p.seasonSkillResetDoneForSeasonId)
                    put("lastEvaluatedEpochDay", p.lastEvaluatedEpochDay)
                    put("skills", JSONArray().apply {
                        p.skills.forEach { sk ->
                            put(JSONObject().apply {
                                put("skillId", sk.skillId)
                                put("level", sk.level)
                            })
                        }
                    })
                    put("achievements", JSONArray().apply {
                        p.achievements.forEach { a ->
                            put(JSONObject().apply {
                                put("achievementId", a.achievementId)
                                put("unlockedAtMillis", a.unlockedAtMillis)
                                put("progressNote", a.progressNote)
                            })
                        }
                    })
                    put("questProgress", JSONArray().apply {
                        p.questProgress.forEach { qp ->
                            put(JSONObject().apply {
                                put("questId", qp.questId)
                                put("periodKey", qp.periodKey)
                                put("currentCount", qp.currentCount)
                                put("countedTaskIds", JSONArray(qp.countedTaskIds))
                                put("completedAtMillis", qp.completedAtMillis ?: -1L)
                                put("rewarded", qp.rewarded)
                            })
                        }
                    })
                    put("inventory", JSONObject().apply {
                        put("ownedItemIds", JSONArray(p.inventory.ownedItemIds))
                        put("equippedBadgeId", p.inventory.equippedBadgeId ?: "")
                        put("equippedFrameId", p.inventory.equippedFrameId ?: "")
                        put("equippedTitleId", p.inventory.equippedTitleId ?: "")
                        put("streakRecoveryTokens", p.inventory.streakRecoveryTokens)
                    })
                    put("reputation", JSONObject().apply {
                        put("score", p.reputation.score)
                        put("history", JSONArray().apply {
                            p.reputation.history.take(20).forEach { h ->
                                put(JSONObject().apply {
                                    put("id", h.id)
                                    put("delta", h.delta)
                                    put("resultingScore", h.resultingScore)
                                    put("reason", h.reason)
                                    put("createdAtMillis", h.createdAtMillis)
                                })
                            }
                        })
                    })
                    put("streak", JSONObject().apply {
                        put("currentDays", p.streak.currentDays)
                        put("bestDays", p.streak.bestDays)
                        put("lastActivityEpochDay", p.streak.lastActivityEpochDay)
                        put("isAtRisk", p.streak.isAtRisk)
                        put("recoveryUsedCount", p.streak.recoveryUsedCount)
                    })
                    put("prestige", JSONObject().apply {
                        put("prestigeLevel", p.prestige.prestigeLevel)
                        put("permanentXpMultiplierBonus", p.prestige.permanentXpMultiplierBonus)
                        put("emblemTitle", p.prestige.emblemTitle)
                        put("activatedAtMillis", p.prestige.activatedAtMillis ?: -1L)
                    })
                })
            }
            put("profiles", profilesArr)
            val txArr = JSONArray()
            state.transactions.take(300).forEach { tx ->
                txArr.put(JSONObject().apply {
                    put("id", tx.id)
                    put("employeeId", tx.employeeId)
                    put("employeeName", tx.employeeName)
                    put("taskId", tx.taskId)
                    put("taskTitle", tx.taskTitle)
                    put("type", tx.type.code)
                    put("xpDelta", tx.xpDelta)
                    put("reputationDelta", tx.reputationDelta)
                    put("priorityAtCompletion", tx.priorityAtCompletion?.code ?: "")
                    put("appliedMultipliersSummary", tx.appliedMultipliersSummary)
                    put("reason", tx.reason)
                    put("actorId", tx.actorId)
                    put("actorName", tx.actorName)
                    put("idempotencyKey", tx.idempotencyKey)
                    put("createdAtMillis", tx.createdAtMillis)
                    put("isRolledBack", tx.isRolledBack)
                })
            }
            put("transactions", txArr)
        }
        return root.toString()
    }

    fun saveRpgState(context: Context, state: com.example.domain.rpg.RpgWorkspaceState) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RPG_WORKSPACE_STATE, serializeRpgStateToJson(state))
            .apply()
    }

    fun deserializeRpgStateFromJson(raw: String): com.example.domain.rpg.RpgWorkspaceState? {
        if (raw.isBlank()) return null
        return runCatching {
            val root = JSONObject(raw)
            val cfgObj = root.optJSONObject("config")
            val defaultRules = com.example.domain.rpg.GamificationConfig.defaultPriorityRules().toMutableMap()
            val prioObj = cfgObj?.optJSONObject("priorityRules")
            if (prioObj != null) {
                com.example.data.model.TaskPriority.entries.forEach { p ->
                    prioObj.optJSONObject(p.code)?.let { r ->
                        defaultRules[p] = com.example.domain.rpg.PriorityXpRule(
                            priority = p,
                            baseXp = r.optInt("baseXp", defaultRules[p]?.baseXp ?: 10),
                            approvalBonusXp = r.optInt("approvalBonusXp", defaultRules[p]?.approvalBonusXp ?: 5)
                        )
                    }
                }
            }
            val config = com.example.domain.rpg.GamificationConfig(
                priorityRules = defaultRules,
                onTimeBonusRate = cfgObj?.optDouble("onTimeBonusRate", 0.20) ?: 0.20,
                earlyBonusRate = cfgObj?.optDouble("earlyBonusRate", 0.35) ?: 0.35,
                cleanPhotoBonusRate = cfgObj?.optDouble("cleanPhotoBonusRate", 0.10) ?: 0.10,
                overduePenaltyRate = cfgObj?.optDouble("overduePenaltyRate", 0.50) ?: 0.50,
                rejectedPenaltyRate = cfgObj?.optDouble("rejectedPenaltyRate", 0.25) ?: 0.25,
                streakTier1Days = cfgObj?.optInt("streakTier1Days", 5) ?: 5,
                streakTier1Multiplier = cfgObj?.optDouble("streakTier1Multiplier", 1.10) ?: 1.10,
                streakTier2Days = cfgObj?.optInt("streakTier2Days", 10) ?: 10,
                streakTier2Multiplier = cfgObj?.optDouble("streakTier2Multiplier", 1.25) ?: 1.25,
                streakTier3Days = cfgObj?.optInt("streakTier3Days", 20) ?: 20,
                streakTier3Multiplier = cfgObj?.optDouble("streakTier3Multiplier", 1.50) ?: 1.50,
                levelCurveBaseXp = cfgObj?.optDouble("levelCurveBaseXp", 45.0) ?: 45.0,
                maxLevel = cfgObj?.optInt("maxLevel", 20) ?: 20,
                seasonalResetSpPenalty = cfgObj?.optInt("seasonalResetSpPenalty", 1) ?: 1
            )
            val sObj = root.optJSONObject("season")
            val season = if (sObj != null) {
                com.example.domain.rpg.Season(
                    id = sObj.optString("id", "season_2026_q4"),
                    titleRu = sObj.optString("titleRu", "Сезон Q4 2026: Ударная смена"),
                    quarterCode = sObj.optString("quarterCode", "2026-Q4"),
                    startEpochDay = sObj.optLong("startEpochDay", 20728L),
                    endEpochDay = sObj.optLong("endEpochDay", 20819L),
                    isActive = sObj.optBoolean("isActive", true)
                )
            } else {
                com.example.domain.rpg.RpgWorkspaceState().currentSeason
            }
            val profilesMap = linkedMapOf<String, com.example.domain.rpg.EmployeeRpgProfile>()
            val pArr = root.optJSONArray("profiles")
            if (pArr != null) {
                for (i in 0 until pArr.length()) {
                    val po = pArr.optJSONObject(i) ?: continue
                    val empId = po.optString("employeeId").ifBlank { continue }
                    val skList = buildList {
                        val sArr = po.optJSONArray("skills")
                        if (sArr != null) {
                            for (j in 0 until sArr.length()) {
                                val so = sArr.optJSONObject(j) ?: continue
                                add(com.example.domain.rpg.UserSkill(so.optString("skillId"), so.optInt("level", 0)))
                            }
                        }
                    }
                    val achList = buildList {
                        val aArr = po.optJSONArray("achievements")
                        if (aArr != null) {
                            for (j in 0 until aArr.length()) {
                                val ao = aArr.optJSONObject(j) ?: continue
                                add(
                                    com.example.domain.rpg.UserAchievement(
                                        ao.optString("achievementId"),
                                        ao.optLong("unlockedAtMillis", 0L),
                                        ao.optString("progressNote", "")
                                    )
                                )
                            }
                        }
                    }
                    val qpList = buildList {
                        val qArr = po.optJSONArray("questProgress")
                        if (qArr != null) {
                            for (j in 0 until qArr.length()) {
                                val qo = qArr.optJSONObject(j) ?: continue
                                val compAt = qo.optLong("completedAtMillis", -1L).takeIf { it > 0L }
                                add(
                                    com.example.domain.rpg.UserQuestProgress(
                                        questId = qo.optString("questId"),
                                        periodKey = qo.optString("periodKey"),
                                        currentCount = qo.optInt("currentCount", 0),
                                        countedTaskIds = jsonArrayToList(qo.optJSONArray("countedTaskIds")),
                                        completedAtMillis = compAt,
                                        rewarded = qo.optBoolean("rewarded", false)
                                    )
                                )
                            }
                        }
                    }
                    val invObj = po.optJSONObject("inventory")
                    val inv = com.example.domain.rpg.UserInventory(
                        ownedItemIds = jsonArrayToList(invObj?.optJSONArray("ownedItemIds")),
                        equippedBadgeId = invObj?.optString("equippedBadgeId")?.takeIf { it.isNotBlank() },
                        equippedFrameId = invObj?.optString("equippedFrameId")?.takeIf { it.isNotBlank() },
                        equippedTitleId = invObj?.optString("equippedTitleId")?.takeIf { it.isNotBlank() },
                        streakRecoveryTokens = invObj?.optInt("streakRecoveryTokens", 1) ?: 1
                    )
                    val repObj = po.optJSONObject("reputation")
                    val repHist = buildList {
                        val hArr = repObj?.optJSONArray("history")
                        if (hArr != null) {
                            for (j in 0 until hArr.length()) {
                                val ho = hArr.optJSONObject(j) ?: continue
                                add(
                                    com.example.domain.rpg.ReputationEntry(
                                        id = ho.optString("id"),
                                        delta = ho.optInt("delta", 0),
                                        resultingScore = ho.optInt("resultingScore", 50),
                                        reason = ho.optString("reason"),
                                        createdAtMillis = ho.optLong("createdAtMillis", 0L)
                                    )
                                )
                            }
                        }
                    }
                    val strObj = po.optJSONObject("streak")
                    val streak = com.example.domain.rpg.Streak(
                        currentDays = strObj?.optInt("currentDays", 0) ?: 0,
                        bestDays = strObj?.optInt("bestDays", 0) ?: 0,
                        lastActivityEpochDay = strObj?.optLong("lastActivityEpochDay", 0L) ?: 0L,
                        isAtRisk = strObj?.optBoolean("isAtRisk", false) ?: false,
                        recoveryUsedCount = strObj?.optInt("recoveryUsedCount", 0) ?: 0
                    )
                    val prObj = po.optJSONObject("prestige")
                    val prestige = com.example.domain.rpg.PrestigeState(
                        prestigeLevel = prObj?.optInt("prestigeLevel", 0) ?: 0,
                        permanentXpMultiplierBonus = prObj?.optDouble("permanentXpMultiplierBonus", 0.0) ?: 0.0,
                        emblemTitle = prObj?.optString("emblemTitle", "Без престижа") ?: "Без престижа",
                        activatedAtMillis = prObj?.optLong("activatedAtMillis", -1L)?.takeIf { it > 0L }
                    )
                    profilesMap[empId] = com.example.domain.rpg.EmployeeRpgProfile(
                        employeeId = empId,
                        totalXp = po.optInt("totalXp", 0),
                        seasonXp = po.optInt("seasonXp", 0),
                        skillPointsAvailable = po.optInt("skillPointsAvailable", 1),
                        skillPointsSpent = po.optInt("skillPointsSpent", 0),
                        skills = skList,
                        achievements = achList,
                        questProgress = qpList,
                        inventory = inv,
                        reputation = com.example.domain.rpg.Reputation(
                            score = repObj?.optInt("score", 50) ?: 50,
                            history = repHist
                        ),
                        streak = streak,
                        prestige = prestige,
                        leaderboardPrivacy = com.example.domain.rpg.LeaderboardPrivacy.fromCode(
                            po.optString("leaderboardPrivacy", "SITE")
                        ),
                        seasonSkillResetDoneForSeasonId = po.optString("seasonSkillResetDoneForSeasonId", ""),
                        lastEvaluatedEpochDay = po.optLong("lastEvaluatedEpochDay", 0L)
                    )
                }
            }
            val txList = buildList {
                val tArr = root.optJSONArray("transactions")
                if (tArr != null) {
                    for (i in 0 until tArr.length()) {
                        val to = tArr.optJSONObject(i) ?: continue
                        val prioCode = to.optString("priorityAtCompletion")
                        add(
                            com.example.domain.rpg.XpTransaction(
                                id = to.optString("id"),
                                employeeId = to.optString("employeeId"),
                                employeeName = to.optString("employeeName"),
                                taskId = to.optString("taskId"),
                                taskTitle = to.optString("taskTitle"),
                                type = com.example.domain.rpg.XpTransactionType.fromCode(to.optString("type")),
                                xpDelta = to.optInt("xpDelta", 0),
                                reputationDelta = to.optInt("reputationDelta", 0),
                                priorityAtCompletion = prioCode.takeIf { it.isNotBlank() }?.let {
                                    com.example.data.model.TaskPriority.fromCode(it)
                                },
                                appliedMultipliersSummary = to.optString("appliedMultipliersSummary"),
                                reason = to.optString("reason"),
                                actorId = to.optString("actorId"),
                                actorName = to.optString("actorName"),
                                idempotencyKey = to.optString("idempotencyKey"),
                                createdAtMillis = to.optLong("createdAtMillis", 0L),
                                isRolledBack = to.optBoolean("isRolledBack", false)
                            )
                        )
                    }
                }
            }
            com.example.domain.rpg.RpgWorkspaceState(
                config = config,
                currentSeason = season,
                profiles = profilesMap,
                transactions = txList
            )
        }.getOrNull()
    }

    fun loadRpgState(context: Context): com.example.domain.rpg.RpgWorkspaceState? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RPG_WORKSPACE_STATE, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return deserializeRpgStateFromJson(raw)
    }

    fun mergeRpgProfiles(
        a: com.example.domain.rpg.EmployeeRpgProfile,
        b: com.example.domain.rpg.EmployeeRpgProfile
    ): com.example.domain.rpg.EmployeeRpgProfile {
        val scoreA = a.prestige.prestigeLevel * 50000 + a.totalXp + a.skillPointsSpent * 100 + a.achievements.size * 50 + a.questProgress.size * 10
        val scoreB = b.prestige.prestigeLevel * 50000 + b.totalXp + b.skillPointsSpent * 100 + b.achievements.size * 50 + b.questProgress.size * 10
        val primary = if (scoreA >= scoreB) a else b
        val secondary = if (scoreA >= scoreB) b else a

        val mergedSkillsMap = linkedMapOf<String, Int>()
        (secondary.skills + primary.skills).forEach { sk ->
            val prev = mergedSkillsMap[sk.skillId] ?: 0
            if (sk.level > prev) mergedSkillsMap[sk.skillId] = sk.level
        }
        val mergedSkills = mergedSkillsMap.entries.map { (id, lvl) ->
            com.example.domain.rpg.UserSkill(skillId = id, level = lvl)
        }

        val mergedAchievements = (primary.achievements + secondary.achievements)
            .distinctBy { it.achievementId }

        val mergedQuestsMap = linkedMapOf<String, com.example.domain.rpg.UserQuestProgress>()
        (secondary.questProgress + primary.questProgress).forEach { qp ->
            val key = "${qp.questId}_${qp.periodKey}"
            val existing = mergedQuestsMap[key]
            if (existing == null || qp.currentCount > existing.currentCount || (qp.rewarded && !existing.rewarded)) {
                val combinedIds = ((existing?.countedTaskIds ?: emptyList()) + qp.countedTaskIds).distinct()
                mergedQuestsMap[key] = qp.copy(
                    countedTaskIds = combinedIds,
                    currentCount = maxOf(qp.currentCount, combinedIds.size),
                    rewarded = qp.rewarded || (existing?.rewarded == true)
                )
            }
        }

        val mergedOwnedItems = (primary.inventory.ownedItemIds + secondary.inventory.ownedItemIds).distinct()
        val mergedInventory = primary.inventory.copy(
            ownedItemIds = mergedOwnedItems,
            equippedBadgeId = primary.inventory.equippedBadgeId ?: secondary.inventory.equippedBadgeId,
            equippedFrameId = primary.inventory.equippedFrameId ?: secondary.inventory.equippedFrameId,
            equippedTitleId = primary.inventory.equippedTitleId ?: secondary.inventory.equippedTitleId,
            streakRecoveryTokens = maxOf(primary.inventory.streakRecoveryTokens, secondary.inventory.streakRecoveryTokens)
        )

        val mergedRepHistory = (primary.reputation.history + secondary.reputation.history)
            .distinctBy { it.id.ifBlank { "${it.createdAtMillis}_${it.reason}" } }
            .sortedByDescending { it.createdAtMillis }
            .take(30)

        val mergedStreak = if (primary.streak.lastActivityEpochDay >= secondary.streak.lastActivityEpochDay) {
            primary.streak.copy(bestDays = maxOf(primary.streak.bestDays, secondary.streak.bestDays))
        } else {
            secondary.streak.copy(bestDays = maxOf(primary.streak.bestDays, secondary.streak.bestDays))
        }

        return primary.copy(
            totalXp = maxOf(primary.totalXp, secondary.totalXp),
            seasonXp = maxOf(primary.seasonXp, secondary.seasonXp),
            skills = mergedSkills,
            achievements = mergedAchievements,
            questProgress = mergedQuestsMap.values.toList(),
            inventory = mergedInventory,
            reputation = primary.reputation.copy(history = mergedRepHistory),
            streak = mergedStreak
        )
    }

    fun mergeRpgWorkspaceStates(
        local: com.example.domain.rpg.RpgWorkspaceState,
        remote: com.example.domain.rpg.RpgWorkspaceState
    ): com.example.domain.rpg.RpgWorkspaceState {
        val mergedProfiles = linkedMapOf<String, com.example.domain.rpg.EmployeeRpgProfile>()
        remote.profiles.forEach { (empId, prof) ->
            mergedProfiles[empId] = prof
        }
        local.profiles.forEach { (empId, localProf) ->
            val remoteProf = mergedProfiles[empId]
            mergedProfiles[empId] = if (remoteProf == null) {
                localProf
            } else {
                mergeRpgProfiles(localProf, remoteProf)
            }
        }

        val txMap = linkedMapOf<String, com.example.domain.rpg.XpTransaction>()
        (remote.transactions + local.transactions).forEach { tx ->
            val key = tx.idempotencyKey.ifBlank { tx.id }
            val existing = txMap[key]
            if (existing == null) {
                txMap[key] = tx
            } else if (tx.isRolledBack && !existing.isRolledBack) {
                txMap[key] = tx
            }
        }
        val mergedTransactions = txMap.values
            .sortedByDescending { it.createdAtMillis }
            .take(400)

        return remote.copy(
            profiles = mergedProfiles,
            transactions = mergedTransactions
        )
    }
}

