package am.puypuy.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * §2.3, optional and off by default. A three-second press on a small corner
 * marker leaves the app — long enough that a 3-year-old will let go first.
 * The ring filling up is the only feedback, and it is deliberately faint.
 */
@Composable
fun ParentGate(onPass: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val fill = remember { Animatable(0f) }

    Canvas(
        modifier
            .size(56.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val hold = scope.launch {
                            fill.snapTo(0f)
                            fill.animateTo(1f, tween(3000))
                            onPass()
                        }
                        tryAwaitRelease()
                        hold.cancel()
                        scope.launch { fill.animateTo(0f, tween(200)) }
                    },
                )
            }
    ) {
        val r = size.minDimension * 0.30f
        val w = size.minDimension * 0.06f
        drawCircle(Color.White.copy(alpha = 0.30f), r, style = Stroke(width = w))
        if (fill.value > 0f) {
            drawArc(
                color = Color.White.copy(alpha = 0.85f),
                startAngle = -90f,
                sweepAngle = 360f * fill.value,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = w),
            )
        }
    }
}
