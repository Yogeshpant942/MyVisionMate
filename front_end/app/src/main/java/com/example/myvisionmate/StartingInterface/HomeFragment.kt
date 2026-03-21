package com.example.myvisionmate.StartingInterface

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myvisionmate.R
import com.example.myvisionmate.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var isListening = false
    private var userName: String = ""

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

        // Get user name from SharedPreferences
        val prefs = requireContext().getSharedPreferences("visionmate", Context.MODE_PRIVATE)
        userName = prefs.getString("user_name", "User") ?: "User"

        initializeSpeechRecognizer()

        setupMicButton()

        requestPermissions()

        // Navigation Click Listeners
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

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Speech Recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognizer", "Ready for speech")
                speakOut("Listening for commands. Say 'call guardian' or 'share location'")
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                Log.d("SpeechRecognizer", "End of speech")
            }

            override fun onError(error: Int) {
                Log.e("SpeechRecognizer", "Error: $error")
                speakOut("Sorry, I didn't catch that. Please try again.")
                isListening = false
                updateMicButtonUI()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0].lowercase(Locale.getDefault()).trim()
                    Log.d("SpeechRecognizer", "Recognized: $command")

                    when {
                        command.contains("call guardian") || command.contains("call") -> {
                            callRandomGuardian()
                        }
                        command.contains("share location") || command.contains("location") -> {
                            shareLocationWithGuardians()
                        }
                        else -> {
                            speakOut("Command not recognized. Say 'call guardian' or 'share location'")
                        }
                    }
                }
                isListening = false
                updateMicButtonUI()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun setupMicButton() {
        binding.btnMic.setOnClickListener {
            if (!isListening) {
                startListening()
            } else {
                stopListening()
            }
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
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

        speechRecognizer.startListening(intent)
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer.stopListening()
        updateMicButtonUI()
    }

    private fun updateMicButtonUI() {
        if (isListening) {
            binding.btnMic.setBackgroundColor(requireContext().getColor(android.R.color.holo_red_light))
        } else {
            binding.btnMic.setBackgroundColor(requireContext().getColor(android.R.color.transparent))
        }
    }

    // ====== CALL GUARDIAN ======
    private fun callRandomGuardian() {
        val prefs = requireContext().getSharedPreferences("visionmate", Context.MODE_PRIVATE)
        val guardianNumbers = prefs.getStringSet("guardian_no", emptySet()) ?: emptySet()

        if (guardianNumbers.isEmpty()) {
            speakOut("No guardians found. Please add guardian numbers in settings first.")
            Toast.makeText(requireContext(), "No guardians configured", Toast.LENGTH_SHORT).show()
            return
        }

        // Select random guardian
        val randomGuardianNumber = guardianNumbers.random()
        speakOut("Calling guardian")
        Toast.makeText(requireContext(), "Calling guardian...", Toast.LENGTH_SHORT).show()

        Log.d("CallGuardian", "Calling guardian at $randomGuardianNumber")

        // Make phone call
        makePhoneCall(randomGuardianNumber)
    }

    private fun makePhoneCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PHONE_PERMISSION_REQUEST
            )
            return
        }

        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = android.net.Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
            Log.d("CallGuardian", "Call initiated to $phoneNumber")
        } catch (e: Exception) {
            Log.e("CallGuardian", "Error making call: ${e.message}")
            Toast.makeText(requireContext(), "Error making call", Toast.LENGTH_SHORT).show()
        }
    }

    // ====== SHARE LOCATION VIA SMS ======
    private fun shareLocationWithGuardians() {
        val prefs = requireContext().getSharedPreferences("visionmate", Context.MODE_PRIVATE)
        val guardianNumbers = prefs.getStringSet("guardian_no", emptySet()) ?: emptySet()

        if (guardianNumbers.isEmpty()) {
            speakOut("No guardians found. Please add guardian numbers in settings first.")
            Toast.makeText(requireContext(), "No guardians configured", Toast.LENGTH_SHORT).show()
            return
        }

        // Check location permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        speakOut("Getting your location. This will take a moment")
        Toast.makeText(requireContext(), "Fetching location...", Toast.LENGTH_SHORT).show()

        // Get current location
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                Log.d("LocationShare", "Location obtained: ${location.latitude}, ${location.longitude}")

                // Create Google Maps link
                val mapsLink = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                val message = "$userName needs help!\nLocation: ${location.latitude}, ${location.longitude}\n$mapsLink"

                // Send SMS to each guardian
                sendSMSToGuardians(guardianNumbers.toList(), message)

                speakOut("Location shared with all guardians via message")
                Toast.makeText(requireContext(), "Location shared with ${guardianNumbers.size} guardians", Toast.LENGTH_SHORT).show()
            } else {
                speakOut("Unable to get your location. Please enable location services.")
                Log.e("LocationShare", "Location is null")
                Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("LocationShare", "Location error: ${e.message}")
            speakOut("Error getting location")
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSMSToGuardians(guardians: List<String>, message: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST
            )
            return
        }

        for (phoneNumber in guardians) {
            sendSingleSMS(phoneNumber, message)
        }
    }

    private fun sendSingleSMS(phoneNumber: String, message: String) {
        try {
            // Open default messaging app with pre-filled message
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
            }
            startActivity(intent)
            Log.d("SendSMS", "SMS intent opened for $phoneNumber")
        } catch (e: Exception) {
            Log.e("SendSMS", "Error opening SMS app: ${e.message}")
            Toast.makeText(requireContext(), "Error opening message app", Toast.LENGTH_SHORT).show()
        }
    }

    // ====== UTILITY FUNCTIONS ======
    private fun speakOut(message: String) {
        Log.d("SpeakOut", message)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsNeeded.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permissions", "Permission granted")
                }
            }
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permissions", "Location permission granted")
                }
            }
            CALL_PHONE_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permissions", "Call permission granted")
                }
            }
            SMS_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permissions", "SMS permission granted")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isListening) {
            speechRecognizer.stopListening()
        }
        speechRecognizer.destroy()
    }
}