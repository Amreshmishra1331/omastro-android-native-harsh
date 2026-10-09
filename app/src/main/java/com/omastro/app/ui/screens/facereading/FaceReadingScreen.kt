package com.omastro.app.ui.screens.facereading

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.WarningAmber
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
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.data.model.FaceReading
import com.omastro.app.data.model.PendingPhotoContextHolder
import com.omastro.app.data.model.PhotoContextAttachment
import com.omastro.app.data.repository.VisionRepository
import com.omastro.app.data.local.ReadingFeatureType
import com.omastro.app.data.repository.ReadingHistoryRepository
import com.omastro.app.ui.i18n.W
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

data class FaceReadingUiState(
    val isLoading: Boolean = false,
    val reading: FaceReading? = null,
    val error: String? = null
)

class FaceReadingViewModel : ViewModel() {
    private val repo = VisionRepository()
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(FaceReadingUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<FaceReadingUiState> = _uiState

    fun analyze(image: File, appContext: Context) {
        _uiState.value = FaceReadingUiState(isLoading = true)
        viewModelScope.launch {
            val result = repo.analyzeFaceReading(image)
            result
                .onSuccess { (reading, _) ->
                    _uiState.value = FaceReadingUiState(reading = reading)
                }
                .onFailure { e ->
                    _uiState.value = FaceReadingUiState(
                        error = e.message ?: W.t("face_err_failed")
                    )
                }
            // Auto-save completed readings to local history.
            // No-face refusals are NEVER saved — only real readings.
            val (reading, rawJson) = result.getOrNull() ?: return@launch
            if (reading.isFace) {
                runCatching {
                    ReadingHistoryRepository(appContext).save(
                        featureType = ReadingFeatureType.FACE,
                        resultJson = rawJson,
                        photo = image
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.value = FaceReadingUiState()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceReadingScreen(
    onBack: () -> Unit,
    onOpenAiChat: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: FaceReadingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // Collect global language so the whole screen auto-switches EN/हिन्दी.
    val lang by LangPrefs.language.collectAsState()

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }

    val cameraPermission = rememberPermissionRequest(
        permission = Manifest.permission.CAMERA,
        title = W.t("palm_cam_title"),
        rationale = W.t("face_cam_rationale")
    )
    val galleryPermission = rememberPermissionRequest(
        permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE,
        title = W.t("palm_lib_title"),
        rationale = W.t("face_lib_rationale")
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
                    snackbarHostState.showSnackbar(W.t("palm_err_save"))
                }
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val f = copyUriToCache(context, uri, "face")
                if (f != null) {
                    imageFile = f
                    previewBitmap = null
                    viewModel.reset()
                } else {
                    snackbarHostState.showSnackbar(W.t("palm_err_read"))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            W.t("face_title"),
                            color = StarlightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            W.t("face_sub"),
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
                    FaceReadingLoadingCard()
                }
                uiState.reading != null -> {
                    FaceReadingResultCard(
                        reading = uiState.reading!!,
                        onAskAi = {
                            // FULL reading → AI chat context: every feature,
                            // personality, career, relationships, health,
                            // guidance — or the honesty refusal only.
                            val block = uiState.reading!!.toContextBlock()
                            if (block.isNotBlank()) {
                                PendingPhotoContextHolder.set(
                                    PhotoContextAttachment(
                                        label = W.t("face_ctx_label"),
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
                    FaceReadingCaptureCard(
                        previewBitmap = previewBitmap,
                        hasImage = imageFile != null,
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
                            if (f != null) viewModel.analyze(f, context.applicationContext)
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
private fun FaceReadingCaptureCard(
    previewBitmap: Bitmap?,
    hasImage: Boolean,
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
            W.t("face_capture_title"),
            color = StarlightWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        listOf(
            W.t("face_tip_1"),
            W.t("face_tip_2"),
            W.t("face_tip_3"),
            W.t("face_tip_4")
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

        // Preview
        if (previewBitmap != null) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = W.t("face_preview_cd"),
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
                    W.t("face_ready"),
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
                Text(W.t("palm_camera"))
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
                Text(W.t("palm_gallery"))
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
            Text(W.t("face_analyze"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun FaceReadingLoadingCard() {
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
            W.t("face_loading"),
            color = StarlightWhite.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        Text(
            W.t("face_loading_sub"),
            color = StarlightWhite.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}

@Composable
internal fun FaceReadingResultCard(
    reading: FaceReading,
    onAskAi: () -> Unit,
    onRetake: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!reading.isFace) {
            // Honesty refusal: no clear face — never show invented features.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    W.t("face_not_face_title"),
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (reading.refusal.isNotBlank()) {
                    Text(
                        reading.refusal,
                        color = StarlightWhite.copy(alpha = 0.75f),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    W.t("face_not_face_hint"),
                    color = StarlightWhite.copy(alpha = 0.55f),
                    fontSize = 12.5.sp
                )
            }
        } else {
            // Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color(0xFF6D4C41), Color(0xFF3E2723))
                        )
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        W.t("face_your_face"),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    if (reading.overallPersonality.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            reading.overallPersonality,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        )
                    } else if (reading.reading.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            reading.reading.take(160),
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Full reading text
            if (reading.reading.isNotBlank()) {
                FaceSectionCard(title = W.t("palm_detailed")) {
                    Text(
                        reading.reading,
                        color = StarlightWhite.copy(alpha = 0.9f),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // Per-feature observation + interpretation
            if (reading.features.isNotEmpty()) {
                FaceSectionCard(title = W.t("face_features")) {
                    reading.features.forEach { feature ->
                        FaceFeatureRow(feature = feature)
                    }
                }
            }

            // Personality / career / relationships
            if (reading.career.isNotBlank()) {
                FaceSectionCard(title = W.t("face_career")) {
                    Text(
                        reading.career,
                        color = StarlightWhite.copy(alpha = 0.9f),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            if (reading.relationships.isNotBlank()) {
                FaceSectionCard(title = W.t("face_relationships")) {
                    Text(
                        reading.relationships,
                        color = StarlightWhite.copy(alpha = 0.9f),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            if (reading.healthIndications.isNotEmpty()) {
                FaceSectionCard(title = W.t("face_health")) {
                    reading.healthIndications.forEach { h ->
                        FaceBulletText(h)
                    }
                }
            }
            if (reading.guidance.isNotEmpty()) {
                FaceSectionCard(title = W.t("face_guidance")) {
                    reading.guidance.forEach { g ->
                        FaceBulletText(g)
                    }
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
                Text(W.t("palm_new"))
            }
            Button(
                onClick = onAskAi,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SacredGold,
                    contentColor = Color(0xFF1A1005)
                )
            ) {
                Text(W.t("palm_ask"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FaceSectionCard(title: String, content: @Composable () -> Unit) {
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
private fun FaceFeatureRow(feature: com.omastro.app.data.model.FaceFeature) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
    ) {
        Text(
            W.faceFeatureName(feature.key),
            color = StarlightWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp
        )
        if (feature.observation.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${W.t("face_observation")}: ${feature.observation}",
                color = StarlightWhite.copy(alpha = 0.8f),
                fontSize = 12.5.sp
            )
        }
        if (feature.interpretation.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "${W.t("face_interpretation")}: ${feature.interpretation}",
                color = SacredGold.copy(alpha = 0.9f),
                fontSize = 12.5.sp
            )
        }
    }
}

@Composable
private fun FaceBulletText(text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text("• ", color = SacredGold, fontSize = 13.sp)
        Text(text, color = StarlightWhite.copy(alpha = 0.85f), fontSize = 13.sp)
    }
}
