package com.Ben.closetstylist

import android.app.Application
import com.Ben.closetstylist.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ClosetStylistApplication : Application() {
    // Survives the lifetime of the process — used for fire-and-forget background tasks
    // (e.g. generating Claude item descriptions after the user has already navigated away).
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
