package com.omastro.app.ui.screens.history

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.data.local.ReadingFeatureType
import com.omastro.app.data.repository.ReadingHistoryRepository
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.theme.CosmicCharcoal
import com.omastro.app.ui.theme.CosmicDeepBlue
import com.omastro.app.ui.theme.CosmicNavy
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

// ------------------------------------------------------------------
// ViewModel
// ------------------------------------------------------------------

data class ReadingHistoryUiState(
    val isLoading: Boolean = true,
    val readings: List<ReadingHistoryRepository.SavedReading> = emptyList(),
    /** null = all types */
    val filter: String? = null
)

class ReadingHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ReadingHistoryRepository(application)
    private val _uiState =
        kotlinx.coroutines.flow.MutableStateFlow(ReadingHistoryUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<ReadingHistoryUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val list = repo.list(_uiState.value.filter)
            _uiState.value = _uiState.value.copy(isLoading = false, readings = list)
        }
    }

    fun setFilter(type: String?) {
        _uiState.value = _uiState.value.copy(filter = type)
        refresh()
    }

    fun rename(id: String, label: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.rename(id, label)
            refresh()
            onDone()
        }
    }

    fun delete(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.delete(id)
            refresh()
            onDone()
        }
    }

    fun thumbnailFile(name: String?) = repo.thumbnailFile(name)
}

// ------------------------------------------------------------------
// Screen
// ------------------------------------------------------------------

private fun featureIcon(type: String): ImageVector = when (type) {
    ReadingFeatureType.VASTU -> Icons.Default.HomeWork
    ReadingFeatureType.PALMISTRY -> Icons.Default.PanTool
    ReadingFeatureType.FACE -> Icons.Default.Face
    else -> Icons.Default.History
}

private fun featureName(type: String): String = when (type) {
    ReadingFeatureType.VASTU -> W.t("vastu_title")
    ReadingFeatureType.PALMISTRY -> W.t("palm_title")
    ReadingFeatureType.FACE -> W.t("face_title")
    else -> type
}

fun formatHistoryDate(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
        .format(Date(epochMillis))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingHistoryScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    viewModel: ReadingHistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // Collect global language so the whole screen auto-switches EN/हिन्दी.
    val lang by LangPrefs.language.collectAsState()

    var pendingDelete by remember { mutableStateOf<ReadingHistoryRepository.SavedReading?>(null) }
    var pendingRename by remember { mutableStateOf<ReadingHistoryRepository.SavedReading?>(null) }

    LaunchedEffect(lang) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            W.t("history_title"),
                            color = StarlightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            W.t("history_sub"),
                            color = StarlightWhite.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = W.t("back"),
                            tint = SacredGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CosmicDeepBlue
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Type filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryFilterChip(
                    selected = uiState.filter == null,
                    label = W.t("history_all"),
                    onClick = { viewModel.setFilter(null) }
                )
                HistoryFilterChip(
                    selected = uiState.filter == ReadingFeatureType.VASTU,
                    label = W.t("vastu_title"),
                    onClick = { viewModel.setFilter(ReadingFeatureType.VASTU) }
                )
                HistoryFilterChip(
                    selected = uiState.filter == ReadingFeatureType.PALMISTRY,
                    label = W.t("palm_title"),
                    onClick = { viewModel.setFilter(ReadingFeatureType.PALMISTRY) }
                )
                HistoryFilterChip(
                    selected = uiState.filter == ReadingFeatureType.FACE,
                    label = W.t("face_title"),
                    onClick = { viewModel.setFilter(ReadingFeatureType.FACE) }
                )
            }

            if (!uiState.isLoading && uiState.readings.isEmpty()) {
                // Empty state — honest, no fabricated content.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicNavy)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = SacredGold.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            W.t("history_empty"),
                            color = StarlightWhite.copy(alpha = 0.7f),
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.readings, key = { it.id }) { reading ->
                        ReadingHistoryCard(
                            reading = reading,
                            thumbnail = viewModel.thumbnailFile(reading.thumbnailFile),
                            onOpen = { onOpenDetail(reading.id) },
                            onLongPress = { pendingDelete = reading },
                            onRename = { pendingRename = reading }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation (long-press target)
    pendingDelete?.let { reading ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(
                    W.t("history_delete_title"),
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "${reading.label}\n${W.t("history_delete_msg")}",
                    color = StarlightWhite.copy(alpha = 0.75f),
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDelete = null
                        viewModel.delete(reading.id) {
                            scope.launch {
                                snackbarHostState.showSnackbar(W.t("history_deleted"))
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB3261E),
                        contentColor = Color.White
                    )
                ) { Text(W.t("history_delete")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(W.t("history_cancel"), color = SacredGold)
                }
            },
            containerColor = CosmicNavy
        )
    }

    // Rename dialog
    pendingRename?.let { reading ->
        var label by remember(reading.id) { mutableStateOf(reading.label) }
        AlertDialog(
            onDismissRequest = { pendingRename = null },
            title = {
                Text(
                    W.t("history_rename"),
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(W.t("history_label_hint")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StarlightWhite,
                        unfocusedTextColor = StarlightWhite,
                        focusedBorderColor = SacredGold,
                        unfocusedBorderColor = StarlightWhite.copy(alpha = 0.3f),
                        focusedLabelColor = SacredGold,
                        unfocusedLabelColor = StarlightWhite.copy(alpha = 0.55f),
                        cursorColor = SacredGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRename = null
                        viewModel.rename(reading.id, label) {
                            scope.launch {
                                snackbarHostState.showSnackbar(W.t("history_renamed"))
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SacredGold,
                        contentColor = Color(0xFF1A1005)
                    )
                ) { Text(W.t("history_save"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRename = null }) {
                    Text(W.t("history_cancel"), color = SacredGold)
                }
            },
            containerColor = CosmicNavy
        )
    }
}

@Composable
private fun HistoryFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SacredGold.copy(alpha = 0.25f),
            selectedLabelColor = SacredGold,
            containerColor = CosmicNavy,
            labelColor = StarlightWhite.copy(alpha = 0.7f)
        ),
        border = BorderStroke(
            1.dp,
            if (selected) SacredGold.copy(alpha = 0.5f)
            else StarlightWhite.copy(alpha = 0.15f)
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadingHistoryCard(
    reading: ReadingHistoryRepository.SavedReading,
    thumbnail: java.io.File?,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onRename: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail (downscaled analyzed photo) or a feature-icon fallback.
        if (thumbnail != null) {
            AsyncImage(
                model = thumbnail,
                contentDescription = W.t("history_open_cd"),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CosmicCharcoal)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SacredGold.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    featureIcon(reading.featureType),
                    contentDescription = W.t("history_no_thumb_cd"),
                    tint = SacredGold,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                reading.label,
                color = StarlightWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    featureIcon(reading.featureType),
                    contentDescription = null,
                    tint = SacredGold.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "  ${featureName(reading.featureType)}  ·  ${formatHistoryDate(reading.createdAt)}",
                    color = StarlightWhite.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onRename) {
            Icon(
                Icons.Default.Edit,
                contentDescription = W.t("history_rename_cd"),
                tint = StarlightWhite.copy(alpha = 0.6f)
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = StarlightWhite.copy(alpha = 0.4f)
        )
    }
}
