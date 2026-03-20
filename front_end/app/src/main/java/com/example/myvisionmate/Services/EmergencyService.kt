package com.example.myvisionmate.Services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.*
import android.location.Location
import android.net.Uri
import android.os.*
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telephony.SmsManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.myvisionmate.MainActivity
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.math.sqrt
class EmergencyService : Service() {
    companion object {
        private const val TAG = "EmergencyService"
        private const val CHANNEL_ID = "emergency_channel"
        private const val NOTIF_ID = 1001
        private const val FALL_THRESHOLD = 20.0
        private const val VOICE_TIMEOUT_MS = 10_000L
        private const val FALL_COOLDOWN_MS = 5_000L
        var GUARDIAN_PHONE:String = ""
    }
    private lateinit var sensorManager: SensorManager
    private lateinit var tts: TextToSpeech
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false
    private var isHandlingFall = false
    private var lastFallTime = 0L
    private var timeoutJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var lastLocation: Location? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initTTS()
        fetchLastLocation()
        val no_prefs = getSharedPreferences("visionmate", Context.MODE_PRIVATE)

        val numbers = no_prefs.getStringSet("guardian_no", emptySet())

        if (!numbers.isNullOrEmpty()) {
            GUARDIAN_PHONE = numbers.random()
        } else {
            Log.e(TAG, "No guardian numbers saved!")
            GUARDIAN_PHONE = "+919528659567"
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startFallDetection()
        Log.d(TAG, "EmergencyService started — fall detection active")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(fallSensorListener)
        tts.shutdown()
        speechRecognizer?.destroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    private fun initTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
                isTtsReady = true
            }
        }
    }

    private fun speak(message: String, onDone: (() -> Unit)? = null) {

        if (!isTtsReady) return

        if (onDone != null) {
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}

                override fun onDone(id: String?) {
                    onDone()
                }

                override fun onError(id: String?) {
                    onDone()
                }
            })
        }

        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "tts")
    }

    private fun startFallDetection() {

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager.registerListener(
                fallSensorListener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
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

                if (!isHandlingFall && now - lastFallTime > FALL_COOLDOWN_MS) {

                    lastFallTime = now
                    onFallDetected()
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    private fun onFallDetected() {

        isHandlingFall = true
        fetchLastLocation()

        speak(
            "Fall detected. Are you okay? Say call, message, or cancel."
        ) {
            Log.d(TAG, "TTS finished speaking. Starting voice recognition.")

            startListeningForResponse()
        }
        serviceScope.launch {
            delay(15000)
            if(isHandlingFall){
                Log.d(TAG,"Force resetting fall state")
                resetFallState()
            }
        }
    }


    private fun startListeningForResponse() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            handleTimeout()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onResults(results: Bundle?) {

                timeoutJob?.cancel()

                val heard = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.lowercase(Locale.getDefault()) ?: ""

                handleUserResponse(heard)
            }

            override fun onError(error: Int) {
                handleTimeout()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)

        timeoutJob = serviceScope.launch {
            delay(VOICE_TIMEOUT_MS)
            handleTimeout()
        }
    }


    private fun handleUserResponse(text: String) {

        when {
            "call" in text -> {
                speak("Calling guardian") {
                    callGuardian()
                    resetFallState()
                }
            }

            "message" in text -> {
                speak("Sending emergency message") {
                    sendEmergencySMS()
                    resetFallState()
                }
            }
            "cancel" in text || "okay" in text -> {
                speak("Okay alert cancelled") {
                    resetFallState()
                }
            }

            else -> handleTimeout()
        }
    }


    private fun handleTimeout() {
        if (!isHandlingFall) return
        speak("No response detected. Sending emergency alert") {
            callGuardian()
            sendEmergencySMS()
            resetFallState()
        }
    }


    private fun callGuardian() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(Intent.ACTION_CALL)

        intent.data = Uri.parse("tel:$GUARDIAN_PHONE")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        startActivity(intent)
    }


    private fun sendEmergencySMS() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val locationText = lastLocation?.let {
            "https://maps.google.com/?q=${it.latitude},${it.longitude}"
        } ?: "Location unavailable"

        val message =
            "EMERGENCY: Possible fall detected.\nLocation: $locationText\nSent by VisionMate"

        SmsManager.getDefault()
            .sendTextMessage(GUARDIAN_PHONE, null, message, null, null)
    }


    private fun fetchLastLocation() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener {

                if (it != null) {
                    lastLocation = it
                }
            }
    }


    private fun resetFallState() {

        isHandlingFall = false

        speechRecognizer?.destroy()
        speechRecognizer = null

        timeoutJob?.cancel()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {

        val channel = NotificationChannel(
            CHANNEL_ID,
            "VisionMate Emergency Monitor",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager =
            getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)
    }
    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VisionMate")
            .setContentText("Fall detection is active")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}