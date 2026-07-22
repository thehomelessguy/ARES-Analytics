package com.ares.analytics.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

// ── Theme State Holder ───────────────────────────────────────────────────────
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
object AresThemeSettings {
    /**
     * colorblindMode var.
     */
    var colorblindMode by mutableStateOf(false)
    /**
     * highContrastMode var.
     */
    var highContrastMode by mutableStateOf(false)
    /**
     * touchOptimizedMode var.
     */
    var touchOptimizedMode by mutableStateOf(false)

    /**
     * currentColors val.
     */
    val currentColors: AresColorPalette
        get() = getAresColors(colorblindMode, highContrastMode)
}

// ── Dynamic Color Lookups ───────────────────────────────────────────────────
/**
 * AresBackground val.
 */
val AresBackground: Color get() = AresThemeSettings.currentColors.background
/**
 * AresSurface val.
 */
val AresSurface: Color get() = AresThemeSettings.currentColors.surface
/**
 * AresSurfaceElevated val.
 */
val AresSurfaceElevated: Color get() = AresThemeSettings.currentColors.surfaceElevated

/**
 * AresRed val.
 */
val AresRed: Color get() = AresThemeSettings.currentColors.red
/**
 * AresRedDark val.
 */
val AresRedDark: Color get() = AresThemeSettings.currentColors.redDark
/**
 * AresRedGlow val.
 */
val AresRedGlow: Color get() = AresThemeSettings.currentColors.redGlow

/**
 * AresCyan val.
 */
val AresCyan: Color get() = AresThemeSettings.currentColors.cyan
/**
 * AresCyanDark val.
 */
val AresCyanDark: Color get() = AresThemeSettings.currentColors.cyanDark
/**
 * AresCyanGlow val.
 */
val AresCyanGlow: Color get() = AresThemeSettings.currentColors.cyanGlow

/**
 * AresGold val.
 */
val AresGold: Color get() = AresThemeSettings.currentColors.gold
/**
 * AresAmber val.
 */
val AresAmber: Color get() = AresThemeSettings.currentColors.amber

/**
 * AresGreen val.
 */
val AresGreen: Color get() = AresThemeSettings.currentColors.green
/**
 * AresError val.
 */
val AresError: Color get() = AresThemeSettings.currentColors.error

/**
 * AresTextPrimary val.
 */
val AresTextPrimary: Color get() = AresThemeSettings.currentColors.textPrimary
/**
 * AresTextSecondary val.
 */
val AresTextSecondary: Color get() = AresThemeSettings.currentColors.textSecondary
/**
 * AresTextTertiary val.
 */
val AresTextTertiary: Color get() = AresThemeSettings.currentColors.textTertiary

/**
 * AresBorder val.
 */
val AresBorder: Color get() = AresThemeSettings.currentColors.border
/**
 * AresBorderFocused val.
 */
val AresBorderFocused: Color get() = AresThemeSettings.currentColors.borderFocused

/**
 * AresGlass val.
 */
val AresGlass: Color get() = AresThemeSettings.currentColors.glass
/**
 * AresGlassBorder val.
 */
val AresGlassBorder: Color get() = AresThemeSettings.currentColors.glassBorder

/**
 * AresAlertActive val.
 */
val AresAlertActive: Color get() = AresThemeSettings.currentColors.alertActive
/**
 * AresAlertLatched val.
 */
val AresAlertLatched: Color get() = AresThemeSettings.currentColors.alertLatched
/**
 * AresAlertTriaged val.
 */
val AresAlertTriaged: Color get() = AresThemeSettings.currentColors.alertTriaged

/**
 * ModeLive val.
 */
val ModeLive: Color get() = AresThemeSettings.currentColors.modeLive
/**
 * ModeLiveGlow val.
 */
val ModeLiveGlow: Color get() = AresThemeSettings.currentColors.modeLiveGlow
/**
 * ModeRewind val.
 */
val ModeRewind: Color get() = AresThemeSettings.currentColors.modeRewind
/**
 * ModeRewindGlow val.
 */
val ModeRewindGlow: Color get() = AresThemeSettings.currentColors.modeRewindGlow
/**
 * ModeReplay val.
 */
val ModeReplay: Color get() = AresThemeSettings.currentColors.modeReplay
/**
 * ModeReplayGlow val.
 */
val ModeReplayGlow: Color get() = AresThemeSettings.currentColors.modeReplayGlow

/**
 * AresPathPlanned val.
 */
val AresPathPlanned: Color get() = AresThemeSettings.currentColors.pathPlanned
/**
 * AresPathActual val.
 */
val AresPathActual: Color get() = AresThemeSettings.currentColors.pathActual
/**
 * AresDeviationLow val.
 */
val AresDeviationLow: Color get() = AresThemeSettings.currentColors.deviationLow
/**
 * AresDeviationMedium val.
 */
val AresDeviationMedium: Color get() = AresThemeSettings.currentColors.deviationMedium
/**
 * AresDeviationHigh val.
 */
val AresDeviationHigh: Color get() = AresThemeSettings.currentColors.deviationHigh

