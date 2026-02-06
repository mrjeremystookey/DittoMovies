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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMovieScreen(navController: NavController, movieId: String?) {
    val viewModel: EditMovieScreenViewModel = viewModel()

    // Load once, not on every recomposition
    LaunchedEffect(movieId) {
        viewModel.loadMovie(id = movieId)
    }

    val state by viewModel.state.collectAsState()
    val topBarTitle = if (movieId == null) "New Movie" else "Edit Movie"

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
                    canDelete = state.canDelete,
                    title = state.title,
                    onTitleChange = { viewModel.updateState { copy(title = it) } },
                    year = state.year,
                    onYearChange = { viewModel.updateState { copy(year = it) } },
                    plot = state.plot,
                    onPlotChange = { viewModel.updateState { copy(plot = it) } },
                    genres = state.genres,
                    onGenresChange = { viewModel.updateState { copy(genres = it) } },
                    rated = state.rated,
                    onRatedChange = { viewModel.updateState { copy(rated = it) } },
                    runtime = state.runtime,
                    onRuntimeChange = { viewModel.updateState { copy(runtime = it) } },
                    poster = state.poster,
                    onPosterChange = { viewModel.updateState { copy(poster = it) } },
                    directors = state.directors,
                    onDirectorsChange = { viewModel.updateState { copy(directors = it) } },
                    cast = state.cast,
                    onCastChange = { viewModel.updateState { copy(cast = it) } },
                    imdbRating = state.imdbRating,
                    onImdbRatingChange = { viewModel.updateState { copy(imdbRating = it) } },
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
