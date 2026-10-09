package com.omastro.app.ui.screens.vastu

import android.Manifest
import android.hardware.SensorManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.screens.aichat.rememberPermissionRequest
import com.omastro.app.ui.theme.CosmicCharcoal
import com.omastro.app.ui.theme.SacredGold
import com.omastro.app.ui.theme.StarlightWhite
import kotlin.math.cos
import kotlin.math.sin

/**
 * Compass Mode — real-time magnetometer compass over a live camera preview.
 *
 * The user points the top of the phone at their main entrance; the dial shows
 * the direction it faces. "Use this direction" fills the Vastu form's
 * facing-direction field with the live 8-sector direction code.
 */
@Composable
fun CompassModeScreen(
    onUseDirection: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // Collect global language so the screen auto-switches EN/हिन्दी.
    val lang by LangPrefs.language.collectAsState()

    val compass = remember { CompassSensorManager(context) }

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

    // Start/stop sensors with the screen lifecycle.
    DisposableEffect(compass) {
        compass.start()
        onDispose { compass.stop() }
    }

    val heading by compass.heading.collectAsState()
    val accuracy by compass.accuracy.collectAsState()
    val directionCode = CompassSensorManager.directionCode(heading)
    val degrees = CompassSensorManager.roundedDegrees(heading)
    val needsCalibration = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!compass.available) {
            // Honest state: device has no magnetometer/accelerometer.
            CompassUnavailable(onBack = onBack)
            return@Box
        }

        if (cameraGranted) {
            CameraPreviewLayer()
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

            Spacer(modifier = Modifier.weight(0.4f))

            // Compass dial
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CompassDial(
                    heading = heading,
                    modifier = Modifier.size(250.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Heading readout
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "$degrees°",
                    color = StarlightWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp
                )
                Text(
                    "${W.dirName(directionCode)} ($directionCode)",
                    color = SacredGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    W.t("vastu_compass_hint"),
                    color = StarlightWhite.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                if (needsCalibration) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        W.t("vastu_compass_calibrate"),
                        color = Color(0xFFFFB74D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Use-this-direction CTA
            Button(
                onClick = { onUseDirection(directionCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
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
                    fontSize = 16.sp
                )
            }
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
            fontSize = 17.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            W.t("vastu_compass_unavailable_msg"),
            color = StarlightWhite.copy(alpha = 0.65f),
            fontSize = 14.sp
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

/** CameraX live preview filling its parent. Bound once; unbound on dispose. */
@Composable
private fun CameraPreviewLayer() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        var cameraProvider: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                cameraProvider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview
                )
            } catch (_: Exception) {
                // Camera unavailable (in use by another app, etc.) — the
                // compass dial still works; the preview simply stays dark.
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            try {
                cameraProvider?.unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Rotating compass dial. The ring turns so the direction the phone points to
 * is always at the top; a fixed gold marker shows the current heading.
 */
@Composable
private fun CompassDial(
    heading: Float,
    modifier: Modifier = Modifier
) {
    // Standard compass abbreviations on the dial are universal; the localized
    // full direction name is shown in the readout below the dial.
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f - 8f
        val center = Offset(cx, cy)

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

        // Tick marks + labels rotate with -heading (current direction at top).
        val rotation = -heading
        for (deg in 0 until 360 step 15) {
            val rad = Math.toRadians((deg + rotation).toDouble())
            val isCardinal = deg % 90 == 0
            val tickLen = if (isCardinal) 22f else 12f
            val start = Offset(
                cx + cos(rad).toFloat() * (radius - tickLen),
                cy + sin(rad).toFloat() * (radius - tickLen)
            )
            val end = Offset(
                cx + cos(rad).toFloat() * radius,
                cy + sin(rad).toFloat() * radius
            )
            drawLine(
                color = if (isCardinal) SacredGold else Color.White.copy(alpha = 0.5f),
                start = start,
                end = end,
                strokeWidth = if (isCardinal) 4f else 2f
            )
        }

        // Direction labels (N at 0°, then every 45° clockwise).
        listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW").forEachIndexed { i, code ->
            val deg = i * 45f + rotation
            val rad = Math.toRadians(deg.toDouble())
            val labelR = radius - 44f
            val pos = Offset(
                cx + cos(rad).toFloat() * labelR,
                cy + sin(rad).toFloat() * labelR
            )
            val isNorth = code == "N"
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = if (isNorth)
                        android.graphics.Color.parseColor("#D4AF37")
                    else
                        android.graphics.Color.argb(220, 255, 255, 255)
                    textSize = if (isNorth) 44f else 34f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
                // drawContext.canvas.nativeCanvas coordinates are in px.
                drawText(code, pos.x, pos.y + 12f, paint)
            }
        }

        // Fixed gold marker at top = the direction the phone points to.
        val markerTop = Offset(cx, cy - radius - 14f)
        drawCircle(SacredGold, radius = 10f, center = markerTop)
        drawCircle(Color.White, radius = 4f, center = markerTop)

        // Center hub
        drawCircle(SacredGold.copy(alpha = 0.9f), radius = 8f, center = center)
    }
}
