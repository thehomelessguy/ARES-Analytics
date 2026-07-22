package com.ares.analytics.service

import com.ares.analytics.shared.Session
import com.ares.analytics.shared.SessionSummary
import com.ares.analytics.shared.TelemetryFrame
import com.ares.analytics.service.AlignedDataRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sign

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class SummaryEngineService(
    private val databaseService: DatabaseService,
    private val sysIdService: SysIdService,
    private val driverAnalysisService: DriverAnalysisService
) {

    suspend fun generateSummary(session: Session): SessionSummary = withContext(Dispatchers.Default) {
        // Use SQL aggregations instead of pulling all frames into Kotlin
        /**
         * aggregateResult val.
         */
        val aggregateResult = databaseService.executeQueryWithParams(
            """
            SELECT
                MIN(CASE WHEN (LOWER(key) LIKE '%voltage%' OR LOWER(key) LIKE '%battery%') AND value > 1.0 THEN value END) AS min_battery_voltage,
                MAX(CASE WHEN LOWER(key) LIKE '%drift%' OR LOWER(key) LIKE '%ekf%' OR LOWER(key) LIKE '%poseerror%' THEN value END) AS max_ekf_drift,
                AVG(CASE WHEN LOWER(key) LIKE '%loop%' OR LOWER(key) LIKE '%period%' THEN value END) AS avg_loop_time,
                AVG(CASE WHEN LOWER(key) LIKE '%vision%' AND (LOWER(key) LIKE '%acceptance%' OR LOWER(key) LIKE '%valid%' OR LOWER(key) LIKE '%quality%') THEN value END) AS vision_acceptance_rate,
                AVG(CASE WHEN LOWER(key) LIKE '%crosstrack%' OR LOWER(key) LIKE '%xte%' THEN ABS(value) END) AS avg_cross_track,
                AVG(CASE WHEN LOWER(key) LIKE '%vision%' AND LOWER(key) LIKE '%latency%' THEN value END) AS avg_vision_latency
            FROM telemetry_frames WHERE session_id = ?
            """.trimIndent(),
            listOf(session.sessionId)
        )

        /**
         * aggRow val.
         */
        val aggRow = aggregateResult.rows.firstOrNull()
        /**
         * minBattery val.
         */
        val minBattery = aggRow?.getOrNull(0)?.toDoubleOrNull() ?: 12.0
        /**
         * maxDrift val.
         */
        val maxDrift = aggRow?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        /**
         * avgLoop val.
         */
        val avgLoop = aggRow?.getOrNull(2)?.toDoubleOrNull() ?: 0.0
        /**
         * visionRate val.
         */
        val visionRate = aggRow?.getOrNull(3)?.toDoubleOrNull() ?: 1.0
        /**
         * avgCrossTrack val.
         */
        val avgCrossTrack = aggRow?.getOrNull(4)?.toDoubleOrNull() ?: 0.0
        /**
         * avgVisionLat val.
         */
        val avgVisionLat = aggRow?.getOrNull(5)?.toDoubleOrNull() ?: 0.0

        // P95 loop time via ordered-set aggregate (DuckDB supports PERCENTILE_CONT)
        /**
         * p95Result val.
         */
        val p95Result = databaseService.executeQueryWithParams(
            """
            SELECT PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY value) AS p95_loop_time
            FROM telemetry_frames
            WHERE session_id = ? AND (LOWER(key) LIKE '%loop%' OR LOWER(key) LIKE '%period%')
            """.trimIndent(),
            listOf(session.sessionId)
        )
        /**
         * p95Loop val.
         */
        val p95Loop = p95Result.rows.firstOrNull()?.getOrNull(0)?.toDoubleOrNull() ?: 0.0

        // Motor current averages grouped by device name extracted from key
        /**
         * motorResult val.
         */
        val motorResult = databaseService.executeQueryWithParams(
            """
            SELECT
                CASE
                    WHEN LOWER(SPLIT_PART(key, '/', -1)) IN ('current', 'amps')
                        THEN SPLIT_PART(key, '/', -2)
                    ELSE REGEXP_REPLACE(REGEXP_REPLACE(SPLIT_PART(key, '/', -1), '(?i)current', ''), '(?i)amps', '')
                END AS motor_name,
                AVG(value) AS avg_current
            FROM telemetry_frames
            WHERE session_id = ? AND LOWER(key) LIKE '%current%' AND LOWER(key) NOT LIKE '%battery%'
            GROUP BY motor_name
            HAVING motor_name IS NOT NULL AND motor_name != ''
            """.trimIndent(),
            listOf(session.sessionId)
        )
        /**
         * motorCurrentAverages val.
         */
        val motorCurrentAverages = motorResult.rows.associate { row ->
            (row.getOrNull(0) ?: "Motor") to (row.getOrNull(1)?.toDoubleOrNull() ?: 0.0)
        }

        // Battery resistance estimation requires sequential row-by-row voltage drop tracking;
        // not feasible to vectorize without window functions + complex stateful logic.
        /**
         * avgResistance val.
         */
        val avgResistance = 0.0

        // Detect OpModes from string_value column
        /**
         * opModeResult val.
         */
        val opModeResult = databaseService.executeQueryWithParams(
            """
            SELECT DISTINCT string_value
            FROM telemetry_frames
            WHERE session_id = ? AND LOWER(key) = 'opmode' AND string_value IS NOT NULL
            """.trimIndent(),
            listOf(session.sessionId)
        )
        /**
         * detectedModes val.
         */
        val detectedModes = opModeResult.rows.mapNotNull { it.getOrNull(0)?.takeIf { v -> v != "NULL" } }.toSet()
        /**
         * finalTags val.
         */
        val finalTags = (session.tags + detectedModes).distinct()

        // Motor thermal estimation requires sequential state (temperature depends on previous temperature)
        // and cannot be vectorized into SQL without recursive CTEs.
        /**
         * maxMotorTemps val.
         */
        val maxMotorTemps = emptyMap<String, Double>()

        /**
         * summary val.
         */
        val summary = SessionSummary(
            sessionId = session.sessionId,
            teamId = session.teamId,
            seasonId = session.seasonId,
            robotId = session.robotId,
            createdAt = session.createdAt,
            durationMs = session.durationMs,
            minBatteryVoltage = minBattery,
            maxEkfDrift = maxDrift,
            avgLoopTimeMs = avgLoop,
            p95LoopTimeMs = p95Loop,
            motorCurrentAverages = motorCurrentAverages,
            visionAcceptanceRate = visionRate,
            avgCrossTrackError = avgCrossTrack,
            avgBatteryResistance = avgResistance,
            maxMotorTemps = maxMotorTemps,
            avgVisionLatencyMs = avgVisionLat,
            tags = finalTags,
            matchNumber = session.matchNumber,
            allianceColor = session.allianceColor
        )

        databaseService.insertSessionSummary(summary)
        calculateAndSaveDiagnostics(session)
        summary
    }

    private suspend fun calculateAndSaveDiagnostics(session: Session) {
        try {
            /**
             * allFrames val.
             */
            val allFrames = databaseService.getTelemetryForFilters(
                sessionId = session.sessionId,
                keys = listOf(
                    "/Drive/Voltage", "Drive/Voltage",
                    "/Drive/Velocity", "Drive/Velocity",
                    "/Drive/Acceleration", "Drive/Acceleration",
                    "Drive/Velocity_Omega", "/Drive/Velocity_Omega"
                ),
                prefixes = listOf("Diagnostics/%", "Hardware/Motors/%")
            )
            if (allFrames.isEmpty()) return

            /**
             * framesToInsert val.
             */
            val framesToInsert = mutableListOf<TelemetryFrame>()

            // Loop Overruns and Comms Losses calculation
            /**
             * loopTimes val.
             */
            val loopTimes = allFrames.filter { it.key.lowercase().contains("loop") || it.key.lowercase().contains("period") }.map { it.value }
            /**
             * loopOverruns val.
             */
            val loopOverruns = loopTimes.count { it > 40.0 }

            /**
             * sortedTimes val.
             */
            val sortedTimes = databaseService.getDistinctTimestamps(session.sessionId)
            /**
             * commsLosses var.
             */
            var commsLosses = 0
            if (sortedTimes.size > 1) {
                for (i in 0 until sortedTimes.size - 1) {
                    /**
                     * gap val.
                     */
                    val gap = sortedTimes[i + 1] - sortedTimes[i]
                    if (gap > 1000) {
                        commsLosses++
                    }
                }
            }

            /**
             * minVoltage val.
             */
            val minVoltage = allFrames.filter { it.key.lowercase().contains("battery") && it.key.lowercase().contains("volt") }
                .map { it.value }
                .minOrNull() ?: 12.0

            framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/System/LoopOverruns", loopOverruns.toDouble()))
            framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/System/CommsLosses", commsLosses.toDouble()))

            // CANbus status, motor faults, and brownout calculations
            /**
             * canFrames val.
             */
            val canFrames = allFrames.filter { it.key.startsWith("Diagnostics/CAN/") || it.key.startsWith("Diagnostics/CANBus/") }
            /**
             * maxBusUtil val.
             */
            val maxBusUtil = canFrames.filter { it.key.endsWith("BusUtilization") || it.key.endsWith("Utilization") }.maxOfOrNull { it.value } ?: 0.0
            /**
             * totalErrorCount val.
             */
            val totalErrorCount = canFrames.filter { it.key.endsWith("ErrorCount") }.maxOfOrNull { it.value } ?: 0.0
            /**
             * totalBusOffs val.
             */
            val totalBusOffs = canFrames.filter { it.key.endsWith("BusOffs") || it.key.endsWith("BusOffCount") }.maxOfOrNull { it.value } ?: 0.0
            /**
             * maxSignalLatency val.
             */
            val maxSignalLatency = canFrames.filter { it.key.endsWith("SignalLatencyMs") }.maxOfOrNull { it.value } ?: 0.0

            /**
             * brownoutCount val.
             */
            val brownoutCount = allFrames.filter { it.key == "Diagnostics/Power/BrownoutCount" }.maxOfOrNull { it.value } ?: 0.0

            /**
             * motorFaultFrames val.
             */
            val motorFaultFrames = allFrames.filter { it.key.startsWith("Diagnostics/Motor/") && it.key.endsWith("/Faults") }
            /**
             * hasMotorFaults val.
             */
            val hasMotorFaults = motorFaultFrames.any { it.value > 0.0 }

            framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/System/MaxCANBusUtilization", maxBusUtil))
            framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/System/TotalCANBusErrors", totalErrorCount))
            framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/System/CANBusOffs", totalBusOffs))
            framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/System/MaxCANBusLatencyMs", maxSignalLatency))
            framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/System/BrownoutCount", brownoutCount))

            /**
             * newTags val.
             */
            val newTags = session.tags.toMutableList()
            if (commsLosses > 0) newTags.add("CommsLoss")
            if (loopOverruns > 5) newTags.add("LoopOverruns")
            if (minVoltage < 9.5 && minVoltage > 0.0) newTags.add("LowBattery")
            if (totalErrorCount > 0.0 || totalBusOffs > 0.0) newTags.add("CANBusFault")
            if (maxBusUtil >= 0.90) newTags.add("CANBusSaturated")
            if (brownoutCount > 0.0) newTags.add("Brownout")
            if (hasMotorFaults) newTags.add("MotorFault")

            /**
             * uniqueTags val.
             */
            val uniqueTags = newTags.distinct()
            if (uniqueTags.size != session.tags.size) {
                databaseService.updateSessionTags(session.sessionId, uniqueTags)
            }

            // 1. Drivetrain SysId Characterization
            /**
             * voltages val.
             */
            val voltages = allFrames.filter { it.key == "/Drive/Voltage" || it.key == "Drive/Voltage" }
            /**
             * velocities val.
             */
            val velocities = allFrames.filter { it.key == "/Drive/Velocity" || it.key == "Drive/Velocity" }
            /**
             * accelerations val.
             */
            val accelerations = allFrames.filter { it.key == "/Drive/Acceleration" || it.key == "Drive/Acceleration" }

            if (voltages.isNotEmpty() && velocities.isNotEmpty()) {
                /**
                 * alignedData val.
                 */
                val alignedData = mutableListOf<AlignedDataRow>()
                /**
                 * timeMap val.
                 */
                val timeMap = voltages.associateBy { it.timestampMs }

                /**
                 * directionChanges val.
                 */
                val directionChanges = mutableListOf<Long>()
                /**
                 * lastSign var.
                 */
                var lastSign = 0.0
                /**
                 * sortedVelocities val.
                 */
                val sortedVelocities = velocities.sortedBy { it.timestampMs }
                for (v in sortedVelocities) {
                    /**
                     * currentSign val.
                     */
                    val currentSign = sign(v.value)
                    if (currentSign != 0.0 && currentSign != lastSign) {
                        directionChanges.add(v.timestampMs)
                        lastSign = currentSign
                    }
                }

                /**
                 * sortedAccels val.
                 */
                val sortedAccels = accelerations.sortedBy { it.timestampMs }
                /**
                 * accelIdx var.
                 */
                var accelIdx = 0

                for (v in sortedVelocities) {
                    /**
                     * t val.
                     */
                    val t = v.timestampMs
                    /**
                     * isNearDirectionChange val.
                     */
                    val isNearDirectionChange = directionChanges.any { abs(it - t) <= 50 }
                    if (isNearDirectionChange) continue

                    /**
                     * volt val.
                     */
                    val volt = timeMap[t]?.value ?: continue

                    /**
                     * accel val.
                     */
                    val accel = if (sortedAccels.isNotEmpty()) {
                        while (accelIdx < sortedAccels.size - 1 &&
                            abs(sortedAccels[accelIdx + 1].timestampMs - t) <= abs(sortedAccels[accelIdx].timestampMs - t)
                        ) {
                            accelIdx++
                        }
                        sortedAccels[accelIdx].value
                    } else 0.0

                    alignedData.add(AlignedDataRow(t, volt, v.value, accel))
                }

                /**
                 * finalAlignedData val.
                 */
                val finalAlignedData = if (alignedData.isNotEmpty() && alignedData.all { it.accel == 0.0 }) {
                    /**
                     * approxRows val.
                     */
                    val approxRows = mutableListOf<AlignedDataRow>()
                    /**
                     * sorted val.
                     */
                    val sorted = alignedData.sortedBy { it.timestampMs }
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
                    approxRows
                } else {
                    alignedData
                }

                if (finalAlignedData.size >= 10) {
                    /**
                     * summary val.
                     */
                    val summary = sysIdService.analyzeRawData(finalAlignedData)
                    if (summary.rSquared > 0.1) {
                        framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/kS", summary.kS))
                        framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/kV", summary.kV))
                        framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/kA", summary.kA))
                        framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/R2", summary.rSquared))

                        if (summary.kA > 1e-6) {
                            framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/ADRC_b0", 1.0 / summary.kA))
                        }
                    }
                }
            }

            // 2. Individual Subsystem & Motor SysId Characterization
            /**
             * motorVoltages val.
             */
            val motorVoltages = mutableMapOf<String, MutableMap<Long, Double>>()
            /**
             * motorVelocities val.
             */
            val motorVelocities = mutableMapOf<String, MutableMap<Long, Double>>()

            for (frame in allFrames) {
                /**
                 * cleanKey val.
                 */
                val cleanKey = frame.key.removePrefix("/")
                if (cleanKey.startsWith("Hardware/Motors/")) {
                    /**
                     * parts val.
                     */
                    val parts = cleanKey.split("/")
                    if (parts.size >= 4) {
                        /**
                         * motorName val.
                         */
                        val motorName = parts[2]
                        /**
                         * metric val.
                         */
                        val metric = parts[3].lowercase()
                        /**
                         * t val.
                         */
                        val t = frame.timestampMs
                        when {
                            metric.contains("volt") || metric.contains("power") -> {
                                /**
                                 * voltVal val.
                                 */
                                val voltVal = if (metric.contains("power") && abs(frame.value) <= 1.0) frame.value * 12.0 else frame.value
                                motorVoltages.getOrPut(motorName) { mutableMapOf() }[t] = voltVal
                            }
                            metric.contains("vel") || metric.contains("speed") -> {
                                motorVelocities.getOrPut(motorName) { mutableMapOf() }[t] = frame.value
                            }
                        }
                    }
                }
            }

            for ((motorName, velocitiesMap) in motorVelocities) {
                /**
                 * voltagesMap val.
                 */
                val voltagesMap = motorVoltages[motorName] ?: continue
                if (velocitiesMap.size < 10 || voltagesMap.size < 10) continue

                /**
                 * alignedRows val.
                 */
                val alignedRows = mutableListOf<AlignedDataRow>()
                /**
                 * sortedTimes val.
                 */
                val sortedTimes = velocitiesMap.keys.sorted()

                /**
                 * lastTime var.
                 */
                var lastTime = 0L
                /**
                 * lastVel var.
                 */
                var lastVel = 0.0

                for (t in sortedTimes) {
                    /**
                     * vel val.
                     */
                    val vel = velocitiesMap[t] ?: continue
                    /**
                     * volt val.
                     */
                    val volt = voltagesMap[t] ?: continue

                    /**
                     * accel val.
                     */
                    val accel = if (lastTime == 0L) 0.0 else {
                        /**
                         * dt val.
                         */
                        val dt = (t - lastTime) / 1000.0
                        if (dt > 1e-4) (vel - lastVel) / dt else 0.0
                    }

                    alignedRows.add(AlignedDataRow(t, volt, vel, accel))
                    lastTime = t
                    lastVel = vel
                }

                if (alignedRows.size >= 10) {
                    /**
                     * summary val.
                     */
                    val summary = sysIdService.analyzeRawData(alignedRows)
                    if (summary.rSquared > 0.5) {
                        framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/Motors/$motorName/kS", summary.kS))
                        framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/Motors/$motorName/kV", summary.kV))
                        framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/Motors/$motorName/kA", summary.kA))
                        framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/Motors/$motorName/R2", summary.rSquared))

                        if (summary.kA > 1e-6) {
                            framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/Motors/$motorName/ADRC_b0", 1.0 / summary.kA))
                        }
                    }

                    // Estimate kG (Gravity Feedforward) for vertical elevators/arms (non-drivetrain)
                    /**
                     * isDrivetrain val.
                     */
                    val isDrivetrain = motorName.lowercase() in listOf("fl", "fr", "rl", "rr", "bl", "br", "frontleft", "frontright", "rearleft", "rearright")
                    if (!isDrivetrain) {
                        /**
                         * holdingVoltages val.
                         */
                        val holdingVoltages = alignedRows.filter { row ->
                            /**
                             * absV val.
                             */
                            val absV = if (row.velocity < 0.0) -row.velocity else row.velocity
                            /**
                             * absA val.
                             */
                            val absA = if (row.accel < 0.0) -row.accel else row.accel
                            absV < 0.05 && absA < 0.1
                        }.map { it.voltage }
                        if (holdingVoltages.size >= 10) {
                            /**
                             * kgEstimate val.
                             */
                            val kgEstimate = holdingVoltages.average()
                            /**
                             * absKg val.
                             */
                            val absKg = if (kgEstimate < 0.0) -kgEstimate else kgEstimate
                            if (absKg > 0.1) {
                                framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/Motors/$motorName/kG", kgEstimate))
                            }
                        }
                    }
                }
            }

            // 3. Drivetrain Angular Characterization
            /**
             * flVolts val.
             */
            val flVolts = motorVoltages["fl"] ?: motorVoltages["FL"] ?: motorVoltages["frontleft"]
            /**
             * rlVolts val.
             */
            val rlVolts = motorVoltages["rl"] ?: motorVoltages["RL"] ?: motorVoltages["bl"] ?: motorVoltages["BL"] ?: motorVoltages["rearleft"]
            /**
             * frVolts val.
             */
            val frVolts = motorVoltages["fr"] ?: motorVoltages["FR"] ?: motorVoltages["frontright"]
            /**
             * rrVolts val.
             */
            val rrVolts = motorVoltages["rr"] ?: motorVoltages["RR"] ?: motorVoltages["br"] ?: motorVoltages["BR"] ?: motorVoltages["rearright"]

            /**
             * leftSideVolts val.
             */
            val leftSideVolts = mutableMapOf<Long, Double>()
            /**
             * rightSideVolts val.
             */
            val rightSideVolts = mutableMapOf<Long, Double>()

            if (flVolts != null) {
                for ((t, v) in flVolts) leftSideVolts[t] = (leftSideVolts[t] ?: 0.0) + v * 0.5
            }
            if (rlVolts != null) {
                for ((t, v) in rlVolts) leftSideVolts[t] = (leftSideVolts[t] ?: 0.0) + v * 0.5
            }
            if (frVolts != null) {
                for ((t, v) in frVolts) rightSideVolts[t] = (rightSideVolts[t] ?: 0.0) + v * 0.5
            }
            if (rrVolts != null) {
                for ((t, v) in rrVolts) rightSideVolts[t] = (rightSideVolts[t] ?: 0.0) + v * 0.5
            }

            /**
             * angularVoltages val.
             */
            val angularVoltages = mutableMapOf<Long, Double>()
            for (t in leftSideVolts.keys) {
                /**
                 * lv val.
                 */
                val lv = leftSideVolts[t] ?: continue
                /**
                 * rv val.
                 */
                val rv = rightSideVolts[t] ?: continue
                angularVoltages[t] = lv - rv
            }

            /**
             * omegas val.
             */
            val omegas = allFrames.filter { it.key == "Drive/Velocity_Omega" || it.key == "/Drive/Velocity_Omega" }
            if (angularVoltages.isNotEmpty() && omegas.isNotEmpty()) {
                /**
                 * alignedAngData val.
                 */
                val alignedAngData = mutableListOf<AlignedDataRow>()
                /**
                 * sortedOmegas val.
                 */
                val sortedOmegas = omegas.sortedBy { it.timestampMs }

                /**
                 * lastTime var.
                 */
                var lastTime = 0L
                /**
                 * lastOmega var.
                 */
                var lastOmega = 0.0

                for (o in sortedOmegas) {
                    /**
                     * t val.
                     */
                    val t = o.timestampMs
                    /**
                     * volt val.
                     */
                    val volt = angularVoltages[t] ?: continue

                    /**
                     * accel val.
                     */
                    val accel = if (lastTime == 0L) 0.0 else {
                        /**
                         * dt val.
                         */
                        val dt = (t - lastTime) / 1000.0
                        if (dt > 1e-4) (o.value - lastOmega) / dt else 0.0
                    }

                    alignedAngData.add(AlignedDataRow(t, volt, o.value, accel))
                    lastTime = t
                    lastOmega = o.value
                }

                if (alignedAngData.size >= 10) {
                    /**
                     * angSummary val.
                     */
                    val angSummary = sysIdService.analyzeRawData(alignedAngData)
                    if (angSummary.rSquared > 0.1) {
                        framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/Angular/kS", angSummary.kS))
                        framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/Angular/kV", angSummary.kV))
                        framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/Angular/kA", angSummary.kA))
                        framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/Angular/R2", angSummary.rSquared))
                        if (angSummary.kA > 1e-6) {
                            framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/SysId/Angular/ADRC_b0", 1.0 / angSummary.kA))
                        }
                    }
                }
            }

            // 4. Wheel Slippage / Traction Loss Calculation
            /**
             * ekfVels val.
             */
            val ekfVels = allFrames.filter { it.key == "Drive/Velocity" || it.key == "/Drive/Velocity" }
            if (ekfVels.isNotEmpty() && motorVelocities.isNotEmpty()) {
                /**
                 * slippages val.
                 */
                val slippages = mutableListOf<Double>()
                for (ev in ekfVels) {
                    /**
                     * t val.
                     */
                    val t = ev.timestampMs
                    /**
                     * ekfV val.
                     */
                    val ekfV = abs(ev.value)

                    /**
                     * wheelSum var.
                     */
                    var wheelSum = 0.0
                    /**
                     * wheelCount var.
                     */
                    var wheelCount = 0
                    for (motorName in listOf("fl", "fr", "rl", "rr", "bl", "br")) {
                        /**
                         * mVel val.
                         */
                        val mVel = motorVelocities[motorName]?.get(t) ?: continue
                        wheelSum += abs(mVel)
                        wheelCount++
                    }

                    if (wheelCount > 0) {
                        /**
                         * avgWheelV val.
                         */
                        val avgWheelV = wheelSum / wheelCount
                        /**
                         * diff val.
                         */
                        val diff = abs(avgWheelV - ekfV)
                        /**
                         * denominator val.
                         */
                        val denominator = maxOf(ekfV, 0.1)
                        slippages.add(diff / denominator)
                    }
                }
                if (slippages.isNotEmpty()) {
                    framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/Drive/TractionLoss", slippages.average()))
                }
            }

            // 5. Driver Jitter Analysis
            /**
             * j val.
             */
            val j = driverAnalysisService.analyzeDriverJitter(session.sessionId)
            if (j.peakFrequencyHz > 0.1) {
                framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/Driver/RecommendedExponent", j.recommendedExponent))
                framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/Driver/RecommendedSlewRate", if (j.recommendedSlewRate == Double.MAX_VALUE) 999.0 else j.recommendedSlewRate))
                framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/Driver/PeakJitterFrequency", j.peakFrequencyHz))
                framesToInsert.add(TelemetryFrame(session.createdAt, session.sessionId, "Diagnostics/Driver/JitterPresent", if (j.hasJitter) 1.0 else 0.0))
            }

            if (framesToInsert.isNotEmpty()) {
                databaseService.insertTelemetryFrames(framesToInsert)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cleanKeyToDeviceName(key: String): String {
        // e.g. "/Drive/MotorFL/Current" -> "MotorFL"
        // e.g. "Drive/Motors/FrontLeftCurrent" -> "FrontLeft"
        /**
         * parts val.
         */
        val parts = key.split("/")
        if (parts.size >= 2) {
            /**
             * last val.
             */
            val last = parts.last()
            if (last.lowercase() == "current" || last.lowercase() == "amps") {
                return parts[parts.size - 2]
            }
        }
        /**
         * lastPart val.
         */
        val lastPart = parts.last()
        return lastPart
            .replace("current", "", ignoreCase = true)
            .replace("amps", "", ignoreCase = true)
            .replace("/", "")
            .ifEmpty { "Motor" }
    }
}
