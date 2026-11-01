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
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Foreground gameplay uses Canvas; the industrial background is a parallax bitmap. */
class GameView(context: Context) : View(context) {
    private var w = 1000f // Dynamic virtual viewport width: screen fills horizontally.
    private val h = 560f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val backgroundRenderer = GameBackground(context)
    private val design = GameDesign(context)
    private val prefs = context.getSharedPreferences("escape_shift", Context.MODE_PRIVATE)
    private val debugMode = DevConfig.ENABLED &&
        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
    private val debugUnlockAll = debugMode && DevConfig.UNLOCK_ALL_LEVELS
    private fun accessible(n: Int) = n in 1..GameProgression.MAX_LEVEL &&
        (debugUnlockAll || GameProgression.isPlayable(n, unlocked))
    private var screen = Screen.MENU
    private var unlocked = GameProgression.safeUnlocked(prefs.getInt("unlocked", 1))
    private var currentLevel = 1
    private var lastTime = System.nanoTime()
    private var frameTime = 0f
    private var heroAnimTime = 0f
    private var heroFacingRight = true
    private val pressed = mutableSetOf<Int>()
    private val keyboardPressed = mutableSetOf<Int>()
    private fun active(id: Int): Boolean = id in pressed || id in keyboardPressed
    private var world = GameWorld(currentLevel)
    private val gameAudio = GameAudio(context)
    private var lastCrateX = world.crateX
    private var lastKey = false
    private var lastCoins = 0
    private var lastGrounded = false
    private var lastVy = 0f
    private var lastPushSoundTick = -100
    private var winSaved = false
    private val shift get() = world.shiftOn
    private val dead get() = world.dead
    private val won get() = world.won
    private var cameraX = 0f
    private var hintTime = 0f

    private val heroFrames = listOf(
        loadSprite(R.drawable.hero_walk_01),
        loadSprite(R.drawable.hero_walk_02),
        loadSprite(R.drawable.hero_walk_03),
        loadSprite(R.drawable.hero_walk_04),
        loadSprite(R.drawable.hero_walk_05)
    )
    private val sawSprite = loadSprite(R.drawable.saw)
    private val monsterSprite = loadSprite(R.drawable.monster_bot)
    private val laserSprite = loadSprite(R.drawable.laser_vertical)
    private val keycardSprite = loadSprite(R.drawable.keycard)

    // Left controls stay at the left edge; jump and SHIFT follow the right edge.
    // Hitboxes and drawing use exactly the same coordinates.
    private val buttons get() = mapOf(
        1 to RectF(28f, 448f, 125f, 532f),
        2 to RectF(143f, 448f, 240f, 532f),
        3 to RectF(w - 263f, 448f, w - 160f, 532f),
        4 to RectF(w - 144f, 448f, w - 25f, 532f)
    )
    private val level get() = world

    private val steelDark = rgb(16, 19, 23)
    private val steelMid = rgb(34, 39, 45)
    private val steelLight = rgb(56, 64, 73)
    private val orange = rgb(238, 116, 24)
    private val orangeLight = rgb(255, 165, 77)
    private val cyan = rgb(97, 224, 230)
    private val red = rgb(255, 91, 99)
    private val blue = rgb(92, 175, 255)
    private val whiteSoft = rgb(245, 246, 247)
    private val panel = rgb(10, 20, 31)

    init { isFocusable = true; isFocusableInTouchMode = true; requestFocus() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewport = GameViewport.forScreen(width, height)
        w = viewport.worldWidth
        canvas.drawColor(Color.rgb(8, 12, 19))
        canvas.save()
        canvas.scale(viewport.scale, viewport.scale)
        val now = System.nanoTime()
        val dt = min(0.035f, (now - lastTime) / 1_000_000_000f)
        lastTime = now
        if (screen == Screen.PLAY && !dead && !won) update(dt)
        if (screen != Screen.PLAY || dead || won) {
            for (type in listOf("laser", "saw", "monster", "conveyor")) gameAudio.updateAmbient(type, null)
        }
        when (screen) {
            Screen.MENU -> drawMenu(canvas)
            Screen.LEVELS -> drawLevels(canvas)
            Screen.PLAY -> drawGame(canvas)
        }
        canvas.restore()
        postInvalidateOnAnimation()
    }

