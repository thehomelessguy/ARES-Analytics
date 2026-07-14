package com.ares.analytics.ui.components.dashboard
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.ui.theme.AresTextPrimary
import com.ares.analytics.ui.theme.AresTextSecondary
import com.ares.analytics.ui.theme.AresSurface
import com.ares.analytics.ui.theme.AresBorder

@Composable
fun TuningCard(
    nt4ClientService: Nt4ClientService,
    modifier: Modifier = Modifier
) {
    val tuningVariables = listOf(
        "trackWidthMeters",
        "wheelBaseMeters",
        "pathTranslationGains/kP",
        "pathTranslationGains/kI",
        "pathTranslationGains/kD",
        "pathRotationGains/kP",
        "pathRotationGains/kI",
        "pathRotationGains/kD",
        "headingGains/kP",
        "headingGains/kI",
        "headingGains/kD",
        "headingDeadzoneDeg",
        "driveFeedforward/kS",
        "driveFeedforward/kV",
        "driveFeedforward/kA",
        "driveSlewRateLimit",
        "motorGains/kP",
        "motorGains/kI",
        "motorGains/kD",
        "motorGains/kF",
        "visionStdDevsX",
        "visionStdDevsY",
        "visionStdDevsHeading",
        "visionMaxDistanceMeters",
        "visionMaxAmbiguity",
        "visionMahalanobisThreshold",
        "driverDeadbandExponent",
        "driverSlewRateLimit"
    )

    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(AresSurface)
            .border(1.dp, AresBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = AresSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Live Tuning",
                color = AresTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tuningVariables.forEach { varName ->
                    TuningRow(nt4ClientService, varName)
                }
            }
        }
    }
}

@Composable
private fun TuningRow(nt4ClientService: Nt4ClientService, name: String) {
    val ntKey = "Tuning/$name"
    
    // Create state that updates when the value from NT4 changes, but also allows local edits
    val ntValue = nt4ClientService.subscribeDouble(ntKey).collectAsState(initial = 0.0)
    var textValue by remember(ntValue.value) { mutableStateOf(ntValue.value.toString()) }

    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            color = AresTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        
        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                newValue.toDoubleOrNull()?.let {
                    coroutineScope.launch {
                        nt4ClientService.publishDouble(ntKey, it)
                    }
                }
            },
            modifier = Modifier.width(120.dp).height(48.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            singleLine = true
        )
    }
}
