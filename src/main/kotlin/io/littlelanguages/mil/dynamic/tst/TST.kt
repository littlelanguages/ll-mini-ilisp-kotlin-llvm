package io.littlelanguages.mil.dynamic.tst

import io.littlelanguages.data.Yamlable

data class Program(val names: Set<String>, val procedures: List<Procedure>) : Yamlable {
    override fun yaml(): Any =
        singletonMap(
            "Program", mapOf(
                Pair("names", names),
                Pair("procedures", procedures.map { it.yaml() })
            )
        )
}

data class Procedure(val name: String, val arguments: List<String>, val es: List<Expression>) : Yamlable {
    override fun yaml(): Any =
        singletonMap(
            "Procedure", mapOf(
                Pair("name", name),
                Pair("arguments", arguments),
                Pair("es", es.map { it.yaml() })
            )
        )
}

sealed class Expression : Yamlable
