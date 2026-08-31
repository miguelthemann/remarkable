/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.miguelthemann.remarkable"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.miguelthemann.remarkable"
        minSdk = 26
        targetSdk = 36
        maxSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true
        resourceConfigurations += listOf("en", "pt-rPT")
        manifestPlaceholders["redirectSchemeName"] = "remarkable"
        manifestPlaceholders["redirectHostName"] = "callback"

        ndk {
            abiFilters.clear()
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            versionNameSuffix = "-debug"
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
        buildConfig = true
    }

    packaging {
        jniLibs {
            excludes += setOf("lib/armeabi-v7a/**", "lib/x86/**", "lib/x86_64/**", "lib/armeabi/**")
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    bundle {
        density {
            enableSplit = false
        }
        abi {
            enableSplit = false
        }
        language {
            enableSplit = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.spotify.auth)
    implementation(libs.gson)
    implementation(files("libs/spotify-app-remote-release-0.8.0.aar"))
}

val spotifyAppRemoteAar = layout.projectDirectory.file("libs/spotify-app-remote-release-0.8.0.aar")
val spotifyAppRemoteUrl = uri(
    "https://github.com/spotify/android-sdk/releases/download/" +
        "v0.8.0-appremote_v2.1.0-auth/spotify-app-remote-release-0.8.0.aar",
)
val fetchSpotifyAppRemote = tasks.register("fetchSpotifyAppRemote") {
    description = "Downloads Spotify App Remote AAR from GitHub (not published to Maven)."
    group = "spotify"
    outputs.file(spotifyAppRemoteAar)
    doLast {
        val dest = spotifyAppRemoteAar.asFile
        if (dest.length() > 0L) return@doLast
        dest.parentFile.mkdirs()
        val url = spotifyAppRemoteUrl.toURL()
        logger.lifecycle("Downloading Spotify App Remote from $url")
        url.openStream().use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        check(dest.length() > 0L) { "Spotify App Remote download produced an empty file." }
    }
}

tasks.configureEach {
    if (name == "preBuild" || name.endsWith("AarMetadata")) {
        dependsOn(fetchSpotifyAppRemote)
    }
}
