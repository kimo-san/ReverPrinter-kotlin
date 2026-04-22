package com.kimo.reverprint.android

import android.app.Application
import com.kimo.reverprint.android.presentation.MainViewModel
import com.kimo.reverprint.android.data.AndroidBluetoothLeController
import com.kimo.reverprint.android.data.LoadedFontImpl
import com.kimo.reverprint.data.pixels.BitmapFabric
import com.kimo.reverprint.domain.images.TextOnBitmapGenerator
import com.kimo.reverprint.domain.printer.DeviceManager
import com.kimo.reverprint.extensions.bitmaps.BitmapConverter
import com.kimo.reverprint.extensions.bitmaps.BitmapConverterImpl
import com.kimo.reverprint.extensions.bitmaps.TextOnBitmapGeneratorImpl
import com.kimo.reverprint.providers.tinyprint.PreviewGenerator
import com.kimo.reverprint.providers.tinyprint.TinyprintBluetoothController
import com.kimo.reverprint.providers.tinyprint.TinyprintManager
import com.kimo.reverprint.tools.bluetooth.BluetoothLeController
import com.kimo.reverprint.tools.graphics.BitmapCreator
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.io.File
import java.util.UUID

val koinModule = module {

    // Bluetooth
    single<BluetoothLeController> {
        AndroidBluetoothLeController(androidContext())
    }

    // Bitmaps
    single<BitmapCreator> {
        BitmapFabric(
            createFile = {
                val id = UUID.randomUUID()
                File(androidContext().filesDir, "saved_bitmap_$id.bin")
            }
        )
    }

    single<BitmapConverter> {
        BitmapConverterImpl(get())
    }

    // Text
    single<TextOnBitmapGenerator> {
        TextOnBitmapGeneratorImpl(get())
    }

    // Tinyprint
    single<DeviceManager> {
        val bluetooth = TinyprintBluetoothController(get())
        val preview = PreviewGenerator(get())
        TinyprintManager(
            deviceController = bluetooth,
            previewGenerator = preview
        )
    }

    // - - - - - - - - - - - - - - - - - -
    // Entry point
    viewModel<MainViewModel> {
        MainViewModel(
            deviceManager = get(),
            font = LoadedFontImpl(androidContext(), get()),
            textGen = get(),
        )
    }
}

fun Application.setupKoin() = startKoin {
    androidLogger()
    androidContext(this@setupKoin)
    modules(koinModule)
}