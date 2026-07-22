package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.ui.theme.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun SwerveModuleVisualizer(
    nt4ClientService: Nt4ClientService,
    modifier: Modifier = Modifier
) {
    /**
     * scope val.
     */
    val scope = rememberCoroutineScope()

    // 0: FL, 1: FR, 2: BL, 3: BR
    /**
     * speedsTarget val.
     */
    val speedsTarget = remember { mutableStateListOf(0.0, 0.0, 0.0, 0.0) }
    /**
     * anglesTarget val.
     */
    val anglesTarget = remember { mutableStateListOf(0.0, 0.0, 0.0, 0.0) }
    /**
     * speedsActual val.
     */
    val speedsActual = remember { mutableStateListOf(0.0, 0.0, 0.0, 0.0) }
    /**
     * anglesActual val.
     */
    val anglesActual = remember { mutableStateListOf(0.0, 0.0, 0.0, 0.0) }

    LaunchedEffect(Unit) {
        scope.launch {
            nt4ClientService.telemetryFlow.collect { frame ->
                /**
                 * key val.
                 */
                val key = frame.key
                /**
                 * value val.
                 */
                val value = frame.value
                when {
                    key.startsWith("Swerve/ModuleSpeedsTarget/") -> {
                        /**
                         * idx val.
                         */
                        val idx = key.substringAfterLast("/").toIntOrNull()
                        if (idx != null && idx in 0..3) speedsTarget[idx] = value
                    }
                    key.startsWith("Swerve/ModuleAnglesTarget/") -> {
                        /**
                         * idx val.
                         */
                        val idx = key.substringAfterLast("/").toIntOrNull()
                        if (idx != null && idx in 0..3) anglesTarget[idx] = value
                    }
                    key.startsWith("Swerve/ModuleSpeedsActual/") -> {
                        /**
                         * idx val.
                         */
                        val idx = key.substringAfterLast("/").toIntOrNull()
                        if (idx != null && idx in 0..3) speedsActual[idx] = value
                    }
                    key.startsWith("Swerve/ModuleAnglesActual/") -> {
                        /**
                         * idx val.
                         */
                        val idx = key.substringAfterLast("/").toIntOrNull()
                        if (idx != null && idx in 0..3) anglesActual[idx] = value
                    }
                    // Support fallback legacy topics
                    key.contains("Swerve/FL_Vel") || key.contains("Swerve/Vel_FL") -> speedsActual[0] = value
                    key.contains("Swerve/FR_Vel") || key.contains("Swerve/Vel_FR") -> speedsActual[1] = value
                    key.contains("Swerve/BL_Vel") || key.contains("Swerve/Vel_BL") -> speedsActual[2] = value
                    key.contains("Swerve/BR_Vel") || key.contains("Swerve/Vel_BR") -> speedsActual[3] = value
                    key.contains("Swerve/FL_Angle") || key.contains("Swerve/Angle_FL") -> anglesActual[0] = value
                    key.contains("Swerve/FR_Angle") || key.contains("Swerve/Angle_FR") -> anglesActual[1] = value
                    key.contains("Swerve/BL_Angle") || key.contains("Swerve/Angle_BL") -> anglesActual[2] = value
                    key.contains("Swerve/BR_Angle") || key.contains("Swerve/Angle_BR") -> anglesActual[3] = value
                }
            }
        }
    }

    Column(
        modifier = modifier
            .background(AresSurface, RoundedCornerShape(12.dp))
            .border(1.dp, AresBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Swerve Module Vectors",
                color = AresTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            /**
             * isConnected val.
             */
            val isConnected by nt4ClientService.isConnected.collectAsState()
            Text(
                if (isConnected) "Live Connected" else "Live Disconnected",
                color = if (isConnected) AresGreen else AresTextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider(color = AresBorder, thickness = 1.dp)

        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModuleCard("Front Left (FL)", speedsTarget[0], anglesTarget[0], speedsActual[0], anglesActual[0], Modifier.weight(1f))
                ModuleCard("Back Left (BL)", speedsTarget[2], anglesTarget[2], speedsActual[2], anglesActual[2], Modifier.weight(1f))
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModuleCard("Front Right (FR)", speedsTarget[1], anglesTarget[1], speedsActual[1], anglesActual[1], Modifier.weight(1f))
                ModuleCard("Back Right (BR)", speedsTarget[3], anglesTarget[3], speedsActual[3], anglesActual[3], Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ModuleCard(
    name: String,
    speedTarget: Double,
    angleTarget: Double,
    speedActual: Double,
    angleActual: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(AresSurfaceElevated, RoundedCornerShape(8.dp))
            .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dial Canvas
        Canvas(modifier = Modifier.size(56.dp)) {
            /**
             * cx val.
             */
            val cx = size.width / 2f
            /**
             * cy val.
             */
            val cy = size.height / 2f
            /**
             * r val.
             */
            val r = size.width / 2f - 4f

            // Draw outer dial boundary
            drawCircle(
                color = AresBorder,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )

            // Draw target vector line (dashed gold)
            /**
             * targetRad val.
             */
            val targetRad = if (Math.abs(angleTarget) > 2 * Math.PI) Math.toRadians(angleTarget) else angleTarget
            /**
             * targetLength val.
             */
            val targetLength = r * (speedTarget / 4.0).coerceIn(0.0, 1.0).toFloat()
            /**
             * targetSteer val.
             */
            val targetSteer = -targetRad - Math.PI / 2.0
            /**
             * tx val.
             */
            val tx = cx + targetLength * cos(targetSteer).toFloat()
            /**
             * ty val.
             */
            val ty = cy + targetLength * sin(targetSteer).toFloat()
            /**
             * dashEffect val.
             */
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            drawLine(
                color = AresGold,
                start = Offset(cx, cy),
                end = Offset(tx, ty),
                strokeWidth = 2f,
                pathEffect = dashEffect,
                cap = StrokeCap.Round
            )

            // Draw actual vector line (solid cyan)
            /**
             * actualRad val.
             */
            val actualRad = if (Math.abs(angleActual) > 2 * Math.PI) Math.toRadians(angleActual) else angleActual
            /**
             * actualLength val.
             */
            val actualLength = r * (speedActual / 4.0).coerceIn(0.0, 1.0).toFloat()
            /**
             * actualSteer val.
             */
            val actualSteer = -actualRad - Math.PI / 2.0
            /**
             * ax val.
             */
            val ax = cx + actualLength * cos(actualSteer).toFloat()
            /**
             * ay val.
             */
            val ay = cy + actualLength * sin(actualSteer).toFloat()
            drawLine(
                color = AresCyan,
                start = Offset(cx, cy),
                end = Offset(ax, ay),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        // Details column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(name, color = AresTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            
            // Speed visualization progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(AresBorder, RoundedCornerShape(3.dp))
            ) {
                // Target speed outline bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth((speedTarget / 4.0).coerceIn(0.0, 1.0).toFloat())
                        .fillMaxHeight()
                        .border(0.5.dp, AresGold, RoundedCornerShape(3.dp))
                )
                // Actual speed filled bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth((speedActual / 4.0).coerceIn(0.0, 1.0).toFloat())
                        .fillMaxHeight()
                        .background(AresCyan, RoundedCornerShape(3.dp))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Tgt: ${"%.2f".format(speedTarget)} m/s @ ${"%.0f".format(Math.toDegrees(angleTarget) % 360)}°",
                    color = AresGold,
                    fontSize = 8.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    "Act: ${"%.2f".format(speedActual)} m/s @ ${"%.0f".format(Math.toDegrees(angleActual) % 360)}°",
                    color = AresCyan,
                    fontSize = 8.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

