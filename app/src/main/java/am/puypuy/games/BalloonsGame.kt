package am.puypuy.games

import am.puypuy.R
import am.puypuy.core.FrameLoop
import am.puypuy.core.GameScope
import am.puypuy.core.Island
import am.puypuy.core.MiniGame
import am.puypuy.core.Moment
import am.puypuy.core.Paint
import am.puypuy.core.Particles
import am.puypuy.core.Puypuy
import am.puypuy.core.PuypuyState
import am.puypuy.core.Shake
import am.puypuy.core.rememberMoment
import am.puypuy.ui.Sky
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Փուչիկներ — balloons rise, and every one of them pops.
 *
 * docs/games.md §1 is the specification and [touch] is all of it. There is no wanted colour,
 * no round and no wrong balloon, because nothing is ever asked. A colour is named AFTER a pop,
 * roughly one in four: a label on something she just did.
 */
class BalloonsGame : MiniGame {

    override val tileImage = R.drawable.tile_balloons
    override val tileLabel = R.string.game_balloons
    override val voiceKey = "game_balloons"

    private val balloons = mutableStateListOf<Balloon>()
    private val clouds = mutableStateListOf<Cloud>()
    private var pops = 0

    /** The colour the sky is mostly about right now, and the one in his paw. */
    private var run = TeachingRun(Paint.entries)

    override fun onEnter() {
        balloons.clear()
        clouds.clear()
        pops = 0
        run = TeachingRun(Paint.entries)
    }

    override fun onExit() {
        balloons.clear()
        clouds.clear()
    }

