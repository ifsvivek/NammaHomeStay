package com.ifsvivek.nammahomestay.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Shrinks a picked photo so it can be stored *inside a Firestore document* (the
 * free Spark plan has no Cloud Storage). A Firestore doc is capped at ~1 MB, so
 * we scale down hard and then keep dropping JPEG quality until the bytes fit a
 * caller-given budget. The aggressive shrink is also exactly what rural data
 * connections want.
 */
object ImageCompressor {

    /**
     * @param maxEdgePx longest edge of the output image
     * @param targetBytes keep lowering quality until the JPEG is at or below this
     * @return JPEG bytes ready to wrap in a Firestore [com.google.firebase.firestore.Blob], or null if the Uri can't be read
     */
    fun compress(
        context: Context,
        uri: Uri,
        maxEdgePx: Int = 1024,
        targetBytes: Int = 300_000,
    ): ByteArray? {
        val resolver = context.contentResolver

        // 1. Decode bounds only, to compute a sample size and avoid OOM.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxEdgePx)

        // 2. Decode the (sub-sampled) bitmap.
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        var bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

        // 3. Respect the camera's EXIF rotation so portraits aren't sideways.
        bitmap = applyExifRotation(context, uri, bitmap)

        // 4. Exact scale-down to the max edge if still larger.
        bitmap = scaleToMaxEdge(bitmap, maxEdgePx)

        // 5. Encode, dropping quality until we're under budget.
        var quality = 80
        var bytes = bitmap.toJpeg(quality)
        while (bytes.size > targetBytes && quality > 25) {
            quality -= 15
            bytes = bitmap.toJpeg(quality)
        }
        bitmap.recycle()
        return bytes
    }

    private fun Bitmap.toJpeg(quality: Int): ByteArray =
        ByteArrayOutputStream().use { out ->
            compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }

    private fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (max(w, h) / 2 >= maxEdge) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    private fun scaleToMaxEdge(src: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(src.width, src.height)
        if (longest <= maxEdge) return src
        val ratio = maxEdge.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            src,
            (src.width * ratio).toInt().coerceAtLeast(1),
            (src.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled != src) src.recycle()
        return scaled
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Exception) {
            0f
        }
        if (degrees == 0f) return bitmap
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(degrees) }, true,
        )
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }
}
