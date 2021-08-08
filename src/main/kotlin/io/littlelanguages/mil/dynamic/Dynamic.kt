package io.littlelanguages.mil.dynamic

import io.littlelanguages.data.Either
import io.littlelanguages.data.Right
import io.littlelanguages.mil.Errors
import io.littlelanguages.mil.dynamic.tst.Program

fun translate(p: io.littlelanguages.mil.static.ast.Program): Either<List<Errors>, Program> {
    return Right(Program(setOf(), listOf()))
}
