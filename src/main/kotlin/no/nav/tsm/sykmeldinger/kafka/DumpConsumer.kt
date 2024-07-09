package no.nav.tsm.sykmeldinger.kafka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nav.tsm.sykmeldinger.database.DumpService
import no.nav.tsm.sykmeldinger.kafka.model.HistoriskSykmeldingInput
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class DumpConsumer(
    private val kafkaConsumer: KafkaConsumer<String, HistoriskSykmeldingInput>,
    private val regdumpTopic: String,
    private val dumpService: DumpService,
    ) {

    // this should be changed to the import org.koin.ktor.ext.inject import for a kotlin idiomatic approach

    companion object {
        private val logger = LoggerFactory.getLogger(DumpConsumer::class.java)
    }
    var counter = 0
    var lastDate = LocalDate.MIN
    suspend fun consumeDump() = withContext(Dispatchers.IO) {
        subscribeToKafkaTopics()
        val loggingJob = launch(Dispatchers.IO) {
            while(true) {
                logger.info("lest $counter meldinger, siste dato $lastDate")
                delay(10_000)
            }
        }
        try {
            while (isActive) {
                processMessages()
            }
        } finally {
            loggingJob.cancel()
            logger.info("unsubscribing and closing kafka consumer")
            kafkaConsumer.unsubscribe()
            kafkaConsumer.close()
        }
    }

    private suspend fun processMessages() {
        try {
            val records = kafkaConsumer.poll(Duration.ofMillis(10_000)).mapNotNull { it.value() }
            processRecord(records)
        } catch (ex: Exception) {
            println("Error processing messages: ${ex.message}")
            kafkaConsumer.unsubscribe()
            delay(30.seconds)
            subscribeToKafkaTopics()
        }
    }


    private suspend fun processRecord(records: List<HistoriskSykmeldingInput>) {
        //logger.info("Received message from topic: ${record.topic()}")
        withContext(Dispatchers.IO) {
            if(records.isNotEmpty()) {
                val toInsert = records.filter { it.mottattDato.isBefore(LocalDate.of(2022, 2, 1).atStartOfDay()) }
                if(toInsert.isNotEmpty()) {
                    dumpService.batchInsert(toInsert)
                    lastDate = toInsert.last().mottattDato.toLocalDate()
                }
            }
        }
        //logger.info("Inserted $counter records into the database when consuming from topic: ${record.topic()}")
    }
    private fun subscribeToKafkaTopics() {
        kafkaConsumer.subscribe(listOf(regdumpTopic))
    }
}
