package com.omastro.app.ui.screens.matching

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.data.model.GunaDetail
import com.omastro.app.data.model.MatchmakingResponse
import com.omastro.app.ui.components.*
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.theme.*

/**
 * MatchmakingScreen — Advanced Vedic Kundali Gun Milan Screen
 *
 * Features:
 * 1. CosmicTopBar "गुण मिलान" + back
 * 2. Two Person Input Cards side by side (expandable form on click)
 * 3. PulseGlowButton "गुण मिलान करें" full width
 * 4. Result Section (visible after match):
 *    a. Circular compatibility meter (Canvas arc, animated to % value, gold center text)
 *    b. Ashtakoot Guna table (8 rows with score bars)
 *    c. Mangal Dosha Card (two-column person indicators)
 *    d. Suggestions numbered list
 */
@Composable
fun MatchmakingScreen(
    onBack: () -> Unit,
    viewModel: MatchmakingViewModel = viewModel()
) {
    val result by viewModel.result.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val lang by LangPrefs.language.collectAsState()
    val hi = lang == "hi"

    // Form expanded states: 0 = None (compact), 1 = Groom expanded, 2 = Bride expanded
    var expandedPerson by remember { mutableIntStateOf(1) } // Default Groom expanded

    // Groom (Boy) inputs
    var boyName by remember { mutableStateOf("") }
    var boyDob by remember { mutableStateOf("") }
    var boyTob by remember { mutableStateOf("") }
    var boyPob by remember { mutableStateOf("") }

    // Bride (Girl) inputs
    var girlName by remember { mutableStateOf("") }
    var girlDob by remember { mutableStateOf("") }
    var girlTob by remember { mutableStateOf("") }
    var girlPob by remember { mutableStateOf("") }

    // Validation errors
    var fieldErrors by remember { mutableStateOf(setOf<String>()) }
    var formError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CosmicTopBar(
                title = if (hi) "गुण मिलान" else "Kundali Matching",
                subtitle = if (hi) "अष्टकूट वैदिक विवाह अनुकूलता" else "Ashtakoot Vedic Compatibility",
                onBackClick = onBack,
                showLogo = false
            )
        },
        containerColor = CosmicBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            when {
                isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = SacredGold,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = if (hi) "कुंडली मिलान की गणना हो रही है..." else "Calculating Ashtakoot Milan...",
                                    color = SacredGoldLight,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (hi) "36 गुणों एवं मंगल दोष का वैदिक विश्लेषण" else "Analyzing 36 Gunas & Mangal Dosha",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                errorMessage != null -> {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            glowColor = InauspiciousRed,
                            glowAlpha = 0.5f
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = InauspiciousRed,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (hi) "मिलान गणना में समस्या आई" else "Matchmaking Failed",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                GradientButton(
                                    text = if (hi) "पुनः प्रयास करें" else "Try Again",
                                    onClick = { viewModel.clearError() },
                                    modifier = Modifier.fillMaxWidth(0.6f)
                                )
                            }
                        }
                    }
                }

                result != null && result!!.hasRealData -> {
                    val res = result!!

                    // ---------------------------------------------------------
                    // 4a. Circular Compatibility Meter
                    // ---------------------------------------------------------
                    item {
                        CircularCompatibilityMeterCard(
                            result = res,
                            boyName = boyName.ifBlank { if (hi) "वर" else "Groom" },
                            girlName = girlName.ifBlank { if (hi) "वधू" else "Bride" },
                            hi = hi
                        )
                    }

                    // ---------------------------------------------------------
                    // 4c. Mangal Dosha Card (Two-column indicators)
                    // ---------------------------------------------------------
                    item {
                        MangalDoshaCard(
                            boyName = boyName.ifBlank { if (hi) "वर" else "Groom" },
                            girlName = girlName.ifBlank { if (hi) "वधू" else "Bride" },
                            manglikBoy = res.manglikBoy,
                            manglikGirl = res.manglikGirl,
                            hi = hi
                        )
                    }

                    // ---------------------------------------------------------
                    // 4b. Ashtakoot Guna Table (8 rows with score bars)
                    // ---------------------------------------------------------
                    item {
                        AshtakootGunaSection(
                            gunas = res.gunas,
                            totalScore = res.totalScore,
                            maxScore = res.maxScore,
                            hi = hi
                        )
                    }

                    // ---------------------------------------------------------
                    // 4d. Vedic Synthesis & Suggestions List
                    // ---------------------------------------------------------
                    item {
                        VedicSuggestionsSection(
                            conclusion = res.conclusion,
                            totalScore = res.totalScore,
                            hi = hi
                        )
                    }

                    // Check Another Pair Button
                    item {
                        OutlinedButton(
                            onClick = { viewModel.clearResult() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, SacredGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SacredGold)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = SacredGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hi) "नए जोड़े का मिलान करें" else "Check Another Pair",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                else -> {
                    // ---------------------------------------------------------
                    // Input Form Mode
                    // ---------------------------------------------------------
                    // 2. Two Person Input Cards Side by Side (Expandable)
                    item {
                        TwoPersonHeaderCards(
                            boyName = boyName,
                            boyDob = boyDob,
                            girlName = girlName,
                            girlDob = girlDob,
                            expandedPerson = expandedPerson,
                            onSelectPerson = { personIndex ->
                                expandedPerson = if (expandedPerson == personIndex) 0 else personIndex
                            },
                            hi = hi
                        )
                    }

                    // Expandable Person Details Form
                    item {
                        AnimatedVisibility(
                            visible = expandedPerson == 1,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            PersonDetailsForm(
                                title = if (hi) "वर विवरण (Groom Details)" else "Groom Details",
                                icon = "👦",
                                name = boyName,
                                dob = boyDob,
                                tob = boyTob,
                                pob = boyPob,
                                namePrefix = "boy",
                                fieldErrors = fieldErrors,
                                onNameChange = {
                                    boyName = it
                                    fieldErrors = fieldErrors - "boyName"
                                    formError = null
                                },
                                onDobChange = {
                                    boyDob = it
                                    fieldErrors = fieldErrors - "boyDob"
                                    formError = null
                                },
                                onTobChange = {
                                    boyTob = it
                                    fieldErrors = fieldErrors - "boyTob"
                                    formError = null
                                },
                                onPobChange = {
                                    boyPob = it
                                    fieldErrors = fieldErrors - "boyPob"
                                    formError = null
                                },
                                onNextClick = { expandedPerson = 2 },
                                hi = hi
                            )
                        }

                        AnimatedVisibility(
                            visible = expandedPerson == 2,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            PersonDetailsForm(
                                title = if (hi) "वधू विवरण (Bride Details)" else "Bride Details",
                                icon = "👧",
                                name = girlName,
                                dob = girlDob,
                                tob = girlTob,
                                pob = girlPob,
                                namePrefix = "girl",
                                fieldErrors = fieldErrors,
                                onNameChange = {
                                    girlName = it
                                    fieldErrors = fieldErrors - "girlName"
                                    formError = null
                                },
                                onDobChange = {
                                    girlDob = it
                                    fieldErrors = fieldErrors - "girlDob"
                                    formError = null
                                },
                                onTobChange = {
                                    girlTob = it
                                    fieldErrors = fieldErrors - "girlTob"
                                    formError = null
                                },
                                onPobChange = {
                                    girlPob = it
                                    fieldErrors = fieldErrors - "girlPob"
                                    formError = null
                                },
                                onNextClick = null,
                                hi = hi
                            )
                        }
                    }

                    // Form Validation Error (if any)
                    if (formError != null) {
                        item {
                            Text(
                                text = formError!!,
                                color = InauspiciousRed,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    }

                    // ---------------------------------------------------------
                    // 3. PulseGlowButton "गुण मिलान करें" Full Width
                    // ---------------------------------------------------------
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        PulseGlowButton(
                            text = if (hi) "गुण मिलान करें" else "Match Kundali Now",
                            onClick = {
                                val missing = buildSet {
                                    if (boyName.isBlank()) add("boyName")
                                    if (boyDob.isBlank()) add("boyDob")
                                    if (boyTob.isBlank()) add("boyTob")
                                    if (boyPob.isBlank()) add("boyPob")
                                    if (girlName.isBlank()) add("girlName")
                                    if (girlDob.isBlank()) add("girlDob")
                                    if (girlTob.isBlank()) add("girlTob")
                                    if (girlPob.isBlank()) add("girlPob")
                                }
                                fieldErrors = missing
                                if (missing.isNotEmpty()) {
                                    formError = if (hi)
                                        "कृपया वर और वधू दोनों के सभी विवरण भरें"
                                    else
                                        "Please fill in all birth details for both Groom and Bride"
                                    // Expand the person with missing fields
                                    if (missing.any { it.startsWith("boy") }) {
                                        expandedPerson = 1
                                    } else {
                                        expandedPerson = 2
                                    }
                                    return@PulseGlowButton
                                }
                                formError = null
                                viewModel.calculate(
                                    boyName = boyName,
                                    boyDob = boyDob,
                                    boyTob = boyTob,
                                    boyPob = boyPob,
                                    girlName = girlName,
                                    girlDob = girlDob,
                                    girlTob = girlTob,
                                    girlPob = girlPob
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component 2: Two Person Header Cards Side by Side
// -----------------------------------------------------------------------------
@Composable
private fun TwoPersonHeaderCards(
    boyName: String,
    boyDob: String,
    girlName: String,
    girlDob: String,
    expandedPerson: Int,
    onSelectPerson: (Int) -> Unit,
    hi: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Groom Card (Person 1)
        PersonSelectorCard(
            modifier = Modifier.weight(1f),
            title = if (hi) "वर (Groom)" else "Groom",
            icon = "👦",
            name = boyName.ifBlank { if (hi) "विवरण भरें" else "Enter Details" },
            subtitle = if (boyDob.isNotBlank()) "🎂 $boyDob" else if (hi) "जन्म विवरण" else "Birth Info",
            isSelected = expandedPerson == 1,
            isCompleted = boyName.isNotBlank() && boyDob.isNotBlank(),
            accentColor = SacredGold,
            onClick = { onSelectPerson(1) }
        )

        // Bride Card (Person 2)
        PersonSelectorCard(
            modifier = Modifier.weight(1f),
            title = if (hi) "वधू (Bride)" else "Bride",
            icon = "👧",
            name = girlName.ifBlank { if (hi) "विवरण भरें" else "Enter Details" },
            subtitle = if (girlDob.isNotBlank()) "🎂 $girlDob" else if (hi) "जन्म विवरण" else "Birth Info",
            isSelected = expandedPerson == 2,
            isCompleted = girlName.isNotBlank() && girlDob.isNotBlank(),
            accentColor = CosmicVioletLight,
            onClick = { onSelectPerson(2) }
        )
    }
}

@Composable
private fun PersonSelectorCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: String,
    name: String,
    subtitle: String,
    isSelected: Boolean,
    isCompleted: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) accentColor else if (isCompleted) AuspiciousGreen.copy(alpha = 0.6f) else CosmicCardBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bg = if (isSelected) {
        Brush.verticalGradient(
            listOf(accentColor.copy(alpha = 0.18f), CosmicCardBg)
        )
    } else {
        Brush.verticalGradient(
            listOf(CosmicCardBg, CosmicSurface)
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isSelected) accentColor.copy(alpha = 0.3f) else Color.Transparent,
                spotColor = if (isSelected) accentColor.copy(alpha = 0.4f) else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, fontSize = 24.sp)
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = AuspiciousGreen,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Expandable Person Details Form
// -----------------------------------------------------------------------------
@Composable
private fun PersonDetailsForm(
    title: String,
    icon: String,
    name: String,
    dob: String,
    tob: String,
    pob: String,
    namePrefix: String,
    fieldErrors: Set<String>,
    onNameChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onTobChange: (String) -> Unit,
    onPobChange: (String) -> Unit,
    onNextClick: (() -> Unit)?,
    hi: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        glowColor = if (namePrefix == "boy") SacredGold else CosmicViolet,
        glowAlpha = 0.4f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = SacredGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(if (hi) "पूरा नाम" else "Full Name", color = TextSecondary) },
                isError = fieldErrors.contains("${namePrefix}Name"),
                supportingText = if (fieldErrors.contains("${namePrefix}Name")) {
                    { Text(if (hi) "नाम आवश्यक है" else "Name is required", color = InauspiciousRed) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SacredGold,
                    unfocusedBorderColor = CosmicCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = SacredGold
                )
            )

            // DOB & TOB Pickers Side by Side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DatePickerField(
                        value = dob,
                        onValueChange = onDobChange,
                        label = if (hi) "जन्म तिथि" else "Birth Date"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    TimePickerField(
                        value = tob,
                        onValueChange = onTobChange,
                        label = if (hi) "जन्म समय" else "Birth Time"
                    )
                }
            }
            if (fieldErrors.contains("${namePrefix}Dob") || fieldErrors.contains("${namePrefix}Tob")) {
                Text(
                    text = if (hi) "जन्म तिथि और समय दोनों भरें" else "Both Date & Time are required",
                    color = InauspiciousRed,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Place of Birth (with PlaceSearchField)
            PlaceSearchField(
                value = pob,
                onValueChange = onPobChange,
                onPlaceSelected = { /* Geocoded by server */ },
                label = if (hi) "जन्म स्थान (शहर / राज्य)" else "Birth Place",
                placeholder = if (hi) "उदा. नई दिल्ली, मुंबई" else "e.g. New Delhi, Mumbai"
            )
            if (fieldErrors.contains("${namePrefix}Pob")) {
                Text(
                    text = if (hi) "जन्म स्थान आवश्यक है" else "Birth place is required",
                    color = InauspiciousRed,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (onNextClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onNextClick) {
                        Text(
                            text = if (hi) "वधू का विवरण भरें ➔" else "Next: Bride Details ➔",
                            color = CosmicVioletLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component 4a: Circular Compatibility Meter with Canvas Arc
// -----------------------------------------------------------------------------
@Composable
private fun CircularCompatibilityMeterCard(
    result: MatchmakingResponse,
    boyName: String,
    girlName: String,
    hi: Boolean
) {
    val score = result.totalScore.coerceAtLeast(0.0)
    val max = if (result.maxScore > 0) result.maxScore else 36.0
    val fraction = (score / max).toFloat().coerceIn(0f, 1f)

    // Animated sweep percentage
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "compatMeterProgress"
    )

    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$boyName ❤️ $girlName",
                color = SacredGoldLight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hi) "अष्टकूट अनुकूलता मीटर" else "Ashtakoot Compatibility Score",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Canvas Arc Circle Meter
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                    // Background Track Arc (260 degrees arc centered at bottom)
                    drawArc(
                        color = CosmicSurface.copy(alpha = 0.9f),
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress Arc with Gradient
                    val arcBrush = Brush.sweepGradient(
                        colors = listOf(
                            SacredGold,
                            SacredSaffron,
                            CosmicVioletLight,
                            SacredGold
                        )
                    )
                    drawArc(
                        brush = arcBrush,
                        startAngle = 140f,
                        sweepAngle = 260f * animatedFraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Center Gold Text Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val displayedScore = (animatedFraction * max).toInt()
                    Text(
                        text = "$displayedScore / ${max.toInt()}",
                        color = SacredGoldGlow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(animatedFraction * 100).toInt()}% ${if (hi) "अनुकूलता" else "Match"}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Verdict Pill
            val verdictColor = if (score >= 21) AuspiciousGreen else if (score >= 18) WarningAmber else InauspiciousRed
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = verdictColor.copy(alpha = 0.18f),
                border = BorderStroke(1.2.dp, verdictColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (score >= 18) "✨" else "⚠️", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = result.verdict.ifBlank {
                            if (score >= 28) "अति उत्तम मिलान (Excellent)"
                            else if (score >= 18) "शुभ मिलान (Favorable Match)"
                            else "सावधानी अपेक्षित (Caution Recommended)"
                        },
                        color = verdictColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component 4b: Ashtakoot Guna Table (8 Rows with Score Bars)
// -----------------------------------------------------------------------------
@Composable
private fun AshtakootGunaSection(
    gunas: List<GunaDetail>,
    totalScore: Double,
    maxScore: Double,
    hi: Boolean
) {
    // 8 Classical Vedic Ashtakoots (Varna, Vashya, Tara, Yoni, Graha Maitri, Gana, Bhakoot, Nadi)
    val defaultGunas = listOf(
        GunaDetail("वर्ण (Varna)", 1.0, 1.0, "आध्यात्मिक एवं सामाजिक स्तर की अनुकूलता"),
        GunaDetail("वश्य (Vashya)", 2.0, 2.0, "आपसी आकर्षण एवं प्रभाव क्षमता"),
        GunaDetail("तारा (Tara)", 3.0, 3.0, "भाग्य, दीर्घायु एवं स्वास्थ्य कल्याण"),
        GunaDetail("योनि (Yoni)", 3.0, 4.0, "जैविक, शारीरिक एवं अंतरंग अनुकूलता"),
        GunaDetail("ग्रह मैत्री (Maitri)", 5.0, 5.0, "मानसिक समझ, मित्रता एवं वैचारिक तालमेल"),
        GunaDetail("गण (Gana)", 6.0, 6.0, "स्वभाव, आचरण एवं जीवनशैली की साम्यता"),
        GunaDetail("भकूट (Bhakoot)", 7.0, 7.0, "संतान, समृद्धि एवं पारिवारिक सुख"),
        GunaDetail("नाड़ी (Nadi)", 8.0, 8.0, "अनुवांशिक स्वास्थ्य, नाड़ी दोष एवं जीवन ऊर्जा")
    )

    val displayList = if (gunas.isNotEmpty()) gunas else defaultGunas

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        glowColor = SacredGold,
        glowAlpha = 0.35f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hi) "अष्टकूट गुण विवरण (8 कूट)" else "Ashtakoot Guna Breakdown",
                    color = SacredGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${totalScore.toInt()} / ${maxScore.toInt()} ${if (hi) "गुण" else "Points"}",
                    color = SacredGoldLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(
                color = CosmicCardBorder.copy(alpha = 0.6f),
                thickness = 0.8.dp
            )

            displayList.forEachIndexed { index, guna ->
                GunaTableRow(index = index + 1, guna = guna)
            }
        }
    }
}

@Composable
private fun GunaTableRow(index: Int, guna: GunaDetail) {
    val fraction = if (guna.maximum > 0) (guna.obtained / guna.maximum).toFloat().coerceIn(0f, 1f) else 1f
    val scoreColor = when {
        fraction >= 0.8f -> AuspiciousGreen
        fraction >= 0.5f -> SacredGold
        else -> InauspiciousRed
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$index.",
                    color = SacredGoldLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(20.dp)
                )
                Column {
                    Text(
                        text = guna.name,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (guna.description.isNotBlank()) {
                        Text(
                            text = guna.description,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Text(
                text = "${guna.obtained.toInt()} / ${guna.maximum.toInt()}",
                color = scoreColor,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Mini animated progress score bar
        AnimatedLinearProgressIndicator(
            progress = fraction,
            color = scoreColor,
            trackColor = CosmicSurface,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

// -----------------------------------------------------------------------------
// Component 4c: Mangal Dosha Card (Two-Column Person Indicators)
// -----------------------------------------------------------------------------
@Composable
private fun MangalDoshaCard(
    boyName: String,
    girlName: String,
    manglikBoy: String,
    manglikGirl: String,
    hi: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        glowColor = Color(0xFFEF4444),
        glowAlpha = 0.35f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🔴", fontSize = 16.sp)
                Text(
                    text = if (hi) "मांगलिक दोष विश्लेषण" else "Mangal Dosha Assessment",
                    color = Color(0xFFFF8A80),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two-column Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Groom Mangal Status Column
                ManglikPersonColumn(
                    modifier = Modifier.weight(1f),
                    title = if (hi) "वर: $boyName" else "Groom: $boyName",
                    statusText = manglikBoy.ifBlank { if (hi) "मांगलिक नहीं" else "Non-Manglik" },
                    hi = hi
                )

                // Bride Mangal Status Column
                ManglikPersonColumn(
                    modifier = Modifier.weight(1f),
                    title = if (hi) "वधू: $girlName" else "Bride: $girlName",
                    statusText = manglikGirl.ifBlank { if (hi) "मांगलिक नहीं" else "Non-Manglik" },
                    hi = hi
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vedic Synthesis verdict about Mangal Dosha cancellation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CosmicSurface)
                    .padding(10.dp)
            ) {
                val isBothManglik = manglikBoy.contains("Manglik", ignoreCase = true) && manglikGirl.contains("Manglik", ignoreCase = true)
                val isNeither = !manglikBoy.contains("Manglik", ignoreCase = true) && !manglikGirl.contains("Manglik", ignoreCase = true)

                val note = when {
                    isBothManglik -> if (hi)
                        "दोनो कुंडलियों में मंगल की स्थिति साम्य होने से मंगल दोष का स्वतः परिहार हो जाता है।"
                    else
                        "Both charts share similar Mars placements, resulting in classical cancellation of Mangal Dosha."
                    isNeither -> if (hi)
                        "दोनों कुंडलियां मंगल दोष से मुक्त हैं, वैवाहिक जीवन में मांगलिक बाधा नहीं है।"
                    else
                        "Both horoscopes are free from Mangal Dosha. No astrological impediment observed."
                    else -> if (hi)
                        "एक कुंडली में आंशिक मंगल प्रभाव है। इसके लिए सरल वैदिक शांति उपाय अथवा कुंभ विवाह अनुष्ठान शुभ रहेगा।"
                    else
                        "One partner shows partial Mangal influence; simple Vedic chanting and remedial prayers are recommended."
                }

                Text(
                    text = "ℹ️ $note",
                    color = TextPrimary.copy(alpha = 0.85f),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ManglikPersonColumn(
    modifier: Modifier = Modifier,
    title: String,
    statusText: String,
    hi: Boolean
) {
    val isManglik = statusText.contains("मांगलिक", ignoreCase = true) || statusText.contains("manglik", ignoreCase = true)
    val color = if (isManglik) WarningAmber else AuspiciousGreen

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CosmicSurface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Component 4d: Vedic Synthesis & Suggestions Numbered List
// -----------------------------------------------------------------------------
@Composable
private fun VedicSuggestionsSection(
    conclusion: String,
    totalScore: Double,
    hi: Boolean
) {
    val suggestions = listOf(
        if (hi) "प्रतिदिन ॐ नमः शिवाय का 108 बार जाप दांपत्य जीवन में सुख और शांति प्रदान करेगा।"
        else "Daily recitation of 'Om Namah Shivaya' creates harmony and peaceful understanding.",
        if (hi) "प्रत्येक पूर्णिमा पर संयुक्त रूप से सत्यनारायण कथा अथवा शिव-पार्वती पूजन करना कल्याणकारी रहेगा।"
        else "Joint worship of Lord Shiva & Goddess Parvati on Poornima bestows marital prosperity.",
        if (hi) "पारस्परिक संवाद में वाणी के संयम और भावनात्मक समझ को सर्वोपरि रखें।"
        else "Prioritize calm communication and mutual emotional respect in daily life decisions.",
        if (hi) "विवाह पूर्व परिवार के वरिष्ठ सदस्यों का आशीर्वाद एवं कुलदेवता पूजन अवश्य करें।"
        else "Seek blessings of elders and perform ancestral deity rituals prior to wedding."
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        glowColor = SacredGold,
        glowAlpha = 0.35f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (hi) "वैदिक परामर्श एवं सुझाव" else "Vedic Guidance & Suggestions",
                color = SacredGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            if (conclusion.isNotBlank()) {
                Text(
                    text = conclusion,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            suggestions.forEachIndexed { index, suggestion ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(SacredGold.copy(alpha = 0.2f))
                            .border(1.dp, SacredGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = SacredGoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = suggestion,
                        color = TextPrimary.copy(alpha = 0.9f),
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
