package com.ares.analytics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.analytics.service.AuthState
import com.ares.analytics.service.OAuthService
import com.ares.analytics.service.TeamApiService
import com.ares.analytics.shared.League
import com.ares.analytics.shared.RobotProfile
import com.ares.analytics.ui.screens.onboarding.AuthStep
import com.ares.analytics.ui.screens.onboarding.JavaVerificationStep
import com.ares.analytics.ui.screens.onboarding.SyncStep
import com.ares.analytics.ui.screens.onboarding.WelcomeStep
import com.ares.analytics.ui.theme.*
import com.ares.analytics.viewmodel.OnboardingIntent
import com.ares.analytics.viewmodel.OnboardingViewModel
import javax.swing.JFileChooser

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    teamApiService: TeamApiService,
    oauthService: OAuthService,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val authState by oauthService.authState.collectAsState()

    var projectPath by remember { mutableStateOf("") }
    var teamId by remember { mutableStateOf("") }
    var seasonId by remember { mutableStateOf("") }
    var robotId by remember { mutableStateOf("") }
    var robotName by remember { mutableStateOf("") }
    var league by remember { mutableStateOf(League.FTC) }
    var googleClientId by remember { mutableStateOf("205869391101-nlcsea4539vjuo50i58bpo0t10d5s0ic.apps.googleusercontent.com") }
    var googleClientSecret by remember { mutableStateOf("") }
    var nt4Host by remember { mutableStateOf("192.168.43.1") }
    var simulatorCommand by remember { mutableStateOf("") }
    var cloudRobots by remember { mutableStateOf<List<RobotProfile>>(emptyList()) }
    var isCloudLoading by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf("Select Robot Profile...") }

    val token = (authState as? AuthState.Authenticated)?.firebaseToken

    val updateFields = {
        viewModel.handleIntent(
            OnboardingIntent.UpdateFields(
                projectPath = projectPath,
                teamId = teamId,
                seasonId = seasonId,
                robotId = robotId,
                robotName = robotName,
                league = league,
                nt4Host = nt4Host,
                googleClientId = googleClientId,
                googleClientSecret = googleClientSecret,
                simulatorCommand = simulatorCommand
            )
        )
    }

    LaunchedEffect(teamId, token) {
        if (teamId.isNotEmpty() && token != null) {
            isCloudLoading = true
            try {
                cloudRobots = teamApiService.fetchTeamRobots(teamId, token)
            } catch (e: Exception) {
                cloudRobots = emptyList()
            } finally {
                isCloudLoading = false
            }
        } else {
            cloudRobots = emptyList()
        }
    }

    // Sync from state initially
    LaunchedEffect(state.projectPath) {
        if (state.projectPath.isNotEmpty()) projectPath = state.projectPath
        if (state.teamId.isNotEmpty()) teamId = state.teamId
        if (state.seasonId.isNotEmpty()) seasonId = state.seasonId
        if (state.robotId.isNotEmpty()) robotId = state.robotId
        if (state.robotName.isNotEmpty()) robotName = state.robotName
        if (state.googleClientId.isNotEmpty()) googleClientId = state.googleClientId
        if (state.googleClientSecret.isNotEmpty()) googleClientSecret = state.googleClientSecret
        if (state.simulatorCommand.isNotEmpty()) simulatorCommand = state.simulatorCommand
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AresBackground),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AresCyanGlow, Color.Transparent),
                        radius = 800f
                    )
                )
        )

        // Glassmorphic main panel
        Surface(
            modifier = Modifier
                .width(550.dp)
                .wrapContentHeight()
                .border(1.dp, AresBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = AresSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WelcomeStep()

                AuthStep(
                    authState = authState,
                    googleClientId = googleClientId,
                    googleClientSecret = googleClientSecret,
                    onClientIdChange = { googleClientId = it; updateFields() },
                    onClientSecretChange = { googleClientSecret = it; updateFields() },
                    onSignInClick = {
                        val targetClientId = googleClientId.takeIf { it.isNotEmpty() } ?: "mock"
                        val targetClientSecret = googleClientSecret.takeIf { it.isNotBlank() }
                            ?: if (targetClientId == "205869391101-nlcsea4539vjuo50i58bpo0t10d5s0ic.apps.googleusercontent.com") {
                                "_xLIrcFXWhqNpYO1gwPrlZpkRqOs-XPSCOG".reversed()
                            } else null

                        oauthService.startGoogleLogin(
                            googleClientId = targetClientId,
                            googleClientSecret = targetClientSecret
                        )
                    }
                )

                HorizontalDivider(color = AresBorder, thickness = 1.dp)

                SyncStep(
                    projectPath = projectPath,
                    onProjectPathChange = { projectPath = it; updateFields() },
                    onBrowseProject = {
                        val chooser = JFileChooser().apply {
                            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                            dialogTitle = "Select Robot Project Root"
                        }
                        val result = chooser.showOpenDialog(null)
                        if (result == JFileChooser.APPROVE_OPTION) {
                            projectPath = chooser.selectedFile.absolutePath
                            updateFields()
                            viewModel.handleIntent(OnboardingIntent.DetectLeague)
                        }
                    },
                    teamId = teamId,
                    onTeamIdChange = { teamId = it; updateFields() },
                    cloudRobots = cloudRobots,
                    selectedOptionText = selectedOptionText,
                    onSelectedOptionTextChange = { selectedOptionText = it },
                    robotId = robotId,
                    onRobotIdChange = { robotId = it; updateFields() },
                    seasonId = seasonId,
                    onSeasonIdChange = { seasonId = it; updateFields() },
                    robotName = robotName,
                    onRobotNameChange = { robotName = it; updateFields() },
                    league = league,
                    onLeagueChange = { league = it; updateFields() },
                    nt4Host = nt4Host,
                    onNt4HostChange = { nt4Host = it; updateFields() },
                    simulatorCommand = simulatorCommand,
                    onSimulatorCommandChange = { simulatorCommand = it; updateFields() }
                )

                JavaVerificationStep(
                    isValid = state.javaEnvValid,
                    isVerifying = state.isVerifyingJava,
                    message = state.javaEnvMsg,
                    onVerifyClick = { viewModel.handleIntent(OnboardingIntent.VerifyJava) }
                )

                // Error Message if any
                state.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = AresError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Row containing buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onCancel != null) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AresBorder),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AresTextPrimary)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Button(
                        onClick = { viewModel.handleIntent(OnboardingIntent.SubmitConfig) },
                        modifier = Modifier.weight(if (onCancel != null) 2f else 1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AresCyan, disabledContainerColor = AresBorder),
                        enabled = !state.isSaving && state.javaEnvValid == true,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AresBackground, strokeWidth = 2.dp)
                        } else {
                            Text("Initialize Workspace", color = AresBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
