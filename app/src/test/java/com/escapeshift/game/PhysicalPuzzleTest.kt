package com.escapeshift.game

import org.junit.Assert.*
import org.junit.Test

class PhysicalPuzzleTest {
    @Test fun threePhysicalPuzzlesLoadFromJsonWithRealColliders() {
        for (level in 21..23) {
            val world = GameWorld(level)
            assertNotNull(world.physicalPuzzle)
            assertFalse("Door at level $level must start locked", world.exitUnlocked)
        }
        assertEquals(1, GameWorld(21).crateBoxes.size)
        assertNotNull(GameWorld(21).physicalPuzzle!!.sliderBox)
        assertNotNull(GameWorld(22).physicalPuzzle!!.wheelBox)
        assertNotNull(GameWorld(22).physicalPuzzle!!.laserBox)
        assertEquals(2, GameWorld(23).crateBoxes.size)
        assertEquals(3, GameWorld(23).physicalPuzzle!!.movingPlatforms().size)
    }

    @Test fun aBridgeMustBePhysicallyPushedBeforeItCanBeUsed() {
        val world = GameWorld(21)
        val bridge = world.physicalPuzzle!!
        val x0 = bridge.sliderBox!!.left
        repeat(15) { world.tick(GameInput(right = true)) }
        assertTrue(bridge.sliderBox!!.left >= x0)
        assertFalse(world.exitUnlocked)
        repeat(48) { world.tick(GameInput(right = true)) }
        assertTrue(bridge.isBridgePlaced)
        assertFalse("Bridge alone must not unlock the exit", world.exitUnlocked)
    }

    @Test fun wheelLaserWindowStartsOnlyWhenWheelTouchesButtonAndExpires() {
        val world = GameWorld(22)
        val puzzle = world.physicalPuzzle!!
        assertTrue(puzzle.laserActive)
        repeat(120) { world.tick(GameInput(right = true)) }
        assertFalse(puzzle.laserActive)
        assertTrue(puzzle.laserRemainingTicks > 0)
        // One step too late: despite the wheel still resting on the plate the
        // laser must reactivate, preventing a permanently open gate.
        repeat(123) { world.tick(GameInput()) }
        assertTrue(puzzle.laserActive)
        assertFalse(world.exitUnlocked)
    }

    @Test fun wrongPairDoesNotSynchronizeMovingPlatforms() {
        val puzzle = GameWorld(23).physicalPuzzle!!
        // Simulation of an incorrect placement (1 + 2), then the correct one (1 + 4).
        val pad1 = puzzle.buttonBoxes[0]
        val pad2 = puzzle.buttonBoxes[1]
        val pad4 = puzzle.buttonBoxes[3]
        fun on(p: HitBox): HitBox = HitBox(p.left + 10f, 401f, p.left + 58f, 444f)
        puzzle.update(HitBox(0f, 405f, 39f, 444f), listOf(on(pad1), on(pad2)))
        assertFalse(puzzle.isSynced)
        puzzle.update(HitBox(0f, 405f, 39f, 444f), listOf(on(pad1), on(pad4)))
        assertTrue(puzzle.isSynced)
        assertFalse(puzzle.isSolved)
        puzzle.onLandMovingPlatform(1)
        assertEquals(0, puzzle.landedCount)
        for (index in 0..2) puzzle.onLandMovingPlatform(index)
        assertTrue(puzzle.isSolved)
    }
}