    private fun update(dt: Float) {
        hintTime += dt
        frameTime += dt
        var ticks = 0
        while (frameTime >= GameWorld.STEP && ticks < 4 && !dead && !won) {
            val oldShift = world.shiftOn
            val oldCoins = world.coinsCollected
            val oldKey = world.keyCollected
            val oldGrounded = world.grounded
            val oldY = world.y
            val oldVy = world.vy
            val oldCrate = world.crateX
            val oldAlive = world.monsters.indices.count { world.isMonsterAlive(it) }
            world.tick(
                GameInput(
                    left = active(1),
                    right = active(2),
                    jump = active(3),
                    shift = active(4)
                )
            )
            if (world.shiftOn != oldShift) gameAudio.oneShot("shift")
            if (world.vy < -250f && oldVy >= -1f) gameAudio.oneShot(if (world.vy < -500f) "spring" else "jump")
            if (!oldGrounded && world.grounded && oldVy > 50f) gameAudio.oneShot("land", .25f)
            if (world.coinsCollected > oldCoins) gameAudio.oneShot("coin")
            if (world.keyCollected && !oldKey) gameAudio.oneShot("key")
            if (abs(world.crateX - oldCrate) > .3f && world.ticks - lastPushSoundTick > 14) {
                gameAudio.oneShot("crate", .22f); lastPushSoundTick = world.ticks
            }
            if (world.monsters.indices.count { world.isMonsterAlive(it) } < oldAlive) gameAudio.oneShot("monster")
            if (world.dead) gameAudio.oneShot("death")
            if (world.won) { gameAudio.oneShot("door"); gameAudio.oneShot("victory", .38f) }
            frameTime -= GameWorld.STEP
            ticks++
        }
        if (abs(world.vx) > 8f) {
            heroAnimTime += dt
            heroFacingRight = world.vx >= 0f
        } else {
            heroAnimTime = 0f
        }
        if (won && !winSaved) {
            if (GameProgression.isPlayable(currentLevel, unlocked)) {
                unlocked = GameProgression.afterWin(unlocked, currentLevel)
                prefs.edit().putInt("unlocked", unlocked).apply()
            }
            winSaved = true
        }
        updateSpatialSounds()
        cameraX = (world.x - w * 0.34f).coerceIn(0f, max(0f, world.worldRight - w))
    }

    private fun updateSpatialSounds() {
        val hero = world.x + GameWorld.SIZE / 2f
        fun nearest(xs: List<Float>): Float? = xs.minOfOrNull { abs(hero - it) }
        val laserXs = buildList {
            if (!world.shiftOn) addAll(world.lasers.map { (it.left + it.right) / 2f })
            if (world.shiftOn) addAll(world.invertedLasers.map { (it.left + it.right) / 2f })
            addAll(world.timedLasers.filter { world.timedLaserActive(it) }.map { it.x })
        }
        gameAudio.updateAmbient("laser", nearest(laserXs))
        gameAudio.updateAmbient("saw", nearest(world.saws.map { (world.sawBox(it).left + world.sawBox(it).right) / 2f }))
        gameAudio.updateAmbient("monster", nearest(world.monsters.indices.filter { world.isMonsterAlive(it) }
            .map { (world.monsterBox(it).left + world.monsterBox(it).right) / 2f }))
        gameAudio.updateAmbient("conveyor", nearest(world.conveyors.map { (it.left + it.right) / 2f }))
    }

    fun pauseAudio() = gameAudio.pause()
    fun resumeAudio() = gameAudio.resume()
    fun releaseAudio() = gameAudio.release()

