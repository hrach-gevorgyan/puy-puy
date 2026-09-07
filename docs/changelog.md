# Changelog

Notable changes per release. Format loosely follows [Keep a Changelog]; versions are tags on
`main`, and each one is an APK on the Releases page.

## [Unreleased]

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
