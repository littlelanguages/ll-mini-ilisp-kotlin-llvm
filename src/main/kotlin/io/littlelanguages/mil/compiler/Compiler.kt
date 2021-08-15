package io.littlelanguages.mil.compiler

import io.littlelanguages.data.Either
import io.littlelanguages.data.Right
import io.littlelanguages.mil.CompilationError
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.dynamic.tst.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM

interface LLVMState {
    val context: LLVMContextRef
    val module: LLVMModuleRef
    val declarations: Map<String, LLVMValueRef>

    fun dispose()
}

fun compile(moduleID: String, program: Program): Either<List<Errors>, LLVMState> {
    LLVM.LLVMInitializeCore(LLVM.LLVMGetGlobalPassRegistry())
    LLVM.LLVMLinkInMCJIT()
    LLVM.LLVMInitializeNativeAsmPrinter()
    LLVM.LLVMInitializeNativeAsmParser()
    LLVM.LLVMInitializeNativeTarget()

    val context: LLVMContextRef = LLVM.LLVMContextCreate()
    val module: LLVMModuleRef = LLVM.LLVMModuleCreateWithNameInContext(moduleID, context)

    val compiler = Compiler(context, module)
    compiler.compile(program)

    return Right(compiler)
}

private class Compiler(
    override val context: LLVMContextRef,
    override val module: LLVMModuleRef
) : LLVMState {
    val builder: LLVMBuilderRef = LLVM.LLVMCreateBuilderInContext(context)

    override val declarations = mutableMapOf<String, LLVMValueRef>()
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
        LLVM.LLVMDisposeBuilder(builder)
        LLVM.LLVMContextDispose(context)
    }

    fun compile(program: Program) {
        for (declaration in program.declarations) {
            compile(declaration)
        }

        val error = BytePointer()
        if (LLVM.LLVMVerifyModule(module, LLVM.LLVMPrintMessageAction, error) != 0) {
            val message = error.string
            LLVM.LLVMDisposeMessage(error)
            throw CompilationError(message)
        }
    }

    private fun compile(declaration: Declaration) {
        if (declaration is Procedure)
            compileProcedure(declaration)
        else
            TODO("Not yet implemented")
    }

    private fun compileProcedure(declaration: Procedure) {
        val procedureType = LLVM.LLVMFunctionType(i32, PointerPointer<LLVMTypeRef>(), 0, 0)
        val procedure = LLVM.LLVMAddFunction(module, declaration.name, procedureType)

        declarations[declaration.name] = procedure

        LLVM.LLVMSetFunctionCallConv(procedure, LLVM.LLVMCCallConv)

        val entry = LLVM.LLVMAppendBasicBlockInContext(context, procedure, "entry")
        LLVM.LLVMPositionBuilderAtEnd(builder, entry)

        for (e in declaration.es) {
            compileE(e)
        }

        LLVM.LLVMBuildRet(builder, LLVM.LLVMConstInt(i32, 0, 0))
    }

    private fun compileEForce(e: Expression): LLVMValueRef =
        compileE(e) ?: builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.V_NULL, nextName())

    private fun compileE(e: Expression): LLVMValueRef? {
        when (e) {
            is BooleanPExpression ->
                return builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.BOOLEANP, listOf(compileEForce(e.es)), nextName())

            is CarExpression ->
                return builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.CAR, listOf(compileEForce(e.es)), nextName())

            is CdrExpression ->
                return builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.CDR, listOf(compileEForce(e.es)), nextName())

            is IntegerPExpression ->
                return builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.INTEGERP, listOf(compileEForce(e.es)), nextName())

            is LiteralBool ->
                return builtinBuiltinDeclarations.invoke(builder, if (e == LiteralBool.TRUE) BuiltinDeclarationEnum.V_TRUE else BuiltinDeclarationEnum.V_FALSE, nextName())

            is LiteralInt ->
                return builtinBuiltinDeclarations.invoke(
                    builder,
                    BuiltinDeclarationEnum.FROM_LITERAL_INT,
                    listOf(LLVM.LLVMConstInt(i32, e.value.toLong(), 0)),
                    nextName()
                )

            is LiteralString -> {
                val globalStringName = LLVM.LLVMAddGlobal(module, LLVM.LLVMArrayType(i8, e.value.length + 1), nextName())
                LLVM.LLVMSetInitializer(globalStringName, LLVM.LLVMConstStringInContext(context, BytePointer(e.value), e.value.length, 0))

                return builtinBuiltinDeclarations.invoke(
                    builder,
                    BuiltinDeclarationEnum.FROM_LITERAL_STRING,
                    listOf(LLVM.LLVMConstInBoundsGEP(globalStringName, PointerPointer(c0i64, c0i64), 2)),
                    nextName()
                )
            }

            is LiteralUnit ->
                return builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.V_NULL, nextName())

            is MinusExpression ->
                return compileOperator(e.es, 0, BuiltinDeclarationEnum.MINUS, true)

            is NullPExpression ->
                return builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.NULLP, listOf(compileEForce(e.es)), nextName())

            is PairExpression ->
                return builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.PAIR, listOf(compileEForce(e.car), compileEForce(e.cdr)), nextName())

            is PairPExpression ->
                return builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.PAIRP, listOf(compileEForce(e.es)), nextName())

            is PlusExpression ->
                return compileOperator(e.es, 0, BuiltinDeclarationEnum.PLUS, false)

            is PrintlnExpression -> {
                for (it in e.es) {
                    val op = compileE(it)

                    if (op != null) {
                        builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.PRINT_VALUE, listOf(op), "")
                    }
                }

                builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.PRINT_NEWLINE, listOf(), "")

                return null
            }

            is SlashExpression ->
                return compileOperator(e.es, 1, BuiltinDeclarationEnum.DIVIDE, true)

            is StarExpression ->
                return compileOperator(e.es, 1, BuiltinDeclarationEnum.MULTIPLY, false)

            is StringPExpression ->
                return builtinBuiltinDeclarations.invoke(builder, BuiltinDeclarationEnum.STRINGP, listOf(compileEForce(e.es)), nextName())

            else ->
                TODO(e.toString())
        }
    }

    private fun nextName(): String {
        val result = "v$expressionName"
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
    BOOLEANP, CAR, CDR, DIVIDE, FROM_LITERAL_INT, FROM_LITERAL_STRING,
    INTEGERP, MINUS, MULTIPLY, NULLP, PAIR, PAIRP, PLUS,
    PRINT_VALUE, PRINT_NEWLINE, STRINGP, V_TRUE,
    V_FALSE, V_NULL
}

