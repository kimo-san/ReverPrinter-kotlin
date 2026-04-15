package com.kimo.reverprint.android.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimo.reverprint.android.toImagePixels
import com.kimo.reverprint.domain.DeviceManager
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.domain.ThermalPrinter
import com.kimo.reverprint.tools.fonts.ColorSettings
import com.kimo.reverprint.tools.fonts.Font
import com.kimo.reverprint.tools.graphics.Color
import com.kimo.reverprint.tools.graphics.Monochrome
import com.kimo.reverprint.interactors.bitmaps.generateImageWithText
import com.kimo.reverprint.interactors.bitmaps.BitmapTextConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val controller: DeviceManager,
    private val font: Font
) : ViewModel() {

    private val scope = viewModelScope

    val loadingPreview get() = _loadingPreview.asStateFlow()
    private val _loadingPreview =
        MutableStateFlow(false)

    val imagePreview get() = _imagePreview.asStateFlow()
    private val _imagePreview =
        MutableStateFlow<DeviceManager.PrintPreviews?>(null)

    val device: StateFlow<ThermalPrinter?>
        get() = controller.connectedTo.stateIn(scope, SharingStarted.WhileSubscribed(5000), null)


    private var currentPreviewJob: Job? = null
    private fun runPreviewJob(
        block: suspend CoroutineScope.() -> Unit
    ) = scope.launch {
        _loadingPreview.value = true
        currentPreviewJob?.cancelAndJoin()
        currentPreviewJob = launch {
            block()
            _loadingPreview.value = false
        }
    }

    fun findAndConnect() = scope.launch {
        loop@ while (true) {
            runCatching {
                val device = controller.findAvailable().first()
                controller.connect(device)
                break@loop
            }
        }
    }

    fun setPreview(
        text: String,
        prefs: UserPrintPreferences
    ): Job = runPreviewJob {

        val img = generateImageWithText(
            text, BitmapTextConfig(
                width = 400,
                letterHeight = 50,
                lineSpacing = 6,
                letterSpacing = 0,
                colors = ColorSettings(
                    model = Monochrome,
                    foreground = Color(0x0),
                    background = Color(0xf)
                ),
                font = font
            )
        )

        _imagePreview.update {
            controller.generatePreviews(
                img, DeviceManager.PrintConfig(
                    addSpaceAfterPrint = prefs.addSpaceAfterPrint,
                    ditherImage = prefs.dither
                )
            )
        }
    }

    fun setPreview(
        image: Bitmap,
        prefs: UserPrintPreferences
    ) = runPreviewJob {
        _imagePreview.update {
            controller.generatePreviews(
                image.toImagePixels(),
                DeviceManager.PrintConfig(
                    addSpaceAfterPrint = prefs.addSpaceAfterPrint,
                    ditherImage = prefs.dither
                )
            )
        }
    }

    fun print(mode: PrintMode) = scope.launch {
        _imagePreview.value?.let {
            controller.print(it, mode)
        } ?: println("preview is not provided")
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }

}