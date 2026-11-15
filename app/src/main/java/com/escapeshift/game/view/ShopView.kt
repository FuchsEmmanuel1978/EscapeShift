package com.escapeshift.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

/** Landscape, Canvas-only shop; no resources, XML or extra dependencies required. */
class ShopView {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ink = Color.rgb(239, 247, 255)
    private val muted = Color.rgb(157, 181, 202)
    private val cyan = Color.rgb(55, 221, 227)
    private val gold = Color.rgb(255, 202, 84)
    private val violet = Color.rgb(191, 129, 255)
    private val pink = Color.rgb(255, 104, 135)
    private val disabled = Color.rgb(86, 107, 125)

    fun draw(canvas: Canvas, viewportWidth: Float, state: ShopState, tab: ShopTab) {
        canvas.save()
        canvas.translate(GameViewport.centerOffset(viewportWidth), 0f)
        panel(canvas, 20f, 16f, 980f, 554f, 27f,
            Color.argb(243, 10, 20, 37), Color.argb(247, 11, 30, 46))
        glow(canvas, 824f, 210f, 220f, Color.argb(38, 138, 65, 253))
        glow(canvas, 240f, 400f, 220f, Color.argb(28, 38, 232, 217))
        pill(canvas, 37f, 29f, 149f, 82f, Color.rgb(33, 55, 71))
        label(canvas, "‹  RETOUR", 50f, 63f, 19f, ink, true)
        label(canvas, "BOUTIQUE", 185f, 72f, 40f, ink, true)
        label(canvas, "ESCAPE SHIFT", 187f, 94f, 13f, muted)
        pill(canvas, 635f, 29f, 794f, 80f, Color.rgb(39, 43, 55))
        coin(canvas, 656f, 54f, 14f)
        label(canvas, state.coins.toString(), 683f, 64f, 23f, gold, true)
        pill(canvas, 801f, 29f, 960f, 80f, Color.rgb(48, 31, 49))
        heart(canvas, 813f, 45f, 29f, pink)
        label(canvas, if (state.premium) "ILLIMITÉ" else "${state.lives}/${state.maxLives}",
            854f, 64f, if (state.premium) 17f else 23f, pink, true)

        val tabs = listOf(
            Triple(48f, "VIES", ShopTab.LIVES),
            Triple(280f, "PIÈCES", ShopTab.COINS),
            Triple(512f, "MONDES", ShopTab.WORLDS),
            Triple(744f, "PREMIUM", ShopTab.PREMIUM)
        )
        for ((x, title, item) in tabs) {
            val active = item == tab
            panel(canvas, x, 108f, x + 208f, 154f, 13f,
                if (active) Color.rgb(29, 93, 104) else Color.rgb(26, 42, 56),
                if (active) Color.rgb(24, 67, 82) else Color.rgb(25, 36, 50))
            if (active) pill(canvas, x + 24f, 148f, x + 184f, 152f, cyan)
            center(canvas, title, x + 104f, 140f, 18f, if (active) ink else muted, active)
        }

        when (tab) {
            ShopTab.LIVES -> lives(canvas, state)
            ShopTab.COINS -> coins(canvas, state)
            ShopTab.WORLDS -> worlds(canvas, state)
            ShopTab.PREMIUM -> premium(canvas, state)
        }
        // Restore is intentionally always visible and requires an explicit tap.
        label(canvas, "RESTAURER ACHATS  ↻", 771f, 542f, 13f, muted, true)
        canvas.restore()
    }

    private fun lives(c: Canvas, s: ShopState) {
        // Three independent choices: earned coins, voluntary rewarded ad, or Play Billing.
        card(c, 51f, 176f, 341f, 506f, pink)
        card(c, 355f, 176f, 646f, 506f, cyan)
        card(c, 660f, 176f, 949f, 506f, violet)

        heart(c, 166f, 223f, 49f, pink)
        center(c, "+1 CŒUR", 196f, 323f, 25f, ink, true)
        center(c, "${BizConfig.Lives.COINS_PER_LIFE} PIÈCES", 196f, 359f, 20f, gold, true)
        val canTrade = s.canExchangeLife()
        action(c, 77f, 401f, 319f, 476f,
            if (s.premium) "ILLIMITÉ" else if (s.lives >= s.maxLives) "VIES PLEINES"
            else if (canTrade) "ÉCHANGER" else "PAS ASSEZ DE PIÈCES", pink, canTrade)

        circle(c, 493f, 265f, 48f, Color.rgb(38, 119, 128))
        center(c, "▶", 493f, 284f, 37f, ink, true)
        center(c, "+${BizConfig.Lives.REWARDED_LIVES} CŒURS", 500f, 323f, 25f, ink, true)
        center(c, "VIDÉO GRATUITE", 500f, 359f, 17f, muted)
        action(c, 379f, 401f, 621f, 476f,
            if (s.rewardedAdPending) "CHARGEMENT..." else "REGARDER", cyan, s.canWatchAd())

        heart(c, 761f, 223f, 49f, violet)
        center(c, "+3 CŒURS", 803f, 323f, 25f, ink, true)
        center(c, s.paidHeartsPrice ?: "PRIX INDISPONIBLE", 803f, 359f, 21f, gold, true)
        action(c, 680f, 401f, 920f, 476f,
            if (s.premium) "ILLIMITÉ" else if (s.lives >= s.maxLives) "VIES PLEINES"
            else if (s.canBuyPaidHearts()) "ACHETER" else "INDISPONIBLE", violet, s.canBuyPaidHearts())
        center(c, "Réserve : ${s.paidHeartReserve} cœurs achetés", 802f, 493f, 14f, muted)
    }

