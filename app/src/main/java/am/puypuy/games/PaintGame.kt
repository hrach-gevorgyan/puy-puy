package am.puypuy.games

import am.puypuy.R
import am.puypuy.core.Chatter
import am.puypuy.core.GameScope
import am.puypuy.core.GreetOnEnter
import am.puypuy.core.Layout
import am.puypuy.core.MinTouch
import am.puypuy.core.FrameLoop
import am.puypuy.core.MiniGame
import am.puypuy.core.rememberMoment
import am.puypuy.core.Particles
import am.puypuy.core.Paint
import am.puypuy.core.Puypuy
import am.puypuy.core.PuypuyState
import am.puypuy.ui.HomeButtonSafe
import am.puypuy.ui.HomeButtonVisual
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * §6. Blank canvas, four fat colours, a finger, and one clear button.
 *
 * Multi-touch draws multiple strokes, because she will use several fingers.
 * There is no undo, no save, no brush size, and no confirmation on clear.
 */
class PaintGame : MiniGame {

    override val tileImage = R.drawable.tile_paint
    override val tileLabel = R.string.game_paint
    override val voiceKey = "game_paint"

    /**
     * One finger's line. [version] is snapshot state bumped on every extension: the Path
     * itself is a plain object, so without an observed read the Canvas would never redraw as
     * the line grows.
     */
    private class Stroke2(val color: Color) {
        /**
         * The line, in FRACTIONS of the canvas rather than in pixels.
         *
         * Stored in pixels, a drawing was scrambled the moment the tablet was turned: the
         * numbers stayed put while the canvas underneath them changed shape. Kept as
         * fractions, the same drawing simply re-scales into whatever the new canvas is.
         */
        private val points = mutableListOf<Offset>()

        var version by mutableIntStateOf(0)
        val count: Int get() = points.size
        var last: Offset = Offset.Unspecified
            private set

        fun extendTo(p: Offset, size: Size) {
            if (size.width <= 0f || size.height <= 0f) return
            points += Offset(p.x / size.width, p.y / size.height)
            last = p
            version++
        }

        fun finish() {
            version++
        }

        /** The line as it should appear on a canvas of [size], smoothed through the midpoints. */
        fun pathFor(size: Size): Path = Path().apply {
            if (points.isEmpty()) return@apply
            fun at(i: Int) = Offset(points[i].x * size.width, points[i].y * size.height)
            moveTo(at(0).x, at(0).y)
            for (i in 1 until points.size) {
                // Quadratic through the midpoint: smooth, and cheap at 30fps.
                val a = at(i - 1)
                val b = at(i)
                val mid = (a + b) / 2f
                quadraticTo(a.x, a.y, mid.x, mid.y)
            }
            val end = at(points.size - 1)
            lineTo(end.x, end.y)
        }
    }

    private val strokes = mutableStateListOf<Stroke2>()

    override fun onEnter() {
        // A blank page every time — she will expect one (§6).
        strokes.clear()
    }

    override fun onExit() {
        strokes.clear()
    }

