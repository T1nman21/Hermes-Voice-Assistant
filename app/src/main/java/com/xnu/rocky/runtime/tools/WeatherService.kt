//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class WeatherService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getWeather(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&hourly=temperature_2m,weather_code&forecast_hours=6&timezone=auto"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext "Failed to fetch weather"

        try {
            val data = json.parseToJsonElement(body).jsonObject
            val current = data["current"]?.jsonObject
            val temp = current?.get("temperature_2m")?.jsonPrimitive?.doubleOrNull
            val humidity = current?.get("relative_humidity_2m")?.jsonPrimitive?.intOrNull
            val weatherCode = current?.get("weather_code")?.jsonPrimitive?.intOrNull
            val windSpeed = current?.get("wind_speed_10m")?.jsonPrimitive?.doubleOrNull

            val condition = weatherCodeToCondition(weatherCode ?: 0)

            val hourly = data["hourly"]?.jsonObject
            val hourlyTemps = hourly?.get("temperature_2m")?.jsonArray
            val hourlyCodes = hourly?.get("weather_code")?.jsonArray

            val forecastLines = (0 until minOf(6, hourlyTemps?.size ?: 0)).map { i ->
                val t = hourlyTemps?.get(i)?.jsonPrimitive?.doubleOrNull ?: 0.0
                val c = weatherCodeToCondition(hourlyCodes?.get(i)?.jsonPrimitive?.intOrNull ?: 0)
                "+${i + 1}h: ${t}°C, $c"
            }

            buildString {
                appendLine("Current: ${temp}°C, $condition")
                appendLine("Humidity: $humidity%, Wind: ${windSpeed} km/h")
                appendLine("Forecast:")
                forecastLines.forEach { appendLine("  $it") }
            }
        } catch (e: Exception) {
            "Weather data error: ${e.message}"
        }
    }

    private fun weatherCodeToCondition(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        in 45..48 -> "Foggy"
        in 51..55 -> "Drizzle"
        in 56..57 -> "Freezing drizzle"
        in 61..65 -> "Rain"
        in 66..67 -> "Freezing rain"
        in 71..75 -> "Snow"
        77 -> "Snow grains"
        in 80..82 -> "Rain showers"
        in 85..86 -> "Snow showers"
        95 -> "Thunderstorm"
        in 96..99 -> "Thunderstorm with hail"
        else -> "Unknown ($code)"
    }
}
