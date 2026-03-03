package com.kimo.reverprint

import android.app.Application
import com.kimo.reverprint.di.setupKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        setupKoin()
    }
}