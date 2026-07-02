package com.eblansoft.camera67

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Конфиг съёмки: зум и лаг-режимы. Каждый включённый режим — это ещё один
 * честный проход по всем пикселям. Больше режимов → больше LAG LVL ⚡.
 */
data class EblanConfig(
    /** Доп. цифровой зум поверх железного (для 1488x — режем пиксели сами). */
    val digitalZoom: Float = 1f,
    val night777: Boolean = false,
    val beauty67: Boolean = false,
    val bwDerzkiy: Boolean = false,
    val sepiaPacan: Boolean = false,
    val fisheye: Boolean = false,
    val glitch2007: Boolean = false,
    /** Портретный блюр фона: нейронка MLKit + фолбэк для камер-говн. */
    val portraitBlur: Boolean = false,
    // Крутилки ебейшести из настроек:
    val juiciness: Int = 155,   // сочность, %
    val sharpness: Int = 86,    // резкость, %
    val glowAlpha: Int = 64,    // сила глоу, 0..128
    val vignette: Int = 45,     // виньетка, %
    val warmth: Int = 3,        // теплота, -10..10
) {
    val lagLevel: Int
        get() = listOf(night777, beauty67, bwDerzkiy, sepiaPacan, fisheye, glitch2007, portraitBlur)
            .count { it } + if (digitalZoom > 1.01f) 1 else 0
}

/**
 * Ядро продукта. Никаких «наложили фильтр и разошлись» — тут настоящая
 * вычислительная фотография уровня:
 *
 *          ✨ AI 777 67 УЛЬТРА++++ ✨
 *
 * Базовый пайплайн — 7 этапов по каждому пикселю (CLAHE, exposure fusion,
 * unsharp mask, тон-кривая, глоу, виньетка, ватермарка), плюс бонусные
 * лаг-режимы: ночной, бьюти, ЧБ, сепия, рыбий глаз, глитч. Телефон честно
 * лагает — это не баг, это глубина обработки.
 */
object EblanAlgorithms {

    const val MODE_NAME = "eblanHRRrawHDR77722867++"
    const val VIDEO_FPS_LABEL = "8771828fps"

    /**
     * Кнопка 67: мемасик на фото и видео. @Volatile, потому что читают
     * и пиксельный конвейер, и видео-оверлей, и UI — все хотят ОКАК.
     */
    @Volatile
    var meme67Enabled = false

    /** Сюда складываем результаты бурмалда-алгоритмов, чтобы JIT их не выкинул. */
    @Volatile
    private var burmaldaSink = 0L

    /** Имена бурмалда-алгоритмов. Что делают — коммерческая тайна (ничего). */
    private val BURMALDA = listOf(
        "Квантовая стабилизация бурмалды",
        "Блокчейн-верификация кадра",
        "Дефрагментация пикселей",
        "Нейро-аура 67",
        "Анти-кринж фильтр",
        "Согласование с вайбом",
        "Реверс-энтропия",
        "Каскадная сверка галочек ✅",
        "Пре-рендер ОКАК",
        "Пост-квантовый шумодав имени деда",
    )

    /** Максимум мегапикселей в обработку — чтобы лагало, но не умирало. */
    private const val MAX_PIXELS = 12_500_000

    val STAGES = listOf(
        "Этап 1/7: читаем RAW-душу кадра 👀",
        "Этап 2/7: CLAHE-гистограммы по 64 зонам 📊",
        "Этап 3/7: синтез 3 экспозиций + fusion 🌗",
        "Этап 4/7: свёртка резкости 67×67 🔪",
        "Этап 5/7: тон-кривая 777 и сочность 🌈",
        "Этап 6/7: AI-глоу и виньетка, кино 🎬",
        "Этап 7/7: пометка AI ✨ и ватермарка 🤝",
    )

