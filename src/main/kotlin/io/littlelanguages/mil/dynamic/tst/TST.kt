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

data class CallExpression(val name: String, val es: Expressions) : Expression {
    override fun yaml(): Any =
        singletonMap(
            "Call", mapOf(
                Pair("name", name),
                Pair("es", es.map { it.yaml() })
            )
        )
}

data class PrintExpression(val es: Expressions) : Expression {
    override fun yaml(): Any =
        singletonMap("Print", es.map { it.yaml() })
}

data class PrintlnExpression(val es: Expressions) : Expression {
    override fun yaml(): Any =
        singletonMap("Println", es.map { it.yaml() })
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

data class LiteralString(val value: String) : Expression {
    override fun yaml(): Any =
        value
}

object LiteralUnit : Expression {
    override fun yaml(): Any = "()"
}