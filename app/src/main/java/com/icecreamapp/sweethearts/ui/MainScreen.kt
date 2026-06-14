package com.icecreamapp.sweethearts.ui

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.icecreamapp.sweethearts.AdminConfig
import com.icecreamapp.sweethearts.data.DropoffRequestDisplay
import com.icecreamapp.sweethearts.data.DropoffWithEta
import com.icecreamapp.sweethearts.data.IceCreamMenuItem
import com.icecreamapp.sweethearts.ui.theme.*
import com.icecreamapp.sweethearts.util.formatDistance
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: IceCreamViewModel,
    modifier: Modifier = Modifier,
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userPhone by viewModel.userPhone.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.message.collectAsState()

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = CreamBackground
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    IceCreamScreen.WELCOME -> WelcomeScreen(
                        onGetStarted = { viewModel.setScreen(IceCreamScreen.SAVE_INFO) },
                        onVendorAccess = { viewModel.setScreen(IceCreamScreen.VENDOR_ACCESS) }
                    )
                    IceCreamScreen.SAVE_INFO -> SaveInfoScreen(
                        onBack = { viewModel.setScreen(IceCreamScreen.WELCOME) },
                        onContinue = { name, phone -> viewModel.saveProfile(name, phone) }
                    )
                    IceCreamScreen.CUSTOMER_DASHBOARD -> CustomerDashboard(
                        viewModel = viewModel,
                        name = userName,
                        phone = userPhone
                    )
                    IceCreamScreen.VENDOR_ACCESS -> VendorAccessScreen(
                        onBack = { viewModel.setScreen(IceCreamScreen.WELCOME) },
                        onAccess = { passcode ->
                            if (passcode == AdminConfig.PASSCODE) {
                                viewModel.setAdminSessionPasscode(AdminConfig.PASSCODE)
                                viewModel.markDeviceAsVendorForPush()
                                viewModel.setScreen(IceCreamScreen.VENDOR_DASHBOARD)
                            }
                        }
                    )
                    IceCreamScreen.VENDOR_DASHBOARD -> VendorDashboard(
                        viewModel = viewModel,
                        onBack = {
                            viewModel.setAdminSessionPasscode(null)
                            viewModel.setScreen(IceCreamScreen.WELCOME)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SweetheartsLogo(
    modifier: Modifier = Modifier,
    onHoldComplete: (() -> Unit)? = null
) {
    var isPressing by remember { mutableStateOf(false) }
    
    LaunchedEffect(isPressing) {
        if (isPressing && onHoldComplete != null) {
            delay(3000L)
            onHoldComplete()
            isPressing = false
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .pointerInput(onHoldComplete) {
                detectTapGestures(
                    onPress = {
                        isPressing = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressing = false
                        }
                    }
                )
            }
    ) {
        Text("🍦", style = MaterialTheme.typography.displaySmall)
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(start = 12.dp)) {
            Text(
                "Sweethearts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ChocolateBrown
            )
            Text(
                "ICE CREAM",
                style = MaterialTheme.typography.labelLarge,
                color = ChocolateBrown.copy(alpha = 0.7f),
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onVendorAccess: () -> Unit
) {
    Scaffold(containerColor = CreamBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            SweetheartsLogo(
                onHoldComplete = onVendorAccess
            )

            // Large Truck Illustration Placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🚚🍦",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Welcome to\nSweethearts Ice Cream",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = ChocolateBrown,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Fresh treats delivered by your\nlocal ice cream truck ❤️",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextBrown.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChocolateBrown)
                ) {
                    Text("Let's Get Started", style = MaterialTheme.typography.titleMedium)
                }
                
                TextButton(onClick = onVendorAccess) {
                    Text(
                        "Vendor Access",
                        color = StrawberryPink,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveInfoScreen(
    onBack: () -> Unit,
    onContinue: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Scaffold(
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Back", modifier = Modifier.padding(4.dp)) 
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {
            SweetheartsLogo()
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                "Let's save your info",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = ChocolateBrown
            )
            Text(
                "so we can serve you faster! ❤️",
                style = MaterialTheme.typography.bodyLarge,
                color = TextBrown.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            Text("What's your name?", style = MaterialTheme.typography.labelLarge, color = TextBrown)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = StrawberryPink) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StrawberryPink,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Phone number", style = MaterialTheme.typography.labelLarge, color = TextBrown)
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() }.take(10) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("(770) 555-1234") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = StrawberryPink) },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StrawberryPink,
                    unfocusedBorderColor = Color.LightGray
                )
            )
            
            Text(
                "We use this to notify you when the truck is on the way!",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { if (name.isNotBlank() && phone.length == 10) onContinue(name, phone) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ChocolateBrown),
                enabled = name.isNotBlank() && phone.length == 10
            ) {
                Text("Continue", style = MaterialTheme.typography.titleMedium)
            }

            Card(
                modifier = Modifier
                    .padding(vertical = 32.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SoftStrawberry),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🍓", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Already saved?", fontWeight = FontWeight.Bold, color = TextBrown)
                        Text("We'll remember you next time.", style = MaterialTheme.typography.bodySmall, color = TextBrown)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VendorAccessScreen(
    onBack: () -> Unit,
    onAccess: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    Scaffold(
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(SoftStrawberry, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock, 
                    contentDescription = null,
                    tint = StrawberryPink,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Vendor Access",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = ChocolateBrown
            )
            Text(
                "Enter your vendor code\nto continue",
                style = MaterialTheme.typography.bodyLarge,
                color = TextBrown.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            // Code Inputs
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(5) { index ->
                    val char = code.getOrNull(index)?.toString() ?: ""
                    Card(
                        modifier = Modifier
                            .size(56.dp)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = if (code.length == index) BorderStroke(2.dp, StrawberryPink) else null,
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = if (char.isNotEmpty()) "●" else "",
                                style = MaterialTheme.typography.headlineMedium,
                                color = ChocolateBrown
                            )
                        }
                    }
                }
            }
            
            // Hidden text field to capture input
            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 5) code = it },
                modifier = Modifier.size(1.dp).padding(0.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { onAccess(code) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ChocolateBrown),
                enabled = code.length >= 4
            ) {
                Text("Access Dashboard", style = MaterialTheme.typography.titleMedium)
            }
            
            TextButton(onClick = onBack) {
                Text("Back", color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerDashboard(
    viewModel: IceCreamViewModel,
    name: String,
    phone: String
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showTryAgainDialog by remember { mutableStateOf(false) }
    var tryAgainMessage by remember { mutableStateOf("Please try again.") }
    
    val context = LocalContext.current
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            locationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.requestDropoff(name, phone, location.latitude, location.longitude)
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    SweetheartsLogo(
                        onHoldComplete = { viewModel.setScreen(IceCreamScreen.VENDOR_ACCESS) }
                    ) 
                },
                actions = {
                    IconButton(onClick = { /* Notification action */ }) {
                        BadgedBox(badge = { Badge { Text("2") } }) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = StrawberryPink
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CreamBackground)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StrawberryPink,
                        selectedTextColor = StrawberryPink,
                        indicatorColor = SoftStrawberry
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.Default.List, contentDescription = "My Requests") },
                    label = { Text("My Requests") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") },
                    label = { Text("Menu") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.Default.Person, contentDescription = "More") },
                    label = { Text("More") }
                )
            }
        },
        containerColor = CreamBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                WelcomeCard(
                    name = name,
                    phoneNumber = phone,
                    onEdit = { showEditProfileDialog = true }
                )
            }

            item {
                TruckStatusCard(
                    status = "Available Now",
                    etaMinutes = 12
                )
            }

            item {
                MapSection(
                    currentLocation = currentLocation,
                    dropoffDisplays = dropoffDisplays,
                    routePolyline = routePolyline,
                    routeLoading = routeLoading,
                    routeError = routeError
                )
            }

            item {
                RequestStopButton(
                    onClick = {
                        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            locationClient.lastLocation.addOnSuccessListener { location ->
                                if (location != null) {
                                    viewModel.requestDropoff(name, phone, location.latitude, location.longitude)
                                } else {
                                    tryAgainMessage = "Location unavailable. Turn on device location and try again."
                                    showTryAgainDialog = true
                                }
                            }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    isLoading = dropoffLoading
                )
            }

            item {
                UpcomingRouteSection(
                    name = name,
                    optimizedDropoffsWithEta = optimizedDropoffsWithEta,
                    dropoffDisplays = dropoffDisplays
                )
            }
        }
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { /* name update logic */ },
                        label = { Text("Name") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showEditProfileDialog = false }) { Text("Save") }
            }
        )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VendorDashboard(
    viewModel: IceCreamViewModel,
    onBack: () -> Unit
) {
    val dropoffDisplays by viewModel.dropoffDisplays.collectAsState()
    val adminDropoffsWithEta by viewModel.adminDropoffsWithEta.collectAsState()
    val adminRoutePolyline by viewModel.adminRoutePolyline.collectAsState()
    val adminRouteLoading by viewModel.adminRouteLoading.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val context = LocalContext.current

    val stopsAhead = adminDropoffsWithEta.size
    val nextStop = adminDropoffsWithEta.firstOrNull()
    val nextStopName = nextStop?.display?.address?.split(",")?.firstOrNull() ?: nextStop?.display?.request?.name ?: "None"
    val nextStopEta = if (nextStop != null && nextStop.etaSecondsFromNow >= 0) "${nextStop.etaSecondsFromNow / 60} min" else "--"

    LaunchedEffect(dropoffDisplays, currentLocation) {
        viewModel.loadAdminRoute(dropoffDisplays)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SweetheartsLogo()
                        Text("Vendor Dashboard", style = MaterialTheme.typography.labelSmall, color = StrawberryPink)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Exit Admin", modifier = Modifier.padding(4.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { /* Notification action */ }) {
                        BadgedBox(badge = { Badge { Text("5") } }) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = StrawberryPink)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CreamBackground)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Dashboard") })
                NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.List, null) }, label = { Text("Route") })
                NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.List, null) }, label = { Text("History") })
                NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Person, null) }, label = { Text("More") })
            }
        },
        containerColor = CreamBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(0.dp)) }

            // Stats Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatusStatCard(
                            modifier = Modifier.weight(1.5f),
                            title = "Status",
                            value = "Serving Now ●",
                            subValue = "Accepting requests",
                            icon = "🚛",
                            valueColor = StatusGreen
                        )
                        NextStopStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Next Stop",
                            value = nextStopEta,
                            subValue = nextStopName,
                            icon = "⏱️"
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Stops Ahead",
                            value = "$stopsAhead",
                            subValue = "Including yours",
                            icon = "📍"
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Success Rate",
                            value = "98%",
                            subValue = "Great job!",
                            icon = "⭐"
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Today",
                            value = "12",
                            subValue = "Total Stops",
                            icon = "📊"
                        )
                    }
                }
            }

            // Map Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    AdminDropoffMap(
                        dropoffDisplays = dropoffDisplays,
                        routePolyline = adminRoutePolyline,
                        adminDropoffsWithEta = adminDropoffsWithEta,
                        currentLocation = currentLocation,
                        routeLoading = adminRouteLoading,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // CTA
            item {
                Button(
                    onClick = { /* Could scroll to pending or open dialog */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChocolateBrown)
                ) {
                    Text("View Pending Requests", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (dropoffDisplays.isNotEmpty()) {
                        Badge(containerColor = StrawberryPink) { Text("${dropoffDisplays.size}", color = Color.White) }
                    }
                }
            }
            
            // Recent Stops / Pending Section
            item {
                Text("Pending Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextBrown)
            }
            
            items(dropoffDisplays, key = { it.request.id }) { display ->
                DropoffRequestRow(
                    display = display,
                    onApprove = { viewModel.updateDropoffStatus(display.request.id, "Approved") },
                    onCancel = { viewModel.updateDropoffStatus(display.request.id, "Canceled") },
                    onDone = { viewModel.markDropoffDone(display.request.id) },
                    onSms = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${display.request.phoneNumber}"))
                        context.startActivity(intent)
                    },
                    onPhone = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${display.request.phoneNumber}"))
                        context.startActivity(intent)
                    },
                    onMap = {
                        val dest = "${display.request.latitude},${display.request.longitude}"
                        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$dest")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String,
    icon: String,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SoftStrawberry, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = valueColor)
                Text(subValue, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun NextStopStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String,
    icon: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = IvoryCardBackground),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = StrawberryPink)
            Text(subValue, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String,
    icon: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.Start) {
            Text(icon)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ChocolateBrown)
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(subValue, style = MaterialTheme.typography.labelSmall, color = Color.LightGray, fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun WelcomeCard(
    name: String,
    phoneNumber: String,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftStrawberry),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextBrown
                )
                Text(
                    text = "$name!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = StrawberryPink
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextBrown
                    )
                    Text(
                        text = " $phoneNumber",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextBrown
                    )
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(14.dp),
                            tint = TextBrown
                        )
                    }
                }
            }
            // Illustration Placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🚚🍦", style = MaterialTheme.typography.displayMedium)
            }
        }
    }
}

