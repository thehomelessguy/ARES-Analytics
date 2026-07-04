package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.shared.League
import com.ares.analytics.ui.components.pathplanner.FieldCanvas
import com.ares.analytics.ui.components.pathplanner.Waypoint
import com.ares.analytics.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun FieldViewerCard(
    nt4ClientService: Nt4ClientService,
    league: League,
    projectPath: String? = null,
    properties: Map<String, String> = emptyMap(),
    onPropertiesChanged: (Map<String, String>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var robotX by remember { mutableStateOf(0.0) }
    var robotY by remember { mutableStateOf(0.0) }
    var robotHeading by remember { mutableStateOf(0.0) }

    val poseHistory = remember { mutableStateListOf<Waypoint>() }

    var ekfX by remember { mutableStateOf<Double?>(null) }
    var ekfY by remember { mutableStateOf<Double?>(null) }
    var ekfHeading by remember { mutableStateOf<Double?>(null) }

    var visionX by remember { mutableStateOf<Double?>(null) }
    var visionY by remember { mutableStateOf<Double?>(null) }
    var visionHeading by remember { mutableStateOf<Double?>(null) }

    val visionPoses = remember { mutableStateMapOf<Int, Double>() }

    val isConnected by nt4ClientService.isConnected.collectAsState()

    LaunchedEffect(Unit) {
        scope.launch {
            nt4ClientService.telemetryFlow.collect { frame ->
                val key = frame.key
                val value = frame.value
                when (key) {
                    "AdvantageKit/RealOutputs/ARES/EstimatedPose/0" -> robotX = value
                    "AdvantageKit/RealOutputs/ARES/EstimatedPose/1" -> robotY = value
                    "AdvantageKit/RealOutputs/ARES/EstimatedPose/2" -> robotHeading = value
                    "Drive/Pose_X" -> robotX = value
                    "Drive/Pose_Y" -> robotY = value
                    "Drive/Pose_Heading", "Drive/Drive_Heading" -> robotHeading = value

                    "Drive/Odom_X", "pinpoint_x", "pinpoint/x" -> ekfX = value
                    "Drive/Odom_Y", "pinpoint_y", "pinpoint/y" -> ekfY = value
                    "Drive/Odom_Heading", "pinpoint_heading", "pinpoint/heading" -> ekfHeading = value

                    "Vision/Pose_X", "Vision/Pose/X" -> visionX = value
                    "Vision/Pose_Y", "Vision/Pose/Y" -> visionY = value
                    "Vision/Pose_Heading", "Vision/Pose/Heading" -> visionHeading = value
                }

                if (key.startsWith("AdvantageScope/VisionPose/")) {
                    val idx = key.substringAfterLast("/").toIntOrNull()
                    if (idx != null) {
                        visionPoses[idx] = value
                    }
                }
            }
        }
    }

    LaunchedEffect(robotX, robotY, robotHeading) {
        if (robotX != 0.0 || robotY != 0.0) {
            val newWp = Waypoint(robotX, robotY, robotHeading)
            val lastWp = poseHistory.lastOrNull()
            if (lastWp == null || kotlin.math.abs(lastWp.x - newWp.x) > 0.01 || kotlin.math.abs(lastWp.y - newWp.y) > 0.01) {
                poseHistory.add(newWp)
                // Limit history to prevent excessive memory usage
                if (poseHistory.size > 2000) {
                    poseHistory.removeRange(0, 500)
                }
            }
        }
    }

    val estimatedPose = if (ekfX != null && ekfY != null && ekfHeading != null) {
        Waypoint(ekfX!!, ekfY!!, ekfHeading!!)
    } else null

    val activeVisionPoses = remember(visionPoses.size, visionX, visionY, visionHeading) {
        val list = mutableListOf<Waypoint>()
        val maxIndex = visionPoses.keys.maxOrNull() ?: -1
        for (i in 0..maxIndex step 3) {
            val vx = visionPoses[i]
            val vy = visionPoses[i + 1]
            val vh = visionPoses[i + 2]
            if (vx != null && vy != null && vh != null) {
                list.add(Waypoint(vx, vy, vh))
            }
        }
        if (list.isEmpty() && visionX != null && visionY != null && visionHeading != null) {
            list.add(Waypoint(visionX!!, visionY!!, visionHeading!!))
        }
        list
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(AresSurface)
            .border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = AresSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    "Field 2D Live Tracker",
                    color = AresTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    if (isConnected) "Connected" else "Offline",
                    color = if (isConnected) AresGreen else AresTextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = AresBorder)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                FieldCanvas(
                    league = league,
                    waypoints = emptyList(),
                    actualPath = poseHistory,
                    onWaypointsChanged = {},
                    projectPath = projectPath,
                    estimatedPose = estimatedPose,
                    visionPoses = activeVisionPoses,
                    showPathControls = false,
                    showObstacleControls = false,
                    initialViewRotation = properties["rotation"]?.toFloatOrNull() ?: 0f,
                    onViewRotationChanged = { newRot -> onPropertiesChanged(properties + ("rotation" to newRot.toString())) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
