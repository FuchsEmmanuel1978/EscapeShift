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
    private var gameView: GameView? = null
    private var rewardedAds: AdMobRewards? = null
    private var consent: ConsentManager? = null
    private var billing: PlayBillingManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val openMenu = Runnable {
        val view = GameView(this,
            onRequestReward = { kind, result ->
                val manager = rewardedAds
                if (manager == null) result(false) else manager.show(kind, result)
            },
            onPrivacyOptions = { consent?.openPrivacyOptions() },
            onRequestPurchase = { product -> billing?.buy(product) },
            onRestorePurchases = { billing?.refresh() },
            onPrice = { product -> billing?.formattedPrice(product) },
            privacyOptionsAvailable = { consent?.privacyOptionsRequired() == true },
            onPauseChanged = { paused -> if (paused) gameMusic?.pause() else gameMusic?.resume() },
            onSettingsChanged = { gameMusic?.applyUserSettings() }
        )
        gameView = view
        setContentView(view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Install the Android assets loader BEFORE constructing any GameWorld.
        // The same JSONs are used by the app and the desktop level simulator.
        LevelCatalog.install { number ->
            assets.open(LevelCatalog.assetPath(number))
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        WorldPreviewCatalog.install { number ->
            assets.open("previews/${WorldPreviewCatalog.filename(number)}")
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
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
        consent = ConsentManager(this)
        rewardedAds = AdMobRewards(this)
        billing = PlayBillingManager(this, onChanged = { gameView?.invalidate() },
            onMessage = { android.widget.Toast.makeText(this, it, android.widget.Toast.LENGTH_LONG).show() })
        billing?.connect()
        consent?.request { canRequest -> if (canRequest) rewardedAds?.initialize() }
    }

    override fun onResume() {
        super.onResume()
        if (gameView?.isPaused != true) gameMusic?.resume()
        gameView?.resumeAudio()
        billing?.refresh()

    }

    override fun onPause() {

        gameMusic?.pause()
        gameView?.pauseAudio()
        super.onPause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(openMenu)
        billing?.close()
        rewardedAds?.release()
        rewardedAds = null
        gameView?.releaseAudio()
        gameMusic?.release()
        gameMusic = null
        super.onDestroy()
    }
}
