package io.littlelanguages.mil.compiler

import io.littlelanguages.data.Either
import io.littlelanguages.data.Right
import io.littlelanguages.mil.ArgumentMismatchError
import io.littlelanguages.mil.CompilationError
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.compiler.llvm.Context
import io.littlelanguages.mil.compiler.llvm.FunctionBuilder
import io.littlelanguages.mil.compiler.llvm.Module
import io.littlelanguages.mil.compiler.llvm.VerifyError
import io.littlelanguages.mil.dynamic.*
import io.littlelanguages.mil.dynamic.tst.*
import io.littlelanguages.mil.static.ast.SExpression
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

data class CompileState(val compiler: Compiler, val functionBuilder: FunctionBuilder, val depth: Int)

fun compile(context: Context, moduleID: String, program: Program<CompileState, LLVMValueRef>): Either<List<Errors>, Module> {
    val module = context.module(moduleID)

    Compiler(module).compile(program)

    return Right(module)
}

class Compiler(private val module: Module) {
    private val toCompile = mutableListOf<Declaration<CompileState, LLVMValueRef>>()

    fun compile(program: Program<CompileState, LLVMValueRef>) {
        addFunctions(program.declarations)

        program.values.forEach {
            module.addGlobal(it, module.structValueP, LLVM.LLVMConstPointerNull(module.structValueP), false)
        }

        while (toCompile.isNotEmpty()) {
            val declaration = toCompile.removeAt(0)
            compile(declaration)
        }

        System.err.println(module.toString())

        when (val result = module.verify()) {
            is VerifyError -> throw CompilationError(result.message)
        }
    }

    private fun addFunctions(declarations: List<Declaration<CompileState, LLVMValueRef>>) {
        declarations.forEach { addFunction(it) }
    }

    private fun addFunctionsFromExpressions(es: List<Expression<CompileState, LLVMValueRef>>) {
        es.forEach { addFunctionsFromExpression(it) }
    }

    private fun addFunctionsFromExpression(e: Expression<CompileState, LLVMValueRef>) {
        when (e) {
            is AssignExpression ->
                addFunctionsFromExpression(e.e)

            is CallProcedureExpression ->
                when (val procedure = e.procedure) {
                    is ExternalProcedureBinding ->
                        addFunctionsFromExpressions(e.es)

                    is DeclaredProcedureBinding ->
                        addFunctionsFromExpressions(e.es)

                    else ->
                        TODO(procedure.toString())
                }

            is IfExpression -> {
                addFunctionsFromExpression(e.e1)
                addFunctionsFromExpression(e.e2)
                addFunctionsFromExpression(e.e3)
            }

            is Procedure ->
                addFunction(e)
        }
    }

    private fun addFunction(declaration: Declaration<CompileState, LLVMValueRef>) {
        if (declaration is Procedure) {
            addProcedureToCompile(declaration)
            if (declaration.name == "_main")
                module.addFunctionHeader(declaration.name, emptyList(), module.i32)
            else
                module.addFunctionHeader(
                    declaration.name,
                    List(declaration.parameters.size + if (declaration.isTopLevel()) 0 else 1) { module.structValueP },
                    module.structValueP
                )

            addFunctionsFromExpressions(declaration.es)
        }
    }

    private fun compile(declaration: Declaration<CompileState, LLVMValueRef>) {
        if (declaration is Procedure)
            if (declaration.name == "_main")
                compileMainProcedure(declaration)
            else
                compileProcedure(declaration)
        else
            TODO(declaration.toString())
    }

    private fun compileMainProcedure(declaration: Procedure<CompileState, LLVMValueRef>) {
        val builder = module.addFunctionBody(declaration.name)
        compileProcedureBody(builder, declaration)

        builder.buildRet(LLVM.LLVMConstInt(module.i32, 0, 0))
    }

    private fun compileProcedure(declaration: Procedure<CompileState, LLVMValueRef>) {
        val builder = module.addFunctionBody(declaration.name)
        val result = compileProcedureBody(builder, declaration)

        builder.buildRet(result ?: builder.buildVNull())
    }

