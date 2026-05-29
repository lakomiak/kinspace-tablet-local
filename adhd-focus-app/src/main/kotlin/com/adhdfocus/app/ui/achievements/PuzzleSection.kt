package com.adhdfocus.app.ui.achievements

import android.media.AudioManager
import android.media.ToneGenerator
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import com.adhdfocus.app.data.model.PuzzleProgress
import com.adhdfocus.app.domain.puzzle.PuzzleAgeBand
import com.adhdfocus.app.domain.puzzle.PuzzleCatalog
import com.adhdfocus.app.domain.puzzle.PuzzleDefinition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleSection(
    selectedAgeBand: PuzzleAgeBand,
    currentPuzzle: PuzzleProgress?,
    modifier: Modifier = Modifier
) {
    val puzzleDefinition = currentPuzzle?.let {
        PuzzleCatalog.definitionFor(PuzzleAgeBand.fromKey(it.ageBandKey), it.cycleIndex)
    } ?: PuzzleCatalog.definitionFor(selectedAgeBand, 0)
    val unlockedPieces = currentPuzzle?.piecesUnlocked ?: 0
    val totalPieces = currentPuzzle?.totalPieces ?: 30
    val progress = if (totalPieces > 0) (unlockedPieces * 100) / totalPieces else 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFFDF8FF),
                            Color(0xFFF1E9FB),
                            Color(0xFFE3DDF0)
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Puzzle Progress",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF101336)
                )
                Text(
                    text = "Complete the day to reveal one new piece. A finished puzzle automatically starts the next one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF443A61)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xE6FFF8FF)
                    ) {
                        Text(
                            text = selectedAgeBand.ageRangeLabel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFF2C214A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = puzzleDefinition.title,
                        color = Color(0xFF101336),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = puzzleDefinition.subtitle,
                        color = Color(0xFF5A5275),
                        fontSize = 14.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFF7F1FD)
                ) {
                    Text(
                        text = "Reveal pieces",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color(0xFF2C214A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            PuzzleRevealCard(
                puzzleDefinition = puzzleDefinition,
                piecesUnlocked = unlockedPieces,
                totalPieces = totalPieces
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = {}, label = { Text("$unlockedPieces/$totalPieces pieces") })
                AssistChip(onClick = {}, label = { Text("$progress% revealed") })
            }

            Text(
                text = if (unlockedPieces >= totalPieces) {
                    "Puzzle complete. The next scene begins automatically."
                } else {
                    "Finish the day to remove the next puzzle piece."
                },
                color = Color(0xFF443A61),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PuzzleRevealCard(
    puzzleDefinition: PuzzleDefinition,
    piecesUnlocked: Int,
    totalPieces: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageResId = remember(puzzleDefinition.imageUrl) {
        context.resources.getIdentifier(
            puzzleDefinition.imageUrl,
            "drawable",
            context.packageName
        )
    }
    var lastAnimatedUnlocked by remember(puzzleDefinition.puzzleKey) { mutableIntStateOf(piecesUnlocked) }
    var highlightedPieceIndex by remember(puzzleDefinition.puzzleKey) { mutableIntStateOf(-1) }
    var showCompletionCelebration by remember(puzzleDefinition.puzzleKey) { mutableStateOf(false) }
    var lastCompletionShownForKey by remember { mutableStateOf<String?>(null) }
    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70) }.getOrNull()
    }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator?.release()
        }
    }

    LaunchedEffect(puzzleDefinition.puzzleKey, piecesUnlocked) {
        if (piecesUnlocked > lastAnimatedUnlocked) {
            highlightedPieceIndex = (piecesUnlocked - 1).coerceAtLeast(0)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
            lastAnimatedUnlocked = piecesUnlocked
            delay(1400)
            highlightedPieceIndex = -1
        } else {
            lastAnimatedUnlocked = piecesUnlocked
        }
    }

    LaunchedEffect(puzzleDefinition.puzzleKey, piecesUnlocked, totalPieces) {
        val completionKey = "${puzzleDefinition.puzzleKey}-$piecesUnlocked"
        if (piecesUnlocked >= totalPieces && lastCompletionShownForKey != completionKey) {
            lastCompletionShownForKey = completionKey
            showCompletionCelebration = true
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 220)
            launch {
                delay(160)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 220)
            }
            launch {
                delay(320)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 320)
            }
            delay(2400)
            showCompletionCelebration = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(Color.White, Color(0xFFBFAFD9))),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = if (imageResId != 0) {
                    androidx.compose.ui.res.painterResource(id = imageResId)
                } else {
                    ColorPainter(Color(0xFFE8DFF5))
                },
                contentDescription = puzzleDefinition.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x33101336))
            )

            PuzzleTileOverlay(
                piecesUnlocked = piecesUnlocked,
                totalPieces = totalPieces,
                highlightedPieceIndex = highlightedPieceIndex,
                modifier = Modifier.fillMaxSize()
            )

            if (showCompletionCelebration) {
                PuzzleCompletionCelebration(
                    title = puzzleDefinition.title,
                    modifier = Modifier.fillMaxSize(),
                    onDismiss = { showCompletionCelebration = false }
                )
            }
        }
    }
}

