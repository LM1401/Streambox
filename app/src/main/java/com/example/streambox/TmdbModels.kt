package com.example.streambox

import com.google.gson.annotations.SerializedName

data class TmdbResponse<T>(
    val results: List<T>
)

data class TmdbMovie(
    val id: Int,
    val title: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    val overview: String,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("adult") val isAdult: Boolean = false,
    @SerializedName("original_language") val originalLanguage: String = "en",
    @SerializedName("runtime") val runtime: Int? = null,
    @SerializedName("genres") val genres: List<Genre> = emptyList(),
    @SerializedName("production_companies") val productionCompanies: List<ProductionCompany> = emptyList(),
    @SerializedName("production_countries") val productionCountries: List<ProductionCountry> = emptyList(),
    @SerializedName("spoken_languages") val spokenLanguages: List<SpokenLanguage> = emptyList(),
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("popularity") val popularity: Double = 0.0,
    @SerializedName("budget") val budget: Long = 0L,
    @SerializedName("revenue") val revenue: Long = 0L,
    @SerializedName("status") val status: String = "Released",
    @SerializedName("tagline") val tagline: String? = null,
    // Database source information
    val databaseSource: String = "TMDB", // TMDB, IMDB, etc.
    val ageRating: String? = null, // PG, PG-13, R, etc.
    val contentWarnings: List<String> = emptyList() // Violence, Language, etc.
) {
    fun getPosterUrl(): String? = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    fun getBackdropUrl(): String? = backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }
    
    fun getAgeRatingDisplay(): String {
        return ageRating ?: when {
            isAdult -> "R"
            genres.any { it.name.contains("Animation", true) } -> "PG"
            genres.any { it.name.contains("Action", true) || it.name.contains("Thriller", true) } -> "PG-13"
            else -> "PG"
        }
    }
    
    fun getDurationDisplay(): String {
        return runtime?.let { 
            val hours = it / 60
            val minutes = it % 60
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        } ?: "Unknown"
    }
    
    fun getGenreNames(): List<String> = genres.take(3).map { it.name }
    
    fun getProductionInfo(): String {
        val companies = productionCompanies.take(2).map { it.name }
        val countries = productionCountries.take(2).map { it.name }
        return when {
            companies.isNotEmpty() && countries.isNotEmpty() -> "${companies.joinToString(", ")} • ${countries.joinToString(", ")}"
            companies.isNotEmpty() -> companies.joinToString(", ")
            countries.isNotEmpty() -> countries.joinToString(", ")
            else -> "Unknown"
        }
    }
}

data class Genre(
    val id: Int,
    val name: String
)

data class ProductionCompany(
    val id: Int,
    @SerializedName("logo_path") val logoPath: String?,
    val name: String,
    @SerializedName("origin_country") val originCountry: String
)

data class ProductionCountry(
    @SerializedName("iso_3166_1") val iso31661: String,
    val name: String
)

data class SpokenLanguage(
    @SerializedName("iso_639_1") val iso6391: String,
    @SerializedName("english_name") val englishName: String,
    val name: String
)

data class TmdbVideo(
    val key: String,
    val site: String,
    val type: String
)

data class PostCreditInfo(
    val hasPostCredit: Boolean,
    val scenes: List<String> // e.g., ["1h 52m", "2h 05m"]
)

// Rating System Models
enum class RatingType {
    LIKE, DISLIKE, SUPER_LIKE, NONE
}

data class MovieRating(
    val movieId: Int,
    val rating: RatingType,
    val timestamp: Long = System.currentTimeMillis(),
    val genreWeights: Map<String, Float> = emptyMap() // Learned genre preferences
)

data class UserProfile(
    val userId: String = "default_user",
    val ratings: List<MovieRating> = emptyList(),
    val genrePreferences: Map<String, Float> = emptyMap(),
    val actorPreferences: Map<String, Float> = emptyMap(),
    val directorPreferences: Map<String, Float> = emptyMap(),
    val decadePreferences: Map<String, Float> = emptyMap(),
    val ratingCount: Int = 0,
    val averageRatingGiven: Float = 0f
) {
    fun getTopGenres(limit: Int = 5): List<Pair<String, Float>> {
        return genrePreferences.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }
    
    fun getRatingStats(): Triple<Int, Int, Int> {
        val likes = ratings.count { it.rating == RatingType.LIKE }
        val dislikes = ratings.count { it.rating == RatingType.DISLIKE }
        val superLikes = ratings.count { it.rating == RatingType.SUPER_LIKE }
        return Triple(likes, dislikes, superLikes)
    }
}

data class RecommendationScore(
    val movie: TmdbMovie,
    val score: Float,
    val reasons: List<String> = emptyList()
)
