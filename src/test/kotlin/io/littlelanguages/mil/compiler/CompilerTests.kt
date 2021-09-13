package io.littlelanguages.mil.compiler

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerContext
import io.kotest.matchers.shouldBe
import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.compiler.llvm.Context
import io.littlelanguages.mil.compiler.llvm.Module
import io.littlelanguages.mil.dynamic.Binding
import io.littlelanguages.mil.dynamic.translate
import io.littlelanguages.mil.static.Scanner
import io.littlelanguages.mil.static.parse
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.yaml.snakeyaml.Yaml
import java.io.*


private val yaml = Yaml()

class CompilerTests : FunSpec({
    context("Conformance Tests") {
        val context = Context()
        val content = File("./src/test/kotlin/io/littlelanguages/mil/compiler/compiler.yaml").readText()

        val scenarios: Any = /*emptyList<String>() */ yaml.load(content)

        if (scenarios is List<*>) {
            parserConformanceTest(builtinBindings, context, this, scenarios)
        }

        context.dispose()
    }
})

fun compile(builtinBindings: List<Binding<CompileState, LLVMValueRef>>, context: Context, input: String): Either<List<Errors>, Module> =
    parse(Scanner(StringReader(input))) mapLeft { listOf(it) } andThen { translate(builtinBindings, it) } andThen { compile(context, "test", it) }

suspend fun parserConformanceTest(
    builtinBindings: List<Binding<CompileState, LLVMValueRef>>,
    context: Context,
    ctx: FunSpecContainerContext,
    scenarios: List<*>
) {
    scenarios.forEach { scenario ->
        val s = scenario as Map<*, *>

        val nestedScenario = s["scenario"] as Map<*, *>?
        if (nestedScenario == null) {
            val name = s["name"] as String
            val input = s["input"] as String
            val output = s["output"]

            ctx.test(name) {
                val lhs = when (val llvmState = compile(builtinBindings, context, input)) {
                    is Left ->
                        llvmState.left.joinToString("")

                    is Right -> {
                        val module = llvmState.right

//                        LLVM.LLVMDumpModule(module)
//                        System.err.println(LLVM.LLVMPrintModuleToString(module).string)
                        module.writeBitcodeToFile("test.bc")
                        runCommand(arrayOf("clang", "test.bc", "src/main/c/lib.o", "./src/main/c/main.o", "-o", "test.bin"))
                        val commandOutput = runCommand(arrayOf("./test.bin"))

                        module.dispose()

                        commandOutput
                    }
                }

                val rhs =
                    (output as Any).toString().trim()

                lhs shouldBe rhs
            }
        } else {
            val name = nestedScenario["name"] as String
            val tests = nestedScenario["tests"] as List<*>
            ctx.context(name) {
                parserConformanceTest(builtinBindings, context, this, tests)
            }
        }
    }
}

private fun runCommand(commands: Array<String>): String {
    val rt = Runtime.getRuntime()
    val proc = rt.exec(commands)

    val sb = StringBuffer()

    fun readInputStream(input: InputStream) {
        BufferedReader(InputStreamReader(input)).use { reader ->
            var s: String?
            while (reader.readLine().also { s = it } != null) {
                sb.append(s)
                sb.append("\n")
            }
        }
    }

    readInputStream(proc.inputStream)
    readInputStream(proc.errorStream)

    return sb.toString().trim()
}