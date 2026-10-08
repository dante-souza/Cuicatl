// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.os.Handler
import android.os.SystemClock
import io.github.dante_souza.cuicatl.MainActivity
import io.github.dante_souza.cuicatl.R
import io.github.dante_souza.cuicatl.domain.MeasurementSession
import io.github.dante_souza.cuicatl.domain.SavedSessionDetail
import io.github.dante_souza.cuicatl.domain.SessionOutcome
import io.github.dante_souza.cuicatl.domain.SessionRuntimeSnapshot
import io.github.dante_souza.cuicatl.domain.SessionState
import io.github.dante_souza.cuicatl.export.MeasurementCsvExporter
import io.github.dante_souza.cuicatl.persistence.SessionRepository
import java.io.File
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

class MeasurementService : Service() {
    private val binder = LocalBinder()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners =
        CopyOnWriteArraySet<(SessionRuntimeSnapshot) -> Unit>()

    private lateinit var repository: SessionRepository
    private lateinit var exporter: MeasurementCsvExporter

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

    override fun onCreate() {
        super.onCreate()
        repository = SessionRepository(this)
        exporter = MeasurementCsvExporter(this)
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
                startMeasurement(label)
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

    private fun startMeasurement(label: String) {
        if (
            runtimeSnapshot.status == SessionRuntimeSnapshot.Status.RUNNING ||
            runtimeSnapshot.status == SessionRuntimeSnapshot.Status.STARTING ||
            runtimeSnapshot.status == SessionRuntimeSnapshot.Status.FINALIZING
        ) {
            return
        }

        val sessionId = UUID.randomUUID().toString()
        val startingSession = MeasurementSession(
            id = sessionId,
            state = SessionState.STARTING,
            label = label.trim().take(MAX_LABEL_LENGTH),
            startedAtUtcEpochMillis = System.currentTimeMillis(),
        )
        activeSession = startingSession
        activeStartedElapsedRealtime = SystemClock.elapsedRealtime()
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
            onStarted = { configuration ->
                val runningSession = startingSession.copy(
                    state = SessionState.RUNNING,
                    source = configuration.source,
                    sampleRateHz = configuration.sampleRateHz,
                    inputIdentity = configuration.inputIdentity,
                    processingState = configuration.processingState,
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

                val elapsed = SystemClock.elapsedRealtime() - activeStartedElapsedRealtime
                val captured = frame.elapsedStartMillis + frame.durationMillis
                val updatedSession = (activeSession ?: startingSession).copy(
                    state = SessionState.RUNNING,
                    elapsedMillis = elapsed,
                    capturedMillis = captured,
                    frameCount = frame.sequence + 1,
                )
                activeSession = updatedSession

                val prior = runtimeSnapshot.history
                val history = (prior + frame).takeLast(MAX_LIVE_HISTORY_FRAMES)
                publish(
                    SessionRuntimeSnapshot(
                        status = SessionRuntimeSnapshot.Status.RUNNING,
                        activeSession = updatedSession,
                        currentFrame = frame,
                        history = history,
                        message =
                            if (frame.clippedSampleCount > 0) {
                                "Clipping detected in the latest 100 ms frame."
                            } else {
                                "Measurement running. Values are digital dBFS, not SPL."
                            },
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
        val activeCapture = capture ?: return
        val current = activeSession
        publish(
            runtimeSnapshot.copy(
                status = SessionRuntimeSnapshot.Status.FINALIZING,
                activeSession = current?.copy(state = SessionState.FINALIZING),
                message = "Finalizing durable session data…",
            ),
        )
        notifyForeground("Finalizing measurement")
        activeCapture.stop()
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

        private const val CHANNEL_ID = "cuicatl_measurement"
        private const val NOTIFICATION_ID = 1001
        private const val MAX_LABEL_LENGTH = 120
        private const val MAX_LIVE_HISTORY_FRAMES = 600
    }
}
