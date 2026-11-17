package com.example

import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.ExportFormat
import com.example.data.model.RecurrenceRule
import com.example.data.model.ReportTemplateDocument
import com.example.data.model.ReportType
import com.example.data.model.SheetLayout
import com.example.data.model.SiteMemberDocument
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskDocument
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.repository.EzhednevnikRepository
import com.example.data.seed.CorporateSeedData
import com.example.domain.reporting.ReportDocumentGenerator
import com.example.domain.rpg.EmployeeRpgProfile
import com.example.domain.rpg.GamificationConfig
import com.example.domain.rpg.LeaderboardPrivacy
import com.example.domain.rpg.PrestigeState
import com.example.domain.rpg.RpgProgressionEngine
import com.example.domain.rpg.RpgWorkspaceState
import com.example.domain.rpg.Streak
import com.example.domain.rpg.UserSkill
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `role hierarchy permissions enforce HEAD over ITR over WORKER`() {
        val head = CorporateRole.HEAD
        val itr = CorporateRole.ITR
        val worker = CorporateRole.WORKER

        // Assignment permissions
        assertTrue(head.canAssignTo(CorporateRole.HEAD))
        assertTrue(head.canAssignTo(CorporateRole.ITR))
        assertTrue(head.canAssignTo(CorporateRole.WORKER))

        assertFalse(itr.canAssignTo(CorporateRole.HEAD))
        assertTrue(itr.canAssignTo(CorporateRole.ITR))
        assertTrue(itr.canAssignTo(CorporateRole.WORKER))

        assertFalse(worker.canAssignTo(CorporateRole.HEAD))
        assertFalse(worker.canAssignTo(CorporateRole.ITR))
        assertTrue(worker.canAssignTo(CorporateRole.WORKER))

        // Approval permissions
        assertTrue(head.canApproveReportOf(CorporateRole.ITR))
        assertTrue(head.canApproveReportOf(CorporateRole.WORKER))
        assertTrue(itr.canApproveReportOf(CorporateRole.WORKER))
        assertFalse(itr.canApproveReportOf(CorporateRole.ITR))
        assertFalse(worker.canApproveReportOf(CorporateRole.WORKER))

        // Consolidated report permissions
        assertTrue(head.canExportConsolidatedPersonnelReport())
        assertTrue(itr.canExportConsolidatedPersonnelReport())
        assertFalse(worker.canExportConsolidatedPersonnelReport())
    }

    @Test
    fun `report preview in SPLIT_SHEETS creates two distinct sheets for Plan and Report`() {
        val today = CorporateSeedData.BASE_EPOCH_DAY
        val tasks = CorporateSeedData.buildInitialTasks(userId = "test_user_1")
        val headActor = CorporateSeedData.findEmployee("emp_head_1")
        val template = CorporateSeedData.buildInitialReportTemplates(userId = "test_user_1").first()

        val preview = ReportDocumentGenerator.buildPreview(
            actor = headActor,
            template = template,
            allTasks = tasks,
            currentEpochDay = today,
            overrideLayout = SheetLayout.SPLIT_SHEETS,
            overrideFormat = ExportFormat.XLSX
        )

        assertEquals(2, preview.sheets.size)
        assertTrue(preview.sheets[0].sheetTitle.contains("План", ignoreCase = true))
        assertTrue(
            preview.sheets[1].sheetTitle.contains("Отчет", ignoreCase = true) ||
                preview.sheets[1].sheetTitle.contains("Отчёт", ignoreCase = true)
        )

        // Single-sheet mode must explicitly contain two separate sections on one sheet:
        // Section 1 = Daily Fact Report, Section 2 = Next-Day Plan
        val singleSheetPreview = ReportDocumentGenerator.buildPreview(
            actor = headActor,
            template = template,
            allTasks = tasks,
            currentEpochDay = today,
            overrideLayout = SheetLayout.SINGLE_SHEET,
            overrideFormat = ExportFormat.PDF
        )
        assertEquals(1, singleSheetPreview.sheets.size)
        assertEquals(2, singleSheetPreview.sheets[0].sections.size)
        assertTrue(singleSheetPreview.sheets[0].sections[0].bannerTitle.contains("ОТЧЁТ ЗА ТЕКУЩИЙ ДЕНЬ"))
        assertTrue(singleSheetPreview.sheets[0].sections[1].bannerTitle.contains("ПЛАН РАБОТ НА СЛЕДУЮЩИЙ ДЕНЬ"))
    }

    @Test
    fun `task volume tracking enforces 100 percent completion and reports unfinished volume`() {
        val subPartial = SubtaskItem(
            id = "sub_1",
            title = "Отгрузка песка",
            isDone = false,
            targetVolume = 100.0,
            completedVolume = 60.0,
            unit = "кг"
        )
        val encoded = subPartial.encode()
        val decoded = SubtaskItem.decode(encoded)

        assertEquals(60, decoded.progressPercent)
        assertFalse(decoded.isDone)

        val taskPartial = TaskDocument(
            id = "task_vol_1",
            userId = "u1",
            title = "Заготовка смеси",
            assigneeId = "emp_head_1",
            assigneeName = "Иванов И.И.",
            dueDateEpochDay = CorporateSeedData.BASE_EPOCH_DAY,
            subtaskItems = listOf(encoded)
        ).withUpdatedVolume(100.0, 60.0, "кг")

        assertFalse(taskPartial.isVolumeCompleted100Percent())
        assertEquals(60, taskPartial.completionPercent())
        val partialReportText = taskPartial.formatVolumeSummaryForReport()
        assertTrue(partialReportText.contains("Не до конца"))
        assertTrue(partialReportText.contains("60 из 100 кг"))

        val subDone = subPartial.copy(isDone = true, completedVolume = 100.0)
        val taskCompleted = taskPartial
            .copy(subtaskItems = listOf(subDone.encode()))
            .withUpdatedVolume(100.0, 100.0, "кг")
        assertTrue(taskCompleted.isVolumeCompleted100Percent())
        assertEquals(100, taskCompleted.completionPercent())
        assertTrue(taskCompleted.formatVolumeSummaryForReport().contains("Выполнено: 100 из 100 кг (100%)"))
    }

    @Test
    fun `report preview supports export with photos and without photos`() {
        val today = CorporateSeedData.BASE_EPOCH_DAY
        val headActor = CorporateSeedData.findEmployee("emp_head_1")
        val template = CorporateSeedData.buildInitialReportTemplates(userId = "test_user_1").first()
        val sampleTask = TaskDocument(
            id = "t_rep_1",
            userId = "test_user_1",
            title = "Монтаж кабельных лотков",
            assigneeId = headActor.id,
            assigneeName = headActor.shortName,
            groupId = headActor.groupId,
            groupName = headActor.groupName,
            dueDateEpochDay = today,
            requiresPhotoReport = true,
            minPhotoCount = 3L,
            photoItems = listOf("p1|file:///tmp/photo1.jpg|emp_head_1|Иванов|2026-11-10|55.75|Шкаф")
        ).withUpdatedVolume(50.0, 35.0, "шт")

        val previewWithPhotos = ReportDocumentGenerator.buildPreview(
            actor = headActor,
            template = template,
            allTasks = listOf(sampleTask),
            currentEpochDay = today,
            includePhotos = true
        )
        assertTrue(previewWithPhotos.includePhotos)
        assertEquals(1, previewWithPhotos.attachedPhotos.size)

        val previewWithoutPhotos = ReportDocumentGenerator.buildPreview(
            actor = headActor,
            template = template,
            allTasks = listOf(sampleTask),
            currentEpochDay = today,
            includePhotos = false
        )
        assertFalse(previewWithoutPhotos.includePhotos)
        assertTrue(previewWithoutPhotos.attachedPhotos.isEmpty())
    }

    @Test
    fun `daily report uses current date title and employee metadata encodes cleanly`() {
        val today = CorporateSeedData.BASE_EPOCH_DAY
        val dateText = ReportDocumentGenerator.formatEpochDay(today)
        val templates = CorporateSeedData.buildInitialReportTemplates(userId = "test_user_1", dateLabel = dateText)
        val firstTemplate = templates.first()

        assertEquals("Отчет за текущий день ($dateText)", firstTemplate.name)
        assertFalse(firstTemplate.name.contains("Суточный рапорт", ignoreCase = true))

        // Legacy template name normalization
        val legacyTemplate = firstTemplate.copy(name = "Суточный рапорт + План на завтра (2 листа)")
        val normalizedTitle = ReportDocumentGenerator.resolveTemplateDisplayName(legacyTemplate, today)
        assertEquals("Отчет за текущий день ($dateText)", normalizedTitle)

        // Employee metadata encoding within 100 chars for Firestore security rules
        val draftMember = com.example.data.model.SiteMemberDocument(
            id = "emp_test_1",
            memberId = "emp_test_1",
            email = "worker@site.ru",
            fullName = "Петров Пётр Петрович",
            role = "WORKER",
            position = "Электромонтажник 5 разряда",
            groupId = "grp_1",
            groupName = "Бригада №1",
            personnelNumber = "ТН-0452",
            siteName = "Участок №1",
            phone = "+79991234567"
        )
        val encodedApprovedBy = draftMember.encodeApprovedByForFirestore(approverName = "Ковалёв Д.С.")
        assertTrue(encodedApprovedBy.length <= 100)

        val decodedMember = draftMember.copy(
            personnelNumber = "",
            siteName = "",
            phone = "",
            approvedByName = encodedApprovedBy
        )
        assertEquals("ТН-0452", decodedMember.effectivePersonnelNumber)
        assertEquals("Участок №1", decodedMember.effectiveSiteName)
        assertEquals("+79991234567", decodedMember.effectivePhone)

        // Updating employee card fields overwrites previous metadata cleanly
        val updatedApprovedBy = decodedMember.encodeApprovedByForFirestore(
            approverName = "Ковалёв Д.С.",
            explicitPersonnelNumber = "ТН-0999",
            explicitSiteName = "Участок №2 — ПТО",
            explicitPhone = "+79005554433"
        )
        val editedMember = decodedMember.copy(
            fullName = "Петров Пётр Сергеевич",
            position = "Мастер участка",
            personnelNumber = "ТН-0999",
            siteName = "Участок №2 — ПТО",
            phone = "+79005554433",
            approvedByName = updatedApprovedBy
        )
        val corpEmp = editedMember.toCorporateEmployee()
        assertEquals("Петров Пётр Сергеевич", corpEmp.fullName)
        assertEquals("Мастер участка", corpEmp.position)
        assertEquals("ТН-0999", corpEmp.personnelNumber)
        assertEquals("Участок №2 — ПТО", corpEmp.siteName)
        assertEquals("+79005554433", corpEmp.phone)
    }

    @Test
    fun `checklist items specify volume per item and report outputs concrete completed volume`() {
        val today = CorporateSeedData.BASE_EPOCH_DAY
        val headActor = CorporateSeedData.findEmployee("emp_head_1")
        val template = CorporateSeedData.buildInitialReportTemplates(userId = "test_user_1").first()

        val item1 = SubtaskItem.parseFromRawLine("Монтаж лотка : 50 п.м.", index = 1)
            .copy(completedVolume = 35.0, isDone = false)
        val item2 = SubtaskItem.parseFromRawLine("Установка коробок : 20 шт", index = 2)
            .copy(completedVolume = 20.0, isDone = true)

        assertEquals(50.0, item1.effectiveTargetVolume, 0.001)
        assertEquals(35.0, item1.effectiveCompletedVolume, 0.001)
        assertEquals("п.м.", item1.effectiveUnit)
        assertEquals(70, item1.progressPercent)

        assertEquals(20.0, item2.effectiveTargetVolume, 0.001)
        assertEquals(20.0, item2.effectiveCompletedVolume, 0.001)
        assertEquals("шт", item2.effectiveUnit)
        assertEquals(100, item2.progressPercent)

        val taskWithPerItemVolumes = TaskDocument(
            id = "task_per_item_vol",
            userId = "test_user_1",
            title = "Электромонтаж кабельной трассы",
            assigneeId = headActor.id,
            assigneeName = headActor.shortName,
            groupId = headActor.groupId,
            groupName = headActor.groupName,
            dueDateEpochDay = today,
            subtaskItems = listOf(item1.encode(), item2.encode())
        )

        val breakdown = taskWithPerItemVolumes.formatChecklistBreakdownForReport()
        assertEquals(2, breakdown.size)
        assertTrue(breakdown[0].contains("Монтаж лотка: выполнено 35 из 50 п.м. (70%, ост. 15 п.м.)"))
        assertTrue(breakdown[1].contains("Установка коробок: выполнено 20 из 20 шт (100%)"))

        val preview = ReportDocumentGenerator.buildPreview(
            actor = headActor,
            template = template,
            allTasks = listOf(taskWithPerItemVolumes),
            currentEpochDay = today
        )
        val factSheet = preview.sheets.last()
        assertEquals(1, factSheet.rows.size)
        val row = factSheet.rows.first()
        assertEquals(2, row.checklistBreakdown.size)
        assertTrue(row.volumeInfo.contains("35 из 50 п.м."))
        assertTrue(row.volumeInfo.contains("20 из 20 шт"))
        assertTrue(preview.summaryText.contains("Монтаж лотка: выполнено 35 из 50 п.м."))
    }

    @Test
    fun `administrator role has full permissions and checklist strips duplicate sub_vol_main item`() {
        val admin = CorporateRole.ADMIN
        assertTrue(admin.isAdmin)
        assertTrue(admin.isAdminOrHead)
        assertTrue(admin.canAssignTo(CorporateRole.ADMIN))
        assertTrue(admin.canAssignTo(CorporateRole.HEAD))
        assertTrue(admin.canAssignTo(CorporateRole.ITR))
        assertTrue(admin.canAssignTo(CorporateRole.WORKER))
        assertTrue(admin.canApproveReportOf(CorporateRole.HEAD))
        assertTrue(admin.canApproveReportOf(CorporateRole.ITR))
        assertTrue(admin.canApproveReportOf(CorporateRole.WORKER))
        assertTrue(admin.canExportConsolidatedPersonnelReport())
        assertTrue(admin.canManageTemplatesAndGroups())

        // Verify SHA-256 hash matching for root admin without storing plaintext credentials in app
        assertTrue(SiteMemberDocument.isRootAdminEmail("m241185m@gmail.com"))
        assertFalse(SiteMemberDocument.isRootAdminEmail("other@gmail.com"))

        // Verify duplicate task-title item (sub_vol_main) is stripped and total volume equals sum of real items
        val legacyDuplicate = SubtaskItem(
            id = "sub_vol_main",
            title = "защита",
            isDone = true,
            targetVolume = 10.0,
            completedVolume = 10.0,
            unit = "шт"
        ).encode()
        val realItem1 = SubtaskItem(
            id = "sub_1",
            title = "взять мешки",
            isDone = true,
            targetVolume = 150.0,
            completedVolume = 150.0,
            unit = "шт"
        ).encode()
        val realItem2 = SubtaskItem(
            id = "sub_2",
            title = "заполнить песком",
            isDone = false,
            targetVolume = 150.0,
            completedVolume = 0.0,
            unit = "шт"
        ).encode()
        val realItem3 = SubtaskItem(
            id = "sub_3",
            title = "уложить",
            isDone = false,
            targetVolume = 150.0,
            completedVolume = 0.0,
            unit = "шт"
        ).encode()

        val task = TaskDocument(
            id = "task_zashita",
            userId = "u1",
            title = "защита",
            assigneeId = "emp_1",
            assigneeName = "Maks Star",
            dueDateEpochDay = CorporateSeedData.BASE_EPOCH_DAY,
            targetVolume = 460.0,
            completedVolume = 160.0,
            subtaskItems = listOf(legacyDuplicate, realItem1, realItem2, realItem3)
        )

        val decoded = task.decodedSubtasks()
        assertEquals(3, decoded.size)
        assertFalse(decoded.any { it.title == "защита" || it.id == "sub_vol_main" })
        assertEquals(450.0, task.effectiveTargetVolume, 0.001)
        assertEquals(150.0, task.effectiveCompletedVolume, 0.001)
    }

    @Test
    fun `canonicalMemberId isolates pending user and administrator accounts even with shared device uid`() {
        val sharedFirebaseUid = "firebase_uid_shared_device_123"
        val pendingUserId = SiteMemberDocument.canonicalMemberId(sharedFirebaseUid, "starmaksai@gmail.com")
        val adminUserId = SiteMemberDocument.canonicalMemberId(sharedFirebaseUid, "m241185m@gmail.com")

        assertEquals("usr_starmaksai_gmail_com", pendingUserId)
        assertEquals("usr_m241185m_gmail_com", adminUserId)
        assertFalse(pendingUserId == adminUserId)

        val pendingMember = SiteMemberDocument(
            id = pendingUserId,
            memberId = pendingUserId,
            email = "starmaksai@gmail.com",
            fullName = "StarMaks",
            position = "Электромеханик",
            role = "PENDING",
            approvalStatus = "PENDING",
            groupId = "grp_monolith",
            groupName = "ЛПДС \"Быково\"",
            siteName = "УЭСАиТМ"
        ).let { draft ->
            draft.copy(
                approvedByName = draft.encodeApprovedByForFirestore(
                    approverName = "Заявка",
                    explicitSiteName = "УЭСАиТМ",
                    isAdminRole = false
                )
            )
        }

        assertTrue(pendingMember.isPendingRole)
        assertFalse(pendingMember.isRejected)
        assertEquals("УЭСАиТМ", pendingMember.effectiveSiteName)
        assertEquals("ЛПДС \"Быково\"", pendingMember.groupName)
    }

    @Test
    fun `home screen persona visibility restricts worker to worker mode, itr to worker and itr, and head or admin to all`() {
        val workerModes = com.example.ui.components.PersonaUiMode.allowedModesForRole(CorporateRole.WORKER)
        assertEquals(listOf(com.example.ui.components.PersonaUiMode.WORKER), workerModes)

        val itrModes = com.example.ui.components.PersonaUiMode.allowedModesForRole(CorporateRole.ITR)
        assertEquals(
            listOf(
                com.example.ui.components.PersonaUiMode.WORKER,
                com.example.ui.components.PersonaUiMode.ITR
            ),
            itrModes
        )

        val headModes = com.example.ui.components.PersonaUiMode.allowedModesForRole(CorporateRole.HEAD)
        assertEquals(
            listOf(
                com.example.ui.components.PersonaUiMode.WORKER,
                com.example.ui.components.PersonaUiMode.ITR,
                com.example.ui.components.PersonaUiMode.DIRECTOR
            ),
            headModes
        )

        val adminModes = com.example.ui.components.PersonaUiMode.allowedModesForRole(CorporateRole.ADMIN)
        assertEquals(
            listOf(
                com.example.ui.components.PersonaUiMode.WORKER,
                com.example.ui.components.PersonaUiMode.ITR,
                com.example.ui.components.PersonaUiMode.DIRECTOR
            ),
            adminModes
        )
    }

    @Test
    fun `formatEpochDay and currentDeviceEpochDay match exact LocalDate calendar day without drift`() {
        // 20769L == 2026-11-12
        val nov12Epoch = java.time.LocalDate.of(2026, 11, 12).toEpochDay()
        assertEquals(20769L, nov12Epoch)
        assertEquals("12.11.2026", ReportDocumentGenerator.formatEpochDay(nov12Epoch))

        // 20768L == 2026-11-11
        val nov11Epoch = java.time.LocalDate.of(2026, 11, 11).toEpochDay()
        assertEquals(20768L, nov11Epoch)
        assertEquals("11.11.2026", ReportDocumentGenerator.formatEpochDay(nov11Epoch))

        // Verify currentDeviceEpochDay matches formatted LocalDate.ofEpochDay
        val todayEpoch = CorporateSeedData.currentDeviceEpochDay()
        val expectedDate = java.time.LocalDate.ofEpochDay(todayEpoch)
        val expectedText = String.format(
            java.util.Locale.US,
            "%02d.%02d.%04d",
            expectedDate.dayOfMonth,
            expectedDate.monthValue,
            expectedDate.year
        )
        assertEquals(expectedText, ReportDocumentGenerator.formatEpochDay(todayEpoch))
    }

    @Test
    fun `PhotoReportItem encode and decode preserves full Base64 JPEG payload across devices`() {
        val sampleB64 = "A".repeat(28500)
        val combinedUri = "/data/user/0/com.aistudio.ezhednevnik/files/task_photos/cam_123.jpg#b64=$sampleB64"
        val item = com.example.data.model.PhotoReportItem(
            id = "ph_test_1",
            uriString = combinedUri,
            authorId = "emp_1",
            authorName = "Плиткин Д.",
            capturedAtText = "13.11 16:16",
            gpsCoords = "Участок №1 — ПГС и Инженер",
            comment = "Фотофиксация СМР #1"
        )
        val encoded = item.encode()
        val decoded = com.example.data.model.PhotoReportItem.decode(encoded)
        assertEquals("ph_test_1", decoded.id)
        assertEquals(combinedUri, decoded.uriString)
        assertTrue(decoded.uriString.contains("#b64="))
        assertEquals(sampleB64, decoded.uriString.substringAfter("#b64="))
        assertEquals("Плиткин Д.", decoded.authorName)
        assertEquals("Фотофиксация #1", decoded.comment)

        val baseTask = com.example.data.model.TaskDocument(id = "t_rev_1", title = "Проверка ревизии")
        val revTask = baseTask.withUpdatedRevision(1700000000123L)
        assertEquals(1700000000123L, revTask.revisionTimestamp)
        assertTrue(revTask.visibleTags.none { it.startsWith("__rev__|") })
    }

    @Test
    fun `task approval hierarchy enforces ITR approves WORKER and HEAD or ADMIN approves ITR and WORKER`() {
        val workerEmp = com.example.data.model.CorporateEmployee(
            id = "emp_w1",
            fullName = "Сидоров А.В.",
            shortName = "Сидоров А.В.",
            position = "Электромонтажник",
            role = CorporateRole.WORKER,
            groupId = "grp_1",
            groupName = "ЛПДС Быково",
            siteId = "site_1",
            siteName = "УЭСАиТМ",
            supervisorId = "emp_itr1",
            phone = "+79001112233",
            email = "worker@site.ru"
        )
        val itrEmp = com.example.data.model.CorporateEmployee(
            id = "emp_itr1",
            fullName = "Плиткин Д.А.",
            shortName = "Плиткин Д.А.",
            position = "Инженер ПТО",
            role = CorporateRole.ITR,
            groupId = "grp_1",
            groupName = "ЛПДС Быково",
            siteId = "site_1",
            siteName = "УЭСАиТМ",
            supervisorId = "emp_head1",
            phone = "+79002223344",
            email = "itr@site.ru"
        )
        val headEmp = com.example.data.model.CorporateEmployee(
            id = "emp_head1",
            fullName = "Ковалёв Д.С.",
            shortName = "Ковалёв Д.С.",
            position = "Начальник участка",
            role = CorporateRole.HEAD,
            groupId = "grp_1",
            groupName = "ЛПДС Быково",
            siteId = "site_1",
            siteName = "УЭСАиТМ",
            supervisorId = null,
            phone = "+79003334455",
            email = "head@site.ru"
        )
        val adminEmp = com.example.data.model.CorporateEmployee(
            id = "emp_admin1",
            fullName = "Старшинов М.Ю.",
            shortName = "Старшинов М.Ю.",
            position = "Администратор",
            role = CorporateRole.ADMIN,
            groupId = "grp_1",
            groupName = "ЛПДС Быково",
            siteId = "site_1",
            siteName = "УЭСАиТМ",
            supervisorId = null,
            phone = "+79004445566",
            email = "admin@site.ru"
        )
        val allEmployees = listOf(workerEmp, itrEmp, headEmp, adminEmp)

        val workerTask = TaskDocument(
            id = "task_worker_1",
            userId = "u1",
            title = "Монтаж заземления",
            assigneeId = workerEmp.id,
            assigneeName = workerEmp.shortName
        )
        val itrTask = TaskDocument(
            id = "task_itr_1",
            userId = "u1",
            title = "Составить план на декабрь 2026",
            assigneeId = itrEmp.id,
            assigneeName = itrEmp.shortName
        )

        // WORKER task: approved by ITR, HEAD, ADMIN; NOT by WORKER
        assertFalse(workerTask.canBeApprovedBy(workerEmp, allEmployees))
        assertTrue(workerTask.canBeApprovedBy(itrEmp, allEmployees))
        assertTrue(workerTask.canBeApprovedBy(headEmp, allEmployees))
        assertTrue(workerTask.canBeApprovedBy(adminEmp, allEmployees))

        // ITR task: approved ONLY by HEAD or ADMIN; NOT by ITR or WORKER
        assertFalse(itrTask.canBeApprovedBy(workerEmp, allEmployees))
        assertFalse(itrTask.canBeApprovedBy(itrEmp, allEmployees))
        assertTrue(itrTask.canBeApprovedBy(headEmp, allEmployees))
        assertTrue(itrTask.canBeApprovedBy(adminEmp, allEmployees))

        // Default SiteMemberDocument has empty subdivision and site fields until selected
        val freshMember = SiteMemberDocument(id = "usr_new", memberId = "usr_new", email = "new@site.ru")
        assertEquals("", freshMember.groupName)
        assertEquals("", freshMember.effectiveSiteName)
    }

    @Test
    fun `task report notification evaluator notifies supervisor on completion with photo and suppresses self actions`() {
        val workerEmp = com.example.data.model.CorporateEmployee(
            id = "emp_w1",
            fullName = "Сидоров А.В.",
            shortName = "Сидоров А.В.",
            position = "Электромонтажник",
            role = CorporateRole.WORKER,
            groupId = "grp_1",
            groupName = "ЛПДС Быково",
            siteId = "site_1",
            siteName = "УЭСАиТМ",
            supervisorId = "emp_itr1",
            phone = "+79001112233",
            email = "worker@site.ru"
        )
        val headEmp = com.example.data.model.CorporateEmployee(
            id = "emp_head1",
            fullName = "Ковалёв Д.С.",
            shortName = "Ковалёв Д.С.",
            position = "Начальник участка",
            role = CorporateRole.HEAD,
            groupId = "grp_1",
            groupName = "ЛПДС Быково",
            siteId = "site_1",
            siteName = "УЭСАиТМ",
            supervisorId = null,
            phone = "+79003334455",
            email = "head@site.ru"
        )
        val employees = listOf(workerEmp, headEmp)

        val submittedReportTask = TaskDocument(
            id = "task_rep_notif_1",
            userId = "u1",
            title = "Монтаж шкафа",
            creatorId = headEmp.id,
            creatorName = headEmp.fullName,
            assigneeId = workerEmp.id,
            assigneeName = workerEmp.shortName,
            status = com.example.data.model.TaskStatus.ON_REVIEW.code,
            photoItems = listOf("ph1|/tmp/p1.jpg#b64=AAA|emp_w1|Сидоров А.В.|14.11 15:00|Участок|Фото шкафа"),
            historyItems = listOf(
                com.example.data.model.StatusHistoryEntry(
                    timestampText = "14.11 15:00",
                    actorName = workerEmp.shortName,
                    statusCode = com.example.data.model.TaskStatus.ON_REVIEW.code,
                    note = "Выполнено и передано на проверку"
                ).encode()
            )
        )

        // Supervisor receives notification with photo count on ON_REVIEW
        val headEval = com.example.ui.viewmodel.EzhednevnikViewModel.computePendingTaskNotifications(
            tasks = listOf(submittedReportTask),
            currentEmployee = headEmp,
            employees = employees,
            seenEventKeys = emptySet(),
            isBaselineReady = false
        )
        assertEquals(1, headEval.eventsToNotify.size)
        assertEquals(
            com.example.ui.viewmodel.EzhednevnikViewModel.TaskNotificationType.REPORT_SUBMITTED,
            headEval.eventsToNotify.first().type
        )
        assertTrue(headEval.eventsToNotify.first().title.contains("фото", ignoreCase = true))
        assertTrue(headEval.eventsToNotify.first().body.contains("Фото: 1 шт."))

        // Re-running with seen keys suppresses duplicate notification
        val headRepeatEval = com.example.ui.viewmodel.EzhednevnikViewModel.computePendingTaskNotifications(
            tasks = listOf(submittedReportTask),
            currentEmployee = headEmp,
            employees = employees,
            seenEventKeys = headEval.keysToMarkSeen,
            isBaselineReady = true
        )
        assertTrue(headRepeatEval.eventsToNotify.isEmpty())

        // Worker who submitted it does NOT get self-notification
        val workerSelfEval = com.example.ui.viewmodel.EzhednevnikViewModel.computePendingTaskNotifications(
            tasks = listOf(submittedReportTask),
            currentEmployee = workerEmp,
            employees = employees,
            seenEventKeys = emptySet(),
            isBaselineReady = true
        )
        assertTrue(workerSelfEval.eventsToNotify.isEmpty())

        // Supervisor rejects -> Worker receives rejection notification with reason
        val rejectedTask = submittedReportTask.copy(
            status = com.example.data.model.TaskStatus.REJECTED.code,
            rejectionReason = "Не видно маркировку кабеля",
            historyItems = submittedReportTask.historyItems + com.example.data.model.StatusHistoryEntry(
                timestampText = "14.11 15:10",
                actorName = headEmp.shortName,
                statusCode = com.example.data.model.TaskStatus.REJECTED.code,
                note = "Отклонено: Не видно маркировку кабеля"
            ).encode()
        )
        val workerRejectEval = com.example.ui.viewmodel.EzhednevnikViewModel.computePendingTaskNotifications(
            tasks = listOf(rejectedTask),
            currentEmployee = workerEmp,
            employees = employees,
            seenEventKeys = emptySet(),
            isBaselineReady = true
        )
        assertEquals(1, workerRejectEval.eventsToNotify.size)
        assertEquals(
            com.example.ui.viewmodel.EzhednevnikViewModel.TaskNotificationType.TASK_REJECTED,
            workerRejectEval.eventsToNotify.first().type
        )
        assertTrue(workerRejectEval.eventsToNotify.first().body.contains("Не видно маркировку кабеля"))
    }

    @Test
    fun `role updates clear sticky admin flag and report formatting matches org subdivision site and signature rules`() {
        // 1. No "прораб" anywhere in role or persona titles
        CorporateRole.entries.forEach { role ->
            assertFalse(role.titleRu.contains("прораб", ignoreCase = true))
            assertFalse(role.shortTitleRu.contains("прораб", ignoreCase = true))
            assertFalse(role.badgeDescription.contains("прораб", ignoreCase = true))
        }
        com.example.ui.components.PersonaUiMode.entries.forEach { mode ->
            assertFalse(mode.titleRu.contains("прораб", ignoreCase = true))
            assertFalse(mode.subtitleRu.contains("прораб", ignoreCase = true))
        }

        // 2. Role change WORKER <-> ITR and demoting ADMIN clears sticky adm=1
        val adminMember = SiteMemberDocument(
            id = "usr_smu",
            memberId = "usr_smu",
            email = "smu@site.ru",
            fullName = "СМЮ",
            position = "Электромеханик по САиПТО",
            role = CorporateRole.ADMIN.code,
            approvalStatus = "APPROVED",
            groupId = "grp_1",
            groupName = "ЛПДС \"Быково\"",
            siteName = "УЭСАиТМ"
        ).let { m ->
            m.copy(approvedByName = m.encodeApprovedByForFirestore(approverName = "Админ", isAdminRole = true))
        }
        assertEquals(CorporateRole.ADMIN, adminMember.parsedRoleOrNull)

        val demotedToItr = adminMember.copy(
            role = CorporateRole.ITR.code,
            approvedByName = adminMember.copy(role = CorporateRole.ITR.code)
                .encodeApprovedByForFirestore(approverName = "Админ")
        )
        assertEquals(CorporateRole.ITR, demotedToItr.parsedRoleOrNull)

        val demotedToWorker = demotedToItr.copy(
            role = CorporateRole.WORKER.code,
            approvedByName = demotedToItr.copy(role = CorporateRole.WORKER.code)
                .encodeApprovedByForFirestore(approverName = "Админ")
        )
        assertEquals(CorporateRole.WORKER, demotedToWorker.parsedRoleOrNull)

        // 3. Report org header and prepared-by signature format
        val orgHeader = ReportDocumentGenerator.formatOrgHeader(
            organizationName = "ООО «Транснефть-Балтика»",
            subdivisionName = "ЛПДС \"Быково\"",
            siteName = "УЭСАиТМ"
        )
        assertEquals("ООО «Транснефть-Балтика», ЛПДС \"Быково\", УЭСАиТМ", orgHeader)

        val itrActor = com.example.data.model.CorporateEmployee(
            id = "emp_itr",
            fullName = "Плиткин Д.А.",
            shortName = "Плиткин Д.А.",
            position = "Инженер по КИПиА",
            role = CorporateRole.ITR,
            groupId = "grp_1",
            groupName = "ЛПДС \"Быково\"",
            siteId = "site_1",
            siteName = "УЭСАиТМ",
            supervisorId = null,
            phone = "",
            email = "itr@site.ru"
        )
        val sig = ReportDocumentGenerator.formatPreparedBySignature(itrActor)
        assertEquals("____________________ (подпись), Плиткин Д.А., Инженер по КИПиА, Участок: УЭСАиТМ", sig)

        // 4. Aspect-fit bounds preserve aspect ratio for both portrait and landscape photos
        val portraitFit = ReportDocumentGenerator.computeAspectFitBounds(
            srcWidth = 900,
            srcHeight = 1600,
            frameLeft = 0f,
            frameTop = 0f,
            frameRight = 240f,
            frameBottom = 160f
        )
        val fitWidthP = portraitFit.right - portraitFit.left
        val fitHeightP = portraitFit.bottom - portraitFit.top
        assertEquals(160f, fitHeightP, 0.5f)
        assertEquals(90f, fitWidthP, 0.5f)

        val landscapeFit = ReportDocumentGenerator.computeAspectFitBounds(
            srcWidth = 1600,
            srcHeight = 900,
            frameLeft = 0f,
            frameTop = 0f,
            frameRight = 240f,
            frameBottom = 180f
        )
        val fitWidthL = landscapeFit.right - landscapeFit.left
        val fitHeightL = landscapeFit.bottom - landscapeFit.top
        assertEquals(240f, fitWidthL, 0.5f)
        assertEquals(135f, fitHeightL, 0.5f)

        // 5. ITR relevance includes workers on ITR's site and excludes workers on a different site
        val workerSameSite = com.example.data.model.CorporateEmployee(
            id = "emp_w_same",
            fullName = "СМЮ",
            shortName = "СМЮ",
            position = "Электромеханик",
            role = CorporateRole.WORKER,
            groupId = "grp_other",
            groupName = "Другое подразделение",
            siteId = "site_1",
            siteName = "УЭСАиТМ",
            supervisorId = null,
            phone = "",
            email = "w1@site.ru"
        )
        val workerOtherSite = workerSameSite.copy(
            id = "emp_w_other",
            fullName = "Другой Р.",
            shortName = "Другой Р.",
            siteName = "НПС-2"
        )
        val allEmps = listOf(itrActor, workerSameSite, workerOtherSite)

        val taskWorkerSameSite = TaskDocument(
            id = "t_same",
            title = "Монтаж датчика",
            assigneeId = workerSameSite.id,
            assigneeName = workerSameSite.fullName,
            groupId = workerSameSite.groupId,
            groupName = workerSameSite.groupName,
            geoLocation = "УЭСАиТМ"
        )
        val taskWorkerOtherSite = TaskDocument(
            id = "t_other",
            title = "Монтаж на другом объекте",
            assigneeId = workerOtherSite.id,
            assigneeName = workerOtherSite.fullName,
            groupId = workerOtherSite.groupId,
            groupName = workerOtherSite.groupName,
            geoLocation = "НПС-2"
        )
        assertTrue(taskWorkerSameSite.isRelevantToItr(itrActor, allEmps))
        assertFalse(taskWorkerOtherSite.isRelevantToItr(itrActor, allEmps))
    }

    @Test
    fun `admin sees all cross-subdivision tasks while site matching and mixed ITR-Worker tasks behave accurately`() {
        val today = CorporateSeedData.BASE_EPOCH_DAY
        val admin = CorporateEmployee(
            id = "emp_admin_1",
            fullName = "Старшинов Максим Юрьевич",
            shortName = "Старшинов М.Ю.",
            position = "Начальник",
            role = CorporateRole.ADMIN,
            groupId = "grp_uesaitm",
            groupName = "УЭСАиТМ",
            siteId = "site_bykovo",
            siteName = "ЛПДС \"Быково\"",
            supervisorId = null,
            phone = "",
            email = "admin@company.ru"
        )
        val itr = CorporateEmployee(
            id = "emp_itr_bykovo",
            fullName = "Плиткин Д.А.",
            shortName = "Плиткин Д.А.",
            position = "Инженер по КИПиА",
            role = CorporateRole.ITR,
            groupId = "grp_uesaitm",
            groupName = "УЭСАиТМ",
            siteId = "site_bykovo",
            siteName = "ЛПДС \"Быково\"",
            supervisorId = admin.id,
            phone = "",
            email = "plitkin@company.ru"
        )
        val otherItr = CorporateEmployee(
            id = "emp_itr_other_sub",
            fullName = "Соколов В.П.",
            shortName = "Соколов В.П.",
            position = "Инженер-энергетик",
            role = CorporateRole.ITR,
            groupId = "grp_energo",
            groupName = "Энергослужба",
            siteId = "site_bykovo",
            siteName = "ЛПДС \"Быково\"",
            supervisorId = admin.id,
            phone = "",
            email = "sokolov@company.ru"
        )
        val workerBykovo = CorporateEmployee(
            id = "emp_worker_bykovo",
            fullName = "СМЮ",
            shortName = "СМЮ",
            position = "Электромеханик",
            role = CorporateRole.WORKER,
            groupId = "grp_uesaitm",
            groupName = "УЭСАиТМ",
            siteId = "site_bykovo",
            siteName = "ЛПДС \"Быково\"",
            supervisorId = itr.id,
            phone = "",
            email = "smyu@company.ru"
        )
        val workerEnergo = workerBykovo.copy(
            id = "emp_worker_energo",
            fullName = "Волков А.С.",
            shortName = "Волков А.С.",
            groupId = "grp_energo",
            groupName = "Энергослужба",
            siteId = "site_bykovo",
            siteName = "ЛПДС \"Быково\"",
            supervisorId = otherItr.id,
            email = "volkov@company.ru"
        )
        val workerNps2 = workerBykovo.copy(
            id = "emp_worker_nps2",
            fullName = "Петров К.Н.",
            shortName = "Петров К.Н.",
            groupId = "grp_remote",
            groupName = "Линейный участок",
            siteId = "site_nps2",
            siteName = "НПС-2",
            supervisorId = null,
            email = "petrov@company.ru"
        )
        val allEmployees = listOf(admin, itr, otherItr, workerBykovo, workerEnergo, workerNps2)

        // 1. Task has custom zone geoLocation="УЭСАиТМ" while assignee belongs to site "ЛПДС \"Быково\""
        val taskWithZoneGeo = TaskDocument(
            id = "t_zone_geo",
            title = "Проверка датчиков давления",
            creatorId = itr.id,
            creatorName = itr.shortName,
            assigneeId = workerBykovo.id,
            assigneeName = workerBykovo.shortName,
            groupId = "grp_uesaitm",
            groupName = "УЭСАиТМ",
            siteId = "site_bykovo",
            geoLocation = "УЭСАиТМ",
            status = TaskStatus.IN_PROGRESS.code,
            dueDateEpochDay = today
        )
        val taskOtherSite = TaskDocument(
            id = "t_nps2",
            title = "Ревизия задвижки на НПС-2",
            creatorId = admin.id,
            creatorName = admin.shortName,
            assigneeId = workerNps2.id,
            assigneeName = workerNps2.shortName,
            groupId = "grp_remote",
            groupName = "Линейный участок",
            siteId = "site_nps2",
            geoLocation = "НПС-2",
            status = TaskStatus.NEW.code,
            dueDateEpochDay = today
        )
        assertTrue(taskWithZoneGeo.belongsToSite("ЛПДС \"Быково\"", "site_bykovo", allEmployees))
        assertFalse(taskOtherSite.belongsToSite("ЛПДС \"Быково\"", "site_bykovo", allEmployees))

        // 2. Mixed multi-assignee (ITR + WORKER) created by another engineer in Энергослужба
        val taskMixedAssignees = TaskDocument(
            id = "t_mixed",
            title = "Совместная наладка щита",
            creatorId = otherItr.id,
            creatorName = otherItr.shortName,
            groupId = "grp_energo",
            groupName = "Энергослужба",
            siteId = "site_bykovo",
            geoLocation = "Щитовая №2",
            status = TaskStatus.ON_REVIEW.code,
            dueDateEpochDay = today
        ).withUpdatedAssignees(
            ids = listOf(otherItr.id, workerEnergo.id),
            names = listOf(otherItr.shortName, workerEnergo.shortName)
        )
        assertTrue(taskMixedAssignees.hasWorkerAssignee(allEmployees, itr))
        assertFalse(taskMixedAssignees.canBeApprovedBy(itr, allEmployees))
        assertTrue(taskMixedAssignees.canBeApprovedBy(admin, allEmployees))
        assertTrue(taskMixedAssignees.isRelevantToItr(itr, allEmployees))

        val allTasks = listOf(taskWithZoneGeo, taskMixedAssignees, taskOtherSite)

        // 3. Statistics for ADMIN include all 3 tasks and both custom subdivisions
        val adminStats = EzhednevnikRepository.computeRoleScopedStatistics(
            actor = admin,
            allTasks = allTasks,
            customEmployees = allEmployees
        )
        assertEquals(3, adminStats.totalAssigned)
        assertTrue(adminStats.groupStats.any { it.groupName == "УЭСАиТМ" && it.totalTasks == 1 })
        assertTrue(adminStats.groupStats.any { it.groupName == "Энергослужба" && it.totalTasks == 1 })
        assertTrue(adminStats.groupStats.any { it.groupName == "Линейный участок" && it.totalTasks == 1 })

        // WORKER sees only their own assigned tasks
        val workerStats = EzhednevnikRepository.computeRoleScopedStatistics(
            actor = workerBykovo,
            allTasks = allTasks,
            customEmployees = allEmployees
        )
        assertEquals(1, workerStats.totalAssigned)

        // ITR sees tasks on their site (taskWithZoneGeo + taskMixedAssignees), excluding НПС-2
        val itrStats = EzhednevnikRepository.computeRoleScopedStatistics(
            actor = itr,
            allTasks = allTasks,
            customEmployees = allEmployees
        )
        assertEquals(2, itrStats.totalAssigned)

        // 4. Consolidated report preview for ADMIN includes all 3 cross-subdivision tasks
        val template = CorporateSeedData.buildInitialReportTemplates(userId = admin.id).first()
        val adminPreview = ReportDocumentGenerator.buildPreview(
            actor = admin,
            template = template,
            allTasks = allTasks,
            currentEpochDay = today,
            overrideLayout = SheetLayout.SINGLE_SHEET,
            overrideFormat = ExportFormat.PDF,
            allEmployees = allEmployees
        )
        val allPreviewRows = adminPreview.sheets.flatMap { it.sections }.flatMap { it.rows }
        assertTrue(allPreviewRows.any { it.taskTitle.contains("Проверка датчиков") })
        assertTrue(allPreviewRows.any { it.taskTitle.contains("Совместная наладка щита") })
        assertTrue(allPreviewRows.any { it.taskTitle.contains("Ревизия задвижки на НПС-2") })
    }

    @Test
    fun `multi-assignee subdivision matching, live name resolution, and employee update propagation`() {
        val plitkin = CorporateEmployee(
            id = "usr_plitkin",
            fullName = "Плиткин Д.А.",
            shortName = "Плиткин Д.А.",
            position = "инженер по КИПиА",
            role = CorporateRole.ITR,
            groupId = "grp_uesaitm",
            groupName = "УЭСАиТМ",
            siteId = "site_bykovo",
            siteName = "ЛПДС \"Быково\"",
            supervisorId = null,
            phone = "",
            email = "plitkin@company.ru"
        )
        val smyuOld = CorporateEmployee(
            id = "usr_starmaks",
            fullName = "StarMaks",
            shortName = "StarMaks",
            position = "Электромеханик по САиПТО",
            role = CorporateRole.ITR,
            groupId = "grp_default",
            groupName = "Основное подразделение",
            siteId = "site_bykovo",
            siteName = "ЛПДС \"Быково\"",
            supervisorId = null,
            phone = "",
            email = "starmaks@company.ru"
        )
        val smyuUpdated = smyuOld.copy(
            fullName = "СМЮ",
            shortName = "СМЮ",
            groupId = "grp_uesaitm",
            groupName = "УЭСАиТМ"
        )
        val otherSubdivisionWorker = CorporateEmployee(
            id = "usr_other",
            fullName = "Иванов И.И.",
            shortName = "Иванов И.И.",
            position = "Слесарь",
            role = CorporateRole.WORKER,
            groupId = "grp_energo",
            groupName = "Энергослужба",
            siteId = "site_bykovo",
            siteName = "ЛПДС \"Быково\"",
            supervisorId = null,
            phone = "",
            email = "ivanov@company.ru"
        )
        val updatedEmployees = listOf(plitkin, smyuUpdated, otherSubdivisionWorker)

        // Task created when assignee had outdated subdivision & old name StarMaks
        val multiAssigneeTask = TaskDocument(
            id = "task_bolgarka",
            title = "взять на работу 150-ю болгарку",
            creatorId = smyuOld.id,
            creatorName = smyuOld.fullName,
            groupId = "grp_default",
            groupName = "Основное подразделение",
            siteId = "site_bykovo",
            geoLocation = "ЛПДС \"Быково\""
        ).withUpdatedAssignees(
            ids = listOf(plitkin.id, smyuOld.id),
            names = listOf(plitkin.fullName, smyuOld.fullName)
        )

        // 1. Live display name resolves stale StarMaks -> СМЮ
        assertEquals("Плиткин Д.А., СМЮ", multiAssigneeTask.resolveDisplayAssigneeName(updatedEmployees))
        assertEquals(listOf("Плиткин Д.А.", "СМЮ"), multiAssigneeTask.resolveDisplayAssigneeNames(updatedEmployees))

        // 2. Subdivision matching finds the task in Admin/ITR brigade "УЭСАиТМ" via current employee membership
        assertFalse(multiAssigneeTask.belongsToSubdivision("УЭСАиТМ", "grp_uesaitm"))
        assertTrue(multiAssigneeTask.belongsToSubdivision("УЭСАиТМ", "grp_uesaitm", updatedEmployees))
        assertFalse(multiAssigneeTask.belongsToSubdivision("Энергослужба", "grp_energo", updatedEmployees))

        // 3. Employee update propagation updates secondary assignee & creator inside __asgn__ and fields
        val propagatedTask = multiAssigneeTask.withReplacedEmployeeDetails(
            oldEmployee = smyuOld,
            newMemberKey = smyuUpdated.id,
            newFullName = "СМЮ",
            newGroupId = "grp_uesaitm",
            newGroupName = "УЭСАиТМ"
        )
        assertNotNull(propagatedTask)
        assertEquals("Плиткин Д.А., СМЮ", propagatedTask!!.assigneeName)
        assertEquals(listOf(plitkin.id, smyuUpdated.id), propagatedTask.allAssigneeIds)
        assertEquals(listOf("Плиткин Д.А.", "СМЮ"), propagatedTask.allAssigneeNames)
        assertEquals("СМЮ", propagatedTask.creatorName)
        assertTrue(propagatedTask.revisionTimestamp > 0L)
    }

    @Test
    fun `recurrence rules generate accurate calendar epoch days and preserve visible tags`() {
        // 2026-11-16 is a Monday
        val mondayEpoch = LocalDate.of(2026, 11, 16).toEpochDay()
        val fridayEpoch = LocalDate.of(2026, 11, 20).toEpochDay()
        val saturdayEpoch = LocalDate.of(2026, 11, 21).toEpochDay()

        assertEquals(listOf(mondayEpoch), RecurrenceRule.NONE.generateOccurrenceEpochDays(mondayEpoch))

        val dailyDays = RecurrenceRule.DAILY.generateOccurrenceEpochDays(mondayEpoch)
        assertEquals(30, dailyDays.size)
        assertEquals(mondayEpoch, dailyDays.first())
        assertEquals(mondayEpoch + 29L, dailyDays.last())

        val weekdayDays = RecurrenceRule.WEEKDAYS.generateOccurrenceEpochDays(fridayEpoch)
        assertTrue(weekdayDays.isNotEmpty())
        assertEquals(fridayEpoch, weekdayDays.first())
        assertFalse(weekdayDays.any { day ->
            val dow = LocalDate.ofEpochDay(day).dayOfWeek
            dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY
        })
        assertEquals(mondayEpoch + 7L, RecurrenceRule.WEEKDAYS.nextOccurrenceEpochDay(fridayEpoch))
        assertEquals(mondayEpoch + 7L, RecurrenceRule.WEEKDAYS.nextOccurrenceEpochDay(saturdayEpoch))

        val fromSaturdayWeekdays = RecurrenceRule.WEEKDAYS.generateOccurrenceEpochDays(saturdayEpoch)
        assertEquals(mondayEpoch + 7L, fromSaturdayWeekdays.first())

        val weeklyDays = RecurrenceRule.WEEKLY.generateOccurrenceEpochDays(mondayEpoch)
        assertEquals(12, weeklyDays.size)
        assertEquals(mondayEpoch + 7L, weeklyDays[1])
        assertEquals(mondayEpoch + 77L, weeklyDays.last())

        val monthlyDays = RecurrenceRule.MONTHLY.generateOccurrenceEpochDays(mondayEpoch)
        assertEquals(6, monthlyDays.size)
        assertEquals(LocalDate.of(2026, 12, 16).toEpochDay(), monthlyDays[1])
        assertEquals(LocalDate.of(2027, 4, 16).toEpochDay(), monthlyDays.last())

        val taggedTask = TaskDocument(
            id = "task_series_1",
            title = "Планёрка в 16:00",
            tags = listOf("__asgn__|emp_1|Иванов", "Кирпичная кладка")
        ).withRecurrenceSeries(seriesId = "ser_abc123", rule = RecurrenceRule.WEEKDAYS)

        assertEquals("ser_abc123", taggedTask.recurrenceSeriesId)
        assertEquals(RecurrenceRule.WEEKDAYS, taggedTask.recurrence)
        assertEquals(listOf("Кирпичная кладка"), taggedTask.visibleTags)
        assertTrue(taggedTask.tags.any { it.startsWith("__asgn__|") })
    }

    @Test
    fun `task edit and delete permissions allow creator and ADMIN while restricting others and archived`() {
        val admin = CorporateEmployee(
            id = "usr_admin",
            fullName = "Старшинов М.Ю.",
            shortName = "Старшинов М.Ю.",
            position = "Администратор",
            role = CorporateRole.ADMIN,
            groupId = "grp_uesaitm",
            groupName = "УЭСАиТМ",
            siteId = "site_bykovo",
            siteName = "ЛПДС Быково",
            supervisorId = null,
            phone = "",
            email = "admin@company.ru"
        )
        val creatorItr = CorporateEmployee(
            id = "emp_itr_creator",
            fullName = "Плиткин Д.А.",
            shortName = "Плиткин Д.А.",
            position = "Инженер",
            role = CorporateRole.ITR,
            groupId = "grp_uesaitm",
            groupName = "УЭСАиТМ",
            siteId = "site_bykovo",
            siteName = "ЛПДС Быково",
            supervisorId = null,
            phone = "",
            email = "plitkin@company.ru"
        )
        val creatorItrCanonicalAlias = creatorItr.copy(id = "usr_plitkin_canonical")
        val otherItr = creatorItr.copy(
            id = "emp_itr_other",
            fullName = "Другой ИТР",
            shortName = "Другой ИТР",
            email = "other_itr@company.ru"
        )
        val otherHead = creatorItr.copy(
            id = "emp_head_other",
            fullName = "Руководитель",
            shortName = "Руководитель",
            role = CorporateRole.HEAD,
            email = "head@company.ru"
        )
        val assignedWorker = creatorItr.copy(
            id = "emp_worker",
            fullName = "Рабочий",
            shortName = "Рабочий",
            role = CorporateRole.WORKER,
            email = "worker@company.ru"
        )

        val task = TaskDocument(
            id = "task_edit_perm",
            title = "Проверка оборудования",
            creatorId = creatorItr.id,
            creatorName = creatorItr.fullName,
            assigneeId = assignedWorker.id,
            assigneeName = assignedWorker.fullName,
            status = TaskStatus.IN_PROGRESS.name
        )

        assertTrue(task.canBeEditedBy(admin))
        assertTrue(task.canBeDeletedBy(admin))
        assertTrue(task.canBeEditedBy(creatorItr))
        assertTrue(task.canBeDeletedBy(creatorItr))
        assertTrue(task.canBeEditedBy(creatorItrCanonicalAlias))

        assertFalse(task.canBeEditedBy(otherItr))
        assertFalse(task.canBeDeletedBy(otherItr))
        assertFalse(task.canBeEditedBy(otherHead))
        assertFalse(task.canBeDeletedBy(otherHead))
        assertFalse(task.canBeEditedBy(assignedWorker))
        assertFalse(task.canBeDeletedBy(assignedWorker))

        val archivedTask = task.copy(status = TaskStatus.ARCHIVED.name)
        assertFalse(archivedTask.canBeEditedBy(admin))
        assertFalse(archivedTask.canBeEditedBy(creatorItr))
        assertTrue(archivedTask.canBeDeletedBy(admin))
        assertTrue(archivedTask.canBeDeletedBy(creatorItr))
    }

    @Test
    fun `rpg xp calculation covers all 5 priorities including EMERGENCY, multipliers, idempotency and priority lock`() {
        val worker = CorporateSeedData.findEmployee("emp_worker_1")
        val itr = CorporateSeedData.findEmployee("emp_itr_1")
        val today = CorporateSeedData.BASE_EPOCH_DAY
        val config = GamificationConfig()

        // 1. All 5 priority base XP scale monotonically up to EMERGENCY (50)
        assertEquals(5, config.ruleFor(TaskPriority.LOW).baseXp)
        assertEquals(10, config.ruleFor(TaskPriority.MEDIUM).baseXp)
        assertEquals(20, config.ruleFor(TaskPriority.HIGH).baseXp)
        assertEquals(35, config.ruleFor(TaskPriority.CRITICAL).baseXp)
        assertEquals(50, config.ruleFor(TaskPriority.EMERGENCY).baseXp)

        // 2. Floor at 0 when massive penalty applied
        val zeroFloorProfile = EmployeeRpgProfile(employeeId = worker.id)
        val harshConfig = config.copy(overduePenaltyRate = 0.80, rejectedPenaltyRate = 0.50)
        val (flooredXp, _) = RpgProgressionEngine.calculateTaskXpBreakdown(
            priority = TaskPriority.LOW,
            config = harshConfig,
            profile = zeroFloorProfile,
            isOnTime = false,
            isOverdue = true,
            wasRejectedBefore = true
        )
        assertEquals(0, flooredXp)

        // 3. Priority-at-completion lock prevents post-completion tampering
        val emergencyTask = TaskDocument(
            id = "task_emerg_lock",
            title = "Ликвидация прорыва",
            creatorId = itr.id,
            creatorName = itr.shortName,
            assigneeId = worker.id,
            assigneeName = worker.shortName,
            priority = TaskPriority.EMERGENCY.code,
            dueDateEpochDay = today,
            requiresPhotoReport = true,
            photoItems = listOf("ph1|/tmp/p.jpg|${worker.id}|Иванов|12:00|Участок|Фото")
        ).withLockedCompletionPriority()

        assertEquals(TaskPriority.EMERGENCY, emergencyTask.lockedPriorityAtCompletion)
        assertTrue(emergencyTask.visibleTags.none { it.startsWith("__xplock__|") })

        // Even if someone tries to mutate task.priority to LOW after completion, lockedPriorityAtCompletion preserves EMERGENCY
        val tamperedTask = emergencyTask.copy(priority = TaskPriority.LOW.code)
        val approvalRes = RpgProgressionEngine.recordTaskCompletionOrApproval(
            state = RpgWorkspaceState(config = config),
            task = tamperedTask,
            assignees = listOf(worker),
            supervisor = itr,
            currentEpochDay = today,
            isApprovalStep = true
        )
        val workerProf = approvalRes.state.profiles.getValue(worker.id)
        // Expected includes base(50+20)=70 * (1+0.2+0.1)=91 + ach_first_approved(15) + ach_emergency_hero(65) = 171
        assertTrue(workerProf.totalXp >= 91)
        assertTrue(approvalRes.unlockedAchievementIds.contains("ach_emergency_hero"))

        // 4. Idempotency: calling again with same task+type+employee must not double-grant XP
        val repeatRes = RpgProgressionEngine.recordTaskCompletionOrApproval(
            state = approvalRes.state,
            task = tamperedTask,
            assignees = listOf(worker),
            supervisor = itr,
            currentEpochDay = today,
            isApprovalStep = true
        )
        assertEquals(workerProf.totalXp, repeatRes.state.profiles.getValue(worker.id).totalXp)
    }

    @Test
    fun `rpg cancellation rollback, rejection penalty, carryover streak risk and recovery work end to end`() {
        val worker = CorporateSeedData.findEmployee("emp_worker_1")
        val itr = CorporateSeedData.findEmployee("emp_itr_1")
        val today = CorporateSeedData.BASE_EPOCH_DAY

        val task = TaskDocument(
            id = "task_rb_1",
            title = "Монтаж задвижки",
            creatorId = itr.id,
            creatorName = itr.shortName,
            assigneeId = worker.id,
            assigneeName = worker.shortName,
            priority = TaskPriority.HIGH.code,
            dueDateEpochDay = today
        )

        // Complete task -> gains XP & quest count
        val completeRes = RpgProgressionEngine.recordTaskCompletionOrApproval(
            state = RpgWorkspaceState(),
            task = task,
            assignees = listOf(worker),
            supervisor = null,
            currentEpochDay = today,
            isApprovalStep = false
        )
        val xpAfterComplete = completeRes.state.profiles.getValue(worker.id).totalXp
        assertTrue(xpAfterComplete > 0)
        assertTrue(completeRes.state.profiles.getValue(worker.id).questProgress.any { task.id in it.countedTaskIds })

        // Cancel task -> rolls back completion XP and removes task from unrewarded quest progress
        val cancelRes = RpgProgressionEngine.rollbackTaskOnCancellationOrDelete(
            state = completeRes.state,
            task = task,
            actor = itr,
            reason = "Отмена наряда"
        )
        val rolledBackTx = cancelRes.state.transactions.first { it.taskId == task.id && it.type.code == "TASK_COMPLETION" }
        assertTrue(rolledBackTx.isRolledBack)
        assertFalse(cancelRes.state.profiles.getValue(worker.id).questProgress.any { task.id in it.countedTaskIds })

        // Carryover puts active streak at risk & reduces reputation by 2, recovery token clears risk
        var stateWithStreak = cancelRes.state.copy(
            profiles = mapOf(
                worker.id to cancelRes.state.profiles.getValue(worker.id).copy(
                    streak = Streak(currentDays = 6, bestDays = 6, lastActivityEpochDay = today - 1L, isAtRisk = false),
                    reputation = com.example.domain.rpg.Reputation(score = 60)
                )
            )
        )
        stateWithStreak = RpgProgressionEngine.processCarryoverPenalties(
            state = stateWithStreak,
            movedTasks = listOf(task),
            employees = listOf(worker, itr),
            currentEpochDay = today
        )
        val riskedProf = stateWithStreak.profiles.getValue(worker.id)
        assertTrue(riskedProf.streak.isAtRisk)
        assertEquals(58, riskedProf.reputation.score)

        val recoveredState = RpgProgressionEngine.useStreakRecovery(stateWithStreak, worker, today).getOrThrow()
        val recoveredProf = recoveredState.profiles.getValue(worker.id)
        assertFalse(recoveredProf.streak.isAtRisk)
        assertEquals(0, recoveredProf.inventory.streakRecoveryTokens)
    }

    @Test
    fun `rpg skill prerequisites, seasonal reset, prestige activation, privacy and manual RBAC enforcement`() {
        val worker = CorporateSeedData.findEmployee("emp_worker_1")
        val itr = CorporateSeedData.findEmployee("emp_itr_1")
        val head = CorporateSeedData.findEmployee("emp_head_1")

        // 1. Prerequisite & role enforcement in skill tree
        var state = RpgWorkspaceState(
            profiles = mapOf(
                worker.id to EmployeeRpgProfile(employeeId = worker.id, skillPointsAvailable = 5, totalXp = 300)
            )
        )
        // Worker cannot unlock tier-2 skill before tier-1 prerequisite
        assertTrue(RpgProgressionEngine.upgradeSkill(state, worker, "sk_prof_emergency").isFailure)
        // Worker cannot unlock ITR-only Leadership skill
        assertTrue(RpgProgressionEngine.upgradeSkill(state, worker, "sk_lead_review").isFailure)
        // Unlock tier-1 then tier-2 works
        state = RpgProgressionEngine.upgradeSkill(state, worker, "sk_prof_speed").getOrThrow()
        state = RpgProgressionEngine.upgradeSkill(state, worker, "sk_prof_emergency").getOrThrow()
        assertEquals(3, state.profiles.getValue(worker.id).skillPointsAvailable)
        assertEquals(2, state.profiles.getValue(worker.id).skillPointsSpent)

        // 2. Seasonal reset refunds (spent - penalty) once per season
        state = RpgProgressionEngine.resetSkillsForSeason(state, worker).getOrThrow()
        assertEquals(3 + (2 - state.config.seasonalResetSpPenalty), state.profiles.getValue(worker.id).skillPointsAvailable)
        assertTrue(RpgProgressionEngine.resetSkillsForSeason(state, worker).isFailure)

        // 3. Manual adjustment RBAC: WORKER & ITR denied; HEAD allowed with non-blank reason
        assertTrue(RpgProgressionEngine.adjustManually(state, worker, worker, 50, 5, "Самопремия").isFailure)
        assertTrue(RpgProgressionEngine.adjustManually(state, itr, worker, 50, 5, "Бонус").isFailure)
        assertTrue(RpgProgressionEngine.adjustManually(state, head, worker, 50, 5, "   ").isFailure)
        state = RpgProgressionEngine.adjustManually(state, head, worker, 100, 10, "Премия за безаварийность").getOrThrow()
        assertEquals(400, state.profiles.getValue(worker.id).totalXp)

        // 4. Prestige requires max level; resets XP to 0 and grants permanent bonus
        val maxXp = state.config.xpRequiredForLevel(state.config.maxLevel)
        state = state.copy(
            profiles = state.profiles + (
                worker.id to state.profiles.getValue(worker.id).copy(totalXp = maxXp)
            )
        )
        state = RpgProgressionEngine.activatePrestige(state, worker).getOrThrow()
        val prestigeProf = state.profiles.getValue(worker.id)
        assertEquals(1, prestigeProf.prestige.prestigeLevel)
        assertEquals(0.05, prestigeProf.prestige.permanentXpMultiplierBonus, 0.001)
        assertTrue(prestigeProf.achievements.any { it.achievementId == "ach_prestige_1" })
        assertEquals(150, prestigeProf.totalXp)
        assertTrue(prestigeProf.totalXp < maxXp)

        // 5. Leaderboard privacy hides HIDDEN worker from peers while HEAD/ADMIN still sees
        state = RpgProgressionEngine.setLeaderboardPrivacy(state, worker.id, LeaderboardPrivacy.HIDDEN)
        val boardForItr = RpgProgressionEngine.buildLeaderboard(state, itr, listOf(worker, itr, head))
        val boardForHead = RpgProgressionEngine.buildLeaderboard(state, head, listOf(worker, itr, head))
        assertFalse(boardForItr.any { it.employeeId == worker.id })
        assertTrue(boardForHead.any { it.employeeId == worker.id })
    }

    @Test
    fun `priority distribution and gamification summary reports generate accurate previews`() {
        val today = CorporateSeedData.BASE_EPOCH_DAY
        val head = CorporateSeedData.findEmployee("emp_head_1")
        val worker = CorporateSeedData.findEmployee("emp_worker_1")
        val emergTask = TaskDocument(
            id = "t_rep_emerg",
            title = "Аварийный ремонт АВР",
            creatorId = head.id,
            creatorName = head.shortName,
            assigneeId = worker.id,
            assigneeName = worker.shortName,
            priority = TaskPriority.EMERGENCY.code,
            status = TaskStatus.APPROVED.code,
            dueDateEpochDay = today
        )

        val prioTpl = ReportTemplateDocument(
            id = "tpl_prio_test",
            name = "Распределение по приоритетам",
            reportType = ReportType.PRIORITY_DISTRIBUTION.code,
            sheetLayout = SheetLayout.SINGLE_SHEET.code,
            exportFormat = ExportFormat.PDF.code
        )
        val prioPreview = ReportDocumentGenerator.buildPreview(
            actor = head,
            template = prioTpl,
            allTasks = listOf(emergTask),
            currentEpochDay = today
        )
        val prioRows = prioPreview.sheets.first().rows
        assertEquals(5, prioRows.size)
        assertTrue(prioRows.any { it.priorityLabel == "Аварийный" && it.taskTitle.contains("Задач: 1") })

        val rpgState = RpgProgressionEngine.recordTaskCompletionOrApproval(
            state = RpgWorkspaceState(),
            task = emergTask,
            assignees = listOf(worker),
            supervisor = head,
            currentEpochDay = today,
            isApprovalStep = true
        ).state

        val gamifTpl = ReportTemplateDocument(
            id = "tpl_gamif_test",
            name = "Сводка прогрессии и XP",
            reportType = ReportType.GAMIFICATION_SUMMARY.code,
            sheetLayout = SheetLayout.SINGLE_SHEET.code,
            exportFormat = ExportFormat.XLSX.code
        )
        val gamifPreview = ReportDocumentGenerator.buildPreview(
            actor = head,
            template = gamifTpl,
            allTasks = listOf(emergTask),
            currentEpochDay = today,
            rpgState = rpgState
        )
        assertTrue(gamifPreview.sheets.first().rows.isNotEmpty())
        assertTrue(gamifPreview.summaryText.contains("XP"))
    }

    @Test
    fun `daily report and next day plan strictly scope tasks and photos to selected day without leaking past period`() {
        val today = LocalDate.of(2026, 11, 16).toEpochDay()
        val head = CorporateSeedData.findEmployee("emp_head_1")
        val worker = CorporateSeedData.findEmployee("emp_worker_1")

        fun makePhoto(id: String, comment: String) = com.example.data.model.PhotoReportItem(
            id = id,
            uriString = "stamp://$id",
            authorId = worker.id,
            authorName = worker.shortName,
            capturedAtText = "12:00",
            gpsCoords = "Участок №1",
            comment = comment
        ).encode()

        val pastApproved3DaysAgo = TaskDocument(
            id = "t_past_13",
            title = "Составить план на декабрь (13.11)",
            assigneeId = worker.id,
            assigneeName = worker.shortName,
            groupId = worker.groupId,
            groupName = worker.groupName,
            status = TaskStatus.APPROVED.code,
            dueDateEpochDay = today - 3L,
            photoItems = listOf(makePhoto("ph_13_1", "Фото 13.11 #1"), makePhoto("ph_13_2", "Фото 13.11 #2"))
        )
        val pastUncarried2DaysAgo = TaskDocument(
            id = "t_past_14",
            title = "Старая задача (14.11)",
            assigneeId = worker.id,
            assigneeName = worker.shortName,
            groupId = worker.groupId,
            groupName = worker.groupName,
            status = TaskStatus.REJECTED.code,
            dueDateEpochDay = today - 2L,
            photoItems = listOf(makePhoto("ph_14_1", "Фото 14.11 #1"))
        )
        val pastApprovedYesterday = TaskDocument(
            id = "t_past_15",
            title = "Взять болгарку (15.11)",
            assigneeId = worker.id,
            assigneeName = worker.shortName,
            groupId = worker.groupId,
            groupName = worker.groupName,
            status = TaskStatus.APPROVED.code,
            dueDateEpochDay = today - 1L,
            photoItems = listOf(makePhoto("ph_15_1", "Фото 15.11 #1"))
        )
        val todayApproved = TaskDocument(
            id = "t_today_done",
            title = "Планёрка сегодня (16.11)",
            assigneeId = worker.id,
            assigneeName = worker.shortName,
            groupId = worker.groupId,
            groupName = worker.groupName,
            status = TaskStatus.APPROVED.code,
            dueDateEpochDay = today,
            photoItems = listOf(makePhoto("ph_16_done", "Фото 16.11 выполнено"))
        )
        val todayInProgress = TaskDocument(
            id = "t_today_active",
            title = "Заполнение еврокубов сегодня (16.11)",
            assigneeId = worker.id,
            assigneeName = worker.shortName,
            groupId = worker.groupId,
            groupName = worker.groupName,
            status = TaskStatus.IN_PROGRESS.code,
            dueDateEpochDay = today,
            photoItems = listOf(makePhoto("ph_16_active", "Фото 16.11 в работе"))
        ).withUpdatedVolume(12.0, 4.0, "шт")
        val tomorrowPlan = TaskDocument(
            id = "t_tomorrow_17",
            title = "План на завтра (17.11)",
            assigneeId = worker.id,
            assigneeName = worker.shortName,
            groupId = worker.groupId,
            groupName = worker.groupName,
            status = TaskStatus.NEW.code,
            dueDateEpochDay = today + 1L
        )
        val distantFuture = TaskDocument(
            id = "t_future_21",
            title = "Дальний план (21.11)",
            assigneeId = worker.id,
            assigneeName = worker.shortName,
            groupId = worker.groupId,
            groupName = worker.groupName,
            status = TaskStatus.NEW.code,
            dueDateEpochDay = today + 5L,
            photoItems = listOf(makePhoto("ph_21_1", "Фото 21.11"))
        )

        val allTasks = listOf(
            pastApproved3DaysAgo,
            pastUncarried2DaysAgo,
            pastApprovedYesterday,
            todayApproved,
            todayInProgress,
            tomorrowPlan,
            distantFuture
        )

        val dailyTemplate = ReportTemplateDocument(
            id = "tpl_daily",
            name = "Отчет за текущий день",
            reportType = ReportType.DAILY_REPORT.code,
            sheetLayout = SheetLayout.SINGLE_SHEET.code,
            exportFormat = ExportFormat.PDF.code
        )

        val dailyPreview = ReportDocumentGenerator.buildPreview(
            actor = head,
            template = dailyTemplate,
            allTasks = allTasks,
            currentEpochDay = today,
            overrideLayout = SheetLayout.SINGLE_SHEET,
            includePhotos = true
        )

        val sections = dailyPreview.sheets.single().sections
        assertEquals(2, sections.size)

        val factRows = sections[0].rows
        assertEquals(2, factRows.size)
        assertTrue(factRows.all { it.deadlineLabel.startsWith("16.11.2026") })
        assertFalse(factRows.any { it.taskTitle.contains("13.11") || it.taskTitle.contains("14.11") || it.taskTitle.contains("15.11") })

        val planRows = sections[1].rows
        assertEquals(2, planRows.size)
        assertTrue(planRows.any { it.taskTitle == "План на завтра (17.11)" })
        assertTrue(planRows.any { it.taskTitle == "Заполнение еврокубов сегодня (16.11)" })
        assertFalse(planRows.any { it.taskTitle.contains("21.11") || it.taskTitle.contains("14.11") })

        assertEquals(2, dailyPreview.attachedPhotos.size)
        assertTrue(dailyPreview.attachedPhotos.all { it.photo.id.startsWith("ph_16_") })

        // Verify SPLIT_SHEETS mode also isolates daily fact & photos to 16.11.2026
        val splitPreview = ReportDocumentGenerator.buildPreview(
            actor = head,
            template = dailyTemplate,
            allTasks = allTasks,
            currentEpochDay = today,
            overrideLayout = SheetLayout.SPLIT_SHEETS,
            includePhotos = true
        )
        assertEquals(2, splitPreview.sheets[1].rows.size)
        assertEquals(2, splitPreview.sheets[0].rows.size)
        assertEquals(2, splitPreview.attachedPhotos.size)

        // Selecting yesterday (15.11.2026) exports strictly yesterday's fact & photo
        val yesterdayPreview = ReportDocumentGenerator.buildPreview(
            actor = head,
            template = dailyTemplate,
            allTasks = allTasks,
            currentEpochDay = today - 1L,
            overrideLayout = SheetLayout.SINGLE_SHEET,
            includePhotos = true
        )
        val yesterdayFactRows = yesterdayPreview.sheets.single().sections[0].rows
        assertEquals(1, yesterdayFactRows.size)
        assertEquals("Взять болгарку (15.11)", yesterdayFactRows.single().taskTitle)
        assertEquals(1, yesterdayPreview.attachedPhotos.size)
        assertEquals("ph_15_1", yesterdayPreview.attachedPhotos.single().photo.id)

        // Multi-day PERIOD_REPORT still aggregates across the period
        val periodTemplate = dailyTemplate.copy(
            id = "tpl_period",
            name = "Отчёт за произвольный период",
            reportType = ReportType.PERIOD_REPORT.code
        )
        val periodPreview = ReportDocumentGenerator.buildPreview(
            actor = head,
            template = periodTemplate,
            allTasks = allTasks,
            currentEpochDay = today,
            overrideLayout = SheetLayout.SINGLE_SHEET,
            includePhotos = true
        )
        assertEquals(7, periodPreview.sheets.single().sections[0].rows.size)
        assertEquals(7, periodPreview.attachedPhotos.size)
    }
}
