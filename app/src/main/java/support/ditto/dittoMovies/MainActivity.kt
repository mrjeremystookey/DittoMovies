package support.ditto.dittoMovies

import android.os.Bundle
import timber.log.Timber
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import live.ditto.transports.DittoSyncPermissions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("🎬 MainActivity onCreate")

        setContent {
            Root()
        }

        requestMissingPermissions()
    }

    private fun requestMissingPermissions() {
        val missingPermissions = DittoSyncPermissions(this).missingPermissions()
        if (missingPermissions.isNotEmpty()) {
            Timber.d("🔐 Requesting ${missingPermissions.size} missing permissions: ${missingPermissions.toList()}")
            this.requestPermissions(missingPermissions, 0)
        } else {
            Timber.d("✅ All permissions already granted")
        }
    }
}
