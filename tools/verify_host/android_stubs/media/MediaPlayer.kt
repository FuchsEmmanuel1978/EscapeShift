package android.media
import android.content.Context
class MediaPlayer {
    var isLooping: Boolean = false
    var isPlaying: Boolean = false
    fun setVolume(left:Float, right:Float) {}
    fun start() { isPlaying = true }
    fun pause() { isPlaying = false }
    fun release() {}
    companion object { fun create(context:Context, resource:Int): MediaPlayer? = MediaPlayer() }
}
