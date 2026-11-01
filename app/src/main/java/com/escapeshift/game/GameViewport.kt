package com.escapeshift.game

/** Keeps a fixed 560-unit game height while adapting the visible width to the phone.
 * Geometry remains square: an object measuring 39x39 stays square on 16:9 and 20:9 screens.
 * No Android dependency, so the coordinate math can be unit tested on a PC.
 */
data class GameViewport(val worldWidth: Float, val scale: Float) {
    companion object {
        const val HEIGHT = 560f
        const val DESIGN_WIDTH = 1000f
        fun forScreen(widthPx: Int, heightPx: Int): GameViewport {
            if (widthPx <= 0 || heightPx <= 0) return GameViewport(DESIGN_WIDTH, 1f)
            val zoom = heightPx.toFloat() / HEIGHT
            return GameViewport(widthPx.toFloat() / zoom, zoom)
        }
        fun centerOffset(visibleWidth: Float) = (visibleWidth - DESIGN_WIDTH) / 2f
    }
    fun toVirtual(xPx: Float, yPx: Float) = Pair(xPx / scale, yPx / scale)
}
