package com.ares.analytics.service

import com.ares.analytics.shared.ControllerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class KeybindingParserService {
    suspend fun parseBindings(projectPath: String): List<ControllerBinding> = withContext(Dispatchers.IO) {
        /**
         * root val.
         */
        val root = File(projectPath)
        /**
         * bindings val.
         */
        val bindings = mutableListOf<ControllerBinding>()
        if (!root.exists()) return@withContext emptyList()

        /**
         * ktFiles val.
         */
        val ktFiles = root.walkTopDown()
            .filter { it.extension == "kt" || it.extension == "java" }
            .filter { !it.absolutePath.contains("build") }
            .toList()

        /**
         * dslRegex val.
         */
        val dslRegex = Regex("""\b([a-zA-Z0-9_]+)\.([a-zA-Z_]+)\.(onPress|onRelease|whilePressed|label)\(\s*"([^"]+)"\s*\)""")
        
        for (file in ktFiles) {
            /**
             * lines val.
             */
            val lines = file.readLines()
            for ((index, line) in lines.withIndex()) {
                /**
                 * match val.
                 */
                val match = dslRegex.find(line)
                if (match != null) {
                    /**
                     * varName val.
                     */
                    val varName = match.groupValues[1]
                    /**
                     * button val.
                     */
                    val button = match.groupValues[2]
                    /**
                     * eventType val.
                     */
                    val eventType = match.groupValues[3] // onPress etc
                    /**
                     * description val.
                     */
                    val description = match.groupValues[4]
                    
                    /**
                     * gamepadId val.
                     */
                    val gamepadId = if (varName.contains("operator") || varName.contains("2")) "gamepad2" else "gamepad1"
                    /**
                     * actionLabel val.
                     */
                    val actionLabel = "[$eventType] $description"
                    
                    bindings.add(ControllerBinding(gamepadId, button, actionLabel, file.name, index + 1))
                }
            }
        }
        
        bindings
    }
}
