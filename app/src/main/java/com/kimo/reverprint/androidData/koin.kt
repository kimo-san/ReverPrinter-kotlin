package com.kimo.reverprint.androidData

import android.app.Application
import com.kimo.reverprint.tools.bluetooth.BluetoothLeController
import com.kimo.reverprint.useCases.tinyprint.units.ProtocolImpl
import com.kimo.reverprint.useCases.tinyprint.TinyprintController
import com.kimo.reverprint.domain.DeviceController
import com.kimo.reverprint.androidPresentation.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {

    single<BluetoothLeController> { AndroidBluetoothLeController(androidContext()) }
    single<DeviceController> {
        TinyprintController(get(), ProtocolImpl())
    }

    viewModel<MainViewModel> { MainViewModel(get()) }
}

fun Application.setupKoin() = startKoin {
    androidLogger()
    androidContext(this@setupKoin)
    modules(koinModule)
}