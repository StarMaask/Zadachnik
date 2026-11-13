package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import java.util.Locale

enum class CorporateRole(
    val code: String,
    val titleRu: String,
    val hierarchyLevel: Int,
    val badgeDescription: String
) {
    WORKER(
        code = "WORKER",
        titleRu = "Рабочий",
        hierarchyLevel = 1,
        badgeDescription = "Личные задачи, фотоотчёты, отчёт по выполненному объёму"
    ),
    ITR(
        code = "ITR",
        titleRu = "ИТР (Прораб / Инженер)",
        hierarchyLevel = 2,
        badgeDescription = "Постановка задач рабочим, приёмка фотоотчётов, сводный отчёт по подразделению"
    ),
    HEAD(
        code = "HEAD",
        titleRu = "Руководитель участка",
        hierarchyLevel = 3,
        badgeDescription = "Управление участком, контроль ИТР и рабочих, утверждение ролей"
    ),
    ADMIN(
        code = "ADMIN",
        titleRu = "Администратор",
        hierarchyLevel = 4,
        badgeDescription = "Полные права администратора системы без ограничений"
    );

    val isAdmin: Boolean
        get() = this == ADMIN

    val isAdminOrHead: Boolean
        get() = this == ADMIN || this == HEAD

    val shortTitleRu: String
        get() = when (this) {
            WORKER -> "Рабочий"
            ITR -> "ИТР"
            HEAD -> "Руководитель"
            ADMIN -> "Администратор"
        }

    fun canAssignTo(targetRole: CorporateRole): Boolean {
        return when (this) {
            ADMIN, HEAD -> true
            ITR -> targetRole == ITR || targetRole == WORKER
            WORKER -> targetRole == WORKER
        }
    }

    fun canAssignToOtherEmployees(): Boolean = this == ADMIN || this == HEAD || this == ITR

    fun canApproveReportOf(assigneeRole: CorporateRole): Boolean {
        return when (this) {
            ADMIN, HEAD -> true
            ITR -> assigneeRole == WORKER
            WORKER -> false
        }
    }

    fun canExportConsolidatedPersonnelReport(): Boolean = this == ADMIN || this == HEAD || this == ITR

    fun canManageTemplatesAndGroups(): Boolean = this == ADMIN || this == HEAD

    companion object {
        fun fromCode(code: String): CorporateRole =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: WORKER
    }
}

enum class TaskPriority(val code: String, val titleRu: String, val weight: Int) {
    LOW("LOW", "Низкий", 1),
    MEDIUM("MEDIUM", "Средний", 2),
    HIGH("HIGH", "Высокий", 3),
    CRITICAL("CRITICAL", "Критический", 4);

    companion object {
        fun fromCode(code: String): TaskPriority =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: MEDIUM
    }
}

enum class TaskStatus(val code: String, val titleRu: String) {
    NEW("NEW", "Новая"),
    IN_PROGRESS("IN_PROGRESS", "В работе"),
    ON_REVIEW("ON_REVIEW", "На проверке"),
    APPROVED("APPROVED", "Подтверждена"),
    REJECTED("REJECTED", "Отклонена"),
    OVERDUE("OVERDUE", "Просрочена"),
    CANCELLED("CANCELLED", "Отменена"),
    ARCHIVED("ARCHIVED", "В архиве");

    val isTerminalOrDone: Boolean
        get() = this == APPROVED || this == CANCELLED || this == ARCHIVED

    val isEligibleForCarryover: Boolean
        get() = this == NEW || this == IN_PROGRESS || this == REJECTED || this == OVERDUE

    companion object {
        fun fromCode(code: String): TaskStatus =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: NEW
    }
}

enum class RecurrenceRule(val code: String, val titleRu: String) {
    NONE("NONE", "Без повтора"),
    DAILY("DAILY", "Ежедневно"),
    WEEKDAYS("WEEKDAYS", "По будням (Пн–Пт)"),
    WEEKLY("WEEKLY", "Еженедельно"),
    MONTHLY("MONTHLY", "Ежемесячно"),
    CUSTOM("CUSTOM", "По спец. графику СМР");

    companion object {
        fun fromCode(code: String): RecurrenceRule =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: NONE
    }
}

