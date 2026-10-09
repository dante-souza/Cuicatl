// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import io.github.dante_souza.cuicatl.domain.CaptureReadiness
import io.github.dante_souza.cuicatl.domain.MeasurementFrame
import io.github.dante_souza.cuicatl.domain.MeasurementSession
import io.github.dante_souza.cuicatl.domain.SavedSessionDetail
import io.github.dante_souza.cuicatl.domain.SessionRuntimeSnapshot
import io.github.dante_souza.cuicatl.platform.AndroidCaptureReadinessProvider
import io.github.dante_souza.cuicatl.platform.MeasurementService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

private enum class AnalyzerPage(val label: String) {
    METER("Meter"),
    HISTORY("History"),
}

@Composable
fun CuicatlApp() {
    val context = LocalContext.current
    val measurementService = rememberMeasurementService()
    var page by rememberSaveable { mutableStateOf(AnalyzerPage.METER) }
    var sessionLabel by rememberSaveable { mutableStateOf("") }
    var snapshot by remember { mutableStateOf(SessionRuntimeSnapshot()) }
    var savedSessions by remember { mutableStateOf(emptyList<MeasurementSession>()) }
    var selectedDetail by remember { mutableStateOf<SavedSessionDetail?>(null) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
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

    DisposableEffect(measurementService) {
        if (measurementService == null) {
            onDispose { }
        } else {
            savedSessions = measurementService.savedSessions()
            val listener: (SessionRuntimeSnapshot) -> Unit = { updated ->
                snapshot = updated
                if (
                    updated.status == SessionRuntimeSnapshot.Status.READY ||
                    updated.status == SessionRuntimeSnapshot.Status.FAILED
                ) {
                    savedSessions = measurementService.savedSessions()
                }
            }
            measurementService.addListener(listener)
            onDispose {
                measurementService.removeListener(listener)
            }
        }
    }

    fun startMeasurement() {
        val intent = Intent(context, MeasurementService::class.java).apply {
            action = MeasurementService.ACTION_START
            putExtra(MeasurementService.EXTRA_LABEL, sessionLabel)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
        permissionDenied = !granted
        if (granted) {
            startMeasurement()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AnalyzerPage.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = page == destination,
                        onClick = { page = destination },
                        icon = {
                            Text(if (destination == AnalyzerPage.METER) "dB" else "↔")
                        },
                        label = { Text(destination.label) },
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
                text = "Phase 1 · usable session + preservation + export",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )

            when (page) {
                AnalyzerPage.METER -> {
                    MeterPage(
                        snapshot = snapshot,
                        sessionLabel = sessionLabel,
                        onLabelChange = { sessionLabel = it },
                        readiness = readiness,
                        permissionGranted = permissionGranted,
                        permissionDenied = permissionDenied,
                        onStart = {
                            if (permissionGranted) {
                                startMeasurement()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStop = {
                            context.startService(
                                Intent(context, MeasurementService::class.java).apply {
                                    action = MeasurementService.ACTION_STOP
                                },
                            )
                        },
                    )
                }

                AnalyzerPage.HISTORY -> {
                    HistoryPage(
                        snapshot = snapshot,
                        savedSessions = savedSessions,
                        selectedDetail = selectedDetail,
                        onSelect = { session ->
                            selectedDetail = measurementService?.loadSavedSession(session.id)
                        },
                        onBack = { selectedDetail = null },
                        onShare = { detail ->
                            val file = measurementService?.exportSavedSession(detail.session.id)
                            if (file != null) {
                                shareCsv(context, file)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MeterPage(
    snapshot: SessionRuntimeSnapshot,
    sessionLabel: String,
    onLabelChange: (String) -> Unit,
    readiness: CaptureReadiness,
    permissionGranted: Boolean,
    permissionDenied: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val isActive =
        snapshot.status == SessionRuntimeSnapshot.Status.STARTING ||
            snapshot.status == SessionRuntimeSnapshot.Status.RUNNING ||
            snapshot.status == SessionRuntimeSnapshot.Status.FINALIZING
    val current = snapshot.currentFrame
    val session = snapshot.activeSession

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Digital level", style = MaterialTheme.typography.titleLarge)
            Text(
                text = formatDb(current?.rmsDbfs),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text("RMS · dBFS")
            Text("Sample peak: " + formatDb(current?.samplePeakDbfs))
            Text("Elapsed: " + formatDuration(session?.elapsedMillis ?: 0L))
            Text("Captured: " + formatDuration(session?.capturedMillis ?: 0L))
            Text("Source: " + (session?.source?.ifBlank { "—" } ?: "—"))
            Text(
                "Sample rate: " +
                    if ((session?.sampleRateHz ?: 0) > 0) {
                        session?.sampleRateHz.toString() + " Hz"
                    } else {
                        "—"
                    },
            )
            Text(
                "Clipped samples, latest frame: " +
                    (current?.clippedSampleCount ?: 0),
            )
            Text(
                if (snapshot.clippedFrameCount > 0) {
                    "Clipping observed in this session: " +
                        snapshot.clippedFrameCount + " frames · " +
                        snapshot.clippedSampleCount + " samples"
                } else {
                    "Session clipping: none observed"
                },
                color =
                    if (snapshot.clippedFrameCount > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
            )
            Text(
                snapshot.message,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                "Digital input only. Cuicatl is not displaying calibrated SPL in Phase 1.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    if (snapshot.history.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Live history", style = MaterialTheme.typography.titleMedium)
                LevelHistoryChart(snapshot.history)
                Text(
                    "100 ms persisted measurement frames · chart is presentation only",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = sessionLabel,
                onValueChange = onLabelChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Session label") },
                enabled = !isActive,
                singleLine = true,
            )

            val readinessText =
                if (permissionDenied) {
                    "Microphone permission denied. No capture was started."
                } else {
                    when (readiness) {
                        CaptureReadiness.Ready ->
                            "Ready. Recording starts only when you press Start."

                        is CaptureReadiness.NotReady ->
                            readiness.detail ?: "Microphone capture is not ready."
                    }
                }
            Text(
                readinessText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )

            Button(
                onClick = if (isActive) onStop else onStart,
                enabled =
                    isActive ||
                        readiness !is CaptureReadiness.NotReady ||
                        readiness.reason != CaptureReadiness.Reason.MICROPHONE_UNAVAILABLE,
            ) {
                Text(
                    when (snapshot.status) {
                        SessionRuntimeSnapshot.Status.STARTING -> "Stop"
                        SessionRuntimeSnapshot.Status.RUNNING -> "Stop"
                        SessionRuntimeSnapshot.Status.FINALIZING -> "Finalizing…"
                        else -> if (permissionGranted) "Start" else "Grant + Start"
                    },
                )
            }
        }
    }
}

@Composable
private fun HistoryPage(
    snapshot: SessionRuntimeSnapshot,
    savedSessions: List<MeasurementSession>,
    selectedDetail: SavedSessionDetail?,
    onSelect: (MeasurementSession) -> Unit,
    onBack: () -> Unit,
    onShare: (SavedSessionDetail) -> Unit,
) {
    if (selectedDetail != null) {
        SavedSessionDetailCard(
            detail = selectedDetail,
            onBack = onBack,
            onShare = onShare,
        )
        return
    }

    if (snapshot.history.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Current session", style = MaterialTheme.typography.titleLarge)
                LevelHistoryChart(snapshot.history)
                Text(
                    "Page changes do not own or reset capture.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Saved sessions", style = MaterialTheme.typography.titleLarge)

            if (savedSessions.isEmpty()) {
                Text(
                    "No saved Phase 1 sessions yet.",
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                savedSessions.forEach { session ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            session.label.ifBlank { "Unlabeled session" },
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            formatSessionDate(session.startedAtUtcEpochMillis) +
                                " · " + session.outcome?.name.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            formatDuration(session.capturedMillis) +
                                " captured · " + session.frameCount + " frames",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = { onSelect(session) }) {
                            Text("Open")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedSessionDetailCard(
    detail: SavedSessionDetail,
    onBack: () -> Unit,
    onShare: (SavedSessionDetail) -> Unit,
) {
    val session = detail.session
    val clippedFrames = detail.frames.count { it.clippedSampleCount > 0 }
    val clippedSamples = detail.frames.sumOf { it.clippedSampleCount.toLong() }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                Button(onClick = { onShare(detail) }) { Text("Share CSV") }
            }

            Text(
                session.label.ifBlank { "Unlabeled session" },
                style = MaterialTheme.typography.titleLarge,
            )
            Text("Outcome: " + session.outcome?.name.orEmpty())
            Text("Captured: " + formatDuration(session.capturedMillis))
            Text(
                "Elapsed: " +
                    (session.elapsedMillis?.let(::formatDuration) ?: "unavailable after recovery"),
            )
            Text("Source: " + session.source)
            Text("Sample rate: " + session.sampleRateHz + " Hz")
            Text("Input: " + session.inputIdentity)
            Text(
                if (clippedFrames > 0) {
                    "Clipping: " + clippedFrames + " frames · " +
                        clippedSamples + " samples"
                } else {
                    "Clipping: none observed"
                },
                color =
                    if (clippedFrames > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
            )
            if (!session.interruptionReason.isNullOrBlank()) {
                Text(
                    "Interruption: " + session.interruptionReason,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Recorded history", style = MaterialTheme.typography.titleMedium)
            LevelHistoryChart(detail.frames)
            Text(
                "CSV schema 1 · validated Phase 1 digital measurement export.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun LevelHistoryChart(frames: List<MeasurementFrame>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val guideColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val displayFrames = remember(frames) {
        if (frames.size <= 320) {
            frames
        } else {
            val chunk = ceil(frames.size / 320.0).toInt()
            frames.chunked(chunk).map { group -> group.last() }
        }
    }
    val values = displayFrames.mapNotNull { it.rmsDbfs }

    if (values.size < 2) {
        Text("Waiting for enough non-zero frames…")
        return
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        drawLine(
            color = guideColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
        )
        drawLine(
            color = guideColor,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
        )

        values.zipWithNext().forEachIndexed { index, pair ->
            val x1 = size.width * index / (values.size - 1).toFloat()
            val x2 = size.width * (index + 1) / (values.size - 1).toFloat()
            val y1 = dbToY(pair.first, size.height)
            val y2 = dbToY(pair.second, size.height)
            drawLine(
                color = lineColor,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 3f,
            )
        }
    }
}

private fun dbToY(db: Double, height: Float): Float {
    val clamped = db.coerceIn(-90.0, 0.0)
    return ((0.0 - clamped) / 90.0 * height).toFloat()
}

@Composable
private fun rememberMeasurementService(): MeasurementService? {
    val context = LocalContext.current
    var service by remember { mutableStateOf<MeasurementService?>(null) }
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = (binder as? MeasurementService.LocalBinder)?.service()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }
    }

    DisposableEffect(context, connection) {
        val bound = context.bindService(
            Intent(context, MeasurementService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )
        onDispose {
            if (bound) {
                runCatching { context.unbindService(connection) }
            }
        }
    }
    return service
}

private fun shareCsv(context: Context, file: java.io.File) {
    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file,
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(shareIntent, "Share Cuicatl measurement CSV"),
    )
}

private fun formatDb(value: Double?): String =
    value?.let { String.format(Locale.US, "%.1f dBFS", it) } ?: "—"

private fun formatDuration(milliseconds: Long): String =
    String.format(
        Locale.US,
        "%d:%02d.%01d",
        milliseconds / 60_000L,
        (milliseconds / 1_000L) % 60L,
        (milliseconds / 100L) % 10L,
    )

private fun formatSessionDate(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))
