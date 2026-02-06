package support.ditto.dittoMovies

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import live.ditto.Ditto
import live.ditto.DittoIdentity
import live.ditto.android.DefaultAndroidDittoDependencies
import support.ditto.dittoMovies.DittoHandler.Companion.ditto
import timber.log.Timber

class MoviesApplication : Application() {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
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

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        ioScope.launch {
            setupDitto()
        }
    }

    private suspend fun setupDitto() {
        Timber.d("🚀 Setting up Ditto...")
        val androidDependencies = DefaultAndroidDittoDependencies(applicationContext)

        val appId = BuildConfig.DITTO_APP_ID
        val token = BuildConfig.DITTO_PLAYGROUND_TOKEN
        val authUrl = BuildConfig.DITTO_AUTH_URL
        val webSocketURL = BuildConfig.DITTO_WEBSOCKET_URL

        Timber.d("🔑 AppId: $appId")
        Timber.d("🔐 Token: ${token.take(8)}...")
        Timber.d("🌐 AuthUrl: $authUrl")
        Timber.d("🔌 WebSocketURL: $webSocketURL")

        val enableDittoCloudSync = false
        Timber.d("☁️ Cloud sync enabled: $enableDittoCloudSync")

        val identity = DittoIdentity.OnlinePlayground(
            dependencies = androidDependencies,
            appId = appId,
            token = token,
            customAuthUrl = authUrl,
            enableDittoCloudSync = enableDittoCloudSync
        )
        Timber.d("🪪 Identity created: OnlinePlayground")

        ditto = Ditto(androidDependencies, identity)
        ditto.updateTransportConfig { config ->
            config.connect.websocketUrls.add(webSocketURL)
        }
        Timber.d("🔧 Transport config updated with WebSocket URL")

        ditto.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false")
        Timber.d("⚙️ DQL strict mode disabled")

        ditto.disableSyncWithV3()
        Timber.d("✅ Ditto setup complete!")
    }
}
