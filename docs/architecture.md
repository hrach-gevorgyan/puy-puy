# Architecture

One Activity, one Compose tree, no navigation library, no view models. Small enough to hold in
your head, which is the point — the interesting constraints here are about a three-year-old,
not about software.

```
app/src/main/java/am/hrach/puypuy/
  MainActivity.kt        immersive fullscreen, keeps the screen on, hands off to PuypuyApp
  core/                  everything shared. Knows nothing about any specific game.
    Layout.kt            the layout arithmetic, extracted so LayoutTest can assert it
    Puypuy.kt            the character: states, the controller, the drawing
    Look.kt           the island and the saturated subject colours
    Audio.kt             two channels: speech and effects
    Chatter.kt           what he says when nothing was asked of him
    Moment.kt            the one routine every meaningful event goes through
    Juice.kt             shared feel: springs, squash, haptics, particles, shake, frame loop
    Idle.kt              how long since the last touch anywhere
    MiniGame.kt          the interface, and GameScope — all a game may touch
  ui/
    App.kt               the root: navigation, the intro, idle clock, back handling
    Hub.kt               the six tiles, the beach, and the coconut chase
    Island.kt            the background, the palms, and the coconut
    HomeButton.kt        top-left, 126dp, identical in all six games
    ParentGate.kt        off by default
  games/
    Games.kt             the registry — the only place a game is named
    BalloonsGame.kt  StackGame.kt   CoconutsGame.kt
    ShapesGame.kt    PuzzleGame.kt  PaintGame.kt
    TeachingRun.kt       one subject at a time, shared by the games that name things
    Tower.kt             the stacking rule, and why a tower falls
    FruitArt.kt          the eight fruits, shared with hashvir
    Animals.kt           the twelve animals
```

## The one rule that shapes everything

**Games do not talk to each other, to the hub, or to the character directly.** A game gets a
`GameScope` and nothing else:

```kotlin
class GameScope(
    val audio: Audio,
    val puypuy: PuypuyController,
    val chatter: Chatter,
    val idle: Idle,
)
```

It cannot navigate, cannot see the other games, cannot animate the character. It calls
`puypuy.react(Happy)` and the character decides what that looks like.

This is why the six games feel like one app rather than six, and it is why adding a seventh is
one file plus one line in `Games.kt`. The hub reads the registry and solves its grid for
whatever is in it — 2×3 upright, 3×2 sideways, more columns on a tablet.

## The one routine — `Moment`

Everything that matters to the child goes through `Moment.happened()`, and it fires **three
channels together**:

1. **The world changes** — the caller has already done it; the particles are the visible proof,
   thrown in the touched object's own colours.
2. **He reacts** — a whole-body state, never a subtle mood.
3. **He says the plain word** — a label on what happened, never a question. With no word of its
   own, an event is occasionally worth praise instead: about one in ten, and then only
   sometimes.

Plus a fourth channel that survives a muted plane: haptics.

This exists because of one finding in [toddler-ux.md](toddler-ux.md): under-sixes could not
read an animal character's expressions as feedback. A reaction on its own is therefore never
enough, and routing every event through one function is what stops a game quietly firing one
channel and calling it done.

`Moment.ack()` is the smaller sibling: she touched sand, sky, water or him — no word, no
reaction, just proof the screen is alive. There is no such thing as a touch that does nothing.

Particles, springs, squash, haptics and screen shake all live in `Juice.kt`, and everything
there is authored in **dp** and converted once in `FrameLoop`, so an effect is the same gesture
on a 1x phone, a 3x phone and a tablet.

## Audio

Two channels, because they have different rules:

- **Effects** — `SoundPool`, up to six streams, fire and forget.
- **Speech** — `MediaPlayer`, one at a time. Speech interrupts speech, latest wins. Effects
  never interrupt speech.

Clips are resolved by `res/raw` name at runtime through `getIdentifier`. **A name with no file
behind it is a silent no-op.** That is a deliberate design choice, not laziness: it let the
whole app be built and played before a single clip existed, and it means a half-recorded set
never crashes.

`Audio.onSpeaking` drives `PuypuyController.speaking`, which moves his mouth. One voice, one
face.

## Idle

A one-second tick in `App.kt` drives everything time-based:

| After | What happens |
|---|---|
| 25s | He says something, once, to see if anyone is there |
| 90s | He falls asleep. Any touch wakes him, `Surprised`, with a line |

The clock is poked on every pointer event, not just the first of a gesture: a finger held down
through a long scribble is not idleness.

Any touch anywhere resets it. The listener sits on the `Initial` pointer pass so it sees the
touch whatever the game below does with it.

## Layout

Anything that decides a size or a position lives in `core/Layout.kt` as a pure function of
`(width, height, count)`, not inline in a composable.

This is not tidiness. Every layout bug this app has had was arithmetic — four tiles measured
off a 360dp screen, five 126dp controls in a 344dp row, a coconut grid whose first cell landed
under the home button, an eat radius that swallowed every fruit at spawn. None of it needed a
device to catch, and none of it was catchable while it was buried in a `Box`. `LayoutTest` runs the same functions the app runs, against eight reference screens including
both tablet orientations.

`Layout.HomeSafe` is the corner the home button occupies. No game may put anything tappable
inside it, and the coconut test asserts exactly that.

## What is deliberately absent

- **No view model, no repository, no use cases.** There is no data. Game state is a handful of
  `mutableStateOf` fields on the `MiniGame` object, cleared in `onEnter`.
- **No persistence.** Nothing is saved, including the painting — she will expect a blank page
  and gets one.
- **No navigation library.** One `Int`, `-1` for the hub, cross-faded in 300ms.
- **No hand-typed illustration.** Art comes from the design pass as assets; only
  deformation (squash, stretch, the belly filling out) is done in code.
