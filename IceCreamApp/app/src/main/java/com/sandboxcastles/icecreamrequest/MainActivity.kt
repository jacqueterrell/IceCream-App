package com.sandboxcastles.icecreamrequest

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.sandboxcastles.icecreamrequest.ui.theme.IceCreamAppTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val okHttpClient = OkHttpClient()
    private var fcmToken: String? = null
    private var deviceLocation: Location? = null
    
    // Cloud Function URLs - update these with your actual function URLs
    private val storeDeviceLocationUrl = "https://storedevicelocation-shobrfiexq-uc.a.run.app"
    private val storePersonLocationUrl = "https://storepersonlocation-shobrfiexq-uc.a.run.app"
    
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.d(TAG, "Notification permission request result: $isGranted")
        if (isGranted) {
            Log.d(TAG, "Notification permission granted by user")
            requestLocationPermission()
        } else {
            Log.w(TAG, "Notification permission denied by user")
        }
    }
    
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            Log.d(TAG, "Location permission granted")
            getFCMToken()
            getCurrentLocation()
        } else {
            Log.w(TAG, "Location permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Request notification permission for Android 13+
        requestNotificationPermission()
        
        setContent {
            IceCreamAppTheme {
                IceCreamAppApp(
                    onRequestIceCream = { name, phone ->
                        requestIceCream(name, phone)
                    },
                    getCurrentLocation = { deviceLocation }
                )
            }
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (hasPermission) {
                Log.d(TAG, "Notification permission already granted")
                requestLocationPermission()
            } else {
                Log.d(TAG, "Requesting notification permission...")
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // For Android 12 and below, notification permission is granted by default
            Log.d(TAG, "Android version < 13, notification permission not required")
            requestLocationPermission()
        }
    }
    
    private fun requestLocationPermission() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (fineLocationGranted || coarseLocationGranted) {
            Log.d(TAG, "Location permission already granted")
            getFCMToken()
            getCurrentLocation()
        } else {
            Log.d(TAG, "Requesting location permission...")
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            fcmToken = token
            Log.d(TAG, "FCM Registration Token: $token")
            
            // Try to call Cloud Function if location is also available
            tryCallCloudFunction()
        }
    }
    
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted")
            return
        }
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                deviceLocation = location
                Log.d(TAG, "Location obtained: ${location.latitude}, ${location.longitude}")
                
                // Try to call Cloud Function if FCM token is also available
                tryCallCloudFunction()
            } else {
                Log.w(TAG, "Location is null, requesting location update...")
                // If last location is not available, request a new location
                requestLocationUpdate()
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to get location", exception)
        }
    }
    
    private fun requestLocationUpdate() {
        // This is a simplified approach - in production you might want to use
        // LocationRequest with LocationCallback for more control
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    deviceLocation = location
                    Log.d(TAG, "Location obtained from update: ${location.latitude}, ${location.longitude}")
                    tryCallCloudFunction()
                } else {
                    Log.w(TAG, "Location still null after update request")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location", e)
        }
    }
    
    private fun tryCallCloudFunction() {
        if (fcmToken != null && deviceLocation != null) {
            Log.d(TAG, "Both FCM token and location available, calling Cloud Function")
            callStoreDeviceLocation(fcmToken!!, deviceLocation!!)
        } else {
            Log.d(TAG, "Waiting for both FCM token and location. Token: ${fcmToken != null}, Location: ${deviceLocation != null}")
        }
    }
    
    private fun callStoreDeviceLocation(deviceId: String, location: Location) {
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("latitude", location.latitude)
            put("longitude", location.longitude)
        }
        
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(storeDeviceLocationUrl)
            .post(requestBody)
            .build()
        
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to call Cloud Function", e)
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully called Cloud Function: $responseBody")
                } else {
                    Log.e(TAG, "Cloud Function returned error: ${response.code} - $responseBody")
                }
                response.close()
            }
        })
    }
    
    private fun requestIceCream(name: String, phone: String) {
        Log.d(TAG, "Request ice cream: name=$name, phone=$phone")
        
        // Check if location permission is granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted, requesting...")
            requestLocationPermission()
            return
        }
        
        // Get current location
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                Log.d(TAG, "Location obtained: ${location.latitude}, ${location.longitude}")
                callStorePersonLocation(name, phone, location)
            } else {
                Log.w(TAG, "Location is null, requesting location update...")
                // Try to get a fresh location
                requestLocationUpdateForPerson(name, phone)
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to get location for ice cream request", exception)
        }
    }
    
    private fun requestLocationUpdateForPerson(name: String, phone: String) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d(TAG, "Location obtained from update: ${location.latitude}, ${location.longitude}")
                    callStorePersonLocation(name, phone, location)
                } else {
                    Log.e(TAG, "Location still null after update request")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location", e)
        }
    }
    
    private fun callStorePersonLocation(name: String, phone: String, location: Location) {
        val json = JSONObject().apply {
            put("name", name)
            put("phone", phone)
            put("latitude", location.latitude)
            put("longitude", location.longitude)
        }
        
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(storePersonLocationUrl)
            .post(requestBody)
            .build()
        
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to call storePersonLocation", e)
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully stored person location: $responseBody")
                } else {
                    Log.e(TAG, "storePersonLocation returned error: ${response.code} - $responseBody")
                }
                response.close()
            }
        })
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}

