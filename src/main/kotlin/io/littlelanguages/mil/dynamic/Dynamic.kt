package io.littlelanguages.mil.dynamic

import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.mil.*
import io.littlelanguages.mil.dynamic.tst.*

fun translate(p: io.littlelanguages.mil.static.ast.Program): Either<List<Errors>, Program> =
    Translator(p).apply()

private class Translator(val ast: io.littlelanguages.mil.static.ast.Program) {
    val errors =
        mutableListOf<Errors>()

    val bindings = Bindings()

    fun apply(): Either<List<Errors>, Program> {
        val r = program(ast.expressions)

        return if (errors.isEmpty()) Right(r) else Left(errors)
    }

    private fun program(es: List<io.littlelanguages.mil.static.ast.Expression>): Program {
        val declarations = mutableListOf<Declaration>()
        val expressions = mutableListOf<Expression>()
        val names = mutableListOf<String>()

        for (e in es) {
            val ep = expressionToTST(e)

            if (ep is Declaration)
                declarations.add(ep)
            else
                expressions.add(ep)

            if (ep is AssignExpression)
                names.add(ep.symbol.name)
        }

        declarations.add(Procedure("_main", emptyList(), expressions))

        return Program(names, declarations)
    }

    private fun expressionToTST(e: io.littlelanguages.mil.static.ast.Expression): Expression =
        when (e) {
            is io.littlelanguages.mil.static.ast.SExpression ->
                if (e.expressions.isEmpty())
                    LiteralUnit
                else if (e.isConst())
                    constToTST(e)
                else {
                    val first = e.expressions[0]

                    if (first is io.littlelanguages.mil.static.ast.Symbol) {
                        val arguments = e.expressions.drop(1).map { expressionToTST(it) }

                        when (first.name) {
                            "boolean?" ->
                                if (arguments.size == 1)
                                    BooleanPExpression(arguments[0])
                                else
                                    reportError(ArgumentMismatchError(first.name, 1, arguments.size, e.position()))
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
                            "=" ->
                                if (arguments.size == 2)
                                    EqualsExpression(arguments[0], arguments[1])
                                else
                                    reportError(ArgumentMismatchError(first.name, 2, arguments.size, e.position()))
                            "if" ->
                                ifToTST(arguments)
                            "integer?" ->
                                if (arguments.size == 1)
                                    IntegerPExpression(arguments[0])
                                else
                                    reportError(ArgumentMismatchError(first.name, 1, arguments.size, e.position()))
                            "<" ->
                                if (arguments.size == 2)
                                    LessThanExpression(arguments[0], arguments[1])
                                else
                                    reportError(ArgumentMismatchError(first.name, 2, arguments.size, e.position()))
                            "-" ->
                                MinusExpression(arguments)
                            "null?" ->
                                if (arguments.size == 1)
                                    NullPExpression(arguments[0])
                                else
                                    reportError(ArgumentMismatchError(first.name, 1, arguments.size, e.position()))
                            "pair" ->
                                if (arguments.size == 2)
                                    PairExpression(arguments[0], arguments[1])
                                else
                                    reportError(ArgumentMismatchError(first.name, 2, arguments.size, e.position()))
                            "pair?" ->
                                if (arguments.size == 1)
                                    PairPExpression(arguments[0])
                                else
                                    reportError(ArgumentMismatchError(first.name, 1, arguments.size, e.position()))
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
                            "string?" ->
                                if (arguments.size == 1)
                                    StringPExpression(arguments[0])
                                else
                                    reportError(ArgumentMismatchError(first.name, 1, arguments.size, e.position()))
                            else -> {
                                val binding = bindings.get(first.name)

                                if (binding == null)
                                    reportError(UnknownSymbolError(first.name, first.position))
                                else if (binding is TopLevelProcedureBinding)
                                    if (binding.parameterCount == arguments.size)
                                        CallProcedureExpression(binding, arguments)
                                    else
                                        reportError(ArgumentMismatchError(first.name, binding.parameterCount, arguments.size, e.position))
                                else
                                    CallValueExpression(SymbolReferenceExpression(binding), arguments)
                            }
                        }
                    } else
                        TODO()
                }
            is io.littlelanguages.mil.static.ast.LiteralBool -> if (e.value) LiteralBool.TRUE else LiteralBool.FALSE
            is io.littlelanguages.mil.static.ast.LiteralInt -> LiteralInt(e.value.toInt())
            is io.littlelanguages.mil.static.ast.LiteralString -> translateLiteralString(e)
            is io.littlelanguages.mil.static.ast.Symbol -> {
                val binding = bindings.get(e.name)

                if (binding == null)
                    reportError(UnknownSymbolError(e.name, e.position))
                else
                    SymbolReferenceExpression(binding)
            }
        }

    private fun constToTST(e: io.littlelanguages.mil.static.ast.SExpression) =
        if (e.expressions.size > 1) {
            val v1 = e.expressions[1]

            if (v1 is io.littlelanguages.mil.static.ast.SExpression && v1.expressions.isNotEmpty()) {
                val v1v0 = v1.expressions[0]

                if (v1v0 is io.littlelanguages.mil.static.ast.Symbol) {
                    val parameters = v1.expressions.drop(1)

                    if (parameters.all { it is io.littlelanguages.mil.static.ast.Symbol }) {
                        val parameterNames = mutableListOf<String>()

                        bindings.add(v1v0.name, TopLevelProcedureBinding(v1v0.name, parameters.size))
                        bindings.open()
                        parameters.forEachIndexed { index, symbol ->
                            if (symbol is io.littlelanguages.mil.static.ast.Symbol) {
                                val parameterName = symbol.name

                                parameterNames.add(parameterName)
                                if (bindings.inCurrentScope(parameterName))
                                    reportError(DuplicateParameterNameError(parameterName, symbol.position()))
                                bindings.add(parameterName, ParameterBinding(parameterName, index))
                            } else
                                reportError(InvalidConstFormError(e.position()))
                        }
                        bindings.open()
                        val procedure = Procedure(v1v0.name, parameterNames, e.expressions.drop(2).map { expressionToTST(it) })
                        bindings.close()
                        bindings.close()

                        procedure
                    } else
                        reportError(InvalidConstFormError(e.position()))
                } else
                    reportError(InvalidConstFormError(e.position()))
            } else if (v1 is io.littlelanguages.mil.static.ast.Symbol && e.expressions.size == 3) {
                val v2 = e.expressions[2]

                val binding = TopLevelValueBinding(v1.name)
                bindings.add(v1.name, binding)
                AssignExpression(binding, expressionToTST(v2))
            } else
                reportError(InvalidConstFormError(e.position()))
        } else
            reportError(InvalidConstFormError(e.position()))

    private fun ifToTST(es: List<Expression>): Expression =
        when (es.size) {
            0 -> LiteralUnit
            1 -> es[0]
            2 -> IfExpression(es[0], es[1], LiteralUnit)
            else -> IfExpression(es[0], es[1], ifToTST(es.drop(2)))
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

private fun io.littlelanguages.mil.static.ast.SExpression.isConst() =
    if (this.expressions.isEmpty())
        false
    else {
        val e1 = this.expressions[0]

        e1 is io.littlelanguages.mil.static.ast.Symbol && e1.name == "const"
    }