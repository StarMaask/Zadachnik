package com.example.domain.rpg

import com.example.data.model.CorporateRole
import com.example.data.model.TaskPriority
import kotlin.math.roundToInt

enum class XpTransactionType(val code: String, val titleRu: String) {
    TASK_COMPLETION("TASK_COMPLETION", "Выполнение задачи"),
    SUPERVISOR_APPROVAL("SUPERVISOR_APPROVAL", "Подтверждение приёмки"),
    QUEST_REWARD("QUEST_REWARD", "Награда за квест"),
    ACHIEVEMENT_BONUS("ACHIEVEMENT_BONUS", "Бонус достижения"),
    REJECTION_PENALTY("REJECTION_PENALTY", "Штраф за возврат"),
    OVERDUE_PENALTY("OVERDUE_PENALTY", "Штраф за просрочку"),
    ROLLBACK_CANCELLATION("ROLLBACK_CANCELLATION", "Откат при отмене задачи"),
    ROLLBACK_REJECTION("ROLLBACK_REJECTION", "Откат при отклонении"),
    MANUAL_ADJUSTMENT("MANUAL_ADJUSTMENT", "Корректировка руководителем"),
    SEASON_RESET("SEASON_RESET", "Сезонный сброс"),
    PRESTIGE_RESET("PRESTIGE_RESET", "Переход на Престиж");

    companion object {
        fun fromCode(code: String): XpTransactionType =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: TASK_COMPLETION
    }
}

data class XpTransaction(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val taskId: String = "",
    val taskTitle: String = "",
    val type: XpTransactionType,
    val xpDelta: Int,
    val reputationDelta: Int = 0,
    val priorityAtCompletion: TaskPriority? = null,
    val appliedMultipliersSummary: String = "",
    val reason: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val idempotencyKey: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isRolledBack: Boolean = false
)

data class PriorityXpRule(
    val priority: TaskPriority,
    val baseXp: Int,
    val approvalBonusXp: Int
)

data class GamificationConfig(
    val priorityRules: Map<TaskPriority, PriorityXpRule> = defaultPriorityRules(),
    val onTimeBonusRate: Double = 0.20,
    val earlyBonusRate: Double = 0.35,
    val cleanPhotoBonusRate: Double = 0.10,
    val overduePenaltyRate: Double = 0.50,
    val rejectedPenaltyRate: Double = 0.25,
    val streakTier1Days: Int = 5,
    val streakTier1Multiplier: Double = 1.10,
    val streakTier2Days: Int = 10,
    val streakTier2Multiplier: Double = 1.25,
    val streakTier3Days: Int = 20,
    val streakTier3Multiplier: Double = 1.50,
    val levelCurveBaseXp: Double = 45.0,
    val levelCurveExponent: Double = 1.5,
    val maxLevel: Int = 20,
    val seasonalResetSpPenalty: Int = 1,
    val prestigePermanentBonusPerLevel: Double = 0.05
) {
    fun ruleFor(priority: TaskPriority): PriorityXpRule =
        priorityRules[priority] ?: defaultPriorityRules()[priority] ?: PriorityXpRule(priority, 10, 5)

    fun xpRequiredForLevel(level: Int): Int {
        if (level <= 1) return 0
        var cumulative = 0.0
        for (n in 1 until level) {
            cumulative += levelCurveBaseXp * Math.pow(n.toDouble(), levelCurveExponent)
        }
        return cumulative.roundToInt().coerceAtLeast(10)
    }

    companion object {
        fun defaultPriorityRules(): Map<TaskPriority, PriorityXpRule> = mapOf(
            TaskPriority.LOW to PriorityXpRule(TaskPriority.LOW, baseXp = 5, approvalBonusXp = 2),
            TaskPriority.MEDIUM to PriorityXpRule(TaskPriority.MEDIUM, baseXp = 10, approvalBonusXp = 5),
            TaskPriority.HIGH to PriorityXpRule(TaskPriority.HIGH, baseXp = 20, approvalBonusXp = 10),
            TaskPriority.CRITICAL to PriorityXpRule(TaskPriority.CRITICAL, baseXp = 35, approvalBonusXp = 15),
            TaskPriority.EMERGENCY to PriorityXpRule(TaskPriority.EMERGENCY, baseXp = 50, approvalBonusXp = 20)
        )
    }
}

