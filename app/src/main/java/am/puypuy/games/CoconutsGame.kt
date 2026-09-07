package am.puypuy.games

import am.puypuy.R
import am.puypuy.core.FrameLoop
import am.puypuy.core.GameScope
import am.puypuy.core.Island
import am.puypuy.core.Juice
import am.puypuy.core.Layout
import am.puypuy.core.MiniGame
import am.puypuy.core.Moment
import am.puypuy.core.Mouse
import am.puypuy.core.Particles
import am.puypuy.core.Puypuy
import am.puypuy.core.PuypuyState
import am.puypuy.core.Shake
import am.puypuy.core.rememberMoment
import am.puypuy.ui.Scenery
import am.puypuy.ui.drawCoconut
import am.puypuy.ui.drawCurled
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Պահմտոցի — peekaboo.
 *
 * docs/games.md §3. Touch-down cracks a coconut open in under 250ms and an animal springs out
 * and **stays**. Nothing is asked and nothing waits: a screen held for a spoken answer does
 * not read as a pause for thought, it reads as a crash.
 *
 * An open cell is never a spent cell. Poke the animal and it does its move; touch the shell and
 * it clacks shut, ready to open on a different animal.
 */
class CoconutsGame : MiniGame {

    override val tileImage = R.drawable.tile_coconuts
    override val tileLabel = R.string.game_coconuts
    override val voiceKey = "game_coconuts"

    private var cells by mutableStateOf(emptyList<Cell>())
    private var generation by mutableIntStateOf(0)

    override fun onEnter() {
        naming = 0
        cells = newBoard()
    }

    override fun onExit() {
        cells = emptyList()
    }

    private fun newBoard(): List<Cell> {
        val animals = Animal.entries.shuffled().take(8)
        return (animals.map { Cell(it) } + Cell(null)).shuffled()
    }

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

        if (cells.isEmpty()) cells = newBoard()
        val board = cells

        val geom = with(density) { Layout.coconutBoard(area.width.toDp(), area.height.toDp()) }
        val cellPx = with(density) { geom.cell.toPx() }
        val ox = with(density) { geom.originX.toPx() }
        val oy = with(density) { geom.originY.toPx() }
        fun centreOf(i: Int) = Offset(ox + (i % 3 + 0.5f) * cellPx, oy + (i / 3 + 0.5f) * cellPx)

        FrameLoop(particles = fx, shake = quake) { dt -> clock += dt }

        // All nine open: the animals hop out and mill about for a moment before a fresh board.
        // Keyed on `generation` only — keying it on "are they all open", which its own body
        // falsifies, made it cancel itself one frame in and the board never came back.
        LaunchedEffect(generation, board) {
            snapshotFlow { board.isNotEmpty() && board.all { it.isOpen } }.first { it }
            delay(600)
            board.forEach { it.partying = true }
            // Long enough that the ninth animal is named before its coconut shuts. The party
            // used to start closing at 3.8s while that name was due at 5.2s, so the last
            // animal of every round was the one she never got told.
            delay(maxOf(3200L, ASK_AFTER_MS + HER_TURN_MS + 400L - 600L))
            coroutineScope {
                board.forEachIndexed { i, c ->
                    launch {
                        delay(i * 70L)
                        c.partying = false
                        c.isOpen = false
                        c.open.animateTo(0f, tween(180))
                    }
                }
            }
            delay(board.size * 70L + 240L)
            cells = newBoard()
            generation++
        }

        LaunchedEffect(area, generation) {
            if (area.width == 0) return@LaunchedEffect
            delay(800)
            scope.puypuy.react(PuypuyState.Waving)
            // Nothing plays itself. He waves hello, and then the screen is hers: a game that
            // takes the first turn has taken it FROM her.
        }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .onSizeChanged { area = it }
                .pointerInput(area, generation) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val p = down.position
                        if (cellPx <= 0f) return@awaitEachGesture

