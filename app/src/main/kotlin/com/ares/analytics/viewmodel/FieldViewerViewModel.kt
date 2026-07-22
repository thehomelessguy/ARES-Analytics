package com.ares.analytics.viewmodel

import com.ares.analytics.shared.AppJson
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.ui.components.pathplanner.Waypoint
import com.ares.analytics.viewmodel.field.FieldPoseBufferManager
import com.ares.analytics.viewmodel.field.FieldTopicSubscriber
import com.ares.analytics.viewmodel.field.FieldCameraGestureController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.decodeFromString
import java.io.File
import com.ares.analytics.shared.League
import com.ares.analytics.shared.PathPlannerFile
import com.ares.analytics.shared.AutoFile
import com.ares.analytics.shared.AutoCommandNode
import com.ares.analytics.shared.GamePiece
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class FieldViewerState(
    /**
     * trueX val.
     */
    val trueX: Double = 0.0,
    /**
     * trueY val.
     */
    val trueY: Double = 0.0,
    /**
     * trueHeading val.
     */
    val trueHeading: Double = 0.0,
    /**
     * simHeading val.
     */
    val simHeading: Double? = null,
    /**
     * ekfX val.
     */
    val ekfX: Double? = null,
    /**
     * ekfY val.
     */
    val ekfY: Double? = null,
    /**
     * ekfHeading val.
     */
    val ekfHeading: Double? = null,
    /**
     * odomX val.
     */
    val odomX: Double? = null,
    /**
     * odomY val.
     */
    val odomY: Double? = null,
    /**
     * odomHeading val.
     */
    val odomHeading: Double? = null,
    /**
     * visionX val.
     */
    val visionX: Double? = null,
    /**
     * visionY val.
     */
    val visionY: Double? = null,
    /**
     * visionHeading val.
     */
    val visionHeading: Double? = null,
    /**
     * visionPoses val.
     */
    val visionPoses: Map<Int, Double> = emptyMap(),
    /**
     * visionHasTarget val.
     */
    val visionHasTarget: Boolean = false,
    /**
     * poseHistory val.
     */
    val poseHistory: List<Waypoint> = emptyList(),
    /**
     * liveGamePieces val.
     */
    val liveGamePieces: Map<Int, GamePiece> = emptyMap(),
    /**
     * isConnected val.
     */
    val isConnected: Boolean = false,
    /**
     * availablePaths val.
     */
    val availablePaths: List<String> = emptyList(),
    /**
     * selectedPathName val.
     */
    val selectedPathName: String? = null,
    /**
     * selectedPathWaypoints val.
     */
    val selectedPathWaypoints: List<Waypoint> = emptyList(),
    /**
     * isRedAlliance val.
     */
    val isRedAlliance: Boolean = true,
    /**
     * indicatorLights val.
     */
    val indicatorLights: Map<String, Double> = emptyMap()
)

