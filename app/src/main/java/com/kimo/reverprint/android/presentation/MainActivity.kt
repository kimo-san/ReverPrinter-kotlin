package com.kimo.reverprint.android.presentation

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kimo.reverprint.android.presentation.entity.ImportedData
import com.kimo.reverprint.android.presentation.theme.ReverPrintTheme

class MainActivity : ComponentActivity() {

    private var data by mutableStateOf<ImportedData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        data = handleIntent(intent)
        setContent {
            ReverPrintTheme {
                MainComposable(data)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        data = handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?): ImportedData? {
        val type = intent?.type ?: return null

        return when {

            type == "text/plain" -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                ImportedData(text = text)
            }

            type.startsWith("image/") -> {

                val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    ?: intent.data
                    ?: return null

                val image = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(applicationContext.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src)
                } else {
                    val inputStream = applicationContext.contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream).also {
                        inputStream?.close()
                    }
                }

                ImportedData(image = image)
            }

            else -> null
        }
    }
}