                        val col = ((p.x - ox) / cellPx).toInt()
                        val row = ((p.y - oy) / cellPx).toInt()
                        if (col !in 0..2 || row !in 0..2) {
                            // Between the coconuts is still a touch.
                            moment.ack(
                                p, listOf(Island.SandDark, Color(0xFFF3E6CC)),
                                count = 7, speed = 120f..300f, spread = 2.0f, direction = -1.57f,
                            )
                            return@awaitEachGesture
                        }
                        val i = row * 3 + col
                        val cell = board.getOrNull(i) ?: return@awaitEachGesture
                        val at = centreOf(i)

                        when {
                            !cell.isOpen -> crack(scope, cell, at, coroutines, moment)

                            // Poke the animal: it does its signature move and its noise. This
                            // is what stops an opened cell from being a spent one.
                            cell.pokes < 2 -> {
                                cell.pokes++
                                cell.fidget(coroutines)
                                moment.happened(
                                    at = at,
                                    colours = listOf(cell.animal?.body ?: Mouse.Body, Island.CoconutFlesh),
                                    word = if (cell.pokes == 1) null else cell.animal?.voiceKey,
                                    effect = cell.animal?.sound ?: "squeak",
                                    count = 8,
                                    // Poking an animal that is already out is play, not an
                                    // achievement. Opening the coconut was the achievement.
                                    success = false,
                                )
                            }

                            // Touch the shell and it clacks shut, to reopen on a different one.
                            else -> {
                                cell.close(coroutines)
                                moment.ack(at, listOf(Island.CoconutShell, Island.CoconutDark), count = 8)
                            }
                        }
                    }
                }
        ) {
            Scenery(seed = 3, modifier = Modifier.fillMaxSize())

            Canvas(Modifier.fillMaxSize()) {
                @Suppress("UNUSED_EXPRESSION") fx.tick
                translate(quake.offsetX, quake.offsetY) {
                    board.forEachIndexed { i, c -> drawCell(c, centreOf(i), cellPx, clock) }
                }
                fx.draw(this)
            }

            Puypuy(
                controller = scope.puypuy,
                size = (maxHeight * 0.22f).coerceIn(150.dp, 240.dp),
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }

    /** A beat after the shell cracks, so the question does not land under the noise of it. */
    private val ASK_AFTER_MS = 700L

    /**
     * How long she gets to answer «Ո՞վ է թաքնվել» before he does. Nothing is waiting on it: the
     * screen is fully alive throughout, and if she opens another coconut in the meantime the
     * question moves to that one.
     */
    private val HER_TURN_MS = 4_500L

    /** Which crack is currently entitled to be named. */
    private var naming = 0

    private fun crack(
        scope: GameScope,
        cell: Cell,
        at: Offset,
        coroutines: CoroutineScope,
        moment: Moment,
    ) {
        cell.isOpen = true
        cell.pokes = 0
        coroutines.launch { cell.open.animateTo(1f, Juice.bouncy()) }

        val jackpot = cell.animal == null
        if (jackpot) scope.puypuy.react(PuypuyState.Surprised)

        // Milk and shell chips at once, then «Ո՞վ է թաքնվել» — who is hiding — and four and a
        // half seconds for her to answer before he does.
        //
        // This game asked exactly this once before and it was cut, because the wait FROZE the
        // screen: nothing responded for four and a half seconds, which does not read as time to
        // think, it reads as a crash. The question is back at the owner's instruction and the
        // freeze is not. The animal springs out on touch-down, every coconut stays touchable
        // throughout, she may open another or poke this one — the only thing the wait governs
        // is when HE says the word.
        moment.happened(
            at = at,
            colours = listOf(Color.White, Island.CoconutFlesh, Island.CoconutShell, Island.CoconutDark),
            reaction = if (jackpot) PuypuyState.Surprised else PuypuyState.Happy,
            trauma = Shake.Crack,
            effect = "chime",
            count = 20,
        )

        // Only the last coconut she opened gets named. Three cracked in quick succession
        // would otherwise all speak at once, three seconds later.
        val turn = ++naming
        coroutines.launch {
            delay(ASK_AFTER_MS)
            if (turn != naming) return@launch
            scope.audio.say("coconut_who_is_there")
            scope.puypuy.react(PuypuyState.Suggesting)

            delay(HER_TURN_MS)
            if (turn != naming || !cell.isOpen) return@launch
            scope.audio.say(if (jackpot) "coconut_here_i_am" else cell.animal!!.voiceKey)
            scope.puypuy.react(PuypuyState.Happy)
        }
    }
}

/** null contents == the jackpot: Պույ-պույ himself, asleep, curled up. */
private class Cell(val animal: Animal?) {
    val open = Animatable(0f)
    var isOpen by mutableStateOf(false)
    var pokes by mutableIntStateOf(0)
    var partying by mutableStateOf(false)

    /** Fixed at board creation so a coconut never jitters between frames. */
    val tilt: Float = (-16..16).random().toFloat()
    val scale: Float = 0.90f + (0..16).random() / 100f
    val idlePhase: Float = (0..628).random() / 100f

    fun fidget(scope: CoroutineScope) {
        scope.launch {
            open.animateTo(1.24f, tween(130))
            open.animateTo(1f, Juice.bouncy())
        }
    }

    fun close(scope: CoroutineScope) {
        isOpen = false
        pokes = 0
        scope.launch { open.animateTo(0f, tween(180)) }
    }
}

private val ShellRim = Island.CoconutDark

/**
 * Two halves: the bottom stays as a bowl, the top lifts clear, and the animal sits IN it.
 *
 * The lid has to clear the contents. At one point it came to rest 0.72r above centre while the
 * animal reached 0.92r, and since the lid draws last it covered the animal completely.
 */
private fun DrawScope.drawCell(cell: Cell, centre: Offset, span: Float, clock: Float) {
    val t = cell.open.value
    val d = span * 0.72f
    val r = d / 2f

    if (t < 0.02f) {
        // A closed coconut rattles every few seconds, so the board is never quite still.
        val rattle = sin((clock + cell.idlePhase) * 1.1f)
        val kick = if (rattle > 0.97f) sin(clock * 40f) * 3.5f else 0f
        drawOval(Color(0x1A3A4454), Offset(centre.x - r * 0.9f, centre.y + r * 0.85f), Size(r * 1.8f, r * 0.34f))
        rotate(cell.tilt + kick, centre) {
            val dd = d * cell.scale
            drawCoconut(dd, Offset(centre.x - dd / 2f, centre.y - dd / 2f))
        }
        return
    }

    // Bottom half: a bowl, white flesh facing up.
    drawPath(
        Path().apply {
            moveTo(centre.x - r, centre.y)
            cubicTo(centre.x - r, centre.y + r * 1.25f, centre.x + r, centre.y + r * 1.25f, centre.x + r, centre.y)
            close()
        },
        Island.CoconutShell,
    )
    drawOval(
        Island.CoconutFlesh,
        Offset(centre.x - r * 0.92f, centre.y - r * 0.16f),
        Size(r * 1.84f, r * 0.32f),
    )

    // The animal, idling. A living animal is what makes an open cell worth returning to.
    val bounce = 1f + (1f - t) * 0.2f
    val idle = sin((clock + cell.idlePhase) * 2.4f)
    val at = Offset(centre.x, centre.y - r * 0.34f * t + idle * r * 0.05f)
    val size = r * 1.15f * t * bounce
    val animal = cell.animal
    if (animal != null) animal.draw(this, at, size) else drawCurled(at, size)

    // Top half, lifted clear of the contents.
    val lid = centre.y - r * (0.50f + 0.78f * t)
    drawPath(
        Path().apply {
            moveTo(centre.x - r, lid)
            cubicTo(centre.x - r, lid - r * 1.25f, centre.x + r, lid - r * 1.25f, centre.x + r, lid)
            close()
        },
        Island.CoconutShell,
    )
    drawOval(Island.CoconutFlesh, Offset(centre.x - r * 0.92f, lid - r * 0.16f), Size(r * 1.84f, r * 0.32f))
    drawArc(
        color = ShellRim,
        startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(centre.x - r, lid - r * 1.05f),
        size = Size(r * 2f, r * 2.1f),
        style = Stroke(width = d * 0.045f),
    )
}

