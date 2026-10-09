package com.omastro.app.ui.screens.vastu

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.SensorManager
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.screens.aichat.rememberPermissionRequest
import com.omastro.app.ui.screens.aichat.saveBitmapToCache
import com.omastro.app.ui.theme.CosmicCharcoal
import com.omastro.app.ui.theme.CosmicNavy
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

/** Switchable compass visual styles. All render on top of the camera preview. */
enum class CompassStyle { CLASSIC, MINIMAL, MANDALA, DIGITAL }

/**
 * Compass Mode — real-time magnetometer compass over a live camera preview.
 *
 * The user points the top of the phone at their main entrance; the dial shows
 * the direction it faces. "Use this direction" fills the Vastu form's
 * facing-direction field with the live 8-sector direction code. The shutter
 * button captures a photo stamped with the live direction + degrees, saves it
 * to the gallery, and can hand it to the Vastu house-map analysis.
 */
@Composable
fun CompassModeScreen(
    onUseDirection: (String) -> Unit,
    onUsePhoto: (File) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Collect global language so the screen auto-switches EN/हिन्दी.
    val lang by LangPrefs.language.collectAsState()

    val compass = remember { CompassSensorManager(context) }
    val compassCamera = remember { CompassCamera(context) }

    val cameraPermission = rememberPermissionRequest(
        Manifest.permission.CAMERA,
        W.t("vastu_compass_cam_title"),
        W.t("vastu_compass_cam_rationale")
    )

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var style by remember { mutableStateOf(CompassStyle.CLASSIC) }
    var capturing by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedFile by remember { mutableStateOf<File?>(null) }
    var capturedSavedToGallery by remember { mutableStateOf(false) }
    var showCapturedDialog by remember { mutableStateOf(false) }

    // Start/stop sensors with the screen lifecycle.
    DisposableEffect(compass) {
        compass.start()
        onDispose { compass.stop() }
    }

    val heading by compass.heading.collectAsState()
    val accuracy by compass.accuracy.collectAsState()
    // Only nag about calibration after the sensor has actually delivered a
    // reading — the initial UNRELIABLE state before first delivery is not
    // a real interference signal.
    val hasReading by compass.hasReading.collectAsState()
    val directionCode = CompassSensorManager.directionCode(heading)
    val degrees = CompassSensorManager.roundedDegrees(heading)
    val needsCalibration =
        hasReading && accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW

    fun capturePhoto() {
        if (capturing) return
        capturing = true
        captureError = false
        // Snapshot the live reading for the stamp.
        val snapCode = CompassSensorManager.directionCode(compass.heading.value)
        val snapDeg = CompassSensorManager.roundedDegrees(compass.heading.value)
        val snapName = W.dirName(snapCode)
        compassCamera.takePhoto { bitmap ->
            if (bitmap == null) {
                capturing = false
                captureError = true
                return@takePhoto
            }
            scope.launch(Dispatchers.IO) {
                val stamped = stampCompassOverlay(bitmap, snapName, snapCode, snapDeg)
                bitmap.recycle()
                val galleryOk = saveBitmapToGallery(context, stamped) != null
                val file = saveBitmapToCache(context, stamped)
                withContext(Dispatchers.Main) {
                    capturing = false
                    capturedBitmap = stamped
                    capturedFile = file
                    capturedSavedToGallery = galleryOk
                    showCapturedDialog = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!compass.available) {
            // Honest state: device has no magnetometer/accelerometer.
            CompassUnavailable(onBack = onBack)
            return@Box
        }

        if (cameraGranted) {
            CameraPreviewLayer(compassCamera)
        } else {
            // Camera not granted yet — dark background with rationale CTA.
            Column(
                modifier = Modifier.fillMaxSize().background(CosmicCharcoal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = StarlightWhite.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    W.t("vastu_compass_cam_title"),
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    W.t("vastu_compass_cam_rationale"),
                    color = StarlightWhite.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        cameraPermission.request(context) { granted -> cameraGranted = granted }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                ) {
                    Text(
                        W.t("perm_allow"),
                        color = Color(0xFF1A1005),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Readability scrim over the camera feed.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = W.t("back"),
                        tint = StarlightWhite
                    )
                }
                Text(
                    W.t("vastu_compass_title"),
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            // Style switcher
            CompassStyleSwitcher(
                style = style,
                onStyle = { style = it }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Compass visual (all styles overlay the camera preview)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (style) {
                    CompassStyle.CLASSIC -> CompassDialClassic(
                        heading = heading,
                        currentCode = directionCode,
                        modifier = Modifier.size(250.dp)
                    )
                    CompassStyle.MINIMAL -> CompassDialMinimal(
                        heading = heading,
                        degrees = degrees,
                        currentCode = directionCode,
                        modifier = Modifier.size(250.dp)
                    )
                    CompassStyle.MANDALA -> CompassDialMandala(
                        heading = heading,
                        currentCode = directionCode,
                        modifier = Modifier.size(270.dp)
                    )
                    CompassStyle.DIGITAL -> CompassDialDigital(
                        heading = heading,
                        degrees = degrees,
                        currentCode = directionCode,
                        modifier = Modifier.size(250.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Heading readout
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    W.t("vastu_compass_hint"),
                    color = StarlightWhite.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Honest sensor note: the dial reads magnetic north (no
                // declination correction) and metal nearby degrades accuracy.
                Text(
                    W.t("vastu_compass_north_note"),
                    color = StarlightWhite.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
                if (needsCalibration) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        W.t("vastu_compass_calibrate"),
                        color = Color(0xFFFFB74D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
                if (captureError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        W.t("vastu_compass_capture_failed"),
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Capture + Use-direction actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Shutter button
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(
                            if (capturing) SacredGold.copy(alpha = 0.4f)
                            else Color.White.copy(alpha = 0.92f)
                        )
                        .clickable(enabled = cameraGranted && !capturing) { capturePhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    if (capturing) {
                        CircularProgressIndicator(
                            color = Color(0xFF1A1005),
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = W.t("vastu_compass_capture_cd"),
                            tint = Color(0xFF1A1005),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                // Use-this-direction CTA
                Button(
                    onClick = { onUseDirection(directionCode) },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF1A1005)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        W.t("vastu_compass_use") + " — ${W.dirName(directionCode)}",
                        color = Color(0xFF1A1005),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // Captured-photo dialog
        if (showCapturedDialog && capturedBitmap != null) {
            CapturedPhotoDialog(
                bitmap = capturedBitmap!!,
                savedToGallery = capturedSavedToGallery,
                hasFile = capturedFile != null,
                onUseForAnalysis = {
                    capturedFile?.let { onUsePhoto(it) }
                    showCapturedDialog = false
                },
                onDismiss = { showCapturedDialog = false }
            )
        }
    }
}

@Composable
private fun CompassUnavailable(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicCharcoal)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Explore,
            contentDescription = null,
            tint = StarlightWhite.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            W.t("vastu_compass_unavailable_title"),
            color = StarlightWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            W.t("vastu_compass_unavailable_msg"),
            color = StarlightWhite.copy(alpha = 0.65f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
        ) {
            Text(
                W.t("back"),
                color = Color(0xFF1A1005),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Four switchable compass styles. */
@Composable
private fun CompassStyleSwitcher(
    style: CompassStyle,
    onStyle: (CompassStyle) -> Unit
) {
    val items = listOf(
        CompassStyle.CLASSIC to W.t("vastu_compass_style_classic"),
        CompassStyle.MINIMAL to W.t("vastu_compass_style_minimal"),
        CompassStyle.MANDALA to W.t("vastu_compass_style_mandala"),
        CompassStyle.DIGITAL to W.t("vastu_compass_style_digital")
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (s, label) ->
            val selected = s == style
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) SacredGold.copy(alpha = 0.9f)
                        else Color.White.copy(alpha = 0.14f)
                    )
                    .clickable { onStyle(s) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) Color(0xFF1A1005) else StarlightWhite,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// Camera: preview + still capture
// ------------------------------------------------------------------

/** Holds CameraX preview + ImageCapture; bound once per lifecycle. */
class CompassCamera(private val context: Context) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (_: Exception) {
                // Camera unavailable — compass still works; preview stays dark.
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun unbind() {
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
        }
        cameraProvider = null
        imageCapture = null
    }

    /** Captures a still photo; callback receives the bitmap or null on failure. */
    fun takePhoto(onResult: (Bitmap?) -> Unit) {
        val capture = imageCapture
        if (capture == null) {
            onResult(null)
            return
        }
        try {
            val tmpFile = File.createTempFile("compass_cap_", ".jpg", context.cacheDir)
            val outputOptions = ImageCapture.OutputFileOptions.Builder(tmpFile).build()
            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val bitmap = try {
                            decodeWithExifRotation(tmpFile)
                        } catch (_: Exception) {
                            null
                        } finally {
                            tmpFile.delete()
                        }
                        onResult(bitmap)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        onResult(null)
                    }
                }
            )
        } catch (_: Exception) {
            onResult(null)
        }
    }

    /** Decodes the capture and applies its EXIF rotation so the bitmap is upright. */
    private fun decodeWithExifRotation(file: File): Bitmap? {
        var bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        try {
            val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
            val degrees = when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees != 0f) {
                val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
                val rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                if (rotated !== bitmap) bitmap.recycle()
                bitmap = rotated
            }
        } catch (_: Exception) {
            // EXIF unreadable — use the bitmap as-is.
        }
        return bitmap
    }
}

/** CameraX live preview filling its parent. Bound once; unbound on dispose. */
@Composable
private fun CameraPreviewLayer(compassCamera: CompassCamera) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        compassCamera.bind(lifecycleOwner, previewView)
        onDispose { compassCamera.unbind() }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Stamps the live compass reading onto the photo: direction name + code,
 * degrees, and timestamp in a bottom banner — the photo itself proves which
 * direction was faced.
 */
private fun stampCompassOverlay(
    src: Bitmap,
    directionName: String,
    code: String,
    degrees: Int
): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(out)
    val w = out.width.toFloat()
    val h = out.height.toFloat()
    val bannerH = (h * 0.14f).coerceAtLeast(140f)

    canvas.drawRect(
        0f, h - bannerH, w, h,
        android.graphics.Paint().apply { color = android.graphics.Color.argb(215, 10, 8, 20) }
    )
    // Gold divider line
    canvas.drawRect(
        0f, h - bannerH, w, h - bannerH + 6f,
        android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#D4AF37") }
    )
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    val main = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = bannerH * 0.30f
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }
    val sub = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(220, 212, 175, 55)
        textSize = bannerH * 0.19f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    // Long direction names (e.g. Hindi "दक्षिण-पश्चिम") must never clip at
    // the photo edges: shrink the text to fit 94% of the photo width.
    val maxTextW = w * 0.94f
    val mainText = "$directionName ($code)  •  $degrees°"
    val subText = "OmAstro Vastu Compass  •  $dateStr"
    fitTextToWidth(main, mainText, maxTextW)
    fitTextToWidth(sub, subText, maxTextW)
    canvas.drawText(
        mainText,
        w / 2f, h - bannerH * 0.52f, main
    )
    canvas.drawText(
        subText,
        w / 2f, h - bannerH * 0.18f, sub
    )
    return out
}

/** Shrinks [paint].textSize until [text] fits within [maxWidth] (min 40%). */
private fun fitTextToWidth(
    paint: android.graphics.Paint,
    text: String,
    maxWidth: Float
) {
    val start = paint.textSize
    var size = start
    while (size > start * 0.4f && paint.measureText(text) > maxWidth) {
        size *= 0.92f
        paint.textSize = size
    }
}

/** Saves the bitmap to the device gallery (Pictures/OmAstro). Returns its Uri. */
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): android.net.Uri? {
    return try {
        val name = "OmAstro_Compass_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OmAstro")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)) {
                resolver.delete(uri, null, null)
                return null
            }
        } ?: run {
            resolver.delete(uri, null, null)
            return null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        uri
    } catch (_: Exception) {
        null
    }
}

/** Dialog after capture: stamped preview + gallery status + analysis handoff. */
@Composable
private fun CapturedPhotoDialog(
    bitmap: Bitmap,
    savedToGallery: Boolean,
    hasFile: Boolean,
    onUseForAnalysis: () -> Unit,
    onDismiss: () -> Unit
) {
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
                .padding(18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        W.t("vastu_compass_photo_title"),
                        color = StarlightWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = W.t("chat_close"),
                            tint = StarlightWhite.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = W.t("vastu_compass_photo_title"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    if (savedToGallery) W.t("vastu_compass_saved")
                    else W.t("vastu_compass_save_failed"),
                    color = if (savedToGallery) Color(0xFF81C784)
                    else Color(0xFFFF8A80),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (hasFile) {
                    Button(
                        onClick = onUseForAnalysis,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                    ) {
                        Text(
                            W.t("vastu_compass_use_photo"),
                            color = Color(0xFF1A1005),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        W.t("chat_close"),
                        color = StarlightWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// Compass dial styles
// ------------------------------------------------------------------

private val DIAL_CODES = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

/**
 * STYLE 1 — Classic dial. Rotating ring; all 8 directions shown; the live
 * facing direction is highlighted (gold wedge + enlarged gold label).
 */
@Composable
private fun CompassDialClassic(
    heading: Float,
    currentCode: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f - 8f
        val center = Offset(cx, cy)
        val rotation = -heading

        // Highlight wedge for the live 45° sector.
        val curIdx = DIAL_CODES.indexOf(currentCode).coerceAtLeast(0)
        val wedgePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy)
            val steps = 24
            for (s in 0..steps) {
                val a = Math.toRadians(
                    (curIdx * 45f - 22.5f + s * (45f / steps) + rotation - 90f).toDouble()
                )
                lineTo(
                    cx + cos(a).toFloat() * (radius - 4f),
                    cy + sin(a).toFloat() * (radius - 4f)
                )
            }
            close()
        }
        drawPath(wedgePath, SacredGold.copy(alpha = 0.22f))
        // (wedgeStart kept for readability of the sector math above)

        // Outer ring
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = radius,
            center = center,
            style = Stroke(width = 2f)
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.45f),
            radius = radius - 2f,
            center = center
        )

        // Tick marks (dial angle a, clockwise from North, maps to screen
        // angle a - 90 + rotation, matching the wedge above).
        for (deg in 0 until 360 step 15) {
            val rad = Math.toRadians((deg + rotation - 90f).toDouble())
            val isCardinal = deg % 90 == 0
            val tickLen = if (isCardinal) 22f else 12f
            drawLine(
                color = if (isCardinal) SacredGold else Color.White.copy(alpha = 0.5f),
                start = Offset(
                    cx + cos(rad).toFloat() * (radius - tickLen),
                    cy + sin(rad).toFloat() * (radius - tickLen)
                ),
                end = Offset(
                    cx + cos(rad).toFloat() * radius,
                    cy + sin(rad).toFloat() * radius
                ),
                strokeWidth = if (isCardinal) 4f else 2f
            )
        }

        // All 8 direction labels; live one highlighted.
        // (Same -90f screen mapping as the wedge/ticks: North at top when
        // heading = 0.)
        DIAL_CODES.forEachIndexed { i, code ->
            val deg = i * 45f + rotation - 90f
            val rad = Math.toRadians(deg.toDouble())
            val labelR = radius - 44f
            val pos = Offset(
                cx + cos(rad).toFloat() * labelR,
                cy + sin(rad).toFloat() * labelR
            )
            val isLive = code == currentCode
            val isNorth = code == "N"
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = when {
                        isLive -> android.graphics.Color.parseColor("#FFD54F")
                        isNorth -> android.graphics.Color.parseColor("#D4AF37")
                        else -> android.graphics.Color.argb(200, 255, 255, 255)
                    }
                    textSize = if (isLive) 46f else if (isNorth) 42f else 32f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
                drawText(code, pos.x, pos.y + 12f, paint)
            }
        }

        // Fixed gold marker at top = the direction the phone points to.
        val markerTop = Offset(cx, cy - radius - 14f)
        drawCircle(SacredGold, radius = 10f, center = markerTop)
        drawCircle(Color.White, radius = 4f, center = markerTop)
        drawCircle(SacredGold.copy(alpha = 0.9f), radius = 8f, center = center)
    }
}

/**
 * STYLE 2 — Modern minimal ring. Thin ring, cardinal ticks only, large
 * digital degrees in the center with the direction name.
 */
@Composable
private fun CompassDialMinimal(
    heading: Float,
    degrees: Int,
    currentCode: String,
    modifier: Modifier = Modifier
) {
    val dirName = W.dirName(currentCode)
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f - 8f
        val center = Offset(cx, cy)
        val rotation = -heading

        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = radius,
            center = center,
            style = Stroke(width = 3f)
        )
        // Progress arc showing heading (gold, from top clockwise to heading).
        drawArc(
            color = SacredGold,
            startAngle = -90f,
            sweepAngle = ((heading % 360f) + 360f) % 360f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = Stroke(width = 7f)
        )
        // 8 minimal ticks (same -90f screen mapping as the other dials).
        for (i in 0 until 8) {
            val rad = Math.toRadians((i * 45f + rotation - 90f).toDouble())
            val isLive = DIAL_CODES[i] == currentCode
            drawLine(
                color = if (isLive) SacredGold else Color.White.copy(alpha = 0.45f),
                start = Offset(
                    cx + cos(rad).toFloat() * (radius - 16f),
                    cy + sin(rad).toFloat() * (radius - 16f)
                ),
                end = Offset(
                    cx + cos(rad).toFloat() * radius,
                    cy + sin(rad).toFloat() * radius
                ),
                strokeWidth = if (isLive) 6f else 3f
            )
        }
        // Center digital readout
        drawContext.canvas.nativeCanvas.apply {
            val degPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 96f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            drawText("$degrees°", cx, cy + 10f, degPaint)
            val dirPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#D4AF37")
                textSize = 40f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            drawText("$dirName ($currentCode)", cx, cy + 62f, dirPaint)
        }
    }
}

/**
 * STYLE 3 — Vastu mandala. Eight direction sectors with their dikpala
 * (guardian deity); the live sector glows gold.
 */
@Composable
private fun CompassDialMandala(
    heading: Float,
    currentCode: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f - 8f
        val center = Offset(cx, cy)
        val rotation = -heading

        // 8 sectors
        DIAL_CODES.forEachIndexed { i, code ->
            val isLive = code == currentCode
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy)
                val steps = 24
                for (s in 0..steps) {
                    val a = Math.toRadians(
                        (i * 45f - 22.5f + s * (45f / steps) + rotation - 90f).toDouble()
                    )
                    lineTo(
                        cx + cos(a).toFloat() * radius,
                        cy + sin(a).toFloat() * radius
                    )
                }
                close()
            }
            drawPath(
                path,
                if (isLive) SacredGold.copy(alpha = 0.55f)
                else if (i % 2 == 0) Color.White.copy(alpha = 0.07f)
                else Color.White.copy(alpha = 0.03f)
            )
            // Sector divider
            val divA = Math.toRadians((i * 45f - 22.5f + rotation - 90f).toDouble())
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = center,
                end = Offset(
                    cx + cos(divA).toFloat() * radius,
                    cy + sin(divA).toFloat() * radius
                ),
                strokeWidth = 2f
            )
            // Direction code + deity name
            val midA = Math.toRadians((i * 45f + rotation - 90f).toDouble())
            val labelR = radius * 0.62f
            val pos = Offset(
                cx + cos(midA).toFloat() * labelR,
                cy + sin(midA).toFloat() * labelR
            )
            drawContext.canvas.nativeCanvas.apply {
                val codePaint = android.graphics.Paint().apply {
                    color = if (isLive) android.graphics.Color.parseColor("#1A1005")
                    else android.graphics.Color.argb(235, 255, 255, 255)
                    textSize = if (isLive) 40f else 34f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
                drawText(code, pos.x, pos.y, codePaint)
            }
        }

        // Outer ring
        drawCircle(
            color = SacredGold.copy(alpha = 0.7f),
            radius = radius,
            center = center,
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.25f),
            radius = radius - 2f,
            center = center
        )
        // Brahmasthan center
        drawCircle(SacredGold.copy(alpha = 0.85f), radius = 26f, center = center)
        drawContext.canvas.nativeCanvas.apply {
            val p = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#1A1005")
                textSize = 26f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            drawText("ॐ", cx, cy + 9f, p)
        }
    }
    // Deity labels need W (composition context) — overlay them in a Box.
    MandalaDeityOverlay(heading = heading, currentCode = currentCode)
}

/**
 * Deity-name overlay for the mandala style. Positioned with the same
 * sector math so names sit inside their sectors and rotate with heading.
 */
@Composable
private fun MandalaDeityOverlay(heading: Float, currentCode: String) {
    Box(
        modifier = Modifier.size(270.dp),
        contentAlignment = Alignment.Center
    ) {
        // Names are placed by angle using offset math in dp. The radius must
        // match the canvas label circle: (270/2 - 8) * 0.62, where the -8f is
        // the same inset CompassDialMandala applies to its ring.
        val radiusDp = (270f / 2f - 8f) * 0.62f
        DIAL_CODES.forEachIndexed { i, code ->
            val a = Math.toRadians((i * 45f - heading - 90f).toDouble())
            val dx = (cos(a) * radiusDp).dp
            val dy = (sin(a) * radiusDp).dp
            Box(
                modifier = Modifier
                    .offset(x = dx, y = dy + 14.dp)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    W.vastuDeityName(code),
                    color = if (code == currentCode) Color(0xFF1A1005)
                    else SacredGold.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = if (code == currentCode) FontWeight.Bold
                    else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * STYLE 4 — Digital readout. Big degrees, direction arrow rotating with
 * heading, direction name — like a digital instrument.
 */
@Composable
private fun CompassDialDigital(
    heading: Float,
    degrees: Int,
    currentCode: String,
    modifier: Modifier = Modifier
) {
    val dirName = W.dirName(currentCode)
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f - 8f
        val center = Offset(cx, cy)

        // Instrument panel
        drawCircle(
            color = Color.Black.copy(alpha = 0.55f),
            radius = radius,
            center = center
        )
        drawCircle(
            color = SacredGold.copy(alpha = 0.8f),
            radius = radius,
            center = center,
            style = Stroke(width = 3f)
        )

        // Rotating arrow (points to current heading).
        rotate(degrees = heading, pivot = center) {
            val arrowLen = radius * 0.72f
            // Shaft
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(cx, cy + arrowLen * 0.35f),
                end = Offset(cx, cy - arrowLen),
                strokeWidth = 10f
            )
            // Head
            val headPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - arrowLen - 26f)
                lineTo(cx - 22f, cy - arrowLen + 14f)
                lineTo(cx + 22f, cy - arrowLen + 14f)
                close()
            }
            drawPath(headPath, SacredGold)
            // Tail
            drawCircle(
                Color.White.copy(alpha = 0.85f),
                radius = 12f,
                center = Offset(cx, cy + arrowLen * 0.35f)
            )
        }

        // Fixed "N" marker at top for reference
        drawContext.canvas.nativeCanvas.apply {
            val p = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(160, 255, 255, 255)
                textSize = 30f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawText("N", cx, cy - radius + 36f, p)
        }

        // Digital readout
        drawContext.canvas.nativeCanvas.apply {
            val degPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 84f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            drawText("$degrees°", cx, cy + radius - 28f, degPaint)
            val dirPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#D4AF37")
                textSize = 38f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            drawText("$dirName ($currentCode)", cx, cy + radius + 14f, dirPaint)
        }
    }
}
