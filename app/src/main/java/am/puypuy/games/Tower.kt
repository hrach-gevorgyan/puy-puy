package am.puypuy.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A tower of coconuts, biggest at the bottom.
 *
 * The teaching is size ordering — seriation — which is the skill underneath counting and one a
 * three-year-old can do with her hands long before she can talk about it. Nothing is explained
 * and nothing is asked: she puts a coconut on the pile and the pile either holds or it does
 * not, and after a few goes she starts reaching for the big ones first.
 *
 * **Toppling is not failing.** Knocking a tower down is the best part of building one, so a
 * bad stack falls over in a heap she can immediately rebuild, with no sound of disapproval and
 * no reaction from him beyond delight. The only thing a topple changes is how much help she
 * gets next time.
 *
 * Pure, so [am.puypuy.TowerTest] can hold the rule that makes it teach: a smaller
 * coconut always holds, a bigger one always brings the tower down.
 */
class Tower(private val sizes: List<Int> = DEFAULT_SIZES) {

    /** Still on the sand, waiting to be picked up. */
    val loose = mutableStateListOf<Int>()

    /** On the pile, bottom first. */
    val stacked = mutableStateListOf<Int>()

    /**
     * How far each stacked coconut sits from the centre of the one below it, as a fraction of
     * that one's radius. This is what makes the tower a real tower: she chooses WHERE it goes,
     * not only which one, and a pile that drifts sideways falls over like a pile of anything
     * else would.
     */
    val offsets = mutableStateListOf<Float>()

    /** Towers toppled since the last one that stood. Drives [help] and nothing else. */
    var topples by mutableIntStateOf(0)
        private set

    /** True the instant every coconut is stacked in order — the tower she built. */
    var finished by mutableStateOf(false)
        private set

    /** 0 no help, 1 the next one is nudging, 2 it is unmistakable. */
    val help: Int get() = topples.coerceAtMost(2)

    /** The one that belongs next: the biggest still loose. */
    val nextRight: Int? get() = loose.maxOrNull()

    init {
        restart()
    }

    fun restart() {
        loose.clear()
        loose += sizes.shuffled()
        stacked.clear()
        offsets.clear()
        finished = false
        topples = 0
    }

    /** Everything falls back to the sand, shuffled, ready to go again. */
    fun collapse() {
        loose += stacked
        stacked.clear()
        offsets.clear()
        // A finished tower that is knocked down is a tower to build again. Leaving this set
        // meant every later coconut was refused, and the game quietly stopped working.
        finished = false
        val again = loose.toList().shuffled()
        loose.clear()
        loose += again
        topples++
    }

    /**
     * She has put [size] on the pile, [offset] radii from the centre of the one below it.
     * Returns true if it held.
     *
     * Two ways for a coconut to fall, and neither is ever stated anywhere in the game:
     *
     *  - it is BIGGER than the one under it, so there is nothing to sit on;
     *  - it is too far OFF the one under it, so it slides off the side.
     *
     * The second is what stops the game being solved by dropping everything in roughly the
     * right order and never looking: she has to place each one, not just choose it.
     */
    fun place(size: Int, offset: Float = 0f): Boolean {
        if (finished || size !in loose) return false
        // From here it is ON the pile whatever happens next — it either holds or it brings
        // the tower down, and the caller can tell which from the return value.
        val top = stacked.lastOrNull()
        loose -= size
        stacked += size
        offsets += offset.coerceIn(-1.5f, 1.5f)

        if (top != null && size > top) return false
        if (top != null && kotlin.math.abs(offset) > BALANCE) return false
        // A tower that has drifted a long way sideways goes over even if every coconut looked
        // fine on the one below it.
        if (kotlin.math.abs(offsets.sum()) > LEAN) return false

        if (loose.isEmpty()) {
            finished = true
            topples = 0
        }
        return true
    }

    companion object {
        /** Five coconuts, distinct enough that the difference is visible across the screen. */
        val DEFAULT_SIZES = listOf(5, 4, 3, 2, 1)

        /**
         * How far off centre one coconut may sit on another, in radii of the one below. Wide
         * enough that a three-year-old's aim is fine, tight enough that dropping it anywhere
         * near the pile is not the same as placing it.
         */
        const val BALANCE = 0.55f

        /** How far the whole pile may drift before it goes over, in radii. */
        const val LEAN = 1.1f
    }
}
