package android.content.res
import java.io.File
import java.io.InputStream
import java.util.Locale
class Resources { val configuration = Configuration() }
class Configuration { val locales = LocaleList() }
class LocaleList {
   fun size() = 1
   operator fun get(i:Int): Locale = Locale.FRENCH
}
class AssetManager {
    fun open(name: String): InputStream = File(System.getProperty("game.assets", "app/src/main/assets"), name).inputStream()
}
