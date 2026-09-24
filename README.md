# BlokHead

BlokHead is an Android port of [Blokout](https://jlehtinen.net/blokout/), the open source 3D falling block game, based on the classic Blockout.
It uses [jlehtine/blokout](https://github.com/jlehtine/blokout) as an upstream source, and the game code was ported over to Kotlin from there.

The game mechanics started out as a direct port of Blokout, so it plays the way the original does, but they've since been tweaked to make the game more customizable and quicker to play. The graphics have been redone in OpenGL ES and the UI has been totally redone for a touch screen. You can play with on screen buttons, with gestures, or with an external controller.

It hasn't been tested much on different shaped devices, so there may be layout problems on some of them. If you run into one, please report it here or on the discord channel below.

Requires Android 8.1 or higher.

Questions, bug reports, suggestions? Check out the #blokhead channel **[on my discord](https://discord.gg/9Wun47jGC6)**.

Enjoy!
Dan

<img src="docs/screenshot-menu.png" alt="BlokHead's main menu: title, Start Game, High Scores, Settings and About" width="180" /> <img src="docs/screenshot-gameplay.png" alt="BlokHead mid-game: a partially built stack of colored cubes in the 3D well, with the move d-pad and rotate cluster below" width="180" /> <img src="docs/screenshot-settings.png" alt="BlokHead's settings screen: diagonal d-pad corners, left-handed mode, sound, starting difficulty, well size, button position and opacity, and block set" width="180" /> <img src="docs/screenshot-pause.png" alt="BlokHead's pause screen with its Menu button pressed, showing a Quit to Menu confirmation dialog" width="180" />

## Features

- Blocks fall and move in 3 dimensions and can be rotated around the X, Y and Z axes, same as in
  Blokout.
- Three block sets to choose from: flat, extended, and the full original set from Blokout.
- On screen buttons: a d-pad to move (with optional diagonal corners) that drops the block from
  its center, and a cluster to rotate. You can change their size, opacity and position, or flip them for left handed play.
- Gesture controls: drag with one finger to move, twist two fingers to rotate around Z, slide two
  fingers to rotate around X and Y, and long press to drop.
- Bluetooth and USB controllers, with buttons you can remap in Settings.
- The well size and depth can both be changed, as can the starting difficulty.
- Layers flash when they clear, like they did in the original.
- Tap the top of the screen to pause or quit.
- A high score table that sticks around between games.
- Sound effects!

## Installing

The easiest way to install BlokHead and keep it up to date is through my F-Droid repo:

[https://roge-rm.gitlab.io/repo](https://roge-rm.gitlab.io/repo?fingerprint=80438B253C257BCCE05CDCB9E3AC9B6174C2250659962B14FCBE7F32FD42D53E)

Then search for BlokHead in F-Droid. When a new version comes out, F-Droid will offer it as an update.

You can also download the APK from the [Releases](https://github.com/roge-rm/BlokHead/releases)
page and sideload it.

## Requirements

- Android Studio (recent stable), or Gradle with a JDK set up.
- minSdk 27 / targetSdk 37.
- No NDK, no native code and no network access. The whole game is plain Kotlin and OpenGL ES 2.0
  through `GLSurfaceView`.

## Building & testing

```
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # run the unit tests
```

The game logic in `game/` is plain Kotlin with nothing from Android in it, so its tests run on the
computer instead of the emulator. That's where the well, collision and scoring code gets checked
against how the original does it.

## Architecture

- `game/` - the port itself: the well, block forms, rotation, collision, scoring, and the
  `GameEngine` that runs it all (`Tube`, `Block`, `Collision`, `Form`, `GameEngine`). None of it
  touches rendering, and all of it has unit tests.
- `render/` - the OpenGL ES 2.0 renderer (`BlokoutRenderer`, `BlokoutSurfaceView`), plus the
  perspective math in `Geometry` that keeps the well's grid lines evenly spaced from front to back
  however deep the well is.
- `ui/` - the Jetpack Compose screens drawn over the GL surface: the main menu, the in-game HUD,
  pause, game over and high score overlays, the touch controls, Settings and About.
- `audio/` - `SfxPlayer`, a small `SoundPool` wrapper for the sound effects.
- `data/` - settings (`SettingsStore`), controller bindings and the high score table
  (`HighScoreStore`), all saved with DataStore.

## Discussion and support

Questions, ideas, bug reports?<br>
Check out the #blokhead channel **[on my discord](https://discord.gg/9Wun47jGC6)**.

## Attribution

- Blokout is Copyright (C) 1998-1999 Johannes Lehtinen and Petri Salmi, from
  [jlehtine/blokout](https://github.com/jlehtine/blokout).
- Ported to Kotlin and Android, with a new OpenGL ES 2.0 renderer and touch UI, by Dan Hunke.

## License

GNU General Public License v3 (or later), same as Blokout. See [LICENSE](LICENSE).
