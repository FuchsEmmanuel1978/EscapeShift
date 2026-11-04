import android.content.Context
import android.graphics.Canvas
import com.escapeshift.game.GameView
import com.escapeshift.game.LevelCatalog
import com.escapeshift.game.WorldPreviewCatalog

/** Host-side Android stand-in smoke; does not replace an actual Xiaomi test. */
fun main() {
    val view = GameView(Context())
    val canvas = Canvas()
    val page = view.javaClass.getDeclaredMethod("drawWorldPage", Canvas::class.java, Int::class.javaPrimitiveType)
    page.isAccessible = true
    for ((vw, vh) in listOf(1000 to 560, 2400 to 1080, 2520 to 1080)) {
        view.width = vw; view.height = vh
        view.onDraw(canvas)
        for (world in 1..10) page.invoke(view, canvas, world)
        println("PASS 10 world pages @ ${vw}x${vh}")
    }
    val start = view.javaClass.getDeclaredMethod("startLevel", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
    start.isAccessible = true
    for (number in listOf(1, 11, 21, 31, 41, 51, 61, 71, 81, 91)) {
        start.invoke(view, number, number != 1)
        repeat(3) { view.onDraw(canvas) }
        println("PASS game screen $number")
    }
    // Original V20 level 11 must still be constructable independently.
    start.invoke(view, 11, false)
    view.onDraw(canvas)
    println("PASS original level 11")
}
