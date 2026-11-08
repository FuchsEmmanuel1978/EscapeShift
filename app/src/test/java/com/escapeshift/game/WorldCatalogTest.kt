package com.escapeshift.game

import org.junit.Assert.*
import org.junit.Test

class WorldCatalogTest {
    @Test fun tenWorldsWithTenSlotsEach() {
        assertEquals(10, WorldCatalog.worlds.size)
        assertEquals(100, WorldCatalog.totalSlots())
        assertTrue(WorldCatalog.worlds.map { it.number } == (1..10).toList())
    }
    @Test fun previewsOutsideImplementedWorlds() {
        for (world in 1..10) for (level in 1..10)
            assertEquals(world <= 2 || (world == 3 && level <= 10) || (world > 3 && level == 1), WorldCatalog.isPlayable(world, level))
        for (world in 4..10) assertTrue(WorldCatalog.isPreviewLevel((world - 1) * 10 + 1))
        assertFalse(WorldCatalog.isPreviewLevel(11))
        assertFalse(WorldCatalog.isPreviewLevel(21))
        assertFalse(WorldCatalog.isPreviewLevel(12))
    }
    @Test fun swipeClampsAndRejectsVerticalMovements() {
        assertEquals(1, WorldCatalog.swipe(1, 100f, 2f))
        assertEquals(2, WorldCatalog.swipe(1, -100f, 2f))
        assertEquals(1, WorldCatalog.swipe(1, -25f, 2f))
        assertEquals(3, WorldCatalog.swipe(3, 200f, 300f))
        assertEquals(10, WorldCatalog.swipe(10, -200f, 10f))
        assertEquals(9, WorldCatalog.swipe(10, 200f, 10f))
    }
    @Test fun worldOnePhysicsStillHasTenExistingRoutes() {
        for (i in 1..10) assertEquals(i, LevelCatalog.create(i).number)
    }
}
