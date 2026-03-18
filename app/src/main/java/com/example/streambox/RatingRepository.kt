package com.example.streambox

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RatingRepository {
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    suspend fun rateMovie(movieId: Int, rating: RatingType, movie: TmdbMovie) {
        val currentProfile = _userProfile.value
        val existingRatingIndex = currentProfile.ratings.indexOfFirst { it.movieId == movieId }
        
        val updatedRatings = if (existingRatingIndex >= 0) {
            currentProfile.ratings.toMutableList().apply {
                set(existingRatingIndex, MovieRating(movieId, rating))
            }
        } else {
            currentProfile.ratings + MovieRating(movieId, rating)
        }

        // Update genre preferences based on rating
        val updatedGenrePreferences = updateGenrePreferences(
            currentProfile.genrePreferences,
            movie.getGenreNames(),
            rating
        )

        // Update decade preferences
        val decade = movie.releaseDate?.take(4) ?: "Unknown"
        val updatedDecadePreferences = updatePreference(
            currentProfile.decadePreferences,
            decade,
            rating
        )

        val newProfile = currentProfile.copy(
            ratings = updatedRatings,
            genrePreferences = updatedGenrePreferences,
            decadePreferences = updatedDecadePreferences,
            ratingCount = updatedRatings.size
        )

        _userProfile.value = newProfile
        saveProfile(newProfile)
    }

    suspend fun removeRating(movieId: Int) {
        val currentProfile = _userProfile.value
        val updatedRatings = currentProfile.ratings.filter { it.movieId != movieId }
        
        val newProfile = currentProfile.copy(
            ratings = updatedRatings,
            ratingCount = updatedRatings.size
        )

        _userProfile.value = newProfile
        saveProfile(newProfile)
    }

    fun getMovieRating(movieId: Int): RatingType {
        return _userProfile.value.ratings
            .find { it.movieId == movieId }
            ?.rating ?: RatingType.NONE
    }

    private fun updateGenrePreferences(
        current: Map<String, Float>,
        genres: List<String>,
        rating: RatingType
    ): Map<String, Float> {
        val updated = current.toMutableMap()
        val weight = when (rating) {
            RatingType.SUPER_LIKE -> 2.0f
            RatingType.LIKE -> 1.0f
            RatingType.DISLIKE -> -0.5f
            RatingType.NONE -> 0f
        }

        genres.forEach { genre ->
            updated[genre] = (updated[genre] ?: 0f) + weight
        }

        return updated
    }

    private fun updatePreference(
        current: Map<String, Float>,
        key: String,
        rating: RatingType
    ): Map<String, Float> {
        val updated = current.toMutableMap()
        val weight = when (rating) {
            RatingType.SUPER_LIKE -> 2.0f
            RatingType.LIKE -> 1.0f
            RatingType.DISLIKE -> -0.5f
            RatingType.NONE -> 0f
        }

        updated[key] = (updated[key] ?: 0f) + weight
        return updated
    }

    private suspend fun saveProfile(profile: UserProfile) {
        // TODO: Implement persistent storage (DataStore/Room)
        // For now, keep in memory
    }

    suspend fun loadProfile() {
        // TODO: Load from persistent storage
        // For now, use default profile
    }
}

class RecommendationEngine(private val ratingRepository: RatingRepository) {
    
    suspend fun getPersonalizedRecommendations(
        allMovies: List<TmdbMovie>,
        limit: Int = 20
    ): List<RecommendationScore> {
        val profile = ratingRepository.userProfile.value
        val ratedMovieIds = profile.ratings.map { it.movieId }.toSet()
        
        val unratedMovies = allMovies.filter { it.id !in ratedMovieIds }
        
        return unratedMovies
            .map { movie ->
                val score = calculateRecommendationScore(movie, profile)
                RecommendationScore(movie, score.score, score.reasons)
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun calculateRecommendationScore(movie: TmdbMovie, profile: UserProfile): RecommendationScore {
        var score = 0f
        val reasons = mutableListOf<String>()

        // Genre-based scoring
        movie.getGenreNames().forEach { genre ->
            val genreWeight = profile.genrePreferences[genre] ?: 0f
            score += genreWeight * 10f
            if (genreWeight > 1f) {
                reasons.add("You like $genre movies")
            }
        }

        // Decade preference
        val decade = movie.releaseDate?.take(4) ?: "Unknown"
        val decadeWeight = profile.decadePreferences[decade] ?: 0f
        score += decadeWeight * 5f

        // Popularity boost
        score += movie.popularity.toFloat() * 0.1f
        if (movie.popularity > 50f) {
            reasons.add("Popular movie")
        }

        // Rating boost
        score += movie.voteAverage.toFloat() * 2f
        if (movie.voteAverage > 7.5f) {
            reasons.add("Highly rated (${movie.voteAverage}/10)")
        }

        // Recent movies boost
        val releaseYear = movie.releaseDate?.take(4)?.toIntOrNull()
        if (releaseYear != null && releaseYear >= 2020) {
            score += 5f
            reasons.add("Recent release")
        }

        // Avoid adult content if user has rated similar poorly
        if (movie.isAdult && profile.ratings.any { 
            it.rating == RatingType.DISLIKE 
        }) {
            score -= 20f
        }

        return RecommendationScore(movie, score, reasons)
    }

    suspend fun getTrendingForUser(
        trendingMovies: List<TmdbMovie>,
        limit: Int = 10
    ): List<TmdbMovie> {
        val personalized = getPersonalizedRecommendations(trendingMovies, limit * 2)
        
        // Mix of personalized and actual trending
        val personalizedMovies = personalized.map { it.movie }
        val remainingSlots = limit - personalizedMovies.size
        
        return if (remainingSlots > 0) {
            val actualTrending = trendingMovies
                .filter { it.id !in personalizedMovies.map { m -> m.id } }
                .take(remainingSlots)
            personalizedMovies + actualTrending
        } else {
            personalizedMovies.take(limit)
        }
    }
}
