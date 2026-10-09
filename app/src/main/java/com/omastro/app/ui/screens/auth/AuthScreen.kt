package com.omastro.app.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.OtpChannels

/**
 * Authentication entry point — matches the Flutter app's login/signup design
 * exactly: light-gray page, white rounded card, sparkle brand mark, orange
 * CTAs, filled input fields.
 *
 * Four modes with animated transitions:
 *  - LOGIN (Email-or-Mobile + password, Remember Password, Terms checkbox,
 *    "Log In" button, then "Sign in with Google")
 *  - SIGNUP ("Create Account" tab: name / email / mobile / password +
 *    strength meter → email OTP)
 *  - VERIFY_SIGNUP_OTP (6 individual digit boxes, auto-advance)
 *  - FORGOT_PASSWORD (email → reset instructions)
 *
 * Google Sign-In: "Sign in with Google" launches the classic
 * GoogleSignInClient account picker; the returned Google ID token is posted
 * to POST /auth/firebase (see [GoogleSignInHelper] and
 * [AuthViewModel.signInWithGoogle]).
 *
 * OTP channel: the signup/verify/resend calls carry `otp_channel`
 * ([OtpChannels.DEFAULT], currently "email"). See [OtpChannels] for how to
 * enable mobile/WhatsApp OTP later. All server errors are shown inline;
 * no success is ever fabricated.
 */

// ─── Light-theme palette (Flutter login match) ──────────────────────
private val FlutterPageBg = Color(0xFFF4F5F7)
private val FlutterCard = Color(0xFFFFFFFF)
private val FlutterFieldBg = Color(0xFFF1F2F5)
private val FlutterPillBg = Color(0xFFF0F1F4)
private val FlutterOrange = Color(0xFFFF6B00)
private val FlutterOrangeDeep = Color(0xFFE8590C)
private val FlutterText = Color(0xFF1F2430)
private val FlutterMuted = Color(0xFF8A8F98)
private val FlutterBorder = Color(0xFFD9DDE3)
private val FlutterSparkleBg = Color(0xFFFFF0E0)
private val LightError = Color(0xFFC62828)
private val LightErrorBg = Color(0xFFFDECEA)
private val LightSuccess = Color(0xFF2E7D32)
private val LightSuccessBg = Color(0xFFE8F5E9)

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val mode by viewModel.mode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()
    val loginSuccess by viewModel.loginSuccess.collectAsState()

    LaunchedEffect(loginSuccess) {
        if (loginSuccess) onLoginSuccess()
    }

    // Classic GoogleSignInClient account-picker launcher. signOut() first so
    // the account chooser always appears (no silent re-use of a stale account).
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (val r = GoogleSignInHelper.parseResult(result.data)) {
            is GoogleSignInHelper.Result.Success ->
                viewModel.signInWithGoogle(r.idToken)
            is GoogleSignInHelper.Result.Cancelled -> { /* user dismissed — stay silent */ }
            is GoogleSignInHelper.Result.Error ->
                viewModel.setGoogleError(r.message)
        }
    }
    val launchGoogleSignIn: () -> Unit = {
        if (!isLoading) {
            val client = GoogleSignInHelper.getClient(context)
            client.signOut().addOnCompleteListener {
                googleLauncher.launch(client.signInIntent)
            }
        }
    }

    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlutterPageBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(FlutterCard)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthBrandHeader(
                        subtitle = when (mode) {
                            AuthMode.SIGNUP -> "Create your account to begin."
                            AuthMode.VERIFY_SIGNUP_OTP -> "Verify your account."
                            AuthMode.FORGOT_PASSWORD -> "Recover your account."
                            else -> "Welcome Back! Please log in to continue."
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

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
                                AuthMode.LOGIN -> LoginForm(
                                    viewModel = viewModel,
                                    isLoading = isLoading,
                                    onGoogleClick = launchGoogleSignIn,
                                    onTermsClick = { showTermsDialog = true },
                                    onPrivacyClick = { showPrivacyDialog = true }
                                )
                                AuthMode.SIGNUP -> SignupForm(
                                    viewModel = viewModel,
                                    isLoading = isLoading,
                                    onGoogleClick = launchGoogleSignIn,
                                    onTermsClick = { showTermsDialog = true },
                                    onPrivacyClick = { showPrivacyDialog = true }
                                )
                                AuthMode.VERIFY_SIGNUP_OTP -> VerifyOtpForm(viewModel, isLoading)
                                AuthMode.FORGOT_PASSWORD -> ForgotPasswordForm(viewModel, isLoading)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            BackToSignInLink(viewModel, targetMode)
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
        }
    }

    TermsPrivacyDialogs(
        showTerms = showTermsDialog,
        showPrivacy = showPrivacyDialog,
        onDismiss = { showTermsDialog = false; showPrivacyDialog = false }
    )
}

