package com.ares.analytics.viewmodel

import com.ares.analytics.service.*
import com.ares.analytics.shared.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class DashboardState(
    /**
     * currentRoleProfile val.
     */
    val currentRoleProfile: String = "Standard",
    /**
     * currentLayout val.
     */
    val currentLayout: DashboardLayoutConfig? = null,
    /**
     * isPickerOpen val.
     */
    val isPickerOpen: Boolean = false,
    /**
     * profileExpanded val.
     */
    val profileExpanded: Boolean = false,
    /**
     * primarySessionId val.
     */
    val primarySessionId: String? = null,
    /**
     * sessionMode val.
     */
    val sessionMode: SessionMode = SessionMode.LIVE_STREAMING,
    /**
     * compareSessionId val.
     */
    val compareSessionId: String? = null,
    /**
     * alerts val.
     */
    val alerts: List<AlertRecord> = emptyList(),
    /**
     * isConnected val.
     */
    val isConnected: Boolean = false,
    /**
     * isImporting val.
     */
    val isImporting: Boolean = false,
    /**
     * importSuccess val.
     */
    val importSuccess: Boolean = false,
    /**
     * errorMessage val.
     */
    val errorMessage: String? = null,
    /**
     * availableProfiles val.
     */
    val availableProfiles: List<String> = emptyList(),
    /**
     * savedLiveProfile val.
     */
    val savedLiveProfile: String? = null
)

