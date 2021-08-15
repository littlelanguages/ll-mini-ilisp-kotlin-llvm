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

    val builtinProcedures = Procedures(module, structValueP, i32, i8P, void)

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
        compileE(e) ?: builtinProcedures.invoke(builder, BuiltinProcedure.V_NULL, nextName())

    private fun compileE(e: Expression): LLVMValueRef? {
        when (e) {
            is CarExpression ->
                return builtinProcedures.invoke(builder, BuiltinProcedure.CAR, listOf(compileEForce(e.es)), nextName())

            is MinusExpression ->
                return compileOperator(e.es, 0, BuiltinProcedure.MINUS, true)

            is PlusExpression ->
                return compileOperator(e.es, 0, BuiltinProcedure.PLUS, false)

            is PrintlnExpression -> {
                for (it in e.es) {
                    val op = compileE(it)

                    if (op != null) {
                        builtinProcedures.invoke(builder, BuiltinProcedure.PRINT_VALUE, listOf(op), "")
                    }
                }

                builtinProcedures.invoke(builder, BuiltinProcedure.PRINT_NEWLINE, listOf(), "")

                return null
            }

            is LiteralBool ->
                return builtinProcedures.invoke(builder, if (e == LiteralBool.TRUE) BuiltinProcedure.V_TRUE else BuiltinProcedure.V_FALSE, nextName())

            is LiteralInt ->
                return builtinProcedures.invoke(
                    builder,
                    BuiltinProcedure.FROM_LITERAL_INT,
                    listOf(LLVM.LLVMConstInt(i32, e.value.toLong(), 0)),
                    nextName()
                )

            is LiteralString -> {
                val globalStringName = LLVM.LLVMAddGlobal(module, LLVM.LLVMArrayType(i8, e.value.length + 1), nextName())
                LLVM.LLVMSetInitializer(globalStringName, LLVM.LLVMConstStringInContext(context, BytePointer(e.value), e.value.length, 0))

                return builtinProcedures.invoke(
                    builder,
                    BuiltinProcedure.FROM_LITERAL_STRING,
                    listOf(LLVM.LLVMConstInBoundsGEP(globalStringName, PointerPointer(c0i64, c0i64), 2)),
                    nextName()
                )
            }

            is LiteralUnit ->
                return builtinProcedures.invoke(builder, BuiltinProcedure.V_NULL, nextName())

            is PairExpression ->
                return builtinProcedures.invoke(builder, BuiltinProcedure.PAIR, listOf(compileEForce(e.car), compileEForce(e.cdr)), nextName())

            is SlashExpression ->
                return compileOperator(e.es, 1, BuiltinProcedure.DIVIDE, true)

            is StarExpression ->
                return compileOperator(e.es, 1, BuiltinProcedure.MULTIPLY, false)

            else ->
                TODO(e.toString())
        }
    }

    private fun nextName(): String {
        val result = "v$expressionName"
        expressionName += 1
        return result
    }

    private fun compileOperator(es: Expressions, unitValue: Int, operator: BuiltinProcedure, explicitFirst: Boolean): LLVMValueRef? {
        val ops = es.mapNotNull { compileE(it) }

        return if (ops.isEmpty())
            compileE(LiteralInt(unitValue))
        else if (explicitFirst && ops.size == 1)
            builtinProcedures.invoke(builder, operator, listOf(compileEForce(LiteralInt(unitValue)), ops[0]), nextName())
        else
            ops.drop(1).fold(ops[0]) { op1, op2 -> builtinProcedures.invoke(builder, operator, listOf(op1, op2), nextName()) }
    }
}

private enum class BuiltinProcedure {
    CAR, DIVIDE, FROM_LITERAL_INT, FROM_LITERAL_STRING,
    MINUS, MULTIPLY, PAIR, PLUS,
    PRINT_VALUE, PRINT_NEWLINE, V_TRUE,
    V_FALSE, V_NULL
}

private class Procedures(val module: LLVMModuleRef, structValueP: LLVMTypeRef, i32: LLVMTypeRef, i8P: LLVMTypeRef, void: LLVMTypeRef) {
    private val declarations = mapOf(
        Pair(BuiltinProcedure.CAR, ProcedureDeclaration("_pair_car", listOf(structValueP), structValueP)),
        Pair(BuiltinProcedure.DIVIDE, ProcedureDeclaration("_divide", listOf(structValueP, structValueP), structValueP)),
        Pair(BuiltinProcedure.FROM_LITERAL_INT, ProcedureDeclaration("_from_literal_int", listOf(i32), structValueP)),
        Pair(BuiltinProcedure.FROM_LITERAL_STRING, ProcedureDeclaration("_from_literal_string", listOf(i8P), structValueP)),
        Pair(BuiltinProcedure.MINUS, ProcedureDeclaration("_minus", listOf(structValueP, structValueP), structValueP)),
        Pair(BuiltinProcedure.MULTIPLY, ProcedureDeclaration("_multiply", listOf(structValueP, structValueP), structValueP)),
        Pair(BuiltinProcedure.PAIR, ProcedureDeclaration("_mk_pair", listOf(structValueP, structValueP), structValueP)),
        Pair(BuiltinProcedure.PLUS, ProcedureDeclaration("_plus", listOf(structValueP, structValueP), structValueP)),
        Pair(BuiltinProcedure.PRINT_VALUE, ProcedureDeclaration("_print_value", listOf(structValueP), void)),
        Pair(BuiltinProcedure.PRINT_NEWLINE, ProcedureDeclaration("_print_newline", listOf(), void)),
        Pair(BuiltinProcedure.V_TRUE, ProcedureDeclaration("_VTrue", null, structValueP)),
        Pair(BuiltinProcedure.V_FALSE, ProcedureDeclaration("_VFalse", null, structValueP)),
        Pair(BuiltinProcedure.V_NULL, ProcedureDeclaration("_VNull", null, structValueP))
    )

    fun get(bip: BuiltinProcedure): LLVMValueRef {
        val declaration = declarations[bip]!!
        val namedFunction: LLVMValueRef? = LLVM.LLVMGetNamedFunction(module, declaration.name)

        return if (namedFunction == null) {
            val parameters = declaration.parameters

            if (parameters == null) {
                val result = LLVM.LLVMAddGlobal(module, declaration.returnType, declaration.name)!!
                LLVM.LLVMSetGlobalConstant(result, 1)
                result
            } else {
                val parameterTypes = declaration.parameters.foldIndexed(PointerPointer<LLVMTypeRef>(parameters.size.toLong())) { idx, acc, item ->
                    acc.put(idx.toLong(), item)
                }

                LLVM.LLVMAddFunction(
                    module,
                    declaration.name,
                    LLVM.LLVMFunctionType(declaration.returnType, parameterTypes, declaration.parameters.size, 0)
                )!!
            }
        } else
            namedFunction
    }

    fun invoke(builder: LLVMBuilderRef, bip: BuiltinProcedure, arguments: List<LLVMValueRef>, name: String): LLVMValueRef =
        LLVM.LLVMBuildCall(
            builder, get(bip),
            arguments.foldIndexed(PointerPointer<Pointer>(arguments.size.toLong())) { idx, acc, op -> acc.put(idx.toLong(), op) },
            arguments.size, name
        )

    fun invoke(builder: LLVMBuilderRef, bip: BuiltinProcedure, name: String): LLVMValueRef =
        LLVM.LLVMBuildLoad(builder, get(bip), name)
}

private data class ProcedureDeclaration(val name: String, val parameters: List<LLVMTypeRef>?, val returnType: LLVMTypeRef)
