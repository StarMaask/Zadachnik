package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.TaskDocument
import com.example.data.model.TaskStatus
import com.example.data.seed.CorporateSeedData
import com.example.ui.components.TaskItemCard
import com.example.ui.theme.LocalAppThemeTokens

private enum class ArchiveFilterChip(
    val code: String,
    val labelRu: String
) {
    ALL("ALL", "Все"),
    TODAY("TODAY", "Сегодня"),
    WEEK("WEEK", "За 7 дней"),
    MONTH("MONTH", "За 30 дней"),
    WITH_PHOTOS("WITH_PHOTOS", "С фотоотчётом")
}

@Composable
fun CompletedArchiveScreen(
    activeEmployee: CorporateEmployee,
    allTasks: List<TaskDocument>,
    currentEpochDay: Long = CorporateSeedData.BASE_EPOCH_DAY,
    onTaskClick: (TaskDocument) -> Unit
) {
    val tokens = LocalAppThemeTokens.current
    val emeraldAccent = tokens.statusDoneColor
    val tonalSurface = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = if (tokens.isDark) 0.28f else 0.44f
    )

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ArchiveFilterChip.ALL) }

    val archivedTasksAll = remember(allTasks) {
        allTasks
            .filter { it.taskStatus == TaskStatus.APPROVED || it.taskStatus == TaskStatus.ARCHIVED }
            .sortedWith(
                compareByDescending<TaskDocument> { it.dueDateEpochDay }
                    .thenByDescending { it.updatedAt?.seconds ?: 0L }
            )
    }

    val filteredArchiveTasks = remember(archivedTasksAll, searchQuery, selectedFilter, currentEpochDay) {
        val query = searchQuery.trim().lowercase()
        archivedTasksAll.filter { task ->
            val matchesFilter = when (selectedFilter) {
                ArchiveFilterChip.ALL -> true
                ArchiveFilterChip.TODAY -> task.dueDateEpochDay == currentEpochDay
                ArchiveFilterChip.WEEK -> task.dueDateEpochDay in (currentEpochDay - 6L)..currentEpochDay
                ArchiveFilterChip.MONTH -> task.dueDateEpochDay in (currentEpochDay - 29L)..currentEpochDay
                ArchiveFilterChip.WITH_PHOTOS -> task.photoItems.isNotEmpty()
            }
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                task.title.lowercase().contains(query) ||
                    task.description.lowercase().contains(query) ||
                    task.assigneeName.lowercase().contains(query) ||
                    task.groupName.lowercase().contains(query) ||
                    task.approvedByName.orEmpty().lowercase().contains(query) ||
                    task.visibleTags.any { it.lowercase().contains(query) }
            }
            matchesFilter && matchesQuery
        }
    }

    val totalPhotosInArchive = remember(archivedTasksAll) {
        archivedTasksAll.sumOf { it.photoItems.size }
    }
    val tasksWithPhotosCount = remember(archivedTasksAll) {
        archivedTasksAll.count { it.photoItems.isNotEmpty() }
    }
    val todayCompletedCount = remember(archivedTasksAll, currentEpochDay) {
        archivedTasksAll.count { it.dueDateEpochDay == currentEpochDay }
    }

    val scopeSubtitle = when (activeEmployee.role) {
        CorporateRole.WORKER -> "Мои выполненные и подтверждённые работы"
        CorporateRole.ITR -> "Подтверждённые работы по подразделению (${activeEmployee.groupName})"
        CorporateRole.HEAD, CorporateRole.ADMIN -> "Принятые и архивные задания по участку (${activeEmployee.siteName})"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("archive_screen_list"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Архив",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = scopeSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(emeraldAccent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Архив выполненных работ",
                            tint = emeraldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Сводная строка архива
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArchiveSummaryPill(
                        label = "Всего в архиве",
                        value = "${archivedTasksAll.size}",
                        accentColor = emeraldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    ArchiveSummaryPill(
                        label = "Сегодня",
                        value = "$todayCompletedCount",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    ArchiveSummaryPill(
                        label = "Фотофиксация",
                        value = "$totalPhotosInArchive",
                        accentColor = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Поиск по архиву
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Поиск по названию, ФИО, бригаде...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Очистить поиск",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("archive_search_input")
                )

                // Фильтры периода и фото
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArchiveFilterChip.entries.forEach { chip ->
                        val isSelected = selectedFilter == chip
                        val badgeCount = when (chip) {
                            ArchiveFilterChip.ALL -> archivedTasksAll.size
                            ArchiveFilterChip.TODAY -> todayCompletedCount
                            ArchiveFilterChip.WEEK -> archivedTasksAll.count {
                                it.dueDateEpochDay in (currentEpochDay - 6L)..currentEpochDay
                            }
                            ArchiveFilterChip.MONTH -> archivedTasksAll.count {
                                it.dueDateEpochDay in (currentEpochDay - 29L)..currentEpochDay
                            }
                            ArchiveFilterChip.WITH_PHOTOS -> tasksWithPhotosCount
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else tonalSurface,
                            shadowElevation = if (isSelected && !tokens.isDark) 1.5.dp else 0.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedFilter = chip }
                                .testTag("archive_filter_${chip.code}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                            ) {
                                if (chip == ArchiveFilterChip.WITH_PHOTOS) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = chip.labelRu,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = badgeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (filteredArchiveTasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = if (tokens.isDark) 0.22f else 0.34f
                    ),
                    border = if (tokens.cardBorderWidthDp > 0f) {
                        BorderStroke(tokens.cardBorderWidthDp.dp, tokens.cardBorderColor)
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(emeraldAccent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = emeraldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = if (archivedTasksAll.isEmpty()) {
                                "Архив выполненных работ пока пуст"
                            } else {
                                "По заданному фильтру ничего не найдено"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (archivedTasksAll.isEmpty()) {
                                "После подтверждения выполнения задания автоматически переносятся с главной ленты сюда."
                            } else {
                                "Попробуйте изменить запрос или выбрать фильтр «Все»."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredArchiveTasks, key = { "archive_item_${it.id}" }) { task ->
                TaskItemCard(
                    task = task,
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
private fun ArchiveSummaryPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAppThemeTokens.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = tokens.cardShadowElevationDp.dp,
        border = if (tokens.cardBorderWidthDp > 0f) {
            BorderStroke(tokens.cardBorderWidthDp.dp, tokens.cardBorderColor)
        } else {
            null
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
