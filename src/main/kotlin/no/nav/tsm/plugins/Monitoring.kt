package no.nav.tsm.plugins

import io.ktor.http.*
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.*
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

fun Application.configureMonitoring() {
    val collectorRegistry = CollectorRegistry.defaultRegistry
    routing {
        get("/internal/is_alive") {
            call.respond(HttpStatusCode.OK)
        }
        get("/internal/is_ready") {
            call.respond(HttpStatusCode.OK)
        }
        get("/internal/prometheus") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: setOf()
            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
            }
        }
    }
}
