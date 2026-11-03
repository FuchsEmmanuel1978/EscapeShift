package com.escapeshift.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LevelJsonTest {
    private fun json(n: Int): String {
        val f = listOf(
            File("src/main/assets/levels/${LevelCatalog.filename(n)}"),
            File("app/src/main/assets/levels/${LevelCatalog.filename(n)}")
        ).first { it.exists() }
        return f.readText(Charsets.UTF_8)
    }

    @Test fun allTenJsonLevelsHaveValidObjectsAndUniqueIds() {
        for (n in 1..10) {
            val data = LevelJsonParser.parse(json(n), n)
            assertTrue(data.objects.isNotEmpty())
            assertEquals(1, data.objects.count { it.type == "door" })
            assertEquals(data.objects.size, data.objects.map { it.id }.distinct().size)
            assertTrue(data.challenges.isNotEmpty())
        }
    }

    @Test fun crateDoorLasersAndPlatformAreFullyDefinedInJson() {
        val l2=LevelJsonParser.parse(json(2),2)
        assertTrue(l2.objects.any { it.type == "crate" && it.x == 201f && it.y == 400f })
        assertTrue(l2.objects.any { it.type == "bridge" && it.width == 140f })
        assertTrue(l2.objects.any { it.type == "door" && it.height == 90f })
        val l5=LevelJsonParser.parse(json(5),5)
        assertTrue(l5.objects.any { it.type == "platform" })
        val l8=LevelJsonParser.parse(json(8),8)
        assertTrue(l8.objects.any { it.type == "monster" })
    }

    @Test fun changedJsonCoordinatesAreUsedByTheRealGamePhysics() {
        val original=json(1)
        val modified=original.replace("\"x\": 385,", "\"x\": 410,", ignoreCase=false)
        assertTrue(modified != original)
        try {
            LevelCatalog.install { n -> if (n == 1) modified else json(n) }
            val level=GameWorld(1)
            assertEquals(410f,level.spikes.first().left,0.01f)
            assertEquals(410f,level.spec.challenges.first().x,0.01f)
        } finally {
            LevelCatalog.useProjectFiles()
        }
    }

    @Test fun gapWidthIsLoadedFromJsonAndCreatesDifferentFloorSegments() {
        val old=json(3)
        val changed=old.replaceFirst("\"width\": 140", "\"width\": 160")
        assertTrue(changed != old)
        try {
            LevelCatalog.install { n -> if (n == 3) changed else json(n) }
            val w=GameWorld(3)
            assertEquals(160f,w.spec.gaps.single().width,0f)
            assertEquals(460f,w.blocks.first { it.left >= 300f }.left,0f)
            // A wider gap is not automatically solved: the existing SHIFT bridge is still 140px.
        } finally {
            LevelCatalog.useProjectFiles()
        }
    }

    @Test fun invalidRouteReferenceCannotSilentlyProduceAnUnplayableLevel() {
        val old=json(1)
        val bad=old.replace("\"route\": [\n    \"spike_01\"", "\"route\": [\n    \"missing_spike\"", ignoreCase=false)
        var rejected=false
        try { LevelJsonParser.parse(bad,1) } catch (_: IllegalStateException) { rejected=true }
        assertTrue(rejected)
    }
}