    /**
     * Вход — стек реальных кадров с EV-брекетингом (HDR RAW 67228+++++).
     * Один кадр — тоже стек, просто грустный.
     */
    fun process(
        frames: List<Bitmap>,
        config: EblanConfig = EblanConfig(),
        onStage: (String) -> Unit = {},
    ): Bitmap {
        require(frames.isNotEmpty()) { "нейросети нужен хотя бы один кадр, брат" }

        // ---------- Этап 1: стек кадров → пиксели ----------
        onStage(STAGES[0])
        // Чем больше кадров в стеке, тем скромнее размер каждого — чтобы
        // лагало от алгоритмов, а не от OutOfMemoryError.
        val perFramePixels = (MAX_PIXELS / frames.size).coerceAtLeast(2_000_000)

        var w = 0
        var h = 0
        val stack = frames.mapIndexed { idx, frame ->
            var input = frame
            if (config.digitalZoom > 1.01f) {
                if (idx == 0) {
                    onStage("БОНУС: зум ×%.0f — нарезаем пиксели 🚀".format(config.digitalZoom))
                }
                input = digitalZoom(input, config.digitalZoom)
            }
            val capped = capResolution(input, perFramePixels)
            if (idx == 0) {
                w = capped.width
                h = capped.height
            }
            val sized = if (capped.width != w || capped.height != h) {
                val s = Bitmap.createScaledBitmap(capped, w, h, true)
                if (capped !== frame) capped.recycle()
                s
            } else capped
            val arr = IntArray(w * h)
            sized.getPixels(arr, 0, w, 0, 0, w, h)
            if (sized !== frame) sized.recycle()
            arr
        }
        val n = w * h

        // ---------- HDR RAW: сплав реального стека по Мертенсу ----------
        val px: IntArray
        if (stack.size > 1) {
            onStage("HDR RAW 67228: сплавляем стек из ${stack.size} кадров 🥞")
            px = fuseStack(stack, n)
        } else {
            px = stack[0]
        }

        if (config.night777) {
            onStage("БОНУС: НОЧНОЙ 777 🌙 вытягиваем тени из подвала")
            applyNight(px, n)
        }

        var luma = ByteArray(n)
        fillLuma(px, luma, n)

        // ---------- Этап 2: CLAHE ----------
        onStage(STAGES[1])
        applyClahe(px, luma, w, h)

        // ---------- Этап 3: exposure fusion ----------
        onStage(STAGES[2])
        applyExposureFusion(px, n)

        // ---------- Этап 4: unsharp mask ----------
        onStage(STAGES[3])
        fillLuma(px, luma, n)
        applyUnsharp(px, luma, w, h, config.sharpness)
        luma = ByteArray(0) // нейросеть освобождает память как умеет

        // ---------- Этап 5: тон-кривая + сочность ----------
        onStage(STAGES[4])
        applyToneAndSaturation(px, n, config.juiciness, config.warmth)

        // ---------- Бурмалда-блок: 67 алгоритмов, все важные ----------
        // Прогресс не показываем: алгоритм eblanHRRrawHDR77722867++
        // ультра секретный. Но вычисления реальные — лаг честный.
        runBurmalda(px, n)

        // ---------- Бонусные лаг-режимы ----------
        if (config.bwDerzkiy) {
            onStage("БОНУС: ЧБ ДЕРЗКИЙ 🖤 цвет для слабых")
            applyBw(px, n)
        }
        if (config.sepiaPacan) {
            onStage("БОНУС: СЕПИЯ ПАЦАНСКАЯ 📜 как у деда")
            applySepia(px, n)
        }
        if (config.glitch2007) {
            onStage("БОНУС: ГЛИТЧ 2007 📼 ломаем каналы")
            applyGlitch(px, w, h)
        }
        if (config.fisheye) {
            onStage("БОНУС: РЫБИЙ ГЛАЗ 🐟 гнём пространство")
            applyFisheye(px, w, h)
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(px, 0, w, 0, 0, w, h)
        val canvas = Canvas(result)

        if (config.portraitBlur) {
            onStage("БОНУС: ПОРТРЕТ БЛЮР 🌫 отделяем красавчика от фона")
            applyPortraitBlur(canvas, result)
        }

        if (config.beauty67) {
            onStage("БОНУС: БЬЮТИ 67 💅 кожа как у младенца")
            applyBeauty(canvas, result)
        }

        // ---------- Этап 6: глоу + виньетка ----------
        onStage(STAGES[5])
        applyGlow(canvas, result, config.glowAlpha)
        applyVignette(canvas, w, h, config.vignette)

        // ---------- Этап 7: AI ✨ и ватермарка ----------
        onStage(STAGES[6])
        drawAiBadge(canvas, w, h)
        drawWatermark(canvas, w, h)
        if (meme67Enabled) {
            drawMeme67(canvas, w, h)
        }
        return result
    }

    /**
     * 67 бурмалда-алгоритмов. Каждый делает реальный проход по пикселям
     * (checksum, гистограммы, чётность, XOR-свёртки) и складывает результат
     * в burmaldaSink, откуда его никто никогда не заберёт. Главное — что
     * алгоритмы ЕСТЬ. ✅✅✅
     */
    private fun runBurmalda(px: IntArray, n: Int) {
        var sink = burmaldaSink
        val hist = IntArray(256)
        for (algo in 0 until 67) {
            val stride = 5 + (algo % 7)
            when (algo % 5) {
                0 -> { // блокчейн-верификация (контрольная сумма)
                    var s = 0L
                    var i = algo % stride
                    while (i < n) { s += px[i] and 0xFFFFFF; i += stride }
                    sink = sink xor s
                }
                1 -> { // нейро-аура (гистограмма яркости)
                    var i = algo % stride
                    while (i < n) {
                        val c = px[i]
                        hist[((c ushr 16 and 0xFF) * 299 +
                            (c ushr 8 and 0xFF) * 587 +
                            (c and 0xFF) * 114) / 1000]++
                        i += stride
                    }
                    sink += hist[algo * 3 % 256]
                }
                2 -> { // каскадная сверка галочек (XOR-свёртка)
                    var x = 0
                    var i = algo % stride
                    while (i < n) { x = x xor px[i]; i += stride }
                    sink = sink xor x.toLong()
                }
                3 -> { // дефрагментация пикселей (поиск максимума)
                    var m = 0
                    var i = algo % stride
                    while (i < n) { if (px[i] and 0xFF > m) m = px[i] and 0xFF; i += stride }
                    sink += m
                }
                else -> { // анти-кринж фильтр (подсчёт чётных пикселей)
                    var even = 0
                    var i = algo % stride
                    while (i < n) { if (px[i] and 1 == 0) even++; i += stride }
                    sink += even.toLong()
                }
            }
        }
        burmaldaSink = sink + BURMALDA.size // имена тоже участвуют
    }

    /** Кнопка 67: молодёжный мемасик с галочками. ОКАК. */
    private fun drawMeme67(canvas: Canvas, width: Int, height: Int) {
        val base = (width.coerceAtMost(height)) / 5f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = base
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            setShadowLayer(base / 5f, 0f, base / 10f, Color.BLACK)
        }
        canvas.save()
        canvas.rotate(-12f, width / 2f, height / 3f)
        canvas.drawText("67 ✅✅✅", width / 2f, height / 3f, paint)
        paint.textSize = base * 0.55f
        canvas.drawText("ОКАК 🤝", width / 2f, height / 3f + base * 0.75f, paint)
        paint.textSize = base * 0.25f
        paint.alpha = 220
        canvas.drawText(
            "сертифицировано пацанами",
            width / 2f,
            height / 3f + base * 1.15f,
            paint,
        )
        canvas.restore()
    }

