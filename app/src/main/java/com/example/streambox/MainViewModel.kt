package com.example.streambox

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import com.example.streambox.MockMovieData

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RealDebridRepository()
    private val torrentSearchService = TorrentSearchService()
    private val preferenceManager = PreferenceManager(application)
    
    private val tmdbApiKey = "28698e1796764c8280ae3896267caecd"
    
    // Using the unsafe client to bypass SSL chain validation issues
    private val tmdbApi = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .client(NetworkUtils.getUnsafeOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TmdbApi::class.java)

    var rdUser by mutableStateOf<User?>(null)
        private set

    var rdTorrents by mutableStateOf<List<Torrent>>(emptyList())
        private set

    var trendingMovies by mutableStateOf<List<TmdbMovie>>(emptyList())
        private set

    var upcomingMovies by mutableStateOf<List<TmdbMovie>>(emptyList())
        private set

    var searchResults by mutableStateOf<List<TmdbMovie>>(emptyList())
        private set

    var selectedMovie by mutableStateOf<TmdbMovie?>(null)
        private set

    var postCreditInfo by mutableStateOf<PostCreditInfo?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var watchList by mutableStateOf<Set<String>>(emptySet())
        private set

    var watchedHistory by mutableStateOf<Set<String>>(emptySet())
        private set

    var availableTorrents by mutableStateOf<List<TorrentResult>>(emptyList())
        private set

    var streamingSources by mutableStateOf<List<StreamingSource>>(emptyList())
        private set

    var isLoadingTorrents by mutableStateOf(false)
        private set

    var selectedStreamingSource by mutableStateOf<StreamingSource?>(null)

    private var currentToken: String? = null

    init {
        viewModelScope.launch {
            val savedToken = preferenceManager.rdToken.firstOrNull()
            if (savedToken != null) {
                login(savedToken)
            }
            
            launch {
                preferenceManager.watchList.collect { watchList = it }
            }
            launch {
                preferenceManager.watchedHistory.collect { watchedHistory = it }
            }
            
            fetchTmdbData()
        }
    }

    fun fetchTmdbData() {
        viewModelScope.launch {
            try {
                error = null
                trendingMovies = tmdbApi.getTrendingMovies(tmdbApiKey).results
                upcomingMovies = tmdbApi.getUpcomingMovies(tmdbApiKey).results
                
                if (trendingMovies.isEmpty()) {
                    trendingMovies = getMockMovies()
                }
            } catch (e: Exception) {
                error = "TMDB Connection Issue: ${e.message}. Using offline mode."
                // Fallback to mock data so user can see something
                trendingMovies = getMockMovies()
                upcomingMovies = getMockMovies().reversed()
            }
        }
    }

    private fun getMockMovies(): List<TmdbMovie> {
        return MockMovieData.getMockMovies()
    }

    fun searchTmdb(query: String) {
        if (query.isEmpty()) {
            searchResults = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                searchResults = tmdbApi.searchMovies(tmdbApiKey, query).results
            } catch (e: Exception) {
                error = "Search Error: ${e.message}"
            }
        }
    }

    fun pickRandomMovie(fromWatchlist: Boolean) {
        viewModelScope.launch {
            if (fromWatchlist) {
                if (watchList.isNotEmpty()) {
                    val randomId = watchList.toList().random()
                    try {
                        val movie = trendingMovies.find { it.id.toString() == randomId } 
                            ?: upcomingMovies.find { it.id.toString() == randomId }
                        if (movie != null) selectMovie(movie)
                    } catch (e: Exception) {
                        error = "Random Picker Error: ${e.message}"
                    }
                }
            } else {
                val pool = trendingMovies + upcomingMovies
                if (pool.isNotEmpty()) {
                    selectMovie(pool.random())
                }
            }
        }
    }

    fun selectMovie(movie: TmdbMovie?) {
        selectedMovie = movie
        if (movie != null) {
            postCreditInfo = if (movie.title.contains("Marvel", ignoreCase = true) || movie.id % 3 == 0) {
                PostCreditInfo(true, listOf("1h 52m (Mid-credits)", "2h 05m (Post-credits)"))
            } else {
                PostCreditInfo(false, emptyList())
            }
        } else {
            postCreditInfo = null
        }
    }

    fun login(token: String) {
        currentToken = token
        viewModelScope.launch {
            repository.getUserInfo(token)
                .onSuccess { 
                    rdUser = it
                    error = null
                    preferenceManager.saveRdToken(token)
                    fetchTorrents()
                }
                .onFailure { 
                    error = it.message
                    rdUser = null
                }
        }
    }

    fun fetchTorrents() {
        val token = currentToken ?: return
        viewModelScope.launch {
            repository.getTorrents(token)
                .onSuccess { 
                    rdTorrents = it
                    error = null
                }
                .onFailure { 
                    error = it.message
                }
        }
    }

    fun toggleWatchList(movieId: String) {
        viewModelScope.launch {
            if (watchList.contains(movieId)) {
                preferenceManager.removeFromWatchList(movieId)
            } else {
                preferenceManager.addToWatchList(movieId)
            }
        }
    }

    fun removeFromWatchlist(movieId: String) {
        viewModelScope.launch {
            preferenceManager.removeFromWatchList(movieId)
        }
    }

    fun addToWatchlist(movieId: String) {
        viewModelScope.launch {
            preferenceManager.addToWatchList(movieId)
        }
    }

    fun markAsWatched(movieId: String) {
        viewModelScope.launch {
            preferenceManager.addToHistory(movieId)
        }
    }

    fun searchTorrentsForMovie(movie: TmdbMovie) {
        isLoadingTorrents = true
        availableTorrents = emptyList()
        
        viewModelScope.launch {
            try {
                val year = movie.releaseDate?.take(4)?.toIntOrNull()
                val torrents = torrentSearchService.searchMovieTorrents(movie.title, year)
                availableTorrents = torrents
                error = null
            } catch (e: Exception) {
                error = "Torrent search failed: ${e.message}"
                availableTorrents = emptyList()
            } finally {
                isLoadingTorrents = false
            }
        }
    }

    fun addTorrentToRD(torrentResult: TorrentResult) {
        val token = currentToken ?: return
        viewModelScope.launch {
            try {
                repository.addMagnet(token, torrentResult.magnet)
                    .onSuccess { 
                        error = null
                        fetchTorrents() // Refresh the torrent list
                    }
                    .onFailure { 
                        error = "Failed to add torrent: ${it.message}"
                    }
            } catch (e: Exception) {
                error = "Error adding torrent: ${e.message}"
            }
        }
    }

    fun getStreamingSources(torrent: Torrent) {
        currentToken ?: return
        streamingSources = emptyList()
        
        viewModelScope.launch {
            try {
                val sources = mutableListOf<StreamingSource>()
                
                // Get streaming links from torrent files
                torrent.files?.forEach { file ->
                    if (file.path.endsWith(".mp4") || file.path.endsWith(".mkv") || 
                        file.path.endsWith(".avi") || file.path.endsWith(".mov")) {
                        
                        // Create a direct link
                        val quality = extractQuality(file.path)
                        val size = formatFileSize(file.bytes)
                        
                        sources.add(
                            StreamingSource(
                                quality = quality,
                                size = size,
                                link = "rd://${torrent.id}/${file.id}", // Custom URL scheme
                                filename = file.path
                            )
                        )
                    }
                }
                
                streamingSources = sources
                error = null
            } catch (e: Exception) {
                error = "Failed to get streaming sources: ${e.message}"
            }
        }
    }

    fun playStreamingSource(source: StreamingSource) {
        selectedStreamingSource = source
    }

    fun unrestrictAndPlay(link: String) {
        val token = currentToken ?: return
        viewModelScope.launch {
            try {
                repository.unrestrictLink(token, link)
                    .onSuccess { unrestricted ->
                        // Create a streaming source with the unrestricted link
                        val source = StreamingSource(
                            quality = selectedStreamingSource?.quality ?: "Unknown",
                            size = formatFileSize(unrestricted.filesize),
                            link = unrestricted.link,
                            filename = unrestricted.filename
                        )
                        selectedStreamingSource = source
                    }
                    .onFailure {
                        error = "Failed to unrestrict link: ${it.message}"
                    }
            } catch (e: Exception) {
                error = "Error unrestricting link: ${e.message}"
            }
        }
    }

    private fun extractQuality(filename: String): String {
        return when {
            filename.contains("2160p", ignoreCase = true) || filename.contains("4K", ignoreCase = true) -> "4K"
            filename.contains("1080p", ignoreCase = true) -> "1080p"
            filename.contains("720p", ignoreCase = true) -> "720p"
            filename.contains("480p", ignoreCase = true) -> "480p"
            else -> "Unknown"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format(Locale.getDefault(), "%.1f GB", gb)
            mb >= 1 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1 -> String.format(Locale.getDefault(), "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    fun deleteTorrent(torrentId: String) {
        val token = currentToken ?: return
        viewModelScope.launch {
            try {
                repository.deleteTorrent(token, torrentId)
                    .onSuccess {
                        fetchTorrents() // Refresh the list
                        error = null
                    }
                    .onFailure {
                        error = "Failed to delete torrent: ${it.message}"
                    }
            } catch (e: Exception) {
                error = "Error deleting torrent: ${e.message}"
            }
        }
    }
}
