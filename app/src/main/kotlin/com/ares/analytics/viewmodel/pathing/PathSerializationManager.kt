package com.ares.analytics.viewmodel.pathing

import com.ares.analytics.shared.*
import com.ares.analytics.service.TrajectoryEstimator
import com.ares.analytics.ui.components.pathplanner.Waypoint
import com.ares.analytics.ui.components.pathplanner.resolveHeading
import com.ares.analytics.viewmodel.PathPreview
import com.ares.analytics.viewmodel.PathPlannerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class PathSerializationManager(
    private val scope: CoroutineScope,
    private val stateFlow: MutableStateFlow<PathPlannerState>,
    private val onTrajectoryChanged: () -> Unit
) {
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun loadPathTrajectory(pathName: String, projectPath: String, league: League): Trajectory? {
        try {
            /**
             * relativeDir val.
             */
            val relativeDir = if (league == League.FTC) {
                if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/pathplanner/paths"
                else "src/main/assets/pathplanner/paths"
            } else {
                "src/main/deploy/pathplanner/paths"
            }
            /**
             * targetDir val.
             */
            val targetDir = File(projectPath, relativeDir)
            /**
             * file val.
             */
            val file = File(targetDir, "$pathName.path")
            if (!file.exists()) return null
            /**
             * pathFile val.
             */
            val pathFile = AppJson.decodeFromString<PathPlannerFile>(file.readText())
            /**
             * loadedWps val.
             */
            val loadedWps = pathFile.waypoints.map { pwp ->
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
                    next != null -> atan2(next.y - pwp.anchor.y, next.x - pwp.anchor.x)
                    prev != null -> atan2(pwp.anchor.y - prev.y, pwp.anchor.x - prev.x)
                    else -> 0.0
                }
                /**
                 * prevLength val.
                 */
                val prevLength = if (prev != null) hypot(pwp.anchor.x - prev.x, pwp.anchor.y - prev.y) else 0.5
                /**
                 * nextLength val.
                 */
                val nextLength = if (next != null) hypot(next.x - pwp.anchor.x, next.y - pwp.anchor.y) else 0.5
                Waypoint(pwp.anchor.x, pwp.anchor.y, heading, prevControlLength = prevLength, nextControlLength = nextLength)
            }
            return TrajectoryEstimator.generateTrajectory(
                waypoints = loadedWps,
                globalConstraints = pathFile.globalConstraints,
                constraintZones = pathFile.constraintZones,
                rotationTargets = pathFile.rotationTargets,
                idealStartingState = pathFile.idealStartingState,
                goalEndState = pathFile.goalEndState
            )
        } catch (e: Exception) {
            return null
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
    fun loadPathWaypoints(pathName: String, projectPath: String, league: League): List<Waypoint>? {
        try {
            /**
             * relativeDir val.
             */
            val relativeDir = if (league == League.FTC) {
                if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/pathplanner/paths"
                else "src/main/assets/pathplanner/paths"
            } else {
                "src/main/deploy/pathplanner/paths"
            }
            /**
             * targetDir val.
             */
            val targetDir = File(projectPath, relativeDir)
            /**
             * file val.
             */
            val file = File(targetDir, "$pathName.path")
            if (!file.exists()) return null
            /**
             * pathFile val.
             */
            val pathFile = AppJson.decodeFromString<PathPlannerFile>(file.readText())
            return pathFile.waypoints.map { pwp ->
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
                    next != null -> atan2(next.y - pwp.anchor.y, next.x - pwp.anchor.x)
                    prev != null -> atan2(pwp.anchor.y - prev.y, pwp.anchor.x - prev.x)
                    else -> 0.0
                }
                /**
                 * prevLength val.
                 */
                val prevLength = if (prev != null) hypot(pwp.anchor.x - prev.x, pwp.anchor.y - prev.y) else 0.5
                /**
                 * nextLength val.
                 */
                val nextLength = if (next != null) hypot(next.x - pwp.anchor.x, next.y - pwp.anchor.y) else 0.5
                Waypoint(pwp.anchor.x, pwp.anchor.y, heading, prevControlLength = prevLength, nextControlLength = nextLength)
            }
        } catch (e: Exception) {
            return null
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
    fun loadAutoTrajectory(autoName: String, projectPath: String, league: League): Trajectory? {
        try {
            /**
             * relativeDir val.
             */
            val relativeDir = if (league == League.FTC) {
                if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/pathplanner/autos"
                else "src/main/assets/pathplanner/autos"
            } else {
                "src/main/deploy/pathplanner/autos"
            }
            /**
             * targetDir val.
             */
            val targetDir = File(projectPath, relativeDir)
            /**
             * file val.
             */
            val file = File(targetDir, "$autoName.auto")
            if (!file.exists()) return null
            /**
             * autoFile val.
             */
            val autoFile = AppJson.decodeFromString<AutoCommandNode>(file.readText())
            
            /**
             * pathNames val.
             */
            val pathNames = mutableListOf<String>()
            /**
             * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
             * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
             * Canvas-to-field coordinate transformation conventions applied where relevant.
             *
             * @param args relevant arguments
             * @return expected results
             */
            fun extractPaths(node: AutoCommandNode) {
                if (node.type == "path") {
                    /**
                     * pName val.
                     */
                    val pName = node.data["pathName"]?.let { 
                        if (it is JsonPrimitive && it.isString) it.content else null 
                    }
                    if (pName != null && pName.isNotEmpty()) {
                        pathNames.add(pName)
                    }
                }
                /**
                 * commandsArray val.
                 */
                val commandsArray = node.data["commands"] as? JsonArray
                commandsArray?.forEach { jsonElement ->
                    try {
                        /**
                         * childNode val.
                         */
                        val childNode = AppJson.decodeFromJsonElement(AutoCommandNode.serializer(), jsonElement)
                        extractPaths(childNode)
                    } catch (e: Exception) {}
                }
            }
            
            /**
             * rootCommandField val.
             */
            val rootCommandField = (autoFile as? AutoCommandNode)?.data?.get("command")
            if (rootCommandField != null) {
                try {
                    /**
                     * rootCommandNode val.
                     */
                    val rootCommandNode = AppJson.decodeFromJsonElement(AutoCommandNode.serializer(), rootCommandField)
                    extractPaths(rootCommandNode)
                } catch (e: Exception) {}
            } else {
                extractPaths(autoFile)
            }

            /**
             * totalTime var.
             */
            var totalTime = 0.0
            /**
             * combinedStates val.
             */
            val combinedStates = mutableListOf<TrajectoryState>()
            for (pName in pathNames) {
                /**
                 * traj val.
                 */
                val traj = loadPathTrajectory(pName, projectPath, league)
                if (traj != null && traj.states.isNotEmpty()) {
                    for (state in traj.states) {
                        combinedStates.add(state.copy(timeSeconds = state.timeSeconds + totalTime))
                    }
                    totalTime += traj.durationSeconds
                }
            }
            
            return if (combinedStates.isNotEmpty()) Trajectory(totalTime, combinedStates) else null
        } catch (e: Exception) {
            return null
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
    fun recalculateAutoTrajectory(projectPath: String?, league: League) {
        if (projectPath == null) return
        scope.launch(Dispatchers.IO) {
            /**
             * s val.
             */
            val s = stateFlow.value
            /**
             * root val.
             */
            val root = s.currentAutoCommands.firstOrNull() ?: return@launch
            
            /**
             * pathNames val.
             */
            val pathNames = mutableListOf<String>()
            /**
             * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
             * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
             * Canvas-to-field coordinate transformation conventions applied where relevant.
             *
             * @param args relevant arguments
             * @return expected results
             */
            fun extractPaths(node: AutoCommandNode) {
                if (node.type == "path") {
                    /**
                     * pathName val.
                     */
                    val pathName = node.data["pathName"]?.let { 
                        if (it is JsonPrimitive && it.isString) it.content else null 
                    }
                    if (pathName != null && pathName.isNotEmpty()) {
                        pathNames.add(pathName)
                    }
                }
                /**
                 * commandsArray val.
                 */
                val commandsArray = node.data["commands"] as? JsonArray
                commandsArray?.forEach { jsonElement ->
                    try {
                        /**
                         * childNode val.
                         */
                        val childNode = AppJson.decodeFromJsonElement(AutoCommandNode.serializer(), jsonElement)
                        extractPaths(childNode)
                    } catch (e: Exception) {}
                }
            }
            extractPaths(root)

            /**
             * totalTime var.
             */
            var totalTime = 0.0
            /**
             * combinedStates val.
             */
            val combinedStates = mutableListOf<TrajectoryState>()
            for (pathName in pathNames) {
                /**
                 * traj val.
                 */
                val traj = loadPathTrajectory(pathName, projectPath, league)
                if (traj != null && traj.states.isNotEmpty()) {
                    for (state in traj.states) {
                        combinedStates.add(state.copy(timeSeconds = state.timeSeconds + totalTime))
                    }
                    totalTime += traj.durationSeconds
                }
            }
            
            /**
             * autoTrajectory val.
             */
            val autoTrajectory = if (combinedStates.isNotEmpty()) Trajectory(totalTime, combinedStates) else null
            stateFlow.update { it.copy(trajectory = autoTrajectory, estimatedDuration = totalTime) }
        }
    }

    suspend fun savePath(projectPath: String, league: League, updateContextAuto: (String?, String?, League) -> Unit) = withContext(Dispatchers.IO) {
        /**
         * s val.
         */
        val s = stateFlow.value
        try {
            /**
             * pWaypoints val.
             */
            val pWaypoints = s.waypoints.mapIndexed { idx, wp ->
                /**
                 * theta val.
                 */
                val theta = resolveHeading(s.waypoints, idx)
                /**
                 * anchor val.
                 */
                val anchor = PathPoint(wp.x, wp.y)
                /**
                 * nextControl val.
                 */
                val nextControl = if (idx == s.waypoints.size - 1) null else PathPoint(
                    wp.x + cos(theta) * wp.nextControlLength,
                    wp.y + sin(theta) * wp.nextControlLength
                )
                /**
                 * prevControl val.
                 */
                val prevControl = if (idx == 0) null else PathPoint(
                    wp.x - cos(theta) * wp.prevControlLength,
                    wp.y - sin(theta) * wp.prevControlLength
                )
                PathPlannerWaypoint(
                    anchor = anchor,
                    prevControl = prevControl,
                    nextControl = nextControl
                )
            }
            /**
             * firstRot val.
             */
            val firstRot = s.waypoints.firstOrNull()?.rotationDeg
            /**
             * lastRot val.
             */
            val lastRot = s.waypoints.lastOrNull()?.rotationDeg
            /**
             * extractedStartingState val.
             */
            val extractedStartingState = IdealStartingState(
                velocity = s.idealStartingState?.velocity ?: 0.0,
                rotation = firstRot ?: 0.0
            )
            /**
             * extractedGoalEndState val.
             */
            val extractedGoalEndState = GoalEndState(
                velocity = s.goalEndState?.velocity ?: 0.0,
                rotation = lastRot ?: 0.0
            )
            /**
             * waypointRotationTargets val.
             */
            val waypointRotationTargets = s.waypoints
                .mapIndexedNotNull { idx, wp ->
                    /**
                     * rot val.
                     */
                    val rot = wp.rotationDeg ?: return@mapIndexedNotNull null
                    if (idx == 0 || idx == s.waypoints.size - 1) null
                    else RotationTarget(idx.toDouble(), rot)
                }
            /**
             * midSegmentTargets val.
             */
            val midSegmentTargets = s.rotationTargets.filter { rt ->
                /**
                 * pos val.
                 */
                val pos = rt.waypointRelativePos
                kotlin.math.abs(pos - kotlin.math.round(pos)) > 1e-3
            }
            /**
             * extractedRotationTargets val.
             */
            val extractedRotationTargets = waypointRotationTargets + midSegmentTargets

            /**
             * pathFile val.
             */
            val pathFile = PathPlannerFile(
                version = "2025.0",
                waypoints = pWaypoints,
                rotationTargets = extractedRotationTargets,
                constraintZones = s.constraintZones,
                pointTowardsZones = s.pointTowardsZones,
                eventMarkers = s.eventMarkers,
                globalConstraints = s.globalConstraints,
                goalEndState = extractedGoalEndState,
                idealStartingState = extractedStartingState,
                reversed = s.reversed,
                useDefaultConstraints = s.useDefaultConstraints
            )

            /**
             * json val.
             */
            val json = Json { 
                prettyPrint = true
                encodeDefaults = false
            }
            /**
             * serialized val.
             */
            val serialized = json.encodeToString(pathFile)

            /**
             * relativeDir val.
             */
            val relativeDir = if (league == League.FTC) {
                "TeamCode/src/main/assets/pathplanner/paths"
            } else {
                "src/main/deploy/pathplanner/paths"
            }
            /**
             * targetDir val.
             */
            val targetDir = File(projectPath, relativeDir)
            targetDir.mkdirs()
            /**
             * targetFile val.
             */
            val targetFile = File(targetDir, "${s.pathName}.path")
            targetFile.writeText(serialized)

            try {
                /**
                 * packageDir val.
                 */
                val packageDir = if (league == League.FTC) {
                    /**
                     * candidates val.
                     */
                    val candidates = listOf(
                        "TeamCode/src/main/java/org/firstinspires/ftc/teamcode",
                        "TeamCode/src/main/kotlin/org/firstinspires/ftc/teamcode",
                        "src/main/java/org/firstinspires/ftc/teamcode",
                        "src/main/kotlin/org/firstinspires/ftc/teamcode"
                    )
                    candidates.map { File(projectPath, it) }.firstOrNull { it.exists() && it.isDirectory }
                } else {
                    /**
                     * srcKotlin val.
                     */
                    val srcKotlin = File(projectPath, "src/main/kotlin")
                    if (srcKotlin.exists() && srcKotlin.isDirectory) srcKotlin else File(projectPath, "src/main/java")
                }

                if (packageDir != null && packageDir.exists()) {
                    /**
                     * className val.
                     */
                    val className = s.pathName.split("_", "-").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } } + "Auto"
                    /**
                     * companionFile val.
                     */
                    val companionFile = File(packageDir, "$className.kt")
                    /**
                     * packageName val.
                     */
                    val packageName = if (league == League.FTC) {
                        "org.firstinspires.ftc.teamcode"
                    } else {
                        "com.areslib.pathing"
                    }

                    /**
                     * code val.
                     */
                    val code = """
                        package $packageName

                        import com.areslib.pathing.DynamicPathLoader
                        import com.areslib.pathing.HolonomicPathFollower

                        /**
                         * Auto-generated companion code for the path: ${s.pathName}.path
                         * Map event callbacks to trigger subsystem commands.
                         */
                        object $className {
                            /**
                             * pathName val.
                             */
                            val pathName = "${s.pathName}"


                            /**
                             * buildPathFollower fun.
                             */
                            fun buildPathFollower(
                                follower: HolonomicPathFollower,
                                eventMap: Map<String, () -> Unit>
                            ) {
                                /**
                                 * path val.
                                 */
                                val path = DynamicPathLoader.loadPath(pathName)
                                follower.startPath(path)
                                follower.onEventTriggered = { eventName ->
                                    println("[Auto] Path event triggered: ${'$'}eventName")
                                    eventMap[eventName]?.invoke()
                                }
                            }
                        }
                    """.trimIndent()
                    companionFile.writeText(code)
                }
            } catch (e: Exception) {
                System.err.println("WARN: Failed to generate companion auto file: ${e.message}")
            }

            if (league == League.FTC) {
                /**
                 * pushed val.
                 */
                val pushed = pushFileToRobot(targetFile, "/sdcard/FIRST/paths", "${s.pathName}.path")
                if (pushed) {
                    stateFlow.update { it.copy(saveStatus = "Saved locally & pushed to robot!") }
                } else {
                    stateFlow.update { it.copy(saveStatus = "Saved locally (robot push failed/no connection)") }
                }
            } else {
                stateFlow.update { it.copy(saveStatus = "Saved to ${targetFile.name}!") }
            }
            
            if (s.contextAutoName != null) {
                updateContextAuto(s.contextAutoName, projectPath, league)
            }
        } catch (e: Exception) {
            stateFlow.update { it.copy(saveStatus = "Save failed: ${e.message}") }
        }
    }

    private fun findAdbPath(): String {
        try {
            /**
             * proc val.
             */
            val proc = ProcessBuilder("adb", "--version").start()
            proc.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            return "adb"
        } catch (e: Exception) {
        }
        /**
         * androidHome val.
         */
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (!androidHome.isNullOrEmpty()) {
            /**
             * exe val.
             */
            val exe = if (System.getProperty("os.name").contains("win", ignoreCase = true)) {
                File(androidHome, "platform-tools/adb.exe")
            } else {
                File(androidHome, "platform-tools/adb")
            }
            if (exe.exists() && exe.canExecute()) {
                return exe.absolutePath
            }
        }
        /**
         * userHome val.
         */
        val userHome = System.getProperty("user.home")
        /**
         * defaultPaths val.
         */
        val defaultPaths = listOf(
            File(userHome, "AppData/Local/Android/Sdk/platform-tools/adb.exe"),
            File(userHome, "Library/Android/sdk/platform-tools/adb"),
            File("/usr/bin/adb"),
            File("/usr/local/bin/adb")
        )
        for (file in defaultPaths) {
            if (file.exists() && file.canExecute()) {
                return file.absolutePath
            }
        }
        return "adb"
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun pushFileToRobot(localFile: File, remoteDir: String, remoteFileName: String): Boolean {
        try {
            /**
             * adb val.
             */
            val adb = findAdbPath()
            ProcessBuilder(adb, "connect", "192.168.43.1:5555").start().waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            ProcessBuilder(adb, "shell", "mkdir", "-p", remoteDir).start().waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            /**
             * proc val.
             */
            val proc = ProcessBuilder(adb, "push", localFile.absolutePath, "$remoteDir/$remoteFileName").start()
            /**
             * finished val.
             */
            val finished = proc.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            return finished && proc.exitValue() == 0
        } catch (e: Exception) {
            System.err.println("Failed to push file via ADB: ${e.message}")
            return false
        }
    }
}
