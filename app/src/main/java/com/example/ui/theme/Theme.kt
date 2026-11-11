package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 3 премиальные темы оформления строительного ежедневника (в стиле Linear, Notion, Apple)
 * + переключатель «Системная тема».
 */
enum class AppThemeMode(
    val code: String,
    val titleEn: String,
    val titleRu: String,
    val badgeLabel: String,
    val vibeDescription: String,
    val isDarkPalette: Boolean,
    val previewBgColor: Color,
    val previewCardColor: Color,
    val previewTextPrimary: Color,
    val previewTextSecondary: Color,
    val previewAccentColor: Color,
    val previewDoneColor: Color,
    val previewBorderColor: Color
) {
    CLASSIC_LIGHT(
        code = "CLASSIC_LIGHT",
        titleEn = "Classic Light",
        titleRu = "Чистый свет",
        badgeLabel = "Linear • Без рамок",
        vibeDescription = "Светло-серый фон (#F5F7FA), чисто белые карточки (#FFFFFF) с мягкими тенями без жёстких обводок, графитовый текст и спокойный синий акцент.",
        isDarkPalette = false,
        previewBgColor = ClassicLightBackground,
        previewCardColor = ClassicLightSurface,
        previewTextPrimary = ClassicLightTextPrimary,
        previewTextSecondary = ClassicLightTextSecondary,
        previewAccentColor = ClassicLightAccentBlue,
        previewDoneColor = ClassicLightStatusDone,
        previewBorderColor = Color.Transparent
    ),
    OBSIDIAN_DARK(
        code = "OBSIDIAN_DARK",
        titleEn = "Obsidian Dark",
        titleRu = "Глубокий космос",
        badgeLabel = "Pro Dark • Glass",
        vibeDescription = "Глубокий тёмный фон (#0B0D12), карточки с эффектом матового стекла (Glassmorphism) и мягкими светящимися акцентами.",
        isDarkPalette = true,
        previewBgColor = ObsidianBackground,
        previewCardColor = ObsidianSurface,
        previewTextPrimary = ObsidianTextPrimary,
        previewTextSecondary = ObsidianTextSecondary,
        previewAccentColor = ObsidianAccentBlue,
        previewDoneColor = ObsidianAccentEmerald,
        previewBorderColor = ObsidianGlassBorder
    ),
    TACTICAL_FIELD(
        code = "TACTICAL_FIELD",
        titleEn = "Field Mode",
        titleRu = "Стройплощадка",
        badgeLabel = "Антиблик • Солнце",
        vibeDescription = "Высококонтрастный режим для яркого солнца: антибликовый тёмный фон, сигнальные оранжево-жёлтые акценты (#F59E0B) и ярко-зелёный статус «Выполнено».",
        isDarkPalette = true,
        previewBgColor = TacticalBackground,
        previewCardColor = TacticalSurface,
        previewTextPrimary = TacticalTextPrimary,
        previewTextSecondary = TacticalTextSecondary,
        previewAccentColor = TacticalAccentAmber,
        previewDoneColor = TacticalStatusLime,
        previewBorderColor = TacticalBorderStrong
    );

    companion object {
        fun fromCode(code: String?): AppThemeMode =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: CLASSIC_LIGHT
    }
}

@Immutable
data class AppThemeDesignTokens(
    val mode: AppThemeMode = AppThemeMode.CLASSIC_LIGHT,
    val isDark: Boolean = false,
    val cardShadowElevationDp: Float = 4f,
    val cardBorderWidthDp: Float = 0f,
    val cardBorderColor: Color = Color.Transparent,
    val useGlassmorphism: Boolean = false,
    val useButtonGlow: Boolean = false,
    val isHighContrastField: Boolean = false,
    val statusDoneColor: Color = ClassicLightStatusDone,
    val statusInProgressColor: Color = Color(0xFFD97706),
    val buttonCornerRadiusDp: Int = 14
)

val LocalAppThemeTokens = staticCompositionLocalOf { AppThemeDesignTokens() }

@Immutable
data class ThemeSettingsState(
    val followSystemTheme: Boolean = false,
    val selectedTheme: AppThemeMode = AppThemeMode.CLASSIC_LIGHT
)

/**
 * Реактивный менеджер темы приложения с сохранением в SharedPreferences.
 * Позволяет мгновенно переключать тему вручную в «Настройки -> Оформление (Темы)»,
 * игнорируя системную тему телефона или следуя ей по выбору пользователя.
 */
object ThemePreferencesManager {
    private const val PREFS_NAME = "ezhednevnik_theme_prefs_v1"
    private const val KEY_FOLLOW_SYSTEM = "follow_system_theme"
    private const val KEY_SELECTED_THEME = "selected_theme_mode"

    @Volatile
    private var isInitialized = false

    private val _state = MutableStateFlow(ThemeSettingsState())
    val state: StateFlow<ThemeSettingsState> = _state.asStateFlow()

    fun ensureInitialized(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val followSys = prefs.getBoolean(KEY_FOLLOW_SYSTEM, false)
            val savedMode = AppThemeMode.fromCode(prefs.getString(KEY_SELECTED_THEME, AppThemeMode.CLASSIC_LIGHT.code))
            _state.value = ThemeSettingsState(
                followSystemTheme = followSys,
                selectedTheme = savedMode
            )
            isInitialized = true
        }
    }

    fun setFollowSystemTheme(context: Context, followSystem: Boolean) {
        ensureInitialized(context)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FOLLOW_SYSTEM, followSystem).apply()
        _state.value = _state.value.copy(followSystemTheme = followSystem)
    }

    fun selectManualTheme(context: Context, themeMode: AppThemeMode) {
        ensureInitialized(context)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_FOLLOW_SYSTEM, false)
            .putString(KEY_SELECTED_THEME, themeMode.code)
            .apply()
        _state.value = ThemeSettingsState(
            followSystemTheme = false,
            selectedTheme = themeMode
        )
    }
}

