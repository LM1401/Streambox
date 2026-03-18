package com.example.streambox

import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.converter.gson.GsonConverterFactory

interface TorrentApi {
    @GET("search")
    suspend fun searchTorrents(
        @Query("q") query: String,
        @Query("sort") sort: String = "seeders",
        @Query("limit") limit: Int = 20
    ): List<TorrentResult>
}

data class TorrentResult(
    val id: String,
    val name: String,
    val magnet: String,
    val size: Long,
    val seeders: Int,
    val leechers: Int,
    val quality: String?,
    val source: String
)

class TorrentSearchService {
    // Using YTS API as an example - you can add more sources
    private val ytsApi = Retrofit.Builder()
        .baseUrl("https://yts.mx/api/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(YtsApi::class.java)

    suspend fun searchMovieTorrents(movieTitle: String, year: Int? = null): List<TorrentResult> {
        val results = mutableListOf<TorrentResult>()
        
        // Search YTS
        try {
            val ytsResponse = ytsApi.searchMovies(movieTitle, year)
            ytsResponse.data.movies?.forEach { movie ->
                movie.torrents.forEach { torrent ->
                    results.add(
                        TorrentResult(
                            id = movie.id.toString(),
                            name = movie.title,
                            magnet = torrent.url,
                            size = torrent.size_bytes,
                            seeders = torrent.seeds,
                            leechers = torrent.peers,
                            quality = torrent.quality,
                            source = "YTS"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Log error but continue with other sources
        }
        
        return results.sortedByDescending { it.seeders }
    }
}

interface YtsApi {
    @GET("list_movies.json")
    suspend fun searchMovies(
        @Query("query_term") query: String,
        @Query("year") year: Int? = null,
        @Query("limit") limit: Int = 20
    ): YtsResponse
}

data class YtsResponse(
    val status: String,
    val status_message: String,
    val data: YtsData
)

data class YtsData(
    val movies: List<YtsMovie>?
)

data class YtsMovie(
    val id: Int,
    val title: String,
    val year: Int,
    val torrents: List<YtsTorrent>
)

data class YtsTorrent(
    val url: String,
    val hash: String,
    val quality: String,
    val seeds: Int,
    val peers: Int,
    val size_bytes: Long
)
