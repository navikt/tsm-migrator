package no.nav.tsm.migrator

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.migrator.digital.DigitalSykmeldingService
import no.nav.tsm.migrator.digital.ManuellOppgave
import no.nav.tsm.ktor.kafka.consumer.KafkaConsumer
import no.nav.tsm.ktor.kafka.producer.KafkaRecordProducer
import no.nav.tsm.ktor.kafka.producer.createProducer
import no.nav.tsm.ktor.kafka.sykmeldinger.SykmeldingInputProducer
import no.nav.tsm.ktor.kafka.sykmeldinger.SykmeldingerConsumer
import no.nav.tsm.ktor.kafka.sykmeldinger.sykmeldingInputProducer
import no.nav.tsm.plugins.Environment
import no.nav.tsm.plugins.KafkaTopics
import no.nav.tsm.migrator.reformat.SykmeldingReformatService
import no.nav.tsm.migrator.legacy.ReceivedSykmelding

fun Application.configureMigrator() {
    val env: Environment by dependencies
    val sykmeldingReformatService: SykmeldingReformatService by dependencies
    val digitalSykmeldingConsumer: DigitalSykmeldingService by dependencies

    install(SykmeldingerConsumer) {
        clientId = env.runtime.name
        groupId = "migrator-digital-sykmelding"
        onRecord = { record, meta -> digitalSykmeldingConsumer.handleRecord(record, meta) }
        onTombstone = { digitalSykmeldingConsumer.handleTombstone(it) }
    }

    install(KafkaConsumer) {
        clientId = env.runtime.name
        groupId = "migrator-sykmelding"
        consume<ReceivedSykmelding>(
            name = KafkaTopics.okSykmeldingTopic,
            onRecord = { record, meta -> sykmeldingReformatService.handleRecord(record, meta) },
            onTombstone = { sykmeldingReformatService.handleRecord(null, it) }
        )
        consume<ReceivedSykmelding>(
            name = KafkaTopics.avvistSykmeldingTopic,
            onRecord = { record, meta -> sykmeldingReformatService.handleRecord(record, meta) },
            onTombstone = { sykmeldingReformatService.handleRecord(null, it) }
        )
        consume<ReceivedSykmelding>(
            name = KafkaTopics.manuellSykmeldingTopic,
            onRecord = { record, meta -> sykmeldingReformatService.handleRecord(record, meta) },
            onTombstone = { sykmeldingReformatService.handleRecord(null, it) }
        )
    }
}

fun Application.configureKafkaDependencies() {
    dependencies {
        provide<SykmeldingInputProducer> {
            this@configureKafkaDependencies.sykmeldingInputProducer()
        }
        provide<KafkaRecordProducer<ReceivedSykmelding>> {
            this@configureKafkaDependencies.createProducer(
                topic = KafkaTopics.okSykmeldingTopic
            )
        }
        provide<KafkaRecordProducer<ManuellOppgave>> {
            this@configureKafkaDependencies.createProducer(
                topic = KafkaTopics.manuellTilbakedateringTopic
            )
        }
        provide(SykmeldingReformatService::class)
        provide(DigitalSykmeldingService::class)
    }
}
