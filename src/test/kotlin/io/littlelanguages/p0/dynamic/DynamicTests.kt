package io.littlelanguages.p0.dynamic

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerContext
import io.kotest.matchers.shouldBe
import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.p0.Errors
import io.littlelanguages.p0.dynamic.tst.Program
import io.littlelanguages.p0.static.Scanner
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.StringReader

private val yaml = Yaml()


class DynamicTests : FunSpec({
    context("Conformance Tests") {
        val content = File("./src/test/kotlin/io/littlelanguages/p0/dynamic/dynamic.yaml").readText()

        val scenarios: Any = yaml.load(content)

        if (scenarios is List<*>) {
            conformanceTest(this, scenarios)
        }
    }
})


fun parse(input: String): Either<List<Errors>, Program> =
    io.littlelanguages.p0.static.parse(Scanner(StringReader(input)))
        .mapLeft { listOf(it) }
        .andThen { translate(it) }

suspend fun conformanceTest(ctx: FunSpecContainerContext, scenarios: List<*>) {
    scenarios.forEach { scenario ->
        val s = scenario as Map<*, *>

        val nestedScenario = s["scenario"] as Map<*, *>?
        if (nestedScenario == null) {
            val name = s["name"] as String
            val input = s["input"] as String
            val output = s["output"]

            ctx.test(fixName(name)) {
                val parseResult =
                    parse(input)

                val lhs =
                    when (parseResult) {
                        is Left ->
                            parseResult.left.map { it.yaml() }.toString()

                        is Right ->
                            parseResult.right.yaml().toString()
                    }

                val rhs =
                    (output as Any).toString()

                lhs shouldBe rhs
            }
        } else {
            val name = nestedScenario["name"] as String
            val tests = nestedScenario["tests"] as List<*>
            ctx.context(fixName(name)) {
                conformanceTest(this, tests)
            }
        }
    }
}

fun fixName(n: String): String =
    if (n.startsWith("!")) " $n" else n
