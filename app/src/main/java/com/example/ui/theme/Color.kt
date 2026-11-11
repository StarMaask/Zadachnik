package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// 1. CLASSIC LIGHT (Премиальный B2B стиль: Linear / Apple / Stripe)
//    Никаких жёстких рамок — разделение за счёт разницы фонов (#F5F7FA и #FFFFFF)
//    и мягкой диффузной тени. Спокойные, благородные акценты без «ядовитости».
// ============================================================================
val ClassicLightBackground = Color(0xFFF5F7FA)       // Воздушный светло-серый холст
val ClassicLightSurface = Color(0xFFFFFFFF)          // Чисто белые карточки
val ClassicLightSurfaceVariant = Color(0xFFEEF1F6)   // Мягкая подложка для чипов и инпутов
val ClassicLightTextPrimary = Color(0xFF18181B)      // Глубокий графит (вместо жёсткого #000000)
val ClassicLightTextSecondary = Color(0xFF64748B)    // Второстепенные данные (исполнитель, дата)
val ClassicLightTextMuted = Color(0xFF94A3B8)
val ClassicLightAccentBlue = Color(0xFF2563EB)       // Благородный сапфирово-синий без кислоты
val ClassicLightAccentContainer = Color(0xFFEFF4FF)
val ClassicLightOnAccentContainer = Color(0xFF1E3A8A)
val ClassicLightOutline = Color(0xFFE2E8F0)
val ClassicLightOutlineSubtle = Color(0xFFF1F5F9)
val ClassicLightStatusDone = Color(0xFF10B981)       // Изумрудно-зелёный статус выполнения
val ClassicLightStatusProblem = Color(0xFFE11D48)    // Благородный карминовый (вместо ядовито-красного)

// ============================================================================
// 2. OBSIDIAN DARK (Глубокий тёмный фон + эффект матового стекла Glassmorphism)
// ============================================================================
val ObsidianBackground = Color(0xFF0B0D12)           // Глубокий обсидиановый космос
val ObsidianSurface = Color(0xFF151922)              // Матовое тёмное стекло карточек
val ObsidianSurfaceVariant = Color(0xFF1E2430)       // Вторичные контейнеры
val ObsidianTextPrimary = Color(0xFFF3F4F6)          // Мягкий белый без засвета сетчатки
val ObsidianTextSecondary = Color(0xFF94A3B8)        // Холодный стальной серый
val ObsidianAccentBlue = Color(0xFF4F7CFF)           // Неоново-кобальтовый с мягким свечением
val ObsidianAccentEmerald = Color(0xFF10B981)        // Изумрудный прогресс
val ObsidianAccentContainer = Color(0xFF172554)
val ObsidianOnAccentContainer = Color(0xFFDBEAFE)
val ObsidianGlassBorder = Color(0xFF262D3D)          // Тончайшая стеклянная грань (hairline)
val ObsidianOutline = Color(0xFF334155)

// ============================================================================
// 3. FIELD MODE (Высококонтрастная тема для стройплощадки и яркого солнца)
//    Тёмный антибликовый фон, янтарные акценты для кнопок, лаймовые для «Выполнено»
// ============================================================================
val TacticalBackground = Color(0xFF111412)           // Антибликовый тёмный асфальт
val TacticalSurface = Color(0xFF1B201D)              // Контрастные матовые панели
val TacticalSurfaceVariant = Color(0xFF252C28)
val TacticalTextPrimary = Color(0xFFFFFFFF)          // Максимальный контраст на солнце
val TacticalTextSecondary = Color(0xFFD1D9D4)
val TacticalAccentAmber = Color(0xFFF59E0B)          // Янтарный акцент для кнопок действий
val TacticalOnAmber = Color(0xFF111412)              // Тёмный текст на янтарной кнопке (WCAG AAA)
val TacticalAccentContainer = Color(0xFF3B2806)
val TacticalOnAccentContainer = Color(0xFFFEF3C7)
val TacticalStatusLime = Color(0xFF84CC16)           // Яркий лаймовый для статусов «Выполнено»
val TacticalBorderStrong = Color(0xFF343E38)

// ============================================================================
// Обратная совместимость констант
// ============================================================================
val SlatePrimaryLight = ClassicLightTextPrimary
val AccentBlue = ClassicLightAccentBlue
val AccentBlueDark = ObsidianAccentBlue
val TealAccent = Color(0xFF0D9488)
val EmeraldVerified = ClassicLightStatusDone
val CrimsonAlert = ClassicLightStatusProblem
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
