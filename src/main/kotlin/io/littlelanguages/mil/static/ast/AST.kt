package io.littlelanguages.mil.static.ast

import io.littlelanguages.data.Yamlable
import io.littlelanguages.scanpiler.Location


data class Program(
    val expressions: List<Expression>
) : Yamlable {
    override fun yaml(): Any =
        expressions.map { it.yaml() }
}

sealed class Expression : Yamlable

data class Symbol(
    val position: Location,
    val name: String
) : Expression() {
    override fun yaml(): Any =
        singletonMap(
            "Symbol",
            mapOf(
                Pair("value", name),
                Pair("position", position.yaml())
            )
        )
}

data class SExpression(
    val expressions: List<Expression>
) : Expression() {
    override fun yaml(): Any =
        singletonMap(
            "SExpression", expressions.map { it.yaml() }
        )
}

data class LiteralBool(
    val position: Location,
    val value: Boolean
) : Expression() {
    override fun yaml(): Any =
        singletonMap(
            "LiteralBool", mapOf(
                Pair("value", if (value) "true" else "false"),
                Pair("position", position.yaml())
            )
        )
}

data class LiteralInt(
    val position: Location,
    val value: String
) : Expression() {
    override fun yaml(): Any =
        singletonMap(
            "LiteralInt", mapOf(
                Pair("value", value),
                Pair("position", position.yaml())
            )
        )
}

data class LiteralString(
    val position: Location,
    val value: String
) : Expression() {
    override fun yaml(): Any =
        singletonMap(
            "LiteralString", mapOf(
                Pair("value", value),
                Pair("position", position.yaml())
            )
        )
}
