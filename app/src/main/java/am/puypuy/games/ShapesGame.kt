package am.puypuy.games

import am.puypuy.R
import am.puypuy.core.FrameLoop
import am.puypuy.core.GameScope
import am.puypuy.core.Island
import am.puypuy.core.Juice
import am.puypuy.core.Layout
import am.puypuy.core.MiniGame
import am.puypuy.core.Moment
import am.puypuy.core.Paint
import am.puypuy.core.Particles
import am.puypuy.core.Puypuy
import am.puypuy.core.PuypuyState
import am.puypuy.core.Shake
import am.puypuy.core.rememberMoment
import am.puypuy.ui.Scenery
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** The five shapes, and the Armenian word for each. */
enum class Shape(val voiceKey: String, val colour: Color) {
    CIRCLE("shape_circle", Paint.BLUE.color),
    SQUARE("shape_square", Paint.GREEN.color),
    TRIANGLE("shape_triangle", Paint.YELLOW.color),
    STAR("shape_star", Paint.ORANGE.color),
    HEART("shape_heart", Paint.RED.color),

    // Four more, so the board is a choice rather than a formality. Each is distinct in
    // SILHOUETTE and not only in name — a rectangle against a square, an oval against a
    // circle: telling those two pairs apart is the whole of the lesson at this age.
    RECTANGLE("shape_rectangle", Paint.PURPLE.color),
    OVAL("shape_oval", Paint.BLUE.color),
    DIAMOND("shape_diamond", Paint.GREEN.color),
    CRESCENT("shape_crescent", Paint.YELLOW.color);

    fun path(centre: Offset, s: Float): Path = when (this) {
        CIRCLE -> Path().apply { addOval(androidx.compose.ui.geometry.Rect(centre, s * 0.5f)) }
        SQUARE -> Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    centre.x - s * 0.44f, centre.y - s * 0.44f, centre.x + s * 0.44f, centre.y + s * 0.44f,
                    androidx.compose.ui.geometry.CornerRadius(s * 0.10f, s * 0.10f),
                )
            )
        }
        TRIANGLE -> Path().apply {
            moveTo(centre.x, centre.y - s * 0.50f)
            lineTo(centre.x + s * 0.48f, centre.y + s * 0.36f)
            lineTo(centre.x - s * 0.48f, centre.y + s * 0.36f)
            close()
        }
        STAR -> Path().apply {
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) s * 0.52f else s * 0.22f
                val a = (-90f + i * 36f) * Math.PI.toFloat() / 180f
                val p = Offset(centre.x + cos(a) * r, centre.y + sin(a) * r)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        HEART -> Path().apply {
            moveTo(centre.x, centre.y + s * 0.44f)
            cubicTo(
                centre.x - s * 0.72f, centre.y - s * 0.02f,
                centre.x - s * 0.40f, centre.y - s * 0.56f,
                centre.x, centre.y - s * 0.20f,
            )
            cubicTo(
                centre.x + s * 0.40f, centre.y - s * 0.56f,
                centre.x + s * 0.72f, centre.y - s * 0.02f,
                centre.x, centre.y + s * 0.44f,
            )
            close()
        }

        RECTANGLE -> Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    centre.x - s * 0.52f, centre.y - s * 0.30f,
                    centre.x + s * 0.52f, centre.y + s * 0.30f,
                    androidx.compose.ui.geometry.CornerRadius(s * 0.08f, s * 0.08f),
                )
            )
        }
        OVAL -> Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    centre.x - s * 0.52f, centre.y - s * 0.34f,
                    centre.x + s * 0.52f, centre.y + s * 0.34f,
                )
            )
        }
        DIAMOND -> Path().apply {
            moveTo(centre.x, centre.y - s * 0.52f)
            lineTo(centre.x + s * 0.40f, centre.y)
            lineTo(centre.x, centre.y + s * 0.52f)
            lineTo(centre.x - s * 0.40f, centre.y)
            close()
        }
        CRESCENT -> Path().apply {
            // A moon: one full circle with a second bitten out of its side.
            val outer = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(centre, s * 0.50f))
            }
            val bite = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        Offset(centre.x + s * 0.26f, centre.y - s * 0.04f), s * 0.44f,
                    )
                )
            }
            addPath(Path().apply { op(outer, bite, PathOperation.Difference) })
        }
    }
}

/**
 * Ձևե՛ր — a shape sorter, which is a physical toy a three-year-old already understands.
 *
 * docs/games.md §4. **Tap a shape and it flies to its own hole** — that tap path is what makes
 * the game completable without a single drag, which matters because precise dragging is the
 * one gesture the research says not to require at this age. Dragging works too, and snaps from
 * a generous distance.
 *
 * The wrong hole does not punish. It simply does not accept: the shape bumps, tips and hops
 * back onto the sand. That is a fact about the world, not a judgement about her, and it is the
 * only "no" anywhere in the app.
 */
