package io.littlelanguages.p0.bin


class MyClassLoader : ClassLoader() {
    fun defineClass(name: String, b: ByteArray): Class<*> {
        return defineClass(name, b, 0, b.size)
    }
}