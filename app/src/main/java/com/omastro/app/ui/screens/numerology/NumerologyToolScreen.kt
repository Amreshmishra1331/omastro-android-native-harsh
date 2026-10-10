package com.omastro.app.ui.screens.numerology

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumerologyToolScreen(
    toolId: String,
    onBack: () -> Unit
) {
    val vm: NumerologyToolViewModel = viewModel(
        key = toolId,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NumerologyToolViewModel(toolId) as T
            }
        }
    )
    val tool = NumerologyTool.fromId(toolId)
    val uiState by vm.uiState.collectAsState()
    // Recompute results when the Settings language changes so the output
    // follows the global language (no per-screen toggle).
    val lang by LangPrefs.language.collectAsState()
    LaunchedEffect(lang) { vm.recalculate() }

    // Pre-fill from the Kundali Numerology tab (birth details carried over),
    // or instantly re-open a locally saved result from My Data (stored JSON,
    // no re-compute).
    LaunchedEffect(Unit) {
        com.omastro.app.ui.navigation.PendingLocalHolder.consumeNumerology()?.let { stored ->
            if (stored.toolId == toolId) {
                vm.loadStoredResult(stored.inputs, stored.result)
            }
        } ?: com.omastro.app.ui.navigation.PendingNumerologyHolder.consume()?.let { (name, dob) ->
            vm.setName(name)
            vm.setDob(dob)
        }
    }

    key(lang) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(W.t("num_tool_${tool.id}_title"), color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = W.t("back"), tint = SacredGold)
                        }
                    },
                    actions = {
                        if (uiState is ToolUiState.Success || uiState is ToolUiState.Error) {
                            IconButton(onClick = { vm.reset() }) {
                                Icon(Icons.Default.Refresh, contentDescription = W.t("num_btn_retry"), tint = SacredGold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
                )
            },
            containerColor = CosmicDeepBlue
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
            ) {
                when (val state = uiState) {
                    is ToolUiState.Idle -> ToolForm(tool, vm)
                    is ToolUiState.Loading -> LoadingView(tool)
                    is ToolUiState.Success -> ToolResultView(state.result)
                    is ToolUiState.Error -> ErrorView(state.message) { vm.reset() }
                }
            }
        }
    }
}

@Composable
private fun ToolForm(tool: NumerologyTool, vm: NumerologyToolViewModel) {
    val name by vm.name.collectAsState()
    val dob by vm.dob.collectAsState()
    val name2 by vm.name2.collectAsState()
    val dob2 by vm.dob2.collectAsState()
    val phone by vm.phone.collectAsState()
    val gender by vm.gender.collectAsState()
    val formError by vm.formError.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = W.t("num_tool_${tool.id}_title"), color = StarlightWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = W.t("num_tool_${tool.id}_sub"), color = TextSecondary, fontSize = 13.sp)

        CosmicCard(borderGold = true, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (vm.needsName()) {
                    OutlinedTextField(
                        value = name, onValueChange = vm::setName,
                        label = { Text(W.t("num_form_name")) }, placeholder = { Text(W.t("num_form_name_hint")) },
                        singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                }
                if (vm.needsDob()) {
                    OutlinedTextField(
                        value = dob, onValueChange = vm::setDob,
                        label = { Text(W.t("num_form_dob")) }, placeholder = { Text(W.t("num_form_dob_hint")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                }
                if (vm.needsSecondPerson()) {
                    HorizontalDivider(color = CosmicBorder)
                    Text(W.t("num_form_second"), color = SacredGoldLight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = name2, onValueChange = vm::setName2,
                        label = { Text(W.t("num_form_name")) }, singleLine = true,
                        colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dob2, onValueChange = vm::setDob2,
                        label = { Text(W.t("num_form_dob")) }, placeholder = { Text(W.t("num_form_dob_hint_short")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                }
                if (vm.needsPhone()) {
                    OutlinedTextField(
                        value = phone, onValueChange = vm::setPhone,
                        label = { Text(W.t("num_form_phone")) }, placeholder = { Text(W.t("num_form_phone_hint")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                }
                if (vm.needsGender()) {
                    Text(W.t("num_form_gender"), color = TextSecondary, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("male" to W.t("num_form_male"), "female" to W.t("num_form_female")).forEach { (v, label) ->
                            FilterChip(
                                selected = gender == v,
                                onClick = { vm.setGender(v) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SacredGold,
                                    selectedLabelColor = CosmicDeepBlue
                                )
                            )
                        }
                    }
                }
            }
        }

        formError?.let {
            Text(text = it, color = InauspiciousRed, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = { vm.calculate() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(W.t("num_btn_calculate"), color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Text(
            text = W.t("num_form_footer"),
            color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LoadingView(tool: NumerologyTool) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = SacredGold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("${W.t("num_loading")} ${W.t("num_tool_${tool.id}_title").lowercase()}…", color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun ToolResultView(result: ToolResult) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        result.sections.forEach { section ->
            CosmicCard(borderGold = false, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = section.heading.uppercase(),
                        color = SacredGoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    section.lines.forEach { line ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (line.label.isNotBlank()) {
                                Text(
                                    text = line.label,
                                    color = TextSecondary, fontSize = 12.sp
                                )
                            }
                            Text(
                                text = line.value,
                                color = if (line.highlight) SacredGold else StarlightWhite.copy(alpha = 0.92f),
                                fontSize = if (line.highlight) 15.sp else 13.5.sp,
                                fontWeight = if (line.highlight) FontWeight.Bold else FontWeight.Normal,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = InauspiciousRed, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, color = StarlightWhite, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(W.t("num_btn_retry"), color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SacredGold,
    unfocusedBorderColor = CosmicBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = SacredGoldLight,
    unfocusedLabelColor = TextSecondary
)
