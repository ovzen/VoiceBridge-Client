plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ovzenproject.voicebridgeclient"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ovzenproject.voicebridgeclient"
        minSdk = 30
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Используем версии, совместимые с compileSdk 34
    implementation("androidx.core:core-ktx:1.12.0") // Понижено с 1.15.0
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.11.00")) // Стабильный BOM
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // Accompanist permissions – последняя стабильная версия для Compose 2024.11
    implementation("com.google.accompanist:accompanist-permissions:0.35.0-alpha")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // или использовать встроенный HttpsURLConnection
    implementation("com.google.code.gson:gson:2.10.1")
}