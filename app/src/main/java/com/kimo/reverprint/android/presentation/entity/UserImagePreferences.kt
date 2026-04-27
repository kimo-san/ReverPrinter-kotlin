package com.kimo.reverprint.android.presentation.entity

import com.kimo.reverprint.domain.printer.PrintMode

data class UserImagePreferences(
    val dither: Boolean,
    val mode: PrintMode,
    val fontSize: Int
)

{
    companion object {
        val default = UserImagePreferences(
            dither = true,
            mode = PrintMode.BPP1,
            fontSize = 50
        )
    }
}