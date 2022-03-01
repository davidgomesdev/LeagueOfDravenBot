package me.l3n.bot.discord.lod

import com.amazonaws.services.lambda.runtime.Context

class LambdaHandler {
    fun handleRequest(
        event: Map<String, Any>,
        @Suppress("UNUSED_PARAMETER") context: Context?,
    ) = event["warmup"] ?: run { runApp(event) }
}
