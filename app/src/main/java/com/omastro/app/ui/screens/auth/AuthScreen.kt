package com.omastro.app.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

/**
 * Authentication entry point — Flutter-style LIGHT theme matching the
 * OmAstro website's day mode (cream background, white cards, orange CTAs).
 *
 * Four modes with animated transitions:
 *  - LOGIN (email + password, plus "Sign in with Google")
 *  - SIGNUP (name / email / mobile / password + strength meter) → email OTP
 *  - VERIFY_SIGNUP_OTP (6 individual digit boxes, auto-advance)
 *  - FORGOT_PASSWORD (email → reset instructions)
 *
 * Google Sign-In: "Sign in with Google" uses Credential Manager to obtain a
 * Google ID token, which is posted to POST /auth/firebase (see
 * [GoogleSignInHelper] and [AuthViewModel.signInWithGoogle]).
 *
 * OTP channel: the signup/verify/resend calls carry `otp_channel`
 * ([OtpChannels.DEFAULT], currently "email"). See [OtpChannels] for how to
 * enable mobile/WhatsApp OTP later. All server errors are shown inline;
 * no success is ever fabricated.
 */

// ─── Light-theme palette (website day mode) ─────────────────────────────
private val LightPageBg = Color(0xFFFAF6EC)
private val LightCard = Color(0xFFFFFFFF)
private val LightCardAlt = Color(0xFFFFFCF3)
private val LightText = Color(0xFF251A0B)
private val LightMuted = Color(0xFF7C6A50)
private val LightFaint = Color(0xFFB5A487)
private val LightAccent = Color(0xFFE8590C)
private val LightAccentDeep = Color(0xFFC2410C)
private val LightGold = Color(0xFFA8760A)
private val LightGoldBright = Color(0xFFD4AF37)
private val LightBorder = Color(0xFFE8DCC4)
private val LightMaroon = Color(0xFF5C0B0B)
private val LightError = Color(0xFFC62828)
private val LightErrorBg = Color(0xFFFDECEA)
private val LightSuccess = Color(0xFF2E7D32)
private val LightSuccessBg = Color(0xFFE8F5E9)

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
            .background(LightPageBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthBrandHeader()

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(LightCard)
                    .border(1.dp, LightBorder, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Secure Vedic astrology guidance",
                color = LightMuted.copy(alpha = 0.85f),
                fontSize = 12.sp,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
private fun AuthBrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // OmAstro logo: gold ॐ in a maroon rounded square, like the website header.
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(LightMaroon)
                .border(2.dp, LightGoldBright.copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ॐ",
                color = LightGoldBright,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "OmAstro AI",
            color = LightText,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Vedic Astrology Guidance",
            color = LightMuted,
            fontSize = 14.sp,
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
            .background(LightPageBg)
            .border(1.dp, LightBorder, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        listOf(AuthMode.LOGIN to "Sign In", AuthMode.SIGNUP to "Sign Up").forEach { (m, label) ->
            val selected = current == m
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) LightAccent else Color.Transparent)
                    .clickable { onSelect(m) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) Color.White else LightMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun AuthMessageBanner(message: String, isError: Boolean) {
    val bg = if (isError) LightErrorBg else LightSuccessBg
    val border = if (isError) LightError.copy(alpha = 0.35f) else LightSuccess.copy(alpha = 0.35f)
    val fg = if (isError) LightError else LightSuccess
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
                    Text("Forgot password?", color = LightAccentDeep, fontSize = 13.sp)
                }
            }
            AuthMode.SIGNUP -> {
                Text(
                    text = "By creating an account you agree to our Terms of Use and Privacy Policy.",
                    color = LightMuted.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            AuthMode.VERIFY_SIGNUP_OTP, AuthMode.FORGOT_PASSWORD -> {
                TextButton(onClick = { viewModel.switchMode(AuthMode.LOGIN) }) {
                    Text("Back to sign in", color = LightAccentDeep, fontSize = 13.sp)
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
                    tint = LightGold,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        prefix = prefix?.let { { Text(it, color = LightGold, fontSize = 14.sp) } },
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
                        tint = LightMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp, color = LightText),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LightAccent,
            unfocusedBorderColor = LightBorder,
            focusedTextColor = LightText,
            unfocusedTextColor = LightText,
            focusedLabelColor = LightAccentDeep,
            unfocusedLabelColor = LightMuted,
            cursorColor = LightAccent,
            focusedLeadingIconColor = LightAccent,
            unfocusedLeadingIconColor = LightFaint,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White
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
            .background(if (isLoading) LightBorder else LightAccent)
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp
            )
        } else {
            Text(
                text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Official Google "G" mark drawn with Canvas in brand colors.
 * (Brand icon — not an emoji.)
 */
@Composable
private fun GoogleGLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val strokeW = s * 0.20f
        val blue = Color(0xFF4285F4)
        val green = Color(0xFF34A853)
        val yellow = Color(0xFFFBBC05)
        val red = Color(0xFFEA4335)

        // Blue arc (main body of G)
        drawArc(
            color = blue,
            startAngle = -50f,
            sweepAngle = 250f,
            useCenter = false,
            topLeft = Offset(s * 0.08f, s * 0.08f),
            size = androidx.compose.ui.geometry.Size(s * 0.84f, s * 0.84f),
            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
        )
        // Green lower-right segment
        drawArc(
            color = green,
            startAngle = 35f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = Offset(s * 0.08f, s * 0.08f),
            size = androidx.compose.ui.geometry.Size(s * 0.84f, s * 0.84f),
            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
        )
        // Yellow lower-left segment
        drawArc(
            color = yellow,
            startAngle = 90f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = Offset(s * 0.08f, s * 0.08f),
            size = androidx.compose.ui.geometry.Size(s * 0.84f, s * 0.84f),
            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
        )
        // Red horizontal bar
        val barY = s * 0.50f
        val barPath = Path().apply {
            moveTo(s * 0.50f, barY - strokeW / 2f)
            lineTo(s * 0.94f, barY - strokeW / 2f)
            lineTo(s * 0.94f, barY + strokeW / 2f)
            lineTo(s * 0.50f, barY + strokeW / 2f)
            close()
        }
        drawPath(barPath, red)
    }
}

/** White "Sign in with Google" button with the Google G mark. */
@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.5.dp, LightBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = LightAccent,
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoogleGLogo(modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Sign in with Google",
                    color = LightText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(LightBorder)
        )
        Text(
            text = "  or continue with email  ",
            color = LightMuted,
            fontSize = 12.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(LightBorder)
        )
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
        LightError,
        Color(0xFFFF7043),
        LightGold,
        LightSuccess
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
                            if (i < strength) colors[index] else LightBorder
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val email by viewModel.loginEmail.collectAsState()
    val password by viewModel.loginPassword.collectAsState()

    Text(
        "Welcome Back",
        color = LightText,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "Sign in to continue your Vedic journey.",
        color = LightMuted,
        fontSize = 14.sp
    )
    Spacer(modifier = Modifier.height(20.dp))

    GoogleSignInButton(isLoading = isLoading) {
        scope.launch {
            when (val result = GoogleSignInHelper.signIn(context)) {
                is GoogleSignInHelper.Result.Success ->
                    viewModel.signInWithGoogle(result.idToken)
                is GoogleSignInHelper.Result.Cancelled -> { /* user dismissed — stay silent */ }
                is GoogleSignInHelper.Result.Error ->
                    viewModel.setGoogleError(result.message)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    OrDivider()
    Spacer(modifier = Modifier.height(16.dp))

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
        color = LightText,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "A verification code will be sent to your $channelLabel.",
        color = LightMuted,
        fontSize = 14.sp
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
                    color = LightText,
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
                            .background(if (filled) LightCardAlt else Color.White)
                            .border(
                                width = if (filled) 1.5.dp else 1.dp,
                                color = if (filled) LightAccent else LightBorder,
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
        color = LightText,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "Enter the 6-digit code sent to $email.",
        color = LightMuted,
        fontSize = 14.sp
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
            color = if (cooldown > 0) LightMuted else LightAccentDeep,
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
        color = LightText,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "Enter your account email to receive reset instructions.",
        color = LightMuted,
        fontSize = 14.sp
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
