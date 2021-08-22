package io.littlelanguages.mil.compiler

import io.littlelanguages.data.Either
import io.littlelanguages.data.Right
import io.littlelanguages.mil.CompilationError
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.compiler.llvm.Builder
import io.littlelanguages.mil.compiler.llvm.Context
import io.littlelanguages.mil.compiler.llvm.Module
import io.littlelanguages.mil.compiler.llvm.pointerPointerOf
import io.littlelanguages.mil.dynamic.ParameterBinding
import io.littlelanguages.mil.dynamic.tst.*
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

fun compile(context: Context, moduleID: String, program: Program): Either<List<Errors>, Module> {
    val module = context.module(moduleID)
    val compiler = Compiler(module)
    compiler.compile(program)

    return Right(module)
}

private class Compiler(val module: Module) {
    val builtinDeclarations = BuiltinDeclarations(module)

    fun compile(program: Program) {
        program.values.forEach {
            module.addGlobal(it, module.structValueP, LLVM.LLVMConstPointerNull(module.structValueP), false)
        }

        program.declarations.forEach {
            compile(it)
        }

        System.err.println(module.toString())

        when (val result = module.verify()) {
            is io.littlelanguages.mil.compiler.llvm.VerifyError -> throw CompilationError(result.message)
        }
    }

    private fun compile(declaration: Declaration) {
        if (declaration is Procedure)
            if (declaration.name == "_main")
                compileMainProcedure(declaration)
            else
                compileProcedure(declaration)
        else
            TODO(declaration.toString())
    }

    private fun compileMainProcedure(declaration: Procedure) {
        val builder = module.addFunction(declaration.name, emptyList(), module.i32)

        declaration.es.forEach {
            compileE(builder, it)
        }

        builder.buildRet(LLVM.LLVMConstInt(module.i32, 0, 0))
    }

    private fun compileProcedure(declaration: Procedure) {
        val builder = module.addFunction(declaration.name, declaration.arguments.map { module.structValueP }, module.structValueP)

        val result = declaration.es.fold(null) { _: LLVMValueRef?, b: Expression ->
            compileE(builder, b)
        }

        builder.buildRet(result ?: builtinDeclarations.invoke(builder, BuiltinDeclarationEnum.V_NULL, module.nextName()))
    }

    private fun compileE(builder: Builder, e: Expression): LLVMValueRef? =
        CompileExpression(builder, builtinDeclarations).compileE(e)
}

private class CompileExpression(val builder: Builder, val builtinDeclarations: BuiltinDeclarations) {
    fun compileEForce(e: Expression): LLVMValueRef =
        compileE(e) ?: builtinDeclarations.invoke(builder, BuiltinDeclarationEnum.V_NULL, builder.nextName())

