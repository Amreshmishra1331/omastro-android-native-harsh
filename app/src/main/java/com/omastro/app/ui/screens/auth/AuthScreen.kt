package com.omastro.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*

/**
 * Authentication entry point.
 *
 * Four modes: LOGIN (email + password), SIGNUP (name/email/mobile/password),
 * VERIFY_SIGNUP_OTP (6-digit emailed code), FORGOT_PASSWORD.
 * All server errors are shown inline; no success is ever fabricated.
 */
@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val mode by viewModel.mode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()
    val loginSuccess by viewModel.loginSuccess.collectAsState()

    LaunchedEffect(loginSuccess) {
        if (loginSuccess) onLoginSuccess()
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
        Text(
            text = "OmAstro AI",
            style = MaterialTheme.typography.headlineLarge,
            color = SacredGold
        )
        Text(
            text = "Vedic Astrology Guidance",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        CosmicCard(borderGold = true, modifier = Modifier.fillMaxWidth()) {
            when (mode) {
                AuthMode.LOGIN -> LoginForm(viewModel, isLoading)
                AuthMode.SIGNUP -> SignupForm(viewModel, isLoading)
                AuthMode.VERIFY_SIGNUP_OTP -> VerifyOtpForm(viewModel, isLoading)
                AuthMode.FORGOT_PASSWORD -> ForgotPasswordForm(viewModel, isLoading)
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = msg,
                    color = InauspiciousRed,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            infoMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = msg,
                    color = AuspiciousGreen,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            AuthModeFooter(viewModel, mode)
        }
    }
}

@Composable
private fun AuthModeFooter(viewModel: AuthViewModel, mode: AuthMode) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        when (mode) {
            AuthMode.LOGIN -> {
                TextButton(onClick = { viewModel.switchMode(AuthMode.FORGOT_PASSWORD) }) {
                    Text("Forgot password?", color = SacredGoldLight, fontSize = 13.sp)
                }
                TextButton(onClick = { viewModel.switchMode(AuthMode.SIGNUP) }) {
                    Text("New here? Create an account", color = SacredGoldLight, fontSize = 13.sp)
                }
            }
            AuthMode.SIGNUP -> {
                TextButton(onClick = { viewModel.switchMode(AuthMode.LOGIN) }) {
                    Text("Already registered? Sign in", color = SacredGoldLight, fontSize = 13.sp)
                }
            }
            AuthMode.VERIFY_SIGNUP_OTP -> {
                TextButton(onClick = { viewModel.switchMode(AuthMode.LOGIN) }) {
                    Text("Back to sign in", color = SacredGoldLight, fontSize = 13.sp)
                }
            }
            AuthMode.FORGOT_PASSWORD -> {
                TextButton(onClick = { viewModel.switchMode(AuthMode.LOGIN) }) {
                    Text("Back to sign in", color = SacredGoldLight, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    singleLine: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "Hide" else "Show", fontSize = 12.sp, color = SacredGoldLight)
                }
            }
        } else null,
        singleLine = singleLine,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SacredGold,
            unfocusedBorderColor = CosmicBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = SacredGoldLight,
            unfocusedLabelColor = TextSecondary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AuthPrimaryButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = CosmicBackground, modifier = Modifier.size(22.dp))
        } else {
            Text(text, color = CosmicBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LoginForm(viewModel: AuthViewModel, isLoading: Boolean) {
    val email by viewModel.loginEmail.collectAsState()
    val password by viewModel.loginPassword.collectAsState()

    Text("Welcome Back", style = MaterialTheme.typography.titleLarge, color = SacredGoldLight, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
    Text("Sign in with your email and password.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    Spacer(modifier = Modifier.height(20.dp))

    AuthTextField(email, viewModel::setLoginEmail, "Email", "you@example.com", KeyboardType.Email)
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(password, viewModel::setLoginPassword, "Password", keyboardType = KeyboardType.Password, isPassword = true)
    Spacer(modifier = Modifier.height(20.dp))
    AuthPrimaryButton("Sign In", isLoading, viewModel::login)
}

@Composable
private fun SignupForm(viewModel: AuthViewModel, isLoading: Boolean) {
    val name by viewModel.signupName.collectAsState()
    val email by viewModel.signupEmail.collectAsState()
    val mobile by viewModel.signupMobile.collectAsState()
    val password by viewModel.signupPassword.collectAsState()
    val confirm by viewModel.signupConfirmPassword.collectAsState()

    Text("Create Account", style = MaterialTheme.typography.titleLarge, color = SacredGoldLight, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
    Text("A verification code will be sent to your email.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    Spacer(modifier = Modifier.height(20.dp))

    AuthTextField(name, viewModel::setSignupName, "Full Name", "Your name")
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(email, viewModel::setSignupEmail, "Email", "you@example.com", KeyboardType.Email)
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(mobile, viewModel::setSignupMobile, "Mobile Number (optional)", "10-digit mobile number", KeyboardType.Number)
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(password, viewModel::setSignupPassword, "Password (min 8 characters)", keyboardType = KeyboardType.Password, isPassword = true)
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(confirm, viewModel::setSignupConfirmPassword, "Confirm Password", keyboardType = KeyboardType.Password, isPassword = true)
    Spacer(modifier = Modifier.height(20.dp))
    AuthPrimaryButton("Create Account", isLoading, viewModel::register)
}

@Composable
private fun VerifyOtpForm(viewModel: AuthViewModel, isLoading: Boolean) {
    val email by viewModel.otpEmail.collectAsState()
    val code by viewModel.otpCode.collectAsState()
    val cooldown by viewModel.resendCooldown.collectAsState()

    Text("Verify Your Email", style = MaterialTheme.typography.titleLarge, color = SacredGoldLight, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
    Text("Enter the 6-digit code sent to $email.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    Spacer(modifier = Modifier.height(20.dp))

    AuthTextField(code, viewModel::setOtpCode, "Verification Code", "123456", KeyboardType.Number)
    Spacer(modifier = Modifier.height(20.dp))
    AuthPrimaryButton("Verify & Continue", isLoading, viewModel::verifySignupOtp)
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(
        onClick = { viewModel.resendSignupOtp() },
        enabled = !isLoading && cooldown == 0,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
        Text(
            if (cooldown > 0) "Resend code in ${cooldown}s" else "Resend Code",
            color = SacredGoldLight, fontSize = 13.sp
        )
    }
}

@Composable
private fun ForgotPasswordForm(viewModel: AuthViewModel, isLoading: Boolean) {
    val email by viewModel.forgotEmail.collectAsState()

    Text("Reset Password", style = MaterialTheme.typography.titleLarge, color = SacredGoldLight, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
    Text("Enter your account email to receive reset instructions.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    Spacer(modifier = Modifier.height(20.dp))

    AuthTextField(email, viewModel::setForgotEmail, "Email", "you@example.com", KeyboardType.Email)
    Spacer(modifier = Modifier.height(20.dp))
    AuthPrimaryButton("Send Reset Instructions", isLoading, viewModel::forgotPassword)
}
