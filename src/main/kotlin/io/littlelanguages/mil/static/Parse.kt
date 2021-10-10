package io.littlelanguages.mil.static

import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.ParseError
import io.littlelanguages.mil.static.ast.*
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.Locationable

fun parse(scanner: Scanner): Either<Errors, Program> =
    try {
        Right(Parser(scanner, ParseVisitor()).program())
    } catch (e: ParsingException) {
        Left(ParseError(e.found, e.expected))
    }

class ParseVisitor : Visitor<
        Program, List<Expression>, List<Expression>, Expression> {
    override fun visitProgram(a: List<List<Expression>>): Program =
        Program(a.flatten())

    override fun visitExpression1(a1: Token, a2: List<Expression>?, a3: Token): List<Expression> =
        if (a2 == null)
            listOf(LiteralUnit(a1.location + a3.location))
        else if (a2.size == 1)
            when (val e = a2[0]) {
                is SExpression ->
                    listOf(SExpression(a1.location + a3.location, e.expressions))

                is IfExpression ->
                    listOf(IfExpression(a1.location + a3.location, e.expressions))

                else ->
                    a2
            }
        else
            a2

    override fun visitExpression2(a: Token): List<Expression> =
        listOf(Symbol(a.location, a.lexeme))

    override fun visitExpression3(a: Token): List<Expression> =
        listOf(LiteralInt(a.location, a.lexeme))

    override fun visitExpression4(a: Token): List<Expression> =
        listOf(LiteralString(a.location, a.lexeme))

    override fun visitExpressionBody1(a1: Token, a2: List<List<Expression>>): List<Expression> =
        listOf(
            IfExpression(locationOfs(a1.location, a2), a2)
        )

    override fun visitExpressionBody2(a1: Token, a2: List<List<Expression>>): List<Expression> =
        a2.flatten()

    override fun visitExpressionBody3(a1: Token, a2: Expression): List<Expression> =
        listOf(a2)

    override fun visitExpressionBody4(a1: Token, a2: Token, a3: List<Token>, a4: Token, a5: List<List<Expression>>): List<Expression> =
        listOf(ProcExpression(locationOfs(a1.location + a4.location, a5), a3.map { Symbol(it.location, it.lexeme) }, a5.flatten()))

    override fun visitExpressionBody5(a1: Token, a2: List<Expression>, a3: List<Expression>): List<Expression> {
        TODO("Not yet implemented")
    }

    override fun visitExpressionBody6(a1: Token, a2: List<Expression>): List<Expression> {
        TODO("Not yet implemented")
    }

    override fun visitExpressionBody7(a1: List<Expression>, a2: List<List<Expression>>): List<Expression> {
        val es = a1 + a2.flatten()

        return listOf(
            SExpression(locationOf(es[0].position, es.drop(1)), es)
        )
    }

    override fun visitConstBody1(a1: Token, a2: List<Expression>): Expression =
        ConstValue(locationOf(a1.location, a2), Symbol(a1.location, a1.lexeme), a2)

    override fun visitConstBody2(a1: Token, a2: Token, a3: List<Token>, a4: Token, a5: List<List<Expression>>): Expression =
        ConstProcedure(
            locationOfs(a1.location + a4.location, a5),
            Symbol(a2.location, a2.lexeme),
            a3.map { Symbol(it.location, it.lexeme) },
            a5.flatten()
        )

}


private fun locationOf(loc: Location, ls: List<Locationable>): Location =
    ls.fold(loc) { l1, l2 -> l1 + l2.position() }

private fun locationOfs(loc: Location, ls: List<List<Locationable>>): Location =
    ls.fold(loc) { l1, l2 -> locationOf(l1, l2) }
