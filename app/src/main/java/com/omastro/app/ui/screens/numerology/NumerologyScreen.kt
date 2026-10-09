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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumerologyScreen(
    onBack: () -> Unit,
    viewModel: NumerologyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Numerology Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
                    }
                },
                actions = {
                    if (uiState is NumerologyUiState.Success || uiState is NumerologyUiState.Error) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "New calculation", tint = SacredGold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            when (val state = uiState) {
                is NumerologyUiState.Idle -> NumerologyForm(viewModel)
                is NumerologyUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = SacredGold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Computing your numerology profile…", color = TextSecondary, fontSize = 14.sp)
                    }
                }
                is NumerologyUiState.Success -> NumerologyResult(state.analysisText)
                is NumerologyUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = InauspiciousRed, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(state.message, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.reset() },
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Try Again", color = CosmicDeepBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumerologyForm(viewModel: NumerologyViewModel) {
    val name by viewModel.name.collectAsState()
    val dob by viewModel.dob.collectAsState()
    val formError by viewModel.formError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Vedic Numerology",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your Mulank, Bhagyank and name vibrations are computed from your details.",
            color = TextSecondary,
            fontSize = 13.sp
        )

        CosmicCard(borderGold = true, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = viewModel::setName,
                    label = { Text("Full Name") },
                    placeholder = { Text("e.g. Rahul Sharma") },
                    singleLine = true,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dob,
                    onValueChange = viewModel::setDob,
                    label = { Text("Date of Birth") },
                    placeholder = { Text("YYYY-MM-DD  e.g. 1995-08-15") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        formError?.let {
            Text(text = it, color = InauspiciousRed, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = { viewModel.calculate() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Calculate Profile", color = CosmicDeepBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun NumerologyResult(analysisText: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CosmicCard(borderGold = true, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = analysisText.trim(),
                color = StarlightWhite.copy(alpha = 0.92f),
                fontSize = 13.5.sp,
                lineHeight = 20.sp
            )
        }
        Text(
            text = "This profile is computed from your name and birth date by the OmAstro engine.",
            color = TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
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
