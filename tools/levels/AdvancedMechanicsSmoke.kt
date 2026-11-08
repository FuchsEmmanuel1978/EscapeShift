package com.escapeshift.game

import java.io.File

/**
 * Sensor and state-transition verification for all 17 advanced puzzles.
 * Unlike AdvancedPhysicsPlaytest, positions below are synthetic sensor inputs.
 * They prove door logic, not end-to-end reachability. Never call these full physical playtests.
 */
fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: "app/src/main/assets/levels")
    LevelCatalog.install { n -> File(root, LevelCatalog.filename(n)).readText() }
    fun near(d: AdvancedDevice): HitBox = d.box
    val away = HitBox(72f, 401f, 111f, 440f)
    fun scene(n: Int) = AdvancedPuzzle(LevelCatalog.create(n).advancedPuzzle!!)
    fun fill(p: AdvancedPuzzle, mapping: Map<String,String>): List<HitBox> {
        return p.crateDevices.map { crate ->
            val pad = mapping[crate.id]?.let { p.plan.find(it) }
            if (pad != null) {
                val others = p.crateDevices.takeWhile { it.id != crate.id }.count { mapping[it.id] == mapping[crate.id] }
                HitBox(pad.x + 5f + others * (crate.width+6), 401f,
                    pad.x + 5f + others * (crate.width+6) + crate.width, 444f)
            } else crate.box
        }
    }
    fun tick(p: AdvancedPuzzle, actor: HitBox = away, mapping: Map<String,String> = emptyMap(), airborne: Boolean=false) {
        p.tick(actor, fill(p,mapping), airborne, 300f)
    }
    fun touchToken(p:AdvancedPuzzle,id:String, mapping:Map<String,String> = emptyMap()) {
        tick(p, near(checkNotNull(p.plan.find(id))), mapping)
    }
    fun rotate(p:AdvancedPuzzle, turns:Int) {
        val wheel=checkNotNull(p.plan.find("wheelA"))
        repeat(turns) { check(p.interact(wheel.box)) }
    }
    for (n in 24..40) {
        val p = scene(n)
        tick(p)
        check(!p.isSolved) { "Level $n unlocked on spawn" }
        when(n) {
            24 -> {
                val pad=checkNotNull(p.plan.find("plateA"))
                tick(p, pad.box, mapOf("crateA" to "plateA", "crateB" to "plateA"))
                tick(p,away,mapOf("crateB" to "springA"))
            }
            25 -> tick(p,away,mapOf("crate02" to "pad1", "crate04" to "pad2", "crate06" to "pad3", "crate08" to "pad4", "crate10" to "pad5"))
            26 -> {
                rotate(p,3)
                val gap=checkNotNull(p.plan.find("gapA"))
                tick(p,HitBox(gap.x-80,401f,gap.x-41,440f))
                tick(p,HitBox(gap.x+gap.width+20,401f,gap.x+gap.width+59,440f))
            }
            27 -> listOf("n2","n3","n5","n7","n11","n13","n17").forEach{touchToken(p,it)}
            28 -> {
                tick(p,away,mapOf("crateA" to "plateB","crateB" to "plateA"))
                val d=p.plan.find("springA")!!;p.pushMovable("springA",d.targetX-d.x)
                tick(p,away,mapOf("crateA" to "plateB","crateB" to "plateA"))
            }
            29 -> listOf("O1","V1","E1","R1").forEach{touchToken(p,it)}
            30 -> {
                touchToken(p,"U21");touchToken(p,"13")
                tick(p,p.plan.find("gravityZone")!!.box)
                tick(p,p.plan.find("reverseZone")!!.box)
            }
            31 -> {
                val d=p.plan.find("bridgeA")!!;p.pushMovable("bridgeA",d.targetX-d.x)
                tick(p,away,mapOf("crate2" to "plateA","crate3" to "plateA"))
            }
            32 -> {
                for (j in 0..4) touchToken(p,"letter_$j")
                rotate(p,4);tick(p)
            }
            33 -> {
                val map=mapOf("crate2" to "pad1","crate3" to "pad3")
                tick(p,p.plan.find("gravityZone")!!.box,map)
                p.onPlatformLanding(0);tick(p,away,map)
            }
            34 -> {
                tick(p,away,mapOf("crateA" to "padA"));tick(p,away)
                val d=p.plan.find("springA")!!;p.pushMovable("springA",d.targetX-d.x)
                tick(p)
            }
            35 -> {
                touchToken(p,"25")
                val wall=p.plan.find("wallA")!!
                tick(p,HitBox(wall.x + wall.width + 7f,310f,wall.x + wall.width+46f,349f),airborne=true)
                val spike=p.plan.find("spikeA")!!
                tick(p,HitBox(spike.x+spike.width+7f,310f,spike.x+spike.width+46f,349f),airborne=true)
            }
            36 -> {
                val map=mapOf("crate5" to "pad1","crate3" to "pad2","crate1" to "pad3")
                tick(p,away,map)
                check(p.interact(p.plan.find("panelA")!!.box))
                tick(p,away,map)
            }
            37 -> {
                for (id in listOf("n2","n3","n5","n7")) touchToken(p,id)
                tick(p,p.plan.find("gravityZone")!!.box)
                p.onPlatformLanding(0);tick(p)
            }
            38 -> {
                rotate(p,6)
                val d=p.plan.find("springA")!!;p.pushMovable("springA",d.targetX-d.x)
                tick(p,away,mapOf("crateA" to "padB","crateB" to "padA"))
            }
            39 -> {
                for (j in 0..4) touchToken(p,"letter_$j")
                val map=mapOf("crateA" to "padA")
                tick(p,p.plan.find("fakeDoor")!!.box,map)
            }
            40 -> {
                touchToken(p,"30")
                rotate(p,2)
                check(p.interact(p.plan.find("wordA")!!.box))
                val d=p.plan.find("bridgeA")!!;p.pushMovable("bridgeA",d.targetX-d.x)
                tick(p,away,mapOf("crate2" to "padA","crate3" to "padA"))
            }
        }
        check(p.isSolved) { "Level $n sensor flow did not unlock, message=${p.feedback}" }
        p.reset()
        check(!p.isSolved)
        println("LEVEL $n: sensors and door condition PASS")
    }
    println("17/17 sensor-state tests PASS (NOT 17 completed in-game paths)")
    run {
        val p=scene(29)
        touchToken(p,"E2")
        for(id in listOf("O1","V1","E1","R1")) touchToken(p,id)
        check(p.hasFailed && !p.isSolved) { "Wrong-letter trap did not hold the door shut" }
    }
    run {
        val p=scene(25)
        tick(p,away,mapOf("crate02" to "pad1","crate04" to "pad2","crate06" to "pad3","crate08" to "pad4","crate09" to "pad5"))
        check(!p.isSolved && p.feedback.contains("4/5")) { "Mastermind was solved with wrong crate" }
    }
    println("Wrong token and wrong combination negative tests PASS")
}
