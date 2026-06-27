/*
 * Mushotoku — a privacy-focused, offline productivity app.
 * Copyright (C) 2026 Tom Frischmuth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mushotoku.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.*
import com.mushotoku.app.R
import com.mushotoku.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MeditationService : Service() {

    companion object {
        private val _state = MutableStateFlow(MeditationState())
        val state = _state.asStateFlow()

        const val ACTION_START  = "com.mushotoku.app.meditation.START"
        const val ACTION_STOP   = "com.mushotoku.app.meditation.STOP"
        const val ACTION_PAUSE  = "com.mushotoku.app.meditation.PAUSE"
        const val ACTION_RESUME = "com.mushotoku.app.meditation.RESUME"

        const val EXTRA_DURATION = "duration_minutes"
        const val EXTRA_BELL_INTERVAL = "bell_interval_minutes"
        const val EXTRA_SOUND = "bell_sound"

        const val SOUND_BOWL  = "BOWL"
        const val SOUND_WARM  = "WARM"
        const val SOUND_KEISU = "KEISU"
    }

    data class MeditationState(
        val isRunning: Boolean = false,
        val isPaused: Boolean = false,
        val totalMs: Long = 0L,
        val remainingMs: Long = 0L,
        val bellIntervalMinutes: Int = 0,
        val bellRinging: Boolean = false
    )

    private val CHANNEL_ID = "meditation_service_channel"
    private val NOTIF_ID   = 42

    private var countDownTimer: CountDownTimer? = null
    private var totalMs: Long = 0L
    private var remainingMs: Long = 0L
    private var bellIntervalMs: Long = 0L
    private var lastBellAtElapsedMs: Long = 0L
    private var bellIntervalMinutes: Int = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private var sessionEnded = false

    private var singleStrikeRes: Int = R.raw.singing_bowl
    private var doubleStrikeRes: Int = R.raw.singing_bowl_two

    private val INTERVAL_BELL_VOLUME = 0.5f

    private val bellPlayers = java.util.concurrent.CopyOnWriteArrayList<MediaPlayer>()

    private val bellAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durationMin  = intent.getIntExtra(EXTRA_DURATION, 10)
                bellIntervalMinutes = intent.getIntExtra(EXTRA_BELL_INTERVAL, 0)
                when (intent.getStringExtra(EXTRA_SOUND)) {
                    SOUND_KEISU -> { singleStrikeRes = R.raw.keisu;        doubleStrikeRes = R.raw.keisu_two }
                    SOUND_WARM  -> { singleStrikeRes = R.raw.bowl_warm;    doubleStrikeRes = R.raw.bowl_warm_two }
                    else        -> { singleStrikeRes = R.raw.singing_bowl; doubleStrikeRes = R.raw.singing_bowl_two }
                }

                totalMs           = durationMin * 60_000L
                remainingMs       = totalMs
                bellIntervalMs    = bellIntervalMinutes * 60_000L
                lastBellAtElapsedMs = 0L
                sessionEnded = false

                acquireWakeLock(totalMs + 30_000L)
                startForeground(NOTIF_ID, buildNotification(remainingMs), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                playOpeningBell()
                startTimer()
                updateState()
            }
            ACTION_STOP   -> stopSession()
            ACTION_PAUSE  -> pauseTimer()
            ACTION_RESUME -> {
                startTimer()
                _state.value = _state.value.copy(isRunning = true, isPaused = false)
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remainingMs, 500L) {
            override fun onTick(ms: Long) {
                remainingMs = ms
                val elapsed = totalMs - ms
                if (bellIntervalMs > 0 && elapsed - lastBellAtElapsedMs >= bellIntervalMs) {
                    lastBellAtElapsedMs += bellIntervalMs
                    playIntervalBell()
                }
                _state.value = _state.value.copy(remainingMs = ms)
                updateNotification(ms)
            }
            override fun onFinish() {
                remainingMs = 0L
                _state.value = _state.value.copy(isRunning = false, remainingMs = 0L)
                playClosingBellThenStop()
            }
        }.start()
        _state.value = _state.value.copy(isRunning = true, isPaused = false)
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        _state.value = _state.value.copy(isRunning = false, isPaused = true)
    }

    private fun stopSession() {
        if (sessionEnded) return
        sessionEnded = true
        countDownTimer?.cancel()
        val elapsedMinutes = ((totalMs - remainingMs) / 60_000L).toInt()
        if (elapsedMinutes > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.getInstance(applicationContext).appSettingsDao().addMeditatedMinutes(elapsedMinutes)
            }
        }
        stopAllBells()
        _state.value = MeditationState()
        wakeLock?.runCatching { if (isHeld) release() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateState() {
        _state.value = MeditationState(
            isRunning = true,
            totalMs   = totalMs,
            remainingMs = remainingMs,
            bellIntervalMinutes = bellIntervalMinutes
        )
    }

    private fun playOpeningBell() {
        strikeBowl(doubleStrikeRes, volume = 1f)
    }

    private fun playIntervalBell() {
        strikeBowl(singleStrikeRes, volume = INTERVAL_BELL_VOLUME)
    }

    private fun playClosingBellThenStop() {
        strikeBowl(doubleStrikeRes, volume = 1f, onComplete = { stopSession() })
    }

    private fun strikeBowl(resId: Int, volume: Float, onComplete: (() -> Unit)? = null) {
        val mp = runCatching {
            MediaPlayer.create(this, resId, bellAttributes, 0)
        }.getOrNull()
        if (mp == null) { onComplete?.invoke(); return }

        mp.runCatching { setVolume(volume, volume) }
        bellPlayers.add(mp)
        _state.value = _state.value.copy(bellRinging = true)
        mp.setOnCompletionListener { player ->
            bellPlayers.remove(player)
            player.runCatching { release() }
            if (bellPlayers.isEmpty()) _state.value = _state.value.copy(bellRinging = false)
            onComplete?.invoke()
        }
        mp.runCatching { start() }
    }

    private fun stopAllBells() {
        bellPlayers.forEach { it.runCatching { if (isPlaying) stop(); release() } }
        bellPlayers.clear()
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Meditation", NotificationManager.IMPORTANCE_LOW)
            .apply { setSound(null, null) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(ms: Long): Notification {
        val min = (ms / 60_000).toInt()
        val sec = ((ms % 60_000) / 1000).toInt()
        val title = getString(R.string.meditation_title)
        val body  = getString(R.string.meditation_notif_remaining, min, sec)
        val stop  = getString(R.string.meditation_notif_stop)
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, MeditationService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pi)
            .addAction(Notification.Action.Builder(null, stop, stopPi).build())
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(ms: Long) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(ms))
    }

    private fun acquireWakeLock(ms: Long) {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mushotoku:meditation")
            .also { it.acquire(ms) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        countDownTimer?.cancel()
        stopAllBells()
        wakeLock?.runCatching { if (isHeld) release() }
        super.onDestroy()
    }
}
