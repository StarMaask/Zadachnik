package com.example

import com.example.data.model.CorporateRole
import com.example.data.model.ExportFormat
import com.example.data.model.ReportTemplateDocument
import com.example.data.model.ReportType
import com.example.data.model.SheetLayout
import com.example.data.seed.CorporateSeedData
import com.example.domain.reporting.ReportDocumentGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(preview.sheets[0].sheetTitle.contains("План"))
        assertTrue(preview.sheets[1].sheetTitle.contains("Отчёт"))
        assertTrue(preview.sheets[0].rows.isNotEmpty())
        assertTrue(preview.sheets[1].rows.isNotEmpty())
    }

    @Test
    fun `worker role is restricted to personal tasks in report preview and blocked from consolidated SLA report`() {
        val today = CorporateSeedData.BASE_EPOCH_DAY
        val tasks = CorporateSeedData.buildInitialTasks(userId = "test_user_1")
        val workerActor = CorporateSeedData.findEmployee("emp_worker_1")
        val dailyTemplate = ReportTemplateDocument(
            id = "tpl_worker_daily",
            userId = "test_user_1",
            name = "Личный суточный отчёт",
            reportType = ReportType.DAILY_REPORT.code,
            sheetLayout = SheetLayout.SINGLE_SHEET.code,
            exportFormat = ExportFormat.PDF.code
        )

        val preview = ReportDocumentGenerator.buildPreview(
            actor = workerActor,
            template = dailyTemplate,
            allTasks = tasks,
            currentEpochDay = today,
            overrideLayout = SheetLayout.SINGLE_SHEET,
            overrideFormat = ExportFormat.PDF
        )

        assertEquals(1, preview.sheets.size)
        val allAssigneeNames = preview.sheets.flatMap { it.rows }.map { it.assigneeFio }.distinct()
        assertTrue(
            "Worker report must only contain worker's own tasks",
            allAssigneeNames.all { it.contains("Морозов") }
        )

        // Verify consolidated-only report throws IllegalAccessException for WORKER
        val consolidatedTemplate = dailyTemplate.copy(reportType = ReportType.APPROVAL_SLA.code)
        var accessDenied = false
        try {
            ReportDocumentGenerator.buildPreview(
                actor = workerActor,
                template = consolidatedTemplate,
                allTasks = tasks,
                currentEpochDay = today
            )
        } catch (_: IllegalAccessException) {
            accessDenied = true
        }
        assertTrue("Consolidated SLA report must be forbidden for WORKER", accessDenied)
    }

    @Test
    fun `role scoped visibility masks peer employees for WORKER while exposing brigade to ITR and site to HEAD`() {
        val workerActor = CorporateSeedData.findEmployee("emp_worker_1")
        val itrActor = CorporateSeedData.findEmployee("emp_itr_1")
        val headActor = CorporateSeedData.findEmployee("emp_head_1")

        val workerVisible = CorporateSeedData.getVisibleEmployeesForStats(workerActor)
        val itrVisible = CorporateSeedData.getVisibleEmployeesForStats(itrActor)
        val headVisible = CorporateSeedData.getVisibleEmployeesForStats(headActor)

        assertEquals(1, workerVisible.size)
        assertEquals("emp_worker_1", workerVisible.first().id)

        assertTrue(itrVisible.size > 1)
        assertTrue(itrVisible.none { it.role == CorporateRole.HEAD })

        assertEquals(CorporateSeedData.employees.size, headVisible.size)
    }
}
