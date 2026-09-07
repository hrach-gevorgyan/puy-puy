# The six games — revised logic

Every rule here traces to a finding in [toddler-ux.md](toddler-ux.md). Where a game changes
from what is in the code today, the change and its reason are stated, so nothing gets quietly
re-litigated.

The art is a separate concern ([art.md](art.md)). This document is only the logic, and none of
it depends on how anything looks.

---

## 0. What is true of all six

**Nothing is ever asked.** No game has a wanted item, a target, a round, a correct answer or a
rule that changes. Instruction-following is a five-year-old skill; at three it produces a
screen where most touches do nothing, which is what got the app rejected.

**No game plays itself.** He waves hello and then the screen is hers. Each game used to take
the first turn — pop a balloon, post a shape, place a piece — as a demonstration. It reads as
the app playing with itself, and it takes the first move away from the child.

**Every touch answers, on touch-DOWN.** Each game below carries an exhaustive *what every touch
does* table. That table is the specification: if a coordinate on the screen is not covered by a
row, the game is not finished. Reaction begins within one frame of the finger landing — never
on release, because a three-year-old's finger travels.

**Three channels, always together.** This is the correction that follows from the study where
under-sixes could not read a character's expression as feedback. Every meaningful event fires
all three:

1. **The world changes** — the balloon is gone, the fruit is gone, the belly is bigger.
2. **He reacts, exaggerated** — not a subtle mood; a whole-body response.
3. **He says the plain word** — «կարմիր», «խնձոր» — as a label on what happened, never as a
   question.

Any event with only one of the three is a bug.

**Tap is sufficient everywhere.** Drag is supported wherever it makes sense and required
nowhere. A drag released anywhere pays out exactly as a tap would.

**Silence is a first-class mode.** Removing `res/raw` entirely must not change what is
playable. Audio is additive — and it must exist, because children this age actively hunt for
sound — but it is never the only carrier.

**Animation is about the touched thing.** Ambient motion stays slow and low-contrast.
Decorative sparkle is not free; it costs attention that belongs to the toy.

**Leavable at any instant.** No round to finish, no progress to lose, no state to resume. A
session is two to three minutes and complete in itself.

**One way home**, top-left, ≥126dp, identical on all six screens, and no game may place
anything touchable inside that corner.

---

## 1. Փուչիկներ — balloons

**What it is.** Balloons rise; every one pops.

**The loop.** 8–14 balloons on screen across the full width, rising at 30–60dp/s with a gentle
sway. Every balloon pops on touch-down. A new one enters ~600ms after any pop, so the sky never
empties.

**How it teaches.** The sky runs on colour: balloons arrive in runs of 9–14 with about a fifth
deliberately off-colour, so one colour dominates for a while and there is always another beside
it to tell it apart from. He holds a balloon in that colour, permanently visible, and names it
when the run changes. Popping a match is always named, with a bigger burst; popping anything
else pops just as well and is named every third time. **No colour is ever requested** — a target
that can only add is the one shape of teaching that works at three.

This gives about eight to ten utterances per colour in a session, clustered, instead of the
three scattered ones a flat one-in-four naming rate produced. `ColourRunTest` asserts it.

| Every touch | What happens |
|---|---|
| A balloon | Squash → stretch → burst into shreds **in that balloon's own colour**; he jumps; the colour is sometimes named |
| Two balloons at once | Both pop. Multi-touch is a delight, not an error |
| Empty sky | Three small bubbles rise from the finger, and every nearby balloon leans toward it — a miss still reads as influence |
| Him | He grabs the nearest balloon, lifts a little, drops, feet kicking |
| The balloon in his paw | It pops like any other, he laughs, and he blows up another in the same colour |
| A cloud | It wobbles and drops two raindrops |
| The ground | Balloons bounce off it |

**Silent-safe.** Entirely. Nothing here needs a word.

**Deleted from today's code.** The wanted colour, `PER_ROUND`, the held-balloon-as-a-target, the
`hit.nudge()` non-response (`sway += 0.9f`, which answered a wrong-colour tap with a 2dp drift
and nothing else), and the spoken colour *before* a pop.

---

## 2. Կառուցի՛ր — the tower

**What it is.** Five coconuts of different sizes lie on the sand. She stacks them.

