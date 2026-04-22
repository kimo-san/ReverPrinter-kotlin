package com.kimo.reverprint.android.presentation

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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kimo.reverprint.android.presentation.entity.ImportedData
import com.kimo.reverprint.android.presentation.entity.UserImagePreferences
import com.kimo.reverprint.android.presentation.entity.UserPrintPreferences
import com.kimo.reverprint.android.toAndroidBitmap
import com.kimo.reverprint.domain.printer.PrintMode
import com.kimo.reverprint.domain.printer.ThermalPrinter
import org.koin.androidx.compose.koinViewModel

enum class InputMode { IMAGE, TEXT }

private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
} else {
    listOf(Manifest.permission.BLUETOOTH)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainComposable(importedData: ImportedData?) {

    val viewModel: MainViewModel = koinViewModel()

    DeviceHandler(viewModel)

    val device by viewModel.device.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device?.name ?: "Not connected yet") }
            )
        }
    ) { pdd ->
        if (device != null) Column(
            Modifier.padding(pdd)
        ) {

            var currentImagePrefs by remember { mutableStateOf(UserImagePreferences.default) }
            var currentPrintPrefs by remember { mutableStateOf(UserPrintPreferences.default) }
            var showingMode by remember { mutableStateOf(PrintMode.BPP1) }
            var currentText by remember { mutableStateOf(importedData?.text) }
            var currentImage by remember { mutableStateOf(importedData?.image) }

            LaunchedEffect(currentImage, currentText, currentImagePrefs, currentPrintPrefs) {

                val image = currentImage
                val text = currentText

                if (text != null) {
                    println("Preview for $text")
                    viewModel.setPreview(
                        text = text,
                        imagePrefs = currentImagePrefs,
                        printPrefs = currentPrintPrefs
                    )
                } else if (image != null) {
                    println("Preview for img")
                    viewModel.setPreview(
                        image = image,
                        imagePrefs = currentImagePrefs,
                        printPrefs = currentPrintPrefs
                    )
                }
            }

            PrinterScreen(
                preview = viewModel.imagePreview.collectAsState().value
                    ?.get(showingMode)
                    ?.toAndroidBitmap()
                    ?.asImageBitmap(),
                loadingPreview = viewModel.loadingPreview.collectAsState().value,
                device = device!!,
                onPickImage = pickImageByUser {
                    currentImage = it
                    currentText = null
                },
                onSetText = {
                    currentText = it
                    currentImage = null
                },
                onPrint = { viewModel.print(showingMode) },
                applyPreferences = { imgPrefs, printPrefs ->

                    when (imgPrefs) {
                        currentImagePrefs -> {
                            Unit
                        }
                        currentImagePrefs.copy(mode = imgPrefs.mode) -> {
                            showingMode = imgPrefs.mode
                        }
                        else -> {
                            currentImagePrefs = imgPrefs
                        }
                    }

                    currentPrintPrefs = printPrefs
                }
            )
        }
    }
}


@Composable
fun PrinterScreen(
    device: ThermalPrinter,
    loadingPreview: Boolean,
    preview: ImageBitmap?,
    onPickImage: () -> Unit,
    onSetText: (String) -> Unit,
    applyPreferences: (UserImagePreferences, UserPrintPreferences) -> Unit,
    onPrint: () -> Unit
) {
    var inputMode by remember { mutableStateOf(InputMode.IMAGE) }
    var imageMode by remember { mutableStateOf(PrintMode.BPP1) }
    var addSpace by remember { mutableStateOf(true) }
    var dither by remember { mutableStateOf(true) }
    var density by remember { mutableFloatStateOf(4f) }
    var fontSize by remember { mutableFloatStateOf(0.2f) }

    LaunchedEffect(imageMode, density, addSpace, dither, fontSize) {
        applyPreferences(
            UserImagePreferences(
                dither = dither,
                mode = imageMode,
                fontSize = (fontSize * device.capabilities.printWidth)
                    .toInt().coerceIn(1..device.capabilities.printWidth),
            ),
            UserPrintPreferences(
                paperDensity = density.toInt(),
                addSpaceAfterPrint = addSpace,
            )
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // ---- INPUT MODE ----
        Text("Source")

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = inputMode == InputMode.IMAGE,
                onClick = { inputMode = InputMode.IMAGE }
            )
            Text("Image")

            Spacer(Modifier.width(16.dp))

            RadioButton(
                selected = inputMode == InputMode.TEXT,
                onClick = { inputMode = InputMode.TEXT }
            )
            Text("Text")
        }

        Spacer(Modifier.height(16.dp))

        // ---- PRINT MODE ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Print mode")
            Spacer(Modifier.weight(1f))
            device.capabilities.supportedModes.forEach {
                RadioButton(
                    selected = imageMode == it,
                    onClick = { imageMode = it }
                )

                Text(it.name)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- ADD SPACE AFTER PRINT ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Feed some paper after print")
            Switch(addSpace, { addSpace = it })
        }

        Spacer(Modifier.height(16.dp))

        // ---- DITHER ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dither image: ")
            Switch(dither, { dither = it })
        }

        Spacer(Modifier.height(16.dp))

        // ---- DENSITY ----
        Text("Paper density: ${density.toInt()}")

        Slider(
            value = density,
            onValueChange = { density = it },
            valueRange = 1f..7f,
            steps = 5
        )

        Spacer(Modifier.height(16.dp))

        // ---- CHOOSE CONTENT ----
        ContentInput(
            selectedMode = inputMode,
            pickImage = { onPickImage() },
            setText = { onSetText(it) },
            fontSize = fontSize,
            setFontSize = { fontSize = it },
        )

        Spacer(Modifier.height(16.dp))

        // ---- PREVIEW ----
        Text("Preview")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Gray)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (loadingPreview) {
                CircularProgressIndicator()
            } else
                if (preview != null)
                    Image(
                        bitmap = preview,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillWidth
                    ) else
                    Text("The preview is not available yet")
        }

        Spacer(Modifier.weight(1f))

        // ---- PRINT ----
        Button(
            onClick = onPrint,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) { Text("PRINT") }
    }
}

@Composable
fun ContentInput(
    selectedMode: InputMode,
    pickImage: () -> Unit,
    setText: (String) -> Unit,
    fontSize: Float,
    setFontSize: (Float) -> Unit
) = when (selectedMode) {
    InputMode.IMAGE -> {
        Button(
            onClick = pickImage,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Pick image") }
    }

    InputMode.TEXT -> {
        var text by remember { mutableStateOf("") }
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                setText(text)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter the text...") }
        )

        Text("Font size")
        Slider(
            value = fontSize,
            onValueChange = { setFontSize(it) },
        )
    }
}

@Composable
@OptIn(ExperimentalPermissionsApi::class)
private fun DeviceHandler(viewModel: MainViewModel) {

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
                CircularProgressIndicator(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(0.3f)
                        .aspectRatio(1f)
                )
                Text(
                    "Searching your printer...",
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
}

@Composable
private fun pickImageByUser(
    applyToImage: (Bitmap) -> Unit
): () -> Unit {

    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(ctx.contentResolver, uri)
            applyToImage(ImageDecoder.decodeBitmap(src))
        } else {
            val inputStream = ctx.contentResolver.openInputStream(uri)
            applyToImage(BitmapFactory.decodeStream(inputStream))
            inputStream?.close()
        }
    }

    return {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}