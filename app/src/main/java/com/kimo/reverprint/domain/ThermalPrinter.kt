package com.kimo.reverprint.domain


data class ThermalPrinter(
    val name: String,
    val macAddress: String,
    val supportedModes: List<PrintMode>,
)