@Composable
private fun TruckStatusCard(
    status: String,
    etaMinutes: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = IvoryCardBackground),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFE8F5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🚚", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Truck Status", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(StatusGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreen
                    )
                }
                Text(
                    text = "We're in your area!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            VerticalDivider(
                modifier = Modifier.height(40.dp),
                thickness = 1.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = StrawberryPink
                    )
                    Text(" ETA", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$etaMinutes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = StrawberryPink
                    )
                    Text(
                        text = " min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StrawberryPink,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text("to your stop", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun MapSection(
    currentLocation: Pair<Double, Double>?,
    dropoffDisplays: List<DropoffRequestDisplay>,
    routePolyline: List<Pair<Double, Double>>,
    routeLoading: Boolean,
    routeError: String?
) {
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
            .height(280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
    ) {
        RouteMapWithNativePolyline(
            center = center,
            startLatLng = startLatLng,
            dropoffMarkers = dropoffMarkers,
            routePolyline = routePolyline,
            loading = routeLoading,
            modifier = Modifier.fillMaxSize(),
        )
        
        // "Center on me" button placeholder
        Card(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.BottomStart),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Center on me", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    
    if (routeError != null) {
        Text(
            text = routeError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun RequestStopButton(
    onClick: () -> Unit,
    isLoading: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ChocolateBrown),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🍦", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Request a Stop",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Bring the ice cream to me!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun UpcomingRouteSection(
    name: String,
    optimizedDropoffsWithEta: List<DropoffWithEta>,
    dropoffDisplays: List<DropoffRequestDisplay>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🚩", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Upcoming Route",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextBrown
                )
            }
            Text(
                "See full route >",
                style = MaterialTheme.typography.labelLarge,
                color = StrawberryPink
            )
        }

        val displayList = if (optimizedDropoffsWithEta.isNotEmpty()) {
            optimizedDropoffsWithEta
        } else {
            dropoffDisplays.map { d ->
                DropoffWithEta(display = d, etaSecondsFromNow = -1L)
            }
        }

        displayList.forEachIndexed { index, item ->
            RouteItem(
                index = index + 1,
                name = item.display.request.name,
                address = item.display.address,
                etaSeconds = item.etaSecondsFromNow,
                isUserStop = item.display.request.name.equals(name, ignoreCase = true)
            )
        }
    }
}

