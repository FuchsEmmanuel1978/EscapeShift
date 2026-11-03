package com.escapeshift.game

import org.junit.Assert.*
import org.junit.Test

class World1AdventureTest {
    @Test fun tenDifferentRoomsAndThreeZones() {
        assertEquals(10, EscapeStory.rooms.size)
        assertEquals(3, EscapeStory.rooms.map {it.zone}.toSet().size)
        assertTrue(EscapeStory.rooms.any {it.puzzle == EscapePuzzle.CODE})
        assertTrue(EscapeStory.rooms.any {it.puzzle == EscapePuzzle.SYMBOLS})
        assertTrue(EscapeStory.rooms.any {it.puzzle == EscapePuzzle.COMBINATION})
    }
    @Test fun invalidCodeCannotOpenDoor() {
        val a = World1Adventure().apply {enter(1)}
        "0000".forEach {a.digit(it.digitToInt(), true)}
        assertFalse(a.isSolved())
        "3142".forEach {a.digit(it.digitToInt(), true)}
        assertTrue(a.isSolved())
    }
    @Test fun wrongItemGivesFeedback() {
        val a=World1Adventure().apply {enter(3);collect(true)}
        a.use("inconnu",true)
        assertFalse(a.isSolved())
        assertTrue(a.message.contains("Choisis"))
        a.use("pince",false)
        assertTrue(a.message.contains("Approche"))
        a.use("pince",true)
        assertTrue(a.isSolved())
    }
    @Test fun combinationRequiresTwoDifferentItems() {
        val a=World1Adventure().apply {enter(1);"3142".forEach{digit(it.digitToInt(),true)};enter(2);collect(true)}
        a.combine("badge","badge")
        assertFalse("passepartout" in a.inventory)
        a.combine("badge","crochet")
        assertTrue("passepartout" in a.inventory)
        a.use("passepartout",true)
        assertTrue(a.isSolved())
    }
    @Test fun finalRoomNeedsCombinedModuleAndCode() {
        val a=World1Adventure().apply {enter(10);inventory.add("puce");inventory.add("aimant")}
        "4207".forEach{a.digit(it.digitToInt(),true)}
        assertFalse(a.isSolved())
        a.combine("puce","aimant")
        a.use("cle_coeur",true)
        "4207".forEach{a.digit(it.digitToInt(),true)}
        assertTrue(a.isSolved())
    }
    @Test fun migrationToAdventureStartsAtFirstUnfinishedRoom() {
        val a = World1Adventure()
        assertEquals(1, a.latestUnfinished())
        a.enter(1)
        "3142".forEach{a.digit(it.digitToInt(),true)}
        assertTrue(a.markCompleted(1))
        val recovered=World1Adventure().apply{restore(a.serialize())}
        assertEquals(2, recovered.latestUnfinished())
    }
    @Test fun inventoryAndRoomProgressAreSaved() {
        val original=World1Adventure().apply {enter(1);"3142".forEach{digit(it.digitToInt(),true)}}
        val recovered=World1Adventure().apply{restore(original.serialize())}
        assertTrue(recovered.isSolved(1))
        assertTrue("badge" in recovered.inventory)
    }
    @Test fun narrativeLockBlocksPhysicsExit() {
        val w=GameWorld(1)
        w.adventureExitReady=false
        assertFalse(w.exitUnlocked)
        w.adventureExitReady=true
        assertTrue(w.exitUnlocked)
    }
}
