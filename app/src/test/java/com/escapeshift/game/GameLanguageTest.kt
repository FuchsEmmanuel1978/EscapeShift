package com.escapeshift.game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameLanguageTest {
    @Test fun usesPreferredDeviceLocaleAndEnglishFallback() {
        assertEquals("fr", GameLanguage.choose(listOf("nl-NL", "fr-FR"), false, ""))
        assertEquals("en", GameLanguage.choose(listOf("nl-NL"), false, ""))
    }
    @Test fun supportsPortugueseAndIndonesianAliases() {
        assertEquals("pt-BR", GameLanguage.choose(listOf("pt-PT"), false, ""))
        assertEquals("id", GameLanguage.choose(listOf("in-ID"), false, ""))
    }
    @Test fun debugOverrideNeverAppliesInRelease() {
        assertEquals("ja", GameLanguage.choose(listOf("fr-FR"), true, "ja"))
        assertEquals("fr", GameLanguage.choose(listOf("fr-FR"), false, "ja"))
        assertEquals("fr", GameLanguage.choose(listOf("fr-FR"), true, "unsupported"))
    }
}
