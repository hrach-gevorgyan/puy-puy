# CLAUDE.md — Պույ-պույ խաղեր

Six mini-games for a three-year-old. Android, Kotlin, Compose. Solo project, no backend, no
network. Companion to [hashvir](https://github.com/hrach-gevorgyan/hashvir) — same character,
same island, same pipeline.

**Պույ-պույ is a he.** hashvir's README calls the character "she"; the family's own telling of
the tale calls him he, and that is canonical here. Armenian has no grammatical gender, so no
recorded clip is affected — only the English in this repo was wrong.

**The tale, in full**, because most design arguments end up being settled by it: he finds a
coconut with a small hole, squeezes in, and eats until his belly is round and packed tight.
Then he cannot get back out — he pushes, shoves, kicks and strains, and the shell has become a
prison. Alone in the dark he cries, and cries, hour after hour, until the tears wash away
every ounce he gained. At last he is light enough to slip through the hole, and he scampers
home having learned something exhausting about eating too much.

hashvir teaches the numbers 1–10. **This one teaches colours, and teaches them without ever
asking a question.** That is the distinction, and it decides most arguments about scope.

The owner overruled the earlier rule ("this one teaches nothing") in favour of a game that
actually carries content. The constraint that survives is the one the research forces: she is
three, and instruction-following is a five-year-old skill (docs/toddler-ux.md). So teaching
here has exactly one legal shape:

- **A target may exist, but it may only ever add.** Փուչիկներ shows him holding a balloon in
  the colour the sky is mostly about. Popping one that matches is named, bigger and louder.
  Popping anything else pops exactly as well. There is no wrong balloon.
- **Պահմտոցի asks**, at the owner's instruction: «Ո՞վ է թաքնվել», with four and a half seconds
  for her to answer before he does. The rule that survives is that **nothing waits** — the
  animal is already out, every coconut stays touchable, and the timer governs only when he
  speaks. A pause that stops the world reads as a crash, which is why this was cut the first
  time. No other game asks.
- **Teach by consequence where you can.** Կառուցի՛ր is the model: a coconut holds if it is
  smaller than the one under it, the rule is never stated, and a wrong guess topples a tower —
  which is the best part of building one, not a punishment.
- **Teach by repetition and contrast, not by instruction.** The same word many times close
  together, with a different colour on screen beside it — how a colour is learned at a kitchen
  table. `ColourRun` does this, and `ColourRunTest` asserts what a session actually sounds like.
- **A child who ignores the teaching entirely is playing correctly.** If any change would make
  that untrue, it belongs in hashvir.

Six games, reached from one hub: **Փուչիկներ** (pop balloons), **Կառուցի՛ր** (stack coconuts
biggest to smallest), **Պահմտոցի** (open coconuts), **Ձևե՛ր** (post shapes), **Փազլ** (a 3x3
jigsaw), **Նկարի՛ր** (finger paint).

**The tale is not played out in any game.** Կերակրի՛ր tried, three times, and was cut. The
coconut, the round belly and the crying remain what the app is ABOUT — the icon, his character,
the hub — and no game re-enacts them.

---

## Part 1 — Behavioural guidelines

Same as hashvir. They bias toward caution over speed; for trivial tasks, use judgment.

### 1. Think before coding

- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — do not pick silently.
- If a simpler approach exists, say so. Push back when warranted.

### 2. Simplicity first

Minimum code that solves the problem. No features beyond what was asked, no abstractions for
single-use code, no configurability nobody requested. If you write 200 lines and it could be
50, rewrite it.

### 3. Surgical changes

Touch only what you must. Do not "improve" adjacent code, do not refactor what is not broken,
match the existing style. Remove imports and helpers *your* change orphaned; leave pre-existing
dead code alone and mention it instead.

### 4. Goal-driven execution

Turn tasks into verifiable goals. State a short plan with a check against each step. Verify by
building and running, not by reasoning about whether it should work.

**Verification here means a device.** This app is animation, touch and sound; none of that is
covered by a compile. "It builds" is not "it works" and must never be reported as such.

---

## Part 2 — Project hard rules

These override "use judgment". Violating one is a bug, not a style difference.

### Privacy and dependencies

- **No permissions we declare, and none the user can see.** No INTERNET, ever. If a change
  needs a permission, stop and ask.

  Precisely: the merged manifest contains exactly one `uses-permission`, and it is
  `am.puypuy.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — a signature-level permission
  androidx.core defines against *our own* package so its runtime receivers are not exported.
  It grants nothing, is not shown to the user, and cannot be revoked. Verify with
  `aapt2 dump permissions` on the release APK; anything else appearing there is a bug.
- **No third-party dependencies** beyond AndroidX and Compose. No analytics, no crash
  reporting, no ads, no DI framework, no navigation library, no image loader.
- Nothing leaves the device, because there is no code that could send it.
- Voice clips are **build inputs**, committed as OGG. The build never reaches the network.

### How it treats the child

- **No failure states.** No score, no timer, no lives, no stars, no progress. Nothing is ever
  wrong, so nothing needs forgiving.
- **A miss gets a kind word, occasionally.** The rule used to be silence — "not a gentle
  reaction, none" — and the owner overruled it. When something does not work out he says
  «Ոչինչ» or «Կրկի՛ն փորձիր», about one time in three, through `Moment.encourage()`. Never the
  word "wrong", never a buzzer, never twice running. A voice that comments on every miss is a
  voice narrating her failures, which is worse than the silence it replaced.
- **Real achievements are always marked.** A finished tower, a completed board, a run of six
  pops: `Moment.happened(milestone = true)` praises every time rather than waiting for the
  one-in-ten. And when one thing is left to do, «Բան չմնաց» — `Moment.almost()`.
- **No progression.** Nothing unlocks anything. Nothing is ever locked.
- **No game plays itself.** He greets her and stops. Demonstrations — the app taking the first
  turn to show how — were tried in all six games and cut: it reads as the app playing without
  her, and the first move belongs to her.
- **No dead ends.** The same home button, top-left, 126dp, same icon, in all six games. One
  tap. Back inside a game goes to the hub; back on the hub does nothing.
- **Silent-safe.** Every audio reward has a visual equivalent, and the visual one is not
  optional. The app must be fully satisfying with the volume at zero — assume it is, because
  on a plane it will be.
- **No text for the child.** She cannot read. The Armenian tile names are for the adult.

### His voice

- **One voice, one face.** Every spoken clip is Պույ-պույ and his mouth moves while it plays.
  If a clip cannot be in his voice, it does not go in.
- **He speaks on events, rarely otherwise.** Openings, waking, the occasional nudge. A
  character who fills every pause is a character you mute — and a muted app loses every word.
- **Praise is rare** — exactly one per ten things she gets RIGHT — or it stops meaning
  anything. It follows success and nothing else: `Moment.happened(success = false)` for
  events that are significant but not achievements, such as a tower coming down. Praising a
  child for knocking something over teaches that knocking things over is the goal.
- SoundPool for effects, MediaPlayer for speech, two channels. Effects never interrupt speech;
  speech interrupts speech, latest wins.
- A missing clip logs nothing and does nothing. It never crashes. The app is playable with an
  empty `res/raw`, which is what makes adding audio safe to defer.

### Look

- **Saturated subject, muted ground.** The balloons, the shapes and the coconuts are always the
  most vivid things on screen. The island never competes; if a decoration out-competes the
  thing she is meant to touch, that is a bug.
- **Touch targets ≥ 126dp** — hashvir's floor, four times the usual adult minimum, not the
  original spec's 88dp.
- Portrait and landscape, phone and tablet. Layouts scale; they do not stretch.
- **Artwork is drawn in a design tool, not typed as coordinates in Kotlin.** This reverses the
  original rule ("every drawing is a Compose vector path; there is not one bitmap in the UI"),
  which turned out to be the ceiling on how good this app could look. Hand-authored bezier
  paths will never read like an illustrated children's book, and the child noticed long before
  anyone else did.

  The art ships as assets: WebP for illustration, VectorDrawable where a shape is genuinely
  geometric (the home icon). The six hub tiles and the launcher icon are generated images,
  cut and sized by `tools/make_tiles.py` and `tools/make_icon.py` from the sources in
  `tools/art/`; see [docs/art.md](docs/art.md) for the prompts and the rules they follow.
  Keep the APK under ~40 MB; it is currently around 9 MB.

  **Պույ-պույ himself is still drawn in code**, and has to be: he squashes, stretches, walks,
  waves, blinks, fills out with the tale and mirrors when he turns. A pose swapped between
  still images reads as a photograph being moved around, which was tried and rejected. He is
  drawn in the same flat style as the tiles — one navy outline of even weight, flat fills, no
  gradients — and the outline is achieved by drawing each shape twice, once enlarged in navy
  and once filled, so body and head fuse into a single silhouette.
- Bundle the Armenian font. Never rely on system fallback, which renders tofu on devices with
  no Armenian font. Armenian ascenders overflow the default line box, so any text needs an
  explicit `lineHeight`.

### Structure

- A game never knows about another game. It reaches the rest of the app only through
  `GameScope`, and drives the character only through `puypuy.react(state)`.
- Every positive event goes through `Moment.happened`. That is what guarantees each game fires
  all three channels — the world changes, he reacts, he says the word — and that the silent
  case always works. `Moment.ack` is the smaller version for touching something that is not a
  toy. There is no second reward routine; a game that draws its own celebration is a bug.
- Adding a seventh game is one `MiniGame` implementation plus one line in `games/Games.kt`.
  The hub solves its grid for whatever it finds; nothing else changes.

### Gotchas that have already cost a session

- **Effects are authored in dp**, and `FrameLoop` converts once. A speed or a size written in
  raw pixels is a different effect on every device.
- **`ClipsTest` matches exact string literals.** A clip name built by concatenation must be
  added to that test's `generated` list, or it reads as unreachable and the test fails.
- **`tools:ignore="MissingClass"` in the manifest is deliberate.** The class is real and on the
  runtime classpath; lint cannot resolve manifest classes from libraries. Removing the ignore
  breaks `./gradlew lint`; removing the provider under it puts emoji2's Play Services call back
  on every cold start.
- **Art is sized for the largest screen that asks, not for the phone.** A hub tile card is
  140dp on a phone and 280dp on a tablet, so the tiles ship at 576px.

---

## Part 3 — Deliberate reversals

Things that look like mistakes against the original spec and are not. Do not "fix" them back.

- **The tiles carry Armenian labels.** The spec said no text anywhere. hashvir reversed this
  and so does this app: the picture is what the child navigates by, the word is so an adult
  can find a game and so the name is in front of her in her own alphabet.
- **He never stops moving.** The spec asks for all animation to stop after 60s idle to save
  battery. hashvir judged a living character worth more, and it is the same character. He
  breathes, blinks and sways until he falls asleep at 90s.
- **Պահմտոցի uses coconuts, not shells.** The spec said shells. The coconut is the object the
  entire tale turns on, and the jackpot coconut has him curled up asleep inside it.
- **The bubbles game is Փուչիկներ, not Բշտիկներ.** «Բշտիկ» is a pimple.
- **The generated voice is pitch-shifted.** See docs/audio.md. It is not an artifact; it is how a
  male TTS becomes a mouse.

---

## Part 4 — Commands

```bash
./gradlew assembleDebug      # build
./gradlew testDebugUnitTest  # layout maths and the clip set
./gradlew lint               # Android lint
./gradlew installDebug       # to an attached device
./gradlew assembleRelease    # unsigned release APK

./tools/get_piper_voice.sh                                    # 63 MB voice model
python tools/gen_voices.py --words tools/words.csv \
                           --out app/src/main/res/raw         # regenerate speech

python tools/make_tiles.py   # tools/art/tile_*.png -> the six hub tiles
python tools/make_icon.py    # tools/art/icon_source.png -> the launcher icon
```

Speech and art are both **build inputs**, committed to the repo. Neither script runs as part of
the build — the build never reaches the network — and both are run by hand when the source
images or `words.csv` change.

---

## Part 5 — Release

`.github/workflows/release.yml` builds on every push and attaches an APK to a GitHub Release
when a `v*` tag is pushed. Signing keys come from repository secrets; without them the
workflow still builds and uploads an unsigned APK.

```bash
git tag v0.1.0 && git push origin v0.1.0
```

Distribution today is the APK on the Releases page. **Google Play is under consideration and
not decided.** If it is decided, [docs/play-store.md](docs/play-store.md) is the checklist: the
app is not submittable as it stands (target API level, App Bundle, Gradle signing config,
privacy policy, Target Audience and Data Safety declarations).

Keep the APK under ~40 MB so it sideloads easily before a trip. The release build is
2.0 MB (minified and resource-shrunk); the debug build is around 9 MB.

---

## Part 6 — Known gaps

Honest list, so nobody goes looking.

- **The app runs on a device and is played there, but nothing is verified automatically.**
  Every claim in this repo about how it *looks* or *feels* comes from the owner's own eyes on
  hardware, or from a still rendered by a script — never from a test.
- **The Armenian in `words.csv` has not been checked by a native speaker.** It was written by
  an assistant. Three lines have already been corrected after review; every line should be
  read aloud before this ships to a child.
- **Sound effects are missing** — `munch`, `brush`, `wipe`, and the 12 animal noises. All
  silent no-ops today, and `ClipsTest` holds the list. See the end of docs/audio.md.
- **The voice is a pitch-shifted male TTS.** It is intelligible and it is his, but it is not a
  young voice. The real options are a free Azure key (`hy-AM-AnahitNeural`) or recording a
  person; see docs/audio.md.
- **No contrast test.** hashvir enforces 4.5:1 for every fruit against every background with
  `ContrastTest`. This app inherited the palette but not the test.
- **No instrumented tests.** `LayoutTest` and `ClipsTest` cover the arithmetic and the clip
  set; nothing yet launches the Activity or taps anything.
- **Rotating mid-drawing scrambles the painting**, because strokes are stored in raw pixels.

### What the audit already fixed

Do not re-report these; they are done, and the tests that hold them are named.

| Was | Now |
|---|---|
| Bubbles never invalidated the Canvas (plain fields, not snapshot state) | `mutableFloatStateOf` on position and alpha |
| The coconut reset effect was keyed on state its own body wrote, so the board never reshuffled | keyed on `generation`, waits via `snapshotFlow` |
| Coconut #1 sat under the home button | square board below `Layout.HomeSafe`, held by `LayoutTest` |
| Paint's five 126dp controls needed 630dp of a 344dp row | `Layout.paintBarFitsRow`, column on narrow screens |
| Every fruit spawned inside the eat radius | `Layout.feed`, held by `LayoutTest` |
| Dragged fruit tracked at ~40% of finger speed | one long-lived animation re-aimed at a target |
| Release APK might ship silent under resource shrinking | `res/raw/keep.xml`, verified in the resource table |
| CI signing gate referenced a step-local `env`, so every release was unsigned | gated on `secrets`, plus zipalign and verify |
| Audio kept playing after the app was backgrounded | lifecycle observer, plus audio focus |
| Four hub tiles were measured off any screen under ~510dp | `Layout.hub`, held by `LayoutTest` |
| Six tiles were laid out six-across in landscape, far under the touch floor | `Layout.hub` solves for the column count; `LayoutTest` covers both tablet orientations |
| Particles, gravity and screen shake were measured in raw pixels, so every effect was a different size on every device | authored in dp, converted once in `FrameLoop` |
| Only the first finger was read in Փուչիկներ | every pointer that goes down is handled |
| Ten voice clips outlived the code that played them | deleted; `ClipsTest` now matches exact literals instead of prefixes |
| Two reward routines existed, and five of six games used neither | one `Moment`; `Rewards`, `Burst` and `RewardOverlay` deleted |