    @Composable
    override fun Content(scope: GameScope) {
        val density = LocalDensity.current
        val moment = rememberMoment(scope)
        val fx = remember { Particles() }
        val quake = remember { Shake() }
        moment.particles = fx
        moment.shake = quake

        var area by remember { mutableStateOf(IntSize.Zero) }
        var nextSpawn by remember { mutableFloatStateOf(0f) }
        var clock by remember { mutableFloatStateOf(0f) }
        // How far off the ground a balloon has carried him, in pixels.
        var lift by remember { mutableFloatStateOf(0f) }
        var carried by remember { mutableStateOf<Balloon?>(null) }
        // Seconds until he has blown up a new one, after she popped the one in his paw.
        var refilling by remember { mutableFloatStateOf(0f) }

        fun dp(v: Float) = with(density) { v.dp.toPx() }

        FrameLoop(particles = fx, shake = quake) { dt ->
            clock += dt
            if (refilling > 0f) refilling = (refilling - dt).coerceAtLeast(0f)
            if (area.width == 0) return@FrameLoop
            val ground = area.height - dp(GROUND_DP)
            // Frame-rate independent easing: how much of the gap survives this frame.
            val ease = { life: Float -> 1f - (1f - dt / life).coerceIn(0f, 1f) }

            val dead = mutableListOf<Balloon>()
            for (b in balloons) {
                if (b.popping > 0f) {
                    // Squash, stretch, gone. The squash is the answer to the finger, and it
                    // lands in the first frame.
                    b.popping -= dt * 7f
                    if (b.popping <= 0f) dead += b
                    continue
                }
                // A push fades and the balloon returns to simply rising.
                b.vy += (-b.rise - b.vy) * ease(0.35f)
                b.drift *= 1f - ease(0.9f)
                b.y += b.vy * dt
                b.phase += dt
                b.x = (b.baseX + sin(b.phase * b.swayRate) * b.swayWidth + b.drift)
                    .coerceIn(b.radius * 0.6f, area.width - b.radius * 0.6f)
                // The ground is solid: a balloon pushed down bounces off the sand.
                if (b.y + b.radius > ground && b.vy > 0f) {
                    b.y = ground - b.radius
                    b.vy = -abs(b.vy) * 0.55f - b.rise * 0.4f
                    moment.ack(
                        Offset(b.x, ground), listOf(Island.SandDark),
                        count = 4, speed = 60f..160f, direction = -1.57f, spread = 1.6f,
                    )
                }
                if (b.y + b.radius < 0f) dead += b
                else if (b.y < b.radius * 2f) b.alpha = (b.y / (b.radius * 2f)).coerceIn(0f, 1f)
            }
            balloons.removeAll(dead)
            if (carried != null && carried !in balloons) carried = null

            // While he holds one it carries him up; when it slips he settles back down.
            val heHeight = (area.height * 0.17f).coerceIn(dp(110f), dp(180f))
            carried?.let { held ->
                lift += (dp(LIFT_DP) - lift) * ease(0.30f)
                held.baseX = heHeight * 0.55f
                held.drift = 0f
                held.y = area.height - dp(GROUND_DP) - lift - heHeight * 1.25f
                held.vy = 0f
            }
            if (carried == null && lift > 0.5f) lift -= lift * ease(0.22f)

            for (c in clouds) {
                c.x += c.speed * dt
                if (c.x - c.width > area.width) c.x = -c.width
                if (c.wobble > 0f) c.wobble = (c.wobble - dt * 2.2f).coerceAtLeast(0f)
            }

            // The colour is on a clock of its own, so filling an empty sky cannot rush it.
            if (run.advance(dt)) {
                scope.puypuy.react(PuypuyState.Waving)
                scope.audio.say(run.subject.voiceKey)
            }

            // The sky never empties: one arrives ~600ms after any pop, and the floor keeps
            // the screen obviously full of things worth touching.
            val lanes = lanesFor(area.width, ::dp)
            if (balloons.size < lanes * 3 && (balloons.size < lanes + 4 || clock >= nextSpawn)) {
                balloons += spawn(area, lanes, ::dp)
                nextSpawn = clock + 0.6f
            }
        }

        LaunchedEffect(area) {
            if (area.width == 0) return@LaunchedEffect
            if (clouds.isEmpty()) {
                repeat(3) { i ->
                    clouds += Cloud(
                        x = area.width * (0.10f + i * 0.36f),
                        y = area.height * (0.07f + 0.09f * i),
                        width = dp((90..130).random().toFloat()),
                        speed = dp((4..9).random().toFloat()),
                    )
                }
            }
            delay(700)
            scope.puypuy.react(PuypuyState.Waving)
            // The colour he is holding, named on the way in. Without this the first word she
            // hears is the FIRST CHANGE, half a minute later — so the game opens on a balloon
            // in his paw that nothing has ever accounted for.
            scope.audio.say(run.subject.voiceKey)
            // Nothing plays itself beyond that: a game that takes the first turn has taken
            // it FROM her.
        }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .onSizeChanged { area = it }
                .pointerInput(area) {
                    // Every finger that goes down, not just the first: two balloons at once
                    // is a delight, and a child grabs with both hands.
                    awaitPointerEventScope {
                        while (true) {
                            for (change in awaitPointerEvent().changes) {
                                if (!change.pressed || change.previousPressed) continue
                                change.consume()
                                touch(
                                    p = change.position,
                                    area = area,
                                    moment = moment,
                                    scope = scope,
                                    dp = ::dp,
                                    lift = lift,
                                    refilling = refilling > 0f,
                                    onCarry = { carried = it },
                                    onRefill = { refilling = REFILL_S },
                                )
                            }
                        }
                    }
                }
        ) {
            Sky(Modifier.fillMaxSize(), horizon = 0.86f)

            Canvas(Modifier.fillMaxSize()) {
                @Suppress("UNUSED_EXPRESSION") fx.tick
                translate(quake.offsetX, quake.offsetY) {
                    clouds.forEach { drawCloud(it) }
                    balloons.forEach { if (it !== carried) drawBalloon(it) }
                }
                fx.draw(this)
            }

            Puypuy(
                controller = scope.puypuy,
                size = (maxHeight * 0.17f).coerceIn(110.dp, 180.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset { IntOffset(0, -lift.toInt()) },
                // He holds it himself, so the string stays tied to his paw through every wave,
                // hop and stride. Drawn over him by the game, it hung in the air while his
                // hand moved out from under it.
                holding = if (refilling <= 0f) run.subject.color else null,
            )

            // The balloon that has picked him up is drawn over him, so its string reads as
            // being in his paw rather than behind his back. The one he always holds is drawn
            // by the sprite itself.
            Canvas(Modifier.fillMaxSize()) {
                translate(quake.offsetX, quake.offsetY) { carried?.let { drawBalloon(it) } }
            }
        }
    }

