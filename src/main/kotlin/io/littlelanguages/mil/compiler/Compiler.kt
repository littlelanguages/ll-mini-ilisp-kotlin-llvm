package io.littlelanguages.mil.compiler

import io.littlelanguages.data.Either
import io.littlelanguages.data.Right
import io.littlelanguages.mil.CompilationError
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.compiler.llvm.Builder
import io.littlelanguages.mil.compiler.llvm.Module
import io.littlelanguages.mil.compiler.llvm.pointerPointerOf
import io.littlelanguages.mil.dynamic.tst.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

interface LLVMState {
    val context: LLVMContextRef
    val module: Module

    fun dispose()
}

fun compile(moduleID: String, program: Program): Either<List<Errors>, LLVMState> {
    LLVM.LLVMInitializeCore(LLVM.LLVMGetGlobalPassRegistry())
    LLVM.LLVMLinkInMCJIT()
    LLVM.LLVMInitializeNativeAsmPrinter()
    LLVM.LLVMInitializeNativeAsmParser()
    LLVM.LLVMInitializeNativeTarget()

    val context: LLVMContextRef = LLVM.LLVMContextCreate()
    val module = Module(moduleID, context)

    val compiler = Compiler(context, module)
    compiler.compile(program)

    return Right(compiler)
}

