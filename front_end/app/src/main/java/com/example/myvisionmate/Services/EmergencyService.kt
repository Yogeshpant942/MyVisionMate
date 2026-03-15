package com.example.myvisionmate.Services

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telephony.SmsManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import kotlin.math.sqrt

class EmergencyService: Service() {

    companion object {
        private const val TAG = "EmergencyService"
        private const val CHANNEL_ID = "emergency_channel"
        private const val NOTIF_ID = 1001

        private const val FALL_THRESHOLD = 25.0

        private const val VOICE_TIMEOUT_MS = 10_000L

        private const val FALL_COOLDOWN_MS = 30_000L

        // ── CONFIGURE THESE ────────────────────────────────────────────────
        private const val GUARDIAN_PHONE = "+911234567890"
        private const val BACKEND_URL    = "https://your-backend.com/api/emergency"
        private const val AUTH_TOKEN     = "Bearer YOUR_JWT_TOKEN"
    }


    private lateinit var sensorManager: SensorManager
    private lateinit var tts: TextToSpeech
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false
    private var isHandlingFall = false
    private var lastFallTime = 0L
    private var timeoutJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var lastLocation: Location? = null


    @SuppressLint("ForegroundServiceType")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initTTS()
        fetchLastLocation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startFallDetection()
        Log.d(TAG, "EmergencyService started — fall detection active")
        return START_STICKY // restart automatically if killed
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(fallSensorListener)
        tts.shutdown()
        speechRecognizer?.destroy()
        serviceScope.coroutineContext[Job.Key]?.cancel()
    }
    override fun onBind(intent: Intent?): IBinder? = null


    private fun initTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
                tts.setSpeechRate(0.9f)   // slightly slower for clarity
                tts.setPitch(1.0f)
                isTtsReady = true
                Log.d(TAG, "TTS initialised")
            } else {
                Log.e(TAG, "TTS initialisation failed: $status")
            }
        }
    }

    private fun speak(message: String, utteranceId: String = "tts_msg", onDone: (() -> Unit)? = null) {
        if (!isTtsReady) return
        if (onDone != null) {
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId) onDone()
                }
                override fun onError(id: String?) { onDone() }
            })
        }
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun startFallDetection() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(
                fallSensorListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else {
            Log.e(TAG, "Accelerometer not available on this device")
        }
    }

    private val fallSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt((x * x + y * y + z * z).toDouble())

            if (magnitude > FALL_THRESHOLD) {
                val now = System.currentTimeMillis()
                if (!isHandlingFall && (now - lastFallTime) > FALL_COOLDOWN_MS) {
                    lastFallTime = now
                    onFallDetected()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // ── Emergency Flow ─────────────────────────────────────────────────────

    private fun onFallDetected() {
        isHandlingFall = true
        fetchLastLocation() // refresh location at time of fall
        Log.d(TAG, "Fall detected! Starting emergency dialog.")

        speak(
            message = "Fall detected. Are you okay? " +
                    "Say call to call your guardian, " +
                    "say message to send a message, " +
                    "or say cancel if you are okay.",
            utteranceId = "fall_prompt",
            onDone = { startListeningForResponse() }
        )
    }

    private fun startListeningForResponse() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available — falling back to auto-action")
            handleTimeout()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                VOICE_TIMEOUT_MS
            )
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                timeoutJob?.cancel()
                val heard = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.lowercase(Locale.getDefault()) ?: ""
                Log.d(TAG, "User said: $heard")
                handleUserResponse(heard)
            }

            override fun onError(error: Int) {
                Log.e(TAG, "Speech recognition error: $error")
                timeoutJob?.cancel()
                handleTimeout()
            }

            // Required overrides — no-ops
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(recognizerIntent)

        // Safety timeout — if recognition never fires onResults or onError
        timeoutJob = serviceScope.launch {
            delay(VOICE_TIMEOUT_MS + 2000)
            Log.d(TAG, "Safety timeout triggered")
            handleTimeout()
        }
    }


    private fun handleUserResponse(userSaid: String) {
        when {
            // User wants a phone call
            "call" in userSaid -> {
                speak("Calling your guardian now.") {
                    callGuardian()
                    sendFCMAlert()         // also notify via FCM
                    saveEmergencyLog()
                    resetFallState()
                }
            }

            "message" in userSaid || "sms" in userSaid || "text" in userSaid -> {
                speak("Sending emergency message to your guardian.") {
                    sendEmergencySMS()
                    sendFCMAlert()
                    saveEmergencyLog()
                    resetFallState()
                }
            }

            "cancel" in userSaid || "okay" in userSaid ||
                    "fine" in userSaid || "ok" in userSaid -> {
                speak("Okay, alert cancelled. Stay safe.") {
                    resetFallState()
                }
            }

            else -> {
                Log.d(TAG, "Unrecognised response: '$userSaid' — triggering timeout fallback")
                handleTimeout()
            }
        }
    }


    private fun handleTimeout() {
        if (!isHandlingFall) return
        speak(
            "No response detected. Calling and messaging your guardian for safety."
        ) {
            callGuardian()
            sendEmergencySMS()
            sendFCMAlert()
            saveEmergencyLog()
            resetFallState()
        }
    }


    private fun callGuardian() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "CALL_PHONE permission not granted")
            return
        }
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$GUARDIAN_PHONE")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(callIntent)
        Log.d(TAG, "Calling guardian: $GUARDIAN_PHONE")
    }

    // ── Send SMS ───────────────────────────────────────────────────────────

    private fun sendEmergencySMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SEND_SMS permission not granted")
            return
        }
        val locationText = lastLocation?.let {
            "https://maps.google.com/?q=${it.latitude},${it.longitude}"
        } ?: "Location unavailable"

        val message = "EMERGENCY: Your person may have fallen and needs help.\n" +
                "Location: $locationText\n" +
                "— Sent by VisionMate"

        try {
            SmsManager.getDefault().sendTextMessage(
                GUARDIAN_PHONE, null, message, null, null
            )
            Log.d(TAG, "SMS sent to $GUARDIAN_PHONE")
        } catch (e: Exception) {
            Log.e(TAG, "SMS failed: ${e.message}")
        }
    }

    // ── Send FCM Push to Guardian ──────────────────────────────────────────

    private fun sendFCMAlert() {
        val locationText = lastLocation?.let {
            "${it.latitude},${it.longitude}"
        } ?: "unknown"

        serviceScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("type", "fall_alert")
                    put("location", locationText)
                    put("timestamp", System.currentTimeMillis())
                    put("message", "Emergency: Your person may have fallen.")
                }

                val body = json.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(BACKEND_URL)
                    .addHeader("Authorization", AUTH_TOKEN)
                    .post(body)
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                Log.d(TAG, "FCM alert sent — response: ${response.code}")
            } catch (e: Exception) {
                Log.e(TAG, "FCM alert failed: ${e.message}")
            }
        }
    }

    // ── Save Emergency Log to Backend ──────────────────────────────────────

    private fun saveEmergencyLog() {
        val locationText = lastLocation?.let {
            "${it.latitude},${it.longitude}"
        } ?: "unknown"

        serviceScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("event", "fall_detected")
                    put("location", locationText)
                    put("timestamp", System.currentTimeMillis())
                }

                val body = json.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BACKEND_URL/log")
                    .addHeader("Authorization", AUTH_TOKEN)
                    .post(body)
                    .build()

                OkHttpClient().newCall(request).execute()
                Log.d(TAG, "Emergency log saved")
            } catch (e: Exception) {
                Log.e(TAG, "Log save failed: ${e.message}")
            }
        }
    }

    // ── GPS Location ───────────────────────────────────────────────────────

    private fun fetchLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted")
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                lastLocation = location
                Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
            }
        }
    }



    private fun resetFallState() {
        isHandlingFall = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        timeoutJob?.cancel()
        Log.d(TAG, "Fall state reset — monitoring resumed")
    }

    // ── Foreground Notif ication ────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "VisionMate Emergency Monitor",
            NotificationManager.IMPORTANCE_LOW   // silent — doesn't interrupt user
        ).apply {
            description = "Monitors for falls in the background"
        }
        val manager = Context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VisionMate")
            .setContentText("Fall detection is active")
            .setSmallIcon(R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true)   // cannot be swiped away
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}