enum class ReportType(
    val code: String,
    val titleRu: String,
    val subtitleRu: String,
    val isConsolidatedOnly: Boolean = false
) {
    NEXT_DAY_PLAN(
        code = "NEXT_DAY_PLAN",
        titleRu = "План работ на следующий день",
        subtitleRu = "Список запланированных работ с ФИО исполнителей, объёмом, сроками и требованием фотоотчёта"
    ),
    DAILY_REPORT(
        code = "DAILY_REPORT",
        titleRu = "Отчет за текущий день",
        subtitleRu = "Выполненные и незавершённые задачи с фактическим объёмом работ, ФИО и фотофиксацией"
    ),
    PERIOD_REPORT(
        code = "PERIOD_REPORT",
        titleRu = "Отчёт за произвольный период",
        subtitleRu = "Агрегация работ по дням, исполнителям и бригадам с совмещением плана и факта"
    ),
    PHOTO_QUALITY(
        code = "PHOTO_QUALITY",
        titleRu = "Отчёт по фотоотчётам (контроль качества)",
        subtitleRu = "Реестр задач с фотофиксацией, количеством снимков, геометками и комментариями"
    ),
    OVERDUE_CARRYOVER(
        code = "OVERDUE_CARRYOVER",
        titleRu = "Отчёт по просрочкам и переносам",
        subtitleRu = "Задачи с автоматическим переносом, остатком объёма и числом сдвигов дедлайна"
    ),
    APPROVAL_SLA(
        code = "APPROVAL_SLA",
        titleRu = "Отчёт по проверке отчётов (SLA)",
        subtitleRu = "Отправлено на проверку, подтверждено, отклонено и оперативность проверяющих ИТР",
        isConsolidatedOnly = true
    ),
    DISCIPLINE_RATING(
        code = "DISCIPLINE_RATING",
        titleRu = "Рейтинг исполнительской дисциплины",
        subtitleRu = "Рейтинг сотрудников по соблюдению сроков и доле подтверждённых с первого раза отчётов",
        isConsolidatedOnly = true
    ),
    GROUP_SITE_SUMMARY(
        code = "GROUP_SITE_SUMMARY",
        titleRu = "Сводка по бригадам и участку",
        subtitleRu = "Сравнительная выработка по группам, загрузка персонала и процент закрытия нарядов",
        isConsolidatedOnly = true
    ),
    IDLE_ACTIVITY(
        code = "IDLE_ACTIVITY",
        titleRu = "Отчёт по простоям и активности",
        subtitleRu = "Контроль отсутствия активных закрытий и длительных задержек по исполнителям",
        isConsolidatedOnly = true
    ),
    RECURRING_EXECUTION(
        code = "RECURRING_EXECUTION",
        titleRu = "Выполнение регулярных регламентов",
        subtitleRu = "Статистика соблюдения ежедневных и еженедельных регламентных проверок"
    ),
    PERIOD_COMPARISON(
        code = "PERIOD_COMPARISON",
        titleRu = "Сравнительный отчёт за два периода",
        subtitleRu = "Динамика выполнения, просрочек и фотофиксации относительно предыдущей недели"
    ),
    INDIVIDUAL_1ON1(
        code = "INDIVIDUAL_1ON1",
        titleRu = "Индивидуальная карточка сотрудника (1:1)",
        subtitleRu = "Персональная выборка выполненных задач, объёмов, возвратов на доработку и комментариев"
    ),
    CUSTOM(
        code = "CUSTOM",
        titleRu = "Пользовательский отчёт (Конструктор)",
        subtitleRu = "Настраиваемый набор колонок, группировок и фильтров по шаблону"
    );

    fun formatTitleWithDate(dateText: String): String {
        return if (this == DAILY_REPORT) {
            "Отчет за текущий день ($dateText)"
        } else {
            titleRu
        }
    }

    companion object {
        fun fromCode(code: String): ReportType =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DAILY_REPORT
    }
}

enum class SheetLayout(val code: String, val titleRu: String, val shortRu: String) {
    SINGLE_SHEET(
        "SINGLE_SHEET",
        "На одном листе (Раздел 1: Отчёт за день + Раздел 2: План на следующий день)",
        "На 1 листе (Отчёт + План)"
    ),
    SPLIT_SHEETS(
        "SPLIT_SHEETS",
        "На раздельных листах (Лист 1: Отчёт за день / Лист 2: План на следующий день)",
        "На 2 листах (Отчёт / План)"
    );

    companion object {
        fun fromCode(code: String): SheetLayout =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SPLIT_SHEETS
    }
}

