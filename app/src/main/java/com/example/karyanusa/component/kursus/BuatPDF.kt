package com.example.karyanusa.component.kursus

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import com.example.karyanusa.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

fun generateCertificatePdf(context: Context, userName: String, kursusTitle: String): Uri? {
    return try {
        val pageWidth = 842
        val pageHeight = 595
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas


        val bg = BitmapFactory.decodeResource(context.resources, R.drawable.sertifikat)
        val scaledBg = bg.scale(pageWidth, pageHeight)
        canvas.drawBitmap(scaledBg, 0f, 0f, null)


        val namePaint = Paint().apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 38f
            color = Color.rgb(233, 30, 99)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val descPaint = Paint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 16f
            color = Color.rgb(60, 60, 60)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val centerX = pageWidth / 2f
        val nameY = pageHeight * 0.50f
        val descY = pageHeight * 0.60f

        canvas.drawText(userName, centerX, nameY, namePaint)

        val description = "telah menyelesaikan kelas \"$kursusTitle\" pada platform KaryaNusa."
        drawMultilineCenteredText(canvas, description, centerX, descY, descPaint, pageWidth - 160)

        document.finishPage(page)

        val fileName = "Sertifikat_KaryaNusa_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
        val savedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStore(context, document, fileName)
        } else {
            saveToExternalFile(context, document, fileName)
        }

        document.close()
        savedUri
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Gagal membuat sertifikat: ${e.message}", Toast.LENGTH_LONG).show()
        null
    }
}


private fun drawMultilineCenteredText(
    canvas: Canvas,
    text: String,
    centerX: Float,
    startY: Float,
    paint: Paint,
    maxWidth: Int
) {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var line = StringBuilder()

    for (word in words) {
        val testLine = if (line.isEmpty()) word else "$line $word"
        if (paint.measureText(testLine) < maxWidth) {
            if (line.isNotEmpty()) line.append(" ")
            line.append(word)
        } else {
            lines.add(line.toString())
            line = StringBuilder(word)
        }
    }
    if (line.isNotEmpty()) lines.add(line.toString())

    val lineHeight = -paint.ascent() + paint.descent()
    var y = startY
    for (l in lines) {
        canvas.drawText(l, centerX, y, paint)
        y += lineHeight
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun saveToMediaStore(context: Context, document: PdfDocument, fileName: String): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }

    val resolver = context.contentResolver
    val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val uri = resolver.insert(collection, values) ?: return null

    resolver.openOutputStream(uri)?.use { out -> document.writeTo(out) }

    values.clear()
    values.put(MediaStore.Downloads.IS_PENDING, 0)
    resolver.update(uri, values, null, null)
    return uri
}

private fun saveToExternalFile(context: Context, document: PdfDocument, fileName: String): Uri? {
    val outFile = File(context.getExternalFilesDir(null), fileName)
    FileOutputStream(outFile).use { out -> document.writeTo(out) }
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", outFile)
}

fun openPdf(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Tidak ada aplikasi untuk membuka PDF.", Toast.LENGTH_SHORT).show()
    }
}
