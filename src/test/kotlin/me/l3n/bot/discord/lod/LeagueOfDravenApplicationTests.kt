package me.l3n.bot.discord.lod

import io.kotest.core.spec.style.ShouldSpec
import io.mockk.*
import me.l3n.bot.discord.lod.models.Champs
import me.l3n.bot.discord.lod.models.Rotations
import me.l3n.bot.discord.lod.service.DiscordBot
import me.l3n.bot.discord.lod.service.http.InvalidHTMLException
import me.l3n.bot.discord.lod.service.http.OpggService
import me.l3n.bot.discord.lod.service.http.RiotService
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class LeagueOfDravenApplicationTests : ShouldSpec({

    val botMockk: DiscordBot = mockk(relaxUnitFun = true) {}
    val opggMockk: OpggService = mockk {
        coEvery { getChampionRoles() } returns Champs.allRolesFilled
    }
    val riotMockk: RiotService = mockk {
        coEvery {
            getCurrentRotation(
                any(),
                any()
            )
        } returns Rotations.withoutLowLevel
    }

    beforeEach {
        startKoin {
            modules(
                module {
                    single { botMockk }
                    single { opggMockk }
                    single { riotMockk }
                }
            )
        }
    }

    afterEach { clearMocks(botMockk); stopKoin() }

    should("do nothing if rotation is up-to-date") {
        coEvery { botMockk.isRotationNewer(any()) } returns false

        LeagueOfDravenApplication.run()

        coVerify(exactly = 1) { botMockk.isRotationNewer(Rotations.withoutLowLevel) }

        coVerify(inverse = true, ordering = Ordering.ALL) {
            botMockk.cleanupOldRotation(any())
            botMockk.sendNewRotation(any())
            botMockk.cleanupCurrentRotation(any())
        }
    }

    should("send message if there's a new rotation") {
        coEvery { botMockk.isRotationNewer(any()) } returns true
        coEvery { botMockk.sendNewRotation(any()) } returns listOf()

        LeagueOfDravenApplication.run()

        coVerify(exactly = 1) { botMockk.isRotationNewer(Rotations.withoutLowLevel) }

        coVerify(ordering = Ordering.SEQUENCE) {
            botMockk.isRotationNewer(any())
            botMockk.cleanupOldRotation(any())
            botMockk.sendNewRotation(any())
            botMockk.cleanupCurrentRotation(any())
        }
    }

    should("be resilient to OpggService failure") {
        coEvery { opggMockk.getChampionRoles() } throws InvalidHTMLException
        coEvery { botMockk.isRotationNewer(any()) } returns false

        LeagueOfDravenApplication.run()

        coVerify(exactly = 1) { botMockk.isRotationNewer(Rotations.withoutLowLevel) }

        coVerify(inverse = true, ordering = Ordering.SEQUENCE) {
            botMockk.isRotationNewer(any())
            botMockk.cleanupOldRotation(any())
            botMockk.sendNewRotation(any())
            botMockk.cleanupCurrentRotation(any())
        }
    }
})