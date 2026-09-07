package am.puypuy.ui

import am.puypuy.BuildConfig
import am.puypuy.core.Audio
import am.puypuy.core.Chatter
import am.puypuy.core.GameScope
import am.puypuy.core.HubChatter
import am.puypuy.core.Idle
import am.puypuy.core.MiniGame
import am.puypuy.core.PuypuyState
import am.puypuy.core.rememberPuypuy
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

private const val HUB = -1

@Composable
fun PuypuyApp(games: List<MiniGame>, onLeaveApp: () -> Unit) {
    val context = LocalContext.current
    val audio = remember { Audio(context.applicationContext) }
    DisposableEffect(audio) { onDispose { audio.release() } }

    val idle = remember { Idle() }
    val puypuy = rememberPuypuy()

    // One voice, one face: his mouth moves whenever he is the one talking.
    DisposableEffect(audio, puypuy) {
        audio.onSpeaking = { puypuy.speaking = it }
        onDispose { audio.onSpeaking = null }
    }

    // She stops when the app leaves the foreground. Otherwise he keeps talking in a pocket,
    // over whatever the adult switched to.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, audio) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                // ON_PAUSE, not ON_STOP: stop is delivered late enough that he got a word
                // or two out into the user's pocket first.
                Lifecycle.Event.ON_PAUSE -> audio.pauseAll()
                Lifecycle.Event.ON_RESUME -> audio.resumeEffects()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val scope = rememberCoroutineScope()
    val chatter = remember(audio, puypuy) { Chatter(audio, puypuy) }

    // She introduces herself once, on the way in. It is the first thing the child hears and
    // the only time he gets to say who he is, so nothing else is allowed to talk over it.
    var introDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (introDone) return@LaunchedEffect
        delay(700)
        puypuy.react(PuypuyState.Waving)
        audio.say("intro")
        // Roughly the length of the clip. Games check this before greeting, so opening one
        // immediately no longer cuts her off in the middle of her own name.
        delay(5_200)
        introDone = true
    }
    LaunchedEffect(introDone) { chatter.introFinished = introDone }

    // One second tick drives both idle thresholds (§1, §7.1).
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            idle.tick()
            // Walking counts as idle here: on the hub he is always walking after his
            // coconut, and without this he would never fall asleep at all.
            val unoccupied = puypuy.state == PuypuyState.Idle || puypuy.state == PuypuyState.Walking
            if (idle.asleep && unoccupied) {
                puypuy.react(PuypuyState.Sleeping)
            }
        }
    }

    var current by rememberSaveable { mutableIntStateOf(HUB) }

    // Any touch anywhere wakes him (§7.1). Watched on the Initial pass so it
    // sees the touch whatever the game below does with it.
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Poke on every event, not just the first: a finger held down through a
                    // long scribble is not idleness.
                    val first = awaitPointerEvent(PointerEventPass.Initial)
                    if (idle.poke()) {
                        // Waking her up is an event worth a word.
                        chatter.say(*Chatter.WAKE, reaction = PuypuyState.Surprised)
                    }
                    while (first.changes.any { it.pressed }) {
                        val next = awaitPointerEvent(PointerEventPass.Initial)
                        idle.poke()
                        if (next.changes.none { it.pressed }) break
                    }
                }
            }
    ) {
        Crossfade(
            targetState = current,
            animationSpec = tween(300),
            label = "screen",
        ) { screen ->
            if (screen == HUB) {
                Hub(
                    games = games,
                    puypuy = puypuy,
                    audio = audio,
                    onOpen = { current = it },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val game = games[screen]
                DisposableEffect(game) {
                    game.onEnter()
                    onDispose {
                        game.onExit()
                        audio.stopAll()
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    game.Content(GameScope(audio, puypuy, chatter, idle, scope))
                    HomeButton(
                        onClick = { current = HUB },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    )
                }
            }
        }

        HubChatter(chatter, idle, onHub = current == HUB)

        if (BuildConfig.PARENT_GATE && current == HUB) {
            ParentGate(
                onPass = onLeaveApp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            )
        }
    }

    // §1: back inside a game goes to the hub; back on the hub does nothing.
    BackHandler(enabled = true) {
        if (current != HUB) current = HUB
    }
}
