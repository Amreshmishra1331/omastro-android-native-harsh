package com.omastro.app.ui.screens.aichat

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omastro.app.data.model.VisionAnalysis
import com.omastro.app.data.repository.VisionRepository
import com.omastro.app.ui.theme.CosmicCharcoal
import com.omastro.app.ui.theme.CosmicNavy
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Copies a content Uri into the app cache dir and returns the file. */
suspend fun copyUriToCache(context: Context, uri: Uri, name: String): File? =
    withContext(Dispatchers.IO) {
        try {
            val ext = when (context.contentResolver.getType(uri)?.lowercase()) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val out = File(context.cacheDir, "vision_${System.currentTimeMillis()}_$name.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            }
            if (out.exists() && out.length() > 0) out else null
        } catch (_: Exception) {
            null
        }
    }

/** Saves a camera bitmap into the app cache dir and returns the file. */
suspend fun saveBitmapToCache(context: Context, bitmap: Bitmap): File? =
    withContext(Dispatchers.IO) {
        try {
            val out = File(context.cacheDir, "vision_${System.currentTimeMillis()}.jpg")
            FileOutputStream(out).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            if (out.exists() && out.length() > 0) out else null
        } catch (_: Exception) {
            null
        }
    }

/**
 * Photo source chooser + preview + question + vision upload.
 *
 * On success calls [onAnalyzed] with the analysis; the caller decides how to
 * display it and whether to attach parsed_facts as chat context.
 */
@Composable
fun PhotoAttachDialog(
    onDismiss: () -> Unit,
    onAnalyzed: (VisionAnalysis, String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf(0) } // 0=chooser, 1=preview+question, 2=uploading
    var imageFile by remember { mutableStateOf<File?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var question by remember { mutableStateOf("") }

    val cameraPermission = rememberPermissionRequest(
        permission = Manifest.permission.CAMERA,
        title = "Camera access",
        rationale = "Taking a photo needs camera access — e.g. a palm photo or a house-map photo for analysis."
    )
    val galleryPermission = rememberPermissionRequest(
        permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE,
        title = "Photo access",
        rationale = "Choosing a photo from your gallery needs photo-library access."
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
                    stage = 1
                } else {
                    onError("Could not save the photo. Please try again.")
                }
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val f = copyUriToCache(context, uri, "gallery")
                if (f != null) {
                    imageFile = f
                    previewBitmap = null
                    stage = 1
                } else {
                    onError("Could not read the selected photo.")
                }
            }
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val f = copyUriToCache(context, uri, "file")
                if (f != null) {
                    imageFile = f
                    previewBitmap = null
                    stage = 1
                } else {
                    onError("Could not read the selected file.")
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CosmicNavy)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Analyze a Photo",
                        color = StarlightWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = StarlightWhite)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                when (stage) {
                    0 -> {
                        Text(
                            "Take or choose a photo — palm, house map, chart — and ask AI about it.",
                            color = StarlightWhite.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PhotoSourceRow(
                            icon = Icons.Default.CameraAlt,
                            title = "Take Photo",
                            subtitle = "Use the camera",
                            onClick = {
                                cameraPermission.request(context) { granted ->
                                    if (granted) cameraLauncher.launch()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PhotoSourceRow(
                            icon = Icons.Default.Image,
                            title = "Choose from Gallery",
                            subtitle = "Pick a saved photo",
                            onClick = {
                                galleryPermission.request(context) { granted ->
                                    if (granted) galleryLauncher.launch("image/*")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PhotoSourceRow(
                            icon = Icons.Default.Description,
                            title = "Choose File",
                            subtitle = "Any image file",
                            onClick = {
                                galleryPermission.request(context) { granted ->
                                    if (granted) fileLauncher.launch("image/*")
                                }
                            }
                        )
                    }
                    1 -> {
                        val bmp = previewBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Photo preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CosmicCharcoal),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    imageFile?.name ?: "Photo ready",
                                    color = StarlightWhite.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = question,
                            onValueChange = { question = it },
                            label = { Text("Your question about this photo") },
                            placeholder = { Text("e.g. What does my palm say about career?") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = StarlightWhite,
                                unfocusedTextColor = StarlightWhite,
                                focusedLabelColor = SacredGold,
                                unfocusedLabelColor = StarlightWhite.copy(alpha = 0.6f),
                                cursorColor = SacredGold,
                                focusedBorderColor = SacredGold,
                                unfocusedBorderColor = StarlightWhite.copy(alpha = 0.25f)
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { stage = 0 }) {
                                Text("Retake", color = StarlightWhite.copy(alpha = 0.7f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val f = imageFile
                                    if (f == null || question.isBlank()) return@Button
                                    stage = 2
                                    scope.launch {
                                        VisionRepository().analyzeVision(f, question.trim())
                                            .onSuccess { analysis ->
                                                onAnalyzed(analysis, f.name)
                                            }
                                            .onFailure { e ->
                                                stage = 1
                                                onError(e.message ?: "Photo analysis failed.")
                                            }
                                    }
                                },
                                enabled = question.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SacredGold,
                                    contentColor = Color(0xFF1A1005),
                                    disabledContainerColor = SacredGold.copy(alpha = 0.35f)
                                )
                            ) {
                                Text("Analyze", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = SacredGold)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "Analyzing your photo...",
                                color = StarlightWhite.copy(alpha = 0.75f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoSourceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicCharcoal)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, title, tint = SacredGold, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(title, color = StarlightWhite, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = StarlightWhite.copy(alpha = 0.55f), fontSize = 12.sp)
        }
    }
}
