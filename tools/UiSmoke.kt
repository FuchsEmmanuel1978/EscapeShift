import com.escapeshift.game.GameView
import android.content.Context
import android.graphics.Canvas

/** Host-side smoke check using Android API stand-ins, NOT a true emulator test. */
fun main() {
    val ui = GameView(Context())
    val c = Canvas()
    for ((pw, ph) in listOf(1000 to 560, 2400 to 1080, 2520 to 1080)) {
        ui.width = pw
        ui.height = ph
        ui.onDraw(c)
        val field = ui.javaClass.getDeclaredField("w")
        field.isAccessible = true
        val virtualWidth = field.getFloat(ui)
        require(virtualWidth > 0f)
        println("SCREEN OK: ${pw}x${ph} -> ${virtualWidth.toInt()} virtual units")
    }
    val method = ui.javaClass.getDeclaredMethod("startLevel", Int::class.javaPrimitiveType)
    method.isAccessible = true
    for(n in 1..10){
        method.invoke(ui,n)
        repeat(3){ui.onDraw(c)}
        println("DRAW OK: Level $n")
    }
}
