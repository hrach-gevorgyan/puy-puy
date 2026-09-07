# Պույ-պույ խաղեր

**Six Armenian mini-games for a three-year-old.** Colours, sizes and shapes, on Պույ-պույ's
island — and it never asks her a single question.

[![Build](https://github.com/hrach-gevorgyan/puy-puy/actions/workflows/release.yml/badge.svg)](https://github.com/hrach-gevorgyan/puy-puy/actions/workflows/release.yml)
[![Download APK](https://img.shields.io/github/v/release/hrach-gevorgyan/puy-puy?label=download%20apk&color=3BA55C)](https://github.com/hrach-gevorgyan/puy-puy/releases/latest)
[![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)](#-build)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)](#-build)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](#-build)

[![Permissions](https://img.shields.io/badge/permissions-none-3BA55C)](#-what-it-does-not-do)
[![Ads](https://img.shields.io/badge/ads-none-3BA55C)](#-what-it-does-not-do)
[![Tracking](https://img.shields.io/badge/tracking-none-3BA55C)](#-what-it-does-not-do)
[![Offline](https://img.shields.io/badge/works-offline-3BA55C)](#-what-it-does-not-do)
[![APK](https://img.shields.io/badge/apk-2%20MB-3BA55C)](#-build)
[![License](https://img.shields.io/badge/license-MIT-2E2A28)](LICENSE)

Companion app to [**hashvir**](https://github.com/hrach-gevorgyan/hashvir) — the same character,
the same island, the same voice. hashvir teaches the numbers 1–10; this one teaches colours and
size.

---

## 🥥 The story it lives in

**Պույ-պույ Ճստունի** is a mouse from an Armenian children's tale. He finds a coconut, squeezes
in through a small hole, eats until he is too round to get back out, and cries himself thin
enough to slip free.

Everything here happens on his beach, and every word the child hears is his — one voice, one
face, from the greeting to the last word.

---

## 🎮 Six ways to play

<table>
<tr>
<td width="33%" valign="top">

### 🎈 Փուչիկներ
**Pop the balloons**

Every balloon pops. The sky runs on one colour at a time and he holds a balloon in that colour,
naming it once when it changes.

Six reds in a row, then six blues — the same word many times, close together, with something
different beside it.

</td>
<td width="33%" valign="top">

### 🥥 Կառուցի՛ր
**Stack the tower**

Five coconuts, biggest at the bottom. A coconut holds if it is smaller than the one below and
not dropped too far off its side.

Get it wrong and the tower leans, then comes down — which is the best part of building one.

</td>
<td width="33%" valign="top">

### 🌴 Պահմտոցի
**Who is hiding?**

Nine coconuts. Crack one open and an animal springs out — or Պույ-պույ himself, curled up
asleep, exactly as the tale has him.

He asks «Ո՞վ է թաքնվել» and gives her four and a half seconds to answer first.

</td>
</tr>
<tr>
<td width="33%" valign="top">

### ⭐ Ձևե՛ր
**Post the shapes**

Nine kinds — circle, square, triangle, star, heart, rectangle, oval, diamond, crescent — six
of them scattered on the sand at a time, each with a hole cut to match.

The wrong hole simply will not take it. It bumps and hops back out, and he does not react at
all.

</td>
<td width="33%" valign="top">

### 🧩 Փազլ
**Finish the picture**

A 3×3 jigsaw with the faintest ghost of the picture underneath — a hint that something goes
here, not which piece.

Pieces have to be carried. Tapping wakes them and nothing more.

</td>
<td width="33%" valign="top">

### 🎨 Նկարի՛ր
**Paint**

Four fat colours, several fingers at once, one button that clears the page and one that puts it
back.

Nothing to get right. It is the game she goes to when she is tired of the others.

</td>
</tr>
</table>

---

## 🧠 What it is actually teaching

**Colours**, **size ordering** and **shapes** — and only one game ever asks her anything.

Instruction-following is a five-year-old skill. At three, *"which one is red?"* produces a
screen where most touches do nothing, and the child hands the tablet back. So the world leans
instead: the sky fills with red balloons and he names the colour once; the tower stands when
the big coconut is at the bottom and falls when it is not.

> A rule she works out by knocking a tower over is hers. A rule she is told is a rule she has
> to remember, and at three she will not.

She is never tested, never wrong, and never waiting — and she is learning the whole time. The
research this rests on, including the parts that contradicted the first design, is in
[docs/toddler-ux.md](docs/toddler-ux.md).

---

## 🎨 Design rules

| | |
|---|---|
| 🚫 **No failure states** | No score, no timer, no lives, no progress. A miss gets «Ոչինչ» — never mind — about one time in three, and nothing else. |
| 🔇 **Silent-safe** | Every audio reward has a visual one. Fully playable with the volume at zero, which on a plane is the normal case. |
| 🖼 **No text for the child** | Navigation is pictures. The Armenian tile names are for the grown-up. |
| 🎯 **Large targets** | 126dp minimum — four times the usual adult minimum, because toddlers have gross motor control and little fine motor control. |
| 🏝 **Saturated subject, muted ground** | The balloons and the coconuts are always the most vivid things on screen. The island never competes. |
| 🏠 **No dead ends** | The same home button, top-left, same place, in all six games. Back on the hub does nothing. |
| 🤫 **Nothing plays itself** | He waves hello and stops. The first move belongs to the child. |

---

## 🔒 What it does **not** do

No internet. No analytics. No crash reporting. No ad SDK. No accounts. No data collection of
any kind.

**We declare no permissions.** The only entry in the merged manifest is a signature-level
permission androidx.core defines against this app's own package so its receivers stay
unexported — it grants nothing and is invisible to the user.

androidx.emoji2's initializer is removed from the merged manifest as well: it does a
font-provider IPC to Play Services on cold start, the app renders no emoji, and *"nothing leaves
the device"* should be true of the whole manifest rather than only of the code we wrote.

Dependencies are AndroidX and Compose, and nothing else.

---

## 🚧 Not built yet

Honest list, so nobody has to go looking:

- **Sound effects.** `munch`, `brush`, `wipe` and the twelve animal noises are silent no-ops.
  A missing clip never crashes — that is what made building the whole app before recording a
  single sound possible.
- **The voice is a pitch-shifted man.** It is intelligible and it is his, but it is not a young
  voice. See [docs/audio.md](docs/audio.md).
- **The Armenian has not been checked by a native speaker.** It should be read aloud once
  before any of it reaches a child.
- **No contrast test.** hashvir enforces 4.5:1 for every subject against every background. This
  app inherited the palette but not the test.
- **Rotating mid-drawing scrambles the painting**, because strokes are stored in raw pixels.

---

## 📦 Install

Grab the APK from [**Releases**](https://github.com/hrach-gevorgyan/puy-puy/releases/latest) and
install it on any device running **Android 7.0 or newer**.

Phone or tablet, portrait or landscape. See [docs/changelog.md](docs/changelog.md) for what is
in each version.

---

## 🔨 Build

```bash
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # unit tests
./gradlew installDebug         # to a connected device
```

Kotlin · Jetpack Compose · minSdk 24 · single Activity · no navigation library.

<details>
<summary><b>Repository layout</b></summary>

```
app/src/main/java/am/puypuy/
  core/       the character, the layout maths, audio, the one event routine
  games/      six games, and the rules they are built on
  ui/         the hub, the island, the home button
docs/         architecture, the games, the research, the art pipeline
tools/        voice generation, tile and icon cutting, words.csv
```

The character is drawn in code, because he has to squash, walk, wave, blink and mirror. The hub
tiles and the launcher icon are illustrations, cut and sized by the scripts in `tools/`. See
[docs/art.md](docs/art.md).

</details>

<details>
<summary><b>Tests</b></summary>

```bash
./gradlew testDebugUnitTest
```

They cover the things that are arithmetic rather than taste, on eight reference screens
including both tablet orientations:

- **Layout** — hub tiles that fit, coconuts that do not overlap, a tower that clears the home
  button, nothing measured off the edge, and a 0×0 screen that still gives a usable layout.
- **Teaching** — what a session actually sounds like: one colour dominates, another is always
  beside it to contrast against, and a turn lasts long enough to be heard.
- **The tower** — smaller holds, bigger topples, a drifting pile goes over, a child's aim is
  good enough, and it can be rebuilt for ever.
- **Clips** — every name the code can say exists, every recorded clip is reachable, and
  `docs/audio.md` documents exactly the clips that exist.

</details>

<details>
<summary><b>Audio</b></summary>

66 Armenian speech clips in `app/src/main/res/raw`, one voice throughout, generated ahead of
time and committed — they are build *inputs*, so the app builds and runs with no network.

```bash
pip install piper-tts && ./tools/get_piper_voice.sh
python tools/gen_voices.py --words tools/words.csv --out app/src/main/res/raw
python tools/sync_audio_doc.py
```

There is no free, keyless Armenian TTS worth the name — `edge-tts`, Google Translate and Meta's
MMS were all tested and none of them speaks Armenian at all. The only free Armenian voice
anywhere is Piper's, and it is a grown man. He is pitched up 25%, slowed 2%, and given back the
consonants the shift smears, and what comes out is a mouse.

[docs/audio.md](docs/audio.md) lists every clip with its Armenian text, for anyone re-recording
the set in a real voice.

</details>

<details>
<summary><b>Releases</b></summary>

GitHub Actions runs the tests on every push and attaches an APK to a Release when a tag is
pushed:

```bash
git tag v1.0.0 && git push origin v1.0.0
```

Signing keys come from repository secrets. Without them the build still succeeds and produces
an unsigned APK.

</details>

---

Built for one three-year-old, and shared in case it helps anyone else raising a small child in
Armenian.

<sub>MIT licensed — see <a href="LICENSE">LICENSE</a>.<br>
Noto Sans Armenian is used under the SIL Open Font License — see
<a href="licenses/">licenses/</a>.</sub>