    fun compileE(e: Expression): LLVMValueRef? =
        when (e) {
            is AssignExpression -> {
                builder.buildStore(compileEForce(e.e), builder.getNamedGlobal(e.symbol.name)!!)

                null
            }

            is BooleanPExpression ->
                builtinDeclarations.invoke(builder, BuiltinDeclarationEnum.BOOLEANP, listOf(compileEForce(e.es)), builder.nextName())

            is CallProcedureExpression ->
                builder.buildCall(builder.getNamedFunction(e.procedure.name)!!, e.es.map { compileEForce(it) }, builder.nextName())

            is CarExpression ->
                builtinDeclarations.invoke(builder, BuiltinDeclarationEnum.CAR, listOf(compileEForce(e.es)), builder.nextName())

            is CdrExpression ->
                builtinDeclarations.invoke(builder, BuiltinDeclarationEnum.CDR, listOf(compileEForce(e.es)), builder.nextName())

            is EqualsExpression ->
                builtinDeclarations.invoke(
                    builder,
                    BuiltinDeclarationEnum.EQUALS,
                    listOf(compileEForce(e.e1), compileEForce(e.e2)),
                    builder.nextName()
                )

            is IfExpression -> {
                val e1op = compileEForce(e.e1)
                val falseOp = builtinDeclarations.invoke(builder, BuiltinDeclarationEnum.V_FALSE, builder.nextName())

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

            is IntegerPExpression ->
                builtinDeclarations.invoke(builder, BuiltinDeclarationEnum.INTEGERP, listOf(compileEForce(e.es)), builder.nextName())

            is LessThanExpression ->
                builtinDeclarations.invoke(
                    builder,
                    BuiltinDeclarationEnum.LESS_THAN,
                    listOf(compileEForce(e.e1), compileEForce(e.e2)),
                    builder.nextName()
                )

            is LiteralBool ->
                builtinDeclarations.invoke(
                    builder,
                    if (e == LiteralBool.TRUE) BuiltinDeclarationEnum.V_TRUE else BuiltinDeclarationEnum.V_FALSE,
                    builder.nextName()
                )

            is LiteralInt ->
                builtinDeclarations.invoke(
                    builder,
                    BuiltinDeclarationEnum.FROM_LITERAL_INT,
                    listOf(LLVM.LLVMConstInt(builder.i32, e.value.toLong(), 0)),
                    builder.nextName()
                )

            is LiteralString -> {
                val globalStringName = builder.addGlobalString(e.value, builder.nextName())

                builtinDeclarations.invoke(
                    builder,
                    BuiltinDeclarationEnum.FROM_LITERAL_STRING,
                    listOf(LLVM.LLVMConstInBoundsGEP(globalStringName, PointerPointer(builder.c0i64, builder.c0i64), 2)),
                    builder.nextName()
                )
            }

            is LiteralUnit ->
                builtinDeclarations.invoke(builder, BuiltinDeclarationEnum.V_NULL, builder.nextName())

            is MinusExpression ->
                compileOperator(e.es, 0, BuiltinDeclarationEnum.MINUS, true)

            is NullPExpression ->
                builtinDeclarations.invoke(builder, BuiltinDeclarationEnum.NULLP, listOf(compileEForce(e.es)), builder.nextName())

            is PairExpression ->
                builtinDeclarations.invoke(
                    builder,
                    BuiltinDeclarationEnum.PAIR,
                    listOf(compileEForce(e.car), compileEForce(e.cdr)),
                    builder.nextName()
                )

            is PairPExpression ->
                builtinDeclarations.invoke(builder, BuiltinDeclarationEnum.PAIRP, listOf(compileEForce(e.es)), builder.nextName())

            is PlusExpression ->
                compileOperator(e.es, 0, BuiltinDeclarationEnum.PLUS, false)

            is PrintlnExpression -> {
                e.es.forEach {
                    val op = compileE(it)

                    if (op != null) {
                        builtinDeclarations.invoke(builder, BuiltinDeclarationEnum.PRINT_VALUE, listOf(op), "")
                    }
                }

                builtinDeclarations.invoke(builder, BuiltinDeclarationEnum.PRINT_NEWLINE, listOf(), "")

                null
            }

            is SlashExpression ->
                compileOperator(e.es, 1, BuiltinDeclarationEnum.DIVIDE, true)

            is StarExpression ->
                compileOperator(e.es, 1, BuiltinDeclarationEnum.MULTIPLY, false)

            is StringPExpression ->
                builtinDeclarations.invoke(builder, BuiltinDeclarationEnum.STRINGP, listOf(compileEForce(e.es)), builder.nextName())

            is SymbolReferenceExpression ->
                when (val symbol = e.symbol) {
                    is ParameterBinding ->
                        builder.getParam(symbol.offset)

                    else ->
                        builder.buildLoad(builder.getNamedGlobal(symbol.name)!!, builder.nextName())
                }

            else ->
                TODO(e.toString())
//                return builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.V_NULL, builder.nextName())
        }

    private fun compileOperator(es: Expressions, unitValue: Int, operator: BuiltinDeclarationEnum, explicitFirst: Boolean): LLVMValueRef? {
        val ops = es.mapNotNull { compileE(it) }

        return if (ops.isEmpty())
            compileE(LiteralInt(unitValue))
        else if (explicitFirst && ops.size == 1)
            builtinDeclarations.invoke(builder, operator, listOf(compileEForce(LiteralInt(unitValue)), ops[0]), builder.nextName())
        else
            ops.drop(1).fold(ops[0]) { op1, op2 -> builtinDeclarations.invoke(builder, operator, listOf(op1, op2), builder.nextName()) }
    }
}

private enum class BuiltinDeclarationEnum {
    BOOLEANP, CAR, CDR, DIVIDE, EQUALS, FROM_LITERAL_INT, FROM_LITERAL_STRING,
    INTEGERP, LESS_THAN, MINUS, MULTIPLY, NULLP, PAIR, PAIRP, PLUS,
    PRINT_VALUE, PRINT_NEWLINE, STRINGP, V_TRUE,
    V_FALSE, V_NULL
}

private class BuiltinDeclarations(val module: Module) {
    private val declarations = mapOf(
        Pair(BuiltinDeclarationEnum.BOOLEANP, BuiltinDeclaration("_booleanp", listOf(module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.CAR, BuiltinDeclaration("_pair_car", listOf(module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.CDR, BuiltinDeclaration("_pair_cdr", listOf(module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.DIVIDE, BuiltinDeclaration("_divide", listOf(module.structValueP, module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.EQUALS, BuiltinDeclaration("_equals", listOf(module.structValueP, module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.FROM_LITERAL_INT, BuiltinDeclaration("_from_literal_int", listOf(module.i32), module.structValueP)),
        Pair(BuiltinDeclarationEnum.FROM_LITERAL_STRING, BuiltinDeclaration("_from_literal_string", listOf(module.i8P), module.structValueP)),
        Pair(BuiltinDeclarationEnum.INTEGERP, BuiltinDeclaration("_integerp", listOf(module.structValueP), module.structValueP)),
        Pair(
            BuiltinDeclarationEnum.LESS_THAN,
            BuiltinDeclaration("_less_than", listOf(module.structValueP, module.structValueP), module.structValueP)
        ),
        Pair(BuiltinDeclarationEnum.MINUS, BuiltinDeclaration("_minus", listOf(module.structValueP, module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.MULTIPLY, BuiltinDeclaration("_multiply", listOf(module.structValueP, module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.NULLP, BuiltinDeclaration("_nullp", listOf(module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.PAIR, BuiltinDeclaration("_mk_pair", listOf(module.structValueP, module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.PAIRP, BuiltinDeclaration("_pairp", listOf(module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.PLUS, BuiltinDeclaration("_plus", listOf(module.structValueP, module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.PRINT_VALUE, BuiltinDeclaration("_print_value", listOf(module.structValueP), module.void)),
        Pair(BuiltinDeclarationEnum.PRINT_NEWLINE, BuiltinDeclaration("_print_newline", listOf(), module.void)),
        Pair(BuiltinDeclarationEnum.STRINGP, BuiltinDeclaration("_stringp", listOf(module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.V_TRUE, BuiltinDeclaration("_VTrue", null, module.structValueP)),
        Pair(BuiltinDeclarationEnum.V_FALSE, BuiltinDeclaration("_VFalse", null, module.structValueP)),
        Pair(BuiltinDeclarationEnum.V_NULL, BuiltinDeclaration("_VNull", null, module.structValueP))
    )

    fun get(bip: BuiltinDeclarationEnum): LLVMValueRef {
        val declaration = declarations[bip]!!
        return if (declaration.isProcedure())
            module.getNamedFunction(declaration.name) ?: run {
                val parameters = declaration.parameters!!
                val parameterTypes = pointerPointerOf(parameters)

                module.addExternalFunction(
                    declaration.name,
                    LLVM.LLVMFunctionType(declaration.returnType, parameterTypes, declaration.parameters.size, 0)
                )
            }
        else
            module.getNamedGlobal(declaration.name) ?: module.addGlobal(declaration.name, declaration.returnType)!!
    }

    fun invoke(builder: Builder, bip: BuiltinDeclarationEnum, arguments: List<LLVMValueRef>, name: String): LLVMValueRef =
        builder.buildCall(get(bip), arguments, name)

    fun invoke(builder: Builder, bip: BuiltinDeclarationEnum, name: String): LLVMValueRef =
        builder.buildLoad(get(bip), name)
}

private data class BuiltinDeclaration(val name: String, val parameters: List<LLVMTypeRef>?, val returnType: LLVMTypeRef) {
    fun isProcedure() =
        parameters != null
}
