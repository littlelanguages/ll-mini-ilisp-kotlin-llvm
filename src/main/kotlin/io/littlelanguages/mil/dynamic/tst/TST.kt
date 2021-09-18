package io.littlelanguages.mil.dynamic.tst

import io.littlelanguages.data.Yamlable
import io.littlelanguages.mil.dynamic.Binding
import io.littlelanguages.mil.dynamic.ProcedureBinding

data class Program<S, T>(val values: List<String>, val declarations: List<Declaration<S, T>>) : Yamlable {
    override fun yaml(): Any =
        singletonMap(
            "program", mapOf(
                Pair("values", values),
                Pair("procedures", declarations.map { it.yaml() })
            )
        )
}

sealed interface Declaration<S, T> : Yamlable

data class Procedure<S, T>(val name: String, val parameters: List<String>, val depth: Int, val offsets: Int, val es: Expressions<S, T>) :
    Declaration<S, T>, Expression<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "procedure", mapOf(
                Pair("name", name),
                Pair("parameters", parameters),
                Pair("depth", depth),
                Pair("offsets", offsets),
                Pair("es", es.map { it.yaml() })
            )
        )
}

typealias  Expressions<S, T> = List<Expression<S, T>>
typealias  Expressionss<S, T> = List<List<Expression<S, T>>>

interface Expression<S, T> : Yamlable

data class AssignExpression<S, T>(val symbol: Binding<S, T>, val es: Expressions<S, T>) : Expression<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "assign", mapOf(
                Pair("symbol", symbol.yaml()),
                Pair("es", es.map { it.yaml() })
            )
        )
}

data class CallProcedureExpression<S, T>(val procedure: ProcedureBinding<S, T>, val es: List<Expressions<S, T>>, val lineNumber: Int) :
    Expression<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "call-procedure", mapOf(
                Pair("procedure", procedure.yaml()),
                Pair("es", es.map { e -> e.map { it.yaml() } }),
                Pair("line-number", lineNumber)
            )
        )
}

data class CallValueExpression<S, T>(val operand: List<Expression<S, T>>, val es: Expressions<S, T>) : Expression<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "call-value", mapOf(
                Pair("operand", operand.map { it.yaml() }),
                Pair("es", es.map { it.yaml() })
            )
        )
}

data class IfExpression<S, T>(val e1: List<Expression<S, T>>, val e2: List<Expression<S, T>>, val e3: List<Expression<S, T>>) : Expression<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "if", mapOf(
                Pair("e1", e1.map { it.yaml() }),
                Pair("e2", e2.map { it.yaml() }),
                Pair("e3", e3.map { it.yaml() })
            )
        )
}

data class SymbolReferenceExpression<S, T>(val symbol: Binding<S, T>) : Expression<S, T> {
    override fun yaml(): Any =
        symbol.yaml()
}

data class LiteralInt<S, T>(val value: Int) : Expression<S, T> {
    override fun yaml(): Any =
        value
}

data class LiteralString<S, T>(val value: String) : Expression<S, T> {
    override fun yaml(): Any =
        value
}

class LiteralUnit<S, T> : Expression<S, T> {
    override fun yaml(): Any = "()"
}
