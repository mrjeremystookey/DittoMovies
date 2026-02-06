import com.android.build.api.variant.BuildConfigField
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun loadEnvProperties(): Properties {
    val properties = Properties()
    val envFile = rootProject.file(".env")

    if (envFile.exists()) {
        FileInputStream(envFile).use { properties.load(it) }
    } else {
        val requiredEnvVars = listOf(
            "DITTO_APP_ID",
            "DITTO_PLAYGROUND_TOKEN",
            "DITTO_AUTH_URL",
            "DITTO_WEBSOCKET_URL"
        )
        for (envVar in requiredEnvVars) {
            val value = System.getenv(envVar)
                ?: throw RuntimeException("Required environment variable $envVar not found")
            properties[envVar] = value
        }
    }
    return properties
}

androidComponents {
    onVariants {
        val prop = loadEnvProperties()
        val buildConfigFields = mapOf(
            "DITTO_APP_ID" to "Ditto application ID",
            "DITTO_PLAYGROUND_TOKEN" to "Ditto playground token",
            "DITTO_AUTH_URL" to "Ditto authentication URL",
            "DITTO_WEBSOCKET_URL" to "Ditto websocket URL"
        )
        buildConfigFields.forEach { (key, description) ->
            it.buildConfigFields?.put(
                key,
                BuildConfigField("String", "\"${prop[key]}\"", description)
            )
        }
    }
}

android {
    namespace = "support.ditto.dittoMovies"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "support.ditto.dittoMovies"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.runtime.livedata)

    // Image loading
    implementation(libs.coil.compose)

    // Ditto SDK
    implementation(libs.live.ditto)
    implementation(libs.ditto.tools)

    // Logging
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
