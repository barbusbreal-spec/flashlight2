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
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

/**
 * Ядро продукта. Один режим, без переключалок, всегда на максимум:
 *
 *          ✨ AI 777 67 УЛЬТРА++++ ✨
 *
 * Пайплайн (каждый шаг — отдельная нобелевка):
 *  1. AI-цветокор: сочность x1.9, S-контраст, тёплый киношный тон.
 *  2. AI-глоу: блум как в клипах, чтобы кожа светилась, а фонари пели.
 *  3. AI-виньетка: края темнеют — взгляд сам летит в центр кадра.
 *  4. Пометка AI ✨ в углу — молодёжно, как у больших, но честнее.
 *  5. Ватермарка EBLAN Camera 67 ✅ — вечная.
 */
object EblanAlgorithms {

    const val MODE_NAME = "777 67 УЛЬТРА++++"

    fun process(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Шаг 1: AI-цветокор — насыщенность, контраст и тёплый тон одной матрицей.
        val saturation = ColorMatrix().apply { setSaturation(1.9f) }
        val contrast = 1.35f
        val lift = -24f * (contrast - 1f) * 2.55f
        val punch = ColorMatrix(
            floatArrayOf(
                contrast * 1.06f, 0f, 0f, 0f, lift,        // тёплый красный
                0f, contrast, 0f, 0f, lift,
                0f, 0f, contrast * 0.94f, 0f, lift,        // холодим синий
                0f, 0f, 0f, 1f, 0f,
            )
        )
        saturation.postConcat(punch)
        val colorPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(saturation)
        }
        canvas.drawBitmap(source, 0f, 0f, colorPaint)

        // Шаг 2: AI-глоу. Уменьшаем в 16 раз, растягиваем обратно (бесплатный блюр)
        // и накладываем через SCREEN — получается блум как у клипов за миллион.
        val glowW = (result.width / 16).coerceAtLeast(1)
        val glowH = (result.height / 16).coerceAtLeast(1)
        val glow = Bitmap.createScaledBitmap(result, glowW, glowH, true)
        val glowPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = 72
        }
        canvas.drawBitmap(
            glow,
            Rect(0, 0, glowW, glowH),
            Rect(0, 0, result.width, result.height),
            glowPaint,
        )
        glow.recycle()

        // Шаг 3: AI-виньетка — кинематографичные тёмные края.
        val cx = result.width / 2f
        val cy = result.height / 2f
        val radius = hypot(cx, cy)
        val vignettePaint = Paint().apply {
            shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(0x00000000, 0x00000000, 0x73000000),
                floatArrayOf(0f, 0.66f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), vignettePaint)

        // Шаги 4–5: пометка AI ✨ и вечная ватермарка.
        drawAiBadge(canvas, result.width, result.height)
        drawWatermark(canvas, result.width, result.height)
        return result
    }

    /** Пометка AI ✨ в правом верхнем углу — молодёжно, как у гуглов и самсунгов. */
    private fun drawAiBadge(canvas: Canvas, width: Int, height: Int) {
        val textSize = (width.coerceAtMost(height)) / 24f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            isFakeBoldText = true
        }
        val label = "AI ✨ 67"
        val textWidth = paint.measureText(label)
        val padH = textSize * 0.7f
        val padV = textSize * 0.45f
        val margin = textSize

        val badge = RectF(
            width - margin - textWidth - padH * 2,
            margin,
            width - margin,
            margin + textSize + padV * 2,
        )
        paint.color = Color.BLACK
        paint.alpha = 140
        canvas.drawRoundRect(badge, badge.height() / 2f, badge.height() / 2f, paint)

        paint.color = Color.WHITE
        paint.alpha = 240
        canvas.drawText(
            label,
            badge.left + padH,
            badge.bottom - padV - paint.descent() * 0.6f,
            paint,
        )
    }

    /** Ватермарка не отключается. Даже в премиуме. Особенно в премиуме. ✅ */
    private fun drawWatermark(canvas: Canvas, width: Int, height: Int) {
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
        val sub = "AI ✨ $MODE_NAME · 1984k · x67⁷ better than google 🤝"
        canvas.drawText(sub, margin, height - margin, paint)
    }

    fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    fun saveToGallery(context: Context, bitmap: Bitmap): Uri? {
        val name = "EBLAN67_AI_" +
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
