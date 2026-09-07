# Toddler UX — the evidence

Researched, cited, and written as rules I can check a build against. Everything here is about
a child of **three to four**, which is the narrowest and least forgiving band in children's
software: old enough to use a tablet fluently, too young for almost every convention adults
assume.

Where a source says something inconvenient for this app, it is recorded as-is.

---

## 1. The finding that changes the most

> "Kids under 6 couldn't understand this feedback; they just made the food to their own
> liking." — [NN/G, *Designing for Kids: Cognitive Considerations*](https://www.nngroup.com/articles/kids-cognition/)

The study watched children use an app where an **animal character gave feedback through facial
expressions and noises**. Under-6s did not read it. Their theory-of-mind is still forming —
they cannot reliably infer what another character thinks or wants.

**This is the single most important line in this document for us**, because the app's design
has repeatedly leaned on exactly that mechanism: the mouse reacts, and his reaction is supposed
to be the feedback. It is not, on its own, feedback a three-year-old receives.

What the same source says works instead:

> "More specific visual and audio cues such as exaggerated facial expressions or saying
> 'Yum, I like carrots' could better help children make the connection."

So the character is not deleted — he is made **unambiguous**. Subtle reaction out; exaggerated
reaction *plus* an explicit spoken label *plus* a change in the world (the fruit vanishes, the
belly visibly grows) in. The rule: **never let the character's mood be the only signal.**

---

## 2. Touch targets and gestures

