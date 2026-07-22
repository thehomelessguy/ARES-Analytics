package com.ares.analytics.viewmodel.field

import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.shared.GamePiece
import com.ares.analytics.viewmodel.FieldViewerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class FieldTopicSubscriber(
    private val nt4ClientService: Nt4ClientService,
    private val scope: CoroutineScope,
    private val stateFlow: MutableStateFlow<FieldViewerState>
) {
    init {
        scope.launch {
            nt4ClientService.isConnected.collect { connected ->
                stateFlow.update { currentState ->
                    currentState.copy(
                        isConnected = connected,
                        visionHasTarget = if (connected) currentState.visionHasTarget else false,
                        visionX = if (connected && currentState.visionHasTarget) currentState.visionX else null,
                        visionY = if (connected && currentState.visionHasTarget) currentState.visionY else null,
                        visionHeading = if (connected && currentState.visionHasTarget) currentState.visionHeading else null,
                        visionPoses = if (connected && currentState.visionHasTarget) currentState.visionPoses else emptyMap()
                    )
                }
            }
        }

        scope.launch {
            /**
             * lastEmit var.
             */
            var lastEmit = System.currentTimeMillis()
            /**
             * currentBuilder var.
             */
            var currentBuilder = FieldViewerStateBuilder(stateFlow.value)
            
            nt4ClientService.telemetryFlow.collect { frame ->
                /**
                 * key val.
                 */
                val key = frame.key
                /**
                 * value val.
                 */
                val value = frame.value
                
                when (key) {
                    "ARES/EstimatedPose/0", "Drive/Pose_X" -> { currentBuilder.trueX = value; if (key == "Drive/Pose_X") currentBuilder.ekfX = value }
                    "ARES/EstimatedPose/1", "Drive/Pose_Y" -> { currentBuilder.trueY = value; if (key == "Drive/Pose_Y") currentBuilder.ekfY = value }
                    "ARES/EstimatedPose/2" -> { 
                        currentBuilder.simHeading = value
                        if (currentBuilder.ekfHeading == null) currentBuilder.trueHeading = value 
                    }
                    "Drive/Pose_Heading", "Drive/Drive_Heading" -> { 
                        currentBuilder.ekfHeading = value
                        currentBuilder.trueHeading = value
                    }
                    "Drive/Odom_X", "pinpoint_x", "pinpoint/x" -> currentBuilder.odomX = value
                    "Drive/Odom_Y", "pinpoint_y", "pinpoint/y" -> currentBuilder.odomY = value
                    "Drive/Odom_Heading", "pinpoint_heading", "pinpoint/heading" -> currentBuilder.odomHeading = value
                    "Vision/HasTarget" -> {
                        /**
                         * hasTarget val.
                         */
                        val hasTarget = value > 0.5
                        currentBuilder.visionHasTarget = hasTarget
                        if (!hasTarget) {
                            currentBuilder.visionX = null
                            currentBuilder.visionY = null
                            currentBuilder.visionHeading = null
                            currentBuilder.visionPoses.clear()
                        }
                    }
                    "Vision/Pose_X", "Vision/Pose/X" -> if (currentBuilder.visionHasTarget) currentBuilder.visionX = value
                    "Vision/Pose_Y", "Vision/Pose/Y" -> if (currentBuilder.visionHasTarget) currentBuilder.visionY = value
                    "Vision/Pose_Heading", "Vision/Pose/Heading" -> if (currentBuilder.visionHasTarget) currentBuilder.visionHeading = value
                    "ARES/Input/isRedAlliance" -> currentBuilder.isRedAlliance = value > 0.5
                }

                if (key.startsWith("Superstructure/IndicatorLight/")) {
                    /**
                     * lightName val.
                     */
                    val lightName = key.substringAfterLast("/")
                    currentBuilder.indicatorLights[lightName] = value
                }

                if (key.startsWith("Vision/PoseArray/") || key.startsWith("AdvantageScope/VisionPose/")) {
                    if (currentBuilder.visionHasTarget) {
                        /**
                         * idx val.
                         */
                        val idx = key.substringAfterLast("/").toIntOrNull()
                        if (idx != null) {
                            currentBuilder.visionPoses[idx] = value
                        }
                    } else {
                        currentBuilder.visionPoses.clear()
                    }
                }

                if (key.startsWith("ARES/GamePieces/")) {
                    /**
                     * arrayIdx val.
                     */
                    val arrayIdx = key.substringAfterLast("/").toIntOrNull()
                    if (arrayIdx != null) {
                        /**
                         * pieceIdx val.
                         */
                        val pieceIdx = arrayIdx / 7
                        /**
                         * attributeIdx val.
                         */
                        val attributeIdx = arrayIdx % 7
                        /**
                         * currentPiece val.
                         */
                        val currentPiece = currentBuilder.liveGamePieces[pieceIdx] ?: GamePiece(
                            id = pieceIdx.toString(),
                            name = "Piece $pieceIdx",
                            x = 0.0,
                            y = 0.0,
                            type = "Decode (Ball)"
                        )
                        
                        /**
                         * updatedPiece val.
                         */
                        val updatedPiece = when (attributeIdx) {
                            0 -> currentPiece.copy(x = value)
                            1 -> currentPiece.copy(y = value)
                            else -> currentPiece
                        }
                        
                        currentBuilder.liveGamePieces[pieceIdx] = updatedPiece
                    }
                }
                
                /**
                 * now val.
                 */
                val now = System.currentTimeMillis()
                if (now - lastEmit > 16) {
                    stateFlow.update { currentBuilder.build(it) }
                    lastEmit = now
                    currentBuilder = FieldViewerStateBuilder(stateFlow.value)
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
class FieldViewerStateBuilder(state: FieldViewerState) {
    /**
     * trueX var.
     */
    var trueX: Double = state.trueX
    /**
     * trueY var.
     */
    var trueY: Double = state.trueY
    /**
     * trueHeading var.
     */
    var trueHeading: Double = state.trueHeading
    /**
     * simHeading var.
     */
    var simHeading: Double? = state.simHeading
    /**
     * ekfX var.
     */
    var ekfX: Double? = state.ekfX
    /**
     * ekfY var.
     */
    var ekfY: Double? = state.ekfY
    /**
     * ekfHeading var.
     */
    var ekfHeading: Double? = state.ekfHeading
    /**
     * odomX var.
     */
    var odomX: Double? = state.odomX
    /**
     * odomY var.
     */
    var odomY: Double? = state.odomY
    /**
     * odomHeading var.
     */
    var odomHeading: Double? = state.odomHeading
    /**
     * visionX var.
     */
    var visionX: Double? = state.visionX
    /**
     * visionY var.
     */
    var visionY: Double? = state.visionY
    /**
     * visionHeading var.
     */
    var visionHeading: Double? = state.visionHeading
    /**
     * visionPoses var.
     */
    var visionPoses: MutableMap<Int, Double> = state.visionPoses.toMutableMap()
    /**
     * visionHasTarget var.
     */
    var visionHasTarget: Boolean = state.visionHasTarget
    /**
     * liveGamePieces var.
     */
    var liveGamePieces: MutableMap<Int, GamePiece> = state.liveGamePieces.toMutableMap()
    /**
     * isRedAlliance var.
     */
    var isRedAlliance: Boolean = state.isRedAlliance
    /**
     * indicatorLights var.
     */
    var indicatorLights: MutableMap<String, Double> = state.indicatorLights.toMutableMap()

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun build(original: FieldViewerState): FieldViewerState {
        return original.copy(
            trueX = trueX,
            trueY = trueY,
            trueHeading = trueHeading,
            simHeading = simHeading,
            ekfX = ekfX,
            ekfY = ekfY,
            ekfHeading = ekfHeading,
            odomX = odomX,
            odomY = odomY,
            odomHeading = odomHeading,
            visionX = visionX,
            visionY = visionY,
            visionHeading = visionHeading,
            visionPoses = visionPoses.toMap(),
            visionHasTarget = visionHasTarget,
            liveGamePieces = liveGamePieces.toMap(),
            isRedAlliance = isRedAlliance,
            indicatorLights = indicatorLights.toMap()
        )
    }
}
