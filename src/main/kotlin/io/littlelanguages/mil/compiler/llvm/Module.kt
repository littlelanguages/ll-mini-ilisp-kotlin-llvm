package io.littlelanguages.mil.compiler.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class Module(moduleID: String, private var context: Context) {
    private val module = LLVM.LLVMModuleCreateWithNameInContext(moduleID, context.context)
    private val builder = LLVM.LLVMCreateBuilderInContext(context.context)

    private var expressionName = 0

    fun dispose() {
        LLVM.LLVMDisposeBuilder(builder)
    }

    fun getNamedFunction(name: String): LLVMValueRef? =
        LLVM.LLVMGetNamedFunction(module, name)

    fun getNamedGlobal(name: String): LLVMValueRef? =
        LLVM.LLVMGetNamedGlobal(module, name)

    fun addExternalFunction(name: String, parameterTypes: List<LLVMTypeRef>, resultType: LLVMTypeRef): LLVMValueRef =
        LLVM.LLVMAddFunction(
            module,
            name,
            functionType(parameterTypes, resultType)
        )

    fun addFunction(name: String, parameterTypes: List<LLVMTypeRef>, resultType: LLVMTypeRef): FunctionBuilder {
        val functionBuilder = FunctionBuilder(
            context,
            this,
            builder,
            LLVM.LLVMAddFunction(
                module,
                name,
                functionType(parameterTypes, resultType)
            )
        )

        LLVM.LLVMSetFunctionCallConv(functionBuilder.procedure, LLVM.LLVMCCallConv)

        return functionBuilder
    }

    private fun functionType(parameterTypes: List<LLVMTypeRef>, resultType: LLVMTypeRef): LLVMTypeRef =
        LLVM.LLVMFunctionType(
            resultType,
            pointerPointerOf(parameterTypes),
            parameterTypes.size,
            0
        )

    fun addGlobal(name: String, type: LLVMTypeRef, global: Boolean = true): LLVMValueRef? {
        val result = LLVM.LLVMAddGlobal(module, type, name)

        if (result != null && global) {
            LLVM.LLVMSetGlobalConstant(result, 1)
        }

        return result
    }

    fun addGlobal(name: String, type: LLVMTypeRef, init: LLVMValueRef, global: Boolean = true): LLVMValueRef {
        val result = LLVM.LLVMAddGlobal(module, type, name)!!

        if (global) {
            LLVM.LLVMSetGlobalConstant(result, 1)
        }
        LLVM.LLVMSetInitializer(result, init)

        return result
    }

    fun verify(): VerifyResult {
        val error = BytePointer()

        return if (LLVM.LLVMVerifyModule(module, LLVM.LLVMPrintMessageAction, error) == 0)
            VerifySuccess()
        else {
            val message = error.string
            LLVM.LLVMDisposeMessage(error)
            VerifyError(message)
        }
    }

    fun writeBitcodeToFile(fileName: String) {
        LLVM.LLVMWriteBitcodeToFile(module, fileName)
    }

    override fun toString(): String =
        LLVM.LLVMPrintModuleToString(module).string

    fun addGlobalString(value: String, name: String): LLVMValueRef {
        val globalStringName = addGlobal(name, LLVM.LLVMArrayType(i8, value.length + 1))
        LLVM.LLVMSetInitializer(globalStringName, LLVM.LLVMConstStringInContext(context.context, BytePointer(value), value.length, 0))

        return globalStringName!!
    }

    fun nextName(): String {
        val result = "_v$expressionName"
        expressionName += 1
        return result
    }

    val void get() = context.void
    val structValueP get() = context.structValueP
    val i8 get() = context.i8
    val i8P get() = context.i8P
    val i32 get() = context.i32
    val c0i64 get() = context.c0i64
}


interface VerifyResult

class VerifySuccess : VerifyResult

class VerifyError(val message: String) : VerifyResult

fun <T : Pointer?> pointerPointerOf(elements: List<T>): PointerPointer<T> {
    val result = PointerPointer<T>(elements.size.toLong())

    for (index in elements.indices) {
        result.put(index.toLong(), elements[index])
    }

    return result
}