| Rule | Source |
|---|---|
| **≥ 2cm × 2cm** touch targets for young children — *four times* the 1cm adult minimum | [NN/G, *Design for Kids Based on Their Stage of Physical Development*](https://www.nngroup.com/articles/children-ux-physical-development/) |
| Gross motor skills develop **before** fine motor; design for big arm movements, not precise fingers | same |
| **Tap and swipe are easy** for even the youngest; **precise dragging to a specific location is hard** | same |
| Small controls are a real failure: a 5mm close button "caused frustration" | same |
| Offer **flexible interaction** — let tapping and dragging both accomplish the same thing | same |

On a 420dpi phone, 2cm ≈ **126dp**, which is where this project's floor came from, and the
evidence supports keeping it.

Vatavu et al. collected **2,912 touch records from 89 children aged 3–6** and found that
"small-age children have difficulties acquiring targets accurately and dragging items", with
success rate and speed improving significantly with each year of age
([*Touch interaction for children aged 3 to 6 years*](https://www.sciencedirect.com/science/article/abs/pii/S1071581914001426)).

**Rules for us**
1. Every target ≥126dp, with generous spacing.
2. **Tap is the only required gesture.** Drag is supported everywhere it appears and never
   required — a released drag pays out wherever it lands.
3. No double-tap, no long-press-to-reveal, no pinch, no two-handed anything.
4. No target that moves faster than she can land on it.

---

## 3. What they do when nothing happens

> "When sites didn't respond with expected sound or movement, they attempted repeated
> interactions to trigger these features." — [NN/G, *Children's UX: Usability Issues*](https://www.nngroup.com/articles/childrens-websites-usability-issues/)

They tap again. And again. Then they leave. This is the mechanism behind the app's first
rejection, and it is why **response density** — the share of touches that produce a visible
result — is the metric that matters most.

Same source, also load-bearing:

- Young children **strongly prefer** designs with animation and audio and **actively hunt for
  them**.
- **Real-world spatial metaphors** are "very helpful for pre-readers" — a place, not a menu.
- They **do not use back buttons**. Navigation must be one obvious in-app control.
- Body text for this age wants **14pt**, not the 12pt used for older children.

---

## 4. Animation: relevant, or harmful

This is the finding that argues against "add more juice everywhere".

An eye-tracking study of preschoolers found that **low-relevance animation measurably hurt
comprehension**, pulling gaze away from what mattered, while **high-relevance animation scored
the same as a static page** — no penalty
([*The interference effect of low-relevant animated elements*](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC11651708/)).

And more broadly, under the Cognitive Theory of Multimedia Learning, children's learning "might
be disrupted if software includes too much material, such as unnecessary animation, text,
graphics, or music"
([*Assessing the educational potential of touchscreen apps for preschool children*](https://www.sciencedirect.com/science/article/pii/S2666557322000313)).

**Rule:** animate the thing she touched and the thing that changed because of it. Ambient
decoration may move, but slowly and at low contrast. Sparkles that mean nothing are not
neutral — they cost attention.

---

## 5. Attention

> "two-and-one-half minutes being the lowest attention span estimate for the youngest children"

Design a session as a **two-to-three-minute** loop that is complete in itself and can be left
at any moment with nothing lost. No progress to abandon, no round to finish, no reason to
resume. Four hours on a plane is 80 of these, not one long one.

---

## 6. Mental models: skeuomorphic wins

> "even a 3-year-old girl was able to make a pasta dish by herself: she put pasta in the pot
> by dragging the bag of noodles to the pot" — [NN/G cognition](https://www.nngroup.com/articles/kids-cognition/)

She succeeded because the interface **mimicked something she had watched in a kitchen**. The
guidance is explicit: use real-world objects and actions, avoid abstract symbols, lean on what
she already knows.

For this app that is unusually easy — the tale supplies every metaphor. Fruit is eaten.
Coconuts crack. Balloons pop. Nothing needs to be learned as a convention.

---

## 7. Open-ended play, from the people who do it best

Sago Mini state their principle plainly: open-ended play means "**there are no instructions, no
rules to follow**, and how your child engages with the games is completely up to them"
([Sago Mini, *Letter to Parents*](https://sagomini.com/article/sago-mini-letter-to-parents/)).
Toca Boca's apps are described the same way — "no goals or scoring".

Their release gate is a good one to steal: *does it encourage kids to explore, imagine and
discover? Is it intuitive, beautiful and fun to play?*

**The tension to hold honestly.** This app is meant to be educational, and "no instructions"
seems to fight that. It does not, if the teaching is arranged as **naming what already
happened** rather than as **asking her to do something**. She pops a balloon; it is named red.
No question was asked, nothing was tested, and the word arrived attached to an event she
caused. That is the only shape of teaching that survives all of §1, §3 and §7 at once.

---

## 8. Shipping to Google Play as a children's app

If Play is ever the target
([Families policy](https://support.google.com/googleplay/android-developer/answer/9893335)):

- A **privacy policy is mandatory and non-waivable**, even collecting nothing.
- Target Audience and Content declaration, plus the Data Safety form.
- COPPA and GDPR compliance, including any SDK pulled in.
- Ads must use a certified Families SDK and be non-personalised — we have none, which is the
  easy path.

---

## 9. The checklist

Testable against a build, not a matter of taste.

1. **Response density: 100%.** No coordinate on any screen is inert.
2. **Reaction within one frame of touch-DOWN**, never touch-up.
3. **Every target ≥126dp**, spaced.
4. **Tap suffices everywhere.** No gesture is ever required.
5. **The character's mood is never the only feedback** — §1. Pair it with an explicit label and
   a change in the world.
6. **Nothing is asked.** No instruction is a precondition for anything happening.
7. **Playable in silence.** Removing `res/raw` changes nothing about what can be played.
8. **Every animation is about something she touched.** No decorative motion above low contrast.
9. **Leavable at any second.** No round, no progress, no resume.
10. **One obvious way home**, same place on every screen.

---

## 10. Where this contradicts what I built before

Recorded so it is not re-litigated:

- **"She is the interface"** was half wrong. He can be the *host*, and he must react to
  everything, but his face cannot carry meaning on its own for this age. §1.
- **"Add juice everywhere"** was wrong. Irrelevant motion measurably costs comprehension. §4.
- **"Give each game a purpose"** was implemented as instruction-following, which §7 and §1 both
  reject at this age. The purpose survives only as naming-after-the-fact.
- **126dp** was right, and is now cited rather than asserted.
- **Silent-safe** was right, and §3 strengthens it: audio is *sought* by children this age, so
  it must be there — but never as the only channel.
