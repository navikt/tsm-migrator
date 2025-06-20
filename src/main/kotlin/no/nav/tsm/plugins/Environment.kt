package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.host
import no.nav.tsm.sykmeldinger.kafka.aiven.KafkaEnvironment.Companion.getEnvVar
import java.util.Properties

class Environment(
    val kafkaConfig: Properties,
    val hostname: String,
    val okSykmeldingTopic: String = "teamsykmelding.ok-sykmelding",
    val avvistSykmeldingTopic: String = "teamsykmelding.avvist-sykmelding",
    val manuellSykmeldingTopic: String = "teamsykmelding.manuell-behandling-sykmelding",
    val teamsykmeldingSykmeldingTopic: String = "tsm.teamsykmelding-sykmeldinger",
    val cluster: String = getEnvVar("NAIS_CLUSTER_NAME")
)

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
