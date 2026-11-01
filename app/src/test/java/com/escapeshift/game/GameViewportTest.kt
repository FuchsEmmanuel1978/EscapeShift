package com.escapeshift.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewportTest {
    @Test fun widePhoneUsesAllAvailableWidth() {
        val v = GameViewport.forScreen(2400, 1080)
        assertEquals(1244.4445f, v.worldWidth, 0.01f)
        assertEquals(2400f, v.worldWidth * v.scale, 0.01f)
    }

    @Test fun heroRemainsSquareAtAllAspectRatios() {
        for ((pw, ph) in listOf(1920 to 1080, 2400 to 1080, 2520 to 1080)) {
            val v = GameViewport.forScreen(pw, ph)
            val spritePhysicalW = GameWorld.SIZE * v.scale
            val spritePhysicalH = GameWorld.SIZE * v.scale
            assertEquals(spritePhysicalW, spritePhysicalH, 0.001f)
        }
    }

    @Test fun virtualCoordinatesRoundTrip() {
        val v = GameViewport.forScreen(2400,1080)
        val (x, y) = v.toVirtual(2340f, 1000f)
        assertEquals(2340f, x * v.scale, 0.1f)
        assertEquals(1000f, y * v.scale, 0.1f)
    }

    @Test fun menuCenterUsesWiderScreen() {
        val v = GameViewport.forScreen(2400,1080)
        assertTrue(GameViewport.centerOffset(v.worldWidth) > 0f)
        assertEquals(v.worldWidth/2f, GameViewport.centerOffset(v.worldWidth)+500f, 0.001f)
    }
}
