package com.ares.analytics.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class WidgetConfig(
    /**
     * id val.
     */
    val id: String,
    /**
     * type val.
     */
    val type: String, // "runs_index", "alerts", "telemetry_chart", "motor_health", "vision_quality", "ai_coach", "match_schedule", "console_viewer"
    /**
     * row val.
     */
    val row: Int,
    /**
     * col val.
     */
    val col: Int,
    /**
     * rowSpan val.
     */
    val rowSpan: Int,
    /**
     * colSpan val.
     */
    val colSpan: Int,
    /**
     * isLocked val.
     */
    val isLocked: Boolean = false,
    /**
     * properties val.
     */
    val properties: Map<String, String> = emptyMap()
)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class DashboardLayoutConfig(
    /**
     * widgets val.
     */
    val widgets: List<WidgetConfig>
)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class LayoutPreferenceService(
    private val baseDir: String = System.getProperty("user.home") + "/.ares-analytics/layouts"
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        File(baseDir).mkdirs()
    }

    private fun getFileForProfile(profileName: String): File {
        return File(baseDir, "${profileName.lowercase().replace(" ", "_")}.json")
    }

    suspend fun saveLayout(profileName: String, config: DashboardLayoutConfig) = withContext(Dispatchers.IO) {
        /**
         * file val.
         */
        val file = getFileForProfile(profileName)
        file.writeText(json.encodeToString(config))
    }

    suspend fun loadLayout(profileName: String): DashboardLayoutConfig = withContext(Dispatchers.IO) {
        /**
         * file val.
         */
        val file = getFileForProfile(profileName)
        if (file.exists()) {
            try {
                return@withContext json.decodeFromString<DashboardLayoutConfig>(file.readText())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Fallback to default layouts
        getDefaultLayout(profileName)
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun getDefaultLayout(profileName: String): DashboardLayoutConfig {
        return when (profileName.lowercase().replace(" ", "_")) {
            "driver_coach" -> DashboardLayoutConfig(
                listOf(
                    WidgetConfig("mecanum_visualizer", "mecanum_visualizer", 0, 0, 6, 6),
                    WidgetConfig("joystick_visualizer", "joystick_visualizer", 0, 6, 6, 3),
                    WidgetConfig("telemetry_chart", "telemetry_chart", 6, 0, 6, 6),
                    WidgetConfig("alerts", "alerts", 6, 6, 3, 3),
                    WidgetConfig("ai_coach", "ai_coach", 9, 6, 3, 3)
                )
            )
            "programmer" -> DashboardLayoutConfig(
                listOf(
                    WidgetConfig("telemetry_chart", "telemetry_chart", 0, 0, 6, 9),
                    WidgetConfig("motor_health", "motor_health", 6, 0, 3, 3),
                    WidgetConfig("vision_quality", "vision_quality", 6, 3, 3, 3),
                    WidgetConfig("alerts", "alerts", 6, 6, 3, 3),
                    WidgetConfig("console_viewer_0", "console_viewer", 9, 0, 3, 9)
                )
            )
            "pit_crew" -> DashboardLayoutConfig(
                listOf(
                    WidgetConfig("runs_index", "runs_index", 0, 0, 6, 3),
                    WidgetConfig("alerts", "alerts", 0, 3, 3, 6),
                    WidgetConfig("ai_coach", "ai_coach", 3, 3, 3, 6),
                    WidgetConfig("motor_health", "motor_health", 6, 0, 3, 5),
                    WidgetConfig("vision_quality", "vision_quality", 6, 5, 3, 4)
                )
            )
            "replay" -> DashboardLayoutConfig(
                listOf(
                    WidgetConfig("runs_index", "runs_index", 0, 0, 9, 2),
                    WidgetConfig("telemetry_chart", "telemetry_chart", 0, 2, 6, 7),
                    WidgetConfig("mecanum_visualizer", "mecanum_visualizer", 6, 2, 3, 4),
                    WidgetConfig("alerts", "alerts", 6, 6, 3, 3)
                )
            )
            else -> DashboardLayoutConfig( // Default standard layout
                listOf(
                    WidgetConfig("runs_index", "runs_index", 0, 0, 6, 3),
                    WidgetConfig("mecanum_visualizer", "mecanum_visualizer", 0, 3, 6, 3),
                    WidgetConfig("joystick_visualizer", "joystick_visualizer", 0, 6, 6, 3),
                    WidgetConfig("telemetry_chart", "telemetry_chart", 6, 0, 3, 6),
                    WidgetConfig("alerts", "alerts", 6, 6, 3, 3)
                )
            )
        }
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun getSavedLayouts(): List<String> {
        /**
         * dir val.
         */
        val dir = File(baseDir)
        if (!dir.exists()) return emptyList()
        /**
         * files val.
         */
        val files = dir.listFiles { _, name -> name.endsWith(".json") } ?: return emptyList()
        return files.map { file ->
            file.nameWithoutExtension.split("_").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun getAvailableLayouts(): List<String> {
        /**
         * defaults val.
         */
        val defaults = listOf("Standard", "Driver Coach", "Programmer", "Pit Crew", "Match Review", "Pit Diagnostics", "Driver Practice")
        /**
         * saved val.
         */
        val saved = getSavedLayouts()
        return (defaults + saved).distinct()
    }

    suspend fun deleteLayout(profileName: String): Boolean = withContext(Dispatchers.IO) {
        /**
         * file val.
         */
        val file = getFileForProfile(profileName)
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
