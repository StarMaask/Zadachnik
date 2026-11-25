package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CorporateEmployee
import com.example.data.model.CorporateRole
import com.example.data.model.TaskPriority
import com.example.domain.rpg.Achievement
import com.example.domain.rpg.EmployeeRpgProfile
import com.example.domain.rpg.GamificationConfig
import com.example.domain.rpg.InventoryItemCategory
import com.example.domain.rpg.LeaderboardPrivacy
import com.example.domain.rpg.PriorityXpRule
import com.example.domain.rpg.Quest
import com.example.domain.rpg.QuestCadence
import com.example.domain.rpg.RpgProgressionEngine
import com.example.domain.rpg.RpgWorkspaceState
import com.example.domain.rpg.ServiceSpecialization
import com.example.domain.rpg.SkillBranch
import com.example.domain.rpg.XpTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RpgProgressionHubContent(
    activeEmployee: CorporateEmployee,
    availableEmployees: List<CorporateEmployee>,
    rpgState: RpgWorkspaceState,
    currentEpochDay: Long,
    onUpgradeSkill: (String) -> Unit,
    onResetSkillsForSeason: () -> Unit,
    onEquipInventoryItem: (String) -> Unit,
    onUseStreakRecovery: () -> Unit,
    onActivatePrestige: () -> Unit,
    onSetLeaderboardPrivacy: (LeaderboardPrivacy) -> Unit,
    onAdjustEmployeeRpg: (CorporateEmployee, Int, Int, String) -> Unit
) {
    val visibleEmployees = remember(activeEmployee, availableEmployees) {
        (listOf(activeEmployee) + availableEmployees)
            .distinctBy { it.id }
            .filter { RpgProgressionEngine.canViewProgression(activeEmployee, it) }
    }

    var inspectedEmployeeId by remember(activeEmployee.id) { mutableStateOf(activeEmployee.id) }
    val targetEmployee = remember(inspectedEmployeeId, visibleEmployees, activeEmployee) {
        visibleEmployees.firstOrNull { it.id == inspectedEmployeeId } ?: activeEmployee
    }
    val isSelf = targetEmployee.id == activeEmployee.id
    val profile = rpgState.profileForEmployee(targetEmployee)
    val userLevel = remember(profile.totalXp, rpgState.config) {
        RpgProgressionEngine.resolveUserLevel(profile.totalXp, rpgState.config)
    }
    val userStatus = remember(targetEmployee.role, userLevel.level) {
        RpgProgressionEngine.resolveUserStatus(targetEmployee.role, userLevel.level)
    }

    var selectedSubtab by remember { mutableIntStateOf(0) } // 0=Профиль, 1=Навыки, 2=Квесты и Награды, 3=Рейтинг и Лог
    var adjustTarget by remember { mutableStateOf<CorporateEmployee?>(null) }
    var showPrestigeConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rpg_progression_hub"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (visibleEmployees.size > 1) {
            RpgEmployeeSelectorBar(
                employees = visibleEmployees,
                selectedEmployeeId = targetEmployee.id,
                selfId = activeEmployee.id,
                onSelect = { inspectedEmployeeId = it }
            )
        }

        RpgHeroSummaryCard(
            targetEmployee = targetEmployee,
            isSelf = isSelf,
            canAdjust = RpgProgressionEngine.canAdjustProgression(activeEmployee),
            profile = profile,
            level = userLevel,
            status = userStatus,
            config = rpgState.config,
            onRequestAdjust = { adjustTarget = targetEmployee },
            onRequestPrestige = { showPrestigeConfirm = true }
        )

        RpgSubtabSwitcher(
            selectedSubtab = selectedSubtab,
            onSelect = { selectedSubtab = it }
        )

        when (selectedSubtab) {
            0 -> RpgProfileAndStreakTab(
                profile = profile,
                config = rpgState.config,
                isSelf = isSelf,
                onUseStreakRecovery = onUseStreakRecovery,
                onEquipItem = onEquipInventoryItem
            )
            1 -> RpgSkillsTreeTab(
                targetEmployee = targetEmployee,
                profile = profile,
                rpgState = rpgState,
                isSelf = isSelf,
                onUpgradeSkill = onUpgradeSkill,
                onRequestReset = { showResetConfirm = true }
            )
            2 -> RpgQuestsAndAchievementsTab(
                targetEmployee = targetEmployee,
                profile = profile,
                rpgState = rpgState,
                currentEpochDay = currentEpochDay
            )
            else -> RpgLeaderboardAndLogTab(
                viewer = activeEmployee,
                targetEmployee = targetEmployee,
                availableEmployees = availableEmployees,
                profile = profile,
                rpgState = rpgState,
                isSelf = isSelf,
                onSetPrivacy = onSetLeaderboardPrivacy
            )
        }
    }

    adjustTarget?.let { target ->
        ManualRpgAdjustDialog(
            targetEmployee = target,
            onDismiss = { adjustTarget = null },
            onConfirm = { xpDelta, repDelta, reason ->
                onAdjustEmployeeRpg(target, xpDelta, repDelta, reason)
                adjustTarget = null
            }
        )
    }

    if (showPrestigeConfirm) {
        AlertDialog(
            onDismissRequest = { showPrestigeConfirm = false },
            title = { Text("Активация Престижа") },
            text = {
                val bonusPct = ((profile.prestige.prestigeLevel + 1) *
                    rpgState.config.prestigePermanentBonusPerLevel * 100).roundToInt()
                Text(
                    "Вы достигли максимального ${rpgState.config.maxLevel} уровня! Переход на Престиж обнулит текущий уровень до 1, но выдаст эмблему Престиж ${profile.prestige.prestigeLevel + 1}, +2 SP и постоянный множитель XP +$bonusPct%."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPrestigeConfirm = false
                        onActivatePrestige()
                    },
                    modifier = Modifier.testTag("confirm_prestige_button")
                ) {
                    Text("Активировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrestigeConfirm = false }) { Text("Отмена") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Сезонный сброс навыков") },
            text = {
                val penalty = rpgState.config.seasonalResetSpPenalty
                Text(
                    "Сброс доступен 1 раз за сезон (${rpgState.currentSeason.titleRu}). Будут сброшены все изученные навыки со штрафом -$penalty SP. Продолжить?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetSkillsForSeason()
                    },
                    modifier = Modifier.testTag("confirm_skill_reset_button")
                ) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun RpgEmployeeSelectorBar(
    employees: List<CorporateEmployee>,
    selectedEmployeeId: String,
    selfId: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        employees.forEach { emp ->
            val selected = emp.id == selectedEmployeeId
            FilterChip(
                selected = selected,
                onClick = { onSelect(emp.id) },
                label = {
                    val tag = if (emp.id == selfId) " (Вы)" else " • ${emp.role.shortTitleRu}"
                    Text("${emp.shortName}$tag", maxLines = 1)
                },
                modifier = Modifier.testTag("rpg_emp_chip_${emp.id}")
            )
        }
    }
}

@Composable
private fun RpgHeroSummaryCard(
    targetEmployee: CorporateEmployee,
    isSelf: Boolean,
    canAdjust: Boolean,
    profile: EmployeeRpgProfile,
    level: com.example.domain.rpg.UserLevel,
    status: com.example.domain.rpg.UserStatus,
    config: GamificationConfig,
    onRequestAdjust: () -> Unit,
    onRequestPrestige: () -> Unit
) {
    val equippedFrame = profile.inventory.equippedFrameId?.let { id ->
        RpgProgressionEngine.inventoryCatalog.firstOrNull { it.id == id }
    }
    val equippedTitle = profile.inventory.equippedTitleId?.let { id ->
        RpgProgressionEngine.inventoryCatalog.firstOrNull { it.id == id }
    }
    val equippedBadge = profile.inventory.equippedBadgeId?.let { id ->
        RpgProgressionEngine.inventoryCatalog.firstOrNull { it.id == id }
    }
    val borderColor = equippedFrame?.let { Color(it.accentHexLong) } ?: MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (equippedFrame != null) 2.dp else 1.dp

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier.fillMaxWidth()
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
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(2.dp, borderColor),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${level.level}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = targetEmployee.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${status.titleRu} • ${targetEmployee.role.shortTitleRu}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (profile.prestige.prestigeLevel > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFD97706).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = profile.prestige.emblemTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (equippedTitle != null || equippedBadge != null) {
                            Text(
                                text = listOfNotNull(equippedTitle?.nameRu, equippedBadge?.nameRu).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF7C3AED),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        val cleanSub = com.example.data.seed.CorporateSeedData.sanitizeSubdivisionName(targetEmployee.groupName)
                        val cleanPos = com.example.data.seed.CorporateSeedData.sanitizePositionName(targetEmployee.position, targetEmployee.role)
                        val cleanSite = com.example.data.seed.CorporateSeedData.sanitizeSiteOrLocation(
                            raw = targetEmployee.siteName,
                            fallbackSubdivision = cleanSub
                        )
                        Text(
                            text = "${com.example.data.seed.CorporateSeedData.DEFAULT_ORG_NAME}, $cleanSub, $cleanPos, Участок: $cleanSite",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF15803D).copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${profile.skillPointsAvailable} SP",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF15803D)
                        )
                        Text(
                            text = "навыки",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (level.isMaxLevel) {
                            "Макс. уровень ${level.level}/${config.maxLevel}"
                        } else {
                            "Уровень ${level.level} → ${level.level + 1}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (level.isMaxLevel) {
                            "${level.totalXp} XP • Сезон: ${profile.seasonXp} XP"
                        } else {
                            "${level.totalXp} / ${level.nextLevelXp} XP (Сезон: ${profile.seasonXp})"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { level.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "Бонус ранга: ${status.perkRu}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (canAdjust || (isSelf && level.isMaxLevel)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSelf && level.isMaxLevel) {
                        Button(
                            onClick = onRequestPrestige,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("rpg_prestige_button")
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Престиж")
                        }
                    }
                    if (canAdjust) {
                        OutlinedButton(
                            onClick = onRequestAdjust,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("rpg_manual_adjust_button")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Корректировка XP/Реп.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RpgSubtabSwitcher(
    selectedSubtab: Int,
    onSelect: (Int) -> Unit
) {
    val tabs = listOf(
        "Серия и Инвентарь",
        "Навыки (5 веток)",
        "Квесты и Награды",
        "Рейтинг и Лог"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            FilterChip(
                selected = selectedSubtab == index,
                onClick = { onSelect(index) },
                label = { Text(title, maxLines = 1) },
                modifier = Modifier.testTag("rpg_subtab_$index")
            )
        }
    }
}

@Composable
private fun RpgProfileAndStreakTab(
    profile: EmployeeRpgProfile,
    config: GamificationConfig,
    isSelf: Boolean,
    onUseStreakRecovery: () -> Unit,
    onEquipItem: (String) -> Unit
) {
    val streakMult = RpgProgressionEngine.streakMultiplierFor(profile.streak.currentDays, config)
    val repColor = when {
        profile.reputation.score >= 75 -> Color(0xFF15803D)
        profile.reputation.score >= 45 -> Color(0xFFD97706)
        else -> Color(0xFFB91C1C)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Streak & Reputation Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (profile.streak.isAtRisk) Color(0xFFD97706) else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = "Серия смен",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Серия смен", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "${profile.streak.currentDays} дн. (×$streakMult)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD97706)
                    )
                    Text(
                        text = "Рекорд: ${profile.streak.bestDays} дн. • Жетоны: ${profile.inventory.streakRecoveryTokens}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (profile.streak.isAtRisk) {
                        Text(
                            text = "Под угрозой сброса!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB91C1C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Репутация",
                            tint = repColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Репутация", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "${profile.reputation.score} / 100",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = repColor
                    )
                    Text(
                        text = profile.reputation.tierTitleRu,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isSelf && profile.streak.isAtRisk && profile.inventory.streakRecoveryTokens > 0) {
            Button(
                onClick = onUseStreakRecovery,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("rpg_recover_streak_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Восстановить серию (-1 жетон)")
            }
        }

        if (profile.reputation.history.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "История репутации (${profile.reputation.history.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    profile.reputation.history.take(5).forEach { entry ->
                        val deltaStr = if (entry.delta >= 0) "+${entry.delta}" else "${entry.delta}"
                        val deltaClr = if (entry.delta >= 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.reason.ifBlank { "Изменение репутации" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$deltaStr (→ ${entry.resultingScore})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = deltaClr
                            )
                        }
                    }
                }
            }
        }

        // Inventory / Cosmetics Section
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Инвентарь и персонализация (${profile.inventory.ownedItemIds.size}/${RpgProgressionEngine.inventoryCatalog.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                RpgProgressionEngine.inventoryCatalog.forEach { item ->
                    val owned = item.id in profile.inventory.ownedItemIds
                    val equipped = when (item.category) {
                        InventoryItemCategory.BADGE -> profile.inventory.equippedBadgeId == item.id
                        InventoryItemCategory.AVATAR_FRAME -> profile.inventory.equippedFrameId == item.id
                        InventoryItemCategory.TITLE -> profile.inventory.equippedTitleId == item.id
                        InventoryItemCategory.THEME -> false
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (owned) {
                            Color(item.accentHexLong).copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (equipped) Color(item.accentHexLong) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.nameRu,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (owned) Color(item.accentHexLong) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "• ${item.category.titleRu}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = item.descriptionRu,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (owned && isSelf) {
                                OutlinedButton(
                                    onClick = { onEquipItem(item.id) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .testTag("rpg_equip_${item.id}")
                                ) {
                                    Text(if (equipped) "Снять" else "Надеть", style = MaterialTheme.typography.labelSmall)
                                }
                            } else if (!owned) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Не получено",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RpgSkillsTreeTab(
    targetEmployee: CorporateEmployee,
    profile: EmployeeRpgProfile,
    rpgState: RpgWorkspaceState,
    isSelf: Boolean,
    onUpgradeSkill: (String) -> Unit,
    onRequestReset: () -> Unit
) {
    val defaultService = remember(targetEmployee) {
        RpgProgressionEngine.resolveEmployeeServiceSpecialization(targetEmployee)
    }
    var selectedService by remember(defaultService) { mutableStateOf(defaultService) }

    val canResetThisSeason = isSelf &&
        profile.skillPointsSpent > 0 &&
        profile.seasonSkillResetDoneForSeasonId != rpgState.currentSeason.id

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
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
                            text = "Дерево навыков (Доступно: ${profile.skillPointsAvailable} SP)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Авто-распределение опыта от типа задач (физ. труд, АСУТП/электрика, документация, ТБ/ирония) + ручная прокачка за SP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelf) {
                        OutlinedButton(
                            onClick = onRequestReset,
                            enabled = canResetThisSeason,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("rpg_reset_skills_button")
                        ) {
                            Text(
                                text = if (profile.seasonSkillResetDoneForSeasonId == rpgState.currentSeason.id) {
                                    "Сброс исп."
                                } else {
                                    "Сброс (-${rpgState.config.seasonalResetSpPenalty} SP)"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "Специализация службы (адаптация названий навыков):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ServiceSpecialization.entries.forEach { svc ->
                        FilterChip(
                            selected = selectedService == svc,
                            onClick = { selectedService = svc },
                            label = { Text(svc.shortTitleRu, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("rpg_service_chip_${svc.code}")
                        )
                    }
                }
                Text(
                    text = "${selectedService.fullTitleRu}: ${selectedService.descriptionRu}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        SkillBranch.entries.forEach { branch ->
            val branchSkills = RpgProgressionEngine.skillCatalog.filter { it.branch == branch }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = branch.titleRu,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = branch.subtitleRu,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    branchSkills.forEach { def ->
                        val currentLvl = profile.skills.firstOrNull { it.skillId == def.id }?.level ?: 0
                        val prereqOk = def.prerequisiteSkillId == null ||
                            (profile.skills.firstOrNull { it.skillId == def.prerequisiteSkillId }?.level ?: 0) >= 1
                        val roleOk = targetEmployee.role.hierarchyWeight >= def.minRoleWeight
                        val canUpgrade = isSelf &&
                            profile.skillPointsAvailable > 0 &&
                            currentLvl < def.maxLevel &&
                            prereqOk &&
                            roleOk
                        val resolvedTitle = def.titleForService(selectedService)
                        val resolvedDesc = def.descriptionForService(selectedService)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (def.isHumorousOrPenaltySkill) {
                                Color(0xFFD97706).copy(alpha = 0.08f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$resolvedTitle ($currentLvl/${def.maxLevel})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (def.isHumorousOrPenaltySkill) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFD97706).copy(alpha = 0.16f)
                                            ) {
                                                Text(
                                                    text = "Ирония / Авто",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFB45309),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = resolvedDesc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!roleOk) {
                                        Text(
                                            text = "Требуется роль ИТР и выше",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFB91C1C)
                                        )
                                    } else if (!prereqOk && def.prerequisiteSkillId != null) {
                                        val reqTitle = RpgProgressionEngine.skillCatalog
                                            .firstOrNull { it.id == def.prerequisiteSkillId }
                                            ?.titleForService(selectedService)
                                            ?: def.prerequisiteSkillId
                                        Text(
                                            text = "Требуется базовый навык: $reqTitle",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }
                                if (isSelf) {
                                    Button(
                                        onClick = { onUpgradeSkill(def.id) },
                                        enabled = canUpgrade,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .heightIn(min = 40.dp)
                                            .testTag("rpg_upgrade_${def.id}")
                                    ) {
                                        Text(
                                            text = if (currentLvl >= def.maxLevel) "Макс" else "+1 ур.",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RpgQuestsAndAchievementsTab(
    targetEmployee: CorporateEmployee,
    profile: EmployeeRpgProfile,
    rpgState: RpgWorkspaceState,
    currentEpochDay: Long
) {
    val weekNum = currentEpochDay / 7L
    val availableQuests = RpgProgressionEngine.questCatalog.filter {
        targetEmployee.role.hierarchyWeight >= it.minRoleWeight
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Активные квесты", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                availableQuests.forEach { quest ->
                    val periodKey = when (quest.cadence) {
                        QuestCadence.DAILY -> "D_$currentEpochDay"
                        QuestCadence.WEEKLY -> "W_$weekNum"
                        QuestCadence.SEASONAL -> "S_${rpgState.currentSeason.id}"
                        QuestCadence.STORY -> "STORY"
                    }
                    val qp = profile.questProgress.firstOrNull { it.questId == quest.id && it.periodKey == periodKey }
                    val curCount = (qp?.currentCount ?: 0).coerceAtMost(quest.targetCount)
                    val done = qp?.rewarded == true
                    QuestRowCard(quest = quest, currentCount = curCount, done = done)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val unlockedMap = profile.achievements.associateBy { it.achievementId }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFD97706))
                    Text(
                        text = "Достижения (${unlockedMap.size}/${RpgProgressionEngine.achievementCatalog.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                RpgProgressionEngine.achievementCatalog.forEach { ach ->
                    AchievementRowCard(
                        achievement = ach,
                        unlocked = unlockedMap.containsKey(ach.id)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestRowCard(
    quest: Quest,
    currentCount: Int,
    done: Boolean
) {
    val fraction = (currentCount.toFloat() / quest.targetCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (done) Color(0xFF15803D).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${quest.cadence.titleRu}: ${quest.titleRu}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (done) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Выполнено", tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        text = "+${quest.xpReward} XP",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = quest.descriptionRu,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (done) Color(0xFF15803D) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$currentCount/${quest.targetCount}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AchievementRowCard(
    achievement: Achievement,
    unlocked: Boolean
) {
    val rarityColor = Color(achievement.rarity.hexColor)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (unlocked) rarityColor.copy(alpha = 0.09f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, if (unlocked) rarityColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (unlocked) Icons.Default.MilitaryTech else Icons.Default.Lock,
                contentDescription = null,
                tint = if (unlocked) rarityColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = achievement.titleRu,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = rarityColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = achievement.rarity.titleRu,
                            style = MaterialTheme.typography.labelSmall,
                            color = rarityColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = achievement.descriptionRu,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "+${achievement.xpReward} XP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (unlocked) rarityColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RpgLeaderboardAndLogTab(
    viewer: CorporateEmployee,
    targetEmployee: CorporateEmployee,
    availableEmployees: List<CorporateEmployee>,
    profile: EmployeeRpgProfile,
    rpgState: RpgWorkspaceState,
    isSelf: Boolean,
    onSetPrivacy: (LeaderboardPrivacy) -> Unit
) {
    val leaderboard = remember(rpgState, viewer, availableEmployees) {
        RpgProgressionEngine.buildLeaderboard(rpgState, viewer, availableEmployees)
    }
    val empTransactions = remember(rpgState.transactions, targetEmployee.id, targetEmployee.email) {
        val canonicalId = com.example.data.model.SiteMemberDocument.canonicalMemberId(targetEmployee.id, targetEmployee.email)
        val normId = targetEmployee.id.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val validIds = setOf(targetEmployee.id, canonicalId, normId)
        rpgState.transactions.filter { it.employeeId in validIds }.take(25)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Season + Privacy
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706))
                    Text(rpgState.currentSeason.titleRu, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                if (isSelf) {
                    Text(
                        text = "Приватность в сезонном рейтинге:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LeaderboardPrivacy.entries.forEach { priv ->
                            FilterChip(
                                selected = profile.leaderboardPrivacy == priv,
                                onClick = { onSetPrivacy(priv) },
                                label = { Text(priv.titleRu, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("rpg_privacy_${priv.code}")
                            )
                        }
                    }
                }
            }
        }

        // Leaderboard Table
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Leaderboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Рейтинг персонала (${leaderboard.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (leaderboard.isEmpty()) {
                    Text(
                        text = "Доступные участники скрыли отображение в рейтинге.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    leaderboard.take(20).forEachIndexed { idx, entry ->
                        val medalColor = when (idx) {
                            0 -> Color(0xFFD97706)
                            1 -> Color(0xFF64748B)
                            2 -> Color(0xFFB45309)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (entry.employeeId == targetEmployee.id) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = medalColor
                                    )
                                    Column {
                                        Text(
                                            text = entry.employeeName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${entry.rankTitle} • Ур.${entry.level} • Реп.${entry.reputation} • Серия ${entry.streakDays}д",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "${entry.seasonXp} XP",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // XP Ledger
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Журнал транзакций XP (${empTransactions.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (empTransactions.isEmpty()) {
                    Text(
                        text = "Пока нет начислений XP. Завершите или подтвердите задачу, чтобы получить опыт.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    empTransactions.forEach { tx ->
                        XpTransactionRow(tx = tx)
                    }
                }
            }
        }
    }
}

@Composable
private fun XpTransactionRow(tx: XpTransaction) {
    val timeText = remember(tx.createdAtMillis) {
        SimpleDateFormat("dd.MM HH:mm", Locale.forLanguageTag("ru-RU")).format(Date(tx.createdAtMillis))
    }
    val xpColor = when {
        tx.isRolledBack -> MaterialTheme.colorScheme.onSurfaceVariant
        tx.xpDelta > 0 -> Color(0xFF15803D)
        tx.xpDelta < 0 -> Color(0xFFB91C1C)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val xpSign = if (tx.xpDelta > 0) "+${tx.xpDelta}" else "${tx.xpDelta}"

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tx.type.titleRu + if (tx.isRolledBack) " [ОТКАЧЕНО]" else "",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$xpSign XP",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = xpColor
                )
            }
            if (tx.reason.isNotBlank()) {
                Text(
                    text = tx.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            val metaParts = listOfNotNull(
                timeText,
                tx.priorityAtCompletion?.let { "Приоритет: ${it.titleRu}" },
                tx.appliedMultipliersSummary.takeIf { it.isNotBlank() },
                if (tx.reputationDelta != 0) "Реп: ${if (tx.reputationDelta > 0) "+${tx.reputationDelta}" else tx.reputationDelta}" else null
            )
            Text(
                text = metaParts.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminGamificationSection(
    activeEmployee: CorporateEmployee,
    availableEmployees: List<CorporateEmployee>,
    rpgState: RpgWorkspaceState,
    onUpdateGamificationConfig: (GamificationConfig) -> Unit,
    onAdjustEmployeeRpg: (CorporateEmployee, Int, Int, String) -> Unit
) {
    if (!RpgProgressionEngine.canAdjustProgression(activeEmployee)) return

    var expanded by remember { mutableStateOf(false) }
    var adjustTarget by remember { mutableStateOf<CorporateEmployee?>(null) }

    val cfg = rpgState.config
    var xpLow by remember(cfg) { mutableStateOf(cfg.ruleFor(TaskPriority.LOW).baseXp.toString()) }
    var xpMedium by remember(cfg) { mutableStateOf(cfg.ruleFor(TaskPriority.MEDIUM).baseXp.toString()) }
    var xpHigh by remember(cfg) { mutableStateOf(cfg.ruleFor(TaskPriority.HIGH).baseXp.toString()) }
    var xpCritical by remember(cfg) { mutableStateOf(cfg.ruleFor(TaskPriority.CRITICAL).baseXp.toString()) }
    var xpEmergency by remember(cfg) { mutableStateOf(cfg.ruleFor(TaskPriority.EMERGENCY).baseXp.toString()) }
    var maxLevelText by remember(cfg) { mutableStateOf(cfg.maxLevel.toString()) }
    var overduePenaltyPct by remember(cfg) { mutableStateOf((cfg.overduePenaltyRate * 100).roundToInt().toString()) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .testTag("admin_gamification_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            text = "Геймификация и правила XP (Админ)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Веса приоритетов (incl. Аварийный), штрафы и ручная коррекция",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "Базовый XP за задачу по приоритетам:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = xpLow,
                        onValueChange = { xpLow = it.filter { c -> c.isDigit() } },
                        label = { Text("Низк.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = xpMedium,
                        onValueChange = { xpMedium = it.filter { c -> c.isDigit() } },
                        label = { Text("Сред.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = xpHigh,
                        onValueChange = { xpHigh = it.filter { c -> c.isDigit() } },
                        label = { Text("Выс.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = xpCritical,
                        onValueChange = { xpCritical = it.filter { c -> c.isDigit() } },
                        label = { Text("Крит.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = xpEmergency,
                        onValueChange = { xpEmergency = it.filter { c -> c.isDigit() } },
                        label = { Text("Авар.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxLevelText,
                        onValueChange = { maxLevelText = it.filter { c -> c.isDigit() } },
                        label = { Text("Макс.ур") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = overduePenaltyPct,
                        onValueChange = { overduePenaltyPct = it.filter { c -> c.isDigit() } },
                        label = { Text("Штраф%") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val nextRules = cfg.priorityRules.toMutableMap()
                        nextRules[TaskPriority.LOW] = cfg.ruleFor(TaskPriority.LOW).copy(baseXp = xpLow.toIntOrNull()?.coerceAtLeast(1) ?: 5)
                        nextRules[TaskPriority.MEDIUM] = cfg.ruleFor(TaskPriority.MEDIUM).copy(baseXp = xpMedium.toIntOrNull()?.coerceAtLeast(1) ?: 10)
                        nextRules[TaskPriority.HIGH] = cfg.ruleFor(TaskPriority.HIGH).copy(baseXp = xpHigh.toIntOrNull()?.coerceAtLeast(1) ?: 20)
                        nextRules[TaskPriority.CRITICAL] = cfg.ruleFor(TaskPriority.CRITICAL).copy(baseXp = xpCritical.toIntOrNull()?.coerceAtLeast(1) ?: 35)
                        nextRules[TaskPriority.EMERGENCY] = cfg.ruleFor(TaskPriority.EMERGENCY).copy(baseXp = xpEmergency.toIntOrNull()?.coerceAtLeast(1) ?: 50)
                        val updatedCfg = cfg.copy(
                            priorityRules = nextRules,
                            maxLevel = maxLevelText.toIntOrNull()?.coerceIn(5, 50) ?: cfg.maxLevel,
                            overduePenaltyRate = ((overduePenaltyPct.toIntOrNull() ?: 50).coerceIn(0, 90)) / 100.0
                        )
                        onUpdateGamificationConfig(updatedCfg)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("admin_save_gamification_config_button")
                ) {
                    Text("Сохранить правила XP")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "Ручная корректировка XP / Репутации сотрудника:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableEmployees.forEach { emp ->
                        OutlinedButton(
                            onClick = { adjustTarget = emp },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("admin_adjust_emp_${emp.id}")
                        ) {
                            Text(emp.shortName, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    adjustTarget?.let { target ->
        ManualRpgAdjustDialog(
            targetEmployee = target,
            onDismiss = { adjustTarget = null },
            onConfirm = { xpDelta, repDelta, reason ->
                onAdjustEmployeeRpg(target, xpDelta, repDelta, reason)
                adjustTarget = null
            }
        )
    }
}

@Composable
fun ManualRpgAdjustDialog(
    targetEmployee: CorporateEmployee,
    onDismiss: () -> Unit,
    onConfirm: (xpDelta: Int, reputationDelta: Int, reason: String) -> Unit
) {
    var xpDeltaText by remember { mutableStateOf("25") }
    var repDeltaText by remember { mutableStateOf("0") }
    var reason by remember { mutableStateOf("") }

    val parsedXp = xpDeltaText.trim().toIntOrNull()
    val parsedRep = repDeltaText.trim().toIntOrNull()
    val canSubmit = parsedXp != null && parsedRep != null && reason.trim().isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Корректировка: ${targetEmployee.shortName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Изменение фиксируется в журнале аудита и транзакций XP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = xpDeltaText,
                        onValueChange = { xpDeltaText = it },
                        label = { Text("XP (+/-)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("adjust_xp_input")
                    )
                    OutlinedTextField(
                        value = repDeltaText,
                        onValueChange = { repDeltaText = it },
                        label = { Text("Репутация (+/-)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("adjust_rep_input")
                    )
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Причина (обязательно) *") },
                    placeholder = { Text("Например: премия за ликвидацию аварии") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adjust_reason_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSubmit) {
                        onConfirm(parsedXp!!, parsedRep!!, reason.trim())
                    }
                },
                enabled = canSubmit,
                modifier = Modifier.testTag("confirm_adjust_rpg_button")
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