    /**
     * docs/games.md §1's touch table. Every coordinate on this screen is one of these rows,
     * and the last one is a catch-all, so there is no such thing as a touch that does nothing.
     */
    private fun touch(
        p: Offset,
        area: IntSize,
        moment: Moment,
        scope: GameScope,
        dp: (Float) -> Float,
        lift: Float,
        refilling: Boolean,
        onCarry: (Balloon?) -> Unit,
        onRefill: () -> Unit,
    ) {
        val hit = balloons
            .filter { it.popping <= 0f }
            .filter { hypot(p.x - it.x, p.y - it.y) <= maxOf(it.radius, dp(HIT_FLOOR_DP)) }
            .minByOrNull { hypot(p.x - it.x, p.y - it.y) }
        if (hit != null) {
            pop(moment, hit)
            return
        }

        // The balloon in his paw. It pops like any other, and he blows up another.
        val him = (area.height * 0.17f).coerceIn(dp(110f), dp(180f))
        val heldAt = Offset(him * 0.62f, area.height - him * 0.72f - lift)
        if (!refilling && hypot(p.x - heldAt.x, p.y - heldAt.y) <= maxOf(him * 0.20f, dp(HIT_FLOOR_DP))) {
            onRefill()
            moment.happened(
                at = heldAt,
                colours = listOf(run.subject.color, run.subject.color.copy(alpha = 0.75f), Color.White),
                word = run.subject.voiceKey,
                trauma = Shake.Pop,
                effect = "star_${(1..3).random()}",
                count = 20,
            )
            return
        }

        // Him: he grabs the nearest balloon and it carries him up a little, feet kicking,
        // until it slips out of his paw and he settles back onto the sand.
        if (p.x < him && p.y > area.height - him - lift) {
            val grab = balloons.filter { it.popping <= 0f }.minByOrNull { it.y }
            if (grab != null) {
                onCarry(grab)
                scope.puypuy.react(PuypuyState.Happy)
                scope.launch {
                    delay(2200)
                    onCarry(null)
                }
            }
            return
        }

        // A cloud: it wobbles and lets go of two raindrops.
        val cloud = clouds.firstOrNull {
            abs(p.x - it.x) < it.width * 0.7f && abs(p.y - it.y) < it.width * 0.34f
        }
        if (cloud != null) {
            cloud.wobble = 1f
            moment.ack(
                Offset(cloud.x, cloud.y + cloud.width * 0.20f),
                listOf(Color(0xFFBFE4EA), Color.White),
                count = 2, speed = 40f..90f, direction = 1.57f, spread = 0.4f, ttl = 1.2f,
            )
            return
        }

        // Empty sky: small bubbles from the finger, and every balloon within reach is pushed
        // away from it — a miss still reads as influence, and it is how a balloon gets driven
        // down to the sand to bounce.
        moment.ack(p, listOf(Color.White, Color(0xFFDDF1F2)), count = 3, ttl = 0.8f)
        for (b in balloons) {
            val d = hypot(p.x - b.x, p.y - b.y)
            val reach = b.radius * 5f
            if (d in 1f..reach) {
                val force = 1f - d / reach
                b.drift += (b.x - p.x) / d * b.radius * 0.55f * force
                b.vy += (b.y - p.y) / d * dp(220f) * force
            }
        }
    }

