package com.ares.analytics.viewmodel

import com.ares.analytics.service.ConstantsParserService
import com.ares.analytics.service.DatabaseService
import com.ares.analytics.service.DriverAnalysisService
import com.ares.analytics.service.DriverProfileAnalysisResult
import com.ares.analytics.service.SysIdService
import com.ares.analytics.shared.CalculatedSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SysIdState(
    val sessionId: String? = null,
    val summary: CalculatedSummary? = null,
    val jitterResult: DriverProfileAnalysisResult? = null,
    val exportStatus: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class SysIdIntent {
    data class LoadSession(val sessionId: String?) : SysIdIntent()
    data class ApplyToRobotCode(
        val recommendedExponent: Double,
        val recommendedSlewRate: Double,
        val projectPath: String
    ) : SysIdIntent()
    object ClearExportStatus : SysIdIntent()
}

class SysIdViewModel(
    private val databaseService: DatabaseService,
    private val sysIdService: SysIdService,
    private val driverAnalysisService: DriverAnalysisService,
    private val constantsParserService: ConstantsParserService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(SysIdState())
    val state: StateFlow<SysIdState> = _state.asStateFlow()

    fun onIntent(intent: SysIdIntent) {
        scope.launch {
            when (intent) {
                is SysIdIntent.LoadSession -> {
                    val sessionId = intent.sessionId
                    _state.update { it.copy(sessionId = sessionId, isLoading = true, summary = null, jitterResult = null, errorMessage = null) }
                    if (sessionId != null) {
                        try {
                            withContext(Dispatchers.IO) {
                                val summaryResult = sysIdService.analyzeMotorData(
                                    sessionId = sessionId,
                                    voltageKey = "/Drive/Voltage",
                                    velocityKey = "/Drive/Velocity",
                                    accelerationKey = "/Drive/Acceleration"
                                )
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
                    _state.update { it.copy(exportStatus = "Applying to robot code...") }
                    try {
                        val success = withContext(Dispatchers.IO) {
                            driverAnalysisService.exportToConstants(
                                recommendedExponent = intent.recommendedExponent,
                                recommendedSlewRate = intent.recommendedSlewRate,
                                constantsService = constantsParserService,
                                projectPath = intent.projectPath
                            )
                        }
                        _state.update {
                            it.copy(exportStatus = if (success) "Successfully applied! 🎉" else "Failed to apply.")
                        }
                    } catch (e: Exception) {
                        _state.update { it.copy(exportStatus = "Failed to apply: ${e.message}") }
                    }
                }
                is SysIdIntent.ClearExportStatus -> {
                    _state.update { it.copy(exportStatus = "") }
                }
            }
        }
    }
}
