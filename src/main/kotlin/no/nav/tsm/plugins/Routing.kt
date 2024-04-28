package no.nav.tsm.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.database.MigrertSykmeldingService
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory


fun Application.configureRouting() {
    val migrertSykmeldingService by inject<MigrertSykmeldingService>()
    val logger = LoggerFactory.getLogger(MigrertSykmeldingService::class.java)
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        var running = false
        post("/api/migrert") {
            if (running) {
                call.respond(HttpStatusCode.Conflict, mapOf("message" to "Job is already running"))
                return@post
            }
            call.respond(HttpStatusCode.Accepted, mapOf("running" to true))
            running = true
            GlobalScope.launch(Dispatchers.IO) {
                logger.info("Getting migrerte sykmeldinger from DB to move to topic")
                try {
                    migrertSykmeldingService.selectSykmeldingerAndProduce()
                } catch (e: Exception) {
                    logger.error("Failed to get migrerte sykmeldinger from migrator DB", e)
                } finally {
                    running = false
                }
            }
        }
    }
}
