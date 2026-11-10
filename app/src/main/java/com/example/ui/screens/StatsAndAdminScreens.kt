package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    val approvedTasks = allTasks.filter { it.taskStatus == TaskStatus.APPROVED }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("approvals_queue_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FactCheck,
                            contentDescription = "Очередь проверки",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Контроль и приёмка фотоотчётов",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (activeEmployee.role) {
                            CorporateRole.HEAD -> "Режим Руководителя: вы можете подтверждать и отклонять любые отчёты ИТР и рабочих участка."
                            CorporateRole.ITR -> "Режим ИТР: вы проверяете и подтверждаете отчёты подчинённых рабочих своей бригады."
                            CorporateRole.WORKER -> "Режим Рабочего: вы видите статус проверки своих отчётов. Подтверждение чужих отчётов недоступно."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        item {
            Text(
                text = "Ожидают проверки ИТР / Руководителя (${onReviewTasks.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (onReviewTasks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "В очереди нет непринятых фотоотчётов. Все отправленные отчёты проверены.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(onReviewTasks, key = { it.id }) { task ->
                TaskItemCard(task = task, onClick = { onTaskClick(task) })
            }
        }

        if (rejectedTasks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Отклонены с замечаниями (${rejectedTasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(rejectedTasks, key = { it.id }) { task ->
                TaskItemCard(task = task, onClick = { onTaskClick(task) })
            }
        }

        if (approvedTasks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Подтверждённые отчёты — зачёркнуты и приняты (${approvedTasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669)
                )
            }
            items(approvedTasks, key = { it.id }) { task ->
                TaskItemCard(task = task, onClick = { onTaskClick(task) })
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Аналитика и исполнительская дисциплина",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Разрез доступа: ${stats.roleScopeLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onExportStats(ExportFormat.PDF) },
                            modifier = Modifier.testTag("export_stats_pdf_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Выгрузить PDF", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Выгрузить в PDF")
                        }
                        OutlinedButton(
                            onClick = { onExportStats(ExportFormat.CSV) },
                            modifier = Modifier.testTag("export_stats_csv_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Выгрузить CSV", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Выгрузить в CSV")
                        }
                    }
                }
            }
        }

        // KPI Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    title = "Назначено",
                    value = stats.totalAssigned.toString(),
                    subtitle = "В работе: ${stats.inProgressCount}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Подтверждено",
                    value = stats.approvedCount.toString(),
                    subtitle = "На проверке: ${stats.onReviewCount}",
                    color = Color(0xFF059669),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    title = "Просрочено / Возврат",
                    value = "${stats.overdueCount} / ${stats.rejectedCount}",
                    subtitle = "Автопереносов: ${stats.totalCarryovers}",
                    color = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Доля фотоотчётов",
                    value = "${stats.photoCompliancePercent}%",
                    subtitle = "Ср. время: ${stats.avgCompletionHours} ч",
                    color = Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Group / Brigade Breakdown
        item {
            Text(
                text = "Сводка по бригадам и подразделениям",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(stats.groupStats, key = { it.groupId }) { grp ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = grp.groupName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${grp.completionPercent}% выполнено",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (grp.completionPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Всего: ${grp.totalTasks} • Подтверждено: ${grp.approvedTasks} • На проверке: ${grp.onReviewTasks} • Просрочено: ${grp.overdueTasks}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Employee Discipline Leaderboard
        item {
            Text(
                text = "Исполнительская дисциплина по сотрудникам (${stats.employeeStats.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(stats.employeeStats, key = { it.employee.id }) { empStat ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${empStat.employee.fullName} (${empStat.employee.role.titleRu})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = empStat.employee.groupName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Назначено: ${empStat.assigned} • Принято: ${empStat.approved} • Возвратов: ${empStat.rejected} • Фото: ${empStat.photoCompliancePercent}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Surface(
                        color = if (empStat.disciplineScore >= 80) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${empStat.disciplineScore}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (empStat.disciplineScore >= 80) Color(0xFF065F46) else Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Current User & Role Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Текущая роль: ${activeEmployee.role.titleRu}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${activeEmployee.fullName} • ${activeEmployee.position}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = activeEmployee.role.badgeDescription,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Выйти", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Выйти")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Как распознаётся руководитель",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Как определяется Руководитель на телефоне: автоматически по корневому Email (m241185m@gmail.com) либо вводом кода активации руководителя «1001» при входе.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // SECTION 1: Pending Role Assignment Queue (Only Head can approve/assign roles)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (pendingMembers.isNotEmpty()) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (pendingMembers.isNotEmpty()) Color(0xFFD97706) else MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = "Ожидают назначения роли",
                                tint = Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Заявки на вход: ожидают назначения роли (${pendingMembers.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (activeEmployee.role == CorporateRole.HEAD) {
                                        "Выберите роль (Рабочий, ИТР, Руководитель) и бригаду для каждого вошедшего пользователя"
                                    } else {
                                        "Назначать роли новым пользователям может только Руководитель участка"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (pendingMembers.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Сейчас нет пользователей, ожидающих назначения роли. Вы можете заранее добавить сотрудника по Email ниже.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
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

        // SECTION 2: Pre-register employee by Email with a pre-assigned role
        if (activeEmployee.role == CorporateRole.HEAD) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Назначить роль по Email заранее",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Добавьте Email сотрудника — при входе с телефона ему сразу откроется выбранная роль",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { showPreRegisterForm = !showPreRegisterForm },
                                modifier = Modifier.testTag("toggle_preregister_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (showPreRegisterForm) "Скрыть" else "Добавить")
                            }
                        }

                        if (showPreRegisterForm) {
                            HorizontalDivider()
                            OutlinedTextField(
                                value = newEmpFullName,
                                onValueChange = { newEmpFullName = it },
                                label = { Text("ФИО сотрудника") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("preregister_name_input")
                            )
                            OutlinedTextField(
                                value = newEmpEmail,
                                onValueChange = { newEmpEmail = it },
                                label = { Text("Google Email сотрудника (например, ivanov@gmail.com)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("preregister_email_input")
                            )
                            OutlinedTextField(
                                value = newEmpPosition,
                                onValueChange = { newEmpPosition = it },
                                label = { Text("Должность / разряд") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "Назначаемая роль:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CorporateRole.entries.forEach { r ->
                                    FilterChip(
                                        selected = newEmpRole == r,
                                        onClick = { newEmpRole = r },
                                        label = { Text(r.titleRu) }
                                    )
                                }
                            }

                            Text(
                                text = "Бригада / подразделение:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CorporateSeedData.workGroups.forEach { grp ->
                                    FilterChip(
                                        selected = newEmpGroup.id == grp.id,
                                        onClick = { newEmpGroup = grp },
                                        label = { Text(grp.name) }
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_preregister_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Сохранить и назначить роль «${newEmpRole.titleRu}»")
                            }
                        }
                    }
                }
            }
        }

        // SECTION 3: Active Personnel & Role Reassignment + Demo Persona Switcher
        item {
            Text(
                text = "Штат участка: управление ролями и переключение профиля (${activeApprovedMembers.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Руководитель может изменить роль любого сотрудника (Рабочий ↔ ИТР ↔ Руководитель) или переключить активный профиль для проверки.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(activeApprovedMembers, key = { it.memberId.ifBlank { it.id } }) { member ->
            val currentRole = member.parsedRoleOrNull ?: CorporateRole.WORKER
            val isCurrentActive = member.memberId == activeEmployee.id
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentActive) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${member.fullName} • ${currentRole.titleRu}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${member.position} • ${member.groupName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Почта: ${member.email}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (CorporateSeedData.employees.any { it.id == member.memberId }) {
                            FilterChip(
                                selected = isCurrentActive,
                                onClick = { onSwitchPersona(member.memberId) },
                                label = { Text(if (isCurrentActive) "Активен" else "Войти в роль") },
                                modifier = Modifier.testTag("switch_persona_${member.memberId}")
                            )
                        }
                    }

                    if (activeEmployee.role == CorporateRole.HEAD) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Роль:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
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

        // SECTION 4: Audit Log Section
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HistoryEdu, contentDescription = "Аудит", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Журнал безопасности и аудита (${auditLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Неизменяемый журнал: назначения ролей, подтверждения, отклонения, переносы и выгрузки сводных отчётов.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(auditLogs, key = { it.id.ifBlank { it.details } }) { log ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${log.actionType} • ${log.actorName} (${log.actorRole})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = log.targetType,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = log.details,
                        style = MaterialTheme.typography.bodyMedium
                    )
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
    var selectedGroup by remember(member.groupId) {
        mutableStateOf(
            CorporateSeedData.workGroups.firstOrNull { it.id == member.groupId }
                ?: CorporateSeedData.workGroups.first()
        )
    }
    var positionInput by remember(member.position) { mutableStateOf(member.position) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, Color(0xFFD97706)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_member_card_${member.memberId}")
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Почта: ${member.email} • Запрошено: ${member.position}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Ожидает роли",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (canManageRoles) {
                HorizontalDivider()

                Text(
                    text = "1. Выберите роль для сотрудника:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CorporateRole.entries.forEach { roleOption ->
                        FilterChip(
                            selected = selectedRole == roleOption,
                            onClick = { selectedRole = roleOption },
                            label = { Text(roleOption.titleRu) },
                            modifier = Modifier.testTag("select_role_${member.memberId}_${roleOption.code}")
                        )
                    }
                }

                Text(
                    text = "2. Выберите бригаду / подразделение:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CorporateSeedData.workGroups.forEach { grp ->
                        FilterChip(
                            selected = selectedGroup.id == grp.id,
                            onClick = { selectedGroup = grp },
                            label = { Text(grp.name) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onAssignRole(selectedRole, selectedGroup, positionInput) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("approve_member_role_${member.memberId}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Назначить роль и допустить", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.testTag("reject_member_role_${member.memberId}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Отклонить")
                    }
                }
            }
        }
    }
}
