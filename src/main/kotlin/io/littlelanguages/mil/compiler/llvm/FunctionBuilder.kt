package io.littlelanguages.mil.compiler.llvm

import io.littlelanguages.data.NestedMap
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMBuilderRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class FunctionBuilder(private val context: Context, private val module: Module, private val builder: LLVMBuilderRef, var procedure: LLVMValueRef) {
    private var currentBasicBlock: LLVMBasicBlockRef = appendBasicBlock("entry")
    private var bindings = NestedMap<Any, LLVMValueRef>()

    init {
        positionAtEnd(currentBasicBlock)
    }

    fun buildBr(basicBlock: LLVMBasicBlockRef): LLVMValueRef =
        LLVM.LLVMBuildBr(builder, basicBlock)

    fun buildCall(functionRef: LLVMValueRef, arguments: List<LLVMValueRef?>, name: String = ""): LLVMValueRef =
        LLVM.LLVMBuildCall(
            builder,
            functionRef,
            pointerPointerOf(arguments),
            arguments.size,
            name
        )

    fun buildCallClosure(
        fileName: LLVMValueRef,
        lineNumber: Int,
        closureRef: LLVMValueRef,
        arguments: List<LLVMValueRef?>,
        name: String = ""
    ): LLVMValueRef {
        val numberOfArguments = arguments.size

        return buildCall(
            getNamedFunction("_call_closure_$numberOfArguments", listOf(i8P, i32) + List(1 + numberOfArguments) { structValueP }, structValueP),
            listOf(
                fileName,
                LLVM.LLVMConstInt(i32, lineNumber.toLong(), 0),
                closureRef
            ) + arguments,
            name
        )
    }

    fun buildCondBr(ifOp: LLVMValueRef, thenOp: LLVMBasicBlockRef, elseOp: LLVMBasicBlockRef): LLVMValueRef =
        LLVM.LLVMBuildCondBr(builder, ifOp, thenOp, elseOp)

    fun buildExceptionSignal(
        fileName: LLVMValueRef,
        lineNumber: Int,
        e: LLVMValueRef,
        name: String = ""
    ) {
        buildCall(
            getNamedFunction("_exception_throw", listOf(i8P, i32, structValueP), void),
            listOf(
                fileName,
                LLVM.LLVMConstInt(i32, lineNumber.toLong(), 0),
                e
            ),
            name
        )
    }

    fun buildExceptionTry(
        fileName: LLVMValueRef,
        lineNumber: Int,
        tryProc: LLVMValueRef,
        catchProc: LLVMValueRef,
        name: String = ""
    ): LLVMValueRef =
        buildCall(
            getNamedFunction("_exception_try", listOf(i8P, i32, structValueP, structValueP), structValueP),
            listOf(
                fileName,
                LLVM.LLVMConstInt(i32, lineNumber.toLong(), 0),
                tryProc,
                catchProc
            ),
            name
        )

    fun buildFromDynamicProcedure(functionName: String, numberOfArguments: Int, frameRef: LLVMValueRef, name: String = ""): LLVMValueRef =
        buildCall(
            getNamedFunction("_from_dynamic_procedure", listOf(i8P, i32, structValueP), structValueP),
            listOf(
                LLVM.LLVMConstBitCast(getNamedFunction(functionName, List(numberOfArguments) { structValueP }, structValueP), i8P),
                LLVM.LLVMConstInt(i32, numberOfArguments.toLong(), 0),
                frameRef
            ),
            name
        )

    fun buildFromLiteralInt(n: Int): LLVMValueRef =
        buildCall(
            getNamedFunction("_from_literal_int", listOf(i32), structValueP),
            listOf(LLVM.LLVMConstInt(i32, n.toLong(), 0)),
        )

    fun buildFromNativeProcedure(
        fileName: LLVMValueRef,
        lineNumber: Int,
        functionName: String,
        numberOfArguments: Int,
        name: String = ""
    ): LLVMValueRef =
        buildCall(
            getNamedFunction("_from_native_procedure", listOf(i8P, i32, i8P, i32), structValueP),
            listOf(
                fileName,
                LLVM.LLVMConstInt(i32, lineNumber.toLong(), 0),
                LLVM.LLVMConstBitCast(getNamedFunction(functionName, List(numberOfArguments) { structValueP }, structValueP), i8P),
                LLVM.LLVMConstInt(i32, numberOfArguments.toLong(), 0)
            ),
            name
        )

    fun buildFromNativeVarArgProcedure(functionName: String, name: String = ""): LLVMValueRef =
        buildCall(
            getNamedFunction("_from_native_var_arg_procedure", listOf(i8P), structValueP),
            listOf(
                LLVM.LLVMConstBitCast(getNamedFunction(functionName, listOf(i32), structValueP, true), i8P)
            ),
            name
        )

    fun buildFromNativeVarArgPositionProcedure(fileName: LLVMValueRef, lineNumber: Int, functionName: String, name: String = ""): LLVMValueRef =
        buildCall(
            getNamedFunction("_from_native_var_arg_position_procedure", listOf(i8P, i32, i8P), structValueP),
            listOf(
                fileName,
                LLVM.LLVMConstInt(i32, lineNumber.toLong(), 0),
                LLVM.LLVMConstBitCast(getNamedFunction(functionName, listOf(i32), structValueP, true), i8P)
            ),
            name
        )

    fun buildFromLiteralString(s: String): LLVMValueRef =
        buildCall(
            getNamedFunction("_from_literal_string", listOf(i8P), structValueP),
            listOf(LLVM.LLVMConstInBoundsGEP(addGlobalString(s), PointerPointer(c0i64, c0i64), 2))
        )

    fun buildGetFrameValue(frame: LLVMValueRef, relativeDepth: Int, index: Int, name: String = ""): LLVMValueRef =
        buildCall(
            getNamedFunction("_get_frame_value", listOf(structValueP, i32, i32), structValueP),
            listOf(frame, LLVM.LLVMConstInt(i32, relativeDepth.toLong(), 0), LLVM.LLVMConstInt(i32, index.toLong(), 0)),
            name
        )

    fun buildICmp(op: Int, lhs: LLVMValueRef, rhs: LLVMValueRef, name: String = ""): LLVMValueRef =
        LLVM.LLVMBuildICmp(builder, op, lhs, rhs, name)

    fun buildLoad(valueRef: LLVMValueRef, name: String = ""): LLVMValueRef =
        LLVM.LLVMBuildLoad(builder, valueRef, name)

    private fun buildLoadNamedGlobal(globalName: String, name: String = ""): LLVMValueRef =
        buildLoad(getNamedGlobal(globalName) ?: addGlobal(globalName, structValueP)!!, name)

    fun buildMkFrame(parentFrame: LLVMValueRef, size: Int, name: String = ""): LLVMValueRef =
        buildCall(
            getNamedFunction("_mk_frame", listOf(structValueP, i32), structValueP),
            listOf(parentFrame, LLVM.LLVMConstInt(i32, size.toLong(), 0)!!),
            name
        )

    fun buildPhi(type: LLVMTypeRef, incomingValues: List<LLVMValueRef>, incomingBlocks: List<LLVMBasicBlockRef>, name: String = ""): LLVMValueRef {
        val phi = LLVM.LLVMBuildPhi(builder, type, name)
        LLVM.LLVMAddIncoming(phi, pointerPointerOf(incomingValues), pointerPointerOf(incomingBlocks), incomingValues.size)
        return phi
    }

    fun buildRet(v: LLVMValueRef): LLVMValueRef =
        LLVM.LLVMBuildRet(builder, v)

    fun buildSetFrameValue(frame: LLVMValueRef, index: Int, operand: LLVMValueRef): LLVMValueRef =
        buildCall(
            getNamedFunction("_set_frame_value", listOf(structValueP, i32, i32, structValueP), void),
            listOf(frame, LLVM.LLVMConstInt(i32, 0.toLong(), 0), LLVM.LLVMConstInt(i32, index.toLong(), 0), operand),
            ""
        )

    fun buildStore(v1: LLVMValueRef, v2: LLVMValueRef) {
        LLVM.LLVMBuildStore(builder, v1, v2)
    }

    private fun buildNamedValue(valueName: String, name: String = ""): LLVMValueRef {
        val result = getBindingValue(valueName)

        return if (result == null) {
            val newResult = buildLoadNamedGlobal(valueName, name)
            addBindingToScope(valueName, newResult)
            newResult
        } else
            result
    }

    fun buildVNull(name: String = ""): LLVMValueRef =
        buildNamedValue("_VNull", name)

    fun buildVTrue(name: String = ""): LLVMValueRef =
        buildNamedValue("_VTrue", name)

    fun buildVFalse(name: String = ""): LLVMValueRef =
        buildNamedValue("_VFalse", name)

    fun positionAtEnd(basicBlock: LLVMBasicBlockRef) {
        LLVM.LLVMPositionBuilderAtEnd(builder, basicBlock)
        currentBasicBlock = basicBlock
    }

    fun getCurrentBasicBlock(): LLVMBasicBlockRef =
        currentBasicBlock

    fun getParam(offset: Int): LLVMValueRef =
        LLVM.LLVMGetParam(procedure, offset)

    fun appendBasicBlock(name: String = ""): LLVMBasicBlockRef =
        LLVM.LLVMAppendBasicBlockInContext(context.context, procedure, name)

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

    fun getNamedFunction(name: String, parameterTypes: List<LLVMTypeRef>, resultType: LLVMTypeRef, varArg: Boolean = false): LLVMValueRef =
        getNamedFunction(name) ?: addExternalFunction(name, parameterTypes, resultType, varArg)

    private fun addExternalFunction(name: String, parameterTypes: List<LLVMTypeRef>, resultType: LLVMTypeRef, varArg: Boolean = false): LLVMValueRef =
        module.addExternalFunction(name, parameterTypes, resultType, varArg)

    private fun addGlobalString(value: String): LLVMValueRef =
        module.addGlobalString(value, "")

    fun openScope() =
        bindings.open()

    fun closeScope() =
        bindings.close()

    fun addBindingToScope(key: Any, value: LLVMValueRef) =
        bindings.add(key, value)

    fun getBindingValue(key: Any): LLVMValueRef? =
        bindings.get(key)
//        if (key == "_frame" || key == "_filename") bindings.get(key) else null
}
