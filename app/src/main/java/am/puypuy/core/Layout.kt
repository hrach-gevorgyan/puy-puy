package am.puypuy.core

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The layout arithmetic, pulled out of the composables so it can be tested.
 *
 * A layout that is wrong on some real screen — tiles off the edge, controls needing more
 * width than the row has, a board under the home button — is arithmetic, and arithmetic can
 * be asserted without a device. See `LayoutTest`.
 */
object Layout {

    /** Space reserved in the top-left corner for the home button, in every game. */
    val HomeSafe: Dp = MinTouch + 24.dp

    data class Hub(
        val columns: Int,
        val rows: Int,
        /** Side of one square tile card, label strip included. */
        val tile: Dp,
        val character: Dp,
        val gap: Dp,
    ) {
        /** Width the tile grid actually needs, outer padding included. */
        fun widthNeeded(gap: Dp = this.gap): Dp = tile * columns + gap * (columns + 1)

        /** Height the tile grid actually needs, outer padding included. */
        fun heightNeeded(): Dp = tile * rows + gap * (rows + 1)
    }

    /**
     * The grid is chosen by trying every column count and keeping whichever gives the
     * BIGGEST tile. Six games are 2x3 held upright and 3x2 turned sideways without either
     * being written down anywhere, and a seventh game would re-solve it on its own.
     *
     * The previous rule was "one row per game in landscape, two columns otherwise", which
     * put six tiles across a phone held sideways at well under the touch floor.
     */
    fun hub(width: Dp, height: Dp, games: Int, gap: Dp = 20.dp): Hub {
        // He stands at the bottom. The tiles never enter his band, because a tile a child
        // cannot press without pressing him is a tile that feels broken.
        val character = (minOf(width, height) * 0.26f).coerceIn(120.dp, 240.dp)
        val forTiles = (height - character * 0.72f).coerceAtLeast(MinTouch + gap * 2)

        var best: Hub? = null
        for (columns in 1..games) {
            val rows = (games + columns - 1) / columns
            val tile = minOf(
                (width - gap * (columns + 1)) / columns,
                (forTiles - gap * (rows + 1)) / rows,
            )
            if (best == null || tile > best.tile) best = Hub(columns, rows, tile, character, gap)
        }
        // The floor is applied AFTER the best shape is chosen, never before it: comparing
        // already-floored sizes made every cramped screen tie at the floor and fall through
        // to a single column, which is the opposite of what a cramped screen needs.
        val grid = best!!
        if (grid.tile >= MinTouch) return grid

        // Too cramped for the shape we wanted. Raising the tile to the floor also means
        // taking columns away, because a grid may scroll DOWN but must never run off the
        // side — that is how a game ends up measured off the edge of the screen.
        val fitting = ((width - gap) / (MinTouch + gap)).toInt().coerceIn(1, games)
        val columns = minOf(grid.columns, fitting)
        return Hub(columns, (games + columns - 1) / columns, MinTouch, character, gap)
    }

    data class Tower(
        /** Radius of each coconut, indexed by size 1..n. */
        val radii: List<Dp>,
        /** Centre of each coconut where it lies loose, left to right, biggest last. */
        val restX: List<Dp>,
        val restY: Dp,
        /** Where the pile is built. */
        val baseX: Dp,
        val baseY: Dp,
        val gap: Dp,
    ) {
        /** How tall the finished tower stands, from the sand to the top of the smallest. */
        fun heightNeeded(): Dp = radii.fold(0.dp) { acc, r -> acc + r * 1.75f }

        /** Width the loose row occupies, gaps included. */
        fun rowWidth(): Dp = radii.fold(gap) { acc, r -> acc + r * 2f + gap }
    }

    /**
     * Where the coconuts of Կառուցի՛ր lie and how big they are.
     *
     * This is here rather than in the game because it is arithmetic, and every layout bug this
     * app has had was arithmetic: the first version of this screen spaced five coconuts 24dp
     * apart while drawing them 42-87dp wide, so they overlapped into one lump and used a third
     * of the screen. `LayoutTest` asserts they cannot touch.
     */
    /** Air above the finished tower, so it never touches the very top of the screen. */
    private val TOP_MARGIN = 16.dp

