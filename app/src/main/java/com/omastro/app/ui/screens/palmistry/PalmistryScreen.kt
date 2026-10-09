package com.omastro.app.ui.screens.palmistry

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omastro.app.data.model.PalmistryReading
import com.omastro.app.data.model.PendingPhotoContextHolder
import com.omastro.app.data.model.PhotoContextAttachment
import com.omastro.app.data.repository.VisionRepository
import com.omastro.app.ui.screens.aichat.copyUriToCache
import com.omastro.app.ui.screens.aichat.rememberPermissionRequest
import com.omastro.app.ui.screens.aichat.saveBitmapToCache
import com.omastro.app.ui.theme.CosmicCharcoal
import com.omastro.app.ui.theme.CosmicDeepBlue
import com.omastro.app.ui.theme.CosmicNavy
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite
import java.io.File
import kotlinx.coroutines.launch

data class PalmistryUiState(
    val isLoading: Boolean = false,
    val reading: PalmistryReading? = null,
    val error: String? = null
)

class PalmistryViewModel : ViewModel() {
    private val repo = VisionRepository()
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(PalmistryUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<PalmistryUiState> = _uiState

    fun analyze(image: File, hand: String) {
        _uiState.value = PalmistryUiState(isLoading = true)
        viewModelScope.launch {
            repo.analyzePalmistry(image, hand = hand)
                .onSuccess { reading ->
                    _uiState.value = PalmistryUiState(reading = reading)
                }
                .onFailure { e ->
                    _uiState.value = PalmistryUiState(
                        error = e.message ?: "Palm analysis failed. Please try again."
                    )
                }
        }
    }

    fun reset() {
        _uiState.value = PalmistryUiState()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalmistryScreen(
    onBack: () -> Unit,
    onOpenAiChat: () -> Unit,
    viewModel: PalmistryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var selectedHand by remember { mutableStateOf("right") }

    val cameraPermission = rememberPermissionRequest(
        permission = Manifest.permission.CAMERA,
        title = "Camera access",
        rationale = "Taking a palm photo needs camera access."
    )
    val galleryPermission = rememberPermissionRequest(
        permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE,
        title = "Photo access",
        rationale = "Choosing a palm photo from your gallery needs photo-library access."
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
                    viewModel.reset()
                } else {
                    snackbarHostState.showSnackbar("Could not save the photo.")
                }
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val f = copyUriToCache(context, uri, "palm")
                if (f != null) {
                    imageFile = f
                    previewBitmap = null
                    viewModel.reset()
                } else {
                    snackbarHostState.showSnackbar("Could not read the selected photo.")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Palmistry",
                        color = StarlightWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                subtitle = {
                    Text(
                        "AI palm reading from your photo",
                        color = StarlightWhite.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SacredGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CosmicDeepBlue
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when {
                uiState.isLoading -> {
                    PalmistryLoadingCard()
                }
                uiState.reading != null -> {
                    PalmistryResultCard(
                        reading = uiState.reading!!,
                        onAskAi = {
                            val block = uiState.reading!!.toContextBlock()
                            if (block.isNotBlank()) {
                                PendingPhotoContextHolder.set(
                                    PhotoContextAttachment(
                                        label = "Palmistry reading",
                                        contextBlock = block
                                    )
                                )
                            }
                            onOpenAiChat()
                        },
                        onRetake = {
                            imageFile = null
                            previewBitmap = null
                            viewModel.reset()
                        }
                    )
                }
                else -> {
                    PalmistryCaptureCard(
                        previewBitmap = previewBitmap,
                        hasImage = imageFile != null,
                        selectedHand = selectedHand,
                        onHandChange = { selectedHand = it },
                        onCamera = {
                            cameraPermission.request(context) { granted ->
                                if (granted) cameraLauncher.launch()
                            }
                        },
                        onGallery = {
                            galleryPermission.request(context) { granted ->
                                if (granted) galleryLauncher.launch("image/*")
                            }
                        },
                        onAnalyze = {
                            val f = imageFile
                            if (f != null) viewModel.analyze(f, selectedHand)
                        }
                    )
                }
            }
            uiState.error?.let { err ->
                Text(
                    err,
                    color = Color(0xFFFF8A80),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PalmistryCaptureCard(
    previewBitmap: Bitmap?,
    hasImage: Boolean,
    selectedHand: String,
    onHandChange: (String) -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onAnalyze: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .padding(18.dp)
    ) {
        Text(
            "Photograph your palm",
            color = StarlightWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        listOf(
            "Hold your palm flat with fingers together",
            "Use bright, even light — avoid shadows",
            "Fill the frame with your palm",
            "Keep the photo sharp, not blurry"
        ).forEach { tip ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 3.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SacredGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(tip, color = StarlightWhite.copy(alpha = 0.75f), fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // Hand selector
        Text("Which hand?", color = StarlightWhite.copy(alpha = 0.7f), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("right" to "Right Hand", "left" to "Left Hand").forEach { (value, label) ->
                val selected = selectedHand == value
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected) SacredGold.copy(alpha = 0.25f)
                            else CosmicCharcoal
                        )
                        .border(
                            1.dp,
                            if (selected) SacredGold else StarlightWhite.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onHandChange(value) }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        label,
                        color = if (selected) SacredGold else StarlightWhite.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // Preview
        if (previewBitmap != null) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Palm preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(14.dp))
        } else if (hasImage) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicCharcoal),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Photo ready for analysis",
                    color = SacredGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onCamera,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicCharcoal,
                    contentColor = StarlightWhite
                )
            ) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Camera")
            }
            Button(
                onClick = onGallery,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicCharcoal,
                    contentColor = StarlightWhite
                )
            ) {
                Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gallery")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onAnalyze,
            enabled = hasImage,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = SacredGold,
                contentColor = Color(0xFF1A1005),
                disabledContainerColor = SacredGold.copy(alpha = 0.3f)
            )
        ) {
            Text("Analyze My Palm", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun PalmistryLoadingCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = SacredGold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Reading your palm...",
            color = StarlightWhite.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        Text(
            "The AI is studying every line and mount.",
            color = StarlightWhite.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PalmistryResultCard(
    reading: PalmistryReading,
    onAskAi: () -> Unit,
    onRetake: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Hand summary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color(0xFF8B5E2E), Color(0xFF5C3A17))
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Text(
                    "Your Palm",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                if (reading.hand.isNotBlank()) {
                    Text(
                        reading.hand.replaceFirstChar { it.uppercase() } + " hand" +
                            (if (reading.handType.isNotBlank()) " · ${reading.handType}" else ""),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }
        }

        // Full reading text
        if (reading.reading.isNotBlank()) {
            ResultSectionCard(title = "Detailed Reading") {
                Text(
                    reading.reading,
                    color = StarlightWhite.copy(alpha = 0.9f),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // Lines
        if (reading.lines.isNotEmpty()) {
            ResultSectionCard(title = "Lines") {
                reading.lines.forEach { line ->
                    PalmDetailRow(
                        name = line.name,
                        badge = line.quality,
                        note = line.note
                    )
                }
            }
        }

        // Mounts
        if (reading.mounts.isNotEmpty()) {
            ResultSectionCard(title = "Mounts") {
                reading.mounts.forEach { mount ->
                    PalmDetailRow(
                        name = mount.name,
                        badge = mount.state,
                        note = mount.note
                    )
                }
            }
        }

        // Key observations
        if (reading.keyObservations.isNotEmpty()) {
            ResultSectionCard(title = "Key Observations") {
                reading.keyObservations.forEach { obs ->
                    BulletText(obs)
                }
            }
        }

        // Guidance
        if (reading.guidance.isNotEmpty()) {
            ResultSectionCard(title = "Guidance") {
                reading.guidance.forEach { g ->
                    BulletText(g)
                }
            }
        }

        // Uncertainties (honesty)
        if (reading.uncertainties.isNotEmpty()) {
            ResultSectionCard(title = "Unclear in Photo") {
                reading.uncertainties.forEach { u ->
                    BulletText(u)
                }
            }
        }

        if (reading.disclaimer.isNotBlank()) {
            Text(
                reading.disclaimer,
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
                onClick = onRetake,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicCharcoal,
                    contentColor = StarlightWhite
                )
            ) {
                Text("New Photo")
            }
            Button(
                onClick = onAskAi,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SacredGold,
                    contentColor = Color(0xFF1A1005)
                )
            ) {
                Text("Ask AI About This", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResultSectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .padding(16.dp)
    ) {
        Text(
            title,
            color = SacredGold,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun PalmDetailRow(name: String, badge: String, note: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                color = StarlightWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                modifier = Modifier.weight(1f)
            )
            if (badge.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SacredGold.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(badge, color = SacredGold, fontSize = 11.5.sp)
                }
            }
        }
        if (note.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(note, color = StarlightWhite.copy(alpha = 0.7f), fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun BulletText(text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text("• ", color = SacredGold, fontSize = 13.sp)
        Text(text, color = StarlightWhite.copy(alpha = 0.85f), fontSize = 13.sp)
    }
}
