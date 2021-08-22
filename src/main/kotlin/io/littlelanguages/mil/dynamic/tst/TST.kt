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

data class Procedure<S, T>(val name: String, val arguments: List<String>, val es: Expressions<S, T>) : Declaration<S, T>, Expression<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "procedure", mapOf(
                Pair("name", name),
                Pair("arguments", arguments),
                Pair("es", es.map { it.yaml() })
            )
        )
}

typealias  Expressions<S, T> = List<Expression<S, T>>

interface Expression<S, T> : Yamlable

data class AssignExpression<S, T>(val symbol: Binding<S, T>, val e: Expression<S, T>) : Expression<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "assign", mapOf(
                Pair("symbol", symbol.yaml()),
                Pair("e", e.yaml())
            )
        )
}

data class CallProcedureExpression<S, T>(val procedure: ProcedureBinding<S, T>, val es: Expressions<S, T>) : Expression<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "call-procedure", mapOf(
                Pair("procedure", procedure.yaml()),
                Pair("es", es.map { it.yaml() })
            )
        )
}

data class CallValueExpression<S, T>(val operand: Expression<S, T>, val es: Expressions<S, T>) : Expression<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "call-value", mapOf(
                Pair("operand", operand.yaml()),
                Pair("es", es.map { it.yaml() })
            )
        )
}

data class IfExpression<S, T>(val e1: Expression<S, T>, val e2: Expression<S, T>, val e3: Expression<S, T>) : Expression<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "if", mapOf(
                Pair("e1", e1.yaml()),
                Pair("e2", e2.yaml()),
                Pair("e3", e3.yaml())
            )
        )
}

data class PrintExpression<S, T>(val es: Expressions<S, T>) : Expression<S, T> {
    override fun yaml(): Any =
        singletonMap("print", es.map { it.yaml() })
}

data class PrintlnExpression<S, T>(val es: Expressions<S, T>) : Expression<S, T> {
    override fun yaml(): Any =
        singletonMap("println", es.map { it.yaml() })
}

data class SymbolReferenceExpression<S, T>(val symbol: Binding<S, T>) : Expression<S, T> {
    override fun yaml(): Any =
        symbol.yaml()
}

data class LiteralBool<S, T>(val value: Boolean) : Expression<S, T> {
    override fun yaml(): Any =
        if (value) "TRUE" else "FALSE"
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
