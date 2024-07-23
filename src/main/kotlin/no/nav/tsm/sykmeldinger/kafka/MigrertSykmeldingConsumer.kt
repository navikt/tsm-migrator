package no.nav.tsm.sykmeldinger.kafka

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.SykmeldingRegisterService
import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory

class MigrertSykmeldingConsumer(
    private val migrertSykmeldingConsumer: KafkaConsumer<String, MigrertSykmelding>,
    private val sykmeldingRegisterService: SykmeldingRegisterService,
    private val migrertTopic: String,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MigrertSykmeldingConsumer::class.java)
    }
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun start() {
        migrertSykmeldingConsumer.subscribe(listOf(migrertTopic))
        val sourceMap = mutableMapOf<String, Int>()
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                sourceMap.forEach { (source, count) ->
                    logger.info("Source: $source, count: $count")
                }
                delay(60_000)
            }
        }
        while (true) {
            val records = migrertSykmeldingConsumer.poll(java.time.Duration.ofMillis(10_000))
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
