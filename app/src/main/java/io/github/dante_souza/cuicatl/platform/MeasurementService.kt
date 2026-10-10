// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.Handler
import android.os.SystemClock
import io.github.dante_souza.cuicatl.MainActivity
import io.github.dante_souza.cuicatl.R
import io.github.dante_souza.cuicatl.domain.FrequencyWeighting
import io.github.dante_souza.cuicatl.domain.LevelStatisticsAccumulator
import io.github.dante_souza.cuicatl.domain.AgcRequest
import io.github.dante_souza.cuicatl.domain.MeasurementSession
import io.github.dante_souza.cuicatl.domain.MeasurementInputConfiguration
import io.github.dante_souza.cuicatl.domain.ReferenceAdjustment
import io.github.dante_souza.cuicatl.domain.ReferenceAdjustmentFactory
import io.github.dante_souza.cuicatl.domain.RootlessCalibrationProfile
import io.github.dante_souza.cuicatl.domain.SavedSessionDetail
import io.github.dante_souza.cuicatl.domain.SessionCommandPolicy
import io.github.dante_souza.cuicatl.domain.SessionOutcome
import io.github.dante_souza.cuicatl.domain.SessionRuntimeSnapshot
import io.github.dante_souza.cuicatl.domain.SessionState
import io.github.dante_souza.cuicatl.export.MeasurementCsvExporter
import io.github.dante_souza.cuicatl.persistence.SessionRepository
import io.github.dante_souza.cuicatl.persistence.ReferenceAdjustmentRepository
import io.github.dante_souza.cuicatl.persistence.RootlessCalibrationRepository
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

class MeasurementService : Service() {
    private val binder = LocalBinder()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners =
        CopyOnWriteArraySet<(SessionRuntimeSnapshot) -> Unit>()

    private lateinit var repository: SessionRepository
    private lateinit var exporter: MeasurementCsvExporter
    private lateinit var referenceRepository: ReferenceAdjustmentRepository
    private lateinit var calibrationRepository: RootlessCalibrationRepository

    @Volatile
    private var runtimeSnapshot = SessionRuntimeSnapshot()

    @Volatile
    private var activeSession: MeasurementSession? = null

    @Volatile
    private var writer: SessionRepository.ActiveWriter? = null

    @Volatile
    private var capture: AndroidSessionCapture? = null

    @Volatile
    private var activeStartedElapsedRealtime = 0L

    @Volatile
    private var levelAccumulator = LevelStatisticsAccumulator()

    private var clippedFrameCount = 0L

    @Volatile
    private var clippedSampleCount = 0L

    override fun onCreate() {
        super.onCreate()
        repository = SessionRepository(this)
        exporter = MeasurementCsvExporter(this)
        referenceRepository = ReferenceAdjustmentRepository(this)
        calibrationRepository = RootlessCalibrationRepository(this)
        calibrationRepository.migrateLegacyActiveAdjustment(referenceRepository)
        val recovered = repository.recoverInterruptedSessions()
        if (recovered > 0) {
            runtimeSnapshot = SessionRuntimeSnapshot(
                message = "Recovered " + recovered +
                    " interrupted session record(s). Capture did not continue while the process was absent.",
            )
        }
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
                val agcRequest = intent.getStringExtra(EXTRA_AGC_REQUEST)
                    ?.let { runCatching { AgcRequest.valueOf(it) }.getOrNull() }
                    ?: AgcRequest.DEFAULT
                val weighting = intent.getStringExtra(EXTRA_FREQUENCY_WEIGHTING)
                    ?.let { runCatching { FrequencyWeighting.valueOf(it) }.getOrNull() }
                    ?: FrequencyWeighting.Z
                startMeasurement(label, agcRequest, weighting)
            }

