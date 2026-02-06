package support.ditto.dittoMovies

import android.app.Application
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import live.ditto.Ditto
import live.ditto.DittoIdentity
import live.ditto.android.DefaultAndroidDittoDependencies
import support.ditto.dittoMovies.DittoHandler.Companion.ditto

class MoviesApplication : Application() {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "MoviesApplication"
        private var instance: MoviesApplication? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        ioScope.launch {
            setupDitto()
        }
    }

    private suspend fun setupDitto() {
        val androidDependencies = DefaultAndroidDittoDependencies(applicationContext)

        val appId = BuildConfig.DITTO_APP_ID
        val token = BuildConfig.DITTO_PLAYGROUND_TOKEN
        val authUrl = BuildConfig.DITTO_AUTH_URL
        val webSocketURL = BuildConfig.DITTO_WEBSOCKET_URL

        Log.d(TAG, "AppId: $appId")
        Log.d(TAG, "AuthUrl: $authUrl")
        Log.d(TAG, "WebSocketURL: $webSocketURL")

        val enableDittoCloudSync = false

        val identity = DittoIdentity.OnlinePlayground(
            dependencies = androidDependencies,
            appId = appId,
            token = token,
            customAuthUrl = authUrl,
            enableDittoCloudSync = enableDittoCloudSync
        )

        ditto = Ditto(androidDependencies, identity)
        ditto.updateTransportConfig { config ->
            config.connect.websocketUrls.add(webSocketURL)
        }

        ditto.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false")
        ditto.disableSyncWithV3()
    }
}
