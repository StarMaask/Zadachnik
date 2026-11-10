package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.RecurrenceRule
import com.example.data.model.TaskDraft
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.seed.CorporateSeedData
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EzhednevnikRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private val headEmployee = CorporateSeedData.findEmployee("emp_head_1")
    private val itrEmployee = CorporateSeedData.findEmployee("emp_itr_1")
    private val workerEmployee = CorporateSeedData.findEmployee("emp_worker_1")

    private fun sampleDraft(assigneeId: String = workerEmployee.id, minPhotos: Int = 2): TaskDraft {
        return TaskDraft(
            title = "Проверка опалубки колонны К-12",
            description = "Выполнить фотофиксацию вертикальности и замков",
            assigneeId = assigneeId,
            priority = TaskPriority.HIGH,
            dueDateEpochDay = 20768L,
            deadlineTime = "18:00",
            requiresPhotoReport = minPhotos > 0,
            minPhotoCount = minPhotos,
            requiresApproval = true,
            recurrenceRule = RecurrenceRule.NONE,
            tags = listOf("Опалубка", "КЖ"),
            subtasks = listOf("Проверить отвес", "Закрепить подкосы"),
            geoLocation = "Участок №1, Секция А"
        )
    }

    @Test
    fun createTask_andPhotoReportLifecycle_enforcesPhotoMinimumAndApproval() = runBlocking {
        signInTestUser("alice_corp@test.com")
        val repository = EzhednevnikRepository(firestore)

        // 1. ITR creates task for Worker requiring 2 photos
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(itrEmployee, sampleDraft(assigneeId = workerEmployee.id, minPhotos = 2))
        }
        assertTrue("Task creation should succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val taskId = createResult.getOrThrow()

        // 2. Worker tries to complete without required 2 photos -> must fail
        val initialTask = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getTaskById(taskId).getOrThrow() }
        val prematureSubmit = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitTaskCompletion(workerEmployee, initialTask, "Попытка закрыть без фото")
        }
        assertTrue("Completion without photos must be blocked", prematureSubmit.isFailure)

        // 3. Worker adds 2 photos and submits -> transitions to ON_REVIEW
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addPhotoToTask(workerEmployee, initialTask, "stamp://photo1", "55.75, 37.61", "Фото 1").getOrThrow()
        }
        val taskWith1Photo = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getTaskById(taskId).getOrThrow() }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addPhotoToTask(workerEmployee, taskWith1Photo, "stamp://photo2", "55.75, 37.61", "Фото 2").getOrThrow()
        }
        val taskWith2Photos = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getTaskById(taskId).getOrThrow() }
        val submitResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitTaskCompletion(workerEmployee, taskWith2Photos, "Все 2 фото приложены")
        }
        assertEquals(TaskStatus.ON_REVIEW, submitResult.getOrThrow())

        // 4. Worker tries to approve report -> blocked by RBAC
        val onReviewTask = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getTaskById(taskId).getOrThrow() }
        val workerApproveAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.approveTaskReport(workerEmployee, onReviewTask, "Сам себе подтверждаю")
        }
        assertTrue("Worker must not be allowed to approve reports", workerApproveAttempt.isFailure)

        // 5. ITR approves report -> status becomes APPROVED
        val itrApprove = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.approveTaskReport(itrEmployee, onReviewTask, "Проверено, замечаний нет")
        }
        assertTrue("ITR approval should succeed", itrApprove.isSuccess)
        val approvedTask = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getTaskById(taskId).getOrThrow() }
        assertEquals(TaskStatus.APPROVED, approvedTask.taskStatus)
    }

    @Test
    fun runEndOfDayCarryover_carriesOverUncompletedTasks_andPreservesOriginalDate() = runBlocking {
        signInTestUser("carryover_user@test.com")
        val repository = EzhednevnikRepository(firestore)

        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(headEmployee, sampleDraft(minPhotos = 1)).getOrThrow()
        }
        val tasksBefore = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserTasks().getOrThrow() }
        val carriedCount = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.runEndOfDayCarryover(headEmployee, tasksBefore, currentEpochDay = 20768L).getOrThrow()
        }
        assertTrue(carriedCount >= 1)

        val carriedTask = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getTaskById(taskId).getOrThrow() }
        assertEquals(20769L, carriedTask.dueDateEpochDay)
        assertEquals(20768L, carriedTask.originalDueDateEpochDay)
        assertEquals(1L, carriedTask.carryoverCount)
    }

    @Test
    fun getTaskById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser("owner_alice@test.com")
        val aliceRepo = EzhednevnikRepository(firestore)
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTask(headEmployee, sampleDraft()).getOrThrow()
        }

        signInTestUser("intruder_bob@test.com")
        val bobRepo = EzhednevnikRepository(firestore)
        val crossUserResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getTaskById(taskId)
        }
        assertFalse("Cross-user task read must fail", crossUserResult.isSuccess)
        val fsEx = crossUserResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = EzhednevnikRepository(firestore)

        val result = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTasks("unauth_uid").first()
            }
        }
        assertTrue("Unauthenticated flow collection must fail", result.isFailure)
        val fsEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
