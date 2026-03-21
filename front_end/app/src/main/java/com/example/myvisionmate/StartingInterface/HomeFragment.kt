package com.example.myvisionmate.StartingInterface
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.speech.*
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myvisionmate.R
import com.example.myvisionmate.databinding.FragmentHomeBinding
import com.google.android.gms.location.LocationServices
import java.util.*
class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false
    private var isListening = false
    private var userName = "yogesh"
    private val PERMISSION_REQUEST_CODE = 100
    private val LOCATION_PERMISSION_REQUEST_CODE = 101
    private val CALL_PHONE_PERMISSION_REQUEST = 102
    private val SMS_PERMISSION_REQUEST = 103

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentHomeBinding.inflate(inflater, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val prefs = requireContext().getSharedPreferences("visionmate", Context.MODE_PRIVATE)
        userName = prefs.getString("user_name", "User") ?: "User"

        initTTS()

        initializeSpeechRecognizer()
        setupMicButton()
        requestPermissions()

        binding.cardDescribe.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_describeSurroundingsFragment)
        }

        binding.cardReadText.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_readTextFragment)
        }

        binding.cardStartScanning.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_cameraFragment)
        }

        binding.cardSettings.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingFragment)
        }

        return binding.root
    }
    private fun initTTS() {
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {

                val result = tts.setLanguage(Locale.ENGLISH)

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e("TTS", "Language not supported")
                } else {
                    isTtsReady = true
                    tts.setSpeechRate(0.9f)
                    tts.setPitch(1.0f)
                    Log.d("TTS", "TTS Ready")
                }
            }
        }
    }
    private fun speakOut(message: String) {
        if (!isTtsReady) return

        tts.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "tts_id"
        )
    }

    private fun initializeSpeechRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Speech not available", Toast.LENGTH_SHORT).show()
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                speakOut("Listening. Say call guardian or share location")
            }
            @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
            override fun onResults(results: Bundle?) {

                val command = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.lowercase(Locale.getDefault()) ?: ""

                when {
                    command.contains("call") -> callRandomGuardian()
                    command.contains("location") -> shareLocationWithGuardians()
                    else -> speakOut("Command not recognized")
                }

                isListening = false
                updateMicButtonUI()
            }

            override fun onError(error: Int) {
                speakOut("Try again")
                isListening = false
                updateMicButtonUI()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun setupMicButton() {
        binding.btnMic.setOnClickListener {
            if (!isListening) startListening() else stopListening()
        }
    }
    private fun startListening() {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
            return
        }
        isListening = true
        updateMicButtonUI()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)

        speechRecognizer.startListening(intent)
    }
    private fun stopListening() {
        isListening = false
        speechRecognizer.stopListening()
        updateMicButtonUI()
    }
    private fun updateMicButtonUI() {
        binding.btnMic.setBackgroundColor(
            if (isListening) requireContext().getColor(android.R.color.holo_red_light)
            else requireContext().getColor(android.R.color.transparent)
        )
    }
    private fun callRandomGuardian() {
        val prefs = requireContext().getSharedPreferences("visionmate", Context.MODE_PRIVATE)
        val numbers = prefs.getStringSet("guardian_no", emptySet()) ?: emptySet()

        if (numbers.isEmpty()) {
            speakOut("No guardians found")
            return
        }
        val number = numbers.random()
        speakOut("Calling guardian")

        val intent = Intent(Intent.ACTION_CALL)
        intent.data = android.net.Uri.parse("tel:$number")
        startActivity(intent)
    }
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun shareLocationWithGuardians() {
        val prefs = requireContext().getSharedPreferences("visionmate", Context.MODE_PRIVATE)
        val numbers = prefs.getStringSet("guardian_no", emptySet()) ?: emptySet()

        if (numbers.isEmpty()) {
            speakOut("No guardians found")
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val message = "$userName needs help! Location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                for (num in numbers) {
                    sendSMS(num, message)
                }
                speakOut("Location sent to guardians")
            }
        }
    }
    private fun sendSMS(phone: String, message: String) {
        val smsManager = android.telephony.SmsManager.getDefault()
        smsManager.sendTextMessage(phone, null, message, null, null)
    }
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        )
        ActivityCompat.requestPermissions(requireActivity(), permissions, PERMISSION_REQUEST_CODE)
    }
    override fun onDestroy() {
        super.onDestroy()
        if (isListening) speechRecognizer.stopListening()
        speechRecognizer.destroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}