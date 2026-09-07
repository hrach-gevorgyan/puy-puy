package am.puypuy.core

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Պույ-պույ Ճստունի.
 *
 * Built from real mouse parts — a pointed muzzle, whiskers, blushed cheeks, hands and feet,
 * a long curled tail — so he reads as a character rather than a stack of circles. Every part
 * is a separate layer, which is what lets him tilt, squash, point and shake independently.
 *
 * He is never still: breathing, blinking, and a tail that keeps swaying whatever else is
 * happening. He stays soft dove grey and never out-saturates the fruit.
 */
enum class PuypuyState {
    /** Between things: breathing, blinking, tail swaying, small head bob. */
    Idle,

    /** Intro: big wave, wide smile, ears up. */
    Waving,

    /** Right answer: squash-and-stretch hops, eyes squeezed shut, both arms up. */
    Happy,

    /** Wrong answer: slow head shake, frown, ears drooping. Never harsh. */
    Sad,

    /** Pointing at what he should look at, and looking there himself. */
    Suggesting,

    /** Strolling along the bottom of the screen, legs swinging. */
    Walking,

    /** Startled: ears straight up, eyes wide, a quick tip backwards. */
    Surprised,

    /** Nobody has touched anything for 90 seconds. Eyes shut, ears down, slow breath. */
    Sleeping,
}

