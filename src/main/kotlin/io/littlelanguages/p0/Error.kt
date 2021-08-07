package io.littlelanguages.p0

import io.littlelanguages.data.Yamlable
import io.littlelanguages.p0.dynamic.tst.BinaryOp
import io.littlelanguages.p0.dynamic.tst.Type
import io.littlelanguages.p0.dynamic.tst.UnaryOp
import io.littlelanguages.p0.static.TToken
import io.littlelanguages.p0.static.Token
import io.littlelanguages.scanpiler.Location

sealed class Errors : Yamlable

data class ParseError(
        val found: Token,
        val expected: Set<TToken>) : Errors() {
    override fun yaml(): Any =
            singletonMap("ParseError", mapOf(
                    Pair("found", found),
                    Pair("expected", expected)
            ))
}

data class AttemptToRedefineDeclarationError(
        val location: Location,
        val name: String) : Errors() {
    override fun yaml(): Any =
            singletonMap("AttemptToRedefineDeclaration", mapOf(
                    Pair("name", name),
                    Pair("position", location.yaml())
            ))
}

data class BinaryExpressionOperandsIncompatible(
        val op: BinaryOp,
        val location1: Location,
        val type1: Type,
        val location2: Location,
        val type2: Type) : Errors() {
    override fun yaml(): Any =
            singletonMap("BinaryExpressionOperandsIncompatible", mapOf(
                    Pair("op", op),
                    Pair("position1", location1),
                    Pair("type1", type1),
                    Pair("position2", location2),
                    Pair("type2", type2)
            ))
}

data class BinaryExpressionRequiresOperandType(
        val op: BinaryOp,
        val type: Type,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("BinaryExpressionRequiresOperandType", mapOf(
                    Pair("op", op),
                    Pair("type", type),
                    Pair("position", location.yaml())
            ))
}

data class FunctionReturnTypeMismatch(
        val location: Location,
        val name: String,
        val typ: Type) : Errors() {
    override fun yaml(): Any =
            singletonMap("FunctionReturnTypeMismatch", mapOf(
                    Pair("position", location.yaml()),
                    Pair("name", name),
                    Pair("type", typ)
            ))
}

data class IfGuardNotBoolean(
        val type: Type,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("IfGuardNotBoolean", mapOf(
                    Pair("type", type),
                    Pair("position", location.yaml())
            ))
}

data class InvalidDeclarationOfMain(
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("InvalidDeclarationOfMain", mapOf(
                    Pair("position", location.yaml())
            ))
}

data class IncompatibleArgumentType(
        val argumentType: Type,
        val parameterType: Type,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("IncompatibleArgumentType", mapOf(
                    Pair("argumentType", argumentType),
                    Pair("parameterType", parameterType),
                    Pair("position", location.yaml())
            ))
}

data class LiteralFloatOverFlowError(
        val location: Location,
        val value: String) : Errors() {
    override fun yaml(): Any =
            singletonMap("LiteralFloatOverflow", mapOf(
                    Pair("text", value),
                    Pair("position", location.yaml())
            ))
}

data class LiteralIntOverFlowError(
        val location: Location,
        val value: String) : Errors() {
    override fun yaml(): Any =
            singletonMap("LiteralIntOverflow", mapOf(
                    Pair("text", value),
                    Pair("position", location.yaml())
            ))
}

data class MismatchInNumberOfParameters(
        val arguments: Int,
        val parameters: Int,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("MismatchInNumberOfParameters", mapOf(
                    Pair("arguments", arguments),
                    Pair("parameters", parameters),
                    Pair("position", location.yaml())
            ))
}

data class TernaryExpressionResultIncompatible(
        val thenLocation: Location,
        val elseLocation: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("TernaryExpressionResultIncompatible", mapOf(
                    Pair("thenPosition", thenLocation.yaml()),
                    Pair("elsePosition", elseLocation.yaml())
            ))
}

data class TernaryExpressionNotBoolean(
        val boolLocation: Location,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("TernaryExpressionNotBoolean", mapOf(
                    Pair("boolPosition", boolLocation.yaml()),
                    Pair("position", location.yaml())
            ))
}

data class UnableToAssignToConstant(
        val name: String,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("UnableToAssignToConstant", mapOf(
                    Pair("name", name),
                    Pair("position", location.yaml())
            ))
}

data class UnableToAssignIncompatibleTypes(
        val type: Type,
        val location: Location,
        val expressionType: Type,
        val expressionLocation: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("UnableToAssignIncompatibleTypes", mapOf(
                    Pair("type", type),
                    Pair("position", location.yaml()),
                    Pair("expressionType", expressionType),
                    Pair("expressionPosition", expressionLocation.yaml())
            ))
}

data class UnableToAssignToFunction(
        val name: String,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("UnableToAssignToFunction", mapOf(
                    Pair("name", name),
                    Pair("position", location.yaml())
            ))
}

data class UnableToCallUnitFunctionAsValueFunction(
        val name: String,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("UnableToCallUnitFunctionAsValueFunction", mapOf(
                    Pair("name", name),
                    Pair("position", location.yaml())
            ))
}

data class UnableToCallConstantAsFunction(
        val name: String,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("UnableToCallConstantAsFunction", mapOf(
                    Pair("name", name),
                    Pair("position", location.yaml())
            ))
}

data class UnableToCallVariableAsFunction(
        val name: String,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("UnableToCallVariableAsFunction", mapOf(
                    Pair("name", name),
                    Pair("position", location.yaml())
            ))
}

data class UnaryExpressionRequiresOperandType(
        val op: UnaryOp,
        val type: Type,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("UnaryExpressionRequiresOperandType", mapOf(
                    Pair("op", op),
                    Pair("type", type),
                    Pair("position", location.yaml())
            ))
}

data class UnableToReferenceFunction(
        val name: String,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("UnableToReferenceFunction", mapOf(
                    Pair("name", name),
                    Pair("position", location.yaml())
            ))
}

data class UnableToCallValueFunctionAsUnitFunction(
        val name: String,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("UnableToCallValueFunctionAsUnitFunction", mapOf(
                    Pair("name", name),
                    Pair("position", location.yaml())
            ))
}

data class UnknownIdentifier(
        val name: String,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("UnknownIdentifier", mapOf(
                    Pair("name", name),
                    Pair("position", location.yaml())
            ))
}

data class WhileGuardNotBoolean(
        val type: Type,
        val location: Location) : Errors() {
    override fun yaml(): Any =
            singletonMap("WhileGuardNotBoolean", mapOf(
                    Pair("type", type),
                    Pair("position", location.yaml())
            ))
}
