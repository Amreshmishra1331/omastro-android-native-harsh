package com.omastro.app.ui.screens.vastu

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.PendingPhotoContextHolder
import com.omastro.app.data.model.PhotoContextAttachment
import com.omastro.app.data.model.VASTU_DIRECTIONS
import com.omastro.app.data.model.VASTU_ROOM_TYPES
import com.omastro.app.data.model.VastuAnalysis
import com.omastro.app.data.model.VastuAnalyzeRequest
import com.omastro.app.data.model.VastuDosha
import com.omastro.app.data.model.VastuPadaInfo
import com.omastro.app.data.model.VastuRoomInput
import com.omastro.app.data.repository.VisionRepository
import com.omastro.app.data.local.ReadingFeatureType
import com.omastro.app.data.repository.ReadingHistoryRepository
import com.omastro.app.ui.screens.aichat.copyUriToCache
import com.omastro.app.ui.screens.aichat.rememberPermissionRequest
import com.omastro.app.ui.screens.aichat.saveBitmapToCache
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.share.ReadingCardShareButton
import com.omastro.app.ui.share.shareCardContent
import com.omastro.app.ui.theme.CosmicCharcoal
import com.omastro.app.ui.theme.CosmicDeepBlue
import com.omastro.app.ui.theme.CosmicNavy
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ------------------------------------------------------------------
// ViewModel
// ------------------------------------------------------------------

data class VastuRoomEntry(
    val type: String = "living_room",
    val direction: String = "E",
    val id: String = java.util.UUID.randomUUID().toString()
)

data class VastuUiState(
    val isLoading: Boolean = false,
    val analysis: VastuAnalysis? = null,
    val error: String? = null,
    val padaReference: List<VastuPadaInfo> = emptyList(),
    /** `photo_parse` from /vastu/analyze-photo (parsed house-map inputs). */
    val photoParse: Map<String, Any?> = emptyMap()
)

class VastuViewModel : ViewModel() {
    private val repo = VisionRepository()
    private val _uiState = MutableStateFlow(VastuUiState())
    val uiState: StateFlow<VastuUiState> = _uiState

    /** Last manual-form request — included in the AI context block. */
    private var lastRequest: VastuAnalyzeRequest? = null

    /** Notes sent with the last photo analysis — included in AI context. */
    private var lastPhotoNotes: String? = null

    fun lastAnalyzedRequest(): VastuAnalyzeRequest? = lastRequest
    fun lastAnalyzedPhotoNotes(): String? = lastPhotoNotes

    fun loadReference() {
        viewModelScope.launch {
            repo.getVastuReference().onSuccess { padas ->
                _uiState.value = _uiState.value.copy(padaReference = padas)
            }
        }
    }

    fun analyze(request: VastuAnalyzeRequest, appContext: Context) {
        lastRequest = request
        lastPhotoNotes = null
        _uiState.value = _uiState.value.copy(
            isLoading = true, error = null, analysis = null, photoParse = emptyMap()
        )
        viewModelScope.launch {
            val result = repo.analyzeVastu(request)
            result
                .onSuccess { (analysis, _) ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, analysis = analysis
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: W.t("vastu_err_failed")
                    )
                }
            // Auto-save completed analyses to local history, verbatim —
            // including null-score "insufficient data" states. Manual
            // form has no photo, so no thumbnail is stored.
            val (_, rawJson) = result.getOrNull() ?: return@launch
            runCatching {
                ReadingHistoryRepository(appContext).save(
                    featureType = ReadingFeatureType.VASTU,
                    resultJson = rawJson,
                    photo = null
                )
            }
        }
    }

    fun analyzePhoto(photo: File, notes: String?, appContext: Context) {
        lastRequest = null
        lastPhotoNotes = notes?.takeIf { it.isNotBlank() }
        _uiState.value = _uiState.value.copy(
            isLoading = true, error = null, analysis = null, photoParse = emptyMap()
        )
        viewModelScope.launch {
            val result = repo.analyzeVastuPhoto(photo, notes)
            result
                .onSuccess { (photoParse, analysis, _) ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, analysis = analysis, photoParse = photoParse
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: W.t("vastu_photo_err_failed")
                    )
                }
            // Auto-save completed photo analyses to local history,
            // with a downscaled thumbnail of the house-map photo.
            val (_, _, rawJson) = result.getOrNull() ?: return@launch
            runCatching {
                ReadingHistoryRepository(appContext).save(
                    featureType = ReadingFeatureType.VASTU,
                    resultJson = rawJson,
                    photo = photo
                )
            }
        }
    }

    fun reset() {
        lastRequest = null
        lastPhotoNotes = null
        _uiState.value = _uiState.value.copy(
            analysis = null, error = null, photoParse = emptyMap()
        )
    }
}

