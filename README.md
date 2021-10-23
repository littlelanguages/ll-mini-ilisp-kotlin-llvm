# ll-mini-ilisp-kotlin-llvm

In my efforts to understand compilers and LLVM code generation I have found Lisp to be a wonderful learning ground in that it is a simple yet powerful language with a minimal collection of constructs.

`mini-iLisp` is a Lisp dialect with the following features:

- tagged values
- closures
- signals also called exceptions
- garbage collection

As I have learnt, other than tagged values, each one of these, from a code generation and runtime system, are complicated.

This project is an implementation of a `mini-iLisp` compiler written into Kotlin that produces LLVM code.

## Calculate Primes

The following `mini-iLisp` program calculates and displays the list of primes in the range 0 to 10000.

```scm
(const (multiple? m n)
    (const remainder
        (- m (* (/ m n) n))
    )

    (= remainder 0)
)

(const (append e lst)
    (if (null? lst)
            (pair e ())
        (pair (car lst) (append e (cdr lst)))
    )
)

(const (divisible-by? n lst)
    (if (null? lst)
            #f
        (multiple? n (car lst))
            #t
        (divisible-by? n (cdr lst))
    )
)

(const (primes min max)
    (const (sieve n p')
        (if (< max n)
                p'
            (divisible-by? n p')
                (sieve (+ n 1) p')
            (sieve (+ n 1) (append n p'))
        )
    )

    (sieve min ())
)

(println (primes 2 10000))
```

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

To get a feel for the language it is helpful to look at the [compiler test cases](./src/test/kotlin/io/littlelanguages/mil/compiler/compiler.yaml) all wrapped up in a single YAML file.  During testing the source for each test is compiled into LLVM, linked to form a binary and then run in a shell with the output captured and validated against the expected output.  This is rather a crude style of testing but it does give me the confidence that, on every run, the entire solution is checked. 

## Syntax