data class LevelDefinition(
    val level: Int,
    val titleRu: String,
    val cumulativeXpRequired: Int,
    val rewardSkillPoints: Int
)

data class UserLevel(
    val level: Int,
    val totalXp: Int,
    val currentLevelFloorXp: Int,
    val nextLevelXp: Int,
    val progressFraction: Float,
    val isMaxLevel: Boolean
)

enum class SkillBranch(val code: String, val titleRu: String, val subtitleRu: String) {
    PROFESSIONAL("PROFESSIONAL", "Профессионализм", "Точность исполнения нарядов и сложные приоритеты"),
    QUALITY("QUALITY", "Качество и фотоотчёт", "Фотофиксация и сдача с первого раза без замечаний"),
    TEAM("TEAM", "Командная работа", "Взаимовыручка в бригаде и совместные смены"),
    SAFETY("SAFETY", "ТБ и дисциплина", "Соблюдение сроков, регламентов и безаварийность"),
    LEADERSHIP("LEADERSHIP", "Лидерство и приёмка", "Контроль качества, наставничество и оперативность ИТР");

    companion object {
        fun fromCode(code: String): SkillBranch =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: PROFESSIONAL
    }
}

data class SkillDefinition(
    val id: String,
    val branch: SkillBranch,
    val titleRu: String,
    val descriptionRu: String,
    val maxLevel: Int = 5,
    val xpBonusPercentPerLevel: Int = 2,
    val prerequisiteSkillId: String? = null,
    val minRoleWeight: Int = CorporateRole.WORKER.hierarchyWeight
)

data class UserSkill(
    val skillId: String,
    val level: Int = 0
)

data class SkillPoint(
    val available: Int,
    val totalEarned: Int,
    val spent: Int
)

data class StatusDefinition(
    val rankIndex: Int,
    val minRoleWeight: Int,
    val minLevel: Int,
    val titleRu: String,
    val perkRu: String
)

data class UserStatus(
    val titleRu: String,
    val perkRu: String,
    val rankIndex: Int
)

enum class AchievementRarity(val code: String, val titleRu: String, val hexColor: Long) {
    COMMON("COMMON", "Обычное", 0xFF64748B),
    RARE("RARE", "Редкое", 0xFF2563EB),
    EPIC("EPIC", "Эпическое", 0xFF7C3AED),
    LEGENDARY("LEGENDARY", "Легендарное", 0xFFD97706);

    companion object {
        fun fromCode(code: String): AchievementRarity =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: COMMON
    }
}

data class Achievement(
    val id: String,
    val titleRu: String,
    val descriptionRu: String,
    val rarity: AchievementRarity,
    val xpReward: Int,
    val badgeItemId: String = ""
)

data class UserAchievement(
    val achievementId: String,
    val unlockedAtMillis: Long,
    val progressNote: String = ""
)

enum class QuestCadence(val code: String, val titleRu: String) {
    DAILY("DAILY", "Ежедневный"),
    WEEKLY("WEEKLY", "Еженедельный"),
    SEASONAL("SEASONAL", "Сезонный"),
    STORY("STORY", "Карьерный");

    companion object {
        fun fromCode(code: String): QuestCadence =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DAILY
    }
}

enum class QuestMetricType(val code: String) {
    COMPLETE_TASKS("COMPLETE_TASKS"),
    PHOTO_VERIFIED_TASKS("PHOTO_VERIFIED_TASKS"),
    FIRST_TRY_APPROVALS("FIRST_TRY_APPROVALS"),
    HIGH_OR_ABOVE_TASKS("HIGH_OR_ABOVE_TASKS"),
    SUPERVISOR_REVIEWS("SUPERVISOR_REVIEWS");

    companion object {
        fun fromCode(code: String): QuestMetricType =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: COMPLETE_TASKS
    }
}

data class Quest(
    val id: String,
    val titleRu: String,
    val descriptionRu: String,
    val cadence: QuestCadence,
    val metricType: QuestMetricType,
    val targetCount: Int,
    val xpReward: Int,
    val reputationReward: Int = 1,
    val rewardItemId: String = "",
    val minRoleWeight: Int = CorporateRole.WORKER.hierarchyWeight
)

data class UserQuestProgress(
    val questId: String,
    val periodKey: String,
    val currentCount: Int = 0,
    val countedTaskIds: List<String> = emptyList(),
    val completedAtMillis: Long? = null,
    val rewarded: Boolean = false
)

