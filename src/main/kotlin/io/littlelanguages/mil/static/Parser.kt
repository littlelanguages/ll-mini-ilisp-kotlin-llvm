package io.littlelanguages.mil.static

interface Visitor<T_Program, T_VariableDeclaration, T_LiteralExpression, T_FunctionDeclaration, T_FunctionDeclarationSuffix, T_Type, T_TypedIdentifier, T_Statement, T_Expression, T_OrExpression, T_AndExpression, T_RelationalExpression, T_RelationalOp, T_AdditiveExpression, T_AdditiveOp, T_MultiplicativeExpression, T_MultiplicativeOp, T_Factor, T_UnaryOp> {
  fun visitProgram(a: List<io.littlelanguages.data.Union2<T_VariableDeclaration, T_FunctionDeclaration>>): T_Program
  fun visitVariableDeclaration(a1: io.littlelanguages.data.Union2<Token, Token>, a2: Token, a3: Token, a4: T_LiteralExpression, a5: Token): T_VariableDeclaration
  fun visitLiteralExpression1(a: Token): T_LiteralExpression
  fun visitLiteralExpression2(a: Token): T_LiteralExpression
  fun visitLiteralExpression3(a1: io.littlelanguages.data.Union2<Token, Token>?, a2: io.littlelanguages.data.Union2<Token, Token>): T_LiteralExpression
  fun visitFunctionDeclaration(a1: Token, a2: Token, a3: Token, a4: io.littlelanguages.data.Tuple2<T_TypedIdentifier, List<io.littlelanguages.data.Tuple2<Token, T_TypedIdentifier>>>?, a5: Token, a6: T_FunctionDeclarationSuffix): T_FunctionDeclaration
  fun visitFunctionDeclarationSuffix1(a1: Token, a2: T_Type, a3: Token, a4: List<T_Statement>, a5: Token, a6: T_Expression, a7: Token, a8: Token): T_FunctionDeclarationSuffix
  fun visitFunctionDeclarationSuffix2(a1: Token, a2: List<T_Statement>, a3: Token): T_FunctionDeclarationSuffix
  fun visitType1(a: Token): T_Type
  fun visitType2(a: Token): T_Type
  fun visitType3(a: Token): T_Type
  fun visitTypedIdentifier(a1: Token, a2: Token, a3: T_Type): T_TypedIdentifier
  fun visitStatement1(a1: io.littlelanguages.data.Union2<Token, Token>, a2: Token, a3: Token, a4: T_Expression, a5: Token): T_Statement
  fun visitStatement2(a1: Token, a2: T_Expression, a3: T_Statement, a4: io.littlelanguages.data.Tuple2<Token, T_Statement>?): T_Statement
  fun visitStatement3(a1: Token, a2: T_Expression, a3: T_Statement): T_Statement
  fun visitStatement4(a1: Token, a2: List<T_Statement>, a3: Token): T_Statement
  fun visitStatement5(a1: Token, a2: io.littlelanguages.data.Union2<io.littlelanguages.data.Tuple3<Token, io.littlelanguages.data.Tuple2<T_Expression, List<io.littlelanguages.data.Tuple2<Token, T_Expression>>>?, Token>, io.littlelanguages.data.Tuple2<Token, T_Expression>>, a3: Token): T_Statement
  fun visitStatement6(a: Token): T_Statement
  fun visitExpression(a1: T_OrExpression, a2: io.littlelanguages.data.Tuple4<Token, T_Expression, Token, T_Expression>?): T_Expression
  fun visitOrExpression(a1: T_AndExpression, a2: List<io.littlelanguages.data.Tuple2<Token, T_AndExpression>>): T_OrExpression
  fun visitAndExpression(a1: T_RelationalExpression, a2: List<io.littlelanguages.data.Tuple2<Token, T_RelationalExpression>>): T_AndExpression
  fun visitRelationalExpression(a1: T_AdditiveExpression, a2: io.littlelanguages.data.Tuple2<T_RelationalOp, T_AdditiveExpression>?): T_RelationalExpression
  fun visitRelationalOp1(a: Token): T_RelationalOp
  fun visitRelationalOp2(a: Token): T_RelationalOp
  fun visitRelationalOp3(a: Token): T_RelationalOp
  fun visitRelationalOp4(a: Token): T_RelationalOp
  fun visitRelationalOp5(a: Token): T_RelationalOp
  fun visitRelationalOp6(a: Token): T_RelationalOp
  fun visitAdditiveExpression(a1: T_MultiplicativeExpression, a2: List<io.littlelanguages.data.Tuple2<T_AdditiveOp, T_MultiplicativeExpression>>): T_AdditiveExpression
  fun visitAdditiveOp1(a: Token): T_AdditiveOp
  fun visitAdditiveOp2(a: Token): T_AdditiveOp
  fun visitMultiplicativeExpression(a1: T_Factor, a2: List<io.littlelanguages.data.Tuple2<T_MultiplicativeOp, T_Factor>>): T_MultiplicativeExpression
  fun visitMultiplicativeOp1(a: Token): T_MultiplicativeOp
  fun visitMultiplicativeOp2(a: Token): T_MultiplicativeOp
  fun visitFactor1(a: Token): T_Factor
  fun visitFactor2(a: Token): T_Factor
  fun visitFactor3(a: Token): T_Factor
  fun visitFactor4(a: Token): T_Factor
  fun visitFactor5(a: Token): T_Factor
  fun visitFactor6(a1: T_UnaryOp, a2: T_Factor): T_Factor
  fun visitFactor7(a1: Token, a2: T_Expression, a3: Token): T_Factor
  fun visitFactor8(a1: Token, a2: io.littlelanguages.data.Tuple3<Token, io.littlelanguages.data.Tuple2<T_Expression, List<io.littlelanguages.data.Tuple2<Token, T_Expression>>>?, Token>?): T_Factor
  fun visitUnaryOp1(a: Token): T_UnaryOp
  fun visitUnaryOp2(a: Token): T_UnaryOp
  fun visitUnaryOp3(a: Token): T_UnaryOp
}

