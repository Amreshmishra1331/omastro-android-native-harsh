package com.omastro.app.ui.screens.matching

import com.omastro.app.ui.i18n.W
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.GunaDetail
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.components.DatePickerField
import com.omastro.app.ui.components.TimePickerField
import com.omastro.app.ui.components.PlaceSearchField
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchmakingScreen(
    onBack: () -> Unit,
    viewModel: MatchmakingViewModel = viewModel()
) {
    val result by viewModel.result.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showForm by remember { mutableStateOf(true) }

    // Cycle 4: blank-form submit no longer silently no-ops — missing fields
    // are highlighted with bilingual error text.
    var fieldErrors by remember { mutableStateOf(setOf<String>()) }
    var formError by remember { mutableStateOf<String?>(null) }

    // Birth detail inputs
    var boyName by remember { mutableStateOf("") }
    var boyDob by remember { mutableStateOf("") }
    var boyTob by remember { mutableStateOf("") }
    var boyPob by remember { mutableStateOf("") }
    var girlName by remember { mutableStateOf("") }
    var girlDob by remember { mutableStateOf("") }
    var girlTob by remember { mutableStateOf("") }
    var girlPob by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(W.t("match_title"), color = StarlightWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SacredGold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(W.t("match_loading"), color = StarlightWhite, fontSize = 13.sp)
                    }
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = W.t("match_failed"),
                            color = StarlightWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: W.t("match_failed_sub"),
                            color = StarlightWhite.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                viewModel.clearError()
                                showForm = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                        ) {
                            Text(W.t("Try Again"), color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            result == null || showForm -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    if (result == null) {
                        item {
                            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = SacredGold,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = W.t("match_intro"),
                                        color = StarlightWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = W.t("match_intro_sub"),
                                        color = StarlightWhite.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    // Groom details
                    item {
                        Text(W.t("match_groom"), color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    item {
                        BirthInputField(
                            W.t("match_full_name"), boyName,
                            { boyName = it; fieldErrors = fieldErrors - "boyName"; formError = null },
                            isError = fieldErrors.contains("boyName")
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.weight(1f)) {
                                DatePickerField(boyDob, { boyDob = it; fieldErrors = fieldErrors - "boyDob"; formError = null }, W.t("match_dob"))
                            }
                            Box(Modifier.weight(1f)) {
                                TimePickerField(boyTob, { boyTob = it; fieldErrors = fieldErrors - "boyTob"; formError = null }, W.t("match_tob"))
                            }
                        }
                    }
                    item {
                        PlaceSearchField(
                            value = boyPob,
                            onValueChange = { boyPob = it; fieldErrors = fieldErrors - "boyPob"; formError = null },
                            onPlaceSelected = { fieldErrors = fieldErrors - "boyPob"; formError = null /* server geocodes the place name */ },
                            label = W.t("match_pob"),
                            placeholder = W.t("match_pob_ph")
                        )
                        if (fieldErrors.contains("boyPob")) {
                            Text(
                                text = W.t("field_required"),
                                color = Color(0xFFFF8A80),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    // Bride details
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(W.t("match_bride"), color = SacredGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    item {
                        BirthInputField(
                            W.t("match_full_name"), girlName,
                            { girlName = it; fieldErrors = fieldErrors - "girlName"; formError = null },
                            isError = fieldErrors.contains("girlName")
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.weight(1f)) {
                                DatePickerField(girlDob, { girlDob = it; fieldErrors = fieldErrors - "girlDob"; formError = null }, W.t("match_dob"))
                            }
                            Box(Modifier.weight(1f)) {
                                TimePickerField(girlTob, { girlTob = it; fieldErrors = fieldErrors - "girlTob"; formError = null }, W.t("match_tob"))
                            }
                        }
                    }
                    item {
                        PlaceSearchField(
                            value = girlPob,
                            onValueChange = { girlPob = it; fieldErrors = fieldErrors - "girlPob"; formError = null },
                            onPlaceSelected = { fieldErrors = fieldErrors - "girlPob"; formError = null /* server geocodes the place name */ },
                            label = W.t("match_pob"),
                            placeholder = W.t("match_pob_ph")
                        )
                        if (fieldErrors.contains("girlPob")) {
                            Text(
                                text = W.t("field_required"),
                                color = Color(0xFFFF8A80),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        formError?.let { err ->
                            Text(
                                text = err,
                                color = Color(0xFFFF8A80),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }
                        Button(
                            onClick = {
                                formError = null
                                // Cycle 4: validate every field — missing fields are
                                // highlighted in red with a bilingual message instead
                                // of the button silently doing nothing.
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
                                    formError = W.t("match_err_missing")
                                    return@Button
                                }
                                showForm = false
                                viewModel.calculate(boyName, boyDob, boyTob, boyPob, girlName, girlDob, girlTob, girlPob)
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Text(W.t("Calculate Compatibility"), color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
            else -> {
                val res = result!!
                // Guard: never display sentinel defaults as a real verdict.
                if (!res.hasRealData) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        item {
                            CosmicCard(modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = W.t("match_incomplete"),
                                        color = SacredGold,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = W.t("match_incomplete_sub"),
                                        color = StarlightWhite.copy(alpha = 0.7f),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // Score Badge Card
                    item {
                        CosmicCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Compatibility",
                                    tint = RadiantAmber,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${res.totalScore} / ${res.maxScore}",
                                    color = SacredGold,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = res.verdict,
                                    color = StarlightWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = W.tf("match_compat_fmt", res.percentage),
                                    color = MysticPurple,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Manglik Dosha Status Card
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CosmicCard(modifier = Modifier.weight(1f)) {
                                Column {
                                    Text(W.t("match_groom_short"), color = SacredGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(res.manglikBoy, color = StarlightWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            CosmicCard(modifier = Modifier.weight(1f)) {
                                Column {
                                    Text(W.t("match_bride_short"), color = SacredGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(res.manglikGirl, color = StarlightWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Ashtakoota Guna Breakdown
                    item {
                        Text(
                            text = W.t("match_koota_title"),
                            color = StarlightWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(res.gunas) { guna ->
                        GunaRow(guna)
                    }

                    // Vedic Synthesis Conclusion
                    item {
                        CosmicCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = SacredGold.copy(alpha = 0.5f)
                        ) {
                            Column {
                                Text(W.t("match_synthesis"), color = SacredGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = res.conclusion,
                                    color = StarlightWhite,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearResult()
                                showForm = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SacredGold)
                        ) {
                            Text(W.t("Check Another Pair"), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                } // end hasRealData else — real result rendered above
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = StarlightWhite.copy(alpha = 0.6f), fontSize = 12.sp) },
        supportingText = if (isError) {
            { Text(W.t("field_required"), color = Color(0xFFFF8A80), fontSize = 11.sp) }
        } else null,
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SacredGold,
            unfocusedBorderColor = StarlightWhite.copy(alpha = 0.3f),
            errorBorderColor = Color(0xFFFF8A80),
            focusedTextColor = StarlightWhite,
            unfocusedTextColor = StarlightWhite,
            cursorColor = SacredGold
        )
    )
}

@Composable
private fun GunaRow(guna: GunaDetail) {
    CosmicCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = guna.name, color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = guna.description, color = StarlightWhite.copy(alpha = 0.6f), fontSize = 11.sp)
            }
            Text(
                text = "${guna.obtained} / ${guna.maximum}",
                color = SacredGold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
