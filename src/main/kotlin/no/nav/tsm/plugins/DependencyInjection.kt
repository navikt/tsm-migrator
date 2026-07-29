package no.nav.tsm.plugins

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import no.nav.tsm.pdl.TsmPdlClient
import no.nav.tsm.digital.DigitalSykmeldingConsumer
import no.nav.tsm.digital.ManuellOppgave
import no.nav.tsm.digital.SykmeldingRecordDeserializer
import no.nav.tsm.ktor.auth.texas.TexasClient
import no.nav.tsm.reformat.sykmelding.SykmeldingReformatService
import no.nav.tsm.reformat.sykmelding.service.SykmeldingMapper
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory
import no.nav.tsm.sykmeldinger.kafka.SykmeldingConsumer
import no.nav.tsm.sykmeldinger.kafka.util.JacksonKafkaDeserializer
import no.nav.tsm.sykmeldinger.kafka.util.JacksonKafkaSerializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

fun Application.configureDependencyInjection() {
    val env = createEnvironment()
    dependencies {
        provide<HttpClient> { configureBaseHttpClient() }
        provide<Environment> { env }
        provide(TexasClient::class)
        provide(TsmPdlClient::class)
        provide<SykmeldingConsumer> { initSykmeldingConsumer(env) }
        provide<SykmeldingReformatService> { initSykmeldingReformatService(env) }
        provide<DigitalSykmeldingConsumer> { initDigitalSykmeldingConsumer(env, resolve()) }
    }
}

private fun configureBaseHttpClient(): HttpClient = HttpClient(Apache5) {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
}

fun initSykmeldingReformatService(env: Environment): SykmeldingReformatService {
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

    return SykmeldingReformatService(
        kafkaConsumer = consumer,
        sykmeldingMapper = SykmeldingMapper(),
        kafkaProducer = producer,
        inputTopic = env.teamsykmeldingSykmeldingTopic,
        cluster = env.cluster
    )
}

fun initDigitalSykmeldingConsumer(env: Environment, pdl: TsmPdlClient): DigitalSykmeldingConsumer {
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

    return DigitalSykmeldingConsumer(
        kafkaConsumer = consumer,
        kafkaProducer = producer,
        kafkaProducerManuellTIlbakedatering = producerManuellTilbakedatring,
        tsmSykmeldingerTopic = env.tsmSykmeldingTopic,
        okSykmeldingTopic = env.okSykmeldingTopic,
        manuellBehanldingTopic = env.manuellTilbakedateringTopic,

        cluster = env.cluster,
        tsmPdlClient = pdl,
    )
}

fun initSykmeldingConsumer(env: Environment): SykmeldingConsumer {
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

    return SykmeldingConsumer(
        kafkaConsumer = consumer,
        kafkaProducer = producer,
        okSykmeldingTopic = env.okSykmeldingTopic,
        manuellBehandlingSykmeldingTopic = env.manuellSykmeldingTopic,
        avvistSykmeldingTopic = env.avvistSykmeldingTopic,
        teamsykmeldingSykmeldigerTopic = env.teamsykmeldingSykmeldingTopic,
    )
}

