package com.example.streambox.ui.enhanced

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.lifecycle.ViewModel
import com.example.streambox.StreamingSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedStreamingPlayerScreen(
    source: StreamingSource,
    viewModel: EnhancedPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val playerState by viewModel.playerState.collectAsState()

    // Initialize player when the screen is shown
    LaunchedEffect(source) {
        viewModel.initializePlayer(source, context)
    }

    // Auto-hide controls
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Video Player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            ) {
                // Top Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    // Video Info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = source.filename,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${source.quality} • ${source.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // More Options
                    IconButton(
                        onClick = { viewModel.showMoreOptions() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                }

                // Center Play/Pause
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(40.dp))
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Bottom Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    // Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatDuration(playerState.currentPosition),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )

                        Slider(
                            value = playerState.progress,
                            onValueChange = { viewModel.seekTo(it) },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        Text(
                            text = formatDuration(playerState.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quality Selector
                        TextButton(
                            onClick = { viewModel.showQualitySelector() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HighQuality, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(source.quality)
                            }
                        }

                        // Subtitles
                        TextButton(
                            onClick = { viewModel.showSubtitleSelector() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CC")
                            }
                        }

                        // Playback Speed
                        TextButton(
                            onClick = { viewModel.showSpeedSelector() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text("${playerState.playbackSpeed}x")
                        }

                        // Picture in Picture
                        IconButton(
                            onClick = { viewModel.enterPiPMode() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.PictureInPicture, contentDescription = "PiP", tint = Color.White)
                        }

                        // Fullscreen
                        IconButton(
                            onClick = { viewModel.toggleFullscreen() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (playerState.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Loading Indicator
        if (playerState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color.White,
                    strokeWidth = 4.dp
                )
            }
        }

        // Error Message
        if (playerState.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = playerState.error ?: "Unknown error",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = { viewModel.retry() }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun formatDuration(positionMs: Long): String {
    val seconds = positionMs / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
    }
}

data class PlayerState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val progress: Float = 0f,
    val playbackSpeed: Float = 1f,
    val isFullscreen: Boolean = false,
    val error: String? = null
)

data class PlayerUIState(
    val showQualitySelector: Boolean = false,
    val showSubtitleSelector: Boolean = false,
    val showSpeedSelector: Boolean = false,
    val showMoreOptions: Boolean = false
)

class EnhancedPlayerViewModel : ViewModel() {
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _uiState = MutableStateFlow(PlayerUIState())
    val uiState: StateFlow<PlayerUIState> = _uiState.asStateFlow()

    lateinit var exoPlayer: ExoPlayer

    fun initializePlayer(source: StreamingSource, context: Context) {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(source.link))
            prepare()
            playWhenReady = true
        }

        // Listen to player events
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> _playerState.update { it.copy(isLoading = true) }
                    Player.STATE_READY, Player.STATE_ENDED -> _playerState.update { it.copy(isLoading = false) }
                    Player.STATE_IDLE -> { /* handle idle state */ }
                }
            }
        })
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(progress: Float) {
        val position = (exoPlayer.duration * progress).toLong()
        exoPlayer.seekTo(position)
    }

    fun showQualitySelector() {
        _uiState.update { it.copy(showQualitySelector = true) }
    }

    fun showSubtitleSelector() {
        _uiState.update { it.copy(showSubtitleSelector = true) }
    }

    fun showSpeedSelector() {
        _uiState.update { it.copy(showSpeedSelector = true) }
    }

    fun showMoreOptions() {
        _uiState.update { it.copy(showMoreOptions = true) }
    }

    fun enterPiPMode() {
        // Implement PiP mode
    }

    fun toggleFullscreen() {
        _playerState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun retry() {
        // Retry playback
    }

    override fun onCleared() {
        super.onCleared()
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
    }
}
