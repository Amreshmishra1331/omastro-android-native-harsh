package com.omastro.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val authConfig by viewModel.authConfig.collectAsState()
    val destination by viewModel.destination.collectAsState()
    val otp by viewModel.otp.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val otpSent by viewModel.otpSent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val loginSuccess by viewModel.loginSuccess.collectAsState()

    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Om Sacred Emblem
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SacredGold.copy(alpha = 0.15f))
                .border(2.dp, SacredGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ॐ",
                fontSize = 36.sp,
                color = SacredGold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "OmAstro AI",
            style = MaterialTheme.typography.headlineLarge,
            color = SacredGold
        )
        Text(
            text = "Ancient Vedic Wisdom • Quantum Precision",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        CosmicCard(borderGold = true) {
            Text(
                text = if (otpSent) "Verify OTP Code" else "Instant Cosmic Sign In",
                style = MaterialTheme.typography.titleLarge,
                color = SacredGoldLight
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (otpSent)
                    "Enter the ${authConfig.otpLength}-digit code sent to $destination"
                else
                    "Dynamic login powered by OmAstro Secure Vault",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (!otpSent) {
                // Dynamic Channel Selector based on server config
                if (authConfig.loginMethod == "both" || authConfig.phoneOtpChannels.size > 1) {
                    Text(
                        text = "Preferred Verification Channel:",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        authConfig.phoneOtpChannels.forEach { channel ->
                            val isSelected = selectedChannel == channel
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setChannel(channel) },
                                color = if (isSelected) SacredGold else CosmicSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) SacredGold else CosmicBorder
                                )
                            ) {
                                Text(
                                    text = if (channel == "whatsapp") "💬 WhatsApp" else "📱 SMS",
                                    fontSize = 13.sp,
                                    color = if (isSelected) CosmicBackground else TextPrimary,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Destination Input
                OutlinedTextField(
                    value = destination,
                    onValueChange = { viewModel.setDestination(it) },
                    label = { Text("Mobile Number or Email") },
                    placeholder = { Text("+91 9876543210 or user@omastro.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SacredGold,
                        unfocusedBorderColor = CosmicBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.requestOtp() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = CosmicBackground, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Send Divine OTP", color = CosmicBackground, style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                // OTP Input Field
                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= authConfig.otpLength) viewModel.setOtp(it) },
                    label = { Text("Verification Code") },
                    placeholder = { Text("123456") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SacredGold,
                        unfocusedBorderColor = CosmicBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.verifyOtp() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = CosmicBackground, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Verify & Continue", color = CosmicBackground, style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { viewModel.requestOtp() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Resend OTP", color = SacredGoldLight, fontSize = 13.sp)
                }
            }

            statusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = msg,
                    color = if (msg.contains("Success", true) || msg.contains("sent", true)) AuspiciousGreen else InauspiciousRed,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
