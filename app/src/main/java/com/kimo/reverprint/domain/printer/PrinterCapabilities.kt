package com.kimo.reverprint.domain.printer

data class PrinterCapabilities(
    val printWidth: Int,
    val supportedModes: List<PrintMode>
)