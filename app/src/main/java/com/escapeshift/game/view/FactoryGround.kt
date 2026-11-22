package com.escapeshift.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * World 1 modular industrial floor (visual layer only).
 *
 * Collision/holes remain owned by GameWorld and the JSON/TrapEngine. Every segment
 * is clipped in world coordinates and sampled on a GLOBAL tile grid: a concealed
 * collapse-floor trap and the permanent floor therefore use exactly the same
 * texture, with no revealing seam or decoration change before the collapse.
 *
 * On collapse, the exposed solid segment edges are fitted automatically, while
 * the released floor graphic and its anchored foreground decorations fall.
 */
class FactoryGround(context: Context) {
    private val center = BitmapFactory.decodeResource(context.resources, R.drawable.factory_ground_center)
    private val centerB = BitmapFactory.decodeResource(context.resources, R.drawable.factory_ground_center_b)
    private val leftEdge = BitmapFactory.decodeResource(context.resources, R.drawable.factory_ground_edge_left)
    private val rightEdge = BitmapFactory.decodeResource(context.resources, R.drawable.factory_ground_edge_right)
    private val slopeUp = BitmapFactory.decodeResource(context.resources, R.drawable.factory_slope_up)
    private val slopeDown = BitmapFactory.decodeResource(context.resources, R.drawable.factory_slope_down)

    private val props: List<Bitmap> = listOf(
        R.drawable.factory_front_pipe,
        R.drawable.factory_front_generator,
        R.drawable.factory_front_vent,
        R.drawable.factory_front_panel,
        R.drawable.factory_front_lamp,
        R.drawable.factory_front_columns,
        R.drawable.factory_front_cables
    ).map { BitmapFactory.decodeResource(context.resources, it) }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private data class Span(val start: Float, val end: Float)

    /**
     * IMPORTANT: visibleGaps = permanent gaps + traps that have ALREADY collapsed.
     * viewLeft is the world-coordinate camera start, used only for render culling.
     * The graphics do NOT alter collisions and do not encode hints for hidden traps.
     */
    fun draw(
        c: Canvas,
        spec: LevelSpec,
        screenWorldWidth: Float,
        screenWorldHeight: Float,
        visibleGaps: List<GroundGap> = spec.gaps,
        viewLeft: Float = 0f
    ) {
        val tileWidth = screenWorldWidth * GameVisualConfig.GROUND_TILE_SCREEN_FRACTION
        val tileHeight = screenWorldHeight * GameVisualConfig.GROUND_TILE_HEIGHT_FRACTION
        if (tileWidth <= 1f || tileHeight <= 1f) return
        val viewRight = viewLeft + screenWorldWidth
        // Split at terrain transitions so the art and the collision share a single surface.
        val boundaries = (listOf(0f, spec.worldRight) + spec.slopes.flatMap { listOf(it.x, it.end) }).distinct().sorted()
        for (segment in solidSpans(spec.worldRight, visibleGaps)) {
            val cuts = (listOf(segment.start, segment.end) + boundaries.filter { it > segment.start && it < segment.end }).sorted()
            for (i in 0 until cuts.lastIndex) {
                val l = cuts[i]; val r = cuts[i + 1]
                if (r <= l) continue
                val ramp = spec.slopes.firstOrNull { l >= it.x - .01f && r <= it.end + .01f }
                if (ramp == null) {
                    val y = GroundTerrain.surface(spec, (l+r)*.5f)
                    paintSpan(c, l, r, y, tileWidth, tileHeight, viewLeft, viewRight)
                } else {
                    val leftY = GroundTerrain.surface(spec, l)
                    val rightY = GroundTerrain.surface(spec, r)
                    val image = if (ramp.rise < 0f) slopeUp else slopeDown
                    // Slope sprites include a diagonal 3D curb and a textured face.
                    c.save()
                    c.clipRect(l, min(leftY,rightY), r, max(leftY,rightY)+tileHeight)
                    c.drawBitmap(image, null, RectF(l, min(leftY,rightY), r, max(leftY,rightY)+tileHeight), paint)
                    c.restore()
                }
            }
            // Gap boundaries get matching left/right trims, never on concealed traps.
            if (segment.start > 0.1f) drawEdge(c, leftEdge, segment.start, true,
                segment.start, segment.end, GroundTerrain.surface(spec, segment.start),
                tileWidth, tileHeight, viewLeft, viewRight)
            if (segment.end < spec.worldRight - 0.1f) drawEdge(c, rightEdge, segment.end, false,
                segment.start, segment.end, GroundTerrain.surface(spec, segment.end),
                tileWidth, tileHeight, viewLeft, viewRight)
        }
    }

    /**
     * Renders a released portion of the floor at its animated height. The physical
     * gap is already active in the model; only pixels (including attached props)
     * animate down. The animation is deterministic from TrapEngine's tick count.
     */
    fun drawFalling(
        c: Canvas,
        spec: LevelSpec,
        screenWorldWidth: Float,
        screenWorldHeight: Float,
        pieces: List<FallingGroundPiece>,
        viewLeft: Float = 0f
    ) {
        val tileWidth = screenWorldWidth * GameVisualConfig.GROUND_TILE_SCREEN_FRACTION
        val tileHeight = screenWorldHeight * GameVisualConfig.GROUND_TILE_HEIGHT_FRACTION
        val viewRight = viewLeft + screenWorldWidth
        if (tileWidth <= 1f || tileHeight <= 1f) return
        for (piece in pieces) {
            val y = spec.groundY + piece.dropY
            if (y > screenWorldHeight + tileHeight || piece.x > viewRight + tileWidth ||
                piece.x + piece.width < viewLeft - tileWidth) continue
            paintSpan(c, piece.x, piece.x + piece.width, y, tileWidth, tileHeight, viewLeft, viewRight)
            // The loose slab exposes both vertical sides when it starts dropping.
            drawEdge(c, leftEdge, piece.x, true, piece.x, piece.x + piece.width,
                y, tileWidth, tileHeight, viewLeft, viewRight)
            drawEdge(c, rightEdge, piece.x + piece.width, false, piece.x,
                piece.x + piece.width, y, tileWidth, tileHeight, viewLeft, viewRight)
        }
    }

