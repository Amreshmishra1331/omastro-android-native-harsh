package com.omastro.app.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.omastro.app.ui.components.CosmicBackground
import com.omastro.app.ui.components.GradientButton
import com.omastro.app.ui.components.GradientText
import com.omastro.app.ui.theme.*

/**
 * Premium Cosmic Vedic AuthScreen — OmAstro AI.
 *
 * Visual hierarchy:
 * - Full-screen CosmicBackground with twinkling starfield
 * - Brand header: Sacred Om symbol in gold badge + "OmAstro AI" title & Vedic tagline
 * - Dual Tabs: LOGIN | REGISTER with gold underline indicator
 * - LOGIN tab: Outlined fields (gold focused border), Forgot Password link, Login GradientButton,
 *   "या" (OR) divider, White Google Sign-In card, "Login with OTP" trigger
 * - REGISTER tab: Full Name, Email, Phone, Password with strength meter, Confirm Password,
 *   Terms checkbox, Create Account CTA
 * - OTP ModalBottomSheet: 6-box OTP auto-advance input with resend cooldown timer
 * - Full backward compatibility with AuthViewModel & repository wiring
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    initialMode: AuthMode = AuthMode.LOGIN,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val mode by viewModel.mode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()
    val loginSuccess by viewModel.loginSuccess.collectAsState()

    // Honor deep-link mode (e.g. forgot password) on initial launch
    LaunchedEffect(Unit) {
        if (initialMode != AuthMode.LOGIN) {
            viewModel.switchMode(initialMode)
        }
    }

    LaunchedEffect(loginSuccess) {
        if (loginSuccess) onLoginSuccess()
    }

    // Google Sign-In Activity Launcher
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (val r = GoogleSignInHelper.parseResult(result.data)) {
            is GoogleSignInHelper.Result.Success -> viewModel.signInWithGoogle(r.idToken)
            is GoogleSignInHelper.Result.Cancelled -> { /* user dismissed silently */ }
            is GoogleSignInHelper.Result.Error -> viewModel.setGoogleError(r.message)
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
    var showOtpLoginPrompt by remember { mutableStateOf(false) }

    CosmicBackground(
        modifier = Modifier.fillMaxSize(),
        starCount = 85,
        showNebula = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Top Brand Header: Om Badge + OmAstro AI Logo ──
            AuthBrandHeader()

            Spacer(modifier = Modifier.height(24.dp))

            // ── Main Glassmorphism Card ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CosmicCardBg.copy(alpha = 0.92f),
                                CosmicSurface.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SacredGold.copy(alpha = 0.5f),
                                CosmicCardBorder.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (mode == AuthMode.LOGIN || mode == AuthMode.SIGNUP) {
                        // ── Tab Row: LOGIN | REGISTER with Gold Underline ──
                        AuthTabRow(
                            selectedMode = mode,
                            onTabSelected = { viewModel.switchMode(it) }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // ── Animated Form Content ──
                    AnimatedContent(
                        targetState = mode,
                        transitionSpec = {
                            val forward = targetState.ordinal > initialState.ordinal
                            (fadeIn(tween(250)) + slideInHorizontally(tween(250)) { if (forward) it / 3 else -it / 3 }) togetherWith
                                (fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { if (forward) -it / 3 else it / 3 })
                        },
                        label = "authFormTransition"
                    ) { targetMode ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            when (targetMode) {
                                AuthMode.LOGIN -> LoginForm(
                                    viewModel = viewModel,
                                    isLoading = isLoading,
                                    onGoogleClick = launchGoogleSignIn,
                                    onOpenOtpLogin = { showOtpLoginPrompt = true },
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
                                AuthMode.VERIFY_SIGNUP_OTP -> {
                                    // Inline helper if bottom sheet is not used or dismissed
                                    Text(
                                        text = "Verification code sent to your registered address.",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    GradientButton(
                                        text = "Open Verification Sheet",
                                        onClick = { /* sheet is auto-opened below */ }
                                    )
                                }
                                AuthMode.FORGOT_PASSWORD -> ForgotPasswordForm(
                                    viewModel = viewModel,
                                    isLoading = isLoading,
                                    onBackToLogin = { viewModel.switchMode(AuthMode.LOGIN) }
                                )
                            }
                        }
                    }

                    // ── Inline Error Banner ──
                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(14.dp))
                        AuthMessageBanner(message = msg, isError = true)
                    }

                    // ── Inline Info Banner ──
                    infoMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(14.dp))
                        AuthMessageBanner(message = msg, isError = false)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── OTP ModalBottomSheet (auto-shown for VERIFY_SIGNUP_OTP or Login OTP trigger) ──
        if (mode == AuthMode.VERIFY_SIGNUP_OTP || showOtpLoginPrompt) {
            val isSignupVerify = (mode == AuthMode.VERIFY_SIGNUP_OTP)
            ModalBottomSheet(
                onDismissRequest = {
                    if (isSignupVerify) {
                        viewModel.switchMode(AuthMode.SIGNUP)
                    } else {
                        showOtpLoginPrompt = false
                    }
                },
                containerColor = CosmicCardBg,
                scrimColor = CosmicBackground.copy(alpha = 0.78f),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(width = 44.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SacredGold.copy(alpha = 0.5f))
                    )
                },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                OtpBottomSheetContent(
                    viewModel = viewModel,
                    isLoading = isLoading,
                    isSignupVerify = isSignupVerify,
                    onDismiss = {
                        if (isSignupVerify) {
                            viewModel.switchMode(AuthMode.SIGNUP)
                        } else {
                            showOtpLoginPrompt = false
                        }
                    }
                )
            }
        }
    }

    // ── Terms & Privacy Dialogs ──
    TermsPrivacyDialogs(
        showTerms = showTermsDialog,
        showPrivacy = showPrivacyDialog,
        onDismiss = {
            showTermsDialog = false
            showPrivacyDialog = false
        }
    )
}

