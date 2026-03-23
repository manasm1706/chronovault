package com.example.chronovault.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Utility object for converting images between different formats
 * Supports Base64 encoding/decoding and bitmap conversions
 */
object ImageConverter {

    /**
     * Convert Bitmap to Base64 string
     * @param bitmap The bitmap to convert
     * @param quality The compression quality (0-100), default 85
     * @return Base64 encoded string
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Convert Base64 string to Bitmap
     * @param base64String The base64 encoded string
     * @return The decoded bitmap, or null if conversion fails
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert image file to Base64 string
     * @param file The image file
     * @param quality The compression quality (0-100), default 85
     * @return Base64 encoded string, or null if conversion fails
     */
    fun fileToBase64(file: File, quality: Int = 85): String? {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            bitmapToBase64(bitmap, quality)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert Uri to Base64 string
     * @param context The context for content resolver
     * @param uri The image URI
     * @param quality The compression quality (0-100), default 85
     * @return Base64 encoded string, or null if conversion fails
     */
    fun uriToBase64(context: Context, uri: Uri, quality: Int = 85): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmapToBase64(bitmap, quality)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save Base64 string to file
     * @param context The context for file directory
     * @param base64String The base64 encoded string
     * @param fileName The output file name
     * @return The file path, or null if save fails
     */
    fun base64ToFile(context: Context, base64String: String, fileName: String): String? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val file = File(context.cacheDir, fileName)
            file.writeBytes(decodedBytes)
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get size of Base64 string in MB
     * @param base64String The base64 encoded string
     * @return Size in MB
     */
    fun getBase64SizeInMB(base64String: String): Double {
        return (base64String.length * 3) / (4 * 1024 * 1024).toDouble()
    }

    /**
     * Compress Base64 image if it exceeds size limit
     * @param base64String The base64 encoded string
     * @param maxSizeMB Maximum size in MB
     * @return Compressed base64 string, or original if already within limit
     */
    fun compressBase64IfNeeded(base64String: String, maxSizeMB: Double = 2.0): String {
        var quality = 85
        var compressed = base64String

        while (getBase64SizeInMB(compressed) > maxSizeMB && quality > 30) {
            val bitmap = base64ToBitmap(compressed) ?: return base64String
            compressed = bitmapToBase64(bitmap, quality)
            quality -= 10
        }

        return compressed
    }
}

