package me.l3n.bot.discord.lod.model

import me.l3n.bot.discord.lod.utils.capitalize

data class Rotation(
    val allChampions: List<Champion>,
    val roledChampions: Map<Role, List<Champion>>,
    val roledChampionsForLowLevel: Map<Role, List<Champion>>? = null,
)

data class Champion(val id: String, val key: Int, val name: String, var icon: List<Byte>?) {
    val nameForEmoji get() = name.replace("[^A-Za-z0-9 ]".toRegex(), "")
}

enum class Role {
    Top,
    Jungle,
    Mid,
    Bot,
    Support,
    Unknown;

    companion object {
        fun convert(name: String): Role {
            return when (name.lowercase()) {
                "top" -> Top
                "jungle" -> Jungle
                "middle" -> Mid
                "mid" -> Mid
                "bottom" -> Bot
                "bot" -> Bot
                "adc" -> Bot
                "support" -> Support
                else -> Unknown
            }
        }
    }

    fun toDisplayName(): String {
        return name.capitalize()
    }
}
