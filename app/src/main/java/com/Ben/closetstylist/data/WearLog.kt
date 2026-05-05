package com.Ben.closetstylist.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "wear_logs",
    foreignKeys = [ForeignKey(
        entity = ClothingItem::class,
        parentColumns = ["id"],
        childColumns = ["itemId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("itemId")],
)
data class WearLog(
    @PrimaryKey val id: String,
    val itemId: String,
    val wornOn: LocalDate,
)