    /**
     * Оверлей для видео: алгоритмы 8771828fps в реальном времени —
     * тёплый тон, виньетка, плашка AI и вечная ватермарка на каждом кадре.
     */
    fun drawVideoOverlay(canvas: Canvas, width: Int, height: Int) {
        canvas.drawColor(0x12FF9A3D) // тёплый киношный тонировочный слой
        applyVignette(canvas, width, height)
        drawAiBadge(canvas, width, height, "AI ✨ $VIDEO_FPS_LABEL")
        drawWatermark(canvas, width, height)
        if (meme67Enabled) {
            drawMeme67(canvas, width, height)
        }
    }

    /** Центр-кроп + растяжка обратно: честный цифровой зум. На 1488x — пиксель-арт. */
    private fun digitalZoom(source: Bitmap, factor: Float): Bitmap {
        val cw = (source.width / factor).toInt().coerceAtLeast(8)
        val ch = (source.height / factor).toInt().coerceAtLeast(8)
        val x = (source.width - cw) / 2
        val y = (source.height - ch) / 2
        val crop = Bitmap.createBitmap(source, x, y, cw, ch)
        val zoomed = Bitmap.createScaledBitmap(crop, source.width, source.height, true)
        if (crop !== zoomed) crop.recycle()
        return zoomed
    }

