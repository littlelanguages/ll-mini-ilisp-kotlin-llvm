package io.littlelanguages.mil.compiler

import io.littlelanguages.data.Either
import io.littlelanguages.data.Right
import io.littlelanguages.mil.ArgumentMismatchError
import io.littlelanguages.mil.CompilationError
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.compiler.llvm.FunctionBuilder
import io.littlelanguages.mil.compiler.llvm.Context
import io.littlelanguages.mil.compiler.llvm.Module
import io.littlelanguages.mil.compiler.llvm.VerifyError
import io.littlelanguages.mil.dynamic.*
import io.littlelanguages.mil.dynamic.tst.*
import io.littlelanguages.mil.static.ast.SExpression
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

fun compile(context: Context, moduleID: String, program: Program<FunctionBuilder, LLVMValueRef>): Either<List<Errors>, Module> {
    val module = context.module(moduleID)

    Compiler(module).compile(program)

    return Right(module)
}

private class Compiler(val module: Module) {
    private val toCompile = mutableListOf<Declaration<FunctionBuilder, LLVMValueRef>>()

    fun compile(program: Program<FunctionBuilder, LLVMValueRef>) {
        program.values.forEach {
            module.addGlobal(it, module.structValueP, LLVM.LLVMConstPointerNull(module.structValueP), false)
        }

        toCompile.addAll(program.declarations)

        while (toCompile.isNotEmpty()) {
            val declaration = toCompile.removeAt(0)
            compile(declaration)
        }

        System.err.println(module.toString())

        when (val result = module.verify()) {
            is VerifyError -> throw CompilationError(result.message)
        }
    }

    private fun compile(declaration: Declaration<FunctionBuilder, LLVMValueRef>) {
        if (declaration is Procedure)
            if (declaration.name == "_main")
                compileMainProcedure(declaration)
            else
                compileProcedure(declaration)
        else
            TODO(declaration.toString())
    }

    private fun compileMainProcedure(declaration: Procedure<FunctionBuilder, LLVMValueRef>) {
        val builder = module.addFunction(declaration.name, emptyList(), module.i32)
        compileProcedureBody(builder, declaration)

        builder.buildRet(LLVM.LLVMConstInt(module.i32, 0, 0))
    }

    private fun compileProcedure(declaration: Procedure<FunctionBuilder, LLVMValueRef>) {
        val builder = module.addFunction(declaration.name, declaration.parameters.map { module.structValueP }, module.structValueP)
        val result = compileProcedureBody(builder, declaration)

        builder.buildRet(result ?: builder.buildVNull())
    }

    private fun compileProcedureBody(functionBuilder: FunctionBuilder, declaration: Procedure<FunctionBuilder, LLVMValueRef>): LLVMValueRef? {
        val frame = functionBuilder.buildMkFrame(functionBuilder.buildVNull(), declaration.offsets, "_frame")

        declaration.parameters.forEachIndexed { index, name ->
            val op = functionBuilder.getParam(index)
            functionBuilder.buildSetFrameValue(frame, index, op)
            functionBuilder.addBindingToScope(name, op)
        }
        functionBuilder.addBindingToScope("_frame", frame)

        functionBuilder.openScope()
        val result = declaration.es.fold(null as LLVMValueRef?) { _, b: Expression<FunctionBuilder, LLVMValueRef> ->
            compileE(functionBuilder, b)
        }
        functionBuilder.closeScope()

        return result
    }

    fun addProcedureToCompile(declaration: Procedure<FunctionBuilder, LLVMValueRef>) {
        toCompile += declaration
    }
}

private fun compileE(functionBuilder: FunctionBuilder, e: Expression<FunctionBuilder, LLVMValueRef>): LLVMValueRef? =
    CompileExpression(functionBuilder).compileE(e)

private fun compileEForce(functionBuilder: FunctionBuilder, e: Expression<FunctionBuilder, LLVMValueRef>): LLVMValueRef =
    CompileExpression(functionBuilder).compileEForce(e)

private class CompileExpression(val functionBuilder: FunctionBuilder) {
    fun compileEForce(e: Expression<FunctionBuilder, LLVMValueRef>): LLVMValueRef =
        compileE(e) ?: functionBuilder.buildVNull()

