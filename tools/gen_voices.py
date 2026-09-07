#!/usr/bin/env python3
"""
Generate Armenian voice clips for the Puypuy app.

Reads words.csv (key,text) and writes one OGG per row into the output
directory, named <key>.ogg so it drops straight into res/raw/.

Every clip is Պույ-պույ. She is a mouse, so the voice is pitched up and slowed
slightly (see MOUSE_PITCH / MOUSE_TEMPO) — raising pitch and formants together
is what makes a voice read as a small animal rather than as a man on helium.

Usage:
    pip install piper-tts
    python gen_voices.py                    # piper, local, no key      (default)
    python gen_voices.py --engine azure     # needs AZURE_SPEECH_KEY + AZURE_SPEECH_REGION
    python gen_voices.py --engine edge      # Microsoft Edge read-aloud
    python gen_voices.py --force            # regenerate everything

Existing files are skipped, so adding a word to words.csv and re-running
only generates the new one.

Engine notes, all verified rather than assumed:
  piper  - the only free, keyless, offline Armenian voice there is. One speaker,
           hy_AM-gor-medium, male, which the mouse shift covers. Download it with
           tools/get_piper_voice.sh. Dataset is GPL-2.0; see docs/audio.md.
  azure  - hy-AM-AnahitNeural, the warm female voice this app would rather have.
           Free tier is 500k chars/month but needs an account.
  edge   - kept because it costs nothing to keep, but Microsoft's read-aloud
           endpoint currently serves NO hy-AM voice at all, so this will fail.
"""

import argparse
import asyncio
import csv
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

VOICE = "hy-AM-AnahitNeural"   # warmer than HaykNeural for a kids' app
RATE = "-15%"                  # slower: easier for a toddler to follow
PITCH = "+8%"                  # slightly brighter (Azure SSML takes a percentage)

# edge-tts only accepts pitch as an absolute Hz offset. Anahit's baseline sits
# around 190Hz, so +8% is roughly +15Hz.
EDGE_PITCH = "+15Hz"

PIPER_MODEL = Path(__file__).parent / "voices" / "hy_AM-gor-medium.onnx"

# The mouse. Pitch and formants go up together, then the clip is stretched back
# out so nothing is rushed. Chosen by ear against single words, not sentences:
# short words like "խնձոր" are where a shift this size either holds or falls apart.
MOUSE_PITCH = 1.2489           # +24.9%  (28.75% was shrill; 3% back down)
MOUSE_TEMPO = 0.9785           # 2.15% slower: 5% dragged, 3% of it given back

# The peak ceiling, as a LINEAR amplitude because that is
# what alimiter takes — written as "-1.5dB" it is ignored
# silently. `level=disabled` matters just as much: alimiter auto-levels by default, which
# normalises every clip back up to full scale, undoes the ceiling, and gives each one a
# different boost — which is where the spread between clips was coming from.
PEAK_CEILING = 0.841

# Padding after the trim. Trailing silence costs nothing — only *leading*
# silence makes a tap feel laggy — and without it the end trim bites into the
# last consonant, which is audible and awful on a word like "կոկոս".
TAIL_PAD_S = 0.18

VALID_KEY = re.compile(r"^[a-z][a-z0-9_]*$")


def load_words(path: Path):
    rows = []
    with path.open(encoding="utf-8") as f:
        for i, row in enumerate(csv.reader(f), start=1):
            if not row or row[0].startswith("#"):
                continue
            if len(row) < 2:
                sys.exit(f"{path}:{i}: expected 'key,text', got {row!r}")
            key, text = row[0].strip(), row[1].strip()
            if not VALID_KEY.match(key):
                sys.exit(
                    f"{path}:{i}: invalid key {key!r} — Android res/raw names "
                    "must be lowercase letters, digits and underscores, "
                    "starting with a letter"
                )
            rows.append((key, text))
    keys = [k for k, _ in rows]
    dupes = {k for k in keys if keys.count(k) > 1}
    if dupes:
        sys.exit(f"duplicate keys in {path}: {', '.join(sorted(dupes))}")
    return rows


async def synth_edge(text: str, out_mp3: Path):
    import edge_tts
    communicate = edge_tts.Communicate(text, VOICE, rate=RATE, pitch=EDGE_PITCH)
    await communicate.save(str(out_mp3))


def synth_azure(text: str, out_mp3: Path):
    import requests

    key = os.environ.get("AZURE_SPEECH_KEY")
    region = os.environ.get("AZURE_SPEECH_REGION")
    if not key or not region:
        sys.exit("set AZURE_SPEECH_KEY and AZURE_SPEECH_REGION")

    ssml = f"""<speak version='1.0' xml:lang='hy-AM'>
  <voice name='{VOICE}'>
    <prosody rate='{RATE}' pitch='{PITCH}'>{text}</prosody>
  </voice>
</speak>"""

    r = requests.post(
        f"https://{region}.tts.speech.microsoft.com/cognitiveservices/v1",
        headers={
            "Ocp-Apim-Subscription-Key": key,
            "Content-Type": "application/ssml+xml",
            "X-Microsoft-OutputFormat": "audio-24khz-96kbitrate-mono-mp3",
            "User-Agent": "puypuy-gen",
        },
        data=ssml.encode("utf-8"),
        timeout=30,
    )
    r.raise_for_status()
    out_mp3.write_bytes(r.content)


