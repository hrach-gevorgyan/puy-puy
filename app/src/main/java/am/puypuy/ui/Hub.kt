package am.puypuy.ui

import am.puypuy.core.Armenian
import am.puypuy.core.Audio
import am.puypuy.core.FrameLoop
import am.puypuy.core.Haptics
import am.puypuy.core.Ink
import am.puypuy.core.Island
import am.puypuy.core.Juice
import am.puypuy.core.Layout
import am.puypuy.core.MinTouch
import am.puypuy.core.MiniGame
import am.puypuy.core.Puypuy
import am.puypuy.core.PuypuyController
import am.puypuy.core.PuypuyState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import kotlin.random.Random
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * The hub: six games, and him on the beach below them.
 *
 * Three channels on every open, the same rule the games follow (docs/games.md): the tile
 * answers the finger, he reacts, and he names the game. Silent is fine — the first two carry
 * it alone.
 *
 * Tiles answer on touch-DOWN. Children tap again and then leave when the first tap produces
 * no answer (docs/toddler-ux.md), and a 300ms crossfade is well past the point where a
 * three-year-old has decided the screen is broken.
 */
@Composable
fun Hub(
    games: List<MiniGame>,
    puypuy: PuypuyController,
    audio: Audio,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val grid = Layout.hub(maxWidth, maxHeight, games.size)
        val view = LocalView.current
        val haptics = remember(view) { Haptics(view) }

        // The ground the tile artwork is drawn on, continued across the whole screen: the six
        // illustrations share one lagoon and one beach, so the grid reads as six windows onto
        // a single island rather than six stickers.
        Backdrop(Modifier.fillMaxSize())

        val band = grid.character * 0.62f
        val needsScroll = grid.heightNeeded() > maxHeight - band
        Column(
            Modifier
                .fillMaxSize()
                .then(if (needsScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(bottom = band),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (r in 0 until grid.rows) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = grid.gap / 2),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (c in 0 until grid.columns) {
                        val i = r * grid.columns + c
                        Box(Modifier.padding(horizontal = grid.gap / 2)) {
                            if (i < games.size) {
                                Tile(
                                    image = games[i].tileImage,
                                    label = stringResource(games[i].tileLabel),
                                    size = grid.tile,
                                    onPress = {
                                        haptics.tap()
                                        puypuy.react(PuypuyState.Suggesting)
                                    },
                                    onRelease = { opened ->
                                        if (!opened) {
                                            puypuy.react(PuypuyState.Idle)
                                        } else {
                                            haptics.confirm()
                                            puypuy.react(PuypuyState.Happy)
                                            // Spoken as the game opens, never before it:
                                            // nothing is ever held up waiting for him.
                                            audio.say(games[i].voiceKey)
                                            onOpen(i)
                                        }
                                    },
                                )
                            } else {
                                Box(Modifier.size(grid.tile))
                            }
                        }
                    }
                }
            }
        }

        // The beach: him chasing his coconut up and down it.
        Beach(
            puypuy = puypuy,
            haptics = haptics,
            size = grid.character,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(grid.character),
        )
    }
}

/**
 * The beach along the bottom of the hub: him chasing his coconut, the way he does in hashvir.
 *
 * Nothing here is driven. The coconut is THROWN and then left alone — it rolls, the sand slows
 * it, it bounces off the ends and stops. He accelerates after it and coasts, so he leans into
 * a turn and overshoots, and when he reaches one at rest he bumps it off again. The chase
 * never resolves, never repeats exactly, and neither of them ever leaves the screen: a host
 * who walks off the side of his own hub is not a host.
 *
 * The pauses come out of the physics rather than a timer.
 */
