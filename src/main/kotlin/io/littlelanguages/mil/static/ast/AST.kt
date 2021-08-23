package io.littlelanguages.mil.static.ast

import io.littlelanguages.data.Yamlable
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.Locationable


data class Program(
    val expressions: List<Expression>
) : Yamlable {
    override fun yaml(): Any =
        expressions.map { it.yaml() }
}

sealed class Expression(open val position: Location) : Yamlable, Locationable {
    override fun position(): Location = position
}

data class Symbol(
    override val position: Location,
    val name: String
) : Expression(position) {
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
    override val position: Location,
    val expressions: List<Expression>
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "SExpression", expressions.map { it.yaml() }
        )
}

data class LiteralInt(
    override val position: Location,
    val value: String
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "LiteralInt", mapOf(
                Pair("value", value),
                Pair("position", position.yaml())
            )
        )
}

data class LiteralString(
    override val position: Location,
    val value: String
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "LiteralString", mapOf(
                Pair("value", value),
                Pair("position", position.yaml())
            )
        )
}
