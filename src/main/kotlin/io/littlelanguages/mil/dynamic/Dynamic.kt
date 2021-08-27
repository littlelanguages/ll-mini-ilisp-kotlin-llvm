package io.littlelanguages.mil.dynamic

import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.mil.*
import io.littlelanguages.mil.dynamic.tst.*

fun <S, T> translate(builtinBindings: List<Binding<S, T>>, p: io.littlelanguages.mil.static.ast.Program): Either<List<Errors>, Program<S, T>> =
    Translator(builtinBindings, p).apply()

private class Translator<S, T>(builtinBindings: List<Binding<S, T>>, val ast: io.littlelanguages.mil.static.ast.Program) {
    val errors =
        mutableListOf<Errors>()

    val bindings = Bindings<S, T>()

    var toplevel = true
    var depth = -1
    var offset = 0

    init {
        builtinBindings.forEach { bindings.add(it.name, it) }
    }

    fun apply(): Either<List<Errors>, Program<S, T>> {
        val r = program(ast.expressions)

        return if (errors.isEmpty()) Right(r) else Left(errors)
    }

    private fun program(es: List<io.littlelanguages.mil.static.ast.Expression>): Program<S, T> {
        val declarations = mutableListOf<Declaration<S, T>>()
        val expressions = mutableListOf<Expression<S, T>>()
        val names = mutableListOf<String>()

        es.map { expressionToTST(it) }.forEach {
            if (it is Procedure<S, T>)
                declarations.add(it)
            else
                expressions.add(it)

            if (it is AssignExpression<S, T>)
                names.add(it.symbol.name)
        }

        declarations.add(Procedure("_main", emptyList(), 0, offset, expressions))

        return Program(names, declarations)
    }

    private fun expressionToTST(e: io.littlelanguages.mil.static.ast.Expression): Expression<S, T> =
        when (e) {
            is io.littlelanguages.mil.static.ast.SExpression ->
                if (e.expressions.isEmpty())
                    LiteralUnit()
                else if (e.isConst())
                    constToTST(e)
                else {
                    val first = e.expressions[0]

                    if (first is io.littlelanguages.mil.static.ast.Symbol) {
                        val arguments = e.expressions.drop(1).map { expressionToTST(it) }

                        when (first.name) {
                            "if" ->
                                ifToTST(arguments)
                            else ->
                                when (val binding = bindings.get(first.name)) {
                                    null ->
                                        reportError(UnknownSymbolError(first.name, first.position))

                                    is DeclaredProcedureBinding ->
                                        if (binding.parameterCount == arguments.size)
                                            CallProcedureExpression(binding, arguments)
                                        else
                                            reportError(ArgumentMismatchError(first.name, binding.parameterCount, arguments.size, e.position))

                                    is ExternalProcedureBinding ->
                                        when (val error = binding.validateArguments(e, first.name, arguments)) {
                                            null ->
                                                CallProcedureExpression(binding, arguments)
                                            else ->
                                                reportError(error)
                                        }

                                    else ->
                                        CallValueExpression(SymbolReferenceExpression(binding), arguments)
                                }

                        }
                    } else
                        TODO()
                }

            is io.littlelanguages.mil.static.ast.LiteralInt ->
                LiteralInt(e.value.toInt())

            is io.littlelanguages.mil.static.ast.LiteralString ->
                translateLiteralString(e)

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

                        val oldTopLevel = toplevel
                        val oldOffset = offset

                        depth += 1
                        bindings.add(v1v0.name, DeclaredProcedureBinding(v1v0.name, parameters.size, depth))
                        toplevel = false
                        offset = parameters.size
                        bindings.open()
                        parameters.forEachIndexed { index, symbol ->
                            if (symbol is io.littlelanguages.mil.static.ast.Symbol) {
                                val parameterName = symbol.name

                                parameterNames.add(parameterName)
                                if (bindings.inCurrentNesting(parameterName))
                                    reportError(DuplicateParameterNameError(parameterName, symbol.position()))
                                bindings.add(parameterName, ParameterBinding(parameterName, depth, index))
                            } else
                                reportError(InvalidConstFormError(e.position()))
                        }
                        bindings.open()
                        val expressions = e.expressions.drop(2).map { expressionToTST(it) }
                        val procedure = Procedure(v1v0.name, parameterNames, depth, offset, expressions)
                        bindings.close()
                        bindings.close()

                        if (oldTopLevel) {
                            toplevel = true
                        }
                        depth -= 1
                        offset = oldOffset

                        procedure
                    } else
                        reportError(InvalidConstFormError(e.position()))
                } else
                    reportError(InvalidConstFormError(e.position()))
            } else if (v1 is io.littlelanguages.mil.static.ast.Symbol && e.expressions.size == 3) {
                val v2 = e.expressions[2]

                val oldToplevel = toplevel
                val binding = if (toplevel) TopLevelValueBinding<S, T>(v1.name) else ProcedureValueBinding(v1.name, depth, offset)

                if (!toplevel)
                    offset += 1

                toplevel = false
                bindings.add(v1.name, binding)
                val expression = AssignExpression(binding, expressionToTST(v2))
                toplevel = oldToplevel
                expression
            } else
                reportError(InvalidConstFormError(e.position()))
        } else
            reportError(InvalidConstFormError(e.position()))

    private fun ifToTST(es: List<Expression<S, T>>): Expression<S, T> =
        when (es.size) {
            0 -> LiteralUnit()
            1 -> es[0]
            2 -> IfExpression(es[0], es[1], LiteralUnit())
            else -> IfExpression(es[0], es[1], ifToTST(es.drop(2)))
        }

    private fun reportError(error: Errors): Expression<S, T> {
        errors.add(error)
        return LiteralUnit()
    }
}

fun <S, T> translateLiteralString(e: io.littlelanguages.mil.static.ast.LiteralString): LiteralString<S, T> {
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