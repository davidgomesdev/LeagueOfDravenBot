package me.l3n.bot.discord.lod.collection


class CaseInsensitiveMap<V> : LinkedHashMap<String, V>() {
    override operator fun get(key: String): V? = super.get(key.lowercase())

    override fun put(key: String, value: V): V? = super.put(key.lowercase(), value)
}

fun <V> caseInsensitiveMapOf(vararg pairs: Pair<String, V>): MutableMap<String, V> =
    if (pairs.isNotEmpty()) pairs.toMap(CaseInsensitiveMap()) else CaseInsensitiveMap()
