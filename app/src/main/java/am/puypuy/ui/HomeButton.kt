package am.puypuy.ui

import am.puypuy.R
import am.puypuy.core.MinTouch
import am.puypuy.core.Mouse
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image

/**
 * How big the button LOOKS. Small enough to stay out of the way of the game, because it is
 * furniture rather than a toy.
 */
val HomeButtonVisual = 64.dp

/**
 * How big the button IS. The touch target stays generous and stays the full [MinTouch] rule —
 * a three-year-old aims at the picture and lands anywhere near it, and the corner is hers.
 */
val HomeButtonSafe = MinTouch + 8.dp

/**
 * Top-left, [MinTouch], the same icon in the same place in all four games. One tap goes home;
 * there is no confirmation and no second step.
 *
 * It floats above live play, so every game must keep its toys out of [HomeButtonSafe] — a
 * child who aims at a coconut and leaves the game instead has been failed by the layout, not
 * by her aim.
 */
@Composable
fun HomeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .padding(8.dp)
            // The hit area is MinTouch; only the white disc inside it is small.
            .size(MinTouch)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // A white disc is invisible on the paint page, which is also white. The ring is
        // what makes the button exist on every background.
        Box(
            Modifier
                .size(HomeButtonVisual)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f))
                .border(3.dp, Mouse.Detail.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_home),
                contentDescription = null,
                modifier = Modifier.size(HomeButtonVisual * 0.62f),
            )
        }
    }
}
