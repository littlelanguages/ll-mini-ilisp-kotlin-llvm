package io.littlelanguages.mil.bin

import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.compiler.CompileState
import io.littlelanguages.mil.compiler.builtinBindings
import io.littlelanguages.mil.compiler.llvm.Context
import io.littlelanguages.mil.compiler.llvm.Module
import io.littlelanguages.mil.dynamic.Binding
import io.littlelanguages.mil.dynamic.translate
import io.littlelanguages.mil.static.Scanner
import io.littlelanguages.mil.static.parse
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM
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

fun compile(input: File, output: File) {
    val context = Context()

    when (val compiledResult = compile(builtinBindings, context, input)) {
        is Left -> {
            println(compiledResult.left)
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
        // Take file and compile with defaults to a .o file.  File must have a .mlsp extension and the extension is changed to a .o
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