    private fun coins(c: Canvas, s: ShopState) {
        label(c, "ACHETER DES PIÈCES", 76f, 219f, 27f, ink, true)
        label(c, "Packs permanents dans la boutique, pièces utilisables pour les cœurs.", 77f, 248f, 16f, muted)
        val packs = listOf(
            Triple(70f, BizConfig.Billing.COIN_PACK_50, "DÉCOUVERTE"),
            Triple(366f, BizConfig.Billing.COIN_PACK_120, "AVENTURE"),
            Triple(662f, BizConfig.Billing.COIN_PACK_350, "TRÉSOR")
        )
        for ((x, id, name) in packs) {
            val offer = s.coinPack(id)
            card(c, x, 275f, x + 268f, 501f, gold)
            coin(c, x + 99f, 327f, 24f)
            label(c, (offer?.amount ?: BizConfig.Billing.CONSUMABLES.getValue(id)).toString(), x + 142f, 342f, 36f, ink, true)
            center(c, name, x + 134f, 383f, 16f, muted, true)
            action(c, x + 25f, 410f, x + 243f, 478f,
                if (s.premium) "PREMIUM ACTIF" else offer?.formattedPrice ?: "INDISPONIBLE",
                gold, s.canBuyCoins(id))
        }
    }

    private fun worlds(c: Canvas, s: ShopState) {
        val offers = listOf(2 to 53f, 3 to 510f)
        for ((world, x) in offers) {
            val offer = s.world(world)
            val available = offer?.developed == true
            val owned = s.premium || offer?.accessible == true
            val accent = if (world == 2) cyan else gold
            card(c, x, 176f, x + 438f, 506f, accent)
            pill(c, x + 26f, 198f, x + 185f, 237f,
                if (world == 2) Color.rgb(22, 79, 96) else Color.rgb(99, 71, 33))
            label(c, "MONDE 0$world", x + 42f, 225f, 19f, ink, true)
            center(c, if (world == 2) "❄" else "◇", x + 220f, 310f, 61f, accent, true)
            label(c, if (world == 2) "LE MONDE GLACÉ" else "LES MYSTÈRES D'ÉGYPTE", x + 35f, 354f, 22f, ink, true)
            action(c, x + 53f, 382f, x + 395f, 470f,
                when {
                    owned -> "DÉJÀ DÉBLOQUÉ"
                    !available -> "BIENTÔT"
                    offer?.formattedPrice == null -> "PRIX INDISPONIBLE"
                    else -> "DÉBLOQUER  •  ${offer.formattedPrice}"
                }, accent, s.canBuyWorld(world))
            label(c, "Accès permanent au premier niveau", x + 36f, 491f, 14f, muted)
        }
    }

    private fun premium(c: Canvas, s: ShopState) {
        panel(c, 54f, 176f, 948f, 509f, 22f,
            Color.rgb(62, 36, 92), Color.rgb(26, 32, 64))
        pill(c, 79f, 202f, 325f, 242f, Color.rgb(112, 63, 138))
        label(c, "✦  PASS PREMIUM", 94f, 231f, 22f, ink, true)
        label(c, "TOUT LE JEU. AUCUNE LIMITE.", 84f, 292f, 32f, ink, true)
        val perks = listOf("✓  Vies illimitées", "✓  Aucune publicité", "✓  Tous les niveaux disponibles", "✓  Mondes standards futurs")
        perks.forEachIndexed { i, perk -> label(c, perk, 84f, 336f + i * 36f, 19f, ink) }
        circle(c, 766f, 267f, 63f, Color.rgb(116, 75, 152))
        center(c, "★", 766f, 287f, 51f, gold, true)
        center(c, s.premiumPrice ?: "PRIX INDISPONIBLE", 770f, 357f, 29f, gold, true)
        action(c, 632f, 369f, 905f, 458f,
            if (s.premium) "PREMIUM ACTIF" else if (s.premiumPrice == null) "INDISPONIBLE" else "DEVENIR PREMIUM",
            violet, s.canBuyPremium())
        label(c, "Achat unique, sans abonnement", 627f, 483f, 14f, muted)
    }

