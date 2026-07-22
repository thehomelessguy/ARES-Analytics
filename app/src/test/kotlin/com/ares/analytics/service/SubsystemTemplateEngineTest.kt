package com.ares.analytics.service

import com.ares.analytics.viewmodel.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubsystemTemplateEngineTest {

    @Test
    fun `generateFiles creates complete subsystem suite`() {
        val state = SubsystemGeneratorState(
            subsystemName = "Elevator",
            packageName = "org.firstinspires.ftc.teamcode",
            hardwareEntries = listOf(
                HardwareEntry("winch", HardwareType.DC_MOTOR_EX),
                HardwareEntry("topLimit", HardwareType.DIGITAL_CHANNEL)
            ),
            stateFields = listOf(
                StateFieldEntry("targetHeight", FieldType.DOUBLE, "0.0"),
                StateFieldEntry("currentHeight", FieldType.DOUBLE, "0.0"),
                StateFieldEntry("isAtTop", FieldType.BOOLEAN, "false")
            ),
            generateMockIO = true,
            generateTestSkeleton = true,
            generateDedicatedActions = true
        )

        val files = SubsystemTemplateEngine.generateFiles(state)

        assertEquals(9, files.size)
        assertTrue(files.containsKey("state/ElevatorState.kt"))
        assertTrue(files.containsKey("hardware/ElevatorIO.kt"))
        assertTrue(files.containsKey("hardware/FtcElevatorIO.kt"))
        assertTrue(files.containsKey("hardware/MockElevatorIO.kt"))
        assertTrue(files.containsKey("control/ElevatorController.kt"))
        assertTrue(files.containsKey("subsystems/ElevatorSubsystem.kt"))
        assertTrue(files.containsKey("state/ElevatorAction.kt"))
        assertTrue(files.containsKey("state/ElevatorReducer.kt"))
        assertTrue(files.containsKey("test/ElevatorSubsystemTest.kt"))

        val stateContent = files["state/ElevatorState.kt"]!!
        assertTrue(stateContent.contains("data class ElevatorState("))
        assertTrue(stateContent.contains("val targetHeight: Double = 0.0"))
        assertTrue(stateContent.contains("val isAtTop: Boolean = false"))

        val ioContent = files["hardware/ElevatorIO.kt"]!!
        assertTrue(ioContent.contains("interface ElevatorIO : SubsystemIO"))
        assertTrue(ioContent.contains("val winchPosition: Double"))
        assertTrue(ioContent.contains("val isTopLimitTriggered: Boolean"))
        assertTrue(ioContent.contains("fun setWinchVoltage(volts: Double)"))
    }

    @Test
    fun `generateFiles handles empty name gracefully`() {
        val state = SubsystemGeneratorState(subsystemName = "")
        val files = SubsystemTemplateEngine.generateFiles(state)
        assertTrue(files.isEmpty())
    }
}
