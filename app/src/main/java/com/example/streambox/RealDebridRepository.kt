package com.example.streambox

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RealDebridRepository {
    private val api = Retrofit.Builder()
        .baseUrl("https://api.real-debrid.com/rest/1.0/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RealDebridApi::class.java)

    suspend fun getUserInfo(token: String): Result<User> = try {
        Result.success(api.getUserInfo("Bearer $token"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTorrents(token: String): Result<List<Torrent>> = try {
        Result.success(api.getTorrents("Bearer $token"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTorrentInfo(token: String, id: String): Result<Torrent> = try {
        Result.success(api.getTorrentInfo("Bearer $token", id))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addMagnet(token: String, magnet: String): Result<Torrent> = try {
        Result.success(api.addMagnet("Bearer $token", magnet))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun selectFiles(token: String, id: String, files: String): Result<Unit> = try {
        api.selectFiles("Bearer $token", id, files)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteTorrent(token: String, id: String): Result<Unit> = try {
        api.deleteTorrent("Bearer $token", id)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun unrestrictLink(token: String, link: String): Result<UnrestrictLink> = try {
        Result.success(api.unrestrictLink("Bearer $token", link))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getDownloads(token: String): Result<List<Download>> = try {
        Result.success(api.getDownloads("Bearer $token"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteDownload(token: String, id: String): Result<Unit> = try {
        api.deleteDownload("Bearer $token", id)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun checkInstantAvailability(token: String, magnets: List<String>): Result<Map<String, Map<String, List<InstantFile>>>> = try {
        val magnetsString = magnets.joinToString(",")
        Result.success(api.instantAvailability("Bearer $token", magnetsString))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