@Composable
private fun PuzzleTileOverlay(
    piecesUnlocked: Int,
    totalPieces: Int,
    highlightedPieceIndex: Int,
    modifier: Modifier = Modifier
) {
    val rows = 6
    val columns = 5
    val pieceLiftProgress by animateFloatAsState(
        targetValue = if (highlightedPieceIndex >= 0) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "puzzlePieceLiftProgress"
    )
    val highlightGlowAlpha by animateFloatAsState(
        targetValue = if (highlightedPieceIndex >= 0) 0.95f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "puzzleHighlightGlowAlpha"
    )

    Canvas(modifier = modifier.padding(14.dp)) {
        val boardPadding = size.minDimension * 0.018f
        val innerWidth = size.width - boardPadding * 2f
        val innerHeight = size.height - boardPadding * 2f
        val gap = size.minDimension * 0.014f
        val cellWidth = (innerWidth - gap * (columns - 1)) / columns
        val cellHeight = (innerHeight - gap * (rows - 1)) / rows
        val boardRect = Rect(
            left = boardPadding,
            top = boardPadding,
            right = boardPadding + innerWidth,
            bottom = boardPadding + innerHeight
        )
        val boardCornerRadius = size.minDimension * 0.06f
        val tileCornerRadius = size.minDimension * 0.02f
        val revealedCount = piecesUnlocked.coerceIn(0, totalPieces)
        val pieceNumberPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 15.sp.toPx()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f, 0f, 1f, android.graphics.Color.argb(120, 6, 4, 17))
        }
        val tiles = buildList {
            repeat(rows) { row ->
                repeat(columns) { column ->
                    val left = boardPadding + column * (cellWidth + gap)
                    val top = boardPadding + row * (cellHeight + gap)
                    add(
                        PuzzlePieceGeometry(
                            index = row * columns + column,
                            rect = Rect(
                                left = left,
                                top = top,
                                right = left + cellWidth,
                                bottom = top + cellHeight
                            ),
                            path = Path()
                        )
                    )
                }
            }
        }
        val highlightedTile = tiles.firstOrNull { it.index == highlightedPieceIndex }

        drawRoundRect(
            color = Color(0x220A0712),
            topLeft = Offset(boardRect.left, boardRect.top + 8.dp.toPx()),
            size = boardRect.size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(boardCornerRadius, boardCornerRadius)
        )

        tiles.forEach { tile ->
            val isUnlocked = tile.index < revealedCount
            val isHighlighted = tile.index == highlightedPieceIndex

            if (!isUnlocked && !isHighlighted) {
                drawRoundRect(
                    color = Color(0xFF171427),
                    topLeft = tile.rect.topLeft,
                    size = tile.rect.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(tileCornerRadius, tileCornerRadius)
                )
            }

            drawIntoCanvas { canvas ->
                pieceNumberPaint.alpha = when {
                    isHighlighted -> 255
                    isUnlocked -> 150
                    else -> 220
                }
                canvas.nativeCanvas.drawText(
                    "${tile.index + 1}",
                    tile.rect.left + 8.dp.toPx(),
                    tile.rect.top + 18.dp.toPx(),
                    pieceNumberPaint
                )
            }
        }

        highlightedTile?.let { tile ->
            val progress = pieceLiftProgress.coerceIn(0f, 1f)
            val liftDx = 14.dp.toPx() * progress
            val liftDy = (-34).dp.toPx() * progress
            val rotation = 5f * progress
            val fade = 1f - (progress * 0.88f)

            translate(left = liftDx, top = liftDy) {
                rotate(degrees = rotation, pivot = tile.rect.center) {
                    drawRoundRect(
                        color = Color(0x220B0417).copy(alpha = 0.5f * fade),
                        topLeft = tile.rect.topLeft,
                        size = tile.rect.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(tileCornerRadius, tileCornerRadius)
                    )
                    drawRoundRect(
                        color = Color(0xFFF7F2FF),
                        topLeft = tile.rect.topLeft,
                        size = tile.rect.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(tileCornerRadius, tileCornerRadius),
                        alpha = fade
                    )
                }
            }

            if (progress > 0f) {
                drawPuzzleSparkleBurst(
                    center = Offset(
                        x = tile.rect.center.x + (size.width * 0.12f * progress),
                        y = tile.rect.center.y - (size.height * 0.16f * progress)
                    ),
                    progress = progress
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPuzzleSparkleBurst(
    center: Offset,
    progress: Float,
) {
    val alpha = (1f - progress).coerceIn(0f, 1f)
    val sparkleColor = Color(0xFFFFF2A8).copy(alpha = alpha)
    val shimmerColor = Color.White.copy(alpha = alpha * 0.9f)
    val points = listOf(
        center.copy(x = center.x - size.width * 0.1f, y = center.y + size.height * 0.015f),
        center.copy(x = center.x - size.width * 0.03f, y = center.y - size.height * 0.09f),
        center.copy(x = center.x + size.width * 0.07f, y = center.y - size.height * 0.04f),
        center.copy(x = center.x + size.width * 0.11f, y = center.y + size.height * 0.03f)
    )

    points.forEachIndexed { index, point ->
        val radius = size.minDimension * (0.018f + (index * 0.003f))
        drawCircle(color = sparkleColor, radius = radius, center = point)
        drawLine(
            color = shimmerColor,
            start = Offset(point.x - radius * 1.3f, point.y),
            end = Offset(point.x + radius * 1.3f, point.y),
            strokeWidth = radius * 0.35f
        )
        drawLine(
            color = shimmerColor,
            start = Offset(point.x, point.y - radius * 1.3f),
            end = Offset(point.x, point.y + radius * 1.3f),
            strokeWidth = radius * 0.35f
        )
    }
}

@Composable
private fun PuzzleCompletionCelebration(
    title: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val bannerScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 420),
        label = "celebrationBannerScale"
    )
    val bannerAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 420),
        label = "celebrationBannerAlpha"
    )

    Box(
        modifier = modifier
            .background(Color(0x6B140B21))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        CelebrationBackdrop(modifier = Modifier.fillMaxSize())

        Card(
            modifier = Modifier
                .padding(28.dp)
                .scale(bannerScale)
                .graphicsLayer { alpha = bannerAlpha },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFDF7E6)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Puzzle Complete!",
                    color = Color(0xFF40205E),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                )
                Text(
                    text = title,
                    color = Color(0xFF5A3A72),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "You revealed the whole picture. The next puzzle is ready when you are.",
                    color = Color(0xFF5F5871),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun CelebrationBackdrop(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val rayColor = Color(0xFFFFF0B8).copy(alpha = 0.42f)
        val confettiColors = listOf(
            Color(0xFFFFD166),
            Color(0xFF7BDFF2),
            Color(0xFFB2F7EF),
            Color(0xFFF7A072),
            Color(0xFFCDB4DB)
        )
        val center = Offset(size.width / 2f, size.height * 0.38f)
        repeat(12) { index ->
            val angle = (index * 30f) * (Math.PI / 180f).toFloat()
            val rayLength = size.minDimension * 0.34f
            val end = Offset(
                x = center.x + kotlin.math.cos(angle) * rayLength,
                y = center.y + kotlin.math.sin(angle) * rayLength
            )
            drawLine(
                color = rayColor,
                start = center,
                end = end,
                strokeWidth = size.minDimension * 0.012f
            )
        }

        repeat(28) { index ->
            val x = size.width * ((index * 37 % 100) / 100f)
            val y = size.height * (0.12f + ((index * 19 % 70) / 100f))
            val color = confettiColors[index % confettiColors.size]
            drawCircle(
                color = color.copy(alpha = 0.95f),
                radius = size.minDimension * (0.008f + (index % 3) * 0.003f),
                center = Offset(x, y)
            )
        }
    }
}

