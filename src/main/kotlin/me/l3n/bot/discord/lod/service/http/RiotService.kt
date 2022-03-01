package me.l3n.bot.discord.lod.service.http

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import me.l3n.bot.discord.lod.model.Champion
import me.l3n.bot.discord.lod.model.RiotConfig
import me.l3n.bot.discord.lod.model.Role
import me.l3n.bot.discord.lod.model.Rotation
import org.slf4j.LoggerFactory

internal const val LEAGUE_URL = "https://euw1.api.riotgames.com/lol"
internal const val LEAGUE_DATA_URL = "https://ddragon.leagueoflegends.com"

internal const val VERSIONS_ENDPOINT = "api/versions.json"
internal const val GET_CURRENT_ROTATION_ENDPOINT = "platform/v3/champion-rotations"
internal const val CDN_ENDPOINT = "cdn"
internal const val GET_ALL_CHAMPIONS_ENDPOINT = "data/en_US/champion.json"
internal const val GET_CHAMPION_ICON = "img/champion"

interface RiotService {
    suspend fun getCurrentRotation(roles: Map<String, Role>?, getLowLevel: Boolean = false): Rotation
}

class RiotServiceImpl(
    private val riotConfig: RiotConfig,
    private val httpClient: HttpClient,
) : RiotService {

    private val logger = LoggerFactory.getLogger(javaClass)

    private var version: String

    init {
        runBlocking {
            version = getLatestVersion()
        }
    }

    suspend fun getLatestVersion(): String {
        val allVersions = httpClient.get<List<String>>("$LEAGUE_DATA_URL/$VERSIONS_ENDPOINT")

        return allVersions.first()
    }

    override suspend fun getCurrentRotation(roles: Map<String, Role>?, getLowLevel: Boolean): Rotation {
        val rotationIds =
            httpClient.get<ChampionRotationResponse>("$LEAGUE_URL/$GET_CURRENT_ROTATION_ENDPOINT") {
                header("X-Riot-Token", riotConfig.token)
            }

        logger.info("Fetched rotation champions' ids")
        logger.debug(rotationIds.freeChampionIds.joinToString(prefix = "Free rotation ids: "))

        val champions = trimChampionsName(getAllChampions())

        logger.info("Fetched all champions")
        logger.debug("Champion count: ${champions.size}")

        var rotationChampions = filterChampionsOnRotation(
            champions, rotationIds.freeChampionIds
        )
        var rotationChampionsForLowLevel = filterChampionsOnRotation(
            champions, rotationIds.freeChampionIdsForNewPlayers
        )

        rotationChampions = addIconsToChampions(rotationChampions)
        logger.info("Fetched rotation icons")

        val championsWithRole =
            getRoleChampions(roles, rotationChampions)
        logger.info("Added roles on champions")

        var rotation = Rotation(
            champions.values.toList(),
            championsWithRole
        )

        if (getLowLevel) {
            rotationChampionsForLowLevel = addIconsToChampions(rotationChampionsForLowLevel)
            logger.info("Fetched low level rotation icons")

            val championsLowLevelWithRole =
                getRoleChampions(roles, rotationChampionsForLowLevel)
            logger.info("Added roles on low level champion")

            rotation = rotation.copy(roledChampionsForLowLevel = championsLowLevelWithRole)
        }

        return rotation
    }

    suspend fun getAllChampions(): Map<String, Champion> {
        logger.debug("Fetched latest version $version")

        val champions = httpClient.get<ChampionListResponse>(
            "$LEAGUE_DATA_URL/$CDN_ENDPOINT/$version/$GET_ALL_CHAMPIONS_ENDPOINT"
        )

        return champions.data
    }

    infix fun trimChampionsName(champions: Map<String, Champion>): Map<String, Champion> =
        champions.mapValues { (_, champ) ->
            champ.copy(name = champ.name.replace("\\s".toRegex(), ""))
        }

    suspend infix fun addIconsToChampions(champions: Map<String, Champion>): Map<String, Champion> =
        coroutineScope {
            val jobs = champions.map { (_, champ) ->
                async(Dispatchers.IO) {
                    champ.apply { icon = getChampionIcon(champ.id) }
                }
            }

            jobs.awaitAll().associateBy { it.id }
        }

    suspend infix fun getChampionIcon(id: String): List<Byte> =
        httpClient.get<ByteArray>(
            "$LEAGUE_DATA_URL/cdn/$version/$GET_CHAMPION_ICON/$id.png"
        ).toList()

    fun filterChampionsOnRotation(
        champions: Map<String, Champion>,
        rotationIds: List<Int>,
    ): Map<String, Champion> {
        return champions.filter { (_, champion) ->
            rotationIds.contains(champion.key)
        }
    }

    fun getRoleChampions(
        roles: Map<String, Role>?,
        champions: Map<String, Champion>,
    ): Map<Role, List<Champion>> {
        if (roles == null) {
            return mapOf(Role.Unknown to champions.values.toList())
        }

        val sortedRoles = mutableMapOf<Role, MutableList<Champion>>(
            Role.Top to mutableListOf(),
            Role.Jungle to mutableListOf(),
            Role.Mid to mutableListOf(),
            Role.Bot to mutableListOf(),
            Role.Support to mutableListOf(),
        )

        champions.forEach { (key, champion) ->
            val role = roles[key] ?: Role.Unknown

            if (role == Role.Unknown)
                sortedRoles.getOrPut(role) { mutableListOf() }

            sortedRoles[role]?.add(champion)
                ?: logger.error("The role $role has not been found in the sorted roles")
        }

        return sortedRoles
    }
}

internal data class ChampionRotationResponse(
    val freeChampionIds: List<Int>,
    val freeChampionIdsForNewPlayers: List<Int>,
)

internal data class ChampionListResponse(
    val data: Map<String, Champion>,
)
