package com.kimo.reverprint.presentation

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toFile
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.ui.theme.ReverPrintTheme
import org.koin.androidx.compose.koinViewModel

private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
} else {
    listOf(Manifest.permission.BLUETOOTH)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReverPrintTheme {
                Greeting()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Greeting(
    viewModel: MainViewModel = koinViewModel()
) {

    val permissionLauncher = rememberMultiplePermissionsState(bluetoothPermissions)

    Scaffold(
        topBar = {
            val printer by viewModel.currentPrinter.collectAsState()
            TopAppBar(
                title = { Text(printer?.name ?: "Not connected yet") }
            )
        }
    ) { it -> it

        Column(
            Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {

            Button(onClick = {
                if (permissionLauncher.allPermissionsGranted) viewModel.findAndConnect()
                else permissionLauncher.launchMultiplePermissionRequest()
            }) {
                Text("Request perms or connect to printer")
            }

            Text(
                "Previews",
                style = MaterialTheme.typography.headlineMedium
            )


            var originalImage by remember { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(originalImage) {
                originalImage?.let { image -> viewModel.setPreview(image) }
            }

            val ctx = LocalContext.current
            val imagePicker = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                uri ?: return@rememberLauncherForActivityResult

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(ctx.contentResolver, uri)
                    originalImage = ImageDecoder.decodeBitmap(src)
                } else {
                    val inputStream = ctx.contentResolver.openInputStream(uri)
                    originalImage = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                }
            }
            Button(
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Text("Pick image")
            }

            val previews by viewModel.imagePreview.collectAsState()
            previews?.let {
                Text("1bpp:")
                it[PrintMode.BPP1]?.let {
                    Image(it.asImageBitmap(), null)
                }
            }
        }

    }

}