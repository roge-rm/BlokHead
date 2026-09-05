package com.rm.blokhead.ui

/** Top-level navigation state, standing in for the original's isMenuOverlay/isGameOn/
 *  isHighScoreMode/isScoreTableMode flags in game.c — those toggled overlays on top of a single
 *  GLUT window; here each is a distinct Compose screen instead. */
enum class AppScreen {
    MENU,
    GAME,
    HIGH_SCORES,
    SETTINGS,
    CONTROLLER,
}
