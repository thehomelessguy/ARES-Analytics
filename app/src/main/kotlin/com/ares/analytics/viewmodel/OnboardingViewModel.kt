package com.ares.analytics.viewmodel

import com.ares.analytics.service.EnvironmentService
import com.ares.analytics.shared.League
import com.ares.analytics.shared.WorkspaceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class OnboardingState(
    /**
     * projectPath val.
     */
    val projectPath: String = "",
    /**
     * teamId val.
     */
    val teamId: String = "",
    /**
     * seasonId val.
     */
    val seasonId: String = "",
    /**
     * robotId val.
     */
    val robotId: String = "",
    /**
     * robotName val.
     */
    val robotName: String = "",
    /**
     * league val.
     */
    val league: League = League.FTC,
    /**
     * nt4Host val.
     */
    val nt4Host: String = "192.168.43.1",
    /**
     * googleClientId val.
     */
    val googleClientId: String = "205869391101-nlcsea4539vjuo50i58bpo0t10d5s0ic.apps.googleusercontent.com",
    /**
     * googleClientSecret val.
     */
    val googleClientSecret: String = "",
    /**
     * isVerifyingJava val.
     */
    val isVerifyingJava: Boolean = false,
    /**
     * javaEnvValid val.
     */
    val javaEnvValid: Boolean? = null,
    /**
     * javaEnvMsg val.
     */
    val javaEnvMsg: String = "",
    /**
     * isSaving val.
     */
    val isSaving: Boolean = false,
    /**
     * saveSuccess val.
     */
    val saveSuccess: Boolean = false,
    /**
     * errorMessage val.
     */
    val errorMessage: String? = null,
    /**
     * simulatorCommand val.
     */
    val simulatorCommand: String = "",
    /**
     * cloudRobots val.
     */
    val cloudRobots: List<com.ares.analytics.shared.RobotProfile> = emptyList(),
    /**
     * isCloudLoading val.
     */
    val isCloudLoading: Boolean = false,
    /**
     * selectedOptionText val.
     */
    val selectedOptionText: String = "Select Robot Profile..."
)

