package am.puypuy.games

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import am.puypuy.core.Fruit
import am.puypuy.ui.drawCoconut

/**
 * The fruit, drawn as vector paths on a normalized 100 x 100 box.
 *
 * Each one is fill first, then the shared outline, then details on top — so the silhouette
 * always reads at a distance and the details only add up close. Fills stay fully saturated;
 * the outline is what carries contrast against the pastel ground.
 */
fun DrawScope.drawFruit(fruit: Fruit, boxSize: Float, topLeft: Offset = Offset.Zero) {
    val unit = boxSize / 100f
    // 3dp on a full-size fruit, scaled with the drawing so a small menu icon is not fenced in
    // by a heavy line, and clamped so it can neither vanish nor dominate.
    val scale = (boxSize / Fruit.ReferenceDiameterDp.dp.toPx()).coerceIn(0.7f, 1.6f)
    val stroke = Stroke(width = Fruit.OutlineWidthDp.dp.toPx() * scale)
    translate(topLeft.x, topLeft.y) {
        when (fruit) {
            Fruit.Apple -> apple(unit, fruit, stroke)
            Fruit.Orange -> orange(unit, fruit, stroke)
            Fruit.Banana -> banana(unit, fruit, stroke)
            Fruit.Pear -> pear(unit, fruit, stroke)
            Fruit.Strawberry -> strawberry(unit, fruit, stroke)
            Fruit.Grapes -> grapes(unit, fruit, stroke)
            Fruit.Coconut -> drawCoconut(boxSize * 0.86f, Offset(boxSize * 0.07f, boxSize * 0.07f))
            Fruit.Mango -> mango(unit, fruit, stroke)
        }
    }
}

