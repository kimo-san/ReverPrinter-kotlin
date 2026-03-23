package com.kimo.reverprint

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kimo.reverprint.data.tinyprint.DeviceCommunicationProtocol
import com.kimo.reverprint.data.tinyprint.TinyprintController
import com.kimo.reverprint.di.initializeTinyprintController
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutomatedTests {

    lateinit var context: Context
    lateinit var controller: TinyprintController

    @Before
    fun setupVars() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        controller = initializeTinyprintController(context)
    }


    @After
    fun close() = runBlocking {
        controller.disconnect()
    }

}