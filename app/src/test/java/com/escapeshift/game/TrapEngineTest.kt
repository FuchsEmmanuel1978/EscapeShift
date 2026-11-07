package com.escapeshift.game

import org.junit.Assert.*
import org.junit.Test

class TrapEngineTest {
    private val player = HitBox(106f, 400f, 135f, 444f)

    @Test fun collapseReopensOnReset() {
        val engine = TrapEngine(listOf(TrapDefinition("f", "collapse_floor", 100f, 444f, 110f, 146f,
            trigger = "player_on", delayTicks = 3)))
        assertEquals(1, engine.solids().size)
        repeat(5) { engine.update(player, player, false, 300f) }
        assertTrue(engine.solids().isEmpty())
        assertEquals(1, engine.collapsedGaps().size)
        engine.reset()
        assertEquals(1, engine.solids().size)
    }

    @Test fun risingSpikesNeedTriggerAndDelay() {
        val engine = TrapEngine(listOf(TrapDefinition("s", "rising_spikes", 100f, 412f, 55f, 32f,
            delayTicks = 3)))
        assertTrue(engine.spikes().isEmpty())
        repeat(5) { engine.update(player, player, false, 300f) }
        assertEquals(1, engine.spikes().size)
    }

    @Test fun movingExitUsesSamePositionForCollisionAndDrawing() {
        val engine = TrapEngine(listOf(TrapDefinition("e", "moving_exit", 100f, 350f, 65f, 90f,
            delayTicks = 1, travelX = 60f, transitionTicks = 5)))
        repeat(10) { engine.update(player, player, false, 300f) }
        assertEquals(160f, engine.exit(HitBox(100f,350f,165f,440f)).left, 0.01f)
        assertEquals(160f, engine.views().single().box.left, 0.01f)
    }

    @Test fun allWorldOneLevelsHaveEventTrapsAndWorkingRoutes() {
        for (level in 1..10) {
            assertEquals(2, GameWorld(level).dynamicTrapViews.size)
            assertTrue("Level $level", LevelPlaytester.test(level).passed)
        }
    }
}
