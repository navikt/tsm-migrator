import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.tsm.plugins.Environment
import no.nav.tsm.plugins.createEnvironment
import no.nav.tsm.smregister.database.SmregisterDatabase
import no.nav.tsm.sykmeldinger.kafka.SykmeldingConsumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import java.sql.DriverManager
import java.util.Properties

fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()

        modules(
            environmentModule(),
            databaseModule,
            sykmeldingConsumer,
            sykmeldingRegisterInputService,
        )
    }
}

fun Application.environmentModule() = module {
    single<Environment> { createEnvironment() }
}

val sykmeldingRegisterInputService = module {
    single {
        val environment = get<Environment>()

    }
}

val databaseModule = module {

    single(named("syfosmregister")) {
        val environment = get<Environment>()
        val props = Properties()
        val dbUser = environment.registerDBUsername
        val dbPassword = environment.registerDBPassword
        val dbName = environment.registerDBName
        val instanceConnectionName = environment.registerDBConnectionName
        logger.info("Connecting to database $dbName, instance $instanceConnectionName, user $dbUser")
        val jdbcUrl = "jdbc:postgresql:///$dbName"
        props.setProperty("user", dbUser)
        props.setProperty("password", dbPassword)
        props.setProperty("socketFactory", "com.google.cloud.sql.postgres.SocketFactory")
        props.setProperty("cloudSqlInstance", instanceConnectionName)
        Database.connect(getNewConnection = { DriverManager.getConnection(jdbcUrl, props) } )
    }

    single { SmregisterDatabase(get(named("syfosmregister"))) }
}

val sykmeldingConsumer = module {

    single {
        val env = get<Environment>()

        KafkaConsumer(get<Environment>().kafkaConfig.apply {
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.GROUP_ID_CONFIG] = "migrator-sykmelding"
            this[ConsumerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-ny-sykmelding-consumer"
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
            this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "10000"

        }, StringDeserializer(), StringDeserializer())
    }
    single {
        SykmeldingConsumer(
            kafkaConsumer = get(),
            kafkaProducer = get(),
            okSykmeldingTopic = get<Environment>().okSykmeldingTopic,
            manuellBehandlingSykmeldingTopic = get<Environment>().manuellSykmeldingTopic,
            avvistSykmeldingTopic = get<Environment>().avvistSykmeldingTopic,
            tsmMigrertTopic = get<Environment>().migrertSykmeldingTopic
        )
    }
}

