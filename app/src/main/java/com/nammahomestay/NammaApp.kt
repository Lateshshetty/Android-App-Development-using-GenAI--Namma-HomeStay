package com.nammahomestay

import android.app.Application
import android.content.Context
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration

class NammaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
