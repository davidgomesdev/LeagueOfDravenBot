package me.l3n.bot.discord.lod.formatter

import me.l3n.bot.discord.lod.model.Champion
import me.l3n.bot.discord.lod.model.MessageStyle

fun MessageStyle.joiner(): MessageJoiner = when (this) {
    MessageStyle.EmojisOnOwnLine -> emojisOnOwnLineJoiner
    MessageStyle.SameLine -> sameLineJoiner
    MessageStyle.SeparateLines -> separateLinesJoiner
    MessageStyle.EmojisOnly -> emojisOnlyJoiner
}

fun interface MessageJoiner {
    fun joinToText(
        emojis: Map<String, String>?,
        champions: List<Champion>,
    ): String
}

private val emojisOnOwnLineJoiner = MessageJoiner { emojis, champions ->
    var text = champions.joinToString(" ") { champ -> champ.name }

    if (emojis != null) {
        val emojisText = champions.joinToString(" ") { champ ->
            emojis[champ.id] ?: ""
        }

        if (emojisText.isNotEmpty())
            text += "\n$emojisText"
    }

    text
}

private val sameLineJoiner = MessageJoiner { emojis, champions ->
    val text = champions.joinToString(" ") { champ ->
        val name = champ.name
        val emoji = emojis?.get(champ.id)

        if (emoji != null) {
            "$emoji $name"
        } else
            name
    }

    text
}

private val separateLinesJoiner = MessageJoiner { emojis, champions ->
    val text = champions.joinToString(" ") { champ ->
        val name = champ.name
        val emoji = emojis?.get(champ.id)

        if (emoji != null) {
            "$emoji $name\n"
        } else
            "$name\n"
    }

    text
}

private val emojisOnlyJoiner = MessageJoiner { emojis, champions ->
    if (emojis == null) {
        champions.joinToString(" ") { champ -> champ.name }
    } else {
        champions.joinToString(" ") { champ ->
            emojis[champ.id] ?: champ.name
        }
    }
}
