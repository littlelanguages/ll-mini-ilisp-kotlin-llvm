package io.littlelanguages.mil.compiler.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class Module(moduleID: String, context: LLVMContextRef) {
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
}


interface VerifyResult

class VerifySuccess : VerifyResult

class VerifyError(val message: String) : VerifyResult