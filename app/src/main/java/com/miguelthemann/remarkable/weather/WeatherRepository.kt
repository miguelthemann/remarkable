/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class WeatherSnapshot(
    val temperatureC: Double,
    val apparentC: Double?,
    val humidity: Int?,
    val windKmh: Double?,
    val weatherCode: Int,
    val place: String,
)

@Serializable
private data class ForecastResponse(
    val current: CurrentWeather? = null,
)

@Serializable
private data class CurrentWeather(
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("apparent_temperature") val apparent: Double? = null,
    @SerialName("relative_humidity_2m") val humidity: Int? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
)

@Serializable
private data class GeocodingResponse(
    val results: List<GeocodingHit> = emptyList(),
)

@Serializable
private data class GeocodingHit(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("admin1") val region: String? = null,
    val country: String? = null,
)

class WeatherRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun fetch(latitude: Double, longitude: Double, place: String): WeatherSnapshot {
        val url = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
            .addQueryParameter("latitude", latitude.toString())
            .addQueryParameter("longitude", longitude.toString())
            .addQueryParameter(
                "current",
                "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m",
            )
            .addQueryParameter("timezone", "auto")
            .build()
        val body = execute(url.toString())
        val parsed = json.decodeFromString<ForecastResponse>(body)
        val current = parsed.current ?: error("No current weather")
        return WeatherSnapshot(
            temperatureC = current.temperature ?: error("No temperature"),
            apparentC = current.apparent,
            humidity = current.humidity,
            windKmh = current.windSpeed,
            weatherCode = current.weatherCode ?: 0,
            place = place,
        )
    }

    fun geocodeCity(query: String): Triple<Double, Double, String>? {
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", query)
            .addQueryParameter("count", "1")
            .addQueryParameter("language", "pt")
            .addQueryParameter("format", "json")
            .build()
        val body = execute(url.toString())
        val hit = json.decodeFromString<GeocodingResponse>(body).results.firstOrNull() ?: return null
        val label = listOfNotNull(hit.name, hit.region, hit.country).joinToString(", ")
        return Triple(hit.latitude, hit.longitude, label)
    }

    private fun execute(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Remarkable/1.0 (MIT; desk clock)")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }
}

fun Double.celsiusToFahrenheit(): Double = this * 9.0 / 5.0 + 32.0
