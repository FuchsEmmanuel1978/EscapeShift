package com.escapeshift.game

import org.junit.Assert.*
import org.junit.Test

class WorldPreviewTest {
    @Test fun showcasePreviewFilesAreIdenticalGeometryToWorldOne() {
        LevelCatalog.useProjectFiles()
        val base = LevelCatalog.create(1)
        for (world in listOf(2,4,5,6,7,8,9,10)) {
            val n = (world - 1) * 10 + 1
            val copy = WorldPreviewCatalog.create(n)
            assertEquals(n, copy.number)
            assertEquals(base.worldRight, copy.worldRight, 0.001f)
            assertEquals(base.gaps, copy.gaps)
            assertEquals(base.objects, copy.objects)
            assertEquals(base.challenges, copy.challenges)
        }
    }
    @Test fun originalLevel11IsNotReplaced() {
        val original = LevelCatalog.create(11)
        val preview = WorldPreviewCatalog.create(11)
        assertTrue(original.objects.size > preview.objects.size)
        assertEquals(11, original.number)
        assertEquals(11, preview.number)
    }
}
