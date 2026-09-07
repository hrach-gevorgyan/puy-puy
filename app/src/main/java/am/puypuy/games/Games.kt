package am.puypuy.games

import am.puypuy.core.MiniGame

/**
 * The registered games, in the order they appear on the hub.
 *
 * This list is the only place any game is named. Adding a fifth is one entry
 * here (§7.3) — the hub sizes its grid to whatever it finds.
 */
fun allGames(): List<MiniGame> = listOf(
    BalloonsGame(),
    StackGame(),
    CoconutsGame(),
    ShapesGame(),
    PuzzleGame(),
    PaintGame(),
)
