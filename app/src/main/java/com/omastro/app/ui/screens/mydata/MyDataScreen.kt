package com.omastro.app.ui.screens.mydata

import android.app.Application
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.AlertDialog
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
import com.omastro.app.data.local.MyDataFeatureType
import com.omastro.app.data.repository.MyDataRepository
import com.omastro.app.data.repository.ReadingHistoryRepository
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.navigation.PendingLocalHolder
import com.omastro.app.ui.theme.CosmicBorder
import com.omastro.app.ui.theme.CosmicCharcoal
import com.omastro.app.ui.theme.CosmicDeepBlue
import com.omastro.app.ui.theme.CosmicNavy
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.SacredGoldLight
import com.omastro.app.ui.theme.StarlightWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

// ------------------------------------------------------------------
// ViewModel
// ------------------------------------------------------------------

/** One row in the unified My Data list (all types, sorted by recency). */
data class MyDataItem(
    /** One of [MyDataFeatureType] constants. */
    val kind: String,
    val id: String,
    val createdAt: Long,
    val label: String,
    val subLabel: String?,
    val thumbnailFile: String?
)

data class MyDataUiState(
    val isLoading: Boolean = true,
    val items: List<MyDataItem> = emptyList(),
    /** null = all types */
    val filter: String? = null,
    val query: String = "",
    val storageBytes: Long = 0L
)

class MyDataViewModel(application: Application) : AndroidViewModel(application) {
    private val myData = MyDataRepository(application)
    private val readings = ReadingHistoryRepository(application)
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(MyDataUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<MyDataUiState> = _uiState

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val items = loadAll()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                items = applyFilter(items, _uiState.value.filter, _uiState.value.query),
                storageBytes = myData.storageUsedBytes()
            )
        }
    }

    fun setFilter(kind: String?) {
        _uiState.value = _uiState.value.copy(filter = kind)
        refresh()
    }

    fun setQuery(q: String) {
        _uiState.value = _uiState.value.copy(query = q)
        refresh()
    }

    fun deleteItem(item: MyDataItem, onDone: () -> Unit) {
        viewModelScope.launch {
            when (item.kind) {
                MyDataFeatureType.KUNDALI -> myData.deleteKundali(item.id)
                MyDataFeatureType.NUMEROLOGY -> myData.deleteNumerology(item.id)
                else -> readings.delete(item.id)
            }
            refresh()
            onDone()
        }
    }

    fun renameItem(item: MyDataItem, label: String, onDone: () -> Unit) {
        viewModelScope.launch {
            when (item.kind) {
                MyDataFeatureType.KUNDALI -> myData.renameKundali(item.id, label)
                MyDataFeatureType.NUMEROLOGY -> myData.renameNumerology(item.id, label)
                else -> readings.rename(item.id, label)
            }
            refresh()
            onDone()
        }
    }

    fun clearAll(onDone: () -> Unit) {
        viewModelScope.launch {
            myData.clearAll()
            refresh()
            onDone()
        }
    }

    /** Builds the instant-reopen payload for a saved numerology result. */
    suspend fun storedNumerologyPayload(id: String): PendingLocalHolder.StoredNumerology? {
        val saved = myData.getNumerology(id) ?: return null
        return PendingLocalHolder.StoredNumerology(
            toolId = saved.toolId,
            inputs = saved.inputs(),
            result = saved.result()
        )
    }

    fun thumbnailFile(name: String?) = readings.thumbnailFile(name)

    private suspend fun loadAll(): List<MyDataItem> {
        val kundalis = myData.listKundalis().map {
            MyDataItem(
                kind = MyDataFeatureType.KUNDALI,
                id = it.id,
                createdAt = it.createdAt,
                label = it.label,
                subLabel = "${it.dob} · ${it.pob}",
                thumbnailFile = null
            )
        }
        val numerologies = myData.listNumerology().map {
            MyDataItem(
                kind = MyDataFeatureType.NUMEROLOGY,
                id = it.id,
                createdAt = it.createdAt,
                label = it.label,
                subLabel = it.toolName,
                thumbnailFile = null
            )
        }
        val vision = readings.list().map {
            MyDataItem(
                kind = it.featureType,
                id = it.id,
                createdAt = it.createdAt,
                label = it.label,
                subLabel = null,
                thumbnailFile = it.thumbnailFile
            )
        }
        return (kundalis + numerologies + vision).sortedByDescending { it.createdAt }
    }

    private fun applyFilter(
        items: List<MyDataItem>,
        filter: String?,
        query: String
    ): List<MyDataItem> {
        val q = query.trim().lowercase(Locale.getDefault())
        return items.filter { item ->
            (filter == null || item.kind == filter) &&
                (q.isEmpty() || item.label.lowercase(Locale.getDefault()).contains(q) ||
                    (item.subLabel?.lowercase(Locale.getDefault())?.contains(q) == true))
        }
    }
}

