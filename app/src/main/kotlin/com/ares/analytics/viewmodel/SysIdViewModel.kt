package com.ares.analytics.viewmodel

import com.ares.analytics.service.DatabaseService
import com.ares.analytics.service.DriverAnalysisService
import com.ares.analytics.service.DriverProfileAnalysisResult
import com.ares.analytics.service.SysIdService
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.service.AlignedDataRow
import com.ares.analytics.shared.CalculatedSummary
import com.areslib.control.assist.SysIdMechanism
import com.areslib.control.assist.SysIdRoutine
import com.ares.analytics.viewmodel.sysid.SysIdDataCollector
import com.ares.analytics.viewmodel.sysid.SysIdRegressionSolver
import com.ares.analytics.viewmodel.sysid.SysIdSignalGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class SysIdState(
    /**
     * sessionId val.
     */
    val sessionId: String? = null,
    /**
     * summary val.
     */
    val summary: CalculatedSummary? = null,
    /**
     * jitterResult val.
     */
    val jitterResult: DriverProfileAnalysisResult? = null,
    /**
     * exportStatus val.
     */
    val exportStatus: String = "",
    /**
     * isLoading val.
     */
    val isLoading: Boolean = false,
    /**
     * errorMessage val.
     */
    val errorMessage: String? = null,
    
    // Robot connection and live routines
    /**
     * isRobotConnected val.
     */
    val isRobotConnected: Boolean = false,
    /**
     * isRoutineRunning val.
     */
    val isRoutineRunning: Boolean = false,
    /**
     * selectedMechanism val.
     */
    val selectedMechanism: SysIdMechanism = SysIdMechanism.LINEAR,
    /**
     * liveSamples val.
     */
    val liveSamples: List<AlignedDataRow> = emptyList(),
    
    // Standalone file upload analysis
    /**
     * localAnalysisResult val.
     */
    val localAnalysisResult: CalculatedSummary? = null,
    /**
     * fileAnalysisError val.
     */
    val fileAnalysisError: String? = null,

    // New Auto-Tuning/Calibration features
    /**
     * activeCalibration val.
     */
    val activeCalibration: String = "NONE", // "NONE", "PINPOINT_SPIN", "TRACK_WIDTH_SPIN", "VISION_CALIBRATION", "LINEAR_DRIVE"
    /**
     * liveCalibrationData val.
     */
    val liveCalibrationData: List<DoubleArray> = emptyList(),
    
    /**
     * recommendedPinpointXOffsetMm val.
     */
    val recommendedPinpointXOffsetMm: Double? = null,
    /**
     * recommendedPinpointYOffsetMm val.
     */
    val recommendedPinpointYOffsetMm: Double? = null,
    /**
     * recommendedTrackWidthMeters val.
     */
    val recommendedTrackWidthMeters: Double? = null,
    /**
     * recommendedVisionStdDevsX val.
     */
    val recommendedVisionStdDevsX: Double? = null,
    /**
     * recommendedVisionStdDevsY val.
     */
    val recommendedVisionStdDevsY: Double? = null,
    /**
     * recommendedVisionStdDevsHeading val.
     */
    val recommendedVisionStdDevsHeading: Double? = null,
    /**
     * recommendedTicksPerMeter val.
     */
    val recommendedTicksPerMeter: Double? = null,

    // For linear drive calibration distance input
    /**
     * linearDriveActualDistanceMeters val.
     */
    val linearDriveActualDistanceMeters: Double = 2.0
)

sealed class SysIdIntent {
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class LoadSession(val sessionId: String?) : SysIdIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class ApplyToRobotCode(
        /**
         * recommendedExponent val.
         */
        val recommendedExponent: Double,
        /**
         * recommendedSlewRate val.
         */
        val recommendedSlewRate: Double,
        /**
         * projectPath val.
         */
        val projectPath: String
    ) : SysIdIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object ClearExportStatus : SysIdIntent()
    
    // Live routine controls
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SetMechanism(val mechanism: SysIdMechanism) : SysIdIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class StartRoutine(val routine: SysIdRoutine) : SysIdIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object StopRoutine : SysIdIntent()
    
    // Standalone log analysis
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class LoadLocalLogFile(val fileContent: String) : SysIdIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object ClearLocalAnalysis : SysIdIntent()

