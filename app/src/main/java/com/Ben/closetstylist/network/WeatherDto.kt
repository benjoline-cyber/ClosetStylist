package com.Ben.closetstylist.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OWMForecastResponse(
    val list: List<OWMForecastItem> = emptyList(),
    val city: OWMCity = OWMCity(),
)

@Serializable
data class OWMForecastItem(
    val main: OWMMain,
    val weather: List<OWMWeather> = emptyList(),
    val rain: OWMRain? = null,
    @SerialName("dt_txt") val dtTxt: String = "",
)

@Serializable
data class OWMCity(
    val name: String = "",
)

@Serializable
data class OWMRain(
    @SerialName("3h") val threeHour: Double = 0.0,
)

@Serializable
data class OWMWeather(
    val description: String = "",
)

@Serializable
data class OWMMain(
    val temp: Double,
)
