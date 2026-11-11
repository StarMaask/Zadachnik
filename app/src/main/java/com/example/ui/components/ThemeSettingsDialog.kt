package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ThemePreferencesManager

/**
 * Экран «Настройки -> Оформление (Темы)» в эстетике Linear / Notion / Stripe.
 * Включает:
 * 1. Верхний блок: Переключатель (Switch) «Следовать системной теме» (Вкл/Выкл).
 * 2. Нижний блок: Интерактивные карточки тем с миниатюрой-превью (UI Mockup карточки ежедневника),
 *    названием, описанием и радио-индикатором выбранной темы (кружок с галочкой).
 */
@Composable
fun ThemeSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val themeSettings by ThemePreferencesManager.state.collectAsState()
    val systemDark = isSystemInDarkTheme()

    val activeEffectiveTheme = if (themeSettings.followSystemTheme) {
        if (systemDark) AppThemeMode.OBSIDIAN_DARK else AppThemeMode.CLASSIC_LIGHT
    } else {
        themeSettings.selectedTheme
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("theme_settings_dialog"),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Шапка экрана «Настройки -> Оформление (Темы)»
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Оформление (Темы)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Настройки • Визуальный стиль и читаемость",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_theme_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть"
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ВЕРХНИЙ БЛОК: Переключатель «Следовать системной теме»
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = if (activeEffectiveTheme == AppThemeMode.CLASSIC_LIGHT) 2.dp else 0.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ThemePreferencesManager.setFollowSystemTheme(
                                        context = context,
                                        followSystem = !themeSettings.followSystemTheme
                                    )
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PhoneAndroid,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.padding(end = 8.dp)) {
                                    Text(
                                        text = "Следовать системной теме",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (themeSettings.followSystemTheme) {
                                            "Включено • Автоматически (Classic Light / Obsidian Dark)"
                                        } else {
                                            "Выключено • Ручной выбор темы (${themeSettings.selectedTheme.titleRu})"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = themeSettings.followSystemTheme,
                                onCheckedChange = { checked ->
                                    ThemePreferencesManager.setFollowSystemTheme(context, checked)
                                },
                                modifier = Modifier.testTag("follow_system_theme_switch")
                            )
                        }
                    }

                    // Заголовок секции ручного выбора тем
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Палитры оформления",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (themeSettings.followSystemTheme) {
                                    "Нажмите на любую карточку ниже, чтобы перейти на ручной режим"
                                } else {
                                    "Выберите оптимальную тему для офиса, ночи или стройплощадки"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // НИЖНИЙ БЛОК: Карточки тем с миниатюрой-превью (UI Mockup)
                    AppThemeMode.entries.forEach { mode ->
                        val isSelectedManual = !themeSettings.followSystemTheme && themeSettings.selectedTheme == mode
                        val isEffectiveActive = activeEffectiveTheme == mode

                        ThemePreviewOptionCard(
                            mode = mode,
                            isSelected = isSelectedManual,
                            isSystemActive = themeSettings.followSystemTheme && isEffectiveActive,
                            dimmedBySystemToggle = themeSettings.followSystemTheme,
                            onSelect = {
                                ThemePreferencesManager.selectManualTheme(context, mode)
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Нижняя панель с кнопкой «Готово»
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Активна: ${activeEffectiveTheme.titleEn}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activeEffectiveTheme.titleRu,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("apply_theme_done_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Готово", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Карточка выбора конкретной темы с миниатюрой-превью (UI Mockup),
 * названием темы и радио-индикатором выбранного состояния.
 */
@Composable
private fun ThemePreviewOptionCard(
    mode: AppThemeMode,
    isSelected: Boolean,
    isSystemActive: Boolean,
    dimmedBySystemToggle: Boolean,
    onSelect: () -> Unit
) {
    val activeHighlight = isSelected || isSystemActive
    val borderStroke = if (activeHighlight) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (mode == AppThemeMode.CLASSIC_LIGHT && activeHighlight) 3.dp else 0.dp,
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (dimmedBySystemToggle && !isSystemActive) 0.88f else 1f)
            .clickable(onClick = onSelect)
            .testTag("theme_card_${mode.code}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Верхняя строка: Название темы + Бейдж + Радио-кнопка с галочкой
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${mode.titleEn} • ${mode.titleRu}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = mode.previewAccentColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = mode.badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = mode.previewAccentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = mode.vibeDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Радио-индикатор (кружок с галочкой)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (activeHighlight) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .border(
                            width = if (activeHighlight) 0.dp else 1.5.dp,
                            color = if (activeHighlight) Color.Transparent else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeHighlight) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Выбрано",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 2. Миниатюра-превью (UI Mockup) карточки строительного ежедневника в цветах темы
            MiniaturePlannerMockup(mode = mode)
        }
    }
}

/**
 * Миниатюра-превью (UI Mockup): рендерит аутентичный фрагмент экрана ежедневника
 * (фон приложения, карточка задачи с чек-листом, бейдж статуса, прогрессбар объёма и акцентная кнопка)
 * строго в цветовых токенах конкретной темы.
 */
@Composable
private fun MiniaturePlannerMockup(mode: AppThemeMode) {
    val isClassicLight = mode == AppThemeMode.CLASSIC_LIGHT
    val isObsidian = mode == AppThemeMode.OBSIDIAN_DARK
    val isTactical = mode == AppThemeMode.TACTICAL_FIELD

    // Контейнер фона темы
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(mode.previewBgColor)
            .border(
                width = 1.dp,
                color = mode.previewBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        // Мини-карточка задачи внутри фона темы
        val cardShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isClassicLight) {
                        // В Classic Light — мягкая воздушная тень без жестких рамок
                        Modifier.shadow(elevation = 3.dp, shape = cardShape, clip = false)
                    } else {
                        Modifier
                    }
                )
                .clip(cardShape)
                .background(
                    if (isObsidian) {
                        // В Obsidian Dark — легкий градиент матового стекла (Glassmorphism)
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF212530),
                                mode.previewCardColor
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(mode.previewCardColor, mode.previewCardColor)
                        )
                    }
                )
                .border(
                    width = when {
                        isClassicLight -> 0.5.dp
                        isTactical -> 1.5.dp
                        else -> 1.dp
                    },
                    color = mode.previewBorderColor,
                    shape = cardShape
                )
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Строка заголовка мини-задачи + статус-бейдж
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
                                .background(mode.previewDoneColor)
                        )
                        Text(
                            text = "Армирование плиты ПМ-1",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isTactical) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = mode.previewTextPrimary
                        )
                    }

                    // Бейдж статуса («Выполнено 100%» / «В работе»)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(mode.previewDoneColor.copy(alpha = if (isTactical) 0.22f else 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Объём: 150 / 150 м²",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = mode.previewDoneColor
                        )
                    }
                }

                // Полоса прогресса + мини-кнопка действия
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Чек-лист: 4/4 п. • Фотоотчёт: 3 шт",
                            style = MaterialTheme.typography.labelSmall,
                            color = mode.previewTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Мини-индикатор выполнения
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(mode.previewTextSecondary.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.76f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(mode.previewAccentColor)
                            )
                        }
                    }

                    // Мини-кнопка действия в акцентном цвете темы
                    val buttonTextColor = if (isTactical) Color(0xFF111312) else Color.White
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(mode.previewAccentColor)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+ Задача",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = buttonTextColor
                        )
                    }
                }
            }
        }
    }
}
