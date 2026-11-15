package com.escapeshift.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/** Screen-independent, no PNG dependency; respects existing graphics/ sourceSets. */
class SecretPassageRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }

    fun draw(canvas: Canvas, views: List<SecretPassageView>, renderWall: (HitBox) -> Unit) {
        for (view in views) {
            val b = view.box
            if (!view.isOpen) {
                renderWall(b) // Identical texture/rendering to existing walls.
                if (view.showCracks) {
                    paint.color = Color.argb(190, 117, 224, 240)
                    val x = (b.left + b.right) / 2f
                    val y = (b.top + b.bottom) / 2f
                    canvas.drawLine(x - 8f, y - 31f, x + 7f, y - 10f, paint)
                    canvas.drawLine(x + 7f, y - 10f, x - 4f, y + 13f, paint)
                    canvas.drawLine(x - 4f, y + 13f, x + 10f, y + 29f, paint)
                }
            } else {
                // Opening is passable; only a faint outline remains for discovery feedback.
                paint.color = Color.argb(95, 95, 242, 208)
                paint.style = Paint.Style.STROKE
                val rect = RectF(b.left, b.top, b.right, b.bottom)
                canvas.drawRoundRect(rect, 4f, 4f, paint)
            }
        }
        paint.style = Paint.Style.STROKE
    }
}
