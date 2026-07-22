package com.ares.analytics.service

import com.ares.analytics.shared.DriverProfile
import com.ares.analytics.shared.TelemetryFrame
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * DriverAnalysisServiceTest class.
 */
class DriverAnalysisServiceTest {

    @Test
    /**
     * testProfilesCRUD fun.
     */
    fun testProfilesCRUD() = runTest {
        /**
         * tempDb val.
         */
        val tempDb = File.createTempFile("driver_crud_db", ".db").apply { deleteOnExit() }
        /**
         * databaseService val.
         */
        val databaseService = DatabaseService(tempDb.absolutePath)
        /**
         * sysIdService val.
         */
        val sysIdService = SysIdService(databaseService)

        /**
         * tempFile val.
         */
        val tempFile = File.createTempFile("driver_profiles", ".json")
        tempFile.delete() // Delete so DriverAnalysisService writes defaults

        /**
         * service val.
         */
        val service = DriverAnalysisService(databaseService, sysIdService, tempFile.absolutePath)

        // Verify default profiles loaded
        /**
         * profiles val.
         */
        val profiles = service.getProfiles()
        assertEquals(3, profiles.size)

        // Save new profile
        /**
         * profile val.
         */
        val profile = DriverProfile("Pro Driver", 1.4, 4.0)
        service.saveProfile(profile)

        /**
         * retrieved val.
         */
        val retrieved = service.getProfile("Pro Driver")
        assertEquals(1.4, retrieved?.deadbandExponent)

        // Delete profile
        service.deleteProfile("Pro Driver")
        kotlin.test.assertNull(service.getProfile("Pro Driver"))
        tempDb.delete()
    }

    @Test
    /**
     * testAnalyzeDriverJitter fun.
     */
    fun testAnalyzeDriverJitter() = runTest {
        /**
         * tempDb val.
         */
        val tempDb = File.createTempFile("driver_jitter_db", ".db").apply { deleteOnExit() }
        /**
         * databaseService val.
         */
        val databaseService = DatabaseService(tempDb.absolutePath)
        /**
         * sysIdService val.
         */
        val sysIdService = SysIdService(databaseService)

        /**
         * tempFile val.
         */
        val tempFile = File.createTempFile("driver_profiles", ".json")
        tempFile.delete() // Delete so DriverAnalysisService writes defaults

        /**
         * service val.
         */
        val service = DriverAnalysisService(databaseService, sysIdService, tempFile.absolutePath)

        /**
         * sessionId val.
         */
        val sessionId = "test-session"
        /**
         * gamepadX val.
         */
        val gamepadX = "/Gamepad1/LeftX"

        // Generate telemetry with a 10 Hz oscillation (jitter) at 100 Hz sample rate
        /**
         * frames val.
         */
        val frames = mutableListOf<TelemetryFrame>()
        /**
         * sampleRate val.
         */
        val sampleRate = 100.0
        /**
         * freq val.
         */
        val freq = 10.0 // 10 Hz
        for (i in 0 until 128) {
            /**
             * t val.
             */
            val t = (i * (1000.0 / sampleRate)).toLong()
            /**
             * value val.
             */
            val value = kotlin.math.sin(2.0 * kotlin.math.PI * freq * (i / sampleRate))
            frames.add(TelemetryFrame(t, sessionId, gamepadX, value))
        }

        databaseService.insertTelemetryFrames(frames)

        /**
         * result val.
         */
        val result = service.analyzeDriverJitter(sessionId, gamepadX, "/Gamepad1/LeftY")
        assertTrue(result.hasJitter)
        assertEquals(10.0, result.peakFrequencyHz, 0.5)
        assertEquals(1.6, result.recommendedExponent)
        assertEquals(2.5, result.recommendedSlewRate)

        tempFile.delete()
        tempDb.delete()
    }

}
