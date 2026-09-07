package am.puypuy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import am.puypuy.core.Island
import am.puypuy.core.Mouse
import kotlin.math.sin
import kotlin.random.Random

/**
 * Պույ-պույ's island, drawn behind everything.
 *
 * From the tale: she finds a coconut, climbs inside, eats until she is too round to get back
 * out, and cries himself thin enough to escape. The whole app lives on that beach.
 *
 * All of it is muted and motionless. Scenery is never the subject — the fruit and the numerals
 * stay the most saturated things on screen.
 */
@Composable
fun Scenery(
    seed: Int,
    modifier: Modifier = Modifier,
    strength: Float = 1f,
    palms: Boolean = true,
    /** Off where a game draws its own — Փուչիկներ's clouds can be poked, so it owns them. */
    clouds: Boolean = true,
) {
    val puffs = remember(seed) {
        val random = Random(seed)
        // Kept inside 0.10..0.72 of the width so no cloud is ever sliced by an edge.
        List(3) { i ->
            Triple(
                0.10f + i * 0.28f + random.nextFloat() * 0.10f,
                0.08f + random.nextFloat() * 0.12f,
                0.8f + random.nextFloat() * 0.4f,
            )
        }
    }
    val shells = remember(seed) {
        val random = Random(seed + 7)
        List(9) { Offset(random.nextFloat(), 0.62f + random.nextFloat() * 0.34f) to random.nextFloat() }
    }

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        fun a(value: Float) = value * strength

        // The horizon. Everything above it is sky and sea, everything below is her beach.
        // A soft, low horizon. A hard-edged band across the middle reads as a
        // green stripe that the fruit and the coconuts sat on top of.
        val horizon = h * Horizon
        val shore = h * Shore

        // Soft at the top as well as the bottom. A hard horizontal line across the whole
        // screen is the single most artificial thing a flat drawing can do.
        val water = Path().apply {
            moveTo(0f, horizon + h * 0.012f)
            var x = 0f
            var up = true
            while (x < w) {
                val next = x + w / 5f
                quadraticTo(
                    (x + next) / 2f,
                    horizon + if (up) -h * 0.008f else h * 0.010f,
                    next,
                    horizon + h * 0.012f,
                )
                up = !up
                x = next
            }
            lineTo(w, shore)
            lineTo(0f, shore)
            close()
        }
        drawPath(water, Island.Sea.copy(alpha = a(0.20f)))

        // The sun sits fully on screen: half a sun in the corner
        // which just looked like a mistake.
        val sun = Offset(w * 0.80f, h * 0.13f)
        drawCircle(Island.Sun.copy(alpha = a(0.16f)), radius = w * 0.17f, center = sun)
        drawCircle(Island.Sun.copy(alpha = a(0.34f)), radius = w * 0.10f, center = sun)

        for ((x, y, scale) in if (clouds) puffs else emptyList()) {
            val cx = x * w
            val cy = y * h
            val r = w * 0.06f * scale
            for ((dx, dy, rs) in listOf(Triple(-0.9f, 0.18f, 0.72f), Triple(0f, 0f, 1f), Triple(0.95f, 0.22f, 0.78f))) {
                drawCircle(
                    color = Color.White.copy(alpha = a(0.55f)),
                    radius = r * rs,
                    center = Offset(cx + dx * r, cy + dy * r),
                )
            }
        }

        // Wave marks, only in the water.
        for (row in 0 until 3) {
            val y = horizon + (shore - horizon) * (0.25f + row * 0.26f)
            var x = -w * 0.05f + row * w * 0.06f
            while (x < w) {
                val wave = Path().apply {
                    moveTo(x, y)
                    quadraticTo(x + w * 0.03f, y - h * 0.008f, x + w * 0.06f, y)
                }
                drawPath(wave, Island.SeaDeep.copy(alpha = a(0.26f)), style = Stroke(width = h * 0.003f))
                x += w * 0.13f
            }
        }

        // The sand, meeting the water on a soft wavy line rather than a ruled edge.
        val sand = Path().apply {
            moveTo(0f, shore)
            var x = 0f
            var up = true
            while (x < w) {
                val next = x + w / 7f
                quadraticTo(
                    (x + next) / 2f,
                    shore + if (up) -h * 0.018f else h * 0.014f,
                    next,
                    shore,
                )
                up = !up
                x = next
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(sand, Island.SandDark.copy(alpha = a(0.34f)))

        // Palms stand ON the sand, well inside the edges. A frond reaches about half the
        // trunk's height sideways, so anything closer to the edge than that gets sliced.
        if (palms) {
            palm(Offset(w * 0.20f, shore + h * 0.15f), h * 0.18f, lean = -1f, strength = strength)
            palm(Offset(w * 0.80f, shore + h * 0.12f), h * 0.16f, lean = 1f, strength = strength)
        }

        for ((point, shade) in shells) {
            drawOval(
                color = Island.SandDark.copy(alpha = a(0.26f + shade * 0.14f)),
                topLeft = Offset(point.x * w, point.y * h),
                size = Size(w * (0.014f + shade * 0.016f), w * (0.008f + shade * 0.009f)),
            )
        }
    }
}

/** One palm: a curved trunk with fronds fanning off the top, and a coconut or two. */
internal fun DrawScope.palm(base: Offset, height: Float, lean: Float, strength: Float) {
    val topX = base.x + lean * height * 0.26f
    val topY = base.y - height

    val trunk = Path().apply {
        moveTo(base.x - height * 0.045f, base.y)
        cubicTo(
            base.x + lean * height * 0.02f, base.y - height * 0.5f,
            topX - lean * height * 0.06f, topY + height * 0.3f,
            topX, topY,
        )
        lineTo(topX + lean * height * 0.05f, topY + height * 0.03f)
        cubicTo(
            topX + height * 0.02f * lean, topY + height * 0.32f,
            base.x + lean * height * 0.06f, base.y - height * 0.5f,
            base.x + height * 0.045f, base.y,
        )
        close()
    }
    drawPath(trunk, Island.Trunk.copy(alpha = 0.30f * strength))

    for (angle in listOf(-115f, -70f, -25f, 20f, 65f)) {
        rotate(degrees = angle * 1f, pivot = Offset(topX, topY)) {
            val frond = Path().apply {
                moveTo(topX, topY)
                quadraticTo(
                    topX + height * 0.28f, topY - height * 0.16f,
                    topX + height * 0.52f, topY - height * 0.04f,
                )
                quadraticTo(
                    topX + height * 0.28f, topY + height * 0.06f,
                    topX, topY,
                )
                close()
            }
            drawPath(frond, Island.PalmLeaf.copy(alpha = 0.34f * strength))
            drawLine(
                color = Island.PalmDark.copy(alpha = 0.26f * strength),
                start = Offset(topX, topY),
                end = Offset(topX + height * 0.50f, topY - height * 0.05f),
                strokeWidth = height * 0.016f,
                cap = StrokeCap.Round,
            )
        }
    }

    drawCircle(Island.CoconutShell.copy(alpha = 0.36f * strength), radius = height * 0.055f, center = Offset(topX - height * 0.05f, topY + height * 0.07f))
    drawCircle(Island.CoconutShell.copy(alpha = 0.30f * strength), radius = height * 0.048f, center = Offset(topX + height * 0.06f, topY + height * 0.09f))
}

/** The coconut of the story: a hairy brown shell with the three dark eyes it really has. */
fun DrawScope.drawCoconut(diameter: Float, topLeft: Offset = Offset.Zero) {
    val r = diameter / 2f
    translate(topLeft.x, topLeft.y) {
        drawCircle(Island.CoconutShell, radius = r, center = Offset(r, r))
        drawCircle(Island.CoconutDark, radius = r, center = Offset(r, r), style = Stroke(width = diameter * 0.05f))
        // Fibre, in short strokes following the curve.
        for (index in 0 until 7) {
            val t = index / 6f
            val x = r * 0.45f + t * r * 1.1f
            drawLine(
                color = Island.CoconutDark.copy(alpha = 0.35f),
                start = Offset(x, r * 0.5f + sin(t * 3f) * r * 0.18f),
                end = Offset(x - diameter * 0.03f, r * 1.5f + sin(t * 3f) * r * 0.14f),
                strokeWidth = diameter * 0.022f,
                cap = StrokeCap.Round,
            )
        }
        // The three eyes.
        for ((dx, dy) in listOf(-0.28f to -0.16f, 0.02f to -0.30f, 0.24f to -0.10f)) {
            drawCircle(Island.CoconutDark, radius = r * 0.11f, center = Offset(r + dx * r, r + dy * r))
        }
        // A small, tight catch-light: anything wider reads as a smudge, not as shine.
        drawOval(
            color = Color.White.copy(alpha = 0.16f),
            topLeft = Offset(r * 0.48f, r * 0.40f),
            size = Size(r * 0.34f, r * 0.22f),
        )
    }
}

/**
 * A sky to rise into: lagoon above, sand along the bottom, palms at the two corners.
 *
 * [Scenery] paints no sky at all — it lays a translucent sea band across the middle of whatever
 * the window background happens to be, which is sand. That is right for a game played ON the
 * beach and wrong for one played above it, where it leaves balloons climbing through cream.
 *
 * [horizon] is where the sand starts, as a fraction of the height. Keep it low: the sky is the
 * playing field.
 */
@Composable
fun Sky(modifier: Modifier = Modifier, horizon: Float = 0.82f, palms: Boolean = true) {
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(SkyHigh)
        drawRect(
            brush = Brush.verticalGradient(
                0f to SkyHigh,
                1f to SkyLow,
                startY = 0f,
                endY = h * horizon,
            ),
            size = Size(w, h * horizon),
        )
        drawCircle(Island.Sun.copy(alpha = 0.22f), radius = w * 0.16f, center = Offset(w * 0.82f, h * 0.10f))
        drawCircle(Island.Sun.copy(alpha = 0.30f), radius = w * 0.09f, center = Offset(w * 0.82f, h * 0.10f))

        val shore = h * horizon
        drawPath(
            Path().apply {
                moveTo(0f, shore + h * 0.010f)
                cubicTo(
                    w * 0.35f, shore - h * 0.014f,
                    w * 0.65f, shore + h * 0.016f,
                    w, shore - h * 0.008f,
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            },
            SandLight,
        )
        if (palms) {
            val tall = h * 0.20f
            palm(Offset(-w * 0.05f, h * 1.02f), tall, lean = -0.5f, strength = 0.8f)
            palm(Offset(w * 1.05f, h * 1.04f), tall * 0.85f, lean = 0.55f, strength = 0.7f)
        }
    }
}

/** Where [Scenery]'s water starts and ends, so a game can tell a touch on the sea from sand. */
const val Horizon = 0.34f
const val Shore = 0.46f

private val SkyHigh = Color(0xFFBFE6EE)
private val SkyLow = Color(0xFFE6F5F3)
private val SandLight = Color(0xFFF7F0DE)

/**
 * Him curled up inside a coconut — the image the whole tale turns on.
 *
 * Shared, because two games show it: Պահմտոցի when a coconut opens on him instead of an
 * animal, and Կերակրի՛ր for the half of the tale where he is stuck in one. [crying] squeezes
 * his eyes shut and opens his mouth.
 */
fun DrawScope.drawCurled(centre: Offset, s: Float, crying: Boolean = false) {
    val fur = Mouse.Body
    val ink = Mouse.Detail
    drawArc(
        color = Color(0xFFA49DB5),
        startAngle = 20f, sweepAngle = 260f, useCenter = false,
        topLeft = Offset(centre.x - s * 0.52f, centre.y - s * 0.42f),
        size = Size(s * 1.04f, s * 0.94f),
        style = Stroke(width = s * 0.07f, cap = StrokeCap.Round),
    )
    drawOval(fur, Offset(centre.x - s * 0.40f, centre.y - s * 0.30f), Size(s * 0.80f, s * 0.62f))
    for (side in listOf(-1f, 1f)) {
        val ex = centre.x + side * s * 0.26f
        drawCircle(fur, s * 0.17f, Offset(ex, centre.y - s * 0.30f))
        drawCircle(Mouse.Ear, s * 0.10f, Offset(ex, centre.y - s * 0.29f))
    }
    // Asleep and crying are the same squeezed-shut arc; only the mouth tells them apart.
    for (side in listOf(-1f, 1f)) {
        val ex = centre.x + side * s * 0.15f
        drawPath(
            Path().apply {
                moveTo(ex - s * 0.08f, centre.y - s * 0.02f)
                quadraticTo(ex, centre.y - s * 0.13f, ex + s * 0.08f, centre.y - s * 0.02f)
            },
            ink, style = Stroke(width = s * 0.03f, cap = StrokeCap.Round),
        )
    }
    if (crying) {
        drawOval(ink, Offset(centre.x - s * 0.07f, centre.y + s * 0.04f), Size(s * 0.14f, s * 0.13f))
    } else {
        drawCircle(Color(0xFFE58A9A), s * 0.055f, Offset(centre.x, centre.y + s * 0.10f))
    }
}
