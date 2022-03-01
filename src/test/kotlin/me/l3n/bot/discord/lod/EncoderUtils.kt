package me.l3n.bot.discord.lod

import com.google.gson.GsonBuilder

fun Any.toJson(): String = GsonBuilder().create().toJson(this)
