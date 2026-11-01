package android.graphics
import android.content.res.Resources
class Bitmap(val width:Int=2172, val height:Int=724)
class Rect(val left:Int, val top:Int, val right:Int, val bottom:Int)
object BitmapFactory {
  fun decodeResource(resources: Resources, resId: Int): Bitmap = Bitmap()
}
