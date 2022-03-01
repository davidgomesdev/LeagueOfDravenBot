package me.l3n.bot.discord.lod

import kotlinx.coroutines.runBlocking
import me.l3n.bot.discord.lod.di.runWithDI
import me.l3n.bot.discord.lod.model.Role
import me.l3n.bot.discord.lod.service.DiscordBot
import me.l3n.bot.discord.lod.service.http.OpggService
import me.l3n.bot.discord.lod.service.http.RiotService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess


fun main() = runApp()

fun runApp(event: Map<String, Any> = mapOf()) = runWithDI(event, LeagueOfDravenApplication::run)

internal object LeagueOfDravenApplication : KoinComponent {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val bot by inject<DiscordBot>()
    private val riotService by inject<RiotService>()
    private val opggService by inject<OpggService>()

    fun run() = runBlocking {
        var roles: Map<String, Role>?

        try {
            roles = opggService.getChampionRoles()
        } catch (ex: Exception) {
            roles = null
            logger.warn("Opgg role fetching failed $ex")
        }
        logger.info("Fetched champion roles")

        val rotation = riotService.getCurrentRotation(roles)

        if (bot.isRotationNewer(rotation)) {
            logger.info("New rotation!")

            bot.cleanupOldRotation(rotation)
            logger.info("Cleaned up old rotation")

            val iconsUploaded = bot.sendNewRotation(rotation)
            logger.info("Sent new rotation")

            bot.cleanupCurrentRotation(iconsUploaded)
            logger.info("Cleaned up newly sent rotation")
        } else
            logger.info("Rotation message is already up to date")
    }
}
