package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
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
import com.ares.analytics.service.ReplayFrame
import com.ares.analytics.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MecanumVisualizer(
    currentFrame: ReplayFrame? = null,
    nt4ClientService: Nt4ClientService? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // FL = 0, FR = 1, BL = 2, BR = 3
    val velocities = remember { mutableStateListOf(0.0, 0.0, 0.0, 0.0) }
    val currents = remember { mutableStateListOf(0.0, 0.0, 0.0, 0.0) }

    if (currentFrame != null) {
        velocities[0] = currentFrame.values["/Drive/MotorVelocity_fl"] ?: currentFrame.values["Drive/MotorVelocity_fl"] ?: currentFrame.values["/Drive/MotorPower_fl"] ?: currentFrame.values["Drive/MotorPower_fl"] ?: 0.0
        velocities[1] = currentFrame.values["/Drive/MotorVelocity_fr"] ?: currentFrame.values["Drive/MotorVelocity_fr"] ?: currentFrame.values["/Drive/MotorPower_fr"] ?: currentFrame.values["Drive/MotorPower_fr"] ?: 0.0
        velocities[2] = currentFrame.values["/Drive/MotorVelocity_bl"] ?: currentFrame.values["Drive/MotorVelocity_bl"] ?: currentFrame.values["/Drive/MotorPower_bl"] ?: currentFrame.values["Drive/MotorPower_bl"] ?: 0.0
        velocities[3] = currentFrame.values["/Drive/MotorVelocity_br"] ?: currentFrame.values["Drive/MotorVelocity_br"] ?: currentFrame.values["/Drive/MotorPower_br"] ?: currentFrame.values["Drive/MotorPower_br"] ?: 0.0

        currents[0] = currentFrame.values["/Drive/MotorCurrent_fl"] ?: currentFrame.values["Drive/MotorCurrent_fl"] ?: 0.0
        currents[1] = currentFrame.values["/Drive/MotorCurrent_fr"] ?: currentFrame.values["Drive/MotorCurrent_fr"] ?: 0.0
        currents[2] = currentFrame.values["/Drive/MotorCurrent_bl"] ?: currentFrame.values["Drive/MotorCurrent_bl"] ?: 0.0
        currents[3] = currentFrame.values["/Drive/MotorCurrent_br"] ?: currentFrame.values["Drive/MotorCurrent_br"] ?: 0.0
    } else if (nt4ClientService != null) {
        LaunchedEffect(Unit) {
            scope.launch {
                nt4ClientService.telemetryFlow.collect { frame ->
                    val key = frame.key
                    val value = frame.value
                    when (key) {
                        "Drive/MotorVelocity_fl", "/Drive/MotorVelocity_fl", "Drive/MotorPower_fl", "/Drive/MotorPower_fl" -> velocities[0] = value
                        "Drive/MotorVelocity_fr", "/Drive/MotorVelocity_fr", "Drive/MotorPower_fr", "/Drive/MotorPower_fr" -> velocities[1] = value
                        "Drive/MotorVelocity_bl", "/Drive/MotorVelocity_bl", "Drive/Drive/MotorPower_bl", "Drive/MotorPower_bl", "/Drive/MotorPower_bl" -> velocities[2] = value
                        "Drive/MotorVelocity_br", "/Drive/MotorVelocity_br", "Drive/MotorPower_br", "/Drive/MotorPower_br" -> velocities[3] = value
                        
                        "Drive/MotorCurrent_fl", "/Drive/MotorCurrent_fl" -> currents[0] = value
                        "Drive/MotorCurrent_fr", "/Drive/MotorCurrent_fr" -> currents[1] = value
                        "Drive/MotorCurrent_bl", "/Drive/MotorCurrent_bl" -> currents[2] = value
                        "Drive/MotorCurrent_br", "/Drive/MotorCurrent_br" -> currents[3] = value
                    }
                }
            }
        }
    }

    val isConnected = if (nt4ClientService != null) {
        nt4ClientService.isConnected.collectAsState().value
    } else false

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AresSurface, RoundedCornerShape(12.dp))
            .border(1.dp, AresBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Mecanum Drivetrain Force Vectors",
                color = AresTextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                if (isConnected) "Live Connected" else "Live Disconnected",
                color = if (isConnected) AresGreen else AresTextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(AresBackground, RoundedCornerShape(8.dp))
                .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Draw robot outline (dashed)
                val robotW = 160f
                val robotH = 220f
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                drawRect(
                    color = AresBorder,
                    topLeft = Offset(cx - robotW / 2f, cy - robotH / 2f),
                    size = Size(robotW, robotH),
                    style = Stroke(width = 2f, pathEffect = dashEffect)
                )

                // Wheel definitions: Name, CenterOffset, RollerAngleRad, Speed
                val wheels = listOf(
                    // FL: rollers at 45 deg (points top-left to bottom-right)
                    WheelData("FL", Offset(cx - robotW / 2f, cy - robotH / 2f), Math.toRadians(45.0), velocities[0], currents[0]),
                    // FR: rollers at -45 deg (points bottom-left to top-right)
                    WheelData("FR", Offset(cx + robotW / 2f, cy - robotH / 2f), Math.toRadians(-45.0), velocities[1], currents[1]),
                    // BL: rollers at -45 deg
                    WheelData("BL", Offset(cx - robotW / 2f, cy + robotH / 2f), Math.toRadians(-45.0), velocities[2], currents[2]),
                    // BR: rollers at 45 deg
                    WheelData("BR", Offset(cx + robotW / 2f, cy + robotH / 2f), Math.toRadians(45.0), velocities[3], currents[3])
                )

                for (w in wheels) {
                    val center = w.center
                    val wWidth = 32f
                    val wHeight = 64f

                    // Draw wheel body
                    drawRoundRect(
                        color = AresSurfaceElevated,
                        topLeft = Offset(center.x - wWidth / 2f, center.y - wHeight / 2f),
                        size = Size(wWidth, wHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = AresBorder,
                        topLeft = Offset(center.x - wWidth / 2f, center.y - wHeight / 2f),
                        size = Size(wWidth, wHeight),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 1.5f)
                    )

                    // Draw wheel rollers (diagonal lines)
                    val spacing = 12f
                    var offset = -wHeight / 2f + 4f
                    while (offset < wHeight / 2f) {
                        val rx1 = center.x - wWidth / 2f + 2f
                        val ry1 = center.y + offset
                        val rx2 = center.x + wWidth / 2f - 2f
                        val ry2 = ry1 + wWidth * tan(w.rollerAngle).toFloat()

                        if (ry2 >= center.y - wHeight / 2f && ry2 <= center.y + wHeight / 2f) {
                            drawLine(
                                color = AresBorder.copy(alpha = 0.6f),
                                start = Offset(rx1, ry1),
                                end = Offset(rx2, ry2),
                                strokeWidth = 2.5f,
                                cap = StrokeCap.Round
                            )
                        }
                        offset += spacing
                    }

                    // Draw spin vector arrow (along wheel axis, vertically)
                    if (Math.abs(w.speed) > 0.05) {
                        val maxArrowLen = 40f
                        val arrowLen = (w.speed * 30f).toFloat().coerceIn(-maxArrowLen, maxArrowLen)
                        val spinEnd = Offset(center.x, center.y - arrowLen)

                        drawLine(
                            color = AresGreen,
                            start = center,
                            end = spinEnd,
                            strokeWidth = 3.5f,
                            cap = StrokeCap.Round
                        )

                        // Draw traction force vector arrow (at 45 degrees, matching roller slide reaction)
                        // The traction force vector points along the roller's perpendicular axis (direction of push)
                        val forceAngle = when (w.name) {
                            "FL" -> if (w.speed >= 0) Math.toRadians(-45.0) else Math.toRadians(135.0)
                            "FR" -> if (w.speed >= 0) Math.toRadians(-135.0) else Math.toRadians(45.0)
                            "BL" -> if (w.speed >= 0) Math.toRadians(45.0) else Math.toRadians(-135.0)
                            "BR" -> if (w.speed >= 0) Math.toRadians(135.0) else Math.toRadians(-45.0)
                            else -> 0.0
                        }

                        val forceLen = Math.abs(w.speed * 30f).toFloat().coerceIn(10f, maxArrowLen)
                        val forceEnd = Offset(
                            center.x + forceLen * cos(forceAngle).toFloat(),
                            center.y + forceLen * sin(forceAngle).toFloat()
                        )

                        drawLine(
                            color = AresCyan,
                            start = center,
                            end = forceEnd,
                            strokeWidth = 3.5f,
                            cap = StrokeCap.Round
                        )

                        // Draw force arrowhead
                        val headSize = 8f
                        val leftWing = Offset(
                            forceEnd.x - headSize * cos(forceAngle - Math.PI / 6).toFloat(),
                            forceEnd.y - headSize * sin(forceAngle - Math.PI / 6).toFloat()
                        )
                        val rightWing = Offset(
                            forceEnd.x - headSize * cos(forceAngle + Math.PI / 6).toFloat(),
                            forceEnd.y - headSize * sin(forceAngle + Math.PI / 6).toFloat()
                        )
                        drawLine(color = AresCyan, start = forceEnd, end = leftWing, strokeWidth = 2.5f)
                        drawLine(color = AresCyan, start = forceEnd, end = rightWing, strokeWidth = 2.5f)
                    }
                }
            }
        }

        // Details Panel
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text("FL: ${"%.2f".format(velocities[0])} rad/s | ${"%.1f".format(currents[0])}A", color = AresTextSecondary, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                Text("BL: ${"%.2f".format(velocities[2])} rad/s | ${"%.1f".format(currents[2])}A", color = AresTextSecondary, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("FR: ${"%.2f".format(velocities[1])} rad/s | ${"%.1f".format(currents[1])}A", color = AresTextSecondary, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                Text("BR: ${"%.2f".format(velocities[3])} rad/s | ${"%.1f".format(currents[3])}A", color = AresTextSecondary, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
        }
    }
}

private data class WheelData(
    val name: String,
    val center: Offset,
    val rollerAngle: Double,
    val speed: Double,
    val current: Double
)

private fun tan(radians: Double): Double = kotlin.math.tan(radians)
