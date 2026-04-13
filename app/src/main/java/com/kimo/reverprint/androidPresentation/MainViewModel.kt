package com.kimo.reverprint.androidPresentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimo.reverprint.androidData.toImagePixels
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.domain.DeviceController
import com.kimo.reverprint.domain.ThermalPrinter
import com.kimo.reverprint.interactors.bitmapPlayground.BitmapTextConfig
import com.kimo.reverprint.interactors.bitmapPlayground.Font
import com.kimo.reverprint.interactors.bitmapPlayground.generateImageWithText
import com.kimo.reverprint.interactors.bitmapPlayground.Monochrome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class UserPrintPreferences(
    val dither: Boolean,
    val mode: PrintMode,
    val paperDensity: Int,
    val addSpaceAfterPrint: Boolean,
) {
    companion object {
        val default = UserPrintPreferences(
            dither = true,
            mode = PrintMode.BPP1,
            paperDensity = 4,
            addSpaceAfterPrint = true
        )
    }
}

class MainViewModel(
    private val controller: DeviceController,
    private val font: Font
) : ViewModel() {

    private val scope = CoroutineScope(
        viewModelScope.coroutineContext.minusKey(Job) + SupervisorJob()
    )
    var imagePreview = MutableStateFlow<DeviceController.PrintPreviews?>(null)
    val device: StateFlow<ThermalPrinter?>
        get() = controller.connectedTo.stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

    fun findAndConnect() = scope.launch {
        loop@ while (true) {
            runCatching {
                val device = controller.findAvailable().first()
                controller.connect(device)
                break@loop
            }
        }
    }

    var previewJob: Job? = null
    fun setPreview(
        text: String,
        prefs: UserPrintPreferences
    ): Job = scope.launch {
        previewJob?.cancelAndJoin()
        previewJob = launch {

            val img = generateImageWithText(
                text, BitmapTextConfig(
                    width = 400,
                    letterHeight = 50,
                    lineSpacing = 6,
                    letterSpacing = 0,
                    colors = BitmapTextConfig.Colors(
                        model = Monochrome,
                        foreground = Monochrome.black,
                        background = Monochrome.white
                    ),
                    font = font,
                    dither = prefs.dither
                )
            )

            imagePreview.update {
                controller.generatePreviews(
                    img, DeviceController.PrintConfig(
                        addSpaceAfterPrint = prefs.addSpaceAfterPrint,
                        ditherImage = prefs.dither
                    )
                )
            }
        }
    }

    fun setPreview(
        image: Bitmap,
        prefs: UserPrintPreferences
    ) = scope.launch {
        previewJob?.cancelAndJoin()
        previewJob = launch {
            imagePreview.update {
                controller.generatePreviews(
                    image.toImagePixels(),
                    DeviceController.PrintConfig(
                        addSpaceAfterPrint = true,
                        ditherImage = prefs.dither
                    )
                )
            }
        }
    }

    fun print(mode: PrintMode) = scope.launch {
        imagePreview.value?.let {
            controller.print(it, mode)
        } ?: println("preview is not provided")
    }

}