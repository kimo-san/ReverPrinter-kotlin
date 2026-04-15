package com.kimo.reverprint.android

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        setupKoin()
    }
}