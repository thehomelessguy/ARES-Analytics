package com.ares.analytics.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class TunableConstant(
    val name: String,
    val value: Double?,
    val filePath: String,
    val lineIndex: Int, // 0-indexed line number in file
    val isKotlin: Boolean
)

class ConstantsParserService {

    private val kotlinRegex = Regex("""(?:val|var)\s+([A-Z_0-9]+)(?:\s*:\s*[a-zA-Z0-9\?]+)?\s*=\s*(-?\d+\.?\d*|null)""")
    private val javaRegex = Regex("""double\s+([A-Z_0-9]+)\s*=\s*(-?\d+\.?\d*|null);""")

    suspend fun loadTunableConstants(projectPath: String): List<TunableConstant> = withContext(Dispatchers.IO) {
        val root = File(projectPath)
        if (!root.exists() || !root.isDirectory) return@withContext emptyList()

        val list = mutableListOf<TunableConstant>()
        val searchFiles = root.walkTopDown()
            .filter { it.name == "Constants.kt" || it.name == "RobotConfig.java" || it.name == "Constants.java" || it.name == "TunerConstants.kt" || it.name == "TunerConstants.java" }

        for (file in searchFiles) {
            val isKotlin = file.name.endsWith(".kt")
            val lines = file.readLines()
            for (idx in lines.indices) {
                val line = lines[idx]
                val match = if (isKotlin) kotlinRegex.find(line) else javaRegex.find(line)
                if (match != null) {
                    val name = match.groupValues[1]
                    val valueStr = match.groupValues[2]
                    val value = if (valueStr == "null") null else valueStr.toDoubleOrNull()
                    list.add(
                        TunableConstant(
                            name = name,
                            value = value,
                            filePath = file.absolutePath,
                            lineIndex = idx,
                            isKotlin = isKotlin
                        )
                    )
                }
            }
        }
        list
    }

    suspend fun saveConstant(constant: TunableConstant, newValue: Double) = withContext(Dispatchers.IO) {
        val file = File(constant.filePath)
        if (!file.exists()) return@withContext

        val lines = file.readLines().toMutableList()
        if (constant.lineIndex >= lines.size) return@withContext

        val originalLine = lines[constant.lineIndex]
        val newLine = if (constant.isKotlin) {
            originalLine.replace(Regex("""=\s*(-?\d+\.?\d*|null)"""), "= $newValue")
        } else {
            originalLine.replace(Regex("""=\s*(-?\d+\.?\d*|null);"""), "= $newValue;")
        }

        lines[constant.lineIndex] = newLine
        file.writeText(lines.joinToString("\n"))
    }
}
