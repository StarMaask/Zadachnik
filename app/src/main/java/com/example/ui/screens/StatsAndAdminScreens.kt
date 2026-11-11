package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AuditLogDocument
import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.ExportFormat
import com.example.data.model.SiteMemberDocument
import com.example.data.model.StatisticsSummary
import com.example.data.model.TaskDocument
import com.example.data.model.TaskStatus
import com.example.data.model.WorkGroup
import com.example.data.seed.CorporateSeedData
import com.example.ui.components.TaskItemCard

@Composable
fun ApprovalsQueueScreen(
    activeEmployee: CorporateEmployee,
    allTasks: List<TaskDocument>,
    onTaskClick: (TaskDocument) -> Unit
) {
    val onReviewTasks = allTasks.filter { it.taskStatus == TaskStatus.ON_REVIEW }
    val rejectedTasks = allTasks.filter { it.taskStatus == TaskStatus.REJECTED }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("approvals_queue_list"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Очередь приёмки работ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "На проверке: ${onReviewTasks.size} • Возвращено на доработку: ${rejectedTasks.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FactCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (onReviewTasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Нет задач, ожидающих подтверждения.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(onReviewTasks, key = { it.id }) { task ->
                TaskItemCard(
                    task = task,
                    onClick = { onTaskClick(task) }
                )
            }
        }

        if (rejectedTasks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "На доработке (${rejectedTasks.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(rejectedTasks, key = { it.id }) { task ->
                TaskItemCard(
                    task = task,
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
fun StatisticsDashboardScreen(
    activeEmployee: CorporateEmployee,
    stats: StatisticsSummary,
    onExportStats: (ExportFormat) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("statistics_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header & quick export
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Сводная статистика",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stats.roleScopeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { onExportStats(ExportFormat.PDF) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { onExportStats(ExportFormat.XLSX) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("XLSX", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Compact KPI Table
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Показатели выполнения задач",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatTableRow("Всего поставлено задач", "${stats.totalAssigned}", MaterialTheme.colorScheme.onSurface)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    StatTableRow("Выполнено и подтверждено", "${stats.approvedCount}", Color(0xFF10B981))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    StatTableRow("В работе / На проверке", "${stats.inProgressCount} / ${stats.onReviewCount}", Color(0xFF2563EB))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    StatTableRow("Просрочено / Переносов", "${stats.overdueCount} / ${stats.totalCarryovers}", Color(0xFFE11D48))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    StatTableRow("Задачи с фотоотчётом", "${stats.tasksWithPhotosCount} (${stats.photoCompliancePercent}%)", MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Employee Table
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Таблица по сотрудникам",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Сотрудник",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.6f)
                        )
                        Text(
                            text = "Всего",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.6f)
                        )
                        Text(
                            text = "Сдано",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.6f)
                        )
                        Text(
                            text = "Рейтинг",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    HorizontalDivider()

                    stats.employeeStats.forEach { empStat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.6f)) {
                                Text(
                                    text = empStat.employee.shortName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subMeta = buildString {
                                    append(empStat.employee.role.titleRu)
                                    if (empStat.employee.personnelNumber.isNotBlank()) {
                                        append(" • Таб. № ${empStat.employee.personnelNumber}")
                                    }
                                }
                                Text(
                                    text = subMeta,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${empStat.assigned}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(0.6f)
                            )
                            Text(
                                text = "${empStat.approved}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(0.6f)
                            )
                            Text(
                                text = "${empStat.disciplineScore}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(0.7f)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTableRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminAndAuditScreen(
    activeEmployee: CorporateEmployee,
    siteMembers: List<SiteMemberDocument>,
    auditLogs: List<AuditLogDocument>,
    organizationName: String = CorporateSeedData.DEFAULT_ORG_NAME,
    siteName: String = CorporateSeedData.DEFAULT_SITE_NAME,
    onOpenOrganizationSettings: () -> Unit = {},
    onAssignRoleToMember: (SiteMemberDocument, CorporateRole, WorkGroup, String?) -> Unit,
    onRejectMemberAccess: (SiteMemberDocument) -> Unit,
    onDeleteSiteMember: (SiteMemberDocument) -> Unit = {},
    onEditEmployeeDetails: (
        targetMember: SiteMemberDocument,
        fullName: String,
        position: String,
        personnelNumber: String,
        siteName: String,
        groupName: String,
        phone: String,
        email: String,
        role: CorporateRole
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onPreRegisterEmployee: (
        email: String,
        fullName: String,
        position: String,
        role: CorporateRole,
        group: WorkGroup,
        personnelNumber: String,
        siteName: String,
        phone: String
    ) -> Unit,
    onSwitchPersona: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val pendingMembers = remember(siteMembers) {
        siteMembers.filter { it.isPendingRole && !it.isRejected }
    }
    val activeApprovedMembers = remember(siteMembers) {
        siteMembers.filter { !it.isPendingRole && !it.isRejected }
    }

    var showPreRegisterForm by remember { mutableStateOf(false) }
    var showAuditLogSection by remember { mutableStateOf(false) }
    var memberBeingEdited by remember { mutableStateOf<SiteMemberDocument?>(null) }

    var newEmpFullName by remember { mutableStateOf("") }
    var newEmpPosition by remember { mutableStateOf("") }
    var newEmpPersonnelNumber by remember { mutableStateOf("") }
    var newEmpSiteName by remember(siteName) { mutableStateOf(siteName) }
    var newEmpPhone by remember { mutableStateOf("") }
    var newEmpEmail by remember { mutableStateOf("") }
    var newEmpRole by remember { mutableStateOf(CorporateRole.WORKER) }
    var newEmpGroup by remember { mutableStateOf(CorporateSeedData.workGroups.first()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_audit_list"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Блок настроек наименования организации и участка
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = organizationName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Участок: $siteName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenOrganizationSettings,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("edit_org_site_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Изменить", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Compact Profile Header Row (с кнопкой редактирования своей карточки сотрудника)
        item {
            val currentSelfMember = remember(activeApprovedMembers, activeEmployee, siteName) {
                activeApprovedMembers.firstOrNull {
                    it.memberId == activeEmployee.id ||
                        it.id == activeEmployee.id ||
                        (activeEmployee.email.isNotBlank() && it.email.equals(activeEmployee.email, ignoreCase = true))
                } ?: SiteMemberDocument(
                    id = activeEmployee.id,
                    memberId = activeEmployee.id,
                    email = activeEmployee.email,
                    fullName = activeEmployee.fullName,
                    role = activeEmployee.role.code,
                    groupId = activeEmployee.groupId,
                    groupName = activeEmployee.groupName,
                    position = activeEmployee.position,
                    personnelNumber = activeEmployee.personnelNumber,
                    siteName = activeEmployee.siteName.ifBlank { siteName },
                    phone = activeEmployee.phone
                ).let { base ->
                    base.copy(
                        approvedByName = base.encodeApprovedByForFirestore(
                            approverName = activeEmployee.fullName,
                            explicitPersonnelNumber = activeEmployee.personnelNumber,
                            explicitSiteName = activeEmployee.siteName.ifBlank { siteName },
                            explicitPhone = activeEmployee.phone
                        )
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeEmployee.fullName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val profileMeta = buildString {
                                append(activeEmployee.position.ifBlank { activeEmployee.role.titleRu })
                                if (activeEmployee.personnelNumber.isNotBlank()) {
                                    append(" • Таб. № ${activeEmployee.personnelNumber}")
                                }
                                if (activeEmployee.siteName.isNotBlank()) {
                                    append(" • ${activeEmployee.siteName}")
                                }
                            }
                            Text(
                                text = profileMeta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { memberBeingEdited = currentSelfMember },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_self_profile_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Карточка сотрудника", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Выйти", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Выйти", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // SECTION 1: Pending Role Assignment Queue
        if (pendingMembers.isNotEmpty()) {
            item {
                Text(
                    text = "Ожидают подтверждения (${pendingMembers.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706)
                )
            }
            items(pendingMembers, key = { it.memberId.ifBlank { it.id } }) { pendingUser ->
                PendingUserRoleAssignmentCard(
                    member = pendingUser,
                    canManageRoles = activeEmployee.role == CorporateRole.HEAD,
                    onAssignRole = { role, group, pos ->
                        onAssignRoleToMember(pendingUser, role, group, pos)
                    },
                    onReject = { onRejectMemberAccess(pendingUser) }
                )
            }
        }

        // SECTION 2: Active Personnel Table + Submenu to Add Employee
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Список сотрудников (${activeApprovedMembers.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                if (activeEmployee.role == CorporateRole.HEAD) {
                    TextButton(
                        onClick = { showPreRegisterForm = !showPreRegisterForm },
                        modifier = Modifier.testTag("toggle_preregister_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showPreRegisterForm) "Скрыть форму" else "Добавить сотрудника")
                    }
                }
            }
        }

        // Collapsible Add Employee Form
        if (activeEmployee.role == CorporateRole.HEAD && showPreRegisterForm) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Новый сотрудник",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = newEmpFullName,
                            onValueChange = { newEmpFullName = it },
                            label = { Text("ФИО сотрудника *") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("preregister_name_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newEmpPosition,
                                onValueChange = { newEmpPosition = it },
                                label = { Text("Должность") },
                                singleLine = true,
                                modifier = Modifier.weight(1.3f)
                            )
                            OutlinedTextField(
                                value = newEmpPersonnelNumber,
                                onValueChange = { newEmpPersonnelNumber = it },
                                label = { Text("Таб. номер") },
                                singleLine = true,
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                        OutlinedTextField(
                            value = newEmpSiteName,
                            onValueChange = { newEmpSiteName = it },
                            label = { Text("Принадлежность к участку") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newEmpPhone,
                                onValueChange = { newEmpPhone = it },
                                label = { Text("Телефон") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newEmpEmail,
                                onValueChange = { newEmpEmail = it },
                                label = { Text("Email (необязательно)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("preregister_email_input")
                            )
                        }

                        Text("Подразделение / бригада:", style = MaterialTheme.typography.labelSmall)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CorporateSeedData.workGroups.forEach { grp ->
                                FilterChip(
                                    selected = newEmpGroup.id == grp.id,
                                    onClick = { newEmpGroup = grp },
                                    label = { Text(grp.name) }
                                )
                            }
                        }

                        Text("Роль:", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CorporateRole.entries.forEach { r ->
                                FilterChip(
                                    selected = newEmpRole == r,
                                    onClick = { newEmpRole = r },
                                    label = {
                                        Text(
                                            when (r) {
                                                CorporateRole.WORKER -> "Рабочий"
                                                CorporateRole.ITR -> "ИТР"
                                                CorporateRole.HEAD -> "Руководитель"
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (newEmpFullName.isNotBlank()) {
                                    onPreRegisterEmployee(
                                        newEmpEmail,
                                        newEmpFullName,
                                        newEmpPosition.ifBlank { newEmpRole.titleRu },
                                        newEmpRole,
                                        newEmpGroup,
                                        newEmpPersonnelNumber,
                                        newEmpSiteName.ifBlank { siteName },
                                        newEmpPhone
                                    )
                                    newEmpFullName = ""
                                    newEmpEmail = ""
                                    newEmpPosition = ""
                                    newEmpPersonnelNumber = ""
                                    newEmpPhone = ""
                                    showPreRegisterForm = false
                                }
                            },
                            enabled = newEmpFullName.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_preregister_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Сохранить сотрудника")
                        }
                    }
                }
            }
        }

        // Staff Rows (Clean Table List with Edit button)
        if (activeApprovedMembers.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "В списке пока только ваша учётная запись. Добавьте сотрудников или дождитесь их входа.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(activeApprovedMembers, key = { it.memberId.ifBlank { it.id } }) { member ->
                val currentRole = member.parsedRoleOrNull ?: CorporateRole.WORKER
                val memberKey = member.memberId.ifBlank { member.id }
                val tabNumber = member.effectivePersonnelNumber
                val empSite = member.effectiveSiteName.ifBlank { siteName }
                val empPhone = member.effectivePhone

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.fullName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val line1 = buildString {
                                    append(member.position.ifBlank { currentRole.titleRu })
                                    if (tabNumber.isNotBlank()) {
                                        append(" • Таб. № $tabNumber")
                                    }
                                }
                                Text(
                                    text = line1,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                val line2 = buildString {
                                    append("Участок: $empSite • ${member.groupName}")
                                    if (empPhone.isNotBlank()) append(" • $empPhone")
                                }
                                Text(
                                    text = line2,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { memberBeingEdited = member },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("edit_member_${memberKey}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Редактировать сотрудника",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (activeEmployee.role == CorporateRole.HEAD && memberKey != activeEmployee.id) {
                                    IconButton(
                                        onClick = { onDeleteSiteMember(member) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Удалить сотрудника",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (activeEmployee.role == CorporateRole.HEAD) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CorporateRole.entries.forEach { roleOption ->
                                    val isRoleSelected = currentRole == roleOption
                                    FilterChip(
                                        selected = isRoleSelected,
                                        onClick = {
                                            if (!isRoleSelected) {
                                                val grp = CorporateSeedData.workGroups.firstOrNull {
                                                    it.id == member.groupId
                                                } ?: CorporateSeedData.workGroups.first()
                                                onAssignRoleToMember(member, roleOption, grp, member.position)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = when (roleOption) {
                                                    CorporateRole.WORKER -> "Рабочий"
                                                    CorporateRole.ITR -> "ИТР"
                                                    CorporateRole.HEAD -> "Руководитель"
                                                },
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    )
                                }
                                OutlinedButton(
                                    onClick = { memberBeingEdited = member },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Редактировать", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Collapsible Submenu for Audit Log
        item {
            TextButton(
                onClick = { showAuditLogSection = !showAuditLogSection },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (showAuditLogSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (showAuditLogSection) {
                        "Скрыть журнал действий"
                    } else {
                        "Журнал действий (${auditLogs.size})"
                    }
                )
            }
        }

        if (showAuditLogSection) {
            items(auditLogs, key = { it.id.ifBlank { it.details } }) { log ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${log.actionType} • ${log.actorName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = log.details,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    memberBeingEdited?.let { target ->
        EditEmployeeDialog(
            member = target,
            defaultSiteName = siteName,
            onDismiss = { memberBeingEdited = null },
            onSave = { fullName, position, personnelNumber, empSite, groupName, phone, email, role ->
                onEditEmployeeDetails(
                    target,
                    fullName,
                    position,
                    personnelNumber,
                    empSite,
                    groupName,
                    phone,
                    email,
                    role
                )
                memberBeingEdited = null
            }
        )
    }
}

/**
 * Диалог редактирования карточки сотрудника:
 * ФИО, должность, табельный номер, принадлежность к участку, подразделение/бригада, телефон, email, роль.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditEmployeeDialog(
    member: SiteMemberDocument,
    defaultSiteName: String,
    onDismiss: () -> Unit,
    onSave: (
        fullName: String,
        position: String,
        personnelNumber: String,
        siteName: String,
        groupName: String,
        phone: String,
        email: String,
        role: CorporateRole
    ) -> Unit
) {
    val memberSnapshotKey = remember(member) {
        "${member.id}_${member.memberId}_${member.fullName}_${member.position}_${member.effectivePersonnelNumber}_${member.effectiveSiteName}_${member.groupName}_${member.effectivePhone}_${member.email}_${member.role}"
    }
    var fullName by remember(memberSnapshotKey) { mutableStateOf(member.fullName) }
    var position by remember(memberSnapshotKey) { mutableStateOf(member.position) }
    var personnelNumber by remember(memberSnapshotKey) { mutableStateOf(member.effectivePersonnelNumber) }
    var siteName by remember(memberSnapshotKey, defaultSiteName) {
        mutableStateOf(member.effectiveSiteName.ifBlank { defaultSiteName })
    }
    var groupName by remember(memberSnapshotKey) { mutableStateOf(member.groupName) }
    var phone by remember(memberSnapshotKey) { mutableStateOf(member.effectivePhone) }
    var email by remember(memberSnapshotKey) { mutableStateOf(member.email) }
    var selectedRole by remember(memberSnapshotKey) {
        mutableStateOf(member.parsedRoleOrNull ?: CorporateRole.WORKER)
    }

    val saveEmployeeAction: () -> Unit = {
        val resolvedName = fullName.trim().ifBlank { member.fullName.ifBlank { "Сотрудник" } }
        onSave(
            resolvedName,
            position.trim().ifBlank { selectedRole.titleRu },
            personnelNumber.trim(),
            siteName.trim().ifBlank { defaultSiteName },
            groupName.trim().ifBlank { "Основное подразделение" },
            phone.trim(),
            email.trim(),
            selectedRole
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.84f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Верхняя шапка с кнопкой «Сохранить»
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Карточка сотрудника",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = saveEmployeeAction,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("top_save_employee_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Сохранить", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Прокручиваемая форма полей сотрудника
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("ФИО сотрудника *") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_employee_fullname_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = position,
                                onValueChange = { position = it },
                                label = { Text("Должность") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("edit_employee_position_input")
                            )
                            OutlinedTextField(
                                value = personnelNumber,
                                onValueChange = { personnelNumber = it },
                                label = { Text("Табельный №") },
                                placeholder = { Text("Напр.: 0142") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("edit_employee_tab_input")
                            )
                        }

                        OutlinedTextField(
                            value = siteName,
                            onValueChange = { siteName = it },
                            label = { Text("Принадлежность к участку") },
                            placeholder = { Text("Напр.: Участок №1 — СМР") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_employee_site_input")
                        )

                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("Бригада / подразделение") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_employee_group_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CorporateSeedData.workGroups.forEach { grp ->
                                FilterChip(
                                    selected = groupName == grp.name,
                                    onClick = { groupName = grp.name },
                                    label = { Text(grp.name) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Телефон") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        Text(
                            text = "Роль в системе:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CorporateRole.entries.forEach { roleOption ->
                                FilterChip(
                                    selected = selectedRole == roleOption,
                                    onClick = { selectedRole = roleOption },
                                    label = {
                                        Text(
                                            when (roleOption) {
                                                CorporateRole.WORKER -> "Рабочий"
                                                CorporateRole.ITR -> "ИТР"
                                                CorporateRole.HEAD -> "Руководитель"
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = saveEmployeeAction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("inline_save_employee_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Применить изменения карточки", fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Нижняя панель кнопок (с безопасным отступом от краёв экрана и панели навигации)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = saveEmployeeAction,
                            modifier = Modifier
                                .weight(1.4f)
                                .height(46.dp)
                                .testTag("confirm_save_employee_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Сохранить изменения", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Диалог настройки наименования организации и участка.
 */
@Composable
fun OrganizationSettingsDialog(
    currentOrganizationName: String,
    currentSiteName: String,
    onDismiss: () -> Unit,
    onSave: (organizationName: String, siteName: String) -> Unit
) {
    var orgName by remember(currentOrganizationName) { mutableStateOf(currentOrganizationName) }
    var siteName by remember(currentSiteName) { mutableStateOf(currentSiteName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Организация и участок",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                OutlinedTextField(
                    value = orgName,
                    onValueChange = { orgName = it },
                    label = { Text("Наименование организации *") },
                    placeholder = { Text("Напр.: ООО «ГлавСтройИнжиниринг»") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("org_name_input")
                )

                OutlinedTextField(
                    value = siteName,
                    onValueChange = { siteName = it },
                    label = { Text("Наименование участка / подразделения *") },
                    placeholder = { Text("Напр.: Участок №1 — СМР и Инженерия") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("site_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            if (orgName.isNotBlank() || siteName.isNotBlank()) {
                                onSave(
                                    orgName.trim().ifBlank { CorporateSeedData.DEFAULT_ORG_NAME },
                                    siteName.trim().ifBlank { CorporateSeedData.DEFAULT_SITE_NAME }
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_org_site_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PendingUserRoleAssignmentCard(
    member: SiteMemberDocument,
    canManageRoles: Boolean,
    onAssignRole: (CorporateRole, WorkGroup, String) -> Unit,
    onReject: () -> Unit
) {
    var selectedRole by remember { mutableStateOf(CorporateRole.WORKER) }
    val selectedGroup = remember(member.groupId) {
        CorporateSeedData.workGroups.firstOrNull { it.id == member.groupId }
            ?: CorporateSeedData.workGroups.first()
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFD97706)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_member_card_${member.memberId}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${member.email} • ${member.position}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Ожидает",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (canManageRoles) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CorporateRole.entries.forEach { roleOption ->
                        FilterChip(
                            selected = selectedRole == roleOption,
                            onClick = { selectedRole = roleOption },
                            label = {
                                Text(
                                    when (roleOption) {
                                        CorporateRole.WORKER -> "Рабочий"
                                        CorporateRole.ITR -> "ИТР"
                                        CorporateRole.HEAD -> "Руководитель"
                                    }
                                )
                            },
                            modifier = Modifier.testTag("select_role_${member.memberId}_${roleOption.code}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAssignRole(selectedRole, selectedGroup, member.position) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("approve_member_role_${member.memberId}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Принять", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.testTag("reject_member_role_${member.memberId}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Отклонить")
                    }
                }
            }
        }
    }
}
