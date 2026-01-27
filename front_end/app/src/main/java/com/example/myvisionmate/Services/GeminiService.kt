package com.example.myvisionmate.Services

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.internal.synchronizedImpl
import kotlinx.coroutines.withContext

class GeminiService {

    private val TAG = "GeminiService"

    private val generativeModel = Firebase.ai(
        backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-2.5-flash",
        systemInstruction = content {
            text("""
                You are an AI assistant helping blind and visually impaired people navigate safely.
                Your responses must be:
                1. Clear and concise
                2. Include specific directions (left, right, ahead, behind)
                3. Provide approximate distances in meters
                4. Identify potential obstacles or hazards
                5. Suggest safe navigation paths
                
                Always prioritize safety and clarity.
            """.trimIndent())
        }
    )

    suspend fun describeScene(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting scene analysis...")

            // Generate content with image and prompt
            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text("""
                        Describe this scene for a blind person who needs to navigate safely.
                        
                        Include:
                        1. Main objects and furniture with their positions (left, right, ahead, behind, center)
                        2. Approximate distances from camera in meters (1m, 2m, 3m, etc.)
                        3. Any obstacles at ground level that could cause tripping
                        4. Overall room layout and dimensions
                        5. Suggested safe path for walking
                        
                        Format: Start with room type, then list objects with positions and distances.
                        Example: "You are in a living room. Couch 2 meters ahead on your left..."
                    """.trimIndent())
                }
            )

            val description = response.text

            if (description.isNullOrBlank()) {
                Log.e(TAG, "Received empty response from Gemini")
                Result.failure(Exception("No description generated"))
            } else {
                Log.d(TAG, "Scene analysis successful. Length: ${description.length} chars")
                Result.success(description)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing scene", e)
            Result.failure(e)
        }
    }

    /**
     * Read and describe text from image (for OCR enhancement)
     * @param bitmap - Image containing text
     * @return Result with text content or error
     */
    suspend fun readText(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text("""
                        Extract and read all visible text from this image.
                        Read it exactly as written, including punctuation.
                        If no text is found, say "No text detected in image".
                    """.trimIndent())
                }
            )

            val text = response.text
            if (text.isNullOrBlank()) {
                Result.failure(Exception("No text extracted"))
            } else {
                Result.success(text)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error reading text", e)
            Result.failure(e)
        }
    }
}
