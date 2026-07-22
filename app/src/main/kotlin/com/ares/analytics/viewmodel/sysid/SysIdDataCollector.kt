package com.ares.analytics.viewmodel.sysid

import com.ares.analytics.service.AlignedDataRow
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.service.SysIdService
import com.ares.analytics.viewmodel.SysIdState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class SysIdDataCollector(
    private val nt4ClientService: Nt4ClientService,
    private val sysIdService: SysIdService,
    private val _state: MutableStateFlow<SysIdState>,
    private val scope: CoroutineScope,
    private val regressionSolver: SysIdRegressionSolver
) {
    private val dataBuffer = ConcurrentHashMap<Long, DoubleArray>()

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun startCollecting() {
        // Collect connection status
        scope.launch {
            nt4ClientService.isConnected.collect { connected ->
                _state.update { it.copy(isRobotConnected = connected) }
            }
        }

        // Collect live streaming data from the robot
        scope.launch {
            nt4ClientService.telemetryFlow.collect { frame ->
                when {
                    frame.key == "SysId/Status" -> {
                        /**
                         * status val.
                         */
                        val status = frame.stringValue ?: ""
                        /**
                         * wasRunning val.
                         */
                        val wasRunning = _state.value.isRoutineRunning
                        /**
                         * isRunning val.
                         */
                        val isRunning = status.isNotEmpty() && status != "NONE"
                        
                        /**
                         * prevCalibration val.
                         */
                        val prevCalibration = _state.value.activeCalibration
                        
                        _state.update { 
                            it.copy(
                                isRoutineRunning = isRunning,
                                activeCalibration = if (isRunning) status else it.activeCalibration
                            ) 
                        }
                        
                        if (wasRunning && !isRunning) {
                            // Routine/Calibration just completed!
                            /**
                             * finalCalibration val.
                             */
                            val finalCalibration = prevCalibration
                            _state.update { it.copy(isLoading = false) }
                            if (finalCalibration == "PINPOINT_SPIN" || finalCalibration == "TRACK_WIDTH_SPIN" ||
                                finalCalibration == "VISION_CALIBRATION" || finalCalibration == "LINEAR_DRIVE") {
                                regressionSolver.runCalibrationAnalysis(finalCalibration, _state.value.liveCalibrationData)
                            } else {
                                /**
                                 * samples val.
                                 */
                                val samples = _state.value.liveSamples
                                if (samples.isNotEmpty()) {
                                    /**
                                     * summary val.
                                     */
                                    val summary = sysIdService.analyzeRawData(samples)
                                    _state.update { it.copy(summary = summary) }
                                }
                            }
                        }
                    }
                    frame.key.startsWith("SysId/Data/") -> {
                        /**
                         * idx val.
                         */
                        val idx = frame.key.removePrefix("SysId/Data/").toIntOrNull()
                        if (idx != null) {
                            /**
                             * t val.
                             */
                            val t = frame.timestampMs
                            /**
                             * arr val.
                             */
                            val arr = dataBuffer.getOrPut(t) { DoubleArray(6) }
                            if (idx < arr.size) {
                                arr[idx] = frame.value
                            }
                            
                            /**
                             * expectedMaxIdx val.
                             */
                            val expectedMaxIdx = when (_state.value.activeCalibration) {
                                "PINPOINT_SPIN", "VISION_CALIBRATION" -> 3
                                "LINEAR_DRIVE" -> 4
                                "TRACK_WIDTH_SPIN" -> 5
                                else -> 4
                            }
                            
                            if (idx == expectedMaxIdx) {
                                /**
                                 * completedArr val.
                                 */
                                val completedArr = dataBuffer[t]
                                if (completedArr != null) {
                                    /**
                                     * sample val.
                                     */
                                    val sample = AlignedDataRow(
                                        timestampMs = completedArr[0].toLong(),
                                        voltage = completedArr[1],
                                        velocity = completedArr[3],
                                        accel = completedArr[4]
                                    )
                                    _state.update {
                                        it.copy(
                                            liveSamples = it.liveSamples + sample,
                                            liveCalibrationData = it.liveCalibrationData + listOf(completedArr.clone())
                                        )
                                    }
                                    if (dataBuffer.size > 500) {
                                        /**
                                         * minT val.
                                         */
                                        val minT = dataBuffer.keys.minOrNull() ?: 0L
                                        dataBuffer.remove(minT)
                                    }
                                }
                            }
                        }
                    }
                    frame.key == "SysId/Data" -> {
                        /**
                         * stringVal val.
                         */
                        val stringVal = frame.stringValue
                        if (stringVal != null) {
                            /**
                             * parts val.
                             */
                            val parts = stringVal.split("|").mapNotNull { it.toDoubleOrNull() }
                            if (parts.size >= 5) {
                                /**
                                 * sample val.
                                 */
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

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun clearBuffer() {
        dataBuffer.clear()
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun parseLogFile(fileContent: String): List<AlignedDataRow> {
        /**
         * rows val.
         */
        val rows = mutableListOf<AlignedDataRow>()
        /**
         * lines val.
         */
        val lines = fileContent.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
        
        if (lines.none()) return emptyList()
        
        /**
         * firstLine val.
         */
        val firstLine = lines.first()
        if (firstLine.startsWith("{")) {
            // JSONL Format
            for (line in lines) {
                try {
                    /**
                     * element val.
                     */
                    val element = Json.parseToJsonElement(line).jsonObject
                    
                    // Scenario A: Check if the log contains the flattened SysId/Data array directly
                    /**
                     * sysIdData val.
                     */
                    val sysIdData = element["SysId/Data"] ?: element["SysId_Data"] ?: element["sysid_data"]
                    if (sysIdData != null) {
                        /**
                         * arr val.
                         */
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
                    /**
                     * t val.
                     */
                    val t = element["TimestampMs"]?.jsonPrimitive?.longOrNull 
                        ?: element["timestamp"]?.jsonPrimitive?.longOrNull 
                        ?: element["time"]?.jsonPrimitive?.longOrNull 
                        ?: continue
                    
                    /**
                     * volt val.
                     */
                    val volt = element["voltage"]?.jsonPrimitive?.doubleOrNull 
                        ?: element["Voltage"]?.jsonPrimitive?.doubleOrNull 
                        ?: element["Drive/Voltage"]?.jsonPrimitive?.doubleOrNull 
                        ?: continue
                        
                    /**
                     * vel val.
                     */
                    val vel = element["velocity"]?.jsonPrimitive?.doubleOrNull 
                        ?: element["Velocity"]?.jsonPrimitive?.doubleOrNull 
                        ?: element["Drive/Velocity"]?.jsonPrimitive?.doubleOrNull 
                        ?: continue
                        
                    /**
                     * accel val.
                     */
                    val accel = element["acceleration"]?.jsonPrimitive?.doubleOrNull 
                        ?: element["Acceleration"]?.jsonPrimitive?.doubleOrNull 
                        ?: element["Drive/Acceleration"]?.jsonPrimitive?.doubleOrNull 
                        ?: 0.0
                        
                    rows.add(AlignedDataRow(t, volt, vel, accel))
                } catch (_: Exception) {}
            }
        } else {
            // CSV Format
            /**
             * header val.
             */
            val header = firstLine.split(",").map { it.trim().removeSurrounding("\"").lowercase() }
            /**
             * tCol val.
             */
            val tCol = header.indexOfFirst { it.contains("time") || it == "t" || it == "ts" }
            /**
             * voltCol val.
             */
            val voltCol = header.indexOfFirst { it.contains("volt") || it == "v" || it == "u" }
            /**
             * velCol val.
             */
            val velCol = header.indexOfFirst { it.contains("vel") || it.contains("speed") || it == "omega" }
            /**
             * accelCol val.
             */
            val accelCol = header.indexOfFirst { it.contains("accel") || it == "a" }
            
            if (voltCol != -1 && velCol != -1) {
                /**
                 * index var.
                 */
                var index = 0
                for (line in lines.drop(1)) {
                    try {
                        /**
                         * cols val.
                         */
                        val cols = line.split(",").map { it.trim().removeSurrounding("\"") }
                        /**
                         * t val.
                         */
                        val t = if (tCol != -1 && tCol < cols.size) cols[tCol].toLongOrNull() ?: index.toLong() else index.toLong()
                        /**
                         * volt val.
                         */
                        val volt = cols[voltCol].toDoubleOrNull() ?: continue
                        /**
                         * vel val.
                         */
                        val vel = cols[velCol].toDoubleOrNull() ?: continue
                        /**
                         * accel val.
                         */
                        val accel = if (accelCol != -1 && accelCol < cols.size) cols[accelCol].toDoubleOrNull() ?: 0.0 else 0.0
                        
                        rows.add(AlignedDataRow(t, volt, vel, accel))
                        index++
                    } catch (_: Exception) {}
                }
            }
        }
        
        // If acceleration is missing (all 0.0), approximate as numerical derivative
        if (rows.isNotEmpty() && rows.all { it.accel == 0.0 }) {
            /**
             * approxRows val.
             */
            val approxRows = mutableListOf<AlignedDataRow>()
            /**
             * sorted val.
             */
            val sorted = rows.sortedBy { it.timestampMs }
            for (i in 0 until sorted.size) {
                /**
                 * current val.
                 */
                val current = sorted[i]
                /**
                 * accel val.
                 */
                val accel = if (i == 0) 0.0 else {
                    /**
                     * prev val.
                     */
                    val prev = sorted[i - 1]
                    /**
                     * dt val.
                     */
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
