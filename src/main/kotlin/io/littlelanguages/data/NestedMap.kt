package io.littlelanguages.data

class NestedMap<K, V> {
    private var last = mutableMapOf<K, V>()
    private val scope = mutableListOf(last)

    fun open() {
        last = mutableMapOf()
        scope.add(last)
    }

    fun close() {
        scope.dropLast(1)
        last = scope.last()
    }

    fun add(key: K, value: V) {
        last[key] = value
    }

    fun inCurrentNesting(key: K): Boolean =
        last.containsKey(key)

    fun get(key: K): V? {
        for (s in scope.size - 1 downTo 0) {
            val value = scope[s][key]

            if (value != null)
                return value
        }

        return null
    }
}
