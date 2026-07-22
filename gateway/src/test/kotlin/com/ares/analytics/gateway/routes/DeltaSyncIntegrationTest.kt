package com.ares.analytics.gateway.routes

import com.ares.analytics.gateway.auth.*
import com.ares.analytics.shared.*
import com.google.api.core.ApiFuture
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.QueryDocumentSnapshot
import com.google.cloud.firestore.QuerySnapshot
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json
import org.mockito.Mockito.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * DeltaSyncIntegrationTest class.
 */
class DeltaSyncIntegrationTest {

    @Suppress("UNCHECKED_CAST")
    @Test
    /**
     * testDeltaSyncFlow fun.
     */
    fun testDeltaSyncFlow() = testApplication {
        /**
         * mockFirestore val.
         */
        val mockFirestore = mock(Firestore::class.java)
        /**
         * mockCollection val.
         */
        val mockCollection = mock(CollectionReference::class.java)
        /**
         * mockQuery1 val.
         */
        val mockQuery1 = mock(Query::class.java)
        /**
         * mockQuery2 val.
         */
        val mockQuery2 = mock(Query::class.java)
        /**
         * mockFuture val.
         */
        val mockFuture = mock(ApiFuture::class.java) as ApiFuture<QuerySnapshot>
        /**
         * mockQuerySnapshot val.
         */
        val mockQuerySnapshot = mock(QuerySnapshot::class.java)

        `when`(mockFirestore.collection("summaries")).thenReturn(mockCollection)
        `when`(mockCollection.whereEqualTo("teamId", "9999")).thenReturn(mockQuery1)
        `when`(mockQuery1.whereEqualTo("seasonId", "2026")).thenReturn(mockQuery2)
        `when`(mockQuery2.get()).thenReturn(mockFuture)
        `when`(mockFuture.get()).thenReturn(mockQuerySnapshot)

        /**
         * mockUserCollection val.
         */
        val mockUserCollection = mock(CollectionReference::class.java)
        /**
         * mockUserDocRef val.
         */
        val mockUserDocRef = mock(DocumentReference::class.java)
        /**
         * mockUserFuture val.
         */
        val mockUserFuture = mock(ApiFuture::class.java) as ApiFuture<DocumentSnapshot>
        /**
         * mockUserDoc val.
         */
        val mockUserDoc = mock(DocumentSnapshot::class.java)

        `when`(mockFirestore.collection("users")).thenReturn(mockUserCollection)
        `when`(mockUserCollection.document("uid")).thenReturn(mockUserDocRef)
        `when`(mockUserDocRef.get()).thenReturn(mockUserFuture)
        `when`(mockUserFuture.get()).thenReturn(mockUserDoc)
        `when`(mockUserDoc.get("githubOrgs")).thenReturn(listOf("9999"))

        /**
         * summary1 val.
         */
        val summary1 = SessionSummary(
            sessionId = "session-1",
            teamId = "9999",
            seasonId = "2026",
            robotId = "ares",
            createdAt = 100000,
            durationMs = 5000,
            minBatteryVoltage = 11.5,
            maxEkfDrift = 0.05,
            avgLoopTimeMs = 15.0,
            p95LoopTimeMs = 22.0,
            motorCurrentAverages = emptyMap(),
            visionAcceptanceRate = 0.95,
            tags = listOf("test"),
            matchNumber = 1,
            allianceColor = "blue"
        )

        /**
         * summary2 val.
         */
        val summary2 = SessionSummary(
            sessionId = "session-2",
            teamId = "9999",
            seasonId = "2026",
            robotId = "ares",
            createdAt = 200000,
            durationMs = 6000,
            minBatteryVoltage = 12.0,
            maxEkfDrift = 0.02,
            avgLoopTimeMs = 14.5,
            p95LoopTimeMs = 20.0,
            motorCurrentAverages = emptyMap(),
            visionAcceptanceRate = 0.98,
            tags = listOf("prod"),
            matchNumber = 2,
            allianceColor = "red"
        )

        /**
         * doc1 val.
         */
        val doc1 = mock(QueryDocumentSnapshot::class.java)
        /**
         * doc2 val.
         */
        val doc2 = mock(QueryDocumentSnapshot::class.java)

        `when`(doc1.data).thenReturn(summary1.toTestMap())
        `when`(doc2.data).thenReturn(summary2.toTestMap())
        `when`(mockQuerySnapshot.documents).thenReturn(listOf(doc1, doc2))

        application {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = false
                    ignoreUnknownKeys = true
                })
            }
            installFirebaseAuthentication()
            install(RateLimit) {
                register(RateLimitName("archive")) {
                    rateLimiter(limit = 30, refillPeriod = 60.seconds)
                }
            }
            routing {
                archiveRoutes(customFirestore = mockFirestore)
            }
        }

        // We already know about session-1, so only session-2 is missing and should be returned
        /**
         * syncReq val.
         */
        val syncReq = SyncRequest(
            teamId = "9999",
            seasonId = "2026",
            knownSessionIds = listOf("session-1")
        )

        /**
         * reqJson val.
         */
        val reqJson = Json.encodeToString(SyncRequest.serializer(), syncReq)

        /**
         * response val.
         */
        val response = client.post("/api/archive/sync") {
            header(HttpHeaders.Authorization, "Bearer mock-token:uid:email:name:9999")
            contentType(ContentType.Application.Json)
            setBody(reqJson)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        /**
         * syncResponse val.
         */
        val syncResponse = Json.decodeFromString<SyncResponse>(response.bodyAsText())
        
        assertEquals(1, syncResponse.missingSummaries.size)
        /**
         * missing val.
         */
        val missing = syncResponse.missingSummaries.first()
        assertEquals("session-2", missing.sessionId)
        assertEquals("red", missing.allianceColor)
        assertEquals(2, missing.matchNumber)
    }

    private fun SessionSummary.toTestMap(): Map<String, Any?> {
        return mapOf(
            "sessionId" to sessionId,
            "teamId" to teamId,
            "seasonId" to seasonId,
            "robotId" to robotId,
            "createdAt" to createdAt,
            "durationMs" to durationMs,
            "minBatteryVoltage" to minBatteryVoltage,
            "maxEkfDrift" to maxEkfDrift,
            "avgLoopTimeMs" to avgLoopTimeMs,
            "p95LoopTimeMs" to p95LoopTimeMs,
            "motorCurrentAverages" to motorCurrentAverages,
            "visionAcceptanceRate" to visionAcceptanceRate,
            "tags" to tags,
            "matchNumber" to matchNumber,
            "allianceColor" to allianceColor
        )
    }
}
