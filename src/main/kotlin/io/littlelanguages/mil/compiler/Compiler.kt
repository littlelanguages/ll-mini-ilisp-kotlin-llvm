package io.littlelanguages.mil.compiler

import io.littlelanguages.data.Either
import io.littlelanguages.data.Right
import io.littlelanguages.mil.ArgumentMismatchError
import io.littlelanguages.mil.CompilationError
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.compiler.llvm.Builder
import io.littlelanguages.mil.compiler.llvm.Context
import io.littlelanguages.mil.compiler.llvm.Module
import io.littlelanguages.mil.compiler.llvm.VerifyError
import io.littlelanguages.mil.dynamic.ExternalProcedureBinding
import io.littlelanguages.mil.dynamic.ExternalValueBinding
import io.littlelanguages.mil.dynamic.ParameterBinding
import io.littlelanguages.mil.dynamic.tst.*
import io.littlelanguages.mil.static.ast.SExpression
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

fun compile(context: Context, moduleID: String, program: Program<Builder, LLVMValueRef>): Either<List<Errors>, Module> {
    val module = context.module(moduleID)
    val compiler = Compiler(module)
    compiler.compile(program)

    return Right(module)
}

private class Compiler(val module: Module) {
    fun compile(program: Program<Builder, LLVMValueRef>) {
        program.values.forEach {
            module.addGlobal(it, module.structValueP, LLVM.LLVMConstPointerNull(module.structValueP), false)
        }

        program.declarations.forEach {
            compile(it)
        }

        System.err.println(module.toString())

        when (val result = module.verify()) {
            is VerifyError -> throw CompilationError(result.message)
        }
    }

    private fun compile(declaration: Declaration<Builder, LLVMValueRef>) {
        if (declaration is Procedure)
            if (declaration.name == "_main")
                compileMainProcedure(declaration)
            else
                compileProcedure(declaration)
        else
            TODO(declaration.toString())
    }

    private fun compileMainProcedure(declaration: Procedure<Builder, LLVMValueRef>) {
        val builder = module.addFunction(declaration.name, emptyList(), module.i32)
        compileProcedureBody(builder, declaration)

        builder.buildRet(LLVM.LLVMConstInt(module.i32, 0, 0))
    }

    private fun compileProcedure(declaration: Procedure<Builder, LLVMValueRef>) {
        val builder = module.addFunction(declaration.name, declaration.arguments.map { module.structValueP }, module.structValueP)
        val result = compileProcedureBody(builder, declaration)

        builder.buildRet(result ?: builder.buildVNull())
    }

    private fun compileProcedureBody(builder: Builder, declaration: Procedure<Builder, LLVMValueRef>): LLVMValueRef? {
        val frame = builder.buildMkFrame(builder.buildVNull(), declaration.arguments.size, "_frame")

        declaration.arguments.forEachIndexed { index, name ->
            val op = builder.getParam(index)
            builder.buildSetFrameValue(frame, index, op)
            builder.addBindingToScope(name, op)
        }
        builder.addBindingToScope("_frame", frame)

        builder.openScope()
        val result = declaration.es.fold(null as LLVMValueRef?) { _, b: Expression<Builder, LLVMValueRef> ->
            compileE(builder, b)
        }
        builder.closeScope()

        return result
    }
}

fun compileE(builder: Builder, e: Expression<Builder, LLVMValueRef>): LLVMValueRef? =
    CompileExpression(builder).compileE(e)

fun compileEForce(builder: Builder, e: Expression<Builder, LLVMValueRef>): LLVMValueRef =
    CompileExpression(builder).compileEForce(e)

private class CompileExpression(val builder: Builder) {
    fun compileEForce(e: Expression<Builder, LLVMValueRef>): LLVMValueRef =
        compileE(e) ?: builder.buildVNull()

    fun compileE(e: Expression<Builder, LLVMValueRef>): LLVMValueRef? =
        when (e) {
            is AssignExpression -> {
                println(e)
                builder.buildStore(compileEForce(e.e), builder.getNamedGlobal(e.symbol.name)!!)

                null
            }

            is CallProcedureExpression ->
                when (val procedure = e.procedure) {
                    is ExternalProcedureBinding ->
                        procedure.compile(builder, e.es)
                    else ->
                        builder.buildCall(builder.getNamedFunction(procedure.name)!!, e.es.map { compileEForce(it) })
                }

            is IfExpression -> {
                val e1op = compileEForce(e.e1)
                val falseOp = builder.buildVFalse()


                val e1Compare = builder.buildICmp(LLVM.LLVMIntNE, e1op, falseOp)

                val ifThen = builder.appendBasicBlock()
                val ifElse = builder.appendBasicBlock()
                val ifEnd = builder.appendBasicBlock()

                builder.buildCondBr(e1Compare, ifThen, ifElse)

                builder.positionAtEnd(ifThen)
                val e2op = compileEForce(e.e2)
                builder.buildBr(ifEnd)
                val fromThen = builder.getCurrentBasicBlock()

                builder.positionAtEnd(ifElse)
                val e3op = compileEForce(e.e3)
                builder.buildBr(ifEnd)
                val fromElse = builder.getCurrentBasicBlock()

                builder.positionAtEnd(ifEnd)

                builder.buildPhi(builder.structValueP, listOf(e2op, e3op), listOf(fromThen, fromElse))
            }

            is LiteralInt ->
                builder.buildFromLiteralInt(e.value)

            is LiteralString ->
                builder.buildFromLiteralString(e.value)

            is LiteralUnit ->
                builder.buildVNull()

            is SymbolReferenceExpression ->
                when (val symbol = e.symbol) {
                    is ParameterBinding ->
                        builder.getParam(symbol.offset)

                    is ExternalValueBinding ->
                        symbol.compile(builder)

                    else ->
                        builder.buildLoad(builder.getNamedGlobal(symbol.name)!!)
                }

            else ->
                TODO(e.toString())
        }
}

