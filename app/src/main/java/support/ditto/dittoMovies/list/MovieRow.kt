package support.ditto.dittoMovies.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import support.ditto.dittoMovies.data.Movie
import java.util.UUID

@Composable
fun MovieRow(
    movie: Movie,
    onClick: ((movie: Movie) -> Unit)? = null,
) {
    ListItem(
        modifier = Modifier.clickable { onClick?.invoke(movie) },
        headlineContent = {
            Text(text = movie.title)
        },
        supportingContent = {
            Column {
                if (movie.year > 0) {
                    Text(
                        text = "${movie.year} • ${movie.genres.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (movie.imdbRating > 0) {
                    Text(
                        text = "⭐ ${movie.imdbRating}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        leadingContent = {
            if (movie.poster.isNotBlank()) {
                AsyncImage(
                    model = movie.poster,
                    contentDescription = movie.title,
                    modifier = Modifier
                        .size(48.dp, 72.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        },
        trailingContent = if (movie.watched) {
            {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Watched",
                    tint = Color(0xFF4CAF50)
                )
            }
        } else null
    )
}

@Preview(showBackground = true)
@Composable
fun MovieRowPreview() {
    Column {
        MovieRow(
            movie = Movie(
                _id = UUID.randomUUID().toString(),
                title = "The Great Train Robbery",
                year = 1903,
                genres = listOf("Short", "Western"),
                imdbRating = 7.4
            )
        )
    }
}
