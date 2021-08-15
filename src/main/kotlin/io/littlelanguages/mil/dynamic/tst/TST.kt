package io.littlelanguages.mil.dynamic.tst

import io.littlelanguages.data.Yamlable

data class Program(val declarations: List<Declaration>) : Yamlable {
    override fun yaml(): Any =
        singletonMap(
            "Program", declarations.map { it.yaml() }
        )
}

interface Declaration : Yamlable

data class Procedure(val name: String, val arguments: List<String>, val es: Expressions) : Declaration, Expression {
    override fun yaml(): Any =
        singletonMap(
            "Procedure", mapOf(
                Pair("name", name),
                Pair("arguments", arguments),
                Pair("es", es.map { it.yaml() })
            )
        )
}

typealias  Expressions = List<Expression>

interface Expression : Yamlable

data class BooleanPExpression(val es: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap("boolean?", es.yaml())
}

data class CallExpression(val name: String, val es: Expressions) : Expression {
    override fun yaml(): Any =
        singletonMap(
            "Call", mapOf(
                Pair("name", name),
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

data class IntegerPExpression(val es: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap("integer?", es.yaml())
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
        singletonMap("Print", es.map { it.yaml() })
}

data class PrintlnExpression(val es: Expressions) : Expression {
    override fun yaml(): Any =
        singletonMap("Println", es.map { it.yaml() })
}

data class StringPExpression(val es: Expression) : Expression {
    override fun yaml(): Any =
        singletonMap("string?", es.yaml())
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

