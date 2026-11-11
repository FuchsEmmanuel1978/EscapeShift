package com.escapeshift.game.game_elements.bate

import com.escapeshift.game.HitBox
import com.escapeshift.game.TrapView
import kotlin.math.max
import kotlin.math.min

/**
 * Side-moving, non-lethal "bait" block. Its position is owned by TrapEngine;
 * this module resolves the HERO's horizontal collision against that live body.
 *
 * Movement can originate on either side: a positive travelX pushes to the right,
 * a negative travelX pushes to the left. Colliding with this block does not
 * directly kill the hero, but can displace them over a hole.
 */
object PushBlock {
    fun resolveHorizontal(
        heroBefore: HitBox,
        heroProposed: HitBox,
        blockBefore: List<TrapView>,
        blockNow: List<TrapView>,
        worldRight: Float
    ): Float {
        if (blockNow.isEmpty()) return heroProposed.left
        val width = heroProposed.right - heroProposed.left
        var left = heroProposed.left
        val oldById = blockBefore.associateBy { it.id }
        for (current in blockNow) {
            val box = current.box
            val hero = HitBox(left, heroProposed.top, left + width, heroProposed.bottom)
            if (!hero.intersects(box)) continue
            val old = oldById[current.id]?.box ?: box
            val pushedX = box.left - old.left
            left = when {
                // Hero approached the block from the LEFT: stop at its left face.
                heroBefore.right <= old.left + 1f -> min(left, box.left - width)
                // Hero was to the RIGHT: moving block carries them to its right face.
                heroBefore.left >= old.right - 1f -> max(left, box.right)
                // Block activated while overlapping hero: follow the shove direction.
                pushedX > 0f -> max(left, box.right)
                pushedX < 0f -> min(left, box.left - width)
                // Static overlap fallback, e.g. initial spawn inside the block.
                else -> if (hero.left + width / 2f < box.left + (box.right - box.left) / 2f)
                    min(left, box.left - width) else max(left, box.right)
            }
        }
        return left.coerceIn(0f, (worldRight - width).coerceAtLeast(0f))
    }
}