// ------------------------------------------------------------------
// Screen
// ------------------------------------------------------------------

private fun itemIcon(kind: String): ImageVector = when (kind) {
    MyDataFeatureType.KUNDALI -> Icons.Default.AutoAwesome
    MyDataFeatureType.NUMEROLOGY -> Icons.Default.Calculate
    MyDataFeatureType.VASTU -> Icons.Default.HomeWork
    MyDataFeatureType.PALMISTRY -> Icons.Default.PanTool
    MyDataFeatureType.FACE -> Icons.Default.Face
    else -> Icons.Default.History
}

private fun itemKindName(kind: String): String = when (kind) {
    MyDataFeatureType.KUNDALI -> W.t("mydata_kundali")
    MyDataFeatureType.NUMEROLOGY -> W.t("mydata_numerology")
    MyDataFeatureType.VASTU -> W.t("vastu_title")
    MyDataFeatureType.PALMISTRY -> W.t("palm_title")
    MyDataFeatureType.FACE -> W.t("face_title")
    else -> kind
}

private fun formatMyDataDate(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
        .format(Date(epochMillis))

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyDataScreen(
    onBack: () -> Unit,
    onOpenKundali: (String) -> Unit,
    onOpenNumerology: (PendingLocalHolder.StoredNumerology) -> Unit,
    onOpenVisionReading: (String) -> Unit,
    viewModel: MyDataViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // Collect global language so the whole screen auto-switches EN/हिन्दी.
    val lang by LangPrefs.language.collectAsState()

    var pendingDelete by remember { mutableStateOf<MyDataItem?>(null) }
    var pendingRename by remember { mutableStateOf<MyDataItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var confirmClearAll by remember { mutableStateOf(false) }

    LaunchedEffect(lang) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            W.t("mydata_title"),
                            color = StarlightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            W.t("mydata_sub"),
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
            // Search (by name / label)
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.setQuery(it) },
                placeholder = { Text(W.t("mydata_search"), color = StarlightWhite.copy(alpha = 0.4f)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = SacredGold)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SacredGold,
                    unfocusedBorderColor = CosmicBorder,
                    focusedTextColor = StarlightWhite,
                    unfocusedTextColor = StarlightWhite,
                    cursorColor = SacredGold
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Type filter chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    MyDataFilterChip(
                        selected = uiState.filter == null,
                        label = W.t("mydata_all"),
                        onClick = { viewModel.setFilter(null) }
                    )
                }
                item {
                    MyDataFilterChip(
                        selected = uiState.filter == MyDataFeatureType.KUNDALI,
                        label = W.t("mydata_kundali"),
                        onClick = { viewModel.setFilter(MyDataFeatureType.KUNDALI) }
                    )
                }
                item {
                    MyDataFilterChip(
                        selected = uiState.filter == MyDataFeatureType.NUMEROLOGY,
                        label = W.t("mydata_numerology"),
                        onClick = { viewModel.setFilter(MyDataFeatureType.NUMEROLOGY) }
                    )
                }
                item {
                    MyDataFilterChip(
                        selected = uiState.filter == MyDataFeatureType.VASTU,
                        label = W.t("vastu_title"),
                        onClick = { viewModel.setFilter(MyDataFeatureType.VASTU) }
                    )
                }
                item {
                    MyDataFilterChip(
                        selected = uiState.filter == MyDataFeatureType.PALMISTRY,
                        label = W.t("palm_title"),
                        onClick = { viewModel.setFilter(MyDataFeatureType.PALMISTRY) }
                    )
                }
                item {
                    MyDataFilterChip(
                        selected = uiState.filter == MyDataFeatureType.FACE,
                        label = W.t("face_title"),
                        onClick = { viewModel.setFilter(MyDataFeatureType.FACE) }
                    )
                }
            }

            // Storage management row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CosmicNavy)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    Icons.Default.SdStorage,
                    contentDescription = null,
                    tint = SacredGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = W.tf("mydata_storage_used", formatBytes(uiState.storageBytes)),
                    color = StarlightWhite.copy(alpha = 0.8f),
                    fontSize = 12.5.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { confirmClearAll = true }) {
                    Text(W.t("mydata_clear_all"), color = SacredGoldLight, fontSize = 13.sp)
                }
            }

            if (!uiState.isLoading && uiState.items.isEmpty()) {
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
                            Icons.Default.SdStorage,
                            contentDescription = null,
                            tint = SacredGold.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            W.t("mydata_empty"),
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
                    items(uiState.items, key = { "${it.kind}:${it.id}" }) { item ->
                        MyDataRow(
                            item = item,
                            viewModel = viewModel,
                            onOpen = {
                                when (item.kind) {
                                    MyDataFeatureType.KUNDALI -> onOpenKundali(item.id)
                                    MyDataFeatureType.NUMEROLOGY -> scope.launch {
                                        viewModel.storedNumerologyPayload(item.id)
                                            ?.let { onOpenNumerology(it) }
                                    }
                                    else -> onOpenVisionReading(item.id)
                                }
                            },
                            onDelete = { pendingDelete = item },
                            onRename = {
                                pendingRename = item
                                renameText = item.label
                            }
                        )
                    }
                }
            }
        }
    }

    // ---- Delete confirmation (also opened via long-press) ----
    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(W.t("mydata_delete_title"), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    W.tf("mydata_delete_msg", item.label),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(item) {
                        pendingDelete = null
                        scope.launch { snackbarHostState.showSnackbar(W.t("mydata_deleted")) }
                    }
                }) {
                    Text(W.t("mydata_delete"), color = SacredGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(W.t("mydata_cancel"))
                }
            }
        )
    }

    // ---- Rename ----
    pendingRename?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingRename = null },
            title = { Text(W.t("mydata_rename"), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(W.t("history_label_hint")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameItem(item, renameText) {
                        pendingRename = null
                        scope.launch { snackbarHostState.showSnackbar(W.t("mydata_renamed")) }
                    }
                }) {
                    Text(W.t("history_save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRename = null }) {
                    Text(W.t("history_cancel"))
                }
            }
        )
    }

    // ---- Clear all (confirmed) ----
    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text(W.t("mydata_clear_all_title"), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    W.t("mydata_clear_all_msg"),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll {
                        confirmClearAll = false
                        scope.launch { snackbarHostState.showSnackbar(W.t("mydata_cleared")) }
                    }
                }) {
                    Text(W.t("mydata_clear_all"), color = SacredGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) {
                    Text(W.t("mydata_cancel"))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MyDataRow(
    item: MyDataItem,
    viewModel: MyDataViewModel,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicCharcoal)
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onDelete
            )
            .padding(12.dp)
    ) {
        // Thumbnail (vision readings) or type icon
        val thumb = viewModel.thumbnailFile(item.thumbnailFile)
        if (thumb != null) {
            AsyncImage(
                model = thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SacredGold.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    itemIcon(item.kind),
                    contentDescription = itemKindName(item.kind),
                    tint = SacredGold,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                itemKindName(item.kind),
                color = SacredGold,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                item.label,
                color = StarlightWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.subLabel != null) {
                    Text(
                        item.subLabel,
                        color = StarlightWhite.copy(alpha = 0.6f),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        " · ",
                        color = StarlightWhite.copy(alpha = 0.4f),
                        fontSize = 11.5.sp
                    )
                }
                Text(
                    formatMyDataDate(item.createdAt),
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onRename) {
            Icon(
                Icons.Default.Edit,
                contentDescription = W.t("mydata_rename_cd"),
                tint = SacredGold.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = W.t("mydata_delete_cd"),
                tint = StarlightWhite.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = SacredGold,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun MyDataFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SacredGold,
            selectedLabelColor = CosmicDeepBlue,
            containerColor = CosmicCharcoal,
            labelColor = StarlightWhite
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = CosmicBorder,
            selectedBorderColor = SacredGold
        )
    )
}
