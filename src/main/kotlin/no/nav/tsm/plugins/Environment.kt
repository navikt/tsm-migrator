package no.nav.tsm.plugins

import io.ktor.server.application.*
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.nais.getRuntimeCluster

object KafkaTopics {
    val okSykmeldingTopic: String = "teamsykmelding.ok-sykmelding"
    val avvistSykmeldingTopic: String = "teamsykmelding.avvist-sykmelding"
    val manuellSykmeldingTopic: String = "teamsykmelding.manuell-behandling-sykmelding"
    val manuellTilbakedateringTopic: String = "teamsykmelding.sykmelding-manuell"
}

class Runtime(val env: RuntimeCluster, val name: String)

class Environment(
    val runtime: Runtime,
)

fun Application.createEnvironment(): Environment {
    return Environment(
        Runtime(
            env = getRuntimeCluster(),
            name = environment.config.property("app.name").getString(),
        ),
    )
}
