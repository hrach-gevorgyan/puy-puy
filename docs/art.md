# The art

Two kinds of picture live in this app, and the rule for which is which is simple:

| | drawn where | why |
|---|---|---|
| Hub tiles, launcher icon | generated images, cut by `tools/` into WebP | still pictures, and hand-authored bezier paths never read like an illustrated children's book |
| Պույ-պույ, the fruit, the animals, the island, the balloons | Compose paths in Kotlin | they deform — squash, stretch, walk, wave, fill out, mirror — and a still image being moved around reads as a photograph, not a character |

Both must look like one hand drew them. That is what the shared style rules below are for.

## The style

Everything in this app, drawn or generated, follows the same five rules:

- **Flat colour.** One fill per surface, plus at most one soft shadow tone. No gradients, no
  airbrush, no gloss, no 3D.
- **One navy outline of even weight** — `#3A4454` — around every shape.
- **Thick, confident, rounded forms.** Nothing spindly, nothing sharp.
- **Saturated subject, muted ground.** The thing she is meant to touch is always the most vivid
  thing on screen. If a decoration out-competes the toy, that is a bug.
- **No text anywhere in a picture.**

In Kotlin the outline is not stroked. Every shape is drawn **twice** — once enlarged in navy,
once filled on top — so overlapping shapes fuse into a single silhouette with no seam. Stroking
each shape separately is what makes a character read as a snowman of parts.

## The palette

```
sand        #FDF3E0    lagoon      #DDF1F2    sea      #5FBDBF
leaf        #5C9E52    trunk       #A97B4F    sun      #FFD166
coconut     #8A5A3B    coconut dk  #6B4429    flesh    #FBF3E4
mouse       #B9B3C7    ears/paws   #F2C4C9    ink      #3A4454
```

The tile artwork was generated against these exact values, and the hub paints its own ground
from the two sampled out of the finished images (`#DCF1F3` lagoon over `#F7F0DE` sand), so the
grid reads as six windows onto one island.

## Generating new tiles or a new icon

Write the prompt against the style rules above, and always state:

- square, 1:1, 1024x1024
- one flat uniform background colour, no scene, no horizon, no props beyond those named
- the subject centred, filling about 70%, generous margin
- no text, no numbers, no frame, no border, no sparkles
- must read at 120x120 pixels
- the palette values, verbatim

For the character, also fix his design in the prompt every time: round grey-lilac body
`#B9B3C7`, pale pink inner ears and paws `#F2C4C9`, navy eyes and outline `#3A4454`, long curled
tail, small round ears, pale belly. *Never brown, never realistic, never a rat.*

Then:

```bash
python tools/make_tiles.py       # tools/art/tile_*.webp -> res/drawable-xxhdpi/*.webp
python tools/make_icon.py        # tools/art/icon_source.webp -> mipmap-*/
```

Both scripts do three things by hand that are easy to forget:

1. **Key out the flat background** by flooding inward from the corners, not by matching a colour
   everywhere — so a pale highlight inside the drawing is never mistaken for background.
2. **Drop everything that is not the subject.** Image generators like to sign a corner with a
   small white sparkle, and to add a soft ground shadow whatever the prompt says. Only the
   largest connected shape survives.
3. **Size for the largest screen that asks.** A hub tile card is 140dp on a phone and 280dp on a
   tablet held upright, so at 3x and 2x the biggest request is ~560px; tiles ship at 576px. The
   launcher icon is fitted to the adaptive-icon safe area, which is the middle 66 of 108dp —
   art drawn edge to edge loses its extremities to the mask.

## The source images

`tools/art/` holds what the generator produced, and the scripts read from there. They are
**kept as WebP at quality 94** rather than as the original PNGs: at 1024px the difference is
invisible and it is the difference between 5.5 MB and 0.4 MB in a repository whose entire
build output is 9 MB.

## Sizes that matter

| | value | why |
|---|---|---|
| touch target | ≥ 126dp | hashvir's floor, four times the adult minimum |
| hub tile art | 576px WebP | covers a tablet at 2x without upscaling |
| adaptive icon | 108dp canvas, art within 0.70 | the mask only guarantees the middle 66dp |
| APK budget | ~40 MB | currently around 9 MB |
