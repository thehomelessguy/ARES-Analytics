package com.ares.analytics.viewmodel

import com.ares.analytics.service.ConstantsParserService
import com.ares.analytics.service.DatabaseService
import com.ares.analytics.service.DriverAnalysisService
import com.ares.analytics.service.DriverProfileAnalysisResult
import com.ares.analytics.service.SysIdService
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.service.AlignedDataRow
import com.ares.analytics.shared.CalculatedSummary
import com.areslib.control.SysIdMechanism
import com.areslib.control.SysIdRoutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

data class SysIdState(
    val sessionId: String? = null,
    val summary: CalculatedSummary? = null,
    val jitterResult: DriverProfileAnalysisResult? = null,
    val exportStatus: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    
    // Robot connection and live routines
    val isRobotConnected: Boolean = false,
    val isRoutineRunning: Boolean = false,
    val selectedMechanism: SysIdMechanism = SysIdMechanism.LINEAR,
    val liveSamples: List<AlignedDataRow> = emptyList(),
    
    // Standalone file upload analysis
    val localAnalysisResult: CalculatedSummary? = null,
    val fileAnalysisError: String? = null
)

sealed class SysIdIntent {
    data class LoadSession(val sessionId: String?) : SysIdIntent()
    data class ApplyToRobotCode(
        val recommendedExponent: Double,
        val recommendedSlewRate: Double,
        val projectPath: String
    ) : SysIdIntent()
    object ClearExportStatus : SysIdIntent()
    
    // Live routine controls
    data class SetMechanism(val mechanism: SysIdMechanism) : SysIdIntent()
    data class StartRoutine(val routine: SysIdRoutine) : SysIdIntent()
    object StopRoutine : SysIdIntent()
    
    // Standalone log analysis
    data class LoadLocalLogFile(val fileContent: String) : SysIdIntent()
    object ClearLocalAnalysis : SysIdIntent()
}

