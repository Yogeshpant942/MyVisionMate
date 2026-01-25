package com.example.visionmate.ui.readtext

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
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
import androidx.navigation.fragment.findNavController
import com.example.myvisionmate.databinding.FragmentReadTextBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ReadTextFragment : Fragment() {

    private lateinit var binding: FragmentReadTextBinding
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

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
        binding = FragmentReadTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTTS()
        setupClickListeners()
        checkCameraPermission()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun initTTS() {
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale("hi","IN")
                speak("Point camera at text and tap capture")
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

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
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
                speak("Camera error")
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun setupClickListeners() {
        binding.btnCaptureText.setOnClickListener {
            captureAndReadText()
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCopy.setOnClickListener {
            copyTextToClipboard()
        }
    }
    @androidx.camera.core.ExperimentalGetImage
    private fun captureAndReadText() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCaptureText.isEnabled = false
        speak("Capturing image")
        imageCapture?.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        recognizeText(image)
                    }
                    imageProxy.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    speak("Capture failed")
                    binding.progressBar.visibility = View.GONE
                    binding.btnCaptureText.isEnabled = true
                }
            }
        )
    }

    private fun recognizeText(image: InputImage) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text

                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCaptureText.isEnabled = true

                    if (text.isNotEmpty()) {
                        binding.tvDetectedText.text = text
                        speak(text)
                    } else {
                        binding.tvDetectedText.text = "No text detected. Try again."
                        speak("No text found")
                    }
                }
            }
            .addOnFailureListener {
                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCaptureText.isEnabled = true
                    speak("Text recognition failed")
                }
            }
    }

    private fun copyTextToClipboard() {
        val text = binding.tvDetectedText.text.toString()
        if (text.isNotEmpty() && text != "No text detected yet. Point camera at text and tap Capture.") {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Detected Text", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Text copied", Toast.LENGTH_SHORT).show()
            speak("Text copied to clipboard")
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
