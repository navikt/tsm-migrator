package no.nav.tsm.sykmelding

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import no.nav.tsm.smregister.models.ReceivedSykmelding
import no.nav.tsm.sykmelding.model.SykmeldingMedBehandlingsutfall
import no.nav.tsm.sykmelding.service.MappingException
import no.nav.tsm.sykmelding.service.SykmeldingMapper
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class SykmeldingReformatService(
    private val kafkaConsumer: KafkaConsumer<String, ReceivedSykmelding>,
    private val sykmeldingMapper: SykmeldingMapper,
    private val kafkaProducer: KafkaProducer<String, SykmeldingMedBehandlingsutfall>,
    private val outputTopic: String,
    private val inputTopic: String,
) {
    companion object {
        private val log = LoggerFactory.getLogger(SykmeldingReformatService::class.java)
    }
    var running : Boolean = false
    suspend fun start() = coroutineScope {
        kafkaConsumer.subscribe(listOf(inputTopic))
        while (isActive) {
            val records = kafkaConsumer.poll(10.seconds.toJavaDuration())
            records.forEach { record ->
                try {
                    val sykmeldingMedBehandlingsutfall = record.value()?.let { sykmeldingMapper.toNewSykmelding(it) }
                    kafkaProducer.send(ProducerRecord(outputTopic, record.key(), sykmeldingMedBehandlingsutfall)).get()
                } catch (mappingException: MappingException) {
                    log.error(mappingException.message, mappingException)
                    throw mappingException
                } catch (ex: Exception) {
                    log.error(ex.message, ex)
                    throw ex
                } finally {
                    running = false
                    kafkaConsumer.unsubscribe()
                }
             }
        }
        kafkaConsumer.unsubscribe()
    }
}

