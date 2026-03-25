package com.kimo.reverprint.useCases.tinyprint

import com.kimo.reverprint.domain.DeviceController
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.domain.PrintMode

class PrintPreview(
    availableModes: Map<PrintMode, ImagePixels>,
    val imagePixels: Map<PrintMode, ImagePixels>,
    configuration: DeviceController.Configuration,
) : DeviceController.PrintPreviews(availableModes, configuration)