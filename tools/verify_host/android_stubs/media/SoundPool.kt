package android.media
import android.content.Context
class AudioAttributes {
    companion object { const val USAGE_GAME=1; const val CONTENT_TYPE_SONIFICATION=2 }
    class Builder { fun setUsage(value:Int)=this; fun setContentType(value:Int)=this; fun build()=AudioAttributes() }
}
class SoundPool {
    class Builder { fun setMaxStreams(n:Int)=this; fun setAudioAttributes(a:AudioAttributes)=this; fun build()=SoundPool() }
    fun setOnLoadCompleteListener(listener:(SoundPool,Int,Int)->Unit) {}
    fun load(ctx:Context, id:Int, priority:Int)=id
    fun play(id:Int,l:Float,r:Float,priority:Int,loop:Int,rate:Float)=1
    fun setVolume(stream:Int,l:Float,r:Float) {}
    fun stop(stream:Int) {}
    fun release() {}
}
