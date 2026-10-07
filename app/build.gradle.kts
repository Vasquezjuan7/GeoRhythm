import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Leer variables de local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val mapTilerApiKey: String = localProperties.getProperty("MAPTILER_API_KEY") ?: ""
val supabaseUrl: String = localProperties.getProperty("SUPABASE_URL") ?: ""
val supabaseKey: String = localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY") ?: ""
val spotifyClientId: String = localProperties.getProperty("SPOTIFY_CLIENT_ID") ?: ""

android {
    namespace = "com.example.georhythm"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.georhythm"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Exponer claves a la aplicación
        buildConfigField("String", "APPLICATION_ID", "\"com.example.georhythm\"")
        buildConfigField("String", "MAPTILER_API_KEY", "\"$mapTilerApiKey\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$spotifyClientId\"")
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // MapLibre Native para mapas vectoriales (MapTiler)
    implementation("org.maplibre.gl:android-sdk:11.5.1")
    // Servicios de ubicación para GPS (velocidad y tracking)
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Coil para portadas; Media3 ExoPlayer para streaming con control de tempo
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.media3:media3-exoplayer:1.8.0")

    // Iconos extendidos para tu reproductor de música (Play, Pause, Skip)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}