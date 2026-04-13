package com.kimo.reverprint.androidPresentation

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
import com.kimo.reverprint.androidData.toAndroidBitmap
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.domain.ThermalPrinter
import org.koin.androidx.compose.koinViewModel

enum class InputMode { IMAGE, TEXT }
private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
} else {
    listOf(Manifest.permission.BLUETOOTH)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainComposable() {

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
        Column(Modifier.padding(pdd)) {

            device?.let { device ->

                val previews by viewModel.imagePreview.collectAsState()
                var currentPrefs: UserPrintPreferences? by remember { mutableStateOf(null) }
                var showingMode by remember { mutableStateOf(PrintMode.BPP1) }

                val generateTextPreview = { text: String ->
                    viewModel.setPreview(
                        text = text,
                        prefs = currentPrefs ?: UserPrintPreferences.default
                    )
                }
                val imagePicker = userPicturePicker { image ->
                    viewModel.setPreview(
                        image = image,
                        prefs = currentPrefs ?: UserPrintPreferences.default
                    )
                }

                PrinterScreen(
                    preview = previews?.get(showingMode)
                        ?.toAndroidBitmap()
                        ?.asImageBitmap(),
                    device = device,
                    onPickImage = { imagePicker.invoke() },
                    onSetText = { generateTextPreview(it) },
                    onPrint = { viewModel.print(showingMode) },
                    applyPreferences = { newPrefs ->
                        if (currentPrefs?.copy(mode = newPrefs.mode) == newPrefs)
                            showingMode = newPrefs.mode
                        else {
                            currentPrefs = newPrefs
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun PrinterScreen(
    device: ThermalPrinter,
    preview: ImageBitmap?,
    onPickImage: () -> Unit,
    onSetText: (String) -> Unit,
    applyPreferences: (UserPrintPreferences) -> Unit,
    onPrint: () -> Unit
) {
    var inputMode by remember { mutableStateOf(InputMode.IMAGE) }
    var imageMode by remember { mutableStateOf(PrintMode.BPP1) }
    var addSpace by remember { mutableStateOf(true) }
    var dither by remember { mutableStateOf(true) }
    var density by remember { mutableFloatStateOf(4f) }
    
    LaunchedEffect(imageMode, density, addSpace, dither) {
        applyPreferences(UserPrintPreferences(
            dither = dither,
            mode = imageMode,
            paperDensity = density.toInt(),
            addSpaceAfterPrint = addSpace
        ))
    }
    

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // ---- INPUT MODE ----
        Text("Источник")

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = inputMode == InputMode.IMAGE,
                onClick = { inputMode = InputMode.IMAGE }
            )
            Text("Изображение")

            Spacer(Modifier.width(16.dp))

            RadioButton(
                selected = inputMode == InputMode.TEXT,
                onClick = { inputMode = InputMode.TEXT }
            )
            Text("Текст")
        }

        // ---- PRINT MODE ----
        Text("Режим печати")

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            device.supportedModes.forEach {
                RadioButton(
                    selected = imageMode == it,
                    onClick = { imageMode = it }
                )

                Text(it.name)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- ADD SPACE AFTER PRINT ----
        Text("Вытянуть бумагу для отрыва")

        Switch(addSpace, { addSpace = it })

        Spacer(Modifier.height(16.dp))

        // ---- DITHER ----
        Text("Применить дизеринг")

        Switch(dither, { dither = it })

        Spacer(Modifier.height(16.dp))

        // ---- DENSITY ----
        Text("Плотность бумаги: ${density.toInt()}")

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
            setText = { onSetText(it) }
        )

        Spacer(Modifier.height(16.dp))

        // ---- PREVIEW ----
        Text("Предпросмотр")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("Preview is not available yet")
            }
        }

        Spacer(Modifier.weight(1f))

        // ---- PRINT ----
        Button(
            onClick = onPrint,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) { Text("ПЕЧАТАТЬ") }
    }
}

@Composable
fun ContentInput(
    selectedMode: InputMode,
    pickImage: () -> Unit,
    setText: (String) -> Unit
) = when (selectedMode) {
    InputMode.IMAGE -> {
        Button(
            onClick = pickImage,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Выбрать изображение") }
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
            placeholder = { Text("Введите текст...") }
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
}

@Composable
private fun userPicturePicker(
    applyToImage: (Bitmap) -> Unit
): () -> Unit {

    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri ?: return@rememberLauncherForActivityResult
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