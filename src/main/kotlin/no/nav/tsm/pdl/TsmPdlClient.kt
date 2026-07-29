package no.nav.tsm.pdl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import no.nav.tsm.ktor.auth.texas.TexasClient
import no.nav.tsm.ktor.auth.texas.TexasToken

data class PdlPerson(
    val identer: List<Ident>,
)

data class Ident(
    val ident: String,
    val gruppe: IDENT_GRUPPE,
    val historisk: Boolean,
)

enum class IDENT_GRUPPE {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID,
}

class TsmPdlClient(
    val texasClient: TexasClient,
    val httpClient: HttpClient,
) {
    private val tsmPdlUrl = "http://tsm-pdl-cache"

    suspend fun getAktorId(fnr: String): String {
        val (token) = getToken()
        val response = httpClient.get("$tsmPdlUrl/api/person") {
            bearerAuth(token)
            header("Ident", fnr)
        }.body<PdlPerson>()

        return response.identer.single { it.gruppe == IDENT_GRUPPE.AKTORID && !it.historisk }.ident
    }

    suspend fun getToken(): TexasToken = texasClient.entraIdToken("tsm", "tsm-pdl-cache")
}
