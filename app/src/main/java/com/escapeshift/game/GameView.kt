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
import kotlin.math.exp
import kotlin.math.ceil

/** Foreground gameplay uses Canvas; the industrial background is a parallax bitmap. */
class GameView(
    context: Context,
    private val onLevelFinished: ((Int, Boolean, () -> Boolean) -> Unit)? = null,
    private val onLeaveResult: (() -> Unit)? = null,
    private val onPauseChanged: ((Boolean) -> Unit)? = null
) : View(context) {
    private var w = 1000f // Dynamic virtual viewport width: screen fills horizontally.
    private val h = 560f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val backgroundRenderer = GameBackground(context)
    private val worldBackgrounds = WorldBackgrounds(context)
    private var selectedWorld = 1
    private var worldScroll = 0f
    private var worldTouchStartX = 0f
    private var worldTouchStartY = 0f
    private var worldTouchStartValid = false
    private val strings = GameStrings(context)
    private val design = GameDesign(context, strings)
    private val factoryGround = FactoryGround(context)
    private val prefs = context.getSharedPreferences("escape_shift", Context.MODE_PRIVATE)
    /** Unique coin IDs across ALL finished/unfinished levels, persisted on the phone. */
    private val savedCoinIds = prefs.getString("all_coins_v1", "")
        ?.split('|')?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf<String>()
    private fun rememberCoins() {
        if (previewSession) return
        var changed = false
        val coinObjects = level.spec.objects.filter { it.type == "coin" }
        for (i in coinObjects.indices) {
            if (!level.isCoinVisible(i)) {
                changed = savedCoinIds.add("${currentLevel}:${coinObjects[i].id}") || changed
            }
        }
        if (changed) prefs.edit().putString("all_coins_v1", savedCoinIds.sorted().joinToString("|")).apply()
    }

    private val debugMode = DevConfig.ENABLED &&
        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
    private val debugUnlockAll = debugMode && DevConfig.UNLOCK_ALL_LEVELS
    /** Preview slots are always selectable without affecting save-game progression. */
    private fun accessible(n: Int): Boolean = when {
        WorldCatalog.isPreviewLevel(n) -> true
        n in 1..GameProgression.MAX_LEVEL -> debugUnlockAll || GameProgression.isPlayable(n, unlocked)
        else -> false
    }
    private var screen = Screen.MENU
    private var unlocked = GameProgression.safeUnlocked(prefs.getInt("unlocked", 1))
    private var currentLevel = 1
    private var previewSession = false
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
    private var endOfLevelReported = false
    private val shift get() = world.shiftOn
    private val dead get() = world.dead
    private val won get() = world.won
    private var cameraX = 0f
    private var hintTime = 0f
    private var isGamePaused = false
    private var deathIrisElapsed = 0f
    private var hearts = GameVisualConfig.MAX_HEARTS
    private var respawnCount = 0
    val isPaused: Boolean get() = isGamePaused
    private val pauseButton get() = RectF(w - 125f, 11f, w - 22f, 54f)

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
    ) + (if (world.advancedPuzzle != null) mapOf(5 to RectF(w - 144f, 351f, w - 25f, 435f)) else emptyMap())
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
        if (screen == Screen.PLAY && !isGamePaused && !dead && !won) update(dt)
        if (screen == Screen.PLAY && dead && !isGamePaused) {
            val duration = if (hearts > 1) GameVisualConfig.RESPAWN_IRIS_SECONDS
                else GameVisualConfig.DEATH_IRIS_DURATION_SECONDS
            deathIrisElapsed = min(duration, deathIrisElapsed + dt)
            if (hearts > 1 && deathIrisElapsed >= duration) respawnAfterDeath()
        }
        if (screen == Screen.LEVELS) {
            val target = (selectedWorld - 1).toFloat()
            worldScroll += (target - worldScroll) * (1f - exp(-dt * 14f))
            if (abs(target - worldScroll) < 0.002f) worldScroll = target
        }
        if (screen != Screen.PLAY || isGamePaused || dead || won) {
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
            val oldCrates = world.crateBoxes.map { it.left }
            val oldAlive = world.monsters.indices.count { world.isMonsterAlive(it) }
            world.tick(
                GameInput(
                    left = active(1),
                    right = active(2),
                    jump = active(3),
                    shift = active(4),
                    interact = active(5)
                )
            )
            if (world.shiftOn != oldShift) gameAudio.oneShot("shift")
            if (world.vy < -250f && oldVy >= -1f) gameAudio.oneShot(if (world.vy < -500f) "spring" else "jump")
            if (!oldGrounded && world.grounded && oldVy > 50f) gameAudio.oneShot("land", .25f)
            if (world.coinsCollected > oldCoins) { gameAudio.oneShot("coin"); rememberCoins() }
            if (world.keyCollected && !oldKey) gameAudio.oneShot("key")
            if (world.crateBoxes.map { it.left }.zip(oldCrates).any { (now, old) -> abs(now - old) > .3f } && world.ticks - lastPushSoundTick > 14) {
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
            if (!previewSession && GameProgression.isPlayable(currentLevel, unlocked)) {
                unlocked = GameProgression.afterWin(unlocked, currentLevel)
                prefs.edit().putInt("unlocked", unlocked).apply()
            }
            winSaved = true
        }
        // Exactly one game result notification per attempt: victory OR defeat.
        // The callback is raised only after the physics engine ends the level.
        if ((won || (dead && hearts <= 1)) && !endOfLevelReported) {
            endOfLevelReported = true
            val resultWasVictory = won
            if (!previewSession) onLevelFinished?.invoke(currentLevel, resultWasVictory) {
                screen == Screen.PLAY &&
                    if (resultWasVictory) won else dead
            }
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
    fun resumeAudio() { if (!isGamePaused) gameAudio.resume() }
    fun releaseAudio() = gameAudio.release()

    private fun drawGame(c: Canvas) {
        drawBackground(c, world.x)
        rect(c, 0f, 65f, w, h, alpha(panel, 42))
        c.save()
        c.translate(-cameraX, 0f)

        for (b in level.blocks) {
            // World 1 floor is now rendered once, in the FOREGROUND, using repeated PNG tiles.
            // Explicit ground.gaps have no tiles and remain completely empty.
            if (currentLevel in 1..10 && b.top == level.spec.groundY &&
                b.bottom == level.spec.groundY + level.spec.groundDepth) continue
            drawPlatform(c, RectF(b.left, b.top, b.right, b.bottom))
        }

        for (crate in level.crateBoxes) drawCrate(c, RectF(crate.left, crate.top, crate.right, crate.bottom))

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
        for (i in level.switches.indices) {
            val pad = level.switches[i]
            drawSwitch(c, RectF(pad.left, pad.top, pad.right, pad.bottom), level.isSwitchTouched(i))
        }
        for (i in level.crateSwitches.indices) {
            val pad = level.crateSwitches[i]
            drawPressurePlate(c, RectF(pad.left, pad.top, pad.right, pad.bottom), level.isPlateActive(i))
        }
        for (i in level.snowflakes.indices) drawSnowflake(c, level.snowflakeBox(i))
        for (i in level.monsters.indices) if (level.isMonsterAlive(i)) drawMonster(c, RectF(level.monsterBox(i).left, level.monsterBox(i).top, level.monsterBox(i).right, level.monsterBox(i).bottom))
        for (saw in level.saws) drawSaw(c, RectF(level.sawBox(saw).left, level.sawBox(saw).top, level.sawBox(saw).right, level.sawBox(saw).bottom))
        for (s in level.spikes) design.drawSpike(c, RectF(s.left, s.top, s.right, s.bottom))
        drawPhysicalPuzzle(c)
        drawAdvancedPuzzle(c)
        drawDynamicTraps(c)
        for (i in level.coins.indices) if(level.isCoinVisible(i)) design.drawCoin(c, level.coins[i], level.ticks)
        if (!level.keyCollected) for (key in level.keys) drawKey(c, RectF(key.left, key.top, key.right, key.bottom))

        val locked = !level.exitUnlocked
        design.drawDoor(c, RectF(level.exit.left, level.exit.top, level.exit.right, level.exit.bottom), locked)
        if (hintTime < 9f && level.hasCrate) {
            val crate = level.crateBoxes.first()
            text(c, strings.t("push"), crate.left - 10f, crate.top - 19f, 17f, rgb(249, 218, 147), true)
        }

        drawPlayer(c)
        if (currentLevel in 1..10) {
            factoryGround.draw(c, level.spec, w, h, level.dynamicGroundGaps)
            // Highlight cracking sections above the tile foreground, not behind it.
            for (v in level.dynamicTrapViews) if (v.type == "collapse_floor" && v.state == "warning") {
                rect(c, v.box.left, level.spec.groundY - 5f, v.box.right,
                    level.spec.groundY + 3f, alpha(orange, 205))
            }
        }
        if (debugMode && DevConfig.SHOW_HITBOXES) drawHitboxes(c)
        c.restore()

        drawTopHud(c)
        if (hintTime < 9f) drawHint(c)
        if (debugMode && DevConfig.SHOW_DEBUG_STATS) drawDebug(c)
        drawControls(c)
        if (isGamePaused && !dead && !won) drawPauseOverlay(c)
        else if (dead && (hearts > 1 || deathIrisElapsed < GameVisualConfig.DEATH_IRIS_DURATION_SECONDS)) drawDeathIris(c)
        else if (dead || won) drawOverlay(c)
    }


    /** Flat 2D prototypes using existing industrial colors; no new sprite assets. */
    private fun drawPhysicalPuzzle(c: Canvas) {
        val puzzle = level.physicalPuzzle ?: return
        puzzle.sliderBox?.let { b ->
            val r = RectF(b.left, b.top, b.right, b.bottom)
            rect(c, r.left, r.top, r.right, r.bottom, steelMid)
            rect(c, r.left, r.top, r.right, r.top + 5f, orangeLight)
            rect(c, r.left + 9f, r.bottom - 5f, r.right - 9f, r.bottom - 2f, whiteSoft)
        }
        puzzle.wheelBox?.let { b ->
            val cx = (b.left + b.right) / 2f; val cy = (b.top + b.bottom) / 2f
            val rad = (b.right - b.left) / 2f
            paint.color = steelDark; paint.style = Paint.Style.FILL
            c.drawCircle(cx, cy, rad, paint)
            paint.color = orangeLight
            c.drawCircle(cx, cy, rad * .78f, paint)
            paint.color = steelMid
            c.drawCircle(cx, cy, rad * .59f, paint)
            paint.color = whiteSoft
            c.drawCircle(cx, cy, 6f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = whiteSoft
            val angle = ((b.left - 330f) / rad) * 57.3f
            c.save(); c.translate(cx, cy); c.rotate(angle)
            c.drawLine(-18f, 0f, 18f, 0f, paint)
            c.drawLine(0f, -18f, 0f, 18f, paint)
            c.restore(); paint.style = Paint.Style.FILL
        }
        for (i in puzzle.buttonBoxes.indices) {
            val pad = puzzle.buttonBoxes[i]
            drawPressurePlate(c, RectF(pad.left, pad.top, pad.right, pad.bottom), puzzle.buttonStates[i])
            text(c, (i + 1).toString(), pad.left + 8f, pad.top - 9f, 17f, whiteSoft, true)
        }
        puzzle.laserBox?.let { b ->
            drawLaserColumn(c, RectF(b.left, b.top, b.right, b.bottom), red, puzzle.laserActive)
            if (!puzzle.laserActive) {
                val seconds = (puzzle.laserRemainingTicks / 60f)
                text(c, "%.1fs".format(java.util.Locale.US, seconds), b.left - 9f, b.top - 14f, 18f, orangeLight, true)
            }
        }
        for ((index,b) in puzzle.movingPlatforms().withIndex()) {
            drawPlatform(c, RectF(b.left, b.top, b.right, b.bottom))
            rect(c, b.left + 10f, b.top + 5f, b.right - 10f, b.top + 8f, if (puzzle.isSynced) cyan else orangeLight)
            text(c, (index + 1).toString(), b.left + 10f, b.top - 10f, 16f, whiteSoft, true)
        }
        val status = when (currentLevel) {
            21 -> strings.t("puzzle_bridge_status", "bridge" to if(puzzle.isBridgePlaced) "✓" else "·",
                "return" to if(puzzle.isReturning) "✓" else "·")
            22 -> strings.t("puzzle_laser_status", "crossed" to if(puzzle.isLaserCrossed) "✓" else "·")
            23 -> strings.t("puzzle_sync_status", "pads" to if(puzzle.isSynced) "✓" else "·",
                "landed" to puzzle.landedCount)
            else -> ""
        }
        if (status.isNotEmpty()) {
            val px = (level.x - 140f).coerceIn(45f, level.worldRight - 300f)
            rounded(c, RectF(px, 92f, px + 375f, 121f), 5f, alpha(steelDark, 180))
            text(c, status, px + 9f, 112f, 17f, whiteSoft)
        }
    }


    /** Experimental puzzles use only familiar 2D Canvas shapes and existing crates. */
    private fun drawAdvancedPuzzle(c: Canvas) {
        val puzzle = level.advancedPuzzle ?: return
        val p = level.playerBox
        for ((i, crate) in puzzle.crateDevices.withIndex()) {
            val cb = level.crateBoxes.getOrNull(i) ?: continue
            val label = if (crate.symbol.isNotBlank()) crate.symbol else crate.id.removePrefix("crate")
            rounded(c, RectF(cb.left + 4f, cb.top - 25f, cb.right - 4f, cb.top - 5f), 4f, alpha(steelDark, 215))
            textFitted(c, label, cb.left + 7f, cb.top - 10f, 15f, (cb.right - cb.left) - 14f, whiteSoft, true)
        }
        for (d in puzzle.devices) {
            val b = puzzle.box(d)
            val r = RectF(b.left, b.top, b.right, b.bottom)
            when (d.kind) {
                "crate", "ground_gap", "exit_gate", "word_exit" -> Unit // Already represented by the game world / ground.
                "heavy_pressure_plate", "weight_button", "weighted_button", "pressure_button", "inverse_button", "speed_button", "code_button" -> {
                    drawPressurePlate(c, r, puzzle.isPressed(d.id, p, level.crateBoxes))
                    text(c, d.id, r.left, r.top - 8f, 15f, whiteSoft, true)
                }
                "rotating_wheel" -> {
                    val rad = r.width() * 0.48f
                    paint.color = steelMid; paint.style = Paint.Style.FILL
                    c.drawCircle(r.centerX(), r.centerY(), rad, paint)
                    paint.color = orangeLight
                    c.drawCircle(r.centerX(), r.centerY(), rad * .76f, paint)
                    paint.color = steelDark
                    c.drawCircle(r.centerX(), r.centerY(), rad * .6f, paint)
                    c.save(); c.translate(r.centerX(), r.centerY()); c.rotate(puzzle.wheelAngle(d.id).toFloat())
                    rect(c, -rad*.67f, -3f, rad*.67f, 3f, whiteSoft)
                    rect(c, -3f, -rad*.67f, 3f, rad*.67f, whiteSoft)
                    c.restore()
                    text(c, "${puzzle.wheelAngle(d.id)}°", r.left - 5f, r.top - 10f, 17f, orangeLight, true)
                }
                "movable_spring", "spring" -> drawSpring(c, r)
                "lift_wall" -> if (puzzle.wallBoxes().any { it == b }) drawPlatform(c, r)
                "surprise_wall" -> drawPlatform(c, RectF(r.left, r.top, r.right, r.bottom))
                "moving_spike" -> design.drawSpike(c, r)
                "sliding_platform", "moving_platform", "adjustable_platform", "lowering_platform" -> Unit // drawn below
                "number_token", "letter_token", "iq_token" -> if (!puzzle.isCollected(d.id)) {
                    rounded(c, r, 5f, orange)
                    rounded(c, RectF(r.left + 3f, r.top + 3f, r.right - 3f, r.bottom - 3f), 4f, steelDark)
                    val tokenLabel = when (d.kind) {
                        "number_token" -> d.id.removePrefix("n")
                        "letter_token" -> if (d.symbol.isNotEmpty()) d.symbol else d.id.take(1)
                        else -> if (d.symbol.isNotEmpty()) d.symbol else d.id
                    }
                    textFitted(c, tokenLabel, r.left + 5f, r.centerY() + 6f, 20f, r.width() - 9f, whiteSoft, true)
                }
                "word_board", "sequence_board" -> {
                    rounded(c, r, 6f, alpha(steelDark, 235))
                    rect(c, r.left + 3f, r.top + 4f, r.right - 3f, r.top + 8f, orange)
                    textFitted(c, d.text, r.left + 10f, r.centerY() + 9f, 21f, r.width() - 18f, whiteSoft, true)
                }
                "gravity_zone", "reverse_controls_zone" -> {
                    rounded(c, r, 5f, alpha(if (d.kind == "gravity_zone") cyan else orange, 46))
                    text(c, if (d.kind == "gravity_zone") "G ↑" else "⇄", r.left + 7f, r.top + 28f, 19f, whiteSoft, true)
                }
                "false_exit" -> {
                    design.drawDoor(c, r, true)
                    text(c, "?", r.left + 22f, r.top + 40f, 27f, orangeLight, true)
                }
            }
        }
        for (b in puzzle.platformBoxes()) drawPlatform(c, RectF(b.left, b.top, b.right, b.bottom))
        if (puzzle.feedback.isNotBlank() || puzzle.sequenceStatus.isNotBlank()) {
            val label = (puzzle.feedback + "  " + puzzle.sequenceStatus).trim()
            val xx = (level.x - 110f).coerceIn(70f, level.worldRight - 440f)
            rounded(c, RectF(xx, 96f, xx + 410f, 131f), 7f, alpha(steelDark, 220))
            textFitted(c, label, xx + 11f, 120f, 18f, 385f, whiteSoft, true)
        }
        if (level.carryingCrate) text(c, "ACT", level.x - 6f, level.y - 52f, 15f, orangeLight, true)
    }

    private fun drawTopHud(c: Canvas) {
        design.drawMenuHeader(c, RectF(0f, 0f, w, 64f))
        // PNG coin symbol and persisted, deduplicated total (not a per-level score).
        design.drawCoinHud(c, RectF(20f, 12f, 57f, 52f))
        textFitted(c, savedCoinIds.size.toString(), 69f, 42f, 28f, w * 0.25f, GameDesign.WHITE, true)
        // Hearts are per-attempt lives. They reset on starting or restarting a level.
        if (currentLevel in 1..10) {
            for (index in 0 until GameVisualConfig.MAX_HEARTS) {
                val hx = 145f + index * 36f
                val heartColor = if (index < hearts) orange else alpha(whiteSoft, 62)
                val shape = Path().apply {
                    moveTo(hx + 11f, 38f)
                    cubicTo(hx - 11f, 26f, hx + 1f, 13f, hx + 11f, 24f)
                    cubicTo(hx + 21f, 13f, hx + 33f, 26f, hx + 11f, 38f)
                    close()
                }
                paint.style = Paint.Style.FILL
                paint.color = heartColor
                c.drawPath(shape, paint)
            }
        }
        // Clean HUD: persistent coins, lives, and a flat, translucent pause icon.
        rounded(c, pauseButton, 9f, alpha(whiteSoft, GameVisualConfig.PAUSE_BACKGROUND_ALPHA))
        val iconColor = alpha(whiteSoft, GameVisualConfig.PAUSE_SYMBOL_ALPHA)
        rect(c, pauseButton.centerX() - 12f, 20f, pauseButton.centerX() - 5f, 45f, iconColor)
        rect(c, pauseButton.centerX() + 5f, 20f, pauseButton.centerX() + 12f, 45f, iconColor)
    }

    private fun drawHint(c: Canvas) {
        c.save()
        c.translate(GameViewport.centerOffset(w), 0f)
        rounded(c, RectF(284f, 83f, 730f, 130f), 10f, alpha(panel, 180))
        rounded(c, RectF(284f, 83f, 730f, 130f), 10f, alpha(whiteSoft, 16))
        textFitted(c, strings.t(level.spec.hint), 295f, 114f, 18f, 425f, whiteSoft)
        c.restore()
    }

    private fun drawDebug(c: Canvas) {
        rounded(c, RectF(16f, 72f, 285f, 103f), 6f, alpha(panel, 176))
        rect(c, 22f, 78f, 112f, 82f, orange)
        text(c, strings.t("debug_info", "x" to world.x.toInt(), "ticks" to world.ticks, "level" to currentLevel), 22f, 95f, 16f, rgb(255, 213, 115))
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
            val label = when(id) { 1 -> "◀"; 2 -> "▶"; 3 -> "▲"; 5 -> strings.t("puzzle_action"); else -> "#" }
            design.drawControl(c, b, label, active(id) || (id == 4 && shift))
        }
    }

    private fun drawPlayer(c: Canvas) {
        val moving = abs(world.vx) > 8f
        val index = if (moving) ((heroAnimTime / 0.09f).toInt() % heroFrames.size) else 0
        val frame = heroFrames[index]
        val playerCenter = world.x + GameWorld.SIZE * .5f
        val spriteScale = if (currentLevel in 1..10) GameVisualConfig.HERO_SCALE else 1f
        val spriteW = (GameWorld.SIZE + 18f) * spriteScale
        val spriteH = (GameWorld.SIZE + 19f) * spriteScale
        val bottom = world.y + GameWorld.SIZE + 11f
        val dest = RectF(playerCenter - spriteW * .5f, bottom - spriteH,
            playerCenter + spriteW * .5f, bottom)
        rounded(c, RectF(world.x - 2f, world.y + GameWorld.SIZE - 3f, world.x + GameWorld.SIZE + 2f, world.y + GameWorld.SIZE + 5f), 6f, alpha(Color.BLACK, 65))
        drawBitmapFit(c, frame, dest, 255, 0f, flipX = !heroFacingRight)
    }

    private fun drawBackground(c: Canvas, heroX: Float = GameBackground.START_X) {
        if (screen != Screen.PLAY) {
            // Keep the familiar factory on the home menu.
            backgroundRenderer.draw(c, w, h, heroX)
            return
        }
        val chapter = ((currentLevel - 1) / 10 + 1).coerceIn(1, WorldCatalog.WORLD_COUNT)
        val progress = ((heroX - level.spec.startX) / (level.worldRight - level.spec.startX).coerceAtLeast(1f))
            .coerceIn(0f, 1f)
        worldBackgrounds.draw(c, chapter, w, h, progress)
    }

    private fun drawMenu(c: Canvas) {
        drawBackground(c)
        rect(c, 0f, 0f, w, h, alpha(panel, 116))
        c.save()
        c.translate(GameViewport.centerOffset(w), 0f)
        text(c, strings.t("brand_escape"), 274f, 173f, 92f, whiteSoft, true)
        text(c, strings.t("brand_shift"), 364f, 260f, 92f, rgb(110, 241, 178), true)
        textFitted(c, strings.t("tagline"), 274f, 309f, 26f, 520f, rgb(174, 201, 214))
        menuButton(c, 340f, 354f, strings.t("play"), rgb(48, 163, 132))
        menuButton(c, 340f, 443f, strings.t("worlds"), GameDesign.BLACK)
        if (debugMode) text(c, strings.t("debug_all"), 290f, 544f, 19f, rgb(255, 216, 131))
        c.restore()
    }

    /** Ten world pages slide horizontally; each page owns its panoramic backdrop. */
    private fun drawLevels(c: Canvas) {
        // Render only pages visible during the swipe animation (never all 10 large images).
        val first = worldScroll.toInt().coerceIn(0, WorldCatalog.WORLD_COUNT - 1)
        val last = ceil(worldScroll.toDouble()).toInt().coerceIn(0, WorldCatalog.WORLD_COUNT - 1)
        for (index in first..last) {
            c.save()
            c.translate((index - worldScroll) * w, 0f)
            drawWorldPage(c, index + 1)
            c.restore()
        }
    }

    private fun drawWorldPage(c: Canvas, number: Int) {
        val info = WorldCatalog.world(number)
        worldBackgrounds.draw(c, number, w, h,
            parallax = (0.5f + 0.12f * (worldScroll - (number - 1))).coerceIn(0f, 1f), overlayAlpha = 58)
        // Readable but translucent panes, preserving a clear view of each landscape.
        val offset = GameViewport.centerOffset(w)
        c.save()
        c.translate(offset, 0f)
        rounded(c, RectF(92f, 32f, 908f, 520f), 20f, alpha(panel, 104))
        rect(c, 108f, 48f, 892f, 52f, GameDesign.ORANGE)
        text(c, strings.t("world_count", "n" to number.toString().padStart(2, '0')), 130f, 98f, 24f, orangeLight, true)
        textFitted(c, strings.t(info.title), 130f, 145f, 43f, 440f, whiteSoft, true)
        textFitted(c, strings.t(info.subtitle), 131f, 179f, 19f, 505f, rgb(203, 220, 229))
        if (!info.developed) {
            textFitted(c, strings.t("world_soon"), 582f, 96f, 19f, 289f, orangeLight, true)
        } else {
            textFitted(c, strings.t("levels_playable"), 606f, 96f, 18f, 260f, rgb(159, 248, 204), true)
        }
        for (i in 0 until WorldCatalog.LEVELS_PER_WORLD) {
            val x = 126f + (i % 5) * 151f
            val y = 204f + (i / 5) * 111f
            val enabled = WorldCatalog.isPlayable(number, i + 1) && accessible((number - 1) * 10 + i + 1)
            // Shared graphic style from V15, no world-specific redesign.
            design.drawLevelTile(c, RectF(x, y, x + 117f, y + 91f), i + 1, enabled)
        }
        if (!info.developed) textFitted(c, strings.t("world_preview") + " • " + strings.t("slots_soon"), 161f, 447f, 19f, 696f, whiteSoft)
        else textFitted(c, strings.t("unlock_hint"), 200f, 447f, 20f, 640f, whiteSoft)
        design.drawMenuButton(c, RectF(340f, 463f, 660f, 514f), strings.t("back"), false)
        c.restore()

        if (number > 1) {
            rounded(c, RectF(10f, 221f, 86f, 320f), 13f, alpha(panel, 181))
            text(c, "◀", 24f, 289f, 47f, whiteSoft, true)
        }
        if (number < WorldCatalog.WORLD_COUNT) {
            rounded(c, RectF(w - 86f, 221f, w - 10f, 320f), 13f, alpha(panel, 181))
            text(c, "▶", w - 73f, 289f, 47f, whiteSoft, true)
        }
        // Simple discrete indicator; page navigation always stays within 1..10.
        val dotWidth = 17f
        val gap = 12f
        val totalWidth = 10f * dotWidth + 9f * gap
        val startX = (w - totalWidth) / 2f
        for (i in 0 until 10) {
            val color = if (i == number - 1) orangeLight else alpha(whiteSoft, 80)
            rounded(c, RectF(startX + i * (dotWidth + gap), 535f,
                startX + i * (dotWidth + gap) + dotWidth, 543f), 3f, color)
        }
    }

    private fun changeWorld(nextWorld: Int) {
        selectedWorld = WorldCatalog.clamp(nextWorld)
    }

    /** A preview never chains into a future (unimplemented) level. */
    private fun nextAfterWin(): Int? = if (previewSession) null
        else (currentLevel + 1).takeIf { it <= GameProgression.MAX_LEVEL && accessible(it) }

    private fun drawOverlay(c: Canvas) {
        rect(c, 0f, 0f, w, h, Color.argb(215, 6, 13, 22))
        c.save()
        c.translate(GameViewport.centerOffset(w), 0f)
        textCenteredFitted(c, strings.t(if (won) "win_title" else "lose_title"), 500f, 190f, 75f, 780f, GameDesign.ORANGE, true)
        textCenteredFitted(
            c,
            if (won) strings.t("level_complete", "n" to currentLevel)
            else strings.t("lose_message"),
            500f, 246f, 24f, 790f, whiteSoft
        )
        val primaryLabel = if (!won) strings.t("retry") else if (nextAfterWin() != null) strings.t("next") else strings.t("levels")
        menuButton(c, 340f, 304f, primaryLabel, GameDesign.ORANGE)
        menuButton(c, 340f, 405f, strings.t("menu"), GameDesign.BLACK)
        c.restore()
    }

    /** Freeze gameplay and positional audio without losing the current level state. */
    private fun setPaused(value: Boolean) {
        if (isGamePaused == value) return
        isGamePaused = value
        pressed.clear()
        keyboardPressed.clear()
        frameTime = 0f // no physics catch-up after reopening the pause menu
        if (value) gameAudio.pause() else gameAudio.resume()
        onPauseChanged?.invoke(value)
        invalidate()
    }

    private fun drawPauseOverlay(c: Canvas) {
        rect(c, 0f, 0f, w, h, Color.argb(198, 6, 10, 15))
        c.save()
        c.translate(GameViewport.centerOffset(w), 0f)
        textCenteredFitted(c, strings.t("pause_title"), 500f, 175f, 63f, 720f, whiteSoft, true)
        menuButton(c, 340f, 220f, strings.t("resume"), GameDesign.ORANGE)
        menuButton(c, 340f, 310f, strings.t("restart"), GameDesign.BLACK)
        menuButton(c, 340f, 400f, strings.t("menu"), GameDesign.BLACK)
        c.restore()
    }

    /** A soft black vignette closes around the hero, then the GAME OVER panel appears. */
    private fun drawDeathIris(c: Canvas) {
        val duration = if (hearts > 1) GameVisualConfig.RESPAWN_IRIS_SECONDS
            else GameVisualConfig.DEATH_IRIS_DURATION_SECONDS
        val progress = (deathIrisElapsed / duration).coerceIn(0f, 1f)
        val eased = progress * progress * (3f - 2f * progress)
        val radius = (max(w, h) * 1.3f) * (1f - eased) + 5f
        val cx = world.x + GameWorld.SIZE * .5f - cameraX
        val cy = world.y + GameWorld.SIZE * .5f
        val edge = GameVisualConfig.DEATH_IRIS_SOFT_EDGE
        // Each annulus has its own alpha; outer parts darken sooner than the soft edge.
        for (step in 8 downTo 0) {
            val holeRadius = radius + step * (edge / 8f)
            val shape = Path().apply {
                fillType = Path.FillType.EVEN_ODD
                addRect(0f, 0f, w, h, Path.Direction.CW)
                addCircle(cx, cy, holeRadius, Path.Direction.CW)
            }
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(if (step == 0) (150 + 100 * progress).toInt().coerceIn(0, 255) else 20,
                0, 0, 0)
            c.drawPath(shape, paint)
        }
    }

    private fun menuButton(c: Canvas, x: Float, y: Float, label: String, color: Int) {
        design.drawMenuButton(c, RectF(x, y, x + 320f, y + 72f), label, color != GameDesign.BLACK && label != strings.t("menu") && label != strings.t("back"))
    }

    private fun startLevel(levelNumber: Int, preview: Boolean = WorldCatalog.isPreviewLevel(levelNumber)) {
        if (!accessible(levelNumber)) return
        onLeaveResult?.invoke()
        currentLevel = levelNumber
        previewSession = preview
        world = GameWorld(currentLevel, if (preview) WorldPreviewCatalog.create(levelNumber) else null)
        reset()
    }

    private fun reset() {
        onLeaveResult?.invoke() // Cancel any delayed ad when user chooses REJOUER/NEXT.
        gameAudio.pause()
        gameAudio.resume()
        world.reset()
        lastPushSoundTick = -100
        frameTime = 0f
        heroAnimTime = 0f
        heroFacingRight = true
        hintTime = 0f
        cameraX = 0f
        deathIrisElapsed = 0f
        // Only world 1 uses the new life system; worlds 2+ retain V23.1 behavior.
        hearts = if (currentLevel in 1..10 && !previewSession) GameVisualConfig.MAX_HEARTS else 1
        respawnCount = 0
        setPaused(false)
        winSaved = false
        endOfLevelReported = false
        pressed.clear()
        keyboardPressed.clear()
        screen = Screen.PLAY
    }

    /** Immediate retry: preserve heart count and global coin records, reset hazards. */
    private fun respawnAfterDeath() {
        hearts--
        respawnCount++
        world.reset()
        pressed.clear()
        keyboardPressed.clear()
        frameTime = 0f
        deathIrisElapsed = 0f
        cameraX = 0f
        heroAnimTime = 0f
        lastPushSoundTick = -100
        endOfLevelReported = false
        hintTime = 0f
    }

    /** Flat Canvas rendering, so the existing industrial 2D style is retained. */
    private fun drawDynamicTraps(c: Canvas) {
        for (v in world.dynamicTrapViews) {
            val b = RectF(v.box.left, v.box.top, v.box.right, v.box.bottom)
            when (v.type) {
                "collapse_floor" -> {
                    if (v.state == "active") continue
                    // The PNG floor is drawn over this segment by FactoryGround.
                    if (v.state == "warning") {
                        rect(c, b.left, b.top + 4f, b.right, b.top + 11f, orange)
                    }
                }
                "moving_platform" -> drawPlatform(c, b)
                "rising_spikes" -> {
                    if (v.state == "hidden") continue
                    if (v.state == "warning") {
                        rect(c, b.left, world.spec.groundY - 5f, b.right, world.spec.groundY, alpha(orange, 140))
                    } else {
                        val count = kotlin.math.max(1, kotlin.math.ceil(b.width() / 27f).toInt())
                        for (j in 0 until count) {
                            val l = b.left + j * b.width() / count
                            val rr = b.left + (j + 1) * b.width() / count
                            val path = Path().apply {
                                moveTo(l, b.bottom)
                                lineTo((l + rr) * .5f, b.top)
                                lineTo(rr, b.bottom)
                                close()
                            }
                            paint.color = orange
                            paint.style = Paint.Style.FILL
                            c.drawPath(path, paint)
                        }
                    }
                }
                "surprise_wall" -> if (v.state == "active") {
                    drawPlatform(c, b)
                    rect(c, b.left + 3f, b.top + 3f, b.right - 3f, b.top + 8f, orange)
                }
                "moving_exit" -> if (v.state == "warning") {
                    rect(c, b.left + 7f, b.top, b.right - 7f, b.top + 4f, orange)
                }
                "reverse_zone" -> {
                    rect(c, b.left, b.top, b.right, b.top + 3f, alpha(cyan, 140))
                    rect(c, b.left, b.bottom - 3f, b.right, b.bottom, alpha(cyan, 140))
                }
            }
        }
    }

    private fun gameButtonForKey(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> 1
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> 2
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_SPACE -> 3
        KeyEvent.KEYCODE_S -> 4
        KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_ENTER -> 5
        else -> null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (screen == Screen.PLAY && isGamePaused) return true
        if (debugMode && DevConfig.ALLOW_LEVEL_SHORTCUTS && screen == Screen.PLAY) {
            if (keyCode == KeyEvent.KEYCODE_PAGE_UP) { startLevel((currentLevel + 1).coerceAtMost(GameProgression.MAX_LEVEL)); return true }
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
        if (screen == Screen.PLAY && dead && (hearts > 1 ||
                deathIrisElapsed < GameVisualConfig.DEATH_IRIS_DURATION_SECONDS)) return true
        if (screen == Screen.PLAY && isGamePaused) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val (x, y) = centeredCoord(event.actionIndex)
                if (x in 340f..660f) when {
                    y in 220f..292f -> setPaused(false)
                    y in 310f..382f -> reset()
                    y in 400f..472f -> {
                        onLeaveResult?.invoke()
                        setPaused(false)
                        screen = Screen.MENU
                    }
                }
            }
            return true
        }
        if (screen == Screen.PLAY && (dead || won)) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val (x, y) = centeredCoord(event.actionIndex)
                if (x in 340f..660f && y in 304f..376f) {
                    if (!won) reset()
                    else {
                        val next = nextAfterWin()
                        if (next != null) startLevel(next, preview = false)
                        else { onLeaveResult?.invoke(); selectedWorld = ((currentLevel - 1) / 10) + 1; worldScroll = (selectedWorld - 1).toFloat(); screen = Screen.LEVELS; pressed.clear(); keyboardPressed.clear() }
                    }
                } else if (x in 340f..660f && y in 405f..477f) {
                    onLeaveResult?.invoke()
                    screen = Screen.MENU; pressed.clear(); keyboardPressed.clear()
                }
            }
            return true
        }
        if (screen == Screen.LEVELS) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val (x, y) = coord(event.actionIndex)
                    worldTouchStartX = x; worldTouchStartY = y
                    worldTouchStartValid = true
                }
                MotionEvent.ACTION_UP -> {
                    val (x, y) = coord(event.actionIndex)
                    val dx = x - worldTouchStartX
                    val dy = y - worldTouchStartY
                    val swipeTarget = WorldCatalog.swipe(selectedWorld, dx, dy)
                    if (worldTouchStartValid && swipeTarget != selectedWorld) changeWorld(swipeTarget)
                    else if (worldTouchStartValid && abs(dx) < 35f && abs(dy) < 35f) {
                        val cx = x - GameViewport.centerOffset(w)
                        if (cx in 340f..660f && y in 463f..514f) screen = Screen.MENU
                        else if (x in 10f..86f && y in 221f..320f) changeWorld(selectedWorld - 1)
                        else if (x in (w - 86f)..(w - 10f) && y in 221f..320f) changeWorld(selectedWorld + 1)
                        else if (WorldCatalog.isPlayable(selectedWorld, 1)) {
                            for (i in 0 until WorldCatalog.LEVELS_PER_WORLD) {
                                val bx = 126f + (i % 5) * 151f
                                val by = 204f + (i / 5) * 111f
                                if (cx in bx..(bx + 117f) && y in by..(by + 91f) && accessible((selectedWorld - 1) * 10 + i + 1)) {
                                    startLevel((selectedWorld - 1) * 10 + i + 1)
                                    break
                                }
                            }
                        }
                    }
                    worldTouchStartValid = false
                }
                MotionEvent.ACTION_CANCEL -> worldTouchStartValid = false
            }
            invalidate()
            return true
        }
        if (screen != Screen.PLAY) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val (x, y) = centeredCoord(event.actionIndex)
                if (screen == Screen.MENU && x in 340f..660f && y in 354f..426f) startLevel(
                    if (debugMode) DevConfig.START_LEVEL.coerceIn(1, GameProgression.MAX_LEVEL) else GameProgression.latestPlayable(unlocked),
                    preview = false // JOUER always uses the original V20 sequence, never a showcase.
                )
                else if (screen == Screen.MENU && x in 340f..660f && y in 443f..515f) screen = Screen.LEVELS
            }
            return true
        }
        if (event.actionMasked == MotionEvent.ACTION_UP && coord(event.actionIndex).let { pauseButton.contains(it.first, it.second) }) {
            setPaused(true)
            return true
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
        val baseW = r.width() + 38f
        val baseH = r.height() + 38f
        val laserScale = if (currentLevel in 1..10) GameVisualConfig.LASER_SCALE else 1f
        val newW = baseW * laserScale
        val newH = baseH * laserScale
        val dest = RectF(r.centerX() - newW * .5f, r.centerY() - newH * .5f,
            r.centerX() + newW * .5f, r.centerY() + newH * .5f)
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
        text(c, if (activated) strings.t("switch_ok") else strings.t("switch_on"), r.left + 8f, r.top - 12f, 16f, if (activated) rgb(122, 236, 214) else rgb(155, 255, 242), true)
    }


    private fun drawMonster(c: Canvas, r: RectF) {
        rounded(c, RectF(r.left + 6f, r.bottom - 4f, r.right - 6f, r.bottom + 4f), 6f, alpha(Color.BLACK, 72))
        val dest = RectF(r.left - 8f, r.top - 10f, r.right + 8f, r.bottom + 4f)
        drawBitmapFit(c, monsterSprite, dest)
    }

    /** White circular falling flake, intentionally no image asset in V20. */
    private fun drawSnowflake(c: Canvas, b: HitBox) {
        val cx = (b.left + b.right) / 2f
        val cy = (b.top + b.bottom) / 2f
        val r = (b.right - b.left) / 2f
        paint.style = Paint.Style.FILL
        paint.color = alpha(whiteSoft, 75)
        c.drawCircle(cx, cy, r + 4f, paint)
        paint.color = alpha(whiteSoft, 245)
        c.drawCircle(cx, cy, r, paint)
        paint.color = rgb(172, 213, 228)
        c.drawCircle(cx - 3f, cy - 3f, r * .26f, paint)
    }

    private fun drawPressurePlate(c: Canvas, b: RectF, active: Boolean) {
        val trim = if (active) rgb(89, 238, 177) else orangeLight
        rect(c, b.left - 2f, b.top - 5f, b.right + 2f, b.bottom + 3f, steelDark)
        rect(c, b.left + 2f, b.top - 4f, b.right - 2f, b.top + 4f, trim)
        rect(c, b.left + 8f, b.top + 6f, b.right - 8f, b.top + 9f, whiteSoft)
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
    private fun textCenteredFitted(c: Canvas, s: String, centerX: Float, y: Float,
                                   size: Float, maxWidth: Float, color: Int, bold: Boolean = false) {
        paint.textSize = size
        paint.typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        val measured = paint.measureText(s)
        val actualSize = if (measured > maxWidth && measured > 0f) (size * maxWidth / measured).coerceAtLeast(12f) else size
        paint.textSize = actualSize
        text(c, s, centerX - paint.measureText(s) / 2f, y, actualSize, color, bold)
    }

    /** For long translations (German/Hindi), shrink text instead of clipping it. */
    private fun textFitted(c: Canvas, s: String, x: Float, y: Float, size: Float,
                           maxWidth: Float, color: Int, bold: Boolean = false) {
        paint.textSize = size
        paint.typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        val measured = paint.measureText(s)
        text(c, s, x, y, if (measured > maxWidth && measured > 0f) (size * maxWidth / measured).coerceAtLeast(11f) else size, color, bold)
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        paint.color = color
        paint.textSize = size
        paint.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
        paint.style = Paint.Style.FILL
        c.drawText(s, x, y, paint)
    }

    private enum class Screen { MENU, LEVELS, PLAY }
}
