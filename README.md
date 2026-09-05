# BlokHead

BlokHead is a native Android port of [Blokout](https://jlehtinen.net/blokout/), itself an open source clone
of the classic game Blockout. 

The game mechanics started as a direct port but have been modified to make the game more customizable.

<img src="docs/screenshot-menu.png" alt="BlokHead's main menu: title, Start Game, High Scores, and Settings" width="200" /> <img src="docs/screenshot-gameplay.png" alt="BlokHead mid-game: a partially built stack of colored cubes in the 3D well, with the move d-pad and rotate cluster below" width="200" /> <img src="docs/screenshot-settings.png" alt="BlokHead's settings screen: diagonal d-pad corners, left-handed mode, sound, starting difficulty, well size, button position and opacity, and block set" width="200" /> <img src="docs/screenshot-pause.png" alt="BlokHead's pause screen with its Menu button pressed, showing a Quit to Menu confirmation dialog" width="200" />

## Features

- Blocks fall and move in 3 dimensions and can be rotated around the X, Y, and Z axis.
- Choose between three piece sets - flat, extended, and the full original set from Blokout
- On screen buttons, gesture controls, and support for Bluetooth and USB controllers
- Classic-style layer flash on layer clear
- Tap the top section of the screen to pause/exit the game
- Persistent high score table
- Sound effects!
- Many customizable settings - grid size, well depth, left hand mode, movable controls

## Requirements

- Android Studio (recent stable) or a JDK-configured Gradle.
- minSdk 27 / targetSdk 37.
- No NDK, no native code, no network access — the whole game runs as plain Kotlin + OpenGL ES 2.0
  via `GLSurfaceView`.

## Building & testing

```
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # run the unit tests
```

The game-logic port (`game/`) is pure Kotlin with no Android dependency, so its unit tests run on
the JVM without an emulator — that's where the well/collision/scoring behavior is verified against
the original's logic as it's translated over.

This project targets a GitHub release only; there's no Play Store listing.

## Architecture

- `game/` — the direct port: well/grid state, piece forms, rotation, collision, scoring, and the
  `GameEngine` state machine, all rendering-free and independently unit-tested against the
  original's logic (`Tube`, `Block`, `Collision`, `Form`, `GameEngine`).
- `render/` — the `GLSurfaceView`-based OpenGL ES 2.0 renderer (`BlokoutRenderer`,
  `BlokoutSurfaceView`), including the perspective/grid-spacing math (`Geometry`) that keeps the
  well's wall lines evenly spaced from front to back regardless of well depth.
- `ui/` — Jetpack Compose screens and overlays layered over the GL surface: the main menu, in-game
  HUD, pause/game-over/high-score overlays, the touch controls (`GameControls`), and `Settings`.
- `audio/` — `SfxPlayer`, a `SoundPool` wrapper for the synthesized sound effects.
- `data/` — DataStore-backed persistence for settings (`SettingsStore`) and the high-score table
  (`HighScoreStore`).

## Attribution

- Game design and original implementation: Copyright (C) 1998-1999 Johannes Lehtinen and Petri
  Salmi, from [jlehtine/blokout](https://github.com/jlehtine/blokout).
- Ported to Kotlin/Android, with a rebuilt touch UI and OpenGL ES 2.0 renderer, by rm.
  
## License

GNU General Public License v3 (or later) — see [LICENSE](LICENSE), matching the upstream project.