// ── Color Palette Generator ──────────────────────────────────────────────────
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class AresColorPalette(
    /**
     * background val.
     */
    val background: Color,
    /**
     * surface val.
     */
    val surface: Color,
    /**
     * surfaceElevated val.
     */
    val surfaceElevated: Color,
    /**
     * red val.
     */
    val red: Color,
    /**
     * redDark val.
     */
    val redDark: Color,
    /**
     * redGlow val.
     */
    val redGlow: Color,
    /**
     * cyan val.
     */
    val cyan: Color,
    /**
     * cyanDark val.
     */
    val cyanDark: Color,
    /**
     * cyanGlow val.
     */
    val cyanGlow: Color,
    /**
     * gold val.
     */
    val gold: Color,
    /**
     * amber val.
     */
    val amber: Color,
    /**
     * green val.
     */
    val green: Color,
    /**
     * error val.
     */
    val error: Color,
    /**
     * textPrimary val.
     */
    val textPrimary: Color,
    /**
     * textSecondary val.
     */
    val textSecondary: Color,
    /**
     * textTertiary val.
     */
    val textTertiary: Color,
    /**
     * border val.
     */
    val border: Color,
    /**
     * borderFocused val.
     */
    val borderFocused: Color,
    /**
     * glass val.
     */
    val glass: Color,
    /**
     * glassBorder val.
     */
    val glassBorder: Color,
    /**
     * alertActive val.
     */
    val alertActive: Color,
    /**
     * alertLatched val.
     */
    val alertLatched: Color,
    /**
     * alertTriaged val.
     */
    val alertTriaged: Color,
    /**
     * modeLive val.
     */
    val modeLive: Color,
    /**
     * modeLiveGlow val.
     */
    val modeLiveGlow: Color,
    /**
     * modeRewind val.
     */
    val modeRewind: Color,
    /**
     * modeRewindGlow val.
     */
    val modeRewindGlow: Color,
    /**
     * modeReplay val.
     */
    val modeReplay: Color,
    /**
     * modeReplayGlow val.
     */
    val modeReplayGlow: Color,
    /**
     * pathPlanned val.
     */
    val pathPlanned: Color,
    /**
     * pathActual val.
     */
    val pathActual: Color,
    /**
     * deviationLow val.
     */
    val deviationLow: Color,
    /**
     * deviationMedium val.
     */
    val deviationMedium: Color,
    /**
     * deviationHigh val.
     */
    val deviationHigh: Color
)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun getAresColors(colorblind: Boolean, highContrast: Boolean): AresColorPalette {
    /**
     * textPrimary val.
     */
    val textPrimary = Color(0xFFE8ECF4)
    /**
     * textSecondary val.
     */
    val textSecondary = if (highContrast) Color(0xFFF3F5F9) else Color(0xFF9CA3B4)
    /**
     * textTertiary val.
     */
    val textTertiary = if (highContrast) Color(0xFFCAD0DE) else Color(0xFF5A6174)

    /**
     * green val.
     */
    val green = if (colorblind) Color(0xFF2979FF) else Color(0xFF66BB6A)
    /**
     * error val.
     */
    val error = if (colorblind) Color(0xFFFF6D00) else Color(0xFFFF5252)
    
    /**
     * gold val.
     */
    val gold = Color(0xFFFFD54F)
    /**
     * amber val.
     */
    val amber = Color(0xFFFFA726)

    /**
     * deviationLow val.
     */
    val deviationLow = green
    /**
     * deviationMedium val.
     */
    val deviationMedium = amber
    /**
     * deviationHigh val.
     */
    val deviationHigh = error

    /**
     * red val.
     */
    val red = if (colorblind) Color(0xFFFF6D00) else Color(0xFFE53935)
    /**
     * redDark val.
     */
    val redDark = if (colorblind) Color(0xFFE65100) else Color(0xFFB71C1C)
    /**
     * redGlow val.
     */
    val redGlow = if (colorblind) Color(0x40FF6D00) else Color(0x40E53935)

    /**
     * cyan val.
     */
    val cyan = Color(0xFF00E5FF)
    /**
     * cyanDark val.
     */
    val cyanDark = Color(0xFF00B8D4)
    /**
     * cyanGlow val.
     */
    val cyanGlow = Color(0x3000E5FF)

    /**
     * background val.
     */
    val background = Color(0xFF0D0F14)
    /**
     * surface val.
     */
    val surface = Color(0xFF161A22)
    /**
     * surfaceElevated val.
     */
    val surfaceElevated = Color(0xFF1E2330)

    /**
     * border val.
     */
    val border = if (highContrast) Color(0xFF8B9BB8) else Color(0xFF6B7B98)
    /**
     * borderFocused val.
     */
    val borderFocused = if (highContrast) Color(0xFFBACCDD) else Color(0xFF8B9BB8)

    /**
     * glass val.
     */
    val glass = Color(0x1AFFFFFF)
    /**
     * glassBorder val.
     */
    val glassBorder = Color(0x20FFFFFF)

    return AresColorPalette(
        background = background,
        surface = surface,
        surfaceElevated = surfaceElevated,
        red = red,
        redDark = redDark,
        redGlow = redGlow,
        cyan = cyan,
        cyanDark = cyanDark,
        cyanGlow = cyanGlow,
        gold = gold,
        amber = amber,
        green = green,
        error = error,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textTertiary = textTertiary,
        border = border,
        borderFocused = borderFocused,
        glass = glass,
        glassBorder = glassBorder,
        alertActive = error,
        alertLatched = amber,
        alertTriaged = textTertiary,
        modeLive = red,
        modeLiveGlow = redGlow,
        modeRewind = amber,
        modeRewindGlow = Color(0x40FFA726),
        modeReplay = cyan,
        modeReplayGlow = cyanGlow,
        pathPlanned = cyan,
        pathActual = gold,
        deviationLow = deviationLow,
        deviationMedium = deviationMedium,
        deviationHigh = deviationHigh
    )
}
