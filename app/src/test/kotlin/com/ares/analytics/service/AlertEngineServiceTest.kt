package com.ares.analytics.service

import com.ares.analytics.shared.AlertRecord
import com.ares.analytics.shared.TelemetryFrame
import com.ares.analytics.shared.ThresholdRule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * MockNt4ClientService class.
 */
class MockNt4ClientService(databaseService: DatabaseService) : Nt4ClientService(databaseService) {
    /**
     * mockTelemetryFlow val.
     */
    val mockTelemetryFlow = MutableSharedFlow<TelemetryFrame>(replay = 100)
    override val telemetryFlow: SharedFlow<TelemetryFrame> = mockTelemetryFlow.asSharedFlow()
}

/**
 * AlertEngineServiceTest class.
 */
class AlertEngineServiceTest {

    @Test
    /**
     * testAlertEvaluation fun.
     */
    fun testAlertEvaluation() {
        runBlocking {
            /**
             * tempDb val.
             */
            val tempDb = File.createTempFile("alert_db_test", ".db").apply { deleteOnExit() }
            /**
             * databaseService val.
             */
            val databaseService = DatabaseService(tempDb.absolutePath)
            /**
             * nt4Service val.
             */
            val nt4Service = MockNt4ClientService(databaseService)

            // Write custom thresholds rules to a temp file
            /**
             * tempFile val.
             */
            val tempFile = File.createTempFile("thresholds_test", ".json")
            tempFile.delete() // Delete so AlertEngineService writes defaults/customs
            /**
             * rulesList val.
             */
            val rulesList = listOf(
                ThresholdRule("/Drive/Voltage", "Low Battery Voltage", minValue = 11.5, audibleAlert = false),
                ThresholdRule("/Drive/EkfDrift", "High EKF Position Drift", maxValue = 0.20, audibleAlert = false)
            )
            tempFile.writeText(Json.encodeToString(rulesList))

            /**
             * alertService val.
             */
            val alertService = AlertEngineService(databaseService, nt4Service, tempFile.absolutePath)

            // Give coroutines a moment to initialize subscription
            delay(200)

            // Emit telemetry violating /Drive/Voltage (< 11.5)
            /**
             * frame1 val.
             */
            val frame1 = TelemetryFrame(1000L, "session-123", "/Drive/Voltage", 11.0)
            nt4Service.mockTelemetryFlow.emit(frame1)

            delay(200)

            /**
             * activeAlerts val.
             */
            val activeAlerts = alertService.alerts.value
            assertEquals(1, activeAlerts.size)
            /**
             * alert val.
             */
            val alert = activeAlerts[0]
            assertEquals("/Drive/Voltage", alert.ruleKey)
            assertEquals("session-123", alert.sessionId)
            assertEquals(11.0, alert.peakValue)
            kotlin.test.assertFalse(alert.triaged)

            // Emit telemetry restoring normal voltage
            /**
             * frame2 val.
             */
            val frame2 = TelemetryFrame(1020L, "session-123", "/Drive/Voltage", 12.0)
            nt4Service.mockTelemetryFlow.emit(frame2)

            delay(200)

            // Should resolve the alert (resolveTimestampMs set)
            /**
             * resolvedAlerts val.
             */
            val resolvedAlerts = alertService.alerts.value
            assertEquals(1, resolvedAlerts.size)
            /**
             * resolved val.
             */
            val resolved = resolvedAlerts[0]
            assertTrue(resolved.resolveTimestampMs != null)
            assertEquals(20L, resolved.durationMs)

            // Triage the alert
            alertService.triageAlert(resolved.alertId)
            delay(200)
            assertTrue(alertService.alerts.value[0].triaged)

            alertService.stop()
            tempFile.delete()
            tempDb.delete()
        }
    }
}
