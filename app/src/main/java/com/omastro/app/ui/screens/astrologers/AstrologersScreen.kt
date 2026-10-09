package com.omastro.app.ui.screens.astrologers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.omastro.app.data.model.Astrologer
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.components.GradientButton
import com.omastro.app.ui.theme.*

/** Directory sort modes. */
private enum class AstroSort(val label: String) {
    RECOMMENDED("Recommended"),
    RATING("Top rated"),
    PRICE_LOW("Price: low to high"),
    EXPERIENCE("Most experienced")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstrologersScreen(
    onAstrologerClick: (String) -> Unit,
    onMenuClick: (() -> Unit)? = null,
    viewModel: ConsultationViewModel = viewModel()
) {
    val astrologers by viewModel.astrologers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listError by viewModel.listError.collectAsState()
    val autoMatchPhase by viewModel.autoMatchPhase.collectAsState()
    val pendingConsent by viewModel.pendingConsent.collectAsState()
    val sessionError by viewModel.sessionError.collectAsState()
    val activeSlug by viewModel.activeSlug.collectAsState()

    // Directory controls: search / sort / online-only filter.
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(AstroSort.RECOMMENDED) }
    var onlineOnly by remember { mutableStateOf(false) }
    val visibleAstrologers = remember(astrologers, searchQuery, sortMode, onlineOnly) {
        var list = astrologers
        if (onlineOnly) list = list.filter { it.online }
        val q = searchQuery.trim().lowercase()
        if (q.isNotEmpty()) {
            list = list.filter { a ->
                a.name.lowercase().contains(q) ||
                    a.specialisations.any { it.lowercase().contains(q) } ||
                    a.languages.any { it.lowercase().contains(q) }
            }
        }
        when (sortMode) {
            AstroSort.RATING -> list.sortedByDescending { it.rating }
            AstroSort.PRICE_LOW -> list.sortedBy { it.ratePaise }
            AstroSort.EXPERIENCE -> list.sortedByDescending { it.experience }
            AstroSort.RECOMMENDED -> list // server order
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consult Astrologers", color = StarlightWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = SacredGold)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAstrologers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SacredGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        if (isLoading && astrologers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SacredGold)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(6.dp)) }

                // Auto-match entry: "Finding best astrologers…" (website parity).
                item {
                    AutoMatchCard(
                        phase = autoMatchPhase,
                        onStart = { viewModel.startAutoMatch() }
                    )
                }

                // Search + sort + online-only filter.
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search name, expertise, language", color = StarlightWhite.copy(alpha = 0.4f), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SacredGold, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StarlightWhite,
                            unfocusedTextColor = StarlightWhite,
                            focusedBorderColor = SacredGold,
                            unfocusedBorderColor = StarlightWhite.copy(alpha = 0.3f)
                        )
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AstroSort.values()) { mode ->
                            FilterChip(
                                selected = sortMode == mode,
                                onClick = { sortMode = mode },
                                label = { Text(mode.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SacredGold,
                                    selectedLabelColor = CosmicDeepBlue
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = onlineOnly,
                                onClick = { onlineOnly = !onlineOnly },
                                label = { Text("Online only", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SacredGold,
                                    selectedLabelColor = CosmicDeepBlue
                                )
                            )
                        }
                    }
                }

                if (listError != null) {
                    item {
                        Text(
                            text = listError ?: "",
                            color = Color(0xFFFF8A80),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                if (sessionError != null) {
                    item {
                        Text(
                            text = sessionError ?: "",
                            color = Color(0xFFFF8A80),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                items(visibleAstrologers) { astro ->
                    AstrologerListItem(astro = astro, onChatClick = { onAstrologerClick(astro.slug) })
                }

                if (visibleAstrologers.isEmpty() && !isLoading) {
                    item {
                        Text(
                            text = if (astrologers.isEmpty()) "No astrologers available right now. Please try again."
                            else "No astrologers match your search.",
                            color = StarlightWhite.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    // Auto-match result: when the ViewModel picks an astrologer without needing
    // consent, it sets activeSlug — navigate to chat.
    val navigatedSlug = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activeSlug, pendingConsent) {
        val slug = activeSlug
        if (slug != null && pendingConsent == null && navigatedSlug.value != slug) {
            navigatedSlug.value = slug
            onAstrologerClick(slug)
        }
        if (slug == null) navigatedSlug.value = null
    }

    // Rate consent dialog (offline OR rate > ₹5/min — website parity).
    pendingConsent?.let { astro ->
        RateConsentDialog(
            astro = astro,
            onConfirm = { viewModel.confirmConsent() },
            onDismiss = { viewModel.dismissConsent() }
        )
    }
}

@Composable
private fun AutoMatchCard(
    phase: ConsultationViewModel.AutoMatchPhase,
    onStart: () -> Unit
) {
    val searching = phase == ConsultationViewModel.AutoMatchPhase.SEARCHING
    CosmicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !searching) { onStart() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (searching) {
                CircularProgressIndicator(
                    color = SacredGold,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = null,
                    tint = SacredGold,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (searching) "Finding best astrologers…" else "Chat Now",
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = if (searching) "Matching you with an available astrologer"
                    else "Auto-match with an online astrologer (₹5/min)",
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            if (!searching) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SacredGold
                )
            }
        }
    }
}

/**
 * Rate consent dialog (website parity): shown when the astrologer is offline
 * or costs more than ₹5/min. Declining charges nothing.
 */
@Composable
private fun RateConsentDialog(
    astro: Astrologer,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val offline = !astro.online
    val expensive = astro.ratePaise > 500
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Start chat with ${astro.name}?", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                if (offline) {
                    Text(
                        "${astro.name} is currently offline. Start the chat with this astrologer anyway?",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (expensive) {
                    Text(
                        "Rate: ${astro.displayRate} (higher than the expected ₹5/min).",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SacredSaffron
                    )
                } else {
                    Text(
                        "Rate: ${astro.displayRate}.",
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Yes, start", color = SacredGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = StarlightWhite.copy(alpha = 0.7f))
            }
        },
        containerColor = CosmicNavy,
        titleContentColor = StarlightWhite,
        textContentColor = StarlightWhite.copy(alpha = 0.85f)
    )
}

@Composable
private fun AstrologerListItem(
    astro: Astrologer,
    onChatClick: () -> Unit
) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar & Online indicator — real photo from server
            Box(modifier = Modifier.size(64.dp)) {
                if (astro.img.isNotBlank()) {
                    AsyncImage(
                        model = astro.img,
                        contentDescription = astro.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(CosmicNavy)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(CosmicNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = "Astrologer", tint = SacredGold, modifier = Modifier.size(28.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(if (astro.online) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = astro.name,
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                val spec = astro.specialisations.take(2).joinToString(", ")
                if (spec.isNotBlank()) {
                    Text(
                        text = spec,
                        color = MysticPurple,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${astro.experience} yrs exp • ${astro.languages.joinToString(", ")}",
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = SacredGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${astro.rating} (${astro.reviews} reviews)",
                        color = SacredGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = astro.displayRate,
                    color = RadiantAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onChatClick,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Chat",
                        color = CosmicDeepBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
