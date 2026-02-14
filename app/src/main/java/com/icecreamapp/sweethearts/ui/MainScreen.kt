package com.icecreamapp.sweethearts.ui

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.icecreamapp.sweethearts.data.DropoffRequestDisplay
import com.icecreamapp.sweethearts.data.IceCreamMenuItem
import com.icecreamapp.sweethearts.util.formatDistance
import com.icecreamapp.sweethearts.ui.theme.IceCreamAppTheme

@Composable
fun MainScreen(
    viewModel: IceCreamViewModel,
    modifier: Modifier = Modifier,
) {
    val menu = viewModel.menu.collectAsState().value
    val loading = viewModel.loading.collectAsState().value
    val message = viewModel.message.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }
    var name by remember { mutableStateOf("") }
    var phoneDigits by remember { mutableStateOf("") }
    var showTryAgainDialog by remember { mutableStateOf(false) }
    var tryAgainMessage by remember { mutableStateOf("Please try again.") }
    var showListScreen by remember { mutableStateOf(false) }
    var showPasscodeDialog by remember { mutableStateOf(false) }
    var passcodeInput by remember { mutableStateOf("") }
    var passcodeError by remember { mutableStateOf("") }
    val context = LocalContext.current
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            locationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val phoneFormatted = if (phoneDigits.length == 10)
                        "${phoneDigits.take(3)}-${phoneDigits.drop(3).take(3)}-${phoneDigits.drop(6)}"
                    else phoneDigits
                    viewModel.requestDropoff(name, phoneFormatted, location.latitude, location.longitude)
                } else {
                    tryAgainMessage = "Location unavailable. Turn on device location and try again."
                    showTryAgainDialog = true
                }
            }
        } else {
            tryAgainMessage = "Location permission is needed to submit a dropoff request."
            showTryAgainDialog = true
        }
    }

    val dropoffSuccess by viewModel.dropoffSuccess.collectAsState()
    val dropoffError by viewModel.dropoffError.collectAsState()
    val dropoffErrorMessage by viewModel.dropoffErrorMessage.collectAsState()
    val dropoffLoading by viewModel.dropoffLoading.collectAsState()
    val dropoffDisplays by viewModel.dropoffDisplays.collectAsState()

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { viewModel.updateCurrentLocation(it.latitude, it.longitude) }
            }
        }
    }

    LaunchedEffect(dropoffError) {
        if (dropoffError) {
            tryAgainMessage = dropoffErrorMessage ?: "Please try again."
            showTryAgainDialog = true
            viewModel.clearDropoffError()
        }
    }

    LaunchedEffect(message) {
        message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .twoFingerLongPress(durationMs = 2000L) { showPasscodeDialog = true },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                showListScreen -> {
                    DropoffListScreen(
                        dropoffDisplays = dropoffDisplays,
                        viewModel = viewModel,
                        onBack = { showListScreen = false },
                    )
                }
                loading && menu.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                )
                                OutlinedTextField(
                                    value = phoneDigits,
                                    onValueChange = { new ->
                                        phoneDigits = new.filter { it.isDigit() }.take(10)
                                    },
                                    label = { Text("Phone Number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    visualTransformation = PhoneNumberVisualTransformation(),
                                )
                                Button(
                                    onClick = {
                                        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            locationClient.lastLocation.addOnSuccessListener { location ->
                                                if (location != null) {
                                                    val phoneFormatted = if (phoneDigits.length == 10)
                                                        "${phoneDigits.take(3)}-${phoneDigits.drop(3).take(3)}-${phoneDigits.drop(6)}"
                                                    else phoneDigits
                                                    viewModel.requestDropoff(name, phoneFormatted, location.latitude, location.longitude)
                                                } else {
                                                    tryAgainMessage = "Location unavailable. Turn on device location and try again."
                                                    showTryAgainDialog = true
                                                }
                                            }
                                        } else {
                                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !dropoffLoading,
                                ) {
                                    if (dropoffLoading) {
                                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                                    } else {
                                        Text("Request Ice Cream Dropoff")
                                    }
                                }
                                TextButton(
                                    onClick = { showPasscodeDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        "Staff: view dropoff requests",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (loading && menu.isNotEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            }
        }
    }

    if (dropoffSuccess) {
        AlertDialog(
            onDismissRequest = viewModel::clearDropoffSuccess,
            title = { Text("Success") },
            text = { Text("Your ice cream dropoff request has been submitted.") },
            confirmButton = {
                Button(onClick = viewModel::clearDropoffSuccess) { Text("OK") }
            },
        )
    }
    if (showTryAgainDialog) {
        AlertDialog(
            onDismissRequest = { showTryAgainDialog = false },
            title = { Text("Request failed") },
            text = { Text(tryAgainMessage) },
            confirmButton = {
                Button(onClick = { showTryAgainDialog = false }) { Text("OK") }
            },
        )
    }
    if (showPasscodeDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasscodeDialog = false
                passcodeInput = ""
                passcodeError = ""
            },
            title = { Text("Enter passcode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = passcodeInput,
                        onValueChange = {
                            passcodeInput = it
                            passcodeError = ""
                        },
                        label = { Text("Passcode") },
                        singleLine = true,
                        isError = passcodeError.isNotEmpty(),
                        supportingText = { if (passcodeError.isNotEmpty()) Text(passcodeError) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passcodeInput == "11233") {
                            showListScreen = true
                            showPasscodeDialog = false
                            passcodeInput = ""
                            passcodeError = ""
                        } else {
                            passcodeError = "Wrong passcode"
                        }
                    },
                ) {
                    Text("OK")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropoffListScreen(
    dropoffDisplays: List<DropoffRequestDisplay>,
    viewModel: IceCreamViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dropoff Requests") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(dropoffDisplays, key = { it.request.id }) { display ->
                DropoffRequestRow(
                    display = display,
                    onDone = { viewModel.markDropoffDone(display.request.id) },
                    onSms = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${display.request.phoneNumber.replace("-", "")}"))
                        context.startActivity(intent)
                    },
                    onPhone = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${display.request.phoneNumber.replace("-", "")}"))
                        context.startActivity(intent)
                    },
                    onMap = {
                        val dest = "${display.request.latitude},${display.request.longitude}"
                        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$dest")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    },
                )
            }
        }
    }
}

@Composable
private fun DropoffRequestRow(
    display: DropoffRequestDisplay,
    onDone: () -> Unit,
    onSms: () -> Unit,
    onPhone: () -> Unit,
    onMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cardWidth = maxWidth
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .width(cardWidth + 80.dp)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Card(
                modifier = Modifier.width(cardWidth),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = display.request.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = display.request.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                    Text(text = display.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Distance: ${formatDistance(display.distanceMeters)}", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = onSms, modifier = Modifier.weight(1f)) { Text("SMS") }
                        Button(onClick = onPhone, modifier = Modifier.weight(1f)) { Text("Phone") }
                        Button(onClick = onMap, modifier = Modifier.weight(1f)) { Text("Map") }
                    }
                }
            }
            Button(
                onClick = onDone,
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun FlavorCard(
    item: IceCreamMenuItem,
    onRequest: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onRequest,
                enabled = enabled,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Request")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FlavorCardPreview() {
    IceCreamAppTheme {
        FlavorCard(
            item = IceCreamMenuItem(
                id = "vanilla",
                name = "Vanilla",
                description = "Classic smooth vanilla",
            ),
            onRequest = {},
            enabled = true,
        )
    }
}