**What it teaches.** Size ordering — putting things in sequence from biggest to smallest. It is
a real preschool skill, it is the ground counting is built on, and a three-year-old can do it
with her hands long before she can talk about it. Nothing is explained and nothing is asked:
she puts a coconut on the pile, and the pile holds or it does not.

**Two rules, neither ever stated anywhere in the game:** a coconut holds if it is smaller than
the one under it, and if it is not dropped too far off the side of it. The first one always
holds, whatever it is. A pile that drifts steadily one way goes over even when every coconut
looked fine on the one below it.

The second rule is what stops the game being solved without looking: **she has to place each
one, not just choose it.** Her aim need not be good — half a radius off still counts — but
dropping everything roughly at the pile is no longer the same as building a tower.

**Toppling is the good part.** Knocking a tower down is why children build them. A bad stack
leans further the longer it stands and then comes down in a heap she can rebuild at once — no
sound of disapproval, no reaction from him but delight. She may also knock a good tower down
whenever she likes, and that is a perfectly reasonable thing to want.

**Help, after she has knocked one over:** the coconut that belongs next starts to nudge; after
a second topple it lifts and grows a ring under it. She cannot end up stuck.

| Every touch | What happens |
|---|---|
| A loose coconut (tap) | Flies to the top of the pile; holds if it is smaller, brings the tower down if it is bigger |
| A loose coconut (drag) | It follows her finger exactly. Dropped near the pile it goes on **where she dropped it**; dropped out on the sand it stays where she put it |
| The pile | It topples, on purpose, in a heap |
| Sand | A puff of grains |

**Sizes and places are arithmetic**, so they live in `Layout.tower` and `LayoutTest` asserts
them: the loose coconuts never overlap, the row uses the width, the finished tower fits on the
screen and never rises into the home button's corner. A short screen gets a shorter tower of
bigger coconuts rather than a taller one of crumbs.

**How it speaks.** Barely: «մեծ» or «փոքր» on about every second coconut that holds — a label
on what she just did, never a request. Finishing the tower is a chime and a burst, no words.

**Silent-safe.** Completely. The tower standing or falling is the whole feedback.

**Replaced Կերակրի՛ր (feeding).** The tale — the coconut, the round belly, the crying — is no
longer played out in any game. It remains what the app is about: the icon, his character, the
hub.

---

## 3. Պահմտոցի — coconuts and animals

**«Ո՞վ է թաքնվել» — and four and a half seconds to answer.** The animal springs out on touch-down
and **stays**; a beat later he asks who was hiding, and she has four and a half seconds to say
it before he does.

This game asked exactly this once and it was cut, because the wait FROZE the screen — nothing
responded for four and a half seconds, which does not read as time to think, it reads as a
crash. The question is back at the owner's instruction and **the freeze is not**: every coconut
stays touchable throughout, she may open another or poke this one, and the only thing the wait
governs is when HE says the word. Opening another coconut moves the question to that one.

**What it is.** Peekaboo. It always was; the quiz was the only thing wrong with it.

**The loop.** A 3×3 grid of coconuts. **Touch-down cracks one open in under 250ms** — shells
split and fall aside, milk droplets, shell chips — and an animal springs out and **stays**.
Open the ninth and all nine animals hop out, mill around for a few seconds while he greets
them, and are herded back for a fresh board.

**An open cell is never spent.** Touch the animal and it does its signature move and its noise.
Touch the empty shell and it clacks shut, ready to open on a **different** animal.

**One coconut holds him**, asleep, curled up — the tale's image. Waking him is the best moment
on the board.

| Every touch | What happens |
|---|---|
| A closed coconut | Cracks open in 180ms; animal springs out; its name plays |
| An open animal | Signature move plus its noise — frog hops, crab pinches, octopus curls |
| An open shell | Clacks shut, reopens later on a different animal |
| Him, asleep in a coconut | He wakes, stretches, waves |
| Him, on the sand | Squash, squeak, and he points at the last coconut opened |
| Between the coconuts | Sand puff |

**How it teaches.** The animal is named as it appears. Twelve animals, each heard in the moment
its picture is on screen and its noise is playing.

**Silent-safe.** The signature move *is* the noise when the volume is off — which is exactly why
each animal needs one.

**Deleted from today's code.** `coconut_guess`, the **4,500ms silent pause** for her to answer
in (4.5× the flow limit; it reads as a crash), and the whole quiz layer.

---

## 4. Ձևե՛ր — shapes

**What it is.** A shape-sorter, the physical toy.

