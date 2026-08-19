package no.nav.tsm.plugins

import io.ktor.server.application.*
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.nais.getRuntimeCluster
import java.util.*

class Runtime(val env: RuntimeCluster, val name: String)

object KafkaTopics {
    val tsmSykmeldingTopic: String = "tsm.sykmeldinger"
    val okSykmeldingTopic: String = "teamsykmelding.ok-sykmelding"
    val avvistSykmeldingTopic: String = "teamsykmelding.avvist-sykmelding"
    val manuellSykmeldingTopic: String = "teamsykmelding.manuell-behandling-sykmelding"
    val manuellTilbakedateringTopic: String = "teamsykmelding.sykmelding-manuell"
}

class Environment(
    val runtime: Runtime,
    val kafkaConfig: Properties,
    val hostname: String,
)

fun Application.createEnvironment(): Environment {
    return Environment(
        Runtime(
            env = getRuntimeCluster(),
            name = environment.config.property("app.name").getString(),
        ),
        kafkaConfig = Properties().apply {
            environment.config.config("ktor.kafka.config").toMap().forEach {
                this[it.key] = it.value
            }
        },
        hostname = environment.config.host,
    )
}
