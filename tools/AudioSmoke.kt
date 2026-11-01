import android.content.Context
import com.escapeshift.game.GameMusic
import android.media.MediaPlayer

fun main() {
    val music = GameMusic(Context())
    val field = GameMusic::class.java.getDeclaredField("player")
    field.isAccessible = true
    val player = field.get(music) as MediaPlayer
    check(player.isLooping)
    check(!player.isPlaying)
    music.resume()
    check(player.isPlaying)
    music.pause()
    check(!player.isPlaying)
    music.resume()
    check(player.isPlaying)
    music.release()
    check(field.get(music) == null)
    println("AUDIO LIFECYCLE OK: loop / resume / pause / release")
}
