package com.escapeshift.game

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import kotlin.math.max

/** A short, full-screen illustrated intro. Rendering-only: the game engine is unchanged. */
class StartupSplashView(context: Context) : View(context) {
    private val picture = BitmapFactory.decodeResource(context.resources, R.drawable.escape_splash)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(12, 19, 27))
        if (width <= 0 || height <= 0) return
        // Preserve aspect ratio. Extended artwork fits modern landscape screens.
        val scale = max(width.toFloat() / picture.width, height.toFloat() / picture.height)
        val destWidth = picture.width * scale
        val destHeight = picture.height * scale
        val left = (width - destWidth) * 0.5f
        val top = (height - destHeight) * 0.5f
        canvas.drawBitmap(picture, Rect(0, 0, picture.width, picture.height),
            RectF(left, top, left + destWidth, top + destHeight), paint)
    }
}
