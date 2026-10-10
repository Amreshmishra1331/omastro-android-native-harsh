package com.omastro.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * On-device "My Data" tables (added in DB v2): local storage for Kundali
 * charts, numerology results, and reusable birth profiles.
 *
 * Design:
 * - `kundali_charts`: one row per generated Kundali — the FULL server
 *   `KundaliResponse` JSON (Gson, verbatim) plus the `KundaliRequest` JSON so
 *   a saved chart re-opens instantly from stored JSON: no re-computation,
 *   no API call. Birth details are stored as plain columns for search/labels.
 * - `numerology_results`: one row per computed tool result — the
 *   `ToolResult` JSON (Gson) plus the input map JSON; re-opens instantly.
 * - `birth_profiles`: reusable birth details (name, dob, tob, place,
 *   lat/lon, gender) the user can pick instead of retyping on the Kundali
 *   birth form.
 *
 * Auto-save: every freshly generated Kundali and every completed
 * numerology tool result is written here automatically; refused/errored
 * analyses are never saved (callers only invoke save on success). AI chat
 * is intentionally NOT stored here — chat history stays cloud-based.
 *
 * History caps: [MAX_KUNDALI]/[MAX_NUMEROLOGY]/[MAX_PROFILES]; the oldest
 * rows are pruned on insert.
 */
object MyDataFeatureType {
    const val KUNDALI = "kundali"
    const val NUMEROLOGY = "numerology"
    const val VASTU = ReadingFeatureType.VASTU
    const val PALMISTRY = ReadingFeatureType.PALMISTRY
    const val FACE = ReadingFeatureType.FACE
}

@Entity(tableName = "kundali_charts")
data class LocalKundaliChartEntity(
    /** Random UUID generated at save time — never a server id. */
    @PrimaryKey val id: String,
    /** Save time, epoch millis. */
    val createdAt: Long,
    /** User-editable label shown on the card. */
    val label: String,
    /** Birth details, denormalized for labels/search (no re-compute needed). */
    val birthName: String,
    val dob: String,
    val tob: String,
    val pob: String,
    val lat: Double,
    val lon: Double,
    val gender: String,
    /** Full [KundaliRequest] JSON (Gson) — restores the request verbatim. */
    val requestJson: String,
    /** Full [KundaliResponse] JSON (Gson) — restores all 23 tabs verbatim. */
    val resultJson: String
)

@Entity(tableName = "numerology_results")
data class LocalNumerologyResultEntity(
    /** Random UUID generated at save time — never a server id. */
    @PrimaryKey val id: String,
    /** Save time, epoch millis. */
    val createdAt: Long,
    /** User-editable label shown on the card. */
    val label: String,
    /** Tool id, e.g. "moolank" / "loshu". */
    val toolId: String,
    /** Bilingual tool display name captured at save time. */
    val toolName: String,
    /** Inputs used for the calculation (name/dob/phone/gender), Gson map. */
    val inputsJson: String,
    /** Full `ToolResult` JSON (Gson) — re-renders verbatim, no re-compute. */
    val resultJson: String
)

@Entity(tableName = "birth_profiles")
data class BirthProfileEntity(
    /** Random UUID generated at save time. */
    @PrimaryKey val id: String,
    /** Save time, epoch millis. */
    val createdAt: Long,
    /** Profile label chosen by the user (e.g. "Me", "Mother"). */
    val name: String,
    val dob: String, // YYYY-MM-DD
    val tob: String, // HH:MM AM/PM as shown in the form
    val pob: String,
    val lat: Double,
    val lon: Double,
    val gender: String
)

@Dao
interface LocalKundaliChartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocalKundaliChartEntity)

    @Query("SELECT * FROM kundali_charts ORDER BY createdAt DESC")
    suspend fun getAll(): List<LocalKundaliChartEntity>

    @Query("SELECT * FROM kundali_charts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LocalKundaliChartEntity?

    @Query("UPDATE kundali_charts SET label = :label WHERE id = :id")
    suspend fun updateLabel(id: String, label: String)

    @Query("DELETE FROM kundali_charts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM kundali_charts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM kundali_charts")
    suspend fun count(): Int
}

@Dao
interface LocalNumerologyResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocalNumerologyResultEntity)

    @Query("SELECT * FROM numerology_results ORDER BY createdAt DESC")
    suspend fun getAll(): List<LocalNumerologyResultEntity>

    @Query("SELECT * FROM numerology_results WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LocalNumerologyResultEntity?

    @Query("UPDATE numerology_results SET label = :label WHERE id = :id")
    suspend fun updateLabel(id: String, label: String)

    @Query("DELETE FROM numerology_results WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM numerology_results")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM numerology_results")
    suspend fun count(): Int
}

@Dao
interface BirthProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BirthProfileEntity)

    @Query("SELECT * FROM birth_profiles ORDER BY createdAt DESC")
    suspend fun getAll(): List<BirthProfileEntity>

    @Query("SELECT * FROM birth_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BirthProfileEntity?

    @Query("DELETE FROM birth_profiles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM birth_profiles")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM birth_profiles")
    suspend fun count(): Int
}

/** DB migration 1 -> 2: add the "My Data" tables (vision_readings untouched). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `kundali_charts` (
                `id` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                `label` TEXT NOT NULL, `birthName` TEXT NOT NULL,
                `dob` TEXT NOT NULL, `tob` TEXT NOT NULL, `pob` TEXT NOT NULL,
                `lat` REAL NOT NULL, `lon` REAL NOT NULL, `gender` TEXT NOT NULL,
                `requestJson` TEXT NOT NULL, `resultJson` TEXT NOT NULL,
                PRIMARY KEY(`id`))""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `numerology_results` (
                `id` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                `label` TEXT NOT NULL, `toolId` TEXT NOT NULL,
                `toolName` TEXT NOT NULL, `inputsJson` TEXT NOT NULL,
                `resultJson` TEXT NOT NULL,
                PRIMARY KEY(`id`))""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `birth_profiles` (
                `id` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                `name` TEXT NOT NULL, `dob` TEXT NOT NULL, `tob` TEXT NOT NULL,
                `pob` TEXT NOT NULL, `lat` REAL NOT NULL, `lon` REAL NOT NULL,
                `gender` TEXT NOT NULL,
                PRIMARY KEY(`id`))""".trimIndent()
        )
    }
}
