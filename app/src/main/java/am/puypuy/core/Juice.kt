package am.puypuy.core

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * How the app feels under a finger. One vocabulary, called from all six games.
 *
 * See docs/toddler-ux.md. The rules that shaped this file:
 *
 *  - A child of three taps, and if nothing happens she taps again, and then she hands the
 *    device back. So every touch produces something, and it starts on touch-DOWN.
 *  - Low-relevance animation measurably *costs* comprehension, so nothing here sparkles for
 *    decoration. Particles come from the thing she touched, in that thing's own colours.
 *  - Haptics are the one channel that survives a plane with the volume at zero.
 */
object Juice {

    /** Nothing in the physical world moves at a constant rate. LinearEasing is banned. */
    val Enter = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val Exit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Press: tight, so the object answers inside one frame. */
    fun <T> press(): SpringSpec<T> = spring(dampingRatio = 0.75f, stiffness = 1500f)

    /** Release, pop, land: loose enough to overshoot, which is what reads as rubber. */
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.35f, stiffness = 400f)

    /** Heavy things — a coconut landing, a full belly. */
    fun <T> heavy(): SpringSpec<T> = spring(dampingRatio = 0.55f, stiffness = 260f)

    const val PressScale = 0.92f

    /** Volume-preserving squash: x·y ≈ 1 is the difference between rubber and a resized image. */
    fun squashX(k: Float) = 1f + k
    fun squashY(k: Float) = 1f / (1f + k)

    fun impactK(speedPxPerSec: Float): Float = min(0.30f, speedPxPerSec / 9000f)
}

/**
 * Haptics. `HapticFeedbackConstants` on a View needs no VIBRATE permission, so the app's
 * zero-permissions rule is intact.
 *
 * `REJECT` is never used anywhere in this app, because nothing is ever rejected.
 */
class Haptics(private val view: View?) {
    fun tap() = fire(HapticFeedbackConstants.KEYBOARD_TAP)

    fun confirm() = fire(
        if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.LONG_PRESS
    )

    private fun fire(constant: Int) {
        view?.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}

/**
 * Screen shake on the trauma model: `displacement = max * trauma²`.
 *
 * Squaring is what makes a small hit feel small. Roll is deliberately zero — rotating a
 * full-screen scene reads as an earthquake rather than as impact.
 */
class Shake {
    var trauma by mutableFloatStateOf(0f)
        private set
    var offsetX by mutableFloatStateOf(0f)
        private set
    var offsetY by mutableFloatStateOf(0f)
        private set

    fun add(amount: Float) {
        trauma = (trauma + amount).coerceAtMost(1f)
    }

    internal fun advance(dt: Float, seed: Float, maxPx: Float) {
        if (trauma <= 0f) {
            offsetX = 0f; offsetY = 0f; return
        }
        trauma = (trauma - dt * 3.0f).coerceAtLeast(0f)
        val d = maxPx * trauma * trauma
        offsetX = sin(seed * 37f) * d
        offsetY = sin(seed * 53f + 1.7f) * d
    }

    companion object {
        const val Crack = 0.35f
        const val Pop = 0.20f
        const val Bite = 0.15f
        const val Thud = 0.25f
    }
}

private class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val size: Float, val colour: Color, val shape: Int,
    var spin: Float, var angle: Float = 0f, var life: Float = 0f, val ttl: Float = 0.9f,
)

/**
 * One particle field per screen, driven by a single frame loop and read through one snapshot
 * counter inside a Canvas. Per-object `animateFloatAsState` would be the obvious way to write
 * this and would also be what kills the frame rate: recomposition, not draw cost, is the risk.
 */
class Particles(private val capacity: Int = 300) {
    private val items = ArrayList<Particle>(capacity)

    /** Bumped once per frame; a Canvas that reads it redraws. */
    var tick by mutableFloatStateOf(0f)
        internal set

    /**
     * Throw pieces from [at]. [colours] must be sampled from the object she touched — colour
     * inheritance is what makes a burst read as caused by her rather than as app confetti.
     */
    /**
     * Pixels per dp, set once by [FrameLoop]. Everything below is expressed in dp and scaled
     * here, because a burst measured in raw pixels is a different burst on every device: at
     * 300px/s a spark crosses a third as much of a 3x phone as of a 1x one, and on a tablet
     * the whole effect reads as a small puff in the middle of a large screen.
     */
    var pxPerDp: Float = 1f

    fun burst(
        at: Offset,
        colours: List<Color>,
        count: Int = 14,
        speed: ClosedFloatingPointRange<Float> = 300f..700f,
        spread: Float = (2f * Math.PI).toFloat(),
        direction: Float = 0f,
        sizeRange: ClosedFloatingPointRange<Float> = 8f..18f,
        ttl: Float = 0.9f,
    ) {
        if (colours.isEmpty()) return
        repeat(count) {
            if (items.size >= capacity) return@repeat
            val a = direction + (kotlin.random.Random.nextFloat() - 0.5f) * spread
            val v = (speed.start + kotlin.random.Random.nextFloat() * (speed.endInclusive - speed.start)) * pxPerDp
            items += Particle(
                x = at.x, y = at.y, vx = cos(a) * v, vy = sin(a) * v,
                size = (sizeRange.start + kotlin.random.Random.nextFloat() * (sizeRange.endInclusive - sizeRange.start)) * pxPerDp,
                colour = colours.random(), shape = kotlin.random.Random.nextInt(3),
                spin = (kotlin.random.Random.nextFloat() - 0.5f) * 720f, ttl = ttl,
            )
        }
    }

    internal fun advance(dt: Float, gravity: Float) {
        if (items.isEmpty()) return
        val it = items.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life += dt
            if (p.life >= p.ttl) { it.remove(); continue }
            p.vy += gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.angle += p.spin * dt
        }
    }

    fun clear() = items.clear()

    fun draw(scope: DrawScope) = with(scope) {
        for (p in items) {
            val t = (p.life / p.ttl).coerceIn(0f, 1f)
            val a = (1f - t) * (1f - t)
            val c = p.colour.copy(alpha = a)
            val s = p.size
            when (p.shape) {
                0 -> drawCircle(c, s * 0.5f, Offset(p.x, p.y))
                1 -> rotate(p.angle, Offset(p.x, p.y)) {
                    drawRect(c, Offset(p.x - s * 0.5f, p.y - s * 0.3f), Size(s, s * 0.6f))
                }
                else -> rotate(p.angle, Offset(p.x, p.y)) {
                    drawOval(c, Offset(p.x - s * 0.5f, p.y - s * 0.35f), Size(s, s * 0.7f))
                }
            }
        }
    }
}

/** The one frame loop a screen is allowed to have. */
@Composable
fun FrameLoop(
    particles: Particles?,
    shake: Shake?,
    gravity: Float = 1400f,
    maxShakePx: Float = 24f,
    onFrame: (dt: Float) -> Unit = {},
) {
    // Everything in here is authored in dp and converted once, so a burst, a fall and a shake
    // are the same GESTURE on a 1x phone, a 3x phone and a tablet — not the same pixel count.
    val scale = LocalDensity.current.density
    particles?.pxPerDp = scale

    LaunchedEffect(particles, shake) {
        var last = 0L
        var seed = 0f
        particles?.pxPerDp = scale
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f
                else ((now - last) / 1_000_000_000.0).toFloat().coerceAtMost(0.064f)
                last = now
                seed += dt
                particles?.advance(dt, gravity * scale)
                shake?.advance(dt, seed, maxShakePx * scale)
                onFrame(dt)
                particles?.tick = seed
            }
        }
    }
}
