package com.example.streambox.tv

data class TVShow(
    val id: Int,
    val name: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val firstAirDate: String?,
    val status: String, // "Returning Series", "Ended", "Canceled"
    val numberOfSeasons: Int?,
    val numberOfEpisodes: Int?,
    val genres: List<String>,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun getPosterUrl(): String? = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    fun getBackdropUrl(): String? = backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }
    fun getStatusDisplay(): String = when (status) {
        "Returning Series" -> "Ongoing"
        "Ended" -> "Completed"
        "Canceled" -> "Canceled"
        else -> status
    }
}

data class Season(
    val id: Int,
    val showId: Int,
    val seasonNumber: Int,
    val name: String,
    val overview: String?,
    val posterPath: String?,
    val airDate: String?,
    val episodeCount: Int,
    val voteAverage: Double
) {
    fun getPosterUrl(): String? = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
}

data class Episode(
    val id: Int,
    val showId: Int,
    val seasonId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String,
    val overview: String?,
    val stillPath: String?,
    val airDate: String?,
    val voteAverage: Double,
    val runtime: Int?, // minutes
    val productionCode: String?
) {
    fun getStillUrl(): String? = stillPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    fun getEpisodeDisplay(): String = "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
    fun getFullTitle(): String = "$name - ${getEpisodeDisplay()}"
}

data class WatchingProgress(
    val showId: Int,
    val seasonId: Int,
    val episodeId: Int,
    val watchedDuration: Long, // seconds
    val totalDuration: Long, // seconds
    val completionPercentage: Float,
    val lastWatchedAt: Long,
    val isCompleted: Boolean = completionPercentage >= 0.9f
) {
    fun getProgressDisplay(): String = "${(completionPercentage * 100).toInt()}%"
}

data class NextEpisodeInfo(
    val nextEpisode: Episode?,
    val daysUntilNext: Int?,
    val isNextEpisodeAvailable: Boolean
) {
    fun getNextEpisodeDisplay(): String {
        return if (nextEpisode != null) {
            val daysText = if (daysUntilNext != null && daysUntilNext > 0) {
                " in $daysUntilNext days"
            } else if (daysUntilNext != null && daysUntilNext == 0) {
                " today"
            } else {
                " available"
            }
            "Next: ${nextEpisode.getFullTitle()}$daysText"
        } else {
            "No upcoming episodes"
        }
    }
}
