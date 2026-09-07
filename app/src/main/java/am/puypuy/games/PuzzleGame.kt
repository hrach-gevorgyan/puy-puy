package am.puypuy.games

import am.puypuy.R
import am.puypuy.core.FrameLoop
import am.puypuy.core.Fruit
import am.puypuy.core.GameScope
import am.puypuy.core.Island
import am.puypuy.core.Juice
import am.puypuy.core.Layout
import am.puypuy.core.MiniGame
import am.puypuy.core.Moment
import am.puypuy.core.Particles
import am.puypuy.core.Puypuy
import am.puypuy.core.PuypuyState
import am.puypuy.core.Shake
import am.puypuy.core.rememberMoment
import am.puypuy.ui.Scenery
import am.puypuy.ui.drawCoconut
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Փազլ — a four-piece picture of something he knows.
 *
 * docs/games.md §5. Part-to-whole reasoning, which is squarely four-year-old work — and the
 * reason this replaced an alphabet game. Armenian has 39 letters and letter recognition is a
 * five-to-six-year-old skill; it would have been another test wearing a game's clothes.
 *
 * **Tap a piece and it flies home.** That tap path is what makes the puzzle completable
 * without a single drag. Dragging works and snaps from a generous distance, and a piece put
 * down anywhere else simply stays there and can be picked up again. Nothing is ever refused.
 */
class PuzzleGame : MiniGame {

    override val tileImage = R.drawable.tile_puzzle
    override val tileLabel = R.string.game_puzzle
    override val voiceKey = "game_puzzle"

    private class Piece(val row: Int, val col: Int, val home: Offset, start: Offset) {
        val pos = Animatable(start, Offset.VectorConverter)
        var placed by mutableStateOf(false)
        var held by mutableStateOf(false)
    }

    private val pieces = mutableStateListOf<Piece>()
    private var picture by mutableIntStateOf(0)
    private var round by mutableIntStateOf(0)
    private var celebrating by mutableStateOf(false)

