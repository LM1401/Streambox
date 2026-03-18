package com.example.streambox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.tv.material3.ClickableSurfaceDefaults
import coil.compose.AsyncImage

// Simple placeholder components to restore functionality
@Composable
fun StreamingPlayerScreen(source: StreamingSource, viewModel: MainViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Streaming Player: ${source.quality}",
            style = androidx.tv.material3.MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun TorrentSourcesList(
    torrents: List<TorrentResult>,
    onTorrentSelected: (TorrentResult) -> Unit
) {
    LazyColumn {
        items(torrents) { torrent ->
            Button(
                onClick = { onTorrentSelected(torrent) },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text(torrent.name)
            }
        }
    }
}

@Composable
fun StreamingSourcesList(
    sources: List<StreamingSource>,
    onSourceSelected: (StreamingSource) -> Unit
) {
    LazyColumn {
        items(sources) { source ->
            Button(
                onClick = { onSourceSelected(source) },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text("${source.quality} - ${source.size}")
            }
        }
    }
}

@Composable
fun MovieCard(
    movie: TmdbMovie,
    onClick: () -> Unit,
    viewModel: MainViewModel
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(240.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            AsyncImage(
                model = movie.getPosterUrl(),
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Text(
                text = movie.title,
                style = androidx.tv.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp),
                maxLines = 2
            )
        }
    }
}

@Composable
fun RDUserInfo(user: User) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome, ${user.username}!", style = androidx.tv.material3.MaterialTheme.typography.headlineSmall)
        Text("Email: ${user.email}")
        Text("Premium: ${if (user.premium > 0) "Active" else "Inactive"}")
    }
}

@Composable
fun NetflixMovieRow(
    title: String,
    movies: List<TmdbMovie>,
    onMovieClick: (TmdbMovie) -> Unit,
    viewModel: MainViewModel
) {
    Column {
        Text(
            text = title,
            style = androidx.tv.material3.MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        LazyRow {
            items(movies) { movie ->
                NetflixMovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}

@Composable
fun NetflixMovieCard(
    movie: TmdbMovie,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(180.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        AsyncImage(
            model = movie.getPosterUrl(),
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun TorrentCard(
    torrent: Torrent,
    onTorrentClicked: (Torrent) -> Unit,
    onDeleteTorrent: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(torrent.filename, style = androidx.tv.material3.MaterialTheme.typography.titleMedium)
                Text("${torrent.status} - ${torrent.progress}%", style = androidx.tv.material3.MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { onTorrentClicked(torrent) }) {
                Text("Open")
            }
        }
    }
}

@Composable
fun TorrentResultCard(
    torrent: TorrentResult,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(torrent.name, style = androidx.tv.material3.MaterialTheme.typography.titleMedium)
                Text("${torrent.quality} - ${torrent.seeders} seeders", style = androidx.tv.material3.MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onClick) {
                Text("Select")
            }
        }
    }
}

@Composable
fun RatingButtons(
    currentRating: RatingType,
    onRatingChanged: (RatingType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        RatingButton("👎", currentRating == RatingType.DISLIKE, Color.Red, { onRatingChanged(RatingType.DISLIKE) })
        RatingButton("👍", currentRating == RatingType.LIKE, Color.Green, { onRatingChanged(RatingType.LIKE) })
        RatingButton("❤️", currentRating == RatingType.SUPER_LIKE, Color.Red, { onRatingChanged(RatingType.SUPER_LIKE) })
    }
}

@Composable
fun RatingButton(
    icon: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = icon,
            style = androidx.tv.material3.MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(8.dp)
        )
    }
}