private class Compiler(
    override val context: LLVMContextRef,
    override val module: Module
) : LLVMState {
    val builder = module.createBuilder()
    var procedure: LLVMValueRef? = null

    var expressionName = 0

    val void = LLVM.LLVMVoidTypeInContext(context)!!
    val i8 = LLVM.LLVMInt8TypeInContext(context)!!
    val i32 = LLVM.LLVMInt32TypeInContext(context)!!
    val i64 = LLVM.LLVMInt64TypeInContext(context)!!
    val i8P = LLVM.LLVMPointerType(i8, 0)!!

    val structValue = LLVM.LLVMStructCreateNamed(context, "struct.Value")!!
    val structValueP = LLVM.LLVMPointerType(structValue, 0)!!
    val structValuePP = LLVM.LLVMPointerType(structValueP, 0)!!
    val unionAnon = LLVM.LLVMStructCreateNamed(context, "union.anon")!!
    val structPair = LLVM.LLVMStructCreateNamed(context, "struct.Pair")!!
    val structVector = LLVM.LLVMStructCreateNamed(context, "struct.Vector")!!
    val structNativeClosure = LLVM.LLVMStructCreateNamed(context, "struct.NativeClosure")!!
    val structDynamicClosure = LLVM.LLVMStructCreateNamed(context, "struct.DynamicClosure")!!

    val c0i64 = LLVM.LLVMConstInt(i64, 0, 0)!!

    val builtinBuiltinDeclarations = BuiltinDeclarations(module, structValueP, i32, i8P, void)

    init {
        LLVM.LLVMStructSetBody(
            structValue,
            PointerPointer(
                i32,
                unionAnon
            ),
            2,
            0
        )

        LLVM.LLVMStructSetBody(
            unionAnon,
            structNativeClosure,
            1,
            0
        )

        LLVM.LLVMStructSetBody(
            structPair,
            PointerPointer(
                structValue,
                structValue
            ),
            2,
            0
        )

        LLVM.LLVMStructSetBody(
            structVector,
            PointerPointer(
                i32,
                structValuePP
            ),
            2,
            0
        )

        LLVM.LLVMStructSetBody(
            structNativeClosure,
            PointerPointer(
                i8P,
                i32,
                i8P
            ),
            3,
            0
        )

        LLVM.LLVMStructSetBody(
            structDynamicClosure,
            PointerPointer(
                i8P,
                i32,
                structValueP
            ),
            3,
            0
        )
    }

    override fun dispose() {
        builder.dispose()

        LLVM.LLVMContextDispose(context)
    }

    fun compile(program: Program) {
        program.values.forEach {
            module.addGlobal(it, structValueP, LLVM.LLVMConstPointerNull(structValueP), false)
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
            compileProcedure(declaration)
        else
            TODO(declaration.toString())
    }

    private fun compileProcedure(declaration: Procedure) {
        val procedureType = LLVM.LLVMFunctionType(i32, PointerPointer<LLVMTypeRef>(), 0, 0)

        procedure = module.addFunction(declaration.name, procedureType)

        LLVM.LLVMSetFunctionCallConv(procedure, LLVM.LLVMCCallConv)

        val entry = LLVM.LLVMAppendBasicBlockInContext(context, procedure, "entry")
        builder.positionAtEnd(entry)

        declaration.es.forEach {
            compileE(it)
        }

        builder.buildRet(LLVM.LLVMConstInt(i32, 0, 0))
    }

    private fun compileEForce(e: Expression): LLVMValueRef =
        compileE(e) ?: builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.V_NULL, nextName())

    private fun compileE(e: Expression): LLVMValueRef? =
        when (e) {
            is AssignExpression -> {
                builder.buildStore(compileEForce(e.e), module.getNamedGlobal(e.symbol.name)!!)

                null
            }

            is BooleanPExpression ->
                builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.BOOLEANP, listOf(compileEForce(e.es)), nextName())

            is CarExpression ->
                builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.CAR, listOf(compileEForce(e.es)), nextName())

            is CdrExpression ->
                builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.CDR, listOf(compileEForce(e.es)), nextName())

            is EqualsExpression ->
                builtinBuiltinDeclarations.invoke(
                    builder,
                    BuiltinDeclarationEnum.EQUALS,
                    listOf(compileEForce(e.e1), compileEForce(e.e2)),
                    nextName()
                )

            is IfExpression -> {
                val e1op = compileEForce(e.e1)
                val falseOp = builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.V_FALSE, nextName())

                val e1Compare = builder.buildICmp(LLVM.LLVMIntNE, e1op, falseOp, nextName())

                val ifThen = LLVM.LLVMAppendBasicBlockInContext(context, procedure, nextName())
                val ifElse = LLVM.LLVMAppendBasicBlockInContext(context, procedure, nextName())
                val ifEnd = LLVM.LLVMAppendBasicBlockInContext(context, procedure, nextName())

                builder.buildCondBr(e1Compare, ifThen, ifElse)

                builder.positionAtEnd(ifThen)
                val e2op = compileEForce(e.e2)
                builder.buildBr(ifEnd)
                val fromThen = builder.getCurrentBasicBlock()!!

                builder.positionAtEnd(ifElse)
                val e3op = compileEForce(e.e3)
                builder.buildBr(ifEnd)
                val fromElse = builder.getCurrentBasicBlock()!!

                builder.positionAtEnd(ifEnd)
                builder.buildPhi(structValueP, listOf(e2op, e3op), listOf(fromThen, fromElse), nextName())
            }

            is IntegerPExpression ->
                builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.INTEGERP, listOf(compileEForce(e.es)), nextName())

            is LessThanExpression ->
                builtinBuiltinDeclarations.invoke(
                    builder,
                    BuiltinDeclarationEnum.LESS_THAN,
                    listOf(compileEForce(e.e1), compileEForce(e.e2)),
                    nextName()
                )

            is LiteralBool ->
                builtinBuiltinDeclarations.invoke(
                    builder,
                    if (e == LiteralBool.TRUE) BuiltinDeclarationEnum.V_TRUE else BuiltinDeclarationEnum.V_FALSE,
                    nextName()
                )

            is LiteralInt ->
                builtinBuiltinDeclarations.invoke(
                    builder,
                    BuiltinDeclarationEnum.FROM_LITERAL_INT,
                    listOf(LLVM.LLVMConstInt(i32, e.value.toLong(), 0)),
                    nextName()
                )

            is LiteralString -> {
                val globalStringName = module.addGlobal(nextName(), LLVM.LLVMArrayType(i8, e.value.length + 1))
                LLVM.LLVMSetInitializer(globalStringName, LLVM.LLVMConstStringInContext(context, BytePointer(e.value), e.value.length, 0))

                builtinBuiltinDeclarations.invoke(
                    builder,
                    BuiltinDeclarationEnum.FROM_LITERAL_STRING,
                    listOf(LLVM.LLVMConstInBoundsGEP(globalStringName, PointerPointer(c0i64, c0i64), 2)),
                    nextName()
                )
            }

            is LiteralUnit ->
                builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.V_NULL, nextName())

            is MinusExpression ->
                compileOperator(e.es, 0, BuiltinDeclarationEnum.MINUS, true)

            is NullPExpression ->
                builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.NULLP, listOf(compileEForce(e.es)), nextName())

            is PairExpression ->
                builtinBuiltinDeclarations.invoke(
                    builder,
                    BuiltinDeclarationEnum.PAIR,
                    listOf(compileEForce(e.car), compileEForce(e.cdr)),
                    nextName()
                )

            is PairPExpression ->
                builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.PAIRP, listOf(compileEForce(e.es)), nextName())

            is PlusExpression ->
                compileOperator(e.es, 0, BuiltinDeclarationEnum.PLUS, false)

            is PrintlnExpression -> {
                e.es.forEach {
                    val op = compileE(it)

                    if (op != null) {
                        builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.PRINT_VALUE, listOf(op), "")
                    }
                }

                builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.PRINT_NEWLINE, listOf(), "")

                null
            }

            is SlashExpression ->
                compileOperator(e.es, 1, BuiltinDeclarationEnum.DIVIDE, true)

            is StarExpression ->
                compileOperator(e.es, 1, BuiltinDeclarationEnum.MULTIPLY, false)

            is StringPExpression ->
                builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.STRINGP, listOf(compileEForce(e.es)), nextName())

            is SymbolReferenceExpression ->
                builder.buildLoad(module.getNamedGlobal(e.symbol.name)!!, nextName())

            else ->
                TODO(e.toString())
