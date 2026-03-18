package com.example.streambox.enhanced

import retrofit2.http.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.streambox.*

interface EnhancedRealDebridApi {
    // User and Account
    @GET("user")
    suspend fun getUserInfo(@Header("Authorization") token: String): User

    @GET("user/subscription")
    suspend fun getUserSubscription(@Header("Authorization") token: String): UserSubscription

    // Torrent Management
    @GET("torrents")
    suspend fun getTorrents(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
        @Query("filter") filter: String? = null // "active", "completed", "magnet", "upload"
    ): TorrentListResponse

    @GET("torrents/info/{id}")
    suspend fun getTorrentInfo(@Header("Authorization") token: String, @Path("id") id: String): Torrent

    @FormUrlEncoded
    @POST("torrents/add/magnet")
    suspend fun addMagnet(@Header("Authorization") token: String, @Field("magnet") magnet: String): Torrent

    @FormUrlEncoded
    @POST("torrents/add/file")
    suspend fun addFile(@Header("Authorization") token: String, @Field("file") file: String): Torrent

    @FormUrlEncoded
    @POST("torrents/selectFiles/{id}")
    suspend fun selectFiles(@Header("Authorization") token: String, @Path("id") id: String, @Field("files") files: String): Torrent

    @DELETE("torrents/delete/{id}")
    suspend fun deleteTorrent(@Header("Authorization") token: String, @Path("id") id: String): Void

    // Downloads
    @GET("downloads")
    suspend fun getDownloads(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): DownloadListResponse

    @DELETE("downloads/delete/{id}")
    suspend fun deleteDownload(@Header("Authorization") token: String, @Path("id") id: String): Void

    @FormUrlEncoded
    @POST("unrestrict/link")
    suspend fun unrestrictLink(@Header("Authorization") token: String, @Field("link") link: String): UnrestrictLink

    @FormUrlEncoded
    @POST("unrestrict/folder")
    suspend fun unrestrictFolder(@Header("Authorization") token: String, @Field("link") link: String): UnrestrictFolder

    // Instant Availability
    @FormUrlEncoded
    @POST("torrents/instant")
    suspend fun checkInstantAvailability(
        @Header("Authorization") token: String,
        @Field("magnets") magnets: String,
        @Field("hash") hash: String? = null
    ): Map<String, List<InstantFile>>

    // Traffic and Stats
    @GET("user/traffic")
    suspend fun getUserTraffic(@Header("Authorization") token: String): UserTraffic

    @GET("user/stats")
    suspend fun getUserStats(@Header("Authorization") token: String): UserStats

    // Streaming
    @GET("streaming")
    suspend fun getStreamingTranscodes(@Header("Authorization") token: String): List<StreamingTranscode>

    @FormUrlEncoded
    @POST("streaming/transcode")
    suspend fun createTranscode(
        @Header("Authorization") token: String,
        @Field("id") id: String,
        @Field("quality") quality: String? = null
    ): StreamingTranscode
}

// Additional Data Classes
data class TorrentListResponse(
    val torrents: List<Torrent>
)

data class DownloadListResponse(
    val downloads: List<Download>
)

data class UserSubscription(
    val id: Int,
    val username: String,
    val email: String,
    val points: Int,
    val locale: String,
    val avatar: String,
    val type: String,
    val premium: Int,
    val expiration: String?,
    val subscription: SubscriptionInfo
)

data class SubscriptionInfo(
    val offer: String,
    val renewal: String,
    val next_billing: String,
    val status: String
)

data class UserTraffic(
    val hosted: Long,
    val hostedSize: Long,
    val uploaded: Long,
    val uploadedSize: Long,
    val downloaded: Long,
    val downloadedSize: Long,
    val uploadedSizeTotal: Long,
    val downloadedSizeTotal: Long,
    val uploadedSizeLimit: Long,
    val downloadedSizeLimit: Long,
    val resetDate: String
)

data class UserStats(
    val user_id: Int,
    val username: String,
    val links_added: Int,
    val links_clicked: Int,
    val torrents_added: Int,
    val torrents_downloaded: Int,
    val points_earned: Int,
    val points_spent: Int,
    val premium_until: String?
)

data class UnrestrictFolder(
    val id: String,
    val filename: String,
    val filesize: Long,
    val link: String,
    val host: String,
    val files: List<UnrestrictFile>
)

data class UnrestrictFile(
    val id: String,
    val filename: String,
    val filesize: Long,
    val link: String,
    val host: String
)

data class StreamingTranscode(
    val id: String,
    val quality: String,
    val status: String,
    val link: String,
    val expires: String
)

class EnhancedRealDebridRepository {
    private val api = Retrofit.Builder()
        .baseUrl("https://api.real-debrid.com/rest/1.0/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(EnhancedRealDebridApi::class.java)

    suspend fun getUserInfo(token: String): Result<User> = try {
        Result.success(api.getUserInfo("Bearer $token"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUserSubscription(token: String): Result<UserSubscription> = try {
        Result.success(api.getUserSubscription("Bearer $token"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTorrents(
        token: String, 
        page: Int = 1, 
        limit: Int = 100, 
        filter: String? = null
    ): Result<List<Torrent>> = try {
        val response = api.getTorrents("Bearer $token", page, limit, filter)
        Result.success(response.torrents)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getDownloads(
        token: String, 
        page: Int = 1, 
        limit: Int = 100
    ): Result<List<Download>> = try {
        val response = api.getDownloads("Bearer $token", page, limit)
        Result.success(response.downloads)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUserTraffic(token: String): Result<UserTraffic> = try {
        Result.success(api.getUserTraffic("Bearer $token"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUserStats(token: String): Result<UserStats> = try {
        Result.success(api.getUserStats("Bearer $token"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun createTranscode(token: String, id: String, quality: String? = null): Result<StreamingTranscode> = try {
        Result.success(api.createTranscode("Bearer $token", id, quality))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun unrestrictFolder(token: String, link: String): Result<UnrestrictFolder> = try {
        Result.success(api.unrestrictFolder("Bearer $token", link))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
