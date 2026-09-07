package am.puypuy.core

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * §7.3. Adding a fifth game is one implementation plus one entry in the hub
 * list — the hub lays out however many games are registered.
 *
 * A game never knows about any other game, and never reaches for the hub.
 */
interface MiniGame {
    /** Drawable for the hub tile. No text — the picture is the whole label. */
    @get:DrawableRes
    val tileImage: Int

    /** Armenian name shown under the tile. For the grown-up; the picture is for her. */
    @get:StringRes
    val tileLabel: Int

    /** res/raw clip of him saying the name, played as the game opens. */
    val voiceKey: String

    /** Set up a fresh session. Called every time the game is opened. */
    fun onEnter()

    /** Release, stop audio. Called on the way back to the hub. */
    fun onExit()

    /** The game itself. [scope] is everything shared it is allowed to touch. */
    @Composable
    fun Content(scope: GameScope)
}

/**
 * What a game is handed. Deliberately small — and note what is NOT here: there is no way for
 * a game to ask the child for anything, because no game does.
 */
class GameScope(
    val audio: Audio,
    val puypuy: PuypuyController,
    val chatter: Chatter,
    val idle: Idle,
    private val scope: CoroutineScope,
) {
    internal fun launch(block: suspend CoroutineScope.() -> Unit): Job = scope.launch(block = block)

    internal fun newMoment(haptics: Haptics) = Moment(audio, puypuy, haptics, scope)
}
