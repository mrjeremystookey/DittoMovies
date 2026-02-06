package support.ditto.dittoMovies.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import support.ditto.dittoMovies.data.MoviesRepository
import timber.log.Timber

data class EditMovieState(
    val title: String = "",
    val year: String = "",
    val plot: String = "",
    val genres: String = "",
    val rated: String = "",
    val runtime: String = "",
    val poster: String = "",
    val directors: String = "",
    val cast: String = "",
    val imdbRating: String = "",
    val watched: Boolean = false,
    val canDelete: Boolean = false,
)

class EditMovieScreenViewModel : ViewModel() {

    private val repository = MoviesRepository.instance
    private var _id: String? = null
    private var _loaded = false

    private val _state = MutableStateFlow(EditMovieState())
    val state: StateFlow<EditMovieState> = _state.asStateFlow()

    fun loadMovie(id: String?) {
        if (_loaded) return
        _loaded = true

        _state.update { it.copy(canDelete = id != null) }
        val movieId: String = id ?: run {
            Timber.d("🆕 Setting up for new movie")
            return
        }

        Timber.d("📝 Loading movie: $movieId")
        viewModelScope.launch {
            val movie = repository.getMovieById(movieId) ?: run {
                Timber.w("⚠️ Movie not found: $movieId")
                return@launch
            }
            Timber.d("✅ Loaded movie: '${movie.title}' (${movie.year})")
            _id = movie._id
            _state.value = EditMovieState(
                title = movie.title,
                year = if (movie.year > 0) movie.year.toString() else "",
                plot = movie.plot,
                genres = movie.genres.joinToString(", "),
                rated = movie.rated,
                runtime = if (movie.runtime > 0) movie.runtime.toString() else "",
                poster = movie.poster,
                directors = movie.directors.joinToString(", "),
                cast = movie.cast.joinToString(", "),
                imdbRating = if (movie.imdbRating > 0) movie.imdbRating.toString() else "",
                watched = movie.watched,
                canDelete = true,
            )
        }
    }

    fun updateState(transform: EditMovieState.() -> EditMovieState) {
        _state.update { it.transform() }
    }

    private fun buildMovieMap(): Map<String, Any?> {
        val s = _state.value
        return mapOf(
            "title" to s.title,
            "year" to (s.year.toIntOrNull() ?: 0),
            "plot" to s.plot,
            "genres" to s.genres.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            "rated" to s.rated,
            "runtime" to (s.runtime.toIntOrNull() ?: 0),
            "poster" to s.poster,
            "directors" to s.directors.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            "cast" to s.cast.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            "imdbRating" to (s.imdbRating.toDoubleOrNull() ?: 0.0),
            "watched" to s.watched,
            "deleted" to false
        )
    }

    fun save() {
        viewModelScope.launch {
            val movieMap = buildMovieMap()
            if (_id == null) {
                Timber.d("💾 Saving new movie: '${movieMap["title"]}'")
                repository.insertMovie(movieMap)
            } else {
                Timber.d("💾 Updating existing movie: $_id -> '${movieMap["title"]}'")
                _id?.let { id -> repository.updateMovie(id, movieMap) }
            }
        }
    }

    fun delete() {
        Timber.d("🗑️ Deleting movie: $_id")
        viewModelScope.launch {
            _id?.let { id -> repository.deleteMovie(id) }
        }
    }
}
