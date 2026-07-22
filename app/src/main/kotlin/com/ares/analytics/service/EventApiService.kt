package com.ares.analytics.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class MatchInfo(
    /**
     * matchNumber val.
     */
    val matchNumber: Int,
    /**
     * compLevel val.
     */
    val compLevel: String,
    /**
     * redAlliance val.
     */
    val redAlliance: List<String>,
    /**
     * blueAlliance val.
     */
    val blueAlliance: List<String>,
    /**
     * scheduledTime val.
     */
    val scheduledTime: Long? = null
)

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class EventApiService(
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
        }
    }
) {
    // Queries TBA (The Blue Alliance) API for FRC match schedules
    suspend fun fetchFrcEventSchedule(eventKey: String, apiKey: String): List<MatchInfo> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext emptyList()
        try {
            httpClient.prepareGet("https://www.thebluealliance.com/api/v3/event/$eventKey/matches/simple") {
                header("X-TBA-Auth-Key", apiKey)
            }.execute { response ->
                if (response.status == HttpStatusCode.OK) {
                    /**
                     * matchesJson val.
                     */
                    val matchesJson = response.body<JsonArray>()
                    matchesJson.mapNotNull { element ->
                        try {
                            /**
                             * obj val.
                             */
                            val obj = element.jsonObject
                            /**
                             * compLevel val.
                             */
                            val compLevel = obj["comp_level"]?.jsonPrimitive?.content ?: "qm"
                            /**
                             * matchNumber val.
                             */
                            val matchNumber = obj["match_number"]?.jsonPrimitive?.int ?: 0
                            /**
                             * alliances val.
                             */
                            val alliances = obj["alliances"]?.jsonObject
                            
                            /**
                             * redAllianceObj val.
                             */
                            val redAllianceObj = alliances?.get("red")?.jsonObject
                            /**
                             * redTeams val.
                             */
                            val redTeams = redAllianceObj?.get("team_keys")?.jsonArray?.map { it.jsonPrimitive.content.removePrefix("frc") } ?: emptyList()
                            
                            /**
                             * blueAllianceObj val.
                             */
                            val blueAllianceObj = alliances?.get("blue")?.jsonObject
                            /**
                             * blueTeams val.
                             */
                            val blueTeams = blueAllianceObj?.get("team_keys")?.jsonArray?.map { it.jsonPrimitive.content.removePrefix("frc") } ?: emptyList()
                            
                            /**
                             * time val.
                             */
                            val time = obj["time"]?.jsonPrimitive?.longOrNull?.let { it * 1000 } // convert to ms
                            
                            MatchInfo(
                                matchNumber = matchNumber,
                                compLevel = if (compLevel == "qm") "quals" else "elims",
                                redAlliance = redTeams,
                                blueAlliance = blueTeams,
                                scheduledTime = time
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedBy { it.matchNumber }
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Queries TOA (The Orange Alliance) API for FTC match schedules
    suspend fun fetchFtcEventSchedule(eventKey: String, apiKey: String): List<MatchInfo> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext emptyList()
        try {
            httpClient.prepareGet("https://theorangealliance.org/api/event/$eventKey/matches") {
                header("X-TOA-ApiKey", apiKey)
                header("X-Application-Name", "ARES-Analytics")
            }.execute { response ->
                if (response.status == HttpStatusCode.OK) {
                    /**
                     * matchesJson val.
                     */
                    val matchesJson = response.body<JsonArray>()
                    matchesJson.mapNotNull { element ->
                        try {
                            /**
                             * obj val.
                             */
                            val obj = element.jsonObject
                            /**
                             * matchNumber val.
                             */
                            val matchNumber = obj["match_number"]?.jsonPrimitive?.int ?: 0
                            /**
                             * name val.
                             */
                            val name = obj["match_name"]?.jsonPrimitive?.content ?: ""
                            /**
                             * compLevel val.
                             */
                            val compLevel = if (name.contains("Quals", ignoreCase = true) || name.contains("Qualification", ignoreCase = true)) "quals" else "elims"
                            
                            /**
                             * participants val.
                             */
                            val participants = obj["participants"]?.jsonArray ?: JsonArray(emptyList())
                            /**
                             * redTeams val.
                             */
                            val redTeams = mutableListOf<String>()
                            /**
                             * blueTeams val.
                             */
                            val blueTeams = mutableListOf<String>()
                            
                            participants.forEach { partElement ->
                                /**
                                 * pObj val.
                                 */
                                val pObj = partElement.jsonObject
                                /**
                                 * teamKey val.
                                 */
                                val teamKey = pObj["team_key"]?.jsonPrimitive?.content ?: ""
                                /**
                                 * station val.
                                 */
                                val station = pObj["station"]?.jsonPrimitive?.int ?: 0
                                /**
                                 * isRed val.
                                 */
                                val isRed = station in 11..19
                                if (isRed) {
                                    redTeams.add(teamKey)
                                } else {
                                    blueTeams.add(teamKey)
                                }
                            }
                            
                            /**
                             * scheduledTimeStr val.
                             */
                            val scheduledTimeStr = obj["scheduled_time"]?.jsonPrimitive?.contentOrNull
                            /**
                             * timeMs val.
                             */
                            val timeMs = scheduledTimeStr?.let {
                                try {
                                    java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            MatchInfo(
                                matchNumber = matchNumber,
                                compLevel = compLevel,
                                redAlliance = redTeams,
                                blueAlliance = blueTeams,
                                scheduledTime = timeMs
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedBy { it.matchNumber }
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
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
    fun close() {
        httpClient.close()
    }
}