@Composable
private fun RouteItem(
    index: Int,
    name: String,
    address: String,
    etaSeconds: Long,
    isUserStop: Boolean
) {
    val etaText = if (etaSeconds >= 0) {
        val etaTime = System.currentTimeMillis() + etaSeconds * 1000L
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(etaTime))
    } else {
        "--:--"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(SoftStrawberry, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StrawberryPink
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isUserStop) "Your Stop" else address.split(",").firstOrNull() ?: name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isUserStop) {
                    Text(
                        text = "Requested",
                        style = MaterialTheme.typography.labelSmall,
                        color = StrawberryPink
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.Gray
                )
                Text(
                    text = " $etaText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.LightGray
                )
            }
        }
    }
}

/** Holder for map, polyline, and markers so we can update when data changes. */
private class MapHolder {
    var googleMap: com.google.android.gms.maps.GoogleMap? = null
    var polyline: com.google.android.gms.maps.model.Polyline? = null
    val markers = mutableListOf<com.google.android.gms.maps.model.Marker>()
    var hasFittedToRoute = false
}

private fun applyMarkersAndRoute(
    map: com.google.android.gms.maps.GoogleMap,
    holder: MapHolder,
    startLatLng: LatLng?,
    dropoffMarkers: List<Pair<LatLng, String>>,
    routePolyline: List<Pair<Double, Double>>,
) {
    holder.markers.forEach { it.remove() }
    holder.markers.clear()
    startLatLng?.let { latLng ->
        map.addMarker(com.google.android.gms.maps.model.MarkerOptions().position(latLng).title("Start"))?.let { holder.markers.add(it) }
    }
    dropoffMarkers.forEach { (latLng, title) ->
        map.addMarker(com.google.android.gms.maps.model.MarkerOptions().position(latLng).title(title))?.let { holder.markers.add(it) }
    }
    applyRoutePolyline(map, holder, routePolyline)
}