    // New Auto-Tuning/Calibration intents
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class StartCalibration(val calibrationType: String) : SysIdIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object StopCalibration : SysIdIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SetLinearDriveDistance(val distance: Double) : SysIdIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class ApplyCalibration(val calibrationType: String) : SysIdIntent()
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class SysIdViewModel(
    private val databaseService: DatabaseService,
    private val sysIdService: SysIdService,
    private val driverAnalysisService: DriverAnalysisService,
    /**
     * nt4ClientService val.
     */
    val nt4ClientService: Nt4ClientService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(SysIdState())
    /**
     * state val.
     */
    val state: StateFlow<SysIdState> = _state.asStateFlow()

    private val regressionSolver = SysIdRegressionSolver(nt4ClientService, _state)
    private val dataCollector = SysIdDataCollector(nt4ClientService, sysIdService, _state, scope, regressionSolver)
    private val signalGenerator = SysIdSignalGenerator(nt4ClientService, _state)

    init {
        dataCollector.startCollecting()
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun onIntent(intent: SysIdIntent) {
        scope.launch {
            when (intent) {
                is SysIdIntent.LoadSession -> {
                    /**
                     * sessionId val.
                     */
                    val sessionId = intent.sessionId
                    _state.update { it.copy(sessionId = sessionId, isLoading = true, summary = null, jitterResult = null, errorMessage = null) }
                    if (sessionId != null) {
                        try {
                            withContext(Dispatchers.IO) {
                                /**
                                 * summaryResult val.
                                 */
                                val summaryResult = sysIdService.analyzeMotorData(
                                    sessionId = sessionId,
                                    voltageKey = "/Drive/Voltage",
                                    velocityKey = "/Drive/Velocity",
                                    accelerationKey = "/Drive/Acceleration"
                                )
                                /**
                                 * jitterResult val.
                                 */
                                val jitterResult = driverAnalysisService.analyzeDriverJitter(
                                    sessionId = sessionId
                                )
                                _state.update {
                                    it.copy(
                                        summary = summaryResult,
                                        jitterResult = jitterResult,
                                        isLoading = false
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to perform analysis") }
                        }
                    } else {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
                is SysIdIntent.ApplyToRobotCode -> {
                    signalGenerator.applyToRobotCode(intent.recommendedExponent, intent.recommendedSlewRate)
                }
                is SysIdIntent.ClearExportStatus -> {
                    _state.update { it.copy(exportStatus = "") }
                }
                is SysIdIntent.SetMechanism -> {
                    _state.update { it.copy(selectedMechanism = intent.mechanism) }
                }
                is SysIdIntent.StartRoutine -> {
                    dataCollector.clearBuffer()
                    signalGenerator.startRoutine(_state.value.selectedMechanism, intent.routine)
                }
                is SysIdIntent.StopRoutine -> {
                    signalGenerator.stopRoutine()
                }
                is SysIdIntent.LoadLocalLogFile -> {
                    _state.update { it.copy(isLoading = true, fileAnalysisError = null, localAnalysisResult = null) }
                    try {
                        /**
                         * rows val.
                         */
                        val rows = withContext(Dispatchers.IO) {
                            dataCollector.parseLogFile(intent.fileContent)
                        }
                        if (rows.size < 10) {
                            _state.update { it.copy(isLoading = false, fileAnalysisError = "Not enough valid data rows found in file (minimum 10 required)") }
                        } else {
                            /**
                             * summary val.
                             */
                            val summary = withContext(Dispatchers.IO) {
                                sysIdService.analyzeRawData(rows)
                            }
                            _state.update { it.copy(isLoading = false, localAnalysisResult = summary) }
                        }
                    } catch (e: Exception) {
                        _state.update { it.copy(isLoading = false, fileAnalysisError = "Failed to parse file: ${e.message}") }
                    }
                }
                is SysIdIntent.ClearLocalAnalysis -> {
                    _state.update { it.copy(localAnalysisResult = null, fileAnalysisError = null) }
                }
                is SysIdIntent.StartCalibration -> {
                    dataCollector.clearBuffer()
                    signalGenerator.startCalibration(intent.calibrationType)
                }
                is SysIdIntent.StopCalibration -> {
                    signalGenerator.stopCalibration()
                }
                is SysIdIntent.SetLinearDriveDistance -> {
                    _state.update { it.copy(linearDriveActualDistanceMeters = intent.distance) }
                }
                is SysIdIntent.ApplyCalibration -> {
                    signalGenerator.applyCalibration(intent.calibrationType)
                }
            }
        }
    }
}
