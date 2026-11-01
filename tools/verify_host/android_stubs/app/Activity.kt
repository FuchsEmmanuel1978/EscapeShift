package android.app
import android.content.Context
import android.os.Bundle
import android.view.View
open class Activity:Context() {
 open fun onCreate(savedInstanceState:Bundle?){}
 open fun onResume(){}
 open fun onPause(){}
 open fun onDestroy(){}
 val window:Window = Window()
 var currentView: View? = null
 fun setContentView(view: View){currentView = view}
 class Window {val decorView:View=View(Context()); var attributes:android.view.WindowManager.LayoutParams = android.view.WindowManager.LayoutParams()}
}
