package com.omastro.app.ui.screens.notifications

import com.omastro.app.ui.i18n.W
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.AppNotification
import com.omastro.app.di.AppContainer
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {
    private val repo = AppContainer.notificationRepository

    val notifications = repo.notifications
    val unreadCount = repo.unreadCount
    val isLoading = repo.isLoading
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            repo.refresh().onFailure { e -> _error.value = e.message }
        }
    }

    fun markRead(n: AppNotification) {
        if (n.isRead) return
        viewModelScope.launch {
            repo.markRead(listOf(n.id)).onFailure { e -> _error.value = e.message }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repo.markRead(null).onFailure { e -> _error.value = e.message }
        }
    }

    fun clearAll(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.clearAll()
                .onSuccess { onDone() }
                .onFailure { e -> _error.value = e.message }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(W.t("Notifications"), color = StarlightWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        IconButton(onClick = { viewModel.markAllRead() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Mark all read", tint = SacredGold)
                        }
                    }
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all", tint = SacredGold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        when {
            isLoading && notifications.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SacredGold)
                }
            }
            notifications.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = SacredGold.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(W.t("No notifications yet"), color = StarlightWhite.copy(alpha = 0.6f), fontSize = 15.sp)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(Modifier.height(6.dp)) }
                    if (error != null) {
                        item {
                            Text(error ?: "", color = Color(0xFFFF8A80), fontSize = 13.sp)
                        }
                    }
                    items(notifications, key = { it.id }) { n ->
                        NotificationRow(notification = n, onClick = { viewModel.markRead(n) })
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(W.t("notif_clear_title"), color = StarlightWhite) },
            text = { Text(W.t("notif_clear_body"), color = StarlightWhite.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearAll()
                }) { Text(W.t("Clear All"), color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(W.t("Cancel"), color = SacredGold) }
            },
            containerColor = CosmicCharcoal
        )
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    CosmicCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        borderGold = !notification.isRead
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                if (notification.isRead) Icons.Default.NotificationsNone else Icons.Default.Notifications,
                contentDescription = null,
                tint = if (notification.isRead) StarlightWhite.copy(alpha = 0.4f) else SacredGold,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = notification.title.ifBlank { W.t("notif_fallback") },
                    color = StarlightWhite,
                    fontSize = 14.sp,
                    fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold
                )
                if (notification.message.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = notification.message,
                        color = StarlightWhite.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
                if (notification.time.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = notification.time,
                        color = StarlightWhite.copy(alpha = 0.45f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
