package io.littlelanguages.mil.compiler.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class Builder(private val context: Context, private val module: Module, private val builder: LLVMBuilderRef, var procedure: LLVMValueRef) {
    private var currentBasicBlock: LLVMBasicBlockRef = appendBasicBlock("entry")

    init {
        positionAtEnd(currentBasicBlock)
    }

    fun buildBr(basicBlock: LLVMBasicBlockRef): LLVMValueRef =
        LLVM.LLVMBuildBr(builder, basicBlock)

    fun buildCall(functionRef: LLVMValueRef, arguments: List<LLVMValueRef?>, name: String? = null): LLVMValueRef =
        LLVM.LLVMBuildCall(
            builder,
            functionRef,
            pointerPointerOf(arguments),
            arguments.size,
            name ?: nextName()
        )

    fun buildCondBr(ifOp: LLVMValueRef, thenOp: LLVMBasicBlockRef, elseOp: LLVMBasicBlockRef): LLVMValueRef =
        LLVM.LLVMBuildCondBr(builder, ifOp, thenOp, elseOp)

    fun buildFromLiteralInt(n: Int): LLVMValueRef =
        buildCall(
            getNamedFunction("_from_literal_int", listOf(i32), structValueP),
            listOf(LLVM.LLVMConstInt(i32, n.toLong(), 0)),
        )

    fun buildFromLiteralString(s: String): LLVMValueRef =
        buildCall(
            getNamedFunction("_from_literal_string", listOf(i8P), structValueP),
            listOf(LLVM.LLVMConstInBoundsGEP(addGlobalString(s, nextName()), PointerPointer(c0i64, c0i64), 2))
        )

    fun buildICmp(op: Int, lhs: LLVMValueRef, rhs: LLVMValueRef, name: String? = null): LLVMValueRef =
        LLVM.LLVMBuildICmp(builder, op, lhs, rhs, name ?: nextName())

    fun buildLoad(valueRef: LLVMValueRef, name: String? = null): LLVMValueRef =
        LLVM.LLVMBuildLoad(builder, valueRef, name ?: nextName())

    fun buildLoadNamedGlobal(globalName: String, name: String? = null): LLVMValueRef =
        buildLoad(getNamedGlobal(globalName) ?: addGlobal(globalName, structValueP)!!, name)

    fun buildPhi(type: LLVMTypeRef, incomingValues: List<LLVMValueRef>, incomingBlocks: List<LLVMBasicBlockRef>, name: String? = null): LLVMValueRef {
        val phi = LLVM.LLVMBuildPhi(builder, type, name ?: nextName())
        LLVM.LLVMAddIncoming(phi, pointerPointerOf(incomingValues), pointerPointerOf(incomingBlocks), incomingValues.size)
        return phi
    }

    fun buildPrintNewline(): LLVMValueRef? {
        buildCall(
            getNamedFunction("_print_newline", listOf(), void),
            listOf(),
            ""
        )

        return null
    }

    fun buildPrintValue(value: LLVMValueRef?): LLVMValueRef? {
        if (value != null)
            buildCall(
                getNamedFunction("_print_value", listOf(structValueP), void),
                listOf(value),
                ""
            )

        return null
    }

    fun buildRet(v: LLVMValueRef): LLVMValueRef =
        LLVM.LLVMBuildRet(builder, v)

    fun buildStore(v1: LLVMValueRef, v2: LLVMValueRef) {
        LLVM.LLVMBuildStore(builder, v1, v2)
    }

    fun buildVNull(name: String? = null): LLVMValueRef =
        buildLoadNamedGlobal("_VNull", name)

    fun buildVTrue(name: String? = null): LLVMValueRef =
        buildLoadNamedGlobal("_VTrue", name)

    fun buildVFalse(name: String? = null): LLVMValueRef =
        buildLoadNamedGlobal("_VFalse", name)

    fun positionAtEnd(basicBlock: LLVMBasicBlockRef) {
        LLVM.LLVMPositionBuilderAtEnd(builder, basicBlock)
        currentBasicBlock = basicBlock
    }

    fun getCurrentBasicBlock(): LLVMBasicBlockRef =
        currentBasicBlock

    fun getParam(offset: Int): LLVMValueRef =
        LLVM.LLVMGetParam(procedure, offset)

    fun appendBasicBlock(name: String? = null): LLVMBasicBlockRef =
        LLVM.LLVMAppendBasicBlockInContext(context.context, procedure, name ?: nextName())

    private fun nextName(): String =
        module.nextName()

    val void get() = context.void
    val structValueP get() = context.structValueP
    val i8 get() = context.i8
    val i8P get() = context.i8P
    val i32 get() = context.i32
    val c0i64 get() = context.c0i64

    private fun addGlobal(name: String, type: LLVMTypeRef): LLVMValueRef? =
        module.addGlobal(name, type)

    fun getNamedGlobal(name: String): LLVMValueRef? =
        module.getNamedGlobal(name)

    fun getNamedFunction(name: String): LLVMValueRef? =
        module.getNamedFunction(name)

    private fun getNamedFunction(name: String, parameterTypes: List<LLVMTypeRef>, resultType: LLVMTypeRef): LLVMValueRef =
        getNamedFunction(name) ?: addExternalFunction(name, parameterTypes, resultType)

    fun addExternalFunction(name: String, parameterTypes: List<LLVMTypeRef>, resultType: LLVMTypeRef): LLVMValueRef =
        module.addExternalFunction(name, parameterTypes, resultType)

    private fun addGlobalString(value: String, name: String): LLVMValueRef =
        module.addGlobalString(value, name)
}

