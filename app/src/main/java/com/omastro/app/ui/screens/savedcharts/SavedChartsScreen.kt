package com.omastro.app.ui.screens.savedcharts

import com.omastro.app.ui.i18n.W
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.remote.SavedChart
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedChartsScreen(
    onBack: () -> Unit,
    onChartSelected: (SavedChart) -> Unit,
    viewModel: SavedChartsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var chartToDelete by remember { mutableStateOf<SavedChart?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(W.t("Saved Charts"), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SacredGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.charts.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SacredGold)
                    }
                }
                state.error != null && state.charts.isEmpty() -> {
                    ErrorState(
                        message = state.error ?: "Could not load saved charts.",
                        onRetry = { viewModel.clearError(); viewModel.refresh() }
                    )
                }
                state.charts.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.charts, key = { it.id }) { chart ->
                            SavedChartCard(
                                chart = chart,
                                isDeleting = chart.id in state.deleteInProgress,
                                onClick = { onChartSelected(chart) },
                                onDelete = { chartToDelete = chart }
                            )
                        }
                    }
                }
            }

            // Inline error snackbar when list is non-empty
            state.error?.let { msg ->
                if (state.charts.isNotEmpty()) {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text(W.t("Dismiss"), color = SacredGold)
                            }
                        }
                    ) {
                        Text(msg)
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    chartToDelete?.let { chart ->
        AlertDialog(
            onDismissRequest = { chartToDelete = null },
            title = { Text("Delete chart?") },
            text = { Text("\"${chart.name}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChart(chart)
                        chartToDelete = null
                    }
                ) {
                    Text(W.t("Delete"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { chartToDelete = null }) {
                    Text(W.t("Cancel"))
                }
            }
        )
    }
}

@Composable
private fun SavedChartCard(
    chart: SavedChart,
    isDeleting: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = SacredGold,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chart.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                val details = buildString {
                    if (chart.birthDate.isNotBlank()) append(chart.birthDate)
                    if (chart.birthTime.isNotBlank()) {
                        if (isNotEmpty()) append(" at ")
                        append(chart.birthTime)
                    }
                    if (chart.birthPlace.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(chart.birthPlace)
                    }
                }
                if (details.isNotEmpty()) {
                    Text(details, fontSize = 13.sp, color = TextSecondary)
                }
                chart.createdAt?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        "Saved ${it.take(10)}",
                        fontSize = 12.sp,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
            if (isDeleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = SacredGold,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete chart",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Bookmarks,
            contentDescription = null,
            tint = SacredGold.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No saved charts yet",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Generate a kundali and tap the save icon to keep it here for quick access.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
        ) {
            Text(W.t("Retry"), color = CosmicDeepBlue)
        }
    }
}
