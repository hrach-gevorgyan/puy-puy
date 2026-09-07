#!/usr/bin/env python3
"""
Turn the generated tile artwork into the hub's drawables.

    python tools/make_tiles.py

Reads tools/art/tile_*.webp and writes app/src/main/res/drawable-xxhdpi/tile_*.webp.

Two things are done to each image, both of which are easy to miss and impossible
to unsee once noticed:

  1. The generator signs almost every image with a small white four-pointed
     sparkle in the bottom-right corner. It is not in the brief, it is not in
     the app, and at tile size it reads as a scratch on the screen.
  2. The art is square already, so nothing is cropped — the six share one sand
     and lagoon ground, and cropping them differently would break the illusion
     that the hub is one island seen through six windows.
"""

from pathlib import Path

from PIL import Image, ImageFilter

SRC = Path("tools/art")
OUT = Path("app/src/main/res/drawable-xxhdpi")
# A tile card is 140dp on a 411dp phone and 280dp on a tablet held upright, so at 3x and 2x
# the biggest anyone asks for is ~560px. 324 was sized for the phone alone and went soft on
# everything larger — the same design has to hold up on a tablet.
SIZE = 576
NAMES = ["balloons", "stack", "coconuts", "shapes", "puzzle", "paint"]


def desparkle(im: Image.Image) -> Image.Image:
    """Erase the small bright mark the generator leaves in the corner.

    A median blur of the bottom-right corner removes anything small; the blurred
    version is then pasted back ONLY where the original is brighter than it. The
    sparkle is small and bright, so it goes. The rainbow stripe on the painting
    tile is large and no brighter than its surroundings, so it stays.
    """
    w, h = im.size
    box = (int(w * 0.78), int(h * 0.78), w, h)
    corner = im.crop(box)
    smooth = corner.filter(ImageFilter.MedianFilter(size=15))
    cp, sp = corner.load(), smooth.load()
    cw, ch = corner.size
    for y in range(ch):
        for x in range(cw):
            c, s = cp[x, y], sp[x, y]
            if sum(c) - sum(s) > 24:
                cp[x, y] = s
    im.paste(corner, box)
    return im


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name in NAMES:
        src = SRC / f"tile_{name}.webp"
        im = desparkle(Image.open(src).convert("RGB"))
        im = im.resize((SIZE, SIZE), Image.LANCZOS)
        dst = OUT / f"tile_{name}.webp"
        im.save(dst, "WEBP", quality=92, method=6)
        print(f"{dst.name}  {dst.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
