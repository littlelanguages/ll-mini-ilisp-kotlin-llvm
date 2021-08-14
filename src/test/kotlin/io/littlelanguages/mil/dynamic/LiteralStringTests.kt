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

    test("\"\\\"\"") {
        translateLiteralString(
            io.littlelanguages.mil.static.ast.LiteralString(LocationCoordinate(0, 0, 0), "\"\\\"\"")
        ) shouldBe LiteralString("\"")
    }

    test("\"\\\"") {
        translateLiteralString(
            io.littlelanguages.mil.static.ast.LiteralString(LocationCoordinate(0, 0, 0), "\"\\\"")
        ) shouldBe LiteralString("\"")
    }

    test("\"\\\" \\\\ \\t \\n \\r\"") {
        translateLiteralString(
            io.littlelanguages.mil.static.ast.LiteralString(LocationCoordinate(0, 0, 0), "\"\\\" \\\\ \\t \\n \\r\"")
        ) shouldBe LiteralString("\" \\ \t \n \r")
    }

    test("\"\\x21\\x20\"") {
        translateLiteralString(
            io.littlelanguages.mil.static.ast.LiteralString(LocationCoordinate(0, 0, 0), "\"\\x21\\x20\"")
        ) shouldBe LiteralString("! ")
    }
})
