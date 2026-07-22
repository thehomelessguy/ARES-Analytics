package com.ares.analytics.viewmodel.pathing

import com.ares.analytics.viewmodel.PathPlannerState
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
class PathUndoRedoManager(
    private val stateFlow: MutableStateFlow<PathPlannerState>
) {
    private val undoStack = mutableListOf<PathPlannerState>()
    private val redoStack = mutableListOf<PathPlannerState>()

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun saveSnapshot() {
        undoStack.add(stateFlow.value)
        redoStack.clear()
        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun undo() {
        if (undoStack.isNotEmpty()) {
            /**
             * current val.
             */
            val current = stateFlow.value
            /**
             * previous val.
             */
            val previous = undoStack.removeLast()
            redoStack.add(current)
            stateFlow.value = previous
        }
    }

    /**
     * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
     * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
     * Canvas-to-field coordinate transformation conventions applied where relevant.
     *
     * @param args relevant arguments
     * @return expected results
     */
    fun redo() {
        if (redoStack.isNotEmpty()) {
            /**
             * current val.
             */
            val current = stateFlow.value
            /**
             * next val.
             */
            val next = redoStack.removeLast()
            undoStack.add(current)
            stateFlow.value = next
        }
    }
}
