package com.escapeshift.game

import android.content.Context
import android.content.pm.ApplicationInfo
import org.json.JSONObject
import java.util.Locale

/** Language selection and lookup using JSON files embedded in this Android project.
 * The canonical translations live in app/src/main/assets/i18n/ and are versioned
 * alongside the game source in the same EscapeShift GitHub repository.
 * The game reads them offline through Android's AssetManager.
 */
object GameLanguage {
    val SUPPORTED = listOf("en", "fr", "es", "pt-BR", "de", "it", "ja", "ko", "hi", "id")

    fun normalize(tag: String): String? {
        val clean = tag.trim().replace('_', '-').lowercase(Locale.ROOT)
        if (clean.isEmpty()) return null
        val language = clean.substringBefore('-')
        // Portuguese uses Brazilian wording for both pt and pt-* devices.
        if (language == "pt") return "pt-BR"
        // Java's older Indonesian language alias.
        if (language == "in") return "id"
        return SUPPORTED.firstOrNull { it.equals(language, ignoreCase = true) }
    }

    /** Device locales are processed in Android priority order, then English fallback. */
    fun choose(tags: List<String>, debug: Boolean, debugOverride: String): String {
        if (debug && debugOverride.isNotBlank()) {
            normalize(debugOverride)?.let { return it }
        }
        return tags.firstNotNullOfOrNull { normalize(it) } ?: "en"
    }
}

class GameStrings(private val context: Context) {
    private val defaultLanguage: String
    private val fallback: Map<String, String>
    private val translations = mutableMapOf<String, Map<String, String>>()
    private val prefs = context.getSharedPreferences("escape_shift", Context.MODE_PRIVATE)
    val language: String get() = prefs.getString("v2_language", null)
        ?.let(GameLanguage::normalize) ?: defaultLanguage

    init {
        val config = context.resources.configuration
        val tags = (0 until config.locales.size()).map { config.locales[it].toLanguageTag() }
        val debug = DevConfig.ENABLED &&
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        defaultLanguage = GameLanguage.choose(tags, debug, DevConfig.FORCE_LANGUAGE)
        fallback = readJson("en")
        translations["en"] = fallback
    }

    private fun readJson(code: String): Map<String, String> {
        val source = context.assets.open("i18n/$code.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val json = JSONObject(source)
        return buildMap {
            val names = json.keys()
            while (names.hasNext()) {
                val key = names.next()
                put(key, json.getString(key))
            }
        }
    }

    /** Lookup with named placeholders: t("level_count", "n" to 3). */
    fun t(key: String, vararg values: Pair<String, Any>): String {
        val translated = translations.getOrPut(language) {
            try { readJson(language) } catch (_: Exception) { fallback }
        }
        var result = translated[key] ?: fallback[key] ?: "[$key]"
        for ((name, value) in values) result = result.replace("{$name}", value.toString())
        return result
    }
}