private enum class PuzzleTabDirection {
    FLAT,
    OUT,
    IN
}

private data class PuzzleBoardLayout(
    val rowFractions: List<Float>,
    val columnFractions: List<Float>,
    val pieceSpecs: List<PuzzlePieceMaskSpec>
) {
    val rows: Int get() = rowFractions.size
    val columns: Int get() = columnFractions.size
}

private data class PuzzlePieceMaskSpec(
    val index: Int,
    val row: Int,
    val column: Int,
    val top: PuzzleTabDirection,
    val right: PuzzleTabDirection,
    val bottom: PuzzleTabDirection,
    val left: PuzzleTabDirection,
    val topProfile: PuzzleEdgeProfile?,
    val rightProfile: PuzzleEdgeProfile?,
    val bottomProfile: PuzzleEdgeProfile?,
    val leftProfile: PuzzleEdgeProfile?
)

private data class PuzzlePieceGeometry(
    val index: Int,
    val rect: Rect,
    val path: Path
)

private data class PuzzleEdgeProfile(
    val centerBias: Float,
    val spanScale: Float,
    val depthScale: Float,
    val shoulderScale: Float,
    val neckScale: Float = 1f
)

private val AuthoredPuzzleMaskLayout = PuzzleBoardLayout(
    rowFractions = listOf(0.16f, 0.18f, 0.18f, 0.17f, 0.16f, 0.15f),
    columnFractions = listOf(0.155f, 0.18f, 0.195f, 0.225f, 0.245f),
    pieceSpecs = buildAuthoredPuzzlePieceSpecs(
        horizontalDirections = listOf(
            listOf(PuzzleTabDirection.OUT, PuzzleTabDirection.IN, PuzzleTabDirection.OUT, PuzzleTabDirection.IN, PuzzleTabDirection.OUT),
            listOf(PuzzleTabDirection.IN, PuzzleTabDirection.OUT, PuzzleTabDirection.IN, PuzzleTabDirection.OUT, PuzzleTabDirection.IN),
            listOf(PuzzleTabDirection.OUT, PuzzleTabDirection.OUT, PuzzleTabDirection.IN, PuzzleTabDirection.IN, PuzzleTabDirection.OUT),
            listOf(PuzzleTabDirection.IN, PuzzleTabDirection.OUT, PuzzleTabDirection.OUT, PuzzleTabDirection.IN, PuzzleTabDirection.OUT),
            listOf(PuzzleTabDirection.OUT, PuzzleTabDirection.IN, PuzzleTabDirection.OUT, PuzzleTabDirection.OUT, PuzzleTabDirection.IN)
        ),
        verticalDirections = listOf(
            listOf(PuzzleTabDirection.OUT, PuzzleTabDirection.IN, PuzzleTabDirection.OUT, PuzzleTabDirection.IN),
            listOf(PuzzleTabDirection.IN, PuzzleTabDirection.OUT, PuzzleTabDirection.IN, PuzzleTabDirection.OUT),
            listOf(PuzzleTabDirection.OUT, PuzzleTabDirection.OUT, PuzzleTabDirection.IN, PuzzleTabDirection.IN),
            listOf(PuzzleTabDirection.IN, PuzzleTabDirection.OUT, PuzzleTabDirection.OUT, PuzzleTabDirection.IN),
            listOf(PuzzleTabDirection.OUT, PuzzleTabDirection.IN, PuzzleTabDirection.OUT, PuzzleTabDirection.OUT),
            listOf(PuzzleTabDirection.IN, PuzzleTabDirection.OUT, PuzzleTabDirection.IN, PuzzleTabDirection.OUT)
        ),
        horizontalProfiles = listOf(
        listOf(
            PuzzleEdgeProfile(-0.18f, 0.92f, 1.08f, 0.82f),
            PuzzleEdgeProfile(0.12f, 1.04f, 1.08f, 0.88f, 1.08f),
            PuzzleEdgeProfile(-0.07f, 0.9f, 1.2f, 0.78f, 0.92f),
            PuzzleEdgeProfile(0.16f, 1.12f, 1.1f, 0.86f, 1.12f),
            PuzzleEdgeProfile(-0.11f, 0.98f, 1.16f, 0.84f, 0.96f)
        ),
        listOf(
            PuzzleEdgeProfile(0.09f, 1.08f, 1.02f, 0.9f, 1.1f),
            PuzzleEdgeProfile(-0.16f, 0.94f, 1.2f, 0.76f, 0.88f),
            PuzzleEdgeProfile(0.18f, 1.1f, 1.08f, 0.82f, 1.06f),
            PuzzleEdgeProfile(-0.12f, 0.88f, 1.24f, 0.74f, 0.86f),
            PuzzleEdgeProfile(0.05f, 1.02f, 1.02f, 0.9f, 1.08f)
        ),
        listOf(
            PuzzleEdgeProfile(-0.14f, 1.0f, 1.18f, 0.8f, 0.94f),
            PuzzleEdgeProfile(0.1f, 0.9f, 1.22f, 0.72f, 0.82f),
            PuzzleEdgeProfile(-0.03f, 1.16f, 1.02f, 0.94f, 1.16f),
            PuzzleEdgeProfile(0.15f, 0.96f, 1.12f, 0.86f, 1.04f),
            PuzzleEdgeProfile(-0.17f, 1.06f, 1.18f, 0.8f, 0.9f)
        ),
        listOf(
            PuzzleEdgeProfile(0.07f, 0.94f, 1.22f, 0.78f, 0.88f),
            PuzzleEdgeProfile(-0.19f, 1.1f, 1.02f, 0.9f, 1.14f),
            PuzzleEdgeProfile(0.14f, 1.02f, 1.18f, 0.82f, 1.02f),
            PuzzleEdgeProfile(-0.08f, 0.92f, 1.12f, 0.84f, 0.94f),
            PuzzleEdgeProfile(0.18f, 1.08f, 1.0f, 0.9f, 1.12f)
        ),
        listOf(
            PuzzleEdgeProfile(-0.1f, 1.12f, 1.12f, 0.86f, 1.1f),
            PuzzleEdgeProfile(0.13f, 0.9f, 1.24f, 0.74f, 0.84f),
            PuzzleEdgeProfile(-0.15f, 1.0f, 1.16f, 0.82f, 0.96f),
            PuzzleEdgeProfile(0.06f, 0.96f, 1.04f, 0.88f, 1.08f),
            PuzzleEdgeProfile(-0.04f, 1.14f, 1.14f, 0.84f, 1.12f)
        )
    ),
        verticalProfiles = listOf(
        listOf(
            PuzzleEdgeProfile(-0.16f, 0.96f, 1.16f, 0.82f, 0.9f),
            PuzzleEdgeProfile(0.14f, 1.08f, 1.02f, 0.9f, 1.08f),
            PuzzleEdgeProfile(-0.08f, 0.9f, 1.22f, 0.72f, 0.82f),
            PuzzleEdgeProfile(0.18f, 1.04f, 1.1f, 0.86f, 1.1f)
        ),
        listOf(
            PuzzleEdgeProfile(0.11f, 1.1f, 1.04f, 0.9f, 1.14f),
            PuzzleEdgeProfile(-0.17f, 0.92f, 1.2f, 0.76f, 0.86f),
            PuzzleEdgeProfile(0.06f, 1.02f, 1.16f, 0.84f, 1.04f),
            PuzzleEdgeProfile(-0.12f, 1.12f, 1.02f, 0.92f, 1.12f)
        ),
        listOf(
            PuzzleEdgeProfile(-0.05f, 0.88f, 1.26f, 0.7f, 0.82f),
            PuzzleEdgeProfile(0.16f, 1.06f, 1.08f, 0.86f, 1.08f),
            PuzzleEdgeProfile(-0.18f, 0.98f, 1.18f, 0.78f, 0.9f),
            PuzzleEdgeProfile(0.08f, 1.14f, 1.04f, 0.9f, 1.16f)
        ),
        listOf(
            PuzzleEdgeProfile(0.15f, 1.04f, 1.16f, 0.82f, 1.04f),
            PuzzleEdgeProfile(-0.09f, 0.9f, 1.2f, 0.74f, 0.88f),
            PuzzleEdgeProfile(0.13f, 1.12f, 1.02f, 0.9f, 1.12f),
            PuzzleEdgeProfile(-0.14f, 0.96f, 1.16f, 0.8f, 0.92f)
        ),
        listOf(
            PuzzleEdgeProfile(-0.19f, 1.08f, 1.12f, 0.86f, 1.1f),
            PuzzleEdgeProfile(0.07f, 0.94f, 1.24f, 0.72f, 0.84f),
            PuzzleEdgeProfile(-0.02f, 1.0f, 1.14f, 0.84f, 0.96f),
            PuzzleEdgeProfile(0.17f, 1.16f, 1.02f, 0.94f, 1.14f)
        ),
        listOf(
            PuzzleEdgeProfile(0.1f, 0.92f, 1.24f, 0.76f, 0.88f),
            PuzzleEdgeProfile(-0.15f, 1.06f, 1.08f, 0.86f, 1.06f),
            PuzzleEdgeProfile(0.18f, 0.98f, 1.18f, 0.78f, 0.92f),
            PuzzleEdgeProfile(-0.11f, 1.1f, 1.04f, 0.9f, 1.1f)
        )
    ))
)

