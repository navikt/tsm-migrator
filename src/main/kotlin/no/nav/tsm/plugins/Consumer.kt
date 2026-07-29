package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.digital.DigitalSykmeldingConsumer
import no.nav.tsm.reformat.sykmelding.SykmeldingReformatService
import no.nav.tsm.sykmeldinger.kafka.SykmeldingConsumer

fun Application.configureConsumer() {
    val sykmeldingConsumer: SykmeldingConsumer by dependencies
    val sykmeldingReformatService: SykmeldingReformatService by dependencies
    val digitalSykmeldingConsumer: DigitalSykmeldingConsumer by dependencies

    val sykmeldingConsumerJob = launch(Dispatchers.IO) { sykmeldingConsumer.start() }
    val sykmeldingReformatJob = launch(Dispatchers.IO) { sykmeldingReformatService.start() }
    val digitalSykmeldingConsumerJob = launch(Dispatchers.IO) { digitalSykmeldingConsumer.start() }

    monitor.subscribe(ApplicationStopping) {
        sykmeldingReformatJob.cancel()
        sykmeldingConsumerJob.cancel()
        digitalSykmeldingConsumerJob.cancel()
    }
}
