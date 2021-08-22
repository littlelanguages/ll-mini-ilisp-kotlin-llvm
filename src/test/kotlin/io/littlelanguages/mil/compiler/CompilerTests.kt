package io.littlelanguages.mil.compiler

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerContext
import io.kotest.matchers.shouldBe
import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.mil.ArgumentMismatchError
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.compiler.llvm.Builder
import io.littlelanguages.mil.compiler.llvm.Context
import io.littlelanguages.mil.compiler.llvm.Module
import io.littlelanguages.mil.dynamic.Binding
import io.littlelanguages.mil.dynamic.ExternalProcedureBinding
import io.littlelanguages.mil.dynamic.translate
import io.littlelanguages.mil.dynamic.tst.Expression
import io.littlelanguages.mil.dynamic.tst.LiteralInt
import io.littlelanguages.mil.static.Scanner
import io.littlelanguages.mil.static.ast.SExpression
import io.littlelanguages.mil.static.parse
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.yaml.snakeyaml.Yaml
import java.io.*


private val yaml = Yaml()

class CompilerTests : FunSpec({
    context("Conformance Tests") {
        val context = Context()
        val content = File("./src/test/kotlin/io/littlelanguages/mil/compiler/compiler.yaml").readText()

        val builtinBindings = listOf(
            ExternalProcedureBinding("boolean?", validateFixedArityArgument(1), compileFixedArity("_booleanp")),
            ExternalProcedureBinding("car", validateFixedArityArgument(1), compileFixedArity("_pair_car")),
            ExternalProcedureBinding("cdr", validateFixedArityArgument(1), compileFixedArity("_pair_cdr")),
            ExternalProcedureBinding("+", validateVariableArityArguments(), compileOperator(0, "_plus", false)),
            ExternalProcedureBinding("-", validateVariableArityArguments(), compileOperator(0, "_minus", true)),
            ExternalProcedureBinding("*", validateVariableArityArguments(), compileOperator(1, "_multiply", false)),
            ExternalProcedureBinding("/", validateVariableArityArguments(), compileOperator(1, "_divide", true)),
            ExternalProcedureBinding("pair", validateFixedArityArgument(2), compileFixedArity("_mk_pair"))
        )

        val scenarios: Any = yaml.load(content)

        if (scenarios is List<*>) {
            parserConformanceTest(builtinBindings, context, this, scenarios)
        }

        context.dispose()
    }
})

private fun validateFixedArityArgument(arity: Int): (e: SExpression, name: String, arguments: List<Expression<Builder, LLVMValueRef>>) -> Errors? =
    { e, name, arguments ->
        if (arity == arguments.size)
            null
        else
            ArgumentMismatchError(name, arity, arguments.size, e.position)
    }

private fun validateVariableArityArguments(): (e: SExpression, name: String, arguments: List<Expression<Builder, LLVMValueRef>>) -> Errors? =
    { _, _, _ -> null }

private fun compileFixedArity(externalName: String): (builder: Builder, arguments: List<Expression<Builder, LLVMValueRef>>) -> LLVMValueRef =
    { builder, arguments ->
        val namedFunction = builder.getNamedFunction(externalName) ?: builder.addExternalFunction(
            externalName,
            List(arguments.size) { builder.structValueP },
            builder.structValueP
        )
        builder.buildCall(namedFunction, arguments.map { compileEForce(builder, it) })
    }

private fun compileOperator(
    unitValue: Int,
    externalName: String,
    explicitFirst: Boolean
): (builder: Builder, arguments: List<Expression<Builder, LLVMValueRef>>) -> LLVMValueRef? =
    { builder, arguments ->
        val ops = arguments.mapNotNull { compileE(builder, it) }

        val namedFunction = builder.getNamedFunction(externalName) ?: builder.addExternalFunction(
            externalName,
            List(2) { builder.structValueP },
            builder.structValueP
        )

        if (ops.isEmpty())
            compileE(builder, LiteralInt(unitValue))
        else if (explicitFirst && ops.size == 1)
            builder.buildCall(namedFunction, listOf(compileEForce(builder, LiteralInt(unitValue)), ops[0]))
        else
            ops.drop(1).fold(ops[0]) { op1, op2 -> builder.buildCall(namedFunction, listOf(op1, op2)) }
    }


fun compile(builtinBindings: List<Binding<Builder, LLVMValueRef>>, context: Context, input: String): Either<List<Errors>, Module> =
    parse(Scanner(StringReader(input))) mapLeft { listOf(it) } andThen { translate(builtinBindings, it) } andThen { compile(context, "test", it) }

suspend fun parserConformanceTest(
    builtinBindings: List<Binding<Builder, LLVMValueRef>>,
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