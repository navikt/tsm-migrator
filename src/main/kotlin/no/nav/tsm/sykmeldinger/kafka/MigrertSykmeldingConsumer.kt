package no.nav.tsm.sykmeldinger.kafka

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.sykmeldinger.SykmeldingRegisterService
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

data class MigrertReceivedSykmelding(
    val sykmeldingId: String,
    val receivedSykmelding: ReceivedSykmelding?,
    val source: String = "NO_SOURCE",
)

class MigrertSykmeldingConsumer(
    private val migrertSykmeldingConsumer: KafkaConsumer<String, MigrertSykmelding>,
    private val sykmeldingRegisterService: SykmeldingRegisterService,
    private val migrertTopic: String,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MigrertSykmeldingConsumer::class.java)
    }
    private val sourceMap = mutableMapOf<String, Int>()

    suspend fun start() = coroutineScope {
        while (isActive) {
            try {
                consumeMessages()
            } catch (e: CancellationException) {
                logger.info("Consumer cancelled")
            } catch (ex: Exception) {
                logger.error("Error processing messages from kafka delaying 60 seconds to tray again", ex)
                migrertSykmeldingConsumer.unsubscribe()
                delay(60_000)
            }
        }
        migrertSykmeldingConsumer.unsubscribe()
    }

    private suspend fun consumeMessages()  = coroutineScope {
        migrertSykmeldingConsumer.subscribe(listOf(migrertTopic))
        while (isActive) {
            val records = migrertSykmeldingConsumer.poll(Duration.ofMillis(10_000))

            if (!records.isEmpty) {
                sykmeldingRegisterService.handleMigrertSykmeldinger(records.map { it.value() })
                records.forEach { record ->
                    sourceMap[record.value().source] = sourceMap.getOrDefault(record.value().source, 0) + 1
                }
            }
        }
    }
}