enum class ExportFormat(val code: String, val extension: String, val mimeType: String) {
    PDF("PDF", "pdf", "application/pdf"),
    XLSX("XLSX", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    DOCX("DOCX", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    CSV("CSV", "csv", "text/csv");

    companion object {
        fun fromCode(code: String): ExportFormat =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: PDF
    }
}

data class CorporateEmployee(
    val id: String,
    val fullName: String,
    val shortName: String,
    val position: String,
    val role: CorporateRole,
    val groupId: String,
    val groupName: String,
    val siteId: String,
    val siteName: String,
    val supervisorId: String?,
    val phone: String,
    val email: String,
    val personnelNumber: String = ""
)

data class WorkGroup(
    val id: String,
    val name: String,
    val siteId: String,
    val siteName: String,
    val supervisorId: String,
    val supervisorName: String,
    val specialization: String
)

/**
 * Пункт чек-листа с обязательным учётом объёма выполнения по каждому пункту (шт, кг, м² и др.).
 * Пункт считается выполненным (isDone = true) при достижении 100% планового объёма.
 */
data class SubtaskItem(
    val id: String,
    val title: String,
    val isDone: Boolean,
    val targetVolume: Double = 1.0,
    val completedVolume: Double = 0.0,
    val unit: String = "шт"
) {
    val effectiveTargetVolume: Double
        get() = if (targetVolume > 0.0) targetVolume else 1.0

    val effectiveCompletedVolume: Double
        get() = when {
            targetVolume > 0.0 -> completedVolume.coerceAtLeast(0.0)
            isDone -> 1.0
            else -> completedVolume.coerceIn(0.0, 1.0)
        }

    val effectiveUnit: String
        get() = unit.trim().ifBlank { "шт" }

    val hasVolume: Boolean
        get() = true

    val progressPercent: Int
        get() = if (targetVolume > 0.0) {
            ((completedVolume / targetVolume) * 100.0).toInt().coerceIn(0, 100)
        } else {
            if (isDone) 100 else ((effectiveCompletedVolume / effectiveTargetVolume) * 100.0).toInt().coerceIn(0, 100)
        }

    fun formatVolumeStatus(): String {
        val u = effectiveUnit
        val cStr = formatVolumeNumber(effectiveCompletedVolume)
        val tStr = formatVolumeNumber(effectiveTargetVolume)
        return "выполнено $cStr из $tStr $u ($progressPercent%)"
    }

    fun formatConcreteVolumeForReport(index: Int? = null): String {
        val prefix = if (index != null) "$index) " else ""
        val u = effectiveUnit
        val cStr = formatVolumeNumber(effectiveCompletedVolume)
        val tStr = formatVolumeNumber(effectiveTargetVolume)
        val pct = progressPercent
        return if (pct < 100) {
            val rem = (effectiveTargetVolume - effectiveCompletedVolume).coerceAtLeast(0.0)
            val remStr = formatVolumeNumber(rem)
            "$prefix$title: выполнено $cStr из $tStr $u ($pct%, ост. $remStr $u)"
        } else {
            "$prefix$title: выполнено $cStr из $tStr $u (100%)"
        }
    }

    fun encode(): String = listOf(
        id,
        if (isDone) "1" else "0",
        title.replace("|", "/"),
        effectiveTargetVolume.toString(),
        effectiveCompletedVolume.toString(),
        effectiveUnit.replace("|", "/")
    ).joinToString("|")

    companion object {
        fun formatVolumeNumber(value: Double): String {
            return if (value % 1.0 == 0.0) {
                value.toLong().toString()
            } else {
                String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
            }
        }

        /**
         * Разбирает строку вида "Монтаж лотков : 50 п.м." или "Укладка кабеля - 120 м"
         * либо использует переданный объём по умолчанию.
         */
        fun parseFromRawLine(
            rawLine: String,
            index: Int,
            defaultTargetVolume: Double = 1.0,
            defaultUnit: String = "шт"
        ): SubtaskItem {
            val trimmed = rawLine.trim()
            if (trimmed.contains("|")) {
                return decode(trimmed)
            }
            val regex = Regex("""^(.+?)\s*[:\-—]\s*(\d+(?:[.,]\d+)?)\s*([а-яА-Яa-zA-Z0-9²³./]+)?$""")
            val match = regex.matchEntire(trimmed)
            return if (match != null) {
                val parsedTitle = match.groupValues[1].trim().ifBlank { trimmed }
                val parsedVol = match.groupValues[2].replace(",", ".").toDoubleOrNull()
                    ?.takeIf { it > 0.0 } ?: defaultTargetVolume.coerceAtLeast(1.0)
                val parsedUnit = match.groupValues[3].trim().ifBlank { defaultUnit.ifBlank { "шт" } }
                SubtaskItem(
                    id = "sub_${System.currentTimeMillis()}_$index",
                    title = parsedTitle,
                    isDone = false,
                    targetVolume = parsedVol,
                    completedVolume = 0.0,
                    unit = parsedUnit
                )
            } else {
                SubtaskItem(
                    id = "sub_${System.currentTimeMillis()}_$index",
                    title = trimmed,
                    isDone = false,
                    targetVolume = defaultTargetVolume.takeIf { it > 0.0 } ?: 1.0,
                    completedVolume = 0.0,
                    unit = defaultUnit.ifBlank { "шт" }
                )
            }
        }

        fun decode(raw: String): SubtaskItem {
            val parts = raw.split("|")
            return when {
                parts.size >= 6 -> {
                    val rawTarget = parts[3].toDoubleOrNull() ?: 0.0
                    val rawCompleted = parts[4].toDoubleOrNull() ?: 0.0
                    val wasDone = parts[1] == "1"
                    val target = if (rawTarget > 0.0) rawTarget else 1.0
                    val completed = if (rawTarget > 0.0) {
                        rawCompleted.coerceAtLeast(0.0)
                    } else {
                        if (wasDone) 1.0 else 0.0
                    }
                    val doneFlag = completed >= target
                    SubtaskItem(
                        id = parts[0],
                        isDone = doneFlag,
                        title = parts[2],
                        targetVolume = target,
                        completedVolume = completed,
                        unit = parts[5].ifBlank { "шт" }
                    )
                }
                parts.size == 3 -> {
                    val doneFlag = parts[1] == "1"
                    SubtaskItem(
                        id = parts[0],
                        isDone = doneFlag,
                        title = parts[2],
                        targetVolume = 1.0,
                        completedVolume = if (doneFlag) 1.0 else 0.0,
                        unit = "шт"
                    )
                }
                else -> {
                    SubtaskItem(
                        id = "sub_${raw.hashCode()}",
                        title = raw,
                        isDone = false,
                        targetVolume = 1.0,
                        completedVolume = 0.0,
                        unit = "шт"
                    )
                }
            }
        }
    }
}

data class PhotoReportItem(
    val id: String,
    val uriString: String,
    val authorId: String,
    val authorName: String,
    val capturedAtText: String,
    val gpsCoords: String,
    val comment: String
) {
    fun encode(): String = listOf(
        id,
        uriString.replace("|", ""),
        authorId,
        authorName.replace("|", "/"),
        capturedAtText.replace("|", "/"),
        gpsCoords.replace("|", "/"),
        comment.replace("|", "/")
    ).joinToString("|")

    companion object {
        fun decode(raw: String): PhotoReportItem {
            val parts = raw.split("|", limit = 7)
            return if (parts.size == 7) {
                PhotoReportItem(
                    id = parts[0],
                    uriString = parts[1],
                    authorId = parts[2],
                    authorName = parts[3],
                    capturedAtText = parts[4],
                    gpsCoords = parts[5],
                    comment = parts[6]
                )
            } else {
                PhotoReportItem(
                    id = "ph_${raw.hashCode()}",
                    uriString = "stamp://site_photo",
                    authorId = "emp_worker_1",
                    authorName = "Сотрудник",
                    capturedAtText = "Сегодня",
                    gpsCoords = "Участок №1",
                    comment = raw
                )
            }
        }
    }
}

data class TaskCommentItem(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorRole: String,
    val text: String,
    val mentionedUser: String,
    val createdAtText: String,
    val isRead: Boolean
) {
    fun encode(): String = listOf(
        id,
        authorId,
        authorName.replace("|", "/"),
        authorRole,
        text.replace("|", "/"),
        mentionedUser.replace("|", "/"),
        createdAtText.replace("|", "/"),
        if (isRead) "1" else "0"
    ).joinToString("|")

    companion object {
        fun decode(raw: String): TaskCommentItem {
            val parts = raw.split("|", limit = 8)
            return if (parts.size == 8) {
                TaskCommentItem(
                    id = parts[0],
                    authorId = parts[1],
                    authorName = parts[2],
                    authorRole = parts[3],
                    text = parts[4],
                    mentionedUser = parts[5],
                    createdAtText = parts[6],
                    isRead = parts[7] == "1"
                )
            } else {
                TaskCommentItem(
                    id = "cmt_${raw.hashCode()}",
                    authorId = "emp_itr_1",
                    authorName = "ИТР",
                    authorRole = "ITR",
                    text = raw,
                    mentionedUser = "",
                    createdAtText = "Сегодня",
                    isRead = true
                )
            }
        }
    }
}

data class StatusHistoryEntry(
    val timestampText: String,
    val actorName: String,
    val statusCode: String,
    val note: String
) {
    fun encode(): String = listOf(
        timestampText.replace("|", "/"),
        actorName.replace("|", "/"),
        statusCode,
        note.replace("|", "/")
    ).joinToString("|")

    companion object {
        fun decode(raw: String): StatusHistoryEntry {
            val parts = raw.split("|", limit = 4)
            return if (parts.size == 4) {
                StatusHistoryEntry(
                    timestampText = parts[0],
                    actorName = parts[1],
                    statusCode = parts[2],
                    note = parts[3]
                )
            } else {
                StatusHistoryEntry(
                    timestampText = "Сегодня",
                    actorName = "Система",
                    statusCode = "NEW",
                    note = raw
                )
            }
        }
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val siteName: String = "Участок №1 — СМР и Инженерия",
    val activePersonaId: String = "emp_head_1",
    val biometricEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TaskDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val assigneeId: String = "",
    val assigneeName: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val siteId: String = "site_1",
    val priority: String = "MEDIUM",
    val status: String = "NEW",
    val dueDateEpochDay: Long = 20768L,
    val originalDueDateEpochDay: Long = 20768L,
    val deadlineTime: String = "18:00",
    val carryoverCount: Long = 0L,
    val requiresPhotoReport: Boolean = true,
    val minPhotoCount: Long = 1L,
    val requiresApproval: Boolean = true,
    val recurrenceRule: String = "NONE",
    val targetVolume: Double = 0.0,
    val completedVolume: Double = 0.0,
    val volumeUnit: String = "",
    val tags: List<String> = emptyList(),
    val subtaskItems: List<String> = emptyList(),
    val photoItems: List<String> = emptyList(),
    val commentItems: List<String> = emptyList(),
    val historyItems: List<String> = emptyList(),
    val rejectionReason: String? = null,
    val approvedByName: String? = null,
    val geoLocation: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:com.google.firebase.firestore.Exclude
    val taskStatus: TaskStatus get() = TaskStatus.fromCode(status)

    @get:com.google.firebase.firestore.Exclude
    val taskPriority: TaskPriority get() = TaskPriority.fromCode(priority)

    @get:com.google.firebase.firestore.Exclude
    val recurrence: RecurrenceRule get() = RecurrenceRule.fromCode(recurrenceRule)

    private fun parsedVolumeTag(): Triple<Double, Double, String>? {
        val raw = tags.firstOrNull { it.startsWith("__vol__|") } ?: return null
        val parts = raw.split("|")
        if (parts.size < 4) return null
        val t = parts[1].toDoubleOrNull() ?: 0.0
        val c = parts[2].toDoubleOrNull() ?: 0.0
        val u = parts[3]
        return Triple(t, c, u)
    }

    @get:com.google.firebase.firestore.Exclude
    private val hasLegacyAutoMainSubtask: Boolean
        get() = subtaskItems.any { it.startsWith("sub_vol_main|") }

    @get:com.google.firebase.firestore.Exclude
    val effectiveTargetVolume: Double
        get() {
            val subs = decodedSubtasks()
            if (hasLegacyAutoMainSubtask && subs.isNotEmpty()) {
                return subs.sumOf { it.effectiveTargetVolume }
            }
            val explicit = if (targetVolume > 0.0) targetVolume else (parsedVolumeTag()?.first ?: 0.0)
            if (explicit > 0.0) return explicit
            return if (subs.isNotEmpty()) subs.sumOf { it.effectiveTargetVolume } else 0.0
        }

    @get:com.google.firebase.firestore.Exclude
    val effectiveCompletedVolume: Double
        get() {
            val subs = decodedSubtasks()
            if (hasLegacyAutoMainSubtask && subs.isNotEmpty()) {
                return subs.sumOf { it.effectiveCompletedVolume }
            }
            val explicitTarget = if (targetVolume > 0.0) targetVolume else (parsedVolumeTag()?.first ?: 0.0)
            if (explicitTarget > 0.0) {
                return if (targetVolume > 0.0 || completedVolume > 0.0) {
                    completedVolume
                } else {
                    parsedVolumeTag()?.second ?: 0.0
                }
            }
            return if (subs.isNotEmpty()) subs.sumOf { it.effectiveCompletedVolume } else 0.0
        }

    @get:com.google.firebase.firestore.Exclude
    val effectiveVolumeUnit: String
        get() {
            val explicit = if (volumeUnit.isNotBlank()) volumeUnit else (parsedVolumeTag()?.third ?: "")
            if (explicit.isNotBlank()) return explicit
            val subs = decodedSubtasks()
            return subs.firstOrNull()?.effectiveUnit ?: "шт"
        }

    @get:com.google.firebase.firestore.Exclude
    val visibleTags: List<String>
        get() = tags.filterNot { it.startsWith("__vol__|") || it.startsWith("__asgn__|") }

    private fun parsedAssigneesTag(): Pair<List<String>, List<String>>? {
        val raw = tags.firstOrNull { it.startsWith("__asgn__|") } ?: return null
        val parts = raw.split("|")
        if (parts.size < 3) return null
        val ids = parts[1].split(";").map { it.trim() }.filter { it.isNotBlank() }
        val names = parts[2].split(";").map { it.trim() }.filter { it.isNotBlank() }
        return Pair(ids, names)
    }

    @get:com.google.firebase.firestore.Exclude
    val allAssigneeIds: List<String>
        get() {
            val fromTag = parsedAssigneesTag()?.first.orEmpty()
            val merged = (fromTag + listOfNotNull(assigneeId.takeIf { it.isNotBlank() })).distinct()
            return merged
        }

    @get:com.google.firebase.firestore.Exclude
    val allAssigneeNames: List<String>
        get() {
            val fromTag = parsedAssigneesTag()?.second.orEmpty()
            val fromField = assigneeName.split(",", ";")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            return (fromTag.ifEmpty { fromField }).distinct()
        }

    fun isAssignedToEmployee(emp: CorporateEmployee): Boolean {
        val empIdClean = emp.id.trim()
        val empCanonicalId = SiteMemberDocument.canonicalMemberId(emp.id, emp.email).trim()
        val ids = allAssigneeIds.map { it.trim() }
        if (empIdClean.isNotBlank() && ids.any { it.equals(empIdClean, ignoreCase = true) }) {
            return true
        }
        if (empCanonicalId.isNotBlank() && ids.any { it.equals(empCanonicalId, ignoreCase = true) }) {
            return true
        }
        val fullClean = emp.fullName.trim().lowercase()
        val shortClean = emp.shortName.trim().lowercase()
        val emailPrefix = emp.email.substringBefore("@").trim().lowercase()
        val names = allAssigneeNames.map { it.trim().lowercase() }
        val rawAssigneeLower = assigneeName.trim().lowercase()

        if (fullClean.isNotBlank() && (names.any { it == fullClean || it.contains(fullClean) || fullClean.contains(it) } || rawAssigneeLower.contains(fullClean))) {
            return true
        }
        if (shortClean.isNotBlank() && shortClean != "сотрудник" && (names.any { it == shortClean || it.contains(shortClean) } || rawAssigneeLower.contains(shortClean))) {
            return true
        }
        if (emailPrefix.length >= 3 && !emp.email.endsWith("@site1.ru") && names.any { it.replace(" ", "").contains(emailPrefix) }) {
            return true
        }
        return false
    }

    fun withUpdatedAssignees(ids: List<String>, names: List<String>): TaskDocument {
        val cleanIds = ids.map { it.trim().replace("|", "").replace(";", "") }.filter { it.isNotBlank() }.distinct()
        val cleanNames = names.map { it.trim().replace("|", "/").replace(";", ",") }.filter { it.isNotBlank() }.distinct()
        val primaryId = (cleanIds.firstOrNull() ?: assigneeId.ifBlank { "emp_worker_1" }).take(64)
        val joinedNames = cleanNames.joinToString(", ").ifBlank { assigneeName.ifBlank { "Исполнитель" } }.take(120)
        val nonAssigneeTags = tags.filterNot { it.startsWith("__asgn__|") }
        val asgnTag = "__asgn__|${cleanIds.joinToString(";")}|${cleanNames.joinToString(";")}".take(480)
        val volTag = nonAssigneeTags.filter { it.startsWith("__vol__|") }
        val userTags = nonAssigneeTags.filterNot { it.startsWith("__vol__|") }
        val updatedTags = (volTag + listOf(asgnTag) + userTags).take(10)
        return copy(
            assigneeId = primaryId,
            assigneeName = joinedNames,
            tags = updatedTags
        )
    }

    fun withUpdatedVolume(newTarget: Double, newCompleted: Double, newUnit: String): TaskDocument {
        val nonVolTags = tags.filterNot { it.startsWith("__vol__|") }
        val updatedTags = if (newTarget > 0.0) {
            listOf("__vol__|$newTarget|$newCompleted|${newUnit.replace("|", "/")}") + nonVolTags
        } else {
            nonVolTags
        }
        return copy(
            targetVolume = newTarget,
            completedVolume = newCompleted,
            volumeUnit = newUnit,
            tags = updatedTags.take(10)
        )
    }

    /**
     * Возвращает список пунктов чек-листа без служебного дубликата наименования задачи (sub_vol_main).
     */
    fun decodedSubtasks(): List<SubtaskItem> {
        val all = subtaskItems.map { SubtaskItem.decode(it) }
        val cleanTaskTitle = title.trim()
        return all.filterNot { item ->
            item.id == "sub_vol_main" ||
                (all.size > 1 && cleanTaskTitle.isNotBlank() && item.title.trim().equals(cleanTaskTitle, ignoreCase = true) && item.id.startsWith("sub_vol"))
        }
    }

    fun decodedPhotos(): List<PhotoReportItem> = photoItems.map { PhotoReportItem.decode(it) }
    fun decodedComments(): List<TaskCommentItem> = commentItems.map { TaskCommentItem.decode(it) }
    fun decodedHistory(): List<StatusHistoryEntry> = historyItems.map { StatusHistoryEntry.decode(it) }

    fun hasEnoughPhotos(): Boolean =
        !requiresPhotoReport || photoItems.size >= minPhotoCount.toInt()

    fun hasVolumeTracking(): Boolean =
        effectiveTargetVolume > 0.0 || subtaskItems.isNotEmpty()

    /**
     * Общий процент выполнения задачи (0..100%).
     * Задача считается выполненной при 100%.
     */
    fun completionPercent(): Int {
        val subs = decodedSubtasks()
        val tVol = effectiveTargetVolume
        val cVol = effectiveCompletedVolume
        return when {
            subs.isNotEmpty() -> {
                val distinctUnits = subs.map { it.effectiveUnit.lowercase() }.distinct()
                if (distinctUnits.size == 1) {
                    val totalSubTarget = subs.sumOf { it.effectiveTargetVolume }
                    val totalSubCompleted = subs.sumOf { it.effectiveCompletedVolume }
                    if (totalSubTarget > 0.0) {
                        ((totalSubCompleted / totalSubTarget) * 100.0).toInt().coerceIn(0, 100)
                    } else {
                        (subs.sumOf { it.progressPercent } / subs.size).coerceIn(0, 100)
                    }
                } else {
                    (subs.sumOf { it.progressPercent } / subs.size).coerceIn(0, 100)
                }
            }
            tVol > 0.0 -> {
                ((cVol / tVol) * 100.0).toInt().coerceIn(0, 100)
            }
            taskStatus == TaskStatus.APPROVED || taskStatus == TaskStatus.ON_REVIEW -> 100
            else -> 0
        }
    }

    /**
     * Проверка, выполнен ли объём на 100% по всем пунктам чек-листа и по задаче.
     */
    fun isVolumeCompleted100Percent(): Boolean {
        val subs = decodedSubtasks()
        val tVol = effectiveTargetVolume
        val cVol = effectiveCompletedVolume
        val mainVolOk = if (tVol > 0.0) cVol >= tVol else true
        val subsOk = if (subs.isNotEmpty()) subs.all { it.progressPercent >= 100 } else true
        return mainVolOk && subsOk
    }

    /**
     * Детализация конкретного выполненного объёма по каждому пункту чек-листа для отчётов.
     */
    fun formatChecklistBreakdownForReport(): List<String> {
        val subs = decodedSubtasks()
        return subs.mapIndexed { idx, sub ->
            sub.formatConcreteVolumeForReport(index = idx + 1)
        }
    }

    /**
     * Текстовое представление конкретного выполненного объёма для карточек и отчётных документов.
     * Если работа выполнена не до конца (< 100%), явно указывает выполненный объём, остаток и расшифровку по пунктам чек-листа.
     */
    fun formatVolumeSummaryForReport(): String {
        val subs = decodedSubtasks()
        val tVol = effectiveTargetVolume
        val cVol = effectiveCompletedVolume
        val uLabel = effectiveVolumeUnit.ifBlank { "шт" }

        val perItemDetails = if (subs.isNotEmpty()) {
            subs.mapIndexed { idx, s ->
                val cItem = SubtaskItem.formatVolumeNumber(s.effectiveCompletedVolume)
                val tItem = SubtaskItem.formatVolumeNumber(s.effectiveTargetVolume)
                "${idx + 1}) ${s.title}: $cItem из $tItem ${s.effectiveUnit} (${s.progressPercent}%)"
            }.joinToString("; ")
        } else {
            ""
        }

        if (tVol > 0.0) {
            val pct = completionPercent()
            val factStr = SubtaskItem.formatVolumeNumber(cVol)
            val planStr = SubtaskItem.formatVolumeNumber(tVol)
            val mainSummary = if (pct < 100) {
                val rem = (tVol - cVol).coerceAtLeast(0.0)
                val remStr = SubtaskItem.formatVolumeNumber(rem)
                "Не до конца: $factStr из $planStr $uLabel ($pct%, ост. $remStr $uLabel)"
            } else {
                "Выполнено: $factStr из $planStr $uLabel (100%)"
            }
            return if (subs.size > 1 || (subs.size == 1 && !subs.first().title.equals(title, ignoreCase = true))) {
                "$mainSummary [Чек-лист: $perItemDetails]"
            } else {
                mainSummary
            }
        }

        if (subs.isNotEmpty()) {
            val pct = completionPercent()
            return if (pct < 100) {
                "Не до конца ($pct%): $perItemDetails"
            } else {
                "Выполнено (100%): $perItemDetails"
            }
        }

        return if (taskStatus == TaskStatus.APPROVED) {
            "Выполнено (100%)"
        } else {
            "Без объёма (${taskStatus.titleRu})"
        }
    }

    /**
     * Проверка принадлежности задачи к тематическому подразделению (бригаде / службе).
     */
    fun belongsToSubdivision(targetGroupName: String, targetGroupId: String): Boolean {
        if (targetGroupId.isNotBlank() && groupId.isNotBlank() && groupId.equals(targetGroupId, ignoreCase = true)) {
            return true
        }
        if (targetGroupName.isNotBlank() && groupName.isNotBlank() && groupName.equals(targetGroupName, ignoreCase = true)) {
            return true
        }
        return false
    }

    /**
     * Проверка принадлежности задачи к строительному участку / объекту.
     */
    fun belongsToSite(targetSiteName: String, targetSiteId: String): Boolean {
        if (targetSiteId.isNotBlank() && siteId.isNotBlank() && siteId.equals(targetSiteId, ignoreCase = true)) {
            return true
        }
        val loc = geoLocation.orEmpty().trim()
        if (targetSiteName.isNotBlank()) {
            if (loc.isNotBlank() && (loc.contains(targetSiteName, ignoreCase = true) || targetSiteName.contains(loc, ignoreCase = true))) {
                return true
            }
        }
        return false
    }

    /**
     * Проверка, относится ли задача к сфере ответственности ИТР (подразделение, участок, исполнитель или создатель).
     */
    fun isRelevantToItr(
        itr: CorporateEmployee,
        allEmployees: List<CorporateEmployee> = emptyList()
    ): Boolean {
        // Лично назначенная или созданная задача относится всегда
        if (isAssignedToEmployee(itr) || creatorId == itr.id) {
            return true
        }
        if (belongsToSubdivision(itr.groupName, itr.groupId)) {
            return true
        }
        if (belongsToSite(itr.siteName, itr.siteId)) {
            return true
        }
        if (allEmployees.isNotEmpty()) {
            val relatedEmps = allEmployees.filter {
                (it.groupId.isNotBlank() && it.groupId == itr.groupId) ||
                    (it.groupName.isNotBlank() && it.groupName.equals(itr.groupName, ignoreCase = true)) ||
                    (it.siteId.isNotBlank() && it.siteId == itr.siteId)
            }
            if (relatedEmps.any { isAssignedToEmployee(it) }) {
                return true
            }
        }
        return false
    }
}

@IgnoreExtraProperties
data class ReportTemplateDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val reportType: String = "DAILY_REPORT",
    val sheetLayout: String = "SPLIT_SHEETS",
    val exportFormat: String = "PDF",
    val orgHeader: String = "ООО «ГлавСтройИнжиниринг» • Участок №1 (СМР)",
    val defaultPeriodDays: Long = 1L,
    val columns: List<String> = listOf("ФИО исполнителя", "Бригада", "Задача", "Объём выполнения", "Статус", "Срок", "Фотоотчёт", "Переносы"),
    val groupByField: String? = "ASSIGNEE",
    val includeSignatures: Boolean? = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedReportType: ReportType get() = ReportType.fromCode(reportType)
    val parsedSheetLayout: SheetLayout get() = SheetLayout.fromCode(sheetLayout)
    val parsedExportFormat: ExportFormat get() = ExportFormat.fromCode(exportFormat)
}

@IgnoreExtraProperties
data class GeneratedReportDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val templateId: String = "",
    val templateName: String = "",
    val reportType: String = "DAILY_REPORT",
    val authorName: String = "",
    val periodLabel: String = "",
    val sheetLayout: String = "SPLIT_SHEETS",
    val exportFormat: String = "PDF",
    val filePath: String = "",
    val summaryText: String = "",
    val recipients: List<String> = emptyList(),
    val filterSummary: String? = null,
    val rowCount: Long? = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLogDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorRole: String = "HEAD",
    val actionType: String = "",
    val targetType: String = "TASK",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class TaskDraft(
    val title: String,
    val description: String,
    val assigneeId: String,
    val assigneeIds: List<String> = emptyList(),
    val customAssigneeName: String = "",
    val priority: TaskPriority,
    val dueDateEpochDay: Long,
    val deadlineTime: String,
    val requiresPhotoReport: Boolean,
    val minPhotoCount: Int,
    val requiresApproval: Boolean,
    val recurrenceRule: RecurrenceRule,
    val targetVolume: Double = 0.0,
    val volumeUnit: String = "",
    val tags: List<String>,
    val subtasks: List<String>,
    val geoLocation: String
)

data class EmployeeDisciplineStat(
    val employee: CorporateEmployee,
    val assigned: Int,
    val approved: Int,
    val onReview: Int,
    val rejected: Int,
    val overdue: Int,
    val carryovers: Int,
    val photoCompliancePercent: Int,
    val disciplineScore: Int
)

data class GroupStatBreakdown(
    val groupId: String,
    val groupName: String,
    val totalTasks: Int,
    val approvedTasks: Int,
    val onReviewTasks: Int,
    val overdueTasks: Int,
    val completionPercent: Int
)

data class StatisticsSummary(
    val roleScopeLabel: String,
    val totalAssigned: Int,
    val inProgressCount: Int,
    val onReviewCount: Int,
    val approvedCount: Int,
    val rejectedCount: Int,
    val overdueCount: Int,
    val totalCarryovers: Int,
    val tasksWithPhotosCount: Int,
    val photoCompliancePercent: Int,
    val avgCompletionHours: Double,
    val employeeStats: List<EmployeeDisciplineStat>,
    val groupStats: List<GroupStatBreakdown>
)

enum class MemberApprovalStatus(val code: String, val titleRu: String) {
    PENDING("PENDING", "Ожидает назначения роли"),
    APPROVED("APPROVED", "Доступ открыт"),
    REJECTED("REJECTED", "Доступ отклонён");

    companion object {
        fun fromCode(code: String): MemberApprovalStatus =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: PENDING
    }
}

@IgnoreExtraProperties
data class SiteMemberDocument(
    @DocumentId val id: String = "",
    val memberId: String = "",
    val email: String = "",
    val fullName: String = "",
    val position: String = "Специалист участка",
    val role: String = "PENDING",
    val approvalStatus: String = "PENDING",
    val groupId: String = "grp_monolith",
    val groupName: String = "Бригада №1 • Монолит и ЖБК",
    val approvedByName: String? = null,
    @get:com.google.firebase.firestore.Exclude val personnelNumber: String = "",
    @get:com.google.firebase.firestore.Exclude val siteName: String = "",
    @get:com.google.firebase.firestore.Exclude val phone: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:com.google.firebase.firestore.Exclude
    val parsedApprovalStatus: MemberApprovalStatus
        get() = MemberApprovalStatus.fromCode(approvalStatus)

    @get:com.google.firebase.firestore.Exclude
    val isPendingRole: Boolean
        get() = approvalStatus.equals(MemberApprovalStatus.PENDING.code, ignoreCase = true) ||
            role.equals("PENDING", ignoreCase = true) ||
            approvalStatus.isBlank()

    @get:com.google.firebase.firestore.Exclude
    val isRejected: Boolean
        get() = approvalStatus.equals(MemberApprovalStatus.REJECTED.code, ignoreCase = true)

    @get:com.google.firebase.firestore.Exclude
    val parsedRoleOrNull: CorporateRole?
        get() = when {
            role.equals("PENDING", ignoreCase = true) || isPendingRole -> null
            extractMetaValue("adm") == "1" || isRootAdminEmail(email) -> CorporateRole.ADMIN
            else -> CorporateRole.fromCode(role)
        }

    private fun extractMetaValue(prefix: String): String {
        val raw = approvedByName ?: return ""
        val parts = raw.split("||")
        return parts.firstOrNull { it.startsWith("$prefix:") }
            ?.substringAfter("$prefix:")
            ?.trim()
            .orEmpty()
    }

    @get:com.google.firebase.firestore.Exclude
    val cleanApprovedByName: String
        get() = (approvedByName ?: "").substringBefore("||").trim()

    @get:com.google.firebase.firestore.Exclude
    val effectivePersonnelNumber: String
        get() = personnelNumber.ifBlank { extractMetaValue("tab") }

    @get:com.google.firebase.firestore.Exclude
    val effectiveSiteName: String
        get() = siteName.ifBlank { extractMetaValue("site").ifBlank { "Участок №1" } }

    @get:com.google.firebase.firestore.Exclude
    val effectivePhone: String
        get() = phone.ifBlank { extractMetaValue("tel") }

    fun encodeApprovedByForFirestore(
        approverName: String = cleanApprovedByName.ifBlank { "Администратор" },
        explicitPersonnelNumber: String? = null,
        explicitSiteName: String? = null,
        explicitPhone: String? = null,
        isAdminRole: Boolean = (role == CorporateRole.ADMIN.code || extractMetaValue("adm") == "1")
    ): String {
        val base = approverName.substringBefore("||").replace("|", "/").trim().take(24)
        val tabPart = (explicitPersonnelNumber ?: effectivePersonnelNumber).replace("|", "/").trim().take(14)
        val sitePart = (explicitSiteName ?: effectiveSiteName).replace("|", "/").trim().take(26)
        val telPart = (explicitPhone ?: effectivePhone).replace("|", "/").trim().take(14)
        return buildString {
            append(base.ifBlank { "Администратор" })
            if (tabPart.isNotBlank()) append("||tab:$tabPart")
            if (sitePart.isNotBlank()) append("||site:$sitePart")
            if (telPart.isNotBlank()) append("||tel:$telPart")
            if (isAdminRole) append("||adm:1")
        }.take(100)
    }

    fun toCorporateEmployee(defaultSiteName: String = "Участок №1"): CorporateEmployee {
        val resolvedRole = parsedRoleOrNull ?: CorporateRole.WORKER
        val parts = fullName.trim().split(" ").filter { it.isNotBlank() }
        val shortName = when {
            parts.size >= 3 -> "${parts[0]} ${parts[1].first()}.${parts[2].first()}."
            parts.size == 2 -> "${parts[0]} ${parts[1].first()}."
            parts.size == 1 -> parts[0]
            else -> "Сотрудник"
        }
        val resolvedSite = effectiveSiteName.ifBlank { defaultSiteName }
        return CorporateEmployee(
            id = memberId.ifBlank { id },
            fullName = fullName.ifBlank { "Сотрудник участка" },
            shortName = shortName,
            position = position.ifBlank { resolvedRole.titleRu },
            role = resolvedRole,
            groupId = groupId.ifBlank { "grp_monolith" },
            groupName = groupName.ifBlank { "Основное подразделение" },
            siteId = "site_1",
            siteName = resolvedSite,
            supervisorId = if (resolvedRole.isAdminOrHead) null else "emp_head_1",
            phone = effectivePhone,
            email = email,
            personnelNumber = effectivePersonnelNumber
        )
    }

    companion object {
        // SHA-256 хеш учётной записи администратора (без хранения паролей и учётных данных в открытом виде)
        private const val ROOT_ADMIN_SHA256 = "cc6a70cd225a870b4731bbd77b3979cdc7f878ca1bb8c09a91d3987e97a3f1b6"

        private fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun isRootAdminEmail(email: String?): Boolean {
            if (email.isNullOrBlank()) return false
            val normalized = email.trim().lowercase()
            return sha256Hex(normalized) == ROOT_ADMIN_SHA256
        }

        fun isRootHeadEmail(email: String?): Boolean = isRootAdminEmail(email)

        fun canonicalMemberId(rawUserId: String, email: String?): String {
            val cleanEmail = email?.trim()?.lowercase().orEmpty()
            val trimmedId = rawUserId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
            // Keep pre-registered employee IDs (emp_...) intact if they have placeholder @site1.ru emails
            if (trimmedId.startsWith("emp_") && (cleanEmail.isBlank() || cleanEmail.endsWith("@site1.ru"))) {
                return trimmedId
            }
            if (cleanEmail.isNotBlank() && !cleanEmail.endsWith("@site1.ru")) {
                return "usr_${cleanEmail.replace(Regex("[^a-zA-Z0-9]"), "_").take(40)}"
            }
            return trimmedId.ifBlank { "usr_${System.currentTimeMillis()}" }
        }
    }
}
