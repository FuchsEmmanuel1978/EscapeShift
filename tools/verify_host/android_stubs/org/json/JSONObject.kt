package org.json
/** Test stand-in for the Android JSON reader (flat string-only dictionaries). */
class JSONObject(s: String) {
    private val data = Regex("\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"")
       .findAll(s).associate { it.groupValues[1] to it.groupValues[2].replace("\\\"", "\"").replace("\\n", "\n") }
    fun keys(): Iterator<String> = data.keys.iterator()
    fun getString(key: String): String = data.getValue(key)
}
