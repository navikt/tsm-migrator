import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.tsm.mottak.sykmelding.kafka.util.DumpDeserializer
import no.nav.tsm.plugins.Environment
import no.nav.tsm.plugins.createEnvironment
import no.nav.tsm.sykmeldinger.database.DumpService
import no.nav.tsm.sykmeldinger.database.MigrertSykmeldingService
import no.nav.tsm.sykmeldinger.kafka.DumpConsumer
import no.nav.tsm.sykmeldinger.kafka.HistoriskSykmeldingConsumer
import no.nav.tsm.sykmeldinger.kafka.MigrertSykmeldingProducer
import no.nav.tsm.sykmeldinger.kafka.aiven.KafkaEnvironment.Companion.getEnvVar
import no.nav.tsm.sykmeldinger.kafka.aiven.KafkaUtils.Companion.getAivenKafkaConfig
import no.nav.tsm.sykmeldinger.kafka.model.HistoriskSykmeldingInput
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import no.nav.tsm.sykmeldinger.kafka.toProducerConfig
import no.nav.tsm.sykmeldinger.kafka.util.FellesformatDeserializer
import no.nav.tsm.sykmeldinger.kafka.util.JacksonKafkaSerializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()

        modules(
            environmentModule(), kafkaModule, databaseModule, migrerteSykmeldingerTask
        )
    }
}

fun Application.environmentModule() = module {
    single<Environment> { createEnvironment() }
}

val databaseModule = module {
//    singleOf(::FellesformatService)
//    singleOf(::GamleSykmeldingerService)
    // disabling because it appears to be completed
}

val migrerteSykmeldingerTask = module {
    single {
        KafkaProducer<String, MigrertSykmelding>(getAivenKafkaConfig("migrator-migrert-sykmelding-producer").toProducerConfig(
            "migrator-migrert-sykmelding-producer",
            JacksonKafkaSerializer::class,
        ).apply { this[ProducerConfig.TRANSACTIONAL_ID_CONFIG] = "migrator-${getEnvVar("HOSTNAME")}" })
    }
    single { MigrertSykmeldingService(get()) }
}

val kafkaModule = module {

    single {
        val env = get<Environment>()

        KafkaConsumer(get<Environment>().kafkaConfig.apply {
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.GROUP_ID_CONFIG] = "migrator-sykmelding-historisk-consumer"
            this[ConsumerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-sykmelding-historisk-consumer"
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
            this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "10000"

        }, StringDeserializer(), StringDeserializer())
    }
    single {
        HistoriskSykmeldingConsumer(
            get(),
            get<Environment>().sykmeldingHistoriskTopic,
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

    single {
        val env = get<Environment>()

        KafkaProducer<String, String>(env.kafkaConfig.apply {
            this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
            this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
            this[ProducerConfig.CLIENT_ID_CONFIG] = "migrert-sykmelding-producer"
        })
    }
    single {
        MigrertSykmeldingProducer(get())
    }
}

