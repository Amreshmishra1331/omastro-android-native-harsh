package com.omastro.app.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.OtpChannels
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*

/**
 * Authentication entry point — rich cosmic-gold design matching the OmAstro brand.
 *
 * Four modes with animated transitions:
 *  - LOGIN (email + password)
 *  - SIGNUP (name / email / mobile / password + strength meter) → email OTP
 *  - VERIFY_SIGNUP_OTP (6 individual digit boxes, auto-advance)
 *  - FORGOT_PASSWORD (email → reset instructions)
 *
 * OTP channel: the signup/verify/resend calls carry `otp_channel`
 * ([OtpChannels.DEFAULT], currently "email"). See [OtpChannels] for how to
 * enable mobile/WhatsApp OTP later. All server errors are shown inline;
 * no success is ever fabricated.
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF141834), CosmicBackground)
                )
            )
    ) {
        // Soft gold glow behind the brand header.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SacredGold.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthBrandHeader()

            Spacer(modifier = Modifier.height(28.dp))

            CosmicCard(borderGold = true, modifier = Modifier.fillMaxWidth()) {
                if (mode == AuthMode.LOGIN || mode == AuthMode.SIGNUP) {
                    AuthModeTabs(mode) { viewModel.switchMode(it) }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        (fadeIn(tween(250)) + slideInHorizontally(tween(250)) { if (forward) it / 3 else -it / 3 }) togetherWith
                            (fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { if (forward) -it / 3 else it / 3 })
                    },
                    label = "auth_mode_transition"
                ) { targetMode ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        when (targetMode) {
                            AuthMode.LOGIN -> LoginForm(viewModel, isLoading)
                            AuthMode.SIGNUP -> SignupForm(viewModel, isLoading)
                            AuthMode.VERIFY_SIGNUP_OTP -> VerifyOtpForm(viewModel, isLoading)
                            AuthMode.FORGOT_PASSWORD -> ForgotPasswordForm(viewModel, isLoading)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        AuthModeFooter(viewModel, targetMode)
                    }
                }

                errorMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthMessageBanner(msg, isError = true)
                }

                infoMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthMessageBanner(msg, isError = false)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Secure Vedic astrology guidance",
                color = TextSecondary.copy(alpha = 0.8f),
                fontSize = 12.sp,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
private fun AuthBrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SacredGold.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
                .border(1.5.dp, SacredGold.copy(alpha = 0.65f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ॐ",
                color = SacredGold,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "OmAstro AI",
            style = MaterialTheme.typography.headlineMedium,
            color = SacredGold,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Vedic Astrology Guidance",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun AuthModeTabs(current: AuthMode, onSelect: (AuthMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicSurface)
            .padding(4.dp)
    ) {
        listOf(AuthMode.LOGIN to "Sign In", AuthMode.SIGNUP to "Sign Up").forEach { (m, label) ->
            val selected = current == m
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) Brush.horizontalGradient(
                            listOf(SacredGoldLight, SacredGold, SacredGoldDark)
                        ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onSelect(m) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) Color(0xFF1A1206) else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun AuthMessageBanner(message: String, isError: Boolean) {
    val bg = if (isError) InauspiciousRed.copy(alpha = 0.12f) else AuspiciousGreen.copy(alpha = 0.12f)
    val border = if (isError) InauspiciousRed.copy(alpha = 0.45f) else AuspiciousGreen.copy(alpha = 0.45f)
    val fg = if (isError) Color(0xFFFF8A80) else Color(0xFF69F0AE)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = message,
            color = fg,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AuthModeFooter(viewModel: AuthViewModel, mode: AuthMode) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (mode) {
            AuthMode.LOGIN -> {
                TextButton(onClick = { viewModel.switchMode(AuthMode.FORGOT_PASSWORD) }) {
                    Text("Forgot password?", color = SacredGoldLight, fontSize = 13.sp)
                }
            }
            AuthMode.SIGNUP -> {
                Text(
                    text = "By creating an account you agree to our Terms of Use and Privacy Policy.",
                    color = TextSecondary.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            AuthMode.VERIFY_SIGNUP_OTP, AuthMode.FORGOT_PASSWORD -> {
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
    leadingIcon: ImageVector? = null,
    prefix: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, fontSize = 14.sp) },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = SacredGold.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        prefix = prefix?.let { { Text(it, color = SacredGoldLight, fontSize = 14.sp) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() }
        ),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SacredGold,
            unfocusedBorderColor = CosmicBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = SacredGoldLight,
            unfocusedLabelColor = TextSecondary,
            cursorColor = SacredGold,
            focusedLeadingIconColor = SacredGold,
            unfocusedLeadingIconColor = TextSecondary
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = if (isLoading) Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                else Brush.horizontalGradient(
                    listOf(Color(0xFFFFC94D), SacredGold, SacredGoldDark)
                )
            )
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = SacredGoldLight,
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp
            )
        } else {
            Text(
                text,
                color = Color(0xFF1A1206),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/** 0 (empty) .. 4 (strong). */
