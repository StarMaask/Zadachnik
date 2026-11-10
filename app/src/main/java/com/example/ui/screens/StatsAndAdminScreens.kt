package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.HourglassTop
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
import androidx.compose.material3.LinearProgressIndicator
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
        contentPadding = PaddingValues(16.dp),
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
        contentPadding = PaddingValues(16.dp),
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
                                Text(
                                    text = empStat.employee.role.titleRu,
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
    onAssignRoleToMember: (SiteMemberDocument, CorporateRole, WorkGroup, String?) -> Unit,
    onRejectMemberAccess: (SiteMemberDocument) -> Unit,
    onDeleteSiteMember: (SiteMemberDocument) -> Unit = {},
    onPreRegisterEmployee: (email: String, fullName: String, position: String, role: CorporateRole, group: WorkGroup) -> Unit,
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
    var newEmpFullName by remember { mutableStateOf("") }
    var newEmpEmail by remember { mutableStateOf("") }
    var newEmpPosition by remember { mutableStateOf("") }
    var newEmpRole by remember { mutableStateOf(CorporateRole.WORKER) }
    var newEmpGroup by remember { mutableStateOf(CorporateSeedData.workGroups.first()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_audit_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Compact Profile Header Row
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
                            text = activeEmployee.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${activeEmployee.role.titleRu} • ${activeEmployee.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        OutlinedTextField(
                            value = newEmpEmail,
                            onValueChange = { newEmpEmail = it },
                            label = { Text("Email сотрудника *") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("preregister_email_input")
                        )
                        OutlinedTextField(
                            value = newEmpPosition,
                            onValueChange = { newEmpPosition = it },
                            label = { Text("Должность (необязательно)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

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
                                if (newEmpFullName.isNotBlank() && newEmpEmail.isNotBlank()) {
                                    onPreRegisterEmployee(
                                        newEmpEmail,
                                        newEmpFullName,
                                        newEmpPosition.ifBlank { newEmpRole.titleRu },
                                        newEmpRole,
                                        newEmpGroup
                                    )
                                    newEmpFullName = ""
                                    newEmpEmail = ""
                                    newEmpPosition = ""
                                    showPreRegisterForm = false
                                }
                            },
                            enabled = newEmpFullName.isNotBlank() && newEmpEmail.isNotBlank(),
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

        // Staff Rows (Clean Table List)
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.fullName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${member.position} • ${member.email}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (activeEmployee.role == CorporateRole.HEAD && memberKey != activeEmployee.id) {
                                IconButton(
                                    onClick = { onDeleteSiteMember(member) },
                                    modifier = Modifier.size(30.dp)
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

                        if (activeEmployee.role == CorporateRole.HEAD) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
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