@Composable
private fun AuthBrandHeader(subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Flutter brand mark: sparkle icon in a light-orange rounded square.
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(FlutterSparkleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "OmAstro",
                tint = FlutterOrange,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "OmAstro AI",
            color = FlutterOrange,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = FlutterMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AuthModeTabs(current: AuthMode, onSelect: (AuthMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FlutterPillBg)
            .padding(4.dp)
    ) {
        listOf(AuthMode.LOGIN to "Sign In", AuthMode.SIGNUP to "Create Account").forEach { (m, label) ->
            val selected = current == m
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (selected) Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) FlutterCard else Color.Transparent)
                    .clickable { onSelect(m) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) FlutterOrange else FlutterMuted,
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
private fun BackToSignInLink(viewModel: AuthViewModel, mode: AuthMode) {
    if (mode == AuthMode.VERIFY_SIGNUP_OTP || mode == AuthMode.FORGOT_PASSWORD) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TextButton(onClick = { viewModel.switchMode(AuthMode.LOGIN) }) {
                Text("Back to sign in", color = FlutterOrangeDeep, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Flutter-style filled input: light-gray background, no outline, orange
 * leading icon, rounded corners — exactly like the Flutter login fields.
 */
@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    leadingIcon: ImageVector? = null,
    prefix: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = FlutterMuted) },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = FlutterOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        prefix = prefix?.let { { Text(it, color = FlutterText, fontSize = 14.sp) } },
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
                        tint = FlutterMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp, color = FlutterText),
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = FlutterFieldBg,
            unfocusedContainerColor = FlutterFieldBg,
            disabledContainerColor = FlutterFieldBg,
            errorContainerColor = FlutterFieldBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedTextColor = FlutterText,
            unfocusedTextColor = FlutterText,
            cursorColor = FlutterOrange,
            focusedLeadingIconColor = FlutterOrange,
            unfocusedLeadingIconColor = FlutterOrange,
            focusedTrailingIconColor = FlutterMuted,
            unfocusedTrailingIconColor = FlutterMuted
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
            .background(if (isLoading) FlutterBorder else FlutterOrange)
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

/** White "Sign in with Google" button with the Google G mark (Flutter placement: after the login button). */
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
            .border(1.dp, FlutterBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = FlutterOrange,
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoogleGLogo(modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Sign in with Google",
                    color = FlutterText,
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
                .background(FlutterBorder)
        )
        Text(
            text = "   OR   ",
            color = FlutterMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(FlutterBorder)
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
        FlutterOrange,
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
                            if (i < strength) colors[index] else FlutterBorder
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

/**
 * Flutter login layout, top to bottom: Email-or-Mobile field, Password field,
 * Remember Password + Forgot Password row, Terms checkbox, "Log In" button,
 * OR divider, "Sign in with Google" button, "Don't have an account?" footer.
 */
@Composable
private fun ColumnScope.LoginForm(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    onGoogleClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val identifier by viewModel.loginEmail.collectAsState()
    val password by viewModel.loginPassword.collectAsState()
    val rememberPw by viewModel.rememberPassword.collectAsState()
    val termsAccepted by viewModel.termsAccepted.collectAsState()

    AuthTextField(
        value = identifier,
        onValueChange = viewModel::setLoginEmail,
        placeholder = "Email or Mobile Number",
        keyboardType = KeyboardType.Text,
        leadingIcon = Icons.Filled.Person,
        imeAction = ImeAction.Next
    )
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(
        value = password,
        onValueChange = viewModel::setLoginPassword,
        placeholder = "Password",
        keyboardType = KeyboardType.Password,
        isPassword = true,
        leadingIcon = Icons.Filled.Lock,
        imeAction = ImeAction.Done,
        onImeAction = { viewModel.login() }
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Remember Password (left) + Forgot Password? (right)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = rememberPw,
            onCheckedChange = viewModel::setRememberPassword,
            colors = CheckboxDefaults.colors(
                checkedColor = FlutterOrange,
                uncheckedColor = FlutterMuted,
                checkmarkColor = Color.White
            )
        )
        Text(
            text = "Remember Password",
            color = FlutterText,
            fontSize = 13.sp,
            modifier = Modifier.clickable { viewModel.setRememberPassword(!rememberPw) }
        )
        Spacer(modifier = Modifier.weight(1f))
        TextButton(
            onClick = { viewModel.switchMode(AuthMode.FORGOT_PASSWORD) },
            contentPadding = PaddingValues(4.dp)
        ) {
            Text("Forgot Password?", color = FlutterOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }

    TermsCheckboxRow(
        checked = termsAccepted,
        onCheckedChange = viewModel::setTermsAccepted,
        onTermsClick = onTermsClick,
        onPrivacyClick = onPrivacyClick
    )

    Spacer(modifier = Modifier.height(16.dp))
    AuthPrimaryButton("Log In", isLoading, viewModel::login)
    Spacer(modifier = Modifier.height(16.dp))
    OrDivider()
    Spacer(modifier = Modifier.height(16.dp))
    GoogleSignInButton(isLoading = isLoading, onClick = onGoogleClick)
    Spacer(modifier = Modifier.height(16.dp))
    AuthFooterLink(
        prefix = "Don't have an account? ",
        link = "Create Account",
        onClick = { viewModel.switchMode(AuthMode.SIGNUP) }
    )
}

@Composable
private fun ColumnScope.SignupForm(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    onGoogleClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val name by viewModel.signupName.collectAsState()
    val email by viewModel.signupEmail.collectAsState()
    val mobile by viewModel.signupMobile.collectAsState()
    val password by viewModel.signupPassword.collectAsState()
    val confirm by viewModel.signupConfirmPassword.collectAsState()
    val termsAccepted by viewModel.termsAccepted.collectAsState()
    val otpChannel by viewModel.otpChannel.collectAsState()

    val channelLabel = when (otpChannel) {
        OtpChannels.MOBILE -> "mobile number via SMS"
        OtpChannels.WHATSAPP -> "WhatsApp"
        else -> "email"
    }

    AuthTextField(
        value = name,
        onValueChange = viewModel::setSignupName,
        placeholder = "Full Name",
        leadingIcon = Icons.Filled.Person,
        imeAction = ImeAction.Next
    )
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(
        value = email,
        onValueChange = viewModel::setSignupEmail,
        placeholder = "Email",
        keyboardType = KeyboardType.Email,
        leadingIcon = Icons.Filled.Email,
        imeAction = ImeAction.Next
    )
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(
        value = mobile,
        onValueChange = viewModel::setSignupMobile,
        placeholder = "Mobile Number",
        keyboardType = KeyboardType.Phone,
        leadingIcon = Icons.Filled.Phone,
        prefix = "+91 ",
        imeAction = ImeAction.Next
    )
    Spacer(modifier = Modifier.height(12.dp))
    AuthTextField(
        value = password,
        onValueChange = viewModel::setSignupPassword,
        placeholder = "Password (min 8 characters)",
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
        placeholder = "Confirm Password",
        keyboardType = KeyboardType.Password,
        isPassword = true,
        leadingIcon = Icons.Filled.Lock,
        imeAction = ImeAction.Done,
        onImeAction = { viewModel.register() }
    )

    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "A verification code will be sent to your $channelLabel.",
        color = FlutterMuted,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    TermsCheckboxRow(
        checked = termsAccepted,
        onCheckedChange = viewModel::setTermsAccepted,
        onTermsClick = onTermsClick,
        onPrivacyClick = onPrivacyClick
    )

    Spacer(modifier = Modifier.height(16.dp))
    AuthPrimaryButton("Create Account", isLoading, viewModel::register)
    Spacer(modifier = Modifier.height(16.dp))
    OrDivider()
    Spacer(modifier = Modifier.height(16.dp))
    GoogleSignInButton(isLoading = isLoading, onClick = onGoogleClick)
    Spacer(modifier = Modifier.height(16.dp))
    AuthFooterLink(
        prefix = "Already have an account? ",
        link = "Sign In",
        onClick = { viewModel.switchMode(AuthMode.LOGIN) }
    )
}

/** Flutter "I agree to the Terms & Conditions and Privacy Policy" checkbox row. */
@Composable
private fun TermsCheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = FlutterOrange,
                uncheckedColor = FlutterMuted,
                checkmarkColor = Color.White
            )
        )
        val annotated = buildAnnotatedString {
            append("I agree to the ")
            pushStringAnnotation(tag = "terms", annotation = "terms")
            withStyle(
                SpanStyle(
                    color = FlutterOrange,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            ) { append("Terms & Conditions") }
            pop()
            append(" and ")
            pushStringAnnotation(tag = "privacy", annotation = "privacy")
            withStyle(
                SpanStyle(
                    color = FlutterOrange,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            ) { append("Privacy Policy") }
            pop()
        }
        ClickableText(
            text = annotated,
            style = TextStyle(color = FlutterMuted, fontSize = 12.sp),
            onClick = { offset ->
                annotated.getStringAnnotations(tag = "terms", start = offset, end = offset)
                    .firstOrNull()?.let { onTermsClick(); return@ClickableText }
                annotated.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                    .firstOrNull()?.let { onPrivacyClick() }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

/** Flutter "Don't have an account? Create Account" footer link. */
@Composable
private fun AuthFooterLink(prefix: String, link: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = prefix, color = FlutterMuted, fontSize = 13.sp)
        Text(
            text = link,
            color = FlutterOrange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

/** Real Terms & Privacy dialogs opened from the auth checkbox links. */
@Composable
private fun TermsPrivacyDialogs(
    showTerms: Boolean,
    showPrivacy: Boolean,
    onDismiss: () -> Unit
) {
    if (showTerms) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold, color = FlutterText) },
            text = {
                Text(
                    "By creating an OmAstro AI account you agree to use the app for lawful " +
                        "personal purposes only. Astrological readings, predictions and " +
                        "remedies are generated from traditional Vedic astrology principles " +
                        "and are provided for informational and guidance purposes only — " +
                        "they are not professional legal, medical or financial advice. " +
                        "You are responsible for keeping your login credentials confidential. " +
                        "OmAstro may update these terms; continued use of the app constitutes " +
                        "acceptance of the updated terms.",
                    color = FlutterText.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = FlutterOrange, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White
        )
    }
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, color = FlutterText) },
            text = {
                Text(
                    "Your birth details and personal information are used solely to generate " +
                        "astrological calculations. We do not sell your personal data. " +
                        "Authentication tokens are stored securely using encrypted storage on " +
                        "your device. You may request deletion of your account and associated " +
                        "data at any time from Settings. Anonymous usage statistics may be " +
                        "collected to improve the service.",
                    color = FlutterText.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = FlutterOrange, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White
        )
    }
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
                    color = FlutterText,
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
                            .background(if (filled) FlutterFieldBg else Color.White)
                            .border(
                                width = if (filled) 1.5.dp else 1.dp,
                                color = if (filled) FlutterOrange else FlutterBorder,
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
        color = FlutterText,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "Enter the 6-digit code sent to $email.",
        color = FlutterMuted,
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
            color = if (cooldown > 0) FlutterMuted else FlutterOrangeDeep,
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
        color = FlutterText,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "Enter your account email to receive reset instructions.",
        color = FlutterMuted,
        fontSize = 14.sp
    )
    Spacer(modifier = Modifier.height(20.dp))

    AuthTextField(
        value = email,
        onValueChange = viewModel::setForgotEmail,
        placeholder = "Email",
        keyboardType = KeyboardType.Email,
        leadingIcon = Icons.Filled.Email,
        imeAction = ImeAction.Done,
        onImeAction = { viewModel.forgotPassword() }
    )
    Spacer(modifier = Modifier.height(20.dp))
    AuthPrimaryButton("Send Reset Instructions", isLoading, viewModel::forgotPassword)
}