    private fun fillLuma(px: IntArray, luma: ByteArray, n: Int) {
        for (i in 0 until n) {
            val c = px[i]
            luma[i] = (((c ushr 16 and 0xFF) * 299 +
                (c ushr 8 and 0xFF) * 587 +
                (c and 0xFF) * 114) / 1000).toByte()
        }
    }

    /**
     * Сплав реального HDR RAW стека по упрощённому Мертенсу: каждый кадр
     * голосует за пиксель гауссовым весом «хорошей экспонированности».
     * Недодержанный кадр спасает света, передержанный — тени. Как HDR+
     * у гугла, только плюсов больше на 67228.
     */
    private fun fuseStack(stack: List<IntArray>, n: Int): IntArray {
        val weightLut = FloatArray(256)
        val sigma2 = 2f * 0.2f * 0.2f
        for (v in 0 until 256) {
            val d = v / 255f - 0.5f
            weightLut[v] = exp(-d * d / sigma2) + 0.05f
        }
        val out = IntArray(n)
        for (i in 0 until n) {
            var sw = 0f
            var sr = 0f
            var sg = 0f
            var sb = 0f
            for (f in stack.indices) {
                val c = stack[f][i]
                val r = c ushr 16 and 0xFF
                val g = c ushr 8 and 0xFF
                val b = c and 0xFF
                val wt = weightLut[(r * 299 + g * 587 + b * 114) / 1000]
                sw += wt
                sr += r * wt
                sg += g * wt
                sb += b * wt
            }
            out[i] = 0xFF000000.toInt() or
                ((sr / sw).toInt().coerceIn(0, 255) shl 16) or
                ((sg / sw).toInt().coerceIn(0, 255) shl 8) or
                (sb / sw).toInt().coerceIn(0, 255)
        }
        return out
    }

