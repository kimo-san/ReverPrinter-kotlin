package com.kimo.reverprint.androidData

import android.app.Application
import com.kimo.reverprint.androidPresentation.MainViewModel
import com.kimo.reverprint.interactors.tinyprint.TinyprintController
import com.kimo.reverprint.interactors.tinyprint.units.ProtocolImpl
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {

    viewModel<MainViewModel> {
        MainViewModel(
            controller = TinyprintController(
                AndroidBluetoothLeController(androidContext()),
                ProtocolImpl()
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