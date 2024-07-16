import io.ktor.http.contentRangeHeaderValue
import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.tsm.plugins.Environment
import no.nav.tsm.plugins.createEnvironment
import no.nav.tsm.smregister.SmregisterDatabase
import no.nav.tsm.sykmeldinger.database.MigrertSykmeldingService
import no.nav.tsm.sykmeldinger.kafka.HistoriskSykmeldingConsumer
import no.nav.tsm.sykmeldinger.kafka.SykmeldingConsumer
import no.nav.tsm.sykmeldinger.kafka.MigrertSykmeldingProducer
import no.nav.tsm.sykmeldinger.kafka.aiven.KafkaEnvironment.Companion.getEnvVar
import no.nav.tsm.sykmeldinger.kafka.aiven.KafkaUtils.Companion.getAivenKafkaConfig
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import no.nav.tsm.sykmeldinger.kafka.toProducerConfig
import no.nav.tsm.sykmeldinger.kafka.util.JacksonKafkaSerializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
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
            historiskSykmeldingConsumer,
            sykmeldingConsumer,
            migrertSykmeldingProducer,
            migrerteSykmeldingerTask
        )
    }
}

fun Application.environmentModule() = module {
    single<Environment> { createEnvironment() }
}

val databaseModule = module {

    single(named("migrator")) {
        val environment = get<Environment>()
        Database.connect(
            url = environment.migratorJdbcUrl,
            user = environment.migratorDbUser,
            password = environment.migratorDbPassword,
            driver = "org.postgresql.Driver",
        )
    }

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

val migrertSykmeldingProducer = module {
    single {
        KafkaProducer<String, MigrertSykmelding>(getAivenKafkaConfig("migrator-migrert-sykmelding-producer").toProducerConfig(
            "migrator-migrert-sykmelding-producer",
            JacksonKafkaSerializer::class,
        ).apply {
            this[ProducerConfig.TRANSACTIONAL_ID_CONFIG] = "migrator-${getEnvVar("HOSTNAME")}" }
        )
    }
}

val migrerteSykmeldingerTask = module {
    single { MigrertSykmeldingService(get()) }
}

val historiskSykmeldingConsumer = module {

    single {
        val env = get<Environment>()

        KafkaConsumer(get<Environment>().kafkaConfig.apply {
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.GROUP_ID_CONFIG] = "migrator-historisk-sykmelding"
            this[ConsumerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-historisk-sykmelding-consumer"
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
            this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "10000"

        }, StringDeserializer(), StringDeserializer())
    }
    single {
        HistoriskSykmeldingConsumer(
            kafkaConsumer = get(),
            kafkaProducer = get(),
            tsmMigrertTopic = get<Environment>().migrertSykmeldingTopic,
            historiskSykmeldingTopic = get<Environment>().sykmeldingHistoriskTopic
        )
    }
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
//    single {
//        val env = get<Environment>()
//
//        KafkaConsumer(get<Environment>().kafkaConfig.apply {
//            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = GamleSykmeldingerDeserializer::class.java.name
//            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
//            this[ConsumerConfig.GROUP_ID_CONFIG] = "migrator-9"
//            this[ConsumerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-gamleSykmeldinger-consumer7"
//            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
//            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
//            this[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = "300000" //5 minutes
//
//        }, StringDeserializer(), GamleSykmeldingerDeserializer(GamleSykmeldingerInput::class))
//    }
//    single {
//        GamleSykmeldingerConsumer(
//            get(),
//            listOf(
//                get<Environment>().gamleSykmeldingTopic,
//            )
//        )
//    }

/*    single {
        val env = get<Environment>()

        KafkaProducer<String, String>(env.kafkaConfig.apply {
            this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
            this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
            this[ProducerConfig.CLIENT_ID_CONFIG] = "migrert-sykmelding-producer"
        })
    }
    single {
        MigrertSykmeldingProducer(get())
    }*/
}

