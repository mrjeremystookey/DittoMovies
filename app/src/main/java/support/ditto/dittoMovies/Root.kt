package support.ditto.dittoMovies

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import support.ditto.dittoMovies.edit.EditMovieScreen
import support.ditto.dittoMovies.list.MoviesListScreen
import support.ditto.dittoMovies.ui.theme.DittoMoviesTheme

@Composable
fun Root() {
    val navController = rememberNavController()

    DittoMoviesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NavHost(navController = navController, startDestination = "movies") {
                composable("movies") { MoviesListScreen(navController = navController) }
                composable("movies/edit") {
                    EditMovieScreen(navController = navController, movieId = null)
                }
                composable("movies/edit/{movieId}") { backStackEntry ->
                    val movieId: String? = backStackEntry.arguments?.getString("movieId")
                    EditMovieScreen(navController = navController, movieId = movieId)
                }
            }
        }
    }
}
