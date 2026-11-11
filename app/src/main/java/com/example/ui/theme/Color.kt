package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// 1. CLASSIC LIGHT ("Чистый свет" — Linear / Stripe Corporate Light)
// ============================================================================
val ClassicLightBackground = Color(0xFFF8F9FA)
val ClassicLightSurface = Color(0xFFFFFFFF)
val ClassicLightSurfaceVariant = Color(0xFFF1F4F8)
val ClassicLightTextPrimary = Color(0xFF18181B)   // Глубокий графит (не #000000)
val ClassicLightTextSecondary = Color(0xFF52525B) // Сбалансированный нейтральный серый
val ClassicLightTextMuted = Color(0xFF94A3B8)
val ClassicLightAccentBlue = Color(0xFF2563EB)    // Сдержанный корпоративный синий
val ClassicLightAccentContainer = Color(0xFFEEF4FF)
val ClassicLightOnAccentContainer = Color(0xFF1E3A8A)
val ClassicLightOutline = Color(0xFFE2E8F0)
val ClassicLightOutlineSubtle = Color(0xFFEDF0F5) // Мягкий разделитель вместо жёстких синих рамок
val ClassicLightStatusDone = Color(0xFF16A34A)

// ============================================================================
// 2. OBSIDIAN DARK ("Глубокий космос" — Linear / Raycast Pro Dark + Glass)
// ============================================================================
val ObsidianBackground = Color(0xFF0F1115)        // Глубокий тёмно-синий/угольный
val ObsidianSurface = Color(0xFF1A1D24)           // Карточки на 1.5 тона светлее фона
val ObsidianSurfaceVariant = Color(0xFF232731)
val ObsidianTextPrimary = Color(0xFFF3F4F6)       // Мягкий белый без засвета
val ObsidianTextSecondary = Color(0xFF9CA3AF)     // Спокойный серебристо-серый
val ObsidianAccentBlue = Color(0xFF3B82F6)        // Неоново-синий акцент
val ObsidianAccentEmerald = Color(0xFF10B981)     // Изумрудный акцент прогресса
val ObsidianAccentContainer = Color(0xFF172554)
val ObsidianOnAccentContainer = Color(0xFFDBEAFE)
val ObsidianGlassBorder = Color(0xFF2B313E)       // Тонкая стеклянная грань (1px hairline)
val ObsidianOutline = Color(0xFF374151)

// ============================================================================
// 3. TACTICAL FIELD ("Полевая / Индустриальная" — Антибликовый режим для стройки)
// ============================================================================
val TacticalBackground = Color(0xFF141715)        // Тёмный асфальт / графитовый хаки
val TacticalSurface = Color(0xFF1E2320)           // Плоские контрастные карточки без теней
val TacticalSurfaceVariant = Color(0xFF282F2B)
val TacticalTextPrimary = Color(0xFFFFFFFF)       // Максимальный контраст на ярком солнце
val TacticalTextSecondary = Color(0xFFD4DBD7)     // Усиленная читаемость вторичного текста
val TacticalAccentAmber = Color(0xFFF59E0B)       // Сигнальный янтарно-оранжевый (главные действия)
val TacticalOnAmber = Color(0xFF111312)           // Чёрно-угольный текст на янтарной кнопке (WCAG AAA)
val TacticalAccentContainer = Color(0xFF3B2806)
val TacticalOnAccentContainer = Color(0xFFFEF3C7)
val TacticalStatusLime = Color(0xFF84CC16)        // Яркий лаймовый для статуса «Выполнено»
val TacticalBorderStrong = Color(0xFF3D4741)      // Чёткая техническая грань без размытия

// ============================================================================
// 4. WARM PAPER ("Инженерная бумага" — Тёплый камень в стиле Notion)
// ============================================================================
val PaperBackground = Color(0xFFF4F3EF)
val PaperSurface = Color(0xFFFFFEFC)
val PaperSurfaceVariant = Color(0xFFEBE9E2)
val PaperTextPrimary = Color(0xFF1C1917)
val PaperTextSecondary = Color(0xFF57534E)
val PaperAccentIndigo = Color(0xFF4F46E5)
val PaperAccentContainer = Color(0xFFEEF2FF)
val PaperOnAccentContainer = Color(0xFF312E81)
val PaperOutlineSubtle = Color(0xFFE5E2DA)
val PaperStatusDone = Color(0xFF059669)

// ============================================================================
// Обратная совместимость базовых констант
// ============================================================================
val SlatePrimaryLight = ClassicLightTextPrimary
val AccentBlue = ClassicLightAccentBlue
val AccentBlueDark = ObsidianAccentBlue
val TealAccent = Color(0xFF0D9488)
val EmeraldVerified = ClassicLightStatusDone
val CrimsonAlert = Color(0xFFDC2626)
val AmberWarning = Color(0xFFD97706)

val SurfaceLight = ClassicLightBackground
val CardLight = ClassicLightSurface
val SurfaceVariantLight = ClassicLightSurfaceVariant
val OutlineLight = ClassicLightOutline

val SurfaceDark = ObsidianBackground
val CardDark = ObsidianSurface
val SurfaceVariantDark = ObsidianSurfaceVariant
val OutlineDark = ObsidianOutline

val TextPrimaryLight = ClassicLightTextPrimary
val TextSecondaryLight = ClassicLightTextSecondary
val TextPrimaryDark = ObsidianTextPrimary
val TextSecondaryDark = ObsidianTextSecondary
