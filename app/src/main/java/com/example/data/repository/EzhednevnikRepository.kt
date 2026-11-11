package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AuditLogDocument
import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.EmployeeDisciplineStat
import com.example.data.model.GeneratedReportDocument
import com.example.data.model.GroupStatBreakdown
import com.example.data.model.MemberApprovalStatus
import com.example.data.model.PhotoReportItem
import com.example.data.model.RecurrenceRule
import com.example.data.model.ReportTemplateDocument
import com.example.data.model.SiteMemberDocument
import com.example.data.model.StatisticsSummary
import com.example.data.model.StatusHistoryEntry
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskCommentItem
import com.example.data.model.TaskDocument
import com.example.data.model.TaskDraft
import com.example.data.model.TaskStatus
import com.example.data.model.UserProfile
import com.example.data.model.WorkGroup
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
        emit(null)
        if (auth.currentUser == null) return@flow
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
        emit(emptyList())
        if (auth.currentUser == null) return@flow
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
        emit(emptyList())
        if (auth.currentUser == null) return@flow
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
        emit(emptyList())
        if (auth.currentUser == null) return@flow
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
        emit(emptyList())
        if (auth.currentUser == null) return@flow
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

    fun observeSiteMembers(): Flow<List<SiteMemberDocument>> = flow {
        emit(emptyList())
        val uid = auth.currentUser?.uid ?: ""
        if (uid.isBlank()) return@flow
        val path = "site_members"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { querySnapshot ->
                    querySnapshot.toObjects(
                        SiteMemberDocument::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<SiteMemberDocument> { !it.isPendingRole }
                            .thenBy { it.fullName }
                    )
                }
                .catch { error ->
                    if (uid.isNotBlank()) {
                        logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    }
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

    suspend fun ensureWorkspaceSeeded(displayName: String = "Руководитель"): Result<Unit> = runCatching {
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
                "displayName" to displayName.ifBlank { "Руководитель" }.take(100),
                "siteName" to CorporateSeedData.DEFAULT_SITE_NAME,
                "activePersonaId" to uid,
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

        // Purge any legacy fake seed tasks and fake accounts from previous versions
        purgeLegacyFakeSeedData(uid)
    }

    suspend fun purgeLegacyFakeSeedData(uid: String = auth.currentUser?.uid ?: "") {
        if (uid.isBlank()) return
        try {
            val batch = db.batch()
            for (i in 1..8) {
                batch.delete(db.collection("tasks").document("${uid}_seed_task_$i"))
                batch.delete(db.collection("tasks").document("seed_task_$i"))
            }
            listOf(
                "emp_head_1",
                "emp_itr_1",
                "emp_itr_2",
                "emp_worker_1",
                "emp_worker_2",
                "emp_worker_3",
                "emp_worker_4",
                "pending_emp_1",
                "pending_emp_2"
            ).forEach { fakeId ->
                batch.delete(db.collection("site_members").document(fakeId))
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Ignore if already purged or offline
        }
    }

    suspend fun registerOrResolveSiteMember(
        rawMemberId: String,
        email: String,
        fullName: String,
        position: String = "Специалист участка",
        requestedGroup: WorkGroup = CorporateSeedData.workGroups.first(),
        forceHeadActivation: Boolean = false
    ): Result<SiteMemberDocument> = runCatching {
        val safeMemberId = rawMemberId
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(120)
            .ifBlank { "usr_${System.currentTimeMillis()}" }
        val normalizedEmail = email.trim().lowercase().ifBlank { "$safeMemberId@site1.ru" }
        val isAutoHead = forceHeadActivation || SiteMemberDocument.isRootHeadEmail(normalizedEmail)

        val resolvedRole = if (isAutoHead) CorporateRole.HEAD.code else "PENDING"
        val resolvedStatus = if (isAutoHead) {
            MemberApprovalStatus.APPROVED.code
        } else {
            MemberApprovalStatus.PENDING.code
        }
        val resolvedGroup = if (isAutoHead) {
            CorporateSeedData.workGroups.last()
        } else {
            requestedGroup
        }
        val resolvedPosition = if (isAutoHead) {
            "Начальник строительного участка"
        } else {
            position.ifBlank { "Специалист участка" }
        }

        if (auth.currentUser != null) {
            // Check if pre-registered by email or already registered by memberId
            val existingByEmail = try {
                db.collection("site_members")
                    .whereEqualTo("email", normalizedEmail)
                    .limit(1)
                    .get()
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }

            val existingDoc = existingByEmail?.documents?.firstOrNull()
            if (existingDoc != null && existingDoc.exists()) {
                val existingObj = existingDoc.toObject(
                    SiteMemberDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existingObj != null) {
                    if (isAutoHead && existingObj.role != CorporateRole.HEAD.code) {
                        existingDoc.reference.update(
                            mapOf(
                                "role" to CorporateRole.HEAD.code,
                                "approvalStatus" to MemberApprovalStatus.APPROVED.code,
                                "position" to "Начальник строительного участка",
                                "approvedByName" to "Система (Автоопределение руководителя)",
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                        return@runCatching existingObj.copy(
                            role = CorporateRole.HEAD.code,
                            approvalStatus = MemberApprovalStatus.APPROVED.code,
                            position = "Начальник строительного участка"
                        )
                    }
                    return@runCatching existingObj
                }
            }

            val docRef = db.collection("site_members").document(safeMemberId)
            val existingByIdSnap = try {
                docRef.get().await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (existingByIdSnap != null && existingByIdSnap.exists()) {
                val existingObj = existingByIdSnap.toObject(
                    SiteMemberDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existingObj != null) {
                    return@runCatching existingObj
                }
            }

            val payload = mutableMapOf<String, Any>(
                "memberId" to safeMemberId,
                "email" to normalizedEmail.take(160),
                "fullName" to fullName.ifBlank { "Сотрудник участка" }.take(120),
                "position" to resolvedPosition.take(120),
                "role" to resolvedRole,
                "approvalStatus" to resolvedStatus,
                "groupId" to resolvedGroup.id.take(64),
                "groupName" to resolvedGroup.name.take(120),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isAutoHead) {
                payload["approvedByName"] = "Система (Владелец / Код руководителя)"
            }
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
            }
        }

        SiteMemberDocument(
            id = safeMemberId,
            memberId = safeMemberId,
            email = normalizedEmail,
            fullName = fullName.ifBlank { "Сотрудник участка" },
            position = resolvedPosition,
            role = resolvedRole,
            approvalStatus = resolvedStatus,
            groupId = resolvedGroup.id,
            groupName = resolvedGroup.name,
            approvedByName = if (isAutoHead) "Система (Владелец / Код руководителя)" else null
        )
    }

    suspend fun assignRoleToSiteMember(
        actor: CorporateEmployee,
        targetMember: SiteMemberDocument,
        assignedRole: CorporateRole,
        assignedGroup: WorkGroup,
        customPosition: String? = null
    ): Result<SiteMemberDocument> = runCatching {
        if (actor.role != CorporateRole.HEAD) {
            throw IllegalAccessException("Назначать роли пользователям может только Руководитель участка")
        }
        val safeId = targetMember.memberId.ifBlank { targetMember.id }
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val newPosition = customPosition?.takeIf { it.isNotBlank() }
            ?: targetMember.position.ifBlank { assignedRole.titleRu }

        val encodedApprovedBy = targetMember.encodeApprovedByForFirestore(actor.fullName)
        val updatedMember = targetMember.copy(
            id = safeId,
            memberId = safeId,
            role = assignedRole.code,
            approvalStatus = MemberApprovalStatus.APPROVED.code,
            groupId = assignedGroup.id,
            groupName = assignedGroup.name,
            position = newPosition,
            approvedByName = encodedApprovedBy
        )

        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            val docRef = db.collection("site_members").document(safeId)
            val snap = try {
                docRef.get().await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (snap != null && snap.exists()) {
                docRef.update(
                    mapOf(
                        "role" to assignedRole.code,
                        "approvalStatus" to MemberApprovalStatus.APPROVED.code,
                        "groupId" to assignedGroup.id.take(64),
                        "groupName" to assignedGroup.name.take(120),
                        "position" to newPosition.take(120),
                        "approvedByName" to encodedApprovedBy,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "memberId" to safeId,
                        "email" to updatedMember.email.ifBlank { "$safeId@site1.ru" }.take(160),
                        "fullName" to updatedMember.fullName.take(120),
                        "position" to newPosition.take(120),
                        "role" to assignedRole.code,
                        "approvalStatus" to MemberApprovalStatus.APPROVED.code,
                        "groupId" to assignedGroup.id.take(64),
                        "groupName" to assignedGroup.name.take(120),
                        "approvedByName" to encodedApprovedBy,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            appendAuditLog(
                uid = currentUid,
                actor = actor,
                actionType = "ASSIGN_USER_ROLE",
                targetType = "USER",
                targetId = safeId,
                details = "Назначена роль «${assignedRole.titleRu}» и подразделение «${assignedGroup.name}» сотруднику ${updatedMember.fullName} (${updatedMember.email})"
            )
        }
        updatedMember
    }

    suspend fun rejectSiteMemberAccess(
        actor: CorporateEmployee,
        targetMember: SiteMemberDocument
    ): Result<SiteMemberDocument> = runCatching {
        if (actor.role != CorporateRole.HEAD) {
            throw IllegalAccessException("Отклонять заявки на вход может только Руководитель участка")
        }
        val safeId = targetMember.memberId.ifBlank { targetMember.id }
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val updatedMember = targetMember.copy(
            approvalStatus = MemberApprovalStatus.REJECTED.code,
            approvedByName = actor.fullName
        )
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            val docRef = db.collection("site_members").document(safeId)
            try {
                docRef.update(
                    mapOf(
                        "approvalStatus" to MemberApprovalStatus.REJECTED.code,
                        "approvedByName" to actor.fullName.take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Ignore if only local seed
            }
            appendAuditLog(
                uid = currentUid,
                actor = actor,
                actionType = "REJECT_USER_ACCESS",
                targetType = "USER",
                targetId = safeId,
                details = "Отклонён доступ в систему пользователю ${targetMember.fullName} (${targetMember.email})"
            )
        }
        updatedMember
    }

    suspend fun preRegisterSiteMemberByEmail(
        actor: CorporateEmployee,
        email: String,
        fullName: String,
        position: String,
        assignedRole: CorporateRole,
        assignedGroup: WorkGroup,
        personnelNumber: String = "",
        siteName: String = "",
        phone: String = ""
    ): Result<SiteMemberDocument> = runCatching {
        if (actor.role != CorporateRole.HEAD) {
            throw IllegalAccessException("Регистрировать сотрудников и назначать роли может только Руководитель")
        }
        require(fullName.isNotBlank()) { "Укажите ФИО сотрудника" }

        val normalizedEmail = email.trim().lowercase().ifBlank {
            "emp_${System.currentTimeMillis()}@site1.ru"
        }
        val safeId = "emp_${normalizedEmail.replace(Regex("[^a-zA-Z0-9]"), "_").take(40)}"
        val resolvedPosition = position.ifBlank { assignedRole.titleRu }

        val baseMember = SiteMemberDocument(
            id = safeId,
            memberId = safeId,
            email = normalizedEmail,
            fullName = fullName.trim(),
            position = resolvedPosition,
            role = assignedRole.code,
            approvalStatus = MemberApprovalStatus.APPROVED.code,
            groupId = assignedGroup.id,
            groupName = assignedGroup.name,
            approvedByName = actor.fullName,
            personnelNumber = personnelNumber.trim(),
            siteName = siteName.trim().ifBlank { actor.siteName },
            phone = phone.trim()
        )
        val encodedApprovedBy = baseMember.encodeApprovedByForFirestore(
            approverName = actor.fullName,
            explicitPersonnelNumber = personnelNumber.trim(),
            explicitSiteName = siteName.trim().ifBlank { actor.siteName },
            explicitPhone = phone.trim()
        )
        val newMember = baseMember.copy(approvedByName = encodedApprovedBy)

        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            val docRef = db.collection("site_members").document(safeId)
            val snap = try {
                docRef.get().await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (snap != null && snap.exists()) {
                docRef.update(
                    mapOf(
                        "email" to normalizedEmail.take(160),
                        "fullName" to newMember.fullName.take(120),
                        "position" to resolvedPosition.take(120),
                        "role" to assignedRole.code,
                        "approvalStatus" to MemberApprovalStatus.APPROVED.code,
                        "groupId" to assignedGroup.id.take(64),
                        "groupName" to assignedGroup.name.take(120),
                        "approvedByName" to encodedApprovedBy,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "memberId" to safeId,
                        "email" to normalizedEmail.take(160),
                        "fullName" to newMember.fullName.take(120),
                        "position" to resolvedPosition.take(120),
                        "role" to assignedRole.code,
                        "approvalStatus" to MemberApprovalStatus.APPROVED.code,
                        "groupId" to assignedGroup.id.take(64),
                        "groupName" to assignedGroup.name.take(120),
                        "approvedByName" to encodedApprovedBy,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            appendAuditLog(
                uid = currentUid,
                actor = actor,
                actionType = "PRE_REGISTER_USER",
                targetType = "USER",
                targetId = safeId,
                details = "Зарегистрирован сотрудник ${newMember.fullName} (таб. № ${personnelNumber.ifBlank { "—" }}, участок: ${newMember.effectiveSiteName}) с ролью «${assignedRole.titleRu}»"
            )
        }
        newMember
    }

    suspend fun updateSiteMemberDetails(
        actor: CorporateEmployee,
        updatedMember: SiteMemberDocument
    ): Result<SiteMemberDocument> = runCatching {
        val safeId = updatedMember.memberId.ifBlank { updatedMember.id }
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(120)
        val encodedApprovedBy = updatedMember.encodeApprovedByForFirestore(actor.fullName)
        val normalizedMember = updatedMember.copy(
            id = safeId,
            memberId = safeId,
            approvedByName = encodedApprovedBy
        )

        val currentUid = auth.currentUser?.uid
        if (currentUid != null && safeId.isNotBlank()) {
            val docRef = db.collection("site_members").document(safeId)
            val snap = try {
                docRef.get().await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            val safeEmail = normalizedMember.email.ifBlank { "$safeId@site1.ru" }.take(160)
            val safeRole = normalizedMember.role.takeIf {
                it in listOf("PENDING", "WORKER", "ITR", "HEAD")
            } ?: CorporateRole.WORKER.code
            val safeStatus = normalizedMember.approvalStatus.takeIf {
                it in listOf("PENDING", "APPROVED", "REJECTED")
            } ?: MemberApprovalStatus.APPROVED.code

            if (snap != null && snap.exists()) {
                docRef.update(
                    mapOf(
                        "email" to safeEmail,
                        "fullName" to normalizedMember.fullName.ifBlank { "Сотрудник" }.take(120),
                        "position" to normalizedMember.position.take(120),
                        "role" to safeRole,
                        "approvalStatus" to safeStatus,
                        "groupId" to normalizedMember.groupId.ifBlank { "grp_monolith" }.take(64),
                        "groupName" to normalizedMember.groupName.ifBlank { "Основное подразделение" }.take(120),
                        "approvedByName" to encodedApprovedBy,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "memberId" to safeId,
                        "email" to safeEmail,
                        "fullName" to normalizedMember.fullName.ifBlank { "Сотрудник" }.take(120),
                        "position" to normalizedMember.position.take(120),
                        "role" to safeRole,
                        "approvalStatus" to safeStatus,
                        "groupId" to normalizedMember.groupId.ifBlank { "grp_monolith" }.take(64),
                        "groupName" to normalizedMember.groupName.ifBlank { "Основное подразделение" }.take(120),
                        "approvedByName" to encodedApprovedBy,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            appendAuditLog(
                uid = currentUid,
                actor = actor,
                actionType = "UPDATE_EMPLOYEE",
                targetType = "USER",
                targetId = safeId,
                details = "Обновлены данные сотрудника: ${normalizedMember.fullName}, должн.: ${normalizedMember.position}, таб. №: ${normalizedMember.effectivePersonnelNumber.ifBlank { "—" }}, участок: ${normalizedMember.effectiveSiteName}"
            )
        }
        normalizedMember
    }

    suspend fun updateOrganizationAndSiteProfile(
        orgName: String,
        siteName: String
    ): Result<Unit> = runCatching {
        val currentUid = auth.currentUser?.uid ?: return@runCatching
        val combinedSiteLabel = "${orgName.trim()} • ${siteName.trim()}".take(160)
        val docRef = db.collection("users").document(currentUid)
        val snap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (snap != null && snap.exists()) {
            docRef.update(
                mapOf(
                    "siteName" to combinedSiteLabel.ifBlank { CorporateSeedData.DEFAULT_SITE_NAME },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
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

    suspend fun deleteSiteMember(
        actor: CorporateEmployee,
        memberId: String
    ): Result<Unit> = runCatching {
        if (actor.role != CorporateRole.HEAD) {
            throw IllegalAccessException("Удалять учётные записи сотрудников может только Руководитель")
        }
        val safeId = memberId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        if (auth.currentUser != null && safeId.isNotBlank()) {
            try {
                db.collection("site_members").document(safeId).delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    // --- TASK CRUD & RBAC OPERATIONS ---

    suspend fun createTask(
        actor: CorporateEmployee,
        draft: TaskDraft,
        resolvedAssignee: CorporateEmployee? = null,
        customTaskId: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val assignee = resolvedAssignee ?: CorporateSeedData.findEmployee(draft.assigneeId)

        // RBAC Enforcement: Check hierarchy rights
        if (actor.id != assignee.id && !actor.role.canAssignTo(assignee.role)) {
            throw IllegalArgumentException(
                "Роль «${actor.role.titleRu}» не может назначать задачи сотруднику с ролью «${assignee.role.titleRu}»"
            )
        }
        if (actor.role == CorporateRole.WORKER && actor.id != assignee.id) {
            throw IllegalArgumentException("Рабочий может создавать только личные задачи для себя")
        }

        val docId = customTaskId?.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")?.take(64)
            ?.takeIf { it.isNotBlank() }
            ?: "task_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("tasks").document(docId)
        val encodedSubtasks = draft.subtasks.mapIndexed { idx, raw ->
            if (raw.contains("|")) {
                raw
            } else {
                SubtaskItem("sub_${idx + 1}", raw, false).encode()
            }
        }
        val initialHistory = listOf(
            StatusHistoryEntry(
                timestampText = nowTimeText(),
                actorName = actor.shortName,
                statusCode = TaskStatus.NEW.code,
                note = "Задача поставлена исполнителю ${assignee.shortName}"
            ).encode()
        )
        val mergedTags = if (draft.targetVolume > 0.0) {
            listOf("__vol__|${draft.targetVolume}|0.0|${draft.volumeUnit.replace("|", "/")}") +
                draft.tags.filterNot { it.startsWith("__vol__|") }
        } else {
            draft.tags
        }

        val taskModel = TaskDocument(
            id = docId,
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
            targetVolume = draft.targetVolume,
            completedVolume = 0.0,
            volumeUnit = draft.volumeUnit,
            tags = mergedTags.take(10),
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

    suspend fun deleteTask(
        actor: CorporateEmployee,
        task: TaskDocument
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank" }
        val docRef = db.collection("tasks").document(task.id)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
        appendAuditLog(
            uid = uid,
            actor = actor,
            actionType = "DELETE_TASK",
            targetType = "TASK",
            targetId = task.id,
            details = "Удалена задача «${task.title}»"
        )
    }

    suspend fun deletePhotoFromTask(
        actor: CorporateEmployee,
        task: TaskDocument,
        photoId: String
    ): Result<Unit> = runCatching {
        require(task.id.isNotBlank()) { "Task ID must not be blank" }
        val updatedPhotos = task.decodedPhotos()
            .filterNot { it.id == photoId }
            .map { it.encode() }
        val docRef = db.collection("tasks").document(task.id)
        try {
            docRef.update(
                mapOf(
                    "photoItems" to updatedPhotos,
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

    suspend fun updateSubtasksList(
        task: TaskDocument,
        encodedSubtasks: List<String>,
        encodedTags: List<String>? = null
    ): Result<Unit> = runCatching {
        require(task.id.isNotBlank()) { "Task ID must not be blank" }
        val docRef = db.collection("tasks").document(task.id)
        val payload = mutableMapOf<String, Any>(
            "subtaskItems" to encodedSubtasks.take(25),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (encodedTags != null) {
            payload["tags"] = encodedTags.take(10)
        }
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
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
        val seedAssignee = CorporateSeedData.employees.firstOrNull { it.id == task.assigneeId }
        val assigneeRole = seedAssignee?.role ?: CorporateRole.WORKER

        if (!supervisor.role.canApproveReportOf(assigneeRole)) {
            throw IllegalAccessException(
                "Недостаточно прав: роль «${supervisor.role.titleRu}» не может подтверждать отчёт сотрудника «${task.assigneeName}»"
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
                mentionedUser = task.assigneeName,
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

        val seedAssignee = CorporateSeedData.employees.firstOrNull { it.id == task.assigneeId }
        val assigneeRole = seedAssignee?.role ?: CorporateRole.WORKER
        if (!supervisor.role.canApproveReportOf(assigneeRole)) {
            throw IllegalAccessException(
                "Недостаточно прав: роль «${supervisor.role.titleRu}» не может отклонять отчёт сотрудника «${task.assigneeName}»"
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
            text = "@${task.assigneeName} Возврат на доработку: ${cleanReason.take(220)}",
            mentionedUser = task.assigneeName,
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
        allTasks: List<TaskDocument>,
        customEmployees: List<CorporateEmployee>? = null
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
            CorporateRole.HEAD -> "Весь участок (${actor.shortName})"
            CorporateRole.ITR -> "Подчинённые и ${actor.groupName}"
            CorporateRole.WORKER -> "Личная статистика (${actor.shortName})"
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

        val basePool = if (!customEmployees.isNullOrEmpty()) {
            when (actor.role) {
                CorporateRole.HEAD -> customEmployees
                CorporateRole.ITR -> customEmployees.filter {
                    it.groupId == actor.groupId || it.id == actor.id
                }
                CorporateRole.WORKER -> customEmployees.filter { it.id == actor.id }
            }.ifEmpty { listOf(actor) }
        } else {
            CorporateSeedData.getVisibleEmployeesForStats(actor)
        }

        val visibleEmployees = basePool
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
