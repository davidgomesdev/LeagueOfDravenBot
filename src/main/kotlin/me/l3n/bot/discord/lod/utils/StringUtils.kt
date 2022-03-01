package me.l3n.bot.discord.lod.utils

// Required own implementation since Kotlin's `capitalized()` is deprecated (and they recommend this impl)
fun String.capitalize() = replaceFirstChar(Char::titlecase)