@Composable
fun PuypuySprite(
    state: PuypuyState,
    size: Dp,
    modifier: Modifier = Modifier,
    speaking: Boolean = false,
    plump: Float = 0f,
    holding: Color? = null,
) {
    val hop = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    val earLift = remember { Animatable(0f) }
    val lean = remember { Animatable(0f) }
    val arm = remember { Animatable(0f) }
    val leftArm = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val smile = remember { Animatable(0.62f) }
    val lookX = remember { Animatable(0f) }
    val lookY = remember { Animatable(0f) }
    val eyesShut = remember { Animatable(0f) }

    val ambient = rememberInfiniteTransition(label = "ambient")
    val breath by ambient.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "breath",
    )
    val blink by ambient.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3800, easing = LinearEasing), RepeatMode.Restart),
        label = "blink",
    )
    val tail by ambient.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tail",
    )
    val step by ambient.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(620, easing = LinearEasing), RepeatMode.Restart),
        label = "step",
    )
    // Roughly syllable rate: fast enough to read as speech, slow enough not to flutter.
    val talk by ambient.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(260, easing = LinearEasing), RepeatMode.Restart),
        label = "talk",
    )

    LaunchedEffect(state) {
        when (state) {
            PuypuyState.Idle -> {
                eyesShut.animateTo(0f, tween(150))
                earLift.animateTo(0f, tween(250))
                lean.animateTo(0f, tween(250))
                arm.animateTo(0f, tween(250))
                leftArm.animateTo(0f, tween(250))
                smile.animateTo(0.62f, tween(250))
                lookX.animateTo(0f, tween(400))
                lookY.animateTo(0f, tween(400))
                shake.snapTo(0f)
                hop.animateTo(0f, tween(200))
                squash.animateTo(0f, tween(200))
            }

            PuypuyState.Waving -> {
                eyesShut.animateTo(0f, tween(150))
                lean.animateTo(0.25f, tween(250))
                earLift.animateTo(0.6f, tween(250))
                smile.animateTo(1f, tween(250))
                arm.animateTo(
                    1f,
                    infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                )
            }


            PuypuyState.Happy -> {
                lean.animateTo(0f, tween(120))
                smile.animateTo(1f, tween(150))
                earLift.animateTo(1f, spring(stiffness = Spring.StiffnessLow))
                // Both paws in the air when he is pleased.
                leftArm.animateTo(1f, tween(150))
                arm.animateTo(1f, tween(150))
                eyesShut.animateTo(1f, tween(150))
                lookY.animateTo(0f, tween(150))
                // Squash before each hop and stretch at the top: the whole read of "jumping".
                squash.animateTo(
                    0f,
                    keyframes {
                        durationMillis = 1000
                        0f at 0
                        0.35f at 90
                        -0.30f at 200
                        0f at 320
                        0.28f at 380
                        -0.22f at 470
                        0f at 580
                        0.18f at 640
                        -0.14f at 710
                        0f at 820
                    },
                )
            }

            PuypuyState.Sad -> {
                leftArm.animateTo(0f, tween(150))
                arm.animateTo(0f, tween(150))
                earLift.animateTo(-1f, tween(300))
                lean.animateTo(0f, tween(150))
                smile.animateTo(-0.55f, tween(250))
                eyesShut.animateTo(0.45f, tween(250))
                lookY.animateTo(0.6f, tween(300))
                // Slow, sympathetic head shake — "not that one" — never a buzz.
                shake.animateTo(
                    0f,
                    keyframes {
                        durationMillis = 900
                        0f at 0
                        -1f at 150
                        1f at 380
                        -0.8f at 600
                        0f at 900
                    },
                )
            }

            PuypuyState.Walking -> {
                leftArm.animateTo(0f, tween(200))
                eyesShut.animateTo(0f, tween(150))
                smile.animateTo(0.7f, tween(250))
                earLift.animateTo(0.25f, tween(250))
                lean.animateTo(0f, tween(250))
                arm.animateTo(0f, tween(250))
                lookY.animateTo(0f, tween(250))
            }

            PuypuyState.Surprised -> {
                eyesShut.animateTo(0f, tween(80))
                leftArm.animateTo(0.9f, tween(120))
                arm.animateTo(0.9f, tween(120))
                earLift.animateTo(1f, tween(120))
                smile.animateTo(0.15f, tween(120))
                lookY.animateTo(-0.3f, tween(120))
                // Tips back and rocks upright again — startled, never frightened.
                lean.animateTo(
                    0f,
                    keyframes {
                        durationMillis = 500
                        0f at 0
                        -0.9f at 110
                        0.25f at 300
                        0f at 500
                    },
                )
            }

            PuypuyState.Sleeping -> {
                leftArm.animateTo(0f, tween(400))
                arm.animateTo(0f, tween(400))
                lean.animateTo(0f, tween(400))
                lookX.animateTo(0f, tween(400))
                lookY.animateTo(0f, tween(400))
                earLift.animateTo(-0.8f, tween(700))
                smile.animateTo(0.35f, tween(700))
                eyesShut.animateTo(1f, tween(700))
            }

            PuypuyState.Suggesting -> {
                eyesShut.animateTo(0f, tween(150))
                smile.animateTo(0.6f, tween(250))
                earLift.animateTo(0.5f, tween(250))
                lean.animateTo(0.5f, tween(250))
                lookX.animateTo(0.8f, tween(400))
                arm.animateTo(
                    1f,
                    infiniteRepeatable(tween(640, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                )
            }
        }
    }

    // Every so often he gestures with one paw or the other, so a long stretch of listening
    // never looks like a frozen picture.
    LaunchedEffect(state, speaking) {
        if (state != PuypuyState.Idle && !speaking) return@LaunchedEffect
        var useLeft = false
        while (true) {
            delay(1800L + (0..2200).random())
            val hand = if (useLeft) leftArm else arm
            useLeft = !useLeft
            hand.animateTo(0.85f, tween(260, easing = FastOutSlowInEasing))
            hand.animateTo(0.45f, tween(200))
            hand.animateTo(0.85f, tween(200))
            hand.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
        }
    }

    // Hops ride on the squash curve, so the feet leave the ground exactly when he stretches.
    LaunchedEffect(state) {
        if (state == PuypuyState.Happy) {
            hop.animateTo(
                0f,
                keyframes {
                    durationMillis = 1000
                    0f at 0
                    0f at 90
                    -1f at 200
                    0f at 320
                    0f at 380
                    -0.7f at 470
                    0f at 580
                    0f at 640
                    -0.4f at 710
                    0f at 820
                },
            )
        } else {
            hop.animateTo(0f, tween(200))
        }
    }

    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val breathe = 1f + 0.020f * sin(breath * 2f * Math.PI.toFloat())
        val autoBlink = blink > 0.965f
        val lidClose = maxOf(eyesShut.value, if (autoBlink) 1f else 0f)
        // While he is talking the jaw moves and the whole head nods very slightly.
        val mouthOpen = if (speaking) (0.35f + 0.65f * abs(sin(talk * Math.PI.toFloat()))) else 0f
        val walking = state == PuypuyState.Walking
        val walkPhase = if (walking) step else 0f
        // Walking bounces twice per stride, once for each foot.
        val bob = when {
            walking -> -kotlin.math.abs(sin(step * 2f * Math.PI.toFloat())) * s * 0.030f
            state == PuypuyState.Idle -> sin(breath * 2f * Math.PI.toFloat()) * s * 0.006f
            else -> 0f
        }

        translate(0f, hop.value * s * 0.14f + bob) {
            // Squash horizontally and stretch vertically about the feet.
            scale(
                scaleX = 1f + squash.value * 0.16f,
                scaleY = 1f - squash.value * 0.16f,
                pivot = Offset(this.size.width / 2f, this.size.height * 0.97f),
            ) {
                rotate(
                    degrees = 6f * lean.value + 10f * shake.value,
                    pivot = Offset(this.size.width / 2f, this.size.height),
                ) {
                    drawMouse(
                        plump = plump,
                        breathe = breathe,
                        earLift = earLift.value,
                        arm = arm.value,
                        lidClose = lidClose,
                        smile = smile.value,
                        lookX = lookX.value,
                        lookY = lookY.value,
                        tailSway = tail,
                        walkPhase = walkPhase,
                        mouthOpen = mouthOpen,
                        leftArm = leftArm.value,
                        // Talking lifts the corners of his mouth further: he sounds pleased,
                        // so he should look it.
                        extraSmile = if (speaking) 0.25f else 0f,
                        holding = holding,
                    )
                }
            }
        }
    }
}

