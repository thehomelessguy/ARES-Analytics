package com.ares.analytics.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.ui.theme.*
import com.ares.analytics.viewmodel.SysIdIntent
import com.ares.analytics.viewmodel.SysIdViewModel
import com.ares.analytics.service.AlignedDataRow
import com.areslib.control.SysIdMechanism
import com.areslib.control.SysIdRoutine
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun SysIdScreen(
    viewModel: SysIdViewModel,
    projectPath: String,
    sessionId: String?
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.onIntent(SysIdIntent.LoadSession(sessionId))
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header and Controls Row
        Surface(
            modifier = Modifier.fillMaxWidth().border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = AresSurface
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "SysId Diagnostics & Drivetrain Characterization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AresTextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (state.isRobotConnected) AresGreen else AresError,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Text(
                            text = if (state.isRobotConnected) "Robot Connected" else "Robot Disconnected",
                            fontSize = 12.sp,
                            color = AresTextSecondary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mechanism Select
                    Text("Target Mode:", fontSize = 12.sp, color = AresTextSecondary)
                    Row(
                        modifier = Modifier
                            .background(AresSurfaceElevated, RoundedCornerShape(6.dp))
                            .border(1.dp, AresBorder, RoundedCornerShape(6.dp))
                    ) {
                        SysIdMechanism.values().forEach { mech ->
                            val selected = state.selectedMechanism == mech
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (selected) AresCyan else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.onIntent(SysIdIntent.SetMechanism(mech)) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = mech.name,
                                    color = if (selected) AresBackground else AresTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Local File Button
                    Button(
                        onClick = {
                            try {
                                val fileDialog = FileDialog(null as Frame?, "Select SysId Log File", FileDialog.LOAD)
                                fileDialog.isVisible = true
                                val dir = fileDialog.directory
                                val file = fileDialog.file
                                if (dir != null && file != null) {
                                    val content = File(dir, file).readText()
                                    viewModel.onIntent(SysIdIntent.LoadLocalLogFile(content))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AresSurfaceElevated),
                        modifier = Modifier.border(1.dp, AresBorder, RoundedCornerShape(6.dp)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = AresCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Analyze Local Log File", color = AresTextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Main Workspace Split
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Panel: Live Controls and Streaming Data
            Surface(
                modifier = Modifier.weight(1.2f).fillMaxHeight().border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = AresSurface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Robot Controls & Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                    Divider(color = AresBorder)

                    if (state.isRoutineRunning) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().border(1.dp, AresError, RoundedCornerShape(8.dp)),
                            color = AresError.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("AUTOMATED TEST RUNNING", color = AresError, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Robot is currently executing routine.", color = AresTextSecondary, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { viewModel.onIntent(SysIdIntent.StopRoutine) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AresError),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("ABORT TEST (STOP)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Display Routine Trigger Grid
                        Text("Select a diagnostic routine to execute automatically:", fontSize = 12.sp, color = AresTextSecondary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RoutineButton(
                                name = "Quasistatic Forward",
                                desc = "Voltage ramps up slowly (+0.2V/s)",
                                onClick = { viewModel.onIntent(SysIdIntent.StartRoutine(SysIdRoutine.QUASISTATIC_FORWARD)) },
                                enabled = state.isRobotConnected
                            )
                            RoutineButton(
                                name = "Quasistatic Reverse",
                                desc = "Voltage ramps down slowly (-0.2V/s)",
                                onClick = { viewModel.onIntent(SysIdIntent.StartRoutine(SysIdRoutine.QUASISTATIC_REVERSE)) },
                                enabled = state.isRobotConnected
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RoutineButton(
                                name = "Dynamic Forward",
                                desc = "Voltage steps instantly to +3.0V",
                                onClick = { viewModel.onIntent(SysIdIntent.StartRoutine(SysIdRoutine.DYNAMIC_FORWARD)) },
                                enabled = state.isRobotConnected
                            )
                            RoutineButton(
                                name = "Dynamic Reverse",
                                desc = "Voltage steps instantly to -3.0V",
                                onClick = { viewModel.onIntent(SysIdIntent.StartRoutine(SysIdRoutine.DYNAMIC_REVERSE)) },
                                enabled = state.isRobotConnected
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Live Telemetry Plot (Velocity vs. Time)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AresTextSecondary)
                    LiveTelemetryPlot(samples = state.liveSamples)
                }
            }

            // Right Panel: Regression Results
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = AresSurface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Calculated Characterization Constants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                    Divider(color = AresBorder)

                    val activeSummary = state.localAnalysisResult ?: state.summary

                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AresCyan)
                        }
                    } else if (activeSummary == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("No characterization data calculated yet.", color = AresTextTertiary, fontSize = 12.sp)
                                Text("Run a live routine or upload a local log file above.", color = AresTextTertiary, fontSize = 11.sp)
                            }
                        }
                    } else {
                        if (state.localAnalysisResult != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().border(1.dp, AresCyan, RoundedCornerShape(8.dp)),
                                color = AresCyan.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Displaying Local File Results", color = AresCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        "Clear",
                                        color = AresTextSecondary,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable { viewModel.onIntent(SysIdIntent.ClearLocalAnalysis) }
                                    )
                                }
                            }
                        }

                        ParamRow("Static Friction (kS)", String.format("%.4f V", activeSummary.kS))
                        ParamRow("Velocity Constant (kV)", String.format("%.4f V/(m/s)", activeSummary.kV))
                        ParamRow("Acceleration Constant (kA)", String.format("%.4f V/(m/s²)", activeSummary.kA))
                        ParamRow("OLS Fit Quality (R²)", String.format("%.2f%%", activeSummary.rSquared * 100))
                        ParamRow("Transient Type", activeSummary.transientClassification.name)
                        
                        state.fileAnalysisError?.let { err ->
                            Text(err, color = AresError, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.RoutineButton(
    name: String,
    desc: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.weight(1f).height(80.dp).border(1.dp, AresBorder, RoundedCornerShape(8.dp)).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) AresSurfaceElevated else AresSurfaceElevated.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (enabled) AresCyan else AresTextTertiary)
            Text(desc, fontSize = 11.sp, color = AresTextTertiary, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun ParamRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(AresSurfaceElevated).border(1.dp, AresBorder, RoundedCornerShape(6.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = AresTextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AresTextPrimary)
    }
}

@Composable
private fun LiveTelemetryPlot(samples: List<AlignedDataRow>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(AresSurfaceElevated, RoundedCornerShape(8.dp))
            .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
    ) {
        if (samples.size < 2) return@Canvas

        val maxTime = samples.maxOf { it.timestampMs }
        val minTime = samples.minOf { it.timestampMs }
        val dt = (maxTime - minTime).toDouble()

        val maxVel = samples.maxOf { kotlin.math.abs(it.velocity) }.coerceAtLeast(1.0)
        val path = Path()

        samples.forEachIndexed { index, sample ->
            val x = if (dt > 0) ((sample.timestampMs - minTime) / dt * size.width).toFloat() else 0f
            val y = (size.height - (kotlin.math.abs(sample.velocity) / maxVel * size.height)).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = AresCyan,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
