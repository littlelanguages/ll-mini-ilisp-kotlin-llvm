package io.littlelanguages.mil.dynamic

import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.mil.ArgumentMismatchError
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.dynamic.tst.*

fun translate(p: io.littlelanguages.mil.static.ast.Program): Either<List<Errors>, Program> =
    Translator(p).apply()

private class Translator(val ast: io.littlelanguages.mil.static.ast.Program) {
    val errors =
        mutableListOf<Errors>()

    fun apply(): Either<List<Errors>, Program> {
        val r = program(ast.expressions)

        return if (errors.isEmpty()) Right(r) else Left(errors)
    }

    private fun program(es: List<io.littlelanguages.mil.static.ast.Expression>): Program {
        val declarations = mutableListOf<Declaration>()
        val expressions = mutableListOf<Expression>()

        for (e in es) {
            val ep = expressionToTST(e)

            if (ep is Declaration)
                declarations.add(ep)
            else
                expressions.add(ep)
        }

        declarations.add(Procedure("_main", emptyList(), expressions))

        return Program(declarations)
    }

    private fun expressionToTST(e: io.littlelanguages.mil.static.ast.Expression): Expression =
        when (e) {
            is io.littlelanguages.mil.static.ast.SExpression -> {
                if (e.expressions.isEmpty())
                    LiteralUnit
                else {
                    val first = e.expressions[0]

                    if (first is io.littlelanguages.mil.static.ast.Symbol) {
                        val arguments = e.expressions.drop(1).map { expressionToTST(it) }

                        when (first.name) {
                            "car" ->
                                if (arguments.size == 1)
                                    CarExpression(arguments[0])
                                else
                                    reportError(ArgumentMismatchError(first.name, 1, arguments.size, e.position()))
                            "cdr" ->
                                if (arguments.size == 1)
                                    CdrExpression(arguments[0])
                                else
                                    reportError(ArgumentMismatchError(first.name, 1, arguments.size, e.position()))
                            "-" ->
                                MinusExpression(arguments)
                            "pair" ->
                                if (arguments.size == 2)
                                    PairExpression(arguments[0], arguments[1])
                                else
                                    reportError(ArgumentMismatchError(first.name, 2, arguments.size, e.position()))
                            "+" ->
                                PlusExpression(arguments)
                            "print" ->
                                PrintExpression(arguments)
                            "println" ->
                                PrintlnExpression(arguments)
                            "/" ->
                                SlashExpression(arguments)
                            "*" ->
                                StarExpression(arguments)
                            else ->
                                CallExpression(first.name, arguments)
                        }
                    } else
                        TODO()
                }
            }
            is io.littlelanguages.mil.static.ast.LiteralBool -> if (e.value) LiteralBool.TRUE else LiteralBool.FALSE
            is io.littlelanguages.mil.static.ast.LiteralInt -> LiteralInt(e.value.toInt())
            is io.littlelanguages.mil.static.ast.LiteralString -> translateLiteralString(e)
            is io.littlelanguages.mil.static.ast.Symbol -> TODO()
        }

    private fun reportError(error: Errors): Expression {
        errors.add(error)
        return LiteralUnit
    }
}

fun translateLiteralString(e: io.littlelanguages.mil.static.ast.LiteralString): LiteralString {
    val sb = StringBuilder()
    val eValue = e.value
    val eLength = eValue.length
    var lp = 1

    while (true) {
        when {
            lp >= eLength || eValue[lp] == '"' ->
                return LiteralString(sb.toString())

            eValue[lp] == '\\' -> {
                when (val c = eValue[lp + 1]) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'x' -> {
                        lp += 2
                        var elp = lp
                        while (elp < eLength && eValue[elp].isHexDigit()) {
                            elp += 1
                        }
                        sb.append(eValue.subSequence(lp, elp).toString().toInt(16).toChar())
                        lp = elp - 2
                    }
                    else -> sb.append(c)
                }
                lp += 2
            }

            else -> {
                sb.append(eValue[lp])
                lp += 1
            }
        }
    }
}

private fun Char.isHexDigit(): Boolean =
    this.isDigit() || this.uppercaseChar() in 'A'..'F'
