package com.ares.analytics.service

import com.ares.analytics.shared.SessionAnnotation
import com.ares.analytics.shared.TelemetryFrame
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.util.UUID

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class DSLogDecoderService(private val databaseService: DatabaseService) {

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    enum class PowerDistributionType {
        REV, CTRE, NONE
    }

    suspend fun parseDsLog(
        dslogFile: File,
        sessionId: String,
        batcher: FrameBatcher
    ) {
        if (!dslogFile.exists()) return

        /**
         * startTimeMs var.
         */
        var startTimeMs = 0.0
        /**
         * recordCount var.
         */
        var recordCount = 0

        FileInputStream(dslogFile).use { fis ->
            DataInputStream(fis).use { dis ->
                try {
                    /**
                     * version val.
                     */
                    val version = dis.readInt()
                    if (version != 4) {
                        throw IllegalArgumentException("Unsupported dslog version: $version")
                    }

                    /**
                     * seconds val.
                     */
                    val seconds = dis.readLong()
                    /**
                     * fractional val.
                     */
                    val fractional = dis.readLong()
                    startTimeMs = convertLVTime(seconds, fractional)

                    /**
                     * lastBatteryVolts var.
                     */
                    var lastBatteryVolts = 0.0

                    while (true) {
                        // Read 10-byte DS status record
                        /**
                         * tripTimeByte val.
                         */
                        val tripTimeByte = dis.readUnsignedByte()
                        /**
                         * packetLossByte val.
                         */
                        val packetLossByte = dis.readByte()
                        /**
                         * batteryVoltageShort val.
                         */
                        val batteryVoltageShort = dis.readUnsignedShort()
                        /**
                         * cpuUtilizationByte val.
                         */
                        val cpuUtilizationByte = dis.readUnsignedByte()
                        /**
                         * maskByte val.
                         */
                        val maskByte = dis.readUnsignedByte()
                        /**
                         * canUtilizationByte val.
                         */
                        val canUtilizationByte = dis.readUnsignedByte()
                        /**
                         * wifiDbByte val.
                         */
                        val wifiDbByte = dis.readUnsignedByte()
                        /**
                         * wifiMbShort val.
                         */
                        val wifiMbShort = dis.readUnsignedShort()

                        /**
                         * timestampMs val.
                         */
                        val timestampMs = (startTimeMs + recordCount * 20).toLong()

                        /**
                         * tripTimeMs val.
                         */
                        val tripTimeMs = tripTimeByte * 0.5
                        /**
                         * packetLoss val.
                         */
                        val packetLoss = Math.min(Math.max(packetLossByte * 4 * 0.01, 0.0), 1.0)
                        
                        /**
                         * batteryVolts var.
                         */
                        var batteryVolts = batteryVoltageShort.toDouble() / 256.0
                        if (batteryVolts > 20.0) {
                            batteryVolts = lastBatteryVolts
                        } else {
                            lastBatteryVolts = batteryVolts
                        }

                        /**
                         * cpuUtilization val.
                         */
                        val cpuUtilization = cpuUtilizationByte * 0.5 * 0.01
                        /**
                         * brownout val.
                         */
                        val brownout = (maskByte and (1 shl 7)) == 0
                        /**
                         * watchdog val.
                         */
                        val watchdog = (maskByte and (1 shl 6)) == 0
                        /**
                         * dsTeleop val.
                         */
                        val dsTeleop = (maskByte and (1 shl 5)) == 0
                        /**
                         * dsDisabled val.
                         */
                        val dsDisabled = (maskByte and (1 shl 3)) == 0
                        /**
                         * robotTeleop val.
                         */
                        val robotTeleop = (maskByte and (1 shl 2)) == 0
                        /**
                         * robotAuto val.
                         */
                        val robotAuto = (maskByte and (1 shl 1)) == 0
                        /**
                         * robotDisabled val.
                         */
                        val robotDisabled = (maskByte and 1) == 0
                        /**
                         * canUtilization val.
                         */
                        val canUtilization = canUtilizationByte * 0.5 * 0.01
                        /**
                         * wifiDb val.
                         */
                        val wifiDb = wifiDbByte * 0.5
                        /**
                         * wifiMb val.
                         */
                        val wifiMb = wifiMbShort.toDouble() / 256.0

                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/TripTimeMS", tripTimeMs))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/PacketLoss", packetLoss))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/BatteryVoltage", batteryVolts))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/CPUUtilization", cpuUtilization))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/Status/Brownout", if (brownout) 1.0 else 0.0))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/Status/Watchdog", if (watchdog) 1.0 else 0.0))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/Status/DSTeleop", if (dsTeleop) 1.0 else 0.0))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/Status/DSDisabled", if (dsDisabled) 1.0 else 0.0))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/Status/RobotTeleop", if (robotTeleop) 1.0 else 0.0))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/Status/RobotAuto", if (robotAuto) 1.0 else 0.0))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/Status/RobotDisabled", if (robotDisabled) 1.0 else 0.0))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/CANUtilization", canUtilization))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/WifiDb", wifiDb))
                        batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/WifiMb", wifiMb))

                        // Power Distribution Header (4 bytes)
                        /**
                         * pdHeader val.
                         */
                        val pdHeader = ByteArray(4)
                        dis.readFully(pdHeader)
                        /**
                         * pdTypeByte val.
                         */
                        val pdTypeByte = pdHeader[3].toInt() and 0xFF
                        /**
                         * pdType val.
                         */
                        val pdType = getPDType(pdTypeByte)

                        when {
                            pdType == PowerDistributionType.REV -> {
                                dis.readUnsignedByte() // skip CAN ID
                                /**
                                 * bitBytes val.
                                 */
                                val bitBytes = ByteArray(27)
                                dis.readFully(bitBytes)

                                /**
                                 * revBooleans val.
                                 */
                                val revBooleans = BooleanArray(216)
                                /**
                                 * bitIdx var.
                                 */
                                var bitIdx = 0
                                for (b in bitBytes) {
                                    /**
                                     * byteVal val.
                                     */
                                    val byteVal = b.toInt() and 0xFF
                                    for (i in 0 until 8) {
                                        revBooleans[bitIdx++] = (byteVal and (1 shl i)) != 0
                                    }
                                }

                                /**
                                 * currents val.
                                 */
                                val currents = mutableListOf<Double>()
                                for (i in 0 until 20) {
                                    /**
                                     * readPosition val.
                                     */
                                    val readPosition = (i / 3) * 32 + (i % 3) * 10
                                    /**
                                     * value var.
                                     */
                                    var value = 0
                                    for (j in 0 until 10) {
                                        if (revBooleans[readPosition + j]) {
                                            value = value or (1 shl j)
                                        }
                                    }
                                    currents.add(value.toDouble() / 8.0)
                                }

                                /**
                                 * extraBytes val.
                                 */
                                val extraBytes = ByteArray(4)
                                dis.readFully(extraBytes)
                                for (i in 0 until 4) {
                                    currents.add((extraBytes[i].toInt() and 0xFF).toDouble() / 16.0)
                                }

                                dis.readUnsignedByte() // skip last byte

                                for (i in currents.indices) {
                                    batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/PowerDistributionCurrents[$i]", currents[i]))
                                }
                            }
                            pdType == PowerDistributionType.CTRE -> {
                                dis.readUnsignedByte() // skip CAN ID
                                /**
                                 * bitBytes val.
                                 */
                                val bitBytes = ByteArray(21)
                                dis.readFully(bitBytes)

                                /**
                                 * ctreBooleans val.
                                 */
                                val ctreBooleans = BooleanArray(168)
                                /**
                                 * bitIdx var.
                                 */
                                var bitIdx = 0
                                for (b in bitBytes) {
                                    /**
                                     * byteVal val.
                                     */
                                    val byteVal = b.toInt() and 0xFF
                                    for (i in 0 until 8) {
                                        ctreBooleans[bitIdx++] = (byteVal and (1 shl i)) != 0
                                    }
                                }

                                /**
                                 * currents val.
                                 */
                                val currents = mutableListOf<Double>()
                                for (i in 0 until 16) {
                                    /**
                                     * readPosition val.
                                     */
                                    val readPosition = (i / 6) * 64 + (i % 6) * 10
                                    /**
                                     * value var.
                                     */
                                    var value = 0
                                    for (j in 0 until 8) {
                                        if (ctreBooleans[readPosition + j]) {
                                            value = value or (1 shl j)
                                        }
                                    }
                                    currents.add(value.toDouble() / 8.0)
                                }

                                dis.skipBytes(3) // skip extra metadata bytes (25 total CTRE payload size minus 1 CAN ID minus 21 currents)

                                for (i in currents.indices) {
                                    batcher.add(TelemetryFrame(timestampMs, sessionId, "/DSLog/PowerDistributionCurrents[$i]", currents[i]))
                                }
                            }
                        }

                        recordCount++
                    }
                } catch (e: EOFException) {
                    // Normal end of file
                }
            }
        }

        // Try parsing matching .dsevents file if it exists
        /**
         * eventsFile val.
         */
        val eventsFile = File(dslogFile.parentFile, dslogFile.nameWithoutExtension + ".dsevents")
        if (eventsFile.exists()) {
            parseDsEvents(eventsFile, sessionId, startTimeMs)
        }
    }

    private suspend fun parseDsEvents(
        dseventsFile: File,
        sessionId: String,
        startTimeMs: Double
    ) {
        FileInputStream(dseventsFile).use { fis ->
            DataInputStream(fis).use { dis ->
                try {
                    /**
                     * version val.
                     */
                    val version = dis.readInt()
                    if (version != 4) return

                    /**
                     * seconds val.
                     */
                    val seconds = dis.readLong()
                    /**
                     * fractional val.
                     */
                    val fractional = dis.readLong()
                    /**
                     * fileStartTimeMs val.
                     */
                    val fileStartTimeMs = convertLVTime(seconds, fractional)

                    while (true) {
                        /**
                         * recSeconds val.
                         */
                        val recSeconds = dis.readLong()
                        /**
                         * recFractional val.
                         */
                        val recFractional = dis.readLong()
                        /**
                         * eventTimeMs val.
                         */
                        val eventTimeMs = convertLVTime(recSeconds, recFractional)

                        /**
                         * length val.
                         */
                        val length = dis.readInt()
                        if (length <= 0) continue

                        /**
                         * textBytes val.
                         */
                        val textBytes = ByteArray(length)
                        dis.readFully(textBytes)
                        /**
                         * text var.
                         */
                        var text = String(textBytes, Charsets.UTF_8)

                        // Filter XML tags
                        /**
                         * tags val.
                         */
                        val tags = listOf("<TagVersion>", "<time>", "<count>", "<flags>", "<Code>", "<location>", "<stack>")
                        for (tag in tags) {
                            while (text.contains(tag)) {
                                /**
                                 * tagIndex val.
                                 */
                                val tagIndex = text.indexOf(tag)
                                /**
                                 * nextIndex val.
                                 */
                                val nextIndex = text.indexOf("<", tagIndex + 1)
                                if (nextIndex != -1) {
                                    text = text.substring(0, tagIndex) + text.substring(nextIndex)
                                } else {
                                    text = text.substring(0, tagIndex)
                                }
                            }
                        }
                        text = text.replace("<message> ", "")
                        text = text.replace("<details> ", "")
                        text = text.trim()

                        /**
                         * relativeSec val.
                         */
                        val relativeSec = (eventTimeMs - fileStartTimeMs) / 1000.0
                        /**
                         * annotation val.
                         */
                        val annotation = SessionAnnotation(
                            annotationId = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            text = "[Event at +${String.format("%.2f", relativeSec)}s] $text",
                            createdAt = eventTimeMs.toLong(),
                            authorId = "Driver Station"
                        )
                        databaseService.insertAnnotation(annotation)
                    }
                } catch (e: EOFException) {
                    // Normal end of file
                }
            }
        }
    }

    private fun convertLVTime(seconds: Long, fractional: Long): Double {
        /**
         * time var.
         */
        var time = -2082826800L // 1904/1/1
        time += seconds
        /**
         * fracDouble val.
         */
        val fracDouble = fractional.toULong().toDouble() / Math.pow(2.0, 64.0)
        return (time.toDouble() + fracDouble) * 1000.0
    }

    private fun getPDType(id: Int): PowerDistributionType {
        return when (id) {
            33 -> PowerDistributionType.REV
            25 -> PowerDistributionType.CTRE
            else -> PowerDistributionType.NONE
        }
    }
}
