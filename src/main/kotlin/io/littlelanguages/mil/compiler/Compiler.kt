package io.littlelanguages.mil.compiler

import io.littlelanguages.data.Either
import io.littlelanguages.data.Right
import io.littlelanguages.mil.ArgumentMismatchError
import io.littlelanguages.mil.CompilationError
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.compiler.llvm.*
import io.littlelanguages.mil.dynamic.ExternalProcedureBinding
import io.littlelanguages.mil.dynamic.ParameterBinding
import io.littlelanguages.mil.dynamic.tst.*
import io.littlelanguages.mil.static.ast.SExpression
import org.bytedeco.javacpp.PointerPointer
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

        declaration.es.forEach {
            compileE(builder, it)
        }

        builder.buildRet(LLVM.LLVMConstInt(module.i32, 0, 0))
    }

    private fun compileProcedure(declaration: Procedure<Builder, LLVMValueRef>) {
        val builder = module.addFunction(declaration.name, declaration.arguments.map { module.structValueP }, module.structValueP)

        val result = declaration.es.fold(null as LLVMValueRef?) { _, b: Expression<Builder, LLVMValueRef> ->
            compileE(builder, b)
        }

        builder.buildRet(result ?: builder.invoke(BuiltinDeclarationEnum.V_NULL))
    }
}

fun compileE(builder: Builder, e: Expression<Builder, LLVMValueRef>): LLVMValueRef? =
    CompileExpression(builder).compileE(e)

fun compileEForce(builder: Builder, e: Expression<Builder, LLVMValueRef>): LLVMValueRef =
    CompileExpression(builder).compileEForce(e)

private class CompileExpression(val builder: Builder) {
    fun compileEForce(e: Expression<Builder, LLVMValueRef>): LLVMValueRef =
        compileE(e) ?: builder.invoke(BuiltinDeclarationEnum.V_NULL)

    fun compileE(e: Expression<Builder, LLVMValueRef>): LLVMValueRef? =
        when (e) {
            is AssignExpression -> {
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
                val falseOp = builder.invoke(BuiltinDeclarationEnum.V_FALSE)

                val e1Compare = builder.buildICmp(LLVM.LLVMIntNE, e1op, falseOp, builder.nextName())

                val ifThen = builder.appendBasicBlock(builder.nextName())
                val ifElse = builder.appendBasicBlock(builder.nextName())
                val ifEnd = builder.appendBasicBlock(builder.nextName())

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
                builder.buildPhi(builder.structValueP, listOf(e2op, e3op), listOf(fromThen, fromElse), builder.nextName())
            }

            is LiteralBool ->
                builder.invoke(if (e.value) BuiltinDeclarationEnum.V_TRUE else BuiltinDeclarationEnum.V_FALSE)

            is LiteralInt ->
                builder.invoke(BuiltinDeclarationEnum.FROM_LITERAL_INT, listOf(LLVM.LLVMConstInt(builder.i32, e.value.toLong(), 0)))

            is LiteralString -> {
                val globalStringName = builder.addGlobalString(e.value, builder.nextName())

                builder.invoke(
                    BuiltinDeclarationEnum.FROM_LITERAL_STRING,
                    listOf(LLVM.LLVMConstInBoundsGEP(globalStringName, PointerPointer(builder.c0i64, builder.c0i64), 2))
                )
            }

            is LiteralUnit ->
                builder.invoke(BuiltinDeclarationEnum.V_NULL)

            is SymbolReferenceExpression ->
                when (val symbol = e.symbol) {
                    is ParameterBinding ->
                        builder.getParam(symbol.offset)

                    else ->
                        builder.buildLoad(builder.getNamedGlobal(symbol.name)!!, builder.nextName())
                }

            else ->
                TODO(e.toString())
        }
}

private fun validateFixedArityArgument(arity: Int): (e: SExpression, name: String, arguments: List<Expression<Builder, LLVMValueRef>>) -> Errors? =
    { e, name, arguments ->
        if (arity == arguments.size)
            null
        else
            ArgumentMismatchError(name, arity, arguments.size, e.position)
    }

