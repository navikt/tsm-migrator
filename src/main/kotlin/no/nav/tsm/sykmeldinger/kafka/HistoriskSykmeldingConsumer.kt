package no.nav.tsm.sykmeldinger.kafka

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.kafka.SykmeldingConsumer.Companion
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime

class HistoriskSykmeldingConsumer(
    private val kafkaConsumer: KafkaConsumer<String, String?>,
    private val kafkaProducer: KafkaProducer<String, MigrertSykmelding>,
    private val tsmMigrertTopic: String,
    private val historiskSykmeldingTopic: String
) {

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(HistoriskSykmeldingConsumer::class.java)
    }

    val sykmeldingTopics = listOf(
        "privat-syfo-sm2013-automatiskBehandling",
        "privat-syfo-sm2013-manuellBehandling",
        "privat-syfo-sm2013-avvistBehandling"
    )

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun start() {
        logger.info("starting consumer for $historiskSykmeldingTopic")
        kafkaConsumer.subscribe(listOf(historiskSykmeldingTopic))
        kafkaProducer.initTransactions()
        var counter = 0
        val topicCount = mutableMapOf<String, Int>()

        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                logger.info("Total count: $counter")
                sykmeldingTopics.forEach {
                    logger.info("Topic: $it, count: ${topicCount[it]}")
                }
                delay(10_000)
            }
        }

        while (true) {
            val records = kafkaConsumer.poll(java.time.Duration.ofMillis(10_000))
            counter += records.count()

            val sykmeldinger = records.filter { record ->
                topicCount[record.topic()] = topicCount.getOrDefault(record.topic(), 0) + 1
                val header =
                    record.headers().headers("topic").firstOrNull()?.value()?.let { String(it, StandardCharsets.UTF_8) }
                        ?: "no-topic"
                header in sykmeldingTopics
            }

            try {
                kafkaProducer.beginTransaction()
                sykmeldinger.forEach {
                    val receivedSykmelding = it.value()
                    val sykmeldingId = it.key()
                    kafkaProducer.send(
                        ProducerRecord(
                            tsmMigrertTopic,
                            sykmeldingId,
                            MigrertSykmelding(sykmeldingId, null, receivedSykmelding)
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
