package com.escapeshift.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * Integration tests: only joystick, jump and ACTION inputs through GameWorld.tick().
 * No direct puzzle-state manipulation or teleportation is used to complete a level.
 * The scripted routes demonstrate one winning trajectory, not an exhaustive solver.
 */
class World3AdvancedPhysicalTest {
    private fun step(g: GameWorld, left: Boolean = false, right: Boolean = false, jump: Boolean = false, action: Boolean = false) {
        g.tick(GameInput(left = left, right = right, jump = jump, interact = action))
        assertFalse("Level ${g.levelNumber} died at x=${g.x}, y=${g.y}", g.dead)
    }

    private fun action(g: GameWorld) { step(g, action = true); step(g) }

    private fun land(g: GameWorld) {
        repeat(95) {
            if (g.grounded) return
            step(g)
        }
        fail("Level ${g.levelNumber} did not land at x=${g.x},y=${g.y}")
    }

    private fun go(g: GameWorld, x: Float, jump: Boolean = false, avoidCrates: Boolean = false,
                   jumpWhen: (GameWorld) -> Boolean = { false }) {
        var jumped = false
        var previousJump = false
        var stuckTicks = 0
        var previousX = g.x
        repeat(1300) {
            if (abs(g.x - x) < 4.9f || g.won) return
            val moveRight = g.x < x
            val nearCrate = avoidCrates && !g.carryingCrate && g.grounded && g.crateBoxes.any { b ->
                if (moveRight) b.left - (g.x + GameWorld.SIZE) in -8f..45f
                else g.x - b.right in -8f..50f
            }
            val desiredJump = (jump && !jumped && g.grounded) || nearCrate || jumpWhen(g) || (stuckTicks > 12 && g.grounded)
            val fireJump = desiredJump && !previousJump
            if (fireJump) jumped = true
            previousJump = desiredJump
            val forwardInput = moveRight != (g.advancedPuzzle?.controlsReversed == true)
            step(g, left = !forwardInput, right = forwardInput, jump = fireJump)
            stuckTicks = if (abs(g.x - previousX) < .6f) stuckTicks + 1 else 0
            previousX = g.x
        }
        fail("Level ${g.levelNumber} stuck at x=${g.x}, goal=$x")
    }

    private fun complete(g: GameWorld) {
        assertTrue("Level ${g.levelNumber}: door remains locked, ${g.advancedPuzzle?.feedback}", g.exitUnlocked)
        assertTrue("Level ${g.levelNumber}: no physical victory", g.won)
        assertFalse(g.dead)
    }

    private fun deliver(g: GameWorld, crateId: String, padId: String) {
        val plan = g.advancedPuzzle!!.plan
        val index = plan.all("crate").indexOfFirst { it.id == crateId }
        assertTrue(index >= 0)
        val b = g.crateBoxes[index]
        val clusteredLeft = g.crateBoxes.withIndex().any { (j, other) ->
            j != index && other.right <= b.left + 2f && b.left - other.right < 24f
        }
        val pickupPosition = if (clusteredLeft) b.right + 12f else b.left - 44f
        go(g, pickupPosition, avoidCrates = true)
        land(g)
        action(g)
        assertTrue("Could not pick $crateId", g.carryingCrate)
        go(g, plan.find(padId)!!.x - 49f, avoidCrates = true)
        land(g)
        action(g)
        assertFalse("Could not drop $crateId", g.carryingCrate)
    }

    @Test fun level24TwoCratesAndHeroWeightThenSpring() {
        val g = GameWorld(24)
        assertFalse(g.exitUnlocked)
        go(g, 155f)
        go(g, 375f, jumpWhen = { it.grounded && it.x in 155f..180f })
        land(g); action(g)
        assertTrue(g.carryingCrate)
        go(g, 526f); action(g)
        go(g, 175f, avoidCrates = true)
        land(g); action(g)
        assertTrue(g.carryingCrate)
        go(g, 618f, avoidCrates = true, jumpWhen = { it.grounded && it.x in 475f..510f })
        land(g); action(g)
        go(g, 540f, avoidCrates = true)
        land(g); action(g)
        assertTrue(g.carryingCrate)
        go(g, 1616f); action(g)
        assertTrue(g.advancedPuzzle!!.isSolved)
        go(g, 2480f)
        complete(g)
    }