private fun passwordStrength(password: String): Int {
    if (password.isEmpty()) return 0
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
    return score.coerceIn(0, 4)
}

@Composable
private fun PasswordStrengthMeter(password: String) {
    if (password.isEmpty()) return
    val strength = passwordStrength(password)
    val labels = listOf("Weak", "Fair", "Good", "Strong")
    val colors = listOf(
        InauspiciousRed,
        Color(0xFFFF7043),
        SacredGold,
        AuspiciousGreen
    )
    val index = (strength - 1).coerceIn(0, 3)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (i < strength) colors[index] else CosmicBorder
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Password strength: ${labels[index]}",
            color = colors[index],
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ColumnScope.LoginForm(viewModel: AuthViewModel, isLoading: Boolean) {
    val email by viewModel.loginEmail.collectAsState()
    val password by viewModel.loginPassword.collectAsState()

    Text(
        "Welcome Back",
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "Sign in with your email and password.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )
    Spacer(modifier = Modifier.height(20.dp))

    AuthTextField(
        value = email,
        onValueChange = viewModel::setLoginEmail,
        label = "Email",
        placeholder = "you@example.com",
        keyboardType = KeyboardType.Email,
        leadingIcon = Icons.Filled.Email,
        imeAction = ImeAction.Next
    )
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(
        value = password,
        onValueChange = viewModel::setLoginPassword,
        label = "Password",
        keyboardType = KeyboardType.Password,
        isPassword = true,
        leadingIcon = Icons.Filled.Lock,
        imeAction = ImeAction.Done,
        onImeAction = { viewModel.login() }
    )
    Spacer(modifier = Modifier.height(20.dp))
    AuthPrimaryButton("Sign In", isLoading, viewModel::login)
}

@Composable
private fun ColumnScope.SignupForm(viewModel: AuthViewModel, isLoading: Boolean) {
    val name by viewModel.signupName.collectAsState()
    val email by viewModel.signupEmail.collectAsState()
    val mobile by viewModel.signupMobile.collectAsState()
    val password by viewModel.signupPassword.collectAsState()
    val confirm by viewModel.signupConfirmPassword.collectAsState()
    val otpChannel by viewModel.otpChannel.collectAsState()

    val channelLabel = when (otpChannel) {
        OtpChannels.MOBILE -> "mobile number via SMS"
        OtpChannels.WHATSAPP -> "WhatsApp"
        else -> "email"
    }

    Text(
        "Create Account",
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "A verification code will be sent to your $channelLabel.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )
    Spacer(modifier = Modifier.height(20.dp))

    AuthTextField(
        value = name,
        onValueChange = viewModel::setSignupName,
        label = "Full Name",
        placeholder = "Your name",
        leadingIcon = Icons.Filled.Person,
        imeAction = ImeAction.Next
    )
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(
        value = email,
        onValueChange = viewModel::setSignupEmail,
        label = "Email",
        placeholder = "you@example.com",
        keyboardType = KeyboardType.Email,
        leadingIcon = Icons.Filled.Email,
        imeAction = ImeAction.Next
    )
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(
        value = mobile,
        onValueChange = viewModel::setSignupMobile,
        label = "Mobile Number",
        placeholder = "10-digit mobile number",
        keyboardType = KeyboardType.Phone,
        leadingIcon = Icons.Filled.Phone,
        prefix = "+91 ",
        imeAction = ImeAction.Next
    )
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(
        value = password,
        onValueChange = viewModel::setSignupPassword,
        label = "Password (min 8 characters)",
        keyboardType = KeyboardType.Password,
        isPassword = true,
        leadingIcon = Icons.Filled.Lock,
        imeAction = ImeAction.Next
    )
    Spacer(modifier = Modifier.height(8.dp))
    PasswordStrengthMeter(password)
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(
        value = confirm,
        onValueChange = viewModel::setSignupConfirmPassword,
        label = "Confirm Password",
        keyboardType = KeyboardType.Password,
        isPassword = true,
        leadingIcon = Icons.Filled.Lock,
        imeAction = ImeAction.Done,
        onImeAction = { viewModel.register() }
    )
    Spacer(modifier = Modifier.height(20.dp))
    AuthPrimaryButton("Create Account", isLoading, viewModel::register)
}

/**
 * Six individual digit boxes with auto-advance on entry and
 * focus-back on clear. Emits the compacted digit string upstream.
 */
@Composable
private fun OtpCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val boxes = remember { mutableStateListOf<Char>().apply { repeat(6) { add(' ') } } }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    // Reset boxes when the code is cleared externally (e.g. after resend).
    LaunchedEffect(code) {
        if (code.isEmpty()) {
            for (i in 0 until 6) boxes[i] = ' '
        }
    }
    LaunchedEffect(Unit) { focusRequesters[0].requestFocus() }

    fun emit() {
        onCodeChange(boxes.filter { it != ' ' }.joinToString(""))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        for (i in 0 until 6) {
            val ch = boxes[i]
            val filled = ch != ' '
            BasicTextField(
                value = if (filled) ch.toString() else "",
                onValueChange = { v ->
                    val digit = v.filter { it.isDigit() }.lastOrNull()
                    if (digit != null) {
                        boxes[i] = digit
                        if (i < 5) focusRequesters[i + 1].requestFocus()
                    } else {
                        boxes[i] = ' '
                        if (i > 0) focusRequesters[i - 1].requestFocus()
                    }
                    emit()
                },
                modifier = Modifier
                    .size(48.dp)
                    .focusRequester(focusRequesters[i]),
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = if (i < 5) ImeAction.Next else ImeAction.Done
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (filled) CosmicCardBg else CosmicSurface)
                            .border(
                                width = if (filled) 1.5.dp else 1.dp,
                                color = if (filled) SacredGold.copy(alpha = 0.75f) else CosmicBorder,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) { innerTextField() }
                }
            )
        }
    }
}

