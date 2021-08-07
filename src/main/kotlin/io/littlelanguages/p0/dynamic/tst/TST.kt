package io.littlelanguages.p0.dynamic.tst

import io.littlelanguages.data.Yamlable
import java.text.DecimalFormat
import kotlin.math.abs

interface TypeOfAble {
    fun typeOf(): Type
}

data class Program(val declarations: List<Declaration>, val statement: Statement) : Yamlable {
    override fun yaml(): Any =
            singletonMap("Program", mapOf(
                    Pair("d", declarations.map { it.yaml() }),
                    Pair("s", statement.yaml())))
}


sealed class Declaration(open val n: String) : Yamlable

data class ConstantDeclaration(
        override val n: String,
        val v: LiteralValue) : Declaration(n) {
    override fun yaml(): Any =
            singletonMap("ConstantDeclaration", mapOf(
                    Pair("identifier", n),
                    Pair("e", v.yaml())
            ))
}

data class VariableDeclaration(
        override val n: String,
        val v: LiteralValue) : Declaration(n) {
    override fun yaml(): Any =
            singletonMap("VariableDeclaration", mapOf(
                    Pair("identifier", n),
                    Pair("e", v.yaml())
            ))

}

data class FunctionDeclaration(
        override val n: String,
        val ps: List<Pair<String, Type>>,
        val ss: List<Statement>,
        val e: Expression?) : Declaration(n) {
    override fun yaml(): Any {
        val suffix =
                e

        return if (suffix == null)
            singletonMap("FunctionDeclaration", mapOf(
                    Pair("identifier", n),
                    Pair("arguments", ps.map {
                        mapOf(
                                Pair("name", it.first),
                                Pair("type", it.second))
                    }),
                    Pair("s", ss.map { it.yaml() })
            ))
        else
            singletonMap("FunctionDeclaration", mapOf(
                    Pair("identifier", n),
                    Pair("arguments", ps.map {
                        mapOf(
                                Pair("name", it.first),
                                Pair("type", it.second))
                    }),
                    Pair("s", ss.map { it.yaml() }),
                    Pair("e", suffix.yaml())
            ))
    }
}


enum class Type {
    Int, Float, Bool, String, TError
}


sealed class Statement : Yamlable

data class AssignmentStatement(
        val n: String,
        val e: Expression) : Statement() {
    override fun yaml(): Any =
            singletonMap("AssignmentStatement", mapOf(
                    Pair("identifier", n),
                    Pair("e", e.yaml())
            ))
}

data class ConstantDeclarationStatement(
        val n: String,
        val e: Expression) : Statement() {
    override fun yaml(): Any =
            singletonMap("ConstantDeclarationStatement", mapOf(
                    Pair("identifier", n),
                    Pair("e", e.yaml())
            ))
}

data class VariableDeclarationStatement(
        val n: String,
        val e: Expression) : Statement() {
    override fun yaml(): Any =
            singletonMap("VariableDeclarationStatement", mapOf(
                    Pair("identifier", n),
                    Pair("e", e.yaml())
            ))
}

data class IfThenElseStatement(
        val e: Expression,
        val s1: Statement,
        val s2: Statement?) : Statement() {
    override fun yaml(): Any {
        val s2 =
                s2

        return if (s2 == null)
            singletonMap("IfThenElseStatement", mapOf(
                    Pair("e", e.yaml()),
                    Pair("s1", s1.yaml())
            ))
        else
            singletonMap("IfThenElseStatement", mapOf(
                    Pair("e", e.yaml()),
                    Pair("s1", s1.yaml()),
                    Pair("s2", s2.yaml())
            ))
    }
}

data class WhileStatement(
        val e: Expression,
        val s: Statement) : Statement() {
    override fun yaml(): Any =
            singletonMap("WhileStatement", mapOf(
                    Pair("e", e.yaml()),
                    Pair("s", s.yaml())
            ))
}

data class BlockStatement(
        val ss: List<Statement>) : Statement() {
    override fun yaml(): Any =
            singletonMap("BlockStatement", ss.map { it.yaml() })
}

data class CallStatement(
        val n: String,
        val args: List<Expression>) : Statement() {
    override fun yaml(): Any =
            singletonMap("CallStatement", mapOf(
                    Pair("identifier", n),
                    Pair("parameters", args.map { it.yaml() })
            ))
}

object EmptyStatement : Statement() {
    override fun yaml(): Any =
            singletonMap("EmptyStatement", emptyMap<String, Any>())
}


