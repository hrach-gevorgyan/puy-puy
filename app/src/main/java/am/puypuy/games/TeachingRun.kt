package am.puypuy.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

/**
 * One thing at a time, for a while, with something else beside it.
 *
 * This is how both teaching games work, and it is the only shape of teaching that survives
 * contact with a three-year-old: instruction-following is a five-year-old skill
 * (docs/toddler-ux.md), so nothing may be asked. Instead the world quietly leans — the sky is
 * mostly red balloons, the sand is mostly apples — he names the thing once when it changes, and
 * she plays with whatever she likes.
 *
 * A turn lasts a NUMBER OF SECONDS, not a number of items. Counting items looks equivalent and
 * is not: a game that fills an empty screen with eight of them at once would spend a whole turn
 * before the child touched anything, and announce the next one over a screen still full of the
 * last.
 *
 * Pure and seeded, so [am.puypuy.TeachingRunTest] can assert what a session actually
 * sounds like rather than the game claiming it.
 */
class TeachingRun<T : Any>(
    private val all: List<T>,
    private val random: Random = Random.Default,
) {

    /**
     * Snapshot state, not a plain field. Games read this during composition to draw the thing
     * he is holding; Compose only re-runs what reads state it can observe, and as a plain `var`
     * he went on holding the previous one while his voice announced the next.
     */
    var subject: T by mutableStateOf(all.random(random))
        private set

    private var secondsLeft = length()

    /**
     * Move time on. Returns true on the frame the subject changes — the one moment worth
     * saying a word out loud.
     */
    fun advance(seconds: Float): Boolean {
        secondsLeft -= seconds
        if (secondsLeft > 0f) return false
        subject = all.filter { it != subject }.random(random)
        secondsLeft = length()
        return true
    }

    /**
     * What the next item should be: mostly the subject, sometimes deliberately not.
     *
     * The minority is not noise. A word maps to a property only when the property can be told
     * apart from the thing carrying it, and that needs a not-red balloon in the sky at the same
     * time as the red ones.
     */
    fun next(): T =
        if (random.nextFloat() < CONTRAST) all.filter { it != subject }.random(random) else subject

    private fun length() = MIN_SECONDS + random.nextFloat() * (MAX_SECONDS - MIN_SECONDS)

    private companion object {
        /** How long one subject stays up. Long enough to hear its name several times over. */
        const val MIN_SECONDS = 22f
        const val MAX_SECONDS = 34f

        /** Share deliberately off-subject, so there is always something to contrast against. */
        const val CONTRAST = 0.22f
    }
}
