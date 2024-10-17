package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.sykmelding.SykmeldingReformatService
import no.nav.tsm.sykmelding.model.SykmeldingMedBehandlingsutfall
import no.nav.tsm.sykmelding.service.SykmeldingMapper
import no.nav.tsm.sykmeldinger.SykmeldingRegisterService
import no.nav.tsm.sykmeldinger.kafka.MigrertSykmeldingConsumer
import no.nav.tsm.sykmeldinger.kafka.SykmeldingConsumer
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import no.nav.tsm.sykmeldinger.kafka.util.JacksonKafkaDeserializer
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
import java.util.Properties

fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()

        modules(
            environmentModule(),
            sykmeldingConsumer,
            migrertSykmeldingConsumer,
            sykmeldingReformatService
        )
    }
}

fun Application.environmentModule() = module {
    single<Environment> { createEnvironment() }
}

val sykmeldingReformatService = module {
    single {
        val env = get<Environment>()
        val consumer = KafkaConsumer(Properties().apply {
            putAll(env.kafkaConfig)
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JacksonKafkaDeserializer::class.java.name
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.GROUP_ID_CONFIG] = "sykmelding-reformat-consumer"
            this[ConsumerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-sykmelding-reformat-consumer"
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
            this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1"
        }, StringDeserializer(), JacksonKafkaDeserializer(ReceivedSykmelding::class))

        val producer = KafkaProducer<String, SykmeldingMedBehandlingsutfall>(Properties().apply {
            putAll(env.kafkaConfig)
            this[ProducerConfig.ACKS_CONFIG] = "all"
            this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
            this[ProducerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-sykmelding-reformat-producer"
            this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
            this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonKafkaSerializer::class.java
            this[ProducerConfig.TRANSACTIONAL_ID_CONFIG] = "${env.hostname}-sykmelding-reformat-producer"
        })

        val reformatService = SykmeldingReformatService(
            kafkaConsumer = consumer,
            sykmeldingMapper = SykmeldingMapper(),
            kafkaProducer = producer,
            outputTopic = env.mottattSykmeldingTopic,
            inputTopic = env.sykmeldingerInputTopic
        )
        reformatService
    }
}

val migrertSykmeldingConsumer = module {
    single {
        val env = get<Environment>()

        val consumer: KafkaConsumer<String, MigrertSykmelding> = KafkaConsumer(Properties().apply {
            putAll(env.kafkaConfig)
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JacksonKafkaDeserializer::class.java.name
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.GROUP_ID_CONFIG] = "migrator-sykmelding"
            this[ConsumerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-migrert-sykmelding-consumer"
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
            this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "100"

        }, StringDeserializer(), JacksonKafkaDeserializer(MigrertSykmelding::class))

        val producer = KafkaProducer<String, ReceivedSykmelding?>(Properties().apply {
            putAll(env.kafkaConfig)
            this[ProducerConfig.ACKS_CONFIG] = "all"
            this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
            this[ProducerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-receivedsykmelding-producer"
            this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
            this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonKafkaSerializer::class.java
        })

        val sykmeldingRegisterService =
            SykmeldingRegisterService(producer, env.sykmeldingerInputTopic)

        MigrertSykmeldingConsumer(
            migrertSykmeldingConsumer = consumer,
            sykmeldingRegisterService = sykmeldingRegisterService,
            migrertTopic = env.migrertSykmeldingTopic
        )
    }
}

val sykmeldingConsumer = module {

    single {
        val env = get<Environment>()
        KafkaConsumer(Properties().apply {
            putAll(env.kafkaConfig)
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.GROUP_ID_CONFIG] = "migrator-sykmelding"
            this[ConsumerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-ny-sykmelding-consumer"
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
            this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "100"
        }, StringDeserializer(), StringDeserializer())
    }


    single {
        val env = get<Environment>()
        val producer = KafkaProducer<String, MigrertSykmelding>(Properties().apply {
            putAll(env.kafkaConfig)
            this[ProducerConfig.ACKS_CONFIG] = "all"
            this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
            this[ProducerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-migrert-sykmelding-producer"
            this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
            this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonKafkaSerializer::class.java
        })

        SykmeldingConsumer(
            kafkaConsumer = get(),
            kafkaProducer = producer,
            okSykmeldingTopic = get<Environment>().okSykmeldingTopic,
            manuellBehandlingSykmeldingTopic = get<Environment>().manuellSykmeldingTopic,
            avvistSykmeldingTopic = get<Environment>().avvistSykmeldingTopic,
            tsmMigrertTopic = get<Environment>().migrertSykmeldingTopic
        )
    }
}

