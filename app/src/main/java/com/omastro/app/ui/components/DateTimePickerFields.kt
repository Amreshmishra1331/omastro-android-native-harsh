package com.omastro.app.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.TextSecondary
import java.util.Calendar

/**
 * Date input field with a native Calendar picker.
 *
 * Displays the date in DD-MM-YYYY format (user-facing) and reports the
 * API format (YYYY-MM-DD) via [onDateChange].
 *
 * @param apiDate current date in YYYY-MM-DD (API format), or empty
 * @param onDateChange called with YYYY-MM-DD when the user picks a date
 * @param label field label
 */
@Composable
fun DatePickerField(
    apiDate: String,
    onDateChange: (String) -> Unit,
    label: String = "Date of Birth",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Display format: DD-MM-YYYY
    val displayDate = remember(apiDate) { apiToDisplayDate(apiDate) }

    fun openPicker() {
        val cal = Calendar.getInstance()
        // Parse current value if present
        parseApiDate(apiDate)?.let { (y, m, d) ->
            cal.set(y, m - 1, d)
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                // month is 0-based from the picker
                onDateChange(String.format("%04d-%02d-%02d", year, month + 1, day))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    OutlinedTextField(
        value = displayDate,
        onValueChange = {},
        readOnly = true,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text("DD-MM-YYYY", fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = SacredGold.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SacredGold,
            unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
            focusedLabelColor = SacredGold,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = SacredGold
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { openPicker() }
    )
}

/**
 * Time input field with a native Clock picker (12-hour with AM/PM).
 *
 * Displays the time as HH:MM AM/PM (user-facing) and reports the same
 * format via [onTimeChange] (backend accepts "08:30 AM").
 *
 * @param displayTime current time like "08:30 AM", "14:30", or empty
 * @param onTimeChange called with "HH:MM AM/PM" when the user picks a time
 * @param label field label
 */
@Composable
fun TimePickerField(
    displayTime: String,
    onTimeChange: (String) -> Unit,
    label: String = "Time of Birth",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Normalize display: ensure AM/PM suffix
    val normalized = remember(displayTime) { normalizeTimeDisplay(displayTime) }

    fun openPicker() {
        val cal = Calendar.getInstance()
        parseTimeTo24(displayTime)?.let { (h, m) ->
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, m)
        }
        // false = 12-hour view with AM/PM selector
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val amPm = if (hourOfDay < 12) "AM" else "PM"
                val hour12 = when {
                    hourOfDay == 0 -> 12
                    hourOfDay > 12 -> hourOfDay - 12
                    else -> hourOfDay
                }
                onTimeChange(String.format("%02d:%02d %s", hour12, minute, amPm))
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    OutlinedTextField(
        value = normalized,
        onValueChange = {},
        readOnly = true,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text("HH:MM AM/PM", fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = SacredGold.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SacredGold,
            unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
            focusedLabelColor = SacredGold,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = SacredGold
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { openPicker() }
    )
}

/** Convert YYYY-MM-DD -> DD-MM-YYYY for display. Returns "" if unparseable. */
fun apiToDisplayDate(apiDate: String): String {
    val parts = apiDate.trim().split("-")
    if (parts.size != 3) return ""
    val (y, m, d) = parts
    if (y.length != 4) return ""
    return "$d-$m-$y"
}

/** Convert DD-MM-YYYY -> YYYY-MM-DD for the API. Returns "" if unparseable. */
fun displayToApiDate(displayDate: String): String {
    val parts = displayDate.trim().split("-")
    if (parts.size != 3) return ""
    val (d, m, y) = parts
    if (y.length != 4) return ""
    return "$y-$m-$d"
}

/** Parse YYYY-MM-DD into (year, month, day). Null if invalid. */
fun parseApiDate(apiDate: String): Triple<Int, Int, Int>? {
    return try {
        val parts = apiDate.trim().split("-")
        if (parts.size != 3) return null
        Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    } catch (e: Exception) {
        null
    }
}

/**
 * Normalize a time string to "HH:MM AM/PM" display.
 * Accepts "08:30 AM", "8:30 pm", "14:30" (24h), "08:30".
 */
fun normalizeTimeDisplay(time: String): String {
    val t = time.trim()
    if (t.isEmpty()) return ""
    // Already has AM/PM
    if (t.endsWith("AM", ignoreCase = true) || t.endsWith("PM", ignoreCase = true)) {
        return t.uppercase().replace(Regex("\\s+"), " ")
    }
    // 24-hour -> convert
    val parts = t.split(":")
    if (parts.size != 2) return t
    return try {
        val h24 = parts[0].toInt()
        val m = parts[1].toInt()
        val amPm = if (h24 < 12) "AM" else "PM"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        String.format("%02d:%02d %s", h12, m, amPm)
    } catch (e: Exception) {
        t
    }
}

/** Parse "08:30 AM" or "14:30" into (hour24, minute). Null if invalid. */
fun parseTimeTo24(time: String): Pair<Int, Int>? {
    return try {
        val t = time.trim().uppercase()
        val isPm = t.endsWith("PM")
        val isAm = t.endsWith("AM")
        val core = t.replace(Regex("\\s*(AM|PM)\\s*$"), "")
        val parts = core.split(":")
        if (parts.size != 2) return null
        var h = parts[0].toInt()
        val m = parts[1].toInt()
        if (isPm && h < 12) h += 12
        if (isAm && h == 12) h = 0
        if (h !in 0..23 || m !in 0..59) return null
        Pair(h, m)
    } catch (e: Exception) {
        null
    }
}
