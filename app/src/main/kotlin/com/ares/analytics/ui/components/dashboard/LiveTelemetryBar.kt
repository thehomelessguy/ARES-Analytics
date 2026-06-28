package com.ares.analytics.ui.components.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.ui.theme.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
fun LiveTelemetryBar(
    nt4ClientService: Nt4ClientService,
    modifier: Modifier = Modifier
) {
    val telemetryFlow = nt4ClientService.telemetryFlow
    val isConnected by nt4ClientService.isConnected.collectAsState()

    // Selectively map voltage, EKF, loop time, etc.
    val voltage by telemetryFlow
        .filter { it.key == "/Drive/Voltage" }
        .map { it.value }
        .collectAsState(initial = 12.0)

    val drift by telemetryFlow
        .filter { it.key == "/Drive/EkfDrift" }
        .map { it.value }
        .collectAsState(initial = 0.0)

    val loopTime by telemetryFlow
        .filter { it.key == "/LoopTimeMs" }
        .map { it.value }
        .collectAsState(initial = 8.0)

    val maxCurrent by telemetryFlow
        .filter { it.key == "/Drive/MotorCurrentMax" }
        .map { it.value }
        .collectAsState(initial = 0.0)

    val containerColor = if (isConnected) AresSurfaceElevated else AresSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.dp, AresBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiveTelemetryItem(
            label = "Battery Voltage",
            value = "${String.format("%.2f", voltage)} V",
            icon = Icons.Default.Bolt,
            color = if (voltage < 11.5) AresError else AresGreen,
            modifier = Modifier.weight(1f)
        )
        LiveTelemetryItem(
            label = "EKF Drift",
            value = "${String.format("%.2f", drift)} m",
            icon = Icons.Default.Visibility,
            color = if (drift > 0.15) AresAmber else AresCyan,
            modifier = Modifier.weight(1f)
        )
        LiveTelemetryItem(
            label = "Loop Time",
            value = "${String.format("%.1f", loopTime)} ms",
            icon = Icons.Default.Speed,
            color = if (loopTime > 20.0) AresError else AresCyan,
            modifier = Modifier.weight(1f)
        )
        LiveTelemetryItem(
            label = "Max Motor Current",
            value = "${String.format("%.1f", maxCurrent)} A",
            icon = Icons.Default.GraphicEq,
            color = if (maxCurrent > 20.0) AresAmber else AresCyan,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LiveTelemetryItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AresSurface)
            .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AresBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }

        Column {
            Text(label, fontSize = 11.sp, color = AresTextSecondary)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AresTextPrimary)
        }
    }
}
