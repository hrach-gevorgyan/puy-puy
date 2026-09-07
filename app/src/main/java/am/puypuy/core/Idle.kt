package am.puypuy.core

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * How long since the last touch anywhere in the app.
 *
 * Two thresholds matter (§1, §7.1):
 *  - 25s — he says something, once, to see if anyone is there.
 *  - 90s — Պույ-պույ falls asleep.
 */
class Idle {
    private var lastTouch = SystemClock.uptimeMillis()

    /** Whole seconds since the last touch. Recomposes once a second. */
    var seconds by mutableIntStateOf(0)
        private set

    val asleep: Boolean get() = seconds >= SLEEP_AFTER

    /** Returns true if this touch woke him up. */
    fun poke(): Boolean {
        val wasAsleep = asleep
        lastTouch = SystemClock.uptimeMillis()
        seconds = 0
        return wasAsleep
    }

    internal fun tick() {
        seconds = ((SystemClock.uptimeMillis() - lastTouch) / 1000L).toInt()
    }

    companion object {
        /** She wonders aloud where the child went. */
        const val CHATTER_AFTER = 25

        const val SLEEP_AFTER = 90
    }
}

