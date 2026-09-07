package am.puypuy.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

/**
 * What Պույ-պույ says when nothing has been asked of him.
 *
 * He carries the app — there is no text to read and no menu to explore — so a silent character
 * is a screen with nothing on it. But a character who fills every pause is a character you mute,
 * and a muted app loses every word in it.
 *
 * The rule: **he speaks on events, and only rarely otherwise.** Game openings, waking up, the
 * occasional nudge if he has been left alone. Everything else goes through [Moment], which
 * already speaks.
 */
class Chatter(private val audio: Audio, private val puypuy: PuypuyController) {

    private var lastSpokenAt = 0L

    /**
     * His introduction is the first thing the child ever hears and the only time he says who
     * he is. Nothing else speaks until it has finished.
     */
    var introFinished = false

    /**
     * Say one of [keys], picked at random, unless he has spoken very recently.
     *
     * [gapMs] is the floor between two unprompted lines. Rewards do not go through here, so
     * naming a colour or a fruit is never suppressed by it.
     */
    fun say(vararg keys: String, gapMs: Long = 0L, reaction: PuypuyState? = null) {
        if (!introFinished) {
            reaction?.let { puypuy.react(it) }
            return
        }
        val now = System.currentTimeMillis()
        if (gapMs > 0L && now - lastSpokenAt < gapMs) return
        lastSpokenAt = now
        reaction?.let { puypuy.react(it) }
        audio.say(keys.random())
    }

    /** Used by the idle chatter to space itself against everything else he says. */
    fun quietFor(ms: Long): Boolean = System.currentTimeMillis() - lastSpokenAt > ms

    companion object {
        /** The one game that still greets on entry; the others demonstrate instead. */
        const val PAINT_START = "paint_start"

        /** Spoken on the hub after he has been left looking at the tiles. */
        val HUB = arrayOf("hub_what_to_play", "hub_pick_one", "hub_hello_again")
        val WAKE = arrayOf("wake_oh", "wake_hello")
    }
}

/**
 * His opening line, once per visit to a game. Delayed a beat so it lands after the cross-fade
 * rather than under it.
 */
@Composable
fun GreetOnEnter(scope: GameScope, key: String, delayMs: Long = 450L) {
    LaunchedEffect(key) {
        delay(delayMs)
        // If the child opened a game while he was still introducing himself, wait rather
        // than talk over him — and rather than drop the greeting altogether.
        while (!scope.chatter.introFinished) delay(200)
        scope.chatter.say(key, reaction = PuypuyState.Waving)
    }
}

/**
 * The nudge, on the hub only: he asks what to play, because on the hub that is the actual
 * open question.
 *
 * Inside a game there is no generic chatter at all: a line attached to nothing is noise, and
 * noise is what makes a parent mute the app and lose every word in it. A game that wants to
 * nudge shows something happening instead.
 */
@Composable
fun HubChatter(chatter: Chatter, idle: Idle, onHub: Boolean) {
    LaunchedEffect(idle.seconds >= Idle.CHATTER_AFTER, idle.asleep, onHub) {
        if (!onHub || idle.asleep || idle.seconds < Idle.CHATTER_AFTER) return@LaunchedEffect
        if (!chatter.quietFor(8_000L)) return@LaunchedEffect
        chatter.say(*Chatter.HUB, reaction = PuypuyState.Suggesting)
    }
}