    private fun capResolution(source: Bitmap, maxPixels: Int = MAX_PIXELS): Bitmap {
        val n = source.width.toLong() * source.height
        if (n <= maxPixels) return source
        val scale = sqrt(maxPixels.toDouble() / n)
        return Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    /** НОЧНОЙ 777: гамма-лифт теней по LUT — как ночной режим, только наш. */
    private fun applyNight(px: IntArray, n: Int) {
        val lut = IntArray(256)
        for (v in 0 until 256) {
            lut[v] = (255f * (v / 255f).pow(0.72f)).toInt().coerceIn(0, 255)
        }
        for (i in 0 until n) {
            val c = px[i]
            px[i] = (c and 0xFF000000.toInt()) or
                (lut[c ushr 16 and 0xFF] shl 16) or
                (lut[c ushr 8 and 0xFF] shl 8) or
                lut[c and 0xFF]
        }
    }

    /** ЧБ ДЕРЗКИЙ: моно + жёсткий контраст. */
    private fun applyBw(px: IntArray, n: Int) {
        for (i in 0 until n) {
            val c = px[i]
            val l = ((c ushr 16 and 0xFF) * 299 +
                (c ushr 8 and 0xFF) * 587 +
                (c and 0xFF) * 114) / 1000
            val v = (((l - 128) * 118) / 100 + 128).coerceIn(0, 255)
            px[i] = (c and 0xFF000000.toInt()) or (v shl 16) or (v shl 8) or v
        }
    }

    /** СЕПИЯ ПАЦАНСКАЯ: классическая матрица, тёплая как чифир. */
    private fun applySepia(px: IntArray, n: Int) {
        for (i in 0 until n) {
            val c = px[i]
            val r = c ushr 16 and 0xFF
            val g = c ushr 8 and 0xFF
            val b = c and 0xFF
            val nr = ((r * 393 + g * 769 + b * 189) / 1000).coerceAtMost(255)
            val ng = ((r * 349 + g * 686 + b * 168) / 1000).coerceAtMost(255)
            val nb = ((r * 272 + g * 534 + b * 131) / 1000).coerceAtMost(255)
            px[i] = (c and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
        }
    }

    /** ГЛИТЧ 2007: красный канал влево, синий вправо — VHS у бабушки. */
    private fun applyGlitch(px: IntArray, w: Int, h: Int) {
        val shift = (w / 160).coerceAtLeast(3)
        val src = px.copyOf()
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val i = row + x
                val r = src[row + (x + shift).coerceAtMost(w - 1)] ushr 16 and 0xFF
                val g = src[i] ushr 8 and 0xFF
                val b = src[row + (x - shift).coerceAtLeast(0)] and 0xFF
                px[i] = (src[i] and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
            }
        }
    }

    /** РЫБИЙ ГЛАЗ: бочковая дисторсия — полный ремап всех пикселей. Лагает изысканно. */
    private fun applyFisheye(px: IntArray, w: Int, h: Int) {
        val src = px.copyOf()
        val cx = w / 2f
        val cy = h / 2f
        val k = 0.55f
        for (y in 0 until h) {
            val ny = (y - cy) / cy
            val row = y * w
            for (x in 0 until w) {
                val nx = (x - cx) / cx
                val r2 = nx * nx + ny * ny
                val d = 1f + k * r2
                val sx = (cx + nx * cx * d).toInt().coerceIn(0, w - 1)
                val sy = (cy + ny * cy * d).toInt().coerceIn(0, h - 1)
                px[row + x] = src[sy * w + sx]
            }
        }
    }

    /**
     * ПОРТРЕТ БЛЮР 🌫: фон в мыло, красавчик резкий. Сначала пробуем
     * нейронку (MLKit selfie segmentation, модель в комплекте). Если
     * нейронка человека не нашла или камера совсем говно — фолбэк:
     * резкий центр, кинематографичное мыло по краям. Боке из ничего.
     */
    private fun applyPortraitBlur(canvas: Canvas, src: Bitmap) {
        val w = src.width
        val h = src.height

        // Жёсткий блюр фона: даунскейл в 12 раз и обратно.
        val bw = (w / 12).coerceAtLeast(1)
        val bh = (h / 12).coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(src, bw, bh, true)
        val blurred = Bitmap.createScaledBitmap(small, w, h, true)
        small.recycle()

        // Слой фона: блюр, у которого альфа = «это фон» (человек прозрачен).
        val layer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(layer)
        layerCanvas.drawBitmap(blurred, 0f, 0f, null)
        blurred.recycle()

        val aiMask = runCatching { buildAiBackgroundMask(src) }.getOrNull()
        if (aiMask != null) {
            val maskPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            layerCanvas.drawBitmap(
                aiMask,
                Rect(0, 0, aiMask.width, aiMask.height),
                Rect(0, 0, w, h),
                maskPaint,
            )
            aiMask.recycle()
        } else {
            // Камера-говно эдишн: нейронка не нашла человека — крутим
            // радиальное боке. Центр резкий, края в мыло. Тоже красиво.
            val radial = Paint().apply {
                shader = RadialGradient(
                    w / 2f, h / 2f,
                    (w.coerceAtLeast(h)) * 0.62f,
                    intArrayOf(0x00000000, 0x00000000, 0xE6000000.toInt()),
                    floatArrayOf(0f, 0.42f, 1f),
                    Shader.TileMode.CLAMP,
                )
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            layerCanvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), radial)
        }
        canvas.drawBitmap(layer, 0f, 0f, null)
        layer.recycle()
    }

