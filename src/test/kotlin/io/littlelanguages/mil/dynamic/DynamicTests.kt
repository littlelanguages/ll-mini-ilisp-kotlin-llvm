package io.littlelanguages.mil.dynamic

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerContext
import io.kotest.matchers.shouldBe
import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.dynamic.tst.Expressionss
import io.littlelanguages.mil.dynamic.tst.Program
import io.littlelanguages.mil.static.Scanner
import io.littlelanguages.mil.static.parse
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.StringReader

private val yaml = Yaml()

typealias S = String
typealias T = String

class DynamicTests : FunSpec({
    context("Conformance Tests") {
        val content = File("./src/test/kotlin/io/littlelanguages/mil/dynamic/dynamic.yaml").readText()

        val scenarios: Any = yaml.load(content)

        val builtinBindings: List<ExternalProcedureBinding<S, T>> = listOf(
            DummyVariableArityExternalProcedure("+"),
            DummyVariableArityExternalProcedure("-"),
            DummyVariableArityExternalProcedure("*"),
            DummyVariableArityExternalProcedure("println")
        )

        if (scenarios is List<*>) {
            parserConformanceTest(builtinBindings, this, scenarios)
        }
    }
})

private class DummyVariableArityExternalProcedure(
    override val name: String
) : ExternalProcedureBinding<S, T>(name, null) {
    override fun compile(state: S, arguments: Expressionss<S, T>): T? = null
}

fun translate(builtinBindings: List<Binding<S, T>>, input: String): Either<List<Errors>, Program<S, T>> =
    parse(Scanner(StringReader(input))) mapLeft { listOf(it) } andThen { translate(builtinBindings, it) }

suspend fun parserConformanceTest(builtinBindings: List<Binding<S, T>>, ctx: FunSpecContainerContext, scenarios: List<*>) {
    scenarios.forEach { scenario ->
        val s = scenario as Map<*, *>

        val nestedScenario = s["scenario"] as Map<*, *>?
        if (nestedScenario == null) {
            val name = s["name"] as String
            val input = s["input"] as String
            val output = s["output"]

            ctx.test(name) {
                val rhs =
                    output.toString()

                when (val lhs = translate(builtinBindings, input)) {
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
                parserConformanceTest(builtinBindings, this, tests)
            }
        }
    }
}
