package com.ares.analytics.service.log

import com.ares.analytics.service.DatabaseService
import com.ares.analytics.service.FrameBatcher
import com.ares.analytics.shared.RobotActionRecord
import com.ares.analytics.shared.TelemetryFrame
import kotlinx.serialization.json.*
import java.io.File

/**
 * Metadata extracted from an action log file's envelope fields.
 */
data class ActionLogMetadata(
    /**
     * durationMs val.
     */
    val durationMs: Long,
    /**
     * matchNumber val.
     */
    val matchNumber: Int,
    /**
     * alliance val.
     */
    val alliance: String
)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class JsonlLogDecoder(private val databaseService: DatabaseService) {

    suspend fun parseJsonlLog(file: File, sessionId: String, batcher: FrameBatcher) {
        file.bufferedReader(Charsets.UTF_8).use { reader ->
            /**
             * line var.
             */
            var line: String? = reader.readLine()
            while (line != null) {
                /**
                 * trimmed val.
                 */
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    try {
                        /**
                         * obj val.
                         */
                        val obj = Json.parseToJsonElement(trimmed).jsonObject
                        // Look for timestamp
                        /**
                         * timestampMs val.
                         */
                        val timestampMs = obj["timestampMs"]?.jsonPrimitive?.longOrNull
                            ?: obj["time"]?.jsonPrimitive?.longOrNull
                            ?: obj["timestamp"]?.jsonPrimitive?.longOrNull

                        if (timestampMs != null) {
                            for ((key, value) in obj) {
                                if (key == "timestampMs" || key == "time" || key == "timestamp") continue
                                /**
                                 * doubleVal val.
                                 */
                                val doubleVal = value.jsonPrimitive.doubleOrNull
                                if (doubleVal != null) {
                                    batcher.add(TelemetryFrame(timestampMs, sessionId, key, doubleVal))
                                } else if (value.jsonPrimitive.isString || value.jsonPrimitive.booleanOrNull != null) {
                                    /**
                                     * strVal val.
                                     */
                                    val strVal = value.jsonPrimitive.content
                                    batcher.add(TelemetryFrame(timestampMs, sessionId, key, 0.0, strVal))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore bad lines
                    }
                }
                line = reader.readLine()
            }
        }
    }

    suspend fun parseActionLogJsonl(file: File, sessionId: String): ActionLogMetadata? {
        /**
         * actions val.
         */
        val actions = mutableListOf<RobotActionRecord>()
        /**
         * minTimestamp var.
         */
        var minTimestamp = Long.MAX_VALUE
        /**
         * maxTimestamp var.
         */
        var maxTimestamp = Long.MIN_VALUE
        /**
         * firstMatchNumber var.
         */
        var firstMatchNumber = 0
        /**
         * firstAlliance var.
         */
        var firstAlliance = "UNKNOWN"
        /**
         * isFirstLine var.
         */
        var isFirstLine = true

        file.bufferedReader(Charsets.UTF_8).use { reader ->
            /**
             * line var.
             */
            var line: String? = reader.readLine()
            while (line != null) {
                /**
                 * trimmed val.
                 */
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    try {
                        /**
                         * obj val.
                         */
                        val obj = Json.parseToJsonElement(trimmed).jsonObject

                        /**
                         * runId val.
                         */
                        val runId = obj["run_id"]?.jsonPrimitive?.contentOrNull ?: ""
                        /**
                         * robotId val.
                         */
                        val robotId = obj["robot_id"]?.jsonPrimitive?.contentOrNull ?: ""
                        /**
                         * matchNumber val.
                         */
                        val matchNumber = obj["match_number"]?.jsonPrimitive?.intOrNull ?: 0
                        /**
                         * alliance val.
                         */
                        val alliance = obj["alliance"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN"
                        /**
                         * actionType val.
                         */
                        val actionType = obj["type"]?.jsonPrimitive?.contentOrNull ?: "Unknown"

                        // Capture envelope metadata from the first line
                        if (isFirstLine) {
                            firstMatchNumber = matchNumber
                            firstAlliance = alliance
                            isFirstLine = false
                        }

                        /**
                         * payload val.
                         */
                        val payload = obj["payload"]?.jsonObject
                        /**
                         * timestampMs val.
                         */
                        val timestampMs = payload?.get("timestampMs")?.jsonPrimitive?.longOrNull ?: 0L
                        /**
                         * payloadJson val.
                         */
                        val payloadJson = payload?.toString() ?: "{}"

                        if (timestampMs > 0L) {
                            if (timestampMs < minTimestamp) minTimestamp = timestampMs
                            if (timestampMs > maxTimestamp) maxTimestamp = timestampMs

                            actions.add(RobotActionRecord(
                                timestampMs = timestampMs,
                                sessionId = sessionId,
                                runId = runId,
                                robotId = robotId,
                                matchNumber = matchNumber,
                                alliance = alliance,
                                actionType = actionType,
                                payloadJson = payloadJson
                            ))
                        }
                    } catch (e: Exception) {
                        // Skip malformed lines
                    }
                }
                line = reader.readLine()
            }
        }

        if (actions.isEmpty()) return null

        databaseService.insertRobotActionsBulk(actions)

        return ActionLogMetadata(
            durationMs = if (maxTimestamp > minTimestamp) maxTimestamp - minTimestamp else 0L,
            matchNumber = firstMatchNumber,
            alliance = firstAlliance
        )
    }
}