class Parser<T_Program, T_VariableDeclaration, T_LiteralExpression, T_FunctionDeclaration, T_FunctionDeclarationSuffix, T_Type, T_TypedIdentifier, T_Statement, T_Expression, T_OrExpression, T_AndExpression, T_RelationalExpression, T_RelationalOp, T_AdditiveExpression, T_AdditiveOp, T_MultiplicativeExpression, T_MultiplicativeOp, T_Factor, T_UnaryOp>(
    private val scanner: Scanner,
    private val visitor: Visitor<T_Program, T_VariableDeclaration, T_LiteralExpression, T_FunctionDeclaration, T_FunctionDeclarationSuffix, T_Type, T_TypedIdentifier, T_Statement, T_Expression, T_OrExpression, T_AndExpression, T_RelationalExpression, T_RelationalOp, T_AdditiveExpression, T_AdditiveOp, T_MultiplicativeExpression, T_MultiplicativeOp, T_Factor, T_UnaryOp>) {
  fun program(): T_Program {
    val a = mutableListOf<io.littlelanguages.data.Union2<T_VariableDeclaration, T_FunctionDeclaration>>()
    
    while (isTokens(set1)) {
      val at: io.littlelanguages.data.Union2<T_VariableDeclaration, T_FunctionDeclaration> = when {
        isTokens(set2)-> {
          val at0: T_VariableDeclaration = variableDeclaration()
          
          io.littlelanguages.data.Union2a(at0)
        }
        isToken(TToken.TFun)-> {
          val at1: T_FunctionDeclaration = functionDeclaration()
          
          io.littlelanguages.data.Union2b(at1)
        }
        else -> {
          throw ParsingException(peek(), set1)
        }
      }
      a.add(at)
    }
    return visitor.visitProgram(a)
  }
  
  fun variableDeclaration(): T_VariableDeclaration {
    val a1: io.littlelanguages.data.Union2<Token, Token> = when {
      isToken(TToken.TConst)-> {
        val a10: Token = matchToken(TToken.TConst)
        
        io.littlelanguages.data.Union2a(a10)
      }
      isToken(TToken.TLet)-> {
        val a11: Token = matchToken(TToken.TLet)
        
        io.littlelanguages.data.Union2b(a11)
      }
      else -> {
        throw ParsingException(peek(), set2)
      }
    }
    val a2: Token = matchToken(TToken.TIdentifier)
    val a3: Token = matchToken(TToken.TEqual)
    val a4: T_LiteralExpression = literalExpression()
    val a5: Token = matchToken(TToken.TSemicolon)
    return visitor.visitVariableDeclaration(a1, a2, a3, a4, a5)
  }
  
  fun literalExpression(): T_LiteralExpression {
    when {
      isToken(TToken.TTrue) -> {
        return visitor.visitLiteralExpression1(matchToken(TToken.TTrue))
      }
      isToken(TToken.TFalse) -> {
        return visitor.visitLiteralExpression2(matchToken(TToken.TFalse))
      }
      isTokens(set3) -> {
        var a1: io.littlelanguages.data.Union2<Token, Token>? = null
        
        if (isTokens(set4)) {
          val a1t: io.littlelanguages.data.Union2<Token, Token> = when {
            isToken(TToken.TPlus)-> {
              val a1t0: Token = matchToken(TToken.TPlus)
              
              io.littlelanguages.data.Union2a(a1t0)
            }
            isToken(TToken.TDash)-> {
              val a1t1: Token = matchToken(TToken.TDash)
              
              io.littlelanguages.data.Union2b(a1t1)
            }
            else -> {
              throw ParsingException(peek(), set4)
            }
          }
          a1 = a1t
        }
        val a2: io.littlelanguages.data.Union2<Token, Token> = when {
          isToken(TToken.TLiteralInt)-> {
            val a20: Token = matchToken(TToken.TLiteralInt)
            
            io.littlelanguages.data.Union2a(a20)
          }
          isToken(TToken.TLiteralFloat)-> {
            val a21: Token = matchToken(TToken.TLiteralFloat)
            
            io.littlelanguages.data.Union2b(a21)
          }
          else -> {
            throw ParsingException(peek(), set5)
          }
        }
        return visitor.visitLiteralExpression3(a1, a2)
      }
      else -> {
        throw ParsingException(peek(), set6)
      }
    }
  }
  
  fun functionDeclaration(): T_FunctionDeclaration {
    val a1: Token = matchToken(TToken.TFun)
    val a2: Token = matchToken(TToken.TIdentifier)
    val a3: Token = matchToken(TToken.TLParen)
    var a4: io.littlelanguages.data.Tuple2<T_TypedIdentifier, List<io.littlelanguages.data.Tuple2<Token, T_TypedIdentifier>>>? = null
    
    if (isToken(TToken.TIdentifier)) {
      val a4t1: T_TypedIdentifier = typedIdentifier()
      val a4t2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_TypedIdentifier>>()
      
      while (isToken(TToken.TComma)) {
        val a4t2t1: Token = matchToken(TToken.TComma)
        val a4t2t2: T_TypedIdentifier = typedIdentifier()
        val a4t2t: io.littlelanguages.data.Tuple2<Token, T_TypedIdentifier> = io.littlelanguages.data.Tuple2(a4t2t1, a4t2t2)
        a4t2.add(a4t2t)
      }
      val a4t: io.littlelanguages.data.Tuple2<T_TypedIdentifier, List<io.littlelanguages.data.Tuple2<Token, T_TypedIdentifier>>> = io.littlelanguages.data.Tuple2(a4t1, a4t2)
      a4 = a4t
    }
    val a5: Token = matchToken(TToken.TRParen)
    val a6: T_FunctionDeclarationSuffix = functionDeclarationSuffix()
    return visitor.visitFunctionDeclaration(a1, a2, a3, a4, a5, a6)
  }
  
  fun functionDeclarationSuffix(): T_FunctionDeclarationSuffix {
    when {
      isToken(TToken.TColon) -> {
        val a1: Token = matchToken(TToken.TColon)
        val a2: T_Type = type()
        val a3: Token = matchToken(TToken.TLCurly)
        val a4= mutableListOf<T_Statement>()
        
        while (isTokens(set7)) {
          val a4t: T_Statement = statement()
          a4.add(a4t)
        }
        val a5: Token = matchToken(TToken.TReturn)
        val a6: T_Expression = expression()
        val a7: Token = matchToken(TToken.TSemicolon)
        val a8: Token = matchToken(TToken.TRCurly)
        return visitor.visitFunctionDeclarationSuffix1(a1, a2, a3, a4, a5, a6, a7, a8)
      }
      isToken(TToken.TLCurly) -> {
        val a1: Token = matchToken(TToken.TLCurly)
        val a2= mutableListOf<T_Statement>()
        
        while (isTokens(set7)) {
          val a2t: T_Statement = statement()
          a2.add(a2t)
        }
        val a3: Token = matchToken(TToken.TRCurly)
        return visitor.visitFunctionDeclarationSuffix2(a1, a2, a3)
      }
      else -> {
        throw ParsingException(peek(), set8)
      }
    }
  }
  
  fun type(): T_Type {
    when {
      isToken(TToken.TInt) -> {
        return visitor.visitType1(matchToken(TToken.TInt))
      }
      isToken(TToken.TFloat) -> {
        return visitor.visitType2(matchToken(TToken.TFloat))
      }
      isToken(TToken.TBool) -> {
        return visitor.visitType3(matchToken(TToken.TBool))
      }
      else -> {
        throw ParsingException(peek(), set9)
      }
    }
  }
  
  fun typedIdentifier(): T_TypedIdentifier {
    val a1: Token = matchToken(TToken.TIdentifier)
    val a2: Token = matchToken(TToken.TColon)
    val a3: T_Type = type()
    return visitor.visitTypedIdentifier(a1, a2, a3)
  }
  
  fun statement(): T_Statement {
    when {
      isTokens(set2) -> {
        val a1: io.littlelanguages.data.Union2<Token, Token> = when {
          isToken(TToken.TConst)-> {
            val a10: Token = matchToken(TToken.TConst)
            
            io.littlelanguages.data.Union2a(a10)
          }
          isToken(TToken.TLet)-> {
            val a11: Token = matchToken(TToken.TLet)
            
            io.littlelanguages.data.Union2b(a11)
          }
          else -> {
            throw ParsingException(peek(), set2)
          }
        }
        val a2: Token = matchToken(TToken.TIdentifier)
        val a3: Token = matchToken(TToken.TEqual)
        val a4: T_Expression = expression()
        val a5: Token = matchToken(TToken.TSemicolon)
        return visitor.visitStatement1(a1, a2, a3, a4, a5)
      }
      isToken(TToken.TIf) -> {
        val a1: Token = matchToken(TToken.TIf)
        val a2: T_Expression = expression()
        val a3: T_Statement = statement()
        var a4: io.littlelanguages.data.Tuple2<Token, T_Statement>? = null
        
        if (isToken(TToken.TElse)) {
          val a4t1: Token = matchToken(TToken.TElse)
          val a4t2: T_Statement = statement()
          val a4t: io.littlelanguages.data.Tuple2<Token, T_Statement> = io.littlelanguages.data.Tuple2(a4t1, a4t2)
          a4 = a4t
        }
        return visitor.visitStatement2(a1, a2, a3, a4)
      }
      isToken(TToken.TWhile) -> {
        val a1: Token = matchToken(TToken.TWhile)
        val a2: T_Expression = expression()
        val a3: T_Statement = statement()
        return visitor.visitStatement3(a1, a2, a3)
      }
      isToken(TToken.TLCurly) -> {
        val a1: Token = matchToken(TToken.TLCurly)
        val a2= mutableListOf<T_Statement>()
        
        while (isTokens(set7)) {
          val a2t: T_Statement = statement()
          a2.add(a2t)
        }
        val a3: Token = matchToken(TToken.TRCurly)
        return visitor.visitStatement4(a1, a2, a3)
      }
      isToken(TToken.TIdentifier) -> {
        val a1: Token = matchToken(TToken.TIdentifier)
        val a2: io.littlelanguages.data.Union2<io.littlelanguages.data.Tuple3<Token, io.littlelanguages.data.Tuple2<T_Expression, List<io.littlelanguages.data.Tuple2<Token, T_Expression>>>?, Token>, io.littlelanguages.data.Tuple2<Token, T_Expression>> = when {
          isToken(TToken.TLParen)-> {
            val a201: Token = matchToken(TToken.TLParen)
            var a202: io.littlelanguages.data.Tuple2<T_Expression, List<io.littlelanguages.data.Tuple2<Token, T_Expression>>>? = null
            
            if (isTokens(set10)) {
              val a202t1: T_Expression = expression()
              val a202t2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_Expression>>()
              
              while (isToken(TToken.TComma)) {
                val a202t2t1: Token = matchToken(TToken.TComma)
                val a202t2t2: T_Expression = expression()
                val a202t2t: io.littlelanguages.data.Tuple2<Token, T_Expression> = io.littlelanguages.data.Tuple2(a202t2t1, a202t2t2)
                a202t2.add(a202t2t)
              }
              val a202t: io.littlelanguages.data.Tuple2<T_Expression, List<io.littlelanguages.data.Tuple2<Token, T_Expression>>> = io.littlelanguages.data.Tuple2(a202t1, a202t2)
              a202 = a202t
            }
            val a203: Token = matchToken(TToken.TRParen)
            val a20: io.littlelanguages.data.Tuple3<Token, io.littlelanguages.data.Tuple2<T_Expression, List<io.littlelanguages.data.Tuple2<Token, T_Expression>>>?, Token> = io.littlelanguages.data.Tuple3(a201, a202, a203)
            
            io.littlelanguages.data.Union2a(a20)
          }
          isToken(TToken.TEqual)-> {
            val a211: Token = matchToken(TToken.TEqual)
            val a212: T_Expression = expression()
            val a21: io.littlelanguages.data.Tuple2<Token, T_Expression> = io.littlelanguages.data.Tuple2(a211, a212)
            
            io.littlelanguages.data.Union2b(a21)
          }
          else -> {
            throw ParsingException(peek(), set11)
          }
        }
        val a3: Token = matchToken(TToken.TSemicolon)
        return visitor.visitStatement5(a1, a2, a3)
      }
      isToken(TToken.TSemicolon) -> {
        return visitor.visitStatement6(matchToken(TToken.TSemicolon))
      }
      else -> {
        throw ParsingException(peek(), set7)
      }
    }
  }
  
  fun expression(): T_Expression {
    val a1: T_OrExpression = orExpression()
    var a2: io.littlelanguages.data.Tuple4<Token, T_Expression, Token, T_Expression>? = null
    
    if (isToken(TToken.TQuestion)) {
      val a2t1: Token = matchToken(TToken.TQuestion)
      val a2t2: T_Expression = expression()
      val a2t3: Token = matchToken(TToken.TColon)
      val a2t4: T_Expression = expression()
      val a2t: io.littlelanguages.data.Tuple4<Token, T_Expression, Token, T_Expression> = io.littlelanguages.data.Tuple4(a2t1, a2t2, a2t3, a2t4)
      a2 = a2t
    }
    return visitor.visitExpression(a1, a2)
  }
  
  fun orExpression(): T_OrExpression {
    val a1: T_AndExpression = andExpression()
    val a2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_AndExpression>>()
    
    while (isToken(TToken.TBarBar)) {
      val a2t1: Token = matchToken(TToken.TBarBar)
      val a2t2: T_AndExpression = andExpression()
      val a2t: io.littlelanguages.data.Tuple2<Token, T_AndExpression> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2.add(a2t)
    }
    return visitor.visitOrExpression(a1, a2)
  }
  
  fun andExpression(): T_AndExpression {
    val a1: T_RelationalExpression = relationalExpression()
    val a2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_RelationalExpression>>()
    
    while (isToken(TToken.TAmpersandAmpersand)) {
      val a2t1: Token = matchToken(TToken.TAmpersandAmpersand)
      val a2t2: T_RelationalExpression = relationalExpression()
      val a2t: io.littlelanguages.data.Tuple2<Token, T_RelationalExpression> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2.add(a2t)
    }
    return visitor.visitAndExpression(a1, a2)
  }
  
  fun relationalExpression(): T_RelationalExpression {
    val a1: T_AdditiveExpression = additiveExpression()
    var a2: io.littlelanguages.data.Tuple2<T_RelationalOp, T_AdditiveExpression>? = null
    
    if (isTokens(set12)) {
      val a2t1: T_RelationalOp = relationalOp()
      val a2t2: T_AdditiveExpression = additiveExpression()
      val a2t: io.littlelanguages.data.Tuple2<T_RelationalOp, T_AdditiveExpression> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2 = a2t
    }
    return visitor.visitRelationalExpression(a1, a2)
  }
  
  fun relationalOp(): T_RelationalOp {
    when {
      isToken(TToken.TEqualEqual) -> {
        return visitor.visitRelationalOp1(matchToken(TToken.TEqualEqual))
      }
      isToken(TToken.TBangEqual) -> {
        return visitor.visitRelationalOp2(matchToken(TToken.TBangEqual))
      }
      isToken(TToken.TLessThanEqual) -> {
        return visitor.visitRelationalOp3(matchToken(TToken.TLessThanEqual))
      }
      isToken(TToken.TLessThan) -> {
        return visitor.visitRelationalOp4(matchToken(TToken.TLessThan))
      }
      isToken(TToken.TGreaterThanEqual) -> {
        return visitor.visitRelationalOp5(matchToken(TToken.TGreaterThanEqual))
      }
      isToken(TToken.TGreaterThan) -> {
        return visitor.visitRelationalOp6(matchToken(TToken.TGreaterThan))
      }
      else -> {
        throw ParsingException(peek(), set12)
      }
    }
  }
  
  fun additiveExpression(): T_AdditiveExpression {
    val a1: T_MultiplicativeExpression = multiplicativeExpression()
    val a2= mutableListOf<io.littlelanguages.data.Tuple2<T_AdditiveOp, T_MultiplicativeExpression>>()
    
    while (isTokens(set4)) {
      val a2t1: T_AdditiveOp = additiveOp()
      val a2t2: T_MultiplicativeExpression = multiplicativeExpression()
      val a2t: io.littlelanguages.data.Tuple2<T_AdditiveOp, T_MultiplicativeExpression> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2.add(a2t)
    }
    return visitor.visitAdditiveExpression(a1, a2)
  }
  
  fun additiveOp(): T_AdditiveOp {
    when {
      isToken(TToken.TPlus) -> {
        return visitor.visitAdditiveOp1(matchToken(TToken.TPlus))
      }
      isToken(TToken.TDash) -> {
        return visitor.visitAdditiveOp2(matchToken(TToken.TDash))
      }
      else -> {
        throw ParsingException(peek(), set4)
      }
    }
  }
  
  fun multiplicativeExpression(): T_MultiplicativeExpression {
    val a1: T_Factor = factor()
    val a2= mutableListOf<io.littlelanguages.data.Tuple2<T_MultiplicativeOp, T_Factor>>()
    
    while (isTokens(set13)) {
      val a2t1: T_MultiplicativeOp = multiplicativeOp()
      val a2t2: T_Factor = factor()
      val a2t: io.littlelanguages.data.Tuple2<T_MultiplicativeOp, T_Factor> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2.add(a2t)
    }
    return visitor.visitMultiplicativeExpression(a1, a2)
  }
  
  fun multiplicativeOp(): T_MultiplicativeOp {
    when {
      isToken(TToken.TStar) -> {
        return visitor.visitMultiplicativeOp1(matchToken(TToken.TStar))
      }
      isToken(TToken.TSlash) -> {
        return visitor.visitMultiplicativeOp2(matchToken(TToken.TSlash))
      }
      else -> {
        throw ParsingException(peek(), set13)
      }
    }
  }
  
  fun factor(): T_Factor {
    when {
      isToken(TToken.TLiteralInt) -> {
        return visitor.visitFactor1(matchToken(TToken.TLiteralInt))
      }
      isToken(TToken.TLiteralFloat) -> {
        return visitor.visitFactor2(matchToken(TToken.TLiteralFloat))
      }
      isToken(TToken.TLiteralString) -> {
        return visitor.visitFactor3(matchToken(TToken.TLiteralString))
      }
      isToken(TToken.TTrue) -> {
        return visitor.visitFactor4(matchToken(TToken.TTrue))
      }
      isToken(TToken.TFalse) -> {
        return visitor.visitFactor5(matchToken(TToken.TFalse))
      }
      isTokens(set14) -> {
        val a1: T_UnaryOp = unaryOp()
        val a2: T_Factor = factor()
        return visitor.visitFactor6(a1, a2)
      }
      isToken(TToken.TLParen) -> {
        val a1: Token = matchToken(TToken.TLParen)
        val a2: T_Expression = expression()
        val a3: Token = matchToken(TToken.TRParen)
        return visitor.visitFactor7(a1, a2, a3)
      }
      isToken(TToken.TIdentifier) -> {
        val a1: Token = matchToken(TToken.TIdentifier)
        var a2: io.littlelanguages.data.Tuple3<Token, io.littlelanguages.data.Tuple2<T_Expression, List<io.littlelanguages.data.Tuple2<Token, T_Expression>>>?, Token>? = null
        
        if (isToken(TToken.TLParen)) {
          val a2t1: Token = matchToken(TToken.TLParen)
          var a2t2: io.littlelanguages.data.Tuple2<T_Expression, List<io.littlelanguages.data.Tuple2<Token, T_Expression>>>? = null
          
          if (isTokens(set10)) {
            val a2t2t1: T_Expression = expression()
            val a2t2t2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_Expression>>()
            
            while (isToken(TToken.TComma)) {
              val a2t2t2t1: Token = matchToken(TToken.TComma)
              val a2t2t2t2: T_Expression = expression()
              val a2t2t2t: io.littlelanguages.data.Tuple2<Token, T_Expression> = io.littlelanguages.data.Tuple2(a2t2t2t1, a2t2t2t2)
              a2t2t2.add(a2t2t2t)
            }
            val a2t2t: io.littlelanguages.data.Tuple2<T_Expression, List<io.littlelanguages.data.Tuple2<Token, T_Expression>>> = io.littlelanguages.data.Tuple2(a2t2t1, a2t2t2)
            a2t2 = a2t2t
          }
          val a2t3: Token = matchToken(TToken.TRParen)
          val a2t: io.littlelanguages.data.Tuple3<Token, io.littlelanguages.data.Tuple2<T_Expression, List<io.littlelanguages.data.Tuple2<Token, T_Expression>>>?, Token> = io.littlelanguages.data.Tuple3(a2t1, a2t2, a2t3)
          a2 = a2t
        }
        return visitor.visitFactor8(a1, a2)
      }
      else -> {
        throw ParsingException(peek(), set10)
      }
    }
  }
  
  fun unaryOp(): T_UnaryOp {
    when {
      isToken(TToken.TBang) -> {
        return visitor.visitUnaryOp1(matchToken(TToken.TBang))
      }
      isToken(TToken.TDash) -> {
        return visitor.visitUnaryOp2(matchToken(TToken.TDash))
      }
      isToken(TToken.TPlus) -> {
        return visitor.visitUnaryOp3(matchToken(TToken.TPlus))
      }
      else -> {
        throw ParsingException(peek(), set14)
      }
    }
  }
  
  
  private fun matchToken(tToken: TToken): Token =
    when (peek().tToken) {
      tToken -> nextToken()
      else -> throw ParsingException(peek(), setOf(tToken))
    }
  
  private fun nextToken(): Token {
    val result =
      peek()
    
    skipToken()
    
    return result
  }
  
  private fun skipToken() {
    scanner.next()
  }
  
  private fun isToken(tToken: TToken): Boolean =
    peek().tToken == tToken
  
  private fun isTokens(tTokens: Set<TToken>): Boolean =
    tTokens.contains(peek().tToken)
  
  private fun peek(): Token =
    scanner.current()
}

