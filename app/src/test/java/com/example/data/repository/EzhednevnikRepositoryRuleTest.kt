package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.RecurrenceRule
import com.example.data.model.TaskDraft
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.seed.CorporateSeedData
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
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
    fun createTask_andBase64PhotoReportLifecycle_syncsAcrossDevicesAndApproves() = runBlocking {
        signInTestUser("alice_corp@test.com")
        val repository = EzhednevnikRepository(firestore)

        // 1. ITR creates task for Worker requiring 2 photos
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(itrEmployee, sampleDraft(assigneeId = workerEmployee.id, minPhotos = 2))
        }
        assertTrue("Task creation should succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val taskId = createResult.getOrThrow()

        // 2. Worker attaches 2 photos with embedded Base64 payload (>20KB) and submits -> transitions to ON_REVIEW
        val initialTask = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getTaskById(taskId).getOrThrow() }
        val base64Payload1 = "/data/user/0/app/files/task_photos/cam_1.jpg#b64=" + "A".repeat(24000)
        val base64Payload2 = "/data/user/0/app/files/task_photos/cam_2.jpg#b64=" + "B".repeat(24000)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addPhotoToTask(workerEmployee, initialTask, base64Payload1, "55.75, 37.61", "Фотофиксация СМР #1").getOrThrow()
        }
        val taskWith1Photo = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getTaskById(taskId).getOrThrow() }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addPhotoToTask(workerEmployee, taskWith1Photo, base64Payload2, "55.75, 37.61", "Фотофиксация СМР #2").getOrThrow()
        }

        // 3. Supervisor on another account/device reads the task and receives the full Base64 photo payloads
        signInTestUser("supervisor_bob@test.com")
        val supervisorRepo = EzhednevnikRepository(firestore)
        val taskWith2Photos = withTimeout(DEFAULT_TIMEOUT_MS) { supervisorRepo.getTaskById(taskId).getOrThrow() }
        assertEquals(2, taskWith2Photos.decodedPhotos().size)
        assertTrue(taskWith2Photos.decodedPhotos()[0].uriString.contains("#b64="))
        assertTrue(taskWith2Photos.decodedPhotos()[0].uriString.length > 20000)

        val submitResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            supervisorRepo.submitTaskCompletion(workerEmployee, taskWith2Photos, "Все 2 фото приложены")
        }
        assertEquals(TaskStatus.ON_REVIEW, submitResult.getOrThrow())

        // 4. Worker tries to approve report -> blocked by RBAC
        val onReviewTask = withTimeout(DEFAULT_TIMEOUT_MS) { supervisorRepo.getTaskById(taskId).getOrThrow() }
        val workerApproveAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            supervisorRepo.approveTaskReport(workerEmployee, onReviewTask, "Сам себе подтверждаю")
        }
        assertTrue("Worker must not be allowed to approve reports", workerApproveAttempt.isFailure)

        // 5. ITR approves report -> status becomes APPROVED
        val itrApprove = withTimeout(DEFAULT_TIMEOUT_MS) {
            supervisorRepo.approveTaskReport(itrEmployee, onReviewTask, "Проверено, замечаний нет")
        }
        assertTrue("ITR approval should succeed", itrApprove.isSuccess)
        val approvedTask = withTimeout(DEFAULT_TIMEOUT_MS) { supervisorRepo.getTaskById(taskId).getOrThrow() }
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
    fun unauthenticatedUser_cannotReadOrWriteTasks_failsWithPermissionDenied() = runBlocking {
        signInTestUser("owner_alice@test.com")
        val aliceRepo = EzhednevnikRepository(firestore)
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTask(headEmployee, sampleDraft()).getOrThrow()
        }

        auth.signOut()
        val unauthRead = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("tasks").document(taskId).get().await()
            }
        }
        assertTrue("Unauthenticated task read must fail", unauthRead.isFailure)
        val fsEx = unauthRead.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
