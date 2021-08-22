package io.littlelanguages.mil.compiler.llvm

import io.littlelanguages.mil.compiler.llvm.Builder
import io.littlelanguages.mil.compiler.llvm.Module
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef

enum class BuiltinDeclarationEnum {
    DIVIDE, EQUALS, FROM_LITERAL_INT, FROM_LITERAL_STRING,
    INTEGERP, LESS_THAN, MINUS, MULTIPLY, NULLP, PAIRP, PLUS,
    PRINT_VALUE, PRINT_NEWLINE, STRINGP, V_TRUE,
    V_FALSE, V_NULL
}

class BuiltinDeclarations(val module: Module) {
    private val declarations = mapOf(
        Pair(BuiltinDeclarationEnum.DIVIDE, BuiltinDeclaration("_divide", listOf(module.structValueP, module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.EQUALS, BuiltinDeclaration("_equals", listOf(module.structValueP, module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.FROM_LITERAL_INT, BuiltinDeclaration("_from_literal_int", listOf(module.i32), module.structValueP)),
        Pair(BuiltinDeclarationEnum.FROM_LITERAL_STRING, BuiltinDeclaration("_from_literal_string", listOf(module.i8P), module.structValueP)),
        Pair(BuiltinDeclarationEnum.INTEGERP, BuiltinDeclaration("_integerp", listOf(module.structValueP), module.structValueP)),
        Pair(
            BuiltinDeclarationEnum.LESS_THAN,
            BuiltinDeclaration("_less_than", listOf(module.structValueP, module.structValueP), module.structValueP)
        ),
        Pair(BuiltinDeclarationEnum.MINUS, BuiltinDeclaration("_minus", listOf(module.structValueP, module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.MULTIPLY, BuiltinDeclaration("_multiply", listOf(module.structValueP, module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.NULLP, BuiltinDeclaration("_nullp", listOf(module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.PAIRP, BuiltinDeclaration("_pairp", listOf(module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.PLUS, BuiltinDeclaration("_plus", listOf(module.structValueP, module.structValueP), module.structValueP)),
        Pair(BuiltinDeclarationEnum.PRINT_VALUE, BuiltinDeclaration("_print_value", listOf(module.structValueP), module.void)),
        Pair(BuiltinDeclarationEnum.PRINT_NEWLINE, BuiltinDeclaration("_print_newline", listOf(), module.void)),
        Pair(BuiltinDeclarationEnum.STRINGP, BuiltinDeclaration("_stringp", listOf(module.structValueP), module.structValueP)),
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