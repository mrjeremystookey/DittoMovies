package support.ditto.dittoMovies.edit

import timber.log.Timber
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import support.ditto.dittoMovies.data.MoviesRepository

class EditMovieScreenViewModel : ViewModel() {

    private val repository
    private var _id: String? = null

    var title = MutableLiveData("")
    var year = MutableLiveData("")
    var plot = MutableLiveData("")
    var genres = MutableLiveData("")
    var rated = MutableLiveData("")
    var runtime = MutableLiveData("")
    var poster = MutableLiveData("")
    var directors = MutableLiveData("")
    var cast = MutableLiveData("")
    var imdbRating = MutableLiveData("")
    var canDelete = MutableLiveData(false)

    fun setupWithMovie(id: String?) {
        canDelete.postValue(id != null)
        val movieId: String = id ?: run {
            Timber.d("🆕 Setting up for new movie")
            return
        }

        Timber.d("📝 Setting up edit for movie: $movieId")
        viewModelScope.launch {
            val movie = repository.getMovieById(movieId) ?: run {
                Timber.w("⚠️ Movie not found: $movieId")
                return@launch
            }
            Timber.d("✅ Loaded movie: '${movie.title}' (${movie.year})")
            _id = movie._id
            title.postValue(movie.title)
            year.postValue(if (movie.year > 0) movie.year.toString() else "")
            plot.postValue(movie.plot)
            genres.postValue(movie.genres.joinToString(", "))
            rated.postValue(movie.rated)
            runtime.postValue(if (movie.runtime > 0) movie.runtime.toString() else "")
            poster.postValue(movie.poster)
            directors.postValue(movie.directors.joinToString(", "))
            cast.postValue(movie.cast.joinToString(", "))
            imdbRating.postValue(if (movie.imdbRating > 0) movie.imdbRating.toString() else "")
        }
    }

    private fun buildMovieMap(): Map<String, Any?> = mapOf(
        "title" to (title.value ?: ""),
        "year" to (year.value?.toIntOrNull() ?: 0),
        "plot" to (plot.value ?: ""),
        "genres" to (genres.value ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() },
        "rated" to (rated.value ?: ""),
        "runtime" to (runtime.value?.toIntOrNull() ?: 0),
        "poster" to (poster.value ?: ""),
        "directors" to (directors.value ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() },
        "cast" to (cast.value ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() },
        "imdbRating" to (imdbRating.value?.toDoubleOrNull() ?: 0.0),
        "deleted" to false
    )

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
