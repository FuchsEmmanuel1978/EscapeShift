package android.content
open class Context {
  val applicationInfo = android.content.pm.ApplicationInfo()
  fun getSharedPreferences(name: String, mode: Int): SharedPreferences = SharedPreferences()
  companion object { const val MODE_PRIVATE = 0 }
}
class SharedPreferences {
  fun getInt(k: String, default: Int): Int = default
  fun edit(): Editor = Editor()
  class Editor {
    fun putInt(k: String, n: Int): Editor = this
    fun apply(): Unit = Unit
  }
}