private val ClassicLightColorScheme = lightColorScheme(
    primary = ClassicLightAccentBlue,
    onPrimary = Color.White,
    primaryContainer = ClassicLightAccentContainer,
    onPrimaryContainer = ClassicLightOnAccentContainer,
    secondary = Color(0xFF334155),
    onSecondary = Color.White,
    secondaryContainer = ClassicLightSurfaceVariant,
    onSecondaryContainer = ClassicLightTextPrimary,
    tertiary = ClassicLightStatusDone,
    onTertiary = Color.White,
    background = ClassicLightBackground,
    onBackground = ClassicLightTextPrimary,
    surface = ClassicLightSurface,
    onSurface = ClassicLightTextPrimary,
    surfaceVariant = ClassicLightSurfaceVariant,
    onSurfaceVariant = ClassicLightTextSecondary,
    outline = ClassicLightOutline,
    outlineVariant = ClassicLightOutlineSubtle,
    error = CrimsonAlert,
    onError = Color.White
)

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = ObsidianAccentBlue,
    onPrimary = Color.White,
    primaryContainer = ObsidianAccentContainer,
    onPrimaryContainer = ObsidianOnAccentContainer,
    secondary = ObsidianAccentEmerald,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = ObsidianAccentEmerald,
    onTertiary = Color.White,
    background = ObsidianBackground,
    onBackground = ObsidianTextPrimary,
    surface = ObsidianSurface,
    onSurface = ObsidianTextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianTextSecondary,
    outline = ObsidianOutline,
    outlineVariant = ObsidianGlassBorder,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

private val TacticalFieldColorScheme = darkColorScheme(
    primary = TacticalAccentAmber,
    onPrimary = TacticalOnAmber,
    primaryContainer = TacticalAccentContainer,
    onPrimaryContainer = TacticalOnAccentContainer,
    secondary = TacticalStatusLime,
    onSecondary = TacticalOnAmber,
    secondaryContainer = Color(0xFF263812),
    onSecondaryContainer = Color(0xFFECFCCB),
    tertiary = TacticalStatusLime,
    onTertiary = TacticalOnAmber,
    background = TacticalBackground,
    onBackground = TacticalTextPrimary,
    surface = TacticalSurface,
    onSurface = TacticalTextPrimary,
    surfaceVariant = TacticalSurfaceVariant,
    onSurfaceVariant = TacticalTextSecondary,
    outline = TacticalBorderStrong,
    outlineVariant = TacticalBorderStrong,
    error = Color(0xFFFB7185),
    onError = Color(0xFF4C0519)
)

fun colorSchemeForMode(mode: AppThemeMode): ColorScheme = when (mode) {
    AppThemeMode.CLASSIC_LIGHT -> ClassicLightColorScheme
    AppThemeMode.OBSIDIAN_DARK -> ObsidianDarkColorScheme
    AppThemeMode.TACTICAL_FIELD -> TacticalFieldColorScheme
}

fun designTokensForMode(mode: AppThemeMode): AppThemeDesignTokens = when (mode) {
    AppThemeMode.CLASSIC_LIGHT -> AppThemeDesignTokens(
        mode = AppThemeMode.CLASSIC_LIGHT,
        isDark = false,
        cardShadowElevationDp = 4f,
        cardBorderWidthDp = 0f, // Никаких жестких обводок (borders) — разделение за счет фонов и мягких теней
        cardBorderColor = Color.Transparent,
        useGlassmorphism = false,
        useButtonGlow = false,
        isHighContrastField = false,
        statusDoneColor = ClassicLightStatusDone,
        statusInProgressColor = Color(0xFFD97706),
        buttonCornerRadiusDp = 14
    )
    AppThemeMode.OBSIDIAN_DARK -> AppThemeDesignTokens(
        mode = AppThemeMode.OBSIDIAN_DARK,
        isDark = true,
        cardShadowElevationDp = 2.5f,
        cardBorderWidthDp = 0.8f,
        cardBorderColor = ObsidianGlassBorder,
        useGlassmorphism = true,
        useButtonGlow = true,
        isHighContrastField = false,
        statusDoneColor = ObsidianAccentEmerald,
        statusInProgressColor = Color(0xFFF59E0B),
        buttonCornerRadiusDp = 14
    )
    AppThemeMode.TACTICAL_FIELD -> AppThemeDesignTokens(
        mode = AppThemeMode.TACTICAL_FIELD,
        isDark = true,
        cardShadowElevationDp = 0f,
        cardBorderWidthDp = 1f,
        cardBorderColor = TacticalBorderStrong,
        useGlassmorphism = false,
        useButtonGlow = false,
        isHighContrastField = true,
        statusDoneColor = TacticalStatusLime,
        statusInProgressColor = TacticalAccentAmber,
        buttonCornerRadiusDp = 14
    )
}

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        ThemePreferencesManager.ensureInitialized(context)
    }
    val themeSettings by ThemePreferencesManager.state.collectAsState()

    val effectiveMode = if (themeSettings.followSystemTheme) {
        if (darkTheme) AppThemeMode.OBSIDIAN_DARK else AppThemeMode.CLASSIC_LIGHT
    } else {
        themeSettings.selectedTheme
    }

    val colorScheme = colorSchemeForMode(effectiveMode)
    val tokens = designTokensForMode(effectiveMode)

    CompositionLocalProvider(LocalAppThemeTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
