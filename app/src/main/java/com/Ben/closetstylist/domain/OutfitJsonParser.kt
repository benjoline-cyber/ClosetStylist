package com.Ben.closetstylist.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object OutfitJsonParser {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class OutfitsWrapper(val outfits: List<OutfitEntry>)

    @Serializable
    private data class OutfitEntry(
        @SerialName("itemIds") val itemIds: List<String>,
        val rationale: String,
    )

    fun parse(rawJson: String, availableIds: Set<String>): List<ParsedOutfit> {
        val cleaned = extractJson(rawJson)
        val wrapper = json.decodeFromString<OutfitsWrapper>(cleaned)
        return wrapper.outfits
            .map { entry ->
                ParsedOutfit(
                    itemIds = entry.itemIds.filter { it in availableIds },
                    rationale = entry.rationale,
                )
            }
            .filter { it.itemIds.isNotEmpty() }
    }

    // Trims any text Claude adds before/after the JSON object.
    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }
}
