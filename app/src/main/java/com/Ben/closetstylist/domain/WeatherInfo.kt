package com.Ben.closetstylist.domain

data class WeatherInfo(
    val temperatureCelsius: Double,
    val description: String,
    val cityName: String,
    val lowTempNext12h: Double = temperatureCelsius,
    val highTempNext12h: Double = temperatureCelsius,
    val rainExpectedAt: String? = null,
) {
    fun summary(): String {
        val temp = temperatureCelsius.toInt()
        val city = if (cityName.isNotBlank()) " in $cityName" else ""
        val swing = highTempNext12h - lowTempNext12h
        val range = if (swing >= 2) " (${lowTempNext12h.toInt()}–${highTempNext12h.toInt()}°C)" else ""
        val rain = if (rainExpectedAt != null) ", rain ~$rainExpectedAt" else ""
        return "${temp}°C$range, $description$city$rain"
    }
}
