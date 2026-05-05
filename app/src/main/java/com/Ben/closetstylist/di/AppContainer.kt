package com.Ben.closetstylist.di

import android.content.Context
import com.Ben.closetstylist.data.ClosetDatabase
import com.Ben.closetstylist.data.ClothingRepository
import com.Ben.closetstylist.data.InspirationRepository
import com.Ben.closetstylist.data.OutfitFeedbackRepository
import com.Ben.closetstylist.data.SettingsRepository
import com.Ben.closetstylist.data.WeatherRepository
import com.Ben.closetstylist.network.ClaudeRepository

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context.applicationContext)
    val claudeRepository = ClaudeRepository(settingsRepository)
    val weatherRepository = WeatherRepository(settingsRepository)

    private val database = ClosetDatabase.getInstance(context)

    val clothingRepository = ClothingRepository(
        clothingItemDao = database.clothingItemDao(),
        wearLogDao = database.wearLogDao(),
    )

    val inspirationRepository = InspirationRepository(
        dao = database.inspirationPhotoDao(),
    )

    val outfitFeedbackRepository = OutfitFeedbackRepository(
        dao = database.outfitFeedbackDao(),
    )
}
