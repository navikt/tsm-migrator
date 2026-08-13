package no.nav.tsm.sykmeldinger.kafka

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import no.nav.tsm.ktor.logger
import no.nav.tsm.smregister.models.ReceivedSykmelding
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Duration

class SykmeldingConsumer(
    private val kafkaConsumer: KafkaConsumer<String, ReceivedSykmelding?>,
    private val kafkaProducer: KafkaProducer<String, ReceivedSykmelding?>,
    private val teamsykmeldingSykmeldigerTopic: String,
    okSykmeldingTopic: String,
    manuellBehandlingSykmeldingTopic: String,
    avvistSykmeldingTopic: String
) {
    private val logger = logger()
    private val sykmeldingTopics = listOf(okSykmeldingTopic, manuellBehandlingSykmeldingTopic, avvistSykmeldingTopic)

    @WithSpan
    suspend fun start() = coroutineScope(runKafkaConsumer())

    private fun runKafkaConsumer(): suspend CoroutineScope.() -> Unit =
        {
            logger.info("Not starting consumer for $sykmeldingTopics, its being deprecated.")
        }

    private suspend fun consumeMessages() = coroutineScope {
        kafkaConsumer.subscribe(sykmeldingTopics)
        while (isActive) {
            val records = kafkaConsumer.poll(Duration.ofMillis(10_000))

            records.forEach {
                val receivedSykmelding = it.value()
                val sykmeldingId = it.key()
                if(receivedSykmelding == null) {
                    logger.info("tombstoning sykmelding with id: $sykmeldingId")
                }
                val headers = it.headers()
                val producerRecord = ProducerRecord(
                    teamsykmeldingSykmeldigerTopic,
                    null,
                    sykmeldingId,
                    receivedSykmelding,
                    headers
                )

                kafkaProducer.send(
                    producerRecord
                ).get()
            }
        }
    }
}
