package com.kimo.reverprint.interactors.tinyprint


interface DeviceChecker {
    suspend fun suspendIfNeeded()
}

