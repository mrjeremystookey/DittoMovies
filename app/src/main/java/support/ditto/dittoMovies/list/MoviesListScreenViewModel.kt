package support.ditto.dittoMovies.list

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import support.ditto.dittoMovies.MoviesApplication
import support.ditto.dittoMovies.data.Movie
import support.ditto.dittoMovies.data.MoviesRepository
import timber.log.Timber

private val Context.preferencesDataStore by preferencesDataStore("movies_list_settings")
private val SYNC_ENABLED_KEY = booleanPreferencesKey("sync_enabled")
private val DATA_IMPORTED_KEY = booleanPreferencesKey("data_imported")

class MoviesListScreenViewModel : ViewModel() {

    private val appContext = MoviesApplication.applicationContext()
    private val preferencesDataStore = appContext.preferencesDataStore
    private val repository = MoviesRepository.instance

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private val _syncEnabled = MutableStateFlow(true)
    val syncEnabled: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    private val _showWatched = MutableStateFlow(false)
    val showWatched: StateFlow<Boolean> = _showWatched.asStateFlow()

    private val _showDeleted = MutableStateFlow(false)
    val showDeleted: StateFlow<Boolean> = _showDeleted.asStateFlow()

    private var observerJob: Job? = null

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            Timber.d("🔄 setSyncEnabled($enabled)")
            preferencesDataStore.edit { settings ->
                settings[SYNC_ENABLED_KEY] = enabled
            }
            _syncEnabled.value = enabled

            if (enabled && !repository.isSyncActive) {
                Timber.d("▶️ Enabling sync...")
                repository.startSync()
            } else if (repository.isSyncActive) {
                Timber.d("⏸️ Disabling sync...")
                repository.stopSync()
            }
        }
    }

    init {
        Timber.d("🎬 ViewModel initializing...")
        viewModelScope.launch {
            // Import movies from JSON asset on first launch
            val alreadyImported = preferencesDataStore.data
                .map { prefs -> prefs[DATA_IMPORTED_KEY] ?: false }
                .first()

            if (!alreadyImported) {
                Timber.d("📥 First launch detected — importing movies from assets")
                repository.importMoviesFromAssets()
                preferencesDataStore.edit { settings ->
                    settings[DATA_IMPORTED_KEY] = true
                }
                Timber.d("✅ Import flag saved to preferences")
            } else {
                Timber.d("⏭️ Movies already imported, skipping")
            }

            // Start observing with current filter state
            startObserving()
        }

        // Restore sync preference in parallel
        viewModelScope.launch {
            val savedSyncPref = preferencesDataStore.data.map { prefs -> prefs[SYNC_ENABLED_KEY] ?: true }.first()
            Timber.d("⚙️ Restoring sync preference: $savedSyncPref")
            setSyncEnabled(savedSyncPref)
        }
    }

    private fun startObserving() {
        observerJob?.cancel()
        val watched = _showWatched.value
        val deleted = _showDeleted.value
        Timber.d("👀 Starting observer (showWatched=$watched, showDeleted=$deleted)")
        observerJob = viewModelScope.launch {
            repository.observeMovies(
                showWatched = watched,
                showDeleted = deleted
            ).collect { list ->
                Timber.d("📋 Received ${list.size} movies from observer")
                _movies.value = list
            }
        }
    }

    fun setShowWatched(enabled: Boolean) {
        Timber.d("🎯 Filter: showWatched=$enabled")
        _showWatched.value = enabled
        startObserving()
    }

    fun setShowDeleted(enabled: Boolean) {
        Timber.d("🎯 Filter: showDeleted=$enabled")
        _showDeleted.value = enabled
        startObserving()
    }

    fun toggleWatched(movieId: String, watched: Boolean) {
        Timber.d("👁️ Toggling watched=$watched for movie: $movieId")
        viewModelScope.launch {
            repository.toggleWatched(movieId, watched)
        }
    }

    fun delete(movieId: String) {
        Timber.d("🗑️ User requested delete for movie: $movieId")
        viewModelScope.launch {
            repository.deleteMovie(movieId)
        }
    }
}
