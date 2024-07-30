package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.host
import io.ktor.server.config.ApplicationConfig
import java.util.Properties

class Environment(
    val kafkaConfig: Properties,
    val hostname: String,
    val okSykmeldingTopic: String = "teamsykmelding.ok-sykmelding",
    val avvistSykmeldingTopic: String = "teamsykmelding.avvist-sykmelding",
    val manuellSykmeldingTopic: String = "teamsykmelding.manuell-behandling-sykmelding",
    val migrertSykmeldingTopic: String = "tsm.migrert-sykmelding",
    val sykmeldingerInputTopic: String = "tsm.sykmeldinger-input",
)

private fun ApplicationConfig.requiredEnv(name: String) =
    propertyOrNull(name)?.getString()
        ?: throw IllegalArgumentException("Missing required environment variable $name")
fun Application.createEnvironment(): Environment {
    return Environment(
        kafkaConfig = Properties().apply {
            environment.config.config("ktor.kafka.config").toMap().forEach {
                this[it.key] = it.value
            }
        },
        hostname = environment.config.host,
    )
}
