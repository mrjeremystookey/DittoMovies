package support.ditto.dittoMovies

import kotlinx.coroutines.CompletableDeferred
import live.ditto.Ditto

class DittoHandler {
    companion object {
        lateinit var ditto: Ditto
        val ready = CompletableDeferred<Unit>()
    }
}
