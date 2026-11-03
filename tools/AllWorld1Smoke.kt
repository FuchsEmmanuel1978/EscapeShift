import com.escapeshift.game.*
fun main() {
 val adventure=World1Adventure()
 for(i in 1..10){
  val p=LevelPlaytester.test(i)
  check(p.passed) {"Level $i physics blocked: ${p.checks.filterNot {it.passed}}"}
  println("PHYSICS $i PASS ticks=${p.ticks}")
 }
 println("10/10 existing platform routes playable")
}
