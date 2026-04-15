package com.kimo.reverprint.android

import android.app.Application
import com.kimo.reverprint.android.presentation.MainViewModel
import com.kimo.reverprint.android.data.AndroidBluetoothLeController
import com.kimo.reverprint.android.data.LoadedFontImpl
import com.kimo.reverprint.providers.tinyprint.TinyprintManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {

    viewModel<MainViewModel> {
        MainViewModel(
            controller = TinyprintManager(
                AndroidBluetoothLeController(androidContext())
            ),
            font = LoadedFontImpl(androidContext())
        )
    }
}

fun Application.setupKoin() = startKoin {
    androidLogger()
    androidContext(this@setupKoin)
    modules(koinModule)
}