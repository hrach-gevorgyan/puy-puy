package am.puypuy

import am.puypuy.core.Layout
import am.puypuy.core.MinTouch
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reference screens. Every one of these is a device someone actually holds, and the app
 * has to work on all of them without anything being measured off the edge.
 */
private val SCREENS = listOf(
    "small phone portrait" to (320.dp to 480.dp),
    "phone portrait" to (360.dp to 640.dp),
    "large phone portrait" to (411.dp to 823.dp),
    "small phone landscape" to (480.dp to 320.dp),
    "phone landscape" to (640.dp to 360.dp),
    "tablet portrait" to (800.dp to 1280.dp),
    "tablet landscape" to (1280.dp to 800.dp),
    "split screen" to (360.dp to 320.dp),
)

class LayoutTest {

    /** Four tiles used to be laid out in a row on any landscape screen, off the edge. */
    @Test
    fun `hub tiles always fit the screen width`() {
        for ((name, size) in SCREENS) {
            val (w, h) = size
            val hub = Layout.hub(w, h, games = 4)
            assertTrue(
                "$name (${w.value}x${h.value}): tile row needs ${hub.widthNeeded(20.dp).value}dp of ${w.value}dp",
                hub.widthNeeded(20.dp) <= w + 1.dp,
            )
        }
    }

    @Test
    fun `hub tiles never fall below the touch floor`() {
        for ((name, size) in SCREENS) {
            val hub = Layout.hub(size.first, size.second, games = 4)
            assertTrue("$name: tile ${hub.tile.value}dp", hub.tile >= MinTouch)
        }
    }

    /**
     * All six games have to be reachable without scrolling on any phone worth the name. The
     * hub is the one screen where a scroll is a real failure: a child who cannot see a game
     * does not know to look for it, and there is nothing on screen to say it exists.
     */
    @Test
    fun `six games fit a phone without scrolling`() {
        for ((name, size) in SCREENS) {
            val (w, h) = size
            val hub = Layout.hub(w, h, games = 6)
            assertTrue("$name: tile ${hub.tile.value}dp", hub.tile >= MinTouch)
            assertTrue(
                "$name: grid needs ${hub.widthNeeded().value}dp of ${w.value}dp",
                hub.widthNeeded() <= w + 1.dp,
            )
            // Two rows of the 126dp floor genuinely do not fit a phone lying on its side, and
            // saying so is better than pretending: those screens scroll, upright ones must not.
            if (h < 600.dp) continue
            val room = h - hub.character * 0.62f
            assertTrue(
                "$name: grid needs ${hub.heightNeeded().value}dp of ${room.value}dp",
                hub.heightNeeded() <= room + 1.dp,
            )
        }
    }

    /**
     * A tablet is not a big phone. Every reference screen has to end up with tiles in a
     * sensible shape and a character sized for the screen it is on — "layouts scale, they do
     * not stretch" is a hard rule, and it is the one nobody notices breaking until the app is
     * on the other device.
     */
    @Test
    fun `the hub scales rather than stretches`() {
        for ((name, size) in SCREENS) {
            val (w, h) = size
            val hub = Layout.hub(w, h, games = 6)
            // Tiles must stay roughly square-ish in their share of the screen: a tile under
            // half the width of its own column is a stretched layout, not a scaled one.
            val column = (w - hub.gap * (hub.columns + 1)) / hub.columns
            assertTrue(
                "$name: ${hub.tile.value}dp tile in a ${column.value}dp column",
                hub.tile >= column * 0.5f,
            )
            assertTrue("$name: character ${hub.character.value}dp", hub.character >= 120.dp)
            assertTrue("$name: character ${hub.character.value}dp", hub.character <= 240.dp)
        }
    }

    /** Six across a phone held sideways is what the old orientation rule produced. */
    @Test
    fun `the hub never puts every game in one row on a phone`() {
        assertEquals(3, Layout.hub(640.dp, 360.dp, games = 6).columns)
        assertEquals(2, Layout.hub(360.dp, 640.dp, games = 6).columns)
    }

    @Test
    fun `hub lays out every registered game`() {
        for (games in 2..6) {
            val hub = Layout.hub(800.dp, 1280.dp, games)
            assertTrue("$games games", hub.columns * hub.rows >= games)
        }
    }

    /**
     * Five 126dp controls need 630dp. On a phone this MUST report false, or the eraser is
     * measured to zero width and a scribbled page can never be cleared.
     */
    @Test
    fun `paint controls never overflow their row`() {
        for ((name, size) in SCREENS) {
            val w = size.first
            if (Layout.paintBarFitsRow(w, controls = 5)) {
                assertTrue(
                    "$name claims a row fits but needs ${(MinTouch * 5).value}dp of ${w.value}dp",
                    MinTouch * 5 <= w,
                )
            }
        }
        assertFalse("360dp phone cannot fit 5 controls in a row", Layout.paintBarFitsRow(360.dp, 5))
        assertTrue("a tablet can", Layout.paintBarFitsRow(1280.dp, 5))
    }

