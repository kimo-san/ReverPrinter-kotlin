package com.kimo.reverprint.interactors.tinyprint

import com.kimo.reverprint.interactors.tinyprint.units.TinyprintDevice

/**
 * Интерфейс, чтобы генерализировать свод доступных комманд
 */
interface DeviceCommunicationProtocol {

    fun deviceGetter(newDevice: () -> TinyprintDevice)

    fun setMode(mode: Mode): ByteArray
    fun setEnergy(concentration: Int): ByteArray
    fun setQuality(level: Quality): ByteArray
    fun feedPaper(lines: Int): ByteArray
    fun retractPaper(lines: Int): ByteArray
    fun requestState(): ByteArray
    fun println1bpp(bitmapLine: IntArray): ByteArray
    fun println4bpp(bitmapLine: IntArray, compress: Boolean): ByteArray
    enum class Mode { GREY_IMG, MONO_IMG }
    enum class Quality { One, Two, Three, Four, Five }

    fun parseReceivedMessage(message: ByteArray): DeviceAnswer?
    sealed interface DeviceAnswer {
        interface State: DeviceAnswer {
            fun warning(): Warning?
            fun batteryLevel(): Percents?
            enum class Warning { NO_PAPER, OPEN_CLAP, OVERHEAT, LOW_BATTERY, MAGIC_TIP, BUSY }
        }
        interface Info: DeviceAnswer
        interface IsFull: DeviceAnswer { val isFull: Boolean }
    }
}

typealias Percents = Float