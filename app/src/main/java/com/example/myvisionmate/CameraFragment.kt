package com.example.myvisionmate

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.myvisionmate.databinding.FragmentCameraBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private lateinit var binding: FragmentCameraBinding
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var cameraExecutor: ExecutorService

    private var camera: Camera? = null
    private var toneGenerator: ToneGenerator? = null

    private var lastSpokenObject = ""
    private var lastSpokenTime = 0L
    private var lastBeepTime = 0L
    private var isScanning = true

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                speak("Camera permission required")
                findNavController().navigateUp()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(inflater, container, false)

        cameraExecutor = Executors.newSingleThreadExecutor()
        initTTS()
        initToneGenerator()
        setupClickListeners()
        checkCameraPermission()

        return binding.root
    }

    private fun initToneGenerator() {
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    }

    private fun initTTS() {
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
                speak("Camera started. Scanning for objects.")
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                speak("Camera error")
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun speak(text: String) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun playProximityBeep(objectSize: Int) {
        val delay = when {
            objectSize > 200_000 -> 200L
            objectSize > 100_000 -> 400L
            objectSize > 50_000 -> 800L
            else -> 2000L
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBeepTime >= delay) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            lastBeepTime = currentTime
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        if (!isScanning) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build()

        val objectDetector = ObjectDetection.getClient(options)

        objectDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                if (detectedObjects.isNotEmpty()) {
                    val obj = detectedObjects[0]
                    val label = obj.labels.firstOrNull()?.text ?: "object"
                    val confidence =
                        ((obj.labels.firstOrNull()?.confidence ?: 0f) * 100).toInt()

                    val boundingBox = obj.boundingBox
                    val size = boundingBox.width() * boundingBox.height()

                    val distance = when {
                        size > 200_000 -> "Very Close"
                        size > 100_000 -> "Close"
                        size > 50_000 -> "Medium"
                        else -> "Far"
                    }

                    requireActivity().runOnUiThread {
                        binding.tvDetectedObject.text =
                            "$label ($confidence% confidence)"
                        binding.tvDistance.text = "Distance: $distance"
                    }

                    playProximityBeep(size)

                    val currentTime = System.currentTimeMillis()
                    if (label != lastSpokenObject || currentTime - lastSpokenTime > 3000) {
                        speak("$label ahead")
                        lastSpokenObject = label
                        lastSpokenTime = currentTime
                    }
                } else {
                    requireActivity().runOnUiThread {
                        binding.tvDetectedObject.text = "No objects detected"
                        binding.tvDistance.text = "Distance: --"
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun setupClickListeners() {
        binding.btnStopScanning.setOnClickListener {
            speak("Stopping camera")
            findNavController().navigateUp()
        }

        binding.btnPauseScanning.setOnClickListener {
            isScanning = !isScanning
            if (isScanning) {
                binding.btnPauseScanning.text = "Pause"
                binding.tvStatus.text = "🔴 Scanning..."
                speak("Scanning resumed")
            } else {
                binding.btnPauseScanning.text = "Resume"
                binding.tvStatus.text = "⏸️ Paused"
                speak("Scanning paused")
            }
        }

        binding.fabHelp.setOnClickListener {
            speak("Hold phone at chest level. Move slowly. Listen for beeps and voice announcements.")
        }

        binding.volumeSlider.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: android.widget.SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                textToSpeech.setSpeechRate(progress / 50f)
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        toneGenerator?.release()
    }
}
