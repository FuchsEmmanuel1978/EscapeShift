package com.escapeshift.game

import org.junit.Assert.*
import org.junit.Test

class GamePhysicsTest {
    @Test fun allTenLevelsHaveAnExecutableWinningPath() {
        for (level in 1..10) {
            val result = LevelPlaytester.test(level)
            assertTrue("Niveau $level : ${result.checks.filterNot { it.passed }} / ${result.events.takeLast(4)}", result.passed)
            assertTrue("Niveau $level dure trop longtemps", result.ticks in 1..2500)
        }
    }

    @Test fun noIdenticalCrateIntroAcrossTenLevels() {
        val starts = (1..10).map { LevelCatalog.create(it).intro }
        assertTrue(starts.toSet().size == 5)
        assertTrue(starts.count { it == StartType.CRATE_WALL } == 2)
        assertFalse(GameWorld(1).hasCrate)
        assertTrue(GameWorld(2).hasCrate)
        assertFalse(GameWorld(3).hasCrate)
        assertTrue(GameWorld(9).hasCrate)
    }

    @Test fun worldOneStartsHarderAndTheLaterLevelsStayDemanding() {
        val counts = (1..10).map { LevelCatalog.create(it).challenges.size }
        assertTrue("Level 1 needs at least four obstacles", counts.first() >= 4)
        assertTrue("Level 10 must have more challenges than level 1", counts.last() > counts.first())
        val intro = LevelPlaytester.test(1)
        assertTrue(intro.passed)
        assertTrue("Level 1 should require multiple jumps", intro.jumps >= 5)
        val finale = LevelPlaytester.test(10)
        assertTrue(finale.jumps > intro.jumps)
        assertTrue(finale.ticks > intro.ticks * 1.4)
    }

    @Test fun elevatedExitsRequireThreeVerticalClimbs() {
        for (level in listOf(1, 2, 3, 4, 5, 7, 8)) {
            val world = GameWorld(level)
            val steps = world.spec.objects.filter { it.id.startsWith("exit_stair_") }.sortedBy { it.x }
            assertEquals("Level $level must have three stair platforms", 3, steps.size)
            assertTrue(steps.zipWithNext().all { (a, b) -> b.y < a.y })
            assertTrue(world.exit.bottom < world.spec.groundY - 150f)
            val result = LevelPlaytester.test(level)
            assertTrue("Climb fails in level $level: ${result.checks}", result.passed)
            assertTrue(result.events.count { it.startsWith("stair ") } == 3)
        }
    }

    @Test fun movingHazardsHaveLargePatrolRanges() {
        val all = (1..10).map { GameWorld(it) }
        assertTrue(all.any { world -> world.saws.any { it.amplitude >= 75f } })
        assertTrue(all.any { world -> world.monsters.any { it.amplitude >= 50f } })
        assertTrue(GameWorld(6).saws.isNotEmpty())
        assertTrue(GameWorld(6).monsters.isNotEmpty())
    }

    @Test fun backtrackingOccursOnThreeLevels() {
        for (level in listOf(6,9,10)) {
            val plan = LevelCatalog.create(level)
            assertTrue(plan.backtrackCount>0)
            val result=LevelPlaytester.test(level)
            assertTrue("No direction switch on level $level",result.reversals>0)
            val x = result.csv.lineSequence().drop(1).filter { it.isNotBlank() }
                .map{it.split(',')[2].toFloat()}.toList()
            assertTrue("No movement backwards on $level",x.zipWithNext().any { (a,b) -> b < a - 1f })
        }
    }

    @Test fun newlyIntroducedMechanicsArePresent() {
        assertTrue(GameWorld(2).hasCrate)
        assertTrue(GameWorld(3).bridge != null)
        assertTrue(GameWorld(3).invertedLasers.isNotEmpty())
        assertTrue(GameWorld(4).timedLasers.isNotEmpty())
        assertTrue(GameWorld(5).keys.isNotEmpty())
        assertTrue(GameWorld(7).springs.isNotEmpty())
        assertTrue(GameWorld(7).saws.isNotEmpty())
        assertTrue(GameWorld(8).conveyors.isNotEmpty())
        assertTrue(GameWorld(9).switches.isNotEmpty())
    }