            ACTION_STOP -> stopMeasurement()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        capture?.close()
        writer?.close()
        capture = null
        writer = null
        super.onDestroy()
    }

    fun snapshot(): SessionRuntimeSnapshot = runtimeSnapshot

    fun addListener(listener: (SessionRuntimeSnapshot) -> Unit) {
        listeners += listener
        listener(runtimeSnapshot)
    }

    fun removeListener(listener: (SessionRuntimeSnapshot) -> Unit) {
        listeners -= listener
    }

    fun savedSessions(): List<MeasurementSession> =
        repository.listSavedSessions()

    fun loadSavedSession(sessionId: String): SavedSessionDetail? =
        repository.loadSession(sessionId)

    fun exportSavedSession(sessionId: String): File? =
        repository.loadSession(sessionId)?.let(exporter::export)

    fun activeReferenceAdjustment(): ReferenceAdjustment? =
        calibrationRepository.loadActive()?.adjustment
            ?: referenceRepository.loadActive()

    fun activeRootlessCalibrationProfile(): RootlessCalibrationProfile? =
        calibrationRepository.loadActive()

    fun createReferenceAdjustment(
        sessionId: String,
        referenceLevelDbSpl: Double,
        referenceMethod: String,
        notes: String,
    ): String? {
        val detail = repository.loadSession(sessionId) ?: return "Saved session could not be loaded."
        val candidate = ReferenceAdjustmentFactory.fromSavedSession(
            detail = detail,
            deviceModel = Build.MODEL,
            referenceLevelDbSpl = referenceLevelDbSpl,
            referenceMethod = referenceMethod,
            createdAtUtcEpochMillis = System.currentTimeMillis(),
            notes = notes,
        )
        val adjustment =
            candidate.adjustment
                ?: return candidate.rejectionReason
                    ?: "Reference adjustment rejected."

        return runCatching {
            calibrationRepository.saveDraftFromAdjustment(
                adjustment = adjustment,
                sourceSessionId = sessionId,
            )
            referenceRepository.clearActive()
            null
        }.getOrElse { error ->
            "Could not persist draft calibration profile: " +
                (error.message ?: error::class.java.simpleName)
        }
    }

    fun clearReferenceAdjustment() {
        calibrationRepository.clearActive()
        referenceRepository.clearActive()
    }

    fun deleteSavedSession(sessionId: String): Boolean {
        val deleted = repository.deleteSavedSession(sessionId)
        if (!deleted) return false

        exporter.deleteCachedExports(sessionId)
        calibrationRepository.deleteProfilesFromSourceSession(sessionId)
        val activeReference = referenceRepository.loadActive()
        if (activeReference?.id == "ref-" + sessionId) {
            referenceRepository.clearActive()
        }
        return true
    }

    fun sanitizeSavedSessions(): Int {
        val deleted = repository.deleteAllSavedSessions()
        exporter.clearCachedExports()
        calibrationRepository.clearAll()
        referenceRepository.clearActive()
        return deleted
    }

    private fun startMeasurement(
        label: String,
        agcRequest: AgcRequest,
        frequencyWeighting: FrequencyWeighting,
    ) {
        if (!SessionCommandPolicy.acceptsStart(runtimeSnapshot.status)) {
            return
        }

        val sessionId = UUID.randomUUID().toString()
        val startedAtUtcEpochMillis = System.currentTimeMillis()
        val timezoneOffset = ZoneId.systemDefault()
            .rules
            .getOffset(Instant.ofEpochMilli(startedAtUtcEpochMillis))
            .id
        val startingSession = MeasurementSession(
            id = sessionId,
            state = SessionState.STARTING,
            label = label.trim().take(MAX_LABEL_LENGTH),
            startedAtUtcEpochMillis = startedAtUtcEpochMillis,
            timezoneOffset = timezoneOffset,
            agcRequest = agcRequest,
            frequencyWeighting = frequencyWeighting,
        )
        activeSession = startingSession
        activeStartedElapsedRealtime = SystemClock.elapsedRealtime()
        levelAccumulator = LevelStatisticsAccumulator()
        clippedFrameCount = 0L
        clippedSampleCount = 0L
        publish(
            SessionRuntimeSnapshot(
                status = SessionRuntimeSnapshot.Status.STARTING,
                activeSession = startingSession,
                message = "Opening the fixed Phase 1 capture path…",
            ),
        )

        startForeground(
            NOTIFICATION_ID,
            buildNotification("Starting measurement"),
        )

        val sessionCapture = AndroidSessionCapture(
            context = this,
            sessionId = sessionId,
            agcRequest = agcRequest,
            frequencyWeighting = frequencyWeighting,
            onStarted = { configuration ->
                val inputConfiguration = MeasurementInputConfiguration(
                    deviceModel = Build.MODEL,
                    inputIdentity = configuration.inputIdentity,
                    audioSource = configuration.source,
                    sampleRateHz = configuration.sampleRateHz,
                    sampleFormat = SAMPLE_FORMAT,
                    frequencyWeighting = frequencyWeighting,
                )
                val calibrationSnapshot =
                    calibrationRepository.loadActive()
                        ?.sessionSnapshot(inputConfiguration)

                val runningSession = startingSession.copy(
                    state = SessionState.RUNNING,
                    source = configuration.source,
                    sampleRateHz = configuration.sampleRateHz,
                    inputIdentity = configuration.inputIdentity,
                    processingState = configuration.processingState,
                    calibrationSnapshot = calibrationSnapshot,
                )
                writer = repository.beginSession(runningSession)
                activeSession = runningSession
                publish(
                    SessionRuntimeSnapshot(
                        status = SessionRuntimeSnapshot.Status.RUNNING,
                        activeSession = runningSession,
                        message = "Measurement running. Values are digital dBFS, not SPL.",
                    ),
                )
                notifyForeground("Recording · " + runningSession.label.ifBlank { "unlabeled session" })
            },
            onFrame = { frame ->
                val activeWriter = writer ?: error("Session writer was not initialized")
                activeWriter.append(frame)
                levelAccumulator.addInterval(
                    frame.weightedMeanSquareFs ?: frame.meanSquareFs,
                    frame.durationMillis,
                )

                val elapsed = SystemClock.elapsedRealtime() - activeStartedElapsedRealtime
                val captured = frame.elapsedStartMillis + frame.durationMillis
                val updatedSession = (activeSession ?: startingSession).copy(
                    state = SessionState.RUNNING,
                    elapsedMillis = elapsed,
                    capturedMillis = captured,
                    frameCount = frame.sequence + 1,
                )
                activeSession = updatedSession

                if (frame.clippedSampleCount > 0) {
                    clippedFrameCount += 1
                    clippedSampleCount += frame.clippedSampleCount.toLong()
                }

                val prior = runtimeSnapshot.history
                val history = (prior + frame).takeLast(MAX_LIVE_HISTORY_FRAMES)
                publish(
                    SessionRuntimeSnapshot(
                        status = SessionRuntimeSnapshot.Status.RUNNING,
                        activeSession = updatedSession,
                        currentFrame = frame,
                        history = history,
                        levelStatistics = levelAccumulator.snapshot(),
                        clippedFrameCount = clippedFrameCount,
                        clippedSampleCount = clippedSampleCount,
                        message = "Measurement running. Values are digital dBFS, not SPL.",
                    ),
                )
            },
            onStopped = { elapsedMillis ->
                finalizeSession(
                    outcome = SessionOutcome.COMPLETED,
                    elapsedMillis = elapsedMillis,
                    interruptionReason = null,
                    message = "Session saved.",
                )
            },
            onFailure = { error, elapsedMillis ->
                finalizeSession(
                    outcome = SessionOutcome.INTERRUPTED,
                    elapsedMillis = elapsedMillis,
                    interruptionReason = error.message ?: error::class.java.simpleName,
                    message = "Capture interrupted: " +
                        (error.message ?: error::class.java.simpleName),
                )
            },
        )
        capture = sessionCapture
        sessionCapture.start()
    }

    private fun stopMeasurement() {
        val activeCapture = capture
        if (
            !SessionCommandPolicy.acceptsStop(
                status = runtimeSnapshot.status,
                hasActiveCapture = activeCapture != null,
            )
        ) {
            return
        }

        val current = activeSession
        publish(
            runtimeSnapshot.copy(
                status = SessionRuntimeSnapshot.Status.FINALIZING,
                activeSession = current?.copy(state = SessionState.FINALIZING),
                message = "Finalizing durable session data…",
            ),
        )
        notifyForeground("Finalizing measurement")
        activeCapture?.stop()
    }

    private fun finalizeSession(
        outcome: SessionOutcome,
        elapsedMillis: Long,
        interruptionReason: String?,
        message: String,
    ) {
        val finalSession = runCatching {
            writer?.finish(
                outcome = outcome,
                elapsedMillis = elapsedMillis,
                interruptionReason = interruptionReason,
            )
        }.getOrNull()

        writer = null
        capture = null
        activeSession = null

        publish(
            SessionRuntimeSnapshot(
                status =
                    if (outcome == SessionOutcome.COMPLETED) {
                        SessionRuntimeSnapshot.Status.READY
                    } else {
                        SessionRuntimeSnapshot.Status.FAILED
                    },
                activeSession = finalSession,
                message = message,
            ),
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publish(snapshot: SessionRuntimeSnapshot) {
        runtimeSnapshot = snapshot
        mainHandler.post {
            listeners.forEach { listener ->
                listener(snapshot)
            }
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Active measurements",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Cuicatl active microphone measurement sessions"
            },
        )
    }

    private fun notifyForeground(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = Intent(this, MeasurementService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Cuicatl")
            .setContentText(text)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Stop",
                    stopPendingIntent,
                ).build(),
            )
            .build()
    }

    inner class LocalBinder : Binder() {
        fun service(): MeasurementService = this@MeasurementService
    }

    companion object {
        const val ACTION_START = "io.github.dante_souza.cuicatl.action.START_MEASUREMENT"
        const val ACTION_STOP = "io.github.dante_souza.cuicatl.action.STOP_MEASUREMENT"
        const val EXTRA_LABEL = "session_label"
        const val EXTRA_AGC_REQUEST = "agc_request"
        const val EXTRA_FREQUENCY_WEIGHTING = "frequency_weighting"

        private const val CHANNEL_ID = "cuicatl_measurement"
        private const val NOTIFICATION_ID = 1001
        private const val MAX_LABEL_LENGTH = 120
        private const val SAMPLE_FORMAT = "PCM16_MONO"
        private const val MAX_LIVE_HISTORY_FRAMES = 600
    }
}
