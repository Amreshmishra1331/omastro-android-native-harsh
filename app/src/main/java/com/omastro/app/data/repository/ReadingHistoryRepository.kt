package com.omastro.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.omastro.app.data.local.ReadingFeatureType
import com.omastro.app.data.local.ReadingHistoryDatabase
import com.omastro.app.data.local.VisionReadingEntity
import com.omastro.app.ui.i18n.W
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local reading-history store for Vastu / Palmistry / Face Reading.
 *
 * - [save] stores a completed analysis: feature type, timestamp,
 *   user-editable label, the full server result JSON (verbatim), and a
 *   downscaled thumbnail of the analyzed photo (256px max side, JPEG ~70%).
 *   Refusals and errors must never reach this method — callers filter them.
 * - History is capped at [MAX_ENTRIES]; inserting beyond the cap prunes the
 *   oldest rows and deletes their thumbnail files.
 * - Thumbnails and JSON are small by design (see storage notes below).
 */
class ReadingHistoryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = ReadingHistoryDatabase.get(appContext).visionReadingDao()

    /** One saved reading, as shown on history cards / the detail screen. */
    data class SavedReading(
        val id: String,
        val featureType: String,
        val createdAt: Long,
        val label: String,
        val resultJson: String,
        val thumbnailFile: String?
    )

    companion object {
        /** Hard cap on stored readings; oldest are pruned on insert. */
        const val MAX_ENTRIES = 100

        /** Thumbnail directory under [Context.getFilesDir]. */
        const val THUMB_DIR = "reading_thumbs"

        /** Longest thumbnail side, px. */
        const val THUMB_MAX_PX = 256

        /**
         * Storage budget: a 256px JPEG thumb is ~10-25 KB, a result JSON is
         * typically 2-8 KB. At the 100-entry cap the whole history stays
         * under ~3 MB of internal storage. Thumbnails are deleted with
         * their row (delete + prune paths) so nothing leaks.
         */
        fun thumbDir(context: Context): File =
            File(context.applicationContext.filesDir, THUMB_DIR)
    }

    /**
     * Saves a completed analysis. [resultJson] must be the full server
     * `data` JSON, verbatim. [photo] is the analyzed photo file, or null
     * when the analysis had no photo (manual-form Vastu). Returns the new
     * row id.
     */
    suspend fun save(
        featureType: String,
        resultJson: String,
        photo: File?
    ): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val thumbName = writeThumbnail(id, photo)
        dao.insert(
            VisionReadingEntity(
                id = id,
                featureType = featureType,
                createdAt = System.currentTimeMillis(),
                label = defaultLabel(featureType),
                resultJson = resultJson,
                thumbnailFile = thumbName
            )
        )
        pruneOverflow()
        id
    }

    suspend fun list(featureType: String? = null): List<SavedReading> =
        withContext(Dispatchers.IO) {
            val rows = if (featureType == null) dao.getAll() else dao.getByType(featureType)
            rows.map { it.toSaved() }
        }

    suspend fun get(id: String): SavedReading? = withContext(Dispatchers.IO) {
        dao.getById(id)?.toSaved()
    }

    /** Renames a saved reading; blank labels are ignored. */
    suspend fun rename(id: String, label: String) = withContext(Dispatchers.IO) {
        val clean = label.trim().take(80)
        if (clean.isNotEmpty()) dao.updateLabel(id, clean)
    }

    /** Deletes a reading and its thumbnail file. */
    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val row = dao.getById(id)
        dao.deleteById(id)
        row?.thumbnailFile?.let { deleteThumb(it) }
    }

    /** Resolves a stored thumbnail filename to a [File], or null. */
    fun thumbnailFile(name: String?): File? {
        if (name.isNullOrBlank()) return null
        val f = File(thumbDir(appContext), name)
        return if (f.exists()) f else null
    }

    /** Default card label: "<Feature> · <date>". Editable by the user later. */
    fun defaultLabel(featureType: String): String {
        val featureName = when (featureType) {
            ReadingFeatureType.VASTU -> W.t("vastu_title")
            ReadingFeatureType.PALMISTRY -> W.t("palm_title")
            ReadingFeatureType.FACE -> W.t("face_title")
            else -> featureType
        }
        val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            .format(Date(System.currentTimeMillis()))
        return "$featureName · $date"
    }

    /** Removes rows (and their thumbnails) beyond [MAX_ENTRIES], oldest first. */
    private suspend fun pruneOverflow() {
        val all = dao.getAll()
        if (all.size <= MAX_ENTRIES) return
        val overflow = all.drop(MAX_ENTRIES)
        overflow.forEach { row ->
            dao.deleteById(row.id)
            row.thumbnailFile?.let { deleteThumb(it) }
        }
    }

    /**
     * Downscales [photo] to [THUMB_MAX_PX] on the longest side and writes a
     * JPEG (~70%) into the thumbnail dir. Returns the filename, or null on
     * any failure / when [photo] is null. Never throws.
     */
    private fun writeThumbnail(id: String, photo: File?): String? {
        if (photo == null || !photo.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(photo.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (longest / (sample * 2) >= THUMB_MAX_PX) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = BitmapFactory.decodeFile(photo.absolutePath, opts) ?: return null
            val scaled = if (maxOf(bitmap.width, bitmap.height) > THUMB_MAX_PX) {
                val ratio = THUMB_MAX_PX.toFloat() / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt().coerceAtLeast(1),
                    (bitmap.height * ratio).toInt().coerceAtLeast(1),
                    true
                ).also { if (it != bitmap) bitmap.recycle() }
            } else bitmap
            val dir = thumbDir(appContext).apply { mkdirs() }
            val name = "$id.jpg"
            FileOutputStream(File(dir, name)).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }
            if (scaled != bitmap) scaled.recycle()
            bitmap.recycle()
            name
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    private fun deleteThumb(name: String) {
        try {
            File(thumbDir(appContext), name).delete()
        } catch (_: Exception) {
        }
    }

    private fun VisionReadingEntity.toSaved() = SavedReading(
        id = id,
        featureType = featureType,
        createdAt = createdAt,
        label = label,
        resultJson = resultJson,
        thumbnailFile = thumbnailFile
    )
}
