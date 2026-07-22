package com.ares.analytics.viewmodel

import com.ares.analytics.service.DatabaseService
import com.ares.analytics.service.FirebaseClientService
import com.ares.analytics.service.SyncEngineService
import com.ares.analytics.service.Nt4ClientService
import com.ares.analytics.service.LogParserService
import com.ares.analytics.shared.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class RobotLogFileInfo(
    /**
     * name val.
     */
    val name: String,
    /**
     * sizeBytes val.
     */
    val sizeBytes: Long,
    /**
     * lastModifiedMs val.
     */
    val lastModifiedMs: Long,
    /**
     * lastModifiedFmt val.
     */
    val lastModifiedFmt: String,
    /**
     * synced val.
     */
    val synced: Boolean,
    /**
     * isActive val.
     */
    val isActive: Boolean? = false
)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class RobotRun(
    /**
     * runId val.
     */
    val runId: String,
    /**
     * files val.
     */
    val files: List<RobotLogFileInfo>,
    /**
     * totalSizeBytes val.
     */
    val totalSizeBytes: Long,
    /**
     * lastModifiedMs val.
     */
    val lastModifiedMs: Long,
    /**
     * lastModifiedFmt val.
     */
    val lastModifiedFmt: String,
    /**
     * allSynced val.
     */
    val allSynced: Boolean,
    /**
     * isActive val.
     */
    val isActive: Boolean = false
)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class SessionSyncInfo(
    /**
     * summary val.
     */
    val summary: SessionSummary,
    /**
     * isLocal val.
     */
    val isLocal: Boolean,
    /**
     * isRemote val.
     */
    val isRemote: Boolean
)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class CloudState(
    /**
     * sessions val.
     */
    val sessions: List<SessionSyncInfo> = emptyList(),
    /**
     * cloudLogs val.
     */
    val cloudLogs: List<SessionSummary> = emptyList(),
    /**
     * robotRuns val.
     */
    val robotRuns: List<RobotRun> = emptyList(),
    /**
     * isSyncing val.
     */
    val isSyncing: Boolean = false,
    /**
     * isFetchingRobotLogs val.
     */
    val isFetchingRobotLogs: Boolean = false,
    /**
     * isUploadingRobotLog val.
     */
    val isUploadingRobotLog: String? = null,
    /**
     * isDeletingCloudLog val.
     */
    val isDeletingCloudLog: String? = null,
    /**
     * errorMessage val.
     */
    val errorMessage: String? = null,
    /**
     * isAuthenticated val.
     */
    val isAuthenticated: Boolean = false,
    /**
     * uploadLogs val.
     */
    val uploadLogs: List<String> = emptyList()
)

sealed class CloudIntent {
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object RefreshCloudLogs : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object RefreshRobotLogs : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class PerformDeltaSync(val teamId: String, val seasonId: String) : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UploadRobotRun(val runId: String, val teamId: String, val seasonId: String, val robotId: String) : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UploadMultipleRobotRuns(val runIds: List<String>, val teamId: String, val seasonId: String, val robotId: String) : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteRobotRun(val runId: String) : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteMultipleRobotRuns(val runIds: List<String>) : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteCloudLog(val sessionId: String, val teamId: String) : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object ClearError : CloudIntent()

