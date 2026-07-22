package com.ares.analytics.service

import com.ares.analytics.shared.League
import com.ares.analytics.shared.WorkspaceConfig
import com.ares.analytics.shared.AppWorkspaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class EnvironmentService(
    private val configPath: String = System.getProperty("user.home") + "/.ares-analytics/config.json",
    private val workspacesPath: String = System.getProperty("user.home") + "/.ares-analytics/workspaces.json"
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun loadWorkspaces(): AppWorkspaces = withContext(Dispatchers.IO) {
        /**
         * file val.
         */
        val file = File(workspacesPath)
        /**
         * legacyFile val.
         */
        val legacyFile = File(configPath)

        if (file.exists()) {
            try {
                return@withContext json.decodeFromString<AppWorkspaces>(file.readText())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (legacyFile.exists()) {
            try {
                /**
                 * legacyConfig val.
                 */
                val legacyConfig = json.decodeFromString<WorkspaceConfig>(legacyFile.readText())
                /**
                 * migratedId val.
                 */
                val migratedId = legacyConfig.id.ifEmpty { "${legacyConfig.league}-${legacyConfig.teamId}-${legacyConfig.robotId}-${legacyConfig.seasonId}" }
                /**
                 * migratedConfig val.
                 */
                val migratedConfig = legacyConfig.copy(id = migratedId)
                /**
                 * migratedWorkspaces val.
                 */
                val migratedWorkspaces = AppWorkspaces(
                    activeWorkspaceId = migratedId,
                    workspaces = listOf(migratedConfig)
                )
                file.parentFile?.mkdirs()
                file.writeText(json.encodeToString(migratedWorkspaces))
                return@withContext migratedWorkspaces
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        AppWorkspaces(activeWorkspaceId = null, workspaces = emptyList())
    }

    suspend fun saveWorkspaces(appWorkspaces: AppWorkspaces) = withContext(Dispatchers.IO) {
        /**
         * file val.
         */
        val file = File(workspacesPath)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(appWorkspaces))
    }

    suspend fun loadConfig(): WorkspaceConfig? {
        /**
         * app val.
         */
        val app = loadWorkspaces()
        /**
         * baseConfig val.
         */
        val baseConfig = app.workspaces.find { it.id == app.activeWorkspaceId } ?: app.workspaces.firstOrNull()
        if (baseConfig != null) {
            /**
             * aresRobotConfig val.
             */
            val aresRobotConfig = readAresRobotJson(baseConfig.projectPath)
            if (aresRobotConfig != null) {
                return baseConfig.copy(
                    teamId = aresRobotConfig.teamId,
                    seasonId = aresRobotConfig.seasonId,
                    robotId = aresRobotConfig.robotId,
                    robotName = aresRobotConfig.name,
                    league = if (aresRobotConfig.league.equals("FRC", ignoreCase = true)) League.FRC else League.FTC
                )
            }
        }
        return baseConfig
    }

    suspend fun saveConfig(config: WorkspaceConfig) {
        /**
         * app val.
         */
        val app = loadWorkspaces()
        /**
         * configWithId val.
         */
        val configWithId = if (config.id.isEmpty()) {
            config.copy(id = "${config.league}-${config.teamId}-${config.robotId}-${config.seasonId}")
        } else {
            config
        }
        /**
         * newList val.
         */
        val newList = app.workspaces.filter { it.id != configWithId.id } + configWithId
        saveWorkspaces(AppWorkspaces(activeWorkspaceId = configWithId.id, workspaces = newList))
    }

    suspend fun verifyJavaEnvironment(): JavaEnvResult = withContext(Dispatchers.IO) {
        /**
         * javaHome val.
         */
        val javaHome = System.getenv("JAVA_HOME")
        
        /**
         * javaExe val.
         */
        val javaExe = if (!javaHome.isNullOrEmpty()) {
            if (System.getProperty("os.name").contains("win", ignoreCase = true)) {
                "$javaHome\\bin\\java.exe"
            } else {
                "$javaHome/bin/java"
            }
        } else {
            "java" // Fallback to PATH
        }

        if (javaExe != "java" && !File(javaExe).exists()) {
            return@withContext JavaEnvResult(false, "java executable not found at $javaExe")
        }

        try {
            /**
             * process val.
             */
            val process = ProcessBuilder(javaExe, "-version")
                .redirectErrorStream(true)
                .start()
            /**
             * output val.
             */
            val output = process.inputStream.bufferedReader().use { it.readText() }
            /**
             * finished val.
             */
            val finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext JavaEnvResult(false, "Java verification timed out.")
            }
            /**
             * exitCode val.
             */
            val exitCode = process.exitValue()
            if (exitCode == 0) {
                JavaEnvResult(true, "Java executable valid. Output:\n$output")
            } else {
                JavaEnvResult(false, "Java version execution failed with exit code $exitCode. Output:\n$output")
            }
        } catch (e: Exception) {
            JavaEnvResult(false, "Failed to run Java verification: ${e.message}")
        }
    }

    suspend fun detectLeague(projectPath: String): League = withContext(Dispatchers.IO) {
        /**
         * root val.
         */
        val root = File(projectPath)
        if (!root.exists() || !root.isDirectory) return@withContext League.FTC

        // Look for typical FRC indicators: build.gradle/settings.gradle mentioning 'frc', or 'wpilibj'
        // or a build.gradle with wpilib dependency.
        /**
         * searchFiles val.
         */
        val searchFiles = root.walkTopDown().maxDepth(3)
        for (file in searchFiles) {
            if (file.name == "build.gradle" || file.name == "build.gradle.kts") {
                /**
                 * content val.
                 */
                val content = file.readText()
                if (content.contains("edu.wpi.first") || content.contains("wpilibj")) {
                    return@withContext League.FRC
                }
            }
        }

        // Default to FTC (the workspace features TeamCode/ARESLib-Kotlin)
        League.FTC
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun getDefaultNt4Host(league: League, teamId: String): String {
        return when (league) {
            League.FTC -> "192.168.43.1"
            League.FRC -> {
                // FRC team host convention: 10.TE.AM.2
                /**
                 * teamNumber val.
                 */
                val teamNumber = teamId.filter { it.isDigit() }
                if (teamNumber.length in 1..4) {
                    /**
                     * padded val.
                     */
                    val padded = teamNumber.padStart(4, '0')
                    /**
                     * te val.
                     */
                    val te = padded.substring(0, 2).toInt()
                    /**
                     * am val.
                     */
                    val am = padded.substring(2, 4).toInt()
                    "10.$te.$am.2"
                } else {
                    "10.0.0.2"
                }
            }
        }
    }

    suspend fun readAresRobotJson(projectPath: String): AresRobotConfig? = withContext(Dispatchers.IO) {
        /**
         * file val.
         */
        val file = File(projectPath, ".ares-robot.json")
        if (file.exists()) {
            try {
                return@withContext json.decodeFromString<AresRobotConfig>(file.readText())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class JavaEnvResult(
    /**
     * isValid val.
     */
    val isValid: Boolean,
    /**
     * message val.
     */
    val message: String
)

@kotlinx.serialization.Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class AresRobotConfig(
    /**
     * teamId val.
     */
    val teamId: String,
    /**
     * seasonId val.
     */
    val seasonId: String,
    /**
     * robotId val.
     */
    val robotId: String,
    /**
     * name val.
     */
    val name: String = "",
    /**
     * league val.
     */
    val league: String = "FTC"
)
