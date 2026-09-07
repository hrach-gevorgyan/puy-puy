package am.puypuy.games

import am.puypuy.core.Mouse
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * The twelve animals, drawn to the same rules as the fruit.
 *
 * Each has to be nameable by a three-year-old from the picture alone — that is the entire
 * job, since the coconut opens on an animal and nothing else says which one it is.
 *
 * The rules, taken from `FruitArt` so both read as one world:
 *
 *  1. **Silhouette first.** Each animal is built from one recognisable body path, not from a
 *     circle with features stuck on. If the shape does not read at a glance in one flat
 *     colour, no amount of detail on top will save it.
 *  2. **Then the shared outline.** Saturated fills cannot hold an edge against pastel sand;
 *     the outline is what does, and it is what makes the set look like a set.
 *  3. **Then two or three details, and no more.** The defining feature of the animal — the
 *     shell's spiral, the crab's claws, the cat's ears — and the eyes. Everything else is
 *     noise at the size these are actually drawn.
 *  4. **Eyes are big, round, and forward.** This is the difference between an animal and a
 *     specimen. Every one of them is friendly.
 *
 * Everything is drawn in a 100 x 100 box and scaled by the caller, exactly like the fruit.
 */
enum class Animal(val voiceKey: String, val body: Color, val detail: Color) {
    CRAB("animal_crab", Color(0xFFE0483C), Color(0xFFB8342A)),
    FISH("animal_fish", Color(0xFFF5901E), Color(0xFFD1720C)),
    SEAGULL("animal_seagull", Color(0xFFF7F9FB), Color(0xFF9FB3C4)),
    TURTLE("animal_turtle", Color(0xFF4C9E52), Color(0xFF2F7A33)),
    STARFISH("animal_starfish", Color(0xFFF5A0B8), Color(0xFFD97C97)),
    OCTOPUS("animal_octopus", Color(0xFF8E5BC7), Color(0xFF6E42A0)),
    FROG("animal_frog", Color(0xFF6FBF3A), Color(0xFF4E9424)),
    BUTTERFLY("animal_butterfly", Color(0xFF3B7DD8), Color(0xFFF2B705)),
    SNAIL("animal_snail", Color(0xFFD9A25C), Color(0xFFA97B4F)),
    BEE("animal_bee", Color(0xFFF2B705), Color(0xFF3A2E12)),
    DUCK("animal_duck", Color(0xFFFFD54A), Color(0xFFF57C1F)),
    CAT("animal_cat", Color(0xFFA98467), Color(0xFF7D5F49));

    /** Effect clip name in `res/raw`. None of these are recorded yet; see docs/audio.md. */
    val sound: String get() = "sfx_${name.lowercase()}"

