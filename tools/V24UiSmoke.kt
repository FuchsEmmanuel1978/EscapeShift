import android.content.Context
import android.graphics.Canvas
import com.escapeshift.game.GameView

/** Android host-side smoke (not a device APK), uses the same real GameView code. */
fun main() {
    val view = GameView(Context())
    val canvas = Canvas()
    val start = view.javaClass.getDeclaredMethod("startLevel", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
    start.isAccessible = true
    val hearts = view.javaClass.getDeclaredField("hearts").apply { isAccessible = true }
    val respawn = view.javaClass.getDeclaredMethod("respawnAfterDeath").apply { isAccessible = true }
    for ((pw,ph) in listOf(1000 to 560, 2400 to 1080, 2520 to 1080)) {
        view.width = pw
        view.height = ph
        for (n in 1..20) {
            start.invoke(view,n,false)
            view.onDraw(canvas)
        }
        println("PASS 20 gameplay renders @$pw x $ph")
    }
    start.invoke(view,1,false)
    check(hearts.getInt(view) == 3)
    respawn.invoke(view)
    check(hearts.getInt(view) == 2)
    respawn.invoke(view)
    check(hearts.getInt(view) == 1)
    start.invoke(view,11,false)
    check(hearts.getInt(view) == 1)
    println("PASS hearts=3 for world1, quick respawn reduces hearts; unchanged world2 death flow")
}
