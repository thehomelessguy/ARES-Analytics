package com.ares.analytics.ui.components.pathplanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.ares.analytics.shared.*
import com.ares.analytics.ui.theme.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private val jsonFormatter: Json = Json { prettyPrint = true }

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun FieldCanvas(
    league: League,
    waypoints: List<Waypoint>,
    actualPath: List<Waypoint>,
    contextPath: List<Waypoint>? = null,
    contextWaypoints: List<Waypoint>? = null,
    onWaypointsChanged: (List<Waypoint>) -> Unit,
    projectPath: String? = null,
    showPathControls: Boolean = true,
    showObstacleControls: Boolean = true,
    fieldImage: ImageBitmap? = null,
    fieldImageConfig: FieldImageConfig? = null,
    obstacles: List<Obstacle>? = null,
    onObstaclesChanged: ((List<Obstacle>) -> Unit)? = null,
    gamePieces: List<GamePiece>? = null,
    onGamePiecesChanged: ((List<GamePiece>) -> Unit)? = null,
    aprilTags: List<AprilTagPlacement>? = null,
    onAprilTagsChanged: ((List<AprilTagPlacement>) -> Unit)? = null,
    fieldWaypoints: List<FieldWaypoint>? = null,
    onFieldWaypointsChanged: ((List<FieldWaypoint>) -> Unit)? = null,
    eventMarkers: List<PathPlannerEventMarker> = emptyList(),
    onEventMarkersChanged: ((List<PathPlannerEventMarker>) -> Unit)? = null,
    rotationTargets: List<RotationTarget> = emptyList(),
    onRotationTargetsChanged: ((List<RotationTarget>) -> Unit)? = null,
    idealStartingState: IdealStartingState? = null,
    onStartingStateChanged: ((IdealStartingState) -> Unit)? = null,
    goalEndState: GoalEndState? = null,
    onGoalEndStateChanged: ((GoalEndState) -> Unit)? = null,
    constraintZones: List<ConstraintsZone> = emptyList(),
    pointTowardsZones: List<PointTowardsZone> = emptyList(),
    globalConstraints: PathConstraints = PathConstraints(),
    estimatedPose: Waypoint? = null,
    playbackPose: Waypoint? = null,
    visionPoses: List<Waypoint> = emptyList(),
    odomPose: Waypoint? = null,
    showTruePose: Boolean = true,
    showEkfPose: Boolean = true,
    showOdomPose: Boolean = true,
    showVisionPoses: Boolean = true,
    onItemSelected: ((String?, String?) -> Unit)? = null,
    onItemDoubleTapped: ((String, String) -> Unit)? = null,
    initialViewRotation: Float = 0f,
    onViewRotationChanged: ((Float) -> Unit)? = null,
    showToolbar: Boolean = true,
    indicatorLightPosition: Double = -1.0,
    modifier: Modifier = Modifier
) {
    /**
     * localFieldImage var.
     */
    var localFieldImage by remember { mutableStateOf<ImageBitmap?>(null) }
    /**
     * localFieldImageConfig var.
     */
    var localFieldImageConfig by remember { mutableStateOf(FieldImageConfig()) }
    /**
     * isDraggingHeading var.
     */
    var isDraggingHeading by remember { mutableStateOf(false) }
    /**
     * isDraggingPrevHeading var.
     */
    var isDraggingPrevHeading by remember { mutableStateOf(false) }
    /**
     * isDraggingRotation var.
     */
    var isDraggingRotation by remember { mutableStateOf(false) }
    /**
     * isDraggingFieldWaypoint var.
     */
    var isDraggingFieldWaypoint by remember { mutableStateOf(false) }
    /**
     * isDraggingFieldWaypointHeading var.
     */
    var isDraggingFieldWaypointHeading by remember { mutableStateOf(false) }
    /**
     * textMeasurer val.
     */
    val textMeasurer = rememberTextMeasurer()
    /**
     * pathCache val.
     */
    val pathCache = remember { PathCacheHolder() }

    /**
     * contextMenuExpanded var.
     */
    var contextMenuExpanded by remember { mutableStateOf(false) }
    /**
     * contextMenuOffset var.
     */
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    /**
     * contextTargetType var.
     */
    var contextTargetType by remember { mutableStateOf<String?>(null) }
    /**
     * contextTargetId var.
     */
    var contextTargetId by remember { mutableStateOf<String?>(null) }
    /**
     * contextTargetIndex var.
     */
    var contextTargetIndex by remember { mutableStateOf<Int>(-1) }

    /**
     * density val.
     */
    val density = LocalDensity.current
    /**
     * splinePoints val.
     */
    val splinePoints = remember(waypoints) {
        if (waypoints.size < 2) emptyList<Waypoint>() else {
            /**
             * list val.
             */
            val list = ArrayList<Waypoint>((waypoints.size - 1) * 30 + 1)
            /**
             * density val.
             */
            val density = 30
            for (i in 0 until waypoints.size - 1) {
                /**
                 * p0 val.
                 */
                val p0 = waypoints[i]
                /**
                 * p1 val.
                 */
                val p1 = waypoints[i + 1]
                /**
                 * h0 val.
                 */
                val h0 = resolveHeading(waypoints, i)
                /**
                 * h1 val.
                 */
                val h1 = resolveHeading(waypoints, i + 1)
                /**
                 * v0x val.
                 */
                val v0x = cos(h0) * p0.nextControlLength
                /**
                 * v0y val.
                 */
                val v0y = sin(h0) * p0.nextControlLength
                /**
                 * v1x val.
                 */
                val v1x = cos(h1) * p1.prevControlLength
                /**
                 * v1y val.
                 */
                val v1y = sin(h1) * p1.prevControlLength

                for (j in 1..density) {
                    /**
                     * t val.
                     */
                    val t = j.toDouble() / density
                    list.add(Waypoint(cubicHermite(p0.x, v0x, p1.x, v1x, t), cubicHermite(p0.y, v0y, p1.y, v1y, t)))
                }
            }
            list
        }
    }

    /**
     * eventMarkerPoints val.
     */
    val eventMarkerPoints = remember(eventMarkers, waypoints) {
        if (waypoints.size < 2) emptyList()
        else eventMarkers.map { getPositionOnSpline(it.waypointRelativePos, waypoints) }
    }

    /**
     * combinedRotationTargets val.
     */
    val combinedRotationTargets = remember(rotationTargets, idealStartingState, goalEndState, waypoints) {
        /**
         * list val.
         */
        val list = mutableListOf<RotationTarget>()
        /**
         * startingRot val.
         */
        val startingRot = idealStartingState?.rotation ?: 0.0
        if (rotationTargets.none { kotlin.math.abs(it.waypointRelativePos) < 1e-3 }) {
            list.add(RotationTarget(waypointRelativePos = 0.0, rotationDegrees = startingRot))
        }
        list.addAll(rotationTargets)
        /**
         * endRot val.
         */
        val endRot = goalEndState?.rotation ?: 0.0
        /**
         * lastWaypointIdx val.
         */
        val lastWaypointIdx = (waypoints.size - 1).toDouble()
        if (lastWaypointIdx >= 0.0 && rotationTargets.none { kotlin.math.abs(it.waypointRelativePos - lastWaypointIdx) < 1e-3 }) {
            list.add(RotationTarget(waypointRelativePos = lastWaypointIdx, rotationDegrees = endRot))
        }
        list
    }

    /**
     * combinedRotationTargetPoints val.
     */
    val combinedRotationTargetPoints = remember(combinedRotationTargets, waypoints) {
        if (waypoints.size < 2) emptyList()
        else combinedRotationTargets.map { getPositionOnSpline(it.waypointRelativePos, waypoints) }
    }

    /**
     * constraintZoneSplines val.
     */
    val constraintZoneSplines = remember(constraintZones, waypoints) {
        if (waypoints.size < 2) emptyList()
        else constraintZones.map { zone ->
            /**
             * minPos val.
             */
            val minPos = zone.minWaypointRelativePos
            /**
             * maxPos val.
             */
            val maxPos = zone.maxWaypointRelativePos
            /**
             * steps val.
             */
            val steps = ((maxPos - minPos) * 20).toInt().coerceIn(2, 100)
            /**
             * pathPoints val.
             */
            val pathPoints = ArrayList<Waypoint>(steps + 1)
            for (step in 0..steps) {
                pathPoints.add(getPositionOnSpline(minPos + (maxPos - minPos) * (step.toDouble() / steps), waypoints))
            }
            pathPoints
        }
    }

    /**
     * pointTowardsZoneRenderData val.
     */
    val pointTowardsZoneRenderData = remember(pointTowardsZones, waypoints) {
        if (waypoints.size < 2) emptyList()
        else pointTowardsZones.map { zone ->
            /**
             * numLines val.
             */
            val numLines = 5
            /**
             * spPoints val.
             */
            val spPoints = ArrayList<Waypoint>(numLines)
            for (k in 0 until numLines) {
                spPoints.add(getPositionOnSpline(zone.minWaypointRelativePos + (zone.maxWaypointRelativePos - zone.minWaypointRelativePos) * (if (numLines > 1) k.toDouble() / (numLines - 1) else 0.5), waypoints))
            }
            PointTowardsZoneRenderData(Waypoint(zone.fieldPosition.x, zone.fieldPosition.y), spPoints)
        }
    }

    /**
     * activeImage val.
     */
    val activeImage = fieldImage ?: localFieldImage
    /**
     * activeConfig val.
     */
    val activeConfig = fieldImageConfig ?: localFieldImageConfig
    /**
     * fieldWidthM val.
     */
    val fieldWidthM = if (activeConfig.widthMeters > 0.0) activeConfig.widthMeters else (if (league == League.FTC) 3.65 else 16.5)
    /**
     * fieldHeightM val.
     */
    val fieldHeightM = if (activeConfig.heightMeters > 0.0) activeConfig.heightMeters else (if (league == League.FTC) 3.65 else 8.2)

    /**
     * editorMode var.
     */
    var editorMode by remember { mutableStateOf(EditorMode.SELECT) }
    /**
     * selectedWaypointIndex var.
     */
    var selectedWaypointIndex by remember { mutableStateOf(-1) }
    /**
     * selectedEventMarkerIndex var.
     */
    var selectedEventMarkerIndex by remember { mutableStateOf(-1) }
    /**
     * selectedObstacleId var.
     */
    var selectedObstacleId by remember { mutableStateOf<String?>(null) }
    /**
     * selectedAprilTagId var.
     */
    var selectedAprilTagId by remember { mutableStateOf<String?>(null) }
    /**
     * selectedGamePieceId var.
     */
    var selectedGamePieceId by remember { mutableStateOf<String?>(null) }
    /**
     * selectedFieldWaypointId var.
     */
    var selectedFieldWaypointId by remember { mutableStateOf<String?>(null) }
    /**
     * activeGamePieceType var.
     */
    var activeGamePieceType by remember { mutableStateOf(if (league == League.FTC) "Sample (Yellow)" else "Note") }
    
    /**
     * zoomScale var.
     */
    var zoomScale by remember { mutableStateOf(1f) }
    /**
     * panOffset var.
     */
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    /**
     * showHeatmap var.
     */
    var showHeatmap by remember { mutableStateOf(false) }
    /**
     * showCostmap var.
     */
    var showCostmap by remember { mutableStateOf(false) }
    /**
     * windowInfo val.
     */
    val windowInfo = LocalWindowInfo.current
    /**
     * isShiftPressed val.
     */
    val isShiftPressed = windowInfo.keyboardModifiers.isShiftPressed
    /**
     * viewRotation var.
     */
    var viewRotation by remember(initialViewRotation) { mutableStateOf(initialViewRotation) }
    
    /**
     * localObstacles val.
     */
    val localObstacles = remember { mutableStateListOf<Obstacle>() }
    /**
     * localGamePieces val.
     */
    val localGamePieces = remember { mutableStateListOf<GamePiece>() }
    /**
     * localAprilTags val.
     */
    val localAprilTags = remember { mutableStateListOf<AprilTagPlacement>() }
    /**
     * localFieldWaypoints val.
     */
    val localFieldWaypoints = remember { mutableStateListOf<FieldWaypoint>() }

    /**
     * activeObstacles val.
     */
    val activeObstacles = obstacles ?: localObstacles
    /**
     * activeGamePieces val.
     */
    val activeGamePieces = gamePieces ?: localGamePieces
    /**
     * activeAprilTags val.
     */
    val activeAprilTags = aprilTags ?: localAprilTags
    /**
     * activeFieldWaypoints val.
     */
    val activeFieldWaypoints = fieldWaypoints ?: localFieldWaypoints

    /**
     * updateObstacles val.
     */
    val updateObstacles: (List<Obstacle>) -> Unit = { newObstacles ->
        if (onObstaclesChanged != null) onObstaclesChanged(newObstacles)
        else { localObstacles.clear(); localObstacles.addAll(newObstacles) }
        if (!projectPath.isNullOrEmpty()) {
            try {
                /**
                 * targetDir val.
                 */
                val targetDir = File(projectPath, if (league == League.FTC) {
                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/paths" else "src/main/assets/paths"
                } else "src/main/deploy/paths")
                targetDir.mkdirs()
                File(targetDir, "obstacles.json").writeText(jsonFormatter.encodeToString(newObstacles))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    /**
     * updateGamePieces val.
     */
    val updateGamePieces: (List<GamePiece>) -> Unit = { newPieces ->
        if (onGamePiecesChanged != null) onGamePiecesChanged(newPieces)
        else { localGamePieces.clear(); localGamePieces.addAll(newPieces) }
        if (!projectPath.isNullOrEmpty()) {
            try {
                /**
                 * targetDir val.
                 */
                val targetDir = File(projectPath, if (league == League.FTC) {
                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/paths" else "src/main/assets/paths"
                } else "src/main/deploy/paths")
                targetDir.mkdirs()
                File(targetDir, "game_pieces.json").writeText(jsonFormatter.encodeToString(newPieces))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    
    /**
     * updateAprilTags val.
     */
    val updateAprilTags: (List<AprilTagPlacement>) -> Unit = { newTags ->
        if (onAprilTagsChanged != null) onAprilTagsChanged(newTags)
        else { localAprilTags.clear(); localAprilTags.addAll(newTags) }
        if (!projectPath.isNullOrEmpty()) {
            try {
                /**
                 * targetDir val.
                 */
                val targetDir = File(projectPath, if (league == League.FTC) {
                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/paths" else "src/main/assets/paths"
                } else "src/main/deploy/paths")
                targetDir.mkdirs()
                File(targetDir, "apriltags.json").writeText(jsonFormatter.encodeToString(newTags))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    /**
     * updateFieldWaypoints val.
     */
    val updateFieldWaypoints: (List<FieldWaypoint>) -> Unit = { newWps ->
        if (onFieldWaypointsChanged != null) onFieldWaypointsChanged(newWps)
        else { localFieldWaypoints.clear(); localFieldWaypoints.addAll(newWps) }
        if (!projectPath.isNullOrEmpty()) {
            try {
                /**
                 * targetDir val.
                 */
                val targetDir = File(projectPath, if (league == League.FTC) {
                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/paths" else "src/main/assets/paths"
                } else "src/main/deploy/paths")
                targetDir.mkdirs()
                File(targetDir, "field_waypoints.json").writeText(jsonFormatter.encodeToString(newWps))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    /**
     * currentPolygonPoints val.
     */
    val currentPolygonPoints = remember { mutableStateListOf<PathPoint>() }
    /**
     * currentWaypoints val.
     */
    val currentWaypoints by rememberUpdatedState(waypoints)
    /**
     * currentActiveObstacles val.
     */
    val currentActiveObstacles by rememberUpdatedState(activeObstacles)
    /**
     * currentActiveGamePieces val.
     */
    val currentActiveGamePieces by rememberUpdatedState(activeGamePieces)
    /**
     * currentActiveAprilTags val.
     */
    val currentActiveAprilTags by rememberUpdatedState(activeAprilTags)
    /**
     * currentActiveFieldWaypoints val.
     */
    val currentActiveFieldWaypoints by rememberUpdatedState(activeFieldWaypoints)
    /**
     * currentEventMarkers val.
     */
    val currentEventMarkers by rememberUpdatedState(eventMarkers)
    /**
     * currentRotationTargets val.
     */
    val currentRotationTargets by rememberUpdatedState(rotationTargets)
    /**
     * currentCombinedRotationTargets val.
     */
    val currentCombinedRotationTargets by rememberUpdatedState(combinedRotationTargets)
    /**
     * currentIdealStartingState val.
     */
    val currentIdealStartingState by rememberUpdatedState(idealStartingState)
    /**
     * currentGoalEndState val.
     */
    val currentGoalEndState by rememberUpdatedState(goalEndState)

    LaunchedEffect(projectPath) {
        if (!projectPath.isNullOrEmpty()) {
            try {
                /**
                 * relDir val.
                 */
                val relDir = if (league == League.FTC) {
                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/paths" else "src/main/assets/paths"
                } else "src/main/deploy/paths"
                /**
                 * fObs val.
                 */
                val fObs = File(File(projectPath, relDir), "obstacles.json")
                if (fObs.exists()) updateObstacles(Json.decodeFromString(fObs.readText()))
                /**
                 * fGp val.
                 */
                val fGp = File(File(projectPath, relDir), "game_pieces.json")
                if (fGp.exists()) updateGamePieces(Json.decodeFromString(fGp.readText()))
                /**
                 * fAt val.
                 */
                val fAt = File(File(projectPath, relDir), "apriltags.json")
                if (fAt.exists()) updateAprilTags(Json.decodeFromString(fAt.readText()))
                /**
                 * fWp val.
                 */
                val fWp = File(File(projectPath, relDir), "field_waypoints.json")
                if (fWp.exists()) updateFieldWaypoints(Json.decodeFromString(fWp.readText()))
                
                /**
                 * imgDir val.
                 */
                val imgDir = if (league == League.FTC) {
                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets" else "src/main/assets"
                } else "src/main/deploy"
                /**
                 * imgFile val.
                 */
                val imgFile = File(File(projectPath, imgDir), "field_image.png")
                localFieldImage = if (imgFile.exists()) org.jetbrains.skia.Image.makeFromEncoded(imgFile.readBytes()).toComposeImageBitmap() else null
                
                /**
                 * confFile val.
                 */
                val confFile = File(File(projectPath, imgDir), "field_image_config.json")
                localFieldImageConfig = if (confFile.exists()) Json.decodeFromString(confFile.readText()) else FieldImageConfig()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (showToolbar) {
            FieldCanvasToolbar(
                showPathControls = showPathControls,
                showObstacleControls = showObstacleControls,
                league = league,
                editorMode = editorMode,
                onEditorModeChanged = { editorMode = it },
                activeGamePieceType = activeGamePieceType,
                onActiveGamePieceTypeChanged = { activeGamePieceType = it },
                currentPolygonPoints = currentPolygonPoints,
                onCurrentPolygonPointsCleared = { currentPolygonPoints.clear() },
                activeObstacles = activeObstacles,
                updateObstacles = updateObstacles,
                zoomScale = zoomScale,
                onZoomScaleChanged = { zoomScale = it },
                onResetZoomPan = { zoomScale = 1f; panOffset = Offset.Zero },
                showHeatmap = showHeatmap,
                onShowHeatmapChanged = { showHeatmap = it },
                showCostmap = showCostmap,
                onShowCostmapChanged = { showCostmap = it },
                viewRotation = viewRotation,
                onViewRotationChanged = { 
                    viewRotation = it
                    onViewRotationChanged?.invoke(it)
                }
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.aspectRatio(fieldWidthM.toFloat() / fieldHeightM.toFloat())) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AresSurface)
                    .pointerInput(Unit) {
                        // Accumulated pixel-space drag for the entire gesture
                        /**
                         * accumulatedDragPx var.
                         */
                        var accumulatedDragPx = Offset.Zero
                        // Initial positions captured at press for absolute positioning
                        /**
                         * dragInitialPos var.
                         */
                        var dragInitialPos = Waypoint(0.0, 0.0)
                        /**
                         * dragInitialVertices var.
                         */
                        var dragInitialVertices: List<PathPoint> = emptyList()
                        awaitEachGesture {
                            // 1. Capture the TRUE press position (before touch slop)
                            /**
                             * down val.
                             */
                            val down = awaitFirstDown(requireUnconsumed = false)
                            /**
                             * pressOffset val.
                             */
                            val pressOffset = down.position
                            /**
                             * hasDragged var.
                             */
                            var hasDragged = false
                            accumulatedDragPx = Offset.Zero

                            // 2. Perform hit-testing at the ACTUAL press position
                            /**
                             * w val.
                             */
                            val w = size.width.toFloat(); val h = size.height.toFloat()
                            when (editorMode) {
                                 EditorMode.SELECT -> {
                                     /**
                                      * hitIdx var.
                                      */
                                     var hitIdx = -1; var hitHeading = false; var hitPrevHeading = false; var hitRotation = false
                                     
                                     // Convert press position to base canvas space (before zoom/pan/rotate transform).
                                     // This matches the coordinate system used by PathRenderer.drawWaypoints.
                                     /**
                                      * basePress val.
                                      */
                                     val basePress = getBaseCanvasFromScreen(pressOffset, w, h, zoomScale, panOffset, viewRotation)
                                     
                                     // Helper: compute rotation handle position in base canvas space (matches PathRenderer draw code)
                                     /**
                                      * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
                                      * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
                                      * Canvas-to-field coordinate transformation conventions applied where relevant.
                                      *
                                      * @param args relevant arguments
                                      * @return expected results
                                      */
                                     fun rotHandleBase(wpBaseOffset: Offset, wp: Waypoint): Offset {
                                         /**
                                          * rotAngleRad val.
                                          */
                                         val rotAngleRad = Math.toRadians(-(wp.rotationDeg ?: 0.0) - 90.0)
                                         /**
                                          * rotHandleLenPx val.
                                          */
                                         val rotHandleLenPx = 30.dp.toPx()
                                         return Offset(
                                             wpBaseOffset.x + rotHandleLenPx * cos(rotAngleRad).toFloat(),
                                             wpBaseOffset.y + rotHandleLenPx * sin(rotAngleRad).toFloat()
                                         )
                                     }
                                     
                                     // Scale-aware hit threshold: fixed screen-size radius divided by zoom
                                     // so clickability doesn't shrink when zoomed out
                                     /**
                                      * hitRadiusPx val.
                                      */
                                     val hitRadiusPx = 15.dp.toPx() / zoomScale
                                     /**
                                      * rotHitRadiusPx val.
                                      */
                                     val rotHitRadiusPx = 18.dp.toPx() / zoomScale
                                     
                                     // 1. Prioritize handles of the ALREADY selected waypoint (if any)
                                     if (selectedWaypointIndex in currentWaypoints.indices) {
                                         /**
                                          * wp val.
                                          */
                                         val wp = currentWaypoints[selectedWaypointIndex]
                                         /**
                                          * wpBase val.
                                          */
                                         val wpBase = getCanvasOffsetBase(wp, w, h, fieldWidthM, fieldHeightM, league)
                                         
                                         // Heading handle (tangent arrowhead)
                                         /**
                                          * selHeading val.
                                          */
                                         val selHeading = resolveHeading(currentWaypoints, selectedWaypointIndex)
                                         /**
                                          * headingWp val.
                                          */
                                         val headingWp = Waypoint(wp.x + wp.nextControlLength * cos(selHeading), wp.y + wp.nextControlLength * sin(selHeading))
                                         /**
                                          * headingBase val.
                                          */
                                         val headingBase = getCanvasOffsetBase(headingWp, w, h, fieldWidthM, fieldHeightM, league)
                                         if (sqrt((basePress.x - headingBase.x).pow(2) + (basePress.y - headingBase.y).pow(2)) < hitRadiusPx) {
                                             hitIdx = selectedWaypointIndex; hitHeading = true
                                         }
                                         
                                         // Prev Heading handle
                                         /**
                                          * prevHeadingWp val.
                                          */
                                         val prevHeadingWp = Waypoint(wp.x + wp.prevControlLength * cos(selHeading + Math.PI), wp.y + wp.prevControlLength * sin(selHeading + Math.PI))
                                         /**
                                          * prevHeadingBase val.
                                          */
                                         val prevHeadingBase = getCanvasOffsetBase(prevHeadingWp, w, h, fieldWidthM, fieldHeightM, league)
                                         if (hitIdx == -1 && sqrt((basePress.x - prevHeadingBase.x).pow(2) + (basePress.y - prevHeadingBase.y).pow(2)) < hitRadiusPx) {
                                             hitIdx = selectedWaypointIndex; hitPrevHeading = true
                                         }
                                         
                                         // Rotation handle (green diamond)
                                         if (hitIdx == -1) {
                                             /**
                                              * rotBase val.
                                              */
                                             val rotBase = rotHandleBase(wpBase, wp)
                                             if (sqrt((basePress.x - rotBase.x).pow(2) + (basePress.y - rotBase.y).pow(2)) < rotHitRadiusPx) {
                                                 hitIdx = selectedWaypointIndex; hitRotation = true
                                             }
                                         }
                                     }
                                     
                                     // 2. Check all waypoint center dots
                                     if (hitIdx == -1) {
                                         for (i in currentWaypoints.indices) {
                                             /**
                                              * wpBase val.
                                              */
                                             val wpBase = getCanvasOffsetBase(currentWaypoints[i], w, h, fieldWidthM, fieldHeightM, league)
                                             if (sqrt((basePress.x - wpBase.x).pow(2) + (basePress.y - wpBase.y).pow(2)) < hitRadiusPx) {
                                                 hitIdx = i; break
                                             }
                                         }
                                     }
                                     
                                     // 3. Check other waypoints' handles (heading + rotation)
                                     if (hitIdx == -1) {
                                         for (i in currentWaypoints.indices) {
                                             if (i == selectedWaypointIndex) continue
                                             /**
                                              * wp val.
                                              */
                                             val wp = currentWaypoints[i]
                                             /**
                                              * wpBase val.
                                              */
                                             val wpBase = getCanvasOffsetBase(wp, w, h, fieldWidthM, fieldHeightM, league)
                                             
                                             /**
                                              * hd val.
                                              */
                                             val hd = resolveHeading(currentWaypoints, i)
                                             /**
                                              * headingWp val.
                                              */
                                             val headingWp = Waypoint(wp.x + wp.nextControlLength * cos(hd), wp.y + wp.nextControlLength * sin(hd))
                                             /**
                                              * headingBase val.
                                              */
                                             val headingBase = getCanvasOffsetBase(headingWp, w, h, fieldWidthM, fieldHeightM, league)
                                             if (sqrt((basePress.x - headingBase.x).pow(2) + (basePress.y - headingBase.y).pow(2)) < hitRadiusPx) {
                                                 hitIdx = i; hitHeading = true; break
                                             }
                                             
                                             /**
                                              * prevHeadingWp val.
                                              */
                                             val prevHeadingWp = Waypoint(wp.x + wp.prevControlLength * cos(hd + Math.PI), wp.y + wp.prevControlLength * sin(hd + Math.PI))
                                             /**
                                              * prevHeadingBase val.
                                              */
                                             val prevHeadingBase = getCanvasOffsetBase(prevHeadingWp, w, h, fieldWidthM, fieldHeightM, league)
                                             if (sqrt((basePress.x - prevHeadingBase.x).pow(2) + (basePress.y - prevHeadingBase.y).pow(2)) < hitRadiusPx) {
                                                 hitIdx = i; hitPrevHeading = true; break
                                             }
                                             /**
                                              * rotBase val.
                                              */
                                             val rotBase = rotHandleBase(wpBase, wp)
                                             if (sqrt((basePress.x - rotBase.x).pow(2) + (basePress.y - rotBase.y).pow(2)) < rotHitRadiusPx) {
                                                 hitIdx = i; hitRotation = true; break
                                             }
                                         }
                                     }
                                     
                                     /**
                                      * hitEventIdx var.
                                      */
                                     var hitEventIdx = -1
                                     if (hitIdx == -1) {
                                         hitEventIdx = currentEventMarkers.indexOfFirst {
                                             /**
                                              * markerBase val.
                                              */
                                             val markerBase = getCanvasOffsetBase(getPositionOnSpline(it.waypointRelativePos, currentWaypoints), w, h, fieldWidthM, fieldHeightM, league)
                                             sqrt((basePress.x - markerBase.x).pow(2) + (basePress.y - markerBase.y).pow(2)) < hitRadiusPx
                                         }
                                     }
                                    
                                    /**
                                     * hitFieldWpId var.
                                     */
                                    var hitFieldWpId: String? = null
                                    /**
                                     * hitFieldWpHeading var.
                                     */
                                    var hitFieldWpHeading = false
                                    /**
                                     * hitFieldWpCenter var.
                                     */
                                    var hitFieldWpCenter = false
                                    if (hitIdx == -1 && hitEventIdx == -1) {
                                        for (wp in currentActiveFieldWaypoints) {
                                            /**
                                             * wpOffset val.
                                             */
                                            val wpOffset = getTransformedCanvasOffset(Waypoint(wp.x, wp.y), w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset, viewRotation)
                                            /**
                                             * angleRad val.
                                             */
                                            val angleRad = Math.toRadians(-wp.headingDegrees - 90.0)
                                            /**
                                             * pointerLen val.
                                             */
                                            val pointerLen = 22.dp.toPx()
                                            /**
                                             * handleOffset val.
                                             */
                                            val handleOffset = Offset(
                                                (wpOffset.x + pointerLen * cos(angleRad)).toFloat(),
                                                (wpOffset.y + pointerLen * sin(angleRad)).toFloat()
                                            )
                                            if (sqrt((pressOffset.x - handleOffset.x).pow(2) + (pressOffset.y - handleOffset.y).pow(2)) < 15.dp.toPx()) {
                                                hitFieldWpId = wp.id; hitFieldWpHeading = true; break
                                            }
                                            if (sqrt((pressOffset.x - wpOffset.x).pow(2) + (pressOffset.y - wpOffset.y).pow(2)) < 15.dp.toPx()) {
                                                hitFieldWpId = wp.id; hitFieldWpCenter = true; break
                                            }
                                        }
                                    }

                                    selectedWaypointIndex = hitIdx; selectedEventMarkerIndex = hitEventIdx
                                    isDraggingHeading = hitIdx != -1 && hitHeading
                                    isDraggingPrevHeading = hitIdx != -1 && hitPrevHeading
                                    isDraggingRotation = hitIdx != -1 && hitRotation
                                    
                                    selectedFieldWaypointId = hitFieldWpId
                                    isDraggingFieldWaypoint = hitFieldWpId != null && hitFieldWpCenter
                                    isDraggingFieldWaypointHeading = hitFieldWpId != null && hitFieldWpHeading

                                     when {
                                         hitFieldWpId != null -> {
                                             selectedObstacleId = null; selectedAprilTagId = null; selectedGamePieceId = null
                                             onItemSelected?.invoke(hitFieldWpId, "FieldWaypoint")
                                             if (hitFieldWpCenter) {
                                                 /**
                                                  * wp val.
                                                  */
                                                 val wp = currentActiveFieldWaypoints.find { it.id == hitFieldWpId }!!
                                                 dragInitialPos = Waypoint(wp.x, wp.y)
                                             }
                                         }
                                         hitIdx != -1 || hitEventIdx != -1 -> {
                                             selectedObstacleId = null; selectedAprilTagId = null; selectedGamePieceId = null; selectedFieldWaypointId = null
                                         }
                                         else -> {
                                             /**
                                              * clickCoord val.
                                              */
                                             val clickCoord = getRobotCoordFromScreen(pressOffset, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                             /**
                                              * hitObs val.
                                              */
                                             val hitObs = currentActiveObstacles.minByOrNull { obs ->
                                                 when (obs) {
                                                     is Obstacle.Circle -> sqrt((clickCoord.x - obs.centerX).pow(2) + (clickCoord.y - obs.centerY).pow(2)) - obs.radius
                                                     is Obstacle.Rectangle -> {
                                                         /**
                                                          * dx val.
                                                          */
                                                         val dx = clickCoord.x - obs.centerX; val dy = clickCoord.y - obs.centerY
                                                         sqrt(dx * dx + dy * dy)
                                                     }
                                                     is Obstacle.Polygon -> obs.vertices.minOf { sqrt((clickCoord.x - it.x).pow(2) + (clickCoord.y - it.y).pow(2)) }
                                                 }
                                             }?.takeIf { obs ->
                                                 when (obs) {
                                                     is Obstacle.Circle -> sqrt((clickCoord.x - obs.centerX).pow(2) + (clickCoord.y - obs.centerY).pow(2)) <= obs.radius
                                                     is Obstacle.Rectangle -> {
                                                         /**
                                                          * dx val.
                                                          */
                                                         val dx = clickCoord.x - obs.centerX; val dy = clickCoord.y - obs.centerY
                                                         /**
                                                          * rad val.
                                                          */
                                                         val rad = Math.toRadians(-obs.rotation)
                                                         kotlin.math.abs(dx * cos(rad) - dy * sin(rad)) <= obs.width / 2.0 && kotlin.math.abs(dx * sin(rad) + dy * cos(rad)) <= obs.height / 2.0
                                                     }
                                                     is Obstacle.Polygon -> obs.vertices.any { sqrt((clickCoord.x - it.x).pow(2) + (clickCoord.y - it.y).pow(2)) < 0.3 }
                                                 }
                                             }
                                             selectedObstacleId = hitObs?.id
                                             if (selectedObstacleId != null) {
                                                 onItemSelected?.invoke(selectedObstacleId, "Obstacle")
                                                 when (hitObs) {
                                                     is Obstacle.Circle -> dragInitialPos = Waypoint(hitObs.centerX, hitObs.centerY)
                                                     is Obstacle.Rectangle -> dragInitialPos = Waypoint(hitObs.centerX, hitObs.centerY)
                                                     is Obstacle.Polygon -> dragInitialVertices = hitObs.vertices.toList()
                                                     else -> {}
                                                 }
                                             } else {
                                                 /**
                                                  * hitAt val.
                                                  */
                                                 val hitAt = currentActiveAprilTags.minByOrNull { sqrt((clickCoord.x - it.x).pow(2) + (clickCoord.y - it.y).pow(2)) }?.takeIf { sqrt((clickCoord.x - it.x).pow(2) + (clickCoord.y - it.y).pow(2)) < 0.3 }
                                                 selectedAprilTagId = hitAt?.id
                                                 if (selectedAprilTagId != null) {
                                                     onItemSelected?.invoke(selectedAprilTagId, "AprilTag")
                                                     dragInitialPos = Waypoint(hitAt!!.x, hitAt.y)
                                                 } else {
                                                     /**
                                                      * hitGp val.
                                                      */
                                                     val hitGp = currentActiveGamePieces.minByOrNull { sqrt((clickCoord.x - it.x).pow(2) + (clickCoord.y - it.y).pow(2)) }?.takeIf { sqrt((clickCoord.x - it.x).pow(2) + (clickCoord.y - it.y).pow(2)) < 0.2 }
                                                     selectedGamePieceId = hitGp?.id
                                                     if (selectedGamePieceId != null) {
                                                         onItemSelected?.invoke(selectedGamePieceId, "GamePiece")
                                                         dragInitialPos = Waypoint(hitGp!!.x, hitGp.y)
                                                     } else onItemSelected?.invoke(null, null)
                                                 }
                                             }
                                         }
                                     }
                                }
                                else -> { /* Placement handled on release when !hasDragged */ }
                            }

                            // 3. Wait for touch slop then track drag
                            /**
                             * slopChange val.
                             */
                            val slopChange = awaitTouchSlopOrCancellation(down.id) { change, over ->
                                change.consume()
                            }

                            if (slopChange != null) {
                                // Include the slop displacement so drag starts without a gap
                                accumulatedDragPx = slopChange.position - down.position
                                // Drag detected — track movement
                                drag(slopChange.id) { change ->
                                    hasDragged = true
                                    /**
                                     * dragAmount val.
                                     */
                                    val dragAmount = change.positionChange()
                                    accumulatedDragPx += dragAmount
                                    change.consume()
                                    /**
                                     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
                                     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
                                     * Canvas-to-field coordinate transformation conventions applied where relevant.
                                     *
                                     * @param args relevant arguments
                                     * @return expected results
                                     */
                                    fun snap(v: Double) = if (isShiftPressed) kotlin.math.round(v * 10.0) / 10.0 else v
                                    /**
                                     * totalDelta val.
                                     */
                                    val totalDelta = getDragDeltaInFieldCoords(accumulatedDragPx, w, h, fieldWidthM, fieldHeightM, league, zoomScale)
                                     when {
                                         selectedEventMarkerIndex != -1 && onEventMarkersChanged != null -> {
                                             /**
                                              * bestPos val.
                                              */
                                             val bestPos = getClosestSplinePosition(change.position, currentWaypoints, w, h, fieldWidthM, fieldHeightM, league)
                                             onEventMarkersChanged(currentEventMarkers.toMutableList().apply { set(selectedEventMarkerIndex, this[selectedEventMarkerIndex].copy(waypointRelativePos = bestPos)) })
                                         }
                                         selectedWaypointIndex != -1 -> {
                                             when {
                                                 isDraggingHeading -> {
                                                     /**
                                                      * wp val.
                                                      */
                                                     val wp = currentWaypoints[selectedWaypointIndex]
                                                     /**
                                                      * posMeters val.
                                                      */
                                                     val posMeters = getRobotCoordFromScreen(change.position, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                                     /**
                                                      * dx val.
                                                      */
                                                     val dx = posMeters.x - wp.x
                                                     /**
                                                      * dy val.
                                                      */
                                                     val dy = posMeters.y - wp.y
                                                     /**
                                                      * angle val.
                                                      */
                                                     val angle = kotlin.math.atan2(dy, dx)
                                                     /**
                                                      * mag val.
                                                      */
                                                     val mag = kotlin.math.sqrt(dx * dx + dy * dy)
                                                     onWaypointsChanged(currentWaypoints.toMutableList().apply { set(selectedWaypointIndex, wp.copy(headingRad = angle, nextControlLength = if (isShiftPressed) snap(mag) else mag, prevControlLength = if (isShiftPressed) snap(mag) else mag)) })
                                                 }
                                                 isDraggingPrevHeading -> {
                                                     /**
                                                      * wp val.
                                                      */
                                                     val wp = currentWaypoints[selectedWaypointIndex]
                                                     /**
                                                      * posMeters val.
                                                      */
                                                     val posMeters = getRobotCoordFromScreen(change.position, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                                     /**
                                                      * dx val.
                                                      */
                                                     val dx = posMeters.x - wp.x
                                                     /**
                                                      * dy val.
                                                      */
                                                     val dy = posMeters.y - wp.y
                                                     /**
                                                      * angle val.
                                                      */
                                                     val angle = kotlin.math.atan2(dy, dx) - Math.PI
                                                     /**
                                                      * normalizedAngle val.
                                                      */
                                                     val normalizedAngle = ((angle + Math.PI) % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI) - Math.PI
                                                     /**
                                                      * mag val.
                                                      */
                                                     val mag = kotlin.math.sqrt(dx * dx + dy * dy)
                                                     onWaypointsChanged(currentWaypoints.toMutableList().apply { set(selectedWaypointIndex, wp.copy(headingRad = normalizedAngle, nextControlLength = if (isShiftPressed) snap(mag) else mag, prevControlLength = if (isShiftPressed) snap(mag) else mag)) })
                                                 }
                                                 isDraggingRotation -> {
                                                     /**
                                                      * wp val.
                                                      */
                                                     val wp = currentWaypoints[selectedWaypointIndex]
                                                     /**
                                                      * posMeters val.
                                                      */
                                                     val posMeters = getRobotCoordFromScreen(change.position, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                                     /**
                                                      * angle val.
                                                      */
                                                     val angle = kotlin.math.atan2(posMeters.y - wp.y, posMeters.x - wp.x)
                                                     /**
                                                      * degrees val.
                                                      */
                                                     val degrees = Math.toDegrees(angle)
                                                     /**
                                                      * snappedDeg val.
                                                      */
                                                     val snappedDeg = if (isShiftPressed) snap(degrees).toDouble() else degrees
                                                     // Update waypoint's own rotation directly
                                                     onWaypointsChanged(currentWaypoints.toMutableList().apply {
                                                         set(selectedWaypointIndex, wp.copy(rotationDeg = snappedDeg))
                                                     })
                                                 }
                                                 else -> {
                                                     /**
                                                      * newPos val.
                                                      */
                                                     val newPos = getRobotCoordFromScreen(change.position, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                                     /**
                                                      * existingWp val.
                                                      */
                                                     val existingWp = currentWaypoints[selectedWaypointIndex]
                                                     onWaypointsChanged(currentWaypoints.toMutableList().apply { set(selectedWaypointIndex, existingWp.copy(x = snap(newPos.x), y = snap(newPos.y))) })
                                                 }
                                             }
                                         }
                                         selectedObstacleId != null && showObstacleControls -> {
                                             /**
                                              * targetObs val.
                                              */
                                             val targetObs = currentActiveObstacles.find { it.id == selectedObstacleId }
                                             if (targetObs != null && !targetObs.locked) {
                                                 updateObstacles(currentActiveObstacles.map { obs ->
                                                     if (obs.id == selectedObstacleId) {
                                                         when (obs) {
                                                             is Obstacle.Circle -> obs.copy(centerX = snap(dragInitialPos.x + totalDelta.x), centerY = snap(dragInitialPos.y + totalDelta.y))
                                                             is Obstacle.Rectangle -> obs.copy(centerX = snap(dragInitialPos.x + totalDelta.x), centerY = snap(dragInitialPos.y + totalDelta.y))
                                                             is Obstacle.Polygon -> obs.copy(vertices = dragInitialVertices.mapIndexed { idx, v -> PathPoint(snap(v.x + totalDelta.x), snap(v.y + totalDelta.y)) })
                                                         }
                                                     } else obs
                                                 })
                                             }
                                         }
                                         selectedAprilTagId != null && showObstacleControls -> {
                                             /**
                                              * targetAt val.
                                              */
                                             val targetAt = currentActiveAprilTags.find { it.id == selectedAprilTagId }
                                             if (targetAt != null && !targetAt.locked) {
                                                 updateAprilTags(currentActiveAprilTags.map { at -> if (at.id == selectedAprilTagId) at.copy(x = snap(dragInitialPos.x + totalDelta.x), y = snap(dragInitialPos.y + totalDelta.y)) else at })
                                             }
                                         }
                                         selectedGamePieceId != null && showObstacleControls -> {
                                             /**
                                              * targetGp val.
                                              */
                                             val targetGp = currentActiveGamePieces.find { it.id == selectedGamePieceId }
                                             if (targetGp != null && !targetGp.locked) {
                                                 updateGamePieces(currentActiveGamePieces.map { gp -> if (gp.id == selectedGamePieceId) gp.copy(x = snap(dragInitialPos.x + totalDelta.x), y = snap(dragInitialPos.y + totalDelta.y)) else gp })
                                             }
                                         }
                                         selectedFieldWaypointId != null && showObstacleControls -> {
                                             /**
                                              * targetWp val.
                                              */
                                             val targetWp = currentActiveFieldWaypoints.find { it.id == selectedFieldWaypointId }
                                             if (targetWp != null && !targetWp.locked) {
                                                 when {
                                                     isDraggingFieldWaypointHeading -> {
                                                         /**
                                                          * posMeters val.
                                                          */
                                                         val posMeters = getRobotCoordFromScreen(change.position, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                                         /**
                                                          * angle val.
                                                          */
                                                         val angle = kotlin.math.atan2(posMeters.y - targetWp.y, posMeters.x - targetWp.x)
                                                         /**
                                                          * degrees val.
                                                          */
                                                         val degrees = Math.toDegrees(angle)
                                                         /**
                                                          * targetHeading val.
                                                          */
                                                         val targetHeading = -degrees - 90.0
                                                         /**
                                                          * normalizedHeading val.
                                                          */
                                                         val normalizedHeading = ((targetHeading + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
                                                         updateFieldWaypoints(currentActiveFieldWaypoints.map { wp ->
                                                             if (wp.id == selectedFieldWaypointId) wp.copy(headingDegrees = if (isShiftPressed) snap(normalizedHeading) else normalizedHeading) else wp
                                                         })
                                                     }
                                                     isDraggingFieldWaypoint -> {
                                                         updateFieldWaypoints(currentActiveFieldWaypoints.map { wp ->
                                                             if (wp.id == selectedFieldWaypointId) wp.copy(x = snap(dragInitialPos.x + totalDelta.x), y = snap(dragInitialPos.y + totalDelta.y)) else wp
                                                         })
                                                     }
                                                 }
                                             }
                                         }
                                     }
                                }
                            }

                            // 4. Gesture ended (pointer up or drag ended) — handle click-to-place
                            if (!hasDragged) {
                                when (editorMode) {
                                    EditorMode.ADD_WAYPOINT -> {
                                        onWaypointsChanged(currentWaypoints + getRobotCoordFromScreen(pressOffset, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset))
                                    }
                                    EditorMode.DRAW_POLYGON -> {
                                        /**
                                         * newWp val.
                                         */
                                        val newWp = getRobotCoordFromScreen(pressOffset, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                        currentPolygonPoints.add(PathPoint(newWp.x, newWp.y))
                                    }
                                    EditorMode.DRAW_CIRCLE -> {
                                        /**
                                         * newWp val.
                                         */
                                        val newWp = getRobotCoordFromScreen(pressOffset, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                        updateObstacles(currentActiveObstacles + Obstacle.Circle("circle_${System.currentTimeMillis()}", "Circle Obstacle ${currentActiveObstacles.size + 1}", newWp.x, newWp.y, 0.25))
                                    }
                                    EditorMode.DRAW_RECTANGLE -> {
                                        /**
                                         * newWp val.
                                         */
                                        val newWp = getRobotCoordFromScreen(pressOffset, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                        updateObstacles(currentActiveObstacles + Obstacle.Rectangle("rect_${System.currentTimeMillis()}", "Rectangle Obstacle ${currentActiveObstacles.size + 1}", newWp.x, newWp.y, 0.5, 0.5, 0.0))
                                    }
                                    EditorMode.PLACE_GAME_PIECE -> {
                                        /**
                                         * newWp val.
                                         */
                                        val newWp = getRobotCoordFromScreen(pressOffset, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                        /**
                                         * typeLabel val.
                                         */
                                        val typeLabel = if (league == League.FTC) when (activeGamePieceType) { "Sample (Yellow)" -> "Yellow Sample"; "Sample (Red)" -> "Red Sample"; "Sample (Blue)" -> "Blue Sample"; "Decode (Ball)" -> "Decode Ball"; else -> "Specimen" } else "Note"
                                        updateGamePieces(currentActiveGamePieces + GamePiece("piece_${System.currentTimeMillis()}", "$typeLabel ${currentActiveGamePieces.size + 1}", newWp.x, newWp.y, activeGamePieceType))
                                    }
                                    EditorMode.PLACE_APRILTAG -> {
                                        /**
                                         * newWp val.
                                         */
                                        val newWp = getRobotCoordFromScreen(pressOffset, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                        updateAprilTags(currentActiveAprilTags + AprilTagPlacement("apriltag_${System.currentTimeMillis()}", 11 + currentActiveAprilTags.size, newWp.x, newWp.y, 0.5, 0.0))
                                    }
                                    EditorMode.PLACE_FIELD_WAYPOINT -> {
                                        /**
                                         * newWp val.
                                         */
                                        val newWp = getRobotCoordFromScreen(pressOffset, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                        updateFieldWaypoints(currentActiveFieldWaypoints + FieldWaypoint("fieldwp_${System.currentTimeMillis()}", "Waypoint ${currentActiveFieldWaypoints.size + 1}", newWp.x, newWp.y, 0.0))
                                    }
                                    EditorMode.ERASER -> {
                                        /**
                                         * hitIdx val.
                                         */
                                        val hitIdx = currentWaypoints.indexOfFirst { sqrt((pressOffset.x - getTransformedCanvasOffset(it, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset, viewRotation).x).pow(2) + (pressOffset.y - getTransformedCanvasOffset(it, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset, viewRotation).y).pow(2)) < 25f }
                                        if (hitIdx != -1) onWaypointsChanged(currentWaypoints.toMutableList().apply { removeAt(hitIdx) })
                                        else {
                                            /**
                                             * robotWp val.
                                             */
                                            val robotWp = getRobotCoordFromScreen(pressOffset, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                            /**
                                             * hitGp val.
                                             */
                                            val hitGp = currentActiveGamePieces.find { sqrt((robotWp.x - it.x).pow(2) + (robotWp.y - it.y).pow(2)) < 0.3 }
                                            if (hitGp != null) updateGamePieces(currentActiveGamePieces - hitGp)
                                            else {
                                                /**
                                                 * hitObs val.
                                                 */
                                                val hitObs = currentActiveObstacles.find { obs ->
                                                    when (obs) {
                                                        is Obstacle.Circle -> sqrt((robotWp.x - obs.centerX).pow(2) + (robotWp.y - obs.centerY).pow(2)) - obs.radius < 0.5
                                                        is Obstacle.Rectangle -> sqrt((robotWp.x - obs.centerX).pow(2) + (robotWp.y - obs.centerY).pow(2)) - maxOf(obs.width, obs.height)/2.0 < 0.5
                                                        is Obstacle.Polygon -> obs.vertices.any { sqrt((robotWp.x - it.x).pow(2) + (robotWp.y - it.y).pow(2)) < 0.5 }
                                                    }
                                                }
                                                if (hitObs != null) updateObstacles(currentActiveObstacles - hitObs)
                                                else {
                                                    /**
                                                     * hitAt val.
                                                     */
                                                    val hitAt = currentActiveAprilTags.find { sqrt((robotWp.x - it.x).pow(2) + (robotWp.y - it.y).pow(2)) < 0.3 }
                                                    if (hitAt != null) updateAprilTags(currentActiveAprilTags - hitAt)
                                                    else {
                                                        /**
                                                         * hitFwp val.
                                                         */
                                                        val hitFwp = currentActiveFieldWaypoints.find { sqrt((robotWp.x - it.x).pow(2) + (robotWp.y - it.y).pow(2)) < 0.3 }
                                                        if (hitFwp != null) updateFieldWaypoints(currentActiveFieldWaypoints - hitFwp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                            // Reset selection state
                            selectedWaypointIndex = -1; selectedEventMarkerIndex = -1
                            selectedObstacleId = null; selectedAprilTagId = null; selectedGamePieceId = null
                            hasDragged = false
                            accumulatedDragPx = Offset.Zero
                            isDraggingHeading = false
                            isDraggingPrevHeading = false
                            isDraggingRotation = false
                            isDraggingFieldWaypoint = false
                            isDraggingFieldWaypointHeading = false
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                /**
                                 * w val.
                                 */
                                val w = size.width.toFloat(); val h = size.height.toFloat()
                                if (editorMode == EditorMode.SELECT) {
                                    /**
                                     * clickCoord val.
                                     */
                                    val clickCoord = getRobotCoordFromScreen(offset, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                    /**
                                     * hitObs val.
                                     */
                                    val hitObs = currentActiveObstacles.find { obs ->
                                        when (obs) {
                                            is Obstacle.Circle -> sqrt((clickCoord.x - obs.centerX).pow(2) + (clickCoord.y - obs.centerY).pow(2)) <= obs.radius
                                            is Obstacle.Rectangle -> {
                                                /**
                                                 * dx val.
                                                 */
                                                val dx = clickCoord.x - obs.centerX; val dy = clickCoord.y - obs.centerY
                                                /**
                                                 * rad val.
                                                 */
                                                val rad = Math.toRadians(-obs.rotation)
                                                kotlin.math.abs(dx * cos(rad) - dy * sin(rad)) <= obs.width / 2.0 && kotlin.math.abs(dx * sin(rad) + dy * cos(rad)) <= obs.height / 2.0
                                            }
                                            is Obstacle.Polygon -> obs.vertices.any { sqrt((clickCoord.x - it.x).pow(2) + (clickCoord.y - it.y).pow(2)) < 0.3 }
                                        }
                                    }
                                    if (hitObs != null) onItemDoubleTapped?.invoke(hitObs.id, "Obstacle")
                                    else {
                                        /**
                                         * hitAt val.
                                         */
                                        val hitAt = currentActiveAprilTags.find { sqrt((clickCoord.x - it.x).pow(2) + (clickCoord.y - it.y).pow(2)) < 0.3 }
                                        if (hitAt != null) onItemDoubleTapped?.invoke(hitAt.id, "AprilTag")
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                /**
                                 * event val.
                                 */
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                                    /**
                                     * offset val.
                                     */
                                    val offset = event.changes.first().position
                                    contextMenuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                                    /**
                                     * w val.
                                     */
                                    val w = size.width.toFloat(); val h = size.height.toFloat()
                                    
                                    /**
                                     * hitWpIdx val.
                                     */
                                    val hitWpIdx = currentWaypoints.indexOfFirst { sqrt((offset.x - getTransformedCanvasOffset(it, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset, viewRotation).x).pow(2) + (offset.y - getTransformedCanvasOffset(it, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset, viewRotation).y).pow(2)) < 20.dp.toPx() }
                                    if (hitWpIdx != -1) { contextTargetType = "Waypoint"; contextTargetIndex = hitWpIdx; contextMenuExpanded = true; continue }
                                    
                                    /**
                                     * hitEventIdx val.
                                     */
                                    val hitEventIdx = currentEventMarkers.indexOfFirst { sqrt((offset.x - getTransformedCanvasOffset(getPositionOnSpline(it.waypointRelativePos, currentWaypoints), w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset, viewRotation).x).pow(2) + (offset.y - getTransformedCanvasOffset(getPositionOnSpline(it.waypointRelativePos, currentWaypoints), w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset, viewRotation).y).pow(2)) < 15.dp.toPx() }
                                    if (hitEventIdx != -1) { contextTargetType = "EventMarker"; contextTargetIndex = hitEventIdx; contextMenuExpanded = true; continue }
                                    
                                    /**
                                     * clickCoord val.
                                     */
                                    val clickCoord = getRobotCoordFromScreen(offset, w, h, fieldWidthM, fieldHeightM, league, zoomScale, panOffset)
                                    /**
                                     * hitObs val.
                                     */
                                    val hitObs = currentActiveObstacles.find { obs ->
                                        when (obs) {
                                            is Obstacle.Circle -> sqrt((clickCoord.x - obs.centerX).pow(2) + (clickCoord.y - obs.centerY).pow(2)) <= obs.radius
                                            is Obstacle.Rectangle -> {
                                                /**
                                                 * dx val.
                                                 */
                                                val dx = clickCoord.x - obs.centerX; val dy = clickCoord.y - obs.centerY; val rad = Math.toRadians(-obs.rotation)
                                                kotlin.math.abs(dx * cos(rad) - dy * sin(rad)) <= obs.width / 2.0 && kotlin.math.abs(dx * sin(rad) + dy * cos(rad)) <= obs.height / 2.0
                                            }
                                            is Obstacle.Polygon -> obs.vertices.any { sqrt((clickCoord.x - it.x).pow(2) + (clickCoord.y - it.y).pow(2)) < 0.3 }
                                        }
                                    }
                                    if (hitObs != null) { contextTargetType = "Obstacle"; contextTargetId = hitObs.id; contextMenuExpanded = true; continue }
                                    
                                    /**
                                     * hitAt val.
                                     */
                                    val hitAt = currentActiveAprilTags.find { sqrt((clickCoord.x - it.x).pow(2) + (clickCoord.y - it.y).pow(2)) < 0.3 }
                                    if (hitAt != null) { contextTargetType = "AprilTag"; contextTargetId = hitAt.id; contextMenuExpanded = true; continue }
                                    
                                    /**
                                     * hitGp val.
                                     */
                                    val hitGp = currentActiveGamePieces.find { sqrt((clickCoord.x - it.x).pow(2) + (clickCoord.y - it.y).pow(2)) < 0.2 }
                                    if (hitGp != null) { contextTargetType = "GamePiece"; contextTargetId = hitGp.id; contextMenuExpanded = true; continue }

                                    /**
                                     * hitFwp val.
                                     */
                                    val hitFwp = currentActiveFieldWaypoints.find { sqrt((clickCoord.x - it.x).pow(2) + (clickCoord.y - it.y).pow(2)) < 0.3 }
                                    if (hitFwp != null) { contextTargetType = "FieldWaypoint"; contextTargetId = hitFwp.id; contextMenuExpanded = true; continue }
                                }
                            }
                        }
                    }
            ) {
                /**
                 * w val.
                 */
                val w = size.width
                /**
                 * h val.
                 */
                val h = size.height


                drawContext.canvas.save()
                // Apply view rotation around the canvas center (replaces Modifier.rotate)
                if (viewRotation != 0f) {
                    drawContext.transform.rotate(viewRotation, pivot = Offset(w / 2f, h / 2f))
                }
                drawContext.transform.translate(panOffset.x, panOffset.y)
                drawContext.transform.scale(zoomScale, zoomScale, pivot = Offset.Zero)

                drawFieldBackground(activeImage, activeConfig, w, h)
                if (showHeatmap) HeatmapOverlay.drawHeatmap(this, actualPath, fieldWidthM, fieldHeightM, league)
                
                drawFieldGrid(w, h, fieldWidthM, fieldHeightM, league, showCostmap = showCostmap)
                drawFtcAllianceStations(w, h, fieldWidthM, fieldHeightM, league, activeConfig)
                if (league == League.FTC) drawCoordinateAxes(w, h, fieldWidthM, fieldHeightM, league, textMeasurer)

                drawCustomObstacles(currentActiveObstacles, w, h, fieldWidthM, fieldHeightM, league, showCostmap = showCostmap)
                drawGamePieces(currentActiveGamePieces, w, h, fieldWidthM, fieldHeightM, league)
                drawAprilTags(currentActiveAprilTags, w, h, fieldWidthM, fieldHeightM, league, textMeasurer)
                drawFieldWaypoints(currentActiveFieldWaypoints, selectedFieldWaypointId, w, h, fieldWidthM, fieldHeightM, league, textMeasurer)
                drawActivePolygonPoints(currentPolygonPoints, w, h, fieldWidthM, fieldHeightM, league)

                if (contextPath != null) {
                    drawContextPath(pathCache, contextPath, contextWaypoints, w, h, fieldWidthM, fieldHeightM, league)
                }

                drawPlannedSpline(pathCache, splinePoints, waypoints, w, h, fieldWidthM, fieldHeightM, league)
                drawEventMarkers(waypoints, eventMarkerPoints, w, h, fieldWidthM, fieldHeightM, league, selectedEventMarkerIndex)
                drawConstraintZones(pathCache, waypoints, constraintZoneSplines, w, h, fieldWidthM, fieldHeightM, league)
                drawPointTowardsZones(pathCache, waypoints, pointTowardsZoneRenderData, w, h, fieldWidthM, fieldHeightM, league)
                drawHolonomicRotationTargets(waypoints, combinedRotationTargets, combinedRotationTargetPoints, w, h, fieldWidthM, fieldHeightM, league)

                drawActualPathAndDeviations(pathCache, actualPath, waypoints, w, h, fieldWidthM, fieldHeightM, league)
                drawRobotRepresentations(
                    pathCache = pathCache,
                    actualPath = actualPath,
                    estimatedPose = estimatedPose,
                    playbackPose = playbackPose,
                    visionPoses = visionPoses,
                    odomPose = odomPose,
                    showTruePose = showTruePose,
                    showEkfPose = showEkfPose,
                    showOdomPose = showOdomPose,
                    showVisionPoses = showVisionPoses,
                    w = w,
                    h = h,
                    fieldWidthM = fieldWidthM,
                    fieldHeightM = fieldHeightM,
                    league = league,
                    indicatorLightPosition = indicatorLightPosition
                )
                drawWaypoints(pathCache, waypoints, selectedWaypointIndex, isDraggingHeading, isDraggingPrevHeading, w, h, fieldWidthM, fieldHeightM, league, combinedRotationTargets, isDraggingRotation)

                drawContext.canvas.restore()
            }
            }

            DropdownMenu(
                expanded = contextMenuExpanded,
                onDismissRequest = { contextMenuExpanded = false },
                offset = contextMenuOffset,
                modifier = Modifier.background(AresSurfaceElevated).border(1.dp, AresBorder, RoundedCornerShape(4.dp))
            ) {
                when {
                    contextTargetType == "Waypoint" && contextTargetIndex in waypoints.indices -> {
                        DropdownMenuItem(onClick = { contextMenuExpanded = false }) { Text("Edit Waypoint...", color = AresTextPrimary) }
                        if (waypoints[contextTargetIndex].rotationDeg != null) {
                            DropdownMenuItem(onClick = {
                                onWaypointsChanged(waypoints.toMutableList().apply {
                                    set(contextTargetIndex, this[contextTargetIndex].copy(rotationDeg = null))
                                })
                                contextMenuExpanded = false
                            }) { Text("Clear Rotation", color = AresTextPrimary) }
                        }
                        if (waypoints[contextTargetIndex].headingRad != null) {
                            DropdownMenuItem(onClick = {
                                onWaypointsChanged(waypoints.toMutableList().apply {
                                    set(contextTargetIndex, this[contextTargetIndex].copy(headingRad = null))
                                })
                                contextMenuExpanded = false
                            }) { Text("Reset to Auto Heading", color = AresTextPrimary) }
                        }
                        DropdownMenuItem(onClick = {
                            onWaypointsChanged(waypoints.toMutableList().apply { removeAt(contextTargetIndex) })
                            contextMenuExpanded = false; selectedWaypointIndex = -1
                        }) { Text("Delete Waypoint", color = AresRed) }
                    }
                    contextTargetType == "Obstacle" && contextTargetId != null -> {
                        DropdownMenuItem(onClick = {
                            updateObstacles(currentActiveObstacles.filter { it.id != contextTargetId })
                            contextMenuExpanded = false; selectedObstacleId = null
                        }) { Text("Delete Obstacle", color = AresRed) }
                    }
                    contextTargetType == "AprilTag" && contextTargetId != null -> {
                        DropdownMenuItem(onClick = {
                            updateAprilTags(currentActiveAprilTags.filter { it.id != contextTargetId })
                            contextMenuExpanded = false; selectedAprilTagId = null
                        }) { Text("Delete AprilTag", color = AresRed) }
                    }
                    contextTargetType == "GamePiece" && contextTargetId != null -> {
                        /**
                         * gp val.
                         */
                        val gp = currentActiveGamePieces.find { it.id == contextTargetId }
                        if (gp != null) {
                            DropdownMenuItem(onClick = {
                                /**
                                 * nextType val.
                                 */
                                val nextType = if (gp.type == "Sample") "Specimen" else "Sample"
                                updateGamePieces(currentActiveGamePieces.map { if (it.id == contextTargetId) it.copy(type = nextType) else it })
                                contextMenuExpanded = false
                            }) { Text("Toggle Type: ${if (gp.type == "Sample") "Specimen" else "Sample"}", color = AresTextPrimary) }
                        }
                        DropdownMenuItem(onClick = {
                            updateGamePieces(currentActiveGamePieces.filter { it.id != contextTargetId })
                            contextMenuExpanded = false; selectedGamePieceId = null
                        }) { Text("Delete Game Piece", color = AresRed) }
                    }
                    contextTargetType == "FieldWaypoint" && contextTargetId != null -> {
                        DropdownMenuItem(onClick = {
                            updateFieldWaypoints(currentActiveFieldWaypoints.filter { it.id != contextTargetId })
                            contextMenuExpanded = false; selectedFieldWaypointId = null
                        }) { Text("Delete Field Waypoint", color = AresRed) }
                    }
                    contextTargetType == "EventMarker" && contextTargetIndex in currentEventMarkers.indices -> {
                        DropdownMenuItem(onClick = {
                            onEventMarkersChanged?.invoke(currentEventMarkers.toMutableList().apply { removeAt(contextTargetIndex) })
                            contextMenuExpanded = false; selectedEventMarkerIndex = -1
                        }) { Text("Delete Event Marker", color = AresRed) }
                    }
                    else -> {
                        DropdownMenuItem(onClick = { contextMenuExpanded = false }) { Text("Cancel", color = AresTextSecondary) }
                    }
                }
            }

            PlannerExportPanel(
                showPathControls = showPathControls,
                projectPath = projectPath,
                league = league,
                waypoints = waypoints,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
