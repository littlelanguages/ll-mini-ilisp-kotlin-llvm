package io.littlelanguages.p0.semantic

import io.littlelanguages.data.Either
import io.littlelanguages.data.Right
import io.littlelanguages.p0.Errors
import io.littlelanguages.p0.dynamic.tst.*
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*

fun compile(p: Program, moduleName: String): Either<List<Errors>, ByteArray> =
        Right(Compiler(moduleName).p(p))


private class Bindings(private var offset: Int = 0) {
    private var offsets =
            mutableListOf<Pair<Int, MutableMap<String, Int>>>()

    private var names =
            mutableMapOf<String, Int>()

    fun newBinding(name: String): Int {
        val result =
                offset

        offset += 1
        names[name] = result

        return result
    }

    fun offset(name: String): Int? =
            names[name]

    fun openScope() {
        offsets.add(Pair(offset, names.toMutableMap()))
    }

    fun closeScope() {
        val previous =
                offsets.last()

        offset = previous.first
        names = previous.second

        offsets.dropLast(1)
    }
}


private class Compiler(private val moduleName: String) {
    private val cw =
            ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES)

    fun p(p: Program): ByteArray {
        cw.visit(V1_5, ACC_PUBLIC, moduleName, null, "java/lang/Object", null)

        addDefaultConstructor()

        for (d in p.declarations) {
            when (d) {
                is ConstantDeclaration ->
                    cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, d.n, d.v.typeOf().descriptor(), null, d.v.valueOf()).visitEnd()
                is VariableDeclaration ->
                    cw.visitField(ACC_PUBLIC + ACC_STATIC, d.n, d.v.typeOf().descriptor(), null, d.v.valueOf()).visitEnd()
                is FunctionDeclaration -> {
                    val methodVisitor =
                            cw.visitMethod(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, d.n, functionDescription(d.ps.map { it.second }, d.e?.typeOf()), null, null)

                    val bindings =
                            Bindings(0)

                    val functionCompiler =
                            FunctionCompiler(moduleName, bindings, methodVisitor)

                    d.ps.forEach { bindings.newBinding(it.first) }
                    d.ss.forEach { functionCompiler.s(it) }

                    if (d.e == null) {
                        methodVisitor.visitInsn(RETURN)
                    } else {
                        functionCompiler.e(d.e)
                        when (d.e.typeOf()) {
                            Type.Float -> methodVisitor.visitInsn(FRETURN)
                            Type.Int -> methodVisitor.visitInsn(IRETURN)
                            Type.Bool -> methodVisitor.visitInsn(IRETURN)
                            else -> internalError("Compiler: p: return type: $d")
                        }
                    }

                    methodVisitor.visitMaxs(2, 1)
                    methodVisitor.visitEnd()
                }
            }
        }

        addMainFunction(p)

        cw.visitEnd()

        return cw.toByteArray()
    }

    private fun addDefaultConstructor() {
        val initVisitor =
                cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)

        initVisitor.visitCode()
        initVisitor.visitVarInsn(ALOAD, 0)
        initVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        initVisitor.visitInsn(RETURN)
        initVisitor.visitMaxs(1, 1)
        initVisitor.visitEnd()
    }

    private fun addMainFunction(p: Program) {
        val mainVisitor =
                cw.visitMethod(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null)

        FunctionCompiler(moduleName, Bindings(1), mainVisitor).s(p.statement)

        mainVisitor.visitInsn(RETURN)
        mainVisitor.visitMaxs(2, 1)
        mainVisitor.visitEnd()
    }
}


private class FunctionCompiler(val moduleName: String, val bindings: Bindings, val mv: MethodVisitor) {
    fun ss(tst: List<Statement>) {
        bindings.openScope()
        tst.forEach { s(it) }
        bindings.closeScope()
    }


