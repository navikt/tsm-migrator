package no.nav.tsm.plugins

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.syfo.pdl.TsmPdlClient
import no.nav.tsm.digital.DigitalSykmeldingConsumer
import no.nav.tsm.digital.ManuellOppgave
import no.nav.tsm.digital.SykmeldingRecordDeserializer
import no.nav.tsm.reformat.sykmelding.SykmeldingReformatService
import no.nav.tsm.reformat.sykmelding.service.SykmeldingMapper
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory
import no.nav.tsm.sykmeldinger.kafka.SykmeldingConsumer
import no.nav.tsm.sykmeldinger.kafka.util.JacksonKafkaDeserializer
import no.nav.tsm.sykmeldinger.kafka.util.JacksonKafkaSerializer
import no.nav.tsm.texas.TexasClient
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
            tsmPdlModule,
            sykmeldingConsumer,
            sykmeldingReformatService,
            digitalSykmeldingConsumer
        )
    }
}

fun Application.environmentModule() = module {
    single<Environment> { createEnvironment() }
}

val tsmPdlModule = module {
    single {
        val env = get<Environment>()
        val config: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
            install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
        }
        val httpClient = HttpClient(Apache, config)
        val tsmPdlScope = "api://${env.cluster}.tsm.tsm-pdl-cache/.default"
        val texasClient = TexasClient(
            tokenEndpoint = env.texasTokenEndpoint,
            httpClient = httpClient,
            tsmPdlScope = tsmPdlScope,
        )
        TsmPdlClient(
            texasClient = texasClient,
            httpClient = httpClient,
            tsmPdlUrl = env.tsmPdlCacheUrl
        )
    }
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

        val producer = SykmeldingInputKafkaInputFactory.naisProducer()

        val reformatService = SykmeldingReformatService(
            kafkaConsumer = consumer,
            sykmeldingMapper = SykmeldingMapper(),
            kafkaProducer = producer,
            inputTopic = env.teamsykmeldingSykmeldingTopic,
            cluster = env.cluster
        )
        reformatService
    }
}

val digitalSykmeldingConsumer = module {
    single {
        val env = get<Environment>()
        val consumer = KafkaConsumer(Properties().apply {
            putAll(env.kafkaConfig)
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = SykmeldingRecordDeserializer::class.java.name
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.GROUP_ID_CONFIG] = "migrator-digital-sykmelding"
            this[ConsumerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-digital-consumer"
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
            this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1"
        }, StringDeserializer(), SykmeldingRecordDeserializer())

        val producer = KafkaProducer<String, ReceivedSykmelding?>(Properties().apply {
            putAll(env.kafkaConfig)
            this[ProducerConfig.ACKS_CONFIG] = "all"
            this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
            this[ProducerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-digital-sykmelding-producer"
            this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
            this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonKafkaSerializer::class.java
            this[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "gzip"
        })

        val producerManuellTilbakedatring = KafkaProducer<String, ManuellOppgave>(Properties().apply {
            putAll(env.kafkaConfig)
            this[ProducerConfig.ACKS_CONFIG] = "all"
            this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
            this[ProducerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-digital-manuell-sykmelding-producer"
            this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
            this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonKafkaSerializer::class.java
            this[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "gzip"
        })

        DigitalSykmeldingConsumer(
            kafkaConsumer = consumer,
            kafkaProducer = producer,
            kafkaProducerManuellTIlbakedatering = producerManuellTilbakedatring,
            tsmSykmeldingerTopic = env.tsmSykmeldingTopic,
            okSykmeldingTopic = env.okSykmeldingTopic,
            manuellBehanldingTopic = env.manuellTilbakedateringTopic,

            cluster = env.cluster,
            tsmPdlClient = get()
        )
    }
}

val sykmeldingConsumer = module {

    single {
        val env = get<Environment>()

        val consumer = KafkaConsumer(Properties().apply {
            putAll(env.kafkaConfig)
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JacksonKafkaDeserializer::class.java.name
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
            this[ConsumerConfig.GROUP_ID_CONFIG] = "migrator-sykmelding"
            this[ConsumerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-ny-sykmelding-consumer"
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
            this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1"
        }, StringDeserializer(), JacksonKafkaDeserializer(ReceivedSykmelding::class))

        val producer = KafkaProducer<String, ReceivedSykmelding?>(Properties().apply {
            putAll(env.kafkaConfig)
            this[ProducerConfig.ACKS_CONFIG] = "all"
            this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
            this[ProducerConfig.CLIENT_ID_CONFIG] = "${env.hostname}-migrert-sykmelding-producer"
            this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
            this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonKafkaSerializer::class.java
            this[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "gzip"
        })

        SykmeldingConsumer(
            kafkaConsumer = consumer,
            kafkaProducer = producer,
            okSykmeldingTopic = env.okSykmeldingTopic,
            manuellBehandlingSykmeldingTopic = env.manuellSykmeldingTopic,
            avvistSykmeldingTopic = env.avvistSykmeldingTopic,
            teamsykmeldingSykmeldigerTopic = env.teamsykmeldingSykmeldingTopic,
        )
    }
}

