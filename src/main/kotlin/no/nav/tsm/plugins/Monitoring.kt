package no.nav.tsm.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureMonitoring() {


    routing {
        get("/internal/is_alive") {
            call.respond(HttpStatusCode.OK)
        }
        get("/internal/is_ready") {
            call.respond(HttpStatusCode.OK)
        }
    }
}
