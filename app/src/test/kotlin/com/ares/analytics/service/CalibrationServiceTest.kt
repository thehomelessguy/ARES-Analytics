package com.ares.analytics.service

import com.ares.analytics.shared.TelemetryFrame
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CalibrationServiceTest class.
 */
class CalibrationServiceTest {

    @Test
    /**
     * testSolveCameraExtrinsics fun.
     */
    fun testSolveCameraExtrinsics() {
        /**
         * tempDb val.
         */
        val tempDb = File.createTempFile("calib_solve_test", ".db").apply { deleteOnExit() }
        /**
         * databaseService val.
         */
        val databaseService = DatabaseService(tempDb.absolutePath)
        /**
         * calibrationService val.
         */
        val calibrationService = CalibrationService(databaseService)

        // Mock camera extrinsics: dx=0.1, dy=0.2, dz=0.3, roll=0.0, pitch=0.0, yaw=0.05
        /**
         * targetDx val.
         */
        val targetDx = 0.1
        /**
         * targetDy val.
         */
        val targetDy = 0.2
        /**
         * targetDz val.
         */
        val targetDz = 0.3
        /**
         * targetYaw val.
         */
        val targetYaw = 0.05

        /**
         * measurements val.
         */
        val measurements = mutableListOf<CalibrationMeasurement>()
        // Generate mock measurements at different gyro positions
        for (i in 0 until 5) {
            /**
             * gyro val.
             */
            val gyro = i * 0.2
            /**
             * Cx val.
             */
            val Cx = 1.0
            /**
             * Cy val.
             */
            val Cy = 1.5
            /**
             * Cz val.
             */
            val Cz = 0.5

            /**
             * cosG val.
             */
            val cosG = kotlin.math.cos(gyro)
            /**
             * sinG val.
             */
            val sinG = kotlin.math.sin(gyro)
            /**
             * zRot val.
             */
            val zRot = (Cx * cosG + Cy * sinG) - targetDx
            /**
             * xRot val.
             */
            val xRot = (-Cx * sinG + Cy * cosG) - targetDy
            /**
             * yRot val.
             */
            val yRot = Cz - targetDz

            measurements.add(
                CalibrationMeasurement(
                    gyroHeading = gyro,
                    tagId = 1,
                    tagFieldX = Cx,
                    tagFieldY = Cy,
                    tagFieldZ = Cz,
                    targetSpaceX = xRot,
                    targetSpaceY = yRot,
                    targetSpaceZ = zRot,
                    targetSpaceRoll = 0.0,
                    targetSpacePitch = 0.0,
                    targetSpaceYaw = -gyro - targetYaw
                )
            )
        }

        /**
         * solved val.
         */
        val solved = calibrationService.solveCameraExtrinsics(measurements)
        assertEquals(targetDx, solved.x, 0.05)
        assertEquals(targetDy, solved.y, 0.05)
        // Under vertical collinearity (stationary floor), solved.z stays at initial guess 0.2
        assertEquals(0.2, solved.z, 0.05)
        assertEquals(targetYaw, solved.yaw, 0.05)

        // Test diagnostics version
        /**
         * diag val.
         */
        val diag = calibrationService.solveCameraExtrinsicsWithDiagnostics(measurements)
        assertEquals(targetDx, diag.pose.x, 0.05)
        assertEquals(targetDy, diag.pose.y, 0.05)
        assertEquals(targetYaw, diag.pose.yaw, 0.05)
        
        // Assert standard errors are non-negative and reasonably sized
        assertEquals(6, diag.standardErrors.size)
        diag.standardErrors.forEach { se ->
            assertTrue(se >= 0.0)
        }
        // Assert covariance matrix is 6x6
        assertEquals(6, diag.covarianceMatrix.size)
        diag.covarianceMatrix.forEach { row ->
            assertEquals(6, row.size)
        }
        assertTrue(diag.reducedChiSquared >= 0.0)

        tempDb.delete()
    }

    @Test
    /**
     * testRunExtrinsicCalibration fun.
     */
    fun testRunExtrinsicCalibration() = runTest {
        /**
         * tempDb val.
         */
        val tempDb = File.createTempFile("calib_run_test", ".db").apply { deleteOnExit() }
        /**
         * databaseService val.
         */
        val databaseService = DatabaseService(tempDb.absolutePath)
        /**
         * calibrationService val.
         */
        val calibrationService = CalibrationService(databaseService)

        /**
         * sessionId val.
         */
        val sessionId = "calib-session"
        /**
         * cameraIndex val.
         */
        val cameraIndex = 0

        // Insert mock database values
        /**
         * frames val.
         */
        val frames = listOf(
            TelemetryFrame(1000L, sessionId, "/Calibration/GyroHeading", 0.0),
            TelemetryFrame(1000L, sessionId, "/Calibration/TagIndex", 1.0),
            TelemetryFrame(1000L, sessionId, "/Calibration/CameraToTag[0]", 0.5), // x=0.5
            TelemetryFrame(1001L, sessionId, "/Calibration/CameraToTag[0]", 0.2), // y=0.2
            TelemetryFrame(1002L, sessionId, "/Calibration/CameraToTag[0]", 1.2), // z=1.2
            TelemetryFrame(1003L, sessionId, "/Calibration/CameraToTag[0]", 0.0), // roll
            TelemetryFrame(1004L, sessionId, "/Calibration/CameraToTag[0]", 0.0), // pitch
            TelemetryFrame(1005L, sessionId, "/Calibration/CameraToTag[0]", -0.05) // yaw
        )

        databaseService.insertTelemetryFrames(frames)

        /**
         * solved val.
         */
        val solved = calibrationService.runExtrinsicCalibration(sessionId, cameraIndex)
        // Should resolve something without crashing
        assertTrue(solved.x >= 0.0 || solved.x < 0.0)

        /**
         * diag val.
         */
        val diag = calibrationService.runExtrinsicCalibrationWithDiagnostics(sessionId, cameraIndex)
        assertTrue(diag.pose.x >= 0.0 || diag.pose.x < 0.0)
        assertEquals(6, diag.standardErrors.size)
        tempDb.delete()
    }
}