    /** Draws the animal centred on [centre], fitting a box of side [s]. */
    fun draw(scope: DrawScope, centre: Offset, s: Float) = with(scope) {
        val u = s / 100f
        fun p(x: Float, y: Float) = Offset(centre.x + (x - 50f) * u, centre.y + (y - 50f) * u)
        val line = Stroke(width = 3.2f * u, cap = StrokeCap.Round)
        val ink = Ink

        when (this@Animal) {
            CRAB -> {
                // Claws first, so the shell overlaps them and they read as attached.
                for (side in listOf(-1f, 1f)) {
                    val cx = 50f + side * 36f
                    outlined(::p, line, body, Path().apply {
                        moveTo(p(cx, 44f).x, p(cx, 44f).y)
                        cubicTo(p(cx - side * 4f, 30f).x, p(cx - side * 4f, 30f).y,
                            p(cx + side * 10f, 30f).x, p(cx + side * 10f, 30f).y,
                            p(cx + side * 9f, 42f).x, p(cx + side * 9f, 42f).y)
                        cubicTo(p(cx + side * 4f, 46f).x, p(cx + side * 4f, 46f).y,
                            p(cx + side * 3f, 50f).x, p(cx + side * 3f, 50f).y,
                            p(cx, 50f).x, p(cx, 50f).y)
                        close()
                    })
                    // Legs.
                    for (i in 0..2) {
                        drawLine(detail, p(50f + side * 22f, 58f + i * 5f), p(50f + side * 40f, 66f + i * 7f), 3.4f * u, StrokeCap.Round)
                    }
                }
                // Shell: wider than tall, flat-bottomed.
                outlined(::p, line, body, Path().apply {
                    moveTo(p(18f, 62f).x, p(18f, 62f).y)
                    cubicTo(p(16f, 40f).x, p(16f, 40f).y, p(84f, 40f).x, p(84f, 40f).y, p(82f, 62f).x, p(82f, 62f).y)
                    close()
                })
                eyeStalks(::p, u, ink)
                smile(::p, line, 50f, 56f, 10f, ink)
            }

            FISH -> {
                outlined(::p, line, body, Path().apply {
                    // Tail.
                    moveTo(p(78f, 50f).x, p(78f, 50f).y)
                    lineTo(p(94f, 34f).x, p(94f, 34f).y)
                    lineTo(p(94f, 66f).x, p(94f, 66f).y)
                    close()
                })
                outlined(::p, line, body, Path().apply {
                    moveTo(p(80f, 50f).x, p(80f, 50f).y)
                    cubicTo(p(64f, 22f).x, p(64f, 22f).y, p(18f, 26f).x, p(18f, 26f).y, p(10f, 50f).x, p(10f, 50f).y)
                    cubicTo(p(18f, 74f).x, p(18f, 74f).y, p(64f, 78f).x, p(64f, 78f).y, p(80f, 50f).x, p(80f, 50f).y)
                    close()
                })
                // Top fin and gill: the two marks that make a body a fish.
                outlined(::p, line, detail, Path().apply {
                    moveTo(p(44f, 27f).x, p(44f, 27f).y)
                    lineTo(p(52f, 12f).x, p(52f, 12f).y)
                    lineTo(p(60f, 30f).x, p(60f, 30f).y)
                    close()
                })
                drawPath(Path().apply {
                    moveTo(p(60f, 32f).x, p(60f, 32f).y)
                    quadraticTo(p(54f, 50f).x, p(54f, 50f).y, p(60f, 68f).x, p(60f, 68f).y)
                }, detail, style = line)
                eye(::p, u, 30f, 44f, 7f, ink)
            }

            SEAGULL -> {
                // A white bird on cream sand needs a grey wing and a firm outline or it is a
                // hole in the screen. That was the single worst drawing in the set.
                outlined(::p, line, body, Path().apply {
                    moveTo(p(22f, 60f).x, p(22f, 60f).y)
                    cubicTo(p(20f, 40f).x, p(20f, 40f).y, p(62f, 34f).x, p(62f, 34f).y, p(78f, 46f).x, p(78f, 46f).y)
                    cubicTo(p(86f, 52f).x, p(86f, 52f).y, p(70f, 72f).x, p(70f, 72f).y, p(42f, 70f).x, p(42f, 70f).y)
                    close()
                })
                outlined(::p, line, detail, Path().apply {
                    moveTo(p(38f, 50f).x, p(38f, 50f).y)
                    cubicTo(p(48f, 40f).x, p(48f, 40f).y, p(66f, 44f).x, p(66f, 44f).y, p(70f, 54f).x, p(70f, 54f).y)
                    cubicTo(p(58f, 58f).x, p(58f, 58f).y, p(46f, 56f).x, p(46f, 56f).y, p(38f, 50f).x, p(38f, 50f).y)
                    close()
                })
                outlined(::p, line, body, Path().apply {
                    moveTo(p(26f, 34f).x, p(26f, 34f).y)
                    cubicTo(p(10f, 34f).x, p(10f, 34f).y, p(10f, 56f).x, p(10f, 56f).y, p(26f, 56f).x, p(26f, 56f).y)
                    cubicTo(p(36f, 56f).x, p(36f, 56f).y, p(36f, 34f).x, p(36f, 34f).y, p(26f, 34f).x, p(26f, 34f).y)
                    close()
                })
                outlined(::p, line, Color(0xFFF5A623), Path().apply {
                    moveTo(p(14f, 42f).x, p(14f, 42f).y)
                    lineTo(p(2f, 47f).x, p(2f, 47f).y)
                    lineTo(p(14f, 52f).x, p(14f, 52f).y)
                    close()
                })
                eye(::p, u, 22f, 42f, 5.5f, ink)
            }

            TURTLE -> {
                // Head and flippers under the shell.
                outlined(::p, line, Color(0xFF9BD37A), Path().apply {
                    addOval(rect(::p, 68f, 44f, 96f, 68f))
                })
                for (dx in listOf(-1f, 1f)) {
                    outlined(::p, line, Color(0xFF9BD37A), Path().apply {
                        addOval(rect(::p, 50f + dx * 26f - 12f, 62f, 50f + dx * 26f + 12f, 78f))
                    })
                }
                outlined(::p, line, body, Path().apply {
                    addOval(rect(::p, 14f, 24f, 80f, 70f))
                })
                // Hexagonal plates: this is what makes it a shell rather than an oval.
                for ((cx, cy, r) in listOf(Triple(47f, 40f, 12f), Triple(30f, 54f, 9f), Triple(64f, 54f, 9f))) {
                    drawPath(hexagon(::p, cx, cy, r), detail, style = Stroke(width = 2.6f * u))
                }
                eye(::p, u, 84f, 52f, 5f, ink)
            }

            STARFISH -> {
                val star = Path()
                for (i in 0 until 10) {
                    val rr = if (i % 2 == 0) 46f else 19f
                    val a = (-90f + i * 36f) * Math.PI.toFloat() / 180f
                    val pt = p(50f + kotlin.math.cos(a) * rr, 50f + kotlin.math.sin(a) * rr)
                    if (i == 0) star.moveTo(pt.x, pt.y) else star.lineTo(pt.x, pt.y)
                }
                star.close()
                outlined(::p, line, body, star)
                for ((cx, cy) in listOf(38f to 40f, 62f to 40f, 30f to 62f, 70f to 62f, 50f to 70f)) {
                    drawCircle(detail, 3.2f * u, p(cx, cy))
                }
                eye(::p, u, 42f, 48f, 6f, ink)
                eye(::p, u, 58f, 48f, 6f, ink)
                smile(::p, line, 50f, 58f, 9f, ink)
            }

            OCTOPUS -> {
                // Eight legs, curled, drawn before the head.
                for (i in 0 until 8) {
                    val t = i / 7f
                    val x = 14f + t * 72f
                    val curl = if (i % 2 == 0) 1f else -1f
                    drawPath(Path().apply {
                        moveTo(p(x, 58f).x, p(x, 58f).y)
                        quadraticTo(p(x + curl * 6f, 78f).x, p(x + curl * 6f, 78f).y, p(x - curl * 4f, 90f).x, p(x - curl * 4f, 90f).y)
                    }, body, style = Stroke(width = 8f * u, cap = StrokeCap.Round))
                }
                outlined(::p, line, body, Path().apply {
                    moveTo(p(14f, 58f).x, p(14f, 58f).y)
                    cubicTo(p(10f, 16f).x, p(10f, 16f).y, p(90f, 16f).x, p(90f, 16f).y, p(86f, 58f).x, p(86f, 58f).y)
                    close()
                })
                eye(::p, u, 38f, 40f, 8f, ink)
                eye(::p, u, 62f, 40f, 8f, ink)
                smile(::p, line, 50f, 50f, 9f, ink)
            }

            FROG -> {
                for (side in listOf(-1f, 1f)) {
                    outlined(::p, line, body, Path().apply {
                        addOval(rect(::p, 50f + side * 30f - 14f, 58f, 50f + side * 30f + 14f, 80f))
                    })
                }
                outlined(::p, line, body, Path().apply {
                    moveTo(p(16f, 66f).x, p(16f, 66f).y)
                    cubicTo(p(14f, 34f).x, p(14f, 34f).y, p(86f, 34f).x, p(86f, 34f).y, p(84f, 66f).x, p(84f, 66f).y)
                    cubicTo(p(70f, 78f).x, p(70f, 78f).y, p(30f, 78f).x, p(30f, 78f).y, p(16f, 66f).x, p(16f, 66f).y)
                    close()
                })
                // The bulging eyes ON TOP of the head are the whole silhouette of a frog.
                for (side in listOf(-1f, 1f)) {
                    outlined(::p, line, body, Path().apply {
                        addOval(rect(::p, 50f + side * 18f - 14f, 18f, 50f + side * 18f + 14f, 46f))
                    })
                    eye(::p, u, 50f + side * 18f, 32f, 7f, ink)
                }
                smile(::p, line, 50f, 58f, 16f, ink)
            }

            BUTTERFLY -> {
                for (side in listOf(-1f, 1f)) {
                    outlined(::p, line, body, Path().apply {
                        addOval(rect(::p, 50f + side * 26f - 24f, 18f, 50f + side * 26f + 24f, 54f))
                    })
                    outlined(::p, line, body, Path().apply {
                        addOval(rect(::p, 50f + side * 22f - 19f, 50f, 50f + side * 22f + 19f, 80f))
                    })
                    drawCircle(detail, 6f * u, p(50f + side * 26f, 34f))
                    drawCircle(detail, 4.5f * u, p(50f + side * 22f, 63f))
                    // Antennae.
                    drawLine(ink, p(50f + side * 3f, 26f), p(50f + side * 16f, 8f), 2.6f * u, StrokeCap.Round)
                    drawCircle(ink, 3.4f * u, p(50f + side * 16f, 7f))
                }
                outlined(::p, line, Ink, Path().apply {
                    addOval(rect(::p, 45f, 26f, 55f, 78f))
                })
                drawCircle(Color.White, 2.4f * u, p(47.5f, 34f))
                drawCircle(Color.White, 2.4f * u, p(52.5f, 34f))
            }

            SNAIL -> {
                outlined(::p, line, Color(0xFFC7D64A), Path().apply {
                    moveTo(p(8f, 78f).x, p(8f, 78f).y)
                    cubicTo(p(4f, 62f).x, p(4f, 62f).y, p(26f, 58f).x, p(26f, 58f).y, p(34f, 62f).x, p(34f, 62f).y)
                    lineTo(p(86f, 62f).x, p(86f, 62f).y)
                    cubicTo(p(94f, 68f).x, p(94f, 68f).y, p(94f, 78f).x, p(94f, 78f).y, p(86f, 80f).x, p(86f, 80f).y)
                    lineTo(p(8f, 80f).x, p(8f, 80f).y)
                    close()
                })
                // Eye stalks.
                for (dx in listOf(0f, 9f)) {
                    drawLine(Color(0xFFC7D64A), p(14f + dx, 62f), p(10f + dx, 40f), 4f * u, StrokeCap.Round)
                    drawCircle(ink, 4.2f * u, p(10f + dx, 38f))
                }
                // The shell, and the spiral that makes it a snail.
                outlined(::p, line, body, Path().apply {
                    addOval(rect(::p, 36f, 20f, 88f, 68f))
                })
                val spiral = Path()
                var a = 0f
                var r = 22f
                spiral.moveTo(p(62f + r, 44f).x, p(62f + r, 44f).y)
                while (r > 2.5f) {
                    a += 0.35f
                    r -= 0.62f
                    spiral.lineTo(p(62f + kotlin.math.cos(a) * r, 44f + kotlin.math.sin(a) * r * 0.92f).x,
                        p(62f + kotlin.math.cos(a) * r, 44f + kotlin.math.sin(a) * r * 0.92f).y)
                }
                drawPath(spiral, detail, style = Stroke(width = 3f * u, cap = StrokeCap.Round))
            }

            BEE -> {
                // Two wings, above and behind.
                for (side in listOf(-1f, 1f)) {
                    outlined(::p, line, Color(0xCCFFFFFF), Path().apply {
                        addOval(rect(::p, 50f + side * 18f - 20f, 8f, 50f + side * 18f + 20f, 38f))
                    })
                }
                outlined(::p, line, body, Path().apply {
                    addOval(rect(::p, 16f, 32f, 82f, 76f))
                })
                // Stripes, clipped to the body by drawing them shorter at the ends.
                for ((x, h) in listOf(38f to 20f, 52f to 22f, 66f to 17f)) {
                    drawLine(detail, p(x, 54f - h), p(x, 54f + h), 7f * u)
                }
                // Sting.
                outlined(::p, line, detail, Path().apply {
                    moveTo(p(80f, 46f).x, p(80f, 46f).y)
                    lineTo(p(96f, 54f).x, p(96f, 54f).y)
                    lineTo(p(80f, 62f).x, p(80f, 62f).y)
                    close()
                })
                eye(::p, u, 26f, 48f, 7f, ink)
                smile(::p, line, 24f, 60f, 8f, ink)
            }

            DUCK -> {
                outlined(::p, line, body, Path().apply {
                    moveTo(p(14f, 62f).x, p(14f, 62f).y)
                    cubicTo(p(10f, 40f).x, p(10f, 40f).y, p(56f, 38f).x, p(56f, 38f).y, p(70f, 56f).x, p(70f, 56f).y)
                    cubicTo(p(78f, 68f).x, p(78f, 68f).y, p(50f, 82f).x, p(50f, 82f).y, p(24f, 76f).x, p(24f, 76f).y)
                    close()
                })
                // Head on a proper neck: a circle stuck to a body reads as a snowman.
                outlined(::p, line, body, Path().apply {
                    moveTo(p(56f, 52f).x, p(56f, 52f).y)
                    cubicTo(p(54f, 26f).x, p(54f, 26f).y, p(64f, 12f).x, p(64f, 12f).y, p(78f, 16f).x, p(78f, 16f).y)
                    cubicTo(p(92f, 20f).x, p(92f, 20f).y, p(90f, 44f).x, p(90f, 44f).y, p(74f, 50f).x, p(74f, 50f).y)
                    close()
                })
                outlined(::p, line, detail, Path().apply {
                    moveTo(p(88f, 26f).x, p(88f, 26f).y)
                    lineTo(p(99f, 31f).x, p(99f, 31f).y)
                    lineTo(p(88f, 36f).x, p(88f, 36f).y)
                    close()
                })
                // A wing, so the body is not a plain lozenge.
                drawPath(Path().apply {
                    moveTo(p(30f, 58f).x, p(30f, 58f).y)
                    quadraticTo(p(46f, 50f).x, p(46f, 50f).y, p(58f, 64f).x, p(58f, 64f).y)
                }, detail, style = line)
                eye(::p, u, 78f, 28f, 5.5f, ink)
            }

            CAT -> {
                for (side in listOf(-1f, 1f)) {
                    outlined(::p, line, body, Path().apply {
                        moveTo(p(50f + side * 12f, 34f).x, p(50f + side * 12f, 34f).y)
                        lineTo(p(50f + side * 30f, 8f).x, p(50f + side * 30f, 8f).y)
                        lineTo(p(50f + side * 34f, 40f).x, p(50f + side * 34f, 40f).y)
                        close()
                    })
                    drawPath(Path().apply {
                        moveTo(p(50f + side * 16f, 32f).x, p(50f + side * 16f, 32f).y)
                        lineTo(p(50f + side * 26f, 16f).x, p(50f + side * 26f, 16f).y)
                        lineTo(p(50f + side * 28f, 34f).x, p(50f + side * 28f, 34f).y)
                        close()
                    }, Color(0xFFF2C4C9))
                }
                outlined(::p, line, body, Path().apply {
                    addOval(rect(::p, 14f, 26f, 86f, 84f))
                })
                eye(::p, u, 36f, 50f, 8f, ink)
                eye(::p, u, 64f, 50f, 8f, ink)
                drawPath(Path().apply {
                    moveTo(p(45f, 62f).x, p(45f, 62f).y)
                    lineTo(p(55f, 62f).x, p(55f, 62f).y)
                    lineTo(p(50f, 68f).x, p(50f, 68f).y)
                    close()
                }, Color(0xFFE58A9A))
                drawPath(Path().apply {
                    moveTo(p(38f, 74f).x, p(38f, 74f).y)
                    quadraticTo(p(50f, 68f).x, p(50f, 68f).y, p(50f, 68f).x, p(50f, 68f).y)
                    quadraticTo(p(50f, 68f).x, p(50f, 68f).y, p(62f, 74f).x, p(62f, 74f).y)
                }, ink, style = line)
                for (side in listOf(-1f, 1f)) {
                    for (dy in listOf(-4f, 2f)) {
                        drawLine(ink.copy(alpha = 0.6f), p(50f + side * 14f, 66f + dy), p(50f + side * 38f, 62f + dy * 1.8f), 2f * u, StrokeCap.Round)
                    }
                }
            }
        }
    }
}

