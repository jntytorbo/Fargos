package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.network.StreamQuality
import com.example.network.SubtitleTrack
import com.example.ui.theme.LocalAccentColor
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.Compass
import compose.icons.feathericons.Cpu
import compose.icons.feathericons.FastForward
import compose.icons.feathericons.Maximize
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Pause
import compose.icons.feathericons.Play
import compose.icons.feathericons.RotateCcw
import compose.icons.feathericons.RotateCw
import compose.icons.feathericons.Settings
import compose.icons.feathericons.Sliders
import compose.icons.feathericons.Sun
import compose.icons.feathericons.Volume2
import compose.icons.feathericons.VolumeX
import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.MPV
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val BROWSER_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

enum class MpvHwDecMode(val label: String, val mpvValue: String) {
    HW_AUTO("Hardware (Auto)", "auto-safe"),
    HW_MEDIACODEC("Hardware (MediaCodec)", "mediacodec-copy"),
    SW_FFMPEG("Software (FFmpeg)", "no")
}

enum class MpvAspectRatio(val label: String, val overrideValue: String) {
    ORIGINAL("Original", "-1"),
    SIXTEEN_NINE("16:9", "16:9"),
    FOUR_THREE("4:3", "4:3"),
    TWENTY_ONE_NINE("21:9", "2.35:1")
}

/**
 * Concrete implementation of MPV Surface View with streaming and codec optimizations
 */
class MpvVideoView(
    context: Context,
    attrs: AttributeSet? = null
) : BaseMPVView(context, attrs) {

    var isSurfaceReady: Boolean = false
        private set
    private var pendingUrl: String? = null

    init {
        keepScreenOn = true
        setZOrderMediaOverlay(true)
    }

    override fun initOptions() {
        setVo("gpu")
        mpv.setOptionString("hwdec", "auto-safe")
        mpv.setOptionString("hwdec-codecs", "all")
        mpv.setOptionString("ao", "audiotrack,opensles")
        mpv.setOptionString("tls-verify", "no")
        mpv.setOptionString("keep-open", "always")
        mpv.setOptionString("ytdl", "no")

        // High-performance network and streaming buffer setup
        mpv.setOptionString("demuxer-max-bytes", "67108864") // 64 MB cache
        mpv.setOptionString("demuxer-max-back-bytes", "33554432") // 32 MB backward cache
        mpv.setOptionString("demuxer-readahead-secs", "60")
        mpv.setOptionString("cache-secs", "60")
        mpv.setOptionString("volume-max", "200") // Up to 200% volume boost
        mpv.setOptionString("network-timeout", "30")
    }

    override fun postInitOptions() {
        mpv.setOptionString("hwdec", "auto-safe")
        mpv.setOptionString("tls-verify", "no")
    }

    override fun surfaceCreated(holder: android.view.SurfaceHolder) {
        super.surfaceCreated(holder)
        isSurfaceReady = true
        val url = pendingUrl
        if (url != null) {
            pendingUrl = null
            playFile(url)
            try {
                mpv.setPropertyBoolean("pause", false)
            } catch (_: Exception) {}
        }
    }

    override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
        isSurfaceReady = false
        super.surfaceDestroyed(holder)
    }

    fun startPlayback(filePath: String) {
        if (isSurfaceReady || (holder.surface != null && holder.surface.isValid)) {
            isSurfaceReady = true
            playFile(filePath)
            try {
                mpv.setPropertyBoolean("pause", false)
            } catch (_: Exception) {}
        } else {
            pendingUrl = filePath
        }
    }

    override fun observeProperties() {
        // Polled continuously in LaunchedEffect
    }
}

data class MpvDiagnosticsData(
    val title: String,
    val streamUrl: String,
    val format: String,
    val resolution: String,
    val fps: String,
    val videoCodec: String,
    val audioCodec: String,
    val hwdecMode: String,
    val speed: String,
    val volume: String,
    val position: String,
    val duration: String,
    val cacheSeconds: String,
    val droppedFrames: String,
    val lastError: String?
)

@Composable
fun GoPlayer(
    title: String,
    qualities: List<StreamQuality>,
    subtitles: List<SubtitleTrack> = emptyList(),
    defaultHeaders: Map<String, String> = emptyMap(),
    onClose: () -> Unit
) {
    MpvPlayerOverlay(
        title = title,
        qualities = qualities,
        subtitles = subtitles,
        defaultHeaders = defaultHeaders,
        onClose = onClose
    )
}

