package com.family.talkly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import com.family.talkly.data.models.CallType
import com.family.talkly.data.models.FamilyMember
import com.family.talkly.data.zego.CurrentCallInfo
import com.family.talkly.ui.theme.WhatsappGreen
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CameraPreviewView(
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    key(isFrontCamera) {
        val context = LocalContext.current

        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        private var cameraDevice: CameraDevice? = null
                        private var captureSession: CameraCaptureSession? = null
                        private var backgroundThread: HandlerThread? = null
                        private var backgroundHandler: Handler? = null

                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            try {
                                backgroundThread = HandlerThread("CameraBackground_${System.currentTimeMillis()}").also { it.start() }
                                backgroundHandler = Handler(backgroundThread!!.looper)

                                val cameraManager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                                val targetFacing = if (isFrontCamera) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK

                                val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                                    val facing = cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
                                    facing == targetFacing
                                } ?: cameraManager.cameraIdList.firstOrNull() ?: return

                                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                    return
                                }

                                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                                    override fun onOpened(camera: CameraDevice) {
                                        cameraDevice = camera
                                        val surface = Surface(st)
                                        val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                        requestBuilder.addTarget(surface)

                                        camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                                            override fun onConfigured(session: CameraCaptureSession) {
                                                captureSession = session
                                                try {
                                                    requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                                    session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                                                } catch (e: Exception) {
                                                    Log.e("CameraPreviewView", "Error setting repeating request: ${e.message}")
                                                }
                                            }

                                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                                        }, backgroundHandler)
                                    }

                                    override fun onDisconnected(camera: CameraDevice) {
                                        try { camera.close() } catch (_: Exception) {}
                                        cameraDevice = null
                                    }

                                    override fun onError(camera: CameraDevice, error: Int) {
                                        try { camera.close() } catch (_: Exception) {}
                                        cameraDevice = null
                                    }
                                }, backgroundHandler)
                            } catch (e: Exception) {
                                Log.e("CameraPreviewView", "Error opening camera: ${e.message}")
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            try {
                                captureSession?.close()
                                cameraDevice?.close()
                                backgroundThread?.quitSafely()
                            } catch (e: Exception) {
                                Log.e("CameraPreviewView", "Error releasing camera: ${e.message}")
                            }
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }
                }
            },
            modifier = modifier
        )
    }
}

@Composable
fun RemoteVideoView(
    member: FamilyMember?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF101D25)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(WhatsappGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member?.name?.take(2)?.uppercase() ?: "FA",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = member?.name ?: "Partner",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = WhatsappGreen.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "HD Video Live",
                    color = WhatsappGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CallScreen(
    callInfo: CurrentCallInfo,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onFlipCamera: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    val member = callInfo.targetMember
    val isVideo = callInfo.callType == CallType.VIDEO

    val minutes = callInfo.durationSeconds / 60
    val seconds = callInfo.durationSeconds % 60
    val formattedTimer = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    val callStatusText = when (callInfo.state) {
        com.family.talkly.data.zego.CallState.OUTGOING_CALLING -> "Calling..."
        com.family.talkly.data.zego.CallState.OUTGOING_RINGING -> "Ringing..."
        com.family.talkly.data.zego.CallState.INCOMING_RINGING -> "Incoming call..."
        com.family.talkly.data.zego.CallState.ACTIVE -> formattedTimer
        com.family.talkly.data.zego.CallState.ENDED -> "Call ended"
        else -> ""
    }

    var isSwapped by remember { mutableStateOf(false) }
    var isSplitScreen by remember { mutableStateOf(false) }
    var pipOffsetX by remember { mutableFloatStateOf(0f) }
    var pipOffsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B141A))
    ) {
        if (isVideo && !callInfo.isCameraOff) {
            if (isSplitScreen) {
                // Split Screen Mode: Top and Bottom halves stacked
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Half: Remote View
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        RemoteVideoView(member = member, modifier = Modifier.fillMaxSize())
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = member?.name ?: "Partner",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Bottom Half: Local View
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        CameraPreviewView(
                            isFrontCamera = callInfo.isFrontCamera,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (callInfo.isFrontCamera) "You (Front)" else "You (Back)",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Exit Split Screen Floating Button
                IconButton(
                    onClick = { isSplitScreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Split Screen",
                        tint = Color.White
                    )
                }

            } else {
                // Standard Overlay Mode with Draggable PIP Popup
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Main Background Feed
                    if (!isSwapped) {
                        RemoteVideoView(member = member, modifier = Modifier.fillMaxSize())
                    } else {
                        CameraPreviewView(
                            isFrontCamera = callInfo.isFrontCamera,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Overlay Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )

                    // Draggable Local/Remote Picture-in-Picture Popup
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 90.dp, end = 16.dp)
                            .offset { IntOffset(pipOffsetX.roundToInt(), pipOffsetY.roundToInt()) }
                            .size(width = 120.dp, height = 170.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, WhatsappGreen, RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    pipOffsetX += dragAmount.x
                                    pipOffsetY += dragAmount.y
                                }
                            }
                            .clickable { isSwapped = !isSwapped },
                        color = Color.Black
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (!isSwapped) {
                                CameraPreviewView(
                                    isFrontCamera = callInfo.isFrontCamera,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                RemoteVideoView(member = member, modifier = Modifier.fillMaxSize())
                            }

                            // Bottom Label Tag
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (!isSwapped) {
                                        if (callInfo.isFrontCamera) "You (Front)" else "You (Rear)"
                                    } else {
                                        member?.name?.take(8) ?: "Partner"
                                    },
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Top-Right Split Screen Icon Button on PIP Popup
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .clickable { isSplitScreen = true }
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Splitscreen,
                                    contentDescription = "Split Screen Mode",
                                    tint = WhatsappGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Audio Call Mode View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(WhatsappGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member?.name?.take(2)?.uppercase() ?: "FA",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = member?.name ?: "Family Member",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = callStatusText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (callInfo.state == com.family.talkly.data.zego.CallState.ACTIVE) WhatsappGreen else Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Top Status Header Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Encrypted",
                    tint = WhatsappGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "End-to-End Encrypted Family Call",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = callStatusText,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Bottom Floating Call Controls
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF1F2C34).copy(alpha = 0.95f),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute Mic
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            if (callInfo.isMuted) Color.White else Color.White.copy(alpha = 0.15f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (callInfo.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (callInfo.isMuted) Color.Red else Color.White
                    )
                }

                // Camera Flip
                IconButton(
                    onClick = onFlipCamera,
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Flip Camera",
                        tint = Color.White
                    )
                }

                // Video Toggle
                IconButton(
                    onClick = onToggleCamera,
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            if (callInfo.isCameraOff) Color.White else Color.White.copy(alpha = 0.15f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (callInfo.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = "Camera Toggle",
                        tint = if (callInfo.isCameraOff) Color.Red else Color.White
                    )
                }

                // Speaker Toggle
                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            if (callInfo.isSpeakerOn) WhatsappGreen else Color.White.copy(alpha = 0.15f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speaker",
                        tint = Color.White
                    )
                }

                // End Call FAB
                FloatingActionButton(
                    onClick = onEndCall,
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

