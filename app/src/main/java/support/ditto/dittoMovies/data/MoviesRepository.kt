package support.ditto.dittoMovies.data

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import live.ditto.DittoError
import live.ditto.DittoSyncSubscription
import support.ditto.dittoMovies.DittoHandler.Companion.ditto
import support.ditto.dittoMovies.MoviesApplication
import timber.log.Timber
import java.io.InputStreamReader

class MoviesRepository {

    companion object {
        private const val COLLECTION = "movies"
        private const val SYNC_QUERY = "SELECT * FROM $COLLECTION"
        private const val IMPORT_BATCH_SIZE = 50

        // Singleton instance
        val instance: MoviesRepository by lazy { MoviesRepository() }

        fun buildQuery(showWatched: Boolean = false, showDeleted: Boolean = false): String {
            val clauses = mutableListOf<String>()
            if (showWatched) clauses.add("watched = true")
            if (showDeleted) clauses.add("deleted = true") else clauses.add("NOT deleted")
            val where = if (clauses.isNotEmpty()) "WHERE ${clauses.joinToString(" AND ")}" else ""
            return "SELECT * FROM $COLLECTION $where ORDER BY title ASC"
        }
    }

    private val appContext: Context = MoviesApplication.applicationContext()
    private var syncSubscription: DittoSyncSubscription? = null

    // ── Observe (Flow-based, parsing on IO) ──

    fun observeMovies(
        showWatched: Boolean = false,
        showDeleted: Boolean = false
    ): Flow<List<Movie>> = callbackFlow {
        val query = buildQuery(showWatched, showDeleted)
        Timber.d("👀 Registering movies observer with query: $query")
        val observer = ditto.store.registerObserver(query) { result ->
            val list = result.items.map { item -> Movie.fromJson(item.jsonString()) }
            Timber.d("📋 Observer received ${list.size} movies")
            trySend(list)
        }
        awaitClose {
            Timber.d("👀 Closing movies observer")
            observer.close()
        }
    }.flowOn(Dispatchers.IO)

    // ── Read ──

    suspend fun getMovieById(movieId: String): Movie? {
        Timber.d("🔍 Fetching movie by id: $movieId")
        return try {
            val item = ditto.store.execute(
                "SELECT * FROM $COLLECTION WHERE _id = :_id",
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
                  imdbRating = :imdbRating,
                  watched = :watched
                WHERE _id = :id
                """,
                movieMap + ("id" to movieId)
            )
            Timber.d("✅ Updated movie: $movieId")
        } catch (e: DittoError) {
            Timber.e("❌ Unable to update movie: $movieId", e)
        }
    }

    // ── Toggle watched ──

    suspend fun toggleWatched(movieId: String, watched: Boolean) {
        Timber.d("👁️ Setting watched=$watched for movie: $movieId")
        try {
            ditto.store.execute(
                "UPDATE $COLLECTION SET watched = :watched WHERE _id = :id",
                mapOf("id" to movieId, "watched" to watched)
            )
            Timber.d("✅ Updated watched=$watched for movie: $movieId")
        } catch (e: DittoError) {
            Timber.e("❌ Unable to toggle watched for movie: $movieId", e)
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
            syncSubscription = ditto.sync.registerSubscription(SYNC_QUERY)
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
                Timber.d("📂 Stream-reading all_movies.json from assets...")
                var imported = 0
                var failed = 0
                val batch = mutableListOf<Map<String, Any?>>()

                appContext.assets.open("all_movies.json").use { inputStream ->
                    JsonReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                        reader.beginArray()
                        while (reader.hasNext()) {
                            try {
                                val movieMap = readMovieObject(reader)
                                batch.add(movieMap)
                            } catch (e: Exception) {
                                failed++
                                Timber.e(e, "❌ Error parsing movie at index ${imported + failed}")
                                reader.skipValue()
                            }

                            if (batch.size >= IMPORT_BATCH_SIZE) {
                                imported += insertBatch(batch)
                                batch.clear()
                                if (imported % 1000 < IMPORT_BATCH_SIZE) {
                                    Timber.d("⏳ Import progress: $imported")
                                }
                            }
                        }
                        reader.endArray()
                    }
                }

                // Insert remaining
                if (batch.isNotEmpty()) {
                    imported += insertBatch(batch)
                    batch.clear()
                }

                Timber.d("🎬 Import complete! ✅ $imported imported, ❌ $failed failed")
            } catch (e: Exception) {
                Timber.e(e, "💥 Error reading movies JSON from assets")
            }
        }
    }

    private suspend fun insertBatch(batch: List<Map<String, Any?>>): Int {
        var count = 0
        for (doc in batch) {
            try {
                ditto.store.execute(
                    "INSERT INTO $COLLECTION INITIAL DOCUMENTS (:doc)",
                    mapOf("doc" to doc)
                )
                count++
            } catch (e: Exception) {
                Timber.e(e, "❌ Error inserting movie: ${doc["title"]}")
            }
        }
        return count
    }

    private fun readMovieObject(reader: JsonReader): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "_id" to java.util.UUID.randomUUID().toString(),
            "title" to "",
            "year" to 0,
            "plot" to "",
            "genres" to emptyList<String>(),
            "rated" to "",
            "runtime" to 0,
            "poster" to "",
            "directors" to emptyList<String>(),
            "cast" to emptyList<String>(),
            "imdbRating" to 0.0,
            "watched" to false,
            "deleted" to false
        )

        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (reader.peek() == JsonToken.NULL) {
                reader.skipValue()
                continue
            }
            when (name) {
                "_id" -> {
                    // _id can be a string or an object like { "$oid": "..." }
                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val key = reader.nextName()
                            if (key == "\$oid") map["_id"] = reader.nextString()
                            else reader.skipValue()
                        }
                        reader.endObject()
                    } else {
                        map["_id"] = reader.nextString()
                    }
                }

                "title" -> map["title"] = reader.nextString()
                "year" -> map["year"] = reader.nextInt()
                "plot" -> map["plot"] = reader.nextString()
                "rated" -> map["rated"] = reader.nextString()
                "runtime" -> map["runtime"] = reader.nextInt()
                "poster" -> map["poster"] = reader.nextString()
                "genres", "directors", "cast" -> map[name] = readStringArray(reader)
                "imdb" -> {
                    // Extract rating from nested imdb object
                    reader.beginObject()
                    while (reader.hasNext()) {
                        val key = reader.nextName()
                        if (key == "rating" && reader.peek() == JsonToken.NUMBER) {
                            map["imdbRating"] = reader.nextDouble()
                        } else {
                            reader.skipValue()
                        }
                    }
                    reader.endObject()
                }

                "imdbRating" -> {
                    if (reader.peek() == JsonToken.NUMBER) map["imdbRating"] = reader.nextDouble()
                    else reader.skipValue()
                }

                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return map
    }

    private fun readStringArray(reader: JsonReader): List<String> {
        val list = mutableListOf<String>()
        reader.beginArray()
        while (reader.hasNext()) {
            if (reader.peek() == JsonToken.STRING) list.add(reader.nextString())
            else reader.skipValue()
        }
        reader.endArray()
        return list
    }
}
