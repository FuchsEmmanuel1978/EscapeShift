import com.escapeshift.game.GamePhysicsTest
import com.escapeshift.game.BackgroundParallaxTest
import com.escapeshift.game.GameViewportTest
import org.junit.Test

fun main() {
    val suites = listOf(GamePhysicsTest(), BackgroundParallaxTest(), GameViewportTest())
    var failures = 0
    var total = 0
    suites.forEach { suite ->
      val methods = suite.javaClass.declaredMethods.filter { it.getAnnotation(Test::class.java) != null }
      total += methods.size
      methods.forEach { test ->
        try {
            test.invoke(suite)
            println("PASS ${test.name}")
        } catch (e: java.lang.reflect.InvocationTargetException) {
            failures++
            println("FAIL ${test.name}: ${e.targetException}")
        }
    }
    }
    println("TEST RESULTS: ${total - failures}/${total} passed")
    check(failures == 0)
}