    /**
     * Lanes are counted off the screen, not fixed at five. A tablet held sideways is over
     * 1200dp wide: five lanes across it put a 70dp balloon in the middle of a 240dp column,
     * which is the difference between a layout that scales and one that stretches.
     */
    private fun lanesFor(widthPx: Int, dp: (Float) -> Float): Int =
        Math.round(widthPx / dp(LANE_DP)).coerceIn(3, 9)

    private fun spawn(area: IntSize, lanes: Int, dp: (Float) -> Float): Balloon {
        val laneW = area.width.toFloat() / lanes
        // Big enough to fill its lane on a tablet, never bigger than a toddler's two hands.
        val radius = minOf(laneW * 0.42f, dp((58..82).random().toFloat() * 1.35f))
        // The emptiest lane, not a random one. Picking at random piles four balloons into one
        // column while a third of the sky stays bare, and a stack reads as one fat balloon.
        val lane = (0 until lanes)
            .groupBy { l ->
                balloons.count { b ->
                    (b.x / laneW).toInt().coerceIn(0, lanes - 1) == l && b.y > area.height * 0.45f
                }
            }
            .minBy { it.key }
            .value
            .random()
        return Balloon(
            baseX = (laneW * (lane + 0.5f)).coerceIn(radius * 1.1f, area.width - radius * 1.1f),
            y = area.height + radius * 0.6f,
            radius = radius,
            rise = dp((30..60).random().toFloat()),
            paint = run.next(),
            swayRate = 0.6f + kotlin.random.Random.nextFloat() * 0.7f,
            swayWidth = dp((8..16).random().toFloat()),
        )
    }

    /**
     * Every balloon pops. One that matches the colour he is holding pops LOUDER — the word, a
     * bigger burst, a bigger reaction — and one that does not pop is exactly as welcome.
     *
     * This is the shape teaching has to take at three: the target only ever adds. There is no
     * wrong balloon, nothing is refused, and a child who ignores the colour entirely is playing
     * the game correctly.
     */
    private fun pop(moment: Moment, b: Balloon) {
        b.popping = 1f
        pops++
        val matches = b.paint == run.subject
        moment.happened(
            at = Offset(b.x, b.y),
            colours = listOf(b.paint.color, b.paint.color.copy(alpha = 0.75f), Color.White),
            // No word here. He names a colour when he CHANGES to it, once, and then lets her
            // play: a word on every matching pop is the same word four times in ten seconds,
            // which stops being a label and becomes nagging.
            word = null,
            trauma = if (matches) Shake.Pop * 1.4f else Shake.Pop,
            effect = "star_${(1..3).random()}",
            count = if (matches) 24 else 14,
            // Six in a row is a run worth a word of its own.
            milestone = pops % 6 == 0,
        )
    }

    private companion object {
        /** One lane per this much width, so the sky is as full on a tablet as on a phone. */
        const val LANE_DP = 110f

        /** Hit floor, so even the smallest balloon clears the 126dp target rule. */
        const val HIT_FLOOR_DP = 63f

        /** Where the sand starts, so a balloon pushed down has something to bounce off. */
        const val GROUND_DP = 40f

        /** How long he takes to blow up a new one after she pops the one in his paw. */
        const val REFILL_S = 1.4f


        /** How far a balloon carries him off the sand. */
        const val LIFT_DP = 120f
    }
}

private class Balloon(
    baseX: Float,
    y: Float,
    val radius: Float,
    val rise: Float,
    val paint: Paint,
    val swayRate: Float,
    val swayWidth: Float,
) {
    var baseX by mutableFloatStateOf(baseX)
    var x by mutableFloatStateOf(baseX)
    var y by mutableFloatStateOf(y)
    var vy by mutableFloatStateOf(-rise)
    var drift by mutableFloatStateOf(0f)
    var alpha by mutableFloatStateOf(1f)
    var phase by mutableFloatStateOf(0f)

    /** 1 the instant it is touched, 0 when it is gone. The whole burst is a seventh of a second. */
    var popping by mutableFloatStateOf(0f)
}

