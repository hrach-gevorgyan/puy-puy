package am.puypuy.games

import am.puypuy.R
import am.puypuy.core.FrameLoop
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Կառուցի՛ր — stack the coconuts, biggest at the bottom.
 *
 * docs/games.md §2. The teaching is size ordering, which a three-year-old can do with her hands
 * long before she can talk about it, and which is the skill underneath counting. Nothing is
 * explained and nothing is asked: she puts a coconut on the pile, and the pile holds or it
 * does not.
 *
 * **Toppling is the good part.** Knocking a tower down is why children build them, so a bad
 * stack comes down in a heap she can rebuild at once — no sound of disapproval, no reaction
 * from him except delight. [Tower] holds the rule; `TowerTest` holds it to it.
 */
class StackGame : MiniGame {

    override val tileImage = R.drawable.tile_stack
    override val tileLabel = R.string.game_stack
    override val voiceKey = "game_stack"

    private var tower = Tower()

    /** Where each coconut is right now, keyed by its size. */
    private val spots = mutableStateMapOf<Int, Animatable<Offset, *>>()

    /** Set on the frame the pile gives up, so the fall happens outside the frame loop. */
    private var falling by mutableStateOf(false)

    /** Set when a finished tower should tumble down and be laid out again to build afresh. */
    private var freshTower by mutableStateOf(false)

    /** The coconut in her hand, if any. */
    private var carried by mutableStateOf<Int?>(null)

    /** How hard the pile is wobbling, 0..1. */
    private var wobble by mutableFloatStateOf(0f)

    override fun onEnter() {
        tower.restart()
        spots.clear()
    }

    override fun onExit() = spots.clear()

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

        fun dp(v: Float) = with(density) { v.dp.toPx() }
        // Every size and position is arithmetic, so it lives in Layout where LayoutTest can
        // assert that the coconuts do not overlap and the finished tower fits on the screen.
        //
        // He stands in the bottom-right corner, so the coconuts are laid out in the width that
        // is LEFT once he has his corner. Laid out across the whole screen, the biggest one
        // ended up underneath him.
        // He stands halfway up the right-hand edge, clear of the sand altogether, so the
        // coconuts get the whole width and the whole bottom of the screen. Standing him in the
        // corner meant reserving room for him, and every version of that reservation cost the
        // game a coconut.
        val full = with(density) { area.width.toDp() }
        val tall = with(density) { area.height.toDp() }
        val his = (tall * 0.15f).coerceIn(100.dp, 170.dp)
        val geom = Layout.tower(full, tall)
        val count = geom.radii.size
        // Clamped. For one frame the screen is 0x0, the layout falls back to its smallest
        // tower, and the model is still holding the sizes from the last screen it saw — which
        // is exactly how this crashed the moment the game was opened.
        fun radius(size: Int) = with(density) {
            geom.radii[(size - 1).coerceIn(0, geom.radii.lastIndex)].toPx()
        }
        val base = with(density) { Offset(geom.baseX.toPx(), geom.baseY.toPx()) }

        fun restPlace(size: Int) = with(density) {
            Offset(geom.restX[(size - 1).coerceIn(0, geom.restX.lastIndex)].toPx(), geom.restY.toPx())
        }

        fun stackPlace(size: Int): Offset {
            // offsets[i] is how far stacked[i] sits off the one BELOW it, measured in that
            // one's radius — so each step uses the radius of stacked[i - 1], not of stacked[i].
            // Pairing them the other way put every coconut from the third up in the wrong
            // place, which is what made a finished tower look like a heap.
            val mine = tower.stacked.indexOf(size)
            var y = base.y
            var x = base.x
            for (i in 0 until maxOf(mine, 0)) y -= radius(tower.stacked[i]) * 1.75f
            for (i in 1..maxOf(mine, 0)) {
                x += radius(tower.stacked[i - 1]) * tower.offsets.getOrElse(i) { 0f }
            }
            return Offset(x, y - radius(size) * 0.9f)
        }

        fun spot(size: Int): Animatable<Offset, *> = spots.getOrPut(size) {
            Animatable(restPlace(size), Offset.VectorConverter)
        }

        FrameLoop(particles = fx, shake = quake) { dt ->
            clock += dt
            // A tower that is out of order, or leaning too far, wobbles harder every second
            // it stands — and then it goes.
            val unsound = tower.stacked.zipWithNext().any { (a, b) -> b > a } ||
                abs(tower.offsets.sum()) > Tower.LEAN ||
                tower.offsets.any { abs(it) > Tower.BALANCE }
            wobble = if (unsound) {
                (wobble + dt * 1.1f).coerceAtMost(1f)
            } else {
                (wobble - dt * 2f).coerceAtLeast(0f)
            }
            // And then it goes. A tower that wobbles for ever teaches nothing; one that falls
            // a second after a bad placement is the lesson landing while she is still watching.
            if (wobble >= 1f && tower.stacked.isNotEmpty()) {
                wobble = 0f
                falling = true
            }
        }