/**
 * Top brand header with glowing Om badge and gold gradient text.
 */
@Composable
private fun AuthBrandHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SacredGoldSubtle)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(SacredGoldGlow, SacredGold, SacredSaffron)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ॐ",
                color = SacredGoldGlow,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        GradientText(
            text = "OmAstro AI",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center
            ),
            gradient = Brush.horizontalGradient(
                listOf(SacredGoldGlow, SacredGold, SacredSaffron)
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "वैदिक ज्ञान, AI शक्ति",
            color = SacredGoldLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Tab row with LOGIN | REGISTER tabs and gold underline indicator.
 */
@Composable
private fun AuthTabRow(
    selectedMode: AuthMode,
    onTabSelected: (AuthMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.dp,
                color = Color.Transparent
            ),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(
            AuthMode.LOGIN to "LOGIN",
            AuthMode.SIGNUP to "REGISTER"
        ).forEach { (modeOption, label) ->
            val isSelected = (selectedMode == modeOption)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(modeOption) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    color = if (isSelected) SacredGold else TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(
                                    listOf(SacredGoldGlow, SacredGold, SacredSaffron)
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.Transparent)
                                )
                            }
                        )
                )
            }
        }
    }
}

/**
 * LOGIN Tab Form.
 */
@Composable
private fun LoginForm(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    onGoogleClick: () -> Unit,
    onOpenOtpLogin: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val identifier by viewModel.loginEmail.collectAsState()
    val password by viewModel.loginPassword.collectAsState()
    val rememberPw by viewModel.rememberPassword.collectAsState()
    val termsAccepted by viewModel.termsAccepted.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        CosmicAuthTextField(
            value = identifier,
            onValueChange = viewModel::setLoginEmail,
            label = "Email or Mobile",
            placeholder = "name@example.com or 10-digit mobile",
            leadingIcon = Icons.Default.Person,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(14.dp))

        CosmicAuthTextField(
            value = password,
            onValueChange = viewModel::setLoginPassword,
            label = "Password",
            placeholder = "Enter your password",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = { viewModel.login() }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Remember Password + Forgot Password?
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberPw,
                onCheckedChange = viewModel::setRememberPassword,
                colors = CheckboxDefaults.colors(
                    checkedColor = SacredGold,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = CosmicBackground
                )
            )
            Text(
                text = "Remember me",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.clickable { viewModel.setRememberPassword(!rememberPw) }
            )

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = { viewModel.switchMode(AuthMode.FORGOT_PASSWORD) },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Forgot Password?",
                    color = SacredGoldLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Terms & Policy Checkbox
        TermsCheckboxRow(
            checked = termsAccepted,
            onCheckedChange = viewModel::setTermsAccepted,
            onTermsClick = onTermsClick,
            onPrivacyClick = onPrivacyClick
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Primary Login Button
        GradientButton(
            text = "Login",
            isLoading = isLoading,
            onClick = { viewModel.login() },
            modifier = Modifier.fillMaxWidth()
        )

        // Divider with "या (OR)"
        OrDivider()

        // Google Sign-In Card Button
        GoogleSignInButton(
            isLoading = isLoading,
            onClick = onGoogleClick
        )

        Spacer(modifier = Modifier.height(14.dp))

        // OTP Login Option Button
        OutlinedButton(
            onClick = onOpenOtpLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.4f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = CosmicSurface.copy(alpha = 0.5f),
                contentColor = SacredGoldLight
            )
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "OTP Login",
                tint = SacredGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Login with OTP / Mobile",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * REGISTER Tab Form.
 */
@Composable
private fun SignupForm(
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

    Column(modifier = Modifier.fillMaxWidth()) {
        CosmicAuthTextField(
            value = name,
            onValueChange = viewModel::setSignupName,
            label = "Full Name",
            placeholder = "Enter your full name",
            leadingIcon = Icons.Default.Person,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(12.dp))

        CosmicAuthTextField(
            value = email,
            onValueChange = viewModel::setSignupEmail,
            label = "Email Address",
            placeholder = "name@example.com",
            keyboardType = KeyboardType.Email,
            leadingIcon = Icons.Default.Email,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(12.dp))

        CosmicAuthTextField(
            value = mobile,
            onValueChange = viewModel::setSignupMobile,
            label = "Phone Number",
            placeholder = "10-digit mobile number",
            keyboardType = KeyboardType.Phone,
            leadingIcon = Icons.Default.Phone,
            prefix = "+91 ",
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(12.dp))

        CosmicAuthTextField(
            value = password,
            onValueChange = viewModel::setSignupPassword,
            label = "Password (min 8 chars)",
            placeholder = "Enter password",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            leadingIcon = Icons.Default.Lock,
            imeAction = ImeAction.Next
        )

        Spacer(modifier = Modifier.height(8.dp))
        PasswordStrengthMeter(password)

        Spacer(modifier = Modifier.height(12.dp))

        CosmicAuthTextField(
            value = confirm,
            onValueChange = viewModel::setSignupConfirmPassword,
            label = "Confirm Password",
            placeholder = "Re-enter password",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            leadingIcon = Icons.Default.Lock,
            imeAction = ImeAction.Done,
            onImeAction = { viewModel.register() }
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "A 6-digit verification code will be sent to your email.",
            color = TextSecondary.copy(alpha = 0.8f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        TermsCheckboxRow(
            checked = termsAccepted,
            onCheckedChange = viewModel::setTermsAccepted,
            onTermsClick = onTermsClick,
            onPrivacyClick = onPrivacyClick
        )

        Spacer(modifier = Modifier.height(18.dp))

        GradientButton(
            text = "Create Account",
            isLoading = isLoading,
            onClick = { viewModel.register() },
            modifier = Modifier.fillMaxWidth()
        )

        OrDivider()

        GoogleSignInButton(
            isLoading = isLoading,
            onClick = onGoogleClick
        )
    }
}

/**
 * Forgot Password view.
 */
@Composable
private fun ForgotPasswordForm(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    onBackToLogin: () -> Unit
) {
    val email by viewModel.forgotEmail.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Reset Password",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Enter your registered email address to receive password reset instructions.",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        CosmicAuthTextField(
            value = email,
            onValueChange = viewModel::setForgotEmail,
            label = "Registered Email",
            placeholder = "name@example.com",
            keyboardType = KeyboardType.Email,
            leadingIcon = Icons.Default.Email,
            imeAction = ImeAction.Done,
            onImeAction = { viewModel.forgotPassword() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        GradientButton(
            text = "Send Reset Instructions",
            isLoading = isLoading,
            onClick = { viewModel.forgotPassword() },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        TextButton(onClick = onBackToLogin) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = SacredGoldLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Back to Sign In",
                    color = SacredGoldLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Content displayed inside the OTP ModalBottomSheet.
 */
@Composable
private fun OtpBottomSheetContent(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    isSignupVerify: Boolean,
    onDismiss: () -> Unit
) {
    val email by viewModel.otpEmail.collectAsState()
    val code by viewModel.otpCode.collectAsState()
    val cooldown by viewModel.resendCooldown.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon badge
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(SacredGoldSubtle)
                .border(1.dp, SacredGold.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "OTP Shield",
                tint = SacredGoldGlow,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isSignupVerify) "सत्यापन कोड (Verify OTP)" else "OTP Login",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        val targetDest = email.ifBlank { "your registered address" }
        Text(
            text = "Enter the 6-digit code sent to $targetDest",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 6-box OTP input
        OtpCodeInput(
            code = code,
            onCodeChange = viewModel::setOtpCode
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Verify CTA Button
        GradientButton(
            text = "Verify & Continue",
            isLoading = isLoading,
            onClick = {
                if (isSignupVerify) {
                    viewModel.verifySignupOtp()
                } else {
                    viewModel.verifySignupOtp()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Resend Timer Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cooldown > 0) {
                Text(
                    text = "Resend code in ${cooldown}s",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            } else {
                TextButton(
                    onClick = { viewModel.resendSignupOtp() },
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Resend Code",
                        color = SacredGoldLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        errorMessage?.let { msg ->
            Spacer(modifier = Modifier.height(10.dp))
            AuthMessageBanner(message = msg, isError = true)
        }

        infoMessage?.let { msg ->
            Spacer(modifier = Modifier.height(10.dp))
            AuthMessageBanner(message = msg, isError = false)
        }
    }
}

/**
 * 6-box OTP Input with auto-advance and focus backspace.
 */
@Composable
private fun OtpCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        focusRequesters.firstOrNull()?.requestFocus()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 6) {
            val char = code.getOrNull(i)?.toString() ?: ""
            var isFocused by remember { mutableStateOf(false) }

            BasicTextField(
                value = char,
                onValueChange = { newValue ->
                    val digitsOnly = newValue.filter { it.isDigit() }
                    if (digitsOnly.isNotEmpty()) {
                        val nextDigit = digitsOnly.last()
                        val newCode = StringBuilder(code)
                        if (i < code.length) {
                            newCode.setCharAt(i, nextDigit)
                        } else {
                            newCode.append(nextDigit)
                        }
                        val finalCode = newCode.take(6).toString()
                        onCodeChange(finalCode)

                        if (i < 5) {
                            focusRequesters[i + 1].requestFocus()
                        }
                    } else {
                        if (code.isNotEmpty() && i < code.length) {
                            val newCode = StringBuilder(code).deleteCharAt(i).toString()
                            onCodeChange(newCode)
                        }
                        if (i > 0) {
                            focusRequesters[i - 1].requestFocus()
                        }
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .focusRequester(focusRequesters[i])
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = TextStyle(
                    color = SacredGoldGlow,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = if (i == 5) ImeAction.Done else ImeAction.Next
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    val filled = char.isNotEmpty()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicSurface.copy(alpha = 0.85f))
                            .border(
                                width = if (isFocused || filled) 1.5.dp else 1.dp,
                                color = if (isFocused) SacredGold else if (filled) SacredGold.copy(alpha = 0.65f) else CosmicCardBorder,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        innerTextField()
                    }
                }
            )
        }
    }
}

/**
 * Premium Outlined TextField with gold focused border.
 */
@Composable
private fun CosmicAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    prefix: String? = null,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = if (placeholder.isNotBlank()) {
            { Text(placeholder, fontSize = 13.sp, color = TextHint) }
        } else null,
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
        prefix = prefix?.let {
            { Text(it, color = SacredGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
        },
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
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() }
        ),
        singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SacredGold,
            unfocusedBorderColor = CosmicCardBorder,
            focusedLabelColor = SacredGold,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = SacredGold,
            focusedLeadingIconColor = SacredGold,
            unfocusedLeadingIconColor = TextSecondary,
            focusedTrailingIconColor = SacredGold,
            unfocusedTrailingIconColor = TextSecondary,
            focusedContainerColor = CosmicSurface.copy(alpha = 0.65f),
            unfocusedContainerColor = CosmicSurface.copy(alpha = 0.45f)
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * White card Google Sign-In button with official Google G mark.
 */
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
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = SacredGold,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleGLogo(modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sign in with Google",
                    color = Color(0xFF1F2430),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Divider with "या (OR)".
 */
@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(CosmicCardBorder)
        )
        Text(
            text = "   या (OR)   ",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(CosmicCardBorder)
        )
    }
}

/**
 * Password strength calculation and indicator.
 */
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
    val labels = listOf("Weak (कमजोर)", "Fair (ठीक)", "Good (अच्छा)", "Strong (मजबूत)")
    val colors = listOf(
        InauspiciousRed,
        WarningAmber,
        SacredGold,
        AuspiciousGreen
    )
    val index = (strength - 1).coerceIn(0, 3)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (i < strength) colors[index] else CosmicCardBorder
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
 * Terms and conditions checkbox row.
 */
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
                checkedColor = SacredGold,
                uncheckedColor = TextSecondary,
                checkmarkColor = CosmicBackground
            )
        )
        val annotated = buildAnnotatedString {
            append("I agree to the ")
            pushStringAnnotation(tag = "terms", annotation = "terms")
            withStyle(
                SpanStyle(
                    color = SacredGoldLight,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            ) { append("Terms & Conditions") }
            pop()
            append(" and ")
            pushStringAnnotation(tag = "privacy", annotation = "privacy")
            withStyle(
                SpanStyle(
                    color = SacredGoldLight,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            ) { append("Privacy Policy") }
            pop()
        }
        ClickableText(
            text = annotated,
            style = TextStyle(color = TextSecondary, fontSize = 12.sp),
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

/**
 * Inline message banner for errors or informational success messages.
 */
@Composable
private fun AuthMessageBanner(message: String, isError: Boolean) {
    val bg = if (isError) InauspiciousRed.copy(alpha = 0.15f) else AuspiciousGreen.copy(alpha = 0.15f)
    val border = if (isError) InauspiciousRed.copy(alpha = 0.4f) else AuspiciousGreen.copy(alpha = 0.4f)
    val fg = if (isError) InauspiciousRed else AuspiciousGreen

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
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

/**
 * Dialogs for Terms & Conditions and Privacy Policy.
 */
@Composable
private fun TermsPrivacyDialogs(
    showTerms: Boolean,
    showPrivacy: Boolean,
    onDismiss: () -> Unit
) {
    if (showTerms) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = SacredGoldLight, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CosmicCardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "Your birth details and personal information are used solely to generate " +
                        "astrological calculations. We do not sell your personal data. " +
                        "Authentication tokens are stored securely using encrypted storage on " +
                        "your device. You may request deletion of your account and associated " +
                        "data at any time from Settings. Anonymous usage statistics may be " +
                        "collected to improve the service.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = SacredGoldLight, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CosmicCardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

/**
 * Official Google 4-color "G" vector mark.
 */
private fun googleGBluePath(): Path = Path().apply {
    moveTo(23.49f, 12.27f)
    cubicTo(23.49f, 11.48f, 23.42f, 10.73f, 23.30f, 10.00f)
    lineTo(12.00f, 10.00f)
    lineTo(12.00f, 14.51f)
    lineTo(18.47f, 14.51f)
    cubicTo(18.18f, 15.99f, 17.33f, 17.24f, 16.07f, 18.09f)
    lineTo(16.07f, 21.09f)
    lineTo(19.93f, 21.09f)
    cubicTo(22.19f, 19.00f, 23.49f, 15.92f, 23.49f, 12.27f)
    close()
}

private fun googleGGreenPath(): Path = Path().apply {
    moveTo(12.00f, 24.00f)
    cubicTo(15.24f, 24.00f, 17.95f, 22.92f, 19.93f, 21.09f)
    lineTo(16.07f, 18.09f)
    cubicTo(14.99f, 18.81f, 13.62f, 19.25f, 12.00f, 19.25f)
    cubicTo(8.87f, 19.25f, 6.22f, 17.14f, 5.27f, 14.29f)
    lineTo(1.29f, 14.29f)
    lineTo(1.29f, 17.38f)
    cubicTo(3.26f, 21.30f, 7.31f, 24.00f, 12.00f, 24.00f)
    close()
}

private fun googleGYellowPath(): Path = Path().apply {
    moveTo(5.27f, 14.29f)
    cubicTo(5.02f, 13.57f, 4.89f, 12.80f, 4.89f, 12.00f)
    cubicTo(4.89f, 11.20f, 5.03f, 10.43f, 5.27f, 9.71f)
    lineTo(5.27f, 6.62f)
    lineTo(1.29f, 6.62f)
    cubicTo(0.47f, 8.24f, 0.00f, 10.06f, 0.00f, 12.00f)
    cubicTo(0.00f, 13.94f, 0.47f, 15.76f, 1.29f, 17.38f)
    lineTo(5.27f, 14.29f)
    close()
}

private fun googleGRedPath(): Path = Path().apply {
    moveTo(12.00f, 4.75f)
    cubicTo(13.77f, 4.75f, 15.35f, 5.36f, 16.60f, 6.55f)
    lineTo(20.02f, 3.13f)
    cubicTo(17.95f, 1.19f, 15.24f, 0.00f, 12.00f, 0.00f)
    cubicTo(7.31f, 0.00f, 3.26f, 2.70f, 1.29f, 6.62f)
    lineTo(5.27f, 9.71f)
    cubicTo(6.22f, 6.86f, 8.87f, 4.75f, 12.00f, 4.75f)
    close()
}

@Composable
private fun GoogleGLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 24f
        withTransform({ scale(scale, scale, Offset.Zero) }) {
            drawPath(googleGBluePath(), Color(0xFF4285F4))
            drawPath(googleGGreenPath(), Color(0xFF34A853))
            drawPath(googleGYellowPath(), Color(0xFFFBBC05))
            drawPath(googleGRedPath(), Color(0xFFEA4335))
        }
    }
}
