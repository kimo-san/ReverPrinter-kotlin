package com.kimo.reverprint.android

import android.app.Application
import com.kimo.reverprint.android.di.setupKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        setupKoin()
    }
}