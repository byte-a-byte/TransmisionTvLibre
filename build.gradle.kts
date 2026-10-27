plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.son.streamplayertv"
    compileSdk = 37 // O el SDK más reciente disponible

    defaultConfig {
        applicationId = "com.son.streamplayertv"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "4.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.6.1") // Agregado para compatibilidad UI
    implementation(libs.androidx.leanback)
    implementation(libs.glide)

    // Reproductor de video ExoPlayer para transmisiones HLS (.m3u8)
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // Retrofit y Convertidor Gson para peticiones HTTP
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
