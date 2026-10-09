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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.data.model.SavedChatSession
import com.omastro.app.ui.theme.CosmicCharcoal
import com.omastro.app.ui.theme.CosmicNavy
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite

/**
 * Saved chat sessions picker — website parity for the AI chat history sidebar.
 *
 * Lists saved sessions (title + date), tap to load (restores messages AND
 * context), trash icon to delete. "New Chat" is handled by the caller.
 */
@Composable
fun SavedSessionsDialog(
    sessions: List<SavedChatSession>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSelect: (SavedChatSession) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit = {}
) {
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
                    "Saved Chats",
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = StarlightWhite.copy(alpha = 0.6f)
                    )
                }
            }
        },
        text = {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SacredGold)
                    }
                }
                sessions.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No saved chats yet.\nYour saved conversations will appear here.",
                            color = StarlightWhite.copy(alpha = 0.5f),
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions, key = { it.id }) { session ->
                            SavedSessionRow(
                                session = session,
                                onSelect = { onSelect(session) },
                                onDelete = { onDelete(session.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sessions.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear All", color = Color(0xFFFF8A80), fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = SacredGold, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    )
}

@Composable
private fun SavedSessionRow(
    session: SavedChatSession,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicCharcoal)
            .border(
                1.dp,
                StarlightWhite.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.displayTitle,
                color = StarlightWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val meta = buildList {
                session.mode?.takeIf { it.isNotBlank() }?.let { add(it.replaceFirstChar { c -> c.uppercase() }) }
                session.updatedAt?.takeIf { it.isNotBlank() }?.let { add(it.take(10)) }
                if (session.storedContext != null) add("Has birth data")
            }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    meta,
                    color = StarlightWhite.copy(alpha = 0.5f),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete chat",
                tint = Color(0xFFFF8A80).copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
