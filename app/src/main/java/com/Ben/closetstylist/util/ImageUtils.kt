package com.Ben.closetstylist.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

fun compressImageFromUri(context: Context, uri: Uri, maxDimension: Int = 1024): ByteArray {
    val stream = context.contentResolver.openInputStream(uri)
        ?: error("Cannot open input stream for $uri")
    val original = stream.use { BitmapFactory.decodeStream(it) }
        ?: error("Cannot decode bitmap from $uri")
    val scaled = scaleBitmap(original, maxDimension)
    if (scaled !== original) original.recycle()
    return ByteArrayOutputStream().use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        scaled.recycle()
        out.toByteArray()
    }
}

fun saveToClosetDir(context: Context, imageBytes: ByteArray, fileName: String): String {
    val dir = File(context.filesDir, "closet").also { it.mkdirs() }
    return File(dir, fileName).also { it.writeBytes(imageBytes) }.let { "file://${it.absolutePath}" }
}

fun saveToInspirationDir(context: Context, imageBytes: ByteArray, fileName: String): String {
    val dir = File(context.filesDir, "inspiration").also { it.mkdirs() }
    return File(dir, fileName).also { it.writeBytes(imageBytes) }.let { "file://${it.absolutePath}" }
}

fun createCameraUri(context: Context): Uri {
    val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDimension && h <= maxDimension) return bitmap
    val ratio = maxDimension.toFloat() / maxOf(w, h)
    return Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
}
