package io.littlelanguages.mil.compiler

import io.littlelanguages.data.Either
import io.littlelanguages.data.Right
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.dynamic.tst.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
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
    override val declarations = mutableMapOf<String, LLVMValueRef>()

    override fun dispose() {
        LLVM.LLVMContextDispose(context)
    }

    fun compile(program: Program) {
        for (declaration in program.declarations) {
            compile(declaration)
        }

        val error = BytePointer()
        if (LLVM.LLVMVerifyModule(module, LLVM.LLVMPrintMessageAction, error) != 0) {
            println(error.string)
            LLVM.LLVMDisposeMessage(error)
            TODO("Not yet implemented")
        }
    }

    private fun compile(declaration: Declaration) {
        if (declaration is Procedure)
            compileProcedure(declaration)
        else
            TODO("Not yet implemented")
    }

    private fun compileProcedure(declaration: Procedure) {
        val builder: LLVMBuilderRef = LLVM.LLVMCreateBuilderInContext(context)
        val i32Type = LLVM.LLVMInt32TypeInContext(context)
        val procedureType = LLVM.LLVMFunctionType(i32Type, i32Type,  /* argumentCount */1,  /* isVariadic */0)
        val procedure = LLVM.LLVMAddFunction(module, declaration.name, procedureType)
        declarations[declaration.name] = procedure

        LLVM.LLVMSetFunctionCallConv(procedure, LLVM.LLVMCCallConv)

        val entry = LLVM.LLVMAppendBasicBlockInContext(context, procedure, "entry")
        LLVM.LLVMPositionBuilderAtEnd(builder, entry)

        for (e in declaration.es) {
            compileE(e)
        }
        LLVM.LLVMBuildRet(builder, LLVM.LLVMConstInt(i32Type, 0,  /* signExtend */0))

        LLVM.LLVMDisposeBuilder(builder)
    }

    private fun compileE(e: Expression): LLVMValueRef? {
        when (e) {
            is PrintlnExpression ->
                return null

            else ->
                TODO(e.toString())
        }
    }
}