    fun s(tst: Statement) {
        when (tst) {
            is AssignmentStatement -> {
                e(tst.e)

                val offset =
                        bindings.offset(tst.n)

                if (offset == null)
                    mv.visitFieldInsn(PUTSTATIC, moduleName, tst.n, tst.e.typeOf().descriptor())
                else
                    when (tst.e.typeOf()) {
                        Type.Bool -> mv.visitVarInsn(ISTORE, offset)
                        Type.Float -> mv.visitVarInsn(FSTORE, offset)
                        Type.Int -> mv.visitVarInsn(ISTORE, offset)
                        else -> internalError("Compiler: s: AssignmentStatement: local: $tst")
                    }

            }

            is ConstantDeclarationStatement -> {
                e(tst.e)

                val offset =
                        bindings.newBinding(tst.n)

                when (tst.e.typeOf()) {
                    Type.Bool -> mv.visitVarInsn(ISTORE, offset)
                    Type.Float -> mv.visitVarInsn(FSTORE, offset)
                    Type.Int -> mv.visitVarInsn(ISTORE, offset)
                    else -> internalError("Compiler: s: ConstantDeclarationStatement: $tst")
                }
            }

            is VariableDeclarationStatement -> {
                e(tst.e)

                val offset =
                        bindings.newBinding(tst.n)

                when (tst.e.typeOf()) {
                    Type.Bool -> mv.visitVarInsn(ISTORE, offset)
                    Type.Float -> mv.visitVarInsn(FSTORE, offset)
                    Type.Int -> mv.visitVarInsn(ISTORE, offset)
                    else -> internalError("Compiler: s: VariableDeclarationStatement: $tst")
                }
            }

            is IfThenElseStatement -> {
                val elseLabel =
                        Label()

                val endLabel =
                        Label()

                e(tst.e)

                mv.visitJumpInsn(IFEQ, elseLabel)
                s(tst.s1)
                mv.visitJumpInsn(GOTO, endLabel)
                mv.visitLabel(elseLabel)
                if (tst.s2 != null)
                    s(tst.s2)
                mv.visitLabel(endLabel)
            }

            is WhileStatement -> {
                val topLabel =
                        Label()

                val endLabel =
                        Label()

                mv.visitLabel(topLabel)

                e(tst.e)

                mv.visitJumpInsn(IFEQ, endLabel)
                s(tst.s)
                mv.visitJumpInsn(GOTO, topLabel)
                mv.visitLabel(endLabel)
            }

            is CallStatement -> {
                if (tst.n == "println" || tst.n == "print") {
                    tst.args.forEach {
                        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")

                        if (it.typeOf() == Type.Float) {
                            mv.visitFieldInsn(GETSTATIC, "java/util/Locale", "US", "Ljava/util/Locale;")
                            mv.visitLdcInsn("%.6f")
                            mv.visitInsn(ICONST_1)
                            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object")
                            mv.visitInsn(DUP)
                            mv.visitInsn(ICONST_0)
                            e(it)
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false)
                            mv.visitInsn(AASTORE)
                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "printf", "(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/io/PrintStream;", false)
                            mv.visitInsn(POP)
                        } else {
                            e(it)

                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", functionDescription(listOf(it.typeOf()), null), false)
                        }
                    }

                    if (tst.n == "println") {
                        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "()V", false)
                    }
                } else {
                    tst.args.forEach { e(it) }
                    mv.visitMethodInsn(INVOKESTATIC, moduleName, tst.n, functionDescription(tst.args.map { it.typeOf() }, null), false)
                }
            }

            is BlockStatement ->
                ss(tst.ss)

            is EmptyStatement -> {
                // Nothing to do
            }
        }
    }

    fun e(tst: Expression) {
        when (tst) {
            is TernaryExpression -> {
                val elseLabel =
                        Label()

                val endLabel =
                        Label()

                e(tst.e1)
                mv.visitJumpInsn(IFEQ, elseLabel)
                e(tst.e2)
                mv.visitJumpInsn(GOTO, endLabel)
                mv.visitLabel(elseLabel)
                e(tst.e3)
                mv.visitLabel(endLabel)
            }

            is BinaryExpression -> {
                e(tst.e1)
                e(tst.e2)

                when (tst.op) {
                    BinaryOp.And ->
                        mv.visitInsn(IAND)

                    BinaryOp.Or ->
                        mv.visitInsn(IOR)

                    BinaryOp.Equal ->
                        when (tst.e1.typeOf()) {
                            Type.Float -> floatRelationalOp(IFEQ)
                            Type.Int -> intRelationalOp(IF_ICMPEQ)
                            Type.Bool -> mv.visitInsn(IAND)
                            else -> internalError("Compiler: e: BinaryOp.Equal: $tst")
                        }

                    BinaryOp.NotEqual ->
                        when (tst.e1.typeOf()) {
                            Type.Float -> floatRelationalOp(IFNE)
                            Type.Int -> intRelationalOp(IF_ICMPNE)
                            Type.Bool -> intRelationalOp(IF_ICMPNE)
                            else -> internalError("Compiler: e: BinaryOp.NotEqual: $tst")
                        }

                    BinaryOp.LessThan ->
                        when (tst.e1.typeOf()) {
                            Type.Float -> floatRelationalOp(IFLT)
                            Type.Int -> intRelationalOp(IF_ICMPLT)
                            else -> internalError("Compiler: e: BinaryOp.LessThan: $tst")
                        }

                    BinaryOp.LessEqual ->
                        when (tst.e1.typeOf()) {
                            Type.Float -> floatRelationalOp(IFLE)
                            Type.Int -> intRelationalOp(IF_ICMPLE)
                            else -> internalError("Compiler: e: BinaryOp.LessEqual: $tst")
                        }

                    BinaryOp.GreaterEqual ->
                        when (tst.e1.typeOf()) {
                            Type.Float -> floatRelationalOp(IFGE)
                            Type.Int -> intRelationalOp(IF_ICMPGE)
                            else -> internalError("Compiler: e: BinaryOp.GreaterEqual: $tst")
                        }

                    BinaryOp.GreaterThan ->
                        when (tst.e1.typeOf()) {
                            Type.Float -> floatRelationalOp(IFGT)
                            Type.Int -> intRelationalOp(IF_ICMPGT)
                            else -> internalError("Compiler: e: BinaryOp.GreaterThan: $tst")
                        }

                    BinaryOp.Plus ->
                        when (tst.e1.typeOf()) {
                            Type.Float -> mv.visitInsn(FADD)
                            Type.Int -> mv.visitInsn(IADD)
                            else -> internalError("Compiler: e: BinaryOp.Plus: $tst")
                        }

                    BinaryOp.Minus ->
                        when (tst.e1.typeOf()) {
                            Type.Float -> mv.visitInsn(FSUB)
                            Type.Int -> mv.visitInsn(ISUB)
                            else -> internalError("Compiler: e: BinaryOp.Minus: $tst")
                        }

                    BinaryOp.Times ->
                        when (tst.e1.typeOf()) {
                            Type.Float -> mv.visitInsn(FMUL)
                            Type.Int -> mv.visitInsn(IMUL)
                            else -> internalError("Compiler: e: BinaryOp.Times: $tst")
                        }

                    BinaryOp.Divide ->
                        when (tst.e1.typeOf()) {
                            Type.Float -> mv.visitInsn(FDIV)
                            Type.Int -> mv.visitInsn(IDIV)
                            else -> internalError("Compiler: e: BinaryOp.Divide: $tst")
                        }
                }
            }

            is UnaryExpression ->
                when (tst.op) {
                    UnaryOp.UnaryNot -> {
                        val elseLabel =
                                Label()

                        val endLabel =
                                Label()

                        e(tst.e)

                        mv.visitJumpInsn(IFEQ, elseLabel)
                        mv.visitInsn(ICONST_0)
                        mv.visitJumpInsn(GOTO, endLabel)
                        mv.visitLabel(elseLabel)
                        mv.visitInsn(ICONST_1)
                        mv.visitLabel(endLabel)
                    }
                    UnaryOp.UnaryMinus -> {
                        e(tst.e)

                        mv.visitInsn(if (tst.t == Type.Int) INEG else FNEG)
                    }
                    UnaryOp.UnaryPlus ->
                        e(tst.e)
                }


            is CallExpression -> {
                tst.args.forEach { e(it) }
                mv.visitMethodInsn(INVOKESTATIC, moduleName, tst.n, functionDescription(tst.args.map { it.typeOf() }, tst.t), false)
            }

            is IdentifierReference -> {
                val offset =
                        bindings.offset(tst.n)

                if (offset == null)
                    mv.visitFieldInsn(GETSTATIC, moduleName, tst.n, tst.t.descriptor())
                else
                    when (tst.t) {
                        Type.Bool -> mv.visitVarInsn(ILOAD, offset)
                        Type.Float -> mv.visitVarInsn(FLOAD, offset)
                        Type.Int -> mv.visitVarInsn(ILOAD, offset)
                        else -> internalError("Compiler: e: IdentifierReference: $tst")
                    }
            }

            is LiteralValueExpression ->
                lv(tst.v)
        }
    }

    private fun floatRelationalOp(jmpInstruction: Int) {
        val elseLabel =
                Label()

        val endLabel =
                Label()

        mv.visitInsn(FCMPG)
        mv.visitJumpInsn(jmpInstruction, elseLabel)
        mv.visitInsn(ICONST_0)
        mv.visitJumpInsn(GOTO, endLabel)
        mv.visitLabel(elseLabel)
        mv.visitInsn(ICONST_1)
        mv.visitLabel(endLabel)
    }

    private fun intRelationalOp(opInstruction: Int) {
        val elseLabel =
                Label()

        val endLabel =
                Label()

        mv.visitJumpInsn(opInstruction, elseLabel)
        mv.visitInsn(ICONST_0)
        mv.visitJumpInsn(GOTO, endLabel)
        mv.visitLabel(elseLabel)
        mv.visitInsn(ICONST_1)
        mv.visitLabel(endLabel)
    }

    private fun lv(tst: LiteralValue) {
        when (tst) {
            is LiteralString ->
                mv.visitLdcInsn(tst.v)

            is LiteralBool ->
                mv.visitLdcInsn(tst.v)

            is LiteralInt ->
                mv.visitLdcInsn(tst.v)

            is LiteralFloat ->
                mv.visitLdcInsn(tst.v)
        }
    }
}

private fun LiteralValue.valueOf(): Any =
        when (this) {
            is LiteralString -> this.v
            is LiteralBool -> this.v
            is LiteralInt -> this.v
            is LiteralFloat -> this.v
        }


private fun functionDescription(ps: List<Type>, r: Type?): String =
        "(" + ps.joinToString("") { it.descriptor() } + ")" + (r?.descriptor() ?: "V")

private fun Type.descriptor(): String =
        when (this) {
            Type.Bool -> "Z"
            Type.Float -> "F"
            Type.Int -> "I"
            Type.String -> "Ljava/lang/String;"
            Type.TError -> "I"
        }

private fun internalError(rationale: String) {
    throw InternalError(rationale)
}