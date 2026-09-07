package am.puypuy.core

import am.puypuy.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The same island as hashvir, so the two apps are one world.
 *
 * From the tale: Պույ-պույ Ճստունի finds a coconut, climbs inside, eats until he is too round
 * to get back out, and cries himself thin enough to escape. Everything here happens on that
 * beach.
 */
/** Tropical set dressing. Muted on purpose: scenery, never the subject. */
object Island {
    val Sea = Color(0xFF5FBDBF)
    val SeaDeep = Color(0xFF3E9BA3)
    val SandDark = Color(0xFFE8CFA6)
    val PalmLeaf = Color(0xFF5C9E52)
    val PalmDark = Color(0xFF3E7A44)
    val Trunk = Color(0xFFA97B4F)
    val Sun = Color(0xFFFFD166)
    val CoconutShell = Color(0xFF8A5A3B)
    val CoconutDark = Color(0xFF6B4429)
    val CoconutFlesh = Color(0xFFFBF3E4)
}

object Mouse {
    val Body = Color(0xFFB9B3C7)
    val Ear = Color(0xFFF2C4C9)
    val Detail = Color(0xFF3A4454)
}

object Ink {
    val Primary = Color(0xFF2E2A28)
}

/**
 * The saturated layer: the things she is meant to touch. Same rule as hashvir — the subject is
 * always more vivid than the island behind it.
 */
enum class Paint(val color: Color, val voiceKey: String) {
    RED(Color(0xFFE03131), "color_red"),
    BLUE(Color(0xFF2D7FC1), "color_blue"),
    YELLOW(Color(0xFFF2B705), "color_yellow"),
    GREEN(Color(0xFF3BA55C), "color_green"),
    ORANGE(Color(0xFFF5901E), "color_orange"),
    PURPLE(Color(0xFF7B3FA0), "color_purple"),
}

/**
 * The countable, draggable things: fully saturated, always more vivid than the island.
 *
 * hashvir's six, plus the coconut of the tale and a mango, because this island is tropical and
 * a 3-year-old on a plane should be looking at fruit she can point at on the way.
 */
enum class Fruit(val color: Color, val detail: Color, val voiceKey: String) {
    Apple(Color(0xFFE03131), Color(0xFF2F9E44), "food_apple"),
    Orange(Color(0xFFF5901E), Color(0xFF2F9E44), "food_orange"),
    Banana(Color(0xFFF2B705), Color(0xFF8A6D3B), "food_banana"),
    Pear(Color(0xFF94C11F), Color(0xFF6B8E13), "food_pear"),
    Strawberry(Color(0xFFE8352E), Color(0xFF2F9E44), "food_strawberry"),
    Grapes(Color(0xFF7B3FA0), Color(0xFF2F9E44), "food_grapes"),
    Coconut(Island.CoconutShell, Island.CoconutDark, "food_coconut"),
    Mango(Color(0xFFF2762E), Color(0xFF2F9E44), "food_mango"),
    ;

    val outline: Color get() = OutlineColor

    companion object {
        val OutlineColor = Mouse.Detail
        const val OutlineWidthDp = 3f

        /** The size the outline width is quoted against: one fruit at the dp floor. */
        const val ReferenceDiameterDp = 126f
    }
}

/**
 * Touch targets are never smaller than this. hashvir's 126dp floor rather than the spec's
 * 88dp: four times the usual adult minimum, because toddlers have gross motor control and
 * almost no fine motor control.
 */
val MinTouch = 126.dp

/**
 * Bundled, as in hashvir, so Armenian never falls back to the system font — which renders tofu
 * on any device without an Armenian-locale font. Armenian ascenders overflow the default line
 * box, so every piece of text here sets an explicit lineHeight.
 */
val Armenian = FontFamily(
    Font(R.font.noto_sans_armenian_bold, FontWeight.Bold),
    Font(R.font.noto_sans_armenian_black, FontWeight.Black),
)
