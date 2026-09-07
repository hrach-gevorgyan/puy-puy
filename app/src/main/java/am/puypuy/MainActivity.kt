package am.puypuy

import am.puypuy.games.allGames
import am.puypuy.ui.PuypuyApp
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // She holds it for a long time without touching anything, so the screen must not
        // sleep mid-game. FLAG_KEEP_SCREEN_ON is scoped to this window, so it stops applying
        // the moment the app is not in front — no need to clear it by hand.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        goFullscreen()

        val games = allGames()
        setContent {
            PuypuyApp(games = games, onLeaveApp = { finish() })
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goFullscreen()
    }

    /**
     * Edge to edge, with the status bar left VISIBLE.
     *
     * Immersive-sticky, which hides both system bars, would keep a toddler from
     * pulling down the shade, but it also took away the clock and the battery from the adult
     * holding the tablet, and immersive-sticky is inert on API 35+ anyway. The navigation bar
     * stays hidden, because that is the one with a Back and a Home on it.
     */
    private fun goFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // Dark icons: the ground behind them is pale sand or a white page. In the phone's
            // dark mode the system defaults these to white, which made the clock and the
            // battery invisible.
            isAppearanceLightStatusBars = true
        }
    }
}
