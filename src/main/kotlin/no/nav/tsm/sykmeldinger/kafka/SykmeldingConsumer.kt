package no.nav.tsm.sykmeldinger.kafka

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
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

    val sykmeldingTopics = listOf(okSykmeldingTopic, manuellBehandlingSykmeldingTopic, avvistSykmeldingTopic)

    @WithSpan
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun start() {
        logger.info("starting consumer for $sykmeldingTopics")
        kafkaConsumer.subscribe(sykmeldingTopics)
        kafkaProducer.initTransactions()
        val topicCount = sykmeldingTopics.associateWith {
            0
        }.toMutableMap()

        val topicDate = sykmeldingTopics.associateWith {
            OffsetDateTime.MIN
        }.toMutableMap()

        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                sykmeldingTopics.forEach {
                    logger.info("Topic: $it, count: ${topicCount[it]}, date: ${topicDate[it]}")
                }
                delay(300_000)
            }
        }

        while (true) {
            val records = kafkaConsumer.poll(java.time.Duration.ofMillis(10_000))

            val topicMap = records
                .groupBy { it.topic() }

            topicMap.forEach { (topic, records) ->
                if (records.isNotEmpty()) {
                    topicCount[topic] = topicCount.getOrDefault(topic, 0) + records.count()
                    topicDate[topic] = records.maxOf { Instant.ofEpochMilli(it.timestamp()).atOffset(ZoneOffset.UTC) }
                }
            }

            try {
                kafkaProducer.beginTransaction()
                records.forEach {
                    val receivedSykmelding = it.value()
                    val sykmeldingId = it.key()
                    kafkaProducer.send(
                        ProducerRecord(
                            tsmMigrertTopic,
                            sykmeldingId,
                            MigrertSykmelding(sykmeldingId, null, receivedSykmelding, it.topic())
                        )
                    )
                }
                kafkaProducer.commitTransaction()
            } catch (ex: Exception) {
                logger.error("Error processing messages ${records.first().partition()}} ${records.first().offset()}")
                kafkaProducer.abortTransaction()
                kafkaConsumer.unsubscribe()
                throw ex
            }
        }
    }
}
