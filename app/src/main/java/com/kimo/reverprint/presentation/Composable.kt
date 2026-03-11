package com.kimo.reverprint.presentation

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kimo.reverprint.domain.PrintMode
import org.koin.androidx.compose.koinViewModel

private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
} else {
    listOf(Manifest.permission.BLUETOOTH)
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Greeting(
    viewModel: MainViewModel = koinViewModel()
) {

    val permissionLauncher = rememberMultiplePermissionsState(bluetoothPermissions)
    val device by viewModel.device.collectAsState()

    if (device == null) {

        LaunchedEffect(Unit) {
            if (permissionLauncher.allPermissionsGranted) viewModel.findAndConnect()
            else permissionLauncher.launchMultiplePermissionRequest()
        }

        Dialog({ /* cannot exit until device is connected */ }) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
                    .wrapContentSize(),
                Arrangement.Center
            ) {
                CircularProgressIndicator(Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.3f)
                    .aspectRatio(1f))
                Text(
                    "Connecting to your printer...",
                    Modifier.padding(8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "If it takes too long, ensure it is on and it is in the general list of supported devises.",
                    Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    var selectedPreview by remember { mutableStateOf<PrintMode?>(null) }
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
    }.let { { it.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device?.name ?: "Not connected yet") }
            )
        },
        floatingActionButton = {
            FloatingActionButton({ imagePicker.invoke() }) {
                Text("Pick image", Modifier.padding(8.dp))
            }
        }
    ) { it -> it
        if (device != null) Column(
            Modifier
                .padding(it)
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState()),
            Arrangement.spacedBy(16.dp),
            Alignment.CenterHorizontally
        ) {


            selectedPreview?.let {
                Button({ viewModel.print(it) }) {
                    Text(
                        "Print ${it.name}",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }


            val bakedPreviews by viewModel.imagePreview.collectAsState()
            bakedPreviews?.let { previews ->

                val carouselState = rememberCarouselState {
                    device!!.supportedModes.size
                }

                LaunchedEffect(carouselState.currentItem) {
                    selectedPreview = PrintMode.entries[carouselState.currentItem]
                }

                HorizontalMultiBrowseCarousel(
                    carouselState,
                    preferredItemWidth = LocalWindowInfo.current.containerDpSize.width,
                    modifier = Modifier.fillMaxSize(),
                ) { mode ->
                    Image(
                        previews[PrintMode.entries[mode]]!!.asImageBitmap(),
                        null, Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }
}