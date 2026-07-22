package com.ares.analytics.di

import com.ares.analytics.service.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Centralized service registry that lazy-initializes all application services
 * in correct dependency order. Replaces the 20 separate `remember {}` blocks
 * that previously lived in Main.kt.
 *
 * Usage:
 * ```
 * val services = remember { ServiceRegistry() }
 * DisposableEffect(Unit) { onDispose { services.dispose() } }
 * ```
 */
class ServiceRegistry {

    // ── Tier 0: No dependencies ──────────────────────────────────────────────
    /**
     * databaseService val.
     */
    val databaseService by lazy { DatabaseService() }
    /**
     * environmentService val.
     */
    val environmentService by lazy { EnvironmentService() }
    /**
     * firebaseClientService val.
     */
    val firebaseClientService by lazy { FirebaseClientService() }
    /**
     * processManagerService val.
     */
    val processManagerService by lazy { ProcessManagerService() }
    /**
     * targetScannerService val.
     */
    val targetScannerService by lazy { TargetScannerService() }
    /**
     * keybindingParserService val.
     */
    val keybindingParserService by lazy { KeybindingParserService() }
    /**
     * simulationService val.
     */
    val simulationService by lazy { SimulationService() }
    /**
     * eventApiService val.
     */
    val eventApiService by lazy { EventApiService() }
    /**
     * layoutPreferenceService val.
     */
    val layoutPreferenceService by lazy { LayoutPreferenceService() }
    /**
     * updateCheckerService val.
     */
    val updateCheckerService by lazy { UpdateCheckerService() }

    // ── Tier 1: Depend on Tier 0 ─────────────────────────────────────────────
    /**
     * nt4ClientService val.
     */
    val nt4ClientService by lazy { Nt4ClientService(databaseService) }
    /**
     * logParserService val.
     */
    val logParserService by lazy { LogParserService(databaseService, summaryEngineService) }
    /**
     * parquetExporterService val.
     */
    val parquetExporterService by lazy { ParquetExporterService(databaseService) }
    /**
     * replayEngineService val.
     */
    val replayEngineService by lazy { ReplayEngineService(databaseService) }
    /**
     * sysIdService val.
     */
    val sysIdService by lazy { SysIdService(databaseService) }
    /**
     * calibrationService val.
     */
    val calibrationService by lazy { CalibrationService(databaseService) }
    /**
     * oauthService val.
     */
    val oauthService by lazy { OAuthService(firebaseClientService) }
    /**
     * exportService val.
     */
    val exportService by lazy { ExportService(databaseService) }

    // ── Tier 2: Depend on Tier 0 + Tier 1 ────────────────────────────────────
    /**
     * alertEngineService val.
     */
    val alertEngineService by lazy { AlertEngineService(databaseService, nt4ClientService) }
    /**
     * driverAnalysisService val.
     */
    val driverAnalysisService by lazy { DriverAnalysisService(databaseService, sysIdService) }
    /**
     * summaryEngineService val.
     */
    val summaryEngineService by lazy { SummaryEngineService(databaseService, sysIdService, driverAnalysisService) }
    /**
     * hootDecoderService val.
     */
    val hootDecoderService by lazy { HootDecoderService(databaseService, summaryEngineService, sysIdService) }
    /**
     * googleDriveService val.
     */
    val googleDriveService by lazy { GoogleDriveService(oauthService, environmentService, firebaseClientService) }
    /**
     * teamApiService val.
     */
    val teamApiService by lazy { TeamApiService(firebaseClientService) }
    /**
     * syncEngineService val.
     */
    val syncEngineService by lazy { SyncEngineService(databaseService, parquetExporterService, firebaseClientService, environmentService, teamApiService, summaryEngineService, googleDriveService) }
    /**
     * phoenixDiagnosticsService val.
     */
    val phoenixDiagnosticsService by lazy { PhoenixDiagnosticsService(nt4ClientService) }
    /**
     * ftcDashboardService val.
     */
    val ftcDashboardService by lazy { FtcDashboardService(nt4ClientService) }

