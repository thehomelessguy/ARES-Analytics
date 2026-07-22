package com.ares.analytics.service

import com.ares.analytics.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    /**
     * body val.
     */
    val body: String? = null
)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class UpdateCheckerService(
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
    private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    sealed class UpdateState {
        /**
         * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
         * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
         * Canvas-to-field coordinate transformation conventions applied where relevant.
         *
         * @param args relevant arguments
         * @return expected results
         */
        object Checking : UpdateState()
        /**
         * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
         * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
         * Canvas-to-field coordinate transformation conventions applied where relevant.
         *
         * @param args relevant arguments
         * @return expected results
         */
        object UpToDate : UpdateState()
        /**
         * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
         * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
         * Canvas-to-field coordinate transformation conventions applied where relevant.
         *
         * @param args relevant arguments
         * @return expected results
         */
        data class UpdateAvailable(val latestVersion: String, val downloadUrl: String, val releaseNotes: String?) : UpdateState()
        /**
         * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
         * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
         * Canvas-to-field coordinate transformation conventions applied where relevant.
         *
         * @param args relevant arguments
         * @return expected results
         */
        data class Error(val message: String) : UpdateState()
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.UpToDate)
    /**
     * updateState val.
     */
    val updateState: StateFlow<UpdateState> = _updateState

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun checkForUpdates() {
        serviceScope.launch {
            _updateState.value = UpdateState.Checking
            try {
                httpClient.prepareGet("https://api.github.com/repos/ares-robotics/ares-analytics/releases/latest") {
                    header(HttpHeaders.UserAgent, "ares-analytics-app")
                }.execute { response ->
                    if (response.status == HttpStatusCode.OK) {
                        /**
                         * release val.
                         */
                        val release = response.body<GitHubRelease>()
                        if (isNewerVersion(BuildConfig.VERSION, release.tagName)) {
                            _updateState.value = UpdateState.UpdateAvailable(
                                latestVersion = release.tagName,
                                downloadUrl = release.htmlUrl,
                                releaseNotes = release.body
                            )
                        } else {
                            _updateState.value = UpdateState.UpToDate
                        }
                    } else {
                        _updateState.value = UpdateState.Error("API returned status ${response.status}")
                    }
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "Unknown error checking updates")
            }
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
    fun dispose() {
        serviceScope.coroutineContext.cancelChildren()
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        /**
         * cleanCurrent val.
         */
        val cleanCurrent = current.removePrefix("v").trim()
        /**
         * cleanLatest val.
         */
        val cleanLatest = latest.removePrefix("v").trim()
        if (cleanCurrent == cleanLatest) return false
        
        /**
         * currentParts val.
         */
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        /**
         * latestParts val.
         */
        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }
        
        /**
         * maxLength val.
         */
        val maxLength = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            /**
             * currVal val.
             */
            val currVal = currentParts.getOrElse(i) { 0 }
            /**
             * latVal val.
             */
            val latVal = latestParts.getOrElse(i) { 0 }
            if (latVal > currVal) return true
            if (currVal > latVal) return false
        }
        return false
    }
}