sealed class FieldViewerIntent {
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class FetchAvailablePaths(val projectPath: String?, val league: League) : FieldViewerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SelectPath(val pathName: String?, val projectPath: String?, val league: League) : FieldViewerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object ClearTrace : FieldViewerIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object ToggleAlliance : FieldViewerIntent()
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class FieldViewerViewModel(
    private val nt4ClientService: Nt4ClientService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(FieldViewerState())
    /**
     * state val.
     */
    val state: StateFlow<FieldViewerState> = _state.asStateFlow()

    private val topicSubscriber = FieldTopicSubscriber(nt4ClientService, scope, _state)
    private val poseBufferManager = FieldPoseBufferManager(scope, _state)
    /**
     * cameraGestureController val.
     */
    val cameraGestureController = FieldCameraGestureController()

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun onIntent(intent: FieldViewerIntent) {
        scope.launch {
            when (intent) {
                is FieldViewerIntent.ToggleAlliance -> {
                    _state.update { it.copy(isRedAlliance = !it.isRedAlliance) }
                }
                is FieldViewerIntent.ClearTrace -> {
                    poseBufferManager.clearTrace()
                }
                is FieldViewerIntent.FetchAvailablePaths -> {
                    fetchAvailablePaths(intent.projectPath, intent.league)
                }
                is FieldViewerIntent.SelectPath -> {
                    selectPath(intent.pathName, intent.projectPath, intent.league)
                }
            }
        }
    }

    private suspend fun fetchAvailablePaths(projectPath: String?, league: League) {
        if (projectPath.isNullOrEmpty()) return
        withContext(Dispatchers.IO) {
            try {
                /**
                 * relativePathsDir val.
                 */
                val relativePathsDir = if (league == League.FTC) {
                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/pathplanner/paths"
                    else "src/main/assets/pathplanner/paths"
                } else {
                    "src/main/deploy/pathplanner/paths"
                }
                /**
                 * relativeAutosDir val.
                 */
                val relativeAutosDir = relativePathsDir.replace("/paths", "/autos").replace("\\paths", "\\autos")

                /**
                 * pathsTargetDir val.
                 */
                val pathsTargetDir = File(projectPath, relativePathsDir)
                /**
                 * autosTargetDir val.
                 */
                val autosTargetDir = File(projectPath, relativeAutosDir)

                /**
                 * pathFiles val.
                 */
                val pathFiles = if (pathsTargetDir.exists() && pathsTargetDir.isDirectory) {
                    pathsTargetDir.listFiles { _, name -> name.endsWith(".path") }?.map { "[Path] ${it.nameWithoutExtension}" } ?: emptyList()
                } else emptyList()

                /**
                 * autoFiles val.
                 */
                val autoFiles = if (autosTargetDir.exists() && autosTargetDir.isDirectory) {
                    autosTargetDir.listFiles { _, name -> name.endsWith(".auto") }?.map { "[Auto] ${it.nameWithoutExtension}" } ?: emptyList()
                } else emptyList()

                /**
                 * allAvailable val.
                 */
                val allAvailable = (autoFiles.sorted() + pathFiles.sorted())
                _state.update { it.copy(availablePaths = allAvailable) }
            } catch (e: Exception) {
                // Ignore failure to fetch paths
            }
        }
    }

    private suspend fun selectPath(pathName: String?, projectPath: String?, league: League) {
        if (pathName == null) {
            _state.update { it.copy(selectedPathName = null, selectedPathWaypoints = emptyList()) }
            return
        }
        if (projectPath.isNullOrEmpty()) return
        withContext(Dispatchers.IO) {
            try {
                /**
                 * relativePathsDir val.
                 */
                val relativePathsDir = if (league == League.FTC) {
                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/pathplanner/paths"
                    else "src/main/assets/pathplanner/paths"
                } else {
                    "src/main/deploy/pathplanner/paths"
                }
                /**
                 * relativeAutosDir val.
                 */
                val relativeAutosDir = relativePathsDir.replace("/paths", "/autos").replace("\\paths", "\\autos")

                /**
                 * pathsTargetDir val.
                 */
                val pathsTargetDir = File(projectPath, relativePathsDir)
                /**
                 * autosTargetDir val.
                 */
                val autosTargetDir = File(projectPath, relativeAutosDir)

                /**
                 * json val.
                 */
                val json = AppJson
                /**
                 * loadedWps val.
                 */
                val loadedWps = mutableListOf<Waypoint>()

                if (pathName.startsWith("[Auto] ")) {
                    /**
                     * autoName val.
                     */
                    val autoName = pathName.substringAfter("[Auto] ")
                    /**
                     * autoFile val.
                     */
                    val autoFile = File(autosTargetDir, "$autoName.auto")
                    if (autoFile.exists()) {
                        /**
                         * content val.
                         */
                        val content = autoFile.readText()
                        /**
                         * auto val.
                         */
                        val auto = json.decodeFromString<AutoFile>(content)

                        /**
                         * extractedPaths val.
                         */
                        val extractedPaths = mutableListOf<String>()
                        /**
                         * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
                         * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
                         * Canvas-to-field coordinate transformation conventions applied where relevant.
                         *
                         * @param args relevant arguments
                         * @return expected results
                         */
                        fun collectPaths(node: AutoCommandNode) {
                            if (node.type == "path") {
                                node.data["pathName"]?.let {
                                    extractedPaths.add(it.toString().removeSurrounding("\""))
                                }
                            }
                            node.data["commands"]?.let { commandsElement ->
                                if (commandsElement is kotlinx.serialization.json.JsonArray) {
                                    for (element in commandsElement) {
                                        if (element is kotlinx.serialization.json.JsonObject) {
                                            try {
                                                /**
                                                 * subNode val.
                                                 */
                                                val subNode = json.decodeFromJsonElement<AutoCommandNode>(element)
                                                collectPaths(subNode)
                                            } catch (e: Exception) {}
                                        }
                                    }
                                }
                            }
                        }
                        collectPaths(auto.command)

                        for (pName in extractedPaths) {
                            /**
                             * pFile val.
                             */
                            val pFile = File(pathsTargetDir, "$pName.path")
                            if (pFile.exists()) {
                                /**
                                 * pContent val.
                                 */
                                val pContent = pFile.readText()
                                /**
                                 * pathFile val.
                                 */
                                val pathFile = json.decodeFromString<PathPlannerFile>(pContent)
                                /**
                                 * pathWps val.
                                 */
                                val pathWps = pathFile.waypoints.map { pwp ->
                                    /**
                                     * next val.
                                     */
                                    val next = pwp.nextControl
                                    /**
                                     * prev val.
                                     */
                                    val prev = pwp.prevControl
                                    /**
                                     * heading val.
                                     */
                                    val heading = when {
                                        next != null -> kotlin.math.atan2(next.y - pwp.anchor.y, next.x - pwp.anchor.x)
                                        prev != null -> kotlin.math.atan2(pwp.anchor.y - prev.y, pwp.anchor.x - prev.x)
                                        else -> 0.0
                                    }
                                    Waypoint(pwp.anchor.x, pwp.anchor.y, heading)
                                }
                                loadedWps.addAll(pathWps)
                            }
                        }
                    }
                } else {
                    /**
                     * cleanPathName val.
                     */
                    val cleanPathName = if (pathName.startsWith("[Path] ")) pathName.substringAfter("[Path] ") else pathName
                    /**
                     * file val.
                     */
                    val file = File(pathsTargetDir, "$cleanPathName.path")
                    if (file.exists()) {
                        /**
                         * content val.
                         */
                        val content = file.readText()
                        /**
                         * pathFile val.
                         */
                        val pathFile = json.decodeFromString<PathPlannerFile>(content)
                        /**
                         * pathWps val.
                         */
                        val pathWps = pathFile.waypoints.map { pwp ->
                            /**
                             * next val.
                             */
                            val next = pwp.nextControl
                            /**
                             * prev val.
                             */
                            val prev = pwp.prevControl
                            /**
                             * heading val.
                             */
                            val heading = when {
                                next != null -> kotlin.math.atan2(next.y - pwp.anchor.y, next.x - pwp.anchor.x)
                                prev != null -> kotlin.math.atan2(pwp.anchor.y - prev.y, pwp.anchor.x - prev.x)
                                else -> 0.0
                            }
                            Waypoint(pwp.anchor.x, pwp.anchor.y, heading)
                        }
                        loadedWps.addAll(pathWps)
                    }
                }

                _state.update { it.copy(selectedPathName = pathName, selectedPathWaypoints = loadedWps) }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
