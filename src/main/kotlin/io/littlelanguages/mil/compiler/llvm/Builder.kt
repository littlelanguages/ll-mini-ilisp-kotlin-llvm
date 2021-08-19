package io.littlelanguages.mil.compiler.llvm

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class Builder(private val builder: LLVMBuilderRef) {
    private var currentBasicBlock: LLVMBasicBlockRef? = null

    fun buildBr(basicBlock: LLVMBasicBlockRef): LLVMValueRef =
        LLVM.LLVMBuildBr(builder, basicBlock)

    fun buildCall(functionRef: LLVMValueRef, arguments: List<LLVMValueRef>, name: String): LLVMValueRef =
        LLVM.LLVMBuildCall(
            builder,
            functionRef,
            pointerPointerOf(arguments),
            arguments.size,
            name
        )

    fun buildCondBr(ifOp: LLVMValueRef, thenOp: LLVMBasicBlockRef, elseOp: LLVMBasicBlockRef): LLVMValueRef =
        LLVM.LLVMBuildCondBr(builder, ifOp, thenOp, elseOp)

    fun buildICmp(op: Int, lhs: LLVMValueRef, rhs: LLVMValueRef, name: String): LLVMValueRef =
        LLVM.LLVMBuildICmp(builder, op, lhs, rhs, name)

    fun buildLoad(valueRef: LLVMValueRef, name: String): LLVMValueRef =
        LLVM.LLVMBuildLoad(builder, valueRef, name)

    fun buildPhi(type: LLVMTypeRef, name: String): LLVMValueRef =
        LLVM.LLVMBuildPhi(builder, type, name)

    fun buildRet(v: LLVMValueRef): LLVMValueRef =
        LLVM.LLVMBuildRet(builder, v)

    fun positionAtEnd(basicBlock: LLVMBasicBlockRef) {
        LLVM.LLVMPositionBuilderAtEnd(builder, basicBlock)
        currentBasicBlock = basicBlock
    }

    fun getCurrentBasicBlock(): LLVMBasicBlockRef? =
        currentBasicBlock

    fun dispose() {
        LLVM.LLVMDisposeBuilder(builder)
    }
}