private fun DrawScope.stem(u: Float, fruit: Fruit, x: Float, y: Float, height: Float) {
    drawLine(
        color = Color(0xFF8A6D3B),
        start = Offset(x * u, y * u),
        end = Offset(x * u, (y - height) * u),
        strokeWidth = 4f * u,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.leaf(u: Float, color: Color, cx: Float, cy: Float, w: Float, h: Float, tilt: Float) {
    val path = Path().apply {
        moveTo((cx - w / 2f) * u, cy * u)
        quadraticTo((cx - w * 0.1f) * u, (cy - h) * u, (cx + w / 2f) * u, (cy - tilt) * u)
        quadraticTo((cx - w * 0.1f) * u, (cy + h * 0.35f) * u, (cx - w / 2f) * u, cy * u)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.apple(u: Float, fruit: Fruit, stroke: Stroke) {
    val body = Path().apply {
        // Two lobes with a dip at the top — the shape that says "apple" at a glance.
        moveTo(50f * u, 28f * u)
        cubicTo(38f * u, 12f * u, 8f * u, 18f * u, 10f * u, 50f * u)
        cubicTo(12f * u, 76f * u, 32f * u, 96f * u, 50f * u, 88f * u)
        cubicTo(68f * u, 96f * u, 88f * u, 76f * u, 90f * u, 50f * u)
        cubicTo(92f * u, 18f * u, 62f * u, 12f * u, 50f * u, 28f * u)
        close()
    }
    drawPath(body, fruit.color)
    drawPath(body, fruit.outline, style = stroke)
    stem(u, fruit, 50f, 26f, 18f)
    leaf(u, fruit.detail, 62f, 16f, 30f, 16f, 6f)
    highlight(u, 32f, 42f, 9f, 14f)
}

private fun DrawScope.orange(u: Float, fruit: Fruit, stroke: Stroke) {
    drawCircle(fruit.color, radius = 40f * u, center = Offset(50f * u, 55f * u))
    drawCircle(fruit.outline, radius = 40f * u, center = Offset(50f * u, 55f * u), style = stroke)
    // Dimpled skin, just enough to separate it from the apple.
    for (angle in listOf(20, 90, 160, 240, 310)) {
        val rad = Math.toRadians(angle.toDouble())
        drawCircle(
            Color(0x22000000),
            radius = 3f * u,
            center = Offset(
                (50f + 26f * Math.cos(rad).toFloat()) * u,
                (55f + 26f * Math.sin(rad).toFloat()) * u,
            ),
        )
    }
    stem(u, fruit, 50f, 18f, 8f)
    leaf(u, fruit.detail, 60f, 12f, 26f, 13f, 4f)
    highlight(u, 34f, 42f, 8f, 12f)
}

private fun DrawScope.banana(u: Float, fruit: Fruit, stroke: Stroke) {
    // Thick through the belly, tapering to a stem at one end and a blossom tip at the other.
    val body = Path().apply {
        moveTo(24f * u, 20f * u)
        // Outer edge, sweeping down and to the right.
        cubicTo(6f * u, 62f * u, 34f * u, 94f * u, 82f * u, 90f * u)
        // Blossom tip.
        cubicTo(94f * u, 89f * u, 95f * u, 72f * u, 83f * u, 71f * u)
        // Inner edge, back up. Kept far from the outer edge so it stays fat.
        cubicTo(50f * u, 68f * u, 33f * u, 47f * u, 40f * u, 22f * u)
        // Shoulder into the stem.
        cubicTo(42f * u, 13f * u, 26f * u, 12f * u, 24f * u, 20f * u)
        close()
    }
    drawPath(body, fruit.color)
    drawPath(body, fruit.outline, style = stroke)

    // Two facets: a banana is ridged, not a tube.
    for ((start, end) in listOf(
        Offset(31f * u, 30f * u) to Offset(74f * u, 80f * u),
        Offset(36f * u, 26f * u) to Offset(78f * u, 76f * u),
    )) {
        val facet = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(
                start.x - 4f * u, start.y + (end.y - start.y) * 0.55f,
                start.x + (end.x - start.x) * 0.45f, end.y + 5f * u,
                end.x, end.y,
            )
        }
        drawPath(facet, Color(0x2E000000), style = Stroke(width = 2.5f * u))
    }
    drawOval(
        color = Color(0x4DFFFFFF),
        topLeft = Offset(26f * u, 28f * u),
        size = Size(12f * u, 26f * u),
    )

    // Stem, and the dark blossom end.
    drawLine(
        color = fruit.detail,
        start = Offset(31f * u, 18f * u),
        end = Offset(33f * u, 6f * u),
        strokeWidth = 9f * u,
        cap = StrokeCap.Round,
    )
    drawCircle(Color(0xFF6B4A2A), radius = 5f * u, center = Offset(88f * u, 80f * u))
}

private fun DrawScope.pear(u: Float, fruit: Fruit, stroke: Stroke) {
    val body = Path().apply {
        moveTo(50f * u, 22f * u)
        cubicTo(34f * u, 24f * u, 34f * u, 46f * u, 38f * u, 56f * u)
        cubicTo(24f * u, 66f * u, 22f * u, 92f * u, 50f * u, 92f * u)
        cubicTo(78f * u, 92f * u, 76f * u, 66f * u, 62f * u, 56f * u)
        cubicTo(66f * u, 46f * u, 66f * u, 24f * u, 50f * u, 22f * u)
        close()
    }
    drawPath(body, fruit.color)
    drawPath(body, fruit.outline, style = stroke)
    stem(u, fruit, 50f, 22f, 14f)
    leaf(u, fruit.detail, 62f, 12f, 26f, 13f, 4f)
    highlight(u, 36f, 68f, 8f, 12f)
}

private fun DrawScope.strawberry(u: Float, fruit: Fruit, stroke: Stroke) {
    val body = Path().apply {
        moveTo(50f * u, 94f * u)
        cubicTo(20f * u, 76f * u, 12f * u, 46f * u, 26f * u, 32f * u)
        cubicTo(38f * u, 20f * u, 62f * u, 20f * u, 74f * u, 32f * u)
        cubicTo(88f * u, 46f * u, 80f * u, 76f * u, 50f * u, 94f * u)
        close()
    }
    drawPath(body, fruit.color)
    drawPath(body, fruit.outline, style = stroke)
    // Seeds: the detail that makes it unmistakably a strawberry.
    for ((sx, sy) in listOf(
        38f to 44f, 52f to 40f, 64f to 48f, 30f to 58f, 46f to 58f,
        62f to 64f, 38f to 72f, 54f to 76f,
    )) {
        drawCircle(Color(0xFFFFE066), radius = 3.2f * u, center = Offset(sx * u, sy * u))
    }
    for (angle in listOf(-40f, 0f, 40f)) {
        leaf(u, fruit.detail, 50f + angle * 0.45f, 26f, 30f, 15f, angle * 0.15f)
    }
    stem(u, fruit, 50f, 22f, 10f)
}

private fun DrawScope.grapes(u: Float, fruit: Fruit, stroke: Stroke) {
    val berries = listOf(
        50f to 30f,
        36f to 46f, 64f to 46f, 50f to 50f,
        24f to 62f, 50f to 68f, 76f to 62f,
        37f to 80f, 63f to 80f,
    )
    stem(u, fruit, 50f, 24f, 14f)
    leaf(u, fruit.detail, 64f, 14f, 30f, 16f, 5f)
    for ((bx, by) in berries) {
        drawCircle(fruit.color, radius = 15f * u, center = Offset(bx * u, by * u))
        drawCircle(fruit.outline, radius = 15f * u, center = Offset(bx * u, by * u), style = stroke)
    }
    drawCircle(Color(0x40FFFFFF), radius = 4.5f * u, center = Offset(45f * u, 26f * u))
}

/** Mango: a fat teardrop leaning over, with a blushed shoulder and one leaf. */
private fun DrawScope.mango(u: Float, fruit: Fruit, stroke: Stroke) {
    val body = Path().apply {
        moveTo(72f * u, 30f * u)
        cubicTo(94f * u, 44f * u, 88f * u, 80f * u, 56f * u, 86f * u)
        cubicTo(26f * u, 91f * u, 8f * u, 66f * u, 18f * u, 46f * u)
        cubicTo(28f * u, 27f * u, 54f * u, 20f * u, 72f * u, 30f * u)
        close()
    }
    drawPath(body, fruit.color)
    drawPath(body, Color(0x33E8352E))
    drawPath(body, fruit.outline, style = stroke)
    leaf(u, fruit.detail, 74f, 26f, 26f, 16f, 6f)
    highlight(u, 40f, 44f, 12f, 8f)
}

/** A soft catch-light. Flat shapes read as flat; one highlight makes them read as round. */
private fun DrawScope.highlight(u: Float, cx: Float, cy: Float, rx: Float, ry: Float) {
    drawOval(
        color = Color(0x59FFFFFF),
        topLeft = Offset((cx - rx) * u, (cy - ry) * u),
        size = Size(rx * 2f * u, ry * 2f * u),
    )
}
