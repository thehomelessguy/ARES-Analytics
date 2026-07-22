package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import com.ares.analytics.service.ReplayFrame
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.ui.theme.*
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
fun MechanismVisualizer(
    currentFrame: ReplayFrame?,
    nt4ClientService: Nt4ClientService? = null,
    modifier: Modifier = Modifier
) {
    /**
     * scope val.
     */
    val scope = rememberCoroutineScope()
    /**
     * armAngleDeg var.
     */
    var armAngleDeg by remember { mutableStateOf(0.0) }
    /**
     * slideExtension var.
     */
    var slideExtension by remember { mutableStateOf(0.0) }

    if (currentFrame != null) {
        armAngleDeg = currentFrame.values["Mechanism/ArmAngle"]
            ?: currentFrame.values["Mechanism/ArmAngleDeg"]
            ?: 0.0
        slideExtension = currentFrame.values["Mechanism/SlideHeight"]
            ?: currentFrame.values["Mechanism/SlideExtension"]
            ?: 0.0
    } else if (nt4ClientService != null) {
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
                    when (key) {
                        "Mechanism/ArmAngle", "Mechanism/ArmAngleDeg" -> armAngleDeg = value
                        "Mechanism/SlideHeight", "Mechanism/SlideExtension" -> slideExtension = value
                    }
                }
            }
        }
    }

    /**
     * armAngleRad val.
     */
    val armAngleRad = Math.toRadians(armAngleDeg)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AresSurface)
            .border(1.dp, AresBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Mechanism Linkage Animator",
            color = AresTextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(AresBackground, RoundedCornerShape(8.dp))
                .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                /**
                 * cx val.
                 */
                val cx = size.width / 2f
                /**
                 * cy val.
                 */
                val cy = size.height * 0.8f // Base anchored at 80% height

                // Ground grid line
                drawLine(
                    color = AresBorder,
                    start = Offset(0f, cy),
                    end = Offset(size.width, cy),
                    strokeWidth = 2f
                )

                // Robot base
                /**
                 * baseWidth val.
                 */
                val baseWidth = 140f
                /**
                 * baseHeight val.
                 */
                val baseHeight = 60f
                drawRect(
                    color = AresSurfaceElevated,
                    topLeft = Offset(cx - baseWidth / 2f, cy - baseHeight),
                    size = Size(baseWidth, baseHeight)
                )
                drawRect(
                    color = AresBorder,
                    topLeft = Offset(cx - baseWidth / 2f, cy - baseHeight),
                    size = Size(baseWidth, baseHeight),
                    style = Stroke(width = 2f)
                )

                // Arm pivot anchor
                /**
                 * anchorX val.
                 */
                val anchorX = cx
                /**
                 * anchorY val.
                 */
                val anchorY = cy - baseHeight

                drawCircle(
                    color = AresCyan,
                    radius = 8f,
                    center = Offset(anchorX, anchorY)
                )

                // Base arm linkage (Length = 120 pixels)
                /**
                 * baseArmLength val.
                 */
                val baseArmLength = 120f
                /**
                 * armEndX val.
                 */
                val armEndX = anchorX + baseArmLength * cos(-armAngleRad).toFloat()
                /**
                 * armEndY val.
                 */
                val armEndY = anchorY + baseArmLength * sin(-armAngleRad).toFloat()

                drawLine(
                    color = AresTextPrimary,
                    start = Offset(anchorX, anchorY),
                    end = Offset(armEndX, armEndY),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )

                // Slide extension linkage
                /**
                 * maxExtensionPx val.
                 */
                val maxExtensionPx = 100f
                /**
                 * extensionPx val.
                 */
                val extensionPx = (slideExtension * 120f).toFloat().coerceIn(0f, maxExtensionPx)
                /**
                 * slideEndX val.
                 */
                val slideEndX = armEndX + (baseArmLength * 0.3f + extensionPx) * cos(-armAngleRad).toFloat()
                /**
                 * slideEndY val.
                 */
                val slideEndY = armEndY + (baseArmLength * 0.3f + extensionPx) * sin(-armAngleRad).toFloat()

                drawLine(
                    color = AresCyan,
                    start = Offset(armEndX, armEndY),
                    end = Offset(slideEndX, slideEndY),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = AresCyan,
                    radius = 5f,
                    center = Offset(slideEndX, slideEndY)
                )

                // Gripper claw
                /**
                 * gripperLen val.
                 */
                val gripperLen = 22f
                /**
                 * perpAngle val.
                 */
                val perpAngle = -armAngleRad + Math.PI / 2
                /**
                 * g1x val.
                 */
                val g1x = slideEndX + gripperLen * cos(perpAngle).toFloat()
                /**
                 * g1y val.
                 */
                val g1y = slideEndY + gripperLen * sin(perpAngle).toFloat()
                /**
                 * g2x val.
                 */
                val g2x = slideEndX - gripperLen * cos(perpAngle).toFloat()
                /**
                 * g2y val.
                 */
                val g2y = slideEndY - gripperLen * sin(perpAngle).toFloat()

                drawLine(
                    color = AresGold,
                    start = Offset(g1x, g1y),
                    end = Offset(g2x, g2y),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Details Panel
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Arm Angle: ${"%.1f".format(armAngleDeg)}°",
                color = AresTextSecondary,
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = "Slide Height: ${"%.2f".format(slideExtension)}m",
                color = AresTextSecondary,
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}
