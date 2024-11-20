package no.nav.tsm.sykmeldinger.input

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import no.nav.tsm.avro.model.toAvroModel
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.sykmeldinger.kafka.SykmeldingConsumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Duration

class SykmeldingInputAvro(private val kafkaConsumer: KafkaConsumer<String, ReceivedSykmelding?>,
                              private val kafkaProducer: KafkaProducer<String, no.nav.tsm.avro.model.ReceivedSykmelding?>,
                              private val inputTopic: String,
                              private val teamsykmeldingSykmeldingAvroTopic: String) {
    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(SykmeldingConsumer::class.java)
    }
    @WithSpan
    suspend fun start() = coroutineScope(runKafkaConsumer())

    private fun runKafkaConsumer(): suspend CoroutineScope.() -> Unit =
        {
            logger.info("starting consumer for $inputTopic")

            while (isActive) {
                try {
                    consumeMessages()
                } catch (ex: CancellationException) {
                    logger.info("Consumer cancelled")
                } catch (ex: Exception) {
                    logger.error("Error processing messages from kafka delaying 60 seconds to tray again")
                    kafkaConsumer.unsubscribe()
                    delay(60_000)
                }
            }
            kafkaConsumer.unsubscribe()
            logger.info("Consumerer is stopping")
        }

    private suspend fun consumeMessages() = coroutineScope {
        kafkaConsumer.subscribe(listOf(inputTopic))
        while (isActive) {
            val records = kafkaConsumer.poll(Duration.ofMillis(10_000))

            records.forEach {
                if(it.value() == null) {
                    logger.info("tombstone for ${it.key()}")
                }
                val receivedSykmelding: no.nav.tsm.avro.model.ReceivedSykmelding? = it.value()?.let { toAvroModel(it) }
                val sykmeldingId = it.key()
                kafkaProducer.send(
                    ProducerRecord(
                        teamsykmeldingSykmeldingAvroTopic,
                        sykmeldingId,
                        receivedSykmelding)
                ).get()
            }
        }
    }
}
