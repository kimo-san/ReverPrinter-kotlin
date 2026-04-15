package com.kimo.reverprint.domain

data class PrinterCapabilities(
    val printWidth: Int,
    val supportedModes: List<PrintMode>
)