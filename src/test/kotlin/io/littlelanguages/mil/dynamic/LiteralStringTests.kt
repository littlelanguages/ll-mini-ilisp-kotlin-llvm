package io.littlelanguages.mil.dynamic

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.littlelanguages.mil.dynamic.tst.LiteralString
import io.littlelanguages.scanpiler.LocationCoordinate

class LiteralStringTests : FunSpec({
    test("\"Hello World\"") {
        translateLiteralString(
            io.littlelanguages.mil.static.ast.LiteralString(LocationCoordinate(0, 0, 0), "\"Hello World\"")
        ) shouldBe LiteralString("Hello World")
    }
})