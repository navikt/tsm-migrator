package no.nav.tsm.sykmeldinger.kafka

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.SykmeldingRegisterService
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

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


        launch(Dispatchers.IO) {
            while (true) {
                sourceMap.forEach { (source, count) ->
                    logger.info("Source: $source, count: $count")
                }
                delay(60_000)
            }
        }
        while (isActive) {
            try {
                consumeMessages()
            } catch (e: CancellationException) {
                logger.info("Consumer cancelled")
            } catch (ex: Exception) {
                logger.error("Error processing messages from kafka delaying 60 seconds to tray again")
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
                records.forEach { record ->
                    sykmeldingRegisterService.handleMigrertSykmelding(record.value())
                    sourceMap[record.value().source] = sourceMap.getOrDefault(record.value().source, 0) + 1
                }
                migrertSykmeldingConsumer.commitSync()
            }
        }
    }
}
