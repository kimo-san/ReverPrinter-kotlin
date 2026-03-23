package com.kimo.reverprint.data.bluetooth

import java.util.UUID

data class BleCharacteristic(
    val uuid: UUID
) {
    constructor(uuid: String): this(UUID.fromString(uuid))
}
