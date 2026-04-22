package com.kimo.reverprint.android.presentation.entity

data class UserPrintPreferences(
    val paperDensity: Int,
    val addSpaceAfterPrint: Boolean,
) {
    companion object {
        val default = UserPrintPreferences(
            paperDensity = 4,
            addSpaceAfterPrint = true
        )
    }
}