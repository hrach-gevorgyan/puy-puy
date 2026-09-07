#!/usr/bin/env python3
"""
Rewrite docs/audio.md's clip tables from tools/words.csv.

    python tools/sync_audio_doc.py

words.csv is the single source of truth for what Պույ-պույ can say. docs/audio.md is the readable
version of it, for anyone re-recording the set in a real voice — and it had drifted badly:
eighteen clips listed that no longer existed and seventeen that did but were undocumented.
Generating it removes the whole class of problem, and `ClipsTest` fails if the two disagree.

The section headings come from the `# --- Title` comments in words.csv, so the grouping is
edited in one place too.
"""

import re
from pathlib import Path

ROOT = next(p for p in [Path(__file__).resolve().parent.parent] if (p / "tools/words.csv").exists())
WORDS = ROOT / "tools/words.csv"
DOC = ROOT / "docs/audio.md"

BANNER = (
    "<!-- Generated from tools/words.csv by tools/sync_audio_doc.py.\n"
    "     Edit words.csv, run the script, regenerate the clips. ClipsTest fails on drift. -->"
)


def groups():
    out, current = [], None
    for line in WORDS.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        heading = re.match(r"^#\s*---\s*(.+?)\s*$", line)
        if heading:
            current = (heading.group(1), [])
            out.append(current)
        elif not line.startswith("#"):
            key, text = (x.strip() for x in line.split(",", 1))
            if current is None:
                current = ("Clips", [])
                out.append(current)
            current[1].append((key, text))
    return [(title, rows) for title, rows in out if rows]


def tables() -> str:
    parts = [BANNER, ""]
    for title, rows in groups():
        parts += [f"## {title}", "", "| File | Says |", "|---|---|"]
        parts += [f"| `{key}` | {text} |" for key, text in rows]
        parts.append("")
    return "\n".join(parts)


def main():
    doc = DOC.read_text(encoding="utf-8")
    start = doc.index("<!-- Generated from") if "<!-- Generated from" in doc else doc.index("## ", doc.index("---\n\n"))
    end = doc.index("## Sound effects") if "## Sound effects" in doc else len(doc)
    DOC.write_text(doc[:start] + tables() + "\n" + doc[end:], encoding="utf-8", newline="\n")
    print(f"docs/audio.md: {sum(len(r) for _, r in groups())} clips in {len(groups())} sections")


if __name__ == "__main__":
    main()
