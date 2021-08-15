# ll-mini-ilisp-kotlin-llvm

In my efforts to understand compilers and LLVM code generation I have found Lisp has been a wonderful learning ground in that it is a simple yet powerful language with a minimal collection of constructs.

`mini-iLisp` is a dialect that I have been kicking around and would like to now produce a compiler for this language producing a binary executable.  This language has the following features:

- tagged values
- closures
- signals also called exceptions
- garbage collection

As I have learnt, other than tagged values, each one of these, from a code generation and runtime system, are complicated.

## Language Description

`mini-iLisp` supports the following data types:

| Data Type | Commentary |
|-|-|
| Null | Literal value of `()` |
| Symbol | Symbols are objects whose values are equivalent if and only if their names are spelt the same way.  So `'hello` and `'Hello` are different symbols whilst `'world` and `'world` are equivalent. |
| Boolean | Composed of the two keywords `#t` and `#f`. Like Scheme *truthy* is any value except `#f` while *falsy* is only `#f`. |
| Character | A representation of 8 bit ASCII (Extended ASCII). Each value is held as an integer in the range 0..255 with this value being coercible into integers. Constant characters are represented using Scheme's notation of a `#\` prefix followed by the character.  So checking that a variable `c` is a numeric character would be expressed as `(<=  #\0 c #\9)`.  A literal character can also be expressed as `#x`*\<hex scalar\>* with *\<hex scalar\>* being the character's ASCII.  |
| Integer | A 32-bit signed value. |
| String | A sequence of characters. A literal string is a sequence of character delimited with quotation marks (`"`). Within a string literal, various escape sequences represent characters other than themselves: <ul><li>`\t`: character tabulation `#x09`</li><li>`\n`: linefeed `#x0a`</li><li>`\r`: return `#x0d`</li><li>`\\`: single backslash</li><li>`\"`: double quote `#x22`</li><li>`\x`*\<hex scalar\>*`;`: specified character with the ASCII code of *\<hex scalar*\></li></ul>|
| Pair | A pair is an object with two fields - *car* and *cdr*. Pairs are created using the procedure `pair` with each field accessible using the procedures `car` and `cdr` respectively. Pairs are used primarily to represent lists. A list is defined recursively as either `()` or a pair whose *cdr* is a list. Pairs created using the procedure `cons` are immutable.  Literal lists are represented by enclosing the elements with parenthesis and separating with whitespace.  The expression `(pair 1 (pair 2 (pair 3 (pair 4 ()))))` can literally be represented as `(1 2 3 4)` |
| Procedure | A procedure is how things get done in `mini-iLisp`.  There are two kinds of procedures - native procedures and compound procedures.  A native procedure is provided by the `min-iLisp` runtime system whilst a compound procedure is composed using a sequence of expressions and represented using the `proc` form. As an aside it pains me to introduce the `proc` form and define increment as `((proc n) (+ n 1))` rather than using Scheme's `lambda` form `((lambda n) (+ n 1))` or Clojure's `fn` form `((fn n) (+ n 1))`.  I have elected to go with the `proc` form for the following reasons: <ul><li>All the Lisp flavours refer to behavior as a procedure rather than a function or method.</li> <li>Functions are grounded in maths and the association to many is they are a mapping from a domain to a range, side effect free and are total.  A language based on functions, like Haskell, is rather explicit about this.</li><li>These behaviours are procedures: they are a step-by-step sequence of instructions, they can mutate both internal and external state, they may fail in execution, and they make no pretence as to being a function or relation.</li></ul>The difficulty in changing from `lambda`/`fn` to `proc` is sentiment and, on reflection, appropriation of the concept felt unnecessary. |

`mini-iLisp` has the following standard forms:

The following table describes the standard available forms.

| Purpose | Forms |
|-|-|
| Binding declaration | `const` |
| Conditional evaluation | `if` |
| Iteration | `proc` |
| Sequence | `do` |
| Signals | `try`, `signal` |

The following is not part of `mini-iLisp`:

- Macros
- Mutation
- Modules

Each of these can be added without too much complexity however they would distract from the simplicity of the implementation.

## Syntax

The following EBNF grammar defines the syntax of a `mini-iLisp` programme using [`parspiler`](https://github.com/littlelanguages/parspiler):

```
Program: {Expression};

Expression
  : "(" {Expression} ")"
  | Symbol
  | LiteralInt
  | LiteralString
  | "#t"
  | "#f"
  ;
```

Using this grammar `mini-iLisp`'s lexical structure is defined using [`scanpiler`](https://github.com/littlelanguages/scanpiler) as follows:

```
tokens
    Symbol = (id \ '-') {digit | id};
    LiteralInt = ['-'] digits;
    LiteralString = '"' {!('"' + cr) | "\" ('"' + '\' + 't' + 'n' + 'r' | "\x" hexDigits)} '"';

comments
   ";" {!cr};

whitespace
  chr(0)-' ';

fragments
  digit = '0'-'9';
  digits = digit {digit};
  hexDigit = digit + 'a'-'f' + 'A'-'F';
  hexDigits = hexDigit {hexDigit};
  id = '!'-'}' \ ('0'-'9' + '"' + '(' + ')' + ';' + '#');
  cr = chr(10);
```

## Builtin Procedures

| Name | Description |
|------|-------------|
| `(+ v1` ... `vn)` | Performs the calculation ( ... ((`v1` + `v2`) + `v3`) + ...) + `vn`).  If the form `(+)` is used then returns 0.  If the form `(+ v1)` is used then returns `v1`.|
| `(- v1` ... `vn)` | Performs the calculation ( ... ((`v1` - `v2`) - `v3`) - ...) - `vn`).  If the form `(-)` is used then returns 0.  If the form `(- v1)` is used then returns -`v1`. |
| `(* v1` ... `vn)` | Performs the calculation ( ... ((`v1` * `v2`) * `v3`) * ...) * `vn`).  If the form `(*)` is used then returns 1.  If the form `(* v1)` is used then returns `v1`. |
| `(/ v1` ... `vn)` | Performs the calculation ( ... ((`v1` / `v2`) / `v3`) - ...) - `vn`).  If the form `(/)` is used then returns 1.  If the form `(/ v1)` is used then returns 1 / `v1`. |
| `(boolean? v)` | Should `v` refer to either `#t` or `#f` then returns `#t` otherwise returns `#f`. |
| `(car v)` | Should `v` refer to a pair node then returns the first (or car) element of that node.  Should `v` not refer to a pair node then raises the signal `ValueNotPair`. |
| `(cdr v)` | Should `v` refer to a pair node then returns the second (or cdr) element of that node.  Should `v` not refer to a pair node then raises the signal `ValueNotPair`. |
| `(null? v)` | Should `v` refer to the `()` value then returns `#t` otherwise returns `#f`. |
| `(pair a b)` | Composes a pair node where the `car` of that node equals `a` and the `cdr` equals `b`. | 
| `(print v1 ... vn)` | Writes the values `v1` to `vn` out to the console.  This procedure does not place a space between the printed values and does not terminate with a newline. |
| `(println v1 ... vn)` | Writes the values `v1` to `vn` out to the console followed by a newline.  This procedure does not place a space between the printed values. |