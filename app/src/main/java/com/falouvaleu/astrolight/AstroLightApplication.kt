package com.falouvaleu.astrolight

import android.app.Application

class AstroLightApplication: Application() {


    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationController.initialize(this)
    }


    companion object {
        lateinit var instance: Application
    }
}