class ShapesGame : MiniGame {

    override val tileImage = R.drawable.tile_shapes
    override val tileLabel = R.string.game_shapes
    override val voiceKey = "game_shapes"

    private class Piece(val shape: Shape, val home: Offset, val hole: Offset) {
        val pos = Animatable(home, Offset.VectorConverter)
        var placed by mutableStateOf(false)
        var held by mutableStateOf(false)
        var wobble by mutableFloatStateOf(0f)
    }

    private val pieces = mutableStateListOf<Piece>()
    private var round by mutableIntStateOf(0)

    override fun onEnter() {
        pieces.clear()
        round = 0
    }

    override fun onExit() = pieces.clear()

    @Composable
    override fun Content(scope: GameScope) {
        val density = LocalDensity.current
        val coroutines = rememberCoroutineScope()
        val moment = rememberMoment(scope)
        val fx = remember { Particles() }
        val quake = remember { Shake() }
        moment.particles = fx
        moment.shake = quake

        var area by remember { mutableStateOf(IntSize.Zero) }

        // Sizes and places are arithmetic, so they live in Layout where LayoutTest asserts
        // them. Inline, this game laid the shapes ABOVE their holes on a phone held sideways
        // and capped their size at a fixed width, so a tablet got phone-sized shapes.
        val geom = with(density) { Layout.shapes(area.width.toDp(), area.height.toDp()) }
        val size = with(density) { geom.size.toPx() }
        val snap = size * 0.85f

        FrameLoop(particles = fx, shake = quake) { dt ->
            for (p in pieces) if (p.wobble > 0f) p.wobble = (p.wobble - dt * 4f).coerceAtLeast(0f)
        }

        LaunchedEffect(area, round) {
            if (area.width == 0) return@LaunchedEffect
            pieces.clear()
            val chosen = Shape.entries.shuffled().take(geom.count)
            // The holes are in a grid because she has to be able to FIND the one she wants.
            // The shapes are scattered, because a tidy row under a tidy row is "drag straight
            // up" and nothing is ever matched to anything.
            val holeOrder = chosen.indices.shuffled()
            val taken = mutableListOf<Offset>()
            chosen.forEachIndexed { i, shape ->
                pieces += Piece(
                    shape = shape,
                    home = scatter(geom, taken, size, density),
                    hole = with(density) {
                        val (hx, hy) = geom.holes[holeOrder[i]]
                        Offset(hx.toPx(), hy.toPx())
                    },
                )
            }
            delay(900)
            scope.puypuy.react(PuypuyState.Waving)
            // Nothing plays itself. He waves hello, and then the screen is hers: a game that
            // takes the first turn has taken it FROM her.
        }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .onSizeChanged { area = it }
                .pointerInput(area, round) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val start = down.position

                        val piece = pieces
                            .minByOrNull { hypot(start.x - it.pos.value.x, start.y - it.pos.value.y) }
                            // A hit floor, so more shapes on screen means smaller DRAWINGS
                            // and not smaller targets: every one clears the 126dp rule however
                            // little of the screen it takes up.
                            ?.takeIf {
                                hypot(start.x - it.pos.value.x, start.y - it.pos.value.y) <=
                                    maxOf(size * 0.75f, with(density) { 63.dp.toPx() })
                            }

                        if (piece == null) {
                            moment.ack(
                                start, listOf(Island.SandDark, Color(0xFFF3E6CC)),
                                count = 7, speed = 120f..300f, spread = 2.0f, direction = -1.57f,
                            )
                            return@awaitEachGesture
                        }

                        if (piece.placed) {
                            // A posted shape pops back out to be used again.
                            piece.placed = false
                            coroutines.launch { piece.pos.animateTo(piece.home, Juice.bouncy()) }
                            moment.ack(piece.hole, listOf(piece.shape.colour, Color.White), count = 8)
                            return@awaitEachGesture
                        }

                        piece.held = true
                        var dragged = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            dragged = true
                            change.consume()
                            coroutines.launch { piece.pos.snapTo(change.position) }
                        }
                        piece.held = false

                        val p = piece.pos.value
                        val nearOwn = hypot(p.x - piece.hole.x, p.y - piece.hole.y) <= snap
                        val nearOther = pieces.any {
                            it !== piece && hypot(p.x - it.hole.x, p.y - it.hole.y) <= snap
                        }

