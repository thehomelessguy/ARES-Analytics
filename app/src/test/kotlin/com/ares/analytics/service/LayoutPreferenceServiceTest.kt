package com.ares.analytics.service

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * LayoutPreferenceServiceTest class.
 */
class LayoutPreferenceServiceTest {

    @Test
    /**
     * testDefaultLayouts fun.
     */
    fun testDefaultLayouts() {
        /**
         * tempDir val.
         */
        val tempDir = File(System.getProperty("java.io.tmpdir"), "ares_layout_test_default")
        /**
         * service val.
         */
        val service = LayoutPreferenceService(tempDir.absolutePath)

        /**
         * programmerLayout val.
         */
        val programmerLayout = service.getDefaultLayout("programmer")
        assertTrue(programmerLayout.widgets.isNotEmpty())
        /**
         * chart val.
         */
        val chart = programmerLayout.widgets.first { it.type == "telemetry_chart" }
        assertEquals(0, chart.row)
        assertEquals(0, chart.col)
        assertEquals(6, chart.rowSpan)
        assertEquals(9, chart.colSpan)

        /**
         * driverCoachLayout val.
         */
        val driverCoachLayout = service.getDefaultLayout("driver_coach")
        assertTrue(driverCoachLayout.widgets.any { it.type == "joystick_visualizer" })
        /**
         * alerts val.
         */
        val alerts = driverCoachLayout.widgets.first { it.type == "alerts" }
        assertEquals(6, alerts.row)
        assertEquals(6, alerts.col)
        assertEquals(3, alerts.rowSpan)
        assertEquals(3, alerts.colSpan)

        /**
         * pitCrewLayout val.
         */
        val pitCrewLayout = service.getDefaultLayout("pit_crew")
        assertTrue(pitCrewLayout.widgets.any { it.type == "ai_coach" })
    }

    @Test
    /**
     * testSaveAndLoadLayout fun.
     */
    fun testSaveAndLoadLayout() = runTest {
        /**
         * tempDir val.
         */
        val tempDir = File(System.getProperty("java.io.tmpdir"), "ares_layout_test_save")
        tempDir.mkdirs()
        /**
         * service val.
         */
        val service = LayoutPreferenceService(tempDir.absolutePath)

        /**
         * customWidgets val.
         */
        val customWidgets = listOf(
            WidgetConfig("chart_1", "telemetry_chart", 0, 0, 2, 2)
        )
        /**
         * config val.
         */
        val config = DashboardLayoutConfig(customWidgets)

        service.saveLayout("custom_profile", config)

        /**
         * loaded val.
         */
        val loaded = service.loadLayout("custom_profile")
        assertEquals(1, loaded.widgets.size)
        assertEquals("chart_1", loaded.widgets.first().id)
        assertEquals("telemetry_chart", loaded.widgets.first().type)

        // Cleanup
        File(tempDir, "custom_profile.json").delete()
        tempDir.delete()
    }

    @Test
    /**
     * testGetAvailableLayouts fun.
     */
    fun testGetAvailableLayouts() = runTest {
        /**
         * tempDir val.
         */
        val tempDir = File(System.getProperty("java.io.tmpdir"), "ares_layout_test_list")
        tempDir.mkdirs()
        /**
         * service val.
         */
        val service = LayoutPreferenceService(tempDir.absolutePath)

        /**
         * config val.
         */
        val config = DashboardLayoutConfig(emptyList())
        service.saveLayout("Custom Team Layout", config)

        /**
         * available val.
         */
        val available = service.getAvailableLayouts()
        assertTrue(available.contains("Standard"))
        assertTrue(available.contains("Custom Team Layout"))

        // Cleanup
        File(tempDir, "custom_team_layout.json").delete()
        tempDir.delete()
    }

    @Test
    /**
     * testDeleteLayout fun.
     */
    fun testDeleteLayout() = runTest {
        /**
         * tempDir val.
         */
        val tempDir = File(System.getProperty("java.io.tmpdir"), "ares_layout_test_delete")
        tempDir.mkdirs()
        /**
         * service val.
         */
        val service = LayoutPreferenceService(tempDir.absolutePath)

        /**
         * config val.
         */
        val config = DashboardLayoutConfig(emptyList())
        service.saveLayout("Temp Delete Profile", config)
        assertTrue(service.getSavedLayouts().contains("Temp Delete Profile"))

        /**
         * deleted val.
         */
        val deleted = service.deleteLayout("Temp Delete Profile")
        assertTrue(deleted)
        assertTrue(!service.getSavedLayouts().contains("Temp Delete Profile"))

        tempDir.delete()
    }
}
