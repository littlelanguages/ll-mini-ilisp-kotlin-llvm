package io.littlelanguages.p0.static

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.littlelanguages.scanpiler.LocationCoordinate
import io.littlelanguages.scanpiler.LocationRange


private val P1 = LocationCoordinate(0, 1, 2)
private val P2 = LocationCoordinate(3, 4, 5)
private val P3 = LocationCoordinate(6, 7, 8)
private val P4 = LocationCoordinate(9, 10, 11)


class LocationTests : StringSpec({
    "adding two ordered coordinates" {
        val r = (P1 + P2) as LocationRange

        r.start.offset shouldBe 0
        r.start.line shouldBe 1
        r.start.column shouldBe 2

        r.end.offset shouldBe 3
        r.end.line shouldBe 4
        r.end.column shouldBe 5
    }

    "adding two coordinates in reverse" {
        val r = (P2 + P1) as LocationRange

        r.start.offset shouldBe 0
        r.start.line shouldBe 1
        r.start.column shouldBe 2

        r.end.offset shouldBe 3
        r.end.line shouldBe 4
        r.end.column shouldBe 5
    }

    "adding two ordered ranges" {
        val r = ((P1 + P2) + (P3 + P4)) as LocationRange

        r.start.offset shouldBe 0
        r.start.line shouldBe 1
        r.start.column shouldBe 2

        r.end.offset shouldBe 9
        r.end.line shouldBe 10
        r.end.column shouldBe 11
    }

    "adding two ranges in reverse" {
        val r = ((P4 + P3) + (P2 + P1)) as LocationRange

        r.start.offset shouldBe 0
        r.start.line shouldBe 1
        r.start.column shouldBe 2

        r.end.offset shouldBe 9
        r.end.line shouldBe 10
        r.end.column shouldBe 11
    }
})
