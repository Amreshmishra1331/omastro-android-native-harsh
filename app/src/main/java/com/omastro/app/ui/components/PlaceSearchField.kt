package com.omastro.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.GeocodePlace
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite
import com.omastro.app.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Place search field with debounced autocomplete.
 *
 * Type a city name -> queries GET /geocode/search?q= (debounced ~400ms) ->
 * dropdown shows matching places -> tap one to select.
 *
 * On selection, [onPlaceSelected] receives the place (name, lat, lon, tz).
 * The text field keeps the place display name; manual editing clears the
 * selection so the user can type a new query.
 *
 * Manual lat/lon entry remains available via the sibling fields in the
 * parent form; this component only fills them when a place is picked.
 */
@Composable
fun PlaceSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onPlaceSelected: (GeocodePlace) -> Unit,
    label: String = "Place of Birth",
    placeholder: String = "e.g. Jaipur, Rajasthan",
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var query by remember(value) { mutableStateOf(value) }
    var places by remember { mutableStateOf<List<GeocodePlace>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Keep internal query in sync when parent sets value externally.
    LaunchedEffect(value) {
        if (value != query) {
            query = value
        }
    }

    fun runSearch(text: String) {
        searchJob?.cancel()
        if (text.trim().length < 2) {
            places = emptyList()
            isSearching = false
            hasSearched = false
            dropdownExpanded = false
            return
        }
        searchJob = scope.launch {
            delay(400)
            isSearching = true
            searchError = null
            try {
                val res = ApiClient.apiService.searchPlaces(text.trim())
                val body = res.body()
                if (res.isSuccessful && body != null && body.success) {
                    places = body.allPlaces
                    hasSearched = true
                    dropdownExpanded = true
                } else {
                    places = emptyList()
                    hasSearched = true
                    searchError = body?.message ?: "Search failed. Please try again."
                    dropdownExpanded = true
                }
            } catch (e: Exception) {
                places = emptyList()
                hasSearched = true
                searchError = "Could not search places. Check your connection."
                dropdownExpanded = true
            }
            isSearching = false
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { newText ->
                query = newText
                onValueChange(newText)
                // User edited manually -> clear previous selection context.
                runSearch(newText)
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SacredGold) },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = SacredGold
                    )
                }
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
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        // Collapse dropdown shortly after focus loss so taps register first.
                        scope.launch {
                            delay(150)
                            dropdownExpanded = false
                        }
                    } else if (places.isNotEmpty() || searchError != null) {
                        dropdownExpanded = true
                    }
                }
        )

        if (dropdownExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A233D)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                when {
                    isSearching -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = SacredGold
                            )
                            Text("Searching places...", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    searchError != null -> {
                        Text(
                            text = searchError ?: "Search failed.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    places.isEmpty() && hasSearched -> {
                        Text(
                            text = "No places found. Try a different spelling.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    places.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 220.dp)
                        ) {
                            items(places, key = { it.displayName + it.lat.toString() }) { place ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            query = place.displayName
                                            onValueChange(place.displayName)
                                            onPlaceSelected(place)
                                            dropdownExpanded = false
                                            places = emptyList()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = SacredGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = place.displayName,
                                            color = StarlightWhite,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = String.format("%.4f, %.4f", place.lat, place.lon),
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    color = TextSecondary.copy(alpha = 0.15f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
