package support.ditto.dittoMovies.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMovieScreen(navController: NavController, movieId: String?) {
    val viewModel: EditMovieScreenViewModel = viewModel()
    viewModel.setupWithMovie(id = movieId)

    val topBarTitle = if (movieId == null) "New Movie" else "Edit Movie"

    val title: String by viewModel.title.observeAsState("")
    val year: String by viewModel.year.observeAsState("")
    val plot: String by viewModel.plot.observeAsState("")
    val genres: String by viewModel.genres.observeAsState("")
    val rated: String by viewModel.rated.observeAsState("")
    val runtime: String by viewModel.runtime.observeAsState("")
    val poster: String by viewModel.poster.observeAsState("")
    val directors: String by viewModel.directors.observeAsState("")
    val cast: String by viewModel.cast.observeAsState("")
    val imdbRating: String by viewModel.imdbRating.observeAsState("")
    val canDelete: Boolean by viewModel.canDelete.observeAsState(false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(topBarTitle, color = MaterialTheme.colorScheme.onPrimary)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                EditMovieForm(
                    canDelete = canDelete,
                    title = title,
                    onTitleChange = { viewModel.title.value = it },
                    year = year,
                    onYearChange = { viewModel.year.value = it },
                    plot = plot,
                    onPlotChange = { viewModel.plot.value = it },
                    genres = genres,
                    onGenresChange = { viewModel.genres.value = it },
                    rated = rated,
                    onRatedChange = { viewModel.rated.value = it },
                    runtime = runtime,
                    onRuntimeChange = { viewModel.runtime.value = it },
                    poster = poster,
                    onPosterChange = { viewModel.poster.value = it },
                    directors = directors,
                    onDirectorsChange = { viewModel.directors.value = it },
                    cast = cast,
                    onCastChange = { viewModel.cast.value = it },
                    imdbRating = imdbRating,
                    onImdbRatingChange = { viewModel.imdbRating.value = it },
                    onSaveClicked = {
                        viewModel.save()
                        navController.popBackStack()
                    },
                    onDeleteClicked = {
                        viewModel.delete()
                        navController.popBackStack()
                    }
                )
            }
        }
    )
}
