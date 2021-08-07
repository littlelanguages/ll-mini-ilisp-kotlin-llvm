package io.littlelanguages.data


sealed class Either<L, R> {
    abstract infix fun <U> map(f: (R) -> U): Either<L, U>

    abstract infix fun <U> mapLeft(f: (L) -> U): Either<U, R>

    abstract infix fun <U> andThen(f: (R) -> Either<L, U>): Either<L, U>
}


data class Left<L, R>(val left: L) : Either<L, R>() {
    override fun <U> map(f: (R) -> U): Either<L, U> =
            Left(left)

    override fun <U> mapLeft(f: (L) -> U): Either<U, R> =
            Left(f(left))

    override fun <U> andThen(f: (R) -> Either<L, U>): Either<L, U> =
            Left(left)
}


data class Right<L, R>(val right: R) : Either<L, R>() {
    override fun <U> map(f: (R) -> U): Either<L, U> =
            Right(f(right))

    override fun <U> mapLeft(f: (L) -> U): Either<U, R> =
            Right(right)

    override fun <U> andThen(f: (R) -> Either<L, U>): Either<L, U> =
            f(right)
}

