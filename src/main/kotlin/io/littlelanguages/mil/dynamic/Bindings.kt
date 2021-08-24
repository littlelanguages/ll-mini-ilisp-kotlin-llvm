package io.littlelanguages.mil.dynamic

import io.littlelanguages.data.Yamlable
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.dynamic.tst.Expression
import io.littlelanguages.mil.static.ast.SExpression

sealed interface Binding<S, T> : Yamlable {
    val name: String
}

data class TopLevelValueBinding<S, T>(override val name: String) : Binding<S, T> {
    override fun yaml(): Any =
        singletonMap("toplevel-value", name)
}

sealed interface ProcedureBinding<S, T> : Binding<S, T>

data class TopLevelProcedureBinding<S, T>(override val name: String, val parameterCount: Int) : ProcedureBinding<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "toplevel-procedure", mapOf(
                Pair("name", name),
                Pair("parameter-count", parameterCount)
            )
        )
}

data class ExternalValueBinding<S, T>(override val name: String, val externalName: String) : Binding<S, T> {
    override fun yaml(): Any =
        singletonMap("external-value", name)
}

abstract class ExternalProcedureBinding<S, T>(
    override val name: String,
) : ProcedureBinding<S, T> {
    override fun yaml(): Any =
        singletonMap("external-procedure", name)

    abstract fun validateArguments(e: SExpression, name: String, arguments: List<Expression<S, T>>): Errors?
    abstract fun compile(builder: S, arguments: List<Expression<S, T>>): T?
}

data class ParameterBinding<S, T>(override val name: String, val offset: Int) : Binding<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "parameter", mapOf(
                Pair("name", name),
                Pair("offset", offset)
            )
        )
}

class Bindings<S, T> {
    private var last = mutableMapOf<String, Binding<S, T>>()
    private val scope = mutableListOf(last)

    fun open() {
        last = mutableMapOf()
        scope.add(last)
    }

    fun close() {
        scope.dropLast(1)
        last = scope.last()
    }

    fun add(name: String, binding: Binding<S, T>) {
        last[name] = binding
    }

    fun inCurrentScope(name: String): Boolean =
        last.containsKey(name)

    fun get(name: String): Binding<S, T>? {
        for (s in scope.size - 1 downTo 0) {
            val binding = scope[s][name]

            if (binding != null)
                return binding
        }

        return null
    }
}
