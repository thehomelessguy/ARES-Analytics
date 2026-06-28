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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import com.ares.analytics.service.ReplayFrame
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun JoystickVisualizer(
    currentFrame: ReplayFrame?,
    nt4ClientService: Nt4ClientService? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var lx by remember { mutableStateOf(0.0) }
    var ly by remember { mutableStateOf(0.0) }
    var rx by remember { mutableStateOf(0.0) }
    var ry by remember { mutableStateOf(0.0) }
    var lt by remember { mutableStateOf(0.0) }
    var rt by remember { mutableStateOf(0.0) }
    var btnA by remember { mutableStateOf(false) }
    var btnB by remember { mutableStateOf(false) }
    var btnX by remember { mutableStateOf(false) }
    var btnY by remember { mutableStateOf(false) }
    var dpadUp by remember { mutableStateOf(false) }
    var dpadDown by remember { mutableStateOf(false) }
    var dpadLeft by remember { mutableStateOf(false) }
    var dpadRight by remember { mutableStateOf(false) }

    if (currentFrame != null) {
        lx = currentFrame.values["/Driver/LeftStickX"] ?: currentFrame.values["Driver/LeftStickX"] ?: 0.0
        ly = currentFrame.values["/Driver/LeftStickY"] ?: currentFrame.values["Driver/LeftStickY"] ?: 0.0
        rx = currentFrame.values["/Driver/RightStickX"] ?: currentFrame.values["Driver/RightStickX"] ?: 0.0
        ry = currentFrame.values["/Driver/RightStickY"] ?: currentFrame.values["Driver/RightStickY"] ?: 0.0
        lt = currentFrame.values["/Driver/LeftTrigger"] ?: currentFrame.values["Driver/LeftTrigger"] ?: 0.0
        rt = currentFrame.values["/Driver/RightTrigger"] ?: currentFrame.values["Driver/RightTrigger"] ?: 0.0
        btnA = (currentFrame.values["/Driver/AButton"] ?: currentFrame.values["Driver/AButton"] ?: 0.0) > 0.5
        btnB = (currentFrame.values["/Driver/BButton"] ?: currentFrame.values["Driver/BButton"] ?: 0.0) > 0.5
        btnX = (currentFrame.values["/Driver/XButton"] ?: currentFrame.values["Driver/XButton"] ?: 0.0) > 0.5
        btnY = (currentFrame.values["/Driver/YButton"] ?: currentFrame.values["Driver/YButton"] ?: 0.0) > 0.5
        dpadUp = (currentFrame.values["/Driver/DpadUp"] ?: 0.0) > 0.5
        dpadDown = (currentFrame.values["/Driver/DpadDown"] ?: 0.0) > 0.5
        dpadLeft = (currentFrame.values["/Driver/DpadLeft"] ?: 0.0) > 0.5
        dpadRight = (currentFrame.values["/Driver/DpadRight"] ?: 0.0) > 0.5
    } else if (nt4ClientService != null) {
        LaunchedEffect(Unit) {
            scope.launch {
                nt4ClientService.telemetryFlow.collect { frame ->
                    val key = frame.key
                    val value = frame.value
                    when (key) {
                        "/Driver/LeftStickX", "Driver/LeftStickX" -> lx = value
                        "/Driver/LeftStickY", "Driver/LeftStickY" -> ly = value
                        "/Driver/RightStickX", "Driver/RightStickX" -> rx = value
                        "/Driver/RightStickY", "Driver/RightStickY" -> ry = value
                        "/Driver/LeftTrigger", "Driver/LeftTrigger" -> lt = value
                        "/Driver/RightTrigger", "Driver/RightTrigger" -> rt = value
                        "/Driver/AButton", "Driver/AButton" -> btnA = value > 0.5
                        "/Driver/BButton", "Driver/BButton" -> btnB = value > 0.5
                        "/Driver/XButton", "Driver/XButton" -> btnX = value > 0.5
                        "/Driver/YButton", "Driver/YButton" -> btnY = value > 0.5
                        "/Driver/DpadUp" -> dpadUp = value > 0.5
                        "/Driver/DpadDown" -> dpadDown = value > 0.5
                        "/Driver/DpadLeft" -> dpadLeft = value > 0.5
                        "/Driver/DpadRight" -> dpadRight = value > 0.5
                    }
                }
            }
        }
    }

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
            "Joystick/Gamepad Input Monitor",
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
                val cx = size.width / 2f
                val cy = size.height / 2f

                // 1. Draw Controller Main Body Outline
                val bodyW = 340f
                val bodyH = 180f
                drawRoundRect(
                    color = AresSurfaceElevated,
                    topLeft = Offset(cx - bodyW / 2f, cy - bodyH / 2f),
                    size = Size(bodyW, bodyH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(40f, 40f)
                )
                drawRoundRect(
                    color = AresBorder,
                    topLeft = Offset(cx - bodyW / 2f, cy - bodyH / 2f),
                    size = Size(bodyW, bodyH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(40f, 40f),
                    style = Stroke(width = 2.5f)
                )

                // 2. Draw Analog Sticks (Left Stick at x-90, y+20; Right Stick at x+40, y+20)
                val stickRadius = 32f
                val leftStickCenter = Offset(cx - 85f, cy + 15f)
                val rightStickCenter = Offset(cx + 45f, cy + 15f)

                // Left Stick Base
                drawCircle(color = AresSurface, radius = stickRadius, center = leftStickCenter)
                drawCircle(color = AresBorder, radius = stickRadius, center = leftStickCenter, style = Stroke(width = 1.5f))

                // Left Stick Deflection Node
                val lNodeX = leftStickCenter.x + (lx * stickRadius * 0.7).toFloat()
                val lNodeY = leftStickCenter.y + (ly * stickRadius * 0.7).toFloat()
                drawCircle(color = AresCyan, radius = 12f, center = Offset(lNodeX, lNodeY))

                // Right Stick Base
                drawCircle(color = AresSurface, radius = stickRadius, center = rightStickCenter)
                drawCircle(color = AresBorder, radius = stickRadius, center = rightStickCenter, style = Stroke(width = 1.5f))

                // Right Stick Deflection Node
                val rNodeX = rightStickCenter.x + (rx * stickRadius * 0.7).toFloat()
                val rNodeY = rightStickCenter.y + (ry * stickRadius * 0.7).toFloat()
                drawCircle(color = AresCyan, radius = 12f, center = Offset(rNodeX, rNodeY))

                // 3. Draw D-pad (Left side: center x-135, y-20)
                val dpadCenter = Offset(cx - 135f, cy - 25f)
                val dpadSize = 16f

                // Draw D-pad background cross
                drawRect(color = AresSurface, topLeft = Offset(dpadCenter.x - dpadSize * 2.5f, dpadCenter.y - dpadSize / 2f), size = Size(dpadSize * 5f, dpadSize))
                drawRect(color = AresSurface, topLeft = Offset(dpadCenter.x - dpadSize / 2f, dpadCenter.y - dpadSize * 2.5f), size = Size(dpadSize, dpadSize * 5f))

                // Draw directions with highlight colors
                drawRect(color = if (dpadLeft) AresCyan else AresBorder, topLeft = Offset(dpadCenter.x - dpadSize * 2.5f, dpadCenter.y - dpadSize / 2f), size = Size(dpadSize, dpadSize))
                drawRect(color = if (dpadRight) AresCyan else AresBorder, topLeft = Offset(dpadCenter.x + dpadSize * 1.5f, dpadCenter.y - dpadSize / 2f), size = Size(dpadSize, dpadSize))
                drawRect(color = if (dpadUp) AresCyan else AresBorder, topLeft = Offset(dpadCenter.x - dpadSize / 2f, dpadCenter.y - dpadSize * 2.5f), size = Size(dpadSize, dpadSize))
                drawRect(color = if (dpadDown) AresCyan else AresBorder, topLeft = Offset(dpadCenter.x - dpadSize / 2f, dpadCenter.y + dpadSize * 1.5f), size = Size(dpadSize, dpadSize))

                // 4. Draw Buttons A/B/X/Y (Right side: center x+110, y-25)
                val buttonsCenter = Offset(cx + 105f, cy - 25f)
                val btnRadius = 12f
                val btnOffset = 24f

                // Y Button (Top)
                drawCircle(color = if (btnY) AresGold else AresSurface, radius = btnRadius, center = Offset(buttonsCenter.x, buttonsCenter.y - btnOffset))
                drawCircle(color = AresBorder, radius = btnRadius, center = Offset(buttonsCenter.x, buttonsCenter.y - btnOffset), style = Stroke(width = 1.5f))

                // A Button (Bottom)
                drawCircle(color = if (btnA) AresGold else AresSurface, radius = btnRadius, center = Offset(buttonsCenter.x, buttonsCenter.y + btnOffset))
                drawCircle(color = AresBorder, radius = btnRadius, center = Offset(buttonsCenter.x, buttonsCenter.y + btnOffset), style = Stroke(width = 1.5f))

                // X Button (Left)
                drawCircle(color = if (btnX) AresGold else AresSurface, radius = btnRadius, center = Offset(buttonsCenter.x - btnOffset, buttonsCenter.y))
                drawCircle(color = AresBorder, radius = btnRadius, center = Offset(buttonsCenter.x - btnOffset, buttonsCenter.y), style = Stroke(width = 1.5f))

                // B Button (Right)
                drawCircle(color = if (btnB) AresGold else AresSurface, radius = btnRadius, center = Offset(buttonsCenter.x + btnOffset, buttonsCenter.y))
                drawCircle(color = AresBorder, radius = btnRadius, center = Offset(buttonsCenter.x + btnOffset, buttonsCenter.y), style = Stroke(width = 1.5f))

                // 5. Draw Triggers and Bumpers (Top: left x-80, right x+80)
                val triggerW = 60f
                val triggerH = 14f

                // Left Trigger Bar
                drawRect(color = AresSurface, topLeft = Offset(cx - 120f, cy - bodyH / 2f - 20f), size = Size(triggerW, triggerH))
                drawRect(color = AresCyan, topLeft = Offset(cx - 120f, cy - bodyH / 2f - 20f), size = Size(triggerW * lt.toFloat(), triggerH))
                drawRect(color = AresBorder, topLeft = Offset(cx - 120f, cy - bodyH / 2f - 20f), size = Size(triggerW, triggerH), style = Stroke(width = 1.5f))

                // Right Trigger Bar
                drawRect(color = AresSurface, topLeft = Offset(cx + 60f, cy - bodyH / 2f - 20f), size = Size(triggerW, triggerH))
                drawRect(color = AresCyan, topLeft = Offset(cx + 60f, cy - bodyH / 2f - 20f), size = Size(triggerW * rt.toFloat(), triggerH))
                drawRect(color = AresBorder, topLeft = Offset(cx + 60f, cy - bodyH / 2f - 20f), size = Size(triggerW, triggerH), style = Stroke(width = 1.5f))
            }
        }

        // Details Panel
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "L Stick: (${"%.2f".format(lx)}, ${"%.2f".format(ly)})", color = AresTextSecondary, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text(text = "R Stick: (${"%.2f".format(rx)}, ${"%.2f".format(ry)})", color = AresTextSecondary, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text(text = "L Trigger: ${"%.2f".format(lt)} | R Trigger: ${"%.2f".format(rt)}", color = AresTextSecondary, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}
