package com.omastro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.theme.CosmicCharcoal
import com.omastro.app.ui.theme.CosmicDeepBlue
import com.omastro.app.ui.theme.CosmicNavy
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite

/**
 * Shared billing UI for the paid photo-AI endpoints
 * (palmistry / face reading / vastu house-map photo).
 *
 * HTTP 402 `insufficient_balance` responses carry the authoritative
 * server-side `price_paise` and `balance_paise`; this dialog shows them
 * bilingually with a prominent Add Money action. The failed analysis is
 * never retried automatically.
 */

/** Formats paise as ₹ (whole rupees, or 2 decimals when not whole). */
fun formatRs(paise: Int): String =
    if (paise % 100 == 0) "₹${paise / 100}"
    else "₹${"%.2f".format(paise / 100.0)}"

/** Small "Free" / "मुफ़्त" badge for the free (engine, non-AI) analyses. */
@Composable
fun FreeBadge(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF2E7D32).copy(alpha = 0.9f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            W.t("vision_free"),
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Insufficient-balance dialog shown when a paid vision/photo-AI endpoint
 * answers HTTP 402. Shows the server-reported price and current balance,
 * offers a prominent Add Money action, and does NOT retry the analysis.
 */
@Composable
fun InsufficientBalanceDialog(
    pricePaise: Int,
    balancePaise: Int,
    onAddMoney: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CosmicNavy,
        icon = {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = SacredGold,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                W.t("vision_insuf_title"),
                color = StarlightWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicCharcoal)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BalanceLine(
                        label = W.t("vision_price_word"),
                        value = formatRs(pricePaise),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BalanceLine(
                        label = W.t("vision_balance_word"),
                        value = formatRs(balancePaise),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    W.tf("vision_insuf_body", formatRs(pricePaise), formatRs(balancePaise)),
                    color = StarlightWhite.copy(alpha = 0.75f),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAddMoney,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SacredGold,
                    contentColor = Color(0xFF1A1005)
                )
            ) {
                Text(W.t("vision_insuf_add"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    W.t("vision_insuf_later"),
                    color = StarlightWhite.copy(alpha = 0.7f)
                )
            }
        }
    )
}

@Composable
private fun BalanceLine(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            color = StarlightWhite.copy(alpha = 0.55f),
            fontSize = 11.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            color = SacredGold,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}
