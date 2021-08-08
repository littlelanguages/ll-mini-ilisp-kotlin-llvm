package io.littlelanguages.mil.static

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerContext
import io.kotest.matchers.shouldBe
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.StringReader

class ScannerTests : FunSpec({
    context("Conformance Tests") {
        val content = File("./src/test/kotlin/io/littlelanguages/mil/static/lexical.yaml").readText()

        val yaml = Yaml()
        val scenarios: Any = yaml.load(content)

        if (scenarios is List<*>) {
            scannerConformanceTest(this, scenarios)
        }
    }
})


private fun assembleTokens(la: Scanner): List<Token> {
    val result = mutableListOf<Token>()

    result += la.current()
    while (la.current().tToken != TToken.TEOS) {
        la.next()
        result += la.current()
    }

    return result
}

private fun tokens(input: String): List<Token> =
    assembleTokens(Scanner(StringReader(input)))


suspend fun scannerConformanceTest(ctx: FunSpecContainerContext, scenarios: List<*>) {
    scenarios.forEach { scenario ->
        val s = scenario as Map<*, *>

        val nestedScenario = s["scenario"] as Map<*, *>?
        if (nestedScenario == null) {
            val name = s["name"] as String
            val input = s["input"] as String
            val output = s["output"] as List<*>

            ctx.test(name) {
                tokens(input).map { it.toString() } shouldBe output.map { (it as String).trim() }
            }
        } else {
            val name = nestedScenario["name"] as String
            val tests = nestedScenario["tests"] as List<*>
            ctx.context(name) {
                scannerConformanceTest(this, tests)
            }
        }
    }
}
