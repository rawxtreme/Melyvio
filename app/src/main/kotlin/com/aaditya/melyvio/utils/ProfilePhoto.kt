/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Local-only profile picture. The picked photo is centre-cropped to a square and saved twice:
 * a small one for the home top bar and a bigger one for the "Your name" page.
 */
object ProfilePhoto {
    private const val LARGE_PX = 512
    private const val SMALL_PX = 160

    fun file(
        context: Context,
        large: Boolean,
    ): File = File(context.filesDir, if (large) "profile_dp_large.jpg" else "profile_dp_small.jpg")

    suspend fun save(
        context: Context,
        uri: Uri,
    ): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = decode(context, uri, LARGE_PX * 2) ?: return@runCatching false
                val side = min(source.width, source.height)
                val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
                write(file(context, true), Bitmap.createScaledBitmap(square, LARGE_PX, LARGE_PX, true))
                write(file(context, false), Bitmap.createScaledBitmap(square, SMALL_PX, SMALL_PX, true))
                true
            }.getOrDefault(false)
        }

    /** Centre-cropped square bitmap of [sizePx] x [sizePx], or null if the picture cannot be read. */
    fun decodeSquare(
        context: Context,
        uri: Uri,
        sizePx: Int,
    ): Bitmap? =
        runCatching {
            val source = decode(context, uri, sizePx * 2) ?: return@runCatching null
            val side = min(source.width, source.height)
            val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
            Bitmap.createScaledBitmap(square, sizePx, sizePx, true)
        }.getOrNull()

    suspend fun remove(context: Context) {
        withContext(Dispatchers.IO) {
            file(context, true).delete()
            file(context, false).delete()
        }
    }

    private fun write(
        target: File,
        bitmap: Bitmap,
    ) {
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    }

    /** Decodes the picture (EXIF rotation handled) without loading a full-size copy into memory. */
    private fun decode(
        context: Context,
        uri: Uri,
        minSide: Int,
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= 28) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val shortest = min(info.size.width, info.size.height)
                val scale = if (shortest > minSide) minSide.toFloat() / shortest else 1f
                decoder.setTargetSize(
                    max(1, (info.size.width * scale).toInt()),
                    max(1, (info.size.height * scale).toInt()),
                )
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val shortest = min(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (shortest / (sample * 2) >= minSide) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}
