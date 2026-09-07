package am.puypuy

import am.puypuy.games.Tower
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rule that makes Կառուցի՛ր teach: a smaller coconut holds, a bigger one brings the tower
 * down. It is never said out loud anywhere in the game, so it had better be exactly right.
 */
class TowerTest {

    @Test
    fun `smaller on bigger holds`() {
        val tower = Tower()
        assertTrue(tower.place(5))
        assertTrue(tower.place(4))
        assertTrue(tower.place(3))
        assertEquals(listOf(5, 4, 3), tower.stacked.toList())
    }

    @Test
    fun `a coconut dropped off the side slides off`() {
        val tower = Tower()
        tower.place(5)
        assertTrue("dead centre would not stay on", tower.place(4, offset = 0f))
        val tipping = Tower()
        tipping.place(5)
        assertFalse("it balanced on the very edge", tipping.place(4, offset = 0.9f))
    }

    @Test
    fun `a childs aim is good enough`() {
        // Precise dragging is the one thing three-year-olds are measurably bad at
        // (docs/toddler-ux.md), so being roughly on top has to count as on top.
        val tower = Tower()
        tower.place(5)
        assertTrue("half a radius off was refused", tower.place(4, offset = 0.5f))
        assertTrue(tower.place(3, offset = -0.4f))
    }

    @Test
    fun `a pile that drifts one way goes over`() {
        // Every coconut sat fine on the one below it, and the tower still leans out of the
        // world. That is what makes it a tower rather than a checklist.
        val tower = Tower()
        tower.place(5)
        assertTrue(tower.place(4, offset = 0.5f))
        assertTrue(tower.place(3, offset = 0.5f))
        assertFalse("the pile leaned right off its base and stood there", tower.place(2, offset = 0.5f))
    }

    @Test
    fun `bigger on smaller brings it down`() {
        val tower = Tower()
        tower.place(2)
        assertFalse("a bigger coconut balanced on a smaller one", tower.place(5))
    }

    @Test
    fun `the first coconut always holds, whatever it is`() {
        for (first in Tower.DEFAULT_SIZES) {
            val tower = Tower()
            assertTrue("$first would not sit on the sand", tower.place(first))
        }
    }

    @Test
    fun `a topple loses nothing`() {
        val tower = Tower()
        tower.place(2)
        tower.place(5)
        tower.collapse()
        assertEquals("coconuts went missing in the tumble", Tower.DEFAULT_SIZES.size, tower.loose.size)
        assertEquals(Tower.DEFAULT_SIZES.toSet(), tower.loose.toSet())
        assertTrue("the pile survived its own collapse", tower.stacked.isEmpty())
    }

    @Test
    fun `help arrives after a topple and stops climbing`() {
        val tower = Tower()
        assertEquals("she was helped before she had done anything", 0, tower.help)
        tower.collapse()
        assertEquals(1, tower.help)
        repeat(10) { tower.collapse() }
        assertEquals("help has to stop somewhere", 2, tower.help)
    }

    @Test
    fun `the right one to pick next is always the biggest left`() {
        val tower = Tower()
        var last = Int.MAX_VALUE
        while (!tower.finished) {
            val next = tower.nextRight!!
            assertTrue("the biggest left was bigger than the one under it", next < last)
            assertTrue(tower.place(next, offset = 0f))
            last = next
        }
        assertEquals(Tower.DEFAULT_SIZES.sortedDescending(), tower.stacked.toList())
    }

    @Test
    fun `finishing clears the slate`() {
        val tower = Tower()
        tower.collapse()
        Tower.DEFAULT_SIZES.sortedDescending().forEach { tower.place(it) }
        assertTrue(tower.finished)
        assertEquals("she finished still carrying old topples", 0, tower.topples)
    }

    @Test
    fun `it can be built again and again`() {
        val tower = Tower()
        repeat(4) {
            tower.restart()
            Tower.DEFAULT_SIZES.sortedDescending().forEach { tower.place(it) }
            assertTrue(tower.finished)
        }
    }
}
