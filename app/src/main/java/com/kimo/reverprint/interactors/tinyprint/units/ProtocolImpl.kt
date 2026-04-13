package com.kimo.reverprint.interactors.tinyprint.units

import com.kimo.reverprint.interactors.tinyprint.DeviceCommunicationProtocol
import com.kimo.reverprint.interactors.tinyprint.Percents
import com.lingmoyun.minilzo.MiniLZO
import kotlin.experimental.or

class ProtocolImpl : DeviceCommunicationProtocol {

    lateinit var deviceGetter: () -> TinyprintDevice
    override fun deviceGetter(newDevice: () -> TinyprintDevice) {
        deviceGetter = newDevice
    }

    fun packLength(data: ByteArray): ByteArray {
        require(data.size <= 0xffff)
        return byteArrayOf(
            (data.size and 0xFF).toByte(),
            ((data.size shr 8) and 0xFF).toByte()
        )
    }

    private fun crc8(data: ByteArray): Byte {
        val crc8Table = intArrayOf(0x00, 0x07, 0x0e, 0x09, 0x1c, 0x1b, 0x12, 0x15, 0x38, 0x3f, 0x36, 0x31, 0x24, 0x23, 0x2a, 0x2d, 0x70, 0x77, 0x7e, 0x79, 0x6c, 0x6b, 0x62, 0x65, 0x48, 0x4f, 0x46, 0x41, 0x54, 0x53, 0x5a, 0x5d, 0xe0, 0xe7, 0xee, 0xe9, 0xfc, 0xfb, 0xf2, 0xf5, 0xd8, 0xdf, 0xd6, 0xd1, 0xc4, 0xc3, 0xca, 0xcd, 0x90, 0x97, 0x9e, 0x99, 0x8c, 0x8b, 0x82, 0x85, 0xa8, 0xaf, 0xa6, 0xa1, 0xb4, 0xb3, 0xba, 0xbd, 0xc7, 0xc0, 0xc9, 0xce, 0xdb, 0xdc, 0xd5, 0xd2, 0xff, 0xf8, 0xf1, 0xf6, 0xe3, 0xe4, 0xed, 0xea, 0xb7, 0xb0, 0xb9, 0xbe, 0xab, 0xac, 0xa5, 0xa2, 0x8f, 0x88, 0x81, 0x86, 0x93, 0x94, 0x9d, 0x9a, 0x27, 0x20, 0x29, 0x2e, 0x3b, 0x3c, 0x35, 0x32, 0x1f, 0x18, 0x11, 0x16, 0x03, 0x04, 0x0d, 0x0a, 0x57, 0x50, 0x59, 0x5e, 0x4b, 0x4c, 0x45, 0x42, 0x6f, 0x68, 0x61, 0x66, 0x73, 0x74, 0x7d, 0x7a, 0x89, 0x8e, 0x87, 0x80, 0x95, 0x92, 0x9b, 0x9c, 0xb1, 0xb6, 0xbf, 0xb8, 0xad, 0xaa, 0xa3, 0xa4, 0xf9, 0xfe, 0xf7, 0xf0, 0xe5, 0xe2, 0xeb, 0xec, 0xc1, 0xc6, 0xcf, 0xc8, 0xdd, 0xda, 0xd3, 0xd4, 0x69, 0x6e, 0x67, 0x60, 0x75, 0x72, 0x7b, 0x7c, 0x51, 0x56, 0x5f, 0x58, 0x4d, 0x4a, 0x43, 0x44, 0x19, 0x1e, 0x17, 0x10, 0x05, 0x02, 0x0b, 0x0c, 0x21, 0x26, 0x2f, 0x28, 0x3d, 0x3a, 0x33, 0x34, 0x4e, 0x49, 0x40, 0x47, 0x52, 0x55, 0x5c, 0x5b, 0x76, 0x71, 0x78, 0x7f, 0x6a, 0x6d, 0x64, 0x63, 0x3e, 0x39, 0x30, 0x37, 0x22, 0x25, 0x2c, 0x2b, 0x06, 0x01, 0x08, 0x0f, 0x1a, 0x1d, 0x14, 0x13, 0xae, 0xa9, 0xa0, 0xa7, 0xb2, 0xb5, 0xbc, 0xbb, 0x96, 0x91, 0x98, 0x9f, 0x8a, 0x8d, 0x84, 0x83, 0xde, 0xd9, 0xd0, 0xd7, 0xc2, 0xc5, 0xcc, 0xcb, 0xe6, 0xe1, 0xe8, 0xef, 0xfa, 0xfd, 0xf4, 0xf3)
        var crc = 0
        for (byte in data) {
            val index = (crc xor (byte.toInt() and 0xFF)) and 0xFF
            crc = crc8Table[index]
        }
        return (crc and 0xff).toByte()
    }

