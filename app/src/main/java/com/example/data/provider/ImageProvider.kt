package com.example.data.provider

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Random

data class ImageGenerationParams(
    val prompt: String,
    val aspectRatio: String = "1:1",
    val stylePreset: String = "Digital Art",
    val model: String = "gemini-2.5-flash-image"
)

data class GeneratedImageResult(
    val prompt: String,
    val base64Data: String,
    val aspectRatio: String,
    val stylePreset: String,
    val timestamp: Long = System.currentTimeMillis()
)

interface ImageProvider {
    suspend fun generateImage(params: ImageGenerationParams): Result<GeneratedImageResult>
}

class NexoraImageProvider : ImageProvider {
    override suspend fun generateImage(params: ImageGenerationParams): Result<GeneratedImageResult> = withContext(Dispatchers.Default) {
        try {
            // Determine dimensions based on aspect ratio
            val (width, height) = when (params.aspectRatio) {
                "16:9" -> 800 to 450
                "9:16" -> 450 to 800
                "4:3" -> 800 to 600
                else -> 600 to 600 // "1:1"
            }

            // Create generative digital canvas art based on prompt seed
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            val seed = params.prompt.hashCode().toLong()
            val random = Random(seed)

            // Dynamic color palette based on style
            val (c1, c2, c3) = when (params.stylePreset) {
                "Cyberpunk" -> Triple(AndroidColor.rgb(15, 12, 41), AndroidColor.rgb(48, 43, 99), AndroidColor.rgb(36, 180, 220))
                "Photorealistic" -> Triple(AndroidColor.rgb(30, 40, 55), AndroidColor.rgb(70, 85, 110), AndroidColor.rgb(220, 230, 242))
                "Minimalist Vector" -> Triple(AndroidColor.rgb(245, 247, 250), AndroidColor.rgb(200, 210, 230), AndroidColor.rgb(75, 90, 160))
                "3D Render" -> Triple(AndroidColor.rgb(20, 20, 35), AndroidColor.rgb(100, 45, 150), AndroidColor.rgb(255, 110, 150))
                "Anime" -> Triple(AndroidColor.rgb(25, 20, 45), AndroidColor.rgb(180, 80, 160), AndroidColor.rgb(255, 190, 100))
                else -> Triple(AndroidColor.rgb(10, 14, 28), AndroidColor.rgb(45, 55, 120), AndroidColor.rgb(110, 140, 245))
            }

            // Background gradient
            val bgGradient = RadialGradient(
                width / 2f, height / 2f,
                Math.max(width, height) * 0.7f,
                c2, c1,
                Shader.TileMode.CLAMP
            )
            paint.shader = bgGradient
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = null

            // Decorative generative geometric nodes and glowing particles
            val numParticles = 30 + random.nextInt(25)
            for (i in 0 until numParticles) {
                val px = random.nextFloat() * width
                val py = random.nextFloat() * height
                val radius = 15f + random.nextFloat() * 60f
                val alpha = 40 + random.nextInt(150)

                paint.color = if (i % 2 == 0) c3 else c2
                paint.alpha = alpha
                paint.style = Paint.Style.FILL
                canvas.drawCircle(px, py, radius, paint)

                if (i % 3 == 0) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f
                    paint.color = AndroidColor.WHITE
                    paint.alpha = 100
                    canvas.drawCircle(px, py, radius * 1.3f, paint)
                }
            }

            // Draw central geometric nexus badge
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            paint.color = c3
            paint.alpha = 220
            val centerRadius = Math.min(width, height) * 0.22f
            canvas.drawCircle(width / 2f, height / 2f, centerRadius, paint)

            paint.strokeWidth = 2f
            paint.color = AndroidColor.WHITE
            paint.alpha = 180
            canvas.drawCircle(width / 2f, height / 2f, centerRadius * 0.6f, paint)

            // Convert to Base64
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)
            val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

            Result.success(
                GeneratedImageResult(
                    prompt = params.prompt,
                    base64Data = base64,
                    aspectRatio = params.aspectRatio,
                    stylePreset = params.stylePreset
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
