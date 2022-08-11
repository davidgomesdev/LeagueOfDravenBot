package me.l3n.bot.discord.lod.service

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.createEmoji
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Embed
import dev.kord.core.entity.GuildEmoji
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.firstOrNull
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.Image
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import me.l3n.bot.discord.lod.formatter.DiscordMessageBuilder
import me.l3n.bot.discord.lod.model.*
import me.l3n.bot.discord.lod.utils.hasLetters
import org.slf4j.LoggerFactory


interface DiscordBot {
    /**
     * Sends the current rotation, if newer
     *
     * @return whether rotation was sent
     */
    suspend fun sendNewRotation(rotation: Rotation): Collection<GuildEmoji>?

    suspend fun cleanupOldRotation(rotation: Rotation)

    suspend fun cleanupCurrentRotation(uploadedEmojis: Collection<GuildEmoji>?)

    suspend fun isRotationNewer(rotation: Rotation): Boolean
}

interface DiscordSender {
    fun sendErrorMessage(loggerName: String, message: String, details: ErrorMessage? = null): Message
}

open class DiscordBotImpl(
    discordConfig: DiscordConfig,
    private val botConfig: BotConfig,
    private val discord: Kord,
    private val messageBuilder: DiscordMessageBuilder,
) : DiscordBot, DiscordSender {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val errorChannel: TextChannel
    private val infoChannel: TextChannel
    private val brokenListChannel: TextChannel?

    init {
        infoChannel = getChannelById(discordConfig.infoChannelID)
            ?: throw IllegalArgumentException("The info channel ID provided isn't valid")
        errorChannel = getChannelById(discordConfig.errorChannelID)
            ?: throw IllegalArgumentException("The error channel ID provided isn't valid")

        val brokenListChannelId = discordConfig.brokenListChannelID

        brokenListChannel =
            if (botConfig.notifyWhenBroken && brokenListChannelId != null)
                getChannelById(brokenListChannelId)
                    ?: throw IllegalArgumentException("The broken channel ID provided isn't valid")
            else {
                if (botConfig.notifyWhenBroken)
                    logger.info("No broken channel ID provided")

                null
            }
    }

    /**
     * Sends the current rotation, if newer
     *
     * @return the champion icons uploaded
     */
    override suspend fun sendNewRotation(rotation: Rotation): Collection<GuildEmoji>? {
        val guild = infoChannel.guild

        setAccentColor(guild, botConfig.format.roleColor)

        val newEmojis = uploadEmojis(rotation, guild)
        val newEmojisData = newEmojis?.mapValues { (_, emoji) -> emoji.data }

        val sendBrokenMessage =
            if (brokenListChannel == null) false
            else {
                val brokenChamps = getBrokenList(brokenListChannel)

                logger.debug("Checking if rotation contains a broken champion")
                val containsBrokenChampion = containsBrokenChampion(brokenChamps, rotation)

                if (containsBrokenChampion)
                    logger.info("It does contain a broken champion!")
                else
                    logger.info("No broken champion in the current rotation")

                containsBrokenChampion
            }

        val currentRotationEmbed = messageBuilder.createBuilderForRotationEmbed(
            rotation,
            newEmojisData,
            sendBrokenMessage,
        )
        logger.info("Created rotation embed")

        sendRotation(infoChannel, currentRotationEmbed)
        logger.info("Sent rotation message with embed")

        return newEmojis?.values
    }

    private suspend fun setAccentColor(guild: GuildBehavior, color: Int) {
        // this would be preferred, but Kord's RoleTags is returning nothing
//        val botRole = guild
//            .roles
//            .firstOrNull { role ->
//                role.managed && role.tags?.botId == discord.selfId
//            }
        val botRole = guild.getMember(discord.selfId).roles.firstOrNull { it.managed }

        if (botRole == null) {
            logger.warn("I don't have my own role!!")
            return
        }

        if (botRole.color.rgb != color) {
            botRole.edit {
                this.color = Color(color)
            }
            logger.info("Changed role color")
        }
    }

    override suspend fun cleanupOldRotation(rotation: Rotation) {
        deleteRotationEmojis(infoChannel.guild, rotation.allChampions)
        logger.info("Deleted old rotation emojis")

        deleteAllMessages(infoChannel)
        logger.info("Deleted all channel messages")
    }

    override suspend fun cleanupCurrentRotation(uploadedEmojis: Collection<GuildEmoji>?) {
        if (uploadedEmojis != null) {
            uploadedEmojis.forEach { emoji -> emoji.delete("Leftover of current rotation") }
            logger.info("Deleted current rotation emojis")
        }
    }

    override suspend fun isRotationNewer(rotation: Rotation): Boolean {
        if (botConfig.debug.alwaysSendRotation) {
            logger.debug("Configured to always send rotation")
            return true
        }

        val lastRotation = getLastEmbedIfOwn(infoChannel) ?: return true
        logger.info("Got last message")

        return isEmbedOutdated(rotation, lastRotation)
    }

    // We can't use [lastMessage] because it gets deleted ones as well
    private suspend fun getLastEmbedIfOwn(textChannel: TextChannel) =
        textChannel.messages.take(1).firstOrNull()?.let { lastMessage ->
            if (lastMessage.author != discord.getSelf()) return null

            val embeds = lastMessage.embeds

            embeds.firstOrNull()
        }

    private fun isEmbedOutdated(rotation: Rotation, old: Embed): Boolean {
        if (old.fields.isEmpty()) return true

        val fields = old.getRoleFields()
        val champions = rotation.roledChampions

        if (fields.size != champions.keys.size) return true

        return !areRolesEqual(fields, champions)
    }

    private fun areRolesEqual(fields: List<Embed.Field>, champions: Map<Role, List<Champion>>) =
        champions.all { (role, champs) ->
            val field = fields.first { it.name == role.toDisplayName() }

            champs.all { champ -> field.value.containsChampion(champ) }
        }

    // The name of the "broken message" field is a "ltm" of UTF-8
    private fun Embed.getRoleFields() = fields.filter { it.name.hasLetters() }

    private fun String.containsChampion(champ: Champion) =
        contains(Regex(".*(${champ.name}|${champ.nameForEmoji}).*"))

    private suspend fun uploadEmojis(rotation: Rotation, guild: GuildBehavior): Map<String, GuildEmoji>? {
        val debugConfig = botConfig.debug
        var currentEmojis: Map<String, GuildEmoji>? = null

        if (debugConfig.sendEmojis) {
            if (debugConfig.maxEmojis == null) {
                currentEmojis = uploadRotationEmojis(guild, rotation.roledChampions.values.flatten())
                logger.info("Uploaded new rotation emojis")
            } else {
                currentEmojis = debugUploadRotationEmojis(guild, debugConfig.maxEmojis, rotation.roledChampions)
                logger.info("Uploaded only ${debugConfig.maxEmojis} new rotation emojis (debug mode)")
            }
        }

        return currentEmojis
    }

    private fun containsBrokenChampion(brokenChamps: List<String>, rotation: Rotation): Boolean {
        val containsBroken = rotation.roledChampions.values.flatten()
            .any { champ ->
                brokenChamps.any { broken ->
                    champ.name.equals(broken, ignoreCase = true)
                }
            }

        return containsBroken
    }

    private suspend fun getBrokenList(textChannel: TextChannel): List<String> {
        val brokenListLimit = botConfig.brokenListLimit

        if (brokenListLimit <= 0) return listOf()

        logger.debug("Getting at most $brokenListLimit broken champions")

        val messages = textChannel.messages.take(brokenListLimit)

        return messages.toList(mutableListOf()).map { message -> message.content }
    }

    private suspend fun deleteAllMessages(textChannel: TextChannel) {
        textChannel.messages.collect { msg ->
            msg.delete()
        }
    }

    private suspend fun deleteRotationEmojis(guild: GuildBehavior, allChamps: List<Champion>) {
        guild.withStrategy(EntitySupplyStrategy.cachingRest)
            .emojis.collect { emoji ->
                val wasUploadedByMe = emoji.member == discord.getSelf()
                val isFromRotation = allChamps.any { champ ->
                    emoji.name?.substringBefore('~')
                        .equals(champ.nameForEmoji, ignoreCase = true)
                }

                if (wasUploadedByMe && isFromRotation) {
                    emoji.delete("Leftover of last rotation")
                }
            }
    }

    private suspend fun uploadRotationEmojis(guild: GuildBehavior, champs: List<Champion>): Map<String, GuildEmoji> {
        val emojis = mutableMapOf<String, GuildEmoji>()

        champs.forEach { champ ->
            val newEmoji = uploadChampPngEmoji(guild, champ)

            emojis[champ.id] = newEmoji
        }

        return emojis
    }

    /// This is a way to test the emojis feature without being rate limited
    private suspend fun debugUploadRotationEmojis(
        guild: GuildBehavior,
        maxEmojis: Int,
        champsWithRoles: Map<Role, List<Champion>>,
    ): Map<String, GuildEmoji> {
        val emojis = mutableMapOf<String, GuildEmoji>()

        val debugChampList: List<Champion> = champsWithRoles.firstNotNullOf { (_, champList) ->
            if (champList.size >= maxEmojis)
                champList
            else
                null
        }

        debugChampList.forEachIndexed { i, champ ->
            if (i >= maxEmojis)
                return@forEachIndexed

            val newEmoji = uploadChampPngEmoji(guild, champ)

            emojis[champ.id] = newEmoji
        }

        return emojis
    }

    private suspend fun uploadChampPngEmoji(guild: GuildBehavior, champ: Champion): GuildEmoji {
        val emojiName = champ.nameForEmoji
        val bytes = champ.icon?.toByteArray()
            ?: throw IllegalArgumentException("Attempted to upload a champion emoji without icon")
        val emoji = guild.createEmoji(emojiName, Image.raw(bytes, Image.Format.PNG)) {
            reason = "For current rotation"
        }

        return emoji
    }

    private suspend fun sendRotation(channel: TextChannel, currentRotationEmbed: EmbedBuilder) {
        channel.createMessage {
            if (botConfig.mentionEveryone)
                content = "@everyone"

            embed = currentRotationEmbed
        }
    }

    override fun sendErrorMessage(loggerName: String, message: String, details: ErrorMessage?) =
        runBlocking {
            errorChannel.createMessage {
                embed = messageBuilder.createBuilderForErrorMessage(loggerName, message, details)
            }
        }

    private fun getChannelById(id: Long): TextChannel? = runBlocking {
        discord.getChannelOf<TextChannel>(Snowflake(id))?.withStrategy(EntitySupplyStrategy.rest)
    }
}

data class ErrorMessage(val title: String, val details: List<String>)