    private fun card(c: Canvas, x: Float, y: Float, r: Float, b: Float, accent: Int) {
        panel(c, x, y, r, b, 19f, Color.rgb(26, 42, 60), Color.rgb(22, 33, 49))
        pill(c, x + 25f, y + 6f, r - 25f, y + 10f, accent)
    }

    private fun action(c: Canvas, x: Float, y: Float, r: Float, b: Float, label: String, color: Int, enabled: Boolean) {
        pill(c, x, y, r, b, if (enabled) color else Color.rgb(65, 79, 92))
        center(c, label, (x+r)/2f, (y+b)/2f + 7f, 19f, if (enabled) Color.rgb(9, 20, 32) else Color.rgb(186, 197, 208), true, r-x-20f)
    }

    private fun panel(c: Canvas, x: Float, y: Float, r: Float, b: Float, radius: Float, top: Int, bottom: Int) {
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        p.shader = LinearGradient(x, y, x, b, top, bottom, Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(x,y,r,b),radius,radius,p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.3f
        p.color = Color.argb(52, 175, 223, 246)
        c.drawRoundRect(RectF(x,y,r,b),radius,radius,p)
        p.style = Paint.Style.FILL
    }
    private fun glow(c: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        p.style = Paint.Style.FILL; p.color = Color.WHITE
        p.shader = android.graphics.RadialGradient(cx,cy,radius,color,Color.TRANSPARENT,Shader.TileMode.CLAMP)
        c.drawCircle(cx,cy,radius,p)
        p.shader = null
    }
    private fun pill(c: Canvas,x:Float,y:Float,r:Float,b:Float,color:Int) {
        p.color=color; p.style=Paint.Style.FILL
        c.drawRoundRect(RectF(x,y,r,b), minOf(18f,(b-y)/2f),minOf(18f,(b-y)/2f),p)
    }
    private fun circle(c: Canvas,x:Float,y:Float,r:Float,color:Int) {
        p.color=color; p.style=Paint.Style.FILL; c.drawCircle(x,y,r,p)
    }
    private fun coin(c: Canvas,x:Float,y:Float,r:Float) {
        circle(c,x,y,r,gold)
        p.color=Color.rgb(160,100,25); p.style=Paint.Style.STROKE; p.strokeWidth=3f
        c.drawCircle(x,y,r*.72f,p); p.style=Paint.Style.FILL
        center(c,"★",x,y+r*.31f,r*.9f,Color.rgb(141,81,20),true)
    }
    private fun heart(c: Canvas,x:Float,y:Float,size:Float,color:Int) {
        val path = Path().apply {
            moveTo(x+size*.5f,y+size)
            cubicTo(x-size*.38f,y+size*.45f,x,y-size*.2f,x+size*.5f,y+size*.28f)
            cubicTo(x+size,y-size*.2f,x+size*1.38f,y+size*.45f,x+size*.5f,y+size)
            close()
        }
        p.style=Paint.Style.FILL; p.color=color; c.drawPath(path,p)
    }
    private fun label(c: Canvas,s:String,x:Float,y:Float,size:Float,color:Int,bold:Boolean=false) {
        p.shader=null; p.style=Paint.Style.FILL; p.color=color; p.textSize=size
        p.typeface=if(bold) Typeface.create(Typeface.DEFAULT,Typeface.BOLD) else Typeface.DEFAULT
        p.textAlign=Paint.Align.LEFT
        c.drawText(s,x,y,p)
    }
    private fun center(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int,bold:Boolean=false,maxWidth:Float=Float.MAX_VALUE) {
        p.shader=null; p.style=Paint.Style.FILL; p.color=color; p.textSize=size
        p.typeface=if(bold) Typeface.create(Typeface.DEFAULT,Typeface.BOLD) else Typeface.DEFAULT
        p.textAlign=Paint.Align.CENTER
        if (p.measureText(s)>maxWidth) p.textSize*=maxWidth/p.measureText(s)
        c.drawText(s,x,y,p)
        p.textAlign=Paint.Align.LEFT
    }
}
