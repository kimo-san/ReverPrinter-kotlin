package com.kimo.reverprint.useCases.tinyprint


interface DeviceChecker {
    suspend fun suspendIfOverloaded()
}

