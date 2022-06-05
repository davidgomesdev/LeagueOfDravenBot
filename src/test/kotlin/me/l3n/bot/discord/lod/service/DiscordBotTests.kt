package me.l3n.bot.discord.lod.service

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Embed
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.supplier.EntitySupplier
import dev.kord.core.supplier.EntitySupplyStrategy
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.*
import kotlinx.coroutines.flow.asFlow
import me.l3n.bot.discord.lod.formatter.DiscordMessageBuilder
import me.l3n.bot.discord.lod.model.BotConfig
import me.l3n.bot.discord.lod.model.DiscordConfig
import me.l3n.bot.discord.lod.model.Role
import me.l3n.bot.discord.lod.models.Champs
import me.l3n.bot.discord.lod.models.Rotations


class DiscordBotTests : ShouldSpec({

    isolationMode = IsolationMode.InstancePerTest

    val infoChannelID = Snowflake(1337)
    val errorChannelID = Snowflake(404)
    val brokenChannelID = Snowflake(666)

    val dummyDiscordConfig = DiscordConfig(
        "aaBBcc", infoChannelID.value, errorChannelID.value, null
    )
    val dummyDiscordConfigWithBroken =
        DiscordConfig("aaBBcc", infoChannelID.value, errorChannelID.value, brokenChannelID.value)

    val dummyBotConfig = BotConfig()

    val infoChannelMock: TextChannel = mockk {}
    val errorChannelMock: TextChannel = mockk {}
    val brokenChannelMock: TextChannel = mockk {}

    val rotationMessageMock: Message = mockk {}
    val rotationEmbedMock: Embed = mockk {}
    val botUserMock: User = mockk {}

    val kordMock: Kord =
        mockk {
            val supplier = mockk<EntitySupplier>()

            val strategy = mockk<EntitySupplyStrategy<*>> {
                every { supply(any()) } returns supplier
            }

            every { resources } returns mockk {
                every { defaultStrategy } returns strategy
            }
            every { defaultSupplier } returns supplier

            coEvery { getChannelOf<TextChannel>(infoChannelID) } returns infoChannelMock
            coEvery { getChannelOf<TextChannel>(errorChannelID) } returns errorChannelMock
            coEvery { getChannelOf<TextChannel>(brokenChannelID) } returns brokenChannelMock
        }
    val messageBuilderMock = mockk<DiscordMessageBuilder>()

    lateinit var bot: DiscordBot

    context("at initialization") {
        context("without broken list") {
            beforeEach {
                bot = DiscordBotImpl(dummyDiscordConfig,
                    dummyBotConfig,
                    kordMock,
                    messageBuilderMock)
            }

            should("get info and error channels") {
                coVerifySequence {
                    kordMock.getChannelOf<Channel>(infoChannelID)
                    kordMock.getChannelOf<Channel>(errorChannelID)
                }
            }
        }

        context("with broken list") {
            beforeEach {
                bot = DiscordBotImpl(
                    dummyDiscordConfigWithBroken,
                    dummyBotConfig.copy(notifyWhenBroken = true),
                    kordMock,
                    messageBuilderMock
                )
            }

            should("get info, error and broken channels") {
                coVerifySequence {
                    kordMock.getChannelOf<Channel>(infoChannelID)
                    kordMock.getChannelOf<Channel>(errorChannelID)
                    kordMock.getChannelOf<Channel>(brokenChannelID)
                }
            }
        }
    }

    context("without messages") {
        beforeEach {
            bot = DiscordBotImpl(dummyDiscordConfig,
                dummyBotConfig,
                kordMock,
                messageBuilderMock)

            coEvery { kordMock.getSelf() } returns botUserMock

            coEvery { infoChannelMock.messages } returns listOf<Message>().asFlow()
        }

        should("say rotation is newer") {
            val isNewer = bot.isRotationNewer(Rotations.withoutLowLevel)

            isNewer.shouldBeTrue()

            coVerifyOrder {
                infoChannelMock.messages
            }
        }
    }

    context("embed without fields") {
        beforeEach {
            bot = DiscordBotImpl(dummyDiscordConfig,
                dummyBotConfig,
                kordMock,
                messageBuilderMock)

            coEvery { rotationMessageMock.author } returns botUserMock
            coEvery { kordMock.getSelf() } returns botUserMock

            coEvery { rotationEmbedMock.fields } returns listOf()
            coEvery { rotationMessageMock.embeds } returns listOf(rotationEmbedMock)
            coEvery { infoChannelMock.messages } returns listOf(rotationMessageMock).asFlow()
        }

        should("say rotation is newer") {
            val isNewer = bot.isRotationNewer(Rotations.withoutLowLevel)

            isNewer.shouldBeTrue()

            coVerifyOrder {
                infoChannelMock.messages
                rotationMessageMock.author
                kordMock.getSelf()
                rotationMessageMock.embeds
                rotationEmbedMock.fields
            }
        }
    }

    context("up to date rotation message") {
        val fieldMocks = Rotations.withoutLowLevel.roledChampions.map { (role, champs) ->
            mockk<Embed.Field> {
                every { name } returns role.toDisplayName()
                every { value } returns champs.joinToString()
            }
        }

        beforeEach {
            bot = DiscordBotImpl(dummyDiscordConfig,
                dummyBotConfig,
                kordMock,
                messageBuilderMock)

            coEvery { rotationMessageMock.author } returns botUserMock
            coEvery { kordMock.getSelf() } returns botUserMock

            coEvery { rotationEmbedMock.fields } returns fieldMocks
            coEvery { rotationMessageMock.embeds } returns listOf(rotationEmbedMock)
            coEvery { infoChannelMock.messages } returns listOf(rotationMessageMock).asFlow()
        }

        should("say rotation is outdated") {
            val isNewer = bot.isRotationNewer(Rotations.withoutLowLevel)

            isNewer.shouldBeFalse()

            coVerify {
                infoChannelMock.messages
                rotationMessageMock.author
                kordMock.getSelf()
                rotationMessageMock.embeds
                rotationEmbedMock.fields
            }
        }
    }

    context("outdated rotation message") {
        context("with embed lacking a role field") {
            val fieldMocks = Rotations.withoutLowLevel.roledChampions
                .filterNot { (role, _) -> role == Role.Mid }
                .map { (role, champs) ->
                    mockk<Embed.Field> {
                        every { name } returns role.toDisplayName()
                        every { value } returns champs.joinToString()
                    }
                }

            beforeEach {
                bot = DiscordBotImpl(dummyDiscordConfig,
                    dummyBotConfig,
                    kordMock,
                    messageBuilderMock)

                coEvery { rotationMessageMock.author } returns botUserMock
                coEvery { kordMock.getSelf() } returns botUserMock

                coEvery { rotationEmbedMock.fields } returns fieldMocks
                coEvery { rotationMessageMock.embeds } returns listOf(rotationEmbedMock)
                coEvery { infoChannelMock.messages } returns listOf(rotationMessageMock).asFlow()
            }

            should("say rotation is newer") {
                val isNewer = bot.isRotationNewer(Rotations.withoutLowLevel)

                isNewer.shouldBeTrue()

                coVerifyOrder {
                    infoChannelMock.messages
                    rotationMessageMock.author
                    kordMock.getSelf()
                    rotationMessageMock.embeds
                    rotationEmbedMock.fields
                }
            }
        }

        context("with rotation missing a role") {
            val fieldMocks = Rotations.withoutLowLevel.roledChampions
                .map { (role, champs) ->
                    mockk<Embed.Field> {
                        every { name } returns role.toDisplayName()
                        every { value } returns champs.joinToString()
                    }
                }.plus(mockk<Embed.Field> {
                    every { name } returns Role.Jungle.toDisplayName()
                    every { value } returns Champs.NoIcon.nami.name
                })

            beforeEach {
                bot = DiscordBotImpl(dummyDiscordConfig,
                    dummyBotConfig,
                    kordMock,
                    messageBuilderMock)

                coEvery { rotationMessageMock.author } returns botUserMock
                coEvery { kordMock.getSelf() } returns botUserMock

                coEvery { rotationEmbedMock.fields } returns fieldMocks
                coEvery { rotationMessageMock.embeds } returns listOf(rotationEmbedMock)
                coEvery { infoChannelMock.messages } returns listOf(rotationMessageMock).asFlow()
            }

            should("say rotation is newer") {
                val isNewer = bot.isRotationNewer(Rotations.withoutLowLevel)

                isNewer.shouldBeTrue()

                coVerifyOrder {
                    infoChannelMock.messages
                    rotationMessageMock.author
                    kordMock.getSelf()
                    rotationMessageMock.embeds
                    rotationEmbedMock.fields
                }
            }
        }

        context("with last rotation being invalid / all unknown") {
            val champions = Rotations.withoutLowLevel.roledChampions
                .values.flatten().joinToString(" ")
            val embedMocks = listOf(mockk<Embed.Field> {
                every { name } returns "Unknown"
                every { value } returns champions
            })

            beforeEach {
                bot = DiscordBotImpl(dummyDiscordConfig,
                    dummyBotConfig,
                    kordMock,
                    messageBuilderMock)

                coEvery { rotationMessageMock.author } returns botUserMock
                coEvery { kordMock.getSelf() } returns botUserMock

                coEvery { rotationEmbedMock.fields } returns embedMocks
                coEvery { rotationMessageMock.embeds } returns listOf(rotationEmbedMock)
                coEvery { infoChannelMock.messages } returns listOf(rotationMessageMock).asFlow()
            }

            should("say rotation is outdated") {
                val isNewer = bot.isRotationNewer(Rotations.withoutLowLevel)

                isNewer.shouldBeTrue()

                coVerifyOrder {
                    infoChannelMock.messages
                    rotationMessageMock.author
                    kordMock.getSelf()
                    rotationMessageMock.embeds
                    rotationEmbedMock.fields
                }
            }
        }
    }
})
