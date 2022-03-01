package me.l3n.bot.discord.lod.service.http

import io.kotest.assertions.fail
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.coEvery
import io.mockk.mockk
import me.l3n.bot.discord.lod.model.RiotConfig
import me.l3n.bot.discord.lod.model.Role
import me.l3n.bot.discord.lod.models.Champs
import me.l3n.bot.discord.lod.models.Champs.currentRotationIds
import me.l3n.bot.discord.lod.toJson
import kotlin.time.ExperimentalTime

private const val LATEST_VERSION = "1.3.3.7"
private val VERSIONS = listOf(LATEST_VERSION, "1.0", "0.1")

@ExperimentalTime
class RiotServiceTests : ShouldSpec({

    val httpResponseMock = mockk<MockableHttpHandler> {
        coEvery { responseFor("$LEAGUE_DATA_URL/$VERSIONS_ENDPOINT", any()) } returns VERSIONS.toJson()

        coEvery {
            responseFor(
                "$LEAGUE_DATA_URL/$CDN_ENDPOINT/$LATEST_VERSION/$GET_ALL_CHAMPIONS_ENDPOINT",
                any(),
            )
        } returns ChampionListResponse(Champs.noIconsList).toJson()

        coEvery {
            responseFor(
                "$LEAGUE_URL/$GET_CURRENT_ROTATION_ENDPOINT",
                any(),
            )
        } returns ChampionRotationResponse(currentRotationIds, currentRotationIds).toJson()

        Champs.withIconsList.values.forEach { champ ->
            val icon = champ.icon ?: fail("Icon of `${champ.id}` should not be null")

            coEvery {
                responseFor(
                    "$LEAGUE_DATA_URL/cdn/$LATEST_VERSION/$GET_CHAMPION_ICON/${champ.id}.png",
                    any()
                )
            } returns icon.toByteArray()
        }
    }
    val httpClient = MockableHttpHandler.createClient(httpResponseMock)

    context("internal API") {
        val target = RiotServiceImpl(RiotConfig("dummy"), httpClient)

        should("remove spaces from champion names") {
            target.trimChampionsName(Champs.noIconsList).values.forEach { champ ->
                champ.name shouldNotContain " "
            }
        }

        should("filter champions from the rotation list") {
            val filtered =
                target.filterChampionsOnRotation(Champs.noIconsList, Champs.currentRotationIds)

            filtered shouldBe Champs.currentRotation
        }

        should("give all roles with champions") {
            val actual = target.getRoleChampions(Champs.allRolesFilled, Champs.noIconsList)

            actual.keys shouldContain Role.Unknown
            actual.keys shouldContainInOrder listOf(
                Role.Top,
                Role.Jungle,
                Role.Mid,
                Role.Bot,
                Role.Support,
                Role.Unknown
            )
            actual shouldBe mapOf(
                Role.Top to listOf(Champs.NoIcon.lulu),
                Role.Jungle to listOf(Champs.NoIcon.nunu),
                Role.Mid to listOf(Champs.NoIcon.syndra),
                Role.Bot to listOf(Champs.NoIcon.draven),
                Role.Support to listOf(Champs.NoIcon.nami),
                Role.Unknown to listOf(Champs.NoIcon.knox),
            )
        }

        should("give roles with champions, without `Unknown` because there's none") {
            val actual = target.getRoleChampions(Champs.rolesFilledWithoutUnknown, Champs.noIconsList)

            actual.keys shouldNotContain Role.Unknown
            actual.keys shouldContainInOrder listOf(Role.Top, Role.Jungle, Role.Mid, Role.Bot, Role.Support)
            actual shouldBe mapOf(
                Role.Top to listOf(Champs.NoIcon.lulu),
                Role.Jungle to listOf(Champs.NoIcon.nunu),
                Role.Mid to listOf(Champs.NoIcon.syndra),
                Role.Bot to listOf(Champs.NoIcon.draven),
                Role.Support to listOf(Champs.NoIcon.nami, Champs.NoIcon.knox),
            )
        }

        should("give some roles with champions, including `Unknown`") {
            val actual = target.getRoleChampions(Champs.someRolesFilled, Champs.noIconsList)

            actual.keys shouldContain Role.Unknown
            actual.keys shouldContainInOrder listOf(Role.Mid, Role.Bot, Role.Support, Role.Unknown)
            actual shouldBe mapOf(
                Role.Top to listOf(),
                Role.Jungle to listOf(),
                Role.Mid to listOf(Champs.NoIcon.syndra),
                Role.Bot to listOf(Champs.NoIcon.draven, Champs.NoIcon.lulu),
                Role.Support to listOf(Champs.NoIcon.nami, Champs.NoIcon.nunu),
                Role.Unknown to listOf(Champs.NoIcon.knox),
            )
        }

        should("give some roles with champions, without `Unknown` because there's none") {
            val actual =
                target.getRoleChampions(Champs.someRolesFilledWithoutUnknown, Champs.noIconsList)

            actual.keys shouldNotContain Role.Unknown
            actual.keys shouldContainInOrder listOf(Role.Mid, Role.Bot, Role.Support)
            actual shouldBe mapOf(
                Role.Top to listOf(),
                Role.Jungle to listOf(),
                Role.Mid to listOf(Champs.NoIcon.syndra, Champs.NoIcon.knox),
                Role.Bot to listOf(Champs.NoIcon.draven, Champs.NoIcon.lulu),
                Role.Support to listOf(Champs.NoIcon.nami, Champs.NoIcon.nunu),
            )
        }

        context("using HTTP services") {
            should("fetch the latest version in initialization") {
                target.getLatestVersion() shouldBe LATEST_VERSION
            }

            should("get all champions") {
                target.getAllChampions() shouldBe Champs.noIconsList
            }

            should("get champion icon") {
                val case = Champs.WithIcon.draven

                val icon = case.icon ?: fail("`case.icon` must not be null")

                val actual = target.getChampionIcon(case.id)

                actual.shouldContainAll(*icon.toTypedArray())
            }

            should("get champions with icons") {
                val actual = target.addIconsToChampions(Champs.noIconsList)

                actual.keys shouldBe Champs.withIconsList.keys
                actual.values shouldBe Champs.withIconsList.values
            }
        }
    }

    context("public API") {
        should("return rotation champions with their icons") {
            val target = RiotServiceImpl(RiotConfig("dummy"), httpClient)

            val currentRotation = target.getCurrentRotation(Champs.allRolesFilled)

            val champs = currentRotation.roledChampions

            champs.keys.size shouldBe 5
            champs.filterValues { it.isEmpty() }.keys shouldContainExactly listOf(Role.Jungle, Role.Mid, Role.Support)
            champs[Role.Bot]?.shouldHaveSingleElement(Champs.WithIcon.draven)
            champs[Role.Top]?.shouldHaveSingleElement(Champs.WithIcon.lulu)
        }
    }
})