private class Cloud(x: Float, val y: Float, val width: Float, val speed: Float) {
    var x by mutableFloatStateOf(x)
    var wobble by mutableFloatStateOf(0f)
}

/** Three overlapping lobes, drifting. They squash when poked. */
private fun DrawScope.drawCloud(c: Cloud) {
    val squash = 1f + sin(c.wobble * 14f) * 0.12f * c.wobble
    val r = c.width * 0.28f
    for ((dx, scale) in listOf(-0.62f to 0.78f, 0f to 1f, 0.62f to 0.72f)) {
        drawCircle(
            Color.White.copy(alpha = 0.55f),
            radius = r * scale * squash,
            center = Offset(c.x + c.width * dx, c.y + (1f - squash) * r * 1.5f),
        )
    }
}

/** Teardrop body, knot, string, one hard highlight. */
private fun DrawScope.drawBalloon(b: Balloon) {
    // Mid-burst it squashes wide and stretches tall as it goes.
    val t = 1f - b.popping
    val burst = if (b.popping > 0f) sin(t * Math.PI.toFloat()) else 0f
    val stretchY = 1f + 0.55f * burst
    val stretchX = 1f - 0.45f * burst
    val fade = if (b.popping > 0f) b.popping.coerceIn(0f, 1f) else 1f

    val c = b.paint.color.copy(alpha = b.alpha * fade)
    val r = b.radius
    val centre = Offset(b.x, b.y)

    drawPath(
        Path().apply {
            moveTo(centre.x, centre.y + r * 1.16f * stretchY)
            quadraticTo(
                centre.x + sin(b.phase * b.swayRate + 1f) * r * 0.35f,
                centre.y + r * 1.7f * stretchY,
                centre.x + sin(b.phase * b.swayRate) * r * 0.20f,
                centre.y + r * 2.3f * stretchY,
            )
        },
        Color(0xFF6B6560).copy(alpha = b.alpha * 0.55f * fade),
        style = Stroke(width = r * 0.045f, cap = StrokeCap.Round),
    )

    val body = Path().apply {
        moveTo(centre.x, centre.y + r * 1.10f * stretchY)
        cubicTo(
            centre.x - r * 0.95f * stretchX, centre.y + r * 0.45f * stretchY,
            centre.x - r * 1.02f * stretchX, centre.y - r * 0.95f * stretchY,
            centre.x, centre.y - r * 1.05f * stretchY,
        )
        cubicTo(
            centre.x + r * 1.02f * stretchX, centre.y - r * 0.95f * stretchY,
            centre.x + r * 0.95f * stretchX, centre.y + r * 0.45f * stretchY,
            centre.x, centre.y + r * 1.10f * stretchY,
        )
        close()
    }
    drawPath(body, c)
    drawPath(body, Color(0xFF3A4454).copy(alpha = b.alpha * 0.75f * fade), style = Stroke(width = r * 0.07f))

    drawPath(
        Path().apply {
            moveTo(centre.x - r * 0.11f, centre.y + r * 1.06f * stretchY)
            lineTo(centre.x + r * 0.11f, centre.y + r * 1.06f * stretchY)
            lineTo(centre.x, centre.y + r * 1.24f * stretchY)
            close()
        },
        c,
    )
    drawOval(
        color = Color.White.copy(alpha = b.alpha * 0.42f * fade),
        topLeft = Offset(centre.x - r * 0.62f * stretchX, centre.y - r * 0.72f * stretchY),
        size = Size(r * 0.40f * stretchX, r * 0.58f * stretchY),
    )
}

