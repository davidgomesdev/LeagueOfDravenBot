package me.l3n.bot.discord.lod.utils

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class StringUtilsTests : ShouldSpec({
    should("capitalize first character") {
        "dog".capitalize() shouldBe "Dog"
    }

    should("return an already capitalized input") {
        "Dog".capitalize() shouldBe "Dog"
    }

    context("given an empty input") {
        should("return empty") {
            "".capitalize() shouldBe ""
        }
    }
})
