package io.littlelanguages.mil.static

import io.littlelanguages.data.Either
import io.littlelanguages.data.Left
import io.littlelanguages.data.Right
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.ParseError
import io.littlelanguages.mil.static.ast.*

fun parse(scanner: Scanner): Either<Errors, Program> =
    try {
        Right(Parser(scanner, ParseVisitor()).program())
    } catch (e: ParsingException) {
        Left(ParseError(e.found, e.expected))
    }

class ParseVisitor : Visitor<
        Program, Expression> {
    override fun visitProgram(a: List<Expression>): Program =
        Program(a)

    override fun visitExpression1(a1: Token, a2: List<Expression>, a3: Token): Expression =
        SExpression(a1.location + a3.location, a2)

    override fun visitExpression2(a: Token): Expression =
        Symbol(a.location, a.lexeme)

    override fun visitExpression3(a: Token): Expression =
        LiteralInt(a.location, a.lexeme)

    override fun visitExpression4(a: Token): Expression =
        LiteralString(a.location, a.lexeme)

    override fun visitExpression5(a: Token): Expression =
        LiteralBool(a.location, true)

    override fun visitExpression6(a: Token): Expression =
        LiteralBool(a.location, false)
}