    // Database / Cloud Sync Manager Intents
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UploadSession(val sessionId: String) : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DownloadSession(val summary: SessionSummary) : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DownloadMultipleSessions(val summaries: List<SessionSummary>) : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteSessionLocal(val sessionId: String) : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteMultipleLocalSessions(val sessionIds: List<String>) : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteSessionRemote(val sessionId: String, val teamId: String) : CloudIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteMultipleRemoteSessions(val sessionIdsAndTeamIds: List<Pair<String, String>>) : CloudIntent()
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class CloudViewModel(
    private val databaseService: DatabaseService,
    private val syncEngineService: SyncEngineService,
    private val firebaseClientService: FirebaseClientService,
    private val nt4ClientService: Nt4ClientService,
    private val logParserService: LogParserService,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(CloudState())
    /**
     * state val.
     */
    val state: StateFlow<CloudState> = _state.asStateFlow()

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30 * 60 * 1000L
            connectTimeoutMillis = 60 * 1000L
            socketTimeoutMillis = 30 * 60 * 1000L
        }
    }

    init {
        checkAuth()
        onIntent(CloudIntent.RefreshCloudLogs)
        onIntent(CloudIntent.RefreshRobotLogs)
    }

    private fun checkAuth() {
        /**
         * hasToken val.
         */
        val hasToken = firebaseClientService.getFirebaseToken() != null || firebaseClientService.isDevMode()
        _state.update { it.copy(isAuthenticated = hasToken) }
    }

    private fun getRobotIp(): String {
        /**
         * ip val.
         */
        val ip = nt4ClientService.serverIp
        if (ip.isBlank() || ip == "0.0.0.0") return "127.0.0.1"
        return ip
    }

    private fun logUpload(message: String) {
        _state.update { it.copy(uploadLogs = it.uploadLogs + message) }
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun onIntent(intent: CloudIntent) {
        scope.launch {
            when (intent) {
                is CloudIntent.RefreshCloudLogs -> {
                    checkAuth()
                    _state.update { it.copy(isSyncing = true, errorMessage = null) }
                    try {
                        /**
                         * localSessions val.
                         */
                        val localSessions = databaseService.getSessions()
                        /**
                         * localSummariesMap val.
                         */
                        val localSummariesMap = databaseService.getAllSessionSummaries().associateBy { it.sessionId }
                        /**
                         * remoteSummaries val.
                         */
                        val remoteSummaries = syncEngineService.getRemoteSummaries()

                        /**
                         * allSessionIds val.
                         */
                        val allSessionIds = (localSessions.map { it.sessionId } + remoteSummaries.map { it.sessionId }).toSet()

                        /**
                         * sessionsList val.
                         */
                        val sessionsList = allSessionIds.map { id ->
                            /**
                             * localSession val.
                             */
                            val localSession = localSessions.find { it.sessionId == id }
                            /**
                             * remoteSummary val.
                             */
                            val remoteSummary = remoteSummaries.find { it.sessionId == id }
                            /**
                             * localSummary val.
                             */
                            val localSummary = localSummariesMap[id]

                            /**
                             * summary val.
                             */
                            val summary = remoteSummary
                                ?: localSummary
                                ?: SessionSummary(
                                    sessionId = id,
                                    teamId = localSession?.teamId ?: "unknown",
                                    seasonId = localSession?.seasonId ?: "unknown",
                                    robotId = localSession?.robotId ?: "unknown",
                                    createdAt = localSession?.createdAt ?: System.currentTimeMillis(),
                                    durationMs = localSession?.durationMs ?: 0L,
                                    tags = localSession?.tags ?: emptyList(),
                                    matchNumber = localSession?.matchNumber,
                                    allianceColor = localSession?.allianceColor,
                                    fileSizeBytes = 0L
                                )

                            SessionSyncInfo(
                                summary = summary,
                                isLocal = localSession != null,
                                isRemote = remoteSummary != null
                            )
                        }.sortedByDescending { it.summary.createdAt }

                        _state.update { it.copy(sessions = sessionsList, cloudLogs = remoteSummaries, isSyncing = false) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(isSyncing = false, errorMessage = e.message ?: "Failed to load database state") }
                    }
                }
                is CloudIntent.RefreshRobotLogs -> {
                    fetchRobotLogs()
                }
                is CloudIntent.PerformDeltaSync -> {
                    _state.update { it.copy(isSyncing = true, errorMessage = null) }
                    try {
                        syncEngineService.performDeltaSync(intent.teamId, intent.seasonId)
                        onIntent(CloudIntent.RefreshCloudLogs)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(isSyncing = false, errorMessage = e.message ?: "Sync failed") }
                    }
                }
                is CloudIntent.UploadRobotRun -> {
                    _state.update { it.copy(isUploadingRobotLog = intent.runId, errorMessage = null, uploadLogs = listOf("Starting upload for run ${intent.runId}...")) }
                    try {
                        /**
                         * run val.
                         */
                        val run = _state.value.robotRuns.find { it.runId == intent.runId }
                        if (run != null) {
                            /**
                             * errors val.
                             */
                            val errors = mutableListOf<String>()
                            /**
                             * downloadedFiles val.
                             */
                            val downloadedFiles = mutableListOf<File>()

                            logUpload("1/5: Downloading ${run.files.size} raw files from robot at ${getRobotIp()}...")
                            for (file in run.files) {
                                try {
                                    /**
                                     * tempFile val.
                                     */
                                    val tempFile = withContext(Dispatchers.IO) {
                                        /**
                                         * tempDir val.
                                         */
                                        val tempDir = File(System.getProperty("java.io.tmpdir"), "ares-raw-upload")
                                        tempDir.mkdirs()
                                        /**
                                         * f val.
                                         */
                                        val f = File(tempDir, file.name)
                                        httpClient.prepareGet("http://${getRobotIp()}:5002/api/download?file=${file.name}").execute { response ->
                                            /**
                                             * channel val.
                                             */
                                            val channel = response.bodyAsChannel()
                                            java.io.FileOutputStream(f).use { outputStream ->
                                                channel.copyTo(outputStream)
                                            }
                                        }
                                        f
                                    }
                                    downloadedFiles.add(tempFile)
                                    logUpload("      -> Downloaded ${file.name} (${tempFile.length() / 1024} KB)")
                                } catch (e: Exception) {
                                    errors.add("${file.name}: ${e.message}")
                                    logUpload("      -> Error downloading ${file.name}: ${e.message}")
                                }
                            }

                            if (errors.isEmpty() && downloadedFiles.isNotEmpty()) {
                                logUpload("2/5: Skipping raw file archival (database sync only)...")

                                /**
                                 * totalSizeKb val.
                                 */
                                val totalSizeKb = downloadedFiles.sumOf { it.length() } / 1024
                                logUpload("3/5: Parsing ${downloadedFiles.size} log files (${totalSizeKb} KB) into DuckDB...")
                                /**
                                 * session val.
                                 */
                                val session = logParserService.parseLogFiles(
                                    files = downloadedFiles,
                                    teamId = intent.teamId,
                                    seasonId = intent.seasonId,
                                    robotId = intent.robotId
                                )
                                logUpload("      -> Parsed session: ${session.sessionId} (${session.durationMs?.let { "${it / 1000}s" } ?: "unknown"} duration)")

                                logUpload("4/5: Pushing DuckDB Parquet blob to Cloud & syncing...")
                                try {
                                    syncEngineService.uploadSession(session.sessionId)
                                    syncEngineService.performDeltaSync(intent.teamId, intent.seasonId)
                                    logUpload("      -> Cloud sync completed successfully.")
                                } catch (syncEx: Exception) {
                                    errors.add("Imported locally but cloud sync failed: ${syncEx.message}")
                                    logUpload("      -> Cloud sync failed: ${syncEx.message}")
                                }

                                downloadedFiles.forEach { it.delete() }

                                logUpload("5/5: Deleting remote files from Robot...")
                                if (errors.isEmpty()) {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            for (file in run.files) {
                                                httpClient.preparePost("http://${getRobotIp()}:5002/api/delete") {
                                                    parameter("file", file.name)
                                                }.execute {}
                                            }
                                        }
                                    } catch (deleteEx: Exception) {
                                        errors.add("Uploaded successfully but robot cleanup failed: ${deleteEx.message}")
                                    }
                                }

                                logUpload("Upload finished! Refreshing UI...")
                                fetchRobotLogs()
                                onIntent(CloudIntent.RefreshCloudLogs)
                            } else {
                                logUpload("Upload encountered errors. Aborting.")
                                _state.update { it.copy(errorMessage = "Upload errors:\n" + errors.joinToString("\n"), isUploadingRobotLog = null) }
                            }
                        } else {
                            logUpload("Run not found in local state.")
                            _state.update { it.copy(isUploadingRobotLog = null) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        logUpload("CRITICAL FATAL: ${e.message}")
                        _state.update { it.copy(errorMessage = e.message ?: "Upload failed", isUploadingRobotLog = null) }
                    } finally {
                        _state.update { it.copy(isUploadingRobotLog = null) }
                    }
                }
                is CloudIntent.UploadMultipleRobotRuns -> {
                    _state.update { it.copy(isUploadingRobotLog = "BATCH", errorMessage = null, uploadLogs = listOf("Starting batch upload for ${intent.runIds.size} runs...")) }
                    try {
                        /**
                         * runsToUpload val.
                         */
                        val runsToUpload = _state.value.robotRuns.filter { it.runId in intent.runIds }
                        for ((index, run) in runsToUpload.withIndex()) {
                            logUpload("=== [${index + 1}/${runsToUpload.size}] Uploading Run: ${run.runId} ===")
                            /**
                             * errors val.
                             */
                            val errors = mutableListOf<String>()
                            /**
                             * downloadedFiles val.
                             */
                            val downloadedFiles = mutableListOf<File>()

                            logUpload("Downloading ${run.files.size} raw files from robot...")
                            for (file in run.files) {
                                try {
                                    /**
                                     * tempFile val.
                                     */
                                    val tempFile = withContext(Dispatchers.IO) {
                                        /**
                                         * tempDir val.
                                         */
                                        val tempDir = File(System.getProperty("java.io.tmpdir"), "ares-raw-upload")
                                        tempDir.mkdirs()
                                        /**
                                         * f val.
                                         */
                                        val f = File(tempDir, file.name)
                                        httpClient.prepareGet("http://${getRobotIp()}:5002/api/download?file=${file.name}").execute { response ->
                                            /**
                                             * channel val.
                                             */
                                            val channel = response.bodyAsChannel()
                                            java.io.FileOutputStream(f).use { outputStream ->
                                                channel.copyTo(outputStream)
                                            }
                                        }
                                        f
                                    }
                                    downloadedFiles.add(tempFile)
                                    logUpload("  -> Downloaded ${file.name} (${tempFile.length() / 1024} KB)")
                                } catch (e: Exception) {
                                    errors.add("${file.name}: ${e.message}")
                                    logUpload("  -> Error downloading ${file.name}: ${e.message}")
                                }
                            }

                            if (errors.isEmpty() && downloadedFiles.isNotEmpty()) {
                                logUpload("Parsing ${downloadedFiles.size} log files into DuckDB...")
                                /**
                                 * session val.
                                 */
                                val session = logParserService.parseLogFiles(
                                    files = downloadedFiles,
                                    teamId = intent.teamId,
                                    seasonId = intent.seasonId,
                                    robotId = intent.robotId
                                )
                                logUpload("  -> Parsed session: ${session.sessionId}")

                                logUpload("Pushing DuckDB Parquet blob to Cloud...")
                                try {
                                    syncEngineService.uploadSession(session.sessionId)
                                    logUpload("  -> Cloud upload completed.")
                                } catch (syncEx: Exception) {
                                    errors.add("Imported locally but cloud upload failed: ${syncEx.message}")
                                    logUpload("  -> Cloud upload failed: ${syncEx.message}")
                                }

                                downloadedFiles.forEach { it.delete() }

                                logUpload("Deleting remote files from Robot...")
                                if (errors.isEmpty()) {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            for (file in run.files) {
                                                httpClient.preparePost("http://${getRobotIp()}:5002/api/delete") {
                                                    parameter("file", file.name)
                                                }.execute {}
                                            }
                                        }
                                    } catch (deleteEx: Exception) {
                                        errors.add("Uploaded successfully but robot cleanup failed: ${deleteEx.message}")
                                    }
                                }
                            } else {
                                logUpload("Run ${run.runId} upload encountered errors. Skipping cleanup.")
                            }
                        }
                        logUpload("Batch upload finished! Syncing metadata...")
                        syncEngineService.performDeltaSync(intent.teamId, intent.seasonId)
                        fetchRobotLogs()
                        onIntent(CloudIntent.RefreshCloudLogs)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(errorMessage = "Batch upload failed: ${e.message}", isUploadingRobotLog = null) }
                    } finally {
                        _state.update { it.copy(isUploadingRobotLog = null) }
                    }
                }
                is CloudIntent.DeleteRobotRun -> {
                    try {
                        /**
                         * run val.
                         */
                        val run = _state.value.robotRuns.find { it.runId == intent.runId }
                        if (run != null) {
                            withContext(Dispatchers.IO) {
                                for (file in run.files) {
                                    httpClient.preparePost("http://${getRobotIp()}:5002/api/delete") {
                                        parameter("file", file.name)
                                    }.execute {}
                                }
                            }
                            fetchRobotLogs()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(errorMessage = e.message ?: "Delete request failed") }
                    }
                }
                is CloudIntent.DeleteMultipleRobotRuns -> {
                    try {
                        /**
                         * runsToDelete val.
                         */
                        val runsToDelete = _state.value.robotRuns.filter { it.runId in intent.runIds }
                        withContext(Dispatchers.IO) {
                            for (run in runsToDelete) {
                                for (file in run.files) {
                                    httpClient.preparePost("http://${getRobotIp()}:5002/api/delete") {
                                        parameter("file", file.name)
                                    }.execute {}
                                }
                            }
                        }
                        fetchRobotLogs()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(errorMessage = e.message ?: "Delete request failed") }
                    }
                }

                is CloudIntent.DeleteCloudLog -> {
                    _state.update { it.copy(isDeletingCloudLog = intent.sessionId, errorMessage = null) }
                    try {
                        syncEngineService.deleteCloudSession(intent.sessionId, intent.teamId)
                        onIntent(CloudIntent.RefreshCloudLogs)
                    } catch (e: SecurityException) {
                        _state.update { it.copy(isDeletingCloudLog = null, errorMessage = "Permission denied") }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(isDeletingCloudLog = null, errorMessage = e.message ?: "Delete failed") }
                    }
                }
                is CloudIntent.UploadSession -> {
                    _state.update { it.copy(isSyncing = true, errorMessage = null) }
                    try {
                        syncEngineService.uploadSession(intent.sessionId)
                        onIntent(CloudIntent.RefreshCloudLogs)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(isSyncing = false, errorMessage = e.message ?: "Upload failed") }
                    }
                }
                is CloudIntent.DownloadSession -> {
                    _state.update { it.copy(isSyncing = true, errorMessage = null) }
                    try {
                        syncEngineService.downloadSession(intent.summary)
                        onIntent(CloudIntent.RefreshCloudLogs)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(isSyncing = false, errorMessage = e.message ?: "Download failed") }
                    }
                }
                is CloudIntent.DownloadMultipleSessions -> {
                    _state.update { it.copy(isSyncing = true, errorMessage = null) }
                    try {
                        for (summary in intent.summaries) {
                            syncEngineService.downloadSession(summary)
                        }
                        onIntent(CloudIntent.RefreshCloudLogs)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(isSyncing = false, errorMessage = e.message ?: "Batch download failed") }
                    }
                }
                is CloudIntent.DeleteSessionLocal -> {
                    _state.update { it.copy(isSyncing = true, errorMessage = null) }
                    try {
                        databaseService.deleteSession(intent.sessionId)
                        onIntent(CloudIntent.RefreshCloudLogs)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(isSyncing = false, errorMessage = e.message ?: "Local delete failed") }
                    }
                }
                is CloudIntent.DeleteMultipleLocalSessions -> {
                    _state.update { it.copy(isSyncing = true, errorMessage = null) }
                    try {
                        for (sessionId in intent.sessionIds) {
                            databaseService.deleteSession(sessionId)
                        }
                        onIntent(CloudIntent.RefreshCloudLogs)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(isSyncing = false, errorMessage = e.message ?: "Batch local delete failed") }
                    }
                }
                is CloudIntent.DeleteSessionRemote -> {
                    _state.update { it.copy(isSyncing = true, errorMessage = null) }
                    try {
                        syncEngineService.deleteCloudSession(intent.sessionId, intent.teamId)
                        onIntent(CloudIntent.RefreshCloudLogs)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(isSyncing = false, errorMessage = e.message ?: "Remote delete failed") }
                    }
                }
                is CloudIntent.DeleteMultipleRemoteSessions -> {
                    _state.update { it.copy(isSyncing = true, errorMessage = null) }
                    try {
                        for (item in intent.sessionIdsAndTeamIds) {
                            syncEngineService.deleteCloudSession(item.first, item.second)
                        }
                        onIntent(CloudIntent.RefreshCloudLogs)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _state.update { it.copy(isSyncing = false, errorMessage = e.message ?: "Batch remote delete failed") }
                    }
                }
                is CloudIntent.ClearError -> {
                    _state.update { it.copy(errorMessage = null) }
                }
            }
        }
    }

    private suspend fun fetchRobotLogs() {
        _state.update { it.copy(isFetchingRobotLogs = true, errorMessage = null) }
        try {
            /**
             * logs val.
             */
            val logs: List<RobotLogFileInfo> = withContext(Dispatchers.IO) {
                httpClient.get("http://${getRobotIp()}:5002/api/logs").body()
            }

            /**
             * runs val.
             */
            val runs = logs.groupBy {
                /**
                 * nameWithoutExt val.
                 */
                val nameWithoutExt = it.name.substringBeforeLast(".")
                when {
                    nameWithoutExt.startsWith("action_log_") -> nameWithoutExt.substringAfter("action_log_")
                    nameWithoutExt.startsWith("ares_log_") -> nameWithoutExt.substringAfter("ares_log_")
                    nameWithoutExt.length > 15 && nameWithoutExt.takeLast(15).matches(Regex("\\d{8}_\\d{6}")) -> {
                        nameWithoutExt.takeLast(15)
                    }
                    else -> it.name
                }
            }.map { (runId, files) ->
                RobotRun(
                    runId = runId,
                    files = files,
                    totalSizeBytes = files.sumOf { it.sizeBytes },
                    lastModifiedMs = files.maxOf { it.lastModifiedMs },
                    lastModifiedFmt = files.maxByOrNull { it.lastModifiedMs }?.lastModifiedFmt ?: "",
                    allSynced = files.all { it.synced },
                    isActive = files.any { it.isActive == true }
                )
            }.sortedByDescending { it.lastModifiedMs }

            _state.update { it.copy(robotRuns = runs, isFetchingRobotLogs = false) }
        } catch (e: Exception) {
            e.printStackTrace()
            _state.update { it.copy(robotRuns = emptyList(), isFetchingRobotLogs = false, errorMessage = "Failed to fetch logs: ${e.message}") }
        }
    }
}