    @Test fun keyAndSwitchAreRequiredBeforeVictory() {
        val l6=GameWorld(6)
        assertTrue(l6.spec.requiresKey)
        assertFalse(l6.exitUnlocked)
        val l9=GameWorld(9)
        assertTrue(l9.spec.requiresSwitch)
        assertFalse(l9.exitUnlocked)
        assertFalse(GameWorld(1).spec.requiresKey)
        assertTrue(GameWorld(1).exitUnlocked)
    }

    @Test fun debugConfigIsExplicitAndLevelsRemainReal() {
        assertTrue(DevConfig.START_LEVEL in 1..10)
        assertTrue(GameProgression.MAX_LEVEL==30)
        assertFalse(GameProgression.isPlayable(9,1))
        assertTrue(GameProgression.isPlayable(9,9))
    }

    @Test fun allVictoryTransitionsAndSavedProgressAreCorrect() {
        var unlocked=1
        for(level in 1..10){
            assertTrue(GameProgression.isPlayable(level,unlocked))
            unlocked=GameProgression.afterWin(unlocked,level)
            assertTrue(unlocked==level+1)
        }
        assertEquals(11, GameProgression.nextLevel(10,unlocked))
        assertTrue(GameProgression.latestPlayable(unlocked)==11)
    }

    @Test fun crateCanBePushedButOnlyWhenPresent() {
        val a=GameWorld(1)
        repeat(70){a.tick(GameInput(right=true))}
        assertFalse(a.hasCrate)
        val b=GameWorld(2)
        repeat(110){b.tick(GameInput(right=true))}
        assertEquals(344f,b.crateX,0.1f)
    }

    @Test fun shiftTogglesOncePerKeyPress() {
        val w=GameWorld(3)
        w.tick(GameInput(shift=true))
        repeat(10){w.tick(GameInput(shift=true))}
        assertTrue(w.shiftOn)
        w.tick(GameInput())
        w.tick(GameInput(shift=true))
        assertFalse(w.shiftOn)
    }

    @Test fun timedGateHasSafeWindow() {
        val w=GameWorld(4);val e=w.timedLasers.first()
        assertTrue((0..119).any{w.timedLaserActive(e,it)})
        assertTrue((0..119).any{!w.timedLaserActive(e,it)})
    }

    @Test fun obstaclesDoNotOverlapInTheSameLevel() {
        for(n in 1..10){
            val w=GameWorld(n)
            val events=w.spec.challenges
            assertTrue(events.zipWithNext().all{(a,b)->b.x - a.x >= 255f})
            assertTrue("Exit on spike in $n",w.spikes.none{it.intersects(w.exit)})
        }
    }

    @Test fun coinsAreAboveOneWayPlatforms() {
        val l5=GameWorld(5)
        assertTrue(l5.coins.isNotEmpty())
        assertTrue(l5.coinsCollected == 0)
        assertTrue(l5.coins.all { coin -> l5.oneWayPlatforms.any { p ->
            coin.right > p.left && coin.left < p.right && coin.bottom < p.top
        } })
        l5.reset()
        assertTrue(l5.coinsCollected == 0)
    }

    @Test fun tallAndDoubleSpikesAppearInLaterLevels() {
        val all=(3..10).flatMap { GameWorld(it).spikes }
        assertTrue(all.any { it.bottom-it.top >= 40f })
        assertTrue((3..10).any { n -> GameWorld(n).spikes.zipWithNext().any { (a,b) -> b.left-a.right in 0f..5f } })
    }

    @Test fun sawsTravelFurtherAndMonstersGuardLateLevels() {
        val l7=GameWorld(7)
        val s=l7.saws.first()
        val left=(0..155).minOf { l7.sawBox(s,it).left }
        val right=(0..155).maxOf { l7.sawBox(s,it).left }
        assertTrue(right-left > 100f)
        for (n in 8..10) {
            val world=GameWorld(n)
            assertTrue(world.monsters.isNotEmpty())
            assertTrue(world.conveyors.isNotEmpty())
            assertTrue(world.monsters.first().anchorX > world.conveyors.first().right)
        }
    }

    @Test fun optionalPlatformsAreNotHorizontalWalls() {
        val l8=GameWorld(8)
        assertTrue(l8.oneWayPlatforms.size>=3)
        assertTrue(l8.solids().none { it in l8.oneWayPlatforms })
    }

