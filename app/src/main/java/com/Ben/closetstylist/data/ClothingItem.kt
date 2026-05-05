package com.Ben.closetstylist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "clothing_items")
data class ClothingItem(
    @PrimaryKey val id: String,
    val imagePath: String,
    val category: ClothingCategory,
    val colorTags: List<String>,
    val seasonTags: List<Season>,
    val description: String,
    val lastWornDate: LocalDate?,
)
