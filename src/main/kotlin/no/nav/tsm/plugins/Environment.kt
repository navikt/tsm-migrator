package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.host
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.nais.getRuntimeCluster
import java.util.Properties

class Runtime(val env: RuntimeCluster, val name: String)

class Environment(
    val runtime: Runtime,
    val kafkaConfig: Properties,
    val hostname: String,
    val okSykmeldingTopic: String = "teamsykmelding.ok-sykmelding",
    val avvistSykmeldingTopic: String = "teamsykmelding.avvist-sykmelding",
    val manuellSykmeldingTopic: String = "teamsykmelding.manuell-behandling-sykmelding",
    val teamsykmeldingSykmeldingTopic: String = "tsm.teamsykmelding-sykmeldinger",
    val manuellTilbakedateringTopic: String = "teamsykmelding.sykmelding-manuell",
    val tsmSykmeldingTopic: String = "tsm.sykmeldinger",
)

fun Application.createEnvironment(): Environment {
    return Environment(
        Runtime(
            env = getRuntimeCluster(),
            name = environment.config.property("app.name").getString(),
        ),
        kafkaConfig = Properties().apply {
            environment.config.config("kafka.config").toMap().forEach {
                this[it.key] = it.value
            }
        },
        hostname = environment.config.host,
    )
}