    private fun drawGame(c: Canvas) {
        drawBackground(c, world.x)
        rect(c, 0f, 65f, w, h, alpha(panel, 42))
        c.save()
        c.translate(-cameraX, 0f)

        for (b in level.blocks) drawPlatform(c, RectF(b.left, b.top, b.right, b.bottom))

        if (level.hasCrate) drawCrate(c, RectF(level.crateBox.left, level.crateBox.top, level.crateBox.right, level.crateBox.bottom))

        level.bridge?.let {
            val bridgeColor = if (shift) cyan else rgb(56, 71, 78)
            drawBridge(c, RectF(it.left, it.top, it.right, it.bottom), bridgeColor, shift)
        }

        for (laser in level.lasers) {
            drawLaserColumn(c, RectF(laser.left, laser.top, laser.right, laser.bottom), red, active = !shift)
        }
        for (laser in level.invertedLasers) {
            drawLaserColumn(c, RectF(laser.left, laser.top, laser.right, laser.bottom), blue, active = shift)
        }
        for (laser in level.timedLasers) {
            drawLaserColumn(c, RectF(level.timedLaserBox(laser).left, level.timedLaserBox(laser).top, level.timedLaserBox(laser).right, level.timedLaserBox(laser).bottom), orangeLight, active = level.timedLaserActive(laser))
        }

        for (spring in level.springs) drawSpring(c, RectF(spring.left, spring.top, spring.right, spring.bottom))
        for (belt in level.conveyors) design.drawConveyor(c, RectF(belt.left, belt.top, belt.right, belt.bottom), level.ticks)
        for (pad in level.switches) drawSwitch(c, RectF(pad.left, pad.top, pad.right, pad.bottom), level.switchActivated)
        for (i in level.monsters.indices) if (level.isMonsterAlive(i)) drawMonster(c, RectF(level.monsterBox(i).left, level.monsterBox(i).top, level.monsterBox(i).right, level.monsterBox(i).bottom))
        for (saw in level.saws) drawSaw(c, RectF(level.sawBox(saw).left, level.sawBox(saw).top, level.sawBox(saw).right, level.sawBox(saw).bottom))
        for (s in level.spikes) design.drawSpike(c, RectF(s.left, s.top, s.right, s.bottom))
        for (i in level.coins.indices) if(level.isCoinVisible(i)) design.drawCoin(c, level.coins[i], level.ticks)
        if (!level.keyCollected) for (key in level.keys) drawKey(c, RectF(key.left, key.top, key.right, key.bottom))

        val locked = !level.exitUnlocked
        design.drawDoor(c, RectF(level.exit.left, level.exit.top, level.exit.right, level.exit.bottom), locked)
        text(c, if (locked) (if (level.spec.requiresSwitch) "BOUTON !" else "CARTE !") else "SORTIE", level.exit.left - 8f, level.exit.top - 17f, 24f, if (locked) orangeLight else rgb(165, 255, 214), true)
        if (hintTime < 9f && level.hasCrate) {
            text(c, "POUSSE →", level.crateBox.left - 10f, level.crateBox.top - 19f, 17f, rgb(249, 218, 147), true)
        }

        drawPlayer(c)
        if (debugMode && DevConfig.SHOW_HITBOXES) drawHitboxes(c)
        c.restore()

        drawTopHud(c)
        if (hintTime < 9f) drawHint(c)
        if (debugMode && DevConfig.SHOW_DEBUG_STATS) drawDebug(c)
        drawControls(c)
        if (dead || won) drawOverlay(c)
    }

    private fun drawTopHud(c: Canvas) {
        design.drawMenuHeader(c, RectF(0f, 0f, w, 64f))
        text(c, "PIÈCES  ${level.coinsCollected}/${level.coins.size}", 22f, 42f, 28f, GameDesign.WHITE, true)
        text(c, "NIVEAU $currentLevel/10", w * 0.38f, 40f, 25f, rgb(190, 216, 229))
        text(c, if (shift) "SHIFT: ON" else "SHIFT: OFF", w - 350f, 40f, 25f, GameDesign.ORANGE, true)
        design.drawMenuButton(c, RectF(w - 125f, 11f, w - 22f, 54f), "MENU", false)
    }

    private fun drawHint(c: Canvas) {
        c.save()
        c.translate(GameViewport.centerOffset(w), 0f)
        rounded(c, RectF(284f, 83f, 730f, 130f), 10f, alpha(panel, 180))
        rounded(c, RectF(284f, 83f, 730f, 130f), 10f, alpha(whiteSoft, 16))
        text(c, level.spec.hint, 295f, 114f, 18f, whiteSoft)
        c.restore()
    }

    private fun drawDebug(c: Canvas) {
        rounded(c, RectF(16f, 72f, 285f, 103f), 6f, alpha(panel, 176))
        rect(c, 22f, 78f, 112f, 82f, orange)
        text(c, "DEBUG  x=${world.x.toInt()} t=${world.ticks}  N=$currentLevel", 22f, 95f, 16f, rgb(255, 213, 115))
    }

