package com.escapeshift.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/**
 * Fixed foreground overlays for World 1.
 * Both strips fill the complete viewport width without camera scrolling,
 * horizontal cropping or independent X/Y stretching.
 *
 * Draw order is controlled by GameView: after gameplay sprites and before HUD.
 */
class FactoryParallaxLayers(context: Context) {
    companion object {
        // Move the lower overlay down a little without changing its size.
        // 0 = flush with bottom of screen, positive = partially below screen.
        const val BOTTOM_OFFSET = 40f
        // 0 = top edge of screen. HUD renders afterward, above the decoration.
        const val TOP_OFFSET = 0f
    }

    private val top = decode(context, R.drawable.world_01_factory_top)
    private val bottom = decode(context, R.drawable.world_01_factory_bottom)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

    private fun decode(context: Context, resourceId: Int): Bitmap? = try {
        BitmapFactory.decodeResource(context.resources, resourceId)
    } catch (_: Exception) {
        null
    }

    // Keep the existing API used by GameView. cameraX is intentionally ignored.
    @Suppress("UNUSED_PARAMETER")
    fun draw(canvas: Canvas, viewportWidth: Float, viewportHeight: Float, cameraX: Float) {
        if (viewportWidth <= 0f || viewportHeight <= 0f) return
        drawFixed(canvas, top, viewportWidth, viewportHeight, isTop = true)
        drawFixed(canvas, bottom, viewportWidth, viewportHeight, isTop = false)
    }

    private fun drawFixed(
        canvas: Canvas,
        bitmap: Bitmap?,
        viewportWidth: Float,
        viewportHeight: Float,
        isTop: Boolean
    ) {
        if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return

        // Uniform scaling: original 2172-px width fills the viewport exactly.
        // The resulting height is computed from the source aspect ratio.
        val displayHeight = viewportWidth * bitmap.height.toFloat() / bitmap.width.toFloat()
        val y = if (isTop) TOP_OFFSET else viewportHeight - displayHeight + BOTTOM_OFFSET

        canvas.save()
        canvas.clipRect(0f, 0f, viewportWidth, viewportHeight)
        canvas.drawBitmap(
            bitmap,
            Rect(0, 0, bitmap.width, bitmap.height),
            RectF(0f, y, viewportWidth, y + displayHeight),
            paint
        )
        canvas.restore()
    }
}
