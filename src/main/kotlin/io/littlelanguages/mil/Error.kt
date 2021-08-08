package io.littlelanguages.mil

import io.littlelanguages.data.Yamlable
import io.littlelanguages.mil.static.TToken
import io.littlelanguages.mil.static.Token

sealed class Errors : Yamlable

data class ParseError(
        val found: Token,
        val expected: Set<TToken>) : Errors() {
    override fun yaml(): Any =
            singletonMap("ParseError", mapOf(
                    Pair("found", found),
                    Pair("expected", expected)
            ))
}