private fun buildPuzzlePiecePath(
    rect: Rect,
    spec: PuzzlePieceMaskSpec
): Path {
    return Path().apply {
        addPuzzlePiecePath(
            rect = rect,
            top = spec.top,
            right = spec.right,
            bottom = spec.bottom,
            left = spec.left,
            topProfile = spec.topProfile,
            rightProfile = spec.rightProfile,
            bottomProfile = spec.bottomProfile,
            leftProfile = spec.leftProfile
        )
    }
}

private fun oppositeTab(direction: PuzzleTabDirection): PuzzleTabDirection = when (direction) {
    PuzzleTabDirection.OUT -> PuzzleTabDirection.IN
    PuzzleTabDirection.IN -> PuzzleTabDirection.OUT
    PuzzleTabDirection.FLAT -> PuzzleTabDirection.FLAT
}

private fun buildAuthoredPuzzlePieceSpecs(
    horizontalDirections: List<List<PuzzleTabDirection>>,
    verticalDirections: List<List<PuzzleTabDirection>>,
    horizontalProfiles: List<List<PuzzleEdgeProfile>>,
    verticalProfiles: List<List<PuzzleEdgeProfile>>
): List<PuzzlePieceMaskSpec> {
    val rows = verticalDirections.size
    val columns = horizontalDirections.firstOrNull()?.size ?: 0
    return buildList {
        repeat(rows) { row ->
            repeat(columns) { column ->
                add(
                    PuzzlePieceMaskSpec(
                        index = row * columns + column,
                        row = row,
                        column = column,
                        top = if (row == 0) PuzzleTabDirection.FLAT else oppositeTab(horizontalDirections[row - 1][column]),
                        right = if (column == columns - 1) PuzzleTabDirection.FLAT else verticalDirections[row][column],
                        bottom = if (row == rows - 1) PuzzleTabDirection.FLAT else horizontalDirections[row][column],
                        left = if (column == 0) PuzzleTabDirection.FLAT else oppositeTab(verticalDirections[row][column - 1]),
                        topProfile = if (row == 0) null else horizontalProfiles[row - 1][column],
                        rightProfile = if (column == columns - 1) null else verticalProfiles[row][column],
                        bottomProfile = if (row == rows - 1) null else horizontalProfiles[row][column],
                        leftProfile = if (column == 0) null else verticalProfiles[row][column - 1]
                    )
                )
            }
        }
    }
}

