# Contributing

This is a personal project built for one three-year-old, but the rules are written down so
anyone — including a future me, and including an assistant — can work on it without breaking
what matters.

**Read [CLAUDE.md](../CLAUDE.md) first.** Part 2 is not style guidance; violating one of those
rules is a bug.

## Before you change anything

| If you are changing… | Read |
|---|---|
| how the app is put together | [docs/architecture.md](architecture.md) |
| anything he says | [docs/audio.md](audio.md) |
| the rules themselves | [CLAUDE.md](../CLAUDE.md) |

## The checks

```bash
./gradlew assembleDebug      # must pass
./gradlew testDebugUnitTest  # must pass
./gradlew installDebug       # and then you must actually look at it
```

`LayoutTest` asserts the layout arithmetic against eight reference screen sizes, and `ClipsTest`
asserts that every clip the code can ask for exists and that every clip that ships is reachable.
Between them they would have caught four of the six blockers the audit found, so **add to them
before reaching for a device**: arithmetic is cheaper to assert than to observe.

They do not, however, run the app. **A green build and green tests are still not verification**:
this app is animation, touch and sound. Do not report a change as working without looking at it.

## Adding a game

1. Implement `MiniGame` in `games/`.
2. Add one line to `games/Games.kt`.
3. Add a tile drawable and an Armenian `game_*` string.
4. Add its opening line to `words.csv` and regenerate.

The hub sizes its grid to the registry. Nothing else needs touching — if it does, something has
leaked out of `GameScope`.

## Adding something he says

1. Add a `key,text` row to `tools/words.csv`.
2. `python tools/gen_voices.py --words tools/words.csv --out app/src/main/res/raw`
   — existing clips are skipped, so only the new one is generated.
3. Regenerate docs/audio.md's tables if you have changed the set meaningfully.

Keep it rare. The temptation is always to have her say more; the failure mode is a parent
turning the volume off, which costs every word in the app at once.

## Armenian

The text in `words.csv` was written by an assistant and **has not been checked by a native
speaker**. If you speak Armenian, that is the single most valuable contribution available:
read each line aloud and fix what a three-year-old would not say.

## Commits

Present tense, one concern each, and say *why* when the why is not obvious from the diff. The
comments in this codebase explain reasoning rather than mechanics; commit messages should too.