    fun compileE(e: Expression<FunctionBuilder, LLVMValueRef>): LLVMValueRef? =
        when (e) {
            is AssignExpression -> {
                when (val symbol = e.symbol) {
                    is TopLevelValueBinding ->
                        functionBuilder.buildStore(compileEForce(e.e), functionBuilder.getNamedGlobal(symbol.name)!!)

                    is ProcedureValueBinding -> {
                        val operand = compileEForce(e.e)
                        functionBuilder.buildSetFrameValue(functionBuilder.getBindingValue("_frame")!!, symbol.offset, operand)
                        functionBuilder.addBindingToScope(symbol.name, operand)
                    }

                    else -> TODO(e.toString())
                }

                null
            }

            is CallProcedureExpression ->
                when (val procedure = e.procedure) {
                    is ExternalProcedureBinding ->
                        procedure.compile(functionBuilder, e.es)

                    is DeclaredProcedureBinding ->
                        functionBuilder.buildCall(functionBuilder.getNamedFunction(procedure.name)!!, e.es.map { compileEForce(it) })

                    else ->
                        TODO(procedure.toString())
                }

            is IfExpression -> {
                val e1op = compileEForce(e.e1)
                val falseOp = functionBuilder.buildVFalse()

                val e1Compare = functionBuilder.buildICmp(LLVM.LLVMIntNE, e1op, falseOp)

                val ifThen = functionBuilder.appendBasicBlock()
                val ifElse = functionBuilder.appendBasicBlock()
                val ifEnd = functionBuilder.appendBasicBlock()

                functionBuilder.buildCondBr(e1Compare, ifThen, ifElse)

                functionBuilder.positionAtEnd(ifThen)
                val e2op = compileEForce(e.e2)
                functionBuilder.buildBr(ifEnd)
                val fromThen = functionBuilder.getCurrentBasicBlock()

                functionBuilder.positionAtEnd(ifElse)
                val e3op = compileEForce(e.e3)
                functionBuilder.buildBr(ifEnd)
                val fromElse = functionBuilder.getCurrentBasicBlock()

                functionBuilder.positionAtEnd(ifEnd)

                functionBuilder.buildPhi(functionBuilder.structValueP, listOf(e2op, e3op), listOf(fromThen, fromElse))
            }

            is LiteralInt ->
                functionBuilder.buildFromLiteralInt(e.value)

            is LiteralString ->
                functionBuilder.buildFromLiteralString(e.value)

            is LiteralUnit ->
                functionBuilder.buildVNull()

            is Procedure -> {
                null
            }

            is SymbolReferenceExpression ->
                when (val symbol = e.symbol) {
                    is ParameterBinding ->
                        functionBuilder.getParam(symbol.offset)

                    is ExternalValueBinding ->
                        symbol.compile(functionBuilder)

                    is ProcedureValueBinding -> {
                        val result = functionBuilder.getBindingValue(symbol.name)
                        if (result == null) {
                            val newResult = functionBuilder.buildGetFrameValue(functionBuilder.getBindingValue("_frame")!!, symbol.offset)
                            functionBuilder.addBindingToScope(symbol.name, newResult)
                            newResult
                        } else
                            result
                    }

                    else ->
                        functionBuilder.buildLoad(functionBuilder.getNamedGlobal(symbol.name)!!)
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
) : ExternalProcedureBinding<FunctionBuilder, LLVMValueRef>(name) {
    override fun validateArguments(e: SExpression, name: String, arguments: List<Expression<FunctionBuilder, LLVMValueRef>>): Errors? =
        if (arity == arguments.size)
            null
        else
            ArgumentMismatchError(name, arity, arguments.size, e.position)

    override fun compile(builder: FunctionBuilder, arguments: List<Expression<FunctionBuilder, LLVMValueRef>>): LLVMValueRef {
        val namedFunction = builder.getNamedFunction(
            externalName,
            List(arguments.size) { builder.structValueP },
            builder.structValueP
        )

        return builder.buildCall(namedFunction, arguments.map { compileEForce(builder, it) })
    }

}

private abstract class VariableArityExternalProcedure(override val name: String) : ExternalProcedureBinding<FunctionBuilder, LLVMValueRef>(name) {
    override fun validateArguments(e: SExpression, name: String, arguments: List<Expression<FunctionBuilder, LLVMValueRef>>): Errors? = null
}

private class OperatorExternalProcedure(
    override val name: String,
    private val unitValue: Int,
    private val externalName: String,
    private val explicitFirst: Boolean
) : VariableArityExternalProcedure(name) {
    override fun compile(builder: FunctionBuilder, arguments: List<Expression<FunctionBuilder, LLVMValueRef>>): LLVMValueRef {
        val ops = arguments.mapNotNull { compileE(builder, it) }

        val namedFunction = builder.getNamedFunction(
            externalName,
            List(2) { builder.structValueP },
            builder.structValueP
        )

        return if (ops.isEmpty())
            builder.buildFromLiteralInt(unitValue)
        else if (explicitFirst && ops.size == 1)
            builder.buildCall(namedFunction, listOf(builder.buildFromLiteralInt(unitValue), ops[0]))
        else
            ops.drop(1).fold(ops[0]) { op1, op2 -> builder.buildCall(namedFunction, listOf(op1, op2)) }
    }
}

private class PrintExternalProcedure(override val name: String) : VariableArityExternalProcedure(name) {
    override fun compile(builder: FunctionBuilder, arguments: List<Expression<FunctionBuilder, LLVMValueRef>>): LLVMValueRef? {
        arguments.forEach { builder.buildPrintValue(compileE(builder, it)) }

        return null
    }
}

private class PrintlnExternalProcedure(override val name: String) : VariableArityExternalProcedure(name) {
    override fun compile(builder: FunctionBuilder, arguments: List<Expression<FunctionBuilder, LLVMValueRef>>): LLVMValueRef? {
        arguments.forEach { builder.buildPrintValue(compileE(builder, it)) }

        return builder.buildPrintNewline()
    }
}

private class VFalseExternalValue : ExternalValueBinding<FunctionBuilder, LLVMValueRef>("#f") {
    override fun compile(builder: FunctionBuilder): LLVMValueRef =
        builder.buildVFalse()
}

private class VTrueExternalValue : ExternalValueBinding<FunctionBuilder, LLVMValueRef>("#t") {
    override fun compile(builder: FunctionBuilder): LLVMValueRef =
        builder.buildVTrue()
}

private class VNullExternalValue : ExternalValueBinding<FunctionBuilder, LLVMValueRef>("()") {
    override fun compile(builder: FunctionBuilder): LLVMValueRef =
        builder.buildVNull()
}
