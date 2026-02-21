package com.icecreamapp.sweethearts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.MapsInitializer
import com.google.firebase.messaging.FirebaseMessaging
import com.icecreamapp.sweethearts.fcm.FcmTokenRepository
import com.icecreamapp.sweethearts.ui.MainScreen
import com.icecreamapp.sweethearts.ui.IceCreamViewModel
import com.icecreamapp.sweethearts.ui.IceCreamViewModelFactory
import com.icecreamapp.sweethearts.ui.theme.IceCreamAppTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PUSH_TITLE = "push_title"
        const val EXTRA_PUSH_BODY = "push_body"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { getFcmToken() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent?.let { i ->
            i.getStringExtra(EXTRA_PUSH_TITLE)?.let { title ->
                i.getStringExtra(EXTRA_PUSH_BODY)?.let { body ->
                    com.icecreamapp.sweethearts.fcm.PushMessageStore.set(title, body)
                }
            }
        }
        requestNotificationPermissionIfNeeded()
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LEGACY) {
            setContent {
            IceCreamAppTheme {
                val app = LocalContext.current.applicationContext as android.app.Application
                val viewModel: IceCreamViewModel = viewModel(factory = IceCreamViewModelFactory(app))
                MainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> getFcmToken()
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            getFcmToken()
        }
    }

    private fun getFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.let { token -> FcmTokenRepository.registerToken(token) }
            }
        }
        // Subscribe to "all-users" so curl with "topic": "all-users" delivers to this device.
        FirebaseMessaging.getInstance().subscribeToTopic("all-users")
    }
}