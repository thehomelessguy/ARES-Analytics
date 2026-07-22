package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.ui.theme.*

@Composable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun PoseViewerCard(
    nt4ClientService: Nt4ClientService,
    modifier: Modifier = Modifier
) {

    /**
     * trueXSim val.
     */
    val trueXSim by nt4ClientService.subscribeDouble("ARES/EstimatedPose/0").collectAsState(initial = null)
    /**
     * trueYSim val.
     */
    val trueYSim by nt4ClientService.subscribeDouble("ARES/EstimatedPose/1").collectAsState(initial = null)
    /**
     * trueHeadingSim val.
     */
    val trueHeadingSim by nt4ClientService.subscribeDouble("ARES/EstimatedPose/2").collectAsState(initial = null)

    /**
     * ekfX val.
     */
    val ekfX by nt4ClientService.subscribeDouble("Drive/Pose_X").collectAsState(initial = null)
    /**
     * ekfY val.
     */
    val ekfY by nt4ClientService.subscribeDouble("Drive/Pose_Y").collectAsState(initial = null)
    /**
     * ekfHeading val.
     */
    val ekfHeading by nt4ClientService.subscribeDouble("Drive/Drive_Heading").collectAsState(initial = null)
    
    /**
     * trueX val.
     */
    val trueX = trueXSim ?: ekfX
    /**
     * trueY val.
     */
    val trueY = trueYSim ?: ekfY
    /**
     * trueHeading val.
     */
    val trueHeading = trueHeadingSim ?: ekfHeading

    /**
     * pinpointX val.
     */
    val pinpointX by nt4ClientService.subscribeDouble("Drive/Odom_X").collectAsState(initial = null)
    /**
     * pinpointY val.
     */
    val pinpointY by nt4ClientService.subscribeDouble("Drive/Odom_Y").collectAsState(initial = null)
    /**
     * pinpointHeading val.
     */
    val pinpointHeading by nt4ClientService.subscribeDouble("Drive/Odom_Heading").collectAsState(initial = null)

    /**
     * visionX val.
     */
    val visionX by nt4ClientService.subscribeDouble("Vision/Pose_X").collectAsState(initial = null)
    /**
     * visionY val.
     */
    val visionY by nt4ClientService.subscribeDouble("Vision/Pose_Y").collectAsState(initial = null)
    /**
     * visionHeading val.
     */
    val visionHeading by nt4ClientService.subscribeDouble("Vision/Pose_Heading").collectAsState(initial = null)

    /**
     * lastUpdateMs var.
     */
    var lastUpdateMs by remember { mutableStateOf<Long?>(null) }
    
    // Simple way to track last update:
    LaunchedEffect(trueX, ekfX, pinpointX) {
        if (trueX != null || ekfX != null || pinpointX != null) {
            lastUpdateMs = System.currentTimeMillis()
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AresSurface)
            .border(1.dp, AresBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = AresCyan
                )
                Text(
                    "Robot Pose Telemetry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AresTextPrimary
                )
            }
            
            // Connection/Update indicator
            /**
             * elapsed val.
             */
            val elapsed = lastUpdateMs?.let { System.currentTimeMillis() - it }
            val (statusText, statusColor) = when {
                elapsed == null -> "No Data" to AresTextTertiary
                elapsed < 500 -> "Active" to AresGreen
                elapsed < 2000 -> "Stale" to AresAmber
                else -> "Offline" to AresError
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor)
                )
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        HorizontalDivider(color = AresBorder, thickness = 1.dp)

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            PoseRow("True (Actual)", trueX, trueY, trueHeading, AresCyan)
            HorizontalDivider(color = AresBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
            PoseRow("Estimated (EKF)", ekfX, ekfY, ekfHeading, AresAmber)
            HorizontalDivider(color = AresBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
            PoseRow("Pinpoint (Odom)", pinpointX, pinpointY, pinpointHeading, AresGreen)
            HorizontalDivider(color = AresBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
            PoseRow("Vision (Limelight)", visionX, visionY, visionHeading, AresGold)
        }
    }
}

@Composable
private fun PoseRow(
    title: String,
    x: Double?,
    y: Double?,
    heading: Double?,
    color: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AresTextSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PoseValueCard("X", x, "m", color, Modifier.weight(1f))
            PoseValueCard("Y", y, "m", color, Modifier.weight(1f))
            PoseValueCard("Heading", heading?.let { Math.toDegrees(it) }, "°", color, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PoseValueCard(
    label: String,
    value: Double?,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AresSurfaceElevated)
            .border(0.5.dp, AresBorder, RoundedCornerShape(6.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                color = AresTextTertiary,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = value?.let { String.format("%.3f", it) } ?: "---",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (value != null) color else AresTextTertiary,
                    fontFamily = FontFamily.Monospace
                )
                if (value != null) {
                    Text(
                        text = unit,
                        fontSize = 9.sp,
                        color = AresTextTertiary,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

