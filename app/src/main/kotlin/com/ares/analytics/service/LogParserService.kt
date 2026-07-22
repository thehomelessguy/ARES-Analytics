package com.ares.analytics.service

import com.ares.analytics.service.log.JsonlLogDecoder
import com.ares.analytics.service.log.WpiLogDecoder
import com.ares.analytics.service.log.CsvLogDecoder
import com.ares.analytics.shared.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
class LogParserService(
    private val databaseService: DatabaseService,
    private val summaryEngineService: SummaryEngineService
) {
    private val jsonlDecoder = JsonlLogDecoder(databaseService)
    private val wpiLogDecoder = WpiLogDecoder()
    private val csvLogDecoder = CsvLogDecoder(databaseService)

    suspend fun parseLogFile(
        file: File,
        teamId: String,
        seasonId: String,
        robotId: String,
        matchNumber: Int? = null,
        allianceColor: String? = null,
        tags: List<String> = emptyList()
    ): Session = withContext(Dispatchers.IO) {
        /**
         * sessionId val.
         */
        val sessionId = UUID.randomUUID().toString()
        /**
         * createdAt val.
         */
        val createdAt = file.lastModified()
        /**
         * session val.
         */
        val session = Session(
            sessionId = sessionId,
            teamId = teamId,
            seasonId = seasonId,
            robotId = robotId,
            createdAt = createdAt,
            matchNumber = matchNumber,
            allianceColor = allianceColor,
            tags = tags
        )

        /**
         * batcher val.
         */
        val batcher = FrameBatcher(databaseService)
        /**
         * lowerName val.
         */
        val lowerName = file.name.lowercase()

        when {
            lowerName.endsWith(".wpilog") -> {
                wpiLogDecoder.parseWpiLog(file, sessionId, batcher)
            }
            lowerName.endsWith(".wpilogxz") -> {
                /**
                 * tempWpiFile val.
                 */
                val tempWpiFile = File.createTempFile("wpilog_", ".wpilog")
                try {
                    FileInputStream(file).use { fis ->
                        org.tukaani.xz.XZInputStream(fis).use { xzis ->
                            tempWpiFile.outputStream().use { fos ->
                                xzis.copyTo(fos)
                            }
                        }
                    }
                    wpiLogDecoder.parseWpiLog(tempWpiFile, sessionId, batcher)
                } finally {
                    tempWpiFile.delete()
                }
            }
            lowerName.endsWith(".jsonl") -> {
                if (lowerName.startsWith("action_log_")) {
                    /**
                     * actionMeta val.
                     */
                    val actionMeta = jsonlDecoder.parseActionLogJsonl(file, sessionId)
                    if (actionMeta != null) {
                        /**
                         * enrichedSession val.
                         */
                        val enrichedSession = session.copy(
                            durationMs = actionMeta.durationMs,
                            matchNumber = matchNumber ?: actionMeta.matchNumber,
                            allianceColor = allianceColor ?: actionMeta.alliance,
                            tags = tags + "action-log"
                        )
                        databaseService.insertSession(enrichedSession)
                        /**
                         * summary val.
                         */
                        val summary = summaryEngineService.generateSummary(enrichedSession)
                        databaseService.insertSessionSummary(summary)
                        return@withContext enrichedSession
                    }
                } else {
                    jsonlDecoder.parseJsonlLog(file, sessionId, batcher)
                }
            }
            lowerName.endsWith(".csv") -> {
                databaseService.insertSession(session)
                csvLogDecoder.parseCsvLogNative(file, sessionId)
                /**
                 * range val.
                 */
                val range = databaseService.getSessionTimestampRange(sessionId)
                if (range != null) {
                    /**
                     * finalSession val.
                     */
                    val finalSession = session.copy(durationMs = range.second - range.first)
                    databaseService.insertSession(finalSession)
                    /**
                     * summary val.
                     */
                    val summary = summaryEngineService.generateSummary(finalSession)
                    databaseService.insertSessionSummary(summary)
                    return@withContext finalSession
                } else {
                    /**
                     * summary val.
                     */
                    val summary = summaryEngineService.generateSummary(session)
                    databaseService.insertSessionSummary(summary)
                    return@withContext session
                }
            }
            lowerName.endsWith(".dslog") || lowerName.endsWith(".dsevents") -> {
                /**
                 * targetFile val.
                 */
                val targetFile = if (lowerName.endsWith(".dsevents")) {
                    File(file.parentFile, file.nameWithoutExtension + ".dslog")
                } else {
                    file
                }
                DSLogDecoderService(databaseService).parseDsLog(targetFile, sessionId, batcher)
            }
            lowerName.endsWith(".log") -> {
                RoadRunnerDecoderService().parseRoadRunnerLog(file, sessionId, batcher)
            }
            lowerName.endsWith(".rlog") -> {
                RlogDecoderService().parseRlog(file, sessionId, batcher)
            }
            lowerName.endsWith(".revlog") -> {
                RevlogDecoderService(databaseService).parseRevlog(file, sessionId, batcher, this@LogParserService)
            }
            else -> {
                throw IllegalArgumentException("Unsupported log file format: ${file.name}")
            }
        }

        batcher.flush()
        databaseService.insertSession(session)

        /**
         * finalSession val.
         */
        val finalSession = if (batcher.frameCount > 0) {
            /**
             * duration val.
             */
            val duration = batcher.maxTimestamp - batcher.minTimestamp
            /**
             * s val.
             */
            val s = session.copy(durationMs = duration)
            databaseService.insertSession(s)
            s
        } else {
            session
        }

        /**
         * summary val.
         */
        val summary = summaryEngineService.generateSummary(finalSession)
        databaseService.insertSessionSummary(summary)
        return@withContext finalSession
    }

    suspend fun parseLogFiles(
        files: List<File>,
        teamId: String,
        seasonId: String,
        robotId: String,
        matchNumber: Int? = null,
        allianceColor: String? = null,
        tags: List<String> = emptyList()
    ): Session = withContext(Dispatchers.IO) {
        if (files.isEmpty()) throw IllegalArgumentException("No log files provided")
        if (files.size == 1) {
            return@withContext parseLogFile(files.first(), teamId, seasonId, robotId, matchNumber, allianceColor, tags)
        }

        /**
         * sessionId val.
         */
        val sessionId = UUID.randomUUID().toString()
        /**
         * createdAt val.
         */
        val createdAt = files.first().lastModified()
        
        /**
         * currentMatchNumber var.
         */
        var currentMatchNumber = matchNumber
        /**
         * currentAlliance var.
         */
        var currentAlliance = allianceColor
        /**
         * currentTags var.
         */
        var currentTags = tags

        /**
         * globalMinTimestamp var.
         */
        var globalMinTimestamp = Long.MAX_VALUE
        /**
         * globalMaxTimestamp var.
         */
        var globalMaxTimestamp = Long.MIN_VALUE

        files.forEachIndexed { index, file ->
            /**
             * batcher val.
             */
            val batcher = FrameBatcher(databaseService, keyTransform = { key ->
                key.removePrefix("/")
            })

            /**
             * lowerName val.
             */
            val lowerName = file.name.lowercase()
            when {
                lowerName.endsWith(".wpilog") -> wpiLogDecoder.parseWpiLog(file, sessionId, batcher)
                lowerName.endsWith(".wpilogxz") -> {
                    /**
                     * tempWpiFile val.
                     */
                    val tempWpiFile = File.createTempFile("wpilog_", ".wpilog")
                    try {
                        FileInputStream(file).use { fis ->
                            org.tukaani.xz.XZInputStream(fis).use { xzis ->
                                tempWpiFile.outputStream().use { fos ->
                                    xzis.copyTo(fos)
                                }
                            }
                        }
                        wpiLogDecoder.parseWpiLog(tempWpiFile, sessionId, batcher)
                    } finally {
                        tempWpiFile.delete()
                    }
                }
                lowerName.endsWith(".jsonl") -> {
                    if (lowerName.startsWith("action_log_")) {
                        /**
                         * actionMeta val.
                         */
                        val actionMeta = jsonlDecoder.parseActionLogJsonl(file, sessionId)
                        if (actionMeta != null) {
                            currentMatchNumber = currentMatchNumber ?: actionMeta.matchNumber
                            currentAlliance = currentAlliance ?: actionMeta.alliance
                            if (!currentTags.contains("action-log")) {
                                currentTags = currentTags + "action-log"
                            }
                        }
                    } else {
                        jsonlDecoder.parseJsonlLog(file, sessionId, batcher)
                    }
                }
                lowerName.endsWith(".csv") -> {
                    csvLogDecoder.parseCsvLogNative(file, sessionId)
                }
                lowerName.endsWith(".dslog") || lowerName.endsWith(".dsevents") -> {
                    /**
                     * targetFile val.
                     */
                    val targetFile = if (lowerName.endsWith(".dsevents")) {
                        File(file.parentFile, file.nameWithoutExtension + ".dslog")
                    } else {
                        file
                    }
                    DSLogDecoderService(databaseService).parseDsLog(targetFile, sessionId, batcher)
                }
                lowerName.endsWith(".log") -> RoadRunnerDecoderService().parseRoadRunnerLog(file, sessionId, batcher)
                lowerName.endsWith(".rlog") -> RlogDecoderService().parseRlog(file, sessionId, batcher)
                lowerName.endsWith(".revlog") -> RevlogDecoderService(databaseService).parseRevlog(file, sessionId, batcher, this@LogParserService)
                else -> throw IllegalArgumentException("Unsupported log file format: ${file.name}")
            }

            batcher.flush()

            if (batcher.frameCount > 0) {
                if (batcher.minTimestamp < globalMinTimestamp) globalMinTimestamp = batcher.minTimestamp
                if (batcher.maxTimestamp > globalMaxTimestamp) globalMaxTimestamp = batcher.maxTimestamp
            }
        }

        /**
         * baseSession val.
         */
        val baseSession = Session(
            sessionId = sessionId,
            teamId = teamId,
            seasonId = seasonId,
            robotId = robotId,
            createdAt = createdAt,
            matchNumber = currentMatchNumber,
            allianceColor = currentAlliance,
            tags = currentTags
        )

        databaseService.insertSession(baseSession)

        /**
         * range val.
         */
        val range = databaseService.getSessionTimestampRange(sessionId)
        /**
         * finalSession val.
         */
        val finalSession = if (range != null) {
            /**
             * duration val.
             */
            val duration = range.second - range.first
            /**
             * s val.
             */
            val s = baseSession.copy(durationMs = duration)
            databaseService.insertSession(s)
            s
        } else {
            baseSession
        }
        
        /**
         * summary val.
         */
        val summary = summaryEngineService.generateSummary(finalSession)
        databaseService.insertSessionSummary(summary)
        
        return@withContext finalSession
    }

    internal suspend fun parseWpiLog(file: File, sessionId: String, batcher: FrameBatcher) {
        wpiLogDecoder.parseWpiLog(file, sessionId, batcher)
    }
}