    /**
     * MLKit selfie segmentation → альфа-маска фона (255 = фон, 0 = красавчик).
     * Сегментируем уменьшенную копию — нейронке хватает, а лаг экономим
     * для бурмалды.
     */
    private fun buildAiBackgroundMask(src: Bitmap): Bitmap? {
        val scale = 512f / maxOf(src.width, src.height)
        val sw = (src.width * scale).toInt().coerceAtLeast(64)
        val sh = (src.height * scale).toInt().coerceAtLeast(64)
        val smallSrc = Bitmap.createScaledBitmap(src, sw, sh, true)
        val segmenter = com.google.mlkit.vision.segmentation.Segmentation.getClient(
            com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions.Builder()
                .setDetectorMode(
                    com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions.SINGLE_IMAGE_MODE
                )
                .build()
        )
        return try {
            val mask = com.google.android.gms.tasks.Tasks.await(
                segmenter.process(
                    com.google.mlkit.vision.common.InputImage.fromBitmap(smallSrc, 0)
                ),
                10, java.util.concurrent.TimeUnit.SECONDS,
            )
            val mw = mask.width
            val mh = mask.height
            val buf = mask.buffer
            buf.rewind()
            val pxm = IntArray(mw * mh)
            var personPixels = 0
            for (i in 0 until mw * mh) {
                val confidence = buf.float // насколько это красавчик, 0..1
                if (confidence > 0.5f) personPixels++
                pxm[i] = (((1f - confidence) * 255).toInt().coerceIn(0, 255)) shl 24
            }
            // Человека в кадре меньше 1%? Нейронка честно говорит «не нашла».
            if (personPixels < mw * mh / 100) {
                null
            } else {
                Bitmap.createBitmap(pxm, mw, mh, Bitmap.Config.ARGB_8888)
            }
        } finally {
            smallSrc.recycle()
            segmenter.close()
        }
    }

