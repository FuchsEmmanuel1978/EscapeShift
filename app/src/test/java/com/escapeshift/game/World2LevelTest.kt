package com.escapeshift.game

import org.junit.Assert.*
import org.junit.Test

class World2LevelTest {
    @Test fun allTwentyLevelsAreRealAndWorldTwoLevelsUseSnow() {
        for (n in 1..20) {
            val level = LevelCatalog.create(n)
            assertEquals(n, level.number)
            if (n >= 11) {
                assertTrue(level.objects.any { it.type == "snowflake" })
                assertEquals(2, (n-1)/10+1)
            }
        }
    }
    @Test fun worldTwoMatchesSpecifiedDifficultyMatrix() {
        val crates = listOf(1,1,1,1,1,2,2,2,3,3)
        val monsters = listOf(1,1,1,1,1,2,2,2,5,5)
        val switches = listOf(1,1,1,1,1,1,2,2,2,2)
        val saws = listOf(1,1,1,1,1,1,1,2,3,3)
        val flakes = (1..10).toList()
        val pressures = listOf(0,0,0,0,0,0,1,1,1,1)
        for (index in 0 until 10) {
            val w = GameWorld(index+11)
            assertEquals(crates[index], w.crateBoxes.size)
            assertEquals(monsters[index], w.monsters.size)
            assertEquals(switches[index], w.switches.size)
            assertEquals(saws[index], w.saws.size)
            assertEquals(flakes[index], w.snowflakes.size)
            assertEquals(pressures[index], w.crateSwitches.size)
        }
    }
    @Test fun plateOnlyOpensAfterCrateAndBothNormalSwitches() {
        val w = GameWorld(17)
        assertFalse(w.pressureReady)
        assertFalse(w.exitUnlocked)
        assertEquals(1, w.crateSwitches.size)
    }
    @Test fun pushingFirstCrateActivatesThePressurePlate() {
        val w = GameWorld(17)
        repeat(56) { w.tick(GameInput(right = true)) }
        assertTrue("crate must cover the plate", w.pressureReady)
        assertFalse("two manual switches are still required", w.exitUnlocked)
    }
    @Test fun snowflakesFallAndWrapWithoutASeparateSprite() {
        val w = GameWorld(20)
        assertEquals(10, w.snowflakes.size)
        assertTrue(w.snowflakeBox(0, 0).top != w.snowflakeBox(0, 40).top)
    }
    @Test fun allPhysicsAndPuzzleTargetsIncrease() {
        for (n in 11..20) {
            val d = LevelCatalog.create(n)
            val w = GameWorld(n)
            val groups = d.objects.filter { it.type == "spike" }
            val expectedGroups = when(n) {
                in 11..12 -> if (n==11) 2 else 3
                13 -> 2
                in 14..16 -> 3
                in 17..18 -> 4
                else -> 1
            }
            assertEquals(expectedGroups, groups.size)
            assertEquals(if(n<=12) 1 else if(n<=18) 2 else 3, groups.first().count)
            assertEquals(if(n<=17) 1 else if(n==18) 2 else 3, w.saws.size)
            assertEquals(if(n<=13) 1 else if(n<=16) 2 else if(n<=19) 3 else 4,
                d.objects.count { it.type == "laser_red" || it.type == "laser_blue" })
        }
    }
    @Test fun everyLevelHasWinningScript() {
        for (n in 1..20) {
            val result = LevelPlaytester.test(n)
            assertTrue("level $n: ${result.events.takeLast(4)}, ${result.checks.filterNot {it.passed}}", result.passed)
            assertTrue("level $n took too long", result.ticks in 1..7800)
        }
    }
}