enum class InventoryItemCategory(val code: String, val titleRu: String) {
    BADGE("BADGE", "Значок"),
    AVATAR_FRAME("AVATAR_FRAME", "Рамка профиля"),
    TITLE("TITLE", "Титул"),
    THEME("THEME", "Тема карточки");

    companion object {
        fun fromCode(code: String): InventoryItemCategory =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: BADGE
    }
}

data class InventoryItem(
    val id: String,
    val nameRu: String,
    val descriptionRu: String,
    val category: InventoryItemCategory,
    val rarity: AchievementRarity,
    val accentHexLong: Long
)

data class UserInventory(
    val ownedItemIds: List<String> = emptyList(),
    val equippedBadgeId: String? = null,
    val equippedFrameId: String? = null,
    val equippedTitleId: String? = null,
    val streakRecoveryTokens: Int = 1
)

data class ReputationEntry(
    val id: String,
    val delta: Int,
    val resultingScore: Int,
    val reason: String,
    val createdAtMillis: Long
)

data class Reputation(
    val score: Int = 50,
    val history: List<ReputationEntry> = emptyList()
) {
    val tierTitleRu: String
        get() = when {
            score >= 90 -> "Образцовая надёжность"
            score >= 75 -> "Высокое доверие"
            score >= 55 -> "Надёжный исполнитель"
            score >= 35 -> "Требует внимания"
            else -> "Критическая зона"
        }
}

data class Streak(
    val currentDays: Int = 0,
    val bestDays: Int = 0,
    val lastActivityEpochDay: Long = 0L,
    val isAtRisk: Boolean = false,
    val recoveryUsedCount: Int = 0
)

data class Season(
    val id: String,
    val titleRu: String,
    val quarterCode: String,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val isActive: Boolean = true
)

enum class LeaderboardPrivacy(val code: String, val titleRu: String) {
    SITE("SITE", "Виден всему участку"),
    GROUP("GROUP", "Только своей бригаде"),
    HIDDEN("HIDDEN", "Скрыт из рейтинга");

    companion object {
        fun fromCode(code: String): LeaderboardPrivacy =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SITE
    }
}

data class LeaderboardEntry(
    val employeeId: String,
    val employeeName: String,
    val role: CorporateRole,
    val groupId: String,
    val groupName: String,
    val rankTitle: String,
    val level: Int,
    val prestigeLevel: Int,
    val seasonXp: Int,
    val totalXp: Int,
    val reputation: Int,
    val streakDays: Int,
    val equippedTitle: String = "",
    val equippedBadgeName: String = "",
    val privacy: LeaderboardPrivacy = LeaderboardPrivacy.SITE
)

data class PrestigeState(
    val prestigeLevel: Int = 0,
    val permanentXpMultiplierBonus: Double = 0.0,
    val emblemTitle: String = "Без престижа",
    val activatedAtMillis: Long? = null
)

data class EmployeeRpgProfile(
    val employeeId: String,
    val totalXp: Int = 0,
    val seasonXp: Int = 0,
    val skillPointsAvailable: Int = 1,
    val skillPointsSpent: Int = 0,
    val skills: List<UserSkill> = emptyList(),
    val achievements: List<UserAchievement> = emptyList(),
    val questProgress: List<UserQuestProgress> = emptyList(),
    val inventory: UserInventory = UserInventory(),
    val reputation: Reputation = Reputation(score = 50),
    val streak: Streak = Streak(),
    val prestige: PrestigeState = PrestigeState(),
    val leaderboardPrivacy: LeaderboardPrivacy = LeaderboardPrivacy.SITE,
    val seasonSkillResetDoneForSeasonId: String = "",
    val lastEvaluatedEpochDay: Long = 0L
)

data class RpgWorkspaceState(
    val config: GamificationConfig = GamificationConfig(),
    val currentSeason: Season = Season(
        id = "season_2026_q4",
        titleRu = "Сезон Q4 2026: Ударная смена",
        quarterCode = "2026-Q4",
        startEpochDay = 20728L,
        endEpochDay = 20819L,
        isActive = true
    ),
    val profiles: Map<String, EmployeeRpgProfile> = emptyMap(),
    val transactions: List<XpTransaction> = emptyList()
)
