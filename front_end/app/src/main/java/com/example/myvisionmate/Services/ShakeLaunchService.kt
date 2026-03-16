package com.example.myvisionmate.Services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.myvisionmate.MainActivity
import kotlin.math.sqrt

class ShakeLaunchService : Service(), SensorEventListener {
    companion object {
        private const val TAG = "ShakeLaunch"
        private const val CHANNEL_ID = "shake_launch_channel"
        private const val NOTIF_ID   = 2001

        private const val SHAKE_THRESHOLD = 10.0f
        private const val REQUIRED_SHAKE_DURATION_MS = 10_000L

        private const val SHAKE_GAP_TOLERANCE_MS = 2000L

        private const val LAUNCH_COOLDOWN_MS = 5_000L


        private val VIBRATE_MILESTONES_MS = listOf(5_000L)
    }


    private lateinit var sensorManager: SensorManager
    private lateinit var vibrator: Vibrator
    private val handler = Handler(Looper.getMainLooper())

    private var shakeStartTime    = 0L
    private var lastShakeTime     = 0L
    private var lastLaunchTime    = 0L
    private var isShaking         = false
    private val milestonesReached = mutableSetOf<Long>()


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startShakeDetection()
        Log.d(TAG, "ShakeLaunchService started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "ShakeLaunchService stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
    private fun startShakeDetection() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
            Log.d(TAG, "Accelerometer registered")
        } else {
            Log.e(TAG, "No accelerometer found on this device")
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        val now = System.currentTimeMillis()

        if (magnitude > SHAKE_THRESHOLD) {
            lastShakeTime = now

            if (!isShaking) {
                isShaking      = true
                shakeStartTime = now
                milestonesReached.clear()
                Log.d(TAG, "Shaking started")
            } else {
                val elapsed = now - shakeStartTime

                checkMilestones(elapsed)

                if (elapsed >= REQUIRED_SHAKE_DURATION_MS) {
                    val timeSinceLastLaunch = now - lastLaunchTime
                    if (timeSinceLastLaunch > LAUNCH_COOLDOWN_MS) {
                        lastLaunchTime = now
                        onShakeDurationReached()
                    }
                }
            }
        } else {
            if (isShaking) {
                val gapSinceLastShake = now - lastShakeTime
                if (gapSinceLastShake > SHAKE_GAP_TOLERANCE_MS) {
                    // Shaking stopped — reset the timer
                    Log.d(TAG, "Shaking stopped. Resetting timer.")
                    isShaking = false
                    shakeStartTime = 0L
                    milestonesReached.clear()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkMilestones(elapsed: Long) {
        for (milestone in VIBRATE_MILESTONES_MS) {
            if (elapsed >= milestone && !milestonesReached.contains(milestone)) {
                milestonesReached.add(milestone)
                when (milestone) {
                    5_000L  -> vibratePattern(longArrayOf(0, 100))
                    10_000L -> vibratePattern(longArrayOf(0, 100, 100, 100))
                }
                Log.d(TAG, "Milestone reached: ${milestone / 1000}s")
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun onShakeDurationReached() {

        Log.d(TAG, "10 seconds of shaking reached — launching VisionMate!")

        sensorManager.unregisterListener(this)

        vibratePattern(longArrayOf(0, 400))

        isShaking = false
        shakeStartTime = 0L
        lastLaunchTime = System.currentTimeMillis()
        milestonesReached.clear()

        handler.postDelayed({
            try {

                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }

                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                pendingIntent.send()

                Log.d(TAG, "PendingIntent launched MainActivity")

            } catch (e: Exception) {
                Log.e(TAG, "Launch failed: ${e.message}")
            }

            handler.postDelayed({
                startShakeDetection()
            }, 3000L)

        }, 300L)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibratePattern(pattern: LongArray) {
        vibrator.vibrate(
            VibrationEffect.createWaveform(pattern, -1)
        )
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "VisionMate Shake Launcher",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors shake gesture to open the app"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VisionMate")
            .setContentText("Shake detection is active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}