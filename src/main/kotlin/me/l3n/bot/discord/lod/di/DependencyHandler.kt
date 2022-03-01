package me.l3n.bot.discord.lod.di

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.addFileSource
import com.sksamuel.hoplite.addMapSource
import dev.kord.core.Kord
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import me.l3n.bot.discord.lod.formatter.DiscordMessageBuilder
import me.l3n.bot.discord.lod.formatter.DiscordMessageBuilderImpl
import me.l3n.bot.discord.lod.formatter.joiner
import me.l3n.bot.discord.lod.model.BotConfig
import me.l3n.bot.discord.lod.model.Config
import me.l3n.bot.discord.lod.model.DiscordConfig
import me.l3n.bot.discord.lod.model.FormatConfig
import me.l3n.bot.discord.lod.service.DiscordBot
import me.l3n.bot.discord.lod.service.DiscordBotImpl
import me.l3n.bot.discord.lod.service.DiscordSender
import me.l3n.bot.discord.lod.service.http.OpggService
import me.l3n.bot.discord.lod.service.http.OpggServiceImpl
import me.l3n.bot.discord.lod.service.http.RiotService
import me.l3n.bot.discord.lod.service.http.RiotServiceImpl
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope
import org.koin.dsl.binds
import org.koin.dsl.module
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.exitProcess


fun runWithDI(additionalConfig: Map<String, Any> = mapOf(), container: () -> Unit) {
    initializeDI(additionalConfig)
    container()
    cleanupDI()
}

private fun initializeDI(additionalConfig: Map<String, Any> = mapOf()) {
    startKoin {
        printLogger()

        modules(
            module { single { additionalConfig } },
            configModule,
            formatModule,
            botModule,
            apiModule,
        )
    }
}

private fun cleanupDI() = stopKoin()

private val configModule = module {
    single {
        val configLoader = ConfigLoader.Builder()
            .addMapSource(get())
            .addFileSource(File("application.yaml"), true)
            .addDefaultSources()
            .build()

        configLoader.loadConfigOrThrow<Config>()
    }

    single { get<Config>().bot }
    single { get<Config>().riot }
    single { get<Config>().discord }
    single { get<BotConfig>().debug }
    single { get<BotConfig>().format }
}

private val formatModule = module {
    single { get<FormatConfig>().messageStyle.joiner() }
}

private val botModule = module {
    single { runBlocking { Kord(get<DiscordConfig>().token) } }
    single<DiscordMessageBuilder> { DiscordMessageBuilderImpl(get(), get()) }
    single(createdAtStart = true) {
        DiscordBotImpl(get(),
            get(),
            get(),
            get())
    } binds arrayOf(DiscordBot::class, DiscordSender::class)
}

private val apiModule = module {
    factory { (logger: Logger) ->
        HttpClient {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
            HttpResponseValidator {
                handleResponseException { exception ->
                    if (exception !is ResponseException)
                        return@handleResponseException

                    val response = exception.response
                    val request = response.call.request

                    logger.error(
                        "HTTP Error",
                        "${request.method.value} -> ${response.status}",
                        request.url
                    )

                    exitProcess(1)
                }
            }
        }
    }

    single<RiotService> { RiotServiceImpl(get(), getHttpClient<RiotServiceImpl>()) }
    single<OpggService> { OpggServiceImpl(getHttpClient<OpggServiceImpl>()) }
}

private inline fun <reified T : Any> Scope.getHttpClient() =
    get<HttpClient>(parameters = { parametersOf(LoggerFactory.getLogger(T::class.java)) })