    @Test fun level25TenCratesFiveCodePads() {
        val g = GameWorld(25)
        assertFalse(g.exitUnlocked)
        for ((crate, pad) in listOf(
            "crate02" to "pad1", "crate04" to "pad2", "crate06" to "pad3",
            "crate08" to "pad4", "crate10" to "pad5"
        )) deliver(g, crate, pad)
        assertTrue(g.advancedPuzzle!!.isSolved)
        go(g, 2480f, avoidCrates = true)
        complete(g)
    }

    @Test fun level26WheelAngle135ThenJump() {
        val g = GameWorld(26)
        assertFalse(g.exitUnlocked)
        go(g, 350f)
        repeat(3) { action(g) }
        assertEquals(135, g.advancedPuzzle!!.wheelAngle("wheelA"))
        go(g, 2480f, jumpWhen = { it.grounded && it.x in 1515f..1545f })
        complete(g)
    }

    @Test fun level27PrimesInOrderAndAvoidComposites() {
        val g = GameWorld(27)
        assertFalse(g.exitUnlocked)
        go(g, 140f); land(g); go(g, 300f, jump = true); land(g)
        go(g, 465f); land(g); go(g, 635f, jump = true); land(g)
        go(g, 798f); land(g); go(g, 960f, jump = true); land(g)
        go(g, 1129f); land(g); go(g, 1260f, jump = true); land(g)
        assertEquals("primes:7/7", g.advancedPuzzle!!.sequenceStatus)
        go(g, 1610f, jump = true); land(g)
        go(g, 1860f, jump = true); land(g)
        go(g, 1920f); land(g)
        go(g, 2270f, jump = true); land(g)
        assertFalse(g.advancedPuzzle!!.hasFailed)
        go(g, 2480f)
        complete(g)
    }

    @Test fun level28WallsAndMovableSpring() {
        val g = GameWorld(28)
        assertFalse(g.exitUnlocked)
        deliver(g, "crateA", "plateB")
        deliver(g, "crateB", "plateA")
        val spring = g.advancedPuzzle!!.plan.find("springA")!!
        go(g, spring.x - 40f, avoidCrates = true)
        go(g, spring.targetX - 40f, avoidCrates = true)
        assertTrue(g.advancedPuzzle!!.isSolved)
        go(g, 2480f)
        complete(g)
    }

    @Test fun level29CollectOverInOrder() {
        val g = GameWorld(29)
        assertFalse(g.exitUnlocked)
        go(g, 2480f, jumpWhen = {
            it.grounded && (it.x in 500f..535f || it.x in 1060f..1095f || it.x in 1310f..1350f)
        })
        assertEquals("over:4/4", g.advancedPuzzle!!.sequenceStatus)
        complete(g)
    }

    @Test fun level30IQTokensThenGravityAndReversedControls() {
        val g = GameWorld(30)
        assertFalse(g.exitUnlocked)
        go(g, 1370f); land(g)
        go(g, 1590f, jump = true); land(g)
        go(g, 1690f); land(g)
        go(g, 1930f, jump = true)
        go(g, 2080f)
        go(g, 2480f)
        assertEquals("iq_letters:1/1 iq_numbers:1/1", g.advancedPuzzle!!.sequenceStatus)
        complete(g)
    }

    @Test fun allWorldThreeLevelsStartLockedAndWorldFourRemainsPrototypes() {
        for (n in 24..30) {
            val g = GameWorld(n)
            assertNotNull("Missing physical puzzle n=$n", g.advancedPuzzle)
            assertFalse("Level $n unlocked without solving puzzle", g.exitUnlocked)
            assertTrue(WorldCatalog.isPlayable(3, n - 20))
        }
        for (n in 31..40) {
            if (n == 31) assertTrue(WorldCatalog.isPreviewLevel(n))
            else assertFalse(WorldCatalog.isAvailableLevel(n))
            assertNull(GameWorld(n).advancedPuzzle)
        }
    }
}