private fun applyRoutePolyline(
    map: com.google.android.gms.maps.GoogleMap,
    holder: MapHolder,
    routePolyline: List<Pair<Double, Double>>,
) {
    holder.polyline?.remove()
    holder.polyline = null
    if (routePolyline.isNotEmpty()) {
        val points = routePolyline.map { LatLng(it.first, it.second) }
        holder.polyline = map.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(android.graphics.Color.parseColor("#1565C0"))
                .width(24f)
                .geodesic(false),
        )
    }
}

// Minimum span (in degrees) to use fit-bounds; below this use center+zoom so very close pins don't cause "view size too small".
private const val MIN_BOUNDS_SPAN_DEGREES = 0.002

/** Build bounds that include all markers and route points, then move camera to fit with padding. Uses zoom when fewer than 2 distinct points or when bounds are very small (pins very close) to avoid "Map size should not be 0" / invalid bounds. */
private fun fitMapToRouteAndMarkers(
    map: com.google.android.gms.maps.GoogleMap,
    startLatLng: LatLng?,
    dropoffMarkers: List<Pair<LatLng, String>>,
    routePolyline: List<Pair<Double, Double>>,
    fallbackCenter: LatLng,
    paddingPx: Int = 120,
) {
    val builder = LatLngBounds.builder()
    startLatLng?.let { builder.include(it) }
    dropoffMarkers.forEach { (latLng, _) -> builder.include(latLng) }
    routePolyline.forEach { (lat, lng) -> builder.include(LatLng(lat, lng)) }
    val bounds = builder.build()
    val southwest = bounds.southwest
    val northeast = bounds.northeast
    val latSpan = kotlin.math.abs(northeast.latitude - southwest.latitude)
    val lngSpan = kotlin.math.abs(northeast.longitude - southwest.longitude)
    val hasValidBounds = southwest != northeast
    val notTooTight = hasValidBounds && latSpan >= MIN_BOUNDS_SPAN_DEGREES && lngSpan >= MIN_BOUNDS_SPAN_DEGREES
    if (notTooTight) {
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx))
    } else {
        val singlePoint = startLatLng ?: dropoffMarkers.firstOrNull()?.first
            ?: routePolyline.firstOrNull()?.let { LatLng(it.first, it.second) }
            ?: fallbackCenter
        // Zoom 15 shows streets and pins clearly when bounds aren't used
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(singlePoint, 15f))
    }
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
        // MapView never receives ON_START/ON_RESUME if we're already resumed when composed
        val state = lifecycleOwner.lifecycle.currentState
        if (state >= Lifecycle.State.STARTED) mapView.onStart()
        if (state >= Lifecycle.State.RESUMED) mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
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
                        applyMarkersAndRoute(map, holder, startLatLng, dropoffMarkers, routePolyline)
                        // Defer camera update until MapView has been laid out (avoids "Map size should not be 0")
                        mapView.post {
                            if (routePolyline.isNotEmpty()) {
                                fitMapToRouteAndMarkers(map, startLatLng, dropoffMarkers, routePolyline, center)
                                holder.hasFittedToRoute = true
                            } else {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 15f))
                            }
                        }
                    }
                } else {
                    holder.googleMap?.let { map ->
                        applyMarkersAndRoute(map, holder, startLatLng, dropoffMarkers, routePolyline)
                        if (routePolyline.isNotEmpty() && !holder.hasFittedToRoute) {
                            mapView.post {
                                fitMapToRouteAndMarkers(map, startLatLng, dropoffMarkers, routePolyline, center)
                                holder.hasFittedToRoute = true
                            }
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
    // Old implementation replaced by MapSection and UpcomingRouteSection in the main layout
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
            contentPadding = PaddingValues(16.dp),
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

@Preview(showBackground = true, backgroundColor = 0xFFFFF9F2)
@Composable
fun WelcomeCardPreview() {
    IceCreamAppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            WelcomeCard(
                name = "Jacque",
                phoneNumber = "(770) 555-1234",
                onEdit = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF9F2)
@Composable
fun TruckStatusCardPreview() {
    IceCreamAppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TruckStatusCard(
                status = "Available Now",
                etaMinutes = 12
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF9F2)
@Composable
fun RequestStopButtonPreview() {
    IceCreamAppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RequestStopButton(
                onClick = {},
                isLoading = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF9F2)
@Composable
fun RouteItemPreview() {
    IceCreamAppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RouteItem(
                index = 1,
                name = "Oak Street",
                address = "123 Oak St, Whitesburg",
                etaSeconds = 300,
                isUserStop = true
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF9F2)
@Composable
fun UpcomingRouteSectionPreview() {
    val sampleDropoffs = listOf(
        DropoffWithEta(
            display = DropoffRequestDisplay(
                request = com.icecreamapp.sweethearts.data.DropoffRequest(id="1", name="Oak Street", phoneNumber="555-0001", latitude=0.0, longitude=0.0),
                address = "Oak Street",
                distanceMeters = 100.0
            ),
            etaSecondsFromNow = 600
        ),
        DropoffWithEta(
            display = DropoffRequestDisplay(
                request = com.icecreamapp.sweethearts.data.DropoffRequest(id="2", name="New Chapel Road", phoneNumber="555-0002", latitude=0.0, longitude=0.0),
                address = "New Chapel Road",
                distanceMeters = 200.0
            ),
            etaSecondsFromNow = 1200
        ),
        DropoffWithEta(
            display = DropoffRequestDisplay(
                request = com.icecreamapp.sweethearts.data.DropoffRequest(id="3", name="Jacque", phoneNumber="555-0003", latitude=0.0, longitude=0.0),
                address = "Your Stop",
                distanceMeters = 300.0
            ),
            etaSecondsFromNow = 1800
        )
    )
    IceCreamAppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            UpcomingRouteSection(
                name = "Jacque",
                optimizedDropoffsWithEta = sampleDropoffs,
                dropoffDisplays = emptyList()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FlavorCardPreview() {
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
