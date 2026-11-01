package com.escapeshift.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.abs

/**
 * Shared V13 visual vocabulary: industrial 2D, flat black / white / orange.
 * Only this file needs changing to restyle doors, hazards, buttons and levels.
 * No physics or hitbox dimensions are defined here.
 */
class GameDesign(private val context: Context) {
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private fun load(id: Int): Bitmap = BitmapFactory.decodeResource(context.resources, id)
    private val spikeImage = load(R.drawable.spike_industrial)
    private val closedDoorImage = load(R.drawable.door_closed)
    private val openDoorImage = load(R.drawable.door_open)
    private val crateImage = load(R.drawable.crate)
    private val coinImage = load(R.drawable.coin_collectible)
    private val springImage = load(R.drawable.spring_platform)
    private val wallImages = listOf(load(R.drawable.wall_graphite), load(R.drawable.wall_orange))
    private val controlOffImage = load(R.drawable.control_off)
    private val controlOnImage = load(R.drawable.control_on)

    private fun drawImage(c: Canvas, bitmap: Bitmap, rect: RectF, transparency: Int = 255) {
        imagePaint.alpha = transparency.coerceIn(0, 255)
        c.drawBitmap(bitmap, null, rect, imagePaint)
        imagePaint.alpha = 255
    }

    companion object {
        val BLACK: Int = Color.rgb(12, 16, 20)
        val GRAPHITE: Int = Color.rgb(36, 43, 49)
        val GREY: Int = Color.rgb(106, 115, 124)
        val WHITE: Int = Color.rgb(248, 248, 245)
        val ORANGE: Int = Color.rgb(242, 114, 29)
        val ORANGE_LIGHT: Int = Color.rgb(255, 170, 85)
        fun opacity(color: Int, alpha: Int): Int = Color.argb(alpha.coerceIn(0,255), Color.red(color), Color.green(color), Color.blue(color))
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private fun fill(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int) {
        p.style = Paint.Style.FILL; p.color = color; c.drawRect(l, t, r, b, p)
    }
    private fun line(c: Canvas, x: Float, y: Float, x2: Float, y2: Float, color: Int, width: Float) {
        p.style = Paint.Style.STROKE; p.strokeWidth = width; p.color = color
        c.drawLine(x, y, x2, y2, p); p.style = Paint.Style.FILL
    }
    private fun outline(c: Canvas, r: RectF, color: Int, width: Float) {
        line(c, r.left, r.top, r.right, r.top, color, width)
        line(c, r.right, r.top, r.right, r.bottom, color, width)
        line(c, r.right, r.bottom, r.left, r.bottom, color, width)
        line(c, r.left, r.bottom, r.left, r.top, color, width)
    }
    private fun label(c: Canvas, s: String, r: RectF, size: Float, color: Int) {
        p.style = Paint.Style.FILL
        p.textSize = size
        p.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        p.color = color
        c.drawText(s, r.centerX() - p.measureText(s) * 0.5f, r.centerY() + size * 0.35f, p)
    }

    /** Shared ON/OFF 2D image-based touch controls. Hitboxes stay in GameView. */
    fun drawControl(c: Canvas, r: RectF, word: String, pressed: Boolean) {
        val image = if (pressed) controlOnImage else controlOffImage
        drawImage(c, image, r)
        // Black flat label panel covers the stock ON/OFF inscription for arrows/jump.
        val labelBox = RectF(r.left + 10f, r.top + 23f, r.right - 10f, r.bottom - 17f)
        fill(c, labelBox.left, labelBox.top, labelBox.right, labelBox.bottom, opacity(BLACK, 197))
        label(c, word, labelBox, if (word == "◀" || word == "▶") 31f else 21f, WHITE)
    }

    /** PNG door states share exactly the same layout and do not alter the exit hitbox. */
    fun drawDoor(c: Canvas, r: RectF, locked: Boolean) {
        drawImage(c, if (locked) closedDoorImage else openDoorImage,
            RectF(r.left - 6f, r.top - 8f, r.right + 6f, r.bottom + 3f))
    }

    /** A panel sprite used ONLY for vertical collision walls, never the floor or aerial platforms. */
    fun drawWall(c: Canvas, r: RectF, alternate: Boolean) {
        val bmp = wallImages[if (alternate) 1 else 0]
        fill(c, r.left, r.top, r.right, r.bottom, BLACK)
        val tileHeight = 57f
        var y = r.top
        while (y < r.bottom) {
            val bottom = minOf(r.bottom, y + tileHeight)
            drawImage(c, bmp, RectF(r.left, y, r.right, bottom))
            y = bottom
        }
    }

    fun drawCrate(c: Canvas, r: RectF) {
        drawImage(c, crateImage, RectF(r.left - 3f, r.top - 2f, r.right + 3f, r.bottom + 2f))
    }

    fun drawSpring(c: Canvas, r: RectF) {
        // The launchpad is 7px high physically. Artwork grows upward, not into the ground.
        drawImage(c, springImage, RectF(r.left - 2f, r.top - 20f, r.right + 2f, r.bottom + 2f))
    }

    /** Draw 1, 2 or 3 copies of the same 2D spike PNG. Collision stays defined by GamePhysics. */
    fun drawSpikeGroup(c: Canvas, r: RectF, count: Int) {
        val n = count.coerceIn(1, 3)
        val unitWidth = r.width() / n
        for (i in 0 until n) {
            val x = r.left + i * unitWidth
            drawImage(c, spikeImage, RectF(x, r.top - 2f, x + unitWidth, r.bottom + 2f))
        }
    }

    fun drawSpike(c: Canvas, r: RectF) = drawSpikeGroup(c, r,
        when { r.width() >= 90f -> 3; r.width() >= 65f -> 2; else -> 1 })

    fun drawCoin(c: Canvas, box: HitBox, ticks: Int) {
        val bob = kotlin.math.sin(ticks * 0.11).toFloat() * 3f
        drawImage(c, coinImage,
            RectF(box.left - 5f, box.top - 5f + bob, box.right + 5f, box.bottom + 5f + bob))
    }

    /** All menu/overlay buttons share the same style and geometry. */
    fun drawMenuButton(c: Canvas, r: RectF, word: String, primary: Boolean) {
        fill(c, r.left, r.top, r.right, r.bottom, if (primary) ORANGE else WHITE)
        outline(c, r, BLACK, 3f) // strong black keyline shared by replay and menu
        fill(c, r.left + 10f, r.top + 7f, r.left + 17f, r.bottom - 7f, if(primary) BLACK else ORANGE)
        label(c, word, r, if (r.height() < 50f) 18f else 31f, if(primary) WHITE else BLACK)
    }

    fun drawMenuHeader(c: Canvas, r: RectF) {
        fill(c, r.left, r.top, r.right, r.bottom, opacity(BLACK, 218))
        fill(c, r.left, r.bottom - 3f, r.right, r.bottom, ORANGE)
    }

    fun drawLevelTile(c: Canvas, r: RectF, level: Int, unlocked: Boolean) {
        fill(c, r.left, r.top, r.right, r.bottom, opacity(BLACK, 225))
        outline(c, r, if(unlocked) WHITE else opacity(GREY, 125), 2f)
        fill(c, r.left, r.top, r.right, r.top+9f, if(unlocked) ORANGE else GREY)
        label(c, "$level", RectF(r.left,r.top+16f,r.right,r.bottom-4f), 49f, WHITE)
        if(!unlocked) {
            label(c, "LOCK", RectF(r.left,r.bottom-35f,r.right,r.bottom-5f), 15f, GREY)
        }
    }

    fun drawConveyor(c: Canvas, r: RectF, ticks: Int) {
        // High-contrast animated leftward belt. Different world.tick speed affects physics.
        fill(c, r.left-4f, r.top-12f, r.right+4f, r.bottom+8f, BLACK)
        fill(c, r.left, r.top-8f, r.right, r.top-1f, WHITE)
        fill(c, r.left, r.top-1f, r.right, r.bottom+1f, GRAPHITE)
        fill(c, r.left, r.bottom-6f, r.right, r.bottom, ORANGE)
        val phase=((ticks*5)%28).toFloat()
        var x=r.left-28f+phase
        while(x < r.right+28f) {
            val a=x.coerceAtLeast(r.left+5f)
            val b=(x+14f).coerceAtMost(r.right-5f)
            if(b>a) {
                line(c,a,r.top+9f,b,r.top+9f,WHITE,3f)
                line(c,a,r.top+12f,b,r.top+12f,ORANGE,2f)
            }
            x+=28f
        }
        fill(c,r.left+3f,r.top-10f,r.left+13f,r.bottom+4f,ORANGE)
        fill(c,r.right-13f,r.top-10f,r.right-3f,r.bottom+4f,ORANGE)
    }

}
