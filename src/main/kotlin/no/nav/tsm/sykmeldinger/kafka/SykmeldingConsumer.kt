package no.nav.tsm.sykmeldinger.kafka

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

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
    private val topicCount = sykmeldingTopics.associateWith {
        0
    }.toMutableMap()

    private val topicDate = sykmeldingTopics.associateWith {
        OffsetDateTime.MIN
    }.toMutableMap()

    @WithSpan
    suspend fun start() = coroutineScope(suspendFunction1())

    private fun SykmeldingConsumer.suspendFunction1(): suspend CoroutineScope.() -> Unit =
        {
            logger.info("starting consumer for $sykmeldingTopics")

            launch(Dispatchers.IO) {
                while (isActive) {
                    sykmeldingTopics.forEach {
                        logger.info("Topic: $it, count: ${topicCount[it]}, date: ${topicDate[it]}")
                    }
                    delay(300_000)
                }
            }

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
            val topicMap = records
                .groupBy { it.topic() }

            topicMap.forEach { (topic, records) ->
                if (records.isNotEmpty()) {
                    topicCount[topic] = topicCount.getOrDefault(topic, 0) + records.count()
                    topicDate[topic] = records.maxOf { Instant.ofEpochMilli(it.timestamp()).atOffset(ZoneOffset.UTC) }
                }
            }
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
