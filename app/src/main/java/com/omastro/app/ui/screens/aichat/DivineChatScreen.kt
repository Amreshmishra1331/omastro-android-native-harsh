package com.omastro.app.ui.screens.aichat

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Divine Chat — dedicated section matching the website's /divine-chat/ page.
 *
 * Uses the same AI chat engine as [AiChatScreen] but locked to the Divine
 * persona (no mode switcher). Conversations are kept separate per mode by
 * [AiChatViewModel], so Divine chats don't mix with General/Mundane chats.
 * The ViewModel is activity-scoped (same instance as [AiChatScreen]) so the
 * conversation survives navigation.
 */
@Composable
fun DivineChatScreen(
    onBack: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    viewModel: AiChatViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
) {
    AiChatScreen(
        onBack = onBack,
        onMenuClick = onMenuClick,
        viewModel = viewModel,
        lockedMode = AiChatMode.DIVINE
    )
}
