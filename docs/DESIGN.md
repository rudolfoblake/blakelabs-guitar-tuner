# Design system

Blake Labs Guitar Tuner is designed like an instrument, not an ad container.

The UI direction is deliberately high-contrast, quiet and fast to parse while somebody is physically holding a guitar. Decorative elements are allowed only when they reinforce state, brand or hierarchy.

## Principles

1. **The note wins.** The detected/target note is the largest piece of information on screen.
2. **Tuning state is redundant on purpose.** Needle position, cents, color and explicit text all communicate flat / in tune / sharp.
3. **Blake lime means something.** `#A7F20A` is reserved for the brand, active controls and successful tuning lock.
4. **OLED-first black.** The base background is true black; raised surfaces are only slightly lighter.
5. **One-hand readability.** Primary interactions use large targets, and the layout remains scrollable on short displays.
6. **No fake premium.** No ornamental gradients that compete with the tuner, no tiny gray-on-gray labels, no mystery gestures.
7. **Privacy is product design.** The interface states that audio stays on-device because the architecture actually guarantees it: the app has no Internet permission.

## Brand mark

The canonical Android asset is:

```text
app/src/main/res/drawable-nodpi/blake_labs_logo_official.png
```

It is derived directly from the supplied official Blake Labs artwork with the full-square black breathing room preserved. Compose uses that PNG directly. Android launcher and Android 12+ splash go through dedicated inset wrappers so platform masks can scale the mark without clipping it. Do not replace it with a generated, hand-reconstructed or tightly cropped lookalike.

## Core tokens

| Token | Value | Use |
| --- | --- | --- |
| Background | `#000000` | App canvas / OLED base |
| Surface | `#090B09` | Primary cards |
| Raised surface | `#101310` | Selected segments / controls |
| Blake lime | `#A7F20A` | Brand, focus, in-tune state |
| Text | `#F5F7F2` | Primary type |
| Muted text | `#9AA398` | Secondary type |
| Warning | `#FFC857` | Flat / sharp guidance |

## Main-screen hierarchy

```text
Brand / mic / settings
        ↓
Tuning context
        ↓
      NOTE
    frequency
        ↓
 tuning gauge
 cents + state
        ↓
signal / lock quality
        ↓
manual string controls
        ↓
tuning presets
        ↓
mode selector
```

## Motion and feedback

- The needle uses a damped spring: responsive, but not jittery.
- Entering the ±3-cent lock window triggers one haptic event when haptics are enabled.
- The branded launch screen is intentionally brief; it should identify the product, not become a toll booth.

## Product promise

**Free. Offline. No ads. No trackers. No nonsense.**

If a future design change requires violating that sentence, the design change is wrong.