// ------------------------------------------------------------------
// Screen
// ------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VastuScreen(
    onBack: () -> Unit,
    onOpenAiChat: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: VastuViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(0) } // 0 = Manual, 1 = Photo
    // Photo captured in Compass Mode, handed to the house-map photo tab.
    var compassPhotoFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    // Collect global language so the whole screen auto-switches EN/हिन्दी.
    val lang by LangPrefs.language.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadReference() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            W.t("vastu_title"),
                            color = StarlightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            W.t("vastu_sub"),
                            color = StarlightWhite.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = W.t("back"),
                            tint = SacredGold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = W.t("history_open_cd"),
                            tint = SacredGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mode tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CosmicNavy)
                    .padding(4.dp)
            ) {
                listOf(W.t("vastu_tab_manual"), W.t("vastu_tab_photo")).forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (tab == i) SacredGold.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { tab = i; viewModel.reset() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (tab == i) SacredGold else StarlightWhite.copy(alpha = 0.6f),
                            fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }

            if (tab == 0) {
                VastuManualForm(
                    padaReference = uiState.padaReference,
                    isLoading = uiState.isLoading,
                    onAnalyze = { viewModel.analyze(it, context.applicationContext) },
                    onCompassPhoto = { file ->
                        compassPhotoFile = file
                        tab = 1
                    }
                )
            } else {
                VastuPhotoForm(
                    isLoading = uiState.isLoading,
                    prefillPhoto = compassPhotoFile,
                    onPhotoConsumed = { compassPhotoFile = null },
                    onAnalyze = { file, notes ->
                        viewModel.analyzePhoto(file, notes, context.applicationContext)
                    }
                )
            }

            // Result / error below the form
            if (uiState.isLoading && uiState.analysis == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SacredGold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            W.t("vastu_analyzing"),
                            color = StarlightWhite.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
            uiState.analysis?.let { analysis ->
                VastuResultView(
                    analysis = analysis,
                    onAskAi = {
                        // FULL analysis context → AI chat: score, inputs, every
                        // verdict, every dosha + remedies. AI can explain
                        // anything in the report.
                        val block = analysis.toFullContextBlock(
                            request = viewModel.lastAnalyzedRequest(),
                            photoParse = uiState.photoParse,
                            photoNotes = viewModel.lastAnalyzedPhotoNotes()
                        )
                        if (block.isNotBlank()) {
                            PendingPhotoContextHolder.set(
                                PhotoContextAttachment(
                                    label = "${W.t("vastu_ctx_label")} (${analysis.score?.let { "$it/100" } ?: "—"})",
                                    contextBlock = block
                                )
                            )
                        }
                        onOpenAiChat()
                    },
                    onNew = { viewModel.reset() }
                )
            }
            uiState.error?.let { err ->
                Text(
                    err,
                    color = Color(0xFFFF8A80),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// Manual form
// ------------------------------------------------------------------

@Composable
private fun VastuManualForm(
    padaReference: List<VastuPadaInfo>,
    isLoading: Boolean,
    onAnalyze: (VastuAnalyzeRequest) -> Unit,
    onCompassPhoto: (File) -> Unit
) {
    var facing by remember { mutableStateOf("E") }
    var padaText by remember { mutableStateOf("") }
    var plotShape by remember { mutableStateOf("rectangle") }
    var plotLength by remember { mutableStateOf("") }
    var plotBreadth by remember { mutableStateOf("") }
    var slope by remember { mutableStateOf("") }
    var road by remember { mutableStateOf("") }
    var water by remember { mutableStateOf("") }
    var tank by remember { mutableStateOf("") }
    var septic by remember { mutableStateOf("") }
    var staircase by remember { mutableStateOf("") }
    var rooms by remember { mutableStateOf(listOf(VastuRoomEntry())) }
    var showCompass by remember { mutableStateOf(false) }

    val padaInfo = padaText.toIntOrNull()?.let { n ->
        padaReference.find { it.pada == n }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VastuCard(title = W.t("vastu_card_facing")) {
            DirectionPicker(
                label = W.t("vastu_facing_label"),
                value = facing,
                onChange = { facing = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            // Live compass: point the phone at the entrance to detect direction.
            OutlinedButton(
                onClick = { showCompass = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SacredGold),
                border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.6f))
            ) {
                Icon(
                    Icons.Default.Explore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    W.t("vastu_compass_open"),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = padaText,
                onValueChange = { padaText = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text(W.t("vastu_pada_label")) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = vastuFieldColors()
            )
            if (padaInfo != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Pada ${padaInfo.pada}: ${padaInfo.devata} (${padaInfo.direction}) — ${padaInfo.verdict}",
                    color = SacredGold,
                    fontSize = 12.5.sp
                )
                if (padaInfo.effect.isNotBlank()) {
                    Text(
                        padaInfo.effect,
                        color = StarlightWhite.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                }
            } else if (padaText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    W.t("vastu_pada_hint"),
                    color = StarlightWhite.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            }
        }

        VastuCard(title = W.t("vastu_card_plot")) {
            DirectionPicker(
                label = W.t("vastu_plot_shape"),
                value = plotShape,
                options = listOf("square", "rectangle", "irregular", "L-shaped", "triangular"),
                nameOf = { W.plotShapeName(it) },
                onChange = { plotShape = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = plotLength,
                    onValueChange = { plotLength = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(W.t("vastu_length")) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = vastuFieldColors()
                )
                OutlinedTextField(
                    value = plotBreadth,
                    onValueChange = { plotBreadth = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(W.t("vastu_breadth")) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = vastuFieldColors()
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            DirectionPicker(label = W.t("vastu_slope"), value = slope, allowEmpty = true, onChange = { slope = it })
            Spacer(modifier = Modifier.height(10.dp))
            DirectionPicker(label = W.t("vastu_road"), value = road, allowEmpty = true, onChange = { road = it })
        }

        VastuCard(title = W.t("vastu_card_water")) {
            DirectionPicker(label = W.t("vastu_water"), value = water, allowEmpty = true, onChange = { water = it })
            Spacer(modifier = Modifier.height(10.dp))
            DirectionPicker(label = W.t("vastu_tank"), value = tank, allowEmpty = true, onChange = { tank = it })
            Spacer(modifier = Modifier.height(10.dp))
            DirectionPicker(label = W.t("vastu_septic"), value = septic, allowEmpty = true, onChange = { septic = it })
            Spacer(modifier = Modifier.height(10.dp))
            DirectionPicker(label = W.t("vastu_staircase"), value = staircase, allowEmpty = true, onChange = { staircase = it })
        }

        VastuCard(title = W.t("vastu_card_rooms")) {
            rooms.forEach { room ->
                VastuRoomRow(
                    room = room,
                    onTypeChange = { t ->
                        rooms = rooms.map { if (it.id == room.id) it.copy(type = t) else it }
                    },
                    onDirChange = { d ->
                        rooms = rooms.map { if (it.id == room.id) it.copy(direction = d) else it }
                    },
                    onRemove = {
                        rooms = rooms.filter { it.id != room.id }
                    },
                    canRemove = rooms.size > 1
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = { rooms = rooms + VastuRoomEntry() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicCharcoal,
                    contentColor = SacredGold
                )
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(W.t("vastu_add_room"))
            }
        }

        Button(
            onClick = {
                onAnalyze(
                    VastuAnalyzeRequest(
                        facing = facing,
                        entrancePada = padaText.toIntOrNull(),
                        plotShape = plotShape,
                        plotLength = plotLength.toDoubleOrNull(),
                        plotBreadth = plotBreadth.toDoubleOrNull(),
                        slope = slope.ifBlank { null },
                        road = road.ifBlank { null },
                        waterSource = water.ifBlank { null },
                        overheadTank = tank.ifBlank { null },
                        septicTank = septic.ifBlank { null },
                        staircase = staircase.ifBlank { null },
                        rooms = rooms.map { VastuRoomInput(it.type, it.direction) }
                    )
                )
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = SacredGold,
                contentColor = Color(0xFF1A1005)
            )
        ) {
            Text(W.t("vastu_analyze"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
    } // end form Column

        // Compass Mode overlay — full-screen live compass + camera.
        if (showCompass) {
            CompassModeScreen(
                onUseDirection = { dir ->
                    facing = dir
                    showCompass = false
                },
                onUsePhoto = { file ->
                    showCompass = false
                    onCompassPhoto(file)
                },
                onBack = { showCompass = false }
            )
        }
    }
}

@Composable
private fun VastuCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .padding(16.dp)
    ) {
        Text(title, color = SacredGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun vastuFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = StarlightWhite,
    unfocusedTextColor = StarlightWhite,
    focusedLabelColor = SacredGold,
    unfocusedLabelColor = StarlightWhite.copy(alpha = 0.55f),
    cursorColor = SacredGold,
    focusedBorderColor = SacredGold,
    unfocusedBorderColor = StarlightWhite.copy(alpha = 0.25f)
)

@Composable
private fun DirectionPicker(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    options: List<String> = VASTU_DIRECTIONS,
    allowEmpty: Boolean = false,
    nameOf: (String) -> String = { opt -> "${W.dirName(opt)} ($opt)" }
) {
    var expanded by remember { mutableStateOf(false) }
    val display = when {
        value.isBlank() && allowEmpty -> W.t("vastu_not_set")
        else -> nameOf(value)
    }
    Column {
        Text(label, color = StarlightWhite.copy(alpha = 0.65f), fontSize = 12.5.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, StarlightWhite.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(display, color = StarlightWhite, fontSize = 14.sp)
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = SacredGold
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(CosmicCharcoal)
            ) {
                if (allowEmpty) {
                    DropdownMenuItem(
                        text = { Text(W.t("vastu_not_set"), color = StarlightWhite.copy(alpha = 0.6f)) },
                        onClick = { onChange(""); expanded = false }
                    )
                }
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(nameOf(opt), color = StarlightWhite) },
                        onClick = { onChange(opt); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun VastuRoomRow(
    room: VastuRoomEntry,
    onTypeChange: (String) -> Unit,
    onDirChange: (String) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var typeExpanded by remember { mutableStateOf(false) }
        var dirExpanded by remember { mutableStateOf(false) }
        val typeLabel = W.roomName(room.type)

        // Type picker
        Box(modifier = Modifier.weight(1.4f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, StarlightWhite.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .clickable { typeExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(typeLabel, color = StarlightWhite, fontSize = 13.sp, maxLines = 1)
                Icon(Icons.Default.ArrowDropDown, null, tint = SacredGold, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false },
                modifier = Modifier
                    .background(CosmicCharcoal)
                    .heightIn(max = 320.dp)
            ) {
                VASTU_ROOM_TYPES.forEach { (value, _) ->
                    DropdownMenuItem(
                        text = { Text(W.roomName(value), color = StarlightWhite, fontSize = 13.sp) },
                        onClick = { onTypeChange(value); typeExpanded = false }
                    )
                }
            }
        }
        // Direction picker
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, StarlightWhite.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .clickable { dirExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${W.dirName(room.direction)} (${room.direction})",
                    color = StarlightWhite,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Icon(Icons.Default.ArrowDropDown, null, tint = SacredGold, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = dirExpanded,
                onDismissRequest = { dirExpanded = false },
                modifier = Modifier.background(CosmicCharcoal)
            ) {
                (VASTU_DIRECTIONS + "C").forEach { d ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${W.dirName(d)} ($d)",
                                color = StarlightWhite,
                                fontSize = 13.sp
                            )
                        },
                        onClick = { onDirChange(d); dirExpanded = false }
                    )
                }
            }
        }
        if (canRemove) {
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, W.t("vastu_remove_room_cd"), tint = Color(0xFFFF8A80))
            }
        }
    }
}

// ------------------------------------------------------------------
// Photo form
// ------------------------------------------------------------------

@Composable
private fun VastuPhotoForm(
    isLoading: Boolean,
    prefillPhoto: File? = null,
    onPhotoConsumed: () -> Unit = {},
    onAnalyze: (File, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var notes by remember { mutableStateOf("") }

    // Photo handed over from Compass Mode (direction stamped on it).
    LaunchedEffect(prefillPhoto) {
        if (prefillPhoto != null && prefillPhoto.exists()) {
            imageFile = prefillPhoto
            previewBitmap = withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(prefillPhoto.absolutePath)
                } catch (_: Exception) {
                    null
                }
            }
            onPhotoConsumed()
        }
    }

    val cameraPermission = rememberPermissionRequest(
        permission = Manifest.permission.CAMERA,
        title = W.t("vastu_cam_title"),
        rationale = W.t("vastu_cam_rationale")
    )
    val galleryPermission = rememberPermissionRequest(
        permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE,
        title = W.t("vastu_lib_title"),
        rationale = W.t("vastu_lib_rationale")
    )
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            scope.launch {
                val f = saveBitmapToCache(context, bitmap)
                if (f != null) {
                    imageFile = f
                    previewBitmap = bitmap
                } else {
                    snackbarHostState.showSnackbar(W.t("vastu_photo_err_save"))
                }
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val f = copyUriToCache(context, uri, "vastu")
                if (f != null) {
                    imageFile = f
                    previewBitmap = null
                } else {
                    snackbarHostState.showSnackbar(W.t("vastu_photo_err_read"))
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        androidx.compose.material3.SnackbarHost(hostState = snackbarHostState)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .padding(18.dp)
        ) {
            Text(
                W.t("vastu_photo_title"),
                color = StarlightWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                W.t("vastu_photo_sub"),
                color = StarlightWhite.copy(alpha = 0.65f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = W.t("vastu_photo_map_cd"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(14.dp))
            } else if (imageFile != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicCharcoal),
                    contentAlignment = Alignment.Center
                ) {
                    Text(W.t("vastu_photo_ready"), color = SacredGold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        cameraPermission.request(context) { granted ->
                            if (granted) cameraLauncher.launch()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicCharcoal,
                        contentColor = StarlightWhite
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(W.t("palm_camera"))
                }
                Button(
                    onClick = {
                        galleryPermission.request(context) { granted ->
                            if (granted) galleryLauncher.launch("image/*")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicCharcoal,
                        contentColor = StarlightWhite
                    )
                ) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(W.t("palm_gallery"))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(W.t("vastu_notes_label")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = vastuFieldColors()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val f = imageFile
                    if (f != null) onAnalyze(f, notes.ifBlank { null })
                },
                enabled = imageFile != null && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SacredGold,
                    contentColor = Color(0xFF1A1005)
                )
            ) {
                Text(W.t("vastu_analyze_map"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------------
// Result view
// ------------------------------------------------------------------

@Composable
internal fun VastuResultView(
    analysis: VastuAnalysis,
    onAskAi: () -> Unit,
    onNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Score dial
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicNavy)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VastuScoreDial(score = analysis.score)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    analysis.grade.ifBlank { gradeFor(analysis.score) },
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DoshaCountChip(W.t("vastu_count_high"), analysis.highCount, Color(0xFFFF5252))
                    DoshaCountChip(W.t("vastu_count_medium"), analysis.mediumCount, Color(0xFFFFB74D))
                    DoshaCountChip(W.t("vastu_count_low"), analysis.lowCount, SacredGold)
                }
            }
        }

        // Doshas by severity
        if (analysis.doshas.isNotEmpty()) {
            VastuCard(title = W.t("vastu_doshas_found")) {
                analysis.doshas.forEach { dosha ->
                    VastuDoshaCard(dosha = dosha)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        } else {
            VastuCard(title = W.t("vastu_doshas")) {
                Text(
                    W.t("vastu_no_doshas"),
                    color = Color(0xFF81C784),
                    fontSize = 13.5.sp
                )
            }
        }

        // Direction / placement checks
        if (analysis.checks.isNotEmpty()) {
            VastuCard(title = W.t("vastu_verdicts")) {
                analysis.checks.forEach { check ->
                    VastuCheckRow(check = check)
                }
            }
        }

        if (analysis.disclaimer.isNotBlank()) {
            Text(
                analysis.disclaimer,
                color = StarlightWhite.copy(alpha = 0.5f),
                fontSize = 11.5.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onNew,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicCharcoal,
                    contentColor = StarlightWhite
                )
            ) {
                Text(W.t("vastu_new"))
            }
            Button(
                onClick = onAskAi,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SacredGold,
                    contentColor = Color(0xFF1A1005)
                )
            ) {
                Text(W.t("vastu_ask_ai"), fontWeight = FontWeight.Bold)
            }
        }
        // Shareable card from the REAL server analysis. The button hides
        // itself when there is nothing meaningful to share; a null score
        // is rendered honestly as "insufficient data", never faked.
        ReadingCardShareButton(content = analysis.shareCardContent())
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun gradeFor(score: Int?): String = when {
    score == null -> W.t("vastu_insufficient_data")
    score >= 85 -> W.t("vastu_grade_85")
    score >= 70 -> W.t("vastu_grade_70")
    score >= 50 -> W.t("vastu_grade_50")
    else -> W.t("vastu_grade_low")
}

@Composable
private fun VastuScoreDial(score: Int?) {
    val sweep = if (score == null) 0f else (score.coerceIn(0, 100) / 100f) * 270f
    val arcColor = when {
        score == null -> StarlightWhite.copy(alpha = 0.30f)
        score >= 85 -> Color(0xFF81C784)
        score >= 70 -> SacredGold
        score >= 50 -> Color(0xFFFFB74D)
        else -> Color(0xFFFF5252)
    }
    Box(
        modifier = Modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 18f, cap = StrokeCap.Round)
            )
            drawArc(
                color = arcColor,
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 18f, cap = StrokeCap.Round)
            )
        }
        if (score == null) {
            // Honest unknown state: never show a fake 0/100.
            Icon(
                Icons.Filled.QuestionMark,
                contentDescription = W.t("vastu_insufficient_data"),
                tint = StarlightWhite.copy(alpha = 0.75f),
                modifier = Modifier.size(52.dp)
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$score",
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp
                )
                Text(
                    "/ 100",
                    color = StarlightWhite.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DoshaCountChip(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "$label: $count",
            color = StarlightWhite.copy(alpha = 0.8f),
            fontSize = 12.5.sp
        )
    }
}

@Composable
private fun VastuDoshaCard(dosha: VastuDosha) {
    val severityColor = when (dosha.severity.lowercase()) {
        "high" -> Color(0xFFFF5252)
        "medium" -> Color(0xFFFFB74D)
        else -> SacredGold
    }
    val severityLabel = when (dosha.severity.lowercase()) {
        "high" -> W.t("vastu_sev_high")
        "medium" -> W.t("vastu_sev_medium")
        else -> W.t("vastu_sev_low")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicCharcoal)
            .border(1.dp, severityColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                dosha.title.ifBlank { W.t("vastu_dosha_fallback") },
                color = StarlightWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(severityColor.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    severityLabel,
                    color = severityColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (dosha.detail.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(dosha.detail, color = StarlightWhite.copy(alpha = 0.75f), fontSize = 12.5.sp)
        }
        if (dosha.rule.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${W.t("vastu_rule")}: ${dosha.rule}",
                color = SacredGold.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
        if (dosha.remedies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                W.t("vastu_remedies"),
                color = StarlightWhite.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            dosha.remedies.forEach { remedy ->
                Row(modifier = Modifier.padding(top = 3.dp)) {
                    Text("• ", color = SacredGold, fontSize = 12.5.sp)
                    Text(remedy, color = StarlightWhite.copy(alpha = 0.8f), fontSize = 12.5.sp)
                }
            }
        }
    }
}

@Composable
private fun VastuCheckRow(check: com.omastro.app.data.model.VastuCheck) {
    val verdictColor = when (check.verdict.lowercase()) {
        "ideal", "auspicious" -> Color(0xFF81C784)
        "acceptable" -> SacredGold
        "needs_care" -> Color(0xFFFFB74D)
        "dosha", "inauspicious" -> Color(0xFFFF5252)
        else -> StarlightWhite.copy(alpha = 0.5f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(check.title, color = StarlightWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (check.value.isNotBlank()) {
                Text(
                    check.value,
                    color = StarlightWhite.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(verdictColor.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                check.verdict.replace('_', ' ').replaceFirstChar { it.uppercase() },
                color = verdictColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
