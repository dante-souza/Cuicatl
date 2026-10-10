// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
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
import androidx.compose.material3.AlertDialog
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
import io.github.dante_souza.cuicatl.domain.RootlessCalibrationEvidenceState
import io.github.dante_souza.cuicatl.domain.RootlessCalibrationProfile
import io.github.dante_souza.cuicatl.domain.RootlessCalibrationStatus
import io.github.dante_souza.cuicatl.domain.RootlessReferenceMethod
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

private data class CalibrationDraftInput(
    val method: RootlessReferenceMethod,
    val equipmentDescription: String,
    val equipmentIdentifier: String,
    val referenceLevelDbSpl: Double,
    val referenceFrequencyHz: Double?,
    val referenceUncertaintyDb: Double?,
    val geometry: String,
    val environmentNotes: String,
    val procedureNotes: String,
)

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
    var activeCalibrationProfile by remember { mutableStateOf<RootlessCalibrationProfile?>(null) }
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
            activeCalibrationProfile = measurementService.activeRootlessCalibrationProfile()
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
                text = "Phase 2 · digital A/Z meter · rootless calibration gated",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )

            when (page) {
                AnalyzerPage.METER -> {
                    MeterPage(
                        snapshot = snapshot,
                        activeCalibrationProfile = activeCalibrationProfile,
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
                        activeCalibrationProfile = activeCalibrationProfile,
                        onCreateCalibrationDraft = { detail, input ->
                            val service = measurementService
                            val error =
                                if (service == null) {
                                    "Measurement service is unavailable."
                                } else {
                                    service.createRootlessCalibrationDraft(
                                        sessionId = detail.session.id,
                                        method = input.method,
                                        equipmentDescription = input.equipmentDescription,
                                        equipmentIdentifier = input.equipmentIdentifier,
                                        referenceLevelDbSpl = input.referenceLevelDbSpl,
                                        referenceFrequencyHz = input.referenceFrequencyHz,
                                        referenceUncertaintyDb = input.referenceUncertaintyDb,
                                        geometry = input.geometry,
                                        environmentNotes = input.environmentNotes,
                                        procedureNotes = input.procedureNotes,
                                    )
                                }
                            activeCalibrationProfile =
                                service?.activeRootlessCalibrationProfile()
                            error
                        },
                        onVerifyCalibration = { detail, allowedDrift, notes ->
                            val service = measurementService
                            val error =
                                if (service == null) {
                                    "Measurement service is unavailable."
                                } else {
                                    service.verifyActiveRootlessCalibration(
                                        verificationSessionId = detail.session.id,
                                        maximumAllowedDriftDb = allowedDrift,
                                        notes = notes,
                                    )
                                }
                            activeCalibrationProfile =
                                service?.activeRootlessCalibrationProfile()
                            error
                        },
                        onClearCalibration = {
                            measurementService?.clearReferenceAdjustment()
                            activeCalibrationProfile = null
                        },
                        onDeleteSession = { detail ->
                            val deleted = measurementService?.deleteSavedSession(detail.session.id) == true
                            if (deleted) {
                                selectedDetail = null
                                selectedSessionId = null
                                savedSessions = measurementService?.savedSessions().orEmpty()
                                activeCalibrationProfile =
                                    measurementService?.activeRootlessCalibrationProfile()
                            }
                            deleted
                        },
                        onSanitizeSavedSessions = {
                            val deleted = measurementService?.sanitizeSavedSessions() ?: 0
                            selectedDetail = null
                            selectedSessionId = null
                            savedSessions = measurementService?.savedSessions().orEmpty()
                            activeCalibrationProfile =
                                measurementService?.activeRootlessCalibrationProfile()
                            deleted
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
    activeCalibrationProfile: RootlessCalibrationProfile?,
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
            val calibrationSnapshot = session?.calibrationSnapshot
            when {
                calibrationSnapshot != null -> {
                    Text(
                        when (calibrationSnapshot.evidenceState) {
                            RootlessCalibrationEvidenceState.UNCALIBRATED ->
                                "Session calibration snapshot: uncalibrated · profile " +
                                    calibrationSnapshot.profileId + " v" +
                                    calibrationSnapshot.profileVersion + "."
                            RootlessCalibrationEvidenceState.PROFILE_MISMATCH ->
                                "Session calibration snapshot: profile mismatch · SPL unavailable."
                            RootlessCalibrationEvidenceState.REFERENCE_ADJUSTED_ESTIMATE ->
                                "Validated rootless profile matched at Start · " +
                                    "SPL display remains disabled pending Phase 2 activation."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (
                                calibrationSnapshot.evidenceState ==
                                RootlessCalibrationEvidenceState.REFERENCE_ADJUSTED_ESTIMATE
                            ) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                    )
                }
                activeCalibrationProfile != null -> {
                    Text(
                        "Active calibration profile: " +
                            activeCalibrationProfile.status.name.lowercase() +
                            " · compatibility is frozen when capture starts. " +
                            "SPL display remains disabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
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
    activeCalibrationProfile: RootlessCalibrationProfile?,
    onCreateCalibrationDraft: (SavedSessionDetail, CalibrationDraftInput) -> String?,
    onVerifyCalibration: (SavedSessionDetail, Double, String) -> String?,
    onClearCalibration: () -> Unit,
    onDeleteSession: (SavedSessionDetail) -> Boolean,
    onSanitizeSavedSessions: () -> Int,
) {
    var showSanitizeConfirmation by remember { mutableStateOf(false) }
    var sanitizeMessage by remember { mutableStateOf<String?>(null) }

    if (showSanitizeConfirmation) {
        AlertDialog(
            onDismissRequest = { showSanitizeConfirmation = false },
            title = { Text("Sanitize saved sessions?") },
            text = {
                Text(
                    "This removes all saved Cuicatl session records, cached CSV exports, and rootless calibration profiles. " +
                        "An active recording is not deleted. Copies already shared or backed up are not affected. " +
                        "This is logical deletion, not guaranteed forensic secure erasure.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val deleted = onSanitizeSavedSessions()
                        sanitizeMessage = "Deleted " + deleted + " saved session(s)."
                        showSanitizeConfirmation = false
                    },
                ) {
                    Text("Delete all local sessions", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSanitizeConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (selectedDetail != null) {
        SavedSessionDetailCard(
            detail = selectedDetail,
            onBack = onBack,
            onShare = onShare,
            activeCalibrationProfile = activeCalibrationProfile,
            onCreateCalibrationDraft = onCreateCalibrationDraft,
            onVerifyCalibration = onVerifyCalibration,
            onClearCalibration = onClearCalibration,
            onDeleteSession = onDeleteSession,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Saved sessions", style = MaterialTheme.typography.titleLarge)
                if (savedSessions.isNotEmpty()) {
                    TextButton(onClick = { showSanitizeConfirmation = true }) {
                        Text("Sanitize", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            sanitizeMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            if (savedSessions.isEmpty()) {
                Text(
                    "No saved sessions yet.",
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
    activeCalibrationProfile: RootlessCalibrationProfile?,
    onCreateCalibrationDraft: (SavedSessionDetail, CalibrationDraftInput) -> String?,
    onVerifyCalibration: (SavedSessionDetail, Double, String) -> String?,
    onClearCalibration: () -> Unit,
    onDeleteSession: (SavedSessionDetail) -> Boolean,
) {
    val session = detail.session
    var showDeleteConfirmation by remember(detail.session.id) { mutableStateOf(false) }
    var deleteMessage by remember(detail.session.id) { mutableStateOf<String?>(null) }
    var referenceMethod by remember(detail.session.id) {
        mutableStateOf(RootlessReferenceMethod.REFERENCE_SOUND_LEVEL_METER)
    }
    var equipmentDescription by remember(detail.session.id) { mutableStateOf("") }
    var equipmentIdentifier by remember(detail.session.id) { mutableStateOf("") }
    var referenceLevelText by remember(detail.session.id) { mutableStateOf("") }
    var referenceFrequencyText by remember(detail.session.id) { mutableStateOf("") }
    var referenceUncertaintyText by remember(detail.session.id) { mutableStateOf("") }
    var referenceGeometry by remember(detail.session.id) { mutableStateOf("") }
    var environmentNotes by remember(detail.session.id) { mutableStateOf("") }
    var procedureNotes by remember(detail.session.id) { mutableStateOf("") }
    var calibrationMessage by remember(detail.session.id) { mutableStateOf<String?>(null) }
    var allowedDriftText by remember(detail.session.id) { mutableStateOf("") }
    var verificationNotes by remember(detail.session.id) { mutableStateOf("") }
    val clippedFrames = detail.frames.count { it.clippedSampleCount > 0 }
    val clippedSamples = detail.frames.sumOf { it.clippedSampleCount.toLong() }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete this local session?") },
            text = {
                Text(
                    "This removes the app-private measurement record and any cached CSV created by Cuicatl. " +
                        "Copies already shared or saved elsewhere are not affected. " +
                        "If this session is the source of calibration profiles, profiles derived from it are removed.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val deleted = onDeleteSession(detail)
                        if (!deleted) {
                            deleteMessage = "The session could not be deleted."
                        }
                        showDeleteConfirmation = false
                    },
                ) {
                    Text("Delete local session", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }

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
            TextButton(onClick = { showDeleteConfirmation = true }) {
                Text("Delete local session", color = MaterialTheme.colorScheme.error)
            }
            deleteMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
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

            session.calibrationSnapshot?.let { snapshot ->
                Spacer(modifier = Modifier.height(8.dp))
                Text("Session calibration snapshot", style = MaterialTheme.typography.titleMedium)
                Text(
                    snapshot.evidenceState.name.lowercase().replace('_', ' ') +
                        " · " + snapshot.profileId + " v" + snapshot.profileVersion,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Reference: " +
                        String.format(Locale.US, "%.2f dB SPL", snapshot.referenceLevelDbSpl) +
                        " · correction " +
                        String.format(Locale.US, "%+.2f dB", snapshot.referenceCorrectionDb),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Rootless calibration", style = MaterialTheme.typography.titleMedium)
            Text(
                "Use only deliberate measurements made against a real physical reference. " +
                    "Creating or validating a profile still does not enable SPL display in this phase.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )

            activeCalibrationProfile?.let { profile ->
                Text(
                    "Active profile: " + profile.status.name +
                        " · " + profile.id + " v" + profile.version,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Method: " + profile.procedure.method.displayLabel(),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Equipment/source: " + profile.procedure.equipmentDescription,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Reference: " +
                        String.format(
                            Locale.US,
                            "%.2f dB SPL",
                            profile.adjustment.referenceLevelDbSpl,
                        ) +
                        " · measured " +
                        String.format(
                            Locale.US,
                            "%.2f dBFS",
                            profile.adjustment.measuredReferenceLevelDbfs,
                        ) +
                        " · correction " +
                        String.format(Locale.US, "%+.2f dB", profile.adjustment.correctionDb),
                    style = MaterialTheme.typography.bodySmall,
                )
                profile.verification?.let { verification ->
                    Text(
                        "Verification drift: " +
                            String.format(Locale.US, "%.2f dB", verification.observedDriftDb) +
                            " · allowed " +
                            String.format(Locale.US, "%.2f dB", verification.maximumAllowedDriftDb) +
                            " · " + if (verification.passes) "PASS" else "FAIL",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (verification.passes) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                    )
                }

                if (profile.sourceSessionId == session.id) {
                    Text(
                        "This session is reference observation #1 for the active profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else if (profile.status == RootlessCalibrationStatus.DRAFT) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Verification observation #2", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Use this only if this session was recorded under the same physical reference, geometry and conditions as observation #1.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    OutlinedTextField(
                        value = allowedDriftText,
                        onValueChange = { allowedDriftText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Maximum allowed drift · dB") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = verificationNotes,
                        onValueChange = { verificationNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Verification notes") },
                    )
                    Button(
                        onClick = {
                            val allowed = allowedDriftText.toDoubleOrNull()
                            calibrationMessage =
                                if (allowed == null || allowed < 0.0) {
                                    "Enter a non-negative maximum allowed drift."
                                } else {
                                    onVerifyCalibration(
                                        detail,
                                        allowed,
                                        verificationNotes,
                                    ) ?: "Calibration profile validated. SPL display remains disabled pending the remaining Phase 2 activation gates."
                                }
                        },
                    ) {
                        Text("Use this session to verify")
                    }
                } else if (profile.status == RootlessCalibrationStatus.VALIDATED) {
                    Text(
                        "The active profile is validated. SPL display remains disabled until the remaining Phase 2 activation gates are complete.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                TextButton(onClick = onClearCalibration) {
                    Text("Clear active calibration")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Create calibration draft from this session",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "This saved session becomes reference observation #1. It must be at least 10 s, unclipped, and recorded while the physical reference was actually present.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )

            Text("Reference method", fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                RootlessReferenceMethod.entries.forEach { method ->
                    if (referenceMethod == method) {
                        Button(onClick = { referenceMethod = method }) {
                            Text(method.displayLabel())
                        }
                    } else {
                        TextButton(onClick = { referenceMethod = method }) {
                            Text(method.displayLabel())
                        }
                    }
                }
            }

            OutlinedTextField(
                value = equipmentDescription,
                onValueChange = { equipmentDescription = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reference equipment / source") },
                singleLine = true,
            )
            OutlinedTextField(
                value = equipmentIdentifier,
                onValueChange = { equipmentIdentifier = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Equipment identifier / serial · optional") },
                singleLine = true,
            )
            OutlinedTextField(
                value = referenceLevelText,
                onValueChange = { referenceLevelText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Known reference level · dB SPL") },
                singleLine = true,
            )
            OutlinedTextField(
                value = referenceFrequencyText,
                onValueChange = { referenceFrequencyText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reference frequency · Hz · optional") },
                singleLine = true,
            )
            OutlinedTextField(
                value = referenceUncertaintyText,
                onValueChange = { referenceUncertaintyText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reference uncertainty · dB · optional") },
                singleLine = true,
            )
            OutlinedTextField(
                value = referenceGeometry,
                onValueChange = { referenceGeometry = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Geometry / coupling") },
            )
            OutlinedTextField(
                value = environmentNotes,
                onValueChange = { environmentNotes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Environment notes") },
            )
            OutlinedTextField(
                value = procedureNotes,
                onValueChange = { procedureNotes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Procedure notes") },
            )
            Button(
                onClick = {
                    val level = referenceLevelText.toDoubleOrNull()
                    val frequency =
                        referenceFrequencyText.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                    val uncertainty =
                        referenceUncertaintyText.takeIf { it.isNotBlank() }?.toDoubleOrNull()

                    calibrationMessage =
                        when {
                            level == null ->
                                "Enter a numeric reference SPL."
                            equipmentDescription.isBlank() ->
                                "Enter the physical reference equipment or source."
                            referenceGeometry.isBlank() ->
                                "Document the geometry or coupling."
                            referenceFrequencyText.isNotBlank() && frequency == null ->
                                "Reference frequency must be numeric when provided."
                            referenceUncertaintyText.isNotBlank() &&
                                (uncertainty == null || uncertainty < 0.0) ->
                                "Reference uncertainty must be a non-negative number."
                            else ->
                                onCreateCalibrationDraft(
                                    detail,
                                    CalibrationDraftInput(
                                        method = referenceMethod,
                                        equipmentDescription = equipmentDescription,
                                        equipmentIdentifier = equipmentIdentifier,
                                        referenceLevelDbSpl = level,
                                        referenceFrequencyHz = frequency,
                                        referenceUncertaintyDb = uncertainty,
                                        geometry = referenceGeometry,
                                        environmentNotes = environmentNotes,
                                        procedureNotes = procedureNotes,
                                    ),
                                ) ?: "Calibration draft stored as observation #1. Record a second session under the same reference to verify it. SPL remains disabled."
                        }
                },
            ) {
                Text("Store calibration draft")
            }
            calibrationMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (message.startsWith("Verification failed")) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
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

private fun RootlessReferenceMethod.displayLabel(): String = when (this) {
    RootlessReferenceMethod.ACOUSTIC_CALIBRATOR -> "Acoustic calibrator"
    RootlessReferenceMethod.REFERENCE_SOUND_LEVEL_METER -> "Reference sound-level meter"
    RootlessReferenceMethod.DOCUMENTED_COMPARISON_SOURCE -> "Documented comparison source"
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
