package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.host
import io.ktor.server.config.ApplicationConfig
import no.nav.tsm.sykmeldinger.kafka.aiven.KafkaEnvironment.Companion.getEnvVar
import java.util.Properties

class Environment(
    val kafkaConfig: Properties,
    val hostname: String,
    val okSykmeldingTopic: String = "teamsykmelding.ok-sykmelding",
    val avvistSykmeldingTopic: String = "teamsykmelding.avvist-sykmelding",
    val manuellSykmeldingTopic: String = "teamsykmelding.manuell-behandling-sykmelding",
    val migrertSykmeldingTopic: String = "tsm.migrert-sykmelding",
    val sykmeldingerInputTopic: String = "tsm.sykmeldinger-input",
    val sykmeldingOutputTopic: String = "tsm.sykmelding-raw",
    val teamsykmeldingSykmeldingTopic: String = "tsm.teamsykmelding-sykmeldinger",
    val teamsykmeldingSykmeldingAvroTopic: String = "tsm.teamsykmelding-sykmeldinger-avro",
    val cluster: String = getEnvVar("NAIS_CLUSTER_NAME")
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