**The loop.** Shapes lie on the sand; shape-holes sit beside them. Push a shape near its hole
and it **snaps in from a generous distance** with a thump and a puff of sand. Five shapes:
circle, square, triangle, star, heart.

**The wrong hole does not punish — it does not accept.** The shape bumps, tips, and hops back
onto the sand, cheerfully. That is a physical fact about the world, not a judgement about her,
and it is the only "no" in the app.

| Every touch | What happens |
|---|---|
| A shape (tap) | It hops in place and he names it |
| A shape (drag) | Follows the finger; near its hole it snaps in; released elsewhere it stays put |
| A shape at the wrong hole | Bumps, tips, hops back out — no sound of failure, no reaction from him |
| A filled hole | The shape pops back out to be used again |
| Him | Squash, squeak; he picks up the nearest shape and holds it out |
| Sand | Puff and a dent |

**How it teaches.** The shape's name lands as it snaps home, and again if she taps it. Shape
vocabulary is the one genuinely three-year-old-appropriate curriculum in the set.

**Silent-safe.** The snap and the bounce-out say everything.

**Session shape.** Five shapes take well under two minutes and the board refills itself.

---

## 5. Փազլ — the picture

**What it is.** A four-to-six piece jigsaw of a picture he is in.

**The loop.** The frame shows a faint ghost of the finished picture. Pieces lie around it.
**Tap a piece and it flies to its own slot** — that is the tap fallback, and it means the game
is completable without a single drag. Drag works too, and snaps from a generous distance.

**Nothing is ever refused.** A piece put down anywhere just stays there, and can be picked up
again. When the last piece lands, the picture **comes alive and moves** for a few seconds
before a new one begins.

| Every touch | What happens |
|---|---|
| A loose piece (tap) | Flies home to its slot and thumps in |
| A loose piece (drag) | Follows the finger; snaps when near its slot; stays put anywhere else |
| A placed piece | Wobbles; can be pulled out again |
| An empty slot | The ghost image under it brightens for a moment |
| Him | Squash, squeak; he points at the piece nearest its home |
| Background | Sand puff |

**How it teaches.** Part-to-whole, which is squarely four-year-old work — and the reason this
replaced the alphabet, which is a five-to-six-year-old skill and would have been another test
in a game's clothes.

**Silent-safe.** Entirely.

---

## 6. Նկարի՛ր — painting

**What it is.** The one game that already worked, and the reference implementation for every
other: 100% of the screen responds, at 0ms, with no instruction.

**Kept exactly.** Full-screen canvas, a stroke on touch-down, historical touch samples replayed
so fast scribbles stay curves, multi-touch drawing.

**Four changes.**

1. **The palette moves off the bottom edge**, where a resting wrist lands, to a vertical strip
   down the right inside the safe area, with a dead margin at the bottom.
2. **Undo** — one 126dp button beside clear. Clear is the only irreversible thing in the app.
3. **The brush leaves a few soft particles** in the current colour, so a single dot is also an
   event.
4. **He is in the drawing.** He walks along the bottom, touchable, gets paint on his paws and
   tracks coloured footprints; a stroke across him leaves a stripe he shakes off.

Choosing a colour tints his whole body for a moment — the colour name **shown** as well as
said.

| Every touch | What happens |
|---|---|
| Anywhere on the page | A stroke, immediately |
| A swatch | It grows, gains a ring, he tints to that colour, and he names it |
| Undo | The last stroke lifts off |
| Clear | A 600ms wipe; he chases it |
| Him | Squash, squeak, and he shakes off any paint on his back |

**Trauma is zero here** — no screen shake ever wraps this canvas, because strokes are stored in
raw pixels and a shaken canvas would record the offset.

---

## 7. What this costs, honestly

Set against the current code, this deletes: two `wanted` fields, `PER_ROUND`, `ask()`,
`RepeatPrompt`, `GUESS_PAUSE_MS`, `coconut_guess`, the eat-radius gate, `goHome()`, and the
`feed_want_*` clips.

It adds: a tap path for every drag, an exhaustive touch table per screen, tears and the coconut
the tower and its physics in Կառուցի՛ր, animal idles and signature moves in Պահմտոցի.

The clips it strands (`feed_want_*`, `coconut_guess`) are seven files and about 40KB. They cost
nothing to regenerate if a game ever wants them back — but on the evidence, none should.