    /**
     * Five coconuts that overlapped into one lump, crammed into a third of the screen. All of
     * it was arithmetic, and none of it needed a device to catch.
     */
    @Test
    fun `loose coconuts never overlap and use the width`() {
        for ((name, size) in SCREENS) {
            val (w, h) = size
            val tower = Layout.tower(w, h)
            for (i in 0 until tower.restX.size - 1) {
                val edgeToEdge = (tower.restX[i + 1] - tower.restX[i]) - tower.radii[i] - tower.radii[i + 1]
                assertTrue(
                    "$name: coconuts $i and ${i + 1} overlap by ${-edgeToEdge.value}dp",
                    edgeToEdge >= 0.dp,
                )
            }
            assertTrue(
                "$name: the row needs ${tower.rowWidth().value}dp of ${w.value}dp",
                tower.rowWidth() <= w + 1.dp,
            )
            // Only where width is what limits them. On a short landscape screen the
            // coconuts are capped by HEIGHT, and a narrower row there is right, not a bug.
            if (h > w) {
                assertTrue(
                    "$name: the row uses only ${(tower.rowWidth() / w * 100f).toInt()}% of the width",
                    tower.rowWidth() >= w * 0.7f,
                )
            }
        }
    }

    @Test
    fun `the finished tower fits on the screen and clears the home button`() {
        for ((name, size) in SCREENS) {
            val (w, h) = size
            val tower = Layout.tower(w, h)
            val top = tower.baseY - tower.heightNeeded()
            assertTrue("$name: the tower reaches ${top.value}dp, off the top", top >= 0.dp)
            assertTrue("$name: the tower is built below the sand", tower.baseY <= h)
            // It may pass the home button's height, but never its corner.
            val leftEdge = tower.baseX - tower.radii.last()
            assertTrue(
                "$name: the pile is ${leftEdge.value}dp from the left, under the ${Layout.HomeSafe.value}dp home button",
                leftEdge >= Layout.HomeSafe || top >= Layout.HomeSafe,
            )
        }
    }

    @Test
    fun `every coconut is worth reaching for`() {
        // The smallest still has to look like a coconut rather than a crumb.
        for ((name, size) in SCREENS) {
            val tower = Layout.tower(size.first, size.second)
            assertTrue("$name: only ${tower.radii.size} coconuts", tower.radii.size >= 3)
            assertTrue("$name: the smallest is ${tower.radii.first().value * 2}dp across", tower.radii.first() >= 18.dp)
            assertTrue("$name: the biggest is only ${tower.radii.last().value * 2}dp across", tower.radii.last() >= 30.dp)
        }
    }

    /**
     * A composable is laid out at 0x0 for one frame before it is measured, and Կառուցի՛ր
     * crashed on the frame it opened: the layout fell back to its smallest tower while the
     * game was still holding five coconuts, and drawing the fifth ran off the end of the list.
     */
    @Test
    fun `an unmeasured screen still gives a usable layout`() {
        for ((name, size) in listOf(
            "unmeasured" to (0.dp to 0.dp),
            "one pixel" to (1.dp to 1.dp),
            "silly narrow" to (40.dp to 900.dp),
            "silly short" to (900.dp to 40.dp),
        )) {
            val tower = Layout.tower(size.first, size.second)
            assertTrue("$name: no coconuts at all", tower.radii.isNotEmpty())
            assertEquals("$name: a place is missing for a coconut", tower.radii.size, tower.restX.size)
            for (r in tower.radii) {
                assertTrue("$name: a coconut of ${r.value}dp", r > 0.dp)
            }
        }
    }

    /** Coconut #1 sat under the home button: aiming at a toy left the game. */
    @Test
    fun `no coconut sits under the home button`() {
        for ((name, size) in SCREENS) {
            val (w, h) = size
            val board = Layout.coconutBoard(w, h)
            for (i in 0 until 9) {
                val cx = board.originX + board.cell * (i % 3 + 0.5f)
                val cy = board.originY + board.cell * (i / 3 + 0.5f)
                val underHome = cx < Layout.HomeSafe && cy < Layout.HomeSafe
                assertFalse("$name: cell $i centre (${cx.value}, ${cy.value}) is under the home button", underHome)
            }
        }
    }

    @Test
    fun `coconut board stays on screen`() {
        for ((name, size) in SCREENS) {
            val (w, h) = size
            val board = Layout.coconutBoard(w, h)
            assertTrue("$name: board starts left of 0", board.originX >= 0.dp)
            assertTrue("$name: board overflows width", board.originX + board.span <= w + 1.dp)
            assertTrue("$name: board overflows height", board.originY + board.span <= h + 1.dp)
        }
    }

    @Test
    fun `home safe area covers the whole home button`() {
        assertEquals(MinTouch + 24.dp, Layout.HomeSafe)
    }
}