//                return builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.V_NULL, nextName())
        }

    private fun nextName(): String {
        val result = "_v$expressionName"
        expressionName += 1
        return result
    }

    private fun compileOperator(es: Expressions, unitValue: Int, operator: BuiltinDeclarationEnum, explicitFirst: Boolean): LLVMValueRef? {
        val ops = es.mapNotNull { compileE(it) }

        return if (ops.isEmpty())
            compileE(LiteralInt(unitValue))
        else if (explicitFirst && ops.size == 1)
            builtinBuiltinDeclarations.invoke(builder, operator, listOf(compileEForce(LiteralInt(unitValue)), ops[0]), nextName())
        else
            ops.drop(1).fold(ops[0]) { op1, op2 -> builtinBuiltinDeclarations.invoke(builder, operator, listOf(op1, op2), nextName()) }
    }
}

private enum class BuiltinDeclarationEnum {
    BOOLEANP, CAR, CDR, DIVIDE, EQUALS, FROM_LITERAL_INT, FROM_LITERAL_STRING,
    INTEGERP, LESS_THAN, MINUS, MULTIPLY, NULLP, PAIR, PAIRP, PLUS,
    PRINT_VALUE, PRINT_NEWLINE, STRINGP, V_TRUE,
    V_FALSE, V_NULL
}

private class BuiltinDeclarations(val module: Module, structValueP: LLVMTypeRef, i32: LLVMTypeRef, i8P: LLVMTypeRef, void: LLVMTypeRef) {
    private val declarations = mapOf(
        Pair(BuiltinDeclarationEnum.BOOLEANP, BuiltinDeclaration("_booleanp", listOf(structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.CAR, BuiltinDeclaration("_pair_car", listOf(structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.CDR, BuiltinDeclaration("_pair_cdr", listOf(structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.DIVIDE, BuiltinDeclaration("_divide", listOf(structValueP, structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.EQUALS, BuiltinDeclaration("_equals", listOf(structValueP, structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.FROM_LITERAL_INT, BuiltinDeclaration("_from_literal_int", listOf(i32), structValueP)),
        Pair(BuiltinDeclarationEnum.FROM_LITERAL_STRING, BuiltinDeclaration("_from_literal_string", listOf(i8P), structValueP)),
        Pair(BuiltinDeclarationEnum.INTEGERP, BuiltinDeclaration("_integerp", listOf(structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.LESS_THAN, BuiltinDeclaration("_less_than", listOf(structValueP, structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.MINUS, BuiltinDeclaration("_minus", listOf(structValueP, structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.MULTIPLY, BuiltinDeclaration("_multiply", listOf(structValueP, structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.NULLP, BuiltinDeclaration("_nullp", listOf(structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.PAIR, BuiltinDeclaration("_mk_pair", listOf(structValueP, structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.PAIRP, BuiltinDeclaration("_pairp", listOf(structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.PLUS, BuiltinDeclaration("_plus", listOf(structValueP, structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.PRINT_VALUE, BuiltinDeclaration("_print_value", listOf(structValueP), void)),
        Pair(BuiltinDeclarationEnum.PRINT_NEWLINE, BuiltinDeclaration("_print_newline", listOf(), void)),
        Pair(BuiltinDeclarationEnum.STRINGP, BuiltinDeclaration("_stringp", listOf(structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.V_TRUE, BuiltinDeclaration("_VTrue", null, structValueP)),
        Pair(BuiltinDeclarationEnum.V_FALSE, BuiltinDeclaration("_VFalse", null, structValueP)),
        Pair(BuiltinDeclarationEnum.V_NULL, BuiltinDeclaration("_VNull", null, structValueP))
    )

    fun get(bip: BuiltinDeclarationEnum): LLVMValueRef {
        val declaration = declarations[bip]!!
        return if (declaration.isProcedure())
            module.getNamedFunction(declaration.name) ?: run {
                val parameters = declaration.parameters!!
                val parameterTypes = pointerPointerOf(parameters)

                module.addFunction(
                    declaration.name,
                    LLVM.LLVMFunctionType(declaration.returnType, parameterTypes, declaration.parameters.size, 0)
                )!!
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
