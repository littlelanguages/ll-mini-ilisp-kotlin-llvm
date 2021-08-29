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

typealias Expressions = List<Expression>

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

data class ConstValue(
    override val position: Location,
    val symbol: Symbol,
    val expressions: Expressions
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "ConstValue",
            mapOf(
                Pair("symbol", symbol.yaml()),
                Pair("expressions", expressions.map { it.yaml() }),
                Pair("position", position.yaml())
            )
        )
}

data class ConstProcedure(
    override val position: Location,
    val symbol: Symbol,
    val parameters: List<Symbol>,
    val expressions: List<Expression>
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "ConstProcedure",
            mapOf(
                Pair("symbol", symbol.yaml()),
                Pair("parameters", parameters.map { it.yaml() }),
                Pair("expressions", expressions.map { it.yaml() }),
                Pair("position", position.yaml())
            )
        )
}

data class IfExpression(
    override val position: Location,
    val expressions: List<List<Expression>>
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "If",
            mapOf(
                Pair("expressions", expressions.map { es -> es.map { it.yaml() } }),
                Pair("position", position.yaml())
            )
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

data class LiteralUnit(
    override val position: Location,
) : Expression(position) {
    override fun yaml(): Any =
        singletonMap(
            "LiteralUnit", mapOf(
                Pair("position", position.yaml())
            )
        )
}