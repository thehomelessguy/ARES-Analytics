import com.google.cloud.firestore.FirestoreOptions

/**
 * High-level description: Handles data processing pipeline, UI state management (MVI), or Ktor endpoint logic.
 * Physical units: Distances in $m$, angles in $rad$, velocities in $m/s$ or $rad/s$, time in $s$.
 * Canvas-to-field coordinate transformation conventions applied where relevant.
 *
 * @param args relevant arguments
 * @return expected results
 */
fun main() {
    try {
        val db = FirestoreOptions.getDefaultInstance().service
        println("Collections:")
        db.listCollections().forEach { println(it.id) }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
