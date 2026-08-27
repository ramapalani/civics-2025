package com.ramapalani.civics2025.ui.guide

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ramapalani.civics2025.data.StudyGuideStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private const val ASSET_TEXT = "study-guide-text.json"

@Composable
fun StudyGuideScreen(
    startPage: Int,
    title: String,
    showQuestionButton: Boolean,
    onBackToQuestion: () -> Unit,
    onHome: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var pdfReady by remember { mutableStateOf(StudyGuideStore.isPresent(context)) }
    var downloading by remember { mutableStateOf(false) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(-1L) }
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var descriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var pageIndex by remember { mutableIntStateOf((startPage - 1).coerceAtLeast(0)) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var extraZoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var error by remember { mutableStateOf<String?>(null) }
    var showText by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    val pageTexts = remember {
        runCatching {
            context.assets.open(ASSET_TEXT).bufferedReader().use {
                Json.decodeFromString<List<String>>(it.readText())
            }
        }.getOrDefault(emptyList())
    }
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.US
            }
        }
        engine
    }

    DisposableEffect(pdfReady) {
        if (pdfReady) {
            try {
                val cached = StudyGuideStore.file(context)
                val pfd = ParcelFileDescriptor.open(cached, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdf = PdfRenderer(pfd)
                descriptor = pfd
                renderer = pdf
                pageCount = pdf.pageCount
                pageIndex = (startPage - 1).coerceIn(0, (pdf.pageCount - 1).coerceAtLeast(0))
                error = null
            } catch (ex: Exception) {
                error = "Could not open the textbook. ${ex.message ?: ""}".trim()
            }
        }
        onDispose {
            speaking = false
            tts.stop()
            renderer?.close()
            renderer = null
            descriptor?.close()
            descriptor = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speaking = false
            tts.stop()
            tts.shutdown()
        }
    }

    LaunchedEffect(renderer, pageIndex) {
        tts.stop()
        speaking = false
        val pdf = renderer ?: return@LaunchedEffect
        pdf.openPage(pageIndex).use { page ->
            val targetWidthPx = with(density) { 900.dp.roundToPx() }
            val scaleUp = (targetWidthPx.toFloat() / page.width.toFloat()).coerceIn(2f, 3.5f)
            val width = (page.width * scaleUp).toInt().coerceAtLeast(1)
            val height = (page.height * scaleUp).toInt().coerceAtLeast(1)
            val next = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(next, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap = next
            extraZoom = 1f
            offset = Offset.Zero
        }
    }

    val pageText = pageTexts.getOrNull(pageIndex).orEmpty()

    fun toggleSpeak() {
        if (speaking) {
            tts.stop()
            speaking = false
            return
        }
        if (pageText.isBlank()) return
        speaking = true
        pageText.chunked(3500).forEachIndexed { i, chunk ->
            val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(chunk, mode, null, "page-$pageIndex-$i")
        }
    }

    fun startDownload() {
        if (downloading) return
        downloading = true
        error = null
        downloadedBytes = 0L
        totalBytes = -1L
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    StudyGuideStore.download(context) { got, total ->
                        scope.launch(Dispatchers.Main.immediate) {
                            downloadedBytes = got
                            totalBytes = total
                        }
                    }
                }
                pdfReady = true
            } catch (ex: Exception) {
                error = "Could not download the textbook. ${ex.message ?: "Check your connection and try again."}"
            } finally {
                downloading = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF1B365D))) {
        val loadError = error
        val page = bitmap
        when {
            !pdfReady -> {
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Official study guide",
                        color = Color(0xFFF6F1E8),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        "The study guide's page text is already on this phone for Text and Listen. To see the original PDF pages, tap below to download the official PDF from USCIS (about 40 MB). It stays on this phone for later reading.",
                        color = Color(0xFFF6F1E8),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (downloading) {
                        val total = totalBytes
                        if (total > 0) {
                            LinearProgressIndicator(
                                progress = { (downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                "${downloadedBytes / 1_000_000} / ${total / 1_000_000} MB",
                                color = Color(0xFFF6F1E8),
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("Downloading from USCIS…", color = Color(0xFFF6F1E8))
                        }
                    } else {
                        Button(onClick = { startDownload() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Download from USCIS")
                        }
                    }
                    if (loadError != null && !downloading) {
                        Text(loadError, color = Color(0xFFF6F1E8))
                    }
                }
            }
            loadError != null -> Text(
                loadError,
                color = Color.White,
                modifier = Modifier.padding(16.dp).align(Alignment.Center),
            )
            showText -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Text(
                        pageText.ifBlank { "No text could be extracted from this page." },
                        color = Color(0xFFF6F1E8),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            page != null -> {
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                ) {
                    val viewW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                    val viewH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                    val pageW = page.width.toFloat()
                    val pageH = page.height.toFloat()
                    val fit = min(viewW / pageW, viewH / pageH)
                    val cover = max(viewW / pageW, viewH / pageH)
                    val totalScale = (cover * extraZoom).coerceIn(fit, cover * 4f)
                    val scaledW = pageW * totalScale
                    val scaledH = pageH * totalScale
                    val maxX = ((scaledW - viewW) / 2f).coerceAtLeast(0f)
                    val maxY = ((scaledH - viewH) / 2f).coerceAtLeast(0f)
                    val clamped = Offset(
                        offset.x.coerceIn(-maxX, maxX),
                        offset.y.coerceIn(-maxY, maxY),
                    )
                    val minExtra = fit / cover
                    val transform = rememberTransformableState { zoom, pan, _ ->
                        extraZoom = (extraZoom * zoom).coerceIn(minExtra, 4f)
                        offset += pan
                    }
                    Image(
                        bitmap = page.asImageBitmap(),
                        contentDescription = pageText.ifBlank { "Study guide page ${pageIndex + 1}" },
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(with(density) { pageW.toDp() })
                            .height(with(density) { pageH.toDp() })
                            .graphicsLayer(
                                scaleX = totalScale,
                                scaleY = totalScale,
                                translationX = clamped.x,
                                translationY = clamped.y,
                            )
                            .transformable(transform)
                            .pointerInput(fit, cover) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        extraZoom = if (extraZoom > 1.05f) 1f else fit / cover
                                        offset = Offset.Zero
                                    },
                                )
                            },
                    )
                }
            }
            else -> Text(
                "Opening the textbook…",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Surface(
            color = Color(0xCC1B365D),
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row {
                        CompactNavButton("Home", onClick = onHome)
                        if (showQuestionButton) {
                            CompactNavButton("Question", onClick = onBackToQuestion)
                        }
                    }
                    Row {
                        CompactNavButton("Prev", enabled = pageIndex > 0) {
                            pageIndex = (pageIndex - 1).coerceAtLeast(0)
                        }
                        Text(
                            "${pageIndex + 1}/${pageCount.coerceAtLeast(1)}",
                            color = Color(0xFFF6F1E8),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        CompactNavButton("Next", enabled = pageIndex < pageCount - 1) {
                            pageIndex = (pageIndex + 1).coerceAtMost((pageCount - 1).coerceAtLeast(0))
                        }
                    }
                    Row {
                        CompactNavButton(if (showText) "Page" else "Text") {
                            tts.stop()
                            speaking = false
                            showText = !showText
                        }
                        CompactNavButton(
                            if (speaking) "Stop" else "Listen",
                            enabled = pageText.isNotBlank(),
                            onClick = { toggleSpeak() },
                        )
                    }
                }
                Text(
                    title,
                    color = Color(0xFFF6F1E8),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun CompactNavButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        modifier = Modifier.height(32.dp),
    ) {
        Text(label, color = Color(0xFFF6F1E8), style = MaterialTheme.typography.labelMedium)
    }
}