private fun buildBoardEdges(start: Float, totalExtent: Float, fractions: List<Float>): FloatArray {
    val edges = FloatArray(fractions.size + 1)
    edges[0] = start
    var cursor = start
    fractions.forEachIndexed { index, fraction ->
        cursor += totalExtent * fraction
        edges[index + 1] = cursor
    }
    edges[edges.lastIndex] = start + totalExtent
    return edges
}

private fun androidx.compose.ui.graphics.Path.addPuzzlePiecePath(
    size: Size,
    top: PuzzleTabDirection,
    right: PuzzleTabDirection,
    bottom: PuzzleTabDirection,
    left: PuzzleTabDirection,
    topProfile: PuzzleEdgeProfile?,
    rightProfile: PuzzleEdgeProfile?,
    bottomProfile: PuzzleEdgeProfile?,
    leftProfile: PuzzleEdgeProfile?
) {
    val width = size.width
    val height = size.height

    moveTo(0f, 0f)
    addHorizontalTab(
        edgeStart = 0f,
        edgeEnd = width,
        edge = 0f,
        direction = top,
        outward = true,
        pieceExtent = height,
        profile = topProfile
    )
    addVerticalTab(
        edgeStart = 0f,
        edgeEnd = height,
        edge = width,
        direction = right,
        outward = true,
        pieceExtent = width,
        profile = rightProfile
    )
    addHorizontalTab(
        edgeStart = width,
        edgeEnd = 0f,
        edge = height,
        direction = bottom,
        outward = false,
        pieceExtent = height,
        profile = bottomProfile
    )
    addVerticalTab(
        edgeStart = height,
        edgeEnd = 0f,
        edge = 0f,
        direction = left,
        outward = false,
        pieceExtent = width,
        profile = leftProfile
    )
    close()
}