                        when {
                            // A tap NAMES the shape and hops it. It used to post it straight
                            // into the hole, which meant three taps finished a round without
                            // the child ever having matched anything to anything.
                            !dragged -> {
                                piece.wobble = 1f
                                moment.happened(
                                    at = p,
                                    colours = listOf(piece.shape.colour, Color.White),
                                    word = piece.shape.voiceKey,
                                    count = 8,
                                    // She picked it up and heard its name. Nothing has been
                                    // solved yet, so nothing is congratulated.
                                    success = false,
                                )
                            }

                            // Carried to its own hole: it goes in.
                            nearOwn -> place(piece, moment, coroutines)

                            // The wrong hole will not take it. It bumps and hops back out —
                            // no sound of failure, and he does not react at all.
                            nearOther -> {
                                // It will not go in there. He does not say "wrong" — now and
                                // then he says «Կրկի՛ն փորձիր», and mostly he says nothing.
                                moment.encourage()
                                piece.wobble = 1f
                                coroutines.launch {
                                    piece.pos.animateTo(piece.home, Juice.bouncy())
                                }
                                moment.ack(p, listOf(Island.SandDark), count = 5, speed = 80f..200f)
                            }

                            // Put down anywhere else: it stays where it was put.
                            else -> Unit
                        }
                    }
                }
        ) {
            Scenery(seed = 6, modifier = Modifier.fillMaxSize())

            // Halfway up the right edge and BEHIND the shapes, like the tower game. Standing
            // in the bottom corner he sat on the row of loose shapes and covered whichever one
            // was nearest him — and the shapes need the whole width now that there are more of
            // them.
            Puypuy(
                controller = scope.puypuy,
                size = (maxHeight * 0.15f).coerceIn(100.dp, 170.dp),
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            Canvas(Modifier.fillMaxSize()) {
                @Suppress("UNUSED_EXPRESSION") fx.tick
                translate(quake.offsetX, quake.offsetY) {
                    // The holes: shape-shaped dents in the sand.
                    for (p in pieces) {
                        drawPath(p.shape.path(p.hole, size * 1.06f), Color(0xFFE0CBA4))
                        drawPath(
                            p.shape.path(p.hole, size * 1.06f),
                            Color(0xFFC4A87C),
                            style = Stroke(width = size * 0.045f),
                        )
                    }
                    // Then the shapes themselves, on top.
                    for (p in pieces) {
                        if (p.placed) continue
                        val s = size * if (p.held) 1.12f else 1f
                        rotate(sin(p.wobble * 18f) * 14f, p.pos.value) {
                            drawPath(p.shape.path(p.pos.value, s), p.shape.colour)
                            drawPath(
                                p.shape.path(p.pos.value, s),
                                Color(0xFF3A4454),
                                style = Stroke(width = s * 0.055f),
                            )
                        }
                    }
                    // Posted shapes sit in their holes, a shade darker.
                    for (p in pieces) {
                        if (!p.placed) continue
                        drawPath(p.shape.path(p.hole, size), p.shape.colour.copy(alpha = 0.9f))
                        drawPath(
                            p.shape.path(p.hole, size),
                            Color(0xFF3A4454),
                            style = Stroke(width = size * 0.055f),
                        )
                    }
                }
                fx.draw(this)
            }

        }
    }

    /**
     * A free spot on the sand, far enough from the ones already used.
     *
     * Twenty tries and then it takes what it can get: a scatter that insists on perfection can
     * fail to place anything at all on a small screen, and a shape half over another is a much
     * smaller problem than a shape that never appeared.
     */
    private fun scatter(
        geom: Layout.Shapes,
        taken: MutableList<Offset>,
        size: Float,
        density: androidx.compose.ui.unit.Density,
    ): Offset {
        val left = with(density) { geom.scatterLeft.toPx() }
        val right = with(density) { geom.scatterRight.toPx() }
        val top = with(density) { geom.scatterTop.toPx() }
        val bottom = with(density) { geom.scatterBottom.toPx() }
        var best = Offset(left, top)
        var bestGap = -1f
        repeat(20) {
            val p = Offset(
                left + kotlin.random.Random.nextFloat() * (right - left).coerceAtLeast(1f),
                top + kotlin.random.Random.nextFloat() * (bottom - top).coerceAtLeast(1f),
            )
            val nearest = taken.minOfOrNull { hypot(p.x - it.x, p.y - it.y) } ?: Float.MAX_VALUE
            if (nearest > size * 1.05f) {
                taken += p
                return p
            }
            if (nearest > bestGap) {
                bestGap = nearest
                best = p
            }
        }
        taken += best
        return best
    }

    private fun place(piece: Piece, moment: Moment, coroutines: CoroutineScope) {
        coroutines.launch {
            piece.pos.animateTo(piece.hole, tween(300, easing = Juice.Enter))
            piece.placed = true
            moment.happened(
                at = piece.hole,
                colours = listOf(piece.shape.colour, Island.SandDark, Color.White),
                word = piece.shape.voiceKey,
                trauma = Shake.Thud,
                count = 14,
            )
            if (pieces.count { !it.placed } == 1) moment.almost()
            // A full board refills itself, so the toy never runs out.
            if (pieces.all { it.placed }) {
                delay(1400)
                round++
            }
        }
    }
}

