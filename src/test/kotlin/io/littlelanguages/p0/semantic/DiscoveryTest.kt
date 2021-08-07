package io.littlelanguages.p0.semantic

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.littlelanguages.p0.bin.MyClassLoader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import org.objectweb.asm.Opcodes.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class DiscoveryTest : FunSpec({
    /**
     * The quintessential Hello World.  The output is written to java.lang.System.out which is captured and validated with an assertion.
     *
     * This code shows off the basic mechanisms for compiling of P0.
     */
    context("Hello World") {
        val cw =
                ClassWriter(COMPUTE_MAXS + COMPUTE_FRAMES)

        cw.visit(V1_5, ACC_PUBLIC, "Main", null, "java/lang/Object", null)

        val initVisitor =
                cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)

        initVisitor.visitCode()
        initVisitor.visitVarInsn(ALOAD, 0)
        initVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        initVisitor.visitInsn(RETURN)
        initVisitor.visitMaxs(1, 1)
        initVisitor.visitEnd()

        val mainVisitor =
                cw.visitMethod(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null)

        mainVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
        mainVisitor.visitLdcInsn("Hello world")
        mainVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
        mainVisitor.visitInsn(RETURN)
        mainVisitor.visitMaxs(2, 1)
        mainVisitor.visitEnd()

        cw.visitEnd()

        val myClassLoader =
                MyClassLoader()

        val bs: ByteArray =
                cw.toByteArray()

        val oldOut =
                System.out

        try {
            val baos =
                    ByteArrayOutputStream()

            val ps =
                    PrintStream(baos)

            System.setOut(ps)

            myClassLoader.defineClass("Main", bs)
            myClassLoader.loadClass("Main").getMethod("main", String::class.java.arrayType()).invoke(null, null)

            ps.close()

            baos.toString() shouldBe "Hello world\n"
        } finally {
            System.setOut(oldOut)
        }
    }
})
