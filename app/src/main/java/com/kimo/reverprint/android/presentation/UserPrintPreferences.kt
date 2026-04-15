package com.kimo.reverprint.android.presentation

import com.kimo.reverprint.domain.PrintMode

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