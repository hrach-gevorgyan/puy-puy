package am.puypuy

import am.puypuy.core.Paint
import am.puypuy.games.TeachingRun
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a session of Փուչիկներ actually sounds like.
 *
 * The game claims to teach colour words to a three-year-old without ever asking her anything.
 * That claim is entirely about the SHAPE of the exposure — the same word several times close
 * together, with a different colour visible beside it — so it is asserted here rather than
 * believed. A flat "name a colour every fourth pop" gives about three scattered words per
 * colour in a sitting, which is ambient noise and not teaching.
 */
class TeachingRunTest {

    /** A sitting: 2.5 minutes, the attention span in docs/toddler-ux.md. */
    private val sessionSeconds = 150f
    private val frame = 1f / 60f

    /** One colour's turn: how long it lasted and what the sky held while it did. */
    private data class Stint(val colour: Paint, val seconds: Float, val balloons: List<Paint>)

    /**
     * Replays a sitting frame by frame, spawning a balloon every 0.7s the way the game does
     * once the sky is full, and starting with the burst of eight that fills an empty sky.
     */
    private fun play(seed: Int, seconds: Float = sessionSeconds): List<Stint> {
        val run = TeachingRun(Paint.entries, Random(seed))
        val stints = mutableListOf<Stint>()
        var colour = run.subject
        var balloons = mutableListOf<Paint>()
        var elapsed = 0f
        var sinceSpawn = 0f
        repeat(8) { balloons += run.next() }     // the opening fill
        var t = 0f
        while (t < seconds) {
            t += frame
            elapsed += frame
            sinceSpawn += frame
            if (sinceSpawn >= 0.7f) {
                balloons += run.next()
                sinceSpawn = 0f
            }
            if (run.advance(frame)) {
                stints += Stint(colour, elapsed, balloons)
                colour = run.subject
                balloons = mutableListOf()
                elapsed = 0f
            }
        }
        return stints
    }

    private fun seeds() = 1..24

    /**
     * The bug this model replaced: counting a run in BALLOONS meant the opening burst of eight
     * spent most of a run before the child touched anything, and he announced a new colour over
     * a sky still full of the old one.
     */
    @Test
    fun `filling an empty sky does not rush the colour`() {
        for (seed in seeds()) {
            val first = play(seed, seconds = 3f)
            assertTrue("seed $seed: the colour changed ${first.size} times in the first 3 seconds", first.isEmpty())
        }
    }

    @Test
    fun `a colour stays up long enough to be heard`() {
        for (seed in seeds()) {
            for (stint in play(seed)) {
                assertTrue("seed $seed: a colour lasted only ${stint.seconds}s", stint.seconds >= 20f)
                assertTrue("seed $seed: a colour lasted ${stint.seconds}s", stint.seconds <= 36f)
            }
        }
    }

    @Test
    fun `the sky is mostly the colour he is holding`() {
        val shares = seeds().flatMap { seed ->
            play(seed).map { it.balloons.count { b -> b == it.colour }.toFloat() / it.balloons.size }
        }
        assertTrue("the sky averaged only ${shares.average()} of his colour", shares.average() >= 0.70)
        assertTrue("one turn came out only ${shares.min()} his colour", shares.min() >= 0.35f)
    }

    @Test
    fun `there is nearly always another colour to contrast against`() {
        // A word maps to a property only if the property can be told apart from the object
        // carrying it. A sky of one single colour teaches "balloon", not "red".
        val all = seeds().flatMap { play(it) }
        val withContrast = all.count { stint -> stint.balloons.any { it != stint.colour } }
        assertTrue(
            "only $withContrast of ${all.size} turns had any contrast in the sky",
            withContrast >= all.size * 0.9,
        )
    }

    @Test
    fun `a colour never follows itself`() {
        for (seed in seeds()) {
            val order = play(seed, seconds = 600f).map { it.colour }
            for ((a, b) in order.zipWithNext()) {
                assertTrue("seed $seed: $a followed itself", a != b)
            }
        }
    }

    @Test
    fun `every colour comes round`() {
        val seen = seeds().flatMap { play(it, seconds = 600f) }.map { it.colour }.toSet()
        assertEquals("some colours never came up at all", Paint.entries.toSet(), seen)
    }

    /**
     * The number this whole change exists for: what she would actually HEAR in one sitting.
     * He names the colour when he picks it up and says nothing else, so this counts the turns
     * she gets — each one a word over a sky she can check it against.
     */
    @Test
    fun `she hears a handful of colours per sitting, each with time to sink in`() {
        val turns = seeds().map { play(it).size }
        assertTrue("only ${turns.min()} colours in the quietest sitting", turns.min() >= 4)
        assertTrue("${turns.max()} colours in the busiest sitting is nagging", turns.max() <= 7)
    }
}
