package com.escapeshift.game

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
class GameDesign {
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

    /** Touch controls: strict flat white, 5% opacity, no outline, bevel or orange stripe. */
    fun drawControl(c: Canvas, r: RectF, word: String, pressed: Boolean) {
        fill(c, r.left, r.top, r.right, r.bottom, opacity(WHITE, 13))
        label(c, word, r, if (word == "◀" || word == "▶") 35f else 25f, if (pressed) ORANGE_LIGHT else WHITE)
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

    fun drawDoor(c: Canvas, r: RectF, locked: Boolean) {
        // Identical outer steel frame. Closed has shutter, open has illuminated opening.
        fill(c, r.left-7f, r.top-6f, r.right+7f, r.bottom+4f, BLACK)
        fill(c, r.left-2f, r.top-1f, r.right+2f, r.bottom, ORANGE)
        fill(c, r.left+6f, r.top+6f, r.right-6f, r.bottom-4f, BLACK)
        if (locked) {
            fill(c, r.left+12f, r.top+12f, r.right-12f, r.bottom-7f, GRAPHITE)
            var y=r.top+20f
            while(y < r.bottom-12f) {
                fill(c, r.left+12f, y, r.right-12f, y+4f, opacity(WHITE, 75))
                y += 12f
            }
            fill(c, r.left+15f, r.top+14f, r.right-15f, r.top+23f, ORANGE)
            fill(c, r.right-11f, r.top+38f, r.right-5f, r.top+56f, WHITE)
        } else {
            fill(c, r.left+12f, r.top+12f, r.right-12f, r.bottom-7f, Color.rgb(15,35,35))
            fill(c, r.left+17f, r.top+14f, r.right-17f, r.bottom-8f, opacity(WHITE, 85))
            fill(c, r.left+17f, r.top+14f, r.left+24f, r.bottom-8f, WHITE)
            fill(c, r.right-24f, r.top+14f, r.right-17f, r.bottom-8f, WHITE)
            fill(c, r.left+22f, r.top+20f, r.right-22f, r.top+26f, ORANGE_LIGHT)
            fill(c, r.right-11f, r.top+38f, r.right-5f, r.top+56f, WHITE)
        }
        fill(c, r.left-7f, r.bottom-4f, r.right+7f, r.bottom+4f, GRAPHITE)
        fill(c, r.left-7f, r.bottom-4f, r.right+7f, r.bottom-1f, WHITE)
    }

    /** Drawn based on the exact triangular hitbox. Tall/double hazards use real collision rectangles. */
    fun drawSpike(c: Canvas, r: RectF) {
        val triangle=Path().apply {
            moveTo(r.left,r.bottom)
            lineTo(r.centerX(),r.top)
            lineTo(r.right,r.bottom)
            close()
        }
        p.style=Paint.Style.FILL; p.color=BLACK
        c.drawPath(triangle,p)
        val inner=Path().apply {
            moveTo(r.left+4f,r.bottom-2f)
            lineTo(r.centerX(),r.top+5f)
            lineTo(r.right-4f,r.bottom-2f)
            close()
        }
        p.color=ORANGE; c.drawPath(inner,p)
        line(c,r.centerX(),r.top+4f,r.centerX(),r.bottom-7f,WHITE,2f)
        fill(c,r.left,r.bottom-5f,r.right,r.bottom,BLACK)
        fill(c,r.left+4f,r.bottom-5f,r.right-4f,r.bottom-2f,WHITE)
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

    fun drawCoin(c: Canvas, box: HitBox, ticks: Int) {
        val bob = kotlin.math.sin(ticks * 0.11).toFloat() * 3.0f
        val cx=(box.left+box.right)*0.5f
        val cy=(box.top+box.bottom)*0.5f+bob
        // A machined octagonal collectible with a sharp white-and-orange contrast.
        val radius=15f
        val path=Path().apply {
            for (i in 0 until 8) {
                val angle=i*Math.PI/4.0
                val x=cx+kotlin.math.cos(angle).toFloat()*radius
                val y=cy+kotlin.math.sin(angle).toFloat()*radius
                if(i==0)moveTo(x,y) else lineTo(x,y)
            }
            close()
        }
        p.style=Paint.Style.FILL; p.color=ORANGE; c.drawPath(path,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=2f;p.color=WHITE;c.drawPath(path,p)
        p.style=Paint.Style.FILL
        fill(c,cx-5f,cy-7f,cx+5f,cy+7f,BLACK)
        fill(c,cx-2f,cy-4f,cx+2f,cy+4f,WHITE)
    }
}
