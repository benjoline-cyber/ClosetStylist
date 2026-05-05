package com.Ben.closetstylist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ClothingItem::class, InspirationPhoto::class, WearLog::class, OutfitFeedback::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ClosetDatabase : RoomDatabase() {

    abstract fun clothingItemDao(): ClothingItemDao
    abstract fun inspirationPhotoDao(): InspirationPhotoDao
    abstract fun wearLogDao(): WearLogDao
    abstract fun outfitFeedbackDao(): OutfitFeedbackDao

    companion object {
        @Volatile
        private var instance: ClosetDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS outfit_feedback (
                        id TEXT NOT NULL PRIMARY KEY,
                        itemIds TEXT NOT NULL,
                        feedbackType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        persona TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        fun getInstance(context: Context): ClosetDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder<ClosetDatabase>(
                    context.applicationContext,
                    "closet_stylist.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
