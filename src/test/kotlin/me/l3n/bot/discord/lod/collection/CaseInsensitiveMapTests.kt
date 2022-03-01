package me.l3n.bot.discord.lod.collection

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldNotContain

private val dravenPair = ("draven" to 1337)
private val capitalizedDravenPair = ("Draven" to 1337)
private val capitalizedDravenDifferentValuePair = ("Draven" to 400)
private val hecarimPair = ("Hecarim" to 120)
private val evelynnPair = ("EVELYNN" to 404)

private val differentPairs = arrayOf(dravenPair, hecarimPair, evelynnPair)
private val sameInsensitivePairs = arrayOf(dravenPair, evelynnPair, capitalizedDravenPair)
private val differentInsensitivePairs = arrayOf(
    dravenPair, evelynnPair, capitalizedDravenDifferentValuePair
)

class CaseInsensitiveMapTests : StringSpec({

    "should create map with pairs successfully" {
        val map = shouldNotThrowAny { caseInsensitiveMapOf(*differentPairs) }

        map shouldContain dravenPair
        map shouldContain hecarimPair
        map shouldContain evelynnPair
    }

    "should add pair successfully" {
        val map = caseInsensitiveMapOf(*differentPairs)

        map[dravenPair.first] = dravenPair.second

        map shouldContain dravenPair
        map shouldContain capitalizedDravenPair
    }

    "should get case insensitive pair" {
        val map = caseInsensitiveMapOf(*differentPairs)

        map shouldContain dravenPair
        map shouldContain capitalizedDravenPair
        map shouldContain evelynnPair
    }

    "should replace same case insensitive pair" {
        val map = caseInsensitiveMapOf(*sameInsensitivePairs)

        map shouldContain dravenPair
        map shouldContain capitalizedDravenPair
        map shouldContain evelynnPair
    }

    "should replace different case insensitive pair" {
        val map = caseInsensitiveMapOf(*differentInsensitivePairs)

        map shouldNotContain dravenPair
        map shouldContain capitalizedDravenDifferentValuePair
        map shouldContain evelynnPair
    }
})
