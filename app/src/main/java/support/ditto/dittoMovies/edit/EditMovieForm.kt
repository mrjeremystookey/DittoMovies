package support.ditto.dittoMovies.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun EditMovieForm(
    canDelete: Boolean,
    title: String,
    onTitleChange: (String) -> Unit = {},
    year: String,
    onYearChange: (String) -> Unit = {},
    plot: String,
    onPlotChange: (String) -> Unit = {},
    genres: String,
    onGenresChange: (String) -> Unit = {},
    rated: String,
    onRatedChange: (String) -> Unit = {},
    runtime: String,
    onRuntimeChange: (String) -> Unit = {},
    poster: String,
    onPosterChange: (String) -> Unit = {},
    directors: String,
    onDirectorsChange: (String) -> Unit = {},
    cast: String,
    onCastChange: (String) -> Unit = {},
    imdbRating: String,
    onImdbRatingChange: (String) -> Unit = {},
    onSaveClicked: () -> Unit = {},
    onDeleteClicked: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Title:")
        TextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Year:")
        TextField(
            value = year,
            onValueChange = onYearChange,
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Plot:")
        TextField(
            value = plot,
            onValueChange = onPlotChange,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4
        )

        Text(text = "Genres (comma-separated):")
        TextField(
            value = genres,
            onValueChange = onGenresChange,
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Rated:")
        TextField(
            value = rated,
            onValueChange = onRatedChange,
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Runtime (minutes):")
        TextField(
            value = runtime,
            onValueChange = onRuntimeChange,
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Poster URL:")
        TextField(
            value = poster,
            onValueChange = onPosterChange,
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Directors (comma-separated):")
        TextField(
            value = directors,
            onValueChange = onDirectorsChange,
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Cast (comma-separated):")
        TextField(
            value = cast,
            onValueChange = onCastChange,
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "IMDB Rating:")
        TextField(
            value = imdbRating,
            onValueChange = onImdbRatingChange,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onSaveClicked,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Save", modifier = Modifier.padding(8.dp))
        }

        if (canDelete) {
            Button(
                onClick = onDeleteClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Delete", modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditMovieFormPreview() {
    EditMovieForm(
        canDelete = true,
        title = "The Great Train Robbery",
        year = "1903",
        plot = "A group of bandits stage a brazen train hold-up.",
        genres = "Short, Western",
        rated = "TV-G",
        runtime = "11",
        poster = "",
        directors = "Edwin S. Porter",
        cast = "A.C. Abadie, George Barnes",
        imdbRating = "7.4"
    )
}