@Composable
private fun Beach(
    puypuy: PuypuyController,
    haptics: Haptics,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val width = with(density) { maxWidth.toPx() }
        val nut = with(density) { (size * 0.52f).toPx() }
        val heW = with(density) { size.toPx() }
        val speed = with(density) { (size * 0.55f).toPx() }   // the coconut, per second

        var nutX by remember(width) { mutableFloatStateOf(width * 0.62f) }
        var nutV by remember(width) { mutableFloatStateOf(0f) }
        var heX by remember(width) { mutableFloatStateOf(width * 0.06f) }
        var heV by remember(width) { mutableFloatStateOf(0f) }
        var facing by remember { mutableFloatStateOf(1f) }
        val hearts = remember { mutableStateListOf<Heart>() }
        val heightPx = with(density) { maxHeight.toPx() }

        val leftWall = 0f
        val rightWall = (width - nut).coerceAtLeast(1f)
        // A nudge roughly every four or five seconds, with a real journey in between.
        val kick = heW * 3.0f          // how hard a nudge sends it
        val drag = heW * 0.30f         // how slowly the sand takes that away again
        val push = heW * 3.2f          // how hard he can accelerate
        val top = heW * 0.90f          // and how fast he can end up going

        FrameLoop(particles = null, shake = null) { dt ->
            // Everything holds still while he is doing something else — waving back at a
            // touch, pointing at a pressed tile, asleep. The chase never competes with the
            // thing the child actually did.
            // Hearts rise whatever else is happening: they are the answer to a touch, and an
            // answer that waits for the chase to finish is not an answer.
            if (hearts.isNotEmpty()) {
                val gone = mutableListOf<Heart>()
                for (h in hearts) {
                    h.life += dt
                    if (h.life > HEART_LIFE + h.delay) gone += h
                }
                hearts.removeAll(gone)
            }

            val free = puypuy.state == PuypuyState.Walking || puypuy.state == PuypuyState.Idle
            if (!free) return@FrameLoop

            // Thrown, then left alone: it slows on the sand, bounces off the ends and stops.
            if (nutV != 0f) {
                nutX += nutV * dt
                val slow = drag * dt
                nutV = if (kotlin.math.abs(nutV) <= slow) 0f else nutV - kotlin.math.sign(nutV) * slow
                if (nutX <= leftWall) { nutX = leftWall; nutV = -nutV * 0.55f }
                if (nutX >= rightWall) { nutX = rightWall; nutV = -nutV * 0.55f }
            }

            // He accelerates toward it and coasts to a stop, rather than snapping to a speed.
            val gap = nutX - heX
            val want = if (kotlin.math.abs(gap) < heW * 0.30f) 0f else kotlin.math.sign(gap) * top
            heV += (want - heV).coerceIn(-push * dt, push * dt)
            heX = (heX + heV * dt).coerceIn(-heW * 0.10f, width - heW * 0.90f)

            // He faces the way he is actually moving, and keeps facing it while he coasts.
            if (kotlin.math.abs(heV) > top * 0.08f) facing = kotlin.math.sign(heV)

            val moving = kotlin.math.abs(heV) > top * 0.06f
            if (moving && puypuy.state != PuypuyState.Walking) puypuy.react(PuypuyState.Walking)
            if (!moving && puypuy.state == PuypuyState.Walking) puypuy.react(PuypuyState.Idle)

            // Caught up with a coconut that has stopped: he bumps it and off it goes again,
            // away from him. That is the whole loop, and it never resolves.
            if (nutV == 0f && kotlin.math.abs(gap) < heW * 0.42f) {
                val away = if (nutX > heX) 1f else -1f
                nutV = away * kick * (0.75f + kotlin.random.Random.nextFloat() * 0.5f)
            }
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(width) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        haptics.tap()
                        if (down.position.x > nutX - nut * 0.4f && down.position.x < nutX + nut * 1.4f) {
                            // She has given it a shove: it bolts away from wherever he is.
                            nutV = (if (nutX > heX) 1f else -1f) * kick * 1.5f
                            puypuy.react(PuypuyState.Happy)
                        } else {
                            // He waves back and says nothing — a character who announces
                            // himself on every poke is a character she mutes — but he is
                            // plainly pleased about it, and hearts say that without a word.
                            puypuy.react(PuypuyState.Waving)
                            repeat(4) { i ->
                                hearts += Heart(
                                    x = heX + heW * (0.35f + Random.nextFloat() * 0.3f),
                                    y = heightPx - heW * (0.55f + Random.nextFloat() * 0.2f),
                                    size = heW * (0.10f + Random.nextFloat() * 0.06f),
                                    drift = (Random.nextFloat() - 0.5f) * heW * 0.5f,
                                    delay = i * 0.09f,
                                )
                            }
                        }
                    }
                },
        ) {
            // Shadows, so neither of them floats above the sand.
            drawOval(
                Island.SandDark.copy(alpha = 0.20f),
                topLeft = Offset(heX + heW * 0.18f, this.size.height * 0.82f),
                size = Size(heW * 0.58f, this.size.height * 0.13f),
            )
            drawOval(
                Island.SandDark.copy(alpha = 0.20f),
                topLeft = Offset(nutX + nut * 0.06f, this.size.height * 0.84f),
                size = Size(nut * 1.05f, this.size.height * 0.11f),
            )
            translate(nutX, this.size.height - nut * 1.12f) {
                // Rolled, not skidded: one turn per circumference travelled, and it spins
                // backwards when it comes back the other way.
                rotate(2f * nutX / nut * 57.2958f, pivot = Offset(nut / 2f, nut / 2f)) {
                    drawCoconut(nut)
                }
            }
        }

        Box(
            Modifier
                .offset { IntOffset(heX.toInt(), 0) }
                // He turns to face the way he is running. Without this he chases it backwards
                // half the time, which is the single most obviously wrong thing a walk can do.
                .scale(scaleX = facing, scaleY = 1f),
        ) {
            Puypuy(controller = puypuy, size = size)
        }
    }
}

