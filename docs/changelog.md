# Changelog

Notable changes per release. Format loosely follows [Keep a Changelog]; versions are tags on
`main`, and each one is an APK on the Releases page.

## [Unreleased]

### Added
- **Կառուցի՛ր** replaces the feeding game: stack five coconuts, biggest at the bottom. A
  coconut holds if it is smaller than the one below and not dropped too far off its side, and
  a bad tower leans further every second and then comes down — which is the best part of
  building one. Neither rule is stated anywhere in the game.
- Nine shapes in **Ձևե՛ր** — rectangle, oval, diamond and crescent join the original five —
  with six on screen at a time, scattered at random rather than laid in a row.
- Encouragement when something does not work out: «Ոչինչ», «Կրկի՛ն փորձիր», about one time in
  three. «Բան չմնաց» when one thing is left to do.
- **Պահմտոցի** asks «Ո՞վ է թաքնվել» and leaves four and a half seconds for her to answer
  first. Nothing waits on it: the animal is already out and every coconut stays touchable.
- Hearts when she touches him on the hub.

### Changed
- The app teaches. Colours in Փուչիկներ, size in Կառուցի՛ր, shapes in Ձևե՛ր — and only
  Պահմտոցի ever asks a question.
- Praise follows success only, exactly one in ten, and real achievements are always marked.
  It used to fire on any significant event, so knocking a tower over could be congratulated.
- No game plays itself. All six used to take the first turn as a demonstration.
- Every effect is authored in dp and converted once, so a burst is the same gesture on a
  phone and on a tablet.
- Layout arithmetic for the tower, the shapes and the puzzle moved into `Layout` with tests.

### Fixed
- Turning the device mid-drawing scrambled the painting: strokes were stored in pixels, and
  are now fractions of the canvas.
- Փազլ printed the answer under the board. There is no ghost now.
- Կառուցի՛ր crashed on the frame it opened, on a screen that had not been measured yet.
- A finished tower refused every later coconut and piled them onto one spot.
- Voice clips peaked at 0 dBFS; there is a limiter at −1.5 dB.

### Removed
- Undo in Նկարի՛ր: two near-identical buttons in one corner, and the only control whose
  effect a three-year-old cannot predict.
- The tale as something played out in a game. It remains what the app is about.

### Added
- Two more games: **Ձևե՛ր** (post shapes into holes) and **Փազլ** (a 3x3 jigsaw), bringing the
  hub to six.
- Illustrated hub tiles and a launcher icon, generated and cut by `tools/make_tiles.py` and
  `tools/make_icon.py`. See [docs/art.md](art.md).
- `Moment`: the one routine every meaningful event goes through, firing the world, his
  reaction and the plain word together. See [docs/toddler-ux.md](toddler-ux.md) for why a
  character's reaction alone is not feedback.
- `Juice`: shared springs, squash, haptics, particles, screen shake and the one frame loop.
- A coconut chase along the bottom of the hub, run on physics rather than fixed speeds.

### Changed
- Պույ-պույ redrawn to match the illustrated tiles: flat fills, one navy outline of even
  weight, head and body as a single silhouette.
- Speech is clearer — a high-pass and a consonant lift, chosen by ear against six candidate
  recipes. Pitch and speed are unchanged.
- Every effect is authored in dp and converted once, so bursts, gravity and shake are the same
  gesture on a phone and on a tablet.
- Փուչիկներ answers every finger, not just the first, and its clouds, ground and character are
  all touchable.
- Ձևե՛ր and Փազլ can no longer be solved by tapping: a piece has to be carried.

### Removed
- `Rewards`, `Burst` and `RewardOverlay` — a second reward routine that only one game still
  called. `Moment` is the only one.
- Ten voice clips that outlived the code that played them, and the unused `Bubble.kt`.

### Fixed
- Only the first pointer was read in Փուչիկներ, so half a two-handed grab was discarded.
- The hub laid six tiles across a phone in landscape, far under the touch floor.

## Earlier, before the first tag

### Added
- The hub and four games: **Փուչիկներ**, **Կերակրի՛ր**, **Պահմտոցի**, **Նկարի՛ր**.
- Պույ-պույ, reused from hashvir, with two states added for this app — `Surprised` and
  `Sleeping` — driven only through `puypuy.react(state)`.
- The island, also from hashvir: palms, coconuts, sun, sea, scattered shells.
- The shared reward routine. Every positive event in every game goes through it, so the
  silent-volume case is guaranteed rather than remembered.
- 59 Armenian speech clips in his voice, generated locally by Piper and pitch-shifted into a
  mouse. See docs/audio.md.
- His introduction, played once over her wave when the app opens.
- Chatter: a spoken opening per game, lines on waking, and a rare nudge after 25s of silence.
- Bundled Noto Sans Armenian, so Armenian never falls back to tofu.
- Optional parent gate behind a build flag, off by default.

### Notes
- Nothing has been verified on a device yet.
- The Armenian text has not been checked by a native speaker.

[Keep a Changelog]: https://keepachangelog.com/en/1.1.0/