class SysIdViewModel(
    private val databaseService: DatabaseService,
    private val sysIdService: SysIdService,
    private val driverAnalysisService: DriverAnalysisService,
    private val constantsParserService: ConstantsParserService,
    private val nt4ClientService: Nt4ClientService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(SysIdState())
    val state: StateFlow<SysIdState> = _state.asStateFlow()

    init {
        // Collect connection status
        scope.launch {
            nt4ClientService.isConnected.collect { connected ->
                _state.update { it.copy(isRobotConnected = connected) }
            }
        }

        // Collect live streaming data from the robot
        scope.launch {
            nt4ClientService.telemetryFlow.collect { frame ->
                when (frame.key) {
                    "SysId/Status" -> {
                        val status = frame.stringValue ?: ""
                        val wasRunning = _state.value.isRoutineRunning
                        val isRunning = status.isNotEmpty() && status != "NONE"
                        _state.update { it.copy(isRoutineRunning = isRunning) }
                        
                        if (wasRunning && !isRunning) {
                            // Routine just completed! Run OLS analysis on live samples
                            val samples = _state.value.liveSamples
                            if (samples.isNotEmpty()) {
                                val summary = sysIdService.analyzeRawData(samples)
                                _state.update { it.copy(summary = summary, isLoading = false) }
                            }
                        }
                    }
                    "SysId/Data" -> {
                        val stringVal = frame.stringValue
                        if (stringVal != null) {
                            val parts = stringVal.split("|").mapNotNull { it.toDoubleOrNull() }
                            if (parts.size >= 5) {
                                val sample = AlignedDataRow(
                                    timestampMs = parts[0].toLong(),
                                    voltage = parts[1],
                                    velocity = parts[3],
                                    accel = parts[4]
                                )
                                _state.update { it.copy(liveSamples = it.liveSamples + sample) }
                            }
                        }
                    }
                }
            }
        }
    }

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
                is SysIdIntent.SetMechanism -> {
                    _state.update { it.copy(selectedMechanism = intent.mechanism) }
                }
                is SysIdIntent.StartRoutine -> {
                    _state.update { it.copy(liveSamples = emptyList(), isRoutineRunning = true, summary = null, isLoading = true) }
                    val cmd = "START_${_state.value.selectedMechanism.name}_${intent.routine.name}"
                    // pubuid 1015 corresponds to "SysId/Command" topic
                    nt4ClientService.publishInputString(1015, cmd)
                }
                is SysIdIntent.StopRoutine -> {
                    _state.update { it.copy(isRoutineRunning = false, isLoading = false) }
                    nt4ClientService.publishInputString(1015, "STOP")
                }
                is SysIdIntent.LoadLocalLogFile -> {
                    _state.update { it.copy(isLoading = true, fileAnalysisError = null, localAnalysisResult = null) }
                    try {
                        val rows = withContext(Dispatchers.IO) {
                            parseLogFile(intent.fileContent)
                        }
                        if (rows.size < 10) {
                            _state.update { it.copy(isLoading = false, fileAnalysisError = "Not enough valid data rows found in file (minimum 10 required)") }
                        } else {
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
            }
        }
    }

    private fun parseLogFile(fileContent: String): List<AlignedDataRow> {
        val rows = mutableListOf<AlignedDataRow>()
        val lines = fileContent.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
        
        if (lines.none()) return emptyList()
        
        val firstLine = lines.first()
        if (firstLine.startsWith("{")) {
            // JSONL Format
            for (line in lines) {
                try {
                    val element = Json.parseToJsonElement(line).jsonObject
                    
                    // Scenario A: Check if the log contains the flattened SysId/Data array directly
                    val sysIdData = element["SysId/Data"] ?: element["SysId_Data"] ?: element["sysid_data"]
                    if (sysIdData != null) {
                        val arr = sysIdData.jsonArray.mapNotNull { it.jsonPrimitive.doubleOrNull }
                        if (arr.size >= 5) {
                            rows.add(AlignedDataRow(
                                timestampMs = arr[0].toLong(),
                                voltage = arr[1],
                                velocity = arr[3],
                                accel = arr[4]
                            ))
                            continue
                        }
                    }
                    
                    // Scenario B: Check for individual fields
                    val t = element["TimestampMs"]?.jsonPrimitive?.longOrNull 
                        ?: element["timestamp"]?.jsonPrimitive?.longOrNull 
                        ?: element["time"]?.jsonPrimitive?.longOrNull 
                        ?: continue
                    
                    val volt = element["voltage"]?.jsonPrimitive?.doubleOrNull 
                        ?: element["Voltage"]?.jsonPrimitive?.doubleOrNull 
                        ?: element["Drive/Voltage"]?.jsonPrimitive?.doubleOrNull 
                        ?: continue
                        
                    val vel = element["velocity"]?.jsonPrimitive?.doubleOrNull 
                        ?: element["Velocity"]?.jsonPrimitive?.doubleOrNull 
                        ?: element["Drive/Velocity"]?.jsonPrimitive?.doubleOrNull 
                        ?: continue
                        
                    val accel = element["acceleration"]?.jsonPrimitive?.doubleOrNull 
                        ?: element["Acceleration"]?.jsonPrimitive?.doubleOrNull 
                        ?: element["Drive/Acceleration"]?.jsonPrimitive?.doubleOrNull 
                        ?: 0.0
                        
                    rows.add(AlignedDataRow(t, volt, vel, accel))
                } catch (_: Exception) {}
            }
        } else {
            // CSV Format
            val header = firstLine.split(",").map { it.trim().removeSurrounding("\"").lowercase() }
            val tCol = header.indexOfFirst { it.contains("time") || it == "t" || it == "ts" }
            val voltCol = header.indexOfFirst { it.contains("volt") || it == "v" || it == "u" }
            val velCol = header.indexOfFirst { it.contains("vel") || it.contains("speed") || it == "omega" }
            val accelCol = header.indexOfFirst { it.contains("accel") || it == "a" }
            
            if (voltCol != -1 && velCol != -1) {
                var index = 0
                for (line in lines.drop(1)) {
                    try {
                        val cols = line.split(",").map { it.trim().removeSurrounding("\"") }
                        val t = if (tCol != -1 && tCol < cols.size) cols[tCol].toLongOrNull() ?: index.toLong() else index.toLong()
                        val volt = cols[voltCol].toDoubleOrNull() ?: continue
                        val vel = cols[velCol].toDoubleOrNull() ?: continue
                        val accel = if (accelCol != -1 && accelCol < cols.size) cols[accelCol].toDoubleOrNull() ?: 0.0 else 0.0
                        
                        rows.add(AlignedDataRow(t, volt, vel, accel))
                        index++
                    } catch (_: Exception) {}
                }
            }
        }
        
        // If acceleration is missing (all 0.0), approximate as numerical derivative
        if (rows.isNotEmpty() && rows.all { it.accel == 0.0 }) {
            val approxRows = mutableListOf<AlignedDataRow>()
            val sorted = rows.sortedBy { it.timestampMs }
            for (i in 0 until sorted.size) {
                val current = sorted[i]
                val accel = if (i == 0) 0.0 else {
                    val prev = sorted[i - 1]
                    val dt = (current.timestampMs - prev.timestampMs) / 1000.0
                    if (dt > 1e-4) (current.velocity - prev.velocity) / dt else 0.0
                }
                approxRows.add(current.copy(accel = accel))
            }
            return approxRows
        }
        
        return rows
    }
}
