package com.ares.analytics.service

import com.ares.analytics.shared.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class HootDecoderService(
    private val databaseService: DatabaseService,
    private val summaryEngineService: SummaryEngineService,
    private val sysIdService: SysIdService
) {

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class MotorKeys(
        /**
         * motorName val.
         */
        val motorName: String,
        /**
         * voltageKey val.
         */
        val voltageKey: String,
        /**
         * velocityKey val.
         */
        val velocityKey: String,
        /**
         * currentKey val.
         */
        val currentKey: String?,
        /**
         * accelKey val.
         */
        val accelKey: String
    )

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SetpointPair(
        /**
         * actualKey val.
         */
        val actualKey: String,
        /**
         * setpointKey val.
         */
        val setpointKey: String
    )

    /**
     * Discovers CTRE's owlet CLI utility on the system.
     * Looks in environment path, user home folder, and AdvantageScope's downloaded binaries in AppData.
     */
    fun findOwletPath(): File? {
        /**
         * os val.
         */
        val os = System.getProperty("os.name").lowercase()
        /**
         * isWindows val.
         */
        val isWindows = os.contains("win")
        /**
         * exeExtension val.
         */
        val exeExtension = if (isWindows) ".exe" else ""

        // Check system path first
        /**
         * pathEnv val.
         */
        val pathEnv = System.getenv("PATH") ?: ""
        /**
         * pathDirs val.
         */
        val pathDirs = pathEnv.split(File.pathSeparator)
        for (dirStr in pathDirs) {
            /**
             * dir val.
             */
            val dir = File(dirStr)
            /**
             * file val.
             */
            val file = File(dir, "owlet$exeExtension")
            if (file.exists() && file.canExecute()) {
                return file
            }
        }

        // Check standard directories
        /**
         * pathsToCheck val.
         */
        val pathsToCheck = mutableListOf<File>()

        // 1. AdvantageScope downloaded binaries in AppData
        /**
         * appData val.
         */
        val appData = System.getenv("APPDATA")
        if (appData != null) {
            pathsToCheck.add(File(appData, "AdvantageScope/owlet"))
            pathsToCheck.add(File(appData, "AdvantageScope"))
        }
        /**
         * userHome val.
         */
        val userHome = System.getProperty("user.home")
        pathsToCheck.add(File(userHome, "Library/Application Support/AdvantageScope/owlet"))
        pathsToCheck.add(File(userHome, ".config/AdvantageScope/owlet"))
        pathsToCheck.add(File(userHome, ".ctre"))
        pathsToCheck.add(File(userHome, ".ctre/owlet"))

        for (dir in pathsToCheck) {
            if (dir.exists() && dir.isDirectory) {
                /**
                 * files val.
                 */
                val files = dir.listFiles { _, name ->
                    /**
                     * lower val.
                     */
                    val lower = name.lowercase()
                    lower.startsWith("owlet") && (exeExtension.isEmpty() || lower.endsWith(exeExtension))
                }
                if (files != null && files.isNotEmpty()) {
                    // Pick the latest one by name/version (e.g. owlet-26.3.0-C19.exe > owlet-25.0.0-C9.exe)
                    return files.sortedByDescending { it.name }.first()
                }
            }
        }
        return null
    }

    /**
     * Converts the selected `.hoot` binary file into a temporary CSV file,
     * reads and parses it line-by-line to write into the SQLite database, and runs diagnostics.
     */
    suspend fun importHootLog(
        hootFile: File,
        teamId: String,
        seasonId: String,
        robotId: String
    ): String = withContext(Dispatchers.IO) {
        /**
         * owletFile val.
         */
        val owletFile = findOwletPath() ?: throw IllegalStateException("owlet CLI tool not found. Please install CTRE tools or AdvantageScope.")
        
        /**
         * tempCsv val.
         */
        val tempCsv = File.createTempFile("hoot_import_", ".csv")
        tempCsv.deleteOnExit()

        /**
         * pb val.
         */
        val pb = ProcessBuilder(
            owletFile.absolutePath,
            hootFile.absolutePath,
            tempCsv.absolutePath,
            "-f",
            "csv"
        )
        
        /**
         * process val.
         */
        val process = pb.start()
        /**
         * finished val.
         */
        val finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            tempCsv.delete()
            throw IllegalStateException("owlet CLI timed out converting hoot log.")
        }
        /**
         * exitCode val.
         */
        val exitCode = process.exitValue()
        if (exitCode != 0) {
            tempCsv.delete()
            throw IllegalStateException("owlet CLI failed to convert hoot log. Exit code: $exitCode")
        }

        /**
         * sessionId val.
         */
        val sessionId = "hoot-${UUID.randomUUID()}"
        
        // Parse CSV and batch-insert into DB
        val (firstTime, lastTime, parsedKeys) = parseAndInsertTelemetry(tempCsv, sessionId)
        
        /**
         * durationMs val.
         */
        val durationMs = lastTime - firstTime
        if (durationMs <= 0L) {
            tempCsv.delete()
            throw IllegalArgumentException("Hoot log file contains no valid timestamp ranges.")
        }

        // Insert Session record
        /**
         * session val.
         */
        val session = Session(
            sessionId = sessionId,
            teamId = teamId,
            seasonId = seasonId,
            robotId = robotId,
            createdAt = firstTime,
            durationMs = durationMs,
            tags = listOf("hoot-import")
        )
        databaseService.insertSession(session)

        // Generate summary details
        /**
         * summary val.
         */
        val summary = summaryEngineService.generateSummary(session)
        databaseService.insertSessionSummary(summary)

        // Execute Post-Processing diagnostic pipeline
        runDiagnostics(sessionId, parsedKeys, firstTime, lastTime, durationMs)

        tempCsv.delete()
        sessionId
    }

    internal suspend fun parseAndInsertTelemetry(
        csvFile: File,
        sessionId: String
    ): Triple<Long, Long, Set<String>> = withContext(Dispatchers.IO) {
        /**
         * absolutePath val.
         */
        val absolutePath = csvFile.absolutePath.replace("\\", "/")

        // 1. Read header to detect time column and extract key names
        /**
         * reader val.
         */
        val reader = csvFile.bufferedReader(Charsets.UTF_8)
        /**
         * headerLine val.
         */
        val headerLine: String
        /**
         * headers val.
         */
        val headers: List<String>
        /**
         * keysSet val.
         */
        val keysSet: Set<String>
        /**
         * scale val.
         */
        val scale: Double
        try {
            headerLine = reader.readLine() ?: throw IllegalArgumentException("Empty CSV file.")
            headers = headerLine.split(",").map { it.trim().replace("\"", "") }
            if (headers.isEmpty() || !headers[0].lowercase().contains("time")) {
                throw IllegalArgumentException("Invalid CSV header format: first column must be timestamp")
            }
            keysSet = headers.drop(1).toSet()

            // 2. Auto-detect timestamp scale by reading first two data lines
            /**
             * firstLine val.
             */
            val firstLine = reader.readLine() ?: return@withContext Triple(0L, 0L, emptySet<String>())
            /**
             * secondLine val.
             */
            val secondLine = reader.readLine()

            /**
             * parts1 val.
             */
            val parts1 = firstLine.split(",")
            /**
             * t1 val.
             */
            val t1 = parts1[0].toDoubleOrNull() ?: 0.0

            scale = when {
                secondLine != null -> {
                    /**
                     * parts2 val.
                     */
                    val parts2 = secondLine.split(",")
                    /**
                     * t2 val.
                     */
                    val t2 = parts2[0].toDoubleOrNull() ?: 0.0
                    /**
                     * dt val.
                     */
                    val dt = abs(t2 - t1)
                    when {
                        dt > 1000.0 -> 0.001  // microseconds → ms
                        dt < 1.0 -> 1000.0    // seconds → ms
                        else -> 1.0           // already ms
                    }
                }
                else -> 1.0
            }
        } finally {
            reader.close()
        }

        /**
         * escapedSessionId val.
         */
        val escapedSessionId = sessionId.replace("'", "''")
        /**
         * escapedTimeCol val.
         */
        val escapedTimeCol = headers[0].replace("'", "''").replace("\"", "\"\"")

        // 3. DuckDB native UNPIVOT import — single SQL pass, no Kotlin-side string parsing
        try {
            databaseService.executeRaw("""
                INSERT INTO telemetry_frames (timestamp_ms, session_id, key, value, string_value)
                SELECT
                    CAST(CAST("$escapedTimeCol" AS DOUBLE) * $scale AS BIGINT) AS timestamp_ms,
                    '$escapedSessionId' AS session_id,
                    key,
                    COALESCE(
                        CASE
                            WHEN LOWER(CAST(value AS VARCHAR)) = 'true' THEN 1.0
                            WHEN LOWER(CAST(value AS VARCHAR)) = 'false' THEN 0.0
                            ELSE TRY_CAST(value AS DOUBLE)
                        END,
                        0.0
                    ) AS value,
                    CASE
                        WHEN LOWER(CAST(value AS VARCHAR)) IN ('true', 'false') THEN NULL
                        WHEN TRY_CAST(value AS DOUBLE) IS NULL THEN CAST(value AS VARCHAR)
                    END AS string_value
                FROM (
                    SELECT * FROM read_csv_auto('$absolutePath', header=true, ignore_errors=true, all_varchar=true)
                ) UNPIVOT (
                    value FOR key IN (* EXCLUDE ("$escapedTimeCol"))
                )
                WHERE value IS NOT NULL AND CAST(value AS VARCHAR) != ''
            """.trimIndent())
        } catch (e: Exception) {
            // Fallback: re-parse with original streaming approach if UNPIVOT fails
            parseAndInsertTelemetryFallback(csvFile, sessionId, headers, scale)
        }

        // 4. Query time range from imported data
        /**
         * range val.
         */
        val range = databaseService.getSessionTimestampRange(sessionId)
        /**
         * firstTime val.
         */
        val firstTime = range?.first ?: 0L
        /**
         * lastTime val.
         */
        val lastTime = range?.second ?: 0L

        Triple(firstTime, lastTime, keysSet)
    }

    /** Fallback streaming parser for CSV files that DuckDB cannot natively import. */
    private suspend fun parseAndInsertTelemetryFallback(
        csvFile: File,
        sessionId: String,
        headers: List<String>,
        scale: Double
    ) {
        /**
         * reader val.
         */
        val reader = csvFile.bufferedReader(Charsets.UTF_8)
        try {
            reader.readLine() // skip header
            /**
             * batch val.
             */
            val batch = mutableListOf<TelemetryFrame>()
            /**
             * line var.
             */
            var line: String? = reader.readLine()
            while (line != null) {
                /**
                 * parts val.
                 */
                val parts = line.split(",")
                if (parts.isNotEmpty()) {
                    /**
                     * rawTime val.
                     */
                    val rawTime = parts[0].toDoubleOrNull()
                    if (rawTime != null) {
                        /**
                         * timeMs val.
                         */
                        val timeMs = (rawTime * scale).toLong()
                        for (i in 1 until minOf(parts.size, headers.size)) {
                            /**
                             * valueStr val.
                             */
                            val valueStr = parts[i].trim()
                            if (valueStr.isNotEmpty()) {
                                /**
                                 * value val.
                                 */
                                val value = valueStr.toDoubleOrNull()
                                if (value != null) {
                                    batch.add(TelemetryFrame(timeMs, sessionId, headers[i], value))
                                }
                            }
                        }
                        if (batch.size >= 5000) {
                            databaseService.insertTelemetryFrames(batch)
                            batch.clear()
                        }
                    }
                }
                line = reader.readLine()
            }
            if (batch.isNotEmpty()) {
                databaseService.insertTelemetryFrames(batch)
            }
        } finally {
            reader.close()
        }
    }

    private suspend fun runDiagnostics(
        sessionId: String,
        keys: Set<String>,
        firstTime: Long,
        lastTime: Long,
        durationMs: Long
    ) {
        /**
         * motors val.
         */
        val motors = mutableMapOf<String, MotorKeys>()
        
        // 1. Detect Motors from voltage and velocity patterns
        for (key in keys) {
            /**
             * parts val.
             */
            val parts = key.split("/")
            if (parts.size >= 2) {
                /**
                 * last val.
                 */
                val last = parts.last().lowercase()
                if (last == "voltage" || last == "appliedoutput" || last == "appliedvolts" || last.contains("motorvoltage")) {
                    /**
                     * name val.
                     */
                    val name = parts[parts.size - 2]
                    /**
                     * pathPrefix val.
                     */
                    val pathPrefix = parts.dropLast(1).joinToString("/")
                    
                    /**
                     * velKey val.
                     */
                    val velKey = keys.firstOrNull { 
                        it.startsWith(pathPrefix) && (it.endsWith("Velocity") || it.endsWith("VelocityRps") || it.lowercase().contains("speed")) 
                    }
                    /**
                     * currentKey val.
                     */
                    val currentKey = keys.firstOrNull {
                        it.startsWith(pathPrefix) && (it.endsWith("Current") || it.endsWith("StatorCurrent") || it.lowercase().contains("amps"))
                    }
                    
                    if (velKey != null) {
                        motors[name] = MotorKeys(
                            motorName = name,
                            voltageKey = key,
                            velocityKey = velKey,
                            currentKey = currentKey,
                            accelKey = "$pathPrefix/Acceleration"
                        )
                    }
                }
            }
        }

        // 2. Compute derivative acceleration if missing, and run SysId
        /**
         * sysIdResults val.
         */
        val sysIdResults = mutableMapOf<String, CalculatedSummary>()
        for (motor in motors.values) {
            if (!keys.contains(motor.accelKey)) {
                /**
                 * velocities val.
                 */
                val velocities = databaseService.getTelemetryForKey(sessionId, motor.velocityKey)
                /**
                 * accelFrames val.
                 */
                val accelFrames = mutableListOf<TelemetryFrame>()
                for (i in 1 until velocities.size) {
                    /**
                     * prev val.
                     */
                    val prev = velocities[i-1]
                    /**
                     * curr val.
                     */
                    val curr = velocities[i]
                    /**
                     * dt val.
                     */
                    val dt = (curr.timestampMs - prev.timestampMs) / 1000.0
                    if (dt > 0.0) {
                        /**
                         * accel val.
                         */
                        val accel = (curr.value - prev.value) / dt
                        accelFrames.add(TelemetryFrame(curr.timestampMs, sessionId, motor.accelKey, accel))
                    }
                }
                databaseService.insertTelemetryFrames(accelFrames)
            }

            /**
             * summary val.
             */
            val summary = sysIdService.analyzeMotorData(sessionId, motor.voltageKey, motor.velocityKey, motor.accelKey)
            if (summary.rSquared > 0.1) {
                sysIdResults[motor.motorName] = summary
            }
        }

        if (sysIdResults.isNotEmpty()) {
            /**
             * report val.
             */
            val report = StringBuilder()
            report.append("Drive Motor SysId characterization results:\n")
            for ((name, summary) in sysIdResults) {
                report.append("- $name: kS = ${"%.4f".format(summary.kS)}, kV = ${"%.4f".format(summary.kV)}, kA = ${"%.4f".format(summary.kA)} (R² = ${"%.2f".format(summary.rSquared)})\n")
            }
            /**
             * sysIdNote val.
             */
            val sysIdNote = SessionAnnotation(
                annotationId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                text = report.toString(),
                createdAt = System.currentTimeMillis(),
                authorId = "SysId Engine"
            )
            databaseService.insertAnnotation(sysIdNote)
        }

        // 3. PID & Backlash FFT oscillation audit
        /**
         * setpointPairs val.
         */
        val setpointPairs = mutableListOf<SetpointPair>()
        for (key in keys) {
            /**
             * lowercase val.
             */
            val lowercase = key.lowercase()
            if (lowercase.endsWith("setpoint") || lowercase.contains("setpoint") || lowercase.endsWith("target") || lowercase.contains("target")) {
                /**
                 * baseKey val.
                 */
                val baseKey = keys.firstOrNull { 
                    it != key && (key.startsWith(it) || it.startsWith(key.replace("setpoint", "", true).replace("target", "", true)))
                }
                if (baseKey != null) {
                    setpointPairs.add(SetpointPair(baseKey, key))
                }
            }
        }

        for (pair in setpointPairs) {
            /**
             * actuals val.
             */
            val actuals = databaseService.getTelemetryForKey(sessionId, pair.actualKey)
            /**
             * setpoints val.
             */
            val setpoints = databaseService.getTelemetryForKey(sessionId, pair.setpointKey)
            if (actuals.size >= 32 && setpoints.isNotEmpty()) {
                /**
                 * actualsSorted val.
                 */
                val actualsSorted = actuals.sortedBy { it.timestampMs }
                /**
                 * setpointsSorted val.
                 */
                val setpointsSorted = setpoints.sortedBy { it.timestampMs }
                /**
                 * errorList val.
                 */
                val errorList = mutableListOf<Double>()
                /**
                 * timestamps val.
                 */
                val timestamps = mutableListOf<Long>()
                /**
                 * setpointIndex var.
                 */
                var setpointIndex = 0

                for (act in actualsSorted) {
                    /**
                     * targetTime val.
                     */
                    val targetTime = act.timestampMs
                    while (setpointIndex + 1 < setpointsSorted.size &&
                           abs(setpointsSorted[setpointIndex + 1].timestampMs - targetTime) <= abs(setpointsSorted[setpointIndex].timestampMs - targetTime)) {
                        setpointIndex++
                    }
                    /**
                     * spVal val.
                     */
                    val spVal = setpointsSorted[setpointIndex].value
                    errorList.add(spVal - act.value)
                    timestamps.add(act.timestampMs)
                }

                if (errorList.size >= 32) {
                    /**
                     * avgDeltaMs val.
                     */
                    val avgDeltaMs = (timestamps.last() - timestamps.first()).toDouble() / (timestamps.size - 1)
                    /**
                     * sampleRateHz val.
                     */
                    val sampleRateHz = if (avgDeltaMs > 0.0) 1000.0 / avgDeltaMs else 50.0
                    
                    /**
                     * fftResult val.
                     */
                    val fftResult = sysIdService.performFftAnalysis(errorList.toDoubleArray(), sampleRateHz)
                    /**
                     * domFreq val.
                     */
                    val domFreq = fftResult.dominantFrequency
                    /**
                     * maxError val.
                     */
                    val maxError = errorList.map { abs(it) }.maxOrNull() ?: 0.0

                    if (maxError > 0.05 && domFreq in 2.0..40.0) {
                        /**
                         * alert val.
                         */
                        val alert = AlertRecord(
                            alertId = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            ruleKey = "/Diagnostics/BacklashFFT/${pair.actualKey}",
                            triggerTimestampMs = firstTime,
                            resolveTimestampMs = lastTime,
                            durationMs = durationMs,
                            peakValue = domFreq,
                            triaged = false
                        )
                        databaseService.insertAlert(alert)
                    }
                }
            }
        }

        // 4. Thermal & Stall diagnostics
        for (motor in motors.values) {
            /**
             * currentKey val.
             */
            val currentKey = motor.currentKey ?: continue
            /**
             * currents val.
             */
            val currents = databaseService.getTelemetryForKey(sessionId, currentKey)
            /**
             * velocities val.
             */
            val velocities = databaseService.getTelemetryForKey(sessionId, motor.velocityKey)
            
            if (currents.isEmpty() || velocities.isEmpty()) continue

            /**
             * currentsSorted val.
             */
            val currentsSorted = currents.sortedBy { it.timestampMs }
            /**
             * velocitiesSorted val.
             */
            val velocitiesSorted = velocities.sortedBy { it.timestampMs }
            /**
             * velIndex var.
             */
            var velIndex = 0
            /**
             * thermalSum var.
             */
            var thermalSum = 0.0
            /**
             * maxStallDurationMs var.
             */
            var maxStallDurationMs = 0L
            /**
             * currentStallDurationMs var.
             */
            var currentStallDurationMs = 0L
            /**
             * lastTimeMs var.
             */
            var lastTimeMs = 0L
            
            for (currFrame in currentsSorted) {
                /**
                 * t val.
                 */
                val t = currFrame.timestampMs
                while (velIndex + 1 < velocitiesSorted.size &&
                       abs(velocitiesSorted[velIndex + 1].timestampMs - t) <= abs(velocitiesSorted[velIndex].timestampMs - t)) {
                    velIndex++
                }
                /**
                 * velFrame val.
                 */
                val velFrame = velocitiesSorted[velIndex]
                
                /**
                 * current val.
                 */
                val current = currFrame.value
                /**
                 * velocity val.
                 */
                val velocity = velFrame.value
                
                if (lastTimeMs > 0L) {
                    /**
                     * dt val.
                     */
                    val dt = (t - lastTimeMs) / 1000.0
                    if (dt > 0.0) {
                        thermalSum += current * current * 0.05 * dt // I^2 * R * dt, R = 0.05 Ohms
                        
                        if (current > 40.0 && abs(velocity) < 0.1) {
                            currentStallDurationMs += (t - lastTimeMs)
                            maxStallDurationMs = maxOf(maxStallDurationMs, currentStallDurationMs)
                        } else {
                            currentStallDurationMs = 0L
                        }
                    }
                }
                lastTimeMs = t
            }

            if (maxStallDurationMs >= 500L) {
                /**
                 * alert val.
                 */
                val alert = AlertRecord(
                    alertId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    ruleKey = "/Diagnostics/MotorStall/${motor.motorName}",
                    triggerTimestampMs = firstTime,
                    resolveTimestampMs = lastTime,
                    durationMs = maxStallDurationMs,
                    peakValue = maxStallDurationMs.toDouble(),
                    triaged = false
                )
                databaseService.insertAlert(alert)
            }

            if (thermalSum > 10000.0) {
                /**
                 * alert val.
                 */
                val alert = AlertRecord(
                    alertId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    ruleKey = "/Diagnostics/ThermalLoad/${motor.motorName}",
                    triggerTimestampMs = firstTime,
                    resolveTimestampMs = lastTime,
                    durationMs = durationMs,
                    peakValue = thermalSum,
                    triaged = false
                )
                databaseService.insertAlert(alert)
            }
        }

        // 5. CAN Jitter analysis on periodic update signals
        for (key in keys) {
            /**
             * frames val.
             */
            val frames = databaseService.getTelemetryForKey(sessionId, key)
            if (frames.size < 50) continue
            
            /**
             * deltas val.
             */
            val deltas = mutableListOf<Double>()
            /**
             * prevTime var.
             */
            var prevTime = 0L
            for (f in frames.sortedBy { it.timestampMs }) {
                if (prevTime > 0L) {
                    deltas.add((f.timestampMs - prevTime).toDouble())
                }
                prevTime = f.timestampMs
            }
            
            if (deltas.isEmpty()) continue
            /**
             * avg val.
             */
            val avg = deltas.average()
            /**
             * variance val.
             */
            val variance = deltas.map { (it - avg) * (it - avg) }.average()
            /**
             * stdDev val.
             */
            val stdDev = sqrt(variance)

            // Flag signals with high jitter (> 8ms standard deviation for signals < 100ms interval)
            if (stdDev > 8.0 && avg < 100.0) {
                /**
                 * alert val.
                 */
                val alert = AlertRecord(
                    alertId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    ruleKey = "/Diagnostics/CANJitter/$key",
                    triggerTimestampMs = firstTime,
                    resolveTimestampMs = lastTime,
                    durationMs = durationMs,
                    peakValue = stdDev,
                    triaged = false
                )
                databaseService.insertAlert(alert)
            }
        }
    }
}
