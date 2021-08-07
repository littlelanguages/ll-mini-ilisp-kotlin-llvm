package io.littlelanguages.p0.bin

import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.p0.Errors
import io.littlelanguages.p0.dynamic.translate
import io.littlelanguages.p0.semantic.compile
import io.littlelanguages.p0.static.Scanner
import io.littlelanguages.p0.static.TToken
import io.littlelanguages.p0.static.Token
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.StringReader
import kotlin.system.exitProcess


private val yaml =
        Yaml()


private fun run(input: Input) {
    if (!input.exists())
        showError("Source file does not exist: " + input.fileName)
    else if (!input.target.exists())
        buildRunClass(input)
    else if (input.src.lastModified() > input.target.lastModified())
        buildRunClass(input)
    else
        runClass(input)
}


private fun lexicalOnly(input: Input) {
    fun assembleTokens(la: Scanner): List<Token> {
        val result = mutableListOf<Token>()

        result += la.current()
        while (la.current().tToken != TToken.TEOS) {
            la.next()
            result += la.current()
        }

        return result
    }


    val sb =
            StringBuilder()

    assembleTokens(Scanner(StringReader(input.src.readText())))
            .forEach { sb.append("- $it\n") }

    input.lexicalYaml.writeText(sb.toString())
}


private fun astOnly(input: Input) {
    val ast =
            io.littlelanguages.p0.static.parse(Scanner(StringReader(input.src.readText())))

    when (ast) {
        is Left ->
            println(ast.left)
        is Right ->
            input.astYaml.writeText(yaml.dump(ast.right.yaml()))
    }
}


private fun tstOnly(input: Input) {
    val tst =
            io.littlelanguages.p0.static.parse(Scanner(StringReader(input.src.readText())))
                    .mapLeft { listOf(it) }
                    .andThen { translate(it) }

    when (tst) {
        is Left ->
            println(tst.left)
        is Right ->
            input.tstYaml.writeText(yaml.dump(tst.right.yaml()))
    }
}


private fun compileOnly(input: Input) {
    val src =
            input.src.readText()

    val moduleName =
            input.moduleName

    val output =
            parse(src, moduleName)

    when (output) {
        is Left -> {
            output.left.forEach {
                println(it)
            }
        }
        is Right ->
            input.target.writeBytes(output.right)
    }
}

fun parse(input: String, moduleName: String): Either<List<Errors>, ByteArray> =
        io.littlelanguages.p0.static.parse(Scanner(StringReader(input)))
                .mapLeft { listOf(it) }
                .andThen { translate(it) }
                .andThen { compile(it, moduleName) }


private fun runClass(input: Input) {
    val moduleName =
            input.moduleName

    val content =
            input.target.readBytes()

    val myClassLoader =
            MyClassLoader()

    myClassLoader.defineClass(moduleName, content)
    myClassLoader.loadClass(moduleName).getMethod("main", String::class.java.arrayType()).invoke(null, null)
}


private fun buildRunClass(input: Input) {
    val src =
            input.src.readText()

    val moduleName =
            input.moduleName

    val output =
            parse(src, moduleName)

    when (output) {
        is Left -> {
            output.left.forEach {
                println(it)
            }
        }
        is Right -> {
            val myClassLoader =
                    MyClassLoader()

            input.target.writeBytes(output.right)

            myClassLoader.defineClass(moduleName, output.right)
            myClassLoader.loadClass(moduleName).getMethod("main", String::class.java.arrayType()).invoke(null, null)
        }
    }
}


private fun showError(error: String) {
    println("Error: $error")
    exitProcess(1)
}


private fun showOptions(error: String? = null) {
    if (error != null) {
        println("Error: $error")
    }
    println("Options: --help | run file | compile file [--phase (lexical | ast | tst | compile)]")
    println("  --help")
    println("    Show all help options.")
    println("")
    println("  run file")
    println("    Run the passed file.  If the content file[.p0] is more recent that file then the P0")
    println("    is first compiled before the binary version is executed.")
    println("")
    println("  compile file")
    println("    Compiles the passed file to the associated phase.  If no phase is supplied")
    println("    then the phase defaults to compile.  Note that this option is a force compile.")
    println("  --phase (lexical | ast | tst | compile | all)")
    println("      lexical: produces YAML content of the tokenized source file.")
    println("      ast: produces YAML content of the abstract syntax tree.")
    println("      tst: produces YAML content of the typed abstract syntax tree.")
    println("      compile: produces class file of the program.")
    println("      all: produces all of the intermediary results.")
}


fun main(args: Array<String>) {
    if (args.isEmpty())
        showOptions("No arguments supplied")
    else if (args.contentEquals(arrayOf("--help")))
        showOptions()
    else if (args.size == 2 && args[0] == "run")
        run(Input(args[1]))
    else if (args.size == 2 && args[0] == "compile")
        compileOnly(Input(args[1]))
    else if (args.size == 4 && args[0] == "compile" && args[2] == "--phase")
        when (args[3]) {
            "lexical" -> lexicalOnly(Input(args[1]))
            "ast" -> astOnly(Input(args[1]))
            "tst" -> tstOnly(Input(args[1]))
            "compile" -> compileOnly(Input(args[1]))
            "all" -> {
                val input =
                        Input(args[1])

                lexicalOnly(input)
                astOnly(input)
                tstOnly(input)
                compileOnly(input)
            }
            else ->
                showOptions("Invalid arguments: " + args.joinToString(" "))
        }
    else
        showOptions("Invalid arguments: " + args.joinToString(" "))
}


private class Input(val fileName: String) {
    val fileNameWithoutExtension =
            if (fileName.endsWith(".p0")) fileName.dropLast(3) else fileName

    val src =
            File("$fileNameWithoutExtension.p0")

    fun exists(): Boolean =
            src.exists()

    val target: File
        get() = File("$fileNameWithoutExtension.class")

    val lexicalYaml: File
        get() = File("$fileNameWithoutExtension.lexical.yaml")

    val astYaml: File
        get() = File("$fileNameWithoutExtension.ast.yaml")

    val tstYaml: File
        get() = File("$fileNameWithoutExtension.tst.yaml")

    val moduleName: String
        get() = src.nameWithoutExtension
}