    /**
     * Tears down services that hold coroutine scopes or background jobs.
     * Call from `DisposableEffect { onDispose { ... } }`.
     */
    fun dispose() {
        if (lazyFieldInitialized(::updateCheckerService)) {
            updateCheckerService.dispose()
        }
        if (lazyFieldInitialized(::targetScannerService)) {
            targetScannerService.stopScanning()
        }
        // Nt4ClientService.stop() cancels the WebSocket client job
        if (lazyFieldInitialized(::nt4ClientService)) {
            nt4ClientService.stop()
        }
        // ProcessManagerService.shutdown() cancels build, sim, logcat, and ADB monitor jobs
        if (lazyFieldInitialized(::processManagerService)) {
            processManagerService.shutdown()
        }
        // ReplayEngineService.stop() cancels the replay playback job
        if (lazyFieldInitialized(::replayEngineService)) {
            replayEngineService.stop()
        }
        if (lazyFieldInitialized(::phoenixDiagnosticsService)) {
            phoenixDiagnosticsService.dispose()
        }
        if (lazyFieldInitialized(::ftcDashboardService)) {
            ftcDashboardService.dispose()
        }
        if (lazyFieldInitialized(::oauthService)) {
            oauthService.dispose()
        }
        if (lazyFieldInitialized(::syncEngineService)) {
            syncEngineService.close()
        }
        if (lazyFieldInitialized(::teamApiService)) {
            teamApiService.close()
        }
        if (lazyFieldInitialized(::eventApiService)) {
            eventApiService.close()
        }
        if (lazyFieldInitialized(::firebaseClientService)) {
            firebaseClientService.close()
        }
        if (lazyFieldInitialized(::databaseService)) {
            databaseService.close()
        }
        if (lazyFieldInitialized(::gamepadService)) {
            gamepadService.dispose()
        }
    }

    // ── Global Keyboard Drive State ──────────────────────────────────────────
    /**
     * keyboardDriveState val.
     */
    val keyboardDriveState by lazy { KeyboardDriveState() }

    // ── Gamepad Service ──────────────────────────────────────────────────────
    /**
     * gamepadService val.
     */
    val gamepadService by lazy { 
        GamepadService().apply { start() } 
    }

    private fun lazyFieldInitialized(prop: kotlin.reflect.KProperty0<*>): Boolean {
        return try {
            (prop.getDelegate() as? Lazy<*>)?.isInitialized() == true
        } catch (t: Throwable) {
            true // Fallback to true so we dispose the service rather than leaking resources/jobs
        }
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
class KeyboardDriveState {
    /**
     * enabled var.
     */
    var enabled by androidx.compose.runtime.mutableStateOf(false)
    /**
     * useGamepad var.
     */
    var useGamepad by androidx.compose.runtime.mutableStateOf(true)
    
    // Left Stick (W/A/S/D)
    /**
     * isWPressed var.
     */
    var isWPressed by androidx.compose.runtime.mutableStateOf(false)
    /**
     * isSPressed var.
     */
    var isSPressed by androidx.compose.runtime.mutableStateOf(false)
    /**
     * isAPressed var.
     */
    var isAPressed by androidx.compose.runtime.mutableStateOf(false)
    /**
     * isDPressed var.
     */
    var isDPressed by androidx.compose.runtime.mutableStateOf(false)
    
    // Right Stick (Arrow Keys)
    /**
     * isUpPressed var.
     */
    var isUpPressed by androidx.compose.runtime.mutableStateOf(false)
    /**
     * isDownPressed var.
     */
    var isDownPressed by androidx.compose.runtime.mutableStateOf(false)
    /**
     * isLeftPressed var.
     */
    var isLeftPressed by androidx.compose.runtime.mutableStateOf(false)
    /**
     * isRightPressed var.
     */
    var isRightPressed by androidx.compose.runtime.mutableStateOf(false)
    
    // Face Buttons (J, L, U, I)
    /**
     * isJPressed var.
     */
    var isJPressed by androidx.compose.runtime.mutableStateOf(false)
    /**
     * isLPressed var.
     */
    var isLPressed by androidx.compose.runtime.mutableStateOf(false)
    /**
     * isUPressed var.
     */
    var isUPressed by androidx.compose.runtime.mutableStateOf(false)
    /**
     * isIPressed var.
     */
    var isIPressed by androidx.compose.runtime.mutableStateOf(false)
    
    // Bumpers (Q, E)
    /**
     * isQPressed var.
     */
    var isQPressed by androidx.compose.runtime.mutableStateOf(false)
    /**
     * isEPressed var.
     */
    var isEPressed by androidx.compose.runtime.mutableStateOf(false)
    
    // Triggers (Space, Shift)
    /**
     * isSpacePressed var.
     */
    var isSpacePressed by androidx.compose.runtime.mutableStateOf(false)
    /**
     * isShiftPressed var.
     */
    var isShiftPressed by androidx.compose.runtime.mutableStateOf(false)
}
