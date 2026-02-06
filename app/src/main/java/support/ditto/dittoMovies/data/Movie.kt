package support.ditto.dittoMovies.data

import timber.log.Timber
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

data class Movie(
    val _id: String = UUID.randomUUID().toString(),
    val title: String,
    val year: Int = 0,
    val plot: String = "",
    val genres: List<String> = emptyList(),
    val rated: String = "",
    val runtime: Int = 0,
    val poster: String = "",
    val directors: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val imdbRating: Double = 0.0,
    val deleted: Boolean = false,
) {
    companion object {

        fun fromJson(jsonString: String): Movie {
            return try {
                val json = JSONObject(jsonString)
                val movie = Movie(
                    _id = json["_id"].toString(),
                    title = json.optString("title", ""),
                    year = json.optInt("year", 0),
                    plot = json.optString("plot", ""),
                    genres = jsonArrayToList(json.optJSONArray("genres")),
                    rated = json.optString("rated", ""),
                    runtime = json.optInt("runtime", 0),
                    poster = json.optString("poster", ""),
                    directors = jsonArrayToList(json.optJSONArray("directors")),
                    cast = jsonArrayToList(json.optJSONArray("cast")),
                    imdbRating = json.optDouble("imdbRating", 0.0),
                    deleted = json.optBoolean("deleted", false)
                )
                Timber.v("🎥 Parsed: '${movie.title}' (${movie.year})")
                movie
            } catch (e: JSONException) {
                Timber.e("❌ Unable to convert JSON to Movie", e)
                Movie(title = "")
            }
        }

        private fun jsonArrayToList(array: org.json.JSONArray?): List<String> {
            if (array == null) return emptyList()
            return (0 until array.length()).map { array.getString(it) }
        }
    }
}
