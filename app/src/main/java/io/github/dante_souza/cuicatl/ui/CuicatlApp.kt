// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.dante_souza.cuicatl.domain.CaptureReadiness
import io.github.dante_souza.cuicatl.platform.AndroidAudioProbe
import io.github.dante_souza.cuicatl.platform.AndroidCaptureReadinessProvider
import io.github.dante_souza.cuicatl.platform.AudioProbeSnapshot
import java.util.Locale

private enum class AnalyzerPage(val label: String) {
    METER("Meter"),
    HISTORY("History"),
}

@Composable
fun CuicatlApp() {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(AnalyzerPage.METER) }
    var probeSnapshot by remember { mutableStateOf(AudioProbeSnapshot()) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val readinessProvider = remember(context) {
        AndroidCaptureReadinessProvider(context)
    }
    val readiness = remember(permissionGranted) {
        readinessProvider.readiness()
    }

    val probe = remember(context) {
        AndroidAudioProbe(context) { snapshot ->
            probeSnapshot = snapshot
        }
    }

    DisposableEffect(probe) {
        onDispose {
            probe.close()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            probe.start()
        } else {
            probeSnapshot = AudioProbeSnapshot(
                message = "Microphone permission denied. No capture was started.",
            )
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AnalyzerPage.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = page == destination,
                        onClick = { page = destination },
                        icon = { Text(if (destination == AnalyzerPage.METER) "dB" else "↔") },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Cuicatl",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Phase 0B · J8 capture/timing probe",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )

            when (page) {
                AnalyzerPage.METER -> MeterProbe(
                    snapshot = probeSnapshot,
                    readiness = readiness,
                )

                AnalyzerPage.HISTORY -> HistoryShell()
            }

            ProbeControls(
                snapshot = probeSnapshot,
                permissionGranted = permissionGranted,
                readiness = readiness,
                onStart = {
                    if (permissionGranted) {
                        probe.start()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStop = probe::stop,
            )
        }
    }
}

@Composable
private fun MeterProbe(
    snapshot: AudioProbeSnapshot,
    readiness: CaptureReadiness,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Diagnostic meter", style = MaterialTheme.typography.titleLarge)
            Text(
                when (readiness) {
                    CaptureReadiness.Ready ->
                        "Microphone readiness checks pass. Capture still requires Start probe."

                    is CaptureReadiness.NotReady ->
                        readiness.detail ?: "Microphone capture is not ready."
                },
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatDb(snapshot.rmsDbfs),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text("RMS · digital input only")
            Text("Peak: ${formatDb(snapshot.peakDbfs)}")
            Text("Source: ${snapshot.source}")
            Text("Sample rate: ${snapshot.sampleRateHz?.let { "${it} Hz" } ?: "—"}")
            Text("Samples: ${snapshot.sampleCount}")
            Text("Elapsed: ${snapshot.elapsedMillis} ms")
            Text("Route: ${snapshot.route}")
            Text("Timing: ${snapshot.timing}")
            Text("Processing: ${snapshot.processing}")

            if (snapshot.attemptedConfigurations.isNotBlank()) {
                Text(
                    "Attempted: ${snapshot.attemptedConfigurations}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Text(
                snapshot.message,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                "These values are dBFS diagnostics. They are not calibrated SPL.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HistoryShell() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("History", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Phase 0B does not persist a public measurement history.")
            Text(
                "The probe establishes the real J8 capture and timing path first. Saved history begins with the Phase 1 usable-session milestone.",
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ProbeControls(
    snapshot: AudioProbeSnapshot,
    permissionGranted: Boolean,
    readiness: CaptureReadiness,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val isRunning = snapshot.status == AudioProbeSnapshot.Status.RUNNING

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when (snapshot.status) {
                        AudioProbeSnapshot.Status.IDLE ->
                            if (permissionGranted) "Ready" else "Permission required"

                        AudioProbeSnapshot.Status.RUNNING -> "Diagnostic capture running"
                        AudioProbeSnapshot.Status.COMPLETED -> "Probe completed"
                        AudioProbeSnapshot.Status.FAILED -> "Probe failed"
                    },
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (isRunning) "Maximum run: 10 seconds" else "No automatic microphone start",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Button(
                onClick = if (isRunning) onStop else onStart,
                enabled = isRunning ||
                    readiness !is CaptureReadiness.NotReady ||
                    readiness.reason != CaptureReadiness.Reason.MICROPHONE_UNAVAILABLE,
            ) {
                Text(if (isRunning) "Stop probe" else "Start probe")
            }
        }
    }
}

private fun formatDb(value: Double?): String =
    value?.let { String.format(Locale.US, "%.1f dBFS", it) } ?: "—"
