package no.nav.tsm.sykmeldinger.kafka

import no.nav.tsm.sykmeldinger.kafka.model.MigrertSykmelding
import org.apache.kafka.clients.consumer.KafkaConsumer

class MigrertSykmeldingConsumer(private val migrertSykmeldingConsumer: KafkaConsumer<String, MigrertSykmelding>,
                                private val migrertTopic: String,
                                private val sykmeldingInputTopic: String, ) {
    suspend fun start() {
        migrertSykmeldingConsumer.subscribe(listOf(migrertTopic))

        while (true) {
            val records = migrertSykmeldingConsumer.poll(java.time.Duration.ofMillis(10_000))
            if(!records.isEmpty){


            }
            records.forEach {
                println("Received record: ${it.value()}")
            }
        }
    }
}
