package com.escapeshift.game.game_elements.puzzle

import com.escapeshift.game.HitBox

/** Canonical SHIFT laser activation, used by collisions and rendering. */
object LaserElements {
    fun redActive(shiftOn: Boolean) = !shiftOn
    fun blueActive(shiftOn: Boolean) = shiftOn
    fun isDangerous(red: List<HitBox>, blue: List<HitBox>, player: HitBox, shiftOn: Boolean): Boolean =
        (redActive(shiftOn) && red.any(player::intersects)) ||
        (blueActive(shiftOn) && blue.any(player::intersects))
}
