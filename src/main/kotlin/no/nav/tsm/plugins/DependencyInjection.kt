package no.nav.tsm.plugins

import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson3.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.ktor.auth.texas.Texas
import no.nav.tsm.ktor.clients.pdl.PdlPlugin

fun Application.configureDependencyInjection() {
    val env = createEnvironment()

    install(PdlPlugin)

    dependencies {
        provide<HttpClient> { configureBaseHttpClient() }
        provide<Environment> { env }
        provide(Texas::class)
    }
}

private fun configureBaseHttpClient(): HttpClient = HttpClient(Apache5) {
    install(ContentNegotiation) {
        jackson {}
    }
}
