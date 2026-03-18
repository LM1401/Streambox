package com.example.streambox

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val points: Int,
    val locale: String,
    val avatar: String,
    val type: String,
    val premium: Int,
    val expiration: String?
)

data class Torrent(
    val id: String,
    val filename: String,
    val hash: String,
    val bytes: Long,
    val host: String,
    val split: Int,
    val progress: Int,
    val status: String,
    val added: String,
    val links: List<String>,
    val ended: String?,
    val files: List<TorrentFile>?
)

data class TorrentFile(
    val id: Int,
    val path: String,
    val bytes: Long,
    val selected: Int
)

data class UnrestrictLink(
    val id: String,
    val filename: String,
    val mimeType: String,
    val filesize: Long,
    val link: String,
    val host: String,
    val chunks: Int,
    val crc: Int,
    val download: String,
    val streamable: Int
)

data class Download(
    val id: String,
    val filename: String,
    val mimeType: String,
    val filesize: Long,
    val link: String,
    val host: String,
    val generated: String,
    val expire: String
)

data class InstantFile(
    val filename: String,
    val filesize: Long,
    val filehost: String,
    val links: List<String>
)

data class StreamingSource(
    val quality: String,
    val size: String,
    val link: String,
    val filename: String
)