private val Fur = Mouse.Body

/** The pale patch on his front. The tale is about this. */
private val Belly = Color(0xFFCBC6D8)

private fun DrawScope.drawMouse(
    plump: Float,
    breathe: Float,
    earLift: Float,
    arm: Float,
    lidClose: Float,
    smile: Float,
    lookX: Float,
    lookY: Float,
    tailSway: Float,
    walkPhase: Float,
    mouthOpen: Float,
    leftArm: Float,
    extraSmile: Float,
    holding: Color? = null,
) {
    val s = size.minDimension
    val cx = size.width / 2f
    val ground = s * 0.94f

    // Drawn in the illustrated style the tiles use: flat fills, no gradients, and ONE navy
    // outline of even weight around everything. The outline is not stroked — every shape is
    // drawn twice, once enlarged in navy and once filled on top, so the body and the head
    // fuse into a single silhouette with no seam where they overlap. Stroking them
    // separately is what made the old mouse read as a snowman of parts.
    val t = s * 0.019f
    val fat = 1f + 0.32f * plump.coerceIn(0f, 1f)
    val bodyW = s * 0.47f * fat * breathe
    val bodyH = s * 0.52f * breathe
    val bodyBot = ground - s * 0.045f
    val bodyMid = bodyBot - bodyH / 2f
    val headR = s * 0.205f * (1f + 0.05f * plump) * breathe
    val headY = bodyBot - bodyH - s * 0.030f

    fun oval(color: Color, x: Float, y: Float, rx: Float, ry: Float) = drawOval(
        color = color, topLeft = Offset(x - rx, y - ry), size = Size(rx * 2f, ry * 2f),
    )

    // Tail: out of the body low on the left, a long sweep down and back, then a tight curl
    // at the tip. The first attempt was one open loop, which read as a handle.
    val tailX = cx - bodyW * 0.42f
    val tailY = bodyBot - bodyH * 0.16f
    val sway = tailSway * s * 0.018f
    val tail = Path().apply {
        moveTo(tailX, tailY)
        cubicTo(
            tailX - s * 0.09f, tailY + s * 0.11f + sway,
            tailX - s * 0.25f, tailY + s * 0.05f + sway,
            tailX - s * 0.285f, tailY - s * 0.09f,
        )
        cubicTo(
            tailX - s * 0.32f, tailY - s * 0.26f - sway,
            tailX - s * 0.10f, tailY - s * 0.31f - sway,
            tailX - s * 0.135f, tailY - s * 0.16f,
        )
    }
    drawPath(tail, Mouse.Detail, style = Stroke(width = t * 3.0f, cap = StrokeCap.Round))
    drawPath(tail, Mouse.Ear, style = Stroke(width = t * 1.5f, cap = StrokeCap.Round))

    // Ears: big, round, and overlapping the head. They lift and spread with his mood.
    val earR = s * 0.135f
    val earY = headY - headR * 0.62f - earLift * s * 0.018f
    val earX = headR * 0.76f + earLift * s * 0.010f
    for (side in listOf(-1f, 1f)) {
        val ex = cx + side * earX
        oval(Mouse.Detail, ex, earY, earR + t, earR + t)
        oval(Fur, ex, earY, earR, earR)
        oval(Mouse.Ear, ex, earY + earR * 0.06f, earR * 0.60f, earR * 0.62f)
    }

    // Body and head, as one shape.
    oval(Mouse.Detail, cx, bodyMid, bodyW / 2f + t, bodyH / 2f + t)
    oval(Mouse.Detail, cx, headY, headR + t, headR + t)
    oval(Fur, cx, bodyMid, bodyW / 2f, bodyH / 2f)
    oval(Fur, cx, headY, headR, headR)
    // The pale belly. It grows with him, because it is the belly the tale is about.
    oval(Belly, cx + bodyW * 0.04f, bodyBot - bodyH * 0.30f, bodyW * 0.34f, bodyH * 0.30f)

    // Feet, in front of the body and below it. In the walk they swing and lift in turn.
    for ((index, side) in listOf(-1f, 1f).withIndex()) {
        val phase = walkPhase * 2f * Math.PI.toFloat() + index * Math.PI.toFloat()
        val swing = if (walkPhase == 0f) 0f else sin(phase) * s * 0.045f
        val lift = if (walkPhase == 0f) 0f else maxOf(0f, sin(phase)) * s * 0.030f
        val fx = cx + side * s * 0.115f + swing
        val fy = ground - s * 0.022f - lift
        oval(Mouse.Detail, fx, fy, s * 0.068f + t, s * 0.036f + t)
        oval(Mouse.Ear, fx, fy, s * 0.068f, s * 0.036f)
        toes(fx, fy - s * 0.006f, s * 0.030f, t)
    }

    // Arms, in front of the body so a wave is never lost behind it. At rest they hang against
    // the belly; raised, they swing up and out past the ear.
    var heldPaw: Offset? = null
    for ((side, raise) in listOf(-1f to leftArm, 1f to arm)) {
        val sx = cx + side * bodyW * 0.36f
        val sy = bodyBot - bodyH * 0.62f
        val angle = (52f - 160f * raise) * (Math.PI / 180f).toFloat()
        val len = s * 0.135f
        val px = sx + side * cos(angle) * len * 0.62f
        val py = sy + sin(angle) * len
        drawLine(Mouse.Detail, Offset(sx, sy), Offset(px, py), strokeWidth = t * 3.4f, cap = StrokeCap.Round)
        drawLine(Fur, Offset(sx, sy), Offset(px, py), strokeWidth = t * 1.9f, cap = StrokeCap.Round)
        oval(Mouse.Detail, px, py, s * 0.034f + t * 0.7f, s * 0.032f + t * 0.7f)
        oval(Mouse.Ear, px, py, s * 0.034f, s * 0.032f)
        toes(px, py - s * 0.004f, s * 0.020f, t)
        if (side > 0f) heldPaw = Offset(px, py)
    }

    // A balloon on a string, tied to the paw that was just drawn. Anywhere else and it hangs
    // in the air while his hand moves away from it.
    if (holding != null && heldPaw != null) {
        val r = s * 0.21f
        val centre = Offset(heldPaw.x + r * 1.1f, heldPaw.y - r * 4.0f)
        drawPath(
            Path().apply {
                moveTo(heldPaw.x, heldPaw.y)
                quadraticTo(
                    (heldPaw.x + centre.x) / 2f - r * 0.25f, (heldPaw.y + centre.y) / 2f,
                    centre.x, centre.y + r * 1.10f,
                )
            },
            Color(0xFF6B6560),
            style = Stroke(width = r * 0.07f, cap = StrokeCap.Round),
        )
        val body = Path().apply {
            moveTo(centre.x, centre.y + r * 1.10f)
            cubicTo(centre.x - r * 0.95f, centre.y + r * 0.45f, centre.x - r * 1.02f, centre.y - r * 0.95f, centre.x, centre.y - r * 1.05f)
            cubicTo(centre.x + r * 1.02f, centre.y - r * 0.95f, centre.x + r * 0.95f, centre.y + r * 0.45f, centre.x, centre.y + r * 1.10f)
            close()
        }
        drawPath(body, holding)
        drawPath(body, Mouse.Detail, style = Stroke(width = r * 0.09f))
        drawOval(
            color = Color.White.copy(alpha = 0.42f),
            topLeft = Offset(centre.x - r * 0.62f, centre.y - r * 0.72f),
            size = Size(r * 0.40f, r * 0.58f),
        )
    }

    // The face: solid navy, no whites, no pupils, no whiskers, no brows. The illustration has
    // none of those, and every one of them was a place the two styles disagreed.
    val eyeR = s * 0.025f
    val eyeY = headY - headR * 0.04f
    for (side in listOf(-1f, 1f)) {
        val ex = cx + side * headR * 0.40f
        if (lidClose > 0.5f) {
            // Shut: a happy upward arc, never a flat line.
            drawPath(
                Path().apply {
                    moveTo(ex - eyeR * 1.4f, eyeY + eyeR * 0.30f)
                    quadraticTo(ex, eyeY - eyeR * 0.90f, ex + eyeR * 1.4f, eyeY + eyeR * 0.30f)
                },
                Mouse.Detail,
                style = Stroke(width = t * 1.3f, cap = StrokeCap.Round),
            )
        } else {
            oval(
                Mouse.Detail,
                ex + lookX * eyeR * 0.30f, eyeY + lookY * eyeR * 0.30f,
                eyeR * 0.86f, eyeR,
            )
        }
    }

    // Nose: a small rounded triangle, navy like the outline.
    val noseY = eyeY + headR * 0.38f
    drawPath(
        Path().apply {
            moveTo(cx - headR * 0.11f, noseY - headR * 0.04f)
            lineTo(cx + headR * 0.11f, noseY - headR * 0.04f)
            lineTo(cx, noseY + headR * 0.10f)
            close()
        },
        Mouse.Detail,
    )

    val mouthY = noseY + headR * 0.12f
    if (mouthOpen > 0.01f) {
        val open = headR * 0.20f * mouthOpen
        oval(Mouse.Detail, cx, mouthY + open * 0.35f, headR * 0.14f, open)
        oval(Color(0xFFE58B93), cx, mouthY + open * 0.55f, headR * 0.09f, open * 0.62f)
    } else {
        // Two small curves under the nose, the way the illustration draws it.
        val lift = (smile + extraSmile).coerceIn(-0.4f, 1.2f)
        for (side in listOf(-1f, 1f)) {
            val mx = cx + side * headR * 0.15f
            drawPath(
                Path().apply {
                    moveTo(mx - side * headR * 0.15f, mouthY - headR * 0.02f)
                    quadraticTo(
                        mx, mouthY + lift * headR * 0.16f,
                        mx + side * headR * 0.15f, mouthY - headR * 0.02f,
                    )
                },
                Mouse.Detail,
                style = Stroke(width = t * 1.1f, cap = StrokeCap.Round),
            )
        }
    }
}


