package com.example.streambox

import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Deferred

// 1337x API
interface LeetxApi {
    @GET("search/{query}/1/")
    suspend fun searchMovies(@Query("query") query: String): LeetxResponse
}

data class LeetxResponse(
    val torrents: List<LeetxTorrent>
)

data class LeetxTorrent(
    val name: String,
    val magnet: String,
    val size: String,
    val seeds: Int,
    val leechers: Int,
    val category: String
)

// EZTV API
interface EztvApi {
    @GET("search/{query}")
    suspend fun searchShows(@Query("q") query: String): EztvResponse
}

data class EztvResponse(
    val torrents: List<EztvTorrent>
)

data class EztvTorrent(
    val title: String,
    val magnet_url: String,
    val size_bytes: Long,
    val seeds: Int,
    val leechers: Int
)

// Enhanced Torrent Search Service
class MultiSourceTorrentSearchService {
    private val ytsApi = Retrofit.Builder()
        .baseUrl("https://yts.mx/api/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(YtsApi::class.java)

    private val leetxApi = Retrofit.Builder()
        .baseUrl("https://1337x.to/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LeetxApi::class.java)

    private val eztvApi = Retrofit.Builder()
        .baseUrl("https://eztv.re/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(EztvApi::class.java)

    data class SearchFilters(
        val minQuality: String? = null, // "720p", "1080p", "4K"
        val maxSizeGB: Int? = null,
        val minSeeders: Int = 0,
        val sources: List<String> = listOf("YTS", "1337x", "EZTV")
    )

    suspend fun searchMovieTorrents(
        movieTitle: String, 
        year: Int? = null,
        filters: SearchFilters = SearchFilters()
    ): List<TorrentResult> = coroutineScope {
        val results = mutableListOf<TorrentResult>()
        val deferredJobs = mutableListOf<Deferred<List<TorrentResult>>>()

        // Search YTS
        if (filters.sources.contains("YTS")) {
            deferredJobs.add(async {
                try {
                    val ytsResponse = ytsApi.searchMovies(movieTitle, year)
                    ytsResponse.data.movies?.mapNotNull { movie ->
                        movie.torrents.mapNotNull { torrent ->
                            if (passesFilter(torrent.quality, torrent.size_bytes, torrent.seeds, filters)) {
                                TorrentResult(
                                    id = "yts_${movie.id}_${torrent.quality}",
                                    name = "${movie.title} (${torrent.quality})",
                                    magnet = torrent.url,
                                    size = torrent.size_bytes,
                                    seeders = torrent.seeds,
                                    leechers = torrent.peers,
                                    quality = torrent.quality,
                                    source = "YTS"
                                )
                            } else null
                        }
                    }?.flatten() ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            })
        }

        // Search 1337x
        if (filters.sources.contains("1337x")) {
            deferredJobs.add(async {
                try {
                    val query = if (year != null) "$movieTitle $year" else movieTitle
                    val response = leetxApi.searchMovies(query)
                    response.torrents.mapNotNull { torrent ->
                        if (torrent.category == "Movies" && 
                            passesFilter(extractQuality(torrent.name), parseSize(torrent.size), torrent.seeds, filters)) {
                            TorrentResult(
                                id = "leetx_${torrent.name.hashCode()}",
                                name = torrent.name,
                                magnet = torrent.magnet,
                                size = parseSize(torrent.size),
                                seeders = torrent.seeds,
                                leechers = torrent.leechers,
                                quality = extractQuality(torrent.name),
                                source = "1337x"
                            )
                        } else null
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            })
        }

        // Search EZTV
        if (filters.sources.contains("EZTV")) {
            deferredJobs.add(async {
                try {
                    val response = eztvApi.searchShows(movieTitle)
                    response.torrents.mapNotNull { torrent ->
                        if (passesFilter(extractQuality(torrent.title), torrent.size_bytes, torrent.seeds, filters)) {
                            TorrentResult(
                                id = "eztv_${torrent.title.hashCode()}",
                                name = torrent.title,
                                magnet = torrent.magnet_url,
                                size = torrent.size_bytes,
                                seeders = torrent.seeds,
                                leechers = torrent.leechers,
                                quality = extractQuality(torrent.title),
                                source = "EZTV"
                            )
                        } else null
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            })
        }

        // Wait for all searches to complete
        val allResults: List<List<TorrentResult>> = deferredJobs.awaitAll()
        allResults.flatten().sortedByDescending { it.seeders }
    }

    private fun passesFilter(
        quality: String?,
        sizeBytes: Long,
        seeders: Int,
        filters: SearchFilters
    ): Boolean {
        // Quality filter
        if (filters.minQuality != null && quality != null) {
            val qualityOrder = mapOf("480p" to 1, "720p" to 2, "1080p" to 3, "4K" to 4, "2160p" to 4)
            val qualityValue = qualityOrder[quality.uppercase()] ?: 0
            val minQualityValue = qualityOrder[filters.minQuality.uppercase()] ?: 0
            if (qualityValue < minQualityValue) return false
        }

        // Size filter
        if (filters.maxSizeGB != null) {
            val sizeGB = sizeBytes / (1024.0 * 1024.0 * 1024.0)
            if (sizeGB > filters.maxSizeGB) return false
        }

        // Seeders filter
        if (seeders < filters.minSeeders) return false

        return true
    }

    private fun extractQuality(filename: String): String? {
        val patterns = listOf("4K", "2160p", "1080p", "720p", "480p")
        return patterns.find { filename.contains(it, ignoreCase = true) }
    }

    private fun parseSize(sizeStr: String): Long {
        val units = mapOf("KB" to 1024, "MB" to 1024 * 1024, "GB" to 1024 * 1024 * 1024)
        val regex = """(\d+\.?\d*)\s*(KB|MB|GB)""".toRegex()
        val match = regex.find(sizeStr.uppercase())
        return match?.let {
            val value = it.groupValues[1].toDouble()
            val unit = it.groupValues[2]
            (value * (units[unit] ?: 1)).toLong()
        } ?: 0
    }
}
