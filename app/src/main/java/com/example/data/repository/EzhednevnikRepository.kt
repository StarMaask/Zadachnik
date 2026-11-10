package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AuditLogDocument
import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.EmployeeDisciplineStat
import com.example.data.model.GeneratedReportDocument
import com.example.data.model.GroupStatBreakdown
import com.example.data.model.PhotoReportItem
import com.example.data.model.RecurrenceRule
import com.example.data.model.ReportTemplateDocument
import com.example.data.model.StatisticsSummary
import com.example.data.model.StatusHistoryEntry
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskCommentItem
import com.example.data.model.TaskDocument
import com.example.data.model.TaskDraft
import com.example.data.model.TaskStatus
import com.example.data.model.UserProfile
import com.example.data.seed.CorporateSeedData
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class EzhednevnikRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun nowTimeText(): String =
        SimpleDateFormat("dd.MM HH:mm", Locale.forLanguageTag("ru")).format(Date())

    // --- REALTIME OBSERVATION FLOWS ---

    fun observeUserProfile(userId: String = requireUserId()): Flow<UserProfile?> = flow {
        val uid = if (userId.isNotBlank()) userId else requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeTasks(userId: String = requireUserId()): Flow<List<TaskDocument>> = flow {
        val uid = if (userId.isNotBlank()) userId else requireUserId()
        val path = "tasks"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { querySnapshot ->
                    querySnapshot.toObjects(
                        TaskDocument::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<TaskDocument> { it.taskStatus.isTerminalOrDone }
                            .thenBy { it.dueDateEpochDay }
                            .thenByDescending { it.taskPriority.weight }
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeReportTemplates(userId: String = requireUserId()): Flow<List<ReportTemplateDocument>> = flow {
        val uid = if (userId.isNotBlank()) userId else requireUserId()
        val path = "report_templates"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { querySnapshot ->
                    querySnapshot.toObjects(
                        ReportTemplateDocument::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeGeneratedReports(userId: String = requireUserId()): Flow<List<GeneratedReportDocument>> = flow {
        val uid = if (userId.isNotBlank()) userId else requireUserId()
        val path = "generated_reports"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { querySnapshot ->
                    querySnapshot.toObjects(
                        GeneratedReportDocument::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAuditLogs(userId: String = requireUserId()): Flow<List<AuditLogDocument>> = flow {
        val uid = if (userId.isNotBlank()) userId else requireUserId()
        val path = "audit_logs"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { querySnapshot ->
                    querySnapshot.toObjects(
                        AuditLogDocument::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    // --- SEEDING & WORKSPACE INITIALIZATION ---

    suspend fun ensureWorkspaceSeeded(displayName: String = "Руководитель участка"): Result<Unit> = runCatching {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val userSnap = try {
            userRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, userRef.path)
            throw e
        }

        if (!userSnap.exists()) {
            val profilePayload = mapOf(
                "userId" to uid,
                "displayName" to displayName.ifBlank { "Ковалёв Д.С." }.take(100),
                "siteName" to CorporateSeedData.DEFAULT_SITE_NAME,
                "activePersonaId" to "emp_head_1",
                "biometricEnabled" to true,
                "notificationsEnabled" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                userRef.set(profilePayload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, userRef.path)
                throw e
            }
        }

        val existingTasks = try {
            db.collection("tasks").whereEqualTo("userId", uid).limit(1).get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "tasks")
            throw e
        }

        if (existingTasks.isEmpty) {
            val batch = db.batch()
            CorporateSeedData.buildInitialTasks(uid).forEach { seedTask ->
                val docRef = db.collection("tasks").document("${uid}_${seedTask.id}")
                batch.set(docRef, taskToCreatePayload(uid, seedTask))
            }
            CorporateSeedData.buildInitialReportTemplates(uid).forEach { tpl ->
                val docRef = db.collection("report_templates").document("${uid}_${tpl.id}")
                batch.set(docRef, templateToCreatePayload(uid, tpl))
            }
            CorporateSeedData.buildInitialAuditLogs(uid).forEach { log ->
                val docRef = db.collection("audit_logs").document("${uid}_${log.id}")
                batch.set(
                    docRef,
                    mapOf(
                        "userId" to uid,
                        "actorId" to log.actorId,
                        "actorName" to log.actorName,
                        "actorRole" to log.actorRole,
                        "actionType" to log.actionType,
                        "targetType" to log.targetType,
                        "targetId" to log.targetId,
                        "details" to log.details,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            try {
                batch.commit().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "batch_seed")
                throw e
            }
        }
    }

    suspend fun switchActivePersona(
        personaId: String,
        biometricEnabled: Boolean? = null,
        notificationsEnabled: Boolean? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val employee = CorporateSeedData.findEmployee(personaId)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            val createPayload = mapOf(
                "userId" to uid,
                "displayName" to employee.fullName,
                "siteName" to employee.siteName,
                "activePersonaId" to employee.id,
                "biometricEnabled" to (biometricEnabled ?: true),
                "notificationsEnabled" to (notificationsEnabled ?: true),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(createPayload).await()
        } else {
            val updates = mutableMapOf<String, Any>(
                "displayName" to employee.fullName,
                "activePersonaId" to employee.id,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (biometricEnabled != null) updates["biometricEnabled"] = biometricEnabled
            if (notificationsEnabled != null) updates["notificationsEnabled"] = notificationsEnabled
            docRef.update(updates).await()
        }
    }

    // --- TASK CRUD & RBAC OPERATIONS ---

    suspend fun createTask(
        actor: CorporateEmployee,
        draft: TaskDraft
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val assignee = CorporateSeedData.findEmployee(draft.assigneeId)

        // RBAC Enforcement: Check hierarchy rights
        if (actor.id != assignee.id && !actor.role.canAssignTo(assignee.role)) {
            throw IllegalArgumentException(
                "Роль «${actor.role.titleRu}» не может назначать задачи сотруднику с ролью «${assignee.role.titleRu}»"
            )
        }
        if (actor.role == CorporateRole.WORKER && actor.id != assignee.id) {
            throw IllegalArgumentException("Рабочий может создавать только личные задачи для себя")
        }

        val docRef = db.collection("tasks").document("task_${UUID.randomUUID().toString().replace("-", "").take(16)}")
        val encodedSubtasks = draft.subtasks.mapIndexed { idx, title ->
            SubtaskItem("sub_${idx + 1}", title, false).encode()
        }
        val initialHistory = listOf(
            StatusHistoryEntry(
                timestampText = nowTimeText(),
                actorName = actor.shortName,
                statusCode = TaskStatus.NEW.code,
                note = "Задача поставлена исполнителю ${assignee.shortName}"
            ).encode()
        )

        val taskModel = TaskDocument(
            userId = uid,
            title = draft.title.trim().take(200),
            description = draft.description.trim().take(2000),
            creatorId = actor.id,
            creatorName = actor.fullName,
            assigneeId = assignee.id,
            assigneeName = assignee.fullName,
            groupId = assignee.groupId,
            groupName = assignee.groupName,
            siteId = assignee.siteId,
            priority = draft.priority.code,
            status = TaskStatus.NEW.code,
            dueDateEpochDay = draft.dueDateEpochDay,
            originalDueDateEpochDay = draft.dueDateEpochDay,
            deadlineTime = draft.deadlineTime.ifBlank { "18:00" }.take(16),
            carryoverCount = 0L,
            requiresPhotoReport = draft.requiresPhotoReport,
            minPhotoCount = if (draft.requiresPhotoReport) draft.minPhotoCount.coerceAtLeast(1).toLong() else 0L,
            requiresApproval = draft.requiresApproval,
            recurrenceRule = draft.recurrenceRule.code,
            tags = draft.tags.take(10),
            subtaskItems = encodedSubtasks.take(25),
            photoItems = emptyList(),
            commentItems = emptyList(),
            historyItems = initialHistory,
            geoLocation = draft.geoLocation.ifBlank { assignee.siteName }.take(120)
        )

        try {
            docRef.set(taskToCreatePayload(uid, taskModel)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }

        if (actor.id != assignee.id || actor.role != CorporateRole.WORKER) {
            appendAuditLog(
                uid = uid,
                actor = actor,
                actionType = "CREATE_TASK",
                targetType = "TASK",
                targetId = docRef.id,
                details = "Поставлена задача «${taskModel.title}» исполнителю ${assignee.shortName}"
            )
        }
        docRef.id
    }

    suspend fun getTaskById(taskId: String): Result<TaskDocument> = runCatching {
        val docRef = db.collection("tasks").document(taskId)
        val snapshot = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
        snapshot.toObject(TaskDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: throw NoSuchElementException("Задача не найдена")
    }

    suspend fun getUserTasks(): Result<List<TaskDocument>> = runCatching {
        val uid = requireUserId()
        val path = "tasks"
        val snapshot = try {
            db.collection(path).whereEqualTo("userId", uid).get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
        snapshot.toObjects(TaskDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

    suspend fun addPhotoToTask(
        actor: CorporateEmployee,
        task: TaskDocument,
        uriString: String,
        gpsCoords: String,
        comment: String
    ): Result<Unit> = runCatching {
        require(task.id.isNotBlank()) { "Task ID must not be blank" }
        if (task.taskStatus == TaskStatus.APPROVED || task.taskStatus == TaskStatus.ARCHIVED) {
            throw IllegalStateException("Фотоотчёт можно дополнять только до подтверждения задачи")
        }

        val newPhoto = PhotoReportItem(
            id = "ph_${System.currentTimeMillis()}",
            uriString = uriString.take(250),
            authorId = actor.id,
            authorName = actor.shortName,
            capturedAtText = nowTimeText(),
            gpsCoords = gpsCoords.ifBlank { "55.7558° N, 37.6173° E" }.take(60),
            comment = comment.ifBlank { "Фотофиксация выполнения на объекте" }.take(180)
        )
        val updatedPhotos = (task.photoItems + newPhoto.encode()).takeLast(30)
        val newStatus = if (task.taskStatus == TaskStatus.NEW) TaskStatus.IN_PROGRESS.code else task.status
        val newHistory = (task.historyItems + StatusHistoryEntry(
            timestampText = nowTimeText(),
            actorName = actor.shortName,
            statusCode = newStatus,
            note = "Добавлено фото в отчёт (всего фото: ${updatedPhotos.size})"
        ).encode()).takeLast(50)

        val docRef = db.collection("tasks").document(task.id)
        try {
            docRef.update(
                mapOf(
                    "photoItems" to updatedPhotos,
                    "status" to newStatus,
                    "historyItems" to newHistory,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleSubtask(
        actor: CorporateEmployee,
        task: TaskDocument,
        subtaskId: String
    ): Result<Unit> = runCatching {
        require(task.id.isNotBlank()) { "Task ID must not be blank" }
        if (task.taskStatus == TaskStatus.ARCHIVED) {
            throw IllegalStateException("Архивная задача недоступна для редактирования")
        }
        val updatedSubtasks = task.decodedSubtasks().map { item ->
            if (item.id == subtaskId) item.copy(isDone = !item.isDone) else item
        }.map { it.encode() }

        val docRef = db.collection("tasks").document(task.id)
        try {
            docRef.update(
                mapOf(
                    "subtaskItems" to updatedSubtasks,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun addComment(
        actor: CorporateEmployee,
        task: TaskDocument,
        text: String,
        mentionedUser: String = ""
    ): Result<Unit> = runCatching {
        require(task.id.isNotBlank()) { "Task ID must not be blank" }
        require(text.isNotBlank()) { "Текст комментария не может быть пустым" }
        val newComment = TaskCommentItem(
            id = "cmt_${System.currentTimeMillis()}",
            authorId = actor.id,
            authorName = actor.shortName,
            authorRole = actor.role.code,
            text = text.trim().take(280),
            mentionedUser = mentionedUser.trim().take(80),
            createdAtText = nowTimeText(),
            isRead = false
        )
        val updatedComments = (task.commentItems + newComment.encode()).takeLast(50)
        val docRef = db.collection("tasks").document(task.id)
        try {
            docRef.update(
                mapOf(
                    "commentItems" to updatedComments,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun submitTaskCompletion(
        actor: CorporateEmployee,
        task: TaskDocument,
        completionNote: String = ""
    ): Result<TaskStatus> = runCatching {
        val uid = requireUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank" }

        // Mandatory Photo Report Validation Rule
        val currentPhotos = task.photoItems.size
        val requiredPhotos = task.minPhotoCount.toInt()
        if (task.requiresPhotoReport && currentPhotos < requiredPhotos) {
            throw IllegalStateException(
                "Нельзя отметить выполнение без фотоотчёта: требуется минимум $requiredPhotos фото (добавлено: $currentPhotos)"
            )
        }

        val targetStatus = if (task.requiresApproval) TaskStatus.ON_REVIEW else TaskStatus.APPROVED
        val historyNote = if (targetStatus == TaskStatus.ON_REVIEW) {
            "Выполнено и отправлено на проверку ИТР/Руководителю (фото: $currentPhotos шт.)"
        } else {
            "Выполнено (подтверждение не требуется)"
        }

        val updatedHistory = (task.historyItems + StatusHistoryEntry(
            timestampText = nowTimeText(),
            actorName = actor.shortName,
            statusCode = targetStatus.code,
            note = historyNote
        ).encode()).takeLast(50)

        val updatedComments = if (completionNote.isNotBlank()) {
            (task.commentItems + TaskCommentItem(
                id = "cmt_${System.currentTimeMillis()}",
                authorId = actor.id,
                authorName = actor.shortName,
                authorRole = actor.role.code,
                text = completionNote.trim().take(280),
                mentionedUser = "",
                createdAtText = nowTimeText(),
                isRead = false
            ).encode()).takeLast(50)
        } else {
            task.commentItems
        }

        val docRef = db.collection("tasks").document(task.id)
        val updates = mutableMapOf<String, Any>(
            "status" to targetStatus.code,
            "historyItems" to updatedHistory,
            "commentItems" to updatedComments,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (targetStatus == TaskStatus.APPROVED) {
            updates["approvedByName"] = actor.fullName
        }

        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }

        // Spawn next instance if recurring task
        if (task.recurrence != RecurrenceRule.NONE) {
            val deltaDays = when (task.recurrence) {
                RecurrenceRule.DAILY, RecurrenceRule.WEEKDAYS -> 1L
                RecurrenceRule.WEEKLY -> 7L
                RecurrenceRule.MONTHLY -> 30L
                RecurrenceRule.CUSTOM -> 3L
                RecurrenceRule.NONE -> 0L
            }
            if (deltaDays > 0L) {
                val nextEpochDay = task.dueDateEpochDay + deltaDays
                val nextDocRef = db.collection("tasks")
                    .document("rec_${UUID.randomUUID().toString().replace("-", "").take(14)}")
                val resetSubtasks = task.decodedSubtasks().map { it.copy(isDone = false).encode() }
                val nextTask = task.copy(
                    id = "",
                    status = TaskStatus.NEW.code,
                    dueDateEpochDay = nextEpochDay,
                    originalDueDateEpochDay = nextEpochDay,
                    carryoverCount = 0L,
                    subtaskItems = resetSubtasks,
                    photoItems = emptyList(),
                    commentItems = emptyList(),
                    historyItems = listOf(
                        StatusHistoryEntry(
                            timestampText = nowTimeText(),
                            actorName = "Система повторов",
                            statusCode = TaskStatus.NEW.code,
                            note = "Автосоздание по правилу «${task.recurrence.titleRu}»"
                        ).encode()
                    ),
                    rejectionReason = null,
                    approvedByName = null
                )
                try {
                    nextDocRef.set(taskToCreatePayload(uid, nextTask)).await()
                } catch (_: Exception) {
                }
            }
        }

        targetStatus
    }

    suspend fun approveTaskReport(
        supervisor: CorporateEmployee,
        task: TaskDocument,
        comment: String = ""
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank" }
        val assignee = CorporateSeedData.findEmployee(task.assigneeId)

        if (!supervisor.role.canApproveReportOf(assignee.role)) {
            throw IllegalAccessException(
                "Недостаточно прав: роль «${supervisor.role.titleRu}» не может подтверждать отчёт сотрудника «${assignee.shortName}» (${assignee.role.titleRu})"
            )
        }

        val updatedHistory = (task.historyItems + StatusHistoryEntry(
            timestampText = nowTimeText(),
            actorName = supervisor.shortName,
            statusCode = TaskStatus.APPROVED.code,
            note = "Отчёт подтверждён (${supervisor.role.titleRu})${if (comment.isNotBlank()) ": $comment" else ""}"
        ).encode()).takeLast(50)

        val updatedComments = if (comment.isNotBlank()) {
            (task.commentItems + TaskCommentItem(
                id = "cmt_${System.currentTimeMillis()}",
                authorId = supervisor.id,
                authorName = supervisor.shortName,
                authorRole = supervisor.role.code,
                text = "Подтверждено: ${comment.trim().take(240)}",
                mentionedUser = assignee.shortName,
                createdAtText = nowTimeText(),
                isRead = false
            ).encode()).takeLast(50)
        } else {
            task.commentItems
        }

        val docRef = db.collection("tasks").document(task.id)
        try {
            docRef.update(
                mapOf(
                    "status" to TaskStatus.APPROVED.code,
                    "approvedByName" to supervisor.fullName,
                    "historyItems" to updatedHistory,
                    "commentItems" to updatedComments,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }

        appendAuditLog(
            uid = uid,
            actor = supervisor,
            actionType = "APPROVE_REPORT",
            targetType = "TASK",
            targetId = task.id,
            details = "Подтверждён отчёт по задаче «${task.title}» (исполнитель: ${task.assigneeName})"
        )
    }

    suspend fun rejectTaskReport(
        supervisor: CorporateEmployee,
        task: TaskDocument,
        reason: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank" }
        val cleanReason = reason.trim()
        if (cleanReason.isBlank()) {
            throw IllegalArgumentException("При отклонении фотоотчёта необходимо указать обязательную причину возврата")
        }

        val assignee = CorporateSeedData.findEmployee(task.assigneeId)
        if (!supervisor.role.canApproveReportOf(assignee.role)) {
            throw IllegalAccessException(
                "Недостаточно прав: роль «${supervisor.role.titleRu}» не может отклонять отчёт сотрудника «${assignee.shortName}»"
            )
        }

        val updatedHistory = (task.historyItems + StatusHistoryEntry(
            timestampText = nowTimeText(),
            actorName = supervisor.shortName,
            statusCode = TaskStatus.REJECTED.code,
            note = "Отчёт отклонён, возвращено в доработку. Причина: $cleanReason"
        ).encode()).takeLast(50)

        val updatedComments = (task.commentItems + TaskCommentItem(
            id = "cmt_${System.currentTimeMillis()}",
            authorId = supervisor.id,
            authorName = supervisor.shortName,
            authorRole = supervisor.role.code,
            text = "@${assignee.shortName} Возврат на доработку: ${cleanReason.take(220)}",
            mentionedUser = assignee.shortName,
            createdAtText = nowTimeText(),
            isRead = false
        ).encode()).takeLast(50)

        val docRef = db.collection("tasks").document(task.id)
        try {
            docRef.update(
                mapOf(
                    "status" to TaskStatus.REJECTED.code,
                    "rejectionReason" to cleanReason.take(500),
                    "historyItems" to updatedHistory,
                    "commentItems" to updatedComments,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }

        appendAuditLog(
            uid = uid,
            actor = supervisor,
            actionType = "REJECT_REPORT",
            targetType = "TASK",
            targetId = task.id,
            details = "Отклонён отчёт «${task.title}» (причина: ${cleanReason.take(150)})"
        )
    }

    suspend fun archiveTask(
        actor: CorporateEmployee,
        task: TaskDocument
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank" }
        val updatedHistory = (task.historyItems + StatusHistoryEntry(
            timestampText = nowTimeText(),
            actorName = actor.shortName,
            statusCode = TaskStatus.ARCHIVED.code,
            note = "Задача перенесена в архив"
        ).encode()).takeLast(50)

        val docRef = db.collection("tasks").document(task.id)
        try {
            docRef.update(
                mapOf(
                    "status" to TaskStatus.ARCHIVED.code,
                    "historyItems" to updatedHistory,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }

        appendAuditLog(
            uid = uid,
            actor = actor,
            actionType = "ARCHIVE_TASK",
            targetType = "TASK",
            targetId = task.id,
            details = "Задача «${task.title}» отправлена в архив"
        )
    }

    // --- AUTOMATIC END-OF-DAY CARRYOVER ENGINE ---

    suspend fun runEndOfDayCarryover(
        actor: CorporateEmployee,
        tasks: List<TaskDocument>,
        currentEpochDay: Long
    ): Result<Int> = runCatching {
        val uid = requireUserId()
        val eligibleTasks = tasks.filter { task ->
            task.dueDateEpochDay <= currentEpochDay && task.taskStatus.isEligibleForCarryover
        }
        if (eligibleTasks.isEmpty()) return@runCatching 0

        val nextEpochDay = currentEpochDay + 1L
        val batch = db.batch()
        eligibleTasks.forEach { task ->
            val newCarryoverCount = task.carryoverCount + 1L
            val newHistory = (task.historyItems + StatusHistoryEntry(
                timestampText = nowTimeText(),
                actorName = "Автоперенос смены",
                statusCode = TaskStatus.IN_PROGRESS.code,
                note = "Невыполненная задача перенесена на следующий день (перенос №$newCarryoverCount, исх. день: ${task.originalDueDateEpochDay})"
            ).encode()).takeLast(50)

            val docRef = db.collection("tasks").document(task.id)
            batch.update(
                docRef,
                mapOf(
                    "dueDateEpochDay" to nextEpochDay,
                    "carryoverCount" to newCarryoverCount,
                    "historyItems" to newHistory,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tasks_carryover")
            throw e
        }

        appendAuditLog(
            uid = uid,
            actor = actor,
            actionType = "AUTO_CARRYOVER",
            targetType = "SYSTEM",
            targetId = "day_$currentEpochDay",
            details = "Автоматический перенос ${eligibleTasks.size} невыполненных задач на следующий день с сохранением originalDueDate"
        )

        eligibleTasks.size
    }

    // --- TEMPLATES & GENERATED REPORTS ---

    suspend fun saveReportTemplate(
        actor: CorporateEmployee,
        template: ReportTemplateDocument
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = template.id.ifBlank { "tpl_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
        val docRef = db.collection("report_templates").document(docId)
        try {
            if (template.id.isNotBlank() && template.createdAt != null) {
                docRef.update(
                    mapOf(
                        "name" to template.name.take(150),
                        "reportType" to template.reportType,
                        "sheetLayout" to template.sheetLayout,
                        "exportFormat" to template.exportFormat,
                        "orgHeader" to template.orgHeader.take(300),
                        "defaultPeriodDays" to template.defaultPeriodDays.coerceIn(1L, 366L),
                        "columns" to template.columns.take(20),
                        "groupByField" to (template.groupByField ?: "ASSIGNEE"),
                        "includeSignatures" to (template.includeSignatures ?: true),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(templateToCreatePayload(uid, template)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
        docId
    }

    suspend fun recordGeneratedReport(
        actor: CorporateEmployee,
        report: GeneratedReportDocument
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = "rep_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val docRef = db.collection("generated_reports").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "templateId" to report.templateId.ifBlank { "custom" }.take(128),
            "templateName" to report.templateName.ifBlank { "Отчёт" }.take(150),
            "reportType" to report.reportType.take(64),
            "authorName" to actor.fullName.take(120),
            "periodLabel" to report.periodLabel.take(120),
            "sheetLayout" to report.sheetLayout,
            "exportFormat" to report.exportFormat,
            "filePath" to report.filePath.take(500),
            "summaryText" to report.summaryText.take(5000),
            "recipients" to report.recipients.take(20),
            "filterSummary" to (report.filterSummary ?: "Роль: ${actor.role.titleRu}").take(300),
            "rowCount" to (report.rowCount ?: 0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }

        appendAuditLog(
            uid = uid,
            actor = actor,
            actionType = "EXPORT_REPORT",
            targetType = "REPORT",
            targetId = docId,
            details = "Сформирован документ «${report.templateName}» (${report.exportFormat}, ${report.sheetLayout})"
        )
        docId
    }

    // --- ROLE-SCOPED STATISTICS ENGINE ---

    fun calculateRoleScopedStatistics(
        actor: CorporateEmployee,
        allTasks: List<TaskDocument>
    ): StatisticsSummary {
        // Access control rules:
        // - HEAD: all tasks across the site
        // - ITR: tasks of subordinate brigade & self
        // - WORKER: personal tasks + aggregated brigade stats
        val scopedTasks = when (actor.role) {
            CorporateRole.HEAD -> allTasks
            CorporateRole.ITR -> allTasks.filter {
                it.groupId == actor.groupId || it.assigneeId == actor.id || it.creatorId == actor.id
            }
            CorporateRole.WORKER -> allTasks.filter { it.assigneeId == actor.id }
        }

        val scopeLabel = when (actor.role) {
            CorporateRole.HEAD -> "Весь участок: ${actor.siteName} (полный доступ)"
            CorporateRole.ITR -> "Подчинённые и ${actor.groupName}"
            CorporateRole.WORKER -> "Личная статистика (${actor.shortName}) + агрегат бригады"
        }

        val total = scopedTasks.size
        val inProgress = scopedTasks.count {
            it.taskStatus == TaskStatus.NEW || it.taskStatus == TaskStatus.IN_PROGRESS
        }
        val onReview = scopedTasks.count { it.taskStatus == TaskStatus.ON_REVIEW }
        val approved = scopedTasks.count { it.taskStatus == TaskStatus.APPROVED }
        val rejected = scopedTasks.count { it.taskStatus == TaskStatus.REJECTED }
        val overdue = scopedTasks.count { it.taskStatus == TaskStatus.OVERDUE }
        val carryovers = scopedTasks.sumOf { it.carryoverCount.toInt() }
        val withPhotos = scopedTasks.count { it.photoItems.isNotEmpty() }
        val photoCompliance = if (total > 0) ((withPhotos * 100.0) / total).toInt() else 100

        val visibleEmployees = CorporateSeedData.getVisibleEmployeesForStats(actor)
        val employeeStats = visibleEmployees.map { emp ->
            val empTasks = allTasks.filter { it.assigneeId == emp.id }
            val empTotal = empTasks.size
            val empApproved = empTasks.count { it.taskStatus == TaskStatus.APPROVED }
            val empOnReview = empTasks.count { it.taskStatus == TaskStatus.ON_REVIEW }
            val empRejected = empTasks.count { it.taskStatus == TaskStatus.REJECTED }
            val empOverdue = empTasks.count { it.taskStatus == TaskStatus.OVERDUE }
            val empCarryovers = empTasks.sumOf { it.carryoverCount.toInt() }
            val empPhotoPct = if (empTotal > 0) {
                (empTasks.count { it.photoItems.isNotEmpty() } * 100) / empTotal
            } else 100
            val rawScore = if (empTotal > 0) {
                (((empApproved + empOnReview) * 100) / empTotal) - (empOverdue * 15) - (empRejected * 10)
            } else 95
            EmployeeDisciplineStat(
                employee = emp,
                assigned = empTotal,
                approved = empApproved,
                onReview = empOnReview,
                rejected = empRejected,
                overdue = empOverdue,
                carryovers = empCarryovers,
                photoCompliancePercent = empPhotoPct,
                disciplineScore = rawScore.coerceIn(20, 100)
            )
        }.sortedByDescending { it.disciplineScore }

        // Group breakdown: Workers see their own group aggregate; ITR sees their group; Head sees all groups
        val visibleGroups = when (actor.role) {
            CorporateRole.HEAD -> CorporateSeedData.workGroups
            CorporateRole.ITR, CorporateRole.WORKER -> CorporateSeedData.workGroups.filter {
                it.id == actor.groupId
            }
        }
        val groupStats = visibleGroups.map { grp ->
            val grpTasks = allTasks.filter { it.groupId == grp.id }
            val gTotal = grpTasks.size
            val gApproved = grpTasks.count { it.taskStatus == TaskStatus.APPROVED }
            val gReview = grpTasks.count { it.taskStatus == TaskStatus.ON_REVIEW }
            val gOverdue = grpTasks.count { it.taskStatus == TaskStatus.OVERDUE }
            val pct = if (gTotal > 0) ((gApproved + gReview) * 100) / gTotal else 0
            GroupStatBreakdown(
                groupId = grp.id,
                groupName = grp.name,
                totalTasks = gTotal,
                approvedTasks = gApproved,
                onReviewTasks = gReview,
                overdueTasks = gOverdue,
                completionPercent = pct
            )
        }

        return StatisticsSummary(
            roleScopeLabel = scopeLabel,
            totalAssigned = total,
            inProgressCount = inProgress,
            onReviewCount = onReview,
            approvedCount = approved,
            rejectedCount = rejected,
            overdueCount = overdue,
            totalCarryovers = carryovers,
            tasksWithPhotosCount = withPhotos,
            photoCompliancePercent = photoCompliance,
            avgCompletionHours = 4.2,
            employeeStats = employeeStats,
            groupStats = groupStats
        )
    }

    // --- INTERNAL PAYLOAD HELPERS ---

    private suspend fun appendAuditLog(
        uid: String,
        actor: CorporateEmployee,
        actionType: String,
        targetType: String,
        targetId: String,
        details: String
    ) {
        val logRef = db.collection("audit_logs")
            .document("log_${UUID.randomUUID().toString().replace("-", "").take(14)}")
        val payload = mapOf(
            "userId" to uid,
            "actorId" to actor.id.take(64),
            "actorName" to actor.fullName.take(120),
            "actorRole" to actor.role.code,
            "actionType" to actionType.take(64),
            "targetType" to targetType.take(64),
            "targetId" to targetId.take(128),
            "details" to details.take(1000),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            logRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, logRef.path)
        }
    }

    private fun taskToCreatePayload(uid: String, task: TaskDocument): Map<String, Any> {
        val map = mutableMapOf<String, Any?>(
            "userId" to uid,
            "title" to task.title.take(200),
            "description" to task.description.take(2000),
            "creatorId" to task.creatorId.take(64),
            "creatorName" to task.creatorName.take(120),
            "assigneeId" to task.assigneeId.take(64),
            "assigneeName" to task.assigneeName.take(120),
            "groupId" to task.groupId.take(64),
            "groupName" to task.groupName.take(120),
            "siteId" to task.siteId.take(64),
            "priority" to task.priority,
            "status" to task.status,
            "dueDateEpochDay" to task.dueDateEpochDay,
            "originalDueDateEpochDay" to task.originalDueDateEpochDay,
            "deadlineTime" to task.deadlineTime.take(16),
            "carryoverCount" to task.carryoverCount,
            "requiresPhotoReport" to task.requiresPhotoReport,
            "minPhotoCount" to task.minPhotoCount,
            "requiresApproval" to task.requiresApproval,
            "recurrenceRule" to task.recurrenceRule,
            "tags" to task.tags.take(10),
            "subtaskItems" to task.subtaskItems.take(25),
            "photoItems" to task.photoItems.take(30),
            "commentItems" to task.commentItems.take(50),
            "historyItems" to task.historyItems.take(50),
            "rejectionReason" to task.rejectionReason?.take(500),
            "approvedByName" to task.approvedByName?.take(120),
            "geoLocation" to task.geoLocation?.take(120),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return map.filterValues { it != null } as Map<String, Any>
    }

    private fun templateToCreatePayload(uid: String, tpl: ReportTemplateDocument): Map<String, Any> {
        val map = mutableMapOf<String, Any?>(
            "userId" to uid,
            "name" to tpl.name.take(150),
            "reportType" to tpl.reportType,
            "sheetLayout" to tpl.sheetLayout,
            "exportFormat" to tpl.exportFormat,
            "orgHeader" to tpl.orgHeader.take(300),
            "defaultPeriodDays" to tpl.defaultPeriodDays.coerceIn(1L, 366L),
            "columns" to tpl.columns.take(20),
            "groupByField" to (tpl.groupByField ?: "ASSIGNEE"),
            "includeSignatures" to (tpl.includeSignatures ?: true),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return map.filterValues { it != null } as Map<String, Any>
    }
}