    // `historical` is still experimental. It is the only way to see the samples the system
    // batched into one frame, and a toddler scribbling fast generates several per frame.
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content(scope: GameScope) {
        val moment = rememberMoment(scope)
        val fx = remember { Particles() }
        moment.particles = fx
        // Paint has no other need for a frame loop; this one exists so the praise sparkle
        // is actually advanced and drawn rather than thrown into a void.
        FrameLoop(particles = fx, shake = null)
        GreetOnEnter(scope, Chatter.PAINT_START)

        val coroutines = rememberCoroutineScope()
        var selected by remember { mutableStateOf(Paint.RED) }
        val wipe = remember { Animatable(1f) }
        var finished by remember { mutableStateOf(0) }

        BoxWithConstraints(Modifier.fillMaxSize().background(Color.White)) {
            // The pill must FIT INSIDE THE SCREEN, and every previous attempt got this wrong
            // by budgeting for the swatches and then forgetting the gaps, her, and the pill's
            // own padding. Count every one of them, and leave a real margin at both ends.
            val sideMargin = 12.dp
            val pillPadding = 10.dp
            val gap = 6.dp
            val herShare = 0.95f                  // her width, as a fraction of a swatch
            val slots = CANVAS_COLOURS.size + herShare
            val fixed = sideMargin * 2 + pillPadding * 2 + gap * (CANVAS_COLOURS.size + 1)
            val swatch = ((maxWidth - fixed) / slots).coerceIn(46.dp, MinTouch)

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        val live = mutableMapOf<Long, Stroke2>()
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                // Read every time: this is what the fractions are measured
                                // against, and it changes the moment the device is turned.
                                val canvas = Size(size.width.toFloat(), size.height.toFloat())
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        // `changes` lists every pointer down, not just the
                                        // one that changed: without pressed/previousPressed
                                        // a second finger restarted finger one's stroke.
                                        event.changes.filter { it.pressed && !it.previousPressed }.forEach { c ->
                                            val s = Stroke2(selected.color)
                                            s.extendTo(c.position, canvas)
                                            live[c.id.value] = s
                                            strokes += s
                                            while (strokes.size > MAX_STROKES) strokes.removeAt(0)
                                            c.consume()
                                        }
                                    }
                                    PointerEventType.Move -> {
                                        event.changes.filter { it.pressed }.forEach { c ->
                                            val s = live[c.id.value] ?: return@forEach
                                            // Replay the historical samples too. A fast
                                            // scribble delivers several positions per frame
                                            // and dropping them turned curves into polygons.
                                            for (i in 0 until c.historical.size) {
                                                s.extendTo(c.historical[i].position, canvas)
                                            }
                                            s.extendTo(c.position, canvas)
                                            c.consume()
                                        }
                                    }
                                    PointerEventType.Release -> {
                                        event.changes.filter { !it.pressed && it.previousPressed }.forEach { c ->
                                            val s = live.remove(c.id.value)
                                            if (s != null) {
                                                s.finish()
                                                finished++
                                            }
                                            c.consume()
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    @Suppress("UNUSED_EXPRESSION") fx.tick
                    val width = BRUSH.toPx()
                    strokes.forEach { s ->
                        // A real read of snapshot state, inside the draw scope: this is what
                        // schedules the redraw as the line grows.
                        if (s.version < 0) return@forEach
                        drawPath(
                            // Rebuilt for the canvas as it is now, so turning the device
                            // re-scales the drawing instead of scrambling it.
                            path = s.pathFor(size),
                            color = s.color,
                            alpha = wipe.value,
                            style = Stroke(
                                width = width,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                    fx.draw(this)
                }
            }

            // ~1 in 5 finished strokes pleases him, through the same one routine as every
            // other positive event in the app.
            LaunchedEffect(finished) {
                if (finished > 0 && (0..4).random() == 0) {
                    val at = strokes.lastOrNull()?.last ?: return@LaunchedEffect
                    moment.happened(
                        at = at,
                        colours = listOf(Color.White, strokes.last().color),
                        word = if ((0..1).random() == 0) "paint_nice" else null,
                        effect = "brush",
                        count = 10,
                    )
                }
            }

            // Controls float ON the page rather than taking a bar out of it.
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = sideMargin, end = sideMargin, bottom = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BarColour.copy(alpha = 0.95f))
                    .padding(horizontal = pillPadding, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // He holds the palette: the controls are his, which is why they are allowed
                // to sit on top of the page at all.
                Puypuy(controller = scope.puypuy, size = swatch * herShare)

                CANVAS_COLOURS.forEach { paint ->
                    ColourButton(
                        paint = paint,
                        selected = paint == selected,
                        size = swatch,
                        onClick = {
                            selected = paint
                            scope.audio.say(paint.voiceKey)
                        },
                    )
                }
            }


            // Clear sits in the opposite corner from home, so the two are never confused and
            // neither is ever under a finger that meant to draw.
            ClearButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                onClick = {
                    scope.audio.effect("wipe")
                    scope.chatter.say("paint_clean", reaction = PuypuyState.Surprised)
                    coroutines.launch {
                        // 600ms wipe, then the page is blank. No dialog — she cannot read
                        // one, and redrawing is free.
                        wipe.animateTo(0f, tween(600))
                        strokes.clear()
                        wipe.snapTo(1f)
                    }
                },
            )


        }
    }

    private companion object {
        // All six. Four was the original spec's number and it was simply too few — orange
        // and purple are the two a three-year-old reaches for first after red.
        val CANVAS_COLOURS = Paint.entries.toList()
        val BarColour = Color(0xFFEFE6D2)

        /**
         * 24dp was a marker pen: three strokes filled the page and nothing she drew had any
         * shape. This is a crayon.
         */
        val BRUSH = 14.dp

        /**
         * A hard ceiling on accumulated strokes. She will scribble for an hour, and without
         * this every frame re-strokes every path ever drawn until the canvas stops keeping
         * up. Dropping the oldest is invisible in practice: it is under everything else.
         */
        const val MAX_STROKES = 240
    }
}

@Composable
private fun ColourButton(paint: Paint, selected: Boolean, size: Dp, onClick: () -> Unit) {
    val disc = if (selected) size * 0.86f else size * 0.68f
    Box(
        Modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(disc + 10.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
        Box(
            Modifier
                .size(disc)
                .clip(CircleShape)
                .background(paint.color),
        )
    }
}

/** An arrow curling back on itself. The only "put it back" in the app. */

@Composable
private fun ClearButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(8.dp)
            .size(MinTouch)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(HomeButtonVisual)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f))
                .border(3.dp, Color(0xFF3A4454).copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_eraser),
                contentDescription = null,
                modifier = Modifier.size(HomeButtonVisual * 0.62f),
            )
        }
    }
}
