package no.nav.tsm.sykmeldinger.kafka

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.database.historiske_sykmeldinger
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class HistoriskSykmeldingConsumer(private val kafkaConsumer: KafkaConsumer<String, String>,
                                  private val okSykmeldingTopic: String,
                                  private val manuellBehandlingSykmeldingTopic: String,
                                  private val avvistSykmeldingTopic: String) {

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(HistoriskSykmeldingConsumer::class.java)
    }

    val sykmeldingTopics = listOf(okSykmeldingTopic, manuellBehandlingSykmeldingTopic, avvistSykmeldingTopic)

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun start() {
        logger.info("starting consumer for $sykmeldingTopics")
        kafkaConsumer.subscribe(sykmeldingTopics)
        val topicCount = sykmeldingTopics.associateWith {
            0
        }.toMutableMap()
        val topicDeleted = sykmeldingTopics.associateWith {
            0
        }.toMutableMap()

        val topicDate = sykmeldingTopics.associateWith {
            OffsetDateTime.MIN
        }.toMutableMap()

        GlobalScope.launch(Dispatchers.IO) {
            while(true) {
                sykmeldingTopics.forEach {
                    logger.info("Topic: $it, count: ${topicCount[it]}, deleted: ${topicDeleted[it]}, date: ${topicDate[it]}")
                }
                delay(10_000)
            }
        }

        while (true) {
            val records = kafkaConsumer.poll(java.time.Duration.ofMillis(10_000))

            val topicMap = records
                .groupBy { it.topic() }

            topicMap.forEach { (topic, records) ->
                if(records.isNotEmpty()) {
                    topicCount[topic] = topicCount.getOrDefault(topic, 0) + records.count()
                    topicDate[topic] = records.maxOf { Instant.ofEpochMilli(it.timestamp()).atOffset(ZoneOffset.UTC) }

                    val deleted = transaction {
                        historiske_sykmeldinger.deleteWhere {
                            sykmeldingId inList records.map { it.key() }
                        }
                    }
                    topicDeleted[topic] = topicDeleted.getOrDefault(topic, 0) + deleted
                }
            }
        }
    }
}
