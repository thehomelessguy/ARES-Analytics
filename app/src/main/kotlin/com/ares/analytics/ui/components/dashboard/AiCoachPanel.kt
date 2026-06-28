package com.ares.analytics.ui.components.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.service.DatabaseService
import com.ares.analytics.service.SyncEngineService
import com.ares.analytics.shared.AlertRecord
import com.ares.analytics.shared.ForensicsResponse
import com.ares.analytics.ui.theme.*
import kotlinx.coroutines.launch

sealed class ForensicsState {
    object Idle : ForensicsState()
    object Loading : ForensicsState()
    data class Success(val response: ForensicsResponse) : ForensicsState()
    data class Error(val message: String) : ForensicsState()
}

@Composable
fun AiCoachPanel(
    databaseService: DatabaseService,
    syncEngineService: SyncEngineService,
    sessionId: String?,
    onForensicsCompleted: (ForensicsResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var forensicsState by remember { mutableStateOf<ForensicsState>(ForensicsState.Idle) }

    LaunchedEffect(sessionId) {
        forensicsState = ForensicsState.Idle
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = AresCyan)
            Text(
                "ARES Pit Diagnostics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AresTextPrimary
            )
        }

        Divider(color = AresBorder, thickness = 1.dp)

        if (sessionId == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Select a session to trigger AI Forensics.", color = AresTextTertiary)
            }
            return
        }

        when (val state = forensicsState) {
            is ForensicsState.Idle -> {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Trigger Vertex AI model to diagnose potential hardware faults and telemetry anomalies.", color = AresTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                forensicsState = ForensicsState.Loading
                                try {
                                    // 1. Gather local data
                                    val summary = databaseService.getSessionSummary(sessionId)
                                    val alerts = databaseService.getAlerts(sessionId)
                                    val topology = databaseService.getTopology(summary?.robotId ?: "")

                                    // 2. Call gateway forensics api
                                    if (summary == null) {
                                        throw IllegalArgumentException("Session summary not found.")
                                    }
                                    val req = com.ares.analytics.shared.ForensicsRequest(
                                        teamId = summary.teamId,
                                        sessionId = sessionId,
                                        alerts = alerts,
                                        summary = summary,
                                        topology = topology,
                                        sysIdDrift = emptyMap() // Can be populated if drift calculation available
                                    )
                                    val response = syncEngineService.requestForensics(req)
                                    forensicsState = ForensicsState.Success(response)
                                    onForensicsCompleted(response)
                                } catch (e: Exception) {
                                    forensicsState = ForensicsState.Error(e.message ?: "Failed to process diagnostics.")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AresCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Analyze Telemetry", color = AresBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is ForensicsState.Loading -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = AresCyan)
                        Text("Running AI Pit Forensics...", color = AresTextSecondary, fontSize = 12.sp)
                    }
                }
            }
            is ForensicsState.Success -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("PROBABLE CAUSE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AresTextSecondary)
                    Text(state.response.probableRootCause, fontSize = 13.sp, color = AresTextPrimary, fontWeight = FontWeight.SemiBold)
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CONFIDENCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AresTextSecondary)
                        val badgeColor = if (state.response.confidenceScore > 0.8) AresGreen else AresAmber
                        Text("${(state.response.confidenceScore * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                    }
                    LinearProgressIndicator(
                        progress = state.response.confidenceScore.toFloat(),
                        color = AresCyan,
                        trackColor = AresBorder,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
            }
            is ForensicsState.Error -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = AresError)
                        Text(state.message, color = AresError, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
