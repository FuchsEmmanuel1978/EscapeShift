package com.escapeshift.game

/** Pure Kotlin snapshot; game mechanisms do not depend on Android or Canvas. */
data class MechanicContext(
    val player: HitBox,
    val crates: List<HitBox>,
    val shiftEnabled: Boolean,
    val interactPressed: Boolean,
    val switchActivated: Boolean
)

/** Optional add-on behavior: never mutate existing gravity, jumping, or game inputs. */
interface GameMechanic {
    fun reset()
    fun update(context: MechanicContext)
    fun solids(): List<HitBox> = emptyList()
}

/** In future lots, append other mechanics to this registry without branching GamePhysics. */
class GameMechanicRegistry(private val mechanics: List<GameMechanic>) {
    fun reset() = mechanics.forEach { it.reset() }
    fun update(context: MechanicContext) = mechanics.forEach { it.update(context) }
    fun solids(): List<HitBox> = mechanics.flatMap { it.solids() }
}
