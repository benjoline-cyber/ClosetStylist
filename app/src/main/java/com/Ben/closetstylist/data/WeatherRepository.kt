package com.Ben.closetstylist.data

import com.Ben.closetstylist.domain.WeatherInfo
import com.Ben.closetstylist.network.WeatherApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

class WeatherRepository(private val settingsRepository: SettingsRepository) {

    private val json = Json { ignoreUnknownKeys = true }

    private val service: WeatherApiService = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(WeatherApiService::class.java)

    suspend fun fetchWeather(lat: Double, lon: Double): WeatherInfo {
        val apiKey = settingsRepository.weatherApiKey.value
        check(apiKey.isNotEmpty()) { "Weather API key not set" }
        val response = service.getForecast(lat = lat, lon = lon, apiKey = apiKey)
        val items = response.list.take(4)
        val current = items.firstOrNull()
        val temps = items.map { it.main.temp }
        val firstRainy = items.firstOrNull { item ->
            item.rain?.threeHour?.let { it > 0 } == true ||
                item.weather.any { w ->
                    w.description.contains("rain", ignoreCase = true) ||
                        w.description.contains("drizzle", ignoreCase = true)
                }
        }
        val rainAt = firstRainy?.dtTxt?.let { txt ->
            txt.substringAfter(" ").take(5)
        }
        return WeatherInfo(
            temperatureCelsius = current?.main?.temp ?: 0.0,
            description = current?.weather?.firstOrNull()?.description ?: "",
            cityName = response.city.name,
            lowTempNext12h = temps.minOrNull() ?: (current?.main?.temp ?: 0.0),
            highTempNext12h = temps.maxOrNull() ?: (current?.main?.temp ?: 0.0),
            rainExpectedAt = rainAt,
        )
    }
}
