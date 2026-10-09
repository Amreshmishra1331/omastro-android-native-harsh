package com.omastro.app.ui.navigation

/**
 * Temporary holder for birth details passed from the Kundali Numerology tab
 * to the Numerology hub, so tools open with name + DOB pre-filled.
 * Cleared after consumption.
 */
object PendingNumerologyHolder {
    var pendingName: String? = null
        private set
    var pendingDob: String? = null
        private set

    fun set(name: String, dob: String) {
        pendingName = name
        pendingDob = dob
    }

    fun consume(): Pair<String, String>? {
        val name = pendingName
        val dob = pendingDob
        pendingName = null
        pendingDob = null
        return if (!name.isNullOrBlank() && !dob.isNullOrBlank()) name to dob else null
    }
}
