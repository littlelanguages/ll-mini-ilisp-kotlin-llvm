package io.littlelanguages.mil.compiler

import io.littlelanguages.data.Either
import io.littlelanguages.data.Right
import io.littlelanguages.mil.CompilationError
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.compiler.llvm.Context
import io.littlelanguages.mil.compiler.llvm.FunctionBuilder
import io.littlelanguages.mil.compiler.llvm.Module
import io.littlelanguages.mil.compiler.llvm.VerifyError
import io.littlelanguages.mil.dynamic.*
import io.littlelanguages.mil.dynamic.tst.*
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

data class CompileState(val compiler: Compiler, val functionBuilder: FunctionBuilder, val depth: Int)

fun compile(context: Context, moduleID: String, program: Program<CompileState, LLVMValueRef>): Either<List<Errors>, Module> {
    val module = context.module(moduleID)

    Compiler(module).compile(program)

    val pm = LLVM.LLVMCreatePassManager()
    LLVM.LLVMAddAggressiveInstCombinerPass(pm)
    LLVM.LLVMAddNewGVNPass(pm)
    LLVM.LLVMAddCFGSimplificationPass(pm)
    LLVM.LLVMRunPassManager(pm, module.module)

    return Right(module)
}

class Compiler(private val module: Module) {
    private val toCompile = mutableListOf<Declaration<CompileState, LLVMValueRef>>()

