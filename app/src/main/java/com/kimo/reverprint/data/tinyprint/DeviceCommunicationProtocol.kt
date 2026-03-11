package com.kimo.reverprint.data.tinyprint

interface DeviceCommunicationProtocol {
    fun setMode(mode: Mode): ByteArray
    fun setEnergy(value: Int): ByteArray
    fun setQuality(level: Quality): ByteArray
    fun feedPaper(lines: Int): ByteArray
    fun retractPaper(lines: Int): ByteArray
    fun println1bpp(bitmapLine: IntArray): ByteArray
    fun println4bpp(bitmapLine: IntArray, compress: Boolean): ByteArray
    enum class Mode { GREY_IMG, MONO_IMG }
    enum class Quality { One, Two, Three, Four, Five }
}