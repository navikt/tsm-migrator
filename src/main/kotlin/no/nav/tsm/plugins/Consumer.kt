package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.reformat.sykmelding.SykmeldingReformatService
import no.nav.tsm.reformat.sykmelding.SykmeldingUpdateService
import no.nav.tsm.sykmeldinger.input.SykmeldingInputConsumer
import no.nav.tsm.sykmeldinger.kafka.SykmeldingConsumer

fun Application.configureConsumer(sykmeldingConsumer: SykmeldingConsumer, sykmeldingReformatService: SykmeldingReformatService,
sykmeldingUpdateService: SykmeldingUpdateService) {
    val sykmeldingConsumerJob = launch(Dispatchers.IO) { sykmeldingConsumer.start() }
    val sykmeldingReformatJob = launch(Dispatchers.IO) { sykmeldingReformatService.start() }
    val sykmeldingUpdateJob = launch(Dispatchers.IO) { sykmeldingUpdateService.start() }
    environment.monitor.subscribe(ApplicationStopping) {
        sykmeldingReformatJob.cancel()
        sykmeldingConsumerJob.cancel()
        sykmeldingConsumerJob.cancel()
    }
}
