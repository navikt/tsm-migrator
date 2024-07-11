package no.nav.tsm.sykmeldinger.kafka

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.database.DumpService.historiske_sykmeldinger
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset

class HistoriskSykmeldingConsumer(private val kafkaConsumer: KafkaConsumer<String, String>, private val topic: String) {

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(HistoriskSykmeldingConsumer::class.java)
    }

    val sykmeldingTopics = listOf("privat-syfo-sm2013-automatiskBehandling", "privat-syfo-sm2013-manuellBehandling", "privat-syfo-sm2013-avvistBehandling")
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun start() {
        logger.info("starting consumer for $topic")
        kafkaConsumer.subscribe(listOf(topic))
        var counter = 0
        var deleted = 0
        var lastDate = java.time.LocalDate.MIN
        GlobalScope.launch(Dispatchers.IO) {
            while(true) {
                logger.info("lest $counter, deleted: $deleted")
                delay(10_000)
            }
        }

        while (true) {
            val records = kafkaConsumer.poll(java.time.Duration.ofMillis(10_000))
            counter += records.count()
            val sykmeldinger = records.filter { record ->
                val header = record.headers().headers("topic").firstOrNull()?.value()?.let { String(it, StandardCharsets.UTF_8) } ?: "no-topic"
                header in sykmeldingTopics
            }.map { it.key() }

            if(sykmeldinger.isNotEmpty()) {
                deleted +=
                    transaction {
                        historiske_sykmeldinger.deleteWhere {
                            sykmeldingId inList sykmeldinger
                        }
                }
            }
        }
    }
}
