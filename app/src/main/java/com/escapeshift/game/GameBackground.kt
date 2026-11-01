package com.escapeshift.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.min

/**
 * Pure calculation of the panoramic source crop.
 * No crop can run outside the picture, even in levels longer than the picture.
 */
object BackgroundParallax {
    const val START_X = 78f
    const val PARALLAX_SPEED = 0.12f

    fun cropWidth(bitmapWidth: Int, bitmapHeight: Int, viewWidth: Float, viewHeight: Float): Int {
        if (bitmapWidth <= 0 || bitmapHeight <= 0 || viewWidth <= 0f || viewHeight <= 0f) return 0
        val desired = (bitmapHeight.toDouble() * viewWidth / viewHeight).toInt()
        return desired.coerceIn(1, bitmapWidth)
    }

    fun offset(bitmapWidth: Int, bitmapHeight: Int, viewWidth: Float, viewHeight: Float, heroX: Float): Int {
        val crop = cropWidth(bitmapWidth, bitmapHeight, viewWidth, viewHeight)
        val available = (bitmapWidth - crop).coerceAtLeast(0)
        if (available == 0 || viewHeight <= 0f) return 0
        // Start slightly inside the panorama, to keep scenery on either side.
        val initial = available * 0.12f
        val traveled = (heroX - START_X).coerceAtLeast(0f)
        val sourcePixelsPerViewUnit = bitmapHeight.toFloat() / viewHeight
        return (initial + traveled * PARALLAX_SPEED * sourcePixelsPerViewUnit)
            .toInt().coerceIn(0, available)
    }
}

/** Decodes the image only once, then draws a moving crop instead of moving all game objects. */
class GameBackground(context: Context) {
    companion object { const val START_X = BackgroundParallax.START_X }

    private val bitmap: Bitmap? = try {
        BitmapFactory.decodeResource(context.resources, R.drawable.escape_factory_bg)
    } catch (_: Exception) {
        null // Draw a plain dark background when the image could not be decoded.
    }
    private val picturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(134, 6, 15, 23)
    }

    fun draw(canvas: Canvas, width: Float, height: Float, heroX: Float) {
        val image = bitmap
        if (image == null || width <= 0f || height <= 0f) {
            canvas.drawColor(Color.rgb(15, 34, 43))
            return
        }
        val crop = BackgroundParallax.cropWidth(image.width, image.height, width, height)
        val left = BackgroundParallax.offset(image.width, image.height, width, height, heroX)
        canvas.drawBitmap(
            image,
            Rect(left, 0, left + crop, image.height),
            RectF(0f, 0f, width, height),
            picturePaint
        )
        // Distinguish true collidable platforms / hazards from the painted scenery.
        canvas.drawRect(0f, 0f, width, height, overlayPaint)
    }
}
