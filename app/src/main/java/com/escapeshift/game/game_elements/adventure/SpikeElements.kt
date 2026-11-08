package com.escapeshift.game.game_elements.adventure

import com.escapeshift.game.HitBox
import com.escapeshift.game.LevelObject

/** Shared geometry for stationary and moving triangle groups. */
object SpikeElements {
    fun boxes(element: LevelObject, displacementX: Float = 0f): List<HitBox> =
        (0 until element.count).map { index ->
            val left = element.x + displacementX + index * (element.width + element.spacing)
            HitBox(left, element.y, left + element.width, element.y + element.height)
        }
}