private val set1 = setOf(TToken.TConst, TToken.TLet, TToken.TFun)

private val set2 = setOf(TToken.TConst, TToken.TLet)

private val set3 = setOf(TToken.TLiteralInt, TToken.TLiteralFloat, TToken.TPlus, TToken.TDash)

private val set4 = setOf(TToken.TPlus, TToken.TDash)

private val set5 = setOf(TToken.TLiteralInt, TToken.TLiteralFloat)

private val set6 = setOf(TToken.TTrue, TToken.TFalse, TToken.TLiteralInt, TToken.TLiteralFloat, TToken.TPlus, TToken.TDash)

private val set7 = setOf(TToken.TConst, TToken.TLet, TToken.TIf, TToken.TWhile, TToken.TLCurly, TToken.TIdentifier, TToken.TSemicolon)

private val set8 = setOf(TToken.TColon, TToken.TLCurly)

private val set9 = setOf(TToken.TInt, TToken.TFloat, TToken.TBool)

private val set10 = setOf(TToken.TLiteralInt, TToken.TLiteralFloat, TToken.TLiteralString, TToken.TTrue, TToken.TFalse, TToken.TLParen, TToken.TIdentifier, TToken.TBang, TToken.TDash, TToken.TPlus)

private val set11 = setOf(TToken.TLParen, TToken.TEqual)

private val set12 = setOf(TToken.TEqualEqual, TToken.TBangEqual, TToken.TLessThanEqual, TToken.TLessThan, TToken.TGreaterThanEqual, TToken.TGreaterThan)

private val set13 = setOf(TToken.TStar, TToken.TSlash)

private val set14 = setOf(TToken.TBang, TToken.TDash, TToken.TPlus)

class ParsingException(
  val found: Token,
  val expected: Set<TToken>) : Exception()