private val Ink = Mouse.Detail

/** Fill, then the shared outline. Every shape in the set goes through here. */
private fun DrawScope.outlined(
    p: (Float, Float) -> Offset,
    line: Stroke,
    fill: Color,
    path: Path,
) {
    drawPath(path, fill)
    drawPath(path, Ink, style = line)
}

private fun rect(p: (Float, Float) -> Offset, l: Float, t: Float, r: Float, b: Float) =
    androidx.compose.ui.geometry.Rect(p(l, t), p(r, b))

/** A big forward-facing eye with a catch-light. The difference between friendly and dead. */
private fun DrawScope.eye(p: (Float, Float) -> Offset, u: Float, x: Float, y: Float, r: Float, ink: Color) {
    drawCircle(Color.White, r * u, p(x, y))
    drawCircle(ink, r * u, p(x, y), style = Stroke(width = 2.2f * u))
    drawCircle(ink, r * 0.55f * u, p(x, y + r * 0.1f))
    drawCircle(Color.White, r * 0.22f * u, p(x + r * 0.25f, y - r * 0.25f))
}

private fun DrawScope.eyeStalks(p: (Float, Float) -> Offset, u: Float, ink: Color) {
    for (side in listOf(-1f, 1f)) {
        drawLine(ink, p(50f + side * 9f, 46f), p(50f + side * 11f, 32f), 2.6f * u, StrokeCap.Round)
        drawCircle(Color.White, 6f * u, p(50f + side * 11f, 29f))
        drawCircle(ink, 6f * u, p(50f + side * 11f, 29f), style = Stroke(width = 2.2f * u))
        drawCircle(ink, 3f * u, p(50f + side * 11f, 30f))
    }
}

private fun DrawScope.smile(p: (Float, Float) -> Offset, line: Stroke, x: Float, y: Float, w: Float, ink: Color) {
    drawPath(Path().apply {
        moveTo(p(x - w, y).x, p(x - w, y).y)
        quadraticTo(p(x, y + w * 0.85f).x, p(x, y + w * 0.85f).y, p(x + w, y).x, p(x + w, y).y)
    }, ink, style = line)
}

private fun hexagon(p: (Float, Float) -> Offset, cx: Float, cy: Float, r: Float): Path =
    Path().apply {
        for (i in 0 until 6) {
            val a = (i * 60f - 30f) * Math.PI.toFloat() / 180f
            val pt = p(cx + kotlin.math.cos(a) * r, cy + kotlin.math.sin(a) * r)
            if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
        }
        close()
    }
