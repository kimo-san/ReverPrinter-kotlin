package com.kimo.reverprint.androidData

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        setupKoin()
    }
}