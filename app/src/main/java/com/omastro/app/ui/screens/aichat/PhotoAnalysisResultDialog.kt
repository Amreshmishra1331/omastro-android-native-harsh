package com.omastro.app.ui.screens.aichat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omastro.app.data.model.VisionAnalysis
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.theme.CosmicCharcoal
import com.omastro.app.ui.theme.CosmicNavy
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite

/**
 * Shows the AI vision analysis result with actions:
 * - Attach to chat: parsed_facts become chat context for follow-ups
 * - Ask now: attach + prefill a follow-up question in the input
 */
@Composable
fun PhotoAnalysisResultDialog(
    analysis: VisionAnalysis,
    fileName: String,
    onDismiss: () -> Unit,
    onAttachToChat: () -> Unit,
    onAskNow: () -> Unit
) {
    // Collect global language so the dialog auto-switches EN/हिन्दी.
    val lang by LangPrefs.language.collectAsState()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CosmicNavy)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    W.t("result_title"),
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    fileName,
                    color = StarlightWhite.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicCharcoal)
                        .padding(14.dp)
                ) {
                    Text(
                        text = analysis.analysis.ifBlank { W.t("result_empty") },
                        color = StarlightWhite.copy(alpha = 0.9f),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    W.t("result_attach_hint"),
                    color = StarlightWhite.copy(alpha = 0.6f),
                    fontSize = 12.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(W.t("chat_close"), color = StarlightWhite.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onAttachToChat) {
                        Text(W.t("result_attach"), color = SacredGold, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAskNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SacredGold,
                            contentColor = Color(0xFF1A1005)
                        )
                    ) {
                        Text(W.t("result_ask"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
