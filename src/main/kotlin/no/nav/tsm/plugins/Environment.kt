package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.config.ApplicationConfig
import no.nav.tsm.sykmeldinger.kafka.aiven.KafkaEnvironment.Companion.getEnvVar
import java.util.Properties

class Environment(
    val migratorJdbcUrl: String,
    val migratorDbUser: String,
    val migratorDbPassword: String,
    val kafkaConfig: Properties,
    val hostname: String,
    val registerDBConnectionName: String,
    val registerDBUsername: String,
    val registerDBPassword: String,
    val registerDBName: String,
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "migrator"),
    val regdumpTopic: String = "tsm.regdump",
    val okSykmeldingTopic: String = "teamsykmelding.ok-sykmelding",
    val avvistSykmeldingTopic: String = "teamsykmelding.avvist-sykmelding",
    val manuellSykmeldingTopic: String = "teamsykmelding.manuell-behandling-sykmelding",
    val gamleSykmeldingTopic: String = "teamsykmelding.gamle-sykmeldinger",
    val migrertSykmeldingTopic: String = "tsm.migrert-sykmelding",
    val sykmeldingHistoriskTopic: String = "teamsykmelding.sykmelding-historisk",
)

private fun ApplicationConfig.requiredEnv(name: String) =
    propertyOrNull(name)?.getString()
        ?: throw IllegalArgumentException("Missing required environment variable $name")
fun Application.createEnvironment(): Environment {
    val host = environment.config.requiredEnv("ktor.database.dbHost")
    val port = environment.config.requiredEnv("ktor.database.dbPort")
    val database = environment.config.requiredEnv("ktor.database.dbName")
    return Environment(
        migratorJdbcUrl = "jdbc:postgresql://$host:$port/$database",
        migratorDbUser = environment.config.requiredEnv("ktor.database.dbUser"),
        migratorDbPassword = environment.config.requiredEnv("ktor.database.dbPassword"),
        hostname = environment.config.host,
        registerDBConnectionName = environment.config.requiredEnv("register.db.connectionName"),
        registerDBUsername = environment.config.requiredEnv("register.db.username"),
        registerDBPassword = environment.config.requiredEnv("register.db.password"),
        registerDBName = environment.config.requiredEnv("register.db.name"),
        kafkaConfig = Properties().apply {
            environment.config.config("ktor.kafka.config").toMap().forEach {
                this[it.key] = it.value
            }
        },
    )
}
