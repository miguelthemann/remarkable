/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.update

import com.miguelthemann.remarkable.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val codename: String,
    val htmlUrl: String,
)

sealed interface AppUpdateStatus {
    data object Idle : AppUpdateStatus
    data object Checking : AppUpdateStatus
    data object UpToDate : AppUpdateStatus
    data class Available(val info: AppUpdateInfo) : AppUpdateStatus
    data class Failed(val message: String) : AppUpdateStatus
}

@Serializable
private data class LatestManifest(
    val versionCode: Int,
    val versionName: String,
    val codename: String = "",
    @SerialName("htmlUrl") val htmlUrl: String = "",
)

@Serializable
private data class GhRelease(
    val name: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("tag_name") val tagName: String? = null,
    val assets: List<GhAsset> = emptyList(),
)

@Serializable
private data class GhAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)

class AppUpdateChecker(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val owner: String = "miguelthemann",
    private val repo: String = "remarkable",
) {
    fun check(): AppUpdateStatus {
        return try {
            val remote = fetchLatestManifest() ?: return AppUpdateStatus.UpToDate
            if (remote.versionCode > BuildConfig.VERSION_CODE) {
                AppUpdateStatus.Available(remote)
            } else {
                AppUpdateStatus.UpToDate
            }
        } catch (t: Throwable) {
            AppUpdateStatus.Failed(t.message ?: "update check failed")
        }
    }

    private fun fetchLatestManifest(): AppUpdateInfo? {
        val release = get("https://api.github.com/repos/$owner/$repo/releases/latest")
            ?.let { json.decodeFromString<GhRelease>(it) }
            ?: return null

        val manifestAsset = release.assets.firstOrNull { it.name.equals("latest.json", ignoreCase = true) }
        if (manifestAsset != null) {
            val body = get(manifestAsset.browserDownloadUrl) ?: return null
            val parsed = json.decodeFromString<LatestManifest>(body)
            return AppUpdateInfo(
                versionCode = parsed.versionCode,
                versionName = parsed.versionName,
                codename = parsed.codename.ifBlank { release.name.orEmpty() },
                htmlUrl = parsed.htmlUrl.ifBlank {
                    release.htmlUrl ?: "https://github.com/$owner/$repo/releases/latest"
                },
            )
        }

        // Fallback: parse "1.0.0 Quiet Peak" from the release title / tag.
        val label = release.name?.trim().orEmpty().ifBlank { release.tagName.orEmpty() }
        val parts = label.removePrefix("v").split(' ', limit = 2)
        val versionName = parts.getOrNull(0)?.takeIf { it.any(Char::isDigit) } ?: return null
        val codename = parts.getOrNull(1).orEmpty()
        val code = versionNameToCode(versionName) ?: return null
        return AppUpdateInfo(
            versionCode = code,
            versionName = versionName,
            codename = codename,
            htmlUrl = release.htmlUrl ?: "https://github.com/$owner/$repo/releases/latest",
        )
    }

    private fun get(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Remarkable-Android/${BuildConfig.VERSION_NAME}")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.string()
        }
    }

    /** Best-effort when latest.json is missing: 1.2.3 → 10203 */
    private fun versionNameToCode(name: String): Int? {
        val bits = name.split('.').mapNotNull { it.toIntOrNull() }
        if (bits.isEmpty()) return null
        val major = bits.getOrElse(0) { 0 }
        val minor = bits.getOrElse(1) { 0 }
        val patch = bits.getOrElse(2) { 0 }
        return major * 10_000 + minor * 100 + patch
    }
}
