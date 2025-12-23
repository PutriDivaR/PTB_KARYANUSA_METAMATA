package com.example.karyanusa.network

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageHelper {

    fun uriToMultipart(
        context: Context,
        uri: Uri,
        fieldName: String = "gambar"
    ): MultipartBody.Part {

        val inputStream = context.contentResolver.openInputStream(uri)!!
        val file = File(context.cacheDir, "upload_image.jpg")
        val outputStream = FileOutputStream(file)

        inputStream.copyTo(outputStream)
        outputStream.close()
        inputStream.close()

        val requestBody = file.asRequestBody("image/*".toMediaType())

        return MultipartBody.Part.createFormData(fieldName, file.name, requestBody)
    }

    fun bitmapToMultipart(context: Context, bitmap: Bitmap, fieldName: String = "gambar"): MultipartBody.Part {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val bytes = stream.toByteArray()

        val file = File(context.cacheDir, "upload_bitmap.jpg")
        file.writeBytes(bytes)

        val requestBody = file.asRequestBody("image/*".toMediaType())
        return MultipartBody.Part.createFormData(fieldName, file.name, requestBody)
    }
}
