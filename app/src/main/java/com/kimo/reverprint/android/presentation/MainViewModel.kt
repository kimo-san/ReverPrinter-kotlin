package com.kimo.reverprint.android.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimo.reverprint.android.presentation.entity.UserImagePreferences
import com.kimo.reverprint.android.presentation.entity.UserPrintPreferences
import com.kimo.reverprint.android.toImagePixels
import com.kimo.reverprint.domain.printer.DeviceManager
import com.kimo.reverprint.domain.printer.PrintMode
import com.kimo.reverprint.domain.printer.ThermalPrinter
import com.kimo.reverprint.tools.fonts.ColorSettings
import com.kimo.reverprint.tools.fonts.Font
import com.kimo.reverprint.tools.graphics.Color
import com.kimo.reverprint.tools.graphics.Monochrome
import com.kimo.reverprint.domain.images.BitmapTextConfig
import com.kimo.reverprint.domain.images.TextOnBitmapGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(
    private val deviceManager: DeviceManager,
    private val font: Font,
    private val textGen: TextOnBitmapGenerator
) : ViewModel() {

    private val scope = viewModelScope

    val loadingPreview get() = _loadingPreview.asStateFlow()
    private val _loadingPreview =
        MutableStateFlow(false)

    val imagePreview get() = _imagePreview.asStateFlow()
    private val _imagePreview =
        MutableStateFlow<DeviceManager.PrintPreviews?>(null)

    val device: StateFlow<ThermalPrinter?> = deviceManager.connectedTo
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)


    private var currentPreviewJob: Job? = null
    private fun runPreviewJob(
        block: suspend CoroutineScope.() -> Unit
    ) = scope.launch(Dispatchers.IO) {
        _loadingPreview.value = true
        currentPreviewJob?.cancel()
        currentPreviewJob = launch {
            block()
            _loadingPreview.value = false
        }
    }

    fun findAndConnect() = scope.launch {
        loop@ while (isActive) {
            runCatching {
                val device = deviceManager.findAvailable().first()
                deviceManager.connect(device)
                break@loop
            }
        }
    }

    fun setPreview(
        text: String,
        imagePrefs: UserImagePreferences,
        printPrefs: UserPrintPreferences
    ) = runPreviewJob {

        val deviceCaps = device.value!!.capabilities

        val fntSize = imagePrefs.fontSize.coerceIn(1, deviceCaps.printWidth)
        val img = textGen.generatePixels(
            text, BitmapTextConfig(
                width = deviceCaps.printWidth,
                letterHeight = fntSize,
                lineSpacing = 0,
                letterSpacing = 0,
                colors = ColorSettings(
                    model = Monochrome,
                    foreground = Color(0x0),
                    background = Color(0x1)
                ),
                font = font
            )
        )

        _imagePreview.update {
            deviceManager.generatePreviews(
                img, DeviceManager.PrintConfig(
                    addSpaceAfterPrint = printPrefs.addSpaceAfterPrint,
                    ditherImage = imagePrefs.dither
                )
            )
        }
    }

    fun setPreview(
        image: Bitmap,
        imagePrefs: UserImagePreferences,
        printPrefs: UserPrintPreferences
    ) = runPreviewJob {
        _imagePreview.update {
            deviceManager.generatePreviews(
                image.toImagePixels(),
                DeviceManager.PrintConfig(
                    addSpaceAfterPrint = printPrefs.addSpaceAfterPrint,
                    ditherImage = imagePrefs.dither
                )
            )
        }
    }

    fun print(mode: PrintMode) = scope.launch {
        _imagePreview.value?.let {
            deviceManager.print(it, mode)
        } ?: println("preview is not provided")
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }

}