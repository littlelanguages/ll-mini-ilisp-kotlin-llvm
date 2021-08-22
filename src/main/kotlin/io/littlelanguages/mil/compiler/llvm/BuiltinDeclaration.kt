package io.littlelanguages.mil.compiler.llvm

import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef

enum class BuiltinDeclarationEnum {
    FROM_LITERAL_INT, FROM_LITERAL_STRING,
    V_TRUE,
    V_FALSE, V_NULL
}

class BuiltinDeclarations(private val module: Module) {
    private val declarations = mapOf(
        Pair(BuiltinDeclarationEnum.FROM_LITERAL_INT, BuiltinDeclaration("_from_literal_int", listOf(module.i32), module.structValueP)),
        Pair(BuiltinDeclarationEnum.FROM_LITERAL_STRING, BuiltinDeclaration("_from_literal_string", listOf(module.i8P), module.structValueP)),
        Pair(BuiltinDeclarationEnum.V_TRUE, BuiltinDeclaration("_VTrue", null, module.structValueP)),
        Pair(BuiltinDeclarationEnum.V_FALSE, BuiltinDeclaration("_VFalse", null, module.structValueP)),
        Pair(BuiltinDeclarationEnum.V_NULL, BuiltinDeclaration("_VNull", null, module.structValueP))
    )

    fun get(bip: BuiltinDeclarationEnum): LLVMValueRef {
        val declaration = declarations[bip]!!
        return if (declaration.isProcedure())
            module.getNamedFunction(declaration.name) ?: module.addExternalFunction(
                declaration.name,
                declaration.parameters!!,
                declaration.returnType
            )
        else
            module.getNamedGlobal(declaration.name) ?: module.addGlobal(declaration.name, declaration.returnType)!!
    }

    fun invoke(builder: Builder, bip: BuiltinDeclarationEnum, arguments: List<LLVMValueRef>, name: String): LLVMValueRef =
        builder.buildCall(get(bip), arguments, name)

    fun invoke(builder: Builder, bip: BuiltinDeclarationEnum, name: String): LLVMValueRef =
        builder.buildLoad(get(bip), name)
}

data class BuiltinDeclaration(val name: String, val parameters: List<LLVMTypeRef>?, val returnType: LLVMTypeRef) {
    fun isProcedure() =
        parameters != null
}