/**
 * One game: a picture, its Armenian name, and a single square target.
 *
 * The whole card is the target — 126dp at the floor and usually much larger — because a
 * three-year-old aims at the picture, not at a hit box drawn around part of it.
 */
@Composable
private fun Tile(
    image: Int,
    label: String,
    size: Dp,
    onPress: () -> Unit,
    onRelease: (opened: Boolean) -> Unit,
) {
    val press = remember { Animatable(1f) }
    val coroutines = rememberCoroutineScope()

    val card = size * 0.80f
    Column(
        Modifier
            .size(size.coerceAtLeast(MinTouch))
            .scale(press.value)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    // Down. Not up, not once a gesture has been disambiguated: now.
                    coroutines.launch { press.animateTo(0.90f, Juice.press()) }
                    onPress()
                    val up = waitForUpOrCancellation()
                    coroutines.launch { press.animateTo(1f, Juice.bouncy()) }
                    onRelease(up != null)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The illustration IS the card, edge to edge. Nothing is laid over it.
        Image(
            painter = painterResource(image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(card)
                // The art shares the hub's own ground, so the shadow is what makes a card
                // read as an object rather than a patch of background.
                .shadow(5.dp, RoundedCornerShape(card * 0.20f))
                .clip(RoundedCornerShape(card * 0.20f))
                .background(Color.White),
        )
        // The name sits UNDER the card, on the sand — for the adult, since she navigates by
        // the picture, and so it can never cover any part of the picture.
        BasicText(
            text = label,
            maxLines = 1,
            style = TextStyle(
                fontFamily = Armenian,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.105f).sp,
                // Armenian ascenders overflow the default line box.
                lineHeight = (size.value * 0.155f).sp,
                color = Ink.Primary,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.padding(top = size * 0.025f),
        )
    }
}

/** Lagoon over sand, in the artwork's own two colours, with palms rooted on the shore. */
@Composable
private fun Backdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(LAGOON)
        val shore = size.height * 0.60f
        drawPath(
            Path().apply {
                moveTo(0f, shore + size.height * 0.012f)
                cubicTo(
                    size.width * 0.35f, shore - size.height * 0.018f,
                    size.width * 0.65f, shore + size.height * 0.020f,
                    size.width, shore - size.height * 0.010f,
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            },
            SAND,
        )
        // One soft sun, well out of the way of the grid.
        drawCircle(
            Color(0xFFFFD166).copy(alpha = 0.13f),
            radius = size.width * 0.11f,
            center = Offset(size.width * 0.86f, size.height * 0.09f),
        )
        // Rooted at the bottom and leaning OFF the screen, so they frame the beach and never
        // reach up behind the tiles: scenery that competes with the toy is a bug.
        val tall = size.height * 0.22f
        palm(Offset(-size.width * 0.04f, size.height * 1.00f), tall, lean = -0.50f, strength = 0.85f)
        palm(Offset(size.width * 0.30f, size.height * 0.99f), tall * 0.72f, lean = -0.35f, strength = 0.45f)
        palm(Offset(size.width * 1.04f, size.height * 1.02f), tall * 0.88f, lean = 0.55f, strength = 0.75f)

        // His home, faint on purpose: a door she can see but never open is a promise the app
        // cannot keep, so it is drawn as scenery.
        home(Offset(size.width * 0.72f, size.height * 0.965f), size.height * 0.085f)
    }
}

