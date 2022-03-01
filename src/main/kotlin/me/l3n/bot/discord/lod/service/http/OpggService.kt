package me.l3n.bot.discord.lod.service.http

import io.ktor.client.*
import io.ktor.client.request.*
import me.l3n.bot.discord.lod.collection.caseInsensitiveMapOf
import me.l3n.bot.discord.lod.model.Role
import org.jsoup.Jsoup


private const val statisticsUrl = "https://euw.op.gg/champion/statistics"
private val urlRegex = Regex("/.*/(?<name>.*)/(?<role>.*)")

interface OpggService {
    suspend fun getChampionRoles(): Map<String, Role>
}

class OpggServiceImpl(private val httpClient: HttpClient) : OpggService {
    override suspend fun getChampionRoles(): Map<String, Role> {
        val html = httpClient.get<String>(statisticsUrl)
        val rootElement = Jsoup.parse(html)

        val championList = rootElement
            .getElementsByTag("aside").first()
            .getElementsByTag("nav").first()
        val championRoles = caseInsensitiveMapOf<Role>()

        championList.getElementsByTag("a").forEach { champion ->
            val champUrl = champion.attr("href")
            val regexResult = urlRegex.matchEntire(champUrl)

            // This can be null, if the champion doesn't have a role
            if (regexResult != null) {
                val devName = regexResult.groups["name"]?.value ?: throw InvalidHTMLException
                val role = regexResult.groups["role"]?.value ?: throw InvalidHTMLException

                championRoles[devName] = Role.convert(role)
            }
        }

        return championRoles
    }
}

object InvalidHTMLException : Exception("Invalid HTML received (possibly changed)")
