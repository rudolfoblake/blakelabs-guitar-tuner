package com.blakelabs.guitartuner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blakelabs.guitartuner.ui.BlakeColors
import com.blakelabs.guitartuner.ui.BlakeTunerTheme
import com.blakelabs.guitartuner.ui.BrandSplash
import com.blakelabs.guitartuner.ui.TunerScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BlakeTunerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BlakeColors.Background,
                ) {
                    val tunerViewModel: TunerViewModel = viewModel()
                    val state by tunerViewModel.state.collectAsStateWithLifecycle()
                    val context = LocalContext.current
                    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle

                    var microphoneGranted by remember {
                        mutableStateOf(
                            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                                PackageManager.PERMISSION_GRANTED,
                        )
                    }
                    var shouldListen by remember { mutableStateOf(true) }
                    var showBrandSplash by rememberSaveable { mutableStateOf(true) }
                    var lifecycleStarted by remember {
                        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                    ) { granted ->
                        microphoneGranted = granted
                    }

                    LaunchedEffect(Unit) {
                        if (showBrandSplash) {
                            delay(BRAND_SPLASH_DURATION_MS)
                            showBrandSplash = false
                        }
                    }

                    DisposableEffect(lifecycle, context) {
                        val observer = LifecycleEventObserver { _, event ->
                            lifecycleStarted = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                            if (event == Lifecycle.Event.ON_RESUME) {
                                // Permission can change while the user is in Android settings.
                                microphoneGranted =
                                    context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                                    PackageManager.PERMISSION_GRANTED
                            }
                        }
                        lifecycle.addObserver(observer)
                        onDispose { lifecycle.removeObserver(observer) }
                    }

                    val captureEnabled =
                        !showBrandSplash && microphoneGranted && shouldListen && lifecycleStarted
                    LaunchedEffect(captureEnabled) {
                        if (captureEnabled) tunerViewModel.start() else tunerViewModel.stop()
                    }

                    DisposableEffect(tunerViewModel) {
                        onDispose { tunerViewModel.stop() }
                    }

                    if (showBrandSplash) {
                        BrandSplash()
                    } else {
                        TunerScreen(
                            state = state,
                            microphoneGranted = microphoneGranted,
                            shouldListen = shouldListen,
                            onRequestMicrophone = {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            onListeningChange = { enabled ->
                                shouldListen = enabled
                                if (enabled && !microphoneGranted) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onModeChange = tunerViewModel::setMode,
                            onPresetChange = tunerViewModel::setPreset,
                            onStringSelected = tunerViewModel::selectString,
                            onA4Change = tunerViewModel::adjustA4,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val BRAND_SPLASH_DURATION_MS = 900L
    }
}
