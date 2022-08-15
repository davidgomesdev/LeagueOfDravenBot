package me.l3n.bot.discord.lod.formatter

import dev.kord.common.Color
import dev.kord.core.cache.data.EmojiData
import dev.kord.rest.builder.message.EmbedBuilder
import me.l3n.bot.discord.lod.model.Champion
import me.l3n.bot.discord.lod.model.FormatConfig
import me.l3n.bot.discord.lod.model.Rotation
import me.l3n.bot.discord.lod.service.ErrorMessage
import org.slf4j.LoggerFactory

private const val ERROR_EMBED_COLOR = 0xED4245

interface DiscordMessageBuilder {
    fun createRotationEmbed(
        rotation: Rotation,
        emojis: Map<String, EmojiData>? = null,
        sendBrokenMessage: Boolean = false,
        embedColor: Int
    ): EmbedBuilder

    fun createTextWithEmojisAndChamps(
        emojis: Map<String, EmojiData>? = null,
        champions: List<Champion>,
    ): String

    fun createBuilderForErrorMessage(
        loggerName: String,
        message: String,
        details: ErrorMessage? = null,
    ): EmbedBuilder
}

class DiscordMessageBuilderImpl(
    private val formatConfig: FormatConfig,
    private val messageJoiner: MessageJoiner,
) : DiscordMessageBuilder {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun createRotationEmbed(
        rotation: Rotation,
        emojis: Map<String, EmojiData>?,
        sendBrokenMessage: Boolean,
        embedColor: Int
    ): EmbedBuilder = EmbedBuilder().apply {
        color = Color(embedColor)
        title = formatConfig.newRotationMessage

        val thumbnailURL = formatConfig.embedThumbnailURL

        if (thumbnailURL != null) thumbnail { url = thumbnailURL }

        logger.debug("""Creating text with emoji style "${formatConfig.messageStyle}"""")
        rotation.roledChampions.forEach { (role, champions) ->
            val championsText = createTextWithEmojisAndChamps(emojis, champions)

            if (championsText.isNotEmpty()) logger.info("Created for ${role.toDisplayName()}")
            else logger.info("No champions for ${role.toDisplayName()}")

            val roleName = role.toDisplayName()

            field {
                name = roleName
                if (championsText.isNotEmpty()) value = championsText
            }
        }

        if (sendBrokenMessage && formatConfig.brokenMessages.isNotEmpty())
            field { value = getBrokenMessage() }
    }

    private fun getBrokenMessage() = "||**⚠ ${formatConfig.brokenMessages.random()}**||"

    override fun createTextWithEmojisAndChamps(
        emojis: Map<String, EmojiData>?,
        champions: List<Champion>,
    ): String {
        val emojisText = emojis?.mapValues { (_, emoji) -> "<:${emoji.name}:${emoji.id.asString}>" }

        return messageJoiner.joinToText(emojisText, champions)
    }

    override fun createBuilderForErrorMessage(
        loggerName: String,
        message: String,
        details: ErrorMessage?,
    ): EmbedBuilder = EmbedBuilder().apply {
        color = Color(ERROR_EMBED_COLOR)
        title = message

        if (details != null)
            field {
                name = details.title
                value = details.details.reduce { total, current ->
                    "$total\n$current"
                }
            }

        footer {
            text = loggerName
        }
    }
}