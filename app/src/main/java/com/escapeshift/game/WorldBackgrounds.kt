package com.escapeshift.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/**
 * A small on-demand cache; unlike loading ten large images at once, the pager
 * only decodes the currently visible world and its neighbour.
 */
class WorldBackgrounds(private val context: Context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val overlay = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(112, 7, 13, 22) }
    private val cache = linkedMapOf<Int, Bitmap>()
    private fun resource(world: Int): Int = when (world) {
        1 -> R.drawable.escape_factory_bg
        2 -> R.drawable.world_02_snow
        3 -> R.drawable.world_03_desert
        4 -> R.drawable.world_04_neon
        5 -> R.drawable.world_05_forest
        6 -> R.drawable.world_06_ocean
        7 -> R.drawable.world_07_volcano
        8 -> R.drawable.world_08_orbit
        9 -> R.drawable.world_09_ruins
        else -> R.drawable.world_10_core
    }
    private fun bitmap(world: Int): Bitmap? {
        cache[world]?.let { return it }
        val decoded = try { BitmapFactory.decodeResource(context.resources, resource(world)) } catch (_: Exception) { null }
        if (decoded != null) {
            cache[world] = decoded
            while (cache.size > 3) cache.remove(cache.keys.first())
        }
        return decoded
    }
    /** Horizontal source crop is panned slightly for depth, never stretched. */
    fun draw(canvas: Canvas, world: Int, width: Float, height: Float, parallax: Float = 0.4f) {
        val image = bitmap(world)
        if (image == null) { canvas.drawColor(Color.rgb(16, 30, 43)); return }
        val crop = BackgroundParallax.cropWidth(image.width, image.height, width, height)
        val free = (image.width - crop).coerceAtLeast(0)
        val left = (free * parallax.coerceIn(0f, 1f)).toInt()
        canvas.drawBitmap(image, Rect(left, 0, left + crop, image.height),
            RectF(0f, 0f, width, height), paint)
        canvas.drawRect(0f, 0f, width, height, overlay)
    }
}