sealed class Expression : TypeOfAble, Yamlable

data class TernaryExpression(
        val e1: Expression,
        val e2: Expression,
        val e3: Expression) : Expression() {
    override fun typeOf(): Type = e2.typeOf()

    override fun yaml(): Any =
            singletonMap("TernaryExpression", mapOf(
                    Pair("type", typeOf()),
                    Pair("e1", e1.yaml()),
                    Pair("e2", e2.yaml()),
                    Pair("e3", e3.yaml())))
}

data class BinaryExpression(
        val op: BinaryOp,
        val e1: Expression,
        val e2: Expression) : Expression() {
    override fun typeOf(): Type =
            if (op == BinaryOp.Minus || op == BinaryOp.Plus || op == BinaryOp.Times || op == BinaryOp.Divide)
                e2.typeOf()
            else
                Type.Bool

    override fun yaml(): Any =
            singletonMap("BinaryExpression", mapOf(
                    Pair("type", typeOf()),
                    Pair("op", op.yaml()),
                    Pair("e1", e1.yaml()),
                    Pair("e2", e2.yaml())))
}

data class UnaryExpression(
        val t: Type,
        val op: UnaryOp,
        val e: Expression) : Expression() {
    override fun typeOf(): Type =
            if (op == UnaryOp.UnaryNot)
                Type.Bool
            else
                e.typeOf()

    override fun yaml(): Any =
            singletonMap("UnaryExpression", mapOf(
                    Pair("type", t),
                    Pair("op", op.yaml()),
                    Pair("e", e.yaml())))
}

data class CallExpression(
        val t: Type,
        val n: String,
        val args: List<Expression>) : Expression() {
    override fun typeOf(): Type = t


    override fun yaml(): Any =
            singletonMap("CallExpression", mapOf(
                    Pair("type", t),
                    Pair("name", n),
                    Pair("parameters", args.map { it.yaml() })))

}

data class IdentifierReference(
        val t: Type,
        val n: String) : Expression() {
    override fun typeOf(): Type = t

    override fun yaml(): Any =
            mapOf(Pair("IdentifierReference", mapOf(
                    Pair("name", n),
                    Pair("type", t))))
}

data class LiteralValueExpression(
        val v: LiteralValue) : Expression() {
    override fun typeOf(): Type = v.typeOf()

    override fun yaml(): Any =
            singletonMap("LiteralValue", v.yaml())
}


sealed class LiteralValue : TypeOfAble, Yamlable

data class LiteralBool(
        val v: Boolean) : LiteralValue() {
    override fun typeOf(): Type = Type.Bool

    override fun yaml(): Any =
            singletonMap("LiteralBool", if (v) "True" else "False")
}

data class LiteralInt(
        val v: Int) : LiteralValue() {
    override fun typeOf(): Type = Type.Int

    override fun yaml(): Any =
            singletonMap("LiteralInt", v)
}

data class LiteralFloat(
        val v: Float) : LiteralValue() {
    override fun typeOf(): Type = Type.Float

    override fun yaml(): Any =
            singletonMap("LiteralFloat",
                    if (abs(v) < 1.0)
                        DecimalFormat("0.0#####E0").format(v).replace('E', 'e').replace(',', '.')
                    else
                        v.toString()
            )
}

data class LiteralString(
        val v: String) : LiteralValue() {
    override fun typeOf(): Type = Type.String

    override fun yaml(): Any =
            singletonMap("LiteralString", "\"${v}\"")
}


enum class BinaryOp : Yamlable {
    Divide, Minus, Plus, Times, Equal, GreaterEqual, GreaterThan, LessEqual, LessThan, NotEqual, And, Or;

    override fun yaml(): String =
            when (this) {
                Divide -> "Divide"
                Minus -> "Minus"
                Plus -> "Plus"
                Times -> "Times"
                Equal -> "Equal"
                GreaterEqual -> "GreaterEqual"
                GreaterThan -> "GreaterThan"
                LessEqual -> "LessEqual"
                LessThan -> "LessThan"
                NotEqual -> "NotEqual"
                And -> "And"
                Or -> "Or"
            }
}

enum class UnaryOp : Yamlable {
    UnaryNot, UnaryMinus, UnaryPlus;

    override fun yaml(): String =
            when (this) {
                UnaryNot -> "UnaryNot"
                UnaryMinus -> "UnaryMinus"
                UnaryPlus -> "UnaryPlus"
            }
}
