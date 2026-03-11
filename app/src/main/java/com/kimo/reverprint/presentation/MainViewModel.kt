package com.kimo.reverprint.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.domain.Printer
import com.kimo.reverprint.domain.ThermalPrinter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val printer: Printer
): ViewModel() {

    private val scope = CoroutineScope(viewModelScope.coroutineContext.minusKey(Job) + SupervisorJob())
    var imagePreview = MutableStateFlow<Printer.PrintPreviews?>(null)
    val device: StateFlow<ThermalPrinter?>
        get() = printer.connectedTo.stateIn(scope, SharingStarted.WhileSubscribed(5000), null)
    val configuration = Printer.Configuration(
        addSpaceAfterPrint = true,
        ditherImage = true
    )

    fun findAndConnect() = scope.launch {
        loop@ while (true) {
            runCatching {
                val device = printer.findAvailable().first()
                printer.connect(device)
                break@loop
            }
        }
    }

    fun setPreview(image: Bitmap) = scope.launch {
        imagePreview.update { printer.generatePreviews(image, configuration) }
    }

    fun print(mode: PrintMode) = scope.launch {
        imagePreview.value?.let {
            printer.print(it, mode)
        } ?: println("preview is not provided")
    }

}