def synth_piper(text: str, out_wav: Path):
    import wave

    voice = _piper_voice()
    with wave.open(str(out_wav), "wb") as w:
        voice.synthesize_wav(text, w)


_PIPER_CACHE = {}


def _piper_voice():
    """Loading the model takes a second or two, so do it once for the whole run."""
    if "voice" not in _PIPER_CACHE:
        from piper import PiperVoice

        if not PIPER_MODEL.exists():
            sys.exit(
                f"piper voice not found at {PIPER_MODEL} — "
                "run tools/get_piper_voice.sh (or see docs/audio.md)"
            )
        _PIPER_CACHE["voice"] = PiperVoice.load(
            str(PIPER_MODEL), config_path=str(PIPER_MODEL) + ".json"
        )
    return _PIPER_CACHE["voice"]


def to_ogg(src: Path, dst_ogg: Path, mouse: bool):
    """Trim, make her a mouse, normalise loudness, pad the tail, encode."""
    chain = [
        # Leading silence only. This is the one that makes taps feel laggy.
        "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.05",
    ]
    if mouse:
        rate = int(22050 * MOUSE_PITCH)
        tempo = round(1.0 / MOUSE_PITCH * MOUSE_TEMPO, 4)
        chain += [f"asetrate={rate}", "aresample=22050", f"atempo={tempo}"]
    if mouse:
        # Clarity, which the shift costs and which a three-year-old needs most.
        # Raising pitch and formants together thins the 2-4kHz band that carries
        # consonants, so a word like "խնձոր" arrives as a shape without edges.
        # These two are deliberately STATIC: a de-esser and a compressor were
        # tried here and smeared short words badly enough to be unintelligible,
        # because anything that rides the level has nothing to ride on in a
        # half-second clip. Nothing dynamic goes in this chain.
        chain += [
            "highpass=f=150",                        # mud left over from a male source
            "equalizer=f=3000:t=q:w=1.4:g=2.5",      # consonants back
        ]
    # Loudness: one loudnorm pass, and a peak ceiling after it.
    #
    # Three "better" schemes were measured against this one and all three were worse. Two-pass
    # loudnorm, feeding the first pass's numbers back, gave 11dB of spread across the set:
    # EBU R128 is GATED, and on a clip of one word it discards most of the material and returns
    # a figure that moves several dB between similar recordings. Matching on RMS instead gave
    # 5.7dB — RMS is stable on short clips but it is not perceptual, so clips of different
    # spectral content end up at the same RMS and different loudness. One plain pass gives
    # 2.3dB, and that is what ships.
    #
    # The ceiling IS worth keeping: without it clips came out peaking at 0 dBFS. `level=disabled`
    # is load-bearing — alimiter auto-levels by default, which normalises every clip back up to
    # full scale and undoes the thing it was added for.
    chain += [
        "loudnorm=I=-16:TP=-1.5:LRA=11",
        f"alimiter=limit={PEAK_CEILING}:level=disabled",
        f"apad=pad_dur={TAIL_PAD_S}",
    ]

    subprocess.run(
        [
            "ffmpeg", "-y", "-loglevel", "error",
            "-i", str(src),
            "-af", ",".join(chain),
            "-c:a", "libvorbis", "-q:a", "4", "-ar", "24000", "-ac", "1",
            str(dst_ogg),
        ],
        check=True,
    )


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--words", type=Path, default=Path("words.csv"))
    ap.add_argument("--out", type=Path, default=Path("app/src/main/res/raw"))
    ap.add_argument("--engine", choices=["piper", "azure", "edge"], default="piper")
    ap.add_argument("--force", action="store_true")
    args = ap.parse_args()

    if not shutil.which("ffmpeg"):
        sys.exit("ffmpeg not found — install it (apt install ffmpeg / brew install ffmpeg)")

    rows = load_words(args.words)
    args.out.mkdir(parents=True, exist_ok=True)

    made = skipped = 0
    with tempfile.TemporaryDirectory() as tmp:
        for key, text in rows:
            dst = args.out / f"{key}.ogg"
            if dst.exists() and not args.force:
                skipped += 1
                continue

            if args.engine == "piper":
                raw = Path(tmp) / f"{key}.wav"
                synth_piper(text, raw)
            else:
                raw = Path(tmp) / f"{key}.mp3"
                if args.engine == "edge":
                    asyncio.run(synth_edge(text, raw))
                else:
                    synth_azure(text, raw)

            # Only Piper needs the mouse shift. Anahit is already a character
            # voice and shifting her as well makes her unintelligible.
            to_ogg(raw, dst, mouse=args.engine == "piper")
            print(f"  {key:<16} {text}")
            made += 1

    print(f"\n{made} generated, {skipped} already present -> {args.out}")


if __name__ == "__main__":
    main()
