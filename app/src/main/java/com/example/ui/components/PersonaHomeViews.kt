package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AuditLogDocument
import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskDocument
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.domain.reporting.ReportDocumentGenerator
import com.example.ui.theme.LocalAppThemeTokens
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * 3 ролевых интерфейса (Persona-based UI) главного экрана в стиле Linear / Notion / Apple:
 * 1. WORKER — Интерфейс РАБОЧЕГО (Монтажник): фокус «Что мне сделать сегодня?».
 * 2. ITR — Интерфейс ИТР (Прораб, Мастер): контроль, горизонтальный скролл дат, пилюли-фильтры.
 * 3. DIRECTOR — Интерфейс РУКОВОДИТЕЛЯ (Директор): Executive Dashboard, Donut-график, KPI, риски, лента событий.
 */
enum class PersonaUiMode(
    val titleRu: String,
    val subtitleRu: String
) {
    WORKER("Рабочий", "Сменное задание на сегодня"),
    ITR("ИТР (Прораб)", "Контроль, распределение, приёмка"),
    DIRECTOR("Руководитель", "Аналитика, KPI и риски объекта");

    companion object {
        fun defaultForRole(role: CorporateRole): PersonaUiMode = when (role) {
            CorporateRole.WORKER -> WORKER
            CorporateRole.ITR -> ITR
            CorporateRole.HEAD, CorporateRole.ADMIN -> ITR
        }

        fun allowedModesForRole(role: CorporateRole): List<PersonaUiMode> = when (role) {
            CorporateRole.WORKER -> listOf(WORKER)
            CorporateRole.ITR -> listOf(WORKER, ITR)
            CorporateRole.HEAD, CorporateRole.ADMIN -> listOf(WORKER, ITR, DIRECTOR)
        }
    }
}

/**
 * Определение области видимости задач для ИТР:
 * - SUBDIVISION: Задачи тематического подразделения (профильная бригада / служба ИТР)
 * - SITE: Задачи строительного участка (все подразделения на объекте)
 * - PERSONAL: Задачи, назначенные лично ИТР
 */
enum class ItrScopeFilter(val titleRu: String, val chipLabel: String) {
    SUBDIVISION("Моё подразделение", "Бригада"),
    SITE("Весь участок", "Участок"),
    PERSONAL("Мои наряды", "Личные")
}

/**
 * Фильтры-пилюли для интерфейса ИТР (Прораб, Мастер):
 * В работе | На проверке | Проблемы | Все
 */
enum class ItrQuickFilter(val titleRu: String) {
    IN_WORK("В работе"),
    ON_REVIEW("На проверке"),
    PROBLEMS("Проблемы"),
    ALL("Все")
}

/**
 * Сегментированный переключатель ролевых интерфейсов, отображающий только режимы,
 * доступные уровню прав текущего пользователя:
 * - Рабочий видит только «Рабочий»
 * - ИТР видит «Рабочий» и «ИТР (Прораб)»
 * - Руководитель / Администратор видит «Рабочий», «ИТР (Прораб)» и «Руководитель»
 */
