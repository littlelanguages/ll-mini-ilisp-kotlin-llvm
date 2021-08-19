package io.littlelanguages.mil.compiler.llvm

import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class Builder(private val builder: LLVMBuilderRef) {
    fun buildBr(basicBlock: LLVMBasicBlockRef): LLVMValueRef =
        LLVM.LLVMBuildBr(builder, basicBlock)

    fun buildCall(
        functionRef: LLVMValueRef,
        arguments: List<LLVMValueRef>,
        name: String
    ): LLVMValueRef =
        LLVM.LLVMBuildCall(
            builder,
            functionRef,
            arguments.foldIndexed(PointerPointer<Pointer>(arguments.size.toLong())) { idx, acc, op -> acc.put(idx.toLong(), op) },
            arguments.size,
            name
        )

    fun buildCondBr(
        ifOp: LLVMValueRef,
        thenOp: LLVMBasicBlockRef,
        elseOp: LLVMBasicBlockRef
    ): LLVMValueRef =
        LLVM.LLVMBuildCondBr(builder, ifOp, thenOp, elseOp)

    fun buildICmp(
        op: Int,
        lhs: LLVMValueRef, rhs: LLVMValueRef,
        name: String
    ): LLVMValueRef =
        LLVM.LLVMBuildICmp(builder, op, lhs, rhs, name)

    fun buildLoad(
        valueRef: LLVMValueRef,
        name: String
    ): LLVMValueRef =
        LLVM.LLVMBuildLoad(builder, valueRef, name)

    fun buildPhi(type: LLVMTypeRef, name: String): LLVMValueRef =
        LLVM.LLVMBuildPhi(builder, type, name)

    fun buildRet(v: LLVMValueRef): LLVMValueRef =
        LLVM.LLVMBuildRet(builder, v)

    fun positionAtEnd(basicBlock: LLVMBasicBlockRef) {
        LLVM.LLVMPositionBuilderAtEnd(builder, basicBlock)
    }

    fun dispose() {
        LLVM.LLVMDisposeBuilder(builder)
    }
}