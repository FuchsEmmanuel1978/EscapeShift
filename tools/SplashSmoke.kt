import com.escapeshift.game.MainActivity
import com.escapeshift.game.StartupSplashView
import com.escapeshift.game.GameView
import android.os.Bundle
import android.graphics.Canvas

fun main() {
  val app = MainActivity()
  app.onCreate(null)
  check(app.currentView is StartupSplashView)
  val splash = app.currentView as StartupSplashView
  for ((w,h) in listOf(1920 to 1080, 2400 to 1080, 2520 to 1080)) {
    splash.width=w; splash.height=h; splash.onDraw(Canvas())
    println("SPLASH DRAW OK ${w}x$h")
  }
  val callback = app.javaClass.getDeclaredField("openMenu")
  callback.isAccessible = true
  (callback.get(app) as Runnable).run()
  check(app.currentView is GameView)
  println("SPLASH -> MENU OK")
  app.onResume()
  app.onPause()
  app.onDestroy()
}
