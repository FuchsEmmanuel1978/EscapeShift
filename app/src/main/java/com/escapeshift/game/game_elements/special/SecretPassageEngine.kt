package com.escapeshift.game

/** A secret wall is solid until the selected in-game action opens it. */
data class SecretPassageDefinition(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val activation: String = "interact",
    val triggerRadius: Float = 75f,
    val showCracks: Boolean = true
) {
    val box: HitBox get() = HitBox(x, y, x + width, y + height)
}

data class SecretPassageView(
    val id: String,
    val box: HitBox,
    val isOpen: Boolean,
    val showCracks: Boolean
)

/** State only lasts until the level resets. Discovered secrets are persisted by GameView. */
class SecretPassageEngine(private val definitions: List<SecretPassageDefinition>) : GameMechanic {
    private val opened = mutableSetOf<String>()
    private val newlyOpened = mutableListOf<String>()

    override fun reset() {
        opened.clear()
        newlyOpened.clear()
    }

    override fun update(context: MechanicContext) {
        for (secret in definitions) {
            if (secret.id in opened) continue
            val region = secret.box
            val range = secret.triggerRadius
            val near = context.player.intersects(HitBox(
                region.left - range, region.top - range,
                region.right + range, region.bottom + range
            ))
            val activated = when (secret.activation) {
                "interact" -> near && context.interactPressed
                "shift" -> near && context.shiftEnabled
                "crate_touch", "push_crate" -> context.crates.any { crate ->
                    crate.intersects(HitBox(region.left - 10f, region.top - 10f,
                        region.right + 10f, region.bottom + 10f))
                }
                "switch_active" -> context.switchActivated
                else -> false // JSON validation rejects unknown activations.
            }
            if (activated) {
                opened.add(secret.id)
                newlyOpened.add(secret.id)
            }
        }
    }

    override fun solids(): List<HitBox> = definitions.filterNot { it.id in opened }.map { it.box }

    fun views(): List<SecretPassageView> = definitions.map {
        SecretPassageView(it.id, it.box, it.id in opened, it.showCracks)
    }

    /** Called once per simulation step; a discovery is never counted twice per attempt. */
    fun drainNewDiscoveries(): List<String> = newlyOpened.toList().also { newlyOpened.clear() }

    fun openedCount(): Int = opened.size
    fun totalCount(): Int = definitions.size
}
