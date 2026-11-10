package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

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
        badgeDescription = "Личные задачи, фотоотчёты, статистика группы"
    ),
    ITR(
        code = "ITR",
        titleRu = "ИТР (Прораб / Инженер)",
        hierarchyLevel = 2,
        badgeDescription = "Постановка задач рабочим, приёмка фотоотчётов, сводный отчёт по бригаде"
    ),
    HEAD(
        code = "HEAD",
        titleRu = "Руководитель участка",
        hierarchyLevel = 3,
        badgeDescription = "Полный доступ по участку, контроль ИТР и рабочих, аудит и шаблоны"
    );

    fun canAssignTo(targetRole: CorporateRole): Boolean {
        return when (this) {
            HEAD -> true
            ITR -> targetRole == ITR || targetRole == WORKER
            WORKER -> targetRole == WORKER
        }
    }

    fun canAssignToOtherEmployees(): Boolean = this == HEAD || this == ITR

    fun canApproveReportOf(assigneeRole: CorporateRole): Boolean {
        return when (this) {
            HEAD -> true
            ITR -> assigneeRole == WORKER
            WORKER -> false
        }
    }

    fun canExportConsolidatedPersonnelReport(): Boolean = this == HEAD || this == ITR

    fun canManageTemplatesAndGroups(): Boolean = this == HEAD

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
        subtitleRu = "Список запланированных работ с ФИО исполнителей, сроками, приоритетом и требованием фотоотчёта"
    ),
    DAILY_REPORT(
        code = "DAILY_REPORT",
        titleRu = "Отчёт за текущий / выбранный день",
        subtitleRu = "Выполненные, подтверждённые, отклонённые, просроченные и перенесённые задачи с ФИО и числом фото"
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
        subtitleRu = "Задачи с автоматическим переносом, исходной датой и числом сдвигов дедлайна"
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
        subtitleRu = "Персональная выборка выполненных задач, возвратов на доработку и комментариев"
    ),
    CUSTOM(
        code = "CUSTOM",
        titleRu = "Пользовательский отчёт (Конструктор)",
        subtitleRu = "Настраиваемый набор колонок, группировок и фильтров по шаблону"
    );

    companion object {
        fun fromCode(code: String): ReportType =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DAILY_REPORT
    }
}

enum class SheetLayout(val code: String, val titleRu: String, val shortRu: String) {
    SINGLE_SHEET("SINGLE_SHEET", "На одном листе (План + Отчёт вместе)", "Единый лист"),
    SPLIT_SHEETS("SPLIT_SHEETS", "На раздельных листах (Лист 1: План / Лист 2: Отчёт)", "Раздельные листы");

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
    val email: String
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

data class SubtaskItem(
    val id: String,
    val title: String,
    val isDone: Boolean
) {
    fun encode(): String = "$id|${if (isDone) "1" else "0"}|${title.replace("|", "/")}"

    companion object {
        fun decode(raw: String): SubtaskItem {
            val parts = raw.split("|", limit = 3)
            return if (parts.size == 3) {
                SubtaskItem(
                    id = parts[0],
                    isDone = parts[1] == "1",
                    title = parts[2]
                )
            } else {
                SubtaskItem(id = "sub_${raw.hashCode()}", title = raw, isDone = false)
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
                    gpsCoords = "55.7558, 37.6173",
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
    val taskStatus: TaskStatus get() = TaskStatus.fromCode(status)
    val taskPriority: TaskPriority get() = TaskPriority.fromCode(priority)
    val recurrence: RecurrenceRule get() = RecurrenceRule.fromCode(recurrenceRule)

    fun decodedSubtasks(): List<SubtaskItem> = subtaskItems.map { SubtaskItem.decode(it) }
    fun decodedPhotos(): List<PhotoReportItem> = photoItems.map { PhotoReportItem.decode(it) }
    fun decodedComments(): List<TaskCommentItem> = commentItems.map { TaskCommentItem.decode(it) }
    fun decodedHistory(): List<StatusHistoryEntry> = historyItems.map { StatusHistoryEntry.decode(it) }

    fun hasEnoughPhotos(): Boolean =
        !requiresPhotoReport || photoItems.size >= minPhotoCount.toInt()
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
    val columns: List<String> = listOf("ФИО исполнителя", "Бригада", "Задача", "Статус", "Срок", "Фотоотчёт", "Переносы"),
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
    val priority: TaskPriority,
    val dueDateEpochDay: Long,
    val deadlineTime: String,
    val requiresPhotoReport: Boolean,
    val minPhotoCount: Int,
    val requiresApproval: Boolean,
    val recurrenceRule: RecurrenceRule,
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedApprovalStatus: MemberApprovalStatus
        get() = MemberApprovalStatus.fromCode(approvalStatus)

    val isPendingRole: Boolean
        get() = approvalStatus == MemberApprovalStatus.PENDING.code || role == "PENDING"

    val isRejected: Boolean
        get() = approvalStatus == MemberApprovalStatus.REJECTED.code

    val parsedRoleOrNull: CorporateRole?
        get() = if (role == "PENDING" || isPendingRole) null else CorporateRole.fromCode(role)

    fun toCorporateEmployee(): CorporateEmployee {
        val resolvedRole = parsedRoleOrNull ?: CorporateRole.WORKER
        val parts = fullName.trim().split(" ").filter { it.isNotBlank() }
        val shortName = when {
            parts.size >= 3 -> "${parts[0]} ${parts[1].first()}.${parts[2].first()}."
            parts.size == 2 -> "${parts[0]} ${parts[1].first()}."
            parts.size == 1 -> parts[0]
            else -> "Сотрудник"
        }
        return CorporateEmployee(
            id = memberId.ifBlank { id },
            fullName = fullName.ifBlank { "Сотрудник участка" },
            shortName = shortName,
            position = position.ifBlank { resolvedRole.titleRu },
            role = resolvedRole,
            groupId = groupId.ifBlank { "grp_monolith" },
            groupName = groupName.ifBlank { "Бригада №1 • Монолит и ЖБК" },
            siteId = "site_1",
            siteName = "Участок №1 — ПГС и Инженерные сети",
            supervisorId = if (resolvedRole == CorporateRole.HEAD) null else "emp_head_1",
            phone = "+7 (916) 440-00-00",
            email = email
        )
    }

    companion object {
        const val DEFAULT_HEAD_ACTIVATION_CODE = "1001"

        private val rootHeadEmails = setOf(
            "m241185m@gmail.com",
            "kovalev.ds@glavstroy.ru"
        )

        fun isRootHeadEmail(email: String?): Boolean {
            if (email.isNullOrBlank()) return false
            return rootHeadEmails.contains(email.trim().lowercase())
        }
    }
}

