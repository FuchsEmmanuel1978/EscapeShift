package com.escapeshift.game

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.View

class MainActivity : Activity() {
    private var gameMusic: GameMusic? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val openMenu = Runnable { setContentView(GameView(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }
        // Show the supplied illustration once when the app is started.
        // The game view and its saved progression remain unchanged.
        setContentView(StartupSplashView(this))
        mainHandler.postDelayed(openMenu, 2200L)
        gameMusic = GameMusic(this)
    }

    override fun onResume() {
        super.onResume()
        gameMusic?.resume()
    }

    override fun onPause() {
        gameMusic?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(openMenu)
        gameMusic?.release()
        gameMusic = null
        super.onDestroy()
    }
}
