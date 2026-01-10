//package com.example.myvisionmate
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.speech.tts.TextToSpeech
//import androidx.fragment.app.Fragment
//import com.example.myvisionmate.databinding.FragmentDescribeSurroundingBinding
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageCapture
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.fragment.findNavController
//import kotlinx.coroutines.launch
//import java.util.Locale
//
//
//class DescribeSurroundingFragment : Fragment() {
//    private lateinit var binding: FragmentDescribeSurroundingBinding
//    private lateinit var textToSpeech: TextToSpeech
//    private var imageCapture: ImageCapture? = null
//
//    private val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        if (isGranted) {
//            startCamera()
//        } else {
//            speak("Camera permission required")
//            findNavController().navigateUp()
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        binding = FragmentDescribeSurroundingBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        initTTS()
//        setupClickListeners()
//        checkCameraPermission()
//    }
//
//    private fun initTTS() {
//        textToSpeech = TextToSpeech(requireContext()) { status ->
//            if (status == TextToSpeech.SUCCESS) {
//                textToSpeech.language = Locale.US
//                speak("Point camera at scene and tap describe")
//            }
//        }
//    }
//
//    private fun checkCameraPermission() {
//        when {
//            ContextCompat.checkSelfPermission(
//                requireContext(),
//                Manifest.permission.CAMERA
//            ) == PackageManager.PERMISSION_GRANTED -> {
//                startCamera()
//            }
//            else -> {
//                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
//            }
//        }
//    }
//
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
//
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//
//            val preview = Preview.Builder().build().also {
//                it.setSurfaceProvider(binding.previewView.surfaceProvider)
//            }
//
//            imageCapture = ImageCapture.Builder().build()
//
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    this,
//                    cameraSelector,
//                    preview,
//                    imageCapture
//                )
//            } catch (e: Exception) {
//                speak("Camera error")
//            }
//
//        }, ContextCompat.getMainExecutor(requireContext()))
//    }
//
//    private fun setupClickListeners() {
//        binding.btnDescribe.setOnClickListener {
//            captureAndDescribe()
//        }
//
//        binding.btnBack.setOnClickListener {
//            findNavController().navigateUp()
//        }
//    }
//
//    private fun captureAndDescribe() {
//        binding.progressBar.visibility = View.VISIBLE
//        binding.btnDescribe.isEnabled = false
//        speak("Analyzing scene")
//
//        // For now, show placeholder
//        lifecycleScope.launch {
//            kotlinx.coroutines.delay(2000)
//
//            val description = "You are in a living room. There is a couch 2 meters ahead on your left. A coffee table is in the center, about 3 meters away. TV on the wall in front of you, 4 meters distance. Window on the right side."
//
//            binding.tvDescription.text = description
//            binding.progressBar.visibility = View.GONE
//            binding.btnDescribe.isEnabled = true
//
//            speak(description)
//        }
//    }
//
//    private fun speak(text: String) {
//        if (::textToSpeech.isInitialized) {
//            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        if (::textToSpeech.isInitialized) {
//            textToSpeech.stop()
//            textToSpeech.shutdown()
//        }
//    }
//}
