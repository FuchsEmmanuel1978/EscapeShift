package android.view
import android.content.Context
import android.graphics.Canvas
open class View(val context:Context) {
 val resources get() = context.resources
 var width:Int = 1000
 var height:Int = 560
 var isFocusable:Boolean=false
 var isFocusableInTouchMode:Boolean=false
 fun requestFocus():Boolean=true
 open fun onKeyDown(keyCode:Int, event:KeyEvent?):Boolean=false
 open fun onKeyUp(keyCode:Int, event:KeyEvent?):Boolean=false
 open fun onDraw(canvas:Canvas){}
 open fun onTouchEvent(event:MotionEvent):Boolean=false
 fun postInvalidateOnAnimation(){}
 fun invalidate(){}
 var systemUiVisibility:Int = 0
 companion object {
  const val SYSTEM_UI_FLAG_FULLSCREEN=1
  const val SYSTEM_UI_FLAG_HIDE_NAVIGATION=2
  const val SYSTEM_UI_FLAG_IMMERSIVE_STICKY=4
  const val SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN=8
  const val SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION=16
  const val SYSTEM_UI_FLAG_LAYOUT_STABLE=32
 }
}
class MotionEvent {
 val actionMasked:Int=0
 val actionIndex:Int=0
 val pointerCount:Int=1
 fun getX(i:Int):Float=0f
 fun getY(i:Int):Float=0f
 companion object {
  const val ACTION_UP=1
  const val ACTION_CANCEL=3
  const val ACTION_POINTER_UP=6
 }
}

class KeyEvent {
 companion object {
 const val KEYCODE_DPAD_LEFT=21; const val KEYCODE_DPAD_RIGHT=22
 const val KEYCODE_A=29; const val KEYCODE_D=32
 const val KEYCODE_DPAD_UP=19; const val KEYCODE_W=51
 const val KEYCODE_SPACE=62; const val KEYCODE_S=47
 const val KEYCODE_PAGE_UP=92; const val KEYCODE_PAGE_DOWN=93
 }
}
