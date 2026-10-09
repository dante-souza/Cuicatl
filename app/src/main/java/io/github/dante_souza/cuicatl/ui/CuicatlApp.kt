// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Build
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
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
import io.github.dante_souza.cuicatl.domain.FrequencyWeighting
import io.github.dante_souza.cuicatl.domain.MeterProjection
import io.github.dante_souza.cuicatl.domain.AgcRequest
import io.github.dante_souza.cuicatl.domain.CaptureReadiness
import io.github.dante_souza.cuicatl.domain.MeasurementFrame
import io.github.dante_souza.cuicatl.domain.MeasurementSession
import io.github.dante_souza.cuicatl.domain.MeasurementInputConfiguration
import io.github.dante_souza.cuicatl.domain.ReferenceAdjustment
import io.github.dante_souza.cuicatl.domain.SavedSessionDetail
import io.github.dante_souza.cuicatl.domain.SessionRuntimeSnapshot
import io.github.dante_souza.cuicatl.platform.AndroidAudioProcessingCapabilities
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
    var agcRequest by rememberSaveable { mutableStateOf(AgcRequest.DEFAULT) }
    var frequencyWeighting by rememberSaveable { mutableStateOf(FrequencyWeighting.Z) }
    var snapshot by remember { mutableStateOf(SessionRuntimeSnapshot()) }
    var savedSessions by remember { mutableStateOf(emptyList<MeasurementSession>()) }
    var selectedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDetail by remember { mutableStateOf<SavedSessionDetail?>(null) }
    var activeReferenceAdjustment by remember { mutableStateOf<ReferenceAdjustment?>(null) }
    val scrollState = rememberScrollState()
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
    val agcAvailable = remember {
        AndroidAudioProcessingCapabilities.isAgcAvailable()
    }
    val readiness = remember(permissionGranted) {
        readinessProvider.readiness()
    }

    DisposableEffect(measurementService) {
        if (measurementService == null) {
            onDispose { }
        } else {
            savedSessions = measurementService.savedSessions()
            activeReferenceAdjustment = measurementService.activeReferenceAdjustment()
            selectedSessionId?.let { selectedId ->
                selectedDetail = measurementService.loadSavedSession(selectedId)
            }
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
            putExtra(MeasurementService.EXTRA_AGC_REQUEST, agcRequest.name)
            putExtra(MeasurementService.EXTRA_FREQUENCY_WEIGHTING, frequencyWeighting.name)
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

    BackHandler(enabled = selectedDetail != null) {
        selectedDetail = null
        selectedSessionId = null
    }

    LaunchedEffect(page, selectedSessionId) {
        scrollState.scrollTo(0)
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
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Cuicatl",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Phase 2 · digital A/Z meter (uncalibrated)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )

            when (page) {
                AnalyzerPage.METER -> {
                    MeterPage(
                        snapshot = snapshot,
                        activeReferenceAdjustment = activeReferenceAdjustment,
                        sessionLabel = sessionLabel,
                        onLabelChange = { sessionLabel = it },
                        frequencyWeighting = frequencyWeighting,
                        onFrequencyWeightingChange = { frequencyWeighting = it },
                        agcRequest = agcRequest,
                        onAgcRequestChange = { agcRequest = it },
                        agcAvailable = agcAvailable,
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
                            selectedSessionId = session.id
                            selectedDetail = measurementService?.loadSavedSession(session.id)
                        },
                        onBack = {
                            selectedDetail = null
                            selectedSessionId = null
                        },
                        onShare = { detail ->
                            val file = measurementService?.exportSavedSession(detail.session.id)
                            if (file != null) {
                                shareCsv(context, file)
                            }
                        },
                        activeReferenceAdjustment = activeReferenceAdjustment,
                        onCreateReferenceAdjustment = { detail, level, method, notes ->
                            val error = measurementService?.createReferenceAdjustment(
                                sessionId = detail.session.id,
                                referenceLevelDbSpl = level,
                                referenceMethod = method,
                                notes = notes,
                            ) ?: "Measurement service is unavailable."
                            activeReferenceAdjustment = measurementService?.activeReferenceAdjustment()
                            error
                        },
                        onClearReferenceAdjustment = {
                            measurementService?.clearReferenceAdjustment()
                            activeReferenceAdjustment = null
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
    activeReferenceAdjustment: ReferenceAdjustment?,
    sessionLabel: String,
    onLabelChange: (String) -> Unit,
    frequencyWeighting: FrequencyWeighting,
    onFrequencyWeightingChange: (FrequencyWeighting) -> Unit,
    agcRequest: AgcRequest,
    onAgcRequestChange: (AgcRequest) -> Unit,
    agcAvailable: Boolean,
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
    val effectiveWeighting = if (isActive) session?.frequencyWeighting ?: frequencyWeighting else frequencyWeighting
    val effectiveAgcRequest =
        if (isActive) session?.agcRequest ?: agcRequest else agcRequest

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Digital level · " + effectiveWeighting.name + " weighting", style = MaterialTheme.typography.titleLarge)
            Text(
                text = formatDb(current?.weightedRmsDbfs ?: current?.rmsDbfs),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text("Weighted RMS · dBFS")
            snapshot.levelStatistics?.let { statistics ->
                Text("Minimum: " + formatDb(statistics.minimumDbfs))
                Text("Maximum: " + formatDb(statistics.maximumDbfs))
                Text("Leq: " + formatDb(statistics.leqDbfs))
            }
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
                "Digital A/Z weighting only. Values are not calibrated SPL.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            activeReferenceAdjustment?.let { active ->
                val configuration =
                    if (
                        session != null &&
                        session.source.isNotBlank() &&
                        session.inputIdentity.isNotBlank() &&
                        session.sampleRateHz > 0
                    ) {
                        MeasurementInputConfiguration(
                            deviceModel = Build.MODEL,
                            inputIdentity = session.inputIdentity,
                            audioSource = session.source,
                            sampleRateHz = session.sampleRateHz,
                            sampleFormat = "PCM16_MONO",
                            frequencyWeighting = effectiveWeighting,
                        )
                    } else {
                        null
                    }
                val mismatches = configuration?.let(active::mismatches)
                Text(
                    when {
                        configuration == null ->
                            "Reference adjustment stored · compatibility will be checked after capture starts."
                        mismatches.isNullOrEmpty() ->
                            "Reference adjustment matches this capture configuration · SPL still disabled pending physical validation."
                        else ->
                            "Reference adjustment mismatch: " +
                                mismatches.joinToString(", ") { it.name.lowercase().replace('_', ' ') } +
                                " · SPL unavailable."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (configuration != null && mismatches.isNullOrEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                )
            }
        }
    }

    if (snapshot.history.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Live history", style = MaterialTheme.typography.titleMedium)
                Text("Weighting: " + (snapshot.activeSession?.frequencyWeighting?.name ?: "Z") + " · digital dBFS")
                LevelHistoryChart(snapshot.history, snapshot.activeSession?.frequencyWeighting ?: FrequencyWeighting.Z)
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

            Text("Frequency weighting", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FrequencyWeighting.entries.forEach { choice ->
                    if (effectiveWeighting == choice) {
                        Button(onClick = { onFrequencyWeightingChange(choice) }, enabled = !isActive) { Text(choice.name) }
                    } else {
                        TextButton(onClick = { onFrequencyWeightingChange(choice) }, enabled = !isActive) { Text(choice.name) }
                    }
                }
            }
            Text("Weighting is fixed from Start through Stop.", style = MaterialTheme.typography.bodySmall)
            Text(
                "Android AGC",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AgcRequest.entries.forEach { request ->
                    val supported =
                        request == AgcRequest.DEFAULT || agcAvailable
                    if (effectiveAgcRequest == request) {
                        Button(
                            onClick = { onAgcRequestChange(request) },
                            enabled = !isActive && supported,
                        ) {
                            Text(request.displayLabel())
                        }
                    } else {
                        TextButton(
                            onClick = { onAgcRequestChange(request) },
                            enabled = !isActive && supported,
                        ) {
                            Text(request.displayLabel())
                        }
                    }
                }
            }
            Text(
                when {
                    !agcAvailable ->
                        "Standard Android AGC is unavailable on this device; Off/On cannot be applied."
                    isActive ->
                        "AGC request is frozen for this active session: " +
                            effectiveAgcRequest.displayLabel()
                    else ->
                        "Choose before Start. Default observes without changing AGC; Off/On request a fixed Android effect state."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            if (isActive && !session?.processingState.isNullOrBlank()) {
                Text(
                    "Applied processing: " + session?.processingState.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

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
    activeReferenceAdjustment: ReferenceAdjustment?,
    onCreateReferenceAdjustment: (SavedSessionDetail, Double, String, String) -> String?,
    onClearReferenceAdjustment: () -> Unit,
) {
    if (selectedDetail != null) {
        SavedSessionDetailCard(
            detail = selectedDetail,
            onBack = onBack,
            onShare = onShare,
            activeReferenceAdjustment = activeReferenceAdjustment,
            onCreateReferenceAdjustment = onCreateReferenceAdjustment,
            onClearReferenceAdjustment = onClearReferenceAdjustment,
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
                LevelHistoryChart(snapshot.history, snapshot.activeSession?.frequencyWeighting ?: FrequencyWeighting.Z)
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
    activeReferenceAdjustment: ReferenceAdjustment?,
    onCreateReferenceAdjustment: (SavedSessionDetail, Double, String, String) -> String?,
    onClearReferenceAdjustment: () -> Unit,
) {
    val session = detail.session
    var referenceLevelText by remember(detail.session.id) { mutableStateOf("") }
    var referenceMethod by remember(detail.session.id) { mutableStateOf("") }
    var referenceNotes by remember(detail.session.id) { mutableStateOf("") }
    var referenceMessage by remember(detail.session.id) { mutableStateOf<String?>(null) }
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
                "AGC request: " +
                    (session.agcRequest?.displayLabel() ?: "Unknown (legacy session)"),
            )
            Text(
                "Processing: " + session.processingState,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
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
            Text("Weighting: " + session.frequencyWeighting.name + " · digital dBFS")
            val statistics = remember(detail) { MeterProjection.statistics(detail.frames, session.frequencyWeighting) }
            Text("Current: " + formatDb(statistics.currentDbfs))
            Text("Minimum: " + formatDb(statistics.minimumDbfs))
            Text("Maximum: " + formatDb(statistics.maximumDbfs))
            Text("Leq: " + formatDb(statistics.leqDbfs))
            LevelHistoryChart(detail.frames, session.frequencyWeighting)
            Text(
                "CSV schema 1 · validated Phase 1 digital measurement export.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Reference adjustment setup", style = MaterialTheme.typography.titleMedium)
            Text(
                "Use only a deliberate physical reference recording. Storing an adjustment does not enable SPL estimates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            activeReferenceAdjustment?.let { active ->
                Text(
                    "Active stored adjustment: " + active.frequencyWeighting.name +
                        " · correction " + String.format(Locale.US, "%+.2f dB", active.correctionDb),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Method: " + active.referenceMethod,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onClearReferenceAdjustment) {
                    Text("Clear active adjustment")
                }
            }
            OutlinedTextField(
                value = referenceLevelText,
                onValueChange = { referenceLevelText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Known reference level · dB SPL") },
                singleLine = true,
            )
            OutlinedTextField(
                value = referenceMethod,
                onValueChange = { referenceMethod = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reference method / equipment") },
                singleLine = true,
            )
            OutlinedTextField(
                value = referenceNotes,
                onValueChange = { referenceNotes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reference notes / geometry") },
            )
            Button(
                onClick = {
                    val level = referenceLevelText.toDoubleOrNull()
                    referenceMessage =
                        if (level == null) {
                            "Enter a numeric reference SPL."
                        } else {
                            onCreateReferenceAdjustment(
                                detail,
                                level,
                                referenceMethod,
                                referenceNotes,
                            ) ?: "Active reference adjustment stored. SPL remains disabled pending the physical reference gate."
                        }
                },
            ) {
                Text("Store as active reference")
            }
            referenceMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun LevelHistoryChart(frames: List<MeasurementFrame>, weighting: FrequencyWeighting) {
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
    // Preserve sample positions and missing/zero intervals: no false bridging.
    val values = displayFrames.map { MeterProjection.level(it, weighting) }
    if (values.count { it != null } < 2) {
        Text("Waiting for enough valid non-zero frames…")
        return
    }

    Canvas(
        modifier = Modifier.fillMaxWidth().height(180.dp),
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
            val first = pair.first
            val second = pair.second
            if (first != null && second != null) {
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * index / (values.size - 1).toFloat(), dbToY(first, size.height)),
                    end = Offset(size.width * (index + 1) / (values.size - 1).toFloat(), dbToY(second, size.height)),
                    strokeWidth = 3f,
                )
            }
        }
    }
}

private fun AgcRequest.displayLabel(): String = when (this) {
    AgcRequest.DEFAULT -> "Default"
    AgcRequest.FORCE_OFF -> "Off"
    AgcRequest.FORCE_ON -> "On"
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
