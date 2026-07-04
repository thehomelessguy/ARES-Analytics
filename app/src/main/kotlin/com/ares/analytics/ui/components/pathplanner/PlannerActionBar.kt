package com.ares.analytics.ui.components.pathplanner

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerActionBar(
    pathName: String,
    availablePaths: List<String>,
    saveStatus: String,
    onPathNameChange: (String) -> Unit,
    onPathSelected: (String) -> Unit,
    onCreateNewPath: () -> Unit,
    onSavePath: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Path Name", fontSize = 11.sp, color = AresTextSecondary, fontWeight = FontWeight.Bold)
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = pathName,
                onValueChange = onPathNameChange,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AresCyan, unfocusedBorderColor = AresBorder),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availablePaths.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p) },
                        onClick = {
                            onPathSelected(p)
                            expanded = false
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCreateNewPath,
                colors = ButtonDefaults.buttonColors(containerColor = AresSurface, contentColor = AresTextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, AresBorder),
                modifier = Modifier.weight(1f)
            ) {
                Text("New Path", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onSavePath,
                colors = ButtonDefaults.buttonColors(containerColor = AresCyan),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Path", color = AresBackground, fontWeight = FontWeight.Bold)
            }
        }
        if (saveStatus.isNotEmpty()) {
            Text(
                text = saveStatus,
                color = if (saveStatus.contains("failed") || saveStatus.contains("No")) AresError else AresGreen,
                fontSize = 11.sp
            )
        }
        HorizontalDivider(color = AresBorder, modifier = Modifier.padding(vertical = 8.dp))
    }
}
