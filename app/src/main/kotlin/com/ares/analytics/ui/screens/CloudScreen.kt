package com.ares.analytics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.shared.SessionSummary
import com.ares.analytics.ui.theme.*
import com.ares.analytics.viewmodel.CloudIntent
import com.ares.analytics.viewmodel.CloudViewModel

@Composable
fun CloudScreen(
    viewModel: CloudViewModel,
    teamId: String,
    seasonId: String
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header / Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AresSurface)
                .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Cloud Data Management", color = AresTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                if (state.isAuthenticated) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AresGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Authenticated with Firebase", color = AresTextSecondary, fontSize = 12.sp)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AresAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Not Authenticated - Syncing may fail", color = AresTextSecondary, fontSize = 12.sp)
                    }
                }
            }

            Button(
                onClick = { viewModel.onIntent(CloudIntent.PerformDeltaSync(teamId, seasonId)) },
                enabled = !state.isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = AresCyan, disabledContainerColor = AresSurfaceElevated)
            ) {
                if (state.isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AresCyan, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = AresBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Now", color = if (state.isSyncing) AresTextTertiary else AresBackground, fontWeight = FontWeight.Bold)
            }
        }

        // Error message if any
        if (state.errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AresRedDark)
                    .border(1.dp, AresRed, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(state.errorMessage!!, color = AresTextPrimary, fontSize = 14.sp)
                TextButton(onClick = { viewModel.onIntent(CloudIntent.ClearError) }) {
                    Text("Dismiss", color = AresTextPrimary)
                }
            }
        }

        // Sessions List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(AresSurface)
                .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            if (state.summaries.isEmpty()) {
                Text("No sessions found. Import a log or run a simulation.", color = AresTextSecondary, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.summaries, key = { it.sessionId }) { summary ->
                        SessionCloudRow(
                            summary = summary,
                            isUploading = state.uploadingSessionId == summary.sessionId,
                            onUpload = { viewModel.onIntent(CloudIntent.UploadSession(summary.sessionId)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCloudRow(
    summary: SessionSummary,
    isUploading: Boolean,
    onUpload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AresBackground, RoundedCornerShape(6.dp))
            .border(1.dp, AresBorder, RoundedCornerShape(6.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Session: ${summary.sessionId.takeLast(8)}", color = AresTextPrimary, fontWeight = FontWeight.Bold)
            Text(
                "Match: ${summary.matchNumber ?: "None"} | Robot: ${summary.robotId} | Date: ${java.util.Date(summary.createdAt)}",
                color = AresTextSecondary,
                fontSize = 12.sp
            )
        }
        
        Button(
            onClick = onUpload,
            enabled = !isUploading,
            colors = ButtonDefaults.buttonColors(containerColor = AresSurfaceElevated)
        ) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AresCyan, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uploading...", color = AresCyan)
            } else {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = AresCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload to Cloud", color = AresCyan)
            }
        }
    }
}
