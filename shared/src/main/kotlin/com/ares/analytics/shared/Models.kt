package com.ares.analytics.shared

import kotlinx.serialization.Serializable

// ────────────────────────────────────────────────────────────────────────────
// Workspace & Configuration
// ────────────────────────────────────────────────────────────────────────────

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class WorkspaceConfig(
    /**
     * id val.
     */
    val id: String = "",
    /**
     * teamId val.
     */
    val teamId: String,
    /**
     * seasonId val.
     */
    val seasonId: String,
    /**
     * robotId val.
     */
    val robotId: String,
    /**
     * robotName val.
     */
    val robotName: String = "",
    /**
     * projectPath val.
     */
    val projectPath: String,
    /**
     * league val.
     */
    val league: League,
    /**
     * nt4Host val.
     */
    val nt4Host: String? = null,
    /**
     * eventCode val.
     */
    val eventCode: String? = null,
    /**
     * toaApiKey val.
     */
    val toaApiKey: String? = null,
    /**
     * tbaApiKey val.
     */
    val tbaApiKey: String? = null,
    /**
     * googleClientId val.
     */
    val googleClientId: String? = null,
    /**
     * firebaseApiKey val.
     */
    val firebaseApiKey: String? = null,
    /**
     * googleClientSecret val.
     */
    val googleClientSecret: String? = null,
    /**
     * simulatorCommand val.
     */
    val simulatorCommand: String? = null,
    /**
     * aiMode val.
     */
    val aiMode: String? = "STUDIO",
    /**
     * geminiApiKey val.
     */
    val geminiApiKey: String? = null,
    /**
     * geminiModel val.
     */
    val geminiModel: String? = "gemini-1.5-flash",
    /**
     * vertexServiceAccountPath val.
     */
    val vertexServiceAccountPath: String? = null,
    /**
     * vertexProjectId val.
     */
    val vertexProjectId: String? = null,
    /**
     * vertexLocation val.
     */
    val vertexLocation: String? = "us-central1",
    /**
     * colorblindMode val.
     */
    val colorblindMode: Boolean = false,
    /**
     * highContrastMode val.
     */
    val highContrastMode: Boolean = false,
    /**
     * touchOptimizedMode val.
     */
    val touchOptimizedMode: Boolean = false
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
enum class League {
    FTC, FRC
}

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class AppWorkspaces(
    /**
     * activeWorkspaceId val.
     */
    val activeWorkspaceId: String?,
    /**
     * workspaces val.
     */
    val workspaces: List<WorkspaceConfig>
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
data class RobotProfile(
    /**
     * robotId val.
     */
    val robotId: String,
    /**
     * league val.
     */
    val league: League,
    /**
     * seasonId val.
     */
    val seasonId: String,
    /**
     * name val.
     */
    val name: String
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
data class TeamRobotsResponse(
    /**
     * robots val.
     */
    val robots: List<RobotProfile>
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
data class AddRobotRequest(
    /**
     * teamId val.
     */
    val teamId: String,
    /**
     * robot val.
     */
    val robot: RobotProfile
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
data class DeleteRobotRequest(
    /**
     * teamId val.
     */
    val teamId: String,
    /**
     * robotId val.
     */
    val robotId: String
)

// ────────────────────────────────────────────────────────────────────────────
// Session & Telemetry
// ────────────────────────────────────────────────────────────────────────────

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
enum class SessionMode {
    LIVE_STREAMING,
    LIVE_REWIND,
    HISTORICAL_REPLAY
}

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class Session(
    /**
     * sessionId val.
     */
    val sessionId: String,
    /**
     * teamId val.
     */
    val teamId: String,
    /**
     * seasonId val.
     */
    val seasonId: String,
    /**
     * robotId val.
     */
    val robotId: String,
    /**
     * createdAt val.
     */
    val createdAt: Long,
    /**
     * durationMs val.
     */
    val durationMs: Long = 0L,
    /**
     * tags val.
     */
    val tags: List<String> = emptyList(),
    /**
     * matchNumber val.
     */
    val matchNumber: Int? = null,
    /**
     * allianceColor val.
     */
    val allianceColor: String? = null
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
data class SessionSummary(
    /**
     * sessionId val.
     */
    val sessionId: String,
    /**
     * teamId val.
     */
    val teamId: String,
    /**
     * seasonId val.
     */
    val seasonId: String,
    /**
     * robotId val.
     */
    val robotId: String,
    /**
     * createdAt val.
     */
    val createdAt: Long,
    /**
     * durationMs val.
     */
    val durationMs: Long = 0L,
    /**
     * minBatteryVoltage val.
     */
    val minBatteryVoltage: Double = 0.0,
    /**
     * maxEkfDrift val.
     */
    val maxEkfDrift: Double = 0.0,
    /**
     * avgLoopTimeMs val.
     */
    val avgLoopTimeMs: Double = 0.0,
    /**
     * p95LoopTimeMs val.
     */
    val p95LoopTimeMs: Double = 0.0,
    /**
     * motorCurrentAverages val.
     */
    val motorCurrentAverages: Map<String, Double> = emptyMap(),
    /**
     * visionAcceptanceRate val.
     */
    val visionAcceptanceRate: Double = 0.0,
    /**
     * avgCrossTrackError val.
     */
    val avgCrossTrackError: Double = 0.0,
    /**
     * avgBatteryResistance val.
     */
    val avgBatteryResistance: Double = 0.0,
    /**
     * maxMotorTemps val.
     */
    val maxMotorTemps: Map<String, Double> = emptyMap(),
    /**
     * avgVisionLatencyMs val.
     */
    val avgVisionLatencyMs: Double = 0.0,
    /**
     * tags val.
     */
    val tags: List<String> = emptyList(),
    /**
     * matchNumber val.
     */
    val matchNumber: Int? = null,
    /**
     * allianceColor val.
     */
    val allianceColor: String? = null,
    /**
     * rawGcsPath val.
     */
    val rawGcsPath: String? = null,
    /**
     * fileSizeBytes val.
     */
    val fileSizeBytes: Long = 0L
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
data class SessionAnnotation(
    /**
     * annotationId val.
     */
    val annotationId: String,
    /**
     * sessionId val.
     */
    val sessionId: String,
    /**
     * text val.
     */
    val text: String,
    /**
     * createdAt val.
     */
    val createdAt: Long,
    /**
     * authorId val.
     */
    val authorId: String? = null
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
data class TelemetryFrame(
    /**
     * timestampMs val.
     */
    val timestampMs: Long,
    /**
     * sessionId val.
     */
    val sessionId: String,
    /**
     * key val.
     */
    val key: String,
    /**
     * value val.
     */
    val value: Double,
    /**
     * stringValue val.
     */
    val stringValue: String? = null
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
data class RobotActionRecord(
    /**
     * timestampMs val.
     */
    val timestampMs: Long,
    /**
     * sessionId val.
     */
    val sessionId: String,
    /**
     * runId val.
     */
    val runId: String,
    /**
     * robotId val.
     */
    val robotId: String,
    /**
     * matchNumber val.
     */
    val matchNumber: Int = 0,
    /**
     * alliance val.
     */
    val alliance: String = "UNKNOWN",
    /**
     * actionType val.
     */
    val actionType: String,
    /**
     * payloadJson val.
     */
    val payloadJson: String
)

// ────────────────────────────────────────────────────────────────────────────
// Alert System
// ────────────────────────────────────────────────────────────────────────────

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class AlertRecord(
    /**
     * alertId val.
     */
    val alertId: String,
    /**
     * sessionId val.
     */
    val sessionId: String,
    /**
     * ruleKey val.
     */
    val ruleKey: String,
    /**
     * triggerTimestampMs val.
     */
    val triggerTimestampMs: Long,
    /**
     * resolveTimestampMs val.
     */
    val resolveTimestampMs: Long? = null,
    /**
     * durationMs val.
     */
    val durationMs: Long = 0L,
    /**
     * peakValue val.
     */
    val peakValue: Double = 0.0,
    /**
     * triaged val.
     */
    val triaged: Boolean = false
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
data class ThresholdRule(
    /**
     * key val.
     */
    val key: String,
    /**
     * displayName val.
     */
    val displayName: String,
    /**
     * minValue val.
     */
    val minValue: Double? = null,
    /**
     * maxValue val.
     */
    val maxValue: Double? = null,
    /**
     * audibleAlert val.
     */
    val audibleAlert: Boolean = true
)

// ────────────────────────────────────────────────────────────────────────────
// Hardware Topology
// ────────────────────────────────────────────────────────────────────────────

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
enum class TopologyNodeType {
    CONTROL_HUB, EXPANSION_HUB, SRS_HUB,
    ROBORIO, CANIVORE,
    MOTOR, CAN_MOTOR_CONTROLLER, SERVO,
    CAMERA, ODOMETRY_COMPUTER, IMU,
    COLOR_SENSOR, DISTANCE_SENSOR, BEAM_BREAK, ANALOG_SENSOR,
    CAN_CODER, PIGEON_IMU, POWER_DISTRIBUTION
}

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class TopologyNode(
    /**
     * id val.
     */
    val id: String,
    /**
     * type val.
     */
    val type: TopologyNodeType,
    /**
     * displayName val.
     */
    val displayName: String,
    /**
     * parentId val.
     */
    val parentId: String? = null,
    /**
     * port val.
     */
    val port: Int? = null,
    /**
     * canId val.
     */
    val canId: Int? = null,
    /**
     * canBus val.
     */
    val canBus: String? = null,
    /**
     * busPosition val.
     */
    val busPosition: Int? = null,
    /**
     * connectionType val.
     */
    val connectionType: String? = null,
    /**
     * metadata val.
     */
    val metadata: Map<String, String> = emptyMap()
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
data class HardwareTopology(
    /**
     * robotId val.
     */
    val robotId: String,
    /**
     * nodes val.
     */
    val nodes: List<TopologyNode> = emptyList()
)

// ────────────────────────────────────────────────────────────────────────────
// Cloud Sync
// ────────────────────────────────────────────────────────────────────────────

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class UploadUrlRequest(
    /**
     * teamId val.
     */
    val teamId: String,
    /**
     * seasonId val.
     */
    val seasonId: String,
    /**
     * robotId val.
     */
    val robotId: String,
    /**
     * sessionId val.
     */
    val sessionId: String,
    /**
     * createdAt val.
     */
    val createdAt: Long,
    /**
     * summary val.
     */
    val summary: SessionSummary
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
data class UploadUrlResponse(
    /**
     * uploadUrl val.
     */
    val uploadUrl: String,
    /**
     * expiresAt val.
     */
    val expiresAt: Long
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
data class DownloadUrlResponse(
    /**
     * downloadUrl val.
     */
    val downloadUrl: String,
    /**
     * expiresAt val.
     */
    val expiresAt: Long
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
data class SyncRequest(
    /**
     * teamId val.
     */
    val teamId: String,
    /**
     * seasonId val.
     */
    val seasonId: String,
    /**
     * knownSessionIds val.
     */
    val knownSessionIds: List<String>
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
data class SyncResponse(
    /**
     * missingSummaries val.
     */
    val missingSummaries: List<SessionSummary>
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
data class DeleteSessionRequest(
    /**
     * sessionId val.
     */
    val sessionId: String,
    /**
     * teamId val.
     */
    val teamId: String
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
data class RawUploadUrlsRequest(
    /**
     * teamId val.
     */
    val teamId: String,
    /**
     * runTimestamp val.
     */
    val runTimestamp: String,
    /**
     * fileNames val.
     */
    val fileNames: List<String>
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
data class RawUploadUrlsResponse(
    /**
     * uploadUrls val.
     */
    val uploadUrls: Map<String, String>,
    /**
     * expiresAt val.
     */
    val expiresAt: Long
)

// ────────────────────────────────────────────────────────────────────────────
// AI Diagnostics
// ────────────────────────────────────────────────────────────────────────────

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class ForensicsRequest(
    /**
     * teamId val.
     */
    val teamId: String,
    /**
     * sessionId val.
     */
    val sessionId: String,
    /**
     * alerts val.
     */
    val alerts: List<AlertRecord>,
    /**
     * summary val.
     */
    val summary: SessionSummary,
    /**
     * topology val.
     */
    val topology: HardwareTopology? = null,
    /**
     * sysIdDrift val.
     */
    val sysIdDrift: Map<String, Double> = emptyMap()
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
data class HardwareFaultLocus(
    /**
     * failedNodeId val.
     */
    val failedNodeId: String,
    /**
     * interruptedLinkId val.
     */
    val interruptedLinkId: String? = null
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
data class ForensicsResponse(
    /**
     * probableRootCause val.
     */
    val probableRootCause: String,
    /**
     * confidenceScore val.
     */
    val confidenceScore: Double,
    /**
     * cascadingNodesAffected val.
     */
    val cascadingNodesAffected: List<String> = emptyList(),
    /**
     * hardwareFaultLocus val.
     */
    val hardwareFaultLocus: HardwareFaultLocus? = null,
    /**
     * recommendedActions val.
     */
    val recommendedActions: List<String> = emptyList()
)

// ────────────────────────────────────────────────────────────────────────────
// SysId Results
// ────────────────────────────────────────────────────────────────────────────

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class CalculatedSummary(
    /**
     * kS val.
     */
    val kS: Double = 0.0,
    /**
     * kV val.
     */
    val kV: Double = 0.0,
    /**
     * kA val.
     */
    val kA: Double = 0.0,
    /**
     * rSquared val.
     */
    val rSquared: Double = 0.0,
    /**
     * transientClassification val.
     */
    val transientClassification: TransientClassification = TransientClassification.UNKNOWN
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
enum class TransientClassification {
    UNDERDAMPED, OVERDAMPED, CRITICALLY_DAMPED, UNKNOWN
}

// ────────────────────────────────────────────────────────────────────────────
// Driver Profiles
// ────────────────────────────────────────────────────────────────────────────

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class DriverProfile(
    /**
     * name val.
     */
    val name: String,
    /**
     * deadbandExponent val.
     */
    val deadbandExponent: Double = 1.0,
    /**
     * slewRateLimit val.
     */
    val slewRateLimit: Double = Double.MAX_VALUE,
    /**
     * jitterPeakFrequencyHz val.
     */
    val jitterPeakFrequencyHz: Double = 0.0,
    /**
     * jitterAmplitude val.
     */
    val jitterAmplitude: Double = 0.0
)

// ────────────────────────────────────────────────────────────────────────────
// Path Planner Obstacles
// ────────────────────────────────────────────────────────────────────────────

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class PathPoint(val x: Double, val y: Double)

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
enum class FTCCoordinateSystem { DIAMOND, SQUARE }

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class FieldImageConfig(
    /**
     * imagePath val.
     */
    val imagePath: String = "",
    /**
     * rotationDegrees val.
     */
    val rotationDegrees: Double = 0.0,
    /**
     * cropLeft val.
     */
    val cropLeft: Double = 0.0,
    /**
     * cropRight val.
     */
    val cropRight: Double = 1.0,
    /**
     * cropTop val.
     */
    val cropTop: Double = 0.0,
    /**
     * cropBottom val.
     */
    val cropBottom: Double = 1.0,
    /**
     * widthMeters val.
     */
    val widthMeters: Double = 3.65,
    /**
     * heightMeters val.
     */
    val heightMeters: Double = 3.65,
    /**
     * ftcCoordinateSystem val.
     */
    val ftcCoordinateSystem: FTCCoordinateSystem = FTCCoordinateSystem.DIAMOND
)

@Serializable
sealed class Obstacle {
    abstract val id: String
    abstract val name: String
    abstract val locked: Boolean
    abstract val colorHex: String

    @Serializable
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class Polygon(
        override val id: String,
        override val name: String,
        /**
         * vertices val.
         */
        val vertices: List<PathPoint>,
        override val locked: Boolean = false,
        override val colorHex: String = "#E53935"
    ) : Obstacle()

    @Serializable
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class Circle(
        override val id: String,
        override val name: String,
        /**
         * centerX val.
         */
        val centerX: Double,
        /**
         * centerY val.
         */
        val centerY: Double,
        /**
         * radius val.
         */
        val radius: Double,
        override val locked: Boolean = false,
        override val colorHex: String = "#E53935"
    ) : Obstacle()

    @Serializable
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class Rectangle(
        override val id: String,
        override val name: String,
        /**
         * centerX val.
         */
        val centerX: Double,
        /**
         * centerY val.
         */
        val centerY: Double,
        /**
         * width val.
         */
        val width: Double,
        /**
         * height val.
         */
        val height: Double,
        /**
         * rotation val.
         */
        val rotation: Double = 0.0,
        override val locked: Boolean = false,
        override val colorHex: String = "#E53935"
    ) : Obstacle()
}

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class GamePiece(
    /**
     * id val.
     */
    val id: String,
    /**
     * name val.
     */
    val name: String,
    /**
     * x val.
     */
    val x: Double,
    /**
     * y val.
     */
    val y: Double,
    /**
     * type val.
     */
    val type: String = "Custom",
    /**
     * locked val.
     */
    val locked: Boolean = false
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
data class AprilTagPlacement(
    /**
     * id val.
     */
    val id: String,
    /**
     * tagId val.
     */
    val tagId: Int,
    /**
     * x val.
     */
    val x: Double,
    /**
     * y val.
     */
    val y: Double,
    /**
     * z val.
     */
    val z: Double = 0.5,
    /**
     * yawDegrees val.
     */
    val yawDegrees: Double = 0.0,
    /**
     * locked val.
     */
    val locked: Boolean = false
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
data class ConsoleMessage(
    /**
     * timestampMs val.
     */
    val timestampMs: Long,
    /**
     * text val.
     */
    val text: String,
    /**
     * severity val.
     */
    val severity: String
)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class ControllerBinding(
    /**
     * gamepadId val.
     */
    val gamepadId: String,
    /**
     * button val.
     */
    val button: String,
    /**
     * action val.
     */
    val action: String,
    /**
     * sourceFile val.
     */
    val sourceFile: String,
    /**
     * lineNumber val.
     */
    val lineNumber: Int
)

// ────────────────────────────────────────────────────────────────────────────
// Trajectory Playback
// ────────────────────────────────────────────────────────────────────────────

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class TrajectoryState(
    /**
     * timeSeconds val.
     */
    val timeSeconds: Double,
    /**
     * x val.
     */
    val x: Double,
    /**
     * y val.
     */
    val y: Double,
    /**
     * headingRad val.
     */
    val headingRad: Double,
    /**
     * velocity val.
     */
    val velocity: Double
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
data class Trajectory(
    /**
     * durationSeconds val.
     */
    val durationSeconds: Double,
    /**
     * states val.
     */
    val states: List<TrajectoryState>
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
data class FieldWaypoint(
    /**
     * id val.
     */
    val id: String,
    /**
     * name val.
     */
    val name: String,
    /**
     * x val.
     */
    val x: Double,
    /**
     * y val.
     */
    val y: Double,
    /**
     * headingDegrees val.
     */
    val headingDegrees: Double,
    /**
     * locked val.
     */
    val locked: Boolean = false
)