The following EBNF grammar defines the syntax of a `mini-iLisp` programme using [`parspiler`](https://github.com/littlelanguages/parspiler):

```
Program: {Expression};

Expression
  : "(" [ExpressionBody] ")"
  | Symbol
  | LiteralInt
  | LiteralString
  ;

ExpressionBody
  : "if" {Expression}
  | "do" {Expression}
  | "const" ConstBody
  | "proc" "(" {Symbol} ")" {Expression}
  | "try" Expression Expression
  | "signal" Expression
  | Expression {Expression}
  ;

ConstBody
  : Symbol Expression
  | "(" Symbol {Symbol} ")" {Expression}
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
| `(= v1 v2)` | Should `v1` refer to the same value as `v2` then returns `#t` otherwise returns `#f` |
| `(< v1 v2)` | Should `v1` refer to a value which is less than the value that `v2` refers to then returns `#t` otherwise returns `#f`. This procedure operates over integer, boolean and string values.  All other values will cause the procedure to return `#f`. |
| `(boolean? v)` | Should `v` refer to either `#t` or `#f` then returns `#t` otherwise returns `#f`. |
| `(car v)` | Should `v` refer to a pair node then returns the first (or car) element of that node.  Should `v` not refer to a pair node then raises the signal `ValueNotPair`. |
| `(cdr v)` | Should `v` refer to a pair node then returns the second (or cdr) element of that node.  Should `v` not refer to a pair node then raises the signal `ValueNotPair`. |
| `(integer? v)` | Should `v` refer to an integer value then returns `#t` otherwise returns `#f`. |
| `(null? v)` | Should `v` refer to the `()` value then returns `#t` otherwise returns `#f`. |
| `(pair a b)` | Composes a pair node where the `car` of that node equals `a` and the `cdr` equals `b`. | 
| `(pair? v)` | Should `v` refer to a pair node then returns `#t` otherwise returns `#f`. |
| `(print v1 ... vn)` | Writes the values `v1` to `vn` out to the console.  This procedure does not place a space between the printed values and does not terminate with a newline. |
| `(println v1 ... vn)` | Writes the values `v1` to `vn` out to the console followed by a newline.  This procedure does not place a space between the printed values. |
| `(string? v)` | Should `v` refer to a string value then returns `#t` otherwise returns `#f`. |

## Building the Compiler

The following dependencies are needed in order to build this compiler

| Name | Reason |
|------|--------|
| [Java JDK](https://openjdk.java.net) | Currently I have OpenJDK 17 installed on my Mac | 
| [gradle](https://gradle.org) | JVM based build tool |
| [Deno](https://deno.land) | The compiler tools [`parspiler`](https://github.com/littlelanguages/parspiler) and [`scanpiler`](https://github.com/littlelanguages/scanpiler) are both implemented in Deno.  The beauty of Deno is application code is downloaded on demand so there is no need to perform installs other than the Deno runtime itself. |
| [LLVM](https://llvm.org) | Currently I have LLVM 13.0.0 installed. Libraries and tooling are required. |
| [autoconf](https://www.gnu.org/software/autoconf/), [automake](https://www.gnu.org/software/automake/) | These tools are needed to compile the [Boehm-Demers-Weiser Garbage Collector](https://github.com/ivmai/bdwgc) |

The following scripts are located in `.bin` off of the project root.  These scripts are really helpful in capturing the build steps.

| Name | Purpose |
|------|---------|
| `build.sh` | Builds the entire code base by running `parspiler`, compiling C [./src/main/c](./src/main/c) code that needs to be linked into the generated `mini-iLisp` code and then build and testing the Kotlin source code. If all the previous steps are successful then creates the distribution file `build/distributions/ll-mini-ilisp-kotlin-llvm.tar` containing all of the dependent jar files as shell script to run the compiler with the correct CLASSPATH settings. |
| `dist.sh` | Runs `build.sh` and then expands the `tar` in the project root.  This is necessary in order to compile the sample code [samples](./samples) |
| `setup.sh` | Downloads the source and builds the [Boehm-Demers-Weiser Garbage Collector](https://github.com/ivmai/bdwgc).  It is only necessary to run this script once. |
| `test.sh` | Does the minimal amount of work to run the test scripts.  |

On a clean checkout these are the steps that I would follow to be able to run the samples.

```
graemelockley@Graemes-iMac-2 ll-mini-ilisp-kotlin-llvm % .bin/setup.sh 
Setting up Darwin
Using brew to install autoconf and automake
Getting bdwgc and building
Cloning into 'bdwgc'...
remote: Enumerating objects: 35917, done.
remote: Counting objects: 100% (1691/1691), done.
remote: Compressing objects: 100% (528/528), done.
...

graemelockley@Graemes-iMac-2 ll-mini-ilisp-kotlin-llvm % .bin/dist.sh 
clang -c lib.c
clang -c main.c

> Task :compileKotlin
...

graemelockley@Graemes-iMac-2 ll-mini-ilisp-kotlin-llvm % cd samples 
graemelockley@Graemes-iMac-2 samples % make
../ll-mini-ilisp-kotlin-llvm/bin/ll-mini-ilisp-kotlin-llvm hello.mlsp
clang hello.bc ../src/main/c/lib.o ../bdwgc/gc.a ../src/main/c/main.o -o hello
../ll-mini-ilisp-kotlin-llvm/bin/ll-mini-ilisp-kotlin-llvm primes.mlsp
clang primes.bc ../src/main/c/lib.o ../bdwgc/gc.a ../src/main/c/main.o -o primes
../ll-mini-ilisp-kotlin-llvm/bin/ll-mini-ilisp-kotlin-llvm euler-001.mlsp
clang euler-001.bc ../src/main/c/lib.o ../bdwgc/gc.a ../src/main/c/main.o -o euler-001
../ll-mini-ilisp-kotlin-llvm/bin/ll-mini-ilisp-kotlin-llvm divide-by-zero.mlsp
clang divide-by-zero.bc ../src/main/c/lib.o ../bdwgc/gc.a ../src/main/c/main.o -o divide-by-zero
rm primes.bc euler-001.bc hello.bc divide-by-zero.bc
graemelockley@Graemes-iMac-2 samples % ./hello 
Hello worlds!
graemelockley@Graemes-iMac-2 samples % ./divide-by-zero 
Hello worlds!
Unhandled Exception: (DivideByZero divide-by-zero.mlsp 4)
graemelockley@Graemes-iMac-2 samples % ./euler-001 
233168
```
