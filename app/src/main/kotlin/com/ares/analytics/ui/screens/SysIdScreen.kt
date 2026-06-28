package com.ares.analytics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.ui.theme.*
import com.ares.analytics.viewmodel.SysIdIntent
import com.ares.analytics.viewmodel.SysIdViewModel

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

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // System Parameters Summary Card
        Surface(
            modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = AresSurface
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("SysId Characterization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                Divider(color = AresBorder)

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AresCyan)
                    }
                } else {
                    val s = state.summary
                    if (s == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Select a session in Dashboard index to analyze SysId.", color = AresTextTertiary, fontSize = 12.sp)
                        }
                    } else {
                        ParamItem("Static Friction (kS)", String.format("%.4f V", s.kS))
                        ParamItem("Velocity Constant (kV)", String.format("%.4f V/(m/s)", s.kV))
                        ParamItem("Acceleration Constant (kA)", String.format("%.4f V/(m/s²)", s.kA))
                        ParamItem("OLS Fit Quality (R²)", String.format("%.2f%%", s.rSquared * 100))
                        ParamItem("Transient Classification", s.transientClassification.name)
                    }
                }
            }
        }

        // Driver Input Jitter Analysis Card
        Surface(
            modifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = AresSurface
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Driver Input Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AresTextPrimary)
                Divider(color = AresBorder)

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AresCyan)
                    }
                } else {
                    val j = state.jitterResult
                    if (j == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Select a session in Dashboard index to analyze joystick telemetry.", color = AresTextTertiary, fontSize = 12.sp)
                        }
                    } else {
                        ParamItem("Jitter Present", if (j.hasJitter) "YES (Warning)" else "NO")
                        ParamItem("Peak Jitter Frequency", String.format("%.1f Hz", j.peakFrequencyHz))
                        ParamItem("Recommended Exponent", String.format("%.1f", j.recommendedExponent))
                        ParamItem("Recommended Slew Rate", if (j.recommendedSlewRate == Double.MAX_VALUE) "No Limit" else String.format("%.1f units/s", j.recommendedSlewRate))

                        Spacer(Modifier.height(8.dp))
                        Text("AI Assessment:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AresTextSecondary)
                        Text(j.message, fontSize = 13.sp, color = AresTextPrimary, lineHeight = 18.sp)

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.onIntent(
                                    SysIdIntent.ApplyToRobotCode(
                                        recommendedExponent = j.recommendedExponent,
                                        recommendedSlewRate = j.recommendedSlewRate,
                                        projectPath = projectPath
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AresCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply to Robot Code", color = AresBackground, fontWeight = FontWeight.Bold)
                        }
                        if (state.exportStatus.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(state.exportStatus, color = if (state.exportStatus.contains("Failed")) AresError else AresGreen, fontSize = 11.sp)
                            LaunchedEffect(state.exportStatus) {
                                kotlinx.coroutines.delay(3000)
                                viewModel.onIntent(SysIdIntent.ClearExportStatus)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(AresSurfaceElevated).border(1.dp, AresBorder, RoundedCornerShape(6.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = AresTextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AresTextPrimary)
    }
}
