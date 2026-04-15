package com.kimo.reverprint.tools.bluetooth

import java.util.UUID

data class BluetoothLeCharacteristic(
    val uuid: UUID
) { constructor(uuid: String): this(UUID.fromString(uuid)) }
