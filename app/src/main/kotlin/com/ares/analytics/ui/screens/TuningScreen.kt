package com.ares.analytics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.ui.theme.*
import com.ares.analytics.viewmodel.TuningIntent
import com.ares.analytics.viewmodel.TuningViewModel

@Composable
fun TuningScreen(
    viewModel: TuningViewModel,
    projectPath: String // Kept for compatibility but unused
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(AresSurface).border(1.dp, AresBorder, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Constants Tuning Board", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AresTextPrimary)
        HorizontalDivider(color = AresBorder)

        if (state.saveStatus.isNotEmpty()) {
            Text(state.saveStatus, color = AresGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            LaunchedEffect(state.saveStatus) {
                kotlinx.coroutines.delay(3000)
                viewModel.onIntent(TuningIntent.ClearSaveStatus)
            }
        }
        val error = state.errorMessage
        if (error != null) {
            Text(error, color = AresError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (state.variables.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Waiting for live Tuning constants from Robot over NT4...", color = AresTextTertiary, fontSize = 12.sp)
            }
        } else {
            // Group by the first segment after Tuning/ (e.g., Tuning/pathTranslationGains/kP -> pathTranslationGains)
            val grouped = state.variables.entries.groupBy { 
                val parts = it.key.removePrefix("Tuning/").split("/")
                if (parts.size > 1) parts[0] else "General Constants"
            }
            
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 320.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp
            ) {
                items(grouped.entries.toList().sortedBy { it.key }) { (category, constants) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AresSurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, AresBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = getDisplayCategory(category),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AresCyan
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            constants.sortedBy { it.key }.forEach { const ->
                                val constKey = const.key
                                val parts = constKey.removePrefix("Tuning/").split("/")
                                val displayName = if (parts.size > 1) parts.drop(1).joinToString("/") else parts[0]
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(displayName, fontSize = 12.sp, color = AresTextPrimary, fontWeight = FontWeight.SemiBold)
                                    }

                                    var textValue by remember(const.value) { mutableStateOf(const.value.toString()) }
                                    BasicTextField(
                                        value = textValue,
                                        onValueChange = { textValue = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AresTextPrimary),
                                        cursorBrush = SolidColor(AresCyan),
                                        modifier = Modifier
                                            .width(90.dp)
                                            .height(32.dp)
                                            .background(AresSurface, RoundedCornerShape(6.dp))
                                            .border(1.dp, AresBorder, RoundedCornerShape(6.dp)),
                                        decorationBox = { innerTextField ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    if (textValue.isEmpty()) {
                                                        Text("null", fontSize = 11.sp, color = AresTextTertiary)
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        }
                                    )
                                    Button(
                                        onClick = {
                                            val newVal = textValue.toDoubleOrNull()
                                            if (newVal != null) {
                                                viewModel.onIntent(TuningIntent.SaveConstant(constKey, newVal))
                                            }
                                        },
                                        modifier = Modifier.height(32.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AresCyan),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Text("Save", color = AresBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getDisplayCategory(category: String): String {
    return when (category) {
        "pathTranslationGains" -> "Path Translation PID"
        "pathRotationGains" -> "Path Rotation PID"
        "headingGains" -> "Heading Lock PID"
        "driveFeedforward" -> "Drivetrain Feedforward"
        "motorGains" -> "Motor PIDF"
        "General Constants" -> "General Variables"
        else -> category.replace(Regex("([a-z])([A-Z]+)"), "$1 $2").replaceFirstChar { it.uppercase() }
    }
}
