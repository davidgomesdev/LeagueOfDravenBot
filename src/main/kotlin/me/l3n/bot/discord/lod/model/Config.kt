package me.l3n.bot.discord.lod.model

data class Config(
    val discord: DiscordConfig,
    val riot: RiotConfig,
    val bot: BotConfig,
)

data class BotConfig(
    /**
     * This is being ignored, since there's no need (who's low level in 2022?)
     * and it doesn't look pretty.
     */
    val showLowLevelRotation: Boolean = false,
    val mentionEveryone: Boolean = true,
    val statusMessage: String = "for the current rotation",
    val notifyWhenBroken: Boolean = false,
    val brokenListLimit: Int = 0,
    val debug: DebugConfig = DebugConfig(),
    val format: FormatConfig = FormatConfig(),
)

data class FormatConfig(
    val newRotationMessage: String = "New champion rotation!",
    val brokenMessages: List<String> = listOf(),
    val themes: Map<String, ThemeConfig> = mapOf("default" to ThemeConfig()),
    val embedThumbnailURL: String? = null,
    val messageStyle: MessageStyle = MessageStyle.SameLine,
)

data class ThemeConfig(
    val roleColor: Int = 0x389faf,
    val embedColor: Int = 0x3a7c85,
)

data class DebugConfig(
    val alwaysSendRotation: Boolean = false,
    val sendEmojis: Boolean = true,
    /**
     * This is a way to test the emojis feature without being rate limited,
     * only works when [sendEmojis] is `true`.
     *
     * Sends all if it is null.
     */
    val maxEmojis: Int? = null,
)

data class DiscordConfig(
    val token: String,
    val infoChannelID: Long,
    val errorChannelID: Long,
    val brokenListChannelID: Long?,
)

data class RiotConfig(
    val token: String,
)

enum class MessageStyle {
    EmojisOnOwnLine,
    SameLine,
    SeparateLines,
    EmojisOnly,
}
