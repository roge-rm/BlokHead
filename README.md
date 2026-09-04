# BlokHead

BlokHead is an Android port of [Blokout](https://jlehtinen.net/blokout/), a 3D falling-block
puzzle game by Johannes Lehtinen and Petri Salmi. Pieces fall through a 3D well and you
translate/rotate them on all three axes to complete and clear horizontal layers.

This is a direct port of the original C/OpenGL source
([jlehtine/blokout](https://github.com/jlehtine/blokout)) to Kotlin/OpenGL ES for Android, so it
carries the same license as upstream.

## Status

Early scaffolding — game logic port in progress.

## Architecture

- `game/` — pure-Kotlin, rendering-free port of the original game logic (well/grid state, piece
  forms, collision, scoring, state machine). Unit-testable independent of Android/GL.
- `render/` — `GLSurfaceView`-based OpenGL ES renderer for the well and pieces.
- `ui/` — Jetpack Compose for the HUD, menus, and high-score screens, layered over the GL surface.
- `data/` — high-score persistence.

## Building

```
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # run unit tests
```

Requires Android Studio (recent stable) or a JDK-configured Gradle; minSdk 27 / targetSdk 37.

## Attribution

- Game design and original implementation: Copyright (C) 1998-1999 Johannes Lehtinen and Petri
  Salmi, from [jlehtine/blokout](https://github.com/jlehtine/blokout).
- Ported to Kotlin/Android by Dan Hunke.

## License

GNU General Public License v3 (or later) — see [LICENSE](LICENSE), matching the upstream project.