    fun tower(width: Dp, height: Dp, most: Int = 5, gap: Dp = 10.dp): Tower {
        // As many coconuts as will still be worth reaching for. A phone held sideways is only
        // 320dp tall, and a five-high tower there works out at 20dp a coconut — so that screen
        // gets a shorter tower of bigger ones rather than a taller one of crumbs.
        for (count in most downTo 3) {
            val tower = build(width, height, count, gap)
            if (tower.radii.first() >= 18.dp && tower.radii.last() >= 30.dp) return tower
        }
        return build(width, height, 3, gap)
    }

    private fun build(width: Dp, height: Dp, count: Int, gap: Dp): Tower {
        // Biggest is 1.0, smallest a little over half, so the difference is visible at a glance.
        val ratios = (1..count).map { 0.55f + 0.45f * it / count }
        val sum = ratios.sum()

        // Two constraints, and the tighter one wins: the loose row must fit across the screen,
        // and the finished tower must fit under the top of it.
        //
        // The tower is built in the MIDDLE, and [HomeSafe] is a corner rather than a band, so
        // it may rise past the home button's height without ever going under it — the caller
        // keeps the pile clear of that corner, and `LayoutTest` checks it does. Treating the
        // corner as a full-width band left a phone held sideways with 146dp to build in.
        val byWidth = (width - gap * (count + 1)) / (2f * sum)
        val byHeight = (height - TOP_MARGIN - 24.dp) / (1.75f * sum + 2.2f)
        // Never below 1dp. A composable is laid out at 0x0 for one frame before it is measured,
        // and negative radii from that frame are not a size — they are a crash waiting for
        // something to divide by.
        val r = minOf(byWidth, byHeight).coerceAtLeast(1.dp)

        val radii = ratios.map { r * it }
        val rowWidth = radii.fold(gap) { acc, each -> acc + each * 2f + gap }
        var x = (width - rowWidth) / 2f + gap
        val restX = radii.map { each ->
            val centre = x + each
            x += each * 2f + gap
            centre
        }
        val biggest = radii.last()
        // Centred, unless centring would put the pile under the home button on a narrow
        // screen — then it shifts right until it clears the corner.
        val baseX = maxOf(width / 2f, HomeSafe + biggest + gap)
            .coerceAtMost(width - biggest - gap)
        return Tower(
            radii = radii,
            restX = restX,
            restY = height - biggest - gap,
            baseX = baseX,
            baseY = height - biggest * 2.2f - gap * 2f,
            gap = gap,
        )
    }

    data class Puzzle(
        /** Top-left of the board, and the side of the whole square. */
        val boardX: Dp,
        val boardY: Dp,
        val board: Dp,
        /** Where each loose piece waits, in board order. */
        val tray: List<Pair<Dp, Dp>>,
        /**
         * How big a piece is drawn while it is still waiting. Smaller than a board cell,
         * because nine cell-sized pieces do not fit beside the board they came from — so a
         * piece grows as it lands, which also reads as it settling into place.
         */
        val trayCell: Dp,
    ) {
        val cell: Dp get() = board / 3f
    }

    /**
     * Փազլ: a square board, and nine pieces waiting to be carried into it.
     *
     * The board is as big as the shorter side allows, and the tray goes wherever there is room
     * left — under the board when the screen is tall, beside it when the screen is wide. Always
     * putting the tray underneath starved the board on a phone held sideways: a 34dp piece,
     * against a 126dp touch floor.
     */
    fun puzzle(width: Dp, height: Dp, gap: Dp = 12.dp): Puzzle {
        val tall = height >= width
        val cols = if (tall) 5 else 3          // how the nine loose pieces are arranged
        val rows = (9 + cols - 1) / cols

        val board: Dp
        val boardX: Dp
        val boardY: Dp
        val trayX: Dp
        val trayY: Dp
        if (tall) {
            val usable = height - HomeSafe - gap * 3f
            board = minOf(width - gap * 2f, usable / (1f + 0.36f * rows)).coerceAtLeast(90.dp)
            boardX = (width - board) / 2f
            boardY = HomeSafe + gap
            trayX = gap
            trayY = boardY + board + gap
        } else {
            val usable = height - HomeSafe - gap * 2f
            board = minOf(usable, width * 0.58f).coerceAtLeast(90.dp)
            boardX = gap
            boardY = HomeSafe + gap
            trayX = boardX + board + gap
            trayY = boardY
        }

        val piece = board / 3f
        val trayStepX = if (tall) (width - gap * 2f) / cols else (width - trayX - gap) / cols
        val trayStepY = if (tall) piece * 1.05f else (height - trayY - gap) / rows
        val tray = (0 until 9).map { i ->
            trayX + trayStepX * (i % cols + 0.5f) to trayY + trayStepY * (i / cols + 0.5f)
        }
        return Puzzle(
            boardX = boardX,
            boardY = boardY,
            board = board,
            tray = tray,
            trayCell = minOf(piece, trayStepX * 0.88f, trayStepY * 0.88f),
        )
    }

