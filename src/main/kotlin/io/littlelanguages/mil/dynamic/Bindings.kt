package io.littlelanguages.mil.dynamic

import io.littlelanguages.data.NestedMap
import io.littlelanguages.data.Yamlable
import io.littlelanguages.mil.ArgumentMismatchError
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

data class ProcedureValueBinding<S, T>(override val name: String, val depth: Int, val offset: Int) : Binding<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "procedure-value", mapOf(
                Pair("name", name),
                Pair("depth", depth),
                Pair("offset", offset)
            )
        )
}

sealed interface ProcedureBinding<S, T> : Binding<S, T>

data class DeclaredProcedureBinding<S, T>(override val name: String, val parameterCount: Int, val depth: Int) : ProcedureBinding<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "declared-procedure", mapOf(
                Pair("name", name),
                Pair("parameter-count", parameterCount),
                Pair("depth", depth)
            )
        )

    fun isToplevel(): Boolean =
        depth == 0
}

abstract class ExternalValueBinding<S, T>(override val name: String) : Binding<S, T> {
    override fun yaml(): Any =
        singletonMap("external-value", name)

    abstract fun compile(state: S): T?
}

abstract class ExternalProcedureBinding<S, T>(
    override val name: String,
    open val arity: Int?
) : ProcedureBinding<S, T> {
    override fun yaml(): Any =
        singletonMap("external-procedure", name)

    fun validateArguments(e: SExpression, name: String, arguments: List<List<Expression<S, T>>>): Errors? =
        when (arity) {
            null -> null
            arguments.size -> null
            else -> ArgumentMismatchError(name, arity!!, arguments.size, e.position)
        }

    abstract fun compile(state: S, arguments: List<Expression<S, T>>): T?
}

data class ParameterBinding<S, T>(override val name: String, val depth: Int, val offset: Int) : Binding<S, T> {
    override fun yaml(): Any =
        singletonMap(
            "parameter", mapOf(
                Pair("name", name),
                Pair("depth", depth),
                Pair("offset", offset)
            )
        )
}

typealias Bindings<S, T> =
        NestedMap<String, Binding<S, T>>
