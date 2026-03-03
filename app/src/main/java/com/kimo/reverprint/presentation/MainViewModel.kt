package com.kimo.reverprint.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimo.reverprint.domain.Printer
import com.kimo.reverprint.domain.ThermalPrinter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val printer: Printer
): ViewModel() {

    var imagePreview = MutableStateFlow<Printer.PrintPreviews?>(null)
    val currentPrinter get() = printer.connectedTo

    fun findAndConnect() = viewModelScope.launch {
        val device = printer.findAvailable().first()
        printer.connect(device)
    }

    fun setPreview(image: Bitmap) = viewModelScope.launch {
        imagePreview.update { printer.generatePreviews(image) }
    }

}