package com.ares.analytics.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ares.analytics.shared.League
import com.ares.analytics.shared.ForensicsResponse
import com.ares.analytics.ui.components.triage.HardwareTopologyMap
import com.ares.analytics.ui.components.triage.PitTriagePanel
import com.ares.analytics.ui.theme.*
import com.ares.analytics.viewmodel.TriageIntent
import com.ares.analytics.viewmodel.TriageViewModel

@Composable
fun TriageScreen(
    viewModel: TriageViewModel,
    league: League,
    diagnosticsResponse: ForensicsResponse?,
    robotId: String
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(robotId) {
        viewModel.onIntent(TriageIntent.LoadTopology(robotId))
    }

    LaunchedEffect(diagnosticsResponse) {
        viewModel.onIntent(TriageIntent.SetDiagnostics(diagnosticsResponse))
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Node graph map
        Box(
            modifier = Modifier.weight(1.2f).fillMaxHeight().border(1.dp, AresBorder, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
        ) {
            HardwareTopologyMap(
                league = league,
                topology = state.topology,
                faultyNodeId = state.diagnosticsResponse?.hardwareFaultLocus?.failedNodeId,
                cascadingNodes = state.diagnosticsResponse?.cascadingNodesAffected ?: emptyList(),
                onNodeSelected = { viewModel.onIntent(TriageIntent.SelectNode(it)) }
            )
        }

        // Pit Triage Checklist panel
        PitTriagePanel(
            diagnostics = state.diagnosticsResponse,
            currentUser = "Pit Crew Leader",
            modifier = Modifier.weight(1f)
        )
    }
}
