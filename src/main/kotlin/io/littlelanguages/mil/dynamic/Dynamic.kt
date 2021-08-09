package io.littlelanguages.mil.dynamic

import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
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
                        when (first.name) {
                            "println" ->
                                PrintlnExpression(e.expressions.drop(1).map { expressionToTST(it) })
                            else ->
                                TODO()
                        }
                    } else
                        TODO()
                }
            }
            is io.littlelanguages.mil.static.ast.LiteralBool -> if (e.value) LiteralBool.TRUE else LiteralBool.FALSE
            is io.littlelanguages.mil.static.ast.LiteralInt -> TODO()
            is io.littlelanguages.mil.static.ast.LiteralString -> LiteralString(e.value)
            is io.littlelanguages.mil.static.ast.Symbol -> TODO()
        }

    private fun reportError(error: Errors) {
        errors.add(error)
    }
}