    /** БЬЮТИ 67: мягкий слой (даунскейл ×4 → апскейл) поверх — кожа шёлк. */
    private fun applyBeauty(canvas: Canvas, result: Bitmap) {
        val sw = (result.width / 4).coerceAtLeast(1)
        val sh = (result.height / 4).coerceAtLeast(1)
        val soft = Bitmap.createScaledBitmap(result, sw, sh, true)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 115 }
        canvas.drawBitmap(
            soft,
            Rect(0, 0, sw, sh),
            Rect(0, 0, result.width, result.height),
            paint,
        )
        soft.recycle()
    }

    /**
     * CLAHE: сетка 8×8 зон, у каждой своя эквализированная гистограмма
     * с клипом, между зонами — билинейная интерполяция. Каждый пиксель
     * получает свой персональный прирост яркости. Потому и лагает.
     */
    private fun applyClahe(px: IntArray, luma: ByteArray, w: Int, h: Int) {
        val grid = 8
        val tileW = (w + grid - 1) / grid
        val tileH = (h + grid - 1) / grid
        val maps = Array(grid * grid) { ByteArray(256) }

        for (ty in 0 until grid) {
            for (tx in 0 until grid) {
                val hist = IntArray(256)
                val x0 = tx * tileW
                val y0 = ty * tileH
                val x1 = (x0 + tileW).coerceAtMost(w)
                val y1 = (y0 + tileH).coerceAtMost(h)
                var count = 0
                for (y in y0 until y1) {
                    var i = y * w + x0
                    for (x in x0 until x1) {
                        hist[luma[i].toInt() and 0xFF]++
                        i++
                        count++
                    }
                }
                if (count == 0) count = 1
                // Клипуем гистограмму, излишки размазываем ровным слоем.
                val clip = (2.5f * count / 256).toInt().coerceAtLeast(4)
                var excess = 0
                for (b in 0 until 256) {
                    if (hist[b] > clip) {
                        excess += hist[b] - clip
                        hist[b] = clip
                    }
                }
                val bonus = excess / 256
                var cdf = 0
                val map = maps[ty * grid + tx]
                for (b in 0 until 256) {
                    cdf += hist[b] + bonus
                    map[b] = ((cdf.toLong() * 255) / count).coerceAtMost(255).toByte()
                }
            }
        }

        // Билинейная интерполяция между зонами + мягкий бленд с оригиналом.
        for (y in 0 until h) {
            val fy = (y.toFloat() / tileH) - 0.5f
            val ty0 = fy.toInt().coerceIn(0, grid - 1)
            val ty1 = (ty0 + 1).coerceAtMost(grid - 1)
            val wy = (fy - ty0).coerceIn(0f, 1f)
            var i = y * w
            for (x in 0 until w) {
                val fx = (x.toFloat() / tileW) - 0.5f
                val tx0 = fx.toInt().coerceIn(0, grid - 1)
                val tx1 = (tx0 + 1).coerceAtMost(grid - 1)
                val wx = (fx - tx0).coerceIn(0f, 1f)

                val l = luma[i].toInt() and 0xFF
                val m00 = maps[ty0 * grid + tx0][l].toInt() and 0xFF
                val m01 = maps[ty0 * grid + tx1][l].toInt() and 0xFF
                val m10 = maps[ty1 * grid + tx0][l].toInt() and 0xFF
                val m11 = maps[ty1 * grid + tx1][l].toInt() and 0xFF
                val top = m00 + (m01 - m00) * wx
                val bot = m10 + (m11 - m10) * wx
                val eq = top + (bot - top) * wy
                // 55% CLAHE + 45% оригинала, чтобы не пережарить бабушку на фото.
                val newL = (0.55f * eq + 0.45f * l)
                val gain = (newL + 1f) / (l + 1f)

                val c = px[i]
                val r = (((c ushr 16 and 0xFF) * gain).toInt()).coerceAtMost(255)
                val g = (((c ushr 8 and 0xFF) * gain).toInt()).coerceAtMost(255)
                val b = (((c and 0xFF) * gain).toInt()).coerceAtMost(255)
                px[i] = (c and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
                i++
            }
        }
    }

    /**
     * Exposure fusion: из одного кадра синтезируем EV- (спасаем света),
     * EV0 и EV+ (вытягиваем тени) и сплавляем по гауссовым весам
     * «хорошей экспонированности». Всё через LUT, но по каждому каналу
     * каждого пикселя — телефон имеет право вспотеть.
     */
    private fun applyExposureFusion(px: IntArray, n: Int) {
        val lutLow = IntArray(256)
        val lutHigh = IntArray(256)
        val wLow = FloatArray(256)
        val wMid = FloatArray(256)
        val wHigh = FloatArray(256)
        val sigma2 = 2f * 0.22f * 0.22f
        for (v in 0 until 256) {
            val f = v / 255f
            lutLow[v] = (255f * f.pow(1.6f)).toInt()
            lutHigh[v] = (255f * f.pow(0.55f)).toInt()
            fun wellExposed(x: Int): Float {
                val d = x / 255f - 0.5f
                return exp(-d * d / sigma2) + 0.02f
            }
            wLow[v] = wellExposed(lutLow[v])
            wMid[v] = wellExposed(v)
            wHigh[v] = wellExposed(lutHigh[v])
        }
        for (i in 0 until n) {
            val c = px[i]
            val r = c ushr 16 and 0xFF
            val g = c ushr 8 and 0xFF
            val b = c and 0xFF
            val l = (r * 299 + g * 587 + b * 114) / 1000
            val kl = wLow[l]
            val km = wMid[l]
            val kh = wHigh[l]
            val ks = kl + km + kh
            val nr = ((lutLow[r] * kl + r * km + lutHigh[r] * kh) / ks).toInt().coerceIn(0, 255)
            val ng = ((lutLow[g] * kl + g * km + lutHigh[g] * kh) / ks).toInt().coerceIn(0, 255)
            val nb = ((lutLow[b] * kl + b * km + lutHigh[b] * kh) / ks).toInt().coerceIn(0, 255)
            px[i] = (c and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
        }
    }

    /**
     * Unsharp mask: раздельный box-blur яркости (два прохода скользящим
     * окном), потом каждый канал докручивается на разницу. Резкость 67/10.
     */
    private fun applyUnsharp(px: IntArray, luma: ByteArray, w: Int, h: Int, sharpness: Int) {
        if (sharpness <= 0) return
        val amount256 = (sharpness * 256) / 100
        val radius = (w.coerceAtMost(h) / 300).coerceIn(2, 8)
        val tmp = ByteArray(luma.size)
        val blur = ByteArray(luma.size)
        val win = radius * 2 + 1

        // Горизонтальный проход.
        for (y in 0 until h) {
            val row = y * w
            var sum = 0
            for (x in -radius..radius) {
                sum += luma[row + x.coerceIn(0, w - 1)].toInt() and 0xFF
            }
            for (x in 0 until w) {
                tmp[row + x] = (sum / win).toByte()
                val outX = (x - radius).coerceAtLeast(0)
                val inX = (x + radius + 1).coerceAtMost(w - 1)
                sum += (luma[row + inX].toInt() and 0xFF) - (luma[row + outX].toInt() and 0xFF)
            }
        }
        // Вертикальный проход.
        for (x in 0 until w) {
            var sum = 0
            for (y in -radius..radius) {
                sum += tmp[y.coerceIn(0, h - 1) * w + x].toInt() and 0xFF
            }
            for (y in 0 until h) {
                blur[y * w + x] = (sum / win).toByte()
                val outY = (y - radius).coerceAtLeast(0)
                val inY = (y + radius + 1).coerceAtMost(h - 1)
                sum += (tmp[inY * w + x].toInt() and 0xFF) - (tmp[outY * w + x].toInt() and 0xFF)
            }
        }
        // Докрутка резкости: сила из настроек.
        for (i in px.indices) {
            val d = (luma[i].toInt() and 0xFF) - (blur[i].toInt() and 0xFF)
            if (d == 0) continue
            val boost = (d * amount256) shr 8
            val c = px[i]
            val r = ((c ushr 16 and 0xFF) + boost).coerceIn(0, 255)
            val g = ((c ushr 8 and 0xFF) + boost).coerceIn(0, 255)
            val b = ((c and 0xFF) + boost).coerceIn(0, 255)
            px[i] = (c and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
        }
    }

    /** S-кривая 777 + сочность + тёплый тон из настроек — финальный пиксельный проход. */
    private fun applyToneAndSaturation(px: IntArray, n: Int, juiciness: Int, warmth: Int) {
        val sCurve = IntArray(256)
        for (v in 0 until 256) {
            val f = v / 255f
            val s = f * f * (3f - 2f * f) // smoothstep — кривая как у больших
            sCurve[v] = (255f * (0.30f * s + 0.70f * f) * 1.06f)
                .toInt().coerceIn(0, 255)
        }
        val warmR = 100 + warmth
        val warmB = 100 - warmth
        for (i in 0 until n) {
            val c = px[i]
            var r = c ushr 16 and 0xFF
            var g = c ushr 8 and 0xFF
            var b = c and 0xFF
            val l = (r * 299 + g * 587 + b * 114) / 1000
            // Сочность: тянем каналы от серого на juiciness процентов.
            r = (l + ((r - l) * juiciness) / 100).coerceIn(0, 255)
            g = (l + ((g - l) * juiciness) / 100).coerceIn(0, 255)
            b = (l + ((b - l) * juiciness) / 100).coerceIn(0, 255)
            // Тёплый киношный тон + S-кривая.
            r = sCurve[(r * warmR / 100).coerceIn(0, 255)]
            g = sCurve[g]
            b = sCurve[(b * warmB / 100).coerceIn(0, 255)]
            px[i] = (c and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
        }
    }

    /** AI-глоу: даунскейл в 16 раз + SCREEN-наложение = блум как в клипах. */
    private fun applyGlow(canvas: Canvas, result: Bitmap, glowAlpha: Int = 64) {
        if (glowAlpha <= 0) return
        val glowW = (result.width / 16).coerceAtLeast(1)
        val glowH = (result.height / 16).coerceAtLeast(1)
        val glow = Bitmap.createScaledBitmap(result, glowW, glowH, true)
        val glowPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = glowAlpha.coerceIn(1, 255)
        }
        canvas.drawBitmap(
            glow,
            Rect(0, 0, glowW, glowH),
            Rect(0, 0, result.width, result.height),
            glowPaint,
        )
        glow.recycle()
    }

    private fun applyVignette(canvas: Canvas, width: Int, height: Int, strength: Int = 45) {
        if (strength <= 0) return
        val cx = width / 2f
        val cy = height / 2f
        val radius = hypot(cx, cy)
        val edgeAlpha = (strength * 255 / 100).coerceIn(0, 255)
        val vignettePaint = Paint().apply {
            shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(0x00000000, 0x00000000, edgeAlpha shl 24),
                floatArrayOf(0f, 0.66f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
    }

    /** Пометка AI ✨ в правом верхнем углу — молодёжно, как у гуглов и самсунгов. */
    private fun drawAiBadge(canvas: Canvas, width: Int, height: Int, label: String = "AI ✨ 67") {
        val textSize = (width.coerceAtMost(height)) / 24f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            isFakeBoldText = true
        }
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

    fun saveToGallery(context: Context, bitmap: Bitmap, quality: Int = 95): Uri? {
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
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(67, 100), out)
        }
        return uri
    }
}
