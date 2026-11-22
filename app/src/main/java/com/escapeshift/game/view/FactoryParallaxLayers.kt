package com.escapeshift.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/**
 * ESCAPE SHIFT - Factory overlays V5.
 *
 * Foreground artwork follows the *world camera* instead of staying locked
 * to the phone screen. At factor 1.0, an element moves at exactly the same
 * horizontal speed as a terrain tile. A lower factor moves more slowly.
 *
 * The PNGs are scaled UNIFORMLY, full-width at cameraX=0, and repeated
 * horizontally when the camera goes past their right edge. No ping-pong,
 * shrinking source crop, distortion, or moving while cameraX is stationary.
 *
 * Rendered by GameView after gameplay and before HUD.
 */
class FactoryParallaxLayers(context: Context) {
    companion object {
        // 1 = same speed as the ground; 0 = fixed on the phone screen.
        const val TOP_SCROLL_FACTOR = 0.97f
        const val BOTTOM_SCROLL_FACTOR = 1.02f

        // Keep the V4 vertical positions. Positive bottom offset = lower.
        const val TOP_OFFSET = 0f
        const val BOTTOM_OFFSET = 40f
    }

    private val top = load(context, R.drawable.world_01_factory_top)
    private val bottom = load(context, R.drawable.world_01_factory_bottom)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private fun load(context: Context, id: Int): Bitmap? = try {
        BitmapFactory.decodeResource(context.resources, id)
    } catch (_: Exception) { null }

    fun draw(canvas: Canvas, viewportWidth: Float, viewportHeight: Float, cameraX: Float) {
        if (viewportWidth <= 0f || viewportHeight <= 0f) return
        drawLayer(canvas, top, viewportWidth, viewportHeight, cameraX,
            TOP_SCROLL_FACTOR, true)
        drawLayer(canvas, bottom, viewportWidth, viewportHeight, cameraX,
            BOTTOM_SCROLL_FACTOR, false)
    }

    private fun drawLayer(
        canvas: Canvas, image: Bitmap?, screenW: Float, screenH: Float,
        cameraX: Float, scrollFactor: Float, topLayer: Boolean
    ) {
        if (image == null || image.isRecycled || image.width <= 0 || image.height <= 0) return

        // Preserve the FULL source image on the viewport at the beginning of
        // the level. The source aspect ratio controls the displayed height.
        val tileW = screenW
        val tileH = tileW * image.height.toFloat() / image.width.toFloat()
        val y = if (topLayer) TOP_OFFSET else screenH - tileH + BOTTOM_OFFSET

        // A world position X appears at screenX = X - cameraX.
        // Using a fraction of cameraX creates a controlled parallax effect.
        // floor / positive modulo is stable for arbitrary level lengths.
        val traveled = cameraX.coerceAtLeast(0f).toDouble() * scrollFactor.toDouble()
        val offset = (traveled % tileW.toDouble()).toFloat()
        val firstX = -offset

        canvas.save()
        canvas.clipRect(0f, 0f, screenW, screenH)
        val src = Rect(0, 0, image.width, image.height)
        // At most two images are required because tileW == screenW.
        canvas.drawBitmap(image, src, RectF(firstX, y, firstX + tileW, y + tileH), paint)
        if (offset > 0.0001f) {
            canvas.drawBitmap(image, src,
                RectF(firstX + tileW, y, firstX + 2f * tileW, y + tileH), paint)
        }
        canvas.restore()
    }
}