        LaunchedEffect(freshTower) {
            if (!freshTower) return@LaunchedEffect
            // She built it. After a moment to admire it, it comes down and the coconuts are
            // back on the sand — otherwise a finished tower is the end of the game.
            delay(2200)
            topple(::restPlace, ::spot, moment, coroutines)
            freshTower = false
        }

        LaunchedEffect(falling) {
            if (!falling) return@LaunchedEffect
            topple(::restPlace, ::spot, moment, coroutines)
            falling = false
        }

        LaunchedEffect(area, count) {
            if (area.width == 0) return@LaunchedEffect
            // A short screen gets a shorter tower of bigger coconuts rather than a taller one
            // of crumbs, so the model is built to whatever the layout decided will fit.
            tower = Tower((1..count).toList())
            spots.clear()
            tower.loose.forEach { spot(it) }
            delay(800)
            scope.puypuy.react(PuypuyState.Waving)
            // Nothing plays itself. He waves, and the coconuts are hers to move.
        }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .onSizeChanged { area = it }
                .pointerInput(area, count) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val p = down.position

                        // Whichever loose coconut she is nearest, with a floor so the small
                        // ones are as easy to pick up as the big ones.
                        val hit = tower.loose
                            .map { it to spot(it).value }
                            .filter { (size, at) ->
                                hypot(p.x - at.x, p.y - at.y) <= maxOf(radius(size), dp(63f))
                            }
                            .minByOrNull { (_, at) -> hypot(p.x - at.x, p.y - at.y) }
                            ?.first

                        if (hit == null) {
                            // The pile: she may knock it down whenever she likes, and that is a
                            // perfectly reasonable thing to want.
                            if (tower.stacked.isNotEmpty() &&
                                abs(p.x - base.x) < radius(count) * 2f && p.y < base.y + radius(count)
                            ) {
                                topple(::restPlace, ::spot, moment, coroutines)
                            } else {
                                moment.ack(
                                    p, listOf(Island.SandDark, Color(0xFFF3E6CC)),
                                    count = 7, speed = 120f..300f, spread = 2.0f, direction = -1.57f,
                                )
                            }
                            return@awaitEachGesture
                        }