private fun androidx.compose.ui.graphics.Path.addPuzzlePiecePath(
    rect: Rect,
    top: PuzzleTabDirection,
    right: PuzzleTabDirection,
    bottom: PuzzleTabDirection,
    left: PuzzleTabDirection,
    topProfile: PuzzleEdgeProfile?,
    rightProfile: PuzzleEdgeProfile?,
    bottomProfile: PuzzleEdgeProfile?,
    leftProfile: PuzzleEdgeProfile?
) {
    val path = Path().apply {
        addPuzzlePiecePath(
            size = Size(rect.width, rect.height),
            top = top,
            right = right,
            bottom = bottom,
            left = left,
            topProfile = topProfile,
            rightProfile = rightProfile,
            bottomProfile = bottomProfile,
            leftProfile = leftProfile
        )
    }
    addPath(path, Offset(rect.left, rect.top))
}

private fun androidx.compose.ui.graphics.Path.addHorizontalTab(
    edgeStart: Float,
    edgeEnd: Float,
    edge: Float,
    direction: PuzzleTabDirection,
    outward: Boolean,
    pieceExtent: Float,
    profile: PuzzleEdgeProfile?
) {
    if (direction == PuzzleTabDirection.FLAT) {
        lineTo(edgeEnd, edge)
        return
    }
    val safeProfile = profile ?: PuzzleEdgeProfile(0f, 1f, 1f, 1f)
    val delta = edgeEnd - edgeStart
    val center = edgeStart + delta * (0.5f + safeProfile.centerBias)
    val baseSpan = kotlin.math.abs(delta) * 0.23f * safeProfile.spanScale
    val shoulder = baseSpan * safeProfile.shoulderScale
    val neck = shoulder * (0.52f * safeProfile.neckScale)
    val knobRadius = pieceExtent * 0.2f * safeProfile.depthScale
    val leftShoulder = center - shoulder * kotlin.math.sign(delta)
    val rightShoulder = center + shoulder * kotlin.math.sign(delta)
    val leftNeck = center - neck * kotlin.math.sign(delta)
    val rightNeck = center + neck * kotlin.math.sign(delta)
    val sign = when {
        outward && direction == PuzzleTabDirection.OUT -> -1f
        outward && direction == PuzzleTabDirection.IN -> 1f
        !outward && direction == PuzzleTabDirection.OUT -> 1f
        else -> -1f
    }

    lineTo(leftShoulder, edge)
    cubicTo(
        leftShoulder + (leftNeck - leftShoulder) * 0.38f, edge,
        leftNeck - shoulder * 0.08f * kotlin.math.sign(delta), edge + sign * knobRadius * 0.42f,
        leftNeck, edge + sign * knobRadius * 0.72f
    )
    cubicTo(
        center - knobRadius * 0.26f * kotlin.math.sign(delta), edge + sign * knobRadius * 1.18f,
        center + knobRadius * 0.26f * kotlin.math.sign(delta), edge + sign * knobRadius * 1.18f,
        rightNeck, edge + sign * knobRadius * 0.72f
    )
    cubicTo(
        rightNeck + shoulder * 0.08f * kotlin.math.sign(delta), edge + sign * knobRadius * 0.42f,
        rightShoulder - (rightShoulder - rightNeck) * 0.38f, edge,
        rightShoulder, edge
    )
    lineTo(edgeEnd, edge)
}

