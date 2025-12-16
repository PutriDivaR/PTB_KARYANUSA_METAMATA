package com.example.karyanusa.component

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    /**
     * Convert URI to File
     */
    fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver

            // Get filename
            val fileName = getFileName(context, uri) ?: "temp_image_${System.currentTimeMillis()}.jpg"

            // Create temp file in cache directory
            val tempFile = File(context.cacheDir, fileName)

            // Copy URI content to temp file
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get filename from URI
     */
    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null

        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            }
        }

        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }

        return result
    }

    /**
     * Create URI for camera capture
     */
    fun createImageUri(context: Context): Uri? {
        return try {
            val fileName = "IMG_${System.currentTimeMillis()}.jpg"
            val imageFile = File(context.cacheDir, fileName)

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}