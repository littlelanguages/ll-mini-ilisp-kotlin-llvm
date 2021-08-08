package io.littlelanguages.mil.static

import java.io.Reader
import io.littlelanguages.scanpiler.AbstractScanner
import io.littlelanguages.scanpiler.AbstractToken
import io.littlelanguages.scanpiler.Location

class Scanner(input: Reader): AbstractScanner<TToken>(input, TToken.TERROR) {
  override fun newToken(ttoken: TToken, location: Location, lexeme: String): AbstractToken<TToken> =
    Token(ttoken, location, lexeme)
  
  override fun next() {
    if (currentToken.tToken != TToken.TEOS) {
      while (nextCh in 0..32) {
        nextChar()
      }
      
      var state = 0
      while (true) {
        when (state) {
          0 -> {
            if (nextCh == 33) {
              markAndNextChar()
              state = 1
            } else if (nextCh == 47) {
              markAndNextChar()
              state = 2
            } else if (nextCh == 42) {
              markAndNextChar()
              state = 3
            } else if (nextCh == 62) {
              markAndNextChar()
              state = 4
            } else if (nextCh == 60) {
              markAndNextChar()
              state = 5
            } else if (nextCh == 61) {
              markAndNextChar()
              state = 6
            } else if (nextCh == 38) {
              markAndNextChar()
              state = 7
            } else if (nextCh == 124) {
              markAndNextChar()
              state = 8
            } else if (nextCh == 63) {
              markAndNextChar()
              state = 9
            } else if (nextCh == 119) {
              markAndNextChar()
              state = 10
            } else if (nextCh == 101) {
              markAndNextChar()
              state = 11
            } else if (nextCh == 105) {
              markAndNextChar()
              state = 12
            } else if (nextCh == 66) {
              markAndNextChar()
              state = 13
            } else if (nextCh == 70) {
              markAndNextChar()
              state = 14
            } else if (nextCh == 73) {
              markAndNextChar()
              state = 15
            } else if (nextCh == 125) {
              markAndNextChar()
              state = 16
            } else if (nextCh == 114) {
              markAndNextChar()
              state = 17
            } else if (nextCh == 123) {
              markAndNextChar()
              state = 18
            } else if (nextCh == 58) {
              markAndNextChar()
              state = 19
            } else if (nextCh == 41) {
              markAndNextChar()
              state = 20
            } else if (nextCh == 44) {
              markAndNextChar()
              state = 21
            } else if (nextCh == 40) {
              markAndNextChar()
              state = 22
            } else if (nextCh == 102) {
              markAndNextChar()
              state = 23
            } else if (nextCh == 45) {
              markAndNextChar()
              state = 24
            } else if (nextCh == 43) {
              markAndNextChar()
              state = 25
            } else if (nextCh == 116) {
              markAndNextChar()
              state = 26
            } else if (nextCh == 59) {
              markAndNextChar()
              state = 27
            } else if (nextCh == 108) {
              markAndNextChar()
              state = 28
            } else if (nextCh == 99) {
              markAndNextChar()
              state = 29
            } else if (nextCh == 65 || nextCh in 67..69 || nextCh == 71 || nextCh == 72 || nextCh in 74..90 || nextCh == 97 || nextCh == 98 || nextCh == 100 || nextCh == 103 || nextCh == 104 || nextCh == 106 || nextCh == 107 || nextCh in 109..113 || nextCh == 115 || nextCh == 117 || nextCh == 118 || nextCh in 120..122) {
              markAndNextChar()
              state = 30
            } else if (nextCh in 48..57) {
              markAndNextChar()
              state = 31
            } else if (nextCh == 34) {
              markAndNextChar()
              state = 32
            } else if (nextCh == -1) {
              markAndNextChar()
              state = 33
            } else if (nextCh == 46) {
              markAndNextChar()
              state = 34
            } else {
              markAndNextChar()
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          1 -> {
            if (nextCh == 61) {
              nextChar()
              state = 35
            } else {
              setToken(TToken.TBang)
              return
            }
          }
          2 -> {
            if (nextCh == 42) {
              nextChar()
              state = 36
            } else if (nextCh == 47) {
              nextChar()
              state = 37
            } else {
              setToken(TToken.TSlash)
              return
            }
          }
          3 -> {
            setToken(TToken.TStar)
            return
          }
          4 -> {
            if (nextCh == 61) {
              nextChar()
              state = 38
            } else {
              setToken(TToken.TGreaterThan)
              return
            }
          }
          5 -> {
            if (nextCh == 61) {
              nextChar()
              state = 39
            } else {
              setToken(TToken.TLessThan)
              return
            }
          }
          6 -> {
            if (nextCh == 61) {
              nextChar()
              state = 40
            } else {
              setToken(TToken.TEqual)
              return
            }
          }
          7 -> {
            if (nextCh == 38) {
              nextChar()
              state = 41
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          8 -> {
            if (nextCh == 124) {
              nextChar()
              state = 42
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          9 -> {
            setToken(TToken.TQuestion)
            return
          }
          10 -> {
            if (nextCh == 104) {
              nextChar()
              state = 43
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..103 || nextCh in 105..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          11 -> {
            if (nextCh == 108) {
              nextChar()
              state = 44
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..107 || nextCh in 109..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          12 -> {
            if (nextCh == 102) {
              nextChar()
              state = 45
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..101 || nextCh in 103..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          13 -> {
            if (nextCh == 111) {
              nextChar()
              state = 46
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..110 || nextCh in 112..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          14 -> {
            if (nextCh == 108) {
              nextChar()
              state = 47
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..107 || nextCh in 109..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          15 -> {
            if (nextCh == 110) {
              nextChar()
              state = 48
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..109 || nextCh in 111..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          16 -> {
            setToken(TToken.TRCurly)
            return
          }
          17 -> {
            if (nextCh == 101) {
              nextChar()
              state = 49
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          18 -> {
            setToken(TToken.TLCurly)
            return
          }
          19 -> {
            setToken(TToken.TColon)
            return
          }
          20 -> {
            setToken(TToken.TRParen)
            return
          }
          21 -> {
            setToken(TToken.TComma)
            return
          }
          22 -> {
            setToken(TToken.TLParen)
            return
          }
          23 -> {
            if (nextCh == 117) {
              nextChar()
              state = 50
            } else if (nextCh == 97) {
              nextChar()
              state = 51
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 98..116 || nextCh in 118..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          24 -> {
            setToken(TToken.TDash)
            return
          }
          25 -> {
            setToken(TToken.TPlus)
            return
          }
          26 -> {
            if (nextCh == 114) {
              nextChar()
              state = 52
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..113 || nextCh in 115..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          27 -> {
            setToken(TToken.TSemicolon)
            return
          }
          28 -> {
            if (nextCh == 101) {
              nextChar()
              state = 53
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          29 -> {
            if (nextCh == 111) {
              nextChar()
              state = 54
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..110 || nextCh in 112..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          30 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          31 -> {
            if (nextCh in 48..57) {
              nextChar()
              state = 31
            } else if (nextCh == 46) {
              markBacktrackPoint(TToken.TLiteralInt)
              nextChar()
              state = 55
            } else if (nextCh == 69 || nextCh == 101) {
              markBacktrackPoint(TToken.TLiteralInt)
              nextChar()
              state = 56
            } else {
              setToken(TToken.TLiteralInt)
              return
            }
          }
          32 -> {
            if (nextCh in 0..9 || nextCh in 11..33 || nextCh in 35..91 || nextCh in 93..255) {
              nextChar()
              state = 32
            } else if (nextCh == 92) {
              nextChar()
              state = 57
            } else if (nextCh == 34) {
              nextChar()
              state = 58
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          33 -> {
            setToken(TToken.TEOS)
            return
          }
          34 -> {
            if (nextCh in 48..57) {
              nextChar()
              state = 59
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          35 -> {
            setToken(TToken.TBangEqual)
            return
          }
          36 -> {
            var nstate = 0
            var nesting = 1
            while (true) {
              when (nstate) {
                0 -> {
                  if (nextCh in 0..41 || nextCh in 43..46 || nextCh in 48..255) {
                    nextChar()
                    nstate = 1
                  } else if (nextCh == 42) {
                    nextChar()
                    nstate = 2
                  } else if (nextCh == 47) {
                    nextChar()
                    nstate = 3
                  } else {
                    attemptBacktrackOtherwise(TToken.TERROR)
                    return
                  }
                }
                1 -> {
                  nstate = 0
                }
                2 -> {
                  if (nextCh == 47) {
                    nextChar()
                    nstate = 4
                  } else {
                    nstate = 0
                  }
                }
                3 -> {
                  if (nextCh == 42) {
                    nextChar()
                    nstate = 5
                  } else {
                    nstate = 0
                  }
                }
                4 -> {
                  nesting -= 1
                  if (nesting == 0) {
                    next()
                    return
                  } else {
                    nstate = 0
                  }
                }
                5 -> {
                  nesting += 1
                  nstate = 0
                }
              }
            }
          }
          37 -> {
            if (nextCh in 0..9 || nextCh in 11..255) {
              nextChar()
              state = 37
            } else {
              next()
              return
            }
          }
          38 -> {
            setToken(TToken.TGreaterThanEqual)
            return
          }
          39 -> {
            setToken(TToken.TLessThanEqual)
            return
          }
          40 -> {
            setToken(TToken.TEqualEqual)
            return
          }
          41 -> {
            setToken(TToken.TAmpersandAmpersand)
            return
          }
          42 -> {
            setToken(TToken.TBarBar)
            return
          }
          43 -> {
            if (nextCh == 105) {
              nextChar()
              state = 60
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..104 || nextCh in 106..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          44 -> {
            if (nextCh == 115) {
              nextChar()
              state = 61
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..114 || nextCh in 116..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          45 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIf)
              return
            }
          }
          46 -> {
            if (nextCh == 111) {
              nextChar()
              state = 62
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..110 || nextCh in 112..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          47 -> {
            if (nextCh == 111) {
              nextChar()
              state = 63
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..110 || nextCh in 112..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          48 -> {
            if (nextCh == 116) {
              nextChar()
              state = 64
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..115 || nextCh in 117..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          49 -> {
            if (nextCh == 116) {
              nextChar()
              state = 65
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..115 || nextCh in 117..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          50 -> {
            if (nextCh == 110) {
              nextChar()
              state = 66
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..109 || nextCh in 111..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          51 -> {
            if (nextCh == 108) {
              nextChar()
              state = 67
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..107 || nextCh in 109..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          52 -> {
            if (nextCh == 117) {
              nextChar()
              state = 68
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..116 || nextCh in 118..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          53 -> {
            if (nextCh == 116) {
              nextChar()
              state = 69
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..115 || nextCh in 117..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          54 -> {
            if (nextCh == 110) {
              nextChar()
              state = 70
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..109 || nextCh in 111..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          55 -> {
            if (nextCh in 48..57) {
              nextChar()
              state = 71
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          56 -> {
            if (nextCh == 43 || nextCh == 45) {
              nextChar()
              state = 72
            } else if (nextCh in 48..57) {
              nextChar()
              state = 73
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          57 -> {
            if (nextCh == 34) {
              nextChar()
              state = 74
            } else if (nextCh in 0..9 || nextCh in 11..33 || nextCh in 35..91 || nextCh in 93..255) {
              nextChar()
              state = 32
            } else if (nextCh == 92) {
              nextChar()
              state = 57
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          58 -> {
            setToken(TToken.TLiteralString)
            return
          }
          59 -> {
            if (nextCh == 69 || nextCh == 101) {
              markBacktrackPoint(TToken.TLiteralFloat)
              nextChar()
              state = 75
            } else if (nextCh in 48..57) {
              nextChar()
              state = 59
            } else {
              setToken(TToken.TLiteralFloat)
              return
            }
          }
          60 -> {
            if (nextCh == 108) {
              nextChar()
              state = 76
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..107 || nextCh in 109..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          61 -> {
            if (nextCh == 101) {
              nextChar()
              state = 77
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          62 -> {
            if (nextCh == 108) {
              nextChar()
              state = 78
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..107 || nextCh in 109..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          63 -> {
            if (nextCh == 97) {
              nextChar()
              state = 79
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 98..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          64 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TInt)
              return
            }
          }
          65 -> {
            if (nextCh == 117) {
              nextChar()
              state = 80
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..116 || nextCh in 118..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          66 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TFun)
              return
            }
          }
          67 -> {
            if (nextCh == 115) {
              nextChar()
              state = 81
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..114 || nextCh in 116..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          68 -> {
            if (nextCh == 101) {
              nextChar()
              state = 82
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          69 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLet)
              return
            }
          }
          70 -> {
            if (nextCh == 115) {
              nextChar()
              state = 83
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..114 || nextCh in 116..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          71 -> {
            if (nextCh == 69 || nextCh == 101) {
              markBacktrackPoint(TToken.TLiteralFloat)
              nextChar()
              state = 84
            } else if (nextCh in 48..57) {
              nextChar()
              state = 71
            } else {
              setToken(TToken.TLiteralFloat)
              return
            }
          }
          72 -> {
            if (nextCh in 48..57) {
              nextChar()
              state = 73
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          73 -> {
            if (nextCh in 48..57) {
              nextChar()
              state = 73
            } else {
              setToken(TToken.TLiteralFloat)
              return
            }
          }
          74 -> {
            if (nextCh == 34) {
              nextChar()
              state = 58
            } else if (nextCh in 0..9 || nextCh in 11..33 || nextCh in 35..91 || nextCh in 93..255) {
              markBacktrackPoint(TToken.TLiteralString)
              nextChar()
              state = 32
            } else if (nextCh == 92) {
              markBacktrackPoint(TToken.TLiteralString)
              nextChar()
              state = 57
            } else {
              setToken(TToken.TLiteralString)
              return
            }
          }
          75 -> {
            if (nextCh == 43 || nextCh == 45) {
              nextChar()
              state = 85
            } else if (nextCh in 48..57) {
              nextChar()
              state = 73
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          76 -> {
            if (nextCh == 101) {
              nextChar()
              state = 86
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          77 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TElse)
              return
            }
          }
          78 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TBool)
              return
            }
          }
          79 -> {
            if (nextCh == 116) {
              nextChar()
              state = 87
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..115 || nextCh in 117..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          80 -> {
            if (nextCh == 114) {
              nextChar()
              state = 88
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..113 || nextCh in 115..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          81 -> {
            if (nextCh == 101) {
              nextChar()
              state = 89
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          82 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TTrue)
              return
            }
          }
          83 -> {
            if (nextCh == 116) {
              nextChar()
              state = 90
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..115 || nextCh in 117..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          84 -> {
            if (nextCh == 43 || nextCh == 45) {
              nextChar()
              state = 91
            } else if (nextCh in 48..57) {
              nextChar()
              state = 73
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          85 -> {
            if (nextCh in 48..57) {
              nextChar()
              state = 73
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          86 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TWhile)
              return
            }
          }
          87 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TFloat)
              return
            }
          }
          88 -> {
            if (nextCh == 110) {
              nextChar()
              state = 92
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..109 || nextCh in 111..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIdentifier)
              return
            }
          }
          89 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TFalse)
              return
            }
          }
          90 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TConst)
              return
            }
          }
          91 -> {
            if (nextCh in 48..57) {
              nextChar()
              state = 73
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          92 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TReturn)
              return
            }
          }
        }
      }
    }
  }
}

enum class TToken {
  TBang,
  TSlash,
  TStar,
  TGreaterThan,
  TGreaterThanEqual,
  TLessThan,
  TLessThanEqual,
  TBangEqual,
  TEqualEqual,
  TAmpersandAmpersand,
  TBarBar,
  TQuestion,
  TWhile,
  TElse,
  TIf,
  TBool,
  TFloat,
  TInt,
  TRCurly,
  TReturn,
  TLCurly,
  TColon,
  TRParen,
  TComma,
  TLParen,
  TFun,
  TDash,
  TPlus,
  TFalse,
  TTrue,
  TSemicolon,
  TEqual,
  TLet,
  TConst,
  TIdentifier,
  TLiteralInt,
  TLiteralFloat,
  TLiteralString,
  TEOS,
  TERROR,
}

typealias Token = AbstractToken<TToken>