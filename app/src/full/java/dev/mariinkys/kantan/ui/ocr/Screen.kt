package dev.mariinkys.kantan.ui.ocr

import android.Manifest
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import dev.mariinkys.kantan.domain.model.DictionaryEntry
import kotlin.math.sqrt

private enum class DragMode {
    NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

private fun Offset.distanceTo(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt(dx * dx + dy * dy)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OcrScreen(
    onBack: () -> Unit,
    onEntryClick: (sequence: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OcrViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    if (cameraPermission.status.isGranted) {
        OcrContent(
            onBack = onBack,
            onEntryClick = onEntryClick,
            viewModel = viewModel,
            modifier = modifier
        )
    } else {
        PermissionDeniedPlaceholder(
            onBack = onBack,
            onRequest = { cameraPermission.launchPermissionRequest() },
            modifier = modifier
        )
    }
}

@Composable
private fun OcrContent(
    onBack: () -> Unit,
    onEntryClick: (sequence: Int) -> Unit,
    viewModel: OcrViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val recognizedWords by viewModel.recognizedWords.collectAsStateWithLifecycle()

    // Container pixel size (updated once the composable is laid out)
    var containerSize by remember { mutableStateOf(Size.Zero) }

    // Movable / resizable scan rectangle
    val scanRectState = remember { mutableStateOf(Rect.Zero) }
    var scanRect by scanRectState

    // We init the rect once we know the container dimensions
    LaunchedEffect(containerSize) {
        if (containerSize != Size.Zero && scanRect == Rect.Zero) {
            val w = containerSize.width
            val h = containerSize.height
            scanRect = Rect(
                left = w * 0.08f,
                top = h * 0.22f,
                right = w * 0.92f,
                bottom = h * 0.62f
            )
        }
    }

    // ML Kit Japanese recognizer
    val textRecognizer = remember {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }
    DisposableEffect(Unit) { onDispose { textRecognizer.close() } }

    // MlKitAnalyzer
    val mlKitAnalyzer = remember(textRecognizer) {
        MlKitAnalyzer(
            listOf(textRecognizer),
            CameraController.IMAGE_ANALYSIS,
            ContextCompat.getMainExecutor(context)
        ) { result ->
            val text = result.getValue(textRecognizer) ?: return@MlKitAnalyzer
            viewModel.processText(text, scanRectState.value)
        }
    }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context), mlKitAnalyzer)
            bindToLifecycle(lifecycleOwner)
        }
    }
    DisposableEffect(Unit) { onDispose { cameraController.unbind() } }

    val infiniteTransition = rememberInfiniteTransition(label = "scanBorder")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    var dragMode by remember { mutableStateOf(DragMode.NONE) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { s ->
                containerSize = Size(s.width.toFloat(), s.height.toFloat())
            }
    ) {

        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = cameraController
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Selection rectangle
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            if (scanRect == Rect.Zero) return@Canvas

            // Semi-transparent overlay
            drawRect(Color(0xCC000000))

            // Cut the scan window
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(scanRect.left, scanRect.top),
                size = Size(scanRect.width, scanRect.height),
                blendMode = BlendMode.Clear
            )
        }

        // Rect border + corner handles
        if (scanRect != Rect.Zero) {
            val primaryColor = MaterialTheme.colorScheme.primary

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 2.5.dp.toPx()
                val handleR = 10.dp.toPx()
                val lineLen = 24.dp.toPx()

                // Border
                drawRect(
                    color = primaryColor.copy(alpha = borderAlpha),
                    topLeft = Offset(scanRect.left, scanRect.top),
                    size = Size(scanRect.width, scanRect.height),
                    style = Stroke(width = strokePx)
                )

                // Corner L-shaped accents
                val corners = listOf(
                    Triple(Offset(scanRect.left, scanRect.top), 1f, 1f),
                    Triple(Offset(scanRect.right, scanRect.top), -1f, 1f),
                    Triple(Offset(scanRect.left, scanRect.bottom), 1f, -1f),
                    Triple(Offset(scanRect.right, scanRect.bottom), -1f, -1f),
                )
                for ((corner, sx, sy) in corners) {
                    // Horizontal arm
                    drawLine(
                        color = Color.White,
                        start = corner,
                        end = corner.copy(x = corner.x + sx * lineLen),
                        strokeWidth = strokePx * 1.8f
                    )
                    // Vertical arm
                    drawLine(
                        color = Color.White,
                        start = corner,
                        end = corner.copy(y = corner.y + sy * lineLen),
                        strokeWidth = strokePx * 1.8f
                    )
                    // Drag handle dot
                    drawCircle(
                        color = Color.White,
                        radius = handleR,
                        center = corner
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = handleR * 0.55f,
                        center = corner
                    )
                }

                // crosshair in the middle
                val cx = (scanRect.left + scanRect.right) / 2f
                val cy = (scanRect.top + scanRect.bottom) / 2f
                val cLen = 10.dp.toPx()
                drawLine(
                    Color.White.copy(alpha = 0.4f),
                    Offset(cx - cLen, cy),
                    Offset(cx + cLen, cy),
                    strokePx
                )
                drawLine(
                    Color.White.copy(alpha = 0.4f),
                    Offset(cx, cy - cLen),
                    Offset(cx, cy + cLen),
                    strokePx
                )
            }
        }

        // Touch / drag handler
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val hitRadius = 36.dp.toPx()
                    val minSizePx = 80.dp.toPx()

                    detectDragGestures(
                        onDragStart = { offset ->
                            dragMode = when {
                                offset.distanceTo(
                                    Offset(
                                        scanRect.left,
                                        scanRect.top
                                    )
                                ) < hitRadius -> DragMode.TOP_LEFT

                                offset.distanceTo(
                                    Offset(
                                        scanRect.right,
                                        scanRect.top
                                    )
                                ) < hitRadius -> DragMode.TOP_RIGHT

                                offset.distanceTo(
                                    Offset(
                                        scanRect.left,
                                        scanRect.bottom
                                    )
                                ) < hitRadius -> DragMode.BOTTOM_LEFT

                                offset.distanceTo(
                                    Offset(
                                        scanRect.right,
                                        scanRect.bottom
                                    )
                                ) < hitRadius -> DragMode.BOTTOM_RIGHT

                                scanRect.contains(offset) -> DragMode.MOVE
                                else -> DragMode.NONE
                            }
                        },
                        onDragEnd = { dragMode = DragMode.NONE },
                        onDragCancel = { dragMode = DragMode.NONE },
                        onDrag = { _, delta ->
                            val cw = containerSize.width
                            val ch = containerSize.height
                            scanRect = when (dragMode) {
                                DragMode.MOVE -> {
                                    val nl =
                                        (scanRect.left + delta.x).coerceIn(0f, cw - scanRect.width)
                                    val nt =
                                        (scanRect.top + delta.y).coerceIn(0f, ch - scanRect.height)
                                    Rect(nl, nt, nl + scanRect.width, nt + scanRect.height)
                                }

                                DragMode.TOP_LEFT -> Rect(
                                    left = (scanRect.left + delta.x).coerceIn(
                                        0f,
                                        scanRect.right - minSizePx
                                    ),
                                    top = (scanRect.top + delta.y).coerceIn(
                                        0f,
                                        scanRect.bottom - minSizePx
                                    ),
                                    right = scanRect.right,
                                    bottom = scanRect.bottom
                                )

                                DragMode.TOP_RIGHT -> Rect(
                                    left = scanRect.left,
                                    top = (scanRect.top + delta.y).coerceIn(
                                        0f,
                                        scanRect.bottom - minSizePx
                                    ),
                                    right = (scanRect.right + delta.x).coerceIn(
                                        scanRect.left + minSizePx,
                                        cw
                                    ),
                                    bottom = scanRect.bottom
                                )

                                DragMode.BOTTOM_LEFT -> Rect(
                                    left = (scanRect.left + delta.x).coerceIn(
                                        0f,
                                        scanRect.right - minSizePx
                                    ),
                                    top = scanRect.top,
                                    right = scanRect.right,
                                    bottom = (scanRect.bottom + delta.y).coerceIn(
                                        scanRect.top + minSizePx,
                                        ch
                                    )
                                )

                                DragMode.BOTTOM_RIGHT -> Rect(
                                    left = scanRect.left,
                                    top = scanRect.top,
                                    right = (scanRect.right + delta.x).coerceIn(
                                        scanRect.left + minSizePx,
                                        cw
                                    ),
                                    bottom = (scanRect.bottom + delta.y).coerceIn(
                                        scanRect.top + minSizePx,
                                        ch
                                    )
                                )

                                DragMode.NONE -> scanRect
                            }
                        }
                    )
                }
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Point at Japanese Text · Drag to Reposition",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom recognized-words panel
        RecognizedWordsPanel(
            words = recognizedWords,
            onWordClick = { entry -> entry?.let { onEntryClick(it.id) } },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun RecognizedWordsPanel(
    words: List<RecognizedWord>,
    onWordClick: (DictionaryEntry?) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = words.isNotEmpty(),
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.93f),
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.extraLarge.copy(
                bottomStart = androidx.compose.foundation.shape.CornerSize(0),
                bottomEnd = androidx.compose.foundation.shape.CornerSize(0)
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Small scanning indicator dot
                    Canvas(modifier = Modifier.size(6.dp)) {
                        drawCircle(Color(0xFF4CAF50))
                    }
                    Text(
                        text = "Recognized in Frame",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = words, key = { it.text }) { word ->
                        WordChip(
                            word = word,
                            onClick = { onWordClick(word.entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordChip(word: RecognizedWord, onClick: () -> Unit) {
    val hasMatch = word.entry != null

    if (hasMatch) {
        ElevatedFilterChip(
            selected = true,
            onClick = onClick,
            colors = FilterChipDefaults.elevatedFilterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.primary
            ),
            label = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = word.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (word.isConjugated) {
                        Text(
                            "→ ${word.entry.expression}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        Text(
                            word.entry.reading,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
    } else {
        // TODO: Maybe we don't show'em?
        // Not in dictionary: muted, informational only
        SuggestionChip(
            onClick = {},
            label = {
                Text(
                    text = word.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
private fun PermissionDeniedPlaceholder(
    onBack: () -> Unit,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Camera access needed",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Grant camera permission to use live Japanese OCR.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Button(onClick = onRequest) { Text("Grant Permission") }

            TextButton(onClick = onBack) { Text("Go Back") }
        }
    }
}