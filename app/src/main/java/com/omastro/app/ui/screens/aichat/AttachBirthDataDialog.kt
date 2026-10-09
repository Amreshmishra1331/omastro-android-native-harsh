package com.omastro.app.ui.screens.aichat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.data.model.BirthDataAttachment
import com.omastro.app.data.model.PhotoContextAttachment
import com.omastro.app.data.remote.GeocodePlace
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.components.DatePickerField
import com.omastro.app.ui.components.PlaceSearchField
import com.omastro.app.ui.components.TimePickerField
import com.omastro.app.ui.theme.CosmicCharcoal
import com.omastro.app.ui.theme.CosmicDeepBlue
import com.omastro.app.ui.theme.CosmicNavy
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite

/**
 * "Attach Birth Data" dialog — website parity for the AI chat attach form.
 *
 * Fields: Name, Gender (M/F toggle), DOB (DD-MM-YYYY calendar picker),
 * TOB (AM/PM clock picker), Place (autocomplete via geocode API).
 * Multiple attachments are allowed; they accumulate and the context string
 * is rebuilt from all of them on the next message send.
 */
@Composable
fun AttachBirthDataDialog(
    onDismiss: () -> Unit,
    onAttach: (BirthDataAttachment) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") }
    var dobApi by remember { mutableStateOf("") } // YYYY-MM-DD
    var tob by remember { mutableStateOf("") }     // "HH:MM AM/PM"
    var place by remember { mutableStateOf("") }
    var selectedPlace by remember { mutableStateOf<GeocodePlace?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val canAttach = name.isNotBlank() && dobApi.isNotBlank() && place.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CosmicNavy,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    W.t("chat_attach_title"),
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = W.t("chat_close"),
                        tint = StarlightWhite.copy(alpha = 0.6f)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    W.t("chat_attach_sub"),
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )

                AttachTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = W.t("chat_attach_name")
                )

                // Gender toggle
                Column {
                    Text(
                        W.t("chat_attach_gender"),
                        color = StarlightWhite.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("M" to W.t("chat_attach_male"), "F" to W.t("chat_attach_female")).forEach { (value, label) ->
                            val selected = gender == value
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) SacredGold else CosmicCharcoal
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) SacredGold
                                        else StarlightWhite.copy(alpha = 0.2f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { gender = value }
                                    .padding(horizontal = 24.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (selected) CosmicDeepBlue else StarlightWhite,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                DatePickerField(
                    apiDate = dobApi,
                    onDateChange = { dobApi = it },
                    label = W.t("chat_attach_dob")
                )

                TimePickerField(
                    displayTime = tob,
                    onTimeChange = { tob = it },
                    label = W.t("chat_attach_tob")
                )

                PlaceSearchField(
                    value = place,
                    onValueChange = {
                        place = it
                        // Manual edit clears the picked place so coords stay honest.
                        if (selectedPlace != null && it != selectedPlace?.displayName) {
                            selectedPlace = null
                        }
                    },
                    onPlaceSelected = {
                        selectedPlace = it
                        place = it.displayName
                    },
                    label = W.t("chat_attach_place")
                )

                validationError?.let {
                    Text(
                        it,
                        color = Color(0xFFFF8A80),
                        fontSize = 12.5.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        name.isBlank() -> validationError = W.t("chat_attach_err_name")
                        dobApi.isBlank() -> validationError = W.t("chat_attach_err_dob")
                        place.isBlank() -> validationError = W.t("chat_attach_err_place")
                        else -> {
                            onAttach(
                                BirthDataAttachment(
                                    name = name.trim(),
                                    gender = gender,
                                    dob = dobApi,
                                    tob = tob.ifBlank { "12:00 PM" },
                                    place = place.trim(),
                                    lat = selectedPlace?.lat,
                                    lon = selectedPlace?.lon,
                                    tz = selectedPlace?.tz
                                )
                            )
                        }
                    }
                },
                enabled = canAttach,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SacredGold,
                    disabledContainerColor = StarlightWhite.copy(alpha = 0.15f),
                    contentColor = CosmicDeepBlue,
                    disabledContentColor = StarlightWhite.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(W.t("chat_attach_btn"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(W.t("chat_cancel"), color = StarlightWhite.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun AttachTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SacredGold,
            unfocusedBorderColor = StarlightWhite.copy(alpha = 0.2f),
            focusedTextColor = StarlightWhite,
            unfocusedTextColor = StarlightWhite,
            focusedLabelColor = SacredGold,
            unfocusedLabelColor = StarlightWhite.copy(alpha = 0.55f),
            cursorColor = SacredGold
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Chips row showing attached birth-data profiles, each removable.
 */
@Composable
fun AttachmentChips(
    attachments: List<BirthDataAttachment>,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        attachments.forEachIndexed { index, attachment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SacredGold.copy(alpha = 0.12f))
                    .border(
                        1.dp,
                        SacredGold.copy(alpha = 0.35f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        attachment.chipLabel,
                        color = SacredGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        attachment.summary,
                        color = StarlightWhite.copy(alpha = 0.55f),
                        fontSize = 11.5.sp,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onRemove(index) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = W.t("chat_remove_cd"),
                        tint = StarlightWhite.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * Chips row showing attached photo-analysis contexts (vision / palmistry /
 * vastu parsed facts), each removable. Same mechanism as birth-data chips.
 */
@Composable
fun PhotoAttachmentChips(
    attachments: List<PhotoContextAttachment>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        attachments.forEach { attachment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF4A90D9).copy(alpha = 0.12f))
                    .border(
                        1.dp,
                        Color(0xFF4A90D9).copy(alpha = 0.35f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = Color(0xFF4A90D9),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            W.t("chat_photo_analysis"),
                            color = Color(0xFF7FB5E8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            attachment.label,
                            color = StarlightWhite.copy(alpha = 0.55f),
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onRemove(attachment.id) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = W.t("chat_remove_cd"),
                        tint = StarlightWhite.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}
