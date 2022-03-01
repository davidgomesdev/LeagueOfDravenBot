package me.l3n.bot.discord.lod.logger

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import me.l3n.bot.discord.lod.service.DiscordSender
import me.l3n.bot.discord.lod.service.ErrorMessage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class DiscordLogger : AppenderBase<ILoggingEvent?>(), KoinComponent {

    private val discordBot: DiscordSender by inject()

    override fun append(event: ILoggingEvent?) {
        if (event == null) return

        if (event.argumentArray == null) {
            discordBot.sendErrorMessage(event.loggerName, event.message)
        } else {
            val args = event.argumentArray.map { arg -> arg.toString() }.toMutableList()
            val title = args.removeFirst()

            discordBot.sendErrorMessage(event.loggerName, event.message, ErrorMessage(title, args))
        }
    }
}
