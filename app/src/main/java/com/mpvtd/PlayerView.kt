package com.mpvtd

import android.content.Context
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVView
import kotlinx.coroutines.delay

/**
 * ============================================
 * MAIN PLAYER VIEW - MODIFY THIS FILE
 * ============================================
 * 
 * This is your main file for adding features:
 * - Add gesture controls
 * - Modify UI layout
 * - Add subtitle support
 * - Implement playlists
 * - Add settings
 * - Customize controls
 * 
 * Everything is in Jetpack Compose for easy modification!
 */

@Composable
fun PlayerView(
    videoUri: Uri?,
    hasPermission: Boolean,
    onPickVideo: () -> Unit,
    onClearVideo: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var mpvViewInstance by remember { mutableStateOf<MPVView?>(null) }
    
    val context = LocalContext.current
    
    // Auto-hide controls after 3 seconds
    LaunchedEffect(showControls) {
        if (showControls && videoUri != null) {
            delay(3000)
            showControls = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (videoUri != null) {
                            showControls = !showControls
                        }
                    }
                )
            }
    ) {
        // MPV Player Surface
        if (videoUri != null) {
            MPVPlayerSurface(
                videoUri = videoUri,
                context = context,
                onMpvReady = { mpvView ->
                    mpvViewInstance = mpvView
                },
                onPlaybackStateChanged = { playing ->
                    isPlaying = playing
                },
                onPositionChanged = { position ->
                    currentPosition = position
                },
                onDurationChanged = { dur ->
                    duration = dur
                }
            )
        }
        
        // Welcome Screen (when no video)
        if (videoUri == null) {
            WelcomeScreen(
                hasPermission = hasPermission,
                onPickVideo = onPickVideo
            )
        }
        
        // Player Controls Overlay
        if (videoUri != null) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PlayerControls(
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onPlayPause = {
                        MPVLib.command(arrayOf("cycle", "pause"))
                    },
                    onSeek = { seconds ->
                        MPVLib.command(arrayOf("seek", seconds.toString()))
                    },
                    onClose = {
                        MPVLib.command(arrayOf("stop"))
                        onClearVideo()
                    }
                )
            }
        }
    }
}

@Composable
fun MPVPlayerSurface(
    videoUri: Uri,
    context: Context,
    onMpvReady: (MPVView) -> Unit,
    onPlaybackStateChanged: (Boolean) -> Unit,
    onPositionChanged: (Long) -> Unit,
    onDurationChanged: (Long) -> Unit
) {
    var initialized by remember { mutableStateOf(false) }
    
    AndroidView(
        factory = { ctx ->
            MPVView(ctx, null).apply {
                // Initialize MPV
                initialize(ctx.applicationContext.filesDir.path)
                
                // Configure MPV for best performance
                configureMPV()
                
                // Set up observer
                addObserver(object : MPVLib.EventObserver {
                    override fun eventProperty(property: String) {}
                    
                    override fun eventProperty(property: String, value: Long) {
                        when (property) {
                            "time-pos" -> onPositionChanged(value)
                            "duration" -> onDurationChanged(value)
                        }
                    }
                    
                    override fun eventProperty(property: String, value: Boolean) {
                        if (property == "pause") {
                            onPlaybackStateChanged(!value)
                        }
                    }
                    
                    override fun eventProperty(property: String, value: String) {}
                    
                    override fun event(eventId: Int) {}
                })
                
                onMpvReady(this)
                initialized = true
            }
        },
        modifier = Modifier.fillMaxSize()
    )
    
    // Load video when initialized
    LaunchedEffect(initialized, videoUri) {
        if (initialized) {
            MPVLib.command(arrayOf("loadfile", videoUri.toString()))
        }
    }
}

@Composable
fun WelcomeScreen(
    hasPermission: Boolean,
    onPickVideo: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VideoLibrary,
            contentDescription = "MpvTD",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "MpvTD",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Simple & Powerful Video Player",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onPickVideo,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Open Video",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        if (!hasPermission) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Permission needed to access videos",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun PlayerControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
    ) {
        // Top Bar - Close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
            
            Text(
                text = "MpvTD",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            // Placeholder for future menu button
            IconButton(onClick = { /* TODO: Settings */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }
        }
        
        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            // Time Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 10s
                IconButton(
                    onClick = { onSeek(-10) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Play/Pause
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                // Forward 10s
                IconButton(
                    onClick = { onSeek(10) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

/**
 * ============================================
 * MPV CONFIGURATION
 * ============================================
 * Modify these settings to change playback behavior
 */
fun configureMPV() {
    // Video output
    MPVLib.setOptionString("vo", "gpu")
    MPVLib.setOptionString("gpu-context", "android")
    MPVLib.setOptionString("opengl-es", "yes")
    MPVLib.setOptionString("hwdec", "auto-safe")
    MPVLib.setOptionString("hwdec-codecs", "all")
    
    // Audio
    MPVLib.setOptionString("audio-channels", "stereo")
    MPVLib.setOptionString("audio-samplerate", "48000")
    
    // Playback
    MPVLib.setOptionString("keep-open", "yes")
    MPVLib.setOptionString("force-window", "yes")
    
    // Performance
    MPVLib.setOptionString("profile", "gpu-hq")
    MPVLib.setOptionString("video-sync", "audio")
    
    // Subtitles (basic support - expand as needed)
    MPVLib.setOptionString("sub-auto", "fuzzy")
    MPVLib.setOptionString("sub-file-paths", "ass:srt:sub:subs:subtitles")
    
    // TODO: Add more configurations here
    // - Custom video scaling
    // - Deinterlacing options
    // - Cache settings
    // - Screenshot options
}

/**
 * ============================================
 * UTILITY FUNCTIONS
 * ============================================
 */
fun formatTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

/**
 * ============================================
 * ADD YOUR FEATURES BELOW
 * ============================================
 * 
 * Ideas to implement:
 * 
 * 1. GESTURE CONTROLS:
 *    - Swipe for volume/brightness
 *    - Double-tap to skip
 *    - Pinch to zoom
 * 
 * 2. SUBTITLE SUPPORT:
 *    - Load external subtitle files
 *    - Subtitle track selection
 *    - Subtitle styling
 * 
 * 3. PLAYBACK SPEED:
 *    - Speed control UI
 *    - Presets (0.5x, 1x, 1.25x, 1.5x, 2x)
 * 
 * 4. AUDIO TRACKS:
 *    - Multi-audio track support
 *    - Track selection UI
 * 
 * 5. SETTINGS:
 *    - Hardware decoder toggle
 *    - Video quality presets
 *    - Theme selection
 * 
 * 6. ADVANCED:
 *    - Picture-in-Picture
 *    - Background audio
 *    - Resume playback
 *    - Video equalizer
 */