@PreviewScreenSizes
@Composable
fun IceCreamAppApp(
    onRequestIceCream: (String, String) -> Unit = { _, _ -> },
    getCurrentLocation: () -> Location? = { null }
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var currentDeviceLocation by remember { mutableStateOf<Location?>(getCurrentLocation()) }
    
    // Update location when destination changes to Requests
    LaunchedEffect(currentDestination) {
        if (currentDestination == AppDestinations.REQUESTS) {
            currentDeviceLocation = getCurrentLocation()
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> IceCreamRequestScreen(
                    onRequestIceCream = onRequestIceCream,
                    modifier = Modifier.padding(innerPadding)
                )
                AppDestinations.REQUESTS -> RequestsScreen(
                    currentLocation = currentDeviceLocation,
                    modifier = Modifier.padding(innerPadding),
                    onRefreshLocation = {
                        // Refresh location when Requests screen is viewed
                        currentDeviceLocation = getCurrentLocation()
                    }
                )
                AppDestinations.PROFILE -> Greeting(
                    name = "Profile",
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun IceCreamRequestScreen(
    onRequestIceCream: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Request Ice Cream",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Button(
            onClick = {
                if (name.isNotBlank() && phone.isNotBlank()) {
                    onRequestIceCream(name.trim(), phone.trim())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && phone.isNotBlank()
        ) {
            Text("Request Ice Cream")
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    REQUESTS("Requests", Icons.Default.List),
    PROFILE("Profile", Icons.Default.AccountBox),
}

// Data class for person from Firestore
data class PersonData(
    val id: String,
    val name: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: com.google.firebase.Timestamp? = null
)

@Composable
fun RequestsScreen(
    currentLocation: Location?,
    modifier: Modifier = Modifier,
    onRefreshLocation: (() -> Unit)? = null
) {
    var people by remember { mutableStateOf<List<PersonData>>(emptyList()) }
    var location by remember { mutableStateOf(currentLocation) }
    val db = FirebaseFirestore.getInstance()
    
    // Update location when screen is displayed
    LaunchedEffect(Unit) {
        location = currentLocation
        onRefreshLocation?.invoke()
    }
    
    // Update location when currentLocation changes
    LaunchedEffect(currentLocation) {
        location = currentLocation
    }
    
    // Listen to Firestore "people" collection for real-time updates
    DisposableEffect(Unit) {
        val listenerRegistration = db.collection("people")
            .addSnapshotListener { snapshot: QuerySnapshot?, error: Exception? ->
                if (error != null) {
                    Log.e("RequestsScreen", "Error listening to Firestore", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val peopleList = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val location = data["location"] as? Map<*, *>
                        val lat = (location?.get("latitude") as? Number)?.toDouble()
                        val lng = (location?.get("longitude") as? Number)?.toDouble()
                        
                        if (lat != null && lng != null) {
                            PersonData(
                                id = doc.id,
                                name = (data["name"] as? String) ?: "",
                                phone = (data["phone"] as? String) ?: "",
                                latitude = lat,
                                longitude = lng,
                                timestamp = data["timestamp"] as? com.google.firebase.Timestamp
                            )
                        } else {
                            null
                        }
                    }
                    people = peopleList
                }
            }
        
        onDispose {
            listenerRegistration.remove()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Ice Cream Requests",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (people.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No requests yet")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(people) { person ->
                    PersonCard(
                        person = person,
                        currentLocation = location
                    )
                }
            }
        }
    }
}

@Composable
fun PersonCard(
    person: PersonData,
    currentLocation: Location?
) {
    val distance = currentLocation?.let { location ->
        calculateDistance(
            location.latitude,
            location.longitude,
            person.latitude,
            person.longitude
        )
    }
    
    val formattedPhone = formatPhoneNumberUS(person.phone)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row {
                Text(
                    text = "Phone: ",
                    fontWeight = FontWeight.Medium
                )
                Text(text = formattedPhone)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (distance != null) {
                    "Distance: ${String.format("%.2f", distance)} miles"
                } else {
                    "Distance: Location unavailable"
                },
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Location: ${String.format("%.6f", person.latitude)}, ${String.format("%.6f", person.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Calculate distance between two coordinates in miles (Haversine formula)
fun calculateDistance(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val earthRadiusMiles = 3958.8 // Earth radius in miles
    
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    
    return earthRadiusMiles * c
}

// Format phone number to US format: (XXX) XXX-XXXX
fun formatPhoneNumberUS(phone: String): String {
    // Remove all non-digit characters
    val digits = phone.filter { it.isDigit() }
    
    return when {
        digits.length == 10 -> {
            // Format as (XXX) XXX-XXXX
            "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
        }
        digits.length == 11 && digits.startsWith("1") -> {
            // Format as +1 (XXX) XXX-XXXX
            "+1 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7)}"
        }
        else -> {
            // Return original if can't format
            phone
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IceCreamAppTheme {
        Greeting("Android")
    }
}