@Composable
fun PersonaModeSwitcherBar(
    selectedMode: PersonaUiMode,
    onSelectMode: (PersonaUiMode) -> Unit,
    allowedModes: List<PersonaUiMode> = PersonaUiMode.entries,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAppThemeTokens.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (tokens.isDark) 0.7f else 0.85f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("persona_mode_switcher")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            allowedModes.forEach { mode ->
                val isSelected = selectedMode == mode || allowedModes.size == 1
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (isSelected && !tokens.isDark) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectMode(mode) }
                        .testTag("persona_tab_${mode.name}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                    ) {
                        Text(
                            text = if (allowedModes.size == 1) {
                                "${mode.titleRu} • ${mode.subtitleRu}"
                            } else {
                                mode.titleRu
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 1. ИНТЕРФЕЙС РАБОЧЕГО (Монтажник)
// Фокус: «Что мне сделать сегодня?». Только текущая дата в шапке, крупные карточки
// без жёстких рамок, локация, огромный чекбокс «Выполнено» на карточке и иконка камеры с фото.
// ============================================================================

@Composable
fun WorkerTodayHeaderCard(
    currentEpochDay: Long,
    selectedEpochDay: Long = currentEpochDay,
    totalTasksToday: Int,
    completedTasksToday: Int,
    onCreateTaskClick: () -> Unit = {},
    onSelectDate: (Long) -> Unit = {},
    isCalendarExpanded: Boolean = false,
    onToggleExpandCalendar: () -> Unit = {}
) {
    val tokens = LocalAppThemeTokens.current
    val dateText = remember(selectedEpochDay) {
        ReportDocumentGenerator.formatEpochDay(selectedEpochDay)
    }
    val dayRelationText = when (selectedEpochDay) {
        currentEpochDay -> "Сегодня • $dateText"
        currentEpochDay + 1L -> "Завтра • $dateText"
        currentEpochDay - 1L -> "Вчера • $dateText"
        else -> dateText
    }
    val russianLocale = remember { Locale.forLanguageTag("ru-RU") }
    val dayStripOffsets = remember { (-1..4).toList() }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = tokens.cardShadowElevationDp.dp,
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
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Верхняя строка: минималистичный заголовок «Мои задачи» и компактный выпадающий селектор даты
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Мои задачи",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (totalTasksToday > 0) {
                            "Выполнено на смену: $completedTasksToday из $totalTasksToday"
                        } else {
                            "Сменное задание и контроль объёмов"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Компактный выпадающий селектор даты (разворачивает/сворачивает календарь)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onToggleExpandCalendar)
                        .testTag("worker_expand_calendar_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = dayRelationText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (isCalendarExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isCalendarExpanded) "Свернуть календарь" else "Выбрать дату в календаре",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Горизонтальный компактный скролл дат (быстрое переключение дня без раскрытия огромного календаря)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dayStripOffsets.forEach { offset ->
                    val dayEpoch = currentEpochDay + offset
                    val localDate = remember(dayEpoch) { LocalDate.ofEpochDay(dayEpoch) }
                    val isSelected = dayEpoch == selectedEpochDay
                    val isToday = dayEpoch == currentEpochDay
                    val shortDow = remember(localDate) {
                        localDate.dayOfWeek.getDisplayName(TextStyle.SHORT, russianLocale)
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }
                    }
                    val pillText = when (offset) {
                        -1 -> "Вчера, ${localDate.dayOfMonth}"
                        0 -> "Сегодня, ${localDate.dayOfMonth}"
                        1 -> "Завтра, ${localDate.dayOfMonth}"
                        else -> "$shortDow, ${localDate.dayOfMonth}.${String.format(Locale.US, "%02d", localDate.monthValue)}"
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectDate(dayEpoch) }
                    ) {
                        Text(
                            text = pillText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isToday -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerPersonaTaskCard(
    task: TaskDocument,
    onOpenDetail: () -> Unit,
    onQuickComplete: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    canApprove: Boolean = false,
    onApproveTask: (() -> Unit)? = null,
    onRejectTask: (() -> Unit)? = null
) {
    val tokens = LocalAppThemeTokens.current
    val isDoneOrReview = task.taskStatus == TaskStatus.APPROVED || task.taskStatus == TaskStatus.ON_REVIEW
    val doneColor = tokens.statusDoneColor
    val targetVol = task.effectiveTargetVolume
    val completedVol = task.effectiveCompletedVolume
    val unitLabel = task.effectiveVolumeUnit.ifBlank { "шт" }
    val progressRatio = if (targetVol > 0.0) {
        (completedVol / targetVol).toFloat().coerceIn(0f, 1f)
    } else {
        (task.completionPercent() / 100f).coerceIn(0f, 1f)
    }
    val subtasks = remember(task.subtaskItems) { task.decodedSubtasks() }
    val requiredPhotos = if (task.requiresPhotoReport) task.minPhotoCount.coerceAtLeast(1L) else 0L
    val currentPhotos = task.photoItems.size

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = tokens.cardShadowElevationDp.dp,
        border = if (tokens.cardBorderWidthDp > 0f) {
            BorderStroke(tokens.cardBorderWidthDp.dp, tokens.cardBorderColor)
        } else {
            null
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail)
            .testTag("task_card_${task.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Заголовок и локация
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (task.taskStatus == TaskStatus.APPROVED) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = task.geoLocation.orEmpty().ifBlank { "Участок №1" } + " • до ${task.deadlineTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (task.assigneeName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (task.allAssigneeNames.size > 1) {
                                "Исполнители (${task.allAssigneeNames.size}): ${task.assigneeName}"
                            } else {
                                "Исп.: ${task.assigneeName}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Иконка камеры с количеством нужных фото
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (currentPhotos >= requiredPhotos && requiredPhotos > 0) {
                        doneColor.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.clickable(onClick = onOpenDetail)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Фотоотчёт",
                            tint = if (currentPhotos >= requiredPhotos && requiredPhotos > 0) {
                                doneColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        val photoBadgeText = if (requiredPhotos > 0) {
                            "$currentPhotos/$requiredPhotos фото"
                        } else {
                            "$currentPhotos фото"
                        }
                        Text(
                            text = photoBadgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentPhotos >= requiredPhotos && requiredPhotos > 0) {
                                doneColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Прогресс-бар и чистые цифры объёма (без дублирования процентов)
            if (targetVol > 0.0) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Объём выполнения",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${SubtaskItem.formatVolumeNumber(completedVol)} / ${SubtaskItem.formatVolumeNumber(targetVol)} $unitLabel",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDoneOrReview) doneColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isDoneOrReview) doneColor else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Пункты чек-листа (чистые цифры объёма напротив каждого пункта)
            if (subtasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    subtasks.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleSubtask(sub.id) }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sub.isDone,
                                onCheckedChange = { onToggleSubtask(sub.id) },
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (sub.isDone) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${SubtaskItem.formatVolumeNumber(sub.effectiveCompletedVolume)} / ${SubtaskItem.formatVolumeNumber(sub.effectiveTargetVolume)} ${sub.effectiveUnit.ifBlank { unitLabel }}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (sub.isDone) doneColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Огромный чекбокс / кнопка «Выполнено» прямо на карточке
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDoneOrReview) {
                    doneColor.copy(alpha = if (tokens.isDark) 0.22f else 0.14f)
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clickable {
                        if (!isDoneOrReview) {
                            onQuickComplete()
                        } else {
                            onOpenDetail()
                        }
                    }
                    .testTag("worker_quick_complete_${task.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDoneOrReview) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isDoneOrReview) doneColor else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isDoneOrReview) {
                            "Выполнено (${task.taskStatus.titleRu})"
                        } else {
                            "Выполнено — сдать работу"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDoneOrReview) doneColor else MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Быстрые кнопки проверки и приёмки отчёта для ИТР / Руководителя / Администратора прямо с карточки
            if (canApprove && task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED &&
                (task.taskStatus == TaskStatus.ON_REVIEW || progressRatio >= 1f) && onApproveTask != null
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRejectTask != null) {
                        OutlinedButton(
                            onClick = onRejectTask,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.65f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("card_reject_task_${task.id}")
                        ) {
                            Text(
                                text = "Отклонить",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                    Button(
                        onClick = onApproveTask,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(42.dp)
                            .testTag("card_approve_task_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Принять отчёт",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. ИНТЕРФЕЙС ИТР (Прораб, Мастер)
// Фокус: Контроль, распределение, проверка.
// Сверху компактный горизонтальный скролл дат (полоска дней недели),
// под ним — горизонтальные пилюли-фильтры: В работе | На проверке | Проблемы | Все.
// Карточки среднего размера с прогресс-баром, статусом и аватаркой исполнителя.
// ============================================================================

@Composable
fun ItrHorizontalDateStrip(
    currentEpochDay: Long,
    selectedEpochDay: Long,
    allVisibleTasks: List<TaskDocument>,
    onSelectDate: (Long) -> Unit,
    onCreateTaskForDate: (Long) -> Unit,
    onToggleFullCalendar: () -> Unit,
    isFullCalendarExpanded: Boolean
) {
    val tokens = LocalAppThemeTokens.current
    val daysWindow = remember(currentEpochDay) {
        (-3L..10L).map { offset -> currentEpochDay + offset }
    }
    val countByDay = remember(allVisibleTasks) {
        allVisibleTasks
            .filter { it.taskStatus != TaskStatus.ARCHIVED }
            .groupBy { it.dueDateEpochDay }
            .mapValues { it.value.size }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = tokens.cardShadowElevationDp.dp,
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Верхняя строка: текущая дата + кнопка разворота календаря с задачами + аккуратная кнопка «+ Задача»
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isFullCalendarExpanded) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                        .clickable { onToggleFullCalendar() }
                        .testTag("toggle_expand_calendar_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Календарь",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = ReportDocumentGenerator.formatEpochDay(selectedEpochDay) +
                                        if (selectedEpochDay == currentEpochDay) " • Сегодня" else "",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isFullCalendarExpanded) {
                                        "Свернуть календарь с задачами"
                                    } else {
                                        "Развернуть календарь с задачами"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isFullCalendarExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isFullCalendarExpanded) "Свернуть календарь" else "Развернуть календарь",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Button(
                    onClick = { onCreateTaskForDate(selectedEpochDay) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("itr_header_add_task_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ Задача",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Горизонтальный скролл дат (полоска дней недели)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val ruLocale = remember { Locale.forLanguageTag("ru") }
                daysWindow.forEach { dayEpoch ->
                    val localDate = remember(dayEpoch) { LocalDate.ofEpochDay(dayEpoch) }
                    val dayOfWeekShort = remember(localDate, ruLocale) {
                        localDate.dayOfWeek.getDisplayName(TextStyle.SHORT, ruLocale)
                            .replaceFirstChar { it.titlecase(ruLocale) }
                    }
                    val isSelected = dayEpoch == selectedEpochDay
                    val isToday = dayEpoch == currentEpochDay
                    val count = countByDay[dayEpoch] ?: 0

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                        modifier = Modifier
                            .width(56.dp)
                            .clickable { onSelectDate(dayEpoch) }
                            .testTag("itr_date_pill_$dayEpoch")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayOfWeekShort,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = localDate.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            count == 0 -> Color.Transparent
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Панель определения области видимости для ИТР:
 * Тематическое подразделение (бригада) / Строительный участок / Личные задачи
 */
@Composable
fun ItrSubdivisionScopeBar(
    activeEmployee: CorporateEmployee,
    selectedScope: ItrScopeFilter,
    subdivisionCount: Int,
    siteCount: Int,
    personalCount: Int,
    onSelectScope: (ItrScopeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAppThemeTokens.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (!tokens.isDark) 2.dp else 0.dp,
        border = if (tokens.isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = activeEmployee.groupName.ifBlank { "Тематическое подразделение" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = activeEmployee.siteName.ifBlank { "Строительный участок" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Селектор области видимости задач ИТР
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ItrScopeFilter.entries.forEach { scope ->
                    val isSelected = selectedScope == scope
                    val count = when (scope) {
                        ItrScopeFilter.SUBDIVISION -> subdivisionCount
                        ItrScopeFilter.SITE -> siteCount
                        ItrScopeFilter.PERSONAL -> personalCount
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        modifier = Modifier
                            .clickable { onSelectScope(scope) }
                            .testTag("itr_scope_chip_${scope.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = scope.titleRu,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItrFilterPillsRow(
    selectedItrFilter: ItrQuickFilter,
    inWorkCount: Int,
    onReviewCount: Int,
    problemsCount: Int,
    allCount: Int,
    onSelectFilter: (ItrQuickFilter) -> Unit
) {
    val tokens = LocalAppThemeTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ItrQuickFilter.entries.forEach { filter ->
            val isSelected = selectedItrFilter == filter
            val count = when (filter) {
                ItrQuickFilter.IN_WORK -> inWorkCount
                ItrQuickFilter.ON_REVIEW -> onReviewCount
                ItrQuickFilter.PROBLEMS -> problemsCount
                ItrQuickFilter.ALL -> allCount
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shadowElevation = if (!tokens.isDark && !isSelected) 2.dp else 0.dp,
                modifier = Modifier
                    .clickable { onSelectFilter(filter) }
                    .testTag("itr_filter_pill_${filter.name}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = filter.titleRu,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItrPersonaTaskCard(
    task: TaskDocument,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onToggleSubtask: ((String) -> Unit)? = null,
    canApprove: Boolean = false,
    onQuickComplete: (() -> Unit)? = null,
    onApproveTask: (() -> Unit)? = null,
    onRejectTask: (() -> Unit)? = null
) {
    val tokens = LocalAppThemeTokens.current
    val doneColor = tokens.statusDoneColor
    val targetVol = task.effectiveTargetVolume
    val completedVol = task.effectiveCompletedVolume
    val unitLabel = task.effectiveVolumeUnit.ifBlank { "шт" }
    val pct = task.completionPercent()
    val subtasks = remember(task.subtaskItems) { task.decodedSubtasks() }
    val initials = remember(task.assigneeName, task.tags) {
        if (task.allAssigneeNames.size > 1) {
            "${task.allAssigneeNames.size}"
        } else {
            task.assigneeName
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
                .ifBlank { "И" }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = tokens.cardShadowElevationDp.dp,
        border = if (tokens.cardBorderWidthDp > 0f) {
            BorderStroke(tokens.cardBorderWidthDp.dp, tokens.cardBorderColor)
        } else {
            null
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("task_card_${task.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Заголовок + статус
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor(task.taskStatus).copy(alpha = if (tokens.isDark) 0.18f else 0.12f)
                ) {
                    Text(
                        text = task.taskStatus.titleRu,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor(task.taskStatus),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Прогресс-бар с чистыми цифрами объёма (190 / 450) без текстового дублирования процентов
            if (targetVol > 0.0) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.geoLocation.orEmpty().ifBlank { "Участок №1" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${SubtaskItem.formatVolumeNumber(completedVol)} / ${SubtaskItem.formatVolumeNumber(targetVol)} $unitLabel",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (pct >= 100) doneColor else MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (pct >= 100) doneColor else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Компактный чек-лист (до 3 пунктов)
            if (subtasks.isNotEmpty() && onToggleSubtask != null) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    subtasks.take(3).forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubtask(sub.id) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sub.isDone,
                                onCheckedChange = { onToggleSubtask(sub.id) },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (sub.isDone) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${SubtaskItem.formatVolumeNumber(sub.effectiveCompletedVolume)} / ${SubtaskItem.formatVolumeNumber(sub.effectiveTargetVolume)} ${sub.effectiveUnit.ifBlank { unitLabel }}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (sub.isDone) doneColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Нижняя строка: аватарка исполнителя + ФИО + дата + фото + удаление
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Аватарка исполнителя с инициалами или числом исполнителей
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.assigneeName.ifBlank { "Исполнитель" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${ReportDocumentGenerator.formatEpochDay(task.dueDateEpochDay)} • до ${task.deadlineTime}" +
                                if (task.requiresPhotoReport || task.photoItems.isNotEmpty()) {
                                    " • 📷 ${task.photoItems.size}/${task.minPhotoCount}"
                                } else {
                                    ""
                                },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_task_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Удалить задачу",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Кнопки быстрой приёмки / подтверждения выполнения прямо на карточке ИТР
            if (task.taskStatus != TaskStatus.APPROVED && task.taskStatus != TaskStatus.ARCHIVED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.taskStatus != TaskStatus.ON_REVIEW && onQuickComplete != null) {
                        OutlinedButton(
                            onClick = onQuickComplete,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("itr_quick_complete_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Сдать работу",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (onRejectTask != null && (task.taskStatus == TaskStatus.ON_REVIEW || pct >= 100)) {
                        OutlinedButton(
                            onClick = onRejectTask,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(0.85f)
                                .height(36.dp)
                                .testTag("itr_reject_task_${task.id}")
                        ) {
                            Text(
                                text = "Отклонить",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                    if (onApproveTask != null) {
                        Button(
                            onClick = onApproveTask,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .height(36.dp)
                                .testTag("itr_approve_task_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Принять отчёт",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. ИНТЕРФЕЙС РУКОВОДИТЕЛЯ (Директор) — Executive Dashboard
// Фокус: Аналитика и риски.
// - Круговой график готовности объекта (Donut Chart)
// - Сетка KPI (Просрочено, На проверке, Людей на объекте)
// - Блок «Требуют внимания» (критичные задачи с кнопками «Принять» / «Отклонить»)
// - «Лента событий» (живой лог: кто и какую работу сдал)
// ============================================================================

@Composable
fun DirectorExecutiveDashboard(
    allTasks: List<TaskDocument>,
    activeStaffCount: Int,
    auditLogs: List<AuditLogDocument>,
    onApproveTask: (TaskDocument) -> Unit,
    onRejectTask: (TaskDocument) -> Unit,
    onOpenTaskDetail: (TaskDocument) -> Unit
) {
    val tokens = LocalAppThemeTokens.current
    val doneColor = tokens.statusDoneColor
    val activeTasks = remember(allTasks) {
        allTasks.filter { it.taskStatus != TaskStatus.ARCHIVED && it.taskStatus != TaskStatus.CANCELLED }
    }
    val approvedCount = remember(activeTasks) { activeTasks.count { it.taskStatus == TaskStatus.APPROVED } }
    val onReviewCount = remember(activeTasks) { activeTasks.count { it.taskStatus == TaskStatus.ON_REVIEW } }
    val overdueOrRejectedCount = remember(activeTasks) {
        activeTasks.count {
            it.taskStatus == TaskStatus.OVERDUE ||
                it.taskStatus == TaskStatus.REJECTED ||
                it.carryoverCount > 0L
        }
    }
    val inProgressCount = remember(activeTasks, approvedCount, onReviewCount, overdueOrRejectedCount) {
        (activeTasks.size - approvedCount - onReviewCount - overdueOrRejectedCount).coerceAtLeast(0)
    }

    val overallReadinessFraction = remember(activeTasks) {
        if (activeTasks.isEmpty()) 0f
        else (activeTasks.sumOf { it.completionPercent() }.toFloat() / (activeTasks.size * 100f)).coerceIn(0f, 1f)
    }
    val totalPlannedVolume = remember(activeTasks) { activeTasks.sumOf { it.effectiveTargetVolume } }
    val totalCompletedVolume = remember(activeTasks) { activeTasks.sumOf { it.effectiveCompletedVolume } }

    val attentionTasks = remember(activeTasks) {
        activeTasks.filter {
            it.taskStatus == TaskStatus.ON_REVIEW ||
                it.taskStatus == TaskStatus.OVERDUE ||
                it.taskStatus == TaskStatus.REJECTED ||
                it.taskPriority == TaskPriority.CRITICAL
        }.take(6)
    }

    // Живая лента событий из истории задач и журнала аудита
    val recentEvents = remember(activeTasks, auditLogs) {
        val taskEvents = activeTasks.flatMap { task ->
            task.decodedHistory().map { entry ->
                Triple(entry.timestampText, entry.actorName, "${task.title}: ${entry.note}")
            }
        }
        val auditEvents = auditLogs.take(6).map { log ->
            Triple("Недавно", log.actorName, log.details)
        }
        (taskEvents.asReversed() + auditEvents).take(6)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Круговой график готовности объекта (Donut Chart)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = tokens.cardShadowElevationDp.dp,
            border = if (tokens.cardBorderWidthDp > 0f) {
                BorderStroke(tokens.cardBorderWidthDp.dp, tokens.cardBorderColor)
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("director_readiness_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Круговой Donut-график
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Box(
                    modifier = Modifier.size(104.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(92.dp)) {
                        val strokeWidth = 11.dp.toPx()
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = if (overallReadinessFraction >= 1f) doneColor else primaryColor,
                            startAngle = -90f,
                            sweepAngle = 360f * overallReadinessFraction,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$approvedCount/${activeTasks.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "сдано",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Готовность объекта",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (totalPlannedVolume > 0.0) {
                        Text(
                            text = "Объём: ${SubtaskItem.formatVolumeNumber(totalCompletedVolume)} / ${SubtaskItem.formatVolumeNumber(totalPlannedVolume)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LegendRowItem(color = doneColor, label = "Принято", count = approvedCount)
                    LegendRowItem(color = MaterialTheme.colorScheme.primary, label = "В работе", count = inProgressCount)
                    LegendRowItem(color = Color(0xFF7C3AED), label = "На проверке", count = onReviewCount)
                    LegendRowItem(color = Color(0xFFDC2626), label = "Риски / срыв", count = overdueOrRejectedCount)
                }
            }
        }

        // 2. Сетка KPI (Просрочено | На проверке | Людей на объекте)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DirectorKpiTile(
                title = "Просрочено",
                value = overdueOrRejectedCount.toString(),
                accentColor = if (overdueOrRejectedCount > 0) Color(0xFFDC2626) else doneColor,
                icon = Icons.Default.ErrorOutline,
                modifier = Modifier.weight(1f)
            )
            DirectorKpiTile(
                title = "На проверке",
                value = onReviewCount.toString(),
                accentColor = Color(0xFF7C3AED),
                icon = Icons.Default.PendingActions,
                modifier = Modifier.weight(1f)
            )
            DirectorKpiTile(
                title = "На объекте",
                value = "$activeStaffCount чел.",
                accentColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Groups,
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Блок «Требуют внимания» (критичные задачи с кнопками «Принять» / «Отклонить»)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = tokens.cardShadowElevationDp.dp,
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
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Требуют внимания",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Приёмка работ и устранение отклонений в один клик",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${attentionTasks.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (attentionTasks.isEmpty()) {
                    Text(
                        text = "Критических отклонений и задач, ожидающих приёмки, сейчас нет.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    attentionTasks.forEach { task ->
                        val tVol = task.effectiveTargetVolume
                        val cVol = task.effectiveCompletedVolume
                        val uLbl = task.effectiveVolumeUnit.ifBlank { "шт" }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenTaskDetail(task) }
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
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (tVol > 0.0) {
                                        Text(
                                            text = "${SubtaskItem.formatVolumeNumber(cVol)} / ${SubtaskItem.formatVolumeNumber(tVol)} $uLbl",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = "${task.assigneeName} • ${task.geoLocation.orEmpty().ifBlank { "Участок №1" }} • 📷 ${task.photoItems.size}/${task.minPhotoCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onApproveTask(task) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = doneColor,
                                            contentColor = if (tokens.isHighContrastField) Color(0xFF111312) else Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Принять", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onRejectTask(task) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Отклонить", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. «Лента событий» (живой лог: кто и какую работу сдал)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = tokens.cardShadowElevationDp.dp,
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
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Лента событий",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (recentEvents.isEmpty()) {
                    Text(
                        text = "События появятся по мере выполнения и приёмки работ.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    recentEvents.forEach { (timeLabel, actorName, description) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$actorName • $timeLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRowItem(
    color: Color,
    label: String,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DirectorKpiTile(
    title: String,
    value: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAppThemeTokens.current
    Surface(
        shape = RoundedCornerShape(16.dp),
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
