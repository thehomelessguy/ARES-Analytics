package com.ares.analytics.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun SystemHealthCard(
    nt4ClientService: Nt4ClientService,
    modifier: Modifier = Modifier
) {
    /**
     * scope val.
     */
    val scope = rememberCoroutineScope()
    
    /**
     * loopTimeMs var.
     */
    var loopTimeMs by remember { mutableStateOf<Double?>(null) }
    /**
     * batteryVoltage var.
     */
    var batteryVoltage by remember { mutableStateOf<Double?>(null) }
    /**
     * brownoutCount var.
     */
    var brownoutCount by remember { mutableStateOf<Int?>(null) }
    /**
     * loopOverruns var.
     */
    var loopOverruns by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            nt4ClientService.telemetryFlow.collect { frame ->
                /**
                 * key val.
                 */
                val key = frame.key.lowercase()
                /**
                 * value val.
                 */
                val value = frame.value
                
                when {
                    key.contains("looptime") || key.contains("loop_time") -> {
                        loopTimeMs = value as? Double
                    }
                    key.contains("batteryvoltage") || key.contains("battery_voltage") -> {
                        batteryVoltage = value as? Double
                    }
                    key.contains("brownoutcount") || key.contains("brownout_count") -> {
                        brownoutCount = (value as? Double)?.toInt() ?: (value as? Int)
                    }
                    key.contains("loopoverruns") || key.contains("loop_overruns") -> {
                        loopOverruns = (value as? Double)?.toInt() ?: (value as? Int)
                    }
                }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AresSurfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "System Health",
                    tint = AresGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("RoboRIO / Control Hub Health", color = AresTextPrimary, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = AresBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Loop Time
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LOOP TIME", color = AresTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    /**
                     * hz val.
                     */
                    val hz = if (loopTimeMs != null && loopTimeMs!! > 0) 1000.0 / loopTimeMs!! else 0.0
                    /**
                     * loopColor val.
                     */
                    val loopColor = when {
                        hz < 35.0 -> AresError
                        hz < 45.0 -> AresGold
                        else -> AresGreen
                    }
                    Text(
                        text = loopTimeMs?.let { String.format("%.1f ms", it) } ?: "--",
                        color = loopColor,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (loopTimeMs != null) String.format("(%.0f Hz)", hz) else "",
                        color = loopColor,
                        fontSize = 12.sp
                    )
                }
                
                // Overruns
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OVERRUNS", color = AresTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    /**
                     * overrunVal val.
                     */
                    val overrunVal = loopOverruns ?: 0
                    /**
                     * overrunColor val.
                     */
                    val overrunColor = if (overrunVal > 0) AresGold else AresGreen
                    Text(
                        text = overrunVal.toString(),
                        color = overrunColor,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Battery Voltage
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BATTERY", color = AresTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    /**
                     * voltage val.
                     */
                    val voltage = batteryVoltage
                    /**
                     * batteryColor val.
                     */
                    val batteryColor = when {
                        voltage == null -> AresTextPrimary
                        voltage < 11.5 -> AresError
                        voltage < 12.2 -> AresGold
                        else -> AresGreen
                    }
                    Text(
                        text = voltage?.let { String.format("%.2f V", it) } ?: "--",
                        color = batteryColor,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Brownouts
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BROWNOUTS", color = AresTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    /**
                     * brownoutVal val.
                     */
                    val brownoutVal = brownoutCount ?: 0
                    /**
                     * brownoutColor val.
                     */
                    val brownoutColor = if (brownoutVal > 0) AresError else AresTextPrimary
                    Text(
                        text = brownoutVal.toString(),
                        color = brownoutColor,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
