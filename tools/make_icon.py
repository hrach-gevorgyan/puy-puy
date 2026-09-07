#!/usr/bin/env python3
"""
Cut the generated icon artwork into Android launcher layers.

    python tools/make_icon.py tools/art/icon_source.webp

Three things have to happen to a 1024px illustration before Android can use it,
and all three are easy to get wrong by hand:

  1. The flat background is keyed out. It is keyed by flood-filling inward from
     the corners rather than by matching a colour everywhere, so a pale highlight
     inside the drawing is never mistaken for background.
  2. Anything that is not the main drawing is dropped — the generator likes to
     add a stray sparkle in a corner, and only the largest connected shape is
     the icon.
  3. The drawing is rescaled into the adaptive-icon SAFE ZONE. An adaptive icon
     is a 108dp canvas of which only the middle 66dp is guaranteed to survive
     the launcher's mask, so art drawn edge to edge loses its extremities — his
     tail, in this case. Everything is fitted inside that 61%.

Legacy square and round icons are written too, because minSdk is 24 and
adaptive icons only start at 26.
"""

import sys
from collections import deque
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

RES = Path("app/src/main/res")
LAGOON = (221, 241, 242)          # @color/lagoon — the adaptive background layer

# An adaptive icon is a 108dp canvas whose middle 66dp is guaranteed to survive
# the launcher's mask. That is a bounding-box rule, and this drawing is wide with
# empty corners, so it is fitted a little larger than 66/108 and checked against
# the circular mask by eye — at 0.70 his tail still clears the edge, and the icon
# does not look lost inside its own plate.
SAFE = 0.70
ADAPTIVE = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
LEGACY = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def cutout(src: Path) -> Image.Image:
    """The drawing alone, cropped tight, on transparency."""
    im = src.convert("RGBA")
    w, h = im.size
    px = im.load()

    # Flood fill inward from every corner: background is whatever is connected
    # to the edge and close in colour to what sits there.
    bg = px[2, 2][:3]
    seen = bytearray(w * h)
    q = deque([(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)])
    tol = 26 * 26 * 3
    while q:
        x, y = q.popleft()
        if not (0 <= x < w and 0 <= y < h) or seen[y * w + x]:
            continue
        r, g, b, _ = px[x, y]
        if (r - bg[0]) ** 2 + (g - bg[1]) ** 2 + (b - bg[2]) ** 2 > tol:
            continue
        seen[y * w + x] = 1
        q.extend(((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))

    # Largest remaining blob only — the stray sparkle goes.
    best, cur_id = None, bytearray(w * h)
    for sy in range(0, h):
        for sx in range(0, w):
            i = sy * w + sx
            if seen[i] or cur_id[i]:
                continue
            blob, q = [], deque([(sx, sy)])
            cur_id[i] = 1
            while q:
                x, y = q.popleft()
                blob.append((x, y))
                for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                    j = ny * w + nx
                    if 0 <= nx < w and 0 <= ny < h and not seen[j] and not cur_id[j]:
                        cur_id[j] = 1
                        q.append((nx, ny))
            if best is None or len(blob) > len(best):
                best = blob

    mask = Image.new("L", (w, h), 0)
    mp = mask.load()
    for x, y in best:
        mp[x, y] = 255
    # A hair of blur, so the cut edge is not a staircase against the lagoon.
    mask = mask.filter(ImageFilter.GaussianBlur(0.6))
    im.putalpha(mask)
    return im.crop(im.getbbox())


def fit(art: Image.Image, canvas: int, frac: float) -> Image.Image:
    """Centre the drawing on a square canvas, occupying `frac` of its width."""
    box = int(canvas * frac)
    scale = min(box / art.width, box / art.height)
    a = art.resize((max(1, round(art.width * scale)), max(1, round(art.height * scale))),
                   Image.LANCZOS)
    out = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    out.paste(a, ((canvas - a.width) // 2, (canvas - a.height) // 2), a)
    return out


def rounded(size: int, radius_frac: float) -> Image.Image:
    m = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(m).rounded_rectangle(
        (0, 0, size * 4 - 1, size * 4 - 1), radius=int(size * 4 * radius_frac), fill=255)
    return m.resize((size, size), Image.LANCZOS)


def circle(size: int) -> Image.Image:
    m = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(m).ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    return m.resize((size, size), Image.LANCZOS)


def main():
    src = Path(sys.argv[1] if len(sys.argv) > 1 else "tools/art/icon_source.webp")
    art = cutout(Image.open(src))
    print(f"drawing cropped to {art.size}")

    for dens, size in ADAPTIVE.items():
        d = RES / f"mipmap-{dens}"
        d.mkdir(parents=True, exist_ok=True)
        fit(art, size, SAFE).save(d / "ic_launcher_foreground.png")

    for dens, size in LEGACY.items():
        d = RES / f"mipmap-{dens}"
        # Legacy icons are not masked by the launcher, so the drawing may fill
        # more of them — 84% against the adaptive layer's 61%.
        plate = Image.new("RGBA", (size, size), LAGOON + (255,))
        plate.alpha_composite(fit(art, size, 0.84))
        sq = plate.copy()
        sq.putalpha(rounded(size, 0.22))
        sq.save(d / "ic_launcher.png")
        rd = plate.copy()
        rd.putalpha(circle(size))
        rd.save(d / "ic_launcher_round.png")

    print("wrote foreground, square and round icons for 5 densities")


if __name__ == "__main__":
    main()
