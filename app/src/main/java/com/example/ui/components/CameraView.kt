package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.BotanicalGold
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Reusable CameraX viewfinder component for real-time plant specimen capture.
 * Provides live preview, tap-to-focus metering, pinch-to-zoom, flash mode cycling,
 * front/back lens switching, framing guide overlay, and runtime permission handling.
 */
@Composable
fun CameraView(
    onPhotoCaptured: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    isCaptureEnabled: Boolean = true,
    onCaptureError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    if (!hasCameraPermission) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Camera permission required",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Camera Access Needed for Live Capture",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Grant camera permission to photograph leaves, flowers, bark, or fruits in real time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("camera_view_grant_permission_button")
                ) {
                    Text("Enable Camera")
                }
            }
        }
        return
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var canSwitchLens by remember { mutableStateOf(false) }
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(4f) }
    var isCapturing by remember { mutableStateOf(false) }
    var showShutterFlash by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }

    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(showShutterFlash) {
        if (showShutterFlash) {
            delay(140)
            showShutterFlash = false
        }
    }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(1100)
            focusPoint = null
        }
    }

    LaunchedEffect(flashMode) {
        imageCaptureRef?.flashMode = flashMode
    }

    DisposableEffect(lifecycleOwner, lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var disposed = false

        cameraProviderFuture.addListener({
            if (disposed) return@addListener
            try {
                val cameraProvider = cameraProviderFuture.get()
                val backAvailable = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                val frontAvailable = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                canSwitchLens = backAvailable && frontAvailable

                val chosenLens = when {
                    lensFacing == CameraSelector.LENS_FACING_BACK && backAvailable -> CameraSelector.LENS_FACING_BACK
                    lensFacing == CameraSelector.LENS_FACING_FRONT && frontAvailable -> CameraSelector.LENS_FACING_FRONT
                    backAvailable -> CameraSelector.LENS_FACING_BACK
                    frontAvailable -> CameraSelector.LENS_FACING_FRONT
                    else -> {
                        cameraError = "No camera hardware detected on this device."
                        return@addListener
                    }
                }

                val selector = CameraSelector.Builder()
                    .requireLensFacing(chosenLens)
                    .build()

                val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                val preview = Preview.Builder()
                    .setTargetRotation(rotation)
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(flashMode)
                    .setTargetRotation(rotation)
                    .build()

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    capture
                )

                cameraRef = camera
                imageCaptureRef = capture
                hasFlashUnit = camera.cameraInfo.hasFlashUnit()
                camera.cameraInfo.zoomState.value?.let { z ->
                    minZoomRatio = z.minZoomRatio
                    maxZoomRatio = z.maxZoomRatio.coerceAtMost(8f)
                    currentZoomRatio = z.zoomRatio
                }
                cameraError = null
            } catch (e: Exception) {
                Log.w(TAG, "CameraX bind error", e)
                cameraError = e.localizedMessage ?: "Unable to initialize camera preview."
            }
        }, mainExecutor)

        onDispose {
            disposed = true
            cameraRef = null
            imageCaptureRef = null
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("camera_view_container")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(Color.Black)
        ) {
            if (cameraError != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Camera error",
                        tint = BotanicalGold,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cameraError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            } else {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val camera = cameraRef ?: return@detectTapGestures
                                focusPoint = offset
                                val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                                val action = FocusMeteringAction.Builder(
                                    point,
                                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                                camera.cameraControl.startFocusAndMetering(action)
                            }
                        }
                        .pointerInput(minZoomRatio, maxZoomRatio) {
                            detectTransformGestures { _, _, zoomChange, _ ->
                                val camera = cameraRef ?: return@detectTransformGestures
                                if (zoomChange != 1f) {
                                    val next = (currentZoomRatio * zoomChange).coerceIn(minZoomRatio, maxZoomRatio)
                                    currentZoomRatio = next
                                    camera.cameraControl.setZoomRatio(next)
                                }
                            }
                        }
                )

                // Subtle Botanical Specimen Reticle Guide
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val padX = size.width * 0.14f
                    val padY = size.height * 0.16f
                    val corner = 24.dp.toPx()
                    val stroke = 2.dp.toPx()
                    val guideColor = Color.White.copy(alpha = 0.45f)

                    // Top-left
                    drawLine(guideColor, Offset(padX, padY), Offset(padX + corner, padY), stroke, StrokeCap.Round)
                    drawLine(guideColor, Offset(padX, padY), Offset(padX, padY + corner), stroke, StrokeCap.Round)
                    // Top-right
                    drawLine(guideColor, Offset(size.width - padX, padY), Offset(size.width - padX - corner, padY), stroke, StrokeCap.Round)
                    drawLine(guideColor, Offset(size.width - padX, padY), Offset(size.width - padX, padY + corner), stroke, StrokeCap.Round)
                    // Bottom-left
                    drawLine(guideColor, Offset(padX, size.height - padY), Offset(padX + corner, size.height - padY), stroke, StrokeCap.Round)
                    drawLine(guideColor, Offset(padX, size.height - padY), Offset(padX, size.height - padY - corner), stroke, StrokeCap.Round)
                    // Bottom-right
                    drawLine(guideColor, Offset(size.width - padX, size.height - padY), Offset(size.width - padX - corner, size.height - padY), stroke, StrokeCap.Round)
                    drawLine(guideColor, Offset(size.width - padX, size.height - padY), Offset(size.width - padX, size.height - padY - corner), stroke, StrokeCap.Round)
                }

                // Tap-to-focus Ring Indicator
                focusPoint?.let { pt ->
                    val ringHalfPx = with(density) { 26.dp.toPx() }
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (pt.x - ringHalfPx).roundToInt(),
                                    (pt.y - ringHalfPx).roundToInt()
                                )
                            }
                            .size(52.dp)
                            .border(2.dp, BotanicalGold, CircleShape)
                    )
                }

                // Shutter Flash Feedback
                androidx.compose.animation.AnimatedVisibility(
                    visible = showShutterFlash,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.45f))
                    )
                }

                // Top Bar: Guidance Pill, Flash Toggle, Lens Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = "Tap to focus • Pinch to zoom",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (hasFlashUnit) {
                            val (flashIcon, flashLabel) = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn to "Flash On"
                                ImageCapture.FLASH_MODE_OFF -> Icons.Filled.FlashOff to "Flash Off"
                                else -> Icons.Filled.FlashAuto to "Flash Auto"
                            }
                            IconButton(
                                onClick = {
                                    flashMode = when (flashMode) {
                                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                                        else -> ImageCapture.FLASH_MODE_AUTO
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .testTag("camera_flash_button")
                            ) {
                                Icon(flashIcon, contentDescription = flashLabel, tint = Color.White)
                            }
                        }

                        if (canSwitchLens) {
                            IconButton(
                                onClick = {
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                        CameraSelector.LENS_FACING_FRONT
                                    } else {
                                        CameraSelector.LENS_FACING_BACK
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .testTag("camera_flip_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Cameraswitch,
                                    contentDescription = "Switch camera lens",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Bottom Controls: Zoom Pill + Capture Shutter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (maxZoomRatio >= 1.8f) {
                        Surface(
                            onClick = {
                                val nextZoom = if (currentZoomRatio < 1.5f) 2f.coerceAtMost(maxZoomRatio) else 1f
                                currentZoomRatio = nextZoom
                                cameraRef?.cameraControl?.setZoomRatio(nextZoom)
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.testTag("camera_zoom_button")
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1fx", currentZoomRatio),
                                style = MaterialTheme.typography.labelMedium,
                                color = BotanicalGold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Button(
                        onClick = {
                            val capture = imageCaptureRef ?: return@Button
                            if (isCapturing || !isCaptureEnabled) return@Button
                            isCapturing = true
                            showShutterFlash = true

                            val dir = File(context.filesDir, "scans").apply { mkdirs() }
                            val outFile = File(dir, "cam_${UUID.randomUUID()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()
                            capture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        isCapturing = false
                                        onPhotoCaptured(Uri.fromFile(outFile))
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isCapturing = false
                                        Log.e(TAG, "Capture failed", exception)
                                        onCaptureError?.invoke(
                                            exception.localizedMessage ?: "Failed to capture photograph."
                                        )
                                    }
                                }
                            )
                        },
                        enabled = isCaptureEnabled && !isCapturing,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BotanicalGold,
                            contentColor = Color(0xFF1A1405)
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .semantics(mergeDescendants = true) {}
                            .testTag("camera_shutter_button")
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF1A1405)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isCaptureEnabled) "Capture Specimen" else "Max Photos Reached")
                        }
                    }

                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        }
    }
}

private const val TAG = "CameraView"