    private fun drawHitboxes(c: Canvas) {
        val monsterBoxes = level.monsters.indices.filter { level.isMonsterAlive(it) }.map { level.monsterBox(it) }
        val boxes = level.blocks + level.spikes + level.lasers + level.invertedLasers +
            level.keys + level.springs + level.conveyors + level.switches + level.exit + monsterBoxes +
            listOf(level.playerBox) + (if (level.hasCrate) listOf(level.crateBox) else emptyList())
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = rgb(255, 245, 115)
        for (b in boxes) c.drawRect(b.left, b.top, b.right, b.bottom, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawControls(c: Canvas) {
        for ((id, b) in buttons) {
            val label = when(id) { 1 -> "◀"; 2 -> "▶"; 3 -> "SAUT"; else -> "SHIFT" }
            design.drawControl(c, b, label, active(id) || (id == 4 && shift))
        }
    }

    private fun drawPlayer(c: Canvas) {
        val moving = abs(world.vx) > 8f
        val index = if (moving) ((heroAnimTime / 0.09f).toInt() % heroFrames.size) else 0
        val frame = heroFrames[index]
        val dest = RectF(world.x - 9f, world.y - 8f, world.x + GameWorld.SIZE + 9f, world.y + GameWorld.SIZE + 11f)
        rounded(c, RectF(world.x - 2f, world.y + GameWorld.SIZE - 3f, world.x + GameWorld.SIZE + 2f, world.y + GameWorld.SIZE + 5f), 6f, alpha(Color.BLACK, 65))
        drawBitmapFit(c, frame, dest, 255, 0f, flipX = !heroFacingRight)
    }

    private fun drawBackground(c: Canvas, heroX: Float = GameBackground.START_X) {
        backgroundRenderer.draw(c, w, h, heroX)
    }

    private fun drawMenu(c: Canvas) {
        drawBackground(c)
        rect(c, 0f, 0f, w, h, alpha(panel, 116))
        c.save()
        c.translate(GameViewport.centerOffset(w), 0f)
        text(c, "ESCAPE", 274f, 173f, 92f, whiteSoft, true)
        text(c, "SHIFT", 364f, 260f, 92f, rgb(110, 241, 178), true)
        text(c, "ACTION + REFLEXION + EVASION", 274f, 309f, 26f, rgb(174, 201, 214))
        menuButton(c, 340f, 354f, "JOUER", rgb(48, 163, 132))
        menuButton(c, 340f, 443f, "NIVEAUX", GameDesign.BLACK)
        if (debugMode) text(c, "MODE DEBUG : niveaux 1-10 disponibles", 290f, 544f, 19f, rgb(255, 216, 131))
        c.restore()
    }

    private fun drawLevels(c: Canvas) {
        drawBackground(c)
        rect(c, 0f, 0f, w, h, alpha(panel, 118))
        c.save()
        c.translate(GameViewport.centerOffset(w), 0f)
        text(c, "CHOISIS UN NIVEAU", 248f, 90f, 47f, whiteSoft, true)
        for (i in 0 until GameProgression.DISPLAY_LEVELS) {
            val col = i % 5
            val row = i / 5
            val x = 147f + col * 146f
            val y = 132f + row * 144f
            design.drawLevelTile(c, RectF(x, y, x + 118f, y + 110f), i + 1, accessible(i + 1))
        }
        text(c, if (debugUnlockAll) "DEBUG : niveaux disponibles sans changer la sauvegarde" else
            "Termine le niveau N pour debloquer N+1 !", 190f, 426f, 22f, rgb(188, 211, 222))
        menuButton(c, 340f, 459f, "RETOUR", rgb(46, 86, 105))
        c.restore()
    }

    private fun nextAfterWin(): Int? = (currentLevel + 1).takeIf { accessible(it) }

    private fun drawOverlay(c: Canvas) {
        rect(c, 0f, 0f, w, h, Color.argb(215, 6, 13, 22))
        c.save()
        c.translate(GameViewport.centerOffset(w), 0f)
        text(c, if (won) "BRAVO !" else "GAME OVER", if (won) 340f else 258f, 190f, 75f, GameDesign.ORANGE, true)
        text(
            c,
            if (won && currentLevel == GameProgression.MAX_LEVEL) "Bravo ! Tu as termine les 10 niveaux !"
            else if (won) "Niveau $currentLevel termine ! Le prochain est debloque."
            else "Le piege t'a eu. Reessaie !",
            247f, 246f, 24f, whiteSoft
        )
        val primaryLabel = if (!won) "REJOUER" else if (nextAfterWin() != null) "NEXT" else "NIVEAUX"
        menuButton(c, 340f, 304f, primaryLabel, GameDesign.ORANGE)
        menuButton(c, 340f, 405f, "MENU", GameDesign.BLACK)
        c.restore()
    }

    private fun menuButton(c: Canvas, x: Float, y: Float, label: String, color: Int) {
        design.drawMenuButton(c, RectF(x, y, x + 320f, y + 72f), label, color != GameDesign.BLACK && label != "MENU" && label != "RETOUR")
    }

    private fun startLevel(levelNumber: Int) {
        if (!accessible(levelNumber)) return
        currentLevel = levelNumber
        world = GameWorld(currentLevel)
        reset()
    }

    private fun reset() {
        gameAudio.pause()
        gameAudio.resume()
        world.reset()
        lastPushSoundTick = -100
        frameTime = 0f
        heroAnimTime = 0f
        heroFacingRight = true
        hintTime = 0f
        cameraX = 0f
        winSaved = false
        pressed.clear()
        keyboardPressed.clear()
        screen = Screen.PLAY
    }

    private fun gameButtonForKey(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> 1
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> 2
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_SPACE -> 3
        KeyEvent.KEYCODE_S -> 4
        else -> null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (debugMode && DevConfig.ALLOW_LEVEL_SHORTCUTS && screen == Screen.PLAY) {
            if (keyCode == KeyEvent.KEYCODE_PAGE_UP) { startLevel((currentLevel + 1).coerceAtMost(10)); return true }
            if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) { startLevel((currentLevel - 1).coerceAtLeast(1)); return true }
        }
        val button = gameButtonForKey(keyCode) ?: return super.onKeyDown(keyCode, event)
        if (screen == Screen.PLAY) {
            keyboardPressed.add(button)
            invalidate()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val button = gameButtonForKey(keyCode) ?: return super.onKeyUp(keyCode, event)
        keyboardPressed.remove(button)
        invalidate()
        return screen == Screen.PLAY || super.onKeyUp(keyCode, event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val viewport = GameViewport.forScreen(width, height)
        w = viewport.worldWidth
        fun coord(i: Int) = viewport.toVirtual(event.getX(i), event.getY(i))
        fun centeredCoord(i: Int): Pair<Float, Float> {
            val (x, y) = coord(i)
            return Pair(x - GameViewport.centerOffset(w), y)
        }
        if (screen == Screen.PLAY && (dead || won)) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val (x, y) = centeredCoord(event.actionIndex)
                if (x in 340f..660f && y in 304f..376f) {
                    if (!won) reset()
                    else {
                        val next = nextAfterWin()
                        if (next != null) startLevel(next)
                        else { screen = Screen.LEVELS; pressed.clear(); keyboardPressed.clear() }
                    }
                } else if (x in 340f..660f && y in 405f..477f) {
                    screen = Screen.MENU; pressed.clear(); keyboardPressed.clear()
                }
            }
            return true
        }
        if (screen != Screen.PLAY) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val (x, y) = centeredCoord(event.actionIndex)
                if (screen == Screen.MENU && x in 340f..660f && y in 354f..426f) startLevel(
                    if (debugMode) DevConfig.START_LEVEL.coerceIn(1, 10) else GameProgression.latestPlayable(unlocked)
                )
                else if (screen == Screen.MENU && x in 340f..660f && y in 443f..515f) screen = Screen.LEVELS
                else if (screen == Screen.LEVELS && x in 340f..660f && y in 459f..531f) screen = Screen.MENU
                else if (screen == Screen.LEVELS) {
                    for (i in 0 until GameProgression.DISPLAY_LEVELS) {
                        val col = i % 5
                        val row = i / 5
                        val bx = 147f + col * 146f
                        val by = 132f + row * 144f
                        if (x in bx..(bx + 118f) && y in by..(by + 110f) && accessible(i + 1)) {
                            startLevel(i + 1)
                            break
                        }
                    }
                }
            }
            return true
        }
        if (event.actionMasked == MotionEvent.ACTION_UP && coord(event.actionIndex).let { it.first in (w - 125f)..(w - 22f) && it.second in 12f..55f }) {
            screen = Screen.MENU; pressed.clear(); return true
        }
        pressed.clear()
        if (event.actionMasked != MotionEvent.ACTION_UP && event.actionMasked != MotionEvent.ACTION_CANCEL) {
            for (i in 0 until event.pointerCount) {
                if (event.actionMasked == MotionEvent.ACTION_POINTER_UP && i == event.actionIndex) continue
                val (x, y) = coord(i)
                for ((id, b) in buttons) if (b.contains(x, y)) pressed.add(id)
            }
        }
        invalidate()
        return true
    }

    private fun drawPlatform(c: Canvas, r: RectF) {
        // Upright walls are textured; ground and overhead platforms keep the liked V14 design.
        if (r.top < GameWorld.GROUND_Y - 36f && r.bottom >= GameWorld.GROUND_Y && r.width() <= 80f) {
            design.drawWall(c, r, alternate = ((r.left / 170f).toInt() % 2 == 0))
            return
        }
        val visibleBottom = if (r.bottom >= 540f) min(r.top + 95f, r.bottom) else r.bottom
        val body = RectF(r.left, r.top, r.right, visibleBottom)
        rect(c, body.left, body.top, body.right, body.bottom, steelDark)
        rect(c, body.left, body.top, body.right, body.top + 3f, alpha(whiteSoft, 210))
        rect(c, body.left, body.top + 3f, body.right, body.top + 11f, orange)
        rect(c, body.left, body.top + 11f, body.right, body.top + 14f, steelMid)
        if (body.right - body.left > 90f) {
            val hzLeft = max(body.left + 18f, body.left + (body.right - body.left) * 0.42f)
            val hzRight = min(body.right - 18f, hzLeft + 118f)
            drawHazardBand(c, RectF(hzLeft, body.top + 3f, hzRight, body.top + 17f))
        }
        val undersideY = min(body.bottom - 18f, body.top + 32f)
        if (undersideY > body.top + 15f) {
            rounded(c, RectF((body.left + body.right) / 2f - 28f, undersideY, (body.left + body.right) / 2f + 28f, undersideY + 7f), 3f, alpha(whiteSoft, 210))
        }
        var x = body.left + 16f
        while (x < body.right - 12f) {
            rect(c, x, body.top + 18f, x + 3f, body.bottom - 6f, alpha(whiteSoft, 22))
            x += 32f
        }
        if (visibleBottom < r.bottom) {
            rect(c, r.left, visibleBottom, r.right, r.bottom, alpha(steelDark, 210))
        }
    }

    private fun drawBridge(c: Canvas, r: RectF, accent: Int, active: Boolean) {
        val body = RectF(r.left, r.top + 4f, r.right, r.bottom)
        rect(c, body.left, body.top, body.right, body.bottom, if (active) steelDark else alpha(steelDark, 120))
        rect(c, r.left, r.top, r.right, r.top + 3f, alpha(whiteSoft, if (active) 190 else 70))
        rect(c, r.left, r.top + 3f, r.right, r.top + 10f, if (active) accent else alpha(accent, 70))
        drawHazardBand(c, RectF(r.left + 10f, r.top + 3f, r.right - 10f, r.top + 16f), inactive = !active)
    }

    private fun drawCrate(c: Canvas, r: RectF) = design.drawCrate(c, r)

    private fun drawLaserColumn(c: Canvas, r: RectF, glow: Int, active: Boolean) {
        val dest = RectF(r.left - 19f, r.top - 19f, r.right + 19f, r.bottom + 19f)
        if (active) {
            rounded(c, RectF((r.left + r.right) / 2f - 13f, r.top, (r.left + r.right) / 2f + 13f, r.bottom), 7f, alpha(glow, 54))
        } else {
            rounded(c, RectF((r.left + r.right) / 2f - 4f, r.top, (r.left + r.right) / 2f + 4f, r.bottom), 4f, alpha(whiteSoft, 34))
        }
        drawBitmapFit(c, laserSprite, dest, if (active) 255 else 110)
        if (!active) rect(c, r.left + 4f, r.top + 20f, r.right - 4f, r.bottom - 20f, alpha(panel, 70))
    }

    private fun drawSpring(c: Canvas, r: RectF) = design.drawSpring(c, r)

    private fun drawSwitch(c: Canvas, r: RectF, activated: Boolean) {
        rect(c, r.left, r.top, r.right, r.bottom, steelDark)
        rect(c, r.left + 5f, r.top + 5f, r.right - 5f, r.bottom - 5f, if (activated) rgb(32, 85, 79) else rgb(36, 141, 138))
        rect(c, r.left + 9f, r.top + 10f, r.right - 9f, r.top + 18f, if (activated) alpha(whiteSoft, 90) else alpha(whiteSoft, 185))
        text(c, if (activated) "OK" else "ON", r.left + 8f, r.top - 12f, 16f, if (activated) rgb(122, 236, 214) else rgb(155, 255, 242), true)
    }


    private fun drawMonster(c: Canvas, r: RectF) {
        rounded(c, RectF(r.left + 6f, r.bottom - 4f, r.right - 6f, r.bottom + 4f), 6f, alpha(Color.BLACK, 72))
        val dest = RectF(r.left - 8f, r.top - 10f, r.right + 8f, r.bottom + 4f)
        drawBitmapFit(c, monsterSprite, dest)
    }

    private fun drawSaw(c: Canvas, r: RectF) {
        val dest = RectF(r.left - 12f, r.top - 12f, r.right + 12f, r.bottom + 12f)
        rounded(c, RectF(r.left + 3f, r.bottom - 4f, r.right - 3f, r.bottom + 4f), 5f, alpha(Color.BLACK, 55))
        drawBitmapFit(c, sawSprite, dest, 255, rotationDeg = (world.ticks * 8f) % 360f)
    }

    private fun drawKey(c: Canvas, r: RectF) {
        val bob = sin(world.elapsed * 4.1f).toFloat() * 3f
        val dest = RectF(r.left - 9f, r.top - 5f + bob, r.right + 12f, r.bottom + 8f + bob)
        drawBitmapFit(c, keycardSprite, dest)
    }

    private fun drawHazardBand(c: Canvas, r: RectF, inactive: Boolean = false) {
        rect(c, r.left, r.top, r.right, r.bottom, if (inactive) alpha(steelLight, 120) else orange)
        var x = r.left - 12f
        while (x < r.right + 16f) {
            val p = Path()
            p.moveTo(x, r.bottom)
            p.lineTo(x + 12f, r.top)
            p.lineTo(x + 24f, r.top)
            p.lineTo(x + 12f, r.bottom)
            p.close()
            paint.style = Paint.Style.FILL
            paint.color = if (inactive) alpha(steelDark, 95) else steelDark
            c.drawPath(p, paint)
            x += 24f
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawCenteredText(c: Canvas, s: String, r: RectF, size: Float, color: Int, bold: Boolean = false, yOffset: Float = 0f) {
        paint.color = color
        paint.textSize = size
        paint.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
        paint.style = Paint.Style.FILL
        val x = r.centerX() - paint.measureText(s) * 0.5f
        val y = r.centerY() + size * 0.33f + yOffset
        c.drawText(s, x, y, paint)
    }

    private fun drawBitmapFit(c: Canvas, bmp: Bitmap, dest: RectF, alpha: Int = 255, rotationDeg: Float = 0f, flipX: Boolean = false) {
        spritePaint.alpha = alpha.coerceIn(0, 255)
        if (rotationDeg == 0f && !flipX) {
            c.drawBitmap(bmp, null, dest, spritePaint)
        } else {
            c.save()
            c.translate(dest.centerX(), dest.centerY())
            if (rotationDeg != 0f) c.rotate(rotationDeg)
            if (flipX) c.scale(-1f, 1f)
            val centered = RectF(-dest.width() / 2f, -dest.height() / 2f, dest.width() / 2f, dest.height() / 2f)
            c.drawBitmap(bmp, null, centered, spritePaint)
            c.restore()
        }
        spritePaint.alpha = 255
    }

    private fun loadSprite(id: Int): Bitmap = BitmapFactory.decodeResource(resources, id)

    private fun strokeLine(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, stroke: Float) {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = stroke
        c.drawLine(x1, y1, x2, y2, paint)
        paint.style = Paint.Style.FILL
    }

    private fun rgb(r: Int, g: Int, b: Int) = Color.rgb(r, g, b)
    private fun alpha(color: Int, a: Int) = Color.argb(a.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    private fun rect(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int) { paint.color = color; paint.style = Paint.Style.FILL; c.drawRect(l, t, r, b, paint) }
    private fun rounded(c: Canvas, r: RectF, rad: Float, color: Int) { paint.color = color; paint.style = Paint.Style.FILL; c.drawRoundRect(r, rad, rad, paint) }
    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        paint.color = color
        paint.textSize = size
        paint.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
        paint.style = Paint.Style.FILL
        c.drawText(s, x, y, paint)
    }

    private enum class Screen { MENU, LEVELS, PLAY }
}