    fun formatMessage(cmd: Byte, data: ByteArray): ByteArray = byteArrayOf(
        *startOfPackageSlice,
        cmd,
        0x00.toByte(),
        *packLength(data),
        *data,
        crc8(data),
        *endOfPackageSlice
    )

    override fun parseReceivedMessage(message: ByteArray): DeviceCommunicationProtocol.DeviceAnswer? {

        val messageType = message.withIndex().first {
            runCatching {
                message[it.index-2] == startOfPackageSlice[0]
                        && message[it.index-1] == startOfPackageSlice[1]
            }.getOrNull() == true
        }
        val dataStartIndex = messageType.index + 2 // skip magic number
        val dataSlice = message.sliceArray(dataStartIndex..message.lastIndex)

        return when (messageType.value) {
            0xA3.toByte() -> StateImpl(dataSlice)
            // 01 00 10 70 FF  -> full
            // 01 00 00 00 FF  -> not full
            0xAE.toByte() -> IsFullImpl(dataSlice[2] == 0x10.toByte())
            0xA8.toByte() -> InfoImpl()
            else -> null
        }
    }

    fun compress(data: ByteArray): ByteArray {
        val compressedData = MiniLZO.compress(data)!!
        return byteArrayOf(
            *packLength(data),
            *packLength(compressedData),
            *compressedData
        )
    }

    override fun setMode(mode: DeviceCommunicationProtocol.Mode): ByteArray {
        val modeAsData = when (mode) {
            DeviceCommunicationProtocol.Mode.GREY_IMG -> byteArrayOf(0, 1)
            DeviceCommunicationProtocol.Mode.MONO_IMG -> byteArrayOf(0)
        }
        return formatMessage(0xBE.toByte(), modeAsData)
    }

    override fun setQuality(level: DeviceCommunicationProtocol.Quality): ByteArray {
        return formatMessage(
            0xA4.toByte(),
            byteArrayOf(
                when (level) {
                    DeviceCommunicationProtocol.Quality.One -> 49.toByte()
                    DeviceCommunicationProtocol.Quality.Two -> 50.toByte()
                    DeviceCommunicationProtocol.Quality.Three -> 51.toByte()
                    DeviceCommunicationProtocol.Quality.Four -> 52.toByte()
                    DeviceCommunicationProtocol.Quality.Five -> 53.toByte()
                }
            )
        )
    }

    val DEFAULT_CONCENTRATION = 4
    val ENERGY_STEP = 0.15
    override fun setEnergy(concentration: Int): ByteArray {
        val deviceModeration = deviceGetter().grayModerationEneragy
        val delta = concentration - DEFAULT_CONCENTRATION
        val value = (deviceModeration + delta * ENERGY_STEP * deviceModeration).toInt()
        return formatMessage(
            0xAF.toByte(),
            byteArrayOf(
                (value.shr(4) and 0xff).toByte(),
                (value        and 0xff).toByte()
            )
        )
    }

    override fun requestState(): ByteArray =
        formatMessage(0xA3.toByte(), byteArrayOf(0))

    override fun feedPaper(lines: Int): ByteArray =
        formatMessage(0xA1.toByte(), byteArrayOf(lines.toByte()))

    override fun retractPaper(lines: Int): ByteArray =
        formatMessage(0xA0.toByte(), byteArrayOf(lines.toByte()))