    @Test fun collectingCoinUpdatesHudCounterAndResetClearsIt() {
        val game = GameWorld(5)
        val coin = game.coins.first()
        val xField = GameWorld::class.java.getDeclaredField("x").apply { isAccessible=true }
        val yField = GameWorld::class.java.getDeclaredField("y").apply { isAccessible=true }
        xField.setFloat(game, coin.left)
        yField.setFloat(game, coin.top)
        game.tick(GameInput())
        assertTrue(game.coinsCollected == 1)
        assertFalse(game.isCoinVisible(0))
        game.reset()
        assertTrue(game.coinsCollected == 0)
        assertTrue(game.isCoinVisible(0))
    }

    @Test fun heroStompsMonsterFromAboveButSideCollisionIsLethal() {
        val good=GameWorld(8)
        val monster=good.monsterBox(0)
        val xf=GameWorld::class.java.getDeclaredField("x").apply{isAccessible=true}
        val yf=GameWorld::class.java.getDeclaredField("y").apply{isAccessible=true}
        val vf=GameWorld::class.java.getDeclaredField("vy").apply{isAccessible=true}
        xf.setFloat(good, monster.left+8f)
        yf.setFloat(good, monster.top-GameWorld.SIZE-3f)
        vf.setFloat(good, 200f)
        good.tick(GameInput())
        assertFalse(good.dead)
        assertFalse(good.isMonsterAlive(0))
        val bad=GameWorld(8)
        val box=bad.monsterBox(0)
        xf.setFloat(bad, box.left-GameWorld.SIZE+3f)
        yf.setFloat(bad, box.bottom-GameWorld.SIZE)
        vf.setFloat(bad, 0f)
        bad.tick(GameInput())
        assertTrue(bad.dead)
    }

    @Test fun resetClearsTransientState() {
        val w=GameWorld(9)
        repeat(100){w.tick(GameInput(right=true))}
        w.tick(GameInput(shift=true))
        w.reset()
        assertEquals(201f,w.crateX,0.01f)
        assertFalse(w.shiftOn)
        assertFalse(w.keyCollected)
        assertFalse(w.switchActivated)
        assertFalse(w.dead)
        assertTrue(w.ticks==0)
    }

    @Test fun longShiftBridgesAreEssentialToCrossTheGaps() {
        for (level in listOf(2, 3, 6, 9)) {
            val world = GameWorld(level)
            val gap = world.spec.gaps.single()
            assertTrue(gap.width >= 260f)
            assertEquals(gap.width, world.bridge!!.right - world.bridge!!.left, 0.01f)
        }
        // A normal jump, launched at the near edge, cannot cross the level 3 gap.
        // SHIFT bridges that same distance with the regular ground physics.
        val base = LevelCatalog.create(3)
        val gap = base.gaps.single()
        val scenario = base.copy(startX = gap.x - 42f, startY = base.groundY - GameWorld.SIZE)
        val noShift = GameWorld(3, scenario)
        noShift.tick(GameInput())
        repeat(120) { noShift.tick(GameInput(right=true, jump=it==0)) }
        assertFalse(noShift.x >= gap.x + gap.width)
        val withShift = GameWorld(3, scenario)
        withShift.tick(GameInput(shift=true))
        repeat(130) { if (withShift.x < gap.x + gap.width + 12f) withShift.tick(GameInput(right=true)) }
        assertTrue(withShift.x >= gap.x + gap.width + 12f)
        assertFalse(withShift.dead)
    }

    @Test fun worldOneBeltsNeedALongJumpNotJustWalking() {
        for (level in listOf(8,9,10)) {
            val b = GameWorld(level).conveyors.first()
            assertEquals(270f, b.right - b.left, .1f)
        }
        val base=LevelCatalog.create(8)
        val belt=base.objects.first { it.type == "conveyor" }
        val scenario=base.copy(startX=belt.x-45f,startY=base.groundY-GameWorld.SIZE)
        val walking=GameWorld(8,scenario)
        walking.tick(GameInput())
        repeat(150) { walking.tick(GameInput(right=true)) }
        assertTrue(walking.x < belt.x + belt.width - 50f)
        val jumping=GameWorld(8,scenario)
        jumping.tick(GameInput())
        var crossed=false
        for (tick in 0..170) {
            jumping.tick(GameInput(right=true, jump=tick==0))
            if(jumping.x >= belt.x + belt.width + 2f) { crossed=true;break }
            if(jumping.dead)break
        }
        assertTrue(crossed && !jumping.dead)
    }

}
