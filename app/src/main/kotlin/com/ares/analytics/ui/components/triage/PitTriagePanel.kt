package com.ares.analytics.ui.components.triage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.shared.ForensicsResponse
import com.ares.analytics.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TriageChecklistItem(
    val action: String,
    val isChecked: Boolean = false,
    val completedAt: Long? = null,
    val completedBy: String? = null
)

@Composable
fun PitTriagePanel(
    diagnostics: ForensicsResponse?,
    currentUser: String,
    modifier: Modifier = Modifier
) {
    // Explicit type declaration to help compiler type inference
    var checklist by remember(diagnostics) {
        mutableStateOf<List<TriageChecklistItem>>(
            diagnostics?.recommendedActions?.map {
                TriageChecklistItem(action = it)
            } ?: emptyList()
        )
    }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxHeight()
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
            Icon(imageVector = Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = AresCyan)
            Text(
                "AI Recommended Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AresTextPrimary
            )
        }

        Divider(color = AresBorder, thickness = 1.dp)

        if (checklist.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No recommended triage actions. Run AI Pit Diagnostics first.", color = AresTextTertiary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(checklist) { idx, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AresSurfaceElevated)
                            .border(1.dp, if (item.isChecked) AresGreen else AresBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                val updated = checklist.toMutableList()
                                val isChecked = !item.isChecked
                                updated[idx] = item.copy(
                                    isChecked = isChecked,
                                    completedAt = if (isChecked) System.currentTimeMillis() else null,
                                    completedBy = if (isChecked) currentUser else null
                                )
                                checklist = updated
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val icon = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank
                        val tint = if (item.isChecked) AresGreen else AresTextTertiary
                        Icon(imageVector = icon, contentDescription = null, tint = tint)

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.action,
                                fontSize = 13.sp,
                                color = if (item.isChecked) AresTextSecondary else AresTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (item.isChecked) {
                                Spacer(Modifier.height(4.dp))
                                val compTime = item.completedAt
                                val compUser = item.completedBy
                                if (compTime != null && compUser != null) {
                                    Text(
                                        "Verified by $compUser at ${dateFormat.format(Date(compTime))}",
                                        fontSize = 10.sp,
                                        color = AresGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