sealed class OnboardingIntent {
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateProjectPath(val projectPath: String) : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateTeamId(val teamId: String) : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateSeasonId(val seasonId: String) : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateRobotId(val robotId: String) : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateRobotName(val robotName: String) : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateLeague(val league: League) : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateNt4Host(val nt4Host: String) : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateGoogleClientId(val googleClientId: String) : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateGoogleClientSecret(val googleClientSecret: String) : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateSimulatorCommand(val simulatorCommand: String) : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateSelectedOptionText(val text: String) : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class FetchCloudRobots(val token: String) : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object DetectLeague : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object VerifyJava : OnboardingIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object SubmitConfig : OnboardingIntent()
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class OnboardingViewModel(
    private val environmentService: EnvironmentService,
    private val teamApiService: com.ares.analytics.service.TeamApiService,
    private val scope: CoroutineScope,
    private val onConfigured: (WorkspaceConfig) -> Unit
) {
    private val _state = MutableStateFlow(OnboardingState())
    /**
     * state val.
     */
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        handleIntent(OnboardingIntent.VerifyJava)
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun handleIntent(intent: OnboardingIntent) {
        scope.launch {
            when (intent) {
                is OnboardingIntent.UpdateProjectPath -> _state.update { it.copy(projectPath = intent.projectPath) }
                is OnboardingIntent.UpdateTeamId -> _state.update { it.copy(teamId = intent.teamId) }
                is OnboardingIntent.UpdateSeasonId -> _state.update { it.copy(seasonId = intent.seasonId) }
                is OnboardingIntent.UpdateRobotId -> _state.update { it.copy(robotId = intent.robotId) }
                is OnboardingIntent.UpdateRobotName -> _state.update { it.copy(robotName = intent.robotName) }
                is OnboardingIntent.UpdateLeague -> _state.update { it.copy(league = intent.league) }
                is OnboardingIntent.UpdateNt4Host -> _state.update { it.copy(nt4Host = intent.nt4Host) }
                is OnboardingIntent.UpdateGoogleClientId -> _state.update { it.copy(googleClientId = intent.googleClientId) }
                is OnboardingIntent.UpdateGoogleClientSecret -> _state.update { it.copy(googleClientSecret = intent.googleClientSecret) }
                is OnboardingIntent.UpdateSimulatorCommand -> _state.update { it.copy(simulatorCommand = intent.simulatorCommand) }
                is OnboardingIntent.UpdateSelectedOptionText -> {
                    _state.update { it.copy(selectedOptionText = intent.text) }
                }
                is OnboardingIntent.FetchCloudRobots -> {
                    /**
                     * currentTeamId val.
                     */
                    val currentTeamId = _state.value.teamId
                    if (currentTeamId.isNotEmpty() && intent.token.isNotEmpty()) {
                        _state.update { it.copy(isCloudLoading = true) }
                        scope.launch {
                            try {
                                /**
                                 * robots val.
                                 */
                                val robots = teamApiService.fetchTeamRobots(currentTeamId, intent.token)
                                _state.update { it.copy(cloudRobots = robots, isCloudLoading = false) }
                            } catch (e: Exception) {
                                _state.update { it.copy(cloudRobots = emptyList(), isCloudLoading = false) }
                            }
                        }
                    } else {
                        _state.update { it.copy(cloudRobots = emptyList()) }
                    }
                }
                is OnboardingIntent.DetectLeague -> {
                    /**
                     * path val.
                     */
                    val path = _state.value.projectPath
                    if (path.isNotEmpty() && File(path).isDirectory) {
                        /**
                         * aresRobotConfig val.
                         */
                        val aresRobotConfig = environmentService.readAresRobotJson(path)
                        if (aresRobotConfig != null) {
                            /**
                             * detectedLeague val.
                             */
                            val detectedLeague = if (aresRobotConfig.league.equals("FRC", ignoreCase = true)) League.FRC else League.FTC
                            /**
                             * defaultHost val.
                             */
                            val defaultHost = environmentService.getDefaultNt4Host(detectedLeague, aresRobotConfig.teamId)
                            _state.update {
                                it.copy(
                                    teamId = aresRobotConfig.teamId,
                                    seasonId = aresRobotConfig.seasonId,
                                    robotId = aresRobotConfig.robotId,
                                    robotName = aresRobotConfig.name,
                                    league = detectedLeague,
                                    nt4Host = defaultHost
                                )
                            }
                        } else {
                            /**
                             * detectedLeague val.
                             */
                            val detectedLeague = environmentService.detectLeague(path)
                            /**
                             * defaultHost val.
                             */
                            val defaultHost = environmentService.getDefaultNt4Host(detectedLeague, _state.value.teamId)
                            _state.update {
                                it.copy(
                                    league = detectedLeague,
                                    nt4Host = defaultHost
                                )
                            }
                        }
                    }
                }
                is OnboardingIntent.VerifyJava -> {
                    _state.update { it.copy(isVerifyingJava = true) }
                    /**
                     * result val.
                     */
                    val result = environmentService.verifyJavaEnvironment()
                    _state.update {
                        it.copy(
                            isVerifyingJava = false,
                            javaEnvValid = result.isValid,
                            javaEnvMsg = result.message
                        )
                    }
                }
                is OnboardingIntent.SubmitConfig -> {
                    /**
                     * currentState val.
                     */
                    val currentState = _state.value
                    if (currentState.projectPath.isEmpty() || currentState.teamId.isEmpty() ||
                        currentState.seasonId.isEmpty() || currentState.robotId.isEmpty()) {
                        _state.update { it.copy(errorMessage = "All fields are required.") }
                        return@launch
                    }

                    /**
                     * projectDir val.
                     */
                    val projectDir = File(currentState.projectPath)
                    if (!projectDir.exists() || !projectDir.isDirectory) {
                        _state.update { it.copy(errorMessage = "Project path must be a valid directory.") }
                        return@launch
                    }

                    _state.update { it.copy(isSaving = true, errorMessage = null) }
                    try {
                        /**
                         * config val.
                         */
                        val config = WorkspaceConfig(
                            teamId = currentState.teamId,
                            seasonId = currentState.seasonId,
                            robotId = currentState.robotId,
                            robotName = currentState.robotName,
                            projectPath = currentState.projectPath,
                            league = currentState.league,
                            nt4Host = currentState.nt4Host.takeIf { it.isNotEmpty() },
                            googleClientId = currentState.googleClientId.takeIf { it.isNotEmpty() },
                            googleClientSecret = currentState.googleClientSecret.takeIf { it.isNotEmpty() },
                            simulatorCommand = currentState.simulatorCommand.takeIf { it.isNotEmpty() }
                        )
                        environmentService.saveConfig(config)
                        
                        // Upload local robot to Firebase
                        try {
                            /**
                             * profile val.
                             */
                            val profile = com.ares.analytics.shared.RobotProfile(
                                robotId = currentState.robotId,
                                league = currentState.league,
                                seasonId = currentState.seasonId,
                                name = currentState.robotName.ifEmpty { "${currentState.robotId} Local Config" }
                            )
                            teamApiService.addRobotProfile(currentState.teamId, profile)
                        } catch (e: Exception) {
                            // Silently fail if they aren't signed in, but maybe log it
                            e.printStackTrace()
                        }
                        
                        _state.update { it.copy(isSaving = false, saveSuccess = true) }
                        onConfigured(config)
                    } catch (e: Exception) {
                        _state.update { it.copy(isSaving = false, errorMessage = "Failed to save configuration: ${e.message}") }
                    }
                }
            }
        }
    }
}
