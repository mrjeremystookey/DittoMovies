package support.ditto.dittoMovies.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import support.ditto.dittoMovies.data.Movie
import support.ditto.dittoMovies.data.MoviesRepository
import timber.log.Timber

class MovieDetailScreenViewModel : ViewModel() {

    private val repository = MoviesRepository.instance
    private var _loaded = false

    private val _movie = MutableStateFlow<Movie?>(null)
    val movie: StateFlow<Movie?> = _movie.asStateFlow()

    fun loadMovie(movieId: String) {
        if (_loaded) return
        _loaded = true

        Timber.d("🎬 Loading movie detail: $movieId")
        viewModelScope.launch {
            val result = repository.getMovieById(movieId)
            if (result != null) {
                Timber.d("✅ Movie detail loaded: '${result.title}'")
            } else {
                Timber.w("⚠️ Movie not found for detail: $movieId")
            }
            _movie.value = result
        }
    }

    fun toggleWatched() {
        val current = _movie.value ?: return
        val newWatched = !current.watched
        Timber.d("👁️ Toggling watched=${newWatched} for '${current.title}'")
        _movie.value = current.copy(watched = newWatched)
        viewModelScope.launch {
            repository.toggleWatched(current._id, newWatched)
        }
    }

    fun delete() {
        val current = _movie.value ?: return
        Timber.d("🗑️ Deleting movie: '${current.title}'")
        viewModelScope.launch {
            repository.deleteMovie(current._id)
        }
    }
}
