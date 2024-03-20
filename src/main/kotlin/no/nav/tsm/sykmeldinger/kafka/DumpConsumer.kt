package no.nav.tsm.sykmeldinger.kafka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.sykmeldinger.database.DumpService
import no.nav.tsm.sykmeldinger.kafka.model.SykmeldingInput
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class DumpConsumer(
    private val kafkaConsumer: KafkaConsumer<String, SykmeldingInput>,
    private val regdumpTopic: String,
    ) {

    val dumpService by inject<DumpService>(DumpService::class.java)
    // this should be changed to the import org.koin.ktor.ext.inject import for a kotlin idiomatic approach

    companion object {
        private val logger = LoggerFactory.getLogger(DumpConsumer::class.java)
    }

    suspend fun consumeDump() = withContext(Dispatchers.IO) {
        subscribeToKafkaTopics()
        try {
            while (isActive) {
                logger.info("Polling for messages from topic: $regdumpTopic")
                processMessages()
            }
        } finally {
            logger.info("unsubscribing and closing kafka consumer")
            kafkaConsumer.unsubscribe()
            kafkaConsumer.close()
        }
    }

    private suspend fun processMessages() {
        try {
            val records = kafkaConsumer.poll(1.seconds.toJavaDuration())
            logger.info("Received ${records.count()} messages from topic: $regdumpTopic")
            records.forEach { record ->
                processRecord(record)
            }
        } catch (ex: Exception) {
            println("Error processing messages: ${ex.message}")
            kafkaConsumer.unsubscribe()
            delay(1.seconds)
            subscribeToKafkaTopics()
        }
    }


    private suspend fun processRecord(record: ConsumerRecord<String, SykmeldingInput>) {
        //logger.info("Received message from topic: ${record.topic()}")
        var counter = 0
        withContext(Dispatchers.IO) {
            dumpService.insertDump(record.value())
            counter++
        }
        //logger.info("Inserted $counter records into the database when consuming from topic: ${record.topic()}")
    }
    private fun subscribeToKafkaTopics() {
        kafkaConsumer.subscribe(listOf(regdumpTopic))
    }
}