    private fun compileProcedureBody(functionBuilder: FunctionBuilder, declaration: Procedure<CompileState, LLVMValueRef>): LLVMValueRef? {
        val frame = functionBuilder.buildMkFrame(
            if (declaration.isTopLevel()) functionBuilder.buildVNull() else functionBuilder.getParam(0),
            declaration.offsets,
            "_frame"
        )

        declaration.parameters.forEachIndexed { index, name ->
            val op = functionBuilder.getParam(index + if (declaration.isTopLevel()) 0 else 1)
            functionBuilder.buildSetFrameValue(frame, index + 1, op)
            functionBuilder.addBindingToScope(name, op)
        }
        functionBuilder.addBindingToScope("_frame", frame)

        functionBuilder.openScope()
        val result = declaration.es.fold(null as LLVMValueRef?) { _, b: Expression<CompileState, LLVMValueRef> ->
            compileE(CompileState(this, functionBuilder, declaration.depth), b)
        }
        functionBuilder.closeScope()

        return result
    }

    private fun addProcedureToCompile(declaration: Procedure<CompileState, LLVMValueRef>) {
        toCompile += declaration
    }
}

private fun <S, T> Procedure<S, T>.isTopLevel(): Boolean =
    this.depth == 0

private fun compileE(compileState: CompileState, e: Expression<CompileState, LLVMValueRef>): LLVMValueRef? =
    CompileExpression(compileState).compileE(e)

private fun compileEForce(compileState: CompileState, e: Expression<CompileState, LLVMValueRef>): LLVMValueRef =
    CompileExpression(compileState).compileEForce(e)

private class CompileExpression(val compileState: CompileState) {
    val functionBuilder = compileState.functionBuilder

    fun compileEForce(e: Expression<CompileState, LLVMValueRef>): LLVMValueRef =
        compileE(e) ?: functionBuilder.buildVNull()

