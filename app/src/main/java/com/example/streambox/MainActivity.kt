package com.example.streambox

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.compose.material3.Surface
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.streambox.RatingType
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.streambox.ui.theme.StreamboxTheme
import com.example.streambox.RatingRepository
import com.example.streambox.RecommendationEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.rememberCoroutineScope
import java.util.IllegalFormatException
import java.util.Locale

fun formatFileSize(bytes: Long): String {
    if (bytes < 0) return "0 B"
    
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return try {
        when {
            gb >= 1 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1 -> String.format(Locale.getDefault(), "%.1f KB", kb)
            else -> "$bytes B"
        }
    } catch (_: IllegalFormatException) {
        "$bytes B"
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @SuppressLint("UnsafeOptInUsageWarning")
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamboxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

enum class Screen { Home, Search, Movies, TVShows, Watchlist, Settings }

@SuppressLint("UnsafeOptInUsageWarning")
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    var selectedScreen by remember { mutableStateOf(Screen.Home) }
    val selectedMovie = viewModel.selectedMovie
    val contentFocusRequester = remember { FocusRequester() }

    NavigationDrawer(
        drawerContent = { drawerValue ->
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .fillMaxHeight()
                    .padding(12.dp)
                    .width(if (drawerValue == DrawerValue.Open) 280.dp else 80.dp)
            ) {
                if (drawerValue == DrawerValue.Open) {
                    Text(
                        text = "StreamBox",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Spacer(modifier = Modifier.height(56.dp))
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                val menuItems = listOf(
                    Triple(Screen.Home, Icons.Default.Home, "Home"),
                    Triple(Screen.Search, Icons.Default.Search, "Search"),
                    Triple(Screen.Movies, Icons.Default.PlayArrow, "Movies"),
                    Triple(Screen.TVShows, Icons.Default.Info, "TV Shows"),
                    Triple(Screen.Watchlist, Icons.Default.Star, "Watchlist"),
                    Triple(Screen.Settings, Icons.Default.Settings, "Settings")
                )

                menuItems.forEach { (screen, icon, labelText) ->
                    NavigationDrawerItem(
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen },
                        leadingContent = {
                            Icon(
                                imageVector = icon,
                                contentDescription = labelText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    ) {
                        if (drawerValue == DrawerValue.Open) {
                            Text(
                                text = labelText,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp)
                    .focusRequester(contentFocusRequester)
                    .focusable()
            ) {
                when (selectedScreen) {
                    Screen.Home -> HomeScreen(viewModel)
                    Screen.Search -> SearchScreen(viewModel)
                    Screen.Settings -> SettingsScreen(viewModel)
                    Screen.Watchlist -> WatchlistScreen(viewModel)
                    else -> PlaceholderScreen(selectedScreen.name)
                }
            }

            AnimatedVisibility(
                visible = selectedMovie != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedMovie?.let { movie ->
                    MovieDetailsScreen(movie = movie, viewModel = viewModel)
                }
            }
        }
    }
    
    // Request focus for the content area
    LaunchedEffect(Unit) {
        contentFocusRequester.requestFocus()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieDetailsScreen(movie: TmdbMovie, viewModel: MainViewModel) {
    val postCredit = viewModel.postCreditInfo
    val isInWatchlist = viewModel.watchList.contains(movie.id.toString())
    val availableTorrents = viewModel.availableTorrents
    val streamingSources = viewModel.streamingSources
    val isLoadingTorrents = viewModel.isLoadingTorrents

    BackHandler {
        // Handle back navigation
    }

    // Auto-search torrents when movie is selected
    LaunchedEffect(movie.id) {
        // Note: Torrent search functionality not implemented in MainViewModel
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)) {
        
        AsyncImage(
            model = movie.getBackdropUrl(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )
        
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = 1000f
                )
            )
        )

        if (viewModel.selectedStreamingSource != null) {
            StreamingPlayerScreen(source = viewModel.selectedStreamingSource!!, viewModel = viewModel)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(58.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .width(300.dp)
                        .height(450.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    AsyncImage(
                        model = movie.getPosterUrl(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = movie.releaseDate?.take(4) ?: "",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "⭐ ${movie.voteAverage}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Yellow
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = movie.overview,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 6,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    if (postCredit != null && postCredit.hasPostCredit) {
                        Column(
                            modifier = Modifier
                                .background(
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "POST-CREDIT SCENES",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.Yellow,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Scenes at: ${postCredit.scenes.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { 
                                if (streamingSources.isNotEmpty()) {
                                    // Play first available source - functionality not implemented
                                }
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = Color.White,
                                contentColor = Color.Black,
                                focusedContainerColor = MaterialTheme.colorScheme.primary,
                                focusedContentColor = Color.White
                            ),
                            enabled = streamingSources.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play Now", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { 
                                if (viewModel.watchList.contains(movie.id.toString())) {
                                    viewModel.removeFromWatchlist(movie.id.toString())
                                } else {
                                    viewModel.addToWatchlist(movie.id.toString())
                                }
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = Color.Gray.copy(alpha = 0.3f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isInWatchlist) "In Watchlist" else "Add to Watchlist")
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Torrent Sources Section
                    Text(
                        text = "Available Sources",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoadingTorrents) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Searching torrents...",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else if (availableTorrents.isNotEmpty()) {
                        TorrentSourcesList(
                            torrents = availableTorrents,
                            onTorrentSelected = { torrent ->
                                // Torrent addition functionality not implemented
                            }
                        )
                    } else {
                        Text(
                            text = "No torrents found for this movie",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Streaming Sources Section
                    if (streamingSources.isNotEmpty()) {
                        Text(
                            text = "Streaming Quality Options",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        StreamingSourcesList(
                            sources = streamingSources,
                            onSourceSelected = { source ->
                                // Source selection functionality not implemented
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(viewModel: MainViewModel) {
    var query by remember { mutableStateOf("") }
    
    LaunchedEffect(query) {
        delay(500)
        viewModel.searchTmdb(query)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(58.dp)
    ) {
        Text(text = "Search", style = MaterialTheme.typography.displayMedium, color = Color.White)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by title, actor, or genre...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.LightGray,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (viewModel.searchResults.isEmpty() && query.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No results found.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(180.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                gridItems(viewModel.searchResults) { movie ->
                    MovieCard(
                        movie = movie,
                        onClick = { viewModel.selectMovie(movie) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    var tokenInput by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(58.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.displayMedium, color = Color.White)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(text = "Real-Debrid Token", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(
            text = "Enter your API token from real-debrid.com/apitoken",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = tokenInput,
            onValueChange = { tokenInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter token...") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.LightGray,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = { viewModel.login(tokenInput) }) {
            Text("Save & Login")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        viewModel.rdUser?.let { user ->
            RDUserInfo(user)
        }
    }
}

@Composable
fun WatchlistScreen(viewModel: MainViewModel) {
    val watchList = viewModel.watchList
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(58.dp)
    ) {
        Text(text = "My Watchlist", style = MaterialTheme.typography.displayMedium, color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))
        
        if (watchList.isEmpty()) {
            Text(text = "Your watchlist is empty.", color = Color.Gray)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(180.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                gridItems(watchList.toList()) { movieId ->
                    // Find the movie from trending or upcoming lists
                    val movie = viewModel.trendingMovies.find { it.id.toString() == movieId } 
                        ?: viewModel.upcomingMovies.find { it.id.toString() == movieId }
                    movie?.let {
                        MovieCard(
                            movie = it,
                            onClick = { viewModel.selectMovie(it) },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$name Screen", style = MaterialTheme.typography.displayLarge, color = Color.White)
    }
}

@SuppressLint("UnsafeOptInUsageWarning")
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val trending = viewModel.trendingMovies
    val upcoming = viewModel.upcomingMovies
    val rdTorrents = viewModel.rdTorrents
    val error = viewModel.error
    var featuredMovie by remember { mutableStateOf<TmdbMovie?>(if (trending.isNotEmpty()) trending.first() else null) }
    
    // Rating system integration
    val ratingRepository = remember { RatingRepository() }
    val recommendationEngine = remember { RecommendationEngine(ratingRepository) }
    
    val userProfile by ratingRepository.userProfile.collectAsState()
    
    // Get personalized recommendations
    val personalizedTrending = remember(trending, userProfile) {
        if (userProfile.ratingCount > 0) {
            runBlocking {
                recommendationEngine.getTrendingForUser(trending, 10)
            }
        } else {
            trending.take(10)
        }
    }
    
    val personalizedUpcoming = remember(upcoming, userProfile) {
        if (userProfile.ratingCount > 0) {
            runBlocking {
                recommendationEngine.getPersonalizedRecommendations(upcoming, 10).map { it.movie }
            }
        } else {
            upcoming.take(10)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // Netflix-style Hero Section
        item {
            NetflixHeroSection(
                featuredMovie = featuredMovie,
                onMovieFocused = { movie ->
                    featuredMovie = movie
                },
                onMovieClicked = { movie -> viewModel.selectMovie(movie) }
            )
        }

        // Personalized Recommendations (if user has rated movies)
        if (userProfile.ratingCount > 0) {
            item {
                NetflixMovieRow(
                    title = "Recommended For You",
                    movies = runBlocking { 
                        recommendationEngine.getPersonalizedRecommendations(trending + upcoming, 8).map { it.movie }
                    },
                    onMovieClick = { movie -> 
                        featuredMovie = movie
                        viewModel.selectMovie(movie)
                    },
                    viewModel = viewModel
                )
            }
        }

        // Error Display
        if (error != null) {
            item {
                Text(
                    text = "Error: $error",
                    color = Color.Red,
                    modifier = Modifier.padding(58.dp)
                )
            }
        }

        // Trending Movies Row (Personalized)
        if (personalizedTrending.isNotEmpty()) {
            item {
                NetflixMovieRow(
                    title = if (userProfile.ratingCount > 0) "Trending For You" else "Trending Now",
                    movies = personalizedTrending,
                    onMovieClick = { movie -> 
                        featuredMovie = movie
                        viewModel.selectMovie(movie)
                    },
                    viewModel = viewModel
                )
            }
        }

        // Upcoming Movies Row (Personalized)
        if (personalizedUpcoming.isNotEmpty()) {
            item {
                NetflixMovieRow(
                    title = if (userProfile.ratingCount > 0) "You Might Like" else "Upcoming Releases",
                    movies = personalizedUpcoming,
                    onMovieClick = { movie -> 
                        featuredMovie = movie
                        viewModel.selectMovie(movie)
                    },
                    viewModel = viewModel
                )
            }
        }

        // Real-Debrid Torrents Section
        if (rdTorrents.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 58.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Real-Debrid Torrents",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = { viewModel.fetchTorrents() },
                            colors = ButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Refresh")
                        }
                    }
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 58.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(rdTorrents) { torrent ->
                            TorrentCard(
                                torrent = torrent,
                                onTorrentClicked = { 
                                    // Streaming sources functionality not implemented
                                },
                                onDeleteTorrent = { 
                                    viewModel.deleteTorrent(torrent.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        // User Profile Section (if user has rated movies)
        if (userProfile.ratingCount > 0) {
            item {
                UserProfileSection(userProfile = userProfile)
            }
        }
    }
}

@Composable
fun UserProfileSection(userProfile: UserProfile) {
    val (likes, dislikes, superLikes) = userProfile.getRatingStats()
    val topGenres = userProfile.getTopGenres(3)

    Column(
        modifier = Modifier
            .padding(horizontal = 58.dp, vertical = 24.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = "Your Taste Profile",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Rating Stats
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your Ratings (${userProfile.ratingCount} movies)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Super Likes
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⭐",
                            style = MaterialTheme.typography.displaySmall,
                            fontSize = 32.sp
                        )
                        Text(
                            text = superLikes.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Yellow
                        )
                        Text(
                            text = "Super Likes",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Likes
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "👍",
                            style = MaterialTheme.typography.displaySmall,
                            fontSize = 32.sp
                        )
                        Text(
                            text = likes.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Green
                        )
                        Text(
                            text = "Likes",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Dislikes
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "👎",
                            style = MaterialTheme.typography.displaySmall,
                            fontSize = 32.sp
                        )
                        Text(
                            text = dislikes.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Red
                        )
                        Text(
                            text = "Dislikes",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Top Genres
        if (topGenres.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Your Favorite Genres",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    topGenres.forEach { (genre, score) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { (score / 10f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageWarning")
@Composable
fun VideoBackdrop(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
        }
    }

    DisposableEffect(url) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = Modifier.fillMaxSize()
    )
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
}

@SuppressLint("UnsafeOptInUsageWarning")
@Composable
fun NetflixHeroSection(
    featuredMovie: TmdbMovie?,
    onMovieFocused: (TmdbMovie) -> Unit = {},
    onMovieClicked: (TmdbMovie) -> Unit = {}
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }

    // Auto-play trailer when movie is focused
    LaunchedEffect(featuredMovie?.id) {
        featuredMovie?.let { movie ->
            val trailerUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            exoPlayer.setMediaItem(MediaItem.fromUri(trailerUrl))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(500.dp)
    ) {
        // Video Background
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        startY = 100f,
                        endY = 500f
                    )
                )
        )

        // Movie Details Overlay
        featuredMovie?.let { movie ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(48.dp)
                    .fillMaxWidth(0.6f)
            ) {
                // Title
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Metadata Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Year
                    Text(
                        text = movie.releaseDate?.take(4) ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )

                    // Age Rating
                    Box(
                        modifier = Modifier
                            .background(
                                Color.Gray.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = movie.getAgeRatingDisplay(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }

                    // Duration
                    Text(
                        text = movie.getDurationDisplay(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )

                    // Rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⭐",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${movie.voteAverage}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Genres
                if (movie.getGenreNames().isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        movie.getGenreNames().forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.2f),
                                        shape = MaterialTheme.shapes.extraSmall
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Database Source and Production Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📊 ${movie.databaseSource}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    if (movie.getProductionInfo() != "Unknown") {
                        Text(
                            text = "🏢 ${movie.getProductionInfo()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Description
                Text(
                    text = movie.overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 3,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { onMovieClicked(movie) },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedContentColor = Color.White
                        ),
                        modifier = Modifier.size(width = 120.dp, height = 45.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onMovieClicked(movie) },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Gray.copy(alpha = 0.3f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(width = 120.dp, height = 45.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Info")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RDUserInfo(viewModel: MainViewModel) {
    val user = viewModel.rdUser
    val error = viewModel.error

    Column {
        if (user != null) {
            Text(
                text = "Logged in as: ${user.username}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Points: ${user.points}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        } else if (error != null) {
            Text(
                text = "Error: $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflixMovieRow(
    title: String, 
    movies: List<TmdbMovie>, 
    onMovieFocused: (TmdbMovie) -> Unit = {},
    onMovieClicked: (TmdbMovie) -> Unit = {},
    ratingRepository: RatingRepository? = null,
    scope: CoroutineScope = rememberCoroutineScope()
) {
    Column(modifier = Modifier.padding(vertical = 24.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 58.dp, vertical = 8.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 58.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                val currentRating = ratingRepository?.getMovieRating(movie.id) ?: RatingType.NONE
                
                NetflixMovieCard(
                    movie = movie,
                    onClick = { onMovieClicked(movie) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflixMovieCard(
    movie: TmdbMovie,
    onFocused: () -> Unit = {},
    onClick: () -> Unit,
    currentRating: RatingType = RatingType.NONE,
    onRatingChanged: (RatingType) -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(300.dp)
            .onFocusChanged { if (it.isFocused) onFocused() },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = Color.White.copy(alpha = 0.15f)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
                shape = MaterialTheme.shapes.medium
            )
        ),
        shape = ClickableSurfaceDefaults.shape()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Movie Poster
            AsyncImage(
                model = movie.getPosterUrl(),
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient Overlay for better text visibility
            Box(modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        ),
                        startY = 200f,
                        endY = 300f
                    )
                )
            )
            
            // Movie Title and Rating at Bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⭐",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Yellow
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${movie.voteAverage}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = movie.releaseDate?.take(4) ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rating Buttons
                RatingButtons(
                    currentRating = currentRating,
                    onRatingChanged = onRatingChanged,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RatingButtons(
    currentRating: RatingType,
    onRatingChanged: (RatingType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Dislike Button
        RatingButton(
            icon = "👎",
            isSelected = currentRating == RatingType.DISLIKE,
            selectedColor = Color.Red.copy(alpha = 0.8f),
            onClick = { 
                onRatingChanged(if (currentRating == RatingType.DISLIKE) RatingType.NONE else RatingType.DISLIKE)
            },
            modifier = Modifier.weight(1f)
        )

        // Like Button
        RatingButton(
            icon = "👍",
            isSelected = currentRating == RatingType.LIKE,
            selectedColor = Color.Green.copy(alpha = 0.8f),
            onClick = { 
                onRatingChanged(if (currentRating == RatingType.LIKE) RatingType.NONE else RatingType.LIKE)
            },
            modifier = Modifier.weight(1f)
        )

        // Super Like Button
        RatingButton(
            icon = "⭐",
            isSelected = currentRating == RatingType.SUPER_LIKE,
            selectedColor = Color.Yellow.copy(alpha = 0.9f),
            onClick = { 
                onRatingChanged(if (currentRating == RatingType.SUPER_LIKE) RatingType.NONE else RatingType.SUPER_LIKE)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RatingButton(
    icon: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(24.dp),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) selectedColor else Color.White.copy(alpha = 0.2f),
            focusedContainerColor = if (isSelected) selectedColor else Color.White.copy(alpha = 0.3f)
        ),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(MaterialTheme.shapes.extraSmall)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TorrentCard(torrent: Torrent, onTorrentClicked: (Torrent) -> Unit, onDeleteTorrent: () -> Unit) {
    Surface(
        onClick = { onTorrentClicked(torrent) },
        modifier = Modifier
            .width(300.dp)
            .height(120.dp),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.1f),
            focusedContainerColor = Color.White.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = torrent.filename,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = torrent.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (torrent.status.lowercase()) {
                            "downloaded", "finished" -> Color.Green
                            "downloading" -> Color.Yellow
                            "error" -> Color.Red
                            else -> Color.Gray
                        }
                    )
                }
                
                Surface(
                    onClick = { onDeleteTorrent() },
                    modifier = Modifier.size(32.dp),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Red.copy(alpha = 0.7f),
                        focusedContainerColor = Color.Red
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "×",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${torrent.progress}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
                
                Text(
                    text = formatFileSize(torrent.bytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }
            
            if (torrent.status.lowercase() == "downloading") {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), shape = MaterialTheme.shapes.extraSmall)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(torrent.progress / 100f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.extraSmall)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieCard(posterUrl: String? = null, onFocused: () -> Unit = {}, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .height(260.dp)
            .onFocusChanged { if (it.isFocused) onFocused() },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = Color.White.copy(alpha = 0.1f)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                shape = MaterialTheme.shapes.small
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = posterUrl ?: "https://image.tmdb.org/t/p/w500/1E5baAaEse26fej7uHcjS3KyR94.jpg",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Box(modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                        startY = 400f
                    )
                )
            )
        }
    }
}

@SuppressLint("UnsafeOptInUsageWarning")
@Composable
fun StreamingPlayerScreen(source: StreamingSource, viewModel: MainViewModel) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    BackHandler {
        viewModel.selectedStreamingSource = null
        exoPlayer.stop()
    }

    LaunchedEffect(source.link) {
        if (source.link.startsWith("http")) {
            exoPlayer.setMediaItem(MediaItem.fromUri(source.link))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Back button
        Surface(
            onClick = { viewModel.selectedStreamingSource = null },
            modifier = Modifier
                .padding(16.dp)
                .size(48.dp),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                focusedContainerColor = Color.Black.copy(alpha = 0.7f)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }
        
        // Video info overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = source.filename,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "${source.quality} • ${source.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun TorrentSourcesList(
    torrents: List<TorrentResult>,
    onTorrentSelected: (TorrentResult) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(torrents) { torrent ->
            TorrentResultCard(
                torrent = torrent,
                onClick = { onTorrentSelected(torrent) }
            )
        }
    }
}

@Composable
fun TorrentResultCard(
    torrent: TorrentResult,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.1f),
            focusedContainerColor = Color.White.copy(alpha = 0.2f)
        ),
        shape = ClickableSurfaceDefaults.shape()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = torrent.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📱 ${torrent.quality ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Yellow
                    )
                    Text(
                        text = "📊 ${formatFileSize(torrent.size)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Text(
                        text = "⬆️ ${torrent.seeders}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Green
                    )
                    Text(
                        text = "⬇️ ${torrent.leechers}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Select Torrent",
                tint = Color.White
            )
        }
    }
}

@Composable
fun StreamingSourcesList(
    sources: List<StreamingSource>,
    onSourceSelected: (StreamingSource) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sources) { source ->
            Surface(
                onClick = { onSourceSelected(source) },
                modifier = Modifier.fillMaxWidth(),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = ClickableSurfaceDefaults.shape()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = source.quality,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = source.size,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun RDUserInfo(user: User) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome, ${user.username}!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Email: ${user.email}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Premium Status: ${if (user.premium > 0) "Active" else "Inactive"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Points: ${user.points}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> "%.1f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        kb >= 1 -> "%.1f KB".format(kb)
        else -> "$bytes B"
    }
}
}
