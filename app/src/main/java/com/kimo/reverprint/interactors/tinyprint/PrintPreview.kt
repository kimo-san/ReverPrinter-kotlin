package com.kimo.reverprint.interactors.tinyprint

import com.kimo.reverprint.domain.DeviceController
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.domain.PrintMode

class PrintPreview(
    availableModes: Map<PrintMode, ImagePixels>,
    val imagePixels: Map<PrintMode, ImagePixels>,
    printConfig: DeviceController.PrintConfig,
) : DeviceController.PrintPreviews(availableModes, printConfig)