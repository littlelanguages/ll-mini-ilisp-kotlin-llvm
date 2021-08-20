package io.littlelanguages.mil.dynamic

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerContext
import io.kotest.matchers.shouldBe
import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.dynamic.tst.Program
import io.littlelanguages.mil.static.Scanner
import io.littlelanguages.mil.static.parse
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.StringReader

private val yaml = Yaml()


class DynamicTests : FunSpec({
    context("Conformance Tests") {
        val content = File("./src/test/kotlin/io/littlelanguages/mil/dynamic/dynamic.yaml").readText()

        val scenarios: Any = yaml.load(content)

        if (scenarios is List<*>) {
            parserConformanceTest(this, scenarios)
        }
    }
})


fun translate(input: String): Either<List<Errors>, Program> =
    parse(Scanner(StringReader(input))) mapLeft { listOf(it) } andThen { translate(it) }


suspend fun parserConformanceTest(ctx: FunSpecContainerContext, scenarios: List<*>) {
    scenarios.forEach { scenario ->
        val s = scenario as Map<*, *>

        val nestedScenario = s["scenario"] as Map<*, *>?
        if (nestedScenario == null) {
            val name = s["name"] as String
            val input = s["input"] as String
            val output = s["output"]

            ctx.test(name) {
//                val lhs =
//                    translate(input).map { it.yaml() }.toString()
                val rhs =
                    output.toString()

                when (val lhs = translate(input)) {
                    is Left ->
                        lhs.left.map { it.yaml() }.toString() shouldBe rhs
                    is Right ->
                        lhs.right.yaml().toString() shouldBe rhs
                }
            }
        } else {
            val name = nestedScenario["name"] as String
            val tests = nestedScenario["tests"] as List<*>
            ctx.context(name) {
                parserConformanceTest(this, tests)
            }
        }
    }
}