/**
 * §7.1. The one handle games are given: `react(state)` and nothing else, so every game's
 * reaction to a good thing is identical because it is literally the same code.
 *
 * Deliberate reversal of the spec, following hashvir: the spec asks for all animation to stop
 * after 60s idle, but hashvir judged a living character worth more than the battery, and he is
 * the same character. He keeps breathing, blinking and swaying until he falls asleep at 90s.
 */
class PuypuyController(private val scope: CoroutineScope) {

    var state by mutableStateOf(PuypuyState.Idle)
        private set

    /** True while a speech clip is playing, so his mouth moves with it. */
    var speaking by mutableStateOf(false)
        internal set

    private var returnToIdle: Job? = null

    fun react(next: PuypuyState) {
        returnToIdle?.cancel()
        state = next
        val hold = when (next) {
            PuypuyState.Happy -> 1000L
            PuypuyState.Surprised -> 500L
            PuypuyState.Waving -> 1600L
            PuypuyState.Sad -> 900L
            else -> 0L
        }
        if (hold > 0L) {
            returnToIdle = scope.launch {
                delay(hold)
                state = PuypuyState.Idle
            }
        }
    }
}

@Composable
fun rememberPuypuy(): PuypuyController {
    val scope = rememberCoroutineScope()
    return remember(scope) { PuypuyController(scope) }
}

/** What games place on screen. They never touch [PuypuySprite] or the state directly. */
@Composable
fun Puypuy(
    controller: PuypuyController,
    size: Dp,
    modifier: Modifier = Modifier,
    plump: Float = 0f,
    /** A balloon tied to his paw, in this colour. It follows the paw through every gesture. */
    holding: Color? = null,
) {
    PuypuySprite(
        state = controller.state,
        size = size,
        modifier = modifier,
        speaking = controller.speaking,
        plump = plump,
        holding = holding,
    )
}

/** The three little navy lines the illustration draws on every paw and foot. */
private fun DrawScope.toes(cx: Float, cy: Float, spread: Float, t: Float) {
    for (i in -1..1) {
        val x = cx + i * spread * 0.55f
        drawLine(
            Mouse.Detail,
            Offset(x, cy),
            Offset(x, cy + spread * 0.62f),
            strokeWidth = t * 0.7f,
            cap = StrokeCap.Round,
        )
    }
}
