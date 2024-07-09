package no.nav.tsm.sykmeldinger.kafka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.sykmeldinger.database.FellesformatService
import no.nav.tsm.sykmeldinger.kafka.model.FellesformatInput
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.koin.java.KoinJavaComponent
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

class FellesformatConsumer(
    private val kafkaConsumer: KafkaConsumer<String, FellesformatInput>,
    private val topics: List<String>,
) {

        val fellesformatService by KoinJavaComponent.inject<FellesformatService>(FellesformatService::class.java)
        // this should be changed to the import org.koin.ktor.ext.inject import for a kotlin idiomatic approach

        companion object {
            private val logger = LoggerFactory.getLogger(FellesformatConsumer::class.java)
        }

        suspend fun consumeDump() = withContext(Dispatchers.IO) {
            subscribeToKafkaTopics()
            try {
                while (isActive) {
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
            val records = kafkaConsumer.poll(Duration.ofMillis(10_000)).mapNotNull {
                try {
                    it.value()
                } catch (ex: Exception) {
                    logger.error("Error deserializing record: ${ex.message}")
                    null
                }
            }
            processRecord(records)
        } catch (ex: Exception) {
            logger.error("Error processing messages: ${ex.message}")
            kafkaConsumer.unsubscribe()
            delay(1.seconds)
            subscribeToKafkaTopics()
        }
    }


        private suspend fun processRecord(records: List<FellesformatInput>) {
            //logger.info("Received message from topic: ${record.topic()}")
            var counter = 0
            withContext(Dispatchers.IO) {
                fellesformatService.batchUpsert(records)
                counter++
            }
            logger.info("Inserted $counter records into the database when consuming from a sykmelding topic")
        }
        private fun subscribeToKafkaTopics() {
            kafkaConsumer.subscribe(topics)
        }
}
