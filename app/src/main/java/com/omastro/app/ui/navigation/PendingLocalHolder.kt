package com.omastro.app.ui.navigation

import com.omastro.app.ui.screens.numerology.ToolResult

/**
 * Temporary holder for locally-saved My Data items selected on the My Data
 * screen, to be opened by their target screens. Cleared after consumption.
 *
 * Locally-saved Kundalis and numerology results re-open from stored JSON —
 * no re-computation, no API call.
 */
object PendingLocalHolder {

    /** A saved numerology result ready to re-render instantly. */
    data class StoredNumerology(
        val toolId: String,
        val inputs: Map<String, String>,
        val result: ToolResult
    )

    var pendingKundaliId: String? = null
        private set
    var pendingNumerology: StoredNumerology? = null
        private set

    fun setKundali(id: String) {
        pendingKundaliId = id
    }

    fun setNumerology(stored: StoredNumerology) {
        pendingNumerology = stored
    }

    fun consumeKundali(): String? {
        val id = pendingKundaliId
        pendingKundaliId = null
        return id
    }

    fun consumeNumerology(): StoredNumerology? {
        val stored = pendingNumerology
        pendingNumerology = null
        return stored
    }
}