@Composable
fun ExoPlayerOverlay(
    title: String,
    qualities: List<StreamQuality>,
    subtitles: List<SubtitleTrack> = emptyList(),
    defaultHeaders: Map<String, String> = emptyMap(),
    onClose: () -> Unit
) {
    MpvPlayerOverlay(
        title = title,
        qualities = qualities,
        subtitles = subtitles,
        defaultHeaders = defaultHeaders,
        onClose = onClose
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpvPlayerOverlay(
    title: String,
    qualities: List<StreamQuality>,
    subtitles: List<SubtitleTrack> = emptyList(),
    defaultHeaders: Map<String, String> = emptyMap(),
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val accent = LocalAccentColor.current

    var selectedQuality by remember(qualities) {
        mutableStateOf(qualities.firstOrNull { it.isDefault } ?: qualities.firstOrNull())
    }
    var selectedSubtitle by remember(subtitles) { mutableStateOf<SubtitleTrack?>(null) }

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPosSec by remember { mutableIntStateOf(0) }
    var durationSec by remember { mutableIntStateOf(0) }
    var videoResolution by remember { mutableStateOf("Detecting...") }
    var videoFps by remember { mutableStateOf("--") }
    var videoCodec by remember { mutableStateOf("--") }
    var audioCodec by remember { mutableStateOf("--") }
    var droppedFramesCount by remember { mutableStateOf("0") }
    var cacheDurationSec by remember { mutableStateOf("0s") }

    var currentHwDec by remember { mutableStateOf(MpvHwDecMode.HW_AUTO) }
    var currentAspect by remember { mutableStateOf(MpvAspectRatio.ORIGINAL) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLongPressSpeedBoost by remember { mutableStateOf(false) }
    var currentVolume by remember { mutableIntStateOf(100) }
    var currentBrightness by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
        )
    }

    // Temporary HUD indicator when swiping gestures (Volume / Brightness / Seek / Speed)
    var gestureIndicatorText by remember { mutableStateOf<String?>(null) }
    var doubleTapIndicator by remember { mutableStateOf<String?>(null) }
    var doubleTapSide by remember { mutableStateOf<String?>(null) } // "left" or "right"

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorDetails by remember { mutableStateOf<String?>(null) }

    var showQualitySheet by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showAspectMenu by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    var mpvViewRef by remember { mutableStateOf<MpvVideoView?>(null) }

    // Merged headers for currently selected stream
    val activeHeaders = remember(selectedQuality, defaultHeaders) {
        val streamHeaders = selectedQuality?.headers ?: emptyMap()
        defaultHeaders + streamHeaders
    }

    // Auto-clear gesture HUD after 1.5 seconds
    LaunchedEffect(gestureIndicatorText) {
        if (gestureIndicatorText != null && !isLongPressSpeedBoost) {
            delay(1500)
            gestureIndicatorText = null
        }
    }

    // Auto-clear double-tap ripple after 700ms
    LaunchedEffect(doubleTapIndicator) {
        if (doubleTapIndicator != null) {
            delay(700)
            doubleTapIndicator = null
            doubleTapSide = null
        }
    }

    // Polling player state, position, codecs, and diagnostics
    LaunchedEffect(mpvViewRef) {
        var pollCounter = 0
        while (true) {
            val view = mpvViewRef
            if (view != null) {
                try {
                    val pos = view.mpv.getPropertyInt("time-pos") ?: 0
                    val dur = view.mpv.getPropertyInt("duration") ?: 0
                    val paused = view.mpv.getPropertyBoolean("pause") ?: false
                    val width = view.mpv.getPropertyInt("width") ?: 0
                    val height = view.mpv.getPropertyInt("height") ?: 0
                    val fpsVal = view.mpv.getPropertyDouble("container-fps")
                    val vCodec = view.mpv.getPropertyString("video-codec")
                    val aCodec = view.mpv.getPropertyString("audio-codec-name")
                    val drops = view.mpv.getPropertyInt("frame-drop-count") ?: 0
                    val cacheDur = view.mpv.getPropertyDouble("demuxer-cache-duration") ?: 0.0
                    val vol = view.mpv.getPropertyInt("volume") ?: 100

                    currentPosSec = pos
                    durationSec = dur
                    isPlaying = !paused
                    currentVolume = vol

                    if (width > 0 && height > 0) {
                        videoResolution = "${width}x${height}"
                    }
                    if (fpsVal != null && fpsVal > 0) {
                        videoFps = String.format("%.1f", fpsVal)
                    }
                    if (!vCodec.isNullOrBlank()) videoCodec = vCodec.uppercase()
                    if (!aCodec.isNullOrBlank()) audioCodec = aCodec.uppercase()
                    droppedFramesCount = drops.toString()
                    cacheDurationSec = "${cacheDur.roundToInt()}s"

                    val pausedForCache = view.mpv.getPropertyBoolean("paused-for-cache") ?: false

                    if (pausedForCache) {
                        isBuffering = true
                    } else if (dur > 0 || pos > 0 || (width > 0 && height > 0) || cacheDur > 0.1) {
                        isBuffering = false
                    } else {
                        pollCounter++
                        if (pollCounter >= 6) {
                            isBuffering = false
                        }
                    }
                } catch (_: Exception) {}
            }
            delay(400)
        }
    }

    // Load file whenever selected quality, stream URL, or subtitle changes
    LaunchedEffect(mpvViewRef, selectedQuality) {
        val view = mpvViewRef ?: return@LaunchedEffect
        val stream = selectedQuality ?: return@LaunchedEffect

        errorMessage = null
        errorDetails = null
        isBuffering = true
        currentPosSec = 0
        durationSec = 0

        try {
            // Apply User-Agent and HTTP header fields
            val ua = activeHeaders["User-Agent"] ?: BROWSER_UA
            view.mpv.setPropertyString("user-agent", ua)

            val customHeaders = activeHeaders.filterKeys { !it.equals("User-Agent", ignoreCase = true) }
            if (customHeaders.isNotEmpty()) {
                val headerString = customHeaders.entries.joinToString(",") { "${it.key}: ${it.value}" }
                view.mpv.setPropertyString("http-header-fields", headerString)
            }

            // Apply selected HW/SW decoder
            view.mpv.setPropertyString("hwdec", currentHwDec.mpvValue)

            // Start stream playback & unpause
            view.startPlayback(stream.url)
            try {
                view.mpv.setPropertyBoolean("pause", false)
            } catch (_: Exception) {}

            // Inject subtitle track if selected
            selectedSubtitle?.let { sub ->
                view.mpv.command("sub-add", sub.url, "select", sub.label)
            }
        } catch (e: Exception) {
            errorMessage = "MPV Playback Error"
            errorDetails = e.message ?: "Failed to initialize stream in MPV"
            isBuffering = false
        }
    }

    // Auto-hide controls after 3 seconds of inactivity when playing (Requirement #2)
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying && errorMessage == null) {
            delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_overlay")
    ) {
        // MPV Surface View (Engine Unchanged)
        AndroidView(
            factory = { ctx ->
                MpvVideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    initialize(ctx.filesDir.path, ctx.cacheDir.path)
                    mpvViewRef = this
                }
            },
            update = { view ->
                if (mpvViewRef == null) {
                    mpvViewRef = view
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Touch & Advanced Gesture Interaction Layer (Requirement #3)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x > screenWidth / 2f) {
                                // Double tap right = Forward 10s
                                mpvViewRef?.mpv?.command("seek", "10", "relative+exact")
                                currentPosSec = (currentPosSec + 10).coerceAtMost(durationSec)
                                doubleTapIndicator = "+10s"
                                doubleTapSide = "right"
                            } else {
                                // Double tap left = Rewind 10s
                                mpvViewRef?.mpv?.command("seek", "-10", "relative+exact")
                                currentPosSec = (currentPosSec - 10).coerceAtLeast(0)
                                doubleTapIndicator = "-10s"
                                doubleTapSide = "left"
                            }
                        },
                        onLongPress = {
                            isLongPressSpeedBoost = true
                            mpvViewRef?.mpv?.setPropertyDouble("speed", 1.5)
                            gestureIndicatorText = "1.5x Fast Forward ⚡"
                        },
                        onPress = {
                            try {
                                tryAwaitRelease()
                            } finally {
                                if (isLongPressSpeedBoost) {
                                    isLongPressSpeedBoost = false
                                    mpvViewRef?.mpv?.setPropertyDouble("speed", playbackSpeed.toDouble())
                                    gestureIndicatorText = null
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        val screenWidth = size.width
                        val isRightHalf = change.position.x >= (screenWidth / 2f)
                        val delta = -dragAmount / 350f

                        if (isRightHalf) {
                            // Right Half = Screen Brightness
                            val newBrightness = (currentBrightness + delta).coerceIn(0.05f, 1.0f)
                            currentBrightness = newBrightness
                            activity?.let { act ->
                                val lp = act.window.attributes
                                lp.screenBrightness = newBrightness
                                act.window.attributes = lp
                            }
                            gestureIndicatorText = "Brightness: ${(newBrightness * 100).roundToInt()}%"
                        } else {
                            // Left Half = Volume with Boost
                            val view = mpvViewRef
                            if (view != null) {
                                val volDelta = (delta * 100).roundToInt()
                                val newVol = (currentVolume + volDelta).coerceIn(0, 200)
                                view.mpv.setPropertyInt("volume", newVol)
                                currentVolume = newVol
                                gestureIndicatorText = if (newVol > 100) {
                                    "Volume: $newVol% (Boost ⚡)"
                                } else {
                                    "Volume: $newVol%"
                                }
                            }
                        }
                    }
                }
        )

        // Double-Tap Animated Indicator (Left / Right Ripple Badge)
        doubleTapIndicator?.let { text ->
            val isRight = doubleTapSide == "right"
            val scaleAnim by animateFloatAsState(
                targetValue = 1.15f,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label = "ripple_scale"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp),
                contentAlignment = if (isRight) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.5.dp, accent),
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scaleAnim)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isRight) FeatherIcons.RotateCw else FeatherIcons.RotateCcw,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Gesture HUD Overlay (Shows volume / brightness / boost indicators)
        gestureIndicatorText?.let { hudText ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 22.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val icon = when {
                        hudText.startsWith("Brightness") -> FeatherIcons.Sun
                        hudText.startsWith("Volume: 0%") -> FeatherIcons.VolumeX
                        hudText.startsWith("Volume") -> FeatherIcons.Volume2
                        else -> FeatherIcons.FastForward
                    }
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                    Text(
                        text = hudText,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Buffering Indicator (Requirement #6 - same application loading indicator style)
        if (isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF141418).copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(46.dp),
                            color = accent,
                            strokeWidth = 3.5.dp
                        )
                        Text(
                            text = "Buffering ${selectedQuality?.quality ?: "Stream"}...",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Error State Card Overlay
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = errorMessage ?: "MPV Playback Error",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = errorDetails ?: "Unable to stream media via MPV engine.",
                            color = Color(0xFFD1D5DB),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Switch to Software (FFmpeg) Decoder on error
                            if (currentHwDec == MpvHwDecMode.HW_MEDIACODEC) {
                                Button(
                                    onClick = {
                                        currentHwDec = MpvHwDecMode.SW_FFMPEG
                                        errorMessage = null
                                        errorDetails = null
                                        isBuffering = true
                                        mpvViewRef?.let { v ->
                                            v.mpv.setPropertyString("hwdec", "no")
                                            selectedQuality?.let { q -> v.startPlayback(q.url) }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                                ) {
                                    Icon(FeatherIcons.Cpu, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Try SW Decoder")
                                }
                            }

                            OutlinedButton(onClick = { showDiagnosticsDialog = true }) {
                                Icon(FeatherIcons.Sliders, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Diagnostics")
                            }

                            Button(
                                onClick = {
                                    errorMessage = null
                                    errorDetails = null
                                    isBuffering = true
                                    selectedQuality?.let { q -> mpvViewRef?.startPlayback(q.url) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry")
                            }

                            FilledTonalButton(onClick = onClose) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }

        // On-Screen Controls Overlay (Nuvio-style Top & Bottom Gradients, Requirements #1, #2, #5)
        AnimatedVisibility(
            visible = showControls && errorMessage == null,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Top Gradient Header (Requirement #5)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.9f),
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(38.dp).testTag("close_player_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // HW / SW Decoder Toggle Pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentHwDec == MpvHwDecMode.HW_MEDIACODEC) Color.White.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.35f),
                                modifier = Modifier.clickable {
                                    val nextMode = if (currentHwDec == MpvHwDecMode.HW_MEDIACODEC) {
                                        MpvHwDecMode.SW_FFMPEG
                                    } else {
                                        MpvHwDecMode.HW_MEDIACODEC
                                    }
                                    currentHwDec = nextMode
                                    mpvViewRef?.mpv?.setPropertyString("hwdec", nextMode.mpvValue)
                                    gestureIndicatorText = "Decoder: ${nextMode.label}"
                                }
                            ) {
                                Text(
                                    text = if (currentHwDec == MpvHwDecMode.HW_MEDIACODEC) "HW" else "SW",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Speed Button
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.clickable { showSpeedMenu = true }
                                ) {
                                    Text(
                                        text = "${playbackSpeed}x",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false }
                                ) {
                                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${spd}x",
                                                    fontWeight = if (playbackSpeed == spd) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                playbackSpeed = spd
                                                mpvViewRef?.mpv?.setPropertyDouble("speed", spd.toDouble())
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Subtitle Picker Button
                            if (subtitles.isNotEmpty()) {
                                Box {
                                    IconButton(
                                        onClick = { showSubtitleMenu = true },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Subtitles,
                                            contentDescription = "Subtitles",
                                            tint = if (selectedSubtitle != null) accent else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showSubtitleMenu,
                                        onDismissRequest = { showSubtitleMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Subtitles Off", fontWeight = if (selectedSubtitle == null) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                selectedSubtitle = null
                                                mpvViewRef?.mpv?.command("sub-remove")
                                                showSubtitleMenu = false
                                            }
                                        )
                                        subtitles.forEach { sub ->
                                            DropdownMenuItem(
                                                text = { Text(sub.label, fontWeight = if (selectedSubtitle == sub) FontWeight.Bold else FontWeight.Normal) },
                                                onClick = {
                                                    selectedSubtitle = sub
                                                    mpvViewRef?.mpv?.command("sub-add", sub.url, "select", sub.label)
                                                    showSubtitleMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Quality Button (Opens Quality Modal BottomSheet, Requirement #4)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = accent.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                                modifier = Modifier.clickable { showQualitySheet = true }
                            ) {
                                Text(
                                    text = selectedQuality?.quality ?: "Quality",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }

                            // Diagnostics
                            IconButton(
                                onClick = { showDiagnosticsDialog = true },
                                modifier = Modifier.size(34.dp).testTag("player_diagnostics_button")
                            ) {
                                Icon(FeatherIcons.MoreVertical, contentDescription = "More", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // 2. Center Large Play/Pause & Seek Controls (Requirement #1)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    IconButton(
                        onClick = {
                            mpvViewRef?.mpv?.command("seek", "-10", "relative+exact")
                            currentPosSec = (currentPosSec - 10).coerceAtLeast(0)
                            doubleTapIndicator = "-10s"
                            doubleTapSide = "left"
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(FeatherIcons.RotateCcw, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    IconButton(
                        onClick = {
                            val willPause = isPlaying
                            mpvViewRef?.mpv?.setPropertyBoolean("pause", willPause)
                            isPlaying = !willPause
                        },
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(accent)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) FeatherIcons.Pause else FeatherIcons.Play,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier
                                .size(34.dp)
                                .offset(x = if (isPlaying) 0.dp else 2.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            mpvViewRef?.mpv?.command("seek", "10", "relative+exact")
                            currentPosSec = (currentPosSec + 10).coerceAtMost(durationSec)
                            doubleTapIndicator = "+10s"
                            doubleTapSide = "right"
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(FeatherIcons.RotateCw, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                // 3. Bottom Gradient Controls & Timeline (Requirement #1)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.94f)
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Time, Tech Badges & Aspect Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatDurationSeconds(currentPosSec)} / ${formatDurationSeconds(durationSec)}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "MPV • $videoResolution",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )

                                // Aspect ratio toggle button (Requirement #1 - Fullscreen / Aspect)
                                IconButton(
                                    onClick = {
                                        val nextAspect = when (currentAspect) {
                                            MpvAspectRatio.ORIGINAL -> MpvAspectRatio.SIXTEEN_NINE
                                            MpvAspectRatio.SIXTEEN_NINE -> MpvAspectRatio.TWENTY_ONE_NINE
                                            MpvAspectRatio.TWENTY_ONE_NINE -> MpvAspectRatio.FOUR_THREE
                                            MpvAspectRatio.FOUR_THREE -> MpvAspectRatio.ORIGINAL
                                        }
                                        currentAspect = nextAspect
                                        mpvViewRef?.mpv?.setPropertyString("video-aspect-override", nextAspect.overrideValue)
                                        gestureIndicatorText = "Aspect: ${nextAspect.label}"
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(FeatherIcons.Maximize, contentDescription = "Aspect Ratio", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Thin Accent Seekbar (Requirement #1)
                        Slider(
                            value = if (durationSec > 0) (currentPosSec.toFloat() / durationSec.toFloat()).coerceIn(0f, 1f) else 0f,
                            onValueChange = { percent ->
                                val seekTo = (percent * durationSec).toInt()
                                currentPosSec = seekTo
                                mpvViewRef?.mpv?.command("seek", seekTo.toString(), "absolute+exact")
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("video_seekbar")
                        )
                    }
                }
            }
        }

        // Quality Selector BottomSheet (Requirement #4)
        if (showQualitySheet) {
            ModalBottomSheet(
                onDismissRequest = { showQualitySheet = false },
                containerColor = Color(0xFF1E1E24),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Select Video Quality",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    qualities.forEach { q ->
                        val isSelected = q == selectedQuality
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, if (isSelected) accent else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable {
                                    selectedQuality = q
                                    showQualitySheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = q.quality,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) accent else Color.White
                                    )
                                    Text(
                                        text = "${q.format} Stream",
                                        fontSize = 12.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                                if (isSelected) {
                                    Icon(FeatherIcons.Check, contentDescription = "Selected", tint = accent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }

        // Diagnostics Dialog Modal
        if (showDiagnosticsDialog) {
            val maskedUrl = remember(selectedQuality) {
                val u = selectedQuality?.url.orEmpty()
                u.replace(Regex("(?i)(token|api_key|key|auth|password)=([^&]+)"), "$1=****")
            }

            AlertDialog(
                onDismissRequest = { showDiagnosticsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(FeatherIcons.Compass, contentDescription = null, tint = accent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MPV Engine Diagnostics", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagnosticsItem("Core Player Engine", "libmpv 0.38+ (Native C++ / FFmpeg)")
                        DiagnosticsItem("Decoder Mode", currentHwDec.label)
                        DiagnosticsItem("Aspect Ratio", currentAspect.label)
                        DiagnosticsItem("Video Codec", "$videoCodec @ $videoFps FPS")
                        DiagnosticsItem("Audio Codec", audioCodec)
                        DiagnosticsItem("Resolution", videoResolution)
                        DiagnosticsItem("Position / Duration", "${formatDurationSeconds(currentPosSec)} / ${formatDurationSeconds(durationSec)}")
                        DiagnosticsItem("Demuxer Cache Buffer", cacheDurationSec)
                        DiagnosticsItem("Dropped Frames", droppedFramesCount)
                        DiagnosticsItem("Current Volume / Boost", "$currentVolume%")
                        DiagnosticsItem("Playback Speed", "${playbackSpeed}x")
                        DiagnosticsItem("Stream Quality", "${selectedQuality?.quality ?: "Unknown"} (${selectedQuality?.format ?: "MP4"})")
                        DiagnosticsItem("Stream URL", maskedUrl)
                        DiagnosticsItem("Headers Count", "${activeHeaders.size} custom headers injected")
                        if (errorMessage != null) {
                            DiagnosticsItem("Last Error", "$errorMessage - $errorDetails")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showDiagnosticsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                        Text("Done")
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mpvViewRef?.destroy()
            } catch (_: Exception) {}
        }
    }
}

@Composable
private fun DiagnosticsItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
        Text(text = value, fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace)
    }
}

private fun formatDurationSeconds(totalSeconds: Int): String {
    val sec = (totalSeconds % 60).coerceAtLeast(0)
    val min = ((totalSeconds / 60) % 60).coerceAtLeast(0)
    val hrs = (totalSeconds / 3600).coerceAtLeast(0)
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, min, sec)
    } else {
        String.format("%02d:%02d", min, sec)
    }
}
