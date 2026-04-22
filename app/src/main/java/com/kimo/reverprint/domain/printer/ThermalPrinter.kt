package com.kimo.reverprint.domain.printer

data class ThermalPrinter(
    val name: String,
    val macAddress: String,
    val capabilities: PrinterCapabilities
)