private fun androidx.compose.ui.graphics.Path.addVerticalTab(
    edgeStart: Float,
    edgeEnd: Float,
    edge: Float,
    direction: PuzzleTabDirection,
    outward: Boolean,
    pieceExtent: Float,
    profile: PuzzleEdgeProfile?
) {
    if (direction == PuzzleTabDirection.FLAT) {
        lineTo(edge, edgeEnd)
        return
    }
    val safeProfile = profile ?: PuzzleEdgeProfile(0f, 1f, 1f, 1f)
    val delta = edgeEnd - edgeStart
    val center = edgeStart + delta * (0.5f + safeProfile.centerBias)
    val baseSpan = kotlin.math.abs(delta) * 0.23f * safeProfile.spanScale
    val shoulder = baseSpan * safeProfile.shoulderScale
    val neck = shoulder * (0.52f * safeProfile.neckScale)
    val knobRadius = pieceExtent * 0.2f * safeProfile.depthScale
    val topShoulder = center - shoulder * kotlin.math.sign(delta)
    val bottomShoulder = center + shoulder * kotlin.math.sign(delta)
    val topNeck = center - neck * kotlin.math.sign(delta)
    val bottomNeck = center + neck * kotlin.math.sign(delta)
    val sign = when {
        outward && direction == PuzzleTabDirection.OUT -> 1f
        outward && direction == PuzzleTabDirection.IN -> -1f
        !outward && direction == PuzzleTabDirection.OUT -> -1f
        else -> 1f
    }

    lineTo(edge, topShoulder)
    cubicTo(
        edge, topShoulder + (topNeck - topShoulder) * 0.38f,
        edge + sign * knobRadius * 0.42f, topNeck - shoulder * 0.08f * kotlin.math.sign(delta),
        edge + sign * knobRadius * 0.72f, topNeck
    )
    cubicTo(
        edge + sign * knobRadius * 1.18f, center - knobRadius * 0.26f * kotlin.math.sign(delta),
        edge + sign * knobRadius * 1.18f, center + knobRadius * 0.26f * kotlin.math.sign(delta),
        edge + sign * knobRadius * 0.72f, bottomNeck
    )
    cubicTo(
        edge + sign * knobRadius * 0.42f, bottomNeck + shoulder * 0.08f * kotlin.math.sign(delta),
        edge, bottomShoulder - (bottomShoulder - bottomNeck) * 0.38f,
        edge, bottomShoulder
    )
    lineTo(edge, edgeEnd)
}
