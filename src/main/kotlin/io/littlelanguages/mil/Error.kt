package io.littlelanguages.mil

import io.littlelanguages.data.Yamlable
import io.littlelanguages.mil.static.TToken
import io.littlelanguages.mil.static.Token
import io.littlelanguages.scanpiler.Location

sealed interface Errors : Yamlable

data class ParseError(
    val found: Token,
    val expected: Set<TToken>
) : Errors {
    override fun yaml(): Any =
        singletonMap(
            "ParseError", mapOf(
                Pair("found", found),
                Pair("expected", expected)
            )
        )
}

data class CompilationError(override val message: String) : Exception(message), Errors {
    override fun yaml(): Any =
        singletonMap("CompilationError", message)
}

data class ArgumentMismatchError(val name: String, val expected: Int, val actual: Int, val location: Location) : Errors {
    override fun yaml(): Any =
        singletonMap(
            "ArgumentMismatchError", mapOf(
                Pair("name", name),
                Pair("expected", expected),
                Pair("actual", actual),
                Pair("location", location)
            )
        )
}

data class DuplicateParameterNameError(val name: String, val location: Location) : Errors {
    override fun yaml(): Any =
        singletonMap(
            "DuplicateParameterNameError", mapOf(
                Pair("name", name),
                Pair("location", location)
            )
        )
}

data class DuplicateNameError(val name: String, val location: Location) : Errors {
    override fun yaml(): Any =
        singletonMap(
            "DuplicateNameError", mapOf(
                Pair("name", name),
                Pair("location", location)
            )
        )
}

data class UnknownSymbolError(val name: String, val location: Location) : Errors {
    override fun yaml(): Any =
        singletonMap(
            "UnknownSymbolError", mapOf(
                Pair("name", name),
                Pair("location", location)
            )
        )
}