/** A sandy mound with a round door: the burrow he scampers home to at the end of the tale. */
private fun DrawScope.home(base: Offset, height: Float, alpha: Float = 0.42f) {
    val w = height * 1.5f
    drawPath(
        Path().apply {
            moveTo(base.x - w / 2f, base.y)
            cubicTo(
                base.x - w * 0.44f, base.y - height * 1.30f,
                base.x + w * 0.44f, base.y - height * 1.30f,
                base.x + w / 2f, base.y,
            )
            close()
        },
        Island.SandDark.copy(alpha = alpha * 0.85f),
    )
    // The doorway: an arch, dark inside, with a lintel of small stones.
    val doorW = height * 0.46f
    val doorH = height * 0.62f
    drawPath(
        Path().apply {
            moveTo(base.x - doorW / 2f, base.y)
            lineTo(base.x - doorW / 2f, base.y - doorH * 0.55f)
            quadraticTo(base.x, base.y - doorH * 1.25f, base.x + doorW / 2f, base.y - doorH * 0.55f)
            lineTo(base.x + doorW / 2f, base.y)
            close()
        },
        Island.CoconutDark.copy(alpha = alpha),
    )
    // Two tufts of grass, so the mound is a home rather than a heap.
    for (side in listOf(-1f, 1f)) {
        for (blade in -1..1) {
            drawLine(
                Island.PalmLeaf.copy(alpha = alpha * 0.75f),
                start = Offset(base.x + side * w * 0.40f + blade * height * 0.05f, base.y),
                end = Offset(
                    base.x + side * w * 0.40f + blade * height * 0.11f,
                    base.y - height * (0.20f + 0.06f * blade),
                ),
                strokeWidth = height * 0.035f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private val LAGOON = Color(0xFFDCF1F3)
private val SAND = Color(0xFFF7F0DE)

/** One heart on its way up, after she has said hello to him. */
private class Heart(
    val x: Float,
    val y: Float,
    val size: Float,
    val drift: Float,
    val delay: Float,
) {
    var life: Float = 0f
}

/** How long a heart takes to rise and fade, in seconds. */
private const val HEART_LIFE = 1.5f

/** Two lobes and a point, in the app's own pink with the navy outline. */
private fun DrawScope.drawHeart(centre: Offset, size: Float, alpha: Float) {
    val path = Path().apply {
        moveTo(centre.x, centre.y + size * 0.75f)
        cubicTo(
            centre.x - size * 1.30f, centre.y - size * 0.10f,
            centre.x - size * 0.50f, centre.y - size * 1.05f,
            centre.x, centre.y - size * 0.35f,
        )
        cubicTo(
            centre.x + size * 0.50f, centre.y - size * 1.05f,
            centre.x + size * 1.30f, centre.y - size * 0.10f,
            centre.x, centre.y + size * 0.75f,
        )
        close()
    }
    drawPath(path, Color(0xFFE86A7C).copy(alpha = alpha))
    drawPath(
        path,
        Ink.Primary.copy(alpha = alpha * 0.7f),
        style = Stroke(width = size * 0.14f),
    )
}
