package io.littlelanguages.mil.dynamic

import io.littlelanguages.data.Yamlable

sealed interface Binding : Yamlable {
    val name: String
}

data class TopLevelValueBinding(override val name: String) : Binding {
    override fun yaml(): Any =
        singletonMap("toplevel-value", name)
}

data class TopLevelProcedureBinding(override val name: String, val parameterCount: Int) : Binding {
    override fun yaml(): Any =
        singletonMap(
            "toplevel-procedure", mapOf(
                Pair("name", name),
                Pair("parameter-count", parameterCount)
            )
        )
}

data class ParameterBinding(override val name: String, val offset: Int) : Binding {
    override fun yaml(): Any =
        singletonMap(
            "parameter", mapOf(
                Pair("name", name),
                Pair("offset", offset)
            )
        )
}

class Bindings {
    private var last = mutableMapOf<String, Binding>()
    private val scope = mutableListOf(last)

    fun open() {
        last = mutableMapOf()
        scope.add(last)
    }

    fun close() {
        scope.dropLast(1)
        last = scope.last()
    }

    fun add(name: String, binding: Binding) {
        last[name] = binding
    }

    fun inCurrentScope(name: String): Boolean =
        last.containsKey(name)

    fun get(name: String): Binding? {
        for (s in scope.size - 1 downTo 0) {
            val binding = scope[s][name]

            if (binding != null)
                return binding
        }

        return null
    }
}
