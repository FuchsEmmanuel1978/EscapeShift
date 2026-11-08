package com.escapeshift.game.game_elements.adventure

import com.escapeshift.game.HitBox

/**
 * Visual forgiveness for triangle hazards only.
 * Does not alter player physics, platform/ground contact, or JSON coordinates.
 * Tune margins in world pixels. Use 0f for the old, strict rectangle behavior.
 */
object SpikeCollision {
    const val HERO_HORIZONTAL_MARGIN = 6f
    const val HERO_TOP_MARGIN = 4f
    const val HERO_BOTTOM_MARGIN = 3f
    const val SPIKE_HORIZONTAL_MARGIN = 3f
    const val SPIKE_TOP_MARGIN = 5f

    fun touches(hero: HitBox, spike: HitBox): Boolean {
        val heroLeft = hero.left + HERO_HORIZONTAL_MARGIN
        val heroRight = hero.right - HERO_HORIZONTAL_MARGIN
        val heroTop = hero.top + HERO_TOP_MARGIN
        val heroBottom = hero.bottom - HERO_BOTTOM_MARGIN
        val spikeLeft = spike.left + SPIKE_HORIZONTAL_MARGIN
        val spikeRight = spike.right - SPIKE_HORIZONTAL_MARGIN
        val spikeTop = spike.top + SPIKE_TOP_MARGIN
        return heroLeft < heroRight && heroTop < heroBottom &&
            spikeLeft < spikeRight && spikeTop < spike.bottom &&
            heroLeft < spikeRight && heroRight > spikeLeft &&
            heroTop < spike.bottom && heroBottom > spikeTop
    }
}
