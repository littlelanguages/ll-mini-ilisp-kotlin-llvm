package io.littlelanguages.mil.compiler.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class Module(moduleID: String, private var context: LLVMContextRef) {
    private val module = LLVM.LLVMModuleCreateWithNameInContext(moduleID, context)

    fun getNamedFunction(name: String): LLVMValueRef? =
        LLVM.LLVMGetNamedFunction(module, name)

    fun getNamedGlobal(name: String): LLVMValueRef? =
        LLVM.LLVMGetNamedGlobal(module, name)

    fun addFunction(name: String, llvmFunctionType: LLVMTypeRef): LLVMValueRef? =
        LLVM.LLVMAddFunction(
            module,
            name,
            llvmFunctionType
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

    fun createBuilder(): Builder =
        Builder(LLVM.LLVMCreateBuilderInContext(context))

    override fun toString(): String =
        LLVM.LLVMPrintModuleToString(module).string
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