val builtinBindings = listOf(
    OperatorExternalProcedure("+", 0, "_plus", false),
    OperatorExternalProcedure("-", 0, "_minus", true),
    OperatorExternalProcedure("*", 1, "_multiply", false),
    OperatorExternalProcedure("/", 1, "_divide", true),
    FixedArityExternalProcedure("=", 2, "_equals"),
    FixedArityExternalProcedure("<", 2, "_less_than"),
    FixedArityExternalProcedure("boolean?", 1, "_booleanp"),
    FixedArityExternalProcedure("car", 1, "_pair_car"),
    FixedArityExternalProcedure("cdr", 1, "_pair_cdr"),
    FixedArityExternalProcedure("integer?", 1, "_integerp"),
    FixedArityExternalProcedure("null?", 1, "_nullp"),
    FixedArityExternalProcedure("pair", 2, "_mk_pair"),
    PrintExternalProcedure("print"),
    PrintlnExternalProcedure("println"),
    FixedArityExternalProcedure("string?", 1, "_stringp"),
    FixedArityExternalProcedure("pair?", 1, "_pairp"),

    VFalseExternalValue(),
    VTrueExternalValue(),
    VNullExternalValue(),
)

private class FixedArityExternalProcedure(
    override val name: String,
    private val arity: Int,
    private val externalName: String
) : ExternalProcedureBinding<Builder, LLVMValueRef>(name) {
    override fun validateArguments(e: SExpression, name: String, arguments: List<Expression<Builder, LLVMValueRef>>): Errors? =
        if (arity == arguments.size)
            null
        else
            ArgumentMismatchError(name, arity, arguments.size, e.position)

    override fun compile(builder: Builder, arguments: List<Expression<Builder, LLVMValueRef>>): LLVMValueRef {
        val namedFunction = builder.getNamedFunction(
            externalName,
            List(arguments.size) { builder.structValueP },
            builder.structValueP
        )

        return builder.buildCall(namedFunction, arguments.map { compileEForce(builder, it) })
    }

}

private abstract class VariableArityExternalProcedure(override val name: String) : ExternalProcedureBinding<Builder, LLVMValueRef>(name) {
    override fun validateArguments(e: SExpression, name: String, arguments: List<Expression<Builder, LLVMValueRef>>): Errors? = null
}

private class OperatorExternalProcedure(
    override val name: String,
    private val unitValue: Int,
    private val externalName: String,
    private val explicitFirst: Boolean
) : VariableArityExternalProcedure(name) {
    override fun compile(builder: Builder, arguments: List<Expression<Builder, LLVMValueRef>>): LLVMValueRef? {
        val ops = arguments.mapNotNull { compileE(builder, it) }

        val namedFunction = builder.getNamedFunction(
            externalName,
            List(2) { builder.structValueP },
            builder.structValueP
        )

        return if (ops.isEmpty())
            compileE(builder, LiteralInt(unitValue))
        else if (explicitFirst && ops.size == 1)
            builder.buildCall(namedFunction, listOf(compileEForce(builder, LiteralInt(unitValue)), ops[0]))
        else
            ops.drop(1).fold(ops[0]) { op1, op2 -> builder.buildCall(namedFunction, listOf(op1, op2)) }
    }
}

private class PrintExternalProcedure(override val name: String) : VariableArityExternalProcedure(name) {
    override fun compile(builder: Builder, arguments: List<Expression<Builder, LLVMValueRef>>): LLVMValueRef? {
        arguments.forEach { builder.buildPrintValue(compileE(builder, it)) }

        return null
    }
}

private class PrintlnExternalProcedure(override val name: String) : VariableArityExternalProcedure(name) {
    override fun compile(builder: Builder, arguments: List<Expression<Builder, LLVMValueRef>>): LLVMValueRef? {
        arguments.forEach { builder.buildPrintValue(compileE(builder, it)) }

        return builder.buildPrintNewline()
    }
}

private class VFalseExternalValue : ExternalValueBinding<Builder, LLVMValueRef>("#f") {
    override fun compile(builder: Builder): LLVMValueRef =
        builder.buildVFalse()
}

private class VTrueExternalValue : ExternalValueBinding<Builder, LLVMValueRef>("#t") {
    override fun compile(builder: Builder): LLVMValueRef =
        builder.buildVTrue()
}

private class VNullExternalValue : ExternalValueBinding<Builder, LLVMValueRef>("()") {
    override fun compile(builder: Builder): LLVMValueRef =
        builder.buildVNull()
}
