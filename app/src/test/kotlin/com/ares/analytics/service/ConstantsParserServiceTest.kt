package com.ares.analytics.service

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConstantsParserServiceTest {

    @Test
    fun testLoadAndSaveKotlinConstants() = runTest {
        val parser = ConstantsParserService()

        // Create a temporary directory structure to mimic projectPath
        val tempDir = File(System.getProperty("java.io.tmpdir"), "ares_project_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        tempDir.deleteOnExit()

        val constantsFile = File(tempDir, "Constants.kt")
        constantsFile.deleteOnExit()
        constantsFile.writeText("""
            package com.ares.robot
            
            object Constants {
                val KP_DRIVE = 0.5
                var DEADBAND = 0.05
            }
        """.trimIndent())

        val list = parser.loadTunableConstants(tempDir.absolutePath)
        assertEquals(2, list.size)

        val kpDrive = list.first { it.name == "KP_DRIVE" }
        assertEquals(0.5, kpDrive.value)
        assertEquals(3, kpDrive.lineIndex) // 0-indexed line index: 3
        assertTrue(kpDrive.isKotlin)

        // Modify constant
        parser.saveConstant(kpDrive, 0.75)

        val updatedList = parser.loadTunableConstants(tempDir.absolutePath)
        val updatedKpDrive = updatedList.first { it.name == "KP_DRIVE" }
        assertEquals(0.75, updatedKpDrive.value)

        // Clean up
        constantsFile.delete()
        tempDir.delete()
    }

    @Test
    fun testLoadAndSaveJavaConstants() = runTest {
        val parser = ConstantsParserService()

        val tempDir = File(System.getProperty("java.io.tmpdir"), "ares_project_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        tempDir.deleteOnExit()

        val constantsFile = File(tempDir, "Constants.java")
        constantsFile.deleteOnExit()
        constantsFile.writeText("""
            public class Constants {
                public static double MAX_SPEED = 4.2;
            }
        """.trimIndent())

        val list = parser.loadTunableConstants(tempDir.absolutePath)
        assertEquals(1, list.size)

        val maxSpeed = list.first { it.name == "MAX_SPEED" }
        assertEquals(4.2, maxSpeed.value)
        kotlin.test.assertFalse(maxSpeed.isKotlin)

        // Modify constant
        parser.saveConstant(maxSpeed, 3.8)

        val updatedList = parser.loadTunableConstants(tempDir.absolutePath)
        val updatedMaxSpeed = updatedList.first { it.name == "MAX_SPEED" }
        assertEquals(3.8, updatedMaxSpeed.value)

        constantsFile.delete()
        tempDir.delete()
    }
}
