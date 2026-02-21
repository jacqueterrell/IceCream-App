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
import androidx.compose.foundation.layout.requiredHeight
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.PolylineOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.icecreamapp.sweethearts.data.DropoffRequestDisplay
import com.icecreamapp.sweethearts.data.DropoffWithEta
import com.icecreamapp.sweethearts.data.IceCreamMenuItem
import com.icecreamapp.sweethearts.util.formatDistance
import com.icecreamapp.sweethearts.ui.theme.IceCreamAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val currentLocation by viewModel.currentLocation.collectAsState()
    val optimizedDropoffsWithEta by viewModel.optimizedDropoffsWithEta.collectAsState()
    val routePolyline by viewModel.routePolyline.collectAsState()
    val routeLoading by viewModel.routeLoading.collectAsState()
    val routeError by viewModel.routeError.collectAsState()

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
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .twoFingerLongPress(durationMs = 2000L) { showPasscodeDialog = true },
        ) {
            when {
                showListScreen -> {
                    val dropoffLoadError by viewModel.dropoffLoadError.collectAsState()
                    val hiddenIds by viewModel.hiddenFromAdminList.collectAsState()
                    val pendingOnly = dropoffDisplays.filter { display ->
                        display.request.id !in hiddenIds &&
                            display.request.status != "Approved" &&
                            display.request.status != "Canceled"
                    }
                    DropoffListScreen(
                        dropoffDisplays = pendingOnly,
                        dropoffLoadError = dropoffLoadError,
                        viewModel = viewModel,
                        onBack = {
                            viewModel.clearHiddenFromAdminList()
                            showListScreen = false
                        },
                    )
                }
                loading && menu.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .twoFingerLongPress(durationMs = 2000L) { showPasscodeDialog = true },
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
                            }
                        }
                        item(key = "route_map") {
                            val displayList = if (optimizedDropoffsWithEta.isNotEmpty()) {
                                optimizedDropoffsWithEta
                            } else {
                                dropoffDisplays.map { d ->
                                    DropoffWithEta(display = d, etaSecondsFromNow = -1L)
                                }
                            }
                            RouteMapAndListSection(
                                currentLocation = currentLocation,
                                dropoffDisplays = dropoffDisplays,
                                displayList = displayList,
                                routePolyline = routePolyline,
                                routeLoading = routeLoading,
                                routeError = routeError,
                            )
                        }
                        items(
                            if (optimizedDropoffsWithEta.isNotEmpty()) optimizedDropoffsWithEta
                            else dropoffDisplays.map { d -> DropoffWithEta(display = d, etaSecondsFromNow = -1L) },
                            key = { it.display.request.id }
                        ) { dropoffWithEta ->
                            DropoffEtaRow(dropoffWithEta = dropoffWithEta)
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

/** Holder for map and polyline so we can update polyline when route changes. */
private class MapHolder {
    var googleMap: com.google.android.gms.maps.GoogleMap? = null
    var polyline: com.google.android.gms.maps.model.Polyline? = null
}

@Composable
private fun RouteMapWithNativePolyline(
    center: LatLng,
    startLatLng: LatLng?,
    dropoffMarkers: List<Pair<LatLng, String>>,
    routePolyline: List<Pair<Double, Double>>,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context).apply { onCreate(null) }
    }
    val holder = remember { MapHolder() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(routePolyline) {
        holder.googleMap?.let { map ->
            holder.polyline?.remove()
            holder.polyline = null
            if (routePolyline.isNotEmpty()) {
                val points = routePolyline.map { LatLng(it.first, it.second) }
                holder.polyline = map.addPolyline(
                    PolylineOptions()
                        .addAll(points)
                        .color(android.graphics.Color.parseColor("#0D47A1"))
                        .width(20f),
                )
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            update = { view ->
                if (holder.googleMap == null) {
                    view.getMapAsync { map ->
                        holder.googleMap = map
                        map.uiSettings.apply {
                            isZoomControlsEnabled = true
                        }
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 12f))
                        startLatLng?.let { map.addMarker(com.google.android.gms.maps.model.MarkerOptions().position(it).title("Start")) }
                        dropoffMarkers.forEachIndexed { _, (latLng, title) ->
                            map.addMarker(com.google.android.gms.maps.model.MarkerOptions().position(latLng).title(title))
                        }
                        if (routePolyline.isNotEmpty()) {
                            val points = routePolyline.map { LatLng(it.first, it.second) }
                            holder.polyline = map.addPolyline(
                                PolylineOptions()
                                    .addAll(points)
                                    .color(android.graphics.Color.parseColor("#0D47A1"))
                                    .width(20f),
                            )
                        }
                    }
                }
            },
        )
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun RouteMapAndListSection(
    currentLocation: Pair<Double, Double>?,
    dropoffDisplays: List<DropoffRequestDisplay>,
    displayList: List<DropoffWithEta>,
    routePolyline: List<Pair<Double, Double>>,
    routeLoading: Boolean,
    routeError: String?,
) {
    val showMap = currentLocation != null || dropoffDisplays.isNotEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showMap) {
            val center = when {
                currentLocation != null -> LatLng(currentLocation.first, currentLocation.second)
                dropoffDisplays.isNotEmpty() -> {
                    val first = dropoffDisplays.first().request
                    LatLng(first.latitude, first.longitude)
                }
                else -> LatLng(37.5, -122.0)
            }
            val startLatLng = currentLocation?.let { LatLng(it.first, it.second) }
            val dropoffMarkers = dropoffDisplays.mapIndexed { index, d ->
                val r = d.request
                LatLng(r.latitude, r.longitude) to "${index + 1}. ${r.name}"
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(240.dp),
            ) {
                RouteMapWithNativePolyline(
                    center = center,
                    startLatLng = startLatLng,
                    dropoffMarkers = dropoffMarkers,
                    routePolyline = routePolyline,
                    loading = routeLoading,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (routeError != null) {
                Text(
                    text = routeError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (displayList.isNotEmpty()) {
                Text(
                    text = if (displayList.any { it.etaSecondsFromNow >= 0 })
                        "Route order & ETAs (5 min at each stop)"
                    else
                        "Dropoff locations (ETA when route available)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DropoffEtaRow(
    dropoffWithEta: DropoffWithEta,
    modifier: Modifier = Modifier,
) {
    val etaText = if (dropoffWithEta.etaSecondsFromNow >= 0) {
        val etaTime = System.currentTimeMillis() + dropoffWithEta.etaSecondsFromNow * 1000L
        "ETA ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(etaTime))}"
    } else {
        "ETA —"
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = dropoffWithEta.display.request.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = dropoffWithEta.display.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = etaText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AdminDropoffMap(
    dropoffDisplays: List<DropoffRequestDisplay>,
    routePolyline: List<Pair<Double, Double>>,
    adminDropoffsWithEta: List<DropoffWithEta>,
    currentLocation: Pair<Double, Double>?,
    routeLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    if (dropoffDisplays.isEmpty()) return
    val center = LatLng(
        dropoffDisplays.first().request.latitude,
        dropoffDisplays.first().request.longitude,
    )
    val startLatLng = currentLocation?.let { LatLng(it.first, it.second) }
    val dropoffMarkers = dropoffDisplays.mapIndexed { index, d ->
        val r = d.request
        LatLng(r.latitude, r.longitude) to "${index + 1}. ${r.name}"
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(220.dp)
    ) {
        RouteMapWithNativePolyline(
            center = center,
            startLatLng = startLatLng,
            dropoffMarkers = dropoffMarkers,
            routePolyline = routePolyline,
            loading = routeLoading,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropoffListScreen(
    dropoffDisplays: List<DropoffRequestDisplay>,
    dropoffLoadError: String?,
    viewModel: IceCreamViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val adminRoutePolyline by viewModel.adminRoutePolyline.collectAsState()
    val adminDropoffsWithEta by viewModel.adminDropoffsWithEta.collectAsState()
    val adminRouteLoading by viewModel.adminRouteLoading.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    LaunchedEffect(dropoffDisplays, currentLocation) {
        viewModel.loadAdminRoute(dropoffDisplays)
    }

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
            if (dropoffDisplays.isNotEmpty()) {
                item(key = "admin_map") {
                    AdminDropoffMap(
                        dropoffDisplays = dropoffDisplays,
                        routePolyline = adminRoutePolyline,
                        adminDropoffsWithEta = adminDropoffsWithEta,
                        currentLocation = currentLocation,
                        routeLoading = adminRouteLoading,
                    )
                }
            }
            if (adminDropoffsWithEta.isNotEmpty()) {
                item(key = "route_etas_label") {
                    Text(
                        text = if (adminDropoffsWithEta.any { it.etaSecondsFromNow >= 0 })
                            "Route order & ETAs (5 min at each stop)"
                        else
                            "Dropoff locations",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (dropoffLoadError != null) {
                item(key = "load_error") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = dropoffLoadError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
            if (adminDropoffsWithEta.isNotEmpty()) {
                items(adminDropoffsWithEta, key = { it.display.request.id }) { item ->
                    val display = item.display
                    val etaSeconds = if (item.etaSecondsFromNow >= 0) item.etaSecondsFromNow else null
                    DropoffRequestRow(
                        display = display,
                        etaSecondsFromNow = etaSeconds,
                        showEtaPlaceholder = true,
                        onApprove = { viewModel.updateDropoffStatus(display.request.id, "Approved") },
                        onCancel = { viewModel.updateDropoffStatus(display.request.id, "Canceled") },
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
            } else {
                items(dropoffDisplays, key = { it.request.id }) { display ->
                    DropoffRequestRow(
                        display = display,
                        etaSecondsFromNow = null,
                        showEtaPlaceholder = true,
                        onApprove = { viewModel.updateDropoffStatus(display.request.id, "Approved") },
                        onCancel = { viewModel.updateDropoffStatus(display.request.id, "Canceled") },
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
}

@Composable
private fun DropoffRequestRow(
    display: DropoffRequestDisplay,
    etaSecondsFromNow: Long? = null,
    showEtaPlaceholder: Boolean = false,
    onApprove: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    onSms: () -> Unit,
    onPhone: () -> Unit,
    onMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etaText = when {
        etaSecondsFromNow != null && etaSecondsFromNow >= 0 -> {
            val etaTime = System.currentTimeMillis() + etaSecondsFromNow * 1000L
            "ETA ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(etaTime))}"
        }
        showEtaPlaceholder -> "ETA —"
        else -> null
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cardWidth = maxWidth
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .width(cardWidth + 96.dp)
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
                    if (etaText != null) {
                        Text(
                            text = etaText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = onSms, modifier = Modifier.weight(1f)) { Text("SMS") }
                        Button(onClick = onPhone, modifier = Modifier.weight(1f)) { Text("Phone") }
                        Button(onClick = onMap, modifier = Modifier.weight(1f)) { Text("Map") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text("Approve") }
                        Button(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    }
                }
            }
            Button(
                onClick = onDone,
                modifier = Modifier
                    .width(96.dp)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Text("Done", maxLines = 1)
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
