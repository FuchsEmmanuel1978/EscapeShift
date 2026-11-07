package android.graphics
class Canvas {
 fun drawColor(color:Int) {}
 fun drawBitmap(bmp:Bitmap, src:Rect?, dst:RectF, p:Paint) {}
 fun save():Int=0
 fun restore() {}
 fun translate(dx:Float,dy:Float) {}
 fun scale(sx:Float,sy:Float) {}
 fun rotate(deg:Float) {}
 fun drawRect(l:Float,t:Float,r:Float,b:Float,p:Paint) {}
 fun drawRoundRect(r:RectF, rx:Float, ry:Float, p:Paint) {}
 fun drawPath(path:Path, paint:Paint) {}
 fun drawLine(x:Float,y:Float,x2:Float,y2:Float,paint:Paint) {}
 fun drawCircle(cx:Float,cy:Float,radius:Float,paint:Paint) {}
 fun drawText(txt:String,x:Float,y:Float,p:Paint) {}
}
class RectF(val left:Float, val top:Float, val right:Float, val bottom:Float) {
 fun contains(x:Float,y:Float):Boolean=false
 fun centerX():Float=(left+right)/2f
 fun centerY():Float=(top+bottom)/2f
 fun width():Float=right-left
 fun height():Float=bottom-top
 companion object {
  @JvmStatic fun intersects(a:RectF,b:RectF):Boolean = false
 }
}
class Paint(flags:Int) {
 companion object { const val ANTI_ALIAS_FLAG = 1 }
 enum class Style { FILL, STROKE }
 var isFilterBitmap:Boolean = false
 var color:Int = 0
 var alpha:Int = 255
 var style:Style = Style.FILL
 var strokeWidth:Float = 0f
 var textSize:Float = 0f
 var typeface:Typeface? = null
 fun measureText(s:String):Float=s.length*textSize*0.5f
}
class Path {
 enum class FillType { EVEN_ODD }
 enum class Direction { CW, CCW }
 var fillType: FillType = FillType.EVEN_ODD
 fun addRect(l:Float,t:Float,r:Float,b:Float,dir:Direction){}
 fun addCircle(cx:Float,cy:Float,radius:Float,dir:Direction){}
 fun cubicTo(x1:Float,y1:Float,x2:Float,y2:Float,x3:Float,y3:Float){}
 fun moveTo(x:Float,y:Float){}
 fun lineTo(x:Float,y:Float){}
 fun close(){}
}
class Typeface {
 companion object {
  const val BOLD=1; const val NORMAL=0
  fun create(family:String, style:Int):Typeface=Typeface()
 }
}
object Color {
 const val WHITE:Int=-1
 const val BLACK:Int=-16777216
 fun rgb(r:Int,g:Int,b:Int):Int=0
 fun argb(a:Int,r:Int,g:Int,b:Int):Int=0
 fun red(c:Int):Int=0
 fun green(c:Int):Int=0
 fun blue(c:Int):Int=0
}
