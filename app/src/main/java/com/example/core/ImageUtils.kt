package com.example.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ValidatedImage(
    val id: String = UUID.randomUUID().toString(),
    val localPath: String,
    val mimeType: String,
    val originalBytes: Long,
    val width: Int,
    val height: Int
)

object ImageUtils {
    private const val MAX_INPUT_BYTES = 15L * 1024L * 1024L // 15 MB limit per photo
    private const val MAX_AI_DIMENSION = 1280
    private const val MAX_THUMBNAIL_DIMENSION = 420
    private const val MAX_THUMBNAIL_BYTES = 140 * 1024 // <= 140 KB (< 190 KB Base64)
    private val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

    fun createTempCameraUri(context: Context): Uri {
        val dir = File(context.filesDir, "scans").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun validateAndSaveLocalCopy(context: Context, uri: Uri): Result<ValidatedImage> = runCatching {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)?.lowercase() ?: "image/jpeg"
        if (mime !in ALLOWED_MIME_TYPES) {
            throw IllegalArgumentException("Unsupported image format ($mime). Please select a JPEG, PNG, or WebP photograph.")
        }

        val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Unable to read the selected image file.")

        if (rawBytes.isEmpty()) {
            throw IllegalArgumentException("The selected image file is empty.")
        }
        if (rawBytes.size.toLong() > MAX_INPUT_BYTES) {
            throw IllegalArgumentException("Image exceeds 15 MB limit. Please choose a smaller photograph.")
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalArgumentException("The selected file does not appear to be a valid image.")
        }

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, 2048, 2048)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
            ?: throw IllegalArgumentException("Could not decode image data.")

        val dir = File(context.filesDir, "scans").apply { mkdirs() }
        val destFile = File(dir, "plant_${UUID.randomUUID()}.jpg")
        FileOutputStream(destFile).use { out ->
            decoded.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        val width = decoded.width
        val height = decoded.height
        decoded.recycle()

        ValidatedImage(
            localPath = destFile.absolutePath,
            mimeType = mime,
            originalBytes = rawBytes.size.toLong(),
            width = width,
            height = height
        )
    }

    fun loadBitmapForDisplay(localPath: String, maxDimension: Int = 1280): Bitmap? {
        val file = File(localPath)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(localPath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxDimension, maxDimension)
        }
        return BitmapFactory.decodeFile(localPath, opts)
    }

    fun prepareBitmapForGemini(localPath: String): Bitmap {
        val raw = loadBitmapForDisplay(localPath, MAX_AI_DIMENSION)
            ?: throw IllegalStateException("Local image could not be loaded for AI analysis.")
        return scaleDownIfNeeded(raw, MAX_AI_DIMENSION)
    }

    fun rotateLocalImage90(localPath: String): Result<ValidatedImage> = runCatching {
        val bitmap = BitmapFactory.decodeFile(localPath)
            ?: throw IllegalStateException("Image not found.")
        val matrix = Matrix().apply { postRotate(90f) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        FileOutputStream(File(localPath)).use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        val res = ValidatedImage(
            localPath = localPath,
            mimeType = "image/jpeg",
            originalBytes = File(localPath).length(),
            width = rotated.width,
            height = rotated.height
        )
        rotated.recycle()
        res
    }

    fun cropLocalImageNormalized(
        localPath: String,
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float
    ): Result<ValidatedImage> = runCatching {
        val bitmap = BitmapFactory.decodeFile(localPath)
            ?: throw IllegalStateException("Image not found.")
        val l = (leftFraction.coerceIn(0f, 0.9f) * bitmap.width).roundToInt()
        val t = (topFraction.coerceIn(0f, 0.9f) * bitmap.height).roundToInt()
        val r = (rightFraction.coerceIn(leftFraction + 0.1f, 1f) * bitmap.width).roundToInt()
        val b = (bottomFraction.coerceIn(topFraction + 0.1f, 1f) * bitmap.height).roundToInt()
        val cropW = max(32, r - l).coerceAtMost(bitmap.width - l)
        val cropH = max(32, b - t).coerceAtMost(bitmap.height - t)
        val cropped = Bitmap.createBitmap(bitmap, l, t, cropW, cropH)
        if (cropped != bitmap) bitmap.recycle()
        FileOutputStream(File(localPath)).use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        val res = ValidatedImage(
            localPath = localPath,
            mimeType = "image/jpeg",
            originalBytes = File(localPath).length(),
            width = cropped.width,
            height = cropped.height
        )
        cropped.recycle()
        res
    }

    fun createCompressedThumbnailBase64(localPath: String): String? {
        val loaded = loadBitmapForDisplay(localPath, MAX_THUMBNAIL_DIMENSION) ?: return null
        val scaled = scaleDownIfNeeded(loaded, MAX_THUMBNAIL_DIMENSION)
        var quality = 78
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= 12
        } while (bytes.size > MAX_THUMBNAIL_BYTES && quality >= 30)
        if (scaled != loaded) loaded.recycle()
        scaled.recycle()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun decodeBase64Thumbnail(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun deleteLocalFile(localPath: String?) {
        if (localPath.isNullOrBlank()) return
        try {
            File(localPath).delete()
        } catch (_: Exception) {
        }
    }

    fun clearAllLocalScanImages(context: Context) {
        try {
            File(context.filesDir, "scans").deleteRecursively()
        } catch (_: Exception) {
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val maxSide = max(bitmap.width, bitmap.height)
        if (maxSide <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / maxSide.toFloat()
        val newW = max(1, (bitmap.width * ratio).roundToInt())
        val newH = max(1, (bitmap.height * ratio).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun calculateSampleSize(width: Int, height: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (height > reqH || width > reqW) {
            val halfH = height / 2
            val halfW = width / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return min(16, max(1, inSampleSize))
    }
}
