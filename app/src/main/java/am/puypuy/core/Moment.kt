package am.puypuy.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Everything that happens to the child, in one call.
 *
 * This class exists because of one finding in docs/toddler-ux.md: children under six watched
 * an app in which an animal character gave feedback through facial expressions and noises, and
 * they did not understand it. A character's reaction is therefore never the feedback on its
 * own. Every meaningful event fires **three channels together**, and routing them all through
 * one function is what stops a game quietly firing only one:
 *
 *   1. **The world changes** — the caller does this before calling, and the particles here are
 *      the visible proof of it, thrown in the touched object's own colours.
 *   2. **He reacts, unmistakably** — a whole-body state, not a subtle mood.
 *   3. **He says the plain word** — a label on what just happened, never a question.
 *
 * Plus the fourth channel that survives a muted plane: haptics.
 */
class Moment(
    private val audio: Audio,
    private val puypuy: PuypuyController,
    private val haptics: Haptics,
    private val scope: CoroutineScope,
) {
    var particles: Particles? = null
    var shake: Shake? = null

    /** Bumped when a game is left, so a delayed word never lands over the next screen. */
    private var generation = 0

    /** Events since he last praised her. Praise is rare on purpose — see [praiseOrNothing]. */
    private var sincePraise = 0

    /** Things that did not work out since he last said anything kind about it. */
    private var sinceEncouraged = 0

    fun endSession() {
        generation++
        particles?.clear()
    }

    /**
     * Roughly one event in ten, and then only sometimes: praise on every success stops
     * meaning anything.
     */
    private fun praiseOrNothing(): String? {
        // Exactly one in ten things she gets right, rather than a one-in-three chance after
        // ten. Praise that arrives at random reads as unrelated to what she did; praise that
        // arrives reliably, and rarely, reads as being about the thing she just managed.
        if (sincePraise < 10) return null
        sincePraise = 0
        return PRAISE.random()
    }

    /**
     * A small acknowledgement: she touched something that is not a toy — sand, sky, water, him.
     * No word, no reaction from him, just proof the screen is alive.
     *
     * There is no such thing as a touch that does nothing.
     */
    fun ack(
        at: Offset,
        colours: List<Color>,
        count: Int = 7,
        speed: ClosedFloatingPointRange<Float> = 90f..260f,
        spread: Float = (2f * Math.PI).toFloat(),
        direction: Float = 0f,
        ttl: Float = 0.7f,
    ) {
        haptics.tap()
        particles?.burst(at, colours, count, speed, spread, direction, ttl = ttl)
    }

    /**
     * A real event: a balloon popped, a fruit eaten, a coconut cracked, a shape home.
     *
     * [colours] are the touched object's own — colour inheritance is what makes the burst read
     * as caused by her. [word] is a `res/raw` name and is a LABEL: it says what happened. If a
     * caller ever wants to pass a question here, the answer is no.
     */
    fun happened(
        at: Offset,
        colours: List<Color>,
        word: String? = null,
        reaction: PuypuyState = PuypuyState.Happy,
        trauma: Float = 0f,
        effect: String? = null,
        count: Int = 16,
        wordDelayMs: Long = 380L,
        /**
         * A real achievement — a finished tower, a full board, a run of pops. He always says
         * something about these, rather than waiting for the one-in-ten to come round.
         */
        milestone: Boolean = false,
        /**
         * Whether this is something she got RIGHT. Praise only ever follows a true here.
         *
         * Not every significant event is a success — a tower coming down is loud, deserves a
         * burst and a reaction, and must never be answered with "well done". Getting this
         * wrong is worse than saying nothing: praise for knocking something over teaches that
         * knocking things over is the goal.
         */
        success: Boolean = true,
    ) {
        val startedIn = generation

        // 1 — the world. The caller has already changed it; this is the proof.
        particles?.burst(at, colours, count)
        if (trauma > 0f) shake?.add(trauma)

        // 4 — the channel that works with the volume at zero.
        haptics.confirm()

        // 2 — him, unmistakably.
        puypuy.react(reaction)

        // 3 — the plain word, after the event rather than over it. With no word of its own,
        // the event is occasionally worth praise instead.
        effect?.let { audio.effect(it) }
        if (success) sincePraise++
        val spoken = when {
            word != null -> word
            milestone -> {
                sincePraise = 0
                PRAISE.random()
            }
            success -> praiseOrNothing()
            else -> null
        }
        if (spoken != null) {
            scope.launch {
                delay(wordDelayMs)
                if (generation == startedIn) audio.say(spoken)
            }
        }
    }

    /**
     * Something did not work out: the tower came down, the shape would not go in.
     *
     * Never a rebuke and never the word "wrong" — «Ոչինչ», «Կրկի՛ն փորձիր». It is deliberately
     * occasional: a voice that comments on every miss is a voice narrating her failures, which
     * is worse than the silence this app used to keep.
     */
    fun encourage(wordDelayMs: Long = 420L) {
        sinceEncouraged++
        if (sinceEncouraged < 3) return
        sinceEncouraged = 0
        val startedIn = generation
        scope.launch {
            delay(wordDelayMs)
            if (generation == startedIn) audio.say(ENCOURAGE.random())
        }
    }

    /** One thing left to do. «Բան չմնաց» — almost nothing left. */
    fun almost(wordDelayMs: Long = 420L) {
        val startedIn = generation
        scope.launch {
            delay(wordDelayMs)
            if (generation == startedIn) audio.say("encourage_close")
        }
    }

    /**
     * He does the thing himself, once, on entering a game.
     *
     * A demonstration is the only kind of instruction that works at three: she watched it
     * happen, so there is nothing to understand. No game may explain itself any other way.
     */
    private companion object {
        /**
         * hashvir's recorded praise clips, copied across so both apps praise the child in the
         * same voice. Only the ones that exist are ever played, so dropping a praise_9.ogg
         * into res/raw is the whole job of adding another.
         */
        val PRAISE = (1..8).map { "praise_$it" }

        /** Kind things to say when something did not work. None of them is a "no". */
        val ENCOURAGE = listOf("encourage_again", "encourage_nothing", "encourage_more")
    }

    fun demonstrate(delayMs: Long = 700L, block: () -> Unit) {
        val startedIn = generation
        scope.launch {
            delay(delayMs)
            if (generation == startedIn) block()
        }
    }
}

@Composable
fun rememberMoment(scope: GameScope): Moment {
    val haptics = rememberHaptics()
    return remember(scope, haptics) { scope.newMoment(haptics) }
}