sealed class DashboardIntent {
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class ChangeProfile(val profile: String) : DashboardIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SetPickerOpen(val isOpen: Boolean) : DashboardIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SetProfileExpanded(val isExpanded: Boolean) : DashboardIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SelectPrimarySession(val sessionId: String?) : DashboardIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SetSessionMode(val mode: SessionMode) : DashboardIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SelectCompareSession(val sessionId: String?) : DashboardIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateLayout(val newWidgets: List<WidgetConfig>) : DashboardIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class AddWidget(val type: String) : DashboardIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class RemoveWidget(val widgetId: String) : DashboardIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object ResetProfile : DashboardIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class ImportLogFiles(val files: List<File>, val teamId: String, val seasonId: String, val robotId: String) : DashboardIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object ClearImportSuccess : DashboardIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SaveLayoutAs(val profileName: String) : DashboardIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteLayout(val profileName: String) : DashboardIntent()
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class DashboardViewModel(
    private val databaseService: DatabaseService,
    private val nt4ClientService: Nt4ClientService,
    private val alertEngineService: AlertEngineService,
    private val syncEngineService: SyncEngineService,
    private val hootDecoderService: HootDecoderService,
    private val logParserService: LogParserService,
    private val layoutPreferenceService: LayoutPreferenceService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(DashboardState())
    /**
     * state val.
     */
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        // Collect alerts
        scope.launch {
            alertEngineService.alerts.collectLatest { list ->
                if (_state.value.sessionMode != SessionMode.HISTORICAL_REPLAY) {
                    _state.update { it.copy(alerts = list) }
                }
            }
        }
        // Collect connection state
        scope.launch {
            nt4ClientService.isConnected.collectLatest { connected ->
                _state.update { it.copy(isConnected = connected) }
            }
        }
        // Load initial layout
        loadLayoutForProfile(_state.value.currentRoleProfile)
        refreshAvailableProfiles()
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun onIntent(intent: DashboardIntent) {
        scope.launch {
            when (intent) {
                is DashboardIntent.ChangeProfile -> {
                    _state.update { it.copy(currentRoleProfile = intent.profile, profileExpanded = false) }
                    loadLayoutForProfile(intent.profile)
                }
                is DashboardIntent.SetPickerOpen -> {
                    _state.update { it.copy(isPickerOpen = intent.isOpen) }
                }
                is DashboardIntent.SetProfileExpanded -> {
                    _state.update { it.copy(profileExpanded = intent.isExpanded) }
                }
                is DashboardIntent.SelectPrimarySession -> {
                    /**
                     * newMode val.
                     */
                    val newMode = if (intent.sessionId == null) SessionMode.LIVE_STREAMING else SessionMode.HISTORICAL_REPLAY
                    
                    if (intent.sessionId != null) {
                        scope.launch {
                            /**
                             * historicalAlerts val.
                             */
                            val historicalAlerts = databaseService.getAlerts(intent.sessionId)
                            _state.update { it.copy(alerts = historicalAlerts) }
                        }
                    } else {
                        _state.update { it.copy(alerts = alertEngineService.alerts.value) }
                    }

                    when {
                        newMode == SessionMode.HISTORICAL_REPLAY && _state.value.sessionMode == SessionMode.LIVE_STREAMING -> {
                            // Going into replay, save the current live layout profile
                            /**
                             * currentLiveProfile val.
                             */
                            val currentLiveProfile = _state.value.currentRoleProfile
                            _state.update { it.copy(
                                primarySessionId = intent.sessionId,
                                sessionMode = newMode,
                                savedLiveProfile = currentLiveProfile,
                                currentRoleProfile = "Replay"
                            ) }
                            loadLayoutForProfile("Replay")
                        }
                        newMode == SessionMode.LIVE_STREAMING && _state.value.sessionMode == SessionMode.HISTORICAL_REPLAY -> {
                            // Returning to live, restore live layout profile
                            /**
                             * restoreProfile val.
                             */
                            val restoreProfile = _state.value.savedLiveProfile ?: "Standard"
                            _state.update { it.copy(
                                primarySessionId = intent.sessionId,
                                sessionMode = newMode,
                                savedLiveProfile = null,
                                currentRoleProfile = restoreProfile
                            ) }
                            loadLayoutForProfile(restoreProfile)
                        }
                        else -> {
                            // Standard update
                            _state.update { it.copy(primarySessionId = intent.sessionId, sessionMode = newMode) }
                        }
                    }
                }
                is DashboardIntent.SetSessionMode -> {
                    _state.update { it.copy(sessionMode = intent.mode) }
                }
                is DashboardIntent.SelectCompareSession -> {
                    _state.update { it.copy(compareSessionId = intent.sessionId) }
                }
                is DashboardIntent.UpdateLayout -> {
                    /**
                     * profile val.
                     */
                    val profile = _state.value.currentRoleProfile
                    /**
                     * newLayout val.
                     */
                    val newLayout = DashboardLayoutConfig(intent.newWidgets)
                    _state.update { it.copy(currentLayout = newLayout) }
                    withContext(Dispatchers.IO) {
                        layoutPreferenceService.saveLayout(profile, newLayout)
                    }
                }
                is DashboardIntent.AddWidget -> {
                    /**
                     * currentLayout val.
                     */
                    val currentLayout = _state.value.currentLayout
                    /**
                     * currentList val.
                     */
                    val currentList = currentLayout?.widgets ?: emptyList()
                    /**
                     * maxRow val.
                     */
                    val maxRow = currentList.maxOfOrNull { it.row + it.rowSpan } ?: 0
                    val (defRowSpan, defColSpan) = getDefaultWidgetSize(intent.type)
                    /**
                     * newWidget val.
                     */
                    val newWidget = WidgetConfig(
                        id = "${intent.type}_${System.currentTimeMillis()}",
                        type = intent.type,
                        row = maxRow,
                        col = 0,
                        rowSpan = defRowSpan,
                        colSpan = defColSpan
                    )
                    /**
                     * newWidgets val.
                     */
                    val newWidgets = currentList + newWidget
                    onIntent(DashboardIntent.UpdateLayout(newWidgets))
                    _state.update { it.copy(isPickerOpen = false) }
                }
                is DashboardIntent.RemoveWidget -> {
                    /**
                     * currentLayout val.
                     */
                    val currentLayout = _state.value.currentLayout
                    /**
                     * currentList val.
                     */
                    val currentList = currentLayout?.widgets ?: emptyList()
                    /**
                     * newWidgets val.
                     */
                    val newWidgets = currentList.filter { it.id != intent.widgetId }
                    onIntent(DashboardIntent.UpdateLayout(newWidgets))
                }
                is DashboardIntent.ResetProfile -> {
                    /**
                     * profile val.
                     */
                    val profile = _state.value.currentRoleProfile
                    /**
                     * defaultConf val.
                     */
                    val defaultConf = layoutPreferenceService.getDefaultLayout(profile)
                    _state.update { it.copy(currentLayout = defaultConf) }
                    withContext(Dispatchers.IO) {
                        layoutPreferenceService.saveLayout(profile, defaultConf)
                    }
                }
                is DashboardIntent.ImportLogFiles -> {
                    _state.update { it.copy(isImporting = true, importSuccess = false, errorMessage = null) }
                    withContext(Dispatchers.IO) {
                        try {
                            /**
                             * sessionId val.
                             */
                            val sessionId = if (intent.files.size == 1 && intent.files.first().name.lowercase().endsWith(".hoot")) {
                                hootDecoderService.importHootLog(
                                    hootFile = intent.files.first(),
                                    teamId = intent.teamId,
                                    seasonId = intent.seasonId,
                                    robotId = intent.robotId
                                )
                            } else {
                                logParserService.parseLogFiles(
                                    files = intent.files,
                                    teamId = intent.teamId,
                                    seasonId = intent.seasonId,
                                    robotId = intent.robotId
                                ).sessionId
                            }

                            // Trigger background cloud sync after successful import
                            try {
                                syncEngineService.uploadSession(sessionId)
                                syncEngineService.performDeltaSync(intent.teamId, intent.seasonId)
                            } catch (syncEx: Exception) {
                                // Fallback silently so local usability is unaffected if offline
                            }

                            _state.update { it.copy(isImporting = false, importSuccess = true) }
                        } catch (e: Exception) {
                            _state.update { it.copy(isImporting = false, errorMessage = e.message ?: "Failed to import log file(s)") }
                        }
                    }
                }
                is DashboardIntent.ClearImportSuccess -> {
                    _state.update { it.copy(importSuccess = false) }
                }
                is DashboardIntent.SaveLayoutAs -> {
                    /**
                     * currentLayout val.
                     */
                    val currentLayout = _state.value.currentLayout
                    if (currentLayout != null) {
                        _state.update { it.copy(currentRoleProfile = intent.profileName) }
                        withContext(Dispatchers.IO) {
                            layoutPreferenceService.saveLayout(intent.profileName, currentLayout)
                        }
                        refreshAvailableProfiles()
                    }
                }
                is DashboardIntent.DeleteLayout -> {
                    /**
                     * currentProfile val.
                     */
                    val currentProfile = _state.value.currentRoleProfile
                    withContext(Dispatchers.IO) {
                        layoutPreferenceService.deleteLayout(intent.profileName)
                    }
                    if (currentProfile == intent.profileName) {
                        onIntent(DashboardIntent.ChangeProfile("Standard"))
                    }
                    refreshAvailableProfiles()
                }
            }
        }
    }

    private fun getDefaultWidgetSize(type: String): Pair<Int, Int> {
        return when (type) {
            // Pair is (rowSpan, colSpan)
            "field_viewer", "camera_stream", "telemetry_chart", "pose_viewer" -> Pair(6, 6)
            "console_viewer" -> Pair(6, 9)
            "runs_index", "match_schedule" -> Pair(4, 9)
            "mecanum_visualizer", "swerve_animator", "joystick_visualizer", "mechanism_visualizer" -> Pair(4, 6)
            "ai_coach" -> Pair(5, 6)
            "alerts", "motor_health", "vision_quality", "battery_health", "system_health", "power_distribution", "indicator_lights" -> Pair(3, 4)
            "statistics_panel", "trends_card", "control_profiler", "state_tracker", "imu_visualizer", "ekf_telemetry", "path_tuning", "profiling_diagnostics" -> Pair(4, 5)
            else -> Pair(3, 3)
        }
    }

    private fun loadLayoutForProfile(profileName: String) {
        scope.launch {
            /**
             * layout val.
             */
            val layout = withContext(Dispatchers.IO) {
                layoutPreferenceService.loadLayout(profileName)
            }
            _state.update { it.copy(currentLayout = layout) }
        }
    }

    private fun refreshAvailableProfiles() {
        scope.launch {
            /**
             * list val.
             */
            val list = withContext(Dispatchers.IO) {
                layoutPreferenceService.getAvailableLayouts()
            }
            _state.update { it.copy(availableProfiles = list) }
        }
    }
}
