package com.Ben.closetstylist

import android.app.Application
import com.Ben.closetstylist.di.AppContainer

class ClosetStylistApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
