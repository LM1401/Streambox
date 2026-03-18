package com.example.streambox

import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface RealDebridApi {
    @GET("user")
    suspend fun getUserInfo(
        @Header("Authorization") token: String
    ): User

    @GET("torrents")
    suspend fun getTorrents(
        @Header("Authorization") token: String
    ): List<Torrent>

    @GET("torrents/info/{id}")
    suspend fun getTorrentInfo(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Torrent

    @FormUrlEncoded
    @POST("torrents/add/magnet")
    suspend fun addMagnet(
        @Header("Authorization") token: String,
        @Field("magnet") magnet: String
    ): Torrent

    @FormUrlEncoded
    @POST("torrents/selectFiles/{id}")
    suspend fun selectFiles(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Field("files") files: String
    ): Unit

    @DELETE("torrents/delete/{id}")
    suspend fun deleteTorrent(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Unit

    @FormUrlEncoded
    @POST("unrestrict/link")
    suspend fun unrestrictLink(
        @Header("Authorization") token: String,
        @Field("link") link: String
    ): UnrestrictLink

    @GET("downloads")
    suspend fun getDownloads(
        @Header("Authorization") token: String
    ): List<Download>

    @DELETE("downloads/delete/{id}")
    suspend fun deleteDownload(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Unit

    @FormUrlEncoded
    @POST("torrents/instant")
    suspend fun instantAvailability(
        @Header("Authorization") token: String,
        @Field("magnets") magnets: String
    ): Map<String, Map<String, List<InstantFile>>>
}
