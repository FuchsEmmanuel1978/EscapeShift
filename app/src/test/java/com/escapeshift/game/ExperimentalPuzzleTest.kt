package com.escapeshift.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Logic-only tests. Device motion and actual platform physics are future integrations. */
class ExperimentalPuzzleTest {
    private fun source(level: Int): String {
        val name = "level_${level.toString().padStart(2, '0')}.json"
        val file = listOf(
            File("src/main/assets/levels", name),
            File("app/src/main/assets/levels", name)
        ).firstOrNull { it.isFile } ?: error("Missing prototype $name")
        return file.readText(Charsets.UTF_8)
    }

    @Test fun twentyGoldenRoutesAndLockedDoors() {
        for (n in 21..40) {
            val raw = source(n)
            assertTrue("Level $n logic solution", ExperimentalPuzzleEngine.fromLevelJson(raw).replaySolution().solved)
            val before = ExperimentalPuzzleEngine.fromLevelJson(raw)
            assertFalse("Level $n door bypass", before.apply(mapOf("action" to "exit", "target" to "doorA")).solved)
        }
    }

    @Test fun laserDeactivationExpiresAfterWheelWindow() {
        val p = ExperimentalPuzzleEngine.fromLevelJson(source(22))
        p.apply(mapOf("action" to "push", "target" to "wheelA", "value" to "plateA"))
        assertEquals(true, p.valueFor("flag:laser_disabled"))
        p.apply(mapOf("action" to "wait", "value" to 91))
        assertEquals(false, p.valueFor("flag:laser_disabled"))
        p.apply(mapOf("action" to "cross", "target" to "laserA"))
        assertFalse(p.apply(mapOf("action" to "exit", "target" to "doorA")).solved)
    }

    @Test fun weightPlateRequiresHeroAsWellAsCrates() {
        val p = ExperimentalPuzzleEngine.fromLevelJson(source(24))
        p.apply(mapOf("action" to "place", "target" to "crateA", "value" to "plateA"))
        p.apply(mapOf("action" to "place", "target" to "crateB", "value" to "plateA"))
        p.apply(mapOf("action" to "press", "target" to "latch"))
        assertNotEquals(true, p.valueFor("flag:lift_lowered"))
        p.apply(mapOf("action" to "stand", "target" to "plateA"))
        p.apply(mapOf("action" to "press", "target" to "latch"))
        assertEquals(true, p.valueFor("flag:lift_lowered"))
    }

    @Test fun wrongPrimeLocksNumberDoor() {
        val p = ExperimentalPuzzleEngine.fromLevelJson(source(27))
        assertTrue(p.apply(mapOf("action" to "collect", "target" to "n4")).failed)
    }

    @Test fun wrongWheelAngleDoesNotUnlock() {
        val p = ExperimentalPuzzleEngine.fromLevelJson(source(26))
        p.apply(mapOf("action" to "rotate", "target" to "wheelA", "value" to 90))
        p.apply(mapOf("action" to "cross", "target" to "gapA"))
        assertFalse(p.apply(mapOf("action" to "exit", "target" to "doorA")).solved)
    }
}