    /** Merge overlapping/adjacent gaps so no overlapping spans or phantom seam. */
    private fun solidSpans(worldRight: Float, gaps: List<GroundGap>): List<Span> {
        val cuts = gaps.mapNotNull { gap ->
            val start = gap.x.coerceIn(0f, worldRight)
            val end = (gap.x + gap.width).coerceIn(0f, worldRight)
            if (end > start) Span(start, end) else null
        }.sortedBy { it.start }
        val merged = mutableListOf<Span>()
        for (cut in cuts) {
            val last = merged.lastOrNull()
            if (last != null && cut.start <= last.end + 0.001f)
                merged[merged.lastIndex] = Span(last.start, max(last.end, cut.end))
            else merged.add(cut)
        }
        val solids = mutableListOf<Span>()
        var x = 0f
        for (cut in merged) {
            if (cut.start > x) solids.add(Span(x, cut.start))
            x = max(x, cut.end)
        }
        if (x < worldRight) solids.add(Span(x, worldRight))
        return solids
    }

    private fun paintSpan(
        c: Canvas, l: Float, r: Float, y: Float, tileWidth: Float, tileHeight: Float,
        viewLeft: Float, viewRight: Float
    ) {
        if (r <= l || r < viewLeft - tileWidth || l > viewRight + tileWidth) return
        c.save()
        c.clipRect(l, y, r, y + tileHeight)
        // Aligned to 0 in world space, not to every segment's start.
        val first = floor(max(l, viewLeft - tileWidth) / tileWidth).toInt()
        val last = ceil(min(r, viewRight + tileWidth) / tileWidth).toInt()
        for (i in first until last) {
            val tileL = i * tileWidth
            val visibleL = max(l, tileL)
            val visibleR = min(r, tileL + tileWidth)
            if (visibleR <= visibleL) continue
            val image = if (Math.floorMod(i, 7) == 3) centerB else center
            val srcL = ((visibleL - tileL) / tileWidth * image.width).toInt().coerceIn(0, image.width - 1)
            val srcR = ((visibleR - tileL) / tileWidth * image.width).toInt().coerceIn(srcL + 1, image.width)
            c.drawBitmap(image, Rect(srcL, 0, srcR, image.height),
                RectF(visibleL, y, visibleR, y + tileHeight), paint)
        }
        drawForeground(c, l, r, y, tileWidth, tileHeight, first, last)
        c.restore()
    }

    /** Foreground on the metal *face*, below the walking line (never obscures hazards). */
    private fun drawForeground(
        c: Canvas, l: Float, r: Float, y: Float, tileWidth: Float,
        tileHeight: Float, first: Int, last: Int
    ) {
        for (i in first until last) {
            // Seed is a world-grid index: moving or revealing a trap does not
            // redistribute props. Small variation keeps long corridors alive.
            val slot = Math.floorMod(i * 11 + 3, 37)
            val propIndex = when (slot) {
                0, 22 -> 0  // pipe
                6 -> 1     // junction/generator
                12 -> 2    // grate
                18 -> 3    // faceplate
                26 -> 5    // industrial stacks
                32 -> 6    // cable bundle
                else -> -1
            }
            if (propIndex < 0) continue
            val prop = props[propIndex]
            val width = tileWidth * when (propIndex) {
                0 -> .91f
                5 -> .70f
                6 -> 1.08f
                else -> .70f
            }
            val height = tileHeight * when (propIndex) {
                0 -> .70f
                5 -> .71f
                6 -> .43f
                else -> .44f
            }
            val left = i * tileWidth + (tileWidth - width) * .5f
            // Never render a decoration floating across a permanent or new hole.
            // A centered prop may be clipped cleanly at a falling block edge.
            if (left > r || left + width < l) continue
            val top = y + tileHeight * (if (propIndex == 6) .40f else .18f)
            c.drawBitmap(prop, null, RectF(left, top, left + width, min(y + tileHeight, top + height)), paint)
        }
    }

    private fun drawEdge(
        c: Canvas, image: Bitmap, boundary: Float, leftBoundary: Boolean,
        segmentStart: Float, segmentEnd: Float, y: Float, tileWidth: Float,
        tileHeight: Float, viewLeft: Float, viewRight: Float
    ) {
        val width = tileWidth * .27f
        if (boundary + width < viewLeft || boundary - width > viewRight) return
        val x = if (leftBoundary) boundary else boundary - width
        c.save()
        // Drawing an edge must NEVER paint into the empty gap.
        c.clipRect(segmentStart, y, segmentEnd, y + tileHeight)
        c.drawBitmap(image, null, RectF(x, y, x + width, y + tileHeight), paint)
        c.restore()
    }
}
