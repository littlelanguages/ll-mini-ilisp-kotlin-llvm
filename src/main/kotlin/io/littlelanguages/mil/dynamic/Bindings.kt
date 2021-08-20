package io.littlelanguages.mil.dynamic

import io.littlelanguages.data.Yamlable

sealed interface Binding  : Yamlable {
    val name: String
}

data class TopLevelValueBinding(override val name: String) : Binding {
    override fun yaml(): Any =
        singletonMap("toplevel-value", name)
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

    fun get(name: String): Binding? {
        for (s in scope.size - 1 downTo 0) {
            val binding = scope[s][name]

            if (binding != null)
                return binding
        }

        return null
    }
}
