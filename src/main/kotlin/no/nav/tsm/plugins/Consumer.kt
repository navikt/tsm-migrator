package no.nav.tsm.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.sykmeldinger.input.SykmeldingInputConsumer

fun Application.configureConsumer(sykmeldingInputConsumer: SykmeldingInputConsumer) {
    //val sykmeldingConsumerJob = launch(Dispatchers.IO) { sykmeldingConsumer.start() } // fra ok-sykmelding, manuell-sykmelding, avvist sykmelding til migrert sykmelding
    //val migrertSykmeldingConsumerJob = launch(Dispatchers.IO) { migrertSykmeldingConsumer.start() } // fra migrert-sykmelding til sykmeldinger-input
    //val sykmeldingReformatJob = launch(Dispatchers.IO) { sykmeldingReformatService.start() }
    val sykmeldingInputConsumerJob = launch(Dispatchers.Default) { sykmeldingInputConsumer.start() }
    environment.monitor.subscribe(ApplicationStopping) {
        //sykmeldingConsumerJob.cancel()
        //migrertSykmeldingConsumerJob.cancel()
        //sykmeldingReformatJob.cancel()
        sykmeldingInputConsumerJob.cancel()
    }
}