    fun compileE(e: Expression<CompileState, LLVMValueRef>): LLVMValueRef? =
        when (e) {
            is AssignExpression -> {
                when (val symbol = e.symbol) {
                    is TopLevelValueBinding -> {
                        val operand = compileEForce(e.e)
                        functionBuilder.buildStore(operand, functionBuilder.getNamedGlobal(symbol.name)!!)
                        functionBuilder.addBindingToScope(symbol.name, operand)
                    }

                    is ProcedureValueBinding -> {
                        val operand = compileEForce(e.e)
                        functionBuilder.buildSetFrameValue(functionBuilder.getBindingValue("_frame")!!, symbol.offset + 1, operand)
                        functionBuilder.addBindingToScope(symbol.name, operand)
                    }

                    else ->
                        TODO(e.toString())
                }

                null
            }

            is CallProcedureExpression ->
                when (val procedure = e.procedure) {
                    is ExternalProcedureBinding ->
                        procedure.compile(compileState, e.es)

                    is DeclaredProcedureBinding ->
                        if (functionBuilder.getNamedFunction(procedure.name) == null)
                            null
                        else if (procedure.isToplevel())
                            functionBuilder.buildCall(functionBuilder.getNamedFunction(procedure.name)!!, e.es.map { compileEForce(it) })
                        else {
                            val frame = if (compileState.depth == procedure.depth)
                                functionBuilder.getParam(0)
                            else if (compileState.depth < procedure.depth)
                                functionBuilder.getBindingValue("_frame")!!
                            else
                                functionBuilder.buildGetFrameValue(
                                    functionBuilder.getBindingValue("_frame")!!,
                                    compileState.depth - procedure.depth,
                                    0
                                )

                            functionBuilder.buildCall(
                                functionBuilder.getNamedFunction(procedure.name)!!,
                                listOf(frame) + e.es.map { compileEForce(it) })
                        }

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

            is Procedure ->
                null

            is SymbolReferenceExpression -> {
                val symbol = e.symbol
                val result = functionBuilder.getBindingValue(symbol.name)
                if (result == null) {
                    val newResult =
                        when (symbol) {
                            is ParameterBinding ->
                                if (compileState.depth == symbol.depth)
                                    functionBuilder.getParam(symbol.offset + if (compileState.depth == 0) 0 else 1)
                                else if (compileState.depth > symbol.depth)
                                    functionBuilder.buildGetFrameValue(
                                        functionBuilder.getParam(0),
                                        compileState.depth - symbol.depth - 1,
                                        symbol.offset + 1
                                    )
                                else
                                    TODO("depth mismatch")

                            is ExternalValueBinding ->
                                symbol.compile(compileState)!!

                            is ProcedureValueBinding ->
                                functionBuilder.buildGetFrameValue(
                                    functionBuilder.getParam(0),
                                    compileState.depth - symbol.depth - 1,
                                    symbol.offset + 1
                                )

                            else ->
                                functionBuilder.buildLoad(functionBuilder.getNamedGlobal(symbol.name)!!)
                        }

                    functionBuilder.addBindingToScope(symbol.name, newResult)
                    newResult
                } else
                    result

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
) : ExternalProcedureBinding<CompileState, LLVMValueRef>(name) {
    override fun validateArguments(e: SExpression, name: String, arguments: List<Expression<CompileState, LLVMValueRef>>): Errors? =
        if (arity == arguments.size)
            null
        else
            ArgumentMismatchError(name, arity, arguments.size, e.position)

    override fun compile(state: CompileState, arguments: List<Expression<CompileState, LLVMValueRef>>): LLVMValueRef {
        val builder = state.functionBuilder

        val namedFunction = builder.getNamedFunction(
            externalName,
            List(arguments.size) { builder.structValueP },
            builder.structValueP
        )

        return builder.buildCall(namedFunction, arguments.map { compileEForce(state, it) })
    }

}

private abstract class VariableArityExternalProcedure(override val name: String) : ExternalProcedureBinding<CompileState, LLVMValueRef>(name) {
    override fun validateArguments(e: SExpression, name: String, arguments: List<Expression<CompileState, LLVMValueRef>>): Errors? = null
}

private class OperatorExternalProcedure(
    override val name: String,
    private val unitValue: Int,
    private val externalName: String,
    private val explicitFirst: Boolean
) : VariableArityExternalProcedure(name) {
    override fun compile(state: CompileState, arguments: List<Expression<CompileState, LLVMValueRef>>): LLVMValueRef {
        val builder = state.functionBuilder
        val ops = arguments.mapNotNull { compileE(state, it) }

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
    override fun compile(state: CompileState, arguments: List<Expression<CompileState, LLVMValueRef>>): LLVMValueRef? {
        val builder = state.functionBuilder
        arguments.forEach { builder.buildPrintValue(compileE(state, it)) }

        return null
    }
}

private class PrintlnExternalProcedure(override val name: String) : VariableArityExternalProcedure(name) {
    override fun compile(state: CompileState, arguments: List<Expression<CompileState, LLVMValueRef>>): LLVMValueRef? {
        val builder = state.functionBuilder
        arguments.forEach { builder.buildPrintValue(compileE(state, it)) }

        return builder.buildPrintNewline()
    }
}

private class VFalseExternalValue : ExternalValueBinding<CompileState, LLVMValueRef>("#f") {
    override fun compile(state: CompileState): LLVMValueRef =
        state.functionBuilder.buildVFalse()
}

private class VTrueExternalValue : ExternalValueBinding<CompileState, LLVMValueRef>("#t") {
    override fun compile(state: CompileState): LLVMValueRef =
        state.functionBuilder.buildVTrue()
}

private class VNullExternalValue : ExternalValueBinding<CompileState, LLVMValueRef>("()") {
    override fun compile(state: CompileState): LLVMValueRef =
        state.functionBuilder.buildVNull()
}