    override fun println1bpp(bitmapLine: IntArray): ByteArray =
        formatMessage(0xA2.toByte(), pack1bpp(bitmapLine))

    override fun println4bpp(bitmapLine: IntArray, compress: Boolean): ByteArray =
        formatMessage(
            cmd = 0xCF.toByte(),
            data = pack4bpp(bitmapLine).let {
                if (compress) compress(it) else it
            }
        )

    private fun pack1bpp(pixels: IntArray): ByteArray {
        require(pixels.all { it == 0 || it == 1 })
        val result = ByteArray(pixels.size / 8)
        var currentByte = 0
        for (i in pixels.indices) {

            if (i % 8 == 0) {
                result[i / 8] = currentByte.toByte()
                currentByte = 0
            }

            val operatingByte = currentByte shr 1
            val insertBit = if (pixels[i] == 0) 0x80 else 0x00
            currentByte = operatingByte or insertBit
        }

        return result
    }

    private fun pack4bpp(pixels: IntArray): ByteArray {
        require(pixels.all { it in 0..15 })
        val result = ByteArray((pixels.size + 1) / 2)

        // Main idea: one byte may contain two pixels. Bytes of those pixel pairs ordered from
        // left to right and those pairs inside are ordered from right to left.
        for (currentByte in result.indices) {

            // get pixel pair and convert it to subtractive color model

            val pixelIndexes = (currentByte * 2) to (currentByte * 2 + 1)

            val thisPixel = -(pixels[pixelIndexes.first] - 15)
            val nextPixel = try {
                -(pixels[pixelIndexes.second] - 15)
            } catch (_: IndexOutOfBoundsException) {
                0x00
            }

            // swap pixel pair inside of the byte and paste it to result array
            val thisAsBits = (thisPixel and 0x0f).toByte()
            val nextAsBits = (nextPixel shl 4).toByte()
            result[currentByte] = thisAsBits or nextAsBits

//            println("INT1: $thisPixel INT2: $nextPixel --- RES: ${result[currentByte].toHexString()} --- BYTE1: ${thisAsBits.toHexString()} BYTE2: ${nextAsBits.toHexString()}")
        }
//        println(result.joinToString(" ") { it.toHexString() })
        return result
    }

    companion object {
        val startOfPackageSlice = byteArrayOf(0x51, 0x78)
        val endOfPackageSlice = byteArrayOf(0xFF.toByte())
    }

    private inner class InfoImpl: DeviceCommunicationProtocol.DeviceAnswer.Info
    private inner class IsFullImpl(override val isFull: Boolean) : DeviceCommunicationProtocol.DeviceAnswer.IsFull
    private inner class StateImpl(val data: ByteArray): DeviceCommunicationProtocol.DeviceAnswer.State {
        override fun warning(): DeviceCommunicationProtocol.DeviceAnswer.State.Warning? = when (data[2].toInt() and 0xff) {
            0x01 -> DeviceCommunicationProtocol.DeviceAnswer.State.Warning.NO_PAPER
            0x02 -> DeviceCommunicationProtocol.DeviceAnswer.State.Warning.OPEN_CLAP
            0x04 -> DeviceCommunicationProtocol.DeviceAnswer.State.Warning.OVERHEAT
            0x08 -> DeviceCommunicationProtocol.DeviceAnswer.State.Warning.LOW_BATTERY
            0x10 -> DeviceCommunicationProtocol.DeviceAnswer.State.Warning.MAGIC_TIP
            0x80 -> DeviceCommunicationProtocol.DeviceAnswer.State.Warning.BUSY
            else -> null
        }
        override fun batteryLevel(): Percents? {
            return when (deviceGetter().showElectricityModel) {
                1 -> {
                    // я от жизни охуел, когда понял, что оно так должно работать. Пиздец
                    val raw = data[4].toHexString().toInt()
                    val level = (raw - 22).coerceIn(0, 6)
                    level.toFloat() / 6f
                }
                2 -> {
                    val level = data[3].toInt() and 0xff
                    level.toFloat() / 0xff
                }
                else -> null
            }
        }
    }
}