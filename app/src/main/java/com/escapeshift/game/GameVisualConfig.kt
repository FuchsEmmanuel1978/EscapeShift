package com.escapeshift.game

/** V22.1 visual settings. Opacity = 255 - transparency percent * 255. */
object GameVisualConfig {
    const val MAX_HEARTS = 3
    const val RESPAWN_IRIS_SECONDS = 0.28f  // short retry, no interstitial between hearts
    const val CONTROL_BACKGROUND_ALPHA = 51  // 80% transparent white
    const val MOVE_TRIANGLE_ALPHA = 128       // 50% visible
    const val JUMP_TRIANGLE_ALPHA = 51        // 80% transparent
    const val SHIFT_HASH_ALPHA = 128          // 50% visible
    const val HERO_SPEED_MULTIPLIER = 1.20f // actual movement, all playable worlds
    const val PAUSE_BACKGROUND_ALPHA = 51  // translucent pause icon, no border
    const val PAUSE_SYMBOL_ALPHA = 200
    const val DEATH_IRIS_DURATION_SECONDS = 1.05f
    const val DEATH_IRIS_SOFT_EDGE = 80f
    const val HERO_SCALE = 1.50f             // visual sprite only; collision size unchanged
    const val LASER_SCALE = 1.25f            // visual sprite only; hitbox unchanged
    const val GROUND_TILE_SCREEN_FRACTION = 0.10f
    const val GROUND_TILE_HEIGHT_FRACTION = 0.20f
    const val WORLD1_CONVEYOR_SPEED = -800f  // strong leftward pull on contact
    const val WORLD1_BELT_JUMP_MULTIPLIER = 1.52f  // cross the longer belt by jumping
}
