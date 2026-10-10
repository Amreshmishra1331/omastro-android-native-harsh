package com.omastro.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local reading history for the vision features (Vastu, Palmistry, Face
 * Reading). Completed analyses are auto-saved here so results survive
 * leaving the screen; nothing is ever re-analyzed or fabricated.
 *
 * Storage design:
 * - One row per completed analysis: feature type, epoch-millis timestamp,
 *   a user-editable label, the FULL server `data` JSON (verbatim — rendered
 *   back exactly as the server returned it, including null-score states),
 *   and the thumbnail filename.
 * - Thumbnails live as JPEG files under `<filesDir>/reading_thumbs/`,
 *   downscaled to 256px on the longest side — never the full photo.
 * - History is capped at [ReadingHistoryRepository.MAX_ENTRIES] rows; the
 *   oldest are pruned (with their thumbnails) on insert.
 *
 * Refusals and errors are NEVER saved — only successful analyses.
 */
object ReadingFeatureType {
    const val VASTU = "vastu"
    const val PALMISTRY = "palmistry"
    const val FACE = "face"
}

@Entity(tableName = "vision_readings")
data class VisionReadingEntity(
    /** Random UUID generated at save time — never a server id. */
    @PrimaryKey val id: String,
    /** One of [ReadingFeatureType] constants. */
    val featureType: String,
    /** Save time, epoch millis. */
    val createdAt: Long,
    /** User-editable label shown on the history card. */
    val label: String,
    /** Full server `data` JSON, verbatim (Gson of the response map). */
    val resultJson: String,
    /** Thumbnail filename under `<filesDir>/reading_thumbs/`, or null when
     * the analysis had no photo (e.g. manual-form Vastu). */
    val thumbnailFile: String?
)

@Dao
interface VisionReadingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VisionReadingEntity)

    @Query("SELECT * FROM vision_readings ORDER BY createdAt DESC")
    suspend fun getAll(): List<VisionReadingEntity>

    @Query("SELECT * FROM vision_readings WHERE featureType = :type ORDER BY createdAt DESC")
    suspend fun getByType(type: String): List<VisionReadingEntity>

    @Query("SELECT * FROM vision_readings WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VisionReadingEntity?

    @Query("UPDATE vision_readings SET label = :label WHERE id = :id")
    suspend fun updateLabel(id: String, label: String)

    @Query("DELETE FROM vision_readings WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(
    entities = [
        VisionReadingEntity::class,
        LocalKundaliChartEntity::class,
        LocalNumerologyResultEntity::class,
        BirthProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ReadingHistoryDatabase : RoomDatabase() {
    abstract fun visionReadingDao(): VisionReadingDao
    abstract fun localKundaliChartDao(): LocalKundaliChartDao
    abstract fun localNumerologyResultDao(): LocalNumerologyResultDao
    abstract fun birthProfileDao(): BirthProfileDao

    companion object {
        private const val DB_NAME = "reading_history.db"

        @Volatile
        private var INSTANCE: ReadingHistoryDatabase? = null

        fun get(context: Context): ReadingHistoryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReadingHistoryDatabase::class.java,
                    DB_NAME
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