@Composable
private fun ColumnScope.VerifyOtpForm(viewModel: AuthViewModel, isLoading: Boolean) {
    val email by viewModel.otpEmail.collectAsState()
    val code by viewModel.otpCode.collectAsState()
    val cooldown by viewModel.resendCooldown.collectAsState()
    val otpChannel by viewModel.otpChannel.collectAsState()

    val channelLabel = when (otpChannel) {
        OtpChannels.MOBILE -> "mobile number"
        OtpChannels.WHATSAPP -> "WhatsApp"
        else -> "email"
    }

    Text(
        "Verify Your $channelLabel".replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "Enter the 6-digit code sent to $email.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )
    Spacer(modifier = Modifier.height(20.dp))

    OtpCodeInput(code = code, onCodeChange = viewModel::setOtpCode)

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
            color = if (cooldown > 0) TextSecondary else SacredGoldLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ColumnScope.ForgotPasswordForm(viewModel: AuthViewModel, isLoading: Boolean) {
    val email by viewModel.forgotEmail.collectAsState()

    Text(
        "Reset Password",
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "Enter your account email to receive reset instructions.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )
    Spacer(modifier = Modifier.height(20.dp))

    AuthTextField(
        value = email,
        onValueChange = viewModel::setForgotEmail,
        label = "Email",
        placeholder = "you@example.com",
        keyboardType = KeyboardType.Email,
        leadingIcon = Icons.Filled.Email,
        imeAction = ImeAction.Done,
        onImeAction = { viewModel.forgotPassword() }
    )
    Spacer(modifier = Modifier.height(20.dp))
    AuthPrimaryButton("Send Reset Instructions", isLoading, viewModel::forgotPassword)
}