private fun validateVariableArityArguments(): (e: SExpression, name: String, arguments: List<Expression<Builder, LLVMValueRef>>) -> Errors? =
    { _, _, _ -> null }

private fun compileFixedArity(externalName: String): (builder: Builder, arguments: List<Expression<Builder, LLVMValueRef>>) -> LLVMValueRef =
    { builder, arguments ->
        val namedFunction = builder.getNamedFunction(externalName) ?: builder.addExternalFunction(
            externalName,
            List(arguments.size) { builder.structValueP },
            builder.structValueP
        )
        builder.buildCall(namedFunction, arguments.map { compileEForce(builder, it) })
    }

private fun compileOperator(
    unitValue: Int,
    externalName: String,
    explicitFirst: Boolean
): (builder: Builder, arguments: List<Expression<Builder, LLVMValueRef>>) -> LLVMValueRef? =
    { builder, arguments ->
        val ops = arguments.mapNotNull { compileE(builder, it) }

        val namedFunction = builder.getNamedFunction(externalName) ?: builder.addExternalFunction(
            externalName,
            List(2) { builder.structValueP },
            builder.structValueP
        )

        if (ops.isEmpty())
            compileE(builder, LiteralInt(unitValue))
        else if (explicitFirst && ops.size == 1)
            builder.buildCall(namedFunction, listOf(compileEForce(builder, LiteralInt(unitValue)), ops[0]))
        else
            ops.drop(1).fold(ops[0]) { op1, op2 -> builder.buildCall(namedFunction, listOf(op1, op2)) }
    }

private val compilePrintln: (builder: Builder, arguments: List<Expression<Builder, LLVMValueRef>>) -> LLVMValueRef? =
    { builder, arguments ->
        arguments.forEach { builder.buildPrintValue(compileE(builder, it)) }

        builder.buildPrintNewline()
    }

private val compilePrint: (builder: Builder, arguments: List<Expression<Builder, LLVMValueRef>>) -> LLVMValueRef? =
    { builder, arguments ->
        arguments.forEach { builder.buildPrintValue(compileE(builder, it)) }

        null
    }

val builtinBindings = listOf(
    ExternalProcedureBinding("+", validateVariableArityArguments(), compileOperator(0, "_plus", false)),
    ExternalProcedureBinding("-", validateVariableArityArguments(), compileOperator(0, "_minus", true)),
    ExternalProcedureBinding("*", validateVariableArityArguments(), compileOperator(1, "_multiply", false)),
    ExternalProcedureBinding("/", validateVariableArityArguments(), compileOperator(1, "_divide", true)),
    ExternalProcedureBinding("=", validateFixedArityArgument(2), compileFixedArity("_equals")),
    ExternalProcedureBinding("<", validateFixedArityArgument(2), compileFixedArity("_less_than")),
    ExternalProcedureBinding("boolean?", validateFixedArityArgument(1), compileFixedArity("_booleanp")),
    ExternalProcedureBinding("car", validateFixedArityArgument(1), compileFixedArity("_pair_car")),
    ExternalProcedureBinding("cdr", validateFixedArityArgument(1), compileFixedArity("_pair_cdr")),
    ExternalProcedureBinding("integer?", validateFixedArityArgument(1), compileFixedArity("_integerp")),
    ExternalProcedureBinding("null?", validateFixedArityArgument(1), compileFixedArity("_nullp")),
    ExternalProcedureBinding("pair", validateFixedArityArgument(2), compileFixedArity("_mk_pair")),
    ExternalProcedureBinding("print", validateVariableArityArguments(), compilePrint),
    ExternalProcedureBinding("println", validateVariableArityArguments(), compilePrintln),
    ExternalProcedureBinding("string?", validateFixedArityArgument(1), compileFixedArity("_stringp")),
    ExternalProcedureBinding("pair?", validateFixedArityArgument(1), compileFixedArity("_pairp")),
)

