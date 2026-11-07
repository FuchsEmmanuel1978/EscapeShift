package com.escapeshift.game

/** Runs with the real Kotlin trap engine, no Android SDK required. */
fun main() {
    fun step(e: TrapEngine, p: HitBox, count: Int, jump: Boolean = false, speed: Float = 300f) {
        repeat(count) { e.update(p, p, jump, speed) }
    }
    val contact = HitBox(106f, 400f, 135f, 444f)
    val floor = TrapEngine(listOf(TrapDefinition("f", "collapse_floor", 100f, 444f, 110f, 146f, "player_on", delayTicks = 4)))
    check(floor.solids().size == 1)
    step(floor, contact, 3)
    check(floor.solids().size == 1)
    step(floor, contact, 3)
    check(floor.solids().isEmpty() && floor.collapsedGaps().size == 1)
    floor.reset()
    check(floor.collapsedGaps().isEmpty() && floor.solids().size == 1)

    val spikes = TrapEngine(listOf(TrapDefinition("s", "rising_spikes", 100f, 412f, 70f, 32f,
        trigger = "player_near", radius = 90f, delayTicks = 2)))
    check(spikes.spikes().isEmpty())
    step(spikes, contact, 5)
    check(spikes.spikes().size == 1)

    val wall = TrapEngine(listOf(TrapDefinition("w", "surprise_wall", 100f, 393f, 40f, 51f,
        trigger = "player_near", radius = 100f, delayTicks = 2)))
    check(wall.solids().isEmpty())
    step(wall, contact, 6)
    check(wall.solids().isNotEmpty())

    val platform = TrapEngine(listOf(TrapDefinition("p", "moving_platform", 100f, 350f, 100f, 20f,
        travelX = 80f, periodTicks = 120)))
    val before = platform.platforms().single()
    step(platform, contact, 30)
    check(platform.platforms().single().left > before.left)

    val door = TrapEngine(listOf(TrapDefinition("d", "moving_exit", 100f, 350f, 65f, 90f,
        trigger = "player_near", radius = 150f, delayTicks = 2, travelX = 55f, transitionTicks = 5)))
    val initial = HitBox(100f, 350f, 165f, 440f)
    check(door.exit(initial).left == initial.left)
    step(door, contact, 12)
    check(door.exit(initial).left == 155f)

    val reverse = TrapEngine(listOf(TrapDefinition("r", "reverse_zone", 100f, 390f, 100f, 100f)))
    check(reverse.reverseControls(contact))
    check(!reverse.reverseControls(HitBox(300f, 400f, 339f, 439f)))

    LevelCatalog.useProjectFiles()
    for (n in 1..10) {
        val world = GameWorld(n)
        check(world.dynamicTrapViews.size == 2) { "World 1, level $n should have two dynamic traps" }
        check(LevelPlaytester.test(n).passed) { "Unplayable world 1 level $n" }
    }
    for (n in 11..20) check(LevelPlaytester.test(n).passed)
    println("PASS: 6 trap mechanisms, reset, JSON loading and 20 winning paths")
}
