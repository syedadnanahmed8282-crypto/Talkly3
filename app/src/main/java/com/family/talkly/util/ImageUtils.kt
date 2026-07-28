package com.family.talkly.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    private const val TAG = "ImageUtils"

    /**
     * Helper to decode Base64 data URLs into ByteArray for Coil AsyncImage rendering,
     * or return the raw string URL/Uri if it's standard HTTP/HTTPS/File/Content scheme.
     */
    fun getProfileImageModel(url: String?): Any? {
        if (url.isNullOrBlank()) return null
        return if (url.startsWith("data:image")) {
            try {
                val base64Data = url.substringAfter(",")
                Base64.decode(base64Data, Base64.DEFAULT)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode base64 image string: ${e.localizedMessage}")
                url
            }
        } else {
            url
        }
    }

    /**
     * Compresses and processes a selected image Uri for a profile picture.
     * 1. Saves a local copy to app internal storage for immediate offline persistence across restarts.
     * 2. Attempts upload to Firebase Storage (returning HTTPS URL).
     * 3. Fallbacks to a lightweight Base64 Data URL string (~15-20KB) stored in Firestore.
     */
    suspend fun processAndSaveProfileImage(
        context: Context,
        userUid: String,
        imageUri: Uri
    ): String = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap != null) {
                val maxDim = 400
                val width = originalBitmap.width
                val height = originalBitmap.height
                val scale = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height, 1.0f)
                val scaledW = (width * scale).toInt().coerceAtLeast(1)
                val scaledH = (height * scale).toInt().coerceAtLeast(1)

                val scaledBitmap = if (scale < 1.0f) {
                    Bitmap.createScaledBitmap(originalBitmap, scaledW, scaledH, true)
                } else {
                    originalBitmap
                }

                // 1. Local persistent file save
                val fileName = "profile_pic_${userUid.ifBlank { System.currentTimeMillis() }}.jpg"
                val profileFile = File(context.filesDir, fileName)
                val fileOut = FileOutputStream(profileFile)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fileOut)
                fileOut.flush()
                fileOut.close()

                // 2. Base64 Data URL fallback string
                val byteArrayOutputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream)
                val imageBytes = byteArrayOutputStream.toByteArray()
                val base64Str = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                val dataUrl = "data:image/jpeg;base64,$base64Str"

                if (scaledBitmap != originalBitmap) {
                    scaledBitmap.recycle()
                }
                originalBitmap.recycle()

                // 3. Attempt Firebase Storage Upload
                if (userUid.isNotBlank()) {
                    try {
                        val storageRef = FirebaseStorage.getInstance().reference.child("profile_pics/${userUid}.jpg")
                        val uploadTask = storageRef.putFile(Uri.fromFile(profileFile))

                        var remoteDownloadUrl: String? = null
                        uploadTask.continueWithTask { task ->
                            if (!task.isSuccessful) {
                                task.exception?.let { throw it }
                            }
                            storageRef.downloadUrl
                        }.addOnSuccessListener { uri ->
                            remoteDownloadUrl = uri.toString()
                        }

                        val start = System.currentTimeMillis()
                        while (!uploadTask.isComplete && System.currentTimeMillis() - start < 3500) {
                            kotlinx.coroutines.delay(100)
                        }

                        if (uploadTask.isSuccessful && !remoteDownloadUrl.isNullOrBlank()) {
                            Log.d(TAG, "Uploaded profile image to Firebase Storage successfully: $remoteDownloadUrl")
                            return@withContext remoteDownloadUrl!!
                        }
                    } catch (ex: Exception) {
                        Log.w(TAG, "Firebase Storage upload fallback to Base64/Local: ${ex.localizedMessage}")
                    }
                }

                // If Firebase Storage failed or offline, return dataUrl which syncs everywhere via Firestore
                return@withContext dataUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing profile image Uri $imageUri: ${e.localizedMessage}", e)
        }
        return@withContext imageUri.toString()
    }
}
