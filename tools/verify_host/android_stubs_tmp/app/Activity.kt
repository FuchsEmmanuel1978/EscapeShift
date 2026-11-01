package android.app
import android.content.Context
import android.os.Bundle
import android.view.View
open class Activity:Context() {
 open fun onCreate(savedInstanceState:Bundle?){}
 val window:Window = Window()
 fun setContentView(view: View){}
 class Window {val decorView:View=View(Context())}
}
