package me.l3n.bot.discord.lod.service.http

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope

/**
 * This is a very shady mock class, but it's the only way to mock the
 * [HttpClient] requests properly.
 */
class MockableHttpHandler {

    // Dummy return just to be mocked
    suspend fun responseFor(url: String, data: String): Any = coroutineScope { "$url$data" }

    companion object {
        fun createClient(handler: MockableHttpHandler) =
            HttpClient(MockEngine) {
                install(JsonFeature) {
                    serializer = GsonSerializer()
                }
                engine {
                    addHandler { request ->
                        val url = request.url.toString()
                        val data = request.body.toByteArray().decodeToString()
                        val responseHeaders =
                            headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

                        when (val responseData = handler.responseFor(url, data)) {
                            is String -> respond(responseData, headers = responseHeaders)
                            is ByteArray -> respond(responseData, headers = responseHeaders)
                            else -> throw IllegalArgumentException(
                                "Allowed types of `responseFor` are ByteArray and String")
                        }
                    }
                }
            }
    }
}
