package com.escapeshift.game.game_elements

/** Single place to declare JSON object families; no hidden gameplay objects are spawned. */
object GameElementRegistry {
    val adventure = setOf("wall", "platform", "crate", "bridge", "spike", "spring", "coin", "snowflake", "conveyor", "saw", "monster", "door")
    val bate = setOf("moving_spikes")
    val puzzle = setOf("laser_red", "laser_blue", "laser_timed", "key", "switch", "crate_switch")
    val supported: Set<String> = adventure + bate + puzzle
    val routeTypes = setOf("spike", "moving_spikes", "laser_red", "laser_blue", "laser_timed", "spring", "key", "switch", "conveyor", "saw", "monster", "wall")
    fun family(type: String): String = when (type) {
        in adventure -> "adventure"
        in bate -> "bate"
        in puzzle -> "puzzle"
        else -> error("Unknown game element: $type")
    }
}