private class BuiltinDeclarations(val module: LLVMModuleRef, structValueP: LLVMTypeRef, i32: LLVMTypeRef, i8P: LLVMTypeRef, void: LLVMTypeRef) {
    private val declarations = mapOf(
        Pair(BuiltinDeclarationEnum.BOOLEANP, BuiltinDeclaration("_booleanp", listOf(structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.CAR, BuiltinDeclaration("_pair_car", listOf(structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.CDR, BuiltinDeclaration("_pair_cdr", listOf(structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.DIVIDE, BuiltinDeclaration("_divide", listOf(structValueP, structValueP), structValueP)),
        Pair(BuiltinDeclarationEnum.FROM_LITERAL_INT, BuiltinDeclaration("_from_literal_int", listOf(i32), structValueP)),
        Pair(BuiltinDeclarationEnum.FROM_LITERAL_STRING, BuiltinDeclaration("_from_literal_string", listOf(i8P), structValueP)),
        Pair(BuiltinDeclarationEnum.INTEGERP, BuiltinDeclaration("_integerp", listOf(structValueP), structValueP)),
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
        val namedFunction: LLVMValueRef? =
            if (declaration.isProcedure())
                LLVM.LLVMGetNamedFunction(module, declaration.name)
            else
                LLVM.LLVMGetNamedGlobal(module, declaration.name)

        return namedFunction
            ?: if (declaration.isProcedure()) {
                val parameters = declaration.parameters!!
                val parameterTypes = declaration.parameters.foldIndexed(PointerPointer<LLVMTypeRef>(parameters.size.toLong())) { idx, acc, item ->
                    acc.put(idx.toLong(), item)
                }

                LLVM.LLVMAddFunction(
                    module,
                    declaration.name,
                    LLVM.LLVMFunctionType(declaration.returnType, parameterTypes, declaration.parameters.size, 0)
                )!!
            } else {
                val result = LLVM.LLVMAddGlobal(module, declaration.returnType, declaration.name)!!
                LLVM.LLVMSetGlobalConstant(result, 1)
                result
            }
    }

    fun invoke(builder: LLVMBuilderRef, bip: BuiltinDeclarationEnum, arguments: List<LLVMValueRef>, name: String): LLVMValueRef =
        LLVM.LLVMBuildCall(
            builder, get(bip),
            arguments.foldIndexed(PointerPointer<Pointer>(arguments.size.toLong())) { idx, acc, op -> acc.put(idx.toLong(), op) },
            arguments.size, name
        )

    fun invoke(builder: LLVMBuilderRef, bip: BuiltinDeclarationEnum, name: String): LLVMValueRef =
        LLVM.LLVMBuildLoad(builder, get(bip), name)
}

private data class BuiltinDeclaration(val name: String, val parameters: List<LLVMTypeRef>?, val returnType: LLVMTypeRef) {
    fun isProcedure() =
        parameters != null
}
