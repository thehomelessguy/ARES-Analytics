package com.ares.analytics.ui.components.pathplanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.DropdownMenu
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.shared.League
import com.ares.analytics.shared.Obstacle
import com.ares.analytics.shared.PathPoint
import com.ares.analytics.shared.FieldImageConfig
import com.ares.analytics.shared.GamePiece
import com.ares.analytics.shared.PathPlannerEventMarker
import com.ares.analytics.shared.RotationTarget
import com.ares.analytics.shared.ConstraintsZone
import com.ares.analytics.shared.PointTowardsZone
import com.ares.analytics.shared.PathConstraints
import com.ares.analytics.ui.theme.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class EditorMode {
    SELECT, ADD_WAYPOINT, DRAW_POLYGON, DRAW_CIRCLE, DRAW_RECTANGLE, PLACE_GAME_PIECE, PLACE_APRILTAG, ERASER
}

private data class PointTowardsZoneRenderData(
    val target: Waypoint,
    val splinePoints: List<Waypoint>
)

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FieldCanvas(
    league: League,
    waypoints: List<Waypoint>,
    actualPath: List<Waypoint>,
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
    aprilTags: List<com.ares.analytics.shared.AprilTagPlacement>? = null,
    onAprilTagsChanged: ((List<com.ares.analytics.shared.AprilTagPlacement>) -> Unit)? = null,
    eventMarkers: List<PathPlannerEventMarker> = emptyList(),
    onEventMarkersChanged: ((List<PathPlannerEventMarker>) -> Unit)? = null,
    rotationTargets: List<RotationTarget> = emptyList(),
    constraintZones: List<ConstraintsZone> = emptyList(),
    pointTowardsZones: List<PointTowardsZone> = emptyList(),
    globalConstraints: PathConstraints = PathConstraints(),
    estimatedPose: Waypoint? = null,
    visionPoses: List<Waypoint> = emptyList(),
    onItemSelected: ((String?, String?) -> Unit)? = null,
    onItemDoubleTapped: ((String, String) -> Unit)? = null,
    initialViewRotation: Float = 0f,
    onViewRotationChanged: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var localFieldImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var localFieldImageConfig by remember { mutableStateOf(FieldImageConfig()) }
    var isDraggingHeading by remember { mutableStateOf(false) }
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    // Precomputed Cache Data Structures for zero-allocation rendering performance
    class PathCacheHolder {
        var splinePoints: List<Waypoint> = emptyList()
        var actualPoints: List<Waypoint> = emptyList()
        var constraintSplines: List<List<Waypoint>> = emptyList()
        var w: Float = 0f
        var h: Float = 0f
        var splinePath: Path? = null
        var actualPath: Path? = null
        var constraintPaths: List<Path> = emptyList()
    }
    
    val pathCache = remember { PathCacheHolder() }

    var contextMenuExpanded by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var contextTargetType by remember { mutableStateOf<String?>(null) }
    var contextTargetId by remember { mutableStateOf<String?>(null) }
    var contextTargetIndex by remember { mutableStateOf<Int>(-1) }

    val density = LocalDensity.current

    val splinePoints = remember(waypoints) {
        if (waypoints.size < 2) {
            emptyList<Waypoint>()
        } else {
            val list = ArrayList<Waypoint>((waypoints.size - 1) * 30 + 1)
            val density = 30
            for (i in 0 until waypoints.size - 1) {
                val p0 = waypoints[i]
                val p1 = waypoints[i + 1]
                val v0x = kotlin.math.cos(p0.headingRad) * p0.tangentMagnitude
                val v0y = kotlin.math.sin(p0.headingRad) * p0.tangentMagnitude
                val v1x = kotlin.math.cos(p1.headingRad) * p1.tangentMagnitude
                val v1y = kotlin.math.sin(p1.headingRad) * p1.tangentMagnitude

                for (j in 1..density) {
                    val t = j.toDouble() / density
                    val px = cubicHermite(p0.x, v0x, p1.x, v1x, t)
                    val py = cubicHermite(p0.y, v0y, p1.y, v1y, t)
                    list.add(Waypoint(px, py))
                }
            }
            list
        }
    }

    val eventMarkerPoints = remember(eventMarkers, waypoints) {
        if (waypoints.size < 2) emptyList<Waypoint>()
        else eventMarkers.map { getPositionOnSpline(it.waypointRelativePos, waypoints) }
    }

    val rotationTargetPoints = remember(rotationTargets, waypoints) {
        if (waypoints.size < 2) emptyList<Waypoint>()
        else rotationTargets.map { getPositionOnSpline(it.waypointRelativePos, waypoints) }
    }

    val constraintZoneSplines = remember(constraintZones, waypoints) {
        if (waypoints.size < 2) emptyList<List<Waypoint>>()
        else constraintZones.map { zone ->
            val minPos = zone.minWaypointRelativePos
            val maxPos = zone.maxWaypointRelativePos
            val steps = ((maxPos - minPos) * 20).toInt().coerceIn(2, 100)
            val pathPoints = ArrayList<Waypoint>(steps + 1)
            for (step in 0..steps) {
                val pos = minPos + (maxPos - minPos) * (step.toDouble() / steps)
                pathPoints.add(getPositionOnSpline(pos, waypoints))
            }
            pathPoints
        }
    }

    val pointTowardsZoneRenderData = remember(pointTowardsZones, waypoints) {
        if (waypoints.size < 2) emptyList<PointTowardsZoneRenderData>()
        else pointTowardsZones.map { zone ->
            val minPos = zone.minWaypointRelativePos
            val maxPos = zone.maxWaypointRelativePos
            val numLines = 5
            val splinePoints = ArrayList<Waypoint>(numLines)
            for (k in 0 until numLines) {
                val t = if (numLines > 1) k.toDouble() / (numLines - 1) else 0.5
                val pos = minPos + (maxPos - minPos) * t
                splinePoints.add(getPositionOnSpline(pos, waypoints))
            }
            PointTowardsZoneRenderData(
                target = Waypoint(zone.fieldPosition.x, zone.fieldPosition.y),
                splinePoints = splinePoints
            )
        }
    }

    val activeImage = fieldImage ?: localFieldImage
    val activeConfig = fieldImageConfig ?: localFieldImageConfig

    val fieldWidthM = if (activeConfig.widthMeters > 0.0) activeConfig.widthMeters else (if (league == League.FTC) 3.65 else 16.5)
    val fieldHeightM = if (activeConfig.heightMeters > 0.0) activeConfig.heightMeters else (if (league == League.FTC) 3.65 else 8.2)

    var editorMode by remember { mutableStateOf(EditorMode.SELECT) }
    var selectedWaypointIndex by remember { mutableStateOf(-1) }
    var selectedEventMarkerIndex by remember { mutableStateOf(-1) }
    var selectedObstacleId by remember { mutableStateOf<String?>(null) }
    var selectedAprilTagId by remember { mutableStateOf<String?>(null) }
    var selectedGamePieceId by remember { mutableStateOf<String?>(null) }
    var activeGamePieceType by remember { mutableStateOf(if (league == League.FTC) "Sample (Yellow)" else "Note") }
    
    // Zoom & Pan state
    var zoomScale by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showHeatmap by remember { mutableStateOf(false) }
    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val isShiftPressed = windowInfo.keyboardModifiers.isShiftPressed
    var viewRotation by remember(initialViewRotation) { mutableStateOf(initialViewRotation) }
    
    // Local lists for when parameters are null
    val localObstacles = remember { mutableStateListOf<Obstacle>() }
    val localGamePieces = remember { mutableStateListOf<GamePiece>() }
    val localAprilTags = remember { mutableStateListOf<com.ares.analytics.shared.AprilTagPlacement>() }

    val activeObstacles = obstacles ?: localObstacles
    val activeGamePieces = gamePieces ?: localGamePieces
    val activeAprilTags = aprilTags ?: localAprilTags

    val updateObstacles: (List<Obstacle>) -> Unit = { newObstacles ->
        if (onObstaclesChanged != null) {
            onObstaclesChanged(newObstacles)
        } else {
            localObstacles.clear()
            localObstacles.addAll(newObstacles)
        }
        if (!projectPath.isNullOrEmpty()) {
            try {
                val relativeDir = if (league == League.FTC) "src/main/assets/paths" else "src/main/deploy/paths"
                val targetDir = File(projectPath, relativeDir)
                targetDir.mkdirs()
                val targetFile = File(targetDir, "obstacles.json")
                val jsonFormat = Json { prettyPrint = true }
                val content = jsonFormat.encodeToString(newObstacles)
                targetFile.writeText(content)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val updateGamePieces: (List<GamePiece>) -> Unit = { newPieces ->
        if (onGamePiecesChanged != null) {
            onGamePiecesChanged(newPieces)
        } else {
            localGamePieces.clear()
            localGamePieces.addAll(newPieces)
        }
        if (!projectPath.isNullOrEmpty()) {
            try {
                val relativeDir = if (league == League.FTC) "src/main/assets/paths" else "src/main/deploy/paths"
                val targetDir = File(projectPath, relativeDir)
                targetDir.mkdirs()
                val targetFile = File(targetDir, "game_pieces.json")
                val jsonFormat = Json { prettyPrint = true }
                val content = jsonFormat.encodeToString(newPieces)
                targetFile.writeText(content)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    val updateAprilTags: (List<com.ares.analytics.shared.AprilTagPlacement>) -> Unit = { newTags ->
        if (onAprilTagsChanged != null) {
            onAprilTagsChanged(newTags)
        } else {
            localAprilTags.clear()
            localAprilTags.addAll(newTags)
        }
        if (!projectPath.isNullOrEmpty()) {
            try {
                val relativeDir = if (league == League.FTC) "src/main/assets/paths" else "src/main/deploy/paths"
                val targetDir = File(projectPath, relativeDir)
                targetDir.mkdirs()
                val targetFile = File(targetDir, "apriltags.json")
                val jsonFormat = Json { prettyPrint = true }
                val content = jsonFormat.encodeToString(newTags)
                targetFile.writeText(content)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Save path name state
    var pathName by remember { mutableStateOf("autonomous_route") }
    var saveStatus by remember { mutableStateOf("") }

    var currentPolygonPoints = remember { mutableStateListOf<PathPoint>() }

    val currentWaypoints by rememberUpdatedState(waypoints)
    val currentActiveObstacles by rememberUpdatedState(activeObstacles)
    val currentActiveGamePieces by rememberUpdatedState(activeGamePieces)
    val currentActiveAprilTags by rememberUpdatedState(activeAprilTags)
    val currentEventMarkers by rememberUpdatedState(eventMarkers)
    val currentOnWaypointsChanged by rememberUpdatedState(onWaypointsChanged)
    val currentUpdateObstacles by rememberUpdatedState(updateObstacles)
    val currentUpdateGamePieces by rememberUpdatedState(updateGamePieces)
    val currentUpdateAprilTags by rememberUpdatedState(updateAprilTags)
    val currentRotationTargets by rememberUpdatedState(rotationTargets)
    val currentConstraintZones by rememberUpdatedState(constraintZones)
    val currentPointTowardsZones by rememberUpdatedState(pointTowardsZones)
    val currentGlobalConstraints by rememberUpdatedState(globalConstraints)

    LaunchedEffect(projectPath) {
        if (!projectPath.isNullOrEmpty()) {
            try {
                val relativeDir = if (league == League.FTC) "src/main/assets/paths" else "src/main/deploy/paths"
                val file = File(File(projectPath, relativeDir), "obstacles.json")
                if (file.exists()) {
                    val content = file.readText()
                    val loaded = Json.decodeFromString<List<Obstacle>>(content)
                    if (obstacles != null && onObstaclesChanged != null) {
                        onObstaclesChanged(loaded)
                    } else {
                        localObstacles.clear()
                        localObstacles.addAll(loaded)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val relativeDir = if (league == League.FTC) "src/main/assets/paths" else "src/main/deploy/paths"
                val file = File(File(projectPath, relativeDir), "game_pieces.json")
                if (file.exists()) {
                    val content = file.readText()
                    val loaded = Json.decodeFromString<List<GamePiece>>(content)
                    if (gamePieces != null && onGamePiecesChanged != null) {
                        onGamePiecesChanged(loaded)
                    } else {
                        localGamePieces.clear()
                        localGamePieces.addAll(loaded)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val relativeDir = if (league == League.FTC) "src/main/assets/paths" else "src/main/deploy/paths"
                val file = File(File(projectPath, relativeDir), "apriltags.json")
                if (file.exists()) {
                    val content = file.readText()
                    val loaded = Json.decodeFromString<List<com.ares.analytics.shared.AprilTagPlacement>>(content)
                    if (aprilTags != null && onAprilTagsChanged != null) {
                        onAprilTagsChanged(loaded)
                    } else {
                        localAprilTags.clear()
                        localAprilTags.addAll(loaded)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val relativeDir = if (league == League.FTC) "src/main/assets" else "src/main/deploy"
                val imgFile = File(File(projectPath, relativeDir), "field_image.png")
                if (imgFile.exists()) {
                    val bitmap = imgFile.inputStream().use { loadImageBitmap(it) }
                    localFieldImage = bitmap
                } else {
                    localFieldImage = null
                }

                val configFile = File(File(projectPath, relativeDir), "field_image_config.json")
                if (configFile.exists()) {
                    val content = configFile.readText()
                    localFieldImageConfig = Json.decodeFromString<FieldImageConfig>(content)
                } else {
                    localFieldImageConfig = FieldImageConfig()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Helpers for mapping coordinates under zoom & pan transformations
    fun getTransformedCanvasOffset(wp: Waypoint, w: Float, h: Float): Offset {
        val base = getCanvasOffsetBase(wp, w, h, fieldWidthM, fieldHeightM, league)
        // Apply zoom scale (relative to top-left or pivot) and pan offset
        return Offset(
            x = base.x * zoomScale + panOffset.x,
            y = base.y * zoomScale + panOffset.y
        )
    }

    fun getRobotCoordFromScreen(screenOffset: Offset, w: Float, h: Float): Waypoint {
        // Reverse pan & zoom
        val baseOffset = Offset(
            x = (screenOffset.x - panOffset.x) / zoomScale,
            y = (screenOffset.y - panOffset.y) / zoomScale
        )
        return getRobotCoordBase(baseOffset, w, h, fieldWidthM, fieldHeightM, league)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar Row (Outside Canvas, top aligned)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = AresSurfaceElevated
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showPathControls || showObstacleControls) {
                    // Select Mode
                    IconButton(
                        onClick = { editorMode = EditorMode.SELECT },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = if (editorMode == EditorMode.SELECT) AresCyan else AresTextTertiary)
                    ) {
                        Icon(Icons.Default.PanTool, contentDescription = "Select / Drag")
                    }
                }

                // Add Waypoint Mode
                if (showPathControls) {
                    IconButton(
                        onClick = { editorMode = EditorMode.ADD_WAYPOINT },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = if (editorMode == EditorMode.ADD_WAYPOINT) AresCyan else AresTextTertiary)
                    ) {
                        Icon(Icons.Default.AddLocation, contentDescription = "Add Waypoint")
                    }
                }

                // Draw Polygon Obstacle
                if (showObstacleControls) {
                    IconButton(
                        onClick = { editorMode = EditorMode.DRAW_POLYGON },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = if (editorMode == EditorMode.DRAW_POLYGON) AresCyan else AresTextTertiary)
                    ) {
                        Icon(Icons.Default.Polyline, contentDescription = "Draw Free Form Obstacle")
                    }
                }
                
                if (showObstacleControls && editorMode == EditorMode.DRAW_POLYGON && currentPolygonPoints.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            if (currentPolygonPoints.size >= 3) {
                                val newPoly = Obstacle.Polygon(
                                    id = "poly_${System.currentTimeMillis()}",
                                    name = "Polygon Obstacle ${activeObstacles.size + 1}",
                                    vertices = currentPolygonPoints.toList()
                                )
                                updateObstacles(activeObstacles + newPoly)
                            }
                            currentPolygonPoints.clear()
                        }
                    ) {
                        Text("Finish Poly", color = AresCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Draw Circle Obstacle
                if (showObstacleControls) {
                    IconButton(
                        onClick = { editorMode = EditorMode.DRAW_CIRCLE },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = if (editorMode == EditorMode.DRAW_CIRCLE) AresCyan else AresTextTertiary)
                    ) {
                        Icon(Icons.Default.TripOrigin, contentDescription = "Draw Circle Obstacle")
                    }
                }

                // Draw Rectangle Obstacle
                if (showObstacleControls) {
                    IconButton(
                        onClick = { editorMode = EditorMode.DRAW_RECTANGLE },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = if (editorMode == EditorMode.DRAW_RECTANGLE) AresCyan else AresTextTertiary)
                    ) {
                        Icon(Icons.Default.CropSquare, contentDescription = "Draw Rect Obstacle")
                    }
                }

                // Place Game Piece Mode
                if (showObstacleControls) {
                    IconButton(
                        onClick = { editorMode = EditorMode.PLACE_GAME_PIECE },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = if (editorMode == EditorMode.PLACE_GAME_PIECE) AresCyan else AresTextTertiary)
                    ) {
                        Icon(Icons.Default.Extension, contentDescription = "Place Game Piece")
                    }
                }

                if (showObstacleControls && editorMode == EditorMode.PLACE_GAME_PIECE) {
                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(AresBorder))
                    if (league == League.FTC) {
                        listOf("Sample (Yellow)", "Sample (Red)", "Sample (Blue)", "Specimen").forEach { type ->
                            val isSel = activeGamePieceType == type
                            val color = when (type) {
                                "Sample (Yellow)" -> Color.Yellow
                                "Sample (Red)" -> AresRed
                                "Sample (Blue)" -> AresCyan
                                else -> Color.Magenta
                            }
                            TextButton(
                                onClick = { activeGamePieceType = type },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = type.substringAfter("(").substringBefore(")").take(3),
                                    color = if (isSel) color else AresTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    } else {
                        listOf("Note", "High Note").forEach { type ->
                            val isSel = activeGamePieceType == type
                            TextButton(
                                onClick = { activeGamePieceType = type },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = type,
                                    color = if (isSel) AresAmber else AresTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Place AprilTag Mode
                if (showObstacleControls && league == League.FTC) {
                    IconButton(
                        onClick = { editorMode = EditorMode.PLACE_APRILTAG },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = if (editorMode == EditorMode.PLACE_APRILTAG) AresCyan else AresTextTertiary)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Place AprilTag")
                    }
                }

                // Eraser Mode
                if (showPathControls || showObstacleControls) {
                    IconButton(
                        onClick = { editorMode = EditorMode.ERASER },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = if (editorMode == EditorMode.ERASER) AresCyan else AresTextTertiary)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Eraser")
                    }
                }

                if (showPathControls || showObstacleControls) {
                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(AresBorder))
                }

                // Zoom controls
                IconButton(onClick = { zoomScale = (zoomScale + 0.1f).coerceAtMost(3f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = AresTextSecondary)
                }
                IconButton(onClick = { zoomScale = (zoomScale - 0.1f).coerceAtLeast(0.5f) }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = AresTextSecondary)
                }
                IconButton(onClick = {
                    zoomScale = 1f
                    panOffset = Offset.Zero
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom", tint = AresTextSecondary)
                }
                IconButton(
                    onClick = { showHeatmap = !showHeatmap },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = if (showHeatmap) AresCyan else AresTextTertiary)
                ) {
                    Icon(Icons.Default.Whatshot, contentDescription = "Toggle Heatmap")
                }
                IconButton(
                    onClick = { 
                        viewRotation = (viewRotation - 90f + 360f) % 360f 
                        onViewRotationChanged?.invoke(viewRotation)
                    }
                ) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate View", tint = AresTextSecondary)
                }
            }
        }

        // Canvas Area (Below toolbar, takes remaining space)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .aspectRatio(fieldWidthM.toFloat() / fieldHeightM.toFloat())
                    .align(Alignment.Center)
                    .rotate(viewRotation)
                    .background(AresSurface)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { },
                            onDragEnd = {
                                selectedWaypointIndex = -1
                                selectedEventMarkerIndex = -1
                                selectedObstacleId = null
                                selectedAprilTagId = null
                                selectedGamePieceId = null
                                isDraggingHeading = false
                            },
                            onDragCancel = {
                                selectedWaypointIndex = -1
                                selectedEventMarkerIndex = -1
                                selectedObstacleId = null
                                selectedAprilTagId = null
                                selectedGamePieceId = null
                                isDraggingHeading = false
                            },
                            onDrag = { change, dragAmount ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val selectedIdx = selectedWaypointIndex
                                val selectedEventIdx = selectedEventMarkerIndex
                                val selectedObs = selectedObstacleId
                                val selectedAt = selectedAprilTagId
                                val selectedGp = selectedGamePieceId
                                
                                fun snap(v: Double) = if (isShiftPressed) kotlin.math.round(v * 10.0) / 10.0 else v
                                
                                if (selectedEventIdx != -1 && onEventMarkersChanged != null) {
                                    val pos = change.position
                                    val bestPos = getClosestSplinePosition(
                                        mouseOffset = pos,
                                        waypoints = currentWaypoints,
                                        canvasW = w,
                                        canvasH = h,
                                        fieldW = fieldWidthM,
                                        fieldH = fieldHeightM,
                                        league = league
                                    )
                                    val updated = currentEventMarkers.toMutableList().apply {
                                        set(selectedEventIdx, this[selectedEventIdx].copy(waypointRelativePos = bestPos))
                                    }
                                    onEventMarkersChanged(updated)
                                } else if (selectedIdx != -1) {
                                    val pos = change.position
                                    if (isDraggingHeading) {
                                        val wp = currentWaypoints[selectedIdx]
                                        val wpOffset = getTransformedCanvasOffset(wp, w, h)
                                        val dx = pos.x - wpOffset.x
                                        val dy = pos.y - wpOffset.y
                                        val rawAngle = kotlin.math.atan2(-dy.toDouble(), dx.toDouble())
                                        val angle = if (isShiftPressed) snap(rawAngle * 180.0 / Math.PI) * Math.PI / 180.0 else rawAngle
                                        
                                        val posMeters = getRobotCoordFromScreen(pos, w, h)
                                        val mag = sqrt((posMeters.x - wp.x).pow(2) + (posMeters.y - wp.y).pow(2))
                                        val finalMag = if (isShiftPressed) snap(mag) else mag
                                        
                                        val updated = currentWaypoints.toMutableList().apply {
                                            set(selectedIdx, wp.copy(headingRad = angle, tangentMagnitude = finalMag))
                                        }
                                        currentOnWaypointsChanged(updated)
                                    } else {
                                        val wp = getRobotCoordFromScreen(pos, w, h)
                                        val updated = currentWaypoints.toMutableList().apply {
                                            set(selectedIdx, Waypoint(snap(wp.x), snap(wp.y), this[selectedIdx].headingRad))
                                        }
                                        currentOnWaypointsChanged(updated)
                                    }
                                } else if (selectedObs != null) {
                                    val prevPos = change.previousPosition
                                    val currPos = change.position
                                    val prevWp = getRobotCoordFromScreen(prevPos, w, h)
                                    val currWp = getRobotCoordFromScreen(currPos, w, h)
                                    val dx = currWp.x - prevWp.x
                                    val dy = currWp.y - prevWp.y

                                    val updatedObsList = currentActiveObstacles.map { obs ->
                                        if (obs.id == selectedObs) {
                                            when (obs) {
                                                is Obstacle.Circle -> obs.copy(centerX = snap(obs.centerX + dx), centerY = snap(obs.centerY + dy))
                                                is Obstacle.Rectangle -> obs.copy(centerX = snap(obs.centerX + dx), centerY = snap(obs.centerY + dy))
                                                is Obstacle.Polygon -> obs.copy(vertices = obs.vertices.map { PathPoint(snap(it.x + dx), snap(it.y + dy)) })
                                            }
                                        } else obs
                                    }
                                    currentUpdateObstacles(updatedObsList)
                                } else if (selectedAt != null) {
                                    val prevPos = change.previousPosition
                                    val currPos = change.position
                                    val prevWp = getRobotCoordFromScreen(prevPos, w, h)
                                    val currWp = getRobotCoordFromScreen(currPos, w, h)
                                    val dx = currWp.x - prevWp.x
                                    val dy = currWp.y - prevWp.y

                                    val updatedList = currentActiveAprilTags.map { at ->
                                        if (at.id == selectedAt) {
                                            at.copy(x = snap(at.x + dx), y = snap(at.y + dy))
                                        } else at
                                    }
                                    currentUpdateAprilTags(updatedList)
                                } else if (selectedGp != null) {
                                    val prevPos = change.previousPosition
                                    val currPos = change.position
                                    val prevWp = getRobotCoordFromScreen(prevPos, w, h)
                                    val currWp = getRobotCoordFromScreen(currPos, w, h)
                                    val dx = currWp.x - prevWp.x
                                    val dy = currWp.y - prevWp.y

                                    val updatedList = currentActiveGamePieces.map { gp ->
                                        if (gp.id == selectedGp) {
                                            gp.copy(x = snap(gp.x + dx), y = snap(gp.y + dy))
                                        } else gp
                                    }
                                    currentUpdateGamePieces(updatedList)
                                } else {
                                    // Panning field background is disabled as requested by the user
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                if (editorMode == EditorMode.SELECT) {
                                    val clickCoord = getRobotCoordFromScreen(offset, w, h)
                                    // Check obstacles
                                    var hitObsId: String? = null
                                    for (obs in currentActiveObstacles) {
                                        val isHit = when (obs) {
                                            is Obstacle.Circle -> {
                                                val dist = sqrt((clickCoord.x - obs.centerX).pow(2) + (clickCoord.y - obs.centerY).pow(2))
                                                dist <= obs.radius
                                            }
                                            is Obstacle.Rectangle -> {
                                                val dx = clickCoord.x - obs.centerX
                                                val dy = clickCoord.y - obs.centerY
                                                val rad = Math.toRadians(-obs.rotation)
                                                val rx = dx * cos(rad) - dy * sin(rad)
                                                val ry = dx * sin(rad) + dy * cos(rad)
                                                kotlin.math.abs(rx) <= obs.width / 2.0 && kotlin.math.abs(ry) <= obs.height / 2.0
                                            }
                                            is Obstacle.Polygon -> {
                                                var nearVertex = false
                                                for (v in obs.vertices) {
                                                    val d = sqrt((clickCoord.x - v.x).pow(2) + (clickCoord.y - v.y).pow(2))
                                                    if (d < 0.3) {
                                                        nearVertex = true
                                                        break
                                                    }
                                                }
                                                nearVertex
                                            }
                                        }
                                        if (isHit) {
                                            hitObsId = obs.id
                                            break
                                        }
                                    }
                                    if (hitObsId != null) {
                                        onItemDoubleTapped?.invoke(hitObsId, "Obstacle")
                                    } else {
                                        // Check AprilTags
                                        var hitAtId: String? = null
                                        for (at in currentActiveAprilTags) {
                                            val dist = sqrt((clickCoord.x - at.x).pow(2) + (clickCoord.y - at.y).pow(2))
                                            if (dist < 0.3) {
                                                hitAtId = at.id
                                                break
                                            }
                                        }
                                        if (hitAtId != null) {
                                            onItemDoubleTapped?.invoke(hitAtId, "AprilTag")
                                        }
                                    }
                                }
                            },
                            onPress = { offset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                
                                when (editorMode) {
                                    EditorMode.SELECT -> {
                                        // Hit test heading handles first
                                        var hitIdx = -1
                                        var hitHeading = false
                                        for (i in currentWaypoints.indices) {
                                            val wp = currentWaypoints[i]
                                            val handleMeters = Waypoint(wp.x + wp.tangentMagnitude * kotlin.math.cos(wp.headingRad), wp.y + wp.tangentMagnitude * kotlin.math.sin(wp.headingRad))
                                            val handleOffset = getCanvasOffsetBase(handleMeters, w, h, fieldWidthM, fieldHeightM, league)
                                            val distHandle = sqrt((offset.x - handleOffset.x).pow(2) + (offset.y - handleOffset.y).pow(2))
                                            if (distHandle < 15.dp.toPx()) {
                                                hitIdx = i
                                                hitHeading = true
                                                break
                                            }
                                        }

                                        if (hitIdx == -1) {
                                            // Hit test waypoint anchors
                                            for (i in currentWaypoints.indices) {
                                                val wpOffset = getTransformedCanvasOffset(currentWaypoints[i], w, h)
                                                val dist = sqrt((offset.x - wpOffset.x).pow(2) + (offset.y - wpOffset.y).pow(2))
                                                if (dist < 20.dp.toPx()) {
                                                    hitIdx = i
                                                    hitHeading = false
                                                    break
                                                }
                                            }
                                        }

                                        var hitEventIdx = -1
                                        if (hitIdx == -1) {
                                            // Hit test event markers
                                            for (i in currentEventMarkers.indices) {
                                                val marker = currentEventMarkers[i]
                                                val markerWp = getPositionOnSpline(marker.waypointRelativePos, currentWaypoints)
                                                val markerOffset = getCanvasOffsetBase(markerWp, w, h, fieldWidthM, fieldHeightM, league)
                                                val dist = sqrt((offset.x - markerOffset.x).pow(2) + (offset.y - markerOffset.y).pow(2))
                                                if (dist < 15.dp.toPx()) {
                                                    hitEventIdx = i
                                                    break
                                                }
                                            }
                                        }

                                        selectedWaypointIndex = hitIdx
                                        selectedEventMarkerIndex = hitEventIdx
                                        isDraggingHeading = hitIdx != -1 && hitHeading
                                        
                                        if (hitIdx == -1 && hitEventIdx == -1) {
                                            // Hit test obstacles
                                            val clickCoord = getRobotCoordFromScreen(offset, w, h)
                                            var hitObsId: String? = null
                                            for (obs in currentActiveObstacles) {
                                                val isHit = when (obs) {
                                                    is Obstacle.Circle -> {
                                                        val dist = sqrt((clickCoord.x - obs.centerX).pow(2) + (clickCoord.y - obs.centerY).pow(2))
                                                        dist <= obs.radius
                                                    }
                                                    is Obstacle.Rectangle -> {
                                                        val dx = clickCoord.x - obs.centerX
                                                        val dy = clickCoord.y - obs.centerY
                                                        val rad = Math.toRadians(-obs.rotation)
                                                        val rx = dx * cos(rad) - dy * sin(rad)
                                                        val ry = dx * sin(rad) + dy * cos(rad)
                                                        kotlin.math.abs(rx) <= obs.width / 2.0 && kotlin.math.abs(ry) <= obs.height / 2.0
                                                    }
                                                    is Obstacle.Polygon -> {
                                                        var nearVertex = false
                                                        for (v in obs.vertices) {
                                                            val d = sqrt((clickCoord.x - v.x).pow(2) + (clickCoord.y - v.y).pow(2))
                                                            if (d < 0.3) {
                                                                nearVertex = true
                                                                break
                                                            }
                                                        }
                                                        nearVertex
                                                    }
                                                }
                                                if (isHit) {
                                                    hitObsId = obs.id
                                                    break
                                                }
                                            }
                                            selectedObstacleId = hitObsId
                                            if (hitObsId != null) {
                                                onItemSelected?.invoke(hitObsId, "Obstacle")
                                            }
                                        } else {
                                            selectedObstacleId = null
                                        }

                                        if (hitIdx == -1 && hitEventIdx == -1 && selectedObstacleId == null) {
                                            val clickCoord = getRobotCoordFromScreen(offset, w, h)
                                            var hitAtId: String? = null
                                            for (at in currentActiveAprilTags) {
                                                val dist = sqrt((clickCoord.x - at.x).pow(2) + (clickCoord.y - at.y).pow(2))
                                                if (dist < 0.3) {
                                                    hitAtId = at.id
                                                    break
                                                }
                                            }
                                            selectedAprilTagId = hitAtId
                                            if (hitAtId != null) {
                                                onItemSelected?.invoke(hitAtId, "AprilTag")
                                            }
                                        } else {
                                            selectedAprilTagId = null
                                        }

                                        if (hitIdx == -1 && hitEventIdx == -1 && selectedObstacleId == null && selectedAprilTagId == null) {
                                            val clickCoord = getRobotCoordFromScreen(offset, w, h)
                                            var hitGpId: String? = null
                                            for (gp in currentActiveGamePieces) {
                                                val dist = sqrt((clickCoord.x - gp.x).pow(2) + (clickCoord.y - gp.y).pow(2))
                                                if (dist < 0.2) {
                                                    hitGpId = gp.id
                                                    break
                                                }
                                            }
                                            selectedGamePieceId = hitGpId
                                            if (hitGpId != null) {
                                                onItemSelected?.invoke(hitGpId, "GamePiece")
                                            }
                                        } else {
                                            selectedGamePieceId = null
                                        }

                                        if (selectedObstacleId == null && selectedAprilTagId == null && selectedGamePieceId == null) {
                                            onItemSelected?.invoke(null, null)
                                        }
                                    }
                                    EditorMode.ADD_WAYPOINT -> {
                                        val robotWp = getTransformedCanvasOffset(Waypoint(0.0, 0.0), w, h) // Dummy check
                                        val newWp = getRobotCoordFromScreen(offset, w, h)
                                        val updated = currentWaypoints + newWp
                                        currentOnWaypointsChanged(updated)
                                    }
                                    EditorMode.DRAW_POLYGON -> {
                                        val newWp = getRobotCoordFromScreen(offset, w, h)
                                        currentPolygonPoints.add(PathPoint(newWp.x, newWp.y))
                                    }
                                    EditorMode.DRAW_CIRCLE -> {
                                        val newWp = getRobotCoordFromScreen(offset, w, h)
                                        val newCircle = Obstacle.Circle(
                                            id = "circle_${System.currentTimeMillis()}",
                                            name = "Circle Obstacle ${currentActiveObstacles.size + 1}",
                                            centerX = newWp.x,
                                            centerY = newWp.y,
                                            radius = 0.25
                                        )
                                        currentUpdateObstacles(currentActiveObstacles + newCircle)
                                    }
                                    EditorMode.DRAW_RECTANGLE -> {
                                        val newWp = getRobotCoordFromScreen(offset, w, h)
                                        val newRect = Obstacle.Rectangle(
                                            id = "rect_${System.currentTimeMillis()}",
                                            name = "Rectangle Obstacle ${currentActiveObstacles.size + 1}",
                                            centerX = newWp.x,
                                            centerY = newWp.y,
                                            width = 0.5,
                                            height = 0.5,
                                            rotation = 0.0
                                        )
                                        currentUpdateObstacles(currentActiveObstacles + newRect)
                                    }
                                    EditorMode.PLACE_GAME_PIECE -> {
                                        val newWp = getRobotCoordFromScreen(offset, w, h)
                                        val typeLabel = if (league == League.FTC) {
                                            when (activeGamePieceType) {
                                                "Sample (Yellow)" -> "Yellow Sample"
                                                "Sample (Red)" -> "Red Sample"
                                                "Sample (Blue)" -> "Blue Sample"
                                                else -> "Specimen"
                                            }
                                        } else {
                                            "Note"
                                        }
                                        val newPiece = GamePiece(
                                            id = "piece_${System.currentTimeMillis()}",
                                            name = "$typeLabel ${currentActiveGamePieces.size + 1}",
                                            x = newWp.x,
                                            y = newWp.y,
                                            type = activeGamePieceType
                                        )
                                        currentUpdateGamePieces(currentActiveGamePieces + newPiece)
                                    }
                                    EditorMode.PLACE_APRILTAG -> {
                                        val newWp = getRobotCoordFromScreen(offset, w, h)
                                        val newTag = com.ares.analytics.shared.AprilTagPlacement(
                                            id = "apriltag_${System.currentTimeMillis()}",
                                            tagId = 11 + currentActiveAprilTags.size,
                                            x = newWp.x,
                                            y = newWp.y,
                                            z = 0.5,
                                            yawDegrees = 0.0
                                        )
                                        currentUpdateAprilTags(currentActiveAprilTags + newTag)
                                    }
                                    EditorMode.ERASER -> {
                                        var hitIdx = -1
                                        for (i in currentWaypoints.indices) {
                                            val wpOffset = getTransformedCanvasOffset(currentWaypoints[i], w, h)
                                            val dist = sqrt((offset.x - wpOffset.x).pow(2) + (offset.y - wpOffset.y).pow(2))
                                            if (dist < 25f) {
                                                hitIdx = i
                                                break
                                            }
                                        }
                                        if (hitIdx != -1) {
                                            currentOnWaypointsChanged(currentWaypoints.toMutableList().apply { removeAt(hitIdx) })
                                        } else {
                                            val robotWp = getRobotCoordFromScreen(offset, w, h)
                                            var closestPiece: GamePiece? = null
                                            var minPieceDist = Double.MAX_VALUE
                                            for (gp in currentActiveGamePieces) {
                                                val dist = sqrt((robotWp.x - gp.x).pow(2) + (robotWp.y - gp.y).pow(2))
                                                if (dist < minPieceDist) {
                                                    minPieceDist = dist
                                                    closestPiece = gp
                                                }
                                            }
                                            if (closestPiece != null && minPieceDist < 0.3) {
                                                currentUpdateGamePieces(currentActiveGamePieces.filter { it != closestPiece })
                                            } else {
                                                var closestObstacle: Obstacle? = null
                                                var minObstacleDist = Double.MAX_VALUE
                                                for (obs in currentActiveObstacles) {
                                                    val dist = when (obs) {
                                                        is Obstacle.Circle -> {
                                                            val d = sqrt((robotWp.x - obs.centerX).pow(2) + (robotWp.y - obs.centerY).pow(2))
                                                            (d - obs.radius).coerceAtLeast(0.0)
                                                        }
                                                        is Obstacle.Rectangle -> {
                                                            val d = sqrt((robotWp.x - obs.centerX).pow(2) + (robotWp.y - obs.centerY).pow(2))
                                                            (d - maxOf(obs.width, obs.height) / 2.0).coerceAtLeast(0.0)
                                                        }
                                                        is Obstacle.Polygon -> {
                                                            var minVertexDist = Double.MAX_VALUE
                                                            for (v in obs.vertices) {
                                                                val d = sqrt((robotWp.x - v.x).pow(2) + (robotWp.y - v.y).pow(2))
                                                                if (d < minVertexDist) minVertexDist = d
                                                            }
                                                            minVertexDist
                                                        }
                                                    }
                                                    if (dist < minObstacleDist) {
                                                        minObstacleDist = dist
                                                        closestObstacle = obs
                                                    }
                                                }
                                                if (closestObstacle != null && minObstacleDist < 0.5) {
                                                    currentUpdateObstacles(currentActiveObstacles.filter { it != closestObstacle })
                                                } else {
                                                    var closestTag: com.ares.analytics.shared.AprilTagPlacement? = null
                                                    var minTagDist = Double.MAX_VALUE
                                                    for (at in currentActiveAprilTags) {
                                                        val dist = sqrt((robotWp.x - at.x).pow(2) + (robotWp.y - at.y).pow(2))
                                                        if (dist < minTagDist) {
                                                            minTagDist = dist
                                                            closestTag = at
                                                        }
                                                    }
                                                    if (closestTag != null && minTagDist < 0.3) {
                                                        currentUpdateAprilTags(currentActiveAprilTags.filter { it != closestTag })
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                                    val offset = event.changes.first().position
                                    contextMenuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    
                                    // Hit test Waypoints
                                    var hitWpIdx = -1
                                    for (i in currentWaypoints.indices) {
                                        val wpOffset = getTransformedCanvasOffset(currentWaypoints[i], w, h)
                                        val dist = kotlin.math.sqrt((offset.x - wpOffset.x).pow(2) + (offset.y - wpOffset.y).pow(2))
                                        if (dist < 20.dp.toPx()) {
                                            hitWpIdx = i
                                            break
                                        }
                                    }
                                    if (hitWpIdx != -1) {
                                        contextTargetType = "Waypoint"
                                        contextTargetIndex = hitWpIdx
                                        contextMenuExpanded = true
                                        continue
                                    }
                                    
                                    // Hit test Event Markers
                                    var hitEventIdx = -1
                                    for (i in currentEventMarkers.indices) {
                                        val markerWp = getPositionOnSpline(currentEventMarkers[i].waypointRelativePos, currentWaypoints)
                                        val markerOffset = getCanvasOffsetBase(markerWp, w, h, fieldWidthM, fieldHeightM, league)
                                        val dist = kotlin.math.sqrt((offset.x - markerOffset.x).pow(2) + (offset.y - markerOffset.y).pow(2))
                                        if (dist < 15.dp.toPx()) {
                                            hitEventIdx = i
                                            break
                                        }
                                    }
                                    if (hitEventIdx != -1) {
                                        contextTargetType = "EventMarker"
                                        contextTargetIndex = hitEventIdx
                                        contextMenuExpanded = true
                                        continue
                                    }
                                    
                                    val clickCoord = getRobotCoordFromScreen(offset, w, h)
                                    
                                    // Hit test Obstacles
                                    var hitObsId: String? = null
                                    for (obs in currentActiveObstacles) {
                                        val isHit = when (obs) {
                                            is Obstacle.Circle -> {
                                                val dist = kotlin.math.sqrt((clickCoord.x - obs.centerX).pow(2) + (clickCoord.y - obs.centerY).pow(2))
                                                dist <= obs.radius
                                            }
                                            is Obstacle.Rectangle -> {
                                                val dx = clickCoord.x - obs.centerX
                                                val dy = clickCoord.y - obs.centerY
                                                val rad = Math.toRadians(-obs.rotation)
                                                val rx = dx * kotlin.math.cos(rad) - dy * kotlin.math.sin(rad)
                                                val ry = dx * kotlin.math.sin(rad) + dy * kotlin.math.cos(rad)
                                                kotlin.math.abs(rx) <= obs.width / 2.0 && kotlin.math.abs(ry) <= obs.height / 2.0
                                            }
                                            is Obstacle.Polygon -> {
                                                var nearVertex = false
                                                for (v in obs.vertices) {
                                                    val d = kotlin.math.sqrt((clickCoord.x - v.x).pow(2) + (clickCoord.y - v.y).pow(2))
                                                    if (d < 0.3) {
                                                        nearVertex = true
                                                        break
                                                    }
                                                }
                                                nearVertex
                                            }
                                        }
                                        if (isHit) {
                                            hitObsId = obs.id
                                            break
                                        }
                                    }
                                    if (hitObsId != null) {
                                        contextTargetType = "Obstacle"
                                        contextTargetId = hitObsId
                                        contextMenuExpanded = true
                                        continue
                                    }
                                    
                                    // Hit test AprilTags
                                    var hitAtId: String? = null
                                    for (at in currentActiveAprilTags) {
                                        val dist = kotlin.math.sqrt((clickCoord.x - at.x).pow(2) + (clickCoord.y - at.y).pow(2))
                                        if (dist < 0.3) {
                                            hitAtId = at.id
                                            break
                                        }
                                    }
                                    if (hitAtId != null) {
                                        contextTargetType = "AprilTag"
                                        contextTargetId = hitAtId
                                        contextMenuExpanded = true
                                        continue
                                    }
                                    
                                    // Hit test GamePieces
                                    var hitGpId: String? = null
                                    for (gp in currentActiveGamePieces) {
                                        val dist = kotlin.math.sqrt((clickCoord.x - gp.x).pow(2) + (clickCoord.y - gp.y).pow(2))
                                        if (dist < 0.2) {
                                            hitGpId = gp.id
                                            break
                                        }
                                    }
                                    if (hitGpId != null) {
                                        contextTargetType = "GamePiece"
                                        contextTargetId = hitGpId
                                        contextMenuExpanded = true
                                        continue
                                    }
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Render elements wrapped in pan & zoom matrix transform
                drawContext.canvas.save()
                drawContext.transform.translate(panOffset.x, panOffset.y)
                drawContext.transform.scale(zoomScale, zoomScale, pivot = Offset.Zero)

                // 1. Draw Field Boundary (Custom Image or fallback solid color)
                activeImage?.let { img ->
                    val cropL = (activeConfig.cropLeft * img.width).toInt().coerceIn(0, img.width)
                    val cropR = (activeConfig.cropRight * img.width).toInt().coerceIn(cropL, img.width)
                    val cropT = (activeConfig.cropTop * img.height).toInt().coerceIn(0, img.height)
                    val cropB = (activeConfig.cropBottom * img.height).toInt().coerceIn(cropT, img.height)
                    val srcW = maxOf(1, cropR - cropL)
                    val srcH = maxOf(1, cropB - cropT)

                    if (activeConfig.rotationDegrees != 0.0) {
                        drawContext.canvas.save()
                        drawContext.transform.rotate(activeConfig.rotationDegrees.toFloat(), Offset(w / 2f, h / 2f))
                        drawImage(
                            image = img,
                            srcOffset = androidx.compose.ui.unit.IntOffset(cropL, cropT),
                            srcSize = androidx.compose.ui.unit.IntSize(srcW, srcH),
                            dstOffset = androidx.compose.ui.unit.IntOffset(0, 0),
                            dstSize = androidx.compose.ui.unit.IntSize(w.toInt(), h.toInt())
                        )
                        drawContext.canvas.restore()
                    } else {
                        drawImage(
                            image = img,
                            srcOffset = androidx.compose.ui.unit.IntOffset(cropL, cropT),
                            srcSize = androidx.compose.ui.unit.IntSize(srcW, srcH),
                            dstOffset = androidx.compose.ui.unit.IntOffset(0, 0),
                            dstSize = androidx.compose.ui.unit.IntSize(w.toInt(), h.toInt())
                        )
                    }
                } ?: run {
                    drawRect(color = AresSurfaceElevated, size = size)
                }

                if (showHeatmap) {
                    HeatmapOverlay.drawHeatmap(
                        drawScope = this,
                        actualPath = actualPath,
                        fieldWidthM = fieldWidthM,
                        fieldHeightM = fieldHeightM,
                        league = league
                    )
                }
                
                val stepX = if (league == League.FTC) fieldWidthM / 6.0 else 1.0
                val stepY = if (league == League.FTC) fieldHeightM / 6.0 else 1.0
                
                var curX = if (league == League.FTC) -fieldWidthM/2 else 0.0
                // Add a small epsilon to avoid precision issues missing the last line
                while (curX <= fieldWidthM/2 + 0.001) {
                    val wp = Waypoint(curX, 0.0)
                    val offset = getCanvasOffsetBase(wp, w, h, fieldWidthM, fieldHeightM, league)
                    drawLine(color = AresBorder, start = Offset(offset.x, 0f), end = Offset(offset.x, h), strokeWidth = 1f)
                    curX += stepX
                }
                var curY = if (league == League.FTC) -fieldHeightM/2 else 0.0
                while (curY <= fieldHeightM/2 + 0.001) {
                    val wp = Waypoint(0.0, curY)
                    val offset = getCanvasOffsetBase(wp, w, h, fieldWidthM, fieldHeightM, league)
                    drawLine(color = AresBorder, start = Offset(0f, offset.y), end = Offset(w, offset.y), strokeWidth = 1f)
                    curY += stepY
                }
                // Field Outline
                drawRect(color = AresBorderFocused, style = Stroke(width = 3f))

                // FTC Alliance Station Overlays
                if (league == League.FTC) {
                    val strokeW = 12f
                    val redColor = AresRed.copy(alpha = 0.7f)
                    val blueColor = AresCyan.copy(alpha = 0.7f)
                    
                    when (activeConfig.ftcCoordinateSystem) {
                        com.ares.analytics.shared.FTCCoordinateSystem.SQUARE -> {
                            // Audience -X. Red -Y, Blue +Y
                            val redStart = getCanvasOffsetBase(Waypoint(-fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
                            val redEnd = getCanvasOffsetBase(Waypoint(fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
                            drawLine(redColor, start = redStart, end = redEnd, strokeWidth = strokeW)
                            
                            val blueStart = getCanvasOffsetBase(Waypoint(-fieldWidthM/2, fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
                            val blueEnd = getCanvasOffsetBase(Waypoint(fieldWidthM/2, fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
                            drawLine(blueColor, start = blueStart, end = blueEnd, strokeWidth = strokeW)
                        }
                        com.ares.analytics.shared.FTCCoordinateSystem.DIAMOND -> {
                            // Red -Y, Blue +X
                            // Red -Y
                            val redStart = getCanvasOffsetBase(Waypoint(-fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
                            val redEnd = getCanvasOffsetBase(Waypoint(fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
                            drawLine(redColor, start = redStart, end = redEnd, strokeWidth = strokeW)
                            
                            // Blue +X
                            val blueStart = getCanvasOffsetBase(Waypoint(fieldWidthM/2, -fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
                            val blueEnd = getCanvasOffsetBase(Waypoint(fieldWidthM/2, fieldHeightM/2), w, h, fieldWidthM, fieldHeightM, league)
                            drawLine(blueColor, start = blueStart, end = blueEnd, strokeWidth = strokeW)
                        }
                        null -> {} // Do nothing if no coordinate system is set
                    }
                    
                    // Draw Coordinate Axes at origin
                    drawCoordinateAxes(w, h, fieldWidthM, fieldHeightM, league, textMeasurer)
                }

                // 2. Render Custom Obstacles
                activeObstacles.forEach { obs ->
                    when (obs) {
                        is Obstacle.Circle -> {
                            val centerWp = Waypoint(obs.centerX, obs.centerY)
                            val centerOffset = getCanvasOffsetBase(centerWp, w, h, fieldWidthM, fieldHeightM, league)
                            val radiusPx = (obs.radius / fieldWidthM) * w
                            drawCircle(color = AresRed.copy(alpha = 0.3f), radius = radiusPx.toFloat(), center = centerOffset)
                            drawCircle(color = AresRed, radius = radiusPx.toFloat(), center = centerOffset, style = Stroke(width = 2f))
                        }
                        is Obstacle.Rectangle -> {
                            val centerWp = Waypoint(obs.centerX, obs.centerY)
                            val centerOffset = getCanvasOffsetBase(centerWp, w, h, fieldWidthM, fieldHeightM, league)
                            val rw = (obs.width / fieldWidthM) * w
                            val rh = (obs.height / fieldHeightM) * h
                            drawContext.canvas.save()
                            drawContext.transform.rotate(obs.rotation.toFloat(), pivot = centerOffset)
                            val rectOffset = Offset((centerOffset.x - rw / 2).toFloat(), (centerOffset.y - rh / 2).toFloat())
                            drawRect(color = AresRed.copy(alpha = 0.3f), topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
                            drawRect(color = AresRed, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()), style = Stroke(width = 2f))
                            drawContext.canvas.restore()
                        }
                        is Obstacle.Polygon -> {
                            if (obs.vertices.isNotEmpty()) {
                                val path = Path()
                                val start = getCanvasOffsetBase(Waypoint(obs.vertices.first().x, obs.vertices.first().y), w, h, fieldWidthM, fieldHeightM, league)
                                path.moveTo(start.x, start.y)
                                obs.vertices.drop(1).forEach { pt ->
                                    val offset = getCanvasOffsetBase(Waypoint(pt.x, pt.y), w, h, fieldWidthM, fieldHeightM, league)
                                    path.lineTo(offset.x, offset.y)
                                }
                                path.close()
                                drawPath(path = path, color = AresRed.copy(alpha = 0.3f))
                                drawPath(path = path, color = AresRed, style = Stroke(width = 2f))
                            }
                        }
                    }
                }

                // 2.5 Render Placed Game Pieces
                activeGamePieces.forEach { gp ->
                    val gpOffset = getCanvasOffsetBase(Waypoint(gp.x, gp.y), w, h, fieldWidthM, fieldHeightM, league)
                    when (gp.type) {
                        "Note", "High Note" -> {
                            val outerRadiusPx = (0.175 / fieldWidthM) * w
                            val innerRadiusPx = (0.07 / fieldWidthM) * w
                            drawCircle(color = AresAmber.copy(alpha = 0.4f), radius = outerRadiusPx.toFloat(), center = gpOffset)
                            drawCircle(color = AresAmber, radius = outerRadiusPx.toFloat(), center = gpOffset, style = Stroke(width = 3f))
                            drawCircle(color = AresBackground, radius = innerRadiusPx.toFloat(), center = gpOffset)
                            drawCircle(color = AresAmber, radius = innerRadiusPx.toFloat(), center = gpOffset, style = Stroke(width = 1.5f))
                        }
                        "Sample (Yellow)" -> {
                            val rw = (0.15 / fieldWidthM) * w
                            val rh = (0.05 / fieldHeightM) * h
                            val rectOffset = Offset((gpOffset.x - rw/2).toFloat(), (gpOffset.y - rh/2).toFloat())
                            drawRect(color = Color.Yellow.copy(alpha = 0.6f), topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
                            drawRect(color = Color.Yellow, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()), style = Stroke(width = 2f))
                        }
                        "Sample (Red)" -> {
                            val rw = (0.15 / fieldWidthM) * w
                            val rh = (0.05 / fieldHeightM) * h
                            val rectOffset = Offset((gpOffset.x - rw/2).toFloat(), (gpOffset.y - rh/2).toFloat())
                            drawRect(color = AresRed.copy(alpha = 0.6f), topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
                            drawRect(color = AresRed, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()), style = Stroke(width = 2f))
                        }
                        "Sample (Blue)" -> {
                            val rw = (0.15 / fieldWidthM) * w
                            val rh = (0.05 / fieldHeightM) * h
                            val rectOffset = Offset((gpOffset.x - rw/2).toFloat(), (gpOffset.y - rh/2).toFloat())
                            drawRect(color = AresCyan.copy(alpha = 0.6f), topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
                            drawRect(color = AresCyan, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()), style = Stroke(width = 2f))
                        }
                        else -> {
                            val sizePx = (0.12 / fieldWidthM) * w
                            val path = Path().apply {
                                moveTo(gpOffset.x, (gpOffset.y - sizePx).toFloat())
                                lineTo((gpOffset.x + sizePx).toFloat(), gpOffset.y)
                                lineTo(gpOffset.x, (gpOffset.y + sizePx).toFloat())
                                lineTo((gpOffset.x - sizePx).toFloat(), gpOffset.y)
                                close()
                            }
                            drawPath(path = path, color = Color(0xFF9C27B0).copy(alpha = 0.6f))
                            drawPath(path = path, color = Color(0xFF9C27B0), style = Stroke(width = 2f))
                        }
                    }
                }

                // 2.6 Render Placed AprilTags
                activeAprilTags.forEach { at ->
                    val atOffset = getCanvasOffsetBase(Waypoint(at.x, at.y), w, h, fieldWidthM, fieldHeightM, league)
                    val rw = (0.15 / fieldWidthM) * w
                    val rh = (0.15 / fieldHeightM) * h
                    
                    drawContext.canvas.save()
                    drawContext.transform.rotate(degrees = -at.yawDegrees.toFloat(), pivot = atOffset)
                    
                    val rectOffset = Offset((atOffset.x - rw/2).toFloat(), (atOffset.y - rh/2).toFloat())
                    
                    // White border
                    drawRect(color = Color.White, topLeft = rectOffset, size = Size(rw.toFloat(), rh.toFloat()))
                    // Black inner
                    val innerOffset = Offset((atOffset.x - rw/2 + 2).toFloat(), (atOffset.y - rh/2 + 2).toFloat())
                    drawRect(color = Color.Black, topLeft = innerOffset, size = Size((rw - 4).toFloat(), (rh - 4).toFloat()))
                    
                    drawContext.canvas.restore()
                }

                // Render current active drawing polygon points
                if (currentPolygonPoints.isNotEmpty()) {
                    val path = Path()
                    val start = getCanvasOffsetBase(Waypoint(currentPolygonPoints.first().x, currentPolygonPoints.first().y), w, h, fieldWidthM, fieldHeightM, league)
                    path.moveTo(start.x, start.y)
                    currentPolygonPoints.drop(1).forEach { pt ->
                        val offset = getCanvasOffsetBase(Waypoint(pt.x, pt.y), w, h, fieldWidthM, fieldHeightM, league)
                        path.lineTo(offset.x, offset.y)
                    }
                    drawPath(path = path, color = AresRed.copy(alpha = 0.15f))
                    drawPath(path = path, color = AresRed, style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))))
                }

                // 3. Planned Spline Path (Cyan) - Cached
                val splinePath = if (pathCache.splinePath != null && pathCache.splinePoints === splinePoints && pathCache.w == w && pathCache.h == h) {
                    pathCache.splinePath!!
                } else {
                    val path = Path()
                    if (splinePoints.isNotEmpty()) {
                        val firstOffset = getCanvasOffsetBase(splinePoints.first(), w, h, fieldWidthM, fieldHeightM, league)
                        path.moveTo(firstOffset.x, firstOffset.y)
                        for (i in 1 until splinePoints.size) {
                            val offset = getCanvasOffsetBase(splinePoints[i], w, h, fieldWidthM, fieldHeightM, league)
                            path.lineTo(offset.x, offset.y)
                        }
                    }
                    pathCache.splinePoints = splinePoints
                    pathCache.w = w
                    pathCache.h = h
                    pathCache.splinePath = path
                    path
                }
                if (waypoints.size >= 2) {
                    drawPath(path = splinePath, color = AresPathPlanned, style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
                }

                // 3.5 Render Event Markers on Spline
                if (waypoints.size >= 2) {
                    eventMarkerPoints.forEach { markerOffsetWp ->
                        val markerOffset = getCanvasOffsetBase(markerOffsetWp, w, h, fieldWidthM, fieldHeightM, league)
                        
                        // Visual glow / outer ring
                        drawCircle(
                            color = Color(0xFF9C27B0).copy(alpha = 0.4f),
                            radius = 8.dp.toPx(),
                            center = markerOffset
                        )
                        // Solid center dot
                        drawCircle(
                            color = Color(0xFF9C27B0),
                            radius = 5.dp.toPx(),
                            center = markerOffset
                        )
                        // Inner core
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = markerOffset
                        )
                    }
                }

                // 3.6 Render Constraint Zones on Spline - Cached
                val constraintPaths = if (pathCache.constraintPaths.isNotEmpty() && pathCache.constraintSplines == constraintZoneSplines && pathCache.w == w && pathCache.h == h) {
                    pathCache.constraintPaths
                } else {
                    val paths = constraintZoneSplines.map { zonePoints ->
                        val path = Path()
                        if (zonePoints.isNotEmpty()) {
                            val firstOffset = getCanvasOffsetBase(zonePoints.first(), w, h, fieldWidthM, fieldHeightM, league)
                            path.moveTo(firstOffset.x, firstOffset.y)
                            for (i in 1 until zonePoints.size) {
                                val offset = getCanvasOffsetBase(zonePoints[i], w, h, fieldWidthM, fieldHeightM, league)
                                path.lineTo(offset.x, offset.y)
                            }
                        }
                        path
                    }
                    pathCache.constraintSplines = constraintZoneSplines
                    pathCache.w = w
                    pathCache.h = h
                    pathCache.constraintPaths = paths
                    paths
                }
                if (waypoints.size >= 2) {
                    constraintPaths.forEach { zonePath ->
                        drawPath(
                            path = zonePath,
                            color = AresRed.copy(alpha = 0.4f),
                            style = Stroke(
                                width = 8.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                            )
                        )
                    }
                }

                // 3.7 Render Point Towards (Aiming) Zones
                if (waypoints.size >= 2) {
                    pointTowardsZoneRenderData.forEach { data ->
                        val targetOffset = getCanvasOffsetBase(data.target, w, h, fieldWidthM, fieldHeightM, league)
                        
                        // Draw aiming target icon
                        drawCircle(
                            color = AresCyan.copy(alpha = 0.3f),
                            radius = 12.dp.toPx(),
                            center = targetOffset
                        )
                        drawCircle(
                            color = AresCyan,
                            radius = 6.dp.toPx(),
                            center = targetOffset,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawLine(
                            color = AresCyan,
                            start = Offset(targetOffset.x - 10.dp.toPx(), targetOffset.y),
                            end = Offset(targetOffset.x + 10.dp.toPx(), targetOffset.y),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawLine(
                            color = AresCyan,
                            start = Offset(targetOffset.x, targetOffset.y - 10.dp.toPx()),
                            end = Offset(targetOffset.x, targetOffset.y + 10.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        
                        // Draw dashed lines from spline points inside the zone to the target
                        data.splinePoints.forEach { wp ->
                            val splineOffset = getCanvasOffsetBase(wp, w, h, fieldWidthM, fieldHeightM, league)
                            drawLine(
                                color = AresCyan.copy(alpha = 0.3f),
                                start = splineOffset,
                                end = targetOffset,
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                            )
                        }
                    }
                }

                // 3.8 Render Holonomic Rotation Targets
                if (waypoints.size >= 2) {
                    rotationTargetPoints.forEachIndexed { idx, targetWp ->
                        val target = rotationTargets[idx]
                        val offset = getCanvasOffsetBase(targetWp, w, h, fieldWidthM, fieldHeightM, league)
                        
                        // Draw an oriented robot rectangular box representing holonomic rotation
                        val rectSize = 24.dp.toPx()
                        drawContext.canvas.save()
                        drawContext.transform.rotate(degrees = -target.rotationDegrees.toFloat(), pivot = offset)
                        
                        // Shaded box
                        drawRect(
                            color = AresAmber.copy(alpha = 0.25f),
                            topLeft = Offset(offset.x - rectSize/2, offset.y - rectSize/2),
                            size = Size(rectSize, rectSize)
                        )
                        // Box border
                        drawRect(
                            color = AresAmber,
                            topLeft = Offset(offset.x - rectSize/2, offset.y - rectSize/2),
                            size = Size(rectSize, rectSize),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        // Cyan front bumper highlight line to show robot orientation direction
                        drawLine(
                            color = AresCyan,
                            start = Offset(offset.x + rectSize/2, offset.y - rectSize/2),
                            end = Offset(offset.x + rectSize/2, offset.y + rectSize/2),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawContext.canvas.restore()
                        
                        // Draw center rotation indicator dot
                        drawCircle(
                            color = AresAmber,
                            radius = 4.dp.toPx(),
                            center = offset
                        )
                    }
                }

                // 4. Actual Path (Gold) - Cached
                val actualPathObj = if (pathCache.actualPath != null &&
                    pathCache.actualPoints.size == actualPath.size &&
                    pathCache.actualPoints.firstOrNull() == actualPath.firstOrNull() &&
                    pathCache.actualPoints.lastOrNull() == actualPath.lastOrNull() &&
                    pathCache.w == w && pathCache.h == h) {
                    pathCache.actualPath!!
                } else {
                    val path = Path()
                    if (actualPath.isNotEmpty()) {
                        val firstOffset = getCanvasOffsetBase(actualPath.first(), w, h, fieldWidthM, fieldHeightM, league)
                        path.moveTo(firstOffset.x, firstOffset.y)
                        for (i in 1 until actualPath.size) {
                            val offset = getCanvasOffsetBase(actualPath[i], w, h, fieldWidthM, fieldHeightM, league)
                            path.lineTo(offset.x, offset.y)
                        }
                    }
                    pathCache.actualPoints = actualPath
                    pathCache.w = w
                    pathCache.h = h
                    pathCache.actualPath = path
                    path
                }
                if (actualPath.size >= 2) {
                    drawPath(path = actualPathObj, color = AresPathActual, style = Stroke(width = 3f))

                    // 5. Draw Path Deviation Vectors (Planned vs Actual)
                    if (waypoints.size >= 2) {
                        actualPath.chunked(maxOf(1, actualPath.size / 20)).forEach { actualPoint ->
                            val actualWp = actualPoint.firstOrNull() ?: return@forEach
                            val actualOffset = getCanvasOffsetBase(actualWp, w, h, fieldWidthM, fieldHeightM, league)
                            
                            // Find closest planned point
                            var closestWp = waypoints.first()
                            var minDistance = Double.MAX_VALUE
                            waypoints.forEach { plannedWp ->
                                val dist = sqrt((actualWp.x - plannedWp.x).pow(2) + (actualWp.y - plannedWp.y).pow(2))
                                if (dist < minDistance) {
                                    minDistance = dist
                                    closestWp = plannedWp
                                }
                            }

                            val plannedOffset = getCanvasOffsetBase(closestWp, w, h, fieldWidthM, fieldHeightM, league)
                            val deviationM = minDistance
                            val deviationColor = when {
                                deviationM < 0.02 -> AresGreen // <2cm
                                deviationM < 0.05 -> AresAmber // <5cm
                                else -> AresRed               // >5cm
                            }
                            drawLine(color = deviationColor, start = actualOffset, end = plannedOffset, strokeWidth = 1.5f)
                        }
                    }
                }

                // 5b. Active Robot Representation (drawn at the latest point of actualPath)
                val activeRobotWp = actualPath.lastOrNull()
                if (activeRobotWp != null) {
                    val robotOffset = getCanvasOffsetBase(activeRobotWp, w, h, fieldWidthM, fieldHeightM, league)
                    val robotSizeM = 0.45
                    val robotSizePx = ((robotSizeM / fieldWidthM) * w).toFloat()
                    
                    drawContext.canvas.save()
                    drawContext.transform.rotate(degrees = -Math.toDegrees(activeRobotWp.headingRad).toFloat(), pivot = robotOffset)
                    drawRect(
                        color = AresCyan.copy(alpha = 0.2f),
                        topLeft = Offset(robotOffset.x - robotSizePx / 2, robotOffset.y - robotSizePx / 2),
                        size = Size(robotSizePx, robotSizePx)
                    )
                    drawRect(
                        color = AresCyan,
                        topLeft = Offset(robotOffset.x - robotSizePx / 2, robotOffset.y - robotSizePx / 2),
                        size = Size(robotSizePx, robotSizePx),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawLine(
                        color = AresAmber,
                        start = Offset(robotOffset.x + robotSizePx / 2, robotOffset.y - robotSizePx / 2),
                        end = Offset(robotOffset.x + robotSizePx / 2, robotOffset.y + robotSizePx / 2),
                        strokeWidth = 3.dp.toPx()
                    )
                    drawContext.canvas.restore()
                }

                // 5c. EKF Estimated Robot Representation (Dashed Amber Box)
                if (estimatedPose != null) {
                    val robotOffset = getCanvasOffsetBase(estimatedPose, w, h, fieldWidthM, fieldHeightM, league)
                    val robotSizeM = 0.45
                    val robotSizePx = ((robotSizeM / fieldWidthM) * w).toFloat()
                    
                    drawContext.canvas.save()
                    drawContext.transform.rotate(degrees = -Math.toDegrees(estimatedPose.headingRad).toFloat(), pivot = robotOffset)
                    drawRect(
                        color = AresAmber.copy(alpha = 0.15f),
                        topLeft = Offset(robotOffset.x - robotSizePx / 2, robotOffset.y - robotSizePx / 2),
                        size = Size(robotSizePx, robotSizePx)
                    )
                    drawRect(
                        color = AresAmber,
                        topLeft = Offset(robotOffset.x - robotSizePx / 2, robotOffset.y - robotSizePx / 2),
                        size = Size(robotSizePx, robotSizePx),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                    drawLine(
                        color = AresAmber,
                        start = Offset(robotOffset.x + robotSizePx / 2, robotOffset.y - robotSizePx / 2),
                        end = Offset(robotOffset.x + robotSizePx / 2, robotOffset.y + robotSizePx / 2),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawContext.canvas.restore()
                }

                // 5d. Limelight Vision Robot Representations (Dotted Green Boxes for N cameras)
                visionPoses.forEach { pose ->
                    val robotOffset = getCanvasOffsetBase(pose, w, h, fieldWidthM, fieldHeightM, league)
                    val robotSizeM = 0.45
                    val robotSizePx = ((robotSizeM / fieldWidthM) * w).toFloat()
                    
                    drawContext.canvas.save()
                    drawContext.transform.rotate(degrees = -Math.toDegrees(pose.headingRad).toFloat(), pivot = robotOffset)
                    drawRect(
                        color = AresGreen.copy(alpha = 0.15f),
                        topLeft = Offset(robotOffset.x - robotSizePx / 2, robotOffset.y - robotSizePx / 2),
                        size = Size(robotSizePx, robotSizePx)
                    )
                    drawRect(
                        color = AresGreen,
                        topLeft = Offset(robotOffset.x - robotSizePx / 2, robotOffset.y - robotSizePx / 2),
                        size = Size(robotSizePx, robotSizePx),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                    )
                    drawLine(
                        color = AresGreen,
                        start = Offset(robotOffset.x + robotSizePx / 2, robotOffset.y - robotSizePx / 2),
                        end = Offset(robotOffset.x + robotSizePx / 2, robotOffset.y + robotSizePx / 2),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawContext.canvas.restore()
                }

                // 6. Draw Waypoint Circles
                waypoints.forEachIndexed { idx, wp ->
                    val offset = getCanvasOffsetBase(wp, w, h, fieldWidthM, fieldHeightM, league)
                    val isSelected = idx == selectedWaypointIndex
                    val color = if (isSelected) AresCyan else AresTextPrimary

                    drawCircle(color = color, radius = 8.dp.toPx(), center = offset)
                    drawCircle(color = AresBackground, radius = 4.dp.toPx(), center = offset)

                    val handleMeters = Waypoint(wp.x + wp.tangentMagnitude * kotlin.math.cos(wp.headingRad), wp.y + wp.tangentMagnitude * kotlin.math.sin(wp.headingRad))
                    val arrowEnd = getCanvasOffsetBase(handleMeters, w, h, fieldWidthM, fieldHeightM, league)
                    drawLine(color = color.copy(alpha = 0.8f), start = offset, end = arrowEnd, strokeWidth = 2.dp.toPx())

                    // Draw heading handle dot
                    val handleColor = if (isSelected && isDraggingHeading) AresCyan else AresAmber
                    drawCircle(color = handleColor, radius = 6.dp.toPx(), center = arrowEnd)
                    drawCircle(color = AresBackground, radius = 3.dp.toPx(), center = arrowEnd)
                }

                drawContext.canvas.restore()
            }

            DropdownMenu(
                expanded = contextMenuExpanded,
                onDismissRequest = { contextMenuExpanded = false },
                offset = contextMenuOffset
            ) {
                if (contextTargetType == "Waypoint" && contextTargetIndex >= 0 && contextTargetIndex < waypoints.size) {
                    androidx.compose.material.DropdownMenuItem(onClick = {
                        val wp = waypoints[contextTargetIndex]
                        // We could open a dialog to edit it manually
                        contextMenuExpanded = false
                    }) { androidx.compose.material.Text("Edit Waypoint...") }
                    
                    androidx.compose.material.DropdownMenuItem(onClick = {
                        val updated = waypoints.toMutableList().apply { removeAt(contextTargetIndex) }
                        onWaypointsChanged?.invoke(updated)
                        contextMenuExpanded = false
                        selectedWaypointIndex = -1
                    }) { androidx.compose.material.Text("Delete Waypoint") }
                } else if (contextTargetType == "Obstacle" && contextTargetId != null) {
                    androidx.compose.material.DropdownMenuItem(onClick = {
                        val updated = currentActiveObstacles.filter { it.id != contextTargetId }
                        currentUpdateObstacles(updated)
                        contextMenuExpanded = false
                        selectedObstacleId = null
                    }) { androidx.compose.material.Text("Delete Obstacle") }
                } else if (contextTargetType == "AprilTag" && contextTargetId != null) {
                    androidx.compose.material.DropdownMenuItem(onClick = {
                        val updated = currentActiveAprilTags.filter { it.id != contextTargetId }
                        currentUpdateAprilTags(updated)
                        contextMenuExpanded = false
                        selectedAprilTagId = null
                    }) { androidx.compose.material.Text("Delete AprilTag") }
                } else if (contextTargetType == "GamePiece" && contextTargetId != null) {
                    val gp = currentActiveGamePieces.find { it.id == contextTargetId }
                    if (gp != null) {
                        androidx.compose.material.DropdownMenuItem(onClick = {
                            val nextType = if (gp.type == "Sample") "Specimen" else "Sample"
                            val updated = currentActiveGamePieces.map { if (it.id == contextTargetId) it.copy(type = nextType) else it }
                            currentUpdateGamePieces(updated)
                            contextMenuExpanded = false
                        }) { androidx.compose.material.Text("Toggle Type: ${if (gp.type == "Sample") "Specimen" else "Sample"}") }
                    }
                    
                    androidx.compose.material.DropdownMenuItem(onClick = {
                        val updated = currentActiveGamePieces.filter { it.id != contextTargetId }
                        currentUpdateGamePieces(updated)
                        contextMenuExpanded = false
                        selectedGamePieceId = null
                    }) { androidx.compose.material.Text("Delete Game Piece") }
                } else if (contextTargetType == "EventMarker" && contextTargetIndex >= 0 && contextTargetIndex < currentEventMarkers.size) {
                    androidx.compose.material.DropdownMenuItem(onClick = {
                        val updated = currentEventMarkers.toMutableList().apply { removeAt(contextTargetIndex) }
                        onEventMarkersChanged?.invoke(updated)
                        contextMenuExpanded = false
                        selectedEventMarkerIndex = -1
                    }) { androidx.compose.material.Text("Delete Event Marker") }
                } else {
                    androidx.compose.material.DropdownMenuItem(onClick = {
                        contextMenuExpanded = false
                    }) { androidx.compose.material.Text("Cancel") }
                }
            }

            // Floating Export Panel (Bottom Right)
            if (showPathControls && !projectPath.isNullOrEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .width(260.dp)
                        .border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = AresSurfaceElevated.copy(alpha = 0.9f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Export Autonomous Path", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                        
                        OutlinedTextField(
                            value = pathName,
                            onValueChange = { pathName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder)
                        )

                        Button(
                            onClick = {
                                try {
                                    val json = Json { prettyPrint = true }
                                    val pathData = waypoints.map { PathPoint(it.x, it.y) }
                                    val serialized = json.encodeToString(pathData)

                                    val relativeDir = if (league == League.FTC) "src/main/assets/paths" else "src/main/deploy/paths"
                                    val targetDir = File(projectPath, relativeDir)
                                    targetDir.mkdirs()
                                    val targetFile = File(targetDir, "$pathName.json")
                                    targetFile.writeText(serialized)
                                    saveStatus = "Path exported to ${targetFile.name}!"
                                } catch (e: Exception) {
                                    saveStatus = "Export failed: ${e.message}"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AresCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export Path JSON", color = AresBackground, fontWeight = FontWeight.Bold)
                        }

                        if (saveStatus.isNotEmpty()) {
                            Text(saveStatus, color = if (saveStatus.contains("failed")) AresError else AresGreen, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

// Convert coordinates base models
private fun getCanvasOffsetBase(
    wp: Waypoint,
    canvasW: Float,
    canvasH: Float,
    fieldW: Double,
    fieldH: Double,
    league: League
): Offset {
    return if (league == League.FTC) {
        val x = (wp.x / fieldW + 0.5) * canvasW
        val y = (-wp.y / fieldH + 0.5) * canvasH
        Offset(x.toFloat(), y.toFloat())
    } else {
        val x = (wp.x / fieldW) * canvasW
        val y = ((fieldH - wp.y) / fieldH) * canvasH
        Offset(x.toFloat(), y.toFloat())
    }
}

private fun getRobotCoordBase(
    offset: Offset,
    canvasW: Float,
    canvasH: Float,
    fieldW: Double,
    fieldH: Double,
    league: League
): Waypoint {
    return if (league == League.FTC) {
        val rx = (offset.x / canvasW - 0.5) * fieldW
        val ry = -(offset.y / canvasH - 0.5) * fieldH
        Waypoint(rx, ry)
    } else {
        val rx = (offset.x / canvasW) * fieldW
        val ry = fieldH - (offset.y / canvasH) * fieldH
        Waypoint(rx, ry)
    }
}

private fun cubicHermite(p0: Double, v0: Double, p1: Double, v1: Double, t: Double): Double {
    val t2 = t * t
    val t3 = t2 * t
    val h00 = 2 * t3 - 3 * t2 + 1
    val h10 = t3 - 2 * t2 + t
    val h01 = -2 * t3 + 3 * t2
    val h11 = t3 - t2
    return h00 * p0 + h10 * v0 + h01 * p1 + h11 * v1
}

private fun getPositionOnSpline(pos: Double, waypoints: List<Waypoint>): Waypoint {
    if (waypoints.isEmpty()) return Waypoint(0.0, 0.0)
    if (waypoints.size == 1) return waypoints.first()
    val maxIdx = waypoints.size - 1
    val i = pos.toInt().coerceIn(0, maxIdx - 1)
    val t = (pos - i).coerceIn(0.0, 1.0)
    
    val p0 = waypoints[i]
    val p1 = waypoints[i + 1]
    
    val v0x = kotlin.math.cos(p0.headingRad) * p0.tangentMagnitude
    val v0y = kotlin.math.sin(p0.headingRad) * p0.tangentMagnitude
    val v1x = kotlin.math.cos(p1.headingRad) * p1.tangentMagnitude
    val v1y = kotlin.math.sin(p1.headingRad) * p1.tangentMagnitude
    
    val px = cubicHermite(p0.x, v0x, p1.x, v1x, t)
    val py = cubicHermite(p0.y, v0y, p1.y, v1y, t)
    return Waypoint(px, py)
}

private fun getClosestSplinePosition(
    mouseOffset: Offset,
    waypoints: List<Waypoint>,
    canvasW: Float,
    canvasH: Float,
    fieldW: Double,
    fieldH: Double,
    league: League
): Double {
    if (waypoints.size < 2) return 0.0
    var bestPos = 0.0
    var minDist = Float.MAX_VALUE
    val maxPos = (waypoints.size - 1).toDouble()
    
    // Sample the spline densely to find closest projection
    val steps = (waypoints.size - 1) * 40
    for (k in 0..steps) {
        val pos = maxPos * k / steps
        val wp = getPositionOnSpline(pos, waypoints)
        val offset = getCanvasOffsetBase(wp, canvasW, canvasH, fieldW, fieldH, league)
        val dist = kotlin.math.sqrt((mouseOffset.x - offset.x).pow(2) + (mouseOffset.y - offset.y).pow(2))
        if (dist < minDist) {
            minDist = dist
            bestPos = pos
        }
    }
    return bestPos
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCoordinateAxes(
    canvasW: Float,
    canvasH: Float,
    fieldW: Double,
    fieldH: Double,
    league: League,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val originOffset = getCanvasOffsetBase(Waypoint(0.0, 0.0), canvasW, canvasH, fieldW, fieldH, league)
    val xAxisOffset = getCanvasOffsetBase(Waypoint(0.8, 0.0), canvasW, canvasH, fieldW, fieldH, league)
    val yAxisOffset = getCanvasOffsetBase(Waypoint(0.0, 0.8), canvasW, canvasH, fieldW, fieldH, league)

    val strokeW = 3.dp.toPx()
    val arrowSize = 10.dp.toPx()

    // X Axis (Red)
    drawLine(color = AresRed, start = originOffset, end = xAxisOffset, strokeWidth = strokeW)
    // X Axis Arrow Head
    val xAngle = kotlin.math.atan2((xAxisOffset.y - originOffset.y).toDouble(), (xAxisOffset.x - originOffset.x).toDouble())
    val xArrowPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(xAxisOffset.x, xAxisOffset.y)
        lineTo(
            xAxisOffset.x - arrowSize * kotlin.math.cos(xAngle - Math.PI / 6).toFloat(),
            xAxisOffset.y - arrowSize * kotlin.math.sin(xAngle - Math.PI / 6).toFloat()
        )
        lineTo(
            xAxisOffset.x - arrowSize * kotlin.math.cos(xAngle + Math.PI / 6).toFloat(),
            xAxisOffset.y - arrowSize * kotlin.math.sin(xAngle + Math.PI / 6).toFloat()
        )
        close()
    }
    drawPath(path = xArrowPath, color = AresRed)

    // Y Axis (Green)
    drawLine(color = AresGreen, start = originOffset, end = yAxisOffset, strokeWidth = strokeW)
    // Y Axis Arrow Head
    val yAngle = kotlin.math.atan2((yAxisOffset.y - originOffset.y).toDouble(), (yAxisOffset.x - originOffset.x).toDouble())
    val yArrowPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(yAxisOffset.x, yAxisOffset.y)
        lineTo(
            yAxisOffset.x - arrowSize * kotlin.math.cos(yAngle - Math.PI / 6).toFloat(),
            yAxisOffset.y - arrowSize * kotlin.math.sin(yAngle - Math.PI / 6).toFloat()
        )
        lineTo(
            yAxisOffset.x - arrowSize * kotlin.math.cos(yAngle + Math.PI / 6).toFloat(),
            yAxisOffset.y - arrowSize * kotlin.math.sin(yAngle + Math.PI / 6).toFloat()
        )
        close()
    }
    drawPath(path = yArrowPath, color = AresGreen)

    // Axis Labels
    val textStyle = androidx.compose.ui.text.TextStyle(
        color = androidx.compose.ui.graphics.Color.White,
        fontSize = 12.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    )
    val textLayoutX = textMeasurer.measure("X", textStyle)
    val textLayoutY = textMeasurer.measure("Y", textStyle)

    drawText(
        textMeasurer = textMeasurer,
        text = "X",
        style = textStyle,
        topLeft = androidx.compose.ui.geometry.Offset(
            xAxisOffset.x + arrowSize * kotlin.math.cos(xAngle).toFloat(),
            xAxisOffset.y + arrowSize * kotlin.math.sin(xAngle).toFloat() - textLayoutX.size.height / 2f
        )
    )

    drawText(
        textMeasurer = textMeasurer,
        text = "Y",
        style = textStyle,
        topLeft = androidx.compose.ui.geometry.Offset(
            yAxisOffset.x + arrowSize * kotlin.math.cos(yAngle).toFloat() - textLayoutY.size.width / 2f,
            yAxisOffset.y + arrowSize * kotlin.math.sin(yAngle).toFloat()
        )
    )
}
