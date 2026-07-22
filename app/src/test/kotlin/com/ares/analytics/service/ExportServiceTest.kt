package com.ares.analytics.service

import com.ares.analytics.shared.TelemetryFrame
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ExportServiceTest class.
 */
class ExportServiceTest {

    @Test
    /**
     * testExportToCsvList fun.
     */
    fun testExportToCsvList() = runTest {
        /**
         * tempDb val.
         */
        val tempDb = File.createTempFile("export_test_db", ".db").apply { deleteOnExit() }
        /**
         * databaseService val.
         */
        val databaseService = DatabaseService(tempDb.absolutePath)
        /**
         * exportService val.
         */
        val exportService = ExportService(databaseService)

        /**
         * sessionId val.
         */
        val sessionId = "session-csv-list"
        /**
         * frames val.
         */
        val frames = listOf(
            TelemetryFrame(1000L, sessionId, "/test/motor1", 1.5),
            TelemetryFrame(2000L, sessionId, "/test/motor1", 2.5)
        )
        databaseService.insertTelemetryFrames(frames)

        /**
         * tempCsv val.
         */
        val tempCsv = File.createTempFile("export_list", ".csv").apply { deleteOnExit() }
        exportService.exportToCsvList(sessionId, listOf("/test/motor1"), tempCsv)

        /**
         * lines val.
         */
        val lines = tempCsv.readLines()
        assertEquals(3, lines.size)
        assertEquals("key,timestamp_ms,value", lines[0])
        assertEquals("/test/motor1,1000,1.5", lines[1])
        assertEquals("/test/motor1,2000,2.5", lines[2])

        tempCsv.delete()
        tempDb.delete()
    }

    @Test
    /**
     * testExportToCsvTable fun.
     */
    fun testExportToCsvTable() = runTest {
        /**
         * tempDb val.
         */
        val tempDb = File.createTempFile("export_test_db_2", ".db").apply { deleteOnExit() }
        /**
         * databaseService val.
         */
        val databaseService = DatabaseService(tempDb.absolutePath)
        /**
         * exportService val.
         */
        val exportService = ExportService(databaseService)

        /**
         * sessionId val.
         */
        val sessionId = "session-csv-table"
        /**
         * frames val.
         */
        val frames = listOf(
            TelemetryFrame(1000L, sessionId, "/test/motor1", 1.5),
            TelemetryFrame(1000L, sessionId, "/test/motor2", 0.5),
            TelemetryFrame(2000L, sessionId, "/test/motor1", 2.5)
        )
        databaseService.insertTelemetryFrames(frames)

        /**
         * tempCsv val.
         */
        val tempCsv = File.createTempFile("export_table", ".csv").apply { deleteOnExit() }
        exportService.exportToCsvTable(sessionId, listOf("/test/motor1", "/test/motor2"), tempCsv)

        /**
         * lines val.
         */
        val lines = tempCsv.readLines()
        assertEquals(3, lines.size)
        assertEquals("timestamp_ms,/test/motor1,/test/motor2", lines[0])
        assertEquals("1000,1.5,0.5", lines[1])
        assertEquals("2000,2.5,0.5", lines[2]) // sample and hold fills motor2 with 0.5

        tempCsv.delete()
        tempDb.delete()
    }

    @Test
    /**
     * testExportToWpiLog fun.
     */
    fun testExportToWpiLog() = runTest {
        /**
         * tempDb val.
         */
        val tempDb = File.createTempFile("export_test_db_3", ".db").apply { deleteOnExit() }
        /**
         * databaseService val.
         */
        val databaseService = DatabaseService(tempDb.absolutePath)
        /**
         * exportService val.
         */
        val exportService = ExportService(databaseService)

        /**
         * sessionId val.
         */
        val sessionId = "session-wpilog"
        /**
         * frames val.
         */
        val frames = listOf(
            TelemetryFrame(1000L, sessionId, "/test/motor1", 1.5)
        )
        databaseService.insertTelemetryFrames(frames)

        /**
         * tempWpiLog val.
         */
        val tempWpiLog = File.createTempFile("export_wpilog", ".wpilog").apply { deleteOnExit() }
        exportService.exportToWpiLog(sessionId, listOf("/test/motor1"), tempWpiLog)

        /**
         * bytes val.
         */
        val bytes = tempWpiLog.readBytes()
        assertTrue(bytes.size > 12)
        // Check for WPILOG header (WPILOG)
        /**
         * header val.
         */
        val header = String(bytes.copyOfRange(0, 6), Charsets.UTF_8)
        assertEquals("WPILOG", header)

        tempWpiLog.delete()
        tempDb.delete()
    }
}
