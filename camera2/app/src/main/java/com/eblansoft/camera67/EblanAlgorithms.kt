package com.eblansoft.camera67

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ядро продукта. Алгоритмы вычислительной фотографии,
 * ровно в 67 раз круче, чем у гугла (замерено на глаз). ✅✅✅
 *
 * Пайплайн eblanHRR™©®:
 *  1. Берём фотку.
 *  2. Крутим насыщенность и контраст, пока не станет "молодежно".
 *  3. Шлёпаем ватермарку EBLAN Camera 67, чтобы все знали, чьё это.
 */
object EblanAlgorithms {

    /**
     * Каждый режим просто сильнее выкручивает насыщенность/контраст.
     * Чем больше плюсиков в названии — тем больше матрица. Наука. 🤝
     */
    fun process(source: Bitmap, hdrModeIndex: Int): Bitmap {
        val level = hdrModeIndex + 1 // 1..5
        val saturation = 1f + level * 0.16f          // до x1.8 сочности
        val contrast = 1f + level * 0.05f            // до x1.25 дерзости
        val translate = (-16f) * level * (contrast - 1f)

        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val matrix = ColorMatrix().apply { setSaturation(saturation) }
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        matrix.postConcat(contrastMatrix)

        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        drawWatermark(canvas, result.width, result.height, hdrModeIndex)
        return result
    }

    /** Ватермарка не отключается. Даже в премиуме. Особенно в премиуме. ✅ */
    private fun drawWatermark(canvas: Canvas, width: Int, height: Int, hdrModeIndex: Int) {
        val textSize = (width.coerceAtMost(height)) / 18f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 230
            this.textSize = textSize
            isFakeBoldText = true
            setShadowLayer(textSize / 6f, 0f, textSize / 12f, Color.BLACK)
        }
        val margin = textSize / 2f

        val main = "EBLAN Camera 67 ✅"
        canvas.drawText(main, margin, height - margin - textSize * 1.2f, paint)

        paint.textSize = textSize * 0.55f
        paint.alpha = 200
        val sub = "shot on ${Prefs.HDR_MODES[hdrModeIndex]} · 67x better than google 🤝"
        canvas.drawText(sub, margin, height - margin, paint)
    }

    fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    fun saveToGallery(context: Context, bitmap: Bitmap): Uri? {
        val name = "EBLAN67_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EBLAN Camera 67")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return uri
    }
}
