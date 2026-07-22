package com.ares.analytics.gateway.routes

import com.ares.analytics.gateway.auth.*
import com.ares.analytics.shared.*
import com.google.api.core.ApiFuture
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.QueryDocumentSnapshot
import com.google.cloud.firestore.QuerySnapshot
import com.google.cloud.firestore.WriteResult
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

/**
 * TeamRobotsIntegrationTest class.
 */
class TeamRobotsIntegrationTest {

    @Suppress("UNCHECKED_CAST")
    @Test
    /**
     * testGetRobotsFlow fun.
     */
    fun testGetRobotsFlow() = testApplication {
        /**
         * mockFirestore val.
         */
        val mockFirestore = mock(Firestore::class.java)
        /**
         * mockTeamsCol val.
         */
        val mockTeamsCol = mock(CollectionReference::class.java)
        /**
         * mockTeamDoc val.
         */
        val mockTeamDoc = mock(DocumentReference::class.java)
        /**
         * mockRobotsCol val.
         */
        val mockRobotsCol = mock(CollectionReference::class.java)
        /**
         * mockFuture val.
         */
        val mockFuture = mock(ApiFuture::class.java) as ApiFuture<QuerySnapshot>
        /**
         * mockQuerySnapshot val.
         */
        val mockQuerySnapshot = mock(QuerySnapshot::class.java)

        `when`(mockFirestore.collection("teams")).thenReturn(mockTeamsCol)
        `when`(mockTeamsCol.document("9999")).thenReturn(mockTeamDoc)
        `when`(mockTeamDoc.collection("robots")).thenReturn(mockRobotsCol)
        `when`(mockRobotsCol.get()).thenReturn(mockFuture)
        `when`(mockFuture.get()).thenReturn(mockQuerySnapshot)

        /**
         * doc1 val.
         */
        val doc1 = mock(QueryDocumentSnapshot::class.java)
        `when`(doc1.id).thenReturn("ares-bot")
        `when`(doc1.data).thenReturn(mapOf(
            "league" to "FTC",
            "seasonId" to "2026",
            "name" to "Ares FTC"
        ))
        `when`(mockQuerySnapshot.documents).thenReturn(listOf(doc1))

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

        /**
         * response val.
         */
        val response = client.get("/api/team/9999/robots") {
            header(HttpHeaders.Authorization, "Bearer mock-token:uid:email:name:9999")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        /**
         * decoded val.
         */
        val decoded = Json.decodeFromString<TeamRobotsResponse>(response.bodyAsText())
        assertEquals(1, decoded.robots.size)
        assertEquals("ares-bot", decoded.robots[0].robotId)
        assertEquals(League.FTC, decoded.robots[0].league)
        assertEquals("Ares FTC", decoded.robots[0].name)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    /**
     * testAddAndDeleteRobotsFlow fun.
     */
    fun testAddAndDeleteRobotsFlow() = testApplication {
        /**
         * mockFirestore val.
         */
        val mockFirestore = mock(Firestore::class.java)
        /**
         * mockTeamsCol val.
         */
        val mockTeamsCol = mock(CollectionReference::class.java)
        /**
         * mockTeamDoc val.
         */
        val mockTeamDoc = mock(DocumentReference::class.java)
        /**
         * mockRobotsCol val.
         */
        val mockRobotsCol = mock(CollectionReference::class.java)
        /**
         * mockRobotDoc val.
         */
        val mockRobotDoc = mock(DocumentReference::class.java)
        /**
         * mockSetFuture val.
         */
        val mockSetFuture = mock(ApiFuture::class.java) as ApiFuture<WriteResult>
        /**
         * mockDeleteFuture val.
         */
        val mockDeleteFuture = mock(ApiFuture::class.java) as ApiFuture<WriteResult>

        // Mock users role verification
        /**
         * mockUsersCol val.
         */
        val mockUsersCol = mock(CollectionReference::class.java)
        /**
         * mockUserDocRef val.
         */
        val mockUserDocRef = mock(DocumentReference::class.java)
        /**
         * mockUserGetFuture val.
         */
        val mockUserGetFuture = mock(ApiFuture::class.java) as ApiFuture<DocumentSnapshot>
        /**
         * mockUserDocSnapshot val.
         */
        val mockUserDocSnapshot = mock(DocumentSnapshot::class.java)

        `when`(mockFirestore.collection("users")).thenReturn(mockUsersCol)
        `when`(mockUsersCol.document("uid")).thenReturn(mockUserDocRef)
        `when`(mockUserDocRef.get()).thenReturn(mockUserGetFuture)
        `when`(mockUserGetFuture.get()).thenReturn(mockUserDocSnapshot)
        `when`(mockUserDocSnapshot.exists()).thenReturn(true)
        `when`(mockUserDocSnapshot.getString("role")).thenReturn("ADMIN")

        `when`(mockFirestore.collection("teams")).thenReturn(mockTeamsCol)
        `when`(mockTeamsCol.document("9999")).thenReturn(mockTeamDoc)
        `when`(mockTeamDoc.collection("robots")).thenReturn(mockRobotsCol)
        `when`(mockRobotsCol.document("ares-bot")).thenReturn(mockRobotDoc)
        `when`(mockRobotDoc.set(anyMap())).thenReturn(mockSetFuture)
        `when`(mockRobotDoc.delete()).thenReturn(mockDeleteFuture)

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

        // Test Add
        /**
         * addReq val.
         */
        val addReq = AddRobotRequest(
            teamId = "9999",
            robot = RobotProfile("ares-bot", League.FTC, "2026", "Ares FTC")
        )
        /**
         * addResponse val.
         */
        val addResponse = client.post("/api/team/robots/add") {
            header(HttpHeaders.Authorization, "Bearer mock-token:uid:email:name:9999")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(AddRobotRequest.serializer(), addReq))
        }
        if (addResponse.status != HttpStatusCode.OK) {
            println("Add robot failed: ${addResponse.bodyAsText()}")
        }
        assertEquals(HttpStatusCode.OK, addResponse.status)

        // Test Delete
        /**
         * deleteReq val.
         */
        val deleteReq = DeleteRobotRequest(
            teamId = "9999",
            robotId = "ares-bot"
        )
        /**
         * deleteResponse val.
         */
        val deleteResponse = client.post("/api/team/robots/delete") {
            header(HttpHeaders.Authorization, "Bearer mock-token:uid:email:name:9999")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(DeleteRobotRequest.serializer(), deleteReq))
        }
        if (deleteResponse.status != HttpStatusCode.OK) {
            println("Delete robot failed: ${deleteResponse.bodyAsText()}")
        }
        assertEquals(HttpStatusCode.OK, deleteResponse.status)
    }
}
