package com.example.visionmate.ui.describe

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myvisionmate.Services.GeminiService
import com.example.myvisionmate.databinding.FragmentDescribeSurroundingBinding
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DescribeSurroundingsFragment : Fragment() {

    private lateinit var binding: FragmentDescribeSurroundingBinding
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    // Initialize Gemini service
    private val geminiService = GeminiService()

    private val TAG = "DescribeFragment"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            speak("Camera permission required")
            findNavController().navigateUp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDescribeSurroundingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        initTTS()
        setupClickListeners()
        checkCameraPermission()
    }

    private fun initTTS() {
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
                speak("Point camera at scene and tap describe")
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // Image capture with high quality
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                speak("Camera error")
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun setupClickListeners() {
        binding.btnDescribe.setOnClickListener {
            captureAndDescribe()
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun captureAndDescribe() {
        // Disable button to prevent multiple clicks
        binding.btnDescribe.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.tvDescription.text = "Capturing image..."

        speak("Capturing scene")

        imageCapture?.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    Log.d(TAG, "Image captured successfully")

                    // Convert ImageProxy to Bitmap
                    val bitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()

                    if (bitmap != null) {
                        // Send to Gemini AI
                        analyzeWithGemini(bitmap)
                    } else {
                        requireActivity().runOnUiThread {
                            handleError("Failed to process image")
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed", exception)
                    requireActivity().runOnUiThread {
                        handleError("Capture failed. Please try again.")
                    }
                }
            }
        )
    }

    private fun analyzeWithGemini(bitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                binding.tvDescription.text = "Analyzing scene with AI..."
                speak("Analyzing scene")

                Log.d(TAG, "Sending image to Gemini API...")

                // Call Gemini service
                val result = geminiService.describeScene(bitmap)

                requireActivity().runOnUiThread {
                    result.onSuccess { description ->
                        Log.d(TAG, "Received description: ${description.take(100)}...")

                        // Display and speak description
                        binding.tvDescription.text = description
                        speak(description)

                        Toast.makeText(
                            requireContext(),
                            "Scene analyzed successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                    }.onFailure { error ->
                        handleError("AI analysis failed: ${error.message}")
                    }

                    binding.progressBar.visibility = View.GONE
                    binding.btnDescribe.isEnabled = true
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during analysis", e)
                requireActivity().runOnUiThread {
                    handleError("Analysis error: ${e.message}")
                }
            }
        }
    }

    private fun handleError(message: String) {
        Log.e(TAG, message)
        binding.tvDescription.text = message
        binding.progressBar.visibility = View.GONE
        binding.btnDescribe.isEnabled = true
        speak("Analysis failed. Please try again.")

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val buffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e)
            null
        }
    }

    private fun speak(text: String) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
}