                        // Carried. It follows the finger exactly — a coconut that lags behind
                        // her hand is a coconut she does not believe she is holding.
                        carried = hit
                        var dragged = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            dragged = true
                            change.consume()
                            coroutines.launch { spot(hit).snapTo(change.position) }
                        }
                        carried = null

                        val at = spot(hit).value
                        // Where the top of the pile currently is, so a drop can be measured
                        // against it rather than against the middle of the screen.
                        val top = tower.stacked.lastOrNull()
                        val topAt = top?.let { stackPlace(it) } ?: base
                        val reach = radius(top ?: count) * 3f
                        val nearPile = abs(at.x - topAt.x) < reach && at.y < topAt.y + reach
                        if (!dragged || nearPile) {
                            // How far off centre she let go, in radii of the coconut below.
                            // This is the whole difference between choosing a coconut and
                            // placing one.
                            val offset = if (!dragged || top == null) 0f
                            else (at.x - topAt.x) / radius(top)
                            put(hit, offset, ::stackPlace, ::restPlace, ::spot, moment, scope, coroutines)
                        } else {
                            // Dropped out on the sand: it simply stays where she put it.
                            moment.ack(at, listOf(Island.SandDark), count = 5, speed = 60f..160f)
                        }
                    }
                }
        ) {
            Scenery(seed = 12, modifier = Modifier.fillMaxSize())

            // Drawn BEFORE the coconuts, so he is behind them: he is company, not an obstacle,
            // and nothing he does may cover the thing she is reaching for.
            Puypuy(
                controller = scope.puypuy,
                size = his,
                // Up on the right, out of the sand: the tower is built in the middle and rises
                // past him, so he is company beside the play rather than standing in it.
                modifier = Modifier.align(Alignment.CenterEnd),
            )

            Canvas(Modifier.fillMaxSize()) {
                @Suppress("UNUSED_EXPRESSION") fx.tick
                if (area.width == 0) return@Canvas
                translate(quake.offsetX, quake.offsetY) {
                    // Loose ones on the sand. The one that belongs next nudges once she has
                    // knocked a tower over, and waves outright if she has done it twice.
                    for (size in tower.loose) {
                        if (size == carried) continue
                        val at = spot(size).value
                        val helping = tower.help > 0 && size == tower.nextRight
                        val lift = if (helping) abs(sin(clock * 2.4f)) * radius(size) * 0.35f * tower.help.toFloat() else 0f
                        if (helping && tower.help > 1) {
                            drawCircle(Color.White.copy(alpha = 0.35f), radius(size) * 1.25f, at)
                        }
                        drawCoconut(radius(size) * 2f, Offset(at.x - radius(size), at.y - radius(size) - lift))
                    }
                    carried?.let { size ->
                        val at = spot(size).value
                        val r = radius(size) * 1.12f
                        drawCircle(Island.SandDark.copy(alpha = 0.18f), r, Offset(at.x, base.y))
                        drawCoconut(r * 2f, Offset(at.x - r, at.y - r))
                    }

                    // The pile, leaning further the longer a bad stack survives.
                    rotate(wobble * 9f * sin(clock * 7f), base) {
                        for (size in tower.stacked) {
                            val at = spot(size).value
                            drawCoconut(radius(size) * 2f, Offset(at.x - radius(size), at.y - radius(size)))
                        }
                    }
                }
                fx.draw(this)
            }

        }
    }

    /** She has put one on the pile. It holds, or the whole thing comes down. */
    private fun put(
        size: Int,
        offset: Float,
        stackPlace: (Int) -> Offset,
        restPlaceOf: (Int) -> Offset,
        spot: (Int) -> Animatable<Offset, *>,
        moment: Moment,
        scope: GameScope,
        coroutines: CoroutineScope,
    ) {
        val held = tower.place(size, offset)
        val onPile = size in tower.stacked
        coroutines.launch {
            if (!onPile) {
                // Refused — the pile is finished, or it is already falling. It goes back to
                // the sand rather than flying to a place on a tower that does not exist,
                // which is how five coconuts ended up stacked on one spot.
                @Suppress("UNCHECKED_CAST")
                (spot(size) as Animatable<Offset, androidx.compose.animation.core.AnimationVector2D>)
                    .animateTo(restPlaceOf(size), Juice.bouncy())
                return@launch
            }
            @Suppress("UNCHECKED_CAST")
            (spot(size) as Animatable<Offset, androidx.compose.animation.core.AnimationVector2D>)
                .animateTo(stackPlace(size), spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
            if (held) {
                moment.happened(
                    at = stackPlace(size),
                    colours = listOf(Island.CoconutShell, Island.SandDark, Color.White),
                    // The size word, on the coconut she just placed. Rarely: it is a label on
                    // something she did, not a lesson.
                    word = if (tower.stacked.size % 2 == 0) {
                        if (size >= 4) "size_big" else "size_small"
                    } else {
                        null
                    },
                    trauma = Shake.Thud,
                    count = 10,
                )
                // One coconut left: «Բան չմնաց».
                if (tower.loose.size == 1) moment.almost()
                if (tower.finished) {
                    delay(700)
                    scope.puypuy.react(PuypuyState.Happy)
                    freshTower = true
                    moment.happened(
                        at = stackPlace(size),
                        colours = listOf(Color.White, Island.CoconutFlesh, Color(0xFFFFD166)),
                        effect = "chime",
                        trauma = Shake.Crack,
                        count = 30,
                        // She built the whole tower. This one is always worth saying so.
                        milestone = true,
                    )
                }
            }
        }
    }

    /** Everything comes down in a heap, which is the best part of building a tower. */
    private fun topple(
        restPlace: (Int) -> Offset,
        spot: (Int) -> Animatable<Offset, *>,
        moment: Moment,
        coroutines: CoroutineScope,
    ) {
        val falling = tower.stacked.toList()
        tower.collapse()
        moment.happened(
            at = spot(falling.last()).value,
            colours = listOf(Island.SandDark, Island.CoconutShell, Color(0xFFF3E6CC)),
            reaction = PuypuyState.Surprised,
            trauma = Shake.Crack,
            count = 24,
            // Loud and good fun, and not a thing to be congratulated for.
            success = false,
        )
        // «Ոչինչ» — never mind. Only now and then, so it is company rather than commentary.
        moment.encourage()
        coroutines.launch {
            for (size in falling) {
                @Suppress("UNCHECKED_CAST")
                (spot(size) as Animatable<Offset, androidx.compose.animation.core.AnimationVector2D>)
                    .animateTo(restPlace(size), tween(520, easing = Juice.Exit))
            }
        }
    }

    private companion object {
        /** Where the sand starts under everything. */
        const val GROUND_DP = 60f

        /** How much of his sprite box he actually fills across. The rest is air. */
        const val BODY = 0.47f
    }
}
