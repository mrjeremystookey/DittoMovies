package support.ditto.dittoMovies.data

import android.content.Context
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.ditto.DittoError
import live.ditto.DittoStoreObserver
import live.ditto.DittoSyncSubscription
import org.json.JSONArray
import org.json.JSONObject
import support.ditto.dittoMovies.DittoHandler.Companion.ditto
import support.ditto.dittoMovies.MoviesApplication

class MoviesRepository {

    companion object {
        private const val COLLECTION = "movies"
        const val QUERY = "SELECT * FROM $COLLECTION WHERE NOT deleted ORDER BY title ASC"

        // Singleton instance
        val instance: MoviesRepository by lazy { MoviesRepository() }
    }

    private val appContext: Context = MoviesApplication.applicationContext()
    private var syncSubscription: DittoSyncSubscription? = null

    // ── Observe ──

    fun observeMovies(onChange: (List<Movie>) -> Unit): DittoStoreObserver {
        Timber.d("👀 Registering movies observer...")
        return ditto.store.registerObserver(QUERY) { result ->
            val list = result.items.map { item -> Movie.fromJson(item.jsonString()) }
            Timber.d("📋 Observer received ${list.size} movies")
            onChange(list)
        }
    }

    // ── Read ──

    suspend fun getMovieById(movieId: String): Movie? {
        Timber.d("🔍 Fetching movie by id: $movieId")
        return try {
            val item = ditto.store.execute(
                "SELECT * FROM $COLLECTION WHERE _id = :_id AND NOT deleted",
                mapOf("_id" to movieId)
            ).items.firstOrNull()

            if (item == null) {
                Timber.w("⚠️ Movie not found: $movieId")
                return null
            }

            val movie = Movie.fromJson(item.jsonString())
            Timber.d("✅ Found movie: '${movie.title}' (${movie.year})")
            movie
        } catch (e: DittoError) {
            Timber.e("❌ Unable to fetch movie by id: $movieId", e)
            null
        }
    }

    // ── Insert ──

    suspend fun insertMovie(movieMap: Map<String, Any?>) {
        Timber.d("➕ Inserting movie: '${movieMap["title"]}'")
        try {
            ditto.store.execute(
                "INSERT INTO $COLLECTION DOCUMENTS (:doc)",
                mapOf("doc" to movieMap)
            )
            Timber.d("✅ Inserted movie: '${movieMap["title"]}'")
        } catch (e: DittoError) {
            Timber.e("❌ Unable to insert movie: '${movieMap["title"]}'", e)
        }
    }

    // ── Update ──

    suspend fun updateMovie(movieId: String, movieMap: Map<String, Any?>) {
        Timber.d("✏️ Updating movie: $movieId -> '${movieMap["title"]}'")
        try {
            ditto.store.execute(
                """
                UPDATE $COLLECTION
                SET
                  title = :title,
                  year = :year,
                  plot = :plot,
                  genres = :genres,
                  rated = :rated,
                  runtime = :runtime,
                  poster = :poster,
                  directors = :directors,
                  cast = :cast,
                  imdbRating = :imdbRating
                WHERE _id = :id
                AND NOT deleted
                """,
                movieMap + ("id" to movieId)
            )
            Timber.d("✅ Updated movie: $movieId")
        } catch (e: DittoError) {
            Timber.e("❌ Unable to update movie: $movieId", e)
        }
    }

    // ── Delete (soft) ──

    suspend fun deleteMovie(movieId: String) {
        Timber.d("🗑️ Soft-deleting movie: $movieId")
        try {
            ditto.store.execute(
                "UPDATE $COLLECTION SET deleted = true WHERE _id = :id",
                mapOf("id" to movieId)
            )
            Timber.d("✅ Soft-deleted movie: $movieId")
        } catch (e: DittoError) {
            Timber.e("❌ Unable to soft-delete movie: $movieId", e)
        }
    }

    // ── Sync ──

    fun startSync() {
        Timber.d("🔄 Starting sync...")
        try {
            ditto.startSync()
            syncSubscription = ditto.sync.registerSubscription(QUERY)
            Timber.d("✅ Sync started and subscription registered")
        } catch (e: DittoError) {
            Timber.e("❌ Unable to start sync", e)
        }
    }

    fun stopSync() {
        Timber.d("⏹️ Stopping sync...")
        try {
            syncSubscription?.close()
            syncSubscription = null
            ditto.stopSync()
            Timber.d("✅ Sync stopped")
        } catch (e: DittoError) {
            Timber.e("❌ Unable to stop sync", e)
        }
    }

    val isSyncActive: Boolean
        get() = ditto.isSyncActive

    // ── Import from assets ──

    suspend fun importMoviesFromAssets() {
        withContext(Dispatchers.IO) {
            try {
                Timber.d("📂 Reading all_movies.json from assets...")
                val jsonString = appContext.assets.open("all_movies.json")
                    .bufferedReader()
                    .use { it.readText() }

                val jsonArray = JSONArray(jsonString)
                val total = jsonArray.length()
                Timber.d("📦 Parsed $total movies from JSON, starting import...")

                var imported = 0
                var failed = 0
                for (i in 0 until total) {
                    try {
                        val obj = jsonArray.getJSONObject(i)
                        val movieMap = flattenMovieJson(obj)

                        ditto.store.execute(
                            "INSERT INTO $COLLECTION INITIAL DOCUMENTS (:movie)",
                            mapOf("movie" to movieMap)
                        )
                        imported++

                        // Log progress every 1000 movies
                        if (imported % 1000 == 0) {
                            Timber.d("⏳ Import progress: $imported / $total")
                        }
                    } catch (e: Exception) {
                        failed++
                        Timber.e("❌ Error importing movie at index $i", e)
                    }
                }

                Timber.d("🎬 Import complete! ✅ $imported imported, ❌ $failed failed, 📊 $total total")
            } catch (e: Exception) {
                Timber.e("💥 Error reading movies JSON from assets", e)
            }
        }
    }

    private fun flattenMovieJson(obj: JSONObject): Map<String, Any?> {
        val id = obj.optJSONObject("_id")?.optString("\$oid")
            ?: obj.optString("_id", java.util.UUID.randomUUID().toString())

        fun jsonArrayToList(key: String): List<String> {
            val arr = obj.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map { arr.getString(it) }
        }

        val imdbRating = obj.optJSONObject("imdb")?.optDouble("rating", 0.0) ?: 0.0

        return mapOf(
            "_id" to id,
            "title" to obj.optString("title", ""),
            "year" to obj.optInt("year", 0),
            "plot" to obj.optString("plot", ""),
            "genres" to jsonArrayToList("genres"),
            "rated" to obj.optString("rated", ""),
            "runtime" to obj.optInt("runtime", 0),
            "poster" to obj.optString("poster", ""),
            "directors" to jsonArrayToList("directors"),
            "cast" to jsonArrayToList("cast"),
            "imdbRating" to imdbRating,
            "deleted" to false
        )
    }
}
