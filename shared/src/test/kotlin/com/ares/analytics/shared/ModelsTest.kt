package com.ares.analytics.shared

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelsTest {

    @Test
    fun testWorkspaceConfigSerialization() {
        val config = WorkspaceConfig(
            teamId = "23247",
            seasonId = "2026",
            robotId = "ares-bot",
            projectPath = "/home/user/ares",
            league = League.FTC,
            nt4Host = "192.168.43.1"
        )

        val json = Json.encodeToString(config)
        val decoded = Json.decodeFromString<WorkspaceConfig>(json)

        assertEquals(config.teamId, decoded.teamId)
        assertEquals(config.league, decoded.league)
        assertEquals(config.nt4Host, decoded.nt4Host)
    }

    @Test
    fun testObstacleSerialization() {
        val circle: Obstacle = Obstacle.Circle(
            id = "c1",
            name = "Obstacle 1",
            centerX = 1.0,
            centerY = 2.0,
            radius = 0.5
        )

        val poly: Obstacle = Obstacle.Polygon(
            id = "p1",
            name = "Obstacle 2",
            vertices = listOf(PathPoint(0.0, 0.0), PathPoint(1.0, 0.0), PathPoint(0.0, 1.0))
        )

        val rect: Obstacle = Obstacle.Rectangle(
            id = "r1",
            name = "Obstacle 3",
            centerX = 2.0,
            centerY = 3.0,
            width = 0.8,
            height = 1.2,
            rotation = 45.0
        )

        val jsonCircle = Json.encodeToString(circle)
        val decodedCircle = Json.decodeFromString<Obstacle>(jsonCircle)
        assertTrue(decodedCircle is Obstacle.Circle)
        assertEquals(0.5, decodedCircle.radius)

        val jsonPoly = Json.encodeToString(poly)
        val decodedPoly = Json.decodeFromString<Obstacle>(jsonPoly)
        assertTrue(decodedPoly is Obstacle.Polygon)
        assertEquals(3, decodedPoly.vertices.size)

        val jsonRect = Json.encodeToString(rect)
        val decodedRect = Json.decodeFromString<Obstacle>(jsonRect)
        assertTrue(decodedRect is Obstacle.Rectangle)
        assertEquals(0.8, decodedRect.width)
        assertEquals(1.2, decodedRect.height)
        assertEquals(45.0, decodedRect.rotation)
    }

    @Test
    fun testFieldImageConfigSerialization() {
        val config = FieldImageConfig(
            imagePath = "/path/to/img.png",
            rotationDegrees = 90.0,
            cropLeft = 0.1,
            cropRight = 0.9,
            cropTop = 0.2,
            cropBottom = 0.8,
            widthMeters = 3.6,
            heightMeters = 3.6
        )

        val json = Json.encodeToString(config)
        val decoded = Json.decodeFromString<FieldImageConfig>(json)

        assertEquals(config.imagePath, decoded.imagePath)
        assertEquals(config.rotationDegrees, decoded.rotationDegrees)
        assertEquals(config.cropLeft, decoded.cropLeft)
        assertEquals(3.6, decoded.widthMeters)
    }

    @Test
    fun testGamePieceSerialization() {
        val gp = GamePiece(
            id = "gp1",
            name = "Yellow Sample 1",
            x = 1.2,
            y = 2.3,
            type = "Sample (Yellow)"
        )
        val json = Json.encodeToString(gp)
        val decoded = Json.decodeFromString<GamePiece>(json)
        assertEquals(gp.id, decoded.id)
        assertEquals(gp.name, decoded.name)
        assertEquals(gp.type, decoded.type)
    }
}
