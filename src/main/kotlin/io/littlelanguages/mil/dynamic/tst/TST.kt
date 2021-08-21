package io.littlelanguages.mil.dynamic.tst

import io.littlelanguages.data.Yamlable
import io.littlelanguages.mil.dynamic.Binding
import io.littlelanguages.mil.dynamic.TopLevelProcedureBinding

data class Program(val values: List<String>, val declarations: List<Declaration>) : Yamlable {
    override fun yaml(): Any =
        singletonMap(
            "program", mapOf(
                Pair("values", values),
                Pair("procedures", declarations.map { it.yaml() })
            )
        )
}

sealed interface Declaration : Yamlable

data class Procedure(val name: String, val arguments: List<String>, val es: Expressions) : Declaration, Expression {
    override fun yaml(): Any =
        singletonMap(
            "procedure", mapOf(
                Pair("name", name),
                Pair("arguments", arguments),
                Pair("es", es.map { it.yaml() })
            )
        )
}

typealias  Expressions = List<Expression>

interface Expression : Yamlable

data class AssignExpression(val symbol: Binding, val e: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap(
            "assign", mapOf(
                Pair("symbol", symbol.yaml()),
                Pair("e", e.yaml())
            )
        )
}

data class BooleanPExpression(val es: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap("boolean?", es.yaml())
}

data class CallProcedureExpression(val procedure: TopLevelProcedureBinding, val es: Expressions) : Expression {
    override fun yaml(): Any =
        singletonMap(
            "call-procedure", mapOf(
                Pair("procedure", procedure.yaml()),
                Pair("es", es.map { it.yaml() })
            )
        )
}

data class CallValueExpression(val operand: Expression, val es: Expressions) : Expression {
    override fun yaml(): Any =
        singletonMap(
            "call-value", mapOf(
                Pair("operand", operand.yaml()),
                Pair("es", es.map { it.yaml() })
            )
        )
}

data class CarExpression(val es: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap("car", es.yaml())
}

data class CdrExpression(val es: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap("cdr", es.yaml())
}

data class EqualsExpression(val e1: Expression, val e2: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap(
            "=", mapOf(
                Pair("e1", e1.yaml()),
                Pair("e2", e2.yaml())
            )
        )
}

data class IfExpression(val e1: Expression, val e2: Expression, val e3: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap(
            "if", mapOf(
                Pair("e1", e1.yaml()),
                Pair("e2", e2.yaml()),
                Pair("e3", e3.yaml())
            )
        )
}

data class IntegerPExpression(val es: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap("integer?", es.yaml())
}

data class LessThanExpression(val e1: Expression, val e2: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap(
            "<", mapOf(
                Pair("e1", e1.yaml()),
                Pair("e2", e2.yaml())
            )
        )
}

data class MinusExpression(val es: Expressions) : Expression {
    override fun yaml(): Any =
        singletonMap("-", es.map { it.yaml() })
}

data class NullPExpression(val es: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap("null?", es.yaml())
}

data class PairPExpression(val es: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap("pair?", es.yaml())
}

data class PlusExpression(val es: Expressions) : Expression {
    override fun yaml(): Any =
        singletonMap("+", es.map { it.yaml() })
}

data class PrintExpression(val es: Expressions) : Expression {
    override fun yaml(): Any =
        singletonMap("print", es.map { it.yaml() })
}

data class PrintlnExpression(val es: Expressions) : Expression {
    override fun yaml(): Any =
        singletonMap("println", es.map { it.yaml() })
}

data class StringPExpression(val es: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap("string?", es.yaml())
}

data class SymbolReferenceExpression(val symbol: Binding) : Expression {
    override fun yaml(): Any =
        symbol.yaml()
}

enum class LiteralBool : Expression {
    TRUE {
        override fun yaml(): Any =
            "TRUE"
    },
    FALSE {
        override fun yaml(): Any =
            "FALSE"
    }
}

data class LiteralInt(val value: Int) : Expression {
    override fun yaml(): Any =
        value
}

data class LiteralString(val value: String) : Expression {
    override fun yaml(): Any =
        value
}

object LiteralUnit : Expression {
    override fun yaml(): Any = "()"
}

data class PairExpression(val car: Expression, val cdr: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap(
            "pair", mapOf(
                Pair("car", car.yaml()),
                Pair("cdr", cdr.yaml())
            )
        )
}

data class SlashExpression(val es: Expressions) : Expression {
    override fun yaml(): Any =
        singletonMap("/", es.map { it.yaml() })
}

data class StarExpression(val es: Expressions) : Expression {
    override fun yaml(): Any =
        singletonMap("*", es.map { it.yaml() })
}

