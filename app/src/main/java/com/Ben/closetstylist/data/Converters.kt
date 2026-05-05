package com.Ben.closetstylist.data

import androidx.room.TypeConverter
import com.Ben.closetstylist.domain.StylistPersona
import java.time.Instant
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(",")

    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun toSeasonList(value: String): List<Season> =
        if (value.isEmpty()) emptyList() else value.split(",").map { Season.valueOf(it) }

    @TypeConverter
    fun fromSeasonList(list: List<Season>): String = list.joinToString(",") { it.name }

    @TypeConverter
    fun toClothingCategory(value: String): ClothingCategory = ClothingCategory.valueOf(value)

    @TypeConverter
    fun fromClothingCategory(category: ClothingCategory): String = category.name

    // ISO-8601 string "YYYY-MM-DD"; lexicographic order matches date order so SQL < / > work correctly.
    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toFeedbackType(value: String): FeedbackType = FeedbackType.valueOf(value)

    @TypeConverter
    fun fromFeedbackType(type: FeedbackType): String = type.name

    @TypeConverter
    fun toStylistPersona(value: String): StylistPersona = StylistPersona.valueOf(value)

    @TypeConverter
    fun fromStylistPersona(persona: StylistPersona): String = persona.name
}
