package com.example.streambox

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.streambox.enhanced.EnhancedRealDebridRepository
import com.example.streambox.enhanced.UserSubscription
import com.example.streambox.enhanced.UserTraffic
import com.example.streambox.enhanced.UserStats
import com.example.streambox.settings.UserPreferences
import com.example.streambox.tv.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class UltimateMainViewModel(application: Application) : AndroidViewModel(application) {
    // Core Services
    private val repository = RealDebridRepository()
    private val enhancedRepository = EnhancedRealDebridRepository()
    private val multiSourceSearch = MultiSourceTorrentSearchService()
    private val preferenceManager = PreferenceManager(application)
    
    // TMDB API
    private val tmdbApiKey = "28698e1796764c8280ae3896267caecd"
    private val tmdbApi = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TmdbApi::class.java)

    // User State
    private val _rdUser = MutableStateFlow<User?>(null)
    val rdUser: StateFlow<User?> = _rdUser.asStateFlow()

    private val _rdSubscription = MutableStateFlow<UserSubscription?>(null)
    val rdSubscription: StateFlow<UserSubscription?> = _rdSubscription.asStateFlow()

    private val _rdTraffic = MutableStateFlow<UserTraffic?>(null)
    val rdTraffic: StateFlow<UserTraffic?> = _rdTraffic.asStateFlow()

    private val _rdStats = MutableStateFlow<UserStats?>(null)
    val rdStats: StateFlow<UserStats?> = _rdStats.asStateFlow()

    // Content State
    private val _trendingMovies = MutableStateFlow<List<TmdbMovie>>(emptyList())
    val trendingMovies: StateFlow<List<TmdbMovie>> = _trendingMovies.asStateFlow()

    private val _upcomingMovies = MutableStateFlow<List<TmdbMovie>>(emptyList())
    val upcomingMovies: StateFlow<List<TmdbMovie>> = _upcomingMovies.asStateFlow()

    private val _tvShows = MutableStateFlow<List<TVShow>>(emptyList())
    val tvShows: StateFlow<List<TVShow>> = _tvShows.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TmdbMovie>>(emptyList())
    val searchResults: StateFlow<List<TmdbMovie>> = _searchResults.asStateFlow()

    private val _tvSearchResults = MutableStateFlow<List<TVShow>>(emptyList())
    val tvSearchResults: StateFlow<List<TVShow>> = _tvSearchResults.asStateFlow()

    // Torrent State
    private val _availableTorrents = MutableStateFlow<List<TorrentResult>>(emptyList())
    val availableTorrents: StateFlow<List<TorrentResult>> = _availableTorrents.asStateFlow()

    private val _rdTorrents = MutableStateFlow<List<Torrent>>(emptyList())
    val rdTorrents: StateFlow<List<Torrent>> = _rdTorrents.asStateFlow()

    private val _streamingSources = MutableStateFlow<List<StreamingSource>>(emptyList())
    val streamingSources: StateFlow<List<StreamingSource>> = _streamingSources.asStateFlow()

    // UI State
    private val _selectedMovie = MutableStateFlow<TmdbMovie?>(null)
    val selectedMovie: StateFlow<TmdbMovie?> = _selectedMovie.asStateFlow()

    private val _selectedTVShow = MutableStateFlow<TVShow?>(null)
    val selectedTVShow: StateFlow<TVShow?> = _selectedTVShow.asStateFlow()

    private val _selectedStreamingSource = MutableStateFlow<StreamingSource?>(null)
    val selectedStreamingSource: StateFlow<StreamingSource?> = _selectedStreamingSource.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // User Preferences
    private val _preferences = MutableStateFlow(UserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    // Watchlist and History
    private val _watchlist = MutableStateFlow<Set<String>>(emptySet())
    val watchlist: StateFlow<Set<String>> = _watchlist.asStateFlow()

    private val _viewingHistory = MutableStateFlow<List<String>>(emptyList())
    val viewingHistory: StateFlow<List<String>> = _viewingHistory.asStateFlow()

    // Search Filters
    private val _searchFilters = MutableStateFlow(MultiSourceTorrentSearchService.SearchFilters())
    val searchFilters: StateFlow<MultiSourceTorrentSearchService.SearchFilters> = _searchFilters.asStateFlow()

    private var currentToken: String? = null

    init {
        viewModelScope.launch {
            // Load saved token and preferences
            val savedToken = preferenceManager.rdToken.firstOrNull()
            if (savedToken != null) {
                login(savedToken)
            }
            
            // Load preferences
            loadPreferences()
            
            // Initialize data
            fetchTrendingContent()
            fetchUserStats()
            
            // Set up periodic updates
            setupPeriodicUpdates()
        }
    }

    // Authentication
    fun login(token: String) {
        currentToken = token
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Get user info
                enhancedRepository.getUserInfo(token)
                    .onSuccess { user ->
                        _rdUser.value = user
                        preferenceManager.saveRdToken(token)
                    }
                    .onFailure { e ->
                        _error.value = "Login failed: ${e.message}"
                    }
                
                // Get subscription info
                enhancedRepository.getUserSubscription(token)
                    .onSuccess { subscription ->
                        _rdSubscription.value = subscription
                    }
                
                // Get traffic info
                enhancedRepository.getUserTraffic(token)
                    .onSuccess { traffic ->
                        _rdTraffic.value = traffic
                    }
                
                // Fetch torrents
                fetchTorrents()
                
            } catch (e: Exception) {
                _error.value = "Login error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Content Fetching
    private fun fetchTrendingContent() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Fetch movies
                val moviesResponse = tmdbApi.getTrendingMovies(tmdbApiKey)
                val upcomingResponse = tmdbApi.getUpcomingMovies(tmdbApiKey)
                
                _trendingMovies.value = moviesResponse.results
                _upcomingMovies.value = upcomingResponse.results
                
            } catch (e: Exception) {
                _error.value = "Failed to fetch content: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchUserStats() {
        val token = currentToken ?: return
        viewModelScope.launch {
            try {
                enhancedRepository.getUserStats(token)
                    .onSuccess { stats ->
                        _rdStats.value = stats
                    }
            } catch (e: Exception) {
                // Stats are not critical, so don't show error
            }
        }
    }

    private fun fetchTorrents() {
        val token = currentToken ?: return
        viewModelScope.launch {
            try {
                enhancedRepository.getTorrents(token)
                    .onSuccess { torrents ->
                        _rdTorrents.value = torrents
                    }
                    .onFailure { e ->
                        _error.value = "Failed to fetch torrents: ${e.message}"
                    }
            } catch (e: Exception) {
                _error.value = "Torrent fetch error: ${e.message}"
            }
        }
    }

    // Movie Selection and Torrent Search
    fun selectMovie(movie: TmdbMovie) {
        _selectedMovie.value = movie
        _selectedTVShow.value = null
        searchTorrentsForMovie(movie)
    }

    fun selectTVShow(show: TVShow) {
        _selectedTVShow.value = show
        _selectedMovie.value = null
        // TODO: Fetch episodes and seasons
    }

    fun searchTorrentsForMovie(movie: TmdbMovie) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _availableTorrents.value = emptyList()
                
                val year = movie.releaseDate?.take(4)?.toIntOrNull()
                val torrents = multiSourceSearch.searchMovieTorrents(
                    movieTitle = movie.title,
                    year = year,
                    filters = _searchFilters.value
                )
                
                _availableTorrents.value = torrents
                
            } catch (e: Exception) {
                _error.value = "Torrent search failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Search Functions
    fun searchMovies(query: String) {
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = tmdbApi.searchMovies(tmdbApiKey, query)
                _searchResults.value = response.results
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchTVShows(query: String) {
        // TODO: Implement TV show search
    }

    // Torrent Management
    fun addTorrentToRD(torrentResult: TorrentResult) {
        val token = currentToken ?: return
        viewModelScope.launch {
            try {
                repository.addMagnet(token, torrentResult.magnet)
                    .onSuccess { 
                        _error.value = null
                        fetchTorrents() // Refresh the torrent list
                    }
                    .onFailure { 
                        _error.value = "Failed to add torrent: ${it.message}"
                    }
            } catch (e: Exception) {
                _error.value = "Error adding torrent: ${e.message}"
            }
        }
    }

    fun getStreamingSources(torrent: Torrent) {
        currentToken ?: return
        viewModelScope.launch {
            try {
                val sources = mutableListOf<StreamingSource>()
                
                // Get streaming links from torrent files
                torrent.files?.forEach { file ->
                    if (file.path.endsWith(".mp4") || file.path.endsWith(".mkv") || 
                        file.path.endsWith(".avi") || file.path.endsWith(".mov")) {
                        
                        val quality = extractQuality(file.path)
                        val size = formatFileSize(file.bytes)
                        
                        sources.add(
                            StreamingSource(
                                quality = quality,
                                size = size,
                                link = "rd://${torrent.id}/${file.id}",
                                filename = file.path
                            )
                        )
                    }
                }
                
                _streamingSources.value = sources
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to get streaming sources: ${e.message}"
            }
        }
    }

    fun playStreamingSource(source: StreamingSource) {
        _selectedStreamingSource.value = source
    }

    fun unrestrictAndPlay(link: String) {
        val token = currentToken ?: return
        viewModelScope.launch {
            try {
                repository.unrestrictLink(token, link)
                    .onSuccess { unrestricted ->
                        val source = StreamingSource(
                            quality = _selectedStreamingSource.value?.quality ?: "Unknown",
                            size = formatFileSize(unrestricted.filesize),
                            link = unrestricted.link,
                            filename = unrestricted.filename
                        )
                        _selectedStreamingSource.value = source
                    }
                    .onFailure {
                        _error.value = "Failed to unrestrict link: ${it.message}"
                    }
            } catch (e: Exception) {
                _error.value = "Error unrestricting link: ${e.message}"
            }
        }
    }

    // Watchlist Management
    fun toggleWatchList(movieId: String) {
        val currentWatchlist = _watchlist.value.toMutableSet()
        if (currentWatchlist.contains(movieId)) {
            currentWatchlist.remove(movieId)
        } else {
            currentWatchlist.add(movieId)
        }
        _watchlist.value = currentWatchlist
    }

    // Viewing History
    fun markAsWatched(movieId: String) {
        val currentHistory = _viewingHistory.value.toMutableList()
        if (!currentHistory.contains(movieId)) {
            currentHistory.add(movieId)
        }
        _viewingHistory.value = currentHistory
    }

    // Search Filters
    fun updateSearchFilters(filters: MultiSourceTorrentSearchService.SearchFilters) {
        _searchFilters.value = filters
        // Re-search if a movie is selected
        _selectedMovie.value?.let { movie ->
            searchTorrentsForMovie(movie)
        }
    }

    // Preferences
    private fun loadPreferences() {
        viewModelScope.launch {
            // TODO: Load from DataStore
            _preferences.value = UserPreferences()
        }
    }

    fun updatePreferences(updates: UserPreferences.() -> UserPreferences) {
        _preferences.value = _preferences.value.updates()
        // TODO: Save to DataStore
    }

    // Utility Functions
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

    // Periodic Updates
    private fun setupPeriodicUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000) // 5 minutes
                fetchTorrents()
                fetchUserStats()
            }
        }
    }

    // Cleanup
    override fun onCleared() {
        super.onCleared()
        // Cancel all coroutines
        viewModelScope.cancel()
    }
}
