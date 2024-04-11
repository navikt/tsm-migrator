import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.tsm.plugins.Environment
import no.nav.tsm.plugins.createEnvironment
import no.nav.tsm.sykmeldinger.database.FellesformatService
import no.nav.tsm.sykmeldinger.database.GamleSykmeldingerService
import no.nav.tsm.sykmeldinger.kafka.FellesformatConsumer
import no.nav.tsm.sykmeldinger.kafka.GamleSykmeldingerConsumer
import no.nav.tsm.sykmeldinger.kafka.MigrertSykmeldingProducer
import no.nav.tsm.sykmeldinger.kafka.model.FellesformatInput
import no.nav.tsm.sykmeldinger.kafka.util.FellesformatDeserializer
import no.nav.tsm.sykmeldinger.kafka.util.GamleSykmeldingerDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()

        modules(
            environmentModule(),
            fellesformatKafkaModule,
            databaseModule
        )
    }
}

fun Application.environmentModule() = module {
    single<Environment> { createEnvironment() }
}

val databaseModule = module {
    singleOf(::FellesformatService)
    //singleOf(::GamleSykmeldingerService)
    // disabling because it appears to be completed
}

val fellesformatKafkaModule = module {

    single {
        val env = get<Environment>()

        KafkaConsumer(get<Environment>().kafkaConfig.apply {
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = FellesformatDeserializer::class.java.name
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.GROUP_ID_CONFIG] = "migrator-4"
            this[ConsumerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-fellesformat-consumer2"
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"

        }, StringDeserializer(), FellesformatDeserializer(FellesformatInput::class))
    }
    single {
        FellesformatConsumer(
            get(),
            listOf(
                get<Environment>().okSykmeldingTopic,
                get<Environment>().avvistSykmeldingTopic,
                get<Environment>().manuellSykmeldingTopic
            )
        )
    }

    single {
        val env = get<Environment>()

        KafkaProducer<String, String>(env.kafkaConfig.apply {
            this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ProducerConfig.CLIENT_ID_CONFIG] = "migrert-sykmelding-producer"
        })
    }
    single{
        MigrertSykmeldingProducer(get(), get())
    }
}