    fun compile(program: Program<CompileState, LLVMValueRef>) {
        addFunctions(program.declarations)

        module.addGlobalString(module.moduleID, "_filename")

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

    private fun addFunctionsFromExpressionss(ess: Expressionss<CompileState, LLVMValueRef>) {
        ess.forEach { addFunctionsFromExpressions(it) }
    }

    private fun addFunctionsFromExpressions(es: Expressions<CompileState, LLVMValueRef>) {
        es.forEach { addFunctionsFromExpression(it) }
    }

    private fun addFunctionsFromExpression(e: Expression<CompileState, LLVMValueRef>) {
        when (e) {
            is AssignExpression ->
                addFunctionsFromExpressions(e.es)

            is CallProcedureExpression ->
                when (val procedure = e.procedure) {
                    is ExternalProcedureBinding ->
                        addFunctionsFromExpressionss(e.es)

                    is DeclaredProcedureBinding ->
                        addFunctionsFromExpressionss(e.es)

                    else ->
                        TODO(procedure.toString())
                }

            is IfExpression -> {
                addFunctionsFromExpressions(e.e1)
                addFunctionsFromExpressions(e.e2)
                addFunctionsFromExpressions(e.e3)
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
            compileExpression(CompileState(this, functionBuilder, declaration.depth), b)
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

private fun compileExpression(compileState: CompileState, e: Expression<CompileState, LLVMValueRef>): LLVMValueRef? =
    CompileExpression(compileState).compileExpression(e)

private fun compileScopedExpressionsForce(compileState: CompileState, es: Expressions<CompileState, LLVMValueRef>): LLVMValueRef =
    CompileExpression(compileState).compileScopedExpressionsForce(es)

private class CompileExpression(val compileState: CompileState) {
    val functionBuilder = compileState.functionBuilder

    fun compileScopedExpressionsForce(es: Expressions<CompileState, LLVMValueRef>): LLVMValueRef {
        functionBuilder.openScope()
        val op = compileExpressionsForce(es)
        functionBuilder.closeScope()
        return op
    }

    fun compileExpressionsForce(es: Expressions<CompileState, LLVMValueRef>): LLVMValueRef =
        es.fold(null as LLVMValueRef?) { _, b: Expression<CompileState, LLVMValueRef> ->
            compileExpression(b)
        } ?: functionBuilder.buildVNull()

    fun compileScopedExpressionForce(e: Expression<CompileState, LLVMValueRef>): LLVMValueRef {
        functionBuilder.openScope()
        val op = compileExpressionForce(e)
        functionBuilder.closeScope()
        return op
    }

    fun compileExpressionForce(e: Expression<CompileState, LLVMValueRef>): LLVMValueRef =
        compileExpression(e) ?: functionBuilder.buildVNull()

    fun compileExpression(e: Expression<CompileState, LLVMValueRef>): LLVMValueRef? =
        when (e) {
            is AssignExpression -> {
                val symbol = e.symbol
                val operand = compileScopedExpressionsForce(e.es)
                when (symbol) {
                    is TopLevelValueBinding ->
                        functionBuilder.buildStore(operand, functionBuilder.getNamedGlobal(symbol.name)!!)

                    is ProcedureValueBinding ->
                        functionBuilder.buildSetFrameValue(functionBuilder.getBindingValue("_frame")!!, symbol.offset + 1, operand)

                    else ->
                        TODO(e.toString())
                }
                functionBuilder.addBindingToScope(symbol.name, operand)

                null
            }

            is CallProcedureExpression ->
                when (val procedure = e.procedure) {
                    is ExternalProcedureBinding ->
                        procedure.compile(compileState, e.lineNumber, e.es)

                    is DeclaredProcedureBinding -> {
                        val functionRef = functionBuilder.getNamedFunction(procedure.name)!!
                        val arguments = e.es.map { compileExpressionsForce(it) }
                        val fullArguments = if (procedure.isToplevel()) arguments else listOf(getFrame(procedure.depth)) + arguments

                        functionBuilder.buildCall(functionRef, fullArguments)
                    }

                    else ->
                        TODO(procedure.toString())
                }

            is CallValueExpression -> {
                val op = compileScopedExpressionsForce(e.operand)
                val es = e.es.map { compileScopedExpressionForce(it) }

                functionBuilder.buildCallClosure(
                    LLVM.LLVMConstInBoundsGEP(
                        functionBuilder.getNamedGlobal("_filename")!!,
                        PointerPointer(functionBuilder.c0i64, functionBuilder.c0i64),
                        2
                    ),
                    e.lineNumber,
                    op, es
                )
            }

            is IfExpression -> {
                val e1op = compileScopedExpressionsForce(e.e1)
                val falseOp = functionBuilder.buildVFalse()

                val e1Compare = functionBuilder.buildICmp(LLVM.LLVMIntNE, e1op, falseOp)

                val ifThen = functionBuilder.appendBasicBlock()
                val ifElse = functionBuilder.appendBasicBlock()
                val ifEnd = functionBuilder.appendBasicBlock()

                functionBuilder.buildCondBr(e1Compare, ifThen, ifElse)

                functionBuilder.positionAtEnd(ifThen)
                val e2op = compileScopedExpressionsForce(e.e2)
                functionBuilder.buildBr(ifEnd)
                val fromThen = functionBuilder.getCurrentBasicBlock()

                functionBuilder.positionAtEnd(ifElse)
                val e3op = compileScopedExpressionsForce(e.e3)
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

                            is FixedArityExternalProcedure ->
                                functionBuilder.buildFromNativeProcedure(
                                    LLVM.LLVMConstInBoundsGEP(
                                        functionBuilder.getNamedGlobal("_filename")!!,
                                        PointerPointer(functionBuilder.c0i64, functionBuilder.c0i64),
                                        2
                                    ), e.lineNumber, symbol.externalName, symbol.arity
                                )

                            is ExternalValueBinding ->
                                symbol.compile(compileState)!!

                            is ProcedureValueBinding ->
                                if (compileState.depth == symbol.depth)
                                    functionBuilder.buildGetFrameValue(
                                        functionBuilder.getBindingValue("_frame")!!,
                                        0,
                                        symbol.offset + 1
                                    )
                                else
                                    functionBuilder.buildGetFrameValue(
                                        functionBuilder.getParam(0),
                                        compileState.depth - symbol.depth - 1,
                                        symbol.offset + 1
                                    )

                            is DeclaredProcedureBinding ->
                                if (symbol.depth == 0)
                                    functionBuilder.buildFromNativeProcedure(
                                        LLVM.LLVMConstInBoundsGEP(
                                            functionBuilder.getNamedGlobal("_filename")!!,
                                            PointerPointer(functionBuilder.c0i64, functionBuilder.c0i64),
                                            2
                                        ),
                                        e.lineNumber,
                                        symbol.name,
                                        symbol.parameterCount
                                    )
                                else
                                    functionBuilder.buildFromDynamicProcedure(
                                        symbol.name,
                                        symbol.parameterCount,
                                        getFrame(symbol.depth)
                                    )

                            is TopLevelValueBinding ->
                                functionBuilder.buildLoad(functionBuilder.getNamedGlobal(symbol.name)!!)

                            is VariableArityExternalProcedure ->
                                functionBuilder.buildFromNativeVarArgProcedure(symbol.externalName)

                            is VariableArityExternalPositionProcedure ->
                                functionBuilder.buildFromNativeVarArgPositionProcedure(
                                    LLVM.LLVMConstInBoundsGEP(
                                        functionBuilder.getNamedGlobal("_filename")!!,
                                        PointerPointer(functionBuilder.c0i64, functionBuilder.c0i64),
                                        2
                                    ),
                                    e.lineNumber,
                                    symbol.externalName
                                )

                            else ->
                                TODO(e.toString())
                        }

                    functionBuilder.addBindingToScope(symbol.name, newResult)
                    newResult
                } else
                    result

            }

            else ->
                TODO(e.toString())
        }

    private fun getFrame(depth: Int): LLVMValueRef =
        if (compileState.depth == depth)
            functionBuilder.getParam(0)
        else if (compileState.depth < depth)
            functionBuilder.getBindingValue("_frame")!!
        else
            functionBuilder.buildGetFrameValue(
                functionBuilder.getBindingValue("_frame")!!,
                compileState.depth - depth,
                0
            )
}

val builtinBindings = listOf(
    VariableArityExternalProcedure("+", "_plus_variable"),
    VariableArityExternalProcedure("-", "_minus_variable"),
    VariableArityExternalProcedure("*", "_multiply_variable"),
    VariableArityExternalProcedure("/", "_divide_variable"),
    FixedArityExternalProcedure("=", 2, "_equals"),
    FixedArityExternalProcedure("<", 2, "_less_than"),
    FixedArityExternalProcedure("boolean?", 1, "_booleanp"),
    FixedArityExternalPositionProcedure("car", 1, "_pair_car"),
    FixedArityExternalPositionProcedure("cdr", 1, "_pair_cdr"),
    FixedArityExternalProcedure("integer?", 1, "_integerp"),
    FixedArityExternalProcedure("null?", 1, "_nullp"),
    FixedArityExternalProcedure("pair", 2, "_mk_pair"),
    VariableArityExternalPositionProcedure("print", "_print"),
    VariableArityExternalPositionProcedure("println", "_println"),
    FixedArityExternalProcedure("string?", 1, "_stringp"),
    FixedArityExternalProcedure("pair?", 1, "_pairp"),

    VFalseExternalValue(),
    VTrueExternalValue(),
    VNullExternalValue(),
)

private class FixedArityExternalProcedure(
    override val name: String,
    override val arity: Int,
    val externalName: String
) : ExternalProcedureBinding<CompileState, LLVMValueRef>(name, arity) {
    override fun compile(state: CompileState, lineNumber: Int, arguments: Expressionss<CompileState, LLVMValueRef>): LLVMValueRef {
        val builder = state.functionBuilder

        val namedFunction = builder.getNamedFunction(
            externalName,
            List(arguments.size) { builder.structValueP },
            builder.structValueP
        )

        return builder.buildCall(namedFunction, arguments.map { compileScopedExpressionsForce(state, it) })
    }
}

private class FixedArityExternalPositionProcedure(
    override val name: String,
    override val arity: Int,
    val externalName: String
) : ExternalProcedureBinding<CompileState, LLVMValueRef>(name, arity) {
    override fun compile(state: CompileState, lineNumber: Int, arguments: Expressionss<CompileState, LLVMValueRef>): LLVMValueRef {
        val builder = state.functionBuilder

        val namedFunction = builder.getNamedFunction(
            externalName,
            listOf(builder.i8P, builder.i32) + List(arguments.size) { builder.structValueP },
            builder.structValueP
        )

        return builder.buildCall(
            namedFunction,
            listOf(
                LLVM.LLVMConstInBoundsGEP(builder.getNamedGlobal("_filename")!!, PointerPointer(builder.c0i64, builder.c0i64), 2),
                LLVM.LLVMConstInt(builder.i32, lineNumber.toLong(), 0)
            ) + arguments.map { compileScopedExpressionsForce(state, it) })
    }
}

private class VariableArityExternalProcedure(
    override val name: String,
    val externalName: String
) : ExternalProcedureBinding<CompileState, LLVMValueRef>(name, null) {
    override fun compile(state: CompileState, lineNumber: Int, arguments: Expressionss<CompileState, LLVMValueRef>): LLVMValueRef {
        val builder = state.functionBuilder

        return builder.buildCall(
            builder.getNamedFunction(externalName, listOf(builder.i32), builder.structValueP, true),
            listOf(LLVM.LLVMConstInt(builder.i32, arguments.size.toLong(), 0)) + arguments.map { compileScopedExpressionsForce(state, it) }
        )
    }
}

private class VariableArityExternalPositionProcedure(
    override val name: String,
    val externalName: String
) : ExternalProcedureBinding<CompileState, LLVMValueRef>(name, null) {
    override fun compile(state: CompileState, lineNumber: Int, arguments: Expressionss<CompileState, LLVMValueRef>): LLVMValueRef {
        val builder = state.functionBuilder

        return builder.buildCall(
            builder.getNamedFunction(externalName, listOf(builder.i8P, builder.i32, builder.i32), builder.structValueP, true),
            listOf(
                LLVM.LLVMConstInBoundsGEP(builder.getNamedGlobal("_filename")!!, PointerPointer(builder.c0i64, builder.c0i64), 2),
                LLVM.LLVMConstInt(builder.i32, lineNumber.toLong(), 0),
                LLVM.LLVMConstInt(builder.i32, arguments.size.toLong(), 0)
            ) + arguments.map { compileScopedExpressionsForce(state, it) }
        )
    }
}

private class VFalseExternalValue : ExternalValueBinding<CompileState, LLVMValueRef>("#f") {
    override fun compile(state: CompileState, lineNumber: Int): LLVMValueRef =
        state.functionBuilder.buildVFalse()
}

private class VTrueExternalValue : ExternalValueBinding<CompileState, LLVMValueRef>("#t") {
    override fun compile(state: CompileState, lineNumber: Int): LLVMValueRef =
        state.functionBuilder.buildVTrue()
}

private class VNullExternalValue : ExternalValueBinding<CompileState, LLVMValueRef>("()") {
    override fun compile(state: CompileState, lineNumber: Int): LLVMValueRef =
        state.functionBuilder.buildVNull()
}
