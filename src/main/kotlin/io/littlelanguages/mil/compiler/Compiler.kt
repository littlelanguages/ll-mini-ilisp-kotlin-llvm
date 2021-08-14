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

    val structValue = LLVM.LLVMStructCreateNamed(context, "struct.Value")!!
    val structValueP = LLVM.LLVMPointerType(structValue, 0)!!
    val structValuePP = LLVM.LLVMPointerType(structValueP, 0)!!
    val unionAnon = LLVM.LLVMStructCreateNamed(context, "union.anon")!!
    val structPair = LLVM.LLVMStructCreateNamed(context, "struct.Pair")!!
    val structVector = LLVM.LLVMStructCreateNamed(context, "struct.Vector")!!
    val structNativeClosure = LLVM.LLVMStructCreateNamed(context, "struct.NativeClosure")!!
    val structDynamicClosure = LLVM.LLVMStructCreateNamed(context, "struct.DynamicClosure")!!

    val void = LLVM.LLVMVoidTypeInContext(context)!!
    val i8 = LLVM.LLVMInt8TypeInContext(context)!!
    val i32 = LLVM.LLVMInt32TypeInContext(context)!!
    val i64 = LLVM.LLVMInt64TypeInContext(context)!!
    val i8P = LLVM.LLVMPointerType(i8, 0)!!

    val c0i64 = LLVM.LLVMConstInt(i64, 0, 0)!!

    val _print_value = LLVM.LLVMAddFunction(module, "_print_value", LLVM.LLVMFunctionType(void, structValueP, 1, 0))!!
    val _print_newline = LLVM.LLVMAddFunction(module, "_print_newline", LLVM.LLVMFunctionType(void, PointerPointer<LLVMTypeRef>(), 0, 0))!!
    val _from_literal_int = LLVM.LLVMAddFunction(module, "_from_literal_int", LLVM.LLVMFunctionType(structValueP, i32, 1, 0))!!
    val _from_literal_string = LLVM.LLVMAddFunction(module, "_from_literal_string", LLVM.LLVMFunctionType(structValueP, i8P, 1, 0))!!

    val _VTrue = LLVM.LLVMAddGlobal(module, structValueP, "_VTrue")!!
    val _VFalse = LLVM.LLVMAddGlobal(module, structValueP, "_VFalse")!!
    val _VNull = LLVM.LLVMAddGlobal(module, structValueP, "_VNull")!!

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

    private fun compileE(e: Expression): LLVMValueRef? {
        when (e) {
            is PrintlnExpression -> {
                for (it in e.es) {
                    val op = compileE(it)

                    if (op != null) {
                        LLVM.LLVMBuildCall(builder, _print_value, PointerPointer<Pointer>(1).put(0, op), 1, "")
                    }
                }

                LLVM.LLVMBuildCall(builder, _print_newline, PointerPointer<Pointer>(0), 0, "")

                return null
            }

            is LiteralBool ->
                return LLVM.LLVMBuildLoad(
                    builder,
                    if (e == LiteralBool.TRUE) _VTrue else _VFalse,
                    nextName()
                )

            is LiteralInt ->
                return LLVM.LLVMBuildCall(
                    builder,
                    _from_literal_int,
                    PointerPointer<Pointer>(1).put(0, LLVM.LLVMConstInt(i32, e.value.toLong(), 0)),
                    1,
                    nextName()
                )

            is LiteralString -> {
                val globalStringName = LLVM.LLVMAddGlobal(module, LLVM.LLVMArrayType(i8, e.value.length + 1), nextName())
                LLVM.LLVMSetInitializer(globalStringName, LLVM.LLVMConstStringInContext(context, BytePointer(e.value), e.value.length, 0))

                return LLVM.LLVMBuildCall(
                    builder,
                    _from_literal_string,
                    PointerPointer<Pointer>(1).put(0, LLVM.LLVMConstInBoundsGEP(globalStringName, PointerPointer(c0i64, c0i64), 2)),
                    1,
                    nextName()
                )
            }

            else ->
                TODO(e.toString())
        }
    }

    private fun nextName(): String {
        val result = "v$expressionName"
        expressionName += 1
        return result
    }
}