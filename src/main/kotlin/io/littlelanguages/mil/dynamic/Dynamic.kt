package io.littlelanguages.mil.dynamic

import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.mil.*
import io.littlelanguages.mil.dynamic.tst.*
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.LocationCoordinate
import io.littlelanguages.scanpiler.LocationRange
import java.lang.Integer.max

fun <S, T> translate(builtinBindings: List<Binding<S, T>>, p: io.littlelanguages.mil.static.ast.Program): Either<List<Errors>, Program<S, T>> =
    Translator(builtinBindings, p).apply()

private class Translator<S, T>(builtinBindings: List<Binding<S, T>>, val ast: io.littlelanguages.mil.static.ast.Program) {
    val errors =
        mutableListOf<Errors>()

    var nameGenerator = 0

    val bindings = Bindings<S, T>()

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

        es.flatMap { expressionToTST(it, true) }.forEach {
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

    private fun expressionToTST(e: io.littlelanguages.mil.static.ast.Expression, toplevelValue: Boolean = false): List<Expression<S, T>> =
        when (e) {
            is io.littlelanguages.mil.static.ast.ConstProcedure ->
                listOf(procedureToTST(e.symbol.name, e.parameters, e.expressions).first)

            is io.littlelanguages.mil.static.ast.ConstValue -> {
                val name = e.symbol.name
                val expressions = e.expressions

                if (bindings.inCurrentNesting(name))
                    reportError(DuplicateNameError(name, e.symbol.position))
                else {
                    val binding = when {
                        toplevelValue && isToplevel() -> TopLevelValueBinding<S, T>(name)
                        else -> ProcedureValueBinding(name, max(depth, 0), offset++)
                    }

                    bindings.add(name, binding)
                    val rhs = expressionsToTST(expressions)

                    listOf(AssignExpression(binding, rhs))
                }
            }

            is io.littlelanguages.mil.static.ast.IfExpression ->
                ifToTST(e.expressions.map { expressionsToTST(it) })

            is io.littlelanguages.mil.static.ast.ProcExpression -> {
                val tst = procedureToTST(nextName(), e.parameters, e.expressions)

                listOf(tst.first, SymbolReferenceExpression(tst.second, lineNumber(e.position)))
            }

            is io.littlelanguages.mil.static.ast.SExpression -> {
                val first = e.expressions[0]

                if (first is io.littlelanguages.mil.static.ast.Symbol) {
                    val arguments = e.expressions.drop(1).map { expressionToTST(it) }

                    when (val binding = bindings.get(first.name)) {
                        null ->
                            reportError(UnknownSymbolError(first.name, first.position))

                        is DeclaredProcedureBinding ->
                            if (binding.parameterCount == arguments.size)
                                listOf(CallProcedureExpression(binding, arguments, lineNumber(e.position)))
                            else
                                reportError(ArgumentMismatchError(first.name, binding.parameterCount, arguments.size, e.position))

                        is ExternalProcedureBinding ->
                            when (val error = binding.validateArguments(e, first.name, arguments)) {
                                null ->
                                    listOf(CallProcedureExpression(binding, arguments, lineNumber(e.position)))

                                else ->
                                    reportError(error)
                            }

                        else ->
                            listOf(
                                CallValueExpression(
                                    listOf(SymbolReferenceExpression(binding, lineNumber(e.position))),
                                    arguments.flatten(),
                                    lineNumber(e.position)
                                )
                            )
                    }
                } else
                    listOf(CallValueExpression(expressionToTST(e.expressions[0]), expressionsToTST(e.expressions.drop(1)), lineNumber(e.position)))
            }

            is io.littlelanguages.mil.static.ast.LiteralInt ->
                listOf(LiteralInt(e.value.toInt()))

            is io.littlelanguages.mil.static.ast.LiteralString ->
                listOf(translateLiteralString(e))

            is io.littlelanguages.mil.static.ast.LiteralUnit ->
                listOf(LiteralUnit())

            is io.littlelanguages.mil.static.ast.Symbol -> {
                val binding = bindings.get(e.name)

                if (binding == null)
                    reportError(UnknownSymbolError(e.name, e.position))
                else
                    listOf(SymbolReferenceExpression(binding, lineNumber(e.position)))
            }
        }

    private fun expressionsToTST(es: List<io.littlelanguages.mil.static.ast.Expression>): List<Expression<S, T>> {
        bindings.open()
        val result = es.flatMap { expressionToTST(it) }
        bindings.close()

        return result
    }

    private fun procedureToTST(
        name: String,
        parameters: List<io.littlelanguages.mil.static.ast.Symbol>,
        expressions: List<io.littlelanguages.mil.static.ast.Expression>
    ): Pair<Procedure<S, T>, DeclaredProcedureBinding<S, T>> {
        val parameterNames = mutableListOf<String>()

        val oldOffset = offset

        depth += 1
        val binding = DeclaredProcedureBinding<S, T>(name, parameters.size, depth)
        bindings.add(name, binding)
        offset = parameters.size
        bindings.open()
        parameters.forEachIndexed { index, symbol ->
            val parameterName = symbol.name

            parameterNames.add(parameterName)
            if (bindings.inCurrentNesting(parameterName))
                reportError(DuplicateParameterNameError(parameterName, symbol.position()))
            bindings.add(parameterName, ParameterBinding(parameterName, depth, index))
        }
        bindings.open()
        val es = expressionsToTST(expressions)
        val procedure = Procedure(name, parameterNames, depth, offset, es)
        bindings.close()
        bindings.close()

        depth -= 1
        offset = oldOffset

        return Pair(procedure, binding)
    }

    private fun ifToTST(es: List<List<Expression<S, T>>>): List<Expression<S, T>> =
        when (es.size) {
            0 -> listOf(LiteralUnit())
            1 -> es[0]
            2 -> listOf(IfExpression(es[0], es[1], listOf(LiteralUnit())))
            else -> listOf(IfExpression(es[0], es[1], ifToTST(es.drop(2))))
        }

    private fun reportError(error: Errors): List<Expression<S, T>> {
        errors.add(error)
        return listOf()
    }

    private fun isToplevel(): Boolean =
        depth == -1

    private fun nextName() =
        "__n${nameGenerator++}"
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

private fun lineNumber(p: Location): Int =
    when (p) {
        is LocationCoordinate -> p.line
        is LocationRange -> p.start.line
    }