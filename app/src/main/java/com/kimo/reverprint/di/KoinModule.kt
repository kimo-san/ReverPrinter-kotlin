package com.kimo.reverprint.di

import android.app.Application
import com.kimo.reverprint.data.tinyprint.TinyprintPrinter
import com.kimo.reverprint.domain.Printer
import com.kimo.reverprint.presentation.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {
    single<Printer> { TinyprintPrinter(get()) }
    viewModel<MainViewModel> { MainViewModel(get()) }
}

fun Application.setupKoin() = startKoin {
    androidLogger()
    androidContext(this@setupKoin)
    modules(koinModule)
}