package com.omastro.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.data.local.ReadingFeatureType
import com.omastro.app.data.model.FaceReading
import com.omastro.app.data.model.PalmistryReading
import com.omastro.app.data.model.PendingPhotoContextHolder
import com.omastro.app.data.model.PhotoContextAttachment
import com.omastro.app.data.model.VastuAnalysis
import com.omastro.app.data.repository.ReadingHistoryRepository
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.screens.facereading.FaceReadingResultCard
import com.omastro.app.ui.screens.palmistry.PalmistryResultCard
import com.omastro.app.ui.screens.vastu.VastuResultView
import com.omastro.app.ui.theme.CosmicDeepBlue
import com.omastro.app.ui.theme.CosmicNavy
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite

/**
 * Re-view of a saved reading.
 *
 * Renders the stored server JSON with the SAME result cards as the live
 * feature screens — nothing is re-analyzed, nothing is re-fetched, and the
 * stored content is shown exactly as the server returned it (including
 * null-score states). A "Saved on <date>" banner makes clear this is a past
 * reading, not a fresh analysis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingHistoryDetailScreen(
    readingId: String,
    onBack: () -> Unit,
    onOpenAiChat: () -> Unit,
    onNewAnalysis: (featureType: String) -> Unit
) {
    val context = LocalContext.current
    // Collect global language so the whole screen auto-switches EN/हिन्दी.
    val lang by LangPrefs.language.collectAsState()

    var saved by remember { mutableStateOf<ReadingHistoryRepository.SavedReading?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(readingId) {
        val row = ReadingHistoryRepository(context).get(readingId)
        if (row == null) loadFailed = true else saved = row
    }

    val gson = remember { Gson() }
    @Suppress("UNCHECKED_CAST")
    val dataMap: Map<String, Any?>? = remember(saved) {
        val json = saved?.resultJson ?: return@remember null
        runCatching { gson.fromJson(json, Map::class.java) as Map<String, Any?> }.getOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            saved?.label ?: W.t("history_title"),
                            color = StarlightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1
                        )
                        if (saved != null) {
                            Text(
                                "${W.t("history_saved_on")} ${formatHistoryDate(saved!!.createdAt)}",
                                color = StarlightWhite.copy(alpha = 0.55f),
                                fontSize = 12.sp
                            )
                        }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        when {
            loadFailed || (saved != null && dataMap == null) -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        W.t("history_load_failed"),
                        color = StarlightWhite.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
            saved == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = SacredGold)
                }
            }
            else -> {
                val row = saved!!
                // Past-reading banner: this is a saved result, not a fresh analysis.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SacredGold.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = SacredGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "  ${W.t("history_saved_on")} ${formatHistoryDate(row.createdAt)}",
                            color = SacredGold,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    when (row.featureType) {
                        ReadingFeatureType.PALMISTRY -> {
                            val reading = PalmistryReading.fromMap(dataMap)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                PalmistryResultCard(
                                    reading = reading,
                                    onAskAi = {
                                        val block = reading.toContextBlock()
                                        if (block.isNotBlank()) {
                                            PendingPhotoContextHolder.set(
                                                PhotoContextAttachment(
                                                    label = W.t("palm_title"),
                                                    contextBlock = block
                                                )
                                            )
                                        }
                                        onOpenAiChat()
                                    },
                                    onRetake = { onNewAnalysis(ReadingFeatureType.PALMISTRY) }
                                )
                            }
                        }
                        ReadingFeatureType.FACE -> {
                            val reading = FaceReading.fromMap(dataMap)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                FaceReadingResultCard(
                                    reading = reading,
                                    onAskAi = {
                                        val block = reading.toContextBlock()
                                        if (block.isNotBlank()) {
                                            PendingPhotoContextHolder.set(
                                                PhotoContextAttachment(
                                                    label = W.t("face_title"),
                                                    contextBlock = block
                                                )
                                            )
                                        }
                                        onOpenAiChat()
                                    },
                                    onRetake = { onNewAnalysis(ReadingFeatureType.FACE) }
                                )
                            }
                        }
                        else -> {
                            // Vastu: photo-mode JSON wraps the analysis under
                            // "vastu_analysis"; manual-form JSON IS the analysis.
                            @Suppress("UNCHECKED_CAST")
                            val analysisMap =
                                (dataMap?.get("vastu_analysis") as? Map<String, Any?>)
                                    ?: dataMap
                            @Suppress("UNCHECKED_CAST")
                            val photoParse =
                                dataMap?.get("photo_parse") as? Map<String, Any?>
                            val analysis = VastuAnalysis.fromMap(analysisMap)
                            VastuResultView(
                                analysis = analysis,
                                onAskAi = {
                                    val block = analysis.toFullContextBlock(
                                        request = null,
                                        photoParse = photoParse,
                                        photoNotes = null
                                    )
                                    if (block.isNotBlank()) {
                                        PendingPhotoContextHolder.set(
                                            PhotoContextAttachment(
                                                label = W.t("vastu_title"),
                                                contextBlock = block
                                            )
                                        )
                                    }
                                    onOpenAiChat()
                                },
                                onNew = { onNewAnalysis(ReadingFeatureType.VASTU) }
                            )
                        }
                    }
                }
            }
        }
    }
}
