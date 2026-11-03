import com.escapeshift.game.*
fun checkRoom(a: World1Adventure, i:Int) {
 a.enter(i)
 when(i) {
  1 -> {a.examineScene(true);for(d in "3142")a.digit(d.digitToInt(),true)}
  2 -> {a.collect(true);a.combine("badge","crochet");a.use("passepartout",true)}
  3 -> {a.collect(true);a.use("pince",true)}
  4 -> {a.examineScene(true);"TRC".forEach {a.symbol(it.toString(),true)}}
  5 -> {a.examineScene(true);"4207".forEach {a.digit(it.digitToInt(),true)}}
  6 -> {a.collect(true);a.combine("bobine","fusible");a.use("fusible_charge",true)}
  7 -> {a.collect(true);a.use("aimant",true)}
  8 -> {a.collect(true);a.combine("carte_labo","passepartout");a.use("badge_labo",true)}
  9 -> {a.collect(true);"RTC".forEach{a.symbol(it.toString(),true)}}
  10 -> {a.combine("puce","aimant");a.use("cle_coeur",true);"4207".forEach{a.digit(it.digitToInt(),true)}}
 }
 check(a.isSolved(i)) {"Salle $i BLOQUEE : ${a.message}, sac=${a.inventory}"}
 println("PASS Salle $i ${EscapeStory.room(i).title} objets=${a.inventory.size}")
}
fun main(){
 val a=World1Adventure()
 // Invalid actions should not bypass puzzle.
 a.enter(1);"0000".forEach{a.digit(it.digitToInt(),true)}
 check(!a.isSolved())
 a.digit(3,false);check(!a.isSolved())
 for (i in 1..10) checkRoom(a,i)
 check("cle_coeur" in a.inventory)
 val restored=World1Adventure().apply {restore(a.serialize())}
 check((1..10).all{restored.isSolved(it)})
 check("cle_coeur" in restored.inventory)
 val world=GameWorld(1)
 world.adventureExitReady = false
 check(!world.exitUnlocked)
 world.adventureExitReady = true
 check(world.exitUnlocked)
 println("PASS sauvegarde, verrouillage, codes errones et 10 salles")
}
