package com.omastro.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.omastro.app.data.local.BirthProfileEntity
import com.omastro.app.data.local.LocalKundaliChartDao
import com.omastro.app.data.local.LocalKundaliChartEntity
import com.omastro.app.data.local.LocalNumerologyResultDao
import com.omastro.app.data.local.LocalNumerologyResultEntity
import com.omastro.app.data.local.ReadingHistoryDatabase
import com.omastro.app.data.model.KundaliRequest
import com.omastro.app.data.model.KundaliResponse
import com.omastro.app.ui.screens.numerology.ToolResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device "My Data" store: local Kundali charts, numerology results and
 * reusable birth profiles (DB v2 tables). AI chat is intentionally NOT
 * stored here — chat history stays cloud-based.
 *
 * - [saveKundali] stores the FULL [KundaliResponse] JSON verbatim plus the
 *   [KundaliRequest] JSON, so a saved chart re-opens instantly with zero
 *   re-computation and zero API calls. Called only on successful generation.
 * - [saveNumerology] stores the full `ToolResult` JSON verbatim plus the
 *   inputs; re-opens instantly. Called only on successful calculation.
 * - Birth profiles persist birth details so the user picks one instead of
 *   retyping on the Kundali form.
 *
 * Storage budget: a Kundali response JSON is typically 20-80 KB, a
 * numerology result 2-10 KB. At the caps below the whole store stays well
 * under ~5 MB of internal storage.
 */
class MyDataRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = ReadingHistoryDatabase.get(appContext)
    private val kundaliDao: LocalKundaliChartDao = db.localKundaliChartDao()
    private val numerologyDao: LocalNumerologyResultDao = db.localNumerologyResultDao()
    private val profileDao = db.birthProfileDao()
    private val gson = Gson()

    /** Saved Kundali chart as shown in My Data / opened instantly. */
    data class SavedKundali(
        val id: String,
        val createdAt: Long,
        val label: String,
        val birthName: String,
        val dob: String,
        val tob: String,
        val pob: String,
        val lat: Double,
        val lon: Double,
        val gender: String,
        val requestJson: String,
        val resultJson: String
    ) {
        fun toRequest(): KundaliRequest =
            Gson().fromJson(requestJson, KundaliRequest::class.java)

        fun toResponse(): KundaliResponse =
            Gson().fromJson(resultJson, KundaliResponse::class.java)
    }

    /** Saved numerology result as shown in My Data / re-rendered instantly. */
    data class SavedNumerology(
        val id: String,
        val createdAt: Long,
        val label: String,
        val toolId: String,
        val toolName: String,
        val inputsJson: String,
        val resultJson: String
    ) {
        fun inputs(): Map<String, String> = try {
            Gson().fromJson(inputsJson, object : TypeToken<Map<String, String>>() {}.type)
                ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }

        fun result(): ToolResult =
            Gson().fromJson(resultJson, ToolResult::class.java)
    }

    /** Reusable birth profile picked on the Kundali birth form. */
    data class BirthProfile(
        val id: String,
        val createdAt: Long,
        val name: String,
        val dob: String,
        val tob: String,
        val pob: String,
        val lat: Double,
        val lon: Double,
        val gender: String
    )

    companion object {
        /** Hard caps; oldest rows are pruned on insert. */
        const val MAX_KUNDALI = 50
        const val MAX_NUMEROLOGY = 50
        const val MAX_PROFILES = 30

        private const val DB_FILE_NAME = "reading_history.db"
    }

    // ------------------------------------------------------------------
    // Kundali charts
    // ------------------------------------------------------------------

    /** Auto-saves a freshly generated Kundali. Never call on failure. */
    suspend fun saveKundali(request: KundaliRequest, response: KundaliResponse): String =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                .format(Date(System.currentTimeMillis()))
            kundaliDao.insert(
                LocalKundaliChartEntity(
                    id = id,
                    createdAt = System.currentTimeMillis(),
                    label = "${request.name} · $date",
                    birthName = request.name,
                    dob = request.dob,
                    tob = request.tob,
                    pob = request.pob,
                    lat = request.lat,
                    lon = request.lon,
                    gender = request.gender,
                    requestJson = gson.toJson(request),
                    resultJson = gson.toJson(response)
                )
            )
            val overflow = kundaliDao.getAll().drop(MAX_KUNDALI)
            overflow.forEach { kundaliDao.deleteById(it.id) }
            id
        }

    suspend fun listKundalis(): List<SavedKundali> = withContext(Dispatchers.IO) {
        kundaliDao.getAll().map { it.toSavedKundali() }
    }

    suspend fun getKundali(id: String): SavedKundali? = withContext(Dispatchers.IO) {
        kundaliDao.getById(id)?.toSavedKundali()
    }

    /** Renames a saved chart; blank labels are ignored. */
    suspend fun renameKundali(id: String, label: String) = withContext(Dispatchers.IO) {
        val clean = label.trim().take(80)
        if (clean.isNotEmpty()) kundaliDao.updateLabel(id, clean)
    }

    suspend fun deleteKundali(id: String) = withContext(Dispatchers.IO) {
        kundaliDao.deleteById(id)
    }

    // ------------------------------------------------------------------
    // Numerology results
    // ------------------------------------------------------------------

    /**
     * Auto-saves a completed numerology tool result. [inputs] carries the
     * form values (name, dob, name2, dob2, phone, gender); [toolName] is the
     * bilingual tool display name captured at save time. Never call on
     * failure — only successful calculations reach this method.
     */
    suspend fun saveNumerology(
        toolId: String,
        toolName: String,
        inputs: Map<String, String>,
        result: ToolResult
    ): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            .format(Date(System.currentTimeMillis()))
        numerologyDao.insert(
            LocalNumerologyResultEntity(
                id = id,
                createdAt = System.currentTimeMillis(),
                label = "$toolName · $date",
                toolId = toolId,
                toolName = toolName,
                inputsJson = gson.toJson(inputs),
                resultJson = gson.toJson(result)
            )
        )
        val overflow = numerologyDao.getAll().drop(MAX_NUMEROLOGY)
        overflow.forEach { numerologyDao.deleteById(it.id) }
        id
    }

    suspend fun listNumerology(): List<SavedNumerology> = withContext(Dispatchers.IO) {
        numerologyDao.getAll().map { it.toSavedNumerology() }
    }

    suspend fun getNumerology(id: String): SavedNumerology? = withContext(Dispatchers.IO) {
        numerologyDao.getById(id)?.toSavedNumerology()
    }

    /** Renames a saved result; blank labels are ignored. */
    suspend fun renameNumerology(id: String, label: String) = withContext(Dispatchers.IO) {
        val clean = label.trim().take(80)
        if (clean.isNotEmpty()) numerologyDao.updateLabel(id, clean)
    }

    suspend fun deleteNumerology(id: String) = withContext(Dispatchers.IO) {
        numerologyDao.deleteById(id)
    }

    // ------------------------------------------------------------------
    // Birth profiles
    // ------------------------------------------------------------------

    suspend fun saveBirthProfile(
        name: String,
        dob: String,
        tob: String,
        pob: String,
        lat: Double,
        lon: Double,
        gender: String
    ): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        profileDao.insert(
            BirthProfileEntity(
                id = id,
                createdAt = System.currentTimeMillis(),
                name = name.trim().take(60),
                dob = dob,
                tob = tob,
                pob = pob,
                lat = lat,
                lon = lon,
                gender = gender
            )
        )
        val overflow = profileDao.getAll().drop(MAX_PROFILES)
        overflow.forEach { profileDao.deleteById(it.id) }
        id
    }

    suspend fun listBirthProfiles(): List<BirthProfile> = withContext(Dispatchers.IO) {
        profileDao.getAll().map { it.toBirthProfile() }
    }

    suspend fun deleteBirthProfile(id: String) = withContext(Dispatchers.IO) {
        profileDao.deleteById(id)
    }

    // ------------------------------------------------------------------
    // Storage management
    // ------------------------------------------------------------------

    /**
     * Bytes used by all on-device data: the Room database file (incl. WAL /
     * SHM) plus the vision reading thumbnails. Everything "My Data" lives
     * in internal storage only.
     */
    suspend fun storageUsedBytes(): Long = withContext(Dispatchers.IO) {
        var total = 0L
        val dbFile = appContext.getDatabasePath(DB_FILE_NAME)
        listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm"))
            .forEach { if (it.exists()) total += it.length() }
        try {
            File(appContext.filesDir, ReadingHistoryRepository.THUMB_DIR)
                .walkTopDown().forEach { if (it.isFile) total += it.length() }
        } catch (_: Exception) {
        }
        total
    }

    /** Deletes everything in My Data — all tables and all thumbnails. */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        kundaliDao.deleteAll()
        numerologyDao.deleteAll()
        profileDao.deleteAll()
        val visionDao = db.visionReadingDao()
        val rows = visionDao.getAll()
        rows.forEach { visionDao.deleteById(it.id) }
        rows.mapNotNull { it.thumbnailFile }.forEach { name ->
            try {
                File(appContext.filesDir, "${ReadingHistoryRepository.THUMB_DIR}/$name").delete()
            } catch (_: Exception) {
            }
        }
    }

    private fun LocalKundaliChartEntity.toSavedKundali() = SavedKundali(
        id = id, createdAt = createdAt, label = label,
        birthName = birthName, dob = dob, tob = tob, pob = pob,
        lat = lat, lon = lon, gender = gender,
        requestJson = requestJson, resultJson = resultJson
    )

    private fun LocalNumerologyResultEntity.toSavedNumerology() = SavedNumerology(
        id = id, createdAt = createdAt, label = label,
        toolId = toolId, toolName = toolName,
        inputsJson = inputsJson, resultJson = resultJson
    )

    private fun BirthProfileEntity.toBirthProfile() = BirthProfile(
        id = id, createdAt = createdAt, name = name,
        dob = dob, tob = tob, pob = pob,
        lat = lat, lon = lon, gender = gender
    )
}
