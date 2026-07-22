package com.ares.analytics.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.ares.analytics.shared.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
data class FieldEditorState(
    /**
     * fieldImage val.
     */
    val fieldImage: ImageBitmap? = null,
    /**
     * fieldImageConfig val.
     */
    val fieldImageConfig: FieldImageConfig = FieldImageConfig(),
    /**
     * obstacles val.
     */
    val obstacles: List<Obstacle> = emptyList(),
    /**
     * gamePieces val.
     */
    val gamePieces: List<GamePiece> = emptyList(),
    /**
     * aprilTags val.
     */
    val aprilTags: List<AprilTagPlacement> = emptyList(),
    /**
     * fieldWaypoints val.
     */
    val fieldWaypoints: List<FieldWaypoint> = emptyList(),
    /**
     * saveStatus val.
     */
    val saveStatus: String = "",
    /**
     * selectedElement val.
     */
    val selectedElement: String? = null,
    /**
     * isLoading val.
     */
    val isLoading: Boolean = false,
    /**
     * errorMessage val.
     */
    val errorMessage: String? = null
)

sealed class FieldEditorIntent {
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class LoadConfig(val projectPath: String?, val league: League) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SaveObstacles(val projectPath: String?, val league: League) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SaveGamePieces(val projectPath: String?, val league: League) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class ImportFieldImage(val imageFile: File, val projectPath: String?, val league: League) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateFieldImageConfig(val config: FieldImageConfig, val projectPath: String?, val league: League) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class AddObstacle(val obstacle: Obstacle) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateObstacle(val index: Int, val obstacle: Obstacle) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteObstacle(val index: Int) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class AddGamePiece(val piece: GamePiece) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateGamePiece(val index: Int, val piece: GamePiece) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteGamePiece(val index: Int) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SaveAprilTags(val projectPath: String?, val league: League) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class AddAprilTag(val tag: AprilTagPlacement) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateAprilTag(val index: Int, val tag: AprilTagPlacement) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteAprilTag(val index: Int) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SaveFieldWaypoints(val projectPath: String?, val league: League) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class AddFieldWaypoint(val waypoint: FieldWaypoint) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class UpdateFieldWaypoint(val index: Int, val waypoint: FieldWaypoint) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class DeleteFieldWaypoint(val index: Int) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SelectElement(val elementId: String?) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    object ClearSaveStatus : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SetObstacles(val obstacles: List<Obstacle>) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SetGamePieces(val gamePieces: List<GamePiece>) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SetAprilTags(val tags: List<AprilTagPlacement>) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class SetFieldWaypoints(val waypoints: List<FieldWaypoint>) : FieldEditorIntent()
    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    data class ImportFmap(val fmapContent: String, val projectPath: String?, val league: League) : FieldEditorIntent()
}

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class FieldEditorViewModel(
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(FieldEditorState())
    /**
     * state val.
     */
    val state: StateFlow<FieldEditorState> = _state.asStateFlow()

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun onIntent(intent: FieldEditorIntent) {
        scope.launch {
            when (intent) {
                is FieldEditorIntent.LoadConfig -> {
                    /**
                     * projectPath val.
                     */
                    val projectPath = intent.projectPath
                    /**
                     * league val.
                     */
                    val league = intent.league
                    _state.update { it.copy(isLoading = true, errorMessage = null) }
                    if (!projectPath.isNullOrEmpty()) {
                        try {
                            withContext(Dispatchers.IO) {
                                /**
                                 * relativePathsDir val.
                                 */
                                val relativePathsDir = if (league == League.FTC) {
                                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/paths"
                                    else "src/main/assets/paths"
                                } else {
                                    "src/main/deploy/paths"
                                }
                                /**
                                 * obsFile val.
                                 */
                                val obsFile = File(File(projectPath, relativePathsDir), "obstacles.json")
                                /**
                                 * loadedObstacles val.
                                 */
                                val loadedObstacles = if (obsFile.exists()) {
                                    Json.decodeFromString<List<Obstacle>>(obsFile.readText())
                                } else {
                                    emptyList()
                                }

                                /**
                                 * gpFile val.
                                 */
                                val gpFile = File(File(projectPath, relativePathsDir), "game_pieces.json")
                                /**
                                 * loadedGamePieces val.
                                 */
                                val loadedGamePieces = if (gpFile.exists()) {
                                    Json.decodeFromString<List<GamePiece>>(gpFile.readText())
                                } else {
                                    emptyList()
                                }

                                /**
                                 * atFile val.
                                 */
                                val atFile = File(File(projectPath, relativePathsDir), "apriltags.json")
                                /**
                                 * loadedAprilTags val.
                                 */
                                val loadedAprilTags = if (atFile.exists()) {
                                    Json.decodeFromString<List<AprilTagPlacement>>(atFile.readText())
                                } else {
                                    emptyList()
                                }

                                /**
                                 * wpFile val.
                                 */
                                val wpFile = File(File(projectPath, relativePathsDir), "field_waypoints.json")
                                /**
                                 * loadedFieldWaypoints val.
                                 */
                                val loadedFieldWaypoints = if (wpFile.exists()) {
                                    Json.decodeFromString<List<FieldWaypoint>>(wpFile.readText())
                                } else {
                                    emptyList()
                                }

                                /**
                                 * relativeDir val.
                                 */
                                val relativeDir = if (league == League.FTC) {
                                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets"
                                    else "src/main/assets"
                                } else {
                                    "src/main/deploy"
                                }
                                /**
                                 * targetDir val.
                                 */
                                val targetDir = File(projectPath, relativeDir)
                                /**
                                 * imgFile val.
                                 */
                                val imgFile = File(targetDir, "field_image.png")
                                /**
                                 * loadedBitmap val.
                                 */
                                val loadedBitmap = if (imgFile.exists()) {
                                    org.jetbrains.skia.Image.makeFromEncoded(imgFile.readBytes()).toComposeImageBitmap()
                                } else {
                                    null
                                }

                                /**
                                 * configFile val.
                                 */
                                val configFile = File(targetDir, "field_image_config.json")
                                /**
                                 * loadedConfig val.
                                 */
                                val loadedConfig = if (configFile.exists()) {
                                    Json.decodeFromString<FieldImageConfig>(configFile.readText())
                                } else {
                                    /**
                                     * defaultW val.
                                     */
                                    val defaultW = if (league == League.FTC) 3.65 else 16.5
                                    /**
                                     * defaultH val.
                                     */
                                    val defaultH = if (league == League.FTC) 3.65 else 8.2
                                    FieldImageConfig(widthMeters = defaultW, heightMeters = defaultH)
                                }

                                _state.update {
                                    it.copy(
                                        obstacles = loadedObstacles,
                                        gamePieces = loadedGamePieces,
                                        aprilTags = loadedAprilTags,
                                        fieldWaypoints = loadedFieldWaypoints,
                                        fieldImage = loadedBitmap,
                                        fieldImageConfig = loadedConfig,
                                        isLoading = false
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load field layout config") }
                        }
                    } else {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
                is FieldEditorIntent.SaveObstacles -> {
                    /**
                     * projectPath val.
                     */
                    val projectPath = intent.projectPath
                    /**
                     * league val.
                     */
                    val league = intent.league
                    /**
                     * s val.
                     */
                    val s = _state.value
                    if (!projectPath.isNullOrEmpty()) {
                        try {
                            withContext(Dispatchers.IO) {
                                /**
                                 * relativeDir val.
                                 */
                                val relativeDir = if (league == League.FTC) {
                                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/paths"
                                    else "src/main/assets/paths"
                                } else {
                                    "src/main/deploy/paths"
                                }
                                /**
                                 * targetDir val.
                                 */
                                val targetDir = File(projectPath, relativeDir)
                                targetDir.mkdirs()
                                /**
                                 * targetFile val.
                                 */
                                val targetFile = File(targetDir, "obstacles.json")
                                /**
                                 * jsonFormat val.
                                 */
                                val jsonFormat = Json { prettyPrint = true }
                                targetFile.writeText(jsonFormat.encodeToString(s.obstacles))
                            }
                            _state.update { it.copy(saveStatus = "Saved obstacles successfully!") }
                        } catch (e: Exception) {
                            _state.update { it.copy(saveStatus = "Failed to save obstacles: ${e.message}") }
                        }
                    }
                }
                is FieldEditorIntent.SaveGamePieces -> {
                    /**
                     * projectPath val.
                     */
                    val projectPath = intent.projectPath
                    /**
                     * league val.
                     */
                    val league = intent.league
                    /**
                     * s val.
                     */
                    val s = _state.value
                    if (!projectPath.isNullOrEmpty()) {
                        try {
                            withContext(Dispatchers.IO) {
                                /**
                                 * relativeDir val.
                                 */
                                val relativeDir = if (league == League.FTC) {
                                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/paths"
                                    else "src/main/assets/paths"
                                } else {
                                    "src/main/deploy/paths"
                                }
                                /**
                                 * targetDir val.
                                 */
                                val targetDir = File(projectPath, relativeDir)
                                targetDir.mkdirs()
                                /**
                                 * targetFile val.
                                 */
                                val targetFile = File(targetDir, "game_pieces.json")
                                /**
                                 * jsonFormat val.
                                 */
                                val jsonFormat = Json { prettyPrint = true }
                                targetFile.writeText(jsonFormat.encodeToString(s.gamePieces))
                            }
                            _state.update { it.copy(saveStatus = "Saved game pieces successfully!") }
                        } catch (e: Exception) {
                            _state.update { it.copy(saveStatus = "Failed to save game pieces: ${e.message}") }
                        }
                    }
                }
                is FieldEditorIntent.ImportFieldImage -> {
                    /**
                     * projectPath val.
                     */
                    val projectPath = intent.projectPath
                    /**
                     * league val.
                     */
                    val league = intent.league
                    if (!projectPath.isNullOrEmpty()) {
                        try {
                            withContext(Dispatchers.IO) {
                                /**
                                 * relativeDir val.
                                 */
                                val relativeDir = if (league == League.FTC) {
                                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets"
                                    else "src/main/assets"
                                } else {
                                    "src/main/deploy"
                                }
                                /**
                                 * targetDir val.
                                 */
                                val targetDir = File(projectPath, relativeDir)
                                targetDir.mkdirs()
                                /**
                                 * targetFile val.
                                 */
                                val targetFile = File(targetDir, "field_image.png")
                                intent.imageFile.copyTo(targetFile, overwrite = true)

                                /**
                                 * bitmap val.
                                 */
                                val bitmap = org.jetbrains.skia.Image.makeFromEncoded(targetFile.readBytes()).toComposeImageBitmap()
                                _state.update { it.copy(fieldImage = bitmap, saveStatus = "Field image imported successfully!") }
                            }
                        } catch (e: Exception) {
                            _state.update { it.copy(saveStatus = "Failed to import field image: ${e.message}") }
                        }
                    }
                }
                is FieldEditorIntent.UpdateFieldImageConfig -> {
                    /**
                     * projectPath val.
                     */
                    val projectPath = intent.projectPath
                    /**
                     * league val.
                     */
                    val league = intent.league
                    /**
                     * newConfig val.
                     */
                    val newConfig = intent.config
                    _state.update { it.copy(fieldImageConfig = newConfig) }
                    if (!projectPath.isNullOrEmpty()) {
                        try {
                            withContext(Dispatchers.IO) {
                                /**
                                 * relativeDir val.
                                 */
                                val relativeDir = if (league == League.FTC) {
                                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets"
                                    else "src/main/assets"
                                } else {
                                    "src/main/deploy"
                                }
                                /**
                                 * targetDir val.
                                 */
                                val targetDir = File(projectPath, relativeDir)
                                targetDir.mkdirs()
                                /**
                                 * configFile val.
                                 */
                                val configFile = File(targetDir, "field_image_config.json")
                                /**
                                 * jsonFormat val.
                                 */
                                val jsonFormat = Json { prettyPrint = true }
                                configFile.writeText(jsonFormat.encodeToString(newConfig))
                            }
                            _state.update { it.copy(saveStatus = "Field config updated successfully!") }
                        } catch (e: Exception) {
                            _state.update { it.copy(saveStatus = "Failed to save field config: ${e.message}") }
                        }
                    }
                }
                is FieldEditorIntent.AddObstacle -> {
                    /**
                     * updated val.
                     */
                    val updated = _state.value.obstacles + intent.obstacle
                    _state.update { it.copy(obstacles = updated) }
                }
                is FieldEditorIntent.UpdateObstacle -> {
                    /**
                     * updated val.
                     */
                    val updated = _state.value.obstacles.toMutableList().apply {
                        set(intent.index, intent.obstacle)
                    }
                    _state.update { it.copy(obstacles = updated) }
                }
                is FieldEditorIntent.DeleteObstacle -> {
                    /**
                     * updated val.
                     */
                    val updated = _state.value.obstacles.toMutableList().apply {
                        removeAt(intent.index)
                    }
                    _state.update { it.copy(obstacles = updated) }
                }
                is FieldEditorIntent.AddGamePiece -> {
                    /**
                     * updated val.
                     */
                    val updated = _state.value.gamePieces + intent.piece
                    _state.update { it.copy(gamePieces = updated) }
                }
                is FieldEditorIntent.UpdateGamePiece -> {
                    /**
                     * updated val.
                     */
                    val updated = _state.value.gamePieces.toMutableList().apply {
                        set(intent.index, intent.piece)
                    }
                    _state.update { it.copy(gamePieces = updated) }
                }
                is FieldEditorIntent.DeleteGamePiece -> {
                    /**
                     * updated val.
                     */
                    val updated = _state.value.gamePieces.toMutableList().apply {
                        removeAt(intent.index)
                    }
                    _state.update { it.copy(gamePieces = updated) }
                }
                is FieldEditorIntent.SaveAprilTags -> {
                    /**
                     * projectPath val.
                     */
                    val projectPath = intent.projectPath
                    /**
                     * league val.
                     */
                    val league = intent.league
                    /**
                     * s val.
                     */
                    val s = _state.value
                    if (!projectPath.isNullOrEmpty()) {
                        try {
                            withContext(Dispatchers.IO) {
                                /**
                                 * relativeDir val.
                                 */
                                val relativeDir = if (league == League.FTC) {
                                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/paths"
                                    else "src/main/assets/paths"
                                } else {
                                    "src/main/deploy/paths"
                                }
                                /**
                                 * targetDir val.
                                 */
                                val targetDir = File(projectPath, relativeDir)
                                targetDir.mkdirs()
                                /**
                                 * targetFile val.
                                 */
                                val targetFile = File(targetDir, "apriltags.json")
                                /**
                                 * jsonFormat val.
                                 */
                                val jsonFormat = Json { prettyPrint = true }
                                targetFile.writeText(jsonFormat.encodeToString(s.aprilTags))
                            }
                            _state.update { it.copy(saveStatus = "Saved AprilTags successfully!") }
                        } catch (e: Exception) {
                            _state.update { it.copy(saveStatus = "Failed to save AprilTags: ${e.message}") }
                        }
                    }
                }
                is FieldEditorIntent.AddAprilTag -> {
                    /**
                     * updated val.
                     */
                    val updated = _state.value.aprilTags + intent.tag
                    _state.update { it.copy(aprilTags = updated) }
                }
                is FieldEditorIntent.UpdateAprilTag -> {
                    /**
                     * updated val.
                     */
                    val updated = _state.value.aprilTags.toMutableList().apply {
                        set(intent.index, intent.tag)
                    }
                    _state.update { it.copy(aprilTags = updated) }
                }
                is FieldEditorIntent.DeleteAprilTag -> {
                    /**
                     * updated val.
                     */
                    val updated = _state.value.aprilTags.toMutableList().apply {
                        removeAt(intent.index)
                    }
                    _state.update { it.copy(aprilTags = updated) }
                }
                is FieldEditorIntent.SelectElement -> {
                    _state.update { it.copy(selectedElement = intent.elementId) }
                }
                is FieldEditorIntent.ClearSaveStatus -> {
                    _state.update { it.copy(saveStatus = "") }
                }
                is FieldEditorIntent.SetObstacles -> {
                    _state.update { it.copy(obstacles = intent.obstacles) }
                }
                is FieldEditorIntent.SetGamePieces -> {
                    _state.update { it.copy(gamePieces = intent.gamePieces) }
                }
                is FieldEditorIntent.SetAprilTags -> {
                    _state.update { it.copy(aprilTags = intent.tags) }
                }
                is FieldEditorIntent.SaveFieldWaypoints -> {
                    /**
                     * projectPath val.
                     */
                    val projectPath = intent.projectPath
                    /**
                     * league val.
                     */
                    val league = intent.league
                    /**
                     * s val.
                     */
                    val s = _state.value
                    if (!projectPath.isNullOrEmpty()) {
                        try {
                            withContext(Dispatchers.IO) {
                                /**
                                 * relativeDir val.
                                 */
                                val relativeDir = if (league == League.FTC) {
                                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/paths"
                                    else "src/main/assets/paths"
                                } else {
                                    "src/main/deploy/paths"
                                }
                                /**
                                 * targetDir val.
                                 */
                                val targetDir = File(projectPath, relativeDir)
                                targetDir.mkdirs()
                                /**
                                 * targetFile val.
                                 */
                                val targetFile = File(targetDir, "field_waypoints.json")
                                /**
                                 * jsonFormat val.
                                 */
                                val jsonFormat = Json { prettyPrint = true }
                                targetFile.writeText(jsonFormat.encodeToString(s.fieldWaypoints))
                            }
                            _state.update { it.copy(saveStatus = "Saved field waypoints successfully!") }
                        } catch (e: Exception) {
                            _state.update { it.copy(saveStatus = "Failed to save field waypoints: ${e.message}") }
                        }
                    }
                }
                is FieldEditorIntent.AddFieldWaypoint -> {
                    /**
                     * updated val.
                     */
                    val updated = _state.value.fieldWaypoints + intent.waypoint
                    _state.update { it.copy(fieldWaypoints = updated) }
                }
                is FieldEditorIntent.UpdateFieldWaypoint -> {
                    /**
                     * updated val.
                     */
                    val updated = _state.value.fieldWaypoints.toMutableList().apply {
                        set(intent.index, intent.waypoint)
                    }
                    _state.update { it.copy(fieldWaypoints = updated) }
                }
                is FieldEditorIntent.DeleteFieldWaypoint -> {
                    /**
                     * updated val.
                     */
                    val updated = _state.value.fieldWaypoints.toMutableList().apply {
                        removeAt(intent.index)
                    }
                    _state.update { it.copy(fieldWaypoints = updated) }
                }
                is FieldEditorIntent.SetFieldWaypoints -> {
                    _state.update { it.copy(fieldWaypoints = intent.waypoints) }
                }
                is FieldEditorIntent.ImportFmap -> {
                    /**
                     * projectPath val.
                     */
                    val projectPath = intent.projectPath
                    /**
                     * league val.
                     */
                    val league = intent.league
                    /**
                     * fmapContent val.
                     */
                    val fmapContent = intent.fmapContent
                    if (!projectPath.isNullOrEmpty()) {
                        try {
                            /**
                             * fmap val.
                             */
                            val fmap = Json.decodeFromString<LimelightFmap>(fmapContent)
                            /**
                             * placements val.
                             */
                            val placements = fmap.fiducials.mapNotNull { fiducial ->
                                /**
                                 * transform val.
                                 */
                                val transform = fiducial.transform
                                if (transform.size >= 16) {
                                    /**
                                     * tx val.
                                     */
                                    val tx = transform[3]
                                    /**
                                     * ty val.
                                     */
                                    val ty = transform[7]
                                    /**
                                     * tz val.
                                     */
                                    val tz = transform[11]
                                    /**
                                     * yawRad val.
                                     */
                                    val yawRad = kotlin.math.atan2(transform[4], transform[0])
                                    /**
                                     * yawDeg val.
                                     */
                                    val yawDeg = Math.toDegrees(yawRad)
                                    AprilTagPlacement(
                                        id = "apriltag_${fiducial.id}",
                                        tagId = fiducial.id,
                                        x = tx,
                                        y = ty,
                                        z = tz,
                                        yawDegrees = yawDeg
                                    )
                                } else null
                            }
                            _state.update { it.copy(aprilTags = placements) }

                            withContext(Dispatchers.IO) {
                                /**
                                 * relativeDir val.
                                 */
                                val relativeDir = if (league == League.FTC) {
                                    if (File(projectPath, "TeamCode/src/main/assets").exists()) "TeamCode/src/main/assets/paths"
                                    else "src/main/assets/paths"
                                } else {
                                    "src/main/deploy/paths"
                                }
                                /**
                                 * targetDir val.
                                 */
                                val targetDir = File(projectPath, relativeDir)
                                targetDir.mkdirs()
                                /**
                                 * targetFile val.
                                 */
                                val targetFile = File(targetDir, "apriltags.json")
                                /**
                                 * jsonFormat val.
                                 */
                                val jsonFormat = Json { prettyPrint = true }
                                targetFile.writeText(jsonFormat.encodeToString(placements))
                            }
                            _state.update { it.copy(saveStatus = "Imported and saved AprilTags successfully!") }
                        } catch (e: Exception) {
                            _state.update { it.copy(saveStatus = "Failed to parse fmap: ${e.message}") }
                        }
                    }
                }
            }
        }
    }
}

@Serializable
private data class LimelightFiducial(
    /**
     * id val.
     */
    val id: Int = 0,
    /**
     * family val.
     */
    val family: String? = null,
    /**
     * size val.
     */
    val size: Double = 0.0,
    /**
     * transform val.
     */
    val transform: List<Double> = emptyList()
)

@Serializable
private data class LimelightFmap(
    /**
     * fiducials val.
     */
    val fiducials: List<LimelightFiducial> = emptyList()
)