    override fun onEnter() {
        pieces.clear()
        picture = (0..2).random()
        round = 0
        celebrating = false
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
        var clock by remember { mutableFloatStateOf(0f) }

        // Sizes and places are arithmetic, so they live in Layout where LayoutTest asserts
        // them. Inline, the tray always went under the board, which starved it on a wide short
        // screen: a phone held sideways got 34dp pieces against a 126dp touch floor.
        val geom = with(density) { Layout.puzzle(area.width.toDp(), area.height.toDp()) }
        val frame = with(density) {
            Rect(
                offset = Offset(geom.boardX.toPx(), geom.boardY.toPx()),
                size = Size(geom.board.toPx(), geom.board.toPx()),
            )
        }
        val cell = frame.width / COLS
        val trayCell = with(density) { geom.trayCell.toPx() }

        FrameLoop(particles = fx, shake = quake) { dt -> clock += dt }

        LaunchedEffect(area, round) {
            if (area.width == 0) return@LaunchedEffect
            pieces.clear()
            celebrating = false
            var i = 0
            for (r in 0 until ROWS) for (c in 0 until COLS) {
                val home = Offset(frame.left + (c + 0.5f) * cell, frame.top + (r + 0.5f) * cell)
                val spot = geom.tray[i]
                val start = with(density) { Offset(spot.first.toPx(), spot.second.toPx()) }
                pieces += Piece(r, c, home, start)
                i++
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
                            ?.takeIf { hypot(start.x - it.pos.value.x, start.y - it.pos.value.y) <= cell * 0.7f }

                        if (piece == null) {
                            if (frame.contains(start)) {
                                // An empty slot: the ghost under it brightens for a moment.
                                moment.ack(start, listOf(Color.White, Island.SandDark), count = 6)
                            } else {
                                moment.ack(
                                    start, listOf(Island.SandDark, Color(0xFFF3E6CC)),
                                    count = 7, speed = 120f..300f, spread = 2.0f, direction = -1.57f,
                                )
                            }
                            return@awaitEachGesture
                        }

                        if (piece.placed) {
                            // A placed piece can always be pulled out again.
                            piece.placed = false
                            moment.ack(piece.home, listOf(Color.White), count = 6)
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
                        val near = hypot(p.x - piece.home.x, p.y - piece.home.y) <= cell * 0.75f
                        if (!dragged) {
                            // A tap wakes the piece and nothing more: a tap that posts it
                            // solves the whole board without a piece ever being carried.
                            moment.ack(p, listOf(Color.White, Island.SandDark), count = 6)
                        } else if (near) {
                            // Dropped near its slot: it lands. The catch is deliberately wide,
                            // because precise dragging is the one thing three-year-olds are
                            // measurably bad at (docs/toddler-ux.md). Dropped anywhere else it
                            // simply stays where it was put — nothing is ever refused.
                            land(piece, moment, coroutines)
                        }
                    }
                }
        ) {
            Scenery(seed = 8, modifier = Modifier.fillMaxSize())

            Canvas(Modifier.fillMaxSize()) {
                @Suppress("UNUSED_EXPRESSION") fx.tick
                translate(quake.offsetX, quake.offsetY) {
                    // The frame the picture is assembled inside.
                    drawRect(Color(0xFFE0CBA4).copy(alpha = 0.55f), frame.topLeft, frame.size)
                    drawRect(
                        Color(0xFFC4A87C), frame.topLeft, frame.size,
                        style = Stroke(width = cell * 0.05f),
                    )
                    // No ghost of the finished picture. Any version of it faint enough to be
                    // a hint was still legible enough to be the answer, and a board with the
                    // answer printed on it is not a puzzle. The frame and its grid say where
                    // the pieces go; which piece goes where is hers to work out.
                    for (c in 1 until COLS) {
                        drawLine(
                            Color(0xFFC4A87C).copy(alpha = 0.5f),
                            Offset(frame.left + c * cell, frame.top),
                            Offset(frame.left + c * cell, frame.bottom),
                            cell * 0.02f,
                        )
                    }
                    for (r in 1 until ROWS) {
                        drawLine(
                            Color(0xFFC4A87C).copy(alpha = 0.5f),
                            Offset(frame.left, frame.top + r * cell),
                            Offset(frame.right, frame.top + r * cell),
                            cell * 0.02f,
                        )
                    }

                    // Placed pieces: the picture showing through its own window.
                    for (p in pieces) {
                        if (!p.placed) continue
                        val l = frame.left + p.col * cell
                        val t = frame.top + p.row * cell
                        clipRect(l, t, l + cell, t + cell) {
                            drawPicture(picture, frame, 1f, clock)
                        }
                        drawRect(
                            Color(0xFF3A4454).copy(alpha = 0.35f),
                            Offset(l, t), Size(cell, cell),
                            style = Stroke(width = cell * 0.03f),
                        )
                    }

                    // Loose pieces: the same window, carried around.
                    for (p in pieces) {
                        if (p.placed) continue
                        val s = cell * if (p.held) 1.08f else 1f
                        val at = p.pos.value
                        val dx = at.x - (frame.left + (p.col + 0.5f) * cell)
                        val dy = at.y - (frame.top + (p.row + 0.5f) * cell)
                        rotate(if (p.held) 0f else sin((clock + p.col) * 1.4f) * 3f, at) {
                            drawRect(Color(0xFFF6EEDF), Offset(at.x - s / 2f, at.y - s / 2f), Size(s, s))
                            clipRect(at.x - s / 2f, at.y - s / 2f, at.x + s / 2f, at.y + s / 2f) {
                                translate(dx, dy) { drawPicture(picture, frame, 1f, clock) }
                            }
                            drawRect(
                                Color(0xFF3A4454),
                                Offset(at.x - s / 2f, at.y - s / 2f), Size(s, s),
                                style = Stroke(width = s * 0.045f),
                            )
                        }
                    }
                }
                fx.draw(this)
            }

            Puypuy(
                controller = scope.puypuy,
                size = (maxHeight * 0.20f).coerceIn(140.dp, 220.dp),
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }

    private fun land(piece: Piece, moment: Moment, coroutines: CoroutineScope) {
        if (piece.placed) return
        coroutines.launch {
            piece.pos.animateTo(piece.home, tween(280, easing = Juice.Enter))
            piece.placed = true
            moment.happened(
                at = piece.home,
                colours = listOf(Island.SandDark, Color.White, Color(0xFFF3E6CC)),
                trauma = Shake.Thud,
                count = 12,
            )
            if (pieces.count { !it.placed } == 1) moment.almost()
            if (pieces.all { it.placed }) {
                // The finished picture comes alive for a few seconds, then a new one.
                celebrating = true
                moment.happened(
                    at = piece.home,
                    colours = listOf(Fruit.Apple.color, Fruit.Banana.color, Fruit.Grapes.color),
                    word = "praise_${(1..8).random()}",
                    reaction = PuypuyState.Happy,
                    count = 26,
                )
                delay(3000)
                picture = (picture + 1) % PICTURES
                round++
            }
        }
    }

    private companion object {
        const val ROWS = 3
        const val COLS = 3
        const val PICTURES = 3
    }
}

/** The three pictures. Existing art, re-used — a puzzle of things she already knows. */
private fun DrawScope.drawPicture(which: Int, frame: Rect, alpha: Float, clock: Float) {
    val c = frame.center
    val s = frame.width
    when (which) {
        0 -> {
            drawCircle(Color(0xFFBBDDE0).copy(alpha = alpha), s * 0.7f, c)
            drawCoconut(s * 0.62f, Offset(c.x - s * 0.31f, c.y - s * 0.31f))
        }
        1 -> {
            drawCircle(Color(0xFFDDEFE8).copy(alpha = alpha), s * 0.7f, c)
            drawFruit(Fruit.Apple, s * 0.44f, Offset(c.x - s * 0.42f, c.y - s * 0.30f))
            drawFruit(Fruit.Banana, s * 0.44f, Offset(c.x + s * 0.02f, c.y - s * 0.06f))
        }
        else -> {
            drawCircle(Color(0xFFF1E2C7).copy(alpha = alpha), s * 0.7f, c)
            drawFruit(Fruit.Grapes, s * 0.42f, Offset(c.x - s * 0.36f, c.y - s * 0.34f))
            drawFruit(Fruit.Strawberry, s * 0.42f, Offset(c.x + s * 0.0f, c.y + s * 0.0f))
        }
    }
}