    data class Shapes(
        /** Side of one shape, and of the hole it fits. */
        val size: Dp,
        /** Centre of every hole, laid out in a grid across the top. */
        val holes: List<Pair<Dp, Dp>>,
        /** The band of sand the loose shapes are scattered over. */
        val scatterTop: Dp,
        val scatterBottom: Dp,
        val scatterLeft: Dp,
        val scatterRight: Dp,
    ) {
        val count: Int get() = holes.size
    }

    /**
     * Ձևե՛ր: a grid of holes across the top, and the same number of shapes scattered loose on
     * the sand below.
     *
     * The holes are ordered because she has to be able to FIND the one she wants; the shapes
     * are not, because a tidy row beneath a tidy row turns the game into "drag straight up".
     */
    fun shapes(width: Dp, height: Dp, want: Int = 6, gap: Dp = 14.dp): Shapes {
        // How many fit across, and therefore how the grid of holes is shaped.
        val columns = ((width - gap) / (86.dp + gap)).toInt().coerceIn(3, want)
        // Two rows of holes where there is room, one where there is not: a phone held sideways
        // has 170dp under the home button, and two rows plus somewhere to scatter do not fit
        // in it. Squeezing them produced a scatter band of negative height.
        for (rows in 2 downTo 1) {
            val laid = layOut(width, height, columns, rows, want, gap)
            if (laid.scatterBottom - laid.scatterTop >= laid.size) return laid
        }
        return layOut(width, height, columns, 1, want, gap)
    }

    private fun layOut(
        width: Dp,
        height: Dp,
        columns: Int,
        rows: Int,
        want: Int,
        gap: Dp,
    ): Shapes {
        val count = minOf(want, columns * rows)
        val column = (width - gap * (columns + 1)) / columns
        // The size is NOT clamped up to a minimum: on a short screen that produced a shape too
        // big for the space it had to sit in. The touch target is held at 126dp by a floor in
        // the game itself, so a small drawing is never a small target.
        val usable = (height - HomeSafe - gap * 3f).coerceAtLeast(2.dp)
        val size = minOf(column * 0.84f, usable / (rows + 2f)).coerceIn(40.dp, 190.dp)

        val gridWidth = size * columns + gap * (columns - 1)
        val left = (width - gridWidth) / 2f + size / 2f
        val holes = (0 until count).map { i ->
            left + (size + gap) * (i % columns) to
                HomeSafe + size * 0.6f + (size + gap) * (i / columns)
        }

        val holesBottom = HomeSafe + size * 0.6f + (size + gap) * (rows - 1) + size / 2f
        return Shapes(
            size = size,
            holes = holes,
            scatterTop = holesBottom + gap * 2f,
            scatterBottom = height - size * 0.6f,
            scatterLeft = size * 0.6f,
            scatterRight = width - size * 0.6f,
        )
    }

    /**
     * True when the paint controls fit across the bottom. Five [MinTouch] buttons need 630dp,
     * so on a phone in portrait they run down the right edge instead — the alternative was
     * the eraser measuring to zero width and the page becoming impossible to clear.
     */
    fun paintBarFitsRow(width: Dp, controls: Int, bar: Dp = MinTouch + 16.dp): Boolean =
        width >= MinTouch * controls + 24.dp

    data class Board(val span: Dp, val cell: Dp, val originX: Dp, val originY: Dp)

    /**
     * A square 3x3 board, centred below the home button rather than filling the screen,
     * which put coconut #1 under the home button: aiming at a toy left the game.
     */
    fun coconutBoard(width: Dp, height: Dp): Board {
        val span = minOf(width, (height - HomeSafe).coerceAtLeast(1.dp)) * 0.96f
        return Board(
            span = span,
            cell = span / 3f,
            originX = (width - span) / 2f,
            originY = HomeSafe + ((height - HomeSafe) - span) / 2f,
        )
    }
}
