package io.littlelanguages.mil.compiler.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.global.LLVM

class Context(val triple: String) {
    init {
        LLVM.LLVMInitializeCore(LLVM.LLVMGetGlobalPassRegistry())
        LLVM.LLVMLinkInMCJIT()
        LLVM.LLVMInitializeNativeAsmPrinter()
        LLVM.LLVMInitializeNativeAsmParser()
        LLVM.LLVMInitializeNativeTarget()
    }

    val context: LLVMContextRef = LLVM.LLVMContextCreate()

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

    fun dispose() {
        LLVM.LLVMContextDispose(context)
    }

    fun module(moduleID: String) =
        Module(moduleID, this)
}

fun targetTriple(): String {
    val llvmGetDefaultTargetTriple = LLVM.LLVMGetDefaultTargetTriple()
    val result = llvmGetDefaultTargetTriple.string
    LLVM.LLVMDisposeMessage(llvmGetDefaultTargetTriple)

    return when (result) {
        "x86_64-apple-darwin20.6.0" ->
            "x86_64-apple-macosx11.0.0"
        
        else ->
            result
    }
}