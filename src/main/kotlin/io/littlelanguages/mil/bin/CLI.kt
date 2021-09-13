package io.littlelanguages.mil.bin

import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.mil.*
import io.littlelanguages.mil.compiler.CompileState
import io.littlelanguages.mil.compiler.builtinBindings
import io.littlelanguages.mil.compiler.llvm.Context
import io.littlelanguages.mil.compiler.llvm.Module
import io.littlelanguages.mil.dynamic.Binding
import io.littlelanguages.mil.dynamic.translate
import io.littlelanguages.mil.static.Scanner
import io.littlelanguages.mil.static.TToken
import io.littlelanguages.mil.static.Token
import io.littlelanguages.mil.static.parse
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.LocationCoordinate
import io.littlelanguages.scanpiler.LocationRange
import org.bytedeco.llvm.LLVM.LLVMValueRef
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File
import java.io.FileReader
import java.util.concurrent.Callable
import kotlin.system.exitProcess

fun compile(builtinBindings: List<Binding<CompileState, LLVMValueRef>>, context: Context, input: File): Either<List<Errors>, Module> {
    val reader = FileReader(input)

    val result = parse(Scanner(reader)) mapLeft { listOf(it) } andThen { translate(builtinBindings, it) } andThen {
        io.littlelanguages.mil.compiler.compile(
            context,
            input.name,
            it
        )
    }
    reader.close()

    return result
}

fun changeExtension(f: File, newExtension: String): File {
    val i = f.name.lastIndexOf('.')
    val name = f.name.substring(0, i)
    return File(f.parent, name + newExtension)
}

fun reportErrors(errors: List<Errors>) {
    errors.forEach { println(formatError(it)) }
}

fun formatTToken(token: TToken): String =
    when (token) {
        TToken.TProc -> "'proc'"
        TToken.TConst -> "'const'"
        TToken.TDo -> "'do'"
        TToken.TIf -> "'if'"
        TToken.TRParen -> "')'"
        TToken.TLParen -> "'('"
        TToken.TLiteralInt -> "Literal Int"
        TToken.TLiteralString -> "Literal String"
        TToken.TSymbol -> "Symbol"
        TToken.TEOS -> "End Of Stream"
        TToken.TERROR -> "<Error Token>"
    }

fun formatToken(token: Token): String =
    when (token.tToken) {
        TToken.TLiteralInt,
        TToken.TLiteralString,
        TToken.TSymbol -> "${formatTToken(token.tToken)} (${token.lexeme})"
        else -> formatTToken(token.tToken)
    }

fun formatLocation(location: Location): String =
    when (location) {
        is LocationCoordinate -> "(${location.line}, ${location.column})"
        is LocationRange -> "(${location.start.line}, ${location.start.column})-(${location.end.line}, ${location.end.column})"
    }

fun formatError(error: Errors): String =
    when (error) {
        is ArgumentMismatchError ->
            "Argument Mismatch: ${formatLocation(error.location)}: Procedure \"${error.name}\" expects ${error.expected} ${if (error.expected == 1) "argument" else "arguments"} but was passed ${error.actual}"

        is CompilationError ->
            "Compilation Error: ${error.message}"

        is DuplicateNameError ->
            "Duplicate Name: ${formatLocation(error.location)}: Attempt to redefine \"${error.name}\""

        is DuplicateParameterNameError ->
            "Duplicate Parameter Name: ${formatLocation(error.location)}: Attempt to redefine \"${error.name}\""

        is ParseError -> {
            val oneOfPhrase = if (error.expected.size == 1) "" else "one of "
            val expectedTokens = error.expected.joinToString { formatTToken(it) }
            val actualToken = formatToken(error.found)

            "Parse Error: ${formatLocation(error.found.location)}: Expected $oneOfPhrase$expectedTokens but found $actualToken"
        }

        is UnknownSymbolError ->
            "Unknown Symbol: ${formatLocation(error.location)}: Reference to unknown symbol \"${error.name}\""
    }

fun compile(input: File, output: File) {
    val context = Context()

    when (val compiledResult = compile(builtinBindings, context, input)) {
        is Left -> {
            reportErrors(compiledResult.left)
            exitProcess(1)
        }
        is Right ->
            compiledResult.right.writeBitcodeToFile(output.absolutePath)
    }
    context.dispose()
}

@Command(name = "ll-mini-ilisp-kotlin-llvm", version = ["0.1"], mixinStandardHelpOptions = true, description = ["A mini iLisp compiler."])
class CLI : Callable<Int> {
    @Parameters(paramLabel = "FILE", description = ["File to compile.  File must exist and have a .mlsp extension"], arity = "1")
    private lateinit var file: File

    private fun failOnError(error: String) {
        println("Error: $error")
        exitProcess(1)
    }

    override fun call(): Int {
        // Take file and compile with defaults to a .bc file.  File must have a .mlsp extension and the extension is changed to a .o
        if (!file.canRead())
            failOnError("Invalid input file: $file is not readable")
        if (file.extension != "mlsp")
            failOnError("Invalid input file: $file requires a .mlsp extension")

        compile(file, changeExtension(file, ".bc"))

        return 0
    }
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(CLI()).execute(*args))
}