package com.Ben.closetstylist.domain

import com.Ben.closetstylist.data.ClothingItem
import java.time.LocalDate

object PromptBuilder {

    fun buildOutfitPrompt(
        availableItems: List<ClothingItem>,
        weather: WeatherInfo,
        persona: StylistPersona,
        recentRejections: List<String> = emptyList(),
    ): String {
        val season = seasonFrom(LocalDate.now().monthValue)
        val itemLines = availableItems.joinToString("\n") { item ->
            val desc = item.description.ifBlank { item.category.displayName() }
            "- ID: ${item.id} | $desc"
        }
        val swing = weather.highTempNext12h - weather.lowTempNext12h
        val weatherNotes = buildList {
            if (swing > 8) add("Temperature swings ${swing.toInt()}°C over the next 12 hours — layers are a smart call.")
            weather.rainExpectedAt?.let { add("Rain expected around $it — prefer water-resistant fabrics or easy-to-remove layers.") }
        }.joinToString("\n")

        val rejectionBlock = if (recentRejections.isNotEmpty()) {
            "\nDo NOT repeat these rejected combinations:\n" +
                recentRejections.joinToString("\n") { "- $it" }
        } else ""

        return """
Current weather: ${weather.summary()}
Season: $season
Style persona: ${persona.description}
${if (weatherNotes.isNotBlank()) "\nWeather notes:\n$weatherNotes" else ""}$rejectionBlock
Available clothing items:
$itemLines

Suggest 3 complete, wearable outfits from the items above, styled in the voice of the persona above and appropriate for the weather and season.
Use only IDs from the list above. Prefer items not recently worn.
Return ONLY valid JSON — no preamble, no explanation:
{"outfits":[{"itemIds":["id1","id2"],"rationale":"one sentence why"},{"itemIds":["id3","id4"],"rationale":"..."},{"itemIds":["id5","id6"],"rationale":"..."}]}
""".trimIndent()
    }

    private fun seasonFrom(month: Int): String = when (month) {
        in 3..5 -> "Spring"
        in 6..8 -> "Summer"
        in 9..11 -> "Fall"
        else -> "Winter"
    }
}
