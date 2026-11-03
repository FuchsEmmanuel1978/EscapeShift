import android.content.Context
import android.graphics.Canvas
import com.escapeshift.game.*
fun main() {
 val ui=GameView(Context())
 val canvas=Canvas()
 for((pw,ph) in listOf(1000 to 560,2400 to 1080,2520 to 1080)) {
   ui.width=pw;ui.height=ph
   val start=ui.javaClass.getDeclaredMethod("startLevel",Int::class.javaPrimitiveType)
   start.isAccessible=true
   for(level in 1..10) {
     start.invoke(ui,level)
     val state=ui.javaClass.getDeclaredField("adventureOverlay")
     state.isAccessible=true
     (state.get(ui) as AdventureOverlay).open=true
     ui.onDraw(canvas)
   }
   println("ADVENTURE UI $pw x $ph: 10 screens rendered")
 }
}
