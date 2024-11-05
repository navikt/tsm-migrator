package no.nav.tsm.sykmeldinger.kafka

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Duration

class SykmeldingConsumer(
    private val kafkaConsumer: KafkaConsumer<String, String>,
    private val kafkaProducer: KafkaProducer<String, MigrertSykmelding>,
    private val tsmMigrertTopic: String,
    okSykmeldingTopic: String,
    manuellBehandlingSykmeldingTopic: String,
    avvistSykmeldingTopic: String
) {

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(SykmeldingConsumer::class.java)
    }

    private val sykmeldingTopics = listOf(okSykmeldingTopic, manuellBehandlingSykmeldingTopic, avvistSykmeldingTopic)

    @WithSpan
    suspend fun start() = coroutineScope(runKafkaConsumer())

    private fun runKafkaConsumer(): suspend CoroutineScope.() -> Unit =
        {
            logger.info("starting consumer for $sykmeldingTopics")

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
        kafkaConsumer.subscribe(sykmeldingTopics)
        while (isActive) {
            val records = kafkaConsumer.poll(Duration.ofMillis(10_000))

            records.forEach {
                val receivedSykmelding = it.value()
                val sykmeldingId = it.key()
                kafkaProducer.send(
                    ProducerRecord(
                        tsmMigrertTopic,
                        sykmeldingId,
                        MigrertSykmelding(sykmeldingId, null, receivedSykmelding, it.topic())
                    )
                ).get()
            }
        }
    }
}
