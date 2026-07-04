package com.ares.analytics.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary Palette ──────────────────────────────────────────────────────────
/** Deep obsidian background — near-black with a cool blue undertone */
val AresBackground = Color(0xFF0D0F14)
/** Elevated surface — slightly lighter for cards and panels */
val AresSurface = Color(0xFF161A22)
/** Higher elevation surface — for modals and floating elements */
val AresSurfaceElevated = Color(0xFF1E2330)

// ── ARES Brand ───────────────────────────────────────────────────────────────
/** ARES signature red — vibrant, used sparingly for brand accents */
val AresRed = Color(0xFFE53935)
/** Darker red for pressed/active states */
val AresRedDark = Color(0xFFB71C1C)
/** Red with transparency for glows and overlays */
val AresRedGlow = Color(0x40E53935)

// ── Accent Colors ────────────────────────────────────────────────────────────
/** Cyan accent — primary interactive color */
val AresCyan = Color(0xFF00E5FF)
/** Darker cyan for hover/pressed states */
val AresCyanDark = Color(0xFF00B8D4)
/** Cyan with transparency for subtle highlights */
val AresCyanGlow = Color(0x3000E5FF)

/** Gold highlight — used for warnings, important metrics */
val AresGold = Color(0xFFFFD54F)
/** Amber warning state */
val AresAmber = Color(0xFFFFA726)

/** Success green — healthy status indicators */
val AresGreen = Color(0xFF66BB6A)
/** Error red — distinct from brand red, used for error states */
val AresError = Color(0xFFFF5252)

// ── Text Hierarchy ───────────────────────────────────────────────────────────
/** Primary text — high contrast marble white */
val AresTextPrimary = Color(0xFFE8ECF4)
/** Secondary text — medium contrast for labels and descriptions */
val AresTextSecondary = Color(0xFF9CA3B4)
/** Tertiary text — low contrast for hints and disabled states */
val AresTextTertiary = Color(0xFF5A6174)

// ── Borders & Dividers ───────────────────────────────────────────────────────
/** Subtle border for cards and panels */
val AresBorder = Color(0xFF6B7B98)
/** Brighter border for focused/hovered elements */
val AresBorderFocused = Color(0xFF8B9BB8)

// ── Glassmorphism ────────────────────────────────────────────────────────────
/** Translucent glass surface for overlay panels */
val AresGlass = Color(0x1AFFFFFF)
/** Glass border — subtle white edge */
val AresGlassBorder = Color(0x20FFFFFF)

// ── Alert State Colors ───────────────────────────────────────────────────────
/** Active alert — flashing red */
val AresAlertActive = AresError
/** Latched alert — static amber (recovered but unacknowledged) */
val AresAlertLatched = AresAmber
/** Triaged alert — dimmed (acknowledged by pit crew) */
val AresAlertTriaged = Color(0xFF5A6174)

// ── Trajectory Overlay Colors ────────────────────────────────────────────────
/** Planned path — cyan dashed line */
val AresPathPlanned = AresCyan
/** Actual driven path — gold solid line */
val AresPathActual = AresGold
/** Deviation < 2cm — acceptable */
val AresDeviationLow = AresGreen
/** Deviation 2-5cm — caution */
val AresDeviationMedium = AresAmber
/** Deviation > 5cm — critical */
val AresDeviationHigh = AresError
