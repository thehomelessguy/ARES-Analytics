package com.ares.analytics.service

import com.ares.analytics.shared.Session
import com.ares.analytics.shared.SessionSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SummaryEngineService(private val databaseService: DatabaseService) {

    suspend fun generateSummary(session: Session): SessionSummary = withContext(Dispatchers.Default) {
        val totalFrames = databaseService.countTelemetryFrames(session.sessionId)
        val batchSize = 10000L

        var minBattery = Double.MAX_VALUE
        var maxDrift = 0.0
        val loopTimes = mutableListOf<Double>()
        val motorCurrents = mutableMapOf<String, MutableList<Double>>()
        val visionAcceptanceValues = mutableListOf<Double>()
        
        val crossTrackErrors = mutableListOf<Double>()
        val visionLatencies = mutableListOf<Double>()
        var lastVoltage = 12.0
        val currentDrawsByTime = mutableMapOf<Long, Double>()
        val resistanceEstimates = mutableListOf<Double>()
        val motorTemps = mutableMapOf<String, Double>()
        val maxMotorTemps = mutableMapOf<String, Double>()
        val lastTimeMs = mutableMapOf<String, Long>()

        for (offset in 0L until totalFrames step batchSize) {
            val frames = databaseService.getTelemetryRangeBatched(session.sessionId, 0L, Long.MAX_VALUE, batchSize, offset)
            for (frame in frames) {
                val keyLower = frame.key.lowercase()
                val t = frame.timestampMs

                // 1. Battery Voltage
                if (keyLower.contains("voltage") || keyLower.contains("battery")) {
                    if (frame.value > 1.0) { // filter out zero noise
                        minBattery = minOf(minBattery, frame.value)
                        val drop = lastVoltage - frame.value
                        if (drop > 0.5) {
                            val totalAmps = currentDrawsByTime[t] ?: 0.0
                            if (totalAmps > 5.0) {
                                resistanceEstimates.add(drop / totalAmps)
                            }
                        }
                        lastVoltage = maxOf(lastVoltage, frame.value)
                    }
                }

                // 2. EKF Drift & Path Deviation
                if (keyLower.contains("drift") || keyLower.contains("ekf") || keyLower.contains("poseerror")) {
                    maxDrift = maxOf(maxDrift, frame.value)
                }
                if (keyLower.contains("crosstrack") || keyLower.contains("xte")) {
                    crossTrackErrors.add(frame.value)
                }

                // 3. Loop Time
                if (keyLower.contains("loop") || keyLower.contains("period")) {
                    loopTimes.add(frame.value)
                }

                // 4. Motor Currents & Thermal Prediction
                if (keyLower.contains("current") && !keyLower.contains("battery")) {
                    val cleanName = cleanKeyToDeviceName(frame.key)
                    motorCurrents.getOrPut(cleanName) { mutableListOf() }.add(frame.value)
                    currentDrawsByTime[t] = (currentDrawsByTime[t] ?: 0.0) + frame.value

                    val lastTime = lastTimeMs.getOrDefault(cleanName, t - 20L)
                    val dt = (t - lastTime) / 1000.0
                    if (dt > 0 && dt < 1.0) {
                        val currentTemp = motorTemps.getOrDefault(cleanName, 25.0)
                        val i = frame.value
                        val rWind = 0.05
                        val rTherm = 100.0
                        val newTemp = currentTemp + (i * i * rWind * dt) - ((currentTemp - 25.0) / rTherm * dt)
                        motorTemps[cleanName] = newTemp
                        maxMotorTemps[cleanName] = maxOf(maxMotorTemps.getOrDefault(cleanName, 25.0), newTemp)
                    }
                    lastTimeMs[cleanName] = t
                }

                // 5. Vision Acceptance & Latency
                if (keyLower.contains("vision")) {
                    if (keyLower.contains("acceptance") || keyLower.contains("valid") || keyLower.contains("quality")) {
                        visionAcceptanceValues.add(frame.value)
                    }
                    if (keyLower.contains("latency")) {
                        visionLatencies.add(frame.value)
                    }
                }
            }
        }

        // Finalize stats
        val finalMinBattery = if (minBattery == Double.MAX_VALUE) 12.0 else minBattery
        val avgLoop = if (loopTimes.isNotEmpty()) loopTimes.average() else 0.0
        val p95Loop = if (loopTimes.isNotEmpty()) {
            val sorted = loopTimes.sorted()
            val index = (sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)
            sorted[index]
        } else 0.0

        val motorCurrentAverages = motorCurrents.mapValues { (_, values) ->
            if (values.isNotEmpty()) values.average() else 0.0
        }

        val visionRate = if (visionAcceptanceValues.isNotEmpty()) visionAcceptanceValues.average() else 1.0
        val avgCrossTrack = if (crossTrackErrors.isNotEmpty()) crossTrackErrors.map { kotlin.math.abs(it) }.average() else 0.0
        val avgResistance = if (resistanceEstimates.isNotEmpty()) resistanceEstimates.average() else 0.0
        val avgVisionLat = if (visionLatencies.isNotEmpty()) visionLatencies.average() else 0.0

        val summary = SessionSummary(
            sessionId = session.sessionId,
            teamId = session.teamId,
            seasonId = session.seasonId,
            robotId = session.robotId,
            createdAt = session.createdAt,
            durationMs = session.durationMs,
            minBatteryVoltage = finalMinBattery,
            maxEkfDrift = maxDrift,
            avgLoopTimeMs = avgLoop,
            p95LoopTimeMs = p95Loop,
            motorCurrentAverages = motorCurrentAverages,
            visionAcceptanceRate = visionRate,
            avgCrossTrackError = avgCrossTrack,
            avgBatteryResistance = avgResistance,
            maxMotorTemps = maxMotorTemps,
            avgVisionLatencyMs = avgVisionLat,
            tags = session.tags,
            matchNumber = session.matchNumber,
            allianceColor = session.allianceColor
        )

        databaseService.insertSessionSummary(summary)
        summary
    }

    private fun cleanKeyToDeviceName(key: String): String {
        // e.g. "/Drive/MotorFL/Current" -> "MotorFL"
        // e.g. "Drive/Motors/FrontLeftCurrent" -> "FrontLeft"
        val parts = key.split("/")
        if (parts.size >= 2) {
            val last = parts.last()
            if (last.lowercase() == "current" || last.lowercase() == "amps") {
                return parts[parts.size - 2]
            }
        }
        val lastPart = parts.last()
        return lastPart
            .replace("current", "", ignoreCase = true)
            .replace("amps", "", ignoreCase = true)
            .replace("/", "")
            .ifEmpty { "Motor" }
    }
}
