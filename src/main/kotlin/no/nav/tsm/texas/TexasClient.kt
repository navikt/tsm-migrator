package no.nav.tsm.texas

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.application.Application

data class TexasResponse(
    val access_token: String,
    val expires_in: Int,
    val token_type: String,
)

data class TexasRequest(
    val identity_provider: String,
    val target: String,
)
class TexasClient(
    private val tokenEndpoint: String,
    private val httpClient: HttpClient,
    private val tsmPdlScope: String,
    ) {
    suspend fun getAccessToken(): String {

        val requestBody = TexasRequest(
            identity_provider = "azuread",
            target = tsmPdlScope,
        )

        return httpClient.post(tokenEndpoint) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body<TexasResponse>().access_token

    }
}
