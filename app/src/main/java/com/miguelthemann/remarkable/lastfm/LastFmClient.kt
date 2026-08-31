/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.lastfm

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class LastFmTrack(
    val title: String,
    val artist: String,
    val album: String = "",
    val isNowPlaying: Boolean = false,
)

/**
 * Last.fm REST client. Signatures are MD5 soup; taste carefully.
 * Docs: https://www.last.fm/api
 */
class LastFmClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun recentTrack(apiKey: String, username: String): LastFmTrack? =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank() || username.isBlank()) return@withContext null
            val url = BASE +
                "?method=user.getRecentTracks" +
                "&user=${username.trim()}" +
                "&api_key=${apiKey.trim()}" +
                "&limit=1&format=json"
            val body = http.newCall(Request.Builder().url(url).get().build())
                .execute()
                .use { it.body?.string().orEmpty() }
            parseRecent(body)
        }

    suspend fun mobileSession(
        apiKey: String,
        sharedSecret: String,
        username: String,
        password: String,
    ): String = withContext(Dispatchers.IO) {
        val params = sortedMapOf(
            "api_key" to apiKey.trim(),
            "method" to "auth.getMobileSession",
            "password" to password,
            "username" to username.trim(),
        )
        params["api_sig"] = sign(params, sharedSecret)
        val form = FormBody.Builder().apply {
            params.forEach { (k, v) -> add(k, v) }
        }.build()
        val body = http.newCall(
            Request.Builder().url(BASE).post(form).build(),
        ).execute().use { it.body?.string().orEmpty() }
        val root = JsonParser.parseString(body).asJsonObject
        if (root.has("error")) {
            error(root.get("message")?.asString ?: "Last.fm auth failed")
        }
        root.getAsJsonObject("session")?.get("key")?.asString
            ?: error("No session key in Last.fm response")
    }

    suspend fun updateNowPlaying(
        apiKey: String,
        sharedSecret: String,
        sessionKey: String,
        artist: String,
        track: String,
        album: String?,
    ) = withContext(Dispatchers.IO) {
        val params = sortedMapOf(
            "api_key" to apiKey.trim(),
            "artist" to artist,
            "method" to "track.updateNowPlaying",
            "sk" to sessionKey,
            "track" to track,
        )
        if (!album.isNullOrBlank()) params["album"] = album
        params["api_sig"] = sign(params, sharedSecret)
        post(params)
    }

    suspend fun scrobble(
        apiKey: String,
        sharedSecret: String,
        sessionKey: String,
        artist: String,
        track: String,
        album: String?,
        timestampSec: Long,
    ) = withContext(Dispatchers.IO) {
        val params = sortedMapOf(
            "api_key" to apiKey.trim(),
            "artist" to artist,
            "method" to "track.scrobble",
            "sk" to sessionKey,
            "timestamp" to timestampSec.toString(),
            "track" to track,
        )
        if (!album.isNullOrBlank()) params["album"] = album
        params["api_sig"] = sign(params, sharedSecret)
        post(params)
    }

    private fun post(params: Map<String, String>) {
        val form = FormBody.Builder().apply {
            params.forEach { (k, v) -> add(k, v) }
            add("format", "json")
        }.build()
        http.newCall(Request.Builder().url(BASE).post(form).build())
            .execute()
            .use { /* fire and mostly forget */ }
    }

    private fun parseRecent(json: String): LastFmTrack? {
        if (json.isBlank()) return null
        val root = JsonParser.parseString(json).asJsonObject
        if (root.has("error")) return null
        val recent = root.getAsJsonObject("recenttracks") ?: return null
        val trackEl = recent.get("track") ?: return null
        val trackObj = when {
            trackEl.isJsonArray -> trackEl.asJsonArray.firstOrNull()?.asJsonObject
            trackEl.isJsonObject -> trackEl.asJsonObject
            else -> null
        } ?: return null
        val attr = trackObj.getAsJsonObject("@attr")
        val now = attr?.get("nowplaying")?.asString == "true"
        val artist = trackObj.get("artist")?.let {
            when {
                it.isJsonObject -> it.asJsonObject.get("#text")?.asString.orEmpty()
                else -> it.asString
            }
        }.orEmpty()
        val album = trackObj.get("album")?.asJsonObject?.get("#text")?.asString.orEmpty()
        val title = trackObj.get("name")?.asString.orEmpty()
        if (title.isBlank()) return null
        return LastFmTrack(title = title, artist = artist, album = album, isNowPlaying = now)
    }

    private fun sign(params: Map<String, String>, secret: String): String {
        val raw = params.toSortedMap().entries.joinToString("") { "${it.key}${it.value}" } + secret
        val digest = MessageDigest.getInstance("MD5").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }

    companion object {
        private const val BASE = "https://ws.audioscrobbler.com/2.0/"
    }
}
