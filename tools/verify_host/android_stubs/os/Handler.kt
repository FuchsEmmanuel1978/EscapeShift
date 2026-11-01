package android.os
class Looper { companion object { fun getMainLooper(): Looper = Looper() } }
class Handler(looper: Looper) {
  fun postDelayed(r: Runnable, delayMillis: Long): Boolean = true
  fun removeCallbacks(r: Runnable) {}
}
