package com.kimo.reverprint.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.domain.Printer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val printer: Printer
): ViewModel() {

    var imagePreview = MutableStateFlow<Printer.PrintPreviews?>(null)
    val device get() = printer.connectedTo
    val printConfiguration = Printer.PrintConfiguration(
        addSpaceAfter = true
    )

    fun findAndConnect() = viewModelScope.launch {
        val device = printer.findAvailable().first()
        printer.connect(device)
    }

    fun setPreview(image: Bitmap) = viewModelScope.launch {
        imagePreview.update { printer.generatePreviews(image) }
    }

    fun print4bpp() = viewModelScope.launch {
        imagePreview.value?.let {
            printer.print(it, PrintMode.BPP4, printConfiguration)
        } ?: println("preview is not provided")
    }

    fun print1bpp() = viewModelScope.launch {
        imagePreview.value?.let {
            printer.print(it, PrintMode.BPP1, printConfiguration)
        }
    }

}