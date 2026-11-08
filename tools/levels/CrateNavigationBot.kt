package com.escapeshift.game
import java.io.File
import kotlin.math.abs

/** Experimental playtest bot with only game-key inputs: no state modification. */
fun main(args:Array<String>) {
 LevelCatalog.install{ n->File(args[0],LevelCatalog.filename(n)).readText() }
 val n=args.getOrNull(1)?.toIntOrNull() ?:25
 fun tick(g:GameWorld, left:Boolean=false,right:Boolean=false,jump:Boolean=false,act:Boolean=false) {
    g.tick(GameInput(left=left,right=right,jump=jump,interact=act))
    check(!g.dead) { "Death in level ${g.levelNumber}, x=${g.x}" }
 }
 fun interact(g:GameWorld) {tick(g,act=true);tick(g)}
 fun goto(g:GameWorld, target:Float) {
  var jumpPrev=false;var stuck=0;var previous=g.x
  repeat(1300) {
    if (abs(g.x-target)<4.9f || g.won) return
    val right=g.x<target
    val obs=g.crateBoxes.filter { it.left >= 0f && it.left < g.worldRight &&
       (if(right) it.right < target-5f && it.left >= g.x else it.left > target+5f && it.right <= g.x+GameWorld.SIZE+145) }
    val near=obs.any {
      if (right) it.left-(g.x+GameWorld.SIZE) in -8f..45f
      else (g.x-it.right) in -8f..45f
    }
    val jumpNeeded=g.grounded && !g.carryingCrate && (near || stuck>13 || (n==33 && g.x in 1325f..1360f))
    val jump=jumpNeeded && !jumpPrev
    tick(g,left=!right,right=right,jump=jump)
    jumpPrev=jumpNeeded
    stuck=if(abs(g.x-previous)<.6f) stuck+1 else 0
    previous=g.x
  }
  error("Not reachable x=${g.x}, target=$target, exitUnlocked=${g.exitUnlocked}, exit=${g.exit}, player=${g.playerBox}, won=${g.won}, ground=${g.grounded}, crates=${g.crateBoxes}")
 }
 fun land(g:GameWorld) { repeat(75) { if (g.grounded) return;tick(g) } }
 fun deliver(g:GameWorld,cr:String,pad:String) {
  val plan=g.advancedPuzzle!!.plan
  val i=plan.all("crate").indexOfFirst { it.id==cr }
  check(i>=0)
  val b=g.crateBoxes[i]
  val clusteredLeft = g.crateBoxes.withIndex().any { (otherIndex,other) ->
      otherIndex != i && other.right <= b.left + 2f && b.left - other.right < 24f
  }
  val pickupAt = if(clusteredLeft) b.right + 12f else b.left - 44f
  goto(g,pickupAt);land(g)
  println("  BEFORE $cr hero=${g.x} boxes=${g.crateBoxes.mapIndexed { idx,v -> idx to v.left }}")
  interact(g)
  check(g.carryingCrate) { "Failed pick $cr: hero=${g.x}, boxes=${g.crateBoxes}" }
  check(g.crateBoxes[i].top < 400f) { "Wrong crate picked for $cr" }
  val d=plan.find(pad)!!
  goto(g,d.x-49f);land(g)
  interact(g)
  check(!g.carryingCrate)
  val f = GameWorld::class.java.getDeclaredField("lockedCrateIndices")
  f.isAccessible = true
  println("  $cr -> $pad: x=${g.x} box=${g.crateBoxes[i]} locks=${f.get(g)}")
 }
 val g=GameWorld(n)
 val placements=when(n) {
 25 -> mapOf("crate02" to "pad1","crate04" to "pad2","crate06" to "pad3","crate08" to "pad4","crate10" to "pad5")
 36 -> mapOf("crate5" to "pad1","crate3" to "pad2","crate1" to "pad3")
 28 -> mapOf("crateA" to "plateB","crateB" to "plateA")
 33 -> mapOf("crate2" to "pad1","crate3" to "pad3")
 38 -> mapOf("crateA" to "padB","crateB" to "padA")
 40 -> mapOf("crate2" to "padA","crate3" to "padA")
  else -> error("No route")
 }
if (n==38) { goto(g, 325f); repeat(6) { interact(g) }; println("38 wheel="+g.advancedPuzzle!!.wheelAngle("wheelA")) }
 for ((k,v) in placements) deliver(g,k,v)
 if(n==28 || n==38) {
   val sp=g.advancedPuzzle!!.plan.find("springA")!!
   goto(g,sp.x-40f)
   goto(g,sp.targetX-40f)
   println("spring position "+g.advancedPuzzle!!.box("springA"))
 }
 if(n==36) {
  goto(g,860f);interact(g)
  println("board="+g.advancedPuzzle!!.feedback)
 }
 if(n==33) { goto(g,2015f);println("33 visited=${g.advancedPuzzle!!.isSolved} gravity=${g.advancedPuzzle!!.gravityActive}") }
 println("Puzzle $n: solved=${g.advancedPuzzle!!.isSolved}, feedback=${g.advancedPuzzle!!.feedback}, crates=${g.crateBoxes}")
 if(g.advancedPuzzle!!.isSolved) {
   goto(g,2480f); land(g)
   println("LEVEL $n FINAL x=${g.x} win=${g.won} death=${g.dead}")
 }
}
