package com.escapeshift.game

import org.junit.Assert.*
import org.junit.Test

class BackgroundParallaxTest {
    @Test fun aspectRatioIsPreserved() {
        val crop = BackgroundParallax.cropWidth(2172, 724, 1000f, 560f)
        assertTrue("Crop should keep image height and viewport ratio", crop in 1292..1294)
        assertTrue("Crop is within picture", crop <= 2172)
    }

    @Test fun backgroundMovesWithHeroEvenWithoutCameraMovement() {
        val first = BackgroundParallax.offset(2172, 724, 1000f, 560f, 78f)
        val walking = BackgroundParallax.offset(2172, 724, 1000f, 560f, 600f)
        val backtracking = BackgroundParallax.offset(2172, 724, 1000f, 560f, 200f)
        assertTrue("Hero must move panorama", walking > first)
        assertTrue("Backtracking must reverse panorama", backtracking < walking)
    }

    @Test fun longLevelsNeverReadOutsideBitmap() {
        val crop = BackgroundParallax.cropWidth(2172, 724, 1000f, 560f)
        for (heroX in listOf(-8000f, 0f, 78f, 1000f, 5000f, 100000f)) {
            val left = BackgroundParallax.offset(2172, 724, 1000f, 560f, heroX)
            assertTrue(left >= 0)
            assertTrue(left + crop <= 2172)
        }
    }

    @Test fun narrowImagesAndInvalidDimensionsAreSafe() {
        assertTrue(BackgroundParallax.cropWidth(200, 400, 1000f, 560f) == 200)
        assertTrue(BackgroundParallax.offset(200, 400, 1000f, 560f, 2500f) == 0)
        assertTrue(BackgroundParallax.cropWidth(0, 0, 1000f, 560f) == 0)
    }
}
