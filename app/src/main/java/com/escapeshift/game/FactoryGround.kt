package com.escapeshift.game

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Foreground-only tiled floor. Missing tiles in JSON ground.gaps make real holes. */
class FactoryGround(context: Context) {
    private val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ground_factory_tile)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

    fun draw(c: Canvas, spec: LevelSpec, screenWorldWidth: Float, screenWorldHeight: Float,
             visibleGaps: List<GroundGap> = spec.gaps) {
        val tileWidth = screenWorldWidth * GameVisualConfig.GROUND_TILE_SCREEN_FRACTION
        val tileHeight = screenWorldHeight * GameVisualConfig.GROUND_TILE_HEIGHT_FRACTION
        val gaps = visibleGaps.sortedBy { it.x }
        var cursor = 0f
        for (gap in gaps) {
            drawSegment(c, cursor, gap.x, spec.groundY, tileWidth, tileHeight)
            cursor = gap.x + gap.width
        }
        drawSegment(c, cursor, spec.worldRight, spec.groundY, tileWidth, tileHeight)
    }

    private fun drawSegment(c: Canvas, l: Float, r: Float, y: Float, tileWidth: Float,
                            tileHeight: Float) {
        if (r <= l) return
        // Align sprites to a global grid, then crop when a segment begins or ends in a tile.
        // This prevents stretched tiles and keeps edge transitions seamless.
        val first = floor(l / tileWidth).toInt()
        val last = ceil(r / tileWidth).toInt()
        for (i in first until last) {
            val tileL = i * tileWidth
            val visibleL = max(l, tileL)
            val visibleR = min(r, tileL + tileWidth)
            if (visibleR <= visibleL) continue
            val srcL = ((visibleL - tileL) / tileWidth * bitmap.width).toInt().coerceIn(0, bitmap.width)
            val srcR = ((visibleR - tileL) / tileWidth * bitmap.width).toInt().coerceIn(srcL + 1, bitmap.width)
            val src = Rect(srcL, 0, srcR, bitmap.height)
            val dst = RectF(visibleL, y, visibleR, y + tileHeight)
            c.drawBitmap(bitmap, src, dst, paint)
        }
    }
}
