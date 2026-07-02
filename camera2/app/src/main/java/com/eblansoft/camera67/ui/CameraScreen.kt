package com.eblansoft.camera67.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.eblansoft.camera67.EblanAlgorithms
import com.eblansoft.camera67.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Бесплатно — минута видео. В премиуме — БЕЗЛИМИТ (тоже минута). ✅ */
private const val FREE_VIDEO_LIMIT_SEC = 60
private const val PREMIUM_VIDEO_LIMIT_SEC = 60

@Composable
fun CameraScreen(onOpenPremium: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(context) }

    var isVideoMode by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var photosLeft by remember { mutableIntStateOf(prefs.photosLeft()) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingStage by remember { mutableStateOf(EblanAlgorithms.STAGES.first()) }
    var showLimitDialog by remember { mutableStateOf(false) }

    var recording by remember { mutableStateOf<Recording?>(null) }
    var recordSeconds by remember { mutableIntStateOf(0) }

    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }

    // Перепривязываем камеру при смене фото/видео или объектива.
    LaunchedEffect(isVideoMode, lensFacing) {
        recording?.stop()
        recording = null
        val provider = context.cameraProvider()
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        provider.unbindAll()
        if (isVideoMode) {
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            val vc = VideoCapture.withOutput(recorder)
            videoCapture = vc
            imageCapture = null
            provider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
        } else {
            val ic = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            imageCapture = ic
            videoCapture = null
            provider.bindToLifecycle(lifecycleOwner, selector, preview, ic)
        }
    }

    // Таймер записи + принудительный «безлимит на минуте».
    LaunchedEffect(recording) {
        recordSeconds = 0
        val active = recording ?: return@LaunchedEffect
        val limit = if (prefs.isPremium) PREMIUM_VIDEO_LIMIT_SEC else FREE_VIDEO_LIMIT_SEC
        while (recordSeconds < limit && recording != null) {
            delay(1000)
            recordSeconds++
        }
        if (recording != null) {
            active.stop()
            recording = null
            statusMessage = if (prefs.isPremium) {
                "Безлимитная минута премиума закончилась ✅🤝 Видео в галерее"
            } else {
                "Лимит 1:00 бесплатного тарифа ✅ Видео сохранено"
            }
        }
    }

    // Статусы сами исчезают, как сторис.
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(4000)
            statusMessage = null
        }
    }

    fun takeEblanPhoto() {
        if (prefs.photosLeft() <= 0) {
            showLimitDialog = true
            return
        }
        val capture = imageCapture ?: return
        isProcessing = true
        processingStage = EblanAlgorithms.STAGES.first()
        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                    val rotation = image.imageInfo.rotationDegrees
                    image.close()
                    scope.launch {
                        val startedAt = System.currentTimeMillis()
                        val saved = withContext(Dispatchers.Default) {
                            val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            val upright = EblanAlgorithms.rotate(raw, rotation)
                            val masterpiece = EblanAlgorithms.process(upright) { stage ->
                                processingStage = stage
                            }
                            EblanAlgorithms.saveToGallery(context, masterpiece)
                        }
                        val lagSec = (System.currentTimeMillis() - startedAt) / 1000f
                        isProcessing = false
                        if (saved != null) {
                            prefs.registerPhoto()
                            photosLeft = prefs.photosLeft()
                            statusMessage =
                                "AI ✨ ${EblanAlgorithms.MODE_NAME}: фотка ебейшая ✅✅✅ " +
                                    "Нейросеть страдала %.1f сек 🥵 Осталось $photosLeft/12"
                                        .format(lagSec)
                        } else {
                            statusMessage = "Не сохранилось 💀 (даже AI 777 бессилен)"
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    isProcessing = false
                    statusMessage = "Камера сказала нет 💀 ${exception.imageCaptureError}"
                }
            }
        )
    }

    fun toggleRecording() {
        val active = recording
        if (active != null) {
            active.stop()
            recording = null
            statusMessage = "Видео сохранено в галерею ✅🤝"
            return
        }
        val vc = videoCapture ?: return
        val name = "EBLAN67_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".mp4"
        val values = android.content.ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/EBLAN Camera 67")
        }
        val options = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(values)
            .build()
        val audioGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val pending = vc.output.prepareRecording(context, options).apply {
            if (audioGranted) withAudioEnabled()
        }
        recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize && event.hasError()) {
                statusMessage = "Видео не записалось 💀 код ${event.error}"
                recording = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        TopBar(
            prefs = prefs,
            photosLeft = photosLeft,
            isRecording = recording != null,
            recordSeconds = recordSeconds,
            onOpenPremium = onOpenPremium,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        statusMessage?.let { msg ->
            Text(
                msg,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .background(Color(0xCC14141C), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        BottomBar(
            isVideoMode = isVideoMode,
            isRecording = recording != null,
            isProcessing = isProcessing,
            onModeChange = { video -> if (recording == null) isVideoMode = video },
            onShutter = { if (isVideoMode) toggleRecording() else takeEblanPhoto() },
            onSwitchCamera = {
                if (recording == null) {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else CameraSelector.LENS_FACING_BACK
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (isProcessing) {
            AiProcessingOverlay(
                stage = processingStage,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (showLimitDialog) {
            AlertDialog(
                onDismissRequest = { showLimitDialog = false },
                title = { Text("Лимит исчерпан 💀") },
                text = {
                    Text(
                        "12 из 12 фото за сегодня. Всё, брат, нейросеть перегрелась 🤝\n\n" +
                            "Приходи завтра. Или возьми PREMIUM — лимит останется 12, " +
                            "но у тебя будет галочка престижа ✅"
                    )
                },
                confirmButton = {
                    Button(onClick = { showLimitDialog = false; onOpenPremium() }) {
                        Text("ХОЧУ ГАЛОЧКУ ✅")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLimitDialog = false }) {
                        Text("приду завтра 😭")
                    }
                },
            )
        }
    }
}

/** Молодёжная пилюля: полупрозрачная, скруглённая, с эмодзи. */
@Composable
private fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0x99000000),
    textColor: Color = Color.White,
    bold: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Text(
        text,
        color = textColor,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(color, RoundedCornerShape(50))
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun TopBar(
    prefs: Prefs,
    photosLeft: Int,
    isRecording: Boolean,
    recordSeconds: Int,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xCC000000), Color.Transparent)
                )
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Pill(
                text = if (prefs.isPremium) "⭐ PREMIUM ✅" else "🆓 FREE → premium",
                color = if (prefs.isPremium) Color(0xCC7C4DFF) else Color(0x99000000),
                bold = prefs.isPremium,
                onClick = onOpenPremium,
            )
            Pill(text = "📸 $photosLeft/12", bold = true)
        }
        // Единственный режим. Переключалок нет и не будет — сразу максимум.
        Pill(
            text = "✨ AI ${EblanAlgorithms.MODE_NAME} ✅",
            color = Color(0xB3311B92),
            bold = true,
        )
        if (isRecording) {
            val limitLabel = if (prefs.isPremium) "БЕЗЛИМИТ (до 1:00) 🤝" else "лимит 1:00"
            Pill(
                text = "🔴 REC %d:%02d · %s".format(
                    recordSeconds / 60, recordSeconds % 60, limitLabel
                ),
                color = Color(0xCC7F0000),
                bold = true,
            )
        }
    }
}

@Composable
private fun BottomBar(
    isVideoMode: Boolean,
    isRecording: Boolean,
    isProcessing: Boolean,
    onModeChange: (Boolean) -> Unit,
    onShutter: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xCC000000))
                )
            )
            .padding(top = 28.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Фото/видео — не режимы, а призвание.
        Row(
            modifier = Modifier.background(Color(0x66000000), RoundedCornerShape(50)),
        ) {
            Pill(
                text = "📸 ФОТО",
                color = if (!isVideoMode) Color(0xE6FFFFFF) else Color.Transparent,
                textColor = if (!isVideoMode) Color.Black else Color.White,
                bold = !isVideoMode,
                onClick = { onModeChange(false) },
            )
            Pill(
                text = "🎬 ВИДЕО",
                color = if (isVideoMode) Color(0xE6FFFFFF) else Color.Transparent,
                textColor = if (isVideoMode) Color.Black else Color.White,
                bold = isVideoMode,
                onClick = { onModeChange(true) },
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            ShutterButton(
                isVideoMode = isVideoMode,
                isRecording = isRecording,
                enabled = !isProcessing,
                onClick = onShutter,
                modifier = Modifier.align(Alignment.Center),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 36.dp)
                    .size(52.dp)
                    .background(Color(0x66FFFFFF), CircleShape)
                    .clickable(onClick = onSwitchCamera),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Cameraswitch,
                    contentDescription = "Сменить камеру",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ShutterButton(
    isVideoMode: Boolean,
    isRecording: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val innerColor by animateColorAsState(
        targetValue = when {
            isRecording -> Color(0xFFFF3B30)
            isVideoMode -> Color(0xFFFF5252)
            else -> Color.White
        },
        label = "shutter",
    )
    Box(
        modifier = modifier
            .size(86.dp)
            .border(5.dp, Color.White, CircleShape)
            .padding(10.dp)
            .background(
                innerColor,
                if (isRecording) RoundedCornerShape(10.dp) else CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick),
    )
}

@Composable
private fun AiProcessingOverlay(stage: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 32.dp)
            .background(Color(0xE60B0B0F), RoundedCornerShape(24.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("✨", style = MaterialTheme.typography.displaySmall)
        CircularProgressIndicator(color = Color(0xFF7C4DFF))
        Text(
            "AI ${EblanAlgorithms.MODE_NAME}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
        // Это не анимация для красоты — реальный этап пайплайна прямо сейчас.
        Text(
            stage,
            color = Color(0xFFCCCCDD),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.height(40.dp),
        )
    }
}

/** ProcessCameraProvider через suspend, чтобы не таскать ListenableFuture по коду. */
private suspend fun Context.cameraProvider(): ProcessCameraProvider {
    return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            { cont.resume(future.get()) { } },
            ContextCompat.getMainExecutor(this),
        )
    }
}
