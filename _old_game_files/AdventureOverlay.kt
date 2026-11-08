package com.escapeshift.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

/** 2-D inventory / escape room interface shared by all World 1 levels. */
class AdventureOverlay(private val state: World1Adventure) {
    var open = false
    var primary = ""; private set
    var secondary = ""; private set
    var page = 0; private set
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val white = Color.rgb(247, 247, 244)
    private val orange = Color.rgb(244, 117, 32)
    private val bg = Color.rgb(14, 20, 27)
    private val mid = Color.rgb(38, 45, 53)
    private fun box(c: Canvas, r: RectF, color: Int) {
        p.color = color; p.style = Paint.Style.FILL
        c.drawRoundRect(r, 8f, 8f, p)
    }
    private fun txt(c: Canvas, s: String, x: Float, y: Float, size: Float = 19f, color: Int=white, bold: Boolean=false) {
        p.color=color; p.style=Paint.Style.FILL;p.textSize=size
        p.typeface=Typeface.create("sans-serif", if(bold) Typeface.BOLD else Typeface.NORMAL)
        c.drawText(s, x, y, p)
    }
    private fun short(s: String, max: Int): String = if(s.length <= max) s else s.take(max-3)+"..."
    private fun button(c: Canvas, r:RectF, s:String, accent:Boolean=false) {
        box(c,r,if(accent) orange else mid)
        txt(c,s,r.left+11f,r.centerY()+6f,17f,white,true)
    }
    fun draw(c:Canvas, width:Float, sceneNear:Boolean, doorNear:Boolean) {
        if (!open) return
        val off=(width-1000f)/2f
        c.save(); c.translate(off,0f)
        p.color=Color.argb(225,4,9,15);c.drawRect(0f,65f,1000f,560f,p)
        box(c,RectF(89f,72f,911f,545f), bg)
        box(c,RectF(101f,81f,899f,122f), orange)
        txt(c,"ENQUETE / INVENTAIRE",118f,108f,24f,Color.BLACK,true)
        button(c,RectF(833f,83f,891f,119f),"X",false)
        val room=EscapeStory.room(state.level)
        txt(c,"${room.zone}   -   SALLE ${room.level}/10",109f,150f,18f,orange,true)
        txt(c,short(room.title+" : "+room.objective,80),109f,177f,19f,white)
        box(c,RectF(103f,188f,490f,232f), if(sceneNear) mid else Color.rgb(24,28,33))
        txt(c,"DECOR ${if(sceneNear) "[PROCHE]" else "[ELOIGNE]"}",116f,206f,16f,orange,true)
        txt(c,short(room.sceneDescription,43),116f,224f,14f,white)
        box(c,RectF(498f,188f,898f,232f),if(doorNear) mid else Color.rgb(24,28,33))
        txt(c,"SORTIE ${if(doorNear) "[PROCHE]" else "[ELOIGNE]"}  ${if(state.isSolved()) "OUVERTE" else "FERMEE"}",510f,206f,16f,orange,true)
        txt(c,short(room.target,37),510f,224f,14f,white)
        txt(c,"SAC : selectionne deux objets, puis COMBINER",108f,253f,17f,white,true)
        if(state.inventory.size>10) button(c,RectF(735f,235f,892f,260f),"SAC > ${page+1}")
        val items=state.inventory.toList()
        for(i in 0 until 10) {
            val idx=page*10+i
            val x=109f+(i%5)*158f; val y=265f+(i/5)*35f
            val item=items.getOrNull(idx)
            val rect=RectF(x,y,x+148f,y+30f)
            box(c,rect,if(item!=null && (item==primary || item==secondary)) orange else mid)
            if(item!=null)txt(c,short(EscapeStory.label(item),15),x+7,y+21f,14f,white,item==primary)
        }
        if(items.size>10) txt(c,"Page ${page+1}/${(items.size+9)/10}  (touche SAC pour changer)",109f,341f,13f,orange)
        val labels=listOf("RAMASSER","EXAMINER","COMBINER","UTILISER","INDICE")
        for(i in labels.indices) {
            val left=110f+i*158f
            button(c,RectF(left,351f,left+149f,393f),labels[i], i==3 && doorNear)
        }
        if (room.puzzle==EscapePuzzle.CODE || room.puzzle==EscapePuzzle.FINAL) {
            txt(c,"CODE 4 CHIFFRES   ${"●".repeat(state.code().length)}${"○".repeat(4-state.code().length)}",109f,418f,16f,orange,true)
            for (i in 0..9) {
                val x=110f+i*79f
                button(c,RectF(x,426f,x+68f,467f),"$i")
            }
        } else txt(c,"MECANISME : ${short(room.target.uppercase(),44)}",109f,443f,17f,orange,true)
        if(room.puzzle==EscapePuzzle.SYMBOLS) {
            txt(c,"SYMBOLES   ${state.symbols().length}/3",109f,495f,16f,orange,true)
            button(c,RectF(257f,478f,414f,511f),"TRIANGLE")
            button(c,RectF(423f,478f,563f,511f),"ROND")
            button(c,RectF(572f,478f,708f,511f),"CARRE")
        } else txt(c,"Cherche, assemble et utilise pour ouvrir la sortie.",109f,497f,16f,white)
        txt(c,short(state.message,88),107f,534f,16f,orange,true)
        c.restore()
    }
    fun handle(x: Float,y:Float,width:Float,nearScene:Boolean,nearExit:Boolean):Boolean {
        if(!open)return false
        val xx=x-(width-1000f)/2f
        if(xx in 833f..891f && y in 81f..122f) {open=false;return true}
        if(y in 235f..262f && xx in 735f..892f && state.inventory.size>10) {
            page=(page+1)%((state.inventory.size+9)/10)
            return true
        }
        if(y in 265f..330f && xx in 109f..890f) {
            val col=((xx-109f)/158f).toInt()
            val row=if(y<300f)0 else 1
            val item=state.inventory.toList().getOrNull(page*10+row*5+col)
            if(item!=null){
                if(primary==item){primary="";secondary=""}
                else if(primary.isBlank()||secondary.isNotBlank()){primary=item;secondary=""}
                else secondary=item
            }
            return true
        }
        if(y in 351f..393f && xx in 110f..900f) {
            when(((xx-110f)/158f).toInt()){
                0->state.collect(nearScene)
                1->if(primary.isNotEmpty())state.examineItem(primary)else state.examineScene(nearScene)
                2->{state.combine(primary,secondary);primary="";secondary=""}
                3->state.use(primary,nearExit)
                4->state.hint()
            }
            return true
        }
        if(y in 426f..467f && xx in 110f..900f){
            val digit=((xx-110f)/79f).toInt()
            if (digit in 0..9)state.digit(digit,nearExit)
            return true
        }
        if(y in 478f..511f) {
            if(xx in 257f..414f)state.symbol("T",nearExit)
            if(xx in 423f..563f)state.symbol("R",nearExit)
            if(xx in 572f..708f)state.symbol("C",nearExit)
            return true
        }
        return true // modal: consume taps